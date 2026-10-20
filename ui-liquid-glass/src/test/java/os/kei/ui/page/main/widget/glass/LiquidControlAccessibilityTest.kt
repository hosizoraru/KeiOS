package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidControlAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun checkboxAndSwitchExposeMinimumTouchTargets() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                androidx.compose.foundation.layout.Column {
                    AppLiquidCheckbox(
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("liquid-checkbox"),
                        contentDescription = "Selection",
                    )
                    AppSwitch(
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("liquid-switch"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("liquid-checkbox")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag("liquid-switch")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun switchRespondsToPhysicalTap() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                var checked by remember { mutableStateOf(false) }
                AppSwitch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.testTag("liquid-switch"),
                )
            }
        }

        composeRule
            .onNode(isToggleable())
            .performTouchInput { click() }
            .assertIsOn()
    }

    @Test
    fun switchWithParentBackdropRespondsToPhysicalTap() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val parentBackdrop = rememberLayerBackdrop()
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides parentBackdrop,
                    LocalRetainedLiquidMaterial provides true,
                ) {
                    var checked by remember { mutableStateOf(false) }
                    AppSwitch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.testTag("parent-liquid-switch"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("parent-liquid-switch")
            .performTouchInput { click() }
        composeRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun switchResolvesOnlyAnEnabledParentBackdrop() {
        var expectedBackdrop: Backdrop? = null
        var resolvedBackdrop: Backdrop? = null
        var disabledBackdrop: Backdrop? = null
        var standaloneBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val parentBackdrop = rememberLayerBackdrop()
                expectedBackdrop = parentBackdrop
                CompositionLocalProvider(LocalLiquidParentBackdrop provides parentBackdrop) {
                    resolvedBackdrop = resolvedAppSwitchBackdrop()
                    CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                        disabledBackdrop = resolvedAppSwitchBackdrop()
                    }
                }
                standaloneBackdrop = resolvedAppSwitchBackdrop()
            }
        }

        composeRule.runOnIdle {
            assertSame(expectedBackdrop, resolvedBackdrop)
            assertNull(disabledBackdrop)
            assertNull(standaloneBackdrop)
        }
    }

    @Test
    fun reducedMotionSwitchRespondsToPhysicalTap() {
        composeRule.setContent {
            CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    var checked by remember { mutableStateOf(false) }
                    AppSwitch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.testTag("reduced-motion-liquid-switch"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("reduced-motion-liquid-switch")
            .performTouchInput { click() }
        composeRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun cancelledSwitchGestureDoesNotToggle() {
        var callbackCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppSwitch(
                    checked = false,
                    onCheckedChange = { callbackCount++ },
                    modifier = Modifier.testTag("liquid-switch"),
                )
            }
        }

        composeRule
            .onNodeWithTag("liquid-switch")
            .performTouchInput {
                down(center)
                cancel()
            }
        composeRule.onNode(isToggleable()).assertIsOff()
        assertEquals(0, callbackCount)
    }

    @Test
    fun verticalGestureOnSwitchScrollsParentWithoutToggling() {
        var callbackCount = 0
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
                    AppSwitch(
                        checked = false,
                        onCheckedChange = { callbackCount++ },
                        modifier = Modifier.testTag("scrolling-liquid-switch"),
                    )
                    Spacer(Modifier.height(600.dp))
                }
            }
        }

        composeRule.onNodeWithTag("scrolling-liquid-switch").performTouchInput {
            down(center)
            moveBy(Offset(0f, -120f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(0, callbackCount)
        kotlin.test.assertTrue(observedScrollValue > 0)
    }

    @Test
    fun splitHorizontalMovementCrossingTouchSlopDoesNotFallBackToTapInLtr() {
        verifySplitHorizontalMovementDoesNotFallBackToTap(
            layoutDirection = LayoutDirection.Ltr,
            movementSign = 1f,
        )
    }

    @Test
    fun splitHorizontalMovementCrossingTouchSlopDoesNotFallBackToTapInRtl() {
        verifySplitHorizontalMovementDoesNotFallBackToTap(
            layoutDirection = LayoutDirection.Rtl,
            movementSign = -1f,
        )
    }

    private fun verifySplitHorizontalMovementDoesNotFallBackToTap(
        layoutDirection: LayoutDirection,
        movementSign: Float,
    ) {
        var touchSlop = 0f
        val selectedValues = mutableListOf<Boolean>()
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    val parentBackdrop = rememberLayerBackdrop()
                    CompositionLocalProvider(LocalLiquidParentBackdrop provides parentBackdrop) {
                        var checked by remember { mutableStateOf(false) }
                        val viewConfiguration = LocalViewConfiguration.current
                        SideEffect { touchSlop = viewConfiguration.touchSlop }
                        AppSwitch(
                            checked = checked,
                            onCheckedChange = { selected ->
                                selectedValues += selected
                                checked = selected
                            },
                            modifier = Modifier.testTag("split-drag-liquid-switch"),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("split-drag-liquid-switch").performTouchInput {
            val partialMove = Offset(x = movementSign * touchSlop * 0.6f, y = 0f)
            down(center)
            moveBy(partialMove)
            moveBy(partialMove)
            up()
        }
        composeRule.waitForIdle()

        composeRule.onNode(isToggleable()).assertIsOff()
        assertEquals(listOf(false), selectedValues)
    }

    @Test
    fun dropdownChoicesExposeSelectionStateAndMinimumTouchTarget() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                androidx.compose.foundation.layout.Column {
                    LiquidGlassDropdownSingleChoiceItem(
                        text = "Single",
                        optionSize = 1,
                        isSelected = true,
                        index = 0,
                        onSelectedIndexChange = {},
                        modifier = Modifier.testTag("single-choice"),
                    )
                    LiquidGlassDropdownItem(
                        text = "Multiple",
                        selected = true,
                        onClick = {},
                        itemType = LiquidGlassDropdownItemType.MultipleChoice,
                        modifier = Modifier.testTag("multiple-choice"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("single-choice")
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag("multiple-choice")
            .assertIsOn()
            .assertHeightIsAtLeast(48.dp)
    }
}
