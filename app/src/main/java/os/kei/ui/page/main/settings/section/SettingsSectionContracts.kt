package os.kei.ui.page.main.settings.section

import os.kei.ui.page.main.student.rendering.GuideViewerRendering
import androidx.compose.runtime.Immutable
import os.kei.core.background.AppBackgroundRecoverySnapshot
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.LauncherIconDesign
import os.kei.core.prefs.SuperIslandFloatBehavior
import os.kei.core.prefs.SuperIslandAutoClose
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckReason
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckStatus
import os.kei.ui.page.main.settings.support.SettingsAppListAccessMode
import os.kei.ui.page.main.settings.support.SettingsAppStandbyBucketState
import os.kei.ui.page.main.settings.support.SettingsOemAutoStartState
import os.kei.core.privilege.PrivilegeStatus
import os.kei.core.privilege.PrivilegeMode

@Immutable
internal data class SettingsPermissionKeepAliveSectionState(
    val notificationPermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val notificationSettingsActionAvailable: Boolean,
    val androidBackgroundRestricted: Boolean,
    val androidPowerSaveMode: Boolean,
    val androidDeviceIdleMode: Boolean,
    val appStandbyBucket: SettingsAppStandbyBucketState,
    val androidBackgroundSettingsActionAvailable: Boolean,
    val backgroundRecoverySnapshot: AppBackgroundRecoverySnapshot,
    val ignoringBatteryOptimizations: Boolean,
    val batteryOptimizationActionAvailable: Boolean,
    val oemAutoStartState: SettingsOemAutoStartState,
    val oemAutoStartVendorLabel: String,
    val oemAutoStartActionAvailable: Boolean,
    val appListAccessMode: SettingsAppListAccessMode,
    val appListPrivilegeMode: PrivilegeMode?,
    val appListDetectedCount: Int,
    val appListSettingsActionAvailable: Boolean,
    val privilegeMode: PrivilegeMode,
    val privilegeGranted: Boolean,
    val privilegeStatus: PrivilegeStatus,
    val accessibilityGuardState: SettingsAccessibilityGuardUiState,
)

internal data class SettingsPermissionKeepAliveSectionActions(
    val onRequestNotificationPermission: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onOpenAndroidBackgroundSettings: () -> Unit,
    val onOpenBatteryOptimizationSettings: () -> Unit,
    val onOpenOemAutoStartSettings: () -> Unit,
    val onOpenAppListPermissionSettings: () -> Unit,
    val onPrivilegeModeChanged: (PrivilegeMode) -> Unit,
    val onCheckOrRequestPrivilege: () -> Unit,
    val onAccessibilityGuardDaemonChanged: (Boolean) -> Unit,
    val onAccessibilityGuardBootCheckChanged: (Boolean) -> Unit,
    val onAccessibilityGuardScreenOnChanged: (Boolean) -> Unit,
    val onRunAccessibilityGuardCheck: () -> Unit,
    val onExportAccessibilityGuardHistory: () -> Unit,
)

@Immutable
internal data class SettingsAccessibilityGuardUiState(
    val loading: Boolean = false,
    val manualCheckRunning: Boolean = false,
    val exportingHistory: Boolean = false,
    val daemonEnabled: Boolean = false,
    val bootCheckEnabled: Boolean = false,
    val screenOnCheckEnabled: Boolean = false,
    val secureSettingsReadable: Boolean = false,
    val privilegeStatus: String = "",
    val activePolicyCount: Int = 0,
    val historyCount: Int = 0,
    val latestHistory: SettingsAccessibilityGuardHistoryUiItem? = null,
)

@Immutable
internal data class SettingsAccessibilityGuardHistoryUiItem(
    val timestampMs: Long,
    val reason: AccessibilityGuardCheckReason,
    val status: AccessibilityGuardCheckStatus,
    val triggerAction: String,
    val checkCount: Int,
    val healthyCount: Int,
    val warningCount: Int,
    val elapsedMs: Long,
    val failureReason: String,
)

@Immutable
internal data class SettingsVisualSectionState(
    val preloadingEnabled: Boolean,
    val launcherIconDesign: LauncherIconDesign,
    val homeIconHdrEnabled: Boolean,
    val homeDynamicFullEffectEnabled: Boolean,
    val appThemeMode: AppThemeMode,
    val appLanguageActionAvailable: Boolean,
    val showThemeModePopup: Boolean,
    val showLauncherIconDesignPopup: Boolean,
    val lobbyRendering: GuideViewerRendering = GuideViewerRendering.SystemWebView,
    val modelRendering: GuideViewerRendering = GuideViewerRendering.SystemWebView,
)

internal data class SettingsVisualSectionActions(
    val onPreloadingEnabledChanged: (Boolean) -> Unit,
    val onLauncherIconDesignChanged: (LauncherIconDesign) -> Unit,
    val onHomeIconHdrChanged: (Boolean) -> Unit,
    val onHomeDynamicFullEffectChanged: (Boolean) -> Unit,
    val onAppThemeModeChanged: (AppThemeMode) -> Unit,
    val onOpenAppLanguageSettings: () -> Unit,
    val onShowThemeModePopupChange: (Boolean) -> Unit,
    val onShowLauncherIconDesignPopupChange: (Boolean) -> Unit,
    val onLobbyRenderingChanged: (GuideViewerRendering) -> Unit = {},
    val onModelRenderingChanged: (GuideViewerRendering) -> Unit = {},
)

@Immutable
internal data class SettingsAnimationSectionState(
    val transitionAnimationsEnabled: Boolean,
    val predictiveBackAnimationsEnabled: Boolean,
)

internal data class SettingsAnimationSectionActions(
    val onTransitionAnimationsChanged: (Boolean) -> Unit,
    val onPredictiveBackAnimationsChanged: (Boolean) -> Unit,
)

@Immutable
internal data class SettingsComponentEffectsSectionState(
    val liquidSwitchEnabled: Boolean,
    val liquidToastEnabled: Boolean,
    val reduceToastInterruptionEnabled: Boolean,
    val searchAutoFocusEnabled: Boolean,
    val gripAwareFloatingDockEnabled: Boolean,
)

internal data class SettingsComponentEffectsSectionActions(
    val onLiquidSwitchChanged: (Boolean) -> Unit,
    val onLiquidToastChanged: (Boolean) -> Unit,
    val onReduceToastInterruptionChanged: (Boolean) -> Unit,
    val onSearchAutoFocusChanged: (Boolean) -> Unit,
    val onGripAwareFloatingDockChanged: (Boolean) -> Unit,
)

@Immutable
internal data class SettingsNotifySectionState(
    val superIslandNotificationEnabled: Boolean,
    val superIslandFloatBehavior: SuperIslandFloatBehavior,
    val superIslandAutoClose: SuperIslandAutoClose,
    val superIslandBypassRestrictionEnabled: Boolean,
    val superIslandRestoreDelayMs: Int,
)

internal data class SettingsNotifySectionActions(
    val onSuperIslandNotificationChanged: (Boolean) -> Unit,
    val onSuperIslandFloatBehaviorChanged: (SuperIslandFloatBehavior) -> Unit,
    val onSuperIslandAutoCloseChanged: (SuperIslandAutoClose) -> Unit,
    val onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    val onSuperIslandRestoreDelayMsChanged: (Int) -> Unit,
)

@Immutable
internal data class SettingsCopySectionState(
    val textCopyCapabilityExpanded: Boolean,
)

internal data class SettingsCopySectionActions(
    val onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit,
)
