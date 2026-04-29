package os.kei.ui.page.main.github.page

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import os.kei.feature.github.data.local.GitHubActionsDownloadHistoryStore
import os.kei.feature.github.data.remote.GitHubActionsRepository
import os.kei.feature.github.domain.GitHubActionsArtifactSelector
import os.kei.feature.github.domain.GitHubActionsRunSelector
import os.kei.feature.github.domain.GitHubActionsRunTracker
import os.kei.feature.github.domain.GitHubActionsWorkflowSelector
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
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactSignal
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import os.kei.feature.github.model.GitHubActionsWorkflowSelectionOptions
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubStrategyLoadTrace

private const val githubActionsSignalWorkflowBatchSize = 2

internal class GitHubActionsPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
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
        lookupConfig: GitHubLookupConfig,
        preferApiTokenRedirect: Boolean = false
    ): Result<GitHubActionsArtifactDownloadResolution> {
        return withContext(ioDispatcher) {
            GitHubActionsRepository.fromLookupConfig(lookupConfig)
                .resolveArtifactDownloadUrl(
                    artifact = artifact,
                    owner = owner,
                    repo = repo,
                    preferApiTokenRedirect = preferApiTokenRedirect
                )
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
