package com.example.keios.ui.page.main.student.page.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.keios.ui.page.main.host.pager.animateTabSwitch
import com.example.keios.ui.page.main.student.GuideBottomTab
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.example.keios.ui.page.main.widget.motion.AppMotionTokens
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun rememberBaStudentGuideTopBarActionItems(
    shareIcon: androidx.compose.ui.graphics.vector.ImageVector,
    refreshIcon: androidx.compose.ui.graphics.vector.ImageVector,
    shareSourceContentDescription: String,
    refreshContentDescription: String,
    onShareSource: () -> Unit,
    onRefresh: () -> Unit
): List<LiquidActionItem> {
    return remember(
        shareIcon,
        refreshIcon,
        shareSourceContentDescription,
        refreshContentDescription,
        onShareSource,
        onRefresh
    ) {
        listOf(
            LiquidActionItem(
                icon = shareIcon,
                contentDescription = shareSourceContentDescription,
                onClick = onShareSource
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshContentDescription,
                onClick = onRefresh
            )
        )
    }
}

@Composable
internal fun rememberBaStudentGuideTabSelectCoordinator(
    bottomTabs: List<GuideBottomTab>,
    pagerState: PagerState,
    transitionAnimationsEnabled: Boolean,
    farJumpAlpha: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    onShowBottomBarChange: (Boolean) -> Unit,
    onSelectedBottomTabIndexChange: (Int) -> Unit
): (Int) -> Unit {
    val pageScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }

    return remember(
        bottomTabs,
        pagerState,
        transitionAnimationsEnabled,
        farJumpAlpha,
        onShowBottomBarChange,
        onSelectedBottomTabIndexChange,
        pageScope
    ) {
        { index: Int ->
            if (bottomTabs.isEmpty()) return@remember
            val safeIndex = index.coerceIn(0, bottomTabs.lastIndex)
            val fromIndex = if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.settledPage
            }
            if (safeIndex == fromIndex && !pagerState.isScrollInProgress) return@remember
            onShowBottomBarChange(true)
            onSelectedBottomTabIndexChange(safeIndex)
            tabJumpJob?.cancel()
            tabJumpJob = pageScope.launch {
                pagerState.animateTabSwitch(
                    fromIndex = fromIndex,
                    targetIndex = safeIndex,
                    animationsEnabled = transitionAnimationsEnabled,
                    onFarJumpBefore = {
                        farJumpAlpha.snapTo(0.94f)
                    },
                    onFarJumpAfter = {
                        farJumpAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = resolvedMotionDuration(
                                    AppMotionTokens.farJumpRestoreEmphasisMs,
                                    transitionAnimationsEnabled
                                ),
                                easing = if (transitionAnimationsEnabled) {
                                    FastOutSlowInEasing
                                } else {
                                    LinearEasing
                                }
                            )
                        )
                    }
                )
            }
        }
    }
}
