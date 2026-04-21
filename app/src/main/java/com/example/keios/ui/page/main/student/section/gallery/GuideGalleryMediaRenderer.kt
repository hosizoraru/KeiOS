package com.example.keios.ui.page.main.student.section.gallery

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ui.page.main.os.appLucideFullscreenIcon
import com.example.keios.ui.page.main.student.GuideRemoteImageAdaptive
import com.example.keios.ui.page.main.student.GuideVideoControlAction
import com.example.keios.ui.page.main.student.GuideVideoFullscreenActivity
import com.example.keios.ui.page.main.student.normalizeGuideMediaSource
import com.example.keios.ui.page.main.student.section.GuidePressableMediaSurface
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.flow.MutableStateFlow
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideGalleryCardContent(
    backdrop: Backdrop?,
    normalizedMediaType: String,
    displayTitle: String,
    mediaTypeLabel: String,
    showMediaTypeLabel: Boolean,
    isMemoryHallBgmTitle: Boolean,
    audioTargetUrl: String,
    displayMediaUrl: String,
    displayImageUrl: String,
    canSaveMedia: Boolean,
    saveTargetUrl: String,
    isImageType: Boolean,
    imageProgress: Float,
    imageLoading: Boolean,
    onImageLoadingChanged: (Boolean) -> Unit,
    imageProgressState: MutableStateFlow<Float>,
    notePlainText: String,
    noteLinks: List<String>,
    canOpenMedia: Boolean,
    itemMediaUrl: String,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit,
    audioState: GuideGalleryAudioPlayerState,
    gestureState: GuideGalleryGestureState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fullscreenIcon = appLucideFullscreenIcon()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTitle,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (isMemoryHallBgmTitle && audioTargetUrl.isNotBlank()) {
                val resolvedAudioSource = normalizeGuideMediaSource(displayMediaUrl)
                val cachedAudioReady = resolvedAudioSource.startsWith("file://", ignoreCase = true)
                // Header indicator keeps a stable readiness meaning: cached = ready.
                val indicatorProgress = when {
                    cachedAudioReady -> 1f
                    audioState.isBuffering -> 0.35f
                    audioState.resolvedDurationMs > 0L -> 1f
                    else -> 0.06f
                }
                val progressForegroundColor = if (indicatorProgress >= 0.999f) {
                    Color(0xFF34C759)
                } else {
                    Color(0xFF3B82F6)
                }
                val progressBackgroundColor = if (indicatorProgress >= 0.999f) {
                    Color(0x5534C759)
                } else {
                    Color(0x553B82F6)
                }
                CircularProgressIndicator(
                    progress = indicatorProgress,
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = progressForegroundColor,
                        backgroundColor = progressBackgroundColor
                    )
                )
            }
            if (showMediaTypeLabel && mediaTypeLabel.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = mediaTypeLabel,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
            }
            if (normalizedMediaType == "video" && displayMediaUrl.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = "",
                    leadingIcon = if (gestureState.videoInlineExpanded && gestureState.videoInlinePlaying) {
                        MiuixIcons.Regular.Pause
                    } else {
                        MiuixIcons.Regular.Play
                    },
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {
                        if (normalizeGuideMediaSource(displayMediaUrl).isBlank()) {
                            Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                        } else if (!gestureState.videoInlineExpanded) {
                            gestureState.videoInlineExpanded = true
                        } else {
                            gestureState.requestToggleVideoPlayback()
                        }
                    }
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = "",
                    leadingIcon = fullscreenIcon,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {
                        val normalized = normalizeGuideMediaSource(displayMediaUrl)
                        if (normalized.isBlank()) {
                            Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                        } else {
                            GuideVideoFullscreenActivity.Companion.launch(
                                context = context,
                                mediaUrl = normalized
                            )
                        }
                    }
                )
            }
            if (canSaveMedia) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = "",
                    leadingIcon = MiuixIcons.Regular.Download,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = { onSaveMedia(saveTargetUrl, displayTitle) }
                )
            }
            if (isImageType && displayImageUrl.isNotBlank()) {
                val imageProgressValue = if (imageLoading) imageProgress.coerceIn(0f, 1f) else 1f
                val progressForegroundColor = if (imageProgressValue >= 0.999f) Color(0xFF34C759) else Color(0xFF3B82F6)
                val progressBackgroundColor = if (imageProgressValue >= 0.999f) Color(0x5534C759) else Color(0x553B82F6)
                CircularProgressIndicator(
                    progress = imageProgressValue,
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = progressForegroundColor,
                        backgroundColor = progressBackgroundColor
                    )
                )
            }
            if (normalizedMediaType == "audio" && audioTargetUrl.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = "",
                    leadingIcon = MiuixIcons.Regular.Replace,
                    textColor = if (audioState.loopEnabled) Color(0xFF34C759) else Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = { audioState.toggleLoop() }
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = "",
                    leadingIcon = if (audioState.isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = { audioState.togglePlay(context) }
                )
            }
        }

        if (notePlainText.isNotBlank()) {
            Text(
                text = notePlainText,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (normalizedMediaType == "audio" && audioTargetUrl.isNotBlank()) {
            GuideAudioSeekBar(
                progress = audioState.displayProgress,
                enabled = audioState.resolvedDurationMs > 0L && audioState.player != null,
                onSeekStarted = { audioState.startSeekPreview() },
                onSeekChanged = { fraction -> audioState.updateSeekPreview(fraction) },
                onSeekFinished = { fraction -> audioState.finishSeek(fraction) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAudioDuration(audioState.displayPositionMs),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = formatAudioDuration(audioState.resolvedDurationMs),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 12.sp
                )
            }
        }
        if (noteLinks.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                noteLinks.forEach { link ->
                    Text(
                        text = link,
                        color = Color(0xFF3B82F6),
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            onOpenMedia(link)
                        }
                    )
                }
            }
        }

        if (displayImageUrl.isNotBlank() && normalizedMediaType != "video" && normalizedMediaType != "audio") {
            GuidePressableMediaSurface(
                onClick = { gestureState.showImageFullscreen = true }
            ) {
                GuideRemoteImageAdaptive(
                    imageUrl = displayImageUrl,
                    progressState = if (isImageType) imageProgressState else null,
                    onLoadingChanged = if (isImageType) {
                        { loading -> onImageLoadingChanged(loading) }
                    } else {
                        null
                    }
                )
            }
        }

        if (canOpenMedia && normalizedMediaType != "audio") {
            when (normalizedMediaType) {
                "video" -> {
                    GuideInlineVideoPlayer(
                        mediaUrl = displayMediaUrl,
                        previewImageUrl = displayImageUrl,
                        backdrop = backdrop,
                        expanded = gestureState.videoInlineExpanded,
                        onExpandedChange = { expanded -> gestureState.videoInlineExpanded = expanded },
                        controlAction = GuideVideoControlAction.TogglePlayPause,
                        controlActionToken = gestureState.videoControlRequestId,
                        onIsPlayingChange = { playing -> gestureState.videoInlinePlaying = playing }
                    )
                }

                else -> {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "打开",
                        leadingIcon = MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = { onOpenMedia(itemMediaUrl) }
                    )
                }
            }
        }

        audioState.loadError?.takeIf { it.isNotBlank() }?.let { err ->
            Text(
                text = "音频播放失败：$err",
                color = MiuixTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
