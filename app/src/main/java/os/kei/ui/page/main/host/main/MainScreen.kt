package os.kei.ui.page.main.host.main

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import os.kei.MainActivity
import os.kei.LocalMainStartupSnapshot
import os.kei.LocalMainStartupTransition
import os.kei.R
import os.kei.core.privilege.PrivilegedShell
import os.kei.mcp.server.McpServerManager
import os.kei.ui.navigation.KeiosRoute
import os.kei.ui.navigation.Navigator
import os.kei.ui.page.main.model.BottomPage
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack

@Composable
fun MainScreen(
    appLabel: String,
    hostState: MainHostUiState,
    hostCallbacks: MainHostCallbacks,
    privilegedShell: PrivilegedShell,
    mcpServerManager: McpServerManager
) {
    // The reified route supertype captures the whole sealed hierarchy reflection-free, so the
    // stack persists across configuration changes and process death without a SerializersModule.
    val backStack = rememberNavBackStack<KeiosRoute>(KeiosRoute.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val context = LocalContext.current
    val appContext = context.applicationContext
    val view = LocalView.current
    val currentAppLabel by rememberUpdatedState(appLabel)
    val currentPrivilegeStatus by rememberUpdatedState(hostState.privilegeStatus)
    val currentNotificationPermissionGranted by rememberUpdatedState(hostState.notificationPermissionGranted)
    val currentOnCheckOrRequestPrivilege by rememberUpdatedState(hostCallbacks.onCheckOrRequestPrivilege)
    val currentOnAppThemeModeChanged by rememberUpdatedState(hostCallbacks.onAppThemeModeChanged)
    val initialPreferences = LocalMainStartupSnapshot.current?.preferences
    val startupTransition = LocalMainStartupTransition.current
    val prefsViewModel: MainScreenPrefsViewModel = viewModel(
        factory = remember(initialPreferences) { MainScreenPrefsViewModel.factory(initialPreferences) },
    )
    val guideNavigationViewModel: MainScreenGuideNavigationViewModel = viewModel()
    var localRequestedBottomPage by rememberSaveable { mutableStateOf<String?>(null) }
    var localRequestedBottomPageToken by rememberSaveable { mutableIntStateOf(0) }
    var localRequestedGitHubActionsTrackId by rememberSaveable { mutableStateOf<String?>(null) }
    var localRequestedGitHubActionsSheetToken by rememberSaveable { mutableIntStateOf(0) }
    var preferLocalActionsRequest by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hostState.requestedBottomPageToken) {
        if (hostState.requestedBottomPageToken > 0) {
            localRequestedGitHubActionsTrackId = null
            preferLocalActionsRequest = false
        }
    }
    val externalBottomPageRequested = !hostState.requestedBottomPage.isNullOrBlank()
    val externalGitHubActionsRequested =
        !preferLocalActionsRequest &&
            hostState.requestedGitHubActionsSheetToken > 0 &&
            !hostState.requestedGitHubActionsTrackId.isNullOrBlank()
    val effectiveRequestedBottomPage = if (externalBottomPageRequested) {
        hostState.requestedBottomPage
    } else {
        localRequestedBottomPage
    }
    val effectiveRequestedBottomPageToken = if (externalBottomPageRequested) {
        hostState.requestedBottomPageToken
    } else {
        localRequestedBottomPageToken
    }
    val effectiveRequestedGitHubActionsTrackId = if (externalGitHubActionsRequested) {
        hostState.requestedGitHubActionsTrackId
    } else if (externalBottomPageRequested) {
        null
    } else {
        localRequestedGitHubActionsTrackId
    }
    val effectiveRequestedGitHubActionsSheetToken = if (externalGitHubActionsRequested) {
        hostState.requestedGitHubActionsSheetToken
    } else {
        // Local history and platform notification counters must not deduplicate each other.
        -localRequestedGitHubActionsSheetToken
    }
    LaunchedEffect(prefsViewModel) {
        prefsViewModel.loadInitialSnapshot()
    }
    val uiPrefsSnapshot by prefsViewModel.snapshot.collectAsStateWithLifecycle()
    val destinationReady = isMainStartupDestinationReady(hostState, backStack.lastOrNull() as? KeiosRoute)
    SideEffect {
        startupTransition?.onContentReady(
            ready = destinationReady,
            transitionAnimationsEnabled = uiPrefsSnapshot.transitionAnimationsEnabled,
            waitForMainPage = backStack.lastOrNull() == KeiosRoute.Main,
        )
    }
    val mainReturnState = rememberMainScreenSettingsReturnState(backStack)
    BindMainScreenBottomPageReturnEffect(
        requestedBottomPageToken = effectiveRequestedBottomPageToken,
        requestedBottomPage = effectiveRequestedBottomPage,
        onReturnToMain = {
            navigator.popUntil { it == KeiosRoute.Main }
        }
    )
    LaunchedEffect(hostState.requestedBaBgmPlaybackToken) {
        if (hostState.requestedBaBgmPlaybackToken <= 0) return@LaunchedEffect
        navigator.popUntil { it == KeiosRoute.Main }
        navigator.push(
            KeiosRoute.BaGuideCatalog(
                openBgmPlaybackToken = hostState.requestedBaBgmPlaybackToken.toLong()
            )
        )
    }
    LaunchedEffect(hostState.requestedWebDavSyncToken) {
        if (hostState.requestedWebDavSyncToken <= 0) return@LaunchedEffect
        navigator.popUntil { it == KeiosRoute.Main }
        navigator.pushSingleTop(KeiosRoute.WebDavSync)
    }
    LaunchedEffect(hostState.requestedOsShellRunnerToken) {
        if (hostState.requestedOsShellRunnerToken <= 0) return@LaunchedEffect
        navigator.popUntil { it == KeiosRoute.Main }
        navigator.pushSingleTop(KeiosRoute.OsShellRunner)
    }
    LaunchedEffect(hostState.requestedBaCalendarPoolToken) {
        if (hostState.requestedBaCalendarPoolToken <= 0) return@LaunchedEffect
        val serverIndex = hostState.requestedBaCalendarPoolServerIndex
        val nonce = hostState.requestedBaCalendarPoolToken.toLong()
        navigator.popUntil { it == KeiosRoute.Main }
        when (hostState.requestedBaCalendarPoolRoute) {
            MainActivity.TARGET_ROUTE_BA_ACTIVITY_CALENDAR ->
                navigator.push(
                    KeiosRoute.BaCalendarPool(serverIndex = serverIndex, nonce = nonce),
                )

            // Same route, opened on its other tab. A banner notification still lands on the banners.
            MainActivity.TARGET_ROUTE_BA_POOL ->
                navigator.push(
                    KeiosRoute.BaCalendarPool(
                        serverIndex = serverIndex,
                        showPool = true,
                        nonce = nonce,
                    ),
                )
        }
    }
    LaunchedEffect(guideNavigationViewModel, navigator) {
        guideNavigationViewModel.events.collect { event ->
            when (event) {
                is MainScreenGuideNavigationEvent.OpenStudentGuide -> {
                    navigator.push(KeiosRoute.BaStudentGuide(nonce = event.warmStartId))
                }
            }
        }
    }
    val uiPrefsState = rememberMainScreenUiPrefsState(
        snapshot = uiPrefsSnapshot,
        appContext = appContext,
        mcpServerManager = mcpServerManager,
        viewModel = prefsViewModel
    )
    val poolGuideMissingText = stringResource(R.string.main_toast_pool_guide_missing)
    val externalOpenFailureText = stringResource(R.string.ba_error_open_activity_link)
    val openGuideDetail = rememberMainScreenOpenGuideDetailAction(
        poolGuideMissingText = poolGuideMissingText,
        externalOpenFailureText = externalOpenFailureText,
        onNavigateToCanonicalGuide = { canonicalGuideUrl ->
            guideNavigationViewModel.saveAndOpenCanonicalGuide(canonicalGuideUrl)
        }
    )
    val pagerCoordinator =
        remember(
            mainReturnState.settingsReturnToken,
            uiPrefsState,
            currentPrivilegeStatus,
            privilegedShell,
            mcpServerManager,
            openGuideDetail,
            effectiveRequestedBottomPage,
            effectiveRequestedBottomPageToken,
            hostState.requestedGitHubRefreshToken,
            effectiveRequestedGitHubActionsTrackId,
            effectiveRequestedGitHubActionsSheetToken,
            hostState.requestedBaAccountId,
            hostState.requestedBaAccountToken,
            externalBottomPageRequested,
            hostCallbacks,
        ) {
            buildMainScreenPagerCoordinator(
                settingsReturnToken = mainReturnState.settingsReturnToken,
                prefsState = uiPrefsState,
                privilegeStatus = currentPrivilegeStatus,
                privilegedShell = privilegedShell,
                mcpServerManager = mcpServerManager,
                onOpenGuideDetail = openGuideDetail,
                requestedBottomPage = effectiveRequestedBottomPage,
                requestedBottomPageToken = effectiveRequestedBottomPageToken,
                requestedGitHubRefreshToken = hostState.requestedGitHubRefreshToken,
                requestedGitHubActionsTrackId = effectiveRequestedGitHubActionsTrackId,
                requestedGitHubActionsSheetToken = effectiveRequestedGitHubActionsSheetToken,
                requestedBaAccountId = hostState.requestedBaAccountId,
                requestedBaAccountToken = hostState.requestedBaAccountToken,
                onRequestedBottomPageConsumed = {
                    if (externalBottomPageRequested) {
                        hostCallbacks.onRequestedBottomPageConsumed()
                    }
                    localRequestedBottomPage = null
                },
                onBaGuideCatalogOpen = {
                    localRequestedBottomPage = BottomPage.Ba.name
                    localRequestedBottomPageToken += 1
                },
                onBaGuideCatalogBack = {
                    localRequestedBottomPage = BottomPage.Ba.name
                    localRequestedBottomPageToken += 1
                },
                // The nonce keeps the content key unique when the same server is opened again while
                // an earlier instance is still on the stack; NavDisplay rejects duplicate keys.
                onOpenBaCalendarPool = { serverIndex ->
                    navigator.push(
                        KeiosRoute.BaCalendarPool(
                            serverIndex = serverIndex,
                            nonce = SystemClock.elapsedRealtimeNanos(),
                        ),
                    )
                },
            )
        }
    MainScreenNavHost(
        backStack = backStack,
        navigator = navigator,
        pagerCoordinator = pagerCoordinator,
        externalNavigationToken = hostState.requestedBottomPageToken,
        prefsState = uiPrefsState,
        appLabel = currentAppLabel,
        onCheckOrRequestPrivilege = currentOnCheckOrRequestPrivilege,
        notificationPermissionGranted = currentNotificationPermissionGranted,
        onRequestNotificationPermission = hostCallbacks.onRequestNotificationPermission,
        mcpServerManager = mcpServerManager,
        appThemeMode = hostState.appThemeMode,
        transientExternalLaunchActive = hostState.transientExternalLaunchActive,
        onAppThemeModeChanged = currentOnAppThemeModeChanged,
        onOpenGitHubActionsTrackFromHistory = { trackId ->
            val normalizedTrackId = trackId.trim()
            if (normalizedTrackId.isNotBlank()) {
                localRequestedBottomPage = BottomPage.GitHub.name
                localRequestedBottomPageToken += 1
                localRequestedGitHubActionsTrackId = normalizedTrackId
                localRequestedGitHubActionsSheetToken += 1
                preferLocalActionsRequest = true
                navigator.popUntil { route -> route == KeiosRoute.Main }
            }
        },
        onRetryGitHubRefreshTargetsFromHistory = { trackIds ->
            if (trackIds.isNotEmpty()) {
                localRequestedBottomPage = BottomPage.GitHub.name
                localRequestedBottomPageToken += 1
                localRequestedGitHubActionsTrackId = null
                navigator.popUntil { route -> route == KeiosRoute.Main }
            }
        },
    )
}
