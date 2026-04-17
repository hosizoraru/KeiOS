package com.example.keios.ui.page.main.github.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.ui.page.main.GitHubOverviewMetricItem
import com.example.keios.ui.page.main.GitHubStatusPalette
import com.example.keios.ui.page.main.OverviewRefreshState
import com.example.keios.ui.page.main.color
import com.example.keios.ui.page.main.formatRefreshAgo
import com.example.keios.ui.page.main.indicatorBackground
import com.example.keios.ui.page.main.overviewApiLabel
import com.example.keios.ui.page.main.overviewLabel
import com.example.keios.ui.page.main.borderColor
import com.example.keios.ui.page.main.surfaceColor
import com.example.keios.ui.page.main.widget.AppOverviewCard
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.ui.page.main.widget.StatusPill
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

internal data class GitHubOverviewMetrics(
    val trackedCount: Int,
    val updatableCount: Int,
    val stableLatestCount: Int,
    val preReleaseCount: Int,
    val preReleaseUpdateCount: Int,
    val failedCount: Int
)

@Composable
internal fun GitHubOverviewCard(
    isDark: Boolean,
    lookupConfig: GitHubLookupConfig,
    overviewRefreshState: OverviewRefreshState,
    refreshProgress: Float,
    lastRefreshMs: Long,
    metrics: GitHubOverviewMetrics,
    cardPressFeedbackEnabled: Boolean,
    onRefreshAllTracked: () -> Unit,
    onOpenTrackSheetForAdd: () -> Unit
) {
    val context = LocalContext.current
    val overviewTitleColor = if (isDark) Color.White else MiuixTheme.colorScheme.onBackgroundVariant
    AppOverviewCard(
        title = stringResource(R.string.github_overview_title),
        titleColor = MiuixTheme.colorScheme.onBackground,
        subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
        containerColor = overviewRefreshState.surfaceColor(
            isDark = isDark,
            neutralSurface = MiuixTheme.colorScheme.surface
        ),
        borderColor = overviewRefreshState.borderColor(
            isDark = isDark,
            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
        ),
        contentColor = MiuixTheme.colorScheme.onBackground,
        showIndication = cardPressFeedbackEnabled,
        onClick = onRefreshAllTracked,
        onLongClick = onOpenTrackSheetForAdd,
        headerEndActions = {
            if (overviewRefreshState != OverviewRefreshState.Idle) {
                val indicatorColor = overviewRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
                val indicatorBg = overviewRefreshState.indicatorBackground(
                    neutralSurface = MiuixTheme.colorScheme.surface
                )
                val progressValue = when (overviewRefreshState) {
                    OverviewRefreshState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
                    OverviewRefreshState.Completed,
                    OverviewRefreshState.Cached -> 1f
                    OverviewRefreshState.Idle -> 0f
                }
                CircularProgressIndicator(
                    progress = progressValue,
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = indicatorColor,
                        backgroundColor = indicatorBg
                    )
                )
            }
            StatusPill(
                label = formatRefreshAgo(context = context, lastRefreshMs = lastRefreshMs),
                color = overviewRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                ),
                backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                borderAlphaOverride = if (isDark) 0.35f else 0.42f
            )
            StatusPill(
                label = when (overviewRefreshState) {
                    OverviewRefreshState.Cached -> StatusLabelText.Cached
                    OverviewRefreshState.Refreshing -> StatusLabelText.Checking
                    OverviewRefreshState.Completed -> StatusLabelText.Checked
                    OverviewRefreshState.Idle -> StatusLabelText.PendingCheck
                },
                color = overviewRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap)
            ) {
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_strategy),
                    value = lookupConfig.selectedStrategy.overviewLabel(context),
                    titleColor = overviewTitleColor,
                    valueColor = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_api),
                    value = lookupConfig.overviewApiLabel(context),
                    titleColor = overviewTitleColor,
                    valueColor = if (lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken) {
                        if (lookupConfig.apiToken.isBlank()) {
                            GitHubStatusPalette.PreRelease
                        } else {
                            GitHubStatusPalette.Active
                        }
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap)
            ) {
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_tracked),
                    value = stringResource(R.string.github_overview_value_count, metrics.trackedCount),
                    titleColor = overviewTitleColor,
                    valueColor = if (metrics.trackedCount > 0) {
                        GitHubStatusPalette.Stable
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_stable_update),
                    value = stringResource(R.string.github_overview_value_count, metrics.updatableCount),
                    titleColor = overviewTitleColor,
                    valueColor = if (metrics.updatableCount > 0) {
                        GitHubStatusPalette.Update
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap)
            ) {
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_stable_latest),
                    value = stringResource(R.string.github_overview_value_count, metrics.stableLatestCount),
                    titleColor = overviewTitleColor,
                    valueColor = if (metrics.stableLatestCount > 0) {
                        GitHubStatusPalette.Stable
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_prerelease_tracked),
                    value = stringResource(R.string.github_overview_value_count, metrics.preReleaseCount),
                    titleColor = overviewTitleColor,
                    valueColor = if (metrics.preReleaseCount > 0) {
                        GitHubStatusPalette.PreRelease
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap)
            ) {
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_prerelease_update),
                    value = stringResource(R.string.github_overview_value_count, metrics.preReleaseUpdateCount),
                    titleColor = overviewTitleColor,
                    valueColor = if (metrics.preReleaseUpdateCount > 0) {
                        GitHubStatusPalette.PreRelease
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_check_failed),
                    value = stringResource(R.string.github_overview_value_count, metrics.failedCount),
                    titleColor = overviewTitleColor,
                    valueColor = if (metrics.failedCount > 0) {
                        GitHubStatusPalette.Error
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(name = "GitHub Overview Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun GitHubOverviewCardPreview() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        GitHubOverviewCard(
            isDark = false,
            lookupConfig = GitHubLookupConfig(
                selectedStrategy = GitHubLookupStrategyOption.GitHubApiToken,
                apiToken = "github_pat_preview_token"
            ),
            overviewRefreshState = OverviewRefreshState.Completed,
            refreshProgress = 1f,
            lastRefreshMs = System.currentTimeMillis() - 180_000L,
            metrics = GitHubOverviewMetrics(
                trackedCount = 18,
                updatableCount = 4,
                stableLatestCount = 11,
                preReleaseCount = 3,
                preReleaseUpdateCount = 2,
                failedCount = 1
            ),
            cardPressFeedbackEnabled = true,
            onRefreshAllTracked = {},
            onOpenTrackSheetForAdd = {}
        )
    }
}
