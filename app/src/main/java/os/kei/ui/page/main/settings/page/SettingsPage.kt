package os.kei.ui.page.main.settings.page

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.core.prefs.AppThemeMode
import os.kei.core.system.ShizukuApiUtils
import os.kei.ui.page.main.host.pager.animateTabSwitch
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.settings.section.SettingsAnimationSection
import os.kei.ui.page.main.settings.section.SettingsBackgroundSection
import os.kei.ui.page.main.settings.section.SettingsCacheSection
import os.kei.ui.page.main.settings.section.SettingsComponentEffectsSection
import os.kei.ui.page.main.settings.section.SettingsLogSection
import os.kei.ui.page.main.settings.section.SettingsNotifySection
import os.kei.ui.page.main.settings.section.SettingsPermissionKeepAliveSection
import os.kei.ui.page.main.settings.section.SettingsCopySection
import os.kei.ui.page.main.settings.section.SettingsVisualSection
import os.kei.ui.page.main.settings.state.rememberSettingsBackgroundController
import os.kei.ui.page.main.settings.state.rememberSettingsCacheController
import os.kei.ui.page.main.settings.state.rememberSettingsLogController
import os.kei.ui.page.main.settings.state.rememberSettingsPageUiState
import os.kei.ui.page.main.settings.state.rememberSettingsSectionContractBundle
import os.kei.ui.page.main.settings.support.rememberSettingsBatteryOptimizationController
import os.kei.ui.page.main.settings.support.rememberSettingsPermissionKeepAliveController
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.chrome.ScrollChromeVisibilityController
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalSettingsLiquidGlassSwitchEnabled = staticCompositionLocalOf { false }

@Composable
fun SettingsPage(
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    liquidBottomBarEnabled: Boolean,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    liquidActionBarLayeredStyleEnabled: Boolean,
    onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
    liquidGlassSwitchEnabled: Boolean,
    onLiquidGlassSwitchChanged: (Boolean) -> Unit,
    transitionAnimationsEnabled: Boolean,
    onTransitionAnimationsChanged: (Boolean) -> Unit,
    predictiveBackAnimationsEnabled: Boolean,
    onPredictiveBackAnimationsChanged: (Boolean) -> Unit,
    cardPressFeedbackEnabled: Boolean,
    onCardPressFeedbackChanged: (Boolean) -> Unit,
    homeIconHdrEnabled: Boolean,
    onHomeIconHdrChanged: (Boolean) -> Unit,
    preloadingEnabled: Boolean,
    onPreloadingEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundEnabled: Boolean,
    onNonHomeBackgroundEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundUri: String,
    onNonHomeBackgroundUriChanged: (String) -> Unit,
    nonHomeBackgroundOpacity: Float,
    onNonHomeBackgroundOpacityChanged: (Float) -> Unit,
    superIslandNotificationEnabled: Boolean,
    onSuperIslandNotificationChanged: (Boolean) -> Unit,
    superIslandBypassRestrictionEnabled: Boolean,
    onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    superIslandRestoreDelayMs: Int,
    onSuperIslandRestoreDelayMsChanged: (Int) -> Unit,
    logDebugEnabled: Boolean,
    onLogDebugChanged: (Boolean) -> Unit,
    textCopyCapabilityExpanded: Boolean,
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit,
    cacheDiagnosticsEnabled: Boolean,
    onCacheDiagnosticsChanged: (Boolean) -> Unit,
    shizukuStatus: String,
    onCheckOrRequestShizuku: () -> Unit,
    shizukuApiUtils: ShizukuApiUtils,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsTitle = stringResource(R.string.settings_title)
    val enabledCardColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    val disabledCardColor = Color(0x2264748B)
    val scope = rememberCoroutineScope()
    val latestNotificationPermissionGranted = rememberUpdatedState(notificationPermissionGranted)
    val latestShizukuStatus = rememberUpdatedState(shizukuStatus)
    var shizukuRefreshToken by remember { mutableIntStateOf(0) }

    val pageUiState = rememberSettingsPageUiState()
    val backgroundController = rememberSettingsBackgroundController(
        nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
        onNonHomeBackgroundEnabledChanged = onNonHomeBackgroundEnabledChanged,
        nonHomeBackgroundUri = nonHomeBackgroundUri,
        onNonHomeBackgroundUriChanged = onNonHomeBackgroundUriChanged
    )
    val logController = rememberSettingsLogController(
        logDebugEnabled = logDebugEnabled,
        pageUiState = pageUiState
    )
    val cacheController = rememberSettingsCacheController(
        context = context,
        cacheDiagnosticsEnabled = cacheDiagnosticsEnabled
    )
    val batteryOptimizationController = rememberSettingsBatteryOptimizationController(context)
    val permissionKeepAliveController = rememberSettingsPermissionKeepAliveController(
        context = context,
        shizukuApiUtils = shizukuApiUtils
    )
    DisposableEffect(lifecycleOwner, batteryOptimizationController, permissionKeepAliveController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationController.refresh()
                scope.launch {
                    permissionKeepAliveController.refresh(
                        notificationPermissionGranted = latestNotificationPermissionGranted.value,
                        shizukuStatus = latestShizukuStatus.value
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(notificationPermissionGranted, shizukuStatus) {
        permissionKeepAliveController.refresh(
            notificationPermissionGranted = notificationPermissionGranted,
            shizukuStatus = shizukuStatus
        )
    }
    LaunchedEffect(shizukuRefreshToken) {
        if (shizukuRefreshToken <= 0) return@LaunchedEffect
        repeat(8) {
            permissionKeepAliveController.refresh(
                notificationPermissionGranted = latestNotificationPermissionGranted.value,
                shizukuStatus = latestShizukuStatus.value
            )
            delay(400)
        }
    }
    val sectionContracts = rememberSettingsSectionContractBundle(
        notificationPermissionGranted = notificationPermissionGranted,
        notificationsEnabled = permissionKeepAliveController.notificationsEnabled,
        notificationSettingsActionAvailable = permissionKeepAliveController.notificationSettingsActionAvailable,
        preloadingEnabled = preloadingEnabled,
        homeIconHdrEnabled = homeIconHdrEnabled,
        appThemeMode = appThemeMode,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        predictiveBackAnimationsEnabled = predictiveBackAnimationsEnabled,
        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        liquidBottomBarEnabled = liquidBottomBarEnabled,
        liquidGlassSwitchEnabled = liquidGlassSwitchEnabled,
        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
        superIslandNotificationEnabled = superIslandNotificationEnabled,
        superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
        superIslandRestoreDelayMs = superIslandRestoreDelayMs,
        ignoringBatteryOptimizations = batteryOptimizationController.ignoringBatteryOptimizations,
        batteryOptimizationActionAvailable = batteryOptimizationController.requestActionAvailable,
        oemAutoStartState = permissionKeepAliveController.oemAutoStartState,
        oemAutoStartVendorLabel = permissionKeepAliveController.oemAutoStartVendorLabel,
        oemAutoStartActionAvailable = permissionKeepAliveController.oemAutoStartActionAvailable,
        appListAccessMode = permissionKeepAliveController.appListAccessMode,
        appListDetectedCount = permissionKeepAliveController.appListDetectedCount,
        appListSettingsActionAvailable = permissionKeepAliveController.appListSettingsActionAvailable,
        shizukuGranted = permissionKeepAliveController.shizukuGranted,
        shizukuStatusText = permissionKeepAliveController.shizukuStatusText,
        textCopyCapabilityExpanded = textCopyCapabilityExpanded,
        pageUiState = pageUiState,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onOpenNotificationSettings = {
            val opened = permissionKeepAliveController.openNotificationSettings()
            if (!opened) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_notification_permission_toast_open_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onPreloadingEnabledChanged = onPreloadingEnabledChanged,
        onHomeIconHdrChanged = onHomeIconHdrChanged,
        onAppThemeModeChanged = onAppThemeModeChanged,
        onTransitionAnimationsChanged = onTransitionAnimationsChanged,
        onPredictiveBackAnimationsChanged = onPredictiveBackAnimationsChanged,
        onLiquidActionBarLayeredStyleChanged = onLiquidActionBarLayeredStyleChanged,
        onLiquidBottomBarChanged = onLiquidBottomBarChanged,
        onLiquidGlassSwitchChanged = onLiquidGlassSwitchChanged,
        onCardPressFeedbackChanged = onCardPressFeedbackChanged,
        onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
        onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
        onSuperIslandRestoreDelayMsChanged = onSuperIslandRestoreDelayMsChanged,
        onOpenBatteryOptimizationSettings = {
            val opened = batteryOptimizationController.openBatteryOptimizationSettings()
            if (!opened) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_battery_optimization_toast_open_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onOpenOemAutoStartSettings = {
            val opened = permissionKeepAliveController.openOemAutoStartSettings()
            if (!opened) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_oem_autostart_toast_open_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onOpenAppListPermissionSettings = {
            val opened = permissionKeepAliveController.openAppListPermissionSettings()
            if (!opened) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_app_list_access_toast_open_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onCheckOrRequestShizuku = {
            shizukuRefreshToken += 1
            onCheckOrRequestShizuku()
        },
        onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged
    )

    val scrollBehavior = MiuixScrollBehavior()
    val categories = remember { SettingsCategory.entries.toList() }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = selectedCategoryIndex.coerceIn(0, categories.lastIndex),
        pageCount = { categories.size }
    )
    val accessListState = rememberLazyListState()
    val appearanceListState = rememberLazyListState()
    val notifyListState = rememberLazyListState()
    val dataListState = rememberLazyListState()
    val bottomBarBackdrop = rememberLayerBackdrop()
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var showBottomBar by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }
    val density = LocalDensity.current
    val bottomBarVisibilityThresholdPx = remember(density) { with(density) { 22.dp.toPx() } }
    val bottomBarVisibilityController = remember(bottomBarVisibilityThresholdPx) {
        ScrollChromeVisibilityController(bottomBarVisibilityThresholdPx)
    }
    val bottomBarNestedScrollConnection = remember(bottomBarVisibilityController) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                bottomBarVisibilityController.update(consumed.y, showBottomBar) { showBottomBar = it }
                return Offset.Zero
            }
        }
    }
    val selectSettingsCategoryAction = remember(
        categories,
        pagerState,
        transitionAnimationsEnabled,
        farJumpAlpha,
        scope
    ) {
        { index: Int ->
            val safeIndex = index.coerceIn(0, categories.lastIndex)
            val stablePageIndex = if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.settledPage
            }
            if (safeIndex != stablePageIndex) {
                selectedCategoryIndex = safeIndex
                tabJumpJob?.cancel()
                tabJumpJob = scope.launch {
                    pagerState.animateTabSwitch(
                        fromIndex = stablePageIndex,
                        targetIndex = safeIndex,
                        animationsEnabled = transitionAnimationsEnabled,
                        onFarJumpBefore = {
                            farJumpAlpha.snapTo(1f)
                            farJumpAlpha.animateTo(
                                targetValue = 0.92f,
                                animationSpec = tween(
                                    durationMillis = resolvedMotionDuration(
                                        AppMotionTokens.farJumpDimMs,
                                        transitionAnimationsEnabled
                                    )
                                )
                            )
                        },
                        onFarJumpAfter = {
                            farJumpAlpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = resolvedMotionDuration(
                                        AppMotionTokens.farJumpRestoreMs,
                                        transitionAnimationsEnabled
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (selectedCategoryIndex != pagerState.settledPage) {
            selectedCategoryIndex = pagerState.settledPage
        }
    }

    AppPageScaffold(
        title = settingsTitle,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(bottomBarNestedScrollConnection),
        scrollBehavior = scrollBehavior,
        topBarColor = Color.Transparent,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = appLucideBackIcon(),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        },
        bottomBar = {
            SettingsCategoryBottomBar(
                visible = showBottomBar,
                navigationBarBottom = navigationBarBottom,
                categories = categories,
                selectedPage = pagerState.targetPage.coerceIn(0, categories.lastIndex),
                selectedPageProvider = { pagerState.targetPage },
                backdrop = bottomBarBackdrop,
                reduceEffectsDuringPagerScroll = pagerState.isScrollInProgress,
                isLiquidEffectEnabled = liquidBottomBarEnabled,
                onSelectCategory = selectSettingsCategoryAction
            )
        }
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalSettingsLiquidGlassSwitchEnabled provides liquidGlassSwitchEnabled
        ) {
            HorizontalPager(
                state = pagerState,
                key = { index -> categories[index].name },
                overscrollEffect = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = farJumpAlpha.value }
                    .layerBackdrop(bottomBarBackdrop)
            ) { pageIndex ->
                val category = categories[pageIndex]
                val pageListState = when (category) {
                    SettingsCategory.Access -> accessListState
                    SettingsCategory.Appearance -> appearanceListState
                    SettingsCategory.Notify -> notifyListState
                    SettingsCategory.Data -> dataListState
                }
                val pageViewportBottomPadding = innerPadding.calculateBottomPadding()
                val pageContentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding()
                )
                AppPageLazyColumn(
                    innerPadding = pageContentPadding,
                    state = pageListState,
                    modifier = Modifier
                        .padding(bottom = pageViewportBottomPadding)
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    bottomExtra = appPageBottomPaddingWithFloatingOverlay(
                        AppChromeTokens.pageBottomInsetExtra
                    ),
                    sectionSpacing = 12.dp
                ) {
                    when (category) {
                        SettingsCategory.Access -> {
                            item {
                                SettingsPermissionKeepAliveSection(
                                    state = sectionContracts.permissionKeepAliveState,
                                    actions = sectionContracts.permissionKeepAliveActions,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                        }
                        SettingsCategory.Appearance -> {
                            item {
                                SettingsVisualSection(
                                    state = sectionContracts.visualState,
                                    actions = sectionContracts.visualActions,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                            item {
                                SettingsAnimationSection(
                                    state = sectionContracts.animationState,
                                    actions = sectionContracts.animationActions,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                            item {
                                SettingsComponentEffectsSection(
                                    state = sectionContracts.componentEffectsState,
                                    actions = sectionContracts.componentEffectsActions,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                            item {
                                SettingsBackgroundSection(
                                    nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
                                    onNonHomeBackgroundEnabledChanged = onNonHomeBackgroundEnabledChanged,
                                    nonHomeBackgroundUri = nonHomeBackgroundUri,
                                    nonHomeBackgroundOpacity = nonHomeBackgroundOpacity,
                                    onNonHomeBackgroundOpacityChanged = onNonHomeBackgroundOpacityChanged,
                                    backgroundPickerLauncher = backgroundController.backgroundPickerLauncher,
                                    onClearBackground = backgroundController.clearBackground,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                        }
                        SettingsCategory.Notify -> {
                            item {
                                SettingsNotifySection(
                                    state = sectionContracts.notifyState,
                                    actions = sectionContracts.notifyActions,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                        }
                        SettingsCategory.Data -> {
                            item {
                                SettingsCopySection(
                                    state = sectionContracts.copyState,
                                    actions = sectionContracts.copyActions,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                            item {
                                SettingsCacheSection(
                                    cacheDiagnosticsEnabled = cacheDiagnosticsEnabled,
                                    onCacheDiagnosticsChanged = onCacheDiagnosticsChanged,
                                    cacheEntries = cacheController.cacheEntries,
                                    cacheEntriesLoading = cacheController.cacheEntriesLoading,
                                    clearingAllCaches = cacheController.clearingAllCaches,
                                    onClearingAllCachesChange = { cacheController.clearingAllCaches = it },
                                    clearingCacheId = cacheController.clearingCacheId,
                                    onClearingCacheIdChange = { cacheController.clearingCacheId = it },
                                    onCacheReload = cacheController::requestCacheReload,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                            item {
                                SettingsLogSection(
                                    logDebugEnabled = logDebugEnabled,
                                    onLogDebugChanged = onLogDebugChanged,
                                    logStats = logController.logStats,
                                    exportingLogZip = logController.exportingLogZip,
                                    clearingLogs = logController.clearingLogs,
                                    onExportZipClick = logController.exportZip,
                                    onClearLogsClick = logController.clearLogs,
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
