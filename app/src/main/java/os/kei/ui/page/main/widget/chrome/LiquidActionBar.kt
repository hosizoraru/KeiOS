package os.kei.ui.page.main.widget.chrome

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import os.kei.ui.animation.DampedDragAnimation
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.rememberGlassReductionProgress
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
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
    val minimumWidth = if (compactSingleItem && itemCount == 1) {
        AppChromeTokens.liquidActionBarSingleWidth
    } else {
        AppChromeTokens.liquidActionBarMinWidth
    }
    val barWidth = maxOf(minimumWidth, (itemCount * AppChromeTokens.liquidActionBarItemStep.value).dp)
    Row(
        modifier = modifier
            .width(barWidth)
            .height(AppChromeTokens.liquidActionBarOuterHeight)
            .padding(horizontal = AppChromeTokens.liquidActionBarHorizontalPadding),
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
                        .width(AppChromeTokens.liquidActionBarItemStep - 2.dp)
                        .height(AppChromeTokens.liquidActionBarInnerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInWindow()
                                val measuredBounds = IntRect(
                                    left = position.x.roundToInt(),
                                    top = position.y.roundToInt(),
                                    right = (position.x + coordinates.size.width).roundToInt(),
                                    bottom = (position.y + coordinates.size.height).roundToInt()
                                )
                                if (anchorBounds[index] != measuredBounds) {
                                    anchorBounds[index] = measuredBounds
                                }
                            }
                    )
                    content(index, anchorBounds.getOrNull(index))
                }
            }
        }
    }
}

@Composable
internal fun RowScope.LiquidActionItemSlot(
    item: LiquidActionItem,
    tint: Color,
    iconScale: Float = 1f,
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
            .weight(1f)
            .graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
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
    layeredStyleEnabled: Boolean = true,
    reduceEffectsDuringPagerScroll: Boolean = false,
    compactSingleItem: Boolean = false,
    selectedIndex: Int = 0,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    if (items.isEmpty()) return
    val clampedSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)

    val isInLightTheme = !isSystemInDarkTheme()
    val accentColor = MiuixTheme.colorScheme.primary
    val palette = rememberLiquidActionBarPalette(
        layeredStyleEnabled = layeredStyleEnabled,
        isBlurEnabled = isBlurEnabled,
        isInLightTheme = isInLightTheme,
        primary = accentColor,
        onSurface = MiuixTheme.colorScheme.onSurface,
        surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    )

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val onInteractionChangedState = rememberUpdatedState(onInteractionChanged)
    val pressedScale = rememberLiquidActionBarPressedScale()

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
    val effectivePanelOffset = if (layeredStyleEnabled) panelOffset else 0f

    var gestureActive by remember { mutableStateOf(false) }
    var dragMoved by remember { mutableStateOf(false) }
    var dragTravelPx by remember { mutableFloatStateOf(0f) }
    val dragActivationThresholdPx = rememberLiquidActionBarDragActivationThresholdPx()

    val dampedDragAnimation = remember(animationScope, items.size, density, isLtr, layeredStyleEnabled) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = clampedSelectedIndex.toFloat(),
            valueRange = 0f..(items.lastIndex).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = pressedScale,
            gestureKey = items.size to isLtr,
            canDrag = { true },
            onDragStarted = {
                gestureActive = true
                dragMoved = false
                dragTravelPx = 0f
                onInteractionChangedState.value(true)
            },
            onDragStopped = {
                if (!gestureActive) return@DampedDragAnimation
                gestureActive = false
                onInteractionChangedState.value(false)
                if (!dragMoved) {
                    if (layeredStyleEnabled) {
                        val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, items.lastIndex)
                        items.getOrNull(targetIndex)?.takeIf { it.enabled }?.onClick?.invoke()
                    }
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                    return@DampedDragAnimation
                }
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, items.lastIndex)
                animateToValue(targetIndex.toFloat())
                items.getOrNull(targetIndex)?.takeIf { it.enabled }?.onClick?.invoke()
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0) {
                    dragTravelPx += abs(dragAmount.x)
                    if (!dragMoved && dragTravelPx >= dragActivationThresholdPx) {
                        dragMoved = true
                    }
                    val raw = (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                        .fastCoerceIn(0f, items.lastIndex.toFloat())
                    snapToValue(raw)
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

    val selectionProgressProvider =
        rememberLiquidActionBarSelectionProgressProvider(dampedDragAnimation, items.size)

    val interactionHighlightColor = if (layeredStyleEnabled || isInLightTheme) {
        Color.White
    } else {
        palette.selectionGlowColor
    }
    val interactionHighlightStrength = liquidActionBarInteractionHighlightStrength(
        layeredStyleEnabled = layeredStyleEnabled,
        isInLightTheme = isInLightTheme
    )
    val interactionHighlightRadiusScale = liquidActionBarInteractionHighlightRadiusScale(
        layeredStyleEnabled = layeredStyleEnabled,
        isInLightTheme = isInLightTheme
    )
    val interactionProgress by remember {
        derivedStateOf { dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f) }
    }
    val reducedEffectsProgress = rememberGlassReductionProgress(
        reduceEffectsDuringMotion = reduceEffectsDuringPagerScroll,
        label = "liquidActionBarGlassReduce"
    )
    val effectBlurScale = androidx.compose.ui.util.lerp(1f, 0.70f, reducedEffectsProgress)
    val effectLensScale = androidx.compose.ui.util.lerp(1f, 0.66f, reducedEffectsProgress)
    val interactionLensScale = androidx.compose.ui.util.lerp(1f, 0.84f, reducedEffectsProgress)
    val effectBlurDp = UiPerformanceBudget.backdropBlur * effectBlurScale
    val effectLensDp = UiPerformanceBudget.backdropLens * effectLensScale
    val interactiveHighlightEnabled = isBlurEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !reduceEffectsDuringPagerScroll &&
        (layeredStyleEnabled || isInLightTheme)
    val interactiveHighlight = if (interactiveHighlightEnabled) {
        remember(
            animationScope,
            dampedDragAnimation,
            tabWidthPx,
            isInLightTheme,
            layeredStyleEnabled,
            isLtr,
            interactionHighlightColor,
            interactionHighlightStrength,
            interactionHighlightRadiusScale
        ) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + effectivePanelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + effectivePanelOffset,
                        size.height / 2f
                    )
                },
                highlightColor = interactionHighlightColor,
                highlightStrength = interactionHighlightStrength,
                highlightRadiusScale = interactionHighlightRadiusScale
            )
        }
    } else {
        null
    }
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    val minimumWidth = if (compactSingleItem && items.size == 1) {
        AppChromeTokens.liquidActionBarSingleWidth
    } else {
        AppChromeTokens.liquidActionBarMinWidth
    }
    val barWidth = remember(items.size, compactSingleItem) {
        maxOf(minimumWidth, (items.size * AppChromeTokens.liquidActionBarItemStep.value).dp)
    }
    val singleBreakoutPadding = if (compactSingleItem && items.size == 1) {
        AppChromeTokens.liquidActionBarSingleBreakoutPadding
    } else {
        0.dp
    }
    val canvasWidth = barWidth + singleBreakoutPadding * 2
    val interactionLockModifier = rememberLiquidActionBarInteractionLockModifier(
        onInteractionChanged = onInteractionChangedState.value
    )

    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .width(canvasWidth)
            .height(AppChromeTokens.liquidActionBarOuterHeight)
            .then(interactionLockModifier),
        contentAlignment = Alignment.Center
    ) {
        val primaryRowModifier = Modifier
            .width(barWidth)
            .onGloballyPositioned { coords ->
                val measuredTotalWidthPx = coords.size.width.toFloat()
                if (abs(totalWidthPx - measuredTotalWidthPx) > 0.5f) {
                    totalWidthPx = measuredTotalWidthPx
                }
                val contentWidthPx = measuredTotalWidthPx - with(density) { 8.dp.toPx() }
                val measuredTabWidthPx = contentWidthPx / items.size
                if (abs(tabWidthPx - measuredTabWidthPx) > 0.5f) {
                    tabWidthPx = measuredTabWidthPx
                }
            }
            .graphicsLayer {
                translationX = effectivePanelOffset
                clip = false
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    if (isBlurEnabled) {
                        vibrancy()
                        blur(effectBlurDp.toPx())
                        lens(effectLensDp.toPx(), effectLensDp.toPx())
                    }
                },
                highlight = {
                    liquidActionBarBaseHighlight(
                        layeredStyleEnabled = layeredStyleEnabled,
                        isBlurEnabled = isBlurEnabled,
                        isInLightTheme = isInLightTheme
                    )
                },
                shadow = {
                    liquidActionBarBaseShadow(
                        layeredStyleEnabled = layeredStyleEnabled,
                        isInLightTheme = isInLightTheme
                    )
                },
                onDrawSurface = { drawRect(palette.baseFillColor) }
            )
            .border(
                width = 1.dp,
                color = palette.outlineColor,
                shape = ContinuousCapsule
            )
            .then(
                if (!layeredStyleEnabled) {
                    Modifier.liquidActionBarSelectionAura(
                        enabled = true,
                        animation = dampedDragAnimation,
                        tabWidthPx = tabWidthPx,
                        panelOffsetPx = effectivePanelOffset,
                        isLtr = isLtr,
                        glowColor = palette.selectionGlowColor,
                        coreColor = palette.selectionCoreColor,
                        interactionProgress = interactionProgress
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (isBlurEnabled && interactiveHighlight != null && interactionProgress > 0.001f) {
                    interactiveHighlight.modifier
                } else {
                    Modifier
                }
            )
            .then(
                if (!layeredStyleEnabled && isBlurEnabled && interactiveHighlight != null) {
                    interactiveHighlight.gestureModifier
                } else {
                    Modifier
                }
            )
            .then(if (!layeredStyleEnabled) dampedDragAnimation.modifier else Modifier)
            .height(AppChromeTokens.liquidActionBarOuterHeight)
            .padding(AppChromeTokens.liquidActionBarHorizontalPadding)

        Row(
            primaryRowModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selectionProgress = if (layeredStyleEnabled) 0f else selectionProgressProvider(index)
                LiquidActionItemSlot(
                    item = item,
                    tint = if (layeredStyleEnabled) {
                        palette.inactiveContentColor
                    } else {
                        lerp(palette.inactiveContentColor, palette.activeContentColor, selectionProgress)
                    },
                    iconScale = if (layeredStyleEnabled) {
                        1f
                    } else {
                        1f + (selectionProgress * 0.05f) + (dampedDragAnimation.pressProgress * selectionProgress * 0.03f)
                    },
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

        LiquidActionBarLayeredVisualOverlay(
            layeredStyleEnabled = layeredStyleEnabled,
            isBlurEnabled = isBlurEnabled,
            items = items,
            backdrop = backdrop,
            tabsBackdrop = tabsBackdrop,
            combinedBackdrop = combinedBackdrop,
            palette = palette,
            accentColor = accentColor,
            dampedDragAnimation = dampedDragAnimation,
            effectBlurDp = effectBlurDp,
            effectLensDp = effectLensDp,
            tabWidthPx = tabWidthPx,
            totalWidthPx = totalWidthPx,
            singleBreakoutPadding = singleBreakoutPadding,
            isInLightTheme = isInLightTheme,
            isLtr = isLtr,
            effectivePanelOffset = effectivePanelOffset,
            interactionLensScale = interactionLensScale,
            interactiveHighlight = interactiveHighlight
        )
    }
}
