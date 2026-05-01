package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule

@Composable
fun LiquidLinearProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = liquidProgressDefaultActiveColor(),
    inactiveColor: Color = liquidProgressDefaultInactiveColor(),
    height: Dp = 4.dp,
    contentDescription: String? = null
) {
    val progressBackdrop = rememberLayerBackdrop()
    val contentDescriptionState = remember(contentDescription) { contentDescription }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescriptionState?.let { this.contentDescription = it }
                progressBarRangeInfo = ProgressBarRangeInfo(
                    progress().coerceIn(valueRange),
                    valueRange,
                    steps = 0
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val safeFraction = liquidProgressFraction(
            value = progress(),
            valueRange = valueRange
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(progressBackdrop)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ContinuousCapsule)
                .background(inactiveColor)
        )
        Box(
            modifier = Modifier
                .height(height)
                .layout { measurable, constraints ->
                    val width = (constraints.maxWidth * safeFraction)
                        .fastRoundToInt()
                        .coerceIn(0, constraints.maxWidth)
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = width,
                            maxWidth = width
                        )
                    )
                    layout(width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .clip(ContinuousCapsule)
                .drawBackdrop(
                    backdrop = progressBackdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        blur(3.dp.toPx())
                        lens(
                            5.dp.toPx(),
                            8.dp.toPx(),
                            depthEffect = true
                        )
                    },
                    highlight = {
                        Highlight.Ambient.copy(alpha = 0.42f)
                    },
                    shadow = {
                        Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.04f))
                    },
                    innerShadow = {
                        InnerShadow(radius = 2.dp, alpha = 0.14f)
                    },
                    onDrawSurface = {
                        drawRect(activeColor)
                        drawRect(Color.White.copy(alpha = 0.10f))
                    }
                )
        )
    }
}

@Composable
fun LiquidMusicProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = liquidProgressDefaultActiveColor(),
    inactiveColor: Color = liquidProgressDefaultInactiveColor(),
    contentDescription: String? = null
) {
    LiquidLinearProgressBar(
        progress = progress,
        modifier = modifier,
        valueRange = valueRange,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        height = 3.dp,
        contentDescription = contentDescription
    )
}

@Composable
fun LiquidCircularProgressBar(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = liquidProgressDefaultActiveColor(),
    inactiveColor: Color = liquidProgressDefaultInactiveColor(),
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
    contentDescription: String? = null
) {
    val contentDescriptionState = remember(contentDescription) { contentDescription }
    val infiniteTransition = rememberInfiniteTransition(label = "liquid-circular-progress")
    val indeterminateRotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid-circular-progress-rotation"
    )
    val indeterminatePulse = infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.66f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid-circular-progress-pulse"
    )
    val progressProvider = progress
    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescriptionState?.let { this.contentDescription = it }
                progressProvider?.let { provider ->
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        provider().coerceIn(valueRange),
                        valueRange,
                        steps = 0
                    )
                }
            }
    ) {
        val strokePx = strokeWidth.toPx()
        val arcInset = strokePx / 2f
        val arcSize = androidx.compose.ui.geometry.Size(
            width = this.size.width - strokePx,
            height = this.size.height - strokePx
        )
        drawArc(
            color = inactiveColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcInset, arcInset),
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
        val fraction = progressProvider?.let { provider ->
            liquidProgressFraction(provider(), valueRange)
        }
        val startAngle = if (fraction == null) indeterminateRotation.value - 90f else -90f
        val sweepAngle = if (fraction == null) {
            72f + 148f * indeterminatePulse.value
        } else {
            (fraction * 360f).coerceIn(0f, 360f)
        }
        drawArc(
            color = activeColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcInset, arcInset),
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = startAngle,
            sweepAngle = sweepAngle.coerceAtMost(120f),
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(arcInset, arcInset),
            size = arcSize,
            style = Stroke(width = (strokePx * 0.46f).coerceAtLeast(1f), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun liquidProgressDefaultActiveColor(): Color {
    return if (isSystemInDarkTheme()) Color(0xFF5DAEFF) else Color(0xFF0088FF)
}

@Composable
private fun liquidProgressDefaultInactiveColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color(0xFF1D1D1F).copy(alpha = 0.15f)
    }
}

private fun liquidProgressFraction(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>
): Float {
    val span = valueRange.endInclusive - valueRange.start
    if (span <= 0f) return 0f
    return ((value.coerceIn(valueRange) - valueRange.start) / span).fastCoerceIn(0f, 1f)
}
