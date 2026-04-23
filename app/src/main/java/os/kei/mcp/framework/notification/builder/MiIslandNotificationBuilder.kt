package os.kei.mcp.framework.notification.builder

import android.app.Notification
import androidx.core.app.NotificationCompat
import os.kei.core.log.AppLogger
import os.kei.core.notification.focus.MiFocusNotificationTemplate

class MiIslandNotificationBuilder(
    private val context: android.content.Context
) : SessionNotificationBuilder {
    private companion object {
        private const val TAG = "McpMiIslandBuilder"
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val spec = MiIslandNotificationSpecResolver.resolve(
            context = context,
            payload = payload
        )
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(spec.smallIconResId)
            .setContentTitle(state.title(context))
            .setContentText(state.content(context).ifBlank { " " })
            .setContentIntent(state.openPendingIntent)
            .setCategory(spec.category)
            .setOngoing(spec.ongoing)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setAutoCancel(false)

        if (spec.requestPromotedOngoing) {
            builder.setRequestPromotedOngoing(true)
        }
        if (spec.foregroundServiceImmediate) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        spec.shortCriticalText?.let(builder::setShortCriticalText)
        if (spec.showExpandedProgress) {
            builder.setProgress(100, spec.progressPercent.coerceIn(0, 100), false)
        }
        spec.regularActions.forEach { action ->
            builder.addAction(0, action.title, action.pendingIntent)
        }
        buildFocusExtras(spec)?.let(builder::addExtras)
        return builder.build()
    }

    private fun buildFocusExtras(spec: MiIslandNotificationSpec) = runCatching {
        MiFocusNotificationTemplate.build(context, spec.focusSpec)
    }.onFailure {
        AppLogger.e(TAG, "Build FocusNotification extras failed", it)
    }.getOrNull()
}
