package os.kei.ui.page.main.widget.chrome

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ScrollChromeVisibilityControllerTest {
    @Test
    fun `hide after enough upward scroll within list bounds`() {
        val controller = ScrollChromeVisibilityController(hideThresholdPx = 20f)
        var visible = true

        controller.updateWithinScrollBounds(
            deltaY = -12f,
            visible = visible,
            canScrollBackward = true,
            canScrollForward = true
        ) { visible = it }
        controller.updateWithinScrollBounds(
            deltaY = -12f,
            visible = visible,
            canScrollBackward = true,
            canScrollForward = true
        ) { visible = it }

        assertFalse(visible)
    }

    @Test
    fun `show after enough downward scroll within list bounds`() {
        val controller = ScrollChromeVisibilityController(hideThresholdPx = 20f)
        var visible = false

        controller.updateWithinScrollBounds(
            deltaY = 6f,
            visible = visible,
            canScrollBackward = true,
            canScrollForward = true
        ) { visible = it }
        controller.updateWithinScrollBounds(
            deltaY = 7f,
            visible = visible,
            canScrollBackward = true,
            canScrollForward = true
        ) { visible = it }

        assertTrue(visible)
    }

    @Test
    fun `hidden chrome recovers when downward drag reaches top boundary`() {
        val controller = ScrollChromeVisibilityController(hideThresholdPx = 20f)
        var visible = false

        controller.updateWithinScrollBounds(
            deltaY = 6f,
            visible = visible,
            canScrollBackward = true,
            canScrollForward = true
        ) { visible = it }
        controller.updateWithinScrollBounds(
            deltaY = 7f,
            visible = visible,
            canScrollBackward = false,
            canScrollForward = true
        ) { visible = it }

        assertTrue(visible)
    }

    @Test
    fun `visible chrome keeps boundary guard for upward drag at bottom`() {
        val controller = ScrollChromeVisibilityController(hideThresholdPx = 20f)
        var visible = true

        controller.updateWithinScrollBounds(
            deltaY = -24f,
            visible = visible,
            canScrollBackward = true,
            canScrollForward = false
        ) { visible = it }

        assertTrue(visible)
    }
}
