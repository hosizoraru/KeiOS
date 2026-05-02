package os.kei.ui.page.main.settings.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.core.log.AppLogStore
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsInfoItem
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.settings.support.formatBytes
import os.kei.ui.page.main.settings.support.formatLogTime
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsLogSection(
    logDebugEnabled: Boolean,
    onLogDebugChanged: (Boolean) -> Unit,
    logStats: AppLogStore.Stats,
    exportingLogZip: Boolean,
    clearingLogs: Boolean,
    onExportZipClick: () -> Unit,
    onClearLogsClick: () -> Unit,
    enabledCardColor: Color,
    disabledCardColor: Color
) {
    val logGroupActive = logDebugEnabled || logStats.fileCount > 0
    val logLatestText = if (logStats.latestModifiedAtMs <= 0L) {
        stringResource(R.string.settings_log_stat_latest_empty)
    } else {
        formatLogTime(logStats.latestModifiedAtMs)
    }
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_log_header),
        title = stringResource(R.string.settings_group_log_title),
        sectionIcon = appLucideNotesIcon(),
        containerColor = if (logGroupActive) enabledCardColor else disabledCardColor
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_log_debug_title),
            summary = if (logDebugEnabled) {
                stringResource(R.string.settings_log_debug_summary_enabled)
            } else {
                stringResource(R.string.settings_log_debug_summary_disabled)
            },
            checked = logDebugEnabled,
            onCheckedChange = onLogDebugChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_log_scope)
        )
        SettingsInfoItem(
            key = stringResource(R.string.common_note),
            value = if (logDebugEnabled) {
                stringResource(R.string.settings_log_note_enabled)
            } else {
                stringResource(R.string.settings_log_note_disabled)
            }
        )
        SettingsInfoItem(
            key = stringResource(R.string.settings_log_stat_size),
            value = formatBytes(logStats.totalBytes)
        )
        SettingsInfoItem(
            key = stringResource(R.string.settings_log_stat_files),
            value = stringResource(R.string.settings_log_stat_files_count, logStats.fileCount)
        )
        SettingsInfoItem(
            key = stringResource(R.string.settings_log_stat_latest),
            value = logLatestText
        )
        AppDualActionRow(
            first = { modifier ->
                AppLiquidTextButton(
                    backdrop = null,
                    variant = GlassVariant.SheetPrimaryAction,
                    text = if (exportingLogZip) {
                        stringResource(R.string.common_processing)
                    } else {
                        stringResource(R.string.settings_log_action_export_zip)
                    },
                    modifier = modifier,
                    textColor = MiuixTheme.colorScheme.primary,
                    enabled = !exportingLogZip && !clearingLogs,
                    onClick = onExportZipClick
                )
            },
            second = { modifier ->
                AppLiquidTextButton(
                    backdrop = null,
                    variant = GlassVariant.SheetDangerAction,
                    text = if (clearingLogs) {
                        stringResource(R.string.common_processing)
                    } else {
                        stringResource(R.string.settings_log_action_clear)
                    },
                    modifier = modifier,
                    textColor = MiuixTheme.colorScheme.error,
                    enabled = !exportingLogZip && !clearingLogs,
                    onClick = onClearLogsClick
                )
            }
        )
    }
}
