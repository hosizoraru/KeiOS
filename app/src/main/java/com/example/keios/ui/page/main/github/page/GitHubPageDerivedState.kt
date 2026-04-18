package com.example.keios.ui.page.main

import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.section.GitHubOverviewMetrics

internal data class GitHubPageDerivedState(
    val filteredTracked: List<GitHubTrackedApp> = emptyList(),
    val sortedTracked: List<GitHubTrackedApp> = emptyList(),
    val overviewMetrics: GitHubOverviewMetrics = GitHubOverviewMetrics(
        trackedCount = 0,
        updatableCount = 0,
        stableLatestCount = 0,
        preReleaseCount = 0,
        preReleaseUpdateCount = 0,
        failedCount = 0
    )
)
