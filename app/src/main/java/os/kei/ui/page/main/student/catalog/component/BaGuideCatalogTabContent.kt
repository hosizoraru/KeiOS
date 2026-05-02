package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.state.BaGuideCatalogFilterSortState
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabContentUiState
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabListState
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
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
    onScrollBoundsChange: (canScrollBackward: Boolean, canScrollForward: Boolean) -> Unit,
    onListScrollInProgressChange: (Boolean) -> Unit,
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
                text = stringResource(tab.labelRes),
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
    val tabContentUiState = rememberBaGuideCatalogTabContentUiState(
        tab = tab,
        searchQuery = filterSortState.searchQuery,
        loading = loading,
        error = error,
        filteredEntriesEmpty = tabListState.filteredEntries.isEmpty()
    )
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    LaunchedEffect(tabListState.listState, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager.snapshotFlow {
            tabListState.listState.canScrollBackward to tabListState.listState.canScrollForward
        }
            .distinctUntilChanged()
            .collect { (canScrollBackward, canScrollForward) ->
                onScrollBoundsChange(canScrollBackward, canScrollForward)
            }
    }
    LaunchedEffect(tabListState.listState, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager.snapshotFlow { tabListState.listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                onListScrollInProgressChange(scrolling)
            }
    }
    BaGuideCatalogTabListLayout(
        listState = tabListState.listState,
        nestedScrollConnection = nestedScrollConnection,
        innerPadding = innerPadding,
        uiState = tabContentUiState,
        progress = progress,
        progressColor = progressColor,
        accent = accent,
        displayedEntries = tabListState.displayedEntries,
        hasMoreEntries = tabListState.hasMoreEntries,
        favoriteCatalogEntries = filterSortState.favoriteCatalogEntries,
        onOpenGuide = onOpenGuide,
        onToggleFavorite = filterSortState::toggleFavorite
    )
}
