@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import os.kei.ui.page.main.widget.glass.LiquidOverlayPortal
import os.kei.ui.page.main.widget.glass.LocalLiquidOverlayDepth
import os.kei.ui.page.main.widget.glass.LocalLiquidPresentationOwnerActive
import os.kei.ui.page.main.widget.glass.presentationGlassBlur
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.sheet.BlockedDismissRequestGate
import os.kei.ui.page.main.widget.sheet.LiquidSheetScrim
import os.kei.ui.page.main.widget.sheet.LocalSceneBackdrop
import os.kei.ui.page.main.widget.sheet.liquidSheetOutsideDismiss
import top.yukonga.miuix.kmp.anim.folmeSpring

internal enum class LiquidModalPlacement {
    /** Alerts and dialogs: a card in the middle of the screen that scales in. */
    Center,

    /** Action sheets: a card at the bottom edge that rises into place. */
    Bottom,
}

private const val LIQUID_MODAL_ENTER_DAMPING = 0.86f
private const val LIQUID_MODAL_ENTER_RESPONSE = 0.32f
private const val LIQUID_MODAL_EXIT_DAMPING = 1f
private const val LIQUID_MODAL_EXIT_RESPONSE = 0.24f

/**
 * Shared scaffolding for every modal presentation in the family — alerts, action sheets and the
 * legacy dialog.
 *
 * Holds exactly the parts that were getting reinvented per component, and holds them the same way the
 * sheet does:
 *
 * - **In-window, through [LiquidOverlayPortal].** A `LayerBackdrop` cannot be sampled from a Dialog
 *   window, so anything hosted there gets no glass at all — `blur()` resolves against
 *   `emptyBackdrop()` and silently draws nothing. That is what left the old dialog readable straight
 *   through.
 * - **One motion driver.** A single `progress` from 0 to 1 feeds the scrim, the card's scale and its
 *   alpha, and the same animation runs both ways. The scrim therefore reaches zero exactly as the
 *   card disappears, so there is no dimmed frame left to cut — the flinch the sheet used to have.
 * - **One dismissal.** A dedup gate means a doubled back press or a double-tapped Cancel dispatches
 *   one request, not two.
 */
@Composable
internal fun LiquidModalPresentation(
    show: Boolean,
    placement: LiquidModalPlacement,
    dismissible: Boolean,
    onDismissRequest: (() -> Unit)?,
    onDismissFinished: (() -> Unit)?,
    content: @Composable (progressProvider: () -> Float) -> Unit,
) {
    var keepMounted by remember { mutableStateOf(false) }
    if (!LocalLiquidPresentationOwnerActive.current) {
        SideEffect { keepMounted = false }
        return
    }
    if (!show && !keepMounted) return

    LiquidOverlayPortal {
        val transitionsEnabled = LocalTransitionAnimationsEnabled.current
        val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
        val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)

        val progress = remember { mutableFloatStateOf(0f) }
        // Coalesces the burst of requests one user action can produce — a doubled back press, a
        // double-tapped Cancel — into a single dispatch, then releases itself so a caller that
        // declines the first request can still be dismissed on the next try.
        val dismissGate = remember(show) { BlockedDismissRequestGate() }
        val scrimBlurRadius = presentationGlassBlur()

        suspend fun animateProgressTo(
            target: Float,
            spec: AnimationSpec<Float>,
        ) {
            animate(
                initialValue = progress.floatValue,
                targetValue = target,
                animationSpec = spec,
            ) { value, _ -> progress.floatValue = value }
            progress.floatValue = target
        }

        LaunchedEffect(show, transitionsEnabled) {
            if (show) {
                keepMounted = true
                if (transitionsEnabled) {
                    animateProgressTo(
                        1f,
                        folmeSpring(
                            damping = LIQUID_MODAL_ENTER_DAMPING,
                            response = LIQUID_MODAL_ENTER_RESPONSE,
                        ),
                    )
                } else {
                    progress.floatValue = 1f
                }
            } else {
                if (transitionsEnabled && progress.floatValue > 0f) {
                    animateProgressTo(
                        0f,
                        folmeSpring(
                            damping = LIQUID_MODAL_EXIT_DAMPING,
                            response = LIQUID_MODAL_EXIT_RESPONSE,
                        ),
                    )
                } else {
                    progress.floatValue = 0f
                }
                currentOnDismissFinished?.invoke()
                keepMounted = false
            }
        }

        // The caller flips `show`; the LaunchedEffect above owns the exit animation.
        val requestDismiss: () -> Unit = {
            if (dismissible) {
                dismissGate.dispatch { currentOnDismissRequest?.invoke() }
            }
        }

        BackHandler(enabled = show && dismissible, onBack = requestDismiss)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = when (placement) {
                LiquidModalPlacement.Center -> Alignment.Center
                LiquidModalPlacement.Bottom -> Alignment.BottomCenter
            },
        ) {
            LiquidSheetScrim(
                // Only the bottom-most overlay blurs. The scene backdrop holds the app, not other
                // overlays, so a stacked card's full-screen plate would paint a blurred page straight
                // over the sheet it was opened from and the sheet would look like it had closed. A
                // dialog over a sheet dims the sheet instead, which is what iOS does anyway.
                backdrop = LocalSceneBackdrop.current.takeIf { LocalLiquidOverlayDepth.current == 0 },
                blurRadius = scrimBlurRadius,
                presentationProvider = { progress.floatValue.coerceIn(0f, 1f) },
                // A card does not cover the screen, so the blurred plate spans all of it rather than
                // stopping at an edge the way the sheet's does.
                blurHeightPxProvider = { Int.MAX_VALUE },
            )
            // Outside-tap dismissal, and a hard stop for drags: without the claim a drag that starts
            // on the scrim reaches the pager and the page switches behind a modal card.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquidSheetOutsideDismiss(
                        allowDismiss = dismissible,
                        onDismissRequest = requestDismiss,
                        onBlockedDismissRequest = null,
                    ),
            )
            content { progress.floatValue.coerceIn(0f, 1f) }
        }
    }
}

/** Kept out of the composable so the scrim/scale relationship can be unit-tested. */
internal fun liquidModalCardScale(progress: Float): Float {
    val eased = progress.coerceIn(0f, 1f)
    return LIQUID_MODAL_MIN_SCALE + (1f - LIQUID_MODAL_MIN_SCALE) * eased
}

internal fun liquidModalCardAlpha(progress: Float): Float =
    // Reaches full opacity before the scale finishes, so the card never looks like it is fading in
    // separately from the scrim.
    (progress.coerceIn(0f, 1f) * LIQUID_MODAL_ALPHA_GAIN).coerceIn(0f, 1f)

internal fun liquidModalBottomOffsetPx(
    progress: Float,
    cardHeightPx: Float,
): Float = ((1f - progress.coerceIn(0f, 1f)) * cardHeightPx).coerceAtLeast(0f)

private const val LIQUID_MODAL_MIN_SCALE = 0.88f
private const val LIQUID_MODAL_ALPHA_GAIN = 1.6f
