package os.kei.ui.page.main.ba

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import os.kei.core.background.AppBackgroundScheduler
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BA_AP_LIMIT_MAX
import os.kei.ui.page.main.ba.support.BA_AP_MAX
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaPoolEntry
import os.kei.ui.page.main.ba.support.currentArenaRefreshSlotMs
import os.kei.ui.page.main.ba.support.currentCafeStudentRefreshSlotMs

internal fun buildBaSettingsSheetState(
    ui: BaPageUiController,
): BaSettingsSheetState {
    return BaSettingsSheetState(
        cafeLevel = ui.sheetCafeLevel,
        apNotifyEnabled = ui.sheetApNotifyEnabled,
        arenaRefreshNotifyEnabled = ui.sheetArenaRefreshNotifyEnabled,
        cafeVisitNotifyEnabled = ui.sheetCafeVisitNotifyEnabled,
        apNotifyThresholdText = ui.sheetApNotifyThresholdText,
        mediaAdaptiveRotationEnabled = ui.sheetMediaAdaptiveRotationEnabled,
        mediaSaveCustomEnabled = ui.sheetMediaSaveCustomEnabled,
        mediaSaveFixedTreeUri = ui.sheetMediaSaveFixedTreeUri,
        showEndedActivities = ui.sheetShowEndedActivities,
        showEndedPools = ui.sheetShowEndedPools,
        showCalendarPoolImages = ui.sheetShowCalendarPoolImages,
    )
}

internal fun buildBaPageContentState(
    isPageActive: Boolean,
    officeSmallTitle: String,
    baSmallTitleMargin: PaddingValues,
    office: BaOfficeController,
    ui: BaPageUiController,
    serverOptions: List<String>,
    cafeLevelOptions: List<Int>,
    baCalendarEntries: List<BaCalendarEntry>,
    baPoolEntries: List<BaPoolEntry>,
): BaPageContentState {
    return BaPageContentState(
        isPageActive = isPageActive,
        officeSmallTitle = officeSmallTitle,
        baSmallTitleMargin = baSmallTitleMargin,
        officeState = office.state(),
        uiNowMs = ui.uiNowMs,
        serverOptions = serverOptions,
        cafeLevelOptions = cafeLevelOptions,
        serverIndex = ui.serverIndex,
        showOverviewServerPopup = ui.showOverviewServerPopup,
        showCafeLevelPopup = ui.showCafeLevelPopup,
        overviewServerPopupAnchorBounds = ui.overviewServerPopupAnchorBounds,
        cafeLevelPopupAnchorBounds = ui.cafeLevelPopupAnchorBounds,
        initState = ui.initState,
        baCalendarEntries = baCalendarEntries,
        baCalendarLoading = ui.baCalendarLoading,
        baCalendarError = ui.baCalendarError,
        baCalendarLastSyncMs = ui.baCalendarLastSyncMs,
        showEndedActivities = ui.showEndedActivities,
        showCalendarPoolImages = ui.showCalendarPoolImages,
        baPoolEntries = baPoolEntries,
        baPoolLoading = ui.baPoolLoading,
        baPoolError = ui.baPoolError,
        baPoolLastSyncMs = ui.baPoolLastSyncMs,
        showEndedPools = ui.showEndedPools,
    )
}

internal fun saveBaPageSettings(
    context: Context,
    office: BaOfficeController,
    ui: BaPageUiController,
    settingsSheetState: BaSettingsSheetState,
    onRefreshCalendar: (Boolean) -> Unit,
    onRefreshPool: (Boolean) -> Unit,
) {
    office.applyCafeStorage()

    val persisted = persistBaSettingsDraft(
        sheetState = settingsSheetState,
        currentCafeLevel = office.cafeLevel,
        currentShowEndedActivities = ui.showEndedActivities,
        currentShowCalendarPoolImages = ui.showCalendarPoolImages,
    )

    office.cafeLevel = persisted.savedCafeLevel
    office.clampCafeStoredToCap()
    val previousArenaRefreshNotifyEnabled = office.arenaRefreshNotifyEnabled
    val previousCafeVisitNotifyEnabled = office.cafeVisitNotifyEnabled
    office.apNotifyEnabled = settingsSheetState.apNotifyEnabled
    office.arenaRefreshNotifyEnabled = persisted.arenaRefreshNotifyEnabled
    office.cafeVisitNotifyEnabled = persisted.cafeVisitNotifyEnabled
    office.apNotifyThreshold = persisted.savedThreshold
    if (!office.arenaRefreshNotifyEnabled) {
        office.arenaRefreshLastNotifiedSlotMs = 0L
        BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(0L)
    } else if (!previousArenaRefreshNotifyEnabled) {
        val baselineSlotMs = currentArenaRefreshSlotMs(
            nowMs = System.currentTimeMillis(),
            serverIndex = ui.serverIndex
        )
        office.arenaRefreshLastNotifiedSlotMs = baselineSlotMs
        BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(baselineSlotMs)
    }
    if (!office.cafeVisitNotifyEnabled) {
        office.cafeVisitLastNotifiedSlotMs = 0L
        BASettingsStore.saveCafeVisitLastNotifiedSlotMs(0L)
    } else if (!previousCafeVisitNotifyEnabled) {
        val baselineSlotMs = currentCafeStudentRefreshSlotMs(
            nowMs = System.currentTimeMillis(),
            serverIndex = ui.serverIndex
        )
        office.cafeVisitLastNotifiedSlotMs = baselineSlotMs
        BASettingsStore.saveCafeVisitLastNotifiedSlotMs(baselineSlotMs)
    }
    ui.showEndedPools = persisted.showEndedPools
    ui.showEndedActivities = persisted.showEndedActivities
    ui.showCalendarPoolImages = persisted.showCalendarPoolImages
    ui.mediaAdaptiveRotationEnabled = persisted.mediaAdaptiveRotationEnabled
    ui.mediaSaveCustomEnabled = persisted.mediaSaveCustomEnabled
    ui.mediaSaveFixedTreeUri = persisted.mediaSaveFixedTreeUri

    if (persisted.turningEndedActivitiesOn) {
        val (calendarCacheRaw, _) = BASettingsStore.loadCalendarCache(ui.serverIndex)
        if (calendarCacheRaw.isBlank()) onRefreshCalendar(true)
    }

    if (persisted.turningImagesOn) {
        val calendarHasImage = hasAnyImageInBaCalendarCache(ui.serverIndex)
        val poolHasImage = hasAnyImageInBaPoolCache(ui.serverIndex)
        if (!calendarHasImage) onRefreshCalendar(true)
        if (!poolHasImage) onRefreshPool(true)
    }

    office.applyApRegen()
    AppBackgroundScheduler.scheduleBaApThreshold(context)
    ui.closeSettingsSheet(office)
}

internal fun buildBaPageContentActions(
    context: Context,
    office: BaOfficeController,
    ui: BaPageUiController,
    onRefreshCalendar: () -> Unit,
    onRefreshPool: () -> Unit,
    onOpenCalendarLink: (String) -> Unit,
    onOpenPoolStudentGuide: (String) -> Unit,
    onOpenGuideCatalog: () -> Unit,
): BaPageContentActions {
    return BaPageContentActions(
        onApCurrentInputChange = { office.apCurrentInput = it },
        onApCurrentDone = {
            val finalValue = office.apCurrentInput.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 0
            office.updateCurrentAp(finalValue, markSync = true)
            office.apCurrentInput = finalValue.toString()
        },
        onApLimitInputChange = { office.apLimitInput = it },
        onApLimitDone = {
            val finalValue = office.apLimitInput.toIntOrNull()?.coerceIn(0, BA_AP_LIMIT_MAX)
                ?: BA_AP_LIMIT_MAX
            office.updateApLimit(finalValue)
            office.applyApRegen()
            office.apLimitInput = finalValue.toString()
        },
        onOverviewServerPopupAnchorBoundsChange = { ui.overviewServerPopupAnchorBounds = it },
        onOverviewServerPopupChange = { ui.showOverviewServerPopup = it },
        onCafeLevelPopupAnchorBoundsChange = { ui.cafeLevelPopupAnchorBounds = it },
        onCafeLevelPopupChange = { ui.showCafeLevelPopup = it },
        onCafeLevelChange = { level ->
            val normalized = level.coerceIn(1, 10)
            office.applyCafeStorage()
            office.cafeLevel = normalized
            office.clampCafeStoredToCap()
            BASettingsStore.saveCafeLevel(normalized)
            ui.sheetCafeLevel = normalized
            ui.showCafeLevelPopup = false
        },
        onServerSelected = { selected ->
            ui.serverIndex = selected
            BASettingsStore.saveServerIndex(selected)
            if (office.cafeVisitNotifyEnabled) {
                val baselineSlotMs = currentCafeStudentRefreshSlotMs(
                    nowMs = System.currentTimeMillis(),
                    serverIndex = selected
                )
                office.cafeVisitLastNotifiedSlotMs = baselineSlotMs
                BASettingsStore.saveCafeVisitLastNotifiedSlotMs(baselineSlotMs)
            }
            if (office.arenaRefreshNotifyEnabled) {
                val baselineSlotMs = currentArenaRefreshSlotMs(
                    nowMs = System.currentTimeMillis(),
                    serverIndex = selected
                )
                office.arenaRefreshLastNotifiedSlotMs = baselineSlotMs
                BASettingsStore.saveArenaRefreshLastNotifiedSlotMs(baselineSlotMs)
            }
            onRefreshCalendar()
            onRefreshPool()
            ui.showOverviewServerPopup = false
        },
        onClaimCafeStoredAp = { office.claimCafeStoredAp(context) },
        onInitStateChange = { ui.initState = it },
        onTouchHead = { office.touchHead(ui.serverIndex) },
        onForceResetHeadpatCooldown = { office.forceResetHeadpatCooldown() },
        onUseInviteTicket1 = { office.useInviteTicket1() },
        onForceResetInviteTicket1Cooldown = { office.forceResetInviteTicket1Cooldown() },
        onUseInviteTicket2 = { office.useInviteTicket2() },
        onForceResetInviteTicket2Cooldown = { office.forceResetInviteTicket2Cooldown() },
        onRefreshCalendar = onRefreshCalendar,
        onOpenCalendarLink = onOpenCalendarLink,
        onRefreshPool = onRefreshPool,
        onOpenPoolStudentGuide = onOpenPoolStudentGuide,
        onOpenGuideCatalog = onOpenGuideCatalog,
        onIdNicknameInputChange = { office.idNicknameInput = it },
        onSaveIdNickname = { office.saveIdNicknameFromInput() },
        onIdFriendCodeInputChange = { office.idFriendCodeInput = it },
        onSaveIdFriendCode = { office.saveIdFriendCodeFromInput(context) },
        onSendApTestNotification = {
            office.sendApTestNotification(context = context, showToast = true)
        },
        onSendCafeVisitTestNotification = {
            office.sendCafeVisitTestNotification(
                context = context,
                serverIndex = ui.serverIndex,
                showToast = true
            )
        },
        onSendArenaRefreshTestNotification = {
            office.sendArenaRefreshTestNotification(
                context = context,
                serverIndex = ui.serverIndex,
                showToast = true
            )
        },
        onTestCafePlus3Hours = { office.testCafePlus3Hours(context) },
    )
}

internal fun applyBaCalendarRefreshInterval(
    ui: BaPageUiController,
    hours: Int,
    onRefreshCalendar: () -> Unit,
    onRefreshPool: () -> Unit,
) {
    ui.calendarRefreshIntervalHours = hours
    BASettingsStore.saveCalendarRefreshIntervalHours(hours)
    val elapsed = (System.currentTimeMillis() - ui.baCalendarLastSyncMs).coerceAtLeast(0L)
    if (ui.baCalendarLastSyncMs <= 0L || elapsed >= hours * 60L * 60L * 1000L) {
        onRefreshCalendar()
        onRefreshPool()
    }
}
