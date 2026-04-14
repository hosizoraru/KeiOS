package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.BAInitState
import com.example.keios.ui.page.main.BaCalendarEntry
import com.example.keios.ui.page.main.BaPoolEntry
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.SmallTitle

internal data class BaPageContentState(
    val isDark: Boolean,
    val officeSmallTitle: String,
    val baSmallTitleMargin: PaddingValues,
    val baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    val baCardBaseColor: Color,
    val baCardBorderColor: Color,
    val officeState: BaOfficeState,
    val uiNowMs: Long,
    val serverOptions: List<String>,
    val serverIndex: Int,
    val showOverviewServerPopup: Boolean,
    val overviewServerPopupAnchorBounds: IntRect?,
    val initState: BAInitState,
    val disableCardFeedback: Boolean,
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
    val onServerSelected: (Int) -> Unit,
    val onClaimCafeStoredAp: () -> Unit,
    val onInitStateChange: (BAInitState) -> Unit,
    val onTouchHead: () -> Unit,
    val onForceResetHeadpatCooldown: () -> Unit,
    val onUseInviteTicket1: () -> Unit,
    val onForceResetInviteTicket1Cooldown: () -> Unit,
    val onUseInviteTicket2: () -> Unit,
    val onForceResetInviteTicket2Cooldown: () -> Unit,
    val onGlassButtonPressedChange: (Boolean) -> Unit,
    val onRefreshCalendar: () -> Unit,
    val onOpenCalendarLink: (String) -> Unit,
    val onRefreshPool: () -> Unit,
    val onOpenPoolStudentGuide: (String) -> Unit,
    val onIdNicknameInputChange: (String) -> Unit,
    val onSaveIdNickname: () -> Unit,
    val onIdFriendCodeInputChange: (String) -> Unit,
    val onSaveIdFriendCode: () -> Unit,
    val onSendApTestNotification: () -> Unit,
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
                isDark = state.isDark,
                backdrop = backdrop,
                baCardShape = state.baCardShape,
                baCardBaseColor = state.baCardBaseColor,
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
                initState = state.initState,
                onInitStateChange = actions.onInitStateChange,
                disableCardFeedback = state.disableCardFeedback,
            )
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
        item { SmallTitle("咖啡厅", insideMargin = state.baSmallTitleMargin) }
        item { Spacer(modifier = Modifier.height(6.dp)) }

        item {
            BaCafeCard(
                backdrop = backdrop,
                baCardShape = state.baCardShape,
                baCardBaseColor = state.baCardBaseColor,
                baCardBorderColor = state.baCardBorderColor,
                uiNowMs = state.uiNowMs,
                serverIndex = state.serverIndex,
                coffeeHeadpatMs = state.officeState.coffeeHeadpatMs,
                coffeeInvite1UsedMs = state.officeState.coffeeInvite1UsedMs,
                coffeeInvite2UsedMs = state.officeState.coffeeInvite2UsedMs,
                onTouchHead = actions.onTouchHead,
                onForceResetHeadpatCooldown = actions.onForceResetHeadpatCooldown,
                onUseInviteTicket1 = actions.onUseInviteTicket1,
                onForceResetInviteTicket1Cooldown = actions.onForceResetInviteTicket1Cooldown,
                onUseInviteTicket2 = actions.onUseInviteTicket2,
                onForceResetInviteTicket2Cooldown = actions.onForceResetInviteTicket2Cooldown,
                onGlassButtonPressedChange = actions.onGlassButtonPressedChange,
                disableCardFeedback = state.disableCardFeedback,
            )
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
        item { SmallTitle("活动日历", insideMargin = state.baSmallTitleMargin) }
        item { Spacer(modifier = Modifier.height(6.dp)) }

        item {
            BaCalendarCard(
                backdrop = backdrop,
                baCardShape = state.baCardShape,
                baCardBaseColor = state.baCardBaseColor,
                baCardBorderColor = state.baCardBorderColor,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                uiNowMs = state.uiNowMs,
                baCalendarEntries = state.baCalendarEntries,
                baCalendarLoading = state.baCalendarLoading,
                baCalendarError = state.baCalendarError,
                baCalendarLastSyncMs = state.baCalendarLastSyncMs,
                showEndedActivities = state.showEndedActivities,
                showCalendarPoolImages = state.showCalendarPoolImages,
                disableCardFeedback = state.disableCardFeedback,
                onRefreshCalendar = actions.onRefreshCalendar,
                onOpenCalendarLink = actions.onOpenCalendarLink,
            )
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
        item { SmallTitle("卡池信息", insideMargin = state.baSmallTitleMargin) }
        item { Spacer(modifier = Modifier.height(6.dp)) }

        item {
            BaPoolCard(
                backdrop = backdrop,
                baCardShape = state.baCardShape,
                baCardBaseColor = state.baCardBaseColor,
                baCardBorderColor = state.baCardBorderColor,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                uiNowMs = state.uiNowMs,
                baPoolEntries = state.baPoolEntries,
                baPoolLoading = state.baPoolLoading,
                baPoolError = state.baPoolError,
                baPoolLastSyncMs = state.baPoolLastSyncMs,
                showEndedPools = state.showEndedPools,
                showCalendarPoolImages = state.showCalendarPoolImages,
                disableCardFeedback = state.disableCardFeedback,
                onRefreshPool = actions.onRefreshPool,
                onOpenPoolStudentGuide = actions.onOpenPoolStudentGuide,
                onOpenCalendarLink = actions.onOpenCalendarLink,
            )
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
        item { SmallTitle("ID卡", insideMargin = state.baSmallTitleMargin) }
        item { Spacer(modifier = Modifier.height(6.dp)) }

        item {
            BaIdCard(
                backdrop = backdrop,
                baCardShape = state.baCardShape,
                baCardBaseColor = state.baCardBaseColor,
                baCardBorderColor = state.baCardBorderColor,
                idNicknameInput = state.officeState.idNicknameInput,
                onIdNicknameInputChange = actions.onIdNicknameInputChange,
                onSaveIdNickname = actions.onSaveIdNickname,
                idFriendCodeInput = state.officeState.idFriendCodeInput,
                onIdFriendCodeInputChange = actions.onIdFriendCodeInputChange,
                onSaveIdFriendCode = actions.onSaveIdFriendCode,
                disableCardFeedback = state.disableCardFeedback,
            )
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
        item { SmallTitle("Test", insideMargin = state.baSmallTitleMargin) }
        item { Spacer(modifier = Modifier.height(6.dp)) }

        item {
            BaDebugCard(
                backdrop = backdrop,
                baCardShape = state.baCardShape,
                baCardBaseColor = state.baCardBaseColor,
                baCardBorderColor = state.baCardBorderColor,
                disableCardFeedback = state.disableCardFeedback,
                onSendApTestNotification = actions.onSendApTestNotification,
                onTestCafePlus3Hours = actions.onTestCafePlus3Hours,
                onGlassButtonPressedChange = actions.onGlassButtonPressedChange,
            )
        }
    }
}
