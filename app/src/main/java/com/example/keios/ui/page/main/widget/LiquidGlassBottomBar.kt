package com.example.keios.ui.page.main.widget

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.example.keios.ui.animation.DampedDragAnimation
import com.example.keios.ui.animation.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalLiquidGlassBottomBarTabScale = staticCompositionLocalOf { { 1.02f } }

@Composable
fun RowScope.LiquidGlassBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selectedScale = LocalLiquidGlassBottomBarTabScale.current
    val targetScale = when {
        pressed -> 0.96f
        selected -> selectedScale()
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 580f),
        label = "liquid_bottom_bar_item_scale"
    )
    Column(
        modifier = modifier
            .clip(AppBottomBarShapes.capsule)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
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
    tabsCount: Int,
    isLiquidEffectEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isInLightTheme = !isSystemInDarkTheme()
    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val clampedSelectedIndex = selectedIndex().coerceIn(0, safeTabsCount - 1)
    val currentSelectedIndex by rememberUpdatedState(clampedSelectedIndex)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val animationScope = rememberCoroutineScope()
    val primary = MiuixTheme.colorScheme.primary
    val horizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding

    val containerFallbackColor = when {
        !isLiquidEffectEnabled -> MiuixTheme.colorScheme.surfaceContainer
        else -> Color.Transparent
    }
    val containerBorderColor = when {
        !isLiquidEffectEnabled -> MiuixTheme.colorScheme.outline.copy(alpha = 0.22f)
        isInLightTheme -> Color.White.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    val indicatorFallbackColor = when {
        !isLiquidEffectEnabled -> primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val indicatorBorderColor = when {
        !isLiquidEffectEnabled -> primary.copy(alpha = 0.34f)
        isInLightTheme -> Color.White.copy(alpha = 0.34f)
        else -> Color.White.copy(alpha = 0.16f)
    }
    val indicatorShadowColor = if (isInLightTheme) {
        Color.Black.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.30f)
    }
    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var gestureActive by remember { mutableStateOf(false) }
    var dragMoved by remember { mutableStateOf(false) }
    var dragTravelPx by remember { mutableFloatStateOf(0f) }

    val dragActivationThresholdPx = remember(viewConfiguration.touchSlop) {
        (viewConfiguration.touchSlop * 1.15f).coerceAtLeast(8f)
    }
    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
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

    val dampedDragAnimation = remember(animationScope, safeTabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = clampedSelectedIndex.toFloat(),
            valueRange = 0f..(safeTabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { tabWidthPx > 0f && totalWidthPx > 0f },
            onDragStarted = {
                gestureActive = true
                dragMoved = false
                dragTravelPx = 0f
            },
            onDragStopped = {
                if (!gestureActive) return@DampedDragAnimation
                gestureActive = false
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, safeTabsCount - 1)
                if (dragMoved) {
                    animateToValue(targetIndex.toFloat())
                    if (targetIndex != currentSelectedIndex) {
                        currentOnSelected(targetIndex)
                    }
                } else {
                    updateValue(currentSelectedIndex.toFloat())
                }
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx == 0f) return@DampedDragAnimation
                dragTravelPx += abs(dragAmount.x)
                if (!dragMoved && dragTravelPx >= dragActivationThresholdPx) {
                    dragMoved = true
                }
                snapToValue(
                    (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                        .fastCoerceIn(0f, (safeTabsCount - 1).toFloat())
                )
                animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                }
            }
        )
    }

    LaunchedEffect(clampedSelectedIndex, safeTabsCount) {
        dampedDragAnimation.updateValue(clampedSelectedIndex.toFloat())
    }

    val pressProgress = if (isLiquidEffectEnabled) dampedDragAnimation.pressProgress else 0f
    val interactionHighlightColor = Color.White
    val interactionHighlightStrength = if (isInLightTheme) 0.88f else 1f
    val interactionHighlightRadiusScale = if (isInLightTheme) 0.90f else 1.12f
    val interactiveHighlight = if (isLiquidEffectEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember(animationScope, tabWidthPx, isLtr) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
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

    val containerScale by remember(density) {
        derivedStateOf {
            if (!isLiquidEffectEnabled || totalWidthPx <= 0f) {
                1f
            } else {
                lerp(
                    1f,
                    1f + with(density) { 12.dp.toPx() / totalWidthPx },
                    pressProgress
                )
            }
        }
    }

    val containerSpec = LiquidBackdropSpec(
        cornerRadiusPx = with(density) { (AppChromeTokens.floatingBottomBarOuterHeight / 2).toPx() },
        refractionHeightPx = with(density) { lerp(18f, 22f, pressProgress).dp.toPx() },
        refractionOffsetPx = with(density) { lerp(54f, 68f, pressProgress).dp.toPx() },
        blurRadiusPx = lerp(9.5f, 12.5f, pressProgress),
        dispersion = lerp(0.14f, 0.24f, pressProgress),
        tintAlpha = 0f,
        tintRed = 1f,
        tintGreen = 1f,
        tintBlue = 1f
    )
    val indicatorSpec = LiquidBackdropSpec(
        cornerRadiusPx = with(density) { (AppChromeTokens.floatingBottomBarInnerHeight / 2).toPx() },
        refractionHeightPx = with(density) { lerp(16f, 22f, pressProgress).dp.toPx() },
        refractionOffsetPx = with(density) { lerp(42f, 58f, pressProgress).dp.toPx() },
        blurRadiusPx = lerp(10.5f, 14.5f, pressProgress),
        dispersion = lerp(0.18f, 0.34f, pressProgress),
        tintAlpha = 0f,
        tintRed = 1f,
        tintGreen = 1f,
        tintBlue = 1f
    )

    CompositionLocalProvider(
        LocalLiquidGlassBottomBarTabScale provides {
            if (isLiquidEffectEnabled) lerp(1.02f, 1.14f, pressProgress) else 1.02f
        }
    ) {
        Box(
            modifier = modifier.width(IntrinsicSize.Min),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { (horizontalPadding * 2).toPx() }
                        tabWidthPx = (contentWidthPx / safeTabsCount).coerceAtLeast(0f)
                    }
                    .graphicsLayer {
                        translationX = panelOffset
                        scaleX = containerScale
                        scaleY = containerScale
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .clip(AppBottomBarShapes.capsule)
                    .border(width = 1.dp, color = containerBorderColor, shape = AppBottomBarShapes.capsule)
                    .then(if (interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                    .drawWithContent {
                        drawContent()
                        if (isLiquidEffectEnabled && pressProgress > 0.02f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isInLightTheme) {
                                        listOf(
                                            Color.White.copy(alpha = 0.05f * pressProgress),
                                            Color.White.copy(alpha = 0.015f * pressProgress),
                                            Color.Transparent
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.035f * pressProgress),
                                            Color.White.copy(alpha = 0.012f * pressProgress),
                                            Color.Transparent
                                        )
                                    }
                                ),
                                blendMode = BlendMode.Plus
                            )
                        }
                    }
                    .height(AppChromeTokens.floatingBottomBarOuterHeight)
            ) {
                LiquidBackdropLayer(
                    modifier = Modifier.fillMaxSize(),
                    enabled = isLiquidEffectEnabled,
                    fallbackColor = containerFallbackColor,
                    spec = containerSpec
                )
                Row(
                    modifier = Modifier
                        .height(AppChromeTokens.floatingBottomBarOuterHeight)
                        .padding(horizontal = horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            if (tabWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) {
                                progressOffset + panelOffset
                            } else {
                                -progressOffset + panelOffset
                            }
                            scaleX = dampedDragAnimation.scaleX / (1f - (dampedDragAnimation.velocity / 10f * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                            scaleY = dampedDragAnimation.scaleY * (1f - (dampedDragAnimation.velocity / 10f * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                            shadowElevation = with(density) { lerp(3f, 14f, pressProgress).dp.toPx() }
                            ambientShadowColor = indicatorShadowColor
                            spotShadowColor = indicatorShadowColor
                            shape = AppBottomBarShapes.capsule
                            clip = false
                        }
                        .then(if (interactiveHighlight != null) interactiveHighlight.gestureModifier else Modifier)
                        .then(dampedDragAnimation.modifier)
                        .clip(AppBottomBarShapes.capsule)
                        .border(width = 1.dp, color = indicatorBorderColor, shape = AppBottomBarShapes.capsule)
                        .drawWithContent {
                            drawContent()
                            if (isLiquidEffectEnabled) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = if (isInLightTheme) {
                                            listOf(
                                                Color.White.copy(alpha = lerp(0.035f, 0.10f, pressProgress)),
                                                Color.White.copy(alpha = lerp(0.010f, 0.030f, pressProgress)),
                                                Color.Transparent
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = lerp(0.020f, 0.050f, pressProgress)),
                                                Color.White.copy(alpha = lerp(0.006f, 0.020f, pressProgress)),
                                                Color.Transparent
                                            )
                                        }
                                    ),
                                    blendMode = BlendMode.Plus
                                )
                                drawRect(
                                    color = if (isInLightTheme) {
                                        Color.Black.copy(alpha = lerp(0.012f, 0.020f, pressProgress))
                                    } else {
                                        Color.Black.copy(alpha = lerp(0.045f, 0.060f, pressProgress))
                                    }
                                )
                            }
                        }
                        .height(AppChromeTokens.floatingBottomBarInnerHeight)
                        .width(with(density) { tabWidthPx.toDp() })
                ) {
                    LiquidBackdropLayer(
                        modifier = Modifier.fillMaxSize(),
                        enabled = isLiquidEffectEnabled,
                        fallbackColor = indicatorFallbackColor,
                        spec = indicatorSpec
                    )
                }
            }
        }
    }
}

private object AppBottomBarShapes {
    val capsule = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
}
