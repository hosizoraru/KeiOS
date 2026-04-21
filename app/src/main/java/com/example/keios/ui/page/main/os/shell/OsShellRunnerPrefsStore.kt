package com.example.keios.ui.page.main.os.shell

import com.tencent.mmkv.MMKV

internal val shellRunnerTimeoutOptionsSeconds = listOf(30, 60, 120, 300, 600)
internal val shellRunnerOutputLimitOptionsChars = listOf(20_000, 60_000, 120_000, 300_000)
private const val shellRunnerDefaultTimeoutSeconds = 300
private const val shellRunnerDefaultOutputLimitChars = 120_000

internal enum class OsShellRunnerOutputSaveMode {
    FullHistory,
    LatestOnly
}

internal enum class OsShellRunnerStartupBehavior {
    FocusInput,
    Silent
}

internal enum class OsShellRunnerExitCleanupMode {
    KeepAll,
    ClearInput,
    ClearOutput
}

internal enum class OsShellRunnerCopyMode {
    FullHistory,
    LatestResult
}

internal data class OsShellRunnerSettings(
    val persistInput: Boolean = false,
    val persistOutput: Boolean = false,
    val commandTimeoutSeconds: Int = shellRunnerDefaultTimeoutSeconds,
    val autoFormatOutput: Boolean = false,
    val autoScrollOutput: Boolean = true,
    val outputLimitChars: Int = shellRunnerDefaultOutputLimitChars,
    val outputSaveMode: OsShellRunnerOutputSaveMode = OsShellRunnerOutputSaveMode.FullHistory,
    val dangerousCommandConfirm: Boolean = true,
    val completionToast: Boolean = true,
    val startupBehavior: OsShellRunnerStartupBehavior = OsShellRunnerStartupBehavior.Silent,
    val exitCleanupMode: OsShellRunnerExitCleanupMode = OsShellRunnerExitCleanupMode.KeepAll,
    val copyMode: OsShellRunnerCopyMode = OsShellRunnerCopyMode.FullHistory
)

internal object OsShellRunnerPrefsStore {
    private const val KV_ID = "os_ui_state"
    private const val KEY_PERSIST_INPUT = "os_shell_runner_persist_input_v1"
    private const val KEY_PERSIST_OUTPUT = "os_shell_runner_persist_output_v1"
    private const val KEY_TIMEOUT_SECONDS = "os_shell_runner_timeout_seconds_v1"
    private const val KEY_AUTO_FORMAT_OUTPUT = "os_shell_runner_auto_format_output_v1"
    private const val KEY_AUTO_SCROLL_OUTPUT = "os_shell_runner_auto_scroll_output_v1"
    private const val KEY_OUTPUT_LIMIT_CHARS = "os_shell_runner_output_limit_chars_v1"
    private const val KEY_OUTPUT_SAVE_MODE = "os_shell_runner_output_save_mode_v1"
    private const val KEY_DANGEROUS_COMMAND_CONFIRM = "os_shell_runner_dangerous_command_confirm_v1"
    private const val KEY_COMPLETION_TOAST = "os_shell_runner_completion_toast_v1"
    private const val KEY_STARTUP_BEHAVIOR = "os_shell_runner_startup_behavior_v1"
    private const val KEY_EXIT_CLEANUP_MODE = "os_shell_runner_exit_cleanup_mode_v1"
    private const val KEY_COPY_MODE = "os_shell_runner_copy_mode_v1"
    private const val KEY_SAVED_INPUT = "os_shell_runner_saved_input_v1"
    private const val KEY_SAVED_OUTPUT = "os_shell_runner_saved_output_v1"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadSettings(): OsShellRunnerSettings {
        val timeoutSeconds = normalizeTimeoutSeconds(
            store.decodeInt(KEY_TIMEOUT_SECONDS, shellRunnerDefaultTimeoutSeconds)
        )
        val outputLimitChars = normalizeOutputLimitChars(
            store.decodeInt(KEY_OUTPUT_LIMIT_CHARS, shellRunnerDefaultOutputLimitChars)
        )
        return OsShellRunnerSettings(
            persistInput = store.decodeBool(KEY_PERSIST_INPUT, false),
            persistOutput = store.decodeBool(KEY_PERSIST_OUTPUT, false),
            commandTimeoutSeconds = timeoutSeconds,
            autoFormatOutput = store.decodeBool(KEY_AUTO_FORMAT_OUTPUT, false),
            autoScrollOutput = store.decodeBool(KEY_AUTO_SCROLL_OUTPUT, true),
            outputLimitChars = outputLimitChars,
            outputSaveMode = decodeOutputSaveMode(
                store.decodeString(KEY_OUTPUT_SAVE_MODE).orEmpty()
            ),
            dangerousCommandConfirm = store.decodeBool(KEY_DANGEROUS_COMMAND_CONFIRM, true),
            completionToast = store.decodeBool(KEY_COMPLETION_TOAST, true),
            startupBehavior = decodeStartupBehavior(
                store.decodeString(KEY_STARTUP_BEHAVIOR).orEmpty()
            ),
            exitCleanupMode = decodeExitCleanupMode(
                store.decodeString(KEY_EXIT_CLEANUP_MODE).orEmpty()
            ),
            copyMode = decodeCopyMode(
                store.decodeString(KEY_COPY_MODE).orEmpty()
            )
        )
    }

    fun savePersistInput(enabled: Boolean) {
        store.encode(KEY_PERSIST_INPUT, enabled)
    }

    fun savePersistOutput(enabled: Boolean) {
        store.encode(KEY_PERSIST_OUTPUT, enabled)
    }

    fun saveTimeoutSeconds(seconds: Int) {
        store.encode(KEY_TIMEOUT_SECONDS, normalizeTimeoutSeconds(seconds))
    }

    fun saveAutoFormatOutput(enabled: Boolean) {
        store.encode(KEY_AUTO_FORMAT_OUTPUT, enabled)
    }

    fun saveAutoScrollOutput(enabled: Boolean) {
        store.encode(KEY_AUTO_SCROLL_OUTPUT, enabled)
    }

    fun saveOutputLimitChars(limit: Int) {
        store.encode(KEY_OUTPUT_LIMIT_CHARS, normalizeOutputLimitChars(limit))
    }

    fun saveOutputSaveMode(mode: OsShellRunnerOutputSaveMode) {
        store.encode(KEY_OUTPUT_SAVE_MODE, encodeOutputSaveMode(mode))
    }

    fun saveDangerousCommandConfirm(enabled: Boolean) {
        store.encode(KEY_DANGEROUS_COMMAND_CONFIRM, enabled)
    }

    fun saveCompletionToast(enabled: Boolean) {
        store.encode(KEY_COMPLETION_TOAST, enabled)
    }

    fun saveStartupBehavior(behavior: OsShellRunnerStartupBehavior) {
        store.encode(KEY_STARTUP_BEHAVIOR, encodeStartupBehavior(behavior))
    }

    fun saveExitCleanupMode(mode: OsShellRunnerExitCleanupMode) {
        store.encode(KEY_EXIT_CLEANUP_MODE, encodeExitCleanupMode(mode))
    }

    fun saveCopyMode(mode: OsShellRunnerCopyMode) {
        store.encode(KEY_COPY_MODE, encodeCopyMode(mode))
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
    private const val maxSavedOutputLength = 360_000

    private fun normalizeTimeoutSeconds(seconds: Int): Int {
        return seconds.coerceIn(5, 3_600)
    }

    private fun normalizeOutputLimitChars(limit: Int): Int {
        return limit.coerceIn(10_000, 500_000)
    }

    private fun encodeOutputSaveMode(mode: OsShellRunnerOutputSaveMode): String {
        return when (mode) {
            OsShellRunnerOutputSaveMode.FullHistory -> "full_history"
            OsShellRunnerOutputSaveMode.LatestOnly -> "latest_only"
        }
    }

    private fun decodeOutputSaveMode(raw: String): OsShellRunnerOutputSaveMode {
        return when (raw.trim().lowercase()) {
            "latest_only" -> OsShellRunnerOutputSaveMode.LatestOnly
            else -> OsShellRunnerOutputSaveMode.FullHistory
        }
    }

    private fun encodeStartupBehavior(behavior: OsShellRunnerStartupBehavior): String {
        return when (behavior) {
            OsShellRunnerStartupBehavior.FocusInput -> "focus_input"
            OsShellRunnerStartupBehavior.Silent -> "silent"
        }
    }

    private fun decodeStartupBehavior(raw: String): OsShellRunnerStartupBehavior {
        return when (raw.trim().lowercase()) {
            "focus_input" -> OsShellRunnerStartupBehavior.FocusInput
            else -> OsShellRunnerStartupBehavior.Silent
        }
    }

    private fun encodeExitCleanupMode(mode: OsShellRunnerExitCleanupMode): String {
        return when (mode) {
            OsShellRunnerExitCleanupMode.KeepAll -> "keep_all"
            OsShellRunnerExitCleanupMode.ClearInput -> "clear_input"
            OsShellRunnerExitCleanupMode.ClearOutput -> "clear_output"
        }
    }

    private fun decodeExitCleanupMode(raw: String): OsShellRunnerExitCleanupMode {
        return when (raw.trim().lowercase()) {
            "clear_input" -> OsShellRunnerExitCleanupMode.ClearInput
            "clear_output" -> OsShellRunnerExitCleanupMode.ClearOutput
            else -> OsShellRunnerExitCleanupMode.KeepAll
        }
    }

    private fun encodeCopyMode(mode: OsShellRunnerCopyMode): String {
        return when (mode) {
            OsShellRunnerCopyMode.FullHistory -> "full_history"
            OsShellRunnerCopyMode.LatestResult -> "latest_result"
        }
    }

    private fun decodeCopyMode(raw: String): OsShellRunnerCopyMode {
        return when (raw.trim().lowercase()) {
            "latest_result" -> OsShellRunnerCopyMode.LatestResult
            else -> OsShellRunnerCopyMode.FullHistory
        }
    }
}
