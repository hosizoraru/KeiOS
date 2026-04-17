package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppOverviewCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.68f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.18f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    contentVerticalSpacing: Dp = CardLayoutRhythm.denseSectionGap,
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    startAction: (@Composable () -> Unit)? = null,
    headerEndActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(containerColor, shape),
        colors = CardDefaults.defaultColors(
            color = containerColor,
            contentColor = contentColor
        ),
        insideMargin = PaddingValues(0.dp),
        showIndication = showIndication && (onClick != null || onLongClick != null),
        onClick = onClick,
        onLongPress = onLongClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(contentVerticalSpacing)
        ) {
            AppCardHeader(
                title = title,
                subtitle = subtitle,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                startAction = startAction,
                endActions = headerEndActions
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CardLayoutRhythm.cardHorizontalPadding,
                        end = CardLayoutRhythm.cardHorizontalPadding,
                        bottom = CardLayoutRhythm.cardVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(contentVerticalSpacing),
                content = content
            )
        }
    }
}

@Preview(name = "Overview Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun AppOverviewCardPreviewLight() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        AppOverviewCard(
            title = "GitHub 项目追踪",
            subtitle = "点击刷新，长按新增",
            containerColor = Color(0xFFEFF6FF),
            borderColor = Color(0xFF93C5FD),
            headerEndActions = {
                StatusPill(
                    label = "3m 前",
                    color = Color(0xFF2563EB)
                )
                StatusPill(
                    label = "已检查",
                    color = Color(0xFF22C55E)
                )
            }
        ) {
            AppInfoRow(label = "追踪项目", value = "18")
            AppInfoRow(label = "可更新", value = "4", valueColor = Color(0xFF2563EB))
            AppInfoRow(label = "预发行", value = "2", valueColor = Color(0xFFF59E0B))
        }
    }
}

@Preview(name = "Overview Dark", showBackground = true, backgroundColor = 0xFF111827)
@Composable
private fun AppOverviewCardPreviewDark() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
        AppOverviewCard(
            title = "系统参数与属性",
            subtitle = "点击刷新系统表",
            containerColor = Color(0xFF1F2937),
            borderColor = Color(0xFF334155),
            titleColor = Color.White,
            subtitleColor = Color(0xFFCBD5E1),
            headerEndActions = {
                StatusPill(
                    label = "缓存",
                    color = Color(0xFFF59E0B)
                )
            }
        ) {
            AppInfoRow(label = "System", value = "82 项", labelColor = Color(0xFFCBD5E1), valueColor = Color.White)
            AppInfoRow(label = "Android", value = "31 项", labelColor = Color(0xFFCBD5E1), valueColor = Color.White)
            AppInfoRow(label = "Java", value = "16 项", labelColor = Color(0xFFCBD5E1), valueColor = Color.White)
        }
    }
}
