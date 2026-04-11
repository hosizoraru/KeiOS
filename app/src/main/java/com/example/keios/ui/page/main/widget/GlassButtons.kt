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
    modifier: Modifier = Modifier,
    blurRadius: Dp? = null,
    lightMaterial: Boolean = false,
    bottomBarStyle: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val iconTint = MiuixTheme.colorScheme.primary
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val blurDp = if (bottomBarStyle) 8.dp else (blurRadius ?: if (isDark) 7.dp else 11.dp)
    val surfaceAlpha = if (bottomBarStyle) 0f else if (lightMaterial) {
        if (isDark) 0.12f else 0.28f
    } else {
        if (isDark) 0.20f else 0.46f
    }
    val overlayAlpha = if (bottomBarStyle) 0f else if (lightMaterial) {
        if (isDark) 0.03f else 0.03f
    } else {
        if (isDark) 0.0f else 0.06f
    }
    val highlightAlpha = if (bottomBarStyle) 1f else if (lightMaterial) {
        if (isDark) 0.52f else 0.58f
    } else {
        if (isDark) 0.9f else 1.0f
    }
    val shadowAlpha = if (bottomBarStyle) {
        if (isDark) 0.2f else 0.1f
    } else if (lightMaterial) {
        if (isDark) 0.08f else 0.10f
    } else {
        if (isDark) 0.12f else 0.20f
    }
    val borderAlpha = if (bottomBarStyle) 0f else if (lightMaterial) 0.10f else 0.16f
    val bottomBarSurface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    val showBorder = !bottomBarStyle
    Box(
        modifier = modifier
            .width(40.dp)
            .height(40.dp)
            .clip(ContinuousCapsule)
            .clickable(onClick = onClick)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(blurDp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = highlightAlpha)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = shadowAlpha)
                            )
                        },
                        onDrawSurface = {
                            if (bottomBarStyle) {
                                drawRect(bottomBarSurface)
                            } else {
                                drawRect(Color.White.copy(alpha = surfaceAlpha))
                                if (overlayAlpha > 0f) {
                                    drawRect(Color.Black.copy(alpha = overlayAlpha))
                                }
                            }
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = if (lightMaterial) 0.68f else 0.9f))
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
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = borderAlpha) else Color.Black.copy(alpha = borderAlpha),
                        shape = ContinuousCapsule
                    )
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint
        )
    }
}

@Composable
fun GlassTextButton(
    backdrop: Backdrop?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    leadingIcon: ImageVector? = null,
    iconTint: Color = textColor,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onPressedChange: ((Boolean) -> Unit)? = null,
    blurRadius: Dp? = null,
    lightMaterial: Boolean = false,
    bottomBarStyle: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val longClick = onLongClick
    val blurDp = if (bottomBarStyle) 8.dp else (blurRadius ?: if (isDark) 7.dp else 11.dp)
    val surfaceAlpha = if (bottomBarStyle) 0f else if (lightMaterial) {
        if (isDark) 0.12f else 0.28f
    } else {
        if (isDark) 0.20f else 0.46f
    }
    val overlayAlpha = if (bottomBarStyle) 0f else if (lightMaterial) {
        if (isDark) 0.03f else 0.03f
    } else {
        if (isDark) 0.0f else 0.06f
    }
    val highlightAlpha = if (bottomBarStyle) 1f else if (lightMaterial) {
        if (isDark) 0.52f else 0.58f
    } else {
        if (isDark) 0.9f else 1.0f
    }
    val shadowAlpha = if (bottomBarStyle) {
        if (isDark) 0.2f else 0.1f
    } else if (lightMaterial) {
        if (isDark) 0.08f else 0.10f
    } else {
        if (isDark) 0.12f else 0.20f
    }
    val borderAlpha = if (bottomBarStyle) 0f else if (lightMaterial) 0.10f else 0.16f
    val bottomBarSurface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    val borderModifier = if (!bottomBarStyle) {
        Modifier.border(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = borderAlpha) else Color.Black.copy(alpha = borderAlpha),
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
                            blur(blurDp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = highlightAlpha)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = shadowAlpha)
                            )
                        },
                        onDrawSurface = {
                            if (bottomBarStyle) {
                                drawRect(bottomBarSurface)
                            } else {
                                drawRect(Color.White.copy(alpha = surfaceAlpha))
                                if (overlayAlpha > 0f) {
                                    drawRect(Color.Black.copy(alpha = overlayAlpha))
                                }
                            }
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = if (lightMaterial) 0.68f else 0.9f))
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
