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
open class BaGuideBgmMediaSessionService : MediaSessionService() {
    private var player: Player? = null
    private var mediaSession: MediaSession? = null
    private var destroyed = false
    internal val sessionCallback: MediaSession.Callback = BaGuideBgmMediaSessionCallback(this)
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
        restorePlaybackSnapshot()
        setMediaNotificationProvider(BaGuideBgmMediaNotificationProviderFactory.create(this))
        createSession()
    }

    protected open fun createPlayer(): Player =
        ExoPlayer.Builder(this)
            .setMediaSourceFactory(createGameKeeMediaSourceFactory(this))
            .build()
            .apply { configureGuideMediaAudioBehavior() }

    protected open fun restorePlaybackSnapshot() {
        serviceScope.launch { playbackRepository.loadSnapshot() }
    }

    private fun createSession(): MediaSession {
        val sessionPlayer = createPlayer()
        sessionPlayer.addListener(repeatModeListener)
        player = sessionPlayer
        return MediaSession
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
            ).setCallback(sessionCallback)
            .build()
            .also { mediaSession = it }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (destroyed) return null
        return mediaSession ?: if (controllerInfo.isTrusted) createSession() else null
    }

    override fun onDestroy() {
        destroyed = true
        try {
            releasePlayback()
        } finally {
            try {
                BaGuideBgmMediaNotificationProviderFactory.cancelNotification(this)
            } finally {
                serviceScope.cancel()
                super.onDestroy()
            }
        }
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
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            if (service.mediaSession !== session) return MediaSession.ConnectionResult.reject()
            val defaults =
                MediaSession.ConnectionResult
                    .AcceptedResultBuilder(session, controller)
                    .build()
            if (!canMutate(session, controller)) return defaults
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
        ): ListenableFuture<SessionResult> {
            if (!canMutate(session, controller)) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
            }
            if (service.mediaSession !== session) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_SESSION_DISCONNECTED))
            }
            return when (customCommand.customAction) {
                BA_GUIDE_BGM_COMMAND_TOGGLE_REPEAT -> {
                    toggleRepeat(session)
                    successResult()
                }

                BA_GUIDE_BGM_COMMAND_STOP_PLAYBACK -> {
                    service.stopPlaybackAndDismiss(session)
                    successResult()
                }

                else -> {
                    Futures.immediateFuture(
                        SessionResult(SessionError.ERROR_NOT_SUPPORTED),
                    )
                }
            }
        }

        private fun canMutate(session: MediaSession, controller: MediaSession.ControllerInfo): Boolean =
            controller.isTrusted || session.isMediaNotificationController(controller)

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

    private fun stopPlaybackAndDismiss(session: MediaSession) {
        // Media3 must deliver the successful command result before the session is released.
        mainHandler.post { stopPlaybackAndDismissOnMain(session) }
    }

    private fun stopPlaybackAndDismissOnMain(session: MediaSession) {
        if (mediaSession !== session) return
        // stopSelf alone cannot destroy a service while a UI controller remains bound.
        try {
            releasePlayback()
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            BaGuideBgmMediaNotificationProviderFactory.cancelNotification(this)
            stopSelf()
        }
    }

    private fun releasePlayback() {
        val session = mediaSession
        val sessionPlayer = player
        mediaSession = null
        player = null
        try {
            sessionPlayer?.removeListener(repeatModeListener)
            sessionPlayer?.playWhenReady = false
            sessionPlayer?.stop()
            sessionPlayer?.clearMediaItems()
            // Let Media3 withdraw its notification and invalidate pending artwork updates.
            triggerNotificationUpdate()
        } finally {
            try {
                if (session != null) {
                    try {
                        if (sessions.contains(session)) removeSession(session)
                    } finally {
                        session.release()
                    }
                }
            } finally {
                sessionPlayer?.release()
            }
        }
    }
}
