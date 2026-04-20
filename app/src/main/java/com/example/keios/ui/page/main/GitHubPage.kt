package com.example.keios.ui.page.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.keios.R
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.github.GitHubSortMode
import com.example.keios.ui.page.main.github.page.BindGitHubPageEffects
import com.example.keios.ui.page.main.github.page.GitHubPageActions
import com.example.keios.ui.page.main.github.page.GitHubPageDerivedState
import com.example.keios.ui.page.main.github.page.rememberGitHubPageState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val pendingShareImportCardVisibleWindowMs = 90_000L
private const val pendingShareImportCardTickMs = 15_000L

@Composable
fun GitHubPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    isPageActive: Boolean = true,
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
    val surfaceColor = MiuixTheme.colorScheme.surface

    val topBarBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val contentBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val sheetBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
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
        scrollToTopSignal = scrollToTopSignal,
        isPageActive = isPageActive,
        state = state,
        actions = actions,
        installedOnlineShareTargets = installedOnlineShareTargets,
        onLaunchAppListPermission = { intent -> appListPermissionLauncher.launch(intent) },
        onActionBarInteractingChanged = onActionBarInteractingChanged
    )

    val trackedUi by remember(state) {
        derivedStateOf {
            val filteredTracked = state.trackedItems.filter { item ->
                state.trackedSearch.isBlank() ||
                    item.owner.contains(state.trackedSearch, ignoreCase = true) ||
                    item.repo.contains(state.trackedSearch, ignoreCase = true) ||
                    item.appLabel.contains(state.trackedSearch, ignoreCase = true) ||
                    item.packageName.contains(state.trackedSearch, ignoreCase = true)
            }
            val isSortUpdatable: (com.example.keios.feature.github.model.GitHubTrackedApp) -> Boolean = { item ->
                item.alwaysShowLatestReleaseDownloadButton || state.checkStates[item.id]?.hasUpdate == true
            }
            val sortedTracked = when (state.sortMode) {
                GitHubSortMode.UpdateFirst -> filteredTracked.sortedWith(
                    compareByDescending<com.example.keios.feature.github.model.GitHubTrackedApp> {
                        isSortUpdatable(it)
                    }
                        .thenByDescending { state.checkStates[it.id]?.hasPreReleaseUpdate == true }
                        .thenBy { it.appLabel.lowercase() }
                )
                GitHubSortMode.NameAsc -> filteredTracked.sortedBy { it.appLabel.lowercase() }
                GitHubSortMode.PreReleaseFirst -> filteredTracked.sortedWith(
                    compareByDescending<com.example.keios.feature.github.model.GitHubTrackedApp> {
                        state.checkStates[it.id]?.isPreRelease == true
                    }
                        .thenByDescending { isSortUpdatable(it) }
                        .thenBy { it.appLabel.lowercase() }
                )
            }
            val trackedCount = state.trackedItems.size
            val updatableCount = state.trackedItems.count { state.checkStates[it.id]?.hasUpdate == true }
            val preReleaseCount = state.trackedItems.count { state.checkStates[it.id]?.isPreRelease == true }
            val preReleaseUpdateCount =
                state.trackedItems.count { state.checkStates[it.id]?.hasPreReleaseUpdate == true }
            val failedCount = state.trackedItems.count { state.checkStates[it.id]?.failed == true }
            val stableLatestCount = state.trackedItems.count {
                val itemState = state.checkStates[it.id]
                itemState?.hasUpdate == false && itemState.isPreRelease.not()
            }
            GitHubPageDerivedState(
                filteredTracked = filteredTracked,
                sortedTracked = sortedTracked,
                overviewMetrics = GitHubOverviewMetrics(
                    trackedCount = trackedCount,
                    updatableCount = updatableCount,
                    stableLatestCount = stableLatestCount,
                    preReleaseCount = preReleaseCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    failedCount = failedCount
                )
            )
        }
    }
    val appLastUpdatedAtByTrackId by remember {
        derivedStateOf {
            buildMap<String, Long> {
                val appUpdatedAtByPackage = buildMap<String, Long> {
                    state.trackedFirstInstallAtByPackage.forEach { (packageName, firstInstallAtMillis) ->
                        if (packageName.isNotBlank() && firstInstallAtMillis > 0L) {
                            put(packageName, firstInstallAtMillis)
                        }
                    }
                    state.appList
                        .filter { it.packageName.isNotBlank() && it.lastUpdateTimeMs > 0L }
                        .forEach { put(it.packageName, it.lastUpdateTimeMs) }
                }
                state.trackedItems.forEach { item ->
                    val byPackage = appUpdatedAtByPackage[item.packageName]
                    val byTrackId = state.trackedAddedAtById[item.id]
                    val updatedAt = byPackage?.takeIf { it > 0L } ?: byTrackId?.takeIf { it > 0L }
                    if (updatedAt != null) {
                        put(item.id, updatedAt)
                    }
                }
            }
        }
    }
    val checkLogicDownloaderOptions = remember(state.showCheckLogicSheet) {
        if (state.showCheckLogicSheet) {
            queryDownloaderOptions(context)
        } else {
            emptyList()
        }
    }
    val pendingShareImportRepoOverlapCount by remember(
        state.pendingShareImportTrack,
        state.trackedItems
    ) {
        derivedStateOf {
            val pending = state.pendingShareImportTrack ?: return@derivedStateOf 0
            state.trackedItems.count { item ->
                item.owner.equals(pending.owner, ignoreCase = true) &&
                    item.repo.equals(pending.repo, ignoreCase = true)
            }
        }
    }
    val pendingShareImportCardNowMillis by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = state.pendingShareImportTrack?.armedAtMillis
    ) {
        value = System.currentTimeMillis()
        if (state.pendingShareImportTrack == null) return@produceState
        while (true) {
            delay(pendingShareImportCardTickMs)
            value = System.currentTimeMillis()
        }
    }
    val showPendingShareImportCard by remember(
        state.pendingShareImportTrack,
        pendingShareImportRepoOverlapCount,
        pendingShareImportCardNowMillis
    ) {
        derivedStateOf {
            val pending = state.pendingShareImportTrack ?: return@derivedStateOf false
            val ageMs = (pendingShareImportCardNowMillis - pending.armedAtMillis).coerceAtLeast(0L)
            val withinVisibleWindow = ageMs <= pendingShareImportCardVisibleWindowMs
            withinVisibleWindow || pendingShareImportRepoOverlapCount > 0
        }
    }
    GitHubMainContent(
        contentBottomPadding = contentBottomPadding,
        listState = listState,
        scrollBehavior = scrollBehavior,
        addButtonScrollConnection = state.addButtonScrollConnection,
        topBarBackdrop = topBarBackdrop,
        contentBackdrop = contentBackdrop,
        topBarColor = topBarColor,
        enableSearchBar = enableSearchBar,
        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
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
        overviewMetrics = trackedUi.overviewMetrics,
        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
        trackedItems = state.trackedItems,
        filteredTracked = trackedUi.filteredTracked,
        sortedTracked = trackedUi.sortedTracked,
        appLastUpdatedAtByTrackId = appLastUpdatedAtByTrackId,
        checkStates = state.checkStates,
        itemRefreshLoading = state.itemRefreshLoading,
        apkAssetBundles = state.apkAssetBundles,
        apkAssetLoading = state.apkAssetLoading,
        apkAssetErrors = state.apkAssetErrors,
        apkAssetExpanded = state.apkAssetExpanded,
        trackedCardExpanded = state.trackedCardExpanded,
        pendingShareImportTrack = state.pendingShareImportTrack,
        showPendingShareImportCard = showPendingShareImportCard,
        pendingShareImportRepoOverlapCount = pendingShareImportRepoOverlapCount,
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

    GitHubStrategySheet(
        show = state.showStrategySheet,
        backdrop = sheetBackdrop,
        lookupConfig = state.lookupConfig,
        selectedStrategyInput = state.selectedStrategyInput,
        githubApiTokenInput = state.githubApiTokenInput,
        showApiTokenPlainText = state.showApiTokenPlainText,
        credentialCheckRunning = state.credentialCheckRunning,
        credentialCheckError = state.credentialCheckError,
        credentialCheckStatus = state.credentialCheckStatus,
        strategyBenchmarkRunning = state.strategyBenchmarkRunning,
        strategyBenchmarkError = state.strategyBenchmarkError,
        strategyBenchmarkReport = state.strategyBenchmarkReport,
        trackedCount = trackedUi.overviewMetrics.trackedCount,
        recommendedTokenGuideExpanded = state.recommendedTokenGuideExpanded,
        onDismissRequest = actions::closeStrategySheet,
        onApply = actions::applyLookupConfig,
        onSelectedStrategyChange = { state.selectedStrategyInput = it },
        onTokenInputChange = {
            state.githubApiTokenInput = it
            state.credentialCheckError = null
            state.credentialCheckStatus = null
        },
        onToggleTokenVisibility = { state.showApiTokenPlainText = !state.showApiTokenPlainText },
        onRunCredentialCheck = actions::runCredentialCheck,
        onRunStrategyBenchmark = actions::runStrategyBenchmark,
        onRecommendedTokenGuideExpandedChange = { state.recommendedTokenGuideExpanded = it },
        onOpenExternalUrl = { url, failureMessage ->
            actions.openExternalUrl(url = url, failureMessage = failureMessage)
        }
    )

    GitHubCheckLogicSheet(
        show = state.showCheckLogicSheet,
        backdrop = sheetBackdrop,
        lookupConfig = state.lookupConfig,
        trackedCount = trackedUi.overviewMetrics.trackedCount,
        refreshIntervalHours = state.refreshIntervalHours,
        refreshIntervalHoursInput = state.refreshIntervalHoursInput,
        checkAllTrackedPreReleasesInput = state.checkAllTrackedPreReleasesInput,
        aggressiveApkFilteringInput = state.aggressiveApkFilteringInput,
        shareImportLinkageEnabledInput = state.shareImportLinkageEnabledInput,
        onlineShareTargetPackageInput = state.onlineShareTargetPackageInput,
        preferredDownloaderPackageInput = state.preferredDownloaderPackageInput,
        installedOnlineShareTargets = installedOnlineShareTargets,
        showCheckLogicIntervalPopup = state.showCheckLogicIntervalPopup,
        showDownloaderPopup = state.showDownloaderPopup,
        showOnlineShareTargetPopup = state.showOnlineShareTargetPopup,
        checkLogicIntervalPopupAnchorBounds = state.checkLogicIntervalPopupAnchorBounds,
        downloaderPopupAnchorBounds = state.downloaderPopupAnchorBounds,
        onlineShareTargetPopupAnchorBounds = state.onlineShareTargetPopupAnchorBounds,
        downloaderOptions = checkLogicDownloaderOptions,
        exportInProgress = tracksExporting,
        importInProgress = tracksImporting,
        onDismissRequest = actions::closeCheckLogicSheet,
        onApply = { actions.applyCheckLogicSheet(installedOnlineShareTargets) },
        onExportTrackedItems = {
            if (tracksExporting || tracksImporting) return@GitHubCheckLogicSheet
            if (state.trackedItems.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_require_track_item),
                    Toast.LENGTH_SHORT
                ).show()
                return@GitHubCheckLogicSheet
            }
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
        },
        onImportTrackedItems = {
            if (tracksExporting || tracksImporting) return@GitHubCheckLogicSheet
            tracksImporting = true
            tracksImportLauncher.launch(arrayOf("application/json", "text/plain"))
        },
        onRefreshIntervalHoursInputChange = { state.refreshIntervalHoursInput = it },
        onCheckAllTrackedPreReleasesInputChange = { state.checkAllTrackedPreReleasesInput = it },
        onAggressiveApkFilteringInputChange = { state.aggressiveApkFilteringInput = it },
        onShareImportLinkageEnabledInputChange = { state.shareImportLinkageEnabledInput = it },
        onPreferredDownloaderPackageInputChange = { state.preferredDownloaderPackageInput = it },
        onOnlineShareTargetPackageInputChange = { state.onlineShareTargetPackageInput = it },
        onShowCheckLogicIntervalPopupChange = { state.showCheckLogicIntervalPopup = it },
        onShowDownloaderPopupChange = { state.showDownloaderPopup = it },
        onShowOnlineShareTargetPopupChange = { state.showOnlineShareTargetPopup = it },
        onCheckLogicIntervalPopupAnchorBoundsChange = {
            state.checkLogicIntervalPopupAnchorBounds = it
        },
        onDownloaderPopupAnchorBoundsChange = { state.downloaderPopupAnchorBounds = it },
        onOnlineShareTargetPopupAnchorBoundsChange = {
            state.onlineShareTargetPopupAnchorBounds = it
        }
    )

    GitHubTrackEditSheet(
        show = state.showAddSheet,
        backdrop = sheetBackdrop,
        editingTrackedItem = state.editingTrackedItem,
        repoUrlInput = state.repoUrlInput,
        appSearch = state.appSearch,
        packageNameInput = state.packageNameInput,
        pickerExpanded = state.pickerExpanded,
        selectedApp = state.selectedApp,
        appList = state.appList,
        preferPreReleaseInput = state.preferPreReleaseInput,
        alwaysShowLatestReleaseDownloadButtonInput = state.alwaysShowLatestReleaseDownloadButtonInput,
        onDismissRequest = actions::dismissTrackSheet,
        onApply = actions::applyTrackSheet,
        onRepoUrlInputChange = { state.repoUrlInput = it },
        onAppSearchChange = { state.appSearch = it },
        onPackageNameInputChange = { input ->
            state.packageNameInput = input
            val selected = state.selectedApp
            val normalizedInput = input.trim()
            if (selected != null) {
                if (normalizedInput.isBlank()) {
                    state.selectedApp = null
                } else if (!selected.packageName.equals(normalizedInput, ignoreCase = true)) {
                    state.selectedApp = null
                }
            }
        },
        onPickerExpandedChange = { state.pickerExpanded = it },
        onSelectedAppChange = { app ->
            state.selectedApp = app
            if (app != null) {
                state.packageNameInput = app.packageName
            }
        },
        onPreferPreReleaseInputChange = { state.preferPreReleaseInput = it },
        onAlwaysShowLatestReleaseDownloadButtonInputChange = {
            state.alwaysShowLatestReleaseDownloadButtonInput = it
        },
        onRequestDelete = actions::requestDeleteEditingItem
    )

    GitHubDeleteTrackDialog(
        pendingDeleteItem = state.pendingDeleteItem,
        deleteInProgress = state.deleteInProgress,
        onDismissRequest = { state.pendingDeleteItem = null },
        onCancel = {
            if (!state.deleteInProgress) {
                state.pendingDeleteItem = null
            }
        },
        onConfirmDelete = actions::confirmDeletePendingItem
    )

    GitHubTrackImportDialog(
        preview = state.pendingTrackImportPreview,
        importInProgress = tracksImporting,
        onDismissRequest = {
            if (!tracksImporting) {
                state.dismissTrackImportPreview()
            }
        },
        onCancel = {
            if (!tracksImporting) {
                state.dismissTrackImportPreview()
            }
        },
        onConfirmImport = {
            val preview = state.pendingTrackImportPreview ?: return@GitHubTrackImportDialog
            if (tracksImporting) return@GitHubTrackImportDialog
            if (!preview.canImport) {
                state.dismissTrackImportPreview()
                return@GitHubTrackImportDialog
            }
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
    )

}
