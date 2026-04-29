package os.kei.ui.page.main.github.actions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.feature.github.model.GitHubActionsLookupStrategyOption
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.page.GitHubPageState
import os.kei.ui.page.main.widget.sheet.SheetContentColumn

@Composable
internal fun GitHubActionsSheetContent(
    state: GitHubPageState,
    backdrop: LayerBackdrop,
    onSelectWorkflow: (Long) -> Unit,
    onSelectBranch: (String) -> Unit,
    onSelectRun: (Long) -> Unit,
    onLoadMoreRuns: () -> Unit,
    onBranchesExpandedChange: (Boolean) -> Unit,
    onWorkflowsExpandedChange: (Boolean) -> Unit,
    onRunsExpandedChange: (Boolean) -> Unit,
    onArtifactsExpandedChange: (Boolean) -> Unit,
    onRefreshRun: (Long) -> Unit,
    onDownloadArtifact: (Long, Long) -> Unit,
    onShareArtifact: (Long, Long) -> Unit,
    onOpenRun: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val workflows = state.actionsWorkflows
    val selectedWorkflowId = state.actionsSelectedWorkflowId
    val selectedRun = state.actionsRuns.firstOrNull {
        it.runArtifacts.run.id == state.actionsSelectedRunId
    }
    val recommendedWorkflowId = workflows.firstOrNull()?.workflow?.id
    val recommendedRunId = (
        state.actionsRuns.firstOrNull { match ->
            match.traits.completed &&
                match.traits.successful &&
                !match.traits.pullRequestLike &&
                match.artifactMatches.isNotEmpty()
        } ?: state.actionsRuns.firstOrNull { it.traits.safeForRecommendation }
        )?.runArtifacts
        ?.run
        ?.id
    val selectedRunArtifactsLoading = selectedRun?.let { runMatch ->
        val runId = runMatch.runArtifacts.run.id
        runMatch.traits.completed &&
            runMatch.runArtifacts.artifacts.isEmpty() &&
            state.actionsStatusRefreshingRunIds[runId] == true
    } == true
    val canResolveArtifacts = state.lookupConfig.actionsArtifactDownloadsAvailable

    SheetContentColumn(verticalSpacing = 10.dp) {
        GitHubActionsSummaryCard(
            state = state,
            canResolveArtifacts = canResolveArtifacts,
            isDark = isDark
        )

        state.actionsError?.takeIf { it.isNotBlank() }?.let { message ->
            val errorText = if (state.lookupConfig.actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink) {
                stringResource(R.string.github_actions_error_load_failed_nightly, message)
            } else {
                stringResource(R.string.github_actions_error_load_failed, message)
            }
            GitHubActionsNoticeCard(
                text = errorText,
                accent = GitHubStatusPalette.Error,
                isDark = isDark
            )
        }

        GitHubActionsBranchSection(
            state = state,
            isDark = isDark,
            onExpandedChange = onBranchesExpandedChange,
            onSelectBranch = onSelectBranch
        )

        GitHubActionsWorkflowsSection(
            state = state,
            workflowsCount = workflows.size,
            selectedWorkflowId = selectedWorkflowId,
            recommendedWorkflowId = recommendedWorkflowId,
            isDark = isDark,
            onExpandedChange = onWorkflowsExpandedChange,
            onSelectWorkflow = onSelectWorkflow
        )

        GitHubActionsRunsSection(
            state = state,
            selectedRunId = state.actionsSelectedRunId,
            selectedRun = selectedRun,
            recommendedRunId = recommendedRunId,
            isDark = isDark,
            backdrop = backdrop,
            onExpandedChange = onRunsExpandedChange,
            onSelectRun = onSelectRun,
            onRefreshRun = onRefreshRun,
            onOpenRun = onOpenRun,
            onLoadMoreRuns = onLoadMoreRuns
        )

        GitHubActionsArtifactsSection(
            state = state,
            selectedRun = selectedRun,
            selectedRunArtifactsLoading = selectedRunArtifactsLoading,
            canResolveArtifacts = canResolveArtifacts,
            isDark = isDark,
            backdrop = backdrop,
            onExpandedChange = onArtifactsExpandedChange,
            onDownloadArtifact = onDownloadArtifact,
            onShareArtifact = onShareArtifact,
            context = context
        )
    }
}
