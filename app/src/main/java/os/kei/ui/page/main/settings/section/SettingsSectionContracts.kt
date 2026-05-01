package os.kei.ui.page.main.settings.section

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntRect
import os.kei.core.prefs.AppThemeMode
import os.kei.ui.page.main.settings.support.SettingsAppListAccessMode
import os.kei.ui.page.main.settings.support.SettingsOemAutoStartState

@Immutable
internal data class SettingsPermissionKeepAliveSectionState(
    val notificationPermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val notificationSettingsActionAvailable: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val batteryOptimizationActionAvailable: Boolean,
    val oemAutoStartState: SettingsOemAutoStartState,
    val oemAutoStartVendorLabel: String,
    val oemAutoStartActionAvailable: Boolean,
    val appListAccessMode: SettingsAppListAccessMode,
    val appListDetectedCount: Int,
    val appListSettingsActionAvailable: Boolean,
    val shizukuGranted: Boolean,
    val shizukuStatusText: String
)

internal data class SettingsPermissionKeepAliveSectionActions(
    val onRequestNotificationPermission: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onOpenBatteryOptimizationSettings: () -> Unit,
    val onOpenOemAutoStartSettings: () -> Unit,
    val onOpenAppListPermissionSettings: () -> Unit,
    val onCheckOrRequestShizuku: () -> Unit
)

@Immutable
internal data class SettingsVisualSectionState(
    val preloadingEnabled: Boolean,
    val homeIconHdrEnabled: Boolean,
    val appThemeMode: AppThemeMode,
    val appLanguageActionAvailable: Boolean,
    val showThemeModePopup: Boolean,
    val themePopupAnchorBounds: IntRect?
)

internal data class SettingsVisualSectionActions(
    val onPreloadingEnabledChanged: (Boolean) -> Unit,
    val onHomeIconHdrChanged: (Boolean) -> Unit,
    val onAppThemeModeChanged: (AppThemeMode) -> Unit,
    val onOpenAppLanguageSettings: () -> Unit,
    val onShowThemeModePopupChange: (Boolean) -> Unit,
    val onThemePopupAnchorBoundsChange: (IntRect?) -> Unit
)

@Immutable
internal data class SettingsAnimationSectionState(
    val transitionAnimationsEnabled: Boolean,
    val predictiveBackAnimationsEnabled: Boolean
)

internal data class SettingsAnimationSectionActions(
    val onTransitionAnimationsChanged: (Boolean) -> Unit,
    val onPredictiveBackAnimationsChanged: (Boolean) -> Unit
)

@Immutable
internal data class SettingsComponentEffectsSectionState(
    val liquidActionBarLayeredStyleEnabled: Boolean,
    val liquidBottomBarEnabled: Boolean,
    val bottomBarFullEffectDuringScrollEnabled: Boolean,
    val cardPressFeedbackEnabled: Boolean
)

internal data class SettingsComponentEffectsSectionActions(
    val onLiquidActionBarLayeredStyleChanged: (Boolean) -> Unit,
    val onLiquidBottomBarChanged: (Boolean) -> Unit,
    val onBottomBarFullEffectDuringScrollChanged: (Boolean) -> Unit,
    val onCardPressFeedbackChanged: (Boolean) -> Unit
)

@Immutable
internal data class SettingsNotifySectionState(
    val superIslandNotificationEnabled: Boolean,
    val superIslandBypassRestrictionEnabled: Boolean,
    val superIslandRestoreDelayMs: Int
)

internal data class SettingsNotifySectionActions(
    val onSuperIslandNotificationChanged: (Boolean) -> Unit,
    val onSuperIslandBypassRestrictionChanged: (Boolean) -> Unit,
    val onSuperIslandRestoreDelayMsChanged: (Int) -> Unit
)

@Immutable
internal data class SettingsCopySectionState(
    val textCopyCapabilityExpanded: Boolean
)

internal data class SettingsCopySectionActions(
    val onTextCopyCapabilityExpandedChanged: (Boolean) -> Unit
)
