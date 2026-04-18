package com.example.keios.ui.page.main.student

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.keios.ui.page.main.widget.AppDropdownAnchorButton
import com.example.keios.ui.page.main.widget.AppFeatureCard
import com.example.keios.ui.page.main.widget.AppSurfaceCard
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
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
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    AppFeatureCard(
        title = title,
        subtitle = noteText,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0x223B82F6),
        headerEndActions = {
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
    ) {
        if (displayMediaUrl.isBlank()) {
            Text(
                text = "未找到可播放的视频地址",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        } else {
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
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0x223B82F6)
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

internal fun buildGuideVideoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
        .build()
}
