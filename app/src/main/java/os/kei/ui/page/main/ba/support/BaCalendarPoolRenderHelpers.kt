package os.kei.ui.page.main.ba.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import os.kei.feature.ba.data.remote.GameKeeFetchHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val GAMEKEE_COVER_MEMORY_CACHE_COUNT = 24
private val gameKeeCoverBitmapCache = object : LruCache<String, Bitmap>(GAMEKEE_COVER_MEMORY_CACHE_COUNT) {}

private fun cachedGameKeeCoverBitmap(url: String): Bitmap? {
    val key = url.trim()
    if (key.isBlank()) return null
    return synchronized(gameKeeCoverBitmapCache) { gameKeeCoverBitmapCache.get(key) }
}

private fun cacheGameKeeCoverBitmap(url: String, bitmap: Bitmap) {
    val key = url.trim()
    if (key.isBlank()) return
    synchronized(gameKeeCoverBitmapCache) { gameKeeCoverBitmapCache.put(key, bitmap) }
}

private fun decodeSampledLocalBitmap(
    localPath: String,
    maxDecodeDimension: Int = 1280
): Bitmap? {
    val safeMax = maxDecodeDimension.coerceAtLeast(512)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(localPath, bounds)
    val srcWidth = bounds.outWidth
    val srcHeight = bounds.outHeight
    if (srcWidth <= 0 || srcHeight <= 0) {
        return BitmapFactory.decodeFile(localPath)
    }
    var sample = 1
    while ((srcWidth / sample) > safeMax || (srcHeight / sample) > safeMax) {
        sample *= 2
    }
    return BitmapFactory.decodeFile(
        localPath,
        BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    )
}

@Composable
internal fun GameKeeCoverImage(
    imageUrl: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    aspectRatioRange: ClosedFloatingPointRange<Float> = 1.0f..2.4f,
    loadEnabled: Boolean = true
) {
    if (!enabled) return
    val normalizedUrl = remember(imageUrl) { normalizeGameKeeImageLink(imageUrl) }
    if (normalizedUrl.isBlank()) return

    var bitmap by remember(normalizedUrl) {
        mutableStateOf(cachedGameKeeCoverBitmap(normalizedUrl))
    }
    LaunchedEffect(normalizedUrl, loadEnabled) {
        cachedGameKeeCoverBitmap(normalizedUrl)?.let { cached ->
            bitmap = cached
            return@LaunchedEffect
        }
        if (!loadEnabled) return@LaunchedEffect
        if (normalizedUrl.startsWith("file://")) {
            val localPath = Uri.parse(normalizedUrl).path.orEmpty()
            if (localPath.isBlank()) {
                return@LaunchedEffect
            }
            val low = withContext(Dispatchers.IO) { decodeSampledLocalBitmap(localPath, 720) }
            if (low != null) {
                bitmap = low
                cacheGameKeeCoverBitmap(normalizedUrl, low)
            }
            val high = withContext(Dispatchers.IO) { decodeSampledLocalBitmap(localPath, 1280) }
            if (high != null) {
                val current = bitmap
                val shouldUpgrade = current == null ||
                    (high.width * high.height) > (current.width * current.height)
                if (shouldUpgrade) {
                    bitmap = high
                    cacheGameKeeCoverBitmap(normalizedUrl, high)
                }
            }
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                GameKeeFetchHelper.fetchImage(
                    imageUrl = normalizedUrl,
                    maxDecodeDimension = 720
                )
            }.getOrNull()
        }
        if (loaded != null) {
            bitmap = loaded
            cacheGameKeeCoverBitmap(normalizedUrl, loaded)
        }
    }

    val rendered = bitmap ?: return
    val minRatio = aspectRatioRange.start.coerceAtLeast(0.2f)
    val maxRatio = aspectRatioRange.endInclusive.coerceAtLeast(minRatio + 0.01f)
    val aspectRatioValue = remember(rendered.width, rendered.height, minRatio, maxRatio) {
        val w = rendered.width.coerceAtLeast(1)
        val h = rendered.height.coerceAtLeast(1)
        (w.toFloat() / h.toFloat()).coerceIn(minRatio, maxRatio)
    }
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatioValue)
            .clip(RoundedCornerShape(12.dp))
    )
}

internal fun formatBaDateTimeNoYearInTimeZone(epochMillis: Long, timeZone: TimeZone): String {
    if (epochMillis <= 0L) return "-"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }.format(Date(epochMillis))
    }.getOrDefault("-")
}
