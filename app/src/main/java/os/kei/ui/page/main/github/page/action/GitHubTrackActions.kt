package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import os.kei.BuildConfig
import os.kei.R
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.FdroidTrustPolicy
import os.kei.feature.github.model.FdroidVersionSelectionMode
import os.kei.feature.github.model.GitHubApkPackageNameScanRequest
import os.kei.feature.github.model.GitHubPackageRepositoryScanCandidate
import os.kei.feature.github.model.GitHubPackageRepositoryScanRequest
import os.kei.feature.github.model.GitHubReleaseIgnoreChannel
import os.kei.feature.github.model.GitHubTrackedActionsUpdateIntervalMode
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedPreciseApkVersionMode
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode
import os.kei.feature.github.model.InstalledAppItem
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import os.kei.feature.github.model.buildGitHubReleaseIgnoreKey
import os.kei.feature.github.model.coerceForSource
import os.kei.feature.github.model.defaultKeiOsTrackedApp
import os.kei.feature.github.model.hasSameGitHubTrackingConfigIgnoringLocalAppType
import os.kei.feature.github.model.withReleaseIgnoreMode
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.localizedGitHubPageErrorMessage
import os.kei.ui.page.main.github.localizedGitHubRepositoryDiscoveryErrorMessage
import os.kei.ui.page.main.github.page.GitHubTrackEditorDraft
import os.kei.ui.page.main.github.page.GitHubTrackEditorResult

internal class GitHubTrackActions(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions,
    private val assetActions: GitHubAssetActions,
) {
    private var appListRefreshJob: Job? = null
    private var trackEditorSaveJob: Job? = null
    private val fdroidSearchActions = GitHubFdroidTrackSearchActions(env)
    private val state get() = env.state
    private val scope get() = env.scope
    private val repository get() = env.repository

    fun openTrackSheetForAdd() {
        fdroidSearchActions.cancel()
        state.replaceTrackEditorForAdd()
    }

    fun openTrackSheetForEdit(item: GitHubTrackedApp) {
        fdroidSearchActions.cancel()
        state.replaceTrackEditorForEdit(item)
        ensureAppListForTrackSheet()
    }

    fun dismissTrackSheet() {
        fdroidSearchActions.cancel()
        state.dismissTrackSheet()
    }

    fun setTrackAppPickerExpanded(value: Boolean) {
        state.pickerExpanded = value
        if (value) {
            ensureAppListForTrackSheet()
        }
    }

    fun setRepoUrlInput(value: String) {
        fdroidSearchActions.cancel()
        state.repoUrlInput = value
        state.repoScanCandidates = emptyList()
        state.fdroidSelectedCandidate = null
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        if (state.trackSourceModeInput == GitHubTrackedSourceMode.FdroidRepository) {
            state.fdroidRepoScopeIdInput =
                FdroidRepositoryPresets.presetForRepoUrl(value)?.id
                    ?: FdroidRepositoryPresets.CUSTOM_ID
        }
    }

    fun setTrackSourceModeInput(value: GitHubTrackedSourceMode) {
        fdroidSearchActions.cancel()
        state.trackSourceModeInput = value
        state.updateIntervalModeInput = state.updateIntervalModeInput.coerceForSource(value)
        state.repoScanCandidates = emptyList()
        state.fdroidSelectedCandidate = null
        state.fdroidAppSearchCandidates = emptyList()
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        state.resetTrackEditorDropdownState()
        if (value == GitHubTrackedSourceMode.FdroidRepository && state.repoUrlInput.isBlank()) {
            state.repoUrlInput = FdroidRepositoryPresets.Main.repoUrl
            state.fdroidRepoScopeIdInput = FdroidRepositoryPresets.COMMON_ID
        }
        if (value != GitHubTrackedSourceMode.GitHubRepository) {
            state.externalBuildUntilReleaseInput = false
            state.alwaysShowLatestReleaseDownloadButtonInput = false
            state.checkActionsUpdatesInput = false
            state.actionsUpdateIntervalModeInput = GitHubTrackedActionsUpdateIntervalMode.FollowGlobal
        }
    }

    fun setAppSearch(value: String) {
        state.appSearch = value
    }

    fun setPackageNameInput(value: String) {
        fdroidSearchActions.cancel()
        state.packageNameInput = value
        state.repoScanCandidates = emptyList()
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        val selectedCandidate = state.fdroidSelectedCandidate
        if (selectedCandidate != null &&
            !selectedCandidate.packageName.equals(value.trim(), ignoreCase = true)
        ) {
            state.fdroidSelectedCandidate = null
        }
        val selected = state.selectedApp
        val normalizedInput = value.trim()
        if (selected != null) {
            if (normalizedInput.isBlank() ||
                !selected.packageName.equals(normalizedInput, ignoreCase = true)
            ) {
                state.selectedApp = null
            }
        }
    }

    fun setAddAppPickerScrollPosition(
        index: Int,
        offset: Int,
    ) {
        state.addTrackAppPickerFirstVisibleItemIndex = index
        state.addTrackAppPickerFirstVisibleItemScrollOffset = offset
    }

    fun setSelectedApp(value: InstalledAppItem?) {
        fdroidSearchActions.cancel()
        state.selectedApp = value
        state.repoScanCandidates = emptyList()
        state.fdroidSelectedCandidate = null
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        if (value != null) {
            state.packageNameInput = value.packageName
            if (state.trackSourceModeInput == GitHubTrackedSourceMode.FdroidRepository) {
                state.fdroidAppSearchQueryInput = value.label
            }
        }
    }

    fun setFdroidRepoScopeIdInput(value: String) {
        fdroidSearchActions.cancel()
        val normalized = value.trim().ifBlank { FdroidRepositoryPresets.COMMON_ID }
        state.fdroidRepoScopeIdInput = normalized
        state.fdroidSelectedCandidate = null
        state.fdroidAppSearchCandidates = emptyList()
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
        if (normalized != FdroidRepositoryPresets.COMMON_ID &&
            normalized != FdroidRepositoryPresets.CUSTOM_ID
        ) {
            FdroidRepositoryPresets.presetForId(normalized)?.let { preset ->
                state.repoUrlInput = preset.repoUrl
            }
        }
    }

    fun setFdroidAppSearchQueryInput(value: String) {
        fdroidSearchActions.cancel()
        state.fdroidAppSearchQueryInput = value
        state.fdroidSelectedCandidate = null
        state.fdroidAppSearchFailures = emptyList()
        state.fdroidAppSearchFailuresExpanded = false
        state.fdroidAppSearchRepoReports = emptyList()
    }

    fun setFdroidSearchFailuresExpanded(value: Boolean) {
        state.fdroidAppSearchFailuresExpanded = value
    }

    fun setFdroidRepoScopeDropdownExpanded(value: Boolean) {
        state.fdroidRepoScopeDropdownExpanded = value
    }

    fun setPreferPreReleaseInput(value: Boolean) {
        state.preferPreReleaseInput = value
    }

    fun setAlwaysShowLatestReleaseDownloadButtonInput(value: Boolean) {
        state.alwaysShowLatestReleaseDownloadButtonInput = value
    }

    fun setCheckActionsUpdatesInput(value: Boolean) {
        state.checkActionsUpdatesInput = value
        if (!value) {
            state.actionsUpdateIntervalModeInput = GitHubTrackedActionsUpdateIntervalMode.FollowGlobal
        }
    }

    fun setExternalBuildUntilReleaseInput(value: Boolean) {
        state.externalBuildUntilReleaseInput = value
    }

    fun setUpdateIntervalModeInput(value: GitHubTrackedUpdateIntervalMode) {
        state.updateIntervalModeInput = value.coerceForSource(state.trackSourceModeInput)
    }

    fun setActionsUpdateIntervalModeInput(value: GitHubTrackedActionsUpdateIntervalMode) {
        state.actionsUpdateIntervalModeInput = value
    }

    fun setPreciseApkVersionModeInput(value: GitHubTrackedPreciseApkVersionMode) {
        state.preciseApkVersionModeInput = value
    }

    fun setIgnoreModeInput(value: GitHubTrackedIgnoreMode) {
        state.ignoreModeInput = value
        when (value) {
            GitHubTrackedIgnoreMode.None,
            GitHubTrackedIgnoreMode.Temporary,
            GitHubTrackedIgnoreMode.AllVersions -> {
                state.ignoredStableReleaseKeyInput = ""
                state.ignoredPreReleaseKeyInput = ""
            }

            GitHubTrackedIgnoreMode.CurrentStable -> {
                state.ignoredStableReleaseKeyInput =
                    currentEditingReleaseIgnoreKey(GitHubReleaseIgnoreChannel.Stable)
                state.ignoredPreReleaseKeyInput = ""
            }

            GitHubTrackedIgnoreMode.CurrentPreRelease -> {
                state.ignoredStableReleaseKeyInput = ""
                state.ignoredPreReleaseKeyInput =
                    currentEditingReleaseIgnoreKey(GitHubReleaseIgnoreChannel.PreRelease)
            }
        }
    }

    fun setSourceModeDropdownExpanded(value: Boolean) {
        state.sourceModeDropdownExpanded = value
    }

    fun setUpdateIntervalDropdownExpanded(value: Boolean) {
        state.updateIntervalDropdownExpanded = value
    }

    fun setActionsIntervalDropdownExpanded(value: Boolean) {
        state.actionsIntervalDropdownExpanded = value
    }

    fun setPreciseModeDropdownExpanded(value: Boolean) {
        state.preciseModeDropdownExpanded = value
    }

    fun setIgnoreModeDropdownExpanded(value: Boolean) {
        state.ignoreModeDropdownExpanded = value
    }

    fun setFdroidVersionSelectionModeInput(value: FdroidVersionSelectionMode) {
        state.fdroidVersionSelectionModeInput = value
        if (value != FdroidVersionSelectionMode.VersionNameRegex) {
            state.fdroidVersionNameRegexInput = ""
        }
    }

    fun setFdroidVersionNameRegexInput(value: String) {
        state.fdroidVersionNameRegexInput = value
    }

    fun setFdroidApkNameRegexInput(value: String) {
        state.fdroidApkNameRegexInput = value
    }

    fun setFdroidTrustPolicyInput(value: FdroidTrustPolicy) {
        state.fdroidTrustPolicyInput = value
    }

    fun setFdroidAntiFeaturePolicyInput(value: FdroidAntiFeaturePolicy) {
        state.fdroidAntiFeaturePolicyInput = value
    }

    fun setFdroidVersionSelectionDropdownExpanded(value: Boolean) {
        state.fdroidVersionSelectionDropdownExpanded = value
    }

    fun setFdroidTrustPolicyDropdownExpanded(value: Boolean) {
        state.fdroidTrustPolicyDropdownExpanded = value
    }

    fun setFdroidAntiFeaturePolicyDropdownExpanded(value: Boolean) {
        state.fdroidAntiFeaturePolicyDropdownExpanded = value
    }

    private fun ensureAppListForTrackSheet() {
        if (!state.appListLoaded) {
            refreshAppListForTrackSheet(forceRefresh = false)
        }
    }

    fun refreshAppListForTrackSheet(forceRefresh: Boolean = true) {
        if (appListRefreshJob?.isActive == true) return
        appListRefreshJob =
            scope.launch {
                refreshActions.reloadApps(
                    forceRefresh = forceRefresh,
                    includeSystemApps = true,
                )
                syncSelectedAppFromPackageInput()
            }
    }

    fun ensureKeiOsSelfTrack() {
        val newItem = defaultKeiOsTrackedApp(packageName = BuildConfig.APPLICATION_ID)
        if (state.trackedItems.any { it.id == newItem.id }) {
            env.toast(R.string.github_toast_track_exists)
            return
        }
        state.trackedItems.add(newItem)
        val nowMillis = env.clock.nowMs()
        state.recordTrackedAddedAt(newItem.id, nowMillis)
        state.recordTrackedModifiedAt(newItem.id, nowMillis)
        state.requestTrackCardFocus(newItem.id)
        env.saveTrackedItems(refreshTrackIds = setOf(newItem.id))
        env.toast(R.string.github_toast_track_current_app_added)
    }

    fun requestDeleteItem(item: GitHubTrackedApp) {
        if (state.deleteInProgress) return
        if (state.trackedItems.none { it.id == item.id }) return
        state.pendingDeleteItem = item
    }

    fun scanPackageNameFromRepo() {
        if (state.packageNameScanRunning || state.repoUrlScanRunning) return
        if (state.repoUrlInput.isBlank()) {
            env.toast(R.string.github_toast_fill_repo_and_select_app)
            return
        }
        state.packageNameScanRunning = true
        state.repoScanCandidates = emptyList()
        val sourceMode = state.trackSourceModeInput
        val request = GitHubApkPackageNameScanRequest(
            repoUrl = state.repoUrlInput,
            lookupConfig = state.lookupConfig,
            expectedPackageName = state.packageNameInput.trim(),
            includePreRelease = state.preferPreReleaseInput,
        )
        scope.launch {
            try {
                val result =
                    when (sourceMode) {
                        GitHubTrackedSourceMode.GitHubRepository -> {
                            repository.scanPackageNameFromRepositoryApk(
                                request,
                            )
                        }

                        GitHubTrackedSourceMode.DirectApk -> {
                            repository.scanPackageNameFromDirectApk(
                                repoUrl = request.repoUrl,
                                lookupConfig = request.lookupConfig,
                            )
                        }

                        GitHubTrackedSourceMode.GitRepository -> {
                            Result.failure(IllegalStateException("Git repository package scanning is unavailable"))
                        }

                        GitHubTrackedSourceMode.FdroidRepository -> {
                            Result.failure(IllegalStateException("F-Droid package scanning is unavailable"))
                        }
                    }.getOrElse { error ->
                        env.toast(
                            R.string.github_toast_package_scan_failed,
                            localizedGitHubPageErrorMessage(
                                context = env.context,
                                error = error,
                                fallbackMessage = env.string(R.string.github_error_package_scan_failed),
                            ),
                        )
                        return@launch
                    }
                refreshActions.reloadApps(
                    forceRefresh = true,
                    includeSystemApps = true,
                )
                state.packageNameInput = result.packageName
                state.selectedApp =
                    state.appList.firstOrNull { app ->
                        app.packageName.equals(result.packageName, ignoreCase = true)
                    }
                env.toast(R.string.github_toast_package_scan_success, result.packageName)
            } finally {
                state.packageNameScanRunning = false
            }
        }
    }

    fun searchFdroidAppsByName() {
        fdroidSearchActions.searchByName()
    }

    fun scanFdroidReposFromPackage() {
        fdroidSearchActions.scanReposFromPackage()
    }

    fun retryFdroidSearchFailures() {
        fdroidSearchActions.retryFailures()
    }

    fun selectFdroidAppSearchCandidate(candidate: FdroidAppSearchCandidate) {
        fdroidSearchActions.selectCandidate(candidate)
    }

    fun scanRepoUrlFromPackage() {
        if (state.repoUrlScanRunning || state.packageNameScanRunning) return
        if (state.trackSourceModeInput != GitHubTrackedSourceMode.GitHubRepository) return
        val packageName =
            state.packageNameInput.trim().ifBlank {
                state.selectedApp
                    ?.packageName
                    .orEmpty()
                    .trim()
            }
        if (packageName.isBlank()) {
            env.toast(R.string.github_toast_repo_scan_requires_package)
            return
        }
        state.repoScanCandidates = emptyList()
        val appLabel =
            state.selectedApp
                ?.label
                ?.trim()
                .orEmpty()
                .ifBlank {
                    state.appList
                        .firstOrNull { app ->
                            app.packageName.equals(packageName, ignoreCase = true)
                        }?.label
                        ?.trim()
                        .orEmpty()
                }
        state.repoUrlScanRunning = true
        scope.launch {
            try {
                val result =
                    repository
                        .scanRepositoryFromPackage(
                            GitHubPackageRepositoryScanRequest(
                                packageName = packageName,
                                appLabel = appLabel,
                                preferredRepoUrl = state.repoUrlInput.trim(),
                                lookupConfig = state.lookupConfig,
                            ),
                        ).getOrElse { error ->
                            env.toast(
                                R.string.github_toast_repo_scan_failed,
                                localizedGitHubRepositoryDiscoveryErrorMessage(env.context, error),
                            )
                            return@launch
                        }
                val candidate = result.matchedCandidates.firstOrNull()
                if (candidate == null) {
                    env.toast(R.string.github_toast_repo_scan_no_match)
                    return@launch
                }
                state.repoScanCandidates = result.matchedCandidates
                env.toast(
                    R.string.github_toast_repo_scan_candidates_found,
                    result.matchedCandidates.size,
                )
            } finally {
                state.repoUrlScanRunning = false
            }
        }
    }

    fun selectRepoScanCandidate(candidate: GitHubPackageRepositoryScanCandidate) {
        state.repoUrlInput = candidate.trackedApp.repoUrl
        state.packageNameInput = candidate.trackedApp.packageName
        state.selectedApp = state.appList.firstOrNull { app ->
            app.packageName.equals(candidate.trackedApp.packageName, ignoreCase = true)
        } ?: state.selectedApp?.takeIf { selected ->
            selected.packageName.equals(candidate.trackedApp.packageName, ignoreCase = true)
        }
        env.toast(
            R.string.github_toast_repo_scan_success,
            candidate.repository.owner,
            candidate.repository.repo,
        )
    }

    fun applyTrackSheet() {
        if (!state.trackDataReady || !state.showAddSheet || trackEditorSaveJob?.isActive == true) return
        val token = state.editorSessionToken
        val editing = state.editingTrackedItem
        val selectedFdroidCandidate = selectedFdroidCandidateForDraft()
        val editingFdroidConfig = editingFdroidConfigForDraft()
        val draft =
            GitHubTrackEditorDraft(
                sourceMode = state.trackSourceModeInput,
                repoUrl = state.repoUrlInput,
                packageName = state.packageNameInput,
                appLabelOverride = selectedFdroidCandidate?.appName.orEmpty(),
                preferPreRelease = state.preferPreReleaseInput,
                alwaysShowLatestReleaseDownloadButton =
                    when (state.trackSourceModeInput) {
                        GitHubTrackedSourceMode.GitHubRepository -> {
                            state.alwaysShowLatestReleaseDownloadButtonInput
                        }

                        GitHubTrackedSourceMode.GitRepository -> {
                            false
                        }

                        GitHubTrackedSourceMode.DirectApk -> {
                            false
                        }

                        GitHubTrackedSourceMode.FdroidRepository -> {
                            false
                        }
                    },
                checkActionsUpdates =
                    when (state.trackSourceModeInput) {
                        GitHubTrackedSourceMode.GitHubRepository -> state.checkActionsUpdatesInput
                        GitHubTrackedSourceMode.GitRepository -> false
                        GitHubTrackedSourceMode.DirectApk -> false
                        GitHubTrackedSourceMode.FdroidRepository -> false
                    },
                externalBuildUntilRelease =
                    state.trackSourceModeInput == GitHubTrackedSourceMode.GitHubRepository &&
                        state.externalBuildUntilReleaseInput,
                updateIntervalMode = state.updateIntervalModeInput,
                actionsUpdateIntervalMode =
                    when {
                        state.trackSourceModeInput != GitHubTrackedSourceMode.GitHubRepository -> {
                            GitHubTrackedActionsUpdateIntervalMode.FollowGlobal
                        }

                        state.checkActionsUpdatesInput -> {
                            state.actionsUpdateIntervalModeInput
                        }

                        else -> {
                            GitHubTrackedActionsUpdateIntervalMode.FollowGlobal
                        }
                    },
                preciseApkVersionMode = state.preciseApkVersionModeInput,
                ignoreMode = state.ignoreModeInput,
                ignoredStableReleaseKey = state.ignoredStableReleaseKeyInput,
                ignoredPreReleaseKey = state.ignoredPreReleaseKeyInput,
                fdroidConfig =
                    if (state.trackSourceModeInput == GitHubTrackedSourceMode.FdroidRepository) {
                        FdroidTrackedAppConfig(
                            selectionMode = state.fdroidVersionSelectionModeInput,
                            versionNameRegex =
                                if (state.fdroidVersionSelectionModeInput ==
                                    FdroidVersionSelectionMode.VersionNameRegex
                                ) {
                                    state.fdroidVersionNameRegexInput.trim()
                                } else {
                                    ""
                                },
                            apkNameRegex = state.fdroidApkNameRegexInput.trim(),
                            trustPolicy = state.fdroidTrustPolicyInput,
                            antiFeaturePolicy = state.fdroidAntiFeaturePolicyInput,
                            packagePageUrl =
                                selectedFdroidCandidate
                                    ?.packagePageUrl
                                    ?.takeIf { url -> url.isNotBlank() }
                                    ?: editingFdroidConfig?.packagePageUrl.orEmpty(),
                            repoPresetId =
                                selectedFdroidCandidate
                                    ?.repoPresetId
                                    ?.takeIf { id -> id.isNotBlank() }
                                    ?: if (state.fdroidRepoScopeIdInput == FdroidRepositoryPresets.COMMON_ID) {
                                        FdroidRepositoryPresets.presetForRepoUrl(state.repoUrlInput)?.id.orEmpty()
                                    } else {
                                        state.fdroidRepoScopeIdInput
                                    }
                        )
                    } else {
                        FdroidTrackedAppConfig()
                    },
                appList = state.appList,
            )
        trackEditorSaveJob = scope.launch {
            try {
                val newItem = when (val result = repository.buildTrackedItem(draft)) {
                    GitHubTrackEditorResult.InvalidRepository -> {
                        env.toast(R.string.github_toast_fill_repo_and_select_app)
                        return@launch
                    }
                    GitHubTrackEditorResult.InvalidPackageName -> {
                        env.toast(R.string.github_toast_invalid_package_name)
                        return@launch
                    }
                    is GitHubTrackEditorResult.Ready -> result.item
                }
                val editingState = editing?.let { state.checkStates[it.id] } ?: VersionCheckUi()
                val saved = persistTrackEditorSave(
                    state = state,
                    expectedToken = token,
                    editing = editing,
                    newItem = newItem,
                    nowMillis = env.clock.nowMs(),
                    persist = { plan ->
                        env.saveTrackedItemsAwaiting(
                            items = plan.items,
                            trackedFirstInstallAtByPackage = plan.firstInstallAt,
                            trackedAddedAtById = plan.addedAt,
                            trackedModifiedAtById = plan.modifiedAt,
                            refreshTrackIds = plan.refreshTrackIds,
                            semanticTrackUpdates = plan.semanticUpdates,
                        )
                    },
                )
                if (saved == TrackEditorSaveResult.Duplicate) {
                    env.toast(R.string.github_toast_track_exists)
                    return@launch
                }
                if (saved != TrackEditorSaveResult.Saved) return@launch
                if (editing != null) {
                    if (editing != newItem && !editing.hasSameGitHubTrackingConfigIgnoringLocalAppType(newItem)) {
                        assetActions.clearApkAssetStateAndCacheNow(item = editing, itemState = editingState)
                    }
                    if (editing.id != newItem.id) {
                        state.checkStates.remove(editing.id)
                        env.viewModel.removeTrackedExpansion(editing.id, removePersistedReleaseExpansion = true)
                        env.actionsRepository.removeRecommendedRunSnapshot(editing.id)
                        state.actionsRecommendedRunSnapshots.remove(editing.id)
                    }
                }
                if (!newItem.checkActionsUpdates) {
                    env.actionsRepository.removeRecommendedRunSnapshot(newItem.id)
                    state.actionsRecommendedRunSnapshots.remove(newItem.id)
                }
                state.requestTrackCardFocus(newItem.id)
                env.toast(if (editing == null) R.string.github_toast_track_added else R.string.github_toast_track_updated)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                env.toast(
                    R.string.common_save_failed_with_reason,
                    localizedGitHubPageErrorMessage(env.context, error),
                )
            }
        }
    }

    fun ignoreCurrentTrackedVersion(
        item: GitHubTrackedApp,
        itemState: VersionCheckUi,
    ) {
        val channel = itemState.currentReleaseIgnoreChannel()
        if (channel == null) {
            env.toast(R.string.github_toast_no_current_update_to_ignore)
            return
        }
        val releaseKey = itemState.releaseIgnoreKeyForChannel(channel)
        if (releaseKey.isBlank()) {
            env.toast(R.string.github_toast_no_current_update_to_ignore)
            return
        }
        val nextMode =
            when (channel) {
                GitHubReleaseIgnoreChannel.Stable -> GitHubTrackedIgnoreMode.CurrentStable
                GitHubReleaseIgnoreChannel.PreRelease -> GitHubTrackedIgnoreMode.CurrentPreRelease
            }
        val updatedItem = item.withReleaseIgnoreMode(
            mode = nextMode,
            stableReleaseKey =
                if (channel == GitHubReleaseIgnoreChannel.Stable) {
                    releaseKey
                } else {
                    ""
                },
            preReleaseKey =
                if (channel == GitHubReleaseIgnoreChannel.PreRelease) {
                    releaseKey
                } else {
                    ""
                },
        )
        val index = state.trackedItems.indexOfFirst { it.id == item.id }
        if (index < 0) return
        val nowMillis = env.clock.nowMs()
        state.trackedItems[index] = updatedItem
        state.checkStates[updatedItem.id] =
            itemState
                .withIgnoredReleaseChannel(channel)
                .copy(checkedAtMillis = nowMillis)
        state.recordTrackedModifiedAt(updatedItem.id, nowMillis)
        state.requestTrackCardFocus(updatedItem.id)
        env.saveTrackedItems()
        refreshActions.persistCheckCache()
        env.toast(R.string.github_toast_track_update_ignored)
    }

    fun confirmDeletePendingItem() {
        if (state.deleteInProgress) return
        state.pendingDeleteItem?.let { deleting ->
            state.deleteInProgress = true
            try {
                val deletingState = state.checkStates[deleting.id] ?: VersionCheckUi()
                refreshActions.cancelRefreshAll()
                assetActions.clearApkAssetStateAndCache(
                    item = deleting,
                    itemState = deletingState,
                )
                state.trackedItems.remove(deleting)
                state.checkStates.remove(deleting.id)
                env.viewModel.removeTrackedExpansion(
                    itemId = deleting.id,
                    removePersistedReleaseExpansion = true,
                )
                state.trackedAddedAtById.remove(deleting.id)
                state.trackedModifiedAtById.remove(deleting.id)
                env.actionsRepository.removeRecommendedRunSnapshot(deleting.id)
                state.actionsRecommendedRunSnapshots.remove(deleting.id)
                env.saveTrackedItems()
                refreshActions.persistCheckCache()
                env.toast(R.string.github_toast_track_deleted, deleting.appLabel)
            } finally {
                state.deleteInProgress = false
            }
        }
        state.pendingDeleteItem = null
    }

    private fun syncSelectedAppFromPackageInput() {
        val packageName = state.packageNameInput.trim()
        if (packageName.isBlank()) return
        val refreshedApp =
            state.appList.firstOrNull { app ->
                app.packageName.equals(packageName, ignoreCase = true)
            } ?: return
        state.selectedApp = refreshedApp
    }

    private fun selectedFdroidCandidateForDraft(): FdroidAppSearchCandidate? {
        if (state.trackSourceModeInput != GitHubTrackedSourceMode.FdroidRepository) return null
        return state.fdroidSelectedCandidate
            ?.takeIf { candidate ->
                candidate.repoUrl.trim().trimEnd('/')
                    .equals(state.repoUrlInput.trim().trimEnd('/'), ignoreCase = true) &&
                    candidate.packageName.equals(state.packageNameInput.trim(), ignoreCase = true)
            }
    }

    private fun editingFdroidConfigForDraft(): FdroidTrackedAppConfig? {
        if (state.trackSourceModeInput != GitHubTrackedSourceMode.FdroidRepository) return null
        val editingItem = state.editingTrackedItem
            ?.takeIf { item -> item.sourceMode == GitHubTrackedSourceMode.FdroidRepository }
            ?: return null
        val draftIdentity = buildFdroidRepositoryTrackIdentity(
            rawUrl = state.repoUrlInput,
            rawPackageName = state.packageNameInput,
        ) ?: return null
        val editingIdentity = buildFdroidRepositoryTrackIdentity(
            rawUrl = editingItem.repoUrl,
            rawPackageName = editingItem.packageName,
        ) ?: return null
        val sameRepo =
            draftIdentity.normalizedRepoUrl.equals(
                editingIdentity.normalizedRepoUrl,
                ignoreCase = true,
            )
        val samePackage =
            draftIdentity.packageName.equals(
                editingIdentity.packageName,
                ignoreCase = true,
            )
        return editingItem.fdroidConfig.takeIf { sameRepo && samePackage }
    }

    private fun currentEditingReleaseIgnoreKey(channel: GitHubReleaseIgnoreChannel): String {
        val editingItem = state.editingTrackedItem ?: return ""
        val itemState = state.checkStates[editingItem.id]
        val currentKey = itemState?.releaseIgnoreKeyForChannel(channel).orEmpty()
        if (currentKey.isNotBlank()) return currentKey
        return when (channel) {
            GitHubReleaseIgnoreChannel.Stable -> editingItem.ignoredStableReleaseKey
            GitHubReleaseIgnoreChannel.PreRelease -> editingItem.ignoredPreReleaseKey
        }
    }

}

private fun VersionCheckUi.currentReleaseIgnoreChannel(): GitHubReleaseIgnoreChannel? {
    return when {
        recommendsPreRelease &&
            releaseIgnoreKeyForChannel(GitHubReleaseIgnoreChannel.PreRelease).isNotBlank() -> {
            GitHubReleaseIgnoreChannel.PreRelease
        }

        hasUpdate == true &&
            releaseIgnoreKeyForChannel(GitHubReleaseIgnoreChannel.Stable).isNotBlank() -> {
            GitHubReleaseIgnoreChannel.Stable
        }

        hasPreReleaseUpdate &&
            releaseIgnoreKeyForChannel(GitHubReleaseIgnoreChannel.PreRelease).isNotBlank() -> {
            GitHubReleaseIgnoreChannel.PreRelease
        }

        else -> null
    }
}

private fun VersionCheckUi.releaseIgnoreKeyForChannel(
    channel: GitHubReleaseIgnoreChannel
): String {
    return when (channel) {
        GitHubReleaseIgnoreChannel.Stable -> buildGitHubReleaseIgnoreKey(
            displayVersion = latestStableName.ifBlank { latestTag.ifBlank { latestStableRawTag } },
            rawTag = latestStableRawTag.ifBlank { latestTag },
            rawName = latestStableName,
            link = latestStableUrl,
            preciseApkVersion = latestStableApkVersion
        )

        GitHubReleaseIgnoreChannel.PreRelease -> buildGitHubReleaseIgnoreKey(
            displayVersion = latestPreName.ifBlank { preReleaseInfo.ifBlank { latestPreRawTag } },
            rawTag = latestPreRawTag.ifBlank { preReleaseInfo },
            rawName = latestPreName,
            link = latestPreUrl,
            preciseApkVersion = latestPreApkVersion
        )
    }
}

private fun VersionCheckUi.withIgnoredReleaseChannel(
    channel: GitHubReleaseIgnoreChannel
): VersionCheckUi {
    val nextHasPreReleaseUpdate =
        hasPreReleaseUpdate && channel != GitHubReleaseIgnoreChannel.PreRelease
    val nextRecommendsPreRelease =
        recommendsPreRelease && channel != GitHubReleaseIgnoreChannel.PreRelease
    val stableUpdateVisible = hasUpdate == true && !recommendsPreRelease
    val nextStableHasUpdate =
        stableUpdateVisible && channel != GitHubReleaseIgnoreChannel.Stable
    val nextHasUpdate = nextStableHasUpdate || nextRecommendsPreRelease
    val nextStatus =
        when {
            nextRecommendsPreRelease -> GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable
            nextStableHasUpdate -> GitHubTrackedReleaseStatus.UpdateAvailable
            nextHasPreReleaseUpdate -> GitHubTrackedReleaseStatus.PreReleaseOptional
            else -> GitHubTrackedReleaseStatus.Ignored
        }
    return copy(
        hasUpdate = nextHasUpdate,
        hasPreReleaseUpdate = nextHasPreReleaseUpdate,
        recommendsPreRelease = nextRecommendsPreRelease,
        message = nextStatus.defaultMessage,
        failed = false,
    )
}
