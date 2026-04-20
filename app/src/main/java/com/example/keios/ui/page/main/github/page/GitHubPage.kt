package com.example.keios.ui.page.main.github.page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.keios.R
import com.example.keios.ui.page.main.host.pager.MainPageRuntime
import com.example.keios.ui.page.main.host.pager.rememberMainPageBackdropSet
import com.example.keios.ui.page.main.github.query.queryDownloaderOptions
import com.example.keios.ui.page.main.github.query.queryOnlineShareTargetOptions
import com.example.keios.ui.page.main.github.query.systemDownloadManagerOption
import com.example.keios.ui.page.main.github.section.GitHubMainContent
import com.example.keios.ui.page.main.github.section.GitHubOverviewMetrics
import com.example.keios.ui.page.main.github.sheet.GitHubCheckLogicSheet
import com.example.keios.ui.page.main.github.sheet.GitHubDeleteTrackDialog
import com.example.keios.ui.page.main.github.sheet.GitHubStrategySheet
import com.example.keios.ui.page.main.github.sheet.GitHubTrackEditSheet
import com.example.keios.ui.page.main.github.sheet.GitHubTrackImportDialog
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.github.page.BindGitHubPageEffects
import com.example.keios.ui.page.main.github.page.GitHubPageActions
import com.example.keios.ui.page.main.github.page.rememberGitHubPageState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val backdrops = rememberMainPageBackdropSet(keyPrefix = "github")
    val topBarColor = rememberMiuixBlurBackdrop(enableBlur = true).getMiuixAppBarColor()

    val state = rememberGitHubPageState()
    var pendingTrackedExportContent by remember { mutableStateOf<String?>(null) }
    var pendingTrackedExportFileName by remember { mutableStateOf<String?>(null) }
    var tracksExporting by remember { mutableStateOf(false) }
    var tracksImporting by remember { mutableStateOf(false) }
    val exportFileNameFormatter = remember {
        DateTimeFormatter.ofPattern("yyMMdd-HHmm", Locale.getDefault())
    }
    val actions = remember(
        context,
        scope,
        state,
        systemDmOption,
        openLinkFailureMessage
    ) {
        GitHubPageActions(
            context = context,
            scope = scope,
            state = state,
            systemDmOption = systemDmOption,
            openLinkFailureMessage = openLinkFailureMessage
        )
    }
    androidx.compose.runtime.LaunchedEffect(externalRefreshTriggerToken) {
        if (externalRefreshTriggerToken <= 0) return@LaunchedEffect
        actions.refreshAllTracked(showToast = true)
    }
    val shouldResolveOnlineShareTargets by remember(state) {
        derivedStateOf {
            state.showCheckLogicSheet ||
                state.lookupConfig.onlineShareTargetPackage.isNotBlank() ||
                state.onlineShareTargetPackageInput.isNotBlank()
        }
    }
    val installedOnlineShareTargets = remember(
        shouldResolveOnlineShareTargets,
        state.appListLoaded,
        state.appList
    ) {
        if (shouldResolveOnlineShareTargets) {
            queryOnlineShareTargetOptions(context, state.appList)
        } else {
            emptyList()
        }
    }
    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { actions.reloadApps(forceRefresh = true) }
        }
    val tracksExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val exportContent = pendingTrackedExportContent
        pendingTrackedExportContent = null
        pendingTrackedExportFileName = null
        if (uri == null || exportContent.isNullOrBlank()) {
            tracksExporting = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                        checkNotNull(writer) { "openOutputStream returned null" }
                        writer.write(exportContent)
                    }
                }
            }
            tracksExporting = false
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
            tracksImporting = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                        checkNotNull(reader) { "openInputStream returned null" }
                        reader.readText()
                    }
                }
                actions.previewTrackedItemsImport(raw)
            }
            tracksImporting = false
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
        isPageActive = runtime.isPageActive,
        state = state,
        actions = actions,
        installedOnlineShareTargets = installedOnlineShareTargets,
        onLaunchAppListPermission = { intent -> appListPermissionLauncher.launch(intent) },
        onActionBarInteractingChanged = onActionBarInteractingChanged
    )

    val contentDerivedState = rememberGitHubPageContentDerivedState(state = state)
    val checkLogicDownloaderOptions = remember(state.showCheckLogicSheet) {
        if (state.showCheckLogicSheet) {
            queryDownloaderOptions(context)
        } else {
            emptyList()
        }
    }
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

    val onExportTrackedItems = {
        if (!(tracksExporting || tracksImporting)) {
            if (state.trackedItems.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_require_track_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                pendingTrackedExportContent = actions.buildTrackedItemsExportJson(
                    exportedAtMillis = System.currentTimeMillis()
                )
                val exportFileName = buildString {
                    append("keios-github-tracks-")
                    append(LocalDateTime.now().format(exportFileNameFormatter))
                    append(".json")
                }
                pendingTrackedExportFileName = exportFileName
                tracksExporting = true
                tracksExportLauncher.launch(exportFileName)
            }
        }
    }

    val onImportTrackedItems = {
        if (!(tracksExporting || tracksImporting)) {
            tracksImporting = true
            tracksImportLauncher.launch(arrayOf("application/json", "text/plain"))
        }
    }

    val onConfirmTrackImport = {
        val preview = state.pendingTrackImportPreview
        if (preview != null) {
            tracksImporting = true
            scope.launch {
                val result = runCatching { actions.applyTrackedItemsImport(preview) }
                tracksImporting = false
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

    GitHubPageSheetHost(
        context = context,
        backdrops = backdrops,
        state = state,
        actions = actions,
        contentDerivedState = contentDerivedState,
        installedOnlineShareTargets = installedOnlineShareTargets,
        checkLogicDownloaderOptions = checkLogicDownloaderOptions,
        tracksExporting = tracksExporting,
        tracksImporting = tracksImporting,
        onExportTrackedItems = onExportTrackedItems,
        onImportTrackedItems = onImportTrackedItems,
        onConfirmTrackImport = onConfirmTrackImport
    )

}
