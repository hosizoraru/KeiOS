@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student.catalog.component

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GameKeeMediaImageLoader
import java.io.ByteArrayOutputStream

internal object BaGuideBgmMediaArtworkPayloadResolver {
    private const val MAX_PAYLOAD_CACHE_COUNT = 32
    private const val MAX_PAYLOAD_CACHE_BYTES = 8 * 1024 * 1024

    private val payloadCache = LinkedHashMap<String, ByteArray>(
        MAX_PAYLOAD_CACHE_COUNT,
        0.75f,
        true
    )

    suspend fun resolve(
        context: Context,
        favorite: GuideBgmFavoriteItem,
        artworkUrlResolver: (GuideBgmFavoriteItem) -> String = { item ->
            item.resolvePlaybackArtworkImageUrl()
        },
        bitmapLoader: (suspend (Context, String) -> Bitmap?) = { ctx, url ->
            loadMediaArtwork(ctx, url)
        },
        defaultDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation
    ): ByteArray? {
        return resolveUrl(
            context = context,
            artworkUrl = artworkUrlResolver(favorite),
            bitmapLoader = bitmapLoader,
            defaultDispatcher = defaultDispatcher
        )
    }

    suspend fun resolveUrl(
        context: Context,
        artworkUrl: String,
        bitmapLoader: suspend (Context, String) -> Bitmap? = { ctx, url ->
            loadMediaArtwork(ctx, url)
        },
        defaultDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation
    ): ByteArray? {
        val key = artworkUrl.trim()
        if (key.isBlank()) return null
        getCached(key)?.let { return it }
        val bitmap = bitmapLoader(context.applicationContext, key) ?: return null
        val payload = withContext(defaultDispatcher) {
            bitmap.toPngByteArray()
        } ?: return null
        synchronized(payloadCache) {
            payloadCache[key] = payload
            while (payloadCache.size > MAX_PAYLOAD_CACHE_COUNT ||
                payloadCache.values.sumOf { it.size.toLong() } > MAX_PAYLOAD_CACHE_BYTES
            ) {
                payloadCache.remove(payloadCache.keys.first())
            }
        }
        return payload
    }

    fun clearMemoryCache() {
        synchronized(payloadCache) {
            payloadCache.clear()
        }
    }

    private fun getCached(key: String): ByteArray? {
        return synchronized(payloadCache) {
            payloadCache[key]
        }
    }

    private suspend fun loadMediaArtwork(context: Context, url: String): Bitmap? {
        val limit = MediaSession.getBitmapDimensionLimit(context)
        // Match Media3's relaxed decode bound before precisely fitting the session limit.
        val decoded = GameKeeMediaImageLoader.loadGuideBitmap(
            context,
            url,
            maxDecodeDimension = limit * 2 - 1
        ) ?: return null
        return withContext(AppDispatchers.uiDerivation) {
            val largestEdge = maxOf(decoded.width, decoded.height)
            if (largestEdge <= limit) decoded else Bitmap.createScaledBitmap(
                decoded,
                (decoded.width.toLong() * limit / largestEdge).toInt().coerceAtLeast(1),
                (decoded.height.toLong() * limit / largestEdge).toInt().coerceAtLeast(1),
                true
            )
        }
    }

    private fun Bitmap.toPngByteArray(): ByteArray? {
        return ByteArrayOutputStream().use { output ->
            val compressed = compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray().takeIf { compressed && it.isNotEmpty() }
        }
    }
}
