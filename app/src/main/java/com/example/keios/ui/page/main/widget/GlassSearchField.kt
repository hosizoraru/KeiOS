package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GlassSearchField(
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
    blurRadius: Dp? = null,
    lightMaterial: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val placeholderColor = MiuixTheme.colorScheme.onBackgroundVariant
    val blurDp = blurRadius ?: 12.dp
    val glassBaseColor = if (isDark) {
        Color(0xFF111111).copy(alpha = if (lightMaterial) 0.12f else 0.60f)
    } else {
        Color.White.copy(alpha = if (lightMaterial) 0.28f else 0.58f)
    }
    val glassOverlayColor = if (isDark) {
        Color.White.copy(alpha = if (lightMaterial) 0.03f else 0.08f)
    } else {
        Color.Black.copy(alpha = if (lightMaterial) 0.03f else 0.06f)
    }
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer

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
                            blur(blurDp.toPx())
                            lens(
                                if (lightMaterial) 24.dp.toPx() else 26.dp.toPx(),
                                if (lightMaterial) 24.dp.toPx() else 28.dp.toPx()
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(
                                alpha = if (lightMaterial) {
                                    if (isDark) 0.52f else 0.58f
                                } else {
                                    if (isDark) 0.98f else 1f
                                }
                            )
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(
                                    alpha = if (lightMaterial) {
                                        if (isDark) 0.08f else 0.10f
                                    } else {
                                        if (isDark) 0.24f else 0.14f
                                    }
                                )
                            )
                        },
                        onDrawSurface = {
                            drawRect(glassBaseColor)
                            drawRect(glassOverlayColor)
                        }
                    )
                } else {
                    Modifier.background(
                        fallbackSurface.copy(alpha = if (lightMaterial) 0.68f else 0.95f),
                        ContinuousCapsule
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isDark) {
                    Color.White.copy(alpha = if (lightMaterial) 0.10f else 0.28f)
                } else {
                    Color.Black.copy(alpha = if (lightMaterial) 0.10f else 0.20f)
                },
                shape = ContinuousCapsule
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = textColor, fontSize = fontSize, textAlign = textAlign),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
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
