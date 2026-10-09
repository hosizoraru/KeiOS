package os.kei.ui.page.main.settings.page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import os.kei.ui.page.main.student.rendering.GuideViewerRenderingStore
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.LauncherIconDesign
import os.kei.core.prefs.SuperIslandFloatBehavior
import os.kei.core.prefs.SuperIslandAutoClose
import os.kei.ui.page.main.settings.section.SettingsAccessibilityGuardUiState
import os.kei.ui.page.main.settings.state.SettingsPageUiState
import os.kei.ui.page.main.settings.state.SettingsPageViewModel
import os.kei.ui.page.main.settings.state.SettingsSectionContractBundle
import os.kei.ui.page.main.settings.state.rememberSettingsSectionContractBundle
import os.kei.ui.page.main.settings.support.SettingsAppLanguageController
import os.kei.ui.page.main.settings.support.SettingsBatteryOptimizationController
import os.kei.ui.page.main.settings.support.SettingsBatteryOptimizationSnapshot
import os.kei.ui.page.main.settings.support.SettingsPermissionKeepAliveController
import os.kei.ui.page.main.settings.support.SettingsPermissionKeepAliveSnapshot
import os.kei.core.privilege.PrivilegeMode

@Composable
internal fun rememberSettingsPageSectionContracts(
    context: Context,
    pageUiState: SettingsPageUiState,
    settingsPageViewModel: SettingsPageViewModel,
    permissionKeepAliveState: SettingsPermissionKeepAliveSnapshot,
    batteryOptimizationState: SettingsBatteryOptimizationSnapshot,
    accessibilityGuardState: SettingsAccessibilityGuardUiState,
    permissionKeepAliveController: SettingsPermissionKeepAliveController,
    batteryOptimizationController: SettingsBatteryOptimizationController,
    appLanguageController: SettingsAppLanguageController,
    notificationPermissionGranted: Boolean,
    preloadingEnabled: Boolean,
    onPreloadingEnabledChanged: (Boolean) -> Unit,
    launcherIconDesign: LauncherIconDesign,
    onLauncherIconDesignChanged: (LauncherIconDesign) -> Unit,
    homeIconHdrEnabled: Boolean,
    onHomeIconHdrChanged: (Boolean) -> Unit,
    homeDynamicFullEffectEnabled: Boolean,
    onHomeDynamicFullEffectChanged: (Boolean) -> Unit,
    appThemeMode: AppThemeMode,
    onAppThemeModeChanged: (AppThemeMode) -> Unit,
    transitionAnimationsEnabled: Boolean,
    onTransitionAnimationsChanged: (Boolean) -> Unit,
    predictiveBackAnimationsEnabled: Boolean,
    onPredictiveBackAnimationsChanged: (Boolean) -> Unit,
    searchAutoFocusEnabled: Boolean,
    onSearchAutoFocusChanged: (Boolean) -> Unit,
    liquidSwitchEnabled: Boolean,
    onLiquidSwitchChanged: (Boolean) -> Unit,
    liquidToastEnabled: Boolean,
    onLiquidToastChanged: (Boolean) -> Unit,
    reduceToastInterruptionEnabled: Boolean,
    onReduceToastInterruptionChanged: (Boolean) -> Unit,
    gripAwareFloatingDockEnabled: Boolean,
    onGripAwareFloatingDockChanged: (Boolean) -> Unit,
    superIslandNotificationEnabled: Boolean,
    onSuperIslandNotificationChanged: (Boolean) -> Unit,
    superIslandFloatBehavior: SuperIslandFloatBehavior,
    superIslandAutoClose: SuperIslandAutoClose,
    onSuperIslandFloatBehaviorChanged: (SuperIslandFloatBehavior) -> Unit,
    onSuperIslandAutoCloseChanged: (SuperIslandAutoClose) -> Unit,
    superIslandBypassRestrictionEnabled: Boolean,
    onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    superIslandRestoreDelayMs: Int,
    onSuperIslandRestoreDelayMsChanged: (Int) -> Unit,
    textCopyCapabilityExpanded: Boolean,
    onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    privilegeMode: PrivilegeMode,
    onPrivilegeModeChanged: (PrivilegeMode) -> Unit,
    onCheckOrRequestPrivilege: () -> Unit,
): SettingsSectionContractBundle {
    val renderingPreferences by GuideViewerRenderingStore.preferences.collectAsStateWithLifecycle()
    return rememberSettingsSectionContractBundle(
        notificationPermissionGranted = notificationPermissionGranted,
        notificationsEnabled = permissionKeepAliveState.notificationsEnabled,
        notificationSettingsActionAvailable = permissionKeepAliveState.notificationSettingsActionAvailable,
        androidBackgroundRestricted = permissionKeepAliveState.androidBackgroundRestricted,
        androidPowerSaveMode = permissionKeepAliveState.androidPowerSaveMode,
        androidDeviceIdleMode = permissionKeepAliveState.androidDeviceIdleMode,
        appStandbyBucket = permissionKeepAliveState.appStandbyBucket,
        androidBackgroundSettingsActionAvailable =
            permissionKeepAliveState.androidBackgroundSettingsActionAvailable,
        backgroundRecoverySnapshot = permissionKeepAliveState.backgroundRecoverySnapshot,
        preloadingEnabled = preloadingEnabled,
        launcherIconDesign = launcherIconDesign,
        homeIconHdrEnabled = homeIconHdrEnabled,
        homeDynamicFullEffectEnabled = homeDynamicFullEffectEnabled,
        appThemeMode = appThemeMode,
        appLanguageActionAvailable = appLanguageController.actionAvailable,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        predictiveBackAnimationsEnabled = predictiveBackAnimationsEnabled,
        searchAutoFocusEnabled = searchAutoFocusEnabled,
        liquidSwitchEnabled = liquidSwitchEnabled,
        liquidToastEnabled = liquidToastEnabled,
        reduceToastInterruptionEnabled = reduceToastInterruptionEnabled,
        gripAwareFloatingDockEnabled = gripAwareFloatingDockEnabled,
        superIslandNotificationEnabled = superIslandNotificationEnabled,
        superIslandFloatBehavior = superIslandFloatBehavior,
        superIslandAutoClose = superIslandAutoClose,
        superIslandBypassRestrictionEnabled = superIslandBypassRestrictionEnabled,
        superIslandRestoreDelayMs = superIslandRestoreDelayMs,
        ignoringBatteryOptimizations = batteryOptimizationState.ignoringBatteryOptimizations,
        batteryOptimizationActionAvailable = batteryOptimizationState.requestActionAvailable,
        oemAutoStartState = permissionKeepAliveState.oemAutoStartState,
        oemAutoStartVendorLabel = permissionKeepAliveState.oemAutoStartVendorLabel,
        oemAutoStartActionAvailable = permissionKeepAliveState.oemAutoStartActionAvailable,
        appListAccessMode = permissionKeepAliveState.appListAccessMode,
        appListPrivilegeMode = permissionKeepAliveState.appListPrivilegeMode,
        appListDetectedCount = permissionKeepAliveState.appListDetectedCount,
        appListSettingsActionAvailable = permissionKeepAliveState.appListSettingsActionAvailable,
        privilegeMode = privilegeMode,
        privilegeGranted = permissionKeepAliveState.privilegeGranted,
        privilegeStatus = permissionKeepAliveState.privilegeStatus,
        accessibilityGuardState = accessibilityGuardState,
        textCopyCapabilityExpanded = textCopyCapabilityExpanded,
        pageUiState = pageUiState,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onOpenNotificationSettings = {
            showSettingsToastIfClosed(
                context = context,
                opened = permissionKeepAliveController.openNotificationSettings(),
                messageRes = R.string.settings_notification_permission_toast_open_failed,
            )
        },
        onOpenAndroidBackgroundSettings = {
            showSettingsToastIfClosed(
                context = context,
                opened = permissionKeepAliveController.openAndroidBackgroundSettings(),
                messageRes = R.string.settings_android_background_toast_open_failed,
            )
        },
        onPreloadingEnabledChanged = onPreloadingEnabledChanged,
        onLauncherIconDesignChanged = onLauncherIconDesignChanged,
        onHomeIconHdrChanged = onHomeIconHdrChanged,
        onHomeDynamicFullEffectChanged = onHomeDynamicFullEffectChanged,
        onAppThemeModeChanged = onAppThemeModeChanged,
        onOpenAppLanguageSettings = {
            showSettingsToastIfClosed(
                context = context,
                opened = appLanguageController.openAppLanguageSettings(),
                messageRes = R.string.settings_app_language_toast_open_failed,
            )
        },
        onTransitionAnimationsChanged = onTransitionAnimationsChanged,
        onPredictiveBackAnimationsChanged = onPredictiveBackAnimationsChanged,
        onSearchAutoFocusChanged = onSearchAutoFocusChanged,
        onLiquidSwitchChanged = onLiquidSwitchChanged,
        onLiquidToastChanged = onLiquidToastChanged,
        onReduceToastInterruptionChanged = onReduceToastInterruptionChanged,
        onGripAwareFloatingDockChanged = onGripAwareFloatingDockChanged,
        onSuperIslandNotificationChanged = onSuperIslandNotificationChanged,
        onSuperIslandFloatBehaviorChanged = onSuperIslandFloatBehaviorChanged,
        onSuperIslandAutoCloseChanged = onSuperIslandAutoCloseChanged,
        onSuperIslandBypassRestrictionChanged = onSuperIslandBypassRestrictionChanged,
        onSuperIslandRestoreDelayMsChanged = onSuperIslandRestoreDelayMsChanged,
        onOpenBatteryOptimizationSettings = {
            showSettingsToastIfClosed(
                context = context,
                opened = batteryOptimizationController.openBatteryOptimizationSettings(batteryOptimizationState),
                messageRes = R.string.settings_battery_optimization_toast_open_failed,
            )
        },
        onOpenOemAutoStartSettings = {
            showSettingsToastIfClosed(
                context = context,
                opened = permissionKeepAliveController.openOemAutoStartSettings(),
                messageRes = R.string.settings_oem_autostart_toast_open_failed,
            )
        },
        onOpenAppListPermissionSettings = {
            showSettingsToastIfClosed(
                context = context,
                opened = permissionKeepAliveController.openAppListPermissionSettings(),
                messageRes = R.string.settings_app_list_access_toast_open_failed,
            )
        },
        onPrivilegeModeChanged = onPrivilegeModeChanged,
        onCheckOrRequestPrivilege = onCheckOrRequestPrivilege,
        onAccessibilityGuardDaemonChanged = { enabled ->
            settingsPageViewModel.updateAccessibilityGuardDaemonEnabled(
                context = context,
                enabled = enabled,
            )
        },
        onAccessibilityGuardBootCheckChanged = { enabled ->
            settingsPageViewModel.updateAccessibilityGuardBootCheckEnabled(
                context = context,
                enabled = enabled,
            )
        },
        onAccessibilityGuardScreenOnChanged = { enabled ->
            settingsPageViewModel.updateAccessibilityGuardScreenOnEnabled(
                context = context,
                enabled = enabled,
            )
        },
        onRunAccessibilityGuardCheck = {
            settingsPageViewModel.runAccessibilityGuardManualCheck(context)
        },
        onExportAccessibilityGuardHistory = {
            settingsPageViewModel.beginAccessibilityGuardHistoryExport()
        },
        onTextCopyCapabilityExpandedChanged = onTextCopyCapabilityExpandedChanged,
        renderingPreferences = renderingPreferences,
        onLobbyRenderingChanged = GuideViewerRenderingStore::setLobby,
        onModelRenderingChanged = GuideViewerRenderingStore::setModel,
    )
}

private fun showSettingsToastIfClosed(
    context: Context,
    opened: Boolean,
    messageRes: Int,
) {
    if (opened) return
    context.showToast(messageRes)
}
