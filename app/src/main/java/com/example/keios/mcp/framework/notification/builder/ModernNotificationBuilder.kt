package com.example.keios.mcp.framework.notification.builder

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.keios.R
import kotlin.math.roundToInt

class ModernNotificationBuilder(
    private val context: Context
) : InstallerNotificationBuilder {

    private companion object {
        private const val PROGRESS_ACTIVE_COLOR = 0xFF2E7D32.toInt()
        private const val PROGRESS_IDLE_COLOR = 0xFF64748B.toInt()
        private const val ACCENT_BLUE = 0xFF2563EB.toInt()
    }

    private data class LiveProgressState(
        val current: Int,
        val indeterminate: Boolean
    )

    override fun build(payload: NotificationPayload): android.app.Notification {
        val state = payload.state
        val isBlueArchiveAp = state.serverName.trim() == "BlueArchive AP"
        val progressState = computeProgressState(state = state, isBlueArchiveAp = isBlueArchiveAp)
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(state.title)
            .setContentText(state.content.ifBlank { " " })
            .setSubText(if (state.running) state.onlineText else "点击返回应用重新启动服务")
            .setContentIntent(state.openPendingIntent)
            .setCategory(if (state.running) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_STATUS)
            .setColorized(true)
            .setColor(ACCENT_BLUE)
            .setOngoing(state.running || state.ongoing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setUsesChronometer(state.running && !isBlueArchiveAp)
            .setRequestPromotedOngoing(state.running || state.ongoing)

        if (payload.environment.isHyperOS) {
            builder
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShortCriticalText(if (state.running) state.shortText else state.statusText)
        }

        builder.addAction(0, "打开", state.openPendingIntent)
        if (state.running) {
            builder.addAction(0, state.stopActionTitle, state.stopPendingIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val segmentColor = if (state.running) PROGRESS_ACTIVE_COLOR else PROGRESS_IDLE_COLOR
            builder.setStyle(
                NotificationCompat.ProgressStyle()
                    .setStyledByProgress(true)
                    .setProgress(progressState.current)
                    .setProgressSegments(
                        listOf(
                            NotificationCompat.ProgressStyle.Segment(100).setColor(segmentColor)
                        )
                    )
                    .setProgressPoints(
                        listOf(
                            NotificationCompat.ProgressStyle.Point(progressState.current)
                                .setColor(0xFFFFFFFF.toInt())
                        )
                    )
            )
            if (progressState.indeterminate) {
                builder.setProgress(100, progressState.current, true)
            }
        } else {
            builder.setProgress(100, progressState.current, progressState.indeterminate)
        }

        return builder.build()
    }

    private fun computeProgressState(
        state: com.example.keios.mcp.McpNotificationPayload,
        isBlueArchiveAp: Boolean
    ): LiveProgressState {
        if (!state.running) {
            return LiveProgressState(current = 0, indeterminate = false)
        }
        if (isBlueArchiveAp) {
            val apLimit = state.clients.coerceAtLeast(1)
            val apCurrent = state.port.coerceAtLeast(0).coerceAtMost(apLimit)
            val normalized = ((apCurrent.toFloat() / apLimit.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            return LiveProgressState(current = normalized, indeterminate = false)
        }
        val onlineClients = state.clients.coerceAtLeast(0)
        val indeterminate = onlineClients <= 0
        val normalized = (onlineClients * 24).coerceIn(8, 100)
        return LiveProgressState(current = normalized, indeterminate = indeterminate)
    }
}
