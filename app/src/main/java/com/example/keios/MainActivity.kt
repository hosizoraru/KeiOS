package com.example.keios

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.metrics.performance.JankStats
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubShareIntentParser
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.perf.AppJankMonitor
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.mcp.LocalMcpService
import com.example.keios.mcp.McpNotificationHelper
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.MainScreen
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TARGET_BOTTOM_PAGE = "com.example.keios.extra.TARGET_BOTTOM_PAGE"
        const val EXTRA_MCP_SERVER_ACTION = "com.example.keios.extra.MCP_SERVER_ACTION"
        const val TARGET_BOTTOM_PAGE_GITHUB = "GitHub"
        const val TARGET_BOTTOM_PAGE_MCP = "Mcp"
        const val TARGET_BOTTOM_PAGE_BA = "Ba"
        const val MCP_SERVER_ACTION_TOGGLE = "toggle"
    }

    private var shizukuStatus = mutableStateOf("Shizuku status: initializing...")
    private var appThemeModeState = mutableStateOf(UiPrefs.getAppThemeMode())
    private var notificationPermissionGranted by mutableStateOf(true)
    private var requestedBottomPage by mutableStateOf<String?>(null)
    private var requestedBottomPageToken by mutableStateOf(0)
    private var incomingGitHubShareText by mutableStateOf<String?>(null)
    private var incomingGitHubShareToken by mutableStateOf(0)
    private var pendingMcpServerAction: String? = null
    private val shizukuApiUtils = ShizukuApiUtils()
    private lateinit var localMcpService: LocalMcpService
    private lateinit var mcpServerManager: McpServerManager
    private var jankStats: JankStats? = null
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyWindowColorMode(UiPrefs.isHomeIconHdrEnabled())
        window.isNavigationBarContrastEnforced = false
        notificationPermissionGranted = hasNotificationPermission()
        if (!notificationPermissionGranted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        consumeIntentNavigation(intent)

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
        applyPendingMcpServerAction()
        McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this)

        shizukuApiUtils.attach { status ->
            shizukuStatus.value = status
        }
        runCatching { localMcpService.getOrCreateServer() }

        setContent {
            val appThemeMode = appThemeModeState.value
            val colorSchemeMode = when (appThemeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                AppThemeMode.LIGHT -> ColorSchemeMode.Light
                AppThemeMode.DARK -> ColorSchemeMode.Dark
            }
            val controller = ThemeController(colorSchemeMode)

            MiuixTheme(controller = controller) {
                SystemBarAutoStyle(appThemeMode)
                MainScreen(
                    appLabel = appLabel,
                    packageInfo = packageInfo,
                    shizukuStatus = shizukuStatus.value,
                    onCheckOrRequestShizuku = { shizukuApiUtils.requestPermissionIfNeeded() },
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                    shizukuApiUtils = shizukuApiUtils,
                    mcpServerManager = mcpServerManager,
                    appThemeMode = appThemeMode,
                    onAppThemeModeChanged = { mode ->
                        appThemeModeState.value = mode
                        UiPrefs.setAppThemeMode(mode)
                    },
                    requestedBottomPage = requestedBottomPage,
                    requestedBottomPageToken = requestedBottomPageToken,
                    onRequestedBottomPageConsumed = {
                        requestedBottomPage = null
                    },
                    incomingGitHubShareText = incomingGitHubShareText,
                    incomingGitHubShareToken = incomingGitHubShareToken,
                    onIncomingGitHubShareConsumed = {
                        incomingGitHubShareText = null
                    }
                )
            }
        }
        jankStats = AppJankMonitor.attach(
            window = window,
            enabled = BuildConfig.DEBUG
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntentNavigation(intent)
        applyPendingMcpServerAction()
    }

    override fun onDestroy() {
        jankStats?.isTrackingEnabled = false
        jankStats = null
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

    private fun hasNotificationPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private fun applyWindowColorMode(hdrEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            window.colorMode = if (hdrEnabled) {
                ActivityInfo.COLOR_MODE_HDR
            } else {
                ActivityInfo.COLOR_MODE_DEFAULT
            }
        }
    }

    private fun consumeIntentNavigation(intent: Intent?) {
        consumeIncomingGitHubShare(intent)
        val target = intent?.getStringExtra(EXTRA_TARGET_BOTTOM_PAGE)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return
        requestedBottomPage = target
        requestedBottomPageToken += 1
        if (target == TARGET_BOTTOM_PAGE_MCP) {
            pendingMcpServerAction = intent.getStringExtra(EXTRA_MCP_SERVER_ACTION)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun consumeIncomingGitHubShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return
        if (!GitHubShareIntentParser.looksLikeGitHubShareText(sharedText)) return

        val shareImportEnabled = GitHubTrackStore.loadLookupConfig().shareImportLinkageEnabled
        if (!shareImportEnabled) return

        incomingGitHubShareText = sharedText
        incomingGitHubShareToken += 1
        requestedBottomPage = TARGET_BOTTOM_PAGE_GITHUB
        requestedBottomPageToken += 1
    }

    private fun applyPendingMcpServerAction() {
        if (!::mcpServerManager.isInitialized) return
        val action = pendingMcpServerAction ?: return
        pendingMcpServerAction = null
        if (action != MCP_SERVER_ACTION_TOGGLE) return

        val state = mcpServerManager.uiState.value
        if (state.running) {
            mcpServerManager.stop()
        } else {
            mcpServerManager.start(
                port = state.port,
                allowExternal = state.allowExternal
            )
        }
    }
}

private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return getPackageInfo(packageName, 0)
}

@Composable
private fun SystemBarAutoStyle(appThemeMode: AppThemeMode) {
    val view = LocalView.current
    val darkTheme = when (appThemeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
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
