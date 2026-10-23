package os.kei.ui.page.main.os.state

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optArray
import os.kei.core.json.optBoolean
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.state.EditorRecoverySlot
import os.kei.core.state.EditorRecoveryStore
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsPageRuntimeState
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtra
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtraType

internal class OsEditorRecovery(private val store: EditorRecoveryStore) {
    private var last = decodeOsEditorRecovery(store.load(EditorRecoverySlot.OsCard).orEmpty())

    fun restore(): OsPageRuntimeState = last ?: OsPageRuntimeState()

    fun save(state: OsPageRuntimeState) {
        val editor = state.editorOnly()
        if (editor == last) return
        store.save(EditorRecoverySlot.OsCard, editor?.let(::encodeOsEditorRecovery))
        last = editor
    }
}

private fun OsPageRuntimeState.editorOnly(): OsPageRuntimeState? = when {
    showActivityShortcutEditor && activityShortcutDraft != null -> OsPageRuntimeState(
        showActivityShortcutEditor = true,
        activityCardEditMode = activityCardEditMode,
        editingActivityShortcutCardId = editingActivityShortcutCardId,
        editingActivityShortcutBuiltIn = editingActivityShortcutBuiltIn,
        activityShortcutDraft = activityShortcutDraft,
    )
    showShellCommandCardEditor && shellCommandCardDraft != null -> OsPageRuntimeState(
        showShellCommandCardEditor = true,
        editingShellCommandCardId = editingShellCommandCardId,
        // Output, run state and cached results are not editor input.
        shellCommandCardDraft = shellCommandCardDraft.copy(runOutput = "", lastRunAtMillis = 0),
    )
    else -> null
}

internal fun encodeOsEditorRecovery(state: OsPageRuntimeState): String = buildJsonObject {
    if (state.showActivityShortcutEditor) {
        val draft = requireNotNull(state.activityShortcutDraft)
        put("kind", "activity")
        put("mode", state.activityCardEditMode.name)
        put("id", state.editingActivityShortcutCardId.orEmpty())
        put("builtIn", state.editingActivityShortcutBuiltIn)
        put("title", draft.title)
        put("subtitle", draft.subtitle)
        put("appName", draft.appName)
        put("package", draft.packageName)
        put("class", draft.className)
        put("action", draft.intentAction)
        put("category", draft.intentCategory)
        put("flags", draft.intentFlags)
        put("uri", draft.intentUriData)
        put("mime", draft.intentMimeType)
        put("extras", buildJsonArray {
            draft.intentExtras.forEach { extra ->
                add(buildJsonObject {
                    put("key", extra.key)
                    put("type", extra.type.rawValue)
                    put("value", extra.value)
                })
            }
        })
    } else {
        val draft = requireNotNull(state.shellCommandCardDraft)
        put("kind", "shell")
        put("id", state.editingShellCommandCardId.orEmpty())
        put("title", draft.title)
        put("subtitle", draft.subtitle)
        put("command", draft.command)
    }
}.encodeCompact()

internal fun decodeOsEditorRecovery(raw: String): OsPageRuntimeState? {
    val record = raw.parseJsonObjectOrNull() ?: return null
    val id = record.optString("id").takeIf(String::isNotBlank)
    return when (record.optString("kind")) {
        "activity" -> {
            val mode = OsActivityCardEditMode.entries.firstOrNull {
                it.name == record.optString("mode")
            } ?: return null
            if (mode == OsActivityCardEditMode.Edit && id == null) return null
            val extras = record.optArray("extras")?.map { element ->
                val extra = element as? JsonObject ?: return null
                val type = ShortcutIntentExtraType.entries.firstOrNull {
                    it.rawValue == extra.optString("type")
                } ?: return null
                ShortcutIntentExtra(extra.optString("key"), type, extra.optString("value"))
            }.orEmpty()
            OsPageRuntimeState(
                showActivityShortcutEditor = true,
                activityCardEditMode = mode,
                editingActivityShortcutCardId = id,
                editingActivityShortcutBuiltIn = record.optBoolean("builtIn"),
                activityShortcutDraft = OsGoogleSystemServiceConfig(
                    title = record.optString("title"),
                    subtitle = record.optString("subtitle"),
                    appName = record.optString("appName"),
                    packageName = record.optString("package"),
                    className = record.optString("class"),
                    intentAction = record.optString("action"),
                    intentCategory = record.optString("category"),
                    intentFlags = record.optString("flags"),
                    intentUriData = record.optString("uri"),
                    intentMimeType = record.optString("mime"),
                    intentExtras = extras,
                ),
            )
        }
        "shell" -> {
            if (id == null) return null
            OsPageRuntimeState(
                showShellCommandCardEditor = true,
                editingShellCommandCardId = id,
                shellCommandCardDraft = OsShellCommandCard(
                    id = id,
                    title = record.optString("title"),
                    subtitle = record.optString("subtitle"),
                    command = record.optString("command"),
                ),
            )
        }
        else -> null
    }
}
