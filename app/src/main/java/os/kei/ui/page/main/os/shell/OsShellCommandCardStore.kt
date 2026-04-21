package os.kei.ui.page.main.os.shell

import com.tencent.mmkv.MMKV
import os.kei.ui.page.main.os.transfer.OS_SHELL_CARD_EXPORT_SCHEMA
import os.kei.ui.page.main.os.transfer.OsCardImportRoot
import os.kei.ui.page.main.os.transfer.OsShellCardImportPayload
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

internal data class OsShellCardImportMergeResult(
    val cards: List<OsShellCommandCard>,
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int
)

internal data class OsShellCommandCard(
    val id: String,
    val visible: Boolean = true,
    val title: String,
    val subtitle: String = "",
    val command: String,
    val runOutput: String = "",
    val lastRunAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

internal fun newOsShellCommandCardId(): String {
    val compactUuid = UUID.randomUUID().toString().replace("-", "").take(12)
    return "shell-${compactUuid.lowercase(Locale.ROOT)}"
}

internal fun defaultOsShellCommandCardTitle(command: String): String {
    val normalized = command.trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return ""
    return if (normalized.length <= 36) normalized else "${normalized.take(35)}…"
}

internal fun createDefaultShellCommandCardDraft(command: String = ""): OsShellCommandCard {
    val normalizedCommand = command.trim()
    return OsShellCommandCard(
        id = "",
        visible = true,
        title = defaultOsShellCommandCardTitle(normalizedCommand),
        subtitle = "",
        command = normalizedCommand,
        runOutput = "",
        lastRunAtMillis = 0L,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}

internal object OsShellCommandCardStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_SHELL_COMMAND_CARDS = "os_shell_command_cards_v1"

    private const val KEY_ID = "id"
    private const val KEY_VISIBLE = "visible"
    private const val KEY_TITLE = "title"
    private const val KEY_SUBTITLE = "subtitle"
    private const val KEY_COMMAND = "command"
    private const val KEY_RUN_OUTPUT = "runOutput"
    private const val KEY_LAST_RUN_AT = "lastRunAtMillis"
    private const val KEY_CREATED_AT = "createdAtMillis"
    private const val KEY_UPDATED_AT = "updatedAtMillis"

    private const val LEGACY_KEY_SHELL_COMMAND = "shell_runner_saved_command"
    private const val LEGACY_KEY_SHELL_COMMAND_TITLE = "shell_runner_saved_command_title"
    private const val LEGACY_KEY_SHELL_COMMAND_SUBTITLE = "shell_runner_saved_command_subtitle"
    private const val LEGACY_KEY_SHELL_COMMAND_SAVED_AT = "shell_runner_saved_command_saved_at"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private val storeLock = Any()
    private var cachedCards: List<OsShellCommandCard>? = null

    fun loadCards(): List<OsShellCommandCard> {
        synchronized(storeLock) {
            return readCachedCardsLocked().toList()
        }
    }

    fun saveCards(cards: List<OsShellCommandCard>) {
        synchronized(storeLock) {
            persistCardsLocked(cards.mapNotNull { normalizeCard(it) })
        }
    }

    fun createCard(
        command: String,
        title: String,
        subtitle: String,
        runOutput: String = ""
    ): OsShellCommandCard? {
        synchronized(storeLock) {
            val now = System.currentTimeMillis()
            val card = normalizeCard(
                OsShellCommandCard(
                    id = newOsShellCommandCardId(),
                    visible = true,
                    title = title,
                    subtitle = subtitle,
                    command = command,
                    runOutput = runOutput,
                    lastRunAtMillis = if (runOutput.isBlank()) 0L else now,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            ) ?: return null
            persistCardsLocked(readCachedCardsLocked() + card)
            return card
        }
    }

    fun updateCard(
        cardId: String,
        title: String,
        subtitle: String,
        command: String
    ): OsShellCommandCard? {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return null
        synchronized(storeLock) {
            val current = readCachedCardsLocked()
            val existing = current.firstOrNull { it.id == targetId } ?: return null
            val now = System.currentTimeMillis()
            val updated = normalizeCard(
                existing.copy(
                    title = title,
                    subtitle = subtitle,
                    command = command,
                    updatedAtMillis = now
                )
            ) ?: return null
            persistCardsLocked(current.map { card -> if (card.id == targetId) updated else card })
            return updated
        }
    }

    fun setCardVisible(cardId: String, visible: Boolean): List<OsShellCommandCard> {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return loadCards()
        synchronized(storeLock) {
            val updated = readCachedCardsLocked().map { card ->
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
            val updated = readCachedCardsLocked().filterNot { card -> card.id == targetId }
            persistCardsLocked(updated)
            return updated
        }
    }

    fun updateCardRunResult(
        cardId: String,
        runOutput: String,
        runAtMillis: Long = System.currentTimeMillis()
    ): OsShellCommandCard? {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return null
        synchronized(storeLock) {
            val current = readCachedCardsLocked()
            val existing = current.firstOrNull { it.id == targetId } ?: return null
            val resolvedRunAt = runAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
            val preservedUpdatedAt = existing.updatedAtMillis
                .takeIf { it > 0L }
                ?: existing.createdAtMillis.takeIf { it > 0L }
                ?: resolvedRunAt
            val updated = normalizeCard(
                existing.copy(
                    runOutput = runOutput,
                    lastRunAtMillis = resolvedRunAt,
                    updatedAtMillis = preservedUpdatedAt
                )
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

    private fun readCachedCardsLocked(): List<OsShellCommandCard> {
        cachedCards?.let { return it }
        val raw = store.decodeString(KEY_SHELL_COMMAND_CARDS).orEmpty().trim()
        val decoded = if (raw.isNotBlank()) {
            decodeCards(raw)
        } else {
            emptyList()
        }
        if (decoded.isNotEmpty()) {
            cachedCards = decoded
            return decoded
        }
        val migrated = loadLegacySnapshot()?.let { legacy -> listOf(legacy) }.orEmpty()
        if (migrated.isNotEmpty()) {
            persistCardsLocked(migrated)
            clearLegacySnapshot()
            return migrated
        }
        cachedCards = emptyList()
        return emptyList()
    }

    private fun persistCardsLocked(cards: List<OsShellCommandCard>) {
        val normalized = cards.mapNotNull { normalizeCard(it) }
        cachedCards = normalized
        if (normalized.isEmpty()) {
            store.removeValueForKey(KEY_SHELL_COMMAND_CARDS)
            return
        }
        store.encode(KEY_SHELL_COMMAND_CARDS, encodeCards(normalized))
    }

    private fun normalizeCard(card: OsShellCommandCard): OsShellCommandCard? {
        val normalizedCommand = card.command.trim()
        if (normalizedCommand.isBlank()) return null
        val now = System.currentTimeMillis()
        val createdAt = card.createdAtMillis.takeIf { it > 0L } ?: now
        val updatedAt = card.updatedAtMillis.takeIf { it > 0L } ?: createdAt
        val lastRunAt = card.lastRunAtMillis.takeIf { it > 0L } ?: 0L
        return card.copy(
            id = card.id.trim().ifBlank { newOsShellCommandCardId() },
            title = card.title.trim().ifBlank { defaultOsShellCommandCardTitle(normalizedCommand) },
            subtitle = card.subtitle.trim(),
            command = normalizedCommand,
            runOutput = normalizeRunOutput(card.runOutput),
            lastRunAtMillis = lastRunAt,
            createdAtMillis = createdAt,
            updatedAtMillis = updatedAt
        )
    }

    private fun encodeCards(cards: List<OsShellCommandCard>): String {
        val array = JSONArray()
        cards.forEach { card ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, card.id)
                    put(KEY_VISIBLE, card.visible)
                    put(KEY_TITLE, card.title)
                    put(KEY_SUBTITLE, card.subtitle)
                    put(KEY_COMMAND, card.command)
                    put(KEY_RUN_OUTPUT, card.runOutput)
                    put(KEY_LAST_RUN_AT, card.lastRunAtMillis)
                    put(KEY_CREATED_AT, card.createdAtMillis)
                    put(KEY_UPDATED_AT, card.updatedAtMillis)
                }
            )
        }
        return array.toString()
    }

    private fun decodeCards(raw: String): List<OsShellCommandCard> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    normalizeCard(
                        OsShellCommandCard(
                            id = item.optString(KEY_ID),
                            visible = item.optBoolean(KEY_VISIBLE, true),
                            title = item.optString(KEY_TITLE),
                            subtitle = item.optString(KEY_SUBTITLE),
                            command = item.optString(KEY_COMMAND),
                            runOutput = item.optString(KEY_RUN_OUTPUT),
                            lastRunAtMillis = item.optLong(KEY_LAST_RUN_AT, 0L),
                            createdAtMillis = item.optLong(KEY_CREATED_AT, 0L),
                            updatedAtMillis = item.optLong(KEY_UPDATED_AT, 0L)
                        )
                    )?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadLegacySnapshot(): OsShellCommandCard? {
        val command = store.decodeString(LEGACY_KEY_SHELL_COMMAND).orEmpty().trim()
        if (command.isBlank()) return null
        val title = store.decodeString(LEGACY_KEY_SHELL_COMMAND_TITLE).orEmpty().trim()
        val subtitle = store.decodeString(LEGACY_KEY_SHELL_COMMAND_SUBTITLE).orEmpty().trim()
        val savedAt = store.decodeLong(LEGACY_KEY_SHELL_COMMAND_SAVED_AT, 0L)
        val now = System.currentTimeMillis()
        val timestamp = savedAt.takeIf { it > 0L } ?: now
        return normalizeCard(
            OsShellCommandCard(
                id = newOsShellCommandCardId(),
                visible = true,
                title = title,
                subtitle = subtitle,
                command = command,
                runOutput = "",
                lastRunAtMillis = 0L,
                createdAtMillis = timestamp,
                updatedAtMillis = timestamp
            )
        )
    }

    private fun clearLegacySnapshot() {
        store.removeValueForKey(LEGACY_KEY_SHELL_COMMAND)
        store.removeValueForKey(LEGACY_KEY_SHELL_COMMAND_TITLE)
        store.removeValueForKey(LEGACY_KEY_SHELL_COMMAND_SUBTITLE)
        store.removeValueForKey(LEGACY_KEY_SHELL_COMMAND_SAVED_AT)
    }

    private fun normalizeRunOutput(output: String): String {
        val normalized = output
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        if (normalized.length <= MAX_OUTPUT_LENGTH) return normalized
        return normalized.takeLast(MAX_OUTPUT_LENGTH)
    }

    private const val MAX_OUTPUT_LENGTH = 24_000
    private const val KEY_EXPORT_SCHEMA = "schema"
    private const val KEY_EXPORT_EXPORTED_AT = "exportedAtMillis"
    private const val KEY_EXPORT_ITEM_COUNT = "itemCount"
    private const val KEY_EXPORT_ITEMS = "items"

    fun buildCardsExportJson(
        cards: List<OsShellCommandCard> = loadCards(),
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val normalized = cards.mapNotNull(::normalizeCard)
        val items = JSONArray(encodeCards(normalized))
        return JSONObject().apply {
            put(KEY_EXPORT_SCHEMA, OS_SHELL_CARD_EXPORT_SCHEMA)
            put(KEY_EXPORT_EXPORTED_AT, exportedAtMillis)
            put(KEY_EXPORT_ITEM_COUNT, normalized.size)
            put(KEY_EXPORT_ITEMS, items)
        }.toString(2)
    }

    fun parseCardsImport(root: OsCardImportRoot): OsShellCardImportPayload {
        if (root.items.length() == 0) {
            return OsShellCardImportPayload(
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
                decodeCard(item)?.let(::add)
            }
        }
        val deduplicated = mutableListOf<OsShellCommandCard>()
        var duplicateCount = 0
        decoded.forEach { imported ->
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
                    createdAtMillis = existing.createdAtMillis.takeIf { it > 0L }
                        ?: imported.createdAtMillis,
                    updatedAtMillis = maxOf(existing.updatedAtMillis, imported.updatedAtMillis)
                )
                duplicateCount += 1
            }
        }
        return OsShellCardImportPayload(
            cards = deduplicated,
            sourceCount = root.sourceCount,
            invalidCount = (root.sourceCount - decoded.size).coerceAtLeast(0),
            duplicateCount = duplicateCount,
            fileKind = root.fileKind,
            isLegacyFormat = root.isLegacyFormat
        )
    }

    fun previewImportedCards(
        payload: OsShellCardImportPayload,
        existingCards: List<OsShellCommandCard>
    ): OsShellCardImportMergeResult {
        return mergeImportedCards(existingCards = existingCards, importedCards = payload.cards)
    }

    fun applyImportedCards(
        payload: OsShellCardImportPayload,
        existingCards: List<OsShellCommandCard>
    ): OsShellCardImportMergeResult {
        val result = mergeImportedCards(existingCards = existingCards, importedCards = payload.cards)
        saveCards(result.cards)
        return result
    }

    fun importCardsFromJsonMerged(raw: String): OsShellCardImportMergeResult {
        val payload = parseCardsImport(root = os.kei.ui.page.main.os.transfer.parseOsCardImportRoot(raw))
        if (payload.cards.isEmpty()) {
            throw IllegalArgumentException("文件中没有有效的 shell card")
        }
        return applyImportedCards(payload = payload, existingCards = loadCards())
    }

    private fun mergeKeyFor(card: OsShellCommandCard): String {
        return card.command.trim().replace(Regex("\\s+"), " ")
    }

    private fun cardsEquivalent(old: OsShellCommandCard, new: OsShellCommandCard): Boolean {
        return old.visible == new.visible &&
            old.title == new.title &&
            old.subtitle == new.subtitle &&
            old.command == new.command &&
            old.runOutput == new.runOutput &&
            old.lastRunAtMillis == new.lastRunAtMillis
    }

    private fun decodeCard(item: JSONObject): OsShellCommandCard? {
        if (!item.has(KEY_COMMAND)) return null
        return normalizeCard(
            OsShellCommandCard(
                id = item.optString(KEY_ID),
                visible = item.optBoolean(KEY_VISIBLE, true),
                title = item.optString(KEY_TITLE),
                subtitle = item.optString(KEY_SUBTITLE),
                command = item.optString(KEY_COMMAND),
                runOutput = item.optString(KEY_RUN_OUTPUT),
                lastRunAtMillis = item.optLong(KEY_LAST_RUN_AT, 0L),
                createdAtMillis = item.optLong(KEY_CREATED_AT, 0L),
                updatedAtMillis = item.optLong(KEY_UPDATED_AT, 0L)
            )
        )
    }

    private fun mergeImportedCards(
        existingCards: List<OsShellCommandCard>,
        importedCards: List<OsShellCommandCard>
    ): OsShellCardImportMergeResult {
        val mergedCards = existingCards.toMutableList()
        var addedCount = 0
        var updatedCount = 0
        var unchangedCount = 0
        importedCards.forEach { imported ->
            val targetIndexById = mergedCards.indexOfFirst { it.id == imported.id }
            val targetIndex = if (targetIndexById >= 0) {
                targetIndexById
            } else {
                mergedCards.indexOfFirst { mergeKeyFor(it) == mergeKeyFor(imported) }
            }
            if (targetIndex < 0) {
                mergedCards += imported
                addedCount += 1
                return@forEach
            }
            val existing = mergedCards[targetIndex]
            val resolved = imported.copy(
                id = existing.id,
                createdAtMillis = existing.createdAtMillis.takeIf { it > 0L } ?: imported.createdAtMillis,
                updatedAtMillis = maxOf(existing.updatedAtMillis, imported.updatedAtMillis)
            )
            if (cardsEquivalent(existing, resolved)) {
                unchangedCount += 1
            } else {
                mergedCards[targetIndex] = resolved
                updatedCount += 1
            }
        }
        return OsShellCardImportMergeResult(
            cards = mergedCards,
            addedCount = addedCount,
            updatedCount = updatedCount,
            unchangedCount = unchangedCount
        )
    }
}
