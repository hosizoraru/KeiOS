@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.github.GitHubSortDirection
import os.kei.ui.page.main.github.GitHubSortMode
import os.kei.ui.page.main.github.GitHubTrackedFilterMode
import os.kei.ui.page.main.github.RefreshIntervalOption
import os.kei.ui.page.main.os.appLucideChevronRightIcon
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideDatabaseIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideEditIcon
import os.kei.ui.page.main.os.appLucideFilterIcon
import os.kei.ui.page.main.os.appLucideFlaskIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideSortIcon
import os.kei.ui.page.main.os.appLucideTimeIcon
import os.kei.ui.page.main.os.appLucideUploadIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppTopBarSection
import os.kei.ui.page.main.widget.chrome.LiquidToolbar
import os.kei.ui.page.main.widget.chrome.LiquidToolbarPopupAnchors
import os.kei.ui.page.main.widget.chrome.LiquidToolbarAction
import os.kei.ui.page.main.widget.chrome.appWindowWidthDp
import os.kei.ui.page.main.widget.glass.LiquidGlassActionMenu
import os.kei.ui.page.main.widget.glass.LiquidGlassActionMenuActionRow
import os.kei.ui.page.main.widget.glass.LiquidGlassActionMenuQuickAction
import os.kei.ui.page.main.widget.glass.LiquidGlassActionMenuSingleChoiceRow
import os.kei.ui.page.main.widget.glass.LiquidGlassActionMenuSubmenuRow
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.testing.KeiOsTestTags
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val GitHubActionMenuCompactMinWidth = 244.dp
private val GitHubActionMenuPreferredMinWidth = 256.dp
private val GitHubActionMenuPreferredMaxWidth = 272.dp
private val GitHubActionMenuHorizontalMargin = 92.dp

@Composable
internal fun GitHubTopBarSection(
    topBarColor: Color,
    scrollBehavior: ScrollBehavior,
    titleBackdrop: Backdrop? = null,
    onTitleClick: () -> Unit = {},
) {
    AppTopBarSection(
        title = "",
        largeTitle = stringResource(R.string.github_page_title),
        scrollBehavior = scrollBehavior,
        color = topBarColor,
        titleBackdrop = titleBackdrop,
        titleEndReserve = AppChromeTokens.topBarTitleActionReserve + 44.dp,
        onTitleClick = onTitleClick,
    )
}

@Composable
internal fun GitHubTopBarActions(
    backdrop: Backdrop,
    sortMode: GitHubSortMode,
    sortDirection: GitHubSortDirection,
    trackedFilterMode: GitHubTrackedFilterMode,
    refreshIntervalHours: Int,
    fdroidCommonRepoCount: Int,
    showActionMenuPopup: Boolean,
    tracksExporting: Boolean,
    tracksImporting: Boolean,
    onOpenStrategySheet: () -> Unit,
    onOpenCheckLogicSheet: () -> Unit,
    onOpenDroidSourcesSheet: () -> Unit,
    onOpenDebugSheet: () -> Unit,
    onRefreshInstalledApps: () -> Unit,
    onShowActionMenuPopupChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubSortMode) -> Unit,
    onSortDirectionChange: (GitHubSortDirection) -> Unit,
    onTrackedFilterModeChange: (GitHubTrackedFilterMode) -> Unit,
    onRefreshIntervalHoursChange: (Int) -> Unit,
    onExportTrackedItems: () -> Unit,
    onImportTrackedItems: () -> Unit,
    onOpenStarImport: () -> Unit,
    onOpenDesktopWidget: () -> Unit = {},
) {
    val editStrategyIcon = appLucideEditIcon()
    val checkLogicIcon = appLucideConfigIcon()
    val exportTracksIcon = appLucideDownloadIcon()
    val importTracksIcon = appLucideUploadIcon()
    val importStarsIcon = appLucideHeartIcon()
    val sortIcon = appLucideSortIcon()
    val filterIcon = appLucideFilterIcon()
    val intervalIcon = appLucideTimeIcon()
    val droidSourcesIcon = appLucideDatabaseIcon()
    val debugIcon = appLucideFlaskIcon()
    val refreshAppsIcon = appLucideRefreshIcon()
    val moreIcon = appLucideMoreIcon()
    val widgetIcon = appLucideGridIcon()
    val widgetDescription = stringResource(R.string.github_widget_add)
    val chevronRightIcon = appLucideChevronRightIcon()
    val editStrategyContentDescription = stringResource(R.string.github_topbar_cd_edit_strategy)
    val checkLogicContentDescription = stringResource(R.string.github_topbar_cd_check_logic)
    val sortContentDescription = stringResource(R.string.github_topbar_cd_sort)
    val sortDirectionLabel = stringResource(R.string.github_topbar_cd_sort_direction)
    val filterLabel = stringResource(R.string.github_topbar_cd_filter)
    val refreshIntervalLabel = stringResource(R.string.github_check_sheet_label_refresh_interval)
    val droidSourcesLabel = stringResource(R.string.github_topbar_droid_sources)
    val droidSourcesCountLabel =
        stringResource(R.string.github_topbar_droid_sources_count, fdroidCommonRepoCount)
    val debugLabel = stringResource(R.string.github_topbar_debug_tools)
    val debugSummary = stringResource(R.string.github_topbar_debug_tools_summary)
    val refreshAppsLabel = stringResource(R.string.github_topbar_refresh_app_list)
    val refreshAppsSummary = stringResource(R.string.github_topbar_refresh_app_list_summary)
    val moreContentDescription = stringResource(R.string.github_item_cd_more_actions)
    val exportTracksLabel =
        if (tracksExporting) {
            stringResource(R.string.github_check_sheet_action_exporting)
        } else {
            stringResource(R.string.github_check_sheet_action_export_tracks)
        }
    val importTracksLabel =
        if (tracksImporting) {
            stringResource(R.string.github_check_sheet_action_importing)
        } else {
            stringResource(R.string.github_check_sheet_action_import_tracks)
        }
    val importStarsLabel = stringResource(R.string.github_check_sheet_action_import_stars)
    val transferActionEnabled = !tracksExporting && !tracksImporting
    val screenWidth = appWindowWidthDp()
    val actionMenuMaxWidth =
        (screenWidth - GitHubActionMenuHorizontalMargin)
            .coerceIn(GitHubActionMenuCompactMinWidth, GitHubActionMenuPreferredMaxWidth)
    val actionMenuMinWidth = minOf(GitHubActionMenuPreferredMinWidth, actionMenuMaxWidth)
    val actionItems =
        remember(
            editStrategyIcon,
            checkLogicIcon,
            moreIcon,
            widgetIcon,
            widgetDescription,
            onOpenDesktopWidget,
            editStrategyContentDescription,
            checkLogicContentDescription,
            moreContentDescription,
            showActionMenuPopup,
            onOpenStrategySheet,
            onOpenCheckLogicSheet,
            onShowActionMenuPopupChange,
        ) {
            listOf(
                LiquidToolbarAction(
                    icon = editStrategyIcon,
                    contentDescription = editStrategyContentDescription,
                    onClick = {
                        onShowActionMenuPopupChange(false)
                        onOpenStrategySheet()
                    },
                    testTag = KeiOsTestTags.GitHubStrategySheetButton,
                ),
                LiquidToolbarAction(
                    icon = checkLogicIcon,
                    contentDescription = checkLogicContentDescription,
                    onClick = {
                        onShowActionMenuPopupChange(false)
                        onOpenCheckLogicSheet()
                    },
                    testTag = KeiOsTestTags.GitHubCheckLogicSheetButton,
                ),
                LiquidToolbarAction(
                    icon = widgetIcon,
                    contentDescription = widgetDescription,
                    onClick = {
                        onShowActionMenuPopupChange(false)
                        onOpenDesktopWidget()
                    },
                    testTag = KeiOsTestTags.GitHubDesktopWidgetButton,
                ),
                LiquidToolbarAction(
                    icon = moreIcon,
                    contentDescription = moreContentDescription,
                    onClick = { onShowActionMenuPopupChange(!showActionMenuPopup) },
                    testTag = KeiOsTestTags.GitHubImportMenuButton,
                ),
            )
        }
    Box {
        LiquidToolbar(
            backdrop = backdrop,
            actions = actionItems,
        )

        LiquidToolbarPopupAnchors(itemCount = 4) { slotIndex, popupAnchorBounds ->
            when (slotIndex) {
                3 -> {
                    key("github-top-bar-action-popup") {
                        SnapshotWindowListPopup(
                            show = showActionMenuPopup,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            anchorBoundsProvider = popupAnchorBounds,
                            placement = SnapshotPopupPlacement.ButtonEnd,
                            onDismissRequest = { onShowActionMenuPopupChange(false) },
                            maxWidth = actionMenuMaxWidth,
                        ) {
                            val modes = GitHubSortMode.entries
                            val sortLabels = modes.map { mode -> stringResource(mode.labelRes) }
                            val selectedSortLabel =
                                sortLabels.getOrElse(modes.indexOf(sortMode)) {
                                    stringResource(sortMode.labelRes)
                                }
                            val directions = GitHubSortDirection.entries
                            val directionLabels =
                                directions.map { direction ->
                                    stringResource(direction.labelRes)
                                }
                            val selectedDirectionLabel =
                                directionLabels.getOrElse(
                                    directions.indexOf(sortDirection),
                                ) {
                                    stringResource(sortDirection.labelRes)
                                }
                            val filterModes = GitHubTrackedFilterMode.entries
                            val filterLabels = filterModes.map { mode -> stringResource(mode.labelRes) }
                            val selectedFilterLabel =
                                filterLabels.getOrElse(filterModes.indexOf(trackedFilterMode)) {
                                    stringResource(trackedFilterMode.labelRes)
                                }
                            val refreshIntervalOptions = RefreshIntervalOption.entries
                            val refreshIntervalLabels =
                                refreshIntervalOptions.map { option ->
                                    stringResource(option.labelRes)
                                }
                            val selectedRefreshInterval =
                                RefreshIntervalOption.fromHours(refreshIntervalHours)
                            val selectedRefreshIntervalLabel =
                                refreshIntervalLabels.getOrElse(
                                    refreshIntervalOptions.indexOf(selectedRefreshInterval),
                                ) {
                                    stringResource(selectedRefreshInterval.labelRes)
                                }
                            LiquidGlassActionMenu(
                                backdrop = backdrop,
                                accentColor = MiuixTheme.colorScheme.onBackground,
                                minWidth = actionMenuMinWidth,
                                maxWidth = actionMenuMaxWidth,
                                quickActions =
                                    listOf(
                                        LiquidGlassActionMenuQuickAction(
                                            id = "export_tracks",
                                            icon = exportTracksIcon,
                                            label = exportTracksLabel,
                                            enabled = transferActionEnabled,
                                            onClick = onExportTrackedItems,
                                        ),
                                        LiquidGlassActionMenuQuickAction(
                                            id = "import_tracks",
                                            icon = importTracksIcon,
                                            label = importTracksLabel,
                                            enabled = transferActionEnabled,
                                            testTag = KeiOsTestTags.GitHubImportTracks,
                                            onClick = onImportTrackedItems,
                                        ),
                                        LiquidGlassActionMenuQuickAction(
                                            id = "import_stars",
                                            icon = importStarsIcon,
                                            label = importStarsLabel,
                                            enabled = transferActionEnabled,
                                            testTag = KeiOsTestTags.GitHubImportStars,
                                            onClick = onOpenStarImport,
                                        ),
                                    ),
                                items =
                                    listOf(
                                        LiquidGlassActionMenuActionRow(
                                            id = "droid_sources",
                                            text = droidSourcesLabel,
                                            subtitle = droidSourcesCountLabel,
                                            leadingIcon = droidSourcesIcon,
                                            onClick = onOpenDroidSourcesSheet,
                                        ),
                                        LiquidGlassActionMenuActionRow(
                                            id = "debug",
                                            text = debugLabel,
                                            subtitle = debugSummary,
                                            leadingIcon = debugIcon,
                                            onClick = {
                                                onShowActionMenuPopupChange(false)
                                                onOpenDebugSheet()
                                            },
                                        ),
                                        LiquidGlassActionMenuActionRow(
                                            id = "refresh_app_list",
                                            text = refreshAppsLabel,
                                            subtitle = refreshAppsSummary,
                                            leadingIcon = refreshAppsIcon,
                                            onClick = onRefreshInstalledApps,
                                        ),
                                        LiquidGlassActionMenuSubmenuRow(
                                            id = "sort",
                                            text = sortContentDescription,
                                            subtitle = selectedSortLabel,
                                            leadingIcon = sortIcon,
                                            trailingIcon = chevronRightIcon,
                                            submenuItems =
                                                modes.mapIndexed { index, mode ->
                                                    LiquidGlassActionMenuSingleChoiceRow(
                                                        id = mode.name,
                                                        text = sortLabels[index],
                                                        selected = sortMode == mode,
                                                        leadingIcon = sortIcon,
                                                        onClick = { onSortModeChange(mode) },
                                                    )
                                                },
                                        ),
                                        LiquidGlassActionMenuSubmenuRow(
                                            id = "sort_direction",
                                            text = sortDirectionLabel,
                                            subtitle = selectedDirectionLabel,
                                            leadingIcon = sortIcon,
                                            trailingIcon = chevronRightIcon,
                                            submenuItems =
                                                directions.mapIndexed { index, direction ->
                                                    LiquidGlassActionMenuSingleChoiceRow(
                                                        id = direction.name,
                                                        text = directionLabels[index],
                                                        selected = sortDirection == direction,
                                                        leadingIcon = sortIcon,
                                                        onClick = { onSortDirectionChange(direction) },
                                                    )
                                                },
                                        ),
                                        LiquidGlassActionMenuSubmenuRow(
                                            id = "filter",
                                            text = filterLabel,
                                            subtitle = selectedFilterLabel,
                                            leadingIcon = filterIcon,
                                            trailingIcon = chevronRightIcon,
                                            submenuItems =
                                                filterModes.mapIndexed { index, mode ->
                                                    LiquidGlassActionMenuSingleChoiceRow(
                                                        id = mode.name,
                                                        text = filterLabels[index],
                                                        selected = trackedFilterMode == mode,
                                                        leadingIcon = filterIcon,
                                                        onClick = { onTrackedFilterModeChange(mode) },
                                                    )
                                                },
                                        ),
                                        LiquidGlassActionMenuSubmenuRow(
                                            id = "refresh_interval",
                                            text = refreshIntervalLabel,
                                            subtitle = selectedRefreshIntervalLabel,
                                            leadingIcon = intervalIcon,
                                            trailingIcon = chevronRightIcon,
                                            submenuItems =
                                                refreshIntervalOptions.mapIndexed { index, option ->
                                                    LiquidGlassActionMenuSingleChoiceRow(
                                                        id = option.name,
                                                        text = refreshIntervalLabels[index],
                                                        selected = selectedRefreshInterval == option,
                                                        leadingIcon = intervalIcon,
                                                        onClick = { onRefreshIntervalHoursChange(option.hours) },
                                                    )
                                                },
                                        ),
                                    ),
                                onDismissRequest = { onShowActionMenuPopupChange(false) },
                            )
                        }
                    }
                }
            }
        }
    }
}
