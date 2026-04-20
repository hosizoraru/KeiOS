package com.example.keios.ui.page.main.student.section

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.GuideBgmLoopStore
import com.example.keios.ui.page.main.student.GuideBgmPlayerStore
import com.example.keios.ui.page.main.student.GuideRemoteImageAdaptive
import com.example.keios.ui.page.main.student.GuideVideoControlAction
import com.example.keios.ui.page.main.student.GuideVideoFullscreenActivity
import com.example.keios.ui.page.main.student.extractGuideWebLinks
import com.example.keios.ui.page.main.student.isInteractiveFurnitureAnimatedGalleryItem
import com.example.keios.ui.page.main.student.isInteractiveFurnitureGalleryItem
import com.example.keios.ui.page.main.student.normalizeGalleryDisplayTitle
import com.example.keios.ui.page.main.student.normalizeGuideMediaSource
import com.example.keios.ui.page.main.student.section.gallery.GuideAudioSeekBar
import com.example.keios.ui.page.main.student.section.gallery.GuideImageFullscreenDialog
import com.example.keios.ui.page.main.student.section.gallery.GuideInlineVideoPlayer
import com.example.keios.ui.page.main.student.section.gallery.formatAudioDuration
import com.example.keios.ui.page.main.student.stripGuideWebLinks
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GuideGalleryCardItem(
    item: BaGuideGalleryItem,
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit = { _, _ -> },
    audioLoopScopeKey: String = "",
    mediaUrlResolver: (String) -> String = { it },
    embedded: Boolean = false,
    showMediaTypeLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalizedMediaType = item.mediaType.lowercase()
    val isInteractiveFurnitureAnimated = remember(item.title, item.mediaUrl, item.imageUrl) {
        isInteractiveFurnitureAnimatedGalleryItem(item)
    }
    val disableFullscreenAutoRotate = remember(
        item.title,
        item.mediaUrl,
        item.imageUrl,
        isInteractiveFurnitureAnimated
    ) {
        isInteractiveFurnitureGalleryItem(item) && !isInteractiveFurnitureAnimated
    }
    val preferredImageRaw = remember(
        item.imageUrl,
        item.mediaUrl,
        normalizedMediaType,
        isInteractiveFurnitureAnimated
    ) {
        when {
            normalizedMediaType == "video" || normalizedMediaType == "audio" -> item.imageUrl
            isInteractiveFurnitureAnimated && item.mediaUrl.isNotBlank() -> item.mediaUrl
            item.imageUrl.isNotBlank() -> item.imageUrl
            else -> item.mediaUrl
        }
    }
    val mediaTypeLabel = when (normalizedMediaType) {
        "video" -> ""
        "audio" -> ""
        "live2d" -> "Live2D"
        "imageset" -> "图集"
        else -> ""
    }
    val displayImageUrl = mediaUrlResolver(preferredImageRaw)
    val displayMediaUrl = mediaUrlResolver(item.mediaUrl.ifBlank { preferredImageRaw })
    val noteText = item.note.trim()
    val noteLinks = remember(noteText) { extractGuideWebLinks(noteText) }
    val notePlainText = remember(noteText) { stripGuideWebLinks(noteText) }
    val displayTitle = remember(item.title, normalizedMediaType) {
        normalizeGalleryDisplayTitle(item.title, normalizedMediaType)
    }
    val isMemoryHallBgmTitle = remember(displayTitle, normalizedMediaType) {
        normalizedMediaType == "audio" && displayTitle.startsWith("回忆大厅BGM")
    }
    val saveTargetUrl = remember(
        normalizedMediaType,
        displayImageUrl,
        displayMediaUrl,
        isInteractiveFurnitureAnimated
    ) {
        when (normalizedMediaType) {
            "video", "audio" -> displayMediaUrl.ifBlank { displayImageUrl }
            else -> {
                if (isInteractiveFurnitureAnimated && displayMediaUrl.isNotBlank()) {
                    displayMediaUrl
                } else {
                    displayImageUrl.ifBlank { displayMediaUrl }
                }
            }
        }
    }
    val canSaveMedia = saveTargetUrl.isNotBlank()
    val isImageType = normalizedMediaType != "video" && normalizedMediaType != "audio"
    val canOpenMedia = item.mediaUrl.isNotBlank() &&
        normalizeGuideMediaSource(displayMediaUrl) != normalizeGuideMediaSource(displayImageUrl)
    var videoInlineExpanded by remember(displayMediaUrl, normalizedMediaType) { mutableStateOf(false) }
    var videoInlinePlaying by remember(displayMediaUrl, normalizedMediaType) { mutableStateOf(false) }
    var videoControlRequestId by remember(displayMediaUrl, normalizedMediaType) { mutableIntStateOf(0) }
    var showImageFullscreen by remember(displayImageUrl, normalizedMediaType) { mutableStateOf(false) }
    val audioTargetUrl = remember(normalizedMediaType, displayMediaUrl) {
        if (normalizedMediaType == "audio") normalizeGuideMediaSource(displayMediaUrl) else ""
    }
    var audioIsPlaying by remember(audioTargetUrl) { mutableStateOf(false) }
    var audioIsBuffering by remember(audioTargetUrl) { mutableStateOf(false) }
    var audioPlayProgress by remember(audioTargetUrl) { mutableFloatStateOf(0f) }
    var audioPositionMs by remember(audioTargetUrl) { mutableLongStateOf(0L) }
    var audioDurationMs by remember(audioTargetUrl) { mutableLongStateOf(0L) }
    var audioSeekProgress by remember(audioTargetUrl) { mutableStateOf<Float?>(null) }
    var audioLoadError by remember(audioTargetUrl) { mutableStateOf<String?>(null) }
    var audioLoopEnabled by remember(audioLoopScopeKey, audioTargetUrl) {
        mutableStateOf(GuideBgmLoopStore.isEnabled(audioLoopScopeKey, audioTargetUrl))
    }
    val audioPlayer = remember(context, audioLoopScopeKey, audioTargetUrl) {
        GuideBgmPlayerStore.getOrCreate(
            context = context,
            scopeKey = audioLoopScopeKey,
            audioUrl = audioTargetUrl
        )
    }
    LaunchedEffect(audioPlayer, audioLoopEnabled) {
        audioPlayer?.repeatMode = if (audioLoopEnabled) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(audioPlayer, audioTargetUrl, audioLoopEnabled) {
        val player = audioPlayer ?: return@DisposableEffect onDispose { }
        audioIsPlaying = player.isPlaying
        val initialDuration = player.duration
        if (initialDuration > 0L) {
            audioDurationMs = initialDuration
        }
        val initialPosition = player.currentPosition.coerceAtLeast(0L)
        audioPositionMs = if (audioDurationMs > 0L) {
            initialPosition.coerceAtMost(audioDurationMs)
        } else {
            initialPosition
        }
        audioPlayProgress = if (audioDurationMs > 0L) {
            (audioPositionMs.toFloat() / audioDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                audioIsPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> audioIsBuffering = true
                    Player.STATE_READY -> {
                        audioIsBuffering = false
                        val duration = player.duration
                        if (duration > 0L) {
                            audioDurationMs = duration
                        }
                        val position = player.currentPosition
                        if (position >= 0L) {
                            audioPositionMs = if (audioDurationMs > 0L) {
                                position.coerceAtMost(audioDurationMs)
                            } else {
                                position
                            }
                        }
                    }

                    Player.STATE_ENDED -> {
                        if (audioLoopEnabled && player.repeatMode == Player.REPEAT_MODE_ONE) {
                            return
                        }
                        audioIsBuffering = false
                        audioIsPlaying = false
                        audioPlayProgress = 1f
                        val duration = player.duration
                        if (duration > 0L) {
                            audioDurationMs = duration
                            audioPositionMs = duration
                        }
                    }

                    Player.STATE_IDLE -> {
                        audioIsBuffering = false
                        if (!player.isPlaying) {
                            audioPlayProgress = 0f
                            audioPositionMs = 0L
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                audioIsBuffering = false
                audioIsPlaying = false
                audioLoadError = error.errorCodeName
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(audioTargetUrl, audioIsPlaying, audioIsBuffering, audioSeekProgress) {
        if (audioSeekProgress != null) return@LaunchedEffect
        val player = audioPlayer ?: run {
            audioPlayProgress = 0f
            audioPositionMs = 0L
            audioDurationMs = 0L
            return@LaunchedEffect
        }

        if (!audioIsPlaying && !audioIsBuffering) {
            val duration = player.duration
            if (duration > 0L) {
                audioDurationMs = duration
            }
            val position = player.currentPosition.coerceAtLeast(0L)
            audioPositionMs = if (audioDurationMs > 0L) {
                position.coerceAtMost(audioDurationMs)
            } else {
                position
            }
            audioPlayProgress = if (audioDurationMs > 0L) {
                (audioPositionMs.toFloat() / audioDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            return@LaunchedEffect
        }

        while ((audioIsPlaying || audioIsBuffering) && audioSeekProgress == null) {
            val duration = player.duration
            val position = player.currentPosition
            if (duration > 0L) {
                audioDurationMs = duration
            }
            audioPositionMs = if (position >= 0L) {
                if (audioDurationMs > 0L) {
                    position.coerceAtMost(audioDurationMs)
                } else {
                    position
                }
            } else {
                0L
            }
            audioPlayProgress = if (audioDurationMs > 0L) {
                (audioPositionMs.toFloat() / audioDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(200)
        }
    }
    val imageProgressState = remember(displayImageUrl) {
        MutableStateFlow(if (displayImageUrl.isBlank()) 1f else 0f)
    }
    val imageProgress by imageProgressState.collectAsState()
    var imageLoading by remember(displayImageUrl) { mutableStateOf(displayImageUrl.isNotBlank()) }

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        Column(
            modifier = contentModifier,
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
                    val indicatorProgress = when {
                        audioIsBuffering -> 0.35f
                        audioIsPlaying -> audioPlayProgress.coerceIn(0f, 1f).coerceAtLeast(0.06f)
                        cachedAudioReady || audioDurationMs > 0L -> 1f
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
                        leadingIcon = if (videoInlineExpanded && videoInlinePlaying) {
                            MiuixIcons.Regular.Pause
                        } else {
                            MiuixIcons.Regular.Play
                        },
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            if (normalizeGuideMediaSource(displayMediaUrl).isBlank()) {
                                Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                            } else if (!videoInlineExpanded) {
                                videoInlineExpanded = true
                            } else {
                                videoControlRequestId += 1
                            }
                        }
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = MiuixIcons.Regular.ExpandMore,
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
                        textColor = if (audioLoopEnabled) Color(0xFF34C759) else Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            val nextEnabled = !audioLoopEnabled
                            audioLoopEnabled = nextEnabled
                            GuideBgmLoopStore.setEnabled(
                                scopeKey = audioLoopScopeKey,
                                audioUrl = audioTargetUrl,
                                enabled = nextEnabled
                            )
                            audioPlayer?.repeatMode = if (nextEnabled) {
                                Player.REPEAT_MODE_ONE
                            } else {
                                Player.REPEAT_MODE_OFF
                            }
                        }
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = if (audioIsPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            val player = audioPlayer ?: run {
                                Toast.makeText(context, "音频地址无效", Toast.LENGTH_SHORT).show()
                                return@GlassTextButton
                            }
                            runCatching {
                                audioLoadError = null
                                if (player.currentMediaItem == null) {
                                    player.setMediaItem(MediaItem.fromUri(audioTargetUrl))
                                    player.prepare()
                                    player.play()
                                } else if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    if (player.playbackState == Player.STATE_ENDED) {
                                        player.seekTo(0)
                                    }
                                    player.play()
                                }
                            }.onFailure {
                                audioLoadError = it.message
                                Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                val playerDuration = audioPlayer?.duration ?: 0L
                val resolvedDurationMs = maxOf(audioDurationMs, playerDuration.coerceAtLeast(0L))
                val seekPreview = audioSeekProgress?.coerceIn(0f, 1f)
                val displayProgress = (seekPreview ?: audioPlayProgress).coerceIn(0f, 1f)
                val displayPositionMs = when {
                    seekPreview != null && resolvedDurationMs > 0L -> (resolvedDurationMs * seekPreview).toLong()
                    else -> audioPositionMs
                }.coerceAtLeast(0L).let { position ->
                    if (resolvedDurationMs > 0L) position.coerceAtMost(resolvedDurationMs) else position
                }

                GuideAudioSeekBar(
                    progress = displayProgress,
                    enabled = resolvedDurationMs > 0L && audioPlayer != null,
                    onSeekStarted = {
                        audioSeekProgress = displayProgress
                    },
                    onSeekChanged = { fraction ->
                        audioSeekProgress = fraction
                    },
                    onSeekFinished = { fraction ->
                        val player = audioPlayer
                        if (player == null) {
                            audioSeekProgress = null
                            return@GuideAudioSeekBar
                        }
                        val duration = maxOf(
                            resolvedDurationMs,
                            player.duration.coerceAtLeast(0L)
                        )
                        if (duration <= 0L) {
                            audioSeekProgress = null
                            return@GuideAudioSeekBar
                        }
                        val targetMs = (duration * fraction.coerceIn(0f, 1f)).toLong()
                            .coerceIn(0L, duration)
                        runCatching { player.seekTo(targetMs) }
                        audioDurationMs = duration
                        audioPositionMs = targetMs
                        audioPlayProgress =
                            (targetMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        audioSeekProgress = null
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatAudioDuration(displayPositionMs),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatAudioDuration(resolvedDurationMs),
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
                    onClick = { showImageFullscreen = true }
                ) {
                    GuideRemoteImageAdaptive(
                        imageUrl = displayImageUrl,
                        progressState = if (isImageType) imageProgressState else null,
                        onLoadingChanged = if (isImageType) {
                            { loading -> imageLoading = loading }
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
                            expanded = videoInlineExpanded,
                            onExpandedChange = { expanded -> videoInlineExpanded = expanded },
                            controlAction = GuideVideoControlAction.TogglePlayPause,
                            controlActionToken = videoControlRequestId,
                            onIsPlayingChange = { playing -> videoInlinePlaying = playing }
                        )
                    }

                    else -> {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "打开",
                            leadingIcon = MiuixIcons.Regular.Play,
                            textColor = Color(0xFF3B82F6),
                            variant = GlassVariant.Compact,
                            onClick = { onOpenMedia(item.mediaUrl) }
                        )
                    }
                }
            }

            audioLoadError?.takeIf { it.isNotBlank() }?.let { err ->
                Text(
                    text = "音频播放失败：$err",
                    color = MiuixTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (embedded) {
        content(
            modifier
                .fillMaxWidth()
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = Color(0x223B82F6),
                contentColor = MiuixTheme.colorScheme.onBackground
            ),
            onClick = {}
        ) {
            content(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }

    if (showImageFullscreen && isImageType && displayImageUrl.isNotBlank()) {
        GuideImageFullscreenDialog(
            imageUrl = displayImageUrl,
            allowAutoRotate = !disableFullscreenAutoRotate,
            onDismiss = { showImageFullscreen = false }
        )
    }
}
