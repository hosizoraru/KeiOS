package com.example.keios.ui.page.main.student.catalog

import android.graphics.Bitmap
import android.util.LruCache
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper

internal object BaGuideCatalogIconCache {
    private const val MAX_CACHE_COUNT = 96
    private const val MAX_DECODE_EDGE = 256

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_COUNT) {}

    fun get(url: String): Bitmap? {
        val key = url.trim()
        if (key.isBlank()) return null
        return synchronized(cache) { cache.get(key) }
    }

    fun getOrLoad(url: String): Bitmap? {
        val key = url.trim()
        if (key.isBlank()) return null
        get(key)?.let { return it }
        val bitmap = runCatching {
            GameKeeFetchHelper.fetchImage(
                imageUrl = key,
                maxDecodeDimension = MAX_DECODE_EDGE
            )
        }.getOrNull() ?: return null
        synchronized(cache) { cache.put(key, bitmap) }
        return bitmap
    }
}
