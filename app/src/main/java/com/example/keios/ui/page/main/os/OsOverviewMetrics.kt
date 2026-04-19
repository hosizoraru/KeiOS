package com.example.keios.ui.page.main

import android.content.Context
import com.example.keios.R

internal data class OsActivityOverviewStats(
    val totalCount: Int,
    val visibleCount: Int,
    val packageConfiguredCount: Int,
    val explicitClassCount: Int,
    val totalExtrasCount: Int,
    val extrasConfiguredCardCount: Int
)

internal fun buildOsActivityOverviewStats(
    cards: List<OsActivityShortcutCard>,
    defaults: OsGoogleSystemServiceConfig
): OsActivityOverviewStats {
    var packageConfiguredCount = 0
    var explicitClassCount = 0
    var totalExtrasCount = 0
    var extrasConfiguredCardCount = 0
    cards.forEach { card ->
        val normalized = normalizeActivityShortcutConfig(
            config = card.config,
            defaults = defaults
        )
        if (normalized.packageName.isNotBlank()) packageConfiguredCount++
        if (normalized.className.isNotBlank()) explicitClassCount++
        val extrasCount = normalized.intentExtras.size
        if (extrasCount > 0) {
            extrasConfiguredCardCount++
            totalExtrasCount += extrasCount
        }
    }
    return OsActivityOverviewStats(
        totalCount = cards.size,
        visibleCount = cards.count { it.visible },
        packageConfiguredCount = packageConfiguredCount,
        explicitClassCount = explicitClassCount,
        totalExtrasCount = totalExtrasCount,
        extrasConfiguredCardCount = extrasConfiguredCardCount
    )
}

internal fun buildOsOverviewMetrics(
    context: Context,
    visibleRowsCount: Int,
    totalRowsCount: Int,
    topInfoCount: Int,
    loadedFreshCount: Int,
    cachedSectionCount: Int,
    sectionCount: Int,
    activityStats: OsActivityOverviewStats
): List<OsOverviewMetric> {
    return listOf(
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_rows_compact),
            value = context.getString(
                R.string.os_overview_metric_rows_compact_value,
                visibleRowsCount,
                totalRowsCount,
                topInfoCount
            )
        ),
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_section_sync),
            value = context.getString(
                R.string.os_overview_metric_section_sync_value,
                loadedFreshCount,
                sectionCount,
                cachedSectionCount
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
            label = context.getString(R.string.os_overview_metric_activity_targets),
            value = context.getString(
                R.string.os_overview_metric_activity_targets_value,
                activityStats.packageConfiguredCount,
                activityStats.explicitClassCount
            )
        ),
        OsOverviewMetric(
            label = context.getString(R.string.os_overview_metric_activity_extras),
            value = context.getString(
                R.string.os_overview_metric_activity_extras_value,
                activityStats.totalExtrasCount,
                activityStats.extrasConfiguredCardCount
            )
        )
    )
}
