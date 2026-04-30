package os.kei.feature.github.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsArtifactDownloadResolution
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubActionsRepositoryInfo
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunStatusSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubStrategyLoadTrace
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

class GitHubActionsRepository(
    private val apiToken: String = "",
    private val client: OkHttpClient = githubClient,
    private val apiBaseUrl: String = DEFAULT_GITHUB_API_BASE_URL,
    private val actionsStrategy: GitHubActionsLookupStrategyOption = GitHubActionsLookupStrategyOption.GitHubApiToken,
    private val requireApiTokenForApiStrategy: Boolean = false,
    private val githubHtmlBaseUrl: String = DEFAULT_GITHUB_HTML_BASE_URL,
    private val nightlyLinkBaseUrl: String = DEFAULT_NIGHTLY_LINK_BASE_URL
) {
    private val sanitizedToken: String = apiToken.trim()
    private val useNightlyLink: Boolean
        get() = actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink
    private val nightlyRepository: GitHubActionsNightlyLinkRepository by lazy {
        GitHubActionsNightlyLinkRepository(
            client = client,
            githubHtmlBaseUrl = githubHtmlBaseUrl,
            nightlyLinkBaseUrl = nightlyLinkBaseUrl
        )
    }
    private val noRedirectClient: OkHttpClient by lazy {
        client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    val authMode: GitHubApiAuthMode
        get() = if (sanitizedToken.isBlank()) GitHubApiAuthMode.Guest else GitHubApiAuthMode.Token

    fun fetchRepositoryInfo(
        owner: String,
        repo: String
    ): GitHubStrategyLoadTrace<GitHubActionsRepositoryInfo> {
        val startedAt = System.currentTimeMillis()
        val result = if (useNightlyLink) {
            nightlyRepository.fetchRepositoryInfo(owner, repo)
        } else {
            requireActionsApiToken().mapCatching {
                fetchJson(
                    url = buildRepositoryUrl(owner, repo),
                    cacheTtlMillis = ACTIONS_METADATA_CACHE_TTL_MS
                ).getOrThrow()
                    .let { json -> parseRepositoryInfo(json, owner, repo) }
            }
        }
        return result.toTrace(startedAt)
    }

    fun fetchRepositoryDefaultBranch(
        owner: String,
        repo: String
    ): GitHubStrategyLoadTrace<String> {
        val startedAt = System.currentTimeMillis()
        val result = if (useNightlyLink) {
            nightlyRepository.fetchRepositoryInfo(owner, repo).mapCatching { it.defaultBranch }
        } else {
            requireActionsApiToken().mapCatching {
                fetchJson(
                    url = buildRepositoryUrl(owner, repo),
                    cacheTtlMillis = ACTIONS_METADATA_CACHE_TTL_MS
                ).getOrThrow()
                    .let { json -> parseRepositoryInfo(json, owner, repo).defaultBranch }
            }
        }
        return result.toTrace(startedAt)
    }

    fun fetchWorkflows(
        owner: String,
        repo: String,
        limit: Int = DEFAULT_WORKFLOW_LIMIT
    ): GitHubStrategyLoadTrace<List<GitHubActionsWorkflow>> {
        val startedAt = System.currentTimeMillis()
        val result = if (useNightlyLink) {
            nightlyRepository.fetchWorkflows(owner, repo, limit)
        } else {
            requireActionsApiToken().mapCatching {
                fetchJson(
                    url = buildWorkflowsUrl(owner, repo, limit),
                    cacheTtlMillis = ACTIONS_METADATA_CACHE_TTL_MS
                ).getOrThrow()
                    .let(::parseWorkflows)
            }
        }
        return result.toTrace(startedAt)
    }

    fun fetchWorkflowRuns(
        owner: String,
        repo: String,
        workflowId: String,
        limit: Int = DEFAULT_RUN_LIMIT,
        branch: String = "",
        event: String = "",
        status: String = "",
        actor: String = "",
        created: String = "",
        headSha: String = "",
        excludePullRequests: Boolean = false
    ): GitHubStrategyLoadTrace<List<GitHubActionsWorkflowRun>> {
        val startedAt = System.currentTimeMillis()
        val result = if (useNightlyLink) {
            nightlyRepository.fetchWorkflowRuns(
                owner = owner,
                repo = repo,
                workflowId = workflowId,
                branch = branch
            )
        } else {
            requireActionsApiToken().mapCatching {
                fetchJson(
                    url = buildWorkflowRunsUrl(
                        owner = owner,
                        repo = repo,
                        workflowId = workflowId,
                        limit = limit,
                        branch = branch,
                        event = event,
                        status = status,
                        actor = actor,
                        created = created,
                        headSha = headSha,
                        excludePullRequests = excludePullRequests
                    ),
                    cacheTtlMillis = ACTIONS_RUNS_CACHE_TTL_MS
                ).getOrThrow().let(::parseWorkflowRuns)
            }
        }
        return result.toTrace(startedAt)
    }

    fun fetchWorkflowRun(
        owner: String,
        repo: String,
        runId: Long
    ): GitHubStrategyLoadTrace<GitHubActionsWorkflowRun> {
        val startedAt = System.currentTimeMillis()
        val result = if (useNightlyLink) {
            nightlyRepository.fetchWorkflowRun(owner = owner, repo = repo, runId = runId)
        } else {
            requireActionsApiToken().mapCatching {
                fetchJson(buildWorkflowRunUrl(owner, repo, runId)).getOrThrow()
                    .let(::parseWorkflowRun)
            }
        }
        return result.toTrace(startedAt)
    }

    fun fetchRunArtifacts(
        owner: String,
        repo: String,
        runId: Long,
        limit: Int = DEFAULT_ARTIFACT_LIMIT
    ): GitHubStrategyLoadTrace<List<GitHubActionsArtifact>> {
        val startedAt = System.currentTimeMillis()
        val result = if (useNightlyLink) {
            nightlyRepository.fetchRunArtifacts(owner = owner, repo = repo, runId = runId, limit = limit)
        } else {
            requireActionsApiToken().mapCatching {
                fetchJson(
                    url = buildRunArtifactsUrl(owner, repo, runId, limit),
                    cacheTtlMillis = ACTIONS_ARTIFACT_CACHE_TTL_MS
                ).getOrThrow()
                    .let { json -> parseArtifacts(json, fallbackWorkflowRunId = runId) }
            }
        }
        return result.toTrace(startedAt)
    }

    fun fetchRunStatusSnapshot(
        owner: String,
        repo: String,
        runId: Long,
        artifactsLimit: Int = DEFAULT_ARTIFACT_LIMIT,
        includeArtifactsWhenCompleted: Boolean = true
    ): GitHubStrategyLoadTrace<GitHubActionsRunStatusSnapshot> {
        val startedAt = System.currentTimeMillis()
        if (useNightlyLink) {
            val result = nightlyRepository.fetchRunStatusSnapshot(
                owner = owner,
                repo = repo,
                runId = runId,
                artifactsLimit = artifactsLimit,
                includeArtifactsWhenCompleted = includeArtifactsWhenCompleted
            )
            return result.toTrace(startedAt)
        }
        requireActionsApiToken().onFailure { error ->
            return Result.failure<GitHubActionsRunStatusSnapshot>(error).toTrace(startedAt)
        }
        val run = fetchWorkflowRun(owner, repo, runId).result.getOrElse { error ->
            return Result.failure<GitHubActionsRunStatusSnapshot>(error).toTrace(startedAt)
        }
        val artifacts = if (
            includeArtifactsWhenCompleted &&
            run.status.equals("completed", ignoreCase = true)
        ) {
            fetchRunArtifacts(
                owner = owner,
                repo = repo,
                runId = runId,
                limit = artifactsLimit
            ).result.getOrElse { error ->
                return Result.failure<GitHubActionsRunStatusSnapshot>(error).toTrace(startedAt)
            }
        } else {
            emptyList()
        }
        return Result.success(
            GitHubActionsRunStatusSnapshot(
                owner = owner,
                repo = repo,
                run = run,
                artifacts = artifacts
            )
        ).toTrace(startedAt)
    }

    fun fetchWorkflowArtifactSnapshot(
        owner: String,
        repo: String,
        workflowId: String,
        runLimit: Int = DEFAULT_RUN_LIMIT,
        artifactsPerRun: Int = DEFAULT_ARTIFACT_LIMIT,
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
        val startedAt = System.currentTimeMillis()
        if (useNightlyLink) {
            val nightlyResult = nightlyRepository.fetchWorkflowArtifactSnapshot(
                owner = owner,
                repo = repo,
                workflowId = workflowId,
                branch = branch,
                artifactsPerRun = artifactsPerRun,
                resolveRunDetail = resolveNightlyRunDetail
            )
            val nightlySnapshot = nightlyResult.getOrNull()
            if (
                nightlySnapshot != null &&
                nightlySnapshot.artifacts.isNotEmpty() &&
                !nightlySnapshot.requiresPublicApiMetadataForNightlyDownload()
            ) {
                return nightlyResult.toTrace(startedAt)
            }
            val fallbackResult = fetchNightlyCompatibleWorkflowArtifactSnapshotFromPublicApi(
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
                excludePullRequests = excludePullRequests
            )
            val fallbackSnapshot = fallbackResult.getOrNull()
            val branchFallbackResult = if (
                branch.isNotBlank() &&
                fallbackSnapshot?.artifacts.orEmpty().isEmpty()
            ) {
                fetchNightlyCompatibleWorkflowArtifactSnapshotFromPublicApi(
                    owner = owner,
                    repo = repo,
                    workflowId = workflowId,
                    runLimit = runLimit,
                    artifactsPerRun = artifactsPerRun,
                    artifactRunLimit = artifactRunLimit,
                    branch = "",
                    event = event,
                    status = status,
                    actor = actor,
                    created = created,
                    headSha = headSha,
                    excludePullRequests = excludePullRequests
                )
            } else {
                null
            }
            val branchFallbackSnapshot = branchFallbackResult?.getOrNull()
            return when {
                fallbackSnapshot != null && fallbackSnapshot.artifacts.isNotEmpty() ->
                    fallbackResult.toTrace(startedAt)
                branchFallbackResult != null &&
                    branchFallbackSnapshot != null &&
                    branchFallbackSnapshot.artifacts.isNotEmpty() ->
                    branchFallbackResult.toTrace(startedAt)
                nightlyResult.isSuccess -> nightlyResult.toTrace(startedAt)
                fallbackResult.isSuccess -> fallbackResult.toTrace(startedAt)
                branchFallbackResult != null && branchFallbackResult.isSuccess ->
                    branchFallbackResult.toTrace(startedAt)
                else -> nightlyResult.toTrace(startedAt)
            }
        }
        requireActionsApiToken().onFailure { error ->
            return Result.failure<GitHubActionsWorkflowArtifactsSnapshot>(error).toTrace(startedAt)
        }
        val runs = fetchWorkflowRuns(
            owner = owner,
            repo = repo,
            workflowId = workflowId,
            limit = runLimit,
            branch = branch,
            event = event,
            status = status,
            actor = actor,
            created = created,
            headSha = headSha,
            excludePullRequests = excludePullRequests
        ).result.getOrElse { error ->
            return Result.failure<GitHubActionsWorkflowArtifactsSnapshot>(error).toTrace(startedAt)
        }
        val artifactRuns = runs.take(artifactRunLimit.coerceAtLeast(0))
        val artifactsByRun = runCatching {
            fetchRunArtifactsForRuns(
                owner = owner,
                repo = repo,
                runs = artifactRuns,
                limit = artifactsPerRun
            )
        }.getOrElse { error ->
            return Result.failure<GitHubActionsWorkflowArtifactsSnapshot>(error).toTrace(startedAt)
        }
        val artifactsByRunId = mutableMapOf<Long, List<GitHubActionsArtifact>>()
        artifactsByRun.forEach { (run, artifactsResult) ->
            val artifacts = artifactsResult.getOrElse { error ->
                return Result.failure<GitHubActionsWorkflowArtifactsSnapshot>(error).toTrace(startedAt)
            }
            artifactsByRunId[run.id] = artifacts
        }
        val runArtifacts = runs.map { run ->
            GitHubActionsRunArtifacts(
                run = run,
                artifacts = artifactsByRunId[run.id].orEmpty()
            )
        }
        return Result.success(
            GitHubActionsWorkflowArtifactsSnapshot(
                owner = owner,
                repo = repo,
                workflowId = workflowId,
                runs = runArtifacts
            )
        ).toTrace(startedAt)
    }

    private fun fetchRunArtifactsForRuns(
        owner: String,
        repo: String,
        runs: List<GitHubActionsWorkflowRun>,
        limit: Int
    ): List<Pair<GitHubActionsWorkflowRun, Result<List<GitHubActionsArtifact>>>> {
        if (runs.isEmpty()) return emptyList()
        val concurrency = runs.size.coerceAtMost(MAX_ARTIFACT_FETCH_CONCURRENCY)
        if (concurrency <= 1) {
            return runs.map { run ->
                run to fetchRunArtifacts(owner = owner, repo = repo, runId = run.id, limit = limit).result
            }
        }
        val executor = Executors.newFixedThreadPool(concurrency)
        return try {
            runs.map { run ->
                executor.submit<Pair<GitHubActionsWorkflowRun, Result<List<GitHubActionsArtifact>>>> {
                    run to fetchRunArtifacts(owner = owner, repo = repo, runId = run.id, limit = limit).result
                }
            }.map { future ->
                future.get()
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun fetchNightlyCompatibleWorkflowArtifactSnapshotFromPublicApi(
        owner: String,
        repo: String,
        workflowId: String,
        runLimit: Int,
        artifactsPerRun: Int,
        artifactRunLimit: Int,
        branch: String,
        event: String,
        status: String,
        actor: String,
        created: String,
        headSha: String,
        excludePullRequests: Boolean
    ): Result<GitHubActionsWorkflowArtifactsSnapshot> = runCatching {
        val workflow = findPublicApiWorkflowForNightly(owner, repo, workflowId).getOrThrow()
        val runs = fetchJson(
            url = buildWorkflowRunsUrl(
                owner = owner,
                repo = repo,
                workflowId = workflow.id.toString(),
                limit = runLimit,
                branch = branch,
                event = event,
                status = status.nightlyPublicApiRunStatus(),
                actor = actor,
                created = created,
                headSha = headSha,
                excludePullRequests = excludePullRequests
            ),
            cacheTtlMillis = ACTIONS_RUNS_CACHE_TTL_MS
        ).getOrThrow().let(::parseWorkflowRuns)
        val artifactRuns = runs.take(artifactRunLimit.coerceAtLeast(0))
        val artifactsByRunId = mutableMapOf<Long, List<GitHubActionsArtifact>>()
        artifactRuns.forEach { run ->
            val artifacts = fetchJson(
                url = buildRunArtifactsUrl(owner, repo, run.id, artifactsPerRun),
                cacheTtlMillis = ACTIONS_ARTIFACT_CACHE_TTL_MS
            ).getOrThrow()
                .let { json -> parseArtifacts(json, fallbackWorkflowRunId = run.id) }
                .map { artifact ->
                    artifact.copy(
                        archiveDownloadUrl = buildNightlyRunArtifactDownloadUrl(
                            owner = owner,
                            repo = repo,
                            runId = run.id,
                            artifactName = artifact.name
                        )
                    )
                }
            artifactsByRunId[run.id] = artifacts
        }
        GitHubActionsWorkflowArtifactsSnapshot(
            owner = owner,
            repo = repo,
            workflowId = workflow.id.toString(),
            runs = runs.map { run ->
                GitHubActionsRunArtifacts(
                    run = run,
                    artifacts = artifactsByRunId[run.id].orEmpty()
                )
            }
        )
    }

    private fun findPublicApiWorkflowForNightly(
        owner: String,
        repo: String,
        workflowId: String
    ): Result<GitHubActionsWorkflow> = runCatching {
        workflowId.trim().toLongOrNull()?.takeIf { it > 0L }?.let { id ->
            return@runCatching GitHubActionsWorkflow(
                id = id,
                name = id.toString(),
                path = workflowId.trim()
            )
        }
        val workflows = fetchJson(
            url = buildWorkflowsUrl(owner, repo, DEFAULT_WORKFLOW_LIMIT),
            cacheTtlMillis = ACTIONS_METADATA_CACHE_TTL_MS
        ).getOrThrow().let(::parseWorkflows)
        selectPublicApiWorkflowForNightly(workflows, workflowId)
            ?: error("GitHub public API found no matching workflow: $workflowId")
    }

    private fun selectPublicApiWorkflowForNightly(
        workflows: List<GitHubActionsWorkflow>,
        workflowId: String
    ): GitHubActionsWorkflow? {
        val lookup = workflowId.trim()
            .substringBefore('?')
            .trim()
        val lookupFile = lookup.substringAfterLast('/').trim()
        val lookupPath = lookup.trimStart('/')
        return workflows.firstOrNull { workflow -> workflow.id.toString() == lookup } ?:
            workflows.firstOrNull { workflow -> workflow.path.equals(lookupPath, ignoreCase = true) } ?:
            workflows.firstOrNull { workflow ->
                workflow.path.substringAfterLast('/').equals(lookupFile, ignoreCase = true)
            } ?:
            workflows.firstOrNull { workflow -> workflow.name.equals(lookup, ignoreCase = true) }
    }

    fun resolveArtifactDownloadUrl(
        artifact: GitHubActionsArtifact,
        owner: String = "",
        repo: String = "",
        preferApiTokenRedirect: Boolean = false
    ): Result<GitHubActionsArtifactDownloadResolution> {
        if (useNightlyLink) {
            val nightlyResult = nightlyRepository.resolveArtifactDownloadUrl(
                artifact = artifact,
                owner = owner,
                repo = repo
            )
            val requiresApiRedirect = artifact.requiresApiBackedNightlyDownload()
            if (
                !preferApiTokenRedirect ||
                sanitizedToken.isBlank() ||
                owner.isBlank() ||
                repo.isBlank() ||
                artifact.id <= 0L
            ) {
                if (requiresApiRedirect) {
                    return Result.failure(IllegalStateException(buildNightlyRawArtifactDownloadMessage(artifact.name)))
                }
                return nightlyResult
            }
            return resolveArtifactDownloadUrl(buildArtifactDownloadUrl(owner, repo, artifact.id))
                .map { resolvedUrl ->
                    GitHubActionsArtifactDownloadResolution(
                        artifactId = artifact.id,
                        downloadUrl = resolvedUrl
                    )
                }
                .recoverCatching { error ->
                    if (requiresApiRedirect) throw error
                    nightlyResult.getOrThrow()
                }
        }
        if (sanitizedToken.isBlank()) {
            return Result.failure(IllegalStateException("A GitHub token is required to download Actions artifacts"))
        }
        val url = artifact.archiveDownloadUrl.trim().ifBlank {
            if (owner.isBlank() || repo.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("The artifact is missing a download URL and repository information")
                )
            }
            buildArtifactDownloadUrl(owner, repo, artifact.id)
        }
        return resolveArtifactDownloadUrl(url).map { resolvedUrl ->
            GitHubActionsArtifactDownloadResolution(
                artifactId = artifact.id,
                downloadUrl = resolvedUrl
            )
        }
    }

    fun resolveArtifactShareUrl(
        artifact: GitHubActionsArtifact,
        owner: String = "",
        repo: String = ""
    ): Result<GitHubActionsArtifactDownloadResolution> {
        if (useNightlyLink) {
            return nightlyRepository.resolveArtifactDownloadUrl(
                artifact = artifact,
                owner = owner,
                repo = repo
            )
        }
        return resolveArtifactDownloadUrl(
            artifact = artifact,
            owner = owner,
            repo = repo
        )
    }

    internal fun parseWorkflows(json: String): List<GitHubActionsWorkflow> {
        val root = JSONObject(json)
        val array = root.optJSONArray("workflows") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val workflow = array.optJSONObject(index) ?: continue
                val id = workflow.optLong("id", 0L).takeIf { it > 0L } ?: continue
                add(
                    GitHubActionsWorkflow(
                        id = id,
                        nodeId = workflow.optString("node_id").trim(),
                        name = workflow.optString("name").trim(),
                        path = workflow.optString("path").trim(),
                        state = workflow.optString("state").trim(),
                        htmlUrl = workflow.optString("html_url").trim(),
                        badgeUrl = workflow.optString("badge_url").trim(),
                        createdAtMillis = workflow.optString("created_at").parseIsoInstantOrNull(),
                        updatedAtMillis = workflow.optString("updated_at").parseIsoInstantOrNull()
                    )
                )
            }
        }.sortedWith(
            compareBy<GitHubActionsWorkflow> { it.state != "active" }
                .thenBy { it.name.lowercase() }
                .thenBy { it.path.lowercase() }
        )
    }

    internal fun parseRepositoryInfo(
        json: String,
        fallbackOwner: String,
        fallbackRepo: String
    ): GitHubActionsRepositoryInfo {
        val root = JSONObject(json)
        return GitHubActionsRepositoryInfo(
            owner = root.optJSONObject("owner")
                ?.optString("login")
                ?.trim()
                .orEmpty()
                .ifBlank { fallbackOwner },
            repo = root.optString("name").trim().ifBlank { fallbackRepo },
            fullName = root.optString("full_name").trim(),
            defaultBranch = root.optString("default_branch").trim()
        )
    }

    internal fun parseWorkflowRuns(json: String): List<GitHubActionsWorkflowRun> {
        val root = JSONObject(json)
        val array = root.optJSONArray("workflow_runs") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val run = array.optJSONObject(index) ?: continue
                parseWorkflowRunObject(run)?.let(::add)
            }
        }.sortedWith(
            compareByDescending<GitHubActionsWorkflowRun> { it.createdAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.id }
        )
    }

    internal fun parseWorkflowRun(json: String): GitHubActionsWorkflowRun {
        val root = JSONObject(json)
        return parseWorkflowRunObject(root)
            ?: throw IllegalArgumentException("workflow run payload missing id")
    }

    private fun parseWorkflowRunObject(run: JSONObject): GitHubActionsWorkflowRun? {
        val id = run.optLong("id", 0L).takeIf { it > 0L } ?: return null
        val actor = run.optJSONObject("actor")
        val triggeringActor = run.optJSONObject("triggering_actor")
        val repository = run.optJSONObject("repository")
        val headRepository = run.optJSONObject("head_repository")
        val pullRequests = run.optJSONArray("pull_requests")
        return GitHubActionsWorkflowRun(
            id = id,
            name = run.optString("name").trim(),
            displayTitle = run.optString("display_title").trim(),
            workflowId = run.optLong("workflow_id", 0L),
            workflowName = run.optString("workflow_name").trim(),
            runNumber = run.optLong("run_number", 0L),
            runAttempt = run.optInt("run_attempt", 0),
            event = run.optString("event").trim(),
            status = run.optString("status").trim(),
            conclusion = run.optString("conclusion").trim(),
            headBranch = run.optString("head_branch").trim(),
            headSha = run.optString("head_sha").trim(),
            htmlUrl = run.optString("html_url").trim(),
            artifactsUrl = run.optString("artifacts_url").trim(),
            actorLogin = actor?.optString("login").orEmpty().trim(),
            triggeringActorLogin = triggeringActor?.optString("login").orEmpty().trim(),
            repositoryFullName = repository?.optString("full_name").orEmpty().trim(),
            headRepositoryFullName = headRepository?.optString("full_name").orEmpty().trim(),
            headRepositoryFork = headRepository?.optBoolean("fork", false) ?: false,
            pullRequestCount = pullRequests?.length() ?: 0,
            checkSuiteId = run.optLong("check_suite_id", 0L),
            createdAtMillis = run.optString("created_at").parseIsoInstantOrNull(),
            runStartedAtMillis = run.optString("run_started_at").parseIsoInstantOrNull(),
            updatedAtMillis = run.optString("updated_at").parseIsoInstantOrNull()
        )
    }

    internal fun parseArtifacts(
        json: String,
        fallbackWorkflowRunId: Long = 0L
    ): List<GitHubActionsArtifact> {
        val root = JSONObject(json)
        val array = root.optJSONArray("artifacts") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val artifact = array.optJSONObject(index) ?: continue
                val id = artifact.optLong("id", 0L).takeIf { it > 0L } ?: continue
                val workflowRun = artifact.optJSONObject("workflow_run")
                add(
                    GitHubActionsArtifact(
                        id = id,
                        nodeId = artifact.optString("node_id").trim(),
                        name = artifact.optString("name").trim(),
                        sizeBytes = artifact.optLong("size_in_bytes", 0L),
                        expired = artifact.optBoolean("expired", false),
                        digest = artifact.optString("digest").trim(),
                        archiveDownloadUrl = artifact.optString("archive_download_url").trim(),
                        workflowRunId = workflowRun?.optLong("id", 0L)
                            ?.takeIf { it > 0L }
                            ?: fallbackWorkflowRunId,
                        workflowRunHeadBranch = workflowRun?.optString("head_branch").orEmpty().trim(),
                        workflowRunHeadSha = workflowRun?.optString("head_sha").orEmpty().trim(),
                        createdAtMillis = artifact.optString("created_at").parseIsoInstantOrNull(),
                        updatedAtMillis = artifact.optString("updated_at").parseIsoInstantOrNull(),
                        expiresAtMillis = artifact.optString("expires_at").parseIsoInstantOrNull()
                    )
                )
            }
        }.sortedWith(
            compareBy<GitHubActionsArtifact> { it.expired }
                .thenByDescending { it.updatedAtMillis ?: Long.MIN_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun resolveArtifactDownloadUrl(url: String): Result<String> = runCatching {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $sanitizedToken")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", GITHUB_USER_AGENT)
            .build()
        noRedirectClient.newCall(request).execute().use { response ->
            when {
                response.isRedirect -> response.header("Location").orEmpty().ifBlank {
                    error("GitHub Actions artifact download returned no redirect URL")
                }
                response.isSuccessful -> response.request.url.toString()
                else -> error(buildErrorMessage(response, response.body.string()))
            }
        }
    }

    private fun fetchJson(
        url: String,
        cacheTtlMillis: Long = 0L
    ): Result<String> = runCatching {
        val cacheKey = jsonResponseCacheKey(url)
        if (cacheTtlMillis > 0L) {
            cachedValue(jsonResponseCache[cacheKey], cacheTtlMillis)?.let { cached ->
                return@runCatching cached
            }
        }
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", GITHUB_USER_AGENT)
        if (sanitizedToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $sanitizedToken")
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            val bodyText = response.body.string()
            if (!response.isSuccessful) {
                error(buildErrorMessage(response, bodyText))
            }
            if (cacheTtlMillis > 0L) {
                putCachedValue(jsonResponseCache, cacheKey, bodyText)
            }
            bodyText
        }
    }

    private fun jsonResponseCacheKey(url: String): String {
        return listOf(
            authCachePartition(),
            apiBaseUrl.trimEnd('/'),
            url
        ).joinToString("|")
    }

    private fun authCachePartition(): String {
        return sanitizedToken
            .takeIf { it.isNotBlank() }
            ?.let { token -> "token:${stableHash(token)}" }
            ?: "guest"
    }

    private fun stableHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun <T> cachedValue(entry: CachedValue<T>?, ttlMillis: Long): T? {
        if (entry == null) return null
        val ageMillis = System.currentTimeMillis() - entry.fetchedAtMillis
        return entry.value.takeIf { ageMillis in 0 until ttlMillis }
    }

    private fun <T> putCachedValue(
        cache: ConcurrentHashMap<String, CachedValue<T>>,
        key: String,
        value: T
    ) {
        if (cache.size >= ACTIONS_CACHE_MAX_ENTRIES) {
            cache.clear()
        }
        cache[key] = CachedValue(value, System.currentTimeMillis())
    }

    private fun <T> Result<T>.toTrace(startedAt: Long): GitHubStrategyLoadTrace<T> {
        return GitHubStrategyLoadTrace(
            result = this,
            fromCache = false,
            elapsedMs = System.currentTimeMillis() - startedAt,
            authMode = authMode
        )
    }

    private fun requireActionsApiToken(): Result<Unit> {
        return if (requireApiTokenForApiStrategy && sanitizedToken.isBlank()) {
            Result.failure(IllegalStateException("GitHub Actions API Token mode requires a token"))
        } else {
            Result.success(Unit)
        }
    }

    private fun buildRepositoryUrl(owner: String, repo: String): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo"
    }

    private fun buildWorkflowsUrl(owner: String, repo: String, limit: Int): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/actions/workflows?per_page=${limit.coerceIn(1, 100)}"
    }

    private fun buildWorkflowRunsUrl(
        owner: String,
        repo: String,
        workflowId: String,
        limit: Int,
        branch: String,
        event: String,
        status: String,
        actor: String,
        created: String,
        headSha: String,
        excludePullRequests: Boolean
    ): String {
        val encodedWorkflowId = workflowId.trim().urlEncode()
        val query = buildList {
            add("per_page=${limit.coerceIn(1, 100)}")
            branch.trim().takeIf { it.isNotBlank() }?.let { add("branch=${it.urlEncode()}") }
            event.trim().takeIf { it.isNotBlank() }?.let { add("event=${it.urlEncode()}") }
            status.trim().takeIf { it.isNotBlank() }?.let { add("status=${it.urlEncode()}") }
            actor.trim().takeIf { it.isNotBlank() }?.let { add("actor=${it.urlEncode()}") }
            created.trim().takeIf { it.isNotBlank() }?.let { add("created=${it.urlEncode()}") }
            headSha.trim().takeIf { it.isNotBlank() }?.let { add("head_sha=${it.urlEncode()}") }
            if (excludePullRequests) add("exclude_pull_requests=true")
        }.joinToString("&")
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/actions/workflows/$encodedWorkflowId/runs?$query"
    }

    private fun buildWorkflowRunUrl(owner: String, repo: String, runId: Long): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/actions/runs/$runId"
    }

    private fun buildRunArtifactsUrl(owner: String, repo: String, runId: Long, limit: Int): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/actions/runs/$runId/artifacts?per_page=${limit.coerceIn(1, 100)}"
    }

    private fun buildArtifactDownloadUrl(owner: String, repo: String, artifactId: Long): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
    }

    private fun buildNightlyRunArtifactDownloadUrl(
        owner: String,
        repo: String,
        runId: Long,
        artifactName: String
    ): String {
        return "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/" +
            "actions/runs/$runId/${artifactName.urlEncode()}.zip"
    }

    private fun buildErrorMessage(response: Response, bodyText: String): String {
        val apiMessage = runCatching {
            JSONObject(bodyText).optString("message").trim()
        }.getOrDefault("")
        val rateRemaining = response.header("X-RateLimit-Remaining").orEmpty()
        val looksRateLimited = response.code == 429 ||
            rateRemaining == "0" ||
            apiMessage.contains("rate limit", ignoreCase = true)
        return when (response.code) {
            401 -> "GitHub Actions token is invalid or expired"
            403, 429 -> when {
                looksRateLimited && authMode == GitHubApiAuthMode.Guest ->
                    "GitHub Actions guest API is rate limited. Try again later or enter a token."
                looksRateLimited -> "GitHub Actions API is rate limited"
                else -> "GitHub Actions API access was denied${apiMessage.toErrorSuffix()}"
            }
            404 -> "The repository or GitHub Actions resource does not exist, or the current token lacks access"
            410 -> "The GitHub Actions artifact has expired"
            else -> "GitHub Actions request failed (HTTP ${response.code}${apiMessage.toErrorSuffix(", ")})"
        }
    }

    private fun String.toErrorSuffix(prefix: String = ": "): String {
        return takeIf { it.isNotBlank() }?.let { "$prefix$it" }.orEmpty()
    }

    private fun String.nightlyPublicApiRunStatus(): String {
        val normalized = trim()
        return when {
            normalized.isBlank() -> "success"
            normalized.equals("completed", ignoreCase = true) -> "success"
            else -> normalized
        }
    }

    private fun GitHubActionsWorkflowArtifactsSnapshot.requiresPublicApiMetadataForNightlyDownload(): Boolean {
        return artifacts.any { artifact ->
            artifact.requiresApiBackedNightlyDownload() &&
                (
                    artifact.sizeBytes <= 0L ||
                        artifact.digest.isBlank() ||
                        artifact.workflowRunHeadSha.isBlank()
                    )
        }
    }

    private fun GitHubActionsArtifact.requiresApiBackedNightlyDownload(): Boolean {
        val normalizedName = name.trim().lowercase()
        if (normalizedName.isBlank()) return false
        val normalizedNightlyBaseUrl = nightlyLinkBaseUrl.trimEnd('/')
        val nightlyUrl = archiveDownloadUrl.startsWith(normalizedNightlyBaseUrl, ignoreCase = true) ||
            archiveDownloadUrl.contains("nightly.link", ignoreCase = true)
        val rawAndroidArtifact = RAW_ANDROID_ARTIFACT_EXTENSIONS.any { extension ->
            normalizedName.endsWith(extension)
        }
        return nightlyUrl && rawAndroidArtifact
    }

    private fun buildNightlyRawArtifactDownloadMessage(artifactName: String): String {
        val name = artifactName.trim().ifBlank { "artifact" }
        return "nightly.link cannot directly download raw APK artifact: $name. " +
            "Enter a GitHub API Token or switch to the GitHub API Token path."
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
    }

    private fun String.parseIsoInstantOrNull(): Long? {
        return runCatching {
            if (isBlank()) null else Instant.parse(this).toEpochMilli()
        }.getOrNull()
    }

    companion object {
        private const val DEFAULT_WORKFLOW_LIMIT = 50
        private const val DEFAULT_RUN_LIMIT = 20
        private const val DEFAULT_ARTIFACT_LIMIT = 100
        private const val MAX_ARTIFACT_FETCH_CONCURRENCY = 4
        private const val ACTIONS_METADATA_CACHE_TTL_MS = 90_000L
        private const val ACTIONS_RUNS_CACHE_TTL_MS = 10_000L
        private const val ACTIONS_ARTIFACT_CACHE_TTL_MS = 45_000L
        private const val ACTIONS_CACHE_MAX_ENTRIES = 160
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val GITHUB_USER_AGENT = "KeiOS-App/1.0 (Android)"
        private const val DEFAULT_GITHUB_API_BASE_URL = "https://api.github.com"
        private const val DEFAULT_GITHUB_HTML_BASE_URL = "https://github.com"
        private const val DEFAULT_NIGHTLY_LINK_BASE_URL = "https://nightly.link"
        private val RAW_ANDROID_ARTIFACT_EXTENSIONS = setOf(".apk", ".apks", ".aab")

        private data class CachedValue<T>(
            val value: T,
            val fetchedAtMillis: Long
        )

        private val jsonResponseCache = ConcurrentHashMap<String, CachedValue<String>>()

        private val githubClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .callTimeout(18, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(14, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .fastFallback(true)
                .build()
        }

        fun fromLookupConfig(config: GitHubLookupConfig): GitHubActionsRepository {
            return GitHubActionsRepository(
                apiToken = config.apiToken,
                actionsStrategy = config.actionsStrategy,
                requireApiTokenForApiStrategy = true
            )
        }
    }
}
