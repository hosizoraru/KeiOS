package os.kei.ui.page.main.github.section

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.GitHubSortMode
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack
import os.kei.ui.page.main.os.appLucideAddIcon
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppFloatingLiquidActionButton
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun GitHubMainContent(
    contentBottomPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    addButtonScrollConnection: NestedScrollConnection,
    topBarBackdrop: LayerBackdrop,
    contentBackdrop: LayerBackdrop,
    topBarColor: Color,
    enableSearchBar: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    reduceEffectsDuringPagerScroll: Boolean,
    reduceEffectsDuringListScroll: Boolean,
    showSearchBar: Boolean,
    trackedSearch: String,
    sortMode: GitHubSortMode,
    showSortPopup: Boolean,
    showFloatingAddButton: Boolean,
    deleteInProgress: Boolean,
    isDark: Boolean,
    overviewRefreshState: OverviewRefreshState,
    refreshProgress: Float,
    lastRefreshMs: Long,
    lookupConfig: GitHubLookupConfig,
    overviewMetrics: GitHubOverviewMetrics,
    cardPressFeedbackEnabled: Boolean,
    trackedItems: List<GitHubTrackedApp>,
    filteredTracked: List<GitHubTrackedApp>,
    sortedTracked: List<GitHubTrackedApp>,
    appLastUpdatedAtByTrackId: Map<String, Long>,
    checkStates: SnapshotStateMap<String, VersionCheckUi>,
    itemRefreshLoading: SnapshotStateMap<String, Boolean>,
    apkAssetBundles: SnapshotStateMap<String, GitHubReleaseAssetBundle>,
    apkAssetLoading: SnapshotStateMap<String, Boolean>,
    apkAssetErrors: SnapshotStateMap<String, String>,
    apkAssetExpanded: SnapshotStateMap<String, Boolean>,
    trackedCardExpanded: SnapshotStateMap<String, Boolean>,
    pendingShareImportTrack: GitHubPendingShareImportTrack?,
    showPendingShareImportCard: Boolean,
    pendingShareImportRepoOverlapCount: Int,
    onTrackedSearchChange: (String) -> Unit,
    onShowSortPopupChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubSortMode) -> Unit,
    onOpenStrategySheet: () -> Unit,
    onOpenCheckLogicSheet: () -> Unit,
    onRefreshAllTracked: () -> Unit,
    onRefreshTrackedItem: (GitHubTrackedApp) -> Unit,
    onOpenActionsSheet: (GitHubTrackedApp) -> Unit,
    onOpenTrackSheetForAdd: () -> Unit,
    onOpenTrackSheetForEdit: (GitHubTrackedApp) -> Unit,
    onRequestDeleteTrackedItem: (GitHubTrackedApp) -> Unit,
    onClearApkAssetUiState: (String) -> Unit,
    onCollapseApkAssetPanel: (GitHubTrackedApp, VersionCheckUi) -> Unit,
    onLoadApkAssets: (GitHubTrackedApp, VersionCheckUi, Boolean, Boolean) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit,
    onCancelPendingShareImportTrack: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                GitHubTopBarSection(
                    backdrop = topBarBackdrop,
                    topBarColor = topBarColor,
                    scrollBehavior = scrollBehavior,
                    enableSearchBar = enableSearchBar,
                    showSearchBar = showSearchBar,
                    trackedSearch = trackedSearch,
                    reduceEffectsDuringListScroll = reduceEffectsDuringListScroll,
                    onTrackedSearchChange = onTrackedSearchChange,
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(addButtonScrollConnection)
            ) {
                AppPageLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    state = listState,
                    innerPadding = innerPadding,
                    bottomExtra = appPageBottomPaddingWithFloatingOverlay(contentBottomPadding),
                    topExtra = 0.dp,
                    sectionSpacing = CardLayoutRhythm.denseSectionGap
                ) {
                    item {
                        GitHubOverviewCard(
                            isDark = isDark,
                            lookupConfig = lookupConfig,
                            overviewRefreshState = overviewRefreshState,
                            refreshProgress = refreshProgress,
                            lastRefreshMs = lastRefreshMs,
                            metrics = overviewMetrics,
                            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                            onRefreshAllTracked = onRefreshAllTracked,
                            onOpenTrackSheetForAdd = onOpenTrackSheetForAdd
                        )
                    }
                    if (showPendingShareImportCard && pendingShareImportTrack != null) {
                        item {
                            GitHubPendingShareImportCard(
                                pending = pendingShareImportTrack,
                                repoOverlapCount = pendingShareImportRepoOverlapCount,
                                onCancel = onCancelPendingShareImportTrack
                            )
                        }
                    }
                    GitHubTrackedItemsSection(
                        trackedItems = trackedItems,
                        filteredTracked = filteredTracked,
                        sortedTracked = sortedTracked,
                        appLastUpdatedAtByTrackId = appLastUpdatedAtByTrackId,
                        checkStates = checkStates,
                        itemRefreshLoading = itemRefreshLoading,
                        contentBackdrop = contentBackdrop,
                        reduceEffectsDuringListScroll = reduceEffectsDuringListScroll,
                        isDark = isDark,
                        apkAssetBundles = apkAssetBundles,
                        apkAssetLoading = apkAssetLoading,
                        apkAssetErrors = apkAssetErrors,
                        apkAssetExpanded = apkAssetExpanded,
                        trackedCardExpanded = trackedCardExpanded,
                        onRefreshTrackedItem = onRefreshTrackedItem,
                        onOpenActionsSheet = onOpenActionsSheet,
                        onOpenTrackSheetForEdit = onOpenTrackSheetForEdit,
                        onRequestDeleteTrackedItem = onRequestDeleteTrackedItem,
                        onClearApkAssetUiState = onClearApkAssetUiState,
                        onCollapseApkAssetPanel = onCollapseApkAssetPanel,
                        onLoadApkAssets = onLoadApkAssets,
                        onOpenExternalUrl = onOpenExternalUrl,
                        onOpenApkInDownloader = onOpenApkInDownloader,
                        onShareApkLink = onShareApkLink,
                        context = context,
                        supportedAbis = supportedAbis
                    )
                }

                AnimatedVisibility(
                    visible = showFloatingAddButton,
                    enter = appFloatingEnter(),
                    exit = appFloatingExit(),
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
                ) {
                    AppFloatingLiquidActionButton(
                        backdrop = if (reduceEffectsDuringListScroll) null else contentBackdrop,
                        icon = appLucideAddIcon(),
                        contentDescription = stringResource(R.string.github_cd_add_track),
                        onClick = onOpenTrackSheetForAdd,
                        modifier = Modifier.padding(end = 14.dp, bottom = contentBottomPadding - 24.dp),
                    )
                }
            }
        }
        AppTopEndActionBarOverlay {
            GitHubTopBarActions(
                backdrop = topBarBackdrop,
                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                reduceEffectsDuringPagerScroll = reduceEffectsDuringPagerScroll,
                sortMode = sortMode,
                showSortPopup = showSortPopup,
                deleteInProgress = deleteInProgress,
                onOpenStrategySheet = onOpenStrategySheet,
                onOpenCheckLogicSheet = onOpenCheckLogicSheet,
                onShowSortPopupChange = onShowSortPopupChange,
                onSortModeChange = onSortModeChange,
                onRefreshAllTracked = onRefreshAllTracked,
                onActionBarInteractingChanged = onActionBarInteractingChanged
            )
        }
    }
}
