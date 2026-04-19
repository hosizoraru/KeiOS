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
internal fun appFloatingEnter(useNewBottomBarTransition: Boolean = false): EnterTransition {
    if (!LocalTransitionAnimationsEnabled.current) return EnterTransition.None
    val fadeInDuration = if (useNewBottomBarTransition) 130 else AppMotionTokens.floatingFadeInMs
    val slideInDuration = if (useNewBottomBarTransition) 260 else AppMotionTokens.floatingSlideInMs
    return fadeIn(animationSpec = tween(durationMillis = fadeInDuration)) +
        slideInVertically(
            animationSpec = tween(durationMillis = slideInDuration),
            initialOffsetY = { fullHeight ->
                if (useNewBottomBarTransition) (fullHeight * 0.32f).toInt() else fullHeight / 2
            }
        )
}

@Composable
internal fun appFloatingExit(useNewBottomBarTransition: Boolean = false): ExitTransition {
    if (!LocalTransitionAnimationsEnabled.current) return ExitTransition.None
    val fadeOutDuration = if (useNewBottomBarTransition) 100 else AppMotionTokens.floatingFadeOutMs
    val slideOutDuration = if (useNewBottomBarTransition) 220 else AppMotionTokens.floatingSlideOutMs
    return fadeOut(animationSpec = tween(durationMillis = fadeOutDuration)) +
        slideOutVertically(
            animationSpec = tween(durationMillis = slideOutDuration),
            targetOffsetY = { fullHeight ->
                if (useNewBottomBarTransition) (fullHeight * 0.36f).toInt() else fullHeight / 2
            }
        )
}
