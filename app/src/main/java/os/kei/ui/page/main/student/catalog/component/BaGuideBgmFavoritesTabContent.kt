package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.section.GuideGalleryCardItem
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.FrostedBlock
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
internal fun BaGuideBgmFavoritesTabContent(
    searchQuery: String,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    accent: Color
) {
    val favorites by GuideBgmFavoriteStore.favoritesFlow().collectAsState()
    val displayedFavorites = remember(favorites, searchQuery) {
        filterBgmFavorites(favorites, searchQuery)
    }
    val listState = rememberLazyListState()
    val bgmLabel = stringResource(R.string.ba_catalog_tab_bgm)
    val tabTitle = stringResource(R.string.ba_catalog_tab_title, bgmLabel)
    val emptyTitle = stringResource(R.string.ba_catalog_bgm_empty_title)
    val emptySubtitle = if (searchQuery.isBlank()) {
        stringResource(R.string.ba_catalog_bgm_empty_subtitle)
    } else {
        stringResource(R.string.ba_catalog_empty_subtitle_search)
    }

    LazyColumn(
        state = listState,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SmallTitle(tabTitle)
                }
            }
        }

        if (displayedFavorites.isEmpty()) {
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
                items = displayedFavorites,
                key = { it.audioUrl }
            ) { favorite ->
                val studentNote = if (favorite.studentTitle.isNotBlank()) {
                    stringResource(R.string.ba_catalog_bgm_student_note, favorite.studentTitle)
                } else {
                    ""
                }
                val displayNote = remember(favorite.note, studentNote) {
                    listOf(studentNote, favorite.note)
                        .filter { it.isNotBlank() }
                        .joinToString(" / ")
                }
                GuideGalleryCardItem(
                    item = BaGuideGalleryItem(
                        title = favorite.title,
                        imageUrl = favorite.imageUrl.ifBlank { favorite.studentImageUrl },
                        mediaType = "audio",
                        mediaUrl = favorite.audioUrl,
                        note = displayNote
                    ),
                    backdrop = null,
                    onOpenMedia = {},
                    onSaveMedia = { _, _ -> },
                    audioLoopScopeKey = GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY,
                    showMediaTypeLabel = false,
                    showSaveAction = false,
                    bgmFavoriteStudentTitle = favorite.studentTitle,
                    bgmFavoriteStudentImageUrl = favorite.studentImageUrl,
                    bgmFavoriteSourceUrl = favorite.sourceUrl
                )
            }
        }
    }
}

private fun filterBgmFavorites(
    favorites: List<GuideBgmFavoriteItem>,
    searchQuery: String
): List<GuideBgmFavoriteItem> {
    val query = searchQuery.trim()
    if (query.isBlank()) return favorites
    return favorites.filter { item ->
        item.title.contains(query, ignoreCase = true) ||
            item.studentTitle.contains(query, ignoreCase = true) ||
            item.note.contains(query, ignoreCase = true) ||
            item.audioUrl.contains(query, ignoreCase = true)
    }
}
