package os.kei.ui.page.main.host.pager

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import os.kei.core.log.AppLogger
import os.kei.core.system.ShizukuApiUtils
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.mcp.server.McpServerManager
import os.kei.ui.navigation.KeiosRoute
import os.kei.ui.navigation.Navigator
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import os.kei.ui.perf.ReportPagerPerformanceState
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MainPagerLayout(
    navigator: Navigator,
    settingsReturnToken: Int,
    liquidBottomBarEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    cardPressFeedbackEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    preloadingEnabled: Boolean,
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String,
    nonHomeBackgroundOpacity: Float,
    visibleBottomPageNames: Set<String>,
    onVisibleBottomPageNamesChange: (Set<String>) -> Unit,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager,
    onOpenGuideDetail: (String) -> Unit,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    requestedGitHubRefreshToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
) {
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val context = LocalContext.current
    val insets = rememberMainPagerInsets()
    val coordinator = rememberMainPagerCoordinator(
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
        onRequestedBottomPageConsumed = onRequestedBottomPageConsumed
    )
    DisposableEffect(
        context,
        homeIconHdrEnabled,
        coordinator.tabs,
        coordinator.pagerState.currentPage,
        coordinator.pagerState.targetPage,
        coordinator.pagerState.settledPage
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onDispose { }
        } else {
            val activity = context as? Activity
            val homeVisibleInPager = listOf(
                coordinator.pagerState.currentPage,
                coordinator.pagerState.targetPage,
                coordinator.pagerState.settledPage
            ).any { pageIndex ->
                coordinator.tabs.getOrElse(pageIndex) { BottomPage.Home } == BottomPage.Home
            }
            runCatching {
                activity?.window?.colorMode = if (homeIconHdrEnabled && homeVisibleInPager) {
                    ActivityInfo.COLOR_MODE_HDR
                } else {
                    ActivityInfo.COLOR_MODE_DEFAULT
                }
            }
            onDispose {
                runCatching {
                    activity?.window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .nestedScroll(coordinator.nestedScrollConnection),
        bottomBar = {
            MainPagerBottomBar(
                visible = coordinator.showBottomBar,
                navigationBarBottom = insets.navigationBarBottom,
                tabs = coordinator.tabs,
                selectedPageIndex = coordinator.pagerState.targetPage,
                selectedPageIndexProvider = { coordinator.pagerState.targetPage },
                backdrop = coordinator.backdrop,
                reduceEffectsDuringPagerScroll = coordinator.pagerState.isScrollInProgress,
                liquidBottomBarEnabled = liquidBottomBarEnabled,
                onPageSelected = coordinator.onPageSelected
            )
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = coordinator.pagerState,
                key = { index -> coordinator.tabs[index].name },
                userScrollEnabled = coordinator.pagerScrollEnabled,
                overscrollEffect = null,
                beyondViewportPageCount = coordinator.pagerRuntime.resolvedBeyondViewportPageCount,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coordinator.farJumpAlpha }
                    .layerBackdrop(coordinator.backdrop)
            ) { pageIndex ->
                MainPagerPageHost(
                    pageType = coordinator.tabs[pageIndex],
                    pageIndex = pageIndex,
                    pagerRuntime = coordinator.pagerRuntime,
                    visibleBottomPages = coordinator.visibleTabsSnapshot,
                    shizukuStatus = shizukuStatus,
                    shizukuApiUtils = shizukuApiUtils,
                    cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    homeIconHdrEnabled = homeIconHdrEnabled,
                    preloadingEnabled = preloadingEnabled,
                    mcpServerManager = mcpServerManager,
                    homeMcpOverview = coordinator.homeMcpOverview,
                    homeGitHubOverview = coordinator.homeGitHubOverview,
                    homeBaOverview = coordinator.homeBaOverview,
                    visibleOverviewCards = coordinator.visibleOverviewCards,
                    homeTopInset = insets.homeTopInset,
                    homeBottomInset = insets.homeBottomInset,
                    bottomOverlayPadding = insets.bottomOverlayPadding,
                    requestedGitHubRefreshToken = requestedGitHubRefreshToken,
                    osScrollToTopSignal = coordinator.osScrollToTopSignal,
                    baScrollToTopSignal = coordinator.baScrollToTopSignal,
                    mcpScrollToTopSignal = coordinator.mcpScrollToTopSignal,
                    githubScrollToTopSignal = coordinator.githubScrollToTopSignal,
                    onBottomPageVisibilityChange = coordinator.onBottomPageVisibilityChange,
                    onOverviewCardVisibilityChange = coordinator.onOverviewCardVisibilityChange,
                    onOpenSettings = { navigator.pushSingleTop(KeiosRoute.Settings) },
                    onOpenAbout = { navigator.pushSingleTop(KeiosRoute.About) },
                    onOpenPoolGuideDetail = onOpenGuideDetail,
                    onOpenBaGuideCatalog = { navigator.pushSingleTop(KeiosRoute.BaGuideCatalog) },
                    onOpenMcpSkill = { navigator.pushSingleTop(KeiosRoute.McpSkill) },
                    onActionBarInteractingChanged = coordinator.onActionBarInteractingChanged
                )
            }

            if (coordinator.pagerRuntime.shouldRenderNonHomeBackground) {
                NonHomePageBackground(
                    enabled = coordinator.hasNonHomeBackground,
                    imageUri = coordinator.effectiveNonHomeBackgroundUri,
                    opacity = nonHomeBackgroundOpacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NonHomePageBackground(
    enabled: Boolean,
    imageUri: String,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    if (!enabled || imageUri.isBlank()) return
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val (targetWidthPx, targetHeightPx) = remember(configuration, density) {
        with(density) {
            val width = configuration.screenWidthDp.dp.roundToPx().coerceAtLeast(1)
            val height = configuration.screenHeightDp.dp.roundToPx().coerceAtLeast(1)
            width to height
        }
    }
    val request = remember(imageUri, targetWidthPx, targetHeightPx) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .size(targetWidthPx, targetHeightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        alpha = opacity.coerceIn(0f, 1f),
        modifier = modifier
    )
}
