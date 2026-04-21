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
    transitionAnimationsEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    liquidBottomBarEnabled: Boolean,
    liquidGlassSwitchEnabled: Boolean,
    cardPressFeedbackEnabled: Boolean,
    superIslandNotificationEnabled: Boolean,
    superIslandBypassRestrictionEnabled: Boolean,
    ignoringBatteryOptimizations: Boolean,
    batteryOptimizationActionAvailable: Boolean,
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
    onTransitionAnimationsChanged: (Boolean) -> Unit,
    onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
    onLiquidBottomBarChanged: (Boolean) -> Unit,
    onLiquidGlassSwitchChanged: (Boolean) -> Unit,
    onCardPressFeedbackChanged: (Boolean) -> Unit,
    onSuperIslandNotificationChanged: (Boolean) -> Unit,
    onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
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
        onOpenAppListPermissionSettings,
        onCheckOrRequestShizuku
    ) {
        SettingsPermissionKeepAliveSectionActions(
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
            onOpenAppListPermissionSettings = onOpenAppListPermissionSettings,
            onCheckOrRequestShizuku = onCheckOrRequestShizuku
        )
    }
    val visualState = remember(
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
    val visualActions = remember(
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
    val animationState = remember(transitionAnimationsEnabled) {
        SettingsAnimationSectionState(
            transitionAnimationsEnabled = transitionAnimationsEnabled
        )
    }
    val animationActions = remember(onTransitionAnimationsChanged) {
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
    val notifyState = remember(
        superIslandNotificationEnabled,
        superIslandBypassRestrictionEnabled
    ) {
        SettingsNotifySectionState(
            superIslandNotificationEnabled = superIslandNotificationEnabled,
            superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled
        )
    }
    val notifyActions = remember(
        onSuperIslandNotificationChanged,
        onSuperIslandBypassRestrictionChanged
    ) {
        SettingsNotifySectionActions(
            onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
            onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged
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
