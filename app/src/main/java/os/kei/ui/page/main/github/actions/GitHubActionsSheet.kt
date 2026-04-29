package os.kei.ui.page.main.github.actions

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.feature.github.model.GitHubActionsArtifactKind
import os.kei.feature.github.model.GitHubActionsArtifactMatch
import os.kei.feature.github.model.GitHubActionsRunBranchTrust
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunTrackingPlan
import os.kei.feature.github.model.GitHubActionsRunWatchState
import os.kei.feature.github.model.GitHubActionsWorkflowKind
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.ui.page.main.github.GitHubCompactInfoRow
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.assetRelativeTimeLabel
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import os.kei.ui.page.main.github.page.GitHubPageState
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SheetSummaryCard
import os.kei.ui.page.main.widget.sheet.SheetSurfaceCard
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    state: GitHubPageState,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
    onSelectWorkflow: (Long) -> Unit,
    onSelectRun: (Long) -> Unit,
    onLoadMoreRuns: () -> Unit,
    onRefreshRun: (Long) -> Unit,
    onDownloadArtifact: (Long, Long) -> Unit,
    onOpenRun: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.github_actions_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideRefreshIcon(),
                contentDescription = stringResource(R.string.github_actions_sheet_cd_refresh),
                onClick = onRefresh
            )
        }
    ) {
        val context = LocalContext.current
        val isDark = isSystemInDarkTheme()
        val workflows = state.actionsWorkflows
        val selectedWorkflowId = state.actionsSelectedWorkflowId
        val selectedRun = state.actionsRuns.firstOrNull {
            it.runArtifacts.run.id == state.actionsSelectedRunId
        }
        val recommendedWorkflowId = workflows.firstOrNull()?.workflow?.id
        val recommendedRunId = state.actionsRuns
            .firstOrNull { it.traits.safeForRecommendation }
            ?.runArtifacts
            ?.run
            ?.id
        val hasToken = state.lookupConfig.apiToken.trim().isNotBlank()

        SheetContentColumn(verticalSpacing = 8.dp) {
            GitHubActionsSummaryCard(
                state = state,
                hasToken = hasToken,
                isDark = isDark
            )

            state.actionsError?.takeIf { it.isNotBlank() }?.let { message ->
                GitHubActionsNoticeCard(
                    text = stringResource(R.string.github_actions_error_load_failed, message),
                    accent = GitHubStatusPalette.Error,
                    isDark = isDark
                )
            }

            SheetSectionTitle(stringResource(R.string.github_actions_section_workflows))
            when {
                state.actionsLoading && workflows.isEmpty() -> {
                    GitHubActionsLoadingCard(
                        text = stringResource(R.string.github_actions_loading_workflows)
                    )
                }
                workflows.isEmpty() -> {
                    GitHubActionsNoticeCard(
                        text = stringResource(R.string.github_actions_empty_workflows),
                        accent = MiuixTheme.colorScheme.onBackgroundVariant,
                        isDark = isDark
                    )
                }
                else -> {
                    workflows.forEach { match ->
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

            SheetSectionTitle(stringResource(R.string.github_actions_section_runs))
            when {
                state.actionsRunsLoading -> {
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
                            selected = runId == state.actionsSelectedRunId,
                            recommended = runId == recommendedRunId,
                            refreshing = state.actionsStatusRefreshingRunIds[runId] == true,
                            isDark = isDark,
                            onClick = { onSelectRun(runId) },
                            onRefresh = { onRefreshRun(runId) }
                        )
                    }
                    if (
                        state.actionsRuns.size >= state.actionsRunLimit &&
                        state.actionsRunLimit < ACTIONS_MAX_RUN_LIMIT
                    ) {
                        GitHubActionsLoadMoreRunsButton(
                            backdrop = backdrop,
                            visibleRunLimit = state.actionsRunLimit,
                            onClick = onLoadMoreRuns
                        )
                    }
                }
            }

            SheetSectionTitle(stringResource(R.string.github_actions_section_artifacts))
            if (selectedRun != null) {
                GitHubActionsRunOpenButton(
                    backdrop = backdrop,
                    onOpenRun = onOpenRun
                )
            }
            when {
                selectedRun == null -> {
                    GitHubActionsNoticeCard(
                        text = stringResource(R.string.github_actions_empty_artifacts),
                        accent = MiuixTheme.colorScheme.onBackgroundVariant,
                        isDark = isDark
                    )
                }
                selectedRun.traits.inProgress -> {
                    GitHubActionsNoticeCard(
                        text = stringResource(R.string.github_actions_hint_run_in_progress),
                        accent = GitHubStatusPalette.Active,
                        isDark = isDark
                    )
                }
                selectedRun.artifactMatches.isEmpty() -> {
                    GitHubActionsNoticeCard(
                        text = stringResource(R.string.github_actions_empty_artifacts),
                        accent = MiuixTheme.colorScheme.onBackgroundVariant,
                        isDark = isDark
                    )
                }
                else -> {
                    SheetDescriptionText(
                        text = if (hasToken) {
                            stringResource(R.string.github_actions_hint_zip_download)
                        } else {
                            stringResource(R.string.github_actions_hint_token_required)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    selectedRun.artifactMatches.forEach { artifactMatch ->
                        GitHubActionsArtifactCard(
                            runMatch = selectedRun,
                            artifactMatch = artifactMatch,
                            hasToken = hasToken,
                            downloading = state.actionsArtifactDownloadLoadingId == artifactMatch.artifact.id,
                            context = context,
                            isDark = isDark,
                            backdrop = backdrop,
                            onDownload = {
                                onDownloadArtifact(
                                    selectedRun.runArtifacts.run.id,
                                    artifactMatch.artifact.id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubActionsSummaryCard(
    state: GitHubPageState,
    hasToken: Boolean,
    isDark: Boolean
) {
    val target = state.actionsTargetItem
    val accent = if (hasToken) GitHubStatusPalette.Update else GitHubStatusPalette.PreRelease
    SheetSummaryCard(
        title = target?.appLabel ?: stringResource(R.string.github_actions_sheet_title),
        accentColor = accent,
        badgeLabel = when {
            hasToken -> stringResource(R.string.github_actions_badge_token_ready)
            state.actionsAuthMode == GitHubApiAuthMode.Guest -> stringResource(R.string.common_guest)
            else -> stringResource(R.string.github_actions_badge_token_required)
        },
        badgeColor = accent,
        containerColor = GitHubStatusPalette.tonedSurface(accent, isDark).copy(
            alpha = if (isDark) 0.26f else 0.15f
        ),
        borderColor = accent.copy(alpha = if (isDark) 0.34f else 0.24f)
    ) {
        if (target != null) {
            GitHubCompactInfoRow(
                label = stringResource(R.string.github_actions_label_repo),
                value = "${target.owner}/${target.repo}",
                valueColor = MiuixTheme.colorScheme.primary,
                titleColor = MiuixTheme.colorScheme.primary
            )
        }
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_actions_label_default_branch),
            value = state.actionsDefaultBranch.ifBlank { stringResource(R.string.common_unknown) },
            valueColor = MiuixTheme.colorScheme.onBackground,
            titleColor = MiuixTheme.colorScheme.primary
        )
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_actions_label_strategy),
            value = state.actionsAuthMode?.label ?: state.lookupConfig.selectedStrategy.label,
            valueColor = accent,
            titleColor = MiuixTheme.colorScheme.primary
        )
        GitHubActionsPillRow {
            StatusPill(
                label = stringResource(R.string.github_actions_value_count, state.actionsWorkflows.size),
                color = GitHubStatusPalette.Active
            )
            StatusPill(
                label = stringResource(R.string.github_actions_label_runs_with_count, state.actionsRuns.size),
                color = GitHubStatusPalette.Update
            )
            StatusPill(
                label = stringResource(
                    R.string.github_actions_label_artifacts_with_count,
                    state.actionsRuns.sumOf { it.artifactMatches.size }
                ),
                color = GitHubStatusPalette.PreRelease
            )
        }
    }
}

@Composable
private fun GitHubActionsWorkflowCard(
    match: GitHubActionsWorkflowMatch,
    selected: Boolean,
    recommended: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val accent = workflowAccent(match)
    GitHubActionsSelectableCard(
        selected = selected,
        accent = accent,
        isDark = isDark,
        onClick = onClick
    ) {
        GitHubActionsTitleRow(
            title = match.workflow.displayName,
            accent = if (selected) accent else MiuixTheme.colorScheme.onBackground,
            trailing = {
                StatusPill(
                    label = workflowKindLabel(match.traits.kind),
                    color = accent,
                    size = AppStatusPillSize.Compact
                )
            }
        )
        Text(
            text = match.workflow.path.ifBlank { match.workflow.id.toString() },
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        GitHubActionsPillRow {
            if (recommended) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Update
                )
            }
            if (match.lastDownload != null) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active
                )
            }
            match.signal?.let { signal ->
                StatusPill(
                    label = stringResource(R.string.github_actions_label_runs_with_count, signal.recentRunCount),
                    color = GitHubStatusPalette.Active
                )
                StatusPill(
                    label = stringResource(
                        R.string.github_actions_label_artifacts_with_count,
                        signal.androidArtifactCount
                    ),
                    color = GitHubStatusPalette.PreRelease
                )
            }
        }
    }
}

@Composable
private fun GitHubActionsRunCard(
    match: GitHubActionsRunMatch,
    trackingPlan: GitHubActionsRunTrackingPlan?,
    selected: Boolean,
    recommended: Boolean,
    refreshing: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val run = match.runArtifacts.run
    val accent = runAccent(match, trackingPlan)
    GitHubActionsSelectableCard(
        selected = selected,
        accent = accent,
        isDark = isDark,
        onClick = onClick
    ) {
        GitHubActionsTitleRow(
            title = run.displayName,
            accent = if (selected) accent else MiuixTheme.colorScheme.onBackground,
            trailing = {
                StatusPill(
                    label = runStatusLabel(match, trackingPlan),
                    color = accent,
                    size = AppStatusPillSize.Compact
                )
            }
        )
        Text(
            text = buildRunSubtitle(match, context = LocalContext.current),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        GitHubActionsPillRow {
            if (recommended) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Update
                )
            }
            if (match.lastDownload != null) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active
                )
            }
            runBranchTrustPill(match)
            if (match.traits.pullRequestLike) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_pr),
                    color = GitHubStatusPalette.Error
                )
            }
            StatusPill(
                label = stringResource(R.string.github_actions_label_artifacts_with_count, match.artifactMatches.size),
                color = GitHubStatusPalette.PreRelease
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            AppCompactIconAction(
                icon = appLucideRefreshIcon(),
                contentDescription = stringResource(R.string.github_actions_action_refresh_run),
                enabled = !refreshing,
                tint = accent,
                minSize = 42.dp,
                onClick = onRefresh,
            )
        }
    }
}

@Composable
private fun GitHubActionsArtifactCard(
    runMatch: GitHubActionsRunMatch,
    artifactMatch: GitHubActionsArtifactMatch,
    hasToken: Boolean,
    downloading: Boolean,
    context: Context,
    isDark: Boolean,
    backdrop: LayerBackdrop,
    onDownload: () -> Unit
) {
    val artifact = artifactMatch.artifact
    val accent = artifactAccent(artifactMatch)
    val canDownload = hasToken && runMatch.traits.completed && !artifact.expired && !downloading
    GitHubActionsSelectableCard(
        selected = false,
        accent = accent,
        isDark = isDark,
        onClick = null
    ) {
        GitHubActionsTitleRow(
            title = artifact.name,
            accent = MiuixTheme.colorScheme.onBackground,
            trailing = {
                StatusPill(
                    label = artifactKindLabel(artifactMatch.traits.kind),
                    color = accent,
                    size = AppStatusPillSize.Compact
                )
            }
        )
        GitHubActionsPillRow {
            StatusPill(
                label = formatAssetSize(artifact.sizeBytes, context),
                color = MiuixTheme.colorScheme.primary
            )
            artifactMatch.traits.abi.takeIf { it.isNotBlank() }?.let { abi ->
                StatusPill(label = abi, color = GitHubStatusPalette.Active)
            }
            artifactMatch.traits.flavors.forEach { flavor ->
                StatusPill(label = flavor, color = GitHubStatusPalette.PreRelease)
            }
            if (artifactMatch.traits.releaseLike) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_release),
                    color = GitHubStatusPalette.Update
                )
            }
            if (artifactMatch.traits.debugLike) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_debug),
                    color = GitHubStatusPalette.PreRelease
                )
            }
            if (artifactMatch.traits.universalLike) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_universal),
                    color = GitHubStatusPalette.Active
                )
            }
            if (artifact.expired) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_expired),
                    color = GitHubStatusPalette.Error
                )
            }
            if (artifactMatch.lastDownload != null) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active
                )
            }
            assetRelativeTimeLabel(artifact.updatedAtMillis, context)?.let { label ->
                StatusPill(label = label, color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artifact.digest.ifBlank {
                    formatReleaseUpdatedAtNoYear(artifact.updatedAtMillis)
                        ?: stringResource(R.string.common_unknown)
                },
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            GlassTextButton(
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
                text = artifactDownloadLabel(
                    hasToken = hasToken,
                    completed = runMatch.traits.completed,
                    expired = artifact.expired,
                    downloading = downloading
                ),
                leadingIcon = appLucideDownloadIcon(),
                enabled = canDownload,
                textColor = accent,
                iconTint = accent,
                modifier = Modifier.widthIn(min = 96.dp),
                onClick = onDownload,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GitHubActionsRunOpenButton(
    backdrop: LayerBackdrop,
    onOpenRun: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        GlassTextButton(
            backdrop = backdrop,
            variant = GlassVariant.SheetAction,
            text = stringResource(R.string.github_actions_action_open_run),
            leadingIcon = appLucideExternalLinkIcon(),
            textColor = MiuixTheme.colorScheme.primary,
            iconTint = MiuixTheme.colorScheme.primary,
            onClick = onOpenRun,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GitHubActionsLoadMoreRunsButton(
    backdrop: LayerBackdrop,
    visibleRunLimit: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        GlassTextButton(
            backdrop = backdrop,
            variant = GlassVariant.SheetAction,
            text = stringResource(R.string.github_actions_action_load_more_runs, visibleRunLimit),
            leadingIcon = appLucideRefreshIcon(),
            textColor = MiuixTheme.colorScheme.primary,
            iconTint = MiuixTheme.colorScheme.primary,
            onClick = onClick,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GitHubActionsSelectableCard(
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val containerColor = if (selected) {
        GitHubStatusPalette.tonedSurface(accent, isDark).copy(alpha = if (isDark) 0.32f else 0.18f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)
    }
    val borderColor = if (selected) {
        accent.copy(alpha = if (isDark) 0.48f else 0.34f)
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f)
    }
    SheetSurfaceCard(
        containerColor = containerColor,
        borderColor = borderColor,
        verticalSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onClick
    ) {
        content()
    }
}

@Composable
private fun GitHubActionsTitleRow(
    title: String,
    accent: Color,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = accent,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHubActionsPillRow(
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() }
    )
}

@Composable
private fun GitHubActionsLoadingCard(text: String) {
    SheetSurfaceCard(
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f),
        borderColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                progress = 0f,
                size = 18.dp,
                strokeWidth = 2.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = MiuixTheme.colorScheme.primary,
                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
            )
            Text(
                text = text,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight
            )
        }
    }
}

@Composable
private fun GitHubActionsNoticeCard(
    text: String,
    accent: Color,
    isDark: Boolean
) {
    SheetSurfaceCard(
        containerColor = GitHubStatusPalette.tonedSurface(accent, isDark).copy(
            alpha = if (isDark) 0.24f else 0.13f
        ),
        borderColor = accent.copy(alpha = if (isDark) 0.32f else 0.22f)
    ) {
        Text(
            text = text,
            color = if (accent == MiuixTheme.colorScheme.onBackgroundVariant) {
                MiuixTheme.colorScheme.onBackgroundVariant
            } else {
                accent
            },
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
    }
}

@Composable
private fun workflowKindLabel(kind: GitHubActionsWorkflowKind): String {
    return when (kind) {
        GitHubActionsWorkflowKind.AndroidBuild -> stringResource(R.string.github_actions_badge_android)
        GitHubActionsWorkflowKind.Release -> stringResource(R.string.github_actions_badge_release)
        GitHubActionsWorkflowKind.Nightly -> stringResource(R.string.github_actions_badge_nightly)
        GitHubActionsWorkflowKind.Ci -> stringResource(R.string.github_actions_badge_ci)
        GitHubActionsWorkflowKind.Quality -> stringResource(R.string.github_actions_badge_quality)
        GitHubActionsWorkflowKind.Localization -> stringResource(R.string.github_actions_badge_localization)
        GitHubActionsWorkflowKind.Dependency -> stringResource(R.string.github_actions_badge_dependency)
        GitHubActionsWorkflowKind.Documentation -> stringResource(R.string.github_actions_badge_docs)
        GitHubActionsWorkflowKind.Automation -> stringResource(R.string.github_actions_badge_auto)
        GitHubActionsWorkflowKind.Unknown -> stringResource(R.string.github_actions_badge_build)
    }
}

@Composable
private fun artifactKindLabel(kind: GitHubActionsArtifactKind): String {
    return when (kind) {
        GitHubActionsArtifactKind.AndroidPackage -> stringResource(R.string.github_actions_badge_apk)
        GitHubActionsArtifactKind.AndroidBundle -> stringResource(R.string.github_actions_badge_aab)
        GitHubActionsArtifactKind.Archive -> stringResource(R.string.github_actions_badge_zip)
        GitHubActionsArtifactKind.Mapping -> stringResource(R.string.github_actions_badge_mapping)
        GitHubActionsArtifactKind.Report -> stringResource(R.string.github_actions_badge_report)
        GitHubActionsArtifactKind.Source -> stringResource(R.string.github_actions_badge_source)
        GitHubActionsArtifactKind.Unknown -> stringResource(R.string.common_unknown)
    }
}

@Composable
private fun runBranchTrustPill(match: GitHubActionsRunMatch) {
    val trust = match.traits.branchTrust
    val label = when (trust) {
        GitHubActionsRunBranchTrust.DefaultBranch -> stringResource(R.string.github_actions_badge_default_branch)
        GitHubActionsRunBranchTrust.ReleaseTag -> stringResource(R.string.github_actions_badge_release_tag)
        GitHubActionsRunBranchTrust.MainlineBranch -> stringResource(R.string.github_actions_badge_default_branch)
        GitHubActionsRunBranchTrust.ReleaseBranch -> stringResource(R.string.github_actions_badge_release_branch)
        GitHubActionsRunBranchTrust.PullRequest -> stringResource(R.string.github_actions_badge_pr)
        GitHubActionsRunBranchTrust.FeatureBranch -> stringResource(R.string.github_actions_badge_branch)
        GitHubActionsRunBranchTrust.Unknown -> stringResource(R.string.common_unknown)
    }
    val color = when (trust) {
        GitHubActionsRunBranchTrust.DefaultBranch,
        GitHubActionsRunBranchTrust.MainlineBranch -> GitHubStatusPalette.Update
        GitHubActionsRunBranchTrust.ReleaseTag,
        GitHubActionsRunBranchTrust.ReleaseBranch -> GitHubStatusPalette.PreRelease
        GitHubActionsRunBranchTrust.PullRequest -> GitHubStatusPalette.Error
        GitHubActionsRunBranchTrust.FeatureBranch,
        GitHubActionsRunBranchTrust.Unknown -> MiuixTheme.colorScheme.onBackgroundVariant
    }
    StatusPill(label = label, color = color)
}

@Composable
private fun runStatusLabel(
    match: GitHubActionsRunMatch,
    trackingPlan: GitHubActionsRunTrackingPlan?
): String {
    return when (trackingPlan?.state) {
        GitHubActionsRunWatchState.Queued -> stringResource(R.string.github_actions_badge_queued)
        GitHubActionsRunWatchState.Running -> stringResource(R.string.github_actions_badge_running)
        GitHubActionsRunWatchState.Completed -> stringResource(R.string.github_actions_badge_success)
        GitHubActionsRunWatchState.Failed -> stringResource(R.string.github_actions_badge_failed)
        GitHubActionsRunWatchState.Unknown,
        null -> when {
            match.traits.inProgress -> stringResource(R.string.github_actions_badge_running)
            match.traits.successful -> stringResource(R.string.github_actions_badge_success)
            match.traits.completed -> stringResource(R.string.github_actions_badge_completed)
            else -> match.runArtifacts.run.status.ifBlank { stringResource(R.string.common_unknown) }
        }
    }
}

private fun workflowAccent(match: GitHubActionsWorkflowMatch): Color {
    return when (match.traits.kind) {
        GitHubActionsWorkflowKind.AndroidBuild -> GitHubStatusPalette.Update
        GitHubActionsWorkflowKind.Release -> GitHubStatusPalette.PreRelease
        GitHubActionsWorkflowKind.Nightly -> GitHubStatusPalette.Active
        GitHubActionsWorkflowKind.Ci -> GitHubStatusPalette.Active
        GitHubActionsWorkflowKind.Quality,
        GitHubActionsWorkflowKind.Localization,
        GitHubActionsWorkflowKind.Dependency,
        GitHubActionsWorkflowKind.Documentation,
        GitHubActionsWorkflowKind.Automation,
        GitHubActionsWorkflowKind.Unknown -> GitHubStatusPalette.Stable
    }
}

private fun runAccent(
    match: GitHubActionsRunMatch,
    trackingPlan: GitHubActionsRunTrackingPlan?
): Color {
    return when {
        trackingPlan?.state == GitHubActionsRunWatchState.Running ||
            trackingPlan?.state == GitHubActionsRunWatchState.Queued -> GitHubStatusPalette.Active
        match.traits.completed && !match.traits.successful -> GitHubStatusPalette.Error
        match.traits.pullRequestLike || match.traits.forkLike -> GitHubStatusPalette.PreRelease
        match.traits.safeForRecommendation -> GitHubStatusPalette.Update
        match.traits.releaseTag || match.traits.releaseBranch -> GitHubStatusPalette.PreRelease
        else -> GitHubStatusPalette.Stable
    }
}

private fun artifactAccent(match: GitHubActionsArtifactMatch): Color {
    return when {
        match.artifact.expired -> GitHubStatusPalette.Error
        match.lastDownload != null -> GitHubStatusPalette.Active
        match.traits.releaseLike -> GitHubStatusPalette.Update
        match.traits.debugLike -> GitHubStatusPalette.PreRelease
        else -> GitHubStatusPalette.Stable
    }
}

@Composable
private fun buildRunSubtitle(
    match: GitHubActionsRunMatch,
    context: Context
): String {
    val run = match.runArtifacts.run
    val runNumber = if (run.runNumber > 0L) {
        stringResource(R.string.github_actions_value_run_number, run.runNumber)
    } else {
        run.id.toString()
    }
    val attempt = run.runAttempt.takeIf { it > 1 }?.let {
        stringResource(R.string.github_actions_value_run_attempt, it)
    }
    val updated = formatReleaseUpdatedAtNoYear(run.updatedAtMillis ?: run.createdAtMillis)
    return listOfNotNull(
        run.headBranch.ifBlank { context.getString(R.string.common_unknown) },
        run.event.ifBlank { context.getString(R.string.common_unknown) },
        runNumber,
        attempt,
        updated
    ).joinToString(" · ")
}

@Composable
private fun artifactDownloadLabel(
    hasToken: Boolean,
    completed: Boolean,
    expired: Boolean,
    downloading: Boolean
): String {
    return when {
        downloading -> stringResource(R.string.github_actions_action_downloading)
        !hasToken -> stringResource(R.string.github_actions_action_need_token)
        expired -> stringResource(R.string.github_actions_action_expired)
        !completed -> stringResource(R.string.github_actions_action_wait_run)
        else -> stringResource(R.string.common_download)
    }
}

private const val ACTIONS_MAX_RUN_LIMIT = 30
