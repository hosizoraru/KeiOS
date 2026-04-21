package com.example.keios.ui.page.main.os.shell.page

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.keios.R
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.os.osLucideClearAllIcon
import com.example.keios.ui.page.main.os.osLucideSettingsIcon
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shell.OsShellRunnerCopyMode
import com.example.keios.ui.page.main.os.shell.OsShellRunnerExitCleanupMode
import com.example.keios.ui.page.main.os.shell.OsShellRunnerOutputSaveMode
import com.example.keios.ui.page.main.os.shell.OsShellRunnerPrefsStore
import com.example.keios.ui.page.main.os.shell.OsShellRunnerStartupBehavior
import com.example.keios.ui.page.main.os.shell.OsShellSettingsSheet
import com.example.keios.ui.page.main.os.shell.component.OsShellRunnerInputCard
import com.example.keios.ui.page.main.os.shell.component.OsShellRunnerOutputCard
import com.example.keios.ui.page.main.os.shell.component.OsShellRunnerSaveSheet
import com.example.keios.ui.page.main.os.shell.state.BindOsShellRunnerAutoScrollEffect
import com.example.keios.ui.page.main.os.shell.state.BindOsShellRunnerPersistEffects
import com.example.keios.ui.page.main.os.shell.state.OsShellRunnerOutputState
import com.example.keios.ui.page.main.os.shell.state.appendShellRunnerOutput
import com.example.keios.ui.page.main.os.shell.state.emptyShellRunnerOutputState
import com.example.keios.ui.page.main.os.shell.state.formatShellRunnerOutput
import com.example.keios.ui.page.main.os.shell.state.loadOsShellRunnerPersistSnapshot
import com.example.keios.ui.page.main.os.shell.state.normalizeShellRunnerOutputState
import com.example.keios.ui.page.main.os.shell.state.rememberOsShellRunnerTextBundle
import com.example.keios.ui.page.main.widget.chrome.AppChromeTokens
import com.example.keios.ui.page.main.widget.chrome.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.chrome.AppPageScaffold
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private val dangerousShellPatterns = listOf(
    Regex("""(^|\s)rm(\s+-[^\n]*)?\s+/(?!sdcard)""", RegexOption.IGNORE_CASE),
    Regex("""(^|\s)pm\s+uninstall(\s|$)""", RegexOption.IGNORE_CASE),
    Regex("""(^|\s)settings\s+put\s+global(\s|$)""", RegexOption.IGNORE_CASE),
    Regex("""(^|\s)settings\s+delete\s+(system|secure|global)(\s|$)""", RegexOption.IGNORE_CASE),
    Regex("""(^|\s)setprop(\s|$)""", RegexOption.IGNORE_CASE),
    Regex("""(^|\s)reboot(\s|$)""", RegexOption.IGNORE_CASE),
    Regex("""(^|\s)am\s+force-stop(\s|$)""", RegexOption.IGNORE_CASE)
)

private fun isPotentiallyDangerousShellCommand(command: String): Boolean {
    val normalized = command.trim()
    if (normalized.isBlank()) return false
    return dangerousShellPatterns.any { regex -> regex.containsMatchIn(normalized) }
}

@Composable
fun OsShellRunnerPage(
    canRunShellCommand: Boolean,
    onRequestShizukuPermission: () -> Unit,
    onRunShellCommand: suspend (String, Long) -> String?,
    onSaveShellCommand: (String, String, String, String) -> Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageListState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val textBundle = rememberOsShellRunnerTextBundle()
    val isDark = isSystemInDarkTheme()
    val shellCommandAccentColor = if (isDark) Color(0xFF7AB8FF) else Color(0xFF2563EB)
    val shellSuccessAccentColor = if (isDark) Color(0xFF7EE7A8) else Color(0xFF15803D)
    val shellStoppedAccentColor = if (isDark) Color(0xFFFF9E9E) else Color(0xFFDC2626)
    val outputScrollState = rememberScrollState()

    var liquidActionBarLayeredStyleEnabled by remember {
        mutableStateOf(UiPrefs.isLiquidActionBarLayeredStyleEnabled())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                liquidActionBarLayeredStyleEnabled = UiPrefs.isLiquidActionBarLayeredStyleEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val topBarBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val persistSnapshot = remember(
        textBundle.commandStoppedText,
        textBundle.outputResultLabel,
        textBundle.outputTimeLabel
    ) {
        loadOsShellRunnerPersistSnapshot(
            commandStoppedText = textBundle.commandStoppedText,
            outputResultLabel = textBundle.outputResultLabel,
            outputTimeLabel = textBundle.outputTimeLabel
        )
    }

    var commandInput by rememberSaveable { mutableStateOf(persistSnapshot.commandInput) }
    var outputText by rememberSaveable { mutableStateOf(persistSnapshot.outputState.outputText) }
    var outputEntries by remember { mutableStateOf(persistSnapshot.outputState.outputEntries) }
    var latestRunResultOutput by rememberSaveable {
        mutableStateOf(persistSnapshot.outputState.latestRunResultOutput)
    }
    var settings by remember { mutableStateOf(persistSnapshot.settings) }
    var startupFocusRequestToken by rememberSaveable {
        mutableIntStateOf(
            if (persistSnapshot.settings.startupBehavior == OsShellRunnerStartupBehavior.FocusInput) {
                1
            } else {
                0
            }
        )
    }

    var runningCommand by remember { mutableStateOf(false) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var suppressStopOutputAppend by remember { mutableStateOf(false) }
    var showSaveSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showDangerousCommandConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingDangerousCommand by rememberSaveable { mutableStateOf("") }
    var saveTitleInput by rememberSaveable { mutableStateOf("") }
    var saveSubtitleInput by rememberSaveable { mutableStateOf("") }

    val latestOutputEntry = remember(outputEntries) { outputEntries.lastOrNull() }

    fun applyOutputState(next: OsShellRunnerOutputState) {
        outputText = next.outputText
        outputEntries = next.outputEntries
        latestRunResultOutput = next.latestRunResultOutput
    }

    fun normalizeOutputStateForSettings() {
        applyOutputState(
            normalizeShellRunnerOutputState(
                outputText = outputText,
                outputEntries = outputEntries,
                commandStoppedText = textBundle.commandStoppedText,
                outputResultLabel = textBundle.outputResultLabel,
                outputTimeLabel = textBundle.outputTimeLabel,
                outputSaveMode = settings.outputSaveMode,
                maxChars = settings.outputLimitChars
            )
        )
    }

    fun appendOutput(command: String, result: String) {
        var next = appendShellRunnerOutput(
            currentOutputText = outputText,
            currentOutputEntries = outputEntries,
            command = command,
            result = result,
            commandStoppedText = textBundle.commandStoppedText,
            outputSaveMode = settings.outputSaveMode,
            maxChars = settings.outputLimitChars
        )
        if (settings.autoFormatOutput) {
            next = formatShellRunnerOutput(
                outputText = next.outputText,
                outputEntries = next.outputEntries,
                commandStoppedText = textBundle.commandStoppedText,
                outputResultLabel = textBundle.outputResultLabel,
                outputTimeLabel = textBundle.outputTimeLabel,
                maxChars = settings.outputLimitChars
            )
        }
        applyOutputState(next)
    }

    fun stopCommand(showStoppedOutput: Boolean = true) {
        val job = runningJob ?: return
        if (!showStoppedOutput) {
            suppressStopOutputAppend = true
        }
        job.cancel(CancellationException("user-stop"))
    }

    fun executeCommand(command: String) {
        if (runningCommand) return
        val timeoutMs = settings.commandTimeoutSeconds.coerceAtLeast(5) * 1_000L
        val job = scope.launch {
            runningCommand = true
            try {
                val output = runCatching { onRunShellCommand(command, timeoutMs) }
                    .getOrElse { throwable ->
                        if (throwable is CancellationException) throw throwable
                        throwable.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: throwable.javaClass.simpleName
                    }
                    ?.takeIf { it.isNotBlank() }
                    ?: textBundle.noOutputText
                appendOutput(command, output)
                if (settings.completionToast) {
                    Toast.makeText(context, textBundle.commandCompletedToast, Toast.LENGTH_SHORT).show()
                }
            } catch (_: CancellationException) {
                if (suppressStopOutputAppend) {
                    suppressStopOutputAppend = false
                } else {
                    appendOutput(command, textBundle.commandStoppedText)
                    if (settings.completionToast) {
                        Toast.makeText(context, textBundle.commandStoppedText, Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                runningCommand = false
                runningJob = null
            }
        }
        runningJob = job
    }

    fun runCommand() {
        if (runningCommand) return
        val command = commandInput.trim()
        if (command.isBlank()) {
            outputText = textBundle.emptyCommandText
            return
        }
        if (!canRunShellCommand) {
            onRequestShizukuPermission()
            outputText = textBundle.missingPermissionText
            return
        }
        if (settings.dangerousCommandConfirm && isPotentiallyDangerousShellCommand(command)) {
            pendingDangerousCommand = command
            showDangerousCommandConfirm = true
            return
        }
        executeCommand(command)
    }

    fun openSaveCommandSheet() {
        val command = commandInput.trim()
        if (command.isBlank()) {
            Toast.makeText(context, textBundle.commandSaveEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val currentCard = OsShellCommandCardStore.findLatestByCommand(command)
        saveTitleInput = ""
        saveSubtitleInput = currentCard?.subtitle.orEmpty()
        showSaveSheet = true
    }

    fun saveCommandToCard() {
        val command = commandInput.trim()
        if (command.isBlank()) {
            Toast.makeText(context, textBundle.commandSaveEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val title = saveTitleInput.trim()
        if (title.isBlank()) {
            Toast.makeText(context, textBundle.saveSheetTitleRequiredToast, Toast.LENGTH_SHORT).show()
            return
        }
        val subtitle = saveSubtitleInput.trim()
        val saved = onSaveShellCommand(command, title, subtitle, latestRunResultOutput)
        if (saved) {
            showSaveSheet = false
            Toast.makeText(context, textBundle.commandSavedToast, Toast.LENGTH_SHORT).show()
        }
    }

    fun copyOutput() {
        val preferred = when (settings.copyMode) {
            OsShellRunnerCopyMode.FullHistory -> outputText.trim()
            OsShellRunnerCopyMode.LatestResult -> latestOutputEntry?.result.orEmpty().trim()
        }
        val output = preferred.ifBlank {
            if (settings.copyMode == OsShellRunnerCopyMode.LatestResult) {
                latestRunResultOutput.trim().ifBlank { outputText.trim() }
            } else {
                outputText.trim()
            }
        }
        if (output.isBlank()) {
            Toast.makeText(context, textBundle.outputCopyEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("shell_output", output))
        Toast.makeText(context, textBundle.outputCopiedToast, Toast.LENGTH_SHORT).show()
    }

    fun formatOutput() {
        val output = outputText.trim()
        if (output.isBlank()) {
            Toast.makeText(context, textBundle.outputFormatEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        applyOutputState(
            formatShellRunnerOutput(
                outputText = outputText,
                outputEntries = outputEntries,
                commandStoppedText = textBundle.commandStoppedText,
                outputResultLabel = textBundle.outputResultLabel,
                outputTimeLabel = textBundle.outputTimeLabel,
                maxChars = settings.outputLimitChars
            )
        )
        Toast.makeText(context, textBundle.outputFormattedToast, Toast.LENGTH_SHORT).show()
    }

    fun clearOutput() {
        applyOutputState(emptyShellRunnerOutputState())
    }

    fun clearAllContent() {
        stopCommand(showStoppedOutput = false)
        commandInput = ""
        clearOutput()
        Toast.makeText(context, textBundle.clearAllToast, Toast.LENGTH_SHORT).show()
    }

    fun requestClose() {
        stopCommand(showStoppedOutput = false)
        when (settings.exitCleanupMode) {
            OsShellRunnerExitCleanupMode.KeepAll -> Unit
            OsShellRunnerExitCleanupMode.ClearInput -> {
                commandInput = ""
                OsShellRunnerPrefsStore.clearSavedInput()
            }
            OsShellRunnerExitCleanupMode.ClearOutput -> {
                clearOutput()
                OsShellRunnerPrefsStore.clearSavedOutput()
            }
        }
        onClose()
    }

    BackHandler(enabled = showSaveSheet) { showSaveSheet = false }
    BackHandler(enabled = !showSaveSheet && showSettingsSheet) { showSettingsSheet = false }
    BackHandler(enabled = !showSaveSheet && !showSettingsSheet && showDangerousCommandConfirm) {
        showDangerousCommandConfirm = false
        pendingDangerousCommand = ""
    }
    BackHandler(
        enabled = !showSaveSheet && !showSettingsSheet && !showDangerousCommandConfirm,
        onBack = { requestClose() }
    )

    val clearAllIcon = osLucideClearAllIcon()
    val settingsIcon = osLucideSettingsIcon()
    val actionItems = remember(textBundle.clearAllActionDescription, textBundle.settingsActionDescription) {
        listOf(
            LiquidActionItem(
                icon = clearAllIcon,
                contentDescription = textBundle.clearAllActionDescription,
                onClick = { clearAllContent() }
            ),
            LiquidActionItem(
                icon = settingsIcon,
                contentDescription = textBundle.settingsActionDescription,
                onClick = { showSettingsSheet = true }
            )
        )
    }

    BindOsShellRunnerPersistEffects(
        persistInputEnabled = settings.persistInput,
        persistOutputEnabled = settings.persistOutput,
        commandInput = commandInput,
        outputText = outputText
    )
    BindOsShellRunnerAutoScrollEffect(
        outputText = outputText,
        outputScrollState = outputScrollState,
        enabled = settings.autoScrollOutput
    )

    AppPageScaffold(
        title = textBundle.shellPageTitle,
        largeTitle = textBundle.shellPageTitle,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            Icon(
                imageVector = appLucideBackIcon(),
                contentDescription = stringResource(R.string.common_close),
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable { requestClose() }
            )
        },
        actions = {
            LiquidActionBar(
                backdrop = topBarBackdrop,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                items = actionItems
            )
        }
    ) { innerPadding ->
        AppPageLazyColumn(
            innerPadding = innerPadding,
            state = pageListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topExtra = 0.dp,
            sectionSpacing = AppChromeTokens.pageSectionGap
        ) {
            item(key = "shell_input_card") {
                OsShellRunnerInputCard(
                    inputTitle = textBundle.inputTitle,
                    inputHint = textBundle.inputHint,
                    commandInput = commandInput,
                    onCommandInputChange = { commandInput = it },
                    runningCommand = runningCommand,
                    runActionDescription = textBundle.runActionDescription,
                    stopActionDescription = textBundle.stopActionDescription,
                    saveCommandActionDescription = textBundle.saveCommandActionDescription,
                    focusRequestToken = startupFocusRequestToken,
                    onRunCommand = { runCommand() },
                    onStopCommand = { stopCommand() },
                    onOpenSaveCommandSheet = { openSaveCommandSheet() }
                )
            }
            item(key = "shell_output_card") {
                OsShellRunnerOutputCard(
                    outputTitle = textBundle.outputTitle,
                    outputHint = textBundle.outputHint,
                    outputText = outputText,
                    outputEntries = outputEntries,
                    outputScrollState = outputScrollState,
                    formatOutputActionDescription = textBundle.formatOutputActionDescription,
                    copyOutputActionDescription = textBundle.copyOutputActionDescription,
                    clearOutputActionDescription = textBundle.clearOutputActionDescription,
                    onFormatOutput = { formatOutput() },
                    onCopyOutput = { copyOutput() },
                    onClearOutput = { clearOutput() }
                )
            }
        }
    }

    OsShellRunnerSaveSheet(
        show = showSaveSheet,
        title = textBundle.saveSheetTitle,
        commandInput = commandInput,
        latestOutputEntry = latestOutputEntry,
        saveSheetCommandLabel = textBundle.saveSheetCommandLabel,
        saveSheetFieldTitle = textBundle.saveSheetFieldTitle,
        saveSheetFieldSubtitle = textBundle.saveSheetFieldSubtitle,
        saveSheetTitleHint = textBundle.saveSheetTitleHint,
        saveSheetSubtitleHint = textBundle.saveSheetSubtitleHint,
        saveSheetTimePlaceholder = textBundle.saveSheetTimePlaceholder,
        saveTitleInput = saveTitleInput,
        onSaveTitleInputChange = { saveTitleInput = it },
        saveSubtitleInput = saveSubtitleInput,
        onSaveSubtitleInputChange = { saveSubtitleInput = it },
        shellCommandAccentColor = shellCommandAccentColor,
        shellSuccessAccentColor = shellSuccessAccentColor,
        shellStoppedAccentColor = shellStoppedAccentColor,
        onDismissRequest = { showSaveSheet = false },
        onConfirm = { saveCommandToCard() }
    )

    OsShellSettingsSheet(
        show = showSettingsSheet,
        onDismissRequest = { showSettingsSheet = false },
        settings = settings,
        onPersistInputEnabledChange = { checked ->
            settings = settings.copy(persistInput = checked)
            OsShellRunnerPrefsStore.savePersistInput(checked)
        },
        onPersistOutputEnabledChange = { checked ->
            settings = settings.copy(persistOutput = checked)
            OsShellRunnerPrefsStore.savePersistOutput(checked)
        },
        onTimeoutSecondsChange = { seconds ->
            settings = settings.copy(commandTimeoutSeconds = seconds)
            OsShellRunnerPrefsStore.saveTimeoutSeconds(seconds)
        },
        onAutoFormatOutputChange = { checked ->
            settings = settings.copy(autoFormatOutput = checked)
            OsShellRunnerPrefsStore.saveAutoFormatOutput(checked)
        },
        onAutoScrollOutputChange = { checked ->
            settings = settings.copy(autoScrollOutput = checked)
            OsShellRunnerPrefsStore.saveAutoScrollOutput(checked)
        },
        onOutputLimitCharsChange = { limit ->
            settings = settings.copy(outputLimitChars = limit)
            OsShellRunnerPrefsStore.saveOutputLimitChars(limit)
            normalizeOutputStateForSettings()
        },
        onOutputSaveModeChange = { mode ->
            settings = settings.copy(outputSaveMode = mode)
            OsShellRunnerPrefsStore.saveOutputSaveMode(mode)
            normalizeOutputStateForSettings()
        },
        onDangerousCommandConfirmChange = { checked ->
            settings = settings.copy(dangerousCommandConfirm = checked)
            OsShellRunnerPrefsStore.saveDangerousCommandConfirm(checked)
        },
        onCompletionToastChange = { checked ->
            settings = settings.copy(completionToast = checked)
            OsShellRunnerPrefsStore.saveCompletionToast(checked)
        },
        onStartupBehaviorChange = { behavior ->
            settings = settings.copy(startupBehavior = behavior)
            OsShellRunnerPrefsStore.saveStartupBehavior(behavior)
            if (behavior == OsShellRunnerStartupBehavior.FocusInput) {
                startupFocusRequestToken += 1
            }
        },
        onExitCleanupModeChange = { mode ->
            settings = settings.copy(exitCleanupMode = mode)
            OsShellRunnerPrefsStore.saveExitCleanupMode(mode)
        },
        onCopyModeChange = { mode ->
            settings = settings.copy(copyMode = mode)
            OsShellRunnerPrefsStore.saveCopyMode(mode)
        }
    )

    val dangerousCommandPreview = remember(pendingDangerousCommand) {
        pendingDangerousCommand.trim().replace('\n', ' ').take(120)
    }
    OsShellDangerousCommandConfirmDialog(
        show = showDangerousCommandConfirm,
        title = textBundle.dangerousCommandDialogTitle,
        summary = context.getString(
            R.string.os_shell_dangerous_command_dialog_summary,
            dangerousCommandPreview.ifBlank { "-" }
        ),
        confirmText = textBundle.dangerousCommandConfirmText,
        onDismissRequest = {
            showDangerousCommandConfirm = false
            pendingDangerousCommand = ""
        },
        onConfirm = {
            val command = pendingDangerousCommand.trim()
            showDangerousCommandConfirm = false
            pendingDangerousCommand = ""
            if (command.isNotBlank()) {
                executeCommand(command)
            }
        }
    )
}

@Composable
private fun OsShellDangerousCommandConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    confirmText: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    WindowDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_cancel),
                onClick = onDismissRequest
            )
            TextButton(
                modifier = Modifier.weight(1f),
                text = confirmText,
                colors = ButtonDefaults.textButtonColors(
                    color = MiuixTheme.colorScheme.error,
                    textColor = MiuixTheme.colorScheme.onError
                ),
                onClick = onConfirm
            )
        }
    }
}
