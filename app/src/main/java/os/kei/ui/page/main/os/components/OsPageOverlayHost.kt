@file:Suppress("FunctionName")

package os.kei.ui.page.main.os.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.OsActivitySuggestionChromeState
import os.kei.ui.page.main.os.OsActivitySuggestionUiState
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.OsPageViewModel
import os.kei.ui.page.main.os.OsSectionCard
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.defaultOsShellCommandCardTitle
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.createDefaultActivityShortcutDraft
import os.kei.ui.page.main.os.shortcut.ensureEditorActivityShortcutDraft
import os.kei.ui.page.main.os.shortcut.normalizeActivityShortcutConfig
import os.kei.ui.page.main.os.state.OsPageCardTransferState
import os.kei.ui.page.main.os.state.OsPageOverlayState
import os.kei.ui.page.main.os.state.rememberOsPageOverlayEditorActions

@Composable
internal fun OsPageOverlayHost(
    context: Context,
    sheetBackdrop: Backdrop,
    overlayState: OsPageOverlayState,
    editorDataReady: Boolean,
    visibleCardsTitle: String,
    visibleCardsHint: String,
    visibleCards: Set<OsSectionCard>,
    applyCardVisibility: (OsSectionCard, Boolean) -> Unit,
    visibleActivitiesTitle: String,
    visibleActivitiesDesc: String,
    activityShortcutCards: List<OsActivityShortcutCard>,
    activityIconBitmaps: Map<String, Bitmap>,
    packageIconBitmaps: Map<String, Bitmap>,
    defaultActivityCardTitle: String,
    cardTransferInProgress: Boolean,
    cardTransferState: OsPageCardTransferState,
    onExportAllActivityCards: () -> Unit,
    onImportAllActivityCards: () -> Unit,
    applyActivityCardVisibility: (String, Boolean) -> Unit,
    visibleShellCardsTitle: String,
    visibleShellCardsDesc: String,
    shellRunnerVisible: Boolean,
    shellCommandCards: List<OsShellCommandCard>,
    builtInShellCommandCards: List<OsShellCommandCard>,
    onExportAllShellCards: () -> Unit,
    onImportAllShellCards: () -> Unit,
    applyShellCommandCardVisibility: (String, Boolean) -> Unit,
    editShellCommandCardTitle: String,
    shellCardCommandRequiredToast: String,
    shellCardDeleteDialogTitle: String,
    addActivityCardTitle: String,
    editActivityCardTitle: String,
    noMatchedResultsText: String,
    activitySuggestionState: OsActivitySuggestionUiState,
    activitySuggestionChromeState: OsActivitySuggestionChromeState,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    googleSystemServiceDefaultTitle: String,
    googleSystemServiceDefaultIntentFlags: String,
    activityCardDeleteDialogTitle: String,
    osPageViewModel: OsPageViewModel,
) {
    val editorActions =
        rememberOsPageOverlayEditorActions(
            context = context,
            osPageViewModel = osPageViewModel,
            overlayState = overlayState,
            activitySuggestionTarget = activitySuggestionChromeState.target,
            googleSystemServiceDefaults = googleSystemServiceDefaults,
            googleSystemServiceDefaultIntentFlags = googleSystemServiceDefaultIntentFlags,
            shellCardCommandRequiredToast = shellCardCommandRequiredToast,
            builtInShellCommandCards = builtInShellCommandCards,
        )
    val editedShellCommandCard =
        shellCommandCards.firstOrNull { card ->
            card.id == overlayState.editingShellCommandCardId
        }
    val shellCommandCardHasUnsavedChanges =
        overlayState.showShellCommandCardEditor &&
            editedShellCommandCard != null &&
            shellCommandCardEditorSnapshot(overlayState.shellCommandCardDraft) !=
            shellCommandCardEditorSnapshot(editedShellCommandCard)
    val savedActivityShortcutDraft =
        if (overlayState.activityCardEditMode == OsActivityCardEditMode.Add) {
            createDefaultActivityShortcutDraft(googleSystemServiceDefaults)
        } else {
            activityShortcutCards
                .firstOrNull { card ->
                    card.id == overlayState.editingActivityShortcutCardId
                }?.config
                ?.toActivityShortcutEditorDraft(googleSystemServiceDefaults)
                ?: createDefaultActivityShortcutDraft(googleSystemServiceDefaults)
        }
    val activityShortcutHasUnsavedChanges =
        overlayState.showActivityShortcutEditor &&
            overlayState.activityShortcutDraft.toActivityShortcutDirtySnapshot(googleSystemServiceDefaults) !=
            savedActivityShortcutDraft.toActivityShortcutDirtySnapshot(googleSystemServiceDefaults)

    OsPageOverlaySheets(
        showCardManager = overlayState.showCardManager,
        visibleCardsTitle = visibleCardsTitle,
        sheetBackdrop = sheetBackdrop,
        cardsHintText = visibleCardsHint,
        visibleCards = visibleCards,
        onDismissCardManager = { overlayState.onShowCardManagerChange(false) },
        onCardVisibilityChange = { card, checked ->
            applyCardVisibility(card, checked)
        },
        showActivityVisibilityManager = overlayState.showActivityVisibilityManager,
        visibleActivitiesTitle = visibleActivitiesTitle,
        activityHintText = visibleActivitiesDesc,
        activityShortcutCards = activityShortcutCards,
        activityIconBitmaps = activityIconBitmaps,
        packageIconBitmaps = packageIconBitmaps,
        defaultActivityCardTitle = defaultActivityCardTitle,
        cardTransferInProgress = cardTransferInProgress,
        pendingCardImportPreview = overlayState.pendingCardImportPreview,
        onExportAllActivityCards = onExportAllActivityCards,
        onImportAllActivityCards = onImportAllActivityCards,
        onDismissActivityVisibilityManager = { overlayState.onShowActivityVisibilityManagerChange(false) },
        activityVisibilityQuery = overlayState.activityVisibilityQuery,
        onActivityVisibilityQueryChange = overlayState.onActivityVisibilityQueryChange,
        onActivityCardVisibilityChange = { cardId, checked ->
            applyActivityCardVisibility(cardId, checked)
        },
        showShellCardVisibilityManager = overlayState.showShellCardVisibilityManager,
        visibleShellCardsTitle = visibleShellCardsTitle,
        visibleShellCardsDesc = visibleShellCardsDesc,
        shellRunnerVisible = shellRunnerVisible,
        onShellRunnerVisibilityChange = { checked ->
            applyCardVisibility(OsSectionCard.SHELL_RUNNER, checked)
        },
        shellCommandCards = shellCommandCards,
        onExportAllShellCards = onExportAllShellCards,
        onImportAllShellCards = onImportAllShellCards,
        onDismissShellVisibilityManager = { overlayState.onShowShellCardVisibilityManagerChange(false) },
        shellCardVisibilityQuery = overlayState.shellCardVisibilityQuery,
        onShellCardVisibilityQueryChange = overlayState.onShellCardVisibilityQueryChange,
        onShellCommandCardVisibilityChange = { cardId, checked ->
            applyShellCommandCardVisibility(cardId, checked)
        },
        showShellCommandCardEditor = overlayState.showShellCommandCardEditor && editorDataReady,
        editShellCommandCardTitle = editShellCommandCardTitle,
        shellCommandCardDraft = overlayState.shellCommandCardDraft,
        onShellCommandCardDraftChange = overlayState.onShellCommandCardDraftChange,
        showShellCardDeleteAction = !overlayState.editingShellCommandCardId.isNullOrBlank(),
        shellCommandCardHasUnsavedChanges = shellCommandCardHasUnsavedChanges,
        onDeleteShellCommandCard = editorActions.onDeleteShellCommandCard,
        onDismissShellCommandCardEditor = editorActions.onDismissShellCommandCardEditor,
        onDismissShellCommandCardEditorFinished = editorActions.onDismissShellCommandCardEditorFinished,
        onSaveShellCommandCard = editorActions.onSaveShellCommandCard,
        showActivityShortcutEditor = overlayState.showActivityShortcutEditor && editorDataReady,
        activityEditorTitle =
            if (overlayState.activityCardEditMode == OsActivityCardEditMode.Add) {
                addActivityCardTitle
            } else {
                editActivityCardTitle
            },
        activityShortcutDraft = overlayState.activityShortcutDraft,
        onActivityShortcutDraftChange = overlayState.onActivityShortcutDraftChange,
        onOpenActivitySuggestionSheet = editorActions.onOpenActivitySuggestionSheet,
        showBuiltInActivityCardBadge = overlayState.editingActivityShortcutBuiltIn,
        showDeleteActivityAction =
            overlayState.activityCardEditMode == OsActivityCardEditMode.Edit &&
                !overlayState.editingActivityShortcutCardId.isNullOrBlank(),
        activityShortcutHasUnsavedChanges = activityShortcutHasUnsavedChanges,
        onDeleteActivityCard = editorActions.onDeleteActivityCard,
        onDismissActivityEditor = editorActions.onDismissActivityEditor,
        onDismissActivityEditorFinished = editorActions.onDismissActivityEditorFinished,
        onSaveActivityEditor = editorActions.onSaveActivityEditor,
        showActivitySuggestionSheet = activitySuggestionChromeState.showSheet,
        suggestionTarget = activitySuggestionChromeState.target,
        packageSuggestions = activitySuggestionState.packageSuggestions,
        packageSuggestionsLoading = activitySuggestionState.packageSuggestionsLoading,
        packageSuggestionQuery = activitySuggestionChromeState.packageQuery,
        onPackageSuggestionQueryChange = osPageViewModel::updateActivityPackageSuggestionQuery,
        classSuggestions = activitySuggestionState.classSuggestions,
        classSuggestionsLoading = activitySuggestionState.classSuggestionsLoading,
        classSuggestionQuery = activitySuggestionChromeState.classQuery,
        onClassSuggestionQueryChange = osPageViewModel::updateActivityClassSuggestionQuery,
        noMatchedResultsText = noMatchedResultsText,
        onDismissSuggestionSheet = osPageViewModel::dismissActivitySuggestionSheet,
        onApplySuggestion = editorActions.onApplySuggestion,
        onApplyExplicitActionRecommendation = editorActions.onApplyExplicitActionRecommendation,
        onApplyImplicitActionRecommendation = editorActions.onApplyImplicitActionRecommendation,
        onApplyExplicitCategoryRecommendation = editorActions.onApplyExplicitCategoryRecommendation,
        onApplyImplicitCategoryRecommendation = editorActions.onApplyImplicitCategoryRecommendation,
        showShellCardDeleteConfirm = overlayState.showShellCardDeleteConfirm,
        shellCardDeleteDialogTitle = shellCardDeleteDialogTitle,
        shellCardDeleteDialogSummary =
            context.getString(
                R.string.os_shell_card_delete_dialog_summary,
                overlayState.shellCommandCardDraft.title.ifBlank {
                    defaultOsShellCommandCardTitle(overlayState.shellCommandCardDraft.command)
                },
            ),
        onDismissShellCardDeleteConfirm = editorActions.onDismissShellCardDeleteConfirm,
        onConfirmShellCardDelete = editorActions.onConfirmShellCardDelete,
        showActivityCardDeleteConfirm = overlayState.showActivityCardDeleteConfirm,
        activityCardDeleteDialogTitle = activityCardDeleteDialogTitle,
        activityCardDeleteDialogSummary =
            context.getString(
                R.string.os_activity_card_delete_dialog_summary,
                overlayState.activityShortcutDraft.title.ifBlank { googleSystemServiceDefaultTitle },
            ),
        onDismissActivityCardDeleteConfirm = editorActions.onDismissActivityCardDeleteConfirm,
        onConfirmActivityCardDelete = editorActions.onConfirmActivityCardDelete,
        onDismissCardImportPreview = {
            if (!cardTransferInProgress) {
                overlayState.onPendingCardImportPreviewChange(null)
            }
        },
        onCancelCardImportPreview = {
            if (!cardTransferInProgress) {
                overlayState.onPendingCardImportPreviewChange(null)
            }
        },
        onConfirmCardImportPreview = cardTransferState.confirmImport,
    )
}

private data class ShellCommandCardEditorSnapshot(
    val title: String,
    val subtitle: String,
    val command: String,
)

private fun shellCommandCardEditorSnapshot(card: OsShellCommandCard): ShellCommandCardEditorSnapshot {
    val normalizedCommand = card.command.trim()
    return ShellCommandCardEditorSnapshot(
        title = card.title.trim().ifBlank { defaultOsShellCommandCardTitle(normalizedCommand) },
        subtitle = card.subtitle.trim(),
        command = normalizedCommand,
    )
}

private fun OsGoogleSystemServiceConfig.toActivityShortcutEditorDraft(defaults: OsGoogleSystemServiceConfig): OsGoogleSystemServiceConfig =
    ensureEditorActivityShortcutDraft(
        normalizeActivityShortcutConfig(
            config = this,
            defaults = defaults,
        ),
    )

private fun OsGoogleSystemServiceConfig.toActivityShortcutDirtySnapshot(
    defaults: OsGoogleSystemServiceConfig,
): OsGoogleSystemServiceConfig =
    normalizeActivityShortcutConfig(
        config = this,
        defaults = defaults,
    )
