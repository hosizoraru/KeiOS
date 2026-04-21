package com.example.keios.ui.page.main.os.shell.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.example.keios.ui.page.main.os.shell.OsShellRunnerPrefsStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

private const val shellPersistDebounceMs = 220L

@Composable
@OptIn(FlowPreview::class)
internal fun BindOsShellRunnerPersistEffects(
    persistInputEnabled: Boolean,
    persistOutputEnabled: Boolean,
    commandInput: String,
    outputText: String
) {
    LaunchedEffect(persistInputEnabled) {
        OsShellRunnerPrefsStore.savePersistInput(persistInputEnabled)
        if (!persistInputEnabled) {
            OsShellRunnerPrefsStore.clearSavedInput()
        }
    }
    LaunchedEffect(persistOutputEnabled) {
        OsShellRunnerPrefsStore.savePersistOutput(persistOutputEnabled)
        if (!persistOutputEnabled) {
            OsShellRunnerPrefsStore.clearSavedOutput()
        }
    }
    LaunchedEffect(persistInputEnabled) {
        if (!persistInputEnabled) return@LaunchedEffect
        snapshotFlow { commandInput }
            .debounce(shellPersistDebounceMs)
            .collectLatest { input ->
                OsShellRunnerPrefsStore.saveInput(input)
            }
    }
    LaunchedEffect(persistOutputEnabled) {
        if (!persistOutputEnabled) return@LaunchedEffect
        snapshotFlow { outputText }
            .debounce(shellPersistDebounceMs)
            .collectLatest { output ->
                OsShellRunnerPrefsStore.saveOutput(output)
            }
    }
}

@Composable
internal fun BindOsShellRunnerAutoScrollEffect(
    outputText: String,
    outputScrollState: ScrollState,
    enabled: Boolean
) {
    LaunchedEffect(outputText, enabled) {
        if (enabled && outputText.isNotBlank()) {
            outputScrollState.scrollTo(outputScrollState.maxValue)
        }
    }
}
