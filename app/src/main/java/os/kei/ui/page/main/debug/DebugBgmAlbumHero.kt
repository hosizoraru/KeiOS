package os.kei.ui.page.main.debug

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRepeatIcon
import os.kei.ui.page.main.os.appLucideVolume2Icon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.LiquidButton
import os.kei.ui.page.main.widget.glass.LiquidVolumeSlider
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmAlbumHero(
    accent: Color,
    collapseProgress: Float,
    repeatEnabled: Boolean,
    isPlaying: Boolean,
    playbackVolume: Float,
    sectionTitle: String,
    sectionMeta: String,
    onRepeatClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: (Float) -> Unit,
    onVolumeSliderInteractionChanged: (Boolean) -> Unit
) {
    var volumeControlVisible by rememberSaveable { mutableStateOf(true) }
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val density = LocalDensity.current
    val volumeTransition = updateTransition(
        targetState = volumeControlVisible,
        label = "debug_bgm_volume_control"
    )
    val volumeMotionDuration = resolvedMotionDuration(DebugBgmVolumeControlMotionMs, animationsEnabled)
    val volumeHeight by volumeTransition.animateDp(
        transitionSpec = {
            tween(durationMillis = volumeMotionDuration, easing = FastOutSlowInEasing)
        },
        label = "debug_bgm_volume_height"
    ) { visible ->
        if (visible) DebugBgmVolumeControlHeight else 0.dp
    }
    val volumeAlpha by volumeTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = volumeMotionDuration, easing = FastOutSlowInEasing)
        },
        label = "debug_bgm_volume_alpha"
    ) { visible ->
        if (visible) 1f else 0f
    }
    val volumeOffsetY by volumeTransition.animateDp(
        transitionSpec = {
            tween(durationMillis = volumeMotionDuration, easing = FastOutSlowInEasing)
        },
        label = "debug_bgm_volume_offset"
    ) { visible ->
        if (visible) 0.dp else (-6).dp
    }
    val volumeScale by volumeTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = volumeMotionDuration, easing = FastOutSlowInEasing)
        },
        label = "debug_bgm_volume_scale"
    ) { visible ->
        if (visible) 1f else 0.98f
    }
    val volumeSpacing by volumeTransition.animateDp(
        transitionSpec = {
            tween(durationMillis = volumeMotionDuration, easing = FastOutSlowInEasing)
        },
        label = "debug_bgm_volume_spacing"
    ) { visible ->
        if (visible) 12.dp else 0.dp
    }
    val volumeOffsetPx = with(density) { volumeOffsetY.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = 1f - collapseProgress * 0.04f
                scaleY = 1f - collapseProgress * 0.04f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DebugBgmAlbumArtwork(accent = accent)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_album_title),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = 25.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sectionTitle,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.SectionTitle.fontSize,
                lineHeight = AppTypographyTokens.SectionTitle.lineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sectionMeta,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(volumeSpacing)
        ) {
            DebugBgmAlbumPrimaryActions(
                accent = accent,
                repeatEnabled = repeatEnabled,
                isPlaying = isPlaying,
                volumeControlVisible = volumeControlVisible,
                onRepeatClick = onRepeatClick,
                onPlayPauseClick = onPlayPauseClick,
                onVolumeClick = { volumeControlVisible = !volumeControlVisible }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(volumeHeight)
                    .clipToBounds()
            ) {
                DebugBgmAlbumVolumeControl(
                    accent = accent,
                    volume = playbackVolume,
                    onVolumeChange = onVolumeChange,
                    onVolumeChangeFinished = onVolumeChangeFinished,
                    onInteractionChanged = onVolumeSliderInteractionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = volumeAlpha
                            translationY = volumeOffsetPx
                            scaleX = volumeScale
                            scaleY = volumeScale
                        }
                )
            }
        }
    }
}

@Composable
private fun DebugBgmAlbumArtwork(accent: Color) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(1f)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF4D6D),
                        Color(0xFFFFC857),
                        Color(0xFF35C2FF),
                        accent,
                        Color(0xFF2ED573)
                    )
                )
            )
            .border(8.dp, Color.White.copy(alpha = 0.78f), shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = appLucideMusicIcon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun DebugBgmAlbumPrimaryActions(
    accent: Color,
    repeatEnabled: Boolean,
    isPlaying: Boolean,
    volumeControlVisible: Boolean,
    onRepeatClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onVolumeClick: () -> Unit
) {
    val actionsBackdrop = rememberLayerBackdrop()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(actionsBackdrop)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebugBgmRoundAction(
                icon = appLucideRepeatIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_repeat),
                accent = accent,
                selected = repeatEnabled,
                onClick = onRepeatClick,
                backdrop = actionsBackdrop
            )
            DebugBgmPlayAction(
                accent = accent,
                isPlaying = isPlaying,
                onClick = onPlayPauseClick,
                backdrop = actionsBackdrop
            )
            DebugBgmRoundAction(
                icon = appLucideVolume2Icon(),
                contentDescription = stringResource(R.string.debug_component_lab_liquid_volume_slider_label),
                accent = accent,
                selected = volumeControlVisible,
                onClick = onVolumeClick,
                backdrop = actionsBackdrop
            )
        }
    }
}

@Composable
private fun DebugBgmAlbumVolumeControl(
    accent: Color,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: (Float) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val volumeBackdrop = rememberLayerBackdrop()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DebugBgmVolumeControlHeight)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(volumeBackdrop)
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = appLucideVolume2Icon(),
                contentDescription = stringResource(R.string.debug_component_lab_liquid_volume_slider_label),
                tint = accent.copy(alpha = 0.95f),
                modifier = Modifier.size(22.dp)
            )
            LiquidVolumeSlider(
                value = { volume.coerceIn(0f, 1f) },
                onValueChange = onVolumeChange,
                onValueChangeFinished = onVolumeChangeFinished,
                onInteractionChanged = onInteractionChanged,
                valueRange = 0f..1f,
                visibilityThreshold = 0.001f,
                backdrop = volumeBackdrop,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            )
            Text(
                text = stringResource(R.string.debug_component_lab_volume_value, (volume * 100).toInt()),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DebugBgmRoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    accent: Color,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    backdrop: Backdrop
) {
    val contentTint = if (selected) {
        accent.copy(alpha = 0.98f)
    } else {
        MiuixTheme.colorScheme.onBackground
    }
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        tint = Color.Unspecified,
        surfaceColor = DebugBgmRoundActionButtonSurfaceColor(),
        shape = CircleShape,
        height = 52.dp,
        horizontalPadding = 0.dp,
        modifier = Modifier.size(52.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun DebugBgmRoundActionButtonSurfaceColor(): Color {
    return MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.16f)
}

@Composable
private fun DebugBgmPlayAction(
    accent: Color,
    isPlaying: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop
) {
    val contentTint = if (isPlaying) {
        accent.copy(alpha = 0.98f)
    } else {
        MiuixTheme.colorScheme.onBackground
    }
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = 116.dp),
        tint = Color.Unspecified,
        surfaceColor = DebugBgmActionButtonSurfaceColor(selected = isPlaying, selectedAlpha = 0.24f),
        shape = Capsule(),
        height = 52.dp,
        horizontalPadding = 24.dp
    ) {
        Icon(
            imageVector = if (isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
            contentDescription = null,
            tint = contentTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(
                if (isPlaying) R.string.debug_component_lab_action_pause else R.string.debug_component_lab_action_play
            ),
            color = contentTint,
            fontSize = AppTypographyTokens.CardHeader.fontSize,
            lineHeight = AppTypographyTokens.CardHeader.lineHeight,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun DebugBgmActionButtonSurfaceColor(
    selected: Boolean,
    selectedAlpha: Float = 0.28f
): Color {
    val alpha = if (selected) selectedAlpha else 0.16f
    return MiuixTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
}

private val DebugBgmVolumeControlHeight = 34.dp
private const val DebugBgmVolumeControlMotionMs = 220
