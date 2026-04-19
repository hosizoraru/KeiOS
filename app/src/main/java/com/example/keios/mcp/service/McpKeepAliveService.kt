package com.example.keios.mcp

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.keios.mcp.McpNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class McpKeepAliveService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var heartbeatJob: Job? = null
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
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID)
                NotificationManagerCompat.from(this).cancel(notificationId)
                val shouldForceStop = notificationId == McpNotificationHelper.BA_AP_NOTIFICATION_ID ||
                    notificationId == McpNotificationHelper.BA_CAFE_VISIT_NOTIFICATION_ID ||
                    intent.getBooleanExtra(EXTRA_FORCE_STOP_ON_DISMISS, false)
                if (shouldForceStop || !currentRunning) {
                    stopHeartbeat()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_STOP -> {
                stopHeartbeat()
                McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(notificationId, notification)
                }
                if (!isBlueArchiveNotification) {
                    McpNotificationHelper.refreshForegroundAsIsland(
                        context = this,
                        notificationId = notificationId,
                        serverName = serverName,
                        running = running,
                        port = port,
                        path = path,
                        clients = clients
                    )
                }
                startHeartbeat()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopHeartbeat()
        McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this)
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
        return McpNotificationHelper.buildForegroundNotification(
            context = this,
            serverName = serverName,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true,
            onlyAlertOnce = false
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

    companion object {
        private const val ACTION_START = "com.example.keios.mcp.keepalive.START"
        private const val ACTION_UPDATE = "com.example.keios.mcp.keepalive.UPDATE"
        private const val ACTION_STOP = "com.example.keios.mcp.keepalive.STOP"
        private const val ACTION_DISMISS = "com.example.keios.mcp.keepalive.DISMISS"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_RUNNING = "running"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_PATH = "path"
        private const val EXTRA_SERVER_NAME = "server_name"
        private const val EXTRA_CLIENTS = "clients"
        private const val EXTRA_HEARTBEAT_ENABLED = "heartbeat_enabled"
        private const val EXTRA_FORCE_STOP_ON_DISMISS = "force_stop_on_dismiss"

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
                putExtra(
                    EXTRA_FORCE_STOP_ON_DISMISS,
                    notificationId == McpNotificationHelper.BA_AP_NOTIFICATION_ID ||
                        notificationId == McpNotificationHelper.BA_CAFE_VISIT_NOTIFICATION_ID
                )
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, McpKeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
