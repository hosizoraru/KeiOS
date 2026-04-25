package os.kei.ui.page.main.os

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard

@OptIn(FlowPreview::class)
internal class OsPageViewModel : ViewModel() {
    private companion object {
        const val QUERY_DEBOUNCE_MS = 180L
        const val MIN_FILTER_QUERY_LENGTH = 2
    }

    private val repository = OsPageRepository()

    val persistentState: StateFlow<OsPagePersistentState> = repository.observePersistentState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = repository.observePersistentState().value
        )

    private val _queryInput = MutableStateFlow("")
    val queryInput: StateFlow<String> = _queryInput.asStateFlow()

    private val _queryApplied = MutableStateFlow("")
    val queryApplied: StateFlow<String> = _queryApplied.asStateFlow()

    init {
        viewModelScope.launch {
            _queryInput
                .debounce(QUERY_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { q ->
                    val normalized = q.trim()
                    _queryApplied.value = when {
                        normalized.isBlank() -> ""
                        normalized.length < MIN_FILTER_QUERY_LENGTH -> ""
                        else -> normalized
                    }
                }
        }
    }

    fun updateQueryInput(value: String) {
        _queryInput.value = value
    }

    fun loadPersistentState(
        googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
        googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig
    ) {
        viewModelScope.launch {
            repository.loadPersistentState(
                googleSystemServiceDefaults = googleSystemServiceDefaults,
                googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults
            )
        }
    }

    fun reloadShellCommandCards() {
        viewModelScope.launch {
            repository.reloadShellCommandCards()
        }
    }

    fun updateVisibleCards(cards: Set<OsSectionCard>) {
        repository.updateVisibleCards(cards)
    }

    fun updateActivityShortcutCards(cards: List<OsActivityShortcutCard>) {
        repository.updateActivityShortcutCards(cards)
    }

    fun updateShellCommandCards(cards: List<OsShellCommandCard>) {
        repository.updateShellCommandCards(cards)
    }

    fun updateTopInfoExpanded(value: Boolean) {
        repository.updateTopInfoExpanded(value)
    }

    fun updateShellRunnerExpanded(value: Boolean) {
        repository.updateShellRunnerExpanded(value)
    }

    fun updateSystemTableExpanded(value: Boolean) {
        repository.updateSystemTableExpanded(value)
    }

    fun updateSecureTableExpanded(value: Boolean) {
        repository.updateSecureTableExpanded(value)
    }

    fun updateGlobalTableExpanded(value: Boolean) {
        repository.updateGlobalTableExpanded(value)
    }

    fun updateAndroidPropsExpanded(value: Boolean) {
        repository.updateAndroidPropsExpanded(value)
    }

    fun updateJavaPropsExpanded(value: Boolean) {
        repository.updateJavaPropsExpanded(value)
    }

    fun updateLinuxEnvExpanded(value: Boolean) {
        repository.updateLinuxEnvExpanded(value)
    }
}
