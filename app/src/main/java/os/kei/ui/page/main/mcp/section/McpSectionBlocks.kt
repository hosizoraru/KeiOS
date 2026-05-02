package os.kei.ui.page.main.mcp.section

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.mcp.server.McpServerUiState
import os.kei.ui.page.main.os.appLucideAppWindowIcon
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideNotesIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.AppLiquidExpandableSection
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun McpServiceControlSection(
    backdrop: LayerBackdrop,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onShowResetConfigConfirm: () -> Unit,
) {
    AppLiquidExpandableSection(
        backdrop = backdrop,
        title = stringResource(R.string.mcp_section_service_control_title),
        subtitle = stringResource(R.string.mcp_section_service_control_subtitle),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerStartAction = {
            McpSectionHeaderIcon(
                icon = appLucideConfigIcon(),
                contentDescription = stringResource(R.string.mcp_section_service_control_title)
            )
        }
    ) {
        AppDualActionRow(
            spacing = CardLayoutRhythm.infoRowGap,
            first = { modifier ->
                AppLiquidTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetPrimaryAction,
                    text = stringResource(R.string.mcp_action_send_test_notification),
                    modifier = modifier,
                    textColor = MiuixTheme.colorScheme.primary,
                    onClick = onSendTestNotification
                )
            },
            second = { modifier ->
                AppLiquidTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetDangerAction,
                    text = stringResource(R.string.mcp_action_reset_service_config),
                    textColor = MiuixTheme.colorScheme.error,
                    modifier = modifier,
                    onClick = onShowResetConfigConfirm
                )
            }
        )
    }
}

@Composable
internal fun McpToolsSection(
    backdrop: LayerBackdrop,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    uiState: McpServerUiState,
) {
    AppLiquidExpandableSection(
        backdrop = backdrop,
        title = stringResource(R.string.mcp_section_tools_title),
        subtitle = stringResource(R.string.mcp_section_tools_subtitle, uiState.tools.size),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerStartAction = {
            McpSectionHeaderIcon(
                icon = appLucideAppWindowIcon(),
                contentDescription = stringResource(R.string.mcp_section_tools_title)
            )
        }
    ) {
        uiState.tools.forEach { tool ->
            MiuixInfoItem(
                key = tool.name,
                value = tool.description
            )
        }
    }
}

@Composable
internal fun McpLogsSection(
    backdrop: LayerBackdrop,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    uiState: McpServerUiState,
    logsExporting: Boolean,
    onExportLogs: (generatedAt: String, fileName: String) -> Unit,
    onClearLogs: () -> Unit,
    subtitleColor: Color,
) {
    AppLiquidExpandableSection(
        backdrop = backdrop,
        title = stringResource(R.string.mcp_section_logs_title),
        subtitle = stringResource(R.string.mcp_section_logs_subtitle, uiState.logs.size),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerStartAction = {
            McpSectionHeaderIcon(
                icon = appLucideNotesIcon(),
                contentDescription = stringResource(R.string.mcp_section_logs_title)
            )
        },
        headerActions = {
            AppCompactIconAction(
                icon = if (logsExporting) {
                    appLucideRefreshIcon()
                } else {
                    appLucideDownloadIcon()
                },
                contentDescription = stringResource(R.string.mcp_action_export_logs),
                tint = if (logsExporting) {
                    subtitleColor
                } else {
                    MiuixTheme.colorScheme.primary
                },
                enabled = !logsExporting,
                onClick = {
                    val generatedAt = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        Locale.getDefault()
                    ).format(Date())
                    val exportStamp = SimpleDateFormat(
                        "yyyyMMdd-HHmmss-SSS",
                        Locale.getDefault()
                    ).format(Date())
                    onExportLogs(generatedAt, "keios-mcp-logs-$exportStamp.json")
                }
            )
        }
    ) {
        if (uiState.logs.isEmpty()) {
            MiuixInfoItem(
                key = stringResource(R.string.mcp_log_label),
                value = stringResource(R.string.mcp_log_empty)
            )
        } else {
            uiState.logs.asReversed().forEach { log ->
                val logTitle = "${log.time} [${log.level}]"
                MiuixInfoItem(
                    key = logTitle,
                    value = log.message
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppLiquidTextButton(
            backdrop = backdrop,
            variant = GlassVariant.Content,
            text = stringResource(R.string.mcp_action_clear_logs),
            onClick = onClearLogs
        )
    }
}
