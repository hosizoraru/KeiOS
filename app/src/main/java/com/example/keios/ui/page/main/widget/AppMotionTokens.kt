package com.example.keios.ui.page.main.widget

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

val LocalTransitionAnimationsEnabled = staticCompositionLocalOf { true }

object AppMotionTokens {
    const val disabledDurationMs = 0
    const val searchBarFadeMs = 160
    const val searchBarSlideMs = 220
    const val expandFadeInMs = 160
    const val expandSizeInMs = 220
    const val expandFadeOutMs = 120
    const val expandSizeOutMs = 180
    const val floatingFadeInMs = 180
    const val floatingSlideInMs = 220
    const val floatingFadeOutMs = 120
    const val floatingSlideOutMs = 180
    const val farJumpDimMs = 70
    const val farJumpRestoreMs = 120
    const val farJumpRestoreEmphasisMs = 130
}

internal fun resolvedMotionDuration(durationMillis: Int, animationsEnabled: Boolean): Int {
    return if (animationsEnabled) durationMillis else AppMotionTokens.disabledDurationMs
}

@Composable
internal fun appMotionFloatState(
    targetValue: Float,
    label: String
): State<Float> {
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    return if (animationsEnabled) {
        animateFloatAsState(
            targetValue = targetValue,
            label = label
        )
    } else {
        rememberUpdatedState(targetValue)
    }
}

@Composable
internal fun appMotionFloatState(
    targetValue: Float,
    durationMillis: Int,
    label: String,
    easing: Easing = FastOutSlowInEasing
): State<Float> {
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    return if (animationsEnabled) {
        animateFloatAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = durationMillis, easing = easing),
            label = label
        )
    } else {
        rememberUpdatedState(targetValue)
    }
}

@Composable
internal fun appMotionDpState(
    targetValue: Dp,
    durationMillis: Int,
    label: String,
    easing: Easing = FastOutSlowInEasing
): State<Dp> {
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    return if (animationsEnabled) {
        animateDpAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = durationMillis, easing = easing),
            label = label
        )
    } else {
        rememberUpdatedState(targetValue)
    }
}

internal fun motionDisabledEasingOrDefault(
    animationsEnabled: Boolean,
    easing: Easing = FastOutSlowInEasing
): Easing {
    return if (animationsEnabled) easing else LinearEasing
}
