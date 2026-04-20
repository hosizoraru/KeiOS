package com.example.keios.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.example.keios.ui.animation.DampedDragAnimation
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

internal fun Modifier.liquidActionBarSelectionAura(
    enabled: Boolean,
    animation: DampedDragAnimation,
    tabWidthPx: Float,
    panelOffsetPx: Float,
    isLtr: Boolean,
    glowColor: Color,
    coreColor: Color,
    interactionProgress: Float
): Modifier {
    if (!enabled || tabWidthPx <= 0f) return this
    return drawWithContent {
        val activeProgress = interactionProgress.fastCoerceIn(0f, 1f)
        if (activeProgress <= 0.001f) {
            drawContent()
            return@drawWithContent
        }
        val centerX = if (isLtr) {
            (animation.value + 0.5f) * tabWidthPx + panelOffsetPx
        } else {
            size.width - (animation.value + 0.5f) * tabWidthPx + panelOffsetPx
        }.fastCoerceIn(0f, size.width)
        val center = Offset(centerX, size.height / 2f)
        val pressProgress = animation.pressProgress.fastCoerceIn(0f, 1f)
        val glowAlpha = (0.04f + pressProgress * 0.16f) * activeProgress
        val coreAlpha = (0.03f + pressProgress * 0.18f) * activeProgress
        drawCircle(
            color = glowColor.copy(alpha = glowAlpha.fastCoerceIn(0f, 0.24f)),
            radius = size.height * (0.82f + pressProgress * 0.14f),
            center = center
        )
        drawCircle(
            color = coreColor.copy(alpha = coreAlpha.fastCoerceIn(0f, 0.22f)),
            radius = size.height * (0.38f + pressProgress * 0.06f),
            center = center
        )
        drawContent()
    }
}

@Composable
internal fun rememberLiquidActionBarPalette(
    layeredStyleEnabled: Boolean,
    isBlurEnabled: Boolean,
    isInLightTheme: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceContainer: Color
): LiquidActionBarPalette = remember(
    layeredStyleEnabled,
    isBlurEnabled,
    isInLightTheme,
    primary,
    onSurface,
    surfaceContainer
) {
    if (layeredStyleEnabled) {
        if (isInLightTheme) {
            return@remember LiquidActionBarPalette(
                baseFillColor = if (isBlurEnabled) surfaceContainer.copy(alpha = 0.40f) else surfaceContainer,
                inactiveContentColor = onSurface.copy(alpha = if (isBlurEnabled) 0.92f else 0.90f),
                activeContentColor = primary,
                selectionGlowColor = Color.White,
                selectionCoreColor = Color.White,
                outlineColor = Color.White.copy(alpha = if (isBlurEnabled) 0.24f else 0.18f)
            )
        }

        return@remember LiquidActionBarPalette(
            baseFillColor = if (isBlurEnabled) {
                lerp(surfaceContainer, onSurface, 0.18f).copy(alpha = 0.58f)
            } else {
                lerp(surfaceContainer, onSurface, 0.14f)
            },
            inactiveContentColor = onSurface.copy(alpha = 0.94f),
            activeContentColor = primary.copy(alpha = 0.98f),
            selectionGlowColor = Color.White.copy(alpha = 0.26f),
            selectionCoreColor = Color.White.copy(alpha = 0.18f),
            outlineColor = Color.White.copy(alpha = if (isBlurEnabled) 0.16f else 0.12f)
        )
    }

    if (!isBlurEnabled) {
        return@remember LiquidActionBarPalette(
            baseFillColor = surfaceContainer,
            inactiveContentColor = onSurface.copy(alpha = 0.68f),
            activeContentColor = onSurface.copy(alpha = 0.94f),
            selectionGlowColor = Color.White.copy(alpha = 0.10f),
            selectionCoreColor = Color.White.copy(alpha = 0.08f),
            outlineColor = if (isInLightTheme) {
                Color.White.copy(alpha = 0.12f)
            } else {
                Color.White.copy(alpha = 0.10f)
            }
        )
    }

    if (isInLightTheme) {
        return@remember LiquidActionBarPalette(
            baseFillColor = surfaceContainer.copy(alpha = 0.20f),
            inactiveContentColor = onSurface.copy(alpha = 0.60f),
            activeContentColor = onSurface.copy(alpha = 0.92f),
            selectionGlowColor = Color.White.copy(alpha = 0.14f),
            selectionCoreColor = Color.White.copy(alpha = 0.10f),
            outlineColor = Color.White.copy(alpha = 0.14f)
        )
    }

    return@remember LiquidActionBarPalette(
        baseFillColor = surfaceContainer.copy(alpha = 0.22f),
        inactiveContentColor = onSurface.copy(alpha = 0.82f),
        activeContentColor = onSurface.copy(alpha = 0.97f),
        selectionGlowColor = onSurface.copy(alpha = 0.13f),
        selectionCoreColor = surfaceContainer.copy(alpha = 0.20f),
        outlineColor = Color.White.copy(alpha = 0.10f)
    )
}

internal fun liquidActionBarBaseHighlight(
    layeredStyleEnabled: Boolean,
    isBlurEnabled: Boolean,
    isInLightTheme: Boolean
): Highlight {
    if (layeredStyleEnabled) {
        return Highlight.Default.copy(alpha = if (isBlurEnabled) 1f else 0f)
    }

    val highlightColor = if (isInLightTheme) {
        Color.White.copy(alpha = if (isBlurEnabled) 0.26f else 0.18f)
    } else {
        Color.White.copy(alpha = if (isBlurEnabled) 0.14f else 0.10f)
    }
    return Highlight(
        width = if (isInLightTheme) 0.50.dp else 0.42.dp,
        blurRadius = if (isInLightTheme) 1.30.dp else 1.05.dp,
        alpha = if (isBlurEnabled) {
            if (isInLightTheme) 0.30f else 0.26f
        } else {
            if (isInLightTheme) 0.18f else 0.14f
        },
        style = HighlightStyle.Default(
            color = highlightColor,
            angle = 84f,
            falloff = 1.45f
        )
    )
}

internal fun liquidActionBarBaseShadow(
    layeredStyleEnabled: Boolean,
    isInLightTheme: Boolean
): Shadow {
    if (layeredStyleEnabled) {
        return Shadow.Default.copy(
            color = Color.Black.copy(alpha = if (isInLightTheme) 0.10f else 0.20f)
        )
    }

    return if (isInLightTheme) {
        Shadow(
            radius = 12.dp,
            offset = DpOffset(0.dp, 1.dp),
            color = Color.Black.copy(alpha = 0.032f)
        )
    } else {
        Shadow(
            radius = 16.dp,
            offset = DpOffset(0.dp, 1.5.dp),
            color = Color.Black.copy(alpha = 0.09f)
        )
    }
}

internal fun liquidActionBarInteractionHighlightStrength(
    layeredStyleEnabled: Boolean,
    isInLightTheme: Boolean
): Float = when {
    layeredStyleEnabled -> 1f
    isInLightTheme -> 0.48f
    else -> 0.62f
}

internal fun liquidActionBarInteractionHighlightRadiusScale(
    layeredStyleEnabled: Boolean,
    isInLightTheme: Boolean
): Float = when {
    layeredStyleEnabled -> 1.2f
    isInLightTheme -> 0.88f
    else -> 0.86f
}

@Stable
internal class LiquidActionBarPalette(
    val baseFillColor: Color,
    val inactiveContentColor: Color,
    val activeContentColor: Color,
    val selectionGlowColor: Color,
    val selectionCoreColor: Color,
    val outlineColor: Color
)
