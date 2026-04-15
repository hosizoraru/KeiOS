package com.example.keios.feature.github.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.keios.MainActivity
import com.example.keios.R
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.feature.notification.NotificationActionReceiver
import com.example.keios.mcp.framework.notification.NotificationHelper
import com.xzakota.hyper.notification.focus.FocusNotification
import kotlin.math.roundToInt

object GitHubRefreshNotificationHelper {
    private const val TAG = "GitHubRefreshNotify"
    const val CHANNEL_ID = "github_refresh_channel_v1"
    const val NOTIFICATION_ID = 38990
    private const val ISLAND_ICON_RES_ID = R.drawable.ic_github_invertocat_island_blue

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
                NotificationManager.IMPORTANCE_DEFAULT
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

    private fun notifyInternal(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ) {
        ensureChannel(context)
        val notification = buildNotification(
            context = context,
            state = state,
            onlyAlertOnce = onlyAlertOnce
        )
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val helper = NotificationHelper(context)
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val style = if (preferSuperIsland && helper.isSupportMiIsland) {
            RenderStyle.MI_ISLAND
        } else {
            RenderStyle.LIVE_UPDATE
        }
        return when (style) {
            RenderStyle.MI_ISLAND -> buildMiIslandNotification(context, state, onlyAlertOnce)
            RenderStyle.LIVE_UPDATE -> {
                if (helper.isModernLiveUpdateEligible) {
                    buildModernLiveUpdateNotification(context, state, onlyAlertOnce)
                } else {
                    buildLegacyLiveUpdateNotification(context, state, onlyAlertOnce)
                }
            }
        }
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
            .addAction(0, context.getString(R.string.common_mark_read), readPendingIntent)
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
            .addAction(0, context.getString(R.string.common_mark_read), readPendingIntent)
            .build()
    }

    private fun buildMiIslandNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val iconResId = ISLAND_ICON_RES_ID
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        val baseBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(content.ifBlank { " " })
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(state.running || state.keepUntilRead)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        buildMiIslandFocusExtras(
            context = context,
            iconResId = iconResId,
            title = title,
            content = content,
            rightTitle = state.shortText,
            ongoing = state.running,
            openPendingIntent = openPendingIntent,
            readPendingIntent = readPendingIntent
        )?.let(baseBuilder::addExtras)
        return baseBuilder.build()
    }

    private fun buildMiIslandFocusExtras(
        context: Context,
        iconResId: Int,
        title: String,
        content: String,
        rightTitle: String,
        ongoing: Boolean,
        openPendingIntent: PendingIntent,
        readPendingIntent: PendingIntent
    ) = runCatching {
        FocusNotification.buildV3 {
            val lightLogoIcon = Icon.createWithResource(context, iconResId)
            val darkLogoIcon = Icon.createWithResource(context, iconResId)
            val light = createPicture("github_logo_light", lightLogoIcon)
            val dark = createPicture("github_logo_dark", darkLogoIcon)

            islandFirstFloat = true
            enableFloat = !ongoing
            updatable = true
            ticker = title
            tickerPic = light

            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = dark
                        }
                    }
                    imageTextInfoRight {
                        type = 3
                        textInfo {
                            this.title = rightTitle
                        }
                    }
                }
                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = dark
                    }
                }
            }

            baseInfo {
                type = 2
                this.title = title
                this.content = content.ifBlank { " " }
            }

            picInfo {
                type = 1
                pic = light
                picDark = dark
            }

            textButton {
                addActionInfo {
                    val nativeAction = Notification.Action.Builder(
                        Icon.createWithResource(context, iconResId),
                        context.getString(R.string.common_open),
                        openPendingIntent
                    ).build()
                    action = createAction("github_action_open", nativeAction)
                    actionTitle = context.getString(R.string.common_open)
                    actionBgColor = "#006EFF"
                    actionBgColorDark = "#006EFF"
                    actionTitleColor = "#FFFFFF"
                    actionTitleColorDark = "#FFFFFF"
                }
                addActionInfo {
                    val nativeAction = Notification.Action.Builder(
                        Icon.createWithResource(context, iconResId),
                        context.getString(R.string.common_mark_read),
                        readPendingIntent
                    ).build()
                    action = createAction("github_action_read", nativeAction)
                    actionTitle = context.getString(R.string.common_mark_read)
                }
            }
        }
    }.onFailure {
        Log.e(TAG, "Build FocusNotification extras failed", it)
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
