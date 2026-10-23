package os.kei.ui.page.main.github.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.GitHubReleaseNotesTarget
import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecar
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidTrustPolicy
import os.kei.feature.github.model.FdroidVersionSelectionMode
import os.kei.feature.github.model.GitHubActionsArtifactMatch
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.githubAssetSourceSignature
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.actions.GitHubActionsSectionExpansionState
import os.kei.ui.page.main.github.install.GitHubApkInstallState
import os.kei.ui.page.main.github.section.GitHubTrackedReleaseExpansionState

@Stable
internal class GitHubPageState(
    pageUiState: GitHubPageUiState = GitHubPageUiState(),
    actionsSectionExpansionState: GitHubActionsSectionExpansionState = GitHubActionsSectionExpansionState(),
    private val editorRecovery: GitHubEditorRecovery? = null,
) {
    private var restoringEditor = false
    private var editorMutationDepth = 0
    private var recoveredEditorPending = false
    private var recoveredEditTargetId: String? = null
    var trackDataReady by mutableStateOf(false)
        private set
    var editorSessionToken: Long = 0
        private set
    private val sheetState = GitHubPageSheetStateHolder()
    private val actionsState = GitHubActionsPageStateHolder(actionsSectionExpansionState)
    private val assetState = GitHubAssetPageStateHolder()
    private val trackEditorState = GitHubTrackEditorPageStateHolder(::persistTrackEditor)
    private val strategyState = GitHubStrategyPageStateHolder()
    private val overviewState = GitHubOverviewPageStateHolder()

    /** Shared with the history pages' asset rows, which run the same APK info and install flow. */
    val apkInstall = GitHubApkInstallState()

    var trackedSearch by mutableStateOf("")
    var trackedFilterMode by mutableStateOf(pageUiState.trackedFilterMode)
    var repoUrlInput by trackEditorState::repoUrlInput
    var packageNameInput by trackEditorState::packageNameInput
    var repoScanCandidates by trackEditorState::repoScanCandidates
    var appSearch by trackEditorState::appSearch
    var addTrackAppPickerFirstVisibleItemIndex by trackEditorState::addTrackAppPickerFirstVisibleItemIndex
    var addTrackAppPickerFirstVisibleItemScrollOffset by trackEditorState::addTrackAppPickerFirstVisibleItemScrollOffset
    var pickerExpanded by trackEditorState::pickerExpanded
    var showAddSheet: Boolean
        get() = sheetState.showAddSheet
        set(value) {
            if (sheetState.showAddSheet == value) return
            sheetState.showAddSheet = value
            persistTrackEditor()
        }
    var showStrategySheet by sheetState::showStrategySheet
    var showCheckLogicSheet by sheetState::showCheckLogicSheet
    var showDroidSourcesSheet by sheetState::showDroidSourcesSheet
    var showDebugSheet by sheetState::showDebugSheet
    var showActionsSheet by sheetState::showActionsSheet
    var showDownloaderPopup by sheetState::showDownloaderPopup
    var editingTrackedItem: GitHubTrackedApp?
        get() = sheetState.editingTrackedItem
        set(value) {
            if (sheetState.editingTrackedItem == value) return
            sheetState.editingTrackedItem = value
            persistTrackEditor()
        }
    var actionsTargetItem by sheetState::actionsTargetItem
    var actionsLoading by actionsState::actionsLoading
    var actionsRunsLoading by actionsState::actionsRunsLoading
    var actionsError by actionsState::actionsError
    var actionsAuthMode by actionsState::actionsAuthMode
    var debugActionsUpdateNotificationLoading by actionsState::debugActionsUpdateNotificationLoading
    var actionsDefaultBranch by actionsState::actionsDefaultBranch
    var actionsSelectedBranch by actionsState::actionsSelectedBranch
    var actionsBranchManuallySelected by actionsState::actionsBranchManuallySelected
    var actionsBranchOptions by actionsState::actionsBranchOptions
    var actionsRawWorkflows by actionsState::actionsRawWorkflows
    var actionsWorkflowSignals by actionsState::actionsWorkflowSignals
    var actionsWorkflows by actionsState::actionsWorkflows
    var actionsSelectedWorkflowId by actionsState::actionsSelectedWorkflowId
    var actionsWorkflowManuallySelected by actionsState::actionsWorkflowManuallySelected
    var actionsSnapshot by actionsState::actionsSnapshot
    var actionsRuns by actionsState::actionsRuns
    var actionsRunLimit by actionsState::actionsRunLimit
    var actionsSelectedRunId by actionsState::actionsSelectedRunId
    var actionsBranchesExpanded by actionsState::actionsBranchesExpanded
    var actionsWorkflowsExpanded by actionsState::actionsWorkflowsExpanded
    var actionsRunsExpanded by actionsState::actionsRunsExpanded
    var actionsArtifactsExpanded by actionsState::actionsArtifactsExpanded
    var actionsArtifactFilter by actionsState::actionsArtifactFilter
    var actionsDownloadHistory by actionsState::actionsDownloadHistory
    var actionsRunTrackingPlans by actionsState::actionsRunTrackingPlans
    var actionsArtifactDownloadLoadingId by actionsState::actionsArtifactDownloadLoadingId
    var actionsArtifactShareLoadingId by actionsState::actionsArtifactShareLoadingId
    var actionsRunWatchJob by actionsState::actionsRunWatchJob
    var debugBackgroundFullRefreshLoading by actionsState::debugBackgroundFullRefreshLoading
    var debugBackgroundDueRefreshLoading by actionsState::debugBackgroundDueRefreshLoading
    var debugForceDueRefreshLoading by actionsState::debugForceDueRefreshLoading
    var debugVisibleIncrementalRefreshLoading by actionsState::debugVisibleIncrementalRefreshLoading
    var preferPreReleaseInput by trackEditorState::preferPreReleaseInput
    var alwaysShowLatestReleaseDownloadButtonInput by trackEditorState::alwaysShowLatestReleaseDownloadButtonInput
    var checkActionsUpdatesInput by trackEditorState::checkActionsUpdatesInput
    var externalBuildUntilReleaseInput by trackEditorState::externalBuildUntilReleaseInput
    var updateIntervalModeInput by trackEditorState::updateIntervalModeInput
    var actionsUpdateIntervalModeInput by trackEditorState::actionsUpdateIntervalModeInput
    var preciseApkVersionModeInput by trackEditorState::preciseApkVersionModeInput
    var ignoreModeInput by trackEditorState::ignoreModeInput
    var ignoredStableReleaseKeyInput by trackEditorState::ignoredStableReleaseKeyInput
    var ignoredPreReleaseKeyInput by trackEditorState::ignoredPreReleaseKeyInput
    var fdroidVersionSelectionModeInput by trackEditorState::fdroidVersionSelectionModeInput
    var fdroidVersionNameRegexInput by trackEditorState::fdroidVersionNameRegexInput
    var fdroidApkNameRegexInput by trackEditorState::fdroidApkNameRegexInput
    var fdroidTrustPolicyInput by trackEditorState::fdroidTrustPolicyInput
    var fdroidAntiFeaturePolicyInput by trackEditorState::fdroidAntiFeaturePolicyInput
    var fdroidRepoScopeIdInput by trackEditorState::fdroidRepoScopeIdInput
    var fdroidAppSearchQueryInput by trackEditorState::fdroidAppSearchQueryInput
    var fdroidAppSearchCandidates by trackEditorState::fdroidAppSearchCandidates
    var fdroidAppSearchFailures by trackEditorState::fdroidAppSearchFailures
    var fdroidAppSearchFailuresExpanded by trackEditorState::fdroidAppSearchFailuresExpanded
    var fdroidAppSearchRepoReports by trackEditorState::fdroidAppSearchRepoReports
    var fdroidSelectedCandidate by trackEditorState::fdroidSelectedCandidate
    var fdroidAppSearchRunning by trackEditorState::fdroidAppSearchRunning
    var trackSourceModeInput by trackEditorState::trackSourceModeInput
    var sourceModeDropdownExpanded by trackEditorState::sourceModeDropdownExpanded
    var updateIntervalDropdownExpanded by trackEditorState::updateIntervalDropdownExpanded
    var actionsIntervalDropdownExpanded by trackEditorState::actionsIntervalDropdownExpanded
    var preciseModeDropdownExpanded by trackEditorState::preciseModeDropdownExpanded
    var ignoreModeDropdownExpanded by trackEditorState::ignoreModeDropdownExpanded
    var fdroidVersionSelectionDropdownExpanded by trackEditorState::fdroidVersionSelectionDropdownExpanded
    var fdroidTrustPolicyDropdownExpanded by trackEditorState::fdroidTrustPolicyDropdownExpanded
    var fdroidAntiFeaturePolicyDropdownExpanded by trackEditorState::fdroidAntiFeaturePolicyDropdownExpanded
    var fdroidRepoScopeDropdownExpanded by trackEditorState::fdroidRepoScopeDropdownExpanded
    var repoUrlScanRunning by trackEditorState::repoUrlScanRunning
    var packageNameScanRunning by trackEditorState::packageNameScanRunning
    var selectedApp by trackEditorState::selectedApp
    var appList by trackEditorState::appList
    var appListLoaded by trackEditorState::appListLoaded
    var appListRefreshing by trackEditorState::appListRefreshing
    var hasAutoRequestedPermission by mutableStateOf(false)
    var hasInitialized by mutableStateOf(false)
    var hasActiveInitialized by mutableStateOf(false)
    var lastTrackStoreSignalVersion by mutableStateOf(0L)
    var deferredTrackStoreSyncAfterRefresh by mutableStateOf(false)
    var showActionMenuPopup by sheetState::showActionMenuPopup
    var showOnlineShareTargetPopup by sheetState::showOnlineShareTargetPopup
    var pendingTrackImportPreview by sheetState::pendingTrackImportPreview
    var pendingShareImportPreview by sheetState::pendingShareImportPreview
    var pendingShareImportTrack by sheetState::pendingShareImportTrack
    var pendingShareImportAttachCandidate by sheetState::pendingShareImportAttachCandidate
    var pendingShareImportResult by sheetState::pendingShareImportResult
    var trackCardFocusRequest by mutableStateOf<GitHubTrackCardFocusRequest?>(null)
    private var nextTrackCardFocusRequestVersion by mutableIntStateOf(0)
    var decisionAssistDetailRequest by sheetState::decisionAssistDetailRequest
    var fdroidDetailRequest by sheetState::fdroidDetailRequest
    var actionsArtifactDetailRequest by sheetState::actionsArtifactDetailRequest
    var apkInfoDetailRequest by apkInstall::apkInfoDetailRequest
    var managedInstallConfirmRequest by apkInstall::managedInstallConfirmRequest
    var shareImportResolving by mutableStateOf(false)
    var sortMode by mutableStateOf(pageUiState.sortMode)
    var sortDirection by mutableStateOf(pageUiState.sortDirection)
    /** Pinned track ids, newest first. Persisted -- a pin is a decision, not a session detail. */
    var pinnedTrackIds by mutableStateOf(pageUiState.pinnedTrackIds)
    /**
     * Track ids currently held in the wide layout's second lane.
     *
     * A card lands here by being expanded and *stays* after it is collapsed again, so a reader can open
     * one, read it, close it, and still have it where they left it rather than watching it jump back
     * across the page. Deliberately not persisted and cleared on leaving the page: it is a record of
     * what this visit opened, and carrying it into the next visit would make the lane fill up over time
     * with things nobody asked to keep.
     */
    var laneDetainedTrackIds by mutableStateOf(emptyList<String>())
    var pendingDeleteItem by sheetState::pendingDeleteItem
    var overviewRefreshState by overviewState::overviewRefreshState
    var lastRefreshMs by overviewState::lastRefreshMs
    var refreshIntervalHours by overviewState::refreshIntervalHours
    var refreshProgress by overviewState::refreshProgress
    var refreshSessionId by overviewState::refreshSessionId
    var lookupConfig by strategyState::lookupConfig
    var selectedStrategyInput by strategyState::selectedStrategyInput
    var selectedActionsStrategyInput by strategyState::selectedActionsStrategyInput
    var githubApiTokenInput by strategyState::githubApiTokenInput
    var checkAllTrackedPreReleasesInput by strategyState::checkAllTrackedPreReleasesInput
    var checkAllDirectApkPreReleasesInput by strategyState::checkAllDirectApkPreReleasesInput
    var aggressiveApkFilteringInput by strategyState::aggressiveApkFilteringInput
    var preciseApkVersionEnabledInput by strategyState::preciseApkVersionEnabledInput
    var scanSystemAppsByDefaultInput by strategyState::scanSystemAppsByDefaultInput
    var profileDepthInput by strategyState::profileDepthInput
    var shareImportFlowModeInput by strategyState::shareImportFlowModeInput
    var appManagedShareInstallEnabledInput by strategyState::appManagedShareInstallEnabledInput
    var foregroundManagedDownloadBoostEnabledInput by
        strategyState::foregroundManagedDownloadBoostEnabledInput
    var onlineShareTargetPackageInput by strategyState::onlineShareTargetPackageInput
    var preferredDownloaderPackageInput by strategyState::preferredDownloaderPackageInput
    var decisionAssistEnabledInput by strategyState::decisionAssistEnabledInput
    var repositoryHealthCardEnabledInput by strategyState::repositoryHealthCardEnabledInput
    var apkTrustCheckEnabledInput by strategyState::apkTrustCheckEnabledInput
    var fdroidCommonRepoIdsInput by strategyState::fdroidCommonRepoIdsInput
    var showApiTokenPlainText by strategyState::showApiTokenPlainText
    var showShareImportFlowModePopup by strategyState::showShareImportFlowModePopup
    var strategyBenchmarkRunning by strategyState::strategyBenchmarkRunning
    var strategyBenchmarkError by strategyState::strategyBenchmarkError
    var strategyBenchmarkReport by strategyState::strategyBenchmarkReport
    var credentialCheckRunning by strategyState::credentialCheckRunning
    var credentialCheckError by strategyState::credentialCheckError
    var credentialCheckStatus by strategyState::credentialCheckStatus
    var recommendedTokenGuideExpanded by strategyState::recommendedTokenGuideExpanded
    var assetSourceSignature by strategyState::assetSourceSignature
    var refreshAllJob by overviewState::refreshAllJob
    var actionsRecommendedRunRefreshJob by actionsState::actionsRecommendedRunRefreshJob
    var refreshTargetIds by overviewState::refreshTargetIds
    var deleteInProgress by overviewState::deleteInProgress

    val trackedItems = mutableStateListOf<GitHubTrackedApp>()
    val checkStates = mutableStateMapOf<String, VersionCheckUi>()
    val apkAssetBundles get() = assetState.apkAssetBundles
    val apkAssetLoading get() = assetState.apkAssetLoading
    val apkAssetErrors get() = assetState.apkAssetErrors
    val apkAssetExpanded get() = assetState.apkAssetExpanded
    val apkAssetIncludeAll get() = assetState.apkAssetIncludeAll
    val releaseNotesLoading get() = assetState.releaseNotesLoading
    val releaseNotesErrors get() = assetState.releaseNotesErrors
    val releaseNotesTargets get() = assetState.releaseNotesTargets
    val releaseNotesSelectedTargets get() = assetState.releaseNotesSelectedTargets
    val releaseNotesBundles get() = assetState.releaseNotesBundles
    val releaseNotesApkVersions get() = assetState.releaseNotesApkVersions
    val apkAssetBundleLoadedAtMs get() = assetState.apkAssetBundleLoadedAtMs
    val releaseNotesTargetsLoadedAtMs get() = assetState.releaseNotesTargetsLoadedAtMs
    val releaseNotesBundleLoadedAtMs get() = assetState.releaseNotesBundleLoadedAtMs
    val apkInfoLoading get() = apkInstall.apkInfoLoading
    val apkInfoErrors get() = apkInstall.apkInfoErrors
    val apkInfoResults get() = apkInstall.apkInfoResults
    val apkInfoInstalledResults get() = apkInstall.apkInfoInstalledResults
    val managedInstallLoading get() = apkInstall.managedInstallLoading
    val itemRefreshLoading = mutableStateMapOf<String, Boolean>()
    val actionsStatusRefreshingRunIds get() = actionsState.actionsStatusRefreshingRunIds
    val actionsRecommendedRunSnapshots get() = actionsState.actionsRecommendedRunSnapshots
    val trackedFirstInstallAtByPackage = mutableStateMapOf<String, Long>()
    val trackedAddedAtById = mutableStateMapOf<String, Long>()
    val trackedModifiedAtById = mutableStateMapOf<String, Long>()

    fun applyPersistedUiState(snapshot: GitHubPagePersistedUiState) {
        val defaultPageUiState = GitHubPageUiState()
        val defaultActionsState = GitHubActionsSectionExpansionState()
        if (trackedFilterMode == defaultPageUiState.trackedFilterMode) {
            trackedFilterMode = snapshot.pageUiState.trackedFilterMode
        }
        if (sortMode == defaultPageUiState.sortMode) {
            sortMode = snapshot.pageUiState.sortMode
        }
        if (sortDirection == defaultPageUiState.sortDirection) {
            sortDirection = snapshot.pageUiState.sortDirection
        }
        if (pinnedTrackIds == defaultPageUiState.pinnedTrackIds) {
            pinnedTrackIds = snapshot.pageUiState.pinnedTrackIds
        }
        if (actionsBranchesExpanded == defaultActionsState.branchesExpanded) {
            actionsBranchesExpanded = snapshot.actionsSectionExpansionState.branchesExpanded
        }
        if (actionsWorkflowsExpanded == defaultActionsState.workflowsExpanded) {
            actionsWorkflowsExpanded = snapshot.actionsSectionExpansionState.workflowsExpanded
        }
        if (actionsRunsExpanded == defaultActionsState.runsExpanded) {
            actionsRunsExpanded = snapshot.actionsSectionExpansionState.runsExpanded
        }
    }

    fun requestTrackCardFocus(trackId: String) {
        val normalizedTrackId = trackId.trim()
        if (normalizedTrackId.isBlank()) return
        nextTrackCardFocusRequestVersion += 1
        trackCardFocusRequest =
            GitHubTrackCardFocusRequest(
                trackId = normalizedTrackId,
                version = nextTrackCardFocusRequestVersion,
            )
    }

    fun resetTrackEditorDropdownState() {
        trackEditorState.resetDropdownState()
    }

    fun consumeTrackCardFocus(request: GitHubTrackCardFocusRequest) {
        if (trackCardFocusRequest?.version == request.version) {
            trackCardFocusRequest = null
        }
    }

    fun activeStrategyId(): String = lookupConfig.selectedStrategy.storageId

    fun buildAssetSourceSignature(config: GitHubLookupConfig = lookupConfig): String = config.githubAssetSourceSignature()

    fun matchesAssetSourceSignature(
        bundle: GitHubReleaseAssetBundle,
        config: GitHubLookupConfig = lookupConfig,
    ): Boolean =
        bundle.sourceConfigSignature.isNotBlank() &&
            bundle.sourceConfigSignature == buildAssetSourceSignature(config)

    fun clearAllAssetUiState() {
        apkAssetBundles.clear()
        apkAssetLoading.clear()
        apkAssetErrors.clear()
        apkAssetExpanded.clear()
        apkAssetIncludeAll.clear()
        releaseNotesLoading.clear()
        releaseNotesErrors.clear()
        releaseNotesTargets.clear()
        releaseNotesSelectedTargets.clear()
        releaseNotesBundles.clear()
        releaseNotesApkVersions.clear()
        apkAssetBundleLoadedAtMs.clear()
        releaseNotesTargetsLoadedAtMs.clear()
        releaseNotesBundleLoadedAtMs.clear()
        apkInstall.clearApkInfo()
    }

    fun clearAssetRuntimeState(itemId: String) {
        apkAssetExpanded.remove(itemId)
        apkAssetLoading.remove(itemId)
        apkAssetErrors.remove(itemId)
        apkAssetBundles.remove(itemId)
        releaseNotesLoading.remove(itemId)
        releaseNotesErrors.remove(itemId)
        releaseNotesTargets.remove(itemId)
        releaseNotesSelectedTargets.remove(itemId)
        releaseNotesBundles.remove(itemId)
        releaseNotesApkVersions.keys.removeAll { key -> key.startsWith("$itemId|") }
        apkAssetBundleLoadedAtMs.remove(itemId)
        releaseNotesTargetsLoadedAtMs.remove(itemId)
        releaseNotesBundleLoadedAtMs.remove(itemId)
    }

    fun clearAssetUiState(itemId: String) {
        clearAssetRuntimeState(itemId)
        apkAssetIncludeAll.remove(itemId)
    }

    fun retainTrackedUiState(validItemIds: Set<String>) {
        apkAssetExpanded.keys.retainAll(validItemIds)
        apkAssetIncludeAll.keys.retainAll(validItemIds)
        itemRefreshLoading.keys.retainAll(validItemIds)
        actionsRecommendedRunSnapshots.keys.retainAll(validItemIds)
        apkAssetLoading.keys.retainAll(validItemIds)
        apkAssetErrors.keys.retainAll(validItemIds)
        apkAssetBundles.keys.retainAll(validItemIds)
        releaseNotesLoading.keys.retainAll(validItemIds)
        releaseNotesErrors.keys.retainAll(validItemIds)
        releaseNotesTargets.keys.retainAll(validItemIds)
        releaseNotesSelectedTargets.keys.retainAll(validItemIds)
        releaseNotesBundles.keys.retainAll(validItemIds)
        apkAssetBundleLoadedAtMs.keys.retainAll(validItemIds)
        releaseNotesTargetsLoadedAtMs.keys.retainAll(validItemIds)
        releaseNotesBundleLoadedAtMs.keys.retainAll(validItemIds)
        releaseNotesApkVersions.keys.removeAll { key ->
            validItemIds.none { itemId -> key.startsWith("$itemId|") }
        }
    }

    fun recordTrackedFirstInstallAt(
        packageName: String,
        firstInstallAtMillis: Long,
    ) {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) return
        if (firstInstallAtMillis <= 0L) return
        val current = trackedFirstInstallAtByPackage[normalizedPackageName]
        if (current == null || current <= 0L || firstInstallAtMillis < current) {
            trackedFirstInstallAtByPackage[normalizedPackageName] = firstInstallAtMillis
        }
    }

    fun retainTrackedFirstInstallAtByTrackedItems() {
        val trackedPackages =
            trackedItems
                .map { it.packageName.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        trackedFirstInstallAtByPackage.keys.retainAll(trackedPackages)
    }

    fun recordTrackedAddedAt(
        trackId: String,
        addedAtMillis: Long,
    ) {
        val normalizedTrackId = trackId.trim()
        if (normalizedTrackId.isBlank()) return
        if (addedAtMillis <= 0L) return
        val current = trackedAddedAtById[normalizedTrackId]
        if (current == null || current <= 0L || addedAtMillis < current) {
            trackedAddedAtById[normalizedTrackId] = addedAtMillis
        }
    }

    fun retainTrackedAddedAtByTrackedItems() {
        val trackedIds =
            trackedItems
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        trackedAddedAtById.keys.retainAll(trackedIds)
    }

    fun recordTrackedModifiedAt(
        trackId: String,
        modifiedAtMillis: Long,
    ) {
        val normalizedTrackId = trackId.trim()
        if (normalizedTrackId.isBlank()) return
        if (modifiedAtMillis <= 0L) return
        trackedModifiedAtById[normalizedTrackId] = modifiedAtMillis
    }

    fun retainTrackedModifiedAtByTrackedItems() {
        val trackedIds =
            trackedItems
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        trackedModifiedAtById.keys.retainAll(trackedIds)
    }

    fun dismissStrategySheet() {
        showStrategySheet = false
        strategyState.dismissStrategySheet()
    }

    fun dismissCheckLogicSheet() {
        showDownloaderPopup = false
        showOnlineShareTargetPopup = false
        showShareImportFlowModePopup = false
        pendingTrackImportPreview = null
        showCheckLogicSheet = false
    }

    fun resetActionsSheetState() {
        actionsRunWatchJob?.cancel()
        actionsRunWatchJob = null
        actionsLoading = false
        actionsRunsLoading = false
        actionsError = null
        actionsAuthMode = null
        actionsDefaultBranch = ""
        actionsSelectedBranch = ""
        actionsBranchManuallySelected = false
        actionsBranchOptions = emptyList()
        actionsRawWorkflows = emptyList()
        actionsWorkflowSignals = emptyMap()
        actionsWorkflows = emptyList()
        actionsSelectedWorkflowId = null
        actionsWorkflowManuallySelected = false
        actionsSnapshot = null
        actionsRuns = emptyList()
        actionsRunLimit = 6
        actionsSelectedRunId = null
        actionsArtifactsExpanded = true
        actionsArtifactFilter = GitHubActionsArtifactFilter.Recommended
        actionsDownloadHistory = emptyList()
        actionsRunTrackingPlans = emptyMap()
        actionsArtifactDownloadLoadingId = null
        actionsArtifactShareLoadingId = null
        actionsArtifactDetailRequest = null
        actionsStatusRefreshingRunIds.clear()
    }

    fun dismissActionsSheet() {
        showActionsSheet = false
        actionsTargetItem = null
        resetActionsSheetState()
    }

    fun dismissTrackImportPreview() {
        pendingTrackImportPreview = null
    }

    fun dismissShareImportPreview() {
        pendingShareImportPreview = null
    }

    fun resetTrackEditor() {
        showAddSheet = false
        editingTrackedItem = null
        trackEditorState.reset()
    }

    fun dismissTrackSheet() {
        showAddSheet = false
        resetTrackEditor()
    }

    fun reconcileRecoveredTrackEditor(items: List<GitHubTrackedApp>) {
        trackDataReady = true
        if (!recoveredEditorPending) return
        val targetId = recoveredEditTargetId
        recoveredEditorPending = false
        recoveredEditTargetId = null
        replaceTrackEditor {
            val current = targetId?.let { id -> items.firstOrNull { it.id == id } }
            if (targetId != null && current == null) {
                dismissTrackSheet()
            } else {
                editingTrackedItem = current
                showAddSheet = true
            }
        }
    }

    fun replaceTrackEditor(block: () -> Unit) {
        recoveredEditorPending = false
        editorMutationDepth++
        try {
            block()
        } finally {
            editorMutationDepth--
            if (editorMutationDepth == 0) persistTrackEditor()
        }
    }

    fun completeTrackEditorSave(expectedToken: Long) {
        if (editorSessionToken == expectedToken) replaceTrackEditor { dismissTrackSheet() }
    }

    private fun persistTrackEditor() {
        if (!restoringEditor && !recoveredEditorPending && editorMutationDepth == 0) {
            editorSessionToken++
            editorRecovery?.save(this)
        }
    }

    init {
        restoringEditor = true
        recoveredEditorPending = editorRecovery?.restore(this) == true
        restoringEditor = false
        recoveredEditTargetId = editingTrackedItem?.id
    }
}

internal data class GitHubApkInfoDetailRequest(
    val item: GitHubTrackedApp,
    val asset: GitHubReleaseAssetFile,
)

internal data class GitHubManagedInstallConfirmRequest(
    val item: GitHubTrackedApp,
    val asset: GitHubReleaseAssetFile,
)

internal data class GitHubFdroidDetailRequest(
    val item: GitHubTrackedApp,
    val sidecar: FdroidMetadataSidecar? = null,
    val loading: Boolean = true,
)

internal fun GitHubTrackedApp.githubManagedInstallKey(asset: GitHubReleaseAssetFile): String =
    listOf(
        id,
        owner,
        repo,
        asset.name,
        asset.downloadUrl,
        asset.apiAssetUrl,
        asset.sizeBytes.toString(),
        asset.digest,
        asset.signerSha256.joinToString(","),
    ).joinToString("|")

internal enum class GitHubDecisionAssistDetailType {
    RepositoryHealth,
    ReleaseNotes,
}

internal enum class GitHubActionsArtifactFilter {
    Recommended,
    Alternatives,
    All,
}

internal data class GitHubDecisionAssistDetailRequest(
    val type: GitHubDecisionAssistDetailType,
    val item: GitHubTrackedApp,
)

internal data class GitHubActionsArtifactDetailRequest(
    val runMatch: GitHubActionsRunMatch,
    val artifactMatch: GitHubActionsArtifactMatch,
    val recommended: Boolean,
)

internal fun GitHubReleaseAssetFile.githubApkInfoKey(): String =
    listOf(name, downloadUrl, apiAssetUrl, sizeBytes.toString()).joinToString("|")

internal fun releaseNotesApkVersionKey(
    itemId: String,
    target: GitHubReleaseNotesTarget,
): String =
    listOf(
        itemId.trim(),
        target.tagName.trim(),
        target.htmlUrl.trim(),
    ).joinToString("|")

@Composable
internal fun rememberGitHubPageState(viewModel: GitHubPageViewModel): GitHubPageState {
    return remember(viewModel) {
        viewModel.pageState()
    }
}
