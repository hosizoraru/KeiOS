package com.example.keios.ui.page.main

import androidx.compose.foundation.layout.Arrangement
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
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppStatusPillSize
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetFieldBlock
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.addShellCommandCards(
    cards: List<OsShellCommandCard>,
    contentBackdrop: LayerBackdrop,
    expandedStates: Map<String, Boolean>,
    onExpandedChange: (String, Boolean) -> Unit,
    onHeaderLongClick: (OsShellCommandCard) -> Unit
) {
    cards.filter { it.visible }.forEach { card ->
        item(key = "os-shell-command-${card.id}") {
            MiuixAccordionCard(
                backdrop = contentBackdrop,
                title = card.title.ifBlank { defaultOsShellCommandCardTitle(card.command) },
                subtitle = card.subtitle,
                expanded = expandedStates[card.id] == true,
                onExpandedChange = { expanded -> onExpandedChange(card.id, expanded) },
                headerStartAction = {
                    Icon(
                        imageVector = MiuixIcons.Regular.Tasks,
                        contentDescription = card.title,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .defaultMinSize(minHeight = 22.dp)
                    )
                },
                onHeaderLongClick = { onHeaderLongClick(card) }
            ) {
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_saved_command_label),
                    value = card.command
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_run_output_label),
                    value = card.runOutput.ifBlank {
                        stringResource(R.string.os_shell_card_run_output_not_ran)
                    }
                )
                OsSectionInfoRow(
                    label = stringResource(R.string.os_shell_card_updated_at_label),
                    value = if (card.updatedAtMillis > 0L) {
                        formatEpochMillis(card.updatedAtMillis)
                    } else {
                        stringResource(R.string.common_unknown)
                    }
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
                icon = MiuixIcons.Regular.Close,
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
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
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
