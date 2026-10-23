package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.notification.McpStandaloneEventRequest
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.ui.page.main.ba.support.BaAccountId

internal object BaCafeApNotificationDispatcher {
    private const val TAG = "BaCafeApNotify"

    fun send(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
        notificationId: Int = McpNotificationHelper.BA_CAFE_AP_NOTIFICATION_ID,
        accountDisplayName: String = "",
        accountId: BaAccountId? = null,
    ): Boolean {
        val notificationsGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) {
            AppLogger.w(TAG) { "skip cafe AP notification: permission missing id=$notificationId" }
            return false
        }

        return runCatching {
            val content = context.getString(
                R.string.ba_cafe_ap_notification_content,
                currentDisplay,
                thresholdDisplay.toString(),
                limitDisplay,
            )
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_CAFE_AP_SERVER_NAME,
                running = true,
                port = currentDisplay,
                path = thresholdDisplay.toString(),
                clients = limitDisplay,
                // A threshold reminder must remain eligible for ordinary notification alerts.
                ongoing = false,
                onlyAlertOnce = false,
                overrideContent = baAccountNotificationContent(
                    context = context,
                    accountDisplayName = accountDisplayName,
                    content = content,
                ),
                targetBaAccountId = accountId?.value,
            )
        }.onSuccess { sent ->
            AppLogger.i(TAG) {
                "send result=$sent id=$notificationId current=$currentDisplay limit=$limitDisplay account=${accountId?.value.orEmpty()}"
            }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "send failed id=$notificationId", throwable)
        }.getOrDefault(false)
    }

    suspend fun sendAwaitingDelivery(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
        notificationId: Int = McpNotificationHelper.BA_CAFE_AP_NOTIFICATION_ID,
        accountDisplayName: String = "",
        accountId: BaAccountId? = null,
        onDelivered: suspend () -> Unit = {},
    ): Boolean {
        val notificationsGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) {
            AppLogger.w(TAG) { "skip cafe AP notification: permission missing id=$notificationId" }
            return false
        }

        return try {
            val content = context.getString(
                R.string.ba_cafe_ap_notification_content,
                currentDisplay,
                thresholdDisplay.toString(),
                limitDisplay,
            )
            val request =
                McpStandaloneEventRequest(
                    serverName = LiveNotificationPayload.BA_CAFE_AP_SERVER_NAME,
                    running = true,
                    port = currentDisplay,
                    path = thresholdDisplay.toString(),
                    clients = limitDisplay,
                    ongoing = false,
                    onlyAlertOnce = false,
                    overrideContent =
                        baAccountNotificationContent(
                            context = context,
                            accountDisplayName = accountDisplayName,
                            content = content,
                        ),
                    targetBaAccountId = accountId?.value,
                )
            McpNotificationHelper.notifyStandaloneEventAwaitingDelivery(
                context = context,
                notificationId = notificationId,
                request = request,
                onDelivered = onDelivered,
            ).also { sent ->
                AppLogger.i(TAG) {
                    "awaited send result=$sent id=$notificationId current=$currentDisplay limit=$limitDisplay account=${accountId?.value.orEmpty()}"
                }
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            AppLogger.e(TAG, "awaited send failed id=$notificationId", throwable)
            false
        }
    }
}
