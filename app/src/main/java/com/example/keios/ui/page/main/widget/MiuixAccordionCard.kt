package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixAccordionCard(
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
    val isDark = isSystemInDarkTheme()
    val surface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.10f)

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
                        onDrawSurface = { drawRect(surface) }
                    )
                } else {
                    Modifier.background(surface)
                }
            )
    ) {
        AppCardHeader(
            title = title,
            subtitle = subtitle,
            startAction = headerStartAction,
            endActions = if (headerActions != null) {
                { headerActions.invoke() }
            } else {
                null
            },
            expandable = true,
            expanded = expanded,
            expandTint = MiuixTheme.colorScheme.primary,
            onClick = { onExpandedChange(!expanded) },
            onLongClick = onHeaderLongClick
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CardLayoutRhythm.cardContentPadding),
                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap)
            ) {
                content()
            }
        }
    }
}
