package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider as ComposePopupPositionProvider
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.math.roundToInt

enum class SnapshotPopupPlacement {
    Dropdown,
    ButtonEnd,
    ActionBarCenter
}

@Composable
fun SnapshotWindowListPopup(
    show: Boolean,
    popupModifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    anchorBounds: IntRect? = null,
    placement: SnapshotPopupPlacement = SnapshotPopupPlacement.Dropdown,
    enableWindowDim: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxHeight: Dp? = null,
    minWidth: Dp = 0.dp,
    maxWidth: Dp? = AppInteractiveTokens.liquidDropdownMaxWidth,
    matchAnchorWidth: Boolean = false,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val explicitAnchorBounds = anchorBounds
    val popupAnimationOffsetPx = with(density) { AppInteractiveTokens.popupAnimationOffset.roundToPx() }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val anchorWidthDp = remember(explicitAnchorBounds, density) {
        explicitAnchorBounds?.let { with(density) { it.width.toDp() } } ?: 0.dp
    }
    val resolvedMinWidth = if (matchAnchorWidth) {
        maxOf(minWidth, anchorWidthDp)
    } else {
        minWidth
    }
    val popupMinWidth = maxWidth?.let { resolvedMinWidth.coerceAtMost(it) } ?: resolvedMinWidth
    val opensDownward = remember(explicitAnchorBounds, screenHeightPx) {
        explicitAnchorBounds?.let {
            val availableBelow = screenHeightPx - it.bottom
            val availableAbove = it.top
            availableBelow >= availableAbove
        } ?: true
    }
    var wasVisible by remember { mutableStateOf(false) }
    var popupRender by remember { mutableStateOf(show) }
    val popupVisibilityState = remember { MutableTransitionState(false) }
    val composePopupPositionProvider = remember(
        density,
        popupPositionProvider,
        alignment,
        placement,
        explicitAnchorBounds
    ) {
        object : ComposePopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val effectiveAnchorBounds = explicitAnchorBounds ?: anchorBounds
                val normalizedAlignment = alignment.normalizeForDropdown(layoutDirection)
                val popupMargin = popupPositionProvider.getMargins().toIntRect(density, layoutDirection)
                val windowBounds = IntRect(0, 0, windowSize.width, windowSize.height)
                val offsetY = calculateDropdownVerticalOffset(
                    anchorBounds = effectiveAnchorBounds,
                    windowBounds = windowBounds,
                    popupContentSize = popupContentSize,
                    popupMargin = popupMargin
                )
                val minX = windowBounds.left + popupMargin.left
                val maxX = (windowBounds.right - popupContentSize.width - popupMargin.right)
                    .coerceAtLeast(minX)
                val rawX = when (placement) {
                    SnapshotPopupPlacement.Dropdown -> {
                        if (normalizedAlignment == PopupPositionProvider.Align.End) {
                            effectiveAnchorBounds.right - popupContentSize.width - popupMargin.right
                        } else {
                            effectiveAnchorBounds.left + popupMargin.left
                        }
                    }

                    SnapshotPopupPlacement.ButtonEnd -> {
                        effectiveAnchorBounds.right - popupContentSize.width - popupMargin.right
                    }

                    SnapshotPopupPlacement.ActionBarCenter -> {
                        effectiveAnchorBounds.left + (effectiveAnchorBounds.width - popupContentSize.width) / 2
                    }
                }
                return IntOffset(rawX.coerceIn(minX, maxX), offsetY)
            }
        }
    }

    LaunchedEffect(show) {
        if (show) {
            wasVisible = true
            popupRender = true
            popupVisibilityState.targetState = true
        } else {
            popupVisibilityState.targetState = false
        }
    }

    LaunchedEffect(
        show,
        popupRender,
        popupVisibilityState.currentState,
        popupVisibilityState.targetState,
        onDismissFinished
    ) {
        if (!show &&
            popupRender &&
            !popupVisibilityState.currentState &&
            !popupVisibilityState.targetState
        ) {
            popupRender = false
            if (wasVisible) {
                wasVisible = false
                onDismissFinished?.invoke()
            }
        }
    }

    if (popupRender) {
        val popupEnter = if (transitionAnimationsEnabled) {
            fadeIn(
                animationSpec = tween(durationMillis = 140)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + slideInVertically(
                initialOffsetY = {
                    if (opensDownward) -popupAnimationOffsetPx else popupAnimationOffsetPx
                },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        } else {
            EnterTransition.None
        }
        val popupExit = if (transitionAnimationsEnabled) {
            fadeOut(
                animationSpec = tween(durationMillis = 100)
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
            ) + slideOutVertically(
                targetOffsetY = {
                    if (opensDownward) -popupAnimationOffsetPx else popupAnimationOffsetPx
                },
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
        } else {
            ExitTransition.None
        }
        Popup(
            popupPositionProvider = composePopupPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = false
            )
        ) {
            AnimatedVisibility(
                visibleState = popupVisibilityState,
                enter = popupEnter,
                exit = popupExit
            ) {
                Box(
                    modifier = popupModifier
                        .defaultMinSize(minWidth = popupMinWidth)
                        .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                        .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SnapshotWindowBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color = BottomSheetDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = BottomSheetDefaults.cornerRadius,
    sheetMaxWidth: Dp = BottomSheetDefaults.maxWidth,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = BottomSheetDefaults.outsideMargin,
    insideMargin: DpSize = DpSize(BottomSheetDefaults.insideMargin.width, 14.dp),
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color = BottomSheetDefaults.dragHandleColor(),
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    content: @Composable () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        cornerRadius = cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = dragHandleColor,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        content = content
    )
}

private fun PaddingValues.toIntRect(
    density: androidx.compose.ui.unit.Density,
    layoutDirection: LayoutDirection
): IntRect = with(density) {
    IntRect(
        left = calculateLeftPadding(layoutDirection).roundToPx(),
        top = calculateTopPadding().roundToPx(),
        right = calculateRightPadding(layoutDirection).roundToPx(),
        bottom = calculateBottomPadding().roundToPx()
    )
}

private fun PopupPositionProvider.Align.normalizeForDropdown(layoutDirection: LayoutDirection): PopupPositionProvider.Align {
    return when (this) {
        PopupPositionProvider.Align.End,
        PopupPositionProvider.Align.TopEnd,
        PopupPositionProvider.Align.BottomEnd -> {
            if (layoutDirection == LayoutDirection.Ltr) PopupPositionProvider.Align.End
            else PopupPositionProvider.Align.Start
        }

        PopupPositionProvider.Align.Start,
        PopupPositionProvider.Align.TopStart,
        PopupPositionProvider.Align.BottomStart -> {
            if (layoutDirection == LayoutDirection.Ltr) PopupPositionProvider.Align.Start
            else PopupPositionProvider.Align.End
        }
    }
}

private fun calculateDropdownVerticalOffset(
    anchorBounds: IntRect,
    windowBounds: IntRect,
    popupContentSize: IntSize,
    popupMargin: IntRect
): Int {
    val availableBelow = windowBounds.bottom - anchorBounds.bottom - popupMargin.bottom
    val availableAbove = anchorBounds.top - windowBounds.top - popupMargin.top
    val preferBelow = availableBelow >= popupContentSize.height || availableBelow >= availableAbove
    val rawY = if (preferBelow) {
        anchorBounds.bottom + popupMargin.bottom
    } else {
        anchorBounds.top - popupContentSize.height - popupMargin.top
    }
    val minY = (windowBounds.top + popupMargin.top)
        .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom)
    val maxY = windowBounds.bottom - popupContentSize.height - popupMargin.bottom
    return rawY.coerceIn(minY, maxY)
}

fun Modifier.capturePopupAnchor(onBoundsChange: (IntRect) -> Unit): Modifier {
    return this.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInWindow()
        onBoundsChange(
            IntRect(
                left = position.x.roundToInt(),
                top = position.y.roundToInt(),
                right = (position.x + coordinates.size.width).roundToInt(),
                bottom = (position.y + coordinates.size.height).roundToInt()
            )
        )
    }
}
