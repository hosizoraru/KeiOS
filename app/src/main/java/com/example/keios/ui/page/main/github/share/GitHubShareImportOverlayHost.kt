package com.example.keios.ui.page.main

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.keios.R
import com.example.keios.core.background.AppBackgroundScheduler
import com.example.keios.core.system.AppPackageChangedEvent
import com.example.keios.core.system.AppPackageChangedEvents
import com.example.keios.feature.github.data.local.GitHubPendingShareImportTrackRecord
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.local.GitHubTrackStoreSignals
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetRepository
import com.example.keios.feature.github.data.remote.GitHubShareImportResolver
import com.example.keios.feature.github.domain.GitHubReleaseCheckService
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.query.systemDownloadManagerOption
import com.example.keios.ui.page.main.github.sheet.GitHubShareImportAttachConfirmDialog
import com.example.keios.ui.page.main.github.sheet.GitHubShareImportDialog
import com.example.keios.ui.page.main.github.sheet.GitHubShareImportPendingDialog
import com.example.keios.ui.page.main.github.state.toCacheEntry
import com.example.keios.ui.page.main.github.state.toUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val shareImportTrackMaxAgeMs = 25 * 60 * 1000L
private const val shareImportTrackUpdateToleranceMs = 2 * 60 * 1000L
private const val shareImportMinHandleIntervalMs = 1200L

private val shareImportAttachActions = setOf(
    Intent.ACTION_PACKAGE_ADDED,
    Intent.ACTION_PACKAGE_REPLACED,
    Intent.ACTION_PACKAGE_CHANGED
)

private data class OverlayInstalledPackageSnapshot(
    val packageName: String,
    val appLabel: String,
    val lastUpdateTimeMs: Long,
    val firstInstallTimeMs: Long
)

private sealed interface ShareImportDeliveryResult {
    data class Success(val toastResId: Int) : ShareImportDeliveryResult
    data class Failure(val toastResId: Int) : ShareImportDeliveryResult
}

private sealed interface ShareImportAttachResult {
    data class Added(val appLabel: String) : ShareImportAttachResult
    data object Duplicate : ShareImportAttachResult
    data class Failed(val message: String) : ShareImportAttachResult
}

@Composable
internal fun GitHubShareImportOverlayHost(
    incomingGitHubShareText: String?,
    incomingGitHubShareToken: Int,
    onIncomingGitHubShareConsumed: () -> Unit,
    onNavigateToGitHubPage: () -> Unit,
    showPendingArmedSheet: Boolean = false,
    onClosePendingArmedSheet: (() -> Unit)? = null,
    onIdleWithNoPendingFlow: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = remember {
        CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    }
    DisposableEffect(Unit) {
        onDispose {
            scope.cancel()
        }
    }
    var pendingPreview by remember { mutableStateOf<GitHubShareImportPreview?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var incomingResolveRunning by remember { mutableStateOf(false) }
    var pendingTrack by remember { mutableStateOf<GitHubPendingShareImportTrackRecord?>(null) }
    var attachCandidate by remember { mutableStateOf<GitHubPendingShareImportAttachCandidate?>(null) }
    var attachDuplicateExists by remember { mutableStateOf(false) }
    var attachSubmitting by remember { mutableStateOf(false) }
    var idleCallbackDispatched by remember { mutableStateOf(false) }
    val handledAtByPackage = remember { mutableStateMapOf<String, Long>() }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            GitHubTrackStore.loadPendingShareImportTrack()
        }
        if (loaded == null) {
            pendingTrack = null
            return@LaunchedEffect
        }
        val age = (System.currentTimeMillis() - loaded.armedAtMillis).coerceAtLeast(0L)
        if (age > shareImportTrackMaxAgeMs) {
            withContext(Dispatchers.IO) {
                GitHubTrackStore.savePendingShareImportTrack(null)
            }
            GitHubTrackStoreSignals.notifyChanged()
            pendingTrack = null
        } else {
            pendingTrack = loaded
        }
    }

    LaunchedEffect(pendingTrack?.armedAtMillis) {
        val current = pendingTrack ?: return@LaunchedEffect
        val age = (System.currentTimeMillis() - current.armedAtMillis).coerceAtLeast(0L)
        if (age <= shareImportTrackMaxAgeMs) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            GitHubTrackStore.savePendingShareImportTrack(null)
        }
        GitHubTrackStoreSignals.notifyChanged()
        pendingTrack = null
    }
    LaunchedEffect(pendingTrack?.armedAtMillis, attachCandidate?.packageName) {
        val armedAtMillis = pendingTrack?.armedAtMillis ?: return@LaunchedEffect
        if (attachCandidate != null) return@LaunchedEffect
        while (true) {
            val currentPending = pendingTrack ?: return@LaunchedEffect
            if (currentPending.armedAtMillis != armedAtMillis) return@LaunchedEffect
            if (attachCandidate != null) return@LaunchedEffect

            val reconciled = withContext(Dispatchers.IO) {
                findRecentInstalledCandidateForPendingTrack(context, currentPending)
            }
            if (reconciled != null) {
                val duplicateId = "${currentPending.owner}/${currentPending.repo}|${reconciled.packageName}"
                val duplicateExists = withContext(Dispatchers.IO) {
                    GitHubTrackStore.load().any { it.id == duplicateId }
                }
                if (duplicateExists) {
                    withContext(Dispatchers.IO) {
                        GitHubTrackStore.savePendingShareImportTrack(null)
                    }
                    GitHubTrackStoreSignals.notifyChanged()
                    pendingTrack = null
                    toast(context, R.string.github_toast_share_import_track_exists)
                    return@LaunchedEffect
                }
                attachCandidate = GitHubPendingShareImportAttachCandidate(
                    projectUrl = currentPending.projectUrl,
                    owner = currentPending.owner,
                    repo = currentPending.repo,
                    packageName = reconciled.packageName,
                    appLabel = reconciled.appLabel.ifBlank { reconciled.packageName },
                    eventAction = "reconciled",
                    detectedAtMillis = System.currentTimeMillis(),
                    firstInstallTimeMs = reconciled.firstInstallTimeMs
                )
                withContext(Dispatchers.IO) {
                    GitHubTrackStore.savePendingShareImportTrack(null)
                }
                GitHubTrackStoreSignals.notifyChanged()
                pendingTrack = null
                return@LaunchedEffect
            }

            val pendingAge = (System.currentTimeMillis() - currentPending.armedAtMillis).coerceAtLeast(0L)
            if (pendingAge > shareImportTrackMaxAgeMs) return@LaunchedEffect
            delay(2500L)
        }
    }

    LaunchedEffect(incomingGitHubShareToken) {
        val sharedText = incomingGitHubShareText?.trim().orEmpty()
        if (sharedText.isBlank()) return@LaunchedEffect
        if (resolving || incomingResolveRunning) return@LaunchedEffect
        incomingResolveRunning = true
        resolving = true
        onIncomingGitHubShareConsumed()
        pendingPreview = null
        try {
            val lookupConfig = withContext(Dispatchers.IO) { GitHubTrackStore.loadLookupConfig() }
            if (!lookupConfig.shareImportLinkageEnabled) return@LaunchedEffect
            try {
                val plan = withContext(Dispatchers.IO) {
                    GitHubShareImportResolver.resolve(
                        sharedText = sharedText,
                        lookupConfig = lookupConfig
                    ).getOrThrow()
                }
                if (plan.assets.isEmpty()) {
                    toast(context, R.string.github_toast_share_import_no_apk)
                } else {
                    pendingPreview = GitHubShareImportPreview(
                        sourceUrl = plan.parsedLink.sourceUrl,
                        projectUrl = plan.parsedLink.projectUrl,
                        owner = plan.parsedLink.owner,
                        repo = plan.parsedLink.repo,
                        releaseTag = plan.resolvedReleaseTag,
                        releaseUrl = plan.resolvedReleaseUrl,
                        strategyLabel = lookupConfig.selectedStrategy.label,
                        assets = plan.assets,
                        preferredAssetName = plan.preferredAssetName
                    )
                }
            } catch (error: Throwable) {
                if (error.shouldSuppressShareImportFailureToast()) return@LaunchedEffect
                val reason = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
                toast(context, R.string.github_toast_share_import_failed, reason)
            }
        } finally {
            resolving = false
            incomingResolveRunning = false
        }
    }

    LaunchedEffect(pendingTrack?.armedAtMillis) {
        val armedAtMillis = pendingTrack?.armedAtMillis ?: return@LaunchedEffect
        AppPackageChangedEvents.events.collect { event ->
            val packageName = event.packageName.trim()
            if (packageName.isBlank()) return@collect
            val currentPending = pendingTrack ?: return@collect
            if (currentPending.armedAtMillis != armedAtMillis) return@collect
            val pendingAge = (event.atMillis - currentPending.armedAtMillis).coerceAtLeast(0L)
            if (pendingAge > shareImportTrackMaxAgeMs) {
                withContext(Dispatchers.IO) {
                    GitHubTrackStore.savePendingShareImportTrack(null)
                }
                GitHubTrackStoreSignals.notifyChanged()
                pendingTrack = null
                return@collect
            }
            if (event.action !in shareImportAttachActions) return@collect

            val lastHandledAt = handledAtByPackage[packageName] ?: 0L
            if ((event.atMillis - lastHandledAt).coerceAtLeast(0L) < shareImportMinHandleIntervalMs) {
                return@collect
            }
            handledAtByPackage[packageName] = event.atMillis

            val packageSnapshot = withContext(Dispatchers.IO) {
                loadInstalledPackageSnapshot(context, packageName)
            } ?: return@collect

            if (
                !isShareImportAttachEventValid(
                    event = event,
                    armedAtMillis = currentPending.armedAtMillis,
                    packageLastUpdateTimeMs = packageSnapshot.lastUpdateTimeMs
                )
            ) {
                return@collect
            }

            val duplicateId = "${currentPending.owner}/${currentPending.repo}|$packageName"
            val duplicateExists = withContext(Dispatchers.IO) {
                GitHubTrackStore.load().any { it.id == duplicateId }
            }
            if (duplicateExists) {
                withContext(Dispatchers.IO) {
                    GitHubTrackStore.savePendingShareImportTrack(null)
                }
                GitHubTrackStoreSignals.notifyChanged()
                pendingTrack = null
                toast(context, R.string.github_toast_share_import_track_exists)
                return@collect
            }

            val currentCandidate = attachCandidate
            if (
                currentCandidate != null &&
                currentCandidate.packageName == packageName &&
                currentCandidate.owner == currentPending.owner &&
                currentCandidate.repo == currentPending.repo
            ) {
                return@collect
            }

            attachCandidate = GitHubPendingShareImportAttachCandidate(
                projectUrl = currentPending.projectUrl,
                owner = currentPending.owner,
                repo = currentPending.repo,
                packageName = packageName,
                appLabel = packageSnapshot.appLabel.ifBlank { packageName },
                eventAction = event.action,
                detectedAtMillis = event.atMillis,
                firstInstallTimeMs = packageSnapshot.firstInstallTimeMs
            )
            withContext(Dispatchers.IO) {
                GitHubTrackStore.savePendingShareImportTrack(null)
            }
            GitHubTrackStoreSignals.notifyChanged()
            pendingTrack = null
        }
    }

    LaunchedEffect(attachCandidate) {
        val candidate = attachCandidate
        if (candidate == null) {
            attachDuplicateExists = false
            attachSubmitting = false
            return@LaunchedEffect
        }
        val candidateId = "${candidate.owner}/${candidate.repo}|${candidate.packageName}"
        attachDuplicateExists = withContext(Dispatchers.IO) {
            GitHubTrackStore.load().any { it.id == candidateId }
        }
    }
    LaunchedEffect(
        resolving,
        incomingResolveRunning,
        pendingPreview,
        pendingTrack?.armedAtMillis,
        attachCandidate,
        incomingGitHubShareText,
        onIdleWithNoPendingFlow
    ) {
        val onIdle = onIdleWithNoPendingFlow ?: return@LaunchedEffect
        val hasIncomingShareText = !incomingGitHubShareText.isNullOrBlank()
        val hasActiveFlow = resolving ||
            incomingResolveRunning ||
            pendingPreview != null ||
            pendingTrack != null ||
            attachCandidate != null
        if (hasIncomingShareText || hasActiveFlow) {
            idleCallbackDispatched = false
            return@LaunchedEffect
        }
        if (idleCallbackDispatched) return@LaunchedEffect
        idleCallbackDispatched = true
        onIdle()
    }

    GitHubShareImportDialog(
        preview = pendingPreview,
        resolving = resolving,
        onDismissRequest = {
            if (!resolving) pendingPreview = null
        },
        onCancel = { pendingPreview = null },
        onConfirmImport = { selectedAsset ->
            scope.launch {
                val preview = pendingPreview ?: return@launch
                val lookupConfig = withContext(Dispatchers.IO) { GitHubTrackStore.loadLookupConfig() }
                val deliveryResult = sendAssetToConfiguredChannel(
                    context = context,
                    lookupConfig = lookupConfig,
                    asset = selectedAsset
                )
                when (deliveryResult) {
                    is ShareImportDeliveryResult.Failure -> {
                        toast(context, deliveryResult.toastResId)
                        return@launch
                    }
                    is ShareImportDeliveryResult.Success -> {
                        toast(context, deliveryResult.toastResId)
                    }
                }

                val pending = GitHubPendingShareImportTrackRecord(
                    projectUrl = preview.projectUrl,
                    owner = preview.owner,
                    repo = preview.repo,
                    releaseTag = preview.releaseTag,
                    assetName = selectedAsset.name,
                    armedAtMillis = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    GitHubTrackStore.savePendingShareImportTrack(pending)
                }
                GitHubTrackStoreSignals.notifyChanged()
                pendingTrack = pending
                attachCandidate = null
                pendingPreview = null
                toast(context, R.string.github_toast_share_import_wait_install, selectedAsset.name)
            }
        }
    )
    GitHubShareImportPendingDialog(
        pending = if (
            showPendingArmedSheet &&
            pendingTrack != null &&
            pendingPreview == null &&
            !resolving &&
            attachCandidate == null
        ) {
            pendingTrack
        } else {
            null
        },
        onDismissRequest = {},
        onClose = {
            onClosePendingArmedSheet?.invoke()
        },
        onCancel = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    GitHubTrackStore.savePendingShareImportTrack(null)
                }
                GitHubTrackStoreSignals.notifyChanged()
                pendingTrack = null
                toast(context, R.string.github_toast_share_import_pending_cancelled)
            }
        }
    )

    GitHubShareImportAttachConfirmDialog(
        candidate = attachCandidate,
        duplicateExists = attachDuplicateExists,
        submitting = attachSubmitting,
        onDismissRequest = {
            if (!attachSubmitting) attachCandidate = null
        },
        onCancel = {
            if (!attachSubmitting) attachCandidate = null
        },
        onConfirm = {
            if (attachSubmitting) return@GitHubShareImportAttachConfirmDialog
            val candidate = attachCandidate ?: return@GitHubShareImportAttachConfirmDialog
            attachSubmitting = true
            scope.launch {
                try {
                    when (val result = attachCandidateToTracked(context, candidate)) {
                        ShareImportAttachResult.Duplicate -> {
                            toast(context, R.string.github_toast_share_import_track_exists)
                            attachCandidate = null
                        }
                        is ShareImportAttachResult.Failed -> {
                            toast(context, R.string.github_toast_share_import_failed, result.message)
                        }
                        is ShareImportAttachResult.Added -> {
                            toast(context, R.string.github_toast_share_import_track_added, result.appLabel)
                            attachCandidate = null
                        }
                    }
                } finally {
                    attachSubmitting = false
                }
            }
        },
        onConfirmAndOpenGitHub = {
            if (attachSubmitting) return@GitHubShareImportAttachConfirmDialog
            val candidate = attachCandidate ?: return@GitHubShareImportAttachConfirmDialog
            attachSubmitting = true
            scope.launch {
                try {
                    when (val result = attachCandidateToTracked(context, candidate)) {
                        ShareImportAttachResult.Duplicate -> {
                            toast(context, R.string.github_toast_share_import_track_exists)
                            attachCandidate = null
                        }
                        is ShareImportAttachResult.Failed -> {
                            toast(context, R.string.github_toast_share_import_failed, result.message)
                        }
                        is ShareImportAttachResult.Added -> {
                            toast(context, R.string.github_toast_share_import_track_added, result.appLabel)
                            attachCandidate = null
                            runCatching {
                                onNavigateToGitHubPage()
                            }.onFailure {
                                toast(context, R.string.common_open_link_failed)
                            }
                        }
                    }
                } finally {
                    attachSubmitting = false
                }
            }
        }
    )
}

private suspend fun sendAssetToConfiguredChannel(
    context: Context,
    lookupConfig: GitHubLookupConfig,
    asset: GitHubReleaseAssetFile
): ShareImportDeliveryResult {
    val resolvedUrl = resolvePreferredAssetUrl(lookupConfig, asset)
    val onlineSharePackage = lookupConfig.onlineShareTargetPackage.trim()
    if (onlineSharePackage.isNotBlank()) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, asset.name)
            putExtra(Intent.EXTRA_TEXT, resolvedUrl)
            `package` = onlineSharePackage
            putExtra("channel", "Online")
            putExtra("extra_channel", "Online")
            putExtra("online_channel", true)
        }
        return runCatching {
            context.startActivity(intent)
            ShareImportDeliveryResult.Success(R.string.github_toast_downloader_selected)
        }.getOrElse {
            ShareImportDeliveryResult.Failure(R.string.github_toast_share_link_failed)
        }
    }

    val preferredPackage = lookupConfig.preferredDownloaderPackage.trim()
    val systemDmPackage = systemDownloadManagerOption(context).packageName
    if (preferredPackage == systemDmPackage) {
        return runCatching {
            enqueueWithSystemDownloadManager(context, resolvedUrl, asset.name)
            ShareImportDeliveryResult.Success(R.string.github_toast_downloader_system_builtin)
        }.getOrElse {
            ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
        }
    }
    if (preferredPackage.isBlank()) {
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            )
            ShareImportDeliveryResult.Success(R.string.github_toast_downloader_system_default)
        }.getOrElse {
            ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
        }
    }

    return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(preferredPackage)
            }
        )
        ShareImportDeliveryResult.Success(R.string.github_toast_downloader_selected)
    }.recoverCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        )
        ShareImportDeliveryResult.Success(R.string.github_toast_downloader_fallback_system)
    }.getOrElse {
        ShareImportDeliveryResult.Failure(R.string.github_toast_open_downloader_failed)
    }
}

private suspend fun resolvePreferredAssetUrl(
    lookupConfig: GitHubLookupConfig,
    asset: GitHubReleaseAssetFile
): String {
    val token = lookupConfig.apiToken.trim()
    val preferApiAsset = lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken
    return withContext(Dispatchers.IO) {
        GitHubReleaseAssetRepository.resolvePreferredDownloadUrl(
            asset = asset,
            useApiAssetUrl = preferApiAsset,
            apiToken = token
        ).getOrElse { asset.downloadUrl }
    }
}

private fun enqueueWithSystemDownloadManager(
    context: Context,
    url: String,
    fileName: String
) {
    val request = DownloadManager.Request(url.toUri()).apply {
        setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
        )
        setTitle(fileName)
        setDescription(fileName)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    }
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        ?: throw IllegalStateException("download manager unavailable")
    manager.enqueue(request)
}

private suspend fun attachCandidateToTracked(
    context: Context,
    candidate: GitHubPendingShareImportAttachCandidate
): ShareImportAttachResult {
    return withContext(Dispatchers.IO) {
        val trackedItems = GitHubTrackStore.load().toMutableList()
        val candidateId = "${candidate.owner}/${candidate.repo}|${candidate.packageName}"
        if (trackedItems.any { it.id == candidateId }) {
            return@withContext ShareImportAttachResult.Duplicate
        }

        val trackedItem = GitHubTrackedApp(
            repoUrl = candidate.projectUrl,
            owner = candidate.owner,
            repo = candidate.repo,
            packageName = candidate.packageName,
            appLabel = candidate.appLabel.ifBlank { candidate.packageName }
        )
        trackedItems.add(trackedItem)
        GitHubTrackStore.save(trackedItems)
        saveTrackedFirstInstallAtFallback(candidate)
        AppBackgroundScheduler.scheduleGitHubRefresh(context)

        runCatching {
            val refreshedUi = GitHubReleaseCheckService.evaluateTrackedApp(context, trackedItem).toUi()
            val (cache, _) = GitHubTrackStore.loadCheckCache()
            val updatedCache = cache.toMutableMap().apply {
                put(trackedItem.id, refreshedUi.toCacheEntry())
            }
            GitHubTrackStore.saveCheckCache(updatedCache, System.currentTimeMillis())
        }
        GitHubTrackStoreSignals.notifyChanged()

        ShareImportAttachResult.Added(trackedItem.appLabel.ifBlank { trackedItem.packageName })
    }
}

private fun saveTrackedFirstInstallAtFallback(candidate: GitHubPendingShareImportAttachCandidate) {
    val packageName = candidate.packageName.trim()
    if (packageName.isBlank()) return
    val firstInstallAtMillis = candidate.firstInstallTimeMs
        .takeIf { it > 0L }
        ?: candidate.detectedAtMillis
    if (firstInstallAtMillis <= 0L) return

    val existing = GitHubTrackStore.loadTrackedFirstInstallAtByPackage().toMutableMap()
    val current = existing[packageName]
    if (current == null || current <= 0L || firstInstallAtMillis < current) {
        existing[packageName] = firstInstallAtMillis
        GitHubTrackStore.saveTrackedFirstInstallAtByPackage(existing)
    }
}

private suspend fun loadInstalledPackageSnapshot(
    context: Context,
    packageName: String
): OverlayInstalledPackageSnapshot? {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info = runCatching {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }.recoverCatching {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }.getOrNull() ?: return@withContext null

        val label = runCatching {
            val appInfo = info.applicationInfo ?: pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
            pm.getApplicationLabel(appInfo).toString().trim()
        }.recoverCatching {
            @Suppress("DEPRECATION")
            val appInfo = info.applicationInfo ?: pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().trim()
        }.getOrDefault("")

        OverlayInstalledPackageSnapshot(
            packageName = packageName,
            appLabel = label,
            lastUpdateTimeMs = info.lastUpdateTime,
            firstInstallTimeMs = info.firstInstallTime
        )
    }
}

private fun findRecentInstalledCandidateForPendingTrack(
    context: Context,
    pendingTrack: GitHubPendingShareImportTrackRecord
): OverlayInstalledPackageSnapshot? {
    val pm = context.packageManager
    val installed = runCatching {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
    }.recoverCatching {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(0)
    }.getOrDefault(emptyList())

    val candidates = installed.asSequence()
        .filter { info ->
            val packageName = info.packageName.trim()
            packageName.isNotBlank() &&
                packageName != context.packageName &&
                pm.getLaunchIntentForPackage(packageName) != null
        }
        .mapNotNull { info ->
            val packageName = info.packageName.trim()
            val updateTime = info.lastUpdateTime
            if (updateTime <= 0L) return@mapNotNull null
            if (updateTime < (pendingTrack.armedAtMillis - shareImportTrackUpdateToleranceMs)) {
                return@mapNotNull null
            }
            val appLabel = runCatching {
                val appInfo = info.applicationInfo ?: pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
                pm.getApplicationLabel(appInfo).toString().trim()
            }.recoverCatching {
                @Suppress("DEPRECATION")
                val appInfo = info.applicationInfo ?: pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString().trim()
            }.getOrDefault("")

            OverlayInstalledPackageSnapshot(
                packageName = packageName,
                appLabel = appLabel,
                lastUpdateTimeMs = updateTime,
                firstInstallTimeMs = info.firstInstallTime
            )
        }
        .sortedByDescending { it.lastUpdateTimeMs }
        .take(3)
        .toList()

    if (candidates.isEmpty()) return null
    if (candidates.size >= 2) {
        val first = candidates[0]
        val second = candidates[1]
        val updateGap = (first.lastUpdateTimeMs - second.lastUpdateTimeMs).coerceAtLeast(0L)
        if (updateGap < 90_000L) {
            return null
        }
    }
    return candidates.first()
}

private fun isShareImportAttachEventValid(
    event: AppPackageChangedEvent,
    armedAtMillis: Long,
    packageLastUpdateTimeMs: Long
): Boolean {
    if (event.action == Intent.ACTION_PACKAGE_ADDED || event.action == Intent.ACTION_PACKAGE_REPLACED) {
        return true
    }
    if (event.action != Intent.ACTION_PACKAGE_CHANGED) return false
    if (packageLastUpdateTimeMs <= 0L) return false
    return packageLastUpdateTimeMs >= (armedAtMillis - shareImportTrackUpdateToleranceMs)
}

private fun toast(
    context: Context,
    resId: Int,
    vararg args: Any
) {
    Toast.makeText(context, context.getString(resId, *args), Toast.LENGTH_SHORT).show()
}

private fun Throwable.shouldSuppressShareImportFailureToast(): Boolean {
    if (this is CancellationException) return true
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < 6) {
        val message = current.message.orEmpty()
        val className = current.javaClass.name
        if (
            message.contains("left the composition", ignoreCase = true) ||
            className.contains("LeftComposition", ignoreCase = true)
        ) {
            return true
        }
        current = current.cause
        depth += 1
    }
    return false
}
