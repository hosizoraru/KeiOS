@file:Suppress("ktlint:standard:filename")

package os.kei.ui.page.main.settings.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import os.kei.ui.page.main.student.rendering.GuideViewerRendering
import os.kei.ui.page.main.student.rendering.GuideViewerRenderingPreferences
import os.kei.core.background.AppBackgroundRecoverySnapshot
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.LauncherIconDesign
import os.kei.core.prefs.SuperIslandFloatBehavior
import os.kei.core.prefs.SuperIslandAutoClose
import os.kei.ui.page.main.settings.section.SettingsAnimationSectionActions
import os.kei.ui.page.main.settings.section.SettingsAnimationSectionState
import os.kei.ui.page.main.settings.section.SettingsComponentEffectsSectionActions
import os.kei.ui.page.main.settings.section.SettingsComponentEffectsSectionState
import os.kei.ui.page.main.settings.section.SettingsCopySectionActions
import os.kei.ui.page.main.settings.section.SettingsCopySectionState
import os.kei.ui.page.main.settings.section.SettingsNotifySectionActions
import os.kei.ui.page.main.settings.section.SettingsNotifySectionState
import os.kei.ui.page.main.settings.section.SettingsAccessibilityGuardUiState
import os.kei.ui.page.main.settings.section.SettingsPermissionKeepAliveSectionActions
import os.kei.ui.page.main.settings.section.SettingsPermissionKeepAliveSectionState
import os.kei.ui.page.main.settings.section.SettingsVisualSectionActions
import os.kei.ui.page.main.settings.section.SettingsVisualSectionState
import os.kei.ui.page.main.settings.support.SettingsAppListAccessMode
import os.kei.ui.page.main.settings.support.SettingsAppStandbyBucketState
import os.kei.ui.page.main.settings.support.SettingsOemAutoStartState
import os.kei.core.privilege.PrivilegeStatus
import os.kei.core.privilege.PrivilegeMode

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
    val copyActions: SettingsCopySectionActions,
)

@Composable
internal fun rememberSettingsSectionContractBundle(
    notificationPermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    notificationSettingsActionAvailable: Boolean,
    androidBackgroundRestricted: Boolean,
    androidPowerSaveMode: Boolean,
    androidDeviceIdleMode: Boolean,
    appStandbyBucket: SettingsAppStandbyBucketState,
    androidBackgroundSettingsActionAvailable: Boolean,
    backgroundRecoverySnapshot: AppBackgroundRecoverySnapshot,
    preloadingEnabled: Boolean,
    launcherIconDesign: LauncherIconDesign,
    homeIconHdrEnabled: Boolean,
    homeDynamicFullEffectEnabled: Boolean,
    appThemeMode: AppThemeMode,
    appLanguageActionAvailable: Boolean,
    transitionAnimationsEnabled: Boolean,
    predictiveBackAnimationsEnabled: Boolean,
    liquidSwitchEnabled: Boolean,
    liquidToastEnabled: Boolean,
    reduceToastInterruptionEnabled: Boolean,
    searchAutoFocusEnabled: Boolean,
    gripAwareFloatingDockEnabled: Boolean,
    superIslandNotificationEnabled: Boolean,
    superIslandFloatBehavior: SuperIslandFloatBehavior,
    superIslandAutoClose: SuperIslandAutoClose,
    superIslandBypassRestrictionEnabled: Boolean,
    superIslandRestoreDelayMs: Int,
    ignoringBatteryOptimizations: Boolean,
    batteryOptimizationActionAvailable: Boolean,
    oemAutoStartState: SettingsOemAutoStartState,
    oemAutoStartVendorLabel: String,
    oemAutoStartActionAvailable: Boolean,
    appListAccessMode: SettingsAppListAccessMode,
    appListPrivilegeMode: PrivilegeMode?,
    appListDetectedCount: Int,
    appListSettingsActionAvailable: Boolean,
    privilegeMode: PrivilegeMode,
    privilegeGranted: Boolean,
    privilegeStatus: PrivilegeStatus,
    accessibilityGuardState: SettingsAccessibilityGuardUiState,
    textCopyCapabilityExpanded: Boolean,
    pageUiState: SettingsPageUiState,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAndroidBackgroundSettings: () -> Unit,
    onPreloadingEnabledChanged: (Boolean) -> Unit,
    onLauncherIconDesignChanged: (LauncherIconDesign) -> Unit,
    onHomeIconHdrChanged: (Boolean) -> Unit,
    onHomeDynamicFullEffectChanged: (Boolean) -> Unit,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    onOpenAppLanguageSettings: () -> Unit,
    onTransitionAnimationsChanged: (Boolean) -> Unit,
    onPredictiveBackAnimationsChanged: (Boolean) -> Unit,
    onLiquidSwitchChanged: (Boolean) -> Unit,
    onLiquidToastChanged: (Boolean) -> Unit,
    onReduceToastInterruptionChanged: (Boolean) -> Unit,
    onSearchAutoFocusChanged: (Boolean) -> Unit,
    onGripAwareFloatingDockChanged: (Boolean) -> Unit,
    onSuperIslandNotificationChanged: (Boolean) -> Unit,
    onSuperIslandFloatBehaviorChanged: (SuperIslandFloatBehavior) -> Unit,
    onSuperIslandAutoCloseChanged: (SuperIslandAutoClose) -> Unit,
    onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    onSuperIslandRestoreDelayMsChanged: (Int) -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenOemAutoStartSettings: () -> Unit,
    onOpenAppListPermissionSettings: () -> Unit,
    onPrivilegeModeChanged: (PrivilegeMode) -> Unit,
    onCheckOrRequestPrivilege: () -> Unit,
    onAccessibilityGuardDaemonChanged: (Boolean) -> Unit,
    onAccessibilityGuardBootCheckChanged: (Boolean) -> Unit,
    onAccessibilityGuardScreenOnChanged: (Boolean) -> Unit,
    onRunAccessibilityGuardCheck: () -> Unit,
    onExportAccessibilityGuardHistory: () -> Unit,
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit,
    renderingPreferences: GuideViewerRenderingPreferences,
    onLobbyRenderingChanged: (GuideViewerRendering) -> Unit,
    onModelRenderingChanged: (GuideViewerRendering) -> Unit,
): SettingsSectionContractBundle {
    val permissionKeepAliveState =
        remember(
            notificationPermissionGranted,
            notificationsEnabled,
            notificationSettingsActionAvailable,
            androidBackgroundRestricted,
            androidPowerSaveMode,
            androidDeviceIdleMode,
            appStandbyBucket,
            androidBackgroundSettingsActionAvailable,
            backgroundRecoverySnapshot,
            ignoringBatteryOptimizations,
            batteryOptimizationActionAvailable,
            oemAutoStartState,
            oemAutoStartVendorLabel,
            oemAutoStartActionAvailable,
            appListAccessMode,
            appListPrivilegeMode,
            appListDetectedCount,
            appListSettingsActionAvailable,
            privilegeMode,
            privilegeGranted,
            privilegeStatus,
            accessibilityGuardState,
        ) {
            SettingsPermissionKeepAliveSectionState(
                notificationPermissionGranted = notificationPermissionGranted,
                notificationsEnabled = notificationsEnabled,
                notificationSettingsActionAvailable = notificationSettingsActionAvailable,
                androidBackgroundRestricted = androidBackgroundRestricted,
                androidPowerSaveMode = androidPowerSaveMode,
                androidDeviceIdleMode = androidDeviceIdleMode,
                appStandbyBucket = appStandbyBucket,
                androidBackgroundSettingsActionAvailable = androidBackgroundSettingsActionAvailable,
                backgroundRecoverySnapshot = backgroundRecoverySnapshot,
                ignoringBatteryOptimizations = ignoringBatteryOptimizations,
                batteryOptimizationActionAvailable = batteryOptimizationActionAvailable,
                oemAutoStartState = oemAutoStartState,
                oemAutoStartVendorLabel = oemAutoStartVendorLabel,
                oemAutoStartActionAvailable = oemAutoStartActionAvailable,
                appListAccessMode = appListAccessMode,
                appListPrivilegeMode = appListPrivilegeMode,
                appListDetectedCount = appListDetectedCount,
                appListSettingsActionAvailable = appListSettingsActionAvailable,
                privilegeMode = privilegeMode,
                privilegeGranted = privilegeGranted,
                privilegeStatus = privilegeStatus,
                accessibilityGuardState = accessibilityGuardState,
            )
        }
    val permissionKeepAliveActions =
        remember(
            onRequestNotificationPermission,
            onOpenNotificationSettings,
            onOpenAndroidBackgroundSettings,
            onOpenBatteryOptimizationSettings,
            onOpenOemAutoStartSettings,
            onOpenAppListPermissionSettings,
            onPrivilegeModeChanged,
            onCheckOrRequestPrivilege,
            onAccessibilityGuardDaemonChanged,
            onAccessibilityGuardBootCheckChanged,
            onAccessibilityGuardScreenOnChanged,
            onRunAccessibilityGuardCheck,
            onExportAccessibilityGuardHistory,
        ) {
            SettingsPermissionKeepAliveSectionActions(
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenAndroidBackgroundSettings = onOpenAndroidBackgroundSettings,
                onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                onOpenOemAutoStartSettings = onOpenOemAutoStartSettings,
                onOpenAppListPermissionSettings = onOpenAppListPermissionSettings,
                onPrivilegeModeChanged = onPrivilegeModeChanged,
                onCheckOrRequestPrivilege = onCheckOrRequestPrivilege,
                onAccessibilityGuardDaemonChanged = onAccessibilityGuardDaemonChanged,
                onAccessibilityGuardBootCheckChanged = onAccessibilityGuardBootCheckChanged,
                onAccessibilityGuardScreenOnChanged = onAccessibilityGuardScreenOnChanged,
                onRunAccessibilityGuardCheck = onRunAccessibilityGuardCheck,
                onExportAccessibilityGuardHistory = onExportAccessibilityGuardHistory,
            )
        }
    val visualState =
        remember(
            preloadingEnabled,
            launcherIconDesign,
            homeIconHdrEnabled,
            homeDynamicFullEffectEnabled,
            appThemeMode,
            appLanguageActionAvailable,
            pageUiState.showThemeModePopup,
            pageUiState.showLauncherIconDesignPopup,
            renderingPreferences,
        ) {
            SettingsVisualSectionState(
                preloadingEnabled = preloadingEnabled,
                launcherIconDesign = launcherIconDesign,
                homeIconHdrEnabled = homeIconHdrEnabled,
                homeDynamicFullEffectEnabled = homeDynamicFullEffectEnabled,
                appThemeMode = appThemeMode,
                appLanguageActionAvailable = appLanguageActionAvailable,
                showThemeModePopup = pageUiState.showThemeModePopup,
                showLauncherIconDesignPopup = pageUiState.showLauncherIconDesignPopup,
                lobbyRendering = renderingPreferences.lobby,
                modelRendering = renderingPreferences.model,
            )
        }
    val visualActions =
        remember(
            onPreloadingEnabledChanged,
            onLauncherIconDesignChanged,
            onHomeIconHdrChanged,
            onHomeDynamicFullEffectChanged,
            onAppThemeModeChanged,
            onOpenAppLanguageSettings,
            onLobbyRenderingChanged,
            onModelRenderingChanged,
        ) {
            SettingsVisualSectionActions(
                onPreloadingEnabledChanged = onPreloadingEnabledChanged,
                onLauncherIconDesignChanged = onLauncherIconDesignChanged,
                onHomeIconHdrChanged = onHomeIconHdrChanged,
                onHomeDynamicFullEffectChanged = onHomeDynamicFullEffectChanged,
                onAppThemeModeChanged = onAppThemeModeChanged,
                onOpenAppLanguageSettings = onOpenAppLanguageSettings,
                onShowThemeModePopupChange = { pageUiState.showThemeModePopup = it },
                onShowLauncherIconDesignPopupChange = {
                    pageUiState.showLauncherIconDesignPopup = it
                },
                onLobbyRenderingChanged = onLobbyRenderingChanged,
                onModelRenderingChanged = onModelRenderingChanged,
            )
        }
    val animationState =
        remember(
            transitionAnimationsEnabled,
            predictiveBackAnimationsEnabled,
        ) {
            SettingsAnimationSectionState(
                transitionAnimationsEnabled = transitionAnimationsEnabled,
                predictiveBackAnimationsEnabled = predictiveBackAnimationsEnabled,
            )
        }
    val animationActions =
        remember(
            onTransitionAnimationsChanged,
            onPredictiveBackAnimationsChanged,
        ) {
            SettingsAnimationSectionActions(
                onTransitionAnimationsChanged = onTransitionAnimationsChanged,
                onPredictiveBackAnimationsChanged = onPredictiveBackAnimationsChanged,
            )
        }
    val componentEffectsState =
        remember(
            liquidSwitchEnabled,
            liquidToastEnabled,
            reduceToastInterruptionEnabled,
            searchAutoFocusEnabled,
            gripAwareFloatingDockEnabled,
        ) {
            SettingsComponentEffectsSectionState(
                liquidSwitchEnabled = liquidSwitchEnabled,
                liquidToastEnabled = liquidToastEnabled,
                reduceToastInterruptionEnabled = reduceToastInterruptionEnabled,
                searchAutoFocusEnabled = searchAutoFocusEnabled,
                gripAwareFloatingDockEnabled = gripAwareFloatingDockEnabled,
            )
        }
    val componentEffectsActions =
        remember(
            onLiquidSwitchChanged,
            onLiquidToastChanged,
            onReduceToastInterruptionChanged,
            onSearchAutoFocusChanged,
            onGripAwareFloatingDockChanged,
        ) {
            SettingsComponentEffectsSectionActions(
                onLiquidSwitchChanged = onLiquidSwitchChanged,
                onLiquidToastChanged = onLiquidToastChanged,
                onReduceToastInterruptionChanged = onReduceToastInterruptionChanged,
                onSearchAutoFocusChanged = onSearchAutoFocusChanged,
                onGripAwareFloatingDockChanged = onGripAwareFloatingDockChanged,
            )
        }
    val notifyState =
        remember(
            superIslandNotificationEnabled,
            superIslandFloatBehavior,
            superIslandAutoClose,
            superIslandBypassRestrictionEnabled,
            superIslandRestoreDelayMs,
        ) {
            SettingsNotifySectionState(
                superIslandNotificationEnabled = superIslandNotificationEnabled,
                superIslandFloatBehavior = superIslandFloatBehavior,
                superIslandAutoClose = superIslandAutoClose,
                superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
                superIslandRestoreDelayMs = superIslandRestoreDelayMs,
            )
        }
    val notifyActions =
        remember(
            onSuperIslandNotificationChanged,
            onSuperIslandFloatBehaviorChanged,
            onSuperIslandAutoCloseChanged,
            onSuperIslandBypassRestrictionChanged,
            onSuperIslandRestoreDelayMsChanged,
        ) {
            SettingsNotifySectionActions(
                onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
                onSuperIslandFloatBehaviorChanged = onSuperIslandFloatBehaviorChanged,
                onSuperIslandAutoCloseChanged = onSuperIslandAutoCloseChanged,
                onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
                onSuperIslandRestoreDelayMsChanged = onSuperIslandRestoreDelayMsChanged,
            )
        }
    val copyState =
        remember(textCopyCapabilityExpanded) {
            SettingsCopySectionState(textCopyCapabilityExpanded = textCopyCapabilityExpanded)
        }
    val copyActions =
        remember(onTextCopyCapabilityExpandedChanged) {
            SettingsCopySectionActions(
                onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged,
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
        copyActions,
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
            copyActions = copyActions,
        )
    }
}
