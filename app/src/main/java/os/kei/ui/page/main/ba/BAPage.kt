package os.kei.ui.page.main.ba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import os.kei.R
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.host.pager.rememberMainPageBackdropSet
import os.kei.ui.page.main.ba.support.BASessionState
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BA_AP_MAX
import os.kei.ui.page.main.ba.BaCalendarPoolViewModel
import os.kei.ui.page.main.ba.BaOfficeViewModel
import os.kei.ui.page.main.ba.BaPageCommonEffects
import os.kei.ui.page.main.ba.BaPageContent
import os.kei.ui.page.main.ba.BaSettingsSheet
import os.kei.ui.page.main.ba.BaTopBar
import os.kei.ui.page.main.ba.applyBaCalendarRefreshInterval
import os.kei.ui.page.main.ba.buildBaPageContentActions
import os.kei.ui.page.main.ba.buildBaPageContentState
import os.kei.ui.page.main.ba.buildBaSettingsSheetState
import os.kei.ui.page.main.ba.openBaExternalLink
import os.kei.ui.page.main.ba.rememberBaPageUiController
import os.kei.ui.page.main.ba.saveBaPageSettings
import os.kei.core.ui.effect.getMiuixAppBarColor
import os.kei.core.ui.effect.rememberMiuixBlurBackdrop
import os.kei.ui.page.main.widget.chrome.AppTopEndActionBarOverlay
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.glass.rememberListScrollGlassRuntime
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BAPage(
    runtime: MainPageRuntime = MainPageRuntime(contentBottomPadding = 72.dp),
    preloadingEnabled: Boolean = false,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    onOpenPoolStudentGuide: (String) -> Unit = {},
    onOpenGuideCatalog: () -> Unit = {},
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val isListScrolling by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    val pageBackdropEffectsEnabled = runtime.isPageActive &&
        !runtime.isPagerScrollInProgress
    val fullBackdropEffectsEnabled = pageBackdropEffectsEnabled &&
        !isListScrolling
    val backdrops = rememberMainPageBackdropSet(
        keyPrefix = "ba",
        refreshOnCompositionEnter = true,
        distinctLayers = fullBackdropEffectsEnabled
    )
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = pageBackdropEffectsEnabled)
    val serverOptions = remember { listOf("国服", "国际服", "日服") }
    val cafeLevelOptions = remember { (1..10).toList() }

    // Reset once per cold process start so app relaunch always lands at BA top.
    LaunchedEffect(Unit) {
        if (!BASessionState.didResetScrollOnThisProcess) {
            BASettingsStore.clearListScrollState()
            listState.scrollToItem(0)
            BASessionState.didResetScrollOnThisProcess = true
        }
    }

    val officeViewModel: BaOfficeViewModel = viewModel()
    val initialSnapshot = officeViewModel.initialSnapshot
    val office = officeViewModel.office
    val ui = rememberBaPageUiController(initialSnapshot)
    val calendarPoolViewModel: BaCalendarPoolViewModel = viewModel()
    val calendarUiState by calendarPoolViewModel.calendarUiState.collectAsState()
    val poolUiState by calendarPoolViewModel.poolUiState.collectAsState()

    val officeName = when (ui.serverIndex) {
        0 -> stringResource(R.string.ba_office_name_cn)
        1 -> stringResource(R.string.ba_office_name_global)
        else -> stringResource(R.string.ba_office_name_jp)
    }
    val officeOverviewTitle = stringResource(R.string.ba_office_overview_title, officeName)

    val settingsSheetState = buildBaSettingsSheetState(ui)
    val pageContentState = buildBaPageContentState(
        isPageActive = runtime.isPageActive,
        officeOverviewTitle = officeOverviewTitle,
        office = office,
        ui = ui,
        serverOptions = serverOptions,
        cafeLevelOptions = cafeLevelOptions,
        baCalendarEntries = calendarUiState.entries,
        baPoolEntries = poolUiState.entries,
    )
    val syncPageActive = if (preloadingEnabled) runtime.isPageActive else runtime.isDataActive
    val baGlassRuntime = rememberListScrollGlassRuntime(
        isListScrolling = isListScrolling,
        label = "baListGlassEffectProgress"
    )

    fun openSettingsSheet() {
        ui.openSettingsSheet(office)
    }

    fun closeSettingsSheet() {
        ui.closeSettingsSheet(office)
    }

    fun refreshCalendar(force: Boolean = false) {
        ui.refreshCalendar(force)
    }

    fun refreshPool(force: Boolean = false) {
        ui.refreshPool(force)
    }

    fun saveSettings() {
        saveBaPageSettings(
            context = context,
            office = office,
            ui = ui,
            settingsSheetState = settingsSheetState,
            onRefreshCalendar = ::refreshCalendar,
            onRefreshPool = ::refreshPool,
        )
    }

    val pageContentActions = buildBaPageContentActions(
        context = context,
        office = office,
        ui = ui,
        onRefreshCalendar = { refreshCalendar(force = true) },
        onRefreshPool = { refreshPool(force = true) },
        onOpenCalendarLink = { url -> openBaExternalLink(context = context, url = url) },
        onOpenPoolStudentGuide = onOpenPoolStudentGuide,
        onOpenGuideCatalog = onOpenGuideCatalog,
    )

    BaPageCommonEffects(
        listState = listState,
        scrollBehavior = scrollBehavior,
        scrollToTopSignal = runtime.scrollToTopSignal,
        isPageActive = runtime.isDataActive,
        consumedScrollToTopSignal = ui.consumedScrollToTopSignal,
        onConsumedScrollToTopSignalChange = { ui.consumedScrollToTopSignal = it },
        onDisposeActionBarInteraction = { onActionBarInteractingChanged(false) },
        office = office,
        onUiNowMsChange = { ui.uiNowMs = it },
        serverIndex = ui.serverIndex,
        onServerChanged = {
            ui.baCalendarLoading = true
            ui.baPoolLoading = true
            ui.baCalendarError = null
            ui.baPoolError = null
            ui.calendarHydrationReady = false
            ui.poolHydrationReady = false
            ui.calendarHydrationReady = true
            ui.poolHydrationReady = true
        },
        context = context,
    )

    LaunchedEffect(
        syncPageActive,
        ui.serverIndex,
        ui.baCalendarReloadSignal,
        ui.calendarRefreshIntervalHours,
        ui.calendarHydrationReady
    ) {
        calendarPoolViewModel.syncCalendar(
            isPageActive = syncPageActive,
            serverIndex = ui.serverIndex,
            reloadSignal = ui.baCalendarReloadSignal,
            calendarRefreshIntervalHours = ui.calendarRefreshIntervalHours,
            hydrationReady = ui.calendarHydrationReady
        )
    }
    LaunchedEffect(
        syncPageActive,
        ui.serverIndex,
        ui.baPoolReloadSignal,
        ui.calendarRefreshIntervalHours,
        ui.poolHydrationReady
    ) {
        calendarPoolViewModel.syncPool(
            isPageActive = syncPageActive,
            serverIndex = ui.serverIndex,
            reloadSignal = ui.baPoolReloadSignal,
            calendarRefreshIntervalHours = ui.calendarRefreshIntervalHours,
            hydrationReady = ui.poolHydrationReady
        )
    }
    LaunchedEffect(calendarUiState) {
        ui.baCalendarLoading = calendarUiState.loading
        ui.baCalendarError = calendarUiState.error
        ui.baCalendarLastSyncMs = calendarUiState.lastSyncMs
    }
    LaunchedEffect(poolUiState) {
        ui.baPoolLoading = poolUiState.loading
        ui.baPoolError = poolUiState.error
        ui.baPoolLastSyncMs = poolUiState.lastSyncMs
    }

    CompositionLocalProvider(LocalGlassEffectRuntime provides baGlassRuntime) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    BaTopBar(
                        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
                        scrollBehavior = scrollBehavior,
                    )
                },
            ) { innerPadding ->
                BaPageContent(
                    backdrop = backdrops.content,
                    innerPadding = innerPadding,
                    contentBottomPadding = runtime.contentBottomPadding,
                    listState = listState,
                    nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                    state = pageContentState,
                    actions = pageContentActions,
                )
            }
            AppTopEndActionBarOverlay {
                BaTopBarActions(
                    backdrop = backdrops.topBar,
                    liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    reduceEffectsDuringPagerScroll = runtime.isPagerScrollInProgress,
                    showCalendarIntervalPopup = ui.showCalendarIntervalPopup,
                    calendarRefreshIntervalHours = ui.calendarRefreshIntervalHours,
                    onShowSettings = ::openSettingsSheet,
                    onShowCalendarIntervalPopupChange = { ui.showCalendarIntervalPopup = it },
                    onCopyFriendCode = { office.copyFriendCodeToClipboard(context) },
                    onRefreshAll = {
                        office.applyCafeStorage()
                        office.applyApRegen()
                        refreshCalendar(force = true)
                        refreshPool(force = true)
                    },
                    onCalendarRefreshIntervalSelected = { hours ->
                        applyBaCalendarRefreshInterval(
                            ui = ui,
                            hours = hours,
                            onRefreshCalendar = { refreshCalendar(force = true) },
                            onRefreshPool = { refreshPool(force = true) },
                        )
                    },
                    onInteractionChanged = onActionBarInteractingChanged,
                )
            }
        }

        BaSettingsSheet(
            show = ui.showSettingsSheet,
            backdrop = backdrops.sheet,
            state = settingsSheetState,
            onApNotifyEnabledChange = { ui.sheetApNotifyEnabled = it },
            onArenaRefreshNotifyEnabledChange = { ui.sheetArenaRefreshNotifyEnabled = it },
            onCafeVisitNotifyEnabledChange = { ui.sheetCafeVisitNotifyEnabled = it },
            onApNotifyThresholdTextChange = { ui.sheetApNotifyThresholdText = it },
            onApNotifyThresholdDone = {
                val normalized = ui.sheetApNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120
                ui.sheetApNotifyThresholdText = normalized.toString()
            },
            onMediaAdaptiveRotationEnabledChange = { ui.sheetMediaAdaptiveRotationEnabled = it },
            onMediaSaveCustomEnabledChange = { ui.sheetMediaSaveCustomEnabled = it },
            onMediaSaveFixedTreeUriChange = { ui.sheetMediaSaveFixedTreeUri = it },
            onShowEndedActivitiesChange = { ui.sheetShowEndedActivities = it },
            onShowEndedPoolsChange = { ui.sheetShowEndedPools = it },
            onShowCalendarPoolImagesChange = { ui.sheetShowCalendarPoolImages = it },
            onDismissRequest = ::closeSettingsSheet,
            onSaveRequest = ::saveSettings,
        )
    }
}
