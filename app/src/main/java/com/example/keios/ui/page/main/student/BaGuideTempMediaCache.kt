package com.example.keios.ui.page.main.student

import android.content.Context
import android.net.Uri
import com.example.keios.ba.helper.GameKeeFetchHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object BaGuideTempMediaCache {
    private const val ROOT_DIR = "ba_student_guide_temp_media"
    private const val MAX_PARALLEL_DOWNLOADS = 3

    private fun sessionDir(context: Context, sourceUrl: String): File {
        val key = sha1(sourceUrl).take(16)
        return File(context.cacheDir, "$ROOT_DIR/$key")
    }

    private fun normalizeTarget(raw: String): String {
        return normalizeGuideUrl(raw.trim()).trim()
    }

    private fun fileExtFromUrl(url: String): String {
        val normalized = url.substringBefore('?').substringBefore('#')
        val fromPath = runCatching { Uri.parse(normalized).lastPathSegment.orEmpty() }
            .getOrDefault("")
            .substringAfterLast('.', "")
            .lowercase()
        val ext = when (fromPath) {
            "png", "jpg", "jpeg", "webp", "gif", "bmp", "mp4", "webm", "mov", "m3u8" -> fromPath
            else -> "bin"
        }
        return ".$ext"
    }

    private fun targetFile(context: Context, sourceUrl: String, normalizedUrl: String): File {
        val name = sha1(normalizedUrl) + fileExtFromUrl(normalizedUrl)
        return File(sessionDir(context, sourceUrl), name)
    }

    suspend fun prefetchForGuide(
        context: Context,
        sourceUrl: String,
        rawUrls: List<String>
    ) = withContext(Dispatchers.IO) {
        val dir = sessionDir(context, sourceUrl)
        dir.mkdirs()
        val targets = rawUrls
            .map(::normalizeTarget)
            .filter { it.isNotBlank() }
            .distinct()
        if (targets.isEmpty()) return@withContext

        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)
            targets.map { url ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        ensureActive()
                        val file = targetFile(context, sourceUrl, url)
                        if (file.exists() && file.length() > 0L) return@withPermit
                        runCatching { GameKeeFetchHelper.downloadToFile(url, file) }
                    }
                }
            }.awaitAll()
        }
    }

    fun resolveCachedUrl(
        context: Context,
        sourceUrl: String,
        rawUrl: String
    ): String {
        val normalized = normalizeTarget(rawUrl)
        if (normalized.isBlank()) return normalized
        val file = targetFile(context, sourceUrl, normalized)
        if (file.exists() && file.length() > 0L) {
            return file.toURI().toString()
        }
        return normalized
    }

    fun clearGuideCache(context: Context, sourceUrl: String) {
        runCatching { sessionDir(context, sourceUrl).deleteRecursively() }
    }

    private fun sha1(raw: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(raw.toByteArray())
        return buildString(bytes.size * 2) {
            bytes.forEach { b -> append("%02x".format(b)) }
        }
    }
}

