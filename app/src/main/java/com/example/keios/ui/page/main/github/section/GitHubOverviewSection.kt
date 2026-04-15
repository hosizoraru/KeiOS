package com.example.keios.ui.page.main.github.section

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.ui.page.main.widget.StatusPill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val overviewShape = RoundedCornerShape(16.dp)
    val overviewTitleColor = if (isDark) Color.White else MiuixTheme.colorScheme.onBackgroundVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(overviewShape)
            .background(
                overviewRefreshState.surfaceColor(
                    isDark = isDark,
                    neutralSurface = MiuixTheme.colorScheme.surface
                ),
                overviewShape
            )
            .border(
                width = 1.dp,
                color = overviewRefreshState.borderColor(
                    isDark = isDark,
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                ),
                shape = overviewShape
            ),
        colors = CardDefaults.defaultColors(
            color = overviewRefreshState.surfaceColor(
                isDark = isDark,
                neutralSurface = MiuixTheme.colorScheme.surface
            ),
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        showIndication = cardPressFeedbackEnabled,
        onClick = onRefreshAllTracked
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onRefreshAllTracked,
                    onLongClick = onOpenTrackSheetForAdd
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.github_overview_title), color = MiuixTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.weight(1f))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
