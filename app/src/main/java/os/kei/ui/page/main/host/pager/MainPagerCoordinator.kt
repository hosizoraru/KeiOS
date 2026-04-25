package os.kei.ui.page.main.host.pager

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.mcp.server.McpServerManager
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.perf.ReportPagerPerformanceState
import com.kyant.backdrop.backdrops.LayerBackdrop

internal data class MainPagerInsets(
    val navigationBarBottom: Dp,
    val homeTopInset: Dp,
    val homeBottomInset: Dp,
    val bottomOverlayPadding: Dp
)

internal data class MainPagerCoordinatorState(
    val tabs: List<BottomPage>,
    val visibleTabsSnapshot: Set<BottomPage>,
    val pagerState: PagerState,
    val pagerRuntime: MainPagerRuntimeSnapshot,
    val homeMcpOverview: HomeMcpOverview,
    val homeGitHubOverview: HomeGitHubOverview,
    val homeBaOverview: HomeBaOverview,
    val visibleOverviewCards: Set<HomeOverviewCard>,
    val pagerScrollEnabled: Boolean,
    val showBottomBar: Boolean,
    val nestedScrollConnection: NestedScrollConnection,
    val backdrop: LayerBackdrop,
    val farJumpAlpha: Float,
    val hasNonHomeBackground: Boolean,
    val effectiveNonHomeBackgroundUri: String,
    val onPageSelected: (Int) -> Unit,
    val onActionBarInteractingChanged: (Boolean) -> Unit,
    val onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    val onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit,
    val osScrollToTopSignal: Int,
    val baScrollToTopSignal: Int,
    val mcpScrollToTopSignal: Int,
    val githubScrollToTopSignal: Int
)

@Composable
internal fun rememberMainPagerInsets(): MainPagerInsets {
    val density = LocalDensity.current
    val navigationBarBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val systemInsets = WindowInsets.safeDrawing.union(WindowInsets.navigationBars).asPaddingValues()
    return remember(navigationBarBottom, systemInsets) {
        MainPagerInsets(
            navigationBarBottom = navigationBarBottom,
            homeTopInset = systemInsets.calculateTopPadding(),
            homeBottomInset = systemInsets.calculateBottomPadding(),
            bottomOverlayPadding = 112.dp + navigationBarBottom
        )
    }
}

@Composable
internal fun rememberMainPagerCoordinator(
    settingsReturnToken: Int,
    transitionAnimationsEnabled: Boolean,
    preloadingEnabled: Boolean,
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String,
    visibleBottomPageNames: Set<String>,
    onVisibleBottomPageNamesChange: (Set<String>) -> Unit,
    mcpServerManager: McpServerManager,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
): MainPagerCoordinatorState {
    val preloadPolicy = remember(preloadingEnabled) {
        UiPerformanceBudget.resolvePreloadPolicy(preloadingEnabled)
    }
    val backgroundState = rememberMainPagerBackgroundState(
        nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
        nonHomeBackgroundUri = nonHomeBackgroundUri
    )
    val tabsState = rememberMainPagerTabsState(
        visibleBottomPageNames = visibleBottomPageNames,
        requestedBottomPage = requestedBottomPage,
        requestedBottomPageToken = requestedBottomPageToken
    )
    val tabs = tabsState.tabs
    val visibleTabsSnapshot = tabsState.visibleTabsSnapshot
    val pagerState = rememberPagerState(
        initialPage = tabsState.initialPageIndex,
        pageCount = { tabs.size }
    )
    var temporaryBeyondViewportCount by remember { mutableStateOf<Int?>(null) }
    val homeOverviewState = rememberMainPagerHomeOverviewState(
        mcpServerManager = mcpServerManager,
        settingsReturnToken = settingsReturnToken
    )
    val backdrop = rememberMainPagerBackdropLifecycle()

    val pagerRuntime = buildMainPagerRuntimeSnapshot(
        tabs = tabs,
        currentPageIndex = pagerState.currentPage,
        targetPageIndex = pagerState.targetPage,
        settledPageIndex = pagerState.settledPage,
        isPagerScrollInProgress = pagerState.isScrollInProgress,
        preloadPolicy = preloadPolicy,
        temporaryBeyondViewportCount = temporaryBeyondViewportCount,
        hasNonHomeBackground = backgroundState.hasNonHomeBackground
    )
    BindMainPagerCoordinatorEffects(
        settingsReturnToken = settingsReturnToken,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        tabsSize = tabs.size,
        pagerState = pagerState,
        onTemporaryBeyondViewportCountChange = { temporaryBeyondViewportCount = it }
    )
    val tabJumpController = rememberMainPagerTabJumpController(
        tabs = tabs,
        pagerState = pagerState,
        pagerRuntime = pagerRuntime,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        requestedBottomPage = requestedBottomPage,
        requestedBottomPageToken = requestedBottomPageToken,
        onRequestedBottomPageConsumed = onRequestedBottomPageConsumed
    )
    val scrollSignalController = rememberMainPagerScrollSignalController(
        tabs = tabs,
        pagerRuntime = pagerRuntime,
        onPageSelected = tabJumpController.onPageSelected
    )
    val onBottomPageVisibilityChange = remember(
        visibleBottomPageNames,
        onVisibleBottomPageNamesChange
    ) {
        buildMainPagerVisibilityChangeAction(
            visibleBottomPageNames = visibleBottomPageNames,
            onVisibleBottomPageNamesChange = onVisibleBottomPageNamesChange
        )
    }

    ReportPagerPerformanceState(
        scope = "main_pager",
        currentPage = tabs.getOrElse(pagerState.currentPage) { BottomPage.Home }.name,
        targetPage = tabs.getOrElse(pagerState.targetPage) { BottomPage.Home }.name,
        scrolling = pagerState.isScrollInProgress
    )

    return remember(
        tabs,
        visibleTabsSnapshot,
        pagerState,
        pagerRuntime,
        homeOverviewState,
        tabJumpController,
        scrollSignalController,
        backdrop,
        tabJumpController.farJumpAlpha,
        backgroundState.hasNonHomeBackground,
        backgroundState.effectiveNonHomeBackgroundUri,
        onBottomPageVisibilityChange
    ) {
        MainPagerCoordinatorState(
            tabs = tabs,
            visibleTabsSnapshot = visibleTabsSnapshot,
            pagerState = pagerState,
            pagerRuntime = pagerRuntime,
            homeMcpOverview = homeOverviewState.homeMcpOverview,
            homeGitHubOverview = homeOverviewState.homeGitHubOverview,
            homeBaOverview = homeOverviewState.homeBaOverview,
            visibleOverviewCards = homeOverviewState.visibleOverviewCards,
            pagerScrollEnabled = tabJumpController.pagerScrollEnabled,
            showBottomBar = tabJumpController.showBottomBar,
            nestedScrollConnection = tabJumpController.nestedScrollConnection,
            backdrop = backdrop,
            farJumpAlpha = tabJumpController.farJumpAlpha,
            hasNonHomeBackground = backgroundState.hasNonHomeBackground,
            effectiveNonHomeBackgroundUri = backgroundState.effectiveNonHomeBackgroundUri,
            onPageSelected = scrollSignalController.onPageSelected,
            onActionBarInteractingChanged = tabJumpController.onActionBarInteractingChanged,
            onBottomPageVisibilityChange = onBottomPageVisibilityChange,
            onOverviewCardVisibilityChange = homeOverviewState.onOverviewCardVisibilityChange,
            osScrollToTopSignal = scrollSignalController.osScrollToTopSignal,
            baScrollToTopSignal = scrollSignalController.baScrollToTopSignal,
            mcpScrollToTopSignal = scrollSignalController.mcpScrollToTopSignal,
            githubScrollToTopSignal = scrollSignalController.githubScrollToTopSignal
        )
    }
}
