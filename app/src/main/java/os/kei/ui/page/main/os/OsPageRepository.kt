package os.kei.ui.page.main.os

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore

internal data class OsPagePersistentState(
    val uiSnapshot: OsUiSnapshot = OsUiSnapshot(),
    val activityShortcutCards: List<OsActivityShortcutCard> = emptyList(),
    val shellCommandCards: List<OsShellCommandCard> = emptyList(),
    val loaded: Boolean = false
)

internal class OsPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val persistentState = MutableStateFlow(OsPagePersistentState())

    fun observePersistentState(): StateFlow<OsPagePersistentState> = persistentState.asStateFlow()

    suspend fun loadPersistentState(
        googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
        googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig
    ) {
        val loaded = withContext(ioDispatcher) {
            OsPagePersistentState(
                uiSnapshot = OsUiStateStore.loadSnapshot(),
                activityShortcutCards = OsActivityShortcutCardStore.loadCards(
                    defaults = googleSystemServiceDefaults,
                    builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
                ),
                shellCommandCards = OsShellCommandCardStore.loadCards(),
                loaded = true
            )
        }
        persistentState.value = loaded
    }

    suspend fun reloadShellCommandCards() {
        val cards = withContext(ioDispatcher) {
            OsShellCommandCardStore.loadCards()
        }
        persistentState.update { state -> state.copy(shellCommandCards = cards) }
    }

    fun updateVisibleCards(cards: Set<OsSectionCard>) {
        persistentState.update { state ->
            state.copy(uiSnapshot = state.uiSnapshot.copy(visibleCards = cards))
        }
    }

    fun updateActivityShortcutCards(cards: List<OsActivityShortcutCard>) {
        persistentState.update { state -> state.copy(activityShortcutCards = cards) }
    }

    fun updateShellCommandCards(cards: List<OsShellCommandCard>) {
        persistentState.update { state -> state.copy(shellCommandCards = cards) }
    }

    fun updateTopInfoExpanded(value: Boolean) = updateUiSnapshot {
        copy(topInfoExpanded = value)
    }

    fun updateShellRunnerExpanded(value: Boolean) = updateUiSnapshot {
        copy(shellRunnerExpanded = value)
    }

    fun updateSystemTableExpanded(value: Boolean) = updateUiSnapshot {
        copy(systemTableExpanded = value)
    }

    fun updateSecureTableExpanded(value: Boolean) = updateUiSnapshot {
        copy(secureTableExpanded = value)
    }

    fun updateGlobalTableExpanded(value: Boolean) = updateUiSnapshot {
        copy(globalTableExpanded = value)
    }

    fun updateAndroidPropsExpanded(value: Boolean) = updateUiSnapshot {
        copy(androidPropsExpanded = value)
    }

    fun updateJavaPropsExpanded(value: Boolean) = updateUiSnapshot {
        copy(javaPropsExpanded = value)
    }

    fun updateLinuxEnvExpanded(value: Boolean) = updateUiSnapshot {
        copy(linuxEnvExpanded = value)
    }

    private fun updateUiSnapshot(reducer: OsUiSnapshot.() -> OsUiSnapshot) {
        persistentState.update { state ->
            val nextSnapshot = state.uiSnapshot.reducer()
            state.copy(uiSnapshot = nextSnapshot)
        }
    }
}
