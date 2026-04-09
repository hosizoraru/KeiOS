package com.example.keios

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.runtime.mutableStateOf
import com.example.keios.mcp.LocalMcpService
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.MainScreen
import com.example.keios.ui.utils.ShizukuApiUtils
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {

    private var shizukuStatus = mutableStateOf("Shizuku status: initializing...")
    private var notificationPermissionGranted by mutableStateOf(true)
    private val shizukuApiUtils = ShizukuApiUtils()
    private lateinit var localMcpService: LocalMcpService
    private lateinit var mcpServerManager: McpServerManager
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        notificationPermissionGranted = hasNotificationPermission()
        if (!notificationPermissionGranted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val appLabel = runCatching {
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault("KeiOS")
        val packageInfo = runCatching {
            packageManager.getPackageInfoCompat(packageName)
        }.getOrNull()
        localMcpService = LocalMcpService(
            appContext = applicationContext,
            shizukuApiUtils = shizukuApiUtils,
            appVersionName = packageInfo?.versionName ?: "unknown",
            appVersionCode = packageInfo?.longVersionCode ?: -1L,
            appPackageName = packageName,
            appLabel = appLabel
        )
        mcpServerManager = McpServerManager(
            appContext = applicationContext,
            localMcpService = localMcpService
        )
        val controller = ThemeController(ColorSchemeMode.System)

        shizukuApiUtils.attach { status ->
            shizukuStatus.value = status
        }
        runCatching { localMcpService.getOrCreateServer() }

        setContent {
            MiuixTheme(controller = controller) {
                SystemBarAutoStyle()
                MainScreen(
                    appLabel = appLabel,
                    packageInfo = packageInfo,
                    shizukuStatus = shizukuStatus.value,
                    onCheckOrRequestShizuku = { shizukuApiUtils.requestPermissionIfNeeded() },
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                    shizukuApiUtils = shizukuApiUtils,
                    mcpServerManager = mcpServerManager
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching { mcpServerManager.stop() }
        shizukuApiUtils.detach()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        notificationPermissionGranted = hasNotificationPermission()
        if (!notificationPermissionGranted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasNotificationPermission()=
         ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return getPackageInfo(packageName, 0)
}

@Composable
private fun SystemBarAutoStyle() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    val backgroundColor = MiuixTheme.colorScheme.background
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? MainActivity)?.window ?: return@SideEffect
            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = backgroundColor.copy(alpha = 0.85f).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
