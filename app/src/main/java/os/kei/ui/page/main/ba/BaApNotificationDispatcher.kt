package os.kei.ui.page.main.ba

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.notification.McpNotificationPayload

internal object BaApNotificationDispatcher {
    fun send(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
        onlyAlertOnce: Boolean = false,
    ): Boolean {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!notificationsGranted) return false

        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = McpNotificationHelper.BA_AP_NOTIFICATION_ID,
                serverName = McpNotificationPayload.BA_AP_SERVER_NAME,
                running = true,
                port = currentDisplay,
                path = thresholdDisplay.toString(),
                clients = limitDisplay,
                onlyAlertOnce = onlyAlertOnce,
            )
        }.isSuccess
    }

    fun refreshIfActive(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
    ): Boolean {
        if (!isApNotificationActive(context)) return false
        return send(
            context = context,
            currentDisplay = currentDisplay,
            limitDisplay = limitDisplay,
            thresholdDisplay = thresholdDisplay,
            onlyAlertOnce = true,
        )
    }

    private fun isApNotificationActive(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return runCatching {
            manager.activeNotifications.any { notification ->
                notification.id == McpNotificationHelper.BA_AP_NOTIFICATION_ID
            }
        }.getOrDefault(false)
    }
}
