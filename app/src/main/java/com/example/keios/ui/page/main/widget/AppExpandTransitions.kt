package com.example.keios.ui.page.main.widget

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment

internal fun appExpandIn(): EnterTransition {
    return fadeIn(animationSpec = tween(durationMillis = 160)) +
        expandVertically(
            animationSpec = tween(durationMillis = 220),
            expandFrom = Alignment.Top
        )
}

internal fun appExpandOut(): ExitTransition {
    return fadeOut(animationSpec = tween(durationMillis = 120)) +
        shrinkVertically(
            animationSpec = tween(durationMillis = 180),
            shrinkTowards = Alignment.Top
        )
}
