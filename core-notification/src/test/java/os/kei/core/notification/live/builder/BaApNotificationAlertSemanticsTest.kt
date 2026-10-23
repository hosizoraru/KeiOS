package os.kei.core.notification.live.builder

import android.app.Application
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.core.notification.live.LiveNotificationPayload
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35, 36])
class BaApNotificationAlertSemanticsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `threshold can alert and refresh is silent on modern and ordinary fallback paths`() {
        apServerNames.forEach { serverName ->
            builders().forEach { (label, builder) ->
                val state = reminder(serverName)
                val first = builder.build(testNotificationPayload(state, isHyperOS = false))
                val refresh = builder.build(
                    testNotificationPayload(state.copy(port = 129, onlyAlertOnce = true), isHyperOS = false),
                )

                listOf(first, refresh).forEach { notification ->
                    assertFalse(notification.flags and Notification.FLAG_ONGOING_EVENT != 0, label)
                    assertFalse(
                        notification.extras.getBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING),
                        label,
                    )
                    assertEquals(NotificationCompat.CATEGORY_STATUS, notification.category, label)
                    assertEquals(state.openPendingIntent, notification.contentIntent, label)
                    assertEquals(state.stopPendingIntent, notification.actions[1].actionIntent, label)
                    assertEquals(state.deletePendingIntent, notification.deleteIntent, label)
                }
                assertFalse(first.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0, label)
                // NotificationCompat implements setSilent via a silent child group, suppressing
                // channel alerts even on the first delivery. ONLY_ALERT_ONCE alone cannot test this.
                assertEquals(NotificationCompat.GROUP_ALERT_ALL, first.groupAlertBehavior, label)
                assertTrue(refresh.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0, label)
                assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, refresh.groupAlertBehavior, label)
            }
        }
    }

    @Test
    fun `custom dismissal survives when there is no secondary button`() {
        builders().forEach { (label, builder) ->
            val state = reminder(LiveNotificationPayload.BA_AP_SERVER_NAME).let {
                it.copy(stopPendingIntent = it.openPendingIntent)
            }
            val notification = builder.build(testNotificationPayload(state))

            assertEquals(1, notification.actions.size, label)
            assertEquals(state.deletePendingIntent, notification.deleteIntent, label)
        }
    }

    @Test
    fun `explicit service and transfer lifecycles remain ongoing with silent updates`() {
        listOf(
            "Local MCP",
            LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
            LiveNotificationPayload.WEBDAV_SYNC_SERVER_NAME,
        ).forEach { serverName ->
            builders().forEach { (label, builder) ->
                val state = reminder(serverName).copy(ongoing = true, onlyAlertOnce = true)
                val notification = builder.build(testNotificationPayload(state))

                assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0, label)
                assertTrue(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0, label)
                assertEquals(NotificationCompat.GROUP_ALERT_SUMMARY, notification.groupAlertBehavior, label)
                assertEquals(NotificationCompat.CATEGORY_PROGRESS, notification.category, label)
                if (label == "modern") {
                    assertTrue(notification.extras.getBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING))
                }
            }
        }
    }

    @Test
    fun `AP threshold lifecycle preserves Xiaomi focus artwork progress and actions`() {
        apServerNames.forEach { serverName ->
            val state = reminder(serverName)
            val builder = MiIslandNotificationBuilder(context)
            val before = builder.build(testNotificationPayload(state.copy(ongoing = true)))
            val after = builder.build(testNotificationPayload(state))

            assertEquals(before.focusJson().toString(), after.focusJson().toString(), serverName)
            assertEquals(state.deletePendingIntent, after.deleteIntent, serverName)
            assertEquals(state.stopPendingIntent, after.focusAction("mcp_action_stop").actionIntent, serverName)
        }
    }

    private fun builders(): List<Pair<String, SessionNotificationBuilder>> = listOf(
        "modern" to ModernNotificationBuilder(context),
        "legacy" to LegacyNotificationBuilder(context),
    )

    private fun reminder(serverName: String): LiveNotificationPayload = LiveNotificationPayload(
        serverName = serverName,
        running = true,
        port = 128,
        path = "120",
        clients = 240,
        ongoing = false,
        onlyAlertOnce = false,
        openPendingIntent = testPendingIntent(context, 501, "os.kei.test.AP_OPEN"),
        stopPendingIntent = testPendingIntent(context, 502, "os.kei.test.AP_READ", broadcast = true),
        deletePendingIntent = testPendingIntent(context, 503, "os.kei.test.AP_DISMISS", broadcast = true),
        notificationId = 243_220,
        miFocusOrderId = "ba-ap-alert-test",
    )

    private val apServerNames = listOf(
        LiveNotificationPayload.BA_AP_SERVER_NAME,
        LiveNotificationPayload.BA_CAFE_AP_SERVER_NAME,
    )
}
