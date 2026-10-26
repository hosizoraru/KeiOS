package os.kei.ui.page.main.os.shell

import os.kei.ui.page.main.os.store.OsCardKeyValueStore
import os.kei.ui.page.main.os.store.MmkvOsCardKeyValueStore

internal class OsShellCommandCardPersistence(
    private val store: OsCardKeyValueStore = MmkvOsCardKeyValueStore(OS_SHELL_COMMAND_CARD_KV_ID),
) {
    private val storeLock = Any()
    private var cachedCards: List<OsShellCommandCard>? = null

    fun loadCards(builtInShellCommandCards: List<OsShellCommandCard> = emptyList()): List<OsShellCommandCard> {
        synchronized(storeLock) {
            return readCachedCardsLocked(builtInShellCommandCards).toList()
        }
    }

    fun saveCards(cards: List<OsShellCommandCard>) {
        synchronized(storeLock) {
            persistCardsLocked(cards.mapNotNull(OsShellCommandCardCodec::normalizeCard))
        }
    }

    fun replaceFromSync(cards: List<OsShellCommandCard>) = synchronized(storeLock) {
        persistCardsLocked(cards.mapNotNull(OsShellCommandCardCodec::normalizeCard))
        clearLegacySnapshot()
        store.encode(KEY_AUTHORITATIVE_SNAPSHOT, true)
    }

    fun createCard(
        command: String,
        title: String,
        subtitle: String,
        runOutput: String,
    ): OsShellCommandCard? {
        synchronized(storeLock) {
            val now = OsShellCommandCardSystemClock.nowMs()
            val card =
                OsShellCommandCardCodec.normalizeCard(
                    OsShellCommandCard(
                        id = newOsShellCommandCardId(),
                        visible = true,
                        title = title,
                        subtitle = subtitle,
                        command = command,
                        runOutput = runOutput,
                        lastRunAtMillis = if (runOutput.isBlank()) 0L else now,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                    nowMs = now,
                ) ?: return null
            persistCardsLocked(readCachedCardsLocked() + card)
            return card
        }
    }

    fun updateCard(
        cardId: String,
        title: String,
        subtitle: String,
        command: String,
    ): OsShellCommandCard? {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return null
        synchronized(storeLock) {
            val current = readCachedCardsLocked()
            val existing = current.firstOrNull { it.id == targetId } ?: return null
            val updated =
                OsShellCommandCardCodec.normalizeCard(
                    existing.copy(
                        title = title,
                        subtitle = subtitle,
                        command = command,
                        updatedAtMillis = OsShellCommandCardSystemClock.nowMs(),
                    ),
                ) ?: return null
            persistCardsLocked(current.map { card -> if (card.id == targetId) updated else card })
            return updated
        }
    }

    fun setCardVisible(
        cardId: String,
        visible: Boolean,
    ): List<OsShellCommandCard> {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return loadCards()
        synchronized(storeLock) {
            val updated =
                readCachedCardsLocked().map { card ->
                    if (card.id == targetId) card.copy(visible = visible) else card
                }
            persistCardsLocked(updated)
            return updated
        }
    }

    fun deleteCard(cardId: String): List<OsShellCommandCard> {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return loadCards()
        synchronized(storeLock) {
            val current = readCachedCardsLocked()
            val targetCard = current.firstOrNull { card -> card.id == targetId }
            val deletedBuiltInId =
                targetId
                    .takeIf { id -> id in BUILT_IN_SHELL_COMMAND_CARD_IDS }
                    ?: targetCard?.let { card -> builtInShellCommandCardIdForCommand(card.command) }
            val updated = current.filterNot { card -> card.id == targetId }
            if (deletedBuiltInId != null) {
                OsShellCommandBuiltInMerge.persistDeletedBuiltInShellCardIds(
                    store = store,
                    ids = OsShellCommandBuiltInMerge.readDeletedBuiltInShellCardIds(store) + deletedBuiltInId,
                )
            }
            persistCardsLocked(updated)
            return updated
        }
    }

    fun updateCardRunResult(
        cardId: String,
        runOutput: String,
        runAtMillis: Long,
    ): OsShellCommandCard? {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return null
        synchronized(storeLock) {
            val current = readCachedCardsLocked()
            val existing = current.firstOrNull { it.id == targetId } ?: return null
            val resolvedRunAt = runAtMillis.takeIf { it > 0L } ?: OsShellCommandCardSystemClock.nowMs()
            val preservedUpdatedAt =
                existing.updatedAtMillis
                    .takeIf { it > 0L }
                    ?: existing.createdAtMillis.takeIf { it > 0L }
                    ?: resolvedRunAt
            val updated =
                OsShellCommandCardCodec.normalizeCard(
                    existing.copy(
                        runOutput = runOutput,
                        lastRunAtMillis = resolvedRunAt,
                        updatedAtMillis = preservedUpdatedAt,
                    ),
                ) ?: return null
            persistCardsLocked(current.map { card -> if (card.id == targetId) updated else card })
            return updated
        }
    }

    fun findLatestByCommand(command: String): OsShellCommandCard? {
        val normalized = command.trim()
        if (normalized.isBlank()) return null
        synchronized(storeLock) {
            return readCachedCardsLocked().asReversed().firstOrNull { it.command == normalized }
        }
    }

    private fun readCachedCardsLocked(builtInShellCommandCards: List<OsShellCommandCard> = emptyList()): List<OsShellCommandCard> {
        if (store.decodeBool(KEY_AUTHORITATIVE_SNAPSHOT, false)) {
            return cachedCards ?: OsShellCommandCardCodec.decodeCards(
                store.decodeString(OS_SHELL_COMMAND_CARD_KEY_CARDS).orEmpty(),
            ).also { cachedCards = it }
        }
        cachedCards?.let { cached ->
            return appendMissingBuiltInsLocked(
                cards = cached,
                builtInShellCommandCards = builtInShellCommandCards,
            )
        }
        val raw = store.decodeString(OS_SHELL_COMMAND_CARD_KEY_CARDS).orEmpty().trim()
        val decoded =
            if (raw.isNotBlank()) {
                OsShellCommandCardCodec.decodeCards(raw)
            } else {
                emptyList()
            }
        if (decoded.isNotEmpty()) {
            return appendMissingBuiltInsLocked(
                cards = decoded,
                builtInShellCommandCards = builtInShellCommandCards,
            )
        }
        val migrated = loadLegacySnapshot()?.let { legacy -> listOf(legacy) }.orEmpty()
        if (migrated.isNotEmpty()) {
            val withBuiltIns =
                appendMissingBuiltInsLocked(
                    cards = migrated,
                    builtInShellCommandCards = builtInShellCommandCards,
                )
            persistCardsLocked(withBuiltIns)
            clearLegacySnapshot()
            return withBuiltIns
        }
        return appendMissingBuiltInsLocked(
            cards = emptyList(),
            builtInShellCommandCards = builtInShellCommandCards,
        )
    }

    private companion object {
        const val KEY_AUTHORITATIVE_SNAPSHOT = "authoritative_sync_snapshot"
    }

    private fun persistCardsLocked(cards: List<OsShellCommandCard>) {
        val normalized = cards.mapNotNull(OsShellCommandCardCodec::normalizeCard)
        cachedCards = normalized
        if (normalized.isEmpty()) {
            store.removeValueForKey(OS_SHELL_COMMAND_CARD_KEY_CARDS)
            return
        }
        store.encode(OS_SHELL_COMMAND_CARD_KEY_CARDS, OsShellCommandCardCodec.encodeCards(normalized))
    }

    private fun appendMissingBuiltInsLocked(
        cards: List<OsShellCommandCard>,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ): List<OsShellCommandCard> {
        val normalized =
            OsShellCommandBuiltInMerge.appendMissingBuiltInShellCommandCards(
                cards = cards,
                builtInShellCommandCards = builtInShellCommandCards,
                store = store,
                persistCards = ::persistCardsLocked,
            )
        cachedCards = normalized
        return normalized
    }

    private fun loadLegacySnapshot(): OsShellCommandCard? {
        val command = store.decodeString(OS_SHELL_LEGACY_KEY_COMMAND).orEmpty().trim()
        if (command.isBlank()) return null
        val title = store.decodeString(OS_SHELL_LEGACY_KEY_TITLE).orEmpty().trim()
        val subtitle = store.decodeString(OS_SHELL_LEGACY_KEY_SUBTITLE).orEmpty().trim()
        val savedAt = store.decodeLong(OS_SHELL_LEGACY_KEY_SAVED_AT, 0L)
        val now = OsShellCommandCardSystemClock.nowMs()
        val timestamp = savedAt.takeIf { it > 0L } ?: now
        return OsShellCommandCardCodec.normalizeCard(
            OsShellCommandCard(
                id = newOsShellCommandCardId(),
                visible = true,
                title = title,
                subtitle = subtitle,
                command = command,
                runOutput = "",
                lastRunAtMillis = 0L,
                createdAtMillis = timestamp,
                updatedAtMillis = timestamp,
            ),
            nowMs = now,
        )
    }

    private fun clearLegacySnapshot() {
        store.removeValueForKey(OS_SHELL_LEGACY_KEY_COMMAND)
        store.removeValueForKey(OS_SHELL_LEGACY_KEY_TITLE)
        store.removeValueForKey(OS_SHELL_LEGACY_KEY_SUBTITLE)
        store.removeValueForKey(OS_SHELL_LEGACY_KEY_SAVED_AT)
    }
}
