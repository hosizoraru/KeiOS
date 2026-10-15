package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.domain.GitHubTrackedItemsTransferService
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubProfileDepth
import os.kei.feature.github.model.GitHubShareImportFlowMode
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.defaultRepositoryProfilePurpose
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.RefreshIntervalOption
import os.kei.ui.page.main.github.localizedGitHubPageErrorMessage
import os.kei.ui.page.main.github.page.GitHubTrackImportApplyResult
import os.kei.ui.page.main.github.page.GitHubTrackImportPreview
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import java.util.Locale

internal class GitHubConfigActions(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions,
    private val assetActions: GitHubAssetActions,
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state
    private val repository get() = env.repository

    fun openStrategySheet() {
        state.showApiTokenPlainText = false
        state.credentialCheckRunning = false
        state.credentialCheckError = null
        state.credentialCheckStatus = null
        state.strategyBenchmarkError = null
        state.strategyBenchmarkReport = null
        state.recommendedTokenGuideExpanded = false
        scope.launch {
            val config = repository.loadLookupConfig()
            state.lookupConfig = config
            state.selectedStrategyInput = config.selectedStrategy
            state.selectedActionsStrategyInput = config.actionsStrategy
            state.githubApiTokenInput = config.apiToken
            state.showStrategySheet = true
        }
    }

    fun openDroidSourcesSheet() {
        state.showActionMenuPopup = false
        scope.launch {
            val config = repository.loadLookupConfig()
            state.lookupConfig = config
            state.fdroidCommonRepoIdsInput = config.normalizedFdroidCommonRepoIds
            state.showDroidSourcesSheet = true
        }
    }

    fun closeDroidSourcesSheet() {
        state.showDroidSourcesSheet = false
    }

    fun closeStrategySheet() {
        state.dismissStrategySheet()
    }

    fun setSelectedStrategyInput(value: GitHubLookupStrategyOption) {
        state.selectedStrategyInput = value
    }

    fun setSelectedActionsStrategyInput(value: GitHubActionsLookupStrategyOption) {
        state.selectedActionsStrategyInput = value
    }

    fun setApiTokenInput(value: String) {
        state.githubApiTokenInput = value
        state.credentialCheckError = null
        state.credentialCheckStatus = null
    }

    fun toggleApiTokenVisibility() {
        state.showApiTokenPlainText = !state.showApiTokenPlainText
    }

    fun setRecommendedTokenGuideExpanded(value: Boolean) {
        state.recommendedTokenGuideExpanded = value
    }

    fun openCheckLogicSheet() {
        state.showDownloaderPopup = false
        state.showOnlineShareTargetPopup = false
        state.showShareImportFlowModePopup = false
        scope.launch {
            val config = repository.loadLookupConfig()
            state.lookupConfig = config
            state.checkAllTrackedPreReleasesInput = config.checkAllTrackedPreReleases
            state.checkAllDirectApkPreReleasesInput = config.checkAllDirectApkPreReleases
            state.aggressiveApkFilteringInput = config.aggressiveApkFiltering
            state.preciseApkVersionEnabledInput = config.preciseApkVersionEnabled
            state.scanSystemAppsByDefaultInput = config.scanSystemAppsByDefault
            state.profileDepthInput = config.profileDepth
            state.shareImportFlowModeInput = config.shareImportFlowMode
            state.appManagedShareInstallEnabledInput = config.appManagedShareInstallEnabled
            state.foregroundManagedDownloadBoostEnabledInput = config.foregroundManagedDownloadBoostEnabled
            state.onlineShareTargetPackageInput = config.onlineShareTargetPackage
            state.preferredDownloaderPackageInput = config.preferredDownloaderPackage
            state.decisionAssistEnabledInput = config.decisionAssistEnabled
            state.repositoryHealthCardEnabledInput = config.repositoryHealthCardEnabled
            state.apkTrustCheckEnabledInput = config.apkTrustCheckEnabled
            state.showCheckLogicSheet = true
        }
    }

    fun closeCheckLogicSheet() {
        state.dismissCheckLogicSheet()
    }

    fun setCheckAllTrackedPreReleasesInput(value: Boolean) {
        state.checkAllTrackedPreReleasesInput = value
    }

    fun setCheckAllDirectApkPreReleasesInput(value: Boolean) {
        state.checkAllDirectApkPreReleasesInput = value
    }

    fun setAggressiveApkFilteringInput(value: Boolean) {
        state.aggressiveApkFilteringInput = value
    }

    fun setPreciseApkVersionEnabledInput(value: Boolean) {
        state.preciseApkVersionEnabledInput = value
    }

    fun setScanSystemAppsByDefaultInput(value: Boolean) {
        state.scanSystemAppsByDefaultInput = value
    }

    fun setProfileDepthInput(value: GitHubProfileDepth) {
        state.profileDepthInput = value
    }

    fun setShareImportFlowModeInput(value: GitHubShareImportFlowMode) {
        state.shareImportFlowModeInput = value
    }

    fun setAppManagedShareInstallEnabledInput(value: Boolean) {
        state.appManagedShareInstallEnabledInput = value
    }

    fun setForegroundManagedDownloadBoostEnabledInput(value: Boolean) {
        state.foregroundManagedDownloadBoostEnabledInput = value
    }

    fun setPreferredDownloaderPackageInput(value: String) {
        state.preferredDownloaderPackageInput = value
    }

    fun setOnlineShareTargetPackageInput(value: String) {
        state.onlineShareTargetPackageInput = value
    }

    fun setDecisionAssistEnabledInput(value: Boolean) {
        state.decisionAssistEnabledInput = value
    }

    fun setRepositoryHealthCardEnabledInput(value: Boolean) {
        state.repositoryHealthCardEnabledInput = value
    }

    fun setApkTrustCheckEnabledInput(value: Boolean) {
        state.apkTrustCheckEnabledInput = value
    }

    fun setFdroidCommonRepoEnabled(
        repoId: String,
        enabled: Boolean
    ) {
        val normalizedRepoId = repoId.trim().lowercase(Locale.ROOT)
        val configurableIds = FdroidRepositoryPresets.commonSearchRepos.map { preset -> preset.id }.toSet()
        if (normalizedRepoId !in configurableIds) return
        val current =
            FdroidRepositoryPresets
                .normalizedCommonSearchRepoIds(state.fdroidCommonRepoIdsInput)
                .toMutableList()
        if (enabled) {
            if (normalizedRepoId !in current) {
                current += normalizedRepoId
            }
        } else {
            if (current.size <= 1 && normalizedRepoId in current) {
                env.toast(R.string.github_toast_fdroid_common_repo_requires_one)
                return
            }
            current.remove(normalizedRepoId)
        }
        state.fdroidCommonRepoIdsInput =
            FdroidRepositoryPresets.normalizedCommonSearchRepoIds(current)
    }

    fun setShowDownloaderPopup(value: Boolean) {
        state.showDownloaderPopup = value
    }

    fun setShowOnlineShareTargetPopup(value: Boolean) {
        state.showOnlineShareTargetPopup = value
    }

    fun setShowShareImportFlowModePopup(value: Boolean) {
        state.showShareImportFlowModePopup = value
    }

    fun selectRefreshIntervalHours(hours: Int) {
        val normalizedHours = RefreshIntervalOption.fromHours(hours).hours
        scope.launch {
            repository.saveRefreshIntervalHours(normalizedHours)
            state.refreshIntervalHours = normalizedHours
            repository.scheduleGitHubRefresh(context)
            env.toast(R.string.github_toast_refresh_interval_saved)
        }
    }

    suspend fun previewTrackedItemsImport(raw: String): GitHubTrackImportPreview {
        val payload = repository.parseTrackedItemsImport(raw)
        return repository.buildTrackedItemsImportPreview(
            payload = payload,
            existingItems = state.trackedItems.toList(),
        )
    }

    suspend fun applyTrackedItemsImport(preview: GitHubTrackImportPreview): GitHubTrackImportApplyResult =
        applyImportedTrackedItems(preview.payload)

    suspend fun importTrackedItemsJson(raw: String): GitHubTrackImportApplyResult = applyTrackedItemsImport(previewTrackedItemsImport(raw))

    fun applyLookupConfig() {
        scope.launch {
            val previousConfig = repository.loadLookupConfig()
            val sanitizedToken = state.githubApiTokenInput.trim()
            val newConfig =
                GitHubLookupConfig(
                    selectedStrategy = state.selectedStrategyInput,
                    actionsStrategy = state.selectedActionsStrategyInput,
                    apiToken = sanitizedToken,
                    checkAllTrackedPreReleases = previousConfig.checkAllTrackedPreReleases,
                    checkAllDirectApkPreReleases = previousConfig.checkAllDirectApkPreReleases,
                    aggressiveApkFiltering = previousConfig.aggressiveApkFiltering,
                    preciseApkVersionEnabled = previousConfig.preciseApkVersionEnabled,
                    scanSystemAppsByDefault = previousConfig.scanSystemAppsByDefault,
                    profileDepth = previousConfig.profileDepth,
                    shareImportFlowMode = previousConfig.shareImportFlowMode,
                    appManagedShareInstallEnabled = previousConfig.appManagedShareInstallEnabled,
                    foregroundManagedDownloadBoostEnabled =
                        previousConfig.foregroundManagedDownloadBoostEnabled,
                    onlineShareTargetPackage = previousConfig.onlineShareTargetPackage,
                    preferredDownloaderPackage = previousConfig.preferredDownloaderPackage,
                    decisionAssistEnabled = previousConfig.decisionAssistEnabled,
                    repositoryHealthCardEnabled = previousConfig.repositoryHealthCardEnabled,
                    apkTrustCheckEnabled = previousConfig.apkTrustCheckEnabled,
                    releaseNotesMode = previousConfig.releaseNotesMode,
                    fdroidCommonRepoIds = previousConfig.normalizedFdroidCommonRepoIds,
                )
            repository.saveLookupConfig(newConfig)
            state.lookupConfig = newConfig
            closeStrategySheet()

            val strategyChanged = previousConfig.selectedStrategy != newConfig.selectedStrategy
            val actionsStrategyChanged = previousConfig.actionsStrategy != newConfig.actionsStrategy
            val tokenChanged = previousConfig.apiToken != newConfig.apiToken
            val releaseTokenChanged =
                newConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken && tokenChanged
            val actionsTokenChanged = newConfig.actionsRequireApiToken && tokenChanged
            val releaseLookupChanged = strategyChanged || releaseTokenChanged
            val actionsLookupChanged = actionsStrategyChanged || actionsTokenChanged
            if (actionsLookupChanged) {
                state.resetActionsSheetState()
            }
            when {
                releaseLookupChanged -> {
                    repository.clearReleaseStrategyCaches()
                    repository.clearCheckCache()
                    state.checkStates.clear()
                    assetActions.clearAllApkAssetStateAndCacheNow()
                    state.assetSourceSignature = state.buildAssetSourceSignature(newConfig)
                    state.lastRefreshMs = 0L
                    state.refreshProgress = 0f
                    state.overviewRefreshState = OverviewRefreshState.Idle
                    if (state.trackedItems.isNotEmpty()) {
                        if (strategyChanged) {
                            env.toast(
                                R.string.github_toast_strategy_switched_recheck,
                                newConfig.selectedStrategy.label,
                            )
                        } else {
                            env.toast(R.string.github_toast_api_credential_saved_recheck)
                        }
                        refreshActions.refreshAllTracked(showToast = true)
                    } else {
                        if (strategyChanged) {
                            env.toast(
                                R.string.github_toast_strategy_switched,
                                newConfig.selectedStrategy.label,
                            )
                        } else {
                            env.toast(R.string.github_toast_api_credential_saved)
                        }
                    }
                }

                actionsLookupChanged -> {
                    if (actionsStrategyChanged) {
                        env.toast(
                            R.string.github_toast_actions_strategy_switched,
                            newConfig.actionsStrategy.label,
                        )
                    } else {
                        env.toast(R.string.github_toast_actions_api_credential_saved)
                    }
                }

                tokenChanged -> {
                    env.toast(R.string.github_toast_api_credential_saved)
                }

                else -> {
                    env.toast(R.string.github_toast_strategy_unchanged)
                }
            }
        }
    }

    fun applyCheckLogicSheet(installedOnlineShareTargets: List<OnlineShareTargetOption>) {
        scope.launch {
            val previousConfig = repository.loadLookupConfig()
            val newConfig =
                previousConfig.copy(
                    checkAllTrackedPreReleases = state.checkAllTrackedPreReleasesInput,
                    checkAllDirectApkPreReleases = state.checkAllDirectApkPreReleasesInput,
                    aggressiveApkFiltering = state.aggressiveApkFilteringInput,
                    preciseApkVersionEnabled = state.preciseApkVersionEnabledInput,
                    scanSystemAppsByDefault = state.scanSystemAppsByDefaultInput,
                    profileDepth = state.profileDepthInput,
                    shareImportFlowMode = state.shareImportFlowModeInput,
                    appManagedShareInstallEnabled = state.appManagedShareInstallEnabledInput,
                    foregroundManagedDownloadBoostEnabled =
                        state.foregroundManagedDownloadBoostEnabledInput,
                    onlineShareTargetPackage =
                        state.onlineShareTargetPackageInput
                            .trim()
                            .takeIf { selected ->
                                installedOnlineShareTargets.any { it.packageName == selected }
                            }.orEmpty(),
                    preferredDownloaderPackage = state.preferredDownloaderPackageInput.trim(),
                    decisionAssistEnabled = state.decisionAssistEnabledInput,
                    repositoryHealthCardEnabled = state.repositoryHealthCardEnabledInput,
                    apkTrustCheckEnabled = state.apkTrustCheckEnabledInput,
                )
            repository.saveLookupConfig(newConfig)
            state.lookupConfig = newConfig
            repository.scheduleGitHubRefresh(context)
            closeCheckLogicSheet()

            val checkScopeChanged =
                previousConfig.checkAllTrackedPreReleases !=
                    newConfig.checkAllTrackedPreReleases ||
                    previousConfig.checkAllDirectApkPreReleases !=
                    newConfig.checkAllDirectApkPreReleases
            val filteringChanged = previousConfig.aggressiveApkFiltering != newConfig.aggressiveApkFiltering
            val preciseVersionChanged =
                previousConfig.preciseApkVersionEnabled != newConfig.preciseApkVersionEnabled
            val scanSystemAppsChanged =
                previousConfig.scanSystemAppsByDefault != newConfig.scanSystemAppsByDefault
            val profileDepthChanged = previousConfig.profileDepth != newConfig.profileDepth
            val profilePurposeChanged =
                previousConfig.defaultRepositoryProfilePurpose() !=
                    newConfig.defaultRepositoryProfilePurpose()
            val shareImportChanged =
                previousConfig.shareImportFlowMode != newConfig.shareImportFlowMode ||
                    previousConfig.appManagedShareInstallEnabled !=
                    newConfig.appManagedShareInstallEnabled ||
                    previousConfig.foregroundManagedDownloadBoostEnabled !=
                    newConfig.foregroundManagedDownloadBoostEnabled
            val onlineShareTargetChanged =
                previousConfig.onlineShareTargetPackage != newConfig.onlineShareTargetPackage
            val downloaderChanged =
                previousConfig.preferredDownloaderPackage != newConfig.preferredDownloaderPackage
            val decisionAssistChanged =
                previousConfig.decisionAssistEnabled != newConfig.decisionAssistEnabled ||
                    previousConfig.repositoryHealthCardEnabled != newConfig.repositoryHealthCardEnabled ||
                    previousConfig.apkTrustCheckEnabled != newConfig.apkTrustCheckEnabled
            val preferenceChangedCount =
                listOf(
                    shareImportChanged,
                    onlineShareTargetChanged,
                    downloaderChanged,
                    decisionAssistChanged,
                ).count { it }
            when {
                checkScopeChanged ||
                    filteringChanged ||
                    preciseVersionChanged ||
                    scanSystemAppsChanged ||
                    profileDepthChanged ||
                    profilePurposeChanged -> {
                    repository.clearCheckCache()
                    state.checkStates.clear()
                    assetActions.clearAllApkAssetStateAndCacheNow()
                    state.assetSourceSignature = state.buildAssetSourceSignature(newConfig)
                    state.lastRefreshMs = 0L
                    state.refreshProgress = 0f
                    state.overviewRefreshState = OverviewRefreshState.Idle
                    if (scanSystemAppsChanged) {
                        refreshActions.reloadApps(forceRefresh = true)
                    }
                    if (state.trackedItems.isNotEmpty()) {
                        env.toast(
                            if (profileDepthChanged) {
                                R.string.github_toast_profile_depth_updated_recheck
                            } else {
                                R.string.github_toast_check_logic_updated_recheck
                            },
                        )
                        refreshActions.refreshAllTracked(showToast = true)
                    } else {
                        env.toast(R.string.github_toast_check_logic_saved)
                    }
                }

                preferenceChangedCount > 1 -> {
                    env.toast(R.string.github_toast_preferences_saved)
                }

                shareImportChanged -> {
                    env.toast(R.string.github_toast_share_import_setting_saved)
                }

                onlineShareTargetChanged -> {
                    env.toast(R.string.github_toast_online_share_target_saved)
                }

                downloaderChanged -> {
                    env.toast(R.string.github_toast_downloader_setting_saved)
                }

                decisionAssistChanged -> {
                    env.toast(R.string.github_toast_preferences_saved)
                }

                else -> {
                    env.toast(R.string.github_toast_check_logic_unchanged)
                }
            }
        }
    }

    fun applyDroidSourcesSheet() {
        scope.launch {
            val previousConfig = repository.loadLookupConfig()
            val normalizedIds =
                FdroidRepositoryPresets.normalizedCommonSearchRepoIds(state.fdroidCommonRepoIdsInput)
            val newConfig = previousConfig.copy(fdroidCommonRepoIds = normalizedIds)
            repository.saveLookupConfig(newConfig)
            state.lookupConfig = newConfig
            state.fdroidCommonRepoIdsInput = newConfig.normalizedFdroidCommonRepoIds
            if (previousConfig.normalizedFdroidCommonRepoIds != newConfig.normalizedFdroidCommonRepoIds &&
                state.trackSourceModeInput == GitHubTrackedSourceMode.FdroidRepository &&
                state.fdroidRepoScopeIdInput == FdroidRepositoryPresets.COMMON_ID
            ) {
                state.fdroidAppSearchCandidates = emptyList()
                state.fdroidSelectedCandidate = null
                state.fdroidAppSearchFailures = emptyList()
                state.fdroidAppSearchFailuresExpanded = false
                state.fdroidAppSearchRepoReports = emptyList()
            }
            closeDroidSourcesSheet()
            if (previousConfig.normalizedFdroidCommonRepoIds == newConfig.normalizedFdroidCommonRepoIds) {
                env.toast(R.string.github_toast_fdroid_sources_unchanged)
            } else {
                env.toast(R.string.github_toast_fdroid_sources_saved)
            }
        }
    }

    fun runStrategyBenchmark() {
        if (state.strategyBenchmarkRunning) return
        scope.launch {
            val targets = repository.buildStrategyBenchmarkTargets(state.trackedItems.toList())
            if (targets.isEmpty()) {
                env.toast(R.string.github_toast_require_track_item)
                return@launch
            }
            state.strategyBenchmarkRunning = true
            state.strategyBenchmarkError = null
            val benchmarkToken = state.githubApiTokenInput.trim()
            runCatching {
                repository.runStrategyBenchmark(
                    targets = targets,
                    apiToken = benchmarkToken,
                )
            }.onSuccess { report ->
                state.strategyBenchmarkReport = report
            }.onFailure { error ->
                state.strategyBenchmarkError =
                    localizedGitHubPageErrorMessage(
                        context = context,
                        error = error,
                    )
            }
            state.strategyBenchmarkRunning = false
        }
    }

    fun runCredentialCheck() {
        if (state.credentialCheckRunning) return
        scope.launch {
            state.credentialCheckRunning = true
            state.credentialCheckError = null
            state.credentialCheckStatus = null
            try {
                val token = state.githubApiTokenInput.trim()
                val trace = repository.checkCredential(token)
                state.credentialCheckStatus = trace.result.getOrNull()
                state.credentialCheckError =
                    trace.result.exceptionOrNull()?.let { error ->
                        localizedGitHubPageErrorMessage(
                            context = context,
                            error = error,
                        )
                    }
            } finally {
                state.credentialCheckRunning = false
            }
        }
    }

    fun handleInstalledOnlineShareTargetsChanged(installedOnlineShareTargets: List<OnlineShareTargetOption>) {
        if (state.onlineShareTargetPackageInput.isNotBlank() &&
            installedOnlineShareTargets.none { it.packageName == state.onlineShareTargetPackageInput }
        ) {
            state.onlineShareTargetPackageInput = ""
        }
        if (state.lookupConfig.onlineShareTargetPackage.isNotBlank() &&
            installedOnlineShareTargets.none { it.packageName == state.lookupConfig.onlineShareTargetPackage }
        ) {
            val updatedConfig = state.lookupConfig.copy(onlineShareTargetPackage = "")
            state.lookupConfig = updatedConfig
            scope.launch {
                repository.saveLookupConfig(updatedConfig)
            }
        }
    }

    private suspend fun applyImportedTrackedItems(payload: GitHubTrackedItemsImportPayload): GitHubTrackImportApplyResult {
        if (payload.items.isEmpty()) {
            return GitHubTrackImportApplyResult(
                addedCount = 0,
                updatedCount = 0,
                unchangedCount = 0,
                invalidCount = payload.invalidCount,
                duplicateCount = payload.duplicateCount,
            )
        }
        val result =
            GitHubTrackedItemsTransferService.applyImport(
                payload = payload,
                onRefreshNeeded = { repository.scheduleGitHubRefresh(context) },
                existingItems = state.trackedItems.toList(),
            )
        if (!result.hasChanges) {
            return GitHubTrackImportApplyResult(
                addedCount = 0,
                updatedCount = 0,
                unchangedCount = result.unchangedCount,
                invalidCount = payload.invalidCount,
                duplicateCount = payload.duplicateCount,
            )
        }
        val trackSnapshot = repository.loadTrackSnapshot()
        refreshActions.applyTrackSnapshot(trackSnapshot)
        result.touchedTrackIds.firstOrNull()?.let(state::requestTrackCardFocus)

        val itemsById = trackSnapshot.items.associateBy { it.id }
        val refreshItems = result.refreshTrackIds.mapNotNull(itemsById::get)
        val refreshCount = refreshItems.size
        if (refreshCount in 1..6) {
            refreshItems.forEach { item ->
                refreshActions.refreshItem(item = item, showToastOnError = false)
            }
        } else if (refreshCount > 6) {
            state.lastRefreshMs = 0L
            state.refreshProgress = 0f
            state.overviewRefreshState = OverviewRefreshState.Idle
            refreshActions.refreshAllTracked(showToast = false)
        }
        return GitHubTrackImportApplyResult(
            addedCount = result.addedCount,
            updatedCount = result.updatedCount,
            unchangedCount = result.unchangedCount,
            invalidCount = payload.invalidCount,
            duplicateCount = payload.duplicateCount,
        )
    }
}
