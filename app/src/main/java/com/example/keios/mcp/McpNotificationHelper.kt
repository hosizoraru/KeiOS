package com.example.keios.mcp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.keios.MainActivity
import com.example.keios.R
import com.example.keios.ui.utils.ShizukuApiUtils
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
    private const val LEGACY_CHANNEL_ID = "mcp_keepalive_channel"
    const val KEEPALIVE_NOTIFICATION_ID = 38888
    private const val BA_AP_NOTIFICATION_ID = 38889
    private const val TEST_NOTIFICATION_ID = KEEPALIVE_NOTIFICATION_ID
    private const val ACTION_STOP = "com.example.keios.mcp.keepalive.STOP"
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
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val isBlueArchiveAp = serverName.trim() == "BlueArchive AP"
        val actionForSecondaryButton = if (isBlueArchiveAp) ACTION_DISMISS else ACTION_STOP
        val stopIntent = Intent(context, McpKeepAliveService::class.java).apply {
            action = actionForSecondaryButton
            if (isBlueArchiveAp) {
                putExtra(EXTRA_NOTIFICATION_ID, BA_AP_NOTIFICATION_ID)
            }
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            stopPendingIntent = stopPendingIntent
        )
        return McpIslandNotificationBuilder.build(context, payload)
    }

    fun notifyTest(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val notification = buildForegroundNotification(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = running,
            onlyAlertOnce = false
        )
        val notificationId = if (serverName.trim() == "BlueArchive AP") {
            BA_AP_NOTIFICATION_ID
        } else {
            TEST_NOTIFICATION_ID
        }
        notifyWithXiaomiMagic(
            context = context,
            notificationId = notificationId,
            notification = notification
        )
    }

    fun refreshForegroundAsIsland(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val notification = buildForegroundNotification(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = false
        )
        notifyWithXiaomiMagic(
            context = context,
            notificationId = KEEPALIVE_NOTIFICATION_ID,
            notification = notification
        )
    }

    fun refreshForegroundPulse(
        context: Context,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val notification = buildForegroundNotification(
            context = context,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = true
        )
        notifyWithXiaomiMagic(
            context = context,
            notificationId = KEEPALIVE_NOTIFICATION_ID,
            notification = notification
        )
    }

    fun restoreXiaomiNetworkIfNeeded(context: Context) {
        val xmsfUid = resolveXmsfUid(context) ?: return
        if (!isXmsfNetworkBlocked) return
        magicScope.launch {
            networkMutex.withLock {
                restoreXmsfNetworkingLocked(xmsfUid)
            }
        }
    }

    fun cleanupXiaomiMagic(context: Context) = restoreXiaomiNetworkIfNeeded(context)

    private fun notifyWithXiaomiMagic(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        val targetUid = resolveXmsfUid(context)
        Log.i(TAG, "notifyWithXiaomiMagic: targetUid=$targetUid notifId=$notificationId")
        if (!shouldExecuteXiaomiMagic(targetUid)) {
            Log.w(TAG, "skip Xiaomi magic: preconditions not satisfied")
            notificationManager.notify(notificationId, notification)
            return
        }
        val nonNullUid = targetUid ?: run {
            Log.w(TAG, "skip Xiaomi magic: xmsf uid is null")
            notificationManager.notify(notificationId, notification)
            return
        }

        magicScope.launch {
            networkMutex.withLock {
                try {
                    Log.i(TAG, "blocking xmsf network for uid=$nonNullUid")
                    blockXmsfNetworkingLocked(nonNullUid)
                    notificationManager.notify(notificationId, notification)
                    delay(XIAOMI_MAGIC_BLOCK_INTERVAL_MS)
                } finally {
                    Log.i(TAG, "restoring xmsf network for uid=$nonNullUid")
                    restoreXmsfNetworkingLocked(nonNullUid)
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
            Log.w(TAG, "shouldExecuteXiaomiMagic=false: xmsf uid not found")
            return false
        }
        if (!shizukuApiUtils.canUseCommand()) {
            Log.w(TAG, "shouldExecuteXiaomiMagic=false: Shizuku command unavailable")
            return false
        }
        val idOutput = shizukuApiUtils.execCommand("id").orEmpty()
        val isShellOrRoot = idOutput.contains("uid=2000") || idOutput.contains("uid=0")
        val mode = resolveMagicCommandSet()
        val canUseMode = mode != XiaomiMagicCommandSet.NONE
        Log.i(TAG, "Shizuku id='$idOutput', mode=$mode, allowMagic=${isShellOrRoot && canUseMode}")
        return isShellOrRoot && canUseMode
    }

    private fun blockXmsfNetworkingLocked(uid: Int) {
        when (resolveMagicCommandSet()) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                val chainEnabled = execMagicCommand("cmd connectivity set-chain3-enabled true")
                val blocked = execMagicCommand("cmd connectivity set-package-networking-enabled false $XMSF_PACKAGE_NAME")
                isXmsfNetworkBlocked = blocked
                isPackageChainEnabled = chainEnabled
                Log.i(
                    TAG,
                    "blockXmsfNetworkingLocked(package): uid=$uid chainEnabled=$chainEnabled blocked=$blocked"
                )
            }

            XiaomiMagicCommandSet.UID_FIREWALL -> {
                val chainEnabled = execMagicCommand("cmd connectivity set-firewall-chain-enabled 9 true")
                val blocked = execMagicCommand("cmd connectivity set-uid-firewall-rule 9 $uid 2")
                isXmsfNetworkBlocked = blocked
                isUidFirewallChainEnabled = chainEnabled
                Log.i(
                    TAG,
                    "blockXmsfNetworkingLocked(uid): uid=$uid chainEnabled=$chainEnabled blocked=$blocked"
                )
            }

            XiaomiMagicCommandSet.NONE -> {
                isXmsfNetworkBlocked = false
                isPackageChainEnabled = false
                isUidFirewallChainEnabled = false
                Log.w(TAG, "blockXmsfNetworkingLocked skipped: no supported connectivity command")
            }
        }
    }

    private fun restoreXmsfNetworkingLocked(uid: Int) {
        val mode = resolveMagicCommandSet()
        val restored = when (mode) {
            XiaomiMagicCommandSet.PACKAGE_NETWORKING -> {
                val packageRestored = if (isXmsfNetworkBlocked) {
                    execMagicCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
                } else {
                    true
                }
                val chainDisabled = if (isPackageChainEnabled) {
                    execMagicCommand("cmd connectivity set-chain3-enabled false")
                } else {
                    true
                }
                packageRestored && chainDisabled
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
        Log.i(TAG, "restoreXmsfNetworkingLocked: uid=$uid mode=$mode restored=$restored")
        isXmsfNetworkBlocked = false
        isPackageChainEnabled = false
        isUidFirewallChainEnabled = false
    }

    private fun resolveMagicCommandSet(): XiaomiMagicCommandSet {
        commandSet?.let { return it }
        val helpText = shizukuApiUtils.execCommand("cmd connectivity help").orEmpty()
        val resolved = when {
            helpText.contains("set-package-networking-enabled") -> XiaomiMagicCommandSet.PACKAGE_NETWORKING
            helpText.contains("set-uid-firewall-rule") || helpText.contains("set-firewall-chain-enabled") -> XiaomiMagicCommandSet.UID_FIREWALL
            else -> XiaomiMagicCommandSet.NONE
        }
        commandSet = resolved
        Log.i(TAG, "resolved Xiaomi magic command set: $resolved")
        return resolved
    }

    private fun execMagicCommand(command: String): Boolean {
        val output = shizukuApiUtils.execCommand("($command) >/dev/null 2>&1 && echo __OK__ || echo __FAIL__")
            ?: return false
        val success = output.contains("__OK__")
        if (!success) {
            Log.w(TAG, "magic command failed: $command; output=$output")
        }
        return success
    }
}
