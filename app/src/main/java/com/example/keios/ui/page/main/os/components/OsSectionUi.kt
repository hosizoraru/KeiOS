package com.example.keios.ui.page.main.os.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.os.OsSectionCard
import com.example.keios.ui.page.main.os.appLucideAppWindowIcon
import com.example.keios.ui.page.main.os.appLucideConfigIcon
import com.example.keios.ui.page.main.os.appLucideFilterIcon
import com.example.keios.ui.page.main.os.appLucideInfoIcon
import com.example.keios.ui.page.main.os.appLucideLayersIcon
import com.example.keios.ui.page.main.os.appLucideListIcon
import com.example.keios.ui.page.main.os.appLucideLockIcon
import com.example.keios.ui.page.main.os.appLucidePackageIcon
import com.example.keios.ui.page.main.os.osLucideConsoleIcon
import com.example.keios.ui.page.main.widget.core.AppInfoRow
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.core.CardLayoutRhythm
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class OsOverviewMetric(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

@Composable
internal fun sectionCardIcon(card: OsSectionCard): ImageVector = when (card) {
    OsSectionCard.TOP_INFO -> appLucideInfoIcon()
    OsSectionCard.SHELL_RUNNER -> osLucideConsoleIcon()
    OsSectionCard.GOOGLE_SYSTEM_SERVICE -> appLucidePackageIcon()
    OsSectionCard.SYSTEM -> appLucideListIcon()
    OsSectionCard.SECURE -> appLucideLockIcon()
    OsSectionCard.GLOBAL -> appLucideLayersIcon()
    OsSectionCard.ANDROID -> appLucideAppWindowIcon()
    OsSectionCard.JAVA -> appLucideConfigIcon()
    OsSectionCard.LINUX -> appLucideFilterIcon()
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
    valueSingleLine: Boolean = false,
    valueMarquee: Boolean = false,
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
        valueMaxLines = if (valueSingleLine) 1 else 6,
        valueOverflow = if (valueSingleLine) TextOverflow.Clip else TextOverflow.Ellipsis,
        labelFontSize = AppTypographyTokens.Body.fontSize,
        labelLineHeight = AppTypographyTokens.Body.lineHeight,
        valueFontSize = AppTypographyTokens.Body.fontSize,
        valueLineHeight = AppTypographyTokens.Body.lineHeight,
        valueMarquee = valueMarquee,
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
