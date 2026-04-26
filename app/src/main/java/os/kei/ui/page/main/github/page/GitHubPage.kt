package os.kei.ui.page.main.github.page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import os.kei.R
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.host.pager.rememberMainPageBackdropSet
import os.kei.ui.page.main.github.query.systemDownloadManagerOption
import os.kei.ui.page.main.github.section.GitHubMainContent
import os.kei.ui.page.main.github.sheet.GitHubCheckLogicSheet
import os.kei.ui.page.main.github.sheet.GitHubDeleteTrackDialog
import os.kei.ui.page.main.github.sheet.GitHubStrategySheet
import os.kei.ui.page.main.github.sheet.GitHubTrackEditSheet
import os.kei.ui.page.main.github.sheet.GitHubTrackImportDialog
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import os.kei.ui.page.main.github.page.BindGitHubPageEffects
import os.kei.ui.page.main.github.page.GitHubPageActions
import os.kei.ui.page.main.github.page.rememberGitHubPageState
import os.kei.feature.github.model.isKeiOsSelfTrack
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.glass.rememberListScrollGlassRuntime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GitHubPage(
    runtime: MainPageRuntime = MainPageRuntime(contentBottomPadding = 72.dp),
    externalRefreshTriggerToken: Int = 0,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    enableSearchBar: Boolean = true,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val openLinkFailureMessage = context.getString(R.string.github_error_open_link)
    val systemDmOption = remember(context) { systemDownloadManagerOption(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = isSystemInDarkTheme()
    val githubPageViewModel: GitHubPageViewModel = viewModel()
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
    val backdrops = rememberMainPageBackdropSet(
        keyPrefix = "github",
        distinctLayers = fullBackdropEffectsEnabled
    )
    val topBarColor = rememberMiuixBlurBackdrop(
        enableBlur = topBarBackdropEffectsEnabled
    ).getMiuixAppBarColor()

    val state = rememberGitHubPageState(githubPageViewModel)
    val transferState by githubPageViewModel.transferState.collectAsState()
    val installedOnlineShareTargets by githubPageViewModel.installedOnlineShareTargets.collectAsState()
    val checkLogicDownloaderOptions by githubPageViewModel.checkLogicDownloaderOptions.collectAsState()
    val contentDerivedState by githubPageViewModel.contentDerivedState.collectAsState()
    LaunchedEffect(context, state) {
        githubPageViewModel.bindContextObservers(
            context = context,
            state = state
        )
    }
    SideEffect {
        state.updateScrollBounds(
            canScrollBackward = listState.canScrollBackward,
            canScrollForward = listState.canScrollForward
        )
    }
    val exportFileNameFormatter = remember {
        DateTimeFormatter.ofPattern("yyMMdd-HHmm", Locale.getDefault())
    }
    val actions = remember(
        context,
        scope,
        state,
        githubPageViewModel.repository,
        systemDmOption,
        openLinkFailureMessage
    ) {
        GitHubPageActions(
            context = context,
            scope = scope,
            state = state,
            repository = githubPageViewModel.repository,
            systemDmOption = systemDmOption,
            openLinkFailureMessage = openLinkFailureMessage
        )
    }
    LaunchedEffect(externalRefreshTriggerToken) {
        if (externalRefreshTriggerToken <= 0) return@LaunchedEffect
        actions.refreshAllTracked(showToast = true)
    }
    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { actions.reloadApps(forceRefresh = true) }
        }
    val tracksExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val request = githubPageViewModel.consumePendingExport()
        if (uri == null || request == null) {
            githubPageViewModel.finishTrackedExport()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                githubPageViewModel.writeExport(
                    contentResolver = context.contentResolver,
                    uri = uri,
                    request = request
                )
            }
            githubPageViewModel.finishTrackedExport()
            result.onSuccess {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_track_exported),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.github_toast_track_export_failed,
                        it.javaClass.simpleName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val tracksImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            githubPageViewModel.finishTrackedImport()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                val raw = githubPageViewModel.readImport(
                    contentResolver = context.contentResolver,
                    uri = uri
                )
                actions.previewTrackedItemsImport(raw)
            }
            githubPageViewModel.finishTrackedImport()
            result.onSuccess { preview ->
                state.pendingTrackImportPreview = preview
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.github_toast_track_import_failed,
                        it.javaClass.simpleName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    BindGitHubPageEffects(
        context = context,
        listState = listState,
        scrollToTopSignal = runtime.scrollToTopSignal,
        isPageWarmActive = runtime.isPageActive,
        isPageDataActive = runtime.isDataActive,
        state = state,
        actions = actions,
        installedOnlineShareTargets = installedOnlineShareTargets,
        onLaunchAppListPermission = { intent -> appListPermissionLauncher.launch(intent) },
        onActionBarInteractingChanged = onActionBarInteractingChanged
    )

    val hasKeiOsSelfTrack by remember {
        derivedStateOf { state.trackedItems.any { it.isKeiOsSelfTrack() } }
    }
    val githubGlassRuntime = rememberListScrollGlassRuntime(
        isListScrolling = isListScrolling,
        label = "githubListGlassEffectProgress"
    )
    CompositionLocalProvider(LocalGlassEffectRuntime provides githubGlassRuntime) {
        GitHubMainContent(
            contentBottomPadding = runtime.contentBottomPadding,
            listState = listState,
            scrollBehavior = scrollBehavior,
            addButtonScrollConnection = state.addButtonScrollConnection,
            topBarBackdrop = backdrops.topBar,
            contentBackdrop = backdrops.content,
            topBarColor = topBarColor,
            enableSearchBar = enableSearchBar,
            liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
            reduceEffectsDuringPagerScroll = runtime.isPagerScrollInProgress,
            reduceEffectsDuringListScroll = isListScrolling,
            showSearchBar = state.showSearchBar,
            trackedSearch = state.trackedSearch,
            sortMode = state.sortMode,
            showSortPopup = state.showSortPopup,
            showFloatingAddButton = state.showFloatingAddButton,
            deleteInProgress = state.deleteInProgress,
            isDark = isDark,
            overviewRefreshState = state.overviewRefreshState,
            refreshProgress = state.refreshProgress,
            lastRefreshMs = state.lastRefreshMs,
            lookupConfig = state.lookupConfig,
            overviewMetrics = contentDerivedState.trackedUi.overviewMetrics,
            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
            trackedItems = state.trackedItems,
            filteredTracked = contentDerivedState.trackedUi.filteredTracked,
            sortedTracked = contentDerivedState.trackedUi.sortedTracked,
            appLastUpdatedAtByTrackId = contentDerivedState.appLastUpdatedAtByTrackId,
            checkStates = state.checkStates,
            itemRefreshLoading = state.itemRefreshLoading,
            apkAssetBundles = state.apkAssetBundles,
            apkAssetLoading = state.apkAssetLoading,
            apkAssetErrors = state.apkAssetErrors,
            apkAssetExpanded = state.apkAssetExpanded,
            trackedCardExpanded = state.trackedCardExpanded,
            pendingShareImportTrack = state.pendingShareImportTrack,
            showPendingShareImportCard = contentDerivedState.showPendingShareImportCard,
            pendingShareImportRepoOverlapCount = contentDerivedState.pendingShareImportRepoOverlapCount,
            onTrackedSearchChange = { state.trackedSearch = it },
            onShowSortPopupChange = { state.showSortPopup = it },
            onSortModeChange = { state.sortMode = it },
            onOpenStrategySheet = actions::openStrategySheet,
            onOpenCheckLogicSheet = actions::openCheckLogicSheet,
            onRefreshAllTracked = { actions.refreshAllTracked(showToast = true) },
            onRefreshTrackedItem = { actions.refreshTrackedItem(it, showToastOnError = true) },
            onOpenTrackSheetForAdd = actions::openTrackSheetForAdd,
            onOpenTrackSheetForEdit = actions::openTrackSheetForEdit,
            onClearApkAssetUiState = actions::clearApkAssetUiState,
            onCollapseApkAssetPanel = { item, itemState ->
                actions.clearApkAssetUiState(item.id)
                actions.clearApkAssetCache(item, itemState)
            },
            onLoadApkAssets = { item, itemState, toggleOnlyWhenCached, includeAllAssets ->
                actions.loadApkAssets(
                    item = item,
                    itemState = itemState,
                    toggleOnlyWhenCached = toggleOnlyWhenCached,
                    includeAllAssets = includeAllAssets
                )
            },
            onOpenExternalUrl = actions::openExternalUrl,
            onOpenApkInDownloader = actions::openApkInDownloader,
            onShareApkLink = actions::shareApkLink,
            onCancelPendingShareImportTrack = actions::cancelPendingShareImportTrack,
            onActionBarInteractingChanged = onActionBarInteractingChanged
        )
    }

    val onExportTrackedItems: () -> Unit = {
        scope.launch {
            val exportedAtMillis = System.currentTimeMillis()
            val exportFileName = buildString {
                append("keios-github-tracks-")
                append(LocalDateTime.now().format(exportFileNameFormatter))
                append(".json")
            }
            when (
                val result = githubPageViewModel.beginTrackedExport(
                    items = state.trackedItems.toList(),
                    exportedAtMillis = exportedAtMillis,
                    fileName = exportFileName
                )
            ) {
                GitHubTrackedExportStartResult.Busy -> Unit
                GitHubTrackedExportStartResult.Empty -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.github_toast_require_track_item),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is GitHubTrackedExportStartResult.Failed -> {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.github_toast_track_export_failed,
                            result.reason ?: result.javaClass.simpleName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                GitHubTrackedExportStartResult.Ready -> {
                    runCatching {
                        tracksExportLauncher.launch(exportFileName)
                    }.onFailure {
                        githubPageViewModel.finishTrackedExport()
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.github_toast_track_export_failed,
                                it.javaClass.simpleName
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    val onImportTrackedItems: () -> Unit = {
        when (githubPageViewModel.beginTrackedImport()) {
            GitHubTrackedImportStartResult.Busy -> Unit
            GitHubTrackedImportStartResult.Ready -> {
                runCatching {
                    tracksImportLauncher.launch(arrayOf("application/json", "text/plain"))
                }.onFailure {
                    githubPageViewModel.finishTrackedImport()
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.github_toast_track_import_failed,
                            it.javaClass.simpleName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val onConfirmTrackImport: () -> Unit = {
        val preview = state.pendingTrackImportPreview
        if (preview != null) {
            when (githubPageViewModel.beginTrackedImport()) {
                GitHubTrackedImportStartResult.Busy -> Unit
                GitHubTrackedImportStartResult.Ready -> {
                    scope.launch {
                        val result = runCatching { actions.applyTrackedItemsImport(preview) }
                        githubPageViewModel.finishTrackedImport()
                        result.onSuccess { importResult ->
                            state.dismissTrackImportPreview()
                            val effectiveCount = importResult.addedCount +
                                importResult.updatedCount +
                                importResult.unchangedCount
                            if (effectiveCount == 0) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.github_toast_track_import_no_valid),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.github_toast_track_imported_summary,
                                        importResult.addedCount,
                                        importResult.updatedCount,
                                        importResult.unchangedCount,
                                        importResult.invalidCount + importResult.duplicateCount
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.onFailure {
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.github_toast_track_import_failed,
                                    it.javaClass.simpleName
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalGlassEffectRuntime provides githubGlassRuntime) {
        GitHubPageSheetHost(
            context = context,
            backdrops = backdrops,
            state = state,
            actions = actions,
            contentDerivedState = contentDerivedState,
            installedOnlineShareTargets = installedOnlineShareTargets,
            checkLogicDownloaderOptions = checkLogicDownloaderOptions,
            hasKeiOsSelfTrack = hasKeiOsSelfTrack,
            tracksExporting = transferState.tracksExporting,
            tracksImporting = transferState.tracksImporting,
            onEnsureKeiOsSelfTrack = actions::ensureKeiOsSelfTrack,
            onExportTrackedItems = onExportTrackedItems,
            onImportTrackedItems = onImportTrackedItems,
            onConfirmTrackImport = onConfirmTrackImport
        )
    }

}
