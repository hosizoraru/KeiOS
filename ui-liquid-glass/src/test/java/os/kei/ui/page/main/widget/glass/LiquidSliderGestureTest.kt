package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidSliderGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun replacingCallbackDuringGestureDoesNotEndTheInteractionAndUsesTheLatestCallbackOnRelease() {
        val revision = mutableStateOf(0)
        val events = mutableListOf<Pair<Int, Boolean>>()
        composeRule.setContent {
            val captured = revision.value
            Box(Modifier.fillMaxWidth().height(48.dp).testTag("interaction-lock")
                .liquidSliderInteractionLock(true) { events += captured to it })
        }
        composeRule.onNodeWithTag("interaction-lock").performTouchInput { down(center) }
        composeRule.runOnIdle { assertEquals(listOf(0 to true), events); revision.value++ }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(0 to true), events) }
        composeRule.onNodeWithTag("interaction-lock").performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(listOf(0 to true, 1 to false), events) }
    }

    @Test
    fun compactVisualInputsKeepFortyEightDpSemanticRoots() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Column {
                    LiquidMusicProgressSlider(
                        value = { 0.25f },
                        onValueChange = {},
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = null,
                        contentDescription = "Playback position",
                        visualVerticalOffset = 10.dp,
                        modifier = Modifier.height(18.dp).testTag("music-slider"),
                    )
                    LiquidKeyPointSlider(
                        value = { 0.50f },
                        onValueChange = {},
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = emptyBackdrop(),
                        keyPoints = listOf(LiquidSliderKeyPoint(0.5f)),
                        contentDescription = "Key point",
                        modifier = Modifier.height(28.dp).testTag("key-point-slider"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("music-slider")
            .assertHeightIsEqualTo(LiquidSliderMinimumInteractiveHeight)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Playback position"),
                ),
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        composeRule
            .onNodeWithTag("key-point-slider")
            .assertHeightIsAtLeast(LiquidSliderMinimumInteractiveHeight)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Key point"),
                ),
            )
    }

    @Test
    fun keyPointSliderWithoutBackdropKeepsSemanticsAndInteractions() {
        var sliderValue = 0.25f
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LiquidKeyPointSlider(
                    value = { sliderValue },
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    backdrop = null,
                    keyPoints = listOf(LiquidSliderKeyPoint(0.5f)),
                    contentDescription = "Fallback key point",
                    modifier = Modifier.height(28.dp).testTag("fallback-key-point-slider"),
                )
            }
        }

        composeRule
            .onNodeWithTag("fallback-key-point-slider")
            .assertHeightIsAtLeast(LiquidSliderMinimumInteractiveHeight)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Fallback key point"),
                ),
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { action ->
                assertTrue(action(0.75f))
            }
        composeRule.waitForIdle()
        assertEquals(0.75f, sliderValue)

        composeRule.onNodeWithTag("fallback-key-point-slider").performTouchInput {
            click(Offset(x = width * 0.20f, y = height - 2f))
        }
        composeRule.waitForIdle()
        assertTrue(sliderValue < 0.30f)
    }

    @Test
    fun disabledCompactSliderHasNoSetProgressAction() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LiquidVolumeSlider(
                    value = { 0.5f },
                    onValueChange = {},
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    backdrop = null,
                    enabled = false,
                    contentDescription = "Unavailable volume",
                    modifier = Modifier.height(18.dp).testTag("disabled-slider"),
                )
            }
        }

        composeRule
            .onNodeWithTag("disabled-slider")
            .assertHeightIsAtLeast(LiquidSliderMinimumInteractiveHeight)
            .assertIsNotEnabled()
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
    }

    @Test
    fun offsetVisualsStillAcceptTapsAtBothVerticalEdges() {
        var sliderValue = 0.1f
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LiquidMusicProgressSlider(
                    value = { sliderValue },
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    backdrop = emptyBackdrop(),
                    visualVerticalOffset = 10.dp,
                    modifier = Modifier.height(18.dp).testTag("edge-slider"),
                )
            }
        }

        composeRule.onNodeWithTag("edge-slider").performTouchInput {
            click(Offset(x = width * 0.80f, y = 2f))
        }
        assertTrue(sliderValue > 0.70f)

        composeRule.onNodeWithTag("edge-slider").performTouchInput {
            click(Offset(x = width * 0.20f, y = height - 2f))
        }
        assertTrue(sliderValue < 0.30f)
    }

    @Test
    fun reducedMotionSliderTapChangesValueImmediately() {
        var sliderValue = 0.1f
        composeRule.setContent {
            CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    LiquidMusicProgressSlider(
                        value = { sliderValue },
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = emptyBackdrop(),
                        modifier = Modifier.height(18.dp).testTag("reduced-motion-slider"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("reduced-motion-slider").performTouchInput {
            click(Offset(x = width * 0.80f, y = centerY))
        }
        composeRule.waitForIdle()

        assertTrue(sliderValue > 0.70f)
    }

    @Test
    fun rtlTapsMapTheLeftEdgeToTheHighValue() {
        var sliderValue = 0.5f
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    LiquidMusicProgressSlider(
                        value = { sliderValue },
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = emptyBackdrop(),
                        modifier = Modifier.height(18.dp).testTag("rtl-slider"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("rtl-slider").performTouchInput {
            click(Offset(x = width * 0.20f, y = centerY))
        }
        assertTrue(sliderValue > 0.70f)

        composeRule.onNodeWithTag("rtl-slider").performTouchInput {
            click(Offset(x = width * 0.80f, y = centerY))
        }
        assertTrue(sliderValue < 0.30f)
    }

    @Test
    fun verticalGestureOnSliderScrollsParentWithoutChangingValue() {
        var sliderValue = 0.5f
        var valueChangeCount = 0
        var observedScrollValue = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val scrollState = rememberScrollState()
                observedScrollValue = scrollState.value
                Column(
                    Modifier
                        .height(180.dp)
                        .verticalScroll(scrollState),
                ) {
                    LiquidVolumeSlider(
                        value = { sliderValue },
                        onValueChange = { value ->
                            sliderValue = value
                            valueChangeCount++
                        },
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = emptyBackdrop(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("scrolling-liquid-slider"),
                    )
                    Spacer(Modifier.height(600.dp))
                }
            }
        }

        composeRule.onNodeWithTag("scrolling-liquid-slider").performTouchInput {
            down(center)
            moveBy(Offset(0f, -120f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(0, valueChangeCount)
        assertEquals(0.5f, sliderValue)
        assertTrue(observedScrollValue > 0)
    }
}
