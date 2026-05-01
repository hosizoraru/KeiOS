package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.liquidSliderInteractionLock(
    enabled: Boolean,
    onInteractionChanged: (Boolean) -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(onInteractionChanged) {
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
