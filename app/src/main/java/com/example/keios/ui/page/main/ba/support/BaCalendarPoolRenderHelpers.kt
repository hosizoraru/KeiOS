package com.example.keios.ui.page.main.ba.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    aspectRatioRange: ClosedFloatingPointRange<Float> = 1.0f..2.4f
) {
    if (!enabled) return
    val normalizedUrl = remember(imageUrl) { normalizeGameKeeImageLink(imageUrl) }
    if (normalizedUrl.isBlank()) return

    val bitmap by produceState<Bitmap?>(initialValue = null, normalizedUrl, enabled) {
        if (!enabled) {
            value = null
            return@produceState
        }
        if (normalizedUrl.startsWith("file://")) {
            val localPath = Uri.parse(normalizedUrl).path.orEmpty()
            if (localPath.isBlank()) {
                value = null
                return@produceState
            }
            value = withContext(Dispatchers.IO) { decodeSampledLocalBitmap(localPath, 720) }
            val high = withContext(Dispatchers.IO) { decodeSampledLocalBitmap(localPath, 1280) }
            if (high != null) {
                val low = value
                val shouldUpgrade = low == null ||
                    (high.width * high.height) > (low.width * low.height)
                if (shouldUpgrade) value = high
            }
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                GameKeeFetchHelper.fetchImage(
                    imageUrl = normalizedUrl,
                    maxDecodeDimension = 720
                )
            }.getOrNull()
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
