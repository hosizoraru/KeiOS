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
import os.kei.R
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.section.gallery.formatAudioDuration
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideBgmMiniPlayer(
    favorite: GuideBgmFavoriteItem,
    runtimeState: BaGuideBgmPlaybackRuntimeState,
    queueIndex: Int,
    queueSize: Int,
    accent: Color,
    onOpenQueue: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
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
    val playPauseDescription = stringResource(
        if (runtimeState.isPlaying) {
            R.string.ba_catalog_bgm_action_pause
        } else {
            R.string.ba_catalog_bgm_action_play
        }
    )
    val shape = RoundedCornerShape(18.dp)
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(collapsed) {
                detectVerticalDragGestures(
                    onDragStart = { dragOffsetY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        dragOffsetY += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            dragOffsetY > 28f -> onCollapsedChange(true)
                            dragOffsetY < -28f -> onCollapsedChange(false)
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
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .background(
                            color = MiuixTheme.colorScheme.outline.copy(alpha = 0.34f),
                            shape = CircleShape
                        )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaGuideCatalogEntryAvatar(
                        imageUrl = favorite.studentImageUrl.ifBlank { favorite.imageUrl },
                        fallbackRes = R.drawable.ba_tab_bgm
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
                    Text(
                        text = miniPlayerSubtitle(favorite),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideListIcon(),
                    contentDescription = openQueueDescription,
                    onClick = onOpenQueue,
                    width = 34.dp,
                    height = 34.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
                GlassIconButton(
                    backdrop = null,
                    icon = if (runtimeState.isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
                    contentDescription = playPauseDescription,
                    onClick = onTogglePlayback,
                    width = 40.dp,
                    height = 40.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
            }
            if (collapsed) {
                LinearProgressIndicator(
                    progress = runtimeState.progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 3.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = accent,
                        backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                    )
                )
                return@Column
            }
            LinearProgressIndicator(
                progress = runtimeState.progress,
                modifier = Modifier.fillMaxWidth(),
                height = 4.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = accent,
                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAudioDuration(runtimeState.positionMs),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideSkipBackIcon(),
                    contentDescription = previousDescription,
                    onClick = onPrevious,
                    width = 32.dp,
                    height = 32.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
                GlassIconButton(
                    backdrop = null,
                    icon = appLucideSkipForwardIcon(),
                    contentDescription = nextDescription,
                    onClick = onNext,
                    width = 32.dp,
                    height = 32.dp,
                    variant = GlassVariant.Compact,
                    iconTint = accent,
                    containerColor = accent
                )
                Text(
                    text = formatAudioDuration(runtimeState.durationMs),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1
                )
            }
        }
    }
}

private fun miniPlayerSubtitle(favorite: GuideBgmFavoriteItem): String {
    return favorite.title.ifBlank { favorite.note }
}
