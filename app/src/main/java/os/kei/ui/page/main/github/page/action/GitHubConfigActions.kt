package os.kei.ui.page.main.github.page.action

import os.kei.R
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.page.GitHubTrackImportApplyResult
import os.kei.ui.page.main.github.page.GitHubTrackImportPreview
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.launch

internal class GitHubConfigActions(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions
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
            state.githubApiTokenInput = config.apiToken
            state.showStrategySheet = true
        }
    }

    fun closeStrategySheet() {
        state.dismissStrategySheet()
    }

    fun openCheckLogicSheet() {
        state.showCheckLogicIntervalPopup = false
        state.showDownloaderPopup = false
        state.showOnlineShareTargetPopup = false
        scope.launch {
            val config = repository.loadLookupConfig()
            state.lookupConfig = config
            state.checkAllTrackedPreReleasesInput = config.checkAllTrackedPreReleases
            state.aggressiveApkFilteringInput = config.aggressiveApkFiltering
            state.shareImportLinkageEnabledInput = config.shareImportLinkageEnabled
            state.onlineShareTargetPackageInput = config.onlineShareTargetPackage
            state.preferredDownloaderPackageInput = config.preferredDownloaderPackage
            state.refreshIntervalHoursInput = repository.loadRefreshIntervalHours()
            state.showCheckLogicSheet = true
        }
    }

    fun closeCheckLogicSheet() {
        state.dismissCheckLogicSheet()
    }

    suspend fun previewTrackedItemsImport(raw: String): GitHubTrackImportPreview {
        val payload = repository.parseTrackedItemsImport(raw)
        return repository.buildTrackedItemsImportPreview(
            payload = payload,
            existingItems = state.trackedItems.toList()
        )
    }

    fun applyTrackedItemsImport(preview: GitHubTrackImportPreview): GitHubTrackImportApplyResult {
        return applyImportedTrackedItems(preview.payload)
    }

    suspend fun importTrackedItemsJson(raw: String): GitHubTrackImportApplyResult {
        return applyTrackedItemsImport(previewTrackedItemsImport(raw))
    }

    fun applyLookupConfig() {
        scope.launch {
            val previousConfig = repository.loadLookupConfig()
            val sanitizedToken = state.githubApiTokenInput.trim()
            val newConfig = GitHubLookupConfig(
                selectedStrategy = state.selectedStrategyInput,
                apiToken = sanitizedToken,
                checkAllTrackedPreReleases = previousConfig.checkAllTrackedPreReleases,
                aggressiveApkFiltering = previousConfig.aggressiveApkFiltering,
                shareImportLinkageEnabled = previousConfig.shareImportLinkageEnabled,
                onlineShareTargetPackage = previousConfig.onlineShareTargetPackage,
                preferredDownloaderPackage = previousConfig.preferredDownloaderPackage
            )
            repository.saveLookupConfig(newConfig)
            state.lookupConfig = newConfig
            closeStrategySheet()

            val strategyChanged = previousConfig.selectedStrategy != newConfig.selectedStrategy
            val activeTokenChanged = newConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken &&
                previousConfig.apiToken != newConfig.apiToken
            when {
                strategyChanged || activeTokenChanged -> {
                    repository.clearReleaseStrategyCaches()
                    repository.clearCheckCache()
                    repository.clearAllAssetCache()
                    state.checkStates.clear()
                    state.clearAllAssetUiState()
                    state.assetSourceSignature = state.buildAssetSourceSignature(newConfig)
                    state.lastRefreshMs = 0L
                    state.refreshProgress = 0f
                    state.overviewRefreshState = OverviewRefreshState.Idle
                    if (state.trackedItems.isNotEmpty()) {
                        env.toast(
                            R.string.github_toast_strategy_switched_recheck,
                            newConfig.selectedStrategy.label
                        )
                        refreshActions.refreshAllTracked(showToast = true)
                    } else {
                        env.toast(
                            R.string.github_toast_strategy_switched,
                            newConfig.selectedStrategy.label
                        )
                    }
                }
                previousConfig.apiToken != newConfig.apiToken -> {
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
            val previousRefreshIntervalHours = repository.loadRefreshIntervalHours()
            val newConfig = previousConfig.copy(
                checkAllTrackedPreReleases = state.checkAllTrackedPreReleasesInput,
                aggressiveApkFiltering = state.aggressiveApkFilteringInput,
                shareImportLinkageEnabled = state.shareImportLinkageEnabledInput,
                onlineShareTargetPackage = state.onlineShareTargetPackageInput.trim().takeIf { selected ->
                    installedOnlineShareTargets.any { it.packageName == selected }
                }.orEmpty(),
                preferredDownloaderPackage = state.preferredDownloaderPackageInput.trim()
            )
            repository.saveLookupConfig(newConfig)
            repository.saveRefreshIntervalHours(state.refreshIntervalHoursInput)
            state.lookupConfig = newConfig
            state.refreshIntervalHours = state.refreshIntervalHoursInput
            repository.scheduleGitHubRefresh(context)
            closeCheckLogicSheet()

            val checkScopeChanged =
                previousConfig.checkAllTrackedPreReleases != newConfig.checkAllTrackedPreReleases
            val filteringChanged = previousConfig.aggressiveApkFiltering != newConfig.aggressiveApkFiltering
            val shareImportChanged =
                previousConfig.shareImportLinkageEnabled != newConfig.shareImportLinkageEnabled
            val intervalChanged = previousRefreshIntervalHours != state.refreshIntervalHoursInput
            when {
                checkScopeChanged || filteringChanged -> {
                    repository.clearCheckCache()
                    state.checkStates.clear()
                    state.clearAllAssetUiState()
                    state.lastRefreshMs = 0L
                    state.refreshProgress = 0f
                    state.overviewRefreshState = OverviewRefreshState.Idle
                    if (state.trackedItems.isNotEmpty()) {
                        env.toast(R.string.github_toast_check_logic_updated_recheck)
                        refreshActions.refreshAllTracked(showToast = true)
                    } else {
                        env.toast(R.string.github_toast_check_logic_saved)
                    }
                }
                shareImportChanged -> {
                    env.toast(R.string.github_toast_check_logic_saved)
                }
                intervalChanged -> {
                    env.toast(R.string.github_toast_refresh_interval_saved)
                }
                else -> {
                    env.toast(R.string.github_toast_check_logic_unchanged)
                }
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
                    apiToken = benchmarkToken
                )
            }.onSuccess { report ->
                state.strategyBenchmarkReport = report
            }.onFailure { error ->
                state.strategyBenchmarkError = error.message ?: "unknown"
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
                state.credentialCheckError = trace.result.exceptionOrNull()?.message
            } finally {
                state.credentialCheckRunning = false
            }
        }
    }

    fun handleInstalledOnlineShareTargetsChanged(
        installedOnlineShareTargets: List<OnlineShareTargetOption>
    ) {
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

    private fun applyImportedTrackedItems(
        payload: GitHubTrackedItemsImportPayload
    ): GitHubTrackImportApplyResult {
        val nowMillis = System.currentTimeMillis()
        if (payload.items.isEmpty()) {
            return GitHubTrackImportApplyResult(
                addedCount = 0,
                updatedCount = 0,
                unchangedCount = 0,
                invalidCount = payload.invalidCount,
                duplicateCount = payload.duplicateCount
            )
        }
        val mergedItems = state.trackedItems.toMutableList()
        val indexById = mergedItems.withIndex()
            .associate { it.value.id to it.index }
            .toMutableMap()
        val touchedItems = mutableListOf<GitHubTrackedApp>()
        var addedCount = 0
        var updatedCount = 0
        var unchangedCount = 0
        payload.items.forEach { item ->
            val existingIndex = indexById[item.id]
            when {
                existingIndex == null -> {
                    mergedItems += item
                    indexById[item.id] = mergedItems.lastIndex
                    state.recordTrackedAddedAt(item.id, nowMillis)
                    touchedItems += item
                    addedCount += 1
                }

                mergedItems[existingIndex] != item -> {
                    mergedItems[existingIndex] = item
                    state.checkStates.remove(item.id)
                    state.clearAssetUiState(item.id)
                    touchedItems += item
                    updatedCount += 1
                }

                else -> {
                    unchangedCount += 1
                }
            }
        }
        if (addedCount == 0 && updatedCount == 0) {
            return GitHubTrackImportApplyResult(
                addedCount = 0,
                updatedCount = 0,
                unchangedCount = unchangedCount,
                invalidCount = payload.invalidCount,
                duplicateCount = payload.duplicateCount
            )
        }
        state.trackedItems.clear()
        state.trackedItems.addAll(mergedItems)
        env.saveTrackedItems()
        refreshActions.persistCheckCache()

        val touchedCount = touchedItems.size
        if (touchedCount in 1..6) {
            touchedItems.forEach { item ->
                refreshActions.refreshItem(item = item, showToastOnError = false)
            }
        } else {
            state.lastRefreshMs = 0L
            state.refreshProgress = 0f
            state.overviewRefreshState = OverviewRefreshState.Idle
            refreshActions.refreshAllTracked(showToast = false)
        }
        return GitHubTrackImportApplyResult(
            addedCount = addedCount,
            updatedCount = updatedCount,
            unchangedCount = unchangedCount,
            invalidCount = payload.invalidCount,
            duplicateCount = payload.duplicateCount
        )
    }

}
