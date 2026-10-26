package os.kei.ui.page.main.sync

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.feature.webdav.model.WebDavConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 * Application-scoped WebDAV auto-sync coordinator.
 *
 * Two trigger points (matching the user's chosen "Auto on launch + on change" model):
 *  1. **Launch sync** — when [init] is called from [Application.onCreate], if a config exists and
 *     auto-sync is enabled, reconcile each enabled item from its last known baseline. Local dirty
 *     data is pushed first; clean data may pull remote changes; missing baselines are probed only.
 *  2. **Push-on-background** — when the app moves to background (last foreground Activity stops),
 *     re-export every enabled item, compute its content hash, and push only those whose hash has
 *     drifted from the last persisted [WebDavSyncStore.getItemContentHash]. This catches local
 *     edits that happened during the foreground session without requiring per-store change
 *     listeners (the stores that lack change signals are the reason "on-change" had to be moved
 *     here from the original plan).
 *
 * All work runs on a bounded WebDAV dispatcher under a single [SupervisorJob] so individual item failures
 * never tear down the whole pass. Concurrent triggers are serialised through [mutex] so two
 * lifecycle-driven passes never race the same engine + store state.
 */
internal object WebDavAutoSync {
    private const val TAG = "WebDavAutoSync"

    /** Wait this long after the app backgrounds before pushing — avoids thrashing on quick swaps. */
    private const val BACKGROUND_PUSH_DELAY_MS = 800L

    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.webDavNetwork)
    private val engine = WebDavSyncEngine()
    private val mutex = Mutex()
    private val foregroundCount = AtomicInteger(0)
    private var pendingBackgroundJob: Job? = null
    private var initialized = false
    private lateinit var appContext: Context

    /**
     * Wire the coordinator into the application lifecycle. Call once from
     * [Application.onCreate]. Subsequent calls are no-ops.
     */
    fun init(application: Application) {
        if (initialized) return
        initialized = true
        appContext = application.applicationContext
        application.registerActivityLifecycleCallbacks(LifecycleObserver)
        scope.launch { runLaunchSync(appContext) }
    }

    suspend fun handleScheduledTick(
        context: Context,
        runtimeDiagnostics: WebDavSyncRuntimeDiagnostics? = null,
    ): WebDavAutoSyncSummary? {
        val appContext = context.applicationContext
        val config = WebDavSyncStore.loadConfig() ?: return null
        if (!WebDavSyncStore.isAutoSyncEnabled()) return null
        val nowMs = System.currentTimeMillis()
        val cooldownMs = autoSyncScheduleCooldownMs(
            intervalMs = WebDavSyncStore.getAutoSyncIntervalMs(),
        )
        if (
            !shouldRunLaunchAutoSync(
                nowMs = nowMs,
                lastAutoAttemptMs = WebDavSyncStore.getLastAutoSyncAttemptTime(),
                lastFullSyncMs = WebDavSyncStore.getLastFullSyncTime(),
                cooldownMs = cooldownMs,
            )
        ) {
            return null
        }
        return runAutoSync(
            context = appContext,
            config = config,
            reason = "job",
            runtimeDiagnostics = runtimeDiagnostics ?: WebDavSyncRuntimeDiagnostics.capture(appContext),
        )
    }

    fun handleScheduledTickTimeout(context: Context) {
        val appContext = context.applicationContext
        if (WebDavSyncStore.loadConfig() == null || !WebDavSyncStore.isAutoSyncEnabled()) return
        val nowMs = System.currentTimeMillis()
        WebDavSyncStore.setLastAutoSyncAttemptTime(nowMs)
        WebDavSyncStore.saveLastAutoSyncSummary(
            failedAutoSyncSummary(
                reason = "alarm-timeout",
                finishedAtMs = nowMs,
                targetCount = WebDavSyncItem.entries.count(WebDavSyncStore::isItemEnabled),
            ).also { summary ->
                WebDavSyncStore.appendHistory(
                    summary.toHistoryEntry(
                        source = WebDavSyncHistorySource.Auto,
                        startedAtMs = nowMs,
                    ),
                )
            },
        )
        WebDavSyncNotificationDispatcher.notifyFailed(
            context = appContext,
            operation = WebDavSyncNotificationOperation.Sync,
            total = WebDavSyncItem.entries.count(WebDavSyncStore::isItemEnabled),
        )
        AppBackgroundScheduler.scheduleWebDavAutoSync(context.applicationContext)
    }

    /** First app start (cold launch) → baseline-aware sync if config + auto-sync allow it. */
    private suspend fun runLaunchSync(context: Context) {
        val config = WebDavSyncStore.loadConfig() ?: return
        if (!WebDavSyncStore.isAutoSyncEnabled()) return
        val nowMs = System.currentTimeMillis()
        val cooldownMs = autoSyncScheduleCooldownMs(
            intervalMs = WebDavSyncStore.getAutoSyncIntervalMs(),
        )
        if (
            !shouldRunLaunchAutoSync(
                nowMs = nowMs,
                lastAutoAttemptMs = WebDavSyncStore.getLastAutoSyncAttemptTime(),
                lastFullSyncMs = WebDavSyncStore.getLastFullSyncTime(),
                cooldownMs = cooldownMs,
            )
        ) {
            AppLogger.i(TAG, "auto-sync (launch) delayed by configured interval (${cooldownMs}ms)")
            return
        }
        runAutoSync(context, config, reason = "launch")
    }

    /** App moved to background → push the items whose local content has drifted since last sync. */
    private fun schedulePushIfChanged() {
        pendingBackgroundJob?.cancel()
        val context = appContext
        pendingBackgroundJob = scope.launch {
            try {
                delay(BACKGROUND_PUSH_DELAY_MS)
                val config = WebDavSyncStore.loadConfig() ?: return@launch
                if (!WebDavSyncStore.isAutoSyncEnabled()) return@launch
                val cooldownMs = autoSyncScheduleCooldownMs(
                    intervalMs = WebDavSyncStore.getAutoSyncIntervalMs(),
                )
                val nowMs = System.currentTimeMillis()
                if (
                    !shouldRunLaunchAutoSync(
                        nowMs = nowMs,
                        lastAutoAttemptMs = WebDavSyncStore.getLastAutoSyncAttemptTime(),
                        lastFullSyncMs = WebDavSyncStore.getLastFullSyncTime(),
                        cooldownMs = cooldownMs,
                    )
                ) {
                    AppLogger.i(TAG, "auto-push (background) delayed by sync interval (${cooldownMs}ms)")
                    return@launch
                }
                pushChangedItems(context, config, reason = "background")
            } finally {
                AppBackgroundScheduler.scheduleWebDavAutoSync(context)
            }
        }
    }

    private suspend fun runAutoSync(
        context: Context,
        config: WebDavConfig,
        reason: String,
        runtimeDiagnostics: WebDavSyncRuntimeDiagnostics? = null,
    ): WebDavAutoSyncSummary = mutex.withLock {
        val startedAtMs = System.currentTimeMillis()
        WebDavSyncStore.setLastAutoSyncAttemptTime(startedAtMs)
        WebDavSyncStore.saveLastAutoSyncSummary(
            WebDavAutoSyncSummary(
                status = WebDavAutoSyncStatus.Running,
                reason = reason,
                finishedAtMs = startedAtMs,
                targetCount = 0,
                succeededCount = 0,
                failedCount = 0,
                skippedCount = 0,
            ),
        )
        try {
            val coroutineContext = currentCoroutineContext()
            val ports = buildWebDavSyncDataPorts(context)
            val targets = WebDavSyncItem.entries.filter { WebDavSyncStore.isItemEnabled(it) }
            val outcomes = mutableListOf<WebDavItemOutcome>()
            val itemOutcomes = mutableListOf<Pair<WebDavSyncItem, WebDavItemOutcome>>()
            val skippedOutcomes = mutableListOf<Pair<WebDavSyncItem, WebDavItemOutcome>>()
            var skippedCount = 0
            if (targets.isNotEmpty()) {
                WebDavSyncNotificationDispatcher.notifyStarted(
                    context = context,
                    operation = WebDavSyncNotificationOperation.Sync,
                    total = targets.size,
                )
            }
            for (item in targets) {
                coroutineContext.ensureActive()
                val port = ports[item]
                if (port == null) {
                    skippedCount += 1
                    skippedOutcomes += item to WebDavItemOutcome(WebDavItemStatus.Error)
                    notifyAutoSyncProgress(
                        context = context,
                        operation = WebDavSyncNotificationOperation.Sync,
                        outcomes = itemOutcomes,
                        skippedCount = skippedCount,
                        total = targets.size,
                    )
                    continue
                }
                val pending = WebDavSyncStore.loadItemPendingSummary(item)?.state
                if (shouldDeferPendingWebDavAutoSync(pending, port)) {
                    skippedCount += 1
                    skippedOutcomes += item to pending.toDeferredAutoSyncOutcome()
                    notifyAutoSyncProgress(
                        context = context,
                        operation = WebDavSyncNotificationOperation.Sync,
                        outcomes = itemOutcomes,
                        skippedCount = skippedCount,
                        total = targets.size,
                    )
                    continue
                }
                val outcome = reconcileItem(config, item, port)
                recordFingerprintRevisionIfSynced(item, port, outcome)
                outcomes += outcome
                itemOutcomes += item to outcome
                if (!outcome.isSuccess) {
                    AppLogger.w(TAG, "auto-sync ($reason) ${item.name} -> ${outcome.status} ${outcome.detail.orEmpty()}")
                }
                notifyAutoSyncProgress(
                    context = context,
                    operation = WebDavSyncNotificationOperation.Sync,
                    outcomes = itemOutcomes,
                    skippedCount = skippedCount,
                    total = targets.size,
                )
            }
            val summary = buildAutoSyncSummary(
                reason = reason,
                finishedAtMs = System.currentTimeMillis(),
                targetCount = targets.size,
                outcomes = outcomes,
                skippedCount = skippedCount,
                skippedOutcomes = skippedOutcomes,
            )
            WebDavSyncStore.saveLastAutoSyncSummary(summary)
            WebDavSyncStore.appendHistory(
                buildWebDavSyncHistoryEntry(
                    source = WebDavSyncHistorySource.Auto,
                    kind = null,
                    reason = reason,
                    startedAtMs = startedAtMs,
                    finishedAtMs = summary.finishedAtMs,
                    targetCount = targets.size,
                    outcomes = itemOutcomes,
                    skippedCount = skippedCount,
                    skippedOutcomes = skippedOutcomes,
                    runtimeDiagnostics = runtimeDiagnostics,
                ),
            )
            if (runtimeDiagnostics?.previousStopReason != null) {
                WebDavSyncStore.clearLastJobStopReason()
            }
            if (summary.status == WebDavAutoSyncStatus.Success) {
                WebDavSyncStore.setLastFullSyncTime(summary.finishedAtMs)
            }
            if (targets.isNotEmpty()) {
                WebDavSyncNotificationDispatcher.notifyFinished(
                    context = context,
                    operation = WebDavSyncNotificationOperation.Sync,
                    status = summary.status,
                    total = summary.targetCount,
                    succeeded = summary.succeededCount,
                    failed = summary.failedCount,
                    skipped = summary.skippedCount,
                )
            }
            summary
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "auto-sync ($reason) failed", e)
            val summary = failedAutoSyncSummary(
                reason = reason,
                finishedAtMs = System.currentTimeMillis(),
            )
            WebDavSyncStore.saveLastAutoSyncSummary(summary)
            WebDavSyncStore.appendHistory(
                summary.toHistoryEntry(
                    source = WebDavSyncHistorySource.Auto,
                    startedAtMs = startedAtMs,
                    runtimeDiagnostics = runtimeDiagnostics,
                ),
            )
            if (runtimeDiagnostics?.previousStopReason != null) {
                WebDavSyncStore.clearLastJobStopReason()
            }
            WebDavSyncNotificationDispatcher.notifyFailed(
                context = context,
                operation = WebDavSyncNotificationOperation.Sync,
                total = summary.targetCount,
            )
            summary
        }
    }

    private suspend fun pushChangedItems(
        context: Context,
        config: WebDavConfig,
        reason: String,
    ): WebDavAutoSyncSummary = mutex.withLock {
        val startedAtMs = System.currentTimeMillis()
        try {
            val coroutineContext = currentCoroutineContext()
            val ports = buildWebDavSyncDataPorts(context)
            val targets = WebDavSyncItem.entries.filter { WebDavSyncStore.isItemEnabled(it) }
            val changedTargets =
                targets.mapNotNull { item ->
                    coroutineContext.ensureActive()
                    val port = ports[item] ?: return@mapNotNull null
                    val pending = WebDavSyncStore.loadItemPendingSummary(item)?.state
                    if (shouldDeferPendingWebDavAutoSync(pending, port)) return@mapNotNull null
                    val currentHash = WebDavSyncEngine.contentHash(port.fingerprintJson())
                    val storedHash = WebDavSyncStore.getItemContentHash(item)
                    val requiresFullReconciliation =
                        requiresWebDavFingerprintReconciliation(
                            storedRevision = WebDavSyncStore.getItemFingerprintRevision(item),
                            currentRevision = port.fingerprintRevision,
                        )
                    if (currentHash == storedHash && !requiresFullReconciliation) {
                        null
                    } else {
                        WebDavAutoUploadTarget(
                            item = item,
                            port = port,
                            storedHash = storedHash,
                            requiresFullReconciliation = requiresFullReconciliation,
                        )
                    }
                }
            val outcomes = mutableListOf<WebDavItemOutcome>()
            val itemOutcomes = mutableListOf<Pair<WebDavSyncItem, WebDavItemOutcome>>()
            val changedCount = changedTargets.size
            if (changedCount > 0) {
                WebDavSyncNotificationDispatcher.notifyStarted(
                    context = context,
                    operation = WebDavSyncNotificationOperation.Upload,
                    total = changedCount,
                )
            }
            for (target in changedTargets) {
                coroutineContext.ensureActive()
                val outcome =
                    pushLocalChange(
                        config = config,
                        item = target.item,
                        port = target.port,
                        storedHash = target.storedHash,
                        requiresFullReconciliation = target.requiresFullReconciliation,
                    )
                recordFingerprintRevisionIfSynced(target.item, target.port, outcome)
                if (!outcome.isSuccess) {
                    AppLogger.w(TAG, "auto-push ($reason) ${target.item.name} -> ${outcome.status} ${outcome.detail.orEmpty()}")
                }
                outcomes += outcome
                itemOutcomes += target.item to outcome
                notifyAutoSyncProgress(
                    context = context,
                    operation = WebDavSyncNotificationOperation.Upload,
                    outcomes = itemOutcomes,
                    skippedCount = 0,
                    total = changedCount,
                )
            }
            val summary = buildAutoSyncSummary(
                reason = reason,
                finishedAtMs = System.currentTimeMillis(),
                targetCount = changedCount,
                outcomes = outcomes,
                skippedCount = 0,
            )
            if (changedCount > 0 || summary.hasIssues) {
                WebDavSyncStore.setLastAutoSyncAttemptTime(startedAtMs)
                WebDavSyncStore.saveLastAutoSyncSummary(summary)
                WebDavSyncStore.appendHistory(
                    buildWebDavSyncHistoryEntry(
                        source = WebDavSyncHistorySource.Auto,
                        kind = WebDavSyncHistoryKind.Upload,
                        reason = reason,
                        startedAtMs = startedAtMs,
                        finishedAtMs = summary.finishedAtMs,
                        targetCount = changedCount,
                        outcomes = itemOutcomes,
                        skippedCount = 0,
                    ),
                )
                WebDavSyncNotificationDispatcher.notifyFinished(
                    context = context,
                    operation = WebDavSyncNotificationOperation.Upload,
                    status = summary.status,
                    total = summary.targetCount,
                    succeeded = summary.succeededCount,
                    failed = summary.failedCount,
                    skipped = summary.skippedCount,
                )
            }
            summary
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "auto-push failed", e)
            val summary = failedAutoSyncSummary(
                reason = reason,
                finishedAtMs = System.currentTimeMillis(),
            )
            WebDavSyncStore.setLastAutoSyncAttemptTime(startedAtMs)
            WebDavSyncStore.saveLastAutoSyncSummary(summary)
            WebDavSyncStore.appendHistory(
                summary.toHistoryEntry(
                    source = WebDavSyncHistorySource.Auto,
                    startedAtMs = startedAtMs,
                ),
            )
            WebDavSyncNotificationDispatcher.notifyFailed(
                context = context,
                operation = WebDavSyncNotificationOperation.Upload,
                total = summary.targetCount,
            )
            summary
        }
    }

    private suspend fun reconcileItem(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavItemOutcome {
        val currentHash = WebDavSyncEngine.contentHash(port.fingerprintJson())
        val storedHash = WebDavSyncStore.getItemContentHash(item)
        return when {
            storedHash == null -> adoptOrDeferMissingBaseline(config, item, port)
            requiresWebDavFingerprintReconciliation(
                storedRevision = WebDavSyncStore.getItemFingerprintRevision(item),
                currentRevision = port.fingerprintRevision,
            ) -> {
                AppLogger.i(
                    TAG,
                    "auto-sync rebaselining ${item.name} fingerprint revision " +
                        "${WebDavSyncStore.getItemFingerprintRevision(item)} -> ${port.fingerprintRevision}",
                )
                engine.sync(config, item, port)
            }
            currentHash != storedHash ->
                pushLocalChange(
                    config = config,
                    item = item,
                    port = port,
                    storedHash = storedHash,
                )
            else -> engine.sync(config, item, port)
        }
    }

    private suspend fun pushLocalChange(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        storedHash: String?,
        requiresFullReconciliation: Boolean = false,
    ): WebDavItemOutcome =
        if (requiresFullReconciliation) {
            engine.sync(config, item, port)
        } else {
            engine.uploadLocalChange(
                config = config,
                item = item,
                port = port,
                expectedRemoteEtag = WebDavSyncStore.getItemEtag(item),
                expectedRemoteHash = storedHash,
            )
        }

    private fun recordFingerprintRevisionIfSynced(
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
        outcome: WebDavItemOutcome,
    ) {
        if (outcome.isSuccess) {
            WebDavSyncStore.setItemFingerprintRevision(item, port.fingerprintRevision)
        }
    }

    private suspend fun adoptOrDeferMissingBaseline(
        config: WebDavConfig,
        item: WebDavSyncItem,
        port: WebDavSyncDataPort,
    ): WebDavItemOutcome {
        val planItem =
            engine.prepareChange(
                config = config,
                kind = WebDavBatchKind.Sync,
                item = item,
                port = port,
            )
        return when (val remote = planItem.remoteState) {
            WebDavSyncPlanRemoteState.Empty ->
                engine.upload(
                    config = config,
                    item = item,
                    port = port,
                    remoteKnownEmpty = true,
                )
            is WebDavSyncPlanRemoteState.Found -> {
                if (planItem.localHash == remote.contentHash) {
                    engine.sync(config, item, port)
                } else if (port.mergeRemoteOnAutoConflict) {
                    engine.sync(config, item, port)
                } else {
                    WebDavSyncStore.setItemPendingState(
                        item = item,
                        state = WebDavSyncPendingState.BaselineRequired,
                    )
                    WebDavItemOutcome(WebDavItemStatus.BaselineRequired)
                }
            }
            is WebDavSyncPlanRemoteState.Error ->
                WebDavItemOutcome(remote.status, remote.detail)
        }
    }

    private object LifecycleObserver : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) {
            // Reaching foreground → cancel any pending push so we don't fire while the user is back.
            if (foregroundCount.getAndIncrement() == 0) {
                pendingBackgroundJob?.cancel()
                pendingBackgroundJob = null
            }
        }
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) {
            val remaining = foregroundCount.updateAndGet { current ->
                (current - 1).coerceAtLeast(0)
            }
            if (remaining == 0) {
                schedulePushIfChanged()
            }
        }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}

internal fun autoSyncScheduleCooldownMs(
    intervalMs: Long,
): Long = intervalMs.coerceAtLeast(0L)

internal fun shouldRunLaunchAutoSync(
    nowMs: Long,
    lastAutoAttemptMs: Long,
    lastFullSyncMs: Long,
    cooldownMs: Long,
): Boolean {
    if (cooldownMs <= 0L) return true
    val lastTouchMs = maxOf(lastAutoAttemptMs, lastFullSyncMs)
    if (lastTouchMs <= 0L) return true
    return nowMs - lastTouchMs >= cooldownMs
}

internal fun shouldDeferPendingWebDavAutoSync(pending: WebDavSyncPendingState?): Boolean =
    pending == WebDavSyncPendingState.RemoteConflict ||
        pending == WebDavSyncPendingState.BaselineRequired

internal fun shouldDeferPendingWebDavAutoSync(
    pending: WebDavSyncPendingState?,
    port: WebDavSyncDataPort,
): Boolean = shouldDeferPendingWebDavAutoSync(pending) && !port.mergeRemoteOnAutoConflict

private fun buildAutoSyncSummary(
    reason: String,
    finishedAtMs: Long,
    targetCount: Int,
    outcomes: List<WebDavItemOutcome>,
    skippedCount: Int,
    skippedOutcomes: List<Pair<WebDavSyncItem, WebDavItemOutcome>> = emptyList(),
): WebDavAutoSyncSummary {
    val succeededCount = outcomes.count { it.isSuccess }
    val reviewCount = outcomes.count { outcome ->
        outcome.status == WebDavItemStatus.BaselineRequired ||
            outcome.status == WebDavItemStatus.ConflictUnresolved
    }
    val failedCount = outcomes.size - succeededCount
    val technicalFailureCount = failedCount - reviewCount
    val skippedReviewCount = skippedOutcomes.count { (_, outcome) -> outcome.requiresReview }
    val skippedTechnicalFailureCount = skippedOutcomes.count { (_, outcome) ->
        !outcome.isSuccess && !outcome.requiresReview
    }
    val status =
        when {
            targetCount <= 0 || (outcomes.isEmpty() && skippedOutcomes.isEmpty()) -> WebDavAutoSyncStatus.Skipped
            technicalFailureCount > 0 || skippedTechnicalFailureCount > 0 -> WebDavAutoSyncStatus.Failed
            reviewCount > 0 || skippedReviewCount > 0 -> WebDavAutoSyncStatus.NeedsReview
            else -> WebDavAutoSyncStatus.Success
        }
    return WebDavAutoSyncSummary(
        status = status,
        reason = reason,
        finishedAtMs = finishedAtMs,
        targetCount = targetCount,
        succeededCount = succeededCount,
        failedCount = failedCount,
        skippedCount = skippedCount,
    )
}

private data class WebDavAutoUploadTarget(
    val item: WebDavSyncItem,
    val port: WebDavSyncDataPort,
    val storedHash: String?,
    val requiresFullReconciliation: Boolean,
)

internal fun requiresWebDavFingerprintReconciliation(
    storedRevision: Int,
    currentRevision: Int,
): Boolean = storedRevision != currentRevision

private fun notifyAutoSyncProgress(
    context: Context,
    operation: WebDavSyncNotificationOperation,
    outcomes: List<Pair<WebDavSyncItem, WebDavItemOutcome>>,
    skippedCount: Int,
    total: Int,
) {
    val succeeded = outcomes.count { (_, outcome) -> outcome.isSuccess }
    val failed = outcomes.size - succeeded
    WebDavSyncNotificationDispatcher.notifyProgress(
        context = context,
        operation = operation,
        current = outcomes.size + skippedCount,
        total = total,
        succeeded = succeeded,
        failed = failed,
        skipped = skippedCount,
    )
}

private val WebDavItemOutcome.requiresReview: Boolean
    get() =
        status == WebDavItemStatus.BaselineRequired ||
            status == WebDavItemStatus.ConflictUnresolved

private fun WebDavSyncPendingState?.toDeferredAutoSyncOutcome(): WebDavItemOutcome =
    when (this) {
        WebDavSyncPendingState.BaselineRequired -> WebDavItemOutcome(WebDavItemStatus.BaselineRequired)
        WebDavSyncPendingState.RemoteConflict -> WebDavItemOutcome(WebDavItemStatus.ConflictUnresolved)
        WebDavSyncPendingState.LocalUploadPending,
        null,
        -> WebDavItemOutcome(WebDavItemStatus.UpToDate)
    }

private fun failedAutoSyncSummary(
    reason: String,
    finishedAtMs: Long,
    targetCount: Int = 1,
): WebDavAutoSyncSummary =
    WebDavAutoSyncSummary(
        status = WebDavAutoSyncStatus.Failed,
        reason = reason,
        finishedAtMs = finishedAtMs,
        targetCount = targetCount.coerceAtLeast(1),
        succeededCount = 0,
        failedCount = 1,
        skippedCount = 0,
    )
