package com.example.keios.ui.page.main.os.shell.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.os.osLucideClearIcon
import com.example.keios.ui.page.main.os.osLucideCopyIcon
import com.example.keios.ui.page.main.os.osLucideFormatIcon
import com.example.keios.ui.page.main.os.osLucideRunIcon
import com.example.keios.ui.page.main.os.osLucideSaveIcon
import com.example.keios.ui.page.main.os.osLucideStopIcon
import com.example.keios.ui.page.main.os.shell.ShellCommandInputField
import com.example.keios.ui.page.main.os.shell.ShellOutputDisplayEntry
import com.example.keios.ui.page.main.os.shell.ShellOutputGlassPanel
import com.example.keios.ui.page.main.widget.core.AppCardHeader
import com.example.keios.ui.page.main.widget.core.AppSurfaceCard
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OsShellRunnerInputCard(
    inputTitle: String,
    inputHint: String,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    runningCommand: Boolean,
    runActionDescription: String,
    stopActionDescription: String,
    saveCommandActionDescription: String,
    focusRequestToken: Int,
    onRunCommand: () -> Unit,
    onStopCommand: () -> Unit,
    onOpenSaveCommandSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth()
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
                    onClick = onRunCommand,
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
                    onClick = onStopCommand,
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
                    onClick = onOpenSaveCommandSheet,
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
                onValueChange = onCommandInputChange,
                label = inputHint,
                minHeight = 90.dp,
                focusRequestToken = focusRequestToken,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun OsShellRunnerOutputCard(
    outputTitle: String,
    outputHint: String,
    outputText: String,
    outputEntries: List<ShellOutputDisplayEntry>,
    outputScrollState: ScrollState,
    formatOutputActionDescription: String,
    copyOutputActionDescription: String,
    clearOutputActionDescription: String,
    onFormatOutput: () -> Unit,
    onCopyOutput: () -> Unit,
    onClearOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth()
    ) {
        AppCardHeader(
            title = outputTitle,
            subtitle = "",
            endActions = {
                GlassIconButton(
                    backdrop = null,
                    icon = osLucideFormatIcon(),
                    contentDescription = formatOutputActionDescription,
                    onClick = onFormatOutput,
                    iconTint = MiuixTheme.colorScheme.primary,
                    variant = GlassVariant.Bar
                )
                GlassIconButton(
                    backdrop = null,
                    icon = osLucideCopyIcon(),
                    contentDescription = copyOutputActionDescription,
                    onClick = onCopyOutput,
                    iconTint = MiuixTheme.colorScheme.primary,
                    variant = GlassVariant.Bar
                )
                GlassIconButton(
                    backdrop = null,
                    icon = osLucideClearIcon(),
                    contentDescription = clearOutputActionDescription,
                    onClick = onClearOutput,
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
