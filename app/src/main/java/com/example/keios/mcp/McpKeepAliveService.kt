package com.example.keios.mcp

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
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
    private var currentClients: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        McpNotificationHelper.ensureChannel(this)
        when (intent?.action) {
            ACTION_STOP -> {
                stopHeartbeat()
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
                val clients = intent?.getIntExtra(EXTRA_CLIENTS, 0) ?: 0
                currentRunning = running
                currentPort = port
                currentPath = path
                currentClients = clients
                val notification = buildNotification(
                    running = running,
                    port = port,
                    path = path,
                    clients = clients
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(McpNotificationHelper.KEEPALIVE_NOTIFICATION_ID, notification)
                }
                McpNotificationHelper.refreshForegroundAsIsland(
                    context = this,
                    running = running,
                    port = port,
                    path = path,
                    clients = clients
                )
                startHeartbeat()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopHeartbeat()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ): Notification {
        return McpNotificationHelper.buildForegroundNotification(
            context = this,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = true
        )
    }

    private fun startHeartbeat() {
        if (!currentRunning) return
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (true) {
                delay(18_000)
                if (!currentRunning) continue
                McpNotificationHelper.refreshForegroundPulse(
                    context = this@McpKeepAliveService,
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
        private const val EXTRA_RUNNING = "running"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_PATH = "path"
        private const val EXTRA_CLIENTS = "clients"

        fun startOrUpdate(
            context: Context,
            running: Boolean,
            port: Int,
            path: String,
            clients: Int,
            forceStart: Boolean
        ) {
            val intent = Intent(context, McpKeepAliveService::class.java).apply {
                action = if (forceStart) ACTION_START else ACTION_UPDATE
                putExtra(EXTRA_RUNNING, running)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_PATH, path)
                putExtra(EXTRA_CLIENTS, clients)
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
