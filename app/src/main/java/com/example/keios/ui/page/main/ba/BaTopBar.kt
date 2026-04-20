package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.keios.R
import com.example.keios.ui.page.main.os.appLucideConfigIcon
import com.example.keios.ui.page.main.os.appLucideEditIcon
import com.example.keios.ui.page.main.os.appLucideRefreshIcon
import com.example.keios.ui.page.main.ba.support.BaCalendarRefreshIntervalOption
import com.example.keios.ui.page.main.os.osLucideCopyIcon
import com.example.keios.ui.page.main.widget.chrome.AppTopBarSection
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.example.keios.ui.page.main.widget.sheet.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.glass.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.glass.LiquidDropdownImpl
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun BaTopBar(
    backdrop: LayerBackdrop,
    liquidActionBarLayeredStyleEnabled: Boolean,
    reduceEffectsDuringPagerScroll: Boolean,
    topBarColor: Color,
    scrollBehavior: ScrollBehavior?,
    showCalendarIntervalPopup: Boolean,
    calendarRefreshIntervalHours: Int,
    onShowSettings: () -> Unit,
    onShowCalendarIntervalPopupChange: (Boolean) -> Unit,
    onCopyFriendCode: () -> Unit,
    onRefreshAll: () -> Unit,
    onCalendarRefreshIntervalSelected: (Int) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
) {
    val editIcon = appLucideEditIcon()
    val refreshIntervalIcon = appLucideConfigIcon()
    val copyFriendCodeIcon = osLucideCopyIcon()
    val refreshIcon = appLucideRefreshIcon()
    val editContentDescription = stringResource(R.string.ba_cd_edit)
    val refreshIntervalContentDescription = stringResource(R.string.ba_cd_refresh_interval)
    val copyFriendCodeContentDescription = stringResource(R.string.ba_cd_copy_friend_code)
    val refreshContentDescription = stringResource(R.string.ba_cd_refresh)
    val actionItems = remember(
        editContentDescription,
        refreshIntervalContentDescription,
        copyFriendCodeContentDescription,
        refreshContentDescription,
        showCalendarIntervalPopup,
        onShowSettings,
        onShowCalendarIntervalPopupChange,
        onCopyFriendCode,
        onRefreshAll
    ) {
        listOf(
            LiquidActionItem(
                icon = editIcon,
                contentDescription = editContentDescription,
                onClick = onShowSettings,
            ),
            LiquidActionItem(
                icon = refreshIntervalIcon,
                contentDescription = refreshIntervalContentDescription,
                onClick = { onShowCalendarIntervalPopupChange(!showCalendarIntervalPopup) },
            ),
            LiquidActionItem(
                icon = copyFriendCodeIcon,
                contentDescription = copyFriendCodeContentDescription,
                onClick = onCopyFriendCode,
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshContentDescription,
                onClick = onRefreshAll,
            ),
        )
    }

    AppTopBarSection(
        title = "",
        largeTitle = stringResource(R.string.ba_topbar_title),
        scrollBehavior = scrollBehavior,
        color = topBarColor,
        actions = {
            Box {
                LiquidActionBar(
                    backdrop = backdrop,
                    layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                    reduceEffectsDuringPagerScroll = reduceEffectsDuringPagerScroll,
                    items = actionItems,
                    onInteractionChanged = onInteractionChanged,
                )

                LiquidActionBarPopupAnchors(itemCount = 4) { slotIndex, popupAnchorBounds ->
                    if (slotIndex == 1 && showCalendarIntervalPopup) {
                        SnapshotWindowListPopup(
                            show = showCalendarIntervalPopup,
                            alignment = PopupPositionProvider.Align.BottomStart,
                            anchorBounds = popupAnchorBounds,
                            placement = SnapshotPopupPlacement.ActionBarCenter,
                            onDismissRequest = { onShowCalendarIntervalPopupChange(false) },
                            enableWindowDim = false,
                        ) {
                            LiquidDropdownColumn {
                                val options = BaCalendarRefreshIntervalOption.entries
                                val selected = BaCalendarRefreshIntervalOption.fromHours(
                                    calendarRefreshIntervalHours,
                                )
                                options.forEachIndexed { index, option ->
                                    LiquidDropdownImpl(
                                        text = option.label,
                                        optionSize = options.size,
                                        isSelected = selected == option,
                                        index = index,
                                        onSelectedIndexChange = { selectedIndex ->
                                            onCalendarRefreshIntervalSelected(options[selectedIndex].hours)
                                            onShowCalendarIntervalPopupChange(false)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
