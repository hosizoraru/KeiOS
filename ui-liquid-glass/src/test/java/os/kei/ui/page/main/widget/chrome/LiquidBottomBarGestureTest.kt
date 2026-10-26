package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LiquidBottomBarGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun draggingPreviewsWithoutNavigatingAndCommitsOnceOnRelease() {
        val requests = mutableListOf<Int>()
        setBar { _, index -> requests += index }
        drag()
        composeRule.runOnIdle { assertEquals(emptyList(), requests) }
        composeRule.onNodeWithTag("bar").performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(listOf(2), requests) }
    }

    @Test
    fun cancelingThePreviewRestoresSelectionAndTheNextDragWorks() {
        val requests = mutableListOf<Int>()
        setBar { _, index -> requests += index }
        drag()
        composeRule.onNodeWithTag("bar").performTouchInput { cancel() }
        composeRule.runOnIdle { assertEquals(emptyList(), requests) }
        drag()
        composeRule.onNodeWithTag("bar").performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(listOf(2), requests) }
    }

    @Test
    fun disablingDuringPreviewCancelsWithoutNavigating() {
        val requests = mutableListOf<Int>()
        val enabled = mutableStateOf(true)
        setBar(enabled = enabled) { _, index -> requests += index }
        drag()
        composeRule.runOnIdle { enabled.value = false }
        composeRule.onNodeWithTag("bar").performTouchInput { up() }
        composeRule.runOnIdle {
            assertEquals(emptyList(), requests)
            enabled.value = true
        }
        drag()
        composeRule.onNodeWithTag("bar").performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(listOf(2), requests) }
    }

    @Test
    fun rtlDragUsesTheLogicalTabOrderAndTheLatestCallback() {
        val revision = mutableStateOf(0)
        val requests = mutableListOf<Pair<Int, Int>>()
        setBar(direction = LayoutDirection.Rtl, callbackRevision = revision) { version, index ->
            requests += version to index
        }
        drag(rtl = true)
        composeRule.runOnIdle { revision.value++ }
        composeRule.onNodeWithTag("bar").performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(listOf(1 to 2), requests) }
    }

    private fun drag(rtl: Boolean = false) {
        composeRule.onNodeWithTag("bar").performTouchInput {
            val startX = width * if (rtl) 5f / 6f else 1f / 6f
            val endX = width * if (rtl) 1f / 6f else 5f / 6f
            down(Offset(startX, centerY))
            moveTo(Offset(endX, centerY))
        }
    }

    private fun setBar(
        direction: LayoutDirection = LayoutDirection.Ltr,
        enabled: MutableState<Boolean> = mutableStateOf(true),
        callbackRevision: MutableState<Int> = mutableStateOf(0),
        onSelected: (Int, Int) -> Unit,
    ) {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides direction,
                    LocalTransitionAnimationsEnabled provides false,
                ) {
                    val selection = remember { mutableStateOf(0) }
                    val revision = callbackRevision.value
                    val select: (Int) -> Unit = { index ->
                        onSelected(revision, index)
                        selection.value = index
                    }
                    LiquidGlassBottomBar(
                        modifier = Modifier.testTag("bar"),
                        selectedIndex = selection.value,
                        onSelected = select,
                        backdrop = emptyBackdrop(),
                        tabsCount = 3,
                        interactionEnabled = enabled.value,
                        isLiquidEffectEnabled = false,
                        expandToMaxWidth = true,
                    ) {
                        repeat(3) { index ->
                            LiquidGlassBottomBarItem(
                                selected = selection.value == index,
                                tabIndex = index,
                                onClick = { select(index) },
                            ) { Text("Tab $index") }
                        }
                    }
                }
            }
        }
    }
}
