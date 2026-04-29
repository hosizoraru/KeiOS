package os.kei.ui.page.main.github.page.action

import android.app.DownloadManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.core.intent.SafeExternalIntents
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsArtifactSelectionOptions
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunSelectionOptions
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.feature.github.model.GitHubActionsWorkflowSelectionOptions
import os.kei.feature.github.model.GitHubTrackedApp

internal class GitHubActionsActions(
    private val env: GitHubPageActionEnvironment
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state
    private val repository get() = env.repository
    private val systemDmOption get() = env.systemDmOption

    fun openActionsSheet(item: GitHubTrackedApp) {
        if (state.trackedItems.none { it.id == item.id }) return
        state.resetActionsSheetState()
        state.actionsTargetItem = item
        state.showActionsSheet = true
        scope.launch {
            loadActionsOverview(item = item, preferredWorkflowId = null)
        }
    }

    fun closeActionsSheet() {
        state.dismissActionsSheet()
    }

    fun refreshActionsSheet() {
        val item = state.actionsTargetItem ?: return
        val selectedWorkflowId = state.actionsSelectedWorkflowId
        scope.launch {
            loadActionsOverview(item = item, preferredWorkflowId = selectedWorkflowId)
        }
    }

    fun selectActionsWorkflow(workflowId: Long) {
        val item = state.actionsTargetItem ?: return
        val workflow = state.actionsWorkflows
            .firstOrNull { it.workflow.id == workflowId }
            ?.workflow
            ?: return
        state.actionsSelectedWorkflowId = workflow.id
        state.actionsRunLimit = DEFAULT_RUN_LIMIT
        state.actionsSelectedRunId = null
        state.actionsRunWatchJob?.cancel()
        scope.launch {
            loadWorkflowSnapshot(
                item = item,
                workflow = workflow,
                preferredRunId = null
            )
        }
    }

    fun loadMoreActionsRuns() {
        val item = state.actionsTargetItem ?: return
        val workflow = selectedWorkflowMatch()?.workflow ?: return
        val currentRunId = state.actionsSelectedRunId
        val nextLimit = (state.actionsRunLimit + RUN_PAGE_SIZE).coerceAtMost(MAX_RUN_LIMIT)
        if (nextLimit <= state.actionsRunLimit) return
        state.actionsRunLimit = nextLimit
        scope.launch {
            loadWorkflowSnapshot(
                item = item,
                workflow = workflow,
                preferredRunId = currentRunId
            )
        }
    }

    fun selectActionsRun(runId: Long) {
        if (state.actionsRuns.none { it.runArtifacts.run.id == runId }) return
        state.actionsSelectedRunId = runId
        scheduleSelectedRunWatch()
    }

    fun refreshActionsRunStatus(runId: Long) {
        scope.launch {
            refreshRunStatus(runId = runId, showToast = true)
        }
    }

    fun downloadActionsArtifact(runId: Long, artifactId: Long) {
        val item = state.actionsTargetItem ?: return
        val workflowMatch = selectedWorkflowMatch() ?: return
        val runMatch = state.actionsRuns.firstOrNull { it.runArtifacts.run.id == runId } ?: return
        val artifactMatch = runMatch.artifactMatches.firstOrNull { it.artifact.id == artifactId } ?: return
        val artifact = artifactMatch.artifact
        if (state.lookupConfig.apiToken.trim().isBlank()) {
            env.toast(R.string.github_actions_toast_token_required)
            return
        }
        if (artifact.expired) {
            env.toast(R.string.github_actions_toast_artifact_expired)
            return
        }
        if (!runMatch.traits.completed) {
            env.toast(R.string.github_actions_toast_wait_run_completed)
            return
        }
        if (state.actionsArtifactDownloadLoadingId == artifact.id) return
        scope.launch {
            state.actionsArtifactDownloadLoadingId = artifact.id
            try {
                val resolution = repository.resolveGitHubActionsArtifactDownloadUrl(
                    artifact = artifact,
                    owner = item.owner,
                    repo = item.repo,
                    lookupConfig = state.lookupConfig
                ).getOrThrow()
                val resolvedUrl = SafeExternalIntents.httpsExternalUrlOrNull(resolution.downloadUrl)
                    ?: error(context.getString(R.string.github_actions_error_download_url_invalid))
                val fileName = artifactArchiveFileName(artifact)
                if (openResolvedArtifactDownloadUrl(resolvedUrl, fileName)) {
                    val record = repository.buildGitHubActionsDownloadRecord(
                        owner = item.owner,
                        repo = item.repo,
                        workflow = workflowMatch.workflow,
                        run = runMatch.runArtifacts.run,
                        artifact = artifact,
                        sourceTrackId = item.id,
                        packageName = item.packageName
                    )
                    repository.recordGitHubActionsArtifactDownload(record)
                    state.actionsDownloadHistory = repository.loadGitHubActionsDownloadHistory(
                        owner = item.owner,
                        repo = item.repo
                    )
                    reselectActionsMatchesAfterHistoryChange()
                    env.toast(R.string.github_actions_toast_download_started)
                }
            } catch (error: Throwable) {
                env.toast(
                    context.getString(
                        R.string.github_actions_toast_download_failed,
                        error.message ?: error.javaClass.simpleName
                    )
                )
            } finally {
                state.actionsArtifactDownloadLoadingId = null
            }
        }
    }

    fun openSelectedActionsRun() {
        val run = state.actionsRuns
            .firstOrNull { it.runArtifacts.run.id == state.actionsSelectedRunId }
            ?.runArtifacts
            ?.run
            ?: return
        val url = run.htmlUrl.trim()
        if (url.isBlank()) return
        if (!SafeExternalIntents.startBrowsableUrl(context, url)) {
            env.toast(R.string.github_error_open_link)
        }
    }

    private suspend fun loadActionsOverview(
        item: GitHubTrackedApp,
        preferredWorkflowId: Long?
    ) {
        state.actionsRunWatchJob?.cancel()
        state.actionsLoading = true
        state.actionsRunsLoading = false
        state.actionsError = null
        state.actionsDefaultBranch = ""
        state.actionsRawWorkflows = emptyList()
        state.actionsWorkflowSignals = emptyMap()
        state.actionsWorkflows = emptyList()
        state.actionsSelectedWorkflowId = null
        state.actionsSnapshot = null
        state.actionsRuns = emptyList()
        state.actionsSelectedRunId = null
        state.actionsRunTrackingPlans = emptyMap()
        state.actionsStatusRefreshingRunIds.clear()
        try {
            val lookupConfig = state.lookupConfig
            val history = repository.loadGitHubActionsDownloadHistory(
                owner = item.owner,
                repo = item.repo
            )
            if (!isCurrentTarget(item)) return
            state.actionsDownloadHistory = history

            val infoTrace = repository.fetchGitHubActionsRepositoryInfo(
                owner = item.owner,
                repo = item.repo,
                lookupConfig = lookupConfig
            )
            val info = infoTrace.result.getOrThrow()
            if (!isCurrentTarget(item)) return
            state.actionsDefaultBranch = info.defaultBranch
            state.actionsAuthMode = infoTrace.authMode

            val workflowsTrace = repository.fetchGitHubActionsWorkflows(
                owner = item.owner,
                repo = item.repo,
                lookupConfig = lookupConfig
            )
            val workflows = workflowsTrace.result.getOrThrow()
            if (!isCurrentTarget(item)) return
            state.actionsAuthMode = workflowsTrace.authMode ?: state.actionsAuthMode

            val signalCandidateWorkflows = selectWorkflowSignalCandidates(
                workflows = workflows,
                history = history
            )
            val signalsTrace = if (signalCandidateWorkflows.isEmpty()) {
                null
            } else {
                repository.fetchGitHubActionsWorkflowArtifactSignals(
                    owner = item.owner,
                    repo = item.repo,
                    workflows = signalCandidateWorkflows,
                    lookupConfig = lookupConfig,
                    runLimit = SIGNAL_RUN_LIMIT,
                    artifactsPerRun = SIGNAL_ARTIFACT_LIMIT,
                    defaultBranch = info.defaultBranch
                )
            }
            val signals = signalsTrace?.result?.getOrElse { emptyMap() }.orEmpty()
            if (!isCurrentTarget(item)) return
            state.actionsAuthMode = signalsTrace?.authMode ?: state.actionsAuthMode
            state.actionsRawWorkflows = workflows
            state.actionsWorkflowSignals = signals
            state.actionsWorkflows = selectWorkflows(
                workflows = workflows,
                signals = signals,
                history = history
            )

            val selectedWorkflow = preferredWorkflowId
                ?.let { id -> state.actionsWorkflows.firstOrNull { it.workflow.id == id } }
                ?: state.actionsWorkflows.firstOrNull()
            if (selectedWorkflow != null) {
                state.actionsSelectedWorkflowId = selectedWorkflow.workflow.id
                state.actionsRunLimit = DEFAULT_RUN_LIMIT
                loadWorkflowSnapshot(
                    item = item,
                    workflow = selectedWorkflow.workflow,
                    preferredRunId = null
                )
            }
        } catch (error: Throwable) {
            if (isCurrentTarget(item)) {
                state.actionsError = error.message ?: error.javaClass.simpleName
            }
        } finally {
            if (isCurrentTarget(item)) {
                state.actionsLoading = false
            }
        }
    }

    private suspend fun loadWorkflowSnapshot(
        item: GitHubTrackedApp,
        workflow: GitHubActionsWorkflow,
        preferredRunId: Long?
    ) {
        state.actionsRunWatchJob?.cancel()
        state.actionsRunsLoading = true
        state.actionsError = null
        state.actionsSnapshot = null
        state.actionsRuns = emptyList()
        state.actionsSelectedRunId = null
        state.actionsRunTrackingPlans = emptyMap()
        state.actionsStatusRefreshingRunIds.clear()
        try {
            val snapshotTrace = repository.fetchGitHubActionsWorkflowArtifactSnapshot(
                owner = item.owner,
                repo = item.repo,
                workflowId = workflow.id.toString(),
                lookupConfig = state.lookupConfig,
                runLimit = state.actionsRunLimit.coerceIn(DEFAULT_RUN_LIMIT, MAX_RUN_LIMIT),
                artifactsPerRun = ARTIFACTS_PER_RUN
            )
            val snapshot = snapshotTrace.result.getOrThrow()
            if (!isCurrentTarget(item) || state.actionsSelectedWorkflowId != workflow.id) return
            val runMatches = selectRunsForSnapshot(
                workflow = workflow,
                snapshot = snapshot,
                history = state.actionsDownloadHistory
            )
            state.actionsAuthMode = snapshotTrace.authMode ?: state.actionsAuthMode
            state.actionsSnapshot = snapshot
            state.actionsRuns = runMatches
            state.actionsRunTrackingPlans = buildTrackingPlans(runMatches)
            state.actionsSelectedRunId = preferredRunId
                ?.takeIf { runId -> runMatches.any { it.runArtifacts.run.id == runId } }
                ?: runMatches.firstOrNull()?.runArtifacts?.run?.id
            scheduleSelectedRunWatch()
        } catch (error: Throwable) {
            if (isCurrentTarget(item) && state.actionsSelectedWorkflowId == workflow.id) {
                state.actionsError = error.message ?: error.javaClass.simpleName
            }
        } finally {
            if (isCurrentTarget(item) && state.actionsSelectedWorkflowId == workflow.id) {
                state.actionsRunsLoading = false
            }
        }
    }

    private suspend fun refreshRunStatus(
        runId: Long,
        showToast: Boolean
    ) {
        val item = state.actionsTargetItem ?: return
        val workflow = selectedWorkflowMatch()?.workflow ?: return
        val currentSnapshot = state.actionsSnapshot ?: return
        if (state.actionsStatusRefreshingRunIds[runId] == true) return
        state.actionsStatusRefreshingRunIds[runId] = true
        try {
            val statusTrace = repository.fetchGitHubActionsRunStatusSnapshot(
                owner = item.owner,
                repo = item.repo,
                runId = runId,
                lookupConfig = state.lookupConfig,
                artifactsLimit = 100,
                includeArtifactsWhenCompleted = true
            )
            val statusSnapshot = statusTrace.result.getOrThrow()
            if (!isCurrentTarget(item) || state.actionsSelectedWorkflowId != workflow.id) return
            state.actionsAuthMode = statusTrace.authMode ?: state.actionsAuthMode
            val updatedRunArtifacts = GitHubActionsRunArtifacts(
                run = statusSnapshot.run,
                artifacts = statusSnapshot.artifacts
            )
            val replaced = currentSnapshot.runs.any { it.run.id == runId }
            val updatedRuns = if (replaced) {
                currentSnapshot.runs.map { runArtifacts ->
                    if (runArtifacts.run.id == runId) updatedRunArtifacts else runArtifacts
                }
            } else {
                listOf(updatedRunArtifacts) + currentSnapshot.runs
            }
            val updatedSnapshot = currentSnapshot.copy(runs = updatedRuns)
            val runMatches = selectRunsForSnapshot(
                workflow = workflow,
                snapshot = updatedSnapshot,
                history = state.actionsDownloadHistory
            )
            state.actionsSnapshot = updatedSnapshot
            state.actionsRuns = runMatches
            state.actionsRunTrackingPlans = buildTrackingPlans(runMatches)
            state.actionsSelectedRunId = runId
                .takeIf { id -> runMatches.any { it.runArtifacts.run.id == id } }
                ?: runMatches.firstOrNull()?.runArtifacts?.run?.id
            if (showToast) {
                env.toast(R.string.common_refreshed)
            }
        } catch (error: Throwable) {
            if (showToast) {
                env.toast(
                    context.getString(
                        R.string.github_actions_toast_refresh_run_failed,
                        error.message ?: error.javaClass.simpleName
                    )
                )
            }
        } finally {
            state.actionsStatusRefreshingRunIds.remove(runId)
            scheduleSelectedRunWatch()
        }
    }

    private suspend fun reselectActionsMatchesAfterHistoryChange() {
        val selectedWorkflowId = state.actionsSelectedWorkflowId
        val selectedRunId = state.actionsSelectedRunId
        state.actionsWorkflows = selectWorkflows(
            workflows = state.actionsRawWorkflows,
            signals = state.actionsWorkflowSignals,
            history = state.actionsDownloadHistory
        )
        val workflow = state.actionsWorkflows
            .firstOrNull { it.workflow.id == selectedWorkflowId }
            ?.workflow
            ?: return
        val snapshot = state.actionsSnapshot ?: return
        val runs = selectRunsForSnapshot(
            workflow = workflow,
            snapshot = snapshot,
            history = state.actionsDownloadHistory
        )
        state.actionsRuns = runs
        state.actionsRunTrackingPlans = buildTrackingPlans(runs)
        state.actionsSelectedRunId = selectedRunId
            ?.takeIf { runId -> runs.any { it.runArtifacts.run.id == runId } }
            ?: runs.firstOrNull()?.runArtifacts?.run?.id
    }

    private suspend fun selectWorkflows(
        workflows: List<GitHubActionsWorkflow>,
        signals: Map<Long, os.kei.feature.github.model.GitHubActionsWorkflowArtifactSignal>,
        history: List<os.kei.feature.github.model.GitHubActionsDownloadRecord>
    ): List<GitHubActionsWorkflowMatch> {
        return repository.selectGitHubActionsWorkflows(
            workflows = workflows,
            artifactSignals = signals,
            options = GitHubActionsWorkflowSelectionOptions(
                includeDisabled = false,
                requireArtifacts = false,
                downloadHistory = history
            )
        )
    }

    private suspend fun selectWorkflowSignalCandidates(
        workflows: List<GitHubActionsWorkflow>,
        history: List<os.kei.feature.github.model.GitHubActionsDownloadRecord>
    ): List<GitHubActionsWorkflow> {
        val historyWorkflowIds = history
            .mapNotNull { record -> record.workflowId.takeIf { it > 0L } }
            .toSet()
        val historyWorkflows = workflows.filter { it.id in historyWorkflowIds }
        val preliminaryMatches = selectWorkflows(
            workflows = workflows,
            signals = emptyMap(),
            history = history
        )
        return (historyWorkflows + preliminaryMatches.map { it.workflow })
            .distinctBy { it.id }
            .take(WORKFLOW_SIGNAL_LIMIT)
    }

    private suspend fun selectRunsForSnapshot(
        workflow: GitHubActionsWorkflow,
        snapshot: GitHubActionsWorkflowArtifactsSnapshot,
        history: List<os.kei.feature.github.model.GitHubActionsDownloadRecord>
    ): List<GitHubActionsRunMatch> {
        val artifactOptions = GitHubActionsArtifactSelectionOptions(
            preferredAbis = Build.SUPPORTED_ABIS.toList(),
            aggressiveAbiFiltering = state.lookupConfig.aggressiveApkFiltering,
            downloadHistory = history
        )
        return repository.selectGitHubActionsRuns(
            runs = snapshot.runs,
            workflow = workflow,
            options = GitHubActionsRunSelectionOptions(
                defaultBranch = state.actionsDefaultBranch,
                includePullRequests = true,
                includeNonDefaultBranches = true,
                includeUnsuccessful = true,
                requireArtifacts = false,
                requireAndroidArtifacts = false,
                artifactOptions = artifactOptions,
                downloadHistory = history
            )
        )
    }

    private suspend fun buildTrackingPlans(
        runs: List<GitHubActionsRunMatch>
    ): Map<Long, os.kei.feature.github.model.GitHubActionsRunTrackingPlan> {
        return runs.associate { match ->
            val run = match.runArtifacts.run
            run.id to repository.buildGitHubActionsRunTrackingPlan(run)
        }
    }

    private fun scheduleSelectedRunWatch() {
        state.actionsRunWatchJob?.cancel()
        val runId = state.actionsSelectedRunId ?: return
        val plan = state.actionsRunTrackingPlans[runId] ?: return
        if (!state.showActionsSheet || !plan.pollable) return
        val delayMillis = plan.nextPollDelayMillis.coerceAtLeast(5_000L)
        state.actionsRunWatchJob = scope.launch {
            delay(delayMillis)
            if (!state.showActionsSheet || state.actionsSelectedRunId != runId) return@launch
            refreshRunStatus(runId = runId, showToast = false)
        }
    }

    private fun selectedWorkflowMatch(): GitHubActionsWorkflowMatch? {
        val workflowId = state.actionsSelectedWorkflowId ?: return null
        return state.actionsWorkflows.firstOrNull { it.workflow.id == workflowId }
    }

    private fun isCurrentTarget(item: GitHubTrackedApp): Boolean {
        return state.showActionsSheet && state.actionsTargetItem?.id == item.id
    }

    private fun openResolvedArtifactDownloadUrl(url: String, fileName: String): Boolean {
        val preferredPackage = state.lookupConfig.preferredDownloaderPackage.trim()
        return runCatching {
            when (preferredPackage) {
                systemDmOption.packageName -> {
                    enqueueWithSystemDownloadManager(url = url, fileName = fileName)
                    env.toast(R.string.github_toast_downloader_system_builtin)
                }
                "" -> {
                    require(SafeExternalIntents.startBrowsableUrl(context, url))
                    env.toast(R.string.github_toast_downloader_system_default)
                }
                else -> {
                    require(SafeExternalIntents.startBrowsableUrl(context, url, preferredPackage))
                    env.toast(R.string.github_toast_downloader_selected)
                }
            }
            true
        }.recoverCatching {
            if (preferredPackage.isNotBlank() && preferredPackage != systemDmOption.packageName) {
                require(SafeExternalIntents.startBrowsableUrl(context, url))
                env.toast(R.string.github_toast_downloader_fallback_system)
                true
            } else {
                throw it
            }
        }.getOrElse {
            env.toast(R.string.github_toast_open_downloader_failed)
            false
        }
    }

    private fun enqueueWithSystemDownloadManager(url: String, fileName: String) {
        val safeUrl = SafeExternalIntents.httpsExternalUrlOrNull(url)
            ?: throw IllegalArgumentException("download url must be https")
        val request = DownloadManager.Request(safeUrl.toUri()).apply {
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            setTitle(fileName)
            setDescription(fileName)
            setMimeType("application/zip")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: throw IllegalStateException("download manager unavailable")
        manager.enqueue(request)
    }

    private fun artifactArchiveFileName(artifact: GitHubActionsArtifact): String {
        val baseName = artifact.name
            .trim()
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .ifBlank { "artifact-${artifact.id}" }
        return if (baseName.endsWith(".zip", ignoreCase = true)) baseName else "$baseName.zip"
    }

    companion object {
        private const val DEFAULT_RUN_LIMIT = 6
        private const val RUN_PAGE_SIZE = 6
        private const val MAX_RUN_LIMIT = 30
        private const val ARTIFACTS_PER_RUN = 80
        private const val WORKFLOW_SIGNAL_LIMIT = 8
        private const val SIGNAL_RUN_LIMIT = 2
        private const val SIGNAL_ARTIFACT_LIMIT = 60
    }
}
