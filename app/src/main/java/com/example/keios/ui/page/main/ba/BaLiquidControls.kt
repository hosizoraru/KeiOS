package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ui.page.main.widget.GlassVariant
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

data class BaLiquidStyle(
    val blur: Dp,
    val baseColor: Color,
    val overlayColor: Color,
    val borderColor: Color,
    val borderWidth: Dp,
    val highlightAlpha: Float,
    val shadowAlpha: Float,
    val fallbackAlpha: Float,
    val lensStart: Dp,
    val lensEnd: Dp
)

@Composable
fun rememberBaLiquidStyle(
    variant: GlassVariant,
    blurRadius: Dp? = null
): BaLiquidStyle {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, variant, blurRadius) {
        when (variant) {
            GlassVariant.Bar -> BaLiquidStyle(
                blur = blurRadius ?: 8.dp,
                baseColor = Color.Transparent,
                overlayColor = Color.Transparent,
                borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.82f),
                borderWidth = 1.dp,
                highlightAlpha = 1f,
                shadowAlpha = if (isDark) 0.20f else 0.10f,
                fallbackAlpha = if (isDark) 0.40f else 0.56f,
                lensStart = 24.dp,
                lensEnd = 24.dp
            )
            GlassVariant.SheetDangerAction -> {
                val blur = blurRadius ?: if (isDark) 7.dp else 11.dp
                if (isDark) {
                    BaLiquidStyle(
                        blur = blur,
                        baseColor = Color(0xFF1A1214).copy(alpha = 0.30f),
                        overlayColor = Color(0xFFFF8A9B).copy(alpha = 0.08f),
                        borderColor = Color(0xFFFFA1AF).copy(alpha = 0.24f),
                        borderWidth = 1.dp,
                        highlightAlpha = 0.68f,
                        shadowAlpha = 0.12f,
                        fallbackAlpha = 0.78f,
                        lensStart = 27.dp,
                        lensEnd = 27.dp
                    )
                } else {
                    BaLiquidStyle(
                        blur = blur,
                        baseColor = Color.White.copy(alpha = 0.78f),
                        overlayColor = Color(0xFFFFE1E6).copy(alpha = 0.48f),
                        borderColor = Color(0xFFFFD0D8).copy(alpha = 0.96f),
                        borderWidth = 1.55.dp,
                        highlightAlpha = 1f,
                        shadowAlpha = 0.20f,
                        fallbackAlpha = 1f,
                        lensStart = 27.dp,
                        lensEnd = 27.dp
                    )
                }
            }
            else -> {
                val blur = blurRadius ?: if (isDark) 7.dp else 11.dp
                if (isDark) {
                    BaLiquidStyle(
                        blur = blur,
                        baseColor = Color(0xFF0F1115).copy(alpha = 0.62f),
                        overlayColor = Color.White.copy(alpha = 0.07f),
                        borderColor = Color.White.copy(alpha = 0.26f),
                        borderWidth = 1.dp,
                        highlightAlpha = 0.96f,
                        shadowAlpha = 0.18f,
                        fallbackAlpha = 0.92f,
                        lensStart = 26.dp,
                        lensEnd = 28.dp
                    )
                } else {
                    BaLiquidStyle(
                        blur = blur,
                        baseColor = Color.White.copy(alpha = 0.66f),
                        overlayColor = Color(0xFFDCEBFF).copy(alpha = 0.30f),
                        borderColor = Color.White.copy(alpha = 0.96f),
                        borderWidth = 1.dp,
                        highlightAlpha = 1f,
                        shadowAlpha = 0.18f,
                        fallbackAlpha = 0.98f,
                        lensStart = 26.dp,
                        lensEnd = 28.dp
                    )
                }
            }
        }
    }
}

@Composable
fun rememberBaInputLiquidStyle(
    blurRadius: Dp? = null
): BaLiquidStyle {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, blurRadius) {
        val blur = blurRadius ?: if (isDark) 6.dp else 10.dp
        if (isDark) {
            BaLiquidStyle(
                blur = blur,
                baseColor = Color(0xFF15181E).copy(alpha = 0.22f),
                overlayColor = Color.White.copy(alpha = 0.05f),
                borderColor = Color.White.copy(alpha = 0.16f),
                borderWidth = 1.dp,
                highlightAlpha = 0.62f,
                shadowAlpha = 0.10f,
                fallbackAlpha = 0.74f,
                lensStart = 24.dp,
                lensEnd = 24.dp
            )
        } else {
            BaLiquidStyle(
                blur = blur,
                baseColor = Color.White.copy(alpha = 0.74f),
                overlayColor = Color(0xFFD8E9FF).copy(alpha = 0.24f),
                borderColor = Color.White.copy(alpha = 0.998f),
                borderWidth = 1.45.dp,
                highlightAlpha = 1f,
                shadowAlpha = 0.22f,
                fallbackAlpha = 0.99f,
                lensStart = 26.dp,
                lensEnd = 26.dp
            )
        }
    }
}

@Composable

fun BaLiquidButton(
    backdrop: Backdrop?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = textColor,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onPressedChange: ((Boolean) -> Unit)? = null,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content
) {
    val style = rememberBaLiquidStyle(variant = variant, blurRadius = blurRadius)
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val overlay = containerColor?.copy(alpha = if (variant == GlassVariant.SheetDangerAction) 0.18f else 0.22f)

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
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                        onLongClick = onLongClick
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
                            blur(style.blur.toPx())
                            lens(style.lensStart.toPx(), style.lensEnd.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = style.highlightAlpha) },
                        shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = style.shadowAlpha)) },
                        onDrawSurface = {
                            if (variant == GlassVariant.Bar) {
                                drawRect(fallbackSurface.copy(alpha = style.fallbackAlpha))
                            } else {
                                drawRect(style.baseColor)
                                if (style.overlayColor != Color.Transparent) drawRect(style.overlayColor)
                            }
                            overlay?.let { drawRect(it) }
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = style.fallbackAlpha), ContinuousCapsule)
                }
            )
            .border(style.borderWidth, style.borderColor, ContinuousCapsule)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(imageVector = it, contentDescription = null, tint = iconTint)
            }
            if (text.isNotBlank()) {
                Text(text = text, color = textColor)
            }
        }
    }
}

@Composable

fun BaLiquidIconButton(
    backdrop: Backdrop?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 40.dp,
    height: Dp = 40.dp,
    shape: Shape = ContinuousCapsule,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content
) {
    val style = rememberBaLiquidStyle(variant = variant, blurRadius = blurRadius)
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .clickable(onClick = onClick)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(style.blur.toPx())
                            lens(style.lensStart.toPx(), style.lensEnd.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = style.highlightAlpha) },
                        shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = style.shadowAlpha)) },
                        onDrawSurface = {
                            if (variant == GlassVariant.Bar) {
                                drawRect(fallbackSurface.copy(alpha = style.fallbackAlpha))
                            } else {
                                drawRect(style.baseColor)
                                if (style.overlayColor != Color.Transparent) drawRect(style.overlayColor)
                            }
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = style.fallbackAlpha), shape)
                }
            )
            .border(style.borderWidth, style.borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.primary
        )
    }
}

@Composable

fun BaLiquidInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: TextUnit = 15.sp,
    textColor: Color = MiuixTheme.colorScheme.onBackground,
    onImeActionDone: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    blurRadius: Dp? = null,
    variant: GlassVariant = GlassVariant.Content
) {
    val focusManager = LocalFocusManager.current
    val style = rememberBaInputLiquidStyle(blurRadius = blurRadius)
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer
    val placeholderColor = MiuixTheme.colorScheme.onBackgroundVariant
    val contentAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(ContinuousCapsule)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(style.blur.toPx())
                            lens(style.lensStart.toPx(), style.lensEnd.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = style.highlightAlpha) },
                        shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = style.shadowAlpha)) },
                        onDrawSurface = {
                            drawRect(style.baseColor)
                            if (style.overlayColor != Color.Transparent) drawRect(style.overlayColor)
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = style.fallbackAlpha), ContinuousCapsule)
                }
            )
            .border(style.borderWidth, style.borderColor, ContinuousCapsule)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = textColor, fontSize = fontSize, textAlign = textAlign),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = if (singleLine) KeyboardOptions(imeAction = ImeAction.Done) else KeyboardOptions.Default,
            keyboardActions = KeyboardActions(
                onDone = {
                    onImeActionDone?.invoke()
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = contentAlignment
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = label,
                            color = placeholderColor,
                            fontSize = fontSize,
                            textAlign = textAlign,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

