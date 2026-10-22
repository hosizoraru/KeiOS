package os.kei.ui.page.main.student.media

import android.app.PendingIntent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.student.BA_GUIDE_BGM_COMMAND_STOP_PLAYBACK
import os.kei.ui.page.main.student.BA_GUIDE_BGM_COMMAND_TOGGLE_REPEAT
import os.kei.ui.page.main.student.BaGuideBgmMediaButtonPreferences
import os.kei.ui.page.main.student.BaGuideBgmPlaybackRepository
import os.kei.ui.page.main.student.BaGuideBgmPlaybackRouteIntentFactory
import os.kei.ui.page.main.student.configureGuideMediaAudioBehavior
import os.kei.ui.page.main.student.createGameKeeMediaBitmapLoader
import os.kei.ui.page.main.student.createGameKeeMediaSourceFactory

internal const val BA_GUIDE_BGM_MEDIA_SESSION_ID = "ba_guide_bgm_media_session"

@OptIn(UnstableApi::class)
class BaGuideBgmMediaSessionService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val playbackRepository = BaGuideBgmPlaybackRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + AppDispatchers.media)
    private val repeatModeListener =
        object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                updateMediaButtonPreferences()
            }
        }

    override fun onCreate() {
        super.onCreate()
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        val sessionPlayer =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createGameKeeMediaSourceFactory(this))
                .build()
                .apply {
                    configureGuideMediaAudioBehavior()
                }
        sessionPlayer.addListener(repeatModeListener)
        player = sessionPlayer
        serviceScope.launch {
            playbackRepository.loadSnapshot()
        }
        setMediaNotificationProvider(BaGuideBgmMediaNotificationProviderFactory.create(this))
        mediaSession =
            MediaSession
                .Builder(this, sessionPlayer)
                .setBitmapLoader(createGameKeeMediaBitmapLoader(this))
                .setId(BA_GUIDE_BGM_MEDIA_SESSION_ID)
                .setSessionActivity(createSessionActivity())
                .setMediaButtonPreferences(
                    BaGuideBgmMediaButtonPreferences.mediaButtonPreferences(
                        context = this,
                        queueMode =
                            BaGuideBgmMediaButtonPreferences.queueModeFromNativeRepeatMode(
                                sessionPlayer.repeatMode,
                            ),
                    ),
                ).setCallback(BaGuideBgmMediaSessionCallback(this))
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        BaGuideBgmMediaNotificationProviderFactory.cancelNotification(this)
        mediaSession?.release()
        mediaSession = null
        player?.removeListener(repeatModeListener)
        player?.release()
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createSessionActivity(): PendingIntent = BaGuideBgmPlaybackRouteIntentFactory.createPendingIntent(this)

    private fun updateMediaButtonPreferences() {
        val session = mediaSession ?: return
        session.setMediaButtonPreferences(
            BaGuideBgmMediaButtonPreferences.mediaButtonPreferences(
                context = this,
                queueMode =
                    BaGuideBgmMediaButtonPreferences.queueModeFromNativeRepeatMode(
                        session.player.repeatMode,
                    ),
            ),
        )
    }

    private class BaGuideBgmMediaSessionCallback(
        private val service: BaGuideBgmMediaSessionService,
    ) : MediaSession.Callback {
        /**
         * Grants each controller what Media3 thinks it should have, plus this session's own two buttons.
         *
         * `AcceptedResultBuilder(session)` is deprecated as of Media3 1.11 because it handed *every*
         * controller the trusted defaults — all player commands, all session commands — whether the
         * caller was the media notification or some other app that had merely found the session. The
         * `(session, controller)` overload branches on `controller.isTrusted()` instead and gives an
         * untrusted one the read-only sets. That is a real tightening and the point of the migration,
         * not a rename: an untrusted controller can still read state and still be told about this
         * session, but can no longer drive transport.
         *
         * The base is read back off a throwaway result rather than re-derived from `DEFAULT_*` here, so
         * which set a controller belongs in stays Media3's rule to state and cannot drift from it.
         *
         * The two custom commands are added on top for every controller, exactly as before. They have to
         * be available to the media-notification controller for [setMediaButtonPreferences] to render
         * enabled buttons, and narrowing them by trust as well would be a policy change this migration
         * has no mandate to make.
         */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val defaults =
                MediaSession.ConnectionResult
                    .AcceptedResultBuilder(session, controller)
                    .build()
            return MediaSession.ConnectionResult
                .AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(
                    BaGuideBgmMediaButtonPreferences.availableSessionCommands(
                        defaults.availableSessionCommands,
                    ),
                ).setMediaButtonPreferences(
                    BaGuideBgmMediaButtonPreferences.mediaButtonPreferences(
                        context = service,
                        queueMode =
                            BaGuideBgmMediaButtonPreferences.queueModeFromNativeRepeatMode(
                                session.player.repeatMode,
                            ),
                    ),
                ).build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> =
            when (customCommand.customAction) {
                BA_GUIDE_BGM_COMMAND_TOGGLE_REPEAT -> {
                    toggleRepeat(session)
                    successResult()
                }

                BA_GUIDE_BGM_COMMAND_STOP_PLAYBACK -> {
                    service.stopPlaybackAndDismiss()
                    successResult()
                }

                else -> {
                    Futures.immediateFuture(
                        SessionResult(SessionError.ERROR_NOT_SUPPORTED),
                    )
                }
            }

        private fun toggleRepeat(session: MediaSession) {
            val currentMode =
                BaGuideBgmMediaButtonPreferences.queueModeFromNativeRepeatMode(
                    session.player.repeatMode,
                )
            val nextMode = BaGuideBgmMediaButtonPreferences.nextQueueMode(currentMode)
            session.player.repeatMode = BaGuideBgmMediaButtonPreferences.nativeRepeatMode(nextMode)
            val selectedAudioUrl =
                session.player.currentMediaItem
                    ?.mediaId
                    .orEmpty()
                    .ifBlank { service.playbackRepository.selectedAudioUrl() }
            service.serviceScope.launch {
                service.playbackRepository.saveSelection(
                    audioUrl = selectedAudioUrl,
                    queueModeName = nextMode.name,
                )
            }
            session.setMediaButtonPreferences(
                BaGuideBgmMediaButtonPreferences.mediaButtonPreferences(
                    context = service,
                    queueMode = nextMode,
                ),
            )
        }

        private fun successResult(): ListenableFuture<SessionResult> = Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    private fun stopPlaybackAndDismiss() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            stopPlaybackAndDismissOnMain()
        } else {
            mainHandler.post { stopPlaybackAndDismissOnMain() }
        }
    }

    private fun stopPlaybackAndDismissOnMain() {
        player?.let { sessionPlayer ->
            runCatching { sessionPlayer.playWhenReady = false }
            runCatching { sessionPlayer.stop() }
            runCatching { sessionPlayer.clearMediaItems() }
        }
        runCatching { triggerNotificationUpdate() }
        runCatching { BaGuideBgmMediaNotificationProviderFactory.cancelNotification(this) }
        runCatching { pauseAllPlayersAndStopSelf() }
    }
}
