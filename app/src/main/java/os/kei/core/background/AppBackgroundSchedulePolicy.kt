package os.kei.core.background

import os.kei.ui.page.main.ba.support.BA_AP_LIMIT_MAX
import os.kei.ui.page.main.ba.support.BA_AP_MAX
import os.kei.ui.page.main.ba.support.BA_AP_REGEN_INTERVAL_MS
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import os.kei.ui.page.main.ba.support.currentArenaRefreshSlotMs
import os.kei.ui.page.main.ba.support.currentCafeStudentRefreshSlotMs
import os.kei.ui.page.main.ba.support.displayAp
import os.kei.ui.page.main.ba.support.nextArenaRefreshMs
import os.kei.ui.page.main.ba.support.nextCafeStudentRefreshMs
import os.kei.ui.page.main.ba.support.normalizeAp
import kotlin.math.ceil

internal object AppBackgroundSchedulePolicy {
    const val MIN_ALARM_DELAY_MS = 15_000L

    private const val GITHUB_FIRST_TICK_DELAY_MS = 2L * 60L * 1000L
    private const val BA_BASELINE_SEED_DELAY_MS = 60_000L
    private const val BA_REFRESH_SETTLE_DELAY_MS = 30_000L
    private const val ANDROID_17_SHORT_WINDOW_MS = 10L * 60L * 1000L
    private const val ANDROID_17_LONG_WINDOW_MS = 30L * 60L * 1000L

    fun nextGitHubRefreshSchedule(
        trackedItemCount: Int,
        lastRefreshMs: Long,
        refreshIntervalHours: Int,
        nowMs: Long,
    ): BackgroundAlarmSchedule? {
        if (trackedItemCount <= 0) return null
        val intervalMs = refreshIntervalHours.coerceIn(1, 12) * 60L * 60L * 1000L
        val dueAtMs = if (lastRefreshMs > 0L) {
            lastRefreshMs + intervalMs
        } else {
            nowMs + GITHUB_FIRST_TICK_DELAY_MS
        }
        val triggerAtMs = dueAtMs.coerceAtLeast(nowMs + MIN_ALARM_DELAY_MS)
        val precision = if (triggerAtMs <= nowMs + 60_000L) {
            BackgroundAlarmPrecision.Prompt
        } else {
            BackgroundAlarmPrecision.Windowed
        }
        return BackgroundAlarmSchedule(
            triggerAtMillis = triggerAtMs,
            workload = BackgroundAlarmWorkload.RoutineSync,
            precision = precision
        )
    }

    fun nextBaReminderSchedule(
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ): BackgroundAlarmSchedule? {
        val candidates = buildList {
            if (snapshot.apNotifyEnabled) {
                nextBaApSchedule(snapshot = snapshot, nowMs = nowMs)?.let(::add)
            }
            if (snapshot.cafeVisitNotifyEnabled) {
                add(nextCafeVisitSchedule(snapshot = snapshot, nowMs = nowMs))
            }
            if (snapshot.arenaRefreshNotifyEnabled) {
                add(nextArenaRefreshSchedule(snapshot = snapshot, nowMs = nowMs))
            }
        }
        return candidates.minByOrNull { it.triggerAtMillis }
    }

    fun android17WindowLength(
        triggerAtMillis: Long,
        nowMs: Long,
    ): Long {
        val delayMs = (triggerAtMillis - nowMs).coerceAtLeast(0L)
        return if (delayMs >= 60L * 60L * 1000L) {
            ANDROID_17_LONG_WINDOW_MS
        } else {
            ANDROID_17_SHORT_WINDOW_MS
        }
    }

    private fun nextBaApSchedule(
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ): BackgroundAlarmSchedule? {
        val limit = snapshot.apLimit.coerceIn(0, BA_AP_LIMIT_MAX)
        val threshold = snapshot.apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        if (threshold > limit || limit <= 0) return null

        val projected = projectAp(snapshot = snapshot, nowMs = nowMs, limit = limit)
        val currentDisplay = displayAp(projected.currentAp)
        if (currentDisplay >= threshold) {
            if (currentDisplay != snapshot.apLastNotifiedLevel) {
                return promptUserReminder(nowMs)
            }
            val nextPointAtMs = nextApPointAtMs(
                currentAp = projected.currentAp,
                apRegenBaseMs = projected.apRegenBaseMs,
                limit = limit,
                nowMs = nowMs
            ) ?: return null
            return userReminderWindow(nextPointAtMs)
        }

        val pointsNeeded = ceil(threshold.toDouble() - projected.currentAp).toLong()
            .coerceAtLeast(1L)
        val nextPointAtMs = nextApPointAtMs(
            currentAp = projected.currentAp,
            apRegenBaseMs = projected.apRegenBaseMs,
            limit = limit,
            nowMs = nowMs
        ) ?: return null
        val thresholdAtMs = nextPointAtMs + (pointsNeeded - 1L) * BA_AP_REGEN_INTERVAL_MS
        return userReminderWindow(thresholdAtMs)
    }

    private fun nextCafeVisitSchedule(
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ): BackgroundAlarmSchedule {
        if (snapshot.cafeVisitLastNotifiedSlotMs <= 0L) {
            return promptUserReminder(nowMs + BA_BASELINE_SEED_DELAY_MS)
        }
        val currentSlotMs = currentCafeStudentRefreshSlotMs(
            nowMs = nowMs,
            serverIndex = snapshot.serverIndex
        )
        if (currentSlotMs > snapshot.cafeVisitLastNotifiedSlotMs) {
            return promptUserReminder(nowMs)
        }
        return userReminderWindow(
            nextCafeStudentRefreshMs(
                fromMs = nowMs,
                serverIndex = snapshot.serverIndex
            ) + BA_REFRESH_SETTLE_DELAY_MS
        )
    }

    private fun nextArenaRefreshSchedule(
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ): BackgroundAlarmSchedule {
        if (snapshot.arenaRefreshLastNotifiedSlotMs <= 0L) {
            return promptUserReminder(nowMs + BA_BASELINE_SEED_DELAY_MS)
        }
        val currentSlotMs = currentArenaRefreshSlotMs(
            nowMs = nowMs,
            serverIndex = snapshot.serverIndex
        )
        if (currentSlotMs > snapshot.arenaRefreshLastNotifiedSlotMs) {
            return promptUserReminder(nowMs)
        }
        return userReminderWindow(
            nextArenaRefreshMs(
                fromMs = nowMs,
                serverIndex = snapshot.serverIndex
            ) + BA_REFRESH_SETTLE_DELAY_MS
        )
    }

    private fun promptUserReminder(triggerAtMs: Long): BackgroundAlarmSchedule {
        return BackgroundAlarmSchedule(
            triggerAtMillis = triggerAtMs,
            workload = BackgroundAlarmWorkload.UserReminder,
            precision = BackgroundAlarmPrecision.Prompt
        )
    }

    private fun userReminderWindow(triggerAtMs: Long): BackgroundAlarmSchedule {
        return BackgroundAlarmSchedule(
            triggerAtMillis = triggerAtMs,
            workload = BackgroundAlarmWorkload.UserReminder,
            precision = BackgroundAlarmPrecision.Windowed
        )
    }

    private fun projectAp(
        snapshot: BaPageSnapshot,
        nowMs: Long,
        limit: Int,
    ): ProjectedBaAp {
        val current = normalizeAp(snapshot.apCurrent.coerceIn(0.0, BA_AP_MAX.toDouble()))
        val baseMs = snapshot.apRegenBaseMs.takeIf { it > 0L } ?: nowMs
        if (current >= limit.toDouble()) {
            return ProjectedBaAp(currentAp = current, apRegenBaseMs = nowMs)
        }
        val elapsedMs = (nowMs - baseMs).coerceAtLeast(0L)
        val gained = elapsedMs / BA_AP_REGEN_INTERVAL_MS
        val pointsUntilLimit = ceil(limit.toDouble() - current).toLong().coerceAtLeast(0L)
        val applied = gained.coerceAtMost(pointsUntilLimit)
        if (applied <= 0L) {
            return ProjectedBaAp(currentAp = current, apRegenBaseMs = baseMs)
        }
        val nextAp = normalizeAp((current + applied.toDouble()).coerceAtMost(limit.toDouble()))
        val nextBaseMs = if (nextAp >= limit.toDouble()) {
            nowMs
        } else {
            baseMs + applied * BA_AP_REGEN_INTERVAL_MS
        }
        return ProjectedBaAp(currentAp = nextAp, apRegenBaseMs = nextBaseMs)
    }

    private fun nextApPointAtMs(
        currentAp: Double,
        apRegenBaseMs: Long,
        limit: Int,
        nowMs: Long,
    ): Long? {
        if (currentAp >= limit.toDouble()) return null
        val baseMs = apRegenBaseMs.takeIf { it > 0L } ?: nowMs
        val elapsedMs = (nowMs - baseMs).coerceAtLeast(0L)
        val remainderMs = elapsedMs % BA_AP_REGEN_INTERVAL_MS
        val untilNextPointMs = if (remainderMs == 0L) {
            BA_AP_REGEN_INTERVAL_MS
        } else {
            BA_AP_REGEN_INTERVAL_MS - remainderMs
        }
        return nowMs + untilNextPointMs
    }

    private data class ProjectedBaAp(
        val currentAp: Double,
        val apRegenBaseMs: Long,
    )
}

internal data class BackgroundAlarmSchedule(
    val triggerAtMillis: Long,
    val workload: BackgroundAlarmWorkload,
    val precision: BackgroundAlarmPrecision,
)

internal enum class BackgroundAlarmWorkload {
    RoutineSync,
    UserReminder,
}

internal enum class BackgroundAlarmPrecision {
    Prompt,
    Windowed,
}
