package os.kei.profilecapture

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import org.json.JSONObject
import os.kei.BuildConfig
import os.kei.ui.page.main.student.GuideWebMemoryLobbyActivity
import os.kei.ui.page.main.student.decodeWebMemoryLobby
import os.kei.ui.page.main.student.model3d.GuideModel3dActivity
import os.kei.ui.page.main.student.rendering.GuideViewerRendering
import os.kei.ui.page.main.student.rendering.GuideViewerRenderingStore

/** A bounded entry to real players in the disposable APK, independent of remote catalog order. */
class GuideViewerCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.APPLICATION_ID == "os.kei.profilecapture")
        // Explicit smoke diagnostics only; normal collection leaves remote debugging disabled.
        WebView.setWebContentsDebuggingEnabled(intent.getBooleanExtra("debug_web", false))
        val rendering = when (intent.getStringExtra("presentation")) {
            "system" -> GuideViewerRendering.SystemWebView
            "compatible" -> GuideViewerRendering.CompatibleFrames
            else -> error("Unknown profile presentation")
        }
        // This preference store belongs to the disposable target, never the user's installation.
        when (intent.getStringExtra("viewer")) {
            "model" -> {
                GuideViewerRenderingStore.setModel(rendering)
                startActivity(Intent(this, GuideModel3dActivity::class.java).putExtra("content_id", 690582L))
            }
            "lobby" -> {
                GuideViewerRenderingStore.setLobby(rendering)
                val json = assets.open("profile/lobby.json").bufferedReader().use { it.readText() }
                val resource = requireNotNull(decodeWebMemoryLobby(JSONObject(json)))
                GuideWebMemoryLobbyActivity.launch(this, resource)
            }
            else -> error("Unknown profile viewer")
        }
        finish()
    }
}
