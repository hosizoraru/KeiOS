package com.example.keios.ui.page.main.widget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GlassIconButton(
    backdrop: Backdrop?,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val resolvedWidth = if (width == Dp.Unspecified) defaultGlassIconButtonSize(variant) else width
    val resolvedHeight = if (height == Dp.Unspecified) defaultGlassIconButtonSize(variant) else height
    GlassIconButtonContainer(
        backdrop = backdrop,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = resolvedWidth,
        height = resolvedHeight,
        shape = shape,
        blurRadius = blurRadius,
        variant = variant,
        isDark = isDark,
        containerColor = containerColor
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint
        )
    }
}

@Composable
fun GlassIconButton(
    backdrop: Backdrop?,
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = Color.Unspecified,
    iconModifier: Modifier = Modifier,
    containerColor: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val resolvedWidth = if (width == Dp.Unspecified) defaultGlassIconButtonSize(variant) else width
    val resolvedHeight = if (height == Dp.Unspecified) defaultGlassIconButtonSize(variant) else height
    GlassIconButtonContainer(
        backdrop = backdrop,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = resolvedWidth,
        height = resolvedHeight,
        shape = shape,
        blurRadius = blurRadius,
        variant = variant,
        isDark = isDark,
        containerColor = containerColor
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = iconTint
        )
    }
}

@Composable
private fun GlassIconButtonContainer(
    backdrop: Backdrop?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
    width: Dp,
    height: Dp,
    shape: Shape,
    blurRadius: Dp?,
    variant: GlassVariant,
    isDark: Boolean,
    containerColor: Color?,
    content: @Composable () -> Unit
) {
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val glass = glassStyle(
        isDark = isDark,
        variant = variant,
        blurRadius = blurRadius
    )
    val showBorder = glass.showBorder
    val containerOverlay = containerColor?.copy(
        alpha = when (variant) {
            GlassVariant.Bar -> 0.34f
            GlassVariant.SheetInput -> if (isDark) 0.20f else 0.20f
            GlassVariant.SheetAction -> if (isDark) 0.24f else 0.34f
            GlassVariant.SheetPrimaryAction -> if (isDark) 0.18f else 0.18f
            GlassVariant.Compact -> if (isDark) 0.22f else 0.28f
            GlassVariant.SheetDangerAction -> if (isDark) 0.18f else 0.18f
            GlassVariant.Content -> if (isDark) 0.26f else 0.32f
        }
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) AppInteractiveTokens.pressedScale else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "glass_icon_button_scale"
    )
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = appControlPressedOverlayAlpha(isPressed = isPressed, isDark = isDark),
        animationSpec = tween(durationMillis = 110),
        label = "glass_icon_button_overlay"
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(shape)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                }
            )
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(glass.blur.toPx())
                            lens(glass.lensStart.toPx(), glass.lensEnd.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = glass.highlightAlpha)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = glass.shadowAlpha)
                            )
                        },
                        onDrawSurface = {
                            if (variant == GlassVariant.Bar) {
                                drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                            } else {
                                drawRect(glass.baseColor)
                                if (glass.overlayColor != Color.Transparent) {
                                    drawRect(glass.overlayColor)
                                }
                            }
                            containerOverlay?.let { drawRect(it) }
                        }
                    )
                } else {
                    val fallbackColor = containerOverlay ?: fallbackSurface.copy(alpha = glass.fallbackAlpha)
                    Modifier.background(fallbackColor)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showBorder) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(ContinuousCapsule)
                    .border(
                        width = glass.borderWidth,
                        color = glass.borderColor,
                        shape = ContinuousCapsule
                    )
            )
        }
        if (pressedOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(appControlPressedOverlayColor(isDark).copy(alpha = pressedOverlayAlpha))
            )
        }
        content()
    }
}

@Composable
fun GlassTextButton(
    backdrop: Backdrop?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color? = null,
    leadingIcon: ImageVector? = null,
    iconTint: Color = textColor,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onPressedChange: ((Boolean) -> Unit)? = null,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    minHeight: Dp = defaultGlassTextButtonMinHeight(variant),
    horizontalPadding: Dp = defaultGlassTextButtonHorizontalPadding(variant),
    verticalPadding: Dp = defaultGlassTextButtonVerticalPadding(variant),
    textMaxLines: Int = Int.MAX_VALUE,
    textOverflow: TextOverflow = TextOverflow.Clip,
    textSoftWrap: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val longClick = onLongClick
    val glass = glassStyle(
        isDark = isDark,
        variant = variant,
        blurRadius = blurRadius
    )
    val containerOverlay = containerColor?.copy(
        alpha = when (variant) {
            GlassVariant.Bar -> 0.34f
            GlassVariant.SheetInput -> if (isDark) 0.20f else 0.20f
            GlassVariant.SheetAction -> if (isDark) 0.24f else 0.34f
            GlassVariant.SheetPrimaryAction -> if (isDark) 0.18f else 0.18f
            GlassVariant.Compact -> if (isDark) 0.22f else 0.28f
            GlassVariant.SheetDangerAction -> if (isDark) 0.18f else 0.18f
            GlassVariant.Content -> if (isDark) 0.26f else 0.32f
        }
    )
    val borderModifier = if (glass.showBorder) {
        Modifier.border(
            width = glass.borderWidth,
            color = glass.borderColor,
            shape = ContinuousCapsule
        )
    } else {
        Modifier
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (enabled && isPressed) AppInteractiveTokens.pressedScale else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "glass_text_button_scale"
    )
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = appControlPressedOverlayAlpha(
            isPressed = enabled && isPressed,
            isDark = isDark
        ),
        animationSpec = tween(durationMillis = 110),
        label = "glass_text_button_overlay"
    )

    LaunchedEffect(isPressed, onPressedChange) {
        onPressedChange?.invoke(isPressed)
    }
    DisposableEffect(onPressedChange) {
        onDispose { onPressedChange?.invoke(false) }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
            }
            .clip(ContinuousCapsule)
            .then(
                if (longClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = { if (enabled) onClick() },
                        onLongClick = longClick
                    )
                } else {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                }
            )
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(glass.blur.toPx())
                            lens(glass.lensStart.toPx(), glass.lensEnd.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = glass.highlightAlpha)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = glass.shadowAlpha)
                            )
                        },
                        onDrawSurface = {
                            if (variant == GlassVariant.Bar) {
                                drawRect(fallbackSurface.copy(alpha = glass.fallbackAlpha))
                            } else {
                                drawRect(glass.baseColor)
                                if (glass.overlayColor != Color.Transparent) {
                                    drawRect(glass.overlayColor)
                                }
                            }
                            containerOverlay?.let { drawRect(it) }
                        }
                    )
                } else {
                    val fallbackColor = containerOverlay ?: fallbackSurface.copy(alpha = glass.fallbackAlpha)
                    Modifier.background(fallbackColor)
                }
            )
            .then(borderModifier)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        if (pressedOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(ContinuousCapsule)
                    .background(appControlPressedOverlayColor(isDark).copy(alpha = pressedOverlayAlpha))
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppInteractiveTokens.controlContentGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
                    maxLines = textMaxLines,
                    overflow = textOverflow,
                    softWrap = textSoftWrap
                )
            }
        }
    }
}
