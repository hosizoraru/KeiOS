package com.example.keios.ui.page.main.github.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.ui.page.main.GitHubSortMode
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.SearchBarHost
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Tune

@Composable
internal fun GitHubTopBarSection(
    backdrop: LayerBackdrop,
    topBarColor: Color,
    scrollBehavior: ScrollBehavior,
    showSearchBar: Boolean,
    trackedSearch: String,
    sortMode: GitHubSortMode,
    showSortPopup: Boolean,
    deleteInProgress: Boolean,
    onTrackedSearchChange: (String) -> Unit,
    onOpenStrategySheet: () -> Unit,
    onOpenCheckLogicSheet: () -> Unit,
    onShowSortPopupChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubSortMode) -> Unit,
    onRefreshAllTracked: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    Column {
        TopAppBar(
            title = "",
            largeTitle = stringResource(R.string.github_page_title),
            scrollBehavior = scrollBehavior,
            color = topBarColor,
            actions = {
                Box {
                    LiquidActionBar(
                        backdrop = backdrop,
                        items = listOf(
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Edit,
                                contentDescription = stringResource(R.string.github_topbar_cd_edit_strategy),
                                onClick = onOpenStrategySheet
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Tune,
                                contentDescription = stringResource(R.string.github_topbar_cd_check_logic),
                                onClick = onOpenCheckLogicSheet
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Sort,
                                contentDescription = stringResource(R.string.github_topbar_cd_sort),
                                onClick = { onShowSortPopupChange(!showSortPopup) }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Refresh,
                                contentDescription = stringResource(R.string.github_topbar_cd_check),
                                onClick = onRefreshAllTracked,
                                enabled = !deleteInProgress
                            )
                        ),
                        onInteractionChanged = onActionBarInteractingChanged
                    )

                    LiquidActionBarPopupAnchors(itemCount = 4) { slotIndex, popupAnchorBounds ->
                        when (slotIndex) {
                            2 -> if (showSortPopup) {
                                SnapshotWindowListPopup(
                                    show = showSortPopup,
                                    alignment = PopupPositionProvider.Align.BottomStart,
                                    anchorBounds = popupAnchorBounds,
                                    placement = SnapshotPopupPlacement.ActionBarCenter,
                                    onDismissRequest = { onShowSortPopupChange(false) },
                                    enableWindowDim = false
                                ) {
                                    LiquidDropdownColumn {
                                        val modes = GitHubSortMode.entries
                                        modes.forEachIndexed { index, mode ->
                                            LiquidDropdownImpl(
                                                text = stringResource(mode.labelRes),
                                                optionSize = modes.size,
                                                isSelected = sortMode == mode,
                                                index = index,
                                                onSelectedIndexChange = { selectedIndex ->
                                                    onSortModeChange(modes[selectedIndex])
                                                    onShowSortPopupChange(false)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
        SearchBarHost(
            visible = showSearchBar,
            animationLabelPrefix = "githubSearchBar"
        ) {
            Column {
                GlassSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    value = trackedSearch,
                    onValueChange = onTrackedSearchChange,
                    label = stringResource(R.string.github_topbar_search_label),
                    backdrop = backdrop,
                    variant = GlassVariant.Bar,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
