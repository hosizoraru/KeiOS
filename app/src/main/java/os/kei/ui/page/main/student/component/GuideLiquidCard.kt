package os.kei.ui.page.main.student.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget

@Composable
internal fun GuideLiquidCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardLayoutRhythm.cardCornerRadius,
    surfaceColor: Color = Color(0x223B82F6),
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    depthEffect: Boolean = true,
    shadow: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardBackdrop = rememberLayerBackdrop()

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(cardBackdrop)
        )
        LiquidSurface(
            backdrop = cardBackdrop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedRectangle(cornerRadius),
            enabled = enabled,
            isInteractive = isInteractive,
            tint = tint,
            surfaceColor = surfaceColor,
            blurRadius = blurRadius,
            lensRadius = lensRadius,
            depthEffect = depthEffect,
            shadow = shadow,
            onClick = onClick,
            content = content
        )
    }
}
