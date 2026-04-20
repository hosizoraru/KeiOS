package com.example.keios.ui.page.main.student.page

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.example.keios.R
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.createGameKeeMediaSourceFactory
import com.example.keios.ui.page.main.student.GuideBottomTab
import com.example.keios.ui.page.main.student.page.component.BaStudentGuideBottomBar
import com.example.keios.ui.page.main.student.page.component.BaStudentGuidePagerContent
import com.example.keios.ui.page.main.student.page.support.rememberGuideSyncProgress
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuideInfoLoadEffect
import com.example.keios.ui.page.main.student.page.state.rememberBaStudentGuideMediaSaveAction
import com.example.keios.ui.page.main.student.page.state.rememberBaStudentGuidePageActions
import com.example.keios.ui.page.main.student.page.state.rememberBaStudentGuideTabSelectCoordinator
import com.example.keios.ui.page.main.student.page.state.rememberBaStudentGuideTopBarActionItems
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuidePagerSyncEffects
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuidePlayerLifecycleEffects
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuidePrefetchEffects
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuideSourceRestoreEffect
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuideVoiceListenerEffect
import com.example.keios.ui.page.main.student.page.state.BindBaStudentGuideVoiceProgressEffect
import com.example.keios.ui.perf.ReportPagerPerformanceState
import com.example.keios.ui.page.main.widget.glass.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBar
import com.example.keios.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.appFloatingEnter
import com.example.keios.ui.page.main.widget.motion.appFloatingExit
import com.example.keios.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.os.appLucideBackIcon
import com.example.keios.ui.page.main.os.appLucideRefreshIcon
import com.example.keios.ui.page.main.os.appLucideShareIcon
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

    var sourceUrl by rememberSaveable { mutableStateOf(BaStudentGuideStore.loadCurrentUrl()) }
    var info by remember(sourceUrl) { mutableStateOf<BaStudentGuideInfo?>(null) }
    val guideSyncToken = info?.syncedAtMs ?: -1L
    var loading by remember(sourceUrl) { mutableStateOf(sourceUrl.isNotBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }
    var manualRefreshRequested by remember { mutableStateOf(false) }
    var selectedBottomTabIndex by rememberSaveable(sourceUrl) { mutableIntStateOf(0) }
    var selectedVoiceLanguage by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var playingVoiceUrl by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var isVoicePlaying by remember(sourceUrl) { mutableStateOf(false) }
    var voicePlayProgress by remember(sourceUrl) { mutableFloatStateOf(0f) }
    var galleryPrefetchRequested by rememberSaveable(sourceUrl, guideSyncToken) { mutableStateOf(false) }
    var staticImagePrefetchStage by rememberSaveable(sourceUrl, guideSyncToken) { mutableIntStateOf(0) }
    var galleryCacheRevision by remember(sourceUrl) { mutableIntStateOf(0) }
    val bottomTabs = GuideBottomTab.entries
    val bottomTabsList = remember { bottomTabs.toList() }
    val pagerState = rememberPagerState(
        initialPage = selectedBottomTabIndex,
        pageCount = { bottomTabs.size }
    )
    ReportPagerPerformanceState(
        scope = "guide_detail_pager",
        currentPage = bottomTabs.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }.name,
        targetPage = bottomTabs.getOrElse(pagerState.targetPage) { GuideBottomTab.Archive }.name,
        scrolling = pagerState.isScrollInProgress
    )
    val activeBottomTab = bottomTabs.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }
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
        onSelectedBottomTabIndexChange = { selectedBottomTabIndex = it }
    )
    val bottomBarNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f && showBottomBar) {
                    showBottomBar = false
                }
                if (available.y > 1f && !showBottomBar) {
                    showBottomBar = true
                }
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
        onManualRefreshRequestedChange = { manualRefreshRequested = it },
        onSourceUrlChange = { sourceUrl = it },
        onErrorChange = { error = it },
        onRefreshSignalIncrease = { refreshSignal += 1 },
        saveGuideMedia = saveGuideMediaAction
    )

    BindBaStudentGuideSourceRestoreEffect(
        sourceUrl = sourceUrl,
        onSourceUrlChange = { sourceUrl = it },
        onErrorChange = { error = it }
    )
    BindBaStudentGuidePagerSyncEffects(
        sourceUrl = sourceUrl,
        bottomTabsSize = bottomTabs.size,
        selectedBottomTabIndex = selectedBottomTabIndex,
        pagerState = pagerState,
        onSelectedBottomTabIndexChange = { selectedBottomTabIndex = it }
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
    BindBaStudentGuideInfoLoadEffect(
        context = context,
        sourceUrl = sourceUrl,
        refreshSignal = refreshSignal,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        initialFetchDelayMs = preloadPolicy.initialFetchDelayMs,
        manualRefreshRequested = manualRefreshRequested,
        onManualRefreshRequestedChange = { manualRefreshRequested = it },
        currentSourceUrlProvider = { sourceUrl },
        currentInfoProvider = { info },
        onInfoChange = { info = it },
        onErrorChange = { error = it },
        onLoadingChange = { loading = it }
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
            onToggleVoicePlayback = pageActions.toggleVoicePlayback,
            onSelectedVoiceLanguageChange = { selectedVoiceLanguage = it }
        )
    }
}
