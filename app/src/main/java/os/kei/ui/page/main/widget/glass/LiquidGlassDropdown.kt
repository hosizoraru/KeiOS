package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LiquidGlassDropdownContainerRadius = 26.dp
private val LiquidGlassDropdownItemRadius = 18.dp
private val LiquidGlassDropdownMinWidth = 168.dp
private val LiquidGlassDropdownMaxWidth = 280.dp
private val LiquidGlassDropdownMaxHeight = 336.dp
private val LiquidGlassDropdownContentPadding = 6.dp
private val LiquidGlassDropdownRowMinHeight = 44.dp
private val LiquidGlassDropdownIconSize = 18.dp
private val LiquidGlassDropdownCheckSize = 18.dp
private val LocalLiquidGlassDropdownSizingPass = staticCompositionLocalOf { false }
private val LocalLiquidGlassDropdownBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun LiquidGlassDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = LiquidGlassDropdownMinWidth,
    maxWidth: Dp = LiquidGlassDropdownMaxWidth,
    maxHeight: Dp = LiquidGlassDropdownMaxHeight,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerShape = RoundedRectangle(LiquidGlassDropdownContainerRadius)
    val scrollState = rememberScrollState()
    val colors = liquidGlassDropdownContainerColors(
        isDark = isDark,
        accentColor = accentColor
    )
    val activeBackdrop = backdrop.takeIf { LocalLiquidControlsEnabled.current }

    Box(
        modifier = modifier
            .widthIn(min = minWidth, max = maxWidth)
            .shadow(
                elevation = 18.dp,
                shape = containerShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.22f else 0.12f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.18f else 0.10f)
            )
            .clip(containerShape)
            .border(
                width = 1.dp,
                color = colors.borderColor,
                shape = containerShape
            )
            .then(
                if (activeBackdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = activeBackdrop,
                        shape = { containerShape },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(
                                18.dp.toPx(),
                                34.dp.toPx(),
                                chromaticAberration = true,
                                depthEffect = true
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isDark) 0.72f else 0.86f)
                        },
                        shadow = {
                            Shadow.Default.copy(color = Color.Black.copy(alpha = if (isDark) 0.22f else 0.14f))
                        },
                        innerShadow = {
                            InnerShadow(radius = 10.dp, alpha = if (isDark) 0.20f else 0.12f)
                        },
                        onDrawSurface = {
                            drawRect(colors.surfaceColor)
                            drawRect(colors.topSheen)
                        }
                    )
                } else {
                    Modifier
                        .background(colors.fallbackBaseColor, containerShape)
                        .background(colors.fallbackMiddleBrush, containerShape)
                        .background(colors.fallbackSheenBrush, containerShape)
                }
            )
    ) {
        CompositionLocalProvider(LocalLiquidGlassDropdownBackdrop provides activeBackdrop) {
            SubcomposeLayout(
                modifier = Modifier
                    .padding(LiquidGlassDropdownContentPadding)
                    .heightIn(max = maxHeight)
                    .verticalScroll(scrollState)
            ) { constraints ->
                val minWidthPx = minWidth.roundToPx()
                val maxWidthPx = maxWidth.roundToPx().coerceAtLeast(minWidthPx)
                val contentInsetPx = LiquidGlassDropdownContentPadding.roundToPx() * 2
                val minContentWidth = (minWidthPx - contentInsetPx).coerceAtLeast(0)
                val maxContentWidth = (maxWidthPx - contentInsetPx).coerceAtLeast(minContentWidth)
                val probeConstraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = maxContentWidth,
                    minHeight = 0
                )
                val probePlaceables = subcompose("probe") {
                    CompositionLocalProvider(
                        LocalLiquidGlassDropdownSizingPass provides true,
                        content = content
                    )
                }.map { measurable ->
                    measurable.measure(probeConstraints)
                }
                val resolvedWidth = probePlaceables.maxOfOrNull { it.width }
                    ?.coerceIn(minContentWidth, maxContentWidth)
                    ?: minContentWidth
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
}

@Composable
fun LiquidGlassDropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    highlighted: Boolean = selected,
    showCheck: Boolean = selected,
    highlightContent: Boolean = selected && showCheck
) {
    if (LocalLiquidGlassDropdownSizingPass.current) {
        LiquidGlassDropdownMeasureItem(
            text = text,
            selected = selected,
            modifier = modifier,
            index = index,
            optionSize = optionSize,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            accentColor = accentColor,
            variant = variant,
            highlighted = highlighted,
            showCheck = showCheck,
            highlightContent = highlightContent
        )
        return
    }

    val isDark = isSystemInDarkTheme()
    val itemBackdrop = LocalLiquidGlassDropdownBackdrop.current
    val itemAccent = liquidGlassDropdownItemAccent(
        isDark = isDark,
        accentColor = accentColor,
        variant = variant
    )
    val contentHighlighted = highlighted && highlightContent
    val textColor = if (contentHighlighted) {
        itemAccent
    } else {
        MiuixTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.96f else 0.92f)
    }
    val iconColor = if (contentHighlighted) {
        itemAccent
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.88f else 0.78f)
    }
    val selectedSurface = liquidGlassDropdownSelectedSurfaceColor(isDark = isDark)
    val currentOnClick by rememberUpdatedState(onClick)
    val rowShape = RoundedRectangle(LiquidGlassDropdownItemRadius)
    val outerTopPadding = if (index == 0) 0.dp else 2.dp
    val outerBottomPadding = if (index == optionSize - 1) 0.dp else 2.dp

    if (itemBackdrop != null && highlighted) {
        LiquidSurface(
            backdrop = itemBackdrop,
            modifier = modifier
                .padding(top = outerTopPadding, bottom = outerBottomPadding)
                .defaultMinSize(minHeight = LiquidGlassDropdownRowMinHeight),
            enabled = enabled,
            shape = rowShape,
            tint = Color.Unspecified,
            surfaceColor = selectedSurface,
            blurRadius = 3.dp,
            lensRadius = 14.dp,
            chromaticAberration = highlighted,
            depthEffect = true,
            shadow = true,
            onClick = { currentOnClick() }
        ) {
            LiquidGlassDropdownRowContent(
                text = text,
                textColor = textColor,
                iconColor = iconColor,
                checkColor = itemAccent,
                showCheck = showCheck,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                modifier = Modifier.matchParentSize()
            )
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by appMotionFloatState(
            targetValue = if (pressed && enabled) AppInteractiveTokens.pressedScale else 1f,
            durationMillis = 110,
            label = "liquid_glass_dropdown_item_scale"
        )
        val pressedAlpha by appMotionFloatState(
            targetValue = appControlPressedOverlayAlpha(pressed && enabled, isDark),
            durationMillis = 110,
            label = "liquid_glass_dropdown_item_pressed_alpha"
        )
        val showSelectionPill = highlighted || pressed
        val pillSurface = if (highlighted) {
            selectedSurface
        } else {
            liquidGlassDropdownPressedSurfaceColor(isDark = isDark)
        }
        Box(
            modifier = modifier
                .padding(top = outerTopPadding, bottom = outerBottomPadding)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (showSelectionPill) {
                        Modifier.shadow(
                            elevation = 10.dp,
                            shape = rowShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.18f else 0.10f),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.16f else 0.08f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(rowShape)
                .background(if (showSelectionPill) pillSurface else Color.Transparent, rowShape)
                .then(
                    if (showSelectionPill) {
                        Modifier.border(
                            width = 1.dp,
                            color = liquidGlassDropdownSelectedBorderColor(isDark = isDark),
                            shape = rowShape
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { currentOnClick() }
                )
                .defaultMinSize(minHeight = LiquidGlassDropdownRowMinHeight)
        ) {
            LiquidGlassDropdownRowContent(
                text = text,
                textColor = textColor,
                iconColor = iconColor,
                checkColor = itemAccent,
                showCheck = showCheck,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                modifier = Modifier.matchParentSize()
            )
            if (pressedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = MiuixTheme.colorScheme.onBackground.copy(alpha = pressedAlpha),
                            shape = rowShape
                        )
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassDropdownMeasureItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    highlighted: Boolean = selected,
    showCheck: Boolean = selected,
    highlightContent: Boolean = selected && showCheck
) {
    val isDark = isSystemInDarkTheme()
    val itemAccent = liquidGlassDropdownItemAccent(
        isDark = isDark,
        accentColor = accentColor,
        variant = variant
    )
    val contentHighlighted = highlighted && highlightContent
    val textColor = if (contentHighlighted) {
        itemAccent
    } else {
        MiuixTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.96f else 0.92f)
    }
    val iconColor = if (contentHighlighted) {
        itemAccent
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.88f else 0.78f)
    }
    val outerTopPadding = if (index == 0) 0.dp else 2.dp
    val outerBottomPadding = if (index == optionSize - 1) 0.dp else 2.dp

    Box(
        modifier = modifier
            .padding(top = outerTopPadding, bottom = outerBottomPadding)
            .defaultMinSize(minHeight = LiquidGlassDropdownRowMinHeight)
    ) {
        LiquidGlassDropdownRowContent(
            text = text,
            textColor = textColor,
            iconColor = iconColor,
            checkColor = itemAccent,
            showCheck = showCheck,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun LiquidGlassDropdownRowContent(
    text: String,
    textColor: Color,
    iconColor: Color,
    checkColor: Color,
    showCheck: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(LiquidGlassDropdownIconSize)
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(LiquidGlassDropdownIconSize)
            )
        }
        if (showCheck) {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier.size(LiquidGlassDropdownCheckSize)
            )
        }
    }
}

@Composable
fun LiquidGlassDropdownSingleChoiceItem(
    text: String,
    optionSize: Int,
    isSelected: Boolean,
    index: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true
) {
    LiquidGlassDropdownItem(
        text = text,
        selected = isSelected,
        onClick = { onSelectedIndexChange(index) },
        modifier = modifier,
        index = index,
        optionSize = optionSize,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        accentColor = accentColor,
        variant = variant,
        enabled = enabled,
        highlighted = isSelected,
        showCheck = isSelected,
        highlightContent = isSelected
    )
}

@Composable
fun LiquidGlassDropdownSingleChoiceList(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true
) {
    options.forEachIndexed { index, option ->
        LiquidGlassDropdownSingleChoiceItem(
            text = option,
            optionSize = options.size,
            isSelected = selectedIndex == index,
            index = index,
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            accentColor = accentColor,
            variant = variant,
            enabled = enabled
        )
    }
}

@Composable
fun AppStandaloneLiquidGlassDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = LiquidGlassDropdownMinWidth,
    maxWidth: Dp = LiquidGlassDropdownMaxWidth,
    maxHeight: Dp = LiquidGlassDropdownMaxHeight,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val localBackdrop = rememberLayerBackdrop()
    AppStandaloneBackdropHost(
        backdrop = localBackdrop,
        modifier = modifier
    ) {
        LiquidGlassDropdownColumn(
            minWidth = minWidth,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            accentColor = accentColor,
            backdrop = localBackdrop,
            content = content
        )
    }
}

@Composable
fun AppLiquidGlassDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = LiquidGlassDropdownMinWidth,
    maxWidth: Dp = LiquidGlassDropdownMaxWidth,
    maxHeight: Dp = LiquidGlassDropdownMaxHeight,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit
) {
    if (backdrop != null) {
        LiquidGlassDropdownColumn(
            modifier = modifier,
            minWidth = minWidth,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            accentColor = accentColor,
            backdrop = backdrop,
            content = content
        )
    } else {
        AppStandaloneLiquidGlassDropdownColumn(
            modifier = modifier,
            minWidth = minWidth,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            accentColor = accentColor,
            content = content
        )
    }
}

@Composable
fun LiquidGlassDropdownActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    optionSize: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    highlighted: Boolean = false
) {
    LiquidGlassDropdownItem(
        text = text,
        selected = false,
        onClick = onClick,
        modifier = modifier,
        index = index,
        optionSize = optionSize,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        accentColor = accentColor,
        variant = variant,
        enabled = enabled,
        highlighted = highlighted,
        showCheck = false,
        highlightContent = false
    )
}

private data class LiquidGlassDropdownContainerColors(
    val surfaceColor: Color,
    val topSheen: Color,
    val borderColor: Color,
    val fallbackBaseColor: Color,
    val fallbackMiddleBrush: Brush,
    val fallbackSheenBrush: Brush
)

@Composable
private fun liquidGlassDropdownContainerColors(
    isDark: Boolean,
    accentColor: Color
): LiquidGlassDropdownContainerColors {
    return if (isDark) {
        LiquidGlassDropdownContainerColors(
            surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
            topSheen = Color.White.copy(alpha = 0.16f),
            borderColor = Color.White.copy(alpha = 0.24f),
            fallbackBaseColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            fallbackMiddleBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.10f)
                ),
                start = Offset.Zero,
                end = Offset(320f, 420f)
            ),
            fallbackSheenBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(96f, 24f),
                radius = 260f
            )
        )
    } else {
        LiquidGlassDropdownContainerColors(
            surfaceColor = Color.White.copy(alpha = 0.68f),
            topSheen = Color.White.copy(alpha = 0.28f),
            borderColor = Color.White.copy(alpha = 0.90f),
            fallbackBaseColor = Color.White.copy(alpha = 0.94f),
            fallbackMiddleBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f),
                    Color(0xFFEAF3FF).copy(alpha = 0.58f),
                    Color.White.copy(alpha = 0.36f)
                ),
                start = Offset.Zero,
                end = Offset(320f, 420f)
            ),
            fallbackSheenBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.78f),
                    Color(0xFFE1EFFF).copy(alpha = 0.30f),
                    Color.Transparent
                ),
                center = Offset(96f, 24f),
                radius = 260f
            )
        )
    }
}

private fun liquidGlassDropdownSelectedSurfaceColor(isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.20f)
    } else {
        Color(0xFFEFF4FB).copy(alpha = 0.72f)
    }
}

private fun liquidGlassDropdownPressedSurfaceColor(isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.52f)
    }
}

private fun liquidGlassDropdownSelectedBorderColor(isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
}

private fun liquidGlassDropdownItemAccent(
    isDark: Boolean,
    accentColor: Color,
    variant: GlassVariant
): Color {
    return when (variant) {
        GlassVariant.SheetDangerAction -> Color(0xFFE25B6A)
        else -> if (accentColor == Color.Unspecified) {
            if (isDark) Color(0xFF71ADFF) else Color(0xFF3B82F6)
        } else {
            accentColor
        }
    }
}
