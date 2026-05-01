package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LiquidSwitchTrackWidth = 52.dp
private val LiquidSwitchTrackHeight = 32.dp
private val LiquidSwitchThumbSize = 26.dp
private val LiquidSwitchThumbPadding = 3.dp

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    checkedColor: Color = Color.Unspecified
) {
    val resolvedCheckedColor = if (checkedColor.isSpecified) {
        checkedColor
    } else {
        MiuixTheme.colorScheme.primary
    }
    if (backdrop != null) {
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = backdrop,
            modifier = modifier,
            enabled = enabled,
            checkedColor = resolvedCheckedColor
        )
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()
    val primary = resolvedCheckedColor

    val checkedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_switch_checked_progress"
    )
    val thumbOffsetX by animateDpAsState(
        targetValue = lerp(
            start = LiquidSwitchThumbPadding,
            stop = LiquidSwitchTrackWidth - LiquidSwitchThumbSize - LiquidSwitchThumbPadding,
            fraction = checkedProgress
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_switch_thumb_offset"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_switch_press_scale"
    )
    val switchAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.48f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liquid_switch_enabled_alpha"
    )
    val thumbScaleX by animateFloatAsState(
        targetValue = if (pressed && enabled) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_switch_thumb_scale_x"
    )

    val offTrackTop = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.86f)
    }
    val offTrackMid = if (isDark) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.54f)
    } else {
        Color.White.copy(alpha = 0.74f)
    }
    val offTrackBottom = if (isDark) {
        Color.Black.copy(alpha = 0.30f)
    } else {
        Color.Black.copy(alpha = 0.14f)
    }
    val onTrackTop = primary.copy(alpha = if (isDark) 0.44f else 0.34f)
    val onTrackMid = primary.copy(alpha = if (isDark) 0.36f else 0.27f)
    val onTrackBottom = primary.copy(alpha = if (isDark) 0.54f else 0.36f)
    val trackTopColor = lerp(offTrackTop, onTrackTop, checkedProgress)
    val trackMidColor = lerp(offTrackMid, onTrackMid, checkedProgress)
    val trackBottomColor = lerp(offTrackBottom, onTrackBottom, checkedProgress)

    val trackBorderColor = lerp(
        start = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.34f else 0.24f),
        stop = primary.copy(alpha = if (isDark) 0.60f else 0.66f),
        fraction = checkedProgress
    )
    val trackSpecularAlpha = lerpFloat(
        start = if (isDark) 0.08f else 0.16f,
        stop = if (isDark) 0.16f else 0.22f,
        fraction = checkedProgress
    )
    val trackRefractionAlpha = lerpFloat(
        start = if (isDark) 0.08f else 0.12f,
        stop = if (isDark) 0.20f else 0.16f,
        fraction = checkedProgress
    )

    val thumbTop = if (isDark) {
        Color.White.copy(alpha = 0.84f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }
    val thumbMid = if (isDark) {
        Color.White.copy(alpha = 0.76f)
    } else {
        Color.White.copy(alpha = 0.93f)
    }
    val thumbBottom = if (isDark) {
        Color.White.copy(alpha = 0.70f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }
    val thumbBorderColor = lerp(
        start = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.24f else 0.16f),
        stop = primary.copy(alpha = if (isDark) 0.38f else 0.26f),
        fraction = checkedProgress
    )
    val thumbSpecularAlpha = lerpFloat(
        start = if (isDark) 0.30f else 0.44f,
        stop = if (isDark) 0.36f else 0.48f,
        fraction = checkedProgress
    )
    val thumbAccentAlpha = lerpFloat(
        start = 0f,
        stop = if (isDark) 0.20f else 0.16f,
        fraction = checkedProgress
    )

    val shadowAmbient = if (isDark) {
        Color.Black.copy(alpha = 0.20f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val shadowSpot = if (isDark) {
        Color.Black.copy(alpha = 0.26f)
    } else {
        Color.Black.copy(alpha = 0.14f)
    }

    val thumbShadowAmbient = if (isDark) {
        Color.Black.copy(alpha = 0.24f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }
    val thumbShadowSpot = if (isDark) {
        Color.Black.copy(alpha = 0.32f)
    } else {
        Color.Black.copy(alpha = 0.16f)
    }

    Box(
        modifier = modifier
            .requiredSize(width = LiquidSwitchTrackWidth, height = LiquidSwitchTrackHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .alpha(switchAlpha)
            .shadow(
                elevation = if (checked) 7.dp else 5.dp,
                shape = ContinuousCapsule,
                clip = false,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot
            )
            .clip(ContinuousCapsule)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(trackTopColor, trackMidColor, trackBottomColor)
                ),
                shape = ContinuousCapsule
            )
            .border(1.dp, trackBorderColor, ContinuousCapsule),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = trackRefractionAlpha),
                            Color.Transparent,
                            primary.copy(alpha = trackRefractionAlpha * 0.42f)
                        )
                    ),
                    shape = ContinuousCapsule
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = trackSpecularAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = ContinuousCapsule
                )
        )
        Box(
            modifier = Modifier
                .requiredSize(LiquidSwitchTrackWidth, LiquidSwitchTrackHeight)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null,
                    onValueChange = onCheckedChange
                )
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .size(LiquidSwitchThumbSize)
                .graphicsLayer {
                    scaleX = thumbScaleX
                    scaleY = 1f
                }
                .shadow(
                    elevation = if (checked) 5.dp else 4.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = thumbShadowAmbient,
                    spotColor = thumbShadowSpot
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            thumbTop,
                            thumbMid,
                            thumbBottom
                        )
                    ),
                    shape = CircleShape
                )
                .border(1.dp, thumbBorderColor, CircleShape),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = thumbSpecularAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                primary.copy(alpha = thumbAccentAlpha)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}
