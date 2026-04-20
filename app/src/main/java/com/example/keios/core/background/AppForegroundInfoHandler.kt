package com.example.keios.core.background

import android.content.Context
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.domain.GitHubReleaseCheckService
import com.example.keios.feature.github.model.GitHubCheckCacheEntry
import com.example.keios.feature.github.model.GitHubTrackedReleaseStatus
import com.example.keios.feature.github.notification.GitHubRefreshNotificationHelper
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.ba.support.BA_AP_MAX
import com.example.keios.ui.page.main.ba.BaApNotificationDispatcher
import com.example.keios.ui.page.main.ba.BaArenaRefreshNotificationDispatcher
import com.example.keios.ui.page.main.ba.BaCafeVisitNotificationDispatcher
import com.example.keios.ui.page.main.ba.support.BaPageSnapshot
import com.example.keios.ui.page.main.ba.applyBaApRegenTick
import com.example.keios.ui.page.main.ba.support.currentArenaRefreshSlotMs
import com.example.keios.ui.page.main.ba.support.currentCafeStudentRefreshSlotMs
import com.example.keios.ui.page.main.ba.support.displayAp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object AppForegroundInfoHandler {
    private val githubTickMutex = Mutex()
    private val baApTickMutex = Mutex()

    suspend fun handleGitHubTick(context: Context) {
        githubTickMutex.withLock {
            val snapshot = withContext(Dispatchers.IO) { GitHubTrackStore.loadSnapshot() }
            val tracked = snapshot.items
            if (tracked.isEmpty()) return

            val intervalMs = snapshot.refreshIntervalHours.coerceIn(1, 12) * 60L * 60L * 1000L
            val nowMs = System.currentTimeMillis()
            if (snapshot.lastRefreshMs > 0L &&
                (nowMs - snapshot.lastRefreshMs).coerceAtLeast(0L) < intervalMs
            ) {
                return
            }

            val states = LinkedHashMap<String, GitHubCheckCacheEntry>()
            var updatableCount = 0
            var failedCount = 0
            tracked.forEach { item ->
                val check = withContext(Dispatchers.IO) {
                    GitHubReleaseCheckService.evaluateTrackedApp(context, item)
                }
                if (check.hasUpdate == true) updatableCount += 1
                if (check.status == GitHubTrackedReleaseStatus.Failed) failedCount += 1
                states[item.id] = with(GitHubReleaseCheckService) { check.toCacheEntry() }
            }

            withContext(Dispatchers.IO) {
                GitHubTrackStore.saveCheckCache(states, nowMs)
            }
            if (updatableCount > 0 || failedCount > 0) {
                GitHubRefreshNotificationHelper.notifyCompleted(
                    context = context,
                    total = tracked.size,
                    trackedCount = tracked.size,
                    updatableCount = updatableCount,
                    failedCount = failedCount
                )
            }
        }
    }

    suspend fun handleBaApTick(context: Context) {
        baApTickMutex.withLock {
            val snapshot = withContext(Dispatchers.IO) { BASettingsStore.loadSnapshot() }
            val shouldHandleApNotify = snapshot.apNotifyEnabled
            val shouldHandleArenaRefreshNotify = snapshot.arenaRefreshNotifyEnabled
            val shouldHandleCafeVisitNotify = snapshot.cafeVisitNotifyEnabled
            if (!shouldHandleApNotify && !shouldHandleArenaRefreshNotify && !shouldHandleCafeVisitNotify) {
                withContext(Dispatchers.IO) {
                    BASettingsStore.saveApLastNotifiedLevel(-1)
                    BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(0L)
                    BASettingsStore.saveCafeVisitLastNotifiedSlotMs(0L)
                }
                return
            }

            val nowMs = System.currentTimeMillis()
            if (shouldHandleApNotify) {
                handleBaApThresholdTick(context = context, snapshot = snapshot, nowMs = nowMs)
            } else {
                withContext(Dispatchers.IO) {
                    BASettingsStore.saveApLastNotifiedLevel(-1)
                }
            }

            if (shouldHandleArenaRefreshNotify) {
                handleBaArenaRefreshTick(context = context, snapshot = snapshot, nowMs = nowMs)
            } else {
                withContext(Dispatchers.IO) {
                    BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(0L)
                }
            }

            if (shouldHandleCafeVisitNotify) {
                handleBaCafeVisitTick(context = context, snapshot = snapshot, nowMs = nowMs)
            } else {
                withContext(Dispatchers.IO) {
                    BASettingsStore.saveCafeVisitLastNotifiedSlotMs(0L)
                }
            }
        }
    }

    private suspend fun handleBaApThresholdTick(
        context: Context,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        val (nextAp, nextBase) = applyBaApRegenTick(
            apLimit = snapshot.apLimit,
            apCurrent = snapshot.apCurrent,
            apRegenBaseMs = snapshot.apRegenBaseMs,
            nowMs = nowMs
        )
        if (nextAp != snapshot.apCurrent) {
            withContext(Dispatchers.IO) {
                BASettingsStore.saveApCurrent(nextAp)
                BASettingsStore.saveApRegenBaseMs(nextBase)
            }
        }

        val threshold = snapshot.apNotifyThreshold.coerceIn(0, BA_AP_MAX)
        val currentDisplay = displayAp(nextAp)
        if (currentDisplay < threshold) {
            withContext(Dispatchers.IO) { BASettingsStore.saveApLastNotifiedLevel(-1) }
            return
        }

        val lastNotifiedLevel = withContext(Dispatchers.IO) { BASettingsStore.loadApLastNotifiedLevel() }
        if (currentDisplay == lastNotifiedLevel) return

        val sent = BaApNotificationDispatcher.send(
            context = context,
            currentDisplay = currentDisplay,
            limitDisplay = snapshot.apLimit.coerceIn(0, BA_AP_MAX),
            thresholdDisplay = threshold
        )
        if (sent) {
            withContext(Dispatchers.IO) { BASettingsStore.saveApLastNotifiedLevel(currentDisplay) }
        }
    }

    private suspend fun handleBaCafeVisitTick(
        context: Context,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        val currentSlotMs = currentCafeStudentRefreshSlotMs(
            nowMs = nowMs,
            serverIndex = snapshot.serverIndex
        )
        val lastSlotMs = withContext(Dispatchers.IO) { BASettingsStore.loadCafeVisitLastNotifiedSlotMs() }
        if (lastSlotMs <= 0L) {
            withContext(Dispatchers.IO) { BASettingsStore.saveCafeVisitLastNotifiedSlotMs(currentSlotMs) }
            return
        }
        if (currentSlotMs <= lastSlotMs) {
            return
        }

        val sent = BaCafeVisitNotificationDispatcher.send(
            context = context,
            serverIndex = snapshot.serverIndex,
            slotMs = currentSlotMs
        )
        if (sent) {
            withContext(Dispatchers.IO) { BASettingsStore.saveCafeVisitLastNotifiedSlotMs(currentSlotMs) }
        }
    }

    private suspend fun handleBaArenaRefreshTick(
        context: Context,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        val currentSlotMs = currentArenaRefreshSlotMs(
            nowMs = nowMs,
            serverIndex = snapshot.serverIndex
        )
        val lastSlotMs = withContext(Dispatchers.IO) { BASettingsStore.loadArenaRefreshLastNotifiedSlotMs() }
        if (lastSlotMs <= 0L) {
            withContext(Dispatchers.IO) { BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(currentSlotMs) }
            return
        }
        if (currentSlotMs <= lastSlotMs) {
            return
        }

        val sent = BaArenaRefreshNotificationDispatcher.send(
            context = context,
            serverIndex = snapshot.serverIndex,
            slotMs = currentSlotMs
        )
        if (sent) {
            withContext(Dispatchers.IO) { BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(currentSlotMs) }
        }
    }

}
