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
import os.kei.ui.page.main.settings.section.SettingsNotifySectionActions
import os.kei.ui.page.main.settings.section.SettingsNotifySectionState
import os.kei.ui.page.main.settings.section.SettingsVisualSectionActions
import os.kei.ui.page.main.settings.section.SettingsVisualSectionState

internal data class SettingsSectionContractBundle(
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
    textCopyCapabilityExpanded: Boolean,
    pageUiState: SettingsPageUiState,
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
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit
): SettingsSectionContractBundle {
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
        superIslandBypassRestrictionEnabled,
        ignoringBatteryOptimizations,
        batteryOptimizationActionAvailable
    ) {
        SettingsNotifySectionState(
            superIslandNotificationEnabled = superIslandNotificationEnabled,
            superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
            ignoringBatteryOptimizations = ignoringBatteryOptimizations,
            batteryOptimizationActionAvailable = batteryOptimizationActionAvailable
        )
    }
    val notifyActions = remember(
        onSuperIslandNotificationChanged,
        onSuperIslandBypassRestrictionChanged,
        onOpenBatteryOptimizationSettings
    ) {
        SettingsNotifySectionActions(
            onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
            onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
            onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings
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
