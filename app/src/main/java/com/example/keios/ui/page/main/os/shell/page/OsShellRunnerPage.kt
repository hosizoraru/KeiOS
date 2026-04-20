package com.example.keios.ui.page.main.os.shell.page

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.os.osLucideClearAllIcon
import com.example.keios.ui.page.main.os.osLucideClearIcon
import com.example.keios.ui.page.main.os.osLucideCopyIcon
import com.example.keios.ui.page.main.os.osLucideFormatIcon
import com.example.keios.ui.page.main.os.osLucideRunIcon
import com.example.keios.ui.page.main.os.osLucideSaveIcon
import com.example.keios.ui.page.main.os.osLucideSettingsIcon
import com.example.keios.ui.page.main.os.osLucideStopIcon
import com.example.keios.ui.page.main.widget.core.AppCardHeader
import com.example.keios.ui.page.main.widget.chrome.AppChromeTokens
import com.example.keios.ui.page.main.widget.chrome.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.chrome.AppPageScaffold
import com.example.keios.ui.page.main.widget.core.AppSurfaceCard
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.example.keios.ui.page.main.os.shell.component.OsShellRunnerSaveSheet
import com.example.keios.ui.page.main.os.shell.util.buildShellOutputHistoryText
import com.example.keios.ui.page.main.os.shell.util.formatShellResultForReadability
import com.example.keios.ui.page.main.os.shell.util.trimShellOutputHistory
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shell.OsShellRunnerPrefsStore
import com.example.keios.ui.page.main.os.shell.OsShellSettingsSheet
import com.example.keios.ui.page.main.os.shell.ShellCommandInputField
import com.example.keios.ui.page.main.os.shell.ShellOutputDisplayEntry
import com.example.keios.ui.page.main.os.shell.ShellOutputGlassPanel
import com.example.keios.ui.page.main.os.shell.parseShellOutputDisplayEntries
import com.example.keios.ui.page.main.os.shell.trimShellOutputEntries

private const val shellPersistDebounceMs = 220L
private const val shellOutputMaxChars = 120_000

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
    val shellPageTitle = stringResource(R.string.os_shell_page_title)
    val inputTitle = stringResource(R.string.os_shell_input_title)
    val outputTitle = stringResource(R.string.os_shell_output_title)
    val outputHint = stringResource(R.string.os_shell_output_hint)
    val outputResultLabel = stringResource(R.string.os_shell_output_block_result)
    val outputTimeLabel = stringResource(R.string.os_shell_output_block_time)
    val runActionDescription = stringResource(R.string.os_shell_action_run)
    val stopActionDescription = stringResource(R.string.os_shell_action_stop)
    val saveCommandActionDescription = stringResource(R.string.os_shell_action_save_command)
    val clearAllActionDescription = stringResource(R.string.os_shell_action_clear_all)
    val settingsActionDescription = stringResource(R.string.os_shell_action_settings)
    val formatOutputActionDescription = stringResource(R.string.os_shell_action_format_output)
    val copyOutputActionDescription = stringResource(R.string.os_shell_action_copy_output)
    val clearOutputActionDescription = stringResource(R.string.os_shell_action_clear_output_history)
    val noOutputText = stringResource(R.string.os_shell_run_empty_output)
    val missingPermissionText = stringResource(R.string.os_shell_run_requires_permission)
    val emptyCommandText = stringResource(R.string.os_shell_run_empty_command)
    val commandStoppedText = stringResource(R.string.os_shell_run_stopped)
    val commandSavedToast = stringResource(R.string.os_shell_toast_command_saved)
    val commandSaveEmptyToast = stringResource(R.string.os_shell_toast_command_save_empty)
    val saveSheetTitle = stringResource(R.string.os_shell_save_sheet_title)
    val saveSheetCommandLabel = stringResource(R.string.os_shell_save_sheet_command_label)
    val saveSheetFieldTitle = stringResource(R.string.os_shell_save_sheet_field_title)
    val saveSheetFieldSubtitle = stringResource(R.string.os_shell_save_sheet_field_subtitle)
    val saveSheetTitleHint = stringResource(R.string.os_shell_save_sheet_title_hint)
    val saveSheetSubtitleHint = stringResource(R.string.os_shell_save_sheet_subtitle_hint)
    val saveSheetTitleRequiredToast = stringResource(R.string.os_shell_toast_save_title_empty)
    val saveSheetTimePlaceholder = stringResource(R.string.os_shell_save_sheet_time_placeholder)
    val outputFormattedToast = stringResource(R.string.os_shell_toast_output_formatted)
    val outputFormatEmptyToast = stringResource(R.string.os_shell_toast_output_format_empty)
    val outputCopiedToast = stringResource(R.string.os_shell_toast_output_copied)
    val outputCopyEmptyToast = stringResource(R.string.os_shell_toast_output_empty)
    val clearAllToast = stringResource(R.string.os_shell_toast_cleared_all)
    val isDark = isSystemInDarkTheme()
    val shellCommandAccentColor = if (isDark) Color(0xFF7AB8FF) else Color(0xFF2563EB)
    val shellSuccessAccentColor = if (isDark) Color(0xFF7EE7A8) else Color(0xFF15803D)
    val shellStoppedAccentColor = if (isDark) Color(0xFFFF9E9E) else Color(0xFFDC2626)
    val surfaceColor = MiuixTheme.colorScheme.surface
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
    val topBarBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val initialPersistSettings = remember { OsShellRunnerPrefsStore.loadPersistSettings() }
    val initialCommandInput = remember {
        if (initialPersistSettings.persistInput) {
            OsShellRunnerPrefsStore.loadSavedInput()
        } else {
            ""
        }
    }
    val initialOutputText = remember {
        if (initialPersistSettings.persistOutput) {
            OsShellRunnerPrefsStore.loadSavedOutput()
        } else {
            ""
        }
    }
    val initialOutputEntries = remember(
        initialOutputText,
        commandStoppedText,
        outputResultLabel,
        outputTimeLabel
    ) {
        parseShellOutputDisplayEntries(
            raw = initialOutputText,
            stoppedOutputText = commandStoppedText,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel
        )
    }

    var commandInput by rememberSaveable { mutableStateOf(initialCommandInput) }
    var outputText by rememberSaveable { mutableStateOf(initialOutputText) }
    var outputEntries by remember { mutableStateOf(initialOutputEntries) }
    var latestRunResultOutput by rememberSaveable {
        mutableStateOf(initialOutputEntries.lastOrNull()?.result.orEmpty().trim())
    }
    var runningCommand by remember { mutableStateOf(false) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var suppressStopOutputAppend by remember { mutableStateOf(false) }
    var showSaveSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var saveTitleInput by rememberSaveable { mutableStateOf("") }
    var saveSubtitleInput by rememberSaveable { mutableStateOf("") }
    var persistInputEnabled by rememberSaveable { mutableStateOf(initialPersistSettings.persistInput) }
    var persistOutputEnabled by rememberSaveable { mutableStateOf(initialPersistSettings.persistOutput) }
    val latestOutputEntry = remember(outputEntries) { outputEntries.lastOrNull() }
    val outputScrollState = rememberScrollState()
    BackHandler(enabled = showSaveSheet) { showSaveSheet = false }
    BackHandler(enabled = !showSaveSheet && showSettingsSheet) { showSettingsSheet = false }
    BackHandler(enabled = !showSaveSheet && !showSettingsSheet, onBack = onClose)

    fun appendOutput(command: String, result: String) {
        val normalizedResult = result.trimEnd()
        latestRunResultOutput = normalizedResult
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val timeLabel = "[$timestamp]"
        val previousOutput = outputText.trimEnd()
        outputText = trimShellOutputHistory(buildString {
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
        }, maxChars = shellOutputMaxChars)
        outputEntries = trimShellOutputEntries(
            outputEntries + ShellOutputDisplayEntry(
                command = command.trim(),
                result = normalizedResult,
                isStopped = normalizedResult.trim() == commandStoppedText.trim(),
                timeLabel = timeLabel
            ),
            maxChars = shellOutputMaxChars
        )
    }

    fun runCommand() {
        if (runningCommand) return
        val command = commandInput.trim()
        if (command.isBlank()) {
            outputText = emptyCommandText
            return
        }
        if (!canRunShellCommand) {
            onRequestShizukuPermission()
            outputText = missingPermissionText
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
                    ?: noOutputText
                appendOutput(command, output)
            } catch (_: CancellationException) {
                if (suppressStopOutputAppend) {
                    suppressStopOutputAppend = false
                } else {
                    appendOutput(command, commandStoppedText)
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
            Toast.makeText(context, commandSaveEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val currentCard = OsShellCommandCardStore.findLatestByCommand(command)
        saveTitleInput = ""
        saveSubtitleInput = if (currentCard != null) {
            currentCard.subtitle
        } else {
            ""
        }
        showSaveSheet = true
    }

    fun saveCommandToCard() {
        val command = commandInput.trim()
        if (command.isBlank()) {
            Toast.makeText(context, commandSaveEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val title = saveTitleInput.trim()
        if (title.isBlank()) {
            Toast.makeText(context, saveSheetTitleRequiredToast, Toast.LENGTH_SHORT).show()
            return
        }
        val subtitle = saveSubtitleInput.trim()
        val saved = onSaveShellCommand(command, title, subtitle, latestRunResultOutput)
        if (saved) {
            showSaveSheet = false
            Toast.makeText(context, commandSavedToast, Toast.LENGTH_SHORT).show()
        }
    }

    fun copyOutput() {
        val output = outputText.trim()
        if (output.isBlank()) {
            Toast.makeText(context, outputCopyEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("shell_output", output))
        Toast.makeText(context, outputCopiedToast, Toast.LENGTH_SHORT).show()
    }

    fun formatOutput() {
        val output = outputText.trim()
        if (output.isBlank()) {
            Toast.makeText(context, outputFormatEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
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
            outputEntries = trimmedEntries
            outputText = buildShellOutputHistoryText(
                entries = trimmedEntries,
                maxChars = shellOutputMaxChars
            )
            latestRunResultOutput = trimmedEntries.lastOrNull()?.result.orEmpty().trim()
        } else {
            outputText = formatShellResultForReadability(outputText)
            outputEntries = parseShellOutputDisplayEntries(
                raw = outputText,
                stoppedOutputText = commandStoppedText,
                outputResultLabel = outputResultLabel,
                outputTimeLabel = outputTimeLabel
            )
            latestRunResultOutput = outputEntries.lastOrNull()?.result.orEmpty().trim()
        }
        Toast.makeText(context, outputFormattedToast, Toast.LENGTH_SHORT).show()
    }

    fun clearAllContent() {
        stopCommand(showStoppedOutput = false)
        commandInput = ""
        outputText = ""
        outputEntries = emptyList()
        latestRunResultOutput = ""
        Toast.makeText(context, clearAllToast, Toast.LENGTH_SHORT).show()
    }
    val clearAllIcon = osLucideClearAllIcon()
    val settingsIcon = osLucideSettingsIcon()
    val actionItems = remember(clearAllActionDescription, settingsActionDescription) {
        listOf(
            LiquidActionItem(
                icon = clearAllIcon,
                contentDescription = clearAllActionDescription,
                onClick = { clearAllContent() }
            ),
            LiquidActionItem(
                icon = settingsIcon,
                contentDescription = settingsActionDescription,
                onClick = { showSettingsSheet = true }
            )
        )
    }

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

    LaunchedEffect(outputText) {
        if (outputText.isNotBlank()) {
            outputScrollState.scrollTo(outputScrollState.maxValue)
        }
    }

    AppPageScaffold(
        title = shellPageTitle,
        largeTitle = shellPageTitle,
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
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    AppCardHeader(
                        title = inputTitle,
                        subtitle = "",
                        titleAccessory = {
                            if (runningCommand) {
                                CircularProgressIndicator(
                                    progress = 0.42f,
                                    size = 14.dp,
                                    strokeWidth = 2.dp,
                                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                        foregroundColor = MiuixTheme.colorScheme.primary,
                                        backgroundColor = MiuixTheme.colorScheme.primary.copy(
                                            alpha = if (isDark) 0.28f else 0.22f
                                        )
                                    )
                                )
                            }
                        },
                        endActions = {
                            GlassIconButton(
                                backdrop = null,
                                icon = osLucideRunIcon(),
                                contentDescription = runActionDescription,
                                onClick = { runCommand() },
                                iconTint = if (runningCommand) {
                                    MiuixTheme.colorScheme.onBackgroundVariant
                                } else {
                                    MiuixTheme.colorScheme.primary
                                },
                                variant = GlassVariant.Bar
                            )
                            GlassIconButton(
                                backdrop = null,
                                icon = osLucideStopIcon(),
                                contentDescription = stopActionDescription,
                                onClick = { stopCommand() },
                                iconTint = if (runningCommand) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onBackgroundVariant
                                },
                                variant = GlassVariant.Bar
                            )
                            GlassIconButton(
                                backdrop = null,
                                icon = osLucideSaveIcon(),
                                contentDescription = saveCommandActionDescription,
                                onClick = { openSaveCommandSheet() },
                                iconTint = MiuixTheme.colorScheme.primary,
                                variant = GlassVariant.Bar
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp)
                    ) {
                        ShellCommandInputField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            label = stringResource(R.string.os_shell_input_hint),
                            minHeight = 90.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
            item(key = "shell_output_card") {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    AppCardHeader(
                        title = outputTitle,
                        subtitle = "",
                        endActions = {
                            GlassIconButton(
                                backdrop = null,
                                icon = osLucideFormatIcon(),
                                contentDescription = formatOutputActionDescription,
                                onClick = { formatOutput() },
                                iconTint = MiuixTheme.colorScheme.primary,
                                variant = GlassVariant.Bar
                            )
                            GlassIconButton(
                                backdrop = null,
                                icon = osLucideCopyIcon(),
                                contentDescription = copyOutputActionDescription,
                                onClick = { copyOutput() },
                                iconTint = MiuixTheme.colorScheme.primary,
                                variant = GlassVariant.Bar
                            )
                            GlassIconButton(
                                backdrop = null,
                                icon = osLucideClearIcon(),
                                contentDescription = clearOutputActionDescription,
                                onClick = {
                                    outputText = ""
                                    outputEntries = emptyList()
                                    latestRunResultOutput = ""
                                },
                                iconTint = MiuixTheme.colorScheme.onBackgroundVariant,
                                variant = GlassVariant.Bar
                            )
                        }
                    )
                    ShellOutputGlassPanel(
                        text = outputText,
                        hint = outputHint,
                        entries = outputEntries,
                        scrollState = outputScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .heightIn(min = 160.dp, max = 320.dp)
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp)
                    )
                }
            }
        }
    }
    OsShellRunnerSaveSheet(
        show = showSaveSheet,
        title = saveSheetTitle,
        commandInput = commandInput,
        latestOutputEntry = latestOutputEntry,
        saveSheetCommandLabel = saveSheetCommandLabel,
        saveSheetFieldTitle = saveSheetFieldTitle,
        saveSheetFieldSubtitle = saveSheetFieldSubtitle,
        saveSheetTitleHint = saveSheetTitleHint,
        saveSheetSubtitleHint = saveSheetSubtitleHint,
        saveSheetTimePlaceholder = saveSheetTimePlaceholder,
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
