package com.example.keios.ui.page.main

import android.content.Context
import android.content.Intent
import com.example.keios.R
import com.example.keios.core.system.AppPackageChangedEvent
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.query.DownloaderOption
import com.example.keios.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GitHubPageActions(
    context: Context,
    scope: CoroutineScope,
    state: GitHubPageState,
    systemDmOption: DownloaderOption,
    openLinkFailureMessage: String
) {
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
    private val handledAtByPackage = mutableMapOf<String, Long>()
    private val packageUpdateActions = setOf(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_CHANGED
    )
    private val packageInstallActions = setOf(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REPLACED
    )

    fun openStrategySheet() = configActions.openStrategySheet()

    fun closeStrategySheet() = configActions.closeStrategySheet()

    fun openCheckLogicSheet() = configActions.openCheckLogicSheet()

    fun closeCheckLogicSheet() = configActions.closeCheckLogicSheet()

    suspend fun reloadApps(forceRefresh: Boolean = false) =
        refreshActions.reloadApps(forceRefresh = forceRefresh)

    suspend fun initializePage() = refreshActions.initializePage()

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
        val candidateId = "${pending.owner}/${pending.repo}|$packageName"
        if (env.state.trackedItems.any { it.id == candidateId }) {
            env.state.pendingShareImportTrack = null
            return
        }
        val appLabel = withContext(Dispatchers.IO) {
            GitHubVersionUtils.queryInstalledLaunchableApps(
                context = env.context,
                forceRefresh = true
            ).firstOrNull { app ->
                app.packageName == packageName
            }?.label
        }.orEmpty().ifBlank { packageName }

        env.state.trackedItems.add(
            GitHubTrackedApp(
                repoUrl = pending.projectUrl,
                owner = pending.owner,
                repo = pending.repo,
                packageName = packageName,
                appLabel = appLabel
            )
        )
        env.saveTrackedItems()
        env.state.pendingShareImportTrack = null
        env.toast(
            R.string.github_toast_share_import_track_added,
            appLabel
        )
    }

    private fun clearExpiredPendingShareImportTrack(nowMillis: Long) {
        val pending = env.state.pendingShareImportTrack ?: return
        val age = (nowMillis - pending.armedAtMillis).coerceAtLeast(0L)
        if (age <= pendingShareImportTrackMaxAgeMs) return
        env.state.pendingShareImportTrack = null
    }
}
