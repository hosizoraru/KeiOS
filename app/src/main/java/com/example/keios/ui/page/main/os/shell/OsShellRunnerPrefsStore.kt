package com.example.keios.ui.page.main

import com.tencent.mmkv.MMKV

internal data class OsShellRunnerPersistSettings(
    val persistInput: Boolean = false,
    val persistOutput: Boolean = false
)

internal object OsShellRunnerPrefsStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_PERSIST_INPUT = "os_shell_runner_persist_input_v1"
    private const val KEY_PERSIST_OUTPUT = "os_shell_runner_persist_output_v1"
    private const val KEY_SAVED_INPUT = "os_shell_runner_saved_input_v1"
    private const val KEY_SAVED_OUTPUT = "os_shell_runner_saved_output_v1"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadPersistSettings(): OsShellRunnerPersistSettings {
        return OsShellRunnerPersistSettings(
            persistInput = store.decodeBool(KEY_PERSIST_INPUT, false),
            persistOutput = store.decodeBool(KEY_PERSIST_OUTPUT, false)
        )
    }

    fun savePersistInput(enabled: Boolean) {
        store.encode(KEY_PERSIST_INPUT, enabled)
    }

    fun savePersistOutput(enabled: Boolean) {
        store.encode(KEY_PERSIST_OUTPUT, enabled)
    }

    fun loadSavedInput(): String {
        return store.decodeString(KEY_SAVED_INPUT).orEmpty()
    }

    fun loadSavedOutput(): String {
        return store.decodeString(KEY_SAVED_OUTPUT).orEmpty()
    }

    fun saveInput(value: String) {
        val normalized = value
            .trimEnd()
            .take(maxSavedInputLength)
        if (normalized.isBlank()) {
            store.removeValueForKey(KEY_SAVED_INPUT)
            return
        }
        store.encode(KEY_SAVED_INPUT, normalized)
    }

    fun saveOutput(value: String) {
        val normalized = value.trimEnd()
        val clipped = if (normalized.length <= maxSavedOutputLength) {
            normalized
        } else {
            normalized.takeLast(maxSavedOutputLength)
        }
        if (clipped.isBlank()) {
            store.removeValueForKey(KEY_SAVED_OUTPUT)
            return
        }
        store.encode(KEY_SAVED_OUTPUT, clipped)
    }

    fun clearSavedInput() {
        store.removeValueForKey(KEY_SAVED_INPUT)
    }

    fun clearSavedOutput() {
        store.removeValueForKey(KEY_SAVED_OUTPUT)
    }

    private const val maxSavedInputLength = 8_000
    private const val maxSavedOutputLength = 120_000
}
