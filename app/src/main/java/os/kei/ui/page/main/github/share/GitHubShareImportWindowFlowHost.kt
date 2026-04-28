package os.kei.ui.page.main.github.share

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
import os.kei.R
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.system.AppPackageChangedEvent
import os.kei.core.system.AppPackageChangedEvents
import os.kei.feature.github.data.local.GitHubPendingShareImportTrackRecord
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.GitHubReleaseAssetRepository
import os.kei.feature.github.data.remote.GitHubShareImportResolver
import os.kei.feature.github.data.remote.GitHubShareIntentParser
import os.kei.feature.github.domain.GitHubReleaseCheckService
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.query.systemDownloadManagerOption
import os.kei.ui.page.main.github.state.toCacheEntry
import os.kei.ui.page.main.github.state.toUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GitHubShareImportWindowFlowHost(
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
    var attachSubmittingAndOpen by remember { mutableStateOf(false) }
    val handledAtByPackage = remember { mutableStateMapOf<String, Long>() }

    LaunchedEffect(Unit) {
        if (!incomingGitHubShareText.isNullOrBlank()) {
            return@LaunchedEffect
        }
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
                val parsedIncoming = GitHubShareIntentParser.parseSharedReleaseLink(sharedText)
                    ?: error("未识别到有效的 GitHub 链接")
                withContext(Dispatchers.IO) {
                    GitHubTrackStore.savePendingShareImportTrack(null)
                }
                GitHubTrackStoreSignals.notifyChanged()
                pendingTrack = null
                attachCandidate = null
                attachDuplicateExists = false
                handledAtByPackage.clear()
                val plan = withContext(Dispatchers.IO) {
                    GitHubShareImportResolver.resolve(
                        sharedText = parsedIncoming.sourceUrl,
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
            attachSubmittingAndOpen = false
            return@LaunchedEffect
        }
        val candidateId = "${candidate.owner}/${candidate.repo}|${candidate.packageName}"
        attachDuplicateExists = withContext(Dispatchers.IO) {
            GitHubTrackStore.load().any { it.id == candidateId }
        }
    }
    BindGitHubShareImportIdleCallback(
        resolving = resolving,
        incomingResolveRunning = incomingResolveRunning,
        pendingPreview = pendingPreview,
        pendingTrack = pendingTrack,
        attachCandidate = attachCandidate,
        incomingGitHubShareText = incomingGitHubShareText,
        onIdleWithNoPendingFlow = onIdleWithNoPendingFlow
    )

    GitHubShareImportSheet(
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
                handledAtByPackage.clear()
                pendingPreview = null
                toast(context, R.string.github_toast_share_import_wait_install, selectedAsset.name)
            }
        }
    )
    GitHubShareImportPendingSheet(
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

    GitHubShareImportAttachConfirmSheet(
        candidate = attachCandidate,
        duplicateExists = attachDuplicateExists,
        submitting = attachSubmitting,
        submittingAndOpen = attachSubmittingAndOpen,
        onDismissRequest = {
            if (!attachSubmitting) attachCandidate = null
        },
        onCancel = {
            if (!attachSubmitting) attachCandidate = null
        },
        onConfirm = {
            if (attachSubmitting) return@GitHubShareImportAttachConfirmSheet
            val candidate = attachCandidate ?: return@GitHubShareImportAttachConfirmSheet
            attachSubmitting = true
            attachSubmittingAndOpen = false
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
                    attachSubmittingAndOpen = false
                }
            }
        },
        onConfirmAndOpenGitHub = {
            if (attachSubmitting) return@GitHubShareImportAttachConfirmSheet
            val candidate = attachCandidate ?: return@GitHubShareImportAttachConfirmSheet
            attachSubmitting = true
            attachSubmittingAndOpen = true
            scope.launch {
                try {
                    when (
                        val result = attachCandidateToTracked(
                            context = context,
                            candidate = candidate,
                            prefetchLatestCheck = false
                        )
                    ) {
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
                    attachSubmittingAndOpen = false
                }
            }
        }
    )
}
