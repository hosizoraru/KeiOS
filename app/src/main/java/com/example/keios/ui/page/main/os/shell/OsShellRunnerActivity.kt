package com.example.keios.ui.page.main.os.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.os.shell.page.OsShellRunnerPage
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class OsShellRunnerActivity : ComponentActivity() {
    private var shizukuStatus by mutableStateOf("Shizuku status: initializing...")
    private val shizukuApiUtils = ShizukuApiUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shizukuApiUtils.attach { status -> shizukuStatus = status }

        setContent {
            val appThemeMode = UiPrefs.getAppThemeMode()
            val colorSchemeMode = when (appThemeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                AppThemeMode.LIGHT -> ColorSchemeMode.Light
                AppThemeMode.DARK -> ColorSchemeMode.Dark
            }
            val controller = ThemeController(colorSchemeMode)

            MiuixTheme(controller = controller) {
                OsShellRunnerPage(
                    canRunShellCommand = shizukuStatus.contains("granted", ignoreCase = true) ||
                        shizukuApiUtils.canUseCommand(),
                    onRequestShizukuPermission = { shizukuApiUtils.requestPermissionIfNeeded() },
                    onRunShellCommand = { command ->
                        shizukuApiUtils.execCommandCancellable(command = command, timeoutMs = 300_000L)
                    },
                    onSaveShellCommand = { command, title, subtitle, runOutput ->
                        OsShellCommandCardStore.createCard(
                            command = command,
                            title = title,
                            subtitle = subtitle,
                            runOutput = runOutput
                        ) != null
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        shizukuApiUtils.detach()
        super.onDestroy()
    }

    companion object {
        fun launch(context: Context) {
            val hostActivity = context.findHostActivity()
            val intent = Intent(context, OsShellRunnerActivity::class.java).apply {
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
