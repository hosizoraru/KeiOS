package os.kei.ui.page.main.os.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.ShellCommandInputField
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.os.shell.defaultOsShellCommandCardTitle
import os.kei.ui.page.main.os.formatEpochMillis
import os.kei.ui.page.main.os.osLucideCardIcon
import os.kei.ui.page.main.os.osLucideRunIcon
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.MiuixAccordionCard
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetFieldBlock
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.addShellCommandCards(
    cards: List<OsShellCommandCard>,
    contentBackdrop: LayerBackdrop,
    expandedStates: Map<String, Boolean>,
    runningCardIds: Set<String>,
    onExpandedChange: (String, Boolean) -> Unit,
    onHeaderLongClick: (OsShellCommandCard) -> Unit,
    onRunCard: (OsShellCommandCard) -> Unit
) {
    cards.filter { it.visible }.forEach { card ->
        item(key = "os-shell-command-${card.id}") {
            val cardIsRunning = runningCardIds.contains(card.id)
            MiuixAccordionCard(
                backdrop = contentBackdrop,
                title = card.title.ifBlank { defaultOsShellCommandCardTitle(card.command) },
                subtitle = card.subtitle,
                expanded = expandedStates[card.id] == true,
                onExpandedChange = { expanded -> onExpandedChange(card.id, expanded) },
                headerStartAction = {
                    Icon(
                        imageVector = osLucideCardIcon(),
                        contentDescription = card.title,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .defaultMinSize(minHeight = 22.dp)
                    )
                },
                headerActions = {
                    if (cardIsRunning) {
                        CircularProgressIndicator(
                            progress = 0.42f,
                            size = 16.dp,
                            strokeWidth = 2.dp,
                            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                foregroundColor = MiuixTheme.colorScheme.primary,
                                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.24f)
                            )
                        )
                    } else {
                        Icon(
                            imageVector = osLucideRunIcon(),
                            contentDescription = stringResource(R.string.os_shell_card_cd_run_saved),
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onRunCard(card) }
                        )
                    }
                },
                onHeaderLongClick = { onHeaderLongClick(card) }
            ) {
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_saved_command_label),
                    value = card.command,
                    copyValueOnly = true
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_run_output_label),
                    value = card.runOutput.ifBlank {
                        stringResource(R.string.os_shell_card_run_output_not_ran)
                    },
                    copyValueOnly = true
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_last_ran_at_label),
                    value = if (card.lastRunAtMillis > 0L) {
                        formatEpochMillis(card.lastRunAtMillis)
                    } else {
                        stringResource(R.string.os_shell_card_run_output_not_ran)
                    },
                    copyValueOnly = true
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_updated_at_label),
                    value = if (card.updatedAtMillis > 0L) {
                        formatEpochMillis(card.updatedAtMillis)
                    } else {
                        stringResource(R.string.common_unknown)
                    },
                    copyValueOnly = true
                )
            }
        }
        item(key = "os-shell-command-space-${card.id}") { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
internal fun OsShellCommandVisibilityManagerSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: LayerBackdrop,
    shellHintText: String,
    shellRunnerVisible: Boolean,
    onShellRunnerVisibilityChange: (Boolean) -> Unit,
    cards: List<OsShellCommandCard>,
    transferInProgress: Boolean,
    onExportAllCards: () -> Unit,
    onImportAllCards: () -> Unit,
    onDismissRequest: () -> Unit,
    onCardVisibilityChange: (String, Boolean) -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        }
    ) {
        SheetContentColumn(
            scrollable = false,
            verticalSpacing = 10.dp
        ) {
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetControlRow(
                    labelContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.os_shell_card_title),
                                color = MiuixTheme.colorScheme.onBackground
                            )
                            StatusPill(
                                label = stringResource(R.string.os_shell_card_builtin_badge),
                                color = Color(0xFF3B82F6),
                                size = AppStatusPillSize.Compact
                            )
                        }
                    }
                ) {
                    Switch(
                        checked = shellRunnerVisible,
                        onCheckedChange = onShellRunnerVisibilityChange
                    )
                }
                cards.forEach { item ->
                    SheetControlRow(
                        labelContent = {
                            Text(
                                text = item.title.ifBlank { defaultOsShellCommandCardTitle(item.command) },
                                color = MiuixTheme.colorScheme.onBackground,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    ) {
                        Switch(
                            checked = item.visible,
                            onCheckedChange = { checked ->
                                onCardVisibilityChange(item.id, checked)
                            }
                        )
                    }
                }
            }
            SheetSectionCard(verticalSpacing = 8.dp) {
                Text(
                    text = stringResource(R.string.os_shell_sheet_transfer_title),
                    color = MiuixTheme.colorScheme.onBackground
                )
                SheetDescriptionText(text = stringResource(R.string.os_shell_sheet_transfer_desc))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextButton(
                            backdrop = sheetBackdrop,
                            text = stringResource(R.string.os_shell_sheet_action_export_backup),
                            onClick = onExportAllCards,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !transferInProgress,
                            variant = GlassVariant.SheetAction,
                            pressScaleEnabled = false,
                            pressOverlayEnabled = false,
                            textMaxLines = 1,
                            textOverflow = TextOverflow.Ellipsis,
                            textSoftWrap = false
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextButton(
                            backdrop = sheetBackdrop,
                            text = stringResource(R.string.os_shell_sheet_action_import_backup),
                            onClick = onImportAllCards,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !transferInProgress,
                            variant = GlassVariant.SheetAction,
                            pressScaleEnabled = false,
                            pressOverlayEnabled = false,
                            textMaxLines = 1,
                            textOverflow = TextOverflow.Ellipsis,
                            textSoftWrap = false
                        )
                    }
                }
            }
            SheetDescriptionText(text = shellHintText)
        }
    }
}

@Composable
internal fun OsShellCommandCardEditorSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: LayerBackdrop,
    draft: OsShellCommandCard,
    onDraftChange: (OsShellCommandCard) -> Unit,
    showDeleteAction: Boolean,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.common_save),
                onClick = onSave
            )
        }
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetFieldBlock(title = stringResource(R.string.os_shell_card_field_title)) {
                    GlassSearchField(
                        value = draft.title,
                        onValueChange = { onDraftChange(draft.copy(title = it)) },
                        label = stringResource(R.string.os_shell_card_hint_title),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(title = stringResource(R.string.os_shell_card_field_subtitle)) {
                    GlassSearchField(
                        value = draft.subtitle,
                        onValueChange = { onDraftChange(draft.copy(subtitle = it)) },
                        label = stringResource(R.string.os_shell_card_hint_subtitle),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(title = stringResource(R.string.os_shell_card_field_command)) {
                    ShellCommandInputField(
                        value = draft.command,
                        onValueChange = { onDraftChange(draft.copy(command = it)) },
                        label = stringResource(R.string.os_shell_card_hint_command),
                        minHeight = 132.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (showDeleteAction) {
                SheetSectionTitle(
                    text = stringResource(R.string.os_shell_card_danger_title),
                    danger = true
                )
                SheetSectionCard {
                    GlassTextButton(
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetDangerAction,
                        text = stringResource(R.string.common_delete),
                        textColor = MiuixTheme.colorScheme.error,
                        onClick = onDelete
                    )
                }
            }
        }
    }
}
