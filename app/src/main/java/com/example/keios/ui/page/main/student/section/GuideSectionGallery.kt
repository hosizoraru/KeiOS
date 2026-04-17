package com.example.keios.ui.page.main.student

import com.example.keios.ui.page.main.widget.GlassVariant
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.keios.R
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.widget.AppDropdownAnchorButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.buildTextCopyPayload
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import com.example.keios.ui.page.main.widget.rememberLightTextCopyAction
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import java.util.concurrent.ConcurrentHashMap

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
    var audioPlayProgress by remember(audioTargetUrl) { mutableStateOf(0f) }
    var audioPositionMs by remember(audioTargetUrl) { mutableStateOf(0L) }
    var audioDurationMs by remember(audioTargetUrl) { mutableStateOf(0L) }
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
                            } else {
                                if (!videoInlineExpanded) {
                                    videoInlineExpanded = true
                                } else {
                                    videoControlRequestId += 1
                                }
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
                                GuideVideoFullscreenActivity.launch(
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
                    seekPreview != null && resolvedDurationMs > 0L -> {
                        (resolvedDurationMs * seekPreview).toLong()
                    }
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

@Composable
internal fun GuideAudioSeekBar(
    progress: Float,
    enabled: Boolean,
    onSeekStarted: () -> Unit,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit
) {
    val normalizedProgress = progress.coerceIn(0f, 1f)
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        factory = { ctx ->
            SeekBar(ctx).apply {
                max = 1000
                isEnabled = enabled
                setPadding(0, 0, 0, 0)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progressValue: Int,
                        fromUser: Boolean
                    ) {
                        if (!fromUser) return
                        onSeekChanged((progressValue / 1000f).coerceIn(0f, 1f))
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        onSeekStarted()
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        val target = ((seekBar?.progress ?: 0) / 1000f).coerceIn(0f, 1f)
                        onSeekFinished(target)
                    }
                })
            }
        },
        update = { seekBar ->
            seekBar.isEnabled = enabled
            val targetProgress = (normalizedProgress * 1000f).toInt().coerceIn(0, 1000)
            if (kotlin.math.abs(seekBar.progress - targetProgress) > 2) {
                seekBar.progress = targetProgress
            }
        }
    )
}

internal fun formatAudioDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
fun GuideGalleryExpressionCardItem(
    title: String,
    items: List<BaGuideGalleryItem>,
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit = { _, _ -> },
    mediaUrlResolver: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    var showPicker by remember(title, items.size) { mutableStateOf(false) }
    var selectedIndex by rememberSaveable(title, items.size) { mutableStateOf(0) }
    LaunchedEffect(items.size) {
        if (selectedIndex !in items.indices) selectedIndex = 0
    }
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }
    val displayImageUrl = mediaUrlResolver(selectedItem.imageUrl)
    val displayMediaUrl = mediaUrlResolver(selectedItem.mediaUrl)
    val saveTargetUrl = remember(selectedItem.mediaType, displayImageUrl, displayMediaUrl) {
        if (selectedItem.mediaType.lowercase() == "video") {
            displayMediaUrl.ifBlank { displayImageUrl }
        } else {
            displayImageUrl.ifBlank { displayMediaUrl }
        }
    }
    val optionLabels = remember(items) {
        items.mapIndexed { index, item ->
            val normalizedTitle = normalizeGalleryTitle(item.title)
            val rawVariant = when {
                normalizedTitle.startsWith("角色表情") -> normalizedTitle.removePrefix("角色表情")
                normalizedTitle.startsWith("表情") -> normalizedTitle.removePrefix("表情")
                else -> ""
            }
            val variant = rawVariant
                .replace(Regex("""\d+$"""), "")
                .trim('（', '）', '(', ')', '-', '·', ' ')
            if (variant.isBlank()) {
                "角色表情${index + 1}"
            } else if (variant == "包") {
                "表情包${index + 1}"
            } else {
                "表情${index + 1}·$variant"
            }
        }
    }
    val pickerMaxHeight = remember(optionLabels.size) {
        val maxVisibleRows = 7
        val visibleRows = optionLabels.size.coerceIn(1, maxVisibleRows)
        8.dp + (46.dp * visibleRows)
    }
    val canOpenMedia = selectedItem.mediaUrl.isNotBlank() && selectedItem.mediaUrl != selectedItem.imageUrl
    val isImageType = selectedItem.mediaType.lowercase() != "video"
    val isVideoType = selectedItem.mediaType.lowercase() == "video"
    var videoInlineExpanded by remember(displayMediaUrl, selectedItem.mediaType) { mutableStateOf(false) }
    var videoInlinePlaying by remember(displayMediaUrl, selectedItem.mediaType) { mutableStateOf(false) }
    var videoControlRequestId by remember(displayMediaUrl, selectedItem.mediaType) { mutableIntStateOf(0) }
    var showImageFullscreen by remember(displayImageUrl) { mutableStateOf(false) }
    val canSwipeExpressions = optionLabels.size > 1
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var expressionDragAccumPx by remember(title, items.size) { mutableFloatStateOf(0f) }
    val expressionDragState = rememberDraggableState { delta ->
        expressionDragAccumPx += delta
    }
    val imageProgressState = remember(displayImageUrl) {
        MutableStateFlow(if (displayImageUrl.isBlank()) 1f else 0f)
    }
    val imageProgress by imageProgressState.collectAsState()
    var imageLoading by remember(displayImageUrl) { mutableStateOf(displayImageUrl.isNotBlank()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                Box(
                    modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it }
                ) {
                    AppDropdownAnchorButton(
                        backdrop = backdrop,
                        text = optionLabels.getOrElse(selectedIndex) { "角色表情1" },
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = { showPicker = !showPicker }
                    )
                    if (showPicker) {
                        SnapshotWindowListPopup(
                            show = showPicker,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            anchorBounds = pickerPopupAnchorBounds,
                            placement = SnapshotPopupPlacement.ButtonEnd,
                            onDismissRequest = { showPicker = false },
                            enableWindowDim = false
                        ) {
                            LiquidDropdownColumn(
                                modifier = Modifier.heightIn(max = pickerMaxHeight)
                            ) {
                                optionLabels.forEachIndexed { idx, option ->
                                    LiquidDropdownImpl(
                                        text = option,
                                        optionSize = optionLabels.size,
                                        isSelected = selectedIndex == idx,
                                        index = idx,
                                        onSelectedIndexChange = { selected ->
                                            selectedIndex = selected
                                            showPicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (isVideoType && displayMediaUrl.isNotBlank()) {
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
                            } else {
                                if (!videoInlineExpanded) {
                                    videoInlineExpanded = true
                                } else {
                                    videoControlRequestId += 1
                                }
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
                                GuideVideoFullscreenActivity.launch(
                                    context = context,
                                    mediaUrl = normalized
                                )
                            }
                        }
                    )
                }
                if (saveTargetUrl.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = MiuixIcons.Regular.Download,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            onSaveMedia(
                                saveTargetUrl,
                                optionLabels.getOrElse(selectedIndex) { title }
                            )
                        }
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
            }

            if (displayImageUrl.isNotBlank() && selectedItem.mediaType.lowercase() != "video") {
                GuidePressableMediaSurface(
                    modifier = Modifier.draggable(
                        state = expressionDragState,
                        orientation = Orientation.Horizontal,
                        enabled = canSwipeExpressions,
                        onDragStopped = { velocity ->
                            val totalDrag = expressionDragAccumPx
                            expressionDragAccumPx = 0f
                            val shouldGoNext =
                                totalDrag <= -swipeThresholdPx || velocity <= -1600f
                            val shouldGoPrev =
                                totalDrag >= swipeThresholdPx || velocity >= 1600f
                            when {
                                shouldGoNext && selectedIndex < items.lastIndex -> {
                                    selectedIndex += 1
                                    showPicker = false
                                }
                                shouldGoPrev && selectedIndex > 0 -> {
                                    selectedIndex -= 1
                                    showPicker = false
                                }
                            }
                        }
                    ),
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

            if (canOpenMedia) {
                if (selectedItem.mediaType.lowercase() == "video") {
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
                } else {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "打开",
                        leadingIcon = MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = { onOpenMedia(selectedItem.mediaUrl) }
                    )
                }
            }
        }
    }

    if (showImageFullscreen && isImageType && displayImageUrl.isNotBlank()) {
        GuideImageFullscreenDialog(
            imageUrl = displayImageUrl,
            onDismiss = { showImageFullscreen = false }
        )
    }
}

@Composable
fun GuideGalleryVideoGroupCardItem(
    title: String,
    items: List<BaGuideGalleryItem>,
    previewFallbackUrl: String = "",
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit = { _, _ -> },
    mediaUrlResolver: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    var showPicker by remember(title, items.size) { mutableStateOf(false) }
    var selectedIndex by rememberSaveable(title, items.size) { mutableStateOf(0) }
    LaunchedEffect(items.size) {
        if (selectedIndex !in items.indices) selectedIndex = 0
    }
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }
    val displayMediaUrl = mediaUrlResolver(selectedItem.mediaUrl)
    val displayPreviewUrl = mediaUrlResolver(
        selectedItem.imageUrl.ifBlank { previewFallbackUrl }
    )
    val saveTargetUrl = remember(displayMediaUrl, displayPreviewUrl) {
        displayMediaUrl.ifBlank { displayPreviewUrl }
    }
    var videoInlineExpanded by remember(displayMediaUrl) { mutableStateOf(false) }
    var videoInlinePlaying by remember(displayMediaUrl) { mutableStateOf(false) }
    var videoControlRequestId by remember(displayMediaUrl) { mutableIntStateOf(0) }
    val noteText = selectedItem.note.trim()
    val optionLabels = remember(title, items) {
        if (items.size <= 1) {
            listOf("视频 1")
        } else {
            items.mapIndexed { index, item ->
                val normalized = item.title.trim()
                if (normalized.isNotBlank() && normalized != title) normalized else "视频 ${index + 1}"
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (items.size > 1) {
                    var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                    Box(
                        modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it }
                    ) {
                        AppDropdownAnchorButton(
                            backdrop = backdrop,
                            text = optionLabels.getOrElse(selectedIndex) { "视频 1" },
                            textColor = Color(0xFF3B82F6),
                            variant = GlassVariant.Compact,
                            onClick = { showPicker = !showPicker }
                        )
                        if (showPicker) {
                            SnapshotWindowListPopup(
                                show = showPicker,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = pickerPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { showPicker = false },
                                enableWindowDim = false
                            ) {
                                LiquidDropdownColumn {
                                    optionLabels.forEachIndexed { idx, option ->
                                        LiquidDropdownImpl(
                                            text = option,
                                            optionSize = optionLabels.size,
                                            isSelected = selectedIndex == idx,
                                            index = idx,
                                            onSelectedIndexChange = { selected ->
                                                selectedIndex = selected
                                                showPicker = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (displayMediaUrl.isNotBlank()) {
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
                            } else {
                                if (!videoInlineExpanded) {
                                    videoInlineExpanded = true
                                } else {
                                    videoControlRequestId += 1
                                }
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
                                GuideVideoFullscreenActivity.launch(
                                    context = context,
                                    mediaUrl = normalized
                                )
                            }
                        }
                    )
                }
                if (saveTargetUrl.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = MiuixIcons.Regular.Download,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            onSaveMedia(
                                saveTargetUrl,
                                optionLabels.getOrElse(selectedIndex) { title }
                            )
                        }
                    )
                }
            }

            if (displayMediaUrl.isBlank()) {
                Text(
                    text = "未找到可播放的视频地址",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            } else {
                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                GuideInlineVideoPlayer(
                    mediaUrl = displayMediaUrl,
                    previewImageUrl = displayPreviewUrl,
                    backdrop = backdrop,
                    expanded = videoInlineExpanded,
                    onExpandedChange = { expanded -> videoInlineExpanded = expanded },
                    controlAction = GuideVideoControlAction.TogglePlayPause,
                    controlActionToken = videoControlRequestId,
                    onIsPlayingChange = { playing -> videoInlinePlaying = playing }
                )
            }
        }
    }
}

@Composable
fun GuideGalleryUnlockLevelCardItem(
    level: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    if (level.isBlank()) return
    val rowCopyPayload = remember(level) {
        buildGuideCopyPayload("回忆大厅解锁等级", level)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideCopyable(rowCopyPayload)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "回忆大厅解锁等级",
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = level,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
internal fun GuideInlineVideoPlayer(
    mediaUrl: String,
    previewImageUrl: String = "",
    backdrop: Backdrop?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    controlAction: GuideVideoControlAction? = null,
    controlActionToken: Int = 0,
    onIsPlayingChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    val normalizedPreviewUrl = remember(previewImageUrl) { normalizeGuideMediaSource(previewImageUrl) }
    var videoRatio by remember(normalizedUrl) { mutableStateOf(16f / 9f) }
    var isBuffering by remember(normalizedUrl) { mutableStateOf(false) }
    var isPlaying by remember(normalizedUrl) { mutableStateOf(false) }
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }
    var loopEnabled by remember(normalizedUrl) { mutableStateOf(false) }
    val openFullscreen = remember(context, normalizedUrl) {
        {
            if (normalizedUrl.isBlank()) {
                Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
            } else {
                GuideVideoFullscreenActivity.launch(
                    context = context,
                    mediaUrl = normalizedUrl
                )
            }
        }
    }

    if (!expanded) {
        if (normalizedPreviewUrl.isNotBlank()) {
            Box(
                modifier = Modifier.clickable {
                    onExpandedChange(false)
                    openFullscreen()
                }
            ) {
                GuideRemoteImageAdaptive(
                    imageUrl = normalizedPreviewUrl
                )
            }
        }
        return
    }

    val player = remember(context, normalizedUrl, expanded) {
        if (!expanded || normalizedUrl.isBlank()) {
            null
        } else {
            buildGuideVideoPlayer(context).apply {
                setMediaItem(MediaItem.fromUri(normalizedUrl))
                playWhenReady = true
                prepare()
            }
        }
    }

    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
                onIsPlayingChange(isPlayingNow)
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                loadError = error.errorCodeName
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            runCatching { boundPlayer.release() }
            isBuffering = false
            isPlaying = false
            onIsPlayingChange(false)
        }
    }

    LaunchedEffect(player, loopEnabled) {
        player?.repeatMode = if (loopEnabled) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    val activePlayer = player
    if (activePlayer == null) {
        onIsPlayingChange(false)
        Text(
            text = "视频暂不可用",
            color = MiuixTheme.colorScheme.onBackgroundVariant
        )
        return
    }

    LaunchedEffect(controlActionToken, controlAction, activePlayer) {
        if (controlActionToken <= 0 || controlAction == null) return@LaunchedEffect
        when (controlAction) {
            GuideVideoControlAction.TogglePlayPause -> {
                if (activePlayer.isPlaying) {
                    activePlayer.pause()
                } else {
                    activePlayer.play()
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(videoRatio)
            .clip(RoundedCornerShape(14.dp)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = activePlayer
            }
        },
        update = { view ->
            view.player = activePlayer
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = MiuixIcons.Regular.Replace,
            textColor = if (loopEnabled) Color(0xFF34C759) else Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = { loopEnabled = !loopEnabled }
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = MiuixIcons.Regular.ExpandLess,
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = { onExpandedChange(false) }
        )
    }

    if (isBuffering) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                progress = 0.35f,
                size = 14.dp,
                strokeWidth = 2.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = Color(0xFF60A5FA),
                    backgroundColor = Color(0x3360A5FA)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "视频加载中...",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        }
    }

    loadError?.takeIf { it.isNotBlank() }?.let { err ->
        Text(
            text = "视频播放失败：$err",
            color = MiuixTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalActivityApi::class)
@Composable
internal fun GuideImageFullscreenDialog(
    imageUrl: String,
    allowAutoRotate: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mediaAdaptiveRotationEnabled = remember { BASettingsStore.loadMediaAdaptiveRotationEnabled() }
    val systemAutoRotateEnabled = rememberSystemAutoRotateEnabled(active = !mediaAdaptiveRotationEnabled)
    val systemRotationDegrees = rememberDeviceRotationDegrees(
        active = !mediaAdaptiveRotationEnabled && systemAutoRotateEnabled
    )
    val normalizedImageUrl = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (normalizedImageUrl.isBlank()) return
    val isGifSource = remember(normalizedImageUrl) { isGifMediaSource(normalizedImageUrl) }
    val zoomState = rememberCoilZoomState()
    LaunchedEffect(zoomState) {
        // Keep fullscreen image interaction predictable:
        // pinch zoom (two fingers) + one-finger drag + double-tap zoom + single-tap dismiss.
        zoomState.zoomable.setDisabledGestureTypes(
            GestureType.ONE_FINGER_SCALE
        )
    }
    var retryToken by rememberSaveable(normalizedImageUrl) { mutableStateOf(0) }
    var lastTransformActiveAtMs by rememberSaveable(normalizedImageUrl) { mutableStateOf(0L) }
    val sampledState by produceState(
        initialValue = GuideFullscreenImageState(loading = !isGifSource),
        normalizedImageUrl,
        isGifSource,
        retryToken
    ) {
        if (isGifSource) {
            value = GuideFullscreenImageState(
                sampledBitmap = null,
                loading = false,
                helperLoadFailed = false
            )
            return@produceState
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(
                    context = context,
                    source = normalizedImageUrl,
                    maxDecodeDimension = 2048
                )
            }.getOrNull()
        }
        value = GuideFullscreenImageState(
            sampledBitmap = bitmap,
            loading = false,
            helperLoadFailed = bitmap == null
        )
    }
    val sampledBitmap = sampledState.sampledBitmap
    val ratioFromUrl = remember(normalizedImageUrl) {
        detectMediaRatioFromUrl(normalizedImageUrl)
    }
    val ratio = remember(sampledBitmap?.width, sampledBitmap?.height, ratioFromUrl) {
        val width = sampledBitmap?.width ?: 0
        val height = sampledBitmap?.height ?: 0
        when {
            width > 0 && height > 0 -> width.toFloat() / height.toFloat()
            ratioFromUrl != null -> ratioFromUrl
            else -> 1f
        }
    }
    LaunchedEffect(zoomState.zoomable.continuousTransformType) {
        if (zoomState.zoomable.continuousTransformType != ContinuousTransformType.NONE) {
            lastTransformActiveAtMs = SystemClock.elapsedRealtime()
        }
    }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_NONE) }
    var dialogWidthPx by remember { mutableIntStateOf(0) }
    // Fallback: keep standard back dismiss path in case predictive back callback
    // is unavailable on some ROM/dialog window combinations.
    BackHandler(enabled = true) {
        onDismiss()
    }
    PredictiveBackHandler(enabled = true) { backEvents ->
        var dismissedByPredictiveProgress = false
        try {
            backEvents.collect { event ->
                predictiveBackProgress = event.progress.coerceIn(0f, 1f)
                predictiveBackSwipeEdge = event.swipeEdge
                if (event.progress >= 0.995f) {
                    dismissedByPredictiveProgress = true
                    onDismiss()
                }
            }
            if (!dismissedByPredictiveProgress) {
                onDismiss()
            }
        } catch (_: CancellationException) {
        } finally {
            predictiveBackProgress = 0f
            predictiveBackSwipeEdge = BackEventCompat.EDGE_NONE
        }
    }
    val clampedBackProgress = predictiveBackProgress.coerceIn(0f, 1f)
    val easedBackProgress = clampedBackProgress * clampedBackProgress * (3f - 2f * clampedBackProgress)
    val backEdgeDirection = when (predictiveBackSwipeEdge) {
        BackEventCompat.EDGE_LEFT -> 1f
        BackEventCompat.EDGE_RIGHT -> -1f
        else -> 0f
    }
    val backTranslationX = dialogWidthPx.toFloat() *
        IMAGE_BACK_GESTURE_TRANSLATION_FACTOR *
        backEdgeDirection *
        easedBackProgress
    val backContentAlpha = (1f - easedBackProgress * IMAGE_BACK_GESTURE_CONTENT_FADE_FACTOR).coerceIn(0f, 1f)
    val backScrimAlpha = (1f - easedBackProgress * IMAGE_BACK_GESTURE_SCRIM_FADE_FACTOR).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { dialogWidthPx = it.width }
                .graphicsLayer {
                    translationX = backTranslationX
                    alpha = backContentAlpha
                }
                .background(Color.Black.copy(alpha = backScrimAlpha))
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val safeRatio = ratio.coerceAtLeast(0.1f)
                val viewportWidth = maxWidth
                val viewportHeight = maxHeight
                val viewportRatio = if (viewportHeight.value > 0f) {
                    viewportWidth.value / viewportHeight.value
                } else {
                    1f
                }

                fun fitArea(targetRatio: Float): Float {
                    val normalizedRatio = targetRatio.coerceAtLeast(0.1f)
                    return if (viewportRatio >= normalizedRatio) {
                        val fittedHeight = viewportHeight.value
                        val fittedWidth = fittedHeight * normalizedRatio
                        fittedWidth * fittedHeight
                    } else {
                        val fittedWidth = viewportWidth.value
                        val fittedHeight = fittedWidth / normalizedRatio
                        fittedWidth * fittedHeight
                    }
                }

                val normalArea = fitArea(safeRatio)
                val rotatedRatio = (1f / safeRatio).coerceAtLeast(0.1f)
                val rotatedArea = fitArea(rotatedRatio)
                val shouldRotate90 = safeRatio > 1.02f && rotatedArea > (normalArea * 1.12f)
                val targetRotation = if (mediaAdaptiveRotationEnabled) {
                    if (allowAutoRotate && shouldRotate90) 90 else 0
                } else {
                    if (systemAutoRotateEnabled) systemRotationDegrees else 0
                }
                val rotationTransition = remember(normalizedImageUrl) { Animatable(0f) }
                var appliedZoomRotation by rememberSaveable(normalizedImageUrl) { mutableIntStateOf(0) }
                var initializedRotation by rememberSaveable(normalizedImageUrl) { mutableStateOf(false) }
                val zoomInteracting = zoomState.zoomable.continuousTransformType != ContinuousTransformType.NONE
                LaunchedEffect(normalizedImageUrl) {
                    rotationTransition.snapTo(0f)
                    appliedZoomRotation = 0
                    initializedRotation = false
                }
                LaunchedEffect(normalizedImageUrl, targetRotation) {
                    if (!initializedRotation) {
                        zoomState.zoomable.rotate(targetRotation)
                        appliedZoomRotation = targetRotation
                        initializedRotation = true
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    if (targetRotation == appliedZoomRotation) {
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    if (zoomInteracting) {
                        zoomState.zoomable.rotate(targetRotation)
                        appliedZoomRotation = targetRotation
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    var delta = (targetRotation - appliedZoomRotation) % 360
                    if (delta > 180) delta -= 360
                    if (delta < -180) delta += 360
                    if (delta == 0) {
                        zoomState.zoomable.rotate(targetRotation)
                        appliedZoomRotation = targetRotation
                        rotationTransition.snapTo(0f)
                        return@LaunchedEffect
                    }
                    rotationTransition.snapTo(0f)
                    rotationTransition.animateTo(
                        targetValue = delta.toFloat(),
                        animationSpec = tween(
                            durationMillis = 220,
                            easing = FastOutSlowInEasing
                        )
                    )
                    zoomState.zoomable.rotate(targetRotation)
                    appliedZoomRotation = targetRotation
                    rotationTransition.snapTo(0f)
                }

                CoilZoomAsyncImage(
                    model = if (isGifSource) normalizedImageUrl else sampledBitmap ?: normalizedImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationTransition.value)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit,
                    zoomState = zoomState,
                    scrollBar = null,
                    onTap = {
                        if (zoomState.zoomable.continuousTransformType != ContinuousTransformType.NONE) {
                            return@CoilZoomAsyncImage
                        }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastTransformActiveAtMs < IMAGE_TAP_DISMISS_GESTURE_COOLDOWN_MS) {
                            return@CoilZoomAsyncImage
                        }
                        val userTransform = zoomState.zoomable.userTransform
                        val scaleNearBase = kotlin.math.abs(userTransform.scaleX - 1f) <= IMAGE_TAP_DISMISS_SCALE_EPSILON &&
                            kotlin.math.abs(userTransform.scaleY - 1f) <= IMAGE_TAP_DISMISS_SCALE_EPSILON
                        val offsetNearBase = kotlin.math.abs(userTransform.offsetX) <= IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX &&
                            kotlin.math.abs(userTransform.offsetY) <= IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX
                        if (!scaleNearBase || !offsetNearBase) {
                            return@CoilZoomAsyncImage
                        }
                        onDismiss()
                    }
                )

                if (sampledState.loading) {
                    CircularProgressIndicator(
                        progress = 0.28f,
                        size = 24.dp,
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = Color(0xFF60A5FA),
                            backgroundColor = Color(0x3360A5FA)
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (!sampledState.loading && sampledState.helperLoadFailed && sampledBitmap == null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "图片加载异常",
                        color = Color(0xFFBFDBFE)
                    )
                    GlassTextButton(
                        backdrop = null,
                        text = "重试",
                        leadingIcon = MiuixIcons.Regular.Refresh,
                        textColor = Color(0xFF60A5FA),
                        variant = GlassVariant.Compact,
                        onClick = { retryToken += 1 }
                    )
                }
            }
        }
    }
}

internal data class GuideFullscreenImageState(
    val sampledBitmap: Bitmap? = null,
    val loading: Boolean = false,
    val helperLoadFailed: Boolean = false
)

@Composable
internal fun GuideVideoFullscreenDialog(
    mediaUrl: String,
    forceLandscape: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }
    var videoRatio by remember(normalizedUrl) { mutableStateOf(16f / 9f) }

    val player = remember(context, normalizedUrl) {
        if (normalizedUrl.isBlank()) {
            null
        } else {
            buildGuideVideoPlayer(context).apply {
                setMediaItem(MediaItem.fromUri(normalizedUrl))
                playWhenReady = true
                prepare()
            }
        }
    }

    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                loadError = error.errorCodeName
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            runCatching { boundPlayer.release() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val activePlayer = player
            if (activePlayer != null) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val safeRatio = videoRatio.coerceAtLeast(0.2f)
                    val shouldRotateLandscape = forceLandscape && maxHeight > maxWidth

                    fun fitSize(targetRatio: Float): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
                        val normalizedRatio = targetRatio.coerceAtLeast(0.2f)
                        val viewportRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
                        return if (viewportRatio >= normalizedRatio) {
                            val h = maxHeight
                            (h * normalizedRatio) to h
                        } else {
                            val w = maxWidth
                            w to (w / normalizedRatio)
                        }
                    }

                    val playerModifier = if (shouldRotateLandscape) {
                        val rotatedFinal = fitSize((1f / safeRatio).coerceAtLeast(0.2f))
                        val preRotate = rotatedFinal.second to rotatedFinal.first
                        Modifier
                            .width(preRotate.first)
                            .height(preRotate.second)
                            .rotate(90f)
                            .align(Alignment.Center)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    AndroidView(
                        modifier = playerModifier,
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                this.player = activePlayer
                            }
                        },
                        update = { view ->
                            view.player = activePlayer
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    )
                }
            } else {
                Text(
                    text = "视频地址无效",
                    color = Color(0xFFBFDBFE),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            loadError?.takeIf { it.isNotBlank() }?.let { err ->
                Text(
                    text = "视频播放失败：$err",
                    color = Color(0xFFFCA5A5),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}

internal fun buildGuideVideoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
        .build()
}
