package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
    singleLine: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val textColor = MiuixTheme.colorScheme.onBackground
    val placeholderColor = MiuixTheme.colorScheme.onBackgroundVariant
    val glassBaseColor = if (isDark) {
        Color(0xFF111111).copy(alpha = 0.60f)
    } else {
        Color.White.copy(alpha = 0.58f)
    }
    val glassOverlayColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val fallbackSurface = MiuixTheme.colorScheme.surfaceContainer

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
                            blur(12.dp.toPx())
                            lens(26.dp.toPx(), 28.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isDark) 0.98f else 1f)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(alpha = if (isDark) 0.24f else 0.14f)
                            )
                        },
                        onDrawSurface = {
                            drawRect(glassBaseColor)
                            drawRect(glassOverlayColor)
                        }
                    )
                } else {
                    Modifier.background(fallbackSurface.copy(alpha = 0.95f), ContinuousCapsule)
                }
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.20f),
                shape = ContinuousCapsule
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = textColor, fontSize = 15.sp),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
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
        )
    }
}
