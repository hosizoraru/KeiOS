package os.kei.ui.page.main.github.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.github.page.GitHubPageState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsWorkflowsSection(
    state: GitHubPageState,
    workflowsCount: Int,
    selectedWorkflowId: Long?,
    recommendedWorkflowId: Long?,
    isDark: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectWorkflow: (Long) -> Unit
) {
    GitHubActionsCollapsibleSection(
        title = stringResource(R.string.github_actions_section_workflows),
        summary = workflowSectionSummary(state, selectedWorkflowId),
        countLabel = stringResource(R.string.github_actions_value_count, workflowsCount),
        expanded = state.actionsWorkflowsExpanded,
        isDark = isDark,
        onExpandedChange = onExpandedChange
    ) {
        when {
            state.actionsLoading && state.actionsWorkflows.isEmpty() -> {
                GitHubActionsLoadingCard(
                    text = stringResource(R.string.github_actions_loading_workflows)
                )
            }
            state.actionsWorkflows.isEmpty() -> {
                GitHubActionsNoticeCard(
                    text = stringResource(R.string.github_actions_empty_workflows),
                    accent = MiuixTheme.colorScheme.onBackgroundVariant,
                    isDark = isDark
                )
            }
            else -> {
                state.actionsWorkflows.forEach { match ->
                    GitHubActionsWorkflowCard(
                        match = match,
                        selected = match.workflow.id == selectedWorkflowId,
                        recommended = match.workflow.id == recommendedWorkflowId,
                        isDark = isDark,
                        onClick = { onSelectWorkflow(match.workflow.id) }
                    )
                }
            }
        }
    }
}
