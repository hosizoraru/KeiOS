package com.example.keios.core.perf

import android.util.Log
import android.view.Window
import androidx.metrics.performance.JankStats
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object AppJankMonitor {
    private const val TAG = "KeiJank"
    private const val REPORT_WINDOW_MS = 6_000L

    fun attach(
        window: Window,
        enabled: Boolean
    ): JankStats? {
        if (!enabled) return null

        val frameCount = AtomicInteger(0)
        val jankCount = AtomicInteger(0)
        val worstDurationTimes100Ms = AtomicLong(0L)
        val lastReportAtMs = AtomicLong(System.currentTimeMillis())

        return runCatching {
            JankStats.createAndTrack(window) { frameData ->
                frameCount.incrementAndGet()
                val durationTimes100Ms = frameData.frameDurationUiNanos / 10_000L

                while (true) {
                    val currentWorst = worstDurationTimes100Ms.get()
                    if (durationTimes100Ms <= currentWorst) break
                    if (worstDurationTimes100Ms.compareAndSet(currentWorst, durationTimes100Ms)) break
                }

                if (frameData.isJank) {
                    jankCount.incrementAndGet()
                }

                val nowMs = System.currentTimeMillis()
                val lastMs = lastReportAtMs.get()
                if (nowMs - lastMs < REPORT_WINDOW_MS) return@createAndTrack
                if (!lastReportAtMs.compareAndSet(lastMs, nowMs)) return@createAndTrack

                val totalFrames = frameCount.getAndSet(0).coerceAtLeast(1)
                val totalJank = jankCount.getAndSet(0)
                val worstMs = worstDurationTimes100Ms.getAndSet(0L) / 100f
                if (totalJank <= 0) return@createAndTrack

                val jankRate = (totalJank.toFloat() * 100f) / totalFrames.toFloat()
                val stateSummary = frameData.states
                    .take(5)
                    .joinToString(" | ") { state -> "${state.key}=${state.value}" }
                Log.w(
                    TAG,
                    "jankRate=${"%.1f".format(Locale.US, jankRate)}% " +
                        "($totalJank/$totalFrames), " +
                        "worst=${"%.2f".format(Locale.US, worstMs)}ms, states=$stateSummary"
                )
            }.apply {
                jankHeuristicMultiplier = 2.0f
            }
        }.onFailure { throwable ->
            Log.w(TAG, "Jank monitor attach failed: ${throwable.message}")
        }.getOrNull()
    }
}
