package os.kei.core.notification.live.builder

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import os.kei.core.notification.R
import os.kei.core.notification.identity.NotificationAppIconResolver
import os.kei.core.notification.live.LiveNotificationPayload

class ModernNotificationBuilder(
    private val context: Context
) : SessionNotificationBuilder {
    private fun baseNotificationBuilder(channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(NotificationAppIconResolver.smallIconResId())
    }

    override fun build(payload: NotificationPayload): Notification {
        val state = payload.state
        val spec = ModernNotificationSpecResolver.resolve(
            state = state,
            defaultAppIconResId = NotificationAppIconResolver.smallIconResId(),
        )
        val isDismissibleCalendarPoolUpdate =
            spec.kind == ModernNotificationKind.BA_CALENDAR_POOL &&
                    !spec.showProgressStyle &&
                    !spec.ongoing
        return baseNotificationBuilder(payload.environment.channelId)
            .clearActions()
            // Prevent state leakage between updates.
            .setContentText(null)
            .setOnlyAlertOnce(state.onlyAlertOnce)
            .setSilent(state.onlyAlertOnce)
            .setOngoing(spec.ongoing)
            .setRequestPromotedOngoing(spec.requestPromotedOngoing)
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
            .setContentIntent(state.openPendingIntent)
            .setCategory(spec.category)
            .setAutoCancel(isDismissibleCalendarPoolUpdate)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setStyle(buildStyle(spec, state))
            .applyDeadline(state.deadlineAtMs)
            .also { builder ->
                if (state.running) {
                    resolveShortCriticalText(spec, state)?.let(builder::setShortCriticalText)
                }
                val showSecondaryAction = state.stopPendingIntent != state.openPendingIntent &&
                        (state.running || state.showSecondaryActionWhenStopped)
                builder.addAction(0, state.primaryActionTitle(context), state.openPendingIntent)
                if (showSecondaryAction) {
                    builder.addAction(0, state.stopActionTitle(context), state.stopPendingIntent)
                }
                val hasCustomDeleteAction = state.deletePendingIntent != state.stopPendingIntent
                if (hasCustomDeleteAction || (showSecondaryAction && isDismissibleCalendarPoolUpdate)) {
                    builder.setDeleteIntent(state.deletePendingIntent)
                }
            }
            .build()
    }

    private fun NotificationCompat.Builder.applyDeadline(deadlineAtMs: Long?): NotificationCompat.Builder {
        if (deadlineAtMs == null) {
            return setShowWhen(true)
                .setUsesChronometer(false)
                .setChronometerCountDown(false)
        }
        return setWhen(deadlineAtMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
    }

    private fun buildStyle(
        spec: ModernNotificationSpec,
        state: LiveNotificationPayload
    ): NotificationCompat.Style {
        if (!spec.showProgressStyle) {
            return NotificationCompat.BigTextStyle()
                .bigText(state.content(context).ifBlank { " " })
        }
        return buildProgressStyle(spec)
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
            .setProgressTrackerIcon(
                spec.trackerIconResId?.let { resId ->
                    IconCompat.createWithResource(context, resId)
                }
            )
    }

    private fun resolveShortCriticalText(
        spec: ModernNotificationSpec,
        state: LiveNotificationPayload
    ): String? {
        return when (spec.shortCriticalMode) {
            ModernShortCriticalMode.NONE -> null
            ModernShortCriticalMode.SHORT_TEXT -> state.shortText
            ModernShortCriticalMode.ONLINE_TEXT -> state.onlineText(context)
        }
    }
}
