package com.example.keios.ui.page.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.keios.R
import com.example.keios.core.system.AppPackageChangedEvent
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.query.DownloaderOption
import com.example.keios.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GitHubPageActions(
    context: Context,
    scope: CoroutineScope,
    state: GitHubPageState,
    systemDmOption: DownloaderOption,
    openLinkFailureMessage: String
) {
    private data class InstalledPackageSnapshot(
        val appLabel: String,
        val lastUpdateTimeMs: Long,
        val firstInstallTimeMs: Long
    )

    private val env = GitHubPageActionEnvironment(
        context = context,
        scope = scope,
        state = state,
        systemDmOption = systemDmOption,
        openLinkFailureMessage = openLinkFailureMessage
    )
    private val refreshActions = GitHubRefreshActions(env)
    private val assetActions = GitHubAssetActions(env)
    private val configActions = GitHubConfigActions(env, refreshActions)
    private val trackActions = GitHubTrackActions(env, refreshActions)
    private val shareImportActions = GitHubShareImportActions(env, assetActions)

    private val minHandleIntervalMs = 1200L
    private val pendingShareImportTrackMaxAgeMs = 25 * 60 * 1000L
    private val pendingShareImportUpdateToleranceMs = 2 * 60 * 1000L
    private val handledAtByPackage = mutableMapOf<String, Long>()
    private val packageUpdateActions = setOf(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_CHANGED
    )
    private val packageInstallActions = setOf(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_CHANGED
    )

    fun openStrategySheet() = configActions.openStrategySheet()

    fun closeStrategySheet() = configActions.closeStrategySheet()

    fun openCheckLogicSheet() = configActions.openCheckLogicSheet()

    fun closeCheckLogicSheet() = configActions.closeCheckLogicSheet()

    suspend fun reloadApps(forceRefresh: Boolean = false) =
        refreshActions.reloadApps(forceRefresh = forceRefresh)

    suspend fun initializePage() = refreshActions.initializePage()

    suspend fun syncTrackSnapshotFromStore(forceRefreshApps: Boolean = true) =
        refreshActions.syncSnapshotFromStore(forceRefreshApps)

    fun refreshAllTracked(showToast: Boolean = true) =
        refreshActions.refreshAllTracked(showToast = showToast) {
            val expandedItemIds = env.state.apkAssetExpanded
                .filterValues { it }
                .keys
                .toSet()
            if (expandedItemIds.isEmpty()) return@refreshAllTracked

            env.state.trackedItems.forEach { item ->
                if (item.id !in expandedItemIds) return@forEach
                val itemState = env.state.checkStates[item.id] ?: return@forEach
                val includeAllAssets = env.state.apkAssetIncludeAll[item.id] == true
                if (canLoadApkAssets(item, itemState)) {
                    assetActions.clearApkAssetCache(item, itemState)
                    assetActions.loadApkAssets(
                        item = item,
                        itemState = itemState,
                        toggleOnlyWhenCached = false,
                        includeAllAssets = includeAllAssets
                    )
                } else {
                    assetActions.clearApkAssetUiState(item.id)
                }
            }
        }

    fun runStrategyBenchmark() = configActions.runStrategyBenchmark()

    fun runCredentialCheck() = configActions.runCredentialCheck()

    fun handleInstalledOnlineShareTargetsChanged(
        installedOnlineShareTargets: List<OnlineShareTargetOption>
    ) = configActions.handleInstalledOnlineShareTargetsChanged(installedOnlineShareTargets)

    fun applyLookupConfig() = configActions.applyLookupConfig()

    fun applyCheckLogicSheet(installedOnlineShareTargets: List<OnlineShareTargetOption>) =
        configActions.applyCheckLogicSheet(installedOnlineShareTargets)

    fun buildTrackedItemsExportJson(
        exportedAtMillis: Long = System.currentTimeMillis()
    ) = configActions.buildTrackedItemsExportJson(exportedAtMillis)

    fun previewTrackedItemsImport(raw: String) = configActions.previewTrackedItemsImport(raw)

    fun applyTrackedItemsImport(preview: GitHubTrackImportPreview) =
        configActions.applyTrackedItemsImport(preview)

    fun importTrackedItemsJson(raw: String) = configActions.importTrackedItemsJson(raw)

    fun handleIncomingGitHubShareText(sharedText: String) =
        shareImportActions.handleIncomingGitHubShareText(sharedText)

    fun dismissShareImportDialog() = shareImportActions.dismissShareImportDialog()

    fun confirmShareImportSelection(asset: GitHubReleaseAssetFile) =
        shareImportActions.confirmShareImportSelection(asset)

    fun cancelPendingShareImportTrack(showToast: Boolean = true) {
        val hadPending = env.state.pendingShareImportTrack != null
        clearPendingShareImportTrack()
        if (hadPending && showToast) {
            env.toast(R.string.github_toast_share_import_pending_cancelled)
        }
    }

    fun dismissPendingShareImportAttachCandidate() {
        env.state.pendingShareImportAttachCandidate = null
    }

    fun confirmPendingShareImportAttachCandidate() {
        val candidate = env.state.pendingShareImportAttachCandidate ?: return
        val candidateId = "${candidate.owner}/${candidate.repo}|${candidate.packageName}"
        if (env.state.trackedItems.any { it.id == candidateId }) {
            env.state.pendingShareImportAttachCandidate = null
            env.toast(R.string.github_toast_share_import_track_exists)
            return
        }
        val trackedItem = GitHubTrackedApp(
            repoUrl = candidate.projectUrl,
            owner = candidate.owner,
            repo = candidate.repo,
            packageName = candidate.packageName,
            appLabel = candidate.appLabel.ifBlank { candidate.packageName }
        )
        env.state.recordTrackedFirstInstallAt(
            packageName = candidate.packageName,
            firstInstallAtMillis = candidate.firstInstallTimeMs
                .takeIf { it > 0L }
                ?: candidate.detectedAtMillis
        )
        env.state.trackedItems.add(trackedItem)
        env.saveTrackedItems()
        env.state.pendingShareImportAttachCandidate = null
        env.scope.launch {
            runCatching { refreshActions.reloadApps(forceRefresh = true) }
            refreshActions.refreshItem(trackedItem, showToastOnError = true)
        }
        env.toast(
            R.string.github_toast_share_import_track_added,
            candidate.appLabel.ifBlank { candidate.packageName }
        )
    }

    fun trimExpiredPendingShareImportTrack(nowMillis: Long = System.currentTimeMillis()) {
        clearExpiredPendingShareImportTrack(nowMillis)
    }

    fun openExternalUrl(url: String, failureMessage: String = env.openLinkFailureMessage) =
        assetActions.openExternalUrl(url = url, failureMessage = failureMessage)

    fun shareApkLink(asset: GitHubReleaseAssetFile) = assetActions.shareApkLink(asset)

    fun openApkInDownloader(asset: GitHubReleaseAssetFile) = assetActions.openApkInDownloader(asset)

    fun clearApkAssetUiState(itemId: String) = assetActions.clearApkAssetUiState(itemId)

    fun clearApkAssetCache(item: GitHubTrackedApp, itemState: VersionCheckUi) =
        assetActions.clearApkAssetCache(item, itemState)

    fun loadApkAssets(
        item: GitHubTrackedApp,
        itemState: VersionCheckUi,
        toggleOnlyWhenCached: Boolean = true,
        includeAllAssets: Boolean = false
    ) = assetActions.loadApkAssets(
        item = item,
        itemState = itemState,
        toggleOnlyWhenCached = toggleOnlyWhenCached,
        includeAllAssets = includeAllAssets
    )

    fun openTrackSheetForAdd() = trackActions.openTrackSheetForAdd()

    fun openTrackSheetForEdit(item: GitHubTrackedApp) = trackActions.openTrackSheetForEdit(item)

    fun dismissTrackSheet() = trackActions.dismissTrackSheet()

    fun requestDeleteEditingItem() = trackActions.requestDeleteEditingItem()

    fun applyTrackSheet() = trackActions.applyTrackSheet()

    fun confirmDeletePendingItem() = trackActions.confirmDeletePendingItem()

    suspend fun handlePackageChangedEvent(event: AppPackageChangedEvent) {
        val packageName = event.packageName.trim()
        if (packageName.isBlank()) return
        clearExpiredPendingShareImportTrack(event.atMillis)
        attachPendingShareImportTrack(
            event = event,
            packageName = packageName
        )
        if (event.action !in packageUpdateActions) return

        val matchedItems = env.state.trackedItems.filter { it.packageName == packageName }
        if (matchedItems.isEmpty()) return

        val lastHandledAt = handledAtByPackage[packageName] ?: 0L
        if ((event.atMillis - lastHandledAt).coerceAtLeast(0L) < minHandleIntervalMs) {
            return
        }
        handledAtByPackage[packageName] = event.atMillis

        refreshActions.reloadApps(forceRefresh = true)
        matchedItems.forEach { item ->
            val wasAssetExpanded = env.state.apkAssetExpanded[item.id] == true
            val includeAllAssets = env.state.apkAssetIncludeAll[item.id] == true
            val previousState = env.state.checkStates[item.id] ?: VersionCheckUi()
            assetActions.clearApkAssetCache(item, previousState)
            refreshActions.refreshItem(item = item, showToastOnError = false) { updatedState ->
                if (wasAssetExpanded && canLoadApkAssets(item, updatedState)) {
                    assetActions.clearApkAssetCache(item, updatedState)
                    assetActions.loadApkAssets(
                        item = item,
                        itemState = updatedState,
                        toggleOnlyWhenCached = false,
                        includeAllAssets = includeAllAssets
                    )
                } else if (wasAssetExpanded) {
                    assetActions.clearApkAssetUiState(item.id)
                } else {
                    assetActions.clearApkAssetRuntimeState(item.id)
                }
            }
        }
    }

    private fun canLoadApkAssets(item: GitHubTrackedApp, itemState: VersionCheckUi): Boolean {
        return item.alwaysShowLatestReleaseDownloadButton ||
            itemState.hasUpdate == true ||
            itemState.recommendsPreRelease ||
            itemState.hasPreReleaseUpdate
    }

    private suspend fun attachPendingShareImportTrack(
        event: AppPackageChangedEvent,
        packageName: String
    ) {
        val pending = env.state.pendingShareImportTrack ?: return
        if (event.action !in packageInstallActions) return
        val packageSnapshot = loadInstalledPackageSnapshot(packageName) ?: return
        if (!isTrackAttachEventValid(event, pending.armedAtMillis, packageSnapshot.lastUpdateTimeMs)) {
            return
        }
        val candidateId = "${pending.owner}/${pending.repo}|$packageName"
        if (env.state.trackedItems.any { it.id == candidateId }) {
            clearPendingShareImportTrack()
            env.toast(R.string.github_toast_share_import_track_exists)
            return
        }
        val appLabel = packageSnapshot.appLabel.ifBlank { packageName }
        val currentCandidate = env.state.pendingShareImportAttachCandidate
        if (
            currentCandidate != null &&
            currentCandidate.packageName == packageName &&
            currentCandidate.owner == pending.owner &&
            currentCandidate.repo == pending.repo
        ) {
            return
        }
        env.state.pendingShareImportAttachCandidate = GitHubPendingShareImportAttachCandidate(
            projectUrl = pending.projectUrl,
            owner = pending.owner,
            repo = pending.repo,
            packageName = packageName,
            appLabel = appLabel,
            eventAction = event.action,
            detectedAtMillis = event.atMillis,
            firstInstallTimeMs = packageSnapshot.firstInstallTimeMs
        )
        clearPendingShareImportTrack()
    }

    private fun isTrackAttachEventValid(
        event: AppPackageChangedEvent,
        armedAtMillis: Long,
        packageLastUpdateTimeMs: Long
    ): Boolean {
        if (event.action == Intent.ACTION_PACKAGE_ADDED || event.action == Intent.ACTION_PACKAGE_REPLACED) {
            return true
        }
        if (event.action != Intent.ACTION_PACKAGE_CHANGED) return false
        if (packageLastUpdateTimeMs <= 0L) return false
        return packageLastUpdateTimeMs >= (armedAtMillis - pendingShareImportUpdateToleranceMs)
    }

    private suspend fun loadInstalledPackageSnapshot(packageName: String): InstalledPackageSnapshot? {
        return withContext(Dispatchers.IO) {
            val pm = env.context.packageManager
            val info = runCatching {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
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

            InstalledPackageSnapshot(
                appLabel = label,
                lastUpdateTimeMs = info.lastUpdateTime,
                firstInstallTimeMs = info.firstInstallTime
            )
        }
    }

    private fun clearExpiredPendingShareImportTrack(nowMillis: Long) {
        val pending = env.state.pendingShareImportTrack ?: return
        val age = (nowMillis - pending.armedAtMillis).coerceAtLeast(0L)
        if (age <= pendingShareImportTrackMaxAgeMs) return
        clearPendingShareImportTrack()
    }

    private fun clearPendingShareImportTrack() {
        env.state.pendingShareImportTrack = null
        GitHubTrackStore.savePendingShareImportTrack(null)
    }

}
