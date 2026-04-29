package os.kei.ui.page.main.github.page

import android.content.Context
import android.content.Intent
import os.kei.R
import os.kei.core.system.AppPackageChangedEvent
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.page.action.GitHubAssetActions
import os.kei.ui.page.main.github.page.action.GitHubActionsActions
import os.kei.ui.page.main.github.page.action.GitHubConfigActions
import os.kei.ui.page.main.github.page.action.GitHubPageActionEnvironment
import os.kei.ui.page.main.github.page.action.GitHubRefreshActions
import os.kei.ui.page.main.github.page.action.GitHubTrackActions
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class GitHubPageActions(
    context: Context,
    scope: CoroutineScope,
    state: GitHubPageState,
    repository: GitHubPageRepository,
    systemDmOption: DownloaderOption,
    openLinkFailureMessage: String
) {
    private val env = GitHubPageActionEnvironment(
        context = context,
        scope = scope,
        state = state,
        repository = repository,
        systemDmOption = systemDmOption,
        openLinkFailureMessage = openLinkFailureMessage
    )
    private val refreshActions = GitHubRefreshActions(env)
    private val assetActions = GitHubAssetActions(env)
    private val actionsActions = GitHubActionsActions(env)
    private val configActions = GitHubConfigActions(env, refreshActions)
    private val trackActions = GitHubTrackActions(env, refreshActions)

    private val minHandleIntervalMs = 1200L
    private val pendingShareImportTrackMaxAgeMs = 25 * 60 * 1000L
    private val handledAtByPackage = mutableMapOf<String, Long>()
    private val packageUpdateActions = setOf(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REMOVED,
        Intent.ACTION_PACKAGE_FULLY_REMOVED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_CHANGED
    )

    fun openStrategySheet() = configActions.openStrategySheet()

    fun closeStrategySheet() = configActions.closeStrategySheet()

    fun openCheckLogicSheet() = configActions.openCheckLogicSheet()

    fun closeCheckLogicSheet() = configActions.closeCheckLogicSheet()

    fun openActionsSheet(item: GitHubTrackedApp) = actionsActions.openActionsSheet(item)

    fun closeActionsSheet() = actionsActions.closeActionsSheet()

    fun refreshActionsSheet() = actionsActions.refreshActionsSheet()

    fun selectActionsWorkflow(workflowId: Long) = actionsActions.selectActionsWorkflow(workflowId)

    fun selectActionsRun(runId: Long) = actionsActions.selectActionsRun(runId)

    fun loadMoreActionsRuns() = actionsActions.loadMoreActionsRuns()

    fun refreshActionsRunStatus(runId: Long) = actionsActions.refreshActionsRunStatus(runId)

    fun downloadActionsArtifact(runId: Long, artifactId: Long) =
        actionsActions.downloadActionsArtifact(runId = runId, artifactId = artifactId)

    fun shareActionsArtifact(runId: Long, artifactId: Long) =
        actionsActions.shareActionsArtifact(runId = runId, artifactId = artifactId)

    fun openSelectedActionsRun() = actionsActions.openSelectedActionsRun()

    suspend fun reloadApps(forceRefresh: Boolean = false) =
        refreshActions.reloadApps(forceRefresh = forceRefresh)

    suspend fun initializeWarmSnapshot() = refreshActions.initializeWarmSnapshot()

    suspend fun initializePageActiveWork() = refreshActions.initializePageActiveWork()

    suspend fun syncTrackSnapshotFromStore(forceRefreshApps: Boolean = true) =
        refreshActions.syncSnapshotFromStore(forceRefreshApps)

    suspend fun syncLocalAppStateOnPageActive() {
        refreshActions.syncLocalAppStateWithInstalledApps(forceRefreshApps = true)
    }

    fun refreshAllTracked(showToast: Boolean = true) {
        env.scope.launch {
            refreshActions.reloadApps(forceRefresh = true)
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
        }
    }

    fun refreshTrackedItem(
        item: GitHubTrackedApp,
        showToastOnError: Boolean = true
    ) {
        if (env.state.trackedItems.none { it.id == item.id }) return
        if (env.state.itemRefreshLoading[item.id] == true) return
        if (env.state.checkStates[item.id]?.loading == true) return
        env.scope.launch {
            env.state.itemRefreshLoading[item.id] = true
            try {
                refreshActions.reloadApps(forceRefresh = true)
                val wasAssetExpanded = env.state.apkAssetExpanded[item.id] == true
                val includeAllAssets = env.state.apkAssetIncludeAll[item.id] == true
                val previousState = env.state.checkStates[item.id] ?: VersionCheckUi()
                assetActions.clearApkAssetCache(item, previousState)
                refreshActions.refreshItemNow(
                    item = item,
                    showToastOnError = showToastOnError,
                    keepCurrentVisualWhileRefreshing = true
                ) { updatedState ->
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
            } finally {
                env.state.itemRefreshLoading.remove(item.id)
            }
        }
    }

    fun runStrategyBenchmark() = configActions.runStrategyBenchmark()

    fun runCredentialCheck() = configActions.runCredentialCheck()

    fun handleInstalledOnlineShareTargetsChanged(
        installedOnlineShareTargets: List<OnlineShareTargetOption>
    ) = configActions.handleInstalledOnlineShareTargetsChanged(installedOnlineShareTargets)

    fun currentTrackStoreSignalVersion(): Long = env.repository.currentTrackStoreSignalVersion()

    fun trackStoreSignalVersions() = env.repository.trackStoreSignalVersions()

    fun buildAppListPermissionIntent() = env.repository.buildAppListPermissionIntent(env.context)

    fun applyLookupConfig() = configActions.applyLookupConfig()

    fun applyCheckLogicSheet(installedOnlineShareTargets: List<OnlineShareTargetOption>) =
        configActions.applyCheckLogicSheet(installedOnlineShareTargets)

    suspend fun previewTrackedItemsImport(raw: String) = configActions.previewTrackedItemsImport(raw)

    fun applyTrackedItemsImport(preview: GitHubTrackImportPreview) =
        configActions.applyTrackedItemsImport(preview)

    suspend fun importTrackedItemsJson(raw: String) = configActions.importTrackedItemsJson(raw)

    fun cancelPendingShareImportTrack(showToast: Boolean = true) {
        val hadPending = env.state.pendingShareImportTrack != null
        clearPendingShareImportTrack()
        if (hadPending && showToast) {
            env.toast(R.string.github_toast_share_import_pending_cancelled)
        }
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

    fun ensureKeiOsSelfTrack() = trackActions.ensureKeiOsSelfTrack()

    fun openTrackSheetForEdit(item: GitHubTrackedApp) = trackActions.openTrackSheetForEdit(item)

    fun dismissTrackSheet() = trackActions.dismissTrackSheet()

    fun requestDeleteEditingItem() = trackActions.requestDeleteEditingItem()

    fun requestDeleteTrackedItem(item: GitHubTrackedApp) = trackActions.requestDeleteItem(item)

    fun applyTrackSheet() = trackActions.applyTrackSheet()

    fun confirmDeletePendingItem() = trackActions.confirmDeletePendingItem()

    suspend fun handlePackageChangedEvent(event: AppPackageChangedEvent) {
        val packageName = event.packageName.trim()
        if (packageName.isBlank()) return
        if (event.action !in packageUpdateActions) return
        if (event.replacing && event.action == Intent.ACTION_PACKAGE_REMOVED) return

        val matchedItems = env.state.trackedItems.filter { it.packageName == packageName }
        if (matchedItems.isEmpty()) return

        val lastHandledAt = handledAtByPackage[packageName] ?: 0L
        if ((event.atMillis - lastHandledAt).coerceAtLeast(0L) < minHandleIntervalMs) {
            return
        }
        handledAtByPackage[packageName] = event.atMillis

        refreshActions.reloadApps(forceRefresh = true)
        val uninstallAction = event.action == Intent.ACTION_PACKAGE_REMOVED ||
            event.action == Intent.ACTION_PACKAGE_FULLY_REMOVED
        matchedItems.forEach { item ->
            val wasAssetExpanded = env.state.apkAssetExpanded[item.id] == true
            val includeAllAssets = env.state.apkAssetIncludeAll[item.id] == true
            val previousState = env.state.checkStates[item.id] ?: VersionCheckUi()
            if (uninstallAction) {
                env.state.checkStates[item.id] = previousState.copy(
                    loading = true,
                    localVersion = "",
                    localVersionCode = -1L,
                    message = env.string(R.string.github_msg_checking)
                )
            }
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

    private fun clearExpiredPendingShareImportTrack(nowMillis: Long) {
        val pending = env.state.pendingShareImportTrack ?: return
        val age = (nowMillis - pending.armedAtMillis).coerceAtLeast(0L)
        if (age <= pendingShareImportTrackMaxAgeMs) return
        clearPendingShareImportTrack()
    }

    private fun clearPendingShareImportTrack() {
        env.state.pendingShareImportTrack = null
        env.scope.launch {
            env.repository.clearPendingShareImportTrack()
        }
    }

}
