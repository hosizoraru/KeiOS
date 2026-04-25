package os.kei.ui.page.main.os.shell.state

import os.kei.ui.page.main.os.shell.OsShellRunnerOutputSaveMode
import os.kei.ui.page.main.os.shell.ShellOutputDisplayEntry
import os.kei.ui.page.main.os.shell.parseShellOutputDisplayEntries
import os.kei.ui.page.main.os.shell.trimShellOutputEntries
import os.kei.ui.page.main.os.shell.util.buildShellOutputHistoryText
import os.kei.ui.page.main.os.shell.util.formatShellResultForReadability
import os.kei.ui.page.main.os.shell.util.trimShellOutputHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val shellOutputDefaultMaxChars = 120_000

internal data class OsShellRunnerOutputState(
    val outputText: String,
    val outputEntries: List<ShellOutputDisplayEntry>,
    val latestRunResultOutput: String
)

internal fun appendShellRunnerOutput(
    currentOutputText: String,
    currentOutputEntries: List<ShellOutputDisplayEntry>,
    command: String,
    result: String,
    commandStoppedText: String,
    outputSaveMode: OsShellRunnerOutputSaveMode,
    maxChars: Int
): OsShellRunnerOutputState {
    val normalizedResult = result.trimEnd()
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val timeLabel = "[$timestamp]"
    val previousOutput = if (outputSaveMode == OsShellRunnerOutputSaveMode.LatestOnly) {
        ""
    } else {
        currentOutputText.trimEnd()
    }
    val previousEntries = if (outputSaveMode == OsShellRunnerOutputSaveMode.LatestOnly) {
        emptyList()
    } else {
        currentOutputEntries
    }
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
        maxChars = maxChars
    )
    val outputEntries = trimShellOutputEntries(
        entries = previousEntries + ShellOutputDisplayEntry(
            command = command.trim(),
            result = normalizedResult,
            isStopped = normalizedResult.trim() == commandStoppedText.trim(),
            timeLabel = timeLabel
        ),
        maxChars = maxChars
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
    outputTimeLabel: String,
    maxChars: Int
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
            maxChars = maxChars
        )
        return OsShellRunnerOutputState(
            outputText = buildShellOutputHistoryText(
                entries = trimmedEntries,
                maxChars = maxChars
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
    if (reparsedEntries.isNotEmpty()) {
        val trimmedEntries = trimShellOutputEntries(
            entries = reparsedEntries,
            maxChars = maxChars
        )
        return OsShellRunnerOutputState(
            outputText = buildShellOutputHistoryText(
                entries = trimmedEntries,
                maxChars = maxChars
            ),
            outputEntries = trimmedEntries,
            latestRunResultOutput = trimmedEntries.lastOrNull()?.result.orEmpty().trim()
        )
    }
    val trimmedText = trimShellOutputHistory(
        raw = formattedText,
        maxChars = maxChars
    )
    return OsShellRunnerOutputState(
        outputText = trimmedText,
        outputEntries = emptyList(),
        latestRunResultOutput = trimmedText.trim()
    )
}

internal fun normalizeShellRunnerOutputState(
    outputText: String,
    outputEntries: List<ShellOutputDisplayEntry>,
    commandStoppedText: String,
    outputResultLabel: String,
    outputTimeLabel: String,
    outputSaveMode: OsShellRunnerOutputSaveMode,
    maxChars: Int
): OsShellRunnerOutputState {
    val candidateEntries = if (outputEntries.isNotEmpty()) {
        outputEntries
    } else {
        parseShellOutputDisplayEntries(
            raw = outputText,
            stoppedOutputText = commandStoppedText,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel
        )
    }
    if (candidateEntries.isNotEmpty()) {
        val modeEntries = when (outputSaveMode) {
            OsShellRunnerOutputSaveMode.FullHistory -> candidateEntries
            OsShellRunnerOutputSaveMode.LatestOnly -> candidateEntries.takeLast(1)
        }
        val trimmedEntries = trimShellOutputEntries(
            entries = modeEntries,
            maxChars = maxChars
        )
        return OsShellRunnerOutputState(
            outputText = buildShellOutputHistoryText(
                entries = trimmedEntries,
                maxChars = maxChars
            ),
            outputEntries = trimmedEntries,
            latestRunResultOutput = trimmedEntries.lastOrNull()?.result.orEmpty().trim()
        )
    }
    val trimmedText = trimShellOutputHistory(
        raw = outputText,
        maxChars = maxChars
    )
    return OsShellRunnerOutputState(
        outputText = trimmedText,
        outputEntries = emptyList(),
        latestRunResultOutput = trimmedText.trim()
    )
}

internal fun emptyShellRunnerOutputState(): OsShellRunnerOutputState {
    return OsShellRunnerOutputState(
        outputText = "",
        outputEntries = emptyList(),
        latestRunResultOutput = ""
    )
}
