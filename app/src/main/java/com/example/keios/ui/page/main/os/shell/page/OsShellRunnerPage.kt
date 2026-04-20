package com.example.keios.ui.page.main.os.shell.page

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OsShellRunnerPage(
    canRunShellCommand: Boolean,
    onRequestShizukuPermission: () -> Unit,
    onRunShellCommand: suspend (String) -> String?,
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
    var runningCommand by remember { mutableStateOf(false) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var suppressStopOutputAppend by remember { mutableStateOf(false) }
    var showSaveSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var saveTitleInput by rememberSaveable { mutableStateOf("") }
    var saveSubtitleInput by rememberSaveable { mutableStateOf("") }
    var persistInputEnabled by rememberSaveable { mutableStateOf(persistSnapshot.persistInputEnabled) }
    var persistOutputEnabled by rememberSaveable { mutableStateOf(persistSnapshot.persistOutputEnabled) }

    val latestOutputEntry = remember(outputEntries) { outputEntries.lastOrNull() }

    BackHandler(enabled = showSaveSheet) { showSaveSheet = false }
    BackHandler(enabled = !showSaveSheet && showSettingsSheet) { showSettingsSheet = false }
    BackHandler(enabled = !showSaveSheet && !showSettingsSheet, onBack = onClose)

    fun applyOutputState(next: OsShellRunnerOutputState) {
        outputText = next.outputText
        outputEntries = next.outputEntries
        latestRunResultOutput = next.latestRunResultOutput
    }

    fun appendOutput(command: String, result: String) {
        applyOutputState(
            appendShellRunnerOutput(
                currentOutputText = outputText,
                currentOutputEntries = outputEntries,
                command = command,
                result = result,
                commandStoppedText = textBundle.commandStoppedText
            )
        )
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
        val job = scope.launch {
            runningCommand = true
            try {
                val output = runCatching { onRunShellCommand(command) }
                    .getOrElse { throwable ->
                        if (throwable is CancellationException) throw throwable
                        throwable.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: throwable.javaClass.simpleName
                    }
                    ?.takeIf { it.isNotBlank() }
                    ?: textBundle.noOutputText
                appendOutput(command, output)
            } catch (_: CancellationException) {
                if (suppressStopOutputAppend) {
                    suppressStopOutputAppend = false
                } else {
                    appendOutput(command, textBundle.commandStoppedText)
                }
            } finally {
                runningCommand = false
                runningJob = null
            }
        }
        runningJob = job
    }

    fun stopCommand(showStoppedOutput: Boolean = true) {
        val job = runningJob ?: return
        if (!showStoppedOutput) {
            suppressStopOutputAppend = true
        }
        job.cancel(CancellationException("user-stop"))
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
        val output = outputText.trim()
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
                outputTimeLabel = textBundle.outputTimeLabel
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
        persistInputEnabled = persistInputEnabled,
        persistOutputEnabled = persistOutputEnabled,
        commandInput = commandInput,
        outputText = outputText
    )
    BindOsShellRunnerAutoScrollEffect(
        outputText = outputText,
        outputScrollState = outputScrollState
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
                modifier = Modifier.clickable { onClose() }
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
        persistInputEnabled = persistInputEnabled,
        onPersistInputEnabledChange = { checked -> persistInputEnabled = checked },
        persistOutputEnabled = persistOutputEnabled,
        onPersistOutputEnabledChange = { checked -> persistOutputEnabled = checked }
    )
}
