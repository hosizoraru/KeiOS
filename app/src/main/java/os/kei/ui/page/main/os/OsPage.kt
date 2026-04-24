package os.kei.ui.page.main.os

import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import os.kei.R
import os.kei.core.system.ShizukuApiUtils
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import androidx.lifecycle.viewmodel.compose.viewModel
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.host.pager.rememberMainPageBackdropSet
import os.kei.ui.page.main.os.components.OsPageMainList
import os.kei.ui.page.main.os.components.OsPageOverlayCoordinator
import os.kei.ui.page.main.os.state.createOsPageActionState
import os.kei.ui.page.main.os.state.rememberOsPageCardTransferState
import os.kei.ui.page.main.os.state.rememberOsPageOverlayState
import os.kei.ui.page.main.os.state.rememberOsPageOverlayTransferActions
import os.kei.ui.page.main.os.state.rememberOsPageSectionStateStore
import os.kei.ui.page.main.os.state.rememberOsPageTextBundle
import os.kei.ui.page.main.os.state.rememberOsPageUiContext
import os.kei.ui.page.main.os.shell.OsShellCommandCardStore
import os.kei.ui.page.main.os.shell.OsShellRunnerActivity
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import os.kei.ui.page.main.os.shortcut.createDefaultActivityShortcutDraft
import os.kei.ui.page.main.widget.chrome.ScrollChromeVisibilityController
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OsPage(
    runtime: MainPageRuntime,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    enableSearchBar: Boolean = true,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val uiContext = rememberOsPageUiContext(
        enableFullBackdropEffects = runtime.isPageActive &&
            !runtime.isPagerScrollInProgress &&
            !listState.isScrollInProgress
    )
    val context = uiContext.context
    val density = uiContext.density
    val scope = uiContext.scope
    val textBundle = uiContext.textBundle
    val isDark = uiContext.isDark
    val inactive = uiContext.inactiveColor
    val titleColor = uiContext.titleColor
    val cachedColor = uiContext.cachedColor
    val refreshingColor = uiContext.refreshingColor
    val syncedColor = uiContext.syncedColor
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    val initialUiSnapshot = remember { OsUiStateStore.loadSnapshot() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var cacheLoaded by remember { mutableStateOf(false) }
    var cachePersisted by remember { mutableStateOf(false) }
    val osPageViewModel: OsPageViewModel = viewModel()
    val queryInput by osPageViewModel.queryInput.collectAsState()
    val queryApplied by osPageViewModel.queryApplied.collectAsState()
    var topInfoExpanded by remember { mutableStateOf(initialUiSnapshot.topInfoExpanded) }
    var shellRunnerExpanded by remember { mutableStateOf(initialUiSnapshot.shellRunnerExpanded) }
    var systemTableExpanded by remember { mutableStateOf(initialUiSnapshot.systemTableExpanded) }
    var secureTableExpanded by remember { mutableStateOf(initialUiSnapshot.secureTableExpanded) }
    var globalTableExpanded by remember { mutableStateOf(initialUiSnapshot.globalTableExpanded) }
    var androidPropsExpanded by remember { mutableStateOf(initialUiSnapshot.androidPropsExpanded) }
    var javaPropsExpanded by remember { mutableStateOf(initialUiSnapshot.javaPropsExpanded) }
    var linuxEnvExpanded by remember { mutableStateOf(initialUiSnapshot.linuxEnvExpanded) }
    var visibleCards by remember { mutableStateOf(initialUiSnapshot.visibleCards) }
    var activityShortcutCards by remember {
        mutableStateOf(
            OsActivityShortcutCardStore.loadCards(
                defaults = textBundle.googleSystemServiceDefaults,
                builtInSampleDefaults = textBundle.googleSettingsBuiltInSampleDefaults
            )
        )
    }
    val activityCardExpanded = remember { mutableStateMapOf<String, Boolean>() }
    val overlayState = rememberOsPageOverlayState(textBundle.googleSystemServiceDefaults)
    var uiStatePersistenceReady by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()
    var refreshing by remember { mutableStateOf(false) }
    var refreshProgress by remember { mutableStateOf(0f) }
    var shellCommandCards by remember { mutableStateOf(OsShellCommandCardStore.loadCards()) }
    val shellCommandCardExpanded = remember { mutableStateMapOf<String, Boolean>() }
    var runningShellCommandCardIds by remember { mutableStateOf(emptySet<String>()) }
    val sectionLoadMutex = remember { Mutex() }
    val sectionLoadDeferreds = remember { mutableStateMapOf<SectionKind, Deferred<List<InfoRow>>>() }
    var showSearchBar by remember { mutableStateOf(true) }
    val surfaceColor = uiContext.surfaceColor
    val backdrops = uiContext.backdrops
    val topBarMaterialBackdrop = uiContext.topBarMaterialBackdrop
    val searchBarHideThresholdPx = uiContext.searchBarHideThresholdPx
    val searchBarVisibilityController = remember(searchBarHideThresholdPx) {
        ScrollChromeVisibilityController(searchBarHideThresholdPx)
    }
    val searchBarScrollConnection = remember(searchBarVisibilityController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                searchBarVisibilityController.update(available.y, showSearchBar) { showSearchBar = it }
                return Offset.Zero
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    BindOsShellCardReloadOnResume(
        lifecycleOwner = lifecycleOwner,
        reloadCards = { shellCommandCards = OsShellCommandCardStore.loadCards() }
    )
    val cardTransferState = rememberOsPageCardTransferState(
        context = context,
        scope = scope,
        overlayState = overlayState,
        activityShortcutCards = activityShortcutCards,
        onActivityShortcutCardsChange = { activityShortcutCards = it },
        activityCardExpanded = activityCardExpanded,
        shellCommandCards = shellCommandCards,
        onShellCommandCardsChange = { shellCommandCards = it },
        shellCommandCardExpanded = shellCommandCardExpanded,
        googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
        googleSettingsBuiltInSampleDefaults = textBundle.googleSettingsBuiltInSampleDefaults,
        cardImportFailedWithReason = textBundle.cardImportFailedWithReason,
        exportSuccessText = textBundle.exportSuccessText
    )
    val overlayTransferActions = rememberOsPageOverlayTransferActions(
        context = context,
        overlayState = overlayState,
        cardTransferState = cardTransferState,
        activityShortcutCards = activityShortcutCards,
        shellCommandCards = shellCommandCards,
        googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults
    )
    val sectionStateStore = rememberOsPageSectionStateStore()
    val sectionStates = sectionStateStore.sectionStates
    val updateSection = sectionStateStore.updateSection
    val actionState = createOsPageActionState(
        context = context,
        scope = scope,
        shizukuStatus = shizukuStatus,
        shizukuApiUtils = shizukuApiUtils,
        sectionLoadMutex = sectionLoadMutex,
        sectionLoadDeferreds = sectionLoadDeferreds,
        visibleCardsProvider = { visibleCards },
        sectionStatesProvider = { sectionStates },
        updateSection = updateSection,
        onCachePersistedChanged = { cachePersisted = it },
        updateVisibleCards = { visibleCards = it },
        setTopInfoExpanded = { topInfoExpanded = it },
        setShellRunnerExpanded = { shellRunnerExpanded = it },
        setSystemTableExpanded = { systemTableExpanded = it },
        setSecureTableExpanded = { secureTableExpanded = it },
        setGlobalTableExpanded = { globalTableExpanded = it },
        setAndroidPropsExpanded = { androidPropsExpanded = it },
        setJavaPropsExpanded = { javaPropsExpanded = it },
        setLinuxEnvExpanded = { linuxEnvExpanded = it },
        activityShortcutCardsProvider = { activityShortcutCards },
        updateActivityShortcutCards = { activityShortcutCards = it },
        googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
        updateShellCommandCards = { shellCommandCards = it },
        runningShellCommandCardIdsProvider = { runningShellCommandCardIds },
        onRunningShellCommandCardIdsChange = { runningShellCommandCardIds = it },
        onRefreshingChange = { refreshing = it },
        onRefreshProgressChange = { refreshProgress = it },
        shellCardCommandRequiredToast = textBundle.shellCardCommandRequiredToast,
        shellRunNoPermissionText = textBundle.shellRunNoPermissionText,
        shellRunNoOutputText = textBundle.shellRunNoOutputText,
        noRefreshableCardText = textBundle.noRefreshableCardText,
        refreshCompletedText = textBundle.refreshCompletedText
    )

    BindOsCardExpandedStateMaps(
        activityShortcutCards = activityShortcutCards,
        activityCardExpanded = activityCardExpanded,
        initialGoogleSystemServiceExpanded = initialUiSnapshot.googleSystemServiceExpanded,
        shellCommandCards = shellCommandCards,
        shellCommandCardExpanded = shellCommandCardExpanded
    )

    BindOsScrollToTopEffect(
        scrollToTopSignal = runtime.scrollToTopSignal,
        listState = listState
    )

    BindOsInitialCacheLoad(
        visibleCards = visibleCards,
        onVisibleCardsChange = { visibleCards = it },
        onSectionStatesChange = sectionStateStore.onSectionStatesChange,
        onCachePersistedChange = { cachePersisted = it },
        onCacheLoadedChange = { cacheLoaded = it },
        onUiStatePersistenceReadyChange = { uiStatePersistenceReady = it },
        isPageActive = runtime.isDataActive,
        ensureLoad = actionState.ensureLoad
    )

    BindOsShizukuInvalidation(
        shizukuReady = shizukuReady,
        updateSection = updateSection
    )

    BindOsExpandedStatePersistence(
        ready = uiStatePersistenceReady,
        snapshotProvider = {
            OsUiSnapshot(
                topInfoExpanded = topInfoExpanded,
                shellRunnerExpanded = shellRunnerExpanded,
                systemTableExpanded = systemTableExpanded,
                secureTableExpanded = secureTableExpanded,
                globalTableExpanded = globalTableExpanded,
                androidPropsExpanded = androidPropsExpanded,
                javaPropsExpanded = javaPropsExpanded,
                linuxEnvExpanded = linuxEnvExpanded
            )
        }
    )

    BindOsVisibleSectionLoadEffects(
        cacheLoaded = cacheLoaded,
        isDataActive = runtime.isDataActive,
        visibleCards = visibleCards,
        systemTableExpanded = systemTableExpanded,
        secureTableExpanded = secureTableExpanded,
        globalTableExpanded = globalTableExpanded,
        androidPropsExpanded = androidPropsExpanded,
        javaPropsExpanded = javaPropsExpanded,
        linuxEnvExpanded = linuxEnvExpanded,
        ensureLoad = { section -> actionState.ensureLoad(section, false) }
    )
    BindOsActivitySuggestionLoadEffect(
        showActivitySuggestionSheet = overlayState.showActivitySuggestionSheet,
        googleSystemServiceSuggestionTarget = overlayState.googleSystemServiceSuggestionTarget,
        activityShortcutDraftPackageName = overlayState.activityShortcutDraft.packageName,
        context = context,
        onPackageSuggestionsLoadingChange = overlayState.onGoogleSystemServicePackageSuggestionsLoadingChange,
        onPackageSuggestionsChange = overlayState.onGoogleSystemServicePackageSuggestionsChange,
        onClassSuggestionsLoadingChange = overlayState.onGoogleSystemServiceClassSuggestionsLoadingChange,
        onClassSuggestionsChange = overlayState.onGoogleSystemServiceClassSuggestionsChange
    )

    val derivedState = rememberOsPageDerivedState(
        context = context,
        queryApplied = queryApplied,
        shizukuStatus = shizukuStatus,
        shellSavedCountLabel = textBundle.shellSavedCountLabel,
        shellCommandCards = shellCommandCards,
        sectionStates = sectionStates,
        topInfoExpanded = topInfoExpanded,
        systemTableExpanded = systemTableExpanded,
        secureTableExpanded = secureTableExpanded,
        globalTableExpanded = globalTableExpanded,
        androidPropsExpanded = androidPropsExpanded,
        javaPropsExpanded = javaPropsExpanded,
        linuxEnvExpanded = linuxEnvExpanded,
        isDark = isDark,
        inactiveColor = inactive,
        cachedColor = cachedColor,
        refreshingColor = refreshingColor,
        syncedColor = syncedColor,
        surfaceColor = surfaceColor,
        refreshing = refreshing,
        refreshProgress = refreshProgress,
        cachePersisted = cachePersisted,
        visibleCards = visibleCards,
        activityShortcutCards = activityShortcutCards
    )

    val overviewState = derivedState.overviewUiState.overviewState
    val statusLabel = derivedState.overviewUiState.statusLabel
    val statusColor = derivedState.overviewUiState.statusColor
    val overviewCardColor = derivedState.overviewUiState.overviewCardColor
    val overviewBorderColor = derivedState.overviewUiState.overviewBorderColor
    val indicatorProgress = derivedState.overviewUiState.indicatorProgress
    val indicatorBg = derivedState.overviewUiState.indicatorBg
    val overviewMetrics = derivedState.overviewUiState.metrics

    OsPageScaffoldShell(
        scrollBehavior = scrollBehavior,
        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
        topBarBackdrop = backdrops.topBar,
        layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        reduceEffectsDuringPagerScroll = runtime.isPagerScrollInProgress,
        manageCardsContentDescription = textBundle.manageCardsContentDescription,
        manageActivitiesContentDescription = textBundle.manageActivitiesContentDescription,
        manageShellCardsContentDescription = textBundle.manageShellCardsContentDescription,
        refreshParamsContentDescription = textBundle.refreshParamsContentDescription,
        refreshing = refreshing,
        onOpenCardManager = { overlayState.onShowCardManagerChange(true) },
        onOpenActivityVisibilityManager = { overlayState.onShowActivityVisibilityManagerChange(true) },
        onOpenShellCardVisibilityManager = { overlayState.onShowShellCardVisibilityManagerChange(true) },
        onRefresh = { scope.launch { actionState.refreshAllSections() } },
        onActionBarInteractingChanged = onActionBarInteractingChanged,
        searchBarVisible = enableSearchBar && showSearchBar,
        queryInput = queryInput,
        onQueryInputChange = osPageViewModel::updateQueryInput,
        searchLabel = textBundle.searchLabel
    ) { innerPadding ->
        OsPageOverlayCoordinator(
            context = context,
            scope = scope,
            sheetBackdrop = backdrops.sheet,
            overlayState = overlayState,
            visibleCards = visibleCards,
            activityShortcutCards = activityShortcutCards,
            shellCommandCards = shellCommandCards,
            actionState = actionState,
            overlayTransferActions = overlayTransferActions,
            cardTransferState = cardTransferState,
            textBundle = textBundle,
            onShellCommandCardsChange = { shellCommandCards = it },
            onRemoveShellCommandCardExpanded = { shellCommandCardExpanded.remove(it) },
            onActivityShortcutCardsChange = { activityShortcutCards = it },
            onRemoveActivityCardExpanded = { activityCardExpanded.remove(it) }
        )
        OsPageMainList(
            context = context,
            listState = listState,
            innerPadding = innerPadding,
            searchBarScrollConnection = searchBarScrollConnection,
            scrollBehaviorConnection = scrollBehavior.nestedScrollConnection,
            contentBackdrop = backdrops.content,
            isDark = isDark,
            titleColor = titleColor,
            cardPressFeedbackEnabled = cardPressFeedbackEnabled,
            refreshing = refreshing,
            overviewState = overviewState,
            indicatorProgress = indicatorProgress,
            statusColor = statusColor,
            indicatorBg = indicatorBg,
            statusLabel = statusLabel,
            overviewCardColor = overviewCardColor,
            overviewBorderColor = overviewBorderColor,
            overviewMetrics = overviewMetrics,
            noMatchedResultsText = textBundle.noMatchedResultsText,
            query = derivedState.query,
            displayedTopInfoRows = derivedState.displayedTopInfoRows,
            groupedTopInfoRows = derivedState.groupedTopInfoRows,
            topInfoExpanded = topInfoExpanded,
            onTopInfoExpandedChange = { topInfoExpanded = it },
            shellRunnerRows = derivedState.shellRunnerRows,
            shellRunnerExpanded = shellRunnerExpanded,
            onShellRunnerExpandedChange = { shellRunnerExpanded = it },
            onOpenShellRunner = { OsShellRunnerActivity.launch(context) },
            shellCommandCards = shellCommandCards,
            shellCommandCardExpanded = shellCommandCardExpanded,
            runningShellCommandCardIds = runningShellCommandCardIds,
            onShellCommandCardExpandedChange = { cardId, expanded ->
                shellCommandCardExpanded[cardId] = expanded
            },
            onOpenShellCommandCardEditor = { card ->
                overlayState.onEditingShellCommandCardIdChange(card.id)
                overlayState.onShellCommandCardDraftChange(card)
                overlayState.onShowShellCommandCardEditorChange(true)
            },
            onRunShellCommandCard = { card ->
                scope.launch { actionState.runShellCommandCard(card) }
            },
            activityShortcutCards = activityShortcutCards,
            defaultActivityCardTitle = textBundle.googleSystemServiceDefaultTitle,
            activityCardExpanded = activityCardExpanded,
            onActivityCardExpandedChange = { cardId, expanded ->
                activityCardExpanded[cardId] = expanded
            },
            onOpenActivityShortcutCard = { card ->
                openOsActivityShortcutCard(
                    context = context,
                    card = card,
                    defaults = textBundle.googleSystemServiceDefaults,
                    invalidTargetMessage = context.getString(R.string.os_google_system_service_toast_invalid_target),
                    openFailedMessage = { error ->
                        context.getString(
                            R.string.os_google_system_service_toast_open_failed,
                            error.javaClass.simpleName
                        )
                    }
                )
            },
            onOpenActivityShortcutCardEditor = { card ->
                beginEditingOsActivityShortcutCard(
                    card = card,
                    defaults = textBundle.googleSystemServiceDefaults,
                    onEditModeChange = overlayState.onActivityCardEditModeChange,
                    onEditingCardIdChange = overlayState.onEditingActivityShortcutCardIdChange,
                    onEditingBuiltInChange = overlayState.onEditingActivityShortcutBuiltInChange,
                    onDraftChange = overlayState.onActivityShortcutDraftChange,
                    onShowEditorChange = overlayState.onShowActivityShortcutEditorChange
                )
            },
            displayedSystemRows = derivedState.displayedSystemRows,
            displayedSecureRows = derivedState.displayedSecureRows,
            displayedGlobalRows = derivedState.displayedGlobalRows,
            displayedAndroidRows = derivedState.displayedAndroidRows,
            displayedJavaRows = derivedState.displayedJavaRows,
            displayedLinuxRows = derivedState.displayedLinuxRows,
            prunedSystemRows = derivedState.prunedSystemRows,
            prunedSecureRows = derivedState.prunedSecureRows,
            prunedGlobalRows = derivedState.prunedGlobalRows,
            prunedAndroidRows = derivedState.prunedAndroidRows,
            prunedJavaRows = derivedState.prunedJavaRows,
            prunedLinuxRows = derivedState.prunedLinuxRows,
            systemTableExpanded = systemTableExpanded,
            onSystemTableExpandedChange = { systemTableExpanded = it },
            secureTableExpanded = secureTableExpanded,
            onSecureTableExpandedChange = { secureTableExpanded = it },
            globalTableExpanded = globalTableExpanded,
            onGlobalTableExpandedChange = { globalTableExpanded = it },
            androidPropsExpanded = androidPropsExpanded,
            onAndroidPropsExpandedChange = { androidPropsExpanded = it },
            javaPropsExpanded = javaPropsExpanded,
            onJavaPropsExpandedChange = { javaPropsExpanded = it },
            linuxEnvExpanded = linuxEnvExpanded,
            onLinuxEnvExpandedChange = { linuxEnvExpanded = it },
            isCardVisible = { card -> isCardVisible(visibleCards, card) },
            sectionSubtitle = { section, size ->
                sectionSubtitle(
                    sectionStates = sectionStates,
                    context = context,
                    section = section,
                    size = size
                )
            },
            exportingCard = overlayState.exportingCard,
            onExportCard = { card ->
                scope.launch {
                    exportOsPageCard(
                        card = card,
                        currentExportingCard = overlayState.exportingCard,
                        updateExportingCard = overlayState.onExportingCardChange,
                        visibleCardsProvider = { visibleCards },
                        ensureLoad = actionState.ensureLoad,
                        sectionStatesProvider = { sectionStates },
                        activityShortcutCardsProvider = { activityShortcutCards },
                        googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
                        context = context,
                        shizukuStatus = shizukuStatus,
                        launchExport = { fileName, payload ->
                            overlayState.onPendingExportContentChange(payload)
                            cardTransferState.exportLauncher.launch(fileName)
                        }
                    )
                }
            },
            onRefreshAll = { scope.launch { actionState.refreshAllSections() } },
            contentBottomPadding = runtime.contentBottomPadding,
            showFloatingAddButton = !overlayState.showActivityShortcutEditor &&
                    !overlayState.showActivitySuggestionSheet &&
                    !overlayState.showShellCommandCardEditor &&
                    !overlayState.showShellCardVisibilityManager,
            onOpenAddActivityShortcutCard = {
                overlayState.onActivityCardEditModeChange(OsActivityCardEditMode.Add)
                overlayState.onEditingActivityShortcutCardIdChange(null)
                overlayState.onEditingActivityShortcutBuiltInChange(false)
                overlayState.onActivityShortcutDraftChange(
                    createDefaultActivityShortcutDraft(textBundle.googleSystemServiceDefaults)
                )
                overlayState.onShowActivityShortcutEditorChange(true)
            }
        )
    }
}
