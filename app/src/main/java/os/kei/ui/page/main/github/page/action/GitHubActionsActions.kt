package os.kei.ui.page.main.github.page.action

import android.os.Build
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.domain.GitHubActionsBranchSelector
import os.kei.feature.github.domain.GitHubActionsWorkflowSelector
import os.kei.feature.github.model.GitHubActionsArtifactSelectionOptions
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunSelectionOptions
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.feature.github.model.GitHubActionsWorkflowSelectionOptions
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.actions.GitHubActionsUiStateStore
import os.kei.ui.page.main.github.localizedGitHubActionsErrorMessage

internal class GitHubActionsActions(
    private val env: GitHubPageActionEnvironment
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state
    private val actionsRepository get() = env.actionsRepository
    private val artifactActions = GitHubActionsArtifactActions(
        env = env,
        actionsRepository = actionsRepository,
        onDownloadHistoryChanged = { reselectActionsMatchesAfterHistoryChange() }
    )

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
        state.actionsWorkflowManuallySelected = true
        state.actionsRunLimit = DEFAULT_RUN_LIMIT
        state.actionsSelectedBranch = ""
        state.actionsBranchManuallySelected = false
        state.actionsBranchOptions = emptyList()
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
                preferredRunId = currentRunId,
                keepCurrentRunsWhileLoading = true
            )
        }
    }

    fun selectActionsBranch(branch: String) {
        val item = state.actionsTargetItem ?: return
        val workflow = selectedWorkflowMatch()?.workflow ?: return
        val normalized = branch.trim()
        if (normalized.isBlank()) return
        if (state.actionsSelectedBranch.equals(normalized, ignoreCase = true)) return
        state.actionsSelectedBranch = normalized
        state.actionsBranchManuallySelected = true
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

    fun setBranchesExpanded(value: Boolean) {
        state.actionsBranchesExpanded = value
        GitHubActionsUiStateStore.setBranchesExpanded(value)
    }

    fun setWorkflowsExpanded(value: Boolean) {
        state.actionsWorkflowsExpanded = value
        GitHubActionsUiStateStore.setWorkflowsExpanded(value)
    }

    fun setRunsExpanded(value: Boolean) {
        state.actionsRunsExpanded = value
        GitHubActionsUiStateStore.setRunsExpanded(value)
    }

    fun selectActionsRun(runId: Long) {
        val item = state.actionsTargetItem ?: return
        val workflow = selectedWorkflowMatch()?.workflow ?: return
        if (state.actionsRuns.none { it.runArtifacts.run.id == runId }) return
        state.actionsSelectedRunId = runId
        scheduleSelectedRunWatch()
        loadRunArtifactsIfNeeded(
            item = item,
            workflowId = workflow.id,
            runId = runId
        )
    }

    fun refreshActionsRunStatus(runId: Long) {
        scope.launch {
            refreshRunStatus(runId = runId, showToast = true)
        }
    }

    fun downloadActionsArtifact(runId: Long, artifactId: Long) {
        artifactActions.downloadActionsArtifact(runId = runId, artifactId = artifactId)
    }

    fun shareActionsArtifact(runId: Long, artifactId: Long) {
        artifactActions.shareActionsArtifact(runId = runId, artifactId = artifactId)
    }

    fun openSelectedActionsRun() {
        artifactActions.openSelectedActionsRun()
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
        state.actionsSelectedBranch = ""
        state.actionsBranchManuallySelected = false
        state.actionsBranchOptions = emptyList()
        state.actionsRawWorkflows = emptyList()
        state.actionsWorkflowSignals = emptyMap()
        state.actionsWorkflows = emptyList()
        state.actionsSelectedWorkflowId = null
        state.actionsWorkflowManuallySelected = false
        state.actionsSnapshot = null
        state.actionsRuns = emptyList()
        state.actionsSelectedRunId = null
        state.actionsRunTrackingPlans = emptyMap()
        state.actionsStatusRefreshingRunIds.clear()
        try {
            val lookupConfig = state.lookupConfig
            val (history, infoTrace, workflowsTrace) = coroutineScope {
                val historyDeferred = async {
                    actionsRepository.loadGitHubActionsDownloadHistory(
                        owner = item.owner,
                        repo = item.repo
                    )
                }
                val infoDeferred = async {
                    actionsRepository.fetchGitHubActionsRepositoryInfo(
                        owner = item.owner,
                        repo = item.repo,
                        lookupConfig = lookupConfig
                    )
                }
                val workflowsDeferred = async {
                    actionsRepository.fetchGitHubActionsWorkflows(
                        owner = item.owner,
                        repo = item.repo,
                        lookupConfig = lookupConfig
                    )
                }
                Triple(
                    historyDeferred.await(),
                    infoDeferred.await(),
                    workflowsDeferred.await()
                )
            }
            val info = infoTrace.result.getOrThrow()
            val workflows = workflowsTrace.result.getOrThrow()
            if (!isCurrentTarget(item)) return
            state.actionsDownloadHistory = history
            state.actionsDefaultBranch = info.defaultBranch
            state.actionsSelectedBranch = info.defaultBranch
            state.actionsAuthMode = infoTrace.authMode
            state.actionsAuthMode = workflowsTrace.authMode ?: state.actionsAuthMode

            val preliminaryWorkflows = selectWorkflows(
                workflows = workflows,
                signals = emptyMap(),
                history = history
            )
            state.actionsRawWorkflows = workflows
            state.actionsWorkflowSignals = emptyMap()
            state.actionsWorkflows = preliminaryWorkflows
            state.actionsLoading = false
            val signalCandidateWorkflows = selectWorkflowSignalCandidates(
                workflows = workflows,
                preliminaryMatches = preliminaryWorkflows,
                history = history
            )

            val selectedWorkflow = preferredWorkflowId
                ?.let { id -> state.actionsWorkflows.firstOrNull { it.workflow.id == id } }
                ?: state.actionsWorkflows.firstOrNull()
            if (selectedWorkflow != null) {
                state.actionsSelectedWorkflowId = selectedWorkflow.workflow.id
                state.actionsWorkflowManuallySelected = false
                state.actionsRunLimit = DEFAULT_RUN_LIMIT
                refreshWorkflowSignalsInBackground(
                    item = item,
                    workflows = workflows,
                    candidateWorkflows = signalCandidateWorkflows,
                    history = history,
                    lookupConfig = lookupConfig,
                    defaultBranch = info.defaultBranch
                )
                loadWorkflowSnapshot(
                    item = item,
                    workflow = selectedWorkflow.workflow,
                    preferredRunId = null
                )
            } else {
                refreshWorkflowSignalsInBackground(
                    item = item,
                    workflows = workflows,
                    candidateWorkflows = signalCandidateWorkflows,
                    history = history,
                    lookupConfig = lookupConfig,
                    defaultBranch = info.defaultBranch
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

    private fun refreshWorkflowSignalsInBackground(
        item: GitHubTrackedApp,
        workflows: List<GitHubActionsWorkflow>,
        candidateWorkflows: List<GitHubActionsWorkflow>,
        history: List<os.kei.feature.github.model.GitHubActionsDownloadRecord>,
        lookupConfig: os.kei.feature.github.model.GitHubLookupConfig,
        defaultBranch: String
    ) {
        val cachedSignals = state.actionsWorkflowSignals
        val missingCandidateWorkflows = candidateWorkflows.filter { workflow ->
            val signal = cachedSignals[workflow.id]
            signal == null || signal.nonExpiredArtifactCount == 0
        }
        if (missingCandidateWorkflows.isEmpty()) return
        val expectedWorkflowIds = workflows.map { it.id }
        scope.launch {
            val signalsTrace = actionsRepository.fetchGitHubActionsWorkflowArtifactSignals(
                owner = item.owner,
                repo = item.repo,
                workflows = missingCandidateWorkflows,
                lookupConfig = lookupConfig,
                runLimit = SIGNAL_RUN_LIMIT,
                artifactsPerRun = SIGNAL_ARTIFACT_LIMIT,
                defaultBranch = defaultBranch
            )
            val signals = signalsTrace.result.getOrElse { return@launch }
            if (!isCurrentTarget(item)) return@launch
            if (state.actionsRawWorkflows.map { it.id } != expectedWorkflowIds) return@launch
            val mergedSignals = state.actionsWorkflowSignals + signals
            val updatedWorkflows = selectWorkflows(
                workflows = state.actionsRawWorkflows,
                signals = mergedSignals,
                history = history
            )
            if (!isCurrentTarget(item)) return@launch
            if (state.actionsRawWorkflows.map { it.id } != expectedWorkflowIds) return@launch
            state.actionsAuthMode = signalsTrace.authMode ?: state.actionsAuthMode
            state.actionsWorkflowSignals = mergedSignals
            state.actionsWorkflows = updatedWorkflows
            val autoSwitchedWorkflow = autoSwitchWorkflowFromSignals(
                item = item,
                updatedWorkflows = updatedWorkflows,
                mergedSignals = mergedSignals
            )
            if (autoSwitchedWorkflow) return@launch
            val selectedWorkflow = state.actionsSelectedWorkflowId
                ?.let { id -> updatedWorkflows.firstOrNull { it.workflow.id == id } }
                ?.workflow
            if (selectedWorkflow != null && !state.actionsBranchManuallySelected) {
                val previousBranch = selectedBranchForRequest()
                refreshBranchSelection(
                    workflow = selectedWorkflow,
                    snapshot = state.actionsSnapshot
                )
                val nextBranch = selectedBranchForRequest()
                if (!previousBranch.equals(nextBranch, ignoreCase = true)) {
                    state.actionsRunLimit = DEFAULT_RUN_LIMIT
                    loadWorkflowSnapshot(
                        item = item,
                        workflow = selectedWorkflow,
                        preferredRunId = null
                    )
                }
            }
        }
    }

    private suspend fun autoSwitchWorkflowFromSignals(
        item: GitHubTrackedApp,
        updatedWorkflows: List<GitHubActionsWorkflowMatch>,
        mergedSignals: Map<Long, os.kei.feature.github.model.GitHubActionsWorkflowArtifactSignal>
    ): Boolean {
        if (state.actionsWorkflowManuallySelected) return false
        val currentWorkflowId = state.actionsSelectedWorkflowId ?: return false
        val recommended = updatedWorkflows.firstOrNull() ?: return false
        if (recommended.workflow.id == currentWorkflowId) return false
        val currentSignal = mergedSignals[currentWorkflowId]
        val recommendedSignal = mergedSignals[recommended.workflow.id]
        val currentHasArtifacts = (currentSignal?.nonExpiredArtifactCount ?: 0) > 0 ||
            state.actionsSnapshot?.artifacts.orEmpty().any { artifact -> !artifact.expired }
        val recommendedHasArtifacts = (recommendedSignal?.nonExpiredArtifactCount ?: 0) > 0
        if (currentHasArtifacts || !recommendedHasArtifacts) return false
        state.actionsSelectedWorkflowId = recommended.workflow.id
        state.actionsSelectedBranch = ""
        state.actionsBranchManuallySelected = false
        state.actionsBranchOptions = emptyList()
        state.actionsRunLimit = DEFAULT_RUN_LIMIT
        loadWorkflowSnapshot(
            item = item,
            workflow = recommended.workflow,
            preferredRunId = null
        )
        return true
    }

    private suspend fun loadWorkflowSnapshot(
        item: GitHubTrackedApp,
        workflow: GitHubActionsWorkflow,
        preferredRunId: Long?,
        keepCurrentRunsWhileLoading: Boolean = false
    ) {
        state.actionsRunWatchJob?.cancel()
        state.actionsRunsLoading = true
        state.actionsError = null
        if (!keepCurrentRunsWhileLoading) {
            state.actionsSnapshot = null
            state.actionsRuns = emptyList()
            state.actionsSelectedRunId = null
            state.actionsRunTrackingPlans = emptyMap()
            state.actionsStatusRefreshingRunIds.clear()
        }
        refreshBranchSelection(
            workflow = workflow,
            snapshot = if (keepCurrentRunsWhileLoading) state.actionsSnapshot else null
        )
        val branch = branchForSnapshotRequest(workflow)
        var requestHandled = false
        try {
            val snapshotTrace = actionsRepository.fetchGitHubActionsWorkflowArtifactSnapshot(
                owner = item.owner,
                repo = item.repo,
                workflowId = workflowLookupId(workflow),
                lookupConfig = state.lookupConfig,
                runLimit = state.actionsRunLimit.coerceIn(DEFAULT_RUN_LIMIT, MAX_RUN_LIMIT),
                artifactsPerRun = ARTIFACTS_PER_RUN,
                artifactRunLimit = INITIAL_ARTIFACT_RUN_LIMIT,
                branch = branch
            )
            val snapshot = snapshotTrace.result.getOrThrow()
            if (!isCurrentSnapshotRequest(item, workflow, branch)) return
            requestHandled = true
            val runMatches = selectRunsForSnapshot(
                workflow = workflow,
                snapshot = snapshot,
                history = state.actionsDownloadHistory
            )
            state.actionsAuthMode = snapshotTrace.authMode ?: state.actionsAuthMode
            state.actionsSnapshot = snapshot
            state.actionsRuns = runMatches
            state.actionsRunTrackingPlans = buildTrackingPlans(runMatches)
            updateWorkflowSignalFromSnapshot(
                workflow = workflow,
                snapshot = snapshot
            )
            refreshBranchSelection(
                workflow = workflow,
                snapshot = snapshot
            )
            state.actionsSelectedRunId = preferredRunId
                ?.takeIf { runId -> runMatches.any { it.runArtifacts.run.id == runId } }
                ?: runMatches.firstOrNull()?.runArtifacts?.run?.id
            scheduleSelectedRunWatch()
            state.actionsSelectedRunId?.let { runId ->
                loadRunArtifactsIfNeeded(
                    item = item,
                    workflowId = workflow.id,
                    runId = runId
                )
            }
        } catch (error: Throwable) {
            if (isCurrentSnapshotRequest(item, workflow, branch)) {
                requestHandled = true
                state.actionsError = error.message ?: error.javaClass.simpleName
                if (keepCurrentRunsWhileLoading) {
                    scheduleSelectedRunWatch()
                }
            }
        } finally {
            if (requestHandled || isCurrentSnapshotRequest(item, workflow, branch)) {
                state.actionsRunsLoading = false
            }
        }
    }

    private suspend fun updateWorkflowSignalFromSnapshot(
        workflow: GitHubActionsWorkflow,
        snapshot: GitHubActionsWorkflowArtifactsSnapshot
    ) {
        if (state.actionsRawWorkflows.none { it.id == workflow.id }) return
        val signal = GitHubActionsWorkflowSelector.buildArtifactSignal(
            workflow = workflow,
            runs = snapshot.runs,
            defaultBranch = state.actionsDefaultBranch
        )
        val mergedSignals = state.actionsWorkflowSignals + (workflow.id to signal)
        state.actionsWorkflowSignals = mergedSignals
        state.actionsWorkflows = selectWorkflows(
            workflows = state.actionsRawWorkflows,
            signals = mergedSignals,
            history = state.actionsDownloadHistory
        )
    }

    private fun refreshBranchSelection(
        workflow: GitHubActionsWorkflow,
        snapshot: GitHubActionsWorkflowArtifactsSnapshot?
    ) {
        val signal = state.actionsWorkflowSignals[workflow.id]
        val options = GitHubActionsBranchSelector.buildOptions(
            defaultBranch = state.actionsDefaultBranch,
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )
        state.actionsBranchOptions = options
        if (state.actionsBranchManuallySelected && state.actionsSelectedBranch.isNotBlank()) {
            return
        }
        val recommendedBranch = GitHubActionsBranchSelector.recommendBranch(
            defaultBranch = state.actionsDefaultBranch,
            workflow = workflow,
            signal = signal,
            snapshot = snapshot
        )
        state.actionsSelectedBranch = recommendedBranch.ifBlank { state.actionsDefaultBranch }
    }

    private fun loadRunArtifactsIfNeeded(
        item: GitHubTrackedApp,
        workflowId: Long,
        runId: Long
    ) {
        if (!isCurrentTarget(item) || state.actionsSelectedWorkflowId != workflowId) return
        if (state.actionsStatusRefreshingRunIds[runId] == true) return
        val runMatch = state.actionsRuns.firstOrNull { it.runArtifacts.run.id == runId } ?: return
        if (!runMatch.traits.completed) return
        if (runMatch.runArtifacts.artifacts.isNotEmpty()) return
        scope.launch {
            if (!isCurrentTarget(item) || state.actionsSelectedWorkflowId != workflowId) return@launch
            refreshRunStatus(runId = runId, showToast = false)
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
            val statusTrace = actionsRepository.fetchGitHubActionsRunStatusSnapshot(
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
                        localizedGitHubActionsErrorMessage(
                            context = context,
                            rawMessage = error.message ?: error.javaClass.simpleName
                        )
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
        return actionsRepository.selectGitHubActionsWorkflows(
            workflows = workflows,
            artifactSignals = signals,
            options = GitHubActionsWorkflowSelectionOptions(
                includeDisabled = false,
                requireArtifacts = false,
                actionsStrategy = state.lookupConfig.actionsStrategy,
                downloadHistory = history
            )
        )
    }

    private fun selectWorkflowSignalCandidates(
        workflows: List<GitHubActionsWorkflow>,
        preliminaryMatches: List<GitHubActionsWorkflowMatch>,
        history: List<os.kei.feature.github.model.GitHubActionsDownloadRecord>
    ): List<GitHubActionsWorkflow> {
        val historyWorkflowIds = history
            .mapNotNull { record -> record.workflowId.takeIf { it > 0L } }
            .toSet()
        val historyWorkflows = workflows.filter { it.id in historyWorkflowIds }
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
            fallbackToAllArtifacts = true,
            downloadHistory = history
        )
        return actionsRepository.selectGitHubActionsRuns(
            runs = snapshot.runs,
            workflow = workflow,
            options = GitHubActionsRunSelectionOptions(
                defaultBranch = state.actionsDefaultBranch,
                preferredBranches = preferredBranchesForRunSelection(),
                includePullRequests = true,
                includeNonDefaultBranches = true,
                includeUnsuccessful = true,
                requireArtifacts = false,
                requireAndroidArtifacts = false,
                actionsStrategy = state.lookupConfig.actionsStrategy,
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
            run.id to actionsRepository.buildGitHubActionsRunTrackingPlan(run)
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

    private fun workflowLookupId(workflow: GitHubActionsWorkflow): String {
        return if (state.lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink) {
            workflow.path.ifBlank { workflow.displayName }
        } else {
            workflow.id.toString()
        }
    }

    private fun selectedBranchForRequest(): String {
        return state.actionsSelectedBranch.trim().ifBlank { state.actionsDefaultBranch.trim() }
    }

    private fun branchForSnapshotRequest(workflow: GitHubActionsWorkflow): String {
        val selectedBranch = selectedBranchForRequest()
        return if (
            state.lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.GitHubApiToken &&
            !state.actionsBranchManuallySelected &&
            state.actionsWorkflowSignals[workflow.id] == null
        ) {
            ""
        } else {
            selectedBranch
        }
    }

    private fun isCurrentSnapshotRequest(
        item: GitHubTrackedApp,
        workflow: GitHubActionsWorkflow,
        requestedBranch: String
    ): Boolean {
        if (!isCurrentTarget(item) || state.actionsSelectedWorkflowId != workflow.id) return false
        val currentBranch = selectedBranchForRequest()
        return if (requestedBranch.isBlank()) {
            !state.actionsBranchManuallySelected
        } else {
            currentBranch.equals(requestedBranch, ignoreCase = true)
        }
    }

    private fun preferredBranchesForRunSelection(): Set<String> {
        val baseBranches = if (state.lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink) {
            listOf(selectedBranchForRequest(), state.actionsDefaultBranch, "dev", "develop")
        } else {
            listOf(selectedBranchForRequest(), state.actionsDefaultBranch)
        }
        return baseBranches
            .map { branch -> branch.trim() }
            .filter { branch -> branch.isNotBlank() }
            .toSet()
    }

    private fun isCurrentTarget(item: GitHubTrackedApp): Boolean {
        return state.showActionsSheet && state.actionsTargetItem?.id == item.id
    }

    companion object {
        private const val DEFAULT_RUN_LIMIT = 6
        private const val RUN_PAGE_SIZE = 6
        private const val MAX_RUN_LIMIT = 30
        private const val INITIAL_ARTIFACT_RUN_LIMIT = 2
        private const val ARTIFACTS_PER_RUN = 80
        private const val WORKFLOW_SIGNAL_LIMIT = 8
        private const val SIGNAL_RUN_LIMIT = 2
        private const val SIGNAL_ARTIFACT_LIMIT = 60
    }
}
