package os.kei.ui.page.main.student.media

import android.app.Application
import android.os.Bundle
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import os.kei.ui.page.main.student.BaGuideBgmMediaButtonPreferences
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmSessionConnectionTest {
    private lateinit var lifecycle: ServiceController<BgmSessionTestService>
    private lateinit var service: BgmSessionTestService
    private lateinit var session: MediaSession

    @Before
    fun setUp() {
        lifecycle = Robolectric.buildService(BgmSessionTestService::class.java).create()
        service = lifecycle.get()
        session = assertNotNull(service.onGetSession(bgmTestController(service.packageName, trusted = true)))
    }

    @After
    fun tearDown() {
        lifecycle.destroy()
    }

    @Test
    fun `untrusted controller retains Media3 read access without mutating commands`() {
        val controller = bgmTestController("other.media.app", trusted = false)
        val defaults = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller).build()
        val result = service.sessionCallback.onConnect(session, controller)

        assertTrue(result.isAccepted)
        assertEquals(defaults.availablePlayerCommands, result.availablePlayerCommands)
        assertEquals(defaults.availableSessionCommands, result.availableSessionCommands)
        assertFalse(result.availablePlayerCommands.contains(Player.COMMAND_PLAY_PAUSE))
        assertFalse(result.availableSessionCommands.contains(BaGuideBgmMediaButtonPreferences.repeatCommand()))
        assertFalse(result.availableSessionCommands.contains(BaGuideBgmMediaButtonPreferences.stopPlaybackCommand()))
    }

    @Test
    fun `trusted and notification controllers receive custom buttons and preserve their defaults`() {
        val notificationHints = Bundle().apply {
            putBoolean(MediaController.KEY_MEDIA_NOTIFICATION_CONTROLLER_FLAG, true)
        }
        val notification = bgmTestController(service.packageName, trusted = false, hints = notificationHints)
        assertTrue(session.isMediaNotificationController(notification))
        listOf(bgmTestController(service.packageName, trusted = true), notification).forEach { controller ->
            val defaults = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller).build()
            val result = service.sessionCallback.onConnect(session, controller)

            assertTrue(result.isAccepted)
            assertEquals(defaults.availablePlayerCommands, result.availablePlayerCommands)
            assertTrue(defaults.availableSessionCommands.commands.all(result.availableSessionCommands::contains))
            assertEquals(defaults.availableSessionCommands.commands.size + 2, result.availableSessionCommands.commands.size)
            assertTrue(result.availableSessionCommands.contains(BaGuideBgmMediaButtonPreferences.repeatCommand()))
            assertTrue(result.availableSessionCommands.contains(BaGuideBgmMediaButtonPreferences.stopPlaybackCommand()))
            assertEquals(2, assertNotNull(result.mediaButtonPreferences).size)
        }
    }

    @Test
    fun `callback rejects untrusted repeat and stop even when invoked directly`() {
        val controller = bgmTestController("other.media.app", trusted = false)
        val player = service.createdPlayers.single()
        val originalRepeatMode = player.repeatMode
        listOf(
            BaGuideBgmMediaButtonPreferences.repeatCommand(),
            BaGuideBgmMediaButtonPreferences.stopPlaybackCommand(),
        ).forEach { command ->
            val result = service.sessionCallback.onCustomCommand(session, controller, command, Bundle.EMPTY).get()
            assertEquals(SessionError.ERROR_PERMISSION_DENIED, result.resultCode)
        }

        assertEquals(originalRepeatMode, player.repeatMode)
        assertEquals(0, player.repeatModeWrites)
        assertEquals(0, player.stopCount)
        assertEquals(0, player.releaseCount)
        assertTrue(service.onGetSession(controller) === session)
    }

    @Test
    fun `notification hint on another package does not authorize mutation`() {
        val hints = Bundle().apply { putBoolean(MediaController.KEY_MEDIA_NOTIFICATION_CONTROLLER_FLAG, true) }
        val controller = bgmTestController("other.media.app", trusted = false, hints = hints)

        assertFalse(session.isMediaNotificationController(controller))
        val result = service.sessionCallback.onCustomCommand(
            session, controller, BaGuideBgmMediaButtonPreferences.stopPlaybackCommand(), Bundle.EMPTY,
        ).get()
        assertEquals(SessionError.ERROR_PERMISSION_DENIED, result.resultCode)
        assertEquals(0, service.createdPlayers.single().releaseCount)
    }

    @Test
    fun `notification controller can execute stop without relying on trusted flag`() {
        val hints = Bundle().apply { putBoolean(MediaController.KEY_MEDIA_NOTIFICATION_CONTROLLER_FLAG, true) }
        val notification = bgmTestController(service.packageName, trusted = false, hints = hints)
        val result = service.sessionCallback.onCustomCommand(
            session, notification, BaGuideBgmMediaButtonPreferences.stopPlaybackCommand(), Bundle.EMPTY,
        ).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        assertEquals(0, service.createdPlayers.single().releaseCount)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, service.createdPlayers.single().releaseCount)
    }
}
