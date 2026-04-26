package os.kei.ui.page.main.student.catalog.component

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.student.page.state.GuideDetailTabRequestStore
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.FrostedBlock
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
internal fun BaGuideBgmFavoritesTabContent(
    searchQuery: String,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    accent: Color,
    onOpenGuide: (String) -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val pageScope = rememberCoroutineScope()
    val favorites by GuideBgmFavoriteStore.favoritesFlow().collectAsState()
    var sortModeName by rememberSaveable { mutableStateOf(BaGuideBgmFavoriteSortMode.Recent.name) }
    val sortMode = remember(sortModeName) {
        BaGuideBgmFavoriteSortMode.entries.firstOrNull { it.name == sortModeName }
            ?: BaGuideBgmFavoriteSortMode.Recent
    }
    val displayedFavorites = remember(favorites, searchQuery, sortMode) {
        filterAndSortBgmFavorites(
            favorites = favorites,
            searchQuery = searchQuery,
            sortMode = sortMode
        )
    }
    var queueModeName by rememberSaveable { mutableStateOf(BaGuideBgmQueueMode.Continuous.name) }
    val queueMode = remember(queueModeName) {
        BaGuideBgmQueueMode.entries.firstOrNull { it.name == queueModeName }
            ?: BaGuideBgmQueueMode.Continuous
    }
    var selectedAudioUrl by rememberSaveable { mutableStateOf("") }
    var autoPlayRequestToken by remember { mutableIntStateOf(0) }
    var cacheRevision by remember { mutableIntStateOf(0) }
    var cachingAudioUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var removedFavorite by remember { mutableStateOf<GuideBgmFavoriteItem?>(null) }
    val cacheSuccessText = stringResource(R.string.ba_catalog_bgm_cache_success)
    val cacheFailedText = stringResource(R.string.ba_catalog_bgm_cache_failed)

    LaunchedEffect(displayedFavorites) {
        selectedAudioUrl = when {
            displayedFavorites.isEmpty() -> ""
            selectedAudioUrl.isBlank() -> displayedFavorites.first().audioUrl
            displayedFavorites.none { it.audioUrl == selectedAudioUrl } -> displayedFavorites.first().audioUrl
            else -> selectedAudioUrl
        }
    }

    fun openFavoriteGuide(favorite: GuideBgmFavoriteItem) {
        GuideDetailTabRequestStore.request(favorite.sourceUrl, GuideBottomTab.Gallery)
        onOpenGuide(favorite.sourceUrl)
    }

    fun selectQueueOffset(offset: Int, startPlayback: Boolean) {
        if (displayedFavorites.isEmpty()) return
        val currentIndex = displayedFavorites.indexOfFirst { it.audioUrl == selectedAudioUrl }
            .takeIf { it >= 0 }
            ?: 0
        val nextIndex = (currentIndex + offset + displayedFavorites.size) % displayedFavorites.size
        selectedAudioUrl = displayedFavorites[nextIndex].audioUrl
        if (startPlayback) {
            autoPlayRequestToken += 1
        }
    }

    fun removeFavorite(favorite: GuideBgmFavoriteItem) {
        GuideBgmFavoriteStore.removeFavorite(favorite.audioUrl)
        removedFavorite = favorite
        if (selectedAudioUrl == favorite.audioUrl) {
            selectedAudioUrl = ""
        }
    }

    fun cacheFavorite(favorite: GuideBgmFavoriteItem) {
        if (favorite.audioUrl.isBlank() || cachingAudioUrls.contains(favorite.audioUrl)) return
        cachingAudioUrls = cachingAudioUrls + favorite.audioUrl
        pageScope.launch {
            val success = runCatching {
                BaGuideTempMediaCache.prefetchForGuide(
                    context = appContext,
                    sourceUrl = favoriteCacheScope(favorite),
                    rawUrls = listOf(favorite.audioUrl)
                )
                isFavoriteBgmCached(appContext, favorite)
            }.getOrDefault(false)
            cachingAudioUrls = cachingAudioUrls - favorite.audioUrl
            cacheRevision += 1
            Toast.makeText(
                context,
                if (success) cacheSuccessText else cacheFailedText,
                Toast.LENGTH_SHORT
            ).show()
        }
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
    val selectedIndex = displayedFavorites.indexOfFirst { it.audioUrl == selectedAudioUrl }
    val selectedFavorite = displayedFavorites.getOrNull(selectedIndex)

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SmallTitle(tabTitle)
                    }
                }
                BaGuideBgmSortModeRow(
                    sortMode = sortMode,
                    accent = accent,
                    onSortModeChange = { sortModeName = it.name }
                )
            }
        }

        removedFavorite?.let { favorite ->
            item(key = "bgm-undo-${favorite.audioUrl}") {
                BaGuideBgmUndoBlock(
                    removedFavorite = favorite,
                    accent = accent,
                    onUndo = {
                        GuideBgmFavoriteStore.restoreFavorite(favorite)
                        removedFavorite = null
                    }
                )
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
            selectedFavorite?.let { favorite ->
                item(key = "bgm-queue-${favorite.audioUrl}") {
                    val cached = remember(favorite, cacheRevision) {
                        isFavoriteBgmCached(appContext, favorite)
                    }
                    BaGuideBgmQueueCard(
                        favorite = favorite,
                        queueIndex = selectedIndex.coerceAtLeast(0),
                        queueSize = displayedFavorites.size,
                        queueMode = queueMode,
                        cached = cached,
                        accent = accent,
                        audioAutoPlayRequestToken = autoPlayRequestToken,
                        mediaUrlResolver = { raw -> resolveFavoriteBgmMediaUrl(appContext, favorite, raw) },
                        onPrevious = { selectQueueOffset(offset = -1, startPlayback = true) },
                        onNext = { selectQueueOffset(offset = 1, startPlayback = true) },
                        onToggleQueueMode = {
                            queueModeName = if (queueMode == BaGuideBgmQueueMode.Continuous) {
                                BaGuideBgmQueueMode.SingleLoop.name
                            } else {
                                BaGuideBgmQueueMode.Continuous.name
                            }
                        },
                        onPlaybackEnded = {
                            if (queueMode == BaGuideBgmQueueMode.Continuous && displayedFavorites.size > 1) {
                                selectQueueOffset(offset = 1, startPlayback = true)
                            }
                        },
                        onOpenGuide = { openFavoriteGuide(favorite) }
                    )
                }
            }

            items(
                items = displayedFavorites,
                key = { it.audioUrl }
            ) { favorite ->
                val cached = remember(favorite, cacheRevision) {
                    isFavoriteBgmCached(appContext, favorite)
                }
                BaGuideBgmFavoriteCard(
                    favorite = favorite,
                    selected = favorite.audioUrl == selectedAudioUrl,
                    cached = cached,
                    caching = cachingAudioUrls.contains(favorite.audioUrl),
                    accent = accent,
                    onOpenGuide = { openFavoriteGuide(favorite) },
                    onSelect = { selectedAudioUrl = favorite.audioUrl },
                    onPlay = {
                        selectedAudioUrl = favorite.audioUrl
                        autoPlayRequestToken += 1
                    },
                    onCache = { cacheFavorite(favorite) },
                    onRemove = { removeFavorite(favorite) }
                )
            }
        }
    }
}

@Composable
private fun BaGuideBgmSortModeRow(
    sortMode: BaGuideBgmFavoriteSortMode,
    accent: Color,
    onSortModeChange: (BaGuideBgmFavoriteSortMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaGuideBgmFavoriteSortMode.entries.forEach { mode ->
            val selected = mode == sortMode
            GlassTextButton(
                backdrop = null,
                text = stringResource(mode.labelRes),
                onClick = { onSortModeChange(mode) },
                textColor = if (selected) accent else androidx.compose.ui.graphics.Color(0xFF64748B),
                containerColor = if (selected) accent else null,
                variant = GlassVariant.Compact,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun favoriteCacheScope(favorite: GuideBgmFavoriteItem): String {
    return favorite.sourceUrl.ifBlank { GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY }
}

private fun resolveFavoriteBgmMediaUrl(
    context: Context,
    favorite: GuideBgmFavoriteItem,
    rawUrl: String
): String {
    return BaGuideTempMediaCache.resolveCachedUrl(
        context = context,
        sourceUrl = favoriteCacheScope(favorite),
        rawUrl = rawUrl
    )
}

private fun isFavoriteBgmCached(
    context: Context,
    favorite: GuideBgmFavoriteItem
): Boolean {
    val raw = normalizeGuideMediaSource(favorite.audioUrl)
    if (raw.isBlank()) return false
    val resolved = resolveFavoriteBgmMediaUrl(context, favorite, raw)
    return resolved.startsWith("file:", ignoreCase = true) && resolved != raw
}
