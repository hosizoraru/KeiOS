@file:Suppress("FunctionName")

package os.kei

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import os.kei.core.ext.showToast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.ui.debug.DebugFpsOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.metrics.performance.JankStats
import os.kei.core.icon.LauncherIconController
import os.kei.core.perf.AppJankMonitor
import os.kei.core.platform.LocalNetworkPermissionCompat
import os.kei.core.platform.TransientExternalLaunchGuard
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.core.shortcut.AppShortcuts
import os.kei.core.shortcut.BaDailyShortcutSync
import os.kei.core.privilege.PrivilegedShell
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRuntime
import os.kei.feature.keepalive.service.AccessibilityGuardForegroundService
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.mcp.server.KeiOsMcpToolPlugins
import os.kei.mcp.server.LocalMcpService
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.ba.baCalendarPoolServerIndexOrNull
import os.kei.ui.page.main.ba.BaApIslandShortcutNotificationCoordinator
import os.kei.ui.page.main.host.main.MainHostCallbacks
import os.kei.ui.page.main.host.main.MainHostUiState
import os.kei.ui.page.main.host.main.MainScreen
import os.kei.ui.page.main.host.main.MainStartupSnapshot
import os.kei.ui.page.main.host.main.MainStartupViewModel
import os.kei.feature.home.data.HomeOverviewRepository
import os.kei.feature.home.model.HomeAppOverview
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeOverviewSnapshot
import os.kei.ui.page.main.widget.sheet.SceneBackdropHost
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.MiuixOverscrollFactory

private const val DEFERRED_ACTIVITY_STARTUP_WORK_DELAY_MS = 2_000L

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TARGET_BOTTOM_PAGE = "os.kei.extra.TARGET_BOTTOM_PAGE"
        const val EXTRA_TARGET_ROUTE = "os.kei.extra.TARGET_ROUTE"
        const val EXTRA_MCP_SERVER_ACTION = "os.kei.extra.MCP_SERVER_ACTION"
        const val EXTRA_SHORTCUT_ACTION = "os.kei.extra.SHORTCUT_ACTION"
        const val EXTRA_GITHUB_ACTIONS_TRACK_ID = "os.kei.extra.GITHUB_ACTIONS_TRACK_ID"
        const val EXTRA_BA_ACCOUNT_ID = "os.kei.extra.BA_ACCOUNT_ID"
        const val TARGET_BOTTOM_PAGE_OS = "Os"
        const val TARGET_BOTTOM_PAGE_GITHUB = "GitHub"
        const val TARGET_BOTTOM_PAGE_MCP = "Mcp"
        const val TARGET_BOTTOM_PAGE_BA = "Ba"
        const val TARGET_ROUTE_WEBDAV_SYNC = "WebDavSync"
        const val TARGET_ROUTE_BA_ACTIVITY_CALENDAR = "BaActivityCalendar"
        const val TARGET_ROUTE_BA_POOL = "BaPool"
        const val TARGET_ROUTE_OS_SHELL_RUNNER = "OsShellRunner"
        const val MCP_SERVER_ACTION_TOGGLE = "toggle"
        const val SHORTCUT_ACTION_BA_AP_ISLAND = "ba_ap_island"
        const val SHORTCUT_ACTION_BA_DAILY_DONE = "ba_daily_done"
        const val SHORTCUT_ACTION_BA_OPEN_BGM_PLAYBACK = "ba_open_bgm_playback"
        const val SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED = "github_refresh_tracked"
    }

    /**
     * Single Compose-observable holder for every Activity-owned UI signal that flows into
     * [MainScreen]. Collapsing 11 independent `mutableStateOf` fields into one [MainHostUiState]
     * lets `MainScreen` skip recomposition by structural equality and removes 19 individual
     * parameters from the `setContent` call site.
     */
    private var hostUiState by mutableStateOf(
        MainHostUiState.Initial.copy(appThemeMode = UiPrefs.getAppThemeMode())
    )
    private var pendingMcpServerAction: String? = null
    private var pendingShortcutAction: String? = null
    private var startMcpAfterLocalNetworkPermission = false
    private val privilegedShell = PrivilegedShell()
    private lateinit var localMcpService: LocalMcpService
    private lateinit var mcpServerManager: McpServerManager
    private var jankStats: JankStats? = null
    private lateinit var startupTransition: MainStartupTransition
    private val transientExternalLaunchGuard = TransientExternalLaunchGuard()
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hostUiState = hostUiState.copy(
                notificationPermissionGranted = granted,
                transientExternalLaunchActive = false
            )
            transientExternalLaunchGuard.clear()
        }
    private val requestLocalNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            transientExternalLaunchGuard.clear()
            hostUiState = hostUiState.copy(transientExternalLaunchActive = false)
            val permissionGranted = granted || hasLocalNetworkPermission()
            showToast(getString(
                if (permissionGranted) {
                    R.string.mcp_toast_local_network_permission_granted
                } else {
                    R.string.mcp_toast_local_network_permission_denied
                },
            ))
            val shouldStartMcp = startMcpAfterLocalNetworkPermission && permissionGranted
            startMcpAfterLocalNetworkPermission = false
            if (shouldStartMcp) {
                startMcpServerFromShortcutIfAllowed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // OEM tablets can still letterbox the manifest's phone portrait request even at API 37.
        // Release it before content/startup setup; phones retain their manifest orientation.
        if (resources.configuration.smallestScreenWidthDp >= 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        super.onCreate(savedInstanceState)
        startupTransition = MainStartupTransition(this)
        enableEdgeToEdge()
        LauncherIconController.applyDesign(this, UiPrefs.getLauncherIconDesign())
        window.isNavigationBarContrastEnforced = false
        val initialNotificationGranted = hasNotificationPermission()
        hostUiState = hostUiState.copy(notificationPermissionGranted = initialNotificationGranted)
        if (!initialNotificationGranted) {
            transientExternalLaunchGuard.markLaunching(
                TransientExternalLaunchGuard.Reason.NotificationPermission,
            )
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        consumeIntentNavigation(intent)
        restoreAccessibilityGuardForegroundServiceIfNeeded()

        val appLabel =
            runCatching {
                packageManager.getApplicationLabel(applicationInfo).toString()
            }.getOrDefault("KeiOS")
        val packageInfo =
            runCatching {
                packageManager.getPackageInfoCompat(packageName)
            }.getOrNull()
        localMcpService =
            LocalMcpService(
                appContext = applicationContext,
                privilegedShell = privilegedShell,
                appVersionName = packageInfo?.versionName ?: "unknown",
                appVersionCode = packageInfo?.longVersionCode ?: -1L,
                appPackageName = packageName,
                appLabel = appLabel,
                toolPlugins = KeiOsMcpToolPlugins.create(),
            )
        mcpServerManager =
            McpServerManager(
                appContext = applicationContext,
                localMcpService = localMcpService,
            )
        val initialAppOverview = HomeAppOverview(
            versionName = packageInfo?.versionName ?: BuildConfig.VERSION_NAME,
            versionCode = packageInfo?.longVersionCode ?: BuildConfig.VERSION_CODE.toLong(),
            loaded = true,
        )
        val startupContext = applicationContext
        val startupMcpState = mcpServerManager.uiState
        val fallbackTheme = hostUiState.appThemeMode
        val startupViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MainStartupViewModel::class.java))
                return MainStartupViewModel(
                    load = {
                        MainStartupSnapshot(
                            preferences = withContext(AppDispatchers.fileIo) { UiPrefs.loadSnapshot() },
                            homeOverview = HomeOverviewRepository(
                                context = startupContext,
                                mcpUiState = startupMcpState,
                            ).loadInitialOverview().copy(appOverview = initialAppOverview),
                        )
                    },
                    fallback = {
                        MainStartupSnapshot(
                            preferences = UiPrefs.defaultSnapshot().copy(appThemeMode = fallbackTheme),
                            homeOverview = HomeOverviewSnapshot(
                                appOverview = initialAppOverview,
                                githubOverview = HomeGitHubOverview(loaded = true),
                                baOverview = HomeBaOverview(loaded = true),
                            ),
                        )
                    },
                ) as T
            }
        })[MainStartupViewModel::class.java]
        applyPendingShortcutActions()
        // Defer non-first-frame work: shortcut sync writes to system storage, Xiaomi network
        // restoration may trigger system calls. Neither affects the first Compose frame.
        lifecycleScope.launch(AppDispatchers.fileIo) {
            delay(DEFERRED_ACTIVITY_STARTUP_WORK_DELAY_MS)
            // This persists a PackageManager override for the next launch, not the active view.
            // Keep its binder/storage work out of this launch's first frame.
            persistStartupTheme(hostUiState.appThemeMode)
            runCatching { McpNotificationHelper.restoreXiaomiNetworkIfNeeded(this@MainActivity) }
            runCatching { AppShortcuts.sync(this@MainActivity) }
            // Collapses every BA write down to real account-identity changes; see the class doc.
            runCatching { BaDailyShortcutSync.start(this@MainActivity, lifecycleScope) }
        }

        privilegedShell.attach { status ->
            hostUiState = hostUiState.copy(privilegeStatus = status)
        }

        // Stable callback bundle: created once and reused across recompositions so MainScreen does
        // not see new lambda identities every time hostUiState changes.
        val hostCallbacks = MainHostCallbacks(
            onCheckOrRequestPrivilege = { privilegedShell.requestAccessIfNeeded() },
            onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
            onAppThemeModeChanged = { mode ->
                hostUiState = hostUiState.copy(appThemeMode = mode)
                UiPrefs.setAppThemeMode(mode)
                lifecycleScope.launch(AppDispatchers.fileIo) { persistStartupTheme(mode) }
            },
            onRequestedBottomPageConsumed = {
                hostUiState = hostUiState.copy(requestedBottomPage = null)
            }
        )

        setContent {
            val state = hostUiState
            val startupSnapshot by startupViewModel.snapshot.collectAsStateWithLifecycle()
            val colorSchemeMode =
                when (state.appThemeMode) {
                    AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                    AppThemeMode.LIGHT -> ColorSchemeMode.Light
                    AppThemeMode.DARK -> ColorSchemeMode.Dark
                }
            val controller = ThemeController(colorSchemeMode)

            MiuixTheme(controller = controller) {
                CompositionLocalProvider(
                    LocalOverscrollFactory provides MiuixOverscrollFactory,
                    LocalMainStartupSnapshot provides startupSnapshot,
                    LocalMainStartupTransition provides startupTransition,
                ) {
                    SystemBarAutoStyle(state.appThemeMode)
                    Box(Modifier.fillMaxSize()) {
                        // Capture only real app content. The FPS overlay updates at 2 Hz and is
                        // intentionally kept outside this producer so debug telemetry cannot
                        // invalidate or become part of descendant Liquid Glass samples.
                        SceneBackdropHost(backgroundColor = MiuixTheme.colorScheme.background) {
                            if (startupSnapshot != null) {
                                MainScreen(
                                    appLabel = appLabel,
                                    hostState = state,
                                    hostCallbacks = hostCallbacks,
                                    privilegedShell = privilegedShell,
                                    mcpServerManager = mcpServerManager,
                                )
                            } else {
                                // A bounded draw gate protects launch. If storage is slow, keep a
                                // truthful brand surface instead of exposing default Home values.
                                val startupBackground = colorResource(when (state.appThemeMode) {
                                    AppThemeMode.FOLLOW_SYSTEM -> R.color.kei_startup_background
                                    AppThemeMode.LIGHT -> R.color.kei_startup_light
                                    AppThemeMode.DARK -> R.color.kei_startup_dark
                                })
                                Box(
                                    Modifier.fillMaxSize().background(startupBackground),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier.size(96.dp),
                                    )
                                }
                            }
                        }
                        if (BuildConfig.DEBUG) {
                            DebugFpsOverlay()
                        }
                    }
                }
            }
        }
        jankStats =
            AppJankMonitor.attach(
                window = window,
                // Perfetto supplies benchmark frame metrics without an in-process callback.
                // Keep JankStats as a debug diagnostic so benchmark and release share the same
                // frame-delivery path, especially on 120 Hz and ARR displays.
                enabled = BuildConfig.DEBUG,
            )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        transientExternalLaunchGuard.markLaunching(
            TransientExternalLaunchGuard.Reason.NotificationRoute,
        )
        setIntent(intent)
        consumeIntentNavigation(intent)
        applyPendingShortcutActions()
    }

    override fun onStop() {
        hostUiState = hostUiState.copy(
            transientExternalLaunchActive = transientExternalLaunchGuard.shouldDeferStopWork()
        )
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        transientExternalLaunchGuard.onResume()
        hostUiState = hostUiState.copy(transientExternalLaunchActive = false)
    }

    override fun onDestroy() {
        startupTransition.dispose()
        jankStats?.isTrackingEnabled = false
        jankStats = null
        // Only stop the MCP server when the user is intentionally leaving (back press / finish).
        // If the system is reclaiming the Activity while McpKeepAliveService is running as a
        // foreground service, the server should survive so connected clients are not dropped.
        if (isFinishing) {
            runCatching { mcpServerManager.stop() }
        }
        privilegedShell.detach()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        val granted = hasNotificationPermission()
        hostUiState = hostUiState.copy(notificationPermissionGranted = granted)
        if (!granted) {
            transientExternalLaunchGuard.markLaunching(
                TransientExternalLaunchGuard.Reason.NotificationPermission,
            )
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasNotificationPermission() =
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestLocalNetworkPermissionIfNeeded(startMcpAfterGrant: Boolean = false): Boolean {
        if (hasLocalNetworkPermission()) return true
        val permission = LocalNetworkPermissionCompat.requiredPermissionOrNull() ?: return true
        startMcpAfterLocalNetworkPermission = startMcpAfterGrant
        transientExternalLaunchGuard.markLaunching(
            TransientExternalLaunchGuard.Reason.LocalNetworkPermission,
        )
        requestLocalNetworkPermissionLauncher.launch(permission)
        showToast(getString(R.string.mcp_toast_local_network_permission_requested))
        return false
    }

    private fun hasLocalNetworkPermission(): Boolean = LocalNetworkPermissionCompat.hasPermission(this)

    private fun restoreAccessibilityGuardForegroundServiceIfNeeded() {
        lifecycleScope.launch(AppDispatchers.fileIo) {
            runCatching {
                val settings = AccessibilityGuardRuntime.newStateStore().loadSettings()
                if (settings.daemonEnabled) {
                    AccessibilityGuardForegroundService.start(applicationContext)
                }
            }
        }
    }

    private fun consumeIntentNavigation(intent: Intent?) {
        pendingMcpServerAction = null
        pendingShortcutAction = null
        val route =
            MainActivityIntentRouting.sanitize(
                rawTargetBottomPage = intent?.getStringExtra(EXTRA_TARGET_BOTTOM_PAGE),
                rawTargetRoute = intent?.getStringExtra(EXTRA_TARGET_ROUTE),
                rawMcpServerAction = intent?.getStringExtra(EXTRA_MCP_SERVER_ACTION),
                rawShortcutAction = intent?.getStringExtra(EXTRA_SHORTCUT_ACTION),
                rawGitHubActionsTrackId = intent?.getStringExtra(EXTRA_GITHUB_ACTIONS_TRACK_ID),
                rawBaAccountId = intent?.getStringExtra(EXTRA_BA_ACCOUNT_ID),
            ) ?: return
        val intentServerIndex = intent?.baCalendarPoolServerIndexOrNull()
        val previous = hostUiState
        val nextActionsTrackId = route.githubActionsTrackId ?: previous.requestedGitHubActionsTrackId
        val nextActionsSheetToken = if (route.githubActionsTrackId != null) {
            previous.requestedGitHubActionsSheetToken + 1
        } else {
            previous.requestedGitHubActionsSheetToken
        }
        val nextBaAccountToken = if (route.baAccountId != null) {
            previous.requestedBaAccountToken + 1
        } else {
            previous.requestedBaAccountToken
        }
        val nextWebDavSyncToken = if (route.targetRoute == TARGET_ROUTE_WEBDAV_SYNC) {
            previous.requestedWebDavSyncToken + 1
        } else {
            previous.requestedWebDavSyncToken
        }
        val nextOsShellRunnerToken = if (route.targetRoute == TARGET_ROUTE_OS_SHELL_RUNNER) {
            previous.requestedOsShellRunnerToken + 1
        } else {
            previous.requestedOsShellRunnerToken
        }
        val isBaCalendarPoolRoute =
            route.targetRoute == TARGET_ROUTE_BA_ACTIVITY_CALENDAR ||
                route.targetRoute == TARGET_ROUTE_BA_POOL
        val nextBaCalendarPoolToken = if (isBaCalendarPoolRoute) {
            previous.requestedBaCalendarPoolToken + 1
        } else {
            previous.requestedBaCalendarPoolToken
        }
        hostUiState = previous.copy(
            requestedBottomPage = route.targetBottomPage,
            requestedBottomPageToken = previous.requestedBottomPageToken + 1,
            requestedGitHubActionsTrackId = nextActionsTrackId,
            requestedGitHubActionsSheetToken = nextActionsSheetToken,
            requestedBaAccountId = route.baAccountId ?: previous.requestedBaAccountId,
            requestedBaAccountToken = nextBaAccountToken,
            requestedWebDavSyncToken = nextWebDavSyncToken,
            requestedOsShellRunnerToken = nextOsShellRunnerToken,
            requestedBaCalendarPoolRoute =
                if (isBaCalendarPoolRoute) route.targetRoute else previous.requestedBaCalendarPoolRoute,
            requestedBaCalendarPoolServerIndex =
                if (isBaCalendarPoolRoute) {
                    intentServerIndex
                } else {
                    previous.requestedBaCalendarPoolServerIndex
                },
            requestedBaCalendarPoolToken = nextBaCalendarPoolToken,
        )
        pendingMcpServerAction = route.mcpServerAction
        pendingShortcutAction = route.shortcutAction
    }

    private fun applyPendingShortcutActions() {
        applyPendingMcpServerAction()
        applyPendingBaApIslandAction()
        applyPendingBaBgmPlaybackAction()
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
            startMcpServerFromShortcutIfAllowed()
        }
    }

    private fun startMcpServerFromShortcutIfAllowed() {
        if (!::mcpServerManager.isInitialized) return
        val state = mcpServerManager.uiState.value
        if (state.running) return
        if (state.allowExternal && !requestLocalNetworkPermissionIfNeeded(startMcpAfterGrant = true)) return
        mcpServerManager.start(
            port = state.port,
            allowExternal = state.allowExternal,
        )
    }

    private fun applyPendingBaApIslandAction() {
        val action = pendingShortcutAction ?: return
        if (action != SHORTCUT_ACTION_BA_AP_ISLAND) return
        pendingShortcutAction = null
        val sent = BaApIslandShortcutNotificationCoordinator.send(this)
        if (!sent) {
            showToast(getString(R.string.ba_toast_notification_permission_required))
        }
    }

    private fun applyPendingGitHubRefreshAction() {
        val action = pendingShortcutAction ?: return
        if (action != SHORTCUT_ACTION_GITHUB_REFRESH_TRACKED) return
        pendingShortcutAction = null
        hostUiState = hostUiState.copy(
            requestedGitHubRefreshToken = hostUiState.requestedGitHubRefreshToken + 1
        )
    }

    private fun applyPendingBaBgmPlaybackAction() {
        val action = pendingShortcutAction ?: return
        if (action != SHORTCUT_ACTION_BA_OPEN_BGM_PLAYBACK) return
        pendingShortcutAction = null
        hostUiState = hostUiState.copy(
            requestedBaBgmPlaybackToken = hostUiState.requestedBaBgmPlaybackToken + 1
        )
    }
}

private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo = getPackageInfo(packageName, 0)

@Composable
@Suppress("DEPRECATION")
private fun SystemBarAutoStyle(appThemeMode: AppThemeMode) {
    val view = LocalView.current
    val darkTheme =
        when (appThemeMode) {
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
