package com.example.keios.ui.page.main.ba

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal const val BA_CALENDAR_ENDPOINT = "/v1/activity/page-list"
internal const val BA_NETWORK_RETRY_ATTEMPTS = 1
internal const val BA_SYNC_TIMEOUT_MS = 20_000L
internal const val BA_CALENDAR_MAX_ITEMS = 10
internal const val BA_CALENDAR_CACHE_SCHEMA_VERSION = 3
internal const val BA_POOL_ENDPOINT = "/v1/cardPool/query-list"
internal const val BA_POOL_MAX_ITEMS = 10
internal const val BA_POOL_CACHE_SCHEMA_VERSION = 5

internal val BA_POOL_TAGS = listOf(
    5 to "常驻",
    6 to "限定",
    7 to "FES限定",
    8 to "联动",
    9 to "复刻",
    92 to "回忆招募"
)
internal val BA_POOL_TAG_NAME_MAP = BA_POOL_TAGS.toMap()
internal const val BA_POOL_FALLBACK_ACTIVE_TAG_ID = 6

internal fun <T> runWithRetry(attempts: Int = BA_NETWORK_RETRY_ATTEMPTS, block: () -> T): T {
    var lastError: Throwable? = null
    repeat(attempts.coerceAtLeast(1)) { index ->
        try {
            return block()
        } catch (t: Throwable) {
            lastError = t
            if (index < attempts - 1) Thread.sleep(300L)
        }
    }
    throw lastError ?: IllegalStateException("network retry failed")
}

internal fun <T> runWithHardTimeout(timeoutMs: Long, block: () -> T): T {
    val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ba-sync-hard-timeout").apply { isDaemon = true }
    }
    val future = executor.submit<T> { block() }
    return try {
        future.get(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (timeout: TimeoutException) {
        future.cancel(true)
        throw IllegalStateException("hard timeout ${timeoutMs}ms", timeout)
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        throw IllegalStateException("hard timeout interrupted", interrupted)
    } catch (execution: ExecutionException) {
        throw (execution.cause ?: execution)
    } finally {
        executor.shutdownNow()
    }
}

internal data class BaCalendarEntry(
    val id: Int,
    val title: String,
    val kindId: Int,
    val kindName: String,
    val beginAtMs: Long,
    val endAtMs: Long,
    val linkUrl: String,
    val imageUrl: String,
    val isRunning: Boolean
)

internal data class BaPoolEntry(
    val id: Int,
    val name: String,
    val tagId: Int,
    val tagName: String,
    val startAtMs: Long,
    val endAtMs: Long,
    val linkUrl: String,
    val imageUrl: String,
    val isRunning: Boolean
)

internal fun gameKeeServerId(serverIndex: Int): Int {
    return when (serverIndex) {
        0 -> 16
        1 -> 17
        else -> 15
    }
}

internal fun normalizeGameKeeLink(url: String): String {
    val raw = url.trim()
    if (raw.isBlank()) return "https://www.gamekee.com/ba/huodong"
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    return if (raw.startsWith("/")) "https://www.gamekee.com$raw" else "https://www.gamekee.com/$raw"
}

internal fun normalizeGameKeeImageLink(url: String): String {
    val raw = url.trim()
    if (raw.isBlank()) return ""
    if (raw.startsWith("file://")) return raw
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    if (raw.startsWith("//")) return "https:$raw"
    return if (raw.startsWith("/")) "https://www.gamekee.com$raw" else "https://www.gamekee.com/$raw"
}

internal fun looksLikeImageUrl(raw: String): Boolean {
    val value = raw.lowercase(Locale.ROOT)
    if (value.isBlank()) return false
    val hasProtocol = value.startsWith("http://") || value.startsWith("https://") || value.startsWith("//") || value.startsWith("/")
    if (!hasProtocol) return false
    if (value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".png") || value.endsWith(".webp") || value.endsWith(".gif")) {
        return true
    }
    return value.contains("image") || value.contains("img") || value.contains("upload") || value.contains("cdn")
}

internal fun findImageLinkRecursively(any: Any?, depth: Int = 0): String {
    if (any == null || depth > 3) return ""
    return when (any) {
        is String -> {
            val normalized = normalizeGameKeeImageLink(any)
            if (looksLikeImageUrl(normalized)) normalized else ""
        }

        is JSONObject -> {
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val found = findImageLinkRecursively(any.opt(key), depth + 1)
                if (found.isNotBlank()) return found
            }
            ""
        }

        is JSONArray -> {
            for (i in 0 until any.length()) {
                val found = findImageLinkRecursively(any.opt(i), depth + 1)
                if (found.isNotBlank()) return found
            }
            ""
        }

        else -> ""
    }
}

internal fun extractGameKeeImageLink(item: JSONObject): String {
    val directKeys = listOf(
        "image_url", "img_url", "cover_url", "cover", "cover_img", "cover_image",
        "topic_img", "topic_image", "title_img", "main_img", "list_img", "small_img",
        "image", "img", "pic_url", "pic", "thumb", "thumbnail", "avatar", "banner", "icon", "logo"
    )
    directKeys.forEach { key ->
        val value = normalizeGameKeeImageLink(item.optString(key))
        if (looksLikeImageUrl(value)) return value
    }

    val nestedKeys = listOf("cover", "image", "thumb", "thumbnail", "banner", "icon")
    val nestedValueKeys = listOf("url", "src", "image_url", "img_url", "cover", "thumb")
    nestedKeys.forEach { key ->
        val nested = item.optJSONObject(key) ?: return@forEach
        nestedValueKeys.forEach { nestedKey ->
            val value = normalizeGameKeeImageLink(nested.optString(nestedKey))
            if (value.isNotBlank()) return value
        }
    }

    val images = item.optJSONArray("images")
    if (images != null) {
        for (index in 0 until images.length()) {
            val imageObj = images.optJSONObject(index) ?: continue
            nestedValueKeys.forEach { nestedKey ->
                val value = normalizeGameKeeImageLink(imageObj.optString(nestedKey))
                if (value.isNotBlank()) return value
            }
        }
    }
    return findImageLinkRecursively(item)
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

internal fun parsePoolTagIds(raw: String): List<Int> {
    return Regex("\\d+")
        .findAll(raw)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
}

internal fun formatBaDateTimeNoYearInTimeZone(epochMillis: Long, timeZone: TimeZone): String {
    if (epochMillis <= 0L) return "-"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }.format(Date(epochMillis))
    }.getOrDefault("-")
}

internal fun fetchBaCalendarEntries(
    serverIndex: Int,
    nowMs: Long = System.currentTimeMillis()
): List<BaCalendarEntry> {
    val serverId = gameKeeServerId(serverIndex)
    val endpointPath = "$BA_CALENDAR_ENDPOINT?importance=0&sort=-1&keyword=&limit=999&page_no=1&serverId=$serverId&status=0"
    val body = runWithRetry {
        GameKeeFetchHelper.fetchJson(
            pathOrUrl = endpointPath,
            refererPath = "/ba/huodong/$serverId",
            extraHeaders = mapOf(
                "device-num" to "1",
                "game-alias" to "ba"
            )
        )
    }
    val root = JSONObject(body)
    if (root.optInt("code", -1) != 0) {
        error("API ${root.optInt("code", -1)}")
    }
    val data = root.optJSONArray("data") ?: return emptyList()
    val entries = mutableListOf<BaCalendarEntry>()
    for (index in 0 until data.length()) {
        val item = data.optJSONObject(index) ?: continue
        val title = item.optString("title").trim()
        if (title.isBlank()) continue

        val beginSec = item.optLong("begin_at", 0L)
        val endSec = item.optLong("end_at", 0L)
        if (beginSec <= 0L || endSec <= 0L) continue
        val beginAtMs = beginSec * 1000L
        val endAtMs = endSec * 1000L

        val kindId = item.optInt("activity_kind_id", 31)
        val kindName = item.optString("activity_kind_name").ifBlank { "其他" }
        entries += BaCalendarEntry(
            id = item.optInt("id", 0),
            title = title,
            kindId = kindId,
            kindName = kindName,
            beginAtMs = beginAtMs,
            endAtMs = endAtMs,
            linkUrl = normalizeGameKeeLink(item.optString("link_url")),
            imageUrl = extractGameKeeImageLink(item),
            isRunning = nowMs in beginAtMs until endAtMs
        )
    }
    return normalizeBaCalendarEntries(entries, nowMs)
}

internal fun normalizeBaCalendarEntries(entries: List<BaCalendarEntry>, nowMs: Long = System.currentTimeMillis()): List<BaCalendarEntry> {
    val normalized = entries
        .asSequence()
        .filter { it.title.isNotBlank() }
        .map { entry ->
            val beginAtMs = entry.beginAtMs.coerceAtLeast(0L)
            val endAtMs = entry.endAtMs.coerceAtLeast(beginAtMs)
            entry.copy(
                beginAtMs = beginAtMs,
                endAtMs = endAtMs,
                isRunning = nowMs in beginAtMs until endAtMs,
                linkUrl = normalizeGameKeeLink(entry.linkUrl),
                imageUrl = normalizeGameKeeImageLink(entry.imageUrl)
            )
        }
        .toList()

    if (normalized.isEmpty()) return emptyList()

    val activeOrUpcoming = normalized.filter { it.endAtMs > nowMs }
    val activeKindIds = activeOrUpcoming.map { it.kindId }.toSet()
    val fallbackMissingKinds = normalized
        .asSequence()
        .map { it.kindId }
        .distinct()
        .filter { it !in activeKindIds }
        .mapNotNull { missingKindId ->
            normalized
                .asSequence()
                .filter { it.kindId == missingKindId }
                .maxWithOrNull(
                    compareBy<BaCalendarEntry> { it.endAtMs }
                        .thenBy { it.beginAtMs }
                )
        }
        .toList()

    val merged = buildList {
        addAll(activeOrUpcoming)
        fallbackMissingKinds.forEach { candidate ->
            if (none { it.id == candidate.id }) add(candidate)
        }
    }

    val sorted = merged
        .sortedWith(
            compareBy<BaCalendarEntry>(
                {
                    when {
                        it.isRunning -> 0
                        it.endAtMs > nowMs -> 1
                        else -> 2
                    }
                },
                {
                    when {
                        it.isRunning -> it.endAtMs
                        it.endAtMs > nowMs -> it.beginAtMs
                        else -> -it.endAtMs
                    }
                },
                { it.kindId }
            )
        )

    return sorted.take(BA_CALENDAR_MAX_ITEMS.coerceAtLeast(1))
}

internal fun encodeBaCalendarEntries(entries: List<BaCalendarEntry>): String {
    val arr = JSONArray()
    entries.forEach { item ->
        arr.put(
            JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("kindId", item.kindId)
                put("kindName", item.kindName)
                put("beginAtMs", item.beginAtMs)
                put("endAtMs", item.endAtMs)
                put("linkUrl", item.linkUrl)
                put("imageUrl", item.imageUrl)
            }
        )
    }
    return arr.toString()
}

internal fun decodeBaCalendarEntries(raw: String, nowMs: Long = System.currentTimeMillis()): List<BaCalendarEntry> {
    if (raw.isBlank()) return emptyList()
    val arr = JSONArray(raw)
    val parsed = mutableListOf<BaCalendarEntry>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val title = obj.optString("title").trim()
        if (title.isBlank()) continue
        val beginAtMs = obj.optLong("beginAtMs", 0L)
        val endAtMs = obj.optLong("endAtMs", 0L)
        if (beginAtMs <= 0L || endAtMs <= 0L) continue
        parsed += BaCalendarEntry(
            id = obj.optInt("id", 0),
            title = title,
            kindId = obj.optInt("kindId", 31),
            kindName = obj.optString("kindName").ifBlank { "其他" },
            beginAtMs = beginAtMs,
            endAtMs = endAtMs,
            linkUrl = normalizeGameKeeLink(obj.optString("linkUrl")),
            imageUrl = normalizeGameKeeImageLink(obj.optString("imageUrl")),
            isRunning = nowMs in beginAtMs until endAtMs
        )
    }
    return normalizeBaCalendarEntries(parsed, nowMs)
}

internal fun activityProgress(entry: BaCalendarEntry, nowMs: Long): Float {
    val total = (entry.endAtMs - entry.beginAtMs).coerceAtLeast(1L)
    val elapsed = (nowMs - entry.beginAtMs).coerceIn(0L, total)
    return (elapsed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

internal fun normalizeBaPoolEntries(entries: List<BaPoolEntry>, nowMs: Long = System.currentTimeMillis()): List<BaPoolEntry> {
    val normalized = entries
        .asSequence()
        .filter { it.name.isNotBlank() }
        .map { entry ->
            val startAtMs = entry.startAtMs.coerceAtLeast(0L)
            val endAtMs = entry.endAtMs.coerceAtLeast(startAtMs)
            entry.copy(
                startAtMs = startAtMs,
                endAtMs = endAtMs,
                isRunning = nowMs in startAtMs until endAtMs,
                linkUrl = normalizeGameKeeLink(entry.linkUrl),
                imageUrl = normalizeGameKeeImageLink(entry.imageUrl)
            )
        }
        .toList()
    if (normalized.isEmpty()) return emptyList()

    val activeOrUpcoming = normalized.filter { it.endAtMs > nowMs }
    val activeTagIds = activeOrUpcoming.map { it.tagId }.toSet()
    val fallbackMissingTags = BA_POOL_TAGS
        .asSequence()
        .map { it.first }
        .filter { it !in activeTagIds }
        .mapNotNull { missingTagId ->
            normalized
                .asSequence()
                .filter { it.tagId == missingTagId }
                .maxWithOrNull(
                    compareBy<BaPoolEntry> { it.endAtMs }
                        .thenBy { it.startAtMs }
                )
        }
        .toList()

    val merged = buildList {
        addAll(activeOrUpcoming)
        fallbackMissingTags.forEach { candidate ->
            if (none { it.id == candidate.id }) add(candidate)
        }
    }

    val sorted = merged.sortedWith(
        compareBy<BaPoolEntry>(
            {
                when {
                    it.isRunning -> 0
                    it.endAtMs > nowMs -> 1
                    else -> 2
                }
            },
            {
                when {
                    it.isRunning -> it.endAtMs
                    it.endAtMs > nowMs -> it.startAtMs
                    else -> -it.endAtMs
                }
            },
            { it.tagId },
            { it.id }
        )
    )

    return sorted.take(BA_POOL_MAX_ITEMS.coerceAtLeast(1))
}

internal fun fetchBaPoolEntriesFromAll(
    serverIndex: Int,
    nowMs: Long
): List<BaPoolEntry> {
    val serverId = gameKeeServerId(serverIndex)
    val endpointPath = "$BA_POOL_ENDPOINT?order_by=-1&card_tag_id=&keyword=&kind_id=6&status=0&serverId=$serverId"
    val body = runWithRetry {
        GameKeeFetchHelper.fetchJson(
            pathOrUrl = endpointPath,
            refererPath = "/ba/kachi/$serverId",
            extraHeaders = mapOf(
                "device-num" to "1",
                "game-alias" to "ba"
            )
        )
    }
    val root = JSONObject(body)
    if (root.optInt("code", -1) != 0) {
        error("API ${root.optInt("code", -1)}")
    }
    val data = root.optJSONArray("data") ?: return emptyList()
    val entries = mutableListOf<BaPoolEntry>()
    for (index in 0 until data.length()) {
        val item = data.optJSONObject(index) ?: continue
        val name = item.optString("name").trim()
        if (name.isBlank()) continue

        val startSec = item.optLong("start_at", 0L)
        val endSec = item.optLong("end_at", 0L)
        if (startSec <= 0L || endSec <= 0L) continue
        val startAtMs = startSec * 1000L
        val endAtMs = endSec * 1000L
        val isRunning = nowMs in startAtMs until endAtMs
        val isUpcoming = startAtMs > nowMs

        val knownTagId = parsePoolTagIds(item.optString("tag_id"))
            .firstOrNull { it in BA_POOL_TAG_NAME_MAP }
        val normalizedTagId = when {
            knownTagId != null -> knownTagId
            isRunning || isUpcoming -> BA_POOL_FALLBACK_ACTIVE_TAG_ID
            else -> null
        } ?: continue
        val normalizedTagName = BA_POOL_TAG_NAME_MAP[normalizedTagId] ?: "卡池"

        entries += BaPoolEntry(
            id = item.optInt("id", 0),
            name = name,
            tagId = normalizedTagId,
            tagName = normalizedTagName,
            startAtMs = startAtMs,
            endAtMs = endAtMs,
            linkUrl = normalizeGameKeeLink(item.optString("link_url")),
            imageUrl = extractGameKeeImageLink(item),
            isRunning = isRunning
        )
    }
    return entries
}

internal fun fetchBaPoolEntries(
    serverIndex: Int,
    nowMs: Long = System.currentTimeMillis()
): List<BaPoolEntry> {
    val merged = mutableMapOf<Int, BaPoolEntry>()
    val sourceErrors = mutableListOf<Throwable>()
    runCatching { fetchBaPoolEntriesFromAll(serverIndex, nowMs) }
        .onSuccess { entries ->
            entries.forEach { entry ->
                val existing = merged[entry.id]
                if (
                    existing == null ||
                    (existing.endAtMs <= nowMs && entry.endAtMs > nowMs) ||
                    (entry.endAtMs > existing.endAtMs) ||
                    (entry.startAtMs > existing.startAtMs)
                ) {
                    merged[entry.id] = entry
                }
            }
        }
        .onFailure { sourceErrors += it }

    if (merged.isEmpty() && sourceErrors.isNotEmpty()) {
        val aggregate = IllegalStateException("pool all sources failed")
        sourceErrors.forEach { aggregate.addSuppressed(it) }
        throw aggregate
    }

    return normalizeBaPoolEntries(merged.values.toList(), nowMs)
}

internal fun encodeBaPoolEntries(entries: List<BaPoolEntry>): String {
    val arr = JSONArray()
    entries.forEach { item ->
        arr.put(
            JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("tagId", item.tagId)
                put("tagName", item.tagName)
                put("startAtMs", item.startAtMs)
                put("endAtMs", item.endAtMs)
                put("linkUrl", item.linkUrl)
                put("imageUrl", item.imageUrl)
            }
        )
    }
    return arr.toString()
}

internal fun decodeBaPoolEntries(raw: String, nowMs: Long = System.currentTimeMillis()): List<BaPoolEntry> {
    if (raw.isBlank()) return emptyList()
    val arr = JSONArray(raw)
    val parsed = mutableListOf<BaPoolEntry>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val name = obj.optString("name").trim()
        if (name.isBlank()) continue
        val startAtMs = obj.optLong("startAtMs", 0L)
        val endAtMs = obj.optLong("endAtMs", 0L)
        if (startAtMs <= 0L || endAtMs <= 0L) continue
        parsed += BaPoolEntry(
            id = obj.optInt("id", 0),
            name = name,
            tagId = obj.optInt("tagId", 0),
            tagName = obj.optString("tagName").ifBlank { "其他" },
            startAtMs = startAtMs,
            endAtMs = endAtMs,
            linkUrl = normalizeGameKeeLink(obj.optString("linkUrl")),
            imageUrl = normalizeGameKeeImageLink(obj.optString("imageUrl")),
            isRunning = nowMs in startAtMs until endAtMs
        )
    }
    return normalizeBaPoolEntries(parsed, nowMs)
}

internal fun poolProgress(entry: BaPoolEntry, nowMs: Long): Float {
    val total = (entry.endAtMs - entry.startAtMs).coerceAtLeast(1L)
    val elapsed = (nowMs - entry.startAtMs).coerceIn(0L, total)
    return (elapsed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

internal fun isNetworkAvailable(context: Context): Boolean {
    return runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
}
