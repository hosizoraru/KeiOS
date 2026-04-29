package os.kei.ui.page.main.github.actions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.feature.github.model.GitHubActionsArtifactKind
import os.kei.feature.github.model.GitHubActionsRunBranchTrust
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunTrackingPlan
import os.kei.feature.github.model.GitHubActionsRunWatchState
import os.kei.feature.github.model.GitHubActionsWorkflowKind
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun workflowKindLabel(kind: GitHubActionsWorkflowKind): String {
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
internal fun artifactKindLabel(kind: GitHubActionsArtifactKind): String {
    return when (kind) {
        GitHubActionsArtifactKind.AndroidPackage -> stringResource(R.string.github_actions_badge_apk)
        GitHubActionsArtifactKind.AndroidBundle -> stringResource(R.string.github_actions_badge_aab)
        GitHubActionsArtifactKind.Archive -> stringResource(R.string.github_actions_badge_zip)
        GitHubActionsArtifactKind.Mapping -> stringResource(R.string.github_actions_badge_mapping)
        GitHubActionsArtifactKind.Report -> stringResource(R.string.github_actions_badge_report)
        GitHubActionsArtifactKind.Source -> stringResource(R.string.github_actions_badge_source)
        GitHubActionsArtifactKind.Unknown -> stringResource(R.string.github_actions_badge_artifact)
    }
}

@Composable
internal fun runBranchTrustPill(match: GitHubActionsRunMatch) {
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
        GitHubActionsRunBranchTrust.ReleaseBranch -> GitHubStatusPalette.Update
        GitHubActionsRunBranchTrust.PullRequest -> GitHubStatusPalette.Error
        GitHubActionsRunBranchTrust.FeatureBranch -> GitHubStatusPalette.PreRelease
        GitHubActionsRunBranchTrust.Unknown -> MiuixTheme.colorScheme.onBackgroundVariant
    }
    GitHubActionsInfoPill(
        label = label,
        color = color,
        emphasized = trust == GitHubActionsRunBranchTrust.DefaultBranch ||
            trust == GitHubActionsRunBranchTrust.ReleaseTag,
        minWidth = GitHubActionsShortPillMinWidth
    )
}

@Composable
internal fun runStatusLabel(
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

@Composable
internal fun buildRunSubtitle(
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
internal fun artifactDownloadLabel(
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
