package os.kei.ui.page.main.mcp.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.mcp.model.McpOverviewMetric
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppOverviewInlineMetricTile
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun McpOverviewCardSection(
    backdrop: Backdrop?,
    titleColor: Color,
    subtitleColor: Color,
    overviewCardColor: Color,
    overviewBorderColor: Color,
    overviewAccentColor: Color,
    runtimeText: String,
    isDark: Boolean,
    running: Boolean,
    overviewMetrics: List<McpOverviewMetric>,
    cardPressFeedbackEnabled: Boolean,
    onToggleServer: () -> Unit,
    onOpenEditSheet: () -> Unit,
) {
    AppOverviewCard(
        title = stringResource(R.string.mcp_overview_title),
        backdrop = backdrop,
        containerColor = overviewCardColor,
        borderColor = overviewBorderColor,
        contentColor = titleColor,
        showIndication = cardPressFeedbackEnabled,
        onClick = onToggleServer,
        onLongClick = onOpenEditSheet,
        headerEndActions = {
            StatusPill(
                label = runtimeText,
                color = overviewAccentColor,
                backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                borderAlphaOverride = if (isDark) 0.35f else 0.42f
            )
            StatusPill(
                label = stringResource(
                    if (running) R.string.common_status_running else R.string.common_status_not_running
                ),
                color = overviewAccentColor
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
        ) {
            var metricIndex = 0
            while (metricIndex < overviewMetrics.size) {
                val metric = overviewMetrics[metricIndex]
                if (metric.spanFullWidth) {
                    McpOverviewMetricItem(
                        metric = metric,
                        labelColor = subtitleColor,
                        defaultValueColor = titleColor,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    metricIndex += 1
                    continue
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap)
                ) {
                    McpOverviewMetricItem(
                        metric = metric,
                        labelColor = subtitleColor,
                        defaultValueColor = titleColor,
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                    val nextMetric = overviewMetrics.getOrNull(metricIndex + 1)
                    if (nextMetric != null && !nextMetric.spanFullWidth) {
                        McpOverviewMetricItem(
                            metric = nextMetric,
                            labelColor = subtitleColor,
                            defaultValueColor = titleColor,
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f)
                        )
                        metricIndex += 2
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        metricIndex += 1
                    }
                }
            }
        }
    }
}

@Composable
internal fun McpOverviewMetricItem(
    metric: McpOverviewMetric,
    labelColor: Color,
    defaultValueColor: Color,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    AppOverviewInlineMetricTile(
        label = metric.label,
        value = metric.value.ifBlank { stringResource(R.string.common_na) },
        modifier = modifier.fillMaxWidth(),
        labelColor = labelColor,
        valueColor = metric.valueColor ?: defaultValueColor,
        valueMaxLines = metric.valueMaxLines,
        labelWeight = metric.labelWeight,
        valueWeight = metric.valueWeight,
        backdrop = backdrop,
        emphasizedValue = true
    )
}

@Composable
internal fun McpSectionHeaderIcon(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MiuixTheme.colorScheme.primary,
        modifier = modifier
            .size(22.dp)
            .defaultMinSize(minHeight = 22.dp)
    )
}
