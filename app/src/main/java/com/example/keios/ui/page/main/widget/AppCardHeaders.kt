package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppCardHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    supportingText: String? = null,
    supportingColor: Color = subtitleColor,
    startAction: (@Composable () -> Unit)? = null,
    titleAccessory: (@Composable RowScope.() -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    expandable: Boolean = false,
    expanded: Boolean = false,
    expandTint: Color = MiuixTheme.colorScheme.primary,
    subtitleMaxLines: Int = 2,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val headerModifier = if (onClick != null || onLongClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        modifier
    }

    Row(
        modifier = headerModifier
            .fillMaxWidth()
            .padding(CardLayoutRhythm.cardContentPadding),
        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        startAction?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowTextGap)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = AppTypographyTokens.SectionTitle.fontSize,
                    lineHeight = AppTypographyTokens.SectionTitle.lineHeight,
                    fontWeight = AppTypographyTokens.SectionTitle.fontWeight
                )
                titleAccessory?.invoke(this)
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            supportingText?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    color = supportingColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            endActions?.invoke(this)
            if (expandable) {
                Icon(
                    imageVector = if (expanded) MiuixIcons.Regular.ExpandLess else MiuixIcons.Regular.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = expandTint
                )
            }
        }
    }
}

@Preview(name = "Card Header Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun AppCardHeaderPreviewLight() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        AppCardHeader(
            title = "MCP Logs",
            subtitle = "8 条日志 · 长按可导出",
            supportingText = "点击展开查看详细记录",
            startAction = {
                Icon(
                    imageVector = MiuixIcons.Regular.Info,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
            },
            endActions = {
                StatusPill(
                    label = "已激活",
                    color = Color(0xFF22C55E)
                )
            },
            expandable = true,
            expanded = false
        )
    }
}
