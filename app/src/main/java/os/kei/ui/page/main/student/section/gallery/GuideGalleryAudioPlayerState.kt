package os.kei.ui.page.main.student.section.gallery

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import os.kei.ui.page.main.student.GuideBgmLoopStore
import os.kei.ui.page.main.student.GuideBgmPlayerStore
import kotlinx.coroutines.delay

@Stable
internal class GuideGalleryAudioPlayerState(
    val audioTargetUrl: String,
    val player: Player?,
    private val audioLoopScopeKey: String
) {
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var playProgress by mutableFloatStateOf(0f)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var seekProgress by mutableStateOf<Float?>(null)
    var loadError by mutableStateOf<String?>(null)
    var loopEnabled by mutableStateOf(
        audioTargetUrl.isNotBlank() && GuideBgmLoopStore.isEnabled(audioLoopScopeKey, audioTargetUrl)
    )

    val resolvedDurationMs: Long
        get() = maxOf(durationMs, player?.duration?.coerceAtLeast(0L) ?: 0L)

    val displayProgress: Float
        get() = (seekProgress ?: playProgress).coerceIn(0f, 1f)

    val displayPositionMs: Long
        get() {
            val preview = seekProgress?.coerceIn(0f, 1f)
            val resolvedDuration = resolvedDurationMs
            val resolvedPosition = when {
                preview != null && resolvedDuration > 0L -> (resolvedDuration * preview).toLong()
                else -> positionMs
            }.coerceAtLeast(0L)
            return if (resolvedDuration > 0L) {
                resolvedPosition.coerceAtMost(resolvedDuration)
            } else {
                resolvedPosition
            }
        }

    fun toggleLoop() {
        updateLoopEnabled(!loopEnabled)
    }

    fun updateLoopEnabled(enabled: Boolean) {
        loopEnabled = enabled
        GuideBgmLoopStore.setEnabled(
            scopeKey = audioLoopScopeKey,
            audioUrl = audioTargetUrl,
            enabled = enabled
        )
        player?.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun play(context: Context, restart: Boolean = false) {
        val currentPlayer = player ?: run {
            Toast.makeText(context, "音频地址无效", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            loadError = null
            GuideBgmPlayerStore.pauseScopeExcept(audioLoopScopeKey, audioTargetUrl)
            if (currentPlayer.currentMediaItem == null) {
                currentPlayer.setMediaItem(MediaItem.fromUri(audioTargetUrl))
                currentPlayer.prepare()
            } else if (restart || currentPlayer.playbackState == Player.STATE_ENDED) {
                currentPlayer.seekTo(0)
            }
            currentPlayer.play()
        }.onFailure {
            loadError = it.message
            Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePlay(context: Context) {
        val currentPlayer = player ?: run {
            Toast.makeText(context, "音频地址无效", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            loadError = null
            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
            } else {
                play(context)
            }
        }.onFailure {
            loadError = it.message
            Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun startSeekPreview() {
        seekProgress = displayProgress
    }

    fun updateSeekPreview(fraction: Float) {
        seekProgress = fraction
    }

    fun finishSeek(fraction: Float) {
        val currentPlayer = player ?: run {
            seekProgress = null
            return
        }
        val duration = maxOf(
            resolvedDurationMs,
            currentPlayer.duration.coerceAtLeast(0L)
        )
        if (duration <= 0L) {
            seekProgress = null
            return
        }
        val targetMs = (duration * fraction.coerceIn(0f, 1f)).toLong()
            .coerceIn(0L, duration)
        runCatching { currentPlayer.seekTo(targetMs) }
        durationMs = duration
        positionMs = targetMs
        playProgress = (targetMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        seekProgress = null
    }
}

@Composable
internal fun rememberGuideGalleryAudioPlayerState(
    context: Context,
    audioLoopScopeKey: String,
    audioTargetUrl: String
): GuideGalleryAudioPlayerState {
    val audioPlayer = remember(context, audioLoopScopeKey, audioTargetUrl) {
        GuideBgmPlayerStore.getOrCreate(
            context = context,
            scopeKey = audioLoopScopeKey,
            audioUrl = audioTargetUrl
        )
    }
    return remember(audioLoopScopeKey, audioTargetUrl, audioPlayer) {
        GuideGalleryAudioPlayerState(
            audioTargetUrl = audioTargetUrl,
            player = audioPlayer,
            audioLoopScopeKey = audioLoopScopeKey
        )
    }
}

@Composable
internal fun BindGuideGalleryAudioPlayerEffects(
    state: GuideGalleryAudioPlayerState,
    onPlaybackEnded: () -> Unit = {}
) {
    val player = state.player
    val latestOnPlaybackEnded by rememberUpdatedState(onPlaybackEnded)
    LaunchedEffect(player, state.loopEnabled) {
        player?.repeatMode = if (state.loopEnabled) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(player, state.audioTargetUrl, state.loopEnabled) {
        val currentPlayer = player ?: return@DisposableEffect onDispose { }
        state.isPlaying = currentPlayer.isPlaying
        val initialDuration = currentPlayer.duration
        if (initialDuration > 0L) {
            state.durationMs = initialDuration
        }
        val initialPosition = currentPlayer.currentPosition.coerceAtLeast(0L)
        state.positionMs = if (state.durationMs > 0L) {
            initialPosition.coerceAtMost(state.durationMs)
        } else {
            initialPosition
        }
        state.playProgress = if (state.durationMs > 0L) {
            (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                state.isPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> state.isBuffering = true
                    Player.STATE_READY -> {
                        state.isBuffering = false
                        val duration = currentPlayer.duration
                        if (duration > 0L) {
                            state.durationMs = duration
                        }
                        val position = currentPlayer.currentPosition
                        if (position >= 0L) {
                            state.positionMs = if (state.durationMs > 0L) {
                                position.coerceAtMost(state.durationMs)
                            } else {
                                position
                            }
                        }
                    }

                    Player.STATE_ENDED -> {
                        if (state.loopEnabled && currentPlayer.repeatMode == Player.REPEAT_MODE_ONE) {
                            return
                        }
                        state.isBuffering = false
                        state.isPlaying = false
                        state.playProgress = 1f
                        val duration = currentPlayer.duration
                        if (duration > 0L) {
                            state.durationMs = duration
                            state.positionMs = duration
                        }
                        latestOnPlaybackEnded()
                    }

                    Player.STATE_IDLE -> {
                        state.isBuffering = false
                        if (!currentPlayer.isPlaying) {
                            state.playProgress = 0f
                            state.positionMs = 0L
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                state.isBuffering = false
                state.isPlaying = false
                state.loadError = error.errorCodeName
            }
        }
        currentPlayer.addListener(listener)
        onDispose { currentPlayer.removeListener(listener) }
    }
    LaunchedEffect(
        state.audioTargetUrl,
        state.isPlaying,
        state.isBuffering,
        state.seekProgress,
        player
    ) {
        val currentPlayer = player ?: run {
            state.playProgress = 0f
            state.positionMs = 0L
            state.durationMs = 0L
            return@LaunchedEffect
        }

        if (state.seekProgress != null) return@LaunchedEffect

        if (!state.isPlaying && !state.isBuffering) {
            val duration = currentPlayer.duration
            if (duration > 0L) {
                state.durationMs = duration
            }
            val position = currentPlayer.currentPosition.coerceAtLeast(0L)
            state.positionMs = if (state.durationMs > 0L) {
                position.coerceAtMost(state.durationMs)
            } else {
                position
            }
            state.playProgress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            return@LaunchedEffect
        }

        while ((state.isPlaying || state.isBuffering) && state.seekProgress == null) {
            val duration = currentPlayer.duration
            val position = currentPlayer.currentPosition
            if (duration > 0L) {
                state.durationMs = duration
            }
            state.positionMs = if (position >= 0L) {
                if (state.durationMs > 0L) {
                    position.coerceAtMost(state.durationMs)
                } else {
                    position
                }
            } else {
                0L
            }
            state.playProgress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(200)
        }
    }
}
