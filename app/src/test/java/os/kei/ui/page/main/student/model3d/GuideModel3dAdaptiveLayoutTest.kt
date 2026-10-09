package os.kei.ui.page.main.student.model3d

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w1280dp-h800dp-xxhdpi")
class GuideModel3dAdaptiveLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `floating tools never resize or remount media when opening closing or hiding controls`() {
        val show = mutableStateOf(true); val controls = mutableStateOf(true)
        var mounts = 0; var disposals = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                GuideModel3dAdaptiveLayout(show.value, controls.value, tools = { Box(Modifier.testTag("tools")) }) { wide, _ ->
                    assertTrue(wide)
                    DisposableEffect(Unit) { mounts++; onDispose { disposals++ } }
                }
            }
        }
        composeRule.waitForIdle()
        val media = composeRule.onNodeWithTag("guide_model3d_media").fetchSemanticsNode().boundsInRoot
        val panel = composeRule.onNodeWithTag("guide_model3d_side_tools").fetchSemanticsNode().boundsInRoot
        assertTrue(panel.left > media.left && panel.right <= media.right)
        assertEquals(1280.dp, with(composeRule.density) { media.width.toDp() })
        composeRule.runOnIdle { controls.value = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guide_model3d_side_tools").assertDoesNotExist()
        composeRule.runOnIdle { controls.value = true; show.value = false }
        composeRule.waitForIdle()
        assertEquals(1, mounts); assertEquals(0, disposals)
        assertEquals(media, composeRule.onNodeWithTag("guide_model3d_media").fetchSemanticsNode().boundsInRoot)
    }
    @Test fun `phone portrait and short landscape windows keep compact tools while spacious Pad uses side tools`() {
        assertFalse(guideModel3dUsesSideTools(360.dp, 800.dp))
        assertFalse(guideModel3dUsesSideTools(960.dp, 420.dp))
        assertFalse(guideModel3dUsesSideTools(800.dp, 1280.dp))
        assertTrue(guideModel3dUsesSideTools(1280.dp, 800.dp))
        assertTrue(guideModel3dUsesSideTools(840.dp, 600.dp))
    }
    @Test fun `media bounds stay constant during each panel animation frame`() {
        val show = mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalTransitionAnimationsEnabled provides true) {
                GuideModel3dAdaptiveLayout(show.value, true, tools = { Box(Modifier.testTag("tools")) }) { _, _ -> }
            }
        }
        val initial = composeRule.onNodeWithTag("guide_model3d_media").fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle { show.value = true }
        repeat(12) {
            composeRule.mainClock.advanceTimeByFrame()
            assertEquals(initial, composeRule.onNodeWithTag("guide_model3d_media").fetchSemanticsNode().boundsInRoot)
        }
        composeRule.runOnIdle { show.value = false }
        repeat(12) {
            composeRule.mainClock.advanceTimeByFrame()
            assertEquals(initial, composeRule.onNodeWithTag("guide_model3d_media").fetchSemanticsNode().boundsInRoot)
        }
    }
}
