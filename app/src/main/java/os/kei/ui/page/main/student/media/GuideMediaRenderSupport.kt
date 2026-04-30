package os.kei.ui.page.main.student

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import os.kei.ui.page.main.student.fetch.normalizeGuideUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

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

internal fun normalizeGalleryDisplayTitle(
    title: String,
    mediaType: String,
    fallbackTitle: String,
    audioBgmPrefix: String
): String {
    val raw = title.trim().ifBlank { fallbackTitle }
    if (mediaType.lowercase() != "audio") return raw
    return if (raw.startsWith("BGM", ignoreCase = true)) {
        raw.replaceFirst(Regex("^BGM", RegexOption.IGNORE_CASE), audioBgmPrefix)
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
    val fallbackRatio = remember(target) { detectMediaRatioFromUrl(target) ?: (16f / 9f) }
    var stableRatio by remember { mutableStateOf(fallbackRatio.coerceIn(0.4f, 4f)) }
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
    var retainedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val bitmap by produceState<Bitmap?>(initialValue = retainedBitmap, target) {
        progressState?.value = 0f
        onLoadingChanged?.invoke(true)
        val loadedBitmap = withContext(Dispatchers.IO) {
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
        if (loadedBitmap != null) {
            retainedBitmap = loadedBitmap
            value = loadedBitmap
            progressState?.value = 1f
        } else {
            value = retainedBitmap
            progressState?.value = 1f
        }
        onLoadingChanged?.invoke(false)
    }
    val rendered = bitmap
    if (rendered == null) {
        Spacer(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(stableRatio)
                .clip(RoundedCornerShape(14.dp))
        )
        return
    }
    val ratio = remember(rendered.width, rendered.height) {
        if (rendered.width > 0 && rendered.height > 0) {
            rendered.width.toFloat() / rendered.height.toFloat()
        } else {
            stableRatio
        }
    }.coerceIn(0.4f, 4f)
    if (kotlin.math.abs(ratio - stableRatio) > 0.001f) {
        stableRatio = ratio
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
    val density = LocalDensity.current
    val target = remember(imageUrl) { normalizeGuideMediaSource(imageUrl) }
    if (target.isBlank()) return
    val iconDecodeDimension = remember(iconWidth, iconHeight, density) {
        val widthPx = with(density) { iconWidth.roundToPx() }
        val heightPx = with(density) { iconHeight.roundToPx() }
        (maxOf(widthPx, heightPx) * 2).coerceIn(96, 768)
    }
    val bitmap by produceState<Bitmap?>(
        initialValue = BaGuideImageCache.peekBitmap(
            source = target,
            maxDecodeDimension = iconDecodeDimension
        ),
        target,
        iconDecodeDimension
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    loadGuideBitmapSource(
                        context = context,
                        source = target,
                        maxDecodeDimension = iconDecodeDimension
                    )
                }.getOrNull()
            }
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
