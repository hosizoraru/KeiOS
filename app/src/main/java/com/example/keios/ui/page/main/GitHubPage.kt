package com.example.keios.ui.page.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.keios.R
import com.example.keios.ui.page.main.github.query.queryDownloaderOptions
import com.example.keios.ui.page.main.github.query.queryOnlineShareTargetOptions
import com.example.keios.ui.page.main.github.query.systemDownloadManagerOption
import com.example.keios.ui.page.main.github.section.GitHubMainContent
import com.example.keios.ui.page.main.github.section.GitHubOverviewMetrics
import com.example.keios.ui.page.main.github.sheet.GitHubCheckLogicSheet
import com.example.keios.ui.page.main.github.sheet.GitHubDeleteTrackDialog
import com.example.keios.ui.page.main.github.sheet.GitHubStrategySheet
import com.example.keios.ui.page.main.github.sheet.GitHubTrackEditSheet
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GitHubPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    enableSearchBar: Boolean = true,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val openLinkFailureMessage = context.getString(R.string.github_error_open_link)
    val systemDmOption = remember(context) { systemDownloadManagerOption(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surface

    val topBarBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val contentBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val sheetBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val topBarColor = rememberMiuixBlurBackdrop(enableBlur = true).getMiuixAppBarColor()

    val state = rememberGitHubPageState()
    val actions = remember(
        context,
        scope,
        state,
        systemDmOption,
        openLinkFailureMessage
    ) {
        GitHubPageActions(
            context = context,
            scope = scope,
            state = state,
            systemDmOption = systemDmOption,
            openLinkFailureMessage = openLinkFailureMessage
        )
    }
    val shouldResolveOnlineShareTargets by remember(state) {
        derivedStateOf {
            state.showCheckLogicSheet ||
                state.lookupConfig.onlineShareTargetPackage.isNotBlank() ||
                state.onlineShareTargetPackageInput.isNotBlank()
        }
    }
    val installedOnlineShareTargets = remember(
        shouldResolveOnlineShareTargets,
        state.appListLoaded,
        state.appList
    ) {
        if (shouldResolveOnlineShareTargets) {
            queryOnlineShareTargetOptions(context, state.appList)
        } else {
            emptyList()
        }
    }
    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { actions.reloadApps(forceRefresh = true) }
        }

    BindGitHubPageEffects(
        context = context,
        listState = listState,
        scrollToTopSignal = scrollToTopSignal,
        state = state,
        actions = actions,
        installedOnlineShareTargets = installedOnlineShareTargets,
        onLaunchAppListPermission = { intent -> appListPermissionLauncher.launch(intent) },
        onActionBarInteractingChanged = onActionBarInteractingChanged
    )

    val trackedUi by remember(state) {
        derivedStateOf {
            val filteredTracked = state.trackedItems.filter { item ->
                state.trackedSearch.isBlank() ||
                    item.owner.contains(state.trackedSearch, ignoreCase = true) ||
                    item.repo.contains(state.trackedSearch, ignoreCase = true) ||
                    item.appLabel.contains(state.trackedSearch, ignoreCase = true) ||
                    item.packageName.contains(state.trackedSearch, ignoreCase = true)
            }
            val isSortUpdatable: (com.example.keios.feature.github.model.GitHubTrackedApp) -> Boolean = { item ->
                item.alwaysShowLatestReleaseDownloadButton || state.checkStates[item.id]?.hasUpdate == true
            }
            val sortedTracked = when (state.sortMode) {
                GitHubSortMode.UpdateFirst -> filteredTracked.sortedWith(
                    compareByDescending<com.example.keios.feature.github.model.GitHubTrackedApp> {
                        isSortUpdatable(it)
                    }
                        .thenByDescending { state.checkStates[it.id]?.hasPreReleaseUpdate == true }
                        .thenBy { it.appLabel.lowercase() }
                )
                GitHubSortMode.NameAsc -> filteredTracked.sortedBy { it.appLabel.lowercase() }
                GitHubSortMode.PreReleaseFirst -> filteredTracked.sortedWith(
                    compareByDescending<com.example.keios.feature.github.model.GitHubTrackedApp> {
                        state.checkStates[it.id]?.isPreRelease == true
                    }
                        .thenByDescending { isSortUpdatable(it) }
                        .thenBy { it.appLabel.lowercase() }
                )
            }
            val trackedCount = state.trackedItems.size
            val updatableCount = state.trackedItems.count { state.checkStates[it.id]?.hasUpdate == true }
            val preReleaseCount = state.trackedItems.count { state.checkStates[it.id]?.isPreRelease == true }
            val preReleaseUpdateCount =
                state.trackedItems.count { state.checkStates[it.id]?.hasPreReleaseUpdate == true }
            val failedCount = state.trackedItems.count { state.checkStates[it.id]?.failed == true }
            val stableLatestCount = state.trackedItems.count {
                val itemState = state.checkStates[it.id]
                itemState?.hasUpdate == false && itemState.isPreRelease.not()
            }
            GitHubPageDerivedState(
                filteredTracked = filteredTracked,
                sortedTracked = sortedTracked,
                overviewMetrics = GitHubOverviewMetrics(
                    trackedCount = trackedCount,
                    updatableCount = updatableCount,
                    stableLatestCount = stableLatestCount,
                    preReleaseCount = preReleaseCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    failedCount = failedCount
                )
            )
        }
    }
    val appLastUpdatedAtByPackage = remember(state.appListLoaded, state.appList) {
        state.appList
            .filter { it.packageName.isNotBlank() && it.lastUpdateTimeMs > 0L }
            .associate { it.packageName to it.lastUpdateTimeMs }
    }
    val checkLogicDownloaderOptions = remember(state.showCheckLogicSheet) {
        if (state.showCheckLogicSheet) {
            queryDownloaderOptions(context)
        } else {
            emptyList()
        }
    }

    GitHubMainContent(
        contentBottomPadding = contentBottomPadding,
        listState = listState,
        scrollBehavior = scrollBehavior,
        addButtonScrollConnection = state.addButtonScrollConnection,
        topBarBackdrop = topBarBackdrop,
        contentBackdrop = contentBackdrop,
        topBarColor = topBarColor,
        enableSearchBar = enableSearchBar,
        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        showSearchBar = state.showSearchBar,
        trackedSearch = state.trackedSearch,
        sortMode = state.sortMode,
        showSortPopup = state.showSortPopup,
        showFloatingAddButton = state.showFloatingAddButton,
        deleteInProgress = state.deleteInProgress,
        isDark = isDark,
        overviewRefreshState = state.overviewRefreshState,
        refreshProgress = state.refreshProgress,
        lastRefreshMs = state.lastRefreshMs,
        lookupConfig = state.lookupConfig,
        overviewMetrics = trackedUi.overviewMetrics,
        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
        trackedItems = state.trackedItems,
        filteredTracked = trackedUi.filteredTracked,
        sortedTracked = trackedUi.sortedTracked,
        appLastUpdatedAtByPackage = appLastUpdatedAtByPackage,
        checkStates = state.checkStates,
        apkAssetBundles = state.apkAssetBundles,
        apkAssetLoading = state.apkAssetLoading,
        apkAssetErrors = state.apkAssetErrors,
        apkAssetExpanded = state.apkAssetExpanded,
        trackedCardExpanded = state.trackedCardExpanded,
        onTrackedSearchChange = { state.trackedSearch = it },
        onShowSortPopupChange = { state.showSortPopup = it },
        onSortModeChange = { state.sortMode = it },
        onOpenStrategySheet = actions::openStrategySheet,
        onOpenCheckLogicSheet = actions::openCheckLogicSheet,
        onRefreshAllTracked = { actions.refreshAllTracked(showToast = true) },
        onOpenTrackSheetForAdd = actions::openTrackSheetForAdd,
        onOpenTrackSheetForEdit = actions::openTrackSheetForEdit,
        onClearApkAssetUiState = actions::clearApkAssetUiState,
        onCollapseApkAssetPanel = { item, itemState ->
            actions.clearApkAssetUiState(item.id)
            actions.clearApkAssetCache(item, itemState)
        },
        onLoadApkAssets = { item, itemState, toggleOnlyWhenCached, includeAllAssets ->
            actions.loadApkAssets(
                item = item,
                itemState = itemState,
                toggleOnlyWhenCached = toggleOnlyWhenCached,
                includeAllAssets = includeAllAssets
            )
        },
        onOpenExternalUrl = actions::openExternalUrl,
        onOpenApkInDownloader = actions::openApkInDownloader,
        onShareApkLink = actions::shareApkLink,
        onActionBarInteractingChanged = onActionBarInteractingChanged
    )

    GitHubStrategySheet(
        show = state.showStrategySheet,
        backdrop = sheetBackdrop,
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
        trackedCount = trackedUi.overviewMetrics.trackedCount,
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
        backdrop = sheetBackdrop,
        lookupConfig = state.lookupConfig,
        refreshIntervalHours = state.refreshIntervalHours,
        refreshIntervalHoursInput = state.refreshIntervalHoursInput,
        checkAllTrackedPreReleasesInput = state.checkAllTrackedPreReleasesInput,
        aggressiveApkFilteringInput = state.aggressiveApkFilteringInput,
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
        onDismissRequest = actions::closeCheckLogicSheet,
        onApply = { actions.applyCheckLogicSheet(installedOnlineShareTargets) },
        onRefreshIntervalHoursInputChange = { state.refreshIntervalHoursInput = it },
        onCheckAllTrackedPreReleasesInputChange = { state.checkAllTrackedPreReleasesInput = it },
        onAggressiveApkFilteringInputChange = { state.aggressiveApkFilteringInput = it },
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

    GitHubTrackEditSheet(
        show = state.showAddSheet,
        backdrop = sheetBackdrop,
        editingTrackedItem = state.editingTrackedItem,
        repoUrlInput = state.repoUrlInput,
        appSearch = state.appSearch,
        pickerExpanded = state.pickerExpanded,
        selectedApp = state.selectedApp,
        appList = state.appList,
        preferPreReleaseInput = state.preferPreReleaseInput,
        alwaysShowLatestReleaseDownloadButtonInput = state.alwaysShowLatestReleaseDownloadButtonInput,
        onDismissRequest = actions::dismissTrackSheet,
        onApply = actions::applyTrackSheet,
        onRepoUrlInputChange = { state.repoUrlInput = it },
        onAppSearchChange = { state.appSearch = it },
        onPickerExpandedChange = { state.pickerExpanded = it },
        onSelectedAppChange = { state.selectedApp = it },
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
}
