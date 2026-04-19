package com.example.keios.ui.page.main

import androidx.compose.runtime.Composable
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
internal fun OsActivityShortcutEditorHost(
    showEditor: Boolean,
    editorTitle: String,
    sheetBackdrop: LayerBackdrop,
    draft: OsGoogleSystemServiceConfig,
    onDraftChange: (OsGoogleSystemServiceConfig) -> Unit,
    onOpenSuggestionSheet: (ShortcutSuggestionField) -> Unit,
    showDeleteAction: Boolean,
    onDeleteEditor: () -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
    showSuggestionSheet: Boolean,
    suggestionTarget: ShortcutSuggestionField,
    packageSuggestions: List<ShortcutInstalledAppOption>,
    packageSuggestionsLoading: Boolean,
    packageSuggestionQuery: String,
    onPackageSuggestionQueryChange: (String) -> Unit,
    classSuggestions: List<ShortcutActivityClassOption>,
    classSuggestionsLoading: Boolean,
    classSuggestionQuery: String,
    onClassSuggestionQueryChange: (String) -> Unit,
    noMatchedResultsText: String,
    onDismissSuggestionSheet: () -> Unit,
    onApplySuggestion: (ShortcutSuggestionItem) -> Unit,
    onApplyExplicitActionRecommendation: () -> Unit,
    onApplyImplicitActionRecommendation: () -> Unit,
    onApplyExplicitCategoryRecommendation: () -> Unit,
    onApplyImplicitCategoryRecommendation: () -> Unit
) {
    OsGoogleSystemServiceEditorSheet(
        show = showEditor,
        title = editorTitle,
        sheetBackdrop = sheetBackdrop,
        draft = draft,
        onDraftChange = onDraftChange,
        onOpenSuggestionSheet = onOpenSuggestionSheet,
        showDeleteAction = showDeleteAction,
        onDelete = onDeleteEditor,
        onDismissRequest = onDismissEditor,
        onSave = onSaveEditor
    )

    OsGoogleSystemServiceSuggestionSheet(
        show = showSuggestionSheet,
        target = suggestionTarget,
        draft = draft,
        sheetBackdrop = sheetBackdrop,
        packageSuggestions = packageSuggestions,
        packageSuggestionsLoading = packageSuggestionsLoading,
        packageSuggestionQuery = packageSuggestionQuery,
        onPackageSuggestionQueryChange = onPackageSuggestionQueryChange,
        classSuggestions = classSuggestions,
        classSuggestionsLoading = classSuggestionsLoading,
        classSuggestionQuery = classSuggestionQuery,
        onClassSuggestionQueryChange = onClassSuggestionQueryChange,
        noMatchedResultsText = noMatchedResultsText,
        onDismissRequest = onDismissSuggestionSheet,
        onApplySuggestion = onApplySuggestion,
        onApplyExplicitActionRecommendation = onApplyExplicitActionRecommendation,
        onApplyImplicitActionRecommendation = onApplyImplicitActionRecommendation,
        onApplyExplicitCategoryRecommendation = onApplyExplicitCategoryRecommendation,
        onApplyImplicitCategoryRecommendation = onApplyImplicitCategoryRecommendation
    )
}
