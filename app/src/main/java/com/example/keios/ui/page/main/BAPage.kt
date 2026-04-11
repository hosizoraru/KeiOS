package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ba.helper.GameKeeFetchHelper
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.ceil
import kotlin.math.roundToInt

private enum class BAInitState {
    Empty,
    Draft
}

private enum class BaCalendarRefreshIntervalOption(val hours: Int, val label: String) {
    Hour1(1, "1 小时"),
    Hour3(3, "3 小时"),
    Hour6(6, "6 小时"),
    Hour12(12, "12 小时"),
    Hour24(24, "24 小时");

    companion object {
        fun fromHours(hours: Int): BaCalendarRefreshIntervalOption {
            return entries.firstOrNull { it.hours == hours } ?: Hour12
        }
    }
}

private const val BA_AP_MAX = 999
private const val BA_AP_LIMIT_MAX = 240
private const val BA_AP_REGEN_INTERVAL_MS = 6 * 60 * 1000L
private const val BA_AP_REGEN_TICK_MS = 30_000L
private const val BA_CAFE_HOURLY_INTERVAL_MS = 60 * 60 * 1000L
private const val BA_HEADPAT_COOLDOWN_MS = 3 * 60 * 60 * 1000L
private const val BA_INVITE_COOLDOWN_MS = 20 * 60 * 60 * 1000L
private const val BA_DEFAULT_NICKNAME = "Kei"
private const val BA_DEFAULT_FRIEND_CODE = "ARISUKEI"
private val BA_CAFE_DAILY_AP_BY_LEVEL = intArrayOf(92, 152, 222, 302, 390, 460, 530, 600, 570, 740)

private fun displayAp(apExact: Double): Int = apExact.coerceAtLeast(0.0).toInt()

private fun normalizeAp(apExact: Double): Double {
    return (apExact.coerceIn(0.0, BA_AP_MAX.toDouble()) * 1000.0).roundToInt() / 1000.0
}

private fun fractionalApPart(apExact: Double): Double {
    val normalized = normalizeAp(apExact)
    val integerPart = displayAp(normalized).toDouble()
    return normalizeAp(normalized - integerPart).coerceIn(0.0, 0.999)
}

private fun cafeHourlyGain(level: Int): Double {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1] / 24.0
}

private fun cafeDailyCapacity(level: Int): Int {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1]
}

private fun cafeStorageCap(level: Int): Double {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1].toDouble()
}

private fun floorToHourMs(epochMs: Long): Long = epochMs - (epochMs % BA_CAFE_HOURLY_INTERVAL_MS)

private fun serverRefreshTimeZone(serverIndex: Int): TimeZone {
    return if (serverIndex == 0) TimeZone.getTimeZone("Asia/Shanghai") else TimeZone.getTimeZone("Asia/Tokyo")
}

private fun nextCafeStudentRefreshMs(fromMs: Long, serverIndex: Int): Long {
    val timeZone = serverRefreshTimeZone(serverIndex)
    val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = fromMs }
    val dayStartCal = Calendar.getInstance(timeZone).apply {
        timeInMillis = fromMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val refresh4 = (dayStartCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 4) }.timeInMillis
    val refresh16 = (dayStartCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 16) }.timeInMillis
    return when {
        fromMs < refresh4 -> refresh4
        fromMs < refresh16 -> refresh16
        else -> {
            nowCal.add(Calendar.DAY_OF_YEAR, 1)
            nowCal.set(Calendar.HOUR_OF_DAY, 4)
            nowCal.set(Calendar.MINUTE, 0)
            nowCal.set(Calendar.SECOND, 0)
            nowCal.set(Calendar.MILLISECOND, 0)
            nowCal.timeInMillis
        }
    }
}

private fun nextArenaRefreshMs(fromMs: Long, serverIndex: Int): Long {
    val timeZone = serverRefreshTimeZone(serverIndex)
    val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = fromMs }
    val refreshCal = Calendar.getInstance(timeZone).apply {
        timeInMillis = fromMs
        set(Calendar.HOUR_OF_DAY, 14)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (fromMs >= refreshCal.timeInMillis) {
        nowCal.add(Calendar.DAY_OF_YEAR, 1)
        nowCal.set(Calendar.HOUR_OF_DAY, 14)
        nowCal.set(Calendar.MINUTE, 0)
        nowCal.set(Calendar.SECOND, 0)
        nowCal.set(Calendar.MILLISECOND, 0)
        return nowCal.timeInMillis
    }
    return refreshCal.timeInMillis
}

private fun calculateNextHeadpatAvailableMs(lastHeadpatMs: Long, serverIndex: Int): Long {
    if (lastHeadpatMs <= 0L) return 0L
    val cooldownReadyAt = lastHeadpatMs + BA_HEADPAT_COOLDOWN_MS
    val refreshAt = nextCafeStudentRefreshMs(lastHeadpatMs, serverIndex)
    return minOf(cooldownReadyAt, refreshAt)
}

private fun calculateInviteTicketAvailableMs(lastUsedMs: Long): Long {
    if (lastUsedMs <= 0L) return 0L
    return lastUsedMs + BA_INVITE_COOLDOWN_MS
}

private fun formatBaDateTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "未同步"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))
    }.getOrDefault("未同步")
}

private fun formatBaDateTimeNoSeconds(epochMillis: Long): String {
    if (epochMillis <= 0L) return "未同步"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }.getOrDefault("未同步")
}

private fun formatBaRemainingTime(targetMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val remainMs = (targetMs - nowMs).coerceAtLeast(0L)
    var totalSeconds = (remainMs + 999L) / 1000L
    val days = totalSeconds / 86_400L
    totalSeconds %= 86_400L
    val hours = totalSeconds / 3_600L
    totalSeconds %= 3_600L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    val parts = mutableListOf<String>()
    if (days > 0L) parts += "${days}d"
    if (hours > 0L) parts += "${hours}h"
    if (minutes > 0L) parts += "${minutes}min"
    if (seconds > 0L || parts.isEmpty()) parts += "${seconds}s"
    return parts.joinToString(" ")
}

private const val BA_CALENDAR_ENDPOINT = "/v1/activity/page-list"
private const val BA_NETWORK_RETRY_ATTEMPTS = 1
private const val BA_SYNC_TIMEOUT_MS = 20_000L
private const val BA_CALENDAR_MAX_ITEMS = 6
private const val BA_CALENDAR_CACHE_SCHEMA_VERSION = 3
private const val BA_POOL_ENDPOINT = "/v1/cardPool/query-list"
private const val BA_POOL_MAX_ITEMS = 6
private const val BA_POOL_CACHE_SCHEMA_VERSION = 5

private val BA_POOL_TAGS = listOf(
    5 to "常驻",
    6 to "限定",
    7 to "FES限定",
    8 to "联动",
    9 to "复刻",
    92 to "回忆招募"
)
private val BA_POOL_TAG_NAME_MAP = BA_POOL_TAGS.toMap()
private const val BA_POOL_FALLBACK_ACTIVE_TAG_ID = 6

private fun <T> runWithRetry(attempts: Int = BA_NETWORK_RETRY_ATTEMPTS, block: () -> T): T {
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

private fun <T> runWithHardTimeout(timeoutMs: Long, block: () -> T): T {
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

private data class BaCalendarEntry(
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

private data class BaPoolEntry(
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

private fun gameKeeServerId(serverIndex: Int): Int {
    return when (serverIndex) {
        0 -> 16 // 国服
        1 -> 17 // 国际服
        else -> 15 // 日服
    }
}

private fun normalizeGameKeeLink(url: String): String {
    val raw = url.trim()
    if (raw.isBlank()) return "https://www.gamekee.com/ba/huodong"
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    return if (raw.startsWith("/")) "https://www.gamekee.com$raw" else "https://www.gamekee.com/$raw"
}

private fun normalizeGameKeeImageLink(url: String): String {
    val raw = url.trim()
    if (raw.isBlank()) return ""
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    if (raw.startsWith("//")) return "https:$raw"
    return if (raw.startsWith("/")) "https://www.gamekee.com$raw" else "https://www.gamekee.com/$raw"
}

private fun looksLikeImageUrl(raw: String): Boolean {
    val value = raw.lowercase(Locale.ROOT)
    if (value.isBlank()) return false
    val hasProtocol = value.startsWith("http://") || value.startsWith("https://") || value.startsWith("//") || value.startsWith("/")
    if (!hasProtocol) return false
    if (value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".png") || value.endsWith(".webp") || value.endsWith(".gif")) {
        return true
    }
    return value.contains("image") || value.contains("img") || value.contains("upload") || value.contains("cdn")
}

private fun findImageLinkRecursively(any: Any?, depth: Int = 0): String {
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

private fun extractGameKeeImageLink(item: JSONObject): String {
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

@Composable
private fun GameKeeCoverImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val normalizedUrl = remember(imageUrl) { normalizeGameKeeImageLink(imageUrl) }
    if (normalizedUrl.isBlank()) return

    val bitmap by produceState<Bitmap?>(initialValue = null, normalizedUrl) {
        value = withContext(Dispatchers.IO) {
            runCatching { GameKeeFetchHelper.fetchImage(normalizedUrl) }
                .getOrNull()
        }
    }

    val rendered = bitmap ?: return
    val aspectRatioValue = remember(rendered.width, rendered.height) {
        val w = rendered.width.coerceAtLeast(1)
        val h = rendered.height.coerceAtLeast(1)
        (w.toFloat() / h.toFloat()).coerceIn(1.0f, 2.4f)
    }
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatioValue)
            .clip(RoundedCornerShape(12.dp))
    )
}

private fun parsePoolTagIds(raw: String): List<Int> {
    return Regex("\\d+")
        .findAll(raw)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
}

private fun formatBaDateTimeNoYearInTimeZone(epochMillis: Long, timeZone: TimeZone): String {
    if (epochMillis <= 0L) return "-"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }.format(Date(epochMillis))
    }.getOrDefault("-")
}

private fun fetchBaCalendarEntries(
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


private fun normalizeBaCalendarEntries(entries: List<BaCalendarEntry>, nowMs: Long = System.currentTimeMillis()): List<BaCalendarEntry> {
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

    val maxItems = BA_CALENDAR_MAX_ITEMS.coerceAtLeast(activeOrUpcoming.size + fallbackMissingKinds.size)
    return sorted.take(maxItems)
}

private fun encodeBaCalendarEntries(entries: List<BaCalendarEntry>): String {
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

private fun decodeBaCalendarEntries(raw: String, nowMs: Long = System.currentTimeMillis()): List<BaCalendarEntry> {
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

private fun activityProgress(entry: BaCalendarEntry, nowMs: Long): Float {
    val total = (entry.endAtMs - entry.beginAtMs).coerceAtLeast(1L)
    val elapsed = (nowMs - entry.beginAtMs).coerceIn(0L, total)
    return (elapsed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

private fun normalizeBaPoolEntries(entries: List<BaPoolEntry>, nowMs: Long = System.currentTimeMillis()): List<BaPoolEntry> {
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

    val maxItems = BA_POOL_MAX_ITEMS.coerceAtLeast(activeOrUpcoming.size + fallbackMissingTags.size)
    return sorted.take(maxItems)
}

private fun fetchBaPoolEntriesFromAll(
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

private fun fetchBaPoolEntries(
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

    // Keep pool sync responsive: "all source" already covers all tags.
    // Tag-by-tag补抓在网络异常时会显著拉长同步时间，这里改为不阻塞主同步流程。

    if (merged.isEmpty() && sourceErrors.isNotEmpty()) {
        val aggregate = IllegalStateException("pool all sources failed")
        sourceErrors.forEach { aggregate.addSuppressed(it) }
        throw aggregate
    }

    return normalizeBaPoolEntries(merged.values.toList(), nowMs)
}

private fun encodeBaPoolEntries(entries: List<BaPoolEntry>): String {
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

private fun decodeBaPoolEntries(raw: String, nowMs: Long = System.currentTimeMillis()): List<BaPoolEntry> {
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

private fun poolProgress(entry: BaPoolEntry, nowMs: Long): Float {
    val total = (entry.endAtMs - entry.startAtMs).coerceAtLeast(1L)
    val elapsed = (nowMs - entry.startAtMs).coerceIn(0L, total)
    return (elapsed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

private fun isNetworkAvailable(context: Context): Boolean {
    return runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
}

private object BASettingsStore {
    private const val KV_ID = "ba_page_settings"
    private const val KEY_SERVER_INDEX = "server_index"
    private const val KEY_CAFE_LEVEL = "cafe_level"
    private const val KEY_CAFE_STORED_AP = "cafe_stored_ap"
    private const val KEY_CAFE_LAST_HOUR_MS = "cafe_last_hour_ms"
    private const val KEY_ID_NICKNAME = "id_nickname"
    private const val KEY_ID_FRIEND_CODE = "id_friend_code"
    private const val KEY_AP_LIMIT = "ap_limit"
    private const val KEY_AP_NOTIFY_ENABLED = "ap_notify_enabled"
    private const val KEY_AP_NOTIFY_THRESHOLD = "ap_notify_threshold"
    private const val KEY_AP_CURRENT = "ap_current"
    private const val KEY_AP_CURRENT_EXACT = "ap_current_exact"
    private const val KEY_AP_REGEN_BASE_MS = "ap_regen_base_ms"
    private const val KEY_AP_SYNC_MS = "ap_sync_ms"
    private const val KEY_POOL_SHOW_ENDED = "pool_show_ended"
    private const val KEY_ACTIVITY_SHOW_ENDED = "activity_show_ended"
    private const val KEY_SHOW_CALENDAR_POOL_IMAGES = "show_calendar_pool_images"
    private const val KEY_COFFEE_HEADPAT_MS = "coffee_headpat_ms"
    private const val KEY_COFFEE_INVITE1_USED_MS = "coffee_invite1_used_ms"
    private const val KEY_COFFEE_INVITE2_USED_MS = "coffee_invite2_used_ms"
    private const val KEY_LIST_SCROLL_INDEX = "list_scroll_index"
    private const val KEY_LIST_SCROLL_OFFSET = "list_scroll_offset"

    private const val DEFAULT_SERVER_INDEX = 2
    private const val DEFAULT_CAFE_LEVEL = 10
    private const val DEFAULT_CAFE_STORED_AP = 0.0
    private const val DEFAULT_ID_NICKNAME = BA_DEFAULT_NICKNAME
    private const val DEFAULT_ID_FRIEND_CODE = BA_DEFAULT_FRIEND_CODE
    private const val DEFAULT_AP_LIMIT = BA_AP_LIMIT_MAX
    private const val DEFAULT_AP_NOTIFY_THRESHOLD = 120
    private const val DEFAULT_AP_CURRENT = 0.0
    private const val KEY_CALENDAR_CACHE_PREFIX = "calendar_cache_"
    private const val KEY_CALENDAR_SYNC_PREFIX = "calendar_sync_"
    private const val KEY_CALENDAR_CACHE_VERSION_PREFIX = "calendar_cache_version_"
    private const val KEY_POOL_CACHE_PREFIX = "pool_cache_"
    private const val KEY_POOL_SYNC_PREFIX = "pool_sync_"
    private const val KEY_POOL_CACHE_VERSION_PREFIX = "pool_cache_version_"
    private const val KEY_CALENDAR_REFRESH_INTERVAL_HOURS = "calendar_refresh_interval_hours"
    private const val DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS = 12

    private fun calendarCacheKey(serverIndex: Int): String = "$KEY_CALENDAR_CACHE_PREFIX${serverIndex.coerceIn(0, 2)}"

    private fun calendarSyncKey(serverIndex: Int): String = "$KEY_CALENDAR_SYNC_PREFIX${serverIndex.coerceIn(0, 2)}"

    private fun calendarCacheVersionKey(serverIndex: Int): String =
        "$KEY_CALENDAR_CACHE_VERSION_PREFIX${serverIndex.coerceIn(0, 2)}"

    private fun poolCacheKey(serverIndex: Int): String = "$KEY_POOL_CACHE_PREFIX${serverIndex.coerceIn(0, 2)}"

    private fun poolSyncKey(serverIndex: Int): String = "$KEY_POOL_SYNC_PREFIX${serverIndex.coerceIn(0, 2)}"

    private fun poolCacheVersionKey(serverIndex: Int): String = "$KEY_POOL_CACHE_VERSION_PREFIX${serverIndex.coerceIn(0, 2)}"

    fun loadCalendarCache(serverIndex: Int): Pair<String, Long> {
        val store = kv()
        return store.decodeString(calendarCacheKey(serverIndex), "").orEmpty() to
            store.decodeLong(calendarSyncKey(serverIndex), 0L)
    }

    fun saveCalendarCache(serverIndex: Int, encodedEntries: String, syncMs: Long) {
        val store = kv()
        store.encode(calendarCacheKey(serverIndex), encodedEntries)
        store.encode(calendarSyncKey(serverIndex), syncMs.coerceAtLeast(0L))
        store.encode(calendarCacheVersionKey(serverIndex), BA_CALENDAR_CACHE_SCHEMA_VERSION)
    }

    fun loadCalendarCacheVersion(serverIndex: Int): Int {
        return kv().decodeInt(calendarCacheVersionKey(serverIndex), 0)
    }

    fun loadPoolCache(serverIndex: Int): Pair<String, Long> {
        val store = kv()
        return store.decodeString(poolCacheKey(serverIndex), "").orEmpty() to
            store.decodeLong(poolSyncKey(serverIndex), 0L)
    }

    fun savePoolCache(serverIndex: Int, encodedEntries: String, syncMs: Long) {
        val store = kv()
        store.encode(poolCacheKey(serverIndex), encodedEntries)
        store.encode(poolSyncKey(serverIndex), syncMs.coerceAtLeast(0L))
        store.encode(poolCacheVersionKey(serverIndex), BA_POOL_CACHE_SCHEMA_VERSION)
    }

    fun loadPoolCacheVersion(serverIndex: Int): Int {
        return kv().decodeInt(poolCacheVersionKey(serverIndex), 0)
    }

    fun loadPoolShowEnded(): Boolean = kv().decodeBool(KEY_POOL_SHOW_ENDED, false)

    fun savePoolShowEnded(enabled: Boolean) {
        kv().encode(KEY_POOL_SHOW_ENDED, enabled)
    }

    fun loadActivityShowEnded(): Boolean = kv().decodeBool(KEY_ACTIVITY_SHOW_ENDED, false)

    fun saveActivityShowEnded(enabled: Boolean) {
        kv().encode(KEY_ACTIVITY_SHOW_ENDED, enabled)
    }

    fun loadShowCalendarPoolImages(): Boolean = kv().decodeBool(KEY_SHOW_CALENDAR_POOL_IMAGES, true)

    fun saveShowCalendarPoolImages(enabled: Boolean) {
        kv().encode(KEY_SHOW_CALENDAR_POOL_IMAGES, enabled)
    }

    fun loadCalendarRefreshIntervalHours(): Int {
        val raw = kv().decodeInt(
            KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
            DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS
        )
        return BaCalendarRefreshIntervalOption.fromHours(raw).hours
    }

    fun saveCalendarRefreshIntervalHours(hours: Int) {
        kv().encode(
            KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
            BaCalendarRefreshIntervalOption.fromHours(hours).hours
        )
    }

    private fun kv(): MMKV = MMKV.mmkvWithID(KV_ID)

    fun loadServerIndex(): Int = kv().decodeInt(KEY_SERVER_INDEX, DEFAULT_SERVER_INDEX).coerceIn(0, 2)

    fun saveServerIndex(index: Int) {
        kv().encode(KEY_SERVER_INDEX, index.coerceIn(0, 2))
    }

    fun loadCafeLevel(): Int = kv().decodeInt(KEY_CAFE_LEVEL, DEFAULT_CAFE_LEVEL).coerceIn(1, 10)

    fun saveCafeLevel(level: Int) {
        kv().encode(KEY_CAFE_LEVEL, level.coerceIn(1, 10))
    }

    fun loadCafeStoredAp(): Double {
        val raw = kv().decodeString(KEY_CAFE_STORED_AP, DEFAULT_CAFE_STORED_AP.toString())
        return normalizeAp(raw?.toDoubleOrNull() ?: DEFAULT_CAFE_STORED_AP)
    }

    fun saveCafeStoredAp(storedAp: Double) {
        kv().encode(KEY_CAFE_STORED_AP, normalizeAp(storedAp).toString())
    }

    fun loadCafeLastHourMs(): Long = kv().decodeLong(KEY_CAFE_LAST_HOUR_MS, 0L)

    fun saveCafeLastHourMs(epochMs: Long) {
        kv().encode(KEY_CAFE_LAST_HOUR_MS, floorToHourMs(epochMs.coerceAtLeast(0L)))
    }

    fun loadIdNickname(): String {
        val raw = kv().decodeString(KEY_ID_NICKNAME, DEFAULT_ID_NICKNAME).orEmpty().take(10)
        return raw.ifEmpty { DEFAULT_ID_NICKNAME }
    }

    fun saveIdNickname(name: String) {
        val sanitized = name.take(10).ifEmpty { DEFAULT_ID_NICKNAME }
        kv().encode(KEY_ID_NICKNAME, sanitized)
    }

    fun loadIdFriendCode(): String {
        val normalized = kv().decodeString(KEY_ID_FRIEND_CODE, DEFAULT_ID_FRIEND_CODE)
            .orEmpty()
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
        return if (normalized.length == 8) normalized else DEFAULT_ID_FRIEND_CODE
    }

    fun saveIdFriendCode(code: String) {
        val normalized = code.uppercase(Locale.ROOT).filter { it in 'A'..'Z' }.take(8)
        kv().encode(
            KEY_ID_FRIEND_CODE,
            if (normalized.length == 8) normalized else DEFAULT_ID_FRIEND_CODE
        )
    }

    fun loadApLimit(): Int = kv().decodeInt(KEY_AP_LIMIT, DEFAULT_AP_LIMIT).coerceIn(0, BA_AP_LIMIT_MAX)

    fun saveApLimit(limit: Int) {
        kv().encode(KEY_AP_LIMIT, limit.coerceIn(0, BA_AP_LIMIT_MAX))
    }

    fun loadApNotifyEnabled(): Boolean = kv().decodeBool(KEY_AP_NOTIFY_ENABLED, false)

    fun saveApNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_AP_NOTIFY_ENABLED, enabled)
    }

    fun loadApNotifyThreshold(): Int =
        kv().decodeInt(KEY_AP_NOTIFY_THRESHOLD, DEFAULT_AP_NOTIFY_THRESHOLD).coerceIn(0, BA_AP_MAX)

    fun saveApNotifyThreshold(threshold: Int) {
        kv().encode(KEY_AP_NOTIFY_THRESHOLD, threshold.coerceIn(0, BA_AP_MAX))
    }

    fun loadApCurrent(): Double {
        val store = kv()
        val value = if (store.containsKey(KEY_AP_CURRENT_EXACT)) {
            store.decodeString(KEY_AP_CURRENT_EXACT, DEFAULT_AP_CURRENT.toString())?.toDoubleOrNull() ?: DEFAULT_AP_CURRENT
        } else {
            store.decodeInt(KEY_AP_CURRENT, DEFAULT_AP_CURRENT.toInt()).toDouble()
        }
        return normalizeAp(value.coerceIn(0.0, BA_AP_MAX.toDouble()))
    }

    fun saveApCurrent(current: Double) {
        val normalized = normalizeAp(current)
        kv().encode(KEY_AP_CURRENT_EXACT, normalized.toString())
        kv().encode(KEY_AP_CURRENT, displayAp(normalized))
    }

    fun loadApRegenBaseMs(): Long = kv().decodeLong(KEY_AP_REGEN_BASE_MS, 0L)

    fun saveApRegenBaseMs(epochMs: Long) {
        kv().encode(KEY_AP_REGEN_BASE_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadApSyncMs(): Long = kv().decodeLong(KEY_AP_SYNC_MS, 0L)

    fun saveApSyncMs(epochMs: Long) {
        kv().encode(KEY_AP_SYNC_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadCoffeeHeadpatMs(): Long = kv().decodeLong(KEY_COFFEE_HEADPAT_MS, 0L)

    fun saveCoffeeHeadpatMs(epochMs: Long) {
        kv().encode(KEY_COFFEE_HEADPAT_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadCoffeeInvite1UsedMs(): Long = kv().decodeLong(KEY_COFFEE_INVITE1_USED_MS, 0L)

    fun saveCoffeeInvite1UsedMs(epochMs: Long) {
        kv().encode(KEY_COFFEE_INVITE1_USED_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadCoffeeInvite2UsedMs(): Long = kv().decodeLong(KEY_COFFEE_INVITE2_USED_MS, 0L)

    fun saveCoffeeInvite2UsedMs(epochMs: Long) {
        kv().encode(KEY_COFFEE_INVITE2_USED_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadListScrollIndex(): Int = kv().decodeInt(KEY_LIST_SCROLL_INDEX, 0).coerceAtLeast(0)

    fun loadListScrollOffset(): Int = kv().decodeInt(KEY_LIST_SCROLL_OFFSET, 0).coerceAtLeast(0)

    fun saveListScrollState(index: Int, offset: Int) {
        kv().encode(KEY_LIST_SCROLL_INDEX, index.coerceAtLeast(0))
        kv().encode(KEY_LIST_SCROLL_OFFSET, offset.coerceAtLeast(0))
    }
}

@Composable
fun BAPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    onOpenPoolStudentGuide: (String) -> Unit = {},
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val initialScrollIndex = remember { BASettingsStore.loadListScrollIndex() }
    val initialScrollOffset = remember { BASettingsStore.loadListScrollOffset() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset
    )
    val scrollBehavior = MiuixScrollBehavior()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val baGlassBlur = 4.dp
    val baLightGlass = true
    val baBottomBarGlass = true
    val baSmallTitleMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    val serverOptions = remember { listOf("国服", "国际服", "日服") }
    val cafeLevelOptions = remember { (1..10).toList() }

    var initState by remember { mutableStateOf(BAInitState.Empty) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showOverviewServerPopup by remember { mutableStateOf(false) }
    var showCafeLevelPopup by remember { mutableStateOf(false) }
    var showCalendarIntervalPopup by remember { mutableStateOf(false) }

    val initialServerIndex = remember { BASettingsStore.loadServerIndex() }
    val initialSnapshotNowMs = remember { System.currentTimeMillis() }
    val initialCalendarCache = remember(initialServerIndex) {
        BASettingsStore.loadCalendarCache(initialServerIndex)
    }
    val initialPoolCache = remember(initialServerIndex) {
        BASettingsStore.loadPoolCache(initialServerIndex)
    }
    val initialCalendarEntries = remember(initialCalendarCache, initialSnapshotNowMs) {
        runCatching {
            decodeBaCalendarEntries(initialCalendarCache.first, initialSnapshotNowMs)
        }.getOrElse { emptyList() }
    }
    val initialPoolEntries = remember(initialPoolCache, initialSnapshotNowMs) {
        runCatching {
            decodeBaPoolEntries(initialPoolCache.first, initialSnapshotNowMs)
        }.getOrElse { emptyList() }
    }

    var serverIndex by remember { mutableIntStateOf(initialServerIndex) }
    var cafeLevel by remember { mutableIntStateOf(BASettingsStore.loadCafeLevel()) }
    var cafeStoredAp by remember { mutableStateOf(BASettingsStore.loadCafeStoredAp()) }
    var cafeLastHourMs by remember { mutableLongStateOf(BASettingsStore.loadCafeLastHourMs()) }
    var idNickname by remember { mutableStateOf(BASettingsStore.loadIdNickname()) }
    var idFriendCode by remember { mutableStateOf(BASettingsStore.loadIdFriendCode()) }
    var apLimit by remember { mutableIntStateOf(BASettingsStore.loadApLimit()) }
    var apCurrent by remember {
        mutableStateOf(BASettingsStore.loadApCurrent().coerceAtLeast(0.0))
    }
    var apRegenBaseMs by remember { mutableLongStateOf(BASettingsStore.loadApRegenBaseMs()) }
    var apSyncMs by remember { mutableLongStateOf(BASettingsStore.loadApSyncMs()) }
    var apNotifyEnabled by remember { mutableStateOf(BASettingsStore.loadApNotifyEnabled()) }
    var apNotifyThreshold by remember { mutableIntStateOf(BASettingsStore.loadApNotifyThreshold()) }
    var coffeeHeadpatMs by remember { mutableLongStateOf(BASettingsStore.loadCoffeeHeadpatMs()) }
    var coffeeInvite1UsedMs by remember { mutableLongStateOf(BASettingsStore.loadCoffeeInvite1UsedMs()) }
    var coffeeInvite2UsedMs by remember { mutableLongStateOf(BASettingsStore.loadCoffeeInvite2UsedMs()) }
    var uiNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var baCalendarEntries by remember { mutableStateOf(initialCalendarEntries) }
    var baCalendarLoading by remember { mutableStateOf(false) }
    var baCalendarError by remember { mutableStateOf<String?>(null) }
    var baCalendarLastSyncMs by remember { mutableLongStateOf(initialCalendarCache.second) }
    var baCalendarReloadSignal by remember { mutableIntStateOf(0) }
    var baPoolEntries by remember { mutableStateOf(initialPoolEntries) }
    var baPoolLoading by remember { mutableStateOf(false) }
    var baPoolError by remember { mutableStateOf<String?>(null) }
    var baPoolLastSyncMs by remember { mutableLongStateOf(initialPoolCache.second) }
    var baPoolReloadSignal by remember { mutableIntStateOf(0) }
    var showEndedPools by remember { mutableStateOf(BASettingsStore.loadPoolShowEnded()) }
    var showEndedActivities by remember { mutableStateOf(BASettingsStore.loadActivityShowEnded()) }
    var showCalendarPoolImages by remember { mutableStateOf(BASettingsStore.loadShowCalendarPoolImages()) }
    var calendarRefreshIntervalHours by remember {
        mutableIntStateOf(BASettingsStore.loadCalendarRefreshIntervalHours())
    }

    var sheetCafeLevel by remember { mutableIntStateOf(cafeLevel) }
    var sheetApNotifyEnabled by remember { mutableStateOf(apNotifyEnabled) }
    var sheetApNotifyThresholdText by remember { mutableStateOf(apNotifyThreshold.toString()) }
    var sheetShowEndedPools by remember { mutableStateOf(showEndedPools) }
    var sheetShowEndedActivities by remember { mutableStateOf(showEndedActivities) }
    var sheetShowCalendarPoolImages by remember { mutableStateOf(showCalendarPoolImages) }

    var apCurrentInput by remember { mutableStateOf(displayAp(apCurrent).toString()) }
    var apLimitInput by remember { mutableStateOf(apLimit.toString()) }
    var idNicknameInput by remember { mutableStateOf(idNickname) }
    var idFriendCodeInput by remember { mutableStateOf(idFriendCode) }
    var apLastNotifiedLevel by remember { mutableIntStateOf(-1) }
    var glassButtonPressCount by remember { mutableIntStateOf(0) }
    var consumedScrollToTopSignal by remember { mutableIntStateOf(scrollToTopSignal) }
    val officeSmallTitle = when (serverIndex) {
        0 -> "沙勒办公室"
        1 -> "夏萊行政室"
        else -> "夏莱办公室"
    }
    val suspendCardFeedback = glassButtonPressCount > 0
    val onGlassButtonPressedChange: (Boolean) -> Unit = { pressed ->
        glassButtonPressCount = if (pressed) {
            glassButtonPressCount + 1
        } else {
            (glassButtonPressCount - 1).coerceAtLeast(0)
        }
    }

    fun ensureRegenBase(nowMs: Long = System.currentTimeMillis()) {
        if (apRegenBaseMs <= 0L) {
            apRegenBaseMs = nowMs
            BASettingsStore.saveApRegenBaseMs(nowMs)
        }
    }

    fun ensureCafeHourBase(nowMs: Long = System.currentTimeMillis()) {
        val currentHour = floorToHourMs(nowMs)
        if (cafeLastHourMs <= 0L || cafeLastHourMs > currentHour) {
            cafeLastHourMs = currentHour
            BASettingsStore.saveCafeLastHourMs(currentHour)
        }
    }

    fun clampCafeStoredToCap() {
        val cap = cafeStorageCap(cafeLevel)
        val clamped = normalizeAp(cafeStoredAp.coerceIn(0.0, cap))
        if (clamped != cafeStoredAp) {
            cafeStoredAp = clamped
            BASettingsStore.saveCafeStoredAp(clamped)
        }
    }

    fun saveIdNicknameFromInput() {
        val sanitized = idNicknameInput.take(10).ifEmpty { BA_DEFAULT_NICKNAME }
        idNickname = sanitized
        idNicknameInput = sanitized
        BASettingsStore.saveIdNickname(sanitized)
    }

    fun saveIdFriendCodeFromInput() {
        val sanitized = idFriendCodeInput
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
        if (sanitized.length != 8) {
            Toast.makeText(context, "好友码需为8位大写字母", Toast.LENGTH_SHORT).show()
            idFriendCodeInput = idFriendCode
            return
        }
        idFriendCode = sanitized
        idFriendCodeInput = sanitized
        BASettingsStore.saveIdFriendCode(sanitized)
    }

    fun updateCurrentAp(newValue: Int, markSync: Boolean) {
        val nowMs = System.currentTimeMillis()
        val integerPart = newValue.coerceIn(0, BA_AP_MAX)
        val fractionPart = fractionalApPart(apCurrent)
        val next = normalizeAp(integerPart.toDouble() + fractionPart)
        apCurrent = next
        BASettingsStore.saveApCurrent(next)
        apRegenBaseMs = nowMs
        BASettingsStore.saveApRegenBaseMs(nowMs)
        if (markSync) {
            apSyncMs = nowMs
            BASettingsStore.saveApSyncMs(nowMs)
        }
    }

    fun addCurrentAp(delta: Double, markSync: Boolean) {
        if (delta <= 0.0) return
        val nowMs = System.currentTimeMillis()
        val next = normalizeAp(apCurrent + delta)
        apCurrent = next
        BASettingsStore.saveApCurrent(next)
        apRegenBaseMs = nowMs
        BASettingsStore.saveApRegenBaseMs(nowMs)
        if (markSync) {
            apSyncMs = nowMs
            BASettingsStore.saveApSyncMs(nowMs)
        }
    }

    fun updateApLimit(newLimit: Int) {
        val clamped = newLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        apLimit = clamped
        BASettingsStore.saveApLimit(clamped)
        ensureRegenBase()
    }

    fun applyApRegen(nowMs: Long = System.currentTimeMillis()) {
        val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        if (limit <= 0) {
            apRegenBaseMs = nowMs
            BASettingsStore.saveApRegenBaseMs(nowMs)
            return
        }
        if (apCurrent < 0.0) {
            apCurrent = 0.0
            BASettingsStore.saveApCurrent(0.0)
        }

        ensureRegenBase(nowMs)

        if (apCurrent >= limit.toDouble()) {
            if (apRegenBaseMs != nowMs) {
                apRegenBaseMs = nowMs
                BASettingsStore.saveApRegenBaseMs(nowMs)
            }
            return
        }

        val elapsed = (nowMs - apRegenBaseMs).coerceAtLeast(0L)
        val gained = (elapsed / BA_AP_REGEN_INTERVAL_MS).toInt()
        if (gained <= 0) return

        val pointsUntilStop = ceil(limit.toDouble() - apCurrent).toInt().coerceAtLeast(0)
        val pointsApplied = gained.coerceAtMost(pointsUntilStop)
        if (pointsApplied <= 0) return

        val nextAp = normalizeAp(apCurrent + pointsApplied.toDouble())
        apCurrent = nextAp
        BASettingsStore.saveApCurrent(nextAp)

        apRegenBaseMs = if (nextAp >= limit.toDouble()) {
            nowMs
        } else {
            apRegenBaseMs + pointsApplied * BA_AP_REGEN_INTERVAL_MS
        }
        BASettingsStore.saveApRegenBaseMs(apRegenBaseMs)
    }

    fun calculateApFullAtMs(nowMs: Long = System.currentTimeMillis()): Long {
        val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        if (limit <= 0) return nowMs
        val current = apCurrent.coerceAtLeast(0.0)
        if (current >= limit.toDouble()) return nowMs

        val base = apRegenBaseMs.takeIf { it > 0L } ?: nowMs
        val elapsed = (nowMs - base).coerceAtLeast(0L)
        val remainder = elapsed % BA_AP_REGEN_INTERVAL_MS
        val pointsNeeded = ceil(limit.toDouble() - current).toLong().coerceAtLeast(0L)
        if (pointsNeeded <= 0L) return nowMs

        val untilNextPoint = if (remainder == 0L) BA_AP_REGEN_INTERVAL_MS else BA_AP_REGEN_INTERVAL_MS - remainder
        return nowMs + untilNextPoint + (pointsNeeded - 1L) * BA_AP_REGEN_INTERVAL_MS
    }

    fun calculateApNextPointAtMs(nowMs: Long = System.currentTimeMillis()): Long {
        val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        if (limit <= 0) return nowMs
        if (apCurrent >= limit.toDouble()) return nowMs
        val base = apRegenBaseMs.takeIf { it > 0L } ?: nowMs
        val elapsed = (nowMs - base).coerceAtLeast(0L)
        val remainder = elapsed % BA_AP_REGEN_INTERVAL_MS
        val untilNextPoint = if (remainder == 0L) BA_AP_REGEN_INTERVAL_MS else BA_AP_REGEN_INTERVAL_MS - remainder
        return nowMs + untilNextPoint
    }

    fun applyCafeStorage(nowMs: Long = System.currentTimeMillis()) {
        ensureCafeHourBase(nowMs)
        val currentHour = floorToHourMs(nowMs)
        if (currentHour <= cafeLastHourMs) return
        val hoursPassed = ((currentHour - cafeLastHourMs) / BA_CAFE_HOURLY_INTERVAL_MS).toInt()
        if (hoursPassed <= 0) return
        val gained = hoursPassed * cafeHourlyGain(cafeLevel)
        val cap = cafeStorageCap(cafeLevel)
        cafeStoredAp = normalizeAp((cafeStoredAp + gained).coerceAtMost(cap))
        BASettingsStore.saveCafeStoredAp(cafeStoredAp)
        cafeLastHourMs = currentHour
        BASettingsStore.saveCafeLastHourMs(currentHour)
    }

    fun claimCafeStoredAp() {
        applyCafeStorage()
        val claim = normalizeAp(cafeStoredAp)
        if (claim <= 0.0) {
            Toast.makeText(context, "咖啡厅暂无可领取体力", Toast.LENGTH_SHORT).show()
            return
        }
        addCurrentAp(claim, markSync = true)
        cafeStoredAp = 0.0
        BASettingsStore.saveCafeStoredAp(0.0)
        Toast.makeText(context, "已领取 ${claim.roundToInt()} 体力", Toast.LENGTH_SHORT).show()
    }

    fun testCafePlus3Hours() {
        applyCafeStorage()
        val gained = normalizeAp(cafeHourlyGain(cafeLevel) * 3.0)
        val cap = cafeStorageCap(cafeLevel)
        cafeStoredAp = normalizeAp((cafeStoredAp + gained).coerceAtMost(cap))
        BASettingsStore.saveCafeStoredAp(cafeStoredAp)
        Toast.makeText(context, "已增加 +3h 咖啡厅体力（+${gained.roundToInt()}）", Toast.LENGTH_SHORT).show()
    }

    fun copyFriendCodeToClipboard() {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val friendCode = idFriendCode.ifBlank { BA_DEFAULT_FRIEND_CODE }
        clipboard.setPrimaryClip(ClipData.newPlainText("BA Friend Code", friendCode))
        Toast.makeText(context, "好友码已复制", Toast.LENGTH_SHORT).show()
    }

    fun touchHead() {
        val nowMs = System.currentTimeMillis()
        val availableAt = calculateNextHeadpatAvailableMs(coffeeHeadpatMs, serverIndex)
        if (coffeeHeadpatMs > 0L && availableAt > nowMs) return
        coffeeHeadpatMs = nowMs
        BASettingsStore.saveCoffeeHeadpatMs(nowMs)
    }

    fun forceResetHeadpatCooldown() {
        coffeeHeadpatMs = 0L
        BASettingsStore.saveCoffeeHeadpatMs(0L)
    }

    fun useInviteTicket1() {
        val nowMs = System.currentTimeMillis()
        val availableAt = calculateInviteTicketAvailableMs(coffeeInvite1UsedMs)
        if (coffeeInvite1UsedMs > 0L && availableAt > nowMs) return
        coffeeInvite1UsedMs = nowMs
        BASettingsStore.saveCoffeeInvite1UsedMs(nowMs)
    }

    fun forceResetInviteTicket1Cooldown() {
        coffeeInvite1UsedMs = 0L
        BASettingsStore.saveCoffeeInvite1UsedMs(0L)
    }

    fun useInviteTicket2() {
        val nowMs = System.currentTimeMillis()
        val availableAt = calculateInviteTicketAvailableMs(coffeeInvite2UsedMs)
        if (coffeeInvite2UsedMs > 0L && availableAt > nowMs) return
        coffeeInvite2UsedMs = nowMs
        BASettingsStore.saveCoffeeInvite2UsedMs(nowMs)
    }

    fun forceResetInviteTicket2Cooldown() {
        coffeeInvite2UsedMs = 0L
        BASettingsStore.saveCoffeeInvite2UsedMs(0L)
    }

    fun openCalendarLink(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "无法打开活动链接", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshCalendar(force: Boolean = false) {
        if (force) {
            baCalendarReloadSignal += 1
        }
    }

    fun refreshPool(force: Boolean = false) {
        if (force) {
            baPoolReloadSignal += 1
        }
    }

    fun sendApTestNotification(
        showToast: Boolean = true,
        thresholdTriggered: Boolean = false
    ): Boolean {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!notificationsGranted) {
            if (showToast) Toast.makeText(context, "请先授予通知权限", Toast.LENGTH_SHORT).show()
            return false
        }
        val currentDisplay = displayAp(apCurrent)
        val limitDisplay = apLimit.coerceIn(0, BA_AP_MAX)
        val thresholdDisplay = apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        McpNotificationHelper.notifyTest(
            context = context,
            serverName = "BlueArchive AP",
            running = true,
            port = currentDisplay,
            path = thresholdDisplay.toString(),
            clients = limitDisplay
        )
        if (showToast) {
            val notifyText = if (thresholdTriggered) "已发送AP阈值提醒" else "已发送AP通知"
            Toast.makeText(context, notifyText, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    fun tryApThresholdNotification() {
        if (!apNotifyEnabled) {
            apLastNotifiedLevel = -1
            return
        }
        val threshold = apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        val currentDisplay = displayAp(apCurrent)
        if (currentDisplay < threshold) {
            apLastNotifiedLevel = -1
            return
        }
        if (currentDisplay == apLastNotifiedLevel) return
        if (sendApTestNotification(showToast = false, thresholdTriggered = true)) {
            apLastNotifiedLevel = currentDisplay
        }
    }

    fun openSettingsSheet() {
        showOverviewServerPopup = false
        showCafeLevelPopup = false
        sheetCafeLevel = cafeLevel
        sheetApNotifyEnabled = apNotifyEnabled
        sheetApNotifyThresholdText = apNotifyThreshold.toString()
        sheetShowEndedPools = showEndedPools
        sheetShowEndedActivities = showEndedActivities
        sheetShowCalendarPoolImages = showCalendarPoolImages
        showSettingsSheet = true
    }

    fun closeSettingsSheet() {
        showSettingsSheet = false
        showCafeLevelPopup = false
        sheetCafeLevel = cafeLevel
        sheetApNotifyEnabled = apNotifyEnabled
        sheetApNotifyThresholdText = apNotifyThreshold.toString()
        sheetShowEndedPools = showEndedPools
        sheetShowEndedActivities = showEndedActivities
        sheetShowCalendarPoolImages = showCalendarPoolImages
    }

    fun hasAnyImageInCalendarCache(serverIdx: Int): Boolean {
        val (raw, _) = BASettingsStore.loadCalendarCache(serverIdx)
        if (raw.isBlank()) return false
        return runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                if (normalizeGameKeeImageLink(obj.optString("imageUrl")).isNotBlank()) return@runCatching true
            }
            false
        }.getOrDefault(false)
    }

    fun hasAnyImageInPoolCache(serverIdx: Int): Boolean {
        val (raw, _) = BASettingsStore.loadPoolCache(serverIdx)
        if (raw.isBlank()) return false
        return runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                if (normalizeGameKeeImageLink(obj.optString("imageUrl")).isNotBlank()) return@runCatching true
            }
            false
        }.getOrDefault(false)
    }

    fun saveSettings() {
        val savedCafeLevel = sheetCafeLevel.coerceIn(1, 10)
        val savedThreshold = sheetApNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120

        applyCafeStorage()

        BASettingsStore.saveCafeLevel(savedCafeLevel)
        cafeLevel = savedCafeLevel
        clampCafeStoredToCap()

        BASettingsStore.saveApNotifyEnabled(sheetApNotifyEnabled)
        BASettingsStore.saveApNotifyThreshold(savedThreshold)
        apNotifyEnabled = sheetApNotifyEnabled
        apNotifyThreshold = savedThreshold
        BASettingsStore.savePoolShowEnded(sheetShowEndedPools)
        showEndedPools = sheetShowEndedPools

        val wasShowingEndedActivities = showEndedActivities
        BASettingsStore.saveActivityShowEnded(sheetShowEndedActivities)
        showEndedActivities = sheetShowEndedActivities
        val turningEndedActivitiesOn = !wasShowingEndedActivities && sheetShowEndedActivities

        val wasShowingImages = showCalendarPoolImages
        val turningImagesOn = !wasShowingImages && sheetShowCalendarPoolImages
        BASettingsStore.saveShowCalendarPoolImages(sheetShowCalendarPoolImages)
        showCalendarPoolImages = sheetShowCalendarPoolImages

        if (turningEndedActivitiesOn) {
            val (calendarCacheRaw, _) = BASettingsStore.loadCalendarCache(serverIndex)
            if (calendarCacheRaw.isBlank()) refreshCalendar(force = true)
        }

        if (turningImagesOn) {
            val calendarHasImage = hasAnyImageInCalendarCache(serverIndex)
            val poolHasImage = hasAnyImageInPoolCache(serverIndex)
            if (!calendarHasImage) refreshCalendar(force = true)
            if (!poolHasImage) refreshPool(force = true)
        }

        applyApRegen()
        showSettingsSheet = false
        showCafeLevelPopup = false
    }

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }

    DisposableEffect(listState) {
        onDispose {
            BASettingsStore.saveListScrollState(
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset
            )
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > consumedScrollToTopSignal) {
            consumedScrollToTopSignal = scrollToTopSignal
            listState.animateScrollToItem(0)
        } else {
            consumedScrollToTopSignal = scrollToTopSignal
        }
    }

    LaunchedEffect(Unit) {
        ensureRegenBase()
        ensureCafeHourBase()
        clampCafeStoredToCap()
        applyCafeStorage()
        applyApRegen()
        while (true) {
            delay(BA_AP_REGEN_TICK_MS)
            applyCafeStorage()
            applyApRegen()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            uiNowMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(apCurrent) {
        val target = displayAp(apCurrent).toString()
        if (apCurrentInput != target) apCurrentInput = target
    }

    LaunchedEffect(apLimit) {
        val target = apLimit.toString()
        if (apLimitInput != target) apLimitInput = target
    }

    LaunchedEffect(idNickname) {
        if (idNicknameInput != idNickname) idNicknameInput = idNickname
    }

    LaunchedEffect(idFriendCode) {
        if (idFriendCodeInput != idFriendCode) idFriendCodeInput = idFriendCode
    }

    LaunchedEffect(apCurrent, apNotifyEnabled, apNotifyThreshold) {
        tryApThresholdNotification()
    }

    LaunchedEffect(serverIndex, baCalendarReloadSignal, calendarRefreshIntervalHours) {
        val now = System.currentTimeMillis()
        val (cachedRaw, cachedSyncMs) = BASettingsStore.loadCalendarCache(serverIndex)
        val cachedVersion = BASettingsStore.loadCalendarCacheVersion(serverIndex)
        val hasCache = cachedRaw.isNotBlank()
        val cachedEntries = if (hasCache) {
            runCatching { decodeBaCalendarEntries(cachedRaw, now) }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val networkAvailable = isNetworkAvailable(context)
        val intervalMs = calendarRefreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val cacheExpired = !hasCache || cachedSyncMs <= 0L || (now - cachedSyncMs).coerceAtLeast(0L) >= intervalMs
        val cacheSchemaExpired = cachedVersion < BA_CALENDAR_CACHE_SCHEMA_VERSION
        val forceRefresh = baCalendarReloadSignal > 0
        val shouldRequestNetwork = forceRefresh || cacheExpired || cacheSchemaExpired

        if (hasCache) {
            baCalendarEntries = cachedEntries
            baCalendarLastSyncMs = cachedSyncMs
        } else {
            baCalendarEntries = emptyList()
            baCalendarLastSyncMs = 0L
        }

        if (!shouldRequestNetwork) {
            baCalendarLoading = false
            baCalendarError = null
            return@LaunchedEffect
        }

        if (!networkAvailable) {
            baCalendarLoading = false
            baCalendarError = if (hasCache) {
                "当前离线，已显示本地缓存"
            } else {
                "当前离线且无缓存，请联网后刷新"
            }
            return@LaunchedEffect
        }

        baCalendarLoading = true
        baCalendarError = null
        try {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    runWithHardTimeout(BA_SYNC_TIMEOUT_MS) {
                        fetchBaCalendarEntries(serverIndex, now)
                    }
                }
            }
            result
                .onSuccess { entries ->
                    val effective = if (entries.isNotEmpty()) entries else cachedEntries
                    baCalendarEntries = effective
                    baCalendarLastSyncMs = if (entries.isNotEmpty()) now else cachedSyncMs
                    baCalendarError = if (entries.isNotEmpty() || !hasCache) null else "本次返回空数据，已保留本地缓存"
                    if (entries.isNotEmpty()) {
                        BASettingsStore.saveCalendarCache(serverIndex, encodeBaCalendarEntries(entries), now)
                    }
                }
                .onFailure {
                    if (hasCache) {
                        baCalendarEntries = cachedEntries
                        baCalendarLastSyncMs = cachedSyncMs
                        baCalendarError = "同步超时或网络失败，已显示本地缓存"
                    } else {
                        baCalendarError = "活动日历同步失败（超时或网络异常）"
                    }
                }
        } finally {
            baCalendarLoading = false
        }
    }

    LaunchedEffect(serverIndex, baPoolReloadSignal, calendarRefreshIntervalHours) {
        val now = System.currentTimeMillis()
        val (cachedRaw, cachedSyncMs) = BASettingsStore.loadPoolCache(serverIndex)
        val cachedVersion = BASettingsStore.loadPoolCacheVersion(serverIndex)
        val hasCache = cachedRaw.isNotBlank()
        val cachedEntries = if (hasCache) {
            runCatching { decodeBaPoolEntries(cachedRaw, now) }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val networkAvailable = isNetworkAvailable(context)
        val intervalMs = calendarRefreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val cacheExpired = !hasCache || cachedSyncMs <= 0L || (now - cachedSyncMs).coerceAtLeast(0L) >= intervalMs
        val cacheSchemaExpired = cachedVersion < BA_POOL_CACHE_SCHEMA_VERSION
        val forceRefresh = baPoolReloadSignal > 0
        val shouldRequestNetwork = forceRefresh || cacheExpired || cacheSchemaExpired

        if (hasCache) {
            baPoolEntries = cachedEntries
            baPoolLastSyncMs = cachedSyncMs
        } else {
            baPoolEntries = emptyList()
            baPoolLastSyncMs = 0L
        }

        if (!shouldRequestNetwork) {
            baPoolLoading = false
            baPoolError = null
            return@LaunchedEffect
        }

        if (!networkAvailable) {
            baPoolLoading = false
            baPoolError = if (hasCache) {
                "当前离线，已显示本地缓存"
            } else {
                "当前离线且无缓存，请联网后刷新"
            }
            return@LaunchedEffect
        }

        baPoolLoading = true
        baPoolError = null
        try {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    runWithHardTimeout(BA_SYNC_TIMEOUT_MS) {
                        fetchBaPoolEntries(serverIndex, now)
                    }
                }
            }
            result
                .onSuccess { entries ->
                    val effective = if (entries.isNotEmpty()) entries else cachedEntries
                    baPoolEntries = effective
                    baPoolLastSyncMs = if (entries.isNotEmpty()) now else cachedSyncMs
                    baPoolError = if (entries.isNotEmpty() || !hasCache) null else "本次返回空数据，已保留本地缓存"
                    if (entries.isNotEmpty()) {
                        BASettingsStore.savePoolCache(serverIndex, encodeBaPoolEntries(entries), now)
                    }
                }
                .onFailure {
                    if (hasCache) {
                        baPoolEntries = cachedEntries
                        baPoolLastSyncMs = cachedSyncMs
                        baPoolError = "同步超时或网络失败，已显示本地缓存"
                    } else {
                        baPoolError = "卡池同步失败（超时或网络异常）"
                    }
                }
        } finally {
            baPoolLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = "BlueArchive",
                    scrollBehavior = scrollBehavior,
                    color = MiuixTheme.colorScheme.surface,
                    actions = {
                        Box {
                            LiquidActionBar(
                                backdrop = backdrop,
                                items = listOf(
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Refresh,
                                        contentDescription = "刷新",
                                        onClick = {
                                            applyCafeStorage()
                                            applyApRegen()
                                            refreshCalendar(force = true)
                                            refreshPool(force = true)
                                        }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Timer,
                                        contentDescription = "刷新间隔",
                                        onClick = { showCalendarIntervalPopup = !showCalendarIntervalPopup }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Edit,
                                        contentDescription = "编辑",
                                        onClick = { openSettingsSheet() }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Copy,
                                        contentDescription = "复制好友码",
                                        onClick = { copyFriendCodeToClipboard() }
                                    )
                                ),
                                onInteractionChanged = onActionBarInteractingChanged
                            )

                            Row(
                                modifier = Modifier
                                    .width(156.dp)
                                    .height(50.dp)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(37.dp)
                                        .height(42.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(37.dp)
                                        .height(42.dp)
                                ) {
                                    if (showCalendarIntervalPopup) {
                                        WindowListPopup(
                                            show = showCalendarIntervalPopup,
                                            alignment = PopupPositionProvider.Align.BottomStart,
                                            onDismissRequest = { showCalendarIntervalPopup = false },
                                            enableWindowDim = false
                                        ) {
                                            ListPopupColumn {
                                                val options = BaCalendarRefreshIntervalOption.entries
                                                val selected = BaCalendarRefreshIntervalOption.fromHours(
                                                    calendarRefreshIntervalHours
                                                )
                                                options.forEachIndexed { index, option ->
                                                    DropdownImpl(
                                                        text = option.label,
                                                        optionSize = options.size,
                                                        isSelected = selected == option,
                                                        index = index,
                                                        onSelectedIndexChange = { selectedIndex ->
                                                            val picked = options[selectedIndex]
                                                            calendarRefreshIntervalHours = picked.hours
                                                            BASettingsStore.saveCalendarRefreshIntervalHours(
                                                                picked.hours
                                                            )
                                                            showCalendarIntervalPopup = false
                                                            val elapsed = (
                                                                System.currentTimeMillis() - baCalendarLastSyncMs
                                                            ).coerceAtLeast(0L)
                                                            if (baCalendarLastSyncMs <= 0L ||
                                                                elapsed >= picked.hours * 60L * 60L * 1000L
                                                            ) {
                                                                refreshCalendar(force = true)
                                                                refreshPool(force = true)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 16.dp,
                start = 12.dp,
                end = 12.dp
            )
        ) {
            item { SmallTitle(officeSmallTitle, insideMargin = baSmallTitleMargin) }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                val isWorkActivated = idFriendCode != BA_DEFAULT_FRIEND_CODE
                val apNextPointAt = calculateApNextPointAtMs(uiNowMs)
                val apFullAt = calculateApFullAtMs(uiNowMs)
                val apNextPointRemain = formatBaRemainingTime(apNextPointAt, uiNowMs)
                val apSyncTimeText = if (apSyncMs > 0L) formatBaDateTime(apSyncMs) else "未同步"
                val apFullText = formatBaRemainingTime(apFullAt, uiNowMs)
                val apFullTimeText = formatBaDateTime(apFullAt)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (initState == BAInitState.Empty) {
                                    initState = BAInitState.Draft
                                }
                            },
                            onLongClick = { initState = BAInitState.Empty }
                        ),
                    colors = CardDefaults.defaultColors(
                        color = if (isWorkActivated) Color(0x333B82F6) else Color(0x33F59E0B),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    showIndication = true,
                    onClick = {
                        if (initState == BAInitState.Empty) {
                            initState = BAInitState.Draft
                        }
                    }
                ) {
                    val accentBlue = Color(0xFF3B82F6)
                    val accentGreen = Color(0xFF22C55E)
                    val countdownBlue = Color(0xFF60A5FA)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("服务器", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Box {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = serverOptions[serverIndex],
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    onClick = { showOverviewServerPopup = !showOverviewServerPopup }
                                )
                                if (showOverviewServerPopup) {
                                    WindowListPopup(
                                        show = showOverviewServerPopup,
                                        alignment = PopupPositionProvider.Align.BottomEnd,
                                        onDismissRequest = { showOverviewServerPopup = false },
                                        enableWindowDim = false
                                    ) {
                                        ListPopupColumn {
                                            serverOptions.forEachIndexed { index, server ->
                                                DropdownImpl(
                                                    text = server,
                                                    optionSize = serverOptions.size,
                                                    isSelected = serverIndex == index,
                                                    index = index,
                                                    onSelectedIndexChange = { selected ->
                                                        serverIndex = selected
                                                        BASettingsStore.saveServerIndex(selected)
                                                        refreshCalendar(force = true)
                                                        refreshPool(force = true)
                                                        showOverviewServerPopup = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("AP", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassSearchField(
                                    modifier = Modifier.width(72.dp),
                                    value = apCurrentInput,
                                    onValueChange = { input ->
                                        apCurrentInput = input.filter { it.isDigit() }.take(3)
                                    },
                                    onImeActionDone = {
                                        val finalValue = apCurrentInput.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 0
                                        updateCurrentAp(finalValue, markSync = true)
                                        apCurrentInput = finalValue.toString()
                                    },
                                    label = "0",
                                    backdrop = backdrop,
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    singleLine = true,
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp,
                                    textColor = accentGreen
                                )
                                Text("/", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                GlassSearchField(
                                    modifier = Modifier.width(72.dp),
                                    value = apLimitInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(3)
                                        if (digits.isBlank()) {
                                            apLimitInput = ""
                                        } else {
                                            val normalized = digits.toIntOrNull()?.coerceIn(0, BA_AP_LIMIT_MAX)
                                            if (normalized != null) {
                                                apLimitInput = normalized.toString()
                                            }
                                        }
                                    },
                                    onImeActionDone = {
                                        val finalValue = apLimitInput.toIntOrNull()?.coerceIn(0, BA_AP_LIMIT_MAX) ?: BA_AP_LIMIT_MAX
                                        updateApLimit(finalValue)
                                        applyApRegen()
                                        apLimitInput = finalValue.toString()
                                    },
                                    label = "240",
                                    backdrop = backdrop,
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    singleLine = true,
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp,
                                    textColor = accentGreen
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("咖啡厅AP", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = "领取",
                                    textColor = Color(0xFF22C55E),
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    onClick = { claimCafeStoredAp() }
                                )
                                GlassTextButton(
                                    backdrop = backdrop,
                                    text = "${displayAp(cafeStoredAp)}/${cafeDailyCapacity(cafeLevel)}",
                                    textColor = accentGreen,
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    onClick = {}
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("AP Sync", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = apNextPointRemain,
                                    color = countdownBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = apSyncTimeText,
                                    color = accentBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("AP Full", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = apFullText,
                                    color = countdownBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = apFullTimeText,
                                    color = accentBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SmallTitle("咖啡厅", insideMargin = baSmallTitleMargin) }
            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                val accentPink = Color(0xFFF472B6)
                val accentYellow = Color(0xFFF59E0B)
                val countdownBlue = Color(0xFF60A5FA)
                val nowMs = uiNowMs
                val nextHeadpatAt = calculateNextHeadpatAvailableMs(coffeeHeadpatMs, serverIndex)
                val nextStudentRefreshAt = nextCafeStudentRefreshMs(nowMs, serverIndex)
                val nextArenaRefreshAt = nextArenaRefreshMs(nowMs, serverIndex)
                val nextHeadpatText = if (coffeeHeadpatMs <= 0L || nextHeadpatAt <= nowMs) {
                    "0s"
                } else {
                    formatBaRemainingTime(nextHeadpatAt, nowMs)
                }
                val nextStudentRefreshText = formatBaRemainingTime(nextStudentRefreshAt, nowMs)
                val nextArenaRefreshText = formatBaRemainingTime(nextArenaRefreshAt, nowMs)
                val invite1AvailableAt = calculateInviteTicketAvailableMs(coffeeInvite1UsedMs)
                val invite2AvailableAt = calculateInviteTicketAvailableMs(coffeeInvite2UsedMs)
                val invite1Ready = coffeeInvite1UsedMs <= 0L || invite1AvailableAt <= nowMs
                val invite2Ready = coffeeInvite2UsedMs <= 0L || invite2AvailableAt <= nowMs
                val invite1Color = if (invite1Ready) accentPink else accentYellow
                val invite2Color = if (invite2Ready) accentPink else accentYellow
                val invite1Text = if (invite1Ready) "0s" else formatBaRemainingTime(invite1AvailableAt, nowMs)
                val invite2Text = if (invite2Ready) "0s" else formatBaRemainingTime(invite2AvailableAt, nowMs)
                val invite1TimeText = formatBaDateTimeNoSeconds(if (invite1Ready) nowMs else invite1AvailableAt)
                val invite2TimeText = formatBaDateTimeNoSeconds(if (invite2Ready) nowMs else invite2AvailableAt)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = if (suspendCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
                    showIndication = !suspendCardFeedback,
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("竞技场", color = accentPink)
                            Text(
                                text = nextArenaRefreshText,
                                color = countdownBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("学生访问", color = accentPink)
                            Text(
                                text = nextStudentRefreshText,
                                color = countdownBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "摸摸头",
                                textColor = accentPink,
                                enabled = coffeeHeadpatMs <= 0L || nextHeadpatAt <= nowMs,
                                blurRadius = baGlassBlur,
                                lightMaterial = baLightGlass,
                                bottomBarStyle = baBottomBarGlass,
                                onClick = { touchHead() },
                                onLongClick = { forceResetHeadpatCooldown() },
                                onPressedChange = onGlassButtonPressedChange
                            )
                            Text(
                                text = nextHeadpatText,
                                color = countdownBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (coffeeHeadpatMs > 0L) formatBaDateTimeNoSeconds(coffeeHeadpatMs) else "-",
                                color = accentPink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "邀请券1",
                                textColor = invite1Color,
                                enabled = invite1Ready,
                                blurRadius = baGlassBlur,
                                lightMaterial = baLightGlass,
                                bottomBarStyle = baBottomBarGlass,
                                onClick = { useInviteTicket1() },
                                onLongClick = { forceResetInviteTicket1Cooldown() },
                                onPressedChange = onGlassButtonPressedChange
                            )
                            Text(
                                text = invite1Text,
                                color = countdownBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = invite1TimeText,
                                color = invite1Color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "邀请券2",
                                textColor = invite2Color,
                                enabled = invite2Ready,
                                blurRadius = baGlassBlur,
                                lightMaterial = baLightGlass,
                                bottomBarStyle = baBottomBarGlass,
                                onClick = { useInviteTicket2() },
                                onLongClick = { forceResetInviteTicket2Cooldown() },
                                onPressedChange = onGlassButtonPressedChange
                            )
                            Text(
                                text = invite2Text,
                                color = countdownBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = invite2TimeText,
                                color = invite2Color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SmallTitle("活动日历", insideMargin = baSmallTitleMargin) }
            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                val accentBlue = Color(0xFF3B82F6)
                val accentGreen = Color(0xFF22C55E)
                val countdownBlue = Color(0xFF60A5FA)
                val serverTimeZone = serverRefreshTimeZone(serverIndex)
                val visibleCalendarEntries = if (showEndedActivities) {
                    baCalendarEntries
                } else {
                    baCalendarEntries.filter { it.endAtMs > uiNowMs }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = if (suspendCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
                    showIndication = !suspendCardFeedback,
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GameKee · ${serverOptions[serverIndex]}", color = MiuixTheme.colorScheme.onBackground)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (baCalendarLoading) "同步中..." else formatBaDateTimeNoYearInTimeZone(
                                        baCalendarLastSyncMs,
                                        serverTimeZone
                                    ),
                                    color = countdownBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                GlassIconButton(
                                    backdrop = backdrop,
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新活动日历",
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    onClick = { refreshCalendar(force = true) }
                                )
                            }
                        }

                        if (!baCalendarError.isNullOrBlank()) {
                            Text(
                                text = baCalendarError.orEmpty(),
                                color = Color(0xFFF59E0B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (!baCalendarLoading && visibleCalendarEntries.isEmpty()) {
                            Text(
                                text = if (showEndedActivities) "暂无活动" else "暂无进行中或即将开始的活动",
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        } else {
                            visibleCalendarEntries.forEachIndexed { index, activity ->
                                val isEnded = activity.endAtMs <= uiNowMs
                                val remainTarget = if (activity.isRunning || isEnded) activity.endAtMs else activity.beginAtMs
                                val remainText = if (isEnded) "已结束" else formatBaRemainingTime(remainTarget, uiNowMs)
                                val statusText = when {
                                    activity.isRunning -> "进行中"
                                    isEnded -> "已结束"
                                    else -> "即将开始"
                                }
                                val statusColor = when {
                                    activity.isRunning -> accentGreen
                                    isEnded -> MiuixTheme.colorScheme.onBackgroundVariant
                                    else -> accentBlue
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { openCalendarLink(activity.linkUrl) },
                                            onLongClick = { openCalendarLink(activity.linkUrl) }
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${activity.kindName} · ${activity.title}",
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (showCalendarPoolImages) {
                                        GameKeeCoverImage(
                                            imageUrl = activity.imageUrl,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        )
                                    }
                                    Text(
                                        text = "${formatBaDateTimeNoYearInTimeZone(activity.beginAtMs, serverTimeZone)} - ${
                                            formatBaDateTimeNoYearInTimeZone(
                                                activity.endAtMs,
                                                serverTimeZone
                                            )
                                        }",
                                        color = countdownBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    LinearProgressIndicator(
                                        progress = activityProgress(activity, uiNowMs),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        height = 5.dp,
                                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                            foregroundColor = if (activity.isRunning) accentGreen else accentBlue,
                                            backgroundColor = MiuixTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(statusText, color = statusColor)
                                        Text(
                                            text = remainText,
                                            color = countdownBlue,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (index < visibleCalendarEntries.lastIndex) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SmallTitle("卡池信息", insideMargin = baSmallTitleMargin) }
            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                val accentBlue = Color(0xFF3B82F6)
                val accentGreen = Color(0xFF22C55E)
                val countdownBlue = Color(0xFF60A5FA)
                val serverTimeZone = serverRefreshTimeZone(serverIndex)
                val visiblePoolEntries = if (showEndedPools) {
                    baPoolEntries
                } else {
                    baPoolEntries.filter { it.endAtMs > uiNowMs }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = if (suspendCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
                    showIndication = !suspendCardFeedback,
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GameKee · ${serverOptions[serverIndex]}", color = MiuixTheme.colorScheme.onBackground)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (baPoolLoading) "同步中..." else formatBaDateTimeNoYearInTimeZone(
                                        baPoolLastSyncMs,
                                        serverTimeZone
                                    ),
                                    color = countdownBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                GlassIconButton(
                                    backdrop = backdrop,
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新卡池",
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    onClick = { refreshPool(force = true) }
                                )
                            }
                        }

                        if (!baPoolError.isNullOrBlank()) {
                            Text(
                                text = baPoolError.orEmpty(),
                                color = Color(0xFFF59E0B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (!baPoolLoading && visiblePoolEntries.isEmpty()) {
                            Text(
                                text = "暂无进行中或即将开始的卡池",
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        } else {
                            visiblePoolEntries.forEachIndexed { index, pool ->
                                val isEnded = pool.endAtMs <= uiNowMs
                                val remainTarget = if (pool.isRunning || isEnded) pool.endAtMs else pool.startAtMs
                                val remainText = if (isEnded) "已结束" else formatBaRemainingTime(remainTarget, uiNowMs)
                                val statusText = when {
                                    pool.isRunning -> "进行中"
                                    isEnded -> "已结束"
                                    else -> "即将开始"
                                }
                                val statusColor = when {
                                    pool.isRunning -> accentGreen
                                    isEnded -> MiuixTheme.colorScheme.onBackgroundVariant
                                    else -> accentBlue
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                BASettingsStore.saveListScrollState(
                                                    index = listState.firstVisibleItemIndex,
                                                    offset = listState.firstVisibleItemScrollOffset
                                                )
                                                onOpenPoolStudentGuide(pool.linkUrl)
                                            },
                                            onLongClick = { openCalendarLink(pool.linkUrl) }
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${pool.tagName} · ${pool.name}",
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (showCalendarPoolImages) {
                                        GameKeeCoverImage(
                                            imageUrl = pool.imageUrl,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        )
                                    }
                                    Text(
                                        text = "${formatBaDateTimeNoYearInTimeZone(pool.startAtMs, serverTimeZone)} - ${formatBaDateTimeNoYearInTimeZone(pool.endAtMs, serverTimeZone)}",
                                        color = countdownBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    LinearProgressIndicator(
                                        progress = poolProgress(pool, uiNowMs),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        height = 5.dp,
                                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                            foregroundColor = if (pool.isRunning) accentGreen else accentBlue,
                                            backgroundColor = MiuixTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(statusText, color = statusColor)
                                        Text(
                                            text = remainText,
                                            color = countdownBlue,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (index < visiblePoolEntries.lastIndex) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SmallTitle("ID卡", insideMargin = baSmallTitleMargin) }
            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                val nicknameLengthForWidth = idNicknameInput.ifEmpty { BA_DEFAULT_NICKNAME }.length.coerceIn(1, 10)
                val nicknameFieldWidth = (nicknameLengthForWidth * 11 + 34).coerceIn(72, 124).dp
                val friendCodeLengthForWidth = idFriendCodeInput.ifEmpty { BA_DEFAULT_FRIEND_CODE }.length.coerceIn(1, 8)
                val friendCodeFieldWidth = (friendCodeLengthForWidth * 11 + 34).coerceIn(92, 128).dp
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = if (suspendCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
                    showIndication = !suspendCardFeedback,
                    onClick = {}
                ) {
                    val accentBlue = Color(0xFF3B82F6)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("昵称", color = MiuixTheme.colorScheme.onBackground)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassSearchField(
                                    modifier = Modifier.width(nicknameFieldWidth),
                                    value = idNicknameInput,
                                    onValueChange = { input ->
                                        idNicknameInput = input.take(10)
                                    },
                                    onImeActionDone = { saveIdNicknameFromInput() },
                                    label = "Kei",
                                    backdrop = backdrop,
                                    blurRadius = baGlassBlur,
                                    lightMaterial = baLightGlass,
                                    bottomBarStyle = baBottomBarGlass,
                                    singleLine = true,
                                    textAlign = TextAlign.Center
                                )
                                Text("老师", color = accentBlue)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("好友码", color = MiuixTheme.colorScheme.onBackground)
                            }
                            GlassSearchField(
                                modifier = Modifier.width(friendCodeFieldWidth),
                                value = idFriendCodeInput,
                                onValueChange = { input ->
                                    idFriendCodeInput = input
                                        .uppercase(Locale.ROOT)
                                        .filter { it in 'A'..'Z' }
                                        .take(8)
                                },
                                onImeActionDone = { saveIdFriendCodeFromInput() },
                                label = "ARISUKEI",
                                backdrop = backdrop,
                                blurRadius = baGlassBlur,
                                lightMaterial = baLightGlass,
                                bottomBarStyle = baBottomBarGlass,
                                singleLine = true,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }
            item { SmallTitle("Test", insideMargin = baSmallTitleMargin) }
            item { Spacer(modifier = Modifier.height(6.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0x223B82F6),
                        contentColor = MiuixTheme.colorScheme.onBackground
                    ),
                    pressFeedbackType = if (suspendCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
                    showIndication = !suspendCardFeedback,
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "AP通知",
                                blurRadius = baGlassBlur,
                                lightMaterial = baLightGlass,
                                bottomBarStyle = baBottomBarGlass,
                                onClick = { sendApTestNotification(showToast = true) },
                                onPressedChange = onGlassButtonPressedChange
                            )
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "咖啡厅3h AP",
                                blurRadius = baGlassBlur,
                                lightMaterial = baLightGlass,
                                bottomBarStyle = baBottomBarGlass,
                                onClick = { testCafePlus3Hours() },
                                onPressedChange = onGlassButtonPressedChange
                            )
                        }
                    }
                }
            }
        }
    }

    WindowBottomSheet(
        show = showSettingsSheet,
        title = "BA 配置",
        onDismissRequest = { closeSettingsSheet() },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                blurRadius = baGlassBlur,
                lightMaterial = baLightGlass,
                bottomBarStyle = baBottomBarGlass,
                onClick = { closeSettingsSheet() }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "保存",
                blurRadius = baGlassBlur,
                lightMaterial = baLightGlass,
                bottomBarStyle = baBottomBarGlass,
                onClick = { saveSettings() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "咖啡厅等级",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                }
                Box {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "${sheetCafeLevel}级",
                        blurRadius = baGlassBlur,
                        lightMaterial = baLightGlass,
                        bottomBarStyle = baBottomBarGlass,
                        onClick = { showCafeLevelPopup = !showCafeLevelPopup }
                    )
                    if (showCafeLevelPopup) {
                        WindowListPopup(
                            show = showCafeLevelPopup,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            onDismissRequest = { showCafeLevelPopup = false },
                            enableWindowDim = false
                        ) {
                            ListPopupColumn {
                                cafeLevelOptions.forEachIndexed { index, level ->
                                    DropdownImpl(
                                        text = "${level}级",
                                        optionSize = cafeLevelOptions.size,
                                        isSelected = sheetCafeLevel == level,
                                        index = index,
                                        onSelectedIndexChange = { selected ->
                                            sheetCafeLevel = cafeLevelOptions[selected]
                                            showCafeLevelPopup = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "AP通知",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                }
                Switch(
                    checked = sheetApNotifyEnabled,
                    onCheckedChange = { checked -> sheetApNotifyEnabled = checked }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "显示已结束活动",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                }
                Switch(
                    checked = sheetShowEndedActivities,
                    onCheckedChange = { checked -> sheetShowEndedActivities = checked }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "显示已结束卡池",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                }
                Switch(
                    checked = sheetShowEndedPools,
                    onCheckedChange = { checked -> sheetShowEndedPools = checked }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "显示活动/卡池图片",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                }
                Switch(
                    checked = sheetShowCalendarPoolImages,
                    onCheckedChange = { checked -> sheetShowCalendarPoolImages = checked }
                )
            }

            if (sheetApNotifyEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "提醒阈值",
                        color = MiuixTheme.colorScheme.onBackground
                    )
                    GlassSearchField(
                        modifier = Modifier.width(70.dp),
                        value = sheetApNotifyThresholdText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(3)
                            if (digits.isBlank()) {
                                sheetApNotifyThresholdText = ""
                            } else {
                                val normalized = digits.toIntOrNull()?.coerceIn(0, BA_AP_MAX)
                                sheetApNotifyThresholdText = normalized?.toString() ?: ""
                            }
                        },
                        onImeActionDone = {
                            val normalized = sheetApNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120
                            sheetApNotifyThresholdText = normalized.toString()
                        },
                        label = "120",
                        backdrop = backdrop,
                        blurRadius = baGlassBlur,
                        lightMaterial = baLightGlass,
                        bottomBarStyle = baBottomBarGlass,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = Color(0xFF22C55E)
                    )
                }
            }

            Text(
                text = "不同服务器时区不同，建议按实际游玩服务器设置。",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
