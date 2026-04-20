package com.example.keios.ui.page.main.host.pager

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import kotlinx.coroutines.delay

@Composable
internal fun BindMainPagerCoordinatorEffects(
    settingsReturnToken: Int,
    transitionAnimationsEnabled: Boolean,
    tabsSize: Int,
    pagerState: PagerState,
    onTemporaryBeyondViewportCountChange: (Int?) -> Unit
) {
    LaunchedEffect(settingsReturnToken, transitionAnimationsEnabled) {
        if (settingsReturnToken <= 0) return@LaunchedEffect
        onTemporaryBeyondViewportCountChange(0)
        withFrameNanos { }
        delay(resolvedMotionDuration(220, transitionAnimationsEnabled).toLong())
        onTemporaryBeyondViewportCountChange(null)
    }

    LaunchedEffect(tabsSize) {
        val lastIndex = tabsSize - 1
        if (lastIndex >= 0 && pagerState.currentPage > lastIndex) {
            pagerState.scrollToPage(lastIndex)
        }
    }
}
