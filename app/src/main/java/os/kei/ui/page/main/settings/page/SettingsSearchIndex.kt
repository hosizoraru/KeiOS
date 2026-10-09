package os.kei.ui.page.main.settings.page

import androidx.compose.runtime.Immutable
import os.kei.R
import java.util.Locale

internal enum class SettingsSearchCard {
    Permissions,
    KeepAlive,
    AccessibilityGuardPolicy,
    AccessibilityGuardHistory,
    ThemeLanguage,
    Performance,
    HomeEffects,
    PageMotion,
    LiquidControls,
    Interaction,
    BackgroundAsset,
    BackgroundLayout,
    BackgroundRendering,
    Notify,
    Copy,
    WebDavSync,
    CacheDiagnostics,
    CacheItems,
    LogLevel,
    LogFiles,
}

@Immutable
internal data class SettingsSearchTarget(
    val card: SettingsSearchCard,
    val category: SettingsCategory,
    private val tokens: List<String>,
) {
    fun matches(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isBlank()) return true
        return tokens.any { token ->
            token.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
    }
}

internal fun buildSettingsSearchTargets(resolveString: (Int) -> String): List<SettingsSearchTarget> {
    val accessCategoryLabel = resolveString(R.string.settings_category_access)
    val permissionsGroupLabel = resolveString(R.string.settings_group_permissions_title)
    val keepAliveGroupLabel = resolveString(R.string.settings_group_keep_alive_title)
    val notificationPermissionLabel = resolveString(R.string.settings_notification_permission_title)
    val androidBackgroundLabel = resolveString(R.string.settings_android_background_title)
    val backgroundRecoveryLabel = resolveString(R.string.settings_background_recovery_title)
    val batteryOptimizationLabel = resolveString(R.string.settings_battery_optimization_title)
    val oemAutostartLabel = resolveString(R.string.settings_oem_autostart_title)
    val appListAccessLabel = resolveString(R.string.settings_app_list_access_title)
    val shizukuPermissionLabel = resolveString(R.string.settings_shizuku_permission_title)
    val rootPermissionLabel = resolveString(R.string.settings_root_permission_title)
    val privilegedModeLabel = resolveString(R.string.settings_privileged_mode_title)
    val appStandbyActiveLabel = resolveString(R.string.settings_app_standby_bucket_active)
    val appStandbyRareLabel = resolveString(R.string.settings_app_standby_bucket_rare)
    val appStandbyRestrictedLabel = resolveString(R.string.settings_app_standby_bucket_restricted)
    val accessibilityGuardHeaderLabel = resolveString(R.string.settings_accessibility_guard_header)
    val accessibilityGuardPolicyLabel = resolveString(R.string.settings_accessibility_guard_policy_title)
    val accessibilityGuardCapabilityLabel = resolveString(R.string.settings_accessibility_guard_capability_title)
    val accessibilityGuardDaemonLabel = resolveString(R.string.settings_accessibility_guard_daemon_title)
    val accessibilityGuardBootCheckLabel = resolveString(R.string.settings_accessibility_guard_boot_check_title)
    val accessibilityGuardScreenOnLabel = resolveString(R.string.settings_accessibility_guard_screen_on_title)
    val accessibilityGuardDisclosureLabel = resolveString(R.string.settings_accessibility_guard_disclosure_title)
    val accessibilityGuardHistoryLabel = resolveString(R.string.settings_accessibility_guard_history_title)

    val keepAliveCategoryLabel = resolveString(R.string.settings_category_keep_alive)
    val interfaceCategoryLabel = resolveString(R.string.settings_category_interface)
    val legacyAppearanceCategoryLabel = resolveString(R.string.settings_category_appearance)
    val legacyEffectsCategoryLabel = resolveString(R.string.settings_category_effects)
    val themeLanguageGroupLabel = resolveString(R.string.settings_group_theme_language_title)
    val performanceGroupLabel = resolveString(R.string.settings_group_performance_title)
    val homeEffectsGroupLabel = resolveString(R.string.settings_group_home_effects_title)
    val themeModeLabel = resolveString(R.string.settings_theme_mode_title)
    val launcherIconDesignLabel = resolveString(R.string.settings_launcher_icon_design_title)
    val launcherIconAppleLabel = resolveString(R.string.settings_launcher_icon_design_apple)
    val launcherIconAndroidLabel = resolveString(R.string.settings_launcher_icon_design_android)
    val appLanguageLabel = resolveString(R.string.settings_app_language_title)
    val preloadingLabel = resolveString(R.string.settings_preloading_title)
    val homeShineLabel = resolveString(R.string.settings_home_shine_title)
    val homeDynamicFullEffectLabel = resolveString(R.string.settings_home_dynamic_full_effect_title)

    val pageMotionGroupLabel = resolveString(R.string.settings_group_page_motion_title)
    val transitionAnimationsLabel = resolveString(R.string.settings_transition_animations_title)
    val predictiveBackAnimationsLabel = resolveString(R.string.settings_predictive_back_animations_title)

    val liquidControlsGroupLabel = resolveString(R.string.settings_group_liquid_controls_title)
    val interactionGroupLabel = resolveString(R.string.settings_group_interaction_title)
    val liquidSwitchLabel = resolveString(R.string.settings_liquid_switch_title)
    val liquidToastLabel = resolveString(R.string.settings_liquid_toast_title)
    val searchAutoFocusLabel = resolveString(R.string.settings_search_auto_focus_title)
    val gripAwareFloatingDockLabel = resolveString(R.string.settings_grip_aware_floating_dock_title)

    val backgroundAssetGroupLabel = resolveString(R.string.settings_group_background_asset_title)
    val backgroundLayoutGroupLabel = resolveString(R.string.settings_group_background_layout_title)
    val backgroundRenderingGroupLabel = resolveString(R.string.settings_group_background_rendering_title)
    val nonHomeBackgroundLabel = resolveString(R.string.settings_non_home_background_title)
    val nonHomeBackgroundImageLabel = resolveString(R.string.settings_non_home_background_image_title)
    val nonHomeBackgroundScaleLabel = resolveString(R.string.settings_non_home_background_scale_title)
    val nonHomeBackgroundScaleCropLabel = resolveString(R.string.settings_non_home_background_scale_crop)
    val nonHomeBackgroundScaleFitLabel = resolveString(R.string.settings_non_home_background_scale_fit)
    val nonHomeBackgroundScaleFillBoundsLabel = resolveString(R.string.settings_non_home_background_scale_fill_bounds)
    val nonHomeBackgroundAlignmentLabel = resolveString(R.string.settings_non_home_background_alignment_title)
    val nonHomeBackgroundAlignmentTopLabel = resolveString(R.string.settings_non_home_background_alignment_top)
    val nonHomeBackgroundAlignmentCenterLabel = resolveString(R.string.settings_non_home_background_alignment_center)
    val nonHomeBackgroundAlignmentBottomLabel = resolveString(R.string.settings_non_home_background_alignment_bottom)
    val nonHomeBackgroundStyleLabel = resolveString(R.string.settings_non_home_background_style_title)
    val nonHomeBackgroundStyleReadableLabel = resolveString(R.string.settings_non_home_background_style_readable)
    val nonHomeBackgroundStyleSoftLabel = resolveString(R.string.settings_non_home_background_style_soft)
    val nonHomeBackgroundStyleFocusedLabel = resolveString(R.string.settings_non_home_background_style_focused)
    val nonHomeBackgroundDepthLabel = resolveString(R.string.settings_non_home_background_depth_title)
    val nonHomeBackgroundOpacityLabel = resolveString(R.string.settings_non_home_background_opacity_title)
    val nonHomeBackgroundSaturationLabel = resolveString(R.string.settings_non_home_background_saturation_title)
    val nonHomeBackgroundScrimLabel = resolveString(R.string.settings_non_home_background_scrim_title)
    val nonHomeBackgroundCropLabel = resolveString(R.string.settings_non_home_background_crop_title)
    val nonHomeBackgroundPreviewLabel = resolveString(R.string.settings_non_home_background_action_preview)
    val nonHomeBackgroundSuggestLabel = resolveString(R.string.settings_non_home_background_action_suggest)

    val notifyGroupLabel = resolveString(R.string.settings_group_notify_title)
    val superIslandStyleLabel = resolveString(R.string.settings_super_island_style_title)
    val superIslandFloatBehaviorLabel = resolveString(R.string.settings_super_island_float_behavior_title)
    val superIslandAutoCloseLabel = resolveString(R.string.settings_super_island_auto_close_title)
    val superIslandBypassLabel = resolveString(R.string.settings_super_island_bypass_title)
    val superIslandRestoreDelayLabel = resolveString(R.string.settings_super_island_restore_delay_title)

    val dataCategoryLabel = resolveString(R.string.settings_category_data)
    val copyGroupLabel = resolveString(R.string.settings_group_copy_title)
    val copyCapabilityLabel = resolveString(R.string.settings_copy_capability_title)

    val cacheDiagnosticsLabel = resolveString(R.string.settings_cache_diagnostics_title)
    val cacheItemsLabel = resolveString(R.string.settings_cache_items_title)
    val cacheEntryOverviewLabel = resolveString(R.string.settings_cache_entry_overview_title)
    val cacheEntryGithubLabel = resolveString(R.string.settings_cache_entry_github_title)
    val cacheEntryBaPageLabel = resolveString(R.string.settings_cache_entry_ba_page_title)
    val cacheEntryBaGuideLabel = resolveString(R.string.settings_cache_entry_ba_guide_title)
    val cacheEntryOsLabel = resolveString(R.string.settings_cache_entry_os_title)
    val cacheEntryMcpLabel = resolveString(R.string.settings_cache_entry_mcp_title)

    val logLevelGroupLabel = resolveString(R.string.settings_group_log_level_title)
    val logFilesGroupLabel = resolveString(R.string.settings_group_log_files_title)
    val logLevelLabel = resolveString(R.string.settings_log_level_title)
    val logFeedbackActionLabel = resolveString(R.string.settings_log_feedback_action)

    val webdavSyncLabel = resolveString(R.string.webdav_sync_title)

    return listOf(
        SettingsSearchTarget(
            card = SettingsSearchCard.Permissions,
            category = SettingsCategory.Access,
            tokens =
                settingsTokens(
                    accessCategoryLabel,
                    permissionsGroupLabel,
                    notificationPermissionLabel,
                    appListAccessLabel,
                    shizukuPermissionLabel,
                    rootPermissionLabel,
                    privilegedModeLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.KeepAlive,
            category = SettingsCategory.KeepAlive,
            tokens =
                settingsTokens(
                    keepAliveCategoryLabel,
                    keepAliveGroupLabel,
                    androidBackgroundLabel,
                    backgroundRecoveryLabel,
                    batteryOptimizationLabel,
                    oemAutostartLabel,
                    appStandbyActiveLabel,
                    appStandbyRareLabel,
                    appStandbyRestrictedLabel,
                    "Doze",
                    "standby",
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.AccessibilityGuardPolicy,
            category = SettingsCategory.KeepAlive,
            tokens =
                settingsTokens(
                    keepAliveCategoryLabel,
                    accessibilityGuardHeaderLabel,
                    accessibilityGuardPolicyLabel,
                    accessibilityGuardCapabilityLabel,
                    accessibilityGuardDaemonLabel,
                    accessibilityGuardBootCheckLabel,
                    accessibilityGuardScreenOnLabel,
                    accessibilityGuardDisclosureLabel,
                    shizukuPermissionLabel,
                    rootPermissionLabel,
                    "Shizuku",
                    "Root",
                    "privileged",
                    "secure settings",
                    "accessibility",
                    "daemon",
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.AccessibilityGuardHistory,
            category = SettingsCategory.KeepAlive,
            tokens =
                settingsTokens(
                    keepAliveCategoryLabel,
                    accessibilityGuardHeaderLabel,
                    accessibilityGuardHistoryLabel,
                    "restore history",
                    "guard history",
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.ThemeLanguage,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyAppearanceCategoryLabel,
                    themeLanguageGroupLabel,
                    themeModeLabel,
                    launcherIconDesignLabel,
                    launcherIconAppleLabel,
                    launcherIconAndroidLabel,
                    appLanguageLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.Performance,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyAppearanceCategoryLabel,
                    performanceGroupLabel,
                    preloadingLabel,
                    resolveString(R.string.settings_lobby_rendering),
                    resolveString(R.string.settings_model_rendering),
                    resolveString(R.string.settings_viewer_rendering_compatible),
                    "Spine", "3D", "WebGL", "OpenGL", "Vulkan",
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.HomeEffects,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyAppearanceCategoryLabel,
                    legacyEffectsCategoryLabel,
                    homeEffectsGroupLabel,
                    homeShineLabel,
                    homeDynamicFullEffectLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.PageMotion,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyEffectsCategoryLabel,
                    pageMotionGroupLabel,
                    transitionAnimationsLabel,
                    predictiveBackAnimationsLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.LiquidControls,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyEffectsCategoryLabel,
                    liquidControlsGroupLabel,
                    liquidSwitchLabel,
                    liquidToastLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.Interaction,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyEffectsCategoryLabel,
                    interactionGroupLabel,
                    searchAutoFocusLabel,
                    gripAwareFloatingDockLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.BackgroundAsset,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyAppearanceCategoryLabel,
                    backgroundAssetGroupLabel,
                    nonHomeBackgroundLabel,
                    nonHomeBackgroundImageLabel,
                    nonHomeBackgroundPreviewLabel,
                    nonHomeBackgroundSuggestLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.BackgroundLayout,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyAppearanceCategoryLabel,
                    backgroundLayoutGroupLabel,
                    nonHomeBackgroundScaleLabel,
                    nonHomeBackgroundScaleCropLabel,
                    nonHomeBackgroundScaleFitLabel,
                    nonHomeBackgroundScaleFillBoundsLabel,
                    nonHomeBackgroundAlignmentLabel,
                    nonHomeBackgroundAlignmentTopLabel,
                    nonHomeBackgroundAlignmentCenterLabel,
                    nonHomeBackgroundAlignmentBottomLabel,
                    nonHomeBackgroundStyleLabel,
                    nonHomeBackgroundStyleReadableLabel,
                    nonHomeBackgroundStyleSoftLabel,
                    nonHomeBackgroundStyleFocusedLabel,
                    nonHomeBackgroundDepthLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.BackgroundRendering,
            category = SettingsCategory.Interface,
            tokens =
                settingsTokens(
                    interfaceCategoryLabel,
                    legacyAppearanceCategoryLabel,
                    backgroundRenderingGroupLabel,
                    nonHomeBackgroundOpacityLabel,
                    nonHomeBackgroundSaturationLabel,
                    nonHomeBackgroundScrimLabel,
                    nonHomeBackgroundCropLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.Notify,
            category = SettingsCategory.Access,
            tokens =
                settingsTokens(
                    accessCategoryLabel,
                    notifyGroupLabel,
                    superIslandStyleLabel,
                    superIslandFloatBehaviorLabel,
                    superIslandAutoCloseLabel,
                    superIslandBypassLabel,
                    superIslandRestoreDelayLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.Copy,
            category = SettingsCategory.Data,
            tokens =
                settingsTokens(
                    dataCategoryLabel,
                    copyGroupLabel,
                    copyCapabilityLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.CacheDiagnostics,
            category = SettingsCategory.Data,
            tokens =
                settingsTokens(
                    dataCategoryLabel,
                    cacheDiagnosticsLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.CacheItems,
            category = SettingsCategory.Data,
            tokens =
                settingsTokens(
                    dataCategoryLabel,
                    cacheItemsLabel,
                    cacheEntryOverviewLabel,
                    cacheEntryGithubLabel,
                    cacheEntryBaPageLabel,
                    cacheEntryBaGuideLabel,
                    cacheEntryOsLabel,
                    cacheEntryMcpLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.LogLevel,
            category = SettingsCategory.Data,
            tokens =
                settingsTokens(
                    dataCategoryLabel,
                    logLevelGroupLabel,
                    logLevelLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.LogFiles,
            category = SettingsCategory.Data,
            tokens =
                settingsTokens(
                    dataCategoryLabel,
                    logFilesGroupLabel,
                    logFeedbackActionLabel,
                ),
        ),
        SettingsSearchTarget(
            card = SettingsSearchCard.WebDavSync,
            category = SettingsCategory.Data,
            tokens =
                settingsTokens(
                    dataCategoryLabel,
                    webdavSyncLabel,
                ),
        ),
    )
}

private fun settingsTokens(vararg values: String): List<String> = values.filter { it.isNotBlank() }

internal fun deriveSettingsSearchTargets(
    targets: List<SettingsSearchTarget>,
    query: String,
): List<SettingsSearchTarget> =
    if (query.isBlank()) {
        emptyList()
    } else {
        targets.filter { it.matches(query) }
    }
