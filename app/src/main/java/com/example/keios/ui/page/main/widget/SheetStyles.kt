package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SheetContentColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    verticalSpacing: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val scrollModifier = if (scrollable) {
        Modifier.verticalScroll(rememberScrollState())
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        content()
    }
}

@Composable
fun SheetRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun SheetInputTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f),
        fontSize = AppTypographyTokens.Body.fontSize,
        lineHeight = AppTypographyTokens.Body.lineHeight,
        modifier = modifier
    )
}

@Composable
fun SheetSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    Text(
        text = text,
        color = if (danger) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onBackground,
        fontSize = AppTypographyTokens.CardHeader.fontSize,
        lineHeight = AppTypographyTokens.CardHeader.lineHeight,
        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
        modifier = modifier
    )
}

@Composable
fun SheetDescriptionText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    AppSupportingBlock(
        text = text,
        modifier = modifier.fillMaxWidth(),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun SheetSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.56f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            ),
        colors = CardDefaults.defaultColors(
            color = containerColor,
            contentColor = contentColor
        ),
        showIndication = onClick != null,
        onClick = onClick ?: {}
    ) {
        AppCardBodyColumn(
            contentPadding = contentPadding,
            verticalSpacing = verticalSpacing,
            content = content
        )
    }
}

@Composable
fun SheetSectionCard(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurfaceCard(
        modifier = modifier,
        verticalSpacing = verticalSpacing,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun SheetControlRow(
    label: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    labelColor: Color = MiuixTheme.colorScheme.onBackground,
    minHeight: Dp = 40.dp,
    trailing: @Composable RowScope.() -> Unit,
) {
    AppControlRow(
        title = label,
        modifier = modifier,
        summary = summary,
        titleColor = labelColor,
        minHeight = minHeight,
        trailing = trailing
    )
}

@Composable
fun SheetControlRow(
    modifier: Modifier = Modifier,
    summary: String? = null,
    minHeight: Dp = 40.dp,
    labelContent: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    AppControlRow(
        modifier = modifier,
        summary = summary,
        minHeight = minHeight,
        titleContent = labelContent,
        trailing = trailing
    )
}

@Composable
fun SheetActionGroup(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

@Composable
fun SheetFieldBlock(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (trailing != null || !summary.isNullOrBlank()) {
            SheetControlRow(
                label = title,
                summary = summary,
                trailing = { trailing?.invoke(this) }
            )
        } else {
            SheetInputTitle(title)
        }
        content()
    }
}

@Composable
fun SheetSummaryCard(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    badgeLabel: String? = null,
    badgeColor: Color = accentColor,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurfaceCard(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = borderColor,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = accentColor,
                    fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                    fontSize = AppTypographyTokens.CardHeader.fontSize,
                    lineHeight = AppTypographyTokens.CardHeader.lineHeight
                )
                badgeLabel?.let { label ->
                    StatusPill(
                        label = label,
                        color = badgeColor
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                headerTrailing?.invoke(this)
            }
            content()
        }
    )
}

@Composable
fun SheetChoiceCard(
    title: String,
    summary: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    selectedLabel: String = "已选择",
    details: (@Composable ColumnScope.() -> Unit)? = null,
) {
    SheetSurfaceCard(
        modifier = modifier,
        containerColor = if (selected) {
            accentColor.copy(alpha = 0.12f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.68f)
        },
        borderColor = if (selected) {
            accentColor.copy(alpha = 0.32f)
        } else {
            MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f)
        },
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = if (selected) accentColor else MiuixTheme.colorScheme.onBackground,
                        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                        fontSize = AppTypographyTokens.CardHeader.fontSize,
                        lineHeight = AppTypographyTokens.CardHeader.lineHeight
                    )
                    if (selected) {
                        StatusPill(
                            label = selectedLabel,
                            color = accentColor
                        )
                    }
                }
                Text(
                    text = summary,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight
                )
                details?.invoke(this)
            }
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
        }
    }
}

@Composable
fun SheetExpandableCard(
    title: String,
    collapsedSummary: String,
    expandedSummary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    badgeLabel: String? = null,
    collapsedHint: String = "点按展开详细说明",
    expandedHint: String = "点按收起详细说明",
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurfaceCard(
        modifier = modifier,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (expanded) 0.78f else 0.68f),
        borderColor = if (expanded) {
            accentColor.copy(alpha = 0.5f)
        } else {
            MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.22f)
        },
        contentPadding = PaddingValues(0.dp)
    ) {
        AppCardHeader(
            title = title,
            subtitle = if (expanded) expandedSummary else collapsedSummary,
            titleColor = accentColor,
            subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
            supportingText = if (expanded) expandedHint else collapsedHint,
            supportingColor = accentColor,
            titleAccessory = if (badgeLabel != null) {
                {
                    StatusPill(
                        label = badgeLabel,
                        color = accentColor
                    )
                }
            } else {
                null
            },
            endActions = {
                StatusPill(
                    label = if (expanded) "收起" else "展开",
                    color = accentColor
                )
            },
            expandable = true,
            expanded = expanded,
            expandTint = accentColor,
            subtitleMaxLines = if (expanded) 3 else 2,
            onClick = { onExpandedChange(!expanded) }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CardLayoutRhythm.cardHorizontalPadding,
                        end = CardLayoutRhythm.cardHorizontalPadding,
                        bottom = CardLayoutRhythm.cardVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap),
                content = content
            )
        }
    }
}
