package os.kei.ui.page.main.os

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.core.privilege.PrivilegedShell
import os.kei.core.state.EditorRecoveryStore
import os.kei.ui.page.main.os.state.OsEditorRecovery
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.ShortcutSuggestionField
import os.kei.ui.page.main.os.state.OsCardImportTarget
import os.kei.ui.page.main.os.transfer.OsCardImportPreview
import os.kei.core.privilege.PrivilegeStatus

@OptIn(FlowPreview::class)
internal class OsPageViewModel : ViewModel() {
    private companion object {
        const val QUERY_DEBOUNCE_MS = 180L
        const val MIN_FILTER_QUERY_LENGTH = 2
    }

    private val repository = OsPageRepository()
    private val exportRepository = OsPageExportRepository()
    private val transferRepository = OsPageCardTransferRepository()
    private val cardRepository = OsPageCardRepository()
    private val visibilityRepository = OsPageVisibilityRepository()
    private val shellCommandRepository = OsPageShellCommandRepository()
    private val refreshRepository = OsPageRefreshRepository()
    private val sectionLoadRepository = OsPageSectionLoadRepository()
    private val activityShortcutRepository = OsPageActivityShortcutRepository()
    private val activityIconLoader = OsActivityShortcutIconLoader(viewModelScope)
    private val rowsStateLoader = OsPageRowsStateLoader(viewModelScope, repository)
    private val activitySuggestionController =
        OsActivitySuggestionController(
            scope = viewModelScope,
            repository = repository,
        )
    private val cardExpansionController = OsCardExpansionController()

    val persistentState: StateFlow<OsPagePersistentState> =
        repository
            .observePersistentState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = repository.observePersistentState().value,
            )

    private val _queryInput = MutableStateFlow("")
    val queryInput: StateFlow<String> = _queryInput.asStateFlow()

    private val _queryApplied = MutableStateFlow("")
    val queryApplied: StateFlow<String> = _queryApplied.asStateFlow()

    private val _chromeState = MutableStateFlow(OsPageChromeState())
    val chromeState: StateFlow<OsPageChromeState> = _chromeState.asStateFlow()

    private val editorRecovery = OsEditorRecovery(EditorRecoveryStore.shared)
    private val _runtimeState = MutableStateFlow(editorRecovery.restore())
    val runtimeState: StateFlow<OsPageRuntimeState> = _runtimeState.asStateFlow()
    val overlayRuntimeActions = OsPageOverlayRuntimeActions(_runtimeState, editorRecovery::save)

    val activitySuggestionState: StateFlow<OsActivitySuggestionUiState> =
        activitySuggestionController.state

    val activitySuggestionChromeState: StateFlow<OsActivitySuggestionChromeState> =
        activitySuggestionController.chromeState

    val activityIconState: StateFlow<OsActivityShortcutIconUiState> =
        activityIconLoader.state

    val cardExpansionState: StateFlow<OsCardExpansionUiState> = cardExpansionController.state

    private val _events = MutableSharedFlow<OsPageEvent>(replay = 0, extraBufferCapacity = 8)
    val events: SharedFlow<OsPageEvent> = _events.asSharedFlow()

    val rowsDerivedState: StateFlow<OsPageRowsUiDerivedState> = rowsStateLoader.state
    val cardListDerivedState: StateFlow<OsPageCardListDerivedState> =
        persistentState
            .map { persistent -> deriveOsPageCardListState(persistent) }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = OsPageCardListDerivedState.Empty,
            )
    private var overlaySearchSuppressionJob: Job? = null

    private val queryState: StateFlow<OsPageQueryState> =
        combine(queryInput, queryApplied) { input, applied ->
            OsPageQueryState(
                input = input,
                applied = applied,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = OsPageQueryState(),
        )

    private val coreUiState: StateFlow<OsPageCoreUiState> =
        combine(
            persistentState,
            runtimeState,
            activitySuggestionState,
            queryState,
            chromeState,
        ) { persistent, runtime, activitySuggestion, query, chrome ->
            OsPageCoreUiState(
                persistentState = persistent,
                runtimeState = runtime,
                activitySuggestionState = activitySuggestion,
                queryState = query,
                chromeState = chrome,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = OsPageCoreUiState(),
        )

    private val pageDerivedState: StateFlow<OsPageDerivedSnapshot> =
        combine(rowsDerivedState, cardListDerivedState) { rows, cards ->
            OsPageDerivedSnapshot(
                rowsDerivedState = rows,
                cardListDerivedState = cards,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = OsPageDerivedSnapshot(),
        )

    val uiState: StateFlow<OsPageUiState> =
        combine(coreUiState, pageDerivedState, activitySuggestionChromeState) { core, derived, activitySuggestionChrome ->
            OsPageUiState(
                persistentState = core.persistentState,
                runtimeState = core.runtimeState,
                activitySuggestionState = core.activitySuggestionState,
                activitySuggestionChromeState = activitySuggestionChrome,
                chromeState = core.chromeState,
                queryInput = core.queryState.input,
                queryApplied = core.queryState.applied,
                rowsDerivedState = derived.rowsDerivedState,
                cardListDerivedState = derived.cardListDerivedState,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = OsPageUiState(),
        )
    private val transferCoordinator =
        OsPageTransferCoordinator(
            scope = viewModelScope,
            repository = repository,
            transferRepository = transferRepository,
            exportRepository = exportRepository,
            persistentState = persistentState,
            runtimeState = runtimeState,
            runtimeMutableState = _runtimeState,
            events = _events,
        )
    private val cardCoordinator =
        OsPageCardCoordinator(
            scope = viewModelScope,
            repository = repository,
            cardRepository = cardRepository,
            visibilityRepository = visibilityRepository,
            shellCommandRepository = shellCommandRepository,
            persistentState = persistentState,
            runtimeState = runtimeState,
            runtimeMutableState = _runtimeState,
            events = _events,
        )
    private val sectionController =
        OsPageSectionController(
            scope = viewModelScope,
            repository = repository,
            visibilityRepository = visibilityRepository,
            refreshRepository = refreshRepository,
            sectionLoadRepository = sectionLoadRepository,
            persistentState = persistentState,
            runtimeState = runtimeState,
            runtimeMutableState = _runtimeState,
            events = _events,
        )

    init {
        viewModelScope.launch {
            _queryInput
                .debounce(QUERY_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { q ->
                    val normalized = q.trim()
                    _queryApplied.value =
                        when {
                            normalized.isBlank() -> ""
                            normalized.length < MIN_FILTER_QUERY_LENGTH -> ""
                            else -> normalized
                        }
                }
        }
        viewModelScope.launch {
            combine(_queryApplied, persistentState, _runtimeState) { query, persistent, runtime ->
                buildRowsDerivationInput(
                    queryApplied = query,
                    uiSnapshot = persistent.uiSnapshot,
                    sectionStates = runtime.sectionStates,
                )
            }.distinctUntilChanged()
                .collectLatest { input ->
                    rowsStateLoader.request(input)
                }
        }
    }

    fun updateQueryInput(value: String) {
        _queryInput.value = value
    }

    fun updateSearchExpanded(expanded: Boolean) {
        _chromeState.update { state -> state.copy(searchExpanded = expanded) }
    }

    fun updateOverlaySheetVisible(visible: Boolean) {
        overlaySearchSuppressionJob?.cancel()
        if (visible) {
            _chromeState.update { state ->
                state.copy(
                    searchExpanded = false,
                    overlaySearchSuppressed = true,
                )
            }
            return
        }
        overlaySearchSuppressionJob =
            viewModelScope.launch {
                delay(360)
                _chromeState.update { state -> state.copy(overlaySearchSuppressed = false) }
            }
    }

    fun openActivitySuggestionSheet(target: ShortcutSuggestionField) {
        activitySuggestionController.openSheet(target)
    }

    fun dismissActivitySuggestionSheet() {
        activitySuggestionController.dismissSheet()
    }

    fun resetActivitySuggestionQueries() {
        activitySuggestionController.resetQueries()
    }

    fun updateActivityPackageSuggestionQuery(query: String) {
        activitySuggestionController.updatePackageQuery(query)
    }

    fun updateActivityClassSuggestionQuery(query: String) {
        activitySuggestionController.updateClassQuery(query)
    }

    fun loadPersistentState(
        googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard>,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) {
        viewModelScope.launch {
            repository.loadPersistentState(
                googleSystemServiceDefaults = googleSystemServiceDefaults,
                builtInActivityShortcutCards = builtInActivityShortcutCards,
                builtInShellCommandCards = builtInShellCommandCards,
            )
            overlayRuntimeActions.reconcileEditorTargets(repository.observePersistentState().value)
        }
    }

    fun reloadShellCommandCards(builtInShellCommandCards: List<OsShellCommandCard> = emptyList()) {
        viewModelScope.launch {
            repository.reloadShellCommandCards(
                builtInShellCommandCards = builtInShellCommandCards,
            )
        }
    }

    fun closePersistentShell() {
        repository.closePersistentShell()
    }

    fun cancelActiveSectionRefreshes() {
        sectionController.cancelActiveSectionRefreshes()
    }

    fun openActivityShortcutCard(
        card: OsActivityShortcutCard,
        defaults: OsGoogleSystemServiceConfig,
    ) {
        viewModelScope.launch {
            try {
                val normalized =
                    activityShortcutRepository.normalizeForOpen(
                        card = card,
                        defaults = defaults,
                    )
                if (normalized.packageName.isBlank()) {
                    _events.emit(OsPageEvent.ActivityShortcutInvalidTarget)
                    return@launch
                }
                _events.emit(OsPageEvent.LaunchActivityShortcut(normalized))
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                _events.emit(OsPageEvent.OperationFailed(error))
            }
        }
    }

    fun openActivityShortcutCardEditor(
        card: OsActivityShortcutCard,
        defaults: OsGoogleSystemServiceConfig,
    ) {
        viewModelScope.launch {
            try {
                val request =
                    activityShortcutRepository.buildEditRequest(
                        card = card,
                        defaults = defaults,
                    )
                _events.emit(OsPageEvent.ShowActivityShortcutEditor(request))
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                _events.emit(OsPageEvent.OperationFailed(error))
            }
        }
    }

    fun openAddActivityShortcutCardEditor(defaults: OsGoogleSystemServiceConfig) {
        viewModelScope.launch {
            try {
                val request = activityShortcutRepository.buildAddRequest(defaults)
                _events.emit(OsPageEvent.ShowActivityShortcutEditor(request))
            } catch (error: Throwable) {
                error.rethrowIfCancellation()
                _events.emit(OsPageEvent.OperationFailed(error))
            }
        }
    }

    fun openShellCommandCardEditor(card: OsShellCommandCard) {
        _events.tryEmit(OsPageEvent.ShowShellCommandCardEditor(card))
    }

    fun requestActivitySuggestions(
        context: Context,
        show: Boolean,
        target: ShortcutSuggestionField,
        packageName: String,
    ) = activitySuggestionController.request(
        context = context,
        show = show,
        target = target,
        packageName = packageName,
    )

    fun requestRowsDerivedState(input: OsPageRowsDerivationInput) = rowsStateLoader.request(input)

    fun requestActivityShortcutIcons(
        context: Context,
        requests: List<OsActivityShortcutIconRequest>,
    ) = activityIconLoader.requestActivityShortcutIcons(
        context = context,
        requests = requests,
    )

    fun requestPackageIcons(
        context: Context,
        packageNames: List<String>,
    ) = activityIconLoader.requestPackageIcons(
        context = context,
        packageNames = packageNames,
    )

    fun syncActivityCardExpansion(
        cards: List<OsActivityShortcutCard>,
        initialGoogleSystemServiceExpanded: Boolean,
    ) = cardExpansionController.syncActivityCards(
        cards = cards,
        initialGoogleSystemServiceExpanded = initialGoogleSystemServiceExpanded,
    )

    fun syncShellCommandCardExpansion(cards: List<OsShellCommandCard>) = cardExpansionController.syncShellCommandCards(cards)

    fun updateActivityCardExpanded(
        cardId: String,
        expanded: Boolean,
    ) = cardExpansionController.updateActivityCard(
        cardId = cardId,
        expanded = expanded,
    )

    fun updateShellCommandCardExpanded(
        cardId: String,
        expanded: Boolean,
    ) = cardExpansionController.updateShellCommandCard(
        cardId = cardId,
        expanded = expanded,
    )

    fun removeActivityCardExpansion(cardId: String) {
        cardExpansionController.removeActivityCard(cardId)
    }

    fun removeShellCommandCardExpansion(cardId: String) {
        cardExpansionController.removeShellCommandCard(cardId)
    }

    fun retainActivityCardExpansion(validIds: Set<String>) {
        cardExpansionController.retainActivityCards(validIds)
    }

    fun retainShellCommandCardExpansion(validIds: Set<String>) {
        cardExpansionController.retainShellCommandCards(validIds)
    }

    fun updateVisibleCards(cards: Set<OsSectionCard>) {
        repository.updateVisibleCards(cards)
    }

    suspend fun saveVisibleCards(cards: Set<OsSectionCard>) {
        repository.saveVisibleCards(cards)
    }

    suspend fun hydrateInitialCache(
        isPageActive: Boolean,
        ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    ) = sectionController.hydrateInitialCache(
        isPageActive = isPageActive,
        ensureLoad = ensureLoad,
    )

    suspend fun saveExpandedStateSnapshot(snapshot: OsUiSnapshot) {
        repository.saveExpandedStateSnapshot(snapshot)
    }

    fun replaceSectionStates(sectionStates: Map<SectionKind, SectionState>) {
        sectionController.replaceSectionStates(sectionStates)
    }

    fun updateSection(
        section: SectionKind,
        transform: (SectionState) -> SectionState,
    ) = sectionController.updateSection(section, transform)

    suspend fun ensureSectionLoaded(
        section: SectionKind,
        forceRefresh: Boolean,
        context: Context,
        privilegeStatus: PrivilegeStatus,
        privilegedShell: PrivilegedShell,
    ) = sectionController.ensureSectionLoaded(
        section = section,
        forceRefresh = forceRefresh,
        context = context,
        privilegeStatus = privilegeStatus,
        privilegedShell = privilegedShell,
    )

    fun invalidatePrivilegeSections() {
        sectionController.invalidatePrivilegeSections()
    }

    fun applySectionCardVisibility(
        card: OsSectionCard,
        visible: Boolean,
        ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    ) = sectionController.applySectionCardVisibility(
        card = card,
        visible = visible,
        ensureLoad = ensureLoad,
    )

    fun applyActivityCardVisibility(
        cardId: String,
        visible: Boolean,
        defaults: OsGoogleSystemServiceConfig,
    ) = cardCoordinator.applyActivityCardVisibility(
        cardId = cardId,
        visible = visible,
        defaults = defaults,
    )

    fun applyShellCommandCardVisibility(
        cardId: String,
        visible: Boolean,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) = cardCoordinator.applyShellCommandCardVisibility(
        cardId = cardId,
        visible = visible,
        builtInShellCommandCards = builtInShellCommandCards,
    )

    fun runShellCommandCard(
        card: OsShellCommandCard,
        privilegedShell: PrivilegedShell,
        shellRunNoOutputText: String,
        shellRunFailedOutput: (String) -> String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) = cardCoordinator.runShellCommandCard(
        card = card,
        privilegedShell = privilegedShell,
        shellRunNoOutputText = shellRunNoOutputText,
        shellRunFailedOutput = shellRunFailedOutput,
        builtInShellCommandCards = builtInShellCommandCards,
    )

    fun refreshAllSections(ensureLoad: suspend (SectionKind, Boolean) -> Unit) {
        sectionController.refreshAllSections(ensureLoad)
    }

    fun prepareActivityCardsExport(defaults: OsGoogleSystemServiceConfig) = transferCoordinator.prepareActivityCardsExport(defaults)

    fun prepareShellCardsExport() = transferCoordinator.prepareShellCardsExport()

    fun prepareSectionCardExport(
        card: OsSectionCard,
        context: Context,
        googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
        privilegeStatus: PrivilegeStatus,
        ensureLoad: suspend (SectionKind, Boolean) -> Unit,
    ) = transferCoordinator.prepareSectionCardExport(
        card = card,
        context = context,
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        privilegeStatus = privilegeStatus,
        ensureLoad = ensureLoad,
    )

    fun writeCardExportContent(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String,
    ) = transferCoordinator.writeCardExportContent(
        contentResolver = contentResolver,
        uri = uri,
        content = content,
    )

    fun requestCardImportPreview(
        contentResolver: ContentResolver,
        uri: Uri,
        target: OsCardImportTarget,
        googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
        googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard>,
    ) = transferCoordinator.requestCardImportPreview(
        contentResolver = contentResolver,
        uri = uri,
        target = target,
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults,
        builtInActivityShortcutCards = builtInActivityShortcutCards,
    )

    fun confirmCardImport(
        preview: OsCardImportPreview,
        googleSystemServiceDefaults: OsGoogleSystemServiceConfig,
        googleSettingsBuiltInSampleDefaults: OsGoogleSystemServiceConfig,
        builtInActivityShortcutCards: List<OsActivityShortcutCard>,
    ) = transferCoordinator.confirmCardImport(
        preview = preview,
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults,
        builtInActivityShortcutCards = builtInActivityShortcutCards,
    )

    fun saveShellCommandCardEdit(
        cardId: String,
        title: String,
        subtitle: String,
        command: String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) = cardCoordinator.saveShellCommandCardEdit(
        cardId = cardId,
        title = title,
        subtitle = subtitle,
        command = command,
        builtInShellCommandCards = builtInShellCommandCards,
    )

    fun deleteShellCommandCard(
        cardId: String,
        builtInShellCommandCards: List<OsShellCommandCard>,
    ) = cardCoordinator.deleteShellCommandCard(
        cardId = cardId,
        builtInShellCommandCards = builtInShellCommandCards,
    )

    fun saveActivityShortcutCard(
        editMode: OsActivityCardEditMode,
        editingCardId: String?,
        draft: OsGoogleSystemServiceConfig,
        defaults: OsGoogleSystemServiceConfig,
    ) = cardCoordinator.saveActivityShortcutCard(
        editMode = editMode,
        editingCardId = editingCardId,
        draft = draft,
        defaults = defaults,
    )

    fun deleteActivityShortcutCard(
        cardId: String,
        defaults: OsGoogleSystemServiceConfig,
    ) = cardCoordinator.deleteActivityShortcutCard(
        cardId = cardId,
        defaults = defaults,
    )

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

    override fun onCleared() {
        activitySuggestionController.cancel()
        rowsStateLoader.cancel()
        activityIconLoader.clearLoadingState()
        super.onCleared()
    }
}

private fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
