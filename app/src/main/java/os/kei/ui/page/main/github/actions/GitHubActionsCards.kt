package os.kei.ui.page.main.github.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunTrackingPlan
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsWorkflowCard(
    match: GitHubActionsWorkflowMatch,
    selected: Boolean,
    recommended: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val accent = MiuixTheme.colorScheme.primary
    val kindColor = workflowKindColor(match.traits.kind)
    GitHubActionsSelectableCard(
        selected = selected,
        isDark = isDark,
        onClick = onClick
    ) {
        GitHubActionsTitleRow(
            title = match.workflow.displayName,
            accent = if (selected) accent else MiuixTheme.colorScheme.onBackground,
            trailing = {
                GitHubActionsInfoPill(
                    label = workflowKindLabel(match.traits.kind),
                    color = kindColor,
                    emphasized = selected,
                    minWidth = GitHubActionsShortPillMinWidth
                )
            }
        )
        Text(
            text = match.workflow.path.ifBlank { match.workflow.id.toString() },
            color = githubActionsSecondaryTextColor(isDark),
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        GitHubActionsPillRow {
            if (recommended) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Update,
                    emphasized = true,
                    minWidth = GitHubActionsShortPillMinWidth
                )
            }
            if (match.lastDownload != null) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active,
                    emphasized = true,
                    minWidth = GitHubActionsStatePillMinWidth
                )
            }
            match.signal?.let { signal ->
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_label_runs_with_count, signal.recentRunCount),
                    color = GitHubStatusPalette.Active,
                    minWidth = GitHubActionsShortPillMinWidth
                )
                GitHubActionsInfoPill(
                    label = stringResource(
                        R.string.github_actions_label_artifacts_with_count,
                        signal.androidArtifactCount
                    ),
                    color = GitHubStatusPalette.PreRelease,
                    minWidth = GitHubActionsShortPillMinWidth
                )
            }
        }
    }
}

@Composable
internal fun GitHubActionsRunCard(
    match: GitHubActionsRunMatch,
    trackingPlan: GitHubActionsRunTrackingPlan?,
    selected: Boolean,
    recommended: Boolean,
    refreshing: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRun: (() -> Unit)?
) {
    val run = match.runArtifacts.run
    val actionAccent = MiuixTheme.colorScheme.primary
    val stateAccent = runStatusColor(match, trackingPlan)
    val metadataColor = MiuixTheme.colorScheme.onBackgroundVariant
    GitHubActionsSelectableCard(
        selected = selected,
        isDark = isDark,
        onClick = onClick
    ) {
        GitHubActionsTitleRow(
            title = run.displayName,
            accent = if (selected) actionAccent else MiuixTheme.colorScheme.onBackground,
            trailing = {
                GitHubActionsInfoPill(
                    label = runStatusLabel(match, trackingPlan),
                    color = stateAccent,
                    emphasized = selected,
                    minWidth = GitHubActionsShortPillMinWidth
                )
            }
        )
        Text(
            text = buildRunSubtitle(match, context = LocalContext.current),
            color = githubActionsSecondaryTextColor(isDark),
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        GitHubActionsPillRow {
            if (recommended) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Update,
                    emphasized = true,
                    minWidth = GitHubActionsShortPillMinWidth
                )
            }
            if (match.lastDownload != null) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active,
                    emphasized = true,
                    minWidth = GitHubActionsStatePillMinWidth
                )
            }
            runBranchTrustPill(match)
            if (match.traits.pullRequestLike) {
                GitHubActionsInfoPill(
                    label = stringResource(R.string.github_actions_badge_pr),
                    color = GitHubStatusPalette.Error,
                    emphasized = true
                )
            }
            GitHubActionsInfoPill(
                label = stringResource(R.string.github_actions_value_count, match.artifactMatches.size),
                color = metadataColor
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            if (selected && onOpenRun != null && run.htmlUrl.isNotBlank()) {
                AppCompactIconAction(
                    icon = appLucideExternalLinkIcon(),
                    contentDescription = stringResource(R.string.github_actions_action_open_run),
                    tint = actionAccent,
                    minSize = 42.dp,
                    onClick = onOpenRun,
                )
            }
            AppCompactIconAction(
                icon = appLucideRefreshIcon(),
                contentDescription = stringResource(R.string.github_actions_action_refresh_run),
                enabled = !refreshing,
                tint = actionAccent,
                minSize = 42.dp,
                onClick = onRefresh,
            )
        }
    }
}
