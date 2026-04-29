package os.kei.ui.page.main.ba.support

import os.kei.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt

internal const val BA_AP_MAX = 999
internal const val BA_AP_LIMIT_MAX = 240
internal const val BA_AP_REGEN_INTERVAL_MS = 6 * 60 * 1000L
internal const val BA_AP_REGEN_TICK_MS = 30_000L
internal const val BA_CAFE_HOURLY_INTERVAL_MS = 60 * 60 * 1000L
internal const val BA_CAFE_STUDENT_REFRESH_INTERVAL_MS = 12 * 60 * 60 * 1000L
internal const val BA_ARENA_REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L
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

internal fun baServerLabelRes(serverIndex: Int): Int {
    return when (serverIndex.coerceIn(0, 2)) {
        0 -> R.string.ba_server_cn
        1 -> R.string.ba_server_global
        else -> R.string.ba_server_jp
    }
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

internal fun currentCafeStudentRefreshSlotMs(nowMs: Long, serverIndex: Int): Long {
    val nextRefreshAt = nextCafeStudentRefreshMs(nowMs, serverIndex)
    return (nextRefreshAt - BA_CAFE_STUDENT_REFRESH_INTERVAL_MS).coerceAtLeast(0L)
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

internal fun currentArenaRefreshSlotMs(nowMs: Long, serverIndex: Int): Long {
    val nextRefreshAt = nextArenaRefreshMs(nowMs, serverIndex)
    return (nextRefreshAt - BA_ARENA_REFRESH_INTERVAL_MS).coerceAtLeast(0L)
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
