package os.kei.ui.page.main.settings.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import os.kei.core.prefs.AppThemeMode
import os.kei.ui.page.main.settings.section.SettingsAnimationSectionActions
import os.kei.ui.page.main.settings.section.SettingsAnimationSectionState
import os.kei.ui.page.main.settings.section.SettingsComponentEffectsSectionActions
import os.kei.ui.page.main.settings.section.SettingsComponentEffectsSectionState
import os.kei.ui.page.main.settings.section.SettingsCopySectionActions
import os.kei.ui.page.main.settings.section.SettingsCopySectionState
import os.kei.ui.page.main.settings.section.SettingsPermissionKeepAliveSectionActions
import os.kei.ui.page.main.settings.section.SettingsPermissionKeepAliveSectionState
import os.kei.ui.page.main.settings.section.SettingsNotifySectionActions
import os.kei.ui.page.main.settings.section.SettingsNotifySectionState
import os.kei.ui.page.main.settings.section.SettingsVisualSectionActions
import os.kei.ui.page.main.settings.section.SettingsVisualSectionState
import os.kei.ui.page.main.settings.support.SettingsAppListAccessMode
import os.kei.ui.page.main.settings.support.SettingsOemAutoStartState

internal data class SettingsSectionContractBundle(
    val permissionKeepAliveState: SettingsPermissionKeepAliveSectionState,
    val permissionKeepAliveActions: SettingsPermissionKeepAliveSectionActions,
    val visualState: SettingsVisualSectionState,
    val visualActions: SettingsVisualSectionActions,
    val animationState: SettingsAnimationSectionState,
    val animationActions: SettingsAnimationSectionActions,
    val componentEffectsState: SettingsComponentEffectsSectionState,
    val componentEffectsActions: SettingsComponentEffectsSectionActions,
    val notifyState: SettingsNotifySectionState,
    val notifyActions: SettingsNotifySectionActions,
    val copyState: SettingsCopySectionState,
    val copyActions: SettingsCopySectionActions
)

@Composable
internal fun rememberSettingsSectionContractBundle(
    notificationPermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    notificationSettingsActionAvailable: Boolean,
    preloadingEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    appThemeMode: AppThemeMode,
    appLanguageActionAvailable: Boolean,
    transitionAnimationsEnabled: Boolean,
    predictiveBackAnimationsEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    liquidSwitchEnabled: Boolean,
    liquidBottomBarEnabled: Boolean,
    bottomBarScrollEffectReductionEnabled: Boolean,
    cardPressFeedbackEnabled: Boolean,
    superIslandNotificationEnabled: Boolean,
    superIslandBypassRestrictionEnabled: Boolean,
    superIslandRestoreDelayMs: Int,
    ignoringBatteryOptimizations: Boolean,
    batteryOptimizationActionAvailable: Boolean,
    oemAutoStartState: SettingsOemAutoStartState,
    oemAutoStartVendorLabel: String,
    oemAutoStartActionAvailable: Boolean,
    appListAccessMode: SettingsAppListAccessMode,
    appListDetectedCount: Int,
    appListSettingsActionAvailable: Boolean,
    shizukuGranted: Boolean,
    shizukuStatusText: String,
    textCopyCapabilityExpanded: Boolean,
    pageUiState: SettingsPageUiState,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onPreloadingEnabledChanged: (Boolean) -> Unit,
    onHomeIconHdrChanged: (Boolean) -> Unit,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenAppLanguageSettings: () -> Unit,
    onTransitionAnimationsChanged: (Boolean) -> Unit,
    onPredictiveBackAnimationsChanged: (Boolean) -> Unit,
    onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
    onLiquidSwitchChanged: (Boolean) -> Unit,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    onBottomBarScrollEffectReductionChanged: (Boolean) -> Unit,
    onCardPressFeedbackChanged: (Boolean) -> Unit,
    onSuperIslandNotificationChanged: (Boolean) -> Unit,
    onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    onSuperIslandRestoreDelayMsChanged: (Int) -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenOemAutoStartSettings: () -> Unit,
    onOpenAppListPermissionSettings: () -> Unit,
    onCheckOrRequestShizuku: () -> Unit,
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit
): SettingsSectionContractBundle {
    val permissionKeepAliveState = remember(
        notificationPermissionGranted,
        notificationsEnabled,
        notificationSettingsActionAvailable,
        ignoringBatteryOptimizations,
        batteryOptimizationActionAvailable,
        oemAutoStartState,
        oemAutoStartVendorLabel,
        oemAutoStartActionAvailable,
        appListAccessMode,
        appListDetectedCount,
        appListSettingsActionAvailable,
        shizukuGranted,
        shizukuStatusText
    ) {
        SettingsPermissionKeepAliveSectionState(
            notificationPermissionGranted = notificationPermissionGranted,
            notificationsEnabled = notificationsEnabled,
            notificationSettingsActionAvailable = notificationSettingsActionAvailable,
            ignoringBatteryOptimizations = ignoringBatteryOptimizations,
            batteryOptimizationActionAvailable = batteryOptimizationActionAvailable,
            oemAutoStartState = oemAutoStartState,
            oemAutoStartVendorLabel = oemAutoStartVendorLabel,
            oemAutoStartActionAvailable = oemAutoStartActionAvailable,
            appListAccessMode = appListAccessMode,
            appListDetectedCount = appListDetectedCount,
            appListSettingsActionAvailable = appListSettingsActionAvailable,
            shizukuGranted = shizukuGranted,
            shizukuStatusText = shizukuStatusText
        )
    }
    val permissionKeepAliveActions = remember(
        onRequestNotificationPermission,
        onOpenNotificationSettings,
        onOpenBatteryOptimizationSettings,
        onOpenOemAutoStartSettings,
        onOpenAppListPermissionSettings,
        onCheckOrRequestShizuku
    ) {
        SettingsPermissionKeepAliveSectionActions(
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
            onOpenOemAutoStartSettings = onOpenOemAutoStartSettings,
            onOpenAppListPermissionSettings = onOpenAppListPermissionSettings,
            onCheckOrRequestShizuku = onCheckOrRequestShizuku
        )
    }
    val visualState = remember(
        preloadingEnabled,
        homeIconHdrEnabled,
        appThemeMode,
        appLanguageActionAvailable,
        pageUiState.showThemeModePopup,
        pageUiState.themePopupAnchorBounds
    ) {
        SettingsVisualSectionState(
            preloadingEnabled = preloadingEnabled,
            homeIconHdrEnabled = homeIconHdrEnabled,
            appThemeMode = appThemeMode,
            appLanguageActionAvailable = appLanguageActionAvailable,
            showThemeModePopup = pageUiState.showThemeModePopup,
            themePopupAnchorBounds = pageUiState.themePopupAnchorBounds
        )
    }
    val visualActions = remember(
        onPreloadingEnabledChanged,
        onHomeIconHdrChanged,
        onAppThemeModeChanged,
        onOpenAppLanguageSettings
    ) {
        SettingsVisualSectionActions(
            onPreloadingEnabledChanged = onPreloadingEnabledChanged,
            onHomeIconHdrChanged = onHomeIconHdrChanged,
            onAppThemeModeChanged = onAppThemeModeChanged,
            onOpenAppLanguageSettings = onOpenAppLanguageSettings,
            onShowThemeModePopupChange = { pageUiState.showThemeModePopup = it },
            onThemePopupAnchorBoundsChange = { pageUiState.themePopupAnchorBounds = it }
        )
    }
    val animationState = remember(
        transitionAnimationsEnabled,
        predictiveBackAnimationsEnabled
    ) {
        SettingsAnimationSectionState(
            transitionAnimationsEnabled = transitionAnimationsEnabled,
            predictiveBackAnimationsEnabled = predictiveBackAnimationsEnabled
        )
    }
    val animationActions = remember(
        onTransitionAnimationsChanged,
        onPredictiveBackAnimationsChanged
    ) {
        SettingsAnimationSectionActions(
            onTransitionAnimationsChanged = onTransitionAnimationsChanged,
            onPredictiveBackAnimationsChanged = onPredictiveBackAnimationsChanged
        )
    }
    val componentEffectsState = remember(
        liquidActionBarLayeredStyleEnabled,
        liquidSwitchEnabled,
        liquidBottomBarEnabled,
        bottomBarScrollEffectReductionEnabled,
        cardPressFeedbackEnabled
    ) {
        SettingsComponentEffectsSectionState(
            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
            liquidSwitchEnabled = liquidSwitchEnabled,
            liquidBottomBarEnabled = liquidBottomBarEnabled,
            bottomBarFullEffectDuringScrollEnabled = !bottomBarScrollEffectReductionEnabled,
            cardPressFeedbackEnabled = cardPressFeedbackEnabled
        )
    }
    val componentEffectsActions = remember(
        onLiquidActionBarLayeredStyleChanged,
        onLiquidSwitchChanged,
        onLiquidBottomBarChanged,
        onBottomBarScrollEffectReductionChanged,
        onCardPressFeedbackChanged
    ) {
        SettingsComponentEffectsSectionActions(
            onLiquidActionBarLayeredStyleChanged = onLiquidActionBarLayeredStyleChanged,
            onLiquidSwitchChanged = onLiquidSwitchChanged,
            onLiquidBottomBarChanged = onLiquidBottomBarChanged,
            onBottomBarFullEffectDuringScrollChanged = { enabled ->
                onBottomBarScrollEffectReductionChanged(!enabled)
            },
            onCardPressFeedbackChanged = onCardPressFeedbackChanged
        )
    }
    val notifyState = remember(
        superIslandNotificationEnabled,
        superIslandBypassRestrictionEnabled,
        superIslandRestoreDelayMs
    ) {
        SettingsNotifySectionState(
            superIslandNotificationEnabled = superIslandNotificationEnabled,
            superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
            superIslandRestoreDelayMs = superIslandRestoreDelayMs
        )
    }
    val notifyActions = remember(
        onSuperIslandNotificationChanged,
        onSuperIslandBypassRestrictionChanged,
        onSuperIslandRestoreDelayMsChanged
    ) {
        SettingsNotifySectionActions(
            onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
            onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
            onSuperIslandRestoreDelayMsChanged = onSuperIslandRestoreDelayMsChanged
        )
    }
    val copyState = remember(textCopyCapabilityExpanded) {
        SettingsCopySectionState(textCopyCapabilityExpanded = textCopyCapabilityExpanded)
    }
    val copyActions = remember(onTextCopyCapabilityExpandedChanged) {
        SettingsCopySectionActions(
            onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged
        )
    }
    return remember(
        permissionKeepAliveState,
        permissionKeepAliveActions,
        visualState,
        visualActions,
        animationState,
        animationActions,
        componentEffectsState,
        componentEffectsActions,
        notifyState,
        notifyActions,
        copyState,
        copyActions
    ) {
        SettingsSectionContractBundle(
            permissionKeepAliveState = permissionKeepAliveState,
            permissionKeepAliveActions = permissionKeepAliveActions,
            visualState = visualState,
            visualActions = visualActions,
            animationState = animationState,
            animationActions = animationActions,
            componentEffectsState = componentEffectsState,
            componentEffectsActions = componentEffectsActions,
            notifyState = notifyState,
            notifyActions = notifyActions,
            copyState = copyState,
            copyActions = copyActions
        )
    }
}
