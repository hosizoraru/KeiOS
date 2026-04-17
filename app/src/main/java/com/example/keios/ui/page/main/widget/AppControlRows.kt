package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppControlRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f),
    minHeight: Dp = AppInteractiveTokens.controlRowMinHeight,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    AppControlRow(
        modifier = modifier,
        summary = summary,
        summaryColor = summaryColor,
        minHeight = minHeight,
        onClick = onClick,
        titleContent = {
            Text(
                text = title,
                color = titleColor,
                fontSize = AppTypographyTokens.CompactTitle.fontSize,
                lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                fontWeight = AppTypographyTokens.CompactTitle.fontWeight
            )
        },
        trailing = trailing
    )
}

@Composable
fun AppControlRow(
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f),
    minHeight: Dp = AppInteractiveTokens.controlRowMinHeight,
    onClick: (() -> Unit)? = null,
    titleContent: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { base -> if (onClick != null) base.clickable { onClick() } else base }
            .defaultMinSize(minHeight = minHeight)
            .padding(vertical = CardLayoutRhythm.controlRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            titleContent()
            summary?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    color = summaryColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    modifier = Modifier.padding(top = CardLayoutRhythm.controlRowTextGap)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing
        )
    }
}
