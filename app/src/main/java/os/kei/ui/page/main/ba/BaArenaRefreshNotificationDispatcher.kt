package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import os.kei.R
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.notification.McpNotificationPayload
import os.kei.ui.page.main.ba.support.baServerLabelRes
import os.kei.ui.page.main.ba.support.serverRefreshTimeZone

internal object BaArenaRefreshNotificationDispatcher {
    fun send(
        context: Context,
        serverIndex: Int,
        slotMs: Long,
    ): Boolean {
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!notificationsGranted) return false

        val detailLine = buildRefreshDetailLine(
            context = context,
            serverIndex = serverIndex,
            slotMs = slotMs
        )

        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = McpNotificationHelper.BA_ARENA_REFRESH_NOTIFICATION_ID,
                serverName = McpNotificationPayload.BA_ARENA_REFRESH_SERVER_NAME,
                running = true,
                port = 0,
                path = detailLine,
                clients = 0
            )
        }.isSuccess
    }

    private fun buildRefreshDetailLine(
        context: Context,
        serverIndex: Int,
        slotMs: Long,
    ): String {
        val timeZone = serverRefreshTimeZone(serverIndex)
        val calendar = java.util.Calendar.getInstance(timeZone).apply {
            timeInMillis = slotMs.coerceAtLeast(0L)
        }
        val slotHour = calendar.get(java.util.Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        return context.getString(
            R.string.ba_arena_refresh_notification_content_detail,
            context.getString(baServerLabelRes(serverIndex)),
            slotHour
        )
    }
}
