package os.kei.ui.page.main.home.state

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.home.HomeCardStatItem
import os.kei.ui.page.main.home.HomeHeaderStatusPillState
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import kotlinx.coroutines.flow.onEach

private const val HOME_HEADER_SINK_PER_HIDDEN_CARD_DP = 22
private val HOME_HERO_AVOIDANCE_SCROLL_DISTANCE_DP = 128.dp

internal data class HomePageHeroMotionState(
    val scrollProgress: Float,
    val topBarProgress: Float,
    val bgAlpha: Float,
    val hdrSweepProgress: Float,
    val logoHeightDp: Dp,
    val homeHeaderSinkOffset: Dp,
    val avoidanceProgress: Float,
    val iconProgress: Float,
    val titleProgress: Float,
    val summaryProgress: Float,
    val onHeroHeightPxChanged: (Int) -> Unit,
    val onLogoHeightPxChanged: (Int) -> Unit,
    val onLogoAreaBottomChanged: (Float) -> Unit,
    val onIconBottomChanged: (Float) -> Unit,
    val onTitleBottomChanged: (Float) -> Unit,
    val onSummaryBottomChanged: (Float) -> Unit
)

internal data class HomePageOverviewCardState(
    val homeHeaderStatusPills: List<HomeHeaderStatusPillState>,
    val mcpOverviewStats: List<HomeCardStatItem>,
    val githubOverviewStats: List<HomeCardStatItem>,
    val baOverviewStats: List<HomeCardStatItem>
)

@Composable
internal fun rememberHomePageHeroMotionState(
    lazyListState: LazyListState,
    homeIconHdrEnabled: Boolean,
    runtime: MainPageRuntime,
    hiddenOverviewCardCount: Int
): HomePageHeroMotionState {
    val density = LocalDensity.current
    var logoHeightPx by remember { mutableIntStateOf(0) }
    val scrollProgress by remember {
        derivedStateOf {
            if (logoHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset.toFloat() / logoHeightPx).coerceIn(0f, 1f)
            }
        }
    }
    val topBarProgress by appMotionFloatState(
        targetValue = scrollProgress,
        label = "home_top_bar_progress"
    )
    val bgAlpha by appMotionFloatState(
        targetValue = 1f - scrollProgress,
        label = "home_bg_alpha"
    )
    var logoHeightDp by remember { mutableStateOf(300.dp) }
    var logoAreaY by remember { mutableFloatStateOf(0f) }
    var iconY by remember { mutableFloatStateOf(0f) }
    var titleY by remember { mutableFloatStateOf(0f) }
    var summaryY by remember { mutableFloatStateOf(0f) }
    var initialLogoAreaY by remember { mutableFloatStateOf(0f) }
    val avoidanceScrollDistancePx = remember(density) {
        with(density) { HOME_HERO_AVOIDANCE_SCROLL_DISTANCE_DP.toPx() }
    }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val hdrSweepProgress = if (
        homeIconHdrEnabled &&
        transitionAnimationsEnabled &&
        runtime.isDataActive &&
        !runtime.isPagerScrollInProgress
    ) {
        val hdrSweep = rememberInfiniteTransition(label = "kei_hdr_sweep")
        val animated by hdrSweep.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = resolvedMotionDuration(4600, transitionAnimationsEnabled),
                    easing = LinearEasing
                )
            ),
            label = "kei_hdr_sweep_progress"
        )
        animated
    } else {
        0f
    }
    var iconProgress by remember { mutableFloatStateOf(0f) }
    var titleProgress by remember { mutableFloatStateOf(0f) }
    var summaryProgress by remember { mutableFloatStateOf(0f) }
    var avoidanceProgress by remember { mutableFloatStateOf(0f) }
    val snapshotFlowManager = rememberAppSnapshotFlowManager()

    LaunchedEffect(lazyListState, snapshotFlowManager) {
        snapshotFlowManager.snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }
            .onEach { (index, offset) ->
                if (index > 0) {
                    if (avoidanceProgress != 1f) avoidanceProgress = 1f
                    if (iconProgress != 1f) iconProgress = 1f
                    if (titleProgress != 1f) titleProgress = 1f
                    if (summaryProgress != 1f) summaryProgress = 1f
                    return@onEach
                }
                avoidanceProgress = homeHeroAvoidanceProgress(
                    offsetPx = offset.toFloat(),
                    distancePx = avoidanceScrollDistancePx
                )

                if (initialLogoAreaY == 0f && logoAreaY > 0f) {
                    initialLogoAreaY = logoAreaY
                }
                val refLogoAreaY = if (initialLogoAreaY > 0f) initialLogoAreaY else logoAreaY

                val stage1 = (refLogoAreaY - summaryY).coerceAtLeast(1f)
                val stage2 = (summaryY - titleY).coerceAtLeast(1f)
                val stage3 = (titleY - iconY).coerceAtLeast(1f)

                val summaryDelay = stage1 * 0.5f
                summaryProgress = ((offset.toFloat() - summaryDelay) / (stage1 - summaryDelay).coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
                titleProgress = ((offset.toFloat() - stage1) / stage2)
                    .coerceIn(0f, 1f)
                iconProgress = ((offset.toFloat() - stage1 - stage2) / stage3)
                    .coerceIn(0f, 1f)
            }
            .collect { }
    }

    return remember(
        scrollProgress,
        topBarProgress,
        bgAlpha,
        hdrSweepProgress,
        logoHeightDp,
        hiddenOverviewCardCount,
        avoidanceProgress,
        iconProgress,
        titleProgress,
        summaryProgress
    ) {
        HomePageHeroMotionState(
            scrollProgress = scrollProgress,
            topBarProgress = topBarProgress,
            bgAlpha = bgAlpha,
            hdrSweepProgress = hdrSweepProgress,
            logoHeightDp = logoHeightDp,
            homeHeaderSinkOffset = (hiddenOverviewCardCount * HOME_HEADER_SINK_PER_HIDDEN_CARD_DP).dp,
            avoidanceProgress = avoidanceProgress,
            iconProgress = iconProgress,
            titleProgress = titleProgress,
            summaryProgress = summaryProgress,
            onHeroHeightPxChanged = { heightPx ->
                with(density) { logoHeightDp = heightPx.toDp() }
            },
            onLogoHeightPxChanged = { logoHeightPx = it },
            onLogoAreaBottomChanged = { logoAreaY = it },
            onIconBottomChanged = { bottom -> if (iconY == 0f) iconY = bottom },
            onTitleBottomChanged = { bottom -> if (titleY == 0f) titleY = bottom },
            onSummaryBottomChanged = { bottom -> if (summaryY == 0f) summaryY = bottom }
        )
    }
}

private fun homeHeroAvoidanceProgress(
    offsetPx: Float,
    distancePx: Float
): Float {
    val progress = (offsetPx / distancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

@Composable
internal fun rememberHomePageOverviewCardState(
    homeStatusMcp: String,
    homeStatusGitHub: String,
    homeStatusBa: String,
    homeStatusShizuku: String,
    mcpRunning: Boolean,
    cacheStateColor: Color,
    baLoaded: Boolean,
    baActivated: Boolean,
    shizukuGranted: Boolean,
    runningColor: Color,
    stoppedColor: Color,
    inactiveColor: Color,
    homeStatStatus: String,
    mcpStatusText: String,
    homeStatRuntime: String,
    mcpRuntimeText: String,
    homeStatClients: String,
    mcpConnectedClients: Int,
    homeStatNetwork: String,
    networkModeText: String,
    homeStatPort: String,
    mcpPort: Int,
    homeStatToken: String,
    mcpTokenStatusText: String,
    homeStatService: String,
    mcpServerName: String,
    homeStatPath: String,
    mcpEndpointPath: String,
    homeStatStableUpdates: String,
    githubUpdatableLine: String,
    homeStatPreReleaseUpdates: String,
    githubPreReleaseUpdateLine: String,
    homeStatFailed: String,
    githubFailedLine: String,
    homeStatTracked: String,
    trackedCountLine: String,
    homeStatCached: String,
    cacheHitCountLine: String,
    homeStatShare: String,
    githubShareLine: String,
    homeStatStrategy: String,
    githubStrategyText: String,
    homeStatApi: String,
    githubApiText: String,
    homeStatLastUpdate: String,
    githubLastUpdateLine: String,
    baActivationLine: String,
    homeStatAp: String,
    baApLine: String,
    homeStatCafeAp: String,
    baCafeApLine: String,
    homeStatApRemaining: String,
    baApRemainingLine: String,
    homeStatBaServer: String,
    baServerLine: String,
    homeStatBaNotify: String,
    baNotifyLine: String
): HomePageOverviewCardState {
    val homeHeaderStatusPills = remember(
        homeStatusMcp,
        homeStatusGitHub,
        homeStatusBa,
        homeStatusShizuku,
        mcpRunning,
        cacheStateColor,
        baLoaded,
        baActivated,
        shizukuGranted,
        runningColor,
        stoppedColor,
        inactiveColor
    ) {
        listOf(
            HomeHeaderStatusPillState(
                label = homeStatusMcp,
                color = if (mcpRunning) runningColor else stoppedColor,
                minWidth = 62.dp
            ),
            HomeHeaderStatusPillState(
                label = homeStatusGitHub,
                color = cacheStateColor,
                minWidth = 72.dp
            ),
            HomeHeaderStatusPillState(
                label = homeStatusBa,
                color = when {
                    !baLoaded -> inactiveColor
                    baActivated -> runningColor
                    else -> stoppedColor
                },
                minWidth = 62.dp
            ),
            HomeHeaderStatusPillState(
                label = homeStatusShizuku,
                color = if (shizukuGranted) runningColor else stoppedColor,
                minWidth = 70.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp,
                    vertical = 5.dp
                )
            )
        )
    }
    val mcpOverviewStats = remember(
        homeStatStatus,
        mcpStatusText,
        homeStatRuntime,
        mcpRuntimeText,
        homeStatClients,
        mcpConnectedClients,
        homeStatNetwork,
        networkModeText,
        homeStatPort,
        mcpPort,
        homeStatToken,
        mcpTokenStatusText,
        homeStatService,
        mcpServerName,
        homeStatPath,
        mcpEndpointPath
    ) {
        listOf(
            HomeCardStatItem(label = homeStatStatus, value = mcpStatusText, emphasize = true),
            HomeCardStatItem(label = homeStatRuntime, value = mcpRuntimeText, emphasize = true),
            HomeCardStatItem(label = homeStatClients, value = mcpConnectedClients.toString()),
            HomeCardStatItem(label = homeStatNetwork, value = networkModeText),
            HomeCardStatItem(label = homeStatPort, value = mcpPort.toString()),
            HomeCardStatItem(label = homeStatToken, value = mcpTokenStatusText),
            HomeCardStatItem(label = homeStatService, value = mcpServerName),
            HomeCardStatItem(label = homeStatPath, value = mcpEndpointPath)
        )
    }
    val githubOverviewStats = remember(
        homeStatStableUpdates,
        githubUpdatableLine,
        homeStatPreReleaseUpdates,
        githubPreReleaseUpdateLine,
        homeStatFailed,
        githubFailedLine,
        homeStatTracked,
        trackedCountLine,
        homeStatCached,
        cacheHitCountLine,
        homeStatShare,
        githubShareLine,
        homeStatStrategy,
        githubStrategyText,
        homeStatApi,
        githubApiText,
        homeStatLastUpdate,
        githubLastUpdateLine
    ) {
        listOf(
            HomeCardStatItem(label = homeStatStableUpdates, value = githubUpdatableLine, emphasize = true),
            HomeCardStatItem(
                label = homeStatPreReleaseUpdates,
                value = githubPreReleaseUpdateLine,
                emphasize = true
            ),
            HomeCardStatItem(label = homeStatFailed, value = githubFailedLine),
            HomeCardStatItem(label = homeStatTracked, value = trackedCountLine),
            HomeCardStatItem(label = homeStatCached, value = cacheHitCountLine),
            HomeCardStatItem(label = homeStatShare, value = githubShareLine),
            HomeCardStatItem(label = homeStatStrategy, value = githubStrategyText),
            HomeCardStatItem(label = homeStatApi, value = githubApiText),
            HomeCardStatItem(label = homeStatLastUpdate, value = githubLastUpdateLine)
        )
    }
    val baOverviewStats = remember(
        homeStatStatus,
        baActivationLine,
        homeStatAp,
        baApLine,
        homeStatCafeAp,
        baCafeApLine,
        homeStatApRemaining,
        baApRemainingLine,
        homeStatBaServer,
        baServerLine,
        homeStatBaNotify,
        baNotifyLine
    ) {
        listOf(
            HomeCardStatItem(label = homeStatStatus, value = baActivationLine, emphasize = true),
            HomeCardStatItem(label = homeStatAp, value = baApLine, emphasize = true),
            HomeCardStatItem(label = homeStatApRemaining, value = baApRemainingLine),
            HomeCardStatItem(label = homeStatCafeAp, value = baCafeApLine),
            HomeCardStatItem(label = homeStatBaServer, value = baServerLine),
            HomeCardStatItem(label = homeStatBaNotify, value = baNotifyLine)
        )
    }

    return remember(
        homeHeaderStatusPills,
        mcpOverviewStats,
        githubOverviewStats,
        baOverviewStats
    ) {
        HomePageOverviewCardState(
            homeHeaderStatusPills = homeHeaderStatusPills,
            mcpOverviewStats = mcpOverviewStats,
            githubOverviewStats = githubOverviewStats,
            baOverviewStats = baOverviewStats
        )
    }
}
