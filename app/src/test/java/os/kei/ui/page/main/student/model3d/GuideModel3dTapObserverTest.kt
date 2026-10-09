package os.kei.ui.page.main.student.model3d

import android.view.MotionEvent
import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class GuideModel3dTapObserverTest {
    @Test fun `single taps restore chrome but orbit pinch and cancelled streams do not`() {
        val observer = GuideModel3dTapObserver(8f)
        fun event(action: Int, x: Float = 100f) = MotionEvent.obtain(0, 10, action, x, 100f, 0).let {
            try { observer.onTouch(it) } finally { it.recycle() }
        }
        assertFalse(event(MotionEvent.ACTION_DOWN)); assertTrue(event(MotionEvent.ACTION_UP, 102f))
        assertFalse(event(MotionEvent.ACTION_DOWN)); assertFalse(event(MotionEvent.ACTION_MOVE, 160f))
        assertFalse(event(MotionEvent.ACTION_UP)) // Returning to the start is still a drag.
        event(MotionEvent.ACTION_DOWN); event(MotionEvent.ACTION_POINTER_DOWN)
        assertFalse(event(MotionEvent.ACTION_UP))
        event(MotionEvent.ACTION_DOWN); event(MotionEvent.ACTION_CANCEL)
        assertFalse(event(MotionEvent.ACTION_UP))
        event(MotionEvent.ACTION_DOWN); assertTrue(event(MotionEvent.ACTION_UP))
    }
}
