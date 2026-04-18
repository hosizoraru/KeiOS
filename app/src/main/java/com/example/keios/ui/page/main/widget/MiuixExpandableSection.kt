package com.example.keios.ui.page.main.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            .clip(RoundedCornerShape(CardLayoutRhythm.cardCornerRadius))
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(CardLayoutRhythm.cardCornerRadius) },
                        effects = {
                            vibrancy()
                            blur(UiPerformanceBudget.backdropBlur.toPx())
                            lens(UiPerformanceBudget.backdropLens.toPx(), UiPerformanceBudget.backdropLens.toPx())
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
            enter = appExpandIn(),
            exit = appExpandOut()
        ) {
            AppCardBodyColumn(
                contentPadding = PaddingValues(
                    start = CardLayoutRhythm.cardHorizontalPadding,
                    end = CardLayoutRhythm.cardHorizontalPadding,
                    bottom = CardLayoutRhythm.cardVerticalPadding
                ),
                verticalSpacing = CardLayoutRhythm.sectionGap,
                content = { content() }
            )
        }
    }
}
