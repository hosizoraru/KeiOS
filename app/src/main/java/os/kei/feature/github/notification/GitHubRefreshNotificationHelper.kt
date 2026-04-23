package os.kei.feature.github.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import os.kei.MainActivity
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.core.notification.focus.MiFocusNotificationAction
import os.kei.core.notification.focus.MiFocusNotificationCompactMode
import os.kei.core.notification.focus.MiFocusNotificationDisplayIconStyle
import os.kei.core.notification.focus.MiFocusNotificationExpandedMode
import os.kei.core.notification.focus.MiFocusNotificationSpec
import os.kei.core.notification.focus.MiFocusNotificationTemplate
import os.kei.core.prefs.UiPrefs
import os.kei.feature.notification.NotificationActionReceiver
import os.kei.mcp.framework.notification.NotificationHelper
import os.kei.mcp.notification.McpNotificationHelper
import kotlin.math.roundToInt

object GitHubRefreshNotificationHelper {
    private const val TAG = "GitHubRefreshNotify"
    const val CHANNEL_ID = "github_refresh_channel_v2"
    const val NOTIFICATION_ID = 38990
    private const val ISLAND_ICON_RES_ID = R.drawable.ic_github_invertocat_island_blue
    private const val MI_PROGRESS_COLOR = "#1A73E8"
    private const val MI_PROGRESS_TRACK_COLOR = "#334155"

    private data class NotificationBuildResult(
        val notification: Notification,
        val style: RenderStyle,
        val useXiaomiMagic: Boolean
    )

    private data class RefreshState(
        val current: Int,
        val total: Int,
        val trackedCount: Int,
        val updatableCount: Int,
        val failedCount: Int,
        val running: Boolean,
        val cancelled: Boolean
    ) {
        val safeTotal: Int = total.coerceAtLeast(1)
        val safeCurrent: Int = current.coerceIn(0, safeTotal)
        val progressPercent: Int =
            ((safeCurrent.toFloat() / safeTotal.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        val shortText: String
            get() = "$safeCurrent/$safeTotal"

        val keepUntilRead: Boolean
            get() = !running && !cancelled
    }

    private enum class RenderStyle {
        MI_ISLAND,
        LIVE_UPDATE
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.github_refresh_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.github_refresh_channel_desc)
                setShowBadge(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyProgress(
        context: Context,
        current: Int,
        total: Int,
        trackedCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        notifyInternal(
            context = context,
            state = RefreshState(
                current = current,
                total = total,
                trackedCount = trackedCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = true,
                cancelled = false
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyCompleted(
        context: Context,
        total: Int,
        trackedCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        notifyInternal(
            context = context,
            state = RefreshState(
                current = total,
                total = total,
                trackedCount = trackedCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = false,
                cancelled = false
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyCancelled(
        context: Context,
        current: Int,
        total: Int,
        trackedCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        notifyInternal(
            context = context,
            state = RefreshState(
                current = current,
                total = total,
                trackedCount = trackedCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = false,
                cancelled = true
            ),
            onlyAlertOnce = true
        )
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun resolveTitle(context: Context, state: RefreshState): String {
        return when {
            state.running -> context.getString(R.string.github_refresh_title_running)
            state.cancelled -> context.getString(R.string.github_refresh_title_cancelled)
            else -> context.getString(R.string.github_refresh_title_completed)
        }
    }

    private fun resolveContent(context: Context, state: RefreshState): String {
        return if (state.failedCount > 0) {
            context.getString(
                R.string.github_refresh_content_with_failed,
                state.safeCurrent,
                state.safeTotal,
                state.trackedCount,
                state.updatableCount,
                state.failedCount
            )
        } else {
            context.getString(
                R.string.github_refresh_content,
                state.safeCurrent,
                state.safeTotal,
                state.trackedCount,
                state.updatableCount
            )
        }
    }

    private fun resolveCompactProgressText(context: Context, state: RefreshState): String {
        return context.getString(R.string.github_refresh_progress_percent, state.progressPercent)
    }

    private fun resolveCompactFractionText(context: Context, state: RefreshState): String {
        return context.getString(
            R.string.github_refresh_progress_fraction,
            state.safeCurrent,
            state.safeTotal
        )
    }

    private fun resolveCompactStateContent(context: Context, state: RefreshState): String? {
        return when {
            state.cancelled -> context.getString(R.string.github_refresh_island_cancelled)
            state.failedCount > 0 ->
                context.getString(R.string.github_refresh_failed_short_with_count, state.failedCount)
            else -> null
        }
    }

    private fun notifyInternal(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ) {
        ensureChannel(context)
        val buildResult = buildNotification(
            context = context,
            state = state,
            onlyAlertOnce = onlyAlertOnce
        )
        if (buildResult.style == RenderStyle.MI_ISLAND) {
            McpNotificationHelper.dispatchNotification(
                context = context,
                notificationId = NOTIFICATION_ID,
                notification = buildResult.notification,
                useXiaomiMagic = buildResult.useXiaomiMagic
            )
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, buildResult.notification)
    }

    private fun buildNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): NotificationBuildResult {
        val helper = NotificationHelper(context)
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val style = if (preferSuperIsland && helper.isMiIslandAvailable) {
            RenderStyle.MI_ISLAND
        } else {
            RenderStyle.LIVE_UPDATE
        }
        AppLogger.i(
            TAG,
            "buildNotification preferSuperIsland=$preferSuperIsland supportMiIsland=${helper.isSupportMiIsland} " +
                "focusPermission=${helper.hasMiIslandPermission} available=${helper.isMiIslandAvailable} " +
                "style=$style"
        )
        val notification = when (style) {
            RenderStyle.MI_ISLAND -> buildMiIslandNotification(context, state, onlyAlertOnce)
            RenderStyle.LIVE_UPDATE -> {
                if (helper.isModernLiveUpdateEligible) {
                    buildModernLiveUpdateNotification(context, state, onlyAlertOnce)
                } else {
                    buildLegacyLiveUpdateNotification(context, state, onlyAlertOnce)
                }
            }
        }
        return NotificationBuildResult(
            notification = notification,
            style = style,
            useXiaomiMagic = style == RenderStyle.MI_ISLAND &&
                UiPrefs.isSuperIslandBypassRestrictionEnabled(defaultValue = false)
        )
    }

    private fun buildModernLiveUpdateNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        val progressColor = if (state.running) 0xFF2E7D32.toInt() else 0xFF64748B.toInt()
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(
                listOf(NotificationCompat.ProgressStyle.Segment(100).setColor(progressColor))
            )
            .setStyledByProgress(true)
            .setProgress(state.progressPercent)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ISLAND_ICON_RES_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openPendingIntent)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setSilent(true)
            .setOngoing(state.running || state.keepUntilRead)
            .setRequestPromotedOngoing(state.running || state.keepUntilRead)
            .setStyle(progressStyle)
            .setShortCriticalText(state.shortText)
            .addAction(0, context.getString(R.string.common_open), openPendingIntent)
            .addAction(0, context.getString(R.string.common_acknowledge), readPendingIntent)
            .build()
    }

    private fun buildLegacyLiveUpdateNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ISLAND_ICON_RES_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(context.getString(R.string.common_progress_with_value, state.shortText))
            .setContentIntent(openPendingIntent)
            .setCategory(
                if (state.running) NotificationCompat.CATEGORY_PROGRESS
                else NotificationCompat.CATEGORY_STATUS
            )
            .setColorized(true)
            .setColor(0xFF2563EB.toInt())
            .setOngoing(state.running || state.keepUntilRead)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setSilent(onlyAlertOnce)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, state.progressPercent, false)
            .addAction(0, context.getString(R.string.common_open), openPendingIntent)
            .addAction(0, context.getString(R.string.common_acknowledge), readPendingIntent)
            .build()
    }

    private fun buildMiIslandNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        val shortCriticalText = if (state.running) {
            resolveCompactProgressText(context, state)
        } else {
            resolveCompactFractionText(context, state)
        }
        val baseBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ISLAND_ICON_RES_ID)
            .setContentTitle(title)
            .setContentText(content.ifBlank { " " })
            .setContentIntent(openPendingIntent)
            .setCategory(
                if (state.running) NotificationCompat.CATEGORY_PROGRESS
                else NotificationCompat.CATEGORY_STATUS
            )
            .setOngoing(state.running || state.keepUntilRead)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setShortCriticalText(shortCriticalText)
        if (state.running) {
            baseBuilder
                .setProgress(100, state.progressPercent, false)
                .addAction(0, context.getString(R.string.common_open), openPendingIntent)
                .addAction(0, context.getString(R.string.common_acknowledge), readPendingIntent)
        }
        buildMiIslandFocusExtras(
            context = context,
            state = state,
            title = title,
            content = content,
            openPendingIntent = openPendingIntent,
            readPendingIntent = readPendingIntent
        )?.let(baseBuilder::addExtras)
        return baseBuilder.build()
    }

    private fun buildMiIslandFocusExtras(
        context: Context,
        state: RefreshState,
        title: String,
        content: String,
        openPendingIntent: PendingIntent,
        readPendingIntent: PendingIntent
    ) = runCatching {
        MiFocusNotificationTemplate.build(
            context = context,
            spec = if (state.running) {
                MiFocusNotificationSpec(
                    title = title,
                    content = content,
                    compactTitle = " ",
                    displayIconResId = ISLAND_ICON_RES_ID,
                    tickerIconResId = ISLAND_ICON_RES_ID,
                    compactMode = MiFocusNotificationCompactMode.PROGRESS,
                    expandedMode = MiFocusNotificationExpandedMode.BASE_INFO,
                    displayIconStyle = MiFocusNotificationDisplayIconStyle.ORIGINAL,
                    allowFloat = false,
                    outerGlow = true,
                    updatable = true,
                    showNotification = true,
                    progressPercent = state.progressPercent,
                    progressColor = MI_PROGRESS_COLOR,
                    progressTrackColor = MI_PROGRESS_TRACK_COLOR,
                    showExpandedProgress = true
                )
            } else {
                MiFocusNotificationSpec(
                    title = title,
                    content = content,
                    compactTitle = resolveCompactFractionText(context, state),
                    compactContent = resolveCompactStateContent(context, state),
                    displayIconResId = ISLAND_ICON_RES_ID,
                    tickerIconResId = ISLAND_ICON_RES_ID,
                    compactMode = MiFocusNotificationCompactMode.TEXT,
                    expandedMode = MiFocusNotificationExpandedMode.ICON_TEXT,
                    displayIconStyle = MiFocusNotificationDisplayIconStyle.ORIGINAL,
                    allowFloat = true,
                    outerGlow = true,
                    updatable = false,
                    showNotification = true,
                    showHighlightColor = state.failedCount > 0,
                    embedTextButtons = true,
                    actions = listOf(
                        MiFocusNotificationAction(
                            key = "github_action_open",
                            title = context.getString(R.string.common_open),
                            pendingIntent = openPendingIntent,
                            isHighlighted = true
                        ),
                        MiFocusNotificationAction(
                            key = "github_action_read",
                            title = context.getString(R.string.common_acknowledge),
                            pendingIntent = readPendingIntent
                        )
                    )
                )
            }
        )
    }.onFailure {
        AppLogger.e(TAG, "Build FocusNotification extras failed", it)
    }.getOrNull()

    private fun buildOpenPendingIntent(context: Context): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_GITHUB)
        }
        return PendingIntent.getActivity(
            context,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMarkReadPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        return PendingIntent.getBroadcast(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
