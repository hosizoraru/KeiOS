package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: AppStatusPillSize = AppStatusPillSize.Default,
    contentPadding: PaddingValues? = null,
    backgroundAlphaOverride: Float? = null,
    borderAlphaOverride: Float? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val metrics = rememberAppStatusPillMetrics(size)
    val resolvedPadding = contentPadding ?: metrics.contentPadding
    val backgroundAlpha = backgroundAlphaOverride ?: if (isDark) 0.18f else 0.24f
    val borderAlpha = borderAlphaOverride ?: if (isDark) 0.35f else 0.42f
    val textColor = if (isDark) color else color.copy(alpha = 0.96f)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .then(modifier)
            .clip(AppStatusPrimitives.pillShape)
            .background(color.copy(alpha = backgroundAlpha))
            .border(
                width = 0.8.dp,
                color = color.copy(alpha = borderAlpha),
                shape = AppStatusPrimitives.pillShape
            )
            .padding(resolvedPadding),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = metrics.typography.fontSize,
            lineHeight = metrics.typography.lineHeight,
            fontWeight = metrics.typography.fontWeight
        )
    }
}
