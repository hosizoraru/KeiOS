package os.kei.ui.page.main.settings.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
internal class SettingsAppLanguageController(
    private val appContext: Context
) {
    val actionAvailable: Boolean
        get() = buildAppLanguageSettingsIntent(appContext) != null

    fun openAppLanguageSettings(): Boolean {
        val intent = buildAppLanguageSettingsIntent(appContext) ?: return false
        return runCatching {
            appContext.startActivity(intent)
        }.isSuccess
    }
}

@Composable
internal fun rememberSettingsAppLanguageController(
    context: Context
): SettingsAppLanguageController {
    val appContext = context.applicationContext
    return remember(appContext) {
        SettingsAppLanguageController(appContext)
    }
}

private fun buildAppLanguageSettingsIntent(context: Context): Intent? {
    val packageManager = context.packageManager
    val packageUri = Uri.parse("package:${context.packageName}")
    val candidateIntents = listOf(
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = packageUri
        },
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    )
    return candidateIntents.firstOrNull { intent ->
        intent.resolveActivity(packageManager) != null
    }?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
