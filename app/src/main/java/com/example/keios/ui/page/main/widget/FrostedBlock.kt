package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FrostedBlock(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    body: String = "",
    accent: Color,
    content: (@Composable () -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val cardSurface = if (isDark) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val overlayColor = if (isDark) {
        Color.White.copy(alpha = 0.03f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val shadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.24f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(16.dp) },
                        effects = {},
                        highlight = { Highlight.Default.copy(alpha = if (isDark) 0.32f else 0.75f) },
                        shadow = { Shadow.Default.copy(color = shadowColor) },
                        onDrawSurface = {
                            drawRect(cardSurface)
                        }
                    )
                } else {
                    Modifier.background(cardSurface)
                }
            )
            .background(overlayColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = title, color = accent)
            }
        }
        Text(
            text = subtitle,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (content != null) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        } else if (body.isNotBlank()) {
            Text(
                text = body,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
