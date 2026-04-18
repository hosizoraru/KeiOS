package com.example.keios.ui.page.main

import com.example.keios.R
import com.example.keios.feature.github.data.local.GitHubReleaseAssetCacheStore
import com.example.keios.feature.github.data.local.GitHubTrackedItemsImportPayload
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubApiTokenReleaseStrategy
import com.example.keios.feature.github.data.remote.GitHubReleaseStrategyRegistry
import com.example.keios.feature.github.domain.GitHubStrategyBenchmarkService
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubStrategyLoadTrace
import com.example.keios.ui.page.main.github.query.OnlineShareTargetOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class GitHubTrackImportApplyResult(
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val invalidCount: Int,
    val duplicateCount: Int
)

internal class GitHubConfigActions(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state

    fun openStrategySheet() {
        val config = GitHubTrackStore.loadLookupConfig()
        state.lookupConfig = config
        state.selectedStrategyInput = config.selectedStrategy
        state.githubApiTokenInput = config.apiToken
        state.showApiTokenPlainText = false
        state.credentialCheckRunning = false
        state.credentialCheckError = null
        state.credentialCheckStatus = null
        state.strategyBenchmarkError = null
        state.strategyBenchmarkReport = null
        state.recommendedTokenGuideExpanded = false
        state.showStrategySheet = true
    }

    fun closeStrategySheet() {
        state.dismissStrategySheet()
    }

    fun openCheckLogicSheet() {
        val config = GitHubTrackStore.loadLookupConfig()
        state.lookupConfig = config
        state.checkAllTrackedPreReleasesInput = config.checkAllTrackedPreReleases
        state.aggressiveApkFilteringInput = config.aggressiveApkFiltering
        state.onlineShareTargetPackageInput = config.onlineShareTargetPackage
        state.preferredDownloaderPackageInput = config.preferredDownloaderPackage
        state.refreshIntervalHoursInput = GitHubTrackStore.loadRefreshIntervalHours()
        state.showCheckLogicIntervalPopup = false
        state.showDownloaderPopup = false
        state.showOnlineShareTargetPopup = false
        state.showCheckLogicSheet = true
    }

    fun closeCheckLogicSheet() {
        state.dismissCheckLogicSheet()
    }

    fun buildTrackedItemsExportJson(
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        return GitHubTrackStore.buildTrackedItemsExportJson(
            items = state.trackedItems.toList(),
            exportedAtMillis = exportedAtMillis
        )
    }

    fun importTrackedItemsJson(raw: String): GitHubTrackImportApplyResult {
        val payload = GitHubTrackStore.parseTrackedItemsImport(raw)
        return applyImportedTrackedItems(payload)
    }

    fun applyLookupConfig() {
        val previousConfig = GitHubTrackStore.loadLookupConfig()
        val sanitizedToken = state.githubApiTokenInput.trim()
        val newConfig = GitHubLookupConfig(
            selectedStrategy = state.selectedStrategyInput,
            apiToken = sanitizedToken,
            checkAllTrackedPreReleases = previousConfig.checkAllTrackedPreReleases,
            aggressiveApkFiltering = previousConfig.aggressiveApkFiltering,
            onlineShareTargetPackage = previousConfig.onlineShareTargetPackage
        )
        GitHubTrackStore.saveLookupConfig(newConfig)
        state.lookupConfig = newConfig
        closeStrategySheet()

        val strategyChanged = previousConfig.selectedStrategy != newConfig.selectedStrategy
        val activeTokenChanged = newConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken &&
            previousConfig.apiToken != newConfig.apiToken
        when {
            strategyChanged || activeTokenChanged -> {
                GitHubReleaseStrategyRegistry.clearAllCaches()
                GitHubTrackStore.clearCheckCache()
                scope.launch(Dispatchers.IO) {
                    GitHubReleaseAssetCacheStore.clearAll()
                }
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

    fun applyCheckLogicSheet(installedOnlineShareTargets: List<OnlineShareTargetOption>) {
        val previousConfig = GitHubTrackStore.loadLookupConfig()
        val previousRefreshIntervalHours = GitHubTrackStore.loadRefreshIntervalHours()
        val newConfig = previousConfig.copy(
            checkAllTrackedPreReleases = state.checkAllTrackedPreReleasesInput,
            aggressiveApkFiltering = state.aggressiveApkFilteringInput,
            onlineShareTargetPackage = state.onlineShareTargetPackageInput.trim().takeIf { selected ->
                installedOnlineShareTargets.any { it.packageName == selected }
            }.orEmpty(),
            preferredDownloaderPackage = state.preferredDownloaderPackageInput.trim()
        )
        GitHubTrackStore.saveLookupConfig(newConfig)
        GitHubTrackStore.saveRefreshIntervalHours(state.refreshIntervalHoursInput)
        state.lookupConfig = newConfig
        state.refreshIntervalHours = state.refreshIntervalHoursInput
        com.example.keios.core.background.AppBackgroundScheduler.scheduleGitHubRefresh(context)
        closeCheckLogicSheet()

        val checkScopeChanged =
            previousConfig.checkAllTrackedPreReleases != newConfig.checkAllTrackedPreReleases
        val filteringChanged = previousConfig.aggressiveApkFiltering != newConfig.aggressiveApkFiltering
        val intervalChanged = previousRefreshIntervalHours != state.refreshIntervalHoursInput
        when {
            checkScopeChanged || filteringChanged -> {
                GitHubTrackStore.clearCheckCache()
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
            intervalChanged -> {
                env.toast(R.string.github_toast_refresh_interval_saved)
            }
            else -> {
                env.toast(R.string.github_toast_check_logic_unchanged)
            }
        }
    }

    fun runStrategyBenchmark() {
        if (state.strategyBenchmarkRunning) return
        val targets = GitHubStrategyBenchmarkService.buildTargets(state.trackedItems.toList())
        if (targets.isEmpty()) {
            env.toast(R.string.github_toast_require_track_item)
            return
        }
        scope.launch {
            state.strategyBenchmarkRunning = true
            state.strategyBenchmarkError = null
            val benchmarkToken = state.githubApiTokenInput.trim()
            runCatching {
                withContext(Dispatchers.IO) {
                    GitHubStrategyBenchmarkService.compareTargets(
                        targets = targets,
                        apiToken = benchmarkToken
                    )
                }
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
                val trace: GitHubStrategyLoadTrace<GitHubApiCredentialStatus> =
                    withContext(Dispatchers.IO) {
                        GitHubApiTokenReleaseStrategy(token).checkCredentialTrace()
                    }
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
            GitHubTrackStore.saveLookupConfig(updatedConfig)
            state.lookupConfig = updatedConfig
        }
    }

    private fun applyImportedTrackedItems(
        payload: GitHubTrackedItemsImportPayload
    ): GitHubTrackImportApplyResult {
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
        val touchedItems = mutableListOf<com.example.keios.feature.github.model.GitHubTrackedApp>()
        var addedCount = 0
        var updatedCount = 0
        var unchangedCount = 0
        payload.items.forEach { item ->
            val existingIndex = indexById[item.id]
            when {
                existingIndex == null -> {
                    mergedItems += item
                    indexById[item.id] = mergedItems.lastIndex
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
