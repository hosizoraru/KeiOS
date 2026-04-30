package os.kei.ui.page.main.github

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import androidx.compose.runtime.Composable
import os.kei.R
import os.kei.ui.page.main.os.appLucideAlertIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideWarningIcon

internal object GitHubStatusPalette {
    val Active = Color(0xFF3B82F6)
    val Stable = Color(0xFF3B82F6)
    val Update = Color(0xFF22C55E)
    val PreRelease = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)

    fun tonedSurface(color: Color, isDark: Boolean): Color {
        return color.copy(alpha = if (isDark) 0.20f else 0.11f)
    }
}

internal fun VersionCheckUi.isFailed(): Boolean = failed ||
    GitHubTrackedReleaseStatus.isFailureMessage(message)

internal fun VersionCheckUi.isLocalAppUninstalled(): Boolean {
    val normalizedLocalVersion = localVersion.trim()
    return localVersionCode < 0L &&
        (normalizedLocalVersion.isBlank() || normalizedLocalVersion.equals("unknown", ignoreCase = true))
}

internal fun OverviewRefreshState.color(neutralColor: Color): Color {
    return when (this) {
        OverviewRefreshState.Refreshing -> GitHubStatusPalette.Active
        OverviewRefreshState.Completed -> GitHubStatusPalette.Update
        OverviewRefreshState.Cached -> GitHubStatusPalette.PreRelease
        OverviewRefreshState.Idle -> neutralColor
    }
}

internal fun OverviewRefreshState.surfaceColor(
    isDark: Boolean,
    neutralSurface: Color
): Color {
    return when (this) {
        OverviewRefreshState.Refreshing -> GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Active, isDark)
        OverviewRefreshState.Completed -> GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Update, isDark)
        OverviewRefreshState.Cached -> GitHubStatusPalette.tonedSurface(GitHubStatusPalette.PreRelease, isDark)
        OverviewRefreshState.Idle -> neutralSurface.copy(alpha = 0.66f)
    }
}

internal fun OverviewRefreshState.borderColor(
    isDark: Boolean,
    neutralColor: Color
): Color {
    val accent = when (this) {
        OverviewRefreshState.Refreshing -> GitHubStatusPalette.Active
        OverviewRefreshState.Completed -> GitHubStatusPalette.Update
        OverviewRefreshState.Cached -> GitHubStatusPalette.PreRelease
        OverviewRefreshState.Idle -> neutralColor
    }
    return if (isDark) {
        accent.copy(alpha = if (this == OverviewRefreshState.Idle) 0.22f else 0.40f)
    } else {
        accent.copy(alpha = if (this == OverviewRefreshState.Idle) 0.16f else 0.34f)
    }
}

internal fun OverviewRefreshState.indicatorBackground(neutralSurface: Color): Color {
    return when (this) {
        OverviewRefreshState.Refreshing -> GitHubStatusPalette.Active.copy(alpha = 0.33f)
        OverviewRefreshState.Completed -> GitHubStatusPalette.Update.copy(alpha = 0.33f)
        OverviewRefreshState.Cached -> GitHubStatusPalette.PreRelease.copy(alpha = 0.33f)
        OverviewRefreshState.Idle -> neutralSurface
    }
}

@Composable
internal fun VersionCheckUi.statusIcon(): ImageVector {
    return when {
        loading -> appLucideRefreshIcon()
        isLocalAppUninstalled() -> appLucideAlertIcon()
        isFailed() -> appLucideWarningIcon()
        recommendsPreRelease -> appLucideDownloadIcon()
        hasPreReleaseUpdate -> appLucideDownloadIcon()
        hasUpdate == true -> appLucideDownloadIcon()
        hasUpdate == false -> appLucideConfirmIcon()
        isPreRelease -> appLucideAlertIcon()
        else -> appLucideMoreIcon()
    }
}

internal fun VersionCheckUi.statusColor(neutralColor: Color): Color {
    return when {
        loading -> GitHubStatusPalette.Active
        isFailed() -> GitHubStatusPalette.Error
        recommendsPreRelease -> GitHubStatusPalette.PreRelease
        hasPreReleaseUpdate -> GitHubStatusPalette.PreRelease
        hasUpdate == true -> GitHubStatusPalette.Update
        hasUpdate == false && isPreRelease -> GitHubStatusPalette.PreRelease
        hasUpdate == false -> GitHubStatusPalette.Stable
        isPreRelease -> GitHubStatusPalette.PreRelease
        isLocalAppUninstalled() -> neutralColor
        else -> neutralColor
    }
}

internal fun VersionCheckUi.statusMessage(context: Context): String {
    val rawMessage = message.trim()
    val status = GitHubTrackedReleaseStatus.fromMessage(rawMessage)
    return when {
        status == GitHubTrackedReleaseStatus.UpdateAvailable ->
            context.getString(R.string.github_status_update_available)
        status == GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable ->
            context.getString(R.string.github_status_prerelease_update_available)
        status == GitHubTrackedReleaseStatus.PreReleaseOptional ->
            context.getString(R.string.github_status_prerelease_optional)
        status == GitHubTrackedReleaseStatus.PreReleaseTracked ->
            context.getString(R.string.github_status_prerelease_tracked)
        status == GitHubTrackedReleaseStatus.UpToDate ->
            context.getString(R.string.github_status_up_to_date)
        status == GitHubTrackedReleaseStatus.MatchedRelease ->
            context.getString(R.string.github_status_matched_release)
        status == GitHubTrackedReleaseStatus.ComparisonUncertain ->
            context.getString(R.string.github_status_comparison_uncertain)
        GitHubTrackedReleaseStatus.isOnlyPreReleasesHint(rawMessage) ->
            context.getString(R.string.github_status_only_prereleases_hint)
        status == GitHubTrackedReleaseStatus.Failed ->
            GitHubTrackedReleaseStatus.localizedFailureDetail(
                rawMessage,
                context.getString(R.string.github_check_failed_prefix)
            )
        else -> {
            rawMessage
        }
    }
}

internal fun githubReleaseHintMessage(context: Context, rawHint: String): String {
    val hint = rawHint.trim()
    return if (GitHubTrackedReleaseStatus.isOnlyPreReleasesHint(hint)) {
        context.getString(R.string.github_status_only_prereleases_hint)
    } else {
        hint
    }
}

internal fun VersionCheckUi.stableVersionColor(neutralColor: Color): Color {
    return when {
        hasUpdate == true && !recommendsPreRelease -> GitHubStatusPalette.Update
        hasUpdate == false -> GitHubStatusPalette.Stable
        else -> neutralColor
    }
}

internal fun VersionCheckUi.preReleaseVersionColor(neutralColor: Color): Color {
    return when {
        recommendsPreRelease -> GitHubStatusPalette.PreRelease
        hasPreReleaseUpdate -> GitHubStatusPalette.PreRelease
        isPreRelease -> GitHubStatusPalette.PreRelease
        else -> neutralColor
    }
}
