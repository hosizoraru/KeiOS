package com.example.keios.ui.page.main.github.section

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.AppIcon
import com.example.keios.ui.page.main.GitHubCompactInfoRow
import com.example.keios.ui.page.main.GitHubSortMode
import com.example.keios.ui.page.main.GitHubStatusPalette
import com.example.keios.ui.page.main.OverviewRefreshState
import com.example.keios.ui.page.main.VersionCheckUi
import com.example.keios.ui.page.main.VersionValueRow
import com.example.keios.ui.page.main.formatReleaseValue
import com.example.keios.ui.page.main.preReleaseVersionColor
import com.example.keios.ui.page.main.stableVersionColor
import com.example.keios.ui.page.main.statusActionUrl
import com.example.keios.ui.page.main.statusColor
import com.example.keios.ui.page.main.statusIcon
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.github.asset.apkAssetTarget
import com.example.keios.ui.page.main.github.asset.assetAbiLabel
import com.example.keios.ui.page.main.github.asset.assetDisplayName
import com.example.keios.ui.page.main.github.asset.assetFileExtensionLabel
import com.example.keios.ui.page.main.github.asset.assetRelativeTimeLabel
import com.example.keios.ui.page.main.github.asset.assetTransportLabel
import com.example.keios.ui.page.main.github.asset.bundleCommitLabel
import com.example.keios.ui.page.main.github.asset.bundleTransportLabel
import com.example.keios.ui.page.main.github.asset.formatAssetSize
import com.example.keios.ui.page.main.github.asset.prefersApiAssetTransport
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubMainContent(
    contentBottomPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    addButtonScrollConnection: NestedScrollConnection,
    topBarBackdrop: LayerBackdrop,
    contentBackdrop: LayerBackdrop,
    topBarColor: Color,
    enableSearchBar: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean,
    showSearchBar: Boolean,
    trackedSearch: String,
    sortMode: GitHubSortMode,
    showSortPopup: Boolean,
    showFloatingAddButton: Boolean,
    deleteInProgress: Boolean,
    isDark: Boolean,
    overviewRefreshState: OverviewRefreshState,
    refreshProgress: Float,
    lastRefreshMs: Long,
    lookupConfig: GitHubLookupConfig,
    overviewMetrics: GitHubOverviewMetrics,
    cardPressFeedbackEnabled: Boolean,
    trackedItems: List<GitHubTrackedApp>,
    filteredTracked: List<GitHubTrackedApp>,
    sortedTracked: List<GitHubTrackedApp>,
    checkStates: SnapshotStateMap<String, VersionCheckUi>,
    apkAssetBundles: SnapshotStateMap<String, GitHubReleaseAssetBundle>,
    apkAssetLoading: SnapshotStateMap<String, Boolean>,
    apkAssetErrors: SnapshotStateMap<String, String>,
    apkAssetExpanded: SnapshotStateMap<String, Boolean>,
    onTrackedSearchChange: (String) -> Unit,
    onShowSortPopupChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubSortMode) -> Unit,
    onOpenStrategySheet: () -> Unit,
    onOpenCheckLogicSheet: () -> Unit,
    onRefreshAllTracked: () -> Unit,
    onOpenTrackSheetForAdd: () -> Unit,
    onOpenTrackSheetForEdit: (GitHubTrackedApp) -> Unit,
    onClearApkAssetUiState: (String) -> Unit,
    onLoadApkAssets: (GitHubTrackedApp, VersionCheckUi, Boolean, Boolean) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GitHubTopBarSection(
                backdrop = topBarBackdrop,
                topBarColor = topBarColor,
                scrollBehavior = scrollBehavior,
                enableSearchBar = enableSearchBar,
                liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                showSearchBar = showSearchBar,
                trackedSearch = trackedSearch,
                sortMode = sortMode,
                showSortPopup = showSortPopup,
                deleteInProgress = deleteInProgress,
                onTrackedSearchChange = onTrackedSearchChange,
                onOpenStrategySheet = onOpenStrategySheet,
                onOpenCheckLogicSheet = onOpenCheckLogicSheet,
                onShowSortPopupChange = onShowSortPopupChange,
                onSortModeChange = onSortModeChange,
                onRefreshAllTracked = onRefreshAllTracked,
                onActionBarInteractingChanged = onActionBarInteractingChanged
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(addButtonScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 64.dp,
                    start = 12.dp,
                    end = 12.dp
                )
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Spacer(modifier = Modifier.height(2.dp)) }
                item {
                    GitHubOverviewCard(
                        isDark = isDark,
                        lookupConfig = lookupConfig,
                        overviewRefreshState = overviewRefreshState,
                        refreshProgress = refreshProgress,
                        lastRefreshMs = lastRefreshMs,
                        metrics = overviewMetrics,
                        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
                        onRefreshAllTracked = onRefreshAllTracked,
                        onOpenTrackSheetForAdd = onOpenTrackSheetForAdd
                    )
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                GitHubTrackedItemsSection(
                    trackedItems = trackedItems,
                    filteredTracked = filteredTracked,
                    sortedTracked = sortedTracked,
                    checkStates = checkStates,
                    contentBackdrop = contentBackdrop,
                    isDark = isDark,
                    apkAssetBundles = apkAssetBundles,
                    apkAssetLoading = apkAssetLoading,
                    apkAssetErrors = apkAssetErrors,
                    apkAssetExpanded = apkAssetExpanded,
                    onOpenTrackSheetForEdit = onOpenTrackSheetForEdit,
                    onClearApkAssetUiState = onClearApkAssetUiState,
                    onLoadApkAssets = onLoadApkAssets,
                    onOpenExternalUrl = onOpenExternalUrl,
                    onOpenApkInDownloader = onOpenApkInDownloader,
                    onShareApkLink = onShareApkLink,
                    context = context
                )
            }

            AnimatedVisibility(
                visible = showFloatingAddButton,
                enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it / 2 }
                ),
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
            ) {
                GlassIconButton(
                    backdrop = contentBackdrop,
                    icon = MiuixIcons.Regular.AddCircle,
                    contentDescription = stringResource(R.string.github_cd_add_track),
                    onClick = onOpenTrackSheetForAdd,
                    modifier = Modifier.padding(end = 14.dp, bottom = contentBottomPadding - 24.dp),
                    width = 60.dp,
                    height = 44.dp,
                    variant = GlassVariant.Bar
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.GitHubTrackedItemsSection(
    trackedItems: List<GitHubTrackedApp>,
    filteredTracked: List<GitHubTrackedApp>,
    sortedTracked: List<GitHubTrackedApp>,
    checkStates: SnapshotStateMap<String, VersionCheckUi>,
    contentBackdrop: LayerBackdrop,
    isDark: Boolean,
    apkAssetBundles: SnapshotStateMap<String, GitHubReleaseAssetBundle>,
    apkAssetLoading: SnapshotStateMap<String, Boolean>,
    apkAssetErrors: SnapshotStateMap<String, String>,
    apkAssetExpanded: SnapshotStateMap<String, Boolean>,
    onOpenTrackSheetForEdit: (GitHubTrackedApp) -> Unit,
    onClearApkAssetUiState: (String) -> Unit,
    onLoadApkAssets: (GitHubTrackedApp, VersionCheckUi, Boolean, Boolean) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit,
    context: Context
) {
    if (trackedItems.isEmpty()) {
        item {
            MiuixInfoItem(
                stringResource(R.string.github_list_label_track_list),
                stringResource(R.string.github_list_msg_empty)
            )
        }
    } else if (filteredTracked.isEmpty()) {
        item {
            MiuixInfoItem(
                stringResource(R.string.github_list_label_search_result),
                stringResource(R.string.github_list_msg_no_match)
            )
        }
    } else {
        items(sortedTracked, key = { it.id }) { item ->
            var expanded by remember(item.id) { mutableStateOf(false) }
            MiuixAccordionCard(
                backdrop = contentBackdrop,
                title = item.appLabel,
                subtitle = item.packageName,
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                    if (!it) {
                        onClearApkAssetUiState(item.id)
                    }
                },
                headerStartAction = {
                    AppIcon(packageName = item.packageName, size = 24.dp)
                },
                onHeaderLongClick = { onOpenTrackSheetForEdit(item) },
                headerActions = {
                    val state = checkStates[item.id] ?: VersionCheckUi()
                    val alwaysLatestReleaseDownload = item.alwaysShowLatestReleaseDownloadButton
                    val latestReleaseAccent = Color(0xFF06B6D4)
                    val statusColor = state.statusColor(
                        neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                    val statusReleaseUrl = state.statusActionUrl(
                        owner = item.owner,
                        repo = item.repo
                    )
                    val canLoadApkAssets = alwaysLatestReleaseDownload ||
                        state.hasUpdate == true ||
                        state.recommendsPreRelease ||
                        state.hasPreReleaseUpdate
                    val isAssetPanelExpanded = apkAssetExpanded[item.id] == true
                    val isAssetPanelLoading = apkAssetLoading[item.id] == true
                    val statusIcon = when {
                        alwaysLatestReleaseDownload && isAssetPanelLoading -> MiuixIcons.Regular.Refresh
                        alwaysLatestReleaseDownload && isAssetPanelExpanded -> MiuixIcons.Regular.Close
                        alwaysLatestReleaseDownload -> MiuixIcons.Regular.Download
                        isAssetPanelLoading -> MiuixIcons.Regular.Refresh
                        canLoadApkAssets && isAssetPanelExpanded -> MiuixIcons.Regular.Close
                        else -> state.statusIcon()
                    }
                    val iconTint = if (alwaysLatestReleaseDownload) latestReleaseAccent else statusColor
                    val clickableModifier = if (canLoadApkAssets || statusReleaseUrl.isNotBlank()) {
                        Modifier.clickable {
                            if (canLoadApkAssets) {
                                if (isAssetPanelExpanded) {
                                    apkAssetExpanded[item.id] = false
                                } else {
                                    onLoadApkAssets(item, state, true, false)
                                }
                            } else {
                                onOpenExternalUrl(statusReleaseUrl)
                            }
                        }
                    } else {
                        Modifier
                    }
                    top.yukonga.miuix.kmp.basic.Icon(
                        imageVector = statusIcon,
                        contentDescription = state.message.ifBlank { stringResource(R.string.github_cd_status) },
                        tint = iconTint,
                        modifier = clickableModifier
                    )
                }
            ) {
                val state = checkStates[item.id] ?: VersionCheckUi()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GitHubCompactInfoRow(
                        label = stringResource(R.string.github_item_label_repo),
                        value = "${item.owner}/${item.repo}",
                        valueColor = MiuixTheme.colorScheme.primary,
                        titleColor = MiuixTheme.colorScheme.primary,
                        onClick = {
                            onOpenExternalUrl(GitHubVersionUtils.buildReleaseUrl(item.owner, item.repo))
                        }
                    )
                    if (state.localVersion.isNotBlank()) {
                        val normalizedLocalVersion = formatReleaseValue(
                            releaseName = state.localVersion,
                            rawTag = state.localVersion
                        )
                        val localText = if (state.localVersionCode >= 0L) {
                            "$normalizedLocalVersion (${state.localVersionCode})"
                        } else {
                            normalizedLocalVersion
                        }
                        VersionValueRow(
                            label = stringResource(R.string.github_item_label_local_version),
                            value = localText,
                            valueColor = MiuixTheme.colorScheme.primary
                        )
                    }
                    if (state.hasStableRelease &&
                        (state.latestStableName.isNotBlank() ||
                            state.latestStableRawTag.isNotBlank() ||
                            state.latestTag.isNotBlank())
                    ) {
                        val latestColor = state.stableVersionColor(
                            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                        )
                        VersionValueRow(
                            label = stringResource(R.string.github_item_label_stable_version),
                            value = formatReleaseValue(
                                releaseName = state.latestStableName.ifBlank { state.latestTag },
                                rawTag = state.latestStableRawTag
                            ),
                            valueColor = latestColor,
                            emphasized = state.hasUpdate == true && !state.recommendsPreRelease
                        )
                    }
                    if (state.showPreReleaseInfo &&
                        (state.latestPreName.isNotBlank() ||
                            state.latestPreRawTag.isNotBlank() ||
                            state.preReleaseInfo.isNotBlank())
                    ) {
                        val preColor = state.preReleaseVersionColor(
                            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                        )
                        VersionValueRow(
                            label = stringResource(R.string.github_item_label_prerelease_version),
                            value = formatReleaseValue(
                                releaseName = state.latestPreName.ifBlank { state.preReleaseInfo },
                                rawTag = state.latestPreRawTag
                            ),
                            valueColor = preColor,
                            emphasized = state.recommendsPreRelease || state.hasPreReleaseUpdate
                        )
                    }
                    if (state.releaseHint.isNotBlank()) {
                        GitHubCompactInfoRow(
                            label = stringResource(R.string.github_item_label_hint),
                            value = state.releaseHint,
                            valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
                            titleColor = MiuixTheme.colorScheme.onBackgroundVariant
                        )
                    }

                    val assetBundle = apkAssetBundles[item.id]
                    val assetLoading = apkAssetLoading[item.id] == true
                    val assetError = apkAssetErrors[item.id].orEmpty()
                    val assetExpanded = apkAssetExpanded[item.id] == true
                    val alwaysLatestReleaseDownload = item.alwaysShowLatestReleaseDownloadButton
                    val latestReleaseAccent = Color(0xFF06B6D4)
                    AnimatedVisibility(
                        visible = assetExpanded || assetLoading || assetError.isNotBlank()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val target = state.apkAssetTarget(
                                owner = item.owner,
                                repo = item.repo,
                                context = context,
                                alwaysLatestRelease = alwaysLatestReleaseDownload
                            )
                            val targetAccent = when {
                                alwaysLatestReleaseDownload -> latestReleaseAccent
                                state.recommendsPreRelease || state.hasPreReleaseUpdate -> GitHubStatusPalette.PreRelease
                                else -> GitHubStatusPalette.Update
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = GitHubStatusPalette.tonedSurface(
                                        targetAccent,
                                        isDark = isDark
                                    ).copy(alpha = if (isDark) 0.30f else 0.18f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val releaseUrl = assetBundle?.htmlUrl
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?: target?.releaseUrl
                                                    .orEmpty()
                                                if (releaseUrl.isNotBlank()) {
                                                    onOpenExternalUrl(releaseUrl)
                                                }
                                            },
                                            onLongClick = {
                                                onLoadApkAssets(item, state, false, true)
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(28.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                            .background(targetAccent.copy(alpha = if (isDark) 0.24f else 0.14f)),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        top.yukonga.miuix.kmp.basic.Icon(
                                            imageVector = if (assetBundle?.showingAllAssets == true) MiuixIcons.Regular.More else MiuixIcons.Regular.Update,
                                            contentDescription = null,
                                            tint = targetAccent
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = target?.label
                                                    ?: stringResource(R.string.github_item_label_update_assets),
                                                color = targetAccent,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                modifier = Modifier.padding(start = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                            ) {
                                                val commitLabel = bundleCommitLabel(assetBundle)
                                                if (commitLabel != null && !assetLoading && assetError.isBlank()) {
                                                    StatusPill(
                                                        label = commitLabel,
                                                        color = GitHubStatusPalette.Active.copy(alpha = 0.92f),
                                                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)
                                                    )
                                                }
                                                val transportLabel = bundleTransportLabel(assetBundle, context)
                                                if (transportLabel != null && !assetLoading && assetError.isBlank()) {
                                                    StatusPill(
                                                        label = transportLabel,
                                                        color = GitHubStatusPalette.Active,
                                                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(28.dp)
                                                    .height(28.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                                                    .background(
                                                        when {
                                                            assetLoading -> GitHubStatusPalette.Active.copy(alpha = if (isDark) 0.22f else 0.16f)
                                                            assetError.isNotBlank() -> GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.22f else 0.16f)
                                                            else -> targetAccent.copy(alpha = if (isDark) 0.22f else 0.16f)
                                                        }
                                                    )
                                                    .border(
                                                        width = 0.8.dp,
                                                        color = when {
                                                            assetLoading -> GitHubStatusPalette.Active.copy(alpha = if (isDark) 0.36f else 0.30f)
                                                            assetError.isNotBlank() -> GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.36f else 0.30f)
                                                            else -> targetAccent.copy(alpha = if (isDark) 0.36f else 0.30f)
                                                        },
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                                    ),
                                                contentAlignment = androidx.compose.ui.Alignment.Center
                                            ) {
                                                Text(
                                                    text = when {
                                                        assetLoading -> stringResource(R.string.github_asset_count_loading)
                                                        assetBundle != null -> assetBundle.assets.size.toString()
                                                        assetError.isNotBlank() -> stringResource(R.string.github_asset_count_error)
                                                        else -> stringResource(R.string.github_asset_count_pending)
                                                    },
                                                    color = when {
                                                        assetLoading -> GitHubStatusPalette.Active
                                                        assetError.isNotBlank() -> GitHubStatusPalette.Error
                                                        else -> targetAccent
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Text(
                                            text = when {
                                                assetLoading -> stringResource(R.string.github_asset_hint_loading)
                                                assetBundle?.showingAllAssets == true -> stringResource(R.string.github_asset_hint_all_loaded)
                                                assetError.isNotBlank() -> stringResource(R.string.github_asset_hint_error)
                                                else -> stringResource(R.string.github_asset_hint_open_release_or_load_all)
                                            },
                                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            when {
                                assetLoading -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.defaultColors(
                                            color = if (alwaysLatestReleaseDownload) {
                                                GitHubStatusPalette.tonedSurface(
                                                    targetAccent,
                                                    isDark = isDark
                                                ).copy(alpha = if (isDark) 0.62f else 0.34f)
                                            } else {
                                                MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                progress = 0f,
                                                size = 18.dp,
                                                strokeWidth = 2.dp,
                                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                                    foregroundColor = MiuixTheme.colorScheme.primary,
                                                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                )
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.github_asset_loading_title),
                                                    color = MiuixTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = stringResource(R.string.github_asset_loading_summary),
                                                    color = MiuixTheme.colorScheme.onBackgroundVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                assetError.isNotBlank() -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.defaultColors(
                                            color = GitHubStatusPalette.tonedSurface(
                                                GitHubStatusPalette.Error,
                                                isDark = isDark
                                            ).copy(alpha = if (isDark) 0.84f else 0.96f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.github_asset_error_title),
                                                color = GitHubStatusPalette.Error,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = assetError,
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                                assetBundle != null -> {
                                    assetBundle.assets.forEach { asset ->
                                        val actionAccent = when {
                                            alwaysLatestReleaseDownload -> targetAccent
                                            prefersApiAssetTransport(asset) -> GitHubStatusPalette.Active
                                            else -> GitHubStatusPalette.Update
                                        }
                                        val abiLabel = assetAbiLabel(asset.name)
                                        val extensionLabel = assetFileExtensionLabel(asset.name)
                                        val displayName = assetDisplayName(asset.name)
                                        val sizeLabel = formatAssetSize(asset.sizeBytes, context)
                                        val relativeTimeLabel = assetRelativeTimeLabel(asset.updatedAtMillis, context)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.defaultColors(
                                                color = if (alwaysLatestReleaseDownload) {
                                                    GitHubStatusPalette.tonedSurface(
                                                        targetAccent,
                                                        isDark = isDark
                                                    ).copy(alpha = if (isDark) 0.68f else 0.36f)
                                                } else {
                                                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                                                }
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = displayName,
                                                    color = MiuixTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    FlowRow(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            if (relativeTimeLabel != null) 8.dp else 6.dp
                                                        ),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        extensionLabel?.let { label ->
                                                            StatusPill(
                                                                label = label,
                                                                color = MiuixTheme.colorScheme.primary
                                                            )
                                                        }
                                                        abiLabel?.let { label ->
                                                            StatusPill(
                                                                label = label,
                                                                color = actionAccent
                                                            )
                                                        }
                                                    }
                                                    relativeTimeLabel?.let { label ->
                                                        StatusPill(
                                                            label = label,
                                                            color = MiuixTheme.colorScheme.onBackgroundVariant
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Row(
                                                        modifier = Modifier.wrapContentWidth(androidx.compose.ui.Alignment.End),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                    ) {
                                                        GlassTextButton(
                                                            backdrop = contentBackdrop,
                                                            text = sizeLabel,
                                                            leadingIcon = MiuixIcons.Regular.Download,
                                                            onClick = { onOpenApkInDownloader(asset) },
                                                            modifier = Modifier,
                                                            variant = GlassVariant.SheetAction,
                                                            textColor = actionAccent,
                                                            iconTint = actionAccent
                                                        )
                                                        GlassIconButton(
                                                            backdrop = contentBackdrop,
                                                            icon = MiuixIcons.Regular.Share,
                                                            contentDescription = stringResource(
                                                                R.string.github_cd_share_asset,
                                                                asset.name
                                                            ),
                                                            onClick = { onShareApkLink(asset) },
                                                            width = 40.dp,
                                                            height = 40.dp,
                                                            variant = GlassVariant.SheetAction,
                                                            iconTint = actionAccent
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
