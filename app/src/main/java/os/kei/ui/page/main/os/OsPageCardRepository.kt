package os.kei.ui.page.main.os

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardDataSource
import os.kei.ui.page.main.os.shell.OsShellCommandCardStoreDataSource
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import os.kei.ui.page.main.os.shortcut.newOsActivityShortcutCardId
import os.kei.ui.page.main.os.shortcut.normalizeActivityShortcutConfig

internal class OsPageCardRepository(
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.osOperations,
    private val shellCommandCards: OsShellCommandCardDataSource = OsShellCommandCardStoreDataSource,
    private val saveActivityCards: (List<OsActivityShortcutCard>, OsGoogleSystemServiceConfig) -> Unit =
        { cards, defaults -> OsActivityShortcutCardStore.saveCards(cards, defaults) },
) {
    suspend fun saveShellCommandCardEdit(
        cardId: String,
        title: String,
        subtitle: String,
        command: String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ): List<OsShellCommandCard>? =
        withContext(ioDispatcher) {
            shellCommandCards.updateCard(
                cardId = cardId,
                title = title,
                subtitle = subtitle,
                command = command,
            ) ?: return@withContext null
            shellCommandCards.loadCards(builtInShellCommandCards)
        }

    suspend fun deleteShellCommandCard(
        cardId: String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ): List<OsShellCommandCard> =
        withContext(ioDispatcher) {
            shellCommandCards.deleteCard(cardId)
            shellCommandCards.loadCards(builtInShellCommandCards)
        }

    suspend fun saveActivityShortcutCard(
        persistent: OsPagePersistentState,
        editMode: OsActivityCardEditMode,
        editingCardId: String?,
        draft: OsGoogleSystemServiceConfig,
        defaults: OsGoogleSystemServiceConfig,
    ): List<OsActivityShortcutCard> =
        withContext(ioDispatcher) {
            check(persistent.loaded) { "Activity cards must be loaded before editing" }
            val cards = persistent.activityShortcutCards
            check(editMode == OsActivityCardEditMode.Add || cards.any { it.id == editingCardId }) {
                "Activity edit target no longer exists"
            }
            val normalized = normalizeActivityShortcutConfig(draft, defaults)
            val updatedCards =
                if (editMode == OsActivityCardEditMode.Add || editingCardId.isNullOrBlank()) {
                    cards +
                        OsActivityShortcutCard(
                            id = newOsActivityShortcutCardId(),
                            visible = true,
                            isBuiltInSample = false,
                            config = normalized,
                        )
                } else {
                    cards.map { card ->
                        if (card.id == editingCardId) card.copy(config = normalized) else card
                    }
                }
            saveActivityCards(updatedCards, defaults)
            updatedCards
        }

    suspend fun deleteActivityShortcutCard(
        cards: List<OsActivityShortcutCard>,
        cardId: String,
        defaults: OsGoogleSystemServiceConfig,
    ): List<OsActivityShortcutCard> =
        withContext(ioDispatcher) {
            val updatedCards = cards.filterNot { card -> card.id == cardId }
            OsActivityShortcutCardStore.saveCards(
                cards = updatedCards,
                defaults = defaults,
            )
            updatedCards
        }
}
