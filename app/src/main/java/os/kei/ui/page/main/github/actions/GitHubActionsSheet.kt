package os.kei.ui.page.main.github.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.assetRelativeTimeLabel
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import os.kei.ui.page.main.github.page.GitHubPageState
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
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
    onWorkflowsExpandedChange: (Boolean) -> Unit,
    onRunsExpandedChange: (Boolean) -> Unit,
    onArtifactsExpandedChange: (Boolean) -> Unit,
    onRefreshRun: (Long) -> Unit,
    onDownloadArtifact: (Long, Long) -> Unit,
    onShareArtifact: (Long, Long) -> Unit,
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
        val selectedRunArtifactsLoading = selectedRun?.let { runMatch ->
            val runId = runMatch.runArtifacts.run.id
            runMatch.traits.completed &&
                runMatch.runArtifacts.artifacts.isEmpty() &&
                state.actionsStatusRefreshingRunIds[runId] == true
        } == true
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

            GitHubActionsCollapsibleSection(
                title = stringResource(R.string.github_actions_section_workflows),
                summary = workflowSectionSummary(state, selectedWorkflowId),
                countLabel = stringResource(R.string.github_actions_value_count, workflows.size),
                expanded = state.actionsWorkflowsExpanded,
                accent = GitHubStatusPalette.Active,
                isDark = isDark,
                onExpandedChange = onWorkflowsExpandedChange
            ) {
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
            }

            GitHubActionsCollapsibleSection(
                title = stringResource(R.string.github_actions_section_runs),
                summary = runSectionSummary(state, selectedRun),
                countLabel = stringResource(R.string.github_actions_value_count, state.actionsRuns.size),
                expanded = state.actionsRunsExpanded,
                accent = GitHubStatusPalette.Update,
                isDark = isDark,
                onExpandedChange = onRunsExpandedChange
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
                                selected = runId == state.actionsSelectedRunId,
                                recommended = runId == recommendedRunId,
                                refreshing = state.actionsStatusRefreshingRunIds[runId] == true,
                                isDark = isDark,
                                onClick = { onSelectRun(runId) },
                                onRefresh = { onRefreshRun(runId) },
                                onOpenRun = if (runId == state.actionsSelectedRunId) onOpenRun else null
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

            GitHubActionsCollapsibleSection(
                title = stringResource(R.string.github_actions_section_artifacts),
                summary = artifactSectionSummary(selectedRun),
                countLabel = stringResource(
                    R.string.github_actions_value_count,
                    selectedRun?.artifactMatches?.size ?: 0
                ),
                expanded = state.actionsArtifactsExpanded,
                accent = GitHubStatusPalette.PreRelease,
                isDark = isDark,
                onExpandedChange = onArtifactsExpandedChange
            ) {
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
                    selectedRunArtifactsLoading -> {
                        GitHubActionsLoadingCard(
                            text = stringResource(R.string.github_actions_loading_artifacts)
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
                        if (!hasToken) {
                            GitHubActionsArtifactHintText(
                                text = stringResource(R.string.github_actions_hint_token_required)
                            )
                        }
                        selectedRun.artifactMatches.forEachIndexed { index, artifactMatch ->
                            GitHubActionsArtifactCard(
                                runMatch = selectedRun,
                                artifactMatch = artifactMatch,
                                recommended = index == 0,
                                hasToken = hasToken,
                                downloading = state.actionsArtifactDownloadLoadingId == artifactMatch.artifact.id,
                                sharing = state.actionsArtifactShareLoadingId == artifactMatch.artifact.id,
                                context = context,
                                isDark = isDark,
                                backdrop = backdrop,
                                onDownload = {
                                    onDownloadArtifact(
                                        selectedRun.runArtifacts.run.id,
                                        artifactMatch.artifact.id
                                    )
                                },
                                onShare = {
                                    onShareArtifact(
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
    onRefresh: () -> Unit,
    onOpenRun: (() -> Unit)?
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
                label = stringResource(R.string.github_actions_value_count, match.artifactMatches.size),
                color = GitHubStatusPalette.PreRelease
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
                    tint = accent,
                    minSize = 42.dp,
                    onClick = onOpenRun,
                )
            }
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
    recommended: Boolean,
    hasToken: Boolean,
    downloading: Boolean,
    sharing: Boolean,
    context: Context,
    isDark: Boolean,
    backdrop: LayerBackdrop,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    val artifact = artifactMatch.artifact
    val accent = if (recommended) GitHubStatusPalette.Update else artifactAccent(artifactMatch)
    val busy = downloading || sharing
    val canDownload = hasToken && runMatch.traits.completed && !artifact.expired && !busy
    val canShare = hasToken && runMatch.traits.completed && !artifact.expired && !busy
    val copyDigestLabel = stringResource(R.string.github_actions_cd_copy_digest)
    val digestCopiedToast = stringResource(R.string.github_actions_toast_digest_copied)
    val hasDigest = artifact.digest.isNotBlank()
    val digestText = if (hasDigest) {
        shortArtifactDigest(artifact.digest)
    } else {
        formatReleaseUpdatedAtNoYear(artifact.updatedAtMillis)
            ?: stringResource(R.string.common_unknown)
    }
    val artifactSizeLabel = formatAssetSize(artifact.sizeBytes, context)
    val containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.54f)
    val borderColor = if (recommended) {
        accent.copy(alpha = if (isDark) 0.36f else 0.26f)
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f)
    }
    GitHubActionsSelectableCard(
        selected = false,
        accent = accent,
        isDark = isDark,
        containerColor = containerColor,
        borderColor = borderColor,
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artifactDisplayName(artifactMatch),
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (recommended) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_recommended),
                    color = GitHubStatusPalette.Update,
                    size = AppStatusPillSize.Compact
                )
            }
            if (artifactMatch.lastDownload != null) {
                StatusPill(
                    label = stringResource(R.string.github_actions_badge_last_downloaded),
                    color = GitHubStatusPalette.Active,
                    size = AppStatusPillSize.Compact
                )
            }
        }
        GitHubActionsPillRow {
            StatusPill(
                label = artifactKindLabel(artifactMatch.traits.kind),
                color = accent
            )
            artifactMatch.traits.abi.takeIf { it.isNotBlank() }?.let { abi ->
                StatusPill(label = abi, color = GitHubStatusPalette.Active)
            }
            artifactMatch.traits.flavors.forEach { flavor ->
                StatusPill(label = flavor, color = GitHubStatusPalette.PreRelease)
            }
            artifactMatch.traits.buildTypes
                .filterNot { it == "release" || it == "debug" }
                .forEach { buildType ->
                    StatusPill(
                        label = artifactBuildTypeLabel(buildType),
                        color = GitHubStatusPalette.PreRelease
                    )
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
                text = digestText,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (hasDigest) {
                            Modifier.clickable(onClickLabel = copyDigestLabel) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("sha256", artifact.digest)
                                )
                                Toast.makeText(context, digestCopiedToast, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Modifier
                        }
                    ),
                color = if (hasDigest) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onBackgroundVariant
                },
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
                    downloading = downloading,
                    readyLabel = artifactSizeLabel
                ),
                leadingIcon = appLucideDownloadIcon(),
                enabled = canDownload,
                textColor = accent,
                iconTint = accent,
                modifier = Modifier.widthIn(min = 104.dp),
                onClick = onDownload,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis
            )
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
                icon = appLucideShareIcon(),
                contentDescription = stringResource(
                    R.string.github_actions_cd_share_artifact,
                    artifact.name
                ),
                iconTint = if (canShare) accent else MiuixTheme.colorScheme.onBackgroundVariant,
                width = 54.dp,
                height = 54.dp,
                onClick = onShare
            )
        }
    }
}

@Composable
private fun GitHubActionsArtifactHintText(
    text: String
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun GitHubActionsLoadMoreRunsButton(
    backdrop: LayerBackdrop,
    visibleRunLimit: Int,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        GlassTextButton(
            backdrop = backdrop,
            variant = GlassVariant.SheetAction,
            text = if (loading) {
                stringResource(R.string.common_loading)
            } else {
                stringResource(R.string.github_actions_action_load_more_runs, visibleRunLimit)
            },
            leadingIcon = appLucideRefreshIcon(),
            enabled = !loading,
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
    containerColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val resolvedContainerColor = containerColor ?: if (selected) {
        GitHubStatusPalette.tonedSurface(accent, isDark).copy(alpha = if (isDark) 0.32f else 0.18f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)
    }
    val resolvedBorderColor = borderColor ?: if (selected) {
        accent.copy(alpha = if (isDark) 0.48f else 0.34f)
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.14f)
    }
    SheetSurfaceCard(
        containerColor = resolvedContainerColor,
        borderColor = resolvedBorderColor,
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
    downloading: Boolean,
    readyLabel: String
): String {
    return when {
        downloading -> stringResource(R.string.github_actions_action_downloading)
        !hasToken -> stringResource(R.string.github_actions_action_need_token)
        expired -> stringResource(R.string.github_actions_action_expired)
        !completed -> stringResource(R.string.github_actions_action_wait_run)
        else -> readyLabel
    }
}

private fun shortArtifactDigest(digest: String): String {
    val trimmed = digest.trim()
    val prefix = trimmed.substringBefore(':', missingDelimiterValue = "")
        .takeIf { ':' in trimmed }
        ?.let { "$it:" }
        .orEmpty()
    val value = if (prefix.isBlank()) trimmed else trimmed.substringAfter(':')
    if (value.length <= 18) return trimmed
    return "$prefix${value.take(8)}...${value.takeLast(4)}"
}

private fun artifactDisplayName(match: GitHubActionsArtifactMatch): String {
    val original = match.artifact.name.trim()
    val traits = match.traits
    val display = artifactDisplayTokens(match)
        .sortedByDescending { it.length }
        .fold(stripArtifactExtension(original, traits.extension)) { current, token ->
            stripArtifactToken(current, token)
        }
        .cleanupArtifactDisplayName()
        .takeIf { it.isNotBlank() }
        ?: original
    if (display.length <= 180) return display
    return "${display.take(112)}...${display.takeLast(56)}"
}

private fun artifactDisplayTokens(match: GitHubActionsArtifactMatch): List<String> {
    val traits = match.traits
    return buildList {
        if (traits.extension in artifactDisplayKnownExtensions) {
            add(traits.extension)
        }
        when (traits.kind) {
            GitHubActionsArtifactKind.AndroidPackage -> add("apk")
            GitHubActionsArtifactKind.AndroidBundle -> addAll(listOf("aab", "apks", "apkm"))
            GitHubActionsArtifactKind.Archive -> addAll(listOf("zip", "tar.gz", "tgz"))
            GitHubActionsArtifactKind.Mapping -> addAll(listOf("mapping", "mappings"))
            GitHubActionsArtifactKind.Report -> addAll(listOf("report", "reports"))
            GitHubActionsArtifactKind.Source -> addAll(listOf("source", "sources"))
            GitHubActionsArtifactKind.Unknown -> Unit
        }
        add(traits.abi)
        when (traits.abi) {
            "arm64-v8a" -> add("arm64")
            "armeabi-v7a" -> add("armeabi")
            "x86_64" -> add("x64")
        }
        addAll(traits.flavors)
        addAll(traits.buildTypes)
        if (traits.releaseLike) addAll(listOf("release", "prod", "signed"))
        if (traits.debugLike) addAll(listOf("debug", "dev", "unsigned"))
        if (traits.universalLike) addAll(listOf("universal", "fat", "all"))
    }
        .map { it.trim().trim('.') }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun artifactBuildTypeLabel(buildType: String): String {
    return when (buildType) {
        "qa" -> "QA"
        else -> buildType.replaceFirstChar { char -> char.uppercase() }
    }
}

private fun stripArtifactExtension(value: String, extension: String): String {
    val normalizedExtension = extension.trim().trim('.')
    if (normalizedExtension !in artifactDisplayKnownExtensions) return value
    return value.replace(
        Regex("""(?i)\.${Regex.escape(normalizedExtension)}$"""),
        ""
    )
}

private fun stripArtifactToken(value: String, token: String): String {
    return value.replace(
        Regex("""(?i)(^|[\s._-]+)${Regex.escape(token)}(?=$|[\s._-]+)""")
    ) { matchResult ->
        matchResult.groups[1]?.value.takeIf { !it.isNullOrBlank() }.orEmpty()
    }
}

private fun String.cleanupArtifactDisplayName(): String {
    return trim()
        .replace(Regex("""[\s._-]{2,}"""), "-")
        .trim(' ', '.', '_', '-')
}

private val artifactDisplayKnownExtensions = setOf(
    "apk",
    "aab",
    "apks",
    "apkm",
    "zip",
    "tar.gz",
    "tgz",
    "gz"
)

private const val ACTIONS_MAX_RUN_LIMIT = 30
