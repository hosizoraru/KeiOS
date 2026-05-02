package os.kei.ui.page.main.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.ui.effect.background.BgEffectBackground
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.feature.home.model.defaultHomeOverviewCards
import os.kei.ui.page.main.home.state.rememberHomePageContentState
import os.kei.ui.page.main.home.state.rememberHomePageHeroMotionState
import os.kei.ui.page.main.home.state.rememberHomePageOverviewCardState
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.osLucideSettingsIcon
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberActionBarBackdrop

@Composable
fun HomePage(
    shizukuStatus: String,
    mcpOverview: HomeMcpOverview = HomeMcpOverview(),
    homeGitHubOverview: HomeGitHubOverview = HomeGitHubOverview(),
    homeBaOverview: HomeBaOverview = HomeBaOverview(),
    homeIconHdrEnabled: Boolean,
    homeDynamicFullEffectEnabled: Boolean = false,
    runtime: MainPageRuntime = MainPageRuntime(),
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    visibleBottomPages: Set<BottomPage>,
    visibleOverviewCards: Set<HomeOverviewCard> = defaultHomeOverviewCards(),
    onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val layoutDirection = LocalLayoutDirection.current
    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    val blurEnabled = isRenderEffectSupported()
    val shaderSupported = isRuntimeShaderSupported()
    val effectBackgroundEnabled = shaderSupported && runtime.isPageActive
    val homeDynamicActive = runtime.isDataActive ||
        (homeDynamicFullEffectEnabled && runtime.isPageActive)
    val dynamicBackgroundEnabled = shaderSupported &&
        homeDynamicActive &&
        (homeDynamicFullEffectEnabled || !runtime.isPagerScrollInProgress)
    val fullBackdropEffectsEnabled = runtime.isPageActive &&
        (
            homeDynamicFullEffectEnabled ||
                (!runtime.isPagerScrollInProgress && !lazyListState.isScrollInProgress)
            )
    val surfaceColor = MiuixTheme.colorScheme.surface
    val actionBarBackdrop = rememberActionBarBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val homeCardBackdrop = if (fullBackdropEffectsEnabled) {
        rememberActionBarBackdrop {
            drawContent()
        }
    } else {
        actionBarBackdrop
    }
    val contentState = rememberHomePageContentState(
        shizukuStatus = shizukuStatus,
        mcpOverview = mcpOverview,
        githubOverview = homeGitHubOverview,
        baOverview = homeBaOverview
    )

    var actionBarSelectedIndex by rememberSaveable { mutableIntStateOf(1) }
    var showBottomPageEditor by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    val layersIcon = appLucideLayersIcon()
    val aboutIcon = appLucideInfoIcon()
    val settingsIcon = osLucideSettingsIcon()
    val editBottomPagesContentDescription = stringResource(R.string.home_cd_edit_bottom_pages)
    val aboutContentDescription = stringResource(R.string.about_page_title)
    val settingsContentDescription = stringResource(R.string.settings_title)
    val homeActionItems = remember(
        editBottomPagesContentDescription,
        aboutContentDescription,
        settingsContentDescription,
        onOpenAbout,
        onOpenSettings
    ) {
        listOf(
            LiquidActionItem(
                icon = layersIcon,
                contentDescription = editBottomPagesContentDescription,
                onClick = {
                    actionBarSelectedIndex = 0
                    showBottomPageEditor = true
                }
            ),
            LiquidActionItem(
                icon = aboutIcon,
                contentDescription = aboutContentDescription,
                onClick = {
                    actionBarSelectedIndex = 1
                    onOpenAbout()
                }
            ),
            LiquidActionItem(
                icon = settingsIcon,
                contentDescription = settingsContentDescription,
                onClick = {
                    actionBarSelectedIndex = 2
                    onOpenSettings()
                }
            )
        )
    }
    val hiddenOverviewCardCount = (HomeOverviewCard.entries.size - visibleOverviewCards.size).coerceAtLeast(0)
    val heroMotionState = rememberHomePageHeroMotionState(
        lazyListState = lazyListState,
        homeIconHdrEnabled = homeIconHdrEnabled,
        runtime = runtime,
        hiddenOverviewCardCount = hiddenOverviewCardCount
    )
    val overviewCardState = rememberHomePageOverviewCardState(
        homeStatusMcp = contentState.homeStatusMcp,
        homeStatusGitHub = contentState.homeStatusGitHub,
        homeStatusBa = contentState.homeStatusBa,
        homeStatusShizuku = contentState.homeStatusShizuku,
        mcpRunning = mcpOverview.running,
        cacheStateColor = contentState.cacheStateColor,
        baLoaded = homeBaOverview.loaded,
        baActivated = homeBaOverview.activated,
        shizukuGranted = contentState.shizukuGranted,
        runningColor = contentState.runningColor,
        stoppedColor = contentState.stoppedColor,
        inactiveColor = contentState.inactiveColor,
        homeStatStatus = contentState.homeStatStatus,
        mcpStatusText = contentState.mcpStatusText,
        homeStatRuntime = contentState.homeStatRuntime,
        mcpRuntimeText = contentState.mcpRuntimeText,
        homeStatClients = contentState.homeStatClients,
        mcpConnectedClients = contentState.mcpConnectedClients,
        homeStatNetwork = contentState.homeStatNetwork,
        networkModeText = contentState.networkModeText,
        homeStatPort = contentState.homeStatPort,
        mcpPort = contentState.mcpPort,
        homeStatPath = contentState.homeStatPath,
        mcpEndpointPath = contentState.mcpEndpointPath,
        homeStatService = contentState.homeStatService,
        mcpServerName = contentState.mcpServerName,
        homeStatToken = contentState.homeStatToken,
        mcpTokenStatusText = contentState.mcpTokenStatusText,
        homeStatStableUpdates = contentState.homeStatStableUpdates,
        githubUpdatableLine = contentState.githubUpdatableLine,
        homeStatPreReleaseUpdates = contentState.homeStatPreReleaseUpdates,
        githubPreReleaseUpdateLine = contentState.githubPreReleaseUpdateLine,
        homeStatFailed = contentState.homeStatFailed,
        githubFailedLine = contentState.githubFailedLine,
        homeStatTracked = contentState.homeStatTracked,
        trackedCountLine = contentState.trackedCountLine,
        homeStatCached = contentState.homeStatCached,
        cacheHitCountLine = contentState.cacheHitCountLine,
        homeStatShare = contentState.homeStatShare,
        githubShareLine = contentState.githubShareLine,
        homeStatStrategy = contentState.homeStatStrategy,
        githubStrategyText = contentState.githubStrategyText,
        homeStatApi = contentState.homeStatApi,
        githubApiText = contentState.githubApiText,
        homeStatLastUpdate = contentState.homeStatLastUpdate,
        githubLastUpdateLine = contentState.githubLastUpdateLine,
        baActivationLine = contentState.baActivationLine,
        homeStatAp = contentState.homeStatAp,
        baApLine = contentState.baApLine,
        homeStatCafeAp = contentState.homeStatCafeAp,
        baCafeApLine = contentState.baCafeApLine,
        homeStatApRemaining = contentState.homeStatApRemaining,
        baApRemainingLine = contentState.baApRemainingLine,
        homeStatBaServer = contentState.homeStatBaServer,
        baServerLine = contentState.baServerLine,
        homeStatBaNotify = contentState.homeStatBaNotify,
        baNotifyLine = contentState.baNotifyLine
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = "",
                    scrollBehavior = topAppBarScrollBehavior,
                    color = Color.Transparent,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = heroMotionState.topBarProgress),
                )
            }
        ) { innerPadding ->
            HomePageControlSheet(
                show = showBottomPageEditor,
                actionBarBackdrop = actionBarBackdrop,
                visibleBottomPages = visibleBottomPages,
                visibleOverviewCards = visibleOverviewCards,
                homeSheetTitle = stringResource(R.string.home_sheet_bottom_pages_title),
                visiblePagesTitle = stringResource(R.string.home_sheet_visible_pages_title),
                visiblePagesDesc = stringResource(R.string.home_sheet_visible_pages_desc),
                visibleCardsTitle = contentState.homeVisibleCardsTitle,
                visibleCardsDesc = contentState.homeVisibleCardsDesc,
                homeCardMcp = contentState.homeCardMcp,
                homeCardGitHub = contentState.homeCardGitHub,
                homeCardBa = contentState.homeCardBa,
                onDismissRequest = { showBottomPageEditor = false },
                onBottomPageVisibilityChange = onBottomPageVisibilityChange,
                onOverviewCardVisibilityChange = onOverviewCardVisibilityChange
            )

            val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val listContentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding() + runtime.contentTopPadding,
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding() + runtime.contentBottomPadding + 16.dp
            )
            val logoPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + runtime.contentTopPadding + 24.dp,
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
            )

            BgEffectBackground(
                dynamicBackground = dynamicBackgroundEnabled,
                modifier = Modifier.fillMaxSize(),
                bgModifier = Modifier,
                effectBackground = effectBackgroundEnabled,
                alpha = heroMotionState.bgAlpha,
            ) {
                HomePageHero(
                    homeIconHdrEnabled = homeIconHdrEnabled,
                    hdrSweepProgress = heroMotionState.hdrSweepProgress,
                    homeHeaderSinkOffset = heroMotionState.homeHeaderSinkOffset,
                    logoPadding = logoPadding,
                    layoutDirection = layoutDirection,
                    homeAppName = contentState.homeAppName,
                    homeTagline = contentState.homeTagline,
                    appVersionText = contentState.appVersionText,
                    avoidanceProgress = heroMotionState.avoidanceProgress,
                    iconProgress = heroMotionState.iconProgress,
                    titleProgress = heroMotionState.titleProgress,
                    summaryProgress = heroMotionState.summaryProgress,
                    statusPills = overviewCardState.homeHeaderStatusPills,
                    onHeroHeightChanged = heroMotionState.onHeroHeightPxChanged,
                    onIconBottomChanged = heroMotionState.onIconBottomChanged,
                    onTitleBottomChanged = heroMotionState.onTitleBottomChanged,
                    onSummaryBottomChanged = heroMotionState.onSummaryBottomChanged
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    contentPadding = listContentPadding,
                ) {
                    item(key = "logo_spacer") {
                        HomePageHeroSpacer(
                            logoHeightDp = heroMotionState.logoHeightDp,
                            logoPadding = logoPadding,
                            listContentPadding = listContentPadding,
                            homeHeaderSinkOffset = heroMotionState.homeHeaderSinkOffset,
                            onLogoHeightPxChanged = heroMotionState.onLogoHeightPxChanged,
                            onLogoAreaBottomChanged = heroMotionState.onLogoAreaBottomChanged
                        )
                    }

                    item(key = "home_content") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = listContentPadding.calculateBottomPadding())
                        ) {
                            HomePageOverviewCards(
                                visibleOverviewCards = visibleOverviewCards,
                                homeCardBackdrop = homeCardBackdrop,
                                blurEnabled = blurEnabled,
                                homeNa = contentState.homeNa,
                                homeCardMcp = contentState.homeCardMcp,
                                mcpStats = overviewCardState.mcpOverviewStats,
                                homeCardGitHub = contentState.homeCardGitHub,
                                githubStats = overviewCardState.githubOverviewStats,
                                homeCardBa = contentState.homeCardBa,
                                baStats = overviewCardState.baOverviewStats
                            )
                        }
                    }
                }
            }
        }

        AppTopEndActionBarOverlay {
            LiquidActionBar(
                backdrop = actionBarBackdrop,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                reduceEffectsDuringPagerScroll = runtime.isPagerScrollInProgress &&
                    !homeDynamicFullEffectEnabled,
                items = homeActionItems,
                selectedIndex = actionBarSelectedIndex,
                onInteractionChanged = onActionBarInteractingChanged
            )
        }
    }
}
