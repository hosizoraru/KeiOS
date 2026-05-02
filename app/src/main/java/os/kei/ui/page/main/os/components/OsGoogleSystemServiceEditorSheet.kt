package os.kei.ui.page.main.os.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.appLucideAddIcon
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtraType
import os.kei.ui.page.main.os.shortcut.ShortcutSuggestionField
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetFieldBlock
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OsGoogleSystemServiceEditorSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: LayerBackdrop,
    draft: OsGoogleSystemServiceConfig,
    onDraftChange: (OsGoogleSystemServiceConfig) -> Unit,
    onOpenSuggestionSheet: (ShortcutSuggestionField) -> Unit,
    showBuiltInBadge: Boolean,
    showDeleteAction: Boolean,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    val intentExtraController = rememberOsGoogleSystemServiceIntentExtraController(
        draft = draft,
        onDraftChange = onDraftChange
    )
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
            if (showBuiltInBadge) {
                SheetSectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.os_activity_card_builtin_sample_desc),
                            color = MiuixTheme.colorScheme.onBackground
                        )
                        StatusPill(
                            label = stringResource(R.string.os_activity_card_builtin_badge),
                            color = Color(0xFF3B82F6),
                            size = AppStatusPillSize.Compact
                        )
                    }
                }
            }
            SheetSectionTitle(stringResource(R.string.os_google_system_service_sheet_section_card))
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_title)
                ) {
                    GlassSearchField(
                        value = draft.title,
                        onValueChange = { onDraftChange(draft.copy(title = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_title),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_subtitle)
                ) {
                    GlassSearchField(
                        value = draft.subtitle,
                        onValueChange = { onDraftChange(draft.copy(subtitle = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_subtitle),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SheetSectionTitle(stringResource(R.string.os_google_system_service_sheet_section_target))
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_app_name)
                ) {
                    GlassSearchField(
                        value = draft.appName,
                        onValueChange = { onDraftChange(draft.copy(appName = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_app_name),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_package_name),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.PackageName) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.packageName,
                        onValueChange = { onDraftChange(draft.copy(packageName = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_package_name),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_class_name),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.ClassName) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.className,
                        onValueChange = { onDraftChange(draft.copy(className = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_class_name),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_intent_action),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.IntentAction) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.intentAction,
                        onValueChange = { onDraftChange(draft.copy(intentAction = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_intent_action),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_intent_category),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.IntentCategory) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.intentCategory,
                        onValueChange = { onDraftChange(draft.copy(intentCategory = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_intent_category),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_intent_flags),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.IntentFlags) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.intentFlags,
                        onValueChange = { onDraftChange(draft.copy(intentFlags = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_intent_flags),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_intent_data),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.IntentUriData) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.intentUriData,
                        onValueChange = { onDraftChange(draft.copy(intentUriData = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_intent_data),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_intent_mime_type),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = { onOpenSuggestionSheet(ShortcutSuggestionField.IntentMimeType) }
                        )
                    }
                ) {
                    GlassSearchField(
                        value = draft.intentMimeType,
                        onValueChange = { onDraftChange(draft.copy(intentMimeType = it)) },
                        label = stringResource(R.string.os_google_system_service_hint_intent_mime_type),
                        backdrop = sheetBackdrop,
                        variant = GlassVariant.SheetInput,
                        textColor = MiuixTheme.colorScheme.primary,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SheetFieldBlock(
                    title = stringResource(R.string.os_google_system_service_field_intent_extras),
                    trailing = {
                        SuggestionTriggerAction(
                            sheetBackdrop = sheetBackdrop,
                            onClick = intentExtraController.onAddIntentExtra
                        )
                    }
                ) {
                    val intentExtraTypeOptions = ShortcutIntentExtraType.entries.map { type ->
                        stringResource(type.labelResId)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        intentExtraController.editableExtras.forEachIndexed { index, extra ->
                            val selectedTypeIndex = ShortcutIntentExtraType.entries.indexOf(extra.type)
                                .coerceAtLeast(0)
                            SheetSectionCard(verticalSpacing = 8.dp) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    GlassSearchField(
                                        value = extra.key,
                                        onValueChange = { input ->
                                            intentExtraController.onExtraKeyChange(index, input)
                                        },
                                        label = stringResource(R.string.os_google_system_service_hint_intent_extra_key),
                                        backdrop = sheetBackdrop,
                                        variant = GlassVariant.SheetInput,
                                        textColor = MiuixTheme.colorScheme.primary,
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AppDropdownSelector(
                                        selectedText = intentExtraTypeOptions[selectedTypeIndex],
                                        options = intentExtraTypeOptions,
                                        selectedIndex = selectedTypeIndex,
                                        expanded = intentExtraController.intentExtraTypePopupExpanded[index] == true,
                                        anchorBounds = intentExtraController.intentExtraTypePopupAnchors[index],
                                        onExpandedChange = { expanded ->
                                            intentExtraController.onExtraTypeExpandedChange(index, expanded)
                                        },
                                        onSelectedIndexChange = { selected ->
                                            val nextType = ShortcutIntentExtraType.entries[selected]
                                            intentExtraController.onExtraTypeChange(index, nextType)
                                        },
                                        onAnchorBoundsChange = { bounds ->
                                            intentExtraController.onExtraTypeAnchorBoundsChange(index, bounds)
                                        },
                                        backdrop = sheetBackdrop,
                                        variant = GlassVariant.SheetAction
                                    )
                                    GlassIconButton(
                                        backdrop = sheetBackdrop,
                                        variant = GlassVariant.SheetDangerAction,
                                        icon = appLucideCloseIcon(),
                                        contentDescription = stringResource(
                                            R.string.os_google_system_service_cd_remove_intent_extra
                                        ),
                                        width = 40.dp,
                                        height = 32.dp,
                                        onClick = { intentExtraController.onRemoveIntentExtra(index) }
                                    )
                                }
                                GlassSearchField(
                                    value = extra.value,
                                    onValueChange = { input ->
                                        intentExtraController.onExtraValueChange(index, input)
                                    },
                                    label = stringResource(R.string.os_google_system_service_hint_intent_extra_value),
                                    backdrop = sheetBackdrop,
                                    variant = GlassVariant.SheetInput,
                                    textColor = MiuixTheme.colorScheme.primary,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            SheetDescriptionText(
                text = stringResource(R.string.os_google_system_service_sheet_desc)
            )
            if (showDeleteAction) {
                SheetSectionTitle(
                    text = stringResource(R.string.os_activity_card_danger_title),
                    danger = true
                )
                SheetSectionCard {
                    AppLiquidTextButton(
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

@Composable
private fun SuggestionTriggerAction(
    sheetBackdrop: LayerBackdrop,
    onClick: () -> Unit
) {
    GlassIconButton(
        backdrop = sheetBackdrop,
        variant = GlassVariant.SheetAction,
        icon = appLucideAddIcon(),
        contentDescription = stringResource(R.string.os_google_system_service_chip_add),
        width = 40.dp,
        height = 32.dp,
        onClick = onClick
    )
}
