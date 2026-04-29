package os.kei.ui.page.main.mcp.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetActionGroup
import os.kei.ui.page.main.widget.sheet.SheetChoiceCard
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun McpEditServiceSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    serverName: String,
    onServerNameChange: (String) -> Unit,
    serverNameFieldWidth: Dp,
    portText: String,
    onPortTextChange: (String) -> Unit,
    portFieldWidth: Dp,
    allowExternal: Boolean,
    onAllowExternalChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
    onShowResetTokenConfirm: () -> Unit,
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.mcp_sheet_edit_service_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.common_save),
                onClick = onSave
            )
        }
    ) {
        SheetContentColumn {
            SheetSectionTitle(stringResource(R.string.mcp_sheet_section_basic))
            SheetSectionCard {
                SheetControlRow(label = stringResource(R.string.mcp_overview_label_service_name)) {
                    GlassSearchField(
                        value = serverName,
                        onValueChange = onServerNameChange,
                        label = stringResource(R.string.mcp_input_service_name_hint),
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(serverNameFieldWidth)
                    )
                }
                SheetControlRow(label = stringResource(R.string.mcp_sheet_label_service_port)) {
                    GlassSearchField(
                        value = portText,
                        onValueChange = onPortTextChange,
                        label = stringResource(R.string.mcp_input_port_hint),
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(portFieldWidth)
                    )
                }
            }
            SheetSectionTitle(stringResource(R.string.mcp_sheet_section_network_access))
            SheetActionGroup {
                McpNetworkModeOption(
                    title = stringResource(R.string.mcp_network_mode_local_only_short),
                    summary = stringResource(R.string.mcp_network_mode_local_only_summary),
                    selected = !allowExternal,
                    onClick = { onAllowExternalChange(false) }
                )
                McpNetworkModeOption(
                    title = stringResource(R.string.mcp_network_mode_lan_short),
                    summary = stringResource(R.string.mcp_network_mode_lan_summary),
                    selected = allowExternal,
                    onClick = { onAllowExternalChange(true) }
                )
            }
            SheetSectionTitle(
                text = stringResource(R.string.common_danger_zone),
                danger = true
            )
            SheetSectionCard {
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetDangerAction,
                    text = stringResource(R.string.mcp_action_reset_token),
                    textColor = MiuixTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onShowResetTokenConfirm
                )
            }
        }
    }
}

@Composable
private fun McpNetworkModeOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFF22C55E)
    SheetChoiceCard(
        title = title,
        summary = summary,
        selected = selected,
        onSelect = onClick,
        accentColor = selectedColor,
        selectedLabel = stringResource(R.string.common_status_activated)
    )
}
