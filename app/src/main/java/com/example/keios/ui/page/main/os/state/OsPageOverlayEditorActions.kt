package com.example.keios.ui.page.main.os.state

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.keios.R
import com.example.keios.ui.page.main.os.OsGoogleSystemServiceConfig
import com.example.keios.ui.page.main.os.shell.OsShellCommandCard
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shortcut.OsActivityCardEditMode
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCard
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionField
import com.example.keios.ui.page.main.os.shortcut.ShortcutSuggestionItem
import com.example.keios.ui.page.main.os.shortcut.applyGoogleSystemServiceSuggestion
import com.example.keios.ui.page.main.os.shortcut.applyShortcutImplicitDefaults
import com.example.keios.ui.page.main.os.shortcut.newOsActivityShortcutCardId
import com.example.keios.ui.page.main.os.shortcut.normalizeActivityShortcutConfig

internal data class OsPageOverlayEditorActions(
    val onDeleteShellCommandCard: () -> Unit,
    val onDismissShellCommandCardEditor: () -> Unit,
    val onSaveShellCommandCard: () -> Unit,
    val onOpenActivitySuggestionSheet: (ShortcutSuggestionField) -> Unit,
    val onDeleteActivityCard: () -> Unit,
    val onDismissActivityEditor: () -> Unit,
    val onSaveActivityEditor: () -> Unit,
    val onApplySuggestion: (ShortcutSuggestionItem) -> Unit,
    val onApplyExplicitActionRecommendation: () -> Unit,
    val onApplyImplicitActionRecommendation: () -> Unit,
    val onApplyExplicitCategoryRecommendation: () -> Unit,
    val onApplyImplicitCategoryRecommendation: () -> Unit,
    val onDismissShellCardDeleteConfirm: () -> Unit,
    val onConfirmShellCardDelete: () -> Unit,
    val onDismissActivityCardDeleteConfirm: () -> Unit,
    val onConfirmActivityCardDelete: () -> Unit
)

@Composable
internal fun rememberOsPageOverlayEditorActions(
    context: Context,
    overlayState: OsPageOverlayState,
    activityShortcutCards: List<OsActivityShortcutCard>,
    onActivityShortcutCardsChange: (List<OsActivityShortcutCard>) -> Unit,
    onRemoveActivityCardExpanded: (String) -> Unit,
    googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
    googleSystemServiceDefaultIntentFlags: String,
    onShellCommandCardsChange: (List<OsShellCommandCard>) -> Unit,
    onRemoveShellCommandCardExpanded: (String) -> Unit,
    shellCardCommandRequiredToast: String,
    shellCardSavedToast: String,
    shellCardDeletedToast: String,
    activityCardDeletedToast: String
): OsPageOverlayEditorActions {
    return remember(
        context,
        overlayState,
        activityShortcutCards,
        onActivityShortcutCardsChange,
        onRemoveActivityCardExpanded,
        googleSystemServiceDefaults,
        googleSystemServiceDefaultIntentFlags,
        onShellCommandCardsChange,
        onRemoveShellCommandCardExpanded,
        shellCardCommandRequiredToast,
        shellCardSavedToast,
        shellCardDeletedToast,
        activityCardDeletedToast
    ) {
        OsPageOverlayEditorActions(
            onDeleteShellCommandCard = {
                val targetId = overlayState.editingShellCommandCardId.orEmpty().trim()
                if (targetId.isBlank()) {
                    overlayState.onShowShellCommandCardEditorChange(false)
                    overlayState.onShowShellCardDeleteConfirmChange(false)
                    return@OsPageOverlayEditorActions
                }
                overlayState.onShowShellCardDeleteConfirmChange(true)
            },
            onDismissShellCommandCardEditor = {
                overlayState.onShowShellCommandCardEditorChange(false)
                overlayState.onShowShellCardDeleteConfirmChange(false)
            },
            onSaveShellCommandCard = {
                val targetId = overlayState.editingShellCommandCardId.orEmpty().trim()
                if (targetId.isBlank()) {
                    Toast.makeText(context, shellCardCommandRequiredToast, Toast.LENGTH_SHORT).show()
                    return@OsPageOverlayEditorActions
                }
                val updated = OsShellCommandCardStore.updateCard(
                    cardId = targetId,
                    title = overlayState.shellCommandCardDraft.title,
                    subtitle = overlayState.shellCommandCardDraft.subtitle,
                    command = overlayState.shellCommandCardDraft.command
                )
                if (updated == null) {
                    Toast.makeText(context, shellCardCommandRequiredToast, Toast.LENGTH_SHORT).show()
                    return@OsPageOverlayEditorActions
                }
                onShellCommandCardsChange(OsShellCommandCardStore.loadCards())
                Toast.makeText(context, shellCardSavedToast, Toast.LENGTH_SHORT).show()
                overlayState.onShowShellCommandCardEditorChange(false)
                overlayState.onShowShellCardDeleteConfirmChange(false)
            },
            onOpenActivitySuggestionSheet = { target ->
                overlayState.onGoogleSystemServiceSuggestionTargetChange(target)
                when (target) {
                    ShortcutSuggestionField.PackageName -> {
                        overlayState.onGoogleSystemServicePackageSuggestionQueryChange("")
                    }

                    ShortcutSuggestionField.ClassName -> {
                        overlayState.onGoogleSystemServiceClassSuggestionQueryChange("")
                    }

                    else -> Unit
                }
                overlayState.onShowActivitySuggestionSheetChange(true)
            },
            onDeleteActivityCard = {
                val targetId = overlayState.editingActivityShortcutCardId.orEmpty().trim()
                if (targetId.isBlank()) {
                    overlayState.onShowActivityShortcutEditorChange(false)
                    overlayState.onShowActivityCardDeleteConfirmChange(false)
                    return@OsPageOverlayEditorActions
                }
                overlayState.onShowActivityCardDeleteConfirmChange(true)
            },
            onDismissActivityEditor = {
                overlayState.onShowActivityShortcutEditorChange(false)
                overlayState.onShowActivityCardDeleteConfirmChange(false)
                overlayState.onEditingActivityShortcutBuiltInChange(false)
            },
            onSaveActivityEditor = {
                val normalized = normalizeActivityShortcutConfig(
                    config = overlayState.activityShortcutDraft,
                    defaults = googleSystemServiceDefaults
                )
                val updatedCards = if (overlayState.activityCardEditMode == OsActivityCardEditMode.Add) {
                    activityShortcutCards + OsActivityShortcutCard(
                        id = newOsActivityShortcutCardId(),
                        visible = true,
                        isBuiltInSample = false,
                        config = normalized
                    )
                } else {
                    val targetId = overlayState.editingActivityShortcutCardId
                    if (targetId.isNullOrBlank()) {
                        activityShortcutCards + OsActivityShortcutCard(
                            id = newOsActivityShortcutCardId(),
                            visible = true,
                            isBuiltInSample = false,
                            config = normalized
                        )
                    } else {
                        activityShortcutCards.map { card ->
                            if (card.id == targetId) card.copy(config = normalized) else card
                        }
                    }
                }
                onActivityShortcutCardsChange(updatedCards)
                OsActivityShortcutCardStore.saveCards(
                    cards = updatedCards,
                    defaults = googleSystemServiceDefaults
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.os_google_system_service_toast_saved),
                    Toast.LENGTH_SHORT
                ).show()
                overlayState.onShowActivityShortcutEditorChange(false)
                overlayState.onShowActivityCardDeleteConfirmChange(false)
                overlayState.onEditingActivityShortcutBuiltInChange(false)
            },
            onApplySuggestion = { suggestion ->
                overlayState.onActivityShortcutDraftChange(
                    applyGoogleSystemServiceSuggestion(
                        draft = overlayState.activityShortcutDraft,
                        target = overlayState.googleSystemServiceSuggestionTarget,
                        item = suggestion,
                        defaultIntentFlags = googleSystemServiceDefaultIntentFlags
                    )
                )
                overlayState.onShowActivitySuggestionSheetChange(false)
            },
            onApplyExplicitActionRecommendation = {
                overlayState.onActivityShortcutDraftChange(
                    overlayState.activityShortcutDraft.copy(intentAction = Intent.ACTION_VIEW)
                )
            },
            onApplyImplicitActionRecommendation = {
                overlayState.onActivityShortcutDraftChange(
                    applyShortcutImplicitDefaults(
                        draft = overlayState.activityShortcutDraft,
                        defaultIntentFlags = googleSystemServiceDefaultIntentFlags
                    )
                )
            },
            onApplyExplicitCategoryRecommendation = {
                overlayState.onActivityShortcutDraftChange(
                    overlayState.activityShortcutDraft.copy(intentCategory = "")
                )
            },
            onApplyImplicitCategoryRecommendation = {
                overlayState.onActivityShortcutDraftChange(
                    applyShortcutImplicitDefaults(
                        draft = overlayState.activityShortcutDraft,
                        defaultIntentFlags = googleSystemServiceDefaultIntentFlags
                    )
                )
            },
            onDismissShellCardDeleteConfirm = {
                overlayState.onShowShellCardDeleteConfirmChange(false)
            },
            onConfirmShellCardDelete = {
                val targetId = overlayState.editingShellCommandCardId.orEmpty().trim()
                overlayState.onShowShellCardDeleteConfirmChange(false)
                if (targetId.isBlank()) return@OsPageOverlayEditorActions
                onShellCommandCardsChange(OsShellCommandCardStore.deleteCard(targetId))
                onRemoveShellCommandCardExpanded(targetId)
                overlayState.onEditingShellCommandCardIdChange(null)
                overlayState.onShowShellCommandCardEditorChange(false)
                Toast.makeText(context, shellCardDeletedToast, Toast.LENGTH_SHORT).show()
            },
            onDismissActivityCardDeleteConfirm = {
                overlayState.onShowActivityCardDeleteConfirmChange(false)
            },
            onConfirmActivityCardDelete = {
                val targetId = overlayState.editingActivityShortcutCardId.orEmpty().trim()
                overlayState.onShowActivityCardDeleteConfirmChange(false)
                if (targetId.isBlank()) return@OsPageOverlayEditorActions
                val updatedCards = activityShortcutCards.filterNot { card -> card.id == targetId }
                onActivityShortcutCardsChange(updatedCards)
                onRemoveActivityCardExpanded(targetId)
                OsActivityShortcutCardStore.saveCards(
                    cards = updatedCards,
                    defaults = googleSystemServiceDefaults
                )
                overlayState.onEditingActivityShortcutCardIdChange(null)
                overlayState.onShowActivityShortcutEditorChange(false)
                overlayState.onShowActivitySuggestionSheetChange(false)
                overlayState.onEditingActivityShortcutBuiltInChange(false)
                Toast.makeText(context, activityCardDeletedToast, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
