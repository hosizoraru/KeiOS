package com.example.keios.mcp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.keios.MainActivity
import com.example.keios.R
import com.example.keios.core.log.AppLogger
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.feature.notification.NotificationActionReceiver
import com.example.keios.mcp.domain.notification.SessionNotifier
import com.example.keios.mcp.framework.notification.NotificationHelper
import com.example.keios.mcp.framework.notification.SessionNotifierImpl
import com.example.keios.mcp.framework.notification.builder.NotificationRenderStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object McpNotificationHelper {
    private const val TAG = "McpNotifyHelper"

    const val CHANNEL_ID = "mcp_keepalive_channel_v2"
    const val LIVE_CHANNEL_ID = "mcp_live_update_channel_v1"
    private const val LEGACY_CHANNEL_ID = "mcp_keepalive_channel"
    const val KEEPALIVE_NOTIFICATION_ID = 38888
    const val BA_AP_NOTIFICATION_ID = 38889
    private const val TEST_NOTIFICATION_ID = KEEPALIVE_NOTIFICATION_ID
    private const val ACTION_DISMISS = "com.example.keios.mcp.keepalive.DISMISS"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"
    private const val XMSF_PACKAGE_NAME = "com.xiaomi.xmsf"
    private const val XIAOMI_MAGIC_BLOCK_INTERVAL_MS = 100L

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
    private var isPackageChainEnabled = false
    @Volatile
    private var isUidFirewallChainEnabled = false
    @Volatile
    private var keepAliveSnapshot: CachedNotificationSnapshot? = null
    @Volatile
    private var baApSnapshot: CachedNotificationSnapshot? = null

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
    }

    fun buildForegroundNotification(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        ongoing: Boolean,
        onlyAlertOnce: Boolean = true
    ): android.app.Notification {
        return buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = onlyAlertOnce
        ).notification
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
        val isBlueArchiveAp = serverName.trim() == "BlueArchive AP"
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(
                MainActivity.EXTRA_TARGET_BOTTOM_PAGE,
                if (isBlueArchiveAp) {
                    MainActivity.TARGET_BOTTOM_PAGE_BA
                } else {
                    MainActivity.TARGET_BOTTOM_PAGE_MCP
                }
            )
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (stopPendingIntent, secondaryActionLabel) = if (isBlueArchiveAp) {
            val dismissIntent = Intent(context, McpKeepAliveService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_NOTIFICATION_ID, BA_AP_NOTIFICATION_ID)
            }
            PendingIntent.getService(
                context,
                1102,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to context.getString(R.string.common_mark_read)
        } else if (secondaryActionMode == SecondaryActionMode.MARK_READ) {
            val readIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_READ
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            PendingIntent.getBroadcast(
                context,
                2102,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to context.getString(R.string.common_acknowledge)
        } else {
            val toggleIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_MCP)
                putExtra(MainActivity.EXTRA_MCP_SERVER_ACTION, MainActivity.MCP_SERVER_ACTION_TOGGLE)
            }
            PendingIntent.getActivity(
                context,
                1102,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) to context.getString(R.string.mcp_action_toggle_service)
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
        val isBlueArchiveAp = serverName.trim() == "BlueArchive AP"
        val runningForNotification = if (isBlueArchiveAp) running else true
        if (isBlueArchiveAp) {
            runCatching {
                McpKeepAliveService.startOrUpdate(
                    context = context,
                    serverName = serverName,
                    running = running,
                    port = port,
                    path = path,
                    clients = clients,
                    forceStart = true,
                    notificationId = BA_AP_NOTIFICATION_ID,
                    heartbeatEnabled = false
                )
            }.onSuccess {
                return
            }.onFailure {
                AppLogger.w(TAG, "AP notifyTest fallback to direct notify: ${it.message ?: it.javaClass.simpleName}")
            }
        }
        ensureChannel(context)
        val buildResult = buildForegroundNotificationResult(
            context = context,
            serverName = serverName,
            running = runningForNotification,
            port = port,
            path = path,
            clients = clients,
            ongoing = runningForNotification,
            onlyAlertOnce = false,
            secondaryActionMode = SecondaryActionMode.MARK_READ,
            notificationId = if (isBlueArchiveAp) BA_AP_NOTIFICATION_ID else TEST_NOTIFICATION_ID
        )
        val snapshot = CachedNotificationSnapshot(
            serverName = serverName,
            running = runningForNotification,
            port = port,
            path = path,
            clients = clients,
            ongoing = runningForNotification,
            onlyAlertOnce = false,
            style = buildResult.style,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
        val notificationId = if (isBlueArchiveAp) {
            BA_AP_NOTIFICATION_ID
        } else {
            TEST_NOTIFICATION_ID
        }
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
            onlyAlertOnce = false
        )
        val snapshot = CachedNotificationSnapshot(
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = false,
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
            onlyAlertOnce = true
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
        val selfPackage = context.packageName
        magicScope.launch {
            networkMutex.withLock {
                healConnectivityStateLocked(selfPackage, xmsfUid)
                if (xmsfUid != null && isXmsfNetworkBlocked) {
                    restoreXmsfNetworkingLocked(xmsfUid, selfPackage)
                }
                healConnectivityStateLocked(selfPackage, xmsfUid)
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
        }
    }

    private fun notifyWithResolvedDispatcher(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification,
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
            val selfPackage = context.packageName
            val targetUid = resolveXmsfUid(context)
            magicScope.launch {
                networkMutex.withLock {
                    healConnectivityStateLocked(selfPackage, targetUid)
                }
            }
        }
        notificationManager.notify(notificationId, notification)
    }

    private fun notifyWithXiaomiMagic(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        val targetUid = resolveXmsfUid(context)
        val selfPackage = context.packageName
        AppLogger.i(TAG, "notifyWithXiaomiMagic: targetUid=$targetUid notifId=$notificationId")
        if (!shouldExecuteXiaomiMagic(targetUid)) {
            AppLogger.w(TAG, "skip Xiaomi magic: preconditions not satisfied")
            if (shizukuApiUtils.canUseCommand()) {
                magicScope.launch {
                    networkMutex.withLock {
                        healConnectivityStateLocked(selfPackage, targetUid)
                    }
                }
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
                try {
                    healConnectivityStateLocked(selfPackage, nonNullUid)
                    AppLogger.i(TAG, "blocking xmsf network for uid=$nonNullUid")
                    blockXmsfNetworkingLocked(nonNullUid, selfPackage)
                    notificationManager.notify(notificationId, notification)
                    delay(XIAOMI_MAGIC_BLOCK_INTERVAL_MS)
                } finally {
                    AppLogger.i(TAG, "restoring xmsf network for uid=$nonNullUid")
                    restoreXmsfNetworkingLocked(nonNullUid, selfPackage)
                    healConnectivityStateLocked(selfPackage, nonNullUid)
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
        val mode = resolveMagicCommandSet()
        val canUseMode = mode != XiaomiMagicCommandSet.NONE
        AppLogger.i(TAG, "Shizuku id='$idOutput', mode=$mode, allowMagic=${isShellOrRoot && canUseMode}")
        return isShellOrRoot && canUseMode
    }

    private fun blockXmsfNetworkingLocked(uid: Int, selfPackage: String) {
        when (resolveMagicCommandSet()) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                val blocked = execMagicCommand("cmd connectivity set-package-networking-enabled false $XMSF_PACKAGE_NAME")
                execMagicCommand("cmd connectivity set-package-networking-enabled true $selfPackage")
                isXmsfNetworkBlocked = blocked
                isPackageChainEnabled = false
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
                isPackageChainEnabled = false
                isUidFirewallChainEnabled = false
                AppLogger.w(TAG, "blockXmsfNetworkingLocked skipped: no supported connectivity command")
            }
        }
    }

    private fun restoreXmsfNetworkingLocked(uid: Int, selfPackage: String) {
        val mode = resolveMagicCommandSet()
        val restored = when (mode) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                val packageRestored = if (isXmsfNetworkBlocked) {
                    execMagicCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
                } else {
                    true
                }
                val selfRestored = execMagicCommand("cmd connectivity set-package-networking-enabled true $selfPackage")
                packageRestored && selfRestored
            }

            XiaomiMagicCommandSet.UID_FIREWALL -> {
                val ruleRestored = if (isXmsfNetworkBlocked) {
                    execMagicCommand("cmd connectivity set-uid-firewall-rule 9 $uid 0")
                } else {
                    true
                }
                val chainDisabled = if (isUidFirewallChainEnabled) {
                    execMagicCommand("cmd connectivity set-firewall-chain-enabled 9 false")
                } else {
                    true
                }
                ruleRestored && chainDisabled
            }

            XiaomiMagicCommandSet.NONE -> false
        }
        AppLogger.i(TAG, "restoreXmsfNetworkingLocked: uid=$uid mode=$mode restored=$restored")
        isXmsfNetworkBlocked = false
        isPackageChainEnabled = false
        isUidFirewallChainEnabled = false
    }

    private fun healConnectivityStateLocked(selfPackage: String, xmsfUid: Int?) {
        when (resolveMagicCommandSet()) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                execMagicCommand("cmd connectivity set-package-networking-enabled true $selfPackage")
                execMagicCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
            }

            XiaomiMagicCommandSet.UID_FIREWALL -> {
                if (xmsfUid != null) {
                    execMagicCommand("cmd connectivity set-uid-firewall-rule 9 $xmsfUid 0")
                }
                execMagicCommand("cmd connectivity set-firewall-chain-enabled 9 false")
            }

            XiaomiMagicCommandSet.NONE -> {
                // no-op
            }
        }
    }

    private fun resolveMagicCommandSet(): XiaomiMagicCommandSet {
        commandSet?.let { return it }
        val helpText = shizukuApiUtils.execCommand("cmd connectivity help").orEmpty()
        val resolved = when {
            helpText.contains("set-package-networking-enabled") -> XiaomiMagicCommandSet.PACKAGE_NETWORKING
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
}
