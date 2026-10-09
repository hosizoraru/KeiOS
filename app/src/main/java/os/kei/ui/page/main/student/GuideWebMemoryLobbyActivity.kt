package os.kei.ui.page.main.student

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import os.kei.ui.page.main.ba.BaStandaloneActivityTheme
import os.kei.ui.page.main.student.section.gallery.GuideWebMemoryLobbyScreen
import os.kei.ui.page.main.widget.sheet.SceneBackdropHost
import org.json.JSONObject
import os.kei.ui.page.main.student.rendering.GuideViewerRenderingStore
import os.kei.ui.page.main.student.rendering.GuideViewerRendering

/** A media host independent of the gallery's lazy items and portrait-only navigation. */
class GuideWebMemoryLobbyActivity : ComponentActivity() {
    private var resource by mutableStateOf<BaGuideWebMemoryLobby?>(null)
    private var rendering by mutableStateOf(GuideViewerRendering.SystemWebView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!readViewerIntent(intent)) return
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            BaStandaloneActivityTheme {
                SceneBackdropHost(backgroundColor = androidx.compose.ui.graphics.Color.Black) {
                    val lobby = resource ?: return@SceneBackdropHost
                    GuideWebMemoryLobbyScreen(
                        resource = lobby,
                        onDismiss = ::finish,
                        onControlsVisibleChange = ::updateSystemBars,
                        rendering = rendering,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readViewerIntent(intent)
    }

    private fun updateSystemBars(visible: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (visible) show(WindowInsetsCompat.Type.systemBars())
            else hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun readViewerIntent(intent: Intent): Boolean {
        resource = runCatching {
            val raw = intent.getStringExtra(EXTRA_RESOURCE).orEmpty()
            if (raw.length > 64 * 1024) null else decodeWebMemoryLobby(JSONObject(raw))
        }.getOrNull()
        if (resource != null) {
            rendering = GuideViewerRenderingStore.preferences.value.lobby
            return true
        }
        finish()
        return false
    }

    companion object {
        private const val EXTRA_RESOURCE = "lobby_resource"

        fun launch(context: Context, resource: BaGuideWebMemoryLobby) {
            val validated = decodeWebMemoryLobby(resource.toJson()) ?: return
            context.startActivity(Intent(context, GuideWebMemoryLobbyActivity::class.java)
                .putExtra(EXTRA_RESOURCE, validated.toJson().toString())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
}
