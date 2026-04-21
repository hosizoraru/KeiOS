package os.kei.ui.page.main.settings.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import os.kei.R
import os.kei.core.prefs.AppThemeMode
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.settings.section.SettingsAnimationSection
import os.kei.ui.page.main.settings.section.SettingsBackgroundSection
import os.kei.ui.page.main.settings.section.SettingsCacheSection
import os.kei.ui.page.main.settings.section.SettingsComponentEffectsSection
import os.kei.ui.page.main.settings.section.SettingsLogSection
import os.kei.ui.page.main.settings.section.SettingsNotifySection
import os.kei.ui.page.main.settings.section.SettingsCopySection
import os.kei.ui.page.main.settings.section.SettingsVisualSection
import os.kei.ui.page.main.settings.state.rememberSettingsBackgroundController
import os.kei.ui.page.main.settings.state.rememberSettingsCacheController
import os.kei.ui.page.main.settings.state.rememberSettingsLogController
import os.kei.ui.page.main.settings.state.rememberSettingsPageUiState
import os.kei.ui.page.main.settings.state.rememberSettingsSectionContractBundle
import os.kei.ui.page.main.settings.support.rememberSettingsBatteryOptimizationController
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import android.widget.Toast
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalSettingsLiquidGlassSwitchEnabled = staticCompositionLocalOf { false }

@Composable
fun SettingsPage(
    liquidBottomBarEnabled: Boolean,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    liquidActionBarLayeredStyleEnabled: Boolean,
    onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
    liquidGlassSwitchEnabled: Boolean,
    onLiquidGlassSwitchChanged: (Boolean) -> Unit,
    transitionAnimationsEnabled: Boolean,
    onTransitionAnimationsChanged: (Boolean) -> Unit,
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
    logDebugEnabled: Boolean,
    onLogDebugChanged: (Boolean) -> Unit,
    textCopyCapabilityExpanded: Boolean,
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit,
    cacheDiagnosticsEnabled: Boolean,
    onCacheDiagnosticsChanged: (Boolean) -> Unit,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsTitle = stringResource(R.string.settings_title)
    val enabledCardColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    val disabledCardColor = Color(0x2264748B)

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
    DisposableEffect(lifecycleOwner, batteryOptimizationController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationController.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val sectionContracts = rememberSettingsSectionContractBundle(
        preloadingEnabled = preloadingEnabled,
        homeIconHdrEnabled = homeIconHdrEnabled,
        appThemeMode = appThemeMode,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        liquidBottomBarEnabled = liquidBottomBarEnabled,
        liquidGlassSwitchEnabled = liquidGlassSwitchEnabled,
        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
        superIslandNotificationEnabled = superIslandNotificationEnabled,
        superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
        ignoringBatteryOptimizations = batteryOptimizationController.ignoringBatteryOptimizations,
        batteryOptimizationActionAvailable = batteryOptimizationController.requestActionAvailable,
        textCopyCapabilityExpanded = textCopyCapabilityExpanded,
        pageUiState = pageUiState,
        onPreloadingEnabledChanged = onPreloadingEnabledChanged,
        onHomeIconHdrChanged = onHomeIconHdrChanged,
        onAppThemeModeChanged = onAppThemeModeChanged,
        onTransitionAnimationsChanged = onTransitionAnimationsChanged,
        onLiquidActionBarLayeredStyleChanged = onLiquidActionBarLayeredStyleChanged,
        onLiquidBottomBarChanged = onLiquidBottomBarChanged,
        onLiquidGlassSwitchChanged = onLiquidGlassSwitchChanged,
        onCardPressFeedbackChanged = onCardPressFeedbackChanged,
        onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
        onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
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
        onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged
    )

    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()

    AppPageScaffold(
        title = settingsTitle,
        modifier = Modifier.fillMaxSize(),
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
        }
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalSettingsLiquidGlassSwitchEnabled provides liquidGlassSwitchEnabled
        ) {
            AppPageLazyColumn(
                innerPadding = innerPadding,
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                sectionSpacing = 12.dp
            ) {
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
                item {
                    SettingsNotifySection(
                        state = sectionContracts.notifyState,
                        actions = sectionContracts.notifyActions,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
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
            }
        }
    }
}
