package com.example.keios.ui.page.main.widget

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
    width: Dp = 40.dp,
    height: Dp = 40.dp,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content
) {
    val isDark = isSystemInDarkTheme()
    val iconTint = MiuixTheme.colorScheme.primary
    GlassIconButtonContainer(
        backdrop = backdrop,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = width,
        height = height,
        shape = shape,
        blurRadius = blurRadius,
        variant = variant,
        isDark = isDark
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
    width: Dp = 40.dp,
    height: Dp = 40.dp,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content,
    iconTint: Color = Color.Unspecified
) {
    val isDark = isSystemInDarkTheme()
    GlassIconButtonContainer(
        backdrop = backdrop,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        width = width,
        height = height,
        shape = shape,
        blurRadius = blurRadius,
        variant = variant,
        isDark = isDark
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
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
    content: @Composable () -> Unit
) {
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val glass = glassStyle(
        isDark = isDark,
        variant = variant,
        blurRadius = blurRadius
    )
    val showBorder = glass.showBorder
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
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
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = glass.fallbackAlpha))
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
    variant: GlassVariant = GlassVariant.Content
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

    LaunchedEffect(isPressed, onPressedChange) {
        onPressedChange?.invoke(isPressed)
    }
    DisposableEffect(onPressedChange) {
        onDispose { onPressedChange?.invoke(false) }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(ContinuousCapsule)
            .then(
                if (longClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = true,
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                Text(text = text, color = textColor)
            }
        }
    }
}
