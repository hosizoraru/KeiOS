package com.example.keios.ui.page.main

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.AppInfoRow
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class OsOverviewMetric(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

internal fun sectionCardIcon(card: OsSectionCard) = when (card) {
    OsSectionCard.TOP_INFO -> MiuixIcons.Regular.Info
    OsSectionCard.SHELL_RUNNER -> MiuixIcons.Regular.Tasks
    OsSectionCard.GOOGLE_SYSTEM_SERVICE -> MiuixIcons.Regular.Update
    OsSectionCard.SYSTEM -> MiuixIcons.Regular.ListView
    OsSectionCard.SECURE -> MiuixIcons.Regular.Lock
    OsSectionCard.GLOBAL -> MiuixIcons.Regular.Layers
    OsSectionCard.ANDROID -> MiuixIcons.Regular.GridView
    OsSectionCard.JAVA -> MiuixIcons.Regular.Tune
    OsSectionCard.LINUX -> MiuixIcons.Regular.Filter
}

@Composable
internal fun OsSectionHeaderIcon(card: OsSectionCard, modifier: Modifier = Modifier) {
    Icon(
        imageVector = sectionCardIcon(card),
        contentDescription = card.title,
        tint = MiuixTheme.colorScheme.primary,
        modifier = modifier
            .size(22.dp)
            .defaultMinSize(minHeight = 22.dp)
    )
}

@Composable
internal fun OsSectionInfoRow(
    label: String,
    value: String,
    copyValueOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayValue = value.ifBlank { "N/A" }
    AppInfoRow(
        label = label,
        value = displayValue,
        modifier = modifier,
        labelColor = MiuixTheme.colorScheme.onBackgroundVariant,
        valueColor = MiuixTheme.colorScheme.onBackground,
        labelMinWidth = 72.dp,
        labelMaxWidth = 136.dp,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.infoRowVerticalPadding,
        valueTextAlign = TextAlign.End,
        labelMaxLines = Int.MAX_VALUE,
        valueMaxLines = 6,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = AppTypographyTokens.Body.fontSize,
        labelLineHeight = AppTypographyTokens.Body.lineHeight,
        valueFontSize = AppTypographyTokens.Body.fontSize,
        valueLineHeight = AppTypographyTokens.Body.lineHeight,
        emphasizedValue = true,
        copyPayloadOverride = if (copyValueOnly) displayValue else null
    )
}

internal data class OsActivityVisibilityItem(
    val id: String,
    val title: String,
    val packageName: String,
    val className: String,
    val builtInSample: Boolean,
    val visible: Boolean
)
