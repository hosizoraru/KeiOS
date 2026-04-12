package com.example.keios.ui.page.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        return color.copy(alpha = if (isDark) 0.33f else 0.20f)
    }
}

internal fun VersionCheckUi.isFailed(): Boolean = message.startsWith("检查失败")

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
        hasPreReleaseUpdate -> MiuixIcons.Regular.Update
        hasUpdate == true -> MiuixIcons.Regular.Update
        isPreRelease -> MiuixIcons.Regular.Report
        hasUpdate == false -> MiuixIcons.Regular.Ok
        else -> MiuixIcons.Regular.More
    }
}

internal fun VersionCheckUi.statusColor(neutralColor: Color): Color {
    return when {
        loading -> GitHubStatusPalette.Active
        isFailed() -> GitHubStatusPalette.Error
        hasPreReleaseUpdate -> GitHubStatusPalette.PreRelease
        hasUpdate == true -> GitHubStatusPalette.Update
        isPreRelease -> GitHubStatusPalette.PreRelease
        hasUpdate == false -> GitHubStatusPalette.Stable
        else -> neutralColor
    }
}

internal fun VersionCheckUi.stableVersionColor(neutralColor: Color): Color {
    return when {
        hasUpdate == true -> GitHubStatusPalette.Update
        hasUpdate == false -> GitHubStatusPalette.Stable
        else -> neutralColor
    }
}

internal fun VersionCheckUi.preReleaseVersionColor(neutralColor: Color): Color {
    return when {
        hasPreReleaseUpdate -> GitHubStatusPalette.PreRelease
        isPreRelease -> GitHubStatusPalette.PreRelease
        else -> neutralColor
    }
}
