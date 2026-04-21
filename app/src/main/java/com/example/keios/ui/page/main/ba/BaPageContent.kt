package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.ba.card.BaCafeCard
import com.example.keios.ui.page.main.ba.card.BaCalendarCard
import com.example.keios.ui.page.main.ba.card.BaDebugCard
import com.example.keios.ui.page.main.ba.card.BaIdCard
import com.example.keios.ui.page.main.ba.card.BaOverviewCard
import com.example.keios.ui.page.main.ba.card.BaPoolCard
import com.example.keios.ui.page.main.ba.card.BaPoolNotesCard
import com.example.keios.ui.page.main.ba.card.filterVisibleCalendarEntries
import com.example.keios.ui.page.main.ba.card.filterVisiblePoolNoteEntries
import com.example.keios.ui.page.main.ba.card.filterVisiblePoolEntries
import com.example.keios.ui.page.main.ba.support.BAInitState
import com.example.keios.ui.page.main.ba.support.BaCalendarEntry
import com.example.keios.ui.page.main.ba.support.BaPoolEntry
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.SmallTitle

internal data class BaPageContentState(
    val isPageActive: Boolean,
    val officeSmallTitle: String,
    val baSmallTitleMargin: PaddingValues,
    val officeState: BaOfficeState,
    val uiNowMs: Long,
    val serverOptions: List<String>,
    val cafeLevelOptions: List<Int>,
    val serverIndex: Int,
    val showOverviewServerPopup: Boolean,
    val showCafeLevelPopup: Boolean,
    val overviewServerPopupAnchorBounds: IntRect?,
    val cafeLevelPopupAnchorBounds: IntRect?,
    val initState: BAInitState,
    val baCalendarEntries: List<BaCalendarEntry>,
    val baCalendarLoading: Boolean,
    val baCalendarError: String?,
    val baCalendarLastSyncMs: Long,
    val showEndedActivities: Boolean,
    val showCalendarPoolImages: Boolean,
    val baPoolEntries: List<BaPoolEntry>,
    val baPoolLoading: Boolean,
    val baPoolError: String?,
    val baPoolLastSyncMs: Long,
    val showEndedPools: Boolean,
)

internal data class BaPageContentActions(
    val onApCurrentInputChange: (String) -> Unit,
    val onApCurrentDone: () -> Unit,
    val onApLimitInputChange: (String) -> Unit,
    val onApLimitDone: () -> Unit,
    val onOverviewServerPopupAnchorBoundsChange: (IntRect?) -> Unit,
    val onOverviewServerPopupChange: (Boolean) -> Unit,
    val onCafeLevelPopupAnchorBoundsChange: (IntRect?) -> Unit,
    val onCafeLevelPopupChange: (Boolean) -> Unit,
    val onCafeLevelChange: (Int) -> Unit,
    val onServerSelected: (Int) -> Unit,
    val onClaimCafeStoredAp: () -> Unit,
    val onInitStateChange: (BAInitState) -> Unit,
    val onTouchHead: () -> Unit,
    val onForceResetHeadpatCooldown: () -> Unit,
    val onUseInviteTicket1: () -> Unit,
    val onForceResetInviteTicket1Cooldown: () -> Unit,
    val onUseInviteTicket2: () -> Unit,
    val onForceResetInviteTicket2Cooldown: () -> Unit,
    val onRefreshCalendar: () -> Unit,
    val onOpenCalendarLink: (String) -> Unit,
    val onRefreshPool: () -> Unit,
    val onOpenPoolStudentGuide: (String) -> Unit,
    val onOpenGuideCatalog: () -> Unit,
    val onIdNicknameInputChange: (String) -> Unit,
    val onSaveIdNickname: () -> Unit,
    val onIdFriendCodeInputChange: (String) -> Unit,
    val onSaveIdFriendCode: () -> Unit,
    val onSendApTestNotification: () -> Unit,
    val onSendCafeVisitTestNotification: () -> Unit,
    val onSendArenaRefreshTestNotification: () -> Unit,
    val onTestCafePlus3Hours: () -> Unit,
)

@Composable
internal fun BaPageContent(
    backdrop: Backdrop?,
    innerPadding: PaddingValues,
    contentBottomPadding: Dp,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    state: BaPageContentState,
    actions: BaPageContentActions,
) {
    val sectionTickMs = 1_000L
    val calendarPoolNowMs = remember(state.uiNowMs, sectionTickMs) {
        (state.uiNowMs / sectionTickMs).coerceAtLeast(0L) * sectionTickMs
    }
    val calendarPoolEffectsEnabled = !listState.isScrollInProgress
    val visibleCalendarEntries = remember(
        state.baCalendarEntries,
        state.showEndedActivities,
        calendarPoolNowMs
    ) {
        filterVisibleCalendarEntries(
            entries = state.baCalendarEntries,
            showEndedActivities = state.showEndedActivities,
            nowMs = calendarPoolNowMs
        )
    }
    val visiblePoolEntries = remember(
        state.baPoolEntries,
        state.showEndedPools,
        calendarPoolNowMs
    ) {
        filterVisiblePoolEntries(
            entries = state.baPoolEntries,
            showEndedPools = state.showEndedPools,
            nowMs = calendarPoolNowMs
        )
    }
    val visiblePoolNoteEntries = remember(
        state.baPoolEntries,
        state.showEndedPools,
        calendarPoolNowMs
    ) {
        filterVisiblePoolNoteEntries(
            entries = state.baPoolEntries,
            showEndedPools = state.showEndedPools,
            nowMs = calendarPoolNowMs
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        state = listState,
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 16.dp,
            start = 12.dp,
            end = 12.dp,
        ),
    ) {
        item { SmallTitle(state.officeSmallTitle, insideMargin = state.baSmallTitleMargin) }
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            BaOverviewCard(
                backdrop = backdrop,
                idFriendCode = state.officeState.idFriendCode,
                uiNowMs = state.uiNowMs,
                apSyncMs = state.officeState.apSyncMs,
                apLimit = state.officeState.apLimit,
                apCurrent = state.officeState.apCurrent,
                apRegenBaseMs = state.officeState.apRegenBaseMs,
                apCurrentInput = state.officeState.apCurrentInput,
                onApCurrentInputChange = actions.onApCurrentInputChange,
                onApCurrentDone = actions.onApCurrentDone,
                apLimitInput = state.officeState.apLimitInput,
                onApLimitInputChange = actions.onApLimitInputChange,
                onApLimitDone = actions.onApLimitDone,
                cafeStoredAp = state.officeState.cafeStoredAp,
                cafeLevel = state.officeState.cafeLevel,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                showOverviewServerPopup = state.showOverviewServerPopup,
                overviewServerPopupAnchorBounds = state.overviewServerPopupAnchorBounds,
                onOverviewServerPopupAnchorBoundsChange = actions.onOverviewServerPopupAnchorBoundsChange,
                onOverviewServerPopupChange = actions.onOverviewServerPopupChange,
                onServerSelected = actions.onServerSelected,
                onClaimCafeStoredAp = actions.onClaimCafeStoredAp,
                onOpenGuideCatalog = actions.onOpenGuideCatalog,
                initState = state.initState,
                onInitStateChange = actions.onInitStateChange,
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            BaCafeCard(
                backdrop = backdrop,
                uiNowMs = state.uiNowMs,
                serverIndex = state.serverIndex,
                cafeLevel = state.officeState.cafeLevel,
                cafeLevelOptions = state.cafeLevelOptions,
                showCafeLevelPopup = state.showCafeLevelPopup,
                cafeLevelPopupAnchorBounds = state.cafeLevelPopupAnchorBounds,
                onCafeLevelPopupAnchorBoundsChange = actions.onCafeLevelPopupAnchorBoundsChange,
                onCafeLevelPopupChange = actions.onCafeLevelPopupChange,
                onCafeLevelChange = actions.onCafeLevelChange,
                coffeeHeadpatMs = state.officeState.coffeeHeadpatMs,
                coffeeInvite1UsedMs = state.officeState.coffeeInvite1UsedMs,
                coffeeInvite2UsedMs = state.officeState.coffeeInvite2UsedMs,
                onTouchHead = actions.onTouchHead,
                onForceResetHeadpatCooldown = actions.onForceResetHeadpatCooldown,
                onUseInviteTicket1 = actions.onUseInviteTicket1,
                onForceResetInviteTicket1Cooldown = actions.onForceResetInviteTicket1Cooldown,
                onUseInviteTicket2 = actions.onUseInviteTicket2,
                onForceResetInviteTicket2Cooldown = actions.onForceResetInviteTicket2Cooldown,
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            BaCalendarCard(
                backdrop = backdrop,
                isPageActive = state.isPageActive,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                baCalendarLoading = state.baCalendarLoading,
                baCalendarLastSyncMs = state.baCalendarLastSyncMs,
                baCalendarError = state.baCalendarError,
                visibleCalendarEntries = visibleCalendarEntries,
                nowMs = calendarPoolNowMs,
                showEndedActivities = state.showEndedActivities,
                showCalendarPoolImages = state.showCalendarPoolImages,
                effectsEnabled = calendarPoolEffectsEnabled,
                onRefreshCalendar = actions.onRefreshCalendar,
                onOpenCalendarLink = actions.onOpenCalendarLink
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            BaPoolCard(
                backdrop = backdrop,
                isPageActive = state.isPageActive,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                baPoolLoading = state.baPoolLoading,
                baPoolLastSyncMs = state.baPoolLastSyncMs,
                baPoolError = state.baPoolError,
                visiblePoolEntries = visiblePoolEntries,
                nowMs = calendarPoolNowMs,
                showEndedPools = state.showEndedPools,
                showCalendarPoolImages = state.showCalendarPoolImages,
                effectsEnabled = calendarPoolEffectsEnabled,
                onRefreshPool = actions.onRefreshPool,
                onOpenPoolStudentGuide = actions.onOpenPoolStudentGuide,
                onOpenCalendarLink = actions.onOpenCalendarLink
            )
        }

        if (visiblePoolNoteEntries.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                BaPoolNotesCard(
                    backdrop = backdrop,
                    serverOptions = state.serverOptions,
                    serverIndex = state.serverIndex,
                    baPoolLoading = state.baPoolLoading,
                    baPoolLastSyncMs = state.baPoolLastSyncMs,
                    baPoolError = state.baPoolError,
                    visiblePoolNoteEntries = visiblePoolNoteEntries,
                    nowMs = calendarPoolNowMs,
                    showEndedPools = state.showEndedPools,
                    effectsEnabled = calendarPoolEffectsEnabled,
                    onRefreshPool = actions.onRefreshPool,
                    onOpenCalendarLink = actions.onOpenCalendarLink
                )
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            BaIdCard(
                backdrop = backdrop,
                idNicknameInput = state.officeState.idNicknameInput,
                onIdNicknameInputChange = actions.onIdNicknameInputChange,
                onSaveIdNickname = actions.onSaveIdNickname,
                idFriendCodeInput = state.officeState.idFriendCodeInput,
                onIdFriendCodeInputChange = actions.onIdFriendCodeInputChange,
                onSaveIdFriendCode = actions.onSaveIdFriendCode,
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            BaDebugCard(
                backdrop = backdrop,
                onSendApTestNotification = actions.onSendApTestNotification,
                onSendCafeVisitTestNotification = actions.onSendCafeVisitTestNotification,
                onSendArenaRefreshTestNotification = actions.onSendArenaRefreshTestNotification,
                onTestCafePlus3Hours = actions.onTestCafePlus3Hours,
            )
        }
    }
}
