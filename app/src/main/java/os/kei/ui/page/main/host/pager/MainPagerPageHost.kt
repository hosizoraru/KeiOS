@file:Suppress("FunctionName")

package os.kei.ui.page.main.host.pager

import androidx.activity.compose.ReportDrawn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import os.kei.core.privilege.PrivilegedShell
import os.kei.feature.home.model.HomeAppOverview
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.feature.home.model.HomeWebDavOverview
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.ba.BAPage
import os.kei.ui.page.main.github.page.GitHubPage
import os.kei.ui.page.main.home.HomePage
import os.kei.ui.page.main.mcp.McpPage
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.os.OsPage
import os.kei.ui.page.main.widget.chrome.LocalAppManagedSceneBackdrop
import os.kei.ui.page.main.widget.chrome.LocalAppScaffoldContainerColor
import os.kei.ui.page.main.widget.glass.GlassEffectRuntime
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.testing.KeiOsTestTags
import os.kei.core.privilege.PrivilegeStatus
import os.kei.LocalMainStartupTransition

@Immutable
internal data class MainPagerHomePageState(
    val privilegeStatus: PrivilegeStatus,
    val homeIconHdrEnabled: Boolean,
    val homeDynamicFullEffectEnabled: Boolean,
    /**
     * Whether the pager is still the top nav entry, from `rememberNavEntryAtTop`.
     *
     * A preference decides whether the background *may* drift; this decides whether anyone can see
     * it. Behind a settled full-screen route nobody can -- see [os.kei.ui.page.main.home.HomePage].
     */
    val pagerAtTop: Boolean,
    val visibleBottomPages: Set<BottomPage>,
    val homeAppOverview: HomeAppOverview,
    val homeMcpOverview: HomeMcpOverview,
    val homeGitHubOverview: HomeGitHubOverview,
    val homeWebDavOverview: HomeWebDavOverview,
    val homeBaOverview: HomeBaOverview,
    val homeRuntimeNowMs: Long,
    val visibleOverviewCards: Set<HomeOverviewCard>,
    val showCacheFreshnessInCards: Boolean,
    val showHomeBottomPageEditor: Boolean,
    val onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    val onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit,
    val onCacheFreshnessVisibilityChange: (Boolean) -> Unit,
    val onHomeBottomPageEditorVisibleChange: (Boolean) -> Unit,
    val onOpenWebDavSync: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenAbout: () -> Unit,
)

@Stable
internal data class MainPagerOsPageState(
    val privilegeStatus: PrivilegeStatus,
    val privilegedShell: PrivilegedShell,
    val onOpenShellRunner: () -> Unit,
)

@Immutable
internal data class MainPagerBaPageState(
    val preloadingEnabled: Boolean,
    val onOpenPoolGuideDetail: (String) -> Unit,
    val onOpenBaGuideCatalog: () -> Unit,
    val onOpenBaCalendarPool: (Int?) -> Unit,
    val requestedAccountId: String?,
    val requestedAccountToken: Int,
)

@Stable
internal data class MainPagerMcpPageState(
    val mcpServerManager: McpServerManager,
    val onOpenMcpSkill: () -> Unit,
)

@Stable
internal data class MainPagerGitHubPageState(
    val privilegedShell: PrivilegedShell,
    val requestedNavigationToken: Int,
    val requestedGitHubRefreshToken: Int,
    val requestedGitHubActionsTrackId: String?,
    val requestedGitHubActionsSheetToken: Int,
    val onOpenActionsNotificationHistory: () -> Unit,
    val onOpenReleaseList: (String) -> Unit,
    val onOpenFdroidVersionList: (String) -> Unit,
)

@Composable
internal fun MainPagerPageHost(
    pageType: BottomPage,
    runtime: MainPageRuntime,
    homePageState: MainPagerHomePageState?,
    osPageState: MainPagerOsPageState?,
    baPageState: MainPagerBaPageState?,
    mcpPageState: MainPagerMcpPageState?,
    githubPageState: MainPagerGitHubPageState?,
) {
    val startupTransition = LocalMainStartupTransition.current
    if (runtime.isSettledDataActive) {
        SideEffect { startupTransition?.onMainPageReady() }
    }
    val glassRuntime = remember { GlassEffectRuntime() }
    CompositionLocalProvider(
        LocalAppScaffoldContainerColor provides
            mainPagerPageContainerColorOverride(
                pageType = pageType,
                managedBackgroundActive = LocalAppManagedSceneBackdrop.current != null,
            ),
        LocalGlassEffectRuntime provides glassRuntime,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(pageType.pageRootTestTag()),
        ) {
            if (!shouldRenderMainPagerPageContent(pageType, runtime)) {
                return@Box
            }
            when (pageType) {
                BottomPage.Home -> {
                    val homeState =
                        checkNotNull(homePageState) {
                            "Home page state is required for the Home tab"
                        }
                    // Reaching the real Home branch means the retained pager state and the first
                    // meaningful app surface are ready for this frame. StartupTimingMetric can now
                    // report timeToFullDisplay instead of falling back to initial display.
                    ReportDrawn()
                    HomePage(
                        privilegeStatus = homeState.privilegeStatus,
                        homeAppOverview = homeState.homeAppOverview,
                        mcpOverview = homeState.homeMcpOverview,
                        homeGitHubOverview = homeState.homeGitHubOverview,
                        homeWebDavOverview = homeState.homeWebDavOverview,
                        homeBaOverview = homeState.homeBaOverview,
                        runtimeNowMs = homeState.homeRuntimeNowMs,
                        homeIconHdrEnabled = homeState.homeIconHdrEnabled,
                        homeDynamicFullEffectEnabled = homeState.homeDynamicFullEffectEnabled,
                        pagerAtTop = homeState.pagerAtTop,
                        runtime = runtime,
                        visibleBottomPages = homeState.visibleBottomPages,
                        visibleOverviewCards = homeState.visibleOverviewCards,
                        showCacheFreshnessInCards = homeState.showCacheFreshnessInCards,
                        showBottomPageEditor = homeState.showHomeBottomPageEditor,
                        onBottomPageVisibilityChange = homeState.onBottomPageVisibilityChange,
                        onOverviewCardVisibilityChange = homeState.onOverviewCardVisibilityChange,
                        onCacheFreshnessVisibilityChange = homeState.onCacheFreshnessVisibilityChange,
                        onBottomPageEditorVisibleChange = homeState.onHomeBottomPageEditorVisibleChange,
                        onOpenWebDavSync = homeState.onOpenWebDavSync,
                        onOpenSettings = homeState.onOpenSettings,
                        onOpenAbout = homeState.onOpenAbout,
                    )
                }

                BottomPage.Os -> {
                    val osState =
                        checkNotNull(osPageState) {
                            "OS page state is required for the OS tab"
                        }
                    OsPage(
                        runtime = runtime,
                        privilegeStatus = osState.privilegeStatus,
                        privilegedShell = osState.privilegedShell,
                        onOpenShellRunner = osState.onOpenShellRunner,
                    )
                }

                BottomPage.Ba -> {
                    val baState =
                        checkNotNull(baPageState) {
                            "BA page state is required for the BA tab"
                        }
                    BAPage(
                        runtime = runtime,
                        preloadingEnabled = baState.preloadingEnabled,
                        onOpenPoolStudentGuide = baState.onOpenPoolGuideDetail,
                        onOpenGuideCatalog = baState.onOpenBaGuideCatalog,
                        onOpenCalendarPool = baState.onOpenBaCalendarPool,
                        requestedAccountId = baState.requestedAccountId,
                        requestedAccountToken = baState.requestedAccountToken,
                    )
                }

                BottomPage.Mcp -> {
                    val mcpState =
                        checkNotNull(mcpPageState) {
                            "MCP page state is required for the MCP tab"
                        }
                    McpPage(
                        mcpServerManager = mcpState.mcpServerManager,
                        runtime = runtime,
                        onOpenSkill = mcpState.onOpenMcpSkill,
                    )
                }

                BottomPage.GitHub -> {
                    val githubState =
                        checkNotNull(githubPageState) {
                            "GitHub page state is required for the GitHub tab"
                        }
                    GitHubPage(
                        privilegedShell = githubState.privilegedShell,
                        runtime = runtime,
                        externalNavigationTriggerToken = githubState.requestedNavigationToken,
                        externalRefreshTriggerToken = githubState.requestedGitHubRefreshToken,
                        externalActionsTrackId = githubState.requestedGitHubActionsTrackId,
                        externalActionsSheetToken = githubState.requestedGitHubActionsSheetToken,
                        onOpenActionsNotificationHistory = githubState.onOpenActionsNotificationHistory,
                        onOpenReleaseList = githubState.onOpenReleaseList,
                        onOpenFdroidVersionList = githubState.onOpenFdroidVersionList,
                    )
                }
            }
        }
    }
}

internal fun shouldRenderMainPagerPageContent(
    pageType: BottomPage,
    runtime: MainPageRuntime,
): Boolean =
    pageType == BottomPage.Home ||
        runtime.hasActivated ||
        runtime.isWarmActive

/**
 * Transparent only while something is actually painting behind the page.
 *
 * This used to be unconditional for every non-Home page, which made `appManagedPageBackgroundActive()`
 * read true with no background in sight and, worse, made `appPageBackdropBaseColor()` resolve to the
 * *elevated* token as the page's base — leaving cards a 6-level step to stand on. With no background the
 * page keeps its own `surface`, so the base/elevated pairing holds and a card reads as a card.
 */
internal fun mainPagerPageContainerColorOverride(
    pageType: BottomPage,
    managedBackgroundActive: Boolean,
): Color? =
    if (pageType == BottomPage.Home || !managedBackgroundActive) null else Color.Transparent

private fun BottomPage.pageRootTestTag(): String =
    when (this) {
        BottomPage.Home -> KeiOsTestTags.HomePageRoot
        BottomPage.Os -> KeiOsTestTags.OsPageRoot
        BottomPage.Mcp -> KeiOsTestTags.McpPageRoot
        BottomPage.GitHub -> KeiOsTestTags.GitHubPageRoot
        BottomPage.Ba -> KeiOsTestTags.BaPageRoot
    }
