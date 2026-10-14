@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.page

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.core.privilege.PrivilegedShell
import os.kei.core.ui.effect.rememberAppTopBarColor
import os.kei.core.ui.resource.resolveString
import os.kei.ui.page.main.common.applicationViewModel
import os.kei.ui.page.main.github.LocalGitHubAppIconBitmaps
import os.kei.ui.page.main.github.section.GitHubMainContent
import os.kei.ui.page.main.github.section.GitHubMainContentActions
import os.kei.ui.page.main.github.section.GitHubMainContentControls
import os.kei.ui.page.main.github.section.GitHubMainContentLayout
import os.kei.ui.page.main.github.section.GitHubMainContentOverview
import os.kei.ui.page.main.github.section.GitHubMainContentShareImport
import os.kei.ui.page.main.github.section.GitHubMainContentSurfaces
import os.kei.ui.page.main.github.section.GitHubMainContentTracked
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.host.pager.rememberMainPageBackdropSet
import os.kei.ui.page.main.widget.chrome.appPageColumnCount
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import kotlin.math.abs

@Composable
fun GitHubPage(
    privilegedShell: PrivilegedShell,
    runtime: MainPageRuntime = MainPageRuntime(contentBottomPadding = 72.dp),
    externalNavigationTriggerToken: Int = 0,
    externalRefreshTriggerToken: Int = 0,
    externalActionsTrackId: String? = null,
    externalActionsSheetToken: Int = 0,
    enableSearchBar: Boolean = true,
    onOpenActionsNotificationHistory: () -> Unit = {},
    onOpenReleaseList: (String) -> Unit = {},
    onOpenFdroidVersionList: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val presentationOwnerActive = os.kei.ui.page.main.widget.glass.LocalLiquidPresentationOwnerActive.current
    val openLinkFailureMessage = context.resolveString(R.string.github_error_open_link)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // Two lanes on a tablet or an unfolded fold: collapsed cards on the left, the ones being read on the
    // right. See `githubTrackedLanesFor` for why a card moves rather than expanding where it sits.
    val secondaryListState = rememberLazyListState()
    val githubColumnCount = appPageColumnCount()
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = isAppInDarkTheme()
    val githubPageViewModel: GitHubPageViewModel = applicationViewModel(create = ::GitHubPageViewModel)
    val isListScrolling by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    val fullBackdropEffectsEnabled =
        runtime.isPageActive &&
            !runtime.isPagerScrollInProgress &&
            !isListScrolling
    val topBarBackdropEffectsEnabled =
        runtime.isPageActive &&
            !runtime.isPagerScrollInProgress
    val state = rememberGitHubPageState(githubPageViewModel)
    val sheetBackdropVisible =
        state.showStrategySheet ||
            state.showCheckLogicSheet ||
            state.showDroidSourcesSheet ||
            state.showDebugSheet ||
            state.showActionsSheet ||
            state.showAddSheet ||
            state.decisionAssistDetailRequest != null ||
            state.fdroidDetailRequest != null ||
            state.actionsArtifactDetailRequest != null ||
            state.apkInfoDetailRequest != null ||
            state.managedInstallConfirmRequest != null
    val backdrops =
        rememberMainPageBackdropSet(
            keyPrefix = "github",
            distinctLayers = fullBackdropEffectsEnabled && sheetBackdropVisible,
            useSolidSurfaceBackdrops = true,
        )
    val topBarColor =
        rememberAppTopBarColor(
            enableBackdropEffects = topBarBackdropEffectsEnabled,
        )

    var consumedExternalActionsSheetToken by rememberSaveable { mutableIntStateOf(0) }
    var consumedExternalNavigationToken by rememberSaveable { mutableIntStateOf(0) }
    val pageUiState by githubPageViewModel.uiState.collectAsStateWithLifecycle()
    val appIconState by githubPageViewModel.appIconState.collectAsStateWithLifecycle()
    val transferState = pageUiState.transferState
    val chromeState = pageUiState.chromeState
    val trackedItemsExpansionState = pageUiState.trackedItemsExpansionState
    val installedOnlineShareTargets = pageUiState.installedOnlineShareTargets
    val checkLogicDownloaderOptions = pageUiState.checkLogicDownloaderOptions
    val contentDerivedState = pageUiState.contentDerivedState
    val appPickerDerivedState = pageUiState.appPickerDerivedState
    val isGitHubPageDataActive = runtime.contentReady && runtime.isDataActive
    val isGitHubSettledDataActive = runtime.isSettledDataActive
    LaunchedEffect(isGitHubPageDataActive) {
        githubPageViewModel.setPageDataActive(isGitHubPageDataActive)
        if (isGitHubPageDataActive) {
            githubPageViewModel.refreshHistoryUnreadCount()
        }
    }
    DisposableEffect(githubPageViewModel) {
        onDispose { githubPageViewModel.setPageDataActive(false) }
    }
    LaunchedEffect(listState, runtime.onScrollBoundsChange) {
        snapshotFlow {
            GitHubListScrollSnapshot(
                canScrollBackward = listState.canScrollBackward,
                canScrollForward = listState.canScrollForward,
            )
        }.distinctUntilChanged()
            .collect { snapshot ->
                runtime.onScrollBoundsChange(
                    snapshot.canScrollBackward,
                    snapshot.canScrollForward,
                )
            }
    }
    LaunchedEffect(context, state, isGitHubSettledDataActive) {
        if (isGitHubSettledDataActive) {
            githubPageViewModel.bindContextObservers(state = state)
        } else {
            githubPageViewModel.unbindContextObservers()
        }
    }
    val actions =
        remember(
            context,
            scope,
            state,
            githubPageViewModel,
            githubPageViewModel.repository,
            privilegedShell,
            openLinkFailureMessage,
        ) {
            GitHubPageActions(
                context = context,
                scope = scope,
                durableScope = githubPageViewModel.durableActionScope,
                state = state,
                viewModel = githubPageViewModel,
                repository = githubPageViewModel.repository,
                privilegedShell = privilegedShell,
                openLinkFailureMessage = openLinkFailureMessage,
            )
        }
    val pinnedTrackIdSet = remember(state.pinnedTrackIds) { state.pinnedTrackIds.toSet() }
    val laneDetainedTrackIdSet = remember(state.laneDetainedTrackIds) { state.laneDetainedTrackIds.toSet() }
    // Opening a card in the wide layout moves it into the reading lane, and it stays there once collapsed.
    val onTrackedCardExpandedChange: (String, Boolean) -> Unit =
        remember(actions, githubColumnCount) {
            { itemId, expanded ->
                actions.setTrackedCardExpanded(itemId, expanded)
                if (expanded && githubColumnCount >= 2) {
                    actions.detainTrackedCardInReadingLane(itemId)
                }
            }
        }
    // Leaving the page is what sends the reading lane's cards home. Doing it on collapse instead would
    // pull a card out from under the reader the moment they closed it; doing it never would let the lane
    // silently accumulate everything they had ever opened.
    LaunchedEffect(runtime.isPageActive) {
        if (!runtime.isPageActive) actions.releaseTrackedReadingLane()
    }
    DisposableEffect(actions) {
        onDispose { actions.dispose() }
    }
    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            actions.refreshInstalledAppsAfterPermissionChange()
        }
    val launchAppListPermission: (Intent) -> Unit =
        remember(context, appListPermissionLauncher) {
            { intent: Intent ->
                runCatching { appListPermissionLauncher.launch(intent) }
                    .onFailure {
                        context.showToast(
                            context.getString(R.string.github_toast_open_permission_page_failed),
                        )
                    }
            }
        }
    val starImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGitHubStarImportActivityResult(
                result = result,
                actions = actions,
            )
        }
    val tracksExportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            handleGitHubTrackExportDestinationResult(
                context = context,
                scope = scope,
                githubPageViewModel = githubPageViewModel,
                uri = uri,
            )
        }
    val tracksImportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            handleGitHubTrackImportSourceResult(
                context = context,
                scope = scope,
                state = state,
                actions = actions,
                githubPageViewModel = githubPageViewModel,
                uri = uri,
            )
        }
    val transferCallbacks =
        remember(
            context,
            scope,
            state,
            actions,
            githubPageViewModel,
            tracksExportLauncher,
            tracksImportLauncher,
            starImportLauncher,
        ) {
            buildGitHubPageTrackTransferCallbacks(
                context = context,
                scope = scope,
                state = state,
                actions = actions,
                githubPageViewModel = githubPageViewModel,
                launchTrackedExport = { fileName -> tracksExportLauncher.launch(fileName) },
                launchTrackedImport = { mimeTypes -> tracksImportLauncher.launch(mimeTypes) },
                launchStarImport = { intent -> starImportLauncher.launch(intent) },
            )
        }
    LaunchedEffect(externalRefreshTriggerToken) {
        if (externalRefreshTriggerToken <= 0) return@LaunchedEffect
        actions.refreshAllTracked(showToast = true)
    }
    LaunchedEffect(
        externalNavigationTriggerToken,
        externalActionsSheetToken,
        externalActionsTrackId,
        contentDerivedState.trackedItemIdKey,
        runtime.contentReady,
        presentationOwnerActive,
    ) {
        if (shouldDismissGitHubActionsForNavigation(
                token = externalNavigationTriggerToken,
                consumedToken = consumedExternalNavigationToken,
            )
        ) {
            consumedExternalNavigationToken = externalNavigationTriggerToken
            actions.closeActionsSheet()
        }
        if (!canConsumeGitHubActionsOpenRequest(
                token = externalActionsSheetToken,
                consumedToken = consumedExternalActionsSheetToken,
                ownerActive = presentationOwnerActive,
                contentReady = runtime.contentReady,
                trackId = externalActionsTrackId,
            )
        ) {
            return@LaunchedEffect
        }
        val trackId = externalActionsTrackId.orEmpty().trim()
        if (trackId.isBlank()) return@LaunchedEffect
        val item = state.trackedItems.firstOrNull { it.id == trackId } ?: return@LaunchedEffect
        consumedExternalActionsSheetToken = externalActionsSheetToken
        state.requestTrackCardFocus(item.id)
        actions.openActionsSheet(item)
    }
    LaunchedEffect(actions, runtime.contentReady) {
        if (!runtime.contentReady) return@LaunchedEffect
        actions.syncActiveShareImportFlowFromStore()
    }
    BindGitHubAppIconPreloadEffect(
        active = isGitHubSettledDataActive,
        trackedPackages = contentDerivedState.trackedIconPreloadPackages,
        installedPackages = contentDerivedState.installedIconPreloadPackages,
        selectedPackageName = state.selectedApp?.packageName.orEmpty(),
        pickerExpanded = state.pickerExpanded,
        appPickerFilteredPackages = appPickerDerivedState.filteredIconPreloadPackages,
        requestAppIcons = { packageNames ->
            githubPageViewModel.requestAppIcons(packageNames)
        },
    )

    BindGitHubPageLifecycleCoordinator(
        context = context,
        listState = listState,
        scrollToTopSignal = runtime.scrollToTopSignal,
        isPageWarmActive = runtime.contentReady && runtime.isWarmDataActive,
        isPageDataActive = isGitHubPageDataActive,
        state = state,
        actions = actions,
        installedOnlineShareTargets = installedOnlineShareTargets,
        onLaunchAppListPermission = launchAppListPermission,
        onHistoryUnreadCountMayChange = githubPageViewModel::refreshHistoryUnreadCount,
    )

    BindGitHubTrackCardFocusCoordinator(
        listState = listState,
        request = state.trackCardFocusRequest,
        sortedTrackIds = contentDerivedState.sortedTrackIds,
        pendingTrackVisible =
            contentDerivedState.showPendingShareImportCard &&
                state.pendingShareImportTrack != null,
        attachCandidateVisible = state.pendingShareImportAttachCandidate != null,
        previewVisible =
            state.pendingShareImportTrack == null &&
                state.pendingShareImportAttachCandidate == null &&
                state.pendingShareImportPreview != null,
        resultVisible =
            state.pendingShareImportPreview == null &&
                state.pendingShareImportTrack == null &&
                state.pendingShareImportAttachCandidate == null &&
                state.pendingShareImportResult != null,
        onConsumed = state::consumeTrackCardFocus,
    )

    val githubGlassRuntime = LocalGlassEffectRuntime.current
    CompositionLocalProvider(
        LocalGlassEffectRuntime provides githubGlassRuntime,
        LocalGitHubAppIconBitmaps provides appIconState.bitmaps,
    ) {
        GitHubMainContent(
            layout =
                GitHubMainContentLayout(
                    contentBottomPadding = runtime.contentBottomPadding,
                    listState = listState,
                    secondaryListState = secondaryListState,
                    columnCount = githubColumnCount,
                    scrollBehavior = scrollBehavior,
                    bottomBarVisible = runtime.bottomBarVisible,
                    floatingDockSide = runtime.floatingDockSide,
                    onExpandFloatingDock = runtime.onShowBottomBar,
                    onTitleClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                ),
            surfaces =
                GitHubMainContentSurfaces(
                    topBarProducer = backdrops.topBarProducer,
                    topBarMaterial = backdrops.topBar,
                    contentBackdrop = backdrops.contentMaterial,
                    sheetProducer = backdrops.sheetProducer,
                    backdropProducerActive = backdrops.sheetProducer !== backdrops.contentProducer,
                    topBarColor = topBarColor,
                    isDark = isDark,
                ),
            controls =
                GitHubMainContentControls(
                    searchExpanded = enableSearchBar && chromeState.searchExpanded,
                    trackedSearch = state.trackedSearch,
                    sortMode = state.sortMode,
                    sortDirection = state.sortDirection,
                    trackedFilterMode = state.trackedFilterMode,
                    refreshIntervalHours = state.refreshIntervalHours,
                    historyUnreadCount = pageUiState.historyUnreadCount,
                    showActionMenuPopup = state.showActionMenuPopup,
                    deleteInProgress = state.deleteInProgress,
                    tracksExporting = transferState.tracksExporting,
                    tracksImporting = transferState.tracksImporting,
                ),
            overview =
                GitHubMainContentOverview(
                    refreshState = state.overviewRefreshState,
                    refreshProgress = state.refreshProgress,
                    lastRefreshMs = state.lastRefreshMs,
                    lookupConfig = state.lookupConfig,
                    metrics = contentDerivedState.trackedUi.overviewMetrics,
                ),
            tracked =
                GitHubMainContentTracked(
                    trackedItems = state.trackedItems,
                    filteredTracked = contentDerivedState.trackedUi.filteredTracked,
                    sortedTracked = contentDerivedState.trackedUi.sortedTracked,
                    appLastUpdatedAtByTrackId = contentDerivedState.appLastUpdatedAtByTrackId,
                    installedAppLabelsByPackage = contentDerivedState.installedAppLabelsByPackage,
                    checkStates = state.checkStates,
                    itemRefreshLoading = state.itemRefreshLoading,
                    apkAssetBundles = state.apkAssetBundles,
                    apkAssetLoading = state.apkAssetLoading,
                    apkAssetErrors = state.apkAssetErrors,
                    apkAssetExpanded = state.apkAssetExpanded,
                    apkInfoResults = state.apkInfoResults,
                    managedInstallLoading = state.managedInstallLoading,
                    actionsRecommendedRunSnapshots = state.actionsRecommendedRunSnapshots,
                    pinnedTrackIds = pinnedTrackIdSet,
                    laneDetainedTrackIds = laneDetainedTrackIdSet,
                    expansionState = trackedItemsExpansionState,
                    relativeTimeNowMillis = contentDerivedState.relativeTimeNowMillis,
                ),
            shareImport =
                GitHubMainContentShareImport(
                    pendingPreview = state.pendingShareImportPreview,
                    pendingTrack = state.pendingShareImportTrack,
                    pendingAttachCandidate = state.pendingShareImportAttachCandidate,
                    pendingResult = state.pendingShareImportResult,
                    showPendingCard = contentDerivedState.showPendingShareImportCard,
                    pendingRepoOverlapCount = contentDerivedState.pendingShareImportRepoOverlapCount,
                    pendingNowMillis = contentDerivedState.pendingShareImportNowMillis,
                ),
            actions =
                GitHubMainContentActions(
                    onTrackedSearchChange = actions::setTrackedSearch,
                    onSearchExpandedChange = { expanded ->
                        githubPageViewModel.updateSearchExpanded(enableSearchBar && expanded)
                    },
                    onShowActionMenuPopupChange = actions::setShowActionMenuPopup,
                    onSortModeChange = actions::setSortMode,
                    onSortDirectionChange = actions::setSortDirection,
                    onTrackedFilterModeChange = actions::setTrackedFilterMode,
                    onRefreshIntervalHoursChange = actions::selectRefreshIntervalHours,
                    onExportTrackedItems = transferCallbacks.onExportTrackedItems,
                    onImportTrackedItems = transferCallbacks.onImportTrackedItems,
                    onOpenStarImport = transferCallbacks.onOpenStarImport,
                    onOpenStrategySheet = actions::openStrategySheet,
                    onOpenCheckLogicSheet = actions::openCheckLogicSheet,
                    onOpenDroidSourcesSheet = actions::openDroidSourcesSheet,
                    onOpenDebugSheet = actions::openDebugSheet,
                    onRefreshInstalledApps = actions::refreshInstalledAppsManually,
                    onOpenActionsNotificationHistory = onOpenActionsNotificationHistory,
                    onLocalVersionExpandedChange = actions::setTrackedLocalVersionExpanded,
                    onStableVersionExpandedChange = actions::setTrackedStableVersionExpanded,
                    onPreReleaseVersionExpandedChange = actions::setTrackedPreReleaseVersionExpanded,
                    onRefreshVisibleTracked = {
                        actions.refreshVisibleTracked(
                            items = contentDerivedState.trackedUi.sortedTracked,
                            showToast = true,
                        )
                    },
                    onRetryFailedTracked = { actions.refreshFailedTrackedItems(showToast = true) },
                    onFailedFilterToggle = actions::setFailedFilterEnabled,
                    onRefreshTrackedItem = { actions.refreshTrackedItem(it, showToastOnError = true) },
                    onOpenActionsSheet = actions::openActionsSheet,
                    onOpenTrackSheetForAdd = actions::openTrackSheetForAdd,
                    onOpenTrackSheetForEdit = actions::openTrackSheetForEdit,
                    onIgnoreCurrentTrackedVersion = actions::ignoreCurrentTrackedVersion,
                    onRequestDeleteTrackedItem = actions::requestDeleteTrackedItem,
                    onOpenFdroidDetail = actions::openFdroidDetail,
                    onOpenReleaseList = { item -> onOpenReleaseList(item.id) },
                    onOpenFdroidVersionList = { item -> onOpenFdroidVersionList(item.id) },
                    onTrackedCardExpandedChange = onTrackedCardExpandedChange,
                    onToggleTrackedPinned = { item -> actions.toggleTrackedPinned(item.id) },
                    onCollapseTrackedCard = actions::collapseTrackedCard,
                    onCollapseApkAssetPanel = actions::collapseApkAssetPanel,
                    onLoadApkAssets = { item, itemState, toggleOnlyWhenCached, includeAllAssets, allowLatestReleaseFallback ->
                        actions.loadApkAssets(
                            item = item,
                            itemState = itemState,
                            toggleOnlyWhenCached = toggleOnlyWhenCached,
                            includeAllAssets = includeAllAssets,
                            allowLatestReleaseFallback = allowLatestReleaseFallback,
                        )
                    },
                    onOpenDecisionAssistDetail = actions::openDecisionAssistDetail,
                    onOpenExternalUrl = actions::openExternalUrl,
                    onOpenApkInfo = actions::openApkInfo,
                    onInstallApk = actions::installApkWithKeiOs,
                    onOpenApkInDownloader = actions::openApkInDownloader,
                    onShareApkLink = actions::shareApkLink,
                    onOpenShareImportFlow = actions::openShareImportFlow,
                    onOpenShareImportResult = actions::focusShareImportResult,
                    onCancelActiveShareImportFlow = actions::cancelActiveShareImportFlow,
                    onCancelPendingShareImportTrack = actions::cancelPendingShareImportTrack,
                    onDismissShareImportResult = actions::dismissShareImportResult,
                ),
        )
    }

    val apkInfoSheetState by githubPageViewModel.apkInfoSheetState.collectAsStateWithLifecycle()
    val actionsSheetState by githubPageViewModel.actionsSheetState.collectAsStateWithLifecycle()
    val releaseNotesDetailState by githubPageViewModel.releaseNotesDetailState.collectAsStateWithLifecycle()
    val managedInstallConfirmSheetState by githubPageViewModel.managedInstallConfirmSheetState.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalGlassEffectRuntime provides githubGlassRuntime,
        LocalGitHubAppIconBitmaps provides appIconState.bitmaps,
    ) {
        GitHubPageSheetHost(
            context = context,
            backdrops = backdrops,
            state = state,
            actions = actions,
            contentDerivedState = contentDerivedState,
            installedOnlineShareTargets = installedOnlineShareTargets,
            checkLogicDownloaderOptions = checkLogicDownloaderOptions,
            appPickerDerivedState = appPickerDerivedState,
            appPickerPreferences = pageUiState.appPickerPreferences,
            apkInfoSheetState = apkInfoSheetState,
            actionsSheetState = actionsSheetState,
            releaseNotesDetailState = releaseNotesDetailState,
            managedInstallConfirmSheetState = managedInstallConfirmSheetState,
            hasKeiOsSelfTrack = contentDerivedState.hasKeiOsSelfTrack,
            tracksExporting = transferState.tracksExporting,
            tracksImporting = transferState.tracksImporting,
            onEnsureKeiOsSelfTrack = actions::ensureKeiOsSelfTrack,
            onRequestAppIcons = githubPageViewModel::requestAppIcons,
            onRequestAppPickerState = githubPageViewModel::requestAppPickerState,
            onAppPickerPreferencesChange = githubPageViewModel::saveAppPickerPreferences,
            onRequestApkInfoSheetState = githubPageViewModel::requestApkInfoSheetState,
            onApkInfoSearchQueryChange = githubPageViewModel::updateApkInfoSheetQuery,
            onClearApkInfoSheetState = githubPageViewModel::clearApkInfoSheetState,
            onRequestReleaseNotesDetailState = githubPageViewModel::requestReleaseNotesDetailState,
            onClearReleaseNotesDetailState = githubPageViewModel::clearReleaseNotesDetailState,
            onRequestManagedInstallConfirmSheetState = githubPageViewModel::requestManagedInstallConfirmSheetState,
            onClearManagedInstallConfirmSheetState = githubPageViewModel::clearManagedInstallConfirmSheetState,
            onConfirmTrackImport = transferCallbacks.onConfirmTrackImport,
        )
    }
}

private data class GitHubListScrollSnapshot(
    val canScrollBackward: Boolean,
    val canScrollForward: Boolean,
)

@Composable
private fun BindGitHubTrackCardFocusCoordinator(
    listState: LazyListState,
    request: GitHubTrackCardFocusRequest?,
    sortedTrackIds: List<String>,
    pendingTrackVisible: Boolean,
    attachCandidateVisible: Boolean,
    previewVisible: Boolean,
    resultVisible: Boolean,
    onConsumed: (GitHubTrackCardFocusRequest) -> Unit,
) {
    LaunchedEffect(
        request?.version,
        sortedTrackIds,
        pendingTrackVisible,
        attachCandidateVisible,
        previewVisible,
        resultVisible,
    ) {
        val focusRequest = request ?: return@LaunchedEffect
        val leadingItemCount =
            githubTrackedListLeadingItemCount(
                pendingTrackVisible = pendingTrackVisible,
                attachCandidateVisible = attachCandidateVisible,
                previewVisible = previewVisible,
                resultVisible = resultVisible,
            )
        val targetIndex =
            githubTrackedLazyListIndex(
                targetTrackId = focusRequest.trackId,
                sortedTrackIds = sortedTrackIds,
                leadingItemCount = leadingItemCount,
            ) ?: return@LaunchedEffect
        listState.animateItemToViewportCenter(targetIndex)
        onConsumed(focusRequest)
    }
}

private suspend fun LazyListState.animateItemToViewportCenter(index: Int) {
    animateScrollToItem(index)
    withFrameNanos { }
    val layoutInfo = layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2
    val deltaPx = itemCenter - viewportCenter
    if (abs(deltaPx) > 2) {
        animateScrollBy(deltaPx.toFloat())
    }
}
