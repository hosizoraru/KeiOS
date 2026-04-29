package os.kei.ui.page.main.github.page

import android.content.Context
import androidx.compose.runtime.Composable
import os.kei.R
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.actions.GitHubActionsSheet
import os.kei.ui.page.main.github.sheet.GitHubCheckLogicSheet
import os.kei.ui.page.main.github.sheet.GitHubDeleteTrackDialog
import os.kei.ui.page.main.github.sheet.GitHubStrategySheet
import os.kei.ui.page.main.github.sheet.GitHubTrackEditSheet
import os.kei.ui.page.main.github.sheet.GitHubTrackImportDialog
import os.kei.ui.page.main.host.pager.MainPageBackdropSet

@Composable
internal fun GitHubPageSheetHost(
    context: Context,
    backdrops: MainPageBackdropSet,
    state: GitHubPageState,
    actions: GitHubPageActions,
    contentDerivedState: GitHubPageContentDerivedState,
    installedOnlineShareTargets: List<OnlineShareTargetOption>,
    checkLogicDownloaderOptions: List<DownloaderOption>,
    hasKeiOsSelfTrack: Boolean,
    tracksExporting: Boolean,
    tracksImporting: Boolean,
    onEnsureKeiOsSelfTrack: () -> Unit,
    onExportTrackedItems: () -> Unit,
    onImportTrackedItems: () -> Unit,
    onConfirmTrackImport: () -> Unit
) {
    GitHubStrategySheet(
        show = state.showStrategySheet,
        backdrop = backdrops.sheet,
        lookupConfig = state.lookupConfig,
        selectedStrategyInput = state.selectedStrategyInput,
        githubApiTokenInput = state.githubApiTokenInput,
        showApiTokenPlainText = state.showApiTokenPlainText,
        credentialCheckRunning = state.credentialCheckRunning,
        credentialCheckError = state.credentialCheckError,
        credentialCheckStatus = state.credentialCheckStatus,
        strategyBenchmarkRunning = state.strategyBenchmarkRunning,
        strategyBenchmarkError = state.strategyBenchmarkError,
        strategyBenchmarkReport = state.strategyBenchmarkReport,
        trackedCount = contentDerivedState.trackedUi.overviewMetrics.trackedCount,
        recommendedTokenGuideExpanded = state.recommendedTokenGuideExpanded,
        onDismissRequest = actions::closeStrategySheet,
        onApply = actions::applyLookupConfig,
        onSelectedStrategyChange = { state.selectedStrategyInput = it },
        onTokenInputChange = {
            state.githubApiTokenInput = it
            state.credentialCheckError = null
            state.credentialCheckStatus = null
        },
        onToggleTokenVisibility = { state.showApiTokenPlainText = !state.showApiTokenPlainText },
        onRunCredentialCheck = actions::runCredentialCheck,
        onRunStrategyBenchmark = actions::runStrategyBenchmark,
        onRecommendedTokenGuideExpandedChange = { state.recommendedTokenGuideExpanded = it },
        onOpenExternalUrl = { url, failureMessage ->
            actions.openExternalUrl(url = url, failureMessage = failureMessage)
        }
    )

    GitHubCheckLogicSheet(
        show = state.showCheckLogicSheet,
        backdrop = backdrops.sheet,
        lookupConfig = state.lookupConfig,
        trackedCount = contentDerivedState.trackedUi.overviewMetrics.trackedCount,
        refreshIntervalHours = state.refreshIntervalHours,
        refreshIntervalHoursInput = state.refreshIntervalHoursInput,
        checkAllTrackedPreReleasesInput = state.checkAllTrackedPreReleasesInput,
        aggressiveApkFilteringInput = state.aggressiveApkFilteringInput,
        shareImportLinkageEnabledInput = state.shareImportLinkageEnabledInput,
        onlineShareTargetPackageInput = state.onlineShareTargetPackageInput,
        preferredDownloaderPackageInput = state.preferredDownloaderPackageInput,
        installedOnlineShareTargets = installedOnlineShareTargets,
        showCheckLogicIntervalPopup = state.showCheckLogicIntervalPopup,
        showDownloaderPopup = state.showDownloaderPopup,
        showOnlineShareTargetPopup = state.showOnlineShareTargetPopup,
        checkLogicIntervalPopupAnchorBounds = state.checkLogicIntervalPopupAnchorBounds,
        downloaderPopupAnchorBounds = state.downloaderPopupAnchorBounds,
        onlineShareTargetPopupAnchorBounds = state.onlineShareTargetPopupAnchorBounds,
        downloaderOptions = checkLogicDownloaderOptions,
        hasKeiOsSelfTrack = hasKeiOsSelfTrack,
        exportInProgress = tracksExporting,
        importInProgress = tracksImporting,
        onDismissRequest = actions::closeCheckLogicSheet,
        onApply = { actions.applyCheckLogicSheet(installedOnlineShareTargets) },
        onEnsureKeiOsSelfTrack = onEnsureKeiOsSelfTrack,
        onExportTrackedItems = onExportTrackedItems,
        onImportTrackedItems = onImportTrackedItems,
        onRefreshIntervalHoursInputChange = { state.refreshIntervalHoursInput = it },
        onCheckAllTrackedPreReleasesInputChange = { state.checkAllTrackedPreReleasesInput = it },
        onAggressiveApkFilteringInputChange = { state.aggressiveApkFilteringInput = it },
        onShareImportLinkageEnabledInputChange = { state.shareImportLinkageEnabledInput = it },
        onPreferredDownloaderPackageInputChange = { state.preferredDownloaderPackageInput = it },
        onOnlineShareTargetPackageInputChange = { state.onlineShareTargetPackageInput = it },
        onShowCheckLogicIntervalPopupChange = { state.showCheckLogicIntervalPopup = it },
        onShowDownloaderPopupChange = { state.showDownloaderPopup = it },
        onShowOnlineShareTargetPopupChange = { state.showOnlineShareTargetPopup = it },
        onCheckLogicIntervalPopupAnchorBoundsChange = {
            state.checkLogicIntervalPopupAnchorBounds = it
        },
        onDownloaderPopupAnchorBoundsChange = { state.downloaderPopupAnchorBounds = it },
        onOnlineShareTargetPopupAnchorBoundsChange = {
            state.onlineShareTargetPopupAnchorBounds = it
        }
    )

    GitHubActionsSheet(
        show = state.showActionsSheet,
        backdrop = backdrops.sheet,
        state = state,
        onDismissRequest = actions::closeActionsSheet,
        onRefresh = actions::refreshActionsSheet,
        onSelectWorkflow = actions::selectActionsWorkflow,
        onSelectRun = actions::selectActionsRun,
        onLoadMoreRuns = actions::loadMoreActionsRuns,
        onWorkflowsExpandedChange = actions::setActionsWorkflowsExpanded,
        onRunsExpandedChange = actions::setActionsRunsExpanded,
        onArtifactsExpandedChange = { state.actionsArtifactsExpanded = it },
        onRefreshRun = actions::refreshActionsRunStatus,
        onDownloadArtifact = actions::downloadActionsArtifact,
        onShareArtifact = actions::shareActionsArtifact,
        onOpenRun = actions::openSelectedActionsRun
    )

    GitHubTrackEditSheet(
        show = state.showAddSheet,
        backdrop = backdrops.sheet,
        editingTrackedItem = state.editingTrackedItem,
        repoUrlInput = state.repoUrlInput,
        appSearch = state.appSearch,
        packageNameInput = state.packageNameInput,
        pickerExpanded = state.pickerExpanded,
        selectedApp = state.selectedApp,
        appList = state.appList,
        preferPreReleaseInput = state.preferPreReleaseInput,
        alwaysShowLatestReleaseDownloadButtonInput = state.alwaysShowLatestReleaseDownloadButtonInput,
        onDismissRequest = actions::dismissTrackSheet,
        onApply = actions::applyTrackSheet,
        onRepoUrlInputChange = { state.repoUrlInput = it },
        onAppSearchChange = { state.appSearch = it },
        onPackageNameInputChange = { input ->
            state.packageNameInput = input
            val selected = state.selectedApp
            val normalizedInput = input.trim()
            if (selected != null) {
                if (normalizedInput.isBlank()) {
                    state.selectedApp = null
                } else if (!selected.packageName.equals(normalizedInput, ignoreCase = true)) {
                    state.selectedApp = null
                }
            }
        },
        onPickerExpandedChange = { state.pickerExpanded = it },
        onSelectedAppChange = { app ->
            state.selectedApp = app
            if (app != null) {
                state.packageNameInput = app.packageName
            }
        },
        onPreferPreReleaseInputChange = { state.preferPreReleaseInput = it },
        onAlwaysShowLatestReleaseDownloadButtonInputChange = {
            state.alwaysShowLatestReleaseDownloadButtonInput = it
        },
        onRequestDelete = actions::requestDeleteEditingItem
    )

    GitHubDeleteTrackDialog(
        pendingDeleteItem = state.pendingDeleteItem,
        deleteInProgress = state.deleteInProgress,
        onDismissRequest = { state.pendingDeleteItem = null },
        onCancel = {
            if (!state.deleteInProgress) {
                state.pendingDeleteItem = null
            }
        },
        onConfirmDelete = actions::confirmDeletePendingItem
    )

    GitHubTrackImportDialog(
        preview = state.pendingTrackImportPreview,
        importInProgress = tracksImporting,
        onDismissRequest = {
            if (!tracksImporting) {
                state.dismissTrackImportPreview()
            }
        },
        onCancel = {
            if (!tracksImporting) {
                state.dismissTrackImportPreview()
            }
        },
        onConfirmImport = {
            val preview = state.pendingTrackImportPreview
            if (preview == null) return@GitHubTrackImportDialog
            if (!preview.canImport) {
                state.dismissTrackImportPreview()
                return@GitHubTrackImportDialog
            }
            if (tracksImporting) return@GitHubTrackImportDialog
            onConfirmTrackImport()
        }
    )

}
