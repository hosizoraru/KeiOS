package com.example.keios.ui.page.main.student

import android.content.Context
import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.tencent.mmkv.MMKV
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
    private const val INDEX_KV_ID = "ba_student_guide_temp_media_index"
    private const val INDEX_VERSION = 1
    private const val KEY_INDEX_VERSION = "index_version"
    private const val KEY_SESSION_IDS = "session_ids"

    private val indexStore: MMKV by lazy { MMKV.mmkvWithID(INDEX_KV_ID) }

    private fun rootDir(context: Context): File = File(context.cacheDir, ROOT_DIR)

    private fun sessionId(sourceUrl: String): String = sha1(sourceUrl).take(16)

    private fun sessionDir(context: Context, sourceUrl: String): File = File(context.cacheDir, "$ROOT_DIR/${sessionId(sourceUrl)}")

    private fun sessionDirById(context: Context, id: String): File = File(context.cacheDir, "$ROOT_DIR/$id")

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
        rebuildSessionIndex(context, sourceUrl)
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
            return Uri.fromFile(file).toString()
        }
        return normalized
    }

    fun clearGuideCache(context: Context, sourceUrl: String) {
        runCatching { sessionDir(context, sourceUrl).deleteRecursively() }
        removeSessionIndex(sourceUrl)
    }

    fun clearMediaCache(
        context: Context,
        sourceUrl: String,
        rawUrl: String
    ) {
        val normalized = normalizeTarget(rawUrl)
        if (normalized.isBlank()) return
        runCatching {
            val file = targetFile(context, sourceUrl, normalized)
            if (file.exists()) file.delete()
        }
        rebuildSessionIndex(context, sourceUrl)
    }

    fun clearAll(context: Context) {
        runCatching { rootDir(context).deleteRecursively() }
        clearIndex()
    }

    fun cacheFileCount(context: Context): Int {
        return loadIndexSummary(context).count
    }

    fun cacheTotalBytes(context: Context): Long {
        return loadIndexSummary(context).bytes
    }

    fun latestModifiedAtMs(context: Context): Long {
        return loadIndexSummary(context).latest
    }

    private data class MediaSummary(
        val count: Int,
        val bytes: Long,
        val latest: Long
    )

    private data class SessionSummary(
        val count: Int,
        val bytes: Long,
        val latest: Long
    )

    private fun sessionCountKey(id: String): String = "s_${id}_c"
    private fun sessionBytesKey(id: String): String = "s_${id}_b"
    private fun sessionLatestKey(id: String): String = "s_${id}_m"

    private fun loadSessionIds(kv: MMKV = indexStore): MutableSet<String> {
        val raw = kv.decodeString(KEY_SESSION_IDS, "").orEmpty()
        if (raw.isBlank()) return mutableSetOf()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
    }

    private fun saveSessionIds(ids: Set<String>, kv: MMKV = indexStore) {
        val encoded = ids.filter { it.isNotBlank() }.sorted().joinToString(",")
        kv.encode(KEY_SESSION_IDS, encoded)
    }

    private fun readSessionSummary(id: String, kv: MMKV = indexStore): SessionSummary? {
        if (!kv.containsKey(sessionCountKey(id)) || !kv.containsKey(sessionBytesKey(id))) return null
        val count = kv.decodeInt(sessionCountKey(id), -1)
        val bytes = kv.decodeLong(sessionBytesKey(id), -1L)
        if (count < 0 || bytes < 0L) return null
        return SessionSummary(
            count = count,
            bytes = bytes,
            latest = kv.decodeLong(sessionLatestKey(id), 0L).coerceAtLeast(0L)
        )
    }

    private fun writeSessionSummary(id: String, summary: SessionSummary, kv: MMKV = indexStore) {
        val ids = loadSessionIds(kv)
        if (summary.count <= 0 || summary.bytes <= 0L) {
            ids.remove(id)
            kv.removeValueForKey(sessionCountKey(id))
            kv.removeValueForKey(sessionBytesKey(id))
            kv.removeValueForKey(sessionLatestKey(id))
        } else {
            ids.add(id)
            kv.encode(sessionCountKey(id), summary.count)
            kv.encode(sessionBytesKey(id), summary.bytes)
            kv.encode(sessionLatestKey(id), summary.latest.coerceAtLeast(0L))
        }
        saveSessionIds(ids, kv)
        kv.encode(KEY_INDEX_VERSION, INDEX_VERSION)
    }

    private fun clearIndex() {
        val kv = indexStore
        loadSessionIds(kv).forEach { id ->
            kv.removeValueForKey(sessionCountKey(id))
            kv.removeValueForKey(sessionBytesKey(id))
            kv.removeValueForKey(sessionLatestKey(id))
        }
        kv.removeValueForKey(KEY_SESSION_IDS)
        kv.removeValueForKey(KEY_INDEX_VERSION)
        kv.trim()
    }

    private fun removeSessionIndex(sourceUrl: String) {
        val id = sessionId(sourceUrl)
        writeSessionSummary(
            id = id,
            summary = SessionSummary(count = 0, bytes = 0L, latest = 0L)
        )
    }

    private fun scanSessionSummary(dir: File): SessionSummary {
        if (!dir.exists()) return SessionSummary(count = 0, bytes = 0L, latest = 0L)
        var count = 0
        var bytes = 0L
        var latest = 0L
        dir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                count += 1
                bytes += file.length()
                latest = maxOf(latest, file.lastModified())
            }
        return SessionSummary(count = count, bytes = bytes, latest = latest)
    }

    private fun rebuildSessionIndex(context: Context, sourceUrl: String) {
        val id = sessionId(sourceUrl)
        val summary = scanSessionSummary(sessionDirById(context, id))
        writeSessionSummary(id, summary)
    }

    private fun rebuildAllIndex(context: Context): MediaSummary {
        val root = rootDir(context)
        if (!root.exists()) {
            clearIndex()
            return MediaSummary(count = 0, bytes = 0L, latest = 0L)
        }
        val kv = indexStore
        val discoveredIds = mutableSetOf<String>()
        root.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .forEach { dir ->
                val id = dir.name.trim()
                if (id.isBlank()) return@forEach
                val summary = scanSessionSummary(dir)
                discoveredIds.add(id)
                writeSessionSummary(id, summary, kv)
            }
        val staleIds = loadSessionIds(kv) - discoveredIds
        staleIds.forEach { staleId ->
            writeSessionSummary(
                id = staleId,
                summary = SessionSummary(count = 0, bytes = 0L, latest = 0L),
                kv = kv
            )
        }
        return aggregateSummaryFromIndex(kv)
    }

    private fun aggregateSummaryFromIndex(kv: MMKV = indexStore): MediaSummary {
        val ids = loadSessionIds(kv)
        if (ids.isEmpty()) return MediaSummary(count = 0, bytes = 0L, latest = 0L)
        var totalCount = 0
        var totalBytes = 0L
        var latest = 0L
        ids.forEach { id ->
            val session = readSessionSummary(id, kv) ?: return@forEach
            totalCount += session.count
            totalBytes += session.bytes
            latest = maxOf(latest, session.latest)
        }
        return MediaSummary(
            count = totalCount.coerceAtLeast(0),
            bytes = totalBytes.coerceAtLeast(0L),
            latest = latest.coerceAtLeast(0L)
        )
    }

    private fun loadIndexSummary(context: Context): MediaSummary {
        val kv = indexStore
        val root = rootDir(context)
        val hasIndex = kv.decodeInt(KEY_INDEX_VERSION, 0) == INDEX_VERSION
        val hasSessions = loadSessionIds(kv).isNotEmpty()
        return when {
            !root.exists() -> {
                if (hasSessions || hasIndex) clearIndex()
                MediaSummary(count = 0, bytes = 0L, latest = 0L)
            }
            !hasIndex -> rebuildAllIndex(context)
            !hasSessions -> {
                val hasFiles = root.listFiles().orEmpty().any { it.isDirectory }
                if (hasFiles) rebuildAllIndex(context) else MediaSummary(count = 0, bytes = 0L, latest = 0L)
            }
            else -> aggregateSummaryFromIndex(kv)
        }
    }

    private fun sha1(raw: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(raw.toByteArray())
        return buildString(bytes.size * 2) {
            bytes.forEach { b -> append("%02x".format(b)) }
        }
    }
}
