package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideAddIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

@Composable
internal fun BaGuideBgmLibraryHeader(
    favoriteCount: Int,
    displayedCount: Int,
    cachedCount: Int,
    searchActive: Boolean,
    sortMode: BaGuideBgmFavoriteSortMode,
    groupMode: BaGuideBgmFavoriteGroupMode,
    accent: Color,
    onSortModeChange: (BaGuideBgmFavoriteSortMode) -> Unit,
    onGroupModeChange: (BaGuideBgmFavoriteGroupMode) -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val summary = if (searchActive) {
        stringResource(
            R.string.ba_catalog_bgm_library_search_summary,
            displayedCount.coerceAtLeast(0),
            favoriteCount.coerceAtLeast(0),
            cachedCount.coerceAtLeast(0)
        )
    } else {
        stringResource(
            R.string.ba_catalog_bgm_library_summary,
            favoriteCount.coerceAtLeast(0),
            cachedCount.coerceAtLeast(0)
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.22f),
                shape = cardShape
            ),
        cornerRadius = 18.dp,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surface.copy(alpha = 0.76f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ba_catalog_bgm_library_title),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CardHeader.fontSize,
                        lineHeight = AppTypographyTokens.CardHeader.lineHeight,
                        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    label = stringResource(
                        R.string.ba_catalog_bgm_library_queue_summary,
                        displayedCount.coerceAtLeast(0)
                    ),
                    color = accent,
                    size = AppStatusPillSize.Compact
                )
            }
            BaGuideBgmSortGroupDropdownRow(
                sortMode = sortMode,
                groupMode = groupMode,
                accent = accent,
                onSortModeChange = onSortModeChange,
                onGroupModeChange = onGroupModeChange
            )
        }
    }
}

@Composable
internal fun BaGuideBgmLibraryToolsCard(
    favoriteCount: Int,
    cachedCount: Int,
    cacheBytes: Long,
    batchCaching: Boolean,
    batchDone: Int,
    batchTotal: Int,
    batchFailedCount: Int,
    exporting: Boolean,
    importing: Boolean,
    accent: Color,
    onCacheAll: () -> Unit,
    onRetryFailed: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val cacheTitle = if (batchCaching) {
        stringResource(
            R.string.ba_catalog_bgm_cache_batch_progress,
            batchDone.coerceAtLeast(0),
            batchTotal.coerceAtLeast(0)
        )
    } else if (batchFailedCount > 0) {
        stringResource(
            R.string.ba_catalog_bgm_cache_batch_failed_summary,
            batchFailedCount.coerceAtLeast(0)
        )
    } else {
        stringResource(
            R.string.ba_catalog_bgm_cache_summary,
            cachedCount.coerceAtLeast(0),
            favoriteCount.coerceAtLeast(0),
            formatBgmCacheBytes(cacheBytes)
        )
    }
    val progress = if (batchTotal > 0) {
        batchDone.toFloat() / batchTotal.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    val retryMode = !batchCaching && batchFailedCount > 0
    val cacheActionText = stringResource(
        if (retryMode) {
            R.string.ba_catalog_bgm_action_retry_failed
        } else {
            R.string.ba_catalog_bgm_action_cache_all
        }
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = Color(0x1A3B82F6)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardLayoutRhythm.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ba_catalog_bgm_tools_title),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        fontWeight = AppTypographyTokens.CardHeader.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = cacheTitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                GlassTextButton(
                    backdrop = null,
                    text = cacheActionText,
                    leadingIcon = if (batchCaching || retryMode) appLucideRefreshIcon() else appLucideDownloadIcon(),
                    onClick = if (retryMode) onRetryFailed else onCacheAll,
                    enabled = !batchCaching &&
                        (
                            retryMode ||
                                (favoriteCount > 0 && cachedCount < favoriteCount)
                            ),
                    textColor = accent,
                    containerColor = accent,
                    variant = GlassVariant.Compact,
                    textMaxLines = 1,
                    textOverflow = TextOverflow.Ellipsis
                )
            }
            if (batchCaching) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 4.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = accent,
                        backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextButton(
                    backdrop = null,
                    text = stringResource(R.string.ba_catalog_bgm_action_import),
                    leadingIcon = appLucideAddIcon(),
                    onClick = onImport,
                    enabled = !exporting && !importing,
                    textColor = accent,
                    variant = GlassVariant.Compact,
                    textMaxLines = 1,
                    textOverflow = TextOverflow.Ellipsis
                )
                GlassTextButton(
                    backdrop = null,
                    text = stringResource(R.string.ba_catalog_bgm_action_export),
                    leadingIcon = appLucideShareIcon(),
                    onClick = onExport,
                    enabled = favoriteCount > 0 && !exporting && !importing,
                    textColor = accent,
                    variant = GlassVariant.Compact,
                    textMaxLines = 1,
                    textOverflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun BaGuideBgmPlaylistHeader(
    count: Int,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.ba_catalog_bgm_playlist_title),
            modifier = Modifier.weight(1f),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.CardHeader.fontSize,
            lineHeight = AppTypographyTokens.CardHeader.lineHeight,
            fontWeight = AppTypographyTokens.CardHeader.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        StatusPill(
            label = stringResource(R.string.ba_catalog_bgm_playlist_count, count.coerceAtLeast(0)),
            color = accent,
            size = AppStatusPillSize.Compact
        )
    }
}

@Composable
internal fun BaGuideBgmSortGroupDropdownRow(
    sortMode: BaGuideBgmFavoriteSortMode,
    groupMode: BaGuideBgmFavoriteGroupMode,
    accent: Color,
    onSortModeChange: (BaGuideBgmFavoriteSortMode) -> Unit,
    onGroupModeChange: (BaGuideBgmFavoriteGroupMode) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var sortAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var groupExpanded by remember { mutableStateOf(false) }
    var groupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    val sortModes = BaGuideBgmFavoriteSortMode.entries
    val groupModes = BaGuideBgmFavoriteGroupMode.entries
    val sortOptions = sortModes.map { mode -> stringResource(mode.labelRes) }
    val groupOptions = groupModes.map { mode -> stringResource(mode.labelRes) }
    val sortIndex = sortModes.indexOf(sortMode).coerceAtLeast(0)
    val groupIndex = groupModes.indexOf(groupMode).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppDropdownSelector(
            selectedText = stringResource(
                R.string.ba_catalog_bgm_sort_dropdown_label,
                sortOptions.getOrElse(sortIndex) { "" }
            ),
            options = sortOptions,
            selectedIndex = sortIndex,
            expanded = sortExpanded,
            anchorBounds = sortAnchorBounds,
            onExpandedChange = { sortExpanded = it },
            onSelectedIndexChange = { index ->
                sortModes.getOrNull(index)?.let(onSortModeChange)
            },
            onAnchorBoundsChange = { sortAnchorBounds = it },
            modifier = Modifier.weight(1f),
            variant = GlassVariant.Compact,
            textColor = accent
        )
        AppDropdownSelector(
            selectedText = stringResource(
                R.string.ba_catalog_bgm_group_dropdown_label,
                groupOptions.getOrElse(groupIndex) { "" }
            ),
            options = groupOptions,
            selectedIndex = groupIndex,
            expanded = groupExpanded,
            anchorBounds = groupAnchorBounds,
            onExpandedChange = { groupExpanded = it },
            onSelectedIndexChange = { index ->
                groupModes.getOrNull(index)?.let(onGroupModeChange)
            },
            onAnchorBoundsChange = { groupAnchorBounds = it },
            modifier = Modifier.weight(1f),
            variant = GlassVariant.Compact,
            textColor = accent
        )
    }
}

private fun formatBgmCacheBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.US, "%.2f GB", safe / gb)
        safe >= mb -> String.format(Locale.US, "%.2f MB", safe / mb)
        safe >= kb -> String.format(Locale.US, "%.2f KB", safe / kb)
        else -> "${bytes.coerceAtLeast(0L)} B"
    }
}
