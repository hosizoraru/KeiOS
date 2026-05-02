package os.kei.ui.page.main.os.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.LiquidSurface

@Composable
internal fun ShellGlassPanelSurface(
    modifier: Modifier = Modifier,
    minHeight: Dp,
    contentPaddingHorizontal: Dp = 14.dp,
    contentPaddingVertical: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val panelBackdrop = rememberLayerBackdrop()
    val shape = RoundedRectangle(18.dp)
    val borderColor = if (isDark) {
        Color(0xFF9CCBFF).copy(alpha = 0.24f)
    } else {
        Color(0xFFC4DCF9).copy(alpha = 0.90f)
    }
    val baseColor = if (isDark) {
        Color(0xFF121A24).copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.66f)
    }
    val overlayColor = if (isDark) {
        Color(0xFF82B6F5).copy(alpha = 0.07f)
    } else {
        Color(0xFFE4F1FF).copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(panelBackdrop)
        )
        LiquidSurface(
            backdrop = panelBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .border(width = 1.dp, color = borderColor, shape = shape),
            shape = shape,
            tint = Color.Unspecified,
            surfaceColor = baseColor,
            blurRadius = 8.dp,
            lensRadius = 24.dp,
            chromaticAberration = true,
            depthEffect = true,
            shadow = false,
            isInteractive = false
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlayColor, shape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(
                        horizontal = contentPaddingHorizontal,
                        vertical = contentPaddingVertical
                    ),
                content = content
            )
        }
    }
}
