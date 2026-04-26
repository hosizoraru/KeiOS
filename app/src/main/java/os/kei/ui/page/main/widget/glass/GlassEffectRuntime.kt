package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import kotlin.math.max

@Immutable
data class GlassEffectRuntime(
    val reducedProgress: Float = 0f
) {
    fun blurScaleFor(variant: GlassVariant): Float = lerp(
        start = 1f,
        stop = when (variant) {
            GlassVariant.Content -> 0.62f
            GlassVariant.Floating -> 0.66f
            else -> 0.70f
        },
        fraction = reducedProgress
    )

    fun lensScaleFor(variant: GlassVariant): Float = lerp(
        start = 1f,
        stop = when (variant) {
            GlassVariant.Content -> 0.58f
            GlassVariant.Floating -> 0.62f
            else -> 0.66f
        },
        fraction = reducedProgress
    )

    val interactionLensScale: Float
        get() = lerp(
            start = 1f,
            stop = 0.84f,
            fraction = reducedProgress
        )
}

val LocalGlassEffectRuntime = compositionLocalOf { GlassEffectRuntime() }

@Composable
@ReadOnlyComposable
internal fun glassEffectRuntime(): GlassEffectRuntime = LocalGlassEffectRuntime.current

@Composable
@ReadOnlyComposable
internal fun resolvedGlassBlurDp(
    base: Dp,
    variant: GlassVariant
): Dp = (base * glassEffectRuntime().blurScaleFor(variant)).clampGlassBlur()

@Composable
@ReadOnlyComposable
internal fun resolvedGlassLensDp(
    base: Dp,
    variant: GlassVariant
): Dp = base * glassEffectRuntime().lensScaleFor(variant)

@Composable
internal fun rememberGlassReductionProgress(
    reduceEffectsDuringMotion: Boolean,
    label: String
): Float {
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val progress = remember(label) {
        Animatable(if (reduceEffectsDuringMotion) 1f else 0f)
    }
    LaunchedEffect(progress, reduceEffectsDuringMotion, animationsEnabled) {
        if (reduceEffectsDuringMotion || !animationsEnabled) {
            progress.snapTo(if (reduceEffectsDuringMotion) 1f else 0f)
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(
                        AppMotionTokens.glassEffectRelaxMs,
                        animationsEnabled
                    )
                )
            )
        }
    }
    return progress.value
}

@Composable
internal fun rememberListScrollGlassRuntime(
    isListScrolling: Boolean,
    label: String,
    reductionScale: Float = UiPerformanceBudget.listScrollGlassReductionScale
): GlassEffectRuntime {
    val upstreamRuntime = glassEffectRuntime()
    val listScrollProgress = rememberGlassReductionProgress(
        reduceEffectsDuringMotion = isListScrolling,
        label = label
    )
    return remember(upstreamRuntime, listScrollProgress, reductionScale) {
        upstreamRuntime.copy(
            reducedProgress = max(
                upstreamRuntime.reducedProgress,
                listScrollProgress * reductionScale
            )
        )
    }
}
