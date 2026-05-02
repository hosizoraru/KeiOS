package os.kei.ui.page.main.widget.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppStatusPrimitives
import os.kei.ui.page.main.widget.core.rememberAppStatusPillMetrics
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: AppStatusPillSize = AppStatusPillSize.Default,
    contentPadding: PaddingValues? = null,
    backgroundAlphaOverride: Float? = null,
    borderAlphaOverride: Float? = null,
    backdrop: Backdrop? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val metrics = rememberAppStatusPillMetrics(size)
    val resolvedPadding = contentPadding ?: metrics.contentPadding
    val backgroundAlpha = backgroundAlphaOverride ?: if (isDark) 0.18f else 0.24f
    val borderAlpha = borderAlphaOverride ?: if (isDark) 0.35f else 0.42f
    val textColor = if (isDark) color else color.copy(alpha = 0.96f)
    val shape = AppStatusPrimitives.pillShape
    val liquidControlsEnabled = LocalLiquidControlsEnabled.current
    val localBackdrop = rememberLayerBackdrop()
    val activeBackdrop = when {
        !liquidControlsEnabled -> null
        backdrop != null -> backdrop
        else -> localBackdrop
    }
    val pillModifier = Modifier
        .then(modifier)
        .clip(shape)
        .then(
            if (activeBackdrop == null) {
                Modifier.background(color.copy(alpha = backgroundAlpha))
            } else {
                Modifier
            }
        )
        .border(
            width = 0.8.dp,
            color = color.copy(alpha = borderAlpha),
            shape = shape
        )
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            color = textColor,
            fontSize = metrics.typography.fontSize,
            lineHeight = metrics.typography.lineHeight,
            fontWeight = metrics.typography.fontWeight
        )
    }
    Box {
        if (activeBackdrop != null && backdrop == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(localBackdrop)
            )
        }
        if (activeBackdrop != null) {
            LiquidSurface(
                backdrop = activeBackdrop,
                modifier = pillModifier,
                shape = shape,
                isInteractive = false,
                surfaceColor = color.copy(alpha = backgroundAlpha),
                blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Compact),
                lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Compact),
                shadow = false
            ) {
                Box(
                    modifier = Modifier.padding(resolvedPadding),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        } else {
            Box(
                modifier = pillModifier,
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.padding(resolvedPadding),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        }
    }
}
