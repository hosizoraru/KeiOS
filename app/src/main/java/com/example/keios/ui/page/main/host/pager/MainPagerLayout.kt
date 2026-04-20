package com.example.keios.ui.page.main.host.pager

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
import com.example.keios.core.log.AppLogger
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.mcp.server.McpServerManager
import com.example.keios.ui.navigation.KeiosRoute
import com.example.keios.ui.navigation.Navigator
import com.example.keios.ui.page.main.home.model.HomeBaOverview
import com.example.keios.ui.page.main.home.model.HomeGitHubOverview
import com.example.keios.ui.page.main.home.model.HomeMcpOverview
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.glass.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.motion.AppMotionTokens
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import com.example.keios.ui.perf.ReportPagerPerformanceState
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
                    homeTopInset = insets.homeTopInset,
                    homeBottomInset = insets.homeBottomInset,
                    bottomOverlayPadding = insets.bottomOverlayPadding,
                    requestedGitHubRefreshToken = requestedGitHubRefreshToken,
                    osScrollToTopSignal = coordinator.osScrollToTopSignal,
                    baScrollToTopSignal = coordinator.baScrollToTopSignal,
                    mcpScrollToTopSignal = coordinator.mcpScrollToTopSignal,
                    githubScrollToTopSignal = coordinator.githubScrollToTopSignal,
                    onBottomPageVisibilityChange = coordinator.onBottomPageVisibilityChange,
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
