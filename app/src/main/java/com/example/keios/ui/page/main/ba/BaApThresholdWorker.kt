package com.example.keios.ui.page.main.ba

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BaApThresholdWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val snapshot = withContext(Dispatchers.IO) { BASettingsStore.loadSnapshot() }
        if (!snapshot.apNotifyEnabled) {
            withContext(Dispatchers.IO) { BASettingsStore.saveApLastNotifiedLevel(-1) }
            return Result.success()
        }

        val nowMs = System.currentTimeMillis()
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
            return Result.success()
        }

        val lastNotifiedLevel = withContext(Dispatchers.IO) { BASettingsStore.loadApLastNotifiedLevel() }
        if (currentDisplay == lastNotifiedLevel) return Result.success()

        val sent = BaApNotificationDispatcher.send(
            context = applicationContext,
            currentDisplay = currentDisplay,
            limitDisplay = snapshot.apLimit.coerceIn(0, BA_AP_MAX),
            thresholdDisplay = threshold
        )
        if (sent) {
            withContext(Dispatchers.IO) { BASettingsStore.saveApLastNotifiedLevel(currentDisplay) }
        }
        return Result.success()
    }
}
