package os.kei.ui.page.main.ba.support

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import org.json.JSONObject
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

internal val BA_POOL_TAG_IDS = listOf(5, 6, 7, 8, 9, 92)
internal val BA_POOL_TAG_ID_SET = BA_POOL_TAG_IDS.toSet()
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
            kindName = normalizeBaCalendarKindFallback(obj.optString("kindName")),
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
            tagName = normalizeBaPoolTagFallback(obj.optString("tagName")),
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
