package os.kei.ui.page.main.os.shell

import os.kei.ui.page.main.os.transfer.OsCardImportError
import os.kei.ui.page.main.os.transfer.OsCardImportException
import os.kei.ui.page.main.os.transfer.OsCardImportRoot
import os.kei.ui.page.main.os.transfer.OsShellCardImportPayload
import os.kei.ui.page.main.os.transfer.parseOsCardImportRoot

internal object OsShellCommandCardStore {
    private val persistence = OsShellCommandCardPersistence()

    fun replaceFromSync(cards: List<OsShellCommandCard>) = persistence.replaceFromSync(cards)

    fun loadCards(builtInShellCommandCards: List<OsShellCommandCard> = emptyList()): List<OsShellCommandCard> =
        persistence.loadCards(builtInShellCommandCards)

    fun saveCards(cards: List<OsShellCommandCard>) {
        persistence.saveCards(cards)
    }

    fun createCard(
        command: String,
        title: String,
        subtitle: String,
        runOutput: String = "",
    ): OsShellCommandCard? =
        persistence.createCard(
            command = command,
            title = title,
            subtitle = subtitle,
            runOutput = runOutput,
        )

    fun updateCard(
        cardId: String,
        title: String,
        subtitle: String,
        command: String,
    ): OsShellCommandCard? =
        persistence.updateCard(
            cardId = cardId,
            title = title,
            subtitle = subtitle,
            command = command,
        )

    fun setCardVisible(
        cardId: String,
        visible: Boolean,
    ): List<OsShellCommandCard> =
        persistence.setCardVisible(
            cardId = cardId,
            visible = visible,
        )

    fun deleteCard(cardId: String): List<OsShellCommandCard> = persistence.deleteCard(cardId)

    fun updateCardRunResult(
        cardId: String,
        runOutput: String,
        runAtMillis: Long = OsShellCommandCardSystemClock.nowMs(),
    ): OsShellCommandCard? =
        persistence.updateCardRunResult(
            cardId = cardId,
            runOutput = runOutput,
            runAtMillis = runAtMillis,
        )

    fun findLatestByCommand(command: String): OsShellCommandCard? = persistence.findLatestByCommand(command)

    fun buildCardsExportJson(
        cards: List<OsShellCommandCard> = loadCards(),
        exportedAtMillis: Long = OsShellCommandCardSystemClock.nowMs(),
    ): String =
        OsShellCommandCardImportExport.buildCardsExportJson(
            cards = cards,
            exportedAtMillis = exportedAtMillis,
        )

    fun parseCardsImport(root: OsCardImportRoot): OsShellCardImportPayload = OsShellCommandCardImportExport.parseCardsImport(root)

    fun previewImportedCards(
        payload: OsShellCardImportPayload,
        existingCards: List<OsShellCommandCard>,
    ): OsShellCardImportMergeResult =
        OsShellCommandCardImportExport.mergeImportedCards(
            existingCards = existingCards,
            importedCards = payload.cards,
        )

    fun applyImportedCards(
        payload: OsShellCardImportPayload,
        existingCards: List<OsShellCommandCard>,
    ): OsShellCardImportMergeResult {
        val result = previewImportedCards(payload = payload, existingCards = existingCards)
        saveCards(result.cards)
        return result
    }

    fun importCardsFromJsonMerged(raw: String): OsShellCardImportMergeResult {
        val payload = parseCardsImport(root = parseOsCardImportRoot(raw))
        if (payload.cards.isEmpty()) {
            throw OsCardImportException(OsCardImportError.NoValidShellCards)
        }
        return applyImportedCards(payload = payload, existingCards = loadCards())
    }
}
