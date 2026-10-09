package os.kei.ui.page.main.student.model3d

import android.view.MotionEvent

/** Observe a tap without taking ownership of the WebView's orbit, pan or pinch gestures. */
internal class GuideModel3dTapObserver(private val slop: Float) {
    private var candidate = false
    private var x = 0f
    private var y = 0f
    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { candidate = true; x = event.x; y = event.y }
            MotionEvent.ACTION_MOVE -> if (kotlin.math.abs(event.x-x) > slop || kotlin.math.abs(event.y-y) > slop) candidate = false
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> candidate = false
            MotionEvent.ACTION_UP -> {
                val tapped = candidate && kotlin.math.abs(event.x-x) <= slop && kotlin.math.abs(event.y-y) <= slop
                candidate = false
                return tapped
            }
        }
        return false
    }
}
