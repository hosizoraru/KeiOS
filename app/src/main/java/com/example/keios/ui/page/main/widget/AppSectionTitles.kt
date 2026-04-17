package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppPageSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowTextGap)
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = AppTypographyTokens.CompactTitle.fontSize,
            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
            fontWeight = AppTypographyTokens.CompactTitle.fontWeight
        )
        summary?.takeIf { it.isNotBlank() }?.let { text ->
            Text(
                text = text,
                color = summaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight
            )
        }
    }
}
