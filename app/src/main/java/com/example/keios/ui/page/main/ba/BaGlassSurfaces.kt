package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.glassStyle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val BaCardShape = RoundedCornerShape(24.dp)
private val BaPanelShape = RoundedCornerShape(18.dp)
private val BaBadgeShape = RoundedCornerShape(999.dp)

@Composable
private fun Modifier.baGlassSurface(
    backdrop: Backdrop?,
    shape: RoundedCornerShape,
    cornerRadius: Dp,
    accentColor: Color,
    accentAlpha: Float,
    variant: GlassVariant,
): Modifier {
    val isDark = isSystemInDarkTheme()
    val glass = glassStyle(
        isDark = isDark,
        variant = variant,
        blurRadius = null,
    )
    val fallbackSurface = accentColor
        .copy(alpha = (accentAlpha * 0.25f).coerceIn(0f, 0.04f))
        .compositeOver(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = glass.fallbackAlpha))
    val borderColor = accentColor
        .copy(alpha = if (isDark) accentAlpha * 1.1f else accentAlpha * 0.95f)
        .compositeOver(glass.borderColor)
    val accentTint = accentColor.copy(alpha = (accentAlpha * 0.35f).coerceIn(0f, 0.05f))

    val surfaceModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(cornerRadius) },
            effects = {
                vibrancy()
                blur(glass.blur.toPx())
                lens(glass.lensStart.toPx(), glass.lensEnd.toPx())
            },
            highlight = {
                Highlight.Default.copy(alpha = glass.highlightAlpha)
            },
            shadow = {
                Shadow.Default.copy(
                    color = Color.Black.copy(alpha = glass.shadowAlpha),
                )
            },
            onDrawSurface = {
                drawRect(glass.baseColor)
                if (glass.overlayColor != Color.Transparent) {
                    drawRect(glass.overlayColor)
                }
                if (accentTint.alpha > 0f) {
                    drawRect(accentTint)
                }
            },
        )
    } else {
        Modifier.background(fallbackSurface, shape)
    }

    return this
        .clip(shape)
        .then(surfaceModifier)
        .border(
            width = glass.borderWidth,
            color = borderColor,
            shape = shape,
        )
}

@Composable
internal fun BaGlassCard(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    accentAlpha: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    verticalSpacing: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionModifier = when {
        onLongClick != null -> {
            Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
        }

        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .baGlassSurface(
                backdrop = backdrop,
                shape = BaCardShape,
                cornerRadius = 24.dp,
                accentColor = accentColor,
                accentAlpha = accentAlpha,
                variant = GlassVariant.Bar,
            )
            .then(interactionModifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

@Composable
internal fun BaGlassPanel(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    accentAlpha: Float = 0.05f,
    variant: GlassVariant = GlassVariant.Compact,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    verticalSpacing: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionModifier = when {
        onLongClick != null -> {
            Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
        }

        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }

    Column(
        modifier = modifier
            .baGlassSurface(
                backdrop = backdrop,
                shape = BaPanelShape,
                cornerRadius = 18.dp,
                accentColor = accentColor,
                accentAlpha = accentAlpha,
                variant = variant,
            )
            .then(interactionModifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

@Composable
internal fun BaGlassBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .clip(BaBadgeShape)
            .background(
                color = color.copy(alpha = if (isDark) 0.18f else 0.14f),
                shape = BaBadgeShape,
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = if (isDark) 0.28f else 0.22f),
                shape = BaBadgeShape,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun BaGlassMetricPanel(
    backdrop: Backdrop?,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    valueColor: Color = accentColor,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    BaGlassPanel(
        backdrop = backdrop,
        modifier = modifier,
        accentColor = accentColor,
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = label,
                        color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f),
                        maxLines = 1,
                    )
                    Text(
                        text = value,
                        color = valueColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    secondary?.takeIf { it.isNotBlank() }?.let { text ->
                        Text(
                            text = text,
                            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.88f),
                            maxLines = 1,
                        )
                    }
                }
            trailing?.invoke(this)
        }
    }
}
