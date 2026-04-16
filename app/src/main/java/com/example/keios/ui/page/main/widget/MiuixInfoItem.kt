package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixInfoItem(
    key: String,
    value: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    valueColor: Color? = null
) {
    val clickableModifier = if (onClick != null || onLongClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.weight(0.42f),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip
        )
        Text(
            text = value.ifBlank { "N/A" },
            color = valueColor ?: MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.weight(0.58f),
            textAlign = TextAlign.End,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            fontWeight = FontWeight.Medium
        )
    }
}
