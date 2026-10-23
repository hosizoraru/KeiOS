@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.ui.page.main.common.applicationViewModel
import os.kei.ui.page.main.host.pager.rememberMainLoadedPagerState
import os.kei.ui.page.main.student.LocalGuideMediaGifTargetRequester
import os.kei.ui.page.main.student.LocalGuideMediaGifTargets
import os.kei.ui.page.main.student.LocalGuideMediaImageBitmaps
import os.kei.ui.page.main.student.LocalGuideMediaImageMissingKeys
import os.kei.ui.page.main.student.LocalGuideMediaImageRequester
import os.kei.ui.page.main.student.catalog.component.LocalBaGuideCatalogImageBitmaps
import os.kei.ui.page.main.student.catalog.component.LocalBaGuideCatalogImageRequester
import os.kei.ui.page.main.student.catalog.component.bgm.rememberBaGuideBgmBottomChromeScrollState
import os.kei.ui.page.main.student.catalog.component.rememberBaGuideBgmPlaybackCoordinator
import os.kei.ui.page.main.student.catalog.component.withResolvedCatalogStudentMetadata
import os.kei.ui.page.main.student.catalog.state.BaGuideCatalogViewModel
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogFilterSortState
import os.kei.ui.page.main.widget.chrome.LocalSearchAutoFocusEnabled
import os.kei.ui.page.main.widget.chrome.appManagedPageBackgroundActive
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.perf.ReportPagerPerformanceState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BaGuideCatalogPage(
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
    notificationPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    preloadingEnabled: Boolean = false,
    enableSearchBar: Boolean = true,
    openBgmPlaybackToken: Long = 0L,
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val pageScope = rememberCoroutineScope()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val preloadPolicy =
        remember(preloadingEnabled) {
            UiPerformanceBudget.resolvePreloadPolicy(preloadingEnabled)
        }
    val pageTitle = stringResource(R.string.ba_catalog_page_title)
    val accent = MiuixTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    // Transparent while a non-Home background is painting, otherwise this opaque panel covers it and the
    // setting silently does nothing on this page.
    val panelBackground =
        when {
            appManagedPageBackgroundActive() -> Color.Transparent
            isDark -> Color(0xFF10141B)
            else -> MiuixTheme.colorScheme.background
        }
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val pageChromeBackdrop: LayerBackdrop =
        key("ba-catalog-page-$activationCount-$isDark") {
            rememberLayerBackdrop()
        }
    val bottomChromeBackdrop: LayerBackdrop =
        key("ba-catalog-bottom-$activationCount-$isDark") {
            rememberLayerBackdrop()
        }

    val loadFailedText = stringResource(R.string.ba_catalog_load_failed)
    val refreshFailedKeepCacheText = stringResource(R.string.ba_catalog_refresh_failed_keep_cached)
    val exportDoneText = stringResource(R.string.ba_catalog_transfer_export_done)
    val exportFailedText = stringResource(R.string.ba_catalog_transfer_export_failed)
    val studentExportSuccessText = stringResource(R.string.ba_catalog_transfer_student_export_success)
    val allExportSuccessText = stringResource(R.string.ba_catalog_transfer_all_export_success)
    val bgmExportSuccessText = stringResource(R.string.ba_catalog_bgm_export_success)
    val catalogViewModel: BaGuideCatalogViewModel = applicationViewModel(create = ::BaGuideCatalogViewModel)
    val routeState = collectBaGuideCatalogRouteState(catalogViewModel)
    val imageState by catalogViewModel.imageState.collectAsStateWithLifecycle()
    val mediaImageState by catalogViewModel.mediaImageState.collectAsStateWithLifecycle()
    val pageChromeState by catalogViewModel.pageChromeState.collectAsStateWithLifecycle()
    val filterSortSnapshot by catalogViewModel.filterSortState.collectAsStateWithLifecycle()
    val pendingBgmFavoriteUndo by catalogViewModel.pendingBgmFavoriteUndo.collectAsStateWithLifecycle()
    val tabs = BaGuideCatalogPageTab.entries
    val pageState =
        rememberBaGuideCatalogPageStateHolder(
            chromeState = pageChromeState,
            chromeActions =
                BaGuideCatalogPageChromeActions(
                    onSelectedTabIndexChange = catalogViewModel::updateCatalogSelectedTabIndex,
                    onSelectedStudentCatalogTabChange = catalogViewModel::updateCatalogSelectedStudentCatalogTab,
                    onSearchQueriesChange = catalogViewModel::updateCatalogSearchQueries,
                    onShowTransferSheetChange = catalogViewModel::updateCatalogTransferSheetVisible,
                    onShowMorePopupChange = catalogViewModel::updateCatalogMorePopupVisible,
                    onCatalogIncrementalRefreshIntervalHoursChange =
                        catalogViewModel::updateCatalogIncrementalRefreshIntervalHours,
                    onImportPreviewStateChange = catalogViewModel::updateCatalogImportPreviewState,
                    onSearchVisibilityChange = catalogViewModel::updateCatalogSearchVisibility,
                    onBgmVolumeControlVisibleChange = catalogViewModel::updateCatalogBgmVolumeControlVisible,
                    onBgmLastAudibleVolumeChange = catalogViewModel::updateCatalogBgmLastAudibleVolume,
                    onPlaybackSliderInteractionChange = catalogViewModel::updateCatalogPlaybackSliderInteractionActive,
                    onStudentBgmNowPlayingVisibleChange = catalogViewModel::updateCatalogStudentBgmNowPlayingVisible,
                    onStudentBgmNowPlayingExpandedChange = catalogViewModel::updateCatalogStudentBgmNowPlayingExpanded,
                    onStudentBgmSliderInteractionChange = catalogViewModel::updateCatalogStudentBgmSliderInteractionActive,
                ),
        )
    val pageActions = rememberBaGuideCatalogPageActions(catalogViewModel)
    BaGuideCatalogPageEventEffects(
        context = context,
        catalogViewModel = catalogViewModel,
        onImportPreviewStateChange = pageState::updateImportPreviewState,
    )
    BaGuideCatalogPageBindEffects(
        pageActions = pageActions,
        transitionAnimationsEnabled = transitionAnimationsEnabled,
        initialFetchDelayMs = preloadPolicy.initialFetchDelayMs,
        loadFailedText = loadFailedText,
        refreshFailedKeepCacheText = refreshFailedKeepCacheText,
    )

    val pagerState =
        rememberMainLoadedPagerState(
            initialPage = pageState.selectedTabIndex.coerceIn(0, tabs.lastIndex.coerceAtLeast(0)),
            pageCount = tabs.size,
        )
    val filterSortState =
        rememberBaGuideCatalogFilterSortState(
            snapshot = filterSortSnapshot,
            activeCatalogTab =
                tabs
                    .getOrNull(pageState.selectedTabIndex)
                    ?.resolvedCatalogTab(pageState.selectedStudentCatalogTab),
            onSnapshotChange = catalogViewModel::updateCatalogFilterSortState,
        )
    BaGuideCatalogPageDerivedEffects(
        pageActions = pageActions,
        catalogDataState = routeState.catalogDataState,
        filterSortState = filterSortState,
        catalogFavoriteEntries = routeState.catalogFavoriteEntries,
        favoriteBgms = routeState.favoriteBgms,
        pageState = pageState,
    )
    val chromeTabs = rememberBaGuideCatalogChromeTabs()
    val chromeScrollState = rememberBaGuideBgmBottomChromeScrollState()
    val playbackFavorites =
        remember(routeState.favoriteBgms, routeState.catalogDataState.catalog) {
            routeState.favoriteBgms.map { favorite ->
                favorite.withResolvedCatalogStudentMetadata(routeState.catalogDataState.catalog)
            }
        }
    val playbackCoordinator =
        rememberBaGuideBgmPlaybackCoordinator(
            context = appContext,
            favorites = playbackFavorites,
            nativeMediaNotificationEnabled = routeState.nativeBgmMediaNotificationEnabled,
        )
    val playbackSessionState by playbackCoordinator.sessionState.collectAsStateWithLifecycle()
    LaunchedEffect(openBgmPlaybackToken) {
        if (openBgmPlaybackToken <= 0L) return@LaunchedEffect
        val playbackTabIndex = tabs.indexOf(BaGuideCatalogPageTab.Bgm)
        if (playbackTabIndex < 0) return@LaunchedEffect
        pageState.activatePlaybackTab(playbackTabIndex)
        pagerState.scrollToPage(playbackTabIndex)
        chromeScrollState.expand()
    }

    val chromePresentation =
        rememberBaGuideCatalogChromePresentation(
            pagerState = pagerState,
            pageState = pageState,
            tabs = tabs,
            catalogDataState = routeState.catalogDataState,
            playbackUiState = playbackSessionState,
        )
    BindBaGuideCatalogImagePreloadEffect(
        routeState = routeState,
        chromePresentation = chromePresentation,
        requestCatalogImages = catalogViewModel::requestCatalogImages,
    )
    LaunchedEffect(chromePresentation.activeTab, chromePresentation.activeCatalogTab) {
        chromeScrollState.expand()
        if (chromePresentation.activeCatalogTab == null) {
            filterSortState.showFilterPopup = false
        }
    }
    val bgmCacheState =
        rememberBaGuideCatalogBgmCacheState(
            snapshot = routeState.bgmCacheSnapshot,
            favoriteCount = routeState.favoriteBgms.size,
            onCacheAllBgm = { pageActions.onCacheMissingBgms(routeState.favoriteBgms) },
            onCleanInvalidBgmCache = { pageActions.onCleanInvalidBgmCache(routeState.favoriteBgms) },
        )
    BaGuideCatalogBgmCacheEffects(
        pageActions = pageActions,
        favoriteBgms = routeState.favoriteBgms,
        showTransferSheet = pageState.showTransferSheet,
    )
    val transferExportAction =
        rememberBaGuideCatalogJsonExportAction(
            context = context,
            pageScope = pageScope,
            transferSettings = routeState.transferSettings,
            exportDoneText = exportDoneText,
            exportFailedText = exportFailedText,
            onArmSafExportRequest = catalogViewModel::armPendingSafJsonExportRequest,
            onConsumeSafExportRequest = catalogViewModel::consumePendingSafJsonExportRequest,
            onArmFixedExportRequest = catalogViewModel::armPendingFixedJsonExportRequest,
            onConsumeFixedExportRequest = catalogViewModel::consumePendingFixedJsonExportRequest,
            onClearFixedExportRequest = catalogViewModel::clearPendingFixedJsonExportRequest,
            onMediaSaveCustomEnabledChange = pageActions.onSetTransferMediaSaveCustomEnabled,
            onMediaSaveFixedTreeUriSelected = pageActions.onSetTransferMediaSaveFixedTreeUri,
            onMediaSaveFixedTreeUriCleared = pageActions.onClearTransferMediaSaveFixedTreeUri,
        )
    val importActions =
        rememberBaGuideCatalogImportActions(
            onRequestImportPreview = pageActions.onRequestImportPreview,
            onConfirmFavoritesImport = pageActions.onConfirmFavoritesImport,
        )

    LaunchedEffect(pagerState.settledPage) {
        pageState.updateSettledPage(pagerState.settledPage)
    }
    ReportPagerPerformanceState(
        scope = "guide_catalog_music_pager",
        currentPage = tabs.getOrElse(pagerState.currentPage) { BaGuideCatalogPageTab.Student }.name,
        targetPage = tabs.getOrElse(pagerState.targetPage) { BaGuideCatalogPageTab.Student }.name,
        scrolling = pagerState.isScrollInProgress,
    )

    val searchAutoFocusEnabled = LocalSearchAutoFocusEnabled.current

    CompositionLocalProvider(
        LocalBaGuideCatalogImageBitmaps provides imageState.bitmaps,
        LocalBaGuideCatalogImageRequester provides catalogViewModel::requestVisibleCatalogImages,
        LocalGuideMediaImageBitmaps provides mediaImageState.bitmaps,
        LocalGuideMediaImageMissingKeys provides mediaImageState.missingKeys,
        LocalGuideMediaGifTargets provides mediaImageState.resolvedGifTargets,
        LocalGuideMediaImageRequester provides catalogViewModel::requestGuideMediaImages,
        LocalGuideMediaGifTargetRequester provides catalogViewModel::requestGuideMediaGifTargets,
    ) {
        BaGuideCatalogPageContent(
            pageTitle = pageTitle,
            accent = accent,
            isDark = isDark,
            panelBackground = panelBackground,
            pageChromeBackdrop = pageChromeBackdrop,
            bottomChromeBackdrop = bottomChromeBackdrop,
            pagerState = pagerState,
            tabs = tabs,
            pageState = pageState,
            filterSortState = filterSortState,
            catalogFavoriteEntries = routeState.catalogFavoriteEntries,
            catalogDataState = routeState.catalogDataState,
            catalogListDerivedStates = routeState.catalogListDerivedStates,
            studentBgmListDerivedState = routeState.studentBgmListDerivedState,
            memoryLobbyListDerivedState = routeState.memoryLobbyListDerivedState,
            studentBgmDisplayedDerivedState = routeState.studentBgmDisplayedDerivedState,
            favoriteBgmListDerivedState = routeState.favoriteBgmListDerivedState,
            favoriteBgms = routeState.favoriteBgms,
            pendingBgmFavoriteUndo = pendingBgmFavoriteUndo,
            favoriteBgmOfflineCacheState = routeState.favoriteBgmOfflineCacheState,
            playbackCoordinator = playbackCoordinator,
            playbackUiState = playbackSessionState,
            chromeScrollState = chromeScrollState,
            chromeTabs = chromeTabs,
            chromePresentation = chromePresentation,
            transferExportAction = transferExportAction,
            importActions = importActions,
            bgmCacheState = bgmCacheState,
            nativeBgmMediaNotificationEnabled = routeState.nativeBgmMediaNotificationEnabled,
            notificationPermissionGranted = notificationPermissionGranted,
            allExportSuccessText = allExportSuccessText,
            studentExportSuccessText = studentExportSuccessText,
            bgmExportSuccessText = bgmExportSuccessText,
            mediaAdaptiveRotationEnabled = routeState.mediaAdaptiveRotationEnabled,
            transitionAnimationsEnabled = transitionAnimationsEnabled,
            searchAutoFocusEnabled = searchAutoFocusEnabled,
            enableSearchBar = enableSearchBar,
            onBack = onBack,
            onOpenGuide = onOpenGuide,
            onRequestVisibleCatalogImages = catalogViewModel::requestVisibleCatalogImages,
            pageActions = pageActions,
            onRequestNotificationPermission = onRequestNotificationPermission,
        )
    }
}
