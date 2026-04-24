package os.kei.ui.page.main.github.page.action

import os.kei.R
import os.kei.core.background.AppBackgroundScheduler
import os.kei.feature.github.data.local.AppIconCache
import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.domain.GitHubReleaseCheckService
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.notification.GitHubRefreshNotificationHelper
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.isLocalAppUninstalled
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack
import os.kei.ui.page.main.github.state.toCacheEntry
import os.kei.ui.page.main.github.state.toUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GitHubRefreshActions(
    private val env: GitHubPageActionEnvironment
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state

    fun persistCheckCache(refreshTimestamp: Long = state.lastRefreshMs) {
        val states = state.trackedItems.associate { item ->
            val itemState = state.checkStates[item.id] ?: VersionCheckUi()
            item.id to itemState.toCacheEntry()
        }
        GitHubTrackStore.saveCheckCache(states, refreshTimestamp)
    }

    fun cancelRefreshAll(reason: String? = null) {
        if (state.refreshAllJob?.isActive == true) {
            state.refreshAllJob?.cancel()
            state.refreshAllJob = null
            val trackedCount = state.trackedItems.size
            if (trackedCount > 0) {
                val checkedCount = (state.refreshProgress * trackedCount.toFloat()).toInt()
                    .coerceIn(0, trackedCount)
                val updatableCount = state.trackedItems.count { state.checkStates[it.id]?.hasUpdate == true }
                val preReleaseUpdateCount =
                    state.trackedItems.count { state.checkStates[it.id]?.hasPreReleaseUpdate == true }
                val failedCount = state.trackedItems.count { state.checkStates[it.id]?.failed == true }
                GitHubRefreshNotificationHelper.notifyCancelled(
                    context = context,
                    current = checkedCount,
                    total = trackedCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    updatableCount = updatableCount,
                    failedCount = failedCount
                )
            } else {
                GitHubRefreshNotificationHelper.cancel(context)
            }
            state.overviewRefreshState = if (state.trackedItems.isEmpty()) {
                OverviewRefreshState.Idle
            } else if (state.checkStates.isNotEmpty()) {
                OverviewRefreshState.Cached
            } else {
                OverviewRefreshState.Idle
            }
            state.refreshProgress = 0f
            env.toast(reason)
        }
    }

    suspend fun reloadApps(forceRefresh: Boolean = false) {
        state.appList = withContext(Dispatchers.IO) {
            GitHubVersionUtils.queryInstalledLaunchableApps(
                context = context,
                forceRefresh = forceRefresh
            )
        }
        val trackedPackages = state.trackedItems
            .map { it.packageName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (trackedPackages.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                AppIconCache.preload(context, trackedPackages)
            }
        }
        state.appListLoaded = true
    }

    suspend fun initializePage() {
        applyTrackSnapshot(
            withContext(Dispatchers.IO) { GitHubTrackStore.loadSnapshot() }
        )
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
        reloadApps(forceRefresh = false)
        val refreshedRequestedTracks = refreshRequestedTracksIfNeeded()
        val hasTracked = state.trackedItems.isNotEmpty()
        val hasCachedForTracked = state.trackedItems.any { item ->
            state.checkStates.containsKey(item.id)
        }
        val stale = hasTracked && state.lastRefreshMs > 0L &&
            (System.currentTimeMillis() - state.lastRefreshMs) >=
            state.refreshIntervalHours * 60L * 60L * 1000L
        when {
            refreshedRequestedTracks -> state.overviewRefreshState = OverviewRefreshState.Cached
            !hasCachedForTracked && hasTracked -> refreshAllTracked(showToast = false)
            stale -> refreshAllTracked(showToast = false)
            hasCachedForTracked -> state.overviewRefreshState = OverviewRefreshState.Cached
            else -> state.overviewRefreshState = OverviewRefreshState.Idle
        }
    }

    suspend fun syncSnapshotFromStore(forceRefreshApps: Boolean = true) {
        applyTrackSnapshot(
            withContext(Dispatchers.IO) { GitHubTrackStore.loadSnapshot() }
        )
        if (forceRefreshApps) {
            reloadApps(forceRefresh = true)
        }
        refreshRequestedTracksIfNeeded()
        val hasTracked = state.trackedItems.isNotEmpty()
        val hasCachedForTracked = state.trackedItems.any { item ->
            state.checkStates.containsKey(item.id)
        }
        state.overviewRefreshState = when {
            !hasTracked -> OverviewRefreshState.Idle
            hasCachedForTracked -> OverviewRefreshState.Cached
            else -> OverviewRefreshState.Idle
        }
    }

    suspend fun syncLocalAppStateWithInstalledApps(forceRefreshApps: Boolean = true) {
        if (forceRefreshApps) {
            reloadApps(forceRefresh = true)
        }
        val trackedSnapshot = state.trackedItems.toList()
        if (trackedSnapshot.isEmpty()) return

        trackedSnapshot.forEach { item ->
            val packageName = item.packageName.trim()
            if (packageName.isBlank()) return@forEach

            val latestLocalVersionInfo = runCatching {
                GitHubVersionUtils.localVersionInfoOrNull(context, packageName)
            }.getOrNull()
            val installed = latestLocalVersionInfo != null

            val cachedState = state.checkStates[item.id] ?: return@forEach
            val cachedUninstalled = cachedState.isLocalAppUninstalled()
            val cachedVersionCode = cachedState.localVersionCode
            val latestVersionCode = latestLocalVersionInfo?.versionCode ?: -1L
            val shouldRefresh = when {
                installed && cachedUninstalled -> true
                !installed && !cachedUninstalled -> true
                installed && cachedVersionCode != latestVersionCode -> true
                else -> false
            }
            if (shouldRefresh) {
                refreshItem(item = item, showToastOnError = false)
            }
        }
    }

    private fun refreshRequestedTracksIfNeeded(): Boolean {
        val trackedById = state.trackedItems.associateBy { it.id }
        if (trackedById.isEmpty()) return false
        val requestedIds = GitHubTrackStoreSignals.consumeTrackRefreshRequests(trackedById.keys)
        if (requestedIds.isEmpty()) return false
        requestedIds.forEach { trackId ->
            val item = trackedById[trackId] ?: return@forEach
            refreshItem(item = item, showToastOnError = false)
        }
        return true
    }

    fun refreshItem(
        item: GitHubTrackedApp,
        showToastOnError: Boolean = false,
        keepCurrentVisualWhileRefreshing: Boolean = false,
        onUpdated: ((VersionCheckUi) -> Unit)? = null
    ): Job {
        return scope.launch {
            refreshItemNow(
                item = item,
                showToastOnError = showToastOnError,
                keepCurrentVisualWhileRefreshing = keepCurrentVisualWhileRefreshing,
                onUpdated = onUpdated
            )
        }
    }

    suspend fun refreshItemNow(
        item: GitHubTrackedApp,
        showToastOnError: Boolean = false,
        keepCurrentVisualWhileRefreshing: Boolean = false,
        onUpdated: ((VersionCheckUi) -> Unit)? = null
    ) {
        val previousState = state.checkStates[item.id] ?: VersionCheckUi()
        val checkingMessage = context.getString(R.string.github_msg_checking)
        state.checkStates[item.id] = if (keepCurrentVisualWhileRefreshing) {
            previousState.copy(message = checkingMessage)
        } else {
            previousState.copy(
                loading = true,
                message = checkingMessage
            )
        }
        val itemState = resolveItemState(item)
        if (state.trackedItems.none { it.id == item.id }) return
        if (showToastOnError && itemState.failed) {
            env.toast(itemState.message)
        }
        state.checkStates[item.id] = itemState
        persistCheckCache()
        onUpdated?.invoke(itemState)
    }

    fun refreshAllTracked(
        showToast: Boolean = true,
        onFinished: (() -> Unit)? = null
    ) {
        val snapshot = state.trackedItems.toList()
        if (snapshot.isEmpty()) {
            if (showToast) {
                env.toast(R.string.github_toast_no_checkable_item)
            }
            state.overviewRefreshState = OverviewRefreshState.Idle
            state.refreshProgress = 0f
            GitHubRefreshNotificationHelper.cancel(context)
            return
        }
        state.refreshAllJob?.cancel()
        state.refreshAllJob = scope.launch {
            GitHubTrackStore.clearCheckCache()
            state.lastRefreshMs = 0L
            state.overviewRefreshState = OverviewRefreshState.Refreshing
            state.refreshProgress = 0f
            val totalCount = snapshot.size
            var updatableCount = 0
            var preReleaseUpdateCount = 0
            var failedCount = 0
            GitHubRefreshNotificationHelper.notifyProgress(
                context = context,
                current = 0,
                total = totalCount,
                preReleaseUpdateCount = 0,
                updatableCount = 0,
                failedCount = 0
            )
            snapshot.forEach { item ->
                state.checkStates[item.id] = VersionCheckUi(
                    loading = true,
                    message = context.getString(R.string.github_msg_checking)
                )
            }
            snapshot.forEachIndexed { index, item ->
                val itemState = resolveItemState(item)
                if (state.trackedItems.any { it.id == item.id }) {
                    state.checkStates[item.id] = itemState
                }
                if (itemState.hasUpdate == true) {
                    updatableCount += 1
                }
                if (itemState.hasPreReleaseUpdate) {
                    preReleaseUpdateCount += 1
                }
                if (itemState.failed) {
                    failedCount += 1
                }
                state.refreshProgress = (index + 1).toFloat() / snapshot.size.toFloat()
                GitHubRefreshNotificationHelper.notifyProgress(
                    context = context,
                    current = index + 1,
                    total = totalCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    updatableCount = updatableCount,
                    failedCount = failedCount
                )
                if (showToast && itemState.failed) {
                    env.toast(
                        R.string.github_toast_repo_message,
                        item.owner,
                        item.repo,
                        itemState.message
                    )
                }
                if (index < snapshot.lastIndex) delay(120)
            }
            state.overviewRefreshState = OverviewRefreshState.Completed
            state.lastRefreshMs = System.currentTimeMillis()
            state.refreshProgress = 1f
            persistCheckCache(state.lastRefreshMs)
            onFinished?.invoke()
            GitHubRefreshNotificationHelper.notifyCompleted(
                context = context,
                total = totalCount,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount
            )
            state.refreshAllJob = null
        }
    }

    private suspend fun resolveItemState(
        item: GitHubTrackedApp
    ): VersionCheckUi {
        return withContext(Dispatchers.IO) {
            GitHubReleaseCheckService.evaluateTrackedApp(context, item).toUi()
        }
    }

    private fun applyTrackSnapshot(trackSnapshot: GitHubTrackSnapshot) {
        val activeStrategyId = trackSnapshot.lookupConfig.selectedStrategy.storageId
        val snapshotAssetSourceSignature = listOf(
            "asset-v2",
            trackSnapshot.lookupConfig.selectedStrategy.storageId,
            trackSnapshot.lookupConfig.apiToken.isNotBlank().toString(),
            trackSnapshot.lookupConfig.aggressiveApkFiltering.toString()
        ).joinToString("|")
        state.lookupConfig = trackSnapshot.lookupConfig
        state.selectedStrategyInput = trackSnapshot.lookupConfig.selectedStrategy
        state.githubApiTokenInput = trackSnapshot.lookupConfig.apiToken
        state.checkAllTrackedPreReleasesInput = trackSnapshot.lookupConfig.checkAllTrackedPreReleases
        state.aggressiveApkFilteringInput = trackSnapshot.lookupConfig.aggressiveApkFiltering
        state.shareImportLinkageEnabledInput = trackSnapshot.lookupConfig.shareImportLinkageEnabled
        state.onlineShareTargetPackageInput = trackSnapshot.lookupConfig.onlineShareTargetPackage
        state.preferredDownloaderPackageInput = trackSnapshot.lookupConfig.preferredDownloaderPackage
        state.refreshIntervalHours = trackSnapshot.refreshIntervalHours
        state.refreshIntervalHoursInput = trackSnapshot.refreshIntervalHours
        if (
            state.assetSourceSignature.isNotBlank() &&
            state.assetSourceSignature != snapshotAssetSourceSignature
        ) {
            state.clearAllAssetUiState()
        }
        state.assetSourceSignature = snapshotAssetSourceSignature

        state.trackedItems.clear()
        state.trackedItems.addAll(trackSnapshot.items)
        state.retainTrackedUiState(trackSnapshot.items.map { it.id }.toSet())
        state.trackedFirstInstallAtByPackage.clear()
        state.trackedFirstInstallAtByPackage.putAll(trackSnapshot.trackedFirstInstallAtByPackage)
        state.retainTrackedFirstInstallAtByTrackedItems()
        state.trackedAddedAtById.clear()
        state.trackedAddedAtById.putAll(trackSnapshot.trackedAddedAtById)
        state.retainTrackedAddedAtByTrackedItems()
        state.pendingShareImportTrack = trackSnapshot.pendingShareImportTrack?.let { pending ->
            GitHubPendingShareImportTrack(
                projectUrl = pending.projectUrl,
                owner = pending.owner,
                repo = pending.repo,
                releaseTag = pending.releaseTag,
                assetName = pending.assetName,
                armedAtMillis = pending.armedAtMillis
            )
        }
        state.pendingShareImportAttachCandidate = null

        val cachedStates = trackSnapshot.checkCache
        state.checkStates.clear()
        trackSnapshot.items.forEach { item ->
            cachedStates[item.id]
                ?.takeIf { cache ->
                    val sourceId = cache.sourceStrategyId.ifBlank {
                        GitHubLookupStrategyOption.AtomFeed.storageId
                    }
                    sourceId == activeStrategyId
                }
                ?.let { cached ->
                    state.checkStates[item.id] = cached.toUi()
                }
        }
        state.lastRefreshMs = if (state.checkStates.isNotEmpty()) {
            trackSnapshot.lastRefreshMs
        } else {
            0L
        }
    }
}
