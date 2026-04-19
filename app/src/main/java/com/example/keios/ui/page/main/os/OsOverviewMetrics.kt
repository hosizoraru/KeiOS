package com.example.keios.ui.page.main

import android.content.Context
import com.example.keios.R

internal data class OsActivityOverviewStats(
    val totalCount: Int,
    val visibleCount: Int
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
    activityStats: OsActivityOverviewStats
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
        )
    )
}
