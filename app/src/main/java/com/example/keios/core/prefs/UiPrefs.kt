package com.example.keios.core.prefs

import com.tencent.mmkv.MMKV

enum class AppThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

data class UiPrefsSnapshot(
    val liquidBottomBarEnabled: Boolean,
    val cardPressFeedbackEnabled: Boolean,
    val homeIconHdrEnabled: Boolean,
    val cacheDiagnosticsEnabled: Boolean,
    val appThemeMode: AppThemeMode,
    val visibleBottomPageNames: Set<String>
)

object UiPrefs {
    private const val KV_ID = "ui_prefs"
    private const val KEY_LIQUID_BOTTOM_BAR = "liquid_bottom_bar"
    private const val KEY_CARD_PRESS_FEEDBACK = "card_press_feedback"
    private const val KEY_HOME_ICON_HDR = "home_icon_hdr"
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

    fun isCardPressFeedbackEnabled(defaultValue: Boolean = true): Boolean {
        return kv().decodeBool(KEY_CARD_PRESS_FEEDBACK, defaultValue)
    }

    fun setCardPressFeedbackEnabled(value: Boolean) {
        kv().encode(KEY_CARD_PRESS_FEEDBACK, value)
    }

    fun isHomeIconHdrEnabled(defaultValue: Boolean = true): Boolean {
        return kv().decodeBool(KEY_HOME_ICON_HDR, defaultValue)
    }

    fun setHomeIconHdrEnabled(value: Boolean) {
        kv().encode(KEY_HOME_ICON_HDR, value)
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
            cardPressFeedbackEnabled = true,
            homeIconHdrEnabled = true,
            cacheDiagnosticsEnabled = true,
            appThemeMode = appThemeMode,
            visibleBottomPageNames = DEFAULT_VISIBLE_BOTTOM_PAGE_NAMES
        )
    }

    fun loadSnapshot(): UiPrefsSnapshot {
        val store = kv()
        return UiPrefsSnapshot(
            liquidBottomBarEnabled = store.decodeBool(KEY_LIQUID_BOTTOM_BAR, true),
            cardPressFeedbackEnabled = store.decodeBool(KEY_CARD_PRESS_FEEDBACK, true),
            homeIconHdrEnabled = store.decodeBool(KEY_HOME_ICON_HDR, true),
            cacheDiagnosticsEnabled = store.decodeBool(KEY_CACHE_DIAGNOSTICS, true),
            appThemeMode = getAppThemeMode(),
            visibleBottomPageNames = loadVisibleBottomPageNames()
        )
    }
}
