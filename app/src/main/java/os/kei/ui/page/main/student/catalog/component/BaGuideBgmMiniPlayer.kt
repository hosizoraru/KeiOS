package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import os.kei.R
import os.kei.ui.page.main.os.appLucideChevronDownIcon
import os.kei.ui.page.main.os.appLucideChevronUpIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRepeatIcon
import os.kei.ui.page.main.os.appLucideRepeatOneIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.section.gallery.formatAudioDuration
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideBgmMiniPlayer(
    favorite: GuideBgmFavoriteItem,
    runtimeState: BaGuideBgmPlaybackRuntimeState,
    queueIndex: Int,
    queueSize: Int,
    queueMode: BaGuideBgmQueueMode,
    accent: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenQueue: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onSliderInteractionChanged: (Boolean) -> Unit,
    onToggleQueueMode: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studentName = favorite.studentTitle.ifBlank {
        stringResource(R.string.ba_catalog_bgm_student_unknown)
    }
    val queuePosition = stringResource(
        R.string.ba_catalog_bgm_queue_position,
        queueIndex + 1,
        queueSize
    )
    val openQueueDescription = stringResource(R.string.ba_catalog_bgm_action_open_queue)
    val previousDescription = stringResource(R.string.ba_catalog_bgm_action_previous)
    val nextDescription = stringResource(R.string.ba_catalog_bgm_action_next)
    val modeText = stringResource(queueMode.labelRes)
    val openGalleryDescription = stringResource(R.string.ba_catalog_bgm_action_open_gallery)
    val seekDescription = stringResource(R.string.ba_catalog_bgm_seekbar)
    val volumeDescription = stringResource(R.string.ba_catalog_bgm_volume)
    val volumeText = stringResource(
        R.string.ba_catalog_bgm_volume_value,
        (runtimeState.volume * 100f).roundToInt().coerceIn(0, 100)
    )
    val expandCollapseDescription = stringResource(
        if (expanded) {
            R.string.ba_catalog_bgm_action_collapse_now_playing
        } else {
            R.string.ba_catalog_bgm_action_expand_now_playing
        }
    )
    val playPauseDescription = stringResource(
        if (runtimeState.isPlaying) {
            R.string.ba_catalog_bgm_action_pause
        } else {
            R.string.ba_catalog_bgm_action_play
        }
    )
    val shape = RoundedCornerShape(18.dp)
    val neutralControlTint = MiuixTheme.colorScheme.onBackgroundVariant
    val neutralControlContainer = MiuixTheme.colorScheme.surfaceContainer
    val primaryControlTint = MiuixTheme.colorScheme.onBackground
    val timeText = "${formatAudioDuration(runtimeState.positionMs)} / ${formatAudioDuration(runtimeState.durationMs)}"
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    GuideLiquidCard(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(expanded) {
                detectVerticalDragGestures(
                    onDragStart = { dragOffsetY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        dragOffsetY += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            dragOffsetY < -28f -> onExpandedChange(true)
                            dragOffsetY > 28f -> onExpandedChange(false)
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f }
                )
            }
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.35f),
                shape = shape
            ),
        cornerRadius = 18.dp,
        surfaceColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.94f),
        isInteractive = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NowPlayingDragHandle(
                expanded = expanded,
                accent = accent
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaGuideCatalogEntryAvatar(
                        imageUrl = favorite.studentImageUrl.ifBlank { favorite.imageUrl },
                        fallbackRes = R.drawable.ba_tab_bgm,
                        size = 42.dp
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = studentName,
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = AppTypographyTokens.CompactTitle.fontSize,
                            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        StatusPill(
                            label = queuePosition,
                            color = accent,
                            size = AppStatusPillSize.Compact
                        )
                    }
                }
                AppStandaloneLiquidIconButton(
                    icon = if (expanded) appLucideChevronDownIcon() else appLucideChevronUpIcon(),
                    contentDescription = expandCollapseDescription,
                    onClick = { onExpandedChange(!expanded) },
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = neutralControlTint,
                    containerColor = neutralControlContainer
                )
                AppStandaloneLiquidIconButton(
                    icon = if (runtimeState.isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
                    contentDescription = playPauseDescription,
                    onClick = onTogglePlayback,
                    width = 40.dp,
                    height = 40.dp,
                    variant = GlassVariant.Compact,
                    iconTint = primaryControlTint,
                    containerColor = accent
                )
            }

            if (!expanded) {
                BaGuideBgmPlaybackSeekBar(
                    progress = runtimeState.progress,
                    enabled = runtimeState.durationMs > 0L,
                    accent = accent,
                    contentDescription = seekDescription,
                    onSeekChanged = onSeekChanged,
                    onSeekFinished = onSeekFinished,
                    onInteractionChanged = onSliderInteractionChanged
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BaGuideBgmPlaybackSeekBar(
                        progress = runtimeState.progress,
                        enabled = runtimeState.durationMs > 0L,
                        accent = accent,
                        contentDescription = seekDescription,
                        onSeekChanged = onSeekChanged,
                        onSeekFinished = onSeekFinished,
                        onInteractionChanged = onSliderInteractionChanged
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeText,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = AppTypographyTokens.Supporting.fontSize,
                            lineHeight = AppTypographyTokens.Supporting.lineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        AppStandaloneLiquidIconButton(
                            icon = appLucideListIcon(),
                            contentDescription = openQueueDescription,
                            onClick = onOpenQueue,
                            width = 32.dp,
                            height = 32.dp,
                            variant = GlassVariant.Compact,
                            iconTint = neutralControlTint,
                            containerColor = neutralControlContainer
                        )
                        AppStandaloneLiquidIconButton(
                            icon = appLucideSkipBackIcon(),
                            contentDescription = previousDescription,
                            onClick = onPrevious,
                            width = 32.dp,
                            height = 32.dp,
                            variant = GlassVariant.Compact,
                            iconTint = neutralControlTint,
                            containerColor = neutralControlContainer
                        )
                        AppStandaloneLiquidIconButton(
                            icon = appLucideSkipForwardIcon(),
                            contentDescription = nextDescription,
                            onClick = onNext,
                            width = 32.dp,
                            height = 32.dp,
                            variant = GlassVariant.Compact,
                            iconTint = neutralControlTint,
                            containerColor = neutralControlContainer
                        )
                        AppStandaloneLiquidIconButton(
                            icon = if (queueMode == BaGuideBgmQueueMode.SingleLoop) {
                                appLucideRepeatOneIcon()
                            } else {
                                appLucideRepeatIcon()
                            },
                            contentDescription = modeText,
                            onClick = onToggleQueueMode,
                            width = 32.dp,
                            height = 32.dp,
                            variant = GlassVariant.Compact,
                            iconTint = if (queueMode == BaGuideBgmQueueMode.SingleLoop) {
                                Color(0xFF22C55E)
                            } else {
                                neutralControlTint
                            },
                            containerColor = if (queueMode == BaGuideBgmQueueMode.SingleLoop) {
                                Color(0x3322C55E)
                            } else {
                                neutralControlContainer
                            }
                        )
                        AppStandaloneLiquidIconButton(
                            icon = appLucideExternalLinkIcon(),
                            contentDescription = openGalleryDescription,
                            onClick = onOpenGuide,
                            width = 32.dp,
                            height = 32.dp,
                            variant = GlassVariant.Compact,
                            iconTint = neutralControlTint,
                            containerColor = neutralControlContainer
                        )
                    }

                    BaGuideBgmVolumeRow(
                        volume = runtimeState.volume,
                        contentDescription = volumeDescription,
                        valueText = volumeText,
                        onVolumeChanged = onVolumeChanged,
                        onInteractionChanged = onSliderInteractionChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingDragHandle(
    expanded: Boolean,
    accent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (expanded) 44.dp else 36.dp)
                .height(4.dp)
                .background(
                    color = if (expanded) {
                        accent.copy(alpha = 0.40f)
                    } else {
                        MiuixTheme.colorScheme.outline.copy(alpha = 0.34f)
                    },
                    shape = CircleShape
                )
        )
    }
}
