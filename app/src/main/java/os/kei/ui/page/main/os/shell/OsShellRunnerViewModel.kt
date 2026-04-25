package os.kei.ui.page.main.os.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import os.kei.ui.page.main.os.shell.state.OsShellRunnerOutputState

internal class OsShellRunnerViewModel : ViewModel() {
    private val repository = OsShellRunnerRepository()
    private var loadJob: Job? = null

    val persistentState: StateFlow<OsShellRunnerPersistentState> = repository.observePersistentState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = repository.observePersistentState().value
        )

    val chromePrefs: StateFlow<OsShellRunnerChromePrefs> = repository.observeChromePrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = repository.observeChromePrefs().value
        )

    fun loadPersistentState(
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        if (loadJob != null) return
        loadJob = viewModelScope.launch {
            repository.loadPersistentState(
                commandStoppedText = commandStoppedText,
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel
            )
        }
    }

    fun refreshChromePrefs() {
        viewModelScope.launch {
            repository.refreshChromePrefs()
        }
    }

    fun updateCommandInput(value: String) {
        repository.updateCommandInput(value)
    }

    fun updateOutputState(outputState: OsShellRunnerOutputState) {
        repository.updateOutputState(outputState)
    }

    fun replaceOutputMessage(message: String) {
        repository.replaceOutputMessage(message)
    }

    fun clearOutput() {
        repository.clearOutput()
    }

    suspend fun appendOutput(
        command: String,
        result: String,
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        repository.appendOutput(
            command = command,
            result = result,
            commandStoppedText = commandStoppedText,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel
        )
    }

    suspend fun formatOutput(
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        repository.formatOutput(
            commandStoppedText = commandStoppedText,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel
        )
    }

    fun updatePersistInput(enabled: Boolean) {
        launchRepositoryUpdate { setPersistInput(enabled) }
    }

    fun updatePersistOutput(enabled: Boolean) {
        launchRepositoryUpdate { setPersistOutput(enabled) }
    }

    fun updateTimeoutSeconds(seconds: Int) {
        launchRepositoryUpdate { setTimeoutSeconds(seconds) }
    }

    fun updateAutoFormatOutput(enabled: Boolean) {
        launchRepositoryUpdate { setAutoFormatOutput(enabled) }
    }

    fun updateAutoScrollOutput(enabled: Boolean) {
        launchRepositoryUpdate { setAutoScrollOutput(enabled) }
    }

    fun updateOutputLimitChars(
        limit: Int,
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        launchRepositoryUpdate {
            setOutputLimitChars(
                limit = limit,
                commandStoppedText = commandStoppedText,
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel
            )
        }
    }

    fun updateOutputSaveMode(
        mode: OsShellRunnerOutputSaveMode,
        commandStoppedText: String,
        outputResultLabel: String,
        outputTimeLabel: String
    ) {
        launchRepositoryUpdate {
            setOutputSaveMode(
                mode = mode,
                commandStoppedText = commandStoppedText,
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel
            )
        }
    }

    fun updateDangerousCommandConfirm(enabled: Boolean) {
        launchRepositoryUpdate { setDangerousCommandConfirm(enabled) }
    }

    fun updateCompletionToast(enabled: Boolean) {
        launchRepositoryUpdate { setCompletionToast(enabled) }
    }

    fun updateStartupBehavior(behavior: OsShellRunnerStartupBehavior) {
        launchRepositoryUpdate { setStartupBehavior(behavior) }
    }

    fun updateExitCleanupMode(mode: OsShellRunnerExitCleanupMode) {
        launchRepositoryUpdate { setExitCleanupMode(mode) }
    }

    fun updateCopyMode(mode: OsShellRunnerCopyMode) {
        launchRepositoryUpdate { setCopyMode(mode) }
    }

    fun persistInput(value: String) {
        viewModelScope.launch {
            repository.persistInput(value)
        }
    }

    fun persistOutput(value: String) {
        viewModelScope.launch {
            repository.persistOutput(value)
        }
    }

    fun clearSavedInput() {
        viewModelScope.launch {
            repository.clearSavedInput()
        }
    }

    fun clearSavedOutput() {
        viewModelScope.launch {
            repository.clearSavedOutput()
        }
    }

    suspend fun latestShellCardSubtitle(command: String): String {
        return repository.latestShellCardSubtitle(command)
    }

    suspend fun createShellCommandCard(
        command: String,
        title: String,
        subtitle: String,
        runOutput: String
    ): Boolean {
        return repository.createShellCommandCard(
            command = command,
            title = title,
            subtitle = subtitle,
            runOutput = runOutput
        )
    }

    private fun launchRepositoryUpdate(
        update: suspend OsShellRunnerRepository.() -> Unit
    ) {
        viewModelScope.launch {
            repository.update()
        }
    }
}
