package os.kei.core.notification.live.builder

import android.content.Context
import androidx.core.app.NotificationCompat
import os.kei.core.notification.R
import os.kei.core.notification.identity.NotificationAppIconResolver
import os.kei.core.notification.live.LiveNotificationPayload
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
            defaultAppIconResId = NotificationAppIconResolver.smallIconResId(),
        )
        val isBlueArchiveAp = spec.kind == ModernNotificationKind.BA_AP ||
                spec.kind == ModernNotificationKind.BA_CAFE_AP
        val isBlueArchiveCafeVisit = spec.kind == ModernNotificationKind.BA_CAFE_VISIT
        val isBlueArchiveArenaRefresh = spec.kind == ModernNotificationKind.BA_ARENA_REFRESH
        val isBlueArchiveCalendarPool = spec.kind == ModernNotificationKind.BA_CALENDAR_POOL
        val isGitHubShareImport = spec.kind == ModernNotificationKind.GITHUB_SHARE_IMPORT
        val progressState = computeProgressState(
            state = state,
            isBlueArchiveAp = isBlueArchiveAp,
            isBlueArchiveCalendarPool = isBlueArchiveCalendarPool,
            isGitHubShareImport = isGitHubShareImport
        )
        val builder = NotificationCompat.Builder(context, payload.environment.channelId)
            .setSmallIcon(spec.iconResId)
            .setLargeIcon(
                payload.semanticIconBitmap
                    ?: spec.kind.takeIf {
                        it == ModernNotificationKind.DEFAULT || it == ModernNotificationKind.WEBDAV_SYNC
                    }?.let { NotificationAppIconResolver.largeIconBitmap(context) }
                    ?: NotificationLargeIconFactory.create(context, spec.expandedIconResId)
            )
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
            .applyDeadline(state.deadlineAtMs)

        val showSecondaryAction = state.stopPendingIntent != state.openPendingIntent &&
                (state.running || state.showSecondaryActionWhenStopped)
        builder.addAction(0, state.primaryActionTitle(context), state.openPendingIntent)
        if (showSecondaryAction) {
            builder.addAction(0, state.stopActionTitle(context), state.stopPendingIntent)
        }
        val hasCustomDeleteAction = state.deletePendingIntent != state.stopPendingIntent
        if (state.stopPendingIntent != state.openPendingIntent &&
            ((!state.ongoing && state.showSecondaryActionWhenStopped) || hasCustomDeleteAction)
        ) {
            builder.setDeleteIntent(state.deletePendingIntent)
        }
        return builder.build()
    }

    private fun computeProgressState(
        state: LiveNotificationPayload,
        isBlueArchiveAp: Boolean,
        isBlueArchiveCalendarPool: Boolean,
        isGitHubShareImport: Boolean
    ): LiveProgressState {
        if (!state.running) {
            return LiveProgressState(current = 0, indeterminate = false)
        }
        if (isBlueArchiveCalendarPool || isGitHubShareImport) {
            return LiveProgressState(
                current = state.overrideProgressPercent?.coerceIn(0, 100) ?: 100,
                indeterminate = false
            )
        }
        if (
            LiveNotificationPayload.isBaCafeVisitServerName(state.serverName) ||
            LiveNotificationPayload.isBaArenaRefreshServerName(state.serverName)
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

    private fun NotificationCompat.Builder.applyDeadline(deadlineAtMs: Long?): NotificationCompat.Builder {
        if (deadlineAtMs == null) return this
        return setWhen(deadlineAtMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
    }
}
