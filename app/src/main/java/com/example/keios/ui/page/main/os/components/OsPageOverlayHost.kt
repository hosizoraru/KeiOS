package com.example.keios.ui.page.main.os.components

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.keios.R
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.state.OsPageOverlayState
import com.example.keios.ui.page.main.os.state.rememberOsPageOverlayEditorActions
import com.example.keios.ui.page.main.os.OsSectionCard
import com.example.keios.ui.page.main.os.shell.OsShellCommandCard
import com.example.keios.ui.page.main.os.shell.defaultOsShellCommandCardTitle
import com.example.keios.ui.page.main.os.shortcut.OsActivityCardEditMode
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCard
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun OsPageOverlayHost(
    context: Context,
    scope: CoroutineScope,
    sheetBackdrop: LayerBackdrop,
    overlayState: OsPageOverlayState,
    visibleCardsTitle: String,
    visibleCardsHint: String,
    visibleCards: Set<OsSectionCard>,
    applyCardVisibility: suspend (OsSectionCard, Boolean) -> Unit,
    visibleActivitiesTitle: String,
    visibleActivitiesDesc: String,
    activityShortcutCards: List<OsActivityShortcutCard>,
    defaultActivityCardTitle: String,
    cardTransferInProgress: Boolean,
    onExportAllActivityCards: () -> Unit,
    onImportAllActivityCards: () -> Unit,
    applyActivityCardVisibility: suspend (String, Boolean) -> Unit,
    visibleShellCardsTitle: String,
    visibleShellCardsDesc: String,
    shellRunnerVisible: Boolean,
    shellCommandCards: List<OsShellCommandCard>,
    onExportAllShellCards: () -> Unit,
    onImportAllShellCards: () -> Unit,
    applyShellCommandCardVisibility: suspend (String, Boolean) -> Unit,
    editShellCommandCardTitle: String,
    onShellCommandCardsChange: (List<OsShellCommandCard>) -> Unit,
    onRemoveShellCommandCardExpanded: (String) -> Unit,
    shellCardCommandRequiredToast: String,
    shellCardSavedToast: String,
    shellCardDeletedToast: String,
    shellCardDeleteDialogTitle: String,
    addActivityCardTitle: String,
    editActivityCardTitle: String,
    noMatchedResultsText: String,
    onActivityShortcutCardsChange: (List<OsActivityShortcutCard>) -> Unit,
    onRemoveActivityCardExpanded: (String) -> Unit,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    googleSystemServiceDefaultTitle: String,
    googleSystemServiceDefaultIntentFlags: String,
    activityCardDeletedToast: String,
    activityCardDeleteDialogTitle: String
) {
    val editorActions = rememberOsPageOverlayEditorActions(
        context = context,
        overlayState = overlayState,
        activityShortcutCards = activityShortcutCards,
        onActivityShortcutCardsChange = onActivityShortcutCardsChange,
        onRemoveActivityCardExpanded = onRemoveActivityCardExpanded,
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        googleSystemServiceDefaultIntentFlags = googleSystemServiceDefaultIntentFlags,
        onShellCommandCardsChange = onShellCommandCardsChange,
        onRemoveShellCommandCardExpanded = onRemoveShellCommandCardExpanded,
        shellCardCommandRequiredToast = shellCardCommandRequiredToast,
        shellCardSavedToast = shellCardSavedToast,
        shellCardDeletedToast = shellCardDeletedToast,
        activityCardDeletedToast = activityCardDeletedToast
    )

    OsPageOverlaySheets(
        showCardManager = overlayState.showCardManager,
        visibleCardsTitle = visibleCardsTitle,
        sheetBackdrop = sheetBackdrop,
        cardsHintText = visibleCardsHint,
        visibleCards = visibleCards,
        onDismissCardManager = { overlayState.onShowCardManagerChange(false) },
        onCardVisibilityChange = { card, checked ->
            scope.launch { applyCardVisibility(card, checked) }
        },
        showActivityVisibilityManager = overlayState.showActivityVisibilityManager,
        visibleActivitiesTitle = visibleActivitiesTitle,
        activityHintText = visibleActivitiesDesc,
        activityShortcutCards = activityShortcutCards,
        defaultActivityCardTitle = defaultActivityCardTitle,
        cardTransferInProgress = cardTransferInProgress,
        onExportAllActivityCards = onExportAllActivityCards,
        onImportAllActivityCards = onImportAllActivityCards,
        onDismissActivityVisibilityManager = { overlayState.onShowActivityVisibilityManagerChange(false) },
        onActivityCardVisibilityChange = { cardId, checked ->
            scope.launch { applyActivityCardVisibility(cardId, checked) }
        },
        showShellCardVisibilityManager = overlayState.showShellCardVisibilityManager,
        visibleShellCardsTitle = visibleShellCardsTitle,
        visibleShellCardsDesc = visibleShellCardsDesc,
        shellRunnerVisible = shellRunnerVisible,
        onShellRunnerVisibilityChange = { checked ->
            scope.launch { applyCardVisibility(OsSectionCard.SHELL_RUNNER, checked) }
        },
        shellCommandCards = shellCommandCards,
        onExportAllShellCards = onExportAllShellCards,
        onImportAllShellCards = onImportAllShellCards,
        onDismissShellVisibilityManager = { overlayState.onShowShellCardVisibilityManagerChange(false) },
        onShellCommandCardVisibilityChange = { cardId, checked ->
            scope.launch { applyShellCommandCardVisibility(cardId, checked) }
        },
        showShellCommandCardEditor = overlayState.showShellCommandCardEditor,
        editShellCommandCardTitle = editShellCommandCardTitle,
        shellCommandCardDraft = overlayState.shellCommandCardDraft,
        onShellCommandCardDraftChange = overlayState.onShellCommandCardDraftChange,
        showShellCardDeleteAction = !overlayState.editingShellCommandCardId.isNullOrBlank(),
        onDeleteShellCommandCard = editorActions.onDeleteShellCommandCard,
        onDismissShellCommandCardEditor = editorActions.onDismissShellCommandCardEditor,
        onSaveShellCommandCard = editorActions.onSaveShellCommandCard,
        showActivityShortcutEditor = overlayState.showActivityShortcutEditor,
        activityEditorTitle = if (overlayState.activityCardEditMode == OsActivityCardEditMode.Add) {
            addActivityCardTitle
        } else {
            editActivityCardTitle
        },
        activityShortcutDraft = overlayState.activityShortcutDraft,
        onActivityShortcutDraftChange = overlayState.onActivityShortcutDraftChange,
        onOpenActivitySuggestionSheet = editorActions.onOpenActivitySuggestionSheet,
        showBuiltInActivityCardBadge = overlayState.editingActivityShortcutBuiltIn,
        showDeleteActivityAction = overlayState.activityCardEditMode == OsActivityCardEditMode.Edit &&
            !overlayState.editingActivityShortcutCardId.isNullOrBlank(),
        onDeleteActivityCard = editorActions.onDeleteActivityCard,
        onDismissActivityEditor = editorActions.onDismissActivityEditor,
        onSaveActivityEditor = editorActions.onSaveActivityEditor,
        showActivitySuggestionSheet = overlayState.showActivitySuggestionSheet,
        suggestionTarget = overlayState.googleSystemServiceSuggestionTarget,
        packageSuggestions = overlayState.googleSystemServicePackageSuggestions,
        packageSuggestionsLoading = overlayState.googleSystemServicePackageSuggestionsLoading,
        packageSuggestionQuery = overlayState.googleSystemServicePackageSuggestionQuery,
        onPackageSuggestionQueryChange = overlayState.onGoogleSystemServicePackageSuggestionQueryChange,
        classSuggestions = overlayState.googleSystemServiceClassSuggestions,
        classSuggestionsLoading = overlayState.googleSystemServiceClassSuggestionsLoading,
        classSuggestionQuery = overlayState.googleSystemServiceClassSuggestionQuery,
        onClassSuggestionQueryChange = overlayState.onGoogleSystemServiceClassSuggestionQueryChange,
        noMatchedResultsText = noMatchedResultsText,
        onDismissSuggestionSheet = { overlayState.onShowActivitySuggestionSheetChange(false) },
        onApplySuggestion = editorActions.onApplySuggestion,
        onApplyExplicitActionRecommendation = editorActions.onApplyExplicitActionRecommendation,
        onApplyImplicitActionRecommendation = editorActions.onApplyImplicitActionRecommendation,
        onApplyExplicitCategoryRecommendation = editorActions.onApplyExplicitCategoryRecommendation,
        onApplyImplicitCategoryRecommendation = editorActions.onApplyImplicitCategoryRecommendation,
        showShellCardDeleteConfirm = overlayState.showShellCardDeleteConfirm,
        shellCardDeleteDialogTitle = shellCardDeleteDialogTitle,
        shellCardDeleteDialogSummary = context.getString(
            R.string.os_shell_card_delete_dialog_summary,
            overlayState.shellCommandCardDraft.title.ifBlank {
                defaultOsShellCommandCardTitle(overlayState.shellCommandCardDraft.command)
            }
        ),
        onDismissShellCardDeleteConfirm = editorActions.onDismissShellCardDeleteConfirm,
        onConfirmShellCardDelete = editorActions.onConfirmShellCardDelete,
        showActivityCardDeleteConfirm = overlayState.showActivityCardDeleteConfirm,
        activityCardDeleteDialogTitle = activityCardDeleteDialogTitle,
        activityCardDeleteDialogSummary = context.getString(
            R.string.os_activity_card_delete_dialog_summary,
            overlayState.activityShortcutDraft.title.ifBlank { googleSystemServiceDefaultTitle }
        ),
        onDismissActivityCardDeleteConfirm = editorActions.onDismissActivityCardDeleteConfirm,
        onConfirmActivityCardDelete = editorActions.onConfirmActivityCardDelete
    )
}
