package os.kei.ui.page.main.os.shortcut

import os.kei.ui.page.main.os.store.OsCardKeyValueStore
import os.kei.ui.page.main.os.store.MmkvOsCardKeyValueStore
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsShortcutCardStore

internal class OsActivityShortcutCardPersistence(
    private val store: OsCardKeyValueStore = MmkvOsCardKeyValueStore(OS_ACTIVITY_SHORTCUT_CARD_KV_ID),
    private val legacyStore: OsCardKeyValueStore = MmkvOsCardKeyValueStore(OS_ACTIVITY_SHORTCUT_CARD_LEGACY_KV_ID),
    private val saveLegacyConfig: (OsGoogleSystemServiceConfig, OsGoogleSystemServiceConfig) -> Unit =
        OsShortcutCardStore::saveGoogleSystemServiceConfig,
) {

    fun loadCards(
        defaults: OsGoogleSystemServiceConfig,
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard>,
    ): List<OsActivityShortcutCard> {
        val persistedRaw = store.decodeString(OS_ACTIVITY_SHORTCUT_CARD_KEY_CARDS).orEmpty().trim()
        if (store.decodeBool(KEY_AUTHORITATIVE_SNAPSHOT, false)) {
            return OsActivityShortcutCardMigration.migrateBuiltInActivityShortcutCards(
                cards = OsActivityShortcutCardCodec.decodeCards(persistedRaw, defaults),
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
                appendMissingBuiltIns = false,
            )
        }
        if (persistedRaw.isNotBlank()) {
            decodeAndPersistMigratedCards(
                raw = persistedRaw,
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
                saveMigrated = true,
                forceSave = false,
            )?.let { return it }
        }

        val legacyRaw = legacyStore.decodeString(OS_ACTIVITY_SHORTCUT_CARD_KEY_CARDS).orEmpty().trim()
        if (legacyRaw.isNotBlank()) {
            decodeAndPersistMigratedCards(
                raw = legacyRaw,
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
                saveMigrated = true,
                forceSave = true,
            )?.let { return it }
        }

        val migrated =
            OsActivityShortcutCardLoadSupport.initialMigratedCards(
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
            )
        saveCards(cards = migrated, defaults = defaults)
        return migrated
    }

    fun saveCards(
        cards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig,
    ) {
        val normalized =
            cards.map { card ->
                card.copy(config = normalizeActivityShortcutConfig(card.config, defaults))
            }
        store.encode(OS_ACTIVITY_SHORTCUT_CARD_KEY_CARDS, OsActivityShortcutCardCodec.encodeCards(normalized))
        legacyStore.removeValueForKey(OS_ACTIVITY_SHORTCUT_CARD_KEY_CARDS)
        normalized.firstOrNull()?.let { first ->
            saveLegacyConfig(first.config, defaults)
        }
    }

    fun replaceFromSync(cards: List<OsActivityShortcutCard>, defaults: OsGoogleSystemServiceConfig) {
        saveCards(cards, defaults)
        store.encode(KEY_AUTHORITATIVE_SNAPSHOT, true)
    }

    private companion object {
        const val KEY_AUTHORITATIVE_SNAPSHOT = "authoritative_sync_snapshot"
    }

    private fun decodeAndPersistMigratedCards(
        raw: String,
        defaults: OsGoogleSystemServiceConfig,
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard>,
        saveMigrated: Boolean,
        forceSave: Boolean,
    ): List<OsActivityShortcutCard>? {
        val persisted =
            OsActivityShortcutCardLoadSupport.loadPersistedCards(
                raw = raw,
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
            ) ?: return null
        val migrated = persisted.migrated
        if (saveMigrated && (forceSave || migrated != persisted.decoded)) {
            saveCards(cards = migrated, defaults = defaults)
        }
        return migrated
    }
}
