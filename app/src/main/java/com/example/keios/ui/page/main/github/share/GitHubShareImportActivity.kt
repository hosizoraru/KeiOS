package com.example.keios.ui.page.main.github.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.keios.MainActivity
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubShareIntentParser
import com.example.keios.ui.page.main.GitHubShareImportOverlayHost
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
                GitHubShareImportOverlayHost(
                    incomingGitHubShareText = incomingGitHubShareText,
                    incomingGitHubShareToken = incomingGitHubShareToken,
                    onIncomingGitHubShareConsumed = {
                        incomingGitHubShareText = null
                    },
                    onNavigateToGitHubPage = {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                putExtra(
                                    MainActivity.EXTRA_TARGET_BOTTOM_PAGE,
                                    MainActivity.TARGET_BOTTOM_PAGE_GITHUB
                                )
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
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
        if (intent?.action != Intent.ACTION_SEND) {
            finishSafely()
            return
        }
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
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
