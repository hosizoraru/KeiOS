package com.example.keios.ui.page.main.widget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LiquidDropdownContainerRadius = 20.dp
private val LiquidDropdownItemRadius = 20.dp

private fun liquidDropdownBlueAccent(isDark: Boolean): Color = if (isDark) {
    Color(0xFF71ADFF)
} else {
    Color(0xFF3B82F6)
}

private fun liquidDropdownItemShape(): RoundedCornerShape = RoundedCornerShape(LiquidDropdownItemRadius)

@Composable
fun LiquidDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = AppInteractiveTokens.liquidDropdownMinWidth,
    maxWidth: Dp = AppInteractiveTokens.liquidDropdownMaxWidth,
    maxHeight: Dp = AppInteractiveTokens.liquidDropdownMaxHeight,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(LiquidDropdownContainerRadius)
    val containerColors = liquidDropdownContainerColors(isDark)
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .widthIn(min = minWidth, max = maxWidth)
            .clip(shape)
            .background(containerColors.baseColor, shape)
            .background(containerColors.middleBrush, shape)
            .background(containerColors.topBrush, shape)
            .background(containerColors.glossBrush, shape)
            .border(
                width = if (isDark) 1.dp else 1.15.dp,
                color = containerColors.borderColor,
                shape = shape
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        SubcomposeLayout(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(scrollState)
        ) { constraints ->
            val minWidthPx = minWidth.roundToPx()
            val maxWidthPx = maxWidth.roundToPx().coerceAtLeast(minWidthPx)
            val probeConstraints = constraints.copy(
                minWidth = 0,
                maxWidth = maxWidthPx,
                minHeight = 0
            )
            val probePlaceables = subcompose("probe", content).map { measurable ->
                measurable.measure(probeConstraints)
            }
            val resolvedWidth = probePlaceables.maxOfOrNull { it.width }
                ?.coerceIn(minWidthPx, maxWidthPx)
                ?: minWidthPx
            val contentConstraints = constraints.copy(
                minWidth = resolvedWidth,
                maxWidth = resolvedWidth,
                minHeight = 0
            )
            val placeables = subcompose("content", content).map { measurable ->
                measurable.measure(contentConstraints)
            }
            val contentHeight = placeables.sumOf { it.height }
            layout(resolvedWidth, contentHeight) {
                var currentY = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, currentY)
                    currentY += placeable.height
                }
            }
        }
    }
}

private data class LiquidDropdownContainerColors(
    val baseColor: Color,
    val middleBrush: Brush,
    val topBrush: Brush,
    val glossBrush: Brush,
    val borderColor: Color
)

@Composable
private fun liquidDropdownContainerColors(isDark: Boolean): LiquidDropdownContainerColors {
    return if (isDark) {
        LiquidDropdownContainerColors(
            baseColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.28f),
            middleBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f),
                    Color(0xFF8BC2FF).copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.02f)
                ),
                start = Offset.Zero,
                end = Offset(420f, 720f)
            ),
            topBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.04f)
                ),
                start = Offset(-40f, 0f),
                end = Offset(440f, 760f)
            ),
            glossBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color(0xFFAED7FF).copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = Offset(96f, 42f),
                radius = 280f
            ),
            borderColor = Color.White.copy(alpha = 0.10f)
        )
    } else {
        LiquidDropdownContainerColors(
            baseColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.26f),
            middleBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f),
                    Color(0xFFE5F1FF).copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.10f)
                ),
                start = Offset.Zero,
                end = Offset(420f, 720f)
            ),
            topBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.62f),
                    Color.White.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                start = Offset(-40f, 0f),
                end = Offset(440f, 760f)
            ),
            glossBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.46f),
                    Color(0xFFD9EBFF).copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(96f, 42f),
                radius = 280f
            ),
            borderColor = Color.White.copy(alpha = 0.70f)
        )
    }
}

@Composable
fun LiquidDropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    variant: GlassVariant = GlassVariant.SheetAction
) {
    val isDark = isSystemInDarkTheme()
    val itemShape = liquidDropdownItemShape()
    val accentColor = when (variant) {
        GlassVariant.SheetDangerAction -> Color(0xFFE25B6A)
        else -> liquidDropdownBlueAccent(isDark)
    }
    val baseTextColor = if (selected) {
        accentColor
    } else {
        MiuixTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.96f else 0.92f)
    }
    val baseCheckColor = if (selected) {
        accentColor
    } else {
        Color.Transparent
    }
    val selectedBorderColor = if (isDark) {
        accentColor.copy(alpha = 0.34f)
    } else {
        accentColor.copy(alpha = 0.38f)
    }
    val selectedShadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val outerTopPadding = if (index == 0) 3.dp else 2.dp
    val outerBottomPadding = if (index == optionSize - 1) 3.dp else 2.dp
    val baseBackdrop = rememberLayerBackdrop()
    val accentBackdrop = rememberLayerBackdrop()
    val selectedBackdrop = rememberCombinedBackdrop(baseBackdrop, accentBackdrop)
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) AppInteractiveTokens.pressedScale else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "liquid_dropdown_item_scale"
    )
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = appControlPressedOverlayAlpha(isPressed = isPressed, isDark = isDark),
        animationSpec = tween(durationMillis = 110),
        label = "liquid_dropdown_item_overlay"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 0.dp)
            .padding(top = outerTopPadding, bottom = outerBottomPadding)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { currentOnClick() }
            )
    ) {
        LiquidDropdownRowContent(
            modifier = Modifier
                .layerBackdrop(baseBackdrop),
            text = text,
            textColor = baseTextColor,
            checkColor = baseCheckColor,
            showCheck = selected
        )

        LiquidDropdownRowContent(
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
                .clearAndSetSemantics {}
                .layerBackdrop(accentBackdrop)
                .graphicsLayer { colorFilter = ColorFilter.tint(accentColor) },
            text = text,
            textColor = Color.White,
            checkColor = Color.White,
            showCheck = selected
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBackdrop(
                        backdrop = selectedBackdrop,
                        shape = { RoundedRectangle(LiquidDropdownItemRadius) },
                        effects = { lens(9.dp.toPx(), 12.dp.toPx(), true) },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isDark) 0.22f else 0.82f)
                        },
                        shadow = {
                            Shadow.Default.copy(color = selectedShadowColor)
                        },
                        innerShadow = {
                            InnerShadow(radius = 7.dp, alpha = if (isDark) 0.16f else 0.08f)
                        },
                        onDrawSurface = {
                            drawRect(
                                color = accentColor.copy(alpha = if (isDark) 0.13f else 0.10f)
                            )
                            drawRect(
                                if (isDark) Color.White.copy(alpha = 0.03f)
                                else Color.White.copy(alpha = 0.10f)
                            )
                        }
                    )
                    .border(1.dp, selectedBorderColor, itemShape)
            )
        }
        if (pressedOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(itemShape)
                    .background(appControlPressedOverlayColor(isDark).copy(alpha = pressedOverlayAlpha))
            )
        }
    }
}

@Composable
private fun LiquidDropdownRowContent(
    modifier: Modifier,
    text: String,
    textColor: Color,
    checkColor: Color,
    showCheck: Boolean
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = AppInteractiveTokens.liquidDropdownRowMinHeight)
            .padding(
                horizontal = AppInteractiveTokens.liquidDropdownRowHorizontalPadding,
                vertical = AppInteractiveTokens.liquidDropdownRowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = AppInteractiveTokens.liquidDropdownRowGap,
            alignment = Alignment.End
        )
    ) {
        Text(
            text = text,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Image(
            modifier = Modifier.size(AppInteractiveTokens.liquidDropdownCheckSize),
            imageVector = MiuixIcons.Basic.Check,
            colorFilter = ColorFilter.tint(if (showCheck) checkColor else Color.Transparent),
            contentDescription = null
        )
    }
}

@Composable
fun LiquidDropdownImpl(
    text: String,
    optionSize: Int,
    isSelected: Boolean,
    index: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.SheetAction
) {
    LiquidDropdownItem(
        text = text,
        selected = isSelected,
        onClick = { onSelectedIndexChange(index) },
        modifier = modifier,
        index = index,
        optionSize = optionSize,
        variant = variant
    )
}
