package com.example.keios.ui.page.main.widget

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.example.keios.ui.animation.DampedDragAnimation
import com.example.keios.ui.animation.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch

data class LiquidActionItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

@Composable
fun LiquidActionBarPopupAnchors(
    itemCount: Int,
    modifier: Modifier = Modifier,
    compactSingleItem: Boolean = false,
    content: @Composable (Int, IntRect?) -> Unit
) {
    if (itemCount <= 0) return
    val anchorBounds = remember(itemCount) {
        mutableStateListOf<IntRect?>().apply {
            repeat(itemCount) { add(null) }
        }
    }
    val minimumWidth = if (compactSingleItem && itemCount == 1) 50.dp else 156.dp
    val barWidth = maxOf(minimumWidth, (itemCount * 38).dp)
    Row(
        modifier = modifier
            .width(barWidth)
            .height(50.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(itemCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(37.dp)
                        .height(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInWindow()
                                anchorBounds[index] = IntRect(
                                    left = position.x.roundToInt(),
                                    top = position.y.roundToInt(),
                                    right = (position.x + coordinates.size.width).roundToInt(),
                                    bottom = (position.y + coordinates.size.height).roundToInt()
                                )
                            }
                    )
                    content(index, anchorBounds.getOrNull(index))
                }
            }
        }
    }
}

@Composable
private fun RowScope.LiquidActionItemSlot(
    item: LiquidActionItem,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null && item.enabled) {
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .then(clickModifier)
            .fillMaxHeight()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription,
            tint = if (item.enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}

@Composable
fun LiquidActionBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    items: List<LiquidActionItem>,
    isBlurEnabled: Boolean = true,
    compactSingleItem: Boolean = false,
    selectedIndex: Int = 0,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    if (items.isEmpty()) return
    val clampedSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)

    val isInLightTheme = !isSystemInDarkTheme()
    val accentColor = MiuixTheme.colorScheme.primary
    val containerColor = if (isBlurEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(0.4f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val iconColor = MiuixTheme.colorScheme.onSurface

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    3f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    var gestureActive by remember { mutableStateOf(false) }

    val dampedDragAnimation = remember(animationScope, items.size, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = clampedSelectedIndex.toFloat(),
            valueRange = 0f..(items.lastIndex).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 74f / 42f,
            canDrag = { true },
            onDragStarted = {
                gestureActive = true
                onInteractionChanged(true)
            },
            onDragStopped = {
                if (!gestureActive) return@DampedDragAnimation
                gestureActive = false
                onInteractionChanged(false)
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, items.lastIndex)
                animateToValue(targetIndex.toFloat())
                items.getOrNull(targetIndex)?.takeIf { it.enabled }?.onClick?.invoke()
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0) {
                    val raw = (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                        .fastCoerceIn(0f, items.lastIndex.toFloat())
                    updateValue(raw)
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        )
    }
    LaunchedEffect(clampedSelectedIndex, items.size) {
        val target = clampedSelectedIndex.toFloat()
        if (abs(dampedDragAnimation.targetValue - target) > 0.001f) {
            dampedDragAnimation.updateValue(target)
        }
    }

    val interactiveHighlight = if (isBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember(animationScope, tabWidthPx) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }
    } else {
        null
    }

    val minimumWidth = if (compactSingleItem && items.size == 1) 50.dp else 156.dp
    val barWidth = remember(items.size, compactSingleItem) { maxOf(minimumWidth, (items.size * 38).dp) }
    val interactionLockModifier = Modifier.pointerInput(onInteractionChanged) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onInteractionChanged(true)
            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                onInteractionChanged(false)
            }
        }
    }

    Box(
        modifier = modifier
            .width(barWidth)
            .then(interactionLockModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = contentWidthPx / items.size
                }
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        if (isBlurEnabled) {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(20f.dp.toPx(), 22f.dp.toPx())
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isBlurEnabled) 1f else 0f)
                    },
                    shadow = {
                        Shadow.Default.copy(
                            color = Color.Black.copy(if (isInLightTheme) 0.1f else 0.2f),
                        )
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(if (isBlurEnabled && interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                .height(50.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                LiquidActionItemSlot(
                    item = item,
                    tint = iconColor,
                    onClick = {
                        dampedDragAnimation.animateToValue(index.toFloat())
                        item.onClick()
                        animationScope.launch {
                            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                        }
                    }
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        if (isBlurEnabled) {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(20f.dp.toPx() * progress, 22f.dp.toPx() * progress)
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(if (isBlurEnabled && interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                .height(42.dp)
                .padding(horizontal = 4.dp)
                .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                LiquidActionItemSlot(item = item, tint = accentColor)
            }
        }

        if (tabWidthPx > 0f) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val contentWidth = totalWidthPx - with(density) { 8.dp.toPx() }
                        val singleTabWidth = contentWidth / items.size
                        val progressOffset = dampedDragAnimation.value * singleTabWidth
                        translationX = if (isLtr) {
                            progressOffset + panelOffset
                        } else {
                            -progressOffset + panelOffset
                        }
                    }
                    .then(if (isBlurEnabled && interactiveHighlight != null) interactiveHighlight.gestureModifier else Modifier)
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        effects = {
                            if (isBlurEnabled) {
                                val progress = dampedDragAnimation.pressProgress
                                lens(9f.dp.toPx() * progress, 12f.dp.toPx() * progress, true)
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                        },
                        shadow = { Shadow(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f) },
                        innerShadow = {
                            InnerShadow(
                                radius = 7f.dp * dampedDragAnimation.pressProgress,
                                alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
                            )
                        },
                        layerBlock = {
                            if (isBlurEnabled) {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                color = if (isInLightTheme) {
                                    Color.Black.copy(0.1f)
                                } else {
                                    Color.White.copy(0.1f)
                                },
                                alpha = progress * (1f - progress)
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .height(42.dp)
                    .width(with(density) { ((totalWidthPx - 8.dp.toPx()) / items.size).toDp() })
            )
        }
    }
}
