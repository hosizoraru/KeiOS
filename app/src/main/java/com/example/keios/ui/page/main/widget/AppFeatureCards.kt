package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(CardLayoutRhythm.cardCornerRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(containerColor, shape),
        colors = CardDefaults.defaultColors(
            color = containerColor,
            contentColor = contentColor
        ),
        insideMargin = PaddingValues(0.dp),
        showIndication = showIndication && (onClick != null || onLongClick != null),
        onClick = onClick,
        onLongPress = onLongClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun AppFeatureCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    eyebrowColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
    borderColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
    contentColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = contentColor,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
    sectionIcon: ImageVector? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headerEndActions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = CardLayoutRhythm.cardHorizontalPadding,
        end = CardLayoutRhythm.cardHorizontalPadding,
        bottom = CardLayoutRhythm.cardVerticalPadding
    ),
    contentVerticalSpacing: Dp = CardLayoutRhythm.sectionGap,
    content: @Composable ColumnScope.() -> Unit
) {
    val headerClick = when {
        collapsible -> ({ onExpandedChange(!expanded) })
        onClick != null -> onClick
        else -> null
    }
    AppSurfaceCard(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = borderColor,
        contentColor = contentColor,
        showIndication = showIndication,
        onClick = if (collapsible) null else onClick,
        onLongClick = onLongClick
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            eyebrow = eyebrow,
            eyebrowColor = eyebrowColor,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            startAction = sectionIcon?.let { icon ->
                {
                    top.yukonga.miuix.kmp.basic.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = titleColor
                    )
                }
            },
            endActions = headerEndActions,
            expandable = collapsible,
            expanded = expanded,
            expandTint = titleColor,
            onClick = headerClick,
            onLongClick = onLongClick
        )
        AnimatedVisibility(
            visible = !collapsible || expanded,
            enter = appExpandIn(),
            exit = appExpandOut()
        ) {
            AppCardBodyColumn(
                contentPadding = contentPadding,
                verticalSpacing = contentVerticalSpacing,
                content = content
            )
        }
    }
}
