package os.kei.ui.page.main.settings.support

import android.app.AppOpsManager
import android.content.ComponentName
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

internal enum class SettingsOemAutoStartState {
    Allowed,
    Restricted,
    Unknown,
    Unsupported
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

    var oemAutoStartState by mutableStateOf(SettingsOemAutoStartState.Unsupported)
        private set

    var oemAutoStartVendorLabel by mutableStateOf("")
        private set

    var oemAutoStartActionAvailable by mutableStateOf(false)
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
        val oemAutoStartSnapshot = resolveOemAutoStartSnapshot(appContext)
        oemAutoStartState = oemAutoStartSnapshot.state
        oemAutoStartVendorLabel = oemAutoStartSnapshot.vendorLabel
        oemAutoStartActionAvailable = oemAutoStartSnapshot.settingsActionAvailable

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

    fun openOemAutoStartSettings(): Boolean {
        val intent = buildOemAutoStartIntent(appContext) ?: return false
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

private data class SettingsOemAutoStartSnapshot(
    val state: SettingsOemAutoStartState,
    val vendorLabel: String,
    val settingsActionAvailable: Boolean
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

private fun resolveOemAutoStartSnapshot(context: Context): SettingsOemAutoStartSnapshot {
    val intent = buildOemAutoStartIntent(context)
    val vendorLabel = resolveOemAutoStartVendorLabel()
    if (intent == null) {
        return SettingsOemAutoStartSnapshot(
            state = SettingsOemAutoStartState.Unsupported,
            vendorLabel = vendorLabel,
            settingsActionAvailable = false
        )
    }
    val restricted = queryOemAutoStartRestriction(context)
    val state = when (restricted) {
        true -> SettingsOemAutoStartState.Restricted
        false -> SettingsOemAutoStartState.Allowed
        null -> SettingsOemAutoStartState.Unknown
    }
    return SettingsOemAutoStartSnapshot(
        state = state,
        vendorLabel = vendorLabel,
        settingsActionAvailable = true
    )
}

private fun buildOemAutoStartIntent(context: Context): Intent? {
    val packageManager = context.packageManager
    val packageName = context.packageName
    val candidates = buildList {
        add(
            Intent("miui.intent.action.OP_AUTO_START").apply {
                setPackage(OEM_SECURITY_CENTER_PACKAGE)
                putExtra("packageName", packageName)
                putExtra("package_name", packageName)
                putExtra("extra_pkgname", packageName)
            }
        )
        add(
            Intent().setComponent(
                ComponentName(
                    OEM_SECURITY_CENTER_PACKAGE,
                    OEM_AUTO_START_ACTIVITY
                )
            ).apply {
                putExtra("packageName", packageName)
                putExtra("package_name", packageName)
                putExtra("extra_pkgname", packageName)
            }
        )
    }
    return candidates.firstOrNull { intent ->
        intent.resolveActivity(packageManager) != null
    }?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private fun queryOemAutoStartRestriction(context: Context): Boolean? {
    queryAutoStartRestrictionViaInjector(context.packageName)?.let { return it }
    return queryAutoStartRestrictionViaAppOps(context)
}

private fun queryAutoStartRestrictionViaInjector(packageName: String): Boolean? {
    return runCatching {
        val method = Class.forName("android.app.AppOpsManagerInjector")
            .getDeclaredMethod("isAutoStartRestriction", String::class.java)
        method.isAccessible = true
        method.invoke(null, packageName) as? Boolean
    }.getOrNull()
}

private fun queryAutoStartRestrictionViaAppOps(context: Context): Boolean? {
    val appOpsManager = context.getSystemService(AppOpsManager::class.java) ?: return null
    val uid = context.applicationInfo.uid
    val mode = runCatching {
        val method = AppOpsManager::class.java.getMethod(
            "checkOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java
        )
        method.invoke(appOpsManager, OEM_OP_AUTO_START, uid, context.packageName) as? Int
    }.getOrNull() ?: return null
    return when (mode) {
        OEM_APP_OPS_MODE_ALLOWED -> false
        OEM_APP_OPS_MODE_IGNORED -> true
        else -> null
    }
}

private fun resolveOemAutoStartVendorLabel(): String {
    readSystemProperty("ro.mi.os.version.name")
        ?.takeIf { it.isNotBlank() }
        ?.let { return "HyperOS" }
    readSystemProperty("ro.miui.ui.version.name")
        ?.takeIf { it.isNotBlank() }
        ?.let { return "MIUI" }
    return when {
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) -> "Xiaomi"
        Build.BRAND.equals("Redmi", ignoreCase = true) -> "Redmi"
        Build.BRAND.equals("POCO", ignoreCase = true) -> "POCO"
        else -> "OEM"
    }
}

private fun readSystemProperty(key: String): String? {
    return runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("get", String::class.java)
        method.isAccessible = true
        method.invoke(null, key) as? String
    }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
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

private const val OEM_SECURITY_CENTER_PACKAGE = "com.miui.securitycenter"
private const val OEM_AUTO_START_ACTIVITY =
    "com.miui.permcenter.autostart.AutoStartManagementActivity"
private const val OEM_OP_AUTO_START = 10008
private const val OEM_APP_OPS_MODE_ALLOWED = 0
private const val OEM_APP_OPS_MODE_IGNORED = 1
