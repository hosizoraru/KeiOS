package com.example.keios.ui.page.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.keios.feature.github.model.GitHubTrackedReleaseStatus
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Update

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
    message.startsWith(GitHubTrackedReleaseStatus.Failed.defaultMessage)

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

internal fun VersionCheckUi.statusIcon(): ImageVector {
    return when {
        loading -> MiuixIcons.Regular.Refresh
        isFailed() -> MiuixIcons.Regular.Report
        recommendsPreRelease -> MiuixIcons.Regular.Update
        hasPreReleaseUpdate -> MiuixIcons.Regular.Report
        hasUpdate == true -> MiuixIcons.Regular.Update
        hasUpdate == false -> MiuixIcons.Regular.Ok
        isPreRelease -> MiuixIcons.Regular.Report
        else -> MiuixIcons.Regular.More
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
        else -> neutralColor
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
