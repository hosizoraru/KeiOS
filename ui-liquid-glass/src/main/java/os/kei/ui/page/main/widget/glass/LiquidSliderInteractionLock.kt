package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal fun Modifier.liquidSliderInteractionLock(
    enabled: Boolean,
    onInteractionChanged: (Boolean) -> Unit
): Modifier {
    if (!enabled) return this
    val currentCallback = rememberUpdatedState(onInteractionChanged)
    // Progress polling and preview changes replace callbacks while a finger is still down.
    // Restarting pointerInput for those replacements cancels the interaction prematurely.
    return pointerInput(enabled) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            currentCallback.value(true)
            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                currentCallback.value(false)
            }
        }
    }
}
