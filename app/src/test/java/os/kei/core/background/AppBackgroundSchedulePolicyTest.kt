package os.kei.core.background

import os.kei.ui.page.main.ba.support.BA_AP_REGEN_INTERVAL_MS
import os.kei.ui.page.main.ba.support.BA_CAFE_STUDENT_REFRESH_INTERVAL_MS
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import os.kei.ui.page.main.ba.support.currentCafeStudentRefreshSlotMs
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppBackgroundSchedulePolicyTest {
    @Test
    fun `github schedule cancels when no tracked item exists`() {
        val schedule = AppBackgroundSchedulePolicy.nextGitHubRefreshSchedule(
            trackedItemCount = 0,
            lastRefreshMs = 0L,
            refreshIntervalHours = 3,
            nowMs = NOW_MS
        )

        assertNull(schedule)
    }

    @Test
    fun `github first refresh uses short startup delay`() {
        val schedule = AppBackgroundSchedulePolicy.nextGitHubRefreshSchedule(
            trackedItemCount = 2,
            lastRefreshMs = 0L,
            refreshIntervalHours = 3,
            nowMs = NOW_MS
        )

        assertNotNull(schedule)
        assertEquals(NOW_MS + 2L * 60L * 1000L, schedule.triggerAtMillis)
        assertEquals(BackgroundAlarmWorkload.RoutineSync, schedule.workload)
        assertEquals(BackgroundAlarmPrecision.Windowed, schedule.precision)
    }

    @Test
    fun `github overdue refresh becomes prompt routine work`() {
        val schedule = AppBackgroundSchedulePolicy.nextGitHubRefreshSchedule(
            trackedItemCount = 1,
            lastRefreshMs = NOW_MS - 2L * 60L * 60L * 1000L,
            refreshIntervalHours = 1,
            nowMs = NOW_MS
        )

        assertNotNull(schedule)
        assertEquals(NOW_MS + AppBackgroundSchedulePolicy.MIN_ALARM_DELAY_MS, schedule.triggerAtMillis)
        assertEquals(BackgroundAlarmWorkload.RoutineSync, schedule.workload)
        assertEquals(BackgroundAlarmPrecision.Prompt, schedule.precision)
    }

    @Test
    fun `ba ap threshold schedules exact threshold crossing event`() {
        val schedule = AppBackgroundSchedulePolicy.nextBaReminderSchedule(
            snapshot = BaPageSnapshot(
                apNotifyEnabled = true,
                apCurrent = 118.0,
                apRegenBaseMs = NOW_MS,
                apNotifyThreshold = 120,
                apLimit = 240,
                apLastNotifiedLevel = -1
            ),
            nowMs = NOW_MS
        )

        assertNotNull(schedule)
        assertEquals(NOW_MS + 2L * BA_AP_REGEN_INTERVAL_MS, schedule.triggerAtMillis)
        assertEquals(BackgroundAlarmWorkload.UserReminder, schedule.workload)
        assertEquals(BackgroundAlarmPrecision.Windowed, schedule.precision)
    }

    @Test
    fun `ba ap already above threshold prompts when level has not been notified`() {
        val schedule = AppBackgroundSchedulePolicy.nextBaReminderSchedule(
            snapshot = BaPageSnapshot(
                apNotifyEnabled = true,
                apCurrent = 121.0,
                apRegenBaseMs = NOW_MS,
                apNotifyThreshold = 120,
                apLimit = 240,
                apLastNotifiedLevel = 120
            ),
            nowMs = NOW_MS
        )

        assertNotNull(schedule)
        assertEquals(NOW_MS, schedule.triggerAtMillis)
        assertEquals(BackgroundAlarmPrecision.Prompt, schedule.precision)
    }

    @Test
    fun `ba cafe visit prompts when stored slot is stale`() {
        val currentSlot = currentCafeStudentRefreshSlotMs(
            nowMs = NOW_MS,
            serverIndex = 2
        )
        val schedule = AppBackgroundSchedulePolicy.nextBaReminderSchedule(
            snapshot = BaPageSnapshot(
                cafeVisitNotifyEnabled = true,
                cafeVisitLastNotifiedSlotMs = currentSlot - BA_CAFE_STUDENT_REFRESH_INTERVAL_MS,
                serverIndex = 2
            ),
            nowMs = NOW_MS
        )

        assertNotNull(schedule)
        assertEquals(NOW_MS, schedule.triggerAtMillis)
        assertEquals(BackgroundAlarmPrecision.Prompt, schedule.precision)
    }

    @Test
    fun `android17 alarm window is wider for long delays`() {
        assertEquals(
            10L * 60L * 1000L,
            AppBackgroundSchedulePolicy.android17WindowLength(
                triggerAtMillis = NOW_MS + 30L * 60L * 1000L,
                nowMs = NOW_MS
            )
        )
        assertEquals(
            30L * 60L * 1000L,
            AppBackgroundSchedulePolicy.android17WindowLength(
                triggerAtMillis = NOW_MS + 2L * 60L * 60L * 1000L,
                nowMs = NOW_MS
            )
        )
    }

    private companion object {
        private const val NOW_MS = 1_777_392_000_000L
    }
}
