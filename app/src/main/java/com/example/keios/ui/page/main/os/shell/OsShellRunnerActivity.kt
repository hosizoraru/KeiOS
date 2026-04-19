package com.example.keios.ui.page.main

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.core.prefs.AppThemeMode
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.ui.page.main.widget.AppCardHeader
import com.example.keios.ui.page.main.widget.AppChromeTokens
import com.example.keios.ui.page.main.widget.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.AppPageScaffold
import com.example.keios.ui.page.main.widget.AppSurfaceCard
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetFieldBlock
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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

private val shellAnsiEscapeRegex = Regex("""\u001B\[[;\d]*[ -/]*[@-~]""")
private val shellKeyValueRegex = Regex("""\b[^\s=]+=[^\s=]+\b""")
private const val shellPersistDebounceMs = 220L
private const val shellOutputMaxChars = 120_000

class OsShellRunnerActivity : ComponentActivity() {
    private var shizukuStatus by mutableStateOf("Shizuku status: initializing...")
    private val shizukuApiUtils = ShizukuApiUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shizukuApiUtils.attach { status -> shizukuStatus = status }

        setContent {
            val appThemeMode = UiPrefs.getAppThemeMode()
            val colorSchemeMode = when (appThemeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> ColorSchemeMode.System
                AppThemeMode.LIGHT -> ColorSchemeMode.Light
                AppThemeMode.DARK -> ColorSchemeMode.Dark
            }
            val controller = ThemeController(colorSchemeMode)

            MiuixTheme(controller = controller) {
                OsShellRunnerPage(
                    canRunShellCommand = shizukuStatus.contains("granted", ignoreCase = true) ||
                        shizukuApiUtils.canUseCommand(),
                    onRequestShizukuPermission = { shizukuApiUtils.requestPermissionIfNeeded() },
                    onRunShellCommand = { command ->
                        shizukuApiUtils.execCommandCancellable(command = command, timeoutMs = 300_000L)
                    },
                    onSaveShellCommand = { command, title, subtitle, runOutput ->
                        OsShellCommandCardStore.createCard(
                            command = command,
                            title = title,
                            subtitle = subtitle,
                            runOutput = runOutput
                        ) != null
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        shizukuApiUtils.detach()
        super.onDestroy()
    }

    companion object {
        fun launch(context: Context) {
            val hostActivity = context.findHostActivity()
            val intent = Intent(context, OsShellRunnerActivity::class.java).apply {
                if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (hostActivity != null) {
                hostActivity.startActivity(intent)
            } else {
                context.startActivity(intent)
            }
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}

@Composable
private fun OsShellRunnerPage(
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
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val shellCommandAccentColor = if (isDark) Color(0xFF7AB8FF) else Color(0xFF2563EB)
    val shellSuccessAccentColor = if (isDark) Color(0xFF7EE7A8) else Color(0xFF15803D)
    val shellStoppedAccentColor = if (isDark) Color(0xFFFF9E9E) else Color(0xFFDC2626)
    val surfaceColor = MiuixTheme.colorScheme.surface
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
        })
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
        outputText = formatShellOutputForReadability(outputText)
        outputEntries = parseShellOutputDisplayEntries(
            raw = outputText,
            stoppedOutputText = commandStoppedText,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel
        )
        latestRunResultOutput = outputEntries.lastOrNull()?.result.orEmpty().trim()
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
                layeredStyleEnabled = true,
                items = listOf(
                    LiquidActionItem(
                        icon = osLucideClearAllIcon(),
                        contentDescription = clearAllActionDescription,
                        onClick = { clearAllContent() }
                    ),
                    LiquidActionItem(
                        icon = osLucideSettingsIcon(),
                        contentDescription = settingsActionDescription,
                        onClick = { showSettingsSheet = true }
                    )
                )
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
    SnapshotWindowBottomSheet(
        show = showSaveSheet,
        title = saveSheetTitle,
        onDismissRequest = { showSaveSheet = false },
        startAction = {
            GlassIconButton(
                backdrop = null,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = { showSaveSheet = false }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = null,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.common_save),
                onClick = { saveCommandToCard() }
            )
        }
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            val commandPreview = commandInput.trim()
            val previewEntry = latestOutputEntry?.takeIf { it.command == commandPreview }
            val previewResult = previewEntry?.result
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.os_shell_card_run_output_not_ran)
            val previewTime = previewEntry?.timeLabel
                ?.takeIf { it.isNotBlank() }
                ?: saveSheetTimePlaceholder
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = saveSheetCommandLabel)
                Text(
                    text = "$ $commandPreview",
                    color = shellCommandAccentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = previewResult,
                    color = if (previewEntry?.isStopped == true) {
                        shellStoppedAccentColor
                    } else {
                        shellSuccessAccentColor
                    },
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = previewTime,
                    color = shellSuccessAccentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetFieldBlock(title = saveSheetFieldTitle) {
                    GlassSearchField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = saveSheetTitleHint,
                        backdrop = null,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(title = saveSheetFieldSubtitle) {
                    GlassSearchField(
                        value = saveSubtitleInput,
                        onValueChange = { saveSubtitleInput = it },
                        label = saveSheetSubtitleHint,
                        backdrop = null,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    OsShellSettingsSheet(
        show = showSettingsSheet,
        onDismissRequest = { showSettingsSheet = false },
        persistInputEnabled = persistInputEnabled,
        onPersistInputEnabledChange = { checked -> persistInputEnabled = checked },
        persistOutputEnabled = persistOutputEnabled,
        onPersistOutputEnabledChange = { checked -> persistOutputEnabled = checked }
    )
}

private fun formatShellOutputForReadability(raw: String): String {
    val normalized = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
    val jsonPretty = tryFormatShellOutputAsJson(normalized)
    if (jsonPretty != null) return jsonPretty

    val noAnsi = shellAnsiEscapeRegex.replace(normalized, "")
    val lines = mutableListOf<String>()
    var previousBlank = true

    noAnsi.lines().forEach { source ->
        val reflowed = reflowShellVerboseLine(
            source.replace("\t", "    ").trimEnd()
        )
        reflowed.lines().forEach { lineRaw ->
            val line = lineRaw.trimEnd()
            if (line.isBlank()) {
                if (!previousBlank) {
                    lines += ""
                    previousBlank = true
                }
            } else {
                if (lineLooksLikeSectionHeading(line) && lines.isNotEmpty() && lines.last().isNotBlank()) {
                    lines += ""
                }
                lines += line
                previousBlank = false
            }
        }
    }

    return lines.joinToString("\n").trim()
}

private fun tryFormatShellOutputAsJson(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    return runCatching {
        when {
            trimmed.startsWith("{") && trimmed.endsWith("}") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") && trimmed.endsWith("]") -> JSONArray(trimmed).toString(2)
            else -> null
        }
    }.getOrNull()
}

private fun reflowShellVerboseLine(line: String): String {
    if (line.length <= 120) return line
    val keyValues = shellKeyValueRegex.findAll(line).map { it.value }.toList()
    if (keyValues.size >= 6) {
        return keyValues.joinToString(separator = "\n") { token -> "  $token" }
    }
    if (line.contains(", ") && line.count { it == ',' } >= 5) {
        return line.replace(", ", ",\n  ")
    }
    if (line.contains("; ") && line.count { it == ';' } >= 4) {
        return line.replace("; ", ";\n  ")
    }
    return line
}

private fun lineLooksLikeSectionHeading(line: String): Boolean {
    if (line.length !in 2..80) return false
    if (line.startsWith(" ") || line.startsWith("\t")) return false
    return line.endsWith(":")
}

private fun trimShellOutputHistory(raw: String): String {
    val normalized = raw.trimEnd()
    if (normalized.length <= shellOutputMaxChars) return normalized
    return normalized
        .takeLast(shellOutputMaxChars)
        .trimStart()
}
