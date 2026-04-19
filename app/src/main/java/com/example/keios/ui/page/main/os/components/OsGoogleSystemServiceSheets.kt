package com.example.keios.ui.page.main

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppDropdownSelector
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.SheetChoiceCard
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetFieldBlock
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.kyant.backdrop.backdrops.LayerBackdrop
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OsGoogleSystemServiceEditorSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: LayerBackdrop,
    draft: OsGoogleSystemServiceConfig,
    onDraftChange: (OsGoogleSystemServiceConfig) -> Unit,
    onOpenSuggestionSheet: (ShortcutSuggestionField) -> Unit,
    showDeleteAction: Boolean,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    val intentExtraTypePopupExpanded = remember { mutableStateMapOf<Int, Boolean>() }
    val intentExtraTypePopupAnchors = remember { mutableStateMapOf<Int, IntRect?>() }

    fun cleanupIntentExtraPopupState(size: Int) {
        intentExtraTypePopupExpanded.keys
            .filter { it >= size }
            .forEach { key ->
                intentExtraTypePopupExpanded.remove(key)
                intentExtraTypePopupAnchors.remove(key)
            }
    }

    fun commitIntentExtras(next: List<ShortcutIntentExtra>) {
        val finalList = if (next.isEmpty()) {
            listOf(ShortcutIntentExtra())
        } else {
            next
        }
        cleanupIntentExtraPopupState(finalList.size)
        onDraftChange(draft.copy(intentExtras = finalList))
    }

    fun updateIntentExtra(index: Int, transform: (ShortcutIntentExtra) -> ShortcutIntentExtra) {
        val current = ensureEditorShortcutIntentExtras(draft.intentExtras).toMutableList()
        if (index !in current.indices) return
        current[index] = transform(current[index])
        commitIntentExtras(current)
    }

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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.PackageName)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.ClassName)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.IntentAction)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.IntentCategory)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.IntentFlags)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.IntentUriData)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                onOpenSuggestionSheet(ShortcutSuggestionField.IntentMimeType)
                            }
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
                        GlassIconButton(
                            backdrop = sheetBackdrop,
                            variant = GlassVariant.SheetAction,
                            icon = MiuixIcons.Regular.AddCircle,
                            contentDescription = stringResource(R.string.os_google_system_service_chip_add),
                            width = 40.dp,
                            height = 32.dp,
                            onClick = {
                                val current = ensureEditorShortcutIntentExtras(draft.intentExtras)
                                commitIntentExtras(current + ShortcutIntentExtra())
                            }
                        )
                    }
                ) {
                    val editableExtras = ensureEditorShortcutIntentExtras(draft.intentExtras)
                    val intentExtraTypeOptions = ShortcutIntentExtraType.entries.map { type ->
                        stringResource(type.labelResId)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        editableExtras.forEachIndexed { index, extra ->
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
                                            updateIntentExtra(index) { current ->
                                                current.copy(key = input)
                                            }
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
                                        expanded = intentExtraTypePopupExpanded[index] == true,
                                        anchorBounds = intentExtraTypePopupAnchors[index],
                                        onExpandedChange = { expanded ->
                                            intentExtraTypePopupExpanded[index] = expanded
                                        },
                                        onSelectedIndexChange = { selected ->
                                            val nextType = ShortcutIntentExtraType.entries[selected]
                                            updateIntentExtra(index) { current ->
                                                current.copy(type = nextType)
                                            }
                                        },
                                        onAnchorBoundsChange = { bounds ->
                                            intentExtraTypePopupAnchors[index] = bounds
                                        },
                                        backdrop = sheetBackdrop,
                                        variant = GlassVariant.SheetAction
                                    )
                                    GlassIconButton(
                                        backdrop = sheetBackdrop,
                                        variant = GlassVariant.SheetDangerAction,
                                        icon = MiuixIcons.Regular.Close,
                                        contentDescription = stringResource(
                                            R.string.os_google_system_service_cd_remove_intent_extra
                                        ),
                                        width = 40.dp,
                                        height = 32.dp,
                                        onClick = {
                                            val current = ensureEditorShortcutIntentExtras(draft.intentExtras)
                                                .toMutableList()
                                            if (index !in current.indices) return@GlassIconButton
                                            current.removeAt(index)
                                            commitIntentExtras(current)
                                        }
                                    )
                                }
                                GlassSearchField(
                                    value = extra.value,
                                    onValueChange = { input ->
                                        updateIntentExtra(index) { current ->
                                            current.copy(value = input)
                                        }
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

@Composable
internal fun OsGoogleSystemServiceSuggestionSheet(
    show: Boolean,
    target: ShortcutSuggestionField,
    draft: OsGoogleSystemServiceConfig,
    sheetBackdrop: LayerBackdrop,
    packageSuggestions: List<ShortcutInstalledAppOption>,
    packageSuggestionsLoading: Boolean,
    packageSuggestionQuery: String,
    onPackageSuggestionQueryChange: (String) -> Unit,
    classSuggestions: List<ShortcutActivityClassOption>,
    classSuggestionsLoading: Boolean,
    classSuggestionQuery: String,
    onClassSuggestionQueryChange: (String) -> Unit,
    noMatchedResultsText: String,
    onDismissRequest: () -> Unit,
    onApplySuggestion: (ShortcutSuggestionItem) -> Unit,
    onApplyExplicitActionRecommendation: () -> Unit,
    onApplyImplicitActionRecommendation: () -> Unit,
    onApplyExplicitCategoryRecommendation: () -> Unit,
    onApplyImplicitCategoryRecommendation: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = when (target) {
            ShortcutSuggestionField.PackageName ->
                stringResource(R.string.os_google_system_service_field_package_name)
            ShortcutSuggestionField.ClassName ->
                stringResource(R.string.os_google_system_service_field_class_name)
            ShortcutSuggestionField.IntentAction ->
                stringResource(R.string.os_google_system_service_field_intent_action)
            ShortcutSuggestionField.IntentCategory ->
                stringResource(R.string.os_google_system_service_field_intent_category)
            ShortcutSuggestionField.IntentFlags ->
                stringResource(R.string.os_google_system_service_field_intent_flags)
            ShortcutSuggestionField.IntentUriData ->
                stringResource(R.string.os_google_system_service_field_intent_data)
            ShortcutSuggestionField.IntentMimeType ->
                stringResource(R.string.os_google_system_service_field_intent_mime_type)
        },
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
        val packageQuery = packageSuggestionQuery.trim()
        val currentPackageName = draft.packageName.trim()
        val filteredInstalledApps = remember(
            packageSuggestions,
            packageQuery,
            currentPackageName
        ) {
            val filtered = if (packageQuery.isBlank()) {
                packageSuggestions
            } else {
                packageSuggestions.filter { app ->
                    app.appName.contains(packageQuery, ignoreCase = true) ||
                        app.packageName.contains(packageQuery, ignoreCase = true)
                }
            }
            if (currentPackageName.isBlank()) {
                filtered
            } else {
                val selected = filtered.filter {
                    it.packageName.equals(currentPackageName, ignoreCase = true)
                }
                val others = filtered.filterNot {
                    it.packageName.equals(currentPackageName, ignoreCase = true)
                }
                selected + others
            }
        }
        val classQuery = classSuggestionQuery.trim()
        val currentClassName = draft.className.trim()
        val filteredActivityClasses = remember(
            classSuggestions,
            classQuery,
            currentClassName
        ) {
            val filtered = if (classQuery.isBlank()) {
                classSuggestions
            } else {
                classSuggestions.filter { option ->
                    option.className.contains(classQuery, ignoreCase = true) ||
                        option.activityName.contains(classQuery, ignoreCase = true)
                }
            }
            if (currentClassName.isBlank()) {
                filtered
            } else {
                val selected = filtered.filter { option ->
                    option.className.equals(currentClassName, ignoreCase = true)
                }
                val others = filtered.filterNot { option ->
                    option.className.equals(currentClassName, ignoreCase = true)
                }
                selected + others
            }
        }
        val currentIntentAction = draft.intentAction.trim()
        val currentCategoryTokensUpper = remember(draft.intentCategory) {
            parseIntentTokenText(draft.intentCategory)
                .map { it.uppercase(Locale.ROOT) }
                .toSet()
        }
        val explicitActionRecommendationSelected = currentClassName.isNotBlank() &&
            currentIntentAction == Intent.ACTION_VIEW
        val implicitActionRecommendationSelected = currentClassName.isBlank() &&
            currentIntentAction == Intent.ACTION_MAIN
        val explicitCategoryRecommendationSelected = currentClassName.isNotBlank() &&
            currentCategoryTokensUpper.isEmpty()
        val implicitCategoryRecommendationSelected = currentClassName.isBlank() &&
            currentCategoryTokensUpper == setOf(Intent.CATEGORY_LAUNCHER.uppercase(Locale.ROOT))
        val suggestions = when (target) {
            ShortcutSuggestionField.PackageName -> filteredInstalledApps.map { app ->
                ShortcutSuggestionItem(
                    label = app.appName,
                    value = app.packageName,
                    summary = app.packageName,
                    relatedAppName = app.appName
                )
            }

            ShortcutSuggestionField.ClassName -> {
                val implicitItem = ShortcutSuggestionItem(
                    label = stringResource(R.string.os_google_system_service_class_suggestion_implicit_title),
                    value = "",
                    summary = stringResource(R.string.os_google_system_service_class_suggestion_implicit_summary)
                )
                listOf(implicitItem) + filteredActivityClasses.map { option ->
                    ShortcutSuggestionItem(
                        label = option.activityName,
                        value = option.className,
                        summary = option.className,
                        classItemExported = option.isExported
                    )
                }
            }

            ShortcutSuggestionField.IntentAction -> listOf(
                ShortcutSuggestionItem(
                    label = stringResource(R.string.os_google_system_service_suggestion_clear),
                    value = "",
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_auto_summary),
                    append = false
                ),
                ShortcutSuggestionItem(
                    label = Intent.ACTION_MAIN,
                    value = Intent.ACTION_MAIN,
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_main_summary)
                ),
                ShortcutSuggestionItem(
                    label = Intent.ACTION_VIEW,
                    value = Intent.ACTION_VIEW,
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_view_summary)
                ),
                ShortcutSuggestionItem(
                    label = Intent.ACTION_SEND,
                    value = Intent.ACTION_SEND,
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_send_summary)
                ),
                ShortcutSuggestionItem(
                    label = Intent.ACTION_SEND_MULTIPLE,
                    value = Intent.ACTION_SEND_MULTIPLE,
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_send_multiple_summary)
                ),
                ShortcutSuggestionItem(
                    label = Intent.ACTION_EDIT,
                    value = Intent.ACTION_EDIT,
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_edit_summary)
                ),
                ShortcutSuggestionItem(
                    label = Intent.ACTION_PICK,
                    value = Intent.ACTION_PICK,
                    summary = stringResource(R.string.os_google_system_service_suggestion_action_pick_summary)
                )
            )

            ShortcutSuggestionField.IntentCategory -> listOf(
                ShortcutSuggestionItem(
                    label = stringResource(R.string.os_google_system_service_suggestion_clear),
                    value = "",
                    summary = stringResource(R.string.os_google_system_service_suggestion_category_clear_summary),
                    append = false
                ),
                ShortcutSuggestionItem(
                    label = Intent.CATEGORY_DEFAULT,
                    value = Intent.CATEGORY_DEFAULT,
                    summary = stringResource(R.string.os_google_system_service_suggestion_category_default_summary),
                    append = true
                ),
                ShortcutSuggestionItem(
                    label = Intent.CATEGORY_BROWSABLE,
                    value = Intent.CATEGORY_BROWSABLE,
                    summary = stringResource(R.string.os_google_system_service_suggestion_category_browsable_summary),
                    append = true
                ),
                ShortcutSuggestionItem(
                    label = Intent.CATEGORY_LAUNCHER,
                    value = Intent.CATEGORY_LAUNCHER,
                    summary = stringResource(R.string.os_google_system_service_suggestion_category_launcher_summary),
                    append = true
                ),
                ShortcutSuggestionItem(
                    label = Intent.CATEGORY_INFO,
                    value = Intent.CATEGORY_INFO,
                    summary = stringResource(R.string.os_google_system_service_suggestion_category_info_summary),
                    append = true
                )
            )

            ShortcutSuggestionField.IntentFlags -> listOf(
                ShortcutSuggestionItem(
                    label = stringResource(R.string.os_google_system_service_suggestion_clear),
                    value = "",
                    summary = stringResource(R.string.os_google_system_service_suggestion_flags_auto_summary),
                    append = false
                ),
                ShortcutSuggestionItem(
                    label = "FLAG_ACTIVITY_NEW_TASK",
                    value = "FLAG_ACTIVITY_NEW_TASK",
                    summary = stringResource(R.string.os_google_system_service_suggestion_flags_new_task_summary),
                    append = true
                ),
                ShortcutSuggestionItem(
                    label = "FLAG_ACTIVITY_CLEAR_TOP",
                    value = "FLAG_ACTIVITY_CLEAR_TOP",
                    summary = stringResource(R.string.os_google_system_service_suggestion_flags_clear_top_summary),
                    append = true
                ),
                ShortcutSuggestionItem(
                    label = "FLAG_ACTIVITY_SINGLE_TOP",
                    value = "FLAG_ACTIVITY_SINGLE_TOP",
                    summary = stringResource(R.string.os_google_system_service_suggestion_flags_single_top_summary),
                    append = true
                ),
                ShortcutSuggestionItem(
                    label = "FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP",
                    value = "FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP",
                    summary = stringResource(R.string.os_google_system_service_suggestion_flags_combo_summary),
                    append = true
                )
            )

            ShortcutSuggestionField.IntentUriData -> listOf(
                ShortcutSuggestionItem(
                    label = stringResource(R.string.os_google_system_service_suggestion_clear),
                    value = "",
                    summary = stringResource(R.string.os_google_system_service_suggestion_uri_clear_summary),
                    append = false
                ),
                ShortcutSuggestionItem(
                    label = "market://details?id=com.android.vending",
                    value = "market://details?id=com.android.vending",
                    summary = stringResource(R.string.os_google_system_service_suggestion_uri_market_summary)
                ),
                ShortcutSuggestionItem(
                    label = "https://play.google.com/store/apps/details?id=com.android.vending",
                    value = "https://play.google.com/store/apps/details?id=com.android.vending",
                    summary = stringResource(R.string.os_google_system_service_suggestion_uri_https_summary)
                ),
                ShortcutSuggestionItem(
                    label = "package:com.android.vending",
                    value = "package:com.android.vending",
                    summary = stringResource(R.string.os_google_system_service_suggestion_uri_package_summary)
                )
            )

            ShortcutSuggestionField.IntentMimeType -> listOf(
                ShortcutSuggestionItem(
                    label = stringResource(R.string.os_google_system_service_suggestion_clear),
                    value = "",
                    summary = stringResource(R.string.os_google_system_service_suggestion_mime_clear_summary),
                    append = false
                ),
                ShortcutSuggestionItem(
                    label = "text/plain",
                    value = "text/plain",
                    summary = stringResource(R.string.os_google_system_service_suggestion_mime_text_summary)
                ),
                ShortcutSuggestionItem(
                    label = "application/vnd.android.package-archive",
                    value = "application/vnd.android.package-archive",
                    summary = stringResource(R.string.os_google_system_service_suggestion_mime_apk_summary)
                ),
                ShortcutSuggestionItem(
                    label = "*/*",
                    value = "*/*",
                    summary = stringResource(R.string.os_google_system_service_suggestion_mime_any_summary)
                )
            )
        }
        val currentValue = currentGoogleSystemServiceSuggestionFieldValue(draft, target)
        val isCurrentSuggestionSelected: (ShortcutSuggestionItem) -> Boolean = { suggestion ->
            when (target) {
                ShortcutSuggestionField.PackageName -> {
                    currentValue.trim().equals(suggestion.value.trim(), ignoreCase = true)
                }

                ShortcutSuggestionField.ClassName -> {
                    currentValue.trim().equals(suggestion.value.trim(), ignoreCase = true)
                }

                ShortcutSuggestionField.IntentAction,
                ShortcutSuggestionField.IntentUriData,
                ShortcutSuggestionField.IntentMimeType -> {
                    currentValue.trim() == suggestion.value.trim()
                }

                ShortcutSuggestionField.IntentCategory,
                ShortcutSuggestionField.IntentFlags -> {
                    val currentTokens = parseIntentTokenText(currentValue)
                        .map { it.uppercase(Locale.ROOT) }
                        .toSet()
                    val suggestionTokens = parseIntentTokenText(suggestion.value)
                        .map { it.uppercase(Locale.ROOT) }
                        .toSet()
                    if (suggestionTokens.isEmpty()) {
                        currentTokens.isEmpty()
                    } else {
                        currentTokens.containsAll(suggestionTokens)
                    }
                }
            }
        }
        val orderedSuggestions = when (target) {
            ShortcutSuggestionField.ClassName -> suggestions
            else -> {
                val selected = suggestions.filter(isCurrentSuggestionSelected)
                val others = suggestions.filterNot(isCurrentSuggestionSelected)
                selected + others
            }
        }
        SheetContentColumn(verticalSpacing = 10.dp) {
            if (target == ShortcutSuggestionField.PackageName) {
                GlassSearchField(
                    value = packageSuggestionQuery,
                    onValueChange = onPackageSuggestionQueryChange,
                    label = stringResource(R.string.os_google_system_service_hint_package_suggestion_search),
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.SheetInput,
                    textColor = MiuixTheme.colorScheme.primary,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (target == ShortcutSuggestionField.ClassName) {
                GlassSearchField(
                    value = classSuggestionQuery,
                    onValueChange = onClassSuggestionQueryChange,
                    label = stringResource(R.string.os_google_system_service_hint_class_suggestion_search),
                    backdrop = sheetBackdrop,
                    variant = GlassVariant.SheetInput,
                    textColor = MiuixTheme.colorScheme.primary,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (target == ShortcutSuggestionField.IntentAction) {
                SheetSectionTitle(
                    text = stringResource(R.string.os_google_system_service_recommend_section)
                )
                SheetChoiceCard(
                    title = stringResource(R.string.os_google_system_service_action_recommend_explicit_title),
                    summary = stringResource(R.string.os_google_system_service_action_recommend_explicit_summary),
                    selected = explicitActionRecommendationSelected,
                    onSelect = onApplyExplicitActionRecommendation,
                    accentColor = Color(0xFF16A34A),
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = Color(0xFF16A34A),
                    summaryColor = Color(0xFF16A34A),
                    selectedLabel = null
                )
                SheetChoiceCard(
                    title = stringResource(R.string.os_google_system_service_action_recommend_implicit_title),
                    summary = stringResource(R.string.os_google_system_service_action_recommend_implicit_summary),
                    selected = implicitActionRecommendationSelected,
                    onSelect = onApplyImplicitActionRecommendation,
                    accentColor = Color(0xFF16A34A),
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = Color(0xFF16A34A),
                    summaryColor = Color(0xFF16A34A),
                    selectedLabel = null
                )
            }
            if (target == ShortcutSuggestionField.IntentCategory) {
                SheetSectionTitle(
                    text = stringResource(R.string.os_google_system_service_recommend_section)
                )
                SheetChoiceCard(
                    title = stringResource(R.string.os_google_system_service_category_recommend_explicit_title),
                    summary = stringResource(R.string.os_google_system_service_category_recommend_explicit_summary),
                    selected = explicitCategoryRecommendationSelected,
                    onSelect = onApplyExplicitCategoryRecommendation,
                    accentColor = Color(0xFF16A34A),
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = Color(0xFF16A34A),
                    summaryColor = Color(0xFF16A34A),
                    selectedLabel = null
                )
                SheetChoiceCard(
                    title = stringResource(R.string.os_google_system_service_category_recommend_implicit_title),
                    summary = stringResource(R.string.os_google_system_service_category_recommend_implicit_summary),
                    selected = implicitCategoryRecommendationSelected,
                    onSelect = onApplyImplicitCategoryRecommendation,
                    accentColor = Color(0xFF16A34A),
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = Color(0xFF16A34A),
                    summaryColor = Color(0xFF16A34A),
                    selectedLabel = null
                )
            }
            if (target == ShortcutSuggestionField.PackageName && packageSuggestionsLoading) {
                Text(
                    text = stringResource(R.string.common_loading),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            if (target == ShortcutSuggestionField.ClassName && classSuggestionsLoading) {
                Text(
                    text = stringResource(R.string.common_loading),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            orderedSuggestions.forEach { suggestion ->
                val selected = isCurrentSuggestionSelected(suggestion)
                val leading = when (target) {
                    ShortcutSuggestionField.PackageName -> {
                        @Composable {
                            AppIcon(
                                packageName = suggestion.value.trim(),
                                size = 24.dp
                            )
                        }
                    }

                    ShortcutSuggestionField.ClassName -> {
                        if (suggestion.value.trim().isBlank()) {
                            null
                        } else {
                            @Composable {
                                ShortcutActivityIcon(
                                    packageName = draft.packageName,
                                    className = suggestion.value,
                                    size = 24.dp,
                                    fallbackToPackageIcon = true
                                )
                            }
                        }
                    }

                    else -> null
                }
                SheetChoiceCard(
                    title = suggestion.label,
                    summary = suggestion.summary,
                    selected = selected,
                    onSelect = { onApplySuggestion(suggestion) },
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = when (target) {
                        ShortcutSuggestionField.ClassName -> {
                            if (suggestion.value.trim().isBlank() || suggestion.classItemExported) {
                                Color(0xFFDC2626)
                            } else {
                                Color(0xFF16A34A)
                            }
                        }
                        else -> MiuixTheme.colorScheme.onBackground
                    },
                    summaryColor = when (target) {
                        ShortcutSuggestionField.ClassName -> {
                            if (suggestion.value.trim().isBlank() || suggestion.classItemExported) {
                                Color(0xFFDC2626)
                            } else {
                                Color(0xFF16A34A)
                            }
                        }
                        else -> MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    selectedLabel = null,
                    leading = leading
                )
            }
            if (
                target == ShortcutSuggestionField.PackageName &&
                !packageSuggestionsLoading &&
                suggestions.isEmpty()
            ) {
                Text(
                    text = noMatchedResultsText,
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            if (
                target == ShortcutSuggestionField.ClassName &&
                !classSuggestionsLoading &&
                suggestions.size <= 1
            ) {
                Text(
                    text = noMatchedResultsText,
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            SheetDescriptionText(
                text = stringResource(R.string.os_google_system_service_suggestion_sheet_desc)
            )
        }
    }
}
