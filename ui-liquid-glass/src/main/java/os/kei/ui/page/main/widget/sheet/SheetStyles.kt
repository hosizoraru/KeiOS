package os.kei.ui.page.main.widget.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import os.kei.ui.liquidglass.R
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppCardBodyColumn
import os.kei.ui.page.main.widget.core.AppCardHeader
import os.kei.ui.page.main.widget.core.AppControlRow
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val DefaultSelectedLabelSentinel = "\u0000default-selected-label"
private const val DefaultCollapsedHintSentinel = "\u0000default-collapsed-hint"
private const val DefaultExpandedHintSentinel = "\u0000default-expanded-hint"

enum class SheetCardSurfaceTone {
    Default,
    Readable,
}

enum class SheetChoiceCardDensity {
    Standard,
    Compact,
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
fun SheetSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    danger: Boolean = false,
    summaryMaxLines: Int = Int.MAX_VALUE,
    summaryOverflow: TextOverflow = TextOverflow.Clip,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SheetSectionTitle(
            text = text,
            danger = danger,
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = summaryMaxLines,
                overflow = summaryOverflow,
            )
        }
    }
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
    containerColor: Color? = null,
    borderColor: Color? = null,
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    surfaceTone: SheetCardSurfaceTone = SheetCardSurfaceTone.Default,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    pressSafePadding: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    selected: Boolean? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isAppInDarkTheme()
    val resolvedContainerColor = containerColor ?: sheetCardSurfaceColor(surfaceTone, isDark)
    val optics = sheetCardOptics(interactive = onClick != null)
    AppSurfaceCard(
        modifier = Modifier.cullOutsideLiquidSheetViewport().then(modifier),
        exportBackdropToContent = true,
        containerColor = resolvedContainerColor,
        borderColor = borderColor ?: sheetCardBorderColor(surfaceTone),
        borderWidth = optics.borderWidth,
        contentColor = contentColor,
        enabled = enabled,
        depthEffect = optics.depthEffect,
        highlightAlpha = optics.highlightAlpha,
        pressSafePadding = pressSafePadding,
        onClick = onClick,
        role = role,
        selected = selected,
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
    containerColor: Color? = null,
    borderColor: Color? = null,
    surfaceTone: SheetCardSurfaceTone = SheetCardSurfaceTone.Default,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurfaceCard(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = borderColor,
        surfaceTone = surfaceTone,
        verticalSpacing = verticalSpacing,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
private fun sheetCardSurfaceColor(
    surfaceTone: SheetCardSurfaceTone,
    isDark: Boolean,
): Color {
    val alpha =
        when (surfaceTone) {
            SheetCardSurfaceTone.Default -> 0.64f
            SheetCardSurfaceTone.Readable -> if (isDark) 0.86f else 0.92f
        }
    return MiuixTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
}

@Composable
private fun sheetCardBorderColor(
    surfaceTone: SheetCardSurfaceTone,
): Color {
    val alpha =
        when (surfaceTone) {
            SheetCardSurfaceTone.Default -> 0.14f
            SheetCardSurfaceTone.Readable -> 0.22f
        }
    return MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = alpha)
}

@Composable
fun SheetControlRow(
    label: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
    labelColor: Color = MiuixTheme.colorScheme.onBackground,
    minHeight: Dp = AppInteractiveTokens.compactControlRowMinHeight,
    trailing: @Composable RowScope.() -> Unit,
) {
    AppControlRow(
        title = label,
        modifier = Modifier.cullOutsideLiquidSheetViewport().then(modifier),
        summary = summary,
        titleColor = labelColor,
        summaryColor = summaryColor,
        minHeight = minHeight,
        trailing = trailing
    )
}

@Composable
fun SheetControlRow(
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
    minHeight: Dp = AppInteractiveTokens.compactControlRowMinHeight,
    labelContent: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    AppControlRow(
        modifier = Modifier.cullOutsideLiquidSheetViewport().then(modifier),
        summary = summary,
        summaryColor = summaryColor,
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
    badgeContentPadding: PaddingValues? = null,
    containerColor: Color? = null,
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    verticalSpacing: Dp = 8.dp,
    titleMaxLines: Int = 1,
    titleOverflow: TextOverflow = TextOverflow.Ellipsis,
    titleFontSize: TextUnit = AppTypographyTokens.CardHeader.fontSize,
    titleLineHeight: TextUnit = AppTypographyTokens.CardHeader.lineHeight,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetSurfaceCard(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = borderColor,
        verticalSpacing = verticalSpacing,
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
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    maxLines = titleMaxLines,
                    overflow = titleOverflow,
                    modifier = Modifier.weight(1f)
                )
                badgeLabel?.let { label ->
                    StatusPill(
                        label = label,
                        color = badgeColor,
                        contentPadding = badgeContentPadding
                    )
                }
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
    enabled: Boolean = true,
    pressSafePadding: Dp = AppInteractiveTokens.liquidPressSafePadding,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    selectedAccentColor: Color = accentColor,
    unselectedTitleColor: Color = MiuixTheme.colorScheme.onBackground,
    summaryColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    selectedLabel: String? = DefaultSelectedLabelSentinel,
    leading: (@Composable () -> Unit)? = null,
    density: SheetChoiceCardDensity = SheetChoiceCardDensity.Standard,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    containerColor: Color? = null,
    borderColor: Color? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showIndicator: Boolean = true,
    details: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val compact = density == SheetChoiceCardDensity.Compact
    val resolvedSelectedLabel = when (selectedLabel) {
        DefaultSelectedLabelSentinel -> stringResource(R.string.common_selected)
        else -> selectedLabel
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(pressSafePadding)
    ) {
        SheetSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor =
                containerColor ?: if (selected) {
                    selectedAccentColor.copy(alpha = 0.12f)
                } else {
                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.68f)
                },
            borderColor =
                borderColor ?: if (selected) {
                    selectedAccentColor.copy(alpha = 0.32f)
                } else {
                    MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f)
                },
            pressSafePadding = 0.dp,
            enabled = enabled,
            onClick = onSelect,
            role = Role.RadioButton,
            selected = selected,
            contentPadding = contentPadding,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compact) 40.dp else Dp.Unspecified),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leading?.invoke()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = if (selected) selectedAccentColor else unselectedTitleColor,
                            fontWeight =
                                if (compact) {
                                    if (selected) FontWeight.Bold else AppTypographyTokens.BodyEmphasis.fontWeight
                                } else {
                                    AppTypographyTokens.CardHeader.fontWeight
                                },
                            fontSize =
                                if (compact) {
                                    AppTypographyTokens.Body.fontSize
                                } else {
                                    AppTypographyTokens.CardHeader.fontSize
                                },
                            lineHeight =
                                if (compact) {
                                    AppTypographyTokens.Body.lineHeight
                                } else {
                                    AppTypographyTokens.CardHeader.lineHeight
                                },
                            maxLines = if (compact) 1 else Int.MAX_VALUE,
                            overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
                            modifier = if (compact) Modifier.weight(1f, fill = false) else Modifier,
                        )
                        if (selected && !resolvedSelectedLabel.isNullOrBlank()) {
                            StatusPill(
                                label = resolvedSelectedLabel,
                                color = selectedAccentColor,
                                size =
                                    if (compact) {
                                        AppStatusPillSize.Compact
                                    } else {
                                        AppStatusPillSize.Default
                                    },
                            )
                        }
                    }
                    Text(
                        text = summary,
                        color = summaryColor,
                        fontSize =
                            if (compact) {
                                AppTypographyTokens.Supporting.fontSize
                            } else {
                                AppTypographyTokens.Body.fontSize
                            },
                        lineHeight =
                            if (compact) {
                                AppTypographyTokens.Supporting.lineHeight
                            } else {
                                AppTypographyTokens.Body.lineHeight
                            },
                        maxLines = if (compact) 1 else Int.MAX_VALUE,
                        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
                    )
                    details?.invoke(this)
                }
                trailing?.invoke(this)
                if (showIndicator) {
                    SheetLiquidChoiceIndicator(
                        selected = selected,
                        onSelect = null,
                        accentColor = selectedAccentColor
                    )
                }
            }
        }
    }
}

@Composable
fun SheetLiquidChoiceIndicator(
    selected: Boolean,
    onSelect: (() -> Unit)?,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val isDark = isAppInDarkTheme()
    val activeBackdrop = resolvedSheetChoiceIndicatorBackdrop()
    val shape = Capsule()
    val interactionSource = remember { MutableInteractionSource() }
    val surfaceColor = when {
        selected && isDark -> accentColor.copy(alpha = 0.18f)
        selected -> accentColor.copy(alpha = 0.14f)
        isDark -> MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.26f)
        else -> MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.44f)
    }
    val idleDotColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.62f else 0.48f)

    val interactionModifier =
        if (onSelect != null) {
            Modifier
                .size(48.dp)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onSelect,
                )
        } else {
            Modifier.size(width = 44.dp, height = 30.dp)
        }
    Box(
        modifier = modifier.then(interactionModifier),
        contentAlignment = Alignment.Center
    ) {
        LiquidSurface(
            backdrop = activeBackdrop,
            modifier = Modifier.size(width = 44.dp, height = 30.dp),
            shape = shape,
            enabled = enabled,
            isInteractive = false,
            tint = Color.Unspecified,
            surfaceColor = surfaceColor,
            blurRadius = if (selected) 6.dp else 4.dp,
            lensRadius = if (selected) 16.dp else 12.dp,
            effectVariant = GlassVariant.Compact,
            chromaticAberration = selected,
            depthEffect = selected,
            shadow = false,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = MiuixIcons.Basic.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .appSquircleBackground(idleDotColor, 999.dp)
                    )
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
internal fun resolvedSheetChoiceIndicatorBackdrop(): Backdrop? =
    activeGlassBackdrop(LocalLiquidParentBackdrop.current)

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
