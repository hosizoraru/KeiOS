package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.sheet.LiquidGlassBottomSheet
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LiquidPresentationOwnerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun callerArgumentChangesRefreshTheHost() {
        val label = mutableStateOf("first")
        val host = LiquidOverlayHostState()
        composeRule.setContent {
            TestHost(host) {
                TaggedOverlay(label.value)
            }
        }
        settle()
        composeRule.onNodeWithTag("first").assertExists()

        composeRule.runOnIdle {
            label.value = "second"
        }
        settle()
        composeRule.onNodeWithTag("first").assertDoesNotExist()
        composeRule.onNodeWithTag("second").assertExists()
    }

    @Test
    fun closingWhileCoveredCannotUnmountTheNextOpening() {
        val active = mutableStateOf(true)
        val show = mutableStateOf(true)
        val host = LiquidOverlayHostState()
        composeRule.setContent {
            TestHost(host) {
                CompositionLocalProvider(LocalLiquidPresentationOwnerActive provides active.value) {
                    LiquidGlassBottomSheet(show = show.value, title = "Actions") {
                        Box(Modifier.size(96.dp).testTag("reopened-sheet"))
                    }
                }
            }
        }
        settle()
        composeRule.runOnIdle {
            active.value = false
            show.value = false
        }
        settle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { active.value = true }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle { assertFalse(host.hasPresentation) }
        composeRule.runOnIdle { show.value = true }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reopened-sheet").assertExists()
        assertTrue(host.hasPresentation)
    }

    @Test
    fun retainedSheetLeavesTheOverlayAndBackStackWhileItsPageIsCovered() {
        val active = mutableStateOf(true)
        val show = mutableStateOf(true)
        val host = LiquidOverlayHostState()
        var dismissCount = 0
        var pageBackCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            TestHost(host) {
                val dispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
                SideEffect { backDispatcher = dispatcher }
                BackHandler { pageBackCount++ }
                CompositionLocalProvider(LocalLiquidPresentationOwnerActive provides active.value) {
                    LiquidGlassBottomSheet(
                        show = show.value,
                        title = "Retained sheet",
                        onDismissRequest = { dismissCount++ },
                    ) {
                        Box(Modifier.size(96.dp).testTag("sheet"))
                    }
                    LiquidOverlayPortal(layer = LiquidOverlayLayer.Notification) {
                        Box(Modifier.size(24.dp).testTag("toast"))
                    }
                }
            }
        }
        settle()
        composeRule.onNodeWithTag("sheet").assertExists()
        assertTrue(host.hasPresentation)

        composeRule.runOnIdle { active.value = false }
        settle()
        composeRule.onNodeWithTag("sheet").assertDoesNotExist()
        composeRule.onNodeWithTag("toast").assertExists()
        assertFalse(host.hasPresentation)
        composeRule.runOnIdle { backDispatcher.onBackPressed() }
        assertEquals(1, pageBackCount)
        assertEquals(0, dismissCount)
        assertTrue(show.value)

        composeRule.runOnIdle { active.value = true }
        settle()
        composeRule.onNodeWithTag("sheet").assertExists()
        assertTrue(host.hasPresentation)
    }

    @Test
    fun retainedPopupDoesNotCoverTheNextPage() {
        val active = mutableStateOf(true)
        val host = LiquidOverlayHostState()
        composeRule.setContent {
            TestHost(host) {
                CompositionLocalProvider(LocalLiquidPresentationOwnerActive provides active.value) {
                    SnapshotWindowListPopup(
                        show = true,
                        anchorBounds = IntRect(120, 180, 260, 240),
                    ) {
                        Box(Modifier.size(96.dp).testTag("popup"))
                    }
                }
            }
        }
        settle()
        composeRule.onNodeWithTag("popup").assertExists()

        composeRule.runOnIdle { active.value = false }
        settle()
        composeRule.onNodeWithTag("popup").assertDoesNotExist()
        assertFalse(host.hasPresentation)
    }

    @Test
    fun coveredOwnerCannotRenderAModalEvenWithoutAnOverlayHost() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLiquidPresentationOwnerActive provides false) {
                LiquidOverlayPortal {
                    Box(Modifier.size(96.dp).testTag("inactive-presentation"))
                }
            }
        }
        composeRule.onNodeWithTag("inactive-presentation").assertDoesNotExist()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
    }
}

@Composable
private fun TaggedOverlay(label: String) {
    LiquidOverlayPortal {
        Box(Modifier.size(96.dp).testTag(label))
    }
}

@Composable
private fun TestHost(host: LiquidOverlayHostState, content: @Composable () -> Unit) {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        CompositionLocalProvider(LocalLiquidOverlayHost provides host) {
            Box(Modifier.fillMaxSize()) {
                content()
                host.Content()
            }
        }
    }
}
