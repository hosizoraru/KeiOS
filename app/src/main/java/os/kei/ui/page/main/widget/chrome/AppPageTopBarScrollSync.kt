package os.kei.ui.page.main.widget.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import top.yukonga.miuix.kmp.basic.ScrollBehavior

internal fun isPageSettledAtTop(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    listScrollInProgress: Boolean
): Boolean {
    return firstVisibleItemIndex == 0 &&
        firstVisibleItemScrollOffset == 0 &&
        !listScrollInProgress
}

internal fun snapTopAppBarToExpanded(scrollBehavior: ScrollBehavior?) {
    scrollBehavior?.state?.heightOffset = 0f
    scrollBehavior?.state?.contentOffset = 0f
}

internal suspend fun expandTopAppBarToPageTop(
    scrollBehavior: ScrollBehavior?,
    animationsEnabled: Boolean
) {
    val state = scrollBehavior?.state ?: return
    if (!animationsEnabled) {
        snapTopAppBarToExpanded(scrollBehavior)
        return
    }
    val startOffset = state.heightOffset
    if (startOffset >= -0.5f) {
        snapTopAppBarToExpanded(scrollBehavior)
        return
    }
    Animatable(startOffset).animateTo(
        targetValue = 0f,
        animationSpec = tween(
            durationMillis = AppMotionTokens.expandSizeOutMs,
            easing = FastOutSlowInEasing
        )
    ) {
        state.heightOffset = value
    }
    state.contentOffset = 0f
}
