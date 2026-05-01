package os.kei.ui.page.main.widget.sheet

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import os.kei.ui.page.main.widget.dialog.AppWindowDialogHost
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class SnapshotPopupPlacement {
    Dropdown,
    ButtonEnd,
    ActionBarCenter
}

private val SnapshotPopupFractionAnimationSpec = ListPopupDefaults.FractionAnimationSpec
private val SnapshotPopupAlphaEnterAnimationSpec = ListPopupDefaults.AlphaEnterAnimationSpec
private val SnapshotPopupAlphaExitAnimationSpec = ListPopupDefaults.AlphaExitAnimationSpec

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
    maxWidth: Dp? = 280.dp,
    matchAnchorWidth: Boolean = false,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val explicitAnchorBounds = anchorBounds
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
    val normalizedAlignment = remember(alignment, layoutDirection) {
        alignment.normalizeForDropdown(layoutDirection)
    }
    val popupTransformOrigin = remember(normalizedAlignment, placement, opensDownward) {
        val pivotX = when (placement) {
            SnapshotPopupPlacement.Dropdown -> {
                if (normalizedAlignment == PopupPositionProvider.Align.End) 1f else 0f
            }
            SnapshotPopupPlacement.ButtonEnd -> 1f
            SnapshotPopupPlacement.ActionBarCenter -> 0.5f
        }
        val pivotY = if (opensDownward) 0f else 1f
        TransformOrigin(pivotFractionX = pivotX, pivotFractionY = pivotY)
    }
    val popupShowBelow = opensDownward
    val popupShowAbove = !opensDownward
    val fractionProgress = remember { Animatable(0f) }
    val alphaProgress = remember { Animatable(0f) }
    var wasVisible by remember { mutableStateOf(false) }
    var popupRender by remember { mutableStateOf(false) }
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

    LaunchedEffect(show, transitionAnimationsEnabled, onDismissFinished) {
        if (show) {
            wasVisible = true
            popupRender = true
            if (transitionAnimationsEnabled) {
                launch {
                    fractionProgress.animateTo(1f, SnapshotPopupFractionAnimationSpec)
                }
                alphaProgress.animateTo(1f, SnapshotPopupAlphaEnterAnimationSpec)
            } else {
                fractionProgress.snapTo(1f)
                alphaProgress.snapTo(1f)
            }
        } else {
            if (!popupRender && !wasVisible) return@LaunchedEffect
            if (transitionAnimationsEnabled) {
                launch {
                    fractionProgress.animateTo(0f, SnapshotPopupFractionAnimationSpec)
                }
                alphaProgress.animateTo(0f, SnapshotPopupAlphaExitAnimationSpec)
                fractionProgress.stop()
            } else {
                fractionProgress.snapTo(0f)
                alphaProgress.snapTo(0f)
            }
            popupRender = false
            if (wasVisible) {
                wasVisible = false
                onDismissFinished?.invoke()
            }
        }
    }

    if (popupRender) {
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
            val fraction = fractionProgress.value.coerceIn(0f, 1f)
            val scale = 0.15f + 0.85f * fraction
            Box(
                modifier = popupModifier
                    .defaultMinSize(minWidth = popupMinWidth)
                    .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                    .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaProgress.value.coerceIn(0f, 1f)
                        transformOrigin = popupTransformOrigin
                    }
                    .drawWithContent {
                        val progress = fractionProgress.value.coerceIn(0f, 1f)
                        val showMiddle = !popupShowBelow && !popupShowAbove
                        val clipStart = when {
                            popupShowAbove -> size.height * (1f - progress)
                            showMiddle -> size.height * (0.5f - 0.5f * progress)
                            else -> 0f
                        }
                        val clipBottom = when {
                            popupShowBelow -> size.height * progress
                            popupShowAbove -> size.height
                            showMiddle -> size.height * (0.5f + 0.5f * progress)
                            else -> size.height
                        }
                        if (clipBottom > clipStart) {
                            clipRect(
                                left = 0f,
                                top = clipStart,
                                right = size.width,
                                bottom = clipBottom
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
            ) {
                content()
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
    var wasShown by remember { mutableStateOf(false) }
    LaunchedEffect(show, onDismissFinished) {
        if (show) {
            wasShown = true
        } else if (wasShown) {
            wasShown = false
            onDismissFinished?.invoke()
        }
    }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    if (!transitionAnimationsEnabled) {
        if (!show) return
        AppWindowDialogHost(
            show = true,
            onDismissRequest = onDismissRequest,
            dismissible = allowDismiss
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (enableWindowDim) Color.Black.copy(alpha = 0.32f) else Color.Transparent
                    )
                    .then(
                        if (allowDismiss) {
                            Modifier.clickable(onClick = { onDismissRequest?.invoke() })
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .widthIn(max = sheetMaxWidth)
                        .padding(
                            horizontal = outsideMargin.width,
                            vertical = outsideMargin.height
                        )
                        .clip(
                            RoundedCornerShape(
                                topStart = cornerRadius,
                                topEnd = cornerRadius
                            )
                        )
                        .background(backgroundColor)
                        .padding(
                            horizontal = insideMargin.width,
                            vertical = insideMargin.height
                        )
                ) {
                    if (!title.isNullOrBlank() || startAction != null || endAction != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            startAction?.invoke()
                            Text(
                                text = title.orEmpty(),
                                modifier = Modifier.weight(1f),
                                color = Color.Unspecified,
                                maxLines = 1
                            )
                            endAction?.invoke()
                        }
                        Spacer(modifier = Modifier)
                    }
                    content()
                }
            }
        }
        return
    }

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
