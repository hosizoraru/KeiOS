package os.kei.mcp.framework.notification.builder

import android.content.Context
import androidx.core.app.NotificationCompat
import os.kei.R
import os.kei.mcp.notification.McpNotificationPayload
import kotlin.math.roundToInt

class LegacyNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {

    private data class LiveProgressState(
        val current: Int,
        val indeterminate: Boolean
    )

    override fun build(payload: NotificationPayload): android.app.Notification {
        val state = payload.state
        val spec = ModernNotificationSpecResolver.resolve(
            state = state,
            preferOemLiveIconLayout = payload.environment.preferOemLiveIconLayout
        )
        val isBlueArchiveAp = spec.kind == ModernNotificationKind.BA_AP
        val isBlueArchiveCafeVisit = spec.kind == ModernNotificationKind.BA_CAFE_VISIT
        val isBlueArchiveArenaRefresh = spec.kind == ModernNotificationKind.BA_ARENA_REFRESH
        val progressState = computeProgressState(state = state, isBlueArchiveAp = isBlueArchiveAp)
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(spec.iconResId)
            .setLargeIcon(NotificationLargeIconFactory.create(context, spec.expandedIconResId))
            .setContentTitle(state.title(context))
            .setContentText(state.content(context).ifBlank { " " })
            .setSubText(
                if (state.running) {
                    state.onlineText(context)
                } else {
                    context.getString(R.string.mcp_notification_content_tap_restart)
                }
            )
            .setContentIntent(state.openPendingIntent)
            .setCategory(spec.category)
            .setColorized(true)
            .setColor(0xFF2563EB.toInt())
            .setOngoing(state.ongoing)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setAutoCancel(false)
            .setSilent(state.onlyAlertOnce)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, progressState.current, progressState.indeterminate)

        builder.addAction(0, context.getString(R.string.common_open), state.openPendingIntent)
        if (state.running) {
            builder.addAction(0, state.stopActionTitle(context), state.stopPendingIntent)
        }
        return builder.build()
    }

    private fun computeProgressState(
        state: McpNotificationPayload,
        isBlueArchiveAp: Boolean
    ): LiveProgressState {
        if (!state.running) {
            return LiveProgressState(current = 0, indeterminate = false)
        }
        if (
            McpNotificationPayload.isBaCafeVisitServerName(state.serverName) ||
            McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        ) {
            return LiveProgressState(current = 100, indeterminate = false)
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
