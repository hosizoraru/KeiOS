package com.example.keios.ui.page.main

import android.content.Context
import android.content.Intent
import com.example.keios.core.system.AppPackageChangedEvent
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.query.DownloaderOption
import com.example.keios.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.CoroutineScope

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

    private val minHandleIntervalMs = 1200L
    private val handledAtByPackage = mutableMapOf<String, Long>()
    private val packageUpdateActions = setOf(
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

    fun refreshAllTracked(showToast: Boolean = true) =
        refreshActions.refreshAllTracked(showToast = showToast)

    fun runStrategyBenchmark() = configActions.runStrategyBenchmark()

    fun runCredentialCheck() = configActions.runCredentialCheck()

    fun handleInstalledOnlineShareTargetsChanged(
        installedOnlineShareTargets: List<OnlineShareTargetOption>
    ) = configActions.handleInstalledOnlineShareTargetsChanged(installedOnlineShareTargets)

    fun applyLookupConfig() = configActions.applyLookupConfig()

    fun applyCheckLogicSheet(installedOnlineShareTargets: List<OnlineShareTargetOption>) =
        configActions.applyCheckLogicSheet(installedOnlineShareTargets)

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
            val previousState = env.state.checkStates[item.id] ?: VersionCheckUi()
            assetActions.clearApkAssetUiState(item.id)
            assetActions.clearApkAssetCache(item, previousState)
            refreshActions.refreshItem(item = item, showToastOnError = false) { updatedState ->
                val canLoadApkAssets = item.alwaysShowLatestReleaseDownloadButton ||
                    updatedState.hasUpdate == true ||
                    updatedState.recommendsPreRelease ||
                    updatedState.hasPreReleaseUpdate
                if (wasAssetExpanded && canLoadApkAssets) {
                    assetActions.loadApkAssets(
                        item = item,
                        itemState = updatedState,
                        toggleOnlyWhenCached = false,
                        includeAllAssets = false
                    )
                }
            }
        }
    }
}
