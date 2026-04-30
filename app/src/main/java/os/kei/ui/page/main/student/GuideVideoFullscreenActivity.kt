package os.kei.ui.page.main.student

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import os.kei.R
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.student.section.gallery.BindGuideVideoForegroundPlaybackGuard
import top.yukonga.miuix.kmp.basic.Text

class GuideVideoFullscreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = resolveVideoFullscreenOrientation()
        enableEdgeToEdge()

        val normalizedUrl = normalizeGuideMediaSource(
            intent?.getStringExtra(EXTRA_MEDIA_URL).orEmpty()
        )

        setContent {
            GuideVideoFullscreenScreen(
                mediaUrl = normalizedUrl,
                onClose = { finish() }
            )
        }
    }

    private fun resolveVideoFullscreenOrientation(): Int {
        val mediaAdaptiveRotationEnabled = BASettingsStore.loadMediaAdaptiveRotationEnabled()
        if (mediaAdaptiveRotationEnabled) {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        val systemAutoRotateEnabled = runCatching {
            Settings.System.getInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        }.getOrDefault(false)
        return if (systemAutoRotateEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    companion object {
        private const val EXTRA_MEDIA_URL = "extra_media_url"

        fun launch(
            context: Context,
            mediaUrl: String
        ) {
            val hostActivity = context.findHostActivity()
            val intent = Intent(context, GuideVideoFullscreenActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
                if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (hostActivity != null) {
                hostActivity.startActivity(intent)
            } else {
                context.startActivity(intent)
            }
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}

@Composable
private fun GuideVideoFullscreenScreen(
    mediaUrl: String,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val context = androidx.compose.ui.platform.LocalContext.current
    var loadError by remember(mediaUrl) { mutableStateOf<String?>(null) }

    val player = remember(context, mediaUrl) {
        if (mediaUrl.isBlank()) {
            null
        } else {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(mediaUrl))
                    playWhenReady = true
                    prepare()
                }
        }
    }

    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                loadError = error.errorCodeName
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            runCatching { boundPlayer.release() }
        }
    }
    BindGuideVideoForegroundPlaybackGuard(player = player)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val activePlayer = player
        if (activePlayer != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = activePlayer
                    }
                },
                update = { view ->
                    view.player = activePlayer
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            )
        } else {
            Text(
                text = stringResource(R.string.guide_media_video_url_invalid),
                color = Color(0xFFBFDBFE),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        loadError?.takeIf { it.isNotBlank() }?.let { err ->
            Text(
                text = stringResource(R.string.guide_gallery_video_failed_with_reason, err),
                color = Color(0xFFFCA5A5),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }

    }
}
