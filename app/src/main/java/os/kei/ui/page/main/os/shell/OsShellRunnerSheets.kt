package os.kei.ui.page.main.os.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetChoiceCard
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet

@Composable
internal fun OsShellSettingsSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    settings: OsShellRunnerSettings,
    onPersistInputEnabledChange: (Boolean) -> Unit,
    onPersistOutputEnabledChange: (Boolean) -> Unit,
    onTimeoutSecondsChange: (Int) -> Unit,
    onAutoFormatOutputChange: (Boolean) -> Unit,
    onAutoScrollOutputChange: (Boolean) -> Unit,
    onOutputLimitCharsChange: (Int) -> Unit,
    onOutputSaveModeChange: (OsShellRunnerOutputSaveMode) -> Unit,
    onDangerousCommandConfirmChange: (Boolean) -> Unit,
    onCompletionToastChange: (Boolean) -> Unit,
    onStartupBehaviorChange: (OsShellRunnerStartupBehavior) -> Unit,
    onExitCleanupModeChange: (OsShellRunnerExitCleanupMode) -> Unit,
    onCopyModeChange: (OsShellRunnerCopyMode) -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.os_shell_settings_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = null,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        }
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_section_general))
                SheetControlRow(
                    label = stringResource(R.string.os_shell_settings_persist_input_label),
                    summary = stringResource(R.string.os_shell_settings_persist_input_summary)
                ) {
                    AppSwitch(
                        checked = settings.persistInput,
                        onCheckedChange = onPersistInputEnabledChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.os_shell_settings_persist_output_label),
                    summary = stringResource(R.string.os_shell_settings_persist_output_summary)
                ) {
                    AppSwitch(
                        checked = settings.persistOutput,
                        onCheckedChange = onPersistOutputEnabledChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.os_shell_settings_auto_format_output_label),
                    summary = stringResource(R.string.os_shell_settings_auto_format_output_summary)
                ) {
                    AppSwitch(
                        checked = settings.autoFormatOutput,
                        onCheckedChange = onAutoFormatOutputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.os_shell_settings_auto_scroll_output_label),
                    summary = stringResource(R.string.os_shell_settings_auto_scroll_output_summary)
                ) {
                    AppSwitch(
                        checked = settings.autoScrollOutput,
                        onCheckedChange = onAutoScrollOutputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.os_shell_settings_danger_confirm_label),
                    summary = stringResource(R.string.os_shell_settings_danger_confirm_summary)
                ) {
                    AppSwitch(
                        checked = settings.dangerousCommandConfirm,
                        onCheckedChange = onDangerousCommandConfirmChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.os_shell_settings_completion_toast_label),
                    summary = stringResource(R.string.os_shell_settings_completion_toast_summary)
                ) {
                    AppSwitch(
                        checked = settings.completionToast,
                        onCheckedChange = onCompletionToastChange
                    )
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_timeout_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shellRunnerTimeoutOptionsSeconds.forEach { seconds ->
                        SheetChoiceCard(
                            title = stringResource(
                                R.string.os_shell_settings_timeout_option_seconds,
                                seconds
                            ),
                            summary = stringResource(R.string.os_shell_settings_timeout_option_summary),
                            selected = settings.commandTimeoutSeconds == seconds,
                            onSelect = { onTimeoutSecondsChange(seconds) }
                        )
                    }
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_output_limit_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shellRunnerOutputLimitOptionsChars.forEach { maxChars ->
                        SheetChoiceCard(
                            title = stringResource(
                                R.string.os_shell_settings_output_limit_option_kilo,
                                maxChars / 1000
                            ),
                            summary = stringResource(R.string.os_shell_settings_output_limit_option_summary),
                            selected = settings.outputLimitChars == maxChars,
                            onSelect = { onOutputLimitCharsChange(maxChars) }
                        )
                    }
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_output_save_mode_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_output_save_mode_full_title),
                        summary = stringResource(R.string.os_shell_settings_output_save_mode_full_summary),
                        selected = settings.outputSaveMode == OsShellRunnerOutputSaveMode.FullHistory,
                        onSelect = { onOutputSaveModeChange(OsShellRunnerOutputSaveMode.FullHistory) }
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_output_save_mode_latest_title),
                        summary = stringResource(R.string.os_shell_settings_output_save_mode_latest_summary),
                        selected = settings.outputSaveMode == OsShellRunnerOutputSaveMode.LatestOnly,
                        onSelect = { onOutputSaveModeChange(OsShellRunnerOutputSaveMode.LatestOnly) }
                    )
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_startup_behavior_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_startup_behavior_focus_title),
                        summary = stringResource(R.string.os_shell_settings_startup_behavior_focus_summary),
                        selected = settings.startupBehavior == OsShellRunnerStartupBehavior.FocusInput,
                        onSelect = { onStartupBehaviorChange(OsShellRunnerStartupBehavior.FocusInput) }
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_startup_behavior_silent_title),
                        summary = stringResource(R.string.os_shell_settings_startup_behavior_silent_summary),
                        selected = settings.startupBehavior == OsShellRunnerStartupBehavior.Silent,
                        onSelect = { onStartupBehaviorChange(OsShellRunnerStartupBehavior.Silent) }
                    )
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_exit_cleanup_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_exit_cleanup_keep_all_title),
                        summary = stringResource(R.string.os_shell_settings_exit_cleanup_keep_all_summary),
                        selected = settings.exitCleanupMode == OsShellRunnerExitCleanupMode.KeepAll,
                        onSelect = { onExitCleanupModeChange(OsShellRunnerExitCleanupMode.KeepAll) }
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_exit_cleanup_clear_input_title),
                        summary = stringResource(R.string.os_shell_settings_exit_cleanup_clear_input_summary),
                        selected = settings.exitCleanupMode == OsShellRunnerExitCleanupMode.ClearInput,
                        onSelect = { onExitCleanupModeChange(OsShellRunnerExitCleanupMode.ClearInput) }
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_exit_cleanup_clear_output_title),
                        summary = stringResource(R.string.os_shell_settings_exit_cleanup_clear_output_summary),
                        selected = settings.exitCleanupMode == OsShellRunnerExitCleanupMode.ClearOutput,
                        onSelect = { onExitCleanupModeChange(OsShellRunnerExitCleanupMode.ClearOutput) }
                    )
                }
            }

            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetSectionTitle(text = stringResource(R.string.os_shell_settings_copy_mode_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_copy_mode_full_title),
                        summary = stringResource(R.string.os_shell_settings_copy_mode_full_summary),
                        selected = settings.copyMode == OsShellRunnerCopyMode.FullHistory,
                        onSelect = { onCopyModeChange(OsShellRunnerCopyMode.FullHistory) }
                    )
                    SheetChoiceCard(
                        title = stringResource(R.string.os_shell_settings_copy_mode_latest_title),
                        summary = stringResource(R.string.os_shell_settings_copy_mode_latest_summary),
                        selected = settings.copyMode == OsShellRunnerCopyMode.LatestResult,
                        onSelect = { onCopyModeChange(OsShellRunnerCopyMode.LatestResult) }
                    )
                }
            }

            SheetDescriptionText(
                text = stringResource(R.string.os_shell_settings_desc)
            )
        }
    }
}
