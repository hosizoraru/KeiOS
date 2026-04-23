package os.kei.mcp.framework.notification.builder

import android.content.Context
import androidx.core.app.NotificationCompat
import os.kei.R
import os.kei.mcp.notification.McpNotificationPayload
import kotlin.math.roundToInt

class LegacyNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {

    private companion object {
        private const val ICON_DEFAULT = R.drawable.ic_kei_logo_color
        private const val ICON_AP = R.drawable.ba_ap_icon
        private const val ICON_BA_CAFE_VISIT = R.drawable.ic_ba_schale
        private const val ICON_BA_ARENA_REFRESH = R.drawable.ic_ba_schale
    }

    private data class LiveProgressState(
        val current: Int,
        val indeterminate: Boolean
    )

    override fun build(payload: NotificationPayload): android.app.Notification {
        val state = payload.state
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(state.serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(state.serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.isBaArenaRefreshServerName(state.serverName)
        val progressState = computeProgressState(state = state, isBlueArchiveAp = isBlueArchiveAp)
        val iconRes = when {
            isBlueArchiveAp -> ICON_AP
            isBlueArchiveCafeVisit -> ICON_BA_CAFE_VISIT
            isBlueArchiveArenaRefresh -> ICON_BA_ARENA_REFRESH
            else -> ICON_DEFAULT
        }
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(iconRes)
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
            .setCategory(if (state.running) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_STATUS)
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
