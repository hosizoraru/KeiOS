package com.example.keios.mcp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.keios.MainActivity
import com.example.keios.R

object McpNotificationHelper {

    const val CHANNEL_ID = "mcp_keepalive_channel_v2"
    private const val LEGACY_CHANNEL_ID = "mcp_keepalive_channel"
    const val KEEPALIVE_NOTIFICATION_ID = 38888
    private const val TEST_NOTIFICATION_ID = KEEPALIVE_NOTIFICATION_ID
    private const val ACTION_STOP = "com.example.keios.mcp.keepalive.STOP"

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
        running: Boolean,
        port: Int,
        path: String,
        clients: Int,
        ongoing: Boolean,
        onlyAlertOnce: Boolean = true
    ): android.app.Notification {
        val title = if (running) "MCP Server 正在后台运行" else "MCP Server 已停止"
        val content = if (running) {
            "MCP 协议 · 端口 $port · 在线 $clients"
        } else {
            "点击返回应用重新启动 MCP 服务"
        }
        val statusText = if (running) "运行中" else "已停止"
        val onlineText = "在线 $clients"
        val shortText = if (running) "$statusText · $onlineText" else "MCP Offline"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(context, McpKeepAliveService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content\nEndpoint: http://127.0.0.1:$port$path"))
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_notification_logo, "打开", openPendingIntent)
            .addAction(R.drawable.ic_notification_logo, "停止", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        McpIslandNotificationBuilder.buildExtras(
            context = context,
            title = title,
            content = content,
            shortText = shortText,
            statusText = statusText,
            onlineText = onlineText,
            openPendingIntent = openPendingIntent,
            stopPendingIntent = stopPendingIntent
        )?.let(builder::addExtras)

        return builder.build()
    }

    fun notifyTest(
        context: Context,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val notification = buildForegroundNotification(
            context = context,
            running = running,
            port = port,
            path = path,
            clients = clients,
            ongoing = running,
            onlyAlertOnce = false
        )
        notifyWithXiaomiMagic(
            context = context,
            notificationId = TEST_NOTIFICATION_ID,
            notification = notification
        )
    }

    fun refreshForegroundAsIsland(
        context: Context,
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val notification = buildForegroundNotification(
            context = context,
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
        running: Boolean,
        port: Int,
        path: String,
        clients: Int
    ) {
        ensureChannel(context)
        val notification = buildForegroundNotification(
            context = context,
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

    private fun notifyWithXiaomiMagic(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        XiaomiIslandMagic.notify(
            context = context,
            notificationId = notificationId,
            notification = notification
        )
    }
}
