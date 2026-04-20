package com.example.keios.ui.page.main.github

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.model.GitHubApiAuthMode
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubStrategyBenchmarkResult
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.sheet.SheetChoiceCard
import com.example.keios.ui.page.main.widget.sheet.SheetExpandableCard
import com.example.keios.ui.page.main.widget.sheet.SheetSummaryCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubStrategyGuideCard(
    guide: GitHubStrategyGuide,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val accent = guide.option.accentColor()
    SheetChoiceCard(
        title = guide.option.label,
        summary = guide.summary,
        selected = selected,
        onSelect = onSelect,
        accentColor = accent,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        details = {
            Text(
                text = stringResource(R.string.github_strategy_guide_pros, guide.pros.joinToString("；")),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight
            )
            Text(
                text = stringResource(R.string.github_strategy_guide_cons, guide.cons.joinToString("；")),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight
            )
            Text(
                text = stringResource(R.string.github_strategy_guide_requirement, guide.requirement),
                color = accent,
                fontWeight = FontWeight.Medium,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight
            )
        }
    )
}

@Composable
internal fun GitHubStrategyDraftSummaryCard(
    selectedStrategy: GitHubLookupStrategyOption,
    tokenInput: String,
    trackedCount: Int,
    changed: Boolean
) {
    val accent = selectedStrategy.accentColor()
    val tokenStatusLabel = when {
        selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken -> stringResource(R.string.common_not_used)
        tokenInput.isNotBlank() -> stringResource(R.string.common_filled)
        else -> stringResource(R.string.common_guest)
    }
    val tokenStatusColor = when {
        selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken -> MiuixTheme.colorScheme.onBackgroundVariant
        tokenInput.isNotBlank() -> GitHubStatusPalette.Update
        else -> GitHubStatusPalette.PreRelease
    }

    SheetSummaryCard(
        title = stringResource(R.string.github_strategy_card_title_draft),
        accentColor = MiuixTheme.colorScheme.onBackground,
        badgeLabel = if (changed) {
            stringResource(R.string.common_pending_save)
        } else {
            stringResource(R.string.github_strategy_badge_same)
        },
        badgeColor = if (changed) accent else MiuixTheme.colorScheme.onBackgroundVariant,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (changed) 0.82f else 0.66f),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_option),
            value = selectedStrategy.label,
            valueColor = accent,
            emphasized = true,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_token),
            value = tokenStatusLabel,
            valueColor = tokenStatusColor,
            emphasized = selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_impact),
            value = if (trackedCount > 0) {
                stringResource(R.string.github_strategy_impact_recheck_count, trackedCount)
            } else {
                stringResource(R.string.github_strategy_impact_no_track)
            },
            valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
            titleMinWidth = 44.dp
        )
    }
}

@Composable
internal fun GitHubRecommendedTokenGuideCard(
    guide: GitHubRecommendedTokenGuide,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    SheetExpandableCard(
        title = stringResource(R.string.github_strategy_card_title_recommended),
        collapsedSummary = guide.collapsedSummary,
        expandedSummary = guide.summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        accentColor = GitHubStatusPalette.Update,
        badgeLabel = stringResource(R.string.github_strategy_badge_least_privilege),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        val accent = GitHubStatusPalette.Update
        guide.fields.forEach { field ->
            GitHubCompactInfoRow(
                label = field.label,
                value = field.value,
                valueColor = if (field.emphasized) accent else MiuixTheme.colorScheme.onBackground,
                emphasized = field.emphasized,
                titleMinWidth = 52.dp
            )
        }
        guide.notes.forEach { note ->
            Text(
                text = note,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight
            )
        }
    }
}

@Composable
internal fun GitHubStrategyBenchmarkCard(
    result: GitHubStrategyBenchmarkResult
) {
    val accent = when (result.displayName) {
        "API" -> GitHubStatusPalette.Update
        else -> GitHubStatusPalette.Active
    }
    SheetSummaryCard(
        title = result.summaryLabel,
        accentColor = accent,
        badgeLabel = "${result.coldSuccessCount}/${result.totalTargets}",
        badgeColor = if (result.failures.isEmpty()) GitHubStatusPalette.Update else GitHubStatusPalette.PreRelease,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        GitHubOverviewMetricItem(
            label = stringResource(R.string.github_strategy_metric_cold_avg),
            value = "${result.coldAverageMs} ms",
            valueColor = accent
        )
        GitHubOverviewMetricItem(
            label = stringResource(R.string.github_strategy_metric_warm_avg),
            value = "${result.warmAverageMs} ms",
            valueColor = GitHubStatusPalette.Stable
        )
        GitHubOverviewMetricItem(
            label = stringResource(R.string.github_strategy_metric_cache_hit),
            value = "${result.cacheHitCount}/${result.warmSamples.size}",
            valueColor = if (result.cacheHitCount == result.warmSamples.size) {
                GitHubStatusPalette.Update
            } else {
                GitHubStatusPalette.PreRelease
            }
        )
        GitHubOverviewMetricItem(
            label = stringResource(R.string.github_strategy_metric_failed),
            value = "${result.failures.size}",
            valueColor = if (result.failures.isEmpty()) {
                MiuixTheme.colorScheme.onBackgroundVariant
            } else {
                GitHubStatusPalette.Error
            }
        )
        if (result.failures.isNotEmpty()) {
            Text(
                text = result.failures.take(2).joinToString("\n"),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun GitHubCredentialStatusCard(
    status: GitHubApiCredentialStatus
) {
    val context = LocalContext.current
    val accent = when (status.authMode) {
        GitHubApiAuthMode.Token -> GitHubStatusPalette.Update
        GitHubApiAuthMode.Guest -> GitHubStatusPalette.PreRelease
    }
    SheetSummaryCard(
        title = stringResource(R.string.github_strategy_card_title_credential_status),
        accentColor = accent,
        badgeLabel = status.summaryLabel,
        badgeColor = accent,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    ) {
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_mode),
            value = status.authMode.label,
            valueColor = accent,
            emphasized = true,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_quota),
            value = "${status.coreRemaining} / ${status.coreLimit}",
            valueColor = if (status.coreRemaining > 0) accent else GitHubStatusPalette.Error,
            emphasized = true,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_used),
            value = status.coreUsed.toString(),
            valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = stringResource(R.string.github_strategy_label_reset),
            value = formatFutureEta(context, status.resetAtMillis),
            valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
            titleMinWidth = 44.dp
        )
    }
}

internal fun GitHubLookupStrategyOption.accentColor(): Color {
    return when (this) {
        GitHubLookupStrategyOption.AtomFeed -> GitHubStatusPalette.Active
        GitHubLookupStrategyOption.GitHubApiToken -> GitHubStatusPalette.Update
    }
}
