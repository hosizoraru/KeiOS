package com.example.keios.ui.utils

import com.tencent.mmkv.MMKV

object UiPrefs {
    private const val KV_ID = "ui_prefs"
    private const val KEY_LIQUID_BOTTOM_BAR = "liquid_bottom_bar"
    private const val KEY_CARD_PRESS_FEEDBACK = "card_press_feedback"

    private fun kv(): MMKV = MMKV.mmkvWithID(KV_ID)

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
}
