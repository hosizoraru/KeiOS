package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.keios.R
import com.example.keios.ui.page.main.appLucideConfigIcon
import com.example.keios.ui.page.main.appLucideEditIcon
import com.example.keios.ui.page.main.appLucideRefreshIcon
import com.example.keios.ui.page.main.osLucideCopyIcon
import com.example.keios.ui.page.main.widget.AppTopBarSection
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun BaTopBar(
    backdrop: LayerBackdrop,
    liquidActionBarLayeredStyleEnabled: Boolean,
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
                    items = listOf(
                        LiquidActionItem(
                            icon = appLucideEditIcon(),
                            contentDescription = stringResource(R.string.ba_cd_edit),
                            onClick = onShowSettings,
                        ),
                        LiquidActionItem(
                            icon = appLucideConfigIcon(),
                            contentDescription = stringResource(R.string.ba_cd_refresh_interval),
                            onClick = { onShowCalendarIntervalPopupChange(!showCalendarIntervalPopup) },
                        ),
                        LiquidActionItem(
                            icon = osLucideCopyIcon(),
                            contentDescription = stringResource(R.string.ba_cd_copy_friend_code),
                            onClick = onCopyFriendCode,
                        ),
                        LiquidActionItem(
                            icon = appLucideRefreshIcon(),
                            contentDescription = stringResource(R.string.ba_cd_refresh),
                            onClick = onRefreshAll,
                        ),
                    ),
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
