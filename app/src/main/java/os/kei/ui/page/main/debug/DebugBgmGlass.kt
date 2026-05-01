package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmGlassCapsule(
    accent: Color,
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousCapsule,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backdrop: Backdrop? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    DebugBgmGlassSurface(
        modifier = modifier,
        accent = accent,
        shape = shape,
        backdrop = backdrop,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
internal fun DebugBgmGlassIcon(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    selected: Boolean = false,
    backdrop: Backdrop? = null,
    onClick: () -> Unit = {}
) {
    DebugBgmGlassSurface(
        modifier = modifier
            .size(size),
        accent = accent,
        shape = CircleShape,
        selected = selected,
        backdrop = backdrop,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) accent else MiuixTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize)
        )
    }
}

@Composable
internal fun DebugBgmInlineIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    size: Dp = 36.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun DebugBgmGlassSurface(
    modifier: Modifier = Modifier,
    accent: Color,
    shape: Shape,
    selected: Boolean = false,
    backdrop: Backdrop? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val glassSurfaceColor = if (selected) {
        surfaceContainer.copy(alpha = 0.80f)
    } else {
        surfaceContainer.copy(alpha = 0.72f)
    }
    val accentOverlay = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur((UiPerformanceBudget.backdropBlur * 0.82f).toPx())
                            lens(
                                (UiPerformanceBudget.backdropLens * 0.78f).toPx(),
                                (UiPerformanceBudget.backdropLens * 0.78f).toPx()
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (selected) 0.82f else 0.64f)
                        },
                        shadow = {
                            Shadow.Default.copy(color = Color.Black.copy(alpha = 0.10f))
                        },
                        onDrawSurface = {
                            drawRect(glassSurfaceColor)
                            if (accentOverlay != Color.Transparent) drawRect(accentOverlay)
                        }
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = if (selected) {
                                listOf(
                                    accent.copy(alpha = 0.22f),
                                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
                                )
                            } else {
                                listOf(
                                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                                )
                            }
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.46f) else Color.White.copy(alpha = 0.24f),
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
