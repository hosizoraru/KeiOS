package com.example.keios.core.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.ui.page.main.ba.BASettingsStore

object AppBackgroundScheduler {
    private const val BA_AP_TICK_INTERVAL_MS = 6L * 60L * 1000L
    private const val GITHUB_FIRST_TICK_DELAY_MS = 2L * 60L * 1000L

    fun scheduleAll(context: Context) {
        scheduleGitHubRefresh(context)
        scheduleBaApThreshold(context)
    }

    fun scheduleGitHubRefresh(context: Context) {
        val appContext = context.applicationContext
        val snapshot = GitHubTrackStore.loadSnapshot()
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pending = AppBackgroundTickReceiver.githubTickPendingIntent(appContext)
        if (snapshot.items.isEmpty()) {
            alarmManager.cancel(pending)
            pending.cancel()
            return
        }
        val intervalMs = snapshot.refreshIntervalHours.coerceIn(1, 12) * 60L * 60L * 1000L
        val nowMs = System.currentTimeMillis()
        val nextAtMs = if (snapshot.lastRefreshMs > 0L) {
            (snapshot.lastRefreshMs + intervalMs).coerceAtLeast(nowMs + 60_000L)
        } else {
            nowMs + GITHUB_FIRST_TICK_DELAY_MS
        }
        scheduleWithAlarmManager(alarmManager, nextAtMs, pending)
    }

    fun scheduleBaApThreshold(context: Context) {
        val appContext = context.applicationContext
        val snapshot = BASettingsStore.loadSnapshot()
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pending = AppBackgroundTickReceiver.baApTickPendingIntent(appContext)
        if (!snapshot.apNotifyEnabled) {
            alarmManager.cancel(pending)
            pending.cancel()
            BASettingsStore.saveApLastNotifiedLevel(-1)
            return
        }
        val nextAtMs = System.currentTimeMillis() + BA_AP_TICK_INTERVAL_MS
        scheduleWithAlarmManager(alarmManager, nextAtMs, pending)
    }

    internal fun onTickHandled(context: Context, action: String) {
        when (action) {
            AppBackgroundTickReceiver.ACTION_GITHUB_TICK -> scheduleGitHubRefresh(context)
            AppBackgroundTickReceiver.ACTION_BA_AP_TICK -> scheduleBaApThreshold(context)
        }
    }

    private fun scheduleWithAlarmManager(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 15_000L),
            pendingIntent
        )
    }
}
