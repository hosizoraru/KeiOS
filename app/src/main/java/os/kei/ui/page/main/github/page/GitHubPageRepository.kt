package os.kei.ui.page.main.github.page

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import os.kei.core.background.AppBackgroundScheduler
import os.kei.feature.github.data.local.AppIconCache
import os.kei.feature.github.data.local.GitHubActionsDownloadHistoryStore
import os.kei.feature.github.data.local.GitHubReleaseAssetCacheStore
import os.kei.feature.github.data.local.GitHubTrackSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.data.remote.GitHubApiTokenReleaseStrategy
import os.kei.feature.github.data.remote.GitHubActionsRepository
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.GitHubReleaseAssetRepository
import os.kei.feature.github.data.remote.GitHubReleaseStrategyRegistry
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.domain.GitHubActionsArtifactSelector
import os.kei.feature.github.domain.GitHubActionsRunTracker
import os.kei.feature.github.domain.GitHubActionsRunSelector
import os.kei.feature.github.domain.GitHubActionsWorkflowSelector
import os.kei.feature.github.domain.GitHubReleaseCheckService
import os.kei.feature.github.domain.GitHubStrategyBenchmarkService
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsArtifactDownloadResolution
import os.kei.feature.github.model.GitHubActionsArtifactMatch
import os.kei.feature.github.model.GitHubActionsArtifactSelectionOptions
import os.kei.feature.github.model.GitHubActionsDownloadRecord
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubActionsRepositoryInfo
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunSelectionOptions
import os.kei.feature.github.model.GitHubActionsRunStatusSnapshot
import os.kei.feature.github.model.GitHubActionsRunTrackingPlan
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactSignal
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.feature.github.model.GitHubActionsWorkflowSelectionOptions
import os.kei.feature.github.model.GitHubApiCredentialStatus
import os.kei.feature.github.model.GitHubCheckCacheEntry
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubRepoTarget
import os.kei.feature.github.model.GitHubStrategyBenchmarkReport
import os.kei.feature.github.model.GitHubStrategyLoadTrace
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.InstalledAppItem
import os.kei.feature.github.notification.GitHubRefreshNotificationHelper
import os.kei.ui.page.main.github.GitHubSortMode
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.query.queryDownloaderOptions
import os.kei.ui.page.main.github.query.queryOnlineShareTargetOptions
import os.kei.ui.page.main.github.section.GitHubOverviewMetrics
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack
import os.kei.ui.page.main.github.state.toUi

private const val pendingShareImportCardVisibleWindowMs = 90_000L
private const val githubActionsSignalWorkflowBatchSize = 2

internal data class GitHubTrackEditorDraft(
    val repoUrl: String,
    val packageName: String,
    val preferPreRelease: Boolean,
    val alwaysShowLatestReleaseDownloadButton: Boolean,
    val appList: List<InstalledAppItem>
)

internal sealed interface GitHubTrackEditorResult {
    data class Ready(val item: GitHubTrackedApp) : GitHubTrackEditorResult
    data object InvalidRepository : GitHubTrackEditorResult
    data object InvalidPackageName : GitHubTrackEditorResult
}

internal data class GitHubPageContentInput(
    val trackedItems: List<GitHubTrackedApp>,
    val trackedSearch: String,
    val sortMode: GitHubSortMode,
    val checkStates: Map<String, VersionCheckUi>,
    val appList: List<InstalledAppItem>,
    val trackedFirstInstallAtByPackage: Map<String, Long>,
    val trackedAddedAtById: Map<String, Long>,
    val pendingShareImportTrack: GitHubPendingShareImportTrack?,
    val nowMillis: Long
)

internal data class GitHubOnlineShareTargetInput(
    val shouldResolve: Boolean,
    val appList: List<InstalledAppItem>
)

internal class GitHubPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val packageNamePattern = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$""")

    suspend fun buildContentState(input: GitHubPageContentInput): GitHubPageContentDerivedState {
        return withContext(defaultDispatcher) {
            val filteredTracked = input.trackedItems.filter { item ->
                input.trackedSearch.isBlank() ||
                    item.owner.contains(input.trackedSearch, ignoreCase = true) ||
                    item.repo.contains(input.trackedSearch, ignoreCase = true) ||
                    item.appLabel.contains(input.trackedSearch, ignoreCase = true) ||
                    item.packageName.contains(input.trackedSearch, ignoreCase = true)
            }
            val isSortUpdatable: (GitHubTrackedApp) -> Boolean = { item ->
                item.alwaysShowLatestReleaseDownloadButton || input.checkStates[item.id]?.hasUpdate == true
            }
            val sortedTracked = when (input.sortMode) {
                GitHubSortMode.UpdateFirst -> filteredTracked.sortedWith(
                    compareByDescending<GitHubTrackedApp> { isSortUpdatable(it) }
                        .thenByDescending { input.checkStates[it.id]?.hasPreReleaseUpdate == true }
                        .thenBy { it.appLabel.lowercase() }
                )

                GitHubSortMode.NameAsc -> filteredTracked.sortedBy { it.appLabel.lowercase() }
                GitHubSortMode.PreReleaseFirst -> filteredTracked.sortedWith(
                    compareByDescending<GitHubTrackedApp> {
                        input.checkStates[it.id]?.isPreRelease == true
                    }
                        .thenByDescending { isSortUpdatable(it) }
                        .thenBy { it.appLabel.lowercase() }
                )
            }
            val trackedCount = input.trackedItems.size
            val updatableCount = input.trackedItems.count { input.checkStates[it.id]?.hasUpdate == true }
            val preReleaseCount = input.trackedItems.count { input.checkStates[it.id]?.isPreRelease == true }
            val preReleaseUpdateCount =
                input.trackedItems.count { input.checkStates[it.id]?.hasPreReleaseUpdate == true }
            val failedCount = input.trackedItems.count { input.checkStates[it.id]?.failed == true }
            val stableLatestCount = input.trackedItems.count {
                val itemState = input.checkStates[it.id]
                itemState?.hasUpdate == false && itemState.isPreRelease.not()
            }
            val appLastUpdatedAtByTrackId = buildAppLastUpdatedAtByTrackId(input)
            val pendingShareImportRepoOverlapCount = input.pendingShareImportTrack?.let { pending ->
                input.trackedItems.count { item ->
                    item.owner.equals(pending.owner, ignoreCase = true) &&
                        item.repo.equals(pending.repo, ignoreCase = true)
                }
            } ?: 0
            val showPendingShareImportCard = input.pendingShareImportTrack?.let { pending ->
                val ageMs = (input.nowMillis - pending.armedAtMillis).coerceAtLeast(0L)
                ageMs <= pendingShareImportCardVisibleWindowMs ||
                    pendingShareImportRepoOverlapCount > 0
            } ?: false
            GitHubPageContentDerivedState(
                trackedUi = GitHubPageDerivedState(
                    filteredTracked = filteredTracked,
                    sortedTracked = sortedTracked,
                    overviewMetrics = GitHubOverviewMetrics(
                        trackedCount = trackedCount,
                        updatableCount = updatableCount,
                        stableLatestCount = stableLatestCount,
                        preReleaseCount = preReleaseCount,
                        preReleaseUpdateCount = preReleaseUpdateCount,
                        failedCount = failedCount
                    )
                ),
                appLastUpdatedAtByTrackId = appLastUpdatedAtByTrackId,
                pendingShareImportRepoOverlapCount = pendingShareImportRepoOverlapCount,
                showPendingShareImportCard = showPendingShareImportCard
            )
        }
    }

    suspend fun queryOnlineShareTargets(
        context: Context,
        input: GitHubOnlineShareTargetInput
    ): List<OnlineShareTargetOption> {
        if (!input.shouldResolve) return emptyList()
        return withContext(defaultDispatcher) {
            queryOnlineShareTargetOptions(context, input.appList)
        }
    }

    suspend fun queryDownloaders(context: Context): List<DownloaderOption> {
        return withContext(defaultDispatcher) {
            queryDownloaderOptions(context)
        }
    }

    suspend fun loadTrackSnapshot(): GitHubTrackSnapshot {
        return withContext(ioDispatcher) {
            GitHubTrackStore.loadSnapshot()
        }
    }

    suspend fun loadLookupConfig(): GitHubLookupConfig {
        return withContext(ioDispatcher) {
            GitHubTrackStore.loadLookupConfig()
        }
    }

    suspend fun saveLookupConfig(config: GitHubLookupConfig) {
        withContext(ioDispatcher) {
            GitHubTrackStore.saveLookupConfig(config)
        }
    }

    suspend fun loadRefreshIntervalHours(): Int {
        return withContext(ioDispatcher) {
            GitHubTrackStore.loadRefreshIntervalHours()
        }
    }

    suspend fun saveRefreshIntervalHours(hours: Int) {
        withContext(ioDispatcher) {
            GitHubTrackStore.saveRefreshIntervalHours(hours)
        }
    }

    suspend fun saveTrackedItems(
        context: Context,
        items: List<GitHubTrackedApp>,
        trackedFirstInstallAtByPackage: Map<String, Long>,
        trackedAddedAtById: Map<String, Long>,
        refreshTrackIds: Set<String> = emptySet()
    ) {
        withContext(ioDispatcher) {
            GitHubTrackStore.save(items)
            GitHubTrackStore.saveTrackedFirstInstallAtByPackage(trackedFirstInstallAtByPackage)
            GitHubTrackStore.saveTrackedAddedAtById(trackedAddedAtById)
            refreshTrackIds.forEach { trackId ->
                GitHubTrackStoreSignals.requestTrackRefresh(
                    trackId = trackId,
                    notifyChangeSignal = false
                )
            }
            GitHubTrackStoreSignals.notifyChanged()
        }
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
    }

    suspend fun saveCheckCache(
        states: Map<String, GitHubCheckCacheEntry>,
        refreshTimestamp: Long
    ) {
        withContext(ioDispatcher) {
            GitHubTrackStore.saveCheckCache(states, refreshTimestamp)
        }
    }

    suspend fun clearCheckCache() {
        withContext(ioDispatcher) {
            GitHubTrackStore.clearCheckCache()
        }
    }

    suspend fun clearPendingShareImportTrack() {
        withContext(ioDispatcher) {
            GitHubTrackStore.savePendingShareImportTrack(null)
            GitHubTrackStoreSignals.notifyChanged()
        }
    }

    fun scheduleGitHubRefresh(context: Context) {
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
    }

    fun currentTrackStoreSignalVersion(): Long {
        return GitHubTrackStoreSignals.version.value
    }

    fun trackStoreSignalVersions(): StateFlow<Long> {
        return GitHubTrackStoreSignals.version
    }

    fun buildAppListPermissionIntent(context: Context): Intent? {
        return GitHubVersionUtils.buildAppListPermissionIntent(context)
    }

    suspend fun consumeTrackRefreshRequests(validTrackIds: Set<String>): Set<String> {
        return withContext(ioDispatcher) {
            GitHubTrackStoreSignals.consumeTrackRefreshRequests(validTrackIds)
        }
    }

    suspend fun queryInstalledLaunchableApps(
        context: Context,
        forceRefresh: Boolean
    ): List<InstalledAppItem> {
        return withContext(ioDispatcher) {
            GitHubVersionUtils.queryInstalledLaunchableApps(
                context = context,
                forceRefresh = forceRefresh
            )
        }
    }

    suspend fun preloadAppIcons(
        context: Context,
        packageNames: List<String>
    ) {
        if (packageNames.isEmpty()) return
        withContext(ioDispatcher) {
            AppIconCache.preload(context, packageNames)
        }
    }

    suspend fun localVersionInfoOrNull(
        context: Context,
        packageName: String
    ): GitHubVersionUtils.LocalVersionInfo? {
        return withContext(ioDispatcher) {
            GitHubVersionUtils.localVersionInfoOrNull(context, packageName)
        }
    }

    suspend fun evaluateTrackedApp(
        context: Context,
        item: GitHubTrackedApp
    ): VersionCheckUi {
        return withContext(ioDispatcher) {
            GitHubReleaseCheckService.evaluateTrackedApp(context, item).toUi()
        }
    }

    suspend fun notifyRefreshProgress(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        withContext(ioDispatcher) {
            GitHubRefreshNotificationHelper.notifyProgress(
                context = context,
                current = current,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount
            )
        }
    }

    suspend fun notifyRefreshCompleted(
        context: Context,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        withContext(ioDispatcher) {
            GitHubRefreshNotificationHelper.notifyCompleted(
                context = context,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount
            )
        }
    }

    suspend fun notifyRefreshCancelled(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int
    ) {
        withContext(ioDispatcher) {
            GitHubRefreshNotificationHelper.notifyCancelled(
                context = context,
                current = current,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount
            )
        }
    }

    fun cancelRefreshNotification(context: Context) {
        GitHubRefreshNotificationHelper.cancel(context)
    }

    suspend fun clearReleaseStrategyCaches() {
        withContext(ioDispatcher) {
            GitHubReleaseStrategyRegistry.clearAllCaches()
        }
    }

    suspend fun clearAllAssetCache() {
        withContext(ioDispatcher) {
            GitHubReleaseAssetCacheStore.clearAll()
        }
    }

    suspend fun parseTrackedItemsImport(raw: String): GitHubTrackedItemsImportPayload {
        return withContext(defaultDispatcher) {
            GitHubTrackStore.parseTrackedItemsImport(raw)
        }
    }

    suspend fun buildTrackedItemsImportPreview(
        payload: GitHubTrackedItemsImportPayload,
        existingItems: List<GitHubTrackedApp>
    ): GitHubTrackImportPreview {
        return withContext(defaultDispatcher) {
            val existingItemsById = existingItems.associateBy { it.id }
            var newCount = 0
            var updatedCount = 0
            var unchangedCount = 0
            payload.items.forEach { item ->
                when (val existingItem = existingItemsById[item.id]) {
                    null -> newCount += 1
                    item -> unchangedCount += 1
                    else -> updatedCount += 1
                }
            }
            GitHubTrackImportPreview(
                payload = payload,
                fileItemCount = payload.sourceCount,
                validCount = payload.items.size,
                duplicateCount = payload.duplicateCount,
                invalidCount = payload.invalidCount,
                newCount = newCount,
                updatedCount = updatedCount,
                unchangedCount = unchangedCount,
                mergedCount = existingItems.size + newCount
            )
        }
    }

    suspend fun buildStrategyBenchmarkTargets(
        items: List<GitHubTrackedApp>
    ): List<GitHubRepoTarget> {
        return withContext(defaultDispatcher) {
            GitHubStrategyBenchmarkService.buildTargets(items)
        }
    }

    suspend fun runStrategyBenchmark(
        targets: List<GitHubRepoTarget>,
        apiToken: String
    ): GitHubStrategyBenchmarkReport {
        return withContext(ioDispatcher) {
            GitHubStrategyBenchmarkService.compareTargets(
                targets = targets,
                apiToken = apiToken
            )
        }
    }

    suspend fun checkCredential(
        apiToken: String
    ): GitHubStrategyLoadTrace<GitHubApiCredentialStatus> {
        return withContext(ioDispatcher) {
            GitHubApiTokenReleaseStrategy(apiToken).checkCredentialTrace()
        }
    }

    suspend fun fetchGitHubActionsWorkflows(
        owner: String,
        repo: String,
        lookupConfig: GitHubLookupConfig
    ): GitHubStrategyLoadTrace<List<GitHubActionsWorkflow>> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .fetchWorkflows(owner = owner, repo = repo)
        }
    }

    suspend fun fetchGitHubActionsRepositoryInfo(
        owner: String,
        repo: String,
        lookupConfig: GitHubLookupConfig
    ): GitHubStrategyLoadTrace<GitHubActionsRepositoryInfo> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .fetchRepositoryInfo(owner = owner, repo = repo)
        }
    }

    suspend fun fetchGitHubActionsRepositoryDefaultBranch(
        owner: String,
        repo: String,
        lookupConfig: GitHubLookupConfig
    ): GitHubStrategyLoadTrace<String> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .fetchRepositoryDefaultBranch(owner = owner, repo = repo)
        }
    }

    suspend fun fetchGitHubActionsWorkflowArtifactSnapshot(
        owner: String,
        repo: String,
        workflowId: String,
        lookupConfig: GitHubLookupConfig,
        runLimit: Int = 20,
        artifactsPerRun: Int = 100,
        artifactRunLimit: Int = Int.MAX_VALUE,
        branch: String = "",
        event: String = "",
        status: String = "",
        actor: String = "",
        created: String = "",
        headSha: String = "",
        excludePullRequests: Boolean = false,
        resolveNightlyRunDetail: Boolean = true
    ): GitHubStrategyLoadTrace<GitHubActionsWorkflowArtifactsSnapshot> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .fetchWorkflowArtifactSnapshot(
                    owner = owner,
                    repo = repo,
                    workflowId = workflowId,
                    runLimit = runLimit,
                    artifactsPerRun = artifactsPerRun,
                    artifactRunLimit = artifactRunLimit,
                    branch = branch,
                    event = event,
                    status = status,
                    actor = actor,
                    created = created,
                    headSha = headSha,
                    excludePullRequests = excludePullRequests,
                    resolveNightlyRunDetail = resolveNightlyRunDetail
                )
        }
    }

    suspend fun fetchGitHubActionsWorkflowRun(
        owner: String,
        repo: String,
        runId: Long,
        lookupConfig: GitHubLookupConfig
    ): GitHubStrategyLoadTrace<GitHubActionsWorkflowRun> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .fetchWorkflowRun(owner = owner, repo = repo, runId = runId)
        }
    }

    suspend fun fetchGitHubActionsRunStatusSnapshot(
        owner: String,
        repo: String,
        runId: Long,
        lookupConfig: GitHubLookupConfig,
        artifactsLimit: Int = 100,
        includeArtifactsWhenCompleted: Boolean = true
    ): GitHubStrategyLoadTrace<GitHubActionsRunStatusSnapshot> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .fetchRunStatusSnapshot(
                    owner = owner,
                    repo = repo,
                    runId = runId,
                    artifactsLimit = artifactsLimit,
                    includeArtifactsWhenCompleted = includeArtifactsWhenCompleted
                )
        }
    }

    suspend fun buildGitHubActionsRunTrackingPlan(
        run: GitHubActionsWorkflowRun
    ): GitHubActionsRunTrackingPlan {
        return withContext(defaultDispatcher) {
            GitHubActionsRunTracker.buildTrackingPlan(run)
        }
    }

    suspend fun fetchGitHubActionsWorkflowArtifactSignals(
        owner: String,
        repo: String,
        workflows: List<GitHubActionsWorkflow>,
        lookupConfig: GitHubLookupConfig,
        runLimit: Int = 3,
        artifactsPerRun: Int = 100,
        defaultBranch: String = ""
    ): GitHubStrategyLoadTrace<Map<Long, GitHubActionsWorkflowArtifactSignal>> {
        return coroutineScope {
            val startedAt = System.currentTimeMillis()
            val actionsRepository = GitHubActionsRepository.fromLookupConfig(lookupConfig)
            val signals = mutableMapOf<Long, GitHubActionsWorkflowArtifactSignal>()
            val useNightlyLink = lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink
            workflows.chunked(githubActionsSignalWorkflowBatchSize).forEach { batch ->
                batch.map { workflow ->
                    async(ioDispatcher) {
                        val workflowId = workflowLookupId(workflow, lookupConfig)
                        val primaryBranch = if (useNightlyLink) defaultBranch else ""
                        val recentSnapshot = actionsRepository.fetchWorkflowArtifactSnapshot(
                            owner = owner,
                            repo = repo,
                            workflowId = workflowId,
                            runLimit = runLimit,
                            artifactsPerRun = artifactsPerRun,
                            branch = primaryBranch,
                            status = if (useNightlyLink) "completed" else "",
                            excludePullRequests = useNightlyLink,
                            resolveNightlyRunDetail = false
                        ).result.getOrElse {
                            return@async null
                        }
                        val recentRuns = recentSnapshot.runs
                        val recentHasDefaultBranchArtifact = defaultBranch.isNotBlank() &&
                            recentRuns.any { runArtifacts ->
                                val run = runArtifacts.run
                                run.headBranch.equals(defaultBranch, ignoreCase = true) &&
                                    run.status.equals("completed", ignoreCase = true) &&
                                    run.conclusion.equals("success", ignoreCase = true) &&
                                    runArtifacts.artifacts.any { artifact -> !artifact.expired }
                            }
                        val defaultBranchRuns = if (useNightlyLink || recentHasDefaultBranchArtifact) {
                            emptyList()
                        } else {
                            defaultBranch
                                .takeIf { it.isNotBlank() }
                                ?.let { branch ->
                                    actionsRepository.fetchWorkflowArtifactSnapshot(
                                        owner = owner,
                                        repo = repo,
                                        workflowId = workflowId,
                                        runLimit = 1,
                                        artifactsPerRun = artifactsPerRun,
                                        branch = branch,
                                        status = "completed",
                                        excludePullRequests = true,
                                        resolveNightlyRunDetail = false
                                    ).result.getOrNull()?.runs
                                }
                                .orEmpty()
                        }
                        val mergedRuns = (recentRuns + defaultBranchRuns)
                            .distinctBy { it.run.id }
                        workflow.id to GitHubActionsWorkflowSelector.buildArtifactSignal(
                            workflow = workflow,
                            runs = mergedRuns,
                            defaultBranch = defaultBranch
                        )
                    }
                }.awaitAll().filterNotNull().forEach { (workflowId, signal) ->
                    signals[workflowId] = signal
                }
            }
            GitHubStrategyLoadTrace(
                result = Result.success(signals),
                fromCache = false,
                elapsedMs = System.currentTimeMillis() - startedAt,
                authMode = actionsRepository.authMode
            )
        }
    }

    suspend fun loadGitHubActionsDownloadHistory(
        owner: String = "",
        repo: String = ""
    ): List<GitHubActionsDownloadRecord> {
        return withContext(ioDispatcher) {
            GitHubActionsDownloadHistoryStore.load(owner = owner, repo = repo)
        }
    }

    suspend fun recordGitHubActionsArtifactDownload(
        record: GitHubActionsDownloadRecord
    ) {
        withContext(ioDispatcher) {
            GitHubActionsDownloadHistoryStore.recordDownload(record)
        }
    }

    fun buildGitHubActionsDownloadRecord(
        owner: String,
        repo: String,
        workflow: GitHubActionsWorkflow,
        run: GitHubActionsWorkflowRun,
        artifact: GitHubActionsArtifact,
        sourceTrackId: String = "",
        packageName: String = "",
        downloadedAtMillis: Long = System.currentTimeMillis()
    ): GitHubActionsDownloadRecord {
        return GitHubActionsDownloadRecord(
            owner = owner,
            repo = repo,
            workflowId = workflow.id,
            workflowName = workflow.name,
            workflowPath = workflow.path,
            runId = run.id,
            runNumber = run.runNumber,
            runAttempt = run.runAttempt,
            runDisplayName = run.displayName,
            headBranch = run.headBranch,
            headSha = run.headSha,
            event = run.event,
            status = run.status,
            conclusion = run.conclusion,
            artifactId = artifact.id,
            artifactName = artifact.name,
            artifactDigest = artifact.digest,
            artifactSizeBytes = artifact.sizeBytes,
            sourceTrackId = sourceTrackId,
            packageName = packageName,
            downloadedAtMillis = downloadedAtMillis
        )
    }

    suspend fun clearGitHubActionsDownloadHistory(
        owner: String = "",
        repo: String = ""
    ) {
        withContext(ioDispatcher) {
            GitHubActionsDownloadHistoryStore.clear(owner = owner, repo = repo)
        }
    }

    suspend fun selectGitHubActionsRuns(
        runs: List<GitHubActionsRunArtifacts>,
        options: GitHubActionsRunSelectionOptions,
        workflow: GitHubActionsWorkflow? = null
    ): List<GitHubActionsRunMatch> {
        return withContext(defaultDispatcher) {
            GitHubActionsRunSelector.selectRuns(
                runs = runs,
                options = options,
                workflowTraits = workflow?.let(GitHubActionsWorkflowSelector::inspectWorkflow)
            )
        }
    }

    suspend fun selectGitHubActionsArtifacts(
        artifacts: List<GitHubActionsArtifact>,
        options: GitHubActionsArtifactSelectionOptions
    ): List<GitHubActionsArtifactMatch> {
        return withContext(defaultDispatcher) {
            GitHubActionsArtifactSelector.selectDisplayArtifacts(
                artifacts = artifacts,
                options = options
            )
        }
    }

    suspend fun selectGitHubActionsWorkflows(
        workflows: List<GitHubActionsWorkflow>,
        artifactSignals: Map<Long, GitHubActionsWorkflowArtifactSignal>,
        options: GitHubActionsWorkflowSelectionOptions
    ): List<GitHubActionsWorkflowMatch> {
        return withContext(defaultDispatcher) {
            GitHubActionsWorkflowSelector.selectWorkflows(
                workflows = workflows,
                artifactSignals = artifactSignals,
                options = options
            )
        }
    }

    suspend fun resolveGitHubActionsArtifactDownloadUrl(
        artifact: GitHubActionsArtifact,
        owner: String,
        repo: String,
        lookupConfig: GitHubLookupConfig
    ): Result<GitHubActionsArtifactDownloadResolution> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .resolveArtifactDownloadUrl(
                    artifact = artifact,
                    owner = owner,
                    repo = repo
                )
        }
    }

    suspend fun buildTrackedItem(draft: GitHubTrackEditorDraft): GitHubTrackEditorResult {
        return withContext(defaultDispatcher) {
            val parsed = GitHubVersionUtils.parseOwnerRepo(draft.repoUrl)
                ?: return@withContext GitHubTrackEditorResult.InvalidRepository
            val resolvedPackageName = draft.packageName.trim()
            if (resolvedPackageName.isNotBlank() && !packageNamePattern.matches(resolvedPackageName)) {
                return@withContext GitHubTrackEditorResult.InvalidPackageName
            }
            val matchedInstalledApp = resolvedPackageName
                .takeIf { it.isNotBlank() }
                ?.let { packageName ->
                    draft.appList.firstOrNull { item ->
                        item.packageName.equals(packageName, ignoreCase = true)
                    }
                }
            val resolvedAppLabel = when {
                matchedInstalledApp != null -> matchedInstalledApp.label
                resolvedPackageName.isNotBlank() -> resolvedPackageName
                else -> "${parsed.first}/${parsed.second}"
            }
            GitHubTrackEditorResult.Ready(
                GitHubTrackedApp(
                    repoUrl = draft.repoUrl.trim(),
                    owner = parsed.first,
                    repo = parsed.second,
                    packageName = resolvedPackageName,
                    appLabel = resolvedAppLabel,
                    preferPreRelease = draft.preferPreRelease,
                    alwaysShowLatestReleaseDownloadButton = draft.alwaysShowLatestReleaseDownloadButton
                )
            )
        }
    }

    fun buildReleaseUrl(owner: String, repo: String): String {
        return GitHubVersionUtils.buildReleaseUrl(owner, repo)
    }

    fun buildAssetCacheKey(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        preferHtml: Boolean,
        aggressiveFiltering: Boolean,
        includeAllAssets: Boolean,
        hasApiToken: Boolean
    ): String {
        return GitHubReleaseAssetCacheStore.buildCacheKey(
            owner = owner,
            repo = repo,
            rawTag = rawTag,
            releaseUrl = releaseUrl,
            preferHtml = preferHtml,
            aggressiveFiltering = aggressiveFiltering,
            includeAllAssets = includeAllAssets,
            hasApiToken = hasApiToken
        )
    }

    suspend fun loadAssetBundle(
        cacheKey: String,
        refreshIntervalHours: Int
    ): GitHubReleaseAssetBundle? {
        return withContext(ioDispatcher) {
            GitHubReleaseAssetCacheStore.load(
                cacheKey = cacheKey,
                refreshIntervalHours = refreshIntervalHours
            )
        }
    }

    suspend fun saveAssetBundle(
        cacheKey: String,
        bundle: GitHubReleaseAssetBundle
    ) {
        withContext(ioDispatcher) {
            GitHubReleaseAssetCacheStore.save(
                cacheKey = cacheKey,
                bundle = bundle
            )
        }
    }

    suspend fun clearAssetCache(cacheKey: String) {
        withContext(ioDispatcher) {
            GitHubReleaseAssetCacheStore.clear(cacheKey)
        }
    }

    suspend fun clearAssetCaches(cacheKeys: List<String>) {
        if (cacheKeys.isEmpty()) return
        withContext(ioDispatcher) {
            cacheKeys.forEach(GitHubReleaseAssetCacheStore::clear)
        }
    }

    suspend fun fetchApkAssets(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        preferHtml: Boolean,
        aggressiveFiltering: Boolean,
        includeAllAssets: Boolean,
        apiToken: String
    ): Result<GitHubReleaseAssetBundle> {
        return withContext(ioDispatcher) {
            GitHubReleaseAssetRepository.fetchApkAssets(
                owner = owner,
                repo = repo,
                rawTag = rawTag,
                releaseUrl = releaseUrl,
                preferHtml = preferHtml,
                aggressiveFiltering = aggressiveFiltering,
                includeAllAssets = includeAllAssets,
                apiToken = apiToken
            )
        }
    }

    suspend fun resolvePreferredDownloadUrl(
        asset: GitHubReleaseAssetFile,
        useApiAssetUrl: Boolean,
        apiToken: String
    ): String {
        return withContext(ioDispatcher) {
            GitHubReleaseAssetRepository.resolvePreferredDownloadUrl(
                asset = asset,
                useApiAssetUrl = useApiAssetUrl,
                apiToken = apiToken
            ).getOrElse { asset.downloadUrl }
        }
    }

    suspend fun buildTrackedItemsExportJson(
        items: List<GitHubTrackedApp>,
        exportedAtMillis: Long
    ): String {
        return withContext(defaultDispatcher) {
            GitHubTrackStore.buildTrackedItemsExportJson(
                items = items,
                exportedAtMillis = exportedAtMillis
            )
        }
    }

    suspend fun writeText(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String
    ) {
        withContext(ioDispatcher) {
            contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                checkNotNull(writer) { "openOutputStream returned null" }
                writer.write(content)
            }
        }
    }

    suspend fun readText(
        contentResolver: ContentResolver,
        uri: Uri
    ): String {
        return withContext(ioDispatcher) {
            contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                checkNotNull(reader) { "openInputStream returned null" }
                reader.readText()
            }
        }
    }

    private fun buildAppLastUpdatedAtByTrackId(
        input: GitHubPageContentInput
    ): Map<String, Long> {
        val appUpdatedAtByPackage = buildMap {
            input.trackedFirstInstallAtByPackage.forEach { (packageName, firstInstallAtMillis) ->
                if (packageName.isNotBlank() && firstInstallAtMillis > 0L) {
                    put(packageName, firstInstallAtMillis)
                }
            }
            input.appList
                .filter { it.packageName.isNotBlank() && it.lastUpdateTimeMs > 0L }
                .forEach { put(it.packageName, it.lastUpdateTimeMs) }
        }
        return buildMap {
            input.trackedItems.forEach { item ->
                val byPackage = appUpdatedAtByPackage[item.packageName]
                val byTrackId = input.trackedAddedAtById[item.id]
                val updatedAt = byPackage?.takeIf { it > 0L } ?: byTrackId?.takeIf { it > 0L }
                if (updatedAt != null) {
                    put(item.id, updatedAt)
                }
            }
        }
    }

    private fun workflowLookupId(
        workflow: GitHubActionsWorkflow,
        lookupConfig: GitHubLookupConfig
    ): String {
        return if (lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink) {
            workflow.path.ifBlank { workflow.displayName }
        } else {
            workflow.id.toString()
        }
    }
}
