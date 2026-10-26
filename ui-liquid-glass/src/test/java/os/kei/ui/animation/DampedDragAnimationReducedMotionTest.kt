package os.kei.ui.animation

import android.app.Application
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(
    application = Application::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class DampedDragAnimationReducedMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reducedMotionKeepsImmediateFeedbackWithoutDeformation() {
        val animation = setAnimation(animationsEnabled = false)

        composeRule.runOnIdle { animation.press() }
        composeRule.waitForIdle()

        assertEquals(1f, animation.pressProgress)
        assertEquals(0f, animation.deformationProgress)
        assertEquals(1f, animation.scaleX)
        assertEquals(1f, animation.scaleY)
        assertEquals(0f, animation.velocity)

        composeRule.runOnIdle { animation.release() }
        composeRule.waitForIdle()

        assertEquals(0f, animation.pressProgress)
        assertEquals(0f, animation.deformationProgress)
    }

    @Test
    fun reducedMotionValueMutationsSettleWithinTheCurrentIdleCycle() {
        val animation = setAnimation(animationsEnabled = false)

        composeRule.runOnIdle { animation.updateValue(0.25f) }
        composeRule.waitForIdle()
        assertEquals(0.25f, animation.value)

        composeRule.runOnIdle { animation.animateToValue(0.75f) }
        composeRule.waitForIdle()
        assertEquals(0.75f, animation.value)

        composeRule.runOnIdle {
            animation.animateToValue(0.1f)
            animation.snapToValue(0.9f, updateVelocity = true)
        }
        composeRule.waitForIdle()

        assertEquals(0.9f, animation.value)
        assertEquals(0f, animation.velocity)
    }

    @Test
    fun rapidPressReleasePressKeepsLatestPressedState() {
        val animation = setAnimation(animationsEnabled = false)

        composeRule.runOnIdle {
            animation.press()
            animation.release()
            animation.press()
        }
        composeRule.waitForIdle()

        assertEquals(1f, animation.pressProgress)
        assertEquals(0f, animation.deformationProgress)
    }

    @Test
    fun enabledMotionRetainsAnimatedDeformation() {
        val animation = setAnimation(animationsEnabled = true)

        composeRule.runOnIdle { animation.press() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            animation.pressProgress > 0.99f && animation.scaleX > 1.1f
        }

        assertTrue(animation.deformationProgress > 0.99f)
        assertTrue(animation.scaleY > 1.1f)
    }

    @Test
    fun externalValueTransitionDoesNotInventAPress() {
        val animation = setAnimation(animationsEnabled = true)
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnIdle { animation.animateToValue(0.75f, pressFeedback = false) }
        composeRule.mainClock.advanceTimeBy(160)
        composeRule.runOnIdle {
            assertTrue(animation.value > 0f)
            assertEquals(0f, animation.pressProgress)
            assertEquals(1f, animation.scaleX)
            assertEquals(1f, animation.scaleY)
        }
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.runOnIdle { assertEquals(0.75f, animation.value) }
    }

    @Test
    fun valueOnlyTransitionDoesNotReleaseAnExistingFingerPress() {
        val animation = setAnimation(animationsEnabled = true)
        composeRule.runOnIdle { animation.press() }
        composeRule.waitUntil(timeoutMillis = 5_000) { animation.pressProgress > 0.99f }

        composeRule.runOnIdle { animation.animateToValue(0.75f, pressFeedback = false) }
        composeRule.mainClock.advanceTimeBy(1_500)
        composeRule.runOnIdle {
            assertEquals(0.75f, animation.value)
            assertTrue(animation.pressProgress > 0.99f)
            animation.release()
        }
    }

    private fun setAnimation(animationsEnabled: Boolean): DampedDragAnimation {
        lateinit var animation: DampedDragAnimation
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            animation =
                remember(scope, animationsEnabled) {
                    DampedDragAnimation(
                        animationScope = scope,
                        initialValue = 0f,
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        initialScale = 1f,
                        pressedScale = 1.25f,
                        animationsEnabled = animationsEnabled,
                        onDragStarted = {},
                        onDragStopped = {},
                        onDrag = { _, _ -> },
                    )
                }
        }
        composeRule.waitForIdle()
        return animation
    }
}
