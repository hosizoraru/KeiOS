package com.example.keios.ui.page.main.os.shortcut

import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.OsShortcutCardStore
import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

internal object OsActivityShortcutCardStore {
    private const val KV_ID = "os_activity_shortcut_cards"
    private const val LEGACY_KV_ID = "os_ui_state"
    private const val KEY_ACTIVITY_SHORTCUT_CARDS = "activity_shortcut_cards_v1"

    private const val KEY_ID = "id"
    private const val KEY_VISIBLE = "visible"
    private const val KEY_IS_BUILT_IN_SAMPLE = "isBuiltInSample"
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
    private const val EXPORT_SCHEMA_VERSION = "keios.os.activity.cards.v1"
    private const val KEY_EXPORT_SCHEMA = "schema"
    private const val KEY_EXPORT_EXPORTED_AT = "exportedAtMillis"
    private const val KEY_EXPORT_ITEMS = "items"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private val legacyStore: MMKV by lazy { MMKV.mmkvWithID(LEGACY_KV_ID) }

    fun loadCards(
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): List<OsActivityShortcutCard> {
        val persistedRaw = store.decodeString(KEY_ACTIVITY_SHORTCUT_CARDS).orEmpty().trim()
        if (persistedRaw.isNotBlank()) {
            decodeCards(raw = persistedRaw, defaults = defaults).takeIf { it.isNotEmpty() }?.let { decoded ->
                val migrated = migrateBuiltInSampleCards(decoded, builtInSampleDefaults)
                if (migrated != decoded) {
                    saveCards(cards = migrated, defaults = defaults)
                }
                return migrated
            }
        }

        val legacyRaw = legacyStore.decodeString(KEY_ACTIVITY_SHORTCUT_CARDS).orEmpty().trim()
        if (legacyRaw.isNotBlank()) {
            decodeCards(raw = legacyRaw, defaults = defaults).takeIf { it.isNotEmpty() }?.let { decoded ->
                val migrated = migrateBuiltInSampleCards(decoded, builtInSampleDefaults)
                saveCards(cards = migrated, defaults = defaults)
                return migrated
            }
        }

        val legacy = normalizeActivityShortcutConfig(
            OsShortcutCardStore.loadGoogleSystemServiceConfig(defaults),
            defaults
        )
        val defaultLegacy = normalizeActivityShortcutConfig(defaults, defaults)
        val initialCard = if (legacy != defaultLegacy) {
            OsActivityShortcutCard(
                id = LEGACY_GOOGLE_SYSTEM_SERVICE_CARD_ID,
                visible = true,
                isBuiltInSample = false,
                config = legacy
            )
        } else {
            OsActivityShortcutCard(
                id = BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID,
                visible = true,
                isBuiltInSample = true,
                config = normalizeActivityShortcutConfig(builtInSampleDefaults, defaults)
            )
        }
        val migrated = migrateBuiltInSampleCards(
            cards = listOf(initialCard),
            builtInSampleDefaults = builtInSampleDefaults
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
        legacyStore.removeValueForKey(KEY_ACTIVITY_SHORTCUT_CARDS)
        normalized.firstOrNull()?.let { first ->
            OsShortcutCardStore.saveGoogleSystemServiceConfig(first.config, defaults)
        }
    }

    fun buildCardsExportJson(
        cards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val normalized = cards.map { card ->
            card.copy(config = normalizeActivityShortcutConfig(card.config, defaults))
        }
        val items = JSONArray(encodeCards(normalized))
        return JSONObject().apply {
            put(KEY_EXPORT_SCHEMA, EXPORT_SCHEMA_VERSION)
            put(KEY_EXPORT_EXPORTED_AT, exportedAtMillis)
            put(KEY_EXPORT_ITEMS, items)
        }.toString()
    }

    fun importCardsFromJson(
        raw: String,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): List<OsActivityShortcutCard> {
        val normalizedRaw = raw.trim()
        if (normalizedRaw.isBlank()) {
            throw IllegalArgumentException("文件内容为空")
        }
        val importedItems = if (normalizedRaw.startsWith("[")) {
            JSONArray(normalizedRaw)
        } else {
            val root = JSONObject(normalizedRaw)
            root.optJSONArray(KEY_EXPORT_ITEMS)
                ?: throw IllegalArgumentException("未找到可导入的活动 card 数据")
        }
        val decoded = decodeCards(importedItems.toString(), defaults)
        if (decoded.isEmpty()) {
            throw IllegalArgumentException("文件中没有有效的活动 card")
        }
        val migrated = migrateBuiltInSampleCards(decoded, builtInSampleDefaults)
        saveCards(cards = migrated, defaults = defaults)
        return migrated
    }

    private fun encodeCards(cards: List<OsActivityShortcutCard>): String {
        val array = JSONArray()
        cards.forEach { card ->
            val normalizedId = card.id.trim().ifBlank { newOsActivityShortcutCardId() }
            val normalizedConfig = card.config
            val json = JSONObject().apply {
                put(KEY_ID, normalizedId)
                put(KEY_VISIBLE, card.visible)
                put(KEY_IS_BUILT_IN_SAMPLE, card.isBuiltInSample)
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
                            id = item.optString(KEY_ID).trim()
                                .ifBlank { newOsActivityShortcutCardId() },
                            visible = item.optBoolean(KEY_VISIBLE, true),
                            isBuiltInSample = item.optBoolean(KEY_IS_BUILT_IN_SAMPLE, false),
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
                        type = ShortcutIntentExtraType.Companion.fromRaw(
                            item.optString(
                                KEY_EXTRA_TYPE
                            )
                        ),
                        value = item.optString(KEY_EXTRA_VALUE)
                    )
                )
            }
        }.let(::normalizeShortcutIntentExtras)
    }

    private fun migrateBuiltInSampleCards(
        cards: List<OsActivityShortcutCard>,
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): List<OsActivityShortcutCard> {
        if (cards.isEmpty()) return emptyList()
        var sampleMigrated = false
        return cards.map { card ->
            val isGoogleSettingsSample = isGoogleSettingsSampleCard(
                card = card,
                builtInSampleDefaults = builtInSampleDefaults
            )
            if (isGoogleSettingsSample && !sampleMigrated) {
                sampleMigrated = true
                card.copy(
                    id = BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID,
                    isBuiltInSample = true
                )
            } else {
                card.copy(isBuiltInSample = false)
            }
        }
    }

    private fun isGoogleSettingsSampleCard(
        card: OsActivityShortcutCard,
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): Boolean {
        if (card.isBuiltInSample) return true
        if (card.id == BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID) return true
        val targetTitle = builtInSampleDefaults.title.trim()
        if (targetTitle.isBlank()) return false
        return card.config.title.trim() == targetTitle
    }
}
