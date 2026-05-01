package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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
import os.kei.ui.page.main.widget.motion.appMotionFloatState
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
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    DebugBgmGlassSurface(
        modifier = modifier,
        accent = accent,
        shape = shape,
        selected = selected,
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
    val contentTint = if (selected) {
        accent
    } else {
        MiuixTheme.colorScheme.onBackground
    }
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
            tint = contentTint,
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
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (interactionSource != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                }
            ),
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by appMotionFloatState(
        targetValue = if (pressed) 1f else 0f,
        durationMillis = DebugBgmGlassSurfacePressMotionMs,
        label = "debug_bgm_glass_surface_press"
    )
    val isDark = isSystemInDarkTheme()
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val glassSurfaceColor = if (selected) {
        surfaceContainer.copy(alpha = if (isDark) 0.30f else 0.52f)
    } else {
        surfaceContainer.copy(alpha = if (isDark) 0.24f else 0.40f)
    }
    val pressOverlay = Color.White.copy(alpha = (if (isDark) 0.06f else 0.14f) * pressProgress)
    val accentOverlay = when {
        selected -> accent.copy(alpha = 0.16f + 0.08f * pressProgress)
        pressProgress > 0f -> accent.copy(alpha = 0.06f * pressProgress)
        else -> Color.Transparent
    }
    val fallbackTopColor = if (isDark) {
        Color.White.copy(alpha = if (selected) 0.16f else 0.10f)
    } else {
        Color.White.copy(alpha = if (selected) 0.42f else 0.36f)
    }
    val fallbackBottomColor = if (isDark) {
        Color.White.copy(alpha = if (selected) 0.09f else 0.06f)
    } else {
        Color.White.copy(alpha = if (selected) 0.25f else 0.22f)
    }
    val fallbackAccentAlpha = when {
        selected -> 0.12f + 0.06f * pressProgress
        pressProgress > 0f -> 0.07f * pressProgress
        else -> 0f
    }
    val fallbackPressColor = Color.White.copy(alpha = (if (isDark) 0.04f else 0.10f) * pressProgress)
    val fallbackAccentTopColor = accent.copy(alpha = fallbackAccentAlpha)
    val fallbackAccentBottomColor = accent.copy(alpha = fallbackAccentAlpha * 0.62f)
    val fallbackAccentModifier = if (fallbackAccentAlpha > 0f) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    fallbackAccentTopColor,
                    fallbackAccentBottomColor
                )
            )
        )
    } else {
        Modifier
    }
    val fallbackPressModifier = if (fallbackPressColor.alpha > 0f) {
        Modifier.background(fallbackPressColor)
    } else {
        Modifier
    }
    val borderColor = when {
        selected -> accent.copy(alpha = 0.46f + 0.08f * pressProgress)
        isDark -> Color.White.copy(alpha = 0.16f + 0.06f * pressProgress)
        else -> Color.White.copy(alpha = 0.62f + 0.10f * pressProgress)
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = 1f - 0.018f * pressProgress
                scaleY = 1f - 0.018f * pressProgress
            }
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
                            if (pressOverlay.alpha > 0f) drawRect(pressOverlay)
                            if (accentOverlay != Color.Transparent) drawRect(accentOverlay)
                        }
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                fallbackTopColor,
                                fallbackBottomColor
                            )
                        )
                    )
                        .then(fallbackPressModifier)
                        .then(fallbackAccentModifier)
                }
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private const val DebugBgmGlassSurfacePressMotionMs = 120
