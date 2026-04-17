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

internal const val IMAGE_TAP_DISMISS_GESTURE_COOLDOWN_MS = 260L
internal const val IMAGE_TAP_DISMISS_SCALE_EPSILON = 0.035f
internal const val IMAGE_TAP_DISMISS_OFFSET_EPSILON_PX = 18f
internal const val IMAGE_BACK_GESTURE_TRANSLATION_FACTOR = 0.2f
internal const val IMAGE_BACK_GESTURE_CONTENT_FADE_FACTOR = 0.16f
internal const val IMAGE_BACK_GESTURE_SCRIM_FADE_FACTOR = 0.72f
internal const val GUIDE_INLINE_GIF_CACHE_SCOPE = "https://www.gamekee.com/__guide_inline_gif_scope"
internal val guideCircledNumbers = listOf(
    "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
    "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳"
)
internal val guideSkillTypeNumericSuffixPattern = Regex("""^(.*?)[\s\-_]*(\d{1,2})$""")
internal val guideSkillTypeCircledSuffixPattern = Regex("""^(.*?)([①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳])$""")
internal val guideSkillTypeBracketPattern = Regex("""[（(【\[]([^()（）【】\[\]]+)[)）】\]]""")
internal val guideSkillTypeStateSplitPattern = Regex("""\s*[、,，/／|｜+＋]\s*""")

internal enum class GuideVideoControlAction {
    TogglePlayPause
}

internal object GuideBgmLoopStore {
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

internal object GuideBgmPlayerStore {
    private val playerByScopedAudio = ConcurrentHashMap<String, ExoPlayer>()

    private fun scopedKey(scopeKey: String, audioUrl: String): String {
        if (scopeKey.isBlank() || audioUrl.isBlank()) return ""
        return "$scopeKey|$audioUrl"
    }

    fun getOrCreate(context: Context, scopeKey: String, audioUrl: String): ExoPlayer? {
        val key = scopedKey(scopeKey, audioUrl)
        if (key.isBlank()) return null
        return playerByScopedAudio[key] ?: synchronized(this) {
            playerByScopedAudio[key] ?: ExoPlayer.Builder(context)
                .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
                .build()
                .also { created ->
                    playerByScopedAudio[key] = created
                }
        }
    }

    fun clearScope(scopeKey: String) {
        if (scopeKey.isBlank()) return
        val prefix = "$scopeKey|"
        val releaseKeys = playerByScopedAudio.keys.filter { key -> key.startsWith(prefix) }
        releaseKeys.forEach { key ->
            playerByScopedAudio.remove(key)?.let { player ->
                runCatching { player.stop() }
                runCatching { player.release() }
            }
        }
    }
}

internal fun clearGuideBgmPlaybackScope(scopeKey: String) {
    GuideBgmPlayerStore.clearScope(scopeKey)
}

internal fun normalizeGuideMediaSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
    }
}

internal fun isSystemAutoRotateEnabled(context: Context): Boolean {
    return runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0
        ) == 1
    }.getOrDefault(false)
}

internal fun currentDisplayRotationDegrees(context: Context): Int {
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

internal fun normalizeRotationDegreesByOrientation(rawOrientation: Int): Int {
    val orientation = ((rawOrientation % 360) + 360) % 360
    return when {
        orientation in 45..134 -> 270
        orientation in 135..224 -> 180
        orientation in 225..314 -> 90
        else -> 0
    }
}

internal fun circularAngleDistance(a: Int, b: Int): Int {
    val diff = kotlin.math.abs(a - b) % 360
    return kotlin.math.min(diff, 360 - diff)
}

internal fun snapCardinalOrientation(rawOrientation: Int): Int? {
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
internal fun rememberSystemAutoRotateEnabled(active: Boolean): Boolean {
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
internal fun rememberDeviceRotationDegrees(active: Boolean): Int {
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

internal fun loadGuideBitmapSource(
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

internal fun normalizeGalleryDisplayTitle(title: String, mediaType: String): String {
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
    maxDecodeDimension: Int = 1920,
    cropAlignment: Alignment = Alignment.Center
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
        alignment = cropAlignment,
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
        val resolvedGifTarget by produceState(
            initialValue = target,
            target
        ) {
            if (!isHttpMediaSource(target)) {
                value = target
                return@produceState
            }
            progressState?.value = 0f
            onLoadingChanged?.invoke(true)
            val warmed = withContext(Dispatchers.IO) {
                runCatching {
                    BaGuideTempMediaCache.prefetchForGuide(
                        context = context,
                        sourceUrl = GUIDE_INLINE_GIF_CACHE_SCOPE,
                        rawUrls = listOf(target)
                    )
                }
                var resolved = BaGuideTempMediaCache.resolveCachedUrl(
                    context = context,
                    sourceUrl = GUIDE_INLINE_GIF_CACHE_SCOPE,
                    rawUrl = target
                )
                if (!isFileMediaSource(resolved)) {
                    runCatching {
                        BaGuideTempMediaCache.prefetchForGuide(
                            context = context,
                            sourceUrl = GUIDE_INLINE_GIF_CACHE_SCOPE,
                            rawUrls = listOf(target),
                            forceReDownload = true
                        )
                    }
                    resolved = BaGuideTempMediaCache.resolveCachedUrl(
                        context = context,
                        sourceUrl = GUIDE_INLINE_GIF_CACHE_SCOPE,
                        rawUrl = target
                    )
                }
                resolved
            }
            value = warmed.ifBlank { target }
        }
        val ratio = remember(resolvedGifTarget, target) {
            detectMediaRatioFromUrl(resolvedGifTarget.ifBlank { target }) ?: (16f / 9f)
        }
        AsyncImage(
            model = resolvedGifTarget,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onLoading = {
                progressState?.value = 0.24f
                onLoadingChanged?.invoke(true)
            },
            onSuccess = {
                progressState?.value = 1f
                onLoadingChanged?.invoke(false)
            },
            onError = {
                progressState?.value = 1f
                onLoadingChanged?.invoke(false)
            },
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

internal fun isGifMediaSource(source: String): Boolean {
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

internal fun isFileMediaSource(source: String): Boolean {
    val value = source.trim()
    if (value.isBlank()) return false
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
    return uri.scheme.equals("file", ignoreCase = true)
}

internal fun isHttpMediaSource(source: String): Boolean {
    val value = source.trim()
    if (value.isBlank()) return false
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
    val scheme = uri.scheme.orEmpty()
    return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
}

internal fun detectMediaRatioFromUrl(source: String): Float? {
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
        MiuixInfoItem(
            key = key,
            value = value,
            onLongClick = rememberGuideCopyAction(buildGuideCopyPayload(key, value))
        )
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
