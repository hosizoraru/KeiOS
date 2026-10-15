package os.kei.core.notification.live.builder

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import org.junit.Test
import os.kei.core.notification.live.LiveNotificationPayload
import sun.misc.Unsafe
import kotlin.test.assertEquals

class ModernNotificationSpecResolverTest {
    @Test
    fun `default running session uses capped client progress`() {
        val spec = ModernNotificationSpecResolver.resolve(
            createState(
                serverName = "Local MCP",
                running = true,
                port = 8080,
                clients = 3,
                ongoing = true
            )
        )

        assertEquals(ModernNotificationKind.DEFAULT, spec.kind)
        assertEquals(72, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.SHORT_TEXT, spec.shortCriticalMode)
        assertEquals(true, spec.ongoing)
        assertEquals(true, spec.requestPromotedOngoing)
        assertEquals(true, spec.showProgressStyle)
        assertEquals(NotificationCompat.CATEGORY_PROGRESS, spec.category)
    }

    @Test
    fun `blue archive ap uses ratio progress`() {
        val spec = ModernNotificationSpecResolver.resolve(
            createState(
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                running = true,
                port = 4,
                clients = 8,
                ongoing = false
            )
        )

        assertEquals(ModernNotificationKind.BA_AP, spec.kind)
        assertEquals(50, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.SHORT_TEXT, spec.shortCriticalMode)
        assertEquals(true, spec.ongoing)
    }

    /**
     * Events that have already happened are dismissible status notifications.
     *
     * Before daily-done joined the one-shot BA events, `ongoing` was `running || state.ongoing` and
     * `requestPromotedOngoing` tracked it, so a finished run arrived as an un-dismissible
     * ONGOING_EVENT|PROMOTED_ONGOING ProgressStyle notification with a bar at 100%. Each row is the
     * dispatcher's real pair: running, not ongoing.
     */
    @Test
    fun `one shot ba events are dismissible status, not promoted ongoing progress`() {
        listOf(
            LiveNotificationPayload.BA_CAFE_VISIT_SERVER_NAME to ModernShortCriticalMode.ONLINE_TEXT,
            LiveNotificationPayload.BA_ARENA_REFRESH_SERVER_NAME to ModernShortCriticalMode.ONLINE_TEXT,
            // A terminal short word comes from the dispatcher's overrideShortText; ONLINE_TEXT would pull
            // the server-status wording in instead.
            LiveNotificationPayload.BA_DAILY_DONE_SERVER_NAME to ModernShortCriticalMode.SHORT_TEXT,
        ).forEach { (serverName, shortMode) ->
            val spec = ModernNotificationSpecResolver.resolve(
                createState(serverName = serverName, running = true, port = 0, clients = 0, ongoing = false),
            )

            assertEquals(100, spec.progressPercent, serverName)
            assertEquals(shortMode, spec.shortCriticalMode, serverName)
            assertEquals(false, spec.ongoing, serverName)
            assertEquals(false, spec.requestPromotedOngoing, serverName)
            assertEquals(false, spec.showProgressStyle, serverName)
            assertEquals(NotificationCompat.CATEGORY_STATUS, spec.category, serverName)
        }
    }

    @Test
    fun `stopped session clears live update emphasis`() {
        val spec = ModernNotificationSpecResolver.resolve(
            createState(
                serverName = "Local MCP",
                running = false,
                port = 8080,
                clients = 5,
                ongoing = false
            )
        )

        assertEquals(0, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.NONE, spec.shortCriticalMode)
        assertEquals(false, spec.ongoing)
        assertEquals(false, spec.requestPromotedOngoing)
    }

    @Test
    fun `calendar pool uses override progress while a deadline is pending`() {
        val spec = ModernNotificationSpecResolver.resolve(
            state = createState(
                serverName = LiveNotificationPayload.BA_CALENDAR_POOL_SERVER_NAME,
                running = true,
                port = 0,
                clients = 1,
                ongoing = true,
                overrideProgressPercent = 67,
                deadlineAtMs = 1_778_007_600_000L
            ),
        )

        assertEquals(ModernNotificationKind.BA_CALENDAR_POOL, spec.kind)
        assertEquals(67, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.SHORT_TEXT, spec.shortCriticalMode)
        assertEquals(true, spec.showProgressStyle)
        assertEquals(true, spec.requestPromotedOngoing)
    }

    @Test
    fun `calendar pool terminal update uses status presentation`() {
        val spec = ModernNotificationSpecResolver.resolve(
            state = createState(
                serverName = LiveNotificationPayload.BA_CALENDAR_POOL_SERVER_NAME,
                running = true,
                port = 0,
                clients = 1,
                ongoing = false,
                overrideProgressPercent = 0,
                deadlineAtMs = null
            ),
        )

        assertEquals(ModernNotificationKind.BA_CALENDAR_POOL, spec.kind)
        assertEquals(0, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.SHORT_TEXT, spec.shortCriticalMode)
        assertEquals(false, spec.ongoing)
        assertEquals(false, spec.requestPromotedOngoing)
        assertEquals(false, spec.showProgressStyle)
    }

    @Test
    fun `github share import draws a bar only for an override progress`() {
        val withOverride = ModernNotificationSpecResolver.resolve(
            state = createState(
                serverName = LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
                running = true,
                port = 72,
                clients = 1,
                ongoing = true,
                overrideProgressPercent = 72
            ),
        )
        val phaseOnly = ModernNotificationSpecResolver.resolve(
            state = createState(
                serverName = LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
                running = true,
                port = 88,
                clients = 1,
                ongoing = true
            ),
        )

        assertEquals(ModernNotificationKind.GITHUB_SHARE_IMPORT, withOverride.kind)
        assertEquals(72, withOverride.progressPercent)
        assertEquals(ModernShortCriticalMode.SHORT_TEXT, withOverride.shortCriticalMode)
        assertEquals(true, withOverride.requestPromotedOngoing)
        assertEquals(true, withOverride.showProgressStyle)
        // Without an override the phase (carried in `port`) still reaches the tracker, but no bar is drawn.
        assertEquals(88, phaseOnly.progressPercent)
        assertEquals(false, phaseOnly.showProgressStyle)
    }

    @Test
    fun `webdav sync uses override progress and accent color`() {
        val spec = ModernNotificationSpecResolver.resolve(
            state = createState(
                serverName = LiveNotificationPayload.WEBDAV_SYNC_SERVER_NAME,
                running = true,
                port = 45,
                clients = 6,
                ongoing = true,
                overrideProgressPercent = 45,
                overrideAccentColor = "#F59E0B"
            ),
        )

        assertEquals(ModernNotificationKind.WEBDAV_SYNC, spec.kind)
        assertEquals(45, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.SHORT_TEXT, spec.shortCriticalMode)
        assertEquals(true, spec.showProgressStyle)
        assertEquals(0xFFF59E0B.toInt(), spec.progressColor)
    }

    @Test
    fun `webdav terminal update uses status presentation`() {
        val spec = ModernNotificationSpecResolver.resolve(
            state = createState(
                serverName = LiveNotificationPayload.WEBDAV_SYNC_SERVER_NAME,
                running = false,
                port = 100,
                clients = 6,
                ongoing = false,
                overrideProgressPercent = 100,
                overrideAccentColor = "#22C55E"
            ),
        )

        assertEquals(ModernNotificationKind.WEBDAV_SYNC, spec.kind)
        assertEquals(0, spec.progressPercent)
        assertEquals(ModernShortCriticalMode.NONE, spec.shortCriticalMode)
        assertEquals(false, spec.ongoing)
        assertEquals(false, spec.showProgressStyle)
        assertEquals(0xFF22C55E.toInt(), spec.progressColor)
    }

    private fun createState(
        serverName: String,
        running: Boolean,
        port: Int,
        clients: Int,
        ongoing: Boolean,
        overrideProgressPercent: Int? = null,
        deadlineAtMs: Long? = null,
        overrideAccentColor: String? = null
    ): LiveNotificationPayload {
        val pendingIntent = createFakePendingIntent()
        return LiveNotificationPayload(
            serverName = serverName,
            running = running,
            port = port,
            path = "demo",
            clients = clients,
            ongoing = ongoing,
            onlyAlertOnce = true,
            openPendingIntent = pendingIntent,
            stopPendingIntent = pendingIntent,
            overrideProgressPercent = overrideProgressPercent,
            overrideAccentColor = overrideAccentColor,
            deadlineAtMs = deadlineAtMs
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
