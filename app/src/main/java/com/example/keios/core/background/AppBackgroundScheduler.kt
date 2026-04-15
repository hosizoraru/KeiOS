package com.example.keios.core.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.worker.GitHubAutoRefreshWorker
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.ba.BaApThresholdWorker
import java.util.concurrent.TimeUnit

object AppBackgroundScheduler {
    private const val UNIQUE_GITHUB_REFRESH_WORK = "keios.github.refresh.periodic"
    private const val UNIQUE_BA_AP_NOTIFY_WORK = "keios.ba.ap.notify.periodic"

    fun scheduleAll(context: Context) {
        scheduleGitHubRefresh(context)
        scheduleBaApThreshold(context)
    }

    fun scheduleGitHubRefresh(context: Context) {
        val appContext = context.applicationContext
        val snapshot = GitHubTrackStore.loadSnapshot()
        val intervalHours = snapshot.refreshIntervalHours.coerceIn(1, 12)
        if (snapshot.items.isEmpty()) {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_GITHUB_REFRESH_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<GitHubAutoRefreshWorker>(
            repeatInterval = intervalHours.toLong(),
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(UNIQUE_GITHUB_REFRESH_WORK)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_GITHUB_REFRESH_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleBaApThreshold(context: Context) {
        val appContext = context.applicationContext
        val snapshot = BASettingsStore.loadSnapshot()
        if (!snapshot.apNotifyEnabled) {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_BA_AP_NOTIFY_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<BaApThresholdWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .addTag(UNIQUE_BA_AP_NOTIFY_WORK)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_BA_AP_NOTIFY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
