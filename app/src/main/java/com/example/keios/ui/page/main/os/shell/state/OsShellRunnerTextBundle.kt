package com.example.keios.ui.page.main.os.shell.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.keios.R

internal data class OsShellRunnerTextBundle(
    val shellPageTitle: String,
    val inputTitle: String,
    val outputTitle: String,
    val outputHint: String,
    val outputResultLabel: String,
    val outputTimeLabel: String,
    val runActionDescription: String,
    val stopActionDescription: String,
    val saveCommandActionDescription: String,
    val clearAllActionDescription: String,
    val settingsActionDescription: String,
    val formatOutputActionDescription: String,
    val copyOutputActionDescription: String,
    val clearOutputActionDescription: String,
    val noOutputText: String,
    val missingPermissionText: String,
    val emptyCommandText: String,
    val commandStoppedText: String,
    val commandSavedToast: String,
    val commandSaveEmptyToast: String,
    val saveSheetTitle: String,
    val saveSheetCommandLabel: String,
    val saveSheetFieldTitle: String,
    val saveSheetFieldSubtitle: String,
    val saveSheetTitleHint: String,
    val saveSheetSubtitleHint: String,
    val saveSheetTitleRequiredToast: String,
    val saveSheetTimePlaceholder: String,
    val outputFormattedToast: String,
    val outputFormatEmptyToast: String,
    val outputCopiedToast: String,
    val outputCopyEmptyToast: String,
    val clearAllToast: String,
    val inputHint: String,
    val commandCompletedToast: String,
    val dangerousCommandDialogTitle: String,
    val dangerousCommandConfirmText: String
)

@Composable
internal fun rememberOsShellRunnerTextBundle(): OsShellRunnerTextBundle {
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
    val inputHint = stringResource(R.string.os_shell_input_hint)
    val commandCompletedToast = stringResource(R.string.os_shell_toast_command_completed)
    val dangerousCommandDialogTitle = stringResource(R.string.os_shell_dangerous_command_dialog_title)
    val dangerousCommandConfirmText = stringResource(R.string.os_shell_dangerous_command_dialog_confirm)

    return remember(
        shellPageTitle,
        inputTitle,
        outputTitle,
        outputHint,
        outputResultLabel,
        outputTimeLabel,
        runActionDescription,
        stopActionDescription,
        saveCommandActionDescription,
        clearAllActionDescription,
        settingsActionDescription,
        formatOutputActionDescription,
        copyOutputActionDescription,
        clearOutputActionDescription,
        noOutputText,
        missingPermissionText,
        emptyCommandText,
        commandStoppedText,
        commandSavedToast,
        commandSaveEmptyToast,
        saveSheetTitle,
        saveSheetCommandLabel,
        saveSheetFieldTitle,
        saveSheetFieldSubtitle,
        saveSheetTitleHint,
        saveSheetSubtitleHint,
        saveSheetTitleRequiredToast,
        saveSheetTimePlaceholder,
        outputFormattedToast,
        outputFormatEmptyToast,
        outputCopiedToast,
        outputCopyEmptyToast,
        clearAllToast,
        inputHint,
        commandCompletedToast,
        dangerousCommandDialogTitle,
        dangerousCommandConfirmText
    ) {
        OsShellRunnerTextBundle(
            shellPageTitle = shellPageTitle,
            inputTitle = inputTitle,
            outputTitle = outputTitle,
            outputHint = outputHint,
            outputResultLabel = outputResultLabel,
            outputTimeLabel = outputTimeLabel,
            runActionDescription = runActionDescription,
            stopActionDescription = stopActionDescription,
            saveCommandActionDescription = saveCommandActionDescription,
            clearAllActionDescription = clearAllActionDescription,
            settingsActionDescription = settingsActionDescription,
            formatOutputActionDescription = formatOutputActionDescription,
            copyOutputActionDescription = copyOutputActionDescription,
            clearOutputActionDescription = clearOutputActionDescription,
            noOutputText = noOutputText,
            missingPermissionText = missingPermissionText,
            emptyCommandText = emptyCommandText,
            commandStoppedText = commandStoppedText,
            commandSavedToast = commandSavedToast,
            commandSaveEmptyToast = commandSaveEmptyToast,
            saveSheetTitle = saveSheetTitle,
            saveSheetCommandLabel = saveSheetCommandLabel,
            saveSheetFieldTitle = saveSheetFieldTitle,
            saveSheetFieldSubtitle = saveSheetFieldSubtitle,
            saveSheetTitleHint = saveSheetTitleHint,
            saveSheetSubtitleHint = saveSheetSubtitleHint,
            saveSheetTitleRequiredToast = saveSheetTitleRequiredToast,
            saveSheetTimePlaceholder = saveSheetTimePlaceholder,
            outputFormattedToast = outputFormattedToast,
            outputFormatEmptyToast = outputFormatEmptyToast,
            outputCopiedToast = outputCopiedToast,
            outputCopyEmptyToast = outputCopyEmptyToast,
            clearAllToast = clearAllToast,
            inputHint = inputHint,
            commandCompletedToast = commandCompletedToast,
            dangerousCommandDialogTitle = dangerousCommandDialogTitle,
            dangerousCommandConfirmText = dangerousCommandConfirmText
        )
    }
}
