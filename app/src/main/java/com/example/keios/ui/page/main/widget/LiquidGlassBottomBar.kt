package com.example.keios.ui.page.main.widget

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
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
    val selectedScaleProvider = LocalLiquidGlassBottomBarTabScale.current
    val selectionProgress = liquidGlassBottomBarItemSelectionProgress(tabIndex)
    val interactive = LocalLiquidGlassBottomBarItemInteractive.current

    val targetScale = when {
        pressed -> 0.965f
        selected || selectionProgress > 0f -> lerp(1f, selectedScaleProvider(), selectionProgress)
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 580f),
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
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    isLiquidEffectEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isInLightTheme = !isSystemInDarkTheme()
    val animationScope = rememberCoroutineScope()

    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val clampedSelectedIndex = selectedIndex().fastCoerceIn(0, safeTabsCount - 1)
    val currentOnSelected by rememberUpdatedState(onSelected)

    val horizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }

    val palette = rememberLiquidBottomBarPalette(
        isLiquidEffectEnabled = isLiquidEffectEnabled,
        isInLightTheme = isInLightTheme,
        primary = MiuixTheme.colorScheme.primary,
        onSurface = MiuixTheme.colorScheme.onSurface,
        surfaceContainer = MiuixTheme.colorScheme.surfaceContainer,
        outline = MiuixTheme.colorScheme.outline
    )

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val trackWidthPx by remember(totalWidthPx, horizontalPaddingPx) {
        derivedStateOf { (totalWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(0f) }
    }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density, totalWidthPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }
    val dampedDragAnimation = remember(animationScope, safeTabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = clampedSelectedIndex.toFloat(),
            valueRange = 0f..(safeTabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position ->
                val animation = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx <= 0f || totalWidthPx <= 0f) return@DampedDragAnimation false
                val indicatorOffset = animation.value * tabWidthPx
                val globalTouchX = if (isLtr) {
                    horizontalPaddingPx + indicatorOffset + position.x
                } else {
                    totalWidthPx - horizontalPaddingPx - tabWidthPx - indicatorOffset + position.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, safeTabsCount - 1)
                animateToValue(targetIndex.toFloat())
                currentOnSelected(targetIndex)
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f) {
                    val deltaProgress = dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f
                    updateValue(
                        (targetValue + deltaProgress).fastCoerceIn(0f, (safeTabsCount - 1).toFloat())
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
        snapshotFlow { selectedIndex().fastCoerceIn(0, safeTabsCount - 1) }
            .collectLatest { index ->
                if (abs(dampedDragAnimation.targetValue - index.toFloat()) > 0.001f) {
                    dampedDragAnimation.updateValue(index.toFloat())
                }
            }
    }

    val pressProgress = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f
    val selectionProgressProvider: (Int) -> Float = remember(dampedDragAnimation) {
        { tabIndex ->
            (1f - abs(dampedDragAnimation.value - tabIndex)).fastCoerceIn(0f, 1f)
        }
    }

    val interactiveHighlight = if (isLiquidEffectEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember(animationScope, tabWidthPx, trackWidthPx, panelOffset, isLtr) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        horizontalPaddingPx + indicatorCenterX(
                            isLtr = isLtr,
                            activeValue = dampedDragAnimation.value,
                            tabWidthPx = tabWidthPx,
                            trackWidthPx = trackWidthPx,
                            panelOffset = panelOffset
                        ),
                        size.height / 2f
                    )
                },
                highlightColor = Color.White,
                highlightStrength = if (isInLightTheme) 0.56f else 0.90f,
                highlightRadiusScale = if (isInLightTheme) 0.90f else 1.08f
            )
        }
    } else {
        null
    }

    CompositionLocalProvider(
        LocalLiquidGlassBottomBarTabScale provides {
            if (isLiquidEffectEnabled) lerp(1.01f, 1.18f, pressProgress) else 1.01f
        },
        LocalLiquidGlassBottomBarSelectionProgress provides selectionProgressProvider,
        LocalLiquidGlassBottomBarContentColor provides { tabIndex ->
            val progress = selectionProgressProvider(tabIndex)
            lerpColor(
                palette.inactiveContentColor,
                palette.activeContentColor,
                progress.fastCoerceIn(0f, 1f)
            )
        },
        LocalLiquidGlassBottomBarItemInteractive provides true
    ) {
        Box(
            modifier = modifier.width(IntrinsicSize.Min),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = (totalWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(0f)
                        tabWidthPx = (contentWidthPx / safeTabsCount).coerceAtLeast(0f)
                    }
                    .height(AppChromeTokens.floatingBottomBarOuterHeight)
                    .clip(ContinuousCapsule)
                    .border(1.dp, palette.baseBorderColor, ContinuousCapsule)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            if (isLiquidEffectEnabled) {
                                vibrancy()
                                blur(UiPerformanceBudget.backdropBlur.toPx())
                                lens(UiPerformanceBudget.backdropLens.toPx(), UiPerformanceBudget.backdropLens.toPx())
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isLiquidEffectEnabled) 1f else 0f)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(if (isInLightTheme) 0.10f else 0.22f)
                            )
                        },
                        onDrawSurface = { drawRect(palette.baseFillColor) }
                    )
                    .then(if (interactiveHighlight != null) interactiveHighlight.modifier else Modifier),
                contentAlignment = Alignment.CenterStart
            ) {
                if (tabWidthPx > 0f) {
                    val indicatorScaleX = dragAnimationScaleX(dampedDragAnimation)
                    val indicatorScaleY = dragAnimationScaleY(dampedDragAnimation)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .graphicsLayer {
                                translationX = indicatorTranslationX(
                                    isLtr = isLtr,
                                    activeValue = dampedDragAnimation.value,
                                    tabWidthPx = tabWidthPx,
                                    panelOffset = panelOffset
                                )
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { ContinuousCapsule },
                                effects = {
                                    if (isLiquidEffectEnabled) {
                                        val progress = dampedDragAnimation.pressProgress
                                        lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, true)
                                    }
                                },
                                highlight = {
                                    Highlight.Default.copy(
                                        alpha = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f
                                    )
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
                                        scaleX = indicatorScaleX
                                        scaleY = indicatorScaleY
                                    }
                                },
                                onDrawSurface = {
                                    drawRect(palette.indicatorFillColor)
                                    drawRect(color = palette.indicatorSurfaceOverlay(dampedDragAnimation.pressProgress))
                                }
                            )
                            .height(AppChromeTokens.floatingBottomBarInnerHeight)
                            .width(with(density) { tabWidthPx.toDp() })
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding)
                        .graphicsLayer { translationX = panelOffset },
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )

                if (tabWidthPx > 0f) {
                    val indicatorScaleX = dragAnimationScaleX(dampedDragAnimation)
                    val indicatorScaleY = dragAnimationScaleY(dampedDragAnimation)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .graphicsLayer {
                                translationX = indicatorTranslationX(
                                    isLtr = isLtr,
                                    activeValue = dampedDragAnimation.value,
                                    tabWidthPx = tabWidthPx,
                                    panelOffset = panelOffset
                                )
                                this.scaleX = indicatorScaleX
                                this.scaleY = indicatorScaleY
                            }
                            .clip(ContinuousCapsule)
                            .then(if (interactiveHighlight != null) interactiveHighlight.gestureModifier else Modifier)
                            .then(dampedDragAnimation.modifier)
                            .clearAndSetSemantics {}
                            .height(AppChromeTokens.floatingBottomBarInnerHeight)
                            .width(with(density) { tabWidthPx.toDp() })
                    )
                }
            }
        }
    }
}

private fun indicatorTranslationX(
    isLtr: Boolean,
    activeValue: Float,
    tabWidthPx: Float,
    panelOffset: Float
): Float {
    val progressOffset = activeValue * tabWidthPx
    return if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
}

private fun indicatorCenterX(
    isLtr: Boolean,
    activeValue: Float,
    tabWidthPx: Float,
    trackWidthPx: Float,
    panelOffset: Float
): Float {
    return if (isLtr) {
        (activeValue + 0.5f) * tabWidthPx + panelOffset
    } else {
        trackWidthPx - (activeValue + 0.5f) * tabWidthPx + panelOffset
    }
}

private fun dragAnimationScaleX(animation: DampedDragAnimation): Float {
    return animation.scaleX /
        (1f - (animation.velocity / 10f * 0.75f).fastCoerceIn(-0.2f, 0.2f))
}

private fun dragAnimationScaleY(animation: DampedDragAnimation): Float {
    return animation.scaleY *
        (1f - (animation.velocity / 10f * 0.25f).fastCoerceIn(-0.2f, 0.2f))
}

@Composable
private fun rememberLiquidBottomBarPalette(
    isLiquidEffectEnabled: Boolean,
    isInLightTheme: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceContainer: Color,
    outline: Color
): LiquidBottomBarPalette = remember(
    isLiquidEffectEnabled,
    isInLightTheme,
    primary,
    onSurface,
    surfaceContainer,
    outline
) {
    if (!isLiquidEffectEnabled) {
        return@remember LiquidBottomBarPalette(
            baseFillColor = surfaceContainer,
            baseBorderColor = outline.copy(alpha = 0.22f),
            indicatorFillColor = primary.copy(alpha = 0.16f),
            indicatorBorderColor = primary.copy(alpha = 0.32f),
            inactiveContentColor = onSurface,
            activeContentColor = primary,
            indicatorRestOverlayColor = Color.Black.copy(alpha = 0.08f),
            indicatorPressedOverlayColor = Color.Black.copy(alpha = 0.02f)
        )
    }

    if (isInLightTheme) {
        return@remember LiquidBottomBarPalette(
            baseFillColor = surfaceContainer.copy(alpha = 0.34f),
            baseBorderColor = Color.White.copy(alpha = 0.54f),
            indicatorFillColor = Color.White.copy(alpha = 0.18f),
            indicatorBorderColor = Color.White.copy(alpha = 0.72f),
            inactiveContentColor = onSurface.copy(alpha = 0.88f),
            activeContentColor = primary,
            indicatorRestOverlayColor = Color.Black.copy(alpha = 0.10f),
            indicatorPressedOverlayColor = Color.Black.copy(alpha = 0.03f)
        )
    }

    return@remember LiquidBottomBarPalette(
        baseFillColor = surfaceContainer.copy(alpha = 0.20f),
        baseBorderColor = Color.White.copy(alpha = 0.24f),
        indicatorFillColor = Color.White.copy(alpha = 0.08f),
        indicatorBorderColor = Color.White.copy(alpha = 0.36f),
        inactiveContentColor = onSurface.copy(alpha = 0.84f),
        activeContentColor = primary.copy(alpha = 0.98f),
        indicatorRestOverlayColor = Color.White.copy(alpha = 0.08f),
        indicatorPressedOverlayColor = Color.Black.copy(alpha = 0.03f)
    )
}

@Stable
private class LiquidBottomBarPalette(
    val baseFillColor: Color,
    val baseBorderColor: Color,
    val indicatorFillColor: Color,
    val indicatorBorderColor: Color,
    val inactiveContentColor: Color,
    val activeContentColor: Color,
    val indicatorRestOverlayColor: Color,
    val indicatorPressedOverlayColor: Color
) {
    fun indicatorSurfaceOverlay(pressProgress: Float): Color {
        return lerpColor(indicatorRestOverlayColor, indicatorPressedOverlayColor, pressProgress)
    }
}
