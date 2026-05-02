package os.kei.ui.page.main.os.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val shellDisplayTimeLineRegex = Regex("""^\[\d{2}:\d{2}:\d{2}]$""")

internal data class ShellOutputDisplayEntry(
    val command: String,
    val result: String,
    val isStopped: Boolean,
    val timeLabel: String = ""
)

@Composable
internal fun ShellOutputGlassPanel(
    text: String,
    hint: String,
    entries: List<ShellOutputDisplayEntry>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val commandColor = if (isDark) {
        Color(0xFF7AB8FF)
    } else {
        Color(0xFF2563EB)
    }
    val successOutputColor = if (isDark) {
        Color(0xFF7EE7A8)
    } else {
        Color(0xFF15803D)
    }
    val stoppedOutputColor = if (isDark) {
        Color(0xFFFF9E9E)
    } else {
        Color(0xFFDC2626)
    }

    ShellGlassPanelSurface(
        modifier = modifier,
        minHeight = 160.dp
    ) {
        if (text.isBlank()) {
            Text(
                text = hint,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight
            )
        } else {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = text,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        entries.forEach { entry ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$ ${entry.command}",
                                    color = commandColor,
                                    fontSize = AppTypographyTokens.Body.fontSize,
                                    lineHeight = AppTypographyTokens.Body.lineHeight,
                                    maxLines = Int.MAX_VALUE,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    text = entry.result,
                                    color = if (entry.isStopped) stoppedOutputColor else successOutputColor,
                                    fontSize = AppTypographyTokens.Body.fontSize,
                                    lineHeight = AppTypographyTokens.Body.lineHeight,
                                    maxLines = Int.MAX_VALUE,
                                    overflow = TextOverflow.Clip
                                )
                                if (entry.timeLabel.isNotBlank()) {
                                    Text(
                                        text = entry.timeLabel,
                                        color = successOutputColor,
                                        fontSize = AppTypographyTokens.Body.fontSize,
                                        lineHeight = AppTypographyTokens.Body.lineHeight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun trimShellOutputEntries(
    entries: List<ShellOutputDisplayEntry>,
    maxChars: Int
): List<ShellOutputDisplayEntry> {
    if (entries.isEmpty()) return emptyList()
    var totalChars = entries.sumOf { entry ->
        entry.command.length + entry.result.length + entry.timeLabel.length + 8
    }
    if (totalChars <= maxChars) return entries
    val trimmed = entries.toMutableList()
    while (trimmed.isNotEmpty() && totalChars > maxChars) {
        val removed = trimmed.removeAt(0)
        totalChars -= removed.command.length + removed.result.length + removed.timeLabel.length + 8
    }
    return trimmed
}

internal fun parseShellOutputDisplayEntries(
    raw: String,
    stoppedOutputText: String,
    outputResultLabel: String,
    outputTimeLabel: String
): List<ShellOutputDisplayEntry> {
    val normalized = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()
    if (normalized.isBlank()) return emptyList()
    val lines = normalized.lines()
    val commandLineIndices = lines.indices.filter { index ->
        lines[index].startsWith("$ ")
    }
    if (commandLineIndices.isEmpty()) return emptyList()
    val stoppedMarker = stoppedOutputText.trim()
    return buildList {
        commandLineIndices.forEachIndexed { index, start ->
            val end = commandLineIndices.getOrNull(index + 1) ?: lines.size
            val command = lines[start].removePrefix("$").trim()
            if (command.isBlank()) return@forEachIndexed
            val outputLines = normalizeShellOutputBlockLines(
                lines = lines.subList(start + 1, end),
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel
            )
            val (resultLines, timeLabel) = splitShellOutputResultAndTime(outputLines)
            val result = resultLines.joinToString("\n").trimEnd()
            add(
                ShellOutputDisplayEntry(
                    command = command,
                    result = result,
                    isStopped = result.trim() == stoppedMarker,
                    timeLabel = timeLabel.orEmpty()
                )
            )
        }
    }
}

private fun splitShellOutputResultAndTime(lines: List<String>): Pair<List<String>, String?> {
    if (lines.isEmpty()) return emptyList<String>() to null
    val trimmed = lines.map { it.trimEnd() }.toMutableList()
    while (trimmed.isNotEmpty() && trimmed.last().isBlank()) {
        trimmed.removeLast()
    }
    val time = trimmed.lastOrNull()?.trim()?.takeIf { shellDisplayTimeLineRegex.matches(it) }
    if (time != null) {
        trimmed.removeLast()
    }
    while (trimmed.isNotEmpty() && trimmed.last().isBlank()) {
        trimmed.removeLast()
    }
    return trimmed to time
}

private fun normalizeShellOutputBlockLines(
    lines: List<String>,
    outputResultLabel: String,
    outputTimeLabel: String
): List<String> {
    if (lines.isEmpty()) return emptyList()
    var start = 0
    while (start < lines.size && lines[start].isBlank()) {
        start += 1
    }
    if (start < lines.size && lines[start].trim() == "$outputResultLabel:") {
        start += 1
    }
    while (start < lines.size && lines[start].isBlank()) {
        start += 1
    }
    var end = lines.size
    while (end > start && lines[end - 1].isBlank()) {
        end -= 1
    }
    if (end > start && lines[end - 1].trim().startsWith("$outputTimeLabel:")) {
        end -= 1
    }
    while (end > start && lines[end - 1].isBlank()) {
        end -= 1
    }
    if (start >= end) return emptyList()
    return lines.subList(start, end).map { it.trimEnd() }
}
