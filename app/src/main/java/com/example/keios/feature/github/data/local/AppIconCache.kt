package com.example.keios.feature.github.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlin.math.max
import kotlin.math.roundToInt

object AppIconCache {
    private const val maxCacheKb = 24 * 1024
    private const val maxIconEdgePx = 128

    private val cache = object : LruCache<String, Bitmap>(maxCacheKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return max(1, value.byteCount / 1024)
        }
    }
    @Volatile
    private var lastUpdatedAtMs: Long = 0L

    fun get(packageName: String): Bitmap? = synchronized(cache) { cache.get(packageName) }

    fun getOrLoad(context: Context, packageName: String): Bitmap? {
        get(packageName)?.let { return it }
        val bitmap = runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        }.getOrNull() ?: return null
        synchronized(cache) { cache.put(packageName, bitmap) }
        lastUpdatedAtMs = System.currentTimeMillis()
        return bitmap
    }

    fun preload(context: Context, packageNames: List<String>) {
        if (packageNames.isEmpty()) return
        packageNames.distinct().forEach { pkg ->
            if (get(pkg) == null) {
                runCatching { getOrLoad(context, pkg) }
            }
        }
    }

    fun size(): Int = synchronized(cache) { cache.snapshot().size }

    fun estimatedMemoryBytes(): Long = synchronized(cache) {
        cache.snapshot().values.fold(0L) { acc, bitmap -> acc + bitmap.byteCount.toLong() }
    }

    fun lastUpdatedAtMs(): Long = lastUpdatedAtMs

    fun clear() {
        synchronized(cache) { cache.evictAll() }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap.downscaleIfNeeded()
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val boundedWidth: Int
        val boundedHeight: Int
        if (width <= maxIconEdgePx && height <= maxIconEdgePx) {
            boundedWidth = width
            boundedHeight = height
        } else {
            val scale = minOf(
                maxIconEdgePx.toFloat() / width.toFloat(),
                maxIconEdgePx.toFloat() / height.toFloat()
            )
            boundedWidth = max(1, (width * scale).roundToInt())
            boundedHeight = max(1, (height * scale).roundToInt())
        }
        val bitmap = Bitmap.createBitmap(boundedWidth, boundedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun Bitmap.downscaleIfNeeded(): Bitmap {
        if (width <= maxIconEdgePx && height <= maxIconEdgePx) return this
        val scale = minOf(
            maxIconEdgePx.toFloat() / width.toFloat(),
            maxIconEdgePx.toFloat() / height.toFloat()
        )
        val targetWidth = max(1, (width * scale).roundToInt())
        val targetHeight = max(1, (height * scale).roundToInt())
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
