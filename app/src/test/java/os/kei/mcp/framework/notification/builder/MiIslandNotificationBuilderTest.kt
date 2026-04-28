package os.kei.mcp.framework.notification.builder

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import os.kei.MainActivity
import os.kei.mcp.notification.McpNotificationPayload
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(
    application = MiIslandNotificationBuilderTestApp::class,
    sdk = [35]
)
class MiIslandNotificationBuilderTest {
    @Test
    fun `focus open action keeps plain activity pending intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notificationOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 501,
            action = "os.kei.test.OPEN_NOTIFICATION"
        )
        val focusOpenPendingIntent = buildOpenPendingIntent(
            context = context,
            requestCode = 502,
            action = "os.kei.test.OPEN_FOCUS"
        )
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            503,
            Intent("os.kei.test.STOP_MCP").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val payload = NotificationPayload(
            state = McpNotificationPayload(
                serverName = "KeiOS MCP",
                running = false,
                port = 8080,
                path = "/mcp",
                clients = 0,
                ongoing = false,
                onlyAlertOnce = true,
                openPendingIntent = notificationOpenPendingIntent,
                stopPendingIntent = stopPendingIntent,
                focusOpenPendingIntent = focusOpenPendingIntent
            ),
            settings = UserSettings(miIslandOuterGlow = true),
            environment = EnvironmentContext(
                channelId = "test_mi_island_channel",
                isHyperOS = true
            )
        )

        val notification = MiIslandNotificationBuilder(context).build(payload)
        val focusOpenAction = notification.focusAction("mcp_action_open")
        val focusStopAction = notification.focusAction("mcp_action_stop")
        val focusParam = notification.extras.getString("miui.focus.param").orEmpty()

        assertEquals(notificationOpenPendingIntent, notification.contentIntent)
        assertEquals(focusOpenPendingIntent, focusOpenAction.actionIntent)
        assertEquals(stopPendingIntent, focusStopAction.actionIntent)
        assertTrue(focusParam.contains("mcp_action_open"))
        assertTrue(focusParam.contains("mcp_action_stop"))
    }

    private fun buildOpenPendingIntent(
        context: Application,
        requestCode: Int,
        action: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            setAction(action)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_MCP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun Notification.focusAction(key: String): Notification.Action {
        val actions = extras.getBundle("miui.focus.actions")
        assertNotNull(actions, "Focus actions bundle should be present")
        return actions.getActionCompat(key)
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getActionCompat(key: String): Notification.Action {
        return getParcelable<Notification.Action>(key)
            ?: error("Missing focus action: $key")
    }
}

class MiIslandNotificationBuilderTestApp : Application()
