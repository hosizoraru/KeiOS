package com.example.keios.ui.page.main.widget

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
internal fun appExpandIn(): EnterTransition {
    if (!LocalTransitionAnimationsEnabled.current) return EnterTransition.None
    return fadeIn(animationSpec = tween(durationMillis = AppMotionTokens.expandFadeInMs)) +
        expandVertically(
            animationSpec = tween(durationMillis = AppMotionTokens.expandSizeInMs),
            expandFrom = Alignment.Top
        )
}

@Composable
internal fun appExpandOut(): ExitTransition {
    if (!LocalTransitionAnimationsEnabled.current) return ExitTransition.None
    return fadeOut(animationSpec = tween(durationMillis = AppMotionTokens.expandFadeOutMs)) +
        shrinkVertically(
            animationSpec = tween(durationMillis = AppMotionTokens.expandSizeOutMs),
            shrinkTowards = Alignment.Top
        )
}

@Composable
internal fun appFloatingEnter(): EnterTransition {
    if (!LocalTransitionAnimationsEnabled.current) return EnterTransition.None
    return fadeIn(animationSpec = tween(durationMillis = AppMotionTokens.floatingFadeInMs)) +
        slideInVertically(
            animationSpec = tween(durationMillis = AppMotionTokens.floatingSlideInMs),
            initialOffsetY = { it / 2 }
        )
}

@Composable
internal fun appFloatingExit(): ExitTransition {
    if (!LocalTransitionAnimationsEnabled.current) return ExitTransition.None
    return fadeOut(animationSpec = tween(durationMillis = AppMotionTokens.floatingFadeOutMs)) +
        slideOutVertically(
            animationSpec = tween(durationMillis = AppMotionTokens.floatingSlideOutMs),
            targetOffsetY = { it / 2 }
        )
}
