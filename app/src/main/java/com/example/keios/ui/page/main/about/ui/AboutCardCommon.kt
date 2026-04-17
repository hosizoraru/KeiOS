package com.example.keios.ui.page.main.about.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow.Companion.Clip
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppCardBodyColumn
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.AppCardHeader
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.appExpandIn
import com.example.keios.ui.page.main.widget.appExpandOut
import com.example.keios.ui.page.main.widget.buildTextCopyPayload
import com.example.keios.ui.page.main.widget.rememberLightTextCopyAction
import com.example.keios.ui.page.main.widget.rememberTextCopyExpandedEnabled
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutCompactRow(
    title: String,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    valueContent: @Composable RowScope.() -> Unit
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = CardLayoutRhythm.compactSectionGap)
    ) {
        val maxTitleWidth = (maxWidth * 0.44f).coerceAtLeast(96.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap)
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
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = Int.MAX_VALUE,
                    overflow = Clip
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
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
    onClick: (() -> Unit)? = null,
    enableLongPressCopy: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    val displayValue = value.ifBlank { stringResource(R.string.common_na) }
    val copyPayload = remember(title, displayValue) {
        buildTextCopyPayload(title, displayValue)
    }
    val quickCopyAction = rememberLightTextCopyAction(copyPayload)
    val expandedCopyMode = rememberTextCopyExpandedEnabled()
    AboutCompactRow(
        title = title,
        modifier = modifier,
        titleIcon = titleIcon,
        onClick = onClick,
        onLongClick = onLongClick ?: if (enableLongPressCopy && !expandedCopyMode) quickCopyAction else null
    ) {
        CopyModeSelectionContainer {
            Text(
                text = displayValue,
                color = valueColor,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                maxLines = Int.MAX_VALUE,
                overflow = Clip,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
        ) {
            AppCardHeader(
                title = title,
                subtitle = subtitle,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                startAction = if (sectionIcon != null) {
                    {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = titleColor
                        )
                    }
                } else {
                    null
                },
                expandable = collapsible,
                expanded = expanded,
                expandTint = titleColor,
                onClick = if (collapsible) {
                    { onExpandedChange(!expanded) }
                } else {
                    null
                }
            )
            AnimatedVisibility(
                visible = !collapsible || expanded,
                enter = appExpandIn(),
                exit = appExpandOut()
            ) {
                AppCardBodyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = CardLayoutRhythm.cardHorizontalPadding,
                        end = CardLayoutRhythm.cardHorizontalPadding,
                        bottom = CardLayoutRhythm.cardVerticalPadding
                    ),
                    verticalSpacing = CardLayoutRhythm.sectionGap,
                    content = content
                )
            }
        }
    }
}
