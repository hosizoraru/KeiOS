package com.example.keios.core.prefs

import com.tencent.mmkv.MMKV

enum class AppThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

data class UiPrefsSnapshot(
    val liquidBottomBarEnabled: Boolean,
    val liquidActionBarLayeredStyleEnabled: Boolean,
    val cardPressFeedbackEnabled: Boolean,
    val homeIconHdrEnabled: Boolean,
    val superIslandNotificationEnabled: Boolean,
    val superIslandBypassRestrictionEnabled: Boolean,
    val cacheDiagnosticsEnabled: Boolean,
    val appThemeMode: AppThemeMode,
    val visibleBottomPageNames: Set<String>
)

object UiPrefs {
    private const val KV_ID = "ui_prefs"
    private const val KEY_LIQUID_BOTTOM_BAR = "liquid_bottom_bar"
    private const val KEY_LIQUID_ACTION_BAR_LAYERED_STYLE = "liquid_action_bar_layered_style"
    private const val KEY_CARD_PRESS_FEEDBACK = "card_press_feedback"
    private const val KEY_HOME_ICON_HDR = "home_icon_hdr"
    private const val KEY_SUPER_ISLAND_NOTIFICATION = "super_island_notification"
    private const val KEY_SUPER_ISLAND_BYPASS_RESTRICTION = "super_island_bypass_restriction"
    private const val KEY_CACHE_DIAGNOSTICS = "cache_diagnostics"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_VISIBLE_BOTTOM_PAGES = "visible_bottom_pages"
    private val DEFAULT_VISIBLE_BOTTOM_PAGE_NAMES = setOf("Os", "Mcp", "GitHub", "Ba")
    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    private fun kv(): MMKV = store

    fun isLiquidBottomBarEnabled(defaultValue: Boolean = true): Boolean {
        return kv().decodeBool(KEY_LIQUID_BOTTOM_BAR, defaultValue)
    }

    fun setLiquidBottomBarEnabled(value: Boolean) {
        kv().encode(KEY_LIQUID_BOTTOM_BAR, value)
    }

    fun isLiquidActionBarLayeredStyleEnabled(defaultValue: Boolean = true): Boolean {
        return kv().decodeBool(KEY_LIQUID_ACTION_BAR_LAYERED_STYLE, defaultValue)
    }

    fun setLiquidActionBarLayeredStyleEnabled(value: Boolean) {
        kv().encode(KEY_LIQUID_ACTION_BAR_LAYERED_STYLE, value)
    }

    fun isCardPressFeedbackEnabled(defaultValue: Boolean = true): Boolean {
        return kv().decodeBool(KEY_CARD_PRESS_FEEDBACK, defaultValue)
    }

    fun setCardPressFeedbackEnabled(value: Boolean) {
        kv().encode(KEY_CARD_PRESS_FEEDBACK, value)
    }

    fun isHomeIconHdrEnabled(defaultValue: Boolean = false): Boolean {
        return kv().decodeBool(KEY_HOME_ICON_HDR, defaultValue)
    }

    fun setHomeIconHdrEnabled(value: Boolean) {
        kv().encode(KEY_HOME_ICON_HDR, value)
    }

    fun isSuperIslandNotificationEnabled(defaultValue: Boolean = false): Boolean {
        return kv().decodeBool(KEY_SUPER_ISLAND_NOTIFICATION, defaultValue)
    }

    fun setSuperIslandNotificationEnabled(value: Boolean) {
        kv().encode(KEY_SUPER_ISLAND_NOTIFICATION, value)
    }

    fun isSuperIslandBypassRestrictionEnabled(defaultValue: Boolean = false): Boolean {
        return kv().decodeBool(KEY_SUPER_ISLAND_BYPASS_RESTRICTION, defaultValue)
    }

    fun setSuperIslandBypassRestrictionEnabled(value: Boolean) {
        kv().encode(KEY_SUPER_ISLAND_BYPASS_RESTRICTION, value)
    }

    fun isCacheDiagnosticsEnabled(defaultValue: Boolean = true): Boolean {
        return kv().decodeBool(KEY_CACHE_DIAGNOSTICS, defaultValue)
    }

    fun setCacheDiagnosticsEnabled(value: Boolean) {
        kv().encode(KEY_CACHE_DIAGNOSTICS, value)
    }

    fun getAppThemeMode(defaultValue: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM): AppThemeMode {
        val raw = kv().decodeString(KEY_THEME_MODE, null) ?: return defaultValue
        return AppThemeMode.values().firstOrNull { it.name == raw } ?: defaultValue
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        kv().encode(KEY_THEME_MODE, mode.name)
    }

    fun loadVisibleBottomPageNames(): Set<String> {
        val store = kv()
        if (!store.containsKey(KEY_VISIBLE_BOTTOM_PAGES)) return DEFAULT_VISIBLE_BOTTOM_PAGE_NAMES
        val raw = store.decodeString(KEY_VISIBLE_BOTTOM_PAGES, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun saveVisibleBottomPageNames(names: Set<String>) {
        val normalized = names
            .filter { it.isNotBlank() && it != "Home" }
            .joinToString(separator = ",")
        kv().encode(KEY_VISIBLE_BOTTOM_PAGES, normalized)
    }

    fun defaultSnapshot(appThemeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM): UiPrefsSnapshot {
        return UiPrefsSnapshot(
            liquidBottomBarEnabled = true,
            liquidActionBarLayeredStyleEnabled = true,
            cardPressFeedbackEnabled = true,
            homeIconHdrEnabled = false,
            superIslandNotificationEnabled = false,
            superIslandBypassRestrictionEnabled = false,
            cacheDiagnosticsEnabled = true,
            appThemeMode = appThemeMode,
            visibleBottomPageNames = DEFAULT_VISIBLE_BOTTOM_PAGE_NAMES
        )
    }

    fun loadSnapshot(): UiPrefsSnapshot {
        val store = kv()
        return UiPrefsSnapshot(
            liquidBottomBarEnabled = store.decodeBool(KEY_LIQUID_BOTTOM_BAR, true),
            liquidActionBarLayeredStyleEnabled = store.decodeBool(KEY_LIQUID_ACTION_BAR_LAYERED_STYLE, true),
            cardPressFeedbackEnabled = store.decodeBool(KEY_CARD_PRESS_FEEDBACK, true),
            homeIconHdrEnabled = store.decodeBool(KEY_HOME_ICON_HDR, false),
            superIslandNotificationEnabled = store.decodeBool(KEY_SUPER_ISLAND_NOTIFICATION, false),
            superIslandBypassRestrictionEnabled = store.decodeBool(KEY_SUPER_ISLAND_BYPASS_RESTRICTION, false),
            cacheDiagnosticsEnabled = store.decodeBool(KEY_CACHE_DIAGNOSTICS, true),
            appThemeMode = getAppThemeMode(),
            visibleBottomPageNames = loadVisibleBottomPageNames()
        )
    }
}
