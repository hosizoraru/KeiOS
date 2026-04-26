package os.kei.ui.page.main.student.catalog.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.derivedStateOf
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
import os.kei.ui.page.main.student.GuideBgmFavoritePlaybackStore
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.page.state.GuideDetailTabRequestStore
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.FrostedBlock
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import kotlinx.coroutines.delay
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
    val savedPlayback = remember { GuideBgmFavoritePlaybackStore.snapshot() }
    var sortModeName by rememberSaveable { mutableStateOf(BaGuideBgmFavoriteSortMode.Recent.name) }
    val sortMode = remember(sortModeName) {
        BaGuideBgmFavoriteSortMode.entries.firstOrNull { it.name == sortModeName }
            ?: BaGuideBgmFavoriteSortMode.Recent
    }
    var groupModeName by rememberSaveable { mutableStateOf(BaGuideBgmFavoriteGroupMode.All.name) }
    val groupMode = remember(groupModeName) {
        BaGuideBgmFavoriteGroupMode.entries.firstOrNull { it.name == groupModeName }
            ?: BaGuideBgmFavoriteGroupMode.All
    }
    val displayedFavorites = remember(favorites, searchQuery, sortMode) {
        filterAndSortBgmFavorites(
            favorites = favorites,
            searchQuery = searchQuery,
            sortMode = sortMode
        )
    }
    var queueModeName by rememberSaveable {
        mutableStateOf(
            savedPlayback.queueModeName
                .takeIf { saved -> BaGuideBgmQueueMode.entries.any { it.name == saved } }
                ?: BaGuideBgmQueueMode.Continuous.name
        )
    }
    val queueMode = remember(queueModeName) {
        BaGuideBgmQueueMode.entries.firstOrNull { it.name == queueModeName }
            ?: BaGuideBgmQueueMode.Continuous
    }
    var selectedAudioUrl by rememberSaveable { mutableStateOf(savedPlayback.selectedAudioUrl) }
    var playbackRuntimeState by remember { mutableStateOf(BaGuideBgmPlaybackRuntimeState()) }
    var cacheRevision by remember { mutableIntStateOf(0) }
    var cachingAudioUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var batchCaching by remember { mutableStateOf(false) }
    var batchCacheDone by remember { mutableIntStateOf(0) }
    var batchCacheTotal by remember { mutableIntStateOf(0) }
    var batchFailedAudioUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var removedFavorite by remember { mutableStateOf<GuideBgmFavoriteItem?>(null) }
    val cacheSuccessText = stringResource(R.string.ba_catalog_bgm_cache_success)
    val cacheFailedText = stringResource(R.string.ba_catalog_bgm_cache_failed)
    val cacheAllReadyText = stringResource(R.string.ba_catalog_bgm_cache_all_ready)

    fun startFavoritePlayback(favorite: GuideBgmFavoriteItem, restart: Boolean = false) {
        selectedAudioUrl = favorite.audioUrl
        val resumePosition = if (restart) {
            0L
        } else {
            GuideBgmFavoritePlaybackStore.progressFor(favorite.audioUrl)?.resumePositionMs ?: 0L
        }
        playFavoriteBgm(
            context = appContext,
            favorite = favorite,
            queueMode = queueMode,
            startPositionMs = resumePosition,
            restart = restart
        )
    }

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
        val nextFavorite = displayedFavorites[nextIndex]
        selectedAudioUrl = nextFavorite.audioUrl
        if (startPlayback) {
            startFavoritePlayback(nextFavorite)
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
            if (success) {
                batchFailedAudioUrls = batchFailedAudioUrls - favorite.audioUrl
            }
            cacheRevision += 1
            Toast.makeText(
                context,
                if (success) cacheSuccessText else cacheFailedText,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun cacheFavoriteBatch(targets: List<GuideBgmFavoriteItem>) {
        if (batchCaching) return
        if (targets.isEmpty()) {
            Toast.makeText(context, cacheAllReadyText, Toast.LENGTH_SHORT).show()
            return
        }
        batchCaching = true
        batchCacheDone = 0
        batchCacheTotal = targets.size
        batchFailedAudioUrls = batchFailedAudioUrls - targets.map { it.audioUrl }.toSet()
        cachingAudioUrls = cachingAudioUrls + targets.map { it.audioUrl }
        pageScope.launch {
            var successCount = 0
            val failedAudioUrls = mutableSetOf<String>()
            targets.forEach { favorite ->
                val success = runCatching {
                    BaGuideTempMediaCache.prefetchForGuide(
                        context = appContext,
                        sourceUrl = favoriteCacheScope(favorite),
                        rawUrls = listOf(favorite.audioUrl)
                    )
                    isFavoriteBgmCached(appContext, favorite)
                }.getOrDefault(false)
                if (success) {
                    successCount += 1
                } else {
                    failedAudioUrls += favorite.audioUrl
                }
                batchCacheDone += 1
                cachingAudioUrls = cachingAudioUrls - favorite.audioUrl
                cacheRevision += 1
            }
            batchCaching = false
            batchFailedAudioUrls = batchFailedAudioUrls + failedAudioUrls
            Toast.makeText(
                context,
                context.getString(R.string.ba_catalog_bgm_cache_batch_done, successCount),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun cacheDisplayedFavorites() {
        val targets = displayedFavorites.filter { favorite ->
            favorite.audioUrl.isNotBlank() && !isFavoriteBgmCached(appContext, favorite)
        }
        cacheFavoriteBatch(targets)
    }

    fun retryFailedCache() {
        val failedTargets = displayedFavorites.filter { favorite ->
            favorite.audioUrl in batchFailedAudioUrls && !isFavoriteBgmCached(appContext, favorite)
        }
        cacheFavoriteBatch(failedTargets)
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
    val cachedFavoriteCount = remember(displayedFavorites, cacheRevision) {
        displayedFavorites.count { favorite -> isFavoriteBgmCached(appContext, favorite) }
    }
    val cachedFavoriteBytes = remember(displayedFavorites, cacheRevision) {
        displayedFavorites.sumOf { favorite -> favoriteBgmCachedBytes(appContext, favorite) }
    }
    val groupLabels = rememberBaGuideBgmFavoriteGroupLabels()
    val playbackSnapshot = remember(playbackRuntimeState, selectedAudioUrl) {
        GuideBgmFavoritePlaybackStore.snapshot()
    }
    val displayedFavoriteGroups = remember(displayedFavorites, groupMode, playbackSnapshot, groupLabels) {
        groupBgmFavorites(
            favorites = displayedFavorites,
            groupMode = groupMode,
            playbackSnapshot = playbackSnapshot,
            labels = groupLabels
        )
    }
    val selectedIndex = displayedFavorites.indexOfFirst { it.audioUrl == selectedAudioUrl }
    val selectedFavorite = displayedFavorites.getOrNull(selectedIndex)
    val queueItemIndex = 1 +
        if (removedFavorite == null) 0 else 1 +
        if (displayedFavorites.isEmpty()) 0 else 1
    val showMiniPlayer by remember(listState, selectedFavorite, queueItemIndex) {
        derivedStateOf {
            selectedFavorite != null &&
                (
                    listState.firstVisibleItemIndex > queueItemIndex ||
                        (
                            listState.firstVisibleItemIndex == queueItemIndex &&
                                listState.firstVisibleItemScrollOffset > 280
                        )
                    )
        }
    }

    LaunchedEffect(selectedAudioUrl, queueModeName) {
        GuideBgmFavoritePlaybackStore.saveSelection(
            audioUrl = selectedAudioUrl,
            queueModeName = queueModeName
        )
    }

    LaunchedEffect(selectedFavorite?.audioUrl, queueMode) {
        val favorite = selectedFavorite ?: return@LaunchedEffect
        val resumePosition = GuideBgmFavoritePlaybackStore
            .progressFor(favorite.audioUrl)
            ?.resumePositionMs
            ?: 0L
        prepareFavoriteBgmPlayback(
            context = appContext,
            favorite = favorite,
            queueMode = queueMode,
            startPositionMs = resumePosition
        )
    }

    LaunchedEffect(selectedFavorite?.audioUrl, queueMode, displayedFavorites) {
        val favorite = selectedFavorite ?: return@LaunchedEffect
        while (true) {
            val runtimeState = favoriteBgmRuntimeState(appContext, favorite)
            playbackRuntimeState = runtimeState
            if (runtimeState.durationMs > 0L || runtimeState.positionMs > 0L || runtimeState.isPlaying) {
                GuideBgmFavoritePlaybackStore.saveProgress(
                    audioUrl = favorite.audioUrl,
                    positionMs = runtimeState.positionMs,
                    durationMs = runtimeState.durationMs,
                    isPlaying = runtimeState.isPlaying
                )
            }
            if (
                runtimeState.isEnded &&
                queueMode == BaGuideBgmQueueMode.Continuous &&
                displayedFavorites.size > 1
            ) {
                selectQueueOffset(offset = 1, startPlayback = true)
                return@LaunchedEffect
            }
            delay(500L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() +
                    AppChromeTokens.pageSectionGap +
                    if (selectedFavorite != null) 124.dp else 0.dp,
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
                    BaGuideBgmGroupModeRow(
                        groupMode = groupMode,
                        accent = accent,
                        onGroupModeChange = { groupModeName = it.name }
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
                item(key = "bgm-cache-controls") {
                    BaGuideBgmCacheControls(
                        favoriteCount = displayedFavorites.size,
                        cachedCount = cachedFavoriteCount,
                        cacheBytes = cachedFavoriteBytes,
                        batchCaching = batchCaching,
                        batchDone = batchCacheDone,
                        batchTotal = batchCacheTotal,
                        batchFailedCount = displayedFavorites.count { favorite ->
                            favorite.audioUrl in batchFailedAudioUrls && !isFavoriteBgmCached(appContext, favorite)
                        },
                        accent = accent,
                        onCacheAll = ::cacheDisplayedFavorites,
                        onRetryFailed = ::retryFailedCache
                    )
                }

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
                            audioAutoPlayRequestToken = 0,
                            mediaUrlResolver = { raw -> resolveFavoriteBgmMediaUrl(appContext, favorite, raw) },
                            onPrevious = { selectQueueOffset(offset = -1, startPlayback = true) },
                            onNext = { selectQueueOffset(offset = 1, startPlayback = true) },
                            onToggleQueueMode = {
                                val nextMode = if (queueMode == BaGuideBgmQueueMode.Continuous) {
                                    BaGuideBgmQueueMode.SingleLoop
                                } else {
                                    BaGuideBgmQueueMode.Continuous
                                }
                                queueModeName = nextMode.name
                                applyFavoriteBgmQueueMode(appContext, favorite, nextMode)
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

                displayedFavoriteGroups.forEach { group ->
                    if (groupMode != BaGuideBgmFavoriteGroupMode.All) {
                        item(key = "bgm-group-${group.key}") {
                            BaGuideBgmFavoriteGroupHeader(
                                label = group.label,
                                count = group.favorites.size,
                                accent = accent
                            )
                        }
                    }
                    items(
                        items = group.favorites,
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
                            onPlay = { startFavoritePlayback(favorite) },
                            onCache = { cacheFavorite(favorite) },
                            onRemove = { removeFavorite(favorite) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showMiniPlayer,
            enter = appFloatingEnter(),
            exit = appFloatingExit(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = AppChromeTokens.pageHorizontalPadding,
                    end = AppChromeTokens.pageHorizontalPadding,
                    bottom = innerPadding.calculateBottomPadding() + AppChromeTokens.pageSectionGap
                )
        ) {
            selectedFavorite?.let { favorite ->
                BaGuideBgmMiniPlayer(
                    favorite = favorite,
                    runtimeState = playbackRuntimeState,
                    queueIndex = selectedIndex.coerceAtLeast(0),
                    queueSize = displayedFavorites.size,
                    accent = accent,
                    onOpenQueue = {
                        pageScope.launch {
                            listState.animateScrollToItem(queueItemIndex)
                        }
                    },
                    onPrevious = { selectQueueOffset(offset = -1, startPlayback = true) },
                    onTogglePlayback = {
                        val resumePosition = GuideBgmFavoritePlaybackStore
                            .progressFor(favorite.audioUrl)
                            ?.resumePositionMs
                            ?: 0L
                        toggleFavoriteBgmPlayback(
                            context = appContext,
                            favorite = favorite,
                            queueMode = queueMode,
                            startPositionMs = resumePosition
                        )
                    },
                    onNext = { selectQueueOffset(offset = 1, startPlayback = true) }
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
