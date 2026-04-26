package os.kei.ui.page.main.settings.page

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
import os.kei.ui.page.main.settings.state.rememberSettingsPageUiState
import os.kei.ui.page.main.settings.state.rememberSettingsSectionContractBundle
import os.kei.ui.page.main.settings.state.SettingsPageViewModel
import os.kei.ui.page.main.settings.support.rememberSettingsBatteryOptimizationController
import os.kei.ui.page.main.settings.support.rememberSettingsPermissionKeepAliveController
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.chrome.ScrollChromeVisibilityController
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.motion.AppMotionTokens
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalSettingsLiquidGlassSwitchEnabled = staticCompositionLocalOf { false }

private fun LazyListState.canMoveForSettingsChrome(deltaY: Float): Boolean {
    return when {
        deltaY < -1f -> canScrollForward
        deltaY > 1f -> canScrollBackward
        else -> true
    }
}

private fun settingsChromeNestedScrollConnection(
    listState: LazyListState,
    delegate: NestedScrollConnection
): NestedScrollConnection {
    return object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!listState.canMoveForSettingsChrome(available.y)) return Offset.Zero
            return delegate.onPreScroll(available, source)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val canMove = listState.canMoveForSettingsChrome(consumed.y) ||
                listState.canMoveForSettingsChrome(available.y)
            if (!canMove) return Offset.Zero
            return delegate.onPostScroll(consumed, available, source)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!listState.canMoveForSettingsChrome(available.y)) return Velocity.Zero
            return delegate.onPreFling(available)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val canMove = listState.canMoveForSettingsChrome(consumed.y) ||
                listState.canMoveForSettingsChrome(available.y)
            if (!canMove) return Velocity.Zero
            return delegate.onPostFling(consumed, available)
        }
    }
}

private fun SettingsCategory.keepsChromeVisibleOnBounds(): Boolean {
    return this == SettingsCategory.Access || this == SettingsCategory.Notify
}

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
    val settingsPageViewModel: SettingsPageViewModel = viewModel()
    val cacheState by settingsPageViewModel.cacheState.collectAsState()
    val logState by settingsPageViewModel.logState.collectAsState()

    val pageUiState = rememberSettingsPageUiState()
    val backgroundController = rememberSettingsBackgroundController(
        nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
        onNonHomeBackgroundEnabledChanged = onNonHomeBackgroundEnabledChanged,
        nonHomeBackgroundUri = nonHomeBackgroundUri,
        onNonHomeBackgroundUriChanged = onNonHomeBackgroundUriChanged
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
    LaunchedEffect(context, cacheDiagnosticsEnabled) {
        settingsPageViewModel.bindCacheDiagnostics(
            context = context,
            enabled = cacheDiagnosticsEnabled
        )
    }
    LaunchedEffect(context, logDebugEnabled) {
        settingsPageViewModel.bindLogStats(
            context = context,
            logDebugEnabled = logDebugEnabled
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

    val logExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            settingsPageViewModel.finishLogExport()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = settingsPageViewModel.exportLogZip(context, uri)
            settingsPageViewModel.finishLogExport()
            if (result.isSuccess) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_log_toast_exported),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val reason = result.exceptionOrNull()?.javaClass?.simpleName
                    ?: context.getString(R.string.common_unknown)
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_log_toast_export_failed, reason),
                    Toast.LENGTH_SHORT
                ).show()
            }
            settingsPageViewModel.reloadLogStats(context)
        }
    }
    LaunchedEffect(logState.pendingExportFileName) {
        val fileName = settingsPageViewModel.consumePendingExportFileName() ?: return@LaunchedEffect
        logExportLauncher.launch(fileName)
    }

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
    val activeCategoryIndex = if (pagerState.isScrollInProgress) {
        pagerState.targetPage
    } else {
        pagerState.settledPage
    }.coerceIn(0, categories.lastIndex)
    val activeCategory = categories[activeCategoryIndex]
    val activePageListState = when (activeCategory) {
        SettingsCategory.Access -> accessListState
        SettingsCategory.Appearance -> appearanceListState
        SettingsCategory.Notify -> notifyListState
        SettingsCategory.Data -> dataListState
    }
    val currentActivePageListState = rememberUpdatedState(activePageListState)
    val currentActiveCategory = rememberUpdatedState(activeCategory)
    val bottomBarNestedScrollConnection = remember(bottomBarVisibilityController) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (currentActiveCategory.value.keepsChromeVisibleOnBounds()) {
                    bottomBarVisibilityController.showNow(showBottomBar) { showBottomBar = it }
                    return Offset.Zero
                }
                val currentListState = currentActivePageListState.value
                bottomBarVisibilityController.updateWithinScrollBounds(
                    deltaY = consumed.y,
                    visible = showBottomBar,
                    canScrollBackward = currentListState.canScrollBackward,
                    canScrollForward = currentListState.canScrollForward
                ) { showBottomBar = it }
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
                beyondViewportPageCount = 1,
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
                val pageNestedScrollConnection = remember(pageListState, scrollBehavior) {
                    settingsChromeNestedScrollConnection(
                        listState = pageListState,
                        delegate = scrollBehavior.nestedScrollConnection
                    )
                }
                AppPageLazyColumn(
                    innerPadding = innerPadding,
                    state = pageListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pageNestedScrollConnection),
                    bottomExtra = appPageBottomPaddingWithFloatingOverlay(
                        AppChromeTokens.floatingBottomBarOuterHeight
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
                                    cacheEntries = cacheState.cacheEntries,
                                    cacheEntriesLoading = cacheState.cacheEntriesLoading,
                                    clearingAllCaches = cacheState.clearingAllCaches,
                                    clearingCacheId = cacheState.clearingCacheId,
                                    onClearAllCaches = {
                                        scope.launch {
                                            val result = settingsPageViewModel.clearAllCaches(context)
                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.settings_cache_toast_cleared_all),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val reason = result.exceptionOrNull()?.javaClass?.simpleName
                                                    ?: context.getString(R.string.common_unknown)
                                                Toast.makeText(
                                                    context,
                                                    context.getString(
                                                        R.string.settings_cache_toast_clear_all_failed,
                                                        reason
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    onClearCache = { cacheId ->
                                        scope.launch {
                                            settingsPageViewModel.clearCache(context, cacheId)
                                        }
                                    },
                                    enabledCardColor = enabledCardColor,
                                    disabledCardColor = disabledCardColor
                                )
                            }
                            item {
                                SettingsLogSection(
                                    logDebugEnabled = logDebugEnabled,
                                    onLogDebugChanged = onLogDebugChanged,
                                    logStats = logState.logStats,
                                    exportingLogZip = logState.exportingLogZip,
                                    clearingLogs = logState.clearingLogs,
                                    onExportZipClick = settingsPageViewModel::beginLogExport,
                                    onClearLogsClick = {
                                        scope.launch {
                                            val result = settingsPageViewModel.clearLogs(context)
                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.settings_log_toast_cleared),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val reason = result.exceptionOrNull()?.javaClass?.simpleName
                                                    ?: context.getString(R.string.common_unknown)
                                                Toast.makeText(
                                                    context,
                                                    context.getString(
                                                        R.string.settings_log_toast_clear_failed,
                                                        reason
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
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
