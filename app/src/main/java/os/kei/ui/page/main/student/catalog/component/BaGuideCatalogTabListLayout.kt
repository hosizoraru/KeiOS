package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.state.BaGuideCatalogTabContentUiState
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.FrostedBlock
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
internal fun BaGuideCatalogTabListLayout(
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    innerPadding: PaddingValues,
    uiState: BaGuideCatalogTabContentUiState,
    progress: Float,
    progressColor: Color,
    accent: Color,
    displayedEntries: List<BaGuideCatalogEntry>,
    hasMoreEntries: Boolean,
    favoriteCatalogEntries: Map<Long, Long>,
    onOpenGuide: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    val loadAvatarImages by remember(listState) {
        derivedStateOf { !listState.isScrollInProgress }
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SmallTitle(uiState.tabTitle)
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
        if (uiState.showError) {
            item {
                FrostedBlock(
                    backdrop = null,
                    title = uiState.syncStatusTitle,
                    subtitle = uiState.errorText,
                    body = uiState.syncStatusBody,
                    accent = Color(0xFFEF4444)
                )
            }
        }
        if (uiState.showEmpty) {
            item {
                FrostedBlock(
                    backdrop = null,
                    title = uiState.emptyTitle,
                    subtitle = uiState.emptySubtitle,
                    accent = accent
                )
            }
        } else {
            renderBaGuideCatalogEntryListAdapter(
                displayedEntries = displayedEntries,
                hasMoreEntries = hasMoreEntries,
                favoriteCatalogEntries = favoriteCatalogEntries,
                loadAvatarImages = loadAvatarImages,
                accent = accent,
                loadingMoreText = uiState.loadingMoreText,
                onOpenGuide = onOpenGuide,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}
