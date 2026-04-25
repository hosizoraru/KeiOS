package os.kei.ui.page.main.host.pager

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.widget.chrome.ScrollChromeVisibilityController
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class MainPagerTabJumpControllerState(
    val pagerScrollEnabled: Boolean,
    val showBottomBar: Boolean,
    val nestedScrollConnection: NestedScrollConnection,
    val farJumpAlpha: Float,
    val onActionBarInteractingChanged: (Boolean) -> Unit,
    val onPageSelected: (Int) -> Unit
)

@Composable
internal fun rememberMainPagerTabJumpController(
    tabs: List<BottomPage>,
    pagerState: PagerState,
    pagerRuntime: MainPagerRuntimeSnapshot,
    transitionAnimationsEnabled: Boolean,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
): MainPagerTabJumpControllerState {
    val coroutineScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }
    var pagerScrollEnabled by remember { mutableStateOf(true) }
    var showBottomBar by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    val density = LocalDensity.current
    val bottomBarVisibilityThresholdPx = remember(density) { with(density) { 22.dp.toPx() } }
    val bottomBarVisibilityController = remember(bottomBarVisibilityThresholdPx) {
        ScrollChromeVisibilityController(bottomBarVisibilityThresholdPx)
    }

    val farJumpBefore: suspend () -> Unit = {
        if (!transitionAnimationsEnabled) {
            farJumpAlpha.snapTo(1f)
        } else {
            farJumpAlpha.snapTo(1f)
            farJumpAlpha.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(
                        AppMotionTokens.farJumpDimMs,
                        transitionAnimationsEnabled
                    )
                )
            )
        }
    }
    val farJumpAfter: suspend () -> Unit = {
        if (!transitionAnimationsEnabled) {
            farJumpAlpha.snapTo(1f)
        } else {
            farJumpAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(
                        AppMotionTokens.farJumpRestoreMs,
                        transitionAnimationsEnabled
                    )
                )
            )
        }
    }
    val nestedScrollConnection = remember(pagerRuntime.homePageBottomBarPinned, bottomBarVisibilityController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (pagerRuntime.homePageBottomBarPinned) {
                    bottomBarVisibilityController.showNow(showBottomBar) { showBottomBar = it }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!pagerRuntime.homePageBottomBarPinned) {
                    bottomBarVisibilityController.update(consumed.y, showBottomBar) { showBottomBar = it }
                }
                return Offset.Zero
            }
        }
    }
    val onActionBarInteractingChanged: (Boolean) -> Unit = { interacting ->
        pagerScrollEnabled = !interacting
    }
    val onPageSelected: (Int) -> Unit = { index ->
        if (index in tabs.indices) {
            showBottomBar = true
            val stablePageIndex = if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.settledPage
            }.coerceIn(0, tabs.lastIndex)
            if (index != stablePageIndex) {
                tabJumpJob?.cancel()
                tabJumpJob = coroutineScope.launch {
                    pagerState.animateTabSwitch(
                        fromIndex = stablePageIndex,
                        targetIndex = index,
                        animationsEnabled = transitionAnimationsEnabled,
                        onFarJumpBefore = farJumpBefore,
                        onFarJumpAfter = farJumpAfter
                    )
                }
            }
        }
    }

    LaunchedEffect(pagerRuntime.homePageBottomBarPinned) {
        if (pagerRuntime.homePageBottomBarPinned && !showBottomBar) {
            bottomBarVisibilityController.reset()
            showBottomBar = true
        }
    }
    LaunchedEffect(requestedBottomPageToken, requestedBottomPage, tabs) {
        val target = requestedBottomPage ?: return@LaunchedEffect
        val index = tabs.indexOfFirst { it.name == target }
        val stablePageIndex = if (pagerState.isScrollInProgress) {
            pagerState.targetPage
        } else {
            pagerState.settledPage
        }.coerceIn(0, tabs.lastIndex)
        if (index >= 0 && index != stablePageIndex) {
            tabJumpJob?.cancel()
            tabJumpJob = coroutineScope.launch {
                pagerState.animateTabSwitch(
                    fromIndex = stablePageIndex,
                    targetIndex = index,
                    animationsEnabled = transitionAnimationsEnabled,
                    onFarJumpBefore = farJumpBefore,
                    onFarJumpAfter = farJumpAfter
                )
            }
            showBottomBar = true
        }
        onRequestedBottomPageConsumed()
    }

    return remember(
        pagerScrollEnabled,
        showBottomBar,
        nestedScrollConnection,
        farJumpAlpha.value,
        onActionBarInteractingChanged,
        onPageSelected
    ) {
        MainPagerTabJumpControllerState(
            pagerScrollEnabled = pagerScrollEnabled,
            showBottomBar = showBottomBar,
            nestedScrollConnection = nestedScrollConnection,
            farJumpAlpha = farJumpAlpha.value,
            onActionBarInteractingChanged = onActionBarInteractingChanged,
            onPageSelected = onPageSelected
        )
    }
}
