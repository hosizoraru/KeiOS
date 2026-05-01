package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressSlider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmMiniPlayer(
    accent: Color,
    currentTrackTitle: String,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlaybackProgressChange: (Float) -> Unit,
    onPlaybackProgressChangeFinished: (Float) -> Unit,
    onPlaybackSliderInteractionChanged: (Boolean) -> Unit,
    expandedProgress: Float,
    compactProgress: Float,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    controlInteractionSource: MutableInteractionSource? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val expanded = expandedProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    val artworkSize = debugBgmLerpDp(38.dp, 42.dp, expanded)
    val artworkCornerRadius = debugBgmLerpDp(10.dp, 11.dp, expanded)
    val contentPadding = PaddingValues(
        horizontal = debugBgmLerpDp(10.dp, 14.dp, expanded),
        vertical = debugBgmLerpDp(8.dp, 7.dp, expanded)
    )
    val titleFontSize = debugBgmLerpSp(12f, AppTypographyTokens.Supporting.fontSize.value, expanded)
    val titleLineHeight = debugBgmLerpSp(14f, AppTypographyTokens.Supporting.lineHeight.value, expanded)
    val playIconSize = debugBgmLerpDp(27.dp, 25.dp, expanded)
    val itemGap = debugBgmLerpDp(8.dp, 10.dp, expanded)
    val progressTopPadding = debugBgmLerpDp(0.dp, 5.dp, expanded)
    val progressHostHeight = debugBgmLerpDp(0.dp, 20.dp, expanded)
    val sideControlSlotWidth = debugBgmLerpDp(0.dp, 32.dp, expanded)
    val sideControlsEnabled = expanded > 0.56f
    val playButtonScale = 1f - compact * 0.02f

    Row(
        modifier = modifier.padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(artworkCornerRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFC857), accent, Color(0xFFFF4D6D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = appLucideMusicIcon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(debugBgmLerpDp(21.dp, 23.dp, expanded))
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTrackTitle,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .padding(top = progressTopPadding)
                    .fillMaxWidth()
                    .height(progressHostHeight)
                    .clipToBounds()
                    .graphicsLayer { alpha = expanded },
                contentAlignment = Alignment.CenterStart
            ) {
                if (backdrop != null) {
                    LiquidMusicProgressSlider(
                        value = { playbackProgress.coerceIn(0f, 1f) },
                        onValueChange = onPlaybackProgressChange,
                        onValueChangeFinished = onPlaybackProgressChangeFinished,
                        onInteractionChanged = onPlaybackSliderInteractionChanged,
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = backdrop,
                        enabled = sideControlsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(18.dp)
                    )
                } else {
                    LinearProgressIndicator(
                        progress = playbackProgress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        height = 3.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = accent,
                            backgroundColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.22f)
                        )
                    )
                }
            }
        }
        DebugBgmMiniPlayerSideControl(
            width = sideControlSlotWidth,
            progress = expanded
        ) {
            DebugBgmInlineIcon(
                icon = appLucideSkipBackIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_previous),
                tint = MiuixTheme.colorScheme.onBackground,
                size = 32.dp,
                iconSize = 22.dp,
                enabled = sideControlsEnabled,
                interactionSource = controlInteractionSource,
                onClick = onPreviousClick
            )
        }
        val playInteractionSource = controlInteractionSource ?: remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    scaleX = playButtonScale
                    scaleY = playButtonScale
                }
                .clickable(
                    interactionSource = playInteractionSource,
                    indication = null,
                    onClick = onPlayPauseClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
                contentDescription = stringResource(
                    if (isPlaying) R.string.debug_component_lab_action_pause else R.string.debug_component_lab_action_play
                ),
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.size(playIconSize)
            )
        }
        DebugBgmMiniPlayerSideControl(
            width = sideControlSlotWidth,
            progress = expanded
        ) {
            DebugBgmInlineIcon(
                icon = appLucideSkipForwardIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_next),
                tint = MiuixTheme.colorScheme.onBackground,
                size = 32.dp,
                iconSize = 22.dp,
                enabled = sideControlsEnabled,
                interactionSource = controlInteractionSource,
                onClick = onNextClick
            )
        }
    }
}

@Composable
private fun DebugBgmMiniPlayerSideControl(
    width: androidx.compose.ui.unit.Dp,
    progress: Float,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .clipToBounds()
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun debugBgmLerpDp(
    start: androidx.compose.ui.unit.Dp,
    end: androidx.compose.ui.unit.Dp,
    fraction: Float
) = (start.value + (end.value - start.value) * fraction.coerceIn(0f, 1f)).dp

private fun debugBgmLerpSp(
    start: Float,
    end: Float,
    fraction: Float
) = (start + (end - start) * fraction.coerceIn(0f, 1f)).sp
