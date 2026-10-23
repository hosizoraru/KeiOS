package os.kei.ui.page.main.os

import androidx.compose.foundation.lazy.rememberLazyListState
import os.kei.ui.page.main.widget.chrome.appPageColumnCount
import os.kei.ui.page.main.widget.chrome.appPageContentMaxWidthFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.core.ext.showLiquidToastOnly
import os.kei.core.ext.showToast
import os.kei.core.privilege.PrivilegedShell
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.os.components.OsPageMainList
import os.kei.ui.page.main.os.components.OsPageMainListChromeState
import os.kei.ui.page.main.os.components.OsPageMainListContentState
import os.kei.ui.page.main.os.components.OsPageMainListExpansionState
import os.kei.ui.page.main.os.components.OsPageMainListOverviewState
import os.kei.ui.page.main.os.components.OsPageMainListSearchDockState
import os.kei.ui.page.main.os.components.OsPageOverlayCoordinator
import os.kei.ui.page.main.os.shortcut.launchGoogleSystemServiceActivity
import os.kei.ui.page.main.os.state.createOsPageActionState
import os.kei.ui.page.main.os.state.rememberOsPageCardTransferEventActions
import os.kei.ui.page.main.os.state.rememberOsPageCardTransferState
import os.kei.ui.page.main.os.state.rememberOsPageOverlayState
import os.kei.ui.page.main.os.state.rememberOsPageOverlayTransferActions
import os.kei.ui.page.main.os.state.rememberOsPageUiContext
import os.kei.ui.page.main.widget.chrome.BindLazyListScrollBoundsEffect
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import os.kei.core.privilege.PrivilegeStatus

@Composable
@Suppress("FunctionName")
fun OsPage(
    runtime: MainPageRuntime,
    privilegeStatus: PrivilegeStatus,
    privilegedShell: PrivilegedShell,
    onOpenShellRunner: () -> Unit,
    enableSearchBar: Boolean = true,
) {
    val listState = rememberLazyListState()
    // Two columns on a tablet or an unfolded fold. The OS list keeps a single lazy column either way;
    // the wide shape puts two lanes inside one of its items, because the second lane is reserved by
    // kind rather than packed by height. See osCardLanes.
    val osColumnCount = appPageColumnCount()
    val secondaryListState = rememberLazyListState()
    val pageScope = rememberCoroutineScope()
    val pageBackdropEffectsEnabled =
        runtime.isPageActive &&
            !runtime.isPagerScrollInProgress
    val settledDataActive = runtime.isSettledDataActive
    val osGlassRuntime = LocalGlassEffectRuntime.current
    val uiContext =
        rememberOsPageUiContext(
            enableFullBackdropEffects = pageBackdropEffectsEnabled,
            enableTopBarBackdropEffects = pageBackdropEffectsEnabled,
        )
    val context = uiContext.context
    val density = uiContext.density
    val textBundle = uiContext.textBundle
    val isDark = uiContext.isDark
    val inactive = uiContext.inactiveColor
    val titleColor = uiContext.titleColor
    val cachedColor = uiContext.cachedColor
    val refreshingColor = uiContext.refreshingColor
    val syncedColor = uiContext.syncedColor
    val privilegeReady = privilegeStatus.isCommandReady
    val lifecycleOwner = LocalLifecycleOwner.current
    val osPageViewModel: OsPageViewModel = viewModel()
    LaunchedEffect(
        settledDataActive,
        textBundle.googleSystemServiceDefaults,
        textBundle.builtInActivityShortcutCards,
        textBundle.builtInShellCommandCards,
    ) {
        if (!settledDataActive) return@LaunchedEffect
        osPageViewModel.loadPersistentState(
            googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
            builtInActivityShortcutCards = textBundle.builtInActivityShortcutCards,
            builtInShellCommandCards = textBundle.builtInShellCommandCards,
        )
    }
    val pageUiState by osPageViewModel.uiState.collectAsStateWithLifecycle()
    val activityIconState by osPageViewModel.activityIconState.collectAsStateWithLifecycle()
    val cardExpansionState by osPageViewModel.cardExpansionState.collectAsStateWithLifecycle()
    val persistentState = pageUiState.persistentState
    val runtimeState = pageUiState.runtimeState
    val activitySuggestionState = pageUiState.activitySuggestionState
    val activitySuggestionChromeState = pageUiState.activitySuggestionChromeState
    val chromeState = pageUiState.chromeState
    val queryInput = pageUiState.queryInput
    val queryApplied = pageUiState.queryApplied
    val rowsDerivedState = pageUiState.rowsDerivedState
    val cardListDerivedState = pageUiState.cardListDerivedState
    val uiSnapshot = persistentState.uiSnapshot
    val topInfoExpanded = uiSnapshot.topInfoExpanded
    val shellRunnerExpanded = uiSnapshot.shellRunnerExpanded
    val systemTableExpanded = uiSnapshot.systemTableExpanded
    val secureTableExpanded = uiSnapshot.secureTableExpanded
    val globalTableExpanded = uiSnapshot.globalTableExpanded
    val androidPropsExpanded = uiSnapshot.androidPropsExpanded
    val javaPropsExpanded = uiSnapshot.javaPropsExpanded
    val linuxEnvExpanded = uiSnapshot.linuxEnvExpanded
    val visibleCards = uiSnapshot.visibleCards
    val activityShortcutCards = cardListDerivedState.activityShortcutCards
    val overlayRuntimeActions = osPageViewModel.overlayRuntimeActions
    val overlayState =
        rememberOsPageOverlayState(
            activityShortcutDraft = runtimeState.activityShortcutDraft
                ?: os.kei.ui.page.main.os.shortcut.createDefaultActivityShortcutDraft(
                    textBundle.googleSystemServiceDefaults,
                ),
            onActivityShortcutDraftChange = overlayRuntimeActions::updateActivityShortcutDraft,
            shellCommandCardDraft = runtimeState.shellCommandCardDraft
                ?: os.kei.ui.page.main.os.shell.createDefaultShellCommandCardDraft(),
            onShellCommandCardDraftChange = overlayRuntimeActions::updateShellCommandCardDraft,
            showCardManager = runtimeState.showCardManager,
            onShowCardManagerChange = overlayRuntimeActions::updateShowCardManager,
            showActivityVisibilityManager = runtimeState.showActivityVisibilityManager,
            onShowActivityVisibilityManagerChange = overlayRuntimeActions::updateShowActivityVisibilityManager,
            activityVisibilityQuery = runtimeState.activityVisibilityQuery,
            onActivityVisibilityQueryChange = overlayRuntimeActions::updateActivityVisibilityQuery,
            showShellCardVisibilityManager = runtimeState.showShellCardVisibilityManager,
            onShowShellCardVisibilityManagerChange = overlayRuntimeActions::updateShowShellCardVisibilityManager,
            shellCardVisibilityQuery = runtimeState.shellCardVisibilityQuery,
            onShellCardVisibilityQueryChange = overlayRuntimeActions::updateShellCardVisibilityQuery,
            showActivityShortcutEditor = runtimeState.showActivityShortcutEditor,
            onShowActivityShortcutEditorChange = overlayRuntimeActions::updateShowActivityShortcutEditor,
            activityCardEditMode = runtimeState.activityCardEditMode,
            onActivityCardEditModeChange = overlayRuntimeActions::updateActivityCardEditMode,
            editingActivityShortcutCardId = runtimeState.editingActivityShortcutCardId,
            onEditingActivityShortcutCardIdChange = overlayRuntimeActions::updateEditingActivityShortcutCardId,
            editingActivityShortcutBuiltIn = runtimeState.editingActivityShortcutBuiltIn,
            onEditingActivityShortcutBuiltInChange = overlayRuntimeActions::updateEditingActivityShortcutBuiltIn,
            showShellCommandCardEditor = runtimeState.showShellCommandCardEditor,
            onShowShellCommandCardEditorChange = overlayRuntimeActions::updateShowShellCommandCardEditor,
            editingShellCommandCardId = runtimeState.editingShellCommandCardId,
            onEditingShellCommandCardIdChange = overlayRuntimeActions::updateEditingShellCommandCardId,
            showShellCardDeleteConfirm = runtimeState.showShellCardDeleteConfirm,
            onShowShellCardDeleteConfirmChange = overlayRuntimeActions::updateShowShellCardDeleteConfirm,
            showActivityCardDeleteConfirm = runtimeState.showActivityCardDeleteConfirm,
            onShowActivityCardDeleteConfirmChange = overlayRuntimeActions::updateShowActivityCardDeleteConfirm,
            pendingExportContent = runtimeState.pendingExportContent,
            onPendingExportContentChange = overlayRuntimeActions::updatePendingExportContent,
            pendingImportTarget = runtimeState.pendingImportTarget,
            onPendingImportTargetChange = overlayRuntimeActions::updatePendingImportTarget,
            pendingCardImportPreview = runtimeState.pendingCardImportPreview,
            onPendingCardImportPreviewChange = overlayRuntimeActions::updatePendingCardImportPreview,
            cardTransferInProgress = runtimeState.cardTransferInProgress,
            onCardTransferInProgressChange = overlayRuntimeActions::updateCardTransferInProgress,
        )
    val scrollBehavior = MiuixScrollBehavior()
    val shellCommandCards = cardListDerivedState.shellCommandCards
    val activityCardExpanded = cardExpansionState.activityCards
    val shellCommandCardExpanded = cardExpansionState.shellCommandCards
    val surfaceColor = uiContext.surfaceColor
    val backdrops = uiContext.backdrops
    val topBarMaterialBackdrop = uiContext.topBarMaterialBackdrop
    val overlaySheetVisible =
        overlayState.showCardManager ||
            overlayState.showActivityVisibilityManager ||
            overlayState.showShellCardVisibilityManager ||
            overlayState.showShellCommandCardEditor ||
            overlayState.showActivityShortcutEditor ||
            activitySuggestionChromeState.showSheet ||
            overlayState.pendingCardImportPreview != null
    LaunchedEffect(overlaySheetVisible) {
        osPageViewModel.updateOverlaySheetVisible(overlaySheetVisible)
    }
    DisposableEffect(Unit) {
        onDispose {
            osPageViewModel.closePersistentShell()
        }
    }
    LaunchedEffect(settledDataActive) {
        if (!settledDataActive) {
            osPageViewModel.closePersistentShell()
            osPageViewModel.cancelActiveSectionRefreshes()
        }
    }
    BindOsShellCardReloadOnResume(
        lifecycleOwner = lifecycleOwner,
        reloadCards = {
            osPageViewModel.reloadShellCommandCards(
                builtInShellCommandCards = textBundle.builtInShellCommandCards,
            )
        },
    )
    val cardTransferState =
        rememberOsPageCardTransferState(
            context = context,
            osPageViewModel = osPageViewModel,
            overlayState = overlayState,
            googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
            googleSettingsBuiltInSampleDefaults = textBundle.googleSettingsBuiltInSampleDefaults,
            builtInActivityShortcutCards = textBundle.builtInActivityShortcutCards,
        )
    val overlayTransferActions =
        rememberOsPageOverlayTransferActions(
            context = context,
            osPageViewModel = osPageViewModel,
            overlayState = overlayState,
            cardTransferState = cardTransferState,
            googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
        )
    val cardTransferEventActions =
        rememberOsPageCardTransferEventActions(
            context = context,
            overlayState = overlayState,
            retainActivityCardExpansion = osPageViewModel::retainActivityCardExpansion,
            retainShellCommandCardExpansion = osPageViewModel::retainShellCommandCardExpansion,
            textBundle = textBundle,
        )
    val collectOsPageEvents: suspend (suspend (OsPageEvent) -> Unit) -> Unit =
        remember(osPageViewModel) {
            { eventHandler ->
                osPageViewModel.events.collect { event ->
                    eventHandler(event)
                }
            }
        }
    BindOsPageEvents(
        collectEvents = collectOsPageEvents,
        onLaunchExportDocument = { fileName, content ->
            overlayState.onPendingExportContentChange(content)
            overlayState.onCardTransferInProgressChange(false)
            cardTransferState.exportLauncher.launch(fileName)
        },
        onExportFailed = { error ->
            overlayState.onCardTransferInProgressChange(false)
            context.showToast(
                context.getString(
                    R.string.common_export_failed_with_reason,
                    error.message ?: error.javaClass.simpleName,
                ),
            )
        },
        onCardExportWritten = cardTransferEventActions.onExportWritten,
        onCardExportWriteFailed = cardTransferEventActions.onExportWriteFailed,
        onCardImportPreviewReady = cardTransferEventActions.onImportPreviewReady,
        onCardImportFailed = cardTransferEventActions.onImportFailed,
        onCardTransferCompleted = cardTransferEventActions.onTransferCompleted,
        onActivityCardsImported = cardTransferEventActions.onActivityCardsImported,
        onShellCardsImported = cardTransferEventActions.onShellCardsImported,
        onOperationFailed = { error ->
            overlayState.onCardTransferInProgressChange(false)
            context.showToast(
                context.getString(
                    R.string.common_export_failed_with_reason,
                    error.message ?: error.javaClass.simpleName,
                ),
            )
        },
        onShellCommandCardSaved = {
            context.showToast(textBundle.shellCardSavedToast)
            overlayState.onShowShellCommandCardEditorChange(false)
            overlayState.onShowShellCardDeleteConfirmChange(false)
        },
        onShellCommandCardSaveFailed = {
            context.showToast(textBundle.shellCardCommandRequiredToast)
        },
        onShellCommandCardDeleted = { cardId ->
            osPageViewModel.removeShellCommandCardExpansion(cardId)
            overlayState.onEditingShellCommandCardIdChange(null)
            overlayState.onShowShellCommandCardEditorChange(false)
            context.showToast(textBundle.shellCardDeletedToast)
        },
        onActivityShortcutCardSaved = {
            context.showToast(R.string.os_google_system_service_toast_saved)
            overlayState.onShowActivityShortcutEditorChange(false)
            osPageViewModel.dismissActivitySuggestionSheet()
            overlayState.onShowActivityCardDeleteConfirmChange(false)
            overlayState.onEditingActivityShortcutBuiltInChange(false)
        },
        onActivityShortcutCardDeleted = { cardId ->
            osPageViewModel.removeActivityCardExpansion(cardId)
            overlayState.onEditingActivityShortcutCardIdChange(null)
            overlayState.onShowActivityShortcutEditorChange(false)
            osPageViewModel.dismissActivitySuggestionSheet()
            overlayState.onEditingActivityShortcutBuiltInChange(false)
            context.showToast(textBundle.activityCardDeletedToast)
        },
        onShellCommandCardCommandRequired = {
            context.showToast(textBundle.shellCardCommandRequiredToast)
        },
        onShellCommandCardNoPermission = {
            context.showToast(textBundle.shellRunNoPermissionText)
        },
        onShellCommandCardRunCompleted = {
            context.showLiquidToastOnly(textBundle.shellCardRunCompletedToast)
        },
        onShellCommandCardRunFailed = { error ->
            context.showToast(
                context.getString(
                    R.string.os_shell_card_toast_run_failed,
                    error.javaClass.simpleName,
                ),
            )
        },
        onRefreshCompleted = { refreshed ->
            context.showToast(
                if (refreshed) {
                    textBundle.refreshCompletedText
                } else {
                    textBundle.noRefreshableCardText
                },
            )
        },
        onLaunchActivityShortcut = { config ->
            runCatching {
                launchGoogleSystemServiceActivity(
                    context = context,
                    config = config,
                    defaults = textBundle.googleSystemServiceDefaults,
                )
            }.onFailure { error ->
                context.showToast(
                    context.getString(
                        R.string.os_google_system_service_toast_open_failed,
                        error.javaClass.simpleName,
                    ),
                )
            }
        },
        onActivityShortcutInvalidTarget = {
            context.showToast(R.string.os_google_system_service_toast_invalid_target)
        },
        onShowActivityShortcutEditor = { request ->
            overlayRuntimeActions.openActivityEditor(request)
        },
        onShowShellCommandCardEditor = { card ->
            overlayRuntimeActions.openShellEditor(card)
        },
    )
    val sectionStates = runtimeState.sectionStates
    val actionState =
        createOsPageActionState(
            context = context,
            privilegeStatus = privilegeStatus,
            privilegedShell = privilegedShell,
            osPageViewModel = osPageViewModel,
            googleSystemServiceDefaults = textBundle.googleSystemServiceDefaults,
            shellRunNoOutputText = textBundle.shellRunNoOutputText,
            builtInShellCommandCards = textBundle.builtInShellCommandCards,
        )

    BindOsCardExpandedStateMaps(
        activityShortcutCards = activityShortcutCards,
        initialGoogleSystemServiceExpanded = uiSnapshot.googleSystemServiceExpanded,
        shellCommandCards = shellCommandCards,
        syncActivityCardExpansion = osPageViewModel::syncActivityCardExpansion,
        syncShellCommandCardExpansion = osPageViewModel::syncShellCommandCardExpansion,
    )

    BindOsScrollToTopEffect(
        scrollToTopSignal = runtime.scrollToTopSignal,
        listState = listState,
    )
    BindLazyListScrollBoundsEffect(
        listState = listState,
        isActive = runtime.isPageActive,
        onScrollBoundsChange = runtime.onScrollBoundsChange,
    )

    BindOsInitialCacheLoad(
        ready = persistentState.loaded,
        cacheLoaded = runtimeState.cacheLoaded,
        isPageActive = settledDataActive,
        hydrateInitialCache = { isPageActive ->
            osPageViewModel.hydrateInitialCache(
                isPageActive = isPageActive,
                ensureLoad = actionState.ensureLoad,
            )
        },
    )

    BindOsPrivilegeInvalidation(
        privilegeReady = privilegeReady,
        onInvalidate = osPageViewModel::invalidatePrivilegeSections,
    )

    BindOsExpandedStatePersistence(
        ready = runtimeState.uiStatePersistenceReady,
        snapshotProvider = {
            uiSnapshot.copy(visibleCards = visibleCards)
        },
        persistSnapshot = osPageViewModel::saveExpandedStateSnapshot,
    )

    BindOsVisibleSectionLoadEffects(
        cacheLoaded = runtimeState.cacheLoaded,
        initialVisibleRefreshComplete = runtimeState.initialVisibleRefreshComplete,
        isDataActive = settledDataActive,
        visibleCards = visibleCards,
        systemTableExpanded = systemTableExpanded,
        secureTableExpanded = secureTableExpanded,
        globalTableExpanded = globalTableExpanded,
        androidPropsExpanded = androidPropsExpanded,
        javaPropsExpanded = javaPropsExpanded,
        linuxEnvExpanded = linuxEnvExpanded,
        ensureLoad = { section -> actionState.ensureLoad(section, false) },
    )
    BindOsActivitySuggestionLoadEffect(
        showActivitySuggestionSheet = activitySuggestionChromeState.showSheet,
        googleSystemServiceSuggestionTarget = activitySuggestionChromeState.target,
        activityShortcutDraftPackageName = overlayState.activityShortcutDraft.packageName,
        context = context,
        requestActivitySuggestions = osPageViewModel::requestActivitySuggestions,
    )
    BindOsActivityShortcutIconPreloadEffect(
        active = settledDataActive,
        activityShortcutCards = activityShortcutCards,
        showActivitySuggestionSheet = activitySuggestionChromeState.showSheet,
        googleSystemServiceSuggestionTarget = activitySuggestionChromeState.target,
        activityShortcutDraftPackageName = overlayState.activityShortcutDraft.packageName,
        packageSuggestions = activitySuggestionState.packageSuggestions,
        classSuggestions = activitySuggestionState.classSuggestions,
        context = context,
        requestActivityShortcutIcons = osPageViewModel::requestActivityShortcutIcons,
        requestPackageIcons = osPageViewModel::requestPackageIcons,
    )

    val routeState =
        rememberOsPageRouteState(
            queryApplied = queryApplied,
            uiSnapshot = uiSnapshot,
            visibleCards = visibleCards,
            activityShortcutCards = activityShortcutCards,
            shellCommandCards = shellCommandCards,
            sectionStates = sectionStates,
            refreshing = runtimeState.refreshing,
            refreshProgress = runtimeState.refreshProgress,
            cachePersisted = runtimeState.cachePersisted,
            runningShellCommandCardIds = runtimeState.runningShellCommandCardIds,
        )

    val derivedState =
        rememberOsPageDerivedState(
            context = context,
            rowsDerivedState = rowsDerivedState,
            privilegeStatus = privilegeStatus,
            shellSavedCountLabel = textBundle.shellSavedCountLabel,
            shellCommandCards = routeState.shellCommandCards,
            sectionStates = routeState.sectionStates,
            isDark = isDark,
            inactiveColor = inactive,
            cachedColor = cachedColor,
            refreshingColor = refreshingColor,
            syncedColor = syncedColor,
            surfaceColor = surfaceColor,
            refreshing = routeState.refreshing,
            refreshProgress = routeState.refreshProgress,
            cachePersisted = routeState.cachePersisted,
            visibleCards = routeState.visibleCards,
            activityShortcutCards = routeState.activityShortcutCards,
        )

    val overviewState = derivedState.overviewUiState.overviewState
    val statusLabel = derivedState.overviewUiState.statusLabel
    val statusColor = derivedState.overviewUiState.statusColor
    val overviewCardColor = derivedState.overviewUiState.overviewCardColor
    val overviewBorderColor = derivedState.overviewUiState.overviewBorderColor
    val indicatorProgress = derivedState.overviewUiState.indicatorProgress
    val indicatorBg = derivedState.overviewUiState.indicatorBg
    val overviewMetrics = derivedState.overviewMetrics
    val mainListActions =
        remember(
            context,
            textBundle,
            actionState,
            routeState,
            privilegeStatus,
            osPageViewModel,
            onOpenShellRunner,
        ) {
            createOsPageMainListActions(
                context = context,
                textBundle = textBundle,
                actionState = actionState,
                routeState = routeState,
                privilegeStatus = privilegeStatus,
                osPageViewModel = osPageViewModel,
                onOpenShellRunner = onOpenShellRunner,
            )
        }
    val mainListChromeState =
        remember(
            isDark,
            titleColor,
            pageBackdropEffectsEnabled,
            overlaySheetVisible,
            runtime.contentBottomPadding,
            runtime.bottomBarVisible,
            runtime.floatingDockSide,
            runtime.onShowBottomBar,
        ) {
            OsPageMainListChromeState(
                isDark = isDark,
                titleColor = titleColor,
                backdropProducerActive = pageBackdropEffectsEnabled && overlaySheetVisible,
                contentBottomPadding = runtime.contentBottomPadding,
                bottomBarVisible = runtime.bottomBarVisible,
                floatingDockSide = runtime.floatingDockSide,
                onExpandFloatingDock = runtime.onShowBottomBar,
            )
        }
    val mainListOverviewState =
        remember(
            runtimeState.refreshing,
            overviewState,
            indicatorProgress,
            statusColor,
            indicatorBg,
            statusLabel,
            overviewCardColor,
            overviewBorderColor,
            overviewMetrics,
        ) {
            OsPageMainListOverviewState(
                refreshing = runtimeState.refreshing,
                overviewState = overviewState,
                indicatorProgress = indicatorProgress,
                statusColor = statusColor,
                indicatorBg = indicatorBg,
                statusLabel = statusLabel,
                overviewCardColor = overviewCardColor,
                overviewBorderColor = overviewBorderColor,
                overviewMetrics = overviewMetrics,
            )
        }
    val mainListContentState =
        remember(
            textBundle.noMatchedResultsText,
            derivedState,
            cardListDerivedState,
            routeState.runningShellCommandCardIds,
            activityIconState.bitmaps,
            textBundle.googleSystemServiceDefaultTitle,
            runtimeState.exportingCard,
        ) {
            OsPageMainListContentState(
                noMatchedResultsText = textBundle.noMatchedResultsText,
                derivedState = derivedState,
                cardListDerivedState = cardListDerivedState,
                runningShellCommandCardIds = routeState.runningShellCommandCardIds,
                activityIconBitmaps = activityIconState.bitmaps,
                defaultActivityCardTitle = textBundle.googleSystemServiceDefaultTitle,
                exportingCard = runtimeState.exportingCard,
            )
        }
    val mainListExpansionState =
        remember(
            topInfoExpanded,
            shellRunnerExpanded,
            shellCommandCardExpanded,
            activityCardExpanded,
            systemTableExpanded,
            secureTableExpanded,
            globalTableExpanded,
            androidPropsExpanded,
            javaPropsExpanded,
            linuxEnvExpanded,
            osPageViewModel,
        ) {
            OsPageMainListExpansionState(
                topInfoExpanded = topInfoExpanded,
                shellRunnerExpanded = shellRunnerExpanded,
                shellCommandCardExpanded = shellCommandCardExpanded,
                activityCardExpanded = activityCardExpanded,
                systemTableExpanded = systemTableExpanded,
                secureTableExpanded = secureTableExpanded,
                globalTableExpanded = globalTableExpanded,
                androidPropsExpanded = androidPropsExpanded,
                javaPropsExpanded = javaPropsExpanded,
                linuxEnvExpanded = linuxEnvExpanded,
                onTopInfoExpandedChange = osPageViewModel::updateTopInfoExpanded,
                onShellRunnerExpandedChange = osPageViewModel::updateShellRunnerExpanded,
                onSystemTableExpandedChange = osPageViewModel::updateSystemTableExpanded,
                onSecureTableExpandedChange = osPageViewModel::updateSecureTableExpanded,
                onGlobalTableExpandedChange = osPageViewModel::updateGlobalTableExpanded,
                onAndroidPropsExpandedChange = osPageViewModel::updateAndroidPropsExpanded,
                onJavaPropsExpandedChange = osPageViewModel::updateJavaPropsExpanded,
                onLinuxEnvExpandedChange = osPageViewModel::updateLinuxEnvExpanded,
            )
        }
    // Guards the overview card's add click the way it used to guard the dock button:
    // no adding while a sheet or the visibility manager owns the screen.
    val mainListAddActionEnabled =
        !activitySuggestionChromeState.showSheet &&
            !overlayState.showShellCardVisibilityManager
    val mainListSearchDockState =
        remember(
            mainListAddActionEnabled,
            enableSearchBar,
            chromeState.searchExpanded,
            chromeState.overlaySearchSuppressed,
            queryInput,
            textBundle.searchLabel,
            osPageViewModel,
        ) {
            OsPageMainListSearchDockState(
                addActionEnabled = mainListAddActionEnabled,
                searchExpanded =
                    enableSearchBar &&
                        chromeState.searchExpanded &&
                        !chromeState.overlaySearchSuppressed,
                queryInput = queryInput,
                searchLabel = textBundle.searchLabel,
                onQueryInputChange = { value ->
                    if (!chromeState.overlaySearchSuppressed) {
                        osPageViewModel.updateQueryInput(value)
                    }
                },
                onSearchExpandedChange = { expanded ->
                    osPageViewModel.updateSearchExpanded(
                        enableSearchBar && expanded && !chromeState.overlaySearchSuppressed,
                    )
                },
            )
        }

    CompositionLocalProvider(LocalGlassEffectRuntime provides osGlassRuntime) {
        OsPageScaffoldShell(
            contentMaxWidth = appPageContentMaxWidthFor(osColumnCount),
            scrollBehavior = scrollBehavior,
            topBarColor = topBarMaterialBackdrop,
            topBarBackdrop = backdrops.topBar,
            manageCardsContentDescription = textBundle.manageCardsContentDescription,
            manageActivitiesContentDescription = textBundle.manageActivitiesContentDescription,
            manageShellCardsContentDescription = textBundle.manageShellCardsContentDescription,
            refreshParamsContentDescription = textBundle.refreshParamsContentDescription,
            refreshing = runtimeState.refreshing,
            onOpenCardManager = { overlayState.onShowCardManagerChange(true) },
            onOpenActivityVisibilityManager = { overlayState.onShowActivityVisibilityManagerChange(true) },
            onOpenShellCardVisibilityManager = { overlayState.onShowShellCardVisibilityManagerChange(true) },
            onRefresh = actionState.refreshAllSections,
            onTitleClick = {
                pageScope.launch {
                    // Both columns: the title belongs to the page, not to a lane.
                    listState.animateScrollToItem(0)
                    if (osColumnCount >= 2) secondaryListState.animateScrollToItem(0)
                }
            },
        ) { innerPadding ->
            OsPageOverlayCoordinator(
                context = context,
                sheetBackdrop = backdrops.sheet,
                overlayState = overlayState,
                editorDataReady = persistentState.loaded,
                visibleCards = visibleCards,
                activityShortcutCards = activityShortcutCards,
                activityIconBitmaps = activityIconState.bitmaps,
                packageIconBitmaps = activityIconState.packageBitmaps,
                shellCommandCards = shellCommandCards,
                activitySuggestionState = activitySuggestionState,
                activitySuggestionChromeState = activitySuggestionChromeState,
                actionState = actionState,
                overlayTransferActions = overlayTransferActions,
                cardTransferState = cardTransferState,
                textBundle = textBundle,
                builtInShellCommandCards = textBundle.builtInShellCommandCards,
                osPageViewModel = osPageViewModel,
            )
            OsPageMainList(
                context = context,
                listState = listState,
                secondaryListState = secondaryListState,
                columnCount = osColumnCount,
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                topBarProducer = backdrops.topBarProducer,
                topBarMaterial = backdrops.topBar,
                contentBackdrop = backdrops.contentMaterial,
                sheetProducer = backdrops.sheetProducer,
                chromeState = mainListChromeState,
                overviewState = mainListOverviewState,
                contentState = mainListContentState,
                expansionState = mainListExpansionState,
                searchDockState = mainListSearchDockState,
                actions = mainListActions,
            )
        }
    }
}
