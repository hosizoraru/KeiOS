package com.example.keios.ui.page.main.os.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.github.AppIcon
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.appLucideCloseIcon
import com.example.keios.ui.page.main.os.shortcut.ShortcutActivityClassOption
import com.example.keios.ui.page.main.os.shortcut.ShortcutActivityIcon
import com.example.keios.ui.page.main.os.shortcut.ShortcutInstalledAppOption
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionField
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionItem
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassSearchField
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.sheet.SheetChoiceCard
import com.example.keios.ui.page.main.widget.sheet.SheetContentColumn
import com.example.keios.ui.page.main.widget.sheet.SheetDescriptionText
import com.example.keios.ui.page.main.widget.sheet.SheetSectionTitle
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val uiState = rememberOsGoogleSystemServiceSuggestionUiState(
        target = target,
        draft = draft,
        packageSuggestions = packageSuggestions,
        packageSuggestionsLoading = packageSuggestionsLoading,
        packageSuggestionQuery = packageSuggestionQuery,
        classSuggestions = classSuggestions,
        classSuggestionsLoading = classSuggestionsLoading,
        classSuggestionQuery = classSuggestionQuery
    )

    SnapshotWindowBottomSheet(
        show = show,
        title = uiState.sheetTitle,
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
        SheetContentColumn(verticalSpacing = 10.dp) {
            if (uiState.showPackageSearch) {
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
            if (uiState.showClassSearch) {
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
            if (uiState.showActionRecommendations) {
                SheetSectionTitle(
                    text = stringResource(R.string.os_google_system_service_recommend_section)
                )
                SheetChoiceCard(
                    title = stringResource(R.string.os_google_system_service_action_recommend_explicit_title),
                    summary = stringResource(R.string.os_google_system_service_action_recommend_explicit_summary),
                    selected = uiState.explicitActionRecommendationSelected,
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
                    selected = uiState.implicitActionRecommendationSelected,
                    onSelect = onApplyImplicitActionRecommendation,
                    accentColor = Color(0xFF16A34A),
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = Color(0xFF16A34A),
                    summaryColor = Color(0xFF16A34A),
                    selectedLabel = null
                )
            }
            if (uiState.showCategoryRecommendations) {
                SheetSectionTitle(
                    text = stringResource(R.string.os_google_system_service_recommend_section)
                )
                SheetChoiceCard(
                    title = stringResource(R.string.os_google_system_service_category_recommend_explicit_title),
                    summary = stringResource(R.string.os_google_system_service_category_recommend_explicit_summary),
                    selected = uiState.explicitCategoryRecommendationSelected,
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
                    selected = uiState.implicitCategoryRecommendationSelected,
                    onSelect = onApplyImplicitCategoryRecommendation,
                    accentColor = Color(0xFF16A34A),
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = Color(0xFF16A34A),
                    summaryColor = Color(0xFF16A34A),
                    selectedLabel = null
                )
            }
            if (uiState.showPackageLoading) {
                Text(
                    text = stringResource(R.string.common_loading),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            if (uiState.showClassLoading) {
                Text(
                    text = stringResource(R.string.common_loading),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            uiState.orderedSuggestions.forEach { suggestion ->
                val selected = uiState.isCurrentSuggestionSelected(suggestion)
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
                val classSuggestionWarning = target == ShortcutSuggestionField.ClassName &&
                    (suggestion.value.trim().isBlank() || suggestion.classItemExported)
                SheetChoiceCard(
                    title = suggestion.label,
                    summary = suggestion.summary,
                    selected = selected,
                    onSelect = { onApplySuggestion(suggestion) },
                    selectedAccentColor = MiuixTheme.colorScheme.primary,
                    unselectedTitleColor = if (classSuggestionWarning) {
                        Color(0xFFDC2626)
                    } else if (target == ShortcutSuggestionField.ClassName) {
                        Color(0xFF16A34A)
                    } else {
                        MiuixTheme.colorScheme.onBackground
                    },
                    summaryColor = if (classSuggestionWarning) {
                        Color(0xFFDC2626)
                    } else if (target == ShortcutSuggestionField.ClassName) {
                        Color(0xFF16A34A)
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    selectedLabel = null,
                    leading = leading
                )
            }
            if (uiState.showPackageNoResult || uiState.showClassNoResult) {
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
