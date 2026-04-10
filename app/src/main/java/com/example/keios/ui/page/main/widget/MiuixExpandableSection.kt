package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
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
    content: @Composable () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val sectionSurface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
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
                        onDrawSurface = { drawRect(sectionSurface) }
                    )
                } else {
                    Modifier.background(sectionSurface)
                }
            )
    ) {
        val headerModifier = if (onHeaderLongClick != null) {
            Modifier.combinedClickable(
                onClick = { onExpandedChange(!expanded) },
                onLongClick = onHeaderLongClick
            )
        } else {
            Modifier
        }
        BasicComponent(
            title = title,
            summary = subtitle,
            modifier = headerModifier,
            startAction = headerStartAction,
            onClick = if (onHeaderLongClick == null) {
                { onExpandedChange(!expanded) }
            } else {
                null
            },
            endActions = {
                Row {
                    headerActions?.invoke()
                    if (headerActions != null) Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) MiuixIcons.Regular.ExpandLess else MiuixIcons.Regular.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MiuixTheme.colorScheme.primary
                    )
                }
            }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                content()
            }
        }
    }
}
