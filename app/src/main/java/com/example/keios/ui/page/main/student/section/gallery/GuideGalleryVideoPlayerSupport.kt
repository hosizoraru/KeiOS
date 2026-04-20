package com.example.keios.ui.page.main.student.section.gallery

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.keios.ui.page.main.student.createGameKeeMediaSourceFactory

internal fun buildGuideVideoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
        .build()
}

@Composable
internal fun rememberGuidePreparedVideoPlayer(
    context: Context,
    mediaUrl: String,
    active: Boolean
): ExoPlayer? {
    return remember(context, mediaUrl, active) {
        if (!active || mediaUrl.isBlank()) {
            null
        } else {
            buildGuideVideoPlayer(context).apply {
                setMediaItem(MediaItem.fromUri(mediaUrl))
                playWhenReady = true
                prepare()
            }
        }
    }
}

@Composable
internal fun BindGuideVideoPlayerState(
    player: ExoPlayer?,
    onVideoRatioChanged: (Float) -> Unit = {},
    onBufferingChanged: (Boolean) -> Unit = {},
    onIsPlayingChanged: (Boolean) -> Unit = {},
    onPlayerErrorChanged: (String?) -> Unit = {},
    onDispose: () -> Unit = {}
) {
    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    onVideoRatioChanged(videoSize.width.toFloat() / videoSize.height.toFloat())
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                onBufferingChanged(playbackState == Player.STATE_BUFFERING)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onIsPlayingChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                onBufferingChanged(false)
                onPlayerErrorChanged(error.errorCodeName)
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            runCatching { boundPlayer.release() }
            onDispose()
        }
    }
}
