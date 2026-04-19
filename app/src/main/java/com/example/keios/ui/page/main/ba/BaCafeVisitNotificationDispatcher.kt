package com.example.keios.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import com.example.keios.mcp.McpKeepAliveService
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.mcp.McpNotificationPayload

internal object BaCafeVisitNotificationDispatcher {
    fun send(context: Context): Boolean {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!notificationsGranted) return false

        runCatching {
            McpKeepAliveService.startOrUpdate(
                context = context,
                serverName = McpNotificationPayload.BA_CAFE_VISIT_SERVER_NAME,
                running = true,
                port = 0,
                path = McpNotificationPayload.BA_CAFE_VISIT_PATH,
                clients = 0,
                forceStart = true,
                notificationId = McpNotificationHelper.BA_CAFE_VISIT_NOTIFICATION_ID,
                heartbeatEnabled = false
            )
        }.onFailure {
            McpNotificationHelper.notifyTest(
                context = context,
                serverName = McpNotificationPayload.BA_CAFE_VISIT_SERVER_NAME,
                running = true,
                port = 0,
                path = McpNotificationPayload.BA_CAFE_VISIT_PATH,
                clients = 0,
            )
        }
        return true
    }
}
