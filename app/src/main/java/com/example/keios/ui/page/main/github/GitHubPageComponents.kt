package com.example.keios.ui.page.main

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.feature.github.data.local.AppIconCache
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubStrategyBenchmarkResult
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.InstalledAppItem
import com.example.keios.ui.page.main.widget.AppInfoRow
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.SheetChoiceCard
import com.example.keios.ui.page.main.widget.SheetExpandableCard
import com.example.keios.ui.page.main.widget.SheetSurfaceCard
import com.example.keios.ui.page.main.widget.SheetSummaryCard
import com.example.keios.ui.page.main.widget.StatusPill
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubCompactInfoRow(
    label: String,
    value: String,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    emphasized: Boolean = false,
    titleMinWidth: Dp = 64.dp,
    onClick: (() -> Unit)? = null
) {
    AppInfoRow(
        label = label,
        value = value,
        labelColor = titleColor,
        valueColor = valueColor,
        labelMinWidth = titleMinWidth,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.metricCardTextGap,
        valueTextAlign = TextAlign.End,
        valueMaxLines = 4,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = 13.sp,
        labelLineHeight = 18.sp,
        valueFontSize = 13.sp,
        valueLineHeight = 18.sp,
        emphasizedValue = emphasized,
        onClick = onClick
    )
}

@Composable
internal fun VersionValueRow(
    label: String,
    value: String,
    valueColor: Color,
    emphasized: Boolean = false
) {
    GitHubCompactInfoRow(
        label = label,
        value = value,
        valueColor = valueColor,
        titleColor = MiuixTheme.colorScheme.primary,
        emphasized = emphasized,
        titleMinWidth = 52.dp
    )
}

@Composable
internal fun GitHubOverviewMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    titleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    emphasized: Boolean = false
) {
    AppInfoRow(
        label = label,
        value = value.ifBlank { stringResource(R.string.common_na) },
        modifier = modifier,
        labelColor = titleColor,
        valueColor = valueColor,
        labelWeight = 0.46f,
        valueWeight = 0.54f,
        horizontalSpacing = CardLayoutRhythm.infoRowGap,
        rowVerticalPadding = CardLayoutRhythm.metricCardTextGap,
        valueTextAlign = TextAlign.End,
        labelMaxLines = 1,
        valueMaxLines = 1,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = 13.sp,
        labelLineHeight = 18.sp,
        valueFontSize = 13.sp,
        valueLineHeight = 18.sp,
        emphasizedValue = emphasized
    )
}

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
        modifier = Modifier.fillMaxWidth(),
        details = {
            Text(
                text = stringResource(R.string.github_strategy_guide_pros, guide.pros.joinToString("；")),
                color = MiuixTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.github_strategy_guide_cons, guide.cons.joinToString("；")),
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
            Text(
                text = stringResource(R.string.github_strategy_guide_requirement, guide.requirement),
                color = accent,
                fontWeight = FontWeight.Medium
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
        modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth()
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
                color = MiuixTheme.colorScheme.onBackgroundVariant
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
        modifier = Modifier.fillMaxWidth()
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
        com.example.keios.feature.github.model.GitHubApiAuthMode.Token -> GitHubStatusPalette.Update
        com.example.keios.feature.github.model.GitHubApiAuthMode.Guest -> GitHubStatusPalette.PreRelease
    }
    SheetSummaryCard(
        title = stringResource(R.string.github_strategy_card_title_credential_status),
        accentColor = accent,
        badgeLabel = status.summaryLabel,
        badgeColor = accent,
        modifier = Modifier.fillMaxWidth(),
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

private fun GitHubLookupStrategyOption.accentColor(): Color {
    return when (this) {
        GitHubLookupStrategyOption.AtomFeed -> GitHubStatusPalette.Active
        GitHubLookupStrategyOption.GitHubApiToken -> GitHubStatusPalette.Update
    }
}

@Composable
internal fun GitHubSelectedAppCard(
    selectedApp: InstalledAppItem
) {
    SheetSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = GitHubStatusPalette.tonedSurface(
            GitHubStatusPalette.Update,
            isDark = androidx.compose.foundation.isSystemInDarkTheme()
        ),
        borderColor = GitHubStatusPalette.Update.copy(alpha = 0.28f),
        verticalSpacing = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = selectedApp.packageName, size = 38.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = selectedApp.label,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedApp.packageName,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                label = stringResource(R.string.github_strategy_status_selected),
                color = GitHubStatusPalette.Update
            )
        }
    }
}

@Composable
internal fun GitHubAppCandidateRow(
    app: InstalledAppItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val accent = if (selected) GitHubStatusPalette.Update else MiuixTheme.colorScheme.primary
    SheetSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (selected) {
            GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Update, isDark)
        } else {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.48f)
        },
        borderColor = if (selected) {
            GitHubStatusPalette.Update.copy(alpha = 0.3f)
        } else {
            MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.12f)
        },
        verticalSpacing = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, size = 32.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = app.label,
                    color = accent,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                StatusPill(
                    label = stringResource(R.string.github_strategy_status_current),
                    color = GitHubStatusPalette.Update
                )
            }
        }
    }
}

@Composable
internal fun AppIcon(
    packageName: String,
    size: Dp
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = AppIconCache.get(packageName), packageName) {
        value = withContext(Dispatchers.IO) { AppIconCache.getOrLoad(context, packageName) }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = packageName,
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule)
        )
    } else {
        Box(
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.github_strategy_app_fallback), color = MiuixTheme.colorScheme.primary)
        }
    }
}
