package os.kei.ui.page.main.host.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.mcp.server.McpServerManager
import os.kei.core.system.ShizukuApiUtils
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.ui.page.main.ba.BAPage
import os.kei.ui.page.main.github.page.GitHubPage
import os.kei.ui.page.main.home.HomePage
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.mcp.McpPage
import os.kei.ui.page.main.os.OsPage
import os.kei.ui.page.main.widget.glass.GlassEffectRuntime
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.glass.rememberGlassReductionProgress

@Composable
internal fun MainPagerPageHost(
    pageType: BottomPage,
    pageIndex: Int,
    pagerRuntime: MainPagerRuntimeSnapshot,
    visibleBottomPages: Set<BottomPage>,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    cardPressFeedbackEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    homeDynamicFullEffectEnabled: Boolean,
    preloadingEnabled: Boolean,
    mcpServerManager: McpServerManager,
    homeMcpOverview: HomeMcpOverview,
    homeGitHubOverview: HomeGitHubOverview,
    homeBaOverview: HomeBaOverview,
    visibleOverviewCards: Set<HomeOverviewCard>,
    homeTopInset: Dp,
    homeBottomInset: Dp,
    bottomOverlayPadding: Dp,
    requestedGitHubRefreshToken: Int,
    osScrollToTopSignal: Int,
    baScrollToTopSignal: Int,
    mcpScrollToTopSignal: Int,
    githubScrollToTopSignal: Int,
    onActivePageListScrollInProgressChanged: (Boolean) -> Unit,
    onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPoolGuideDetail: (String) -> Unit,
    onOpenBaGuideCatalog: () -> Unit,
    onOpenMcpSkill: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    val runtime = pagerRuntime.pageRuntime(
        pageIndex = pageIndex,
        contentTopPadding = if (pageType == BottomPage.Home) homeTopInset else 0.dp,
        contentBottomPadding = if (pageType == BottomPage.Home) homeBottomInset else bottomOverlayPadding,
        scrollToTopSignal = when (pageType) {
            BottomPage.Home -> 0
            BottomPage.Os -> osScrollToTopSignal
            BottomPage.Ba -> baScrollToTopSignal
            BottomPage.Mcp -> mcpScrollToTopSignal
            BottomPage.GitHub -> githubScrollToTopSignal
        }
    )
    val reducedGlassProgress = rememberGlassReductionProgress(
        reduceEffectsDuringMotion = runtime.isPagerScrollInProgress &&
            !(pageType == BottomPage.Home && homeDynamicFullEffectEnabled),
        label = "mainPagerGlassEffectProgress"
    )
    CompositionLocalProvider(
        LocalGlassEffectRuntime provides GlassEffectRuntime(reducedProgress = reducedGlassProgress)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (pageType) {
                BottomPage.Home -> {
                    HomePage(
                        shizukuStatus = shizukuStatus,
                        mcpOverview = homeMcpOverview,
                        homeGitHubOverview = homeGitHubOverview,
                        homeBaOverview = homeBaOverview,
                        homeIconHdrEnabled = homeIconHdrEnabled,
                        homeDynamicFullEffectEnabled = homeDynamicFullEffectEnabled,
                        runtime = runtime,
                        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        visibleBottomPages = visibleBottomPages,
                        visibleOverviewCards = visibleOverviewCards,
                        onBottomPageVisibilityChange = onBottomPageVisibilityChange,
                        onOverviewCardVisibilityChange = onOverviewCardVisibilityChange,
                        onOpenSettings = onOpenSettings,
                        onOpenAbout = onOpenAbout,
                        onActionBarInteractingChanged = onActionBarInteractingChanged
                    )
                }

                BottomPage.Os -> {
                    OsPage(
                        runtime = runtime,
                        shizukuStatus = shizukuStatus,
                        shizukuApiUtils = shizukuApiUtils,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        onActionBarInteractingChanged = onActionBarInteractingChanged
                    )
                }

                BottomPage.Ba -> {
                    BAPage(
                        runtime = runtime,
                        preloadingEnabled = preloadingEnabled,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        onOpenPoolStudentGuide = onOpenPoolGuideDetail,
                        onOpenGuideCatalog = onOpenBaGuideCatalog,
                        onActionBarInteractingChanged = onActionBarInteractingChanged
                    )
                }

                BottomPage.Mcp -> {
                    McpPage(
                        mcpServerManager = mcpServerManager,
                        runtime = runtime,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        onOpenSkill = onOpenMcpSkill,
                        onActionBarInteractingChanged = onActionBarInteractingChanged
                    )
                }

                BottomPage.GitHub -> {
                    GitHubPage(
                        runtime = runtime,
                        externalRefreshTriggerToken = requestedGitHubRefreshToken,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        onActionBarInteractingChanged = onActionBarInteractingChanged,
                        onListScrollInProgressChanged = onActivePageListScrollInProgressChanged
                    )
                }
            }
        }
    }
}
