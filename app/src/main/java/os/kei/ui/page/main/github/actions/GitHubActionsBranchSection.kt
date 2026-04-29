package os.kei.ui.page.main.github.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.feature.github.model.GitHubActionsBranchOption
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.page.GitHubPageState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsBranchSection(
    state: GitHubPageState,
    isDark: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectBranch: (String) -> Unit
) {
    GitHubActionsCollapsibleSection(
        title = stringResource(R.string.github_actions_section_branch),
        summary = branchSectionSummary(state),
        countLabel = stringResource(R.string.github_actions_value_count, state.actionsBranchOptions.size),
        expanded = state.actionsBranchesExpanded,
        isDark = isDark,
        onExpandedChange = onExpandedChange
    ) {
        when {
            state.actionsLoading && state.actionsBranchOptions.isEmpty() -> {
                GitHubActionsLoadingCard(
                    text = stringResource(R.string.github_actions_loading_branches)
                )
            }
            state.actionsBranchOptions.isEmpty() -> {
                GitHubActionsNoticeCard(
                    text = stringResource(R.string.github_actions_empty_branches),
                    accent = MiuixTheme.colorScheme.onBackgroundVariant,
                    isDark = isDark
                )
            }
            else -> {
                state.actionsBranchOptions.forEach { option ->
                    GitHubActionsBranchCard(
                        option = option,
                        selected = state.actionsSelectedBranch.equals(option.name, ignoreCase = true),
                        isDark = isDark,
                        onClick = { onSelectBranch(option.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun branchSectionSummary(state: GitHubPageState): String {
    val branch = state.actionsSelectedBranch.ifBlank { state.actionsDefaultBranch }
    return branch.ifBlank {
        if (state.actionsLoading) {
            stringResource(R.string.github_actions_loading_branches)
        } else {
            stringResource(R.string.github_actions_empty_branches)
        }
    }
}

@Composable
private fun GitHubActionsBranchCard(
    option: GitHubActionsBranchOption,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val accent = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground
    GitHubActionsSelectableCard(
        selected = selected,
        isDark = isDark,
        onClick = onClick
    ) {
        GitHubActionsTitleRow(
            title = option.name,
            accent = accent,
            trailing = {
                GitHubActionsInfoPill(
                    label = if (selected) {
                        stringResource(R.string.github_actions_badge_current)
                    } else {
                        stringResource(R.string.github_actions_label_runs_with_count, option.runCount)
                    },
                    color = if (selected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    emphasized = selected,
                    minWidth = GitHubActionsStatePillMinWidth
                )
            }
        )
        GitHubActionsPillRow {
            if (option.defaultBranch) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_default_branch),
                    color = GitHubStatusPalette.Update,
                    emphasized = selected
                )
            }
            if (option.recommended) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Active,
                    emphasized = true
                )
            }
            if (option.artifactCount > 0) {
                GitHubActionsInfoPill(
                    label = stringResource(
                        R.string.github_actions_label_artifacts_with_count,
                        option.artifactCount
                    ),
                    color = GitHubStatusPalette.PreRelease,
                    emphasized = selected
                )
            }
            if (option.runCount > 0 && selected) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_label_runs_with_count, option.runCount),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    }
}
