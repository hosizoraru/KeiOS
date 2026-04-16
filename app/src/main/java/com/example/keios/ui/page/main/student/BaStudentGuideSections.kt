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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.keios.R
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import top.yukonga.miuix.kmp.icon.extended.Close
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

private const val IMAGE_TAP_DISMISS_GESTURE_COOLDOWN_MS = 260L
private val guideCircledNumbers = listOf(
    "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
    "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳"
)
private val guideSkillTypeNumericSuffixPattern = Regex("""^(.*?)[\s\-_]*(\d{1,2})$""")
private val guideSkillTypeCircledSuffixPattern = Regex("""^(.*?)([①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳])$""")
private val guideSkillTypeBracketPattern = Regex("""[（(【\[]([^()（）【】\[\]]+)[)）】\]]""")
private val guideSkillTypeStateSplitPattern = Regex("""\s*[、,，/／|｜+＋]\s*""")

private enum class GuideVideoControlAction {
    TogglePlayPause
}

private object GuideBgmLoopStore {
    private val loopByScopedAudio = ConcurrentHashMap<String, Boolean>()

    private fun scopedKey(scopeKey: String, audioUrl: String): String {
        if (scopeKey.isBlank() || audioUrl.isBlank()) return ""
        return "$scopeKey|$audioUrl"
    }

    fun isEnabled(scopeKey: String, audioUrl: String): Boolean {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return false
        return loopByScopedAudio[key] == true
    }

    fun setEnabled(scopeKey: String, audioUrl: String, enabled: Boolean) {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return
        if (enabled) {
            loopByScopedAudio[key] = true
        } else {
            loopByScopedAudio.remove(key)
        }
    }

    fun clearScope(scopeKey: String) {
        if (scopeKey.isBlank()) return
        val prefix = "$scopeKey|"
        loopByScopedAudio.keys.forEach { key ->
            if (key.startsWith(prefix)) {
                loopByScopedAudio.remove(key)
            }
        }
    }
}

internal fun clearGuideBgmLoopScope(scopeKey: String) {
    GuideBgmLoopStore.clearScope(scopeKey)
}

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

private fun isSystemAutoRotateEnabled(context: Context): Boolean {
    return runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0
        ) == 1
    }.getOrDefault(false)
}

private fun currentDisplayRotationDegrees(context: Context): Int {
    val rotation = runCatching { context.display.rotation }
        .getOrElse {
            runCatching {
                context.getSystemService(DisplayManager::class.java)
                    ?.getDisplay(Display.DEFAULT_DISPLAY)
                    ?.rotation
                    ?: Surface.ROTATION_0
            }.getOrDefault(Surface.ROTATION_0)
        }
    return when (rotation) {
        Surface.ROTATION_90 -> 270
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 90
        else -> 0
    }
}

private fun normalizeRotationDegreesByOrientation(rawOrientation: Int): Int {
    val orientation = ((rawOrientation % 360) + 360) % 360
    return when {
        orientation in 45..134 -> 270
        orientation in 135..224 -> 180
        orientation in 225..314 -> 90
        else -> 0
    }
}

private fun circularAngleDistance(a: Int, b: Int): Int {
    val diff = kotlin.math.abs(a - b) % 360
    return kotlin.math.min(diff, 360 - diff)
}

private fun snapCardinalOrientation(rawOrientation: Int): Int? {
    val orientation = ((rawOrientation % 360) + 360) % 360
    val candidates = intArrayOf(0, 90, 180, 270)
    var best = 0
    var bestDistance = Int.MAX_VALUE
    candidates.forEach { candidate ->
        val distance = circularAngleDistance(orientation, candidate)
        if (distance < bestDistance) {
            bestDistance = distance
            best = candidate
        }
    }
    return if (bestDistance <= 24) best else null
}

@Composable
private fun rememberSystemAutoRotateEnabled(active: Boolean): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isSystemAutoRotateEnabled(context)) }
    DisposableEffect(context, active) {
        if (!active) {
            enabled = false
            return@DisposableEffect onDispose { }
        }
        enabled = isSystemAutoRotateEnabled(context)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = isSystemAutoRotateEnabled(context)
            }
        }
        val uri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose {
            runCatching {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }
    return enabled
}

@Composable
private fun rememberDeviceRotationDegrees(active: Boolean): Int {
    val context = LocalContext.current
    var degrees by remember { mutableStateOf(0) }
    DisposableEffect(context, active) {
        if (!active) {
            degrees = 0
            return@DisposableEffect onDispose { }
        }
        degrees = currentDisplayRotationDegrees(context)
        val handler = Handler(Looper.getMainLooper())
        var pendingDegrees = degrees
        var pendingApplyRunnable: Runnable? = null
        val listener = object : OrientationEventListener(context.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val snapped = snapCardinalOrientation(orientation) ?: return
                val nextDegrees = normalizeRotationDegreesByOrientation(snapped)
                if (nextDegrees == degrees) {
                    pendingApplyRunnable?.let(handler::removeCallbacks)
                    pendingApplyRunnable = null
                    pendingDegrees = degrees
                    return
                }
                pendingDegrees = nextDegrees
                pendingApplyRunnable?.let(handler::removeCallbacks)
                val applyRunnable = Runnable {
                    if (pendingDegrees != degrees) {
                        degrees = pendingDegrees
                    }
                }
                pendingApplyRunnable = applyRunnable
                handler.postDelayed(applyRunnable, 120L)
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose {
            listener.disable()
            pendingApplyRunnable?.let(handler::removeCallbacks)
            pendingApplyRunnable = null
        }
    }
    return degrees
}

private fun loadGuideBitmapSource(
    context: Context,
    source: String,
    maxDecodeDimension: Int = 2048,
    onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
): Bitmap? {
    if (source.isBlank()) return null
    return BaGuideImageCache.loadBitmap(
        context = context,
        source = source,
        maxDecodeDimension = maxDecodeDimension,
        onProgress = onProgress
    )
}

private fun normalizeGalleryDisplayTitle(title: String, mediaType: String): String {
    val raw = title.trim().ifBlank { "影画条目" }
    if (mediaType.lowercase() != "audio") return raw
    return if (raw.startsWith("BGM", ignoreCase = true)) {
        raw.replaceFirst(Regex("^BGM", RegexOption.IGNORE_CASE), "回忆大厅BGM")
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
    val context = LocalContext.current
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(
                    context = context,
                    source = target,
                    maxDecodeDimension = maxDecodeDimension
                )
            }.getOrNull()
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
    val context = LocalContext.current
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) {
        progressState?.value = 1f
        onLoadingChanged?.invoke(false)
        return
    }
    val isGifSource = remember(target) { isGifMediaSource(target) }
    if (isGifSource) {
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
                    context = context,
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
    if (Regex("""\.gif(\?.*)?(#.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(value)) return true
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    if (!uri?.scheme.equals("file", ignoreCase = true)) return false
    val path = uri?.path.orEmpty().ifBlank { Uri.decode(uri?.encodedPath.orEmpty()) }
    if (path.isBlank()) return false
    return runCatching {
        java.io.File(path).inputStream().use { input ->
            val header = ByteArray(6)
            if (input.read(header) != 6) return@runCatching false
            val magic = String(header)
            magic == "GIF87a" || magic == "GIF89a"
        }
    }.getOrDefault(false)
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
    val context = LocalContext.current
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) return
    val bitmap by produceState<Bitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                loadGuideBitmapSource(
                    context = context,
                    source = target
                )
            }.getOrNull()
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
    LaunchedEffect(audioPlayer, audioLoopEnabled) {
        audioPlayer?.repeatMode = if (audioLoopEnabled) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(audioPlayer, audioTargetUrl, audioLoopEnabled) {
        val player = audioPlayer ?: return@DisposableEffect onDispose { }
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
private fun GuideAudioSeekBar(
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

private fun formatAudioDuration(durationMs: Long): String {
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
        items.mapIndexed { index, _ -> "角色表情${index + 1}" }
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
                Box(
                    modifier = Modifier
                        .draggable(
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
                        )
                        .clickable { showImageFullscreen = true }
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

@Composable
private fun GuideImageFullscreenDialog(
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
        // pinch zoom (two fingers) + one-finger drag + single-tap dismiss.
        zoomState.zoomable.setDisabledGestureTypes(
            GestureType.ONE_FINGER_SCALE or GestureType.DOUBLE_TAP_SCALE
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

private data class GuideFullscreenImageState(
    val sampledBitmap: Bitmap? = null,
    val loading: Boolean = false,
    val helperLoadFailed: Boolean = false
)

@Composable
private fun GuideVideoFullscreenDialog(
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

            GlassIconButton(
                backdrop = null,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                variant = GlassVariant.Compact,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp),
                onClick = onDismiss,
                width = 40.dp,
                height = 40.dp
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
        val titleMaxWidth = if (inlineTitleIcon) {
            (maxWidth * 0.34f).coerceIn(68.dp, 136.dp)
        } else {
            (maxWidth * 0.42f).coerceIn(80.dp, 176.dp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
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
                        maxLines = 1,
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
                        maxLines = 1,
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
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = summary,
                        color = MiuixTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Clip
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
    var levelPopupAnchorBounds by remember(card.id) { mutableStateOf<IntRect?>(null) }
    var skillTitleRowHeightPx by remember(card.id) { mutableStateOf(0) }
    val levelOptions = card.levelOptions
    var selectedLevel by rememberSaveable(card.id) { mutableStateOf(card.defaultLevel) }
    var typeCapsuleHeightPx by remember(card.id, selectedLevel) { mutableStateOf(0) }
    var typeStateBlockHeightPx by remember(card.id, selectedLevel) { mutableStateOf(0) }
    var typeSubRowHeightPx by remember(card.id, selectedLevel) { mutableStateOf(0) }
    var skillNameLineCount by remember(card.id, selectedLevel) { mutableStateOf(1) }
    var descriptionLineCount by remember(card.id, selectedLevel) { mutableStateOf(1) }
    val density = LocalDensity.current

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
    val parsedSkillType = remember(card.type) { parseGuideSkillTypeMeta(card.type) }
    val displaySkillType = parsedSkillType.baseType
    val skillTypeStateTags = parsedSkillType.stateTags
    val skillTypeVariantBadge = parsedSkillType.variantIndex?.let(::toGuideCircledNumber)
    val hasTypeStateBlock = skillTypeStateTags.isNotEmpty()
    val hasTypeSubRow = skillTypeVariantBadge != null || levelOptions.isNotEmpty()
    val isExSkill = remember(card.type) { card.type.contains("EX", ignoreCase = true) }
    val hasSkillMetaColumn = remember(displaySkillType, skillCost, levelOptions, skillTypeVariantBadge, skillTypeStateTags) {
        displaySkillType.isNotBlank() ||
            skillCost.isNotBlank() ||
            levelOptions.isNotEmpty() ||
            skillTypeVariantBadge != null ||
            skillTypeStateTags.isNotEmpty()
    }
    val metaColumnShouldTopAlign = descriptionLineCount >= 3
    val skillNameTooLong = skillNameLineCount > 1
    val typeAlignToTitleOffset = if (
        displaySkillType.isNotBlank() &&
        !skillNameTooLong &&
        skillTitleRowHeightPx > 0 &&
        typeCapsuleHeightPx > 0
    ) {
        with(density) { ((skillTitleRowHeightPx - typeCapsuleHeightPx).coerceAtLeast(0) / 2).toDp() }
    } else {
        0.dp
    }
    val descriptionTopOffsetDp = with(density) { skillTitleRowHeightPx.toDp() } + 8.dp
    val stateBlockHeightDp = if (hasTypeStateBlock) {
        if (typeStateBlockHeightPx > 0) {
            with(density) { typeStateBlockHeightPx.toDp() }
        } else {
            26.dp
        }
    } else {
        0.dp
    }
    val subRowHeightDp = if (hasTypeSubRow) {
        if (typeSubRowHeightPx > 0) {
            with(density) { typeSubRowHeightPx.toDp() }
        } else {
            30.dp
        }
    } else {
        0.dp
    }
    val occupiedBeforeCostDp = when {
        displaySkillType.isNotBlank() -> {
            val typeBottomSpacing = when {
                hasTypeStateBlock && hasTypeSubRow -> 4.dp + stateBlockHeightDp + 4.dp + subRowHeightDp + 4.dp
                hasTypeStateBlock -> 4.dp + stateBlockHeightDp + 4.dp
                hasTypeSubRow -> 4.dp + subRowHeightDp + 4.dp
                else -> 4.dp
            }
            typeAlignToTitleOffset + with(density) { typeCapsuleHeightPx.toDp() } + typeBottomSpacing
        }
        hasTypeStateBlock && hasTypeSubRow -> stateBlockHeightDp + 4.dp + subRowHeightDp + 4.dp
        hasTypeStateBlock -> stateBlockHeightDp + 4.dp
        hasTypeSubRow -> subRowHeightDp + 4.dp
        else -> 0.dp
    }
    val costAlignToDescriptionOffset = if (metaColumnShouldTopAlign) {
        (descriptionTopOffsetDp - occupiedBeforeCostDp).coerceAtLeast(0.dp)
    } else {
        0.dp
    }

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
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { skillTitleRowHeightPx = it.height },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (card.iconUrl.isNotBlank()) {
                        GuideRemoteIcon(
                            imageUrl = card.iconUrl,
                            modifier = Modifier.alignBy { it.measuredHeight / 2 },
                            iconWidth = 34.dp,
                            iconHeight = 34.dp
                        )
                    }
                    Text(
                        text = card.name,
                        modifier = Modifier
                            .weight(1f)
                            .alignBy { it.measuredHeight / 2 },
                        color = MiuixTheme.colorScheme.onBackground,
                        maxLines = if (isExSkill) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { layoutResult ->
                            val safeLineCount = layoutResult.lineCount.coerceAtLeast(1)
                            if (skillNameLineCount != safeLineCount) {
                                skillNameLineCount = safeLineCount
                            }
                        }
                    )
                }
                GuideSkillDescriptionText(
                    description = skillDesc.ifBlank { "暂未解析到技能描述。" },
                    glossaryIcons = card.glossaryIcons,
                    descriptionIcons = card.descriptionIconsFor(selectedLevel),
                    onLineCountChanged = { lineCount ->
                        val safeLineCount = lineCount.coerceAtLeast(1)
                        if (descriptionLineCount != safeLineCount) {
                            descriptionLineCount = safeLineCount
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (hasSkillMetaColumn) {
                Column(
                    modifier = Modifier.widthIn(min = 68.dp, max = 90.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (displaySkillType.isNotBlank()) {
                        if (typeAlignToTitleOffset > 0.dp) {
                            Spacer(modifier = Modifier.height(typeAlignToTitleOffset))
                        }
                        Box(modifier = Modifier.onSizeChanged { typeCapsuleHeightPx = it.height }) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = displaySkillType,
                                enabled = false,
                                textColor = Color(0xFF3B82F6),
                                variant = GlassVariant.Compact,
                                minHeight = 30.dp,
                                horizontalPadding = 10.dp,
                                verticalPadding = 6.dp,
                                onClick = {}
                            )
                        }
                    }
                    if (hasTypeStateBlock) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { typeStateBlockHeightPx = it.height },
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            skillTypeStateTags.forEach { stateTag ->
                                GuideSkillStateTagButton(
                                    label = stateTag,
                                    backdrop = backdrop
                                )
                            }
                        }
                    }
                    if (hasTypeSubRow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { typeSubRowHeightPx = it.height },
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (skillTypeVariantBadge != null) {
                                GuideSkillVariantBadge(
                                    label = skillTypeVariantBadge
                                )
                            }
                            if (levelOptions.isNotEmpty()) {
                                Box(
                                    modifier = Modifier.capturePopupAnchor { levelPopupAnchorBounds = it }
                                ) {
                                    GlassTextButton(
                                        backdrop = backdrop,
                                        text = displayLevel,
                                        variant = GlassVariant.Compact,
                                        minHeight = 30.dp,
                                        horizontalPadding = 10.dp,
                                        verticalPadding = 6.dp,
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
                    if (costAlignToDescriptionOffset > 0.dp && skillCost.isNotBlank()) {
                        Spacer(modifier = Modifier.height(costAlignToDescriptionOffset))
                    }
                    if (skillCost.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "COST:$skillCost",
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            variant = GlassVariant.Compact,
                            minHeight = 30.dp,
                            horizontalPadding = 10.dp,
                            verticalPadding = 6.dp,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

private data class GuideSkillTypeMeta(
    val baseType: String,
    val variantIndex: Int? = null,
    val stateTags: List<String> = emptyList()
)

private fun parseGuideSkillTypeMeta(raw: String): GuideSkillTypeMeta {
    val cleaned = sanitizeGuideSkillLabelForDisplay(raw).trim()
    if (cleaned.isBlank()) return GuideSkillTypeMeta(baseType = "")

    var variantIndex: Int? = null
    val stateTags = mutableListOf<String>()
    val stateCandidates = guideSkillTypeBracketPattern
        .findAll(cleaned)
        .map { it.groupValues.getOrElse(1) { "" }.trim() }
        .filter { it.isNotBlank() }
        .toList()

    stateCandidates.forEach { candidate ->
        val tokens = candidate
            .split(guideSkillTypeStateSplitPattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(candidate) }
        tokens.forEach { token ->
            val tokenMeta = parseGuideSkillTypeToken(token)
            if (variantIndex == null && tokenMeta.variantIndex != null) {
                variantIndex = tokenMeta.variantIndex
            }
            val tag = tokenMeta.base.ifBlank { token.trim() }
            if (tag.isNotBlank()) {
                stateTags += tag
            }
        }
    }

    val baseCandidate = guideSkillTypeBracketPattern
        .replace(cleaned, "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '_', '/', '／', '|', '｜')
        .trim()
    val baseMeta = parseGuideSkillTypeToken(baseCandidate.ifBlank { cleaned })
    if (variantIndex == null) {
        variantIndex = baseMeta.variantIndex
    }

    return GuideSkillTypeMeta(
        baseType = baseMeta.base.ifBlank { cleaned },
        variantIndex = variantIndex,
        stateTags = stateTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    )
}

private data class GuideSkillTypeTokenMeta(
    val base: String,
    val variantIndex: Int? = null
)

private fun parseGuideSkillTypeToken(raw: String): GuideSkillTypeTokenMeta {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return GuideSkillTypeTokenMeta(base = "")

    val circledMatch = guideSkillTypeCircledSuffixPattern.matchEntire(cleaned)
    if (circledMatch != null) {
        val base = circledMatch.groupValues[1].trim()
        val circled = circledMatch.groupValues[2]
        val index = guideCircledNumbers.indexOf(circled).takeIf { it >= 0 }?.plus(1)
        return GuideSkillTypeTokenMeta(
            base = if (base.isBlank()) cleaned else base,
            variantIndex = index
        )
    }

    val numericMatch = guideSkillTypeNumericSuffixPattern.matchEntire(cleaned)
    if (numericMatch != null) {
        val base = numericMatch.groupValues[1].trim()
        val index = numericMatch.groupValues[2].toIntOrNull()?.takeIf { it > 0 }
        if (index != null) {
            return GuideSkillTypeTokenMeta(
                base = if (base.isBlank()) cleaned else base,
                variantIndex = index
            )
        }
    }

    return GuideSkillTypeTokenMeta(base = cleaned)
}

private fun toGuideCircledNumber(index: Int): String {
    return guideCircledNumbers.getOrNull(index - 1) ?: index.toString()
}

@Composable
private fun GuideSkillVariantBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(0x223B82F6))
            .border(width = 1.dp, color = Color(0x663B82F6), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF3B82F6),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun GuideSkillStateTagButton(
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    GlassTextButton(
        backdrop = backdrop,
        text = label,
        enabled = false,
        textColor = Color(0xFF3B82F6),
        variant = GlassVariant.Compact,
        minHeight = 26.dp,
        horizontalPadding = 8.dp,
        verticalPadding = 5.dp,
        modifier = modifier,
        onClick = {}
    )
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "配音",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.widthIn(min = 34.dp)
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
    backdrop: Backdrop?,
    playbackUrl: String,
    isPlaying: Boolean,
    playProgress: Float,
    onTogglePlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceLines = buildVoiceLinePairsForCard(entry, languageHeaders)
    val normalizedPlaybackUrl = normalizeGuideMediaSource(playbackUrl)
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
                if (normalizedPlaybackUrl.isNotBlank()) {
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
                            onClick = { onTogglePlay(normalizedPlaybackUrl) }
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

private fun canonicalVoiceLineLabelInCard(raw: String): String {
    val normalized = raw
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
        .trim()
    if (normalized.isBlank()) return ""
    return when {
        normalized.contains("官翻") || normalized.contains("官方翻译") || normalized.contains("官方中文") || normalized.contains("官中") -> "官翻"
        normalized.contains("韩") || normalized.contains("kr") || normalized.contains("kor") || normalized.contains("korean") -> "韩配"
        normalized.contains("中") || normalized.contains("cn") || normalized.contains("国语") || normalized.contains("国配") || normalized.contains("中文") -> "中配"
        normalized.contains("日") || normalized.contains("jp") || normalized.contains("jpn") || normalized.contains("日本") -> "日配"
        else -> raw.trim()
    }
}

private fun voiceLinePriorityForCard(label: String): Int {
    return when (canonicalVoiceLineLabelInCard(label)) {
        "日配" -> 0
        "中配" -> 1
        "官翻" -> 2
        "韩配" -> 3
        else -> 4
    }
}

private fun buildVoiceLinePairsForCard(
    entry: BaGuideVoiceEntry,
    fallbackHeaders: List<String>
): List<Pair<String, String>> {
    val explicitPairs = if (
        entry.lineHeaders.size == entry.lines.size &&
        entry.lineHeaders.any { it.trim().isNotBlank() }
    ) {
        entry.lineHeaders.zip(entry.lines)
    } else {
        emptyList()
    }
    val rawPairs = if (explicitPairs.isNotEmpty()) {
        explicitPairs
    } else {
        entry.lines.mapIndexed { index, line ->
            val label = fallbackHeaders.getOrNull(index).orEmpty().ifBlank { "台词${index + 1}" }
            label to line
        }
    }
    return rawPairs.withIndex()
        .mapNotNull { indexed ->
            val label = indexed.value.first.trim()
            val text = indexed.value.second.trim()
            if (text.isBlank()) return@mapNotNull null
            val normalizedLabel = canonicalVoiceLineLabelInCard(label).ifBlank {
                label.ifBlank { "台词${indexed.index + 1}" }
            }
            Triple(normalizedLabel, text, indexed.index)
        }
        .sortedWith(
            compareBy<Triple<String, String, Int>> { item ->
                voiceLinePriorityForCard(item.first)
            }.thenBy { item ->
                item.third
            }
        )
        .map { item -> item.first to item.second }
        .ifEmpty { listOf("台词" to "暂无台词文本") }
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
    onLineCountChanged: ((Int) -> Unit)? = null,
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
        onTextLayout = { layoutResult ->
            onLineCountChanged?.invoke(layoutResult.lineCount)
        },
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
