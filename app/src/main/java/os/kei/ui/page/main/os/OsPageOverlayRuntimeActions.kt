package os.kei.ui.page.main.os

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.state.OsCardImportTarget
import os.kei.ui.page.main.os.transfer.OsCardImportPreview

internal class OsPageOverlayRuntimeActions(
    private val runtimeMutableState: MutableStateFlow<OsPageRuntimeState>,
    private val persistEditor: (OsPageRuntimeState) -> Unit = {},
) {
    private fun updateEditor(transform: (OsPageRuntimeState) -> OsPageRuntimeState) {
        persistEditor(runtimeMutableState.updateAndGet(transform))
    }

    fun openActivityEditor(request: OsActivityShortcutEditorRequest) {
        updateEditor { state ->
            state.copy(
                showActivityShortcutEditor = true,
                showShellCommandCardEditor = false,
                activityCardEditMode = request.editMode,
                editingActivityShortcutCardId = request.editingCardId,
                editingActivityShortcutBuiltIn = request.editingBuiltIn,
                activityShortcutDraft = request.draft,
            )
        }
    }

    fun openShellEditor(card: OsShellCommandCard) {
        updateEditor { state ->
            state.copy(
                showActivityShortcutEditor = false,
                showShellCommandCardEditor = true,
                editingShellCommandCardId = card.id,
                shellCommandCardDraft = card,
            )
        }
    }

    fun reconcileEditorTargets(persistent: OsPagePersistentState) {
        if (!persistent.loaded) return
        updateEditor { state ->
            state.copy(
                showActivityShortcutEditor = state.showActivityShortcutEditor &&
                    (state.activityCardEditMode == OsActivityCardEditMode.Add ||
                        persistent.activityShortcutCards.any { it.id == state.editingActivityShortcutCardId }),
                showShellCommandCardEditor = state.showShellCommandCardEditor &&
                    persistent.shellCommandCards.any { it.id == state.editingShellCommandCardId },
            )
        }
    }

    fun updateActivityShortcutDraft(draft: OsGoogleSystemServiceConfig) {
        updateEditor { it.copy(activityShortcutDraft = draft) }
    }

    fun updateShellCommandCardDraft(draft: OsShellCommandCard) {
        updateEditor { it.copy(shellCommandCardDraft = draft) }
    }

    fun updatePendingExportContent(content: String?) {
        runtimeMutableState.update { state ->
            if (state.pendingExportContent == content) state else state.copy(pendingExportContent = content)
        }
    }

    fun updateShowCardManager(show: Boolean) {
        runtimeMutableState.update { state ->
            if (state.showCardManager == show) state else state.copy(showCardManager = show)
        }
    }

    fun updateShowActivityVisibilityManager(show: Boolean) {
        runtimeMutableState.update { state ->
            if (state.showActivityVisibilityManager == show) {
                state
            } else {
                state.copy(
                    showActivityVisibilityManager = show,
                    activityVisibilityQuery = if (show) state.activityVisibilityQuery else "",
                )
            }
        }
    }

    fun updateActivityVisibilityQuery(query: String) {
        runtimeMutableState.update { state ->
            if (state.activityVisibilityQuery == query) {
                state
            } else {
                state.copy(activityVisibilityQuery = query)
            }
        }
    }

    fun updateShowShellCardVisibilityManager(show: Boolean) {
        runtimeMutableState.update { state ->
            if (state.showShellCardVisibilityManager == show) {
                state
            } else {
                state.copy(
                    showShellCardVisibilityManager = show,
                    shellCardVisibilityQuery = if (show) state.shellCardVisibilityQuery else "",
                )
            }
        }
    }

    fun updateShellCardVisibilityQuery(query: String) {
        runtimeMutableState.update { state ->
            if (state.shellCardVisibilityQuery == query) {
                state
            } else {
                state.copy(shellCardVisibilityQuery = query)
            }
        }
    }

    fun updateShowActivityShortcutEditor(show: Boolean) {
        updateEditor { state ->
            if (state.showActivityShortcutEditor == show) state else state.copy(showActivityShortcutEditor = show)
        }
    }

    fun updateActivityCardEditMode(mode: OsActivityCardEditMode) {
        updateEditor { state ->
            if (state.activityCardEditMode == mode) state else state.copy(activityCardEditMode = mode)
        }
    }

    fun updateEditingActivityShortcutCardId(cardId: String?) {
        updateEditor { state ->
            if (state.editingActivityShortcutCardId == cardId) {
                state
            } else {
                state.copy(editingActivityShortcutCardId = cardId)
            }
        }
    }

    fun updateEditingActivityShortcutBuiltIn(builtIn: Boolean) {
        updateEditor { state ->
            if (state.editingActivityShortcutBuiltIn == builtIn) {
                state
            } else {
                state.copy(editingActivityShortcutBuiltIn = builtIn)
            }
        }
    }

    fun updateShowShellCommandCardEditor(show: Boolean) {
        updateEditor { state ->
            if (state.showShellCommandCardEditor == show) state else state.copy(showShellCommandCardEditor = show)
        }
    }

    fun updateEditingShellCommandCardId(cardId: String?) {
        updateEditor { state ->
            if (state.editingShellCommandCardId == cardId) state else state.copy(editingShellCommandCardId = cardId)
        }
    }

    fun updateShowShellCardDeleteConfirm(show: Boolean) {
        runtimeMutableState.update { state ->
            if (state.showShellCardDeleteConfirm == show) state else state.copy(showShellCardDeleteConfirm = show)
        }
    }

    fun updateShowActivityCardDeleteConfirm(show: Boolean) {
        runtimeMutableState.update { state ->
            if (state.showActivityCardDeleteConfirm == show) {
                state
            } else {
                state.copy(showActivityCardDeleteConfirm = show)
            }
        }
    }

    fun updatePendingImportTarget(target: OsCardImportTarget?) {
        runtimeMutableState.update { state ->
            if (state.pendingImportTarget == target) state else state.copy(pendingImportTarget = target)
        }
    }

    fun updatePendingCardImportPreview(preview: OsCardImportPreview?) {
        runtimeMutableState.update { state ->
            if (state.pendingCardImportPreview == preview) {
                state
            } else {
                state.copy(pendingCardImportPreview = preview)
            }
        }
    }

    fun updateCardTransferInProgress(inProgress: Boolean) {
        runtimeMutableState.update { state ->
            if (state.cardTransferInProgress == inProgress) state else state.copy(cardTransferInProgress = inProgress)
        }
    }
}
