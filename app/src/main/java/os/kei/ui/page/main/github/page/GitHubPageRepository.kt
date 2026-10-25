package os.kei.ui.page.main.github.page

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import os.kei.core.privilege.PrivilegedShell
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.data.local.GitHubAppPickerPreferences
import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.GitHubReleaseNotesTarget
import os.kei.feature.github.data.remote.GitHubReleasePage
import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecar
import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecarStore
import os.kei.feature.github.model.FdroidAppSearchRequest
import os.kei.feature.github.model.FdroidAppSearchResult
import os.kei.feature.github.model.GitHubApiCredentialStatus
import os.kei.feature.github.model.GitHubApkPackageNameScanRequest
import os.kei.feature.github.model.GitHubApkPackageNameScanResult
import os.kei.feature.github.model.GitHubAppRepositorySearchRequest
import os.kei.feature.github.model.GitHubAppRepositorySearchResult
import os.kei.feature.github.model.GitHubCheckCacheEntry
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubPackageRepositoryScanRequest
import os.kei.feature.github.model.GitHubPackageRepositoryScanResult
import os.kei.feature.github.model.GitHubRepoTarget
import os.kei.feature.github.model.GitHubRepositoryProfilePurpose
import os.kei.feature.github.model.GitHubRepositoryProfileSnapshot
import os.kei.feature.github.model.GitHubStarredRepositoryImportPreview
import os.kei.feature.github.model.GitHubStarredRepositoryImportRequest
import os.kei.feature.github.model.GitHubStrategyBenchmarkReport
import os.kei.feature.github.model.GitHubStrategyLoadTrace
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedActionsUpdateIntervalMode
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedPreciseApkVersionMode
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode
import os.kei.feature.github.model.GitRepositoryTrackIdentity
import os.kei.feature.github.model.InstalledAppItem
import os.kei.feature.github.domain.GitHubReleaseAssetService
import os.kei.feature.github.domain.GitHubHistoryUnreadService
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.domain.GitHubTrackChangeSemanticUpdate
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchEvaluator
import os.kei.ui.page.main.github.GitHubTrackedFilterMode
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.actions.GitHubActionsSectionExpansionState
import os.kei.ui.page.main.github.actions.GitHubActionsSheetInput
import os.kei.ui.page.main.github.actions.GitHubActionsSheetUiState
import os.kei.ui.page.main.github.actions.GitHubActionsUiStateStore
import os.kei.ui.page.main.github.actions.deriveGitHubActionsSheetState
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerDerivedState
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerInput
import os.kei.ui.page.main.github.picker.filterAndSortGitHubTrackAppCandidates
import os.kei.ui.page.main.github.picker.gitHubTrackAppPickerIconPreloadPackages
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.section.GitHubTrackedReleaseExpansionState
import os.kei.ui.page.main.github.section.GitHubTrackedReleaseUiStateStore
import os.kei.ui.page.main.github.share.GitHubPendingShareImportAttachCandidate
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack
import os.kei.ui.page.main.github.share.GitHubShareImportPreview
import os.kei.ui.page.main.github.share.GitHubShareImportResult
import os.kei.ui.page.main.github.sheet.GitHubApkInfoSheetInput
import os.kei.ui.page.main.github.sheet.GitHubApkInfoSheetUiState
import os.kei.ui.page.main.github.sheet.GitHubManagedInstallConfirmSheetInput
import os.kei.ui.page.main.github.sheet.GitHubManagedInstallConfirmSheetUiState
import os.kei.ui.page.main.github.sheet.GitHubReleaseNotesDetailInput
import os.kei.ui.page.main.github.sheet.GitHubReleaseNotesDetailUiState
import os.kei.ui.page.main.github.sheet.deriveGitHubApkInfoSheetState
import os.kei.ui.page.main.github.sheet.deriveGitHubManagedInstallConfirmSheetState
import os.kei.ui.page.main.github.sheet.deriveGitHubReleaseNotesDetailState

@Immutable
internal data class GitHubTrackEditorDraft(
    val sourceMode: GitHubTrackedSourceMode,
    val repoUrl: String,
    val packageName: String,
    val appLabelOverride: String = "",
    val preferPreRelease: Boolean,
    val alwaysShowLatestReleaseDownloadButton: Boolean,
    val checkActionsUpdates: Boolean,
    val externalBuildUntilRelease: Boolean,
    val updateIntervalMode: GitHubTrackedUpdateIntervalMode,
    val actionsUpdateIntervalMode: GitHubTrackedActionsUpdateIntervalMode,
    val preciseApkVersionMode: GitHubTrackedPreciseApkVersionMode,
    val ignoreMode: GitHubTrackedIgnoreMode = GitHubTrackedIgnoreMode.None,
    val ignoredStableReleaseKey: String = "",
    val ignoredPreReleaseKey: String = "",
    val fdroidConfig: FdroidTrackedAppConfig = FdroidTrackedAppConfig(),
    val appList: List<InstalledAppItem>,
)

internal sealed interface GitHubTrackEditorResult {
    data class Ready(
        val item: GitHubTrackedApp,
    ) : GitHubTrackEditorResult

    data object InvalidRepository : GitHubTrackEditorResult

    data object InvalidPackageName : GitHubTrackEditorResult
}

@Immutable
internal data class GitHubOnlineShareTargetInput(
    val shouldResolve: Boolean,
    val appList: List<InstalledAppItem>,
)

@Immutable
internal data class GitHubActiveShareImportFlow(
    val preview: GitHubShareImportPreview?,
    val pendingTrack: GitHubPendingShareImportTrack?,
    val attachCandidate: GitHubPendingShareImportAttachCandidate?,
    val result: GitHubShareImportResult?,
)

@Immutable
internal data class GitHubPagePersistedUiState(
    val pageUiState: GitHubPageUiState = GitHubPageUiState(),
    val actionsSectionExpansionState: GitHubActionsSectionExpansionState = GitHubActionsSectionExpansionState(),
    val trackedReleaseExpansionState: GitHubTrackedReleaseExpansionState = GitHubTrackedReleaseExpansionState(),
)

internal class GitHubPageRepository(
    ioDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork,
    private val localDispatcher: CoroutineDispatcher = AppDispatchers.githubLocal,
    private val defaultDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
) {
    private val contentStateDeriver = GitHubPageContentStateDeriver(defaultDispatcher)
    private val trackRepository = GitHubPageTrackRepository(localDispatcher)
    private val installedAppRepository =
        GitHubPageInstalledAppRepository(
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
        )
    private val refreshRepository = GitHubPageRefreshRepository(ioDispatcher)
    private val transferRepository =
        GitHubPageTransferRepository(
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
        )
    private val discoveryRepository =
        GitHubPageDiscoveryRepository(
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
        )
    private val assetBridge =
        GitHubReleaseAssetService(
            networkDispatcher = ioDispatcher,
            localDispatcher = localDispatcher,
        )
    private val historyUnreadService = GitHubHistoryUnreadService(localDispatcher)

    suspend fun buildContentState(input: GitHubPageContentInput): GitHubPageContentDerivedState = contentStateDeriver.build(input)

    suspend fun buildApkInfoSheetState(input: GitHubApkInfoSheetInput): GitHubApkInfoSheetUiState =
        withContext(defaultDispatcher) {
            deriveGitHubApkInfoSheetState(input)
        }

    suspend fun buildReleaseNotesDetailState(input: GitHubReleaseNotesDetailInput): GitHubReleaseNotesDetailUiState =
        withContext(defaultDispatcher) {
            deriveGitHubReleaseNotesDetailState(input)
        }

    suspend fun buildActionsSheetState(input: GitHubActionsSheetInput): GitHubActionsSheetUiState =
        withContext(defaultDispatcher) {
            deriveGitHubActionsSheetState(input)
        }

    suspend fun buildManagedInstallConfirmSheetState(
        input: GitHubManagedInstallConfirmSheetInput,
    ): GitHubManagedInstallConfirmSheetUiState =
        withContext(defaultDispatcher) {
            deriveGitHubManagedInstallConfirmSheetState(input)
        }

    suspend fun buildAppPickerState(input: GitHubTrackAppPickerInput): GitHubTrackAppPickerDerivedState =
        withContext(defaultDispatcher) {
            val filteredApps =
                filterAndSortGitHubTrackAppCandidates(
                    apps = input.appList,
                    query = input.query,
                    includeUserApps = input.includeUserApps,
                    includeSystemApps = input.includeSystemApps,
                    includeTrackedApps = input.includeTrackedApps,
                    includeDisabledOrFrozenApps = input.includeDisabledOrFrozenApps,
                    trackedPackageNames = input.trackedPackageNames,
                    pinnedPackageNames = input.pinnedPackageNames,
                    sortMode = input.sortMode,
                    sortDirection = input.sortDirection,
                )
            GitHubTrackAppPickerDerivedState(
                filteredApps = filteredApps,
                filteredIconPreloadPackages = gitHubTrackAppPickerIconPreloadPackages(filteredApps),
                deriving = false,
                input = input,
            )
        }

    suspend fun loadPersistedUiState(): GitHubPagePersistedUiState =
        withContext(localDispatcher) {
            GitHubPagePersistedUiState(
                pageUiState = GitHubPageUiStateStore.load(),
                actionsSectionExpansionState = GitHubActionsUiStateStore.loadSectionExpansionState(),
                trackedReleaseExpansionState = GitHubTrackedReleaseUiStateStore.load(),
            )
        }

    suspend fun saveTrackedFilterMode(value: GitHubTrackedFilterMode) {
        withContext(localDispatcher) {
            GitHubPageUiStateStore.setTrackedFilterMode(value)
        }
    }

    suspend fun saveTrackedStableVersionExpanded(
        itemId: String,
        value: Boolean,
    ) {
        withContext(localDispatcher) {
            GitHubTrackedReleaseUiStateStore.setStableVersionExpanded(itemId, value)
        }
    }

    suspend fun saveTrackedLocalVersionExpanded(
        itemId: String,
        value: Boolean,
    ) {
        withContext(localDispatcher) {
            GitHubTrackedReleaseUiStateStore.setLocalVersionExpanded(itemId, value)
        }
    }

    suspend fun saveTrackedPreReleaseVersionExpanded(
        itemId: String,
        value: Boolean,
    ) {
        withContext(localDispatcher) {
            GitHubTrackedReleaseUiStateStore.setPreReleaseVersionExpanded(itemId, value)
        }
    }

    suspend fun removeTrackedReleaseExpansion(itemId: String) {
        withContext(localDispatcher) {
            GitHubTrackedReleaseUiStateStore.remove(itemId)
        }
    }

    suspend fun retainTrackedReleaseExpansion(validItemIds: Set<String>) {
        withContext(localDispatcher) {
            GitHubTrackedReleaseUiStateStore.retain(validItemIds)
        }
    }

    suspend fun queryOnlineShareTargets(
        context: Context,
        input: GitHubOnlineShareTargetInput,
    ): List<OnlineShareTargetOption> = installedAppRepository.queryOnlineShareTargets(context, input)

    suspend fun queryDownloaders(context: Context): List<DownloaderOption> = installedAppRepository.queryDownloaders(context)

    suspend fun loadTrackSnapshot(): GitHubTrackSnapshot = trackRepository.loadTrackSnapshot()

    suspend fun loadLookupConfig(): GitHubLookupConfig = trackRepository.loadLookupConfig()

    suspend fun saveLookupConfig(config: GitHubLookupConfig) = trackRepository.saveLookupConfig(config)

    suspend fun loadRefreshIntervalHours(): Int = trackRepository.loadRefreshIntervalHours()

    suspend fun loadFdroidMetadataSidecar(trackId: String): FdroidMetadataSidecar? =
        withContext(localDispatcher) {
            FdroidMetadataSidecarStore.load(trackId)
        }

    suspend fun saveRefreshIntervalHours(hours: Int) = trackRepository.saveRefreshIntervalHours(hours)

    suspend fun loadAppPickerPreferences(): GitHubAppPickerPreferences = trackRepository.loadAppPickerPreferences()

    suspend fun saveAppPickerPreferences(preferences: GitHubAppPickerPreferences) = trackRepository.saveAppPickerPreferences(preferences)

    suspend fun loadHistoryUnreadCount(): Int = historyUnreadService.loadCounts().totalCount

    fun historyUnreadSignalVersions(): StateFlow<Long> = historyUnreadService.signalVersions()

    suspend fun saveTrackedItems(
        context: Context,
        items: List<GitHubTrackedApp>,
        trackedFirstInstallAtByPackage: Map<String, Long>,
        trackedAddedAtById: Map<String, Long>,
        trackedModifiedAtById: Map<String, Long>,
        refreshTrackIds: Set<String> = emptySet(),
        emitStoreSignal: Boolean = true,
        trackChangeSource: GitHubTrackChangeHistorySource = GitHubTrackChangeHistorySource.Page,
        semanticTrackUpdates: List<GitHubTrackChangeSemanticUpdate> = emptyList(),
    ) = trackRepository.saveTrackedItems(
        context = context,
        items = items,
        trackedFirstInstallAtByPackage = trackedFirstInstallAtByPackage,
        trackedAddedAtById = trackedAddedAtById,
        trackedModifiedAtById = trackedModifiedAtById,
        refreshTrackIds = refreshTrackIds,
        emitStoreSignal = emitStoreSignal,
        trackChangeSource = trackChangeSource,
        semanticTrackUpdates = semanticTrackUpdates,
    )

    suspend fun saveCheckCache(
        states: Map<String, GitHubCheckCacheEntry>,
        refreshTimestamp: Long,
    ): Long = trackRepository.saveCheckCache(states, refreshTimestamp)

    suspend fun mergeCheckCache(
        entries: Map<String, GitHubCheckCacheEntry>,
        refreshTimestamp: Long,
    ): Long = trackRepository.mergeCheckCache(entries, refreshTimestamp)

    suspend fun clearCheckCache() = trackRepository.clearCheckCache()

    suspend fun clearPendingShareImportTrack() = trackRepository.clearPendingShareImportTrack()

    suspend fun clearActiveShareImportFlow() = trackRepository.clearActiveShareImportFlow()

    suspend fun clearShareImportResult() = trackRepository.clearShareImportResult()

    suspend fun saveShareImportResult(result: GitHubShareImportResult) = trackRepository.saveShareImportResult(result)

    suspend fun loadActiveShareImportFlow(): GitHubActiveShareImportFlow = trackRepository.loadActiveShareImportFlow()

    fun scheduleGitHubRefresh(context: Context) = trackRepository.scheduleGitHubRefresh(context)

    fun currentTrackStoreSignalVersion(): Long = trackRepository.currentTrackStoreSignalVersion()

    fun trackStoreSignalVersions(): StateFlow<Long> = trackRepository.trackStoreSignalVersions()

    fun buildAppListPermissionIntent(context: Context): Intent? = installedAppRepository.buildAppListPermissionIntent(context)

    suspend fun consumeTrackRefreshRequests(validTrackIds: Set<String>): Set<String> =
        trackRepository.consumeTrackRefreshRequests(validTrackIds)

    suspend fun queryInstalledLaunchableApps(
        context: Context,
        privilegedShell: PrivilegedShell,
        forceRefresh: Boolean,
        includeSystemApps: Boolean = true,
        pinnedSystemPackageNames: Set<String> = emptySet(),
        requiredPackageNames: Set<String> = emptySet(),
    ): List<InstalledAppItem> =
        installedAppRepository.queryInstalledLaunchableApps(
            context = context,
            privilegedShell = privilegedShell,
            forceRefresh = forceRefresh,
            includeSystemApps = includeSystemApps,
            pinnedSystemPackageNames = pinnedSystemPackageNames,
            requiredPackageNames = requiredPackageNames,
        )

    suspend fun preloadAppIcons(
        context: Context,
        packageNames: List<String>,
    ) = installedAppRepository.preloadAppIcons(context, packageNames)

    suspend fun localVersionInfoOrNull(
        context: Context,
        packageName: String,
    ) = installedAppRepository.localVersionInfoOrNull(context, packageName)

    suspend fun evaluateTrackedApp(
        context: Context,
        item: GitHubTrackedApp,
        profilePurposeOverride: GitHubRepositoryProfilePurpose? = null,
        forceRefresh: Boolean = false,
        existingRepositoryProfile: GitHubRepositoryProfileSnapshot? = null,
        batchEvaluator: GitHubTrackedRefreshBatchEvaluator? = null,
    ): VersionCheckUi =
        refreshRepository.evaluateTrackedApp(
            context = context,
            item = item,
            profilePurposeOverride = profilePurposeOverride,
            forceRefresh = forceRefresh,
            existingRepositoryProfile = existingRepositoryProfile,
            batchEvaluator = batchEvaluator,
        )

    suspend fun notifyRefreshProgress(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long,
        scope: GitHubRefreshScope,
        source: GitHubRefreshSource,
        totalTrackedCount: Int,
    ) = refreshRepository.notifyRefreshProgress(
        context = context,
        current = current,
        total = total,
        preReleaseUpdateCount = preReleaseUpdateCount,
        updatableCount = updatableCount,
        failedCount = failedCount,
        sessionId = sessionId,
        scope = scope,
        source = source,
        totalTrackedCount = totalTrackedCount,
    )

    suspend fun notifyRefreshCompleted(
        context: Context,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long,
        scope: GitHubRefreshScope,
        source: GitHubRefreshSource,
        totalTrackedCount: Int,
    ) = refreshRepository.notifyRefreshCompleted(
        context = context,
        total = total,
        preReleaseUpdateCount = preReleaseUpdateCount,
        updatableCount = updatableCount,
        failedCount = failedCount,
        sessionId = sessionId,
        scope = scope,
        source = source,
        totalTrackedCount = totalTrackedCount,
    )

    suspend fun notifyRefreshFailed(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long,
        scope: GitHubRefreshScope,
        source: GitHubRefreshSource,
        totalTrackedCount: Int,
    ) = refreshRepository.notifyRefreshFailed(
        context = context,
        current = current,
        total = total,
        preReleaseUpdateCount = preReleaseUpdateCount,
        updatableCount = updatableCount,
        failedCount = failedCount,
        sessionId = sessionId,
        scope = scope,
        source = source,
        totalTrackedCount = totalTrackedCount,
    )

    suspend fun notifyRefreshCancelled(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long,
        scope: GitHubRefreshScope,
        source: GitHubRefreshSource,
        totalTrackedCount: Int,
    ) = refreshRepository.notifyRefreshCancelled(
        context = context,
        current = current,
        total = total,
        preReleaseUpdateCount = preReleaseUpdateCount,
        updatableCount = updatableCount,
        failedCount = failedCount,
        sessionId = sessionId,
        scope = scope,
        source = source,
        totalTrackedCount = totalTrackedCount,
    )

    fun cancelRefreshNotification(
        context: Context,
        sessionId: Long,
    ) = refreshRepository.cancelRefreshNotification(context, sessionId)

    suspend fun clearReleaseStrategyCaches() = refreshRepository.clearReleaseStrategyCaches()

    suspend fun clearAllAssetCache() = assetBridge.clearAllAssetCache()

    suspend fun parseTrackedItemsImport(raw: String): GitHubTrackedItemsImportPayload = transferRepository.parseTrackedItemsImport(raw)

    suspend fun buildTrackedItemsImportPreview(
        payload: GitHubTrackedItemsImportPayload,
        existingItems: List<GitHubTrackedApp>,
    ): GitHubTrackImportPreview =
        transferRepository.buildTrackedItemsImportPreview(
            payload = payload,
            existingItems = existingItems,
        )

    suspend fun buildStrategyBenchmarkTargets(items: List<GitHubTrackedApp>): List<GitHubRepoTarget> =
        discoveryRepository.buildStrategyBenchmarkTargets(items)

    suspend fun runStrategyBenchmark(
        targets: List<GitHubRepoTarget>,
        apiToken: String,
    ): GitHubStrategyBenchmarkReport = discoveryRepository.runStrategyBenchmark(targets, apiToken)

    suspend fun checkCredential(apiToken: String): GitHubStrategyLoadTrace<GitHubApiCredentialStatus> =
        discoveryRepository.checkCredential(apiToken)

    suspend fun buildTrackedItem(draft: GitHubTrackEditorDraft): GitHubTrackEditorResult = discoveryRepository.buildTrackedItem(draft)

    suspend fun previewStarredRepositoryImport(
        request: GitHubStarredRepositoryImportRequest,
        existingItems: List<GitHubTrackedApp>,
    ): Result<GitHubStarredRepositoryImportPreview> = discoveryRepository.previewStarredRepositoryImport(request, existingItems)

    suspend fun searchRepositoriesForApp(
        request: GitHubAppRepositorySearchRequest,
        existingItems: List<GitHubTrackedApp>,
    ): Result<GitHubAppRepositorySearchResult> = discoveryRepository.searchRepositoriesForApp(request, existingItems)

    suspend fun scanPackageNameFromRepositoryApk(request: GitHubApkPackageNameScanRequest): Result<GitHubApkPackageNameScanResult> =
        discoveryRepository.scanPackageNameFromRepositoryApk(request)

    suspend fun scanPackageNameFromDirectApk(
        repoUrl: String,
        lookupConfig: GitHubLookupConfig,
    ): Result<GitHubApkPackageNameScanResult> = discoveryRepository.scanPackageNameFromDirectApk(repoUrl, lookupConfig)

    suspend fun scanRepositoryFromPackage(request: GitHubPackageRepositoryScanRequest): Result<GitHubPackageRepositoryScanResult> =
        discoveryRepository.scanRepositoryFromPackage(request)

    suspend fun searchFdroidApps(request: FdroidAppSearchRequest): Result<FdroidAppSearchResult> =
        discoveryRepository.searchFdroidApps(request)

    fun buildReleaseUrl(
        owner: String,
        repo: String,
    ): String = assetBridge.buildReleaseUrl(owner, repo)

    fun buildAssetCacheKey(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        preferHtml: Boolean,
        aggressiveFiltering: Boolean,
        includeAllAssets: Boolean,
        hasApiToken: Boolean,
    ): String =
        assetBridge.buildAssetCacheKey(
            owner = owner,
            repo = repo,
            rawTag = rawTag,
            releaseUrl = releaseUrl,
            preferHtml = preferHtml,
            aggressiveFiltering = aggressiveFiltering,
            includeAllAssets = includeAllAssets,
            hasApiToken = hasApiToken,
        )

    suspend fun loadAssetBundle(
        cacheKey: String,
        refreshIntervalHours: Int,
    ): GitHubReleaseAssetBundle? = assetBridge.loadAssetBundle(cacheKey, refreshIntervalHours)

    suspend fun saveAssetBundle(
        cacheKey: String,
        bundle: GitHubReleaseAssetBundle,
    ) = assetBridge.saveAssetBundle(cacheKey, bundle)

    suspend fun clearAssetCache(cacheKey: String) = assetBridge.clearAssetCache(cacheKey)

    suspend fun clearAssetCaches(cacheKeys: List<String>) = assetBridge.clearAssetCaches(cacheKeys)

    suspend fun fetchApkAssets(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        preferHtml: Boolean,
        aggressiveFiltering: Boolean,
        includeAllAssets: Boolean,
        apiToken: String,
    ): Result<GitHubReleaseAssetBundle> =
        assetBridge.fetchApkAssets(
            owner = owner,
            repo = repo,
            rawTag = rawTag,
            releaseUrl = releaseUrl,
            preferHtml = preferHtml,
            aggressiveFiltering = aggressiveFiltering,
            includeAllAssets = includeAllAssets,
            apiToken = apiToken,
        )

    suspend fun fetchReleasePage(
        owner: String,
        repo: String,
        apiToken: String,
        page: Int,
    ): Result<GitHubReleasePage> = assetBridge.fetchReleasePage(owner, repo, apiToken, page)

    suspend fun fetchReleaseNotesTargets(
        owner: String,
        repo: String,
        apiToken: String,
    ): Result<List<GitHubReleaseNotesTarget>> = assetBridge.fetchReleaseNotesTargets(owner, repo, apiToken)

    fun buildGitRepositoryAssetCacheKey(
        identity: GitRepositoryTrackIdentity,
        rawTag: String,
        releaseUrl: String,
        lookupConfig: GitHubLookupConfig,
        includeAllAssets: Boolean,
    ): String =
        assetBridge.buildGitRepositoryAssetCacheKey(
            identity = identity,
            rawTag = rawTag,
            releaseUrl = releaseUrl,
            lookupConfig = lookupConfig,
            includeAllAssets = includeAllAssets,
        )

    suspend fun fetchGitRepositoryReleaseNotesTargets(
        identity: GitRepositoryTrackIdentity,
    ): Result<List<GitHubReleaseNotesTarget>> =
        assetBridge.fetchGitRepositoryReleaseNotesTargets(identity)

    suspend fun fetchGitRepositoryReleaseAssetBundle(
        identity: GitRepositoryTrackIdentity,
        rawTag: String,
        releaseUrl: String,
        lookupConfig: GitHubLookupConfig,
        includeAllAssets: Boolean,
    ): Result<GitHubReleaseAssetBundle> =
        assetBridge.fetchGitRepositoryReleaseAssetBundle(
            identity = identity,
            rawTag = rawTag,
            releaseUrl = releaseUrl,
            lookupConfig = lookupConfig,
            includeAllAssets = includeAllAssets,
        )

    suspend fun resolvePreferredDownloadUrl(
        asset: GitHubReleaseAssetFile,
        useApiAssetUrl: Boolean,
        apiToken: String,
    ): String = assetBridge.resolvePreferredDownloadUrl(asset, useApiAssetUrl, apiToken)

    suspend fun buildTrackedItemsExportJson(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long,
    ): String = transferRepository.buildTrackedItemsExportJson(items, exportedAtMillis)

    suspend fun writeText(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String,
    ) = transferRepository.writeText(contentResolver, uri, content)

    suspend fun readText(
        contentResolver: ContentResolver,
        uri: Uri,
    ): String = transferRepository.readText(contentResolver, uri)
}
