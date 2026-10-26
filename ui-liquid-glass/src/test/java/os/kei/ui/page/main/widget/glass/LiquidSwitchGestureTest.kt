package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LiquidSwitchGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rejectedToggleReturnsToTheAuthoritativeStateVisually() {
        val requests = mutableListOf<Boolean>()
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides rememberLayerBackdrop(),
                    LocalTransitionAnimationsEnabled provides false,
                ) {
                    AppSwitch(false, { requests += it }, Modifier.testTag("switch"))
                }
            }
        }

        val before = pixels()
        composeRule.onNodeWithTag("switch").performTouchInput { click() }
        composeRule.onNodeWithTag("switch").assertIsOff()
        composeRule.runOnIdle { assertEquals(listOf(true), requests) }
        assertContentEquals(before, pixels())
    }

    @Test
    fun disablingAndReenablingDuringPressDoesNotToggleAndAllowsTheNextTap() {
        val enabled = mutableStateOf(true)
        val checked = mutableStateOf(false)
        val requests = mutableListOf<Boolean>()
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides rememberLayerBackdrop(),
                    LocalTransitionAnimationsEnabled provides false,
                ) {
                    AppSwitch(
                        checked = checked.value,
                        onCheckedChange = { checked.value = it; requests += it },
                        enabled = enabled.value,
                        modifier = Modifier.testTag("switch"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("switch").performTouchInput { down(center) }
        composeRule.runOnIdle { enabled.value = false }
        composeRule.onNodeWithTag("switch").assertIsNotEnabled()
        composeRule.onNodeWithTag("switch").performTouchInput { up() }
        composeRule.runOnIdle {
            assertEquals(emptyList(), requests)
            enabled.value = true
        }
        composeRule.onNodeWithTag("switch").assertIsOff().performTouchInput { click() }
        composeRule.onNodeWithTag("switch").assertIsOn()
        composeRule.runOnIdle { assertEquals(listOf(true), requests) }
    }

    @Test
    fun canceledDragDiscardsThePreviewWithoutChangingTheSetting() {
        val requests = mutableListOf<Boolean>()
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides rememberLayerBackdrop(),
                    LocalTransitionAnimationsEnabled provides false,
                ) {
                    AppSwitch(false, { requests += it }, Modifier.testTag("switch"))
                }
            }
        }
        val before = pixels()
        composeRule.onNodeWithTag("switch").performTouchInput {
            down(Offset(width * 0.2f, centerY))
            moveBy(Offset(width * 0.4f, 0f))
        }
        composeRule.runOnIdle { assertEquals(emptyList(), requests) }
        composeRule.onNodeWithTag("switch").performTouchInput { cancel() }
        composeRule.onNodeWithTag("switch").assertIsOff()
        composeRule.runOnIdle { assertEquals(emptyList(), requests) }
        assertContentEquals(before, pixels())
    }

    private fun pixels(): IntArray {
        val bitmap = composeRule.onNodeWithTag("switch").captureToImage().asAndroidBitmap()
        return IntArray(bitmap.width * bitmap.height).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
    }
}
