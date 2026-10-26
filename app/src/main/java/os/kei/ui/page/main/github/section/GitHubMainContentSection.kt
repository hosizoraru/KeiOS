package os.kei.ui.page.main.github.section

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.R
import os.kei.ui.page.main.github.GitHubTrackedFilterMode
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.host.pager.MainPageContentBackdropScene
import os.kei.ui.page.main.os.appLucideAddIcon
import os.kei.ui.page.main.os.appLucideHistoryIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.appPageContentMaxWidthFor
import os.kei.ui.page.main.widget.chrome.LocalAppPageContentMaxWidth
import os.kei.ui.page.main.widget.chrome.AppPageTwoColumnLists
import os.kei.ui.page.main.github.page.githubTrackedLanesFor
import os.kei.ui.page.main.github.page.GitHubTrackedLanes
import androidx.compose.foundation.lazy.LazyListScope
import os.kei.ui.page.main.widget.chrome.appPageEdgePaddingStart
import os.kei.ui.page.main.widget.chrome.appPageEdgePaddingEnd
import os.kei.ui.page.main.widget.chrome.AppScaffold
import os.kei.ui.page.main.widget.chrome.appFloatingDockEndPadding
import os.kei.ui.page.main.widget.chrome.appFloatingDockStartPadding
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.chrome.rememberAppPullToRefreshState
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppEdgeStackKeepAlive
import os.kei.ui.page.main.widget.glass.AppEdgeStackListTopInset
import os.kei.ui.page.main.widget.glass.AppFloatingDockAction
import os.kei.ui.page.main.widget.glass.AppFloatingDockSide
import os.kei.ui.page.main.widget.glass.LocalAppEdgeStackCards
import os.kei.ui.page.main.widget.glass.appEdgeStackKeepAliveTopPadding
import os.kei.ui.page.main.widget.glass.rememberAppEdgeStackState
import os.kei.ui.page.main.widget.glass.AppFloatingRefreshStatus
import os.kei.ui.page.main.widget.glass.AppFloatingVerticalSearchActionDock
import os.kei.ui.page.main.widget.glass.appFloatingDockBottomTarget
import os.kei.ui.page.main.widget.glass.rememberAppFloatingDockBottomState
import os.kei.ui.page.main.widget.glass.rememberAppFloatingKeyboardLiftState
import os.kei.ui.testing.KeiOsTestTags
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Suppress("FunctionName")
@Composable
internal fun GitHubMainContent(
    layout: GitHubMainContentLayout,
    surfaces: GitHubMainContentSurfaces,
    controls: GitHubMainContentControls,
    overview: GitHubMainContentOverview,
    tracked: GitHubMainContentTracked,
    shareImport: GitHubMainContentShareImport,
    actions: GitHubMainContentActions,
) {
    val context = LocalContext.current
    val supportedAbis = remember { Build.SUPPORTED_ABIS?.toList().orEmpty() }
    val searchDockBottomTarget =
        appFloatingDockBottomTarget(
            contentBottomPadding = layout.contentBottomPadding,
            bottomBarVisible = layout.bottomBarVisible,
        )
    val searchDockBottomState =
        rememberAppFloatingDockBottomState(
            contentBottomPadding = layout.contentBottomPadding,
            bottomBarVisible = layout.bottomBarVisible,
            label = "github_floating_search_bottom",
        )
    val floatingKeyboardLiftState =
        rememberAppFloatingKeyboardLiftState(
            restingBottomGap = searchDockBottomTarget,
            label = "github_floating_keyboard_lift",
        )
    val floatingKeyboardLiftProvider = remember(floatingKeyboardLiftState) { { floatingKeyboardLiftState.value } }
    val dockAlignment =
        if (layout.floatingDockSide == AppFloatingDockSide.Start) {
            androidx.compose.ui.Alignment.BottomStart
        } else {
            androidx.compose.ui.Alignment.BottomEnd
        }
    val dockStartPadding = appFloatingDockStartPadding(layout.floatingDockSide == AppFloatingDockSide.Start)
    val dockEndPadding = appFloatingDockEndPadding(layout.floatingDockSide == AppFloatingDockSide.End)
    val refreshStatus =
        when (overview.refreshState) {
            OverviewRefreshState.Refreshing -> AppFloatingRefreshStatus.Refreshing
            OverviewRefreshState.Completed -> AppFloatingRefreshStatus.Success
            OverviewRefreshState.Failed -> AppFloatingRefreshStatus.Danger
            OverviewRefreshState.Cached -> AppFloatingRefreshStatus.Cached
            OverviewRefreshState.Idle -> AppFloatingRefreshStatus.Idle
        }
    // Background and dock refreshes drive the same OverviewRefreshState, so the pull header
    // keys on a gesture-scoped session instead of the shared state; otherwise a launch-time
    // or dock refresh would expand the header and freeze list scrolling without a pull.
    var pullRefreshSessionActive by remember { mutableStateOf(false) }
    val overviewRefreshStateNow by rememberUpdatedState(overview.refreshState)
    LaunchedEffect(pullRefreshSessionActive) {
        if (!pullRefreshSessionActive) return@LaunchedEffect
        val refreshStarted =
            withTimeoutOrNull(GitHubPullRefreshStartTimeoutMs) {
                snapshotFlow { overviewRefreshStateNow }
                    .first { it == OverviewRefreshState.Refreshing }
            } != null
        if (refreshStarted) {
            snapshotFlow { overviewRefreshStateNow }
                .first { it != OverviewRefreshState.Refreshing }
        }
        pullRefreshSessionActive = false
    }
    val actionsHistoryIcon = appLucideHistoryIcon()
    val moreIcon = appLucideMoreIcon()
    val actionsHistoryDescription = stringResource(R.string.github_history_cd_open)
    val expandDockDescription = stringResource(R.string.common_expand)
    val actionsHistoryTint = MiuixTheme.colorScheme.primary
    val historyUnreadBadgeLabel = githubDockBadgeLabel(controls.historyUnreadCount)
    val historyUnreadBadgeColor: Color? =
        if (controls.historyUnreadCount > 0) {
            MiuixTheme.colorScheme.primary
        } else {
            null
        }
    val historyUnreadBadgeContentColor: Color? =
        if (controls.historyUnreadCount > 0) {
            MiuixTheme.colorScheme.onPrimary
        } else {
            null
        }
    val historyUnreadBadgeTooltip =
        historyUnreadBadgeLabel?.let {
            stringResource(R.string.github_history_unread_badge_tooltip, controls.historyUnreadCount)
        }
    val refreshBadgeCount =
        if (overview.metrics.failedCount > 0) {
            overview.metrics.failedCount
        } else {
            overview.metrics.totalUpdatableCount
        }
    val refreshBadgeLabel = githubDockBadgeLabel(refreshBadgeCount)
    val refreshBadgeColor: Color? =
        when {
            overview.metrics.failedCount > 0 -> MiuixTheme.colorScheme.error
            overview.metrics.totalUpdatableCount > 0 -> MiuixTheme.colorScheme.primary
            else -> null
        }
    val refreshBadgeContentColor: Color? =
        when {
            overview.metrics.failedCount > 0 -> MiuixTheme.colorScheme.onError
            overview.metrics.totalUpdatableCount > 0 -> MiuixTheme.colorScheme.onPrimary
            else -> null
        }
    val refreshBadgeTooltip =
        when {
            overview.metrics.failedCount > 0 ->
                stringResource(
                    R.string.github_refresh_badge_failed_tooltip,
                    overview.metrics.failedCount,
                    overview.metrics.totalUpdatableCount,
                    overview.metrics.stableUpdateCount,
                    overview.metrics.preReleaseUpdateCount,
                )
            overview.metrics.totalUpdatableCount > 0 ->
                stringResource(
                    R.string.github_refresh_badge_updates_tooltip,
                    overview.metrics.totalUpdatableCount,
                    overview.metrics.stableUpdateCount,
                    overview.metrics.preReleaseUpdateCount,
                )
            else -> null
        }
    val dockExtraActions =
        remember(
            actionsHistoryIcon,
            actionsHistoryDescription,
            actionsHistoryTint,
            historyUnreadBadgeLabel,
            historyUnreadBadgeColor,
            historyUnreadBadgeContentColor,
            historyUnreadBadgeTooltip,
            actions.onSearchExpandedChange,
            actions.onOpenActionsNotificationHistory,
        ) {
            listOf(
                AppFloatingDockAction(
                    icon = actionsHistoryIcon,
                    contentDescription = actionsHistoryDescription,
                    iconTint = actionsHistoryTint,
                    testTag = KeiOsTestTags.GitHubActionsHistoryButton,
                    badgeLabel = historyUnreadBadgeLabel,
                    badgeColor = historyUnreadBadgeColor,
                    badgeContentColor = historyUnreadBadgeContentColor,
                    tooltipText = historyUnreadBadgeTooltip,
                    onClick = {
                        actions.onSearchExpandedChange(false)
                        actions.onOpenActionsNotificationHistory()
                    },
                ),
            )
        }
    MainPageContentBackdropScene(
        contentProducer = null,
        sheetProducer = surfaces.sheetProducer,
        producerActive = surfaces.backdropProducerActive,
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(KeiOsTestTags.GitHubPageRoot),
    ) {
        // Published here because this page builds its own chrome rather than going through
        // AppPageScaffold: the top row is a sibling of the lists and has to centre against the same
        // column they use. The floating docks are deliberately *not* on this cap -- they stay on the
        // single-column chrome cap, which `appBottomChromeSideGutter*` pins them to.
        CompositionLocalProvider(
            LocalAppPageContentMaxWidth provides appPageContentMaxWidthFor(layout.columnCount),
        ) {
        AppScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                GitHubTopBarSection(
                    topBarColor = surfaces.topBarColor,
                    scrollBehavior = layout.scrollBehavior,
                    titleBackdrop = surfaces.topBarMaterial,
                    onTitleClick = layout.onTitleClick,
                )
            },
        ) { innerPadding ->
            val edgeStackState = rememberAppEdgeStackState(stackLine = AppEdgeStackListTopInset)
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                // The version-tracking hub stays pinned above the list; tracked cards
                // stack beneath it and the pull gesture runs in the list region only.
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = appPageEdgePaddingStart(),
                                end = appPageEdgePaddingEnd(),
                                top = innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap,
                            ),
                ) {
                    GitHubOverviewCard(
                        backdrop = surfaces.contentBackdrop,
                        isDark = surfaces.isDark,
                        lookupConfig = overview.lookupConfig,
                        overviewRefreshState = overview.refreshState,
                        refreshProgress = overview.refreshProgress,
                        lastRefreshMs = overview.lastRefreshMs,
                        metrics = overview.metrics,
                        failedFilterActive = controls.trackedFilterMode == GitHubTrackedFilterMode.FailedChecks,
                        onRetryFailedTracked = actions.onRetryFailedTracked,
                        onFailedFilterToggle = actions.onFailedFilterToggle,
                        onAddTracked = actions.onOpenTrackSheetForAdd,
                    )
                }
                CompositionLocalProvider(LocalAppEdgeStackCards provides edgeStackState) {
                PullToRefresh(
                    isRefreshing = pullRefreshSessionActive,
                    onRefresh = {
                        if (tracked.sortedTracked.isNotEmpty()) {
                            pullRefreshSessionActive = true
                        }
                        actions.onRefreshVisibleTracked()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .layerBackdrop(surfaces.topBarProducer),
                    pullToRefreshState = rememberAppPullToRefreshState(),
                    topAppBarScrollBehavior = layout.scrollBehavior,
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    refreshTexts =
                        listOf(
                            stringResource(R.string.github_pull_refresh_pull),
                            stringResource(R.string.github_pull_refresh_release),
                            stringResource(R.string.github_pull_refresh_refreshing),
                            stringResource(R.string.github_pull_refresh_done),
                        ),
                ) {
                    // Inside PullToRefresh, so the RefreshHeader above keeps its own anchor — see
                    // OsPageMainList.
                    AppEdgeStackKeepAlive(
                        state = edgeStackState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                    val lanes =
                        remember(
                            tracked.sortedTracked,
                            tracked.pinnedTrackIds,
                            tracked.laneDetainedTrackIds,
                            layout.columnCount,
                        ) {
                            if (layout.columnCount >= 2) {
                                githubTrackedLanesFor(
                                    sortedTracked = tracked.sortedTracked,
                                    pinnedIds = tracked.pinnedTrackIds.toList(),
                                    detainedIds = tracked.laneDetainedTrackIds,
                                )
                            } else {
                                GitHubTrackedLanes(first = tracked.sortedTracked, second = emptyList())
                            }
                        }
                    // The browsing lane also carries the share-import cards and the list's own empty
                    // states: those describe the list, not one of its halves.
                    val browsingLane: LazyListScope.() -> Unit = {
                        if (shareImport.showPendingCard && shareImport.pendingTrack != null) {
                            item(
                                key = "github_pending_share_import_track",
                                contentType = "github_share_import",
                            ) {
                                GitHubPendingShareImportCard(
                                    pending = shareImport.pendingTrack,
                                    nowMillis = shareImport.pendingNowMillis,
                                    repoOverlapCount = shareImport.pendingRepoOverlapCount,
                                    onOpen = actions.onOpenShareImportFlow,
                                    onCancel = actions.onCancelPendingShareImportTrack,
                                )
                            }
                        }
                        shareImport.pendingAttachCandidate?.let { candidate ->
                            item(
                                key = "github_pending_share_import_attach",
                                contentType = "github_share_import",
                            ) {
                                GitHubShareImportAttachCandidateCard(
                                    candidate = candidate,
                                    onOpen = actions.onOpenShareImportFlow,
                                    onCancel = actions.onCancelActiveShareImportFlow,
                                )
                            }
                        }
                        if (shareImport.pendingTrack == null && shareImport.pendingAttachCandidate == null) {
                            shareImport.pendingPreview?.let { preview ->
                                item(
                                    key = "github_share_import_preview",
                                    contentType = "github_share_import",
                                ) {
                                    GitHubShareImportPreviewCard(
                                        preview = preview,
                                        onOpen = actions.onOpenShareImportFlow,
                                        onCancel = actions.onCancelActiveShareImportFlow,
                                    )
                                }
                            }
                        }
                        if (
                            shareImport.pendingPreview == null &&
                            shareImport.pendingTrack == null &&
                            shareImport.pendingAttachCandidate == null
                        ) {
                            shareImport.pendingResult?.let { result ->
                                item(
                                    key = "github_share_import_result",
                                    contentType = "github_share_import",
                                ) {
                                    GitHubShareImportResultCard(
                                        result = result,
                                        onOpen = actions.onOpenShareImportResult,
                                        onDismiss = actions.onDismissShareImportResult,
                                    )
                                }
                            }
                        }
                        GitHubTrackedItemsSection(
                            content =
                                GitHubTrackedItemsContent(
                                    lookupConfig = overview.lookupConfig,
                                    trackedItems = tracked.trackedItems,
                                    filteredTracked = tracked.filteredTracked,
                                    sortedTracked = tracked.sortedTracked,
                                    laneTracked = lanes.first,
                                    pinnedTrackIds = tracked.pinnedTrackIds,
                                    installedAppLabelsByPackage = tracked.installedAppLabelsByPackage,
                                    appLastUpdatedAtByTrackId = tracked.appLastUpdatedAtByTrackId,
                                ),
                            surfaces =
                                GitHubTrackedItemsSurfaces(
                                    contentBackdrop = surfaces.contentBackdrop,
                                    isDark = surfaces.isDark,
                                ),
                            checkState =
                                GitHubTrackedItemsCheckState(
                                    checkStates = tracked.checkStates,
                                    itemRefreshLoading = tracked.itemRefreshLoading,
                                    actionsRecommendedRunSnapshots = tracked.actionsRecommendedRunSnapshots,
                                ),
                            assetState =
                                GitHubTrackedItemsAssetState(
                                    apkAssetBundles = tracked.apkAssetBundles,
                                    apkAssetLoading = tracked.apkAssetLoading,
                                    apkAssetErrors = tracked.apkAssetErrors,
                                    apkAssetExpanded = tracked.apkAssetExpanded,
                                    apkInfoResults = tracked.apkInfoResults,
                                    managedInstallLoading = tracked.managedInstallLoading,
                                ),
                            expansionState =
                                tracked.expansionState,
                            runtime =
                                GitHubTrackedItemsRuntime(
                                    context = context,
                                    supportedAbis = supportedAbis,
                                    relativeTimeNowMillis = tracked.relativeTimeNowMillis,
                                ),
                            actions =
                                GitHubTrackedItemsActions(
                                    onRefreshTrackedItem = actions.onRefreshTrackedItem,
                                    onOpenActionsSheet = actions.onOpenActionsSheet,
                                    onOpenTrackSheetForEdit = actions.onOpenTrackSheetForEdit,
                                    onIgnoreCurrentTrackedVersion = actions.onIgnoreCurrentTrackedVersion,
                                    onRequestDeleteTrackedItem = actions.onRequestDeleteTrackedItem,
                                    onOpenFdroidDetail = actions.onOpenFdroidDetail,
                                    onOpenReleaseList = actions.onOpenReleaseList,
                                    onOpenFdroidVersionList = actions.onOpenFdroidVersionList,
                                    onTrackedCardExpandedChange = actions.onTrackedCardExpandedChange,
                                    onToggleTrackedPinned = actions.onToggleTrackedPinned,
                                    onCollapseTrackedCard = actions.onCollapseTrackedCard,
                                    onLocalVersionExpandedChange = actions.onLocalVersionExpandedChange,
                                    onStableVersionExpandedChange = actions.onStableVersionExpandedChange,
                                    onPreReleaseVersionExpandedChange = actions.onPreReleaseVersionExpandedChange,
                                    onCollapseApkAssetPanel = actions.onCollapseApkAssetPanel,
                                    onLoadApkAssets = actions.onLoadApkAssets,
                                    onOpenDecisionAssistDetail = actions.onOpenDecisionAssistDetail,
                                    onOpenExternalUrl = actions.onOpenExternalUrl,
                                    onOpenApkInfo = actions.onOpenApkInfo,
                                    onInstallApk = actions.onInstallApk,
                                    onOpenApkInDownloader = actions.onOpenApkInDownloader,
                                    onShareApkLink = actions.onShareApkLink,
                                ),
                        )
                    }
                    // Empty until a card is opened or pinned, which is the whole point: the list keeps its
                    // full height until the reader asks to read something.
                    val readingLane: LazyListScope.() -> Unit = {
                        GitHubTrackedItemsSection(
                            content =
                                GitHubTrackedItemsContent(
                                    lookupConfig = overview.lookupConfig,
                                    trackedItems = tracked.trackedItems,
                                    filteredTracked = tracked.filteredTracked,
                                    sortedTracked = tracked.sortedTracked,
                                    laneTracked = lanes.second,
                                    showListStatus = false,
                                    pinnedTrackIds = tracked.pinnedTrackIds,
                                    installedAppLabelsByPackage = tracked.installedAppLabelsByPackage,
                                    appLastUpdatedAtByTrackId = tracked.appLastUpdatedAtByTrackId,
                                ),
                            surfaces =
                                GitHubTrackedItemsSurfaces(
                                    contentBackdrop = surfaces.contentBackdrop,
                                    isDark = surfaces.isDark,
                                ),
                            checkState =
                                GitHubTrackedItemsCheckState(
                                    checkStates = tracked.checkStates,
                                    itemRefreshLoading = tracked.itemRefreshLoading,
                                    actionsRecommendedRunSnapshots = tracked.actionsRecommendedRunSnapshots,
                                ),
                            assetState =
                                GitHubTrackedItemsAssetState(
                                    apkAssetBundles = tracked.apkAssetBundles,
                                    apkAssetLoading = tracked.apkAssetLoading,
                                    apkAssetErrors = tracked.apkAssetErrors,
                                    apkAssetExpanded = tracked.apkAssetExpanded,
                                    apkInfoResults = tracked.apkInfoResults,
                                    managedInstallLoading = tracked.managedInstallLoading,
                                ),
                            expansionState =
                                tracked.expansionState,
                            runtime =
                                GitHubTrackedItemsRuntime(
                                    context = context,
                                    supportedAbis = supportedAbis,
                                    relativeTimeNowMillis = tracked.relativeTimeNowMillis,
                                ),
                            actions =
                                GitHubTrackedItemsActions(
                                    onRefreshTrackedItem = actions.onRefreshTrackedItem,
                                    onOpenActionsSheet = actions.onOpenActionsSheet,
                                    onOpenTrackSheetForEdit = actions.onOpenTrackSheetForEdit,
                                    onIgnoreCurrentTrackedVersion = actions.onIgnoreCurrentTrackedVersion,
                                    onRequestDeleteTrackedItem = actions.onRequestDeleteTrackedItem,
                                    onOpenFdroidDetail = actions.onOpenFdroidDetail,
                                    onOpenReleaseList = actions.onOpenReleaseList,
                                    onOpenFdroidVersionList = actions.onOpenFdroidVersionList,
                                    onTrackedCardExpandedChange = actions.onTrackedCardExpandedChange,
                                    onToggleTrackedPinned = actions.onToggleTrackedPinned,
                                    onCollapseTrackedCard = actions.onCollapseTrackedCard,
                                    onLocalVersionExpandedChange = actions.onLocalVersionExpandedChange,
                                    onStableVersionExpandedChange = actions.onStableVersionExpandedChange,
                                    onPreReleaseVersionExpandedChange = actions.onPreReleaseVersionExpandedChange,
                                    onCollapseApkAssetPanel = actions.onCollapseApkAssetPanel,
                                    onLoadApkAssets = actions.onLoadApkAssets,
                                    onOpenDecisionAssistDetail = actions.onOpenDecisionAssistDetail,
                                    onOpenExternalUrl = actions.onOpenExternalUrl,
                                    onOpenApkInfo = actions.onOpenApkInfo,
                                    onInstallApk = actions.onInstallApk,
                                    onOpenApkInDownloader = actions.onOpenApkInDownloader,
                                    onShareApkLink = actions.onShareApkLink,
                                ),
                        )
                    }
                    val listModifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(layout.scrollBehavior.nestedScrollConnection)
                    val listInnerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                    val listBottomExtra =
                        appPageBottomPaddingWithFloatingOverlay(layout.contentBottomPadding)
                    val listTopExtra = appEdgeStackKeepAliveTopPadding(AppEdgeStackListTopInset)
                    if (layout.columnCount >= 2) {
                        AppPageTwoColumnLists(
                            innerPadding = listInnerPadding,
                            primaryState = layout.listState,
                            secondaryState = layout.secondaryListState,
                            modifier = listModifier,
                            bottomExtra = listBottomExtra,
                            topExtra = listTopExtra,
                            sectionSpacing = CardLayoutRhythm.denseSectionGap,
                            primary = browsingLane,
                            secondary = readingLane,
                        )
                    } else {
                        AppPageLazyColumn(
                            modifier = listModifier,
                            state = layout.listState,
                            innerPadding = listInnerPadding,
                            bottomExtra = listBottomExtra,
                            topExtra = listTopExtra,
                            sectionSpacing = CardLayoutRhythm.denseSectionGap,
                            content = browsingLane,
                        )
                    }
                }
                }
                }
                }

                AppFloatingVerticalSearchActionDock(
                    backdrop = surfaces.topBarMaterial,
                    expanded = controls.searchExpanded,
                    query = controls.trackedSearch,
                    onQueryChange = actions.onTrackedSearchChange,
                    onExpandedChange = actions.onSearchExpandedChange,
                    searchIcon = appLucideSearchIcon(),
                    searchContentDescription = stringResource(R.string.github_topbar_search_label),
                    placeholder = stringResource(R.string.github_topbar_search_label),
                    addIcon = appLucideAddIcon(),
                    addContentDescription = stringResource(R.string.github_cd_add_track),
                    onAddClick = actions.onOpenTrackSheetForAdd,
                    refreshIcon = appLucideRefreshIcon(),
                    refreshContentDescription = stringResource(R.string.github_topbar_cd_check),
                    onRefreshClick = actions.onRefreshVisibleTracked,
                    showAddAction = false,
                    showRefreshAction = false,
                    refreshEnabled = !controls.deleteInProgress,
                    refreshStatus = refreshStatus,
                    refreshBadgeLabel = refreshBadgeLabel,
                    refreshBadgeColor = refreshBadgeColor,
                    refreshBadgeContentColor = refreshBadgeContentColor,
                    refreshTooltipText = refreshBadgeTooltip,
                    compact = !layout.bottomBarVisible,
                    compactIcon = moreIcon,
                    compactContentDescription = expandDockDescription,
                    // The collapsed label is derived from the actions the dock hides, so the number no
                    // longer changes meaning when the dock collapses. Only the tint is chosen here.
                    compactBadgeColor = refreshBadgeColor,
                    compactBadgeContentColor = refreshBadgeContentColor,
                    compactTooltipText = refreshBadgeTooltip ?: expandDockDescription,
                    onCompactClick = layout.onExpandFloatingDock,
                    extraActions = dockExtraActions,
                    dockSide = layout.floatingDockSide,
                    keyboardLiftProvider = floatingKeyboardLiftProvider,
                    modifier =
                        Modifier
                            .align(dockAlignment)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = -searchDockBottomState.value.roundToPx(),
                                )
                            }.padding(
                                start = dockStartPadding,
                                end = dockEndPadding,
                            ),
                )
            }
        }
        AppTopEndActionBarOverlay {
            GitHubTopBarActions(
                backdrop = surfaces.topBarMaterial,
                sortMode = controls.sortMode,
                sortDirection = controls.sortDirection,
                trackedFilterMode = controls.trackedFilterMode,
                refreshIntervalHours = controls.refreshIntervalHours,
                fdroidCommonRepoCount = overview.lookupConfig.normalizedFdroidCommonRepoIds.size,
                showActionMenuPopup = controls.showActionMenuPopup,
                tracksExporting = controls.tracksExporting,
                tracksImporting = controls.tracksImporting,
                onOpenStrategySheet = actions.onOpenStrategySheet,
                onOpenCheckLogicSheet = actions.onOpenCheckLogicSheet,
                onOpenDroidSourcesSheet = actions.onOpenDroidSourcesSheet,
                onOpenDebugSheet = actions.onOpenDebugSheet,
                onRefreshInstalledApps = actions.onRefreshInstalledApps,
                onShowActionMenuPopupChange = actions.onShowActionMenuPopupChange,
                onSortModeChange = actions.onSortModeChange,
                onSortDirectionChange = actions.onSortDirectionChange,
                onTrackedFilterModeChange = actions.onTrackedFilterModeChange,
                onRefreshIntervalHoursChange = actions.onRefreshIntervalHoursChange,
                onExportTrackedItems = actions.onExportTrackedItems,
                onImportTrackedItems = actions.onImportTrackedItems,
                onOpenStarImport = actions.onOpenStarImport,
                onOpenDesktopWidget = { os.kei.appwidget.github.GitHubWidgetConfigureActivity.launch(context) },
            )
        }
    }
    }
}

private const val GitHubDockBadgeMaxCount = 99

// Covers the installed-app reload that runs before a pulled batch flips the shared state to
// Refreshing; a pull that never starts a batch (for example an empty checkable list) ends here.
private const val GitHubPullRefreshStartTimeoutMs = 8_000L

private fun githubDockBadgeLabel(count: Int): String? =
    when {
        count <= 0 -> null
        count > GitHubDockBadgeMaxCount -> "$GitHubDockBadgeMaxCount+"
        else -> count.toString()
    }
