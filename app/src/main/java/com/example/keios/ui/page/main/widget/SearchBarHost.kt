package com.example.keios.ui.page.main.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

@Composable
fun SearchBarHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animationLabelPrefix: String,
    backdrop: LayerBackdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val searchBarHeight by animateDpAsState(
        targetValue = if (visible) 48.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "${animationLabelPrefix}Height"
    )
    val searchBarAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "${animationLabelPrefix}Alpha"
    )
    val searchBarOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else (-12).dp,
        animationSpec = tween(durationMillis = 220),
        label = "${animationLabelPrefix}Offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(searchBarHeight)
            .clipToBounds()
            .then(
                if (backdrop != null) {
                    Modifier.layerBackdrop(backdrop)
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                alpha = searchBarAlpha
                translationY = with(density) { searchBarOffsetY.toPx() }
            },
        content = content
    )
}
