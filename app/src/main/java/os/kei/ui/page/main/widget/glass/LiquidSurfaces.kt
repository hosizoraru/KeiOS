package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.animation.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousCapsule,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = true,
    shadow: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = if (isInteractive) null else LocalIndication.current,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(
                        lensRadius.toPx(),
                        lensRadius.toPx(),
                        chromaticAberration = chromaticAberration,
                        depthEffect = depthEffect
                    )
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isInteractive && enabled) 1f else 0.82f)
                },
                shadow = {
                    if (shadow) {
                        Shadow.Default.copy(color = Color.Black.copy(alpha = 0.10f))
                    } else {
                        Shadow(alpha = 0f)
                    }
                },
                innerShadow = {
                    val progress = if (isInteractive && enabled) interactiveHighlight.pressProgress else 0f
                    InnerShadow(radius = 6.dp * progress, alpha = progress)
                },
                layerBlock = if (isInteractive && enabled) {
                    {
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                        val maxOffset = size.minDimension
                        val offset = interactiveHighlight.offset
                        val initialDerivative = 0.05f
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (size.width / size.height).fastCoerceAtMost(1f)
                        scaleY = scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (size.height / size.width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = tint.alpha * 0.70f))
                    }
                    if (surfaceColor.isSpecified && surfaceColor.alpha > 0f) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .then(clickableModifier)
            .then(
                if (isInteractive && enabled) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
            },
        content = content
    )
}

@Composable
fun LiquidRoundedCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = true,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidSurface(
        backdrop = backdrop,
        modifier = modifier,
        shape = RoundedRectangle(cornerRadius),
        tint = tint,
        surfaceColor = surfaceColor,
        blurRadius = blurRadius,
        lensRadius = lensRadius,
        chromaticAberration = chromaticAberration,
        depthEffect = depthEffect,
        shadow = shadow
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
