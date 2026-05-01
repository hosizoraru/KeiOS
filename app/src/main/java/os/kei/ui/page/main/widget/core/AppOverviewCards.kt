package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
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
    contentVerticalSpacing: Dp = CardLayoutRhythm.overviewSectionGap,
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    startAction: (@Composable () -> Unit)? = null,
    headerEndActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(CardLayoutRhythm.cardCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(containerColor, shape)
            .then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.overviewHeaderBodyGap)
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            minHeight = 44.dp,
            contentPadding = PaddingValues(
                horizontal = CardLayoutRhythm.overviewHeaderHorizontalPadding,
                vertical = CardLayoutRhythm.overviewHeaderVerticalPadding
            ),
            titleTypography = AppTypographyTokens.CompactTitle,
            startAction = startAction,
            endActions = headerEndActions
        )
        AppCardBodyColumn(
            contentPadding = PaddingValues(
                start = CardLayoutRhythm.cardHorizontalPadding,
                end = CardLayoutRhythm.cardHorizontalPadding,
                bottom = CardLayoutRhythm.overviewBodyBottomPadding
            ),
            verticalSpacing = contentVerticalSpacing,
            content = content
        )
    }
}

@Composable
fun AppOverviewMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    containerColor: Color? = null,
    borderColor: Color? = null,
    valueMaxLines: Int = 2,
    emphasizedValue: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val resolvedContainerColor = containerColor ?: if (isDark) {
        Color(0xFF0F1115).copy(alpha = 0.34f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    val resolvedOverlayColor = if (containerColor != null) {
        Color.Transparent
    } else if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color(0xFFDCEBFF).copy(alpha = 0.24f)
    }
    val resolvedBorderColor = borderColor ?: if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.86f)
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(resolvedContainerColor, shape)
            .background(resolvedOverlayColor, shape)
            .border(width = 1.dp, color = resolvedBorderColor, shape = shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.metricCardHorizontalPadding,
                    vertical = CardLayoutRhythm.metricCardVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricCardTextGap)
        ) {
            top.yukonga.miuix.kmp.basic.Text(
                text = label,
                color = labelColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            top.yukonga.miuix.kmp.basic.Text(
                text = value.ifBlank { "N/A" },
                color = valueColor,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = if (emphasizedValue) AppTypographyTokens.BodyEmphasis.fontWeight else AppTypographyTokens.Body.fontWeight,
                maxLines = valueMaxLines,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppOverviewInlineMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    containerColor: Color? = null,
    borderColor: Color? = null,
    labelMaxLines: Int = 2,
    valueMaxLines: Int = 2,
    labelWeight: Float = 0.58f,
    valueWeight: Float = 0.42f,
    emphasizedValue: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val resolvedContainerColor = containerColor ?: if (isDark) {
        Color(0xFF0F1115).copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.58f)
    }
    val resolvedOverlayColor = if (containerColor != null) {
        Color.Transparent
    } else if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color(0xFFDCEBFF).copy(alpha = 0.22f)
    }
    val resolvedBorderColor = borderColor ?: if (isDark) {
        Color.White.copy(alpha = 0.17f)
    } else {
        Color.White.copy(alpha = 0.84f)
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(resolvedContainerColor, shape)
            .background(resolvedOverlayColor, shape)
            .border(width = 1.dp, color = resolvedBorderColor, shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.metricCardHorizontalPadding,
                    vertical = CardLayoutRhythm.metricCardVerticalPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            top.yukonga.miuix.kmp.basic.Text(
                text = label,
                color = labelColor,
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
                modifier = Modifier.weight(labelWeight),
                maxLines = labelMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            top.yukonga.miuix.kmp.basic.Text(
                text = value.ifBlank { "N/A" },
                color = valueColor,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = if (emphasizedValue) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(valueWeight),
                maxLines = valueMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Overview Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun AppOverviewCardPreviewLight() {
    CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
        MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
            AppOverviewCard(
                title = "GitHub Tracking",
                subtitle = "Tap to refresh, long-press to add",
                containerColor = Color(0xFFEFF6FF),
                borderColor = Color(0xFF93C5FD),
                headerEndActions = {
                    StatusPill(
                        label = "3m ago",
                        color = Color(0xFF2563EB)
                    )
                    StatusPill(
                        label = "Checked",
                        color = Color(0xFF22C55E)
                    )
                }
            ) {
                AppInfoRow(label = "Tracked", value = "18")
                AppInfoRow(label = "Updates", value = "4", valueColor = Color(0xFF2563EB))
                AppInfoRow(label = "Pre-release", value = "2", valueColor = Color(0xFFF59E0B))
            }
        }
    }
}

@Preview(name = "Overview Dark", showBackground = true, backgroundColor = 0xFF111827)
@Composable
private fun AppOverviewCardPreviewDark() {
    CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
        MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
            AppOverviewCard(
                title = "System Properties",
                subtitle = "Tap to refresh system tables",
                containerColor = Color(0xFF1F2937),
                borderColor = Color(0xFF334155),
                titleColor = Color.White,
                subtitleColor = Color(0xFFCBD5E1),
                headerEndActions = {
                    StatusPill(
                        label = "Cached",
                        color = Color(0xFFF59E0B)
                    )
                }
            ) {
                AppInfoRow(label = "System", value = "82 items", labelColor = Color(0xFFCBD5E1), valueColor = Color.White)
                AppInfoRow(label = "Android", value = "31 items", labelColor = Color(0xFFCBD5E1), valueColor = Color.White)
                AppInfoRow(label = "Java", value = "16 items", labelColor = Color(0xFFCBD5E1), valueColor = Color.White)
            }
        }
    }
}
