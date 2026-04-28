package os.kei.mcp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import os.kei.MainActivity
import os.kei.R
import os.kei.core.intent.PendingIntentLaunchOptionsCompat
import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefs
import os.kei.core.system.ShizukuApiUtils
import os.kei.feature.notification.NotificationActionReceiver
import os.kei.mcp.service.McpKeepAliveService
import os.kei.mcp.domain.notification.SessionNotifier
import os.kei.mcp.framework.notification.NotificationHelper
import os.kei.mcp.framework.notification.SessionNotifierImpl
import os.kei.mcp.framework.notification.builder.NotificationRenderStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object McpNotificationHelper {
    private const val TAG = "McpNotifyHelper"

    const val CHANNEL_ID = "mcp_keepalive_channel_v2"
    const val LIVE_CHANNEL_ID = "mcp_live_update_channel_v1"
    const val FOREGROUND_SERVICE_CHANNEL_ID = "mcp_keepalive_service_channel_v1"
    private const val LEGACY_CHANNEL_ID = "mcp_keepalive_channel"
    const val KEEPALIVE_FOREGROUND_NOTIFICATION_ID = 38887
    const val KEEPALIVE_NOTIFICATION_ID = 38888
    const val BA_AP_NOTIFICATION_ID = 38889
    const val BA_CAFE_VISIT_NOTIFICATION_ID = 38890
    const val BA_ARENA_REFRESH_NOTIFICATION_ID = 38891
    private const val TEST_NOTIFICATION_ID = KEEPALIVE_NOTIFICATION_ID
    private const val ACTION_DISMISS = "os.kei.mcp.keepalive.DISMISS"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"
    private const val XMSF_PACKAGE_NAME = "com.xiaomi.xmsf"

    private enum class XiaomiMagicCommandSet {
        PACKAGE_NETWORKING,
        UID_FIREWALL,
        NONE
    }

    private val shizukuApiUtils = ShizukuApiUtils()
    private val magicScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val networkMutex = Mutex()
    @Volatile
    private var commandSet: XiaomiMagicCommandSet? = null
    @Volatile
    private var isXmsfNetworkBlocked = false
    @Volatile
    private var isUidFirewallChainEnabled = false
    @Volatile
    private var keepAliveSnapshot: CachedNotificationSnapshot? = null
    @Volatile
    private var baApSnapshot: CachedNotificationSnapshot? = null
    @Volatile
    private var baCafeVisitSnapshot: CachedNotificationSnapshot? = null
    @Volatile
    private var baArenaRefreshSnapshot: CachedNotificationSnapshot? = null

    private data class CachedNotificationSnapshot(
        val serverName: String,
        val running: Boolean,
        val port: Int,
        val path: String,
        val clients: Int,
        val ongoing: Boolean,
        val onlyAlertOnce: Boolean,
        val style: NotificationRenderStyle,
        val useXiaomiMagic: Boolean
    )

    private enum class SecondaryActionMode {
        DEFAULT,
        MARK_READ
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            val legacy = manager.getNotificationChannel(LEGACY_CHANNEL_ID)
            if (legacy != null && legacy.importance < NotificationManager.IMPORTANCE_HIGH) {
                manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            }
        }
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val keepalive = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.mcp_keepalive_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.mcp_keepalive_channel_desc)
                setShowBadge(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(keepalive)
        }
        if (manager.getNotificationChannel(FOREGROUND_SERVICE_CHANNEL_ID) == null) {
            val foregroundShell = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                context.getString(R.string.mcp_keepalive_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.mcp_keepalive_service_channel_desc)
                setShowBadge(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(foregroundShell)
        }
        if (manager.getNotificationChannel(LIVE_CHANNEL_ID) == null) {
            val liveUpdate = NotificationChannel(
                LIVE_CHANNEL_ID,
                context.getString(R.string.mcp_live_update_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.mcp_live_update_channel_desc)
                setShowBadge(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(liveUpdate)
        }
    }

    fun refreshCurrentNotificationStyle(context: Context) {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        refreshCachedNotificationIfActive(
            context = context,
            manager = manager,
            notificationId = KEEPALIVE_NOTIFICATION_ID,
            snapshot = keepAliveSnapshot
        )
        refreshCachedNotificationIfActive(
            context = context,
            manager = manager,
            notificationId = BA_AP_NOTIFICATION_ID,
            snapshot = baApSnapshot
        )
        refreshCachedNotificationIfActive(
            context = context,
            manager = manager,
            notificationId = BA_CAFE_VISIT_NOTIFICATION_ID,
            snapshot = baCafeVisitSnapshot
        )
        refreshCachedNotificationIfActive(
            context = context,
            manager = manager,
            notificationId = BA_ARENA_REFRESH_NOTIFICATION_ID,
            snapshot = baArenaRefreshSnapshot
        )
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
            notificationId = notificationId
        ).notification
    }

    fun buildForegroundBootstrapNotification(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID
    ): Notification {
        val isBlueArchiveNotification = McpNotificationPayload.isBaNotificationServerName(serverName)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(
                MainActivity.EXTRA_TARGET_BOTTOM_PAGE,
                if (isBlueArchiveNotification) {
                    MainActivity.TARGET_BOTTOM_PAGE_BA
                } else {
                    MainActivity.TARGET_BOTTOM_PAGE_MCP
                }
            )
        }
        val openPendingIntent = PendingIntentLaunchOptionsCompat.getUserVisibleActivity(
            context,
            310_100 + notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            .setSmallIcon(R.drawable.ic_kei_logo_live_update)
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
        notificationId: Int = KEEPALIVE_NOTIFICATION_ID
    ): SessionNotifier.NotificationBuildResult {
        val isBlueArchiveNotification = McpNotificationPayload.Companion.isBaNotificationServerName(serverName)
        val openRequestCode = 110_100 + notificationId
        val secondaryRequestCode = 110_200 + notificationId
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(
                MainActivity.EXTRA_TARGET_BOTTOM_PAGE,
                if (isBlueArchiveNotification) {
                    MainActivity.TARGET_BOTTOM_PAGE_BA
                } else {
                    MainActivity.TARGET_BOTTOM_PAGE_MCP
                }
            )
        }
        val openPendingIntent = PendingIntentLaunchOptionsCompat.getUserVisibleActivity(
            context,
            openRequestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (stopPendingIntent, secondaryActionLabel) = if (secondaryActionMode == SecondaryActionMode.MARK_READ) {
            val readIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_READ
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            PendingIntent.getBroadcast(
                context,
                210_200 + notificationId,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_STOP_MCP_SERVER
            }
            PendingIntent.getBroadcast(
                context,
                secondaryRequestCode,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to context.getString(R.string.mcp_action_stop_service)
        }

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
            secondaryActionLabel = secondaryActionLabel
        )
        val notifier = SessionNotifierImpl(NotificationHelper(context))
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
        val isBlueArchiveAp = McpNotificationPayload.Companion.isBaApServerName(serverName)
        val isBlueArchiveCafeVisit = McpNotificationPayload.Companion.isBaCafeVisitServerName(serverName)
        val isBlueArchiveArenaRefresh = McpNotificationPayload.Companion.isBaArenaRefreshServerName(serverName)
        val isBlueArchiveNotification = isBlueArchiveAp || isBlueArchiveCafeVisit || isBlueArchiveArenaRefresh
        val runningForNotification = if (isBlueArchiveNotification) running else true
        val baNotificationId = when {
            isBlueArchiveAp -> BA_AP_NOTIFICATION_ID
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
        onlyAlertOnce: Boolean = false
    ) {
        ensureChannel(context)
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
            notificationId = notificationId
        )
        val snapshot = CachedNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = onlyAlertOnce,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        cacheNotificationSnapshot(notificationId, snapshot)
        notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
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
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = onlyAlertOnce,
            notificationId = notificationId
        )
        val snapshot = CachedNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = onlyAlertOnce,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        cacheNotificationSnapshot(notificationId, snapshot)
        notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
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
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = true,
            notificationId = notificationId
        )
        val snapshot = CachedNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = true,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        cacheNotificationSnapshot(notificationId, snapshot)
        notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
    }

    fun restoreXiaomiNetworkIfNeeded(context: Context) {
        val xmsfUid = resolveXmsfUid(context)
        magicScope.launch {
            networkMutex.withLock {
                if (xmsfUid != null && shizukuApiUtils.canUseCommand()) {
                    healXmsfNetworkingLocked(xmsfUid)
                }
            }
        }
    }

    private fun refreshCachedNotificationIfActive(
        context: Context,
        manager: NotificationManager,
        notificationId: Int,
        snapshot: CachedNotificationSnapshot?
    ) {
        if (snapshot == null || !isNotificationActive(manager, notificationId)) return
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = snapshot.serverName,
            running = snapshot.running,
            port = snapshot.port,
            path = snapshot.path,
            clients = snapshot.clients,
            ongoing = snapshot.ongoing,
            onlyAlertOnce = snapshot.onlyAlertOnce,
            notificationId = notificationId
        )
        cacheNotificationSnapshot(
            notificationId = notificationId,
            snapshot = snapshot.copy(
                style = buildResult.style,
                useXiaomiMagic = buildResult.useXiaomiMagic
            )
        )
        notifyWithResolvedDispatcher(
            context = context,
            notificationId = notificationId,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
    }

    private fun isNotificationActive(manager: NotificationManager, notificationId: Int): Boolean {
        return runCatching {
            manager.activeNotifications.any { it.id == notificationId }
        }.getOrDefault(false)
    }

    private fun cacheNotificationSnapshot(
        notificationId: Int,
        snapshot: CachedNotificationSnapshot
    ) {
        when (notificationId) {
            KEEPALIVE_NOTIFICATION_ID -> keepAliveSnapshot = snapshot
            BA_AP_NOTIFICATION_ID -> baApSnapshot = snapshot
            BA_CAFE_VISIT_NOTIFICATION_ID -> baCafeVisitSnapshot = snapshot
            BA_ARENA_REFRESH_NOTIFICATION_ID -> baArenaRefreshSnapshot = snapshot
        }
    }

    private fun clearNotificationSnapshot(notificationId: Int) {
        when (notificationId) {
            KEEPALIVE_NOTIFICATION_ID -> keepAliveSnapshot = null
            BA_AP_NOTIFICATION_ID -> baApSnapshot = null
            BA_CAFE_VISIT_NOTIFICATION_ID -> baCafeVisitSnapshot = null
            BA_ARENA_REFRESH_NOTIFICATION_ID -> baArenaRefreshSnapshot = null
        }
    }

    private fun notifyWithResolvedDispatcher(
        context: Context,
        notificationId: Int,
        notification: Notification,
        useXiaomiMagic: Boolean
    ) {
        if (useXiaomiMagic) {
            notifyWithXiaomiMagic(
                context = context,
                notificationId = notificationId,
                notification = notification
            )
            return
        }
        val notificationManager = NotificationManagerCompat.from(context)
        if (shizukuApiUtils.canUseCommand()) {
            restoreXiaomiNetworkIfNeeded(context)
        }
        notificationManager.notify(notificationId, notification)
    }

    fun dispatchNotification(
        context: Context,
        notificationId: Int,
        notification: Notification,
        useXiaomiMagic: Boolean
    ) {
        notifyWithResolvedDispatcher(
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
        clearNotificationSnapshot(notificationId)
        if (shizukuApiUtils.canUseCommand()) {
            restoreXiaomiNetworkIfNeeded(context)
        }
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun notifyWithXiaomiMagic(
        context: Context,
        notificationId: Int,
        notification: Notification
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        val targetUid = resolveXmsfUid(context)
        AppLogger.i(TAG, "notifyWithXiaomiMagic: targetUid=$targetUid notifId=$notificationId")
        if (!shouldExecuteXiaomiMagic(targetUid)) {
            AppLogger.w(TAG, "skip Xiaomi magic: preconditions not satisfied")
            if (shizukuApiUtils.canUseCommand()) {
                restoreXiaomiNetworkIfNeeded(context)
            }
            notificationManager.notify(notificationId, notification)
            return
        }
        val nonNullUid = targetUid ?: run {
            AppLogger.w(TAG, "skip Xiaomi magic: xmsf uid is null")
            notificationManager.notify(notificationId, notification)
            return
        }

        magicScope.launch {
            networkMutex.withLock {
                var notificationDispatched = false
                var networkTouched = false
                try {
                    healXmsfNetworkingLocked(nonNullUid)
                    AppLogger.i(TAG, "blocking xmsf network for uid=$nonNullUid")
                    blockXmsfNetworkingLocked(nonNullUid)
                    networkTouched = isXmsfNetworkBlocked || isUidFirewallChainEnabled
                    notificationManager.notify(notificationId, notification)
                    notificationDispatched = true
                    delay(resolveXiaomiMagicBlockIntervalMs())
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    AppLogger.e(TAG, "Xiaomi magic execution failed", throwable)
                    if (!notificationDispatched) {
                        runCatching {
                            notificationManager.notify(notificationId, notification)
                        }.onFailure {
                            AppLogger.e(TAG, "Fallback notification dispatch failed", it)
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        if (networkTouched || isXmsfNetworkBlocked || isUidFirewallChainEnabled) {
                            AppLogger.i(TAG, "restoring xmsf network for uid=$nonNullUid")
                            runCatching {
                                restoreXmsfNetworkingLocked(nonNullUid)
                            }.onFailure {
                                AppLogger.e(TAG, "Xiaomi magic network restoration failed", it)
                            }
                            runCatching {
                                healXmsfNetworkingLocked(nonNullUid)
                            }.onFailure {
                                AppLogger.e(TAG, "Xiaomi magic network healing failed", it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resolveXmsfUid(context: Context): Int? {
        return runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageUid(XMSF_PACKAGE_NAME, 0)
        }.getOrNull()?.takeIf { it > 0 }
    }

    private fun shouldExecuteXiaomiMagic(xmsfUid: Int?): Boolean {
        if (xmsfUid == null) {
            AppLogger.w(TAG, "shouldExecuteXiaomiMagic=false: xmsf uid not found")
            return false
        }
        if (!shizukuApiUtils.canUseCommand()) {
            AppLogger.w(TAG, "shouldExecuteXiaomiMagic=false: Shizuku command unavailable")
            return false
        }
        val idOutput = shizukuApiUtils.execCommand("id").orEmpty()
        val isShellOrRoot = idOutput.contains("uid=2000") || idOutput.contains("uid=0")
        if (!isShellOrRoot) {
            AppLogger.w(TAG, "shouldExecuteXiaomiMagic=false: unsupported Shizuku identity '$idOutput'")
            return false
        }
        val mode = resolveMagicCommandSet()
        val canUseMode = mode != XiaomiMagicCommandSet.NONE
        AppLogger.i(TAG, "Shizuku id='$idOutput', mode=$mode, allowMagic=$canUseMode")
        return canUseMode
    }

    private fun blockXmsfNetworkingLocked(uid: Int) {
        when (resolveMagicCommandSet()) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                val blocked = execMagicCommand("cmd connectivity set-package-networking-enabled false $XMSF_PACKAGE_NAME")
                isXmsfNetworkBlocked = blocked
                AppLogger.i(
                    TAG,
                    "blockXmsfNetworkingLocked(package): uid=$uid blocked=$blocked"
                )
            }

            XiaomiMagicCommandSet.UID_FIREWALL -> {
                val chainEnabled = execMagicCommand("cmd connectivity set-firewall-chain-enabled 9 true")
                val blocked = execMagicCommand("cmd connectivity set-uid-firewall-rule 9 $uid 2")
                isXmsfNetworkBlocked = blocked
                isUidFirewallChainEnabled = chainEnabled
                AppLogger.i(
                    TAG,
                    "blockXmsfNetworkingLocked(uid): uid=$uid chainEnabled=$chainEnabled blocked=$blocked"
                )
            }

            XiaomiMagicCommandSet.NONE -> {
                isXmsfNetworkBlocked = false
                isUidFirewallChainEnabled = false
                AppLogger.w(TAG, "blockXmsfNetworkingLocked skipped: no supported connectivity command")
            }
        }
    }

    private fun restoreXmsfNetworkingLocked(uid: Int) {
        val mode = resolveMagicCommandSet()
        val restored = when (mode) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                if (isXmsfNetworkBlocked) {
                    execMagicCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
                } else {
                    true
                }
            }

            XiaomiMagicCommandSet.UID_FIREWALL -> {
                val ruleRestored = if (isXmsfNetworkBlocked) {
                    execMagicCommand("cmd connectivity set-uid-firewall-rule 9 $uid 0")
                } else {
                    true
                }
                ruleRestored
            }

            XiaomiMagicCommandSet.NONE -> false
        }
        AppLogger.i(TAG, "restoreXmsfNetworkingLocked: uid=$uid mode=$mode restored=$restored")
        isXmsfNetworkBlocked = false
        isUidFirewallChainEnabled = false
    }

    private fun healXmsfNetworkingLocked(uid: Int) {
        when (resolveMagicCommandSet()) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                val restored = execMagicCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
                AppLogger.i(TAG, "healXmsfNetworkingLocked(package): uid=$uid restored=$restored")
            }

            XiaomiMagicCommandSet.UID_FIREWALL -> {
                val ruleRestored = execMagicCommand("cmd connectivity set-uid-firewall-rule 9 $uid 0")
                AppLogger.i(TAG, "healXmsfNetworkingLocked(uid): uid=$uid ruleRestored=$ruleRestored")
            }

            XiaomiMagicCommandSet.NONE -> {
                AppLogger.w(TAG, "healXmsfNetworkingLocked skipped: no supported connectivity command")
            }
        }
        isXmsfNetworkBlocked = false
        isUidFirewallChainEnabled = false
    }

    private fun resolveMagicCommandSet(): XiaomiMagicCommandSet {
        commandSet?.let { return it }
        val helpText = shizukuApiUtils.execCommand("cmd connectivity help")
        if (helpText.isNullOrBlank()) {
            AppLogger.w(TAG, "resolveMagicCommandSet skipped: connectivity help unavailable")
            return XiaomiMagicCommandSet.NONE
        }
        val resolved = when {
            helpText.contains("set-package-networking-enabled") -> XiaomiMagicCommandSet.PACKAGE_NETWORKING
            helpText.contains("set-firewall-chain-enabled") &&
                helpText.contains("set-uid-firewall-rule") -> XiaomiMagicCommandSet.UID_FIREWALL
            else -> XiaomiMagicCommandSet.NONE
        }
        commandSet = resolved
        AppLogger.i(TAG, "resolved Xiaomi magic command set: $resolved")
        return resolved
    }

    private fun execMagicCommand(command: String): Boolean {
        val output = shizukuApiUtils.execCommand("($command) >/dev/null 2>&1 && echo __OK__ || echo __FAIL__")
            ?: return false
        val success = output.contains("__OK__")
        if (!success) {
            AppLogger.w(TAG, "magic command failed: $command; output=$output")
        }
        return success
    }

    private fun resolveXiaomiMagicBlockIntervalMs(): Long {
        return UiPrefs.getSuperIslandRestoreDelayMs().toLong()
    }
}
