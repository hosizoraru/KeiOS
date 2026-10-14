package os.kei.mcp.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import os.kei.feature.mcp.R
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.SessionNotifier
import os.kei.core.notification.live.NotificationHelper
import os.kei.core.notification.live.LiveNotificationChannels
import os.kei.core.notification.live.SessionNotifierImpl
import os.kei.core.notification.identity.NotificationAppIconResolver
import os.kei.mcp.service.McpKeepAliveService

object McpNotificationHelper {
    private const val TAG = "McpNotificationHelper"
    const val CHANNEL_ID = "mcp_keepalive_channel_v2"
    const val LIVE_CHANNEL_ID = "mcp_live_update_channel_v1"
    const val FOREGROUND_SERVICE_CHANNEL_ID = "mcp_keepalive_service_channel_v1"
    const val KEEPALIVE_FOREGROUND_NOTIFICATION_ID = 38887
    const val KEEPALIVE_NOTIFICATION_ID = 38888
    const val BA_AP_NOTIFICATION_ID = 38889
    const val BA_CAFE_VISIT_NOTIFICATION_ID = 38890
    const val BA_ARENA_REFRESH_NOTIFICATION_ID = 38891
    const val BA_CAFE_AP_NOTIFICATION_ID = 38892
    const val WEBDAV_SYNC_NOTIFICATION_ID = 38893
    private const val TEST_NOTIFICATION_ID = KEEPALIVE_NOTIFICATION_ID
    private const val ACTION_DISMISS = "os.kei.mcp.keepalive.DISMISS"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"
    private enum class SecondaryActionMode {
        DEFAULT,
        MARK_READ
    }

    fun ensureChannel(context: Context) {
        McpNotificationChannels.ensure(context)
    }

    fun refreshCurrentNotificationStyle(context: Context) {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        McpNotificationSnapshotStore.entries().forEach { (notificationId, snapshot) ->
            refreshCachedNotificationIfActive(
                context = context,
                manager = manager,
                notificationId = notificationId,
                snapshot = snapshot
            )
        }
    }

    fun buildForegroundNotification(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        ongoing: Boolean,
        onlyAlertOnce: Boolean = true,
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID,
        targetBaAccountId: String? = null,
    ): Notification {
        return buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = onlyAlertOnce,
            notificationId = notificationId,
            targetBaAccountId = targetBaAccountId,
        ).notification
    }

    fun buildForegroundBootstrapNotification(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID,
        targetBaAccountId: String? = null,
    ): Notification {
        val openIntent =
            buildOpenIntent(
                context = context,
                serverName = serverName,
                targetBaAccountId = targetBaAccountId,
            )
        val openPendingIntent = McpNotificationOpenIntents.create(
            context = context,
            notificationId = notificationId,
            intent = openIntent,
            entry = McpNotificationOpenEntry.ForegroundBootstrap,
        )
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = serverName,
                notificationId = notificationId,
                miFocusOrderId = null,
            )
        val payload = McpNotificationPayload(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = true,
            openPendingIntent = openPendingIntent,
            stopPendingIntent = openPendingIntent
        )
        return NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID)
            .setSmallIcon(NotificationAppIconResolver.smallIconResId())
            .setLargeIcon(NotificationAppIconResolver.largeIconBitmap(context))
            .setContentTitle(payload.title(context))
            .setContentText(payload.content(context).ifBlank { " " })
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun buildForegroundNotificationResult(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        ongoing: Boolean,
        onlyAlertOnce: Boolean,
        secondaryActionMode: SecondaryActionMode = SecondaryActionMode.DEFAULT,
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID,
        primaryActionLabel: String? = null,
        secondaryActionLabelOverride: String? = null,
        showSecondaryActionWhenStopped: Boolean = false,
        outerGlow: Boolean = true,
        overrideTitle: String? = null,
        overrideContent: String? = null,
        overrideOnlineText: String? = null,
        overrideShortText: String? = null,
        overrideProgressPercent: Int? = null,
        overrideAccentColor: String? = null,
        miFocusTitle: String? = null,
        miFocusSpecialTitle: String? = null,
        miFocusContent: String? = null,
        deadlineAtMs: Long? = null,
        miFocusOrderId: String? = null,
        targetBaAccountId: String? = null,
        targetRoute: String? = null,
    ): SessionNotifier.NotificationBuildResult {
        val isBlueArchiveNotification =
            McpNotificationPayload.isBaNotificationServerName(serverName)
        val resolvedTargetBaAccountId =
            normalizeTargetBaAccountId(
                serverName = serverName,
                targetBaAccountId = targetBaAccountId,
            )
        val secondaryRequestCode = 110_200 + notificationId
        val openIntent =
            buildOpenIntent(
                context = context,
                serverName = serverName,
                targetBaAccountId = resolvedTargetBaAccountId,
                targetRoute = targetRoute,
            )
        val focusOpenPendingIntent = McpNotificationOpenIntents.create(
            context = context,
            notificationId = notificationId,
            intent = openIntent,
            entry = McpNotificationOpenEntry.Focus,
        )
        val openPendingIntent = McpNotificationOpenIntents.create(
            context = context,
            notificationId = notificationId,
            intent = openIntent,
            entry = McpNotificationOpenEntry.Body,
        )
        val (stopPendingIntent, resolvedSecondaryActionLabel) = if (secondaryActionMode == SecondaryActionMode.MARK_READ) {
            McpNotificationInteractionIntents.markReadPendingIntent(
                context = context,
                notificationId = notificationId,
                serverName = serverName,
                targetBaAccountId = resolvedTargetBaAccountId,
            ) to context.getString(
                if (isBlueArchiveNotification) {
                    R.string.common_mark_read
                } else {
                    R.string.common_acknowledge
                }
            )
        } else if (isBlueArchiveNotification) {
            val dismissIntent = Intent(context, McpKeepAliveService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            PendingIntent.getService(
                context,
                secondaryRequestCode,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to context.getString(R.string.common_mark_read)
        } else {
            val stopIntent = Intent().apply {
                setClassName(
                    context.packageName,
                    McpNotificationActionContract.NOTIFICATION_ACTION_RECEIVER_CLASS_NAME,
                )
                action = McpNotificationActionContract.ACTION_STOP_MCP_SERVER
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            PendingIntent.getBroadcast(
                context,
                secondaryRequestCode,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to context.getString(R.string.mcp_action_stop_service)
        }
        val deletePendingIntent =
            if (secondaryActionMode == SecondaryActionMode.MARK_READ) {
                McpNotificationInteractionIntents.dismissPendingIntent(
                    context = context,
                    notificationId = notificationId,
                    serverName = serverName,
                    targetBaAccountId = resolvedTargetBaAccountId,
                )
            } else {
                stopPendingIntent
            }
        val effectiveSecondaryActionLabel =
            secondaryActionLabelOverride?.takeIf { it.isNotBlank() } ?: resolvedSecondaryActionLabel
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = serverName,
                notificationId = notificationId,
                miFocusOrderId = miFocusOrderId,
            )

        val payload = McpNotificationPayload(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = onlyAlertOnce,
            openPendingIntent = openPendingIntent,
            stopPendingIntent = stopPendingIntent,
            deletePendingIntent = deletePendingIntent,
            focusOpenPendingIntent = focusOpenPendingIntent,
            primaryActionLabel = primaryActionLabel,
            secondaryActionLabel = effectiveSecondaryActionLabel,
            showSecondaryActionWhenStopped = showSecondaryActionWhenStopped,
            outerGlow = outerGlow,
            overrideTitle = overrideTitle,
            overrideContent = overrideContent,
            overrideOnlineText = overrideOnlineText,
            overrideShortText = overrideShortText,
            overrideProgressPercent = overrideProgressPercent,
            overrideAccentColor = overrideAccentColor,
            miFocusTitle = miFocusTitle,
            miFocusSpecialTitle = miFocusSpecialTitle,
            miFocusContent = miFocusContent,
            deadlineAtMs = deadlineAtMs,
            notificationId = notificationId,
            miFocusOrderId = resolvedMiFocusOrderId
        )
        val notifier = SessionNotifierImpl(
            NotificationHelper(
                context = context,
                channels = LiveNotificationChannels(
                    islandChannelId = CHANNEL_ID,
                    liveUpdateChannelId = LIVE_CHANNEL_ID
                )
            )
        )
        return notifier.build(payload)
    }

    fun notifyTest(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        val isBlueArchiveAp = McpNotificationPayload.isBaApServerName(serverName)
        val isBlueArchiveCafeAp = McpNotificationPayload.isBaCafeApServerName(serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.isBaCafeVisitServerName(serverName)
        val isBlueArchiveArenaRefresh =
            McpNotificationPayload.isBaArenaRefreshServerName(serverName)
        val isBlueArchiveNotification =
            isBlueArchiveAp || isBlueArchiveCafeAp || isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh
        val runningForNotification = if (isBlueArchiveNotification) running else true
        val baNotificationId = when {
            isBlueArchiveAp -> BA_AP_NOTIFICATION_ID
            isBlueArchiveCafeAp -> BA_CAFE_AP_NOTIFICATION_ID
            isBlueArchiveCafeVisit -> BA_CAFE_VISIT_NOTIFICATION_ID
            isBlueArchiveArenaRefresh -> BA_ARENA_REFRESH_NOTIFICATION_ID
            else -> TEST_NOTIFICATION_ID
        }
        notifyStandaloneEvent(
            context = context,
            notificationId = baNotificationId,
            serverName = serverName,
            running = runningForNotification,
            port = port,
            path = path,
            clients = clients,
            ongoing = runningForNotification,
            onlyAlertOnce = false
        )
    }

    fun notifyStandaloneEvent(
        context: Context,
        notificationId: Int,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        ongoing: Boolean = running,
        onlyAlertOnce: Boolean = false,
        primaryActionLabel: String? = null,
        secondaryActionLabel: String? = null,
        showSecondaryActionWhenStopped: Boolean = false,
        outerGlow: Boolean = true,
        overrideTitle: String? = null,
        overrideContent: String? = null,
        overrideOnlineText: String? = null,
        overrideShortText: String? = null,
        overrideProgressPercent: Int? = null,
        overrideAccentColor: String? = null,
        miFocusTitle: String? = null,
        miFocusSpecialTitle: String? = null,
        miFocusContent: String? = null,
        deadlineAtMs: Long? = null,
        miFocusOrderId: String? = null,
        targetBaAccountId: String? = null,
        targetRoute: String? = null,
    ): Boolean {
        val request =
            McpStandaloneEventRequest(
                serverName = serverName,
                running = running,
                port = port,
                path = path,
                clients = clients,
                ongoing = ongoing,
                onlyAlertOnce = onlyAlertOnce,
                primaryActionLabel = primaryActionLabel,
                secondaryActionLabel = secondaryActionLabel,
                showSecondaryActionWhenStopped = showSecondaryActionWhenStopped,
                outerGlow = outerGlow,
                overrideTitle = overrideTitle,
                overrideContent = overrideContent,
                overrideOnlineText = overrideOnlineText,
                overrideShortText = overrideShortText,
                overrideProgressPercent = overrideProgressPercent,
                overrideAccentColor = overrideAccentColor,
                miFocusTitle = miFocusTitle,
                miFocusSpecialTitle = miFocusSpecialTitle,
                miFocusContent = miFocusContent,
                deadlineAtMs = deadlineAtMs,
                miFocusOrderId = miFocusOrderId,
                targetBaAccountId = targetBaAccountId,
                targetRoute = targetRoute,
            )
        val prepared = prepareStandaloneEvent(
            context = context,
            notificationId = notificationId,
            request = request,
        )
        if (prepared.alreadyActive) return true
        val dispatched = notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = prepared.notification,
            useXiaomiMagic = prepared.useXiaomiMagic,
        )
        if (dispatched) {
            McpNotificationSnapshotStore.put(notificationId, prepared.snapshot)
        }
        return dispatched
    }

    suspend fun notifyStandaloneEventAwaitingDelivery(
        context: Context,
        notificationId: Int,
        request: McpStandaloneEventRequest,
        onDelivered: suspend () -> Unit = {},
    ): Boolean {
        val prepared = prepareStandaloneEvent(
            context = context,
            notificationId = notificationId,
            request = request,
        )
        if (prepared.alreadyActive) {
            withContext(NonCancellable) {
                runStandaloneEventDeliveryCommit {
                    McpNotificationSnapshotStore.put(notificationId, prepared.snapshot)
                    onDelivered()
                }
            }
            return true
        }
        val dispatched = dispatchStandaloneEventAwaitingDelivery(
            context = context,
            notificationId = notificationId,
            notification = prepared.notification,
            useXiaomiMagic = prepared.useXiaomiMagic,
            onDelivered = {
                McpNotificationSnapshotStore.put(notificationId, prepared.snapshot)
                onDelivered()
            },
        )
        return dispatched
    }

    private fun prepareStandaloneEvent(
        context: Context,
        notificationId: Int,
        request: McpStandaloneEventRequest,
    ): PreparedStandaloneEvent {
        ensureChannel(context)
        val resolvedTargetBaAccountId =
            normalizeTargetBaAccountId(
                serverName = request.serverName,
                targetBaAccountId = request.targetBaAccountId,
            )
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = request.serverName,
                notificationId = notificationId,
                miFocusOrderId = request.miFocusOrderId,
            )
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = request.serverName,
            running = request.running,
            port = request.port,
            path = request.path,
            clients = request.clients,
            ongoing = request.ongoing,
            onlyAlertOnce = request.onlyAlertOnce,
            secondaryActionMode = SecondaryActionMode.MARK_READ,
            notificationId = notificationId,
            primaryActionLabel = request.primaryActionLabel,
            secondaryActionLabelOverride = request.secondaryActionLabel,
            showSecondaryActionWhenStopped = request.showSecondaryActionWhenStopped,
            outerGlow = request.outerGlow,
            overrideTitle = request.overrideTitle,
            overrideContent = request.overrideContent,
            overrideOnlineText = request.overrideOnlineText,
            overrideShortText = request.overrideShortText,
            overrideProgressPercent = request.overrideProgressPercent,
            overrideAccentColor = request.overrideAccentColor,
            miFocusTitle = request.miFocusTitle,
            miFocusSpecialTitle = request.miFocusSpecialTitle,
            miFocusContent = request.miFocusContent,
            deadlineAtMs = request.deadlineAtMs,
            miFocusOrderId = resolvedMiFocusOrderId,
            targetBaAccountId = resolvedTargetBaAccountId,
            targetRoute = request.targetRoute,
        )
        val snapshot = McpNotificationSnapshot(
            serverName = request.serverName,
            running = request.running,
            port = request.port,
            path = request.path,
            clients = request.clients,
            ongoing = request.ongoing,
            onlyAlertOnce = request.onlyAlertOnce,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic,
            primaryActionLabel = request.primaryActionLabel,
            secondaryActionLabel = request.secondaryActionLabel,
            showSecondaryActionWhenStopped = request.showSecondaryActionWhenStopped,
            outerGlow = request.outerGlow,
            overrideTitle = request.overrideTitle,
            overrideContent = request.overrideContent,
            overrideOnlineText = request.overrideOnlineText,
            overrideShortText = request.overrideShortText,
            overrideProgressPercent = request.overrideProgressPercent,
            overrideAccentColor = request.overrideAccentColor,
            miFocusTitle = request.miFocusTitle,
            miFocusSpecialTitle = request.miFocusSpecialTitle,
            miFocusContent = request.miFocusContent,
            deadlineAtMs = request.deadlineAtMs,
            miFocusOrderId = resolvedMiFocusOrderId,
            targetBaAccountId = resolvedTargetBaAccountId,
            targetRoute = request.targetRoute,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        val alreadyActive =
            McpNotificationSnapshotStore.get(notificationId) == snapshot &&
                manager != null &&
                isNotificationActive(manager, notificationId)
        if (alreadyActive) {
            AppLogger.d(TAG) {
                "skip identical active notification id=$notificationId server=${request.serverName}"
            }
        }
        return PreparedStandaloneEvent(
            notification = buildResult.notification,
            snapshot = snapshot,
            useXiaomiMagic = buildResult.useXiaomiMagic,
            alreadyActive = alreadyActive,
        )
    }

    fun refreshStandaloneEventIfActive(
        context: Context,
        notificationId: Int,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        ongoing: Boolean = running,
        onlyAlertOnce: Boolean = true,
        primaryActionLabel: String? = null,
        secondaryActionLabel: String? = null,
        showSecondaryActionWhenStopped: Boolean = false,
        outerGlow: Boolean = true,
        overrideTitle: String? = null,
        overrideContent: String? = null,
        overrideOnlineText: String? = null,
        overrideShortText: String? = null,
        overrideProgressPercent: Int? = null,
        overrideAccentColor: String? = null,
        deadlineAtMs: Long? = null,
        miFocusOrderId: String? = null,
        targetBaAccountId: String? = null,
        targetRoute: String? = null,
    ): Boolean {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!isNotificationActive(manager, notificationId)) return false
        val resolvedTargetBaAccountId =
            normalizeTargetBaAccountId(
                serverName = serverName,
                targetBaAccountId = targetBaAccountId,
            )
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = serverName,
                notificationId = notificationId,
                miFocusOrderId = miFocusOrderId,
            )
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = onlyAlertOnce,
            secondaryActionMode = SecondaryActionMode.MARK_READ,
            notificationId = notificationId,
            primaryActionLabel = primaryActionLabel,
            secondaryActionLabelOverride = secondaryActionLabel,
            showSecondaryActionWhenStopped = showSecondaryActionWhenStopped,
            outerGlow = outerGlow,
            overrideTitle = overrideTitle,
            overrideContent = overrideContent,
            overrideOnlineText = overrideOnlineText,
            overrideShortText = overrideShortText,
            overrideProgressPercent = overrideProgressPercent,
            overrideAccentColor = overrideAccentColor,
            deadlineAtMs = deadlineAtMs,
            miFocusOrderId = resolvedMiFocusOrderId,
            targetBaAccountId = resolvedTargetBaAccountId,
            targetRoute = targetRoute,
        )
        val snapshot = McpNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = onlyAlertOnce,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic,
            primaryActionLabel = primaryActionLabel,
            secondaryActionLabel = secondaryActionLabel,
            showSecondaryActionWhenStopped = showSecondaryActionWhenStopped,
            outerGlow = outerGlow,
            overrideTitle = overrideTitle,
            overrideContent = overrideContent,
            overrideOnlineText = overrideOnlineText,
            overrideShortText = overrideShortText,
            overrideProgressPercent = overrideProgressPercent,
            overrideAccentColor = overrideAccentColor,
            deadlineAtMs = deadlineAtMs,
            miFocusOrderId = resolvedMiFocusOrderId,
            targetBaAccountId = resolvedTargetBaAccountId,
            targetRoute = targetRoute,
        )
        if (McpNotificationSnapshotStore.get(notificationId) == snapshot) return true
        val dispatched = notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        if (dispatched) {
            McpNotificationSnapshotStore.put(notificationId, snapshot)
        }
        return dispatched
    }

    fun refreshForegroundAsIsland(
        context: Context,
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        onlyAlertOnce: Boolean = true
    ) {
        ensureChannel(context)
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = serverName,
                notificationId = notificationId,
                miFocusOrderId = null,
            )
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = onlyAlertOnce,
            notificationId = notificationId,
            miFocusOrderId = resolvedMiFocusOrderId,
        )
        val snapshot = McpNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = onlyAlertOnce,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic,
            miFocusOrderId = resolvedMiFocusOrderId,
        )
        val dispatched = notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        if (dispatched) {
            McpNotificationSnapshotStore.put(notificationId, snapshot)
        }
    }

    fun refreshForegroundPulse(
        context: Context,
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = serverName,
                notificationId = notificationId,
                miFocusOrderId = null,
            )
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = true,
            notificationId = notificationId,
            miFocusOrderId = resolvedMiFocusOrderId,
        )
        val snapshot = McpNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = true,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic,
            miFocusOrderId = resolvedMiFocusOrderId,
        )
        val dispatched = notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        if (dispatched) {
            McpNotificationSnapshotStore.put(notificationId, snapshot)
        }
    }

    fun restoreXiaomiNetworkIfNeeded(context: Context) {
        McpXiaomiMagicDispatcher.restoreNetworkIfNeeded(context)
    }

    private fun refreshCachedNotificationIfActive(
        context: Context,
        manager: NotificationManager,
        notificationId: Int,
        snapshot: McpNotificationSnapshot?
    ) {
        if (snapshot == null) {
            McpNotificationSnapshotStore.clear(notificationId)
            McpNotificationActiveStateCache.clear(notificationId)
            return
        }
        if (!isNotificationActive(manager, notificationId)) {
            McpNotificationSnapshotStore.clear(notificationId)
            McpNotificationActiveStateCache.clear(notificationId)
            return
        }
        val resolvedMiFocusOrderId =
            resolveMiFocusOrderId(
                serverName = snapshot.serverName,
                notificationId = notificationId,
                miFocusOrderId = snapshot.miFocusOrderId,
            )
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = snapshot.serverName,
            running = snapshot.running,
            port = snapshot.port,
            path = snapshot.path,
            clients = snapshot.clients,
            ongoing = snapshot.ongoing,
            onlyAlertOnce = snapshot.onlyAlertOnce,
            secondaryActionMode = if (McpNotificationPayload.isBaNotificationServerName(snapshot.serverName)) {
                SecondaryActionMode.MARK_READ
            } else {
                secondaryActionModeFor(notificationId)
            },
            notificationId = notificationId,
            primaryActionLabel = snapshot.primaryActionLabel,
            secondaryActionLabelOverride = snapshot.secondaryActionLabel,
            showSecondaryActionWhenStopped = snapshot.showSecondaryActionWhenStopped,
            outerGlow = snapshot.outerGlow,
            overrideTitle = snapshot.overrideTitle,
            overrideContent = snapshot.overrideContent,
            overrideOnlineText = snapshot.overrideOnlineText,
            overrideShortText = snapshot.overrideShortText,
            overrideProgressPercent = snapshot.overrideProgressPercent,
            overrideAccentColor = snapshot.overrideAccentColor,
            deadlineAtMs = snapshot.deadlineAtMs,
            miFocusOrderId = resolvedMiFocusOrderId,
            targetBaAccountId = snapshot.targetBaAccountId,
            targetRoute = snapshot.targetRoute,
        )
        val nextSnapshot = snapshot.copy(
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic,
            miFocusOrderId = resolvedMiFocusOrderId,
        )
        if (snapshot == nextSnapshot) return
        val dispatched = notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        if (dispatched) {
            McpNotificationSnapshotStore.put(notificationId, nextSnapshot)
        }
    }

    private fun isNotificationActive(manager: NotificationManager, notificationId: Int): Boolean {
        return McpNotificationActiveStateCache.isActive(notificationId) {
            runCatching {
                manager.activeNotifications.any { it.id == notificationId }
            }.getOrDefault(false)
        }
    }

    private fun secondaryActionModeFor(notificationId: Int): SecondaryActionMode {
        return when (notificationId) {
            BA_AP_NOTIFICATION_ID,
            BA_CAFE_AP_NOTIFICATION_ID,
            BA_CAFE_VISIT_NOTIFICATION_ID,
            BA_ARENA_REFRESH_NOTIFICATION_ID -> SecondaryActionMode.MARK_READ

            else -> SecondaryActionMode.DEFAULT
        }
    }

    internal fun resolveMiFocusOrderId(
        serverName: String,
        notificationId: Int,
        miFocusOrderId: String?,
    ): String =
        miFocusOrderId?.trim()?.takeIf { it.isNotBlank() }
            ?: buildMiFocusOrderId(serverName, notificationId)

    private fun buildMiFocusOrderId(serverName: String, notificationId: Int): String {
        val normalizedServerName = serverName.trim().ifBlank { "keios" }
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "notification" }
        return "$normalizedServerName-$notificationId"
    }

    internal fun buildOpenIntent(
        context: Context,
        serverName: String,
        targetBaAccountId: String? = null,
        targetRoute: String? = null,
    ): Intent {
        val isBlueArchiveNotification =
            McpNotificationPayload.isBaNotificationServerName(serverName)
        val resolvedTargetBaAccountId =
            normalizeTargetBaAccountId(
                serverName = serverName,
                targetBaAccountId = targetBaAccountId,
            )
        return Intent().apply {
            setClassName(context.packageName, McpAppIntentContract.MAIN_ACTIVITY_CLASS_NAME)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(
                McpAppIntentContract.EXTRA_TARGET_BOTTOM_PAGE,
                if (isBlueArchiveNotification) {
                    McpAppIntentContract.TARGET_BOTTOM_PAGE_BA
                } else {
                    McpAppIntentContract.TARGET_BOTTOM_PAGE_MCP
                },
            )
            normalizeTargetRoute(targetRoute)?.let { route ->
                putExtra(McpAppIntentContract.EXTRA_TARGET_ROUTE, route)
            }
            resolvedTargetBaAccountId?.let { accountId ->
                putExtra(McpAppIntentContract.EXTRA_BA_ACCOUNT_ID, accountId)
            }
        }
    }

    private fun normalizeTargetBaAccountId(
        serverName: String,
        targetBaAccountId: String?,
    ): String? {
        if (!McpNotificationPayload.isBaNotificationServerName(serverName)) return null
        return targetBaAccountId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun normalizeTargetRoute(targetRoute: String?): String? =
        when (targetRoute?.trim()) {
            McpAppIntentContract.TARGET_ROUTE_WEBDAV_SYNC -> McpAppIntentContract.TARGET_ROUTE_WEBDAV_SYNC
            else -> null
        }

    private fun notifyWithResolvedDispatcher(
        context: Context,
        notificationId: Int,
        notification: Notification,
        useXiaomiMagic: Boolean
    ): Boolean {
        val dispatched = if (useXiaomiMagic) {
            McpXiaomiMagicDispatcher.enqueue(
                context = context,
                notificationId = notificationId,
                notification = notification
            )
        } else {
            val notificationManager = NotificationManagerCompat.from(context)
            restoreXiaomiNetworkIfNeeded(context)
            notifySafely(context, notificationManager, notificationId, notification)
        }
        if (dispatched) {
            McpNotificationActiveStateCache.markActive(notificationId, active = true)
        }
        AppLogger.i(TAG) {
            "dispatch result=$dispatched id=$notificationId xiaomiMagic=$useXiaomiMagic"
        }
        return dispatched
    }

    fun dispatchNotification(
        context: Context,
        notificationId: Int,
        notification: Notification,
        useXiaomiMagic: Boolean
    ): Boolean {
        return notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = notification,
            useXiaomiMagic = useXiaomiMagic
        )
    }

    fun cancelNotification(
        context: Context,
        notificationId: Int
    ) {
        invalidateNotificationRuntimeState(notificationId)
        restoreXiaomiNetworkIfNeeded(context)
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun invalidateNotificationRuntimeState(notificationId: Int) {
        McpNotificationSnapshotStore.clear(notificationId)
        McpNotificationActiveStateCache.clear(notificationId)
    }

    internal fun canPostNotifications(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    internal fun notifySafely(
        context: Context,
        notificationManager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification
    ): Boolean {
        if (!canPostNotifications(context)) return false
        return runCatching {
            notificationManager.notify(notificationId, notification)
            true
        }.getOrElse { throwable ->
            AppLogger.e(TAG, "Notification dispatch failed id=$notificationId", throwable)
            false
        }
    }
}
