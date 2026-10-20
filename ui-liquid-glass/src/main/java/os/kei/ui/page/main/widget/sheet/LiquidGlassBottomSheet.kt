@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import os.kei.ui.page.main.widget.glass.LiquidOverlayPortal
import os.kei.ui.page.main.widget.glass.LocalLiquidShaderPooling
import os.kei.ui.page.main.widget.glass.LocalLiquidPresentationOwnerActive
import os.kei.ui.page.main.widget.glass.presentationGlassBlur
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import kotlin.math.roundToInt

private val LiquidSheetCornerRadius = 28.dp
private val LiquidSheetCompactMaxWidth = 480.dp
private val LiquidSheetMediumMaxWidth = 560.dp
private val LiquidSheetInsideMargin = DpSize(width = 20.dp, height = 0.dp)
private val LiquidSheetOutsideMargin = DpSize(width = 0.dp, height = 0.dp)
private val LiquidSheetEstimatedChromeHeight = 72.dp

private const val DETENT_ONE_THIRD = 1f / 3f
private const val DETENT_HALF = 0.50f
private const val DETENT_THREE_QUARTER = 0.75f
private const val DETENT_FULL = 1.0f
private const val DETENT_SOLIDNESS_START = 0.58f
private const val LIQUID_SHEET_VISUAL_FRACTION_STEPS = 48f
private val LiquidSheetDetentDragThreshold = 72.dp

enum class LiquidSheetInitialDetent(
    internal val fraction: Float,
) {
    OneThird(DETENT_ONE_THIRD),
    Half(DETENT_HALF),
    ThreeQuarter(DETENT_THREE_QUARTER),
    Full(DETENT_FULL),
}

enum class LiquidSheetSurfaceTone {
    Default,
    Readable,
}

val LocalLiquidSheetContentOverflowReporter =
    compositionLocalOf<(Boolean) -> Unit> { {} }
val LocalLiquidSheetManagedScrollableContentReporter =
    compositionLocalOf<(Boolean) -> Unit> { {} }
val LocalLiquidSheetVisibleHeightPx =
    compositionLocalOf<(() -> Int)?> { null }

/**
 * Liquid glass bottom sheet.
 *
 * The sheet opens at a content-adaptive height. Dragging the grabber resizes it while the bottom edge
 * stays anchored, so content behind can be revealed temporarily; tapping the grabber cycles the
 * detents. Long sheets stop at one third of the available window before a further downward drag
 * requests dismissal.
 *
 * Renders through [LiquidOverlayPortal] into the activity window rather than a Dialog window, which
 * is what lets the glass sample real content — see [os.kei.ui.page.main.widget.glass.LiquidOverlayHostState].
 */
@Composable
fun LiquidGlassBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color? = null,
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = LiquidSheetCornerRadius,
    sheetMaxWidth: Dp = BottomSheetDefaults.maxWidth,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = LiquidSheetOutsideMargin,
    insideMargin: DpSize = LiquidSheetInsideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color? = null,
    allowDismiss: Boolean = true,
    onBlockedDismissRequest: (() -> Unit)? = null,
    enableNestedScroll: Boolean = true,
    initialDetent: LiquidSheetInitialDetent = LiquidSheetInitialDetent.ThreeQuarter,
    surfaceTone: LiquidSheetSurfaceTone = LiquidSheetSurfaceTone.Default,
    preferExportedBackdrop: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Stays mounted through the exit animation, so the sheet is never torn out from under a running
    // dismissal — the previous sheet's flinch was exactly that kind of cut.
    var keepMounted by remember { mutableStateOf(false) }
    if (!LocalLiquidPresentationOwnerActive.current) {
        SideEffect { keepMounted = false }
        return
    }
    if (!show && !keepMounted) return

    val isDark = isAppInDarkTheme()
    val density = LocalDensity.current
    // The scrim is what makes the app recede behind the sheet, so it takes the same ceiling the sheet
    // itself does — a soft 6dp smear still leaves headings and cards readable through it.
    val scrimBlurRadius = presentationGlassBlur()

    var managedScrollableContent by remember(show) { mutableStateOf(false) }
    var scrollableContentOverflowsOpeningDetent by remember(show, initialDetent) { mutableStateOf(false) }
    var plainContentExceedsOpeningDetent by remember(show, initialDetent) { mutableStateOf(false) }

    val adaptedInitialDetent =
        liquidSheetAdaptedInitialDetent(
            initialDetent = initialDetent,
            contentOverflowsOpeningDetent = plainContentExceedsOpeningDetent,
        )
    val targetFraction = adaptedInitialDetent.fraction
    val solidness = liquidSheetSolidness(liquidSheetQuantizedVisualDetentFraction(targetFraction))
    val minHeight = liquidSheetMinHeight(targetFraction)
    val minimumFloatingHeight = liquidSheetMinHeight(DETENT_ONE_THIRD)
    val openingMinHeight = liquidSheetMinHeight(initialDetent.fraction)
    val contentDetentHeight = (minHeight - LiquidSheetEstimatedChromeHeight).coerceAtLeast(0.dp)
    val openingContentMinHeight = (openingMinHeight - LiquidSheetEstimatedChromeHeight).coerceAtLeast(0.dp)
    val resolvedSheetMaxWidth = liquidSheetMaxWidth(sheetMaxWidth)
    val safeTopInset = liquidSheetSafeTopInset()
    val animatedContentDetentHeight =
        animateDpAsState(
            targetValue = contentDetentHeight,
            label = "liquid_sheet_detent_content_height",
        )
    val shouldBoundManagedScrollableContent =
        managedScrollableContent && scrollableContentOverflowsOpeningDetent

    LiquidSheetPortal {
        LiquidSheetPresentation(
            show = show,
            onMountedChanged = { mounted -> keepMounted = mounted },
            modifier = modifier,
            title = title,
            startAction = startAction,
            endAction = endAction,
            solidness = solidness,
            surfaceTone = surfaceTone,
            explicitBackgroundColor = backgroundColor,
            fallbackSurfaceColor =
                liquidSheetSurfaceColor(
                    isDark = isDark,
                    solidness = solidness,
                    surfaceTone = surfaceTone,
                ),
            enableDim = enableWindowDim,
            scrimBlurRadius = scrimBlurRadius,
            cornerRadius = cornerRadius,
            sheetMaxWidth = resolvedSheetMaxWidth,
            outsideMargin = outsideMargin,
            insideMargin = insideMargin,
            applyImePadding = defaultWindowInsetsPadding,
            dragHandleColor =
                dragHandleColor ?: liquidSheetDragHandleColor(isDark = isDark, solidness = solidness),
            allowDismiss = allowDismiss,
            enableNestedScroll = enableNestedScroll,
            minimumHeight = minimumFloatingHeight,
            topInset = safeTopInset,
            dismissDragThreshold = LiquidSheetDetentDragThreshold,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            onBlockedDismissRequest = onBlockedDismissRequest,
            preferExportedBackdrop = preferExportedBackdrop,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Short sheets keep their natural height. Overflowing opening content gets a
                    // bounded viewport so the sheet can scroll internally without forcing blank
                    // space into compact sheets. After the user resizes the sheet, this viewport
                    // follows the sheet height from the layout phase.
                    .then(
                        if (shouldBoundManagedScrollableContent) {
                            val visibleHeightPxProvider = LocalLiquidSheetVisibleHeightPx.current
                            val estimatedChromeHeightPx =
                                with(density) { LiquidSheetEstimatedChromeHeight.toPx().roundToInt() }
                            Modifier.liquidSheetContentMaxHeightPx {
                                val openingContentHeightPx =
                                    with(density) {
                                        animatedContentDetentHeight.value.toPx().roundToInt()
                                    }
                                val resizedContentHeightPx =
                                    visibleHeightPxProvider
                                        ?.invoke()
                                        ?.minus(estimatedChromeHeightPx)
                                        ?.coerceAtLeast(0)
                                        ?: 0
                                liquidSheetManagedContentMaxHeightPx(
                                    openingContentHeightPx = openingContentHeightPx,
                                    resizedContentHeightPx = resizedContentHeightPx,
                                )
                            }
                        } else {
                            Modifier
                        },
                    ).onSizeChanged { size ->
                        if (initialDetent != LiquidSheetInitialDetent.ThreeQuarter) return@onSizeChanged
                        val openingContentMinHeightPx =
                            with(density) { openingContentMinHeight.toPx() }
                        if (size.height > openingContentMinHeightPx + 1f) {
                            if (managedScrollableContent) {
                                scrollableContentOverflowsOpeningDetent = true
                            } else {
                                plainContentExceedsOpeningDetent = true
                            }
                        }
                    },
            ) {
                CompositionLocalProvider(
                    LocalLiquidSheetContentOverflowReporter provides { overflows ->
                        if (overflows) scrollableContentOverflowsOpeningDetent = true
                    },
                    LocalLiquidSheetManagedScrollableContentReporter provides { managed ->
                        managedScrollableContent = managed
                        if (managed) plainContentExceedsOpeningDetent = false
                    },
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun LiquidSheetPortal(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLiquidShaderPooling provides true) {
        LiquidOverlayPortal(content = content)
    }
}

private fun Modifier.liquidSheetContentMaxHeightPx(maxHeightPx: () -> Int): Modifier =
    layout { measurable, constraints ->
        val resolvedMaxHeight = maxHeightPx().coerceIn(0, constraints.maxHeight)
        val placeable =
            measurable.measure(
                constraints.copy(
                    minHeight = constraints.minHeight.coerceAtMost(resolvedMaxHeight),
                    maxHeight = resolvedMaxHeight,
                ),
            )
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

internal fun liquidSheetManagedContentMaxHeightPx(
    openingContentHeightPx: Int,
    resizedContentHeightPx: Int,
): Int =
    if (resizedContentHeightPx > 0) {
        resizedContentHeightPx
    } else {
        openingContentHeightPx
    }

@Composable
internal fun liquidSheetSafeTopInset(): Dp {
    val statusBars = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionBar = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
    val displayCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    return remember(statusBars, captionBar, displayCutout) {
        maxOf(statusBars, captionBar, displayCutout)
    }
}

@Composable
private fun liquidSheetMinHeight(fraction: Float): Dp {
    val windowHeight = LocalWindowInfo.current.containerDpSize.height
    val availableHeight = (windowHeight - liquidSheetSafeTopInset()).coerceAtLeast(0.dp)
    return availableHeight * fraction.coerceIn(DETENT_ONE_THIRD, DETENT_FULL)
}

@Composable
private fun liquidSheetMaxWidth(requestedMaxWidth: Dp): Dp {
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val adaptiveMaxWidth =
        when {
            windowWidth >= 840.dp -> BottomSheetDefaults.maxWidth
            windowWidth >= 600.dp -> LiquidSheetMediumMaxWidth
            else -> LiquidSheetCompactMaxWidth
        }
    return minOf(requestedMaxWidth, adaptiveMaxWidth)
}

/**
 * The opaque fallback fill, used when glass is off or the caller pinned a colour.
 *
 * Kept deliberately high: without a backdrop this fill is the *only* thing separating sheet text from
 * whatever is behind it.
 */
internal fun liquidSheetSurfaceColor(
    isDark: Boolean,
    solidness: Float,
    surfaceTone: LiquidSheetSurfaceTone = LiquidSheetSurfaceTone.Default,
): Color {
    val alpha =
        when (surfaceTone) {
            LiquidSheetSurfaceTone.Default ->
                if (isDark) lerp(0.90f, 0.99f, solidness) else lerp(0.87f, 0.99f, solidness)

            LiquidSheetSurfaceTone.Readable ->
                if (isDark) lerp(0.94f, 0.99f, solidness) else lerp(0.93f, 0.99f, solidness)
        }
    return if (isDark) {
        Color(0xFF141420).copy(alpha = alpha)
    } else {
        Color(0xFFF8F9FC).copy(alpha = alpha)
    }
}

/**
 * The fill painted on top of the blurred, refracted sample.
 *
 * Much lighter than [liquidSheetSurfaceColor] because it is doing a different job: the blur already
 * destroys the detail underneath, so this only has to set the tint and lift contrast. The floors here
 * are higher than the pre-rewrite values, which were tuned against a backdrop that never drew.
 */
internal fun liquidSheetGlassSurfaceColor(
    isDark: Boolean,
    solidness: Float,
    surfaceTone: LiquidSheetSurfaceTone = LiquidSheetSurfaceTone.Default,
): Color {
    val alpha =
        when (surfaceTone) {
            LiquidSheetSurfaceTone.Default ->
                if (isDark) lerp(0.58f, 0.76f, solidness) else lerp(0.52f, 0.72f, solidness)

            LiquidSheetSurfaceTone.Readable ->
                if (isDark) lerp(0.76f, 0.88f, solidness) else lerp(0.74f, 0.86f, solidness)
        }
    return if (isDark) {
        Color(0xFF141420).copy(alpha = alpha)
    } else {
        Color(0xFFF8F9FC).copy(alpha = alpha)
    }
}

internal fun liquidSheetDragHandleColor(
    isDark: Boolean,
    solidness: Float,
): Color =
    if (isDark) {
        Color.White.copy(alpha = lerp(0.38f, 0.28f, solidness))
    } else {
        Color.Black.copy(alpha = lerp(0.28f, 0.20f, solidness))
    }

internal fun liquidSheetSolidness(detentFraction: Float): Float {
    val linear =
        (
            (detentFraction - DETENT_SOLIDNESS_START) /
                (DETENT_FULL - DETENT_SOLIDNESS_START)
        ).coerceIn(0f, 1f)
    return linear * linear * (3f - 2f * linear)
}

internal fun liquidSheetQuantizedVisualDetentFraction(detentFraction: Float): Float {
    val steps = LIQUID_SHEET_VISUAL_FRACTION_STEPS
    return ((detentFraction.coerceIn(0f, 1f) * steps).roundToInt() / steps)
        .coerceIn(0f, 1f)
}

fun liquidSheetAdaptedInitialDetent(
    initialDetent: LiquidSheetInitialDetent,
    contentOverflowsOpeningDetent: Boolean,
): LiquidSheetInitialDetent =
    if (
        initialDetent == LiquidSheetInitialDetent.ThreeQuarter &&
        contentOverflowsOpeningDetent
    ) {
        LiquidSheetInitialDetent.Full
    } else {
        initialDetent
    }
