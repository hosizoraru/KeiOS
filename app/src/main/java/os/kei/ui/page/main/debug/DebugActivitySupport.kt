package os.kei.ui.page.main.debug

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.Composable
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

internal fun Context.launchDebugActivity(activityClass: Class<out Activity>) {
    val hostActivity = findHostActivity()
    val intent = Intent(this, activityClass).apply {
        if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (hostActivity != null) {
        hostActivity.startActivity(intent)
    } else {
        startActivity(intent)
    }
}

@Composable
internal fun DebugActivityTheme(content: @Composable () -> Unit) {
    val appThemeMode = UiPrefs.getAppThemeMode()
    val colorSchemeMode = when (appThemeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
        AppThemeMode.LIGHT -> ColorSchemeMode.Light
        AppThemeMode.DARK -> ColorSchemeMode.Dark
    }

    MiuixTheme(controller = ThemeController(colorSchemeMode)) {
        content()
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}
