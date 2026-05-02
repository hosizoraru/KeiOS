package os.kei.ui.page.main.github

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppOverviewInlineMetricTile
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubCompactInfoRow(
    label: String,
    value: String,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    emphasized: Boolean = false,
    titleMinWidth: Dp = 56.dp,
    onClick: (() -> Unit)? = null
) {
    AppInfoRow(
        label = label,
        value = value,
        labelColor = titleColor,
        valueColor = valueColor,
        labelMinWidth = titleMinWidth,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.metricCardTextGap,
        valueTextAlign = TextAlign.End,
        valueMaxLines = 4,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = AppTypographyTokens.Supporting.fontSize,
        labelLineHeight = AppTypographyTokens.Supporting.lineHeight,
        valueFontSize = AppTypographyTokens.Body.fontSize,
        valueLineHeight = AppTypographyTokens.Body.lineHeight,
        emphasizedValue = emphasized,
        onClick = onClick
    )
}

@Composable
internal fun VersionValueRow(
    label: String,
    value: String,
    valueColor: Color,
    emphasized: Boolean = false
) {
    GitHubCompactInfoRow(
        label = label,
        value = value,
        valueColor = valueColor,
        titleColor = MiuixTheme.colorScheme.primary,
        emphasized = emphasized,
        titleMinWidth = 52.dp
    )
}

@Composable
internal fun GitHubOverviewMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    emphasized: Boolean = false,
    labelMaxLines: Int = 2,
    valueMaxLines: Int = 2,
    labelWeight: Float = 0.58f,
    valueWeight: Float = 0.42f,
    backdrop: Backdrop? = null
) {
    AppOverviewInlineMetricTile(
        label = label,
        value = value.ifBlank { stringResource(R.string.common_na) },
        modifier = modifier,
        labelColor = titleColor,
        valueColor = valueColor,
        labelMaxLines = labelMaxLines,
        valueMaxLines = valueMaxLines,
        labelWeight = labelWeight,
        valueWeight = valueWeight,
        backdrop = backdrop,
        emphasizedValue = emphasized
    )
}
