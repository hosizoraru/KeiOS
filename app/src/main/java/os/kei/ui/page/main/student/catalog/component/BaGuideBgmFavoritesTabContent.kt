package os.kei.ui.page.main.student.catalog.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import os.kei.R
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.GuideBgmFavoritePlaybackStore
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.page.state.GuideDetailTabRequestStore
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.LiquidInfoBlock
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BaGuideBgmFavoritesTabContent(
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
    var playbackRuntimeState by remember {
        mutableStateOf(BaGuideBgmPlaybackRuntimeState(volume = savedPlayback.volume))
    }
    var seekPreviewProgress by remember { mutableStateOf<Float?>(null) }
    var cacheRevision by remember { mutableIntStateOf(0) }
    var cachingAudioUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var batchCaching by remember { mutableStateOf(false) }
    var batchCacheDone by remember { mutableIntStateOf(0) }
    var batchCacheTotal by remember { mutableIntStateOf(0) }
    var batchFailedAudioUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingExportJson by remember { mutableStateOf("") }
    var exportingFavorites by remember { mutableStateOf(false) }
    var importingFavorites by remember { mutableStateOf(false) }
    var nowPlayingVisible by rememberSaveable { mutableStateOf(false) }
    var nowPlayingExpanded by remember { mutableStateOf(false) }
    var sliderInteractionActive by remember { mutableStateOf(false) }
    var removedFavorite by remember { mutableStateOf<GuideBgmFavoriteItem?>(null) }
    val cacheSuccessText = stringResource(R.string.ba_catalog_bgm_cache_success)
    val cacheFailedText = stringResource(R.string.ba_catalog_bgm_cache_failed)
    val cacheAllReadyText = stringResource(R.string.ba_catalog_bgm_cache_all_ready)
    val exportEmptyText = stringResource(R.string.ba_catalog_bgm_export_empty)
    val exportSuccessText = stringResource(R.string.ba_catalog_bgm_export_success)
    val exportFailedText = stringResource(R.string.ba_catalog_bgm_export_failed)
    val importFailedText = stringResource(R.string.ba_catalog_bgm_import_failed)

    fun setNowPlayingVisible(visible: Boolean) {
        nowPlayingVisible = visible
        onNowPlayingVisibilityChange(visible && selectedAudioUrl.isNotBlank())
    }

    fun setSliderInteractionActive(active: Boolean) {
        sliderInteractionActive = active
        onSliderInteractionChanged(active)
    }

    fun startFavoritePlayback(
        favorite: GuideBgmFavoriteItem,
        restart: Boolean = false,
        collapseNowPlaying: Boolean = false
    ) {
        selectedAudioUrl = favorite.audioUrl
        setNowPlayingVisible(true)
        if (collapseNowPlaying) {
            nowPlayingExpanded = false
        }
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

    fun selectQueueOffset(
        offset: Int,
        startPlayback: Boolean,
        collapseNowPlaying: Boolean = false,
        restart: Boolean = false
    ) {
        if (displayedFavorites.isEmpty()) return
        val currentIndex = displayedFavorites.indexOfFirst { it.audioUrl == selectedAudioUrl }
            .takeIf { it >= 0 }
            ?: 0
        val nextIndex = (currentIndex + offset + displayedFavorites.size) % displayedFavorites.size
        val nextFavorite = displayedFavorites[nextIndex]
        selectedAudioUrl = nextFavorite.audioUrl
        if (startPlayback) {
            startFavoritePlayback(
                favorite = nextFavorite,
                restart = restart,
                collapseNowPlaying = collapseNowPlaying
            )
        }
    }

    fun removeFavorite(favorite: GuideBgmFavoriteItem) {
        GuideBgmFavoriteStore.removeFavorite(favorite.audioUrl)
        removedFavorite = favorite
        if (selectedAudioUrl == favorite.audioUrl) {
            selectedAudioUrl = ""
            setNowPlayingVisible(false)
            nowPlayingExpanded = false
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

    val exportFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingExportJson
        pendingExportJson = ""
        if (uri == null || payload.isBlank()) {
            exportingFavorites = false
            return@rememberLauncherForActivityResult
        }
        pageScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                        if (writer == null) return@runCatching false
                        writer.write(payload)
                        true
                    }
                }.getOrDefault(false)
            }
            exportingFavorites = false
            Toast.makeText(
                context,
                if (success) exportSuccessText else exportFailedText,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val importFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            importingFavorites = false
            return@rememberLauncherForActivityResult
        }
        pageScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    GuideBgmFavoriteStore.importFavoritesJsonMerged(raw)
                }
            }
            importingFavorites = false
            result.onSuccess { importResult ->
                if (importResult.importedCount > 0) {
                    cacheRevision += 1
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.ba_catalog_bgm_import_success,
                            importResult.addedCount,
                            importResult.updatedCount
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, importFailedText, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, importFailedText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportFavorites() {
        if (favorites.isEmpty()) {
            Toast.makeText(context, exportEmptyText, Toast.LENGTH_SHORT).show()
            return
        }
        pendingExportJson = GuideBgmFavoriteStore.buildFavoritesExportJson()
        exportingFavorites = true
        exportFavoritesLauncher.launch("keios-ba-bgm-favorites.json")
    }

    fun importFavorites() {
        importingFavorites = true
        importFavoritesLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
    }

    val listState = rememberLazyListState()
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    LaunchedEffect(listState, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager.snapshotFlow { listState.canScrollBackward to listState.canScrollForward }
            .distinctUntilChanged()
            .collect { (canScrollBackward, canScrollForward) ->
                onScrollBoundsChange(canScrollBackward, canScrollForward)
            }
    }
    LaunchedEffect(listState, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager.snapshotFlow { listState.isScrollInProgress }
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
    val playlistHeaderItemIndex = 1 + if (removedFavorite == null) 0 else 1
    val showNowPlaying = selectedFavorite != null && nowPlayingVisible
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomChromePadding = if (showNowPlaying) {
        navigationBarBottom
    } else {
        innerPadding.calculateBottomPadding()
    }
    val nowPlayingBottomPadding = navigationBarBottom + AppChromeTokens.pageSectionGap

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

    LaunchedEffect(selectedFavorite?.audioUrl, queueMode, displayedFavorites) {
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
                displayedFavorites.size > 1
            ) {
                selectQueueOffset(
                    offset = 1,
                    startPlayback = true,
                    collapseNowPlaying = false,
                    restart = true
                )
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
            item(key = "bgm-library-header") {
                BaGuideBgmLibraryHeader(
                    favoriteCount = favorites.size,
                    displayedCount = displayedFavorites.size,
                    cachedCount = cachedFavoriteCount,
                    cacheBytes = cachedFavoriteBytes,
                    searchActive = searchQuery.isNotBlank(),
                    sortMode = sortMode,
                    groupMode = groupMode,
                    batchCaching = batchCaching,
                    batchDone = batchCacheDone,
                    batchTotal = batchCacheTotal,
                    batchFailedCount = displayedFavorites.count { favorite ->
                        favorite.audioUrl in batchFailedAudioUrls && !isFavoriteBgmCached(appContext, favorite)
                    },
                    exporting = exportingFavorites,
                    importing = importingFavorites,
                    accent = accent,
                    onSortModeChange = { sortModeName = it.name },
                    onGroupModeChange = { groupModeName = it.name },
                    onCacheAll = ::cacheDisplayedFavorites,
                    onRetryFailed = ::retryFailedCache,
                    onExport = ::exportFavorites,
                    onImport = ::importFavorites
                )
            }

            removedFavorite?.let { favorite ->
                item(key = "bgm-undo-${favorite.audioUrl}") {
                    BaGuideBgmUndoBlock(
                        removedFavorite = favorite,
                        accent = accent,
                        onUndo = {
                            GuideBgmFavoriteStore.restoreFavorite(favorite)
                            removedFavorite = null
                            val studentName = favorite.studentTitle.ifBlank {
                                context.getString(R.string.ba_catalog_bgm_student_unknown)
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.ba_catalog_bgm_restore_success, studentName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            if (displayedFavorites.isEmpty()) {
                item {
                    LiquidInfoBlock(
                        backdrop = null,
                        title = emptyTitle,
                        subtitle = emptySubtitle,
                        accent = accent
                    )
                }
            } else {
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
                            onPlay = {
                                startFavoritePlayback(
                                    favorite = favorite,
                                    collapseNowPlaying = false
                                )
                            },
                            onCache = { cacheFavorite(favorite) },
                            onRemove = { removeFavorite(favorite) }
                        )
                    }
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
                    queueSize = displayedFavorites.size,
                    queueMode = queueMode,
                    accent = accent,
                    expanded = nowPlayingExpanded,
                    onExpandedChange = { nowPlayingExpanded = it },
                    onOpenQueue = {
                        pageScope.launch {
                            listState.animateScrollToItem(playlistHeaderItemIndex)
                        }
                    },
                    onPrevious = {
                        selectQueueOffset(
                            offset = -1,
                            startPlayback = true,
                            collapseNowPlaying = false,
                            restart = true
                        )
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
                        selectQueueOffset(
                            offset = 1,
                            startPlayback = true,
                            collapseNowPlaying = false,
                            restart = true
                        )
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
                        val safeVolume = volume.coerceIn(0f, 1f)
                        playbackRuntimeState = updateFavoriteBgmVolume(
                            context = appContext,
                            favorite = favorite,
                            volume = safeVolume
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
