package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixExpandableSection(
    backdrop: Backdrop?,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    headerStartAction: (@Composable () -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    onHeaderLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val sectionSurface = containerColor ?: MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    val shouldDrawSurface = sectionSurface.alpha > 0f
    val shadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.20f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(16.dp) },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = 1f) },
                        shadow = { Shadow.Default.copy(color = shadowColor) },
                        onDrawSurface = {
                            if (shouldDrawSurface) drawRect(sectionSurface)
                        }
                    )
                } else {
                    if (shouldDrawSurface) Modifier.background(sectionSurface) else Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onExpandedChange(!expanded) },
                    onLongClick = { onHeaderLongClick?.invoke() }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            headerStartAction?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onBackground
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headerActions?.invoke()
                Icon(
                    imageVector = if (expanded) MiuixIcons.Regular.ExpandLess else MiuixIcons.Regular.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MiuixTheme.colorScheme.primary
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            ) {
                content()
            }
        }
    }
}
