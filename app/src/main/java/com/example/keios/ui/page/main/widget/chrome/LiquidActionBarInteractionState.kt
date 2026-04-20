package com.example.keios.ui.page.main.widget.chrome

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.util.fastCoerceIn
import com.example.keios.ui.animation.DampedDragAnimation
import kotlin.math.abs

@Composable
internal fun rememberLiquidActionBarPressedScale(): Float {
    return remember { 62f / 44f }
}

@Composable
internal fun rememberLiquidActionBarDragActivationThresholdPx(): Float {
    val viewConfiguration = LocalViewConfiguration.current
    return remember(viewConfiguration.touchSlop) {
        (viewConfiguration.touchSlop * 1.15f).coerceAtLeast(8f)
    }
}

@Composable
internal fun rememberLiquidActionBarSelectionProgressProvider(
    animation: DampedDragAnimation,
    itemCount: Int
): (Int) -> Float {
    return remember(animation, itemCount) {
        { index ->
            (1f - abs(animation.value - index)).fastCoerceIn(0f, 1f)
        }
    }
}

@Composable
internal fun rememberLiquidActionBarInteractionLockModifier(
    onInteractionChanged: (Boolean) -> Unit
): Modifier {
    return Modifier.pointerInput(onInteractionChanged) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onInteractionChanged(true)
            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                onInteractionChanged(false)
            }
        }
    }
}
