package os.kei.feature.github.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xzakota.hyper.notification.focus.FocusNotification
import os.kei.MainActivity
import os.kei.R
import os.kei.core.intent.PendingIntentLaunchOptionsCompat
import os.kei.core.log.AppLogger
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
    private const val RUNNING_PROGRESS_FLOOR = 6
    private const val RUNNING_PROGRESS_CEILING = 96

    private val progressLock = Any()
    private var lastRunningTotal = 0
    private var lastDisplayedProgressPercent = 0

    private data class NotificationBuildResult(
        val notification: Notification,
        val style: RenderStyle,
        val useXiaomiMagic: Boolean
    )

    private data class RefreshState(
        val current: Int,
        val total: Int,
        val preReleaseUpdateCount: Int,
        val updatableCount: Int,
        val failedCount: Int,
        val running: Boolean,
        val cancelled: Boolean,
        val displayProgressPercent: Int
    ) {
        val safeTotal: Int = total.coerceAtLeast(1)
        val safeCurrent: Int = current.coerceIn(0, safeTotal)
        val progressPercent: Int =
            displayProgressPercent.coerceIn(0, 100)

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
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        val isComplete = total > 0 && safeCurrent >= safeTotal
        val displayProgressPercent = resolveDisplayProgressPercent(
            current = safeCurrent,
            total = safeTotal,
            running = !isComplete,
            cancelled = false
        )
        notifyInternal(
            context = context,
            state = RefreshState(
                current = safeCurrent,
                total = safeTotal,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = !isComplete,
                cancelled = false,
                displayProgressPercent = displayProgressPercent
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyCompleted(
        context: Context,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        notifyInternal(
            context = context,
            state = RefreshState(
                current = total,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = false,
                cancelled = false,
                displayProgressPercent = resolveDisplayProgressPercent(
                    current = total,
                    total = total,
                    running = false,
                    cancelled = false
                )
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyCancelled(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        notifyInternal(
            context = context,
            state = RefreshState(
                current = current,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = false,
                cancelled = true,
                displayProgressPercent = resolveDisplayProgressPercent(
                    current = current,
                    total = total,
                    running = false,
                    cancelled = true
                )
            ),
            onlyAlertOnce = true
        )
    }

    fun cancel(context: Context) {
        resetDisplayProgress()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun resolveDisplayProgressPercent(
        current: Int,
        total: Int,
        running: Boolean,
        cancelled: Boolean
    ): Int = synchronized(progressLock) {
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        if (cancelled) {
            val cancelledProgress = lastDisplayedProgressPercent.coerceIn(0, RUNNING_PROGRESS_CEILING)
            resetDisplayProgressLocked()
            return@synchronized cancelledProgress
        }
        if (!running) {
            resetDisplayProgressLocked()
            return@synchronized 100
        }
        if (safeTotal != lastRunningTotal || safeCurrent == 0) {
            lastRunningTotal = safeTotal
            lastDisplayedProgressPercent = 0
        }
        val rawProgress = ((safeCurrent.toFloat() / safeTotal.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(RUNNING_PROGRESS_FLOOR, RUNNING_PROGRESS_CEILING)
        val smoothedProgress = rawProgress
            .coerceAtLeast(lastDisplayedProgressPercent)
            .coerceAtMost(RUNNING_PROGRESS_CEILING)
        lastDisplayedProgressPercent = smoothedProgress
        smoothedProgress
    }

    private fun resetDisplayProgress() = synchronized(progressLock) {
        resetDisplayProgressLocked()
    }

    private fun resetDisplayProgressLocked() {
        lastRunningTotal = 0
        lastDisplayedProgressPercent = 0
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
                state.preReleaseUpdateCount,
                state.updatableCount,
                state.failedCount
            )
        } else {
            context.getString(
                R.string.github_refresh_content,
                state.safeCurrent,
                state.safeTotal,
                state.preReleaseUpdateCount,
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
        val style = if (preferSuperIsland && helper.isSupportMiIsland) {
            RenderStyle.MI_ISLAND
        } else {
            RenderStyle.LIVE_UPDATE
        }
        AppLogger.i(
            TAG,
            "buildNotification preferSuperIsland=$preferSuperIsland supportMiIsland=${helper.isSupportMiIsland} " +
                "focusPermission=${helper.hasMiIslandPermission} style=$style"
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
        val iconResId = ISLAND_ICON_RES_ID
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val shortCriticalText = if (state.running) {
            resolveCompactProgressText(context, state)
        } else {
            resolveCompactFractionText(context, state)
        }
        val baseBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(content.ifBlank { " " })
            .setContentIntent(openPendingIntent)
            .setCategory(
                if (state.running) NotificationCompat.CATEGORY_PROGRESS
                else NotificationCompat.CATEGORY_STATUS
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColorized(true)
            .setColor(Color.parseColor(MI_PROGRESS_COLOR))
            .setOngoing(state.running || state.keepUntilRead)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setRequestPromotedOngoing(state.running || state.keepUntilRead)
            .setShortCriticalText(shortCriticalText)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(
                if (state.running) 100 else 0,
                if (state.running) state.progressPercent else 0,
                false
            )

        buildMiIslandFocusExtras(
            context = context,
            state = state,
            title = title,
            content = content,
            iconResId = iconResId
        )?.let(baseBuilder::addExtras)
        return baseBuilder.build()
    }

    private fun buildMiIslandFocusExtras(
        context: Context,
        state: RefreshState,
        title: String,
        content: String,
        iconResId: Int
    ) = runCatching {
        val progressPercent = state.progressPercent.coerceIn(0, 100)
        val fractionText = resolveCompactFractionText(context, state)
        val compactStateContent = resolveCompactStateContent(context, state)
        FocusNotification.buildV3 {
            val lightLogoIcon = Icon.createWithResource(context, iconResId)
            val darkLogoIcon = Icon.createWithResource(context, iconResId)
            val light = createPicture("github_logo_light", lightLogoIcon)
            val dark = createPicture("github_logo_dark", darkLogoIcon)

            islandFirstFloat = true
            enableFloat = !state.running
            updatable = true
            ticker = title
            tickerPic = light
            tickerPicDark = dark
            outEffectSrc = "outer_glow"

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
                    if (state.running) {
                        progressTextInfo {
                            progressInfo {
                                progress = progressPercent
                                isCCW = true
                                colorReach = MI_PROGRESS_COLOR
                                colorUnReach = MI_PROGRESS_TRACK_COLOR
                            }
                            textInfo {
                                this.title = " "
                            }
                        }
                    } else {
                        imageTextInfoRight {
                            type = 3
                            textInfo {
                                this.title = fractionText
                                this.content = compactStateContent
                                this.showHighlightColor = state.failedCount > 0
                            }
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

            if (state.running) {
                multiProgressInfo {
                    progress = progressPercent
                    color = MI_PROGRESS_COLOR
                }
            }

            picInfo {
                type = 1
                pic = light
                picDark = dark
            }

            if (!state.running) {
                textButton {
                    addActionInfo {
                        val nativeAction = Notification.Action.Builder(
                            Icon.createWithResource(context, iconResId),
                            context.getString(R.string.common_open),
                            buildOpenPendingIntent(context)
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
                            context.getString(R.string.common_acknowledge),
                            buildMarkReadPendingIntent(context)
                        ).build()
                        action = createAction("github_action_read", nativeAction)
                        actionTitle = context.getString(R.string.common_acknowledge)
                    }
                }
            }
        }
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
        return PendingIntentLaunchOptionsCompat.getUserVisibleActivity(
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
