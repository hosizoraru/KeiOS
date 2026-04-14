package com.example.keios.ui.page.main.student

import com.example.keios.ui.page.main.widget.GlassVariant
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.rotate
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
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.R
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.capturePopupAnchor

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
    maxDecodeDimension: Int = 2048,
    onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
): Bitmap? {
    if (source.isBlank()) return null
    val uri = runCatching { Uri.parse(source) }.getOrNull()
    if (uri?.scheme.equals("file", ignoreCase = true)) {
        val path = uri?.path.orEmpty().ifBlank { Uri.decode(uri?.encodedPath.orEmpty()) }
        if (path.isBlank()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            return BitmapFactory.decodeFile(path)
        }
        var sample = 1
        val safeMax = maxDecodeDimension.coerceAtLeast(512)
        while ((srcWidth / sample) > safeMax || (srcHeight / sample) > safeMax) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }
    return if (onProgress != null) {
        GameKeeFetchHelper.fetchImageWithProgress(
            imageUrl = source,
            onProgress = onProgress,
            maxDecodeDimension = maxDecodeDimension
        )
    } else {
        GameKeeFetchHelper.fetchImage(
            imageUrl = source,
            maxDecodeDimension = maxDecodeDimension
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun normalizeGalleryDisplayTitle(title: String, mediaType: String): String {
    val raw = title.trim().ifBlank { "影画条目" }
    if (mediaType.lowercase() != "audio") return raw
    return if (raw.startsWith("BGM", ignoreCase = true)) {
        raw.replaceFirst(Regex("^BGM", RegexOption.IGNORE_CASE), "Live2d BGM")
    } else {
        raw
    }
}

@Composable
fun GuideRemoteImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    imageHeight: androidx.compose.ui.unit.Dp = 220.dp,
    maxDecodeDimension: Int = 1920
) {
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching { loadGuideBitmapSource(target, maxDecodeDimension = maxDecodeDimension) }.getOrNull()
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
    maxDecodeDimension: Int = 2048,
    progressState: MutableStateFlow<Float>? = null,
    onLoadingChanged: ((Boolean) -> Unit)? = null
) {
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) {
        progressState?.value = 1f
        onLoadingChanged?.invoke(false)
        return
    }
    if (isGifMediaSource(target)) {
        progressState?.value = 1f
        onLoadingChanged?.invoke(false)
        val ratio = remember(target) { detectMediaRatioFromUrl(target) ?: (16f / 9f) }
        AsyncImage(
            model = target,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(14.dp))
        )
        return
    }
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        progressState?.value = 0f
        onLoadingChanged?.invoke(true)
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(
                    source = target,
                    maxDecodeDimension = maxDecodeDimension
                ) { downloadedBytes, totalBytes ->
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

private fun isGifMediaSource(source: String): Boolean {
    val value = source.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:image/gif", ignoreCase = true)) return true
    return Regex("""\.gif(\?.*)?(#.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(value)
}

private fun detectMediaRatioFromUrl(source: String): Float? {
    val match = Regex("""/w_(\d{1,4})/h_(\d{1,4})/""")
        .find(source)
    val width = match?.groupValues?.getOrNull(1)?.toFloatOrNull()
    val height = match?.groupValues?.getOrNull(2)?.toFloatOrNull()
    if (width == null || height == null || width <= 0f || height <= 0f) return null
    val ratio = width / height
    if (ratio.isNaN() || ratio.isInfinite()) return null
    return ratio.coerceIn(0.4f, 4f)
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
    embedded: Boolean = false,
    showMediaTypeLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalizedMediaType = item.mediaType.lowercase()
    val mediaTypeLabel = when (normalizedMediaType) {
        "video" -> "视频"
        "audio" -> ""
        "live2d" -> "Live2D"
        "imageset" -> "图集"
        else -> "影画"
    }
    val displayImageUrl = mediaUrlResolver(item.imageUrl)
    val displayMediaUrl = mediaUrlResolver(item.mediaUrl)
    val noteText = item.note.trim()
    val displayTitle = remember(item.title, normalizedMediaType) {
        normalizeGalleryDisplayTitle(item.title, normalizedMediaType)
    }
    val isImageType = normalizedMediaType != "video" && normalizedMediaType != "audio"
    val canOpenMedia = item.mediaUrl.isNotBlank() && item.mediaUrl != item.imageUrl
    var showImageFullscreen by remember(displayImageUrl, normalizedMediaType) { mutableStateOf(false) }
    val audioTargetUrl = remember(normalizedMediaType, displayMediaUrl) {
        if (normalizedMediaType == "audio") normalizeGuideMediaSource(displayMediaUrl) else ""
    }
    var audioIsPlaying by remember(audioTargetUrl) { mutableStateOf(false) }
    var audioIsBuffering by remember(audioTargetUrl) { mutableStateOf(false) }
    var audioPlayProgress by remember(audioTargetUrl) { mutableStateOf(0f) }
    var audioLoadError by remember(audioTargetUrl) { mutableStateOf<String?>(null) }
    val audioPlayer = remember(context, audioTargetUrl) {
        if (audioTargetUrl.isNotBlank()) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
                .build()
        } else {
            null
        }
    }
    DisposableEffect(audioPlayer) {
        onDispose {
            runCatching { audioPlayer?.release() }
        }
    }
    DisposableEffect(audioPlayer, audioTargetUrl) {
        val player = audioPlayer ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                audioIsPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> audioIsBuffering = true
                    Player.STATE_READY -> audioIsBuffering = false
                    Player.STATE_ENDED -> {
                        audioIsBuffering = false
                        audioIsPlaying = false
                        audioPlayProgress = 1f
                    }
                    Player.STATE_IDLE -> {
                        audioIsBuffering = false
                        if (!player.isPlaying) audioPlayProgress = 0f
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
    LaunchedEffect(audioIsPlaying, audioTargetUrl) {
        if (!audioIsPlaying) {
            val player = audioPlayer
            if (player == null || player.duration <= 0L || player.currentPosition <= 0L) {
                audioPlayProgress = 0f
            }
            return@LaunchedEffect
        }
        val player = audioPlayer ?: return@LaunchedEffect
        while (audioIsPlaying) {
            val duration = player.duration
            val position = player.currentPosition
            audioPlayProgress = if (duration > 0L && position >= 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
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
                if (normalizedMediaType == "audio" && audioTargetUrl.isNotBlank()) {
                    if (audioIsPlaying || audioIsBuffering) {
                        CircularProgressIndicator(
                            progress = if (audioIsBuffering) 0.35f else audioPlayProgress.coerceIn(0f, 1f),
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

            if (noteText.isNotBlank()) {
                Text(
                    text = noteText,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (displayImageUrl.isNotBlank() && normalizedMediaType != "video" && normalizedMediaType != "audio") {
                Box(
                    modifier = Modifier.clickable { showImageFullscreen = true }
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
                            onOpenExternal = onOpenMedia
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
            onDismiss = { showImageFullscreen = false }
        )
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
    var showImageFullscreen by remember(displayImageUrl) { mutableStateOf(false) }
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
                var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                Box(
                    modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it }
                ) {
                    GlassTextButton(
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

            if (displayImageUrl.isNotBlank() && selectedItem.mediaType.lowercase() != "video") {
                Box(
                    modifier = Modifier.clickable { showImageFullscreen = true }
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
                        onOpenExternal = onOpenMedia
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
    val displayMediaUrl = mediaUrlResolver(selectedItem.mediaUrl)
    val displayPreviewUrl = mediaUrlResolver(
        selectedItem.imageUrl.ifBlank { previewFallbackUrl }
    )
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
                        GlassTextButton(
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
                GlassTextButton(
                    backdrop = backdrop,
                    text = "视频",
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
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
                    onOpenExternal = onOpenMedia
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
                variant = GlassVariant.Compact,
                onClick = {}
            )
        }
    }
}

@Composable
private fun GuideInlineVideoPlayer(
    mediaUrl: String,
    previewImageUrl: String = "",
    backdrop: Backdrop?,
    onOpenExternal: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(mediaUrl) { mutableStateOf(false) }
    var showFullscreen by rememberSaveable(mediaUrl) { mutableStateOf(false) }
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    val normalizedPreviewUrl = remember(previewImageUrl) { normalizeGuideMediaSource(previewImageUrl) }
    var videoRatio by remember(normalizedUrl) { mutableStateOf(16f / 9f) }
    var isBuffering by remember(normalizedUrl) { mutableStateOf(false) }
    var isPlaying by remember(normalizedUrl) { mutableStateOf(false) }
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }

    if (showFullscreen && normalizedUrl.isNotBlank()) {
        GuideVideoFullscreenDialog(
            mediaUrl = normalizedUrl,
            onDismiss = { showFullscreen = false }
        )
    }

    if (!expanded) {
        if (normalizedPreviewUrl.isNotBlank()) {
            Box(
                modifier = Modifier.clickable {
                    if (normalizedUrl.isBlank()) {
                        Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                    } else {
                        showFullscreen = true
                    }
                }
            ) {
                GuideRemoteImageAdaptive(
                    imageUrl = normalizedPreviewUrl
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTextButton(
                backdrop = backdrop,
                text = "播放",
                leadingIcon = MiuixIcons.Regular.Play,
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.Compact,
                onClick = {
                    if (normalizedUrl.isBlank()) {
                        Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                        return@GlassTextButton
                    }
                    loadError = null
                    expanded = true
                }
            )
            GlassTextButton(
                backdrop = backdrop,
                text = "全屏",
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.Compact,
                onClick = {
                    if (normalizedUrl.isBlank()) {
                        Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                        return@GlassTextButton
                    }
                    showFullscreen = true
                }
            )
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
            variant = GlassVariant.Compact,
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
            variant = GlassVariant.Compact,
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
            text = "全屏",
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = { showFullscreen = true }
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "外部打开",
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = { onOpenExternal(mediaUrl) }
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "收起",
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
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

@Composable
private fun GuideRequestedOrientationEffect(requestedOrientation: Int) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, requestedOrientation) {
        if (activity == null || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            onDispose { }
        } else {
            val previousOrientation = activity.requestedOrientation
            val applied = runCatching {
                activity.requestedOrientation = requestedOrientation
            }.isSuccess
            onDispose {
                if (applied) {
                    runCatching {
                        activity.requestedOrientation = previousOrientation
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideImageFullscreenDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val normalizedImageUrl = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (normalizedImageUrl.isBlank()) return
    val sampledBitmap by produceState<Bitmap?>(initialValue = null, normalizedImageUrl) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(
                    source = normalizedImageUrl,
                    maxDecodeDimension = 1024
                )
            }.getOrNull()
        }
    }
    val ratio = remember(sampledBitmap?.width, sampledBitmap?.height) {
        val width = sampledBitmap?.width ?: 0
        val height = sampledBitmap?.height ?: 0
        if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
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
            if (ratio > 1.02f) {
                val rotatedRatio = if (ratio > 0f) (1f / ratio) else 1f
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CoilZoomAsyncImage(
                        model = normalizedImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(rotatedRatio)
                            .rotate(90f),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                CoilZoomAsyncImage(
                    model = normalizedImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
            GlassTextButton(
                backdrop = null,
                text = "关闭",
                textColor = Color(0xFFBFDBFE),
                variant = GlassVariant.Compact,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 16.dp),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun GuideVideoFullscreenDialog(
    mediaUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    GuideRequestedOrientationEffect(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }

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
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
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
            } else {
                Text(
                    text = "视频地址无效",
                    color = Color(0xFFBFDBFE),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            GlassTextButton(
                backdrop = null,
                text = "关闭",
                textColor = Color(0xFFBFDBFE),
                variant = GlassVariant.Compact,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 16.dp),
                onClick = onDismiss
            )

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

private fun buildGuideVideoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
        .build()
}

private fun adaptiveValueMaxLines(value: String, lineCharBudget: Int): Int {
    val normalized = value.trim()
    if (normalized.isBlank()) return 1
    if (normalized.contains('\n')) return 2
    val budget = lineCharBudget.coerceIn(10, 52)
    val breakable = normalized.any {
        it == ' ' || it == '/' || it == '-' || it == ',' || it == '，' || it == '、' || it == ':' || it == '：'
    }
    val veryLong = normalized.length > budget + budget / 2
    val longAndBreakable = breakable && normalized.length > budget
    return if (veryLong || longAndBreakable) 2 else 1
}

@Composable
fun GuideProfileMetaLine(item: BaGuideMetaItem) {
    val isPosition = item.title == "位置"
    val isRarity = item.title == "稀有度"
    val inlineTitleIcon = isRarity || item.title == "学院"
    val summary = if (isPosition) "" else item.value.ifBlank { "-" }
    val iconSlotWidth = 34.dp
    val iconSlotHeight = 24.dp
    val iconWidth = when {
        isRarity -> 30.dp
        isPosition -> 30.dp
        else -> 20.dp
    }
    val iconHeight = when {
        isRarity -> 24.dp
        else -> 20.dp
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val titleMaxWidth = (maxWidth * 0.48f).coerceIn(80.dp, 176.dp)
        val valueCharBudget = ((maxWidth - titleMaxWidth).value / 7.2f).toInt().coerceAtLeast(10)
        val summaryMaxLines = adaptiveValueMaxLines(summary, valueCharBudget)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inlineTitleIcon) {
                Row(
                    modifier = Modifier.widthIn(max = titleMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    if (item.imageUrl.isNotBlank()) {
                        GuideRemoteIcon(
                            imageUrl = item.imageUrl,
                            iconWidth = iconWidth,
                            iconHeight = iconHeight
                        )
                    }
                }
            } else {
                val hasLeadingIcon = item.imageUrl.isNotBlank()
                Row(
                    modifier = Modifier.widthIn(max = titleMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    if (hasLeadingIcon) {
                        Box(
                            modifier = Modifier
                                .width(iconSlotWidth)
                                .height(iconSlotHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            GuideRemoteIcon(
                                imageUrl = item.imageUrl,
                                iconWidth = iconWidth,
                                iconHeight = iconHeight
                            )
                        }
                    }
                }
            }
            if (!isPosition) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = summary,
                        color = MiuixTheme.colorScheme.onBackground,
                        textAlign = TextAlign.End,
                        maxLines = summaryMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
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
    val iconWidth = if (adaptiveWide) 28.dp else 18.dp
    val iconHeight = if (adaptiveWide) 18.dp else 18.dp
    val extraIconWidth = 30.dp
    val extraIconHeight = 18.dp
    val iconSlotWidth = 30.dp
    val iconSlotHeight = 22.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.36f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val trailingSlotsWidth = if (item.extraImageUrl.isNotBlank()) iconSlotWidth * 2 else iconSlotWidth
        val titleMaxWidth = ((maxWidth - trailingSlotsWidth) * if (adaptiveWide) 0.52f else 0.46f)
            .coerceIn(86.dp, 180.dp)
        val valueCharBudget = ((maxWidth - titleMaxWidth - trailingSlotsWidth).value / 7.2f)
            .toInt()
            .coerceAtLeast(10)
        val valueMaxLines = adaptiveValueMaxLines(value, valueCharBudget)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.widthIn(max = titleMaxWidth),
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            Text(
                text = value,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = valueMaxLines,
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
                            variant = GlassVariant.Compact,
                            onClick = {}
                        )
                    }
                    if (skillCost.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "COST: $skillCost",
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            variant = GlassVariant.Compact,
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
                    var levelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                    Box(
                        modifier = Modifier.capturePopupAnchor { levelPopupAnchorBounds = it }
                    ) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = displayLevel,
                            variant = GlassVariant.Compact,
                            onClick = { showLevelPopup = !showLevelPopup }
                        )
                        if (showLevelPopup) {
                            SnapshotWindowListPopup(
                                show = showLevelPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = levelPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { showLevelPopup = false },
                                enableWindowDim = false
                            ) {
                                LiquidDropdownColumn {
                                    levelOptions.forEachIndexed { index, option ->
                                        LiquidDropdownImpl(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuideVoiceLanguageCard(
    headers: List<String>,
    selectedHeader: String,
    backdrop: Backdrop?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleHeaders = headers
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf("日配", "中配") }
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "配音",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleHeaders.forEach { header ->
                    val selected = header.equals(selectedHeader.trim(), ignoreCase = true)
                    GlassTextButton(
                        backdrop = backdrop,
                        text = header,
                        textColor = if (selected) Color(0xFF2563EB) else MiuixTheme.colorScheme.onBackgroundVariant,
                        containerColor = if (selected) Color(0x443B82F6) else null,
                        variant = GlassVariant.Compact,
                        onClick = { onSelected(header) }
                    )
                }
            }
        }
    }
}

@Composable
fun GuideVoiceEntryCard(
    entry: BaGuideVoiceEntry,
    languageHeaders: List<String>,
    dubbingHeaders: List<String>,
    selectedDubbingHeader: String,
    officialTranslationHeader: String,
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
    val dubbingLines = buildList {
        dubbingHeaders.forEach { header ->
            val index = labels.indexOfFirst { label -> label.equals(header, ignoreCase = true) }
            if (index < 0) return@forEach
            val line = entry.lines.getOrNull(index).orEmpty().trim()
            if (line.isNotBlank()) {
                add(header to line)
            }
        }
    }
    val selectedLine = dubbingLines.firstOrNull { (label, _) ->
        label.equals(selectedDubbingHeader.trim(), ignoreCase = true)
    }
    val orderedDubbingLines = buildList {
        if (selectedLine != null) add(selectedLine)
        dubbingLines.forEach { line ->
            if (selectedLine == null || !line.first.equals(selectedLine.first, ignoreCase = true)) {
                add(line)
            }
        }
    }
    val officialTranslationLine = if (officialTranslationHeader.isNotBlank()) {
        val index = labels.indexOfFirst { label ->
            label.equals(officialTranslationHeader, ignoreCase = true)
        }
        if (index >= 0) {
            entry.lines.getOrNull(index).orEmpty().trim()
                .takeIf { it.isNotBlank() }
                ?.let { officialTranslationHeader to it }
        } else {
            null
        }
    } else {
        null
    }
    val voiceLines = buildList {
        addAll(orderedDubbingLines)
        if (officialTranslationLine != null) {
            add(officialTranslationLine)
        }
    }.ifEmpty {
        listOf("台词" to "暂无台词文本")
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
                if (entry.section.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = entry.section,
                        enabled = false,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
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
                            variant = GlassVariant.Compact,
                            onClick = { onTogglePlay(entry.audioUrl) }
                        )
                    }
                }
            }

            voiceLines.forEach { (label, text) ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val labelMaxWidth = (maxWidth * 0.28f).coerceIn(52.dp, 92.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = label,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.widthIn(max = labelMaxWidth),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = text,
                            color = MiuixTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Clip
                        )
                    }
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
                    variant = GlassVariant.Compact,
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
                            var levelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                            Box(
                                modifier = Modifier.capturePopupAnchor { levelPopupAnchorBounds = it }
                            ) {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = selectedLevel,
                                    variant = GlassVariant.Compact,
                                    onClick = { showLevelPopup = !showLevelPopup }
                                )
                                if (showLevelPopup) {
                                    SnapshotWindowListPopup(
                                        show = showLevelPopup,
                                        alignment = PopupPositionProvider.Align.BottomEnd,
                                        anchorBounds = levelPopupAnchorBounds,
                                        placement = SnapshotPopupPlacement.ButtonEnd,
                                        onDismissRequest = { showLevelPopup = false },
                                        enableWindowDim = false
                                    ) {
                                        LiquidDropdownColumn {
                                            levelOptions.forEachIndexed { idx, option ->
                                                LiquidDropdownImpl(
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
                        val valueText = levelValue(stat)
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val titleMaxWidth = (maxWidth * 0.34f).coerceIn(64.dp, 128.dp)
                            val valueCharBudget = ((maxWidth - titleMaxWidth).value / 7f).toInt().coerceAtLeast(10)
                            val valueMaxLines = adaptiveValueMaxLines(valueText, valueCharBudget)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stat.title,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    modifier = Modifier.widthIn(max = titleMaxWidth),
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    text = valueText,
                                    color = MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                    maxLines = valueMaxLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (card.extraStatRows.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "附加属性（25级）",
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                    card.extraStatRows.forEach { stat ->
                        val valueText = stat.values.firstOrNull().orEmpty().ifBlank { "-" }
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val titleMaxWidth = (maxWidth * 0.34f).coerceIn(64.dp, 128.dp)
                            val valueCharBudget = ((maxWidth - titleMaxWidth).value / 7f).toInt().coerceAtLeast(10)
                            val valueMaxLines = adaptiveValueMaxLines(valueText, valueCharBudget)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stat.title,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    modifier = Modifier.widthIn(max = titleMaxWidth),
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    text = valueText,
                                    color = MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                    maxLines = valueMaxLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                    variant = GlassVariant.Compact,
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
                    variant = GlassVariant.Compact,
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
    var levelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    Box(
        modifier = Modifier.capturePopupAnchor { levelPopupAnchorBounds = it }
    ) {
        GlassTextButton(
            backdrop = backdrop,
            text = selectedLevel,
            variant = GlassVariant.Compact,
            onClick = onTogglePopup
        )
        if (showLevelPopup) {
            SnapshotWindowListPopup(
                show = showLevelPopup,
                alignment = PopupPositionProvider.Align.BottomEnd,
                anchorBounds = levelPopupAnchorBounds,
                placement = SnapshotPopupPlacement.ButtonEnd,
                onDismissRequest = onDismissPopup,
                enableWindowDim = false
            ) {
                LiquidDropdownColumn {
                    levelOptions.forEachIndexed { idx, option ->
                        LiquidDropdownImpl(
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
