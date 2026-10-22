package os.kei.ui.page.main.student.media

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaGuideBgmMediaSessionServiceTest {
    private lateinit var lifecycle: ServiceController<BgmSessionTestService>
    private lateinit var service: BgmSessionTestService
    private lateinit var trusted: MediaSession.ControllerInfo

    @Before
    fun setUp() {
        lifecycle = Robolectric.buildService(BgmSessionTestService::class.java).create()
        service = lifecycle.get()
        trusted = bgmTestController(service.packageName, trusted = true)
    }

    @After
    fun tearDown() {
        if (!service.destroyedByFramework) lifecycle.destroy()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `explicit stop releases playback and disconnects UI before service destruction`() {
        val session = assertNotNull(service.onGetSession(trusted))
        val player = service.createdPlayers.single()
        service.addSession(session)
        var disconnected = false
        val future = MediaController.Builder(service, session.token)
            .setApplicationLooper(Looper.getMainLooper())
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) { disconnected = true }
            })
            .buildAsync()
        shadowOf(Looper.getMainLooper()).idle()
        val controller = future.get(1, TimeUnit.SECONDS)
        try {
            assertTrue(controller.isConnected)
            val result = stop(session)
            assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
            assertEquals(0, player.releaseCount)
            assertTrue(controller.isConnected)
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
            assertFalse(service.destroyedByFramework)
            assertTrue(service.sessions.isEmpty())
            assertFalse(player.playWhenReady)
            assertEquals(0, player.mediaItemCount)
            assertEquals(1, player.stopCount)
            assertEquals(1, player.clearCount)
            assertEquals(1, player.releaseCount)
            assertTrue(disconnected)
            assertFalse(controller.isConnected)

            assertEquals(SessionError.ERROR_SESSION_DISCONNECTED, stop(session).resultCode)
            service.onDestroy()
            assertEquals(1, player.releaseCount)
        } finally {
            controller.release()
        }
    }

    @Test
    fun `controller stop receives success before both connected controllers disconnect`() {
        val session = assertNotNull(service.onGetSession(trusted))
        val player = service.createdPlayers.single()
        service.addSession(session)
        val events = mutableListOf<String>()
        val controllerFutures = (1..2).map { index ->
            MediaController.Builder(service, session.token)
                .setApplicationLooper(Looper.getMainLooper())
                .setListener(object : MediaController.Listener {
                    override fun onDisconnected(controller: MediaController) {
                        events.add("disconnected-$index")
                    }
                })
                .buildAsync()
        }
        shadowOf(Looper.getMainLooper()).idle()
        val controllers = controllerFutures.map { it.get(1, TimeUnit.SECONDS) }
        try {
            assertTrue(controllers.all { it.isConnected })
            val acknowledgment = controllers.first().sendCustomCommand(
                BaGuideBgmMediaButtonPreferences.stopPlaybackCommand(), Bundle.EMPTY,
            )
            acknowledgment.addListener({ events.add("acknowledged") }, MoreExecutors.directExecutor())
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(SessionResult.RESULT_SUCCESS, acknowledgment.get(1, TimeUnit.SECONDS).resultCode)
            assertEquals("acknowledged", events.first())
            assertEquals(setOf("disconnected-1", "disconnected-2"), events.drop(1).toSet())
            assertTrue(controllers.none { it.isConnected })
            assertTrue(service.sessions.isEmpty())
            assertFalse(service.destroyedByFramework)
            assertEquals(1, player.releaseCount)
        } finally {
            controllers.forEach { it.release() }
        }
    }

    @Test
    fun `new trusted request creates fresh session and obsolete callback cannot stop it`() {
        val previous = assertNotNull(service.onGetSession(trusted))
        assertEquals(SessionResult.RESULT_SUCCESS, stop(previous).resultCode)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(service.onGetSession(bgmTestController("other.media.app", trusted = false)))

        val fresh = assertNotNull(service.onGetSession(trusted))
        assertTrue(fresh !== previous)
        assertEquals(2, service.createdPlayers.size)
        assertEquals(1, service.createdPlayers.first().releaseCount)
        assertEquals(0, service.createdPlayers.last().releaseCount)
        assertEquals(SessionError.ERROR_SESSION_DISCONNECTED, stop(previous).resultCode)
        assertFalse(service.sessionCallback.onConnect(previous, trusted).isAccepted)
        assertTrue(service.onGetSession(trusted) === fresh)
        assertEquals(0, service.createdPlayers.last().releaseCount)

        val future = MediaController.Builder(service, fresh.token).buildAsync()
        shadowOf(Looper.getMainLooper()).idle()
        val controller = future.get(1, TimeUnit.SECONDS)
        try {
            assertTrue(controller.isConnected)
            assertEquals(SessionResult.RESULT_SUCCESS, stop(fresh).resultCode)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, service.createdPlayers.last().releaseCount)
        } finally {
            controller.release()
        }
    }

    @Test
    fun `transient focus suppression and buffering keep session and player alive`() {
        val session = assertNotNull(service.onGetSession(trusted))
        val player = service.createdPlayers.single()
        service.addSession(session)
        shadowOf(Looper.getMainLooper()).idle()
        player.setTransientState(Player.STATE_READY, Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(service.onGetSession(trusted) === session)
        assertTrue(player.playWhenReady)
        assertEquals(0, player.releaseCount)

        player.setTransientState(Player.STATE_BUFFERING, Player.PLAYBACK_SUPPRESSION_REASON_NONE)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(service.onGetSession(trusted) === session)
        assertTrue(player.playWhenReady)
        assertEquals(0, player.stopCount)
        assertEquals(0, player.clearCount)
        assertEquals(0, player.releaseCount)
    }

    @Test
    fun `destruction releases playback and cannot recreate a session`() {
        val player = service.createdPlayers.single()
        service.onDestroy()

        assertEquals(1, player.releaseCount)
        assertNull(service.onGetSession(trusted))
        assertEquals(1, service.createdPlayers.size)
    }

    private fun stop(session: MediaSession): SessionResult =
        service.sessionCallback.onCustomCommand(
            session, trusted, BaGuideBgmMediaButtonPreferences.stopPlaybackCommand(), Bundle.EMPTY,
        ).get()
}

@OptIn(UnstableApi::class)
internal class BgmSessionTestService : BaGuideBgmMediaSessionService() {
    val createdPlayers = mutableListOf<BgmServiceTestPlayer>()
    var destroyedByFramework = false
        private set

    override fun createPlayer(): Player = BgmServiceTestPlayer().also(createdPlayers::add)

    // Playback persistence is independent of the session lifecycle and requires native MMKV.
    override fun restorePlaybackSnapshot() = Unit

    override fun onDestroy() {
        destroyedByFramework = true
        super.onDestroy()
    }
}

@OptIn(UnstableApi::class)
internal fun bgmTestController(
    packageName: String,
    trusted: Boolean,
    hints: Bundle = Bundle.EMPTY,
): MediaSession.ControllerInfo = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
    packageName, Process.myPid(), Process.myUid(), MediaLibraryInfo.VERSION_INT, 1, trusted, hints, true,
)

@OptIn(UnstableApi::class)
internal class BgmServiceTestPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    var stopCount = 0
        private set
    var clearCount = 0
        private set
    var releaseCount = 0
        private set
    var repeatModeWrites = 0
        private set
    private var snapshot = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_STOP,
            Player.COMMAND_RELEASE,
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
            Player.COMMAND_SET_REPEAT_MODE,
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_METADATA,
        ).build())
        .setPlaylist(listOf(MediaItemData.Builder("bgm-test-item")
            .setMediaItem(MediaItem.fromUri("https://example.com/bgm.mp3"))
            .setDurationUs(60_000_000L)
            .build()))
        .setPlaybackState(Player.STATE_READY)
        .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setContentPositionMs(3_500L)
        .build()

    override fun getState(): State = snapshot

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        snapshot = snapshot.buildUpon()
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        stopCount++
        snapshot = snapshot.buildUpon().setPlaybackState(Player.STATE_IDLE).build()
        return Futures.immediateVoidFuture()
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        clearCount++
        snapshot = snapshot.buildUpon().setPlaylist(emptyList())
            .setCurrentMediaItemIndex(C.INDEX_UNSET).build()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        repeatModeWrites++
        snapshot = snapshot.buildUpon().setRepeatMode(repeatMode).build()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        releaseCount++
        return Futures.immediateVoidFuture()
    }

    fun setTransientState(playbackState: Int, suppressionReason: Int) {
        snapshot = snapshot.buildUpon().setPlaybackState(playbackState)
            .setPlaybackSuppressionReason(suppressionReason).build()
        invalidateState()
    }
}
