package os.kei.ui.page.main.github.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import os.kei.feature.github.model.GitHubActionsArtifactKind
import os.kei.feature.github.model.GitHubActionsRunMatch
import os.kei.feature.github.model.GitHubActionsRunTrackingPlan
import os.kei.feature.github.model.GitHubActionsRunWatchState
import os.kei.feature.github.model.GitHubActionsWorkflowKind
import os.kei.ui.page.main.github.GitHubStatusPalette
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun githubActionsNeutralCardColor(
    isDark: Boolean,
    prominent: Boolean = false
): Color {
    val alpha = when {
        isDark && prominent -> 0.19f
        isDark -> 0.15f
        prominent -> 0.11f
        else -> 0.085f
    }
    return MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = alpha)
}

@Composable
internal fun githubActionsNeutralBorderColor(
    isDark: Boolean,
    prominent: Boolean = false
): Color {
    val alpha = when {
        isDark && prominent -> 0.26f
        isDark -> 0.20f
        prominent -> 0.18f
        else -> 0.14f
    }
    return MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = alpha)
}

@Composable
internal fun githubActionsSecondaryTextColor(isDark: Boolean): Color {
    return if (isDark) {
        MiuixTheme.colorScheme.onBackground.copy(alpha = 0.80f)
    } else {
        MiuixTheme.colorScheme.onBackground.copy(alpha = 0.70f)
    }
}

@Composable
internal fun workflowKindColor(kind: GitHubActionsWorkflowKind): Color {
    return when (kind) {
        GitHubActionsWorkflowKind.AndroidBuild,
        GitHubActionsWorkflowKind.Ci -> GitHubStatusPalette.Active
        GitHubActionsWorkflowKind.Release,
        GitHubActionsWorkflowKind.Quality -> GitHubStatusPalette.Update
        GitHubActionsWorkflowKind.Nightly,
        GitHubActionsWorkflowKind.Dependency,
        GitHubActionsWorkflowKind.Automation -> GitHubStatusPalette.PreRelease
        GitHubActionsWorkflowKind.Localization,
        GitHubActionsWorkflowKind.Documentation,
        GitHubActionsWorkflowKind.Unknown -> MiuixTheme.colorScheme.onBackgroundVariant
    }
}

@Composable
internal fun artifactKindColor(kind: GitHubActionsArtifactKind): Color {
    return when (kind) {
        GitHubActionsArtifactKind.AndroidPackage,
        GitHubActionsArtifactKind.AndroidBundle -> GitHubStatusPalette.Update
        GitHubActionsArtifactKind.Archive,
        GitHubActionsArtifactKind.Source -> GitHubStatusPalette.Active
        GitHubActionsArtifactKind.Mapping,
        GitHubActionsArtifactKind.Report -> GitHubStatusPalette.PreRelease
        GitHubActionsArtifactKind.Unknown -> MiuixTheme.colorScheme.onBackgroundVariant
    }
}

@Composable
internal fun runStatusColor(
    match: GitHubActionsRunMatch,
    trackingPlan: GitHubActionsRunTrackingPlan?
): Color {
    return when (trackingPlan?.state) {
        GitHubActionsRunWatchState.Queued,
        GitHubActionsRunWatchState.Running -> GitHubStatusPalette.Active
        GitHubActionsRunWatchState.Completed -> GitHubStatusPalette.Update
        GitHubActionsRunWatchState.Failed -> GitHubStatusPalette.Error
        GitHubActionsRunWatchState.Unknown,
        null -> when {
            match.traits.inProgress -> GitHubStatusPalette.Active
            match.traits.completed && !match.traits.successful -> GitHubStatusPalette.Error
            match.traits.successful -> GitHubStatusPalette.Update
            match.traits.completed -> GitHubStatusPalette.Update
            else -> MiuixTheme.colorScheme.onBackgroundVariant
        }
    }
}

internal fun artifactBuildTypeColor(buildType: String): Color {
    return when (buildType.lowercase()) {
        "release",
        "prod",
        "production",
        "stable",
        "signed" -> GitHubStatusPalette.Update
        "debug",
        "dev",
        "develop",
        "development",
        "preview",
        "nightly",
        "alpha",
        "beta",
        "canary",
        "snapshot",
        "benchmark",
        "qa",
        "unsigned" -> GitHubStatusPalette.PreRelease
        else -> GitHubStatusPalette.Active
    }
}
