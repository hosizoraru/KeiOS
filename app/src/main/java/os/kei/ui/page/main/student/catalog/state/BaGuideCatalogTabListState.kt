package os.kei.ui.page.main.student.catalog.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.filterByQuery
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

internal const val CATALOG_BATCH_SIZE = 20
private const val CATALOG_LOAD_MORE_THRESHOLD = 10
internal const val CATALOG_RELEASE_DATE_FETCH_LIMIT_PER_PASS = 24

@Stable
internal data class BaGuideCatalogTabListState(
    val listState: LazyListState,
    val filteredEntries: List<BaGuideCatalogEntry>,
    val displayedEntries: List<BaGuideCatalogEntry>,
    val hasMoreEntries: Boolean
)

@Composable
internal fun rememberBaGuideCatalogTabListState(
    tab: BaGuideCatalogTab,
    catalog: BaGuideCatalogBundle,
    sortMode: BaGuideCatalogSortMode,
    favoriteCatalogEntries: Map<Long, Long>,
    searchQuery: String,
    loading: Boolean,
    isPageActive: Boolean
): BaGuideCatalogTabListState {
    val currentEntries = remember(catalog, tab, sortMode, favoriteCatalogEntries) {
        catalog.entries(tab).sortedByMode(sortMode, favoriteCatalogEntries)
    }
    val filteredEntries = remember(currentEntries, searchQuery) {
        currentEntries.filterByQuery(searchQuery)
    }

    val listState = rememberLazyListState()
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    var visibleCount by rememberSaveable(tab, searchQuery) { mutableIntStateOf(0) }
    LaunchedEffect(filteredEntries.size) {
        visibleCount = minOf(filteredEntries.size, CATALOG_BATCH_SIZE)
    }
    LaunchedEffect(isPageActive, listState, filteredEntries.size, loading, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager.snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalCount) ->
                if (loading) return@collect
                if (visibleCount >= filteredEntries.size) return@collect
                if (totalCount <= 0) return@collect
                val triggerIndex = (totalCount - 1 - CATALOG_LOAD_MORE_THRESHOLD).coerceAtLeast(0)
                if (lastVisible < triggerIndex) return@collect

                val viewportItems = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(6)
                val appendBatch = max(CATALOG_BATCH_SIZE, viewportItems * 3)
                    .coerceAtMost(CATALOG_BATCH_SIZE * 3)
                visibleCount = minOf(visibleCount + appendBatch, filteredEntries.size)
            }
    }

    val displayedEntries = remember(filteredEntries, visibleCount) {
        if (visibleCount >= filteredEntries.size) {
            filteredEntries
        } else {
            filteredEntries.subList(0, visibleCount)
        }
    }

    return remember(listState, filteredEntries, displayedEntries, visibleCount) {
        BaGuideCatalogTabListState(
            listState = listState,
            filteredEntries = filteredEntries,
            displayedEntries = displayedEntries,
            hasMoreEntries = visibleCount < filteredEntries.size
        )
    }
}
