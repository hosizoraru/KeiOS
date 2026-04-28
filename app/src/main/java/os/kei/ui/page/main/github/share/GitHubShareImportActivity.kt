package os.kei.ui.page.main.github.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import os.kei.MainActivity
import os.kei.R
import os.kei.core.intent.SafeExternalIntents
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.remote.GitHubShareIntentParser
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class GitHubShareImportActivity : ComponentActivity() {
    private var incomingGitHubShareText by mutableStateOf<String?>(null)
    private var incomingGitHubShareToken by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeIncomingShareIntent(intent)
        if (isFinishing) return

        setContent {
            val appThemeMode = UiPrefs.getAppThemeMode()
            val colorSchemeMode = when (appThemeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                AppThemeMode.LIGHT -> ColorSchemeMode.Light
                AppThemeMode.DARK -> ColorSchemeMode.Dark
            }
            val controller = ThemeController(colorSchemeMode)

            MiuixTheme(controller = controller) {
                Box(modifier = Modifier.fillMaxSize())
                GitHubShareImportWindowFlowHost(
                    incomingGitHubShareText = incomingGitHubShareText,
                    incomingGitHubShareToken = incomingGitHubShareToken,
                    onIncomingGitHubShareConsumed = {
                        incomingGitHubShareText = null
                    },
                    onNavigateToGitHubPage = {
                        val targetIntent = Intent(this, MainActivity::class.java).apply {
                            putExtra(
                                MainActivity.EXTRA_TARGET_BOTTOM_PAGE,
                                MainActivity.TARGET_BOTTOM_PAGE_GITHUB
                            )
                            addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }
                        val launched = runCatching {
                            startActivity(targetIntent)
                            true
                        }.getOrElse {
                            runCatching {
                                startActivity(
                                    Intent(this, MainActivity::class.java).apply {
                                        putExtra(
                                            MainActivity.EXTRA_TARGET_BOTTOM_PAGE,
                                            MainActivity.TARGET_BOTTOM_PAGE_GITHUB
                                        )
                                    }
                                )
                                true
                            }.getOrDefault(false)
                        }
                        if (!launched) {
                            Toast.makeText(
                                this,
                                getString(R.string.common_open_link_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finishSafely()
                    },
                    showPendingArmedSheet = true,
                    onClosePendingArmedSheet = { finishSafely() },
                    onIdleWithNoPendingFlow = { finishSafely() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingShareIntent(intent)
    }

    private fun consumeIncomingShareIntent(intent: Intent?) {
        if (!SafeExternalIntents.isPlainTextSend(intent)) {
            finishSafely()
            return
        }
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: run {
                finishSafely()
                return
            }
        if (!GitHubShareIntentParser.looksLikeGitHubShareText(sharedText)) {
            finishSafely()
            return
        }
        val shareImportEnabled = GitHubTrackStore.loadLookupConfig().shareImportLinkageEnabled
        if (!shareImportEnabled) {
            finishSafely()
            return
        }
        incomingGitHubShareText = sharedText
        incomingGitHubShareToken += 1
    }

    private fun finishSafely() {
        if (!isFinishing) {
            finish()
        }
    }
}
