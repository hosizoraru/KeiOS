package com.example.keios.ui.page.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keios.ui.page.main.ba.BASessionState
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.ba.BA_AP_MAX
import com.example.keios.ui.page.main.ba.BaCalendarEntry
import com.example.keios.ui.page.main.ba.BaCalendarPoolViewModel
import com.example.keios.ui.page.main.ba.BaPageCommonEffects
import com.example.keios.ui.page.main.ba.BaPageContent
import com.example.keios.ui.page.main.ba.BaPoolEntry
import com.example.keios.ui.page.main.ba.BaSettingsSheet
import com.example.keios.ui.page.main.ba.BaTopBar
import com.example.keios.ui.page.main.ba.applyBaCalendarRefreshInterval
import com.example.keios.ui.page.main.ba.buildBaPageContentActions
import com.example.keios.ui.page.main.ba.buildBaPageContentState
import com.example.keios.ui.page.main.ba.buildBaSettingsSheetState
import com.example.keios.ui.page.main.ba.openBaExternalLink
import com.example.keios.ui.page.main.ba.rememberBaOfficeController
import com.example.keios.ui.page.main.ba.rememberBaPageUiController
import com.example.keios.ui.page.main.ba.saveBaPageSettings
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BAPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    isPageActive: Boolean = true,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    onOpenPoolStudentGuide: (String) -> Unit = {},
    onOpenGuideCatalog: () -> Unit = {},
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val surfaceColor = MiuixTheme.colorScheme.surface
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val topBarBackdrop: LayerBackdrop = key("ba-topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val contentBackdrop: LayerBackdrop = key("ba-content-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val sheetBackdrop: LayerBackdrop = key("ba-sheet-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val baSmallTitleMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
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

    val initialSnapshot = remember { BASettingsStore.loadSnapshot() }
    val office = rememberBaOfficeController(initialSnapshot)
    val ui = rememberBaPageUiController(initialSnapshot)
    val calendarPoolViewModel: BaCalendarPoolViewModel = viewModel()
    val calendarUiState by calendarPoolViewModel.calendarUiState.collectAsState()
    val poolUiState by calendarPoolViewModel.poolUiState.collectAsState()

    var baCalendarEntries by remember { mutableStateOf(emptyList<BaCalendarEntry>()) }
    var baPoolEntries by remember { mutableStateOf(emptyList<BaPoolEntry>()) }
    val officeSmallTitle = when (ui.serverIndex) {
        0 -> "沙勒办公室"
        1 -> "夏萊行政室"
        else -> "夏莱办公室"
    }

    val settingsSheetState = buildBaSettingsSheetState(ui)
    val pageContentState = buildBaPageContentState(
        isPageActive = isPageActive,
        officeSmallTitle = officeSmallTitle,
        baSmallTitleMargin = baSmallTitleMargin,
        office = office,
        ui = ui,
        serverOptions = serverOptions,
        cafeLevelOptions = cafeLevelOptions,
        baCalendarEntries = baCalendarEntries,
        baPoolEntries = baPoolEntries,
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
        scrollToTopSignal = scrollToTopSignal,
        isPageActive = isPageActive,
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
            delay(96)
            ui.poolHydrationReady = true
        },
        context = context,
    )

    LaunchedEffect(
        isPageActive,
        ui.serverIndex,
        ui.baCalendarReloadSignal,
        ui.calendarRefreshIntervalHours,
        ui.calendarHydrationReady
    ) {
        calendarPoolViewModel.syncCalendar(
            isPageActive = isPageActive,
            serverIndex = ui.serverIndex,
            reloadSignal = ui.baCalendarReloadSignal,
            calendarRefreshIntervalHours = ui.calendarRefreshIntervalHours,
            hydrationReady = ui.calendarHydrationReady
        )
    }
    LaunchedEffect(
        isPageActive,
        ui.serverIndex,
        ui.baPoolReloadSignal,
        ui.calendarRefreshIntervalHours,
        ui.poolHydrationReady
    ) {
        calendarPoolViewModel.syncPool(
            isPageActive = isPageActive,
            serverIndex = ui.serverIndex,
            reloadSignal = ui.baPoolReloadSignal,
            calendarRefreshIntervalHours = ui.calendarRefreshIntervalHours,
            hydrationReady = ui.poolHydrationReady
        )
    }
    LaunchedEffect(calendarUiState) {
        baCalendarEntries = calendarUiState.entries
        ui.baCalendarLoading = calendarUiState.loading
        ui.baCalendarError = calendarUiState.error
        ui.baCalendarLastSyncMs = calendarUiState.lastSyncMs
    }
    LaunchedEffect(poolUiState) {
        baPoolEntries = poolUiState.entries
        ui.baPoolLoading = poolUiState.loading
        ui.baPoolError = poolUiState.error
        ui.baPoolLastSyncMs = poolUiState.lastSyncMs
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BaTopBar(
                backdrop = topBarBackdrop,
                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
                scrollBehavior = scrollBehavior,
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
        },
    ) { innerPadding ->
        BaPageContent(
            backdrop = contentBackdrop,
            innerPadding = innerPadding,
            contentBottomPadding = contentBottomPadding,
            listState = listState,
            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            state = pageContentState,
            actions = pageContentActions,
        )
    }

    BaSettingsSheet(
        show = ui.showSettingsSheet,
        backdrop = sheetBackdrop,
        state = settingsSheetState,
        onApNotifyEnabledChange = { ui.sheetApNotifyEnabled = it },
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
