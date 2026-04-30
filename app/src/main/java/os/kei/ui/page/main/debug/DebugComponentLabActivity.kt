package os.kei.ui.page.main.debug

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class DebugComponentLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DebugActivityTheme {
                DebugComponentLabPage(
                    onClose = { finish() },
                    onOpenBgmMusic = { DebugBgmMusicActivity.launch(this) }
                )
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.launchDebugActivity(DebugComponentLabActivity::class.java)
        }
    }
}
