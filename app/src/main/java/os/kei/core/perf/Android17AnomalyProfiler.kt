package os.kei.core.perf

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.os.ProfilingManager
import android.os.ProfilingResult
import android.os.ProfilingTrigger
import android.util.Log
import os.kei.core.log.AppLogger
import os.kei.core.platform.AndroidPlatformVersions
import java.util.Locale

object Android17AnomalyProfiler {
    private const val TAG = "A17AnomalyProfiler"
    private const val ANOMALY_RATE_LIMIT_HOURS = 12
    private const val EXIT_LOOKBACK_COUNT = 8

    fun install(application: Application) {
        inspectPreviousResourceExit(application)
        if (!AndroidPlatformVersions.isAtLeastAndroid17) return
        registerAnomalyTrigger(application)
    }

    @SuppressLint("NewApi")
    private fun registerAnomalyTrigger(application: Application) {
        runCatching {
            val profilingManager = application.getSystemService(ProfilingManager::class.java)
                ?: return@runCatching
            profilingManager.registerForAllProfilingResults(application.mainExecutor) { result ->
                logProfilingResult(result)
            }
            val trigger = ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_ANOMALY)
                .setRateLimitingPeriodHours(ANOMALY_RATE_LIMIT_HOURS)
                .build()
            profilingManager.addProfilingTriggers(listOf(trigger))
            Log.i(TAG, "Registered Android 17 anomaly profiling trigger")
        }.onFailure { throwable ->
            AppLogger.w(TAG, "Register anomaly profiling trigger failed: ${throwable.message}", throwable)
        }
    }

    private fun inspectPreviousResourceExit(application: Application) {
        runCatching {
            val activityManager = application.getSystemService(ActivityManager::class.java)
                ?: return@runCatching
            activityManager
                .getHistoricalProcessExitReasons(application.packageName, 0, EXIT_LOOKBACK_COUNT)
                .filter(::isResourceRelatedExit)
                .forEach { info ->
                    AppLogger.w(
                        TAG,
                        "Previous resource exit: reason=${reasonLabel(info.reason)} " +
                            "description=${info.description.orEmpty()} " +
                            "pss=${formatBytes(info.pss * 1024L)} rss=${formatBytes(info.rss * 1024L)}"
                    )
                }
        }.onFailure { throwable ->
            AppLogger.w(TAG, "Inspect previous process exits failed: ${throwable.message}", throwable)
        }
    }

    private fun isResourceRelatedExit(info: ApplicationExitInfo): Boolean {
        val description = info.description.orEmpty()
        return info.reason == ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ||
            description.contains("MemoryLimiter", ignoreCase = true) ||
            description.contains("excessive", ignoreCase = true)
    }

    @SuppressLint("NewApi")
    private fun logProfilingResult(result: ProfilingResult) {
        val message = "trigger=${result.triggerType} tag=${result.tag.orEmpty()} " +
            "file=${result.resultFilePath.orEmpty()} error=${result.errorCode} " +
            "message=${result.errorMessage.orEmpty()}"
        if (result.errorCode == ProfilingResult.ERROR_NONE) {
            Log.i(TAG, "Profiling result: $message")
        } else {
            AppLogger.w(TAG, "Profiling result failed: $message")
        }
    }

    private fun reasonLabel(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_CRASH -> "CRASH"
            ApplicationExitInfo.REASON_OTHER -> "OTHER"
            else -> reason.toString()
        }
    }

    private fun formatBytes(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        if (safeBytes < 1024L) return "$safeBytes B"
        val kib = safeBytes / 1024.0
        if (kib < 1024.0) return String.format(Locale.US, "%.1f KiB", kib)
        val mib = kib / 1024.0
        if (mib < 1024.0) return String.format(Locale.US, "%.1f MiB", mib)
        return String.format(Locale.US, "%.2f GiB", mib / 1024.0)
    }
}
