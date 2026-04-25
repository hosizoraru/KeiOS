package os.kei.ui.page.main.os.shell

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import os.kei.core.prefs.AppThemeMode
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.os.shell.state.OsShellRunnerOutputState
import os.kei.ui.page.main.os.shell.state.appendShellRunnerOutput
import os.kei.ui.page.main.os.shell.state.emptyShellRunnerOutputState
import os.kei.ui.page.main.os.shell.state.formatShellRunnerOutput
import os.kei.ui.page.main.os.shell.state.normalizeShellRunnerOutputState

internal data class OsShellRunnerChromePrefs(
    val appThemeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val liquidActionBarLayeredStyleEnabled: Boolean = true
)

internal data class OsShellRunnerPersistentState(
    val commandInput: String = "",
    val settings: OsShellRunnerSettings = OsShellRunnerSettings(),
    val outputState: OsShellRunnerOutputState = emptyShellRunnerOutputState(),
    val loaded: Boolean = false
)

internal class OsShellRunnerRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val persistentState = MutableStateFlow(OsShellRunnerPersistentState())
    private val chromePrefs = MutableStateFlow(OsShellRunnerChromePrefs())

    fun observePersistentState(): StateFlow<OsShellRunnerPersistentState> = persistentState.asStateFlow()

    fun observeChromePrefs(): StateFlow<OsShellRunnerChromePrefs> = chromePrefs.asStateFlow()

    suspend fun loadPersistentState(
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        val loaded = withContext(ioDispatcher) {
            val settings = OsShellRunnerPrefsStore.loadSettings()
            val commandInput = if (settings.persistInput) {
                OsShellRunnerPrefsStore.loadSavedInput()
            } else {
                ""
            }
            val savedOutputText = if (settings.persistOutput) {
                OsShellRunnerPrefsStore.loadSavedOutput()
            } else {
                ""
            }
            OsShellRunnerPersistentState(
                commandInput = commandInput,
                settings = settings,
                outputState = normalizeShellRunnerOutputState(
                    outputText = savedOutputText,
                    outputEntries = emptyList(),
                    commandStoppedText = commandStoppedText,
                    outputResultLabel = outputResultLabel,
                    outputTimeLabel = outputTimeLabel,
                    outputSaveMode = settings.outputSaveMode,
                    maxChars = settings.outputLimitChars
                ),
                loaded = true
            )
        }
        persistentState.value = loaded
    }

    suspend fun refreshChromePrefs() {
        val loaded = withContext(ioDispatcher) {
            OsShellRunnerChromePrefs(
                appThemeMode = UiPrefs.getAppThemeMode(),
                liquidActionBarLayeredStyleEnabled = UiPrefs.isLiquidActionBarLayeredStyleEnabled()
            )
        }
        chromePrefs.value = loaded
    }

    fun updateCommandInput(value: String) {
        persistentState.update { state -> state.copy(commandInput = value) }
    }

    fun updateOutputState(outputState: OsShellRunnerOutputState) {
        persistentState.update { state -> state.copy(outputState = outputState) }
    }

    fun replaceOutputMessage(message: String) {
        persistentState.update { state ->
            state.copy(
                outputState = OsShellRunnerOutputState(
                    outputText = message,
                    outputEntries = emptyList(),
                    latestRunResultOutput = message
                )
            )
        }
    }

    fun clearOutput() {
        persistentState.update { state -> state.copy(outputState = emptyShellRunnerOutputState()) }
    }

    suspend fun appendOutput(
        command: String,
        result: String,
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        val state = persistentState.value
        val next = withContext(ioDispatcher) {
            var outputState = appendShellRunnerOutput(
                currentOutputText = state.outputState.outputText,
                currentOutputEntries = state.outputState.outputEntries,
                command = command,
                result = result,
                commandStoppedText = commandStoppedText,
                outputSaveMode = state.settings.outputSaveMode,
                maxChars = state.settings.outputLimitChars
            )
            if (state.settings.autoFormatOutput) {
                outputState = formatShellRunnerOutput(
                    outputText = outputState.outputText,
                    outputEntries = outputState.outputEntries,
                    commandStoppedText = commandStoppedText,
                    outputResultLabel = outputResultLabel,
                    outputTimeLabel = outputTimeLabel,
                    maxChars = state.settings.outputLimitChars
                )
            }
            outputState
        }
        persistentState.update { current -> current.copy(outputState = next) }
    }

    suspend fun formatOutput(
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        val state = persistentState.value
        val formatted = withContext(ioDispatcher) {
            formatShellRunnerOutput(
                outputText = state.outputState.outputText,
                outputEntries = state.outputState.outputEntries,
                commandStoppedText = commandStoppedText,
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel,
                maxChars = state.settings.outputLimitChars
            )
        }
        persistentState.update { current -> current.copy(outputState = formatted) }
    }

    suspend fun setPersistInput(enabled: Boolean) {
        updateSettingsAndPersist({ copy(persistInput = enabled) }) {
            OsShellRunnerPrefsStore.savePersistInput(enabled)
            if (!enabled) OsShellRunnerPrefsStore.clearSavedInput()
        }
    }

    suspend fun setPersistOutput(enabled: Boolean) {
        updateSettingsAndPersist({ copy(persistOutput = enabled) }) {
            OsShellRunnerPrefsStore.savePersistOutput(enabled)
            if (!enabled) OsShellRunnerPrefsStore.clearSavedOutput()
        }
    }

    suspend fun setTimeoutSeconds(seconds: Int) {
        updateSettingsAndPersist({ copy(commandTimeoutSeconds = seconds) }) {
            OsShellRunnerPrefsStore.saveTimeoutSeconds(seconds)
        }
    }

    suspend fun setAutoFormatOutput(enabled: Boolean) {
        updateSettingsAndPersist({ copy(autoFormatOutput = enabled) }) {
            OsShellRunnerPrefsStore.saveAutoFormatOutput(enabled)
        }
    }

    suspend fun setAutoScrollOutput(enabled: Boolean) {
        updateSettingsAndPersist({ copy(autoScrollOutput = enabled) }) {
            OsShellRunnerPrefsStore.saveAutoScrollOutput(enabled)
        }
    }

    suspend fun setOutputLimitChars(
        limit: Int,
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        updateSettingsAndPersist(
            reducer = { copy(outputLimitChars = limit) },
            beforePersist = {
                normalizeOutputState(
                    commandStoppedText = commandStoppedText,
                    outputResultLabel = outputResultLabel,
                    outputTimeLabel = outputTimeLabel
                )
            },
            persist = { OsShellRunnerPrefsStore.saveOutputLimitChars(limit) }
        )
    }

    suspend fun setOutputSaveMode(
        mode: OsShellRunnerOutputSaveMode,
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        updateSettingsAndPersist(
            reducer = { copy(outputSaveMode = mode) },
            beforePersist = {
                normalizeOutputState(
                    commandStoppedText = commandStoppedText,
                    outputResultLabel = outputResultLabel,
                    outputTimeLabel = outputTimeLabel
                )
            },
            persist = { OsShellRunnerPrefsStore.saveOutputSaveMode(mode) }
        )
    }

    suspend fun setDangerousCommandConfirm(enabled: Boolean) {
        updateSettingsAndPersist({ copy(dangerousCommandConfirm = enabled) }) {
            OsShellRunnerPrefsStore.saveDangerousCommandConfirm(enabled)
        }
    }

    suspend fun setCompletionToast(enabled: Boolean) {
        updateSettingsAndPersist({ copy(completionToast = enabled) }) {
            OsShellRunnerPrefsStore.saveCompletionToast(enabled)
        }
    }

    suspend fun setStartupBehavior(behavior: OsShellRunnerStartupBehavior) {
        updateSettingsAndPersist({ copy(startupBehavior = behavior) }) {
            OsShellRunnerPrefsStore.saveStartupBehavior(behavior)
        }
    }

    suspend fun setExitCleanupMode(mode: OsShellRunnerExitCleanupMode) {
        updateSettingsAndPersist({ copy(exitCleanupMode = mode) }) {
            OsShellRunnerPrefsStore.saveExitCleanupMode(mode)
        }
    }

    suspend fun setCopyMode(mode: OsShellRunnerCopyMode) {
        updateSettingsAndPersist({ copy(copyMode = mode) }) {
            OsShellRunnerPrefsStore.saveCopyMode(mode)
        }
    }

    suspend fun persistInput(value: String) {
        withContext(ioDispatcher) {
            OsShellRunnerPrefsStore.saveInput(value)
        }
    }

    suspend fun persistOutput(value: String) {
        withContext(ioDispatcher) {
            OsShellRunnerPrefsStore.saveOutput(value)
        }
    }

    suspend fun clearSavedInput() {
        withContext(ioDispatcher) {
            OsShellRunnerPrefsStore.clearSavedInput()
        }
    }

    suspend fun clearSavedOutput() {
        withContext(ioDispatcher) {
            OsShellRunnerPrefsStore.clearSavedOutput()
        }
    }

    suspend fun latestShellCardSubtitle(command: String): String {
        return withContext(ioDispatcher) {
            OsShellCommandCardStore.findLatestByCommand(command)?.subtitle.orEmpty()
        }
    }

    suspend fun createShellCommandCard(
        command: String,
        title: String,
        subtitle: String,
        runOutput: String
    ): Boolean {
        return withContext(ioDispatcher) {
            OsShellCommandCardStore.createCard(
                command = command,
                title = title,
                subtitle = subtitle,
                runOutput = runOutput
            ) != null
        }
    }

    private suspend fun updateSettingsAndPersist(
        reducer: OsShellRunnerSettings.() -> OsShellRunnerSettings,
        beforePersist: suspend () -> Unit = {},
        persist: () -> Unit
    ) {
        persistentState.update { state ->
            state.copy(settings = state.settings.reducer())
        }
        beforePersist()
        withContext(ioDispatcher) {
            persist()
        }
    }

    private suspend fun normalizeOutputState(
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        val state = persistentState.value
        val normalized = withContext(ioDispatcher) {
            normalizeShellRunnerOutputState(
                outputText = state.outputState.outputText,
                outputEntries = state.outputState.outputEntries,
                commandStoppedText = commandStoppedText,
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel,
                outputSaveMode = state.settings.outputSaveMode,
                maxChars = state.settings.outputLimitChars
            )
        }
        persistentState.update { current ->
            current.copy(outputState = normalized)
        }
    }
}
