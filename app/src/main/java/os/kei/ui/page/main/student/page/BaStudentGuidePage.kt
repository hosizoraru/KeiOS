package os.kei.ui.page.main.student.page

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import os.kei.R
import os.kei.ui.page.main.student.createGameKeeMediaSourceFactory
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.page.component.BaStudentGuideBottomBar
import os.kei.ui.page.main.student.page.component.BaStudentGuidePagerContent
import os.kei.ui.page.main.student.page.support.rememberGuideSyncProgress
import os.kei.ui.page.main.student.page.support.resolveGuideBottomTabs
import os.kei.ui.page.main.student.page.state.rememberBaStudentGuideMediaSaveAction
import os.kei.ui.page.main.student.page.state.rememberBaStudentGuideMediaPackSaveAction
import os.kei.ui.page.main.student.page.state.rememberBaStudentGuidePageActions
import os.kei.ui.page.main.student.page.state.rememberBaStudentGuideTabSelectCoordinator
import os.kei.ui.page.main.student.page.state.rememberBaStudentGuideTopBarActionItems
import os.kei.ui.page.main.student.page.state.BindBaStudentGuidePagerSyncEffects
import os.kei.ui.page.main.student.page.state.BindBaStudentGuidePlayerLifecycleEffects
import os.kei.ui.page.main.student.page.state.BindBaStudentGuidePrefetchEffects
import os.kei.ui.page.main.student.page.state.BaStudentGuideViewModel
import os.kei.ui.page.main.student.page.state.BindBaStudentGuideVoiceListenerEffect
import os.kei.ui.page.main.student.page.state.BindBaStudentGuideVoiceProgressEffect
import os.kei.ui.perf.ReportPagerPerformanceState
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBar
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.ScrollChromeVisibilityController
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import os.kei.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import os.kei.core.prefs.UiPrefs
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BaStudentGuidePage(
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    preloadingEnabled: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val preloadPolicy = remember(preloadingEnabled) {
        UiPerformanceBudget.resolvePreloadPolicy(preloadingEnabled)
    }
    val defaultPageTitle = stringResource(R.string.guide_page_title_default)
    val shareSourceEmptyText = stringResource(R.string.guide_share_source_empty)
    val shareSourceChooserTitle = stringResource(R.string.guide_share_source_chooser_title)
    val shareSourceFailedText = stringResource(R.string.common_share_failed)
    val openLinkFailedText = stringResource(R.string.common_open_link_failed)
    val shareSourceContentDescription = stringResource(R.string.guide_cd_share_source)
    val refreshContentDescription = stringResource(R.string.common_refresh)
    val loadFailedText = stringResource(R.string.guide_load_failed)
    val refreshFailedKeepCacheText = stringResource(R.string.guide_refresh_failed_keep_cached)
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface
    // Keep backdrop allocation stable per page lifecycle to avoid RenderThread native crashes
    // when rapidly switching guide tabs on some HyperOS builds.
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    // Keep top-level backdrop only for navigator/pager layer and bottom bar.
    val navBackdrop: LayerBackdrop = key("nav-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    // Top action bar uses its own backdrop instance to avoid cross-layer recursion.
    val topBarBackdrop: LayerBackdrop = key("topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val scrollBehavior = MiuixScrollBehavior()

    val guideViewModel: BaStudentGuideViewModel = viewModel()
    val guideDataState by guideViewModel.dataState.collectAsState()
    LaunchedEffect(
        guideViewModel,
        transitionAnimationsEnabled,
        preloadPolicy.initialFetchDelayMs,
        loadFailedText,
        refreshFailedKeepCacheText
    ) {
        guideViewModel.bind(
            transitionAnimationsEnabled = transitionAnimationsEnabled,
            initialFetchDelayMs = preloadPolicy.initialFetchDelayMs,
            loadFailedText = loadFailedText,
            refreshFailedKeepCacheText = refreshFailedKeepCacheText
        )
    }
    val sourceUrl = guideDataState.sourceUrl
    val info = guideDataState.info
    val guideSyncToken = info?.syncedAtMs ?: -1L
    val loading = guideDataState.loading
    val error = guideDataState.error
    var selectedBottomTabOrdinal by rememberSaveable(sourceUrl) {
        mutableIntStateOf(GuideBottomTab.Archive.ordinal)
    }
    var selectedVoiceLanguage by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var playingVoiceUrl by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var isVoicePlaying by remember(sourceUrl) { mutableStateOf(false) }
    var voicePlayProgress by remember(sourceUrl) { mutableFloatStateOf(0f) }
    var galleryPrefetchRequested by rememberSaveable(sourceUrl, guideSyncToken) { mutableStateOf(false) }
    var staticImagePrefetchStage by rememberSaveable(sourceUrl, guideSyncToken) { mutableIntStateOf(0) }
    var galleryCacheRevision by remember(sourceUrl) { mutableIntStateOf(0) }
    val bottomTabsList = remember(info) { resolveGuideBottomTabs(info) }
    LaunchedEffect(bottomTabsList, selectedBottomTabOrdinal) {
        if (bottomTabsList.none { it.ordinal == selectedBottomTabOrdinal }) {
            selectedBottomTabOrdinal = bottomTabsList.firstOrNull()?.ordinal ?: GuideBottomTab.Archive.ordinal
        }
    }
    val selectedBottomTabIndex = bottomTabsList.indexOfFirst { tab ->
        tab.ordinal == selectedBottomTabOrdinal
    }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(
        initialPage = selectedBottomTabIndex,
        pageCount = { bottomTabsList.size }
    )
    ReportPagerPerformanceState(
        scope = "guide_detail_pager",
        currentPage = bottomTabsList.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }.name,
        targetPage = bottomTabsList.getOrElse(pagerState.targetPage) { GuideBottomTab.Archive }.name,
        scrolling = pagerState.isScrollInProgress
    )
    val activeBottomTab = bottomTabsList.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }
    val pageScope = rememberCoroutineScope()
    val syncProgress = rememberGuideSyncProgress(
        loading = loading,
        animationsEnabled = transitionAnimationsEnabled
    )
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liquidBottomBarEnabled = remember { UiPrefs.isLiquidBottomBarEnabled() }
    var showBottomBar by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
    val selectBottomTabAction = rememberBaStudentGuideTabSelectCoordinator(
        bottomTabs = bottomTabsList,
        pagerState = pagerState,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        farJumpAlpha = farJumpAlpha,
        onShowBottomBarChange = { showBottomBar = it },
        onSelectedBottomTabIndexChange = { selectedIndex ->
            selectedBottomTabOrdinal = bottomTabsList
                .getOrNull(selectedIndex)
                ?.ordinal
                ?: GuideBottomTab.Archive.ordinal
        }
    )
    val density = LocalDensity.current
    val bottomBarVisibilityThresholdPx = remember(density) { with(density) { 22.dp.toPx() } }
    val bottomBarVisibilityController = remember(bottomBarVisibilityThresholdPx) {
        ScrollChromeVisibilityController(bottomBarVisibilityThresholdPx)
    }
    val bottomBarNestedScrollConnection = remember(bottomBarVisibilityController) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                bottomBarVisibilityController.update(consumed.y, showBottomBar) { showBottomBar = it }
                return Offset.Zero
            }
        }
    }
    val pageTitle = info?.title?.ifBlank { defaultPageTitle } ?: defaultPageTitle
    val voicePlayer = remember(context, sourceUrl) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
            .build()
    }
    val saveGuideMediaAction = rememberBaStudentGuideMediaSaveAction(
        pageScope = pageScope,
        currentStudentNamePrefix = { info?.title?.trim().orEmpty() }
    )
    val saveGuideMediaPackAction = rememberBaStudentGuideMediaPackSaveAction(
        pageScope = pageScope,
        currentStudentNamePrefix = { info?.title?.trim().orEmpty() }
    )

    BindBaStudentGuidePlayerLifecycleEffects(
        context = context,
        sourceUrl = sourceUrl,
        voicePlayer = voicePlayer
    )
    BindBaStudentGuideVoiceListenerEffect(
        context = context,
        voicePlayer = voicePlayer,
        playingVoiceUrl = playingVoiceUrl,
        onPlayingVoiceUrlChange = { playingVoiceUrl = it },
        onIsVoicePlayingChange = { isVoicePlaying = it },
        onVoicePlayProgressChange = { voicePlayProgress = it }
    )
    val pageActions = rememberBaStudentGuidePageActions(
        info = info,
        sourceUrl = sourceUrl,
        shareSourceEmptyText = shareSourceEmptyText,
        shareSourceChooserTitle = shareSourceChooserTitle,
        shareSourceFailedText = shareSourceFailedText,
        openLinkFailedText = openLinkFailedText,
        voicePlayer = voicePlayer,
        playingVoiceUrl = playingVoiceUrl,
        onPlayingVoiceUrlChange = { playingVoiceUrl = it },
        onIsVoicePlayingChange = { isVoicePlaying = it },
        onVoicePlayProgressChange = { voicePlayProgress = it },
        onOpenGuideInPage = guideViewModel::openGuide,
        onRefresh = guideViewModel::requestRefresh,
        saveGuideMedia = saveGuideMediaAction,
        saveGuideMediaPack = saveGuideMediaPackAction
    )

    BindBaStudentGuidePagerSyncEffects(
        sourceUrl = sourceUrl,
        bottomTabsSize = bottomTabsList.size,
        selectedBottomTabIndex = selectedBottomTabIndex,
        pagerState = pagerState,
        onSelectedBottomTabIndexChange = { selectedIndex ->
            selectedBottomTabOrdinal = bottomTabsList
                .getOrNull(selectedIndex)
                ?.ordinal
                ?: GuideBottomTab.Archive.ordinal
        }
    )
    BindBaStudentGuideVoiceProgressEffect(
        activeBottomTab = activeBottomTab,
        isVoicePlaying = isVoicePlaying,
        playingVoiceUrl = playingVoiceUrl,
        voicePlayer = voicePlayer,
        onVoicePlayProgressChange = { voicePlayProgress = it }
    )
    BindBaStudentGuidePrefetchEffects(
        context = context,
        sourceUrl = sourceUrl,
        guideSyncToken = guideSyncToken,
        info = info,
        activeBottomTab = activeBottomTab,
        galleryPrefetchRequested = galleryPrefetchRequested,
        onGalleryPrefetchRequestedChange = { galleryPrefetchRequested = it },
        staticImagePrefetchStage = staticImagePrefetchStage,
        onStaticImagePrefetchStageChange = { staticImagePrefetchStage = it },
        initialPrefetchCount = preloadPolicy.guideStaticPrefetchInitialCount,
        galleryExtraPrefetchCount = preloadPolicy.guideStaticPrefetchGalleryExtraCount,
        onGalleryCacheRevisionIncrease = { galleryCacheRevision += 1 }
    )
    val shareIcon = appLucideShareIcon()
    val refreshIcon = appLucideRefreshIcon()
    val actionItems = rememberBaStudentGuideTopBarActionItems(
        shareIcon = shareIcon,
        refreshIcon = refreshIcon,
        shareSourceContentDescription = shareSourceContentDescription,
        refreshContentDescription = refreshContentDescription,
        onShareSource = pageActions.shareSource,
        onRefresh = pageActions.requestRefresh
    )
    val topBarCollapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val topBarTitleAlpha = 1f - topBarCollapsedFraction

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .nestedScroll(bottomBarNestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = pageTitle,
                    largeTitle = pageTitle,
                    scrollBehavior = scrollBehavior,
                    color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = topBarTitleAlpha),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = appLucideBackIcon(),
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BaStudentGuideBottomBar(
                    visible = showBottomBar,
                    navigationBarBottom = navigationBarBottom,
                    bottomTabs = bottomTabsList,
                    selectedPage = pagerState.targetPage,
                    selectedPageProvider = { pagerState.targetPage },
                    backdrop = navBackdrop,
                    isLiquidEffectEnabled = liquidBottomBarEnabled,
                    onSelectTab = selectBottomTabAction
                )
            }
        ) { innerPadding ->
            BaStudentGuidePagerContent(
                sourceUrl = sourceUrl,
                info = info,
                error = error,
                pagerState = pagerState,
                bottomTabs = bottomTabsList,
                syncProgress = syncProgress,
                activationCount = activationCount,
                surfaceColor = surfaceColor,
                accent = accent,
                innerPadding = innerPadding,
                farJumpAlpha = farJumpAlpha.value,
                navBackdrop = navBackdrop,
                galleryCacheRevision = galleryCacheRevision,
                selectedVoiceLanguage = selectedVoiceLanguage,
                playingVoiceUrl = playingVoiceUrl,
                isVoicePlaying = isVoicePlaying,
                voicePlayProgress = voicePlayProgress,
                includeTargetPageInHeavyRender = preloadPolicy.includeTargetPageInHeavyRender,
                guidePagerBeyondViewportPageCount = preloadPolicy.guidePagerBeyondViewportPageCount,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                onOpenExternal = pageActions.openExternal,
                onOpenGuide = pageActions.openGuideInPage,
                onSaveMedia = pageActions.saveGuideMedia,
                onSaveMediaPack = pageActions.saveGuideMediaPack,
                onToggleVoicePlayback = pageActions.toggleVoicePlayback,
                onSelectedVoiceLanguageChange = { selectedVoiceLanguage = it }
            )
        }
        AppTopEndActionBarOverlay {
            LiquidActionBar(
                backdrop = topBarBackdrop,
                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                items = actionItems
            )
        }
    }
}
