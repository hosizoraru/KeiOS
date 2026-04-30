package os.kei.ui.page.main.student.section.gallery

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.R
import os.kei.ui.page.main.os.appLucideFullscreenIcon
import os.kei.ui.page.main.student.GuideRemoteImageAdaptive
import os.kei.ui.page.main.student.GuideVideoControlAction
import os.kei.ui.page.main.student.GuideVideoFullscreenActivity
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.student.section.GuidePressableMediaSurface
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.flow.MutableStateFlow
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
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
    showBgmFavoriteAction: Boolean,
    isBgmFavorite: Boolean,
    bgmFavoriteContentDescription: String,
    onToggleBgmFavorite: () -> Unit,
    showAudioLoopAction: Boolean,
    onToggleAudioLoop: () -> Unit,
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
                            Toast.makeText(
                                context,
                                context.getString(R.string.guide_media_video_url_invalid),
                                Toast.LENGTH_SHORT
                            ).show()
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
                            Toast.makeText(
                                context,
                                context.getString(R.string.guide_media_video_url_invalid),
                                Toast.LENGTH_SHORT
                            ).show()
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
                if (showBgmFavoriteAction) {
                    val favoriteColor = if (isBgmFavorite) Color(0xFFEC4899) else Color(0xFF3B82F6)
                    GlassIconButton(
                        backdrop = backdrop,
                        icon = MiuixIcons.Regular.FavoritesFill,
                        contentDescription = bgmFavoriteContentDescription,
                        onClick = onToggleBgmFavorite,
                        width = 34.dp,
                        height = 34.dp,
                        variant = GlassVariant.Compact,
                        iconTint = favoriteColor,
                        containerColor = favoriteColor
                    )
                }
                if (showAudioLoopAction) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = MiuixIcons.Regular.Replace,
                        textColor = if (audioState.loopEnabled) Color(0xFF34C759) else Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = onToggleAudioLoop
                    )
                }
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
                        text = stringResource(R.string.guide_action_open),
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
                text = context.getString(R.string.guide_media_audio_play_failed_with_reason, err),
                color = MiuixTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
