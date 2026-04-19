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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    onSaveShellCommand = { command ->
                        OsShellCommandStore.saveCommand(command).command.isNotBlank()
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
    onSaveShellCommand: (String) -> Boolean,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageListState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val shellPageTitle = stringResource(R.string.os_shell_page_title)
    val inputTitle = stringResource(R.string.os_shell_input_title)
    val outputTitle = stringResource(R.string.os_shell_output_title)
    val outputHint = stringResource(R.string.os_shell_output_hint)
    val runActionDescription = stringResource(R.string.os_shell_action_run)
    val stopActionDescription = stringResource(R.string.os_shell_action_stop)
    val saveCommandActionDescription = stringResource(R.string.os_shell_action_save_command)
    val copyOutputActionDescription = stringResource(R.string.os_shell_action_copy_output)
    val clearOutputActionDescription = stringResource(R.string.os_shell_action_clear_output_history)
    val outputRunningSubtitle = stringResource(R.string.os_shell_output_subtitle_running)
    val outputReadySubtitle = stringResource(R.string.os_shell_output_subtitle_ready)
    val noOutputText = stringResource(R.string.os_shell_run_empty_output)
    val missingPermissionText = stringResource(R.string.os_shell_run_requires_permission)
    val emptyCommandText = stringResource(R.string.os_shell_run_empty_command)
    val commandStoppedText = stringResource(R.string.os_shell_run_stopped)
    val commandSavedToast = stringResource(R.string.os_shell_toast_command_saved)
    val commandSaveEmptyToast = stringResource(R.string.os_shell_toast_command_save_empty)
    val outputCopiedToast = stringResource(R.string.os_shell_toast_output_copied)
    val outputCopyEmptyToast = stringResource(R.string.os_shell_toast_output_empty)

    var commandInput by rememberSaveable { mutableStateOf("") }
    var outputText by rememberSaveable { mutableStateOf("") }
    var runningCommand by remember { mutableStateOf(false) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    val outputScrollState = rememberScrollState()
    val shellOutputStateText = if (runningCommand) outputRunningSubtitle else outputReadySubtitle

    fun appendOutput(command: String, result: String) {
        val timestamp = SimpleDateFormat(
            "HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
        val previousOutput = outputText
        outputText = buildString {
            if (previousOutput.isNotBlank()) {
                appendLine(previousOutput)
                appendLine()
            }
            appendLine("$ $command")
            appendLine(result)
            append("[$timestamp]")
        }
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
                appendOutput(command, commandStoppedText)
            } finally {
                runningCommand = false
                runningJob = null
            }
        }
        runningJob = job
    }

    fun stopCommand() {
        val job = runningJob ?: return
        job.cancel(CancellationException("user-stop"))
    }

    fun saveCommandToCard() {
        val command = commandInput.trim()
        if (command.isBlank()) {
            Toast.makeText(context, commandSaveEmptyToast, Toast.LENGTH_SHORT).show()
            return
        }
        val saved = onSaveShellCommand(command)
        if (saved) {
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
                imageVector = MiuixIcons.Regular.Back,
                contentDescription = stringResource(R.string.common_close),
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.clickable { onClose() }
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
                        subtitle = shellOutputStateText,
                        endActions = {
                            GlassIconButton(
                                backdrop = null,
                                icon = MiuixIcons.Regular.Play,
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
                                icon = MiuixIcons.Regular.Close,
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
                                icon = MiuixIcons.Regular.Download,
                                contentDescription = saveCommandActionDescription,
                                onClick = { saveCommandToCard() },
                                iconTint = MiuixTheme.colorScheme.primary,
                                variant = GlassVariant.Bar
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp)
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp)
                    ) {
                        ShellCommandInputField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            label = stringResource(R.string.os_shell_input_hint),
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
                                icon = MiuixIcons.Regular.Copy,
                                contentDescription = copyOutputActionDescription,
                                onClick = { copyOutput() },
                                iconTint = MiuixTheme.colorScheme.primary,
                                variant = GlassVariant.Bar
                            )
                            GlassIconButton(
                                backdrop = null,
                                icon = MiuixIcons.Regular.Close,
                                contentDescription = clearOutputActionDescription,
                                onClick = { outputText = "" },
                                iconTint = MiuixTheme.colorScheme.onBackgroundVariant,
                                variant = GlassVariant.Bar
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 520.dp)
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp)
                            .background(
                                color = MiuixTheme.colorScheme.surface.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (outputText.isBlank()) {
                            Text(
                                text = outputHint,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                fontSize = AppTypographyTokens.Body.fontSize,
                                lineHeight = AppTypographyTokens.Body.lineHeight,
                            )
                        } else {
                            SelectionContainer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(outputScrollState)
                            ) {
                                Text(
                                    text = outputText,
                                    color = MiuixTheme.colorScheme.onBackground,
                                    fontSize = AppTypographyTokens.Body.fontSize,
                                    lineHeight = AppTypographyTokens.Body.lineHeight,
                                    maxLines = Int.MAX_VALUE,
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

@Composable
private fun ShellCommandInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val shape: CornerBasedShape = RoundedCornerShape(18.dp)
    val textColor = MiuixTheme.colorScheme.onBackground
    val placeholderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = if (isDark) 0.72f else 0.64f)
    val borderColor = if (isDark) {
        Color(0xFF9CCBFF).copy(alpha = 0.30f)
    } else {
        Color(0xFFBFD8F8).copy(alpha = 0.92f)
    }
    val baseColor = if (isDark) {
        Color(0xFF121A24).copy(alpha = 0.46f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val overlayColor = if (isDark) {
        Color(0xFF82B6F5).copy(alpha = 0.09f)
    } else {
        Color(0xFFE4F1FF).copy(alpha = 0.26f)
    }
    val textStyle = TextStyle(
        color = textColor,
        fontSize = AppTypographyTokens.Body.fontSize,
        lineHeight = AppTypographyTokens.Body.lineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor, shape)
            .background(overlayColor, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            textStyle = textStyle,
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 240.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 240.dp)
                ) {
                    if (value.isBlank()) {
                        BasicText(
                            text = label,
                            style = textStyle.copy(color = placeholderColor),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
