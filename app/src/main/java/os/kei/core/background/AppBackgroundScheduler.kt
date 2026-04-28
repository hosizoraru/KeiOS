package os.kei.core.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import os.kei.core.platform.AndroidPlatformVersions
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.ui.page.main.ba.support.BASettingsStore

object AppBackgroundScheduler {
    fun scheduleAll(context: Context) {
        scheduleGitHubRefresh(context)
        scheduleBaApThreshold(context)
    }

    fun scheduleGitHubRefresh(context: Context) {
        val appContext = context.applicationContext
        val snapshot = GitHubTrackStore.loadSnapshot()
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pending = AppBackgroundTickReceiver.githubTickPendingIntent(appContext)
        val schedule = AppBackgroundSchedulePolicy.nextGitHubRefreshSchedule(
            trackedItemCount = snapshot.items.size,
            lastRefreshMs = snapshot.lastRefreshMs,
            refreshIntervalHours = snapshot.refreshIntervalHours,
            nowMs = System.currentTimeMillis()
        )
        if (schedule == null) {
            alarmManager.cancel(pending)
            pending.cancel()
            return
        }
        scheduleWithAlarmManager(alarmManager, schedule, pending)
    }

    fun scheduleBaApThreshold(context: Context) {
        val appContext = context.applicationContext
        val snapshot = BASettingsStore.loadSnapshot()
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pending = AppBackgroundTickReceiver.baApTickPendingIntent(appContext)
        val needsBaBackgroundTick =
            snapshot.apNotifyEnabled || snapshot.cafeVisitNotifyEnabled || snapshot.arenaRefreshNotifyEnabled
        if (!needsBaBackgroundTick) {
            alarmManager.cancel(pending)
            pending.cancel()
            BASettingsStore.saveApLastNotifiedLevel(-1)
            BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(0L)
            BASettingsStore.saveCafeVisitLastNotifiedSlotMs(0L)
            return
        }
        val schedule = AppBackgroundSchedulePolicy.nextBaReminderSchedule(
            snapshot = snapshot,
            nowMs = System.currentTimeMillis()
        )
        if (schedule == null) {
            alarmManager.cancel(pending)
            pending.cancel()
            return
        }
        scheduleWithAlarmManager(alarmManager, schedule, pending)
    }

    internal fun onTickHandled(context: Context, action: String) {
        when (action) {
            AppBackgroundTickReceiver.ACTION_GITHUB_TICK -> scheduleGitHubRefresh(context)
            AppBackgroundTickReceiver.ACTION_BA_AP_TICK -> scheduleBaApThreshold(context)
        }
    }

    private fun scheduleWithAlarmManager(
        alarmManager: AlarmManager,
        schedule: BackgroundAlarmSchedule,
        pendingIntent: PendingIntent
    ) {
        val nowMs = System.currentTimeMillis()
        val triggerAtMillis = schedule.triggerAtMillis.coerceAtLeast(
            nowMs + AppBackgroundSchedulePolicy.MIN_ALARM_DELAY_MS
        )
        alarmManager.cancel(pendingIntent)
        if (AndroidPlatformVersions.isAtLeastAndroid17) {
            scheduleAndroid17Alarm(alarmManager, schedule, triggerAtMillis, nowMs, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun scheduleAndroid17Alarm(
        alarmManager: AlarmManager,
        schedule: BackgroundAlarmSchedule,
        triggerAtMillis: Long,
        nowMs: Long,
        pendingIntent: PendingIntent,
    ) {
        val alarmType = when (schedule.workload) {
            BackgroundAlarmWorkload.RoutineSync -> AlarmManager.RTC
            BackgroundAlarmWorkload.UserReminder -> AlarmManager.RTC_WAKEUP
        }
        when (schedule.precision) {
            BackgroundAlarmPrecision.Prompt -> alarmManager.set(
                alarmType,
                triggerAtMillis,
                pendingIntent
            )

            BackgroundAlarmPrecision.Windowed -> alarmManager.setWindow(
                alarmType,
                triggerAtMillis,
                AppBackgroundSchedulePolicy.android17WindowLength(
                    triggerAtMillis = triggerAtMillis,
                    nowMs = nowMs
                ),
                pendingIntent
            )
        }
    }
}
