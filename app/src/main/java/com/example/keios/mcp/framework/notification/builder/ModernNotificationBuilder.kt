package com.example.keios.mcp.framework.notification.builder

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.keios.R
import com.example.keios.mcp.notification.McpNotificationPayload

class ModernNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val spec = ModernNotificationSpecResolver.resolve(state)
        return NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(spec.iconResId)
            .setContentTitle(state.title(context))
            .setContentText(state.content(context).ifBlank { " " })
            .setContentIntent(state.openPendingIntent)
            .setCategory(spec.category)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setSilent(true)
            .setAutoCancel(false)
            .setOngoing(spec.ongoing)
            .setRequestPromotedOngoing(spec.requestPromotedOngoing)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setStyle(buildProgressStyle(spec))
            .apply {
                resolveShortCriticalText(spec, state)?.let(::setShortCriticalText)
                addAction(0, context.getString(R.string.common_open), state.openPendingIntent)
                if (state.running) {
                    addAction(0, state.stopActionTitle(context), state.stopPendingIntent)
                }
            }
            .build()
    }

    private fun buildProgressStyle(spec: ModernNotificationSpec): NotificationCompat.ProgressStyle {
        return NotificationCompat.ProgressStyle()
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(100)
                        .setColor(spec.progressColor)
                )
            )
            .setStyledByProgress(true)
            .setProgress(spec.progressPercent)
    }

    private fun resolveShortCriticalText(
        spec: ModernNotificationSpec,
        state: McpNotificationPayload
    ): String? {
        return when (spec.shortCriticalMode) {
            ModernShortCriticalMode.NONE -> null
            ModernShortCriticalMode.SHORT_TEXT -> state.shortText
            ModernShortCriticalMode.ONLINE_TEXT -> state.onlineText(context)
        }
    }
}
