package com.example.keios.ui.page.main.settings.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.CacheEntrySummary
import com.example.keios.core.prefs.CacheStores
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.settings.section.SettingsAnimationSection
import com.example.keios.ui.page.main.settings.section.SettingsBackgroundSection
import com.example.keios.ui.page.main.settings.section.SettingsCacheSection
import com.example.keios.ui.page.main.settings.section.SettingsComponentEffectsSection
import com.example.keios.ui.page.main.settings.section.SettingsCopySection
import com.example.keios.ui.page.main.settings.section.SettingsLogSection
import com.example.keios.ui.page.main.settings.section.SettingsNotifySection
import com.example.keios.ui.page.main.settings.section.SettingsVisualSection
import com.example.keios.ui.page.main.settings.state.rememberSettingsBackgroundController
import com.example.keios.ui.page.main.settings.state.rememberSettingsLogController
import com.example.keios.ui.page.main.settings.state.rememberSettingsPageUiState
import com.example.keios.ui.page.main.widget.chrome.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.chrome.AppPageScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    var cacheEntries by remember(cacheDiagnosticsEnabled) {
        mutableStateOf<List<CacheEntrySummary>?>(if (cacheDiagnosticsEnabled) null else emptyList())
    }
    var cacheEntriesLoading by remember(cacheDiagnosticsEnabled) {
        mutableStateOf(cacheDiagnosticsEnabled)
    }
    LaunchedEffect(cacheDiagnosticsEnabled, pageUiState.cacheReloadSignal) {
        if (!cacheDiagnosticsEnabled) {
            cacheEntries = emptyList()
            cacheEntriesLoading = false
            return@LaunchedEffect
        }
        cacheEntriesLoading = cacheEntries == null
        cacheEntries = withContext(Dispatchers.IO) { CacheStores.list(context) }
        cacheEntriesLoading = false
    }

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
                        preloadingEnabled = preloadingEnabled,
                        onPreloadingEnabledChanged = onPreloadingEnabledChanged,
                        homeIconHdrEnabled = homeIconHdrEnabled,
                        onHomeIconHdrChanged = onHomeIconHdrChanged,
                        appThemeMode = appThemeMode,
                        onAppThemeModeChanged = onAppThemeModeChanged,
                        showThemeModePopup = pageUiState.showThemeModePopup,
                        onShowThemeModePopupChange = { pageUiState.showThemeModePopup = it },
                        themePopupAnchorBounds = pageUiState.themePopupAnchorBounds,
                        onThemePopupAnchorBoundsChange = { pageUiState.themePopupAnchorBounds = it },
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsAnimationSection(
                        transitionAnimationsEnabled = transitionAnimationsEnabled,
                        onTransitionAnimationsChanged = onTransitionAnimationsChanged,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsComponentEffectsSection(
                        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        onLiquidActionBarLayeredStyleChanged = onLiquidActionBarLayeredStyleChanged,
                        liquidBottomBarEnabled = liquidBottomBarEnabled,
                        onLiquidBottomBarChanged = onLiquidBottomBarChanged,
                        liquidGlassSwitchEnabled = liquidGlassSwitchEnabled,
                        onLiquidGlassSwitchChanged = onLiquidGlassSwitchChanged,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        onCardPressFeedbackChanged = onCardPressFeedbackChanged,
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
                        superIslandNotificationEnabled = superIslandNotificationEnabled,
                        onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
                        superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
                        onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsCopySection(
                        textCopyCapabilityExpanded = textCopyCapabilityExpanded,
                        onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsCacheSection(
                        cacheDiagnosticsEnabled = cacheDiagnosticsEnabled,
                        onCacheDiagnosticsChanged = onCacheDiagnosticsChanged,
                        cacheEntries = cacheEntries,
                        cacheEntriesLoading = cacheEntriesLoading,
                        clearingAllCaches = pageUiState.clearingAllCaches,
                        onClearingAllCachesChange = { pageUiState.clearingAllCaches = it },
                        clearingCacheId = pageUiState.clearingCacheId,
                        onClearingCacheIdChange = { pageUiState.clearingCacheId = it },
                        onCacheReload = pageUiState::requestCacheReload,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
            }
        }
    }
}
