package com.example.keios.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import com.example.keios.mcp.McpKeepAliveService
import com.example.keios.mcp.McpNotificationHelper

internal object BaApNotificationDispatcher {
    fun send(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
    ): Boolean {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!notificationsGranted) return false

        runCatching {
            McpKeepAliveService.startOrUpdate(
                context = context,
                serverName = "BlueArchive AP",
                running = true,
                port = currentDisplay,
                path = thresholdDisplay.toString(),
                clients = limitDisplay,
                forceStart = true,
                notificationId = McpNotificationHelper.BA_AP_NOTIFICATION_ID,
                heartbeatEnabled = false
            )
        }.onFailure {
            McpNotificationHelper.notifyTest(
                context = context,
                serverName = "BlueArchive AP",
                running = true,
                port = currentDisplay,
                path = thresholdDisplay.toString(),
                clients = limitDisplay,
            )
        }
        return true
    }
}
