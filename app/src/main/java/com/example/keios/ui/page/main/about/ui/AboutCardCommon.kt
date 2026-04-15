package com.example.keios.ui.page.main.about.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow.Companion.Clip
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.StatusPill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutCompactRow(
    title: String,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    valueContent: @Composable RowScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 4.dp)
    ) {
        val maxTitleWidth = (maxWidth * 0.44f).coerceAtLeast(96.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.widthIn(min = 70.dp, max = maxTitleWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (titleIcon != null) {
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = Int.MAX_VALUE,
                    overflow = Clip
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
                content = valueContent
            )
        }
    }
}

@Composable
fun AboutCompactInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    onClick: (() -> Unit)? = null
) {
    AboutCompactRow(
        title = title,
        modifier = modifier,
        titleIcon = titleIcon,
        onClick = onClick
    ) {
        Text(
            text = value,
            color = valueColor,
            maxLines = Int.MAX_VALUE,
            overflow = Clip,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun AboutCompactPillRow(
    title: String,
    label: String,
    color: Color,
    titleIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    AboutCompactRow(
        title = title,
        titleIcon = titleIcon,
        onClick = onClick
    ) {
        StatusPill(
            label = label,
            color = color
        )
    }
}

@Composable
fun AboutSectionCard(
    cardColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color,
    subtitleColor: Color,
    sectionIcon: ImageVector? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {
            if (collapsible) onExpandedChange(!expanded)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sectionIcon != null) {
                        Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = titleColor
                        )
                    }
                    Text(
                        text = title,
                        color = titleColor
                    )
                }
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) MiuixIcons.Regular.ExpandLess else MiuixIcons.Regular.ExpandMore,
                        contentDescription = if (expanded) {
                            stringResource(R.string.about_action_collapse)
                        } else {
                            stringResource(R.string.about_action_expand)
                        },
                        tint = titleColor
                    )
                }
            }
            Text(
                text = subtitle,
                color = subtitleColor
            )
            if (!collapsible || expanded) {
                content()
            }
        }
    }
}
