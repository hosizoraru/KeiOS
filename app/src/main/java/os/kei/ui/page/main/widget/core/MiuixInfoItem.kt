package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixInfoItem(
    key: String,
    value: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    valueColor: Color? = null
) {
    val displayKey = key.ifBlank { stringResource(R.string.common_info) }
    val displayValue = value.ifBlank { stringResource(R.string.common_na) }
    AppInfoRow(
        label = displayKey,
        value = displayValue,
        labelColor = MiuixTheme.colorScheme.onBackgroundVariant,
        valueColor = valueColor ?: MiuixTheme.colorScheme.onBackground,
        labelWeight = 0.42f,
        valueWeight = 0.58f,
        horizontalSpacing = 10.dp,
        rowVerticalPadding = 4.dp,
        valueTextAlign = TextAlign.End,
        labelMaxLines = Int.MAX_VALUE,
        valueMaxLines = Int.MAX_VALUE,
        labelOverflow = TextOverflow.Clip,
        valueOverflow = TextOverflow.Clip,
        onClick = onClick,
        onLongClick = onLongClick
    )
}
