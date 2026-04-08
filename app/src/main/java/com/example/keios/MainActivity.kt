package com.example.keios

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
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
    private val shizukuApiUtils = ShizukuApiUtils()
    private lateinit var localMcpService: LocalMcpService
    private lateinit var mcpServerManager: McpServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val appLabel = runCatching {
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault("KeiOS")
        val packageInfo = runCatching {
            packageManager.getPackageInfoCompat(packageName)
        }.getOrNull()
        localMcpService = LocalMcpService(
            shizukuApiUtils = shizukuApiUtils,
            appVersionName = packageInfo?.versionName ?: "unknown",
            appVersionCode = packageInfo?.longVersionCode ?: -1L,
            appPackageName = packageName,
            appLabel = appLabel
        )
        mcpServerManager = McpServerManager(localMcpService)
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
}

private fun android.content.pm.PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
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
