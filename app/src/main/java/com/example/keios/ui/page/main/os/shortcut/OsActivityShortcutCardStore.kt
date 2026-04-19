package com.example.keios.ui.page.main

import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

internal object OsActivityShortcutCardStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_ACTIVITY_SHORTCUT_CARDS = "activity_shortcut_cards_v1"

    private const val KEY_ID = "id"
    private const val KEY_VISIBLE = "visible"
    private const val KEY_TITLE = "title"
    private const val KEY_SUBTITLE = "subtitle"
    private const val KEY_APP_NAME = "appName"
    private const val KEY_PACKAGE_NAME = "packageName"
    private const val KEY_CLASS_NAME = "className"
    private const val KEY_INTENT_ACTION = "intentAction"
    private const val KEY_INTENT_CATEGORY = "intentCategory"
    private const val KEY_INTENT_FLAGS = "intentFlags"
    private const val KEY_INTENT_URI_DATA = "intentUriData"
    private const val KEY_INTENT_MIME_TYPE = "intentMimeType"
    private const val KEY_INTENT_EXTRAS = "intentExtras"

    private const val KEY_EXTRA_KEY = "key"
    private const val KEY_EXTRA_TYPE = "type"
    private const val KEY_EXTRA_VALUE = "value"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadCards(
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig()
    ): List<OsActivityShortcutCard> {
        val persistedRaw = store.decodeString(KEY_ACTIVITY_SHORTCUT_CARDS).orEmpty().trim()
        if (persistedRaw.isNotBlank()) {
            decodeCards(raw = persistedRaw, defaults = defaults).takeIf { it.isNotEmpty() }?.let {
                return it
            }
        }

        val legacy = OsShortcutCardStore.loadGoogleSystemServiceConfig(defaults)
        val migrated = listOf(
            OsActivityShortcutCard(
                id = LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID,
                visible = true,
                config = normalizeActivityShortcutConfig(legacy, defaults)
            )
        )
        saveCards(cards = migrated, defaults = defaults)
        return migrated
    }

    fun saveCards(
        cards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig()
    ) {
        val normalized = cards.map { card ->
            card.copy(config = normalizeActivityShortcutConfig(card.config, defaults))
        }
        store.encode(KEY_ACTIVITY_SHORTCUT_CARDS, encodeCards(normalized))
        normalized.firstOrNull()?.let { first ->
            OsShortcutCardStore.saveGoogleSystemServiceConfig(first.config, defaults)
        }
    }

    private fun encodeCards(cards: List<OsActivityShortcutCard>): String {
        val array = JSONArray()
        cards.forEach { card ->
            val normalizedId = card.id.trim().ifBlank { newOsActivityShortcutCardId() }
            val normalizedConfig = card.config
            val json = JSONObject().apply {
                put(KEY_ID, normalizedId)
                put(KEY_VISIBLE, card.visible)
                put(KEY_TITLE, normalizedConfig.title)
                put(KEY_SUBTITLE, normalizedConfig.subtitle)
                put(KEY_APP_NAME, normalizedConfig.appName)
                put(KEY_PACKAGE_NAME, normalizedConfig.packageName)
                put(KEY_CLASS_NAME, normalizedConfig.className)
                put(KEY_INTENT_ACTION, normalizedConfig.intentAction)
                put(KEY_INTENT_CATEGORY, normalizedConfig.intentCategory)
                put(KEY_INTENT_FLAGS, normalizedConfig.intentFlags)
                put(KEY_INTENT_URI_DATA, normalizedConfig.intentUriData)
                put(KEY_INTENT_MIME_TYPE, normalizedConfig.intentMimeType)
                put(KEY_INTENT_EXTRAS, encodeIntentExtras(normalizedConfig.intentExtras))
            }
            array.put(json)
        }
        return array.toString()
    }

    private fun decodeCards(
        raw: String,
        defaults: OsGoogleSystemServiceConfig
    ): List<OsActivityShortcutCard> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val config = OsGoogleSystemServiceConfig(
                        title = item.optString(KEY_TITLE),
                        subtitle = item.optString(KEY_SUBTITLE),
                        appName = item.optString(KEY_APP_NAME),
                        packageName = item.optString(KEY_PACKAGE_NAME),
                        className = item.optString(KEY_CLASS_NAME),
                        intentAction = item.optString(KEY_INTENT_ACTION),
                        intentCategory = item.optString(KEY_INTENT_CATEGORY),
                        intentFlags = item.optString(KEY_INTENT_FLAGS),
                        intentUriData = item.optString(KEY_INTENT_URI_DATA),
                        intentMimeType = item.optString(KEY_INTENT_MIME_TYPE),
                        intentExtras = decodeIntentExtras(item.optJSONArray(KEY_INTENT_EXTRAS))
                    )
                    add(
                        OsActivityShortcutCard(
                            id = item.optString(KEY_ID).trim().ifBlank { newOsActivityShortcutCardId() },
                            visible = item.optBoolean(KEY_VISIBLE, true),
                            config = normalizeActivityShortcutConfig(config, defaults)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeIntentExtras(extras: List<ShortcutIntentExtra>): JSONArray {
        val normalized = normalizeShortcutIntentExtras(extras)
        val array = JSONArray()
        normalized.forEach { extra ->
            array.put(
                JSONObject().apply {
                    put(KEY_EXTRA_KEY, extra.key)
                    put(KEY_EXTRA_TYPE, extra.type.rawValue)
                    put(KEY_EXTRA_VALUE, extra.value)
                }
            )
        }
        return array
    }

    private fun decodeIntentExtras(raw: JSONArray?): List<ShortcutIntentExtra> {
        if (raw == null) return emptyList()
        return buildList {
            for (index in 0 until raw.length()) {
                val item = raw.optJSONObject(index) ?: continue
                add(
                    ShortcutIntentExtra(
                        key = item.optString(KEY_EXTRA_KEY),
                        type = ShortcutIntentExtraType.fromRaw(item.optString(KEY_EXTRA_TYPE)),
                        value = item.optString(KEY_EXTRA_VALUE)
                    )
                )
            }
        }.let(::normalizeShortcutIntentExtras)
    }
}
