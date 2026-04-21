package com.example.keios.ui.page.main.host.main

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.mcp.server.McpServerManager
import com.example.keios.ui.navigation.KeiosRoute
import com.example.keios.ui.navigation.Navigator
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
    appLabel: String,
    packageInfo: PackageInfo?,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    shizukuApiUtils: ShizukuApiUtils,
    mcpServerManager: McpServerManager,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    requestedBottomPage: String?,
    requestedBottomPageToken: Int,
    requestedGitHubRefreshToken: Int,
    onRequestedBottomPageConsumed: () -> Unit
) {
    val backStack = remember { mutableStateListOf<NavKey>().apply { add(KeiosRoute.Main) } }
    val navigator = remember { Navigator(backStack) }
    val context = LocalContext.current
    val appContext = context.applicationContext
    val view = LocalView.current
    val currentAppLabel by rememberUpdatedState(appLabel)
    val currentPackageInfo by rememberUpdatedState(packageInfo)
    val currentShizukuStatus by rememberUpdatedState(shizukuStatus)
    val currentNotificationPermissionGranted by rememberUpdatedState(notificationPermissionGranted)
    val currentOnCheckOrRequestShizuku by rememberUpdatedState(onCheckOrRequestShizuku)
    val currentOnAppThemeModeChanged by rememberUpdatedState(onAppThemeModeChanged)
    val mainReturnState = rememberMainScreenSettingsReturnState(backStack)
    BindMainScreenBottomPageReturnEffect(
        requestedBottomPageToken = requestedBottomPageToken,
        requestedBottomPage = requestedBottomPage,
        onReturnToMain = {
            navigator.popUntil { it == KeiosRoute.Main }
        }
    )
    val uiPrefsSnapshot by produceState(
        initialValue = UiPrefs.defaultSnapshot(appThemeMode)
    ) {
        value = withContext(Dispatchers.IO) { UiPrefs.loadSnapshot() }
    }
    val uiPrefsState = rememberMainScreenUiPrefsState(
        snapshot = uiPrefsSnapshot,
        appContext = appContext,
        mcpServerManager = mcpServerManager
    )
    val poolGuideMissingText = stringResource(R.string.main_toast_pool_guide_missing)
    val externalOpenFailureText = stringResource(R.string.ba_error_open_activity_link)
    val openGuideDetail = rememberMainScreenOpenGuideDetailAction(
        poolGuideMissingText = poolGuideMissingText,
        externalOpenFailureText = externalOpenFailureText,
        onNavigateToCanonicalGuide = { canonicalGuideUrl ->
            BaStudentGuideStore.setCurrentUrl(canonicalGuideUrl)
            navigator.push(KeiosRoute.BaStudentGuide(nonce = System.nanoTime()))
        }
    )
    val pagerCoordinator = buildMainScreenPagerCoordinator(
        settingsReturnToken = mainReturnState.settingsReturnToken,
        prefsState = uiPrefsState,
        shizukuStatus = currentShizukuStatus,
        shizukuApiUtils = shizukuApiUtils,
        mcpServerManager = mcpServerManager,
        onOpenGuideDetail = openGuideDetail,
        requestedBottomPage = requestedBottomPage,
        requestedBottomPageToken = requestedBottomPageToken,
        requestedGitHubRefreshToken = requestedGitHubRefreshToken,
        onRequestedBottomPageConsumed = onRequestedBottomPageConsumed
    )
    if (!view.isInEditMode) {
        SideEffect {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@SideEffect
            val activity = view.context as? Activity ?: return@SideEffect
            runCatching {
                activity.window.colorMode = if (uiPrefsState.homeIconHdrEnabled) {
                    ActivityInfo.COLOR_MODE_HDR
                } else {
                    ActivityInfo.COLOR_MODE_DEFAULT
                }
            }
        }
    }
    MainScreenNavHost(
        backStack = backStack,
        navigator = navigator,
        pagerCoordinator = pagerCoordinator,
        prefsState = uiPrefsState,
        appLabel = currentAppLabel,
        packageInfo = currentPackageInfo,
        onCheckOrRequestShizuku = currentOnCheckOrRequestShizuku,
        notificationPermissionGranted = currentNotificationPermissionGranted,
        mcpServerManager = mcpServerManager,
        appThemeMode = appThemeMode,
        onAppThemeModeChanged = currentOnAppThemeModeChanged,
    )
}
