package os.kei.ui.page.main.os.shortcut

import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.transfer.OsActivityCardImportPayload
import os.kei.ui.page.main.os.transfer.OsCardImportError
import os.kei.ui.page.main.os.transfer.OsCardImportException
import os.kei.ui.page.main.os.transfer.OsCardImportRoot
import os.kei.ui.page.main.os.transfer.parseOsCardImportRoot

internal object OsActivityShortcutCardStore {
    private val persistence = OsActivityShortcutCardPersistence()

    fun replaceFromSync(cards: List<OsActivityShortcutCard>, defaults: OsGoogleSystemServiceConfig) =
        persistence.replaceFromSync(cards, defaults)

    fun loadCards(
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard> =
            defaultBuiltInActivityShortcutCards(builtInSampleDefaults),
    ): List<OsActivityShortcutCard> =
        persistence.loadCards(
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
        )

    fun saveCards(
        cards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
    ) {
        persistence.saveCards(cards = cards, defaults = defaults)
    }

    fun buildCardsExportJson(
        cards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        exportedAtMillis: Long = OsSystemActivityShortcutCardClock.nowMs(),
    ): String =
        OsActivityShortcutCardImportExport.buildCardsExportJson(
            cards = cards,
            defaults = defaults,
            exportedAtMillis = exportedAtMillis,
        )

    fun parseCardsImport(
        root: OsCardImportRoot,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard> =
            defaultBuiltInActivityShortcutCards(builtInSampleDefaults),
    ): OsActivityCardImportPayload =
        OsActivityShortcutCardImportExport.parseCardsImport(
            root = root,
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
        )

    fun previewImportedCards(
        payload: OsActivityCardImportPayload,
        existingCards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard> =
            defaultBuiltInActivityShortcutCards(builtInSampleDefaults),
    ): OsActivityCardImportMergeResult =
        OsActivityShortcutCardImportExport.mergeImportedCards(
            existingCards = existingCards,
            importedCards = payload.cards,
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
        )

    fun applyImportedCards(
        payload: OsActivityCardImportPayload,
        existingCards: List<OsActivityShortcutCard>,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard> =
            defaultBuiltInActivityShortcutCards(builtInSampleDefaults),
    ): OsActivityCardImportMergeResult {
        val result =
            previewImportedCards(
                payload = payload,
                existingCards = existingCards,
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
            )
        saveCards(cards = result.cards, defaults = defaults)
        return result
    }

    fun importCardsFromJsonMerged(
        raw: String,
        defaults: OsGoogleSystemServiceConfig = OsGoogleSystemServiceConfig(),
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard> =
            defaultBuiltInActivityShortcutCards(builtInSampleDefaults),
    ): OsActivityCardImportMergeResult {
        val payload =
            parseCardsImport(
                root = parseOsCardImportRoot(raw),
                defaults = defaults,
                builtInSampleDefaults = builtInSampleDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
            )
        if (payload.cards.isEmpty()) {
            throw OsCardImportException(OsCardImportError.NoValidActivityCards)
        }
        return applyImportedCards(
            payload = payload,
            existingCards =
                loadCards(
                    defaults = defaults,
                    builtInSampleDefaults = builtInSampleDefaults,
                    builtInActivityShortcutCards = builtInActivityShortcutCards,
                ),
            defaults = defaults,
            builtInSampleDefaults = builtInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
        )
    }

    internal fun migrateBuiltInActivityShortcutCards(
        cards: List<OsActivityShortcutCard>,
        builtInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard> =
            defaultBuiltInActivityShortcutCards(builtInSampleDefaults),
        appendMissingBuiltIns: Boolean = false,
    ): List<OsActivityShortcutCard> =
        OsActivityShortcutCardMigration.migrateBuiltInActivityShortcutCards(
            cards = cards,
            builtInSampleDefaults = builtInSampleDefaults,
            builtInActivityShortcutCards = builtInActivityShortcutCards,
            appendMissingBuiltIns = appendMissingBuiltIns,
        )
}
