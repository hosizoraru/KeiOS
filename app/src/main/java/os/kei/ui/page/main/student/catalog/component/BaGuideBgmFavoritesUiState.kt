package os.kei.ui.page.main.student.catalog.component

import androidx.annotation.StringRes
import os.kei.R
import os.kei.ui.page.main.student.GuideBgmFavoriteItem

internal enum class BaGuideBgmFavoriteSortMode(@param:StringRes val labelRes: Int) {
    Recent(R.string.ba_catalog_bgm_sort_recent),
    AddedOrder(R.string.ba_catalog_bgm_sort_added_order),
    Student(R.string.ba_catalog_bgm_sort_student),
    Title(R.string.ba_catalog_bgm_sort_title)
}

internal enum class BaGuideBgmQueueMode(@param:StringRes val labelRes: Int) {
    Continuous(R.string.ba_catalog_bgm_queue_continuous),
    SingleLoop(R.string.ba_catalog_bgm_queue_single_loop)
}

internal fun filterAndSortBgmFavorites(
    favorites: List<GuideBgmFavoriteItem>,
    searchQuery: String,
    sortMode: BaGuideBgmFavoriteSortMode
): List<GuideBgmFavoriteItem> {
    val query = searchQuery.trim()
    val filtered = if (query.isBlank()) {
        favorites
    } else {
        favorites.filter { item -> bgmFavoriteMatchesSearch(item, query) }
    }
    return when (sortMode) {
        BaGuideBgmFavoriteSortMode.Recent -> filtered.sortedByDescending { it.favoritedAtMs }
        BaGuideBgmFavoriteSortMode.AddedOrder -> filtered.sortedBy { it.favoritedAtMs }
        BaGuideBgmFavoriteSortMode.Student -> filtered.sortedWith(
            compareBy<GuideBgmFavoriteItem> { it.studentTitle.ifBlank { it.title }.lowercase() }
                .thenBy { it.title.lowercase() }
                .thenByDescending { it.favoritedAtMs }
        )
        BaGuideBgmFavoriteSortMode.Title -> filtered.sortedWith(
            compareBy<GuideBgmFavoriteItem> { it.title.lowercase() }
                .thenBy { it.studentTitle.lowercase() }
                .thenByDescending { it.favoritedAtMs }
        )
    }
}
