package os.kei.ui.page.main.github.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.ui.page.main.github.page.GitHubPageState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsRunsSection(
    state: GitHubPageState,
    selectedRunId: Long?,
    selectedRun: GitHubActionsRunMatch?,
    recommendedRunId: Long?,
    isDark: Boolean,
    backdrop: LayerBackdrop,
    onExpandedChange: (Boolean) -> Unit,
    onSelectRun: (Long) -> Unit,
    onRefreshRun: (Long) -> Unit,
    onOpenRun: () -> Unit,
    onLoadMoreRuns: () -> Unit
) {
    GitHubActionsCollapsibleSection(
        title = stringResource(R.string.github_actions_section_runs),
        summary = runSectionSummary(state, selectedRun),
        countLabel = stringResource(R.string.github_actions_value_count, state.actionsRuns.size),
        expanded = state.actionsRunsExpanded,
        isDark = isDark,
        onExpandedChange = onExpandedChange
    ) {
        when {
            state.actionsRunsLoading && state.actionsRuns.isEmpty() -> {
                GitHubActionsLoadingCard(
                    text = stringResource(R.string.github_actions_loading_runs)
                )
            }
            state.actionsRuns.isEmpty() -> {
                GitHubActionsNoticeCard(
                    text = stringResource(R.string.github_actions_empty_runs),
                    accent = MiuixTheme.colorScheme.onBackgroundVariant,
                    isDark = isDark
                )
            }
            else -> {
                state.actionsRuns.forEach { match ->
                    val runId = match.runArtifacts.run.id
                    GitHubActionsRunCard(
                        match = match,
                        trackingPlan = state.actionsRunTrackingPlans[runId],
                        selected = runId == selectedRunId,
                        recommended = runId == recommendedRunId,
                        refreshing = state.actionsStatusRefreshingRunIds[runId] == true,
                        isDark = isDark,
                        onClick = { onSelectRun(runId) },
                        onRefresh = { onRefreshRun(runId) },
                        onOpenRun = if (runId == selectedRunId) onOpenRun else null
                    )
                }
                val showLoadMoreButton =
                    (state.actionsRunsLoading && state.actionsRuns.isNotEmpty()) ||
                        (
                            state.actionsRuns.size >= state.actionsRunLimit &&
                                state.actionsRunLimit < ACTIONS_MAX_RUN_LIMIT
                            )
                if (showLoadMoreButton) {
                    GitHubActionsLoadMoreRunsButton(
                        backdrop = backdrop,
                        visibleRunLimit = state.actionsRunLimit,
                        loading = state.actionsRunsLoading,
                        onClick = onLoadMoreRuns
                    )
                }
            }
        }
    }
}

private const val ACTIONS_MAX_RUN_LIMIT = 30
