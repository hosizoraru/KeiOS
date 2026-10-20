package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SheetViewportDrawingTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun escapedOverlayDoesNotInheritTheOriginatingScrollClip() {
        val viewport = LiquidSheetDrawViewport()
        var draws = 0
        composeRule.setContent {
            LiquidSheetTestTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.size(200.dp).liquidSheetDrawViewport(viewport))
                    CompositionLocalProvider(LocalLiquidSheetDrawViewport provides viewport) {
                        Box(Modifier.offset(y = 320.dp)) {
                            SheetControlRow(
                                label = "Overlay action",
                                modifier = Modifier.testTag("overlay").height(80.dp).drawWithContent {
                                    draws++
                                    drawContent()
                                },
                            ) {
                                Box(Modifier.size(24.dp).background(Color.Red))
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay").captureToImage()
        composeRule.onNodeWithText("Overlay action").assertIsDisplayed()
        assertTrue(draws > 0, "An overlay must draw outside its originating sheet viewport")
    }

    @Test
    fun offscreenRowsKeepStateAndSemanticsWithoutRecordingTheirContent() {
        val drawEpoch = mutableIntStateOf(0)
        val draws = IntArray(12)
        var compositions = 0
        composeRule.setContent {
            LiquidSheetTestTheme {
                Box(Modifier.size(width = 320.dp, height = 200.dp)) {
                    SheetContentColumn(Modifier.testTag("viewport"), verticalSpacing = 0.dp) {
                        repeat(draws.size) { index ->
                            SheetControlRow(
                                label = "Control $index",
                                modifier = Modifier.height(80.dp).drawWithContent {
                                    drawEpoch.intValue
                                    draws[index]++
                                    drawContent()
                                },
                            ) {
                                SideEffect { compositions++ }
                                Box(Modifier.size(24.dp).background(Color.Red))
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("viewport").captureToImage()
        val composedBefore = compositions
        assertTrue(composedBefore >= draws.size, "Every row must remain composed")
        assertEquals(0, draws[10], "The initial draw must also skip clipped glass content")
        composeRule.runOnIdle {
            draws.fill(0)
            drawEpoch.intValue++
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("viewport").captureToImage()

        composeRule.onNodeWithText("Control 10").assertExists().assertIsNotDisplayed()
        assertTrue(draws[0] > 0, "Visible content must still be recorded")
        assertEquals(0, draws[10], "Clipped glass content must not be recorded")

        composeRule.onNodeWithTag("viewport").performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 800.dp.value * composeRule.density.density)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("viewport").captureToImage()
        composeRule.onNodeWithText("Control 10").assertIsDisplayed()
        assertTrue(draws[10] > 0, "Revealing a row must restore its drawing")
        assertEquals(composedBefore, compositions, "Scrolling must preserve composed row state")
    }
}
