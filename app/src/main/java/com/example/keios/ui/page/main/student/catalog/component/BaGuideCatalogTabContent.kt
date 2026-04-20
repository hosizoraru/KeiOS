package com.example.keios.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.state.BaGuideCatalogFilterSortState
import com.example.keios.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabListState
import com.example.keios.ui.page.main.widget.chrome.AppChromeTokens
import com.example.keios.ui.page.main.widget.glass.FrostedBlock
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideCatalogTabContent(
    tab: BaGuideCatalogTab,
    catalog: BaGuideCatalogBundle,
    filterSortState: BaGuideCatalogFilterSortState,
    loading: Boolean,
    error: String?,
    progress: Float,
    progressColor: Color,
    accent: Color,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    isPageActive: Boolean,
    renderHeavyContent: Boolean,
    onOpenGuide: (String) -> Unit
) {
    if (!renderHeavyContent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + AppChromeTokens.pageSectionGap,
                    start = AppChromeTokens.pageHorizontalPadding,
                    end = AppChromeTokens.pageHorizontalPadding
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.label,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    val tabListState = rememberBaGuideCatalogTabListState(
        tab = tab,
        catalog = catalog,
        sortMode = filterSortState.sortMode,
        favoriteCatalogEntries = filterSortState.favoriteCatalogEntries,
        searchQuery = filterSortState.searchQuery,
        loading = loading,
        isPageActive = isPageActive
    )

    val syncStatusTitle = stringResource(R.string.ba_catalog_sync_status_title)
    val syncStatusBody = stringResource(R.string.ba_catalog_sync_status_body_retry)
    val emptyTitle = stringResource(R.string.ba_catalog_empty_title)
    val emptySubtitle = if (filterSortState.searchQuery.isBlank()) {
        stringResource(R.string.ba_catalog_empty_subtitle_default)
    } else {
        stringResource(R.string.ba_catalog_empty_subtitle_search)
    }

    LazyColumn(
        state = tabListState.listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + AppChromeTokens.pageSectionGap,
            start = AppChromeTokens.pageHorizontalPadding,
            end = AppChromeTokens.pageHorizontalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(AppChromeTokens.pageSectionGap)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SmallTitle(stringResource(R.string.ba_catalog_tab_title, tab.label))
                }
                CircularProgressIndicator(
                    progress = progress,
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = progressColor,
                        backgroundColor = progressColor.copy(alpha = 0.30f),
                    ),
                )
            }
        }

        if (!error.isNullOrBlank()) {
            item {
                FrostedBlock(
                    backdrop = null,
                    title = syncStatusTitle,
                    subtitle = error,
                    body = syncStatusBody,
                    accent = Color(0xFFEF4444)
                )
            }
        }

        if (!loading && tabListState.filteredEntries.isEmpty()) {
            item {
                FrostedBlock(
                    backdrop = null,
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    accent = accent
                )
            }
        } else {
            items(
                items = tabListState.displayedEntries,
                key = { "${it.tab.name}-${it.entryId}-${it.contentId}" }
            ) { entry ->
                BaGuideCatalogEntryCard(
                    entry = entry,
                    isFavorite = filterSortState.favoriteCatalogEntries.containsKey(entry.contentId),
                    onOpenGuide = onOpenGuide,
                    onToggleFavorite = filterSortState::toggleFavorite
                )
            }
            if (tabListState.hasMoreEntries) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = 0.3f,
                            size = 16.dp,
                            strokeWidth = 2.dp,
                            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                foregroundColor = accent,
                                backgroundColor = accent.copy(alpha = 0.30f),
                            ),
                        )
                        Text(
                            text = stringResource(R.string.ba_catalog_loading_more),
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
