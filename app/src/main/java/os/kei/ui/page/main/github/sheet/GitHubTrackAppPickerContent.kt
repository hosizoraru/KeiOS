package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.data.local.GitHubAppPickerPreferences
import os.kei.feature.github.model.InstalledAppItem
import os.kei.ui.page.main.github.GitHubAppCandidateRow
import os.kei.ui.page.main.github.GitHubSelectedAppCard
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerDerivedState
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerInput
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerSortDirection
import os.kei.ui.page.main.github.picker.GitHubTrackAppPickerSortMode
import os.kei.ui.page.main.github.picker.gitHubTrackAppCandidateInitialScrollIndex
import os.kei.ui.page.main.github.picker.gitHubTrackAppPickerViewportIconPackages
import os.kei.ui.page.main.github.picker.isTimeSort
import os.kei.ui.page.main.github.picker.showsInstallSourcePill
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.chrome.appWindowHeightDp
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.glass.AppLiquidCheckbox
import os.kei.ui.page.main.widget.glass.AppLiquidExpandableSection
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidSearchField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidInfoBlock
import os.kei.ui.page.main.widget.glass.LiquidInfoBlockDensity
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetInputTitle
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubTrackAppPickerControls(
    backdrop: Backdrop,
    includeUserApps: Boolean,
    includeSystemApps: Boolean,
    includeTrackedApps: Boolean,
    includeDisabledOrFrozenApps: Boolean,
    sortMode: GitHubTrackAppPickerSortMode,
    sortDirection: GitHubTrackAppPickerSortDirection,
    onIncludeUserAppsChange: (Boolean) -> Unit,
    onIncludeSystemAppsChange: (Boolean) -> Unit,
    onIncludeTrackedAppsChange: (Boolean) -> Unit,
    onIncludeDisabledOrFrozenAppsChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubTrackAppPickerSortMode) -> Unit,
    onSortDirectionChange: (GitHubTrackAppPickerSortDirection) -> Unit
) {
    var sortModeExpanded by remember { mutableStateOf(false) }
    var sortDirectionExpanded by remember { mutableStateOf(false) }
    val sortModes = GitHubTrackAppPickerSortMode.entries
    val sortDirections = GitHubTrackAppPickerSortDirection.entries
    val sortOptions = sortModes.map { mode -> stringResource(mode.labelRes) }
    val directionOptions = sortDirections.map { direction -> stringResource(direction.labelRes) }
    val sortIndex = sortModes.indexOf(sortMode).coerceAtLeast(0)
    val directionIndex = sortDirections.indexOf(sortDirection).coerceAtLeast(0)
    val frozenAppsLabel = stringResource(R.string.github_track_sheet_app_filter_frozen_apps)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GitHubTrackAppPickerButtonRow(
            label = stringResource(R.string.github_track_sheet_app_filter_scope_label)
        ) {
            GitHubTrackAppTypeCheckbox(
                backdrop = backdrop,
                text = stringResource(R.string.github_track_sheet_app_filter_user_apps),
                checked = includeUserApps,
                onCheckedChange = onIncludeUserAppsChange,
            )
            GitHubTrackAppTypeCheckbox(
                backdrop = backdrop,
                text = stringResource(R.string.github_track_sheet_app_filter_system_apps),
                checked = includeSystemApps,
                onCheckedChange = onIncludeSystemAppsChange,
            )
            GitHubTrackAppTypeCheckbox(
                backdrop = backdrop,
                text = stringResource(R.string.github_track_sheet_app_filter_tracked_apps),
                checked = includeTrackedApps,
                onCheckedChange = onIncludeTrackedAppsChange,
            )
        }
        SheetControlRow(
            label = frozenAppsLabel,
            summary = stringResource(R.string.github_track_sheet_app_filter_frozen_apps_summary),
        ) {
            AppSwitch(
                checked = includeDisabledOrFrozenApps,
                onCheckedChange = onIncludeDisabledOrFrozenAppsChange,
                modifier = Modifier.semantics { contentDescription = frozenAppsLabel },
            )
        }
        GitHubTrackAppPickerSortRow(
            label = stringResource(R.string.github_track_sheet_app_sort_label)
        ) {
            AppDropdownSelector(
                selectedText = stringResource(
                    R.string.github_track_sheet_app_sort_dropdown_format,
                    sortOptions.getOrElse(sortIndex) { "" }
                ),
                options = sortOptions,
                selectedIndex = sortIndex,
                expanded = sortModeExpanded,
                onExpandedChange = { sortModeExpanded = it },
                onSelectedIndexChange = { index ->
                    sortModes.getOrNull(index)?.let { mode ->
                        onSortModeChange(mode)
                        if (mode.isTimeSort()) {
                            onSortDirectionChange(GitHubTrackAppPickerSortDirection.Descending)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                backdrop = backdrop,
                variant = GlassVariant.Content,
                minHeight = 32.dp,
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp
            )
            AppDropdownSelector(
                selectedText = stringResource(
                    R.string.github_track_sheet_app_sort_direction_dropdown_format,
                    directionOptions.getOrElse(directionIndex) { "" }
                ),
                options = directionOptions,
                selectedIndex = directionIndex,
                expanded = sortDirectionExpanded,
                onExpandedChange = { sortDirectionExpanded = it },
                onSelectedIndexChange = { index ->
                    sortDirections.getOrNull(index)?.let(onSortDirectionChange)
                },
                modifier = Modifier.weight(1f),
                backdrop = backdrop,
                variant = GlassVariant.Content,
                minHeight = 32.dp,
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp
            )
        }
    }
}

@Composable
internal fun GitHubTrackAppPickerSortRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetInputTitle(label)
        content()
    }
}

@Composable
internal fun GitHubTrackAppTypeCheckbox(
    backdrop: Backdrop,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .widthIn(min = 88.dp)
            .heightIn(min = AppInteractiveTokens.compactControlRowMinHeight)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLiquidCheckbox(
            checked = checked,
            onCheckedChange = null,
            backdrop = backdrop
        )
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}

@Composable
internal fun GitHubTrackAppPickerButtonRow(
    label: String,
    content: @Composable FlowRowScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SheetInputTitle(label)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
internal fun GitHubTrackAppPickerHeader(
    title: String,
    resultCount: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheetSectionTitle(
            text = title,
            modifier = Modifier.weight(1f),
        )
        SheetInputTitle(text = resultCount)
    }
}

@Composable
internal fun GitHubTrackAppPickerContent(
    backdrop: Backdrop,
    active: Boolean,
    appSearch: String,
    selectedApp: InstalledAppItem?,
    appList: List<InstalledAppItem>,
    derivedState: GitHubTrackAppPickerDerivedState,
    appPickerPreferences: GitHubAppPickerPreferences,
    trackedPackageNames: Set<String>,
    editingPackageName: String,
    appListRefreshing: Boolean,
    rememberAddPickerScroll: Boolean,
    rememberedFirstVisibleItemIndex: Int,
    rememberedFirstVisibleItemScrollOffset: Int,
    onAppSearchChange: (String) -> Unit,
    onPickerExpandedChange: (Boolean) -> Unit,
    onRefreshAppList: () -> Unit,
    onRequestAppIcons: (List<String>) -> Unit,
    onRequestAppPickerState: (GitHubTrackAppPickerInput) -> Unit,
    onAppPickerPreferencesChange: (GitHubAppPickerPreferences) -> Unit,
    onAddAppPickerScrollPositionChange: (Int, Int) -> Unit,
    onSelectedAppChange: (InstalledAppItem?) -> Unit
) {
    val windowHeight = appWindowHeightDp()
    val adaptiveListMinHeight = (windowHeight * 0.34f).coerceIn(260.dp, 420.dp)
    val listMaxHeight = (windowHeight * 0.60f).coerceIn(340.dp, 680.dp)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentOnRequestAppIcons by rememberUpdatedState(onRequestAppIcons)
    var includeUserApps by remember(appPickerPreferences) {
        mutableStateOf(appPickerPreferences.includeUserApps)
    }
    var includeSystemApps by remember(appPickerPreferences) {
        mutableStateOf(appPickerPreferences.includeSystemApps)
    }
    var includeTrackedApps by remember(appPickerPreferences) {
        mutableStateOf(appPickerPreferences.includeTrackedApps)
    }
    var includeDisabledOrFrozenApps by remember(appPickerPreferences) {
        mutableStateOf(appPickerPreferences.includeDisabledOrFrozenApps)
    }
    var sortMode by remember(appPickerPreferences) {
        mutableStateOf(GitHubTrackAppPickerSortMode.fromStorageId(appPickerPreferences.sortModeId))
    }
    var sortDirection by remember(appPickerPreferences) {
        mutableStateOf(
            GitHubTrackAppPickerSortDirection.fromStorageId(appPickerPreferences.sortDirectionId)
        )
    }
    var filtersExpanded by remember { mutableStateOf(false) }
    var initialAppFocusApplied by remember(selectedApp?.packageName) {
        mutableStateOf(false)
    }
    val showInstallSourcePill = sortMode.showsInstallSourcePill()
    val appPickerInput =
        remember(
            appList,
            appSearch,
            includeUserApps,
            includeSystemApps,
            includeTrackedApps,
            includeDisabledOrFrozenApps,
            trackedPackageNames,
            selectedApp?.packageName,
            editingPackageName,
            sortMode,
            sortDirection,
        ) {
            val pinnedPackageNames =
                setOf(
                    selectedApp?.packageName.orEmpty(),
                    editingPackageName,
                )
            GitHubTrackAppPickerInput(
                appList = appList,
                query = appSearch,
                includeUserApps = includeUserApps,
                includeSystemApps = includeSystemApps,
                includeTrackedApps = includeTrackedApps,
                includeDisabledOrFrozenApps = includeDisabledOrFrozenApps,
                trackedPackageNames = trackedPackageNames,
                pinnedPackageNames = pinnedPackageNames,
                sortMode = sortMode,
                sortDirection = sortDirection,
            )
        }
    val derivedInputMatches = derivedState.input == appPickerInput
    val filteredApps =
        if (derivedInputMatches) {
            derivedState.filteredApps
        } else {
            emptyList()
        }
    val appFilterReady = derivedInputMatches && !derivedState.deriving
    val visibleRowsForStableHeight = derivedState.filteredApps.size.coerceIn(1, 6)
    val contentDrivenListMinHeight = (visibleRowsForStableHeight * 72).dp
    val listMinHeight = minOf(adaptiveListMinHeight, contentDrivenListMinHeight)
    LaunchedEffect(active, appPickerInput) {
        if (!active) return@LaunchedEffect
        onRequestAppPickerState(appPickerInput)
    }

    fun saveAppPickerPreferences() {
        onAppPickerPreferencesChange(
            GitHubAppPickerPreferences(
                includeUserApps = includeUserApps,
                includeSystemApps = includeSystemApps,
                includeTrackedApps = includeTrackedApps,
                includeDisabledOrFrozenApps = includeDisabledOrFrozenApps,
                sortModeId = sortMode.storageId,
                sortDirectionId = sortDirection.storageId
            )
        )
    }

    fun scrollAppListToTop() {
        coroutineScope.launch {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(
        filteredApps,
        selectedApp?.packageName,
        rememberAddPickerScroll,
        rememberedFirstVisibleItemIndex,
        rememberedFirstVisibleItemScrollOffset,
        initialAppFocusApplied,
        active,
    ) {
        if (!active) return@LaunchedEffect
        if (initialAppFocusApplied || filteredApps.isEmpty()) return@LaunchedEffect
        if (rememberAddPickerScroll && selectedApp == null) {
            listState.scrollToItem(
                rememberedFirstVisibleItemIndex.coerceIn(filteredApps.indices),
                rememberedFirstVisibleItemScrollOffset.coerceAtLeast(0)
            )
        } else {
            listState.scrollToItem(
                gitHubTrackAppCandidateInitialScrollIndex(
                    candidates = filteredApps,
                    selectedPackageName = selectedApp?.packageName
                )
            )
        }
        initialAppFocusApplied = true
    }

    LaunchedEffect(active, rememberAddPickerScroll, listState) {
        if (!active || !rememberAddPickerScroll) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                onAddAppPickerScrollPositionChange(index, offset)
            }
    }

    LaunchedEffect(active, filteredApps, listState) {
        if (!active || filteredApps.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            gitHubTrackAppPickerViewportIconPackages(
                apps = filteredApps,
                visibleItemIndices =
                    listState.layoutInfo.visibleItemsInfo.map { itemInfo -> itemInfo.index },
            )
        }
            .distinctUntilChanged()
            .collect { packageNames ->
                if (packageNames.isNotEmpty()) {
                    currentOnRequestAppIcons(packageNames)
                }
            }
    }

    SheetContentColumn(
        scrollable = false,
        verticalSpacing = 14.dp
    ) {
        GitHubTrackAppPickerHeader(
            title = stringResource(R.string.github_track_sheet_section_app_candidates),
            resultCount =
                stringResource(
                    R.string.github_track_sheet_app_result_count_format,
                    filteredApps.size,
                    appList.size,
                ),
        )
        SheetSectionCard(verticalSpacing = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLiquidSearchField(
                    value = appSearch,
                    onValueChange = { value ->
                        onAppSearchChange(value)
                        scrollAppListToTop()
                    },
                    label = stringResource(R.string.github_track_sheet_input_app_filter),
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    variant = GlassVariant.SheetInput,
                    singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val refreshContentDescription =
                        if (appListRefreshing) {
                            stringResource(R.string.common_loading)
                        } else {
                            stringResource(R.string.common_refresh)
                        }
                    AppLiquidIconButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        icon = appLucideRefreshIcon(),
                        contentDescription = refreshContentDescription,
                        enabled = !appListRefreshing,
                        onClick = onRefreshAppList,
                        width = 30.dp,
                        height = 30.dp,
                    )
                    AppLiquidTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = stringResource(R.string.github_track_sheet_btn_collapse),
                        onClick = { onPickerExpandedChange(false) },
                        minHeight = 30.dp,
                        horizontalPadding = 10.dp,
                        verticalPadding = 4.dp,
                        textMaxLines = 1,
                        textOverflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        AppLiquidExpandableSection(
            backdrop = backdrop,
            title = stringResource(R.string.github_track_sheet_app_filter_scope_label),
            subtitle =
                stringResource(
                    R.string.github_track_sheet_app_filter_summary,
                    stringResource(sortMode.labelRes),
                    stringResource(sortDirection.labelRes),
                ),
            expanded = filtersExpanded,
            onExpandedChange = { filtersExpanded = it },
        ) {
            GitHubTrackAppPickerControls(
                backdrop = backdrop,
                includeUserApps = includeUserApps,
                includeSystemApps = includeSystemApps,
                includeTrackedApps = includeTrackedApps,
                includeDisabledOrFrozenApps = includeDisabledOrFrozenApps,
                sortMode = sortMode,
                sortDirection = sortDirection,
                onIncludeUserAppsChange = {
                    includeUserApps = it
                    saveAppPickerPreferences()
                    scrollAppListToTop()
                },
                onIncludeSystemAppsChange = {
                    includeSystemApps = it
                    saveAppPickerPreferences()
                    scrollAppListToTop()
                },
                onIncludeTrackedAppsChange = {
                    includeTrackedApps = it
                    saveAppPickerPreferences()
                    scrollAppListToTop()
                },
                onIncludeDisabledOrFrozenAppsChange = {
                    includeDisabledOrFrozenApps = it
                    saveAppPickerPreferences()
                    scrollAppListToTop()
                },
                onSortModeChange = {
                    sortMode = it
                    saveAppPickerPreferences()
                    scrollAppListToTop()
                },
                onSortDirectionChange = {
                    sortDirection = it
                    saveAppPickerPreferences()
                    scrollAppListToTop()
                }
            )
        }
        selectedApp?.let { app ->
            GitHubSelectedAppCard(
                selectedApp = app,
                showInstallSource = showInstallSourcePill
            )
        }
        if (!appFilterReady) {
            LiquidInfoBlock(
                backdrop = backdrop,
                title = stringResource(R.string.github_track_sheet_label_app_list),
                subtitle = stringResource(R.string.common_loading),
                accent = MiuixTheme.colorScheme.primary,
                density = LiquidInfoBlockDensity.Compact,
            )
        } else if (filteredApps.isEmpty()) {
            LiquidInfoBlock(
                backdrop = backdrop,
                title = stringResource(R.string.github_track_sheet_label_app_list),
                subtitle = stringResource(R.string.github_track_sheet_msg_app_no_match),
                accent = MiuixTheme.colorScheme.primary,
                density = LiquidInfoBlockDensity.Compact,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = listMinHeight, max = listMaxHeight)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName },
                    contentType = { "github_app_candidate" }
                ) { app ->
                    GitHubAppCandidateRow(
                        app = app,
                        selected = selectedApp?.packageName == app.packageName,
                        showInstallSource = showInstallSourcePill,
                        onClick = {
                            onSelectedAppChange(app)
                            onPickerExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}
