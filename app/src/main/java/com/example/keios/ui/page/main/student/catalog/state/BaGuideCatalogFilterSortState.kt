package com.example.keios.ui.page.main.student.catalog.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore

@Stable
internal class BaGuideCatalogFilterSortState(
    private val searchQueryState: MutableState<String>,
    private val sortModeState: MutableState<BaGuideCatalogSortMode>,
    private val showSortPopupState: MutableState<Boolean>,
    private val favoriteCatalogEntriesState: MutableState<Map<Long, Long>>
) {
    var searchQuery: String
        get() = searchQueryState.value
        set(value) {
            searchQueryState.value = value
        }

    var sortMode: BaGuideCatalogSortMode
        get() = sortModeState.value
        set(value) {
            sortModeState.value = value
        }

    var showSortPopup: Boolean
        get() = showSortPopupState.value
        set(value) {
            showSortPopupState.value = value
        }

    var favoriteCatalogEntries: Map<Long, Long>
        get() = favoriteCatalogEntriesState.value
        private set(value) {
            favoriteCatalogEntriesState.value = value
        }

    fun selectSortMode(mode: BaGuideCatalogSortMode) {
        sortMode = mode
        showSortPopup = false
    }

    fun toggleFavorite(contentId: Long) {
        if (contentId <= 0L) return
        val next = favoriteCatalogEntries.toMutableMap()
        if (next.containsKey(contentId)) {
            next.remove(contentId)
        } else {
            next[contentId] = System.currentTimeMillis().coerceAtLeast(1L)
        }
        val frozen = next.toMap()
        favoriteCatalogEntries = frozen
        BaGuideCatalogStore.saveFavorites(frozen)
    }
}

@Composable
internal fun rememberBaGuideCatalogFilterSortState(): BaGuideCatalogFilterSortState {
    val searchQueryState = rememberSaveable { mutableStateOf("") }
    val sortModeState = rememberSaveable { mutableStateOf(BaGuideCatalogSortMode.Default) }
    val showSortPopupState = remember { mutableStateOf(false) }
    val favoriteCatalogEntriesState = remember {
        mutableStateOf(BaGuideCatalogStore.loadFavorites())
    }

    return remember(
        searchQueryState,
        sortModeState,
        showSortPopupState,
        favoriteCatalogEntriesState
    ) {
        BaGuideCatalogFilterSortState(
            searchQueryState = searchQueryState,
            sortModeState = sortModeState,
            showSortPopupState = showSortPopupState,
            favoriteCatalogEntriesState = favoriteCatalogEntriesState
        )
    }
}
