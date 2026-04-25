package os.kei.ui.page.main.student.catalog.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import os.kei.R
import os.kei.core.prefs.UiPrefs
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideSortIcon
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.component.BaGuideCatalogSortActionPopup
import os.kei.ui.page.main.student.catalog.component.BaGuideCatalogTabContent
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogDataController
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogFilterSortState
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabSelectCoordinator
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTopBarActionItems
import os.kei.ui.page.main.student.catalog.state.rememberCatalogSyncProgress
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.chrome.AppTopBarSearchField
import os.kei.ui.page.main.widget.chrome.AppTopBarSection
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionBarPopupAnchors
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBar
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import os.kei.ui.page.main.widget.chrome.ScrollChromeVisibilityController
import os.kei.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import os.kei.ui.perf.ReportPagerPerformanceState
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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

    val loadFailedText = stringResource(R.string.ba_catalog_load_failed)
    val refreshFailedKeepCacheText = stringResource(R.string.ba_catalog_refresh_failed_keep_cached)
    val catalogData = rememberBaGuideCatalogDataController(
        context = context,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        initialFetchDelayMs = preloadPolicy.initialFetchDelayMs,
        loadFailedText = loadFailedText,
        refreshFailedKeepCacheText = refreshFailedKeepCacheText
    )
    val catalog = catalogData.catalog
    val loading = catalogData.loading
    val error = catalogData.error
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val filterSortState = rememberBaGuideCatalogFilterSortState()

    val sortIcon = appLucideSortIcon()
    val refreshIcon = appLucideRefreshIcon()
    val actionItems = rememberBaGuideCatalogTopBarActionItems(
        sortIcon = sortIcon,
        refreshIcon = refreshIcon,
        sortActionContentDescription = sortActionContentDescription,
        refreshActionContentDescription = refreshActionContentDescription,
        showSortPopup = filterSortState.showSortPopup,
        onShowSortPopupChange = { filterSortState.showSortPopup = it },
        onRefreshRequest = catalogData.requestRefresh
    )

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

    LaunchedEffect(pagerState.settledPage) {
        if (selectedTabIndex != pagerState.settledPage) {
            selectedTabIndex = pagerState.settledPage
        }
    }

    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liquidBottomBarEnabled = remember { UiPrefs.isLiquidBottomBarEnabled() }
    var showBottomBar by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    val selectCatalogTabAction = rememberBaGuideCatalogTabSelectCoordinator(
        tabs = tabs.toList(),
        pagerState = pagerState,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        farJumpAlpha = farJumpAlpha,
        onSelectedTabIndexChange = { selectedTabIndex = it },
        onSortPopupChange = { filterSortState.showSortPopup = it }
    )
    var showSearchBar by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val searchBarHideThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }
    val searchBarVisibilityController = remember(searchBarHideThresholdPx) {
        ScrollChromeVisibilityController(searchBarHideThresholdPx)
    }
    val bottomBarVisibilityController = remember(searchBarHideThresholdPx) {
        ScrollChromeVisibilityController(searchBarHideThresholdPx)
    }
    val bottomBarNestedScrollConnection = remember(
        searchBarVisibilityController,
        bottomBarVisibilityController
    ) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                bottomBarVisibilityController.update(consumed.y, showBottomBar) { showBottomBar = it }
                searchBarVisibilityController.update(consumed.y, showSearchBar) { showSearchBar = it }
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                                    onClick = { selectCatalogTabAction(index) },
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
                                    selectCatalogTabAction(index)
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
        AppTopEndActionBarOverlay {
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
        }
    }
}
