package com.example.keios.ui.page.main.os

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.keios.R
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.ui.effect.getMiuixAppBarColor
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keios.ui.page.main.host.pager.MainPageRuntime
import com.example.keios.ui.page.main.host.pager.rememberMainPageBackdropSet
import com.example.keios.ui.page.main.os.components.OsPageMainList
import com.example.keios.ui.page.main.os.components.OsPageOverlayHost
import com.example.keios.ui.page.main.os.state.createOsPageActionState
import com.example.keios.ui.page.main.os.state.rememberOsPageCardTransferState
import com.example.keios.ui.page.main.os.state.rememberOsPageOverlayState
import com.example.keios.ui.page.main.os.state.rememberOsPageOverlayTransferActions
import com.example.keios.ui.page.main.os.state.rememberOsPageTextBundle
import com.example.keios.ui.page.main.os.shell.OsShellCommandCardStore
import com.example.keios.ui.page.main.os.shell.OsShellRunnerActivity
import com.example.keios.ui.page.main.os.shortcut.OsActivityCardEditMode
import com.example.keios.ui.page.main.os.shortcut.OsActivityShortcutCardStore
import com.example.keios.ui.page.main.os.shortcut.createDefaultActivityShortcutDraft
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
    val isDark = isSystemInDarkTheme()
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val cachedColor = Color(0xFFF59E0B)
    val refreshingColor = Color(0xFF3B82F6)
    val syncedColor = Color(0xFF22C55E)
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textBundle = rememberOsPageTextBundle()
    val exportSuccessText = textBundle.exportSuccessText
    val noRefreshableCardText = textBundle.noRefreshableCardText
    val refreshCompletedText = textBundle.refreshCompletedText
    val manageCardsContentDescription = textBundle.manageCardsContentDescription
    val manageActivitiesContentDescription = textBundle.manageActivitiesContentDescription
    val manageShellCardsContentDescription = textBundle.manageShellCardsContentDescription
    val refreshParamsContentDescription = textBundle.refreshParamsContentDescription
    val searchLabel = textBundle.searchLabel
    val visibleCardsTitle = textBundle.visibleCardsTitle
    val visibleActivitiesTitle = textBundle.visibleActivitiesTitle
    val visibleShellCardsTitle = textBundle.visibleShellCardsTitle
    val visibleShellCardsDesc = textBundle.visibleShellCardsDesc
    val googleSystemServiceDefaultTitle = textBundle.googleSystemServiceDefaultTitle
    val shellSavedCountLabel = textBundle.shellSavedCountLabel
    val shellCardSavedToast = textBundle.shellCardSavedToast
    val shellCardDeletedToast = textBundle.shellCardDeletedToast
    val shellCardCommandRequiredToast = textBundle.shellCardCommandRequiredToast
    val shellCardDeleteDialogTitle = textBundle.shellCardDeleteDialogTitle
    val shellRunNoPermissionText = textBundle.shellRunNoPermissionText
    val shellRunNoOutputText = textBundle.shellRunNoOutputText
    val editShellCommandCardTitle = textBundle.editShellCommandCardTitle
    val editActivityCardTitle = textBundle.editActivityCardTitle
    val addActivityCardTitle = textBundle.addActivityCardTitle
    val activityCardDeletedToast = textBundle.activityCardDeletedToast
    val activityCardDeleteDialogTitle = textBundle.activityCardDeleteDialogTitle
    val cardImportFailedWithReason = textBundle.cardImportFailedWithReason
    val noMatchedResultsText = textBundle.noMatchedResultsText
    val googleSystemServiceDefaultIntentFlags = textBundle.googleSystemServiceDefaultIntentFlags
    val googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults
    val googleSettingsBuiltInSampleDefaults = textBundle.googleSettingsBuiltInSampleDefaults
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
                defaults = googleSystemServiceDefaults,
                builtInSampleDefaults = googleSettingsBuiltInSampleDefaults
            )
        )
    }
    val activityCardExpanded = remember { mutableStateMapOf<String, Boolean>() }
    val overlayState = rememberOsPageOverlayState(googleSystemServiceDefaults)
    var uiStatePersistenceReady by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    var refreshing by remember { mutableStateOf(false) }
    var refreshProgress by remember { mutableStateOf(0f) }
    var shellCommandCards by remember { mutableStateOf(OsShellCommandCardStore.loadCards()) }
    val shellCommandCardExpanded = remember { mutableStateMapOf<String, Boolean>() }
    var runningShellCommandCardIds by remember { mutableStateOf(emptySet<String>()) }
    val sectionLoadMutex = remember { Mutex() }
    val sectionLoadDeferreds = remember { mutableStateMapOf<SectionKind, Deferred<List<InfoRow>>>() }
    var showSearchBar by remember { mutableStateOf(true) }
    var searchBarHideOffsetPx by remember { mutableStateOf(0f) }
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrops = rememberMainPageBackdropSet(
        keyPrefix = "os",
        refreshOnCompositionEnter = true
    )
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val searchBarHideThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }
    val searchBarScrollConnection = remember(searchBarHideThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) {
                    if (showSearchBar) {
                        searchBarHideOffsetPx = (searchBarHideOffsetPx + (-available.y)).coerceAtMost(searchBarHideThresholdPx)
                        if (searchBarHideOffsetPx >= searchBarHideThresholdPx) {
                            showSearchBar = false
                            searchBarHideOffsetPx = 0f
                        }
                    }
                }
                if (available.y > 1f) {
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
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        googleSettingsBuiltInSampleDefaults = googleSettingsBuiltInSampleDefaults,
        cardImportFailedWithReason = cardImportFailedWithReason,
        exportSuccessText = exportSuccessText
    )
    val overlayTransferActions = rememberOsPageOverlayTransferActions(
        context = context,
        overlayState = overlayState,
        cardTransferState = cardTransferState,
        activityShortcutCards = activityShortcutCards,
        shellCommandCards = shellCommandCards,
        googleSystemServiceDefaults = googleSystemServiceDefaults
    )
    var sectionStates by remember {
        mutableStateOf(
            mapOf(
                SectionKind.SYSTEM to SectionState(),
                SectionKind.SECURE to SectionState(),
                SectionKind.GLOBAL to SectionState(),
                SectionKind.ANDROID to SectionState(),
                SectionKind.JAVA to SectionState(),
                SectionKind.LINUX to SectionState()
            )
        )
    }
    fun updateSection(section: SectionKind, transform: (SectionState) -> SectionState) {
        sectionStates = sectionStates.toMutableMap().also { map ->
            val old = map[section] ?: SectionState()
            map[section] = transform(old)
        }
    }
    val actionState = createOsPageActionState(
        context = context,
        scope = scope,
        shizukuStatus = shizukuStatus,
        shizukuApiUtils = shizukuApiUtils,
        sectionLoadMutex = sectionLoadMutex,
        sectionLoadDeferreds = sectionLoadDeferreds,
        visibleCardsProvider = { visibleCards },
        sectionStatesProvider = { sectionStates },
        updateSection = ::updateSection,
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
        googleSystemServiceDefaults = googleSystemServiceDefaults,
        updateShellCommandCards = { shellCommandCards = it },
        runningShellCommandCardIdsProvider = { runningShellCommandCardIds },
        onRunningShellCommandCardIdsChange = { runningShellCommandCardIds = it },
        onRefreshingChange = { refreshing = it },
        onRefreshProgressChange = { refreshProgress = it },
        shellCardCommandRequiredToast = shellCardCommandRequiredToast,
        shellRunNoPermissionText = shellRunNoPermissionText,
        shellRunNoOutputText = shellRunNoOutputText,
        noRefreshableCardText = noRefreshableCardText,
        refreshCompletedText = refreshCompletedText
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
        onSectionStatesChange = { sectionStates = it },
        onCachePersistedChange = { cachePersisted = it },
        onCacheLoadedChange = { cacheLoaded = it },
        onUiStatePersistenceReadyChange = { uiStatePersistenceReady = it },
        isPageActive = runtime.isDataActive,
        ensureLoad = actionState.ensureLoad
    )

    BindOsShizukuInvalidation(
        shizukuReady = shizukuReady,
        updateSection = ::updateSection
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
        shellSavedCountLabel = shellSavedCountLabel,
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
        manageCardsContentDescription = manageCardsContentDescription,
        manageActivitiesContentDescription = manageActivitiesContentDescription,
        manageShellCardsContentDescription = manageShellCardsContentDescription,
        refreshParamsContentDescription = refreshParamsContentDescription,
        refreshing = refreshing,
        onOpenCardManager = { overlayState.onShowCardManagerChange(true) },
        onOpenActivityVisibilityManager = { overlayState.onShowActivityVisibilityManagerChange(true) },
        onOpenShellCardVisibilityManager = { overlayState.onShowShellCardVisibilityManagerChange(true) },
        onRefresh = { scope.launch { actionState.refreshAllSections() } },
        onActionBarInteractingChanged = onActionBarInteractingChanged,
        searchBarVisible = enableSearchBar && showSearchBar,
        queryInput = queryInput,
        onQueryInputChange = osPageViewModel::updateQueryInput,
        searchLabel = searchLabel
    ) { innerPadding ->
        OsPageOverlayHost(
            context = context,
            scope = scope,
            sheetBackdrop = backdrops.sheet,
            overlayState = overlayState,
            visibleCardsTitle = visibleCardsTitle,
            visibleCardsHint = "隐藏卡片后会清空对应缓存；重新显示时会立即重新获取并缓存。",
            visibleCards = visibleCards,
            applyCardVisibility = actionState.applyCardVisibility,
            visibleActivitiesTitle = visibleActivitiesTitle,
            visibleActivitiesDesc = stringResource(R.string.os_sheet_visible_activities_desc),
            activityShortcutCards = activityShortcutCards,
            defaultActivityCardTitle = googleSystemServiceDefaultTitle,
            cardTransferInProgress = overlayState.cardTransferInProgress,
            onExportAllActivityCards = overlayTransferActions.onExportAllActivityCards,
            onImportAllActivityCards = overlayTransferActions.onImportAllActivityCards,
            applyActivityCardVisibility = actionState.applyActivityCardVisibility,
            visibleShellCardsTitle = visibleShellCardsTitle,
            visibleShellCardsDesc = visibleShellCardsDesc,
            shellRunnerVisible = visibleCards.contains(OsSectionCard.SHELL_RUNNER),
            shellCommandCards = shellCommandCards,
            onExportAllShellCards = overlayTransferActions.onExportAllShellCards,
            onImportAllShellCards = overlayTransferActions.onImportAllShellCards,
            applyShellCommandCardVisibility = actionState.applyShellCommandCardVisibility,
            editShellCommandCardTitle = editShellCommandCardTitle,
            onShellCommandCardsChange = { shellCommandCards = it },
            onRemoveShellCommandCardExpanded = { shellCommandCardExpanded.remove(it) },
            shellCardCommandRequiredToast = shellCardCommandRequiredToast,
            shellCardSavedToast = shellCardSavedToast,
            shellCardDeletedToast = shellCardDeletedToast,
            shellCardDeleteDialogTitle = shellCardDeleteDialogTitle,
            addActivityCardTitle = addActivityCardTitle,
            editActivityCardTitle = editActivityCardTitle,
            noMatchedResultsText = noMatchedResultsText,
            onActivityShortcutCardsChange = { activityShortcutCards = it },
            onRemoveActivityCardExpanded = { activityCardExpanded.remove(it) },
            googleSystemServiceDefaults = googleSystemServiceDefaults,
            googleSystemServiceDefaultTitle = googleSystemServiceDefaultTitle,
            googleSystemServiceDefaultIntentFlags = googleSystemServiceDefaultIntentFlags,
            activityCardDeletedToast = activityCardDeletedToast,
            activityCardDeleteDialogTitle = activityCardDeleteDialogTitle
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
            noMatchedResultsText = noMatchedResultsText,
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
            defaultActivityCardTitle = googleSystemServiceDefaultTitle,
            activityCardExpanded = activityCardExpanded,
            onActivityCardExpandedChange = { cardId, expanded ->
                activityCardExpanded[cardId] = expanded
            },
            onOpenActivityShortcutCard = { card ->
                openOsActivityShortcutCard(
                    context = context,
                    card = card,
                    defaults = googleSystemServiceDefaults,
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
                    defaults = googleSystemServiceDefaults,
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
                        googleSystemServiceDefaults = googleSystemServiceDefaults,
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
                    createDefaultActivityShortcutDraft(googleSystemServiceDefaults)
                )
                overlayState.onShowActivityShortcutEditorChange(true)
            }
        )
    }
}
