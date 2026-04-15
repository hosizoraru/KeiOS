package com.example.keios.ui.page.main.ba

import android.content.Context
import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

internal object BaCalendarPoolImageCache {
    private const val ROOT_DIR = "ba_calendar_pool_media"
    private const val MAX_PARALLEL_DOWNLOADS = 3
    private const val INDEX_KV_ID = "ba_calendar_pool_media_index"
    private const val INDEX_VERSION = 1
    private const val KEY_INDEX_VERSION = "index_version"

    private val indexStore: MMKV by lazy { MMKV.mmkvWithID(INDEX_KV_ID) }

    private enum class Category(val folderName: String) {
        Calendar("calendar"),
        Pool("pool"),
    }

    private fun rootDir(context: Context): File = File(context.cacheDir, ROOT_DIR)

    private fun categoryDir(context: Context, category: Category, serverIndex: Int): File {
        return File(rootDir(context), "${category.folderName}/${serverIndex.coerceIn(0, 2)}")
    }

    private fun normalizeRemoteTarget(raw: String): String {
        val normalized = normalizeGameKeeImageLink(raw.trim())
        if (normalized.isBlank()) return ""
        if (normalized.startsWith("file://")) return ""
        return normalized
    }

    private fun fileExtFromUrl(url: String): String {
        val normalized = url.substringBefore('?').substringBefore('#')
        val segment = runCatching { Uri.parse(normalized).lastPathSegment.orEmpty() }
            .getOrDefault("")
            .substringAfterLast('.', "")
            .lowercase()
        val ext = when (segment) {
            "png", "jpg", "jpeg", "webp", "gif", "bmp", "svg" -> segment
            else -> "bin"
        }
        return ".$ext"
    }

    private fun targetFileName(normalizedRemoteUrl: String): String {
        return sha1(normalizedRemoteUrl) + fileExtFromUrl(normalizedRemoteUrl)
    }

    private fun targetFile(
        context: Context,
        category: Category,
        serverIndex: Int,
        normalizedRemoteUrl: String
    ): File {
        return File(categoryDir(context, category, serverIndex), targetFileName(normalizedRemoteUrl))
    }

    suspend fun prefetchForCalendar(
        context: Context,
        serverIndex: Int,
        entries: List<BaCalendarEntry>
    ) {
        prefetchForCategory(
            context = context,
            category = Category.Calendar,
            serverIndex = serverIndex,
            rawUrls = entries.map { it.imageUrl }
        )
    }

    suspend fun prefetchForPool(
        context: Context,
        serverIndex: Int,
        entries: List<BaPoolEntry>
    ) {
        prefetchForCategory(
            context = context,
            category = Category.Pool,
            serverIndex = serverIndex,
            rawUrls = entries.map { it.imageUrl }
        )
    }

    private suspend fun prefetchForCategory(
        context: Context,
        category: Category,
        serverIndex: Int,
        rawUrls: List<String>
    ) = withContext(Dispatchers.IO) {
        val targets = rawUrls
            .map(::normalizeRemoteTarget)
            .filter { it.isNotBlank() }
            .distinct()
        if (targets.isEmpty()) return@withContext

        val dir = categoryDir(context, category, serverIndex)
        dir.mkdirs()

        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)
            targets.map { url ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val file = targetFile(context, category, serverIndex, url)
                        if (file.exists() && file.length() > 0L) return@withPermit
                        runCatching { GameKeeFetchHelper.downloadToFile(url, file) }
                        if (file.exists() && file.length() <= 0L) {
                            runCatching { file.delete() }
                        }
                    }
                }
            }.awaitAll()
        }
        rebuildScopeIndex(context, category, serverIndex)
    }

    suspend fun pruneCalendarStale(
        context: Context,
        serverIndex: Int,
        entries: List<BaCalendarEntry>
    ) {
        pruneCategoryStale(
            context = context,
            category = Category.Calendar,
            serverIndex = serverIndex,
            rawUrls = entries.map { it.imageUrl }
        )
    }

    suspend fun prunePoolStale(
        context: Context,
        serverIndex: Int,
        entries: List<BaPoolEntry>
    ) {
        pruneCategoryStale(
            context = context,
            category = Category.Pool,
            serverIndex = serverIndex,
            rawUrls = entries.map { it.imageUrl }
        )
    }

    private suspend fun pruneCategoryStale(
        context: Context,
        category: Category,
        serverIndex: Int,
        rawUrls: List<String>
    ) = withContext(Dispatchers.IO) {
        val dir = categoryDir(context, category, serverIndex)
        if (!dir.exists()) return@withContext

        val keepNames = rawUrls
            .map(::normalizeRemoteTarget)
            .filter { it.isNotBlank() }
            .map(::targetFileName)
            .toSet()

        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name !in keepNames) {
                runCatching { file.delete() }
            }
        }
        rebuildScopeIndex(context, category, serverIndex)
    }

    fun resolveCalendarImageUrl(
        context: Context,
        serverIndex: Int,
        rawUrl: String,
        localOnly: Boolean
    ): String {
        return resolveCategoryImageUrl(
            context = context,
            category = Category.Calendar,
            serverIndex = serverIndex,
            rawUrl = rawUrl,
            localOnly = localOnly
        )
    }

    fun resolvePoolImageUrl(
        context: Context,
        serverIndex: Int,
        rawUrl: String,
        localOnly: Boolean
    ): String {
        return resolveCategoryImageUrl(
            context = context,
            category = Category.Pool,
            serverIndex = serverIndex,
            rawUrl = rawUrl,
            localOnly = localOnly
        )
    }

    private fun resolveCategoryImageUrl(
        context: Context,
        category: Category,
        serverIndex: Int,
        rawUrl: String,
        localOnly: Boolean
    ): String {
        val raw = rawUrl.trim()
        if (raw.isBlank()) return ""
        if (raw.startsWith("file://")) return raw
        val normalizedRemote = normalizeRemoteTarget(raw)
        if (normalizedRemote.isBlank()) return if (localOnly) "" else normalizeGameKeeImageLink(raw)
        val file = targetFile(context, category, serverIndex, normalizedRemote)
        if (file.exists() && file.length() > 0L) {
            return Uri.fromFile(file).toString()
        }
        return if (localOnly) "" else normalizedRemote
    }

    fun applyCachedCalendarImageUrls(
        context: Context,
        serverIndex: Int,
        entries: List<BaCalendarEntry>,
        localOnly: Boolean
    ): List<BaCalendarEntry> {
        if (entries.isEmpty()) return emptyList()
        return entries.map { entry ->
            entry.copy(
                imageUrl = resolveCalendarImageUrl(
                    context = context,
                    serverIndex = serverIndex,
                    rawUrl = entry.imageUrl,
                    localOnly = localOnly
                )
            )
        }
    }

    fun applyCachedPoolImageUrls(
        context: Context,
        serverIndex: Int,
        entries: List<BaPoolEntry>,
        localOnly: Boolean
    ): List<BaPoolEntry> {
        if (entries.isEmpty()) return emptyList()
        return entries.map { entry ->
            entry.copy(
                imageUrl = resolvePoolImageUrl(
                    context = context,
                    serverIndex = serverIndex,
                    rawUrl = entry.imageUrl,
                    localOnly = localOnly
                )
            )
        }
    }

    fun clearCalendarCache(context: Context, serverIndex: Int) {
        runCatching { categoryDir(context, Category.Calendar, serverIndex).deleteRecursively() }
        clearScopeIndex(Category.Calendar, serverIndex)
    }

    fun clearPoolCache(context: Context, serverIndex: Int) {
        runCatching { categoryDir(context, Category.Pool, serverIndex).deleteRecursively() }
        clearScopeIndex(Category.Pool, serverIndex)
    }

    fun clearAll(context: Context) {
        runCatching { rootDir(context).deleteRecursively() }
        clearIndex()
    }

    fun cacheFileCount(context: Context): Int = loadIndexSummary(context).count

    fun cacheTotalBytes(context: Context): Long = loadIndexSummary(context).bytes

    fun latestModifiedAtMs(context: Context): Long = loadIndexSummary(context).latest

    private data class ScopeSummary(
        val count: Int,
        val bytes: Long,
        val latest: Long
    )

    private data class MediaSummary(
        val count: Int,
        val bytes: Long,
        val latest: Long
    )

    private fun scopePrefix(category: Category, serverIndex: Int): String {
        return "${category.folderName}_${serverIndex.coerceIn(0, 2)}"
    }

    private fun scopeCountKey(category: Category, serverIndex: Int): String = "${scopePrefix(category, serverIndex)}_count"

    private fun scopeBytesKey(category: Category, serverIndex: Int): String = "${scopePrefix(category, serverIndex)}_bytes"

    private fun scopeLatestKey(category: Category, serverIndex: Int): String = "${scopePrefix(category, serverIndex)}_latest"

    private fun readScopeSummary(category: Category, serverIndex: Int, kv: MMKV = indexStore): ScopeSummary? {
        val countKey = scopeCountKey(category, serverIndex)
        val bytesKey = scopeBytesKey(category, serverIndex)
        if (!kv.containsKey(countKey) || !kv.containsKey(bytesKey)) return null
        val count = kv.decodeInt(countKey, -1)
        val bytes = kv.decodeLong(bytesKey, -1L)
        if (count < 0 || bytes < 0L) return null
        return ScopeSummary(
            count = count,
            bytes = bytes,
            latest = kv.decodeLong(scopeLatestKey(category, serverIndex), 0L).coerceAtLeast(0L)
        )
    }

    private fun writeScopeSummary(
        category: Category,
        serverIndex: Int,
        summary: ScopeSummary,
        kv: MMKV = indexStore
    ) {
        val countKey = scopeCountKey(category, serverIndex)
        val bytesKey = scopeBytesKey(category, serverIndex)
        val latestKey = scopeLatestKey(category, serverIndex)
        if (summary.count <= 0 || summary.bytes <= 0L) {
            kv.removeValueForKey(countKey)
            kv.removeValueForKey(bytesKey)
            kv.removeValueForKey(latestKey)
        } else {
            kv.encode(countKey, summary.count)
            kv.encode(bytesKey, summary.bytes)
            kv.encode(latestKey, summary.latest.coerceAtLeast(0L))
        }
        kv.encode(KEY_INDEX_VERSION, INDEX_VERSION)
    }

    private fun scanScopeSummary(dir: File): ScopeSummary {
        if (!dir.exists()) return ScopeSummary(count = 0, bytes = 0L, latest = 0L)
        var count = 0
        var bytes = 0L
        var latest = 0L
        dir.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .forEach { file ->
                count += 1
                bytes += file.length()
                latest = maxOf(latest, file.lastModified())
            }
        return ScopeSummary(count = count, bytes = bytes, latest = latest)
    }

    private fun rebuildScopeIndex(context: Context, category: Category, serverIndex: Int) {
        val summary = scanScopeSummary(categoryDir(context, category, serverIndex))
        writeScopeSummary(category, serverIndex, summary)
    }

    private fun clearScopeIndex(category: Category, serverIndex: Int) {
        writeScopeSummary(
            category = category,
            serverIndex = serverIndex,
            summary = ScopeSummary(count = 0, bytes = 0L, latest = 0L)
        )
    }

    private fun clearIndex() {
        val kv = indexStore
        Category.entries.forEach { category ->
            for (server in 0..2) {
                kv.removeValueForKey(scopeCountKey(category, server))
                kv.removeValueForKey(scopeBytesKey(category, server))
                kv.removeValueForKey(scopeLatestKey(category, server))
            }
        }
        kv.removeValueForKey(KEY_INDEX_VERSION)
        kv.trim()
    }

    private fun aggregateSummaryFromIndex(kv: MMKV = indexStore): MediaSummary {
        var totalCount = 0
        var totalBytes = 0L
        var latest = 0L
        Category.entries.forEach { category ->
            for (server in 0..2) {
                val summary = readScopeSummary(category, server, kv) ?: continue
                totalCount += summary.count
                totalBytes += summary.bytes
                latest = maxOf(latest, summary.latest)
            }
        }
        return MediaSummary(
            count = totalCount.coerceAtLeast(0),
            bytes = totalBytes.coerceAtLeast(0L),
            latest = latest.coerceAtLeast(0L)
        )
    }

    private fun rebuildAllIndex(context: Context): MediaSummary {
        Category.entries.forEach { category ->
            for (server in 0..2) {
                rebuildScopeIndex(context, category, server)
            }
        }
        return aggregateSummaryFromIndex()
    }

    private fun loadIndexSummary(context: Context): MediaSummary {
        val root = rootDir(context)
        val kv = indexStore
        val hasIndex = kv.decodeInt(KEY_INDEX_VERSION, 0) == INDEX_VERSION
        return when {
            !root.exists() -> {
                if (hasIndex) clearIndex()
                MediaSummary(count = 0, bytes = 0L, latest = 0L)
            }
            !hasIndex -> rebuildAllIndex(context)
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
