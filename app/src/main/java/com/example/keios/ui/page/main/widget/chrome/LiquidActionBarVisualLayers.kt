package com.example.keios.ui.page.main.widget.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.example.keios.ui.animation.DampedDragAnimation
import com.example.keios.ui.animation.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule

@Composable
internal fun LiquidActionBarLayeredVisualOverlay(
    layeredStyleEnabled: Boolean,
    isBlurEnabled: Boolean,
    items: List<LiquidActionItem>,
    backdrop: Backdrop,
    tabsBackdrop: LayerBackdrop,
    combinedBackdrop: Backdrop,
    palette: LiquidActionBarPalette,
    accentColor: Color,
    dampedDragAnimation: DampedDragAnimation,
    effectBlurDp: Dp,
    effectLensDp: Dp,
    tabWidthPx: Float,
    totalWidthPx: Float,
    isInLightTheme: Boolean,
    isLtr: Boolean,
    effectivePanelOffset: Float,
    interactionLensScale: Float,
    interactiveHighlight: InteractiveHighlight?
) {
    if (!layeredStyleEnabled) return
    val density = LocalDensity.current
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .alpha(0f)
            .layerBackdrop(tabsBackdrop)
            .graphicsLayer {
                translationX = effectivePanelOffset
                clip = false
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    if (isBlurEnabled) {
                        val progress = dampedDragAnimation.pressProgress
                        vibrancy()
                        blur(effectBlurDp.toPx())
                        lens(
                            effectLensDp.toPx() * progress,
                            effectLensDp.toPx() * progress
                        )
                    }
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                },
                onDrawSurface = { drawRect(palette.baseFillColor) }
            )
            .height(AppChromeTokens.liquidActionBarInnerHeight)
            .padding(horizontal = AppChromeTokens.liquidActionBarHorizontalPadding)
            .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        items.forEach { item ->
            LiquidActionItemSlot(item = item, tint = accentColor)
        }
    }

    if (tabWidthPx <= 0f) return

    Box(
        Modifier
            .padding(horizontal = AppChromeTokens.liquidActionBarHorizontalPadding)
            .graphicsLayer {
                val contentWidth = totalWidthPx - with(density) { 8.dp.toPx() }
                val singleTabWidth = contentWidth / items.size
                val progressOffset = dampedDragAnimation.value * singleTabWidth
                translationX = if (isLtr) {
                    progressOffset + effectivePanelOffset
                } else {
                    -progressOffset + effectivePanelOffset
                }
                clip = false
            }
            .then(if (isBlurEnabled && interactiveHighlight != null) interactiveHighlight.gestureModifier else Modifier)
            .then(dampedDragAnimation.modifier)
            .drawBackdrop(
                backdrop = combinedBackdrop,
                shape = { ContinuousCapsule },
                effects = {
                    if (isBlurEnabled && dampedDragAnimation.pressProgress > 0f) {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            9f.dp.toPx() * progress * interactionLensScale,
                            12f.dp.toPx() * progress * interactionLensScale,
                            true
                        )
                    }
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                },
                shadow = { Shadow(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f) },
                innerShadow = {
                    InnerShadow(
                        radius = 7f.dp * dampedDragAnimation.pressProgress,
                        alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
                    )
                },
                layerBlock = {
                    if (isBlurEnabled) {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    }
                },
                onDrawSurface = {
                    val progress = dampedDragAnimation.pressProgress
                    drawRect(
                        color = if (isInLightTheme) {
                            Color.Black.copy(0.1f)
                        } else {
                            Color.White.copy(0.1f)
                        },
                        alpha = progress * (1f - progress)
                    )
                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                }
            )
            .height(AppChromeTokens.liquidActionBarInnerHeight)
            .width(
                with(density) {
                    ((totalWidthPx - (AppChromeTokens.liquidActionBarHorizontalPadding * 2).toPx()) / items.size).toDp()
                }
            )
    )
}
