package os.kei.ui.page.main.settings.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.core.system.ShizukuApiUtils
import os.kei.feature.github.data.remote.GitHubVersionUtils

internal enum class SettingsAppListAccessMode {
    Direct,
    Shizuku,
    Restricted
}

@Stable
internal class SettingsPermissionKeepAliveController(
    private val appContext: Context,
    private val shizukuApiUtils: ShizukuApiUtils
) {
    var notificationsEnabled by mutableStateOf(false)
        private set

    var notificationSettingsActionAvailable by mutableStateOf(false)
        private set

    var shizukuGranted by mutableStateOf(false)
        private set

    var shizukuStatusText by mutableStateOf("")
        private set

    var appListAccessMode by mutableStateOf(SettingsAppListAccessMode.Restricted)
        private set

    var appListDetectedCount by mutableIntStateOf(0)
        private set

    var appListSettingsActionAvailable by mutableStateOf(false)
        private set

    suspend fun refresh(
        notificationPermissionGranted: Boolean,
        shizukuStatus: String
    ) {
        notificationsEnabled = notificationPermissionGranted &&
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        notificationSettingsActionAvailable = buildNotificationSettingsIntent(appContext) != null

        shizukuGranted = shizukuApiUtils.canUseCommand()
        shizukuStatusText = shizukuStatus.ifBlank { shizukuApiUtils.currentStatus() }

        appListSettingsActionAvailable = GitHubVersionUtils.buildAppListPermissionIntent(appContext) != null
        val appListState = withContext(Dispatchers.IO) {
            resolveAppListAccessState(appContext, shizukuApiUtils)
        }
        appListAccessMode = appListState.mode
        appListDetectedCount = appListState.detectedCount
    }

    fun openNotificationSettings(): Boolean {
        val intent = buildNotificationSettingsIntent(appContext) ?: return false
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    fun openAppListPermissionSettings(): Boolean {
        val intent = GitHubVersionUtils.buildAppListPermissionIntent(appContext) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }
}

@Composable
internal fun rememberSettingsPermissionKeepAliveController(
    context: Context,
    shizukuApiUtils: ShizukuApiUtils
): SettingsPermissionKeepAliveController {
    val appContext = context.applicationContext
    return remember(appContext, shizukuApiUtils) {
        SettingsPermissionKeepAliveController(
            appContext = appContext,
            shizukuApiUtils = shizukuApiUtils
        )
    }
}

private data class SettingsAppListAccessState(
    val mode: SettingsAppListAccessMode,
    val detectedCount: Int
)

private fun resolveAppListAccessState(
    context: Context,
    shizukuApiUtils: ShizukuApiUtils
): SettingsAppListAccessState {
    val shizukuPackageCount = queryShizukuPackageCount(shizukuApiUtils)
    if (shizukuPackageCount > 0) {
        return SettingsAppListAccessState(
            mode = SettingsAppListAccessMode.Shizuku,
            detectedCount = shizukuPackageCount
        )
    }

    val directApps = runCatching {
        GitHubVersionUtils.queryInstalledLaunchableApps(
            context = context,
            forceRefresh = true,
            ttlMs = 0L
        )
    }.getOrDefault(emptyList())
    if (directApps.isNotEmpty()) {
        return SettingsAppListAccessState(
            mode = SettingsAppListAccessMode.Direct,
            detectedCount = directApps.size
        )
    }

    return SettingsAppListAccessState(
        mode = SettingsAppListAccessMode.Restricted,
        detectedCount = 0
    )
}

private fun queryShizukuPackageCount(shizukuApiUtils: ShizukuApiUtils): Int {
    val output = shizukuApiUtils.execCommand("pm list packages", timeoutMs = 2500L).orEmpty()
    return output.lineSequence()
        .count { line -> line.startsWith("package:") }
        .coerceAtLeast(0)
}

private fun buildNotificationSettingsIntent(context: Context): Intent? {
    val packageManager = context.packageManager
    val packageUri = Uri.parse("package:${context.packageName}")
    val candidateIntents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }
    return candidateIntents.firstOrNull { intent ->
        intent.resolveActivity(packageManager) != null
    }?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
