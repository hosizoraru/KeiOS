package com.example.keios.ui.page.main

import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

internal data class OsShellCommandCard(
    val id: String,
    val visible: Boolean = true,
    val title: String,
    val subtitle: String = "",
    val command: String,
    val runOutput: String = "",
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
    private const val KEY_CREATED_AT = "createdAtMillis"
    private const val KEY_UPDATED_AT = "updatedAtMillis"

    private const val LEGACY_KEY_SHELL_COMMAND = "shell_runner_saved_command"
    private const val LEGACY_KEY_SHELL_COMMAND_TITLE = "shell_runner_saved_command_title"
    private const val LEGACY_KEY_SHELL_COMMAND_SUBTITLE = "shell_runner_saved_command_subtitle"
    private const val LEGACY_KEY_SHELL_COMMAND_SAVED_AT = "shell_runner_saved_command_saved_at"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadCards(): List<OsShellCommandCard> {
        val raw = store.decodeString(KEY_SHELL_COMMAND_CARDS).orEmpty().trim()
        if (raw.isNotBlank()) {
            decodeCards(raw).takeIf { it.isNotEmpty() }?.let { return it }
        }
        val migrated = loadLegacySnapshot()?.let { legacy -> listOf(legacy) }.orEmpty()
        if (migrated.isNotEmpty()) {
            saveCards(migrated)
            clearLegacySnapshot()
        }
        return migrated
    }

    fun saveCards(cards: List<OsShellCommandCard>) {
        val normalized = cards.mapNotNull { normalizeCard(it) }
        if (normalized.isEmpty()) {
            store.removeValueForKey(KEY_SHELL_COMMAND_CARDS)
            return
        }
        store.encode(KEY_SHELL_COMMAND_CARDS, encodeCards(normalized))
    }

    fun createCard(
        command: String,
        title: String,
        subtitle: String,
        runOutput: String = ""
    ): OsShellCommandCard? {
        val now = System.currentTimeMillis()
        val card = normalizeCard(
            OsShellCommandCard(
                id = newOsShellCommandCardId(),
                visible = true,
                title = title,
                subtitle = subtitle,
                command = command,
                runOutput = runOutput,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        ) ?: return null
        saveCards(loadCards() + card)
        return card
    }

    fun updateCard(
        cardId: String,
        title: String,
        subtitle: String,
        command: String
    ): OsShellCommandCard? {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return null
        val current = loadCards()
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
        val next = current.map { card -> if (card.id == targetId) updated else card }
        saveCards(next)
        return updated
    }

    fun setCardVisible(cardId: String, visible: Boolean): List<OsShellCommandCard> {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return loadCards()
        val updated = loadCards().map { card ->
            if (card.id == targetId) card.copy(visible = visible) else card
        }
        saveCards(updated)
        return updated
    }

    fun deleteCard(cardId: String): List<OsShellCommandCard> {
        val targetId = cardId.trim()
        if (targetId.isBlank()) return loadCards()
        val updated = loadCards().filterNot { card -> card.id == targetId }
        saveCards(updated)
        return updated
    }

    fun findLatestByCommand(command: String): OsShellCommandCard? {
        val normalized = command.trim()
        if (normalized.isBlank()) return null
        return loadCards().asReversed().firstOrNull { it.command == normalized }
    }

    private fun normalizeCard(card: OsShellCommandCard): OsShellCommandCard? {
        val normalizedCommand = card.command.trim()
        if (normalizedCommand.isBlank()) return null
        val now = System.currentTimeMillis()
        val updatedAt = card.updatedAtMillis.takeIf { it > 0L } ?: now
        val createdAt = card.createdAtMillis.takeIf { it > 0L } ?: updatedAt
        return card.copy(
            id = card.id.trim().ifBlank { newOsShellCommandCardId() },
            title = card.title.trim().ifBlank { defaultOsShellCommandCardTitle(normalizedCommand) },
            subtitle = card.subtitle.trim(),
            command = normalizedCommand,
            runOutput = normalizeRunOutput(card.runOutput),
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
}
