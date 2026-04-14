package com.example.keios.ui.page.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.ba.BaCafeCard
import com.example.keios.ui.page.main.ba.BaCalendarCard
import com.example.keios.ui.page.main.ba.BaDebugCard
import com.example.keios.ui.page.main.ba.BaIdCard
import com.example.keios.ui.page.main.ba.BaOverviewCard
import com.example.keios.ui.page.main.ba.BaPoolCard
import com.example.keios.ui.page.main.ba.BaPageCommonEffects
import com.example.keios.ui.page.main.ba.BaPageContent
import com.example.keios.ui.page.main.ba.BaPageContentActions
import com.example.keios.ui.page.main.ba.BaPageContentState
import com.example.keios.ui.page.main.ba.BaCalendarSyncEffect
import com.example.keios.ui.page.main.ba.rememberBaOfficeController
import com.example.keios.ui.page.main.ba.hasAnyImageInBaCalendarCache
import com.example.keios.ui.page.main.ba.hasAnyImageInBaPoolCache
import com.example.keios.ui.page.main.ba.openBaExternalLink
import com.example.keios.ui.page.main.ba.persistBaSettingsDraft
import com.example.keios.ui.page.main.ba.BaPoolSyncEffect
import com.example.keios.ui.page.main.ba.BaSettingsSheet
import com.example.keios.ui.page.main.ba.BaSettingsSheetState
import com.example.keios.ui.page.main.ba.BaTopBar
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt


@Composable
fun BAPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    cardPressFeedbackEnabled: Boolean = true,
    onOpenPoolStudentGuide: (String) -> Unit = {},
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val baCardShape = RoundedCornerShape(16.dp)
    val baCardBaseColor = if (isDark) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.44f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.70f)
    }
    val baCardBorderColor = if (isDark) {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.22f)
    } else {
        MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.12f)
    }
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
            val baSmallTitleMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    val serverOptions = remember { listOf("国服", "国际服", "日服") }
    val cafeLevelOptions = remember { (1..10).toList() }

    var initState by remember { mutableStateOf(BAInitState.Empty) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showOverviewServerPopup by remember { mutableStateOf(false) }
    var showCafeLevelPopup by remember { mutableStateOf(false) }
    var overviewServerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var cafeLevelPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var showCalendarIntervalPopup by remember { mutableStateOf(false) }

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

    var serverIndex by remember { mutableIntStateOf(initialSnapshot.serverIndex) }
    var uiNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var baCalendarEntries by remember { mutableStateOf(emptyList<BaCalendarEntry>()) }
    var baCalendarLoading by remember { mutableStateOf(true) }
    var baCalendarError by remember { mutableStateOf<String?>(null) }
    var baCalendarLastSyncMs by remember { mutableLongStateOf(0L) }
    var baCalendarReloadSignal by remember { mutableIntStateOf(0) }
    var baPoolEntries by remember { mutableStateOf(emptyList<BaPoolEntry>()) }
    var baPoolLoading by remember { mutableStateOf(true) }
    var baPoolError by remember { mutableStateOf<String?>(null) }
    var baPoolLastSyncMs by remember { mutableLongStateOf(0L) }
    var baPoolReloadSignal by remember { mutableIntStateOf(0) }
    var showEndedPools by remember { mutableStateOf(initialSnapshot.showEndedPools) }
    var showEndedActivities by remember { mutableStateOf(initialSnapshot.showEndedActivities) }
    var showCalendarPoolImages by remember { mutableStateOf(initialSnapshot.showCalendarPoolImages) }
    var calendarRefreshIntervalHours by remember {
        mutableIntStateOf(initialSnapshot.calendarRefreshIntervalHours)
    }
    var calendarHydrationReady by remember { mutableStateOf(false) }
    var poolHydrationReady by remember { mutableStateOf(false) }

    var sheetCafeLevel by remember { mutableIntStateOf(office.cafeLevel) }
    var sheetApNotifyEnabled by remember { mutableStateOf(office.apNotifyEnabled) }
    var sheetApNotifyThresholdText by remember { mutableStateOf(office.apNotifyThreshold.toString()) }
    var sheetShowEndedPools by remember { mutableStateOf(showEndedPools) }
    var sheetShowEndedActivities by remember { mutableStateOf(showEndedActivities) }
    var sheetShowCalendarPoolImages by remember { mutableStateOf(showCalendarPoolImages) }

    var glassButtonPressCount by remember { mutableIntStateOf(0) }
    var consumedScrollToTopSignal by remember { mutableIntStateOf(scrollToTopSignal) }
    val officeSmallTitle = when (serverIndex) {
        0 -> "沙勒办公室"
        1 -> "夏萊行政室"
        else -> "夏莱办公室"
    }
    val disableCardFeedback = glassButtonPressCount > 0 || !cardPressFeedbackEnabled
    val onGlassButtonPressedChange: (Boolean) -> Unit = { pressed ->
        glassButtonPressCount = if (pressed) {
            glassButtonPressCount + 1
        } else {
            (glassButtonPressCount - 1).coerceAtLeast(0)
        }
    }
    val settingsSheetState = BaSettingsSheetState(
        cafeLevel = sheetCafeLevel,
        apNotifyEnabled = sheetApNotifyEnabled,
        apNotifyThresholdText = sheetApNotifyThresholdText,
        showEndedActivities = sheetShowEndedActivities,
        showEndedPools = sheetShowEndedPools,
        showCalendarPoolImages = sheetShowCalendarPoolImages,
    )
    val pageContentState = BaPageContentState(
        isDark = isDark,
        officeSmallTitle = officeSmallTitle,
        baSmallTitleMargin = baSmallTitleMargin,
        baCardShape = baCardShape,
        baCardBaseColor = baCardBaseColor,
        baCardBorderColor = baCardBorderColor,
        officeState = office.state(),
        uiNowMs = uiNowMs,
        serverOptions = serverOptions,
        serverIndex = serverIndex,
        showOverviewServerPopup = showOverviewServerPopup,
        overviewServerPopupAnchorBounds = overviewServerPopupAnchorBounds,
        initState = initState,
        disableCardFeedback = disableCardFeedback,
        baCalendarEntries = baCalendarEntries,
        baCalendarLoading = baCalendarLoading,
        baCalendarError = baCalendarError,
        baCalendarLastSyncMs = baCalendarLastSyncMs,
        showEndedActivities = showEndedActivities,
        showCalendarPoolImages = showCalendarPoolImages,
        baPoolEntries = baPoolEntries,
        baPoolLoading = baPoolLoading,
        baPoolError = baPoolError,
        baPoolLastSyncMs = baPoolLastSyncMs,
        showEndedPools = showEndedPools,
    )



    fun openSettingsSheet() {
        showOverviewServerPopup = false
        showCafeLevelPopup = false
        sheetCafeLevel = office.cafeLevel
        sheetApNotifyEnabled = office.apNotifyEnabled
        sheetApNotifyThresholdText = office.apNotifyThreshold.toString()
        sheetShowEndedPools = showEndedPools
        sheetShowEndedActivities = showEndedActivities
        sheetShowCalendarPoolImages = showCalendarPoolImages
        showSettingsSheet = true
    }

    fun closeSettingsSheet() {
        showSettingsSheet = false
        showCafeLevelPopup = false
        sheetCafeLevel = office.cafeLevel
        sheetApNotifyEnabled = office.apNotifyEnabled
        sheetApNotifyThresholdText = office.apNotifyThreshold.toString()
        sheetShowEndedPools = showEndedPools
        sheetShowEndedActivities = showEndedActivities
        sheetShowCalendarPoolImages = showCalendarPoolImages
    }

    fun hasAnyImageInCalendarCache(serverIdx: Int): Boolean = hasAnyImageInBaCalendarCache(serverIdx)

    fun hasAnyImageInPoolCache(serverIdx: Int): Boolean = hasAnyImageInBaPoolCache(serverIdx)

    fun openCalendarLink(url: String) {
        openBaExternalLink(context = context, url = url)
    }

    fun refreshCalendar(force: Boolean = false) {
        if (force) baCalendarReloadSignal += 1
    }

    fun refreshPool(force: Boolean = false) {
        if (force) baPoolReloadSignal += 1
    }

    fun saveSettings() {
        office.applyCafeStorage()

        val persisted = persistBaSettingsDraft(
            sheetState = settingsSheetState,
            currentCafeLevel = office.cafeLevel,
            currentShowEndedActivities = showEndedActivities,
            currentShowCalendarPoolImages = showCalendarPoolImages,
        )

        office.cafeLevel = persisted.savedCafeLevel
        office.clampCafeStoredToCap()
        office.apNotifyEnabled = settingsSheetState.apNotifyEnabled
        office.apNotifyThreshold = persisted.savedThreshold
        showEndedPools = persisted.showEndedPools
        showEndedActivities = persisted.showEndedActivities
        showCalendarPoolImages = persisted.showCalendarPoolImages

        if (persisted.turningEndedActivitiesOn) {
            val (calendarCacheRaw, _) = BASettingsStore.loadCalendarCache(serverIndex)
            if (calendarCacheRaw.isBlank()) refreshCalendar(force = true)
        }

        if (persisted.turningImagesOn) {
            val calendarHasImage = hasAnyImageInCalendarCache(serverIndex)
            val poolHasImage = hasAnyImageInPoolCache(serverIndex)
            if (!calendarHasImage) refreshCalendar(force = true)
            if (!poolHasImage) refreshPool(force = true)
        }

        office.applyApRegen()
        showSettingsSheet = false
        showCafeLevelPopup = false
    }
    val pageContentActions = BaPageContentActions(
        onApCurrentInputChange = { office.apCurrentInput = it },
        onApCurrentDone = {
            val finalValue = office.apCurrentInput.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 0
            office.updateCurrentAp(finalValue, markSync = true)
            office.apCurrentInput = finalValue.toString()
        },
        onApLimitInputChange = { office.apLimitInput = it },
        onApLimitDone = {
            val finalValue = office.apLimitInput.toIntOrNull()?.coerceIn(0, BA_AP_LIMIT_MAX) ?: BA_AP_LIMIT_MAX
            office.updateApLimit(finalValue)
            office.applyApRegen()
            office.apLimitInput = finalValue.toString()
        },
        onOverviewServerPopupAnchorBoundsChange = { overviewServerPopupAnchorBounds = it },
        onOverviewServerPopupChange = { showOverviewServerPopup = it },
        onServerSelected = { selected: Int ->
            serverIndex = selected
            BASettingsStore.saveServerIndex(selected)
            refreshCalendar(force = true)
            refreshPool(force = true)
            showOverviewServerPopup = false
        },
        onClaimCafeStoredAp = { office.claimCafeStoredAp(context) },
        onInitStateChange = { initState = it },
        onTouchHead = { office.touchHead(serverIndex) },
        onForceResetHeadpatCooldown = { office.forceResetHeadpatCooldown() },
        onUseInviteTicket1 = { office.useInviteTicket1() },
        onForceResetInviteTicket1Cooldown = { office.forceResetInviteTicket1Cooldown() },
        onUseInviteTicket2 = { office.useInviteTicket2() },
        onForceResetInviteTicket2Cooldown = { office.forceResetInviteTicket2Cooldown() },
        onGlassButtonPressedChange = onGlassButtonPressedChange,
        onRefreshCalendar = { refreshCalendar(force = true) },
        onOpenCalendarLink = { openCalendarLink(it) },
        onRefreshPool = { refreshPool(force = true) },
        onOpenPoolStudentGuide = onOpenPoolStudentGuide,
        onIdNicknameInputChange = { office.idNicknameInput = it },
        onSaveIdNickname = { office.saveIdNicknameFromInput() },
        onIdFriendCodeInputChange = { office.idFriendCodeInput = it },
        onSaveIdFriendCode = { office.saveIdFriendCodeFromInput(context) },
        onSendApTestNotification = { office.sendApTestNotification(context = context, showToast = true) },
        onTestCafePlus3Hours = { office.testCafePlus3Hours(context) },
    )


    BaPageCommonEffects(
        listState = listState,
        scrollToTopSignal = scrollToTopSignal,
        consumedScrollToTopSignal = consumedScrollToTopSignal,
        onConsumedScrollToTopSignalChange = { consumedScrollToTopSignal = it },
        onDisposeActionBarInteraction = { onActionBarInteractingChanged(false) },
        office = office,
        onUiNowMsChange = { uiNowMs = it },
        serverIndex = serverIndex,
        onServerChanged = {
            baCalendarLoading = true
            baPoolLoading = true
            baCalendarError = null
            baPoolError = null
            calendarHydrationReady = false
            poolHydrationReady = false
            calendarHydrationReady = true
            delay(96)
            poolHydrationReady = true
        },
        context = context,
    )

    BaCalendarSyncEffect(

        context = context,
        serverIndex = serverIndex,
        reloadSignal = baCalendarReloadSignal,
        calendarRefreshIntervalHours = calendarRefreshIntervalHours,
        hydrationReady = calendarHydrationReady,
        onLoadingChange = { baCalendarLoading = it },
        onErrorChange = { baCalendarError = it },
        onEntriesChange = { baCalendarEntries = it },
        onLastSyncMsChange = { baCalendarLastSyncMs = it },
    )

    BaPoolSyncEffect(
        context = context,
        serverIndex = serverIndex,
        reloadSignal = baPoolReloadSignal,
        calendarRefreshIntervalHours = calendarRefreshIntervalHours,
        hydrationReady = poolHydrationReady,
        onLoadingChange = { baPoolLoading = it },
        onErrorChange = { baPoolError = it },
        onEntriesChange = { baPoolEntries = it },
        onLastSyncMsChange = { baPoolLastSyncMs = it },
    )

    Scaffold(

        modifier = Modifier.fillMaxSize(),
        topBar = {
            BaTopBar(
                backdrop = backdrop,
                topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
                scrollBehavior = scrollBehavior,
                showCalendarIntervalPopup = showCalendarIntervalPopup,
                calendarRefreshIntervalHours = calendarRefreshIntervalHours,
                onShowSettings = { openSettingsSheet() },
                onShowCalendarIntervalPopupChange = { showCalendarIntervalPopup = it },
                onCopyFriendCode = { office.copyFriendCodeToClipboard(context) },
                onRefreshAll = {
                    office.applyCafeStorage()
                    office.applyApRegen()
                    refreshCalendar(force = true)
                    refreshPool(force = true)
                },
                onCalendarRefreshIntervalSelected = { hours ->
                    calendarRefreshIntervalHours = hours
                    BASettingsStore.saveCalendarRefreshIntervalHours(hours)
                    val elapsed = (
                        System.currentTimeMillis() - baCalendarLastSyncMs
                    ).coerceAtLeast(0L)
                    if (baCalendarLastSyncMs <= 0L || elapsed >= hours * 60L * 60L * 1000L) {
                        refreshCalendar(force = true)
                        refreshPool(force = true)
                    }
                },
                onInteractionChanged = onActionBarInteractingChanged,
            )
        },
    ) { innerPadding ->
        BaPageContent(
            backdrop = backdrop,
            innerPadding = innerPadding,
            contentBottomPadding = contentBottomPadding,
            listState = listState,
            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            state = pageContentState,
            actions = pageContentActions,
        )
    }

    BaSettingsSheet(

        show = showSettingsSheet,
        backdrop = backdrop,
        state = settingsSheetState,
        cafeLevelOptions = cafeLevelOptions,
        showCafeLevelPopup = showCafeLevelPopup,
        cafeLevelPopupAnchorBounds = cafeLevelPopupAnchorBounds,
        onCafeLevelPopupAnchorBoundsChange = { cafeLevelPopupAnchorBounds = it },
        onShowCafeLevelPopupChange = { showCafeLevelPopup = it },
        onCafeLevelChange = { sheetCafeLevel = it },
        onApNotifyEnabledChange = { sheetApNotifyEnabled = it },
        onApNotifyThresholdTextChange = { sheetApNotifyThresholdText = it },
        onApNotifyThresholdDone = {
            val normalized = sheetApNotifyThresholdText.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 120
            sheetApNotifyThresholdText = normalized.toString()
        },
        onShowEndedActivitiesChange = { sheetShowEndedActivities = it },
        onShowEndedPoolsChange = { sheetShowEndedPools = it },
        onShowCalendarPoolImagesChange = { sheetShowCalendarPoolImages = it },
        onDismissRequest = { closeSettingsSheet() },
        onSaveRequest = { saveSettings() },
    )
}
