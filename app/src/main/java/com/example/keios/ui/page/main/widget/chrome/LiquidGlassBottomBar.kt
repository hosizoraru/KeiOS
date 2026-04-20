package com.example.keios.ui.page.main.widget.chrome

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.example.keios.ui.animation.DampedDragAnimation
import com.example.keios.ui.animation.InteractiveHighlight
import com.example.keios.ui.page.main.widget.glass.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.appMotionFloatState
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.sign

val LocalLiquidGlassBottomBarTabScale = staticCompositionLocalOf { { 1f } }
private val LocalLiquidGlassBottomBarSelectionProgress = staticCompositionLocalOf<(Int) -> Float> { { 0f } }
private val LocalLiquidGlassBottomBarContentColor = staticCompositionLocalOf<(Int) -> Color> { { Color.Unspecified } }
private val LocalLiquidGlassBottomBarItemInteractive = staticCompositionLocalOf { true }

@Composable
fun liquidGlassBottomBarItemSelectionProgress(tabIndex: Int): Float {
    return LocalLiquidGlassBottomBarSelectionProgress.current(tabIndex)
}

@Composable
fun liquidGlassBottomBarItemContentColor(tabIndex: Int): Color {
    return LocalLiquidGlassBottomBarContentColor.current(tabIndex)
}

@Composable
fun RowScope.LiquidGlassBottomBarItem(
    selected: Boolean,
    tabIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selectedScale = LocalLiquidGlassBottomBarTabScale.current
    val selectionProgress = liquidGlassBottomBarItemSelectionProgress(tabIndex)
    val interactive = LocalLiquidGlassBottomBarItemInteractive.current

    val targetScale = when {
        pressed -> 0.965f
        selected || selectionProgress > 0f -> lerp(1f, selectedScale(), selectionProgress)
        else -> 1f
    }
    val scale by appMotionFloatState(
        targetValue = targetScale,
        durationMillis = 160,
        label = "liquid_bottom_bar_item_scale"
    )

    Column(
        modifier = modifier
            .clip(ContinuousCapsule)
            .then(
                if (interactive) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun LiquidGlassBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    reduceEffectsDuringPagerScroll: Boolean = false,
    isLiquidEffectEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isInLightTheme = !isSystemInDarkTheme()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val animationScope = rememberCoroutineScope()

    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val horizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding

    val palette = rememberLiquidBottomBarPalette(
        isLiquidEffectEnabled = isLiquidEffectEnabled,
        isInLightTheme = isInLightTheme,
        primary = MiuixTheme.colorScheme.primary,
        onSurface = MiuixTheme.colorScheme.onSurface,
        surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    )

    val tabsBackdrop = rememberLayerBackdrop()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    var currentIndex by remember(safeTabsCount) {
        mutableIntStateOf(selectedIndex.fastCoerceIn(0, safeTabsCount - 1))
    }

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }

    val dampedDragAnimation = remember(animationScope, safeTabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = currentIndex.toFloat(),
            valueRange = 0f..(safeTabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                val animation = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx <= 0f || totalWidthPx <= 0f) return@DampedDragAnimation false
                val paddingPx = with(density) { 4.dp.toPx() }
                val indicatorX = animation.value * tabWidthPx
                val globalTouchX = if (isLtr) {
                    paddingPx + indicatorX + offset.x
                } else {
                    totalWidthPx - paddingPx - tabWidthPx - indicatorX + offset.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, safeTabsCount - 1)
                currentIndex = targetIndex
                if (transitionAnimationsEnabled) {
                    animateToValue(targetIndex.toFloat())
                } else {
                    snapToValue(targetIndex.toFloat())
                }
                animationScope.launch {
                    if (transitionAnimationsEnabled) {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    } else {
                        offsetAnimation.snapTo(0f)
                    }
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f) {
                    val progressDelta = dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f
                    snapToValue(
                        (value + progressDelta).fastCoerceIn(0f, (safeTabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        )
    }
    holder.instance = dampedDragAnimation

    LaunchedEffect(selectedIndex, safeTabsCount) {
        currentIndex = selectedIndex.fastCoerceIn(0, safeTabsCount - 1)
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                if (transitionAnimationsEnabled) {
                    dampedDragAnimation.animateToValue(index.toFloat())
                } else {
                    dampedDragAnimation.snapToValue(index.toFloat())
                }
                onSelected(index)
            }
    }

    val pressProgress = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f
    val effectBlurDp = if (reduceEffectsDuringPagerScroll) {
        UiPerformanceBudget.backdropBlur * 0.72f
    } else {
        UiPerformanceBudget.backdropBlur
    }
    val effectLensDp = if (reduceEffectsDuringPagerScroll) {
        UiPerformanceBudget.backdropLens * 0.70f
    } else {
        UiPerformanceBudget.backdropLens
    }
    val interactionLensScale = if (reduceEffectsDuringPagerScroll) 0.62f else 1f

    val selectionProgressProvider: (Int) -> Float = remember(dampedDragAnimation) {
        { tabIndex ->
            (1f - abs(dampedDragAnimation.value - tabIndex)).fastCoerceIn(0f, 1f)
        }
    }

    val interactiveHighlight = if (
        isLiquidEffectEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !reduceEffectsDuringPagerScroll
    ) {
        remember(animationScope, tabWidthPx) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                        size.height / 2f
                    )
                },
                highlightColor = Color.White,
                highlightStrength = if (isInLightTheme) 0.60f else 0.90f,
                highlightRadiusScale = if (isInLightTheme) 0.90f else 1.08f
            )
        }
    } else {
        null
    }
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    CompositionLocalProvider(
        LocalLiquidGlassBottomBarTabScale provides {
            if (isLiquidEffectEnabled) lerp(1f, 1.2f, pressProgress) else 1f
        },
        LocalLiquidGlassBottomBarSelectionProgress provides selectionProgressProvider,
        LocalLiquidGlassBottomBarContentColor provides { palette.inactiveContentColor },
        LocalLiquidGlassBottomBarItemInteractive provides true
    ) {
        Box(
            modifier = modifier.width(IntrinsicSize.Min),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        val measuredTotalWidthPx = coords.size.width.toFloat()
                        if (abs(totalWidthPx - measuredTotalWidthPx) > 0.5f) {
                            totalWidthPx = measuredTotalWidthPx
                        }
                        val contentWidthPx = measuredTotalWidthPx - with(density) {
                            (horizontalPadding * 2).toPx()
                        }
                        val measuredTabWidthPx = (contentWidthPx / safeTabsCount).coerceAtLeast(0f)
                        if (abs(tabWidthPx - measuredTabWidthPx) > 0.5f) {
                            tabWidthPx = measuredTabWidthPx
                        }
                    }
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            if (isLiquidEffectEnabled) {
                                vibrancy()
                                blur(effectBlurDp.toPx())
                                lens(effectLensDp.toPx(), effectLensDp.toPx())
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isLiquidEffectEnabled) 1f else 0f)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(if (isInLightTheme) 0.10f else 0.20f)
                            )
                        },
                        onDrawSurface = { drawRect(palette.baseFillColor) }
                    )
                    .then(if (interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                    .height(AppChromeTokens.floatingBottomBarOuterHeight)
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )

            CompositionLocalProvider(
                LocalLiquidGlassBottomBarContentColor provides { palette.activeContentColor },
                LocalLiquidGlassBottomBarItemInteractive provides false
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                if (isLiquidEffectEnabled) {
                                    val progress = dampedDragAnimation.pressProgress
                                    vibrancy()
                                    blur(effectBlurDp.toPx())
                                    lens(
                                        effectLensDp.toPx() * progress,
                                        effectLensDp.toPx() * progress
                                    )
                                }
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f)
                            },
                            onDrawSurface = { drawRect(palette.baseFillColor) }
                        )
                        .height(AppChromeTokens.floatingBottomBarInnerHeight)
                        .padding(horizontal = horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            if (tabWidthPx > 0f) {
                Box(
                    Modifier
                        .padding(horizontal = horizontalPadding)
                        .graphicsLayer {
                            val contentWidth = totalWidthPx - with(density) {
                                (horizontalPadding * 2).toPx()
                            }
                            val singleTabWidth = contentWidth / safeTabsCount
                            val progressOffset = dampedDragAnimation.value * singleTabWidth
                            translationX = if (isLtr) {
                                progressOffset + panelOffset
                            } else {
                                -progressOffset + panelOffset
                            }
                        }
                        .then(if (interactiveHighlight != null) interactiveHighlight.gestureModifier else Modifier)
                        .then(dampedDragAnimation.modifier)
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                if (isLiquidEffectEnabled && dampedDragAnimation.pressProgress > 0f) {
                                    val progress = dampedDragAnimation.pressProgress
                                    lens(
                                        10f.dp.toPx() * progress * interactionLensScale,
                                        14f.dp.toPx() * progress * interactionLensScale,
                                        true
                                    )
                                }
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f)
                            },
                            shadow = {
                                Shadow(alpha = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f)
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 8f.dp * dampedDragAnimation.pressProgress,
                                    alpha = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f
                                )
                            },
                            layerBlock = {
                                if (isLiquidEffectEnabled) {
                                    scaleX = dampedDragAnimation.scaleX
                                    scaleY = dampedDragAnimation.scaleY
                                    val velocity = dampedDragAnimation.velocity / 10f
                                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                                }
                            },
                            onDrawSurface = {
                                val progress = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f
                                drawRect(
                                    color = if (isInLightTheme) Color.Black.copy(0.10f) else Color.White.copy(0.10f),
                                    alpha = 1f - progress
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }
                        )
                        .clearAndSetSemantics {}
                        .height(AppChromeTokens.floatingBottomBarInnerHeight)
                        .width(with(density) {
                            ((totalWidthPx - (horizontalPadding * 2).toPx()) / safeTabsCount).toDp()
                        })
                )
            }
        }
    }
}

@Composable
private fun rememberLiquidBottomBarPalette(
    isLiquidEffectEnabled: Boolean,
    isInLightTheme: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceContainer: Color
): LiquidBottomBarPalette = remember(
    isLiquidEffectEnabled,
    isInLightTheme,
    primary,
    onSurface,
    surfaceContainer
) {
    if (!isLiquidEffectEnabled) {
        return@remember LiquidBottomBarPalette(
            baseFillColor = surfaceContainer,
            inactiveContentColor = onSurface,
            activeContentColor = primary
        )
    }

    if (isInLightTheme) {
        return@remember LiquidBottomBarPalette(
            baseFillColor = surfaceContainer.copy(alpha = 0.40f),
            inactiveContentColor = onSurface.copy(alpha = 0.88f),
            activeContentColor = primary
        )
    }

    return@remember LiquidBottomBarPalette(
        baseFillColor = surfaceContainer.copy(alpha = 0.20f),
        inactiveContentColor = onSurface.copy(alpha = 0.84f),
        activeContentColor = primary.copy(alpha = 0.98f)
    )
}

@Stable
private class LiquidBottomBarPalette(
    val baseFillColor: Color,
    val inactiveContentColor: Color,
    val activeContentColor: Color
)
