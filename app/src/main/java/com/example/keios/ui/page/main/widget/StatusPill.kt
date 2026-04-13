package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
    backgroundAlphaOverride: Float? = null,
    borderAlphaOverride: Float? = null
) {
    val shape = RoundedCornerShape(999.dp)
    val isDark = isSystemInDarkTheme()
    val backgroundAlpha = backgroundAlphaOverride ?: if (isDark) 0.18f else 0.24f
    val borderAlpha = borderAlphaOverride ?: if (isDark) 0.35f else 0.42f
    val textColor = if (isDark) color else color.copy(alpha = 0.96f)
    Box(
        modifier = Modifier
            .then(modifier)
            .clip(shape)
            .background(color.copy(alpha = backgroundAlpha))
            .border(width = 0.8.dp, color = color.copy(alpha = borderAlpha), shape = shape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor)
    }
}
