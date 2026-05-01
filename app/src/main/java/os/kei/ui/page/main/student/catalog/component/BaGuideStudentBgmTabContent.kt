package os.kei.ui.page.main.student.catalog.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.R
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoritePlaybackStore
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.filterByQuery
import os.kei.ui.page.main.student.fetch.normalizeGuideUrl
import os.kei.ui.page.main.student.page.state.GuideDetailTabRequestStore
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.FrostedBlock
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import kotlin.math.max

private const val STUDENT_BGM_BATCH_SIZE = 20
private const val STUDENT_BGM_LOAD_MORE_THRESHOLD = 10

@Composable
internal fun BaGuideStudentBgmTabContent(
    catalog: BaGuideCatalogBundle,
    searchQuery: String,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    accent: Color,
    isPageActive: Boolean,
    onScrollBoundsChange: (canScrollBackward: Boolean, canScrollForward: Boolean) -> Unit,
    onListScrollInProgressChange: (Boolean) -> Unit,
    onSliderInteractionChanged: (Boolean) -> Unit,
    onNowPlayingVisibilityChange: (Boolean) -> Unit,
    onOpenGuide: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = remember(context) { context.applicationContext }
    val pageScope = rememberCoroutineScope()
    val favorites by GuideBgmFavoriteStore.favoritesFlow().collectAsState()
    val savedPlayback = remember { GuideBgmFavoritePlaybackStore.snapshot() }
    val lookupStates = remember { mutableStateMapOf<Long, BaGuideStudentBgmLookupState>() }
    var selectedAudioUrl by rememberSaveable { mutableStateOf(savedPlayback.selectedAudioUrl) }
    var nowPlayingVisible by rememberSaveable { mutableStateOf(false) }
    var nowPlayingExpanded by remember { mutableStateOf(false) }
    var sliderInteractionActive by remember { mutableStateOf(false) }
    var playbackRuntimeState by remember {
        mutableStateOf(BaGuideBgmPlaybackRuntimeState(volume = savedPlayback.volume))
    }
    var seekPreviewProgress by remember { mutableStateOf<Float?>(null) }
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
    val bgmMissingText = stringResource(R.string.ba_catalog_student_bgm_toast_missing)
    val bgmResolveFailedText = stringResource(R.string.ba_catalog_student_bgm_toast_resolve_failed)
    val favoriteAddedText = stringResource(R.string.guide_bgm_toast_favorite_added)
    val favoriteRemovedText = stringResource(R.string.guide_bgm_toast_favorite_removed)

    val allStudentEntries = remember(catalog) {
        catalog.entries(BaGuideCatalogTab.Student).sortedBy { it.order }
    }
    val filteredEntries = remember(allStudentEntries, searchQuery) {
        allStudentEntries.filterByQuery(searchQuery)
    }
    val listState = rememberLazyListState()
    var visibleCount by rememberSaveable(searchQuery) { mutableIntStateOf(0) }
    LaunchedEffect(filteredEntries.size) {
        visibleCount = minOf(filteredEntries.size, STUDENT_BGM_BATCH_SIZE)
    }
    LaunchedEffect(isPageActive, listState, filteredEntries.size) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalCount) ->
                if (visibleCount >= filteredEntries.size) return@collect
                if (totalCount <= 0) return@collect
                val triggerIndex = (totalCount - 1 - STUDENT_BGM_LOAD_MORE_THRESHOLD).coerceAtLeast(0)
                if (lastVisible < triggerIndex) return@collect
                val viewportItems = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(6)
                val appendBatch = max(STUDENT_BGM_BATCH_SIZE, viewportItems * 3)
                    .coerceAtMost(STUDENT_BGM_BATCH_SIZE * 3)
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

    fun setNowPlayingVisible(visible: Boolean) {
        nowPlayingVisible = visible
        onNowPlayingVisibilityChange(visible && selectedAudioUrl.isNotBlank())
    }

    fun setSliderInteractionActive(active: Boolean) {
        sliderInteractionActive = active
        onSliderInteractionChanged(active)
    }

    fun openStudentGuide(entry: BaGuideCatalogEntry) {
        GuideDetailTabRequestStore.request(entry.detailUrl, GuideBottomTab.Gallery)
        onOpenGuide(entry.detailUrl)
    }

    fun openFavoriteGuide(favorite: GuideBgmFavoriteItem) {
        GuideDetailTabRequestStore.request(favorite.sourceUrl, GuideBottomTab.Gallery)
        onOpenGuide(favorite.sourceUrl)
    }

    fun favoriteForEntry(entry: BaGuideCatalogEntry): GuideBgmFavoriteItem? {
        val detailUrl = normalizeGuideUrl(entry.detailUrl)
        if (detailUrl.isBlank()) return null
        return favorites.firstOrNull { favorite ->
            normalizeGuideUrl(favorite.sourceUrl) == detailUrl
        }
    }

    fun stateWithFavoriteFallback(
        entry: BaGuideCatalogEntry,
        lookupState: BaGuideStudentBgmLookupState
    ): BaGuideStudentBgmLookupState {
        if (lookupState is BaGuideStudentBgmLookupState.Ready) return lookupState
        if (lookupState == BaGuideStudentBgmLookupState.Loading) return lookupState
        val favorite = favoriteForEntry(entry) ?: return lookupState
        return BaGuideStudentBgmLookupState.Ready(
            BaGuideStudentBgmResolvedItem(
                favorite = favorite,
                fromCache = false,
                fromFavorite = true
            )
        )
    }

    fun updateResolved(entry: BaGuideCatalogEntry, item: BaGuideStudentBgmResolvedItem?) {
        lookupStates[entry.contentId] = if (item == null) {
            BaGuideStudentBgmLookupState.Missing
        } else {
            BaGuideStudentBgmLookupState.Ready(item)
        }
    }

    fun resolveEntry(
        entry: BaGuideCatalogEntry,
        allowNetwork: Boolean,
        onResolved: (BaGuideStudentBgmResolvedItem?) -> Unit
    ) {
        val current = lookupStates[entry.contentId]
        if (current is BaGuideStudentBgmLookupState.Ready) {
            onResolved(current.item)
            return
        }
        if (current == BaGuideStudentBgmLookupState.Loading) return
        lookupStates[entry.contentId] = BaGuideStudentBgmLookupState.Loading
        pageScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                runCatching {
                    if (allowNetwork) {
                        fetchStudentBgmFavorite(entry)
                    } else {
                        loadCachedStudentBgmFavorite(entry)
                    }
                }.getOrNull()
            }
            if (allowNetwork || resolved != null) {
                updateResolved(entry, resolved)
            } else {
                lookupStates.remove(entry.contentId)
            }
            onResolved(resolved)
        }
    }

    fun startPlayback(favorite: GuideBgmFavoriteItem, restart: Boolean = false) {
        selectedAudioUrl = favorite.audioUrl
        setNowPlayingVisible(true)
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

    fun playEntry(entry: BaGuideCatalogEntry) {
        val lookupState = lookupStates[entry.contentId] ?: BaGuideStudentBgmLookupState.Idle
        stateWithFavoriteFallback(entry, lookupState).readyFavoriteOrNull()?.let { favorite ->
            if (lookupState !is BaGuideStudentBgmLookupState.Ready) {
                lookupStates[entry.contentId] = BaGuideStudentBgmLookupState.Ready(
                    BaGuideStudentBgmResolvedItem(
                        favorite = favorite,
                        fromCache = false,
                        fromFavorite = true
                    )
                )
            }
            startPlayback(favorite)
            return
        }
        resolveEntry(entry = entry, allowNetwork = true) { resolved ->
            val favorite = resolved?.favorite
            if (favorite == null) {
                Toast.makeText(context, bgmMissingText, Toast.LENGTH_SHORT).show()
            } else {
                startPlayback(favorite)
            }
        }
    }

    fun toggleEntryFavorite(entry: BaGuideCatalogEntry) {
        val lookupState = lookupStates[entry.contentId] ?: BaGuideStudentBgmLookupState.Idle
        if (lookupState !is BaGuideStudentBgmLookupState.Ready) {
            val savedFavorite = favoriteForEntry(entry)
            if (savedFavorite != null) {
                GuideBgmFavoriteStore.removeFavorite(savedFavorite.audioUrl)
                Toast.makeText(context, favoriteRemovedText, Toast.LENGTH_SHORT).show()
                return
            }
        }
        resolveEntry(entry = entry, allowNetwork = true) { resolved ->
            val favorite = resolved?.favorite
            if (favorite == null) {
                Toast.makeText(context, bgmResolveFailedText, Toast.LENGTH_SHORT).show()
                return@resolveEntry
            }
            val added = GuideBgmFavoriteStore.toggleFavorite(favorite)
            Toast.makeText(
                context,
                if (added) favoriteAddedText else favoriteRemovedText,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun isFavoriteEntry(
        entry: BaGuideCatalogEntry,
        lookupState: BaGuideStudentBgmLookupState
    ): Boolean {
        val readyAudioUrl = lookupState.readyFavoriteOrNull()?.audioUrl
        if (!readyAudioUrl.isNullOrBlank()) {
            return favorites.any { it.audioUrl == readyAudioUrl }
        }
        return favoriteForEntry(entry) != null
    }

    val displayedPlayableFavorites = remember(displayedEntries, favorites, lookupStates.toMap()) {
        displayedEntries.mapNotNull { entry ->
            val lookupState = lookupStates[entry.contentId] ?: BaGuideStudentBgmLookupState.Idle
            stateWithFavoriteFallback(entry, lookupState).readyFavoriteOrNull()
        }.distinctBy { it.audioUrl }
    }
    val selectedIndex = displayedPlayableFavorites.indexOfFirst { it.audioUrl == selectedAudioUrl }
    val selectedFavorite = displayedPlayableFavorites.getOrNull(selectedIndex)
    val displayedPlaybackRuntimeState = remember(playbackRuntimeState, seekPreviewProgress) {
        val previewProgress = seekPreviewProgress
        if (previewProgress != null && playbackRuntimeState.durationMs > 0L) {
            val durationMs = playbackRuntimeState.durationMs
            playbackRuntimeState.copy(
                positionMs = (durationMs * previewProgress.coerceIn(0f, 1f))
                    .toLong()
                    .coerceIn(0L, durationMs)
            )
        } else {
            playbackRuntimeState
        }
    }
    val showNowPlaying = selectedFavorite != null && nowPlayingVisible
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomChromePadding = if (showNowPlaying) {
        navigationBarBottom
    } else {
        innerPadding.calculateBottomPadding()
    }
    val nowPlayingBottomPadding = navigationBarBottom + AppChromeTokens.pageSectionGap

    fun selectQueueOffset(offset: Int, shouldStartPlayback: Boolean, restart: Boolean = false) {
        if (displayedPlayableFavorites.isEmpty()) return
        val currentIndex = displayedPlayableFavorites.indexOfFirst { it.audioUrl == selectedAudioUrl }
            .takeIf { it >= 0 }
            ?: 0
        val nextIndex = (currentIndex + offset + displayedPlayableFavorites.size) % displayedPlayableFavorites.size
        val nextFavorite = displayedPlayableFavorites[nextIndex]
        selectedAudioUrl = nextFavorite.audioUrl
        if (shouldStartPlayback) {
            startPlayback(nextFavorite, restart)
        }
    }

    LaunchedEffect(catalog.syncedAtMs) {
        lookupStates.clear()
    }
    LaunchedEffect(displayedEntries.map { it.contentId }) {
        val pendingEntries = displayedEntries.filter { entry ->
            lookupStates[entry.contentId] == null
        }
        if (pendingEntries.isEmpty()) return@LaunchedEffect
        val cached = withContext(Dispatchers.IO) {
            pendingEntries.mapNotNull { entry ->
                loadCachedStudentBgmFavorite(entry)?.let { entry.contentId to it }
            }
        }
        cached.forEach { (contentId, item) ->
            lookupStates[contentId] = BaGuideStudentBgmLookupState.Ready(item)
        }
    }
    LaunchedEffect(selectedAudioUrl, queueModeName) {
        GuideBgmFavoritePlaybackStore.saveSelection(
            audioUrl = selectedAudioUrl,
            queueModeName = queueModeName
        )
    }
    LaunchedEffect(selectedFavorite?.audioUrl) {
        seekPreviewProgress = null
    }
    LaunchedEffect(showNowPlaying) {
        onNowPlayingVisibilityChange(showNowPlaying)
    }
    LaunchedEffect(listState, isPageActive) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlow { listState.canScrollBackward to listState.canScrollForward }
            .distinctUntilChanged()
            .collect { (canScrollBackward, canScrollForward) ->
                onScrollBoundsChange(canScrollBackward, canScrollForward)
            }
    }
    LaunchedEffect(listState, isPageActive) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                onListScrollInProgressChange(scrolling)
            }
    }
    DisposableEffect(Unit) {
        onDispose { onNowPlayingVisibilityChange(false) }
    }
    DisposableEffect(Unit) {
        onDispose { onSliderInteractionChanged(false) }
    }
    DisposableEffect(lifecycleOwner, selectedFavorite?.audioUrl) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                selectedFavorite?.let { favorite ->
                    playbackRuntimeState = pauseFavoriteBgmPlayback(
                        context = appContext,
                        favorite = favorite
                    )
                }
                setNowPlayingVisible(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
    LaunchedEffect(selectedFavorite?.audioUrl, queueMode, displayedPlayableFavorites) {
        val favorite = selectedFavorite ?: return@LaunchedEffect
        while (true) {
            val runtimeState = favoriteBgmRuntimeState(appContext, favorite)
            playbackRuntimeState = runtimeState
            if (runtimeState.isPlaying && !nowPlayingVisible) {
                setNowPlayingVisible(true)
            }
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
                displayedPlayableFavorites.size > 1
            ) {
                selectQueueOffset(offset = 1, shouldStartPlayback = true, restart = true)
                return@LaunchedEffect
            }
            delay(500L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            userScrollEnabled = !sliderInteractionActive,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = listBottomChromePadding +
                    AppChromeTokens.pageSectionGap +
                    if (showNowPlaying) {
                        if (nowPlayingExpanded) 210.dp else 96.dp
                    } else {
                        0.dp
                    },
                start = AppChromeTokens.pageHorizontalPadding,
                end = AppChromeTokens.pageHorizontalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "student-bgm-header") {
                BaGuideStudentBgmHeader(
                    totalCount = allStudentEntries.size,
                    displayedCount = filteredEntries.size,
                    resolvedCount = displayedEntries.count { entry ->
                        val lookupState = lookupStates[entry.contentId] ?: BaGuideStudentBgmLookupState.Idle
                        stateWithFavoriteFallback(entry, lookupState) is BaGuideStudentBgmLookupState.Ready
                    },
                    favoriteCount = favorites.size,
                    loadingCount = lookupStates.values.count { it == BaGuideStudentBgmLookupState.Loading },
                    searchActive = searchQuery.isNotBlank(),
                    accent = accent
                )
            }

            if (filteredEntries.isEmpty()) {
                item(key = "student-bgm-empty") {
                    FrostedBlock(
                        backdrop = null,
                        title = stringResource(R.string.ba_catalog_empty_title),
                        subtitle = stringResource(R.string.ba_catalog_empty_subtitle_search),
                        accent = accent
                    )
                }
            } else {
                items(
                    items = displayedEntries,
                    key = { it.contentId }
                ) { entry ->
                    val lookupState = lookupStates[entry.contentId] ?: BaGuideStudentBgmLookupState.Idle
                    val displayState = stateWithFavoriteFallback(entry, lookupState)
                    val readyFavorite = displayState.readyFavoriteOrNull()
                    val selected = readyFavorite?.audioUrl == selectedAudioUrl
                    BaGuideStudentBgmCard(
                        entry = entry,
                        lookupState = displayState,
                        selected = selected,
                        playing = selected && playbackRuntimeState.isPlaying,
                        favorite = isFavoriteEntry(entry, lookupState),
                        accent = accent,
                        onOpenGuide = { openStudentGuide(entry) },
                        onPlay = { playEntry(entry) },
                        onToggleFavorite = { toggleEntryFavorite(entry) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying,
            enter = appFloatingEnter(),
            exit = appFloatingExit(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = AppChromeTokens.pageHorizontalPadding,
                    end = AppChromeTokens.pageHorizontalPadding,
                    bottom = nowPlayingBottomPadding
                )
        ) {
            selectedFavorite?.let { favorite ->
                BaGuideBgmMiniPlayer(
                    favorite = favorite,
                    runtimeState = displayedPlaybackRuntimeState,
                    queueIndex = selectedIndex.coerceAtLeast(0),
                    queueSize = displayedPlayableFavorites.size,
                    queueMode = queueMode,
                    accent = accent,
                    expanded = nowPlayingExpanded,
                    onExpandedChange = { nowPlayingExpanded = it },
                    onOpenQueue = {
                        pageScope.launch { listState.animateScrollToItem(0) }
                    },
                    onPrevious = {
                        selectQueueOffset(offset = -1, shouldStartPlayback = true, restart = true)
                    },
                    onTogglePlayback = {
                        setNowPlayingVisible(true)
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
                    onNext = {
                        selectQueueOffset(offset = 1, shouldStartPlayback = true, restart = true)
                    },
                    onSeekChanged = { progress ->
                        seekPreviewProgress = progress.coerceIn(0f, 1f)
                    },
                    onSeekFinished = {
                        val seekProgress = seekPreviewProgress
                            ?: displayedPlaybackRuntimeState.progress
                        val runtimeState = seekFavoriteBgmPlayback(
                            context = appContext,
                            favorite = favorite,
                            queueMode = queueMode,
                            progress = seekProgress
                        )
                        playbackRuntimeState = runtimeState
                        if (
                            runtimeState.durationMs > 0L ||
                            runtimeState.positionMs > 0L ||
                            runtimeState.isPlaying
                        ) {
                            GuideBgmFavoritePlaybackStore.saveProgress(
                                audioUrl = favorite.audioUrl,
                                positionMs = runtimeState.positionMs,
                                durationMs = runtimeState.durationMs,
                                isPlaying = runtimeState.isPlaying
                            )
                        }
                        seekPreviewProgress = null
                    },
                    onVolumeChanged = { volume ->
                        playbackRuntimeState = updateFavoriteBgmVolume(
                            context = appContext,
                            favorite = favorite,
                            volume = volume.coerceIn(0f, 1f)
                        )
                    },
                    onSliderInteractionChanged = ::setSliderInteractionActive,
                    onToggleQueueMode = {
                        val nextMode = if (queueMode == BaGuideBgmQueueMode.Continuous) {
                            BaGuideBgmQueueMode.SingleLoop
                        } else {
                            BaGuideBgmQueueMode.Continuous
                        }
                        queueModeName = nextMode.name
                        applyFavoriteBgmQueueMode(appContext, favorite, nextMode)
                    },
                    onOpenGuide = { openFavoriteGuide(favorite) }
                )
            }
        }
    }
}
