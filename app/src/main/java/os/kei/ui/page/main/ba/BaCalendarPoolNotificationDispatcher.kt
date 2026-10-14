package os.kei.ui.page.main.ba

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import os.kei.MainActivity
import os.kei.R
import os.kei.core.intent.PendingIntentLaunchOptionsCompat
import os.kei.core.log.AppLogger
import os.kei.feature.notification.MiFocusNotificationActions
import os.kei.core.notification.live.NotificationHelper
import os.kei.core.notification.live.SessionNotifierImpl
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaPoolEntry
import os.kei.ui.page.main.ba.support.baCalendarKindLabel
import os.kei.ui.page.main.ba.support.baPoolTagLabel
import os.kei.ui.page.main.ba.support.baServerLabelRes
import os.kei.ui.page.main.ba.support.formatBaDateTimeNoYearInTimeZone
import os.kei.ui.page.main.ba.support.serverRefreshTimeZone

internal object BaCalendarPoolNotificationDispatcher {
    private const val TAG = "BaCalendarPoolNotify"
    private const val CALENDAR_UPCOMING_NOTIFICATION_ID_BASE = 389_100_000
    private const val CALENDAR_ENDING_NOTIFICATION_ID_BASE = 390_100_000
    private const val POOL_UPCOMING_NOTIFICATION_ID_BASE = 391_100_000
    private const val POOL_ENDING_NOTIFICATION_ID_BASE = 392_100_000
    private const val CHANGE_NOTIFICATION_ID = 38914
    private const val MAX_VISIBLE_NAMES = 1
    private const val MAX_NOTIFICATION_NAME_CHARS = 16
    private const val MAX_CHANGE_DETAIL_CHARS = 14

    fun sendCalendarUpcoming(
        context: Context,
        serverIndex: Int,
        entry: BaCalendarEntry,
    ): Boolean = sendCalendarUpcomingGroup(context, serverIndex, listOf(entry))

    fun sendCalendarUpcomingGroup(
        context: Context,
        serverIndex: Int,
        entries: List<BaCalendarEntry>,
    ): Boolean {
        val normalizedEntries = entries.sortedBy { it.title }
        val notifyAtMs = normalizedEntries.firstOrNull()?.beginAtMs ?: return false
        val serverLabel = context.getString(baServerLabelRes(serverIndex))
        return sendLiveUpdate(
            context = context,
            notificationId =
                baCalendarPoolGroupedNotificationId(
                    baseId = CALENDAR_UPCOMING_NOTIFICATION_ID_BASE,
                    serverIndex = serverIndex,
                    notifyAtMs = notifyAtMs,
                ),
            destination = BaCalendarPoolNotificationDestination.Calendar,
            serverIndex = serverIndex,
            title =
                context.getString(
                    R.string.ba_calendar_notify_upcoming_title_with_server,
                    serverLabel,
                ),
            focusTitle = context.getString(R.string.ba_calendar_notify_upcoming_title),
            content =
                context.getString(
                    R.string.ba_calendar_notify_upcoming_content,
                    summarizeCalendarEntries(context, normalizedEntries),
                    formatBaDateTimeNoYearInTimeZone(
                        notifyAtMs,
                        serverRefreshTimeZone(serverIndex),
                    ),
                ),
            shortText = context.getString(R.string.ba_calendar_pool_notify_short_calendar),
            onlineText = context.getString(R.string.ba_calendar_pool_notify_phase_start),
            deadlineAtMs = notifyAtMs,
            progressPercent = resolveDeadlineProgressPercent(notifyAtMs),
        )
    }

    fun sendCalendarEnding(
        context: Context,
        serverIndex: Int,
        entry: BaCalendarEntry,
    ): Boolean = sendCalendarEndingGroup(context, serverIndex, listOf(entry))

    fun sendCalendarEndingGroup(
        context: Context,
        serverIndex: Int,
        entries: List<BaCalendarEntry>,
    ): Boolean {
        val normalizedEntries = entries.sortedBy { it.title }
        val notifyAtMs = normalizedEntries.firstOrNull()?.endAtMs ?: return false
        val serverLabel = context.getString(baServerLabelRes(serverIndex))
        return sendLiveUpdate(
            context = context,
            notificationId =
                baCalendarPoolGroupedNotificationId(
                    baseId = CALENDAR_ENDING_NOTIFICATION_ID_BASE,
                    serverIndex = serverIndex,
                    notifyAtMs = notifyAtMs,
                ),
            destination = BaCalendarPoolNotificationDestination.Calendar,
            serverIndex = serverIndex,
            title =
                context.getString(
                    R.string.ba_calendar_notify_ending_title_with_server,
                    serverLabel,
                ),
            focusTitle = context.getString(R.string.ba_calendar_notify_ending_title),
            content =
                context.getString(
                    R.string.ba_calendar_notify_ending_content,
                    summarizeCalendarEntries(context, normalizedEntries),
                    formatBaDateTimeNoYearInTimeZone(notifyAtMs, serverRefreshTimeZone(serverIndex)),
                ),
            shortText = context.getString(R.string.ba_calendar_pool_notify_short_calendar),
            onlineText = context.getString(R.string.ba_calendar_pool_notify_phase_end),
            deadlineAtMs = notifyAtMs,
            progressPercent = resolveDeadlineProgressPercent(notifyAtMs),
        )
    }

    fun sendPoolUpcoming(
        context: Context,
        serverIndex: Int,
        entry: BaPoolEntry,
    ): Boolean = sendPoolUpcomingGroup(context, serverIndex, listOf(entry))

    fun sendPoolUpcomingGroup(
        context: Context,
        serverIndex: Int,
        entries: List<BaPoolEntry>,
    ): Boolean {
        val normalizedEntries = entries.sortedBy { it.name }
        val notifyAtMs = normalizedEntries.firstOrNull()?.startAtMs ?: return false
        val serverLabel = context.getString(baServerLabelRes(serverIndex))
        return sendLiveUpdate(
            context = context,
            notificationId =
                baCalendarPoolGroupedNotificationId(
                    baseId = POOL_UPCOMING_NOTIFICATION_ID_BASE,
                    serverIndex = serverIndex,
                    notifyAtMs = notifyAtMs,
                ),
            destination = BaCalendarPoolNotificationDestination.Pool,
            serverIndex = serverIndex,
            title =
                context.getString(
                    R.string.ba_pool_notify_upcoming_title_with_server,
                    serverLabel,
                ),
            focusTitle = context.getString(R.string.ba_pool_notify_upcoming_title),
            content =
                context.getString(
                    R.string.ba_pool_notify_upcoming_content,
                    summarizePoolEntries(context, normalizedEntries),
                    formatBaDateTimeNoYearInTimeZone(
                        notifyAtMs,
                        serverRefreshTimeZone(serverIndex),
                    ),
                ),
            shortText = context.getString(R.string.ba_calendar_pool_notify_short_pool),
            onlineText = context.getString(R.string.ba_calendar_pool_notify_phase_start),
            deadlineAtMs = notifyAtMs,
            progressPercent = resolveDeadlineProgressPercent(notifyAtMs),
        )
    }

    fun sendPoolEnding(
        context: Context,
        serverIndex: Int,
        entry: BaPoolEntry,
    ): Boolean = sendPoolEndingGroup(context, serverIndex, listOf(entry))

    fun sendPoolEndingGroup(
        context: Context,
        serverIndex: Int,
        entries: List<BaPoolEntry>,
    ): Boolean {
        val normalizedEntries = entries.sortedBy { it.name }
        val notifyAtMs = normalizedEntries.firstOrNull()?.endAtMs ?: return false
        val serverLabel = context.getString(baServerLabelRes(serverIndex))
        return sendLiveUpdate(
            context = context,
            notificationId =
                baCalendarPoolGroupedNotificationId(
                    baseId = POOL_ENDING_NOTIFICATION_ID_BASE,
                    serverIndex = serverIndex,
                    notifyAtMs = notifyAtMs,
                ),
            destination = BaCalendarPoolNotificationDestination.Pool,
            serverIndex = serverIndex,
            title =
                context.getString(
                    R.string.ba_pool_notify_ending_title_with_server,
                    serverLabel,
                ),
            focusTitle = context.getString(R.string.ba_pool_notify_ending_title),
            content =
                context.getString(
                    R.string.ba_pool_notify_ending_content,
                    summarizePoolEntries(context, normalizedEntries),
                    formatBaDateTimeNoYearInTimeZone(notifyAtMs, serverRefreshTimeZone(serverIndex)),
                ),
            shortText = context.getString(R.string.ba_calendar_pool_notify_short_pool),
            onlineText = context.getString(R.string.ba_calendar_pool_notify_phase_end),
            deadlineAtMs = notifyAtMs,
            progressPercent = resolveDeadlineProgressPercent(notifyAtMs),
        )
    }

    fun sendDataChanged(
        context: Context,
        serverIndex: Int,
        calendarChangeCount: Int,
        poolChangeCount: Int,
        detail: String = "",
    ): Boolean {
        val copy =
            buildDataChangedCopy(
                context = context,
                serverIndex = serverIndex,
                calendarChangeCount = calendarChangeCount,
                poolChangeCount = poolChangeCount,
                detail = detail,
            )
        return sendLiveUpdate(
            context = context,
            notificationId = changeNotificationId(serverIndex),
            destination = copy.destination,
            serverIndex = serverIndex,
            title = copy.title,
            focusTitle = copy.focusTitle,
            content = copy.content,
            shortText = copy.shortText,
            onlineText = copy.onlineText,
            deadlineAtMs = null,
            progressPercent = 0,
        )
    }

    private fun notificationsGranted(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun sendLiveUpdate(
        context: Context,
        notificationId: Int,
        destination: BaCalendarPoolNotificationDestination,
        serverIndex: Int,
        title: String,
        focusTitle: String,
        content: String,
        shortText: String,
        onlineText: String,
        deadlineAtMs: Long?,
        progressPercent: Int,
    ): Boolean {
        if (!notificationsGranted(context)) {
            AppLogger.w(TAG) {
                "skip notification: permission missing id=$notificationId destination=$destination server=$serverIndex"
            }
            return false
        }
        McpNotificationHelper.ensureChannel(context)
        val helper = NotificationHelper(context)
        val openPendingIntent =
            openBaPendingIntent(
                context = context,
                notificationId = notificationId,
                destination = destination,
                serverIndex = serverIndex,
            )
        val focusOpenPendingIntent =
            focusOpenBaPendingIntent(
                context = context,
                notificationId = notificationId,
                destination = destination,
                serverIndex = serverIndex,
            )
        val acknowledgePendingIntent = acknowledgePendingIntent(context, notificationId)
        val ongoing = deadlineAtMs != null
        val payload =
            LiveNotificationPayload(
                serverName = LiveNotificationPayload.BA_CALENDAR_POOL_SERVER_NAME,
                running = true,
                port = progressPercent.coerceIn(0, 100),
                path = content,
                clients = 1,
                ongoing = ongoing,
                onlyAlertOnce = false,
                openPendingIntent = openPendingIntent,
                stopPendingIntent = acknowledgePendingIntent,
                focusOpenPendingIntent = focusOpenPendingIntent,
                secondaryActionLabel = context.getString(R.string.common_acknowledge),
                overrideTitle = title,
                overrideContent = content,
                overrideOnlineText = onlineText,
                overrideShortText = shortText,
                overrideProgressPercent = progressPercent.coerceIn(0, 100),
                miFocusTitle = focusTitle,
                miFocusSpecialTitle = context.getString(baServerLabelRes(serverIndex)),
                miFocusContent = content,
                deadlineAtMs = deadlineAtMs,
                notificationId = notificationId,
                miFocusOrderId = baCalendarPoolMiFocusOrderId(notificationId),
            )
        val buildResult = SessionNotifierImpl(helper).build(payload)
        val dispatched = McpNotificationHelper.dispatchNotification(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic,
        )
        AppLogger.i(TAG) {
            "dispatch result=$dispatched id=$notificationId destination=$destination server=$serverIndex " +
                "deadline=${deadlineAtMs != null} style=${buildResult.style} xiaomiMagic=${buildResult.useXiaomiMagic}"
        }
        return dispatched
    }

    fun buildDataChangedCopy(
        context: Context,
        serverIndex: Int,
        calendarChangeCount: Int,
        poolChangeCount: Int,
        detail: String,
    ): BaCalendarPoolNotificationCopy {
        val serverLabel = context.getString(baServerLabelRes(serverIndex))
        val calendarCount = calendarChangeCount.coerceAtLeast(0)
        val poolCount = poolChangeCount.coerceAtLeast(0)
        val destination =
            if (poolCount > 0 && calendarCount <= 0) {
                BaCalendarPoolNotificationDestination.Pool
            } else {
                BaCalendarPoolNotificationDestination.Calendar
            }
        val title =
            when {
                calendarCount > 0 && poolCount > 0 ->
                    context.getString(
                        R.string.ba_calendar_pool_notify_change_title_with_server,
                        serverLabel,
                    )

                poolCount > 0 ->
                    context.getString(
                        R.string.ba_pool_notify_change_title_with_server,
                        serverLabel,
                    )

                else ->
                    context.getString(
                        R.string.ba_calendar_notify_change_title_with_server,
                        serverLabel,
                    )
            }
        val focusTitle =
            context.getString(R.string.ba_calendar_pool_notify_change_title)
        val baseContent =
            when {
                calendarCount > 0 && poolCount > 0 ->
                    context.getString(
                        R.string.ba_calendar_pool_notify_change_content,
                        calendarCount,
                        poolCount,
                    )

                poolCount > 0 ->
                    context.getString(R.string.ba_pool_notify_change_content, poolCount)

                else ->
                    context.getString(R.string.ba_calendar_notify_change_content, calendarCount)
            }
        val detailText =
            detail.trim()
                .takeIf { calendarCount + poolCount == 1 && it.isNotBlank() }
                ?.takeNotificationDetailPrefix()
        val content =
            detailText?.let {
                context.getString(
                    R.string.ba_calendar_pool_notify_change_content_with_detail,
                    baseContent,
                    it,
                )
            } ?: baseContent
        val onlineText =
            when {
                calendarCount > 0 && poolCount > 0 ->
                    context.getString(R.string.ba_calendar_pool_notify_short_both)

                poolCount > 0 ->
                    context.getString(R.string.ba_calendar_pool_notify_short_pool)

                else ->
                    context.getString(R.string.ba_calendar_pool_notify_short_calendar)
            }
        return BaCalendarPoolNotificationCopy(
            destination = destination,
            title = title,
            focusTitle = focusTitle,
            content = content,
            shortText = context.getString(R.string.ba_calendar_pool_notify_short_change),
            onlineText = onlineText,
        )
    }

    private fun String.takeNotificationDetailPrefix(): String =
        takeNotificationTextPrefix(MAX_CHANGE_DETAIL_CHARS)

    private fun String.takeNotificationNamePrefix(): String =
        takeNotificationTextPrefix(MAX_NOTIFICATION_NAME_CHARS)

    private fun String.takeNotificationTextPrefix(maxChars: Int): String =
        if (length <= maxChars) {
            this
        } else {
            take(maxChars).trimEnd() + "…"
        }

    private fun summarizeCalendarEntries(
        context: Context,
        entries: List<BaCalendarEntry>,
    ): String =
        summarizeNames(
            context = context,
            names =
                entries.map { entry ->
                    entry.title.ifBlank {
                        context.baCalendarKindLabel(entry.kindId, entry.kindName)
                    }
                },
        )

    private fun summarizePoolEntries(
        context: Context,
        entries: List<BaPoolEntry>,
    ): String =
        summarizeNames(
            context = context,
            names =
                entries.map { entry ->
                    entry.name.ifBlank { context.baPoolTagLabel(entry.tagId, entry.tagName) }
                },
        )

    private fun summarizeNames(
        context: Context,
        names: List<String>,
    ): String {
        val visibleNames =
            names
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.takeNotificationNamePrefix() }
                .take(MAX_VISIBLE_NAMES)
        val separator = context.getString(R.string.ba_calendar_pool_notify_name_separator)
        val visibleText = visibleNames.joinToString(separator = separator)
        val remainingCount = (names.size - visibleNames.size).coerceAtLeast(0)
        return if (remainingCount > 0) {
            context.getString(
                R.string.ba_calendar_pool_notify_name_list_more,
                visibleText,
                remainingCount,
            )
        } else {
            visibleText
        }
    }

    private fun changeNotificationId(serverIndex: Int): Int =
        CHANGE_NOTIFICATION_ID + serverIndex.coerceIn(0, 2)

    private fun resolveDeadlineProgressPercent(deadlineAtMs: Long): Int {
        val nowMs = System.currentTimeMillis()
        val leadMs =
            BaSettingsPersistenceRepository
                .loadCalendarPoolNotificationSettings()
                .calendarPoolNotifyLeadHours
                .coerceAtLeast(1) * 60L * 60L * 1000L
        val windowStartMs = deadlineAtMs - leadMs
        return (((nowMs - windowStartMs).coerceAtLeast(0L).toFloat() / leadMs.toFloat()) * 100f)
            .toInt()
            .coerceIn(1, 99)
    }

    private fun openBaPendingIntent(
        context: Context,
        notificationId: Int,
        destination: BaCalendarPoolNotificationDestination,
        serverIndex: Int,
    ): PendingIntent {
        val intent = baCalendarPoolOpenIntent(context, destination, serverIndex).apply {
            identifier = "os.kei.ba.calendar.pool.notification:body:$notificationId"
        }
        return PendingIntentLaunchOptionsCompat.getUserVisibleActivity(
            context,
            520_100 + notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun focusOpenBaPendingIntent(
        context: Context,
        notificationId: Int,
        destination: BaCalendarPoolNotificationDestination,
        serverIndex: Int,
    ): PendingIntent {
        val intent = baCalendarPoolOpenIntent(context, destination, serverIndex).apply {
            identifier = "os.kei.ba.calendar.pool.notification:focus:$notificationId"
        }
        return PendingIntent.getActivity(
            context,
            522_100 + notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun acknowledgePendingIntent(
        context: Context,
        notificationId: Int,
    ): PendingIntent =
        MiFocusNotificationActions.markReadPendingIntent(
            context = context,
            notificationId = notificationId,
            requestCode = 521_100 + notificationId,
        )
}

internal data class BaCalendarPoolNotificationCopy(
    val destination: BaCalendarPoolNotificationDestination,
    val title: String,
    val focusTitle: String,
    val content: String,
    val shortText: String,
    val onlineText: String,
)

internal fun baCalendarPoolOpenIntent(
    context: Context,
    destination: BaCalendarPoolNotificationDestination,
    serverIndex: Int,
): Intent {
    val targetRoute =
        when (destination) {
            BaCalendarPoolNotificationDestination.Calendar -> MainActivity.TARGET_ROUTE_BA_ACTIVITY_CALENDAR
            BaCalendarPoolNotificationDestination.Pool -> MainActivity.TARGET_ROUTE_BA_POOL
        }
    // Both destinations are nav routes now, so the notification lands in MainActivity and pushes
    // onto the shared back stack the way the WebDAV sync notification already does.
    return Intent(context, MainActivity::class.java)
        .withBaCalendarPoolServerIndex(serverIndex)
        .apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_BA)
            putExtra(MainActivity.EXTRA_TARGET_ROUTE, targetRoute)
        }
}

internal fun baCalendarPoolGroupedNotificationId(
    baseId: Int,
    serverIndex: Int,
    notifyAtMs: Long,
): Int {
    val serverBucket = serverIndex.coerceIn(0, 2) * 300_000
    val timeBucketHash = (notifyAtMs / 60_000L).hashCode().and(0x7fffffff) % 300_000
    return baseId + serverBucket + timeBucketHash
}

internal fun baCalendarPoolMiFocusOrderId(notificationId: Int): String =
    "ba-calendar-pool-${notificationId.coerceAtLeast(0)}"
