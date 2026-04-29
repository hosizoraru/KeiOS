package os.kei.ui.page.main.github.actions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.page.GitHubPageState
import os.kei.ui.page.main.os.appLucideChevronDownIcon
import os.kei.ui.page.main.os.appLucideChevronUpIcon
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.sheet.SheetSurfaceCard
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsSummaryCard(
    state: GitHubPageState,
    hasToken: Boolean,
    isDark: Boolean
) {
    val target = state.actionsTargetItem
    val accent = if (hasToken) GitHubStatusPalette.Update else GitHubStatusPalette.PreRelease
    val badgeLabel = when {
        hasToken -> stringResource(R.string.github_actions_badge_token_ready)
        state.actionsAuthMode == GitHubApiAuthMode.Guest -> stringResource(R.string.common_guest)
        else -> stringResource(R.string.github_actions_badge_token_required)
    }
    SheetSurfaceCard(
        containerColor = GitHubStatusPalette.tonedSurface(accent, isDark).copy(
            alpha = if (isDark) 0.24f else 0.13f
        ),
        borderColor = accent.copy(alpha = if (isDark) 0.32f else 0.22f),
        verticalSpacing = 6.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = target?.appLabel ?: stringResource(R.string.github_actions_sheet_title),
                modifier = Modifier.weight(1f),
                color = accent,
                fontSize = AppTypographyTokens.CardHeader.fontSize,
                lineHeight = AppTypographyTokens.CardHeader.lineHeight,
                fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusPill(
                label = badgeLabel,
                color = accent,
                size = AppStatusPillSize.Compact
            )
        }
        if (target != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${target.owner}/${target.repo}",
                    modifier = Modifier.weight(1f),
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                state.actionsDefaultBranch.takeIf { it.isNotBlank() }?.let { branch ->
                    StatusPill(
                        label = stringResource(R.string.github_actions_summary_default_branch, branch),
                        color = MiuixTheme.colorScheme.primary,
                        size = AppStatusPillSize.Compact
                    )
                }
            }
        }
    }
}

@Composable
internal fun workflowSectionSummary(
    state: GitHubPageState,
    selectedWorkflowId: Long?
): String {
    if (state.actionsLoading && state.actionsWorkflows.isEmpty()) {
        return stringResource(R.string.github_actions_loading_workflows)
    }
    val selected = selectedWorkflowId?.let { id ->
        state.actionsWorkflows.firstOrNull { it.workflow.id == id }
    }
    return selected?.workflow?.displayName
        ?: stringResource(R.string.github_actions_empty_workflows)
}

@Composable
internal fun runSectionSummary(
    state: GitHubPageState,
    selectedRun: GitHubActionsRunMatch?
): String {
    if (state.actionsRunsLoading && state.actionsRuns.isEmpty()) {
        return stringResource(R.string.github_actions_loading_runs)
    }
    val match = selectedRun ?: return stringResource(R.string.github_actions_empty_runs)
    val run = match.runArtifacts.run
    val number = run.runNumber.takeIf { it > 0L }?.let {
        stringResource(R.string.github_actions_value_run_number, it)
    }
    return listOfNotNull(
        number,
        run.headBranch.ifBlank { null },
        run.displayName.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
}

@Composable
internal fun artifactSectionSummary(
    selectedRun: GitHubActionsRunMatch?
): String {
    if (selectedRun == null) {
        return stringResource(R.string.github_actions_empty_artifacts)
    }
    if (selectedRun.traits.inProgress) {
        return stringResource(R.string.github_actions_hint_run_in_progress)
    }
    return selectedRun.artifactMatches.firstOrNull()?.artifact?.name
        ?: stringResource(R.string.github_actions_empty_artifacts)
}

@Composable
internal fun GitHubActionsCollapsibleSection(
    title: String,
    summary: String,
    countLabel: String,
    expanded: Boolean,
    accent: Color,
    isDark: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SheetSurfaceCard(
            containerColor = GitHubStatusPalette.tonedSurface(accent, isDark).copy(
                alpha = if (expanded) {
                    if (isDark) 0.24f else 0.13f
                } else {
                    if (isDark) 0.16f else 0.08f
                }
            ),
            borderColor = accent.copy(alpha = if (expanded) 0.30f else 0.18f),
            verticalSpacing = 0.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            onClick = { onExpandedChange(!expanded) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CardHeader.fontSize,
                        lineHeight = AppTypographyTokens.CardHeader.lineHeight,
                        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(label = countLabel, color = accent)
                AppCompactIconAction(
                    icon = if (expanded) appLucideChevronUpIcon() else appLucideChevronDownIcon(),
                    contentDescription = stringResource(
                        if (expanded) R.string.common_collapse else R.string.common_expand
                    ),
                    tint = accent,
                    minSize = 40.dp,
                    onClick = { onExpandedChange(!expanded) }
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = appExpandIn(),
            exit = appExpandOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}
