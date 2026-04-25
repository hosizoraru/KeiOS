package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.github.GitHubSortMode
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideEditIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideSortIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppTopBarSearchField
import os.kei.ui.page.main.widget.chrome.AppTopBarSection
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionBarPopupAnchors
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import os.kei.ui.page.main.widget.glass.LiquidDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidDropdownImpl
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

@Composable
internal fun GitHubTopBarSection(
    backdrop: LayerBackdrop,
    topBarColor: Color,
    scrollBehavior: ScrollBehavior,
    enableSearchBar: Boolean,
    showSearchBar: Boolean,
    trackedSearch: String,
    onTrackedSearchChange: (String) -> Unit,
) {
    AppTopBarSection(
        title = "",
        largeTitle = stringResource(R.string.github_page_title),
        scrollBehavior = scrollBehavior,
        color = topBarColor,
        searchBarVisible = enableSearchBar && showSearchBar,
        searchBarAnimationLabelPrefix = "githubSearchBar",
    ) {
        Column {
            AppTopBarSearchField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppChromeTokens.searchFieldHorizontalPadding),
                value = trackedSearch,
                onValueChange = onTrackedSearchChange,
                label = stringResource(R.string.github_topbar_search_label),
                backdrop = backdrop
            )
        }
    }
}

@Composable
internal fun GitHubTopBarActions(
    backdrop: LayerBackdrop,
    liquidActionBarLayeredStyleEnabled: Boolean,
    reduceEffectsDuringPagerScroll: Boolean,
    sortMode: GitHubSortMode,
    showSortPopup: Boolean,
    deleteInProgress: Boolean,
    onOpenStrategySheet: () -> Unit,
    onOpenCheckLogicSheet: () -> Unit,
    onShowSortPopupChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubSortMode) -> Unit,
    onRefreshAllTracked: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    val editStrategyIcon = appLucideEditIcon()
    val checkLogicIcon = appLucideConfigIcon()
    val sortIcon = appLucideSortIcon()
    val refreshIcon = appLucideRefreshIcon()
    val editStrategyContentDescription = stringResource(R.string.github_topbar_cd_edit_strategy)
    val checkLogicContentDescription = stringResource(R.string.github_topbar_cd_check_logic)
    val sortContentDescription = stringResource(R.string.github_topbar_cd_sort)
    val refreshContentDescription = stringResource(R.string.github_topbar_cd_check)
    val actionItems = remember(
        editStrategyContentDescription,
        checkLogicContentDescription,
        sortContentDescription,
        refreshContentDescription,
        showSortPopup,
        deleteInProgress,
        onOpenStrategySheet,
        onOpenCheckLogicSheet,
        onShowSortPopupChange,
        onRefreshAllTracked
    ) {
        listOf(
            LiquidActionItem(
                icon = editStrategyIcon,
                contentDescription = editStrategyContentDescription,
                onClick = onOpenStrategySheet
            ),
            LiquidActionItem(
                icon = checkLogicIcon,
                contentDescription = checkLogicContentDescription,
                onClick = onOpenCheckLogicSheet
            ),
            LiquidActionItem(
                icon = sortIcon,
                contentDescription = sortContentDescription,
                onClick = { onShowSortPopupChange(!showSortPopup) }
            ),
            LiquidActionItem(
                icon = refreshIcon,
                contentDescription = refreshContentDescription,
                onClick = onRefreshAllTracked,
                enabled = !deleteInProgress
            )
        )
    }
    Box {
        LiquidActionBar(
            backdrop = backdrop,
            layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
            reduceEffectsDuringPagerScroll = reduceEffectsDuringPagerScroll,
            items = actionItems,
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
