package os.kei.mcp.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.notification.McpNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class McpKeepAliveService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private var heartbeatJob: Job? = null
    private var islandRefreshJob: Job? = null
    private var isForegroundPromoted: Boolean = false
    private var currentRunning: Boolean = false
    private var currentPort: Int = 38888
    private var currentPath: String = "/mcp"
    private var currentServerName: String = "KeiOS MCP"
    private var currentClients: Int = 0
    private var currentNotificationId: Int = McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID
    private var currentHeartbeatEnabled: Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        McpNotificationHelper.ensureChannel(this)
        when (intent?.action) {
            ACTION_DISMISS -> {
                val requestedNotificationId = intent.getIntExtra(
                    EXTRA_NOTIFICATION_ID,
                    McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID
                )
                val resolvedNotificationId = if (
                    requestedNotificationId == McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID &&
                    currentNotificationId != McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID
                ) {
                    currentNotificationId
                } else {
                    requestedNotificationId
                }
                McpNotificationHelper.cancelNotification(this, resolvedNotificationId)
                if (!currentRunning) {
                    stopHeartbeat()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_STOP -> {
                stopIslandRefresh()
                stopHeartbeat()
                McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this)
                cancelCurrentIslandNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundPromoted = false
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START,
            ACTION_UPDATE,
            null -> {
                val running = intent?.getBooleanExtra(EXTRA_RUNNING, false) == true
                val port = intent?.getIntExtra(EXTRA_PORT, 38888) ?: 38888
                val path = intent?.getStringExtra(EXTRA_PATH).orEmpty().ifBlank { "/mcp" }
                val serverName = intent?.getStringExtra(EXTRA_SERVER_NAME).orEmpty().ifBlank { "KeiOS MCP" }
                val clients = intent?.getIntExtra(EXTRA_CLIENTS, 0) ?: 0
                val notificationId = intent?.getIntExtra(
                    EXTRA_NOTIFICATION_ID,
                    McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID
                ) ?: McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID
                val heartbeatEnabled = intent?.getBooleanExtra(EXTRA_HEARTBEAT_ENABLED, true) == true
                val isBlueArchiveNotification = McpNotificationPayload.isBaNotificationServerName(serverName)
                val previousNotificationId = currentNotificationId
                currentRunning = running
                currentPort = port
                currentPath = path
                currentServerName = serverName
                currentClients = clients
                currentNotificationId = notificationId
                currentHeartbeatEnabled = if (isBlueArchiveNotification) false else heartbeatEnabled
                val notification = buildNotification(
                    serverName = serverName,
                    running = running,
                    port = port,
                    path = path,
                    clients = clients
                )
                val shouldPromoteForeground =
                    !isBlueArchiveNotification &&
                        (!isForegroundPromoted || intent?.action == ACTION_START)
                if (shouldPromoteForeground) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            McpNotificationHelper.KEEPALIVE_FOREGROUND_NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(
                            McpNotificationHelper.KEEPALIVE_FOREGROUND_NOTIFICATION_ID,
                            notification
                        )
                    }
                    isForegroundPromoted = true
                } else if (!isBlueArchiveNotification && isForegroundPromoted) {
                    notificationManager.notify(
                        McpNotificationHelper.KEEPALIVE_FOREGROUND_NOTIFICATION_ID,
                        notification
                    )
                }
                if (!isBlueArchiveNotification && previousNotificationId != notificationId) {
                    McpNotificationHelper.cancelNotification(this, previousNotificationId)
                }
                if (!isBlueArchiveNotification) {
                    refreshForegroundNotification(
                        notificationId = notificationId,
                        serverName = serverName,
                        running = running,
                        port = port,
                        path = path,
                        clients = clients,
                        settleForeground = shouldPromoteForeground
                    )
                } else {
                    stopIslandRefresh()
                }
                startHeartbeat()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopIslandRefresh()
        stopHeartbeat()
        McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this)
        cancelCurrentIslandNotification()
        notificationManager.cancel(McpNotificationHelper.KEEPALIVE_FOREGROUND_NOTIFICATION_ID)
        isForegroundPromoted = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ): Notification {
        return McpNotificationHelper.buildForegroundBootstrapNotification(
            context = this,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            notificationId = McpNotificationHelper.KEEPALIVE_FOREGROUND_NOTIFICATION_ID
        )
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        if (!currentRunning || !currentHeartbeatEnabled) return
        heartbeatJob = serviceScope.launch {
            while (true) {
                delay(18_000)
                if (!currentRunning || !currentHeartbeatEnabled) continue
                McpNotificationHelper.refreshForegroundPulse(
                    context = this@McpKeepAliveService,
                    notificationId = currentNotificationId,
                    serverName = currentServerName,
                    running = currentRunning,
                    port = currentPort,
                    path = currentPath,
                    clients = currentClients
                )
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun refreshForegroundNotification(
        notificationId: Int,
        serverName: String,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        settleForeground: Boolean
    ) {
        McpNotificationHelper.refreshForegroundAsIsland(
            context = this,
            notificationId = notificationId,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            onlyAlertOnce = true
        )
        if (settleForeground) {
            scheduleIslandRefresh(
                notificationId = notificationId,
                serverName = serverName,
                port = port,
                path = path,
                clients = clients
            )
        } else {
            stopIslandRefresh()
        }
    }

    private fun scheduleIslandRefresh(
        notificationId: Int,
        serverName: String,
        port: Int,
        path: String,
        clients: Int
    ) {
        islandRefreshJob?.cancel()
        islandRefreshJob = serviceScope.launch {
            // Cold-start Focus handling on some HyperOS builds still benefits from a few
            // follow-up island re-applies after the foreground service is settled.
            val refreshCheckpoints = longArrayOf(420L, 1_150L, 2_300L, 4_000L)
            var elapsedMs = 0L
            refreshCheckpoints.forEach { checkpointMs ->
                delay((checkpointMs - elapsedMs).coerceAtLeast(0L))
                elapsedMs = checkpointMs
                if (!canRefreshIsland(notificationId, serverName, port, path, clients)) return@launch
                McpNotificationHelper.refreshForegroundAsIsland(
                    context = this@McpKeepAliveService,
                    notificationId = notificationId,
                    serverName = serverName,
                    running = currentRunning,
                    port = port,
                    path = path,
                    clients = clients,
                    onlyAlertOnce = true
                )
            }
        }
    }

    private fun canRefreshIsland(
        notificationId: Int,
        serverName: String,
        port: Int,
        path: String,
        clients: Int
    ): Boolean {
        return currentRunning &&
            currentNotificationId == notificationId &&
            currentServerName == serverName &&
            currentPort == port &&
            currentPath == path &&
            currentClients == clients
    }

    private fun stopIslandRefresh() {
        islandRefreshJob?.cancel()
        islandRefreshJob = null
    }

    private fun cancelCurrentIslandNotification() {
        McpNotificationHelper.cancelNotification(this, currentNotificationId)
    }

    companion object {
        private const val ACTION_START = "os.kei.mcp.keepalive.START"
        private const val ACTION_UPDATE = "os.kei.mcp.keepalive.UPDATE"
        private const val ACTION_STOP = "os.kei.mcp.keepalive.STOP"
        private const val ACTION_DISMISS = "os.kei.mcp.keepalive.DISMISS"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_RUNNING = "running"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_PATH = "path"
        private const val EXTRA_SERVER_NAME = "server_name"
        private const val EXTRA_CLIENTS = "clients"
        private const val EXTRA_HEARTBEAT_ENABLED = "heartbeat_enabled"

        fun startOrUpdate(
            context: Context,
            serverName: String,
            running: Boolean,
            port: Int,
            path: String,
            clients: Int,
            forceStart: Boolean,
            notificationId: Int = McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID,
            heartbeatEnabled: Boolean = true
        ) {
            val intent = Intent(context, McpKeepAliveService::class.java).apply {
                action = if (forceStart) ACTION_START else ACTION_UPDATE
                putExtra(EXTRA_RUNNING, running)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_PATH, path)
                putExtra(EXTRA_SERVER_NAME, serverName)
                putExtra(EXTRA_CLIENTS, clients)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_HEARTBEAT_ENABLED, heartbeatEnabled)
            }
            if (forceStart) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, McpKeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
