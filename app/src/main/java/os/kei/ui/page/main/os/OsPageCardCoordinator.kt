package os.kei.ui.page.main.os

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.core.privilege.PrivilegedShell
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode

internal class OsPageCardCoordinator(
    private val scope: CoroutineScope,
    private val repository: OsPageRepository,
    private val cardRepository: OsPageCardRepository,
    private val visibilityRepository: OsPageVisibilityRepository,
    private val shellCommandRepository: OsPageShellCommandRepository,
    private val persistentState: StateFlow<OsPagePersistentState>,
    private val runtimeState: StateFlow<OsPageRuntimeState>,
    private val runtimeMutableState: MutableStateFlow<OsPageRuntimeState>,
    private val events: MutableSharedFlow<OsPageEvent>,
) {
    fun applyActivityCardVisibility(
        cardId: String,
        visible: Boolean,
        defaults: OsGoogleSystemServiceConfig,
    ) {
        scope.launch {
            try {
                val updatedCards =
                    visibilityRepository.setActivityCardVisible(
                        cards = persistentState.value.activityShortcutCards,
                        cardId = cardId,
                        visible = visible,
                        defaults = defaults,
                    )
                repository.updateActivityShortcutCards(updatedCards)
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                events.emit(OsPageEvent.OperationFailed(error))
            }
        }
    }

    fun applyShellCommandCardVisibility(
        cardId: String,
        visible: Boolean,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) {
        scope.launch {
            try {
                val updatedCards =
                    visibilityRepository.setShellCommandCardVisible(
                        cardId = cardId,
                        visible = visible,
                        builtInShellCommandCards = builtInShellCommandCards,
                    )
                repository.updateShellCommandCards(updatedCards)
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                events.emit(OsPageEvent.OperationFailed(error))
            }
        }
    }

    fun runShellCommandCard(
        card: OsShellCommandCard,
        privilegedShell: PrivilegedShell,
        shellRunNoOutputText: String,
        shellRunFailedOutput: (String) -> String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) {
        val command = card.command.trim()
        if (command.isBlank()) {
            events.tryEmit(OsPageEvent.ShellCommandCardCommandRequired)
            return
        }
        if (runtimeState.value.runningShellCommandCardIds.contains(card.id)) return
        if (!privilegedShell.canUseCommand()) {
            privilegedShell.requestAccessIfNeeded()
            events.tryEmit(OsPageEvent.ShellCommandCardNoPermission)
            return
        }
        runtimeMutableState.update { state ->
            state.copy(runningShellCommandCardIds = state.runningShellCommandCardIds + card.id)
        }
        scope.launch {
            try {
                val output =
                    shellCommandRepository
                        .runCommand(
                            privilegedShell = privilegedShell,
                            command = command,
                        ).orEmpty()
                        .trim()
                        .ifBlank { shellRunNoOutputText }
                val updatedCards =
                    shellCommandRepository.saveRunResult(
                        cardId = card.id,
                        output = output,
                        builtInShellCommandCards = builtInShellCommandCards,
                    )
                repository.updateShellCommandCards(updatedCards)
                events.emit(OsPageEvent.ShellCommandCardRunCompleted)
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                val reason = error.javaClass.simpleName
                val updatedCards =
                    shellCommandRepository.saveRunResult(
                        cardId = card.id,
                        output = shellRunFailedOutput(reason).trim().ifBlank { reason },
                        builtInShellCommandCards = builtInShellCommandCards,
                    )
                repository.updateShellCommandCards(updatedCards)
                events.emit(OsPageEvent.ShellCommandCardRunFailed(error))
            } finally {
                runtimeMutableState.update { state ->
                    state.copy(runningShellCommandCardIds = state.runningShellCommandCardIds - card.id)
                }
            }
        }
    }

    fun saveShellCommandCardEdit(
        cardId: String,
        title: String,
        subtitle: String,
        command: String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) {
        if (!persistentState.value.loaded) return
        scope.launch {
            try {
                val updatedCards =
                    cardRepository.saveShellCommandCardEdit(
                        cardId = cardId,
                        title = title,
                        subtitle = subtitle,
                        command = command,
                        builtInShellCommandCards = builtInShellCommandCards,
                    )
                if (updatedCards == null) {
                    events.emit(OsPageEvent.ShellCommandCardSaveFailed)
                    return@launch
                }
                repository.updateShellCommandCards(updatedCards)
                events.emit(OsPageEvent.ShellCommandCardSaved)
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                events.emit(OsPageEvent.ShellCommandCardSaveFailed)
            }
        }
    }

    fun deleteShellCommandCard(
        cardId: String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) {
        if (!persistentState.value.loaded) return
        scope.launch {
            try {
                val updatedCards =
                    cardRepository.deleteShellCommandCard(
                        cardId = cardId,
                        builtInShellCommandCards = builtInShellCommandCards,
                    )
                repository.updateShellCommandCards(updatedCards)
                events.emit(OsPageEvent.ShellCommandCardDeleted(cardId))
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                events.emit(OsPageEvent.ExportFailed(error))
            }
        }
    }

    fun saveActivityShortcutCard(
        editMode: OsActivityCardEditMode,
        editingCardId: String?,
        draft: OsGoogleSystemServiceConfig,
        defaults: OsGoogleSystemServiceConfig,
    ) {
        val persistent = persistentState.value
        if (!persistent.loaded) return
        scope.launch {
            try {
                val updatedCards =
                    cardRepository.saveActivityShortcutCard(
                        persistent = persistent,
                        editMode = editMode,
                        editingCardId = editingCardId,
                        draft = draft,
                        defaults = defaults,
                    )
                repository.updateActivityShortcutCards(updatedCards)
                events.emit(OsPageEvent.ActivityShortcutCardSaved)
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                events.emit(OsPageEvent.ExportFailed(error))
            }
        }
    }

    fun deleteActivityShortcutCard(
        cardId: String,
        defaults: OsGoogleSystemServiceConfig,
    ) {
        if (!persistentState.value.loaded) return
        scope.launch {
            try {
                val updatedCards =
                    cardRepository.deleteActivityShortcutCard(
                        cards = persistentState.value.activityShortcutCards,
                        cardId = cardId,
                        defaults = defaults,
                    )
                repository.updateActivityShortcutCards(updatedCards)
                events.emit(OsPageEvent.ActivityShortcutCardDeleted(cardId))
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                events.emit(OsPageEvent.ExportFailed(error))
            }
        }
    }
}

private fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
