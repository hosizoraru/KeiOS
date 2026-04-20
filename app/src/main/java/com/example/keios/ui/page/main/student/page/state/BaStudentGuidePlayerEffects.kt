package com.example.keios.ui.page.main.student.page.state

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.keios.R
import com.example.keios.ui.page.main.student.BaGuideTempMediaCache
import com.example.keios.ui.page.main.student.GuideBottomTab
import com.example.keios.ui.page.main.student.clearGuideBgmLoopScope
import com.example.keios.ui.page.main.student.clearGuideBgmPlaybackScope
import kotlinx.coroutines.delay

@Composable
internal fun BindBaStudentGuidePlayerLifecycleEffects(
    context: Context,
    sourceUrl: String,
    voicePlayer: ExoPlayer
) {
    DisposableEffect(voicePlayer) {
        onDispose {
            runCatching { voicePlayer.release() }
        }
    }

    DisposableEffect(sourceUrl) {
        onDispose {
            clearGuideBgmLoopScope(sourceUrl)
            clearGuideBgmPlaybackScope(sourceUrl)
            BaGuideTempMediaCache.clearGuideCache(context, sourceUrl)
        }
    }
}

@Composable
internal fun BindBaStudentGuideVoiceListenerEffect(
    context: Context,
    voicePlayer: ExoPlayer,
    playingVoiceUrl: String,
    onPlayingVoiceUrlChange: (String) -> Unit,
    onIsVoicePlayingChange: (Boolean) -> Unit,
    onVoicePlayProgressChange: (Float) -> Unit
) {
    DisposableEffect(voicePlayer, playingVoiceUrl) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val active = isPlaying && playingVoiceUrl.isNotBlank()
                onIsVoicePlayingChange(active)
                if (!active) {
                    onVoicePlayProgressChange(0f)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                onVoicePlayProgressChange(0f)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        onPlayingVoiceUrlChange("")
                        onIsVoicePlayingChange(false)
                        onVoicePlayProgressChange(0f)
                    }

                    Player.STATE_READY -> {
                        onIsVoicePlayingChange(
                            voicePlayer.isPlaying && playingVoiceUrl.isNotBlank()
                        )
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlayingVoiceUrlChange("")
                onIsVoicePlayingChange(false)
                onVoicePlayProgressChange(0f)
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.guide_toast_voice_play_failed_with_reason,
                        error.errorCodeName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        voicePlayer.addListener(listener)
        onDispose {
            voicePlayer.removeListener(listener)
        }
    }
}

@Composable
internal fun BindBaStudentGuideVoiceProgressEffect(
    activeBottomTab: GuideBottomTab,
    isVoicePlaying: Boolean,
    playingVoiceUrl: String,
    voicePlayer: ExoPlayer,
    onVoicePlayProgressChange: (Float) -> Unit
) {
    val voiceTabActive = activeBottomTab == GuideBottomTab.Voice
    val voicePlayingForProgress = if (voiceTabActive) isVoicePlaying else false
    val voiceUrlForProgress = if (voiceTabActive) playingVoiceUrl else ""

    LaunchedEffect(voiceTabActive, voicePlayingForProgress, voiceUrlForProgress) {
        if (!voiceTabActive || !voicePlayingForProgress || voiceUrlForProgress.isBlank()) {
            onVoicePlayProgressChange(0f)
            return@LaunchedEffect
        }
        while (voiceTabActive && voicePlayingForProgress && voiceUrlForProgress.isNotBlank()) {
            val duration = voicePlayer.duration
            val position = voicePlayer.currentPosition
            onVoicePlayProgressChange(
                if (duration > 0L && position >= 0L) {
                    (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            )
            delay(220)
        }
    }
}
