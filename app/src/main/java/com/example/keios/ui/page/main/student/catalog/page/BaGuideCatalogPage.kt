package com.example.keios.ui.page.main.student.catalog.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.host.pager.animateTabSwitch
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.os.appLucideRefreshIcon
import com.example.keios.ui.page.main.os.appLucideSortIcon
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.component.BaGuideCatalogSortActionPopup
import com.example.keios.ui.page.main.student.catalog.clearBaGuideCatalogCache
import com.example.keios.ui.page.main.student.catalog.component.BaGuideCatalogTabContent
import com.example.keios.ui.page.main.student.catalog.fetchBaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.hydrateBaGuideCatalogReleaseDateIndex
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogBundleComplete
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import com.example.keios.ui.page.main.student.catalog.loadCachedBaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.state.BaGuideCatalogSortMode
import com.example.keios.ui.page.main.student.catalog.state.CATALOG_RELEASE_DATE_FETCH_LIMIT_PER_PASS
import com.example.keios.ui.page.main.student.catalog.state.rememberBaGuideCatalogFilterSortState
import com.example.keios.ui.page.main.student.catalog.state.rememberCatalogSyncProgress
import com.example.keios.ui.page.main.widget.chrome.AppChromeTokens
import com.example.keios.ui.page.main.widget.chrome.AppTopBarSearchField
import com.example.keios.ui.page.main.widget.chrome.AppTopBarSection
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBar
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import com.example.keios.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import com.example.keios.ui.page.main.widget.glass.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.motion.AppMotionTokens
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.appFloatingEnter
import com.example.keios.ui.page.main.widget.motion.appFloatingExit
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import com.example.keios.ui.perf.ReportPagerPerformanceState
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val filterSortState = rememberBaGuideCatalogFilterSortState()

    val sortIcon = appLucideSortIcon()
    val refreshIcon = appLucideRefreshIcon()
    val actionItems = remember(
        sortActionContentDescription,
        refreshActionContentDescription,
        filterSortState.showSortPopup
    ) {
        listOf(
            LiquidActionItem(
                icon = sortIcon,
                contentDescription = sortActionContentDescription,
                onClick = { filterSortState.showSortPopup = !filterSortState.showSortPopup }
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshActionContentDescription,
                onClick = { refreshSignal += 1 }
            )
        )
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
                    if (showBottomBar) {
                        showBottomBar = false
                    }
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
                    if (!showBottomBar) {
                        showBottomBar = true
                    }
                    if (!showSearchBar) {
                        showSearchBar = true
                    }
                    if (searchBarHideOffsetPx != 0f) {
                        searchBarHideOffsetPx = 0f
                    }
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
        filterSortState.showSortPopup = false
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
                            items = actionItems
                        )
                        LiquidActionBarPopupAnchors(itemCount = 2) { slotIndex, popupAnchorBounds ->
                            if (slotIndex != 0) return@LiquidActionBarPopupAnchors
                            BaGuideCatalogSortActionPopup(
                                show = filterSortState.showSortPopup,
                                anchorBounds = popupAnchorBounds,
                                sortMode = filterSortState.sortMode,
                                onDismissRequest = { filterSortState.showSortPopup = false },
                                onSelectSortMode = filterSortState::selectSortMode
                            )
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
                        value = filterSortState.searchQuery,
                        onValueChange = { filterSortState.searchQuery = it },
                        label = searchLabel
                    )
                }
            }
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = appFloatingEnter(),
                    exit = appFloatingExit(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val bottomBarModifier = Modifier.padding(
                        horizontal = AppChromeTokens.pageHorizontalPadding,
                        vertical = AppChromeTokens.pageSectionGap + navigationBarBottom
                    )
                    val bottomBarTabs: @Composable RowScope.() -> Unit = {
                        tabs.forEachIndexed { index, tab ->
                            val selected = pagerState.targetPage == index
                            val tabColor = liquidGlassBottomBarItemContentColor(index)
                            val tabContent: @Composable ColumnScope.() -> Unit = {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label,
                                    tint = tabColor,
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
                                    color = tabColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                            LiquidGlassBottomBarItem(
                                selected = selected,
                                tabIndex = index,
                                onClick = { selectCatalogTab(index) },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                                content = tabContent
                            )
                        }
                    }

                    LiquidGlassBottomBar(
                        modifier = bottomBarModifier,
                        selectedIndex = pagerState.targetPage,
                        onSelected = { index ->
                            if (index != pagerState.targetPage) {
                                selectCatalogTab(index)
                            }
                        },
                        backdrop = bottomBarBackdrop,
                        tabsCount = tabs.size,
                        isLiquidEffectEnabled = liquidBottomBarEnabled,
                        content = bottomBarTabs
                    )
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
            BaGuideCatalogTabContent(
                tab = pageTab,
                catalog = catalog,
                filterSortState = filterSortState,
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
                onOpenGuide = onOpenGuide
            )
        }
    }
}
