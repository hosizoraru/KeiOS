package com.example.keios.ui.page.main.student

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.keios.ba.helper.GameKeeFetchHelper
import com.example.keios.R
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

private fun normalizeGuideMediaSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
    }
}

private fun loadGuideBitmapSource(
    source: String,
    onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
): Bitmap? {
    if (source.isBlank()) return null
    val uri = runCatching { Uri.parse(source) }.getOrNull()
    if (uri?.scheme.equals("file", ignoreCase = true)) {
        val path = uri?.path.orEmpty().ifBlank { Uri.decode(uri?.encodedPath.orEmpty()) }
        return if (path.isNotBlank()) BitmapFactory.decodeFile(path) else null
    }
    return if (onProgress != null) {
        GameKeeFetchHelper.fetchImageWithProgress(source, onProgress)
    } else {
        GameKeeFetchHelper.fetchImage(source)
    }
}

@Composable
fun GuideRemoteImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageHeight: androidx.compose.ui.unit.Dp = 220.dp
) {
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { loadGuideBitmapSource(target) }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(14.dp))
    )
}

@Composable
fun GuideRemoteImageAdaptive(
    imageUrl: String,
    modifier: Modifier = Modifier,
    progressState: MutableStateFlow<Float>? = null,
    onLoadingChanged: ((Boolean) -> Unit)? = null
) {
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) {
        progressState?.value = 1f
        onLoadingChanged?.invoke(false)
        return
    }
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        progressState?.value = 0f
        onLoadingChanged?.invoke(true)
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(target) { downloadedBytes, totalBytes ->
                    if (totalBytes > 0L) {
                        progressState?.value =
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    }
                }
            }.getOrNull()
        }
        if (value != null) {
            progressState?.value = 1f
        }
        onLoadingChanged?.invoke(false)
    }
    val rendered = bitmap ?: return
    val ratio = remember(rendered.width, rendered.height) {
        if (rendered.width > 0 && rendered.height > 0) {
            rendered.width.toFloat() / rendered.height.toFloat()
        } else {
            1f
        }
    }
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(14.dp))
    )
}

@Composable
fun GuideRemoteIcon(
    imageUrl: String,
    modifier: Modifier = Modifier,
    iconWidth: androidx.compose.ui.unit.Dp = 20.dp,
    iconHeight: androidx.compose.ui.unit.Dp = iconWidth
) {
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { loadGuideBitmapSource(target) }.getOrNull()
        }
    }
    val rendered = bitmap ?: return
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(iconWidth)
            .height(iconHeight)
    )
}

@Composable
fun GuideRowsSection(
    rows: List<BaGuideRow>,
    emptyText: String,
    imageHeight: androidx.compose.ui.unit.Dp = 96.dp
) {
    if (rows.isEmpty()) {
        Text(emptyText, color = MiuixTheme.colorScheme.onBackgroundVariant)
        return
    }
    val visibleRows = rows.take(120)
    visibleRows.forEachIndexed { index, row ->
        val key = row.key.ifBlank { "信息" }
        val hasImage = row.imageUrl.isNotBlank()
        val value = row.value
            .takeIf { it.isNotBlank() && it != "图片" }
            ?: if (hasImage) "见下图" else "-"
        MiuixInfoItem(key, value)
        if (hasImage) {
            Spacer(modifier = Modifier.height(6.dp))
            GuideRemoteImage(
                imageUrl = row.imageUrl,
                imageHeight = imageHeight
            )
        }
        if (index < visibleRows.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun GuideGalleryCardItem(
    item: BaGuideGalleryItem,
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    mediaUrlResolver: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    val mediaTypeLabel = when (item.mediaType.lowercase()) {
        "video" -> "视频"
        "live2d" -> "Live2D"
        "imageset" -> "图集"
        else -> "影画"
    }
    val displayImageUrl = mediaUrlResolver(item.imageUrl)
    val displayMediaUrl = mediaUrlResolver(item.mediaUrl)
    val isImageType = item.mediaType.lowercase() != "video"
    val canOpenMedia = item.mediaUrl.isNotBlank() && item.mediaUrl != item.imageUrl
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
                    text = item.title.ifBlank { "影画条目" },
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isImageType && displayImageUrl.isNotBlank()) {
                    CircularProgressIndicator(
                        progress = if (imageLoading) imageProgress.coerceIn(0f, 1f) else 1f,
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = Color(0xFF3B82F6),
                            backgroundColor = Color(0x553B82F6)
                        )
                    )
                }
                GlassTextButton(
                    backdrop = backdrop,
                    text = mediaTypeLabel,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }

            if (displayImageUrl.isNotBlank()) {
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

            if (canOpenMedia) {
                if (item.mediaType.lowercase() == "video") {
                    GuideInlineVideoPlayer(
                        mediaUrl = displayMediaUrl,
                        backdrop = backdrop,
                        onOpenExternal = onOpenMedia
                    )
                } else {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "打开",
                        leadingIcon = MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        bottomBarStyle = true,
                        onClick = { onOpenMedia(item.mediaUrl) }
                    )
                }
            }
        }
    }
}

@Composable
fun GuideGalleryExpressionCardItem(
    title: String,
    items: List<BaGuideGalleryItem>,
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    mediaUrlResolver: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    var showPicker by remember(title, items.size) { mutableStateOf(false) }
    var selectedIndex by rememberSaveable(title, items.size) { mutableStateOf(0) }
    LaunchedEffect(items.size) {
        if (selectedIndex !in items.indices) selectedIndex = 0
    }
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }
    val displayImageUrl = mediaUrlResolver(selectedItem.imageUrl)
    val displayMediaUrl = mediaUrlResolver(selectedItem.mediaUrl)
    val optionLabels = remember(items) {
        items.mapIndexed { index, _ -> "角色表情${index + 1}" }
    }
    val canOpenMedia = selectedItem.mediaUrl.isNotBlank() && selectedItem.mediaUrl != selectedItem.imageUrl
    val isImageType = selectedItem.mediaType.lowercase() != "video"
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
                if (isImageType && displayImageUrl.isNotBlank()) {
                    CircularProgressIndicator(
                        progress = if (imageLoading) imageProgress.coerceIn(0f, 1f) else 1f,
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = Color(0xFF3B82F6),
                            backgroundColor = Color(0x553B82F6)
                        )
                    )
                }
                Box {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = optionLabels.getOrElse(selectedIndex) { "角色表情1" },
                        textColor = Color(0xFF3B82F6),
                        bottomBarStyle = true,
                        onClick = { showPicker = !showPicker }
                    )
                    if (showPicker) {
                        WindowListPopup(
                            show = showPicker,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            onDismissRequest = { showPicker = false },
                            enableWindowDim = false
                        ) {
                            ListPopupColumn {
                                optionLabels.forEachIndexed { idx, option ->
                                    DropdownImpl(
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

            if (displayImageUrl.isNotBlank()) {
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

            if (canOpenMedia) {
                if (selectedItem.mediaType.lowercase() == "video") {
                    GuideInlineVideoPlayer(
                        mediaUrl = displayMediaUrl,
                        backdrop = backdrop,
                        onOpenExternal = onOpenMedia
                    )
                } else {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "打开",
                        leadingIcon = MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        bottomBarStyle = true,
                        onClick = { onOpenMedia(selectedItem.mediaUrl) }
                    )
                }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                bottomBarStyle = true,
                onClick = {}
            )
        }
    }
}

@Composable
private fun GuideInlineVideoPlayer(
    mediaUrl: String,
    backdrop: Backdrop?,
    onOpenExternal: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(mediaUrl) { mutableStateOf(false) }
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    var videoRatio by remember(normalizedUrl) { mutableStateOf(16f / 9f) }
    var isBuffering by remember(normalizedUrl) { mutableStateOf(false) }
    var isPlaying by remember(normalizedUrl) { mutableStateOf(false) }
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }

    if (!expanded) {
        GlassTextButton(
            backdrop = backdrop,
            text = "播放",
            leadingIcon = MiuixIcons.Regular.Play,
            textColor = Color(0xFF3B82F6),
            bottomBarStyle = true,
            onClick = {
                if (normalizedUrl.isBlank()) {
                    Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                    return@GlassTextButton
                }
                loadError = null
                expanded = true
            }
        )
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
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                loadError = error.errorCodeName
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            runCatching { boundPlayer.release() }
            isBuffering = false
            isPlaying = false
        }
    }

    val activePlayer = player
    if (activePlayer == null) {
        GlassTextButton(
            backdrop = backdrop,
            text = "外部打开",
            textColor = Color(0xFF3B82F6),
            bottomBarStyle = true,
            onClick = { onOpenExternal(mediaUrl) }
        )
        return
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
            text = if (isPlaying) "暂停" else "继续",
            leadingIcon = if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
            textColor = Color(0xFF3B82F6),
            bottomBarStyle = true,
            onClick = {
                if (isPlaying) {
                    activePlayer.pause()
                } else {
                    activePlayer.play()
                }
            }
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "外部打开",
            textColor = Color(0xFF3B82F6),
            bottomBarStyle = true,
            onClick = { onOpenExternal(mediaUrl) }
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "收起",
            textColor = Color(0xFF3B82F6),
            bottomBarStyle = true,
            onClick = { expanded = false }
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
            text = "视频播放失败：$err，可尝试外部打开",
            color = MiuixTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildGuideVideoPlayer(context: Context): ExoPlayer {
    val referer = "https://www.gamekee.com/"
    val desktopUa =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
    val defaultHeaders = mapOf(
        "Accept" to "*/*",
        "Accept-Language" to "zh-CN",
        "Referer" to referer,
        "Origin" to "https://www.gamekee.com",
        "User-Agent" to desktopUa,
        "device-num" to "1",
        "game-alias" to "ba",
        "Connection" to "close"
    )
    val httpFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent(desktopUa)
        .setDefaultRequestProperties(defaultHeaders)
    val mediaSourceFactory = DefaultMediaSourceFactory(
        DefaultDataSource.Factory(context, httpFactory)
    )
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
}

@Composable
fun GuideProfileMetaLine(item: BaGuideMetaItem) {
    val isPosition = item.title == "位置"
    val summary = if (isPosition) "" else item.value.ifBlank { "-" }
    val titleSlotWidth = 70.dp
    val iconSlotWidth = 34.dp
    val iconSlotHeight = 24.dp
    val iconWidth = if (isPosition) 30.dp else 20.dp
    val iconHeight = 20.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(titleSlotWidth),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(iconSlotWidth)
                .height(iconSlotHeight),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = item.imageUrl,
                    iconWidth = iconWidth,
                    iconHeight = iconHeight
                )
            }
        }
        if (!isPosition) {
            Text(
                text = summary,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun GuideCombatMetaTile(
    item: BaGuideMetaItem,
    modifier: Modifier = Modifier
) {
    val value = item.value.ifBlank { "-" }
    val adaptiveWide = item.title.contains("战术") || item.title == "武器类型"
    val titleWidth = 112.dp
    val iconWidth = if (adaptiveWide) 28.dp else 18.dp
    val iconHeight = if (adaptiveWide) 18.dp else 18.dp
    val extraIconWidth = 30.dp
    val extraIconHeight = 18.dp
    val iconSlotWidth = 30.dp
    val iconSlotHeight = 22.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.36f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.width(titleWidth),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(iconSlotWidth)
                .height(iconSlotHeight),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = item.imageUrl,
                    iconWidth = iconWidth,
                    iconHeight = iconHeight
                )
            }
        }
        if (item.extraImageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .width(iconSlotWidth)
                    .height(iconSlotHeight),
                contentAlignment = Alignment.Center
            ) {
                GuideRemoteIcon(
                    imageUrl = item.extraImageUrl,
                    iconWidth = extraIconWidth,
                    iconHeight = extraIconHeight
                )
            }
        }
    }
}

@Composable
fun GuideSkillCardItem(
    card: GuideSkillCardModel,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    var showLevelPopup by remember(card.id) { mutableStateOf(false) }
    val levelOptions = card.levelOptions
    var selectedLevel by rememberSaveable(card.id) { mutableStateOf(card.defaultLevel) }

    LaunchedEffect(card.id, card.defaultLevel, levelOptions) {
        if (levelOptions.isEmpty()) {
            selectedLevel = card.defaultLevel
        } else if (selectedLevel !in levelOptions) {
            selectedLevel = card.defaultLevel
        }
    }

    val skillDesc = card.descriptionFor(selectedLevel)
    val skillCost = card.costFor(selectedLevel)
    val displayLevel = selectedLevel.ifBlank { card.defaultLevel }

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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (card.iconUrl.isNotBlank()) {
                        GuideRemoteIcon(
                            imageUrl = card.iconUrl,
                            iconWidth = 34.dp,
                            iconHeight = 34.dp
                        )
                    }
                    Text(
                        text = card.name,
                        modifier = Modifier.weight(1f),
                        color = MiuixTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (card.type.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = card.type,
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            bottomBarStyle = true,
                            onClick = {}
                        )
                    }
                    if (skillCost.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "COST: $skillCost",
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            bottomBarStyle = true,
                            onClick = {}
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuideSkillDescriptionText(
                    description = skillDesc.ifBlank { "暂未解析到技能描述。" },
                    glossaryIcons = card.glossaryIcons,
                    descriptionIcons = card.descriptionIconsFor(selectedLevel),
                    modifier = Modifier.weight(1f)
                )
                if (levelOptions.isNotEmpty()) {
                    Box {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = displayLevel,
                            bottomBarStyle = true,
                            onClick = { showLevelPopup = !showLevelPopup }
                        )
                        if (showLevelPopup) {
                            WindowListPopup(
                                show = showLevelPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                onDismissRequest = { showLevelPopup = false },
                                enableWindowDim = false
                            ) {
                                ListPopupColumn {
                                    levelOptions.forEachIndexed { index, option ->
                                        DropdownImpl(
                                            text = option,
                                            optionSize = levelOptions.size,
                                            isSelected = selectedLevel == option,
                                            index = index,
                                            onSelectedIndexChange = { selected ->
                                                selectedLevel = levelOptions[selected]
                                                showLevelPopup = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideVoiceLanguageCard(
    headers: List<String>,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val visibleHeaders = headers
        .filterNot { header ->
            val value = header.lowercase()
            value.contains("韩") || value.contains("kr") || value.contains("korean")
        }
        .ifEmpty { listOf("日配", "中配") }
        .take(2)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = Color(0x223B82F6),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "配音",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
            visibleHeaders.forEach { header ->
                GlassTextButton(
                    backdrop = backdrop,
                    text = header,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun GuideVoiceEntryCard(
    entry: BaGuideVoiceEntry,
    languageHeaders: List<String>,
    backdrop: Backdrop?,
    isPlaying: Boolean,
    playProgress: Float,
    onTogglePlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = if (languageHeaders.isNotEmpty()) {
        languageHeaders
    } else {
        listOf("日配", "中配")
    }
    val jpLabel = labels.getOrNull(0) ?: "日配"
    val cnLabel = labels.getOrNull(1) ?: "中配"
    val jpText = entry.lines.getOrNull(0).orEmpty().trim()
    val cnText = entry.lines.getOrNull(1).orEmpty().trim()
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
                if (entry.section.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = entry.section,
                        enabled = false,
                        textColor = Color(0xFF3B82F6),
                        bottomBarStyle = true,
                        onClick = {}
                    )
                }
                Text(
                    text = entry.title.ifBlank { "语音条目" },
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (entry.audioUrl.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            CircularProgressIndicator(
                                progress = playProgress.coerceIn(0f, 1f),
                                size = 18.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = Color(0xFF3B82F6),
                                    backgroundColor = Color(0x553B82F6)
                                )
                            )
                        }
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "",
                            leadingIcon = if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                            textColor = Color(0xFF3B82F6),
                            bottomBarStyle = true,
                            onClick = { onTogglePlay(entry.audioUrl) }
                        )
                    }
                }
            }

            val compareLines = listOf(jpLabel to jpText, cnLabel to cnText)
            compareLines.forEach { (label, line) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = label,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.width(58.dp)
                    )
                    Text(
                        text = line.ifBlank { "暂无台词文本" },
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun GuideWeaponCardItem(
    card: GuideWeaponCardModel,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val levelOptions = remember(card.statHeaders) { card.statHeaders.filter { it.isNotBlank() } }
    val defaultLevel = remember(levelOptions) { levelOptions.lastOrNull().orEmpty() }
    var showLevelPopup by remember(card.name, card.imageUrl) { mutableStateOf(false) }
    var selectedLevel by rememberSaveable(card.name, card.imageUrl) { mutableStateOf(defaultLevel) }

    LaunchedEffect(levelOptions, defaultLevel) {
        if (levelOptions.isEmpty()) {
            selectedLevel = ""
        } else if (selectedLevel !in levelOptions) {
            selectedLevel = defaultLevel
        }
    }

    fun levelValue(row: GuideWeaponStatRow): String {
        if (row.values.isEmpty()) return "-"
        if (levelOptions.isEmpty()) return row.values.joinToString(" / ")
        val index = levelOptions.indexOf(selectedLevel).coerceAtLeast(0)
        return row.values.getOrNull(index) ?: row.values.last()
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.name.ifBlank { "专属武器" },
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = "专武",
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }

            Text(
                text = card.description.ifBlank { "暂无专武描述。" },
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            if (card.imageUrl.isNotBlank()) {
                GuideRemoteImage(
                    imageUrl = card.imageUrl,
                    imageHeight = 132.dp
                )
            }

            if (card.statRows.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "专武数值",
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (levelOptions.isNotEmpty()) {
                            Box {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = selectedLevel,
                                    bottomBarStyle = true,
                                    onClick = { showLevelPopup = !showLevelPopup }
                                )
                                if (showLevelPopup) {
                                    WindowListPopup(
                                        show = showLevelPopup,
                                        alignment = PopupPositionProvider.Align.BottomEnd,
                                        onDismissRequest = { showLevelPopup = false },
                                        enableWindowDim = false
                                    ) {
                                        ListPopupColumn {
                                            levelOptions.forEachIndexed { idx, option ->
                                                DropdownImpl(
                                                    text = option,
                                                    optionSize = levelOptions.size,
                                                    isSelected = selectedLevel == option,
                                                    index = idx,
                                                    onSelectedIndexChange = { selected ->
                                                        selectedLevel = levelOptions[selected]
                                                        showLevelPopup = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    card.statRows.forEach { stat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stat.title,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = levelValue(stat),
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (card.starEffects.isNotEmpty()) {
                card.starEffects.forEachIndexed { index, effect ->
                    GuideWeaponStarEffectItem(
                        effect = effect,
                        glossaryIcons = card.glossaryIcons,
                        backdrop = backdrop
                    )
                    if (index < card.starEffects.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideWeaponStarEffectItem(
    effect: GuideWeaponStarEffect,
    glossaryIcons: Map<String, String>,
    backdrop: Backdrop?
) {
    var showLevelPopup by remember(effect.id) { mutableStateOf(false) }
    val levelOptions = effect.levelOptions
    var selectedLevel by rememberSaveable(effect.id) { mutableStateOf(effect.defaultLevel) }

    LaunchedEffect(effect.id, effect.defaultLevel, levelOptions) {
        if (levelOptions.isEmpty()) {
            selectedLevel = effect.defaultLevel
        } else if (selectedLevel !in levelOptions) {
            selectedLevel = effect.defaultLevel
        }
    }

    val desc = effect.descriptionFor(selectedLevel).trim()

    if (effect.starLabel == "★2") {
        GuideWeaponTwoStarEffectItem(
            effect = effect,
            desc = desc,
            glossaryIcons = glossaryIcons,
            backdrop = backdrop,
            levelOptions = levelOptions,
            selectedLevel = selectedLevel,
            showLevelPopup = showLevelPopup,
            onTogglePopup = { showLevelPopup = !showLevelPopup },
            onDismissPopup = { showLevelPopup = false },
            onLevelSelected = { selected ->
                selectedLevel = levelOptions[selected]
                showLevelPopup = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.28f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuideWeaponStarBadgeRow(effect.starLabel, iconSize = 18.dp)
            if (effect.roleTag.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = effect.roleTag,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }
            Text(
                text = effect.name.ifBlank { "效果" },
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (effect.iconUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = effect.iconUrl,
                    iconWidth = 20.dp,
                    iconHeight = 20.dp
                )
            }
            GuideEffectLevelPicker(
                backdrop = backdrop,
                levelOptions = levelOptions,
                selectedLevel = selectedLevel,
                showLevelPopup = showLevelPopup,
                onTogglePopup = { showLevelPopup = !showLevelPopup },
                onDismissPopup = { showLevelPopup = false },
                onLevelSelected = { selected ->
                    selectedLevel = levelOptions[selected]
                    showLevelPopup = false
                }
            )
        }

        if (desc.isNotBlank()) {
            GuideSkillDescriptionText(
                description = desc,
                glossaryIcons = glossaryIcons,
                descriptionIcons = effect.descriptionIconsFor(selectedLevel)
            )
        }
    }
}

@Composable
private fun GuideWeaponTwoStarEffectItem(
    effect: GuideWeaponStarEffect,
    desc: String,
    glossaryIcons: Map<String, String>,
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.34f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuideWeaponStarBadgeRow(effect.starLabel, iconSize = 19.dp)
            if (effect.iconUrl.isNotBlank()) {
                GuideRemoteIcon(
                    imageUrl = effect.iconUrl,
                    iconWidth = 20.dp,
                    iconHeight = 20.dp
                )
            }
            Text(
                text = effect.name.ifBlank { "辅助技能强化" },
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (effect.roleTag.isNotBlank()) {
                GlassTextButton(
                    backdrop = backdrop,
                    text = effect.roleTag,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    bottomBarStyle = true,
                    onClick = {}
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuideSkillDescriptionText(
                description = desc.ifBlank { "暂无效果描述。" },
                glossaryIcons = glossaryIcons,
                descriptionIcons = effect.descriptionIconsFor(selectedLevel),
                modifier = Modifier.weight(1f)
            )
            GuideEffectLevelPicker(
                backdrop = backdrop,
                levelOptions = levelOptions,
                selectedLevel = selectedLevel,
                showLevelPopup = showLevelPopup,
                onTogglePopup = onTogglePopup,
                onDismissPopup = onDismissPopup,
                onLevelSelected = onLevelSelected
            )
        }
    }
}

@Composable
private fun GuideEffectLevelPicker(
    backdrop: Backdrop?,
    levelOptions: List<String>,
    selectedLevel: String,
    showLevelPopup: Boolean,
    onTogglePopup: () -> Unit,
    onDismissPopup: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    if (levelOptions.isEmpty()) return
    Box {
        GlassTextButton(
            backdrop = backdrop,
            text = selectedLevel,
            bottomBarStyle = true,
            onClick = onTogglePopup
        )
        if (showLevelPopup) {
            WindowListPopup(
                show = showLevelPopup,
                alignment = PopupPositionProvider.Align.BottomEnd,
                onDismissRequest = onDismissPopup,
                enableWindowDim = false
            ) {
                ListPopupColumn {
                    levelOptions.forEachIndexed { idx, option ->
                        DropdownImpl(
                            text = option,
                            optionSize = levelOptions.size,
                            isSelected = selectedLevel == option,
                            index = idx,
                            onSelectedIndexChange = onLevelSelected
                        )
                    }
                }
            }
        }
    }
}

private data class SkillDescriptionRichText(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
private fun GuideSkillDescriptionText(
    description: String,
    glossaryIcons: Map<String, String>,
    descriptionIcons: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val textColor = MiuixTheme.colorScheme.onBackground
    val richText = remember(description, glossaryIcons, descriptionIcons, textColor) {
        buildSkillDescriptionRichText(
            description = description,
            glossaryIcons = glossaryIcons,
            leadingIcons = descriptionIcons,
            numberColor = Color(0xFFD84A40)
        )
    }
    BasicText(
        text = richText.text,
        inlineContent = richText.inlineContent,
        style = TextStyle(
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        modifier = modifier
    )
}

private fun buildSkillDescriptionRichText(
    description: String,
    glossaryIcons: Map<String, String>,
    leadingIcons: List<String>,
    numberColor: Color
): SkillDescriptionRichText {
    if (description.isBlank()) {
        return SkillDescriptionRichText(AnnotatedString(""), emptyMap())
    }

    val numberRegex = Regex("""(?<![A-Za-z])[-+]?\d+(?:\.\d+)?%?""")
    val glossary = glossaryIcons
        .filter { (label, icon) -> label.isNotBlank() && icon.isNotBlank() }
        .entries
        .sortedByDescending { it.key.length }

    val inlineContent = linkedMapOf<String, InlineTextContent>()
    var inlineCounter = 0
    val normalizedDescription = normalizeGlossaryToken(description)
    val fuzzyLeadingIcons = glossary
        .asSequence()
        .filter { entry ->
            val label = entry.key
            if (description.contains(label)) return@filter false
            val normalizedLabel = normalizeGlossaryToken(label)
            normalizedLabel.isNotBlank() && normalizedDescription.contains(normalizedLabel)
        }
        .map { it.value }
        .distinct()
        .take(6)
        .toList()
    val prefixIcons = (leadingIcons + fuzzyLeadingIcons)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    val text = buildAnnotatedString {
        prefixIcons
            .forEach { iconUrl ->
                val inlineId = "skill_icon_prefix_$inlineCounter"
                inlineCounter += 1
                inlineContent[inlineId] = InlineTextContent(
                    Placeholder(
                        width = 15.sp,
                        height = 15.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 15.dp,
                        iconHeight = 15.dp
                    )
                }
                appendInlineContent(inlineId, "[图标]")
                append(" ")
            }
        var index = 0
        while (index < description.length) {
            val glossaryMatch = glossary
                .mapNotNull { entry ->
                    val start = description.indexOf(entry.key, index)
                    if (start < 0) null else Triple(start, entry.key, entry.value)
                }
                .minByOrNull { it.first }
            val numberMatch = numberRegex.find(description, index)

            val nextIsGlossary = when {
                glossaryMatch == null -> false
                numberMatch == null -> true
                else -> glossaryMatch.first <= numberMatch.range.first
            }

            if (glossaryMatch == null && numberMatch == null) {
                append(description.substring(index))
                break
            }

            if (nextIsGlossary) {
                val (start, label, iconUrl) = glossaryMatch ?: continue
                if (start > index) {
                    append(description.substring(index, start))
                }
                val inlineId = "skill_icon_$inlineCounter"
                inlineCounter += 1
                inlineContent[inlineId] = InlineTextContent(
                    Placeholder(
                        width = 15.sp,
                        height = 15.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 15.dp,
                        iconHeight = 15.dp
                    )
                }
                appendInlineContent(inlineId, "[图标]")
                append(label)
                index = start + label.length
            } else {
                val number = numberMatch ?: continue
                if (number.range.first > index) {
                    append(description.substring(index, number.range.first))
                }
                withStyle(SpanStyle(color = numberColor, fontWeight = FontWeight.SemiBold)) {
                    append(number.value)
                }
                index = number.range.last + 1
            }
        }
    }
    return SkillDescriptionRichText(text, inlineContent)
}

private fun normalizeGlossaryToken(raw: String): String {
    return raw
        .replace(Regex("""[\s\u3000]"""), "")
        .replace(Regex("""[，。、“”‘’：:；;（）()【】\[\]《》<>·•\-—_+*/\\|!?！？]"""), "")
        .lowercase()
}

@Composable
private fun GuideWeaponStarBadgeRow(
    starLabel: String,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val count = parseWeaponStarCount(starLabel)
    if (count <= 0) {
        Text(
            text = starLabel,
            color = Color(0xFFEC4899)
        )
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count.coerceAtMost(5)) {
            Image(
                painter = painterResource(R.drawable.ba_weapon_star_badge),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private fun parseWeaponStarCount(starLabel: String): Int {
    return Regex("""(\d{1,2})""")
        .find(starLabel)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: 0
}

fun showLoadingText(loading: Boolean, hasInfo: Boolean): Boolean {
    return loading && hasInfo
}
