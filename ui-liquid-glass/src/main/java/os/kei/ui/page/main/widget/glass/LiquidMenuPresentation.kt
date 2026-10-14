@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotPopupSafeInsets
import os.kei.ui.page.main.widget.sheet.calculateSnapshotPopupLayout
import os.kei.ui.page.main.widget.sheet.calculateSnapshotPopupWindowBounds
import os.kei.ui.page.main.widget.sheet.liquidSheetOutsideDismiss
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

private const val LIQUID_MENU_ENTER_DAMPING = 0.82f
private const val LIQUID_MENU_ENTER_RESPONSE = 0.28f
private const val LIQUID_MENU_EXIT_DAMPING = 1f
private const val LIQUID_MENU_EXIT_RESPONSE = 0.18f

/** How far the panel travels toward its anchor before settling — a directional cue, not a slide. */
private const val LIQUID_MENU_TRANSLATION_DP = 8f

/**
 * Hosts an anchored menu panel inside the activity window.
 *
 * Menus used to render in a `Popup`, which is why none of them had glass: a `LayerBackdrop` resolves the
 * consumer↔producer offset through shared `LayoutCoordinates`, two windows have none, and
 * [LiquidBackdropWindowBoundary] blanks the scene backdrop accordingly. Every blur the menu asked for
 * drew nothing, so the panel fell back to a flat filled card. Hosting it in-window through
 * [LiquidOverlayPortal] is what makes the material possible at all.
 *
 * Placement is unchanged. [calculateSnapshotPopupLayout] and the miuix position provider already solved
 * anchoring, flipping and inset clamping, and they are pure functions with their own tests — so they are
 * reused verbatim, just fed the activity window's bounds instead of the popup window's. The one
 * difference worth knowing: a `Popup` with `clippingEnabled = false` could overflow the window, and this
 * cannot. Menus are clamped inside the safe insets either way, so nothing that was reachable stops being
 * reachable.
 *
 * The panel does not draw its own scrim. A pull-down menu is not modal in the way an alert is — iOS
 * leaves the content behind it undimmed — so the only thing covering the app is a transparent catcher
 * for the outside tap, which also claims drags so a swipe that starts outside the menu cannot reach the
 * pager underneath.
 */
@Composable
internal fun LiquidMenuPresentation(
    show: Boolean,
    anchorBounds: IntRect?,
    popupPositionProvider: PopupPositionProvider,
    alignment: PopupPositionProvider.Align,
    placement: SnapshotPopupPlacement,
    onDismissRequest: (() -> Unit)?,
    onDismissFinished: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    var keepMounted by remember { mutableStateOf(false) }
    if (!LocalLiquidPresentationOwnerActive.current) {
        SideEffect { keepMounted = false }
        return
    }
    if (!show && !keepMounted) return

    LiquidOverlayPortal {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val transitionsEnabled = LocalTransitionAnimationsEnabled.current
        val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
        val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)

        val progress = remember { mutableFloatStateOf(0f) }
        // Written during layout, read during draw. A plain holder rather than snapshot state because a
        // layout-phase write to state that the same phase reads is an invalidation loop; draw always
        // runs after layout, so this ordering is safe and costs no recomposition.
        val pivot = remember { LiquidMenuPivotHolder() }

        val safeInsets = liquidMenuSafeInsets()
        val translationOffsetPx = with(density) { LIQUID_MENU_TRANSLATION_DP.dp.toPx() }

        val reveal =
            remember(pivot) {
                LiquidMenuRevealState(
                    progressProvider = { progress.floatValue.coerceIn(0f, 1f) },
                    placementProvider = { pivot.value },
                )
            }

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
                            damping = LIQUID_MENU_ENTER_DAMPING,
                            response = LIQUID_MENU_ENTER_RESPONSE,
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
                            damping = LIQUID_MENU_EXIT_DAMPING,
                            response = LIQUID_MENU_EXIT_RESPONSE,
                        ),
                    )
                } else {
                    progress.floatValue = 0f
                }
                currentOnDismissFinished?.invoke()
                keepMounted = false
            }
        }

        val requestDismiss: () -> Unit = { currentOnDismissRequest?.invoke() }

        // Latched, and enabled for as long as the panel is *mounted* rather than only while `show` is
        // true. Both halves matter, and the popup this replaces got both right the hard way:
        //
        // - Enabled through the exit animation, because a back press while the panel is still visibly
        //   collapsing has to be consumed. Gating on `show` would let that press fall through and pop the
        //   route out from under a menu the user can still see.
        // - Latched, because one user action can produce a burst of presses before the caller's `show`
        //   flip has recomposed, and each of those would otherwise dispatch its own dismiss request.
        var dismissDispatched by remember(show) { mutableStateOf(false) }
        BackHandler(enabled = true) {
            if (show && !dismissDispatched) {
                dismissDispatched = true
                requestDismiss()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquidSheetOutsideDismiss(
                        allowDismiss = true,
                        onDismissRequest = requestDismiss,
                        onBlockedDismissRequest = null,
                    ),
            )
            CompositionLocalProvider(LocalLiquidMenuReveal provides reveal) {
                Box(
                    modifier = Modifier.layout { measurable, constraints ->
                        // Measure the panel first, then place it: the anchor solve needs the panel's
                        // real size, and doing it here rather than through onSizeChanged means there is
                        // no frame where the panel sits at the wrong offset.
                        val placeable =
                            measurable.measure(
                                constraints.copy(minWidth = 0, minHeight = 0),
                            )
                        val windowWidth = constraints.maxWidth
                        val windowHeight = constraints.maxHeight
                        val resolved =
                            resolveLiquidMenuPlacement(
                                anchorBounds = anchorBounds,
                                windowSize = IntSize(windowWidth, windowHeight),
                                safeInsets = safeInsets,
                                panelSize = IntSize(placeable.width, placeable.height),
                                popupPositionProvider = popupPositionProvider,
                                alignment = alignment,
                                placement = placement,
                                layoutDirection = layoutDirection,
                                density = density,
                                translationOffsetPx = translationOffsetPx,
                            )
                        pivot.value = resolved.pivot
                        layout(windowWidth, windowHeight) {
                            placeable.place(resolved.offset)
                        }
                    },
                ) {
                    content()
                }
            }
        }
    }
}

/** Mutable, non-snapshot: see the holder's use site for why. */
internal class LiquidMenuPivotHolder {
    var value: LiquidMenuPivot = LiquidMenuPivot()
}

internal class LiquidMenuPlacement(
    val offset: IntOffset,
    val pivot: LiquidMenuPivot,
)

/**
 * Resolves where the panel sits and where it grows from.
 *
 * Delegates to the same two pure functions the popup used, so anchoring behaviour is bit-identical —
 * this only converts their result into the pivot form [liquidMenuTransform] needs, and falls back to a
 * centred reveal when a caller opens a menu without an anchor.
 */
internal fun resolveLiquidMenuPlacement(
    anchorBounds: IntRect?,
    windowSize: IntSize,
    safeInsets: SnapshotPopupSafeInsets,
    panelSize: IntSize,
    popupPositionProvider: PopupPositionProvider,
    alignment: PopupPositionProvider.Align,
    placement: SnapshotPopupPlacement,
    layoutDirection: LayoutDirection,
    density: Density,
    translationOffsetPx: Float,
): LiquidMenuPlacement {
    val windowBounds = calculateSnapshotPopupWindowBounds(windowSize, safeInsets)
    if (anchorBounds == null) {
        // No anchor to grow from, so grow from the middle of wherever it lands.
        val centered =
            IntOffset(
                x = ((windowBounds.left + windowBounds.right - panelSize.width) / 2)
                    .coerceAtLeast(windowBounds.left),
                y = ((windowBounds.top + windowBounds.bottom - panelSize.height) / 2)
                    .coerceAtLeast(windowBounds.top),
            )
        return LiquidMenuPlacement(
            offset = centered,
            pivot = LiquidMenuPivot(pivotX = 0.5f, pivotY = 0.5f, directionalOffsetPx = 0f),
        )
    }
    val popupMargin = liquidMenuPopupMargin(popupPositionProvider, density, layoutDirection)
    val providerOffset =
        popupPositionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowBounds = windowBounds,
            layoutDirection = layoutDirection,
            popupContentSize = panelSize,
            popupMargin = popupMargin,
            alignment = alignment,
        )
    val layoutInfo =
        calculateSnapshotPopupLayout(
            anchorBounds = anchorBounds,
            windowBounds = windowBounds,
            layoutDirection = layoutDirection,
            popupContentSize = panelSize,
            popupMargin = popupMargin,
            alignment = alignment,
            placement = placement,
            providerOffset = providerOffset,
        )
    // A panel below its anchor rises into place; one above it settles downward.
    val directional =
        when {
            layoutInfo.showBelow -> -translationOffsetPx
            layoutInfo.showAbove -> translationOffsetPx
            else -> 0f
        }
    return LiquidMenuPlacement(
        offset = layoutInfo.offset,
        pivot =
            LiquidMenuPivot(
                pivotX = layoutInfo.transformOrigin.pivotFractionX,
                pivotY = layoutInfo.transformOrigin.pivotFractionY,
                directionalOffsetPx = directional,
            ),
    )
}

private fun liquidMenuPopupMargin(
    popupPositionProvider: PopupPositionProvider,
    density: Density,
    layoutDirection: LayoutDirection,
): IntRect {
    val margins = popupPositionProvider.getMargins()
    return with(density) {
        IntRect(
            left = margins.calculateLeftPadding(layoutDirection).roundToPx(),
            top = margins.calculateTopPadding().roundToPx(),
            right = margins.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = margins.calculateBottomPadding().roundToPx(),
        )
    }
}

@Composable
private fun liquidMenuSafeInsets(): SnapshotPopupSafeInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val displayCutout = WindowInsets.displayCutout
    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val captionBar = WindowInsets.captionBar
    return with(density) {
        SnapshotPopupSafeInsets(
            left =
                maxOf(
                    displayCutout.getLeft(this, layoutDirection),
                    navigationBars.getLeft(this, layoutDirection),
                    captionBar.getLeft(this, layoutDirection),
                ),
            top =
                maxOf(
                    displayCutout.getTop(this),
                    statusBars.getTop(this),
                    captionBar.getTop(this),
                ),
            right =
                maxOf(
                    displayCutout.getRight(this, layoutDirection),
                    navigationBars.getRight(this, layoutDirection),
                    captionBar.getRight(this, layoutDirection),
                ),
            bottom =
                maxOf(
                    displayCutout.getBottom(this),
                    navigationBars.getBottom(this),
                    captionBar.getBottom(this),
                ),
        )
    }
}
