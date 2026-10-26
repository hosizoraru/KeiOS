package os.kei.ui.page.main.os.shell

import os.kei.ui.page.main.os.store.OsCardKeyValueStore
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import os.kei.core.json.encodeCompact
import os.kei.core.json.optString
import os.kei.core.json.parseJsonArrayOrNull

internal object OsShellCommandBuiltInMerge {
    fun appendMissingBuiltInShellCommandCards(
        cards: List<OsShellCommandCard>,
        builtInShellCommandCards: List<OsShellCommandCard>,
        store: OsCardKeyValueStore,
        persistCards: (List<OsShellCommandCard>) -> Unit,
    ): List<OsShellCommandCard> {
        val builtIns = builtInShellCommandCards.mapNotNull(OsShellCommandCardCodec::normalizeCard)
        if (builtIns.isEmpty()) return cards
        val deletedBuiltInIds = readDeletedBuiltInShellCardIds(store)
        val merged = cards.toMutableList()
        builtIns.forEach { builtIn ->
            val alreadyPresent =
                merged.any { card ->
                    card.id == builtIn.id ||
                        OsShellCommandCardCodec.mergeKeyFor(card) == OsShellCommandCardCodec.mergeKeyFor(builtIn)
                }
            if (!alreadyPresent && builtIn.id !in deletedBuiltInIds) {
                merged += builtIn
            }
        }
        val normalized = merged.mapNotNull(OsShellCommandCardCodec::normalizeCard)
        if (normalized != cards) {
            persistCards(normalized)
        }
        return normalized
    }

    fun readDeletedBuiltInShellCardIds(store: OsCardKeyValueStore): Set<String> {
        val raw = store.decodeString(OS_SHELL_COMMAND_CARD_KEY_DELETED_BUILT_INS).orEmpty().trim()
        if (raw.isBlank()) return emptySet()
        return runCatching {
            val array = raw.parseJsonArrayOrNull() ?: return@runCatching emptySet()
            buildSet {
                for (index in array.indices) {
                    val id = array.optString(index).trim()
                    if (id in BUILT_IN_SHELL_COMMAND_CARD_IDS) add(id)
                }
            }
        }.getOrDefault(emptySet())
    }

    fun persistDeletedBuiltInShellCardIds(
        store: OsCardKeyValueStore,
        ids: Set<String>,
    ) {
        val normalized = ids.filter { id -> id in BUILT_IN_SHELL_COMMAND_CARD_IDS }.sorted()
        if (normalized.isEmpty()) {
            store.removeValueForKey(OS_SHELL_COMMAND_CARD_KEY_DELETED_BUILT_INS)
            return
        }
        val array = buildJsonArray {
            normalized.forEach { id ->
                add(JsonPrimitive(id))
            }
        }
        store.encode(OS_SHELL_COMMAND_CARD_KEY_DELETED_BUILT_INS, array.encodeCompact())
    }
}
