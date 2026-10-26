package os.kei.ui.animation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.core.ui.gesture.inspectDragGestures
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class GlassGestureLifecycleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resettingPointerInputCancelsOnceAndTheNextGestureCompletes() {
        val revision = mutableStateOf(0)
        val events = mutableListOf<String>()
        composeRule.setContent {
            Box(
                Modifier.size(120.dp).testTag("target").pointerInput(revision.value) {
                    inspectDragGestures(
                        onDragStart = { events += "start" },
                        onDragEnd = { events += "end" },
                        onDragCancel = { events += "cancel" },
                        onDrag = { _, _ -> },
                    )
                },
            )
        }

        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.runOnIdle { revision.value++ }
        composeRule.runOnIdle { assertEquals(listOf("start", "cancel"), events) }
        composeRule.onNodeWithTag("target").performTouchInput {
            up()
            click()
        }
        composeRule.runOnIdle { assertEquals(listOf("start", "cancel", "start", "end"), events) }
    }

    @Test
    fun removingTheTargetCancelsAnActiveGestureOnce() {
        val visible = mutableStateOf(true)
        val events = mutableListOf<String>()
        composeRule.setContent {
            if (visible.value) {
                Box(
                    Modifier.size(120.dp).testTag("target").pointerInput(Unit) {
                        inspectDragGestures(
                            onDragStart = { events += "start" },
                            onDragEnd = { events += "end" },
                            onDragCancel = { events += "cancel" },
                            onDrag = { _, _ -> },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.runOnIdle { visible.value = false }
        composeRule.runOnIdle { assertEquals(listOf("start", "cancel"), events) }
    }

    @Test
    fun systemCancellationDoesNotCompleteTheGesture() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            Box(
                Modifier.size(120.dp).testTag("target").pointerInput(Unit) {
                    inspectDragGestures(
                        onDragStart = { events += "start" },
                        onDragEnd = { events += "end" },
                        onDragCancel = { events += "cancel" },
                        onDrag = { _, _ -> },
                    )
                },
            )
        }
        composeRule.onNodeWithTag("target").performTouchInput {
            down(center)
            cancel()
        }
        composeRule.runOnIdle { assertEquals(listOf("start", "cancel"), events) }
    }

    @Test
    fun replacingTheDragAnimationRebindsInputAndCancelsThePreviousOwner() {
        val revision = mutableStateOf(0)
        val events = mutableListOf<Pair<Int, String>>()
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val version = revision.value
            val animation = remember(scope, version) {
                DampedDragAnimation(
                    animationScope = scope,
                    initialValue = 0f,
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    initialScale = 1f,
                    pressedScale = 1.25f,
                    animationsEnabled = false,
                    onDragStarted = { events += version to "start" },
                    onDragStopped = { events += version to "end" },
                    onDragCancelled = { events += version to "cancel" },
                    onDrag = { _, _ -> },
                )
            }
            Box(Modifier.size(120.dp).testTag("target").then(animation.modifier))
        }

        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.runOnIdle { revision.value++ }
        composeRule.runOnIdle { assertEquals(listOf(0 to "start", 0 to "cancel"), events) }
        composeRule.onNodeWithTag("target").performTouchInput {
            up()
            click()
        }
        composeRule.runOnIdle {
            assertEquals(listOf(0 to "start", 0 to "cancel", 1 to "start", 1 to "end"), events)
        }
    }

    @Test
    fun replacingTheHighlightReleasesItsOldPressAndDrivesTheNewOwner() {
        val revision = mutableStateOf(0)
        lateinit var highlight: InteractiveHighlight
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            highlight = remember(scope, revision.value) {
                InteractiveHighlight(animationScope = scope, animationsEnabled = false)
            }
            Box(Modifier.size(120.dp).testTag("target").then(highlight.gestureModifier))
        }

        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        val oldHighlight = highlight
        composeRule.runOnIdle {
            assertEquals(1f, oldHighlight.pressProgress)
            revision.value++
        }
        composeRule.runOnIdle { assertEquals(0f, oldHighlight.pressProgress) }
        composeRule.onNodeWithTag("target").performTouchInput {
            up()
            down(Offset(24f, 36f))
        }
        composeRule.runOnIdle {
            assertEquals(1f, highlight.pressProgress)
            assertEquals(Offset(24f, 36f), highlight.touchPosition)
        }
        composeRule.onNodeWithTag("target").performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(0f, highlight.pressProgress) }
    }
}
