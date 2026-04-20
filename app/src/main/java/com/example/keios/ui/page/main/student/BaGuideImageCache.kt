package com.example.keios.ui.page.main.student

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import java.io.File
import java.security.MessageDigest

internal object BaGuideImageCache {
    private const val ROOT_DIR = "ba_student_guide_image_cache"
    private const val MAX_MEMORY_ENTRIES = 72

    private val memory = object : LruCache<String, Bitmap>(MAX_MEMORY_ENTRIES) {}

    private fun normalizeTarget(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return ""
        val isFile = runCatching {
            android.net.Uri.parse(value).scheme.orEmpty().equals("file", ignoreCase = true)
        }.getOrDefault(false)
        return if (isFile) value else normalizeGuideUrl(value)
    }

    private fun decodeSampledFile(file: File, maxDecodeDimension: Int): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null
        val safeMax = maxDecodeDimension.coerceIn(512, 4096)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
        var sample = 1
        while ((srcWidth / sample) > safeMax || (srcHeight / sample) > safeMax) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun cacheRoot(context: Context): File {
        return File(context.cacheDir, ROOT_DIR).apply { mkdirs() }
    }

    private fun fileExtFromUrl(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png", "jpg", "jpeg", "webp", "gif", "bmp", "avif", "svg" -> ".$ext"
            else -> ".img"
        }
    }

    private fun cacheFileForUrl(context: Context, normalizedUrl: String): File {
        val name = sha1(normalizedUrl) + fileExtFromUrl(normalizedUrl)
        return File(cacheRoot(context), name)
    }

    private fun isFileFreshByBaInterval(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val refreshIntervalHours = BASettingsStore.loadCalendarRefreshIntervalHours().coerceAtLeast(1)
        val ttlMs = refreshIntervalHours * 60L * 60L * 1000L
        val ageMs = (System.currentTimeMillis() - file.lastModified()).coerceAtLeast(0L)
        return ageMs < ttlMs
    }

    private fun localFileFromFileUrl(target: String): File? {
        val uri = runCatching { android.net.Uri.parse(target) }.getOrNull() ?: return null
        if (!uri.scheme.equals("file", ignoreCase = true)) return null
        val path = uri.path.orEmpty().ifBlank { android.net.Uri.decode(uri.encodedPath.orEmpty()) }
        if (path.isBlank()) return null
        return File(path)
    }

    private fun memoryKey(normalizedUrl: String, maxDecodeDimension: Int): String {
        return "${normalizedUrl}|${maxDecodeDimension.coerceIn(128, 4096)}"
    }

    private fun sha1(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    fun clearAll(context: Context) {
        synchronized(memory) { memory.evictAll() }
        runCatching { cacheRoot(context).deleteRecursively() }
    }

    fun loadBitmap(
        context: Context,
        source: String,
        maxDecodeDimension: Int = 2048,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Bitmap? {
        val normalized = normalizeTarget(source)
        if (normalized.isBlank()) return null
        val key = memoryKey(normalized, maxDecodeDimension)
        synchronized(memory) {
            memory.get(key)?.let { return it }
        }

        val localFile = localFileFromFileUrl(normalized)
        if (localFile != null) {
            val bitmap = decodeSampledFile(localFile, maxDecodeDimension)
            if (bitmap != null) {
                synchronized(memory) { memory.put(key, bitmap) }
            }
            return bitmap
        }

        val diskFile = cacheFileForUrl(context, normalized)
        if (isFileFreshByBaInterval(diskFile)) {
            val bitmap = decodeSampledFile(diskFile, maxDecodeDimension)
            if (bitmap != null) {
                synchronized(memory) { memory.put(key, bitmap) }
                if (onProgress != null) {
                    val size = diskFile.length()
                    onProgress(size, size)
                }
                return bitmap
            }
            runCatching { diskFile.delete() }
        } else if (diskFile.exists()) {
            runCatching { diskFile.delete() }
        }

        val downloaded = runCatching {
            GameKeeFetchHelper.downloadToFile(
                mediaUrl = normalized,
                targetFile = diskFile,
                onProgress = onProgress
            )
        }.getOrDefault(false)
        if (downloaded && diskFile.exists() && diskFile.length() > 0L) {
            val bitmap = decodeSampledFile(diskFile, maxDecodeDimension)
            if (bitmap != null) {
                synchronized(memory) { memory.put(key, bitmap) }
                return bitmap
            }
            runCatching { diskFile.delete() }
        }

        val fallback = if (onProgress != null) {
            GameKeeFetchHelper.fetchImageWithProgress(
                imageUrl = normalized,
                onProgress = onProgress,
                maxDecodeDimension = maxDecodeDimension
            )
        } else {
            GameKeeFetchHelper.fetchImage(
                imageUrl = normalized,
                maxDecodeDimension = maxDecodeDimension
            )
        }
        if (fallback != null) {
            synchronized(memory) { memory.put(key, fallback) }
        }
        return fallback
    }

    fun peekBitmap(
        source: String,
        maxDecodeDimension: Int = 2048
    ): Bitmap? {
        val normalized = normalizeTarget(source)
        if (normalized.isBlank()) return null
        val key = memoryKey(normalized, maxDecodeDimension)
        return synchronized(memory) { memory.get(key) }
    }
}
