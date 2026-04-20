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
import com.example.keios.ui.page.main.ba.card.BaCalendarEntryPanel
import com.example.keios.ui.page.main.ba.card.BaCalendarSectionHeaderCard
import com.example.keios.ui.page.main.ba.card.BaCalendarStatePanel
import com.example.keios.ui.page.main.ba.card.BaDebugCard
import com.example.keios.ui.page.main.ba.card.BaIdCard
import com.example.keios.ui.page.main.ba.card.BaOverviewCard
import com.example.keios.ui.page.main.ba.card.BaPoolEntryPanel
import com.example.keios.ui.page.main.ba.card.BaPoolSectionHeaderCard
import com.example.keios.ui.page.main.ba.card.BaPoolStatePanel
import com.example.keios.ui.page.main.ba.card.filterVisibleCalendarEntries
import com.example.keios.ui.page.main.ba.card.filterVisiblePoolEntries
import com.example.keios.ui.page.main.ba.support.BAInitState
import com.example.keios.ui.page.main.ba.support.BaCalendarEntry
import com.example.keios.ui.page.main.ba.support.BaPoolEntry
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val sectionTickMs = if (listState.isScrollInProgress) 30_000L else 5_000L
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
            BaCalendarSectionHeaderCard(
                backdrop = backdrop,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                baCalendarLoading = state.baCalendarLoading,
                baCalendarLastSyncMs = state.baCalendarLastSyncMs,
                effectsEnabled = calendarPoolEffectsEnabled,
                onRefreshCalendar = actions.onRefreshCalendar,
            )
        }

        when {
            !state.baCalendarError.isNullOrBlank() -> {
                item {
                    BaCalendarStatePanel(
                        backdrop = backdrop,
                        text = state.baCalendarError.orEmpty(),
                        accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                        effectsEnabled = calendarPoolEffectsEnabled
                    )
                }
            }

            !state.baCalendarLoading && visibleCalendarEntries.isEmpty() -> {
                item {
                    BaCalendarStatePanel(
                        backdrop = backdrop,
                        text = if (state.showEndedActivities) "暂无活动" else "暂无进行中或即将开始的活动",
                        accentColor = MiuixTheme.colorScheme.onBackgroundVariant,
                        effectsEnabled = calendarPoolEffectsEnabled
                    )
                }
            }

            else -> {
                items(
                    items = visibleCalendarEntries,
                    key = { activity -> "calendar-${activity.id}-${activity.beginAtMs}-${activity.endAtMs}" },
                    contentType = { "ba_calendar_entry" }
                ) { activity ->
                    BaCalendarEntryPanel(
                        backdrop = backdrop,
                        isPageActive = state.isPageActive,
                        serverIndex = state.serverIndex,
                        activity = activity,
                        nowMs = calendarPoolNowMs,
                        showCalendarPoolImages = state.showCalendarPoolImages,
                        effectsEnabled = calendarPoolEffectsEnabled,
                        onOpenCalendarLink = actions.onOpenCalendarLink
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            BaPoolSectionHeaderCard(
                backdrop = backdrop,
                serverOptions = state.serverOptions,
                serverIndex = state.serverIndex,
                baPoolLoading = state.baPoolLoading,
                baPoolLastSyncMs = state.baPoolLastSyncMs,
                effectsEnabled = calendarPoolEffectsEnabled,
                onRefreshPool = actions.onRefreshPool,
            )
        }

        when {
            !state.baPoolError.isNullOrBlank() -> {
                item {
                    BaPoolStatePanel(
                        backdrop = backdrop,
                        text = state.baPoolError.orEmpty(),
                        accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                        effectsEnabled = calendarPoolEffectsEnabled
                    )
                }
            }

            !state.baPoolLoading && visiblePoolEntries.isEmpty() -> {
                item {
                    BaPoolStatePanel(
                        backdrop = backdrop,
                        text = "暂无进行中或即将开始的卡池",
                        accentColor = MiuixTheme.colorScheme.onBackgroundVariant,
                        effectsEnabled = calendarPoolEffectsEnabled
                    )
                }
            }

            else -> {
                items(
                    items = visiblePoolEntries,
                    key = { pool -> "pool-${pool.id}-${pool.startAtMs}-${pool.endAtMs}" },
                    contentType = { "ba_pool_entry" }
                ) { pool ->
                    BaPoolEntryPanel(
                        backdrop = backdrop,
                        isPageActive = state.isPageActive,
                        serverIndex = state.serverIndex,
                        pool = pool,
                        nowMs = calendarPoolNowMs,
                        showCalendarPoolImages = state.showCalendarPoolImages,
                        effectsEnabled = calendarPoolEffectsEnabled,
                        onOpenPoolStudentGuide = actions.onOpenPoolStudentGuide,
                        onOpenCalendarLink = actions.onOpenCalendarLink
                    )
                }
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
