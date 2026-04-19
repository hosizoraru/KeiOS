package com.example.keios.ui.page.main

import android.graphics.Bitmap
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogEntry
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogIconCache
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.clearBaGuideCatalogCache
import com.example.keios.ui.page.main.student.catalog.fetchBaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.filterByQuery
import com.example.keios.ui.page.main.student.catalog.hydrateBaGuideCatalogReleaseDateIndex
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogBundleComplete
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import com.example.keios.ui.page.main.student.catalog.loadCachedBaGuideCatalogBundle
import com.example.keios.ui.perf.ReportPagerPerformanceState
import com.example.keios.ui.page.main.widget.AppChromeTokens
import com.example.keios.ui.page.main.widget.AppMotionTokens
import com.example.keios.ui.page.main.widget.AppStatusPillSize
import com.example.keios.ui.page.main.widget.AppTopBarSearchField
import com.example.keios.ui.page.main.widget.AppTopBarSection
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.LiquidGlassBottomBar
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.appFloatingEnter
import com.example.keios.ui.page.main.widget.appFloatingExit
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.resolvedMotionDuration
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.max
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val CATALOG_BATCH_SIZE = 20
private const val CATALOG_LOAD_MORE_THRESHOLD = 10
private const val CATALOG_RELEASE_DATE_FETCH_LIMIT_PER_PASS = 24

private enum class BaGuideCatalogSortMode(@StringRes val labelRes: Int) {
    Default(R.string.ba_catalog_sort_default),
    ReleaseDateDesc(R.string.ba_catalog_sort_release_date_desc),
    ReleaseDateAsc(R.string.ba_catalog_sort_release_date_asc),
}

@Composable
fun BaGuideCatalogPage(
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    preloadingEnabled: Boolean = false,
    enableSearchBar: Boolean = true,
) {
    val context = LocalContext.current
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val preloadPolicy = remember(preloadingEnabled) {
        UiPerformanceBudget.resolvePreloadPolicy(preloadingEnabled)
    }
    val pageTitle = stringResource(R.string.ba_catalog_page_title)
    val sortActionContentDescription = stringResource(R.string.ba_catalog_action_sort)
    val refreshActionContentDescription = stringResource(R.string.ba_catalog_action_refresh)
    val searchLabel = stringResource(R.string.ba_catalog_search_label)
    val syncStatusTitle = stringResource(R.string.ba_catalog_sync_status_title)
    val syncStatusBody = stringResource(R.string.ba_catalog_sync_status_body_retry)
    val emptyTitle = stringResource(R.string.ba_catalog_empty_title)
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val topBarBackdrop: LayerBackdrop = key("catalog-topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val bottomBarBackdrop: LayerBackdrop = key("catalog-bottom-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val scrollBehavior = MiuixScrollBehavior()

    var refreshSignal by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf(BaGuideCatalogBundle.EMPTY) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(BaGuideCatalogSortMode.Default) }
    var showSortPopup by remember { mutableStateOf(false) }
    var favoriteCatalogEntries by remember { mutableStateOf(BaGuideCatalogStore.loadFavorites()) }
    val emptySubtitle = if (searchQuery.isBlank()) {
        stringResource(R.string.ba_catalog_empty_subtitle_default)
    } else {
        stringResource(R.string.ba_catalog_empty_subtitle_search)
    }

    val tabs = BaGuideCatalogTab.entries
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex.coerceIn(0, tabs.lastIndex.coerceAtLeast(0)),
        pageCount = { tabs.size }
    )
    ReportPagerPerformanceState(
        scope = "guide_catalog_pager",
        currentPage = tabs.getOrElse(pagerState.currentPage) { BaGuideCatalogTab.Student }.name,
        targetPage = tabs.getOrElse(pagerState.targetPage) { BaGuideCatalogTab.Student }.name,
        scrolling = pagerState.isScrollInProgress
    )
    val pagerScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(pagerState.settledPage) {
        if (selectedTabIndex != pagerState.settledPage) {
            selectedTabIndex = pagerState.settledPage
        }
    }

    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liquidBottomBarEnabled = remember { UiPrefs.isLiquidBottomBarEnabled() }
    val newBottomBarTransitionEnabled = remember { UiPrefs.isNewBottomBarTransitionEnabled() }
    var showBottomBar by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    var showSearchBar by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    var searchBarHideOffsetPx by remember { mutableStateOf(0f) }
    val searchBarHideThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }
    val bottomBarNestedScrollConnection = remember(searchBarHideThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) {
                    showBottomBar = false
                    if (showSearchBar) {
                        searchBarHideOffsetPx =
                            (searchBarHideOffsetPx + (-available.y)).coerceAtMost(searchBarHideThresholdPx)
                        if (searchBarHideOffsetPx >= searchBarHideThresholdPx) {
                            showSearchBar = false
                            searchBarHideOffsetPx = 0f
                        }
                    }
                }
                if (available.y > 1f) {
                    showBottomBar = true
                    showSearchBar = true
                    searchBarHideOffsetPx = 0f
                }
                return Offset.Zero
            }
        }
    }

    fun selectCatalogTab(index: Int) {
        if (index !in tabs.indices) return
        val stablePageIndex = if (pagerState.isScrollInProgress) {
            pagerState.targetPage
        } else {
            pagerState.settledPage
        }
        if (index == stablePageIndex) return
        selectedTabIndex = index
        showSortPopup = false
        tabJumpJob?.cancel()
        tabJumpJob = pagerScope.launch {
            pagerState.animateTabSwitch(
                fromIndex = stablePageIndex,
                targetIndex = index,
                animationsEnabled = transitionAnimationsEnabled,
                onFarJumpBefore = {
                    farJumpAlpha.snapTo(1f)
                    farJumpAlpha.animateTo(
                        targetValue = 0.92f,
                        animationSpec = tween(
                            durationMillis = resolvedMotionDuration(
                                AppMotionTokens.farJumpDimMs,
                                transitionAnimationsEnabled
                            )
                        )
                    )
                },
                onFarJumpAfter = {
                    farJumpAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = resolvedMotionDuration(
                                AppMotionTokens.farJumpRestoreMs,
                                transitionAnimationsEnabled
                            )
                        )
                    )
                }
            )
        }
    }

    fun toggleCatalogFavorite(contentId: Long) {
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

    LaunchedEffect(refreshSignal) {
        if (refreshSignal == 0 && transitionAnimationsEnabled && preloadPolicy.initialFetchDelayMs > 0) {
            delay(preloadPolicy.initialFetchDelayMs.toLong())
        }
        val manualRefresh = refreshSignal > 0
        val now = System.currentTimeMillis()
        loading = true
        val refreshIntervalHours = withContext(Dispatchers.IO) {
            BASettingsStore.loadCalendarRefreshIntervalHours()
        }
        val cachedBundle = withContext(Dispatchers.IO) { loadCachedBaGuideCatalogBundle() }
        val cacheComplete = isBaGuideCatalogBundleComplete(cachedBundle)
        val cacheExpired = isBaGuideCatalogCacheExpired(
            bundle = cachedBundle,
            refreshIntervalHours = refreshIntervalHours,
            nowMs = now
        )

        if (!manualRefresh && cacheComplete && !cacheExpired) {
            catalog = cachedBundle!!
            error = null
            loading = false
            return@LaunchedEffect
        }

        val shouldClearLocalCache = manualRefresh || (cachedBundle != null && (cacheExpired || !cacheComplete))
        if (shouldClearLocalCache) {
            withContext(Dispatchers.IO) { clearBaGuideCatalogCache(context) }
        }

        val result = withContext(Dispatchers.IO) {
            runCatching { fetchBaGuideCatalogBundle(forceRefresh = true) }
        }
        result.onSuccess { latest ->
            catalog = latest
            error = null
        }.onFailure {
            error = if (catalog.entriesByTab.values.all { it.isEmpty() }) {
                "图鉴列表加载失败"
            } else {
                "刷新失败，已显示上次列表"
            }
        }
        loading = false
    }

    LaunchedEffect(catalog.syncedAtMs, loading) {
        if (loading) return@LaunchedEffect
        if (catalog.entriesByTab.values.all { it.isEmpty() }) return@LaunchedEffect
        hydrateBaGuideCatalogReleaseDateIndex(
            source = catalog,
            maxNetworkFetchPerPass = CATALOG_RELEASE_DATE_FETCH_LIMIT_PER_PASS,
            onBundleUpdated = { updated ->
                catalog = updated
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .nestedScroll(bottomBarNestedScrollConnection),
        topBar = {
            AppTopBarSection(
                title = pageTitle,
                largeTitle = pageTitle,
                scrollBehavior = scrollBehavior,
                color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = appLucideBackIcon(),
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Box {
                        LiquidActionBar(
                            backdrop = topBarBackdrop,
                            layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                            items = listOf(
                                LiquidActionItem(
                                    icon = appLucideSortIcon(),
                                    contentDescription = sortActionContentDescription,
                                    onClick = { showSortPopup = !showSortPopup }
                                ),
                                LiquidActionItem(
                                    icon = appLucideRefreshIcon(),
                                    contentDescription = refreshActionContentDescription,
                                    onClick = { refreshSignal += 1 }
                                )
                            )
                        )
                        LiquidActionBarPopupAnchors(itemCount = 2) { slotIndex, popupAnchorBounds ->
                            if (slotIndex == 0 && showSortPopup) {
                                SnapshotWindowListPopup(
                                    show = showSortPopup,
                                    alignment = PopupPositionProvider.Align.BottomStart,
                                    anchorBounds = popupAnchorBounds,
                                    placement = SnapshotPopupPlacement.ActionBarCenter,
                                    onDismissRequest = { showSortPopup = false },
                                    enableWindowDim = false
                                ) {
                                    LiquidDropdownColumn {
                                        val modes = BaGuideCatalogSortMode.entries
                                        modes.forEachIndexed { index, mode ->
                                            LiquidDropdownImpl(
                                                text = stringResource(mode.labelRes),
                                                optionSize = modes.size,
                                                isSelected = sortMode == mode,
                                                index = index,
                                                onSelectedIndexChange = { selectedIndex ->
                                                    sortMode = modes[selectedIndex]
                                                    showSortPopup = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                searchBarVisible = enableSearchBar && showSearchBar
            ) {
                Column {
                    AppTopBarSearchField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppChromeTokens.searchFieldHorizontalPadding),
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = searchLabel
                    )
                }
            }
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = appFloatingEnter(useNewBottomBarTransition = newBottomBarTransitionEnabled),
                    exit = appFloatingExit(useNewBottomBarTransition = newBottomBarTransitionEnabled),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val bottomBarModifier = Modifier.padding(
                        horizontal = AppChromeTokens.pageHorizontalPadding,
                        vertical = AppChromeTokens.pageSectionGap + navigationBarBottom
                    )
                    val bottomBarTabs: @Composable RowScope.() -> Unit = {
                        tabs.forEachIndexed { index, tab ->
                            FloatingBottomBarItem(
                                onClick = { selectCatalogTab(index) },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = 1f
                                            scaleY = 1f
                                        }
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }

                    if (newBottomBarTransitionEnabled) {
                        LiquidGlassBottomBar(
                            modifier = bottomBarModifier,
                            selectedIndex = { pagerState.targetPage },
                            onSelected = { index ->
                                if (index != pagerState.targetPage) {
                                    selectCatalogTab(index)
                                }
                            },
                            tabsCount = tabs.size,
                            isLiquidEffectEnabled = liquidBottomBarEnabled,
                            content = bottomBarTabs
                        )
                    } else {
                        FloatingBottomBar(
                            modifier = bottomBarModifier,
                            selectedIndex = { pagerState.targetPage },
                            onSelected = { index ->
                                // Ignore mirror callbacks emitted after pager state sync.
                                if (index != pagerState.targetPage) {
                                    selectCatalogTab(index)
                                }
                            },
                            backdrop = bottomBarBackdrop,
                            tabsCount = tabs.size,
                            isBlurEnabled = liquidBottomBarEnabled,
                            content = bottomBarTabs
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val progress = rememberCatalogSyncProgress(
            loading = loading,
            animationsEnabled = transitionAnimationsEnabled
        )
        val progressColor = when {
            loading -> Color(0xFF3B82F6)
            !error.isNullOrBlank() -> Color(0xFFEF4444)
            else -> Color(0xFF22C55E)
        }
        HorizontalPager(
            state = pagerState,
            key = { index -> tabs[index].name },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = farJumpAlpha.value }
                .layerBackdrop(bottomBarBackdrop),
            beyondViewportPageCount = preloadPolicy.catalogPagerBeyondViewportPageCount
        ) { pageIndex ->
            val pageTab = tabs.getOrElse(pageIndex) { BaGuideCatalogTab.Student }
            CatalogTabContent(
                tab = pageTab,
                catalog = catalog,
                sortMode = sortMode,
                favoriteCatalogEntries = favoriteCatalogEntries,
                searchQuery = searchQuery,
                loading = loading,
                error = error,
                progress = progress,
                progressColor = progressColor,
                accent = accent,
                innerPadding = innerPadding,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                isPageActive = pageIndex == pagerState.currentPage,
                renderHeavyContent = pageIndex == pagerState.currentPage ||
                    pageIndex == pagerState.settledPage ||
                    (preloadPolicy.includeTargetPageInHeavyRender && pageIndex == pagerState.targetPage),
                onOpenGuide = onOpenGuide,
                onToggleFavorite = ::toggleCatalogFavorite
            )
        }
    }
}

@Composable
private fun CatalogTabContent(
    tab: BaGuideCatalogTab,
    catalog: BaGuideCatalogBundle,
    sortMode: BaGuideCatalogSortMode,
    favoriteCatalogEntries: Map<Long, Long>,
    searchQuery: String,
    loading: Boolean,
    error: String?,
    progress: Float,
    progressColor: Color,
    accent: Color,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    isPageActive: Boolean,
    renderHeavyContent: Boolean,
    onOpenGuide: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit,
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
                text = tab.label,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    val currentEntries = remember(catalog, tab, sortMode, favoriteCatalogEntries) {
        catalog.entries(tab).sortedByMode(sortMode, favoriteCatalogEntries)
    }
    val filteredEntries = remember(currentEntries, searchQuery) {
        currentEntries.filterByQuery(searchQuery)
    }
    val listState = rememberLazyListState()
    var visibleCount by rememberSaveable(tab, searchQuery) { mutableIntStateOf(0) }
    LaunchedEffect(filteredEntries.size) {
        visibleCount = minOf(filteredEntries.size, CATALOG_BATCH_SIZE)
    }
    LaunchedEffect(isPageActive, listState, filteredEntries.size, loading) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlow {
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
    val syncStatusTitle = stringResource(R.string.ba_catalog_sync_status_title)
    val syncStatusBody = stringResource(R.string.ba_catalog_sync_status_body_retry)
    val emptyTitle = stringResource(R.string.ba_catalog_empty_title)
    val emptySubtitle = if (searchQuery.isBlank()) {
        stringResource(R.string.ba_catalog_empty_subtitle_default)
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SmallTitle(stringResource(R.string.ba_catalog_tab_title, tab.label))
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

                if (!error.isNullOrBlank()) {
                    item {
                        FrostedBlock(
                            backdrop = null,
                            title = syncStatusTitle,
                            subtitle = error.orEmpty(),
                            body = syncStatusBody,
                            accent = Color(0xFFEF4444)
                        )
                    }
            }

                if (!loading && filteredEntries.isEmpty()) {
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
                    items = displayedEntries,
                    key = { "${it.tab.name}-${it.entryId}-${it.contentId}" }
                ) { entry ->
                    BaGuideCatalogEntryCard(
                        entry = entry,
                        isFavorite = favoriteCatalogEntries.containsKey(entry.contentId),
                        onOpenGuide = onOpenGuide,
                        onToggleFavorite = onToggleFavorite
                    )
                }
                if (visibleCount < filteredEntries.size) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = 0.3f,
                                size = 16.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = accent,
                                    backgroundColor = accent.copy(alpha = 0.30f),
                                ),
                            )
                            Text(
                                text = " 继续加载更多条目…",
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
    }
}

@Composable
internal fun BaGuideCatalogEntryCard(
    entry: BaGuideCatalogEntry,
    isFavorite: Boolean,
    onOpenGuide: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedToast = stringResource(R.string.guide_toast_item_copied)
    val copyPayload = remember(entry) {
        buildString {
            append(entry.name.ifBlank { "未知角色" })
            append(" | ID ")
            append(entry.contentId)
            if (entry.aliasDisplay.isNotBlank()) {
                append('\n')
                append(entry.aliasDisplay)
            }
            if (entry.detailUrl.isNotBlank()) {
                append('\n')
                append(entry.detailUrl)
            }
        }
    }
    val copyAction = remember(context, clipboard, copiedToast, copyPayload) {
        {
            clipboard.setText(AnnotatedString(copyPayload))
            Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
        }
    }
    val cardShape = RoundedCornerShape(16.dp)
    val favoriteActionColor = if (isFavorite) {
        Color(0xFFEC4899)
    } else {
        Color(0xFF3B82F6)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isFavorite) {
                    Color(0x99EC4899)
                } else {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.24f)
                },
                shape = cardShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onOpenGuide(entry.detailUrl) },
                onLongClick = copyAction
            ),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = if (isFavorite) {
                Color(0x33EC4899)
            } else {
                MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                    vertical = CardLayoutRhythm.cardVerticalPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (entry.iconUrl.isBlank()) {
                    CatalogAvatarFallback(iconRes = entry.tab.iconRes)
                } else {
                    CatalogAvatarImage(
                        imageUrl = entry.iconUrl,
                        fallbackRes = entry.tab.iconRes
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowTextGap)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = entry.name,
                            modifier = Modifier.weight(1f),
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = AppTypographyTokens.CompactTitle.fontSize,
                            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusPill(
                            label = "ID ${entry.contentId}",
                            color = MiuixTheme.colorScheme.primary,
                            size = AppStatusPillSize.Compact
                        )
                    }
                    if (entry.aliasDisplay.isNotBlank()) {
                        Text(
                            text = entry.aliasDisplay,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = AppTypographyTokens.Supporting.fontSize,
                            lineHeight = AppTypographyTokens.Supporting.lineHeight,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            GlassIconButton(
                backdrop = null,
                icon = MiuixIcons.Regular.FavoritesFill,
                contentDescription = if (isFavorite) {
                    stringResource(R.string.ba_catalog_cd_unfavorite_student)
                } else {
                    stringResource(R.string.ba_catalog_cd_favorite_student)
                },
                onClick = { onToggleFavorite(entry.contentId) },
                width = 34.dp,
                height = 34.dp,
                variant = GlassVariant.Floating,
                iconTint = favoriteActionColor,
                containerColor = favoriteActionColor
            )
        }
    }
}

@Composable
private fun rememberCatalogSyncProgress(
    loading: Boolean,
    animationsEnabled: Boolean
): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(loading, animationsEnabled) {
        if (!animationsEnabled) {
            progress.snapTo(if (loading) 0.9f else 1f)
            return@LaunchedEffect
        }
        if (loading) {
            progress.snapTo(0.12f)
            progress.animateTo(
                targetValue = 0.68f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(520, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
            progress.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(1800, animationsEnabled),
                    easing = LinearEasing
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(260, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
        }
    }
    return progress.value
}

@Composable
private fun CatalogAvatarImage(
    imageUrl: String,
    fallbackRes: Int
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = BaGuideCatalogIconCache.get(imageUrl), imageUrl) {
        value = withContext(Dispatchers.IO) { BaGuideCatalogIconCache.getOrLoad(context, imageUrl) }
    }
    val rendered = bitmap
    if (rendered == null) {
        CatalogAvatarFallback(iconRes = fallbackRes)
        return
    }
    Image(
        bitmap = rendered.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun CatalogAvatarFallback(iconRes: Int) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun List<BaGuideCatalogEntry>.sortedByMode(
    mode: BaGuideCatalogSortMode,
    favoriteCatalogEntries: Map<Long, Long>
): List<BaGuideCatalogEntry> {
    val sortedBase = when (mode) {
        BaGuideCatalogSortMode.Default -> sortedBy { it.order }
        BaGuideCatalogSortMode.ReleaseDateDesc -> sortedWith(
            compareByDescending<BaGuideCatalogEntry> {
                when {
                    it.releaseDateSec > 0L -> it.releaseDateSec
                    it.createdAtSec > 0L -> it.createdAtSec
                    else -> Long.MIN_VALUE
                }
            }.thenBy { it.order }
        )
        BaGuideCatalogSortMode.ReleaseDateAsc -> sortedWith(
            compareBy<BaGuideCatalogEntry> {
                when {
                    it.releaseDateSec > 0L -> it.releaseDateSec
                    it.createdAtSec > 0L -> it.createdAtSec
                    else -> Long.MAX_VALUE
                }
            }.thenBy { it.order }
        )
    }
    if (favoriteCatalogEntries.isEmpty()) return sortedBase
    val pinnedFavorites = sortedBase
        .filter { entry -> favoriteCatalogEntries.containsKey(entry.contentId) }
        .sortedWith(
            compareBy<BaGuideCatalogEntry> {
                favoriteCatalogEntries[it.contentId] ?: Long.MAX_VALUE
            }.thenBy { it.order }
        )
    val regularEntries = sortedBase.filterNot { entry ->
        favoriteCatalogEntries.containsKey(entry.contentId)
    }
    return pinnedFavorites + regularEntries
}
