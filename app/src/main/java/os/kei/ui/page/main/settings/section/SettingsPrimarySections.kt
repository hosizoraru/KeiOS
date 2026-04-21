package os.kei.ui.page.main.settings.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.core.prefs.AppThemeMode
import os.kei.ui.page.main.os.appLucideAlertIcon
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.appLucideTimeIcon
import os.kei.ui.page.main.os.osLucideCopyIcon
import os.kei.ui.page.main.settings.support.SettingsActionItem
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.AppDropdownSelector

@Composable
internal fun SettingsVisualSection(
    state: SettingsVisualSectionState,
    actions: SettingsVisualSectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    val visualGroupActive = state.preloadingEnabled || state.homeIconHdrEnabled
    val themeModeOptions = listOf(
        AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.settings_theme_follow_system),
        AppThemeMode.LIGHT to stringResource(R.string.settings_theme_light_mode),
        AppThemeMode.DARK to stringResource(R.string.settings_theme_dark_mode)
    )
    val currentThemeLabel =
        themeModeOptions.firstOrNull { it.first == state.appThemeMode }?.second
            ?: stringResource(R.string.settings_theme_follow_system)
    val themeSummary = when (state.appThemeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_summary_follow_system)
        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_summary_light)
        AppThemeMode.DARK -> stringResource(R.string.settings_theme_summary_dark)
    }
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_visual_header),
        title = stringResource(R.string.settings_group_visual_title),
        sectionIcon = appLucideLayersIcon(),
        containerColor = if (visualGroupActive) enabledCardColor else disabledCardColor
    ) {
        SettingsActionItem(
            title = stringResource(R.string.settings_theme_mode_title),
            summary = themeSummary
        ) {
            AppDropdownSelector(
                selectedText = currentThemeLabel,
                options = themeModeOptions.map { it.second },
                selectedIndex = themeModeOptions.indexOfFirst { it.first == state.appThemeMode }
                    .coerceAtLeast(0),
                expanded = state.showThemeModePopup,
                anchorBounds = state.themePopupAnchorBounds,
                onExpandedChange = actions.onShowThemeModePopupChange,
                onSelectedIndexChange = { selectedIndex ->
                    actions.onAppThemeModeChanged(themeModeOptions[selectedIndex].first)
                },
                onAnchorBoundsChange = actions.onThemePopupAnchorBoundsChange,
                backdrop = null,
                variant = GlassVariant.SheetAction
            )
        }
        SettingsToggleItem(
            title = stringResource(R.string.settings_preloading_title),
            summary = if (state.preloadingEnabled) {
                stringResource(R.string.settings_preloading_summary_enabled)
            } else {
                stringResource(R.string.settings_preloading_summary_disabled)
            },
            checked = state.preloadingEnabled,
            onCheckedChange = actions.onPreloadingEnabledChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_preloading_scope)
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_home_shine_title),
            summary = if (state.homeIconHdrEnabled) {
                stringResource(R.string.settings_home_shine_summary_enabled)
            } else {
                stringResource(R.string.settings_home_shine_summary_disabled)
            },
            checked = state.homeIconHdrEnabled,
            onCheckedChange = actions.onHomeIconHdrChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_home_shine_scope)
        )
    }
}

@Composable
internal fun SettingsAnimationSection(
    state: SettingsAnimationSectionState,
    actions: SettingsAnimationSectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_animation_header),
        title = stringResource(R.string.settings_group_animation_title),
        sectionIcon = appLucideTimeIcon(),
        containerColor = if (state.transitionAnimationsEnabled) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_transition_animations_title),
            summary = if (state.transitionAnimationsEnabled) {
                stringResource(R.string.settings_transition_animations_summary_enabled)
            } else {
                stringResource(R.string.settings_transition_animations_summary_disabled)
            },
            checked = state.transitionAnimationsEnabled,
            onCheckedChange = actions.onTransitionAnimationsChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_transition_animations_scope)
        )
    }
}

@Composable
internal fun SettingsComponentEffectsSection(
    state: SettingsComponentEffectsSectionState,
    actions: SettingsComponentEffectsSectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    val componentEffectsGroupActive = state.liquidActionBarLayeredStyleEnabled ||
        state.liquidBottomBarEnabled ||
        state.liquidGlassSwitchEnabled ||
        state.cardPressFeedbackEnabled
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_component_effects_header),
        title = stringResource(R.string.settings_group_component_effects_title),
        sectionIcon = appLucideConfigIcon(),
        containerColor = if (componentEffectsGroupActive) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_actionbar_style_title),
            summary = if (state.liquidActionBarLayeredStyleEnabled) {
                stringResource(R.string.settings_actionbar_style_summary_enabled)
            } else {
                stringResource(R.string.settings_actionbar_style_summary_disabled)
            },
            checked = state.liquidActionBarLayeredStyleEnabled,
            onCheckedChange = actions.onLiquidActionBarLayeredStyleChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_actionbar_style_scope)
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_bottom_bar_title),
            summary = stringResource(R.string.settings_bottom_bar_summary),
            checked = state.liquidBottomBarEnabled,
            onCheckedChange = actions.onLiquidBottomBarChanged
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_liquid_switch_title),
            summary = if (state.liquidGlassSwitchEnabled) {
                stringResource(R.string.settings_liquid_switch_summary_enabled)
            } else {
                stringResource(R.string.settings_liquid_switch_summary_disabled)
            },
            checked = state.liquidGlassSwitchEnabled,
            onCheckedChange = actions.onLiquidGlassSwitchChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_liquid_switch_scope)
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_card_feedback_title),
            summary = if (state.cardPressFeedbackEnabled) {
                stringResource(R.string.settings_card_feedback_summary_enabled)
            } else {
                stringResource(R.string.settings_card_feedback_summary_disabled)
            },
            checked = state.cardPressFeedbackEnabled,
            onCheckedChange = actions.onCardPressFeedbackChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_card_feedback_scope)
        )
    }
}

@Composable
internal fun SettingsNotifySection(
    state: SettingsNotifySectionState,
    actions: SettingsNotifySectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    val notifyGroupActive = state.superIslandNotificationEnabled ||
        state.superIslandBypassRestrictionEnabled ||
        state.ignoringBatteryOptimizations
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_notify_header),
        title = stringResource(R.string.settings_group_notify_title),
        sectionIcon = appLucideAlertIcon(),
        containerColor = if (notifyGroupActive) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_super_island_style_title),
            summary = if (state.superIslandNotificationEnabled) {
                stringResource(R.string.settings_super_island_style_summary_enabled)
            } else {
                stringResource(R.string.settings_super_island_style_summary_disabled)
            },
            checked = state.superIslandNotificationEnabled,
            onCheckedChange = actions.onSuperIslandNotificationChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_super_island_style_scope)
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_super_island_bypass_title),
            summary = if (state.superIslandBypassRestrictionEnabled) {
                stringResource(R.string.settings_super_island_bypass_summary_enabled)
            } else {
                stringResource(R.string.settings_super_island_bypass_summary_disabled)
            },
            checked = state.superIslandBypassRestrictionEnabled,
            onCheckedChange = actions.onSuperIslandBypassRestrictionChanged,
            infoKey = stringResource(R.string.common_note),
            infoValue = stringResource(R.string.settings_super_island_bypass_note)
        )
        SettingsActionItem(
            title = stringResource(R.string.settings_battery_optimization_title),
            summary = if (state.ignoringBatteryOptimizations) {
                stringResource(R.string.settings_battery_optimization_summary_ignored)
            } else {
                stringResource(R.string.settings_battery_optimization_summary_restricted)
            },
            infoKey = stringResource(R.string.settings_battery_optimization_info_status),
            infoValue = if (state.ignoringBatteryOptimizations) {
                stringResource(R.string.settings_battery_optimization_status_ignored)
            } else {
                stringResource(R.string.settings_battery_optimization_status_restricted)
            },
            trailing = {
                GlassTextButton(
                    backdrop = null,
                    variant = GlassVariant.Compact,
                    text = if (state.ignoringBatteryOptimizations) {
                        stringResource(R.string.common_open)
                    } else {
                        stringResource(R.string.settings_battery_optimization_action_request)
                    },
                    enabled = state.batteryOptimizationActionAvailable,
                    onClick = actions.onOpenBatteryOptimizationSettings,
                    pressOverlayEnabled = true
                )
            }
        )
    }
}

@Composable
internal fun SettingsCopySection(
    state: SettingsCopySectionState,
    actions: SettingsCopySectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_copy_header),
        title = stringResource(R.string.settings_group_copy_title),
        sectionIcon = osLucideCopyIcon(),
        containerColor = if (state.textCopyCapabilityExpanded) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_copy_capability_title),
            summary = if (state.textCopyCapabilityExpanded) {
                stringResource(R.string.settings_copy_capability_summary_enabled)
            } else {
                stringResource(R.string.settings_copy_capability_summary_disabled)
            },
            checked = state.textCopyCapabilityExpanded,
            onCheckedChange = actions.onTextCopyCapabilityExpandedChanged,
            infoKey = stringResource(R.string.common_note),
            infoValue = if (state.textCopyCapabilityExpanded) {
                stringResource(R.string.settings_copy_capability_note_enabled)
            } else {
                stringResource(R.string.settings_copy_capability_note_disabled)
            }
        )
    }
}
