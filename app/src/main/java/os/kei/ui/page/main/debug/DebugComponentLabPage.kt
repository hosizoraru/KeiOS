package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideFlaskIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppInfoListBody
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppOverviewMetricTile
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugComponentLabPage(
    onClose: () -> Unit,
    onOpenBgmMusic: () -> Unit
) {
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val accent = MiuixTheme.colorScheme.primary
    val pageBackdrop = rememberLayerBackdrop()

    AppPageScaffold(
        title = stringResource(R.string.debug_component_lab_title),
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = Color.Transparent,
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = appLucideBackIcon(),
                    contentDescription = stringResource(R.string.common_close),
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MiuixTheme.colorScheme.background,
                                accent.copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.08f),
                                MiuixTheme.colorScheme.background
                            )
                        )
                    )
                    .layerBackdrop(pageBackdrop)
            )
            AppPageLazyColumn(
                innerPadding = innerPadding,
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomExtra = 40.dp,
                sectionSpacing = 14.dp
            ) {
                item {
                    DebugLabIntroCard(accent = accent)
                }
                item {
                    DebugLiquidCatalogCard(
                        accent = accent,
                        backdrop = pageBackdrop
                    )
                }
                item {
                    DebugBgmPreviewCard(
                        accent = accent,
                        onOpenBgmMusic = onOpenBgmMusic
                    )
                }
                item {
                    DebugIterationQueueCard(accent = accent)
                }
            }
        }
    }
}

@Composable
private fun DebugLabIntroCard(accent: Color) {
    AppOverviewCard(
        title = stringResource(R.string.debug_component_lab_intro_title),
        subtitle = stringResource(R.string.debug_component_lab_intro_subtitle),
        titleColor = accent,
        containerColor = accent.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.12f),
        borderColor = accent.copy(alpha = 0.24f),
        startAction = {
            Icon(
                imageVector = appLucideFlaskIcon(),
                contentDescription = null,
                tint = accent
            )
        },
        headerEndActions = {
            StatusPill(
                label = stringResource(R.string.debug_component_lab_badge_easter),
                color = accent,
                size = AppStatusPillSize.Compact
            )
        }
    ) {
        AppSupportingBlock(
            text = stringResource(R.string.debug_component_lab_intro_note),
            accentColor = accent
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap)
        ) {
            AppOverviewMetricTile(
                label = stringResource(R.string.debug_component_lab_metric_host_label),
                value = stringResource(R.string.debug_component_lab_metric_host_value),
                modifier = Modifier.weight(1f),
                valueMaxLines = 1
            )
            AppOverviewMetricTile(
                label = stringResource(R.string.debug_component_lab_metric_focus_label),
                value = stringResource(R.string.debug_component_lab_metric_focus_value),
                modifier = Modifier.weight(1f),
                valueMaxLines = 1
            )
        }
    }
}

@Composable
private fun DebugBgmPreviewCard(
    accent: Color,
    onOpenBgmMusic: () -> Unit
) {
    val openLabel = stringResource(R.string.debug_component_lab_action_open_bgm_activity)
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_bgm_title),
        subtitle = stringResource(R.string.debug_component_lab_bgm_subtitle),
        sectionIcon = appLucideMusicIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.70f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
        onClick = onOpenBgmMusic,
        headerEndActions = {
            IconButton(onClick = onOpenBgmMusic) {
                Icon(
                    imageVector = appLucideExternalLinkIcon(),
                    contentDescription = openLabel,
                    tint = accent
                )
            }
        }
    ) {
        AppSupportingBlock(
            text = stringResource(R.string.debug_component_lab_bgm_entry_note),
            accentColor = accent
        )
        AppInfoListBody {
            AppInfoRow(
                label = stringResource(R.string.debug_component_lab_row_entry_title),
                value = stringResource(R.string.debug_component_lab_row_entry_value),
                labelMaxLines = 1,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis
            )
            AppInfoRow(
                label = stringResource(R.string.debug_component_lab_row_surface_title),
                value = stringResource(R.string.debug_component_lab_row_surface_value),
                labelMaxLines = 1,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis
            )
            AppInfoRow(
                label = stringResource(R.string.debug_component_lab_row_components_title),
                value = stringResource(R.string.debug_component_lab_row_components_value),
                labelMaxLines = 1,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebugIterationQueueCard(accent: Color) {
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_iterations_title),
        subtitle = stringResource(R.string.debug_component_lab_iterations_subtitle),
        sectionIcon = appLucideLayersIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.66f),
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
        headerEndActions = {
            StatusPill(
                label = stringResource(R.string.debug_component_lab_queue_badge),
                color = Color(0xFFF59E0B),
                size = AppStatusPillSize.Compact
            )
        }
    ) {
        AppInfoListBody(verticalSpacing = 2.dp) {
            AppInfoRow(
                label = stringResource(R.string.debug_component_lab_iteration_bgm_title),
                value = stringResource(R.string.debug_component_lab_iteration_bgm_value),
                labelMaxLines = 1,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis
            )
            AppInfoRow(
                label = stringResource(R.string.debug_component_lab_iteration_controls_title),
                value = stringResource(R.string.debug_component_lab_iteration_controls_value),
                labelMaxLines = 1,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis
            )
            AppInfoRow(
                label = stringResource(R.string.debug_component_lab_iteration_status_title),
                value = stringResource(R.string.debug_component_lab_iteration_status_value),
                labelMaxLines = 1,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis
            )
        }
    }
}
