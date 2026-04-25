package os.kei

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.metrics.performance.JankStats
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.core.perf.AppJankMonitor
import os.kei.core.shortcut.AppShortcuts
import os.kei.core.system.ShizukuApiUtils
import os.kei.mcp.server.LocalMcpService
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.ba.BaApNotificationDispatcher
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.host.main.MainScreen
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TARGET_BOTTOM_PAGE = "os.kei.extra.TARGET_BOTTOM_PAGE"
        const val EXTRA_MCP_SERVER_ACTION = "os.kei.extra.MCP_SERVER_ACTION"
        const val EXTRA_SHORTCUT_ACTION = "os.kei.extra.SHORTCUT_ACTION"
        const val TARGET_BOTTOM_PAGE_GITHUB = "GitHub"
        const val TARGET_BOTTOM_PAGE_MCP = "Mcp"
        const val TARGET_BOTTOM_PAGE_BA = "Ba"
        const val MCP_SERVER_ACTION_TOGGLE = "toggle"
        const val SHORTCUT_ACTION_BA_AP_ISLAND = "ba_ap_island"
        const val SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED = "github_refresh_tracked"
    }

    private var shizukuStatus = mutableStateOf("Shizuku status: initializing...")
    private var appThemeModeState = mutableStateOf(UiPrefs.getAppThemeMode())
    private var notificationPermissionGranted by mutableStateOf(true)
    private var requestedBottomPage by mutableStateOf<String?>(null)
    private var requestedBottomPageToken by mutableStateOf(0)
    private var requestedGitHubRefreshToken by mutableStateOf(0)
    private var pendingMcpServerAction: String? = null
    private var pendingShortcutAction: String? = null
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
        applyPendingShortcutActions()
        McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this)
        runCatching { AppShortcuts.sync(this) }

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
                CompositionLocalProvider(LocalOverscrollFactory provides null) {
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
                        requestedGitHubRefreshToken = requestedGitHubRefreshToken,
                        onRequestedBottomPageConsumed = {
                            requestedBottomPage = null
                        }
                    )
                }
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
        applyPendingShortcutActions()
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
        pendingShortcutAction = intent.getStringExtra(EXTRA_SHORTCUT_ACTION)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun applyPendingShortcutActions() {
        applyPendingMcpServerAction()
        applyPendingBaApIslandAction()
        applyPendingGitHubRefreshAction()
        pendingShortcutAction = null
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

    private fun applyPendingBaApIslandAction() {
        val action = pendingShortcutAction ?: return
        if (action != SHORTCUT_ACTION_BA_AP_ISLAND) return
        pendingShortcutAction = null
        val snapshot = BASettingsStore.loadSnapshot()
        val sent = BaApNotificationDispatcher.send(
            context = this,
            currentDisplay = snapshot.apCurrent.coerceAtLeast(0.0).toInt(),
            limitDisplay = snapshot.apLimit.coerceAtLeast(0),
            thresholdDisplay = snapshot.apNotifyThreshold.coerceAtLeast(0)
        )
        if (!sent) {
            Toast.makeText(
                this,
                getString(R.string.ba_toast_notification_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun applyPendingGitHubRefreshAction() {
        val action = pendingShortcutAction ?: return
        if (action != SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED) return
        pendingShortcutAction = null
        requestedGitHubRefreshToken += 1
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
