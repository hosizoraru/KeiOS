package os.kei.ui.page.main.widget.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppCardBodyColumn
import os.kei.ui.page.main.widget.core.AppCardHeader
import os.kei.ui.page.main.widget.core.AppControlRow
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val DefaultSelectedLabelSentinel = "\u0000default-selected-label"
private const val DefaultCollapsedHintSentinel = "\u0000default-collapsed-hint"
private const val DefaultExpandedHintSentinel = "\u0000default-expanded-hint"

@Composable
fun SheetContentColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    verticalSpacing: Dp = AppChromeTokens.pageSectionGapLarge,
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
        color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f),
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
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
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor, shape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .then(clickModifier)
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
    minHeight: Dp = AppInteractiveTokens.compactControlRowMinHeight,
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
    minHeight: Dp = AppInteractiveTokens.compactControlRowMinHeight,
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
    verticalSpacing: Dp = AppChromeTokens.pageSectionGap,
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
        verticalArrangement = Arrangement.spacedBy(AppChromeTokens.pageSectionGap)
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
    selectedAccentColor: Color = accentColor,
    unselectedTitleColor: Color = MiuixTheme.colorScheme.onBackground,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    selectedLabel: String? = DefaultSelectedLabelSentinel,
    leading: (@Composable () -> Unit)? = null,
    details: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val resolvedSelectedLabel = when (selectedLabel) {
        DefaultSelectedLabelSentinel -> stringResource(R.string.common_selected)
        else -> selectedLabel
    }
    SheetSurfaceCard(
        modifier = modifier,
        containerColor = if (selected) {
            selectedAccentColor.copy(alpha = 0.12f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.68f)
        },
        borderColor = if (selected) {
            selectedAccentColor.copy(alpha = 0.32f)
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
            leading?.invoke()
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
                        color = if (selected) selectedAccentColor else unselectedTitleColor,
                        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                        fontSize = AppTypographyTokens.CardHeader.fontSize,
                        lineHeight = AppTypographyTokens.CardHeader.lineHeight
                    )
                    if (selected && !resolvedSelectedLabel.isNullOrBlank()) {
                        StatusPill(
                            label = resolvedSelectedLabel,
                            color = selectedAccentColor
                        )
                    }
                }
                Text(
                    text = summary,
                    color = summaryColor,
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
    collapsedHint: String? = DefaultCollapsedHintSentinel,
    expandedHint: String? = DefaultExpandedHintSentinel,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedCollapsedHint = when (collapsedHint) {
        DefaultCollapsedHintSentinel -> stringResource(R.string.common_expand_details_hint)
        else -> collapsedHint
    }
    val resolvedExpandedHint = when (expandedHint) {
        DefaultExpandedHintSentinel -> stringResource(R.string.common_collapse_details_hint)
        else -> expandedHint
    }
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
            supportingText = if (expanded) resolvedExpandedHint else resolvedCollapsedHint,
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
                    label = stringResource(if (expanded) R.string.common_collapse else R.string.common_expand),
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
            enter = appExpandIn(),
            exit = appExpandOut()
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
