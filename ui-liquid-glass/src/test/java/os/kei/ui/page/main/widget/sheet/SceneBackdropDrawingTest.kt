package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LiquidOverlayPortal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35])
class SceneBackdropDrawingTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun overlayMotionDoesNotRecordThePageAgainButPageChangesStayLive() {
        var offset by mutableFloatStateOf(0f)
        var color by mutableStateOf(Color.Blue)
        var presented by mutableStateOf(true)
        var pageDraws = 0
        composeRule.setContent {
            Box(Modifier.size(100.dp).testTag("scene")) {
                SceneBackdropHost(backgroundColor = Color.Black) {
                    Box(
                        Modifier.size(100.dp).drawBehind {
                            pageDraws++
                            drawRect(color)
                        },
                    )
                    if (presented) {
                        LiquidOverlayPortal {
                            Box(
                                Modifier.size(100.dp).drawBehind {
                                    drawRect(Color.Red, Offset(0f, offset), Size(20f, 20f))
                                },
                            )
                        }
                    }
                }
            }
        }
        fun capture() = composeRule.onNodeWithTag("scene").captureToImage().toPixelMap()
        capture()
        val initial = pageDraws
        assertTrue(initial > 0)
        repeat(3) { frame ->
            composeRule.runOnIdle { offset = (frame + 1) * 10f }
            capture()
            assertEquals(initial, pageDraws, "overlay motion must reuse the page recording")
        }
        composeRule.runOnIdle { color = Color.Green }
        val changed = capture()[80, 80]
        assertTrue(pageDraws > initial)
        assertTrue(changed.green > 0.98f && changed.blue < 0.02f, "$changed")
        composeRule.runOnIdle { presented = false; color = Color.Yellow }
        val dismissed = capture()[80, 80]
        assertTrue(dismissed.red > 0.98f && dismissed.green > 0.98f && dismissed.blue < 0.02f, "$dismissed")
        composeRule.runOnIdle { presented = true; color = Color.Cyan }
        val reopened = capture()[80, 80]
        assertTrue(reopened.green > 0.98f && reopened.blue > 0.98f && reopened.red < 0.02f, "$reopened")
    }
}
