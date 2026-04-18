package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.keios.R
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
    eyebrow: String? = null,
    eyebrowColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
    supportingText: String? = null,
    supportingColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.76f),
    startAction: (@Composable () -> Unit)? = null,
    titleAccessory: (@Composable RowScope.() -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    expandable: Boolean = false,
    expanded: Boolean = false,
    expandTint: Color = MiuixTheme.colorScheme.primary,
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 2,
    minHeight: androidx.compose.ui.unit.Dp = AppInteractiveTokens.controlRowMinHeight,
    contentPadding: PaddingValues = CardLayoutRhythm.cardContentPadding,
    titleTypography: AppTypographyToken = AppTypographyTokens.SectionTitle,
    subtitleTypography: AppTypographyToken = AppTypographyTokens.Supporting,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val expandContentDescription = if (expanded) {
        stringResource(R.string.common_collapse)
    } else {
        stringResource(R.string.common_expand)
    }
    val headerModifier = if (onClick != null || onLongClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        modifier
    }

    Row(
        modifier = headerModifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        startAction?.let { action ->
            Box(
                modifier = Modifier.size(AppInteractiveTokens.cardHeaderLeadingSlotSize),
                contentAlignment = Alignment.Center
            ) {
                action()
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowTextGap)
        ) {
            eyebrow?.takeIf { it.isNotBlank() }?.let { value ->
                Text(
                    text = value,
                    color = eyebrowColor,
                    fontSize = AppTypographyTokens.Eyebrow.fontSize,
                    lineHeight = AppTypographyTokens.Eyebrow.lineHeight,
                    fontWeight = AppTypographyTokens.Eyebrow.fontWeight
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = titleTypography.fontSize,
                    lineHeight = titleTypography.lineHeight,
                    fontWeight = titleTypography.fontWeight,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                titleAccessory?.invoke(this)
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = subtitleTypography.fontSize,
                    lineHeight = subtitleTypography.lineHeight,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            supportingText?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    color = supportingColor,
                    fontSize = AppTypographyTokens.Caption.fontSize,
                    lineHeight = AppTypographyTokens.Caption.lineHeight
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
                    contentDescription = expandContentDescription,
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
