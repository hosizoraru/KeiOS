/*
 * Adapted from compose-floating-tab-bar.
 * Copyright 2025 Elyes Mansour.
 * SPDX-License-Identifier: Apache-2.0
 */

package os.kei.ui.component.floatingtabbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A [NestedScrollConnection] that handles scroll events to transition between inline and expanded states.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded).
 * @param scrollThresholdPx The minimum scroll distance in pixels required to trigger a state change.
 * @param inlineBehavior Defines when the tab bar should transition to inline state.
 */
class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
    private val inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Return early for the fixed expanded mode
        if (inlineBehavior == FloatingTabBarInlineBehavior.Never) {
            return Offset.Zero
        }

        val scrollDelta = available.y

        // Reset accumulated scroll if changing direction
        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }

        // Accumulate scroll
        accumulatedScroll += scrollDelta

        when (inlineBehavior) {
            FloatingTabBarInlineBehavior.OnScrollDown -> {
                // Check if we've scrolled enough to trigger state change
                if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
                    // Scrolling down enough - transition to inline mode
                    isInline = true
                    accumulatedScroll = 0f // Reset after state change
                } else if (accumulatedScroll >= scrollThresholdPx && isInline) {
                    // Scrolling up enough - transition to expanded mode
                    isInline = false
                    accumulatedScroll = 0f // Reset after state change
                }
            }

            FloatingTabBarInlineBehavior.OnScrollUp -> {
                // Check if we've scrolled enough to trigger state change
                if (accumulatedScroll >= scrollThresholdPx && !isInline) {
                    // Scrolling up enough - transition to inline mode
                    isInline = true
                    accumulatedScroll = 0f // Reset after state change
                } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
                    // Scrolling down enough - transition to expanded mode
                    isInline = false
                    accumulatedScroll = 0f // Reset after state change
                }
            }

            FloatingTabBarInlineBehavior.Never -> {
                // Handled by the early return above
            }
        }

        return Offset.Zero // Pass the scroll to the child content
    }
}

/**
 * Creates and remembers a [FloatingTabBarScrollConnection] instance.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded). Default is false.
 * @param scrollThreshold The minimum scroll distance required to trigger a state change. Default is 50.dp.
 * @param inlineBehavior Defines when the tab bar should transition to inline state. Default is [FloatingTabBarInlineBehavior.OnScrollDown].
 * @return A remembered [FloatingTabBarScrollConnection] instance.
 */
@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 50.dp,
    inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    remember(scrollThresholdPx, inlineBehavior, initialIsInline) {
        FloatingTabBarScrollConnection(initialIsInline, scrollThresholdPx, inlineBehavior)
    }
}

/**
 * Defines when the floating tab bar should transition to inline state.
 */
enum class FloatingTabBarInlineBehavior {
    /** Never transition to inline - it stays in expanded state */
    Never,

    /** Transition to inline when scrolling down */
    OnScrollDown,

    /** Transition to inline when scrolling up */
    OnScrollUp
}
