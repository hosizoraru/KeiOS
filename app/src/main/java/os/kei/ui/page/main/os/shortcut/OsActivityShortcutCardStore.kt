package os.kei.ui.page.main.os.shortcut

import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsShortcutCardStore
import os.kei.ui.page.main.os.transfer.OS_ACTIVITY_CARD_EXPORT_SCHEMA
import os.kei.ui.page.main.os.transfer.OsActivityCardImportPayload
import os.kei.ui.page.main.os.transfer.OsCardImportError
import os.kei.ui.page.main.os.transfer.OsCardImportException
import os.kei.ui.page.main.os.transfer.OsCardImportRoot
import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

internal data class OsActivityCardImportMergeResult(
    val cards: List<OsActivityShortcutCard>,
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int
)

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
    private const val KEY_EXPORT_SCHEMA = "schema"
    private const val KEY_EXPORT_EXPORTED_AT = "exportedAtMillis"
    private const val KEY_EXPORT_ITEM_COUNT = "itemCount"
    private const val KEY_EXPORT_ITEMS = "items"
    private const val LEGACY_GOOGLE_SETTINGS_ACTIVITY_CLASS =
        "com.google.android.gms.app.settings.GoogleSettingsActivity"

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
            put(KEY_EXPORT_SCHEMA, OS_ACTIVITY_CARD_EXPORT_SCHEMA)
            put(KEY_EXPORT_EXPORTED_AT, exportedAtMillis)
            put(KEY_EXPORT_ITEM_COUNT, normalized.size)
            put(KEY_EXPORT_ITEMS, items)
        }.toString(2)
    }

    fun parseCardsImport(
        root: OsCardImportRoot,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): OsActivityCardImportPayload {
        if (root.items.length() == 0) {
            return OsActivityCardImportPayload(
                cards = emptyList(),
                sourceCount = root.sourceCount,
                invalidCount = 0,
                duplicateCount = 0,
                fileKind = root.fileKind,
                isLegacyFormat = root.isLegacyFormat
            )
        }
        val decoded = buildList {
            for (index in 0 until root.items.length()) {
                val item = root.items.optJSONObject(index) ?: continue
                decodeCard(item, defaults)?.let(::add)
            }
        }
        val migrated = migrateBuiltInSampleCards(
            cards = decoded,
            builtInSampleDefaults = builtInSampleDefaults
        )
        val deduplicated = mutableListOf<OsActivityShortcutCard>()
        var duplicateCount = 0
        migrated.forEach { imported ->
            val targetIndexById = deduplicated.indexOfFirst { it.id == imported.id }
            val targetIndex = if (targetIndexById >= 0) {
                targetIndexById
            } else {
                deduplicated.indexOfFirst { mergeKeyFor(it) == mergeKeyFor(imported) }
            }
            if (targetIndex < 0) {
                deduplicated += imported
            } else {
                val existing = deduplicated[targetIndex]
                deduplicated[targetIndex] = imported.copy(
                    id = existing.id,
                    isBuiltInSample = existing.isBuiltInSample || imported.isBuiltInSample
                )
                duplicateCount += 1
            }
        }
        return OsActivityCardImportPayload(
            cards = deduplicated,
            sourceCount = root.sourceCount,
            invalidCount = (root.sourceCount - decoded.size).coerceAtLeast(0),
            duplicateCount = duplicateCount,
            fileKind = root.fileKind,
            isLegacyFormat = root.isLegacyFormat
        )
    }

    fun previewImportedCards(
        payload: OsActivityCardImportPayload,
        existingCards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): OsActivityCardImportMergeResult {
        return mergeImportedCards(
            existingCards = existingCards,
            importedCards = payload.cards,
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults
        )
    }

    fun applyImportedCards(
        payload: OsActivityCardImportPayload,
        existingCards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): OsActivityCardImportMergeResult {
        val result = mergeImportedCards(
            existingCards = existingCards,
            importedCards = payload.cards,
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults
        )
        saveCards(cards = result.cards, defaults = defaults)
        return result
    }

    fun importCardsFromJsonMerged(
        raw: String,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): OsActivityCardImportMergeResult {
        val payload = parseCardsImport(
            root = os.kei.ui.page.main.os.transfer.parseOsCardImportRoot(raw),
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults
        )
        if (payload.cards.isEmpty()) {
            throw OsCardImportException(OsCardImportError.NoValidActivityCards)
        }
        return applyImportedCards(
            payload = payload,
            existingCards = loadCards(
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults
            ),
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults
        )
    }

    private fun mergeKeyFor(card: OsActivityShortcutCard): String {
        val config = card.config
        return listOf(
            config.packageName.trim().lowercase(),
            config.className.trim().lowercase(),
            config.intentAction.trim().lowercase(),
            config.intentUriData.trim().lowercase(),
            config.title.trim().lowercase()
        ).joinToString("|")
    }

    private fun cardsEquivalent(
        old: OsActivityShortcutCard,
        new: OsActivityShortcutCard,
        defaults: OsGoogleSystemServiceConfig
    ): Boolean {
        val oldConfig = normalizeActivityShortcutConfig(old.config, defaults)
        val newConfig = normalizeActivityShortcutConfig(new.config, defaults)
        return old.visible == new.visible &&
            old.isBuiltInSample == new.isBuiltInSample &&
            oldConfig == newConfig
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
                    decodeCard(item, defaults)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun decodeCard(
        item: JSONObject,
        defaults: OsGoogleSystemServiceConfig
    ): OsActivityShortcutCard? {
        if (!isActivityCardItem(item)) return null
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
        return OsActivityShortcutCard(
            id = item.optString(KEY_ID).trim()
                .ifBlank { newOsActivityShortcutCardId() },
            visible = item.optBoolean(KEY_VISIBLE, true),
            isBuiltInSample = item.optBoolean(KEY_IS_BUILT_IN_SAMPLE, false),
            config = normalizeActivityShortcutConfig(config, defaults)
        )
    }

    private fun isActivityCardItem(item: JSONObject): Boolean {
        return item.has(KEY_APP_NAME) ||
            item.has(KEY_PACKAGE_NAME) ||
            item.has(KEY_CLASS_NAME) ||
            item.has(KEY_INTENT_ACTION) ||
            item.has(KEY_INTENT_CATEGORY) ||
            item.has(KEY_INTENT_FLAGS) ||
            item.has(KEY_INTENT_URI_DATA) ||
            item.has(KEY_INTENT_MIME_TYPE) ||
            item.has(KEY_INTENT_EXTRAS)
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
                val upgradedClassName = when {
                    card.config.className.trim()
                        .equals(LEGACY_GOOGLE_SETTINGS_ACTIVITY_CLASS, ignoreCase = true) ->
                        builtInSampleDefaults.className
                    else -> card.config.className.trim().ifBlank { builtInSampleDefaults.className }
                }
                val upgradedConfig = normalizeActivityShortcutConfig(
                    config = card.config.copy(
                        title = builtInSampleDefaults.title,
                        subtitle = builtInSampleDefaults.subtitle,
                        appName = builtInSampleDefaults.appName,
                        packageName = card.config.packageName.trim()
                            .ifBlank { builtInSampleDefaults.packageName },
                        className = upgradedClassName,
                        intentAction = card.config.intentAction.trim()
                            .ifBlank { builtInSampleDefaults.intentAction },
                        intentFlags = card.config.intentFlags.trim()
                            .ifBlank { builtInSampleDefaults.intentFlags }
                    ),
                    defaults = builtInSampleDefaults
                )
                card.copy(
                    id = BUILTIN_GOOGLE_SETTINGS_SAMPLE_CARD_ID,
                    isBuiltInSample = true,
                    config = upgradedConfig
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

    private fun mergeImportedCards(
        existingCards: List<OsActivityShortcutCard>,
        importedCards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig,
        builtInSampleDefaults: OsGoogleSystemServiceConfig
    ): OsActivityCardImportMergeResult {
        val mergedCards = existingCards.toMutableList()
        var addedCount = 0
        var updatedCount = 0
        var unchangedCount = 0
        importedCards.forEach { imported ->
            val targetIndexById = mergedCards.indexOfFirst { it.id == imported.id }
            val targetIndex = if (targetIndexById >= 0) {
                targetIndexById
            } else {
                val importedKey = mergeKeyFor(imported)
                mergedCards.indexOfFirst { mergeKeyFor(it) == importedKey }
            }
            if (targetIndex < 0) {
                mergedCards += imported
                addedCount += 1
                return@forEach
            }
            val existing = mergedCards[targetIndex]
            val resolved = imported.copy(
                id = existing.id,
                isBuiltInSample = existing.isBuiltInSample || imported.isBuiltInSample
            )
            if (cardsEquivalent(existing, resolved, defaults)) {
                unchangedCount += 1
            } else {
                mergedCards[targetIndex] = resolved
                updatedCount += 1
            }
        }
        return OsActivityCardImportMergeResult(
            cards = migrateBuiltInSampleCards(
                cards = mergedCards,
                builtInSampleDefaults = builtInSampleDefaults
            ),
            addedCount = addedCount,
            updatedCount = updatedCount,
            unchangedCount = unchangedCount
        )
    }
}
