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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.feature.github.data.local.AppIconCache
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubStrategyBenchmarkResult
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.ui.page.main.widget.SheetChoiceCard
import com.example.keios.ui.page.main.widget.SheetExpandableCard
import com.example.keios.ui.page.main.widget.SheetSummaryCard
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
    titleMinWidth: Dp = 72.dp,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = titleColor,
            modifier = Modifier.widthIn(min = titleMinWidth)
        )
        Text(
            text = value,
            color = valueColor,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = titleColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value.ifBlank { "N/A" },
            color = valueColor,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
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
                text = "优点：${guide.pros.joinToString("；")}",
                color = MiuixTheme.colorScheme.onBackground
            )
            Text(
                text = "注意：${guide.cons.joinToString("；")}",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
            Text(
                text = "要求：${guide.requirement}",
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
        selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken -> "未使用"
        tokenInput.isNotBlank() -> "已填写"
        else -> "游客"
    }
    val tokenStatusColor = when {
        selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken -> MiuixTheme.colorScheme.onBackgroundVariant
        tokenInput.isNotBlank() -> GitHubStatusPalette.Update
        else -> GitHubStatusPalette.PreRelease
    }

    SheetSummaryCard(
        title = "当前待保存配置",
        accentColor = MiuixTheme.colorScheme.onBackground,
        badgeLabel = if (changed) "待保存" else "与当前一致",
        badgeColor = if (changed) accent else MiuixTheme.colorScheme.onBackgroundVariant,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (changed) 0.82f else 0.66f),
        modifier = Modifier.fillMaxWidth()
    ) {
        GitHubCompactInfoRow(
            label = "方案",
            value = selectedStrategy.label,
            valueColor = accent,
            emphasized = true,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = "Token",
            value = tokenStatusLabel,
            valueColor = tokenStatusColor,
            emphasized = selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = "影响",
            value = if (trackedCount > 0) {
                "保存后将重新检查 $trackedCount 个跟踪项目"
            } else {
                "当前还没有已跟踪项目"
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
        title = "推荐新建方案",
        collapsedSummary = guide.collapsedSummary,
        expandedSummary = guide.summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        accentColor = GitHubStatusPalette.Update,
        badgeLabel = "最小权限",
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
            label = "首轮均时",
            value = "${result.coldAverageMs} ms",
            valueColor = accent
        )
        GitHubOverviewMetricItem(
            label = "缓存均时",
            value = "${result.warmAverageMs} ms",
            valueColor = GitHubStatusPalette.Stable
        )
        GitHubOverviewMetricItem(
            label = "缓存命中",
            value = "${result.cacheHitCount}/${result.warmSamples.size}",
            valueColor = if (result.cacheHitCount == result.warmSamples.size) {
                GitHubStatusPalette.Update
            } else {
                GitHubStatusPalette.PreRelease
            }
        )
        GitHubOverviewMetricItem(
            label = "失败",
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
    val accent = when (status.authMode) {
        com.example.keios.feature.github.model.GitHubApiAuthMode.Token -> GitHubStatusPalette.Update
        com.example.keios.feature.github.model.GitHubApiAuthMode.Guest -> GitHubStatusPalette.PreRelease
    }
    SheetSummaryCard(
        title = "凭证检测",
        accentColor = accent,
        badgeLabel = status.summaryLabel,
        badgeColor = accent,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    ) {
        GitHubCompactInfoRow(
            label = "模式",
            value = status.authMode.label,
            valueColor = accent,
            emphasized = true,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = "配额",
            value = "${status.coreRemaining} / ${status.coreLimit}",
            valueColor = if (status.coreRemaining > 0) accent else GitHubStatusPalette.Error,
            emphasized = true,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = "已用",
            value = status.coreUsed.toString(),
            valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
            titleMinWidth = 44.dp
        )
        GitHubCompactInfoRow(
            label = "恢复",
            value = formatFutureEta(status.resetAtMillis),
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
            Text("App", color = MiuixTheme.colorScheme.primary)
        }
    }
}
