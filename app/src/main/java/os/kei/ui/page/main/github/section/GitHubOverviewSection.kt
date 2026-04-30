package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.ui.page.main.github.GitHubOverviewMetricItem
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.color
import os.kei.ui.page.main.github.formatRefreshAgo
import os.kei.ui.page.main.github.indicatorBackground
import os.kei.ui.page.main.github.overviewApiLabel
import os.kei.ui.page.main.github.overviewLabel
import os.kei.ui.page.main.github.borderColor
import os.kei.ui.page.main.github.surfaceColor
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.status.StatusPill
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

private fun overviewMetricColor(
    color: Color,
    emphasized: Boolean,
    isDark: Boolean
): Color {
    return if (emphasized) {
        color
    } else {
        color.copy(alpha = if (isDark) 0.76f else 0.84f)
    }
}

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
    val displayRefreshState = if (
        overviewRefreshState == OverviewRefreshState.Idle && lastRefreshMs > 0L
    ) {
        OverviewRefreshState.Cached
    } else {
        overviewRefreshState
    }
    AppOverviewCard(
        title = stringResource(R.string.github_overview_title),
        titleColor = MiuixTheme.colorScheme.onBackground,
        subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
        containerColor = displayRefreshState.surfaceColor(
            isDark = isDark,
            neutralSurface = MiuixTheme.colorScheme.surface
        ),
        borderColor = displayRefreshState.borderColor(
            isDark = isDark,
            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
        ),
        contentColor = MiuixTheme.colorScheme.onBackground,
        showIndication = cardPressFeedbackEnabled,
        onClick = onRefreshAllTracked,
        onLongClick = onOpenTrackSheetForAdd,
        headerEndActions = {
            if (displayRefreshState != OverviewRefreshState.Idle) {
                val indicatorColor = displayRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
                val indicatorBg = displayRefreshState.indicatorBackground(
                    neutralSurface = MiuixTheme.colorScheme.surface
                )
                val progressValue = when (displayRefreshState) {
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
                color = displayRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                ),
                backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                borderAlphaOverride = if (isDark) 0.35f else 0.42f
            )
            StatusPill(
                label = when (displayRefreshState) {
                    OverviewRefreshState.Cached -> stringResource(R.string.common_status_cached)
                    OverviewRefreshState.Refreshing -> stringResource(R.string.common_status_checking)
                    OverviewRefreshState.Completed -> stringResource(R.string.common_status_checked)
                    OverviewRefreshState.Idle -> stringResource(R.string.common_status_pending_check)
                },
                color = displayRefreshState.color(
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
                    valueColor = lookupConfig.selectedStrategy.run {
                        when (this) {
                            GitHubLookupStrategyOption.AtomFeed -> GitHubStatusPalette.Active
                            GitHubLookupStrategyOption.GitHubApiToken -> GitHubStatusPalette.Update
                        }
                    },
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
                        overviewMetricColor(
                            color = GitHubStatusPalette.Active,
                            emphasized = false,
                            isDark = isDark
                        )
                    },
                    labelMaxLines = 1,
                    valueMaxLines = 1,
                    labelWeight = 0.24f,
                    valueWeight = 0.76f,
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
                    valueColor = overviewMetricColor(
                        color = GitHubStatusPalette.Stable,
                        emphasized = metrics.trackedCount > 0,
                        isDark = isDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_stable_update),
                    value = stringResource(R.string.github_overview_value_count, metrics.updatableCount),
                    titleColor = overviewTitleColor,
                    valueColor = overviewMetricColor(
                        color = GitHubStatusPalette.Update,
                        emphasized = metrics.updatableCount > 0,
                        isDark = isDark
                    ),
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
                    valueColor = overviewMetricColor(
                        color = GitHubStatusPalette.Stable,
                        emphasized = metrics.stableLatestCount > 0,
                        isDark = isDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_prerelease_tracked),
                    value = stringResource(R.string.github_overview_value_count, metrics.preReleaseCount),
                    titleColor = overviewTitleColor,
                    valueColor = overviewMetricColor(
                        color = GitHubStatusPalette.PreRelease,
                        emphasized = metrics.preReleaseCount > 0,
                        isDark = isDark
                    ),
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
                    valueColor = overviewMetricColor(
                        color = GitHubStatusPalette.PreRelease,
                        emphasized = metrics.preReleaseUpdateCount > 0,
                        isDark = isDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                GitHubOverviewMetricItem(
                    label = stringResource(R.string.github_overview_label_check_failed),
                    value = stringResource(R.string.github_overview_value_count, metrics.failedCount),
                    titleColor = overviewTitleColor,
                    valueColor = overviewMetricColor(
                        color = GitHubStatusPalette.Error,
                        emphasized = metrics.failedCount > 0,
                        isDark = isDark
                    ),
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
