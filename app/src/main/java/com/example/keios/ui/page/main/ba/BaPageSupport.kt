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
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
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

internal enum class BAInitState {
    Empty,
    Draft
}

internal data class BaPageSnapshot(
    val serverIndex: Int = 2,
    val cafeLevel: Int = 10,
    val cafeStoredAp: Double = 0.0,
    val cafeLastHourMs: Long = 0L,
    val idNickname: String = BA_DEFAULT_NICKNAME,
    val idFriendCode: String = BA_DEFAULT_FRIEND_CODE,
    val apLimit: Int = BA_AP_LIMIT_MAX,
    val apCurrent: Double = 0.0,
    val apRegenBaseMs: Long = 0L,
    val apSyncMs: Long = 0L,
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = 120,
    val coffeeHeadpatMs: Long = 0L,
    val coffeeInvite1UsedMs: Long = 0L,
    val coffeeInvite2UsedMs: Long = 0L,
    val showEndedPools: Boolean = false,
    val showEndedActivities: Boolean = false,
    val showCalendarPoolImages: Boolean = true,
    val calendarRefreshIntervalHours: Int = 12
)

internal data class BaCacheSnapshot(
    val raw: String = "",
    val syncMs: Long = 0L,
    val version: Int = 0
)

internal object BASessionState {
    var didResetScrollOnThisProcess: Boolean = false
}

internal enum class BaCalendarRefreshIntervalOption(val hours: Int, val label: String) {
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

internal const val BA_AP_MAX = 999
internal const val BA_AP_LIMIT_MAX = 240
internal const val BA_AP_REGEN_INTERVAL_MS = 6 * 60 * 1000L
internal const val BA_AP_REGEN_TICK_MS = 30_000L
internal const val BA_CAFE_HOURLY_INTERVAL_MS = 60 * 60 * 1000L
internal const val BA_HEADPAT_COOLDOWN_MS = 3 * 60 * 60 * 1000L
internal const val BA_INVITE_COOLDOWN_MS = 20 * 60 * 60 * 1000L
internal const val BA_DEFAULT_NICKNAME = "Kei"
internal const val BA_DEFAULT_FRIEND_CODE = "ARISUKEI"
internal val BA_CAFE_DAILY_AP_BY_LEVEL = intArrayOf(92, 152, 222, 302, 390, 460, 530, 600, 570, 740)

internal fun displayAp(apExact: Double): Int = apExact.coerceAtLeast(0.0).toInt()

internal fun normalizeAp(apExact: Double): Double {
    return (apExact.coerceIn(0.0, BA_AP_MAX.toDouble()) * 1000.0).roundToInt() / 1000.0
}

internal fun fractionalApPart(apExact: Double): Double {
    val normalized = normalizeAp(apExact)
    val integerPart = displayAp(normalized).toDouble()
    return normalizeAp(normalized - integerPart).coerceIn(0.0, 0.999)
}

internal fun cafeHourlyGain(level: Int): Double {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1] / 24.0
}

internal fun cafeDailyCapacity(level: Int): Int {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1]
}

internal fun cafeStorageCap(level: Int): Double {
    val safeLevel = level.coerceIn(1, 10)
    return BA_CAFE_DAILY_AP_BY_LEVEL[safeLevel - 1].toDouble()
}

internal fun calculateApFullAtMs(
    apLimit: Int,
    apCurrent: Double,
    apRegenBaseMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): Long {
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

internal fun calculateApNextPointAtMs(
    apLimit: Int,
    apCurrent: Double,
    apRegenBaseMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): Long {
    val limit = apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
    if (limit <= 0) return nowMs
    if (apCurrent >= limit.toDouble()) return nowMs
    val base = apRegenBaseMs.takeIf { it > 0L } ?: nowMs
    val elapsed = (nowMs - base).coerceAtLeast(0L)
    val remainder = elapsed % BA_AP_REGEN_INTERVAL_MS
    val untilNextPoint = if (remainder == 0L) BA_AP_REGEN_INTERVAL_MS else BA_AP_REGEN_INTERVAL_MS - remainder
    return nowMs + untilNextPoint
}

internal fun floorToHourMs(epochMs: Long): Long = epochMs - (epochMs % BA_CAFE_HOURLY_INTERVAL_MS)

internal fun serverRefreshTimeZone(serverIndex: Int): TimeZone {
    return if (serverIndex == 0) TimeZone.getTimeZone("Asia/Shanghai") else TimeZone.getTimeZone("Asia/Tokyo")
}

internal fun nextCafeStudentRefreshMs(fromMs: Long, serverIndex: Int): Long {
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

internal fun nextArenaRefreshMs(fromMs: Long, serverIndex: Int): Long {
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

internal fun calculateNextHeadpatAvailableMs(lastHeadpatMs: Long, serverIndex: Int): Long {
    if (lastHeadpatMs <= 0L) return 0L
    val cooldownReadyAt = lastHeadpatMs + BA_HEADPAT_COOLDOWN_MS
    val refreshAt = nextCafeStudentRefreshMs(lastHeadpatMs, serverIndex)
    return minOf(cooldownReadyAt, refreshAt)
}

internal fun calculateInviteTicketAvailableMs(lastUsedMs: Long): Long {
    if (lastUsedMs <= 0L) return 0L
    return lastUsedMs + BA_INVITE_COOLDOWN_MS
}

internal fun formatBaDateTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "未同步"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))
    }.getOrDefault("未同步")
}

internal fun formatBaDateTimeNoSeconds(epochMillis: Long): String {
    if (epochMillis <= 0L) return "未同步"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }.getOrDefault("未同步")
}

internal fun formatBaRemainingTime(targetMs: Long, nowMs: Long = System.currentTimeMillis()): String {
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
    if (minutes > 0L) parts += "${minutes}m"
    if (seconds > 0L || parts.isEmpty()) parts += "${seconds}s"
    return parts.joinToString(" ")
}

internal const val BA_CALENDAR_ENDPOINT = "/v1/activity/page-list"
internal const val BA_NETWORK_RETRY_ATTEMPTS = 1
internal const val BA_SYNC_TIMEOUT_MS = 20_000L
internal const val BA_CALENDAR_MAX_ITEMS = 6
internal const val BA_CALENDAR_CACHE_SCHEMA_VERSION = 3
internal const val BA_POOL_ENDPOINT = "/v1/cardPool/query-list"
internal const val BA_POOL_MAX_ITEMS = 6
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
        0 -> 16 // 国服
        1 -> 17 // 国际服
        else -> 15 // 日服
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

@Composable
internal fun GameKeeCoverImage(
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

    val maxItems = BA_CALENDAR_MAX_ITEMS.coerceAtLeast(activeOrUpcoming.size + fallbackMissingKinds.size)
    return sorted.take(maxItems)
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

    val maxItems = BA_POOL_MAX_ITEMS.coerceAtLeast(activeOrUpcoming.size + fallbackMissingTags.size)
    return sorted.take(maxItems)
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

    // Keep pool sync responsive: "all source" already covers all tags.
    // Tag-by-tag补抓在网络异常时会显著拉长同步时间，这里改为不阻塞主同步流程。

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

internal object BASettingsStore {
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

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    private fun kv(): MMKV = store

    fun loadSnapshot(): BaPageSnapshot {
        val store = kv()
        val serverIndex = store.decodeInt(KEY_SERVER_INDEX, DEFAULT_SERVER_INDEX).coerceIn(0, 2)
        val cafeLevel = store.decodeInt(KEY_CAFE_LEVEL, DEFAULT_CAFE_LEVEL).coerceIn(1, 10)
        val cafeStoredAp = normalizeAp(
            store.decodeString(KEY_CAFE_STORED_AP, DEFAULT_CAFE_STORED_AP.toString())?.toDoubleOrNull()
                ?: DEFAULT_CAFE_STORED_AP
        )
        val idNickname = store.decodeString(KEY_ID_NICKNAME, DEFAULT_ID_NICKNAME).orEmpty().take(10)
            .ifEmpty { DEFAULT_ID_NICKNAME }
        val idFriendCode = store.decodeString(KEY_ID_FRIEND_CODE, DEFAULT_ID_FRIEND_CODE)
            .orEmpty()
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
            .let { if (it.length == 8) it else DEFAULT_ID_FRIEND_CODE }
        val apCurrent = if (store.containsKey(KEY_AP_CURRENT_EXACT)) {
            store.decodeString(KEY_AP_CURRENT_EXACT, DEFAULT_AP_CURRENT.toString())?.toDoubleOrNull() ?: DEFAULT_AP_CURRENT
        } else {
            store.decodeInt(KEY_AP_CURRENT, DEFAULT_AP_CURRENT.toInt()).toDouble()
        }
        val refreshHours = BaCalendarRefreshIntervalOption.fromHours(
            store.decodeInt(
                KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
                DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS
            )
        ).hours
        return BaPageSnapshot(
            serverIndex = serverIndex,
            cafeLevel = cafeLevel,
            cafeStoredAp = normalizeAp(cafeStoredAp),
            cafeLastHourMs = store.decodeLong(KEY_CAFE_LAST_HOUR_MS, 0L),
            idNickname = idNickname,
            idFriendCode = idFriendCode,
            apLimit = store.decodeInt(KEY_AP_LIMIT, DEFAULT_AP_LIMIT).coerceIn(0, BA_AP_LIMIT_MAX),
            apCurrent = normalizeAp(apCurrent.coerceIn(0.0, BA_AP_MAX.toDouble())),
            apRegenBaseMs = store.decodeLong(KEY_AP_REGEN_BASE_MS, 0L),
            apSyncMs = store.decodeLong(KEY_AP_SYNC_MS, 0L),
            apNotifyEnabled = store.decodeBool(KEY_AP_NOTIFY_ENABLED, false),
            apNotifyThreshold = store.decodeInt(KEY_AP_NOTIFY_THRESHOLD, DEFAULT_AP_NOTIFY_THRESHOLD).coerceIn(0, BA_AP_MAX),
            coffeeHeadpatMs = store.decodeLong(KEY_COFFEE_HEADPAT_MS, 0L),
            coffeeInvite1UsedMs = store.decodeLong(KEY_COFFEE_INVITE1_USED_MS, 0L),
            coffeeInvite2UsedMs = store.decodeLong(KEY_COFFEE_INVITE2_USED_MS, 0L),
            showEndedPools = store.decodeBool(KEY_POOL_SHOW_ENDED, false),
            showEndedActivities = store.decodeBool(KEY_ACTIVITY_SHOW_ENDED, false),
            showCalendarPoolImages = store.decodeBool(KEY_SHOW_CALENDAR_POOL_IMAGES, true),
            calendarRefreshIntervalHours = refreshHours
        )
    }

    fun loadCalendarCacheSnapshot(serverIndex: Int): BaCacheSnapshot {
        val store = kv()
        return BaCacheSnapshot(
            raw = store.decodeString(calendarCacheKey(serverIndex), "").orEmpty(),
            syncMs = store.decodeLong(calendarSyncKey(serverIndex), 0L),
            version = store.decodeInt(calendarCacheVersionKey(serverIndex), 0)
        )
    }

    fun loadPoolCacheSnapshot(serverIndex: Int): BaCacheSnapshot {
        val store = kv()
        return BaCacheSnapshot(
            raw = store.decodeString(poolCacheKey(serverIndex), "").orEmpty(),
            syncMs = store.decodeLong(poolSyncKey(serverIndex), 0L),
            version = store.decodeInt(poolCacheVersionKey(serverIndex), 0)
        )
    }

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

    fun clearCalendarAndPoolCaches() {
        val store = kv()
        for (serverIndex in 0..2) {
            store.removeValueForKey(calendarCacheKey(serverIndex))
            store.removeValueForKey(calendarSyncKey(serverIndex))
            store.removeValueForKey(calendarCacheVersionKey(serverIndex))
            store.removeValueForKey(poolCacheKey(serverIndex))
            store.removeValueForKey(poolSyncKey(serverIndex))
            store.removeValueForKey(poolCacheVersionKey(serverIndex))
        }
        store.trim()
    }

    fun storageFootprintBytes(): Long = kv().totalSize()

    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long {
        var total = 0L
        for (serverIndex in 0..2) {
            val calendar = loadCalendarCacheSnapshot(serverIndex)
            val pool = loadPoolCacheSnapshot(serverIndex)
            total += calendar.raw.length.toLong() * 2 + 16L
            total += pool.raw.length.toLong() * 2 + 16L
        }
        return total
    }

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        return listOf(snapshot.idNickname, snapshot.idFriendCode).sumOf { it.length.toLong() * 2 } + 160L
    }

    fun clearListScrollState() {
        val store = kv()
        store.removeValueForKey(KEY_LIST_SCROLL_INDEX)
        store.removeValueForKey(KEY_LIST_SCROLL_OFFSET)
    }
}
