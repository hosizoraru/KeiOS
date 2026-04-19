package com.example.keios.ui.page.main.ba

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect

@Stable
internal data class BaPageUiState(
    val showSettingsSheet: Boolean,
    val showOverviewServerPopup: Boolean,
    val showCafeLevelPopup: Boolean,
    val overviewServerPopupAnchorBounds: IntRect?,
    val cafeLevelPopupAnchorBounds: IntRect?,
    val showCalendarIntervalPopup: Boolean,
    val initState: BAInitState,
    val serverIndex: Int,
    val uiNowMs: Long,
    val baCalendarLoading: Boolean,
    val baCalendarError: String?,
    val baCalendarLastSyncMs: Long,
    val baCalendarReloadSignal: Int,
    val baPoolLoading: Boolean,
    val baPoolError: String?,
    val baPoolLastSyncMs: Long,
    val baPoolReloadSignal: Int,
    val showEndedPools: Boolean,
    val showEndedActivities: Boolean,
    val showCalendarPoolImages: Boolean,
    val mediaAdaptiveRotationEnabled: Boolean,
    val mediaSaveCustomEnabled: Boolean,
    val mediaSaveFixedTreeUri: String,
    val calendarRefreshIntervalHours: Int,
    val calendarHydrationReady: Boolean,
    val poolHydrationReady: Boolean,
    val sheetCafeLevel: Int,
    val sheetApNotifyEnabled: Boolean,
    val sheetCafeVisitNotifyEnabled: Boolean,
    val sheetApNotifyThresholdText: String,
    val sheetMediaAdaptiveRotationEnabled: Boolean,
    val sheetMediaSaveCustomEnabled: Boolean,
    val sheetMediaSaveFixedTreeUri: String,
    val sheetShowEndedPools: Boolean,
    val sheetShowEndedActivities: Boolean,
    val sheetShowCalendarPoolImages: Boolean,
    val consumedScrollToTopSignal: Int,
)


internal class BaPageUiController(snapshot: BaPageSnapshot) {
    var showSettingsSheet by mutableStateOf(false)
    var showOverviewServerPopup by mutableStateOf(false)
    var showCafeLevelPopup by mutableStateOf(false)
    var overviewServerPopupAnchorBounds by mutableStateOf<IntRect?>(null)
    var cafeLevelPopupAnchorBounds by mutableStateOf<IntRect?>(null)
    var showCalendarIntervalPopup by mutableStateOf(false)
    var initState by mutableStateOf(BAInitState.Empty)
    var serverIndex by mutableIntStateOf(snapshot.serverIndex)
    var uiNowMs by mutableLongStateOf(System.currentTimeMillis())
    var baCalendarLoading by mutableStateOf(true)
    var baCalendarError by mutableStateOf<String?>(null)
    var baCalendarLastSyncMs by mutableLongStateOf(0L)
    var baCalendarReloadSignal by mutableIntStateOf(0)
    var baPoolLoading by mutableStateOf(true)
    var baPoolError by mutableStateOf<String?>(null)
    var baPoolLastSyncMs by mutableLongStateOf(0L)
    var baPoolReloadSignal by mutableIntStateOf(0)
    var showEndedPools by mutableStateOf(snapshot.showEndedPools)
    var showEndedActivities by mutableStateOf(snapshot.showEndedActivities)
    var showCalendarPoolImages by mutableStateOf(snapshot.showCalendarPoolImages)
    var mediaAdaptiveRotationEnabled by mutableStateOf(snapshot.mediaAdaptiveRotationEnabled)
    var mediaSaveCustomEnabled by mutableStateOf(snapshot.mediaSaveCustomEnabled)
    var mediaSaveFixedTreeUri by mutableStateOf(snapshot.mediaSaveFixedTreeUri)
    var calendarRefreshIntervalHours by mutableIntStateOf(snapshot.calendarRefreshIntervalHours)
    var calendarHydrationReady by mutableStateOf(false)
    var poolHydrationReady by mutableStateOf(false)
    var sheetCafeLevel by mutableIntStateOf(snapshot.cafeLevel)
    var sheetApNotifyEnabled by mutableStateOf(snapshot.apNotifyEnabled)
    var sheetCafeVisitNotifyEnabled by mutableStateOf(snapshot.cafeVisitNotifyEnabled)
    var sheetApNotifyThresholdText by mutableStateOf(snapshot.apNotifyThreshold.toString())
    var sheetMediaAdaptiveRotationEnabled by mutableStateOf(snapshot.mediaAdaptiveRotationEnabled)
    var sheetMediaSaveCustomEnabled by mutableStateOf(snapshot.mediaSaveCustomEnabled)
    var sheetMediaSaveFixedTreeUri by mutableStateOf(snapshot.mediaSaveFixedTreeUri)
    var sheetShowEndedPools by mutableStateOf(snapshot.showEndedPools)
    var sheetShowEndedActivities by mutableStateOf(snapshot.showEndedActivities)
    var sheetShowCalendarPoolImages by mutableStateOf(snapshot.showCalendarPoolImages)
    var consumedScrollToTopSignal by mutableIntStateOf(0)

    fun state(): BaPageUiState {
        return BaPageUiState(
            showSettingsSheet = showSettingsSheet,
            showOverviewServerPopup = showOverviewServerPopup,
            showCafeLevelPopup = showCafeLevelPopup,
            overviewServerPopupAnchorBounds = overviewServerPopupAnchorBounds,
            cafeLevelPopupAnchorBounds = cafeLevelPopupAnchorBounds,
            showCalendarIntervalPopup = showCalendarIntervalPopup,
            initState = initState,
            serverIndex = serverIndex,
            uiNowMs = uiNowMs,
            baCalendarLoading = baCalendarLoading,
            baCalendarError = baCalendarError,
            baCalendarLastSyncMs = baCalendarLastSyncMs,
            baCalendarReloadSignal = baCalendarReloadSignal,
            baPoolLoading = baPoolLoading,
            baPoolError = baPoolError,
            baPoolLastSyncMs = baPoolLastSyncMs,
            baPoolReloadSignal = baPoolReloadSignal,
            showEndedPools = showEndedPools,
            showEndedActivities = showEndedActivities,
            showCalendarPoolImages = showCalendarPoolImages,
            mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
            mediaSaveCustomEnabled = mediaSaveCustomEnabled,
            mediaSaveFixedTreeUri = mediaSaveFixedTreeUri,
            calendarRefreshIntervalHours = calendarRefreshIntervalHours,
            calendarHydrationReady = calendarHydrationReady,
            poolHydrationReady = poolHydrationReady,
            sheetCafeLevel = sheetCafeLevel,
            sheetApNotifyEnabled = sheetApNotifyEnabled,
            sheetCafeVisitNotifyEnabled = sheetCafeVisitNotifyEnabled,
            sheetApNotifyThresholdText = sheetApNotifyThresholdText,
            sheetMediaAdaptiveRotationEnabled = sheetMediaAdaptiveRotationEnabled,
            sheetMediaSaveCustomEnabled = sheetMediaSaveCustomEnabled,
            sheetMediaSaveFixedTreeUri = sheetMediaSaveFixedTreeUri,
            sheetShowEndedPools = sheetShowEndedPools,
            sheetShowEndedActivities = sheetShowEndedActivities,
            sheetShowCalendarPoolImages = sheetShowCalendarPoolImages,
            consumedScrollToTopSignal = consumedScrollToTopSignal,
        )
    }

    fun refreshCalendar(force: Boolean = false) {
        if (force) baCalendarReloadSignal += 1
    }

    fun refreshPool(force: Boolean = false) {
        if (force) baPoolReloadSignal += 1
    }

    fun openSettingsSheet(office: BaOfficeController) {
        showOverviewServerPopup = false
        showCafeLevelPopup = false
        sheetCafeLevel = office.cafeLevel
        sheetApNotifyEnabled = office.apNotifyEnabled
        sheetCafeVisitNotifyEnabled = office.cafeVisitNotifyEnabled
        sheetApNotifyThresholdText = office.apNotifyThreshold.toString()
        sheetMediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled
        sheetMediaSaveCustomEnabled = mediaSaveCustomEnabled
        sheetMediaSaveFixedTreeUri = mediaSaveFixedTreeUri
        sheetShowEndedPools = showEndedPools
        sheetShowEndedActivities = showEndedActivities
        sheetShowCalendarPoolImages = showCalendarPoolImages
        showSettingsSheet = true
    }

    fun closeSettingsSheet(office: BaOfficeController) {
        showSettingsSheet = false
        showCafeLevelPopup = false
        sheetCafeLevel = office.cafeLevel
        sheetApNotifyEnabled = office.apNotifyEnabled
        sheetCafeVisitNotifyEnabled = office.cafeVisitNotifyEnabled
        sheetApNotifyThresholdText = office.apNotifyThreshold.toString()
        sheetMediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled
        sheetMediaSaveCustomEnabled = mediaSaveCustomEnabled
        sheetMediaSaveFixedTreeUri = mediaSaveFixedTreeUri
        sheetShowEndedPools = showEndedPools
        sheetShowEndedActivities = showEndedActivities
        sheetShowCalendarPoolImages = showCalendarPoolImages
    }
}

@Composable
internal fun rememberBaPageUiController(snapshot: BaPageSnapshot): BaPageUiController {
    return remember(snapshot) { BaPageUiController(snapshot) }
}
