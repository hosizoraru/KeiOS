package os.kei.mcp.framework.notification.builder

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import os.kei.R
import os.kei.mcp.notification.McpNotificationPayload

class ModernNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {
    private val baseNotificationBuilder by lazy {
        NotificationCompat.Builder(context, os.kei.mcp.notification.McpNotificationHelper.LIVE_CHANNEL_ID)
            .setSmallIcon(os.kei.R.drawable.ic_kei_logo_color)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val spec = ModernNotificationSpecResolver.resolve(state)
        return baseNotificationBuilder
            .clearActions()
            // Prevent state leakage between updates.
            .setContentText(null)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setSilent(true)
            .setOngoing(spec.ongoing)
            .setRequestPromotedOngoing(spec.requestPromotedOngoing)
            .setSmallIcon(spec.iconResId)
            .setContentTitle(state.title(context))
            .setContentText(state.content(context).ifBlank { " " })
            .setContentIntent(state.openPendingIntent)
            .setCategory(spec.category)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setStyle(buildProgressStyle(spec))
            .also { builder ->
                if (state.running) {
                    resolveShortCriticalText(spec, state)?.let(builder::setShortCriticalText)
                }
                builder.addAction(0, context.getString(R.string.common_open), state.openPendingIntent)
                if (state.running) {
                    builder.addAction(0, state.stopActionTitle(context), state.stopPendingIntent)
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
