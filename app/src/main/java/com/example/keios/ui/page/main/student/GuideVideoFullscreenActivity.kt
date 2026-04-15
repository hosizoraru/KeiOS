package com.example.keios.ui.page.main.student

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import top.yukonga.miuix.kmp.basic.Text

class GuideVideoFullscreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    companion object {
        private const val EXTRA_MEDIA_URL = "extra_media_url"

        fun launch(
            context: Context,
            mediaUrl: String
        ) {
            val intent = Intent(context, GuideVideoFullscreenActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

private fun normalizeGuideMediaSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
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
                text = "视频地址无效",
                color = Color(0xFFBFDBFE),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        loadError?.takeIf { it.isNotBlank() }?.let { err ->
            Text(
                text = "视频播放失败：$err",
                color = Color(0xFFFCA5A5),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }

        GlassTextButton(
            backdrop = null,
            text = "关闭",
            textColor = Color(0xFFBFDBFE),
            variant = GlassVariant.Compact,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp),
            onClick = onClose
        )
    }
}
