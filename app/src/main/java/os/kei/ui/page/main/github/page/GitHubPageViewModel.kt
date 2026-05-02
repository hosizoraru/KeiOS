package os.kei.ui.page.main.github.page

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.core.ui.snapshot.AppSnapshotFlowManager
import os.kei.ui.page.main.github.actions.GitHubActionsUiStateStore
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption

private const val pendingShareImportCardTickMs = 15_000L

internal data class GitHubTrackedExportRequest(
    val content: String,
    val fileName: String
)

internal data class GitHubTrackTransferUiState(
    val tracksExporting: Boolean = false,
    val tracksImporting: Boolean = false,
    val pendingExport: GitHubTrackedExportRequest? = null
)

internal sealed interface GitHubTrackedExportStartResult {
    data object Ready : GitHubTrackedExportStartResult
    data object Busy : GitHubTrackedExportStartResult
    data object Empty : GitHubTrackedExportStartResult
    data class Failed(val reason: String?) : GitHubTrackedExportStartResult
}

internal sealed interface GitHubTrackedImportStartResult {
    data object Ready : GitHubTrackedImportStartResult
    data object Busy : GitHubTrackedImportStartResult
}

internal class GitHubPageViewModel : ViewModel() {
    val repository = GitHubPageRepository()
    private var pageState: GitHubPageState? = null
    private var contentStateJob: Job? = null
    private var pendingShareImportClockJob: Job? = null
    private var onlineShareTargetsJob: Job? = null
    private var downloaderOptionsJob: Job? = null
    private val snapshotFlowManager = AppSnapshotFlowManager()
    private val pendingShareImportNowMillis = MutableStateFlow(System.currentTimeMillis())

    private val _contentDerivedState = MutableStateFlow(GitHubPageContentDerivedState())
    val contentDerivedState: StateFlow<GitHubPageContentDerivedState> = _contentDerivedState.asStateFlow()

    private val _transferState = MutableStateFlow(GitHubTrackTransferUiState())
    val transferState: StateFlow<GitHubTrackTransferUiState> = _transferState.asStateFlow()

    private val _installedOnlineShareTargets = MutableStateFlow<List<OnlineShareTargetOption>>(emptyList())
    val installedOnlineShareTargets: StateFlow<List<OnlineShareTargetOption>> =
        _installedOnlineShareTargets.asStateFlow()

    private val _checkLogicDownloaderOptions = MutableStateFlow<List<DownloaderOption>>(emptyList())
    val checkLogicDownloaderOptions: StateFlow<List<DownloaderOption>> =
        _checkLogicDownloaderOptions.asStateFlow()

    fun pageState(searchBarHideThresholdPx: Float): GitHubPageState {
        val current = pageState
        if (current != null) return current
        return GitHubPageState(
            searchBarHideThresholdPx = searchBarHideThresholdPx,
            actionsSectionExpansionState = GitHubActionsUiStateStore.loadSectionExpansionState()
        ).also {
            pageState = it
            bindContentState(it)
        }
    }

    fun bindContextObservers(
        context: Context,
        state: GitHubPageState
    ) {
        val appContext = context.applicationContext
        if (onlineShareTargetsJob?.isActive != true) {
            onlineShareTargetsJob = viewModelScope.launch {
                snapshotFlowManager.snapshotFlow {
                    GitHubOnlineShareTargetInput(
                        shouldResolve = state.showCheckLogicSheet ||
                            state.lookupConfig.onlineShareTargetPackage.isNotBlank() ||
                            state.onlineShareTargetPackageInput.isNotBlank(),
                        appList = state.appList.toList()
                    )
                }.collectLatest { input ->
                    _installedOnlineShareTargets.value = repository.queryOnlineShareTargets(
                        context = appContext,
                        input = input
                    )
                }
            }
        }
        if (downloaderOptionsJob?.isActive != true) {
            downloaderOptionsJob = viewModelScope.launch {
                snapshotFlowManager.snapshotFlow { state.showCheckLogicSheet }
                    .collectLatest { showCheckLogicSheet ->
                        _checkLogicDownloaderOptions.value = if (showCheckLogicSheet) {
                            repository.queryDownloaders(appContext)
                        } else {
                            emptyList()
                        }
                    }
            }
        }
    }

    suspend fun beginTrackedExport(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long,
        fileName: String
    ): GitHubTrackedExportStartResult {
        if (_transferState.value.tracksExporting || _transferState.value.tracksImporting) {
            return GitHubTrackedExportStartResult.Busy
        }
        if (items.isEmpty()) return GitHubTrackedExportStartResult.Empty
        _transferState.update { state -> state.copy(tracksExporting = true) }
        val content = runCatching {
            repository.buildTrackedItemsExportJson(
                items = items,
                exportedAtMillis = exportedAtMillis
            )
        }.getOrElse { error ->
            finishTrackedExport()
            return GitHubTrackedExportStartResult.Failed(error.message ?: error.javaClass.simpleName)
        }
        _transferState.update { state ->
            state.copy(
                tracksExporting = true,
                pendingExport = GitHubTrackedExportRequest(
                    content = content,
                    fileName = fileName
                )
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
                pendingExport = null
            )
        }
    }

    fun finishTrackedImport() {
        _transferState.update { state -> state.copy(tracksImporting = false) }
    }

    suspend fun writeExport(
        contentResolver: ContentResolver,
        uri: Uri,
        request: GitHubTrackedExportRequest
    ) {
        repository.writeText(
            contentResolver = contentResolver,
            uri = uri,
            content = request.content
        )
    }

    suspend fun readImport(
        contentResolver: ContentResolver,
        uri: Uri
    ): String {
        return repository.readText(
            contentResolver = contentResolver,
            uri = uri
        )
    }

    private fun bindContentState(state: GitHubPageState) {
        contentStateJob?.cancel()
        pendingShareImportClockJob?.cancel()
        pendingShareImportClockJob = viewModelScope.launch {
            snapshotFlowManager.snapshotFlow { state.pendingShareImportTrack?.armedAtMillis }
                .collectLatest { armedAtMillis ->
                    pendingShareImportNowMillis.value = System.currentTimeMillis()
                    if (armedAtMillis == null) return@collectLatest
                    while (true) {
                        kotlinx.coroutines.delay(pendingShareImportCardTickMs)
                        pendingShareImportNowMillis.value = System.currentTimeMillis()
                    }
                }
        }
        contentStateJob = viewModelScope.launch {
            combine(
                snapshotFlowManager.snapshotFlow {
                    GitHubPageContentInput(
                        trackedItems = state.trackedItems.toList(),
                        trackedSearch = state.trackedSearch,
                        sortMode = state.sortMode,
                        checkStates = state.checkStates.toMap(),
                        appList = state.appList.toList(),
                        trackedFirstInstallAtByPackage = state.trackedFirstInstallAtByPackage.toMap(),
                        trackedAddedAtById = state.trackedAddedAtById.toMap(),
                        pendingShareImportTrack = state.pendingShareImportTrack,
                        nowMillis = pendingShareImportNowMillis.value
                    )
                },
                pendingShareImportNowMillis
            ) { input, nowMillis ->
                input.copy(nowMillis = nowMillis)
            }.collectLatest { input ->
                _contentDerivedState.value = repository.buildContentState(input)
            }
        }
    }

    override fun onCleared() {
        contentStateJob?.cancel()
        pendingShareImportClockJob?.cancel()
        onlineShareTargetsJob?.cancel()
        downloaderOptionsJob?.cancel()
        snapshotFlowManager.dispose()
        super.onCleared()
    }
}
