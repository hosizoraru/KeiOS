package os.kei.ui.page.main.os

import android.content.Context
import androidx.compose.ui.graphics.Color
import os.kei.R
import os.kei.ui.page.main.os.components.OsOverviewMetric
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard

internal data class OsActivityOverviewStats(
    val totalCount: Int,
    val visibleCount: Int
)

internal data class OsShellOverviewStats(
    val totalCount: Int,
    val visibleCount: Int
)

internal data class OsOverviewUiState(
    val overviewState: SystemOverviewState,
    val statusLabel: String,
    val statusColor: Color,
    val overviewCardColor: Color,
    val overviewBorderColor: Color,
    val indicatorProgress: Float,
    val indicatorBg: Color,
    val metrics: List<OsOverviewMetric>
)

internal fun buildOsActivityOverviewStats(
    cards: List<OsActivityShortcutCard>
): OsActivityOverviewStats {
    return OsActivityOverviewStats(
        totalCount = cards.size,
        visibleCount = cards.count { it.visible }
    )
}

internal fun buildOsOverviewMetrics(
    context: Context,
    topInfoCount: Int,
    visibleRowsCount: Int,
    visibleParameterCardCount: Int,
    totalParameterCardCount: Int,
    activityStats: OsActivityOverviewStats,
    shellStats: OsShellOverviewStats
): List<OsOverviewMetric> {
    return listOf(
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_top_info),
            value = context.getString(
                R.string.os_overview_metric_top_info_value,
                topInfoCount,
                visibleRowsCount
            )
        ),
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_rows_compact),
            value = context.getString(
                R.string.os_overview_metric_rows_compact_value,
                visibleParameterCardCount,
                totalParameterCardCount
            )
        ),
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_activity_cards),
            value = context.getString(
                R.string.os_overview_metric_activity_cards_value,
                activityStats.visibleCount,
                activityStats.totalCount
            )
        ),
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_shell_cards),
            value = context.getString(
                R.string.os_overview_metric_shell_cards_value,
                shellStats.visibleCount,
                shellStats.totalCount
            )
        )
    )
}

internal fun buildOsOverviewUiState(
    context: Context,
    isDark: Boolean,
    inactiveColor: Color,
    cachedColor: Color,
    refreshingColor: Color,
    syncedColor: Color,
    surfaceColor: Color,
    refreshing: Boolean,
    refreshProgress: Float,
    cachePersisted: Boolean,
    visibleCards: Set<OsSectionCard>,
    sectionStates: Map<SectionKind, SectionState>,
    topInfoCount: Int,
    visibleRowsCount: Int,
    activityCards: List<OsActivityShortcutCard>,
    shellCommandCards: List<OsShellCommandCard>
): OsOverviewUiState {
    val currentVisibleSectionKinds = visibleSectionKinds(visibleCards)
    val visibleSectionStates = currentVisibleSectionKinds.mapNotNull { sectionStates[it] }
    val loadedFreshCount = visibleSectionStates.count { it.loadedFresh }
    val cachedSectionCount = visibleSectionStates.count { !it.loadedFresh && it.rows.isNotEmpty() }
    val sectionCount = currentVisibleSectionKinds.size
    val hasTopInfoCard = visibleCards.contains(OsSectionCard.TOP_INFO)
    val overviewState = when {
        refreshing -> SystemOverviewState.Refreshing
        loadedFreshCount == sectionCount && sectionCount > 0 -> SystemOverviewState.Completed
        cachePersisted || cachedSectionCount > 0 -> SystemOverviewState.Cached
        sectionCount == 0 && hasTopInfoCard -> SystemOverviewState.Completed
        else -> SystemOverviewState.Idle
    }
    val statusLabel = when (overviewState) {
        SystemOverviewState.Cached -> context.getString(R.string.common_status_cached)
        SystemOverviewState.Refreshing -> context.getString(R.string.common_status_syncing)
        SystemOverviewState.Completed -> context.getString(R.string.common_synced)
        SystemOverviewState.Idle -> context.getString(R.string.common_status_pending_sync)
    }
    val statusColor = when (overviewState) {
        SystemOverviewState.Cached -> cachedColor
        SystemOverviewState.Refreshing -> refreshingColor
        SystemOverviewState.Completed -> syncedColor
        SystemOverviewState.Idle -> inactiveColor
    }
    val overviewCardColor = if (isDark) {
        statusColor.copy(alpha = 0.16f)
    } else {
        statusColor.copy(alpha = 0.09f)
    }
    val overviewBorderColor = if (isDark) {
        statusColor.copy(alpha = 0.32f)
    } else {
        statusColor.copy(alpha = 0.26f)
    }
    val indicatorProgress = when (overviewState) {
        SystemOverviewState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
        SystemOverviewState.Completed,
        SystemOverviewState.Cached -> 1f
        SystemOverviewState.Idle -> 0f
    }
    val indicatorBg = when (overviewState) {
        SystemOverviewState.Refreshing -> Color(0x553B82F6)
        SystemOverviewState.Completed -> Color(0x5522C55E)
        SystemOverviewState.Cached -> Color(0x55F59E0B)
        SystemOverviewState.Idle -> surfaceColor
    }
    val totalParameterCardCount = OsSectionCard.entries.count {
        it != OsSectionCard.GOOGLE_SYSTEM_SERVICE &&
            it != OsSectionCard.SHELL_RUNNER
    }
    val visibleParameterCardCount = visibleCards.count {
        it != OsSectionCard.GOOGLE_SYSTEM_SERVICE &&
            it != OsSectionCard.SHELL_RUNNER
    }
    val activityOverviewStats = buildOsActivityOverviewStats(cards = activityCards)
    val shellOverviewStats = OsShellOverviewStats(
        totalCount = shellCommandCards.size + 1,
        visibleCount = shellCommandCards.count { it.visible } +
            if (visibleCards.contains(OsSectionCard.SHELL_RUNNER)) 1 else 0
    )
    val metrics = buildOsOverviewMetrics(
        context = context,
        topInfoCount = topInfoCount,
        visibleRowsCount = visibleRowsCount,
        visibleParameterCardCount = visibleParameterCardCount,
        totalParameterCardCount = totalParameterCardCount,
        activityStats = activityOverviewStats,
        shellStats = shellOverviewStats
    )
    return OsOverviewUiState(
        overviewState = overviewState,
        statusLabel = statusLabel,
        statusColor = statusColor,
        overviewCardColor = overviewCardColor,
        overviewBorderColor = overviewBorderColor,
        indicatorProgress = indicatorProgress,
        indicatorBg = indicatorBg,
        metrics = metrics
    )
}
