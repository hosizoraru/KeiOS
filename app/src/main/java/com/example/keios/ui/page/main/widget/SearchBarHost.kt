package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SearchBarHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animationLabelPrefix: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val searchBarHeight by appMotionDpState(
        targetValue = if (visible) AppChromeTokens.searchBarHostHeight else 0.dp,
        durationMillis = AppMotionTokens.searchBarSlideMs,
        label = "${animationLabelPrefix}Height"
    )
    val searchBarAlpha by appMotionFloatState(
        targetValue = if (visible) 1f else 0f,
        durationMillis = AppMotionTokens.searchBarFadeMs,
        label = "${animationLabelPrefix}Alpha"
    )
    val searchBarOffsetY by appMotionDpState(
        targetValue = if (visible) 0.dp else (-12).dp,
        durationMillis = AppMotionTokens.searchBarSlideMs,
        label = "${animationLabelPrefix}Offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(searchBarHeight)
            .clipToBounds()
            .graphicsLayer {
                alpha = searchBarAlpha
                translationY = with(density) { searchBarOffsetY.toPx() }
            },
        content = content
    )
}
