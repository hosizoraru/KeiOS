package com.example.keios.feature.github.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.domain.GitHubReleaseCheckService
import com.example.keios.feature.github.notification.GitHubRefreshNotificationHelper
import com.example.keios.feature.github.model.GitHubTrackedReleaseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubAutoRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val snapshot = withContext(Dispatchers.IO) { GitHubTrackStore.loadSnapshot() }
        val tracked = snapshot.items
        if (tracked.isEmpty()) return Result.success()

        val intervalMs = snapshot.refreshIntervalHours.coerceIn(1, 12) * 60L * 60L * 1000L
        val nowMs = System.currentTimeMillis()
        if (snapshot.lastRefreshMs > 0L &&
            (nowMs - snapshot.lastRefreshMs).coerceAtLeast(0L) < intervalMs
        ) {
            return Result.success()
        }

        return runCatching {
            val states = LinkedHashMap<String, com.example.keios.feature.github.model.GitHubCheckCacheEntry>()
            var updatableCount = 0
            var failedCount = 0
            tracked.forEach { item ->
                val check = withContext(Dispatchers.IO) {
                    GitHubReleaseCheckService.evaluateTrackedApp(applicationContext, item)
                }
                if (check.hasUpdate == true) updatableCount += 1
                if (check.status == GitHubTrackedReleaseStatus.Failed) failedCount += 1
                val cacheEntry = with(GitHubReleaseCheckService) { check.toCacheEntry() }
                states[item.id] = cacheEntry
            }
            withContext(Dispatchers.IO) {
                GitHubTrackStore.saveCheckCache(states, nowMs)
            }
            if (updatableCount > 0 || failedCount > 0) {
                GitHubRefreshNotificationHelper.notifyCompleted(
                    context = applicationContext,
                    total = tracked.size,
                    trackedCount = tracked.size,
                    updatableCount = updatableCount,
                    failedCount = failedCount
                )
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
