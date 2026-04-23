package os.kei.mcp.framework.notification.builder

import android.app.PendingIntent
import os.kei.core.notification.focus.MiFocusNotificationCompactMode
import os.kei.core.notification.focus.MiFocusNotificationExpandedMode
import os.kei.mcp.notification.McpNotificationPayload
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import sun.misc.Unsafe

class MiIslandNotificationSpecResolverTest {
    @Test
    fun `default running session keeps service reminder template`() {
        val behavior = MiIslandNotificationSpecResolver.resolveBehavior(
            createState(
                serverName = "Local MCP",
                running = true
            )
        )

        assertEquals(MiIslandNotificationVariant.DEFAULT_RUNNING, behavior.variant)
        assertEquals(MiFocusNotificationCompactMode.TEXT, behavior.compactMode)
        assertEquals(MiFocusNotificationExpandedMode.BASE_INFO, behavior.expandedMode)
        assertFalse(behavior.showNotification)
        assertTrue(behavior.updatable)
        assertFalse(behavior.embedTextButtons)
        assertFalse(behavior.allowFloat)
    }

    @Test
    fun `stopped session uses visible action template`() {
        val behavior = MiIslandNotificationSpecResolver.resolveBehavior(
            createState(
                serverName = "Local MCP",
                running = false
            )
        )

        assertEquals(MiIslandNotificationVariant.DEFAULT_STOPPED, behavior.variant)
        assertEquals(MiFocusNotificationExpandedMode.ICON_TEXT, behavior.expandedMode)
        assertTrue(behavior.showNotification)
        assertFalse(behavior.updatable)
        assertTrue(behavior.embedTextButtons)
        assertTrue(behavior.allowFloat)
    }

    @Test
    fun `blue archive ap uses progress template`() {
        val behavior = MiIslandNotificationSpecResolver.resolveBehavior(
            createState(
                serverName = McpNotificationPayload.BA_AP_SERVER_NAME,
                running = true
            )
        )

        assertEquals(MiIslandNotificationVariant.BA_AP_RUNNING, behavior.variant)
        assertEquals(MiFocusNotificationCompactMode.PROGRESS, behavior.compactMode)
        assertEquals(MiFocusNotificationExpandedMode.BASE_INFO, behavior.expandedMode)
        assertTrue(behavior.showNotification)
        assertTrue(behavior.updatable)
        assertTrue(behavior.embedTextButtons)
        assertFalse(behavior.allowFloat)
    }

    @Test
    fun `blue archive event reminders use visible text template`() {
        val cafeBehavior = MiIslandNotificationSpecResolver.resolveBehavior(
            createState(
                serverName = McpNotificationPayload.BA_CAFE_VISIT_SERVER_NAME,
                running = true
            )
        )
        val arenaBehavior = MiIslandNotificationSpecResolver.resolveBehavior(
            createState(
                serverName = McpNotificationPayload.BA_ARENA_REFRESH_SERVER_NAME,
                running = true
            )
        )

        assertEquals(MiIslandNotificationVariant.BA_CAFE_VISIT_RUNNING, cafeBehavior.variant)
        assertEquals(MiIslandNotificationVariant.BA_ARENA_REFRESH_RUNNING, arenaBehavior.variant)
        assertEquals(MiFocusNotificationCompactMode.TEXT, cafeBehavior.compactMode)
        assertEquals(MiFocusNotificationExpandedMode.ICON_TEXT, cafeBehavior.expandedMode)
        assertTrue(cafeBehavior.showNotification)
        assertFalse(cafeBehavior.updatable)
        assertTrue(cafeBehavior.embedTextButtons)
        assertTrue(cafeBehavior.allowFloat)
        assertEquals(cafeBehavior, arenaBehavior.copy(variant = MiIslandNotificationVariant.BA_CAFE_VISIT_RUNNING))
    }

    private fun createState(
        serverName: String,
        running: Boolean
    ): McpNotificationPayload {
        val pendingIntent = createFakePendingIntent()
        return McpNotificationPayload(
            serverName = serverName,
            running = running,
            port = 1,
            path = "demo",
            clients = 2,
            ongoing = running,
            onlyAlertOnce = true,
            openPendingIntent = pendingIntent,
            stopPendingIntent = pendingIntent
        )
    }

    private fun createFakePendingIntent(): PendingIntent {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe").apply {
            isAccessible = true
        }
        val unsafe = unsafeField.get(null) as Unsafe
        return unsafe.allocateInstance(PendingIntent::class.java) as PendingIntent
    }
}
