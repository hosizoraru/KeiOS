package os.kei.ui.page.main.settings.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
internal class SettingsBatteryOptimizationController(
    private val appContext: Context
) {
    var ignoringBatteryOptimizations by mutableStateOf(false)
        private set

    var requestActionAvailable by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun refresh() {
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext)
        requestActionAvailable = buildBatteryOptimizationIntent(
            context = appContext,
            alreadyIgnored = ignoringBatteryOptimizations
        ) != null
    }

    fun openBatteryOptimizationSettings(): Boolean {
        val intent = buildBatteryOptimizationIntent(
            context = appContext,
            alreadyIgnored = ignoringBatteryOptimizations
        ) ?: return false
        return runCatching {
            appContext.startActivity(intent)
        }.isSuccess
    }
}

@Composable
internal fun rememberSettingsBatteryOptimizationController(
    context: Context
): SettingsBatteryOptimizationController {
    val appContext = context.applicationContext
    return remember(appContext) {
        SettingsBatteryOptimizationController(appContext)
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return runCatching {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)
}

private fun buildBatteryOptimizationIntent(
    context: Context,
    alreadyIgnored: Boolean
): Intent? {
    val packageManager = context.packageManager
    val packageUri = Uri.parse("package:${context.packageName}")
    val candidateIntents = buildList {
        if (!alreadyIgnored) {
            add(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri))
        }
        add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }
    return candidateIntents.firstOrNull { intent ->
        intent.resolveActivity(packageManager) != null
    }?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
