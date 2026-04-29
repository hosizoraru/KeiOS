package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import os.kei.R
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import os.kei.ui.page.main.widget.support.buildTextCopyPayload
import os.kei.ui.page.main.widget.support.copyModeAwareRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Shared key-value row style for data-like surfaces (Settings / OS / MCP / GitHub).
 * Keeps typography and spacing consistent while allowing lightweight per-page tuning.
 */
@Composable
fun AppInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    labelMinWidth: Dp = Dp.Unspecified,
    labelMaxWidth: Dp = Dp.Unspecified,
    labelWeight: Float? = null,
    valueWeight: Float = 1f,
    valueMinWidth: Dp = Dp.Unspecified,
    horizontalSpacing: Dp = CardLayoutRhythm.infoRowGap,
    rowVerticalPadding: Dp = CardLayoutRhythm.infoRowVerticalPadding,
    valueTextAlign: TextAlign = TextAlign.End,
    labelMaxLines: Int = Int.MAX_VALUE,
    valueMaxLines: Int = Int.MAX_VALUE,
    labelOverflow: TextOverflow = TextOverflow.Clip,
    valueOverflow: TextOverflow = TextOverflow.Clip,
    labelFontSize: TextUnit = AppTypographyTokens.Supporting.fontSize,
    labelLineHeight: TextUnit = AppTypographyTokens.Supporting.lineHeight,
    valueFontSize: TextUnit = AppTypographyTokens.Body.fontSize,
    valueLineHeight: TextUnit = AppTypographyTokens.Body.lineHeight,
    emphasizedValue: Boolean = true,
    copyPayloadOverride: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val displayLabel = label.ifBlank { stringResource(R.string.common_info) }
    val displayValue = value.ifBlank { stringResource(R.string.common_na) }
    val copyPayload = remember(displayLabel, displayValue, copyPayloadOverride) {
        copyPayloadOverride?.takeIf { it.isNotBlank() }
            ?: buildTextCopyPayload(displayLabel, displayValue)
    }
    val rowModifier = modifier
        .fillMaxWidth()
        .copyModeAwareRow(
            copyPayload = copyPayload,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .padding(vertical = rowVerticalPadding)

    CopyModeSelectionContainer {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelModifier = when {
                labelWeight != null -> Modifier.weight(labelWeight)
                labelMinWidth != Dp.Unspecified || labelMaxWidth != Dp.Unspecified -> {
                    Modifier.widthIn(min = labelMinWidth, max = labelMaxWidth)
                }
                else -> Modifier.wrapContentWidth()
            }
            val baseValueModifier = if (valueWeight > 0f) {
                Modifier.weight(valueWeight)
            } else {
                Modifier.wrapContentWidth()
            }
            val valueModifier = if (valueMinWidth != Dp.Unspecified) {
                baseValueModifier.widthIn(min = valueMinWidth)
            } else {
                baseValueModifier
            }
            Text(
                text = displayLabel,
                color = labelColor,
                fontSize = labelFontSize,
                lineHeight = labelLineHeight,
                modifier = labelModifier,
                maxLines = labelMaxLines,
                overflow = labelOverflow
            )
            Text(
                text = displayValue,
                color = valueColor,
                fontSize = valueFontSize,
                lineHeight = valueLineHeight,
                fontWeight = if (emphasizedValue) FontWeight.Medium else FontWeight.Normal,
                textAlign = valueTextAlign,
                modifier = valueModifier,
                maxLines = valueMaxLines,
                overflow = valueOverflow
            )
        }
    }
}
