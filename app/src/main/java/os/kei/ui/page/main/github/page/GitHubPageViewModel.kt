package os.kei.ui.page.main.github.page

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.BuildConfig
import os.kei.core.ui.snapshot.AppSnapshotFlowManager
import os.kei.feature.github.data.local.GitHubAppPickerPreferences
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.GitHubTrackedFilterMode
import os.kei.ui.page.main.github.actions.GitHubActionsSheetUiState
import os.kei.ui.page.main.github.page.action.GitHubActionClock
import os.kei.ui.page.main.github.page.action.GitHubSystemActionClock
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerDerivedState
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerInput
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.section.GitHubTrackedItemsExpansionState
import os.kei.ui.page.main.github.sheet.GitHubApkInfoSheetInput
import os.kei.ui.page.main.github.sheet.GitHubApkInfoSheetUiState
import os.kei.ui.page.main.github.sheet.GitHubManagedInstallConfirmSheetInput
import os.kei.ui.page.main.github.sheet.GitHubManagedInstallConfirmSheetUiState
import os.kei.ui.page.main.github.sheet.GitHubReleaseNotesDetailInput
import os.kei.ui.page.main.github.sheet.GitHubReleaseNotesDetailUiState
import kotlin.time.Duration.Companion.milliseconds

private const val PENDING_SHARE_IMPORT_CARD_TICK_MS = 15_000L
private const val CONTENT_DERIVATION_DEBOUNCE_MS = 48L

internal data class GitHubTrackedExportRequest(
    val content: String,
    val fileName: String,
)

internal data class GitHubTrackTransferUiState(
    val tracksExporting: Boolean = false,
    val tracksImporting: Boolean = false,
    val pendingExport: GitHubTrackedExportRequest? = null,
)

@Immutable
internal data class GitHubPageChromeState(
    val searchExpanded: Boolean = false,
)

@Immutable
private data class GitHubPageUiCoreSnapshot(
    val runtimeState: GitHubPageUiRuntimeSnapshot = GitHubPageUiRuntimeSnapshot(),
    val installedOnlineShareTargets: List<OnlineShareTargetOption> = emptyList(),
    val checkLogicDownloaderOptions: List<DownloaderOption> = emptyList(),
    val contentDerivedState: GitHubPageContentDerivedState = GitHubPageContentDerivedState(),
    val appPickerPreferences: GitHubAppPickerPreferences = GitHubAppPickerPreferences(),
)

@Immutable
private data class GitHubPageUiRuntimeSnapshot(
    val transferState: GitHubTrackTransferUiState = GitHubTrackTransferUiState(),
    val chromeState: GitHubPageChromeState = GitHubPageChromeState(),
    val trackedItemsExpansionState: GitHubTrackedItemsExpansionState = GitHubTrackedItemsExpansionState(),
)

@Immutable
internal data class GitHubPageUiSnapshot(
    val transferState: GitHubTrackTransferUiState = GitHubTrackTransferUiState(),
    val chromeState: GitHubPageChromeState = GitHubPageChromeState(),
    val trackedItemsExpansionState: GitHubTrackedItemsExpansionState = GitHubTrackedItemsExpansionState(),
    val installedOnlineShareTargets: List<OnlineShareTargetOption> = emptyList(),
    val checkLogicDownloaderOptions: List<DownloaderOption> = emptyList(),
    val contentDerivedState: GitHubPageContentDerivedState = GitHubPageContentDerivedState(),
    val appPickerDerivedState: GitHubTrackAppPickerDerivedState = GitHubTrackAppPickerDerivedState.Empty,
    val appPickerPreferences: GitHubAppPickerPreferences = GitHubAppPickerPreferences(),
    val historyUnreadCount: Int = 0,
)

internal sealed interface GitHubTrackedExportStartResult {
    data object Ready : GitHubTrackedExportStartResult

    data object Busy : GitHubTrackedExportStartResult

    data object Empty : GitHubTrackedExportStartResult

    data class Failed(
        val reason: String?,
    ) : GitHubTrackedExportStartResult
}

internal sealed interface GitHubTrackedImportStartResult {
    data object Ready : GitHubTrackedImportStartResult

    data object Busy : GitHubTrackedImportStartResult
}

internal class GitHubPageViewModel(
    application: Application,
    private val clock: GitHubActionClock = GitHubSystemActionClock,
) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext
    val repository = GitHubPageRepository()
    val durableActionScope: CoroutineScope
        get() = viewModelScope
    private val appIconLoader = GitHubAppIconLoader(viewModelScope)
    private var pageState: GitHubPageState? = null
    private var contentStateJob: Job? = null
    private var pendingShareImportClockJob: Job? = null
    private var onlineShareTargetsJob: Job? = null
    private var downloaderOptionsJob: Job? = null
    private var appPickerStateJob: Job? = null
    private var appPickerStateInput: GitHubTrackAppPickerInput? = null
    private val snapshotFlowManager = AppSnapshotFlowManager()
    private val sheetDerivationController =
        GitHubPageSheetDerivationController(
            scope = viewModelScope,
            repository = repository,
            snapshotFlowManager = snapshotFlowManager,
            clock = clock,
        )
    private val trackedExpansionController =
        GitHubTrackedExpansionStateController(
            scope = viewModelScope,
            repository = repository,
        )
    private val pendingShareImportPageActive = MutableStateFlow(false)
    private val pendingShareImportNowMillis = MutableStateFlow(clock.nowMs())

    private val _contentDerivedState = MutableStateFlow(GitHubPageContentDerivedState())
    val contentDerivedState: StateFlow<GitHubPageContentDerivedState> = _contentDerivedState.asStateFlow()

    private val _transferState = MutableStateFlow(GitHubTrackTransferUiState())
    val transferState: StateFlow<GitHubTrackTransferUiState> = _transferState.asStateFlow()

    private val _chromeState = MutableStateFlow(GitHubPageChromeState())
    val chromeState: StateFlow<GitHubPageChromeState> = _chromeState.asStateFlow()

    val trackedItemsExpansionState = trackedExpansionController.state

    private val _installedOnlineShareTargets = MutableStateFlow<List<OnlineShareTargetOption>>(emptyList())
    val installedOnlineShareTargets: StateFlow<List<OnlineShareTargetOption>> =
        _installedOnlineShareTargets.asStateFlow()

    private val _checkLogicDownloaderOptions = MutableStateFlow<List<DownloaderOption>>(emptyList())
    val checkLogicDownloaderOptions: StateFlow<List<DownloaderOption>> =
        _checkLogicDownloaderOptions.asStateFlow()

    private val _appPickerPreferences = MutableStateFlow(GitHubAppPickerPreferences())
    val appPickerPreferences: StateFlow<GitHubAppPickerPreferences> =
        _appPickerPreferences.asStateFlow()

    private val historyUnreadBadge =
        GitHubHistoryUnreadBadge(
            scope = viewModelScope,
            signalVersions = repository.historyUnreadSignalVersions(),
            loadCount = repository::loadHistoryUnreadCount,
        )
    val historyUnreadCount: StateFlow<Int> = historyUnreadBadge.count

    private val _appPickerDerivedState =
        MutableStateFlow(GitHubTrackAppPickerDerivedState.Empty)
    val appPickerDerivedState: StateFlow<GitHubTrackAppPickerDerivedState> =
        _appPickerDerivedState.asStateFlow()

    val apkInfoSheetState: StateFlow<GitHubApkInfoSheetUiState> =
        sheetDerivationController.apkInfoSheetState

    val releaseNotesDetailState: StateFlow<GitHubReleaseNotesDetailUiState> =
        sheetDerivationController.releaseNotesDetailState

    val managedInstallConfirmSheetState: StateFlow<GitHubManagedInstallConfirmSheetUiState> =
        sheetDerivationController.managedInstallConfirmSheetState

    val actionsSheetState: StateFlow<GitHubActionsSheetUiState> =
        sheetDerivationController.actionsSheetState

    val appIconState: StateFlow<GitHubAppIconUiState> =
        appIconLoader.state

    private val runtimeUiState: StateFlow<GitHubPageUiRuntimeSnapshot> =
        combine(
            transferState,
            chromeState,
            trackedItemsExpansionState,
        ) { transfer, chrome, expansion ->
            GitHubPageUiRuntimeSnapshot(
                transferState = transfer,
                chromeState = chrome,
                trackedItemsExpansionState = expansion,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = GitHubPageUiRuntimeSnapshot(),
        )

    private val coreUiState: StateFlow<GitHubPageUiCoreSnapshot> =
        combine(
            runtimeUiState,
            installedOnlineShareTargets,
            checkLogicDownloaderOptions,
            contentDerivedState,
            appPickerPreferences,
        ) { runtime, shareTargets, downloaderOptions, content, appPickerPreferences ->
            GitHubPageUiCoreSnapshot(
                runtimeState = runtime,
                installedOnlineShareTargets = shareTargets,
                checkLogicDownloaderOptions = downloaderOptions,
                contentDerivedState = content,
                appPickerPreferences = appPickerPreferences,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = GitHubPageUiCoreSnapshot(),
        )

    val uiState: StateFlow<GitHubPageUiSnapshot> =
        combine(coreUiState, appPickerDerivedState, historyUnreadCount) { core, appPicker, historyUnreadCount ->
            GitHubPageUiSnapshot(
                transferState = core.runtimeState.transferState,
                chromeState = core.runtimeState.chromeState,
                trackedItemsExpansionState = core.runtimeState.trackedItemsExpansionState,
                installedOnlineShareTargets = core.installedOnlineShareTargets,
                checkLogicDownloaderOptions = core.checkLogicDownloaderOptions,
                contentDerivedState = core.contentDerivedState,
                appPickerDerivedState = appPicker,
                appPickerPreferences = core.appPickerPreferences,
                historyUnreadCount = historyUnreadCount,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = GitHubPageUiSnapshot(),
        )

    init {
        viewModelScope.launch {
            _appPickerPreferences.value = repository.loadAppPickerPreferences()
        }
    }

    fun pageState(): GitHubPageState {
        val current = pageState
        if (current != null) return current
        return GitHubPageState(
            editorRecovery = GitHubEditorRecovery(os.kei.core.state.EditorRecoveryStore.shared),
        ).also {
            pageState = it
            bindContentState(it)
            sheetDerivationController.bindActionsSheetState(it)
            viewModelScope.launch {
                val persistedState = repository.loadPersistedUiState()
                if (pageState === it) {
                    it.applyPersistedUiState(persistedState)
                    trackedExpansionController.applyPersistedTrackedReleaseExpansionState(
                        persistedState.trackedReleaseExpansionState,
                    )
                }
            }
        }
    }

    fun updateSearchExpanded(expanded: Boolean) {
        _chromeState.update { state ->
            val target = state.copy(searchExpanded = expanded)
            if (target == state) state else target
        }
    }

    fun setTrackedCardExpanded(
        itemId: String,
        expanded: Boolean,
    ) = trackedExpansionController.setTrackedCardExpanded(itemId, expanded)

    fun removeTrackedCardExpansion(itemId: String) = trackedExpansionController.removeTrackedCardExpansion(itemId)

    fun setTrackedStableVersionExpanded(
        itemId: String,
        expanded: Boolean,
    ) = trackedExpansionController.setTrackedStableVersionExpanded(itemId, expanded)

    fun setTrackedLocalVersionExpanded(
        itemId: String,
        expanded: Boolean,
    ) = trackedExpansionController.setTrackedLocalVersionExpanded(itemId, expanded)

    fun setTrackedPreReleaseVersionExpanded(
        itemId: String,
        expanded: Boolean,
    ) = trackedExpansionController.setTrackedPreReleaseVersionExpanded(itemId, expanded)

    fun removeTrackedExpansion(
        itemId: String,
        removePersistedReleaseExpansion: Boolean,
    ) = trackedExpansionController.removeTrackedExpansion(itemId, removePersistedReleaseExpansion)

    fun retainTrackedExpansion(validItemIds: Set<String>) = trackedExpansionController.retainTrackedExpansion(validItemIds)

    fun bindContextObservers(state: GitHubPageState) {
        if (onlineShareTargetsJob?.isActive != true) {
            onlineShareTargetsJob =
                viewModelScope.launch {
                    snapshotFlowManager
                        .snapshotFlow {
                            GitHubOnlineShareTargetInput(
                                shouldResolve =
                                    state.showCheckLogicSheet ||
                                        state.lookupConfig.onlineShareTargetPackage.isNotBlank() ||
                                        state.onlineShareTargetPackageInput.isNotBlank(),
                                appList = state.appList.toList(),
                            )
                        }.distinctUntilChanged()
                        .collectLatest { input ->
                            _installedOnlineShareTargets.value =
                                repository.queryOnlineShareTargets(
                                    context = appContext,
                                    input = input,
                                )
                        }
                }
        }
        if (downloaderOptionsJob?.isActive != true) {
            downloaderOptionsJob =
                viewModelScope.launch {
                    snapshotFlowManager
                        .snapshotFlow { state.showCheckLogicSheet }
                        .distinctUntilChanged()
                        .collectLatest { showCheckLogicSheet ->
                            _checkLogicDownloaderOptions.value =
                                if (showCheckLogicSheet) {
                                    repository.queryDownloaders(appContext)
                                } else {
                                    emptyList()
                                }
                        }
                }
        }
    }

    fun unbindContextObservers() {
        onlineShareTargetsJob?.cancel()
        onlineShareTargetsJob = null
        downloaderOptionsJob?.cancel()
        downloaderOptionsJob = null
    }

    fun setPageDataActive(active: Boolean) {
        if (pendingShareImportPageActive.value == active) return
        pendingShareImportPageActive.value = active
    }

    fun requestAppIcons(packageNames: List<String>) =
        appIconLoader.requestIcons(
            context = appContext,
            packageNames = packageNames,
        )

    fun invalidateAppIcon(packageName: String) =
        appIconLoader.invalidatePackage(packageName)

    fun requestAppPickerState(input: GitHubTrackAppPickerInput) {
        val previousInput = appPickerStateInput
        if (previousInput == input && _appPickerDerivedState.value !== GitHubTrackAppPickerDerivedState.Empty) {
            return
        }
        appPickerStateInput = input
        appPickerStateJob?.cancel()
        _appPickerDerivedState.update { state ->
            state.copy(
                deriving = true,
                input = input,
            )
        }
        appPickerStateJob =
            viewModelScope.launch {
                val derivedState = repository.buildAppPickerState(input)
                if (appPickerStateInput != input) return@launch
                _appPickerDerivedState.value = derivedState
            }
    }

    fun requestApkInfoSheetState(input: GitHubApkInfoSheetInput) {
        sheetDerivationController.requestApkInfoSheetState(input)
    }

    fun updateApkInfoSheetQuery(query: String) {
        sheetDerivationController.updateApkInfoSheetQuery(query)
    }

    fun clearApkInfoSheetState() {
        sheetDerivationController.clearApkInfoSheetState()
    }

    fun requestReleaseNotesDetailState(input: GitHubReleaseNotesDetailInput) {
        sheetDerivationController.requestReleaseNotesDetailState(input)
    }

    fun clearReleaseNotesDetailState() {
        sheetDerivationController.clearReleaseNotesDetailState()
    }

    fun requestManagedInstallConfirmSheetState(input: GitHubManagedInstallConfirmSheetInput) {
        sheetDerivationController.requestManagedInstallConfirmSheetState(input)
    }

    fun clearManagedInstallConfirmSheetState() {
        sheetDerivationController.clearManagedInstallConfirmSheetState()
    }

    fun saveAppPickerPreferences(preferences: GitHubAppPickerPreferences) {
        if (_appPickerPreferences.value == preferences) return
        _appPickerPreferences.value = preferences
        viewModelScope.launch {
            repository.saveAppPickerPreferences(preferences)
        }
    }

    fun refreshHistoryUnreadCount() = historyUnreadBadge.refresh()

    suspend fun beginTrackedExport(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long,
        fileName: String,
    ): GitHubTrackedExportStartResult {
        if (_transferState.value.tracksExporting || _transferState.value.tracksImporting) {
            return GitHubTrackedExportStartResult.Busy
        }
        if (items.isEmpty()) return GitHubTrackedExportStartResult.Empty
        _transferState.update { state -> state.copy(tracksExporting = true) }
        val content =
            runCatching {
                repository.buildTrackedItemsExportJson(
                    items = items,
                    exportedAtMillis = exportedAtMillis,
                )
            }.getOrElse { error ->
                finishTrackedExport()
                return GitHubTrackedExportStartResult.Failed(error.message ?: error.javaClass.simpleName)
            }
        _transferState.update { state ->
            state.copy(
                tracksExporting = true,
                pendingExport =
                    GitHubTrackedExportRequest(
                        content = content,
                        fileName = fileName,
                    ),
            )
        }
        return GitHubTrackedExportStartResult.Ready
    }

    fun beginTrackedImport(): GitHubTrackedImportStartResult {
        if (_transferState.value.tracksExporting || _transferState.value.tracksImporting) {
            return GitHubTrackedImportStartResult.Busy
        }
        _transferState.update { state -> state.copy(tracksImporting = true) }
        return GitHubTrackedImportStartResult.Ready
    }

    fun consumePendingExport(): GitHubTrackedExportRequest? {
        val request = _transferState.value.pendingExport
        _transferState.update { state -> state.copy(pendingExport = null) }
        return request
    }

    fun finishTrackedExport() {
        _transferState.update { state ->
            state.copy(
                tracksExporting = false,
                pendingExport = null,
            )
        }
    }

    fun finishTrackedImport() {
        _transferState.update { state -> state.copy(tracksImporting = false) }
    }

    suspend fun writeExport(
        contentResolver: ContentResolver,
        uri: Uri,
        request: GitHubTrackedExportRequest,
    ) {
        repository.writeText(
            contentResolver = contentResolver,
            uri = uri,
            content = request.content,
        )
    }

    suspend fun readImport(
        contentResolver: ContentResolver,
        uri: Uri,
    ): String =
        repository.readText(
            contentResolver = contentResolver,
            uri = uri,
        )

    @OptIn(FlowPreview::class)
    private fun bindContentState(state: GitHubPageState) {
        contentStateJob?.cancel()
        pendingShareImportClockJob?.cancel()
        pendingShareImportClockJob =
            viewModelScope.launch {
                combine(
                    snapshotFlowManager
                        .snapshotFlow { state.pendingShareImportTrack?.armedAtMillis }
                        .distinctUntilChanged(),
                    pendingShareImportPageActive,
                ) { armedAtMillis, pageActive ->
                    armedAtMillis.takeIf { pageActive }
                }.distinctUntilChanged()
                    .collectLatest { armedAtMillis ->
                        pendingShareImportNowMillis.value = clock.nowMs()
                        if (armedAtMillis == null) return@collectLatest
                        while (true) {
                            kotlinx.coroutines.delay(PENDING_SHARE_IMPORT_CARD_TICK_MS.milliseconds)
                            pendingShareImportNowMillis.value = clock.nowMs()
                        }
                    }
            }
        contentStateJob =
            viewModelScope.launch {
                combine(
                    snapshotFlowManager
                        .snapshotFlow {
                            GitHubPageContentInput(
                                trackedItems = state.trackedItems.toList(),
                                trackedSearch = state.trackedSearch,
                                trackedFilterMode = state.trackedFilterMode,
                                sortMode = state.sortMode,
                                sortDirection = state.sortDirection,
                                checkStates = state.checkStates.toMap(),
                                appList = state.appList.toList(),
                                trackedFirstInstallAtByPackage = state.trackedFirstInstallAtByPackage.toMap(),
                                trackedAddedAtById = state.trackedAddedAtById.toMap(),
                                trackedModifiedAtById = state.trackedModifiedAtById.toMap(),
                                pendingShareImportTrack = state.pendingShareImportTrack,
                                selfPackageName = BuildConfig.APPLICATION_ID,
                                pinnedTrackIds = state.pinnedTrackIds,
                                nowMillis = 0L,
                            )
                        }.conflate()
                        .debounce(CONTENT_DERIVATION_DEBOUNCE_MS.milliseconds)
                        .distinctUntilChanged(),
                    pendingShareImportNowMillis,
                ) { input, nowMillis ->
                    input.copy(nowMillis = nowMillis)
                }.collectLatest { input ->
                    val derived = repository.buildContentState(input)
                    if (shouldResetFailedTrackedFilter(input, derived)) {
                        state.trackedFilterMode = GitHubTrackedFilterMode.All
                        repository.saveTrackedFilterMode(GitHubTrackedFilterMode.All)
                        _contentDerivedState.value =
                            repository.buildContentState(
                                input.copy(trackedFilterMode = GitHubTrackedFilterMode.All),
                            )
                    } else {
                        _contentDerivedState.value = derived
                    }
                }
            }
    }

    override fun onCleared() {
        contentStateJob?.cancel()
        pendingShareImportClockJob?.cancel()
        onlineShareTargetsJob?.cancel()
        downloaderOptionsJob?.cancel()
        appPickerStateJob?.cancel()
        sheetDerivationController.cancel()
        appIconLoader.clearLoadingState()
        snapshotFlowManager.dispose()
        super.onCleared()
    }
}

internal fun shouldResetFailedTrackedFilter(
    input: GitHubPageContentInput,
    derived: GitHubPageContentDerivedState,
): Boolean =
    input.trackedFilterMode == GitHubTrackedFilterMode.FailedChecks &&
        input.trackedItems.isNotEmpty() &&
        derived.trackedUi.overviewMetrics.failedCount == 0
