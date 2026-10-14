@file:Suppress("FunctionName")

package os.kei.ui.page.main.host.pager

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.core.prefs.NonHomeBackgroundAlignment
import os.kei.core.prefs.NonHomeBackgroundContentScale
import os.kei.core.prefs.NonHomeBackgroundPageStyle
import os.kei.core.privilege.PrivilegedShell
import os.kei.mcp.server.McpServerManager
import os.kei.ui.navigation.KeiosRoute
import os.kei.ui.navigation.Navigator
import os.kei.ui.page.main.back.LocalBackNavigationRuntimeState
import os.kei.ui.page.main.back.MainBackNavigationAction
import os.kei.ui.page.main.back.resolveMainBackNavigationAction
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundImage
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundOverlay
import os.kei.ui.page.main.widget.chrome.appManagedBackgroundRender
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundStyles
import os.kei.ui.page.main.widget.chrome.AppScaffold
import os.kei.ui.page.main.widget.chrome.LocalAppManagedSceneBackdrop
import os.kei.ui.page.main.widget.glass.appGripAwareDockTouchObserver
import os.kei.ui.page.main.widget.glass.rememberAppGripAwareDockState
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.testing.KeiOsTestTags
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.theme.MiuixTheme
import os.kei.core.privilege.PrivilegeStatus
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import os.kei.ui.page.main.widget.chrome.AppNavigationPlacement
import os.kei.ui.page.main.widget.chrome.LocalAppNavigationPlacement
import os.kei.ui.page.main.widget.chrome.appNavigationPlacementFor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.appSidebarAvailableAt
import os.kei.ui.page.main.widget.chrome.appTopBarEdgePadding
import androidx.compose.foundation.layout.padding
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.widget.chrome.appNavigationVisible
import os.kei.ui.page.main.widget.glass.LocalLiquidPresentationOwnerActive

@Composable
internal fun MainPagerLayout(
    rootBackHandlersEnabled: Boolean,
    navigator: Navigator,
    settingsReturnToken: Int,
    gripAwareFloatingDockEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    homeDynamicFullEffectEnabled: Boolean,
    preloadingEnabled: Boolean,
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String,
    nonHomeBackgroundOpacity: Float,
    nonHomeBackgroundContentScale: NonHomeBackgroundContentScale,
    nonHomeBackgroundAlignment: NonHomeBackgroundAlignment,
    nonHomeBackgroundPageStyle: NonHomeBackgroundPageStyle,
    nonHomeBackgroundScrim: Float,
    nonHomeBackgroundDepthEnabled: Boolean,
    nonHomeBackgroundSaturation: Float,
    visibleBottomPageNames: Set<String>,
    onVisibleBottomPageNamesChange: (Set<String>) -> Unit,
    privilegeStatus: PrivilegeStatus,
    privilegedShell: PrivilegedShell,
    mcpServerManager: McpServerManager,
    onOpenGuideDetail: (String) -> Unit,
    onOpenBaGuideCatalog: () -> Unit,
    routeAtTop: Boolean,
    onOpenBaCalendarPool: (Int?) -> Unit,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    externalNavigationToken: Int,
    requestedGitHubRefreshToken: Int,
    requestedGitHubActionsTrackId: String?,
    requestedGitHubActionsSheetToken: Int,
    requestedBaAccountId: String?,
    requestedBaAccountToken: Int,
    transientExternalLaunchActive: Boolean,
    onRequestedBottomPageConsumed: () -> Unit,
) {
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val backNavigationRuntime = LocalBackNavigationRuntimeState.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    // iPadOS `sidebarAdaptable`: the same tab bar, held against a different edge once the window is
    // tablet-shaped. The preference is the user's and survives a window too narrow to honour it.
    val windowWidth = LocalConfiguration.current.screenWidthDp.dp
    // Read from prefs, not composition state: this is a choice the user made, and the adaptable style keeps it
    // through a window too narrow to honour it. Losing it on a process restart would make the sidebar something
    // the user has to re-ask for every cold start.
    var sidebarPreferred by remember { mutableStateOf(UiPrefs.isSidebarNavigationPreferred()) }
    val onSidebarPreferredChange: (Boolean) -> Unit =
        remember {
            { preferred: Boolean ->
                sidebarPreferred = preferred
                UiPrefs.setSidebarNavigationPreferred(preferred)
            }
        }
    val navigationPlacement =
        remember(windowWidth, sidebarPreferred) {
            appNavigationPlacementFor(availableWidth = windowWidth, sidebarPreferred = sidebarPreferred)
        }
    val insets = rememberMainPagerInsets(navigationPlacement)
    val floatingDockState = rememberAppGripAwareDockState(gripAwareFloatingDockEnabled)
    val floatingDockSide = floatingDockState.layoutSide(layoutDirection)
    val onOpenSettings =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.Settings) }
        }
    val onOpenAbout =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.About) }
        }
    val openBaGuideCatalog =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.BaGuideCatalog()) }
        }
    val onOpenMcpSkill =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.McpSkill) }
        }
    val onOpenShellRunner =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.OsShellRunner) }
        }
    val onOpenGitHubReleaseList =
        remember(navigator) {
            { trackId: String -> navigator.pushSingleTop(KeiosRoute.GitHubReleaseList(trackId)) }
        }
    val onOpenFdroidVersionList =
        remember(navigator) {
            { trackId: String -> navigator.pushSingleTop(KeiosRoute.FdroidVersionList(trackId)) }
        }
    val onOpenGitHubActionsNotificationHistory =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.GitHubActionsNotificationHistory) }
        }
    val onOpenWebDavSync =
        remember(navigator) {
            { navigator.pushSingleTop(KeiosRoute.WebDavSync) }
        }
    val coordinator =
        rememberMainPagerCoordinator(
            settingsReturnToken = settingsReturnToken,
            transitionAnimationsEnabled = transitionAnimationsEnabled,
            preloadingEnabled = preloadingEnabled,
            nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
            nonHomeBackgroundUri = nonHomeBackgroundUri,
            visibleBottomPageNames = visibleBottomPageNames,
            onVisibleBottomPageNamesChange = onVisibleBottomPageNamesChange,
            mcpServerManager = mcpServerManager,
            requestedBottomPage = requestedBottomPage,
            requestedBottomPageToken = requestedBottomPageToken,
            onRequestedBottomPageConsumed = onRequestedBottomPageConsumed,
        )
    // Keep HDR scoped to the settled, idle Home page so the Window color mode and
    // visual sweep follow one stable runtime gate.
    val homeIndex =
        remember(coordinator.tabs) {
            coordinator.tabs.indexOf(BottomPage.Home)
        }
    val homeHdrEffectActive =
        shouldActivateHomeHdrEffect(
            homeIconHdrEnabled = homeIconHdrEnabled,
            transitionAnimationsEnabled = transitionAnimationsEnabled,
            mainRouteActive = rootBackHandlersEnabled,
            homeIndex = homeIndex,
            settledPage = coordinator.pagerState.settledPage,
            pagerScrollInProgress = coordinator.pagerState.isScrollInProgress,
            navigationActive = coordinator.navigationActive,
        )
    HomeHdrWindowModeEffect(active = homeHdrEffectActive)
    val homePageIndex =
        coordinator.tabs
            .indexOf(BottomPage.Home)
            .takeIf { it >= 0 }
            ?: 0
    val mainBackAction =
        resolveMainBackNavigationAction(
            backStackSize = 1,
            targetPageIndex =
                coordinator.pagerState.targetPage.coerceIn(
                    0,
                    (coordinator.tabs.size - 1).coerceAtLeast(0),
                ),
            homePageIndex = homePageIndex,
        )
    val mainPagerBackGestureState =
        rememberMainPagerHomeBackGestureState(
            enabled = rootBackHandlersEnabled && mainBackAction == MainBackNavigationAction.NavigateHome,
            selectedPageIndex = coordinator.selectedPageIndex,
            homePageIndex = homePageIndex,
        ) {
            coordinator.onPageSelected(homePageIndex)
        }

    // The page composite the pager's glass samples: base colour, background image and readability
    // overlay, recorded once and drawn under every layer that sits on top of it. Without it the chrome
    // refracted a flat token and read as a black plate on a photograph — see
    // [os.kei.ui.page.main.widget.chrome.LocalAppManagedSceneBackdrop].
    val nonHomeBackgroundActive =
        coordinator.pagerRuntime.shouldRenderNonHomeBackground && coordinator.hasNonHomeBackground
    val pageBaseColor = MiuixTheme.colorScheme.background
    val sceneBackdrop =
        if (nonHomeBackgroundActive) {
            rememberLayerBackdrop {
                drawRect(pageBaseColor)
                drawContent()
            }
        } else {
            null
        }
    val bottomBarBackdrop: Backdrop =
        if (sceneBackdrop != null) {
            rememberCombinedBackdrop(sceneBackdrop, coordinator.backdrop)
        } else {
            coordinator.backdrop
        }

    AppScaffold(
        // Transparent so the `background(colorScheme.background)` below is this page's only opaque base,
        // and so it is the *same* base the routes use.
        //
        // `AppScaffold` otherwise defaults to `colorScheme.surface`, which painted straight over that
        // line and made it dead paint — and the two tokens differ: miuix uses `background` =
        // White / `#242424` against `surface` = `#F7F7F7` / Black for light / dark. A custom background
        // image was therefore composited over `surface` here but over `background` inside
        // `AppManagedBackgroundHost`, so one opacity setting produced two different results depending on
        // how deep the page was. Measured in dark theme with one image at 16%: a main page read
        // rgb(30,36,40) where the same pixel on a secondary page read rgb(59,64,69) — a delta of 29
        // against the 36 that `#242424` contributes.
        //
        // Safe only because the routes now paint their own opaque base; while they did not, making this
        // transparent let the pager show through them.
        containerColor = Color.Transparent,
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .appGripAwareDockTouchObserver(
                    enabled = gripAwareFloatingDockEnabled,
                    state = floatingDockState,
                ).background(
                    // Apple, Dark Mode: "the system uses two sets of background colors — called base and
                    // elevated... The base colors are dimmer, making background interfaces appear to
                    // recede, and the elevated colors are brighter."
                    //
                    // miuix ships exactly that pair — `surface` is Black / `#F7F7F7` against `background`
                    // and `surfaceContainer` at `#242424` / White — and this page was painting the
                    // *elevated* one as its base. `AppFeatureCard` fills with `surfaceContainer` at 64%,
                    // which in dark is that same `#242424`, so 64% of it over itself changed nothing:
                    // measured card rgb(42,44,46) against page rgb(36,36,36), a 6-level step held together
                    // only by the card's rim. Routes never had this, because their scaffold paints
                    // `surface` — so this also stops a card looking different on a page than on a route.
                    //
                    // With a managed background the base stays `background`: that is the colour the image
                    // composites over and the readability ceiling is solved for.
                    if (nonHomeBackgroundActive) pageBaseColor else MiuixTheme.colorScheme.surface,
                )
                .nestedScroll(coordinator.nestedScrollConnection),
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
        floatingToolbar = {
            // The phone navigation is visually and behaviorally a floating toolbar: it overlays the page,
            // collapses while reading, and leaves content visible underneath. Let MIUIX own that layer so its
            // snackbar host can keep transient messages above the dock. The regular-width top bar and sidebar
            // remain content overlays because they participate in different adaptive rows.
            if (navigationPlacement == AppNavigationPlacement.Bottom) {
                val safeSelectedPageIndex =
                    coordinator.pagerState.targetPage.coerceIn(
                        0,
                        (coordinator.tabs.size - 1).coerceAtLeast(0),
                    )
                val lastPagePosition = (coordinator.tabs.size - 1).coerceAtLeast(0).toFloat()
                val selectedPagePositionProvider =
                    remember(coordinator.pagerState, mainPagerBackGestureState, lastPagePosition) {
                        {
                            if (mainPagerBackGestureState.inProgress) {
                                mainPagerBackGestureState.selectedPagePosition()
                            } else {
                                coordinator.pagerState.pagePosition
                            }.coerceIn(0f, lastPagePosition)
                        }
                    }
                MainPagerBottomBar(
                    visible = coordinator.showBottomBar,
                    placement = navigationPlacement,
                    navigationBarBottom = insets.navigationBarBottom,
                    topInset = insets.homeTopInset,
                    tabs = coordinator.tabs,
                    selectedPageIndex = safeSelectedPageIndex,
                    selectedPagePosition = null,
                    selectedPagePositionProvider = selectedPagePositionProvider,
                    backdrop = bottomBarBackdrop,
                    onPageSelected = coordinator.onPageSelected,
                    onExpand = coordinator.onShowBottomBar,
                )
            }
        },
    ) { _ ->
      // Published here rather than derived per page: the bar is one overlay owned by the pager, but all five
      // pages are composed at once, so each page's own chrome has to be told to leave the centre of the top
      // row alone. See LocalAppNavigationPlacement.
      CompositionLocalProvider(
          LocalAppNavigationPlacement provides navigationPlacement,
          LocalLiquidPresentationOwnerActive provides rootBackHandlersEnabled,
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coordinator.pagerRuntime.shouldRenderNonHomeBackground) {
                val backgroundStyle =
                    remember(nonHomeBackgroundPageStyle) {
                        AppManagedBackgroundStyles.forPageStyle(nonHomeBackgroundPageStyle)
                    }
                val baseColor = pageBaseColor
                val darkBase = baseColor.luminance() < 0.5f
                val backgroundDepthTranslationPx =
                    remember(density) {
                        with(density) { 18.dp.toPx() }
                    }
                val backgroundDepthPositionProvider =
                    remember(
                        coordinator.pagerState,
                        backgroundDepthTranslationPx,
                        nonHomeBackgroundDepthEnabled,
                    ) {
                        if (nonHomeBackgroundDepthEnabled) {
                            {
                                val relativePageOffset =
                                    (coordinator.pagerState.pagePosition - coordinator.pagerState.settledPage)
                                        .coerceIn(-1f, 1f)
                                -relativePageOffset * backgroundDepthTranslationPx
                            }
                        } else {
                            null
                        }
                    }
                // Same helper the route host uses, so the image alpha and the readability floor cannot
                // drift between the two levels again.
                val render =
                    appManagedBackgroundRender(
                        opacity = nonHomeBackgroundOpacity,
                        style = backgroundStyle,
                        darkBase = darkBase,
                    )
                // Recorded as one composite so the chrome's glass refracts the wallpaper the way it
                // refracts any other content. The base rect lives in the recording block rather than
                // here because `layerBackdrop` draws only `drawContent()` to the screen — the opaque
                // base on screen is the scaffold modifier's `background(colorScheme.background)`.
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .then(
                                if (sceneBackdrop != null) {
                                    Modifier.layerBackdrop(sceneBackdrop)
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    AppManagedBackgroundImage(
                        enabled = coordinator.hasNonHomeBackground,
                        imageUri = coordinator.effectiveNonHomeBackgroundUri,
                        opacity = render.imageOpacity,
                        saturation = nonHomeBackgroundSaturation,
                        contentScale = nonHomeBackgroundContentScale,
                        alignment = nonHomeBackgroundAlignment,
                        motionScale = if (nonHomeBackgroundDepthEnabled) 1.022f else 1f,
                        motionTranslationXProvider = backgroundDepthPositionProvider,
                        modifier = Modifier.fillMaxSize(),
                    )
                    AppManagedBackgroundOverlay(
                        baseColor = baseColor,
                        darkBase = darkBase,
                        style = backgroundStyle,
                        scrim = nonHomeBackgroundScrim,
                        readabilityOverlay = render.readabilityOverlay,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            val pagerModifier =
                Modifier
                    .fillMaxSize()
                    // Nothing is inset for the rail. The pager fills the window so a page's background runs
                    // the full width and the rail floats over it — the background extension effect the
                    // guidance asks for. Content is pushed clear of the rail by appPageSideGutterStart()
                    // instead, which is the same arrangement the top tab bar already uses.
                    .testTag(
                        coordinator.tabs
                            .getOrElse(coordinator.pagerState.settledPage) { BottomPage.Home }
                            .mainPagerSettledTestTag(),
                    )
                    .onSizeChanged { size ->
                        mainPagerBackGestureState.onContainerSizeChanged(size.width, size.height)
                    }.graphicsLayer {
                        val backMotion = mainPagerBackGestureState.motionValues
                        transformOrigin = TransformOrigin(backMotion.pivotX, backMotion.pivotY)
                        translationX = backMotion.translationX
                        scaleX = backMotion.scale
                        scaleY = backMotion.scale
                        alpha = backMotion.contentAlpha
                    }.then(
                        // The backdrop producer records the whole pager into a GraphicsLayer every
                        // frame so glass surfaces can sample it. While another route covers this
                        // one that recording feeds nothing on screen, so stop producing until the
                        // entry is the interactive top again. Nothing visible changes: the gate can
                        // only close behind a full-screen opaque page.
                        if (routeAtTop) Modifier.layerBackdrop(coordinator.backdrop) else Modifier,
                    )
            val activationState =
                rememberMainPageActivationState(
                    tabs = coordinator.tabs,
                    settledPageIndex = coordinator.pagerState.settledPage,
                    targetPageIndex = coordinator.pagerState.targetPage,
                    isPagerScrollInProgress = coordinator.pagerState.isScrollInProgress,
                )
            val pageContent: @Composable (Int) -> Unit = { pageIndex ->
                val pageType = coordinator.tabs[pageIndex]
                key(pageType.name) {
                    // Memoize per-page MainPageRuntime so we don't allocate a fresh
                    // @Immutable instance on every recomposition. During a swipe with
                    // beyondViewportPageCount = 1, ~3 pages recompose every frame —
                    // without this remember, that's ~3 allocations/frame plus broken
                    // equality skip checks downstream into each page composable.
                    // Inputs cover everything pageRuntime() reads.
                    val contentTopPadding =
                        if (pageType == BottomPage.Home) insets.homeTopInset else 0.dp
                    val contentBottomPadding =
                        if (pageType == BottomPage.Home) {
                            insets.homeBottomInset
                        } else {
                            insets.bottomOverlayPadding
                        }
                    val scrollToTopSignal =
                        when (pageType) {
                            BottomPage.Home -> 0
                            BottomPage.Os -> coordinator.osScrollToTopSignal
                            BottomPage.Ba -> coordinator.baScrollToTopSignal
                            BottomPage.Mcp -> coordinator.mcpScrollToTopSignal
                            BottomPage.GitHub -> coordinator.githubScrollToTopSignal
                        }
                    val hasActivated = activationState.hasActivated(pageType)
                    val contentReady = activationState.contentReady(pageType)
                    val contentWorkAllowed =
                        backNavigationRuntime.contentWorkAllowed &&
                            !transientExternalLaunchActive
                    val pageRuntime =
                        remember(
                            coordinator.pagerRuntime,
                            pageIndex,
                            contentTopPadding,
                            contentBottomPadding,
                            coordinator.showBottomBar,
                            floatingDockSide,
                            coordinator.onShowBottomBar,
                            coordinator.onPageScrollBoundsChange,
                            scrollToTopSignal,
                            hasActivated,
                            contentReady,
                            contentWorkAllowed,
                        ) {
                            coordinator.pagerRuntime.pageRuntime(
                                pageIndex = pageIndex,
                                contentTopPadding = contentTopPadding,
                                contentBottomPadding = contentBottomPadding,
                                bottomBarVisible =
                                    appNavigationVisible(
                                        placement = navigationPlacement,
                                        scrolledAway = !coordinator.showBottomBar,
                                    ),
                                floatingDockSide = floatingDockSide,
                                onShowBottomBar = coordinator.onShowBottomBar,
                                onScrollBoundsChange = { canScrollBackward, canScrollForward ->
                                    coordinator.onPageScrollBoundsChange(
                                        pageIndex,
                                        canScrollBackward,
                                        canScrollForward,
                                    )
                                },
                                scrollToTopSignal = scrollToTopSignal,
                                hasActivated = hasActivated,
                                contentReady = contentReady,
                                contentWorkAllowed = contentWorkAllowed,
                            )
                        }
                    val homePageState =
                        if (pageType == BottomPage.Home) {
                            remember(
                                privilegeStatus,
                                homeHdrEffectActive,
                                homeDynamicFullEffectEnabled,
                                routeAtTop,
                                coordinator.visibleTabsSnapshot,
                                coordinator.homeAppOverview,
                                coordinator.homeMcpOverview,
                                coordinator.homeGitHubOverview,
                                coordinator.homeWebDavOverview,
                                coordinator.homeBaOverview,
                                coordinator.homeRuntimeNowMs,
                                coordinator.visibleOverviewCards,
                                coordinator.showCacheFreshnessInCards,
                                coordinator.showHomeBottomPageEditor,
                                coordinator.onBottomPageVisibilityChange,
                                coordinator.onOverviewCardVisibilityChange,
                                coordinator.onCacheFreshnessVisibilityChange,
                                coordinator.onHomeBottomPageEditorVisibleChange,
                                onOpenWebDavSync,
                                onOpenSettings,
                                onOpenAbout,
                            ) {
                                MainPagerHomePageState(
                                    privilegeStatus = privilegeStatus,
                                    homeIconHdrEnabled = homeHdrEffectActive,
                                    homeDynamicFullEffectEnabled = homeDynamicFullEffectEnabled,
                                    pagerAtTop = routeAtTop,
                                    visibleBottomPages = coordinator.visibleTabsSnapshot,
                                    homeAppOverview = coordinator.homeAppOverview,
                                    homeMcpOverview = coordinator.homeMcpOverview,
                                    homeGitHubOverview = coordinator.homeGitHubOverview,
                                    homeWebDavOverview = coordinator.homeWebDavOverview,
                                    homeBaOverview = coordinator.homeBaOverview,
                                    homeRuntimeNowMs = coordinator.homeRuntimeNowMs,
                                    visibleOverviewCards = coordinator.visibleOverviewCards,
                                    showCacheFreshnessInCards = coordinator.showCacheFreshnessInCards,
                                    showHomeBottomPageEditor = coordinator.showHomeBottomPageEditor,
                                    onBottomPageVisibilityChange = coordinator.onBottomPageVisibilityChange,
                                    onOverviewCardVisibilityChange = coordinator.onOverviewCardVisibilityChange,
                                    onCacheFreshnessVisibilityChange = coordinator.onCacheFreshnessVisibilityChange,
                                    onHomeBottomPageEditorVisibleChange = coordinator.onHomeBottomPageEditorVisibleChange,
                                    onOpenWebDavSync = onOpenWebDavSync,
                                    onOpenSettings = onOpenSettings,
                                    onOpenAbout = onOpenAbout,
                                )
                            }
                        } else {
                            null
                        }
                    val osPageState =
                        if (pageType == BottomPage.Os) {
                            remember(privilegeStatus, privilegedShell) {
                                MainPagerOsPageState(
                                    privilegeStatus = privilegeStatus,
                                    privilegedShell = privilegedShell,
                                    onOpenShellRunner = onOpenShellRunner,
                                )
                            }
                        } else {
                            null
                        }
                    val baPageState =
                        if (pageType == BottomPage.Ba) {
                            remember(
                                preloadingEnabled,
                                onOpenGuideDetail,
                                openBaGuideCatalog,
                                requestedBaAccountId,
                                requestedBaAccountToken,
                            ) {
                                MainPagerBaPageState(
                                    preloadingEnabled = preloadingEnabled,
                                    onOpenPoolGuideDetail = onOpenGuideDetail,
                                    onOpenBaGuideCatalog = openBaGuideCatalog,
                                    onOpenBaCalendarPool = onOpenBaCalendarPool,
                                    requestedAccountId = requestedBaAccountId,
                                    requestedAccountToken = requestedBaAccountToken,
                                )
                            }
                        } else {
                            null
                        }
                    val mcpPageState =
                        if (pageType == BottomPage.Mcp) {
                            remember(mcpServerManager, onOpenMcpSkill) {
                                MainPagerMcpPageState(
                                    mcpServerManager = mcpServerManager,
                                    onOpenMcpSkill = onOpenMcpSkill,
                                )
                            }
                        } else {
                            null
                        }
                    val githubPageState =
                        if (pageType == BottomPage.GitHub) {
                            remember(
                                privilegedShell,
                                externalNavigationToken,
                                requestedGitHubRefreshToken,
                                requestedGitHubActionsTrackId,
                                requestedGitHubActionsSheetToken,
                                onOpenGitHubActionsNotificationHistory,
                            ) {
                                MainPagerGitHubPageState(
                                    privilegedShell = privilegedShell,
                                    requestedNavigationToken = externalNavigationToken,
                                    requestedGitHubRefreshToken = requestedGitHubRefreshToken,
                                    requestedGitHubActionsTrackId = requestedGitHubActionsTrackId,
                                    requestedGitHubActionsSheetToken = requestedGitHubActionsSheetToken,
                                    onOpenActionsNotificationHistory = onOpenGitHubActionsNotificationHistory,
                                    onOpenReleaseList = onOpenGitHubReleaseList,
                                    onOpenFdroidVersionList = onOpenFdroidVersionList,
                                )
                            }
                        } else {
                            null
                        }
                    // Home is the one page this background does not apply to — it has its own — so it
                    // must not have the non-Home composite handed to its glass either.
                    CompositionLocalProvider(
                        LocalAppManagedSceneBackdrop provides
                            sceneBackdrop.takeIf { pageType != BottomPage.Home },
                        LocalLiquidPresentationOwnerActive provides
                            (rootBackHandlersEnabled && pageIndex == coordinator.pagerState.targetPage),
                    ) {
                        MainPagerPageHost(
                            pageType = pageType,
                            runtime = pageRuntime,
                            homePageState = homePageState,
                            osPageState = osPageState,
                            baPageState = baPageState,
                            mcpPageState = mcpPageState,
                            githubPageState = githubPageState,
                        )
                    }
                }
            }
            when (val pagerState = coordinator.pagerState) {
                is MainLoadedPagerState -> {
                    MainLoadedPager(
                        state = pagerState,
                        userScrollEnabled = coordinator.pagerScrollEnabled,
                        animationsEnabled = transitionAnimationsEnabled,
                        additionalMotionInProgress = mainPagerBackGestureState.inProgress,
                        modifier = pagerModifier,
                        pageContent = pageContent,
                    )
                }

                else -> {
                    error("Unsupported main pager state: ${pagerState::class.java.name}")
                }
            }
            // Last child, so it draws above the pager. Navigation lives in the Liquid Glass functional layer
            // *over* the content layer, and content peeks through beneath it — drawn first it would simply be
            // covered by the page.
            if (navigationPlacement == AppNavigationPlacement.Sidebar) {
                MainPagerSidebar(
                    tabs = coordinator.tabs,
                    selectedIndex = coordinator.selectedPageIndex,
                    backdrop = bottomBarBackdrop,
                    topInset = insets.homeTopInset,
                    bottomInset = insets.homeBottomInset,
                    onSelected = coordinator.onPageSelected,
                    onConvertToTabBar = { onSidebarPreferredChange(false) },
                )
            }
            if (navigationPlacement == AppNavigationPlacement.Top && appSidebarAvailableAt(windowWidth)) {
                // The adaptable style keeps the button in both shapes. In this one it sits at the leading edge
                // of the top row, ahead of the title, which is the slot iPadOS uses for it.
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = appTopBarEdgePadding(),
                                top = insets.homeTopInset + AppChromeTokens.topBarChromeTopPadding,
                            ),
                ) {
                    MainPagerSidebarToggle(
                        backdrop = bottomBarBackdrop,
                        expanded = false,
                        onClick = { onSidebarPreferredChange(true) },
                    )
                }
            }
            // Top only. In the sidebar shape the rail *is* the tab bar, and drawing both left the page's own
            // title card hidden underneath a bar that no longer had a job.
            if (navigationPlacement == AppNavigationPlacement.Top) {
                MainPagerTopNavigationBar(
                    coordinator = coordinator,
                    placement = navigationPlacement,
                    insets = insets,
                    backGestureState = mainPagerBackGestureState,
                    backdrop = bottomBarBackdrop,
                )
            }
        }
      }
    }
}

@Composable
private fun HomeHdrWindowModeEffect(active: Boolean) {
    val activity = LocalContext.current.findActivity()
    val window = activity?.window
    val targetMode =
        if (active) {
            ActivityInfo.COLOR_MODE_HDR
        } else {
            ActivityInfo.COLOR_MODE_DEFAULT
        }
    LaunchedEffect(window, targetMode) {
        runCatching { window?.colorMode = targetMode }
    }
    DisposableEffect(window) {
        onDispose {
            runCatching { window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT }
        }
    }
}

internal fun shouldActivateHomeHdrEffect(
    homeIconHdrEnabled: Boolean,
    transitionAnimationsEnabled: Boolean,
    mainRouteActive: Boolean,
    homeIndex: Int,
    settledPage: Int,
    pagerScrollInProgress: Boolean,
    navigationActive: Boolean,
): Boolean =
    homeIconHdrEnabled &&
        transitionAnimationsEnabled &&
        mainRouteActive &&
        homeIndex >= 0 &&
        settledPage == homeIndex &&
        !pagerScrollInProgress &&
        !navigationActive

private fun BottomPage.mainPagerSettledTestTag(): String =
    when (this) {
        BottomPage.Home -> KeiOsTestTags.MainPagerSettledHome
        BottomPage.Os -> KeiOsTestTags.MainPagerSettledOs
        BottomPage.Mcp -> KeiOsTestTags.MainPagerSettledMcp
        BottomPage.GitHub -> KeiOsTestTags.MainPagerSettledGitHub
        BottomPage.Ba -> KeiOsTestTags.MainPagerSettledBa
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
