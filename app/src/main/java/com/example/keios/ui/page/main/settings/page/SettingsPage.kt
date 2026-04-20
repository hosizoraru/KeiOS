package com.example.keios.ui.page.main.settings.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.settings.section.SettingsAnimationSection
import com.example.keios.ui.page.main.settings.section.SettingsBackgroundSection
import com.example.keios.ui.page.main.settings.section.SettingsCacheSection
import com.example.keios.ui.page.main.settings.section.SettingsComponentEffectsSection
import com.example.keios.ui.page.main.settings.section.SettingsCopySection
import com.example.keios.ui.page.main.settings.section.SettingsCopySectionActions
import com.example.keios.ui.page.main.settings.section.SettingsCopySectionState
import com.example.keios.ui.page.main.settings.section.SettingsLogSection
import com.example.keios.ui.page.main.settings.section.SettingsNotifySection
import com.example.keios.ui.page.main.settings.section.SettingsNotifySectionActions
import com.example.keios.ui.page.main.settings.section.SettingsNotifySectionState
import com.example.keios.ui.page.main.settings.section.SettingsAnimationSectionActions
import com.example.keios.ui.page.main.settings.section.SettingsAnimationSectionState
import com.example.keios.ui.page.main.settings.section.SettingsComponentEffectsSectionActions
import com.example.keios.ui.page.main.settings.section.SettingsComponentEffectsSectionState
import com.example.keios.ui.page.main.settings.section.SettingsVisualSection
import com.example.keios.ui.page.main.settings.section.SettingsVisualSectionActions
import com.example.keios.ui.page.main.settings.section.SettingsVisualSectionState
import com.example.keios.ui.page.main.settings.state.rememberSettingsBackgroundController
import com.example.keios.ui.page.main.settings.state.rememberSettingsCacheController
import com.example.keios.ui.page.main.settings.state.rememberSettingsLogController
import com.example.keios.ui.page.main.settings.state.rememberSettingsPageUiState
import com.example.keios.ui.page.main.widget.chrome.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.chrome.AppPageScaffold
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
    val cacheController = rememberSettingsCacheController(
        context = context,
        cacheDiagnosticsEnabled = cacheDiagnosticsEnabled
    )
    val visualSectionState = remember(
        preloadingEnabled,
        homeIconHdrEnabled,
        appThemeMode,
        pageUiState.showThemeModePopup,
        pageUiState.themePopupAnchorBounds
    ) {
        SettingsVisualSectionState(
            preloadingEnabled = preloadingEnabled,
            homeIconHdrEnabled = homeIconHdrEnabled,
            appThemeMode = appThemeMode,
            showThemeModePopup = pageUiState.showThemeModePopup,
            themePopupAnchorBounds = pageUiState.themePopupAnchorBounds
        )
    }
    val visualSectionActions = remember(
        onPreloadingEnabledChanged,
        onHomeIconHdrChanged,
        onAppThemeModeChanged
    ) {
        SettingsVisualSectionActions(
            onPreloadingEnabledChanged = onPreloadingEnabledChanged,
            onHomeIconHdrChanged = onHomeIconHdrChanged,
            onAppThemeModeChanged = onAppThemeModeChanged,
            onShowThemeModePopupChange = { pageUiState.showThemeModePopup = it },
            onThemePopupAnchorBoundsChange = { pageUiState.themePopupAnchorBounds = it }
        )
    }
    val animationSectionState = remember(transitionAnimationsEnabled) {
        SettingsAnimationSectionState(
            transitionAnimationsEnabled = transitionAnimationsEnabled
        )
    }
    val animationSectionActions = remember(onTransitionAnimationsChanged) {
        SettingsAnimationSectionActions(
            onTransitionAnimationsChanged = onTransitionAnimationsChanged
        )
    }
    val componentEffectsState = remember(
        liquidActionBarLayeredStyleEnabled,
        liquidBottomBarEnabled,
        liquidGlassSwitchEnabled,
        cardPressFeedbackEnabled
    ) {
        SettingsComponentEffectsSectionState(
            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
            liquidBottomBarEnabled = liquidBottomBarEnabled,
            liquidGlassSwitchEnabled = liquidGlassSwitchEnabled,
            cardPressFeedbackEnabled = cardPressFeedbackEnabled
        )
    }
    val componentEffectsActions = remember(
        onLiquidActionBarLayeredStyleChanged,
        onLiquidBottomBarChanged,
        onLiquidGlassSwitchChanged,
        onCardPressFeedbackChanged
    ) {
        SettingsComponentEffectsSectionActions(
            onLiquidActionBarLayeredStyleChanged = onLiquidActionBarLayeredStyleChanged,
            onLiquidBottomBarChanged = onLiquidBottomBarChanged,
            onLiquidGlassSwitchChanged = onLiquidGlassSwitchChanged,
            onCardPressFeedbackChanged = onCardPressFeedbackChanged
        )
    }
    val notifySectionState = remember(
        superIslandNotificationEnabled,
        superIslandBypassRestrictionEnabled
    ) {
        SettingsNotifySectionState(
            superIslandNotificationEnabled = superIslandNotificationEnabled,
            superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled
        )
    }
    val notifySectionActions = remember(
        onSuperIslandNotificationChanged,
        onSuperIslandBypassRestrictionChanged
    ) {
        SettingsNotifySectionActions(
            onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
            onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged
        )
    }
    val copySectionState = remember(textCopyCapabilityExpanded) {
        SettingsCopySectionState(textCopyCapabilityExpanded = textCopyCapabilityExpanded)
    }
    val copySectionActions = remember(onTextCopyCapabilityExpandedChanged) {
        SettingsCopySectionActions(
            onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged
        )
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
                        state = visualSectionState,
                        actions = visualSectionActions,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsAnimationSection(
                        state = animationSectionState,
                        actions = animationSectionActions,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsComponentEffectsSection(
                        state = componentEffectsState,
                        actions = componentEffectsActions,
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
                        state = notifySectionState,
                        actions = notifySectionActions,
                        enabledCardColor = enabledCardColor,
                        disabledCardColor = disabledCardColor
                    )
                }
                item {
                    SettingsCopySection(
                        state = copySectionState,
                        actions = copySectionActions,
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
