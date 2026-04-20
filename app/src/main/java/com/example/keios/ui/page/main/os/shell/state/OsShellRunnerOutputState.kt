package com.example.keios.ui.page.main.os.shell.state

import com.example.keios.ui.page.main.os.shell.OsShellRunnerPrefsStore
import com.example.keios.ui.page.main.os.shell.ShellOutputDisplayEntry
import com.example.keios.ui.page.main.os.shell.parseShellOutputDisplayEntries
import com.example.keios.ui.page.main.os.shell.trimShellOutputEntries
import com.example.keios.ui.page.main.os.shell.util.buildShellOutputHistoryText
import com.example.keios.ui.page.main.os.shell.util.formatShellResultForReadability
import com.example.keios.ui.page.main.os.shell.util.trimShellOutputHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val shellOutputMaxChars = 120_000

internal data class OsShellRunnerOutputState(
    val outputText: String,
    val outputEntries: List<ShellOutputDisplayEntry>,
    val latestRunResultOutput: String
)

internal data class OsShellRunnerPersistSnapshot(
    val commandInput: String,
    val persistInputEnabled: Boolean,
    val persistOutputEnabled: Boolean,
    val outputState: OsShellRunnerOutputState
)

internal fun loadOsShellRunnerPersistSnapshot(
    commandStoppedText: String,
    outputResultLabel: String,
    outputTimeLabel: String
): OsShellRunnerPersistSnapshot {
    val persistSettings = OsShellRunnerPrefsStore.loadPersistSettings()
    val commandInput = if (persistSettings.persistInput) {
        OsShellRunnerPrefsStore.loadSavedInput()
    } else {
        ""
    }
    val outputText = if (persistSettings.persistOutput) {
        OsShellRunnerPrefsStore.loadSavedOutput()
    } else {
        ""
    }
    val outputEntries = parseShellOutputDisplayEntries(
        raw = outputText,
        stoppedOutputText = commandStoppedText,
        outputResultLabel = outputResultLabel,
        outputTimeLabel = outputTimeLabel
    )
    return OsShellRunnerPersistSnapshot(
        commandInput = commandInput,
        persistInputEnabled = persistSettings.persistInput,
        persistOutputEnabled = persistSettings.persistOutput,
        outputState = OsShellRunnerOutputState(
            outputText = outputText,
            outputEntries = outputEntries,
            latestRunResultOutput = outputEntries.lastOrNull()?.result.orEmpty().trim()
        )
    )
}

internal fun appendShellRunnerOutput(
    currentOutputText: String,
    currentOutputEntries: List<ShellOutputDisplayEntry>,
    command: String,
    result: String,
    commandStoppedText: String
): OsShellRunnerOutputState {
    val normalizedResult = result.trimEnd()
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val timeLabel = "[$timestamp]"
    val previousOutput = currentOutputText.trimEnd()
    val outputText = trimShellOutputHistory(
        raw = buildString {
            if (previousOutput.isNotBlank()) {
                append(previousOutput)
                appendLine()
                appendLine()
            }
            appendLine("$ $command")
            appendLine()
            appendLine(normalizedResult)
            appendLine()
            append(timeLabel)
        },
        maxChars = shellOutputMaxChars
    )
    val outputEntries = trimShellOutputEntries(
        entries = currentOutputEntries + ShellOutputDisplayEntry(
            command = command.trim(),
            result = normalizedResult,
            isStopped = normalizedResult.trim() == commandStoppedText.trim(),
            timeLabel = timeLabel
        ),
        maxChars = shellOutputMaxChars
    )
    return OsShellRunnerOutputState(
        outputText = outputText,
        outputEntries = outputEntries,
        latestRunResultOutput = normalizedResult
    )
}

internal fun formatShellRunnerOutput(
    outputText: String,
    outputEntries: List<ShellOutputDisplayEntry>,
    commandStoppedText: String,
    outputResultLabel: String,
    outputTimeLabel: String
): OsShellRunnerOutputState {
    val parsedEntries = if (outputEntries.isNotEmpty()) {
        outputEntries
    } else {
        parseShellOutputDisplayEntries(
            raw = outputText,
            stoppedOutputText = commandStoppedText,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel
        )
    }

    if (parsedEntries.isNotEmpty()) {
        val formattedEntries = parsedEntries.map { entry ->
            if (entry.isStopped) {
                entry
            } else {
                entry.copy(result = formatShellResultForReadability(entry.result))
            }
        }
        val trimmedEntries = trimShellOutputEntries(
            entries = formattedEntries,
            maxChars = shellOutputMaxChars
        )
        return OsShellRunnerOutputState(
            outputText = buildShellOutputHistoryText(
                entries = trimmedEntries,
                maxChars = shellOutputMaxChars
            ),
            outputEntries = trimmedEntries,
            latestRunResultOutput = trimmedEntries.lastOrNull()?.result.orEmpty().trim()
        )
    }

    val formattedText = formatShellResultForReadability(outputText)
    val reparsedEntries = parseShellOutputDisplayEntries(
        raw = formattedText,
        stoppedOutputText = commandStoppedText,
        outputResultLabel = outputResultLabel,
        outputTimeLabel = outputTimeLabel
    )
    return OsShellRunnerOutputState(
        outputText = formattedText,
        outputEntries = reparsedEntries,
        latestRunResultOutput = reparsedEntries.lastOrNull()?.result.orEmpty().trim()
    )
}

internal fun emptyShellRunnerOutputState(): OsShellRunnerOutputState {
    return OsShellRunnerOutputState(
        outputText = "",
        outputEntries = emptyList(),
        latestRunResultOutput = ""
    )
}
