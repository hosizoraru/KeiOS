package os.kei.ui.page.main.os.shell.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
    outputText: String,
    onPersistInput: (String) -> Unit,
    onPersistOutput: (String) -> Unit
) {
    val currentCommandInput = rememberUpdatedState(commandInput)
    val currentOutputText = rememberUpdatedState(outputText)
    val currentPersistInput = rememberUpdatedState(onPersistInput)
    val currentPersistOutput = rememberUpdatedState(onPersistOutput)
    LaunchedEffect(persistInputEnabled) {
        if (!persistInputEnabled) return@LaunchedEffect
        snapshotFlow { currentCommandInput.value }
            .debounce(shellPersistDebounceMs)
            .collectLatest { input ->
                currentPersistInput.value(input)
            }
    }
    LaunchedEffect(persistOutputEnabled) {
        if (!persistOutputEnabled) return@LaunchedEffect
        snapshotFlow { currentOutputText.value }
            .debounce(shellPersistDebounceMs)
            .collectLatest { output ->
                currentPersistOutput.value(output)
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
