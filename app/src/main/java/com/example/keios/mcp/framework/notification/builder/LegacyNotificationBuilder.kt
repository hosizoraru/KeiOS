package com.example.keios.mcp.framework.notification.builder

import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.keios.R
import kotlin.math.roundToInt

class LegacyNotificationBuilder(
    private val context: Context
) : InstallerNotificationBuilder {

    private data class LiveProgressState(
        val current: Int,
        val indeterminate: Boolean
    )

    override fun build(payload: NotificationPayload): android.app.Notification {
        val state = payload.state
        val isBlueArchiveAp = state.serverName.trim() == "BlueArchive AP"
        val progressState = computeProgressState(state = state, isBlueArchiveAp = isBlueArchiveAp)
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(R.drawable.ic_kei_logo_color)
            .setContentTitle(state.title)
            .setContentText(state.content.ifBlank { " " })
            .setSubText(if (state.running) state.onlineText else "点击返回应用重新启动服务")
            .setContentIntent(state.openPendingIntent)
            .setCategory(if (state.running) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_STATUS)
            .setColorized(true)
            .setColor(0xFF2563EB.toInt())
            .setOngoing(state.ongoing)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setAutoCancel(false)
            .setSilent(state.onlyAlertOnce)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, progressState.current, progressState.indeterminate)

        builder.addAction(0, "打开", state.openPendingIntent)
        if (state.running) {
            builder.addAction(0, state.stopActionTitle, state.stopPendingIntent)
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
