package com.example.keios.ui.page.main.github.section

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.keios.ui.page.main.github.share.GitHubPendingShareImportTrack
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
import com.example.keios.ui.page.main.widget.AppInfoListBody
import com.example.keios.ui.page.main.widget.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.AppStatusPillSize
import com.example.keios.ui.page.main.widget.AppSupportingBlock
import com.example.keios.ui.page.main.widget.AppSurfaceCard
import com.example.keios.ui.page.main.widget.AppTypographyTokens
import com.example.keios.ui.page.main.widget.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.appExpandIn
import com.example.keios.ui.page.main.widget.appExpandOut
import com.example.keios.ui.page.main.widget.appPageBottomPaddingWithFloatingOverlay
import com.example.keios.ui.page.main.widget.appFloatingEnter
import com.example.keios.ui.page.main.widget.appFloatingExit
import com.example.keios.ui.page.main.github.asset.apkAssetTarget
import com.example.keios.ui.page.main.github.asset.assetAbiLabel
import com.example.keios.ui.page.main.github.asset.assetDisplayName
import com.example.keios.ui.page.main.github.asset.assetFileExtensionLabel
import com.example.keios.ui.page.main.github.asset.assetIsPreferredForDevice
import com.example.keios.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import com.example.keios.ui.page.main.github.asset.assetRelativeTimeLabel
import com.example.keios.ui.page.main.github.asset.assetTransportLabel
import com.example.keios.ui.page.main.github.asset.bundleReleaseUpdatedAtMillis
import com.example.keios.ui.page.main.github.asset.bundleCommitLabel
import com.example.keios.ui.page.main.github.asset.bundleTransportLabel
import com.example.keios.ui.page.main.github.asset.formatReleaseUpdatedAtCompact
import com.example.keios.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import com.example.keios.ui.page.main.github.asset.formatAssetSize
import com.example.keios.ui.page.main.github.asset.prefersApiAssetTransport
import com.kyant.backdrop.backdrops.LayerBackdrop
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
    appLastUpdatedAtByPackage: Map<String, Long>,
    checkStates: SnapshotStateMap<String, VersionCheckUi>,
    apkAssetBundles: SnapshotStateMap<String, GitHubReleaseAssetBundle>,
    apkAssetLoading: SnapshotStateMap<String, Boolean>,
    apkAssetErrors: SnapshotStateMap<String, String>,
    apkAssetExpanded: SnapshotStateMap<String, Boolean>,
    trackedCardExpanded: SnapshotStateMap<String, Boolean>,
    pendingShareImportTrack: GitHubPendingShareImportTrack?,
    showPendingShareImportCard: Boolean,
    pendingShareImportRepoOverlapCount: Int,
    onTrackedSearchChange: (String) -> Unit,
    onShowSortPopupChange: (Boolean) -> Unit,
    onSortModeChange: (GitHubSortMode) -> Unit,
    onOpenStrategySheet: () -> Unit,
    onOpenCheckLogicSheet: () -> Unit,
    onRefreshAllTracked: () -> Unit,
    onOpenTrackSheetForAdd: () -> Unit,
    onOpenTrackSheetForEdit: (GitHubTrackedApp) -> Unit,
    onClearApkAssetUiState: (String) -> Unit,
    onCollapseApkAssetPanel: (GitHubTrackedApp, VersionCheckUi) -> Unit,
    onLoadApkAssets: (GitHubTrackedApp, VersionCheckUi, Boolean, Boolean) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit,
    onCancelPendingShareImportTrack: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()
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
            AppPageLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                innerPadding = innerPadding,
                bottomExtra = appPageBottomPaddingWithFloatingOverlay(contentBottomPadding),
                topExtra = 0.dp
            ) {
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
                if (showPendingShareImportCard && pendingShareImportTrack != null) {
                    item {
                        GitHubPendingShareImportCard(
                            pending = pendingShareImportTrack,
                            repoOverlapCount = pendingShareImportRepoOverlapCount,
                            onCancel = onCancelPendingShareImportTrack
                        )
                    }
                }
                GitHubTrackedItemsSection(
                    trackedItems = trackedItems,
                    filteredTracked = filteredTracked,
                    sortedTracked = sortedTracked,
                    appLastUpdatedAtByPackage = appLastUpdatedAtByPackage,
                    checkStates = checkStates,
                    contentBackdrop = contentBackdrop,
                    isDark = isDark,
                    apkAssetBundles = apkAssetBundles,
                    apkAssetLoading = apkAssetLoading,
                    apkAssetErrors = apkAssetErrors,
                    apkAssetExpanded = apkAssetExpanded,
                    trackedCardExpanded = trackedCardExpanded,
                    onOpenTrackSheetForEdit = onOpenTrackSheetForEdit,
                    onClearApkAssetUiState = onClearApkAssetUiState,
                    onCollapseApkAssetPanel = onCollapseApkAssetPanel,
                    onLoadApkAssets = onLoadApkAssets,
                    onOpenExternalUrl = onOpenExternalUrl,
                    onOpenApkInDownloader = onOpenApkInDownloader,
                    onShareApkLink = onShareApkLink,
                    context = context,
                    supportedAbis = supportedAbis
                )
            }

            AnimatedVisibility(
                visible = showFloatingAddButton,
                enter = appFloatingEnter(),
                exit = appFloatingExit(),
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
                    containerColor = MiuixTheme.colorScheme.primary,
                    variant = GlassVariant.Floating
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
    appLastUpdatedAtByPackage: Map<String, Long>,
    checkStates: SnapshotStateMap<String, VersionCheckUi>,
    contentBackdrop: LayerBackdrop,
    isDark: Boolean,
    apkAssetBundles: SnapshotStateMap<String, GitHubReleaseAssetBundle>,
    apkAssetLoading: SnapshotStateMap<String, Boolean>,
    apkAssetErrors: SnapshotStateMap<String, String>,
    apkAssetExpanded: SnapshotStateMap<String, Boolean>,
    trackedCardExpanded: SnapshotStateMap<String, Boolean>,
    onOpenTrackSheetForEdit: (GitHubTrackedApp) -> Unit,
    onClearApkAssetUiState: (String) -> Unit,
    onCollapseApkAssetPanel: (GitHubTrackedApp, VersionCheckUi) -> Unit,
    onLoadApkAssets: (GitHubTrackedApp, VersionCheckUi, Boolean, Boolean) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit,
    context: Context,
    supportedAbis: List<String>
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
        items(
            items = sortedTracked,
            key = { it.id },
            contentType = { "tracked_app" }
        ) { item ->
            val expanded = trackedCardExpanded[item.id] == true
            MiuixAccordionCard(
                backdrop = contentBackdrop,
                title = item.appLabel,
                subtitle = item.packageName,
                expanded = expanded,
                onExpandedChange = {
                    trackedCardExpanded[item.id] = it
                    if (!it) {
                        val collapseState = checkStates[item.id] ?: VersionCheckUi()
                        if (apkAssetExpanded[item.id] == true) {
                            onCollapseApkAssetPanel(item, collapseState)
                        } else {
                            onClearApkAssetUiState(item.id)
                        }
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
                                    onCollapseApkAssetPanel(item, state)
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
                AppInfoListBody(
                    modifier = Modifier.fillMaxWidth(),
                    verticalSpacing = CardLayoutRhythm.denseSectionGap
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
                    val localText = formatLocalVersionText(context, state)
                    if (localText != null) {
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
                    val appUpdatedAtLabel = formatReleaseUpdatedAtCompact(
                        appLastUpdatedAtByPackage[item.packageName]?.takeIf { it > 0L }
                    ) ?: stringResource(R.string.common_unknown)
                    VersionValueRow(
                        label = stringResource(R.string.github_item_label_updated_at),
                        value = appUpdatedAtLabel,
                        valueColor = GitHubStatusPalette.Active
                    )
                    if (state.releaseHint.isNotBlank()) {
                        AppSupportingBlock(
                            text = state.releaseHint,
                            accentColor = MiuixTheme.colorScheme.onBackgroundVariant
                        )
                    }

                    val assetBundle = apkAssetBundles[item.id]
                    val assetLoading = apkAssetLoading[item.id] == true
                    val assetError = apkAssetErrors[item.id].orEmpty()
                    val assetExpanded = apkAssetExpanded[item.id] == true
                    val alwaysLatestReleaseDownload = item.alwaysShowLatestReleaseDownloadButton
                    val latestReleaseAccent = Color(0xFF06B6D4)
                    AnimatedVisibility(
                        visible = assetExpanded || assetLoading || assetError.isNotBlank(),
                        enter = appExpandIn(),
                        exit = appExpandOut()
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
                            val summaryContainerColor = GitHubStatusPalette.tonedSurface(
                                targetAccent,
                                isDark = isDark
                            ).copy(alpha = if (isDark) 0.30f else 0.18f)
                            val summaryBorderColor = targetAccent.copy(alpha = if (isDark) 0.30f else 0.20f)
                            AppSurfaceCard(
                                containerColor = summaryContainerColor,
                                borderColor = summaryBorderColor,
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
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = CardLayoutRhythm.cardHorizontalPadding,
                                            vertical = CardLayoutRhythm.cardVerticalPadding
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
                                    verticalAlignment = androidx.compose.ui.Alignment.Top
                                ) {
                                    val commitLabel = bundleCommitLabel(assetBundle)
                                    val transportLabel = bundleTransportLabel(assetBundle, context)
                                    val fallbackReleaseName = when {
                                        state.latestStableName.isNotBlank() -> state.latestStableName
                                        state.latestPreName.isNotBlank() -> state.latestPreName
                                        else -> ""
                                    }
                                    val loadedReleaseName = assetBundle?.releaseName?.trim().orEmpty()
                                        .ifBlank { fallbackReleaseName.ifBlank { target?.rawTag.orEmpty() } }
                                    val loadedReleaseTag = assetBundle?.tagName?.trim().orEmpty()
                                        .ifBlank { target?.rawTag.orEmpty() }
                                    val loadedReleaseUpdatedAtMillis = bundleReleaseUpdatedAtMillis(assetBundle)
                                        ?: when {
                                            loadedReleaseTag.isBlank() -> null
                                            loadedReleaseTag.equals(state.latestStableRawTag, ignoreCase = true) ->
                                                state.latestStableUpdatedAtMillis.takeIf { it > 0L }
                                            loadedReleaseTag.equals(state.latestTag, ignoreCase = true) ->
                                                state.latestStableUpdatedAtMillis.takeIf { it > 0L }
                                            loadedReleaseTag.equals(state.latestPreRawTag, ignoreCase = true) ->
                                                state.latestPreUpdatedAtMillis.takeIf { it > 0L }
                                            else -> null
                                        }
                                    val loadedReleaseUpdatedAt =
                                        formatReleaseUpdatedAtNoYear(loadedReleaseUpdatedAtMillis)
                                    val showLoadedReleaseMeta =
                                        loadedReleaseName.isNotBlank() || loadedReleaseTag.isNotBlank()
                                    val summaryMetaPillModifier = Modifier
                                    val summaryMetaPillPadding = PaddingValues(horizontal = 5.dp, vertical = 3.dp)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
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
                                                fontSize = AppTypographyTokens.CompactTitle.fontSize,
                                                lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                                                fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!assetLoading && assetError.isBlank()) {
                                                commitLabel?.let { label ->
                                                    StatusPill(
                                                        label = label,
                                                        color = GitHubStatusPalette.Active.copy(alpha = 0.92f),
                                                        size = AppStatusPillSize.Compact,
                                                        modifier = summaryMetaPillModifier,
                                                        contentPadding = summaryMetaPillPadding
                                                    )
                                                }
                                                transportLabel?.let { label ->
                                                    StatusPill(
                                                        label = label,
                                                        color = GitHubStatusPalette.Active,
                                                        size = AppStatusPillSize.Compact,
                                                        modifier = summaryMetaPillModifier,
                                                        contentPadding = summaryMetaPillPadding
                                                    )
                                                }
                                                loadedReleaseUpdatedAt?.let { label ->
                                                    StatusPill(
                                                        label = label,
                                                        color = targetAccent,
                                                        size = AppStatusPillSize.Compact,
                                                        modifier = summaryMetaPillModifier,
                                                        contentPadding = summaryMetaPillPadding
                                                    )
                                                }
                                            }
                                            GitHubAssetCountBubble(
                                                modifier = summaryMetaPillModifier,
                                                label = when {
                                                    assetBundle != null -> assetBundle.assets.size.toString()
                                                    assetError.isNotBlank() -> stringResource(R.string.github_asset_count_error)
                                                    else -> stringResource(R.string.github_asset_count_pending)
                                                },
                                                color = when {
                                                    assetError.isNotBlank() -> GitHubStatusPalette.Error
                                                    else -> targetAccent
                                                },
                                                loading = assetLoading
                                            )
                                        }
                                        if (showLoadedReleaseMeta) {
                                            val releaseNameLabel = loadedReleaseName.ifBlank {
                                                stringResource(R.string.common_unknown)
                                            }
                                            val releaseTagLabel = loadedReleaseTag.ifBlank {
                                                stringResource(R.string.common_unknown)
                                            }
                                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                val releaseNameMaxWidth = maxWidth * 0.82f
                                                val releaseTagMaxWidth = maxWidth * 0.64f
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    StatusPill(
                                                        label = releaseNameLabel,
                                                        color = targetAccent,
                                                        size = AppStatusPillSize.Compact,
                                                        modifier = Modifier.widthIn(max = releaseNameMaxWidth),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                    StatusPill(
                                                        label = releaseTagLabel,
                                                        color = targetAccent,
                                                        size = AppStatusPillSize.Compact,
                                                        modifier = Modifier.widthIn(max = releaseTagMaxWidth),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        when {
                                            assetLoading -> Text(
                                                text = stringResource(R.string.github_asset_hint_loading),
                                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            assetBundle?.showingAllAssets == true -> Text(
                                                text = stringResource(R.string.github_asset_hint_all_loaded),
                                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            assetError.isNotBlank() -> Text(
                                                text = stringResource(R.string.github_asset_hint_error),
                                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            when {
                                assetLoading -> {
                                    val stateContainerColor = if (alwaysLatestReleaseDownload) {
                                        GitHubStatusPalette.tonedSurface(
                                            targetAccent,
                                            isDark = isDark
                                        ).copy(alpha = if (isDark) 0.62f else 0.34f)
                                    } else {
                                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                                    }
                                    AppSurfaceCard(
                                        containerColor = stateContainerColor,
                                        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                                                    vertical = CardLayoutRhythm.cardVerticalPadding
                                                ),
                                            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
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
                                                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricCardTextGap)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.github_asset_loading_title),
                                                    color = MiuixTheme.colorScheme.onBackground,
                                                    fontSize = AppTypographyTokens.Body.fontSize,
                                                    lineHeight = AppTypographyTokens.Body.lineHeight,
                                                    fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight
                                                )
                                                Text(
                                                    text = stringResource(R.string.github_asset_loading_summary),
                                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                    fontSize = AppTypographyTokens.Supporting.fontSize,
                                                    lineHeight = AppTypographyTokens.Supporting.lineHeight
                                                )
                                            }
                                        }
                                    }
                                }
                                assetError.isNotBlank() -> {
                                    AppSurfaceCard(
                                        containerColor = GitHubStatusPalette.tonedSurface(
                                            GitHubStatusPalette.Error,
                                            isDark = isDark
                                        ).copy(alpha = if (isDark) 0.84f else 0.96f),
                                        borderColor = GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.34f else 0.22f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = CardLayoutRhythm.cardHorizontalPadding,
                                                    vertical = CardLayoutRhythm.cardVerticalPadding
                                                ),
                                            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.github_asset_error_title),
                                                color = GitHubStatusPalette.Error,
                                                fontSize = AppTypographyTokens.Body.fontSize,
                                                lineHeight = AppTypographyTokens.Body.lineHeight,
                                                fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight
                                            )
                                            Text(
                                                text = assetError,
                                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                                fontSize = AppTypographyTokens.Supporting.fontSize,
                                                lineHeight = AppTypographyTokens.Supporting.lineHeight
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
                                        val actionButtonColor = MiuixTheme.colorScheme.primary
                                        val abiLabel = assetAbiLabel(asset.name)
                                        val extensionLabel = assetFileExtensionLabel(asset.name)
                                        val displayName = assetDisplayName(asset.name)
                                        val sizeLabel = formatAssetSize(asset.sizeBytes, context)
                                        val relativeTimeLabel = assetRelativeTimeLabel(asset.updatedAtMillis, context)
                                        val preferredForDevice = assetIsPreferredForDevice(
                                            fileName = asset.name,
                                            supportedAbis = supportedAbis
                                        )
                                        val likelyCompatible = assetLikelyCompatibleWithDevice(
                                            fileName = asset.name,
                                            supportedAbis = supportedAbis
                                        )
                                        val assetDownloadButtonMinWidth = 100.dp
                                        val assetShareButtonSize = 40.dp
                                        val assetCardContainerColor = summaryContainerColor
                                        val assetCardBorderColor = summaryBorderColor
                                        AppSurfaceCard(
                                            containerColor = assetCardContainerColor,
                                            borderColor = assetCardBorderColor
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = CardLayoutRhythm.cardHorizontalPadding,
                                                        vertical = CardLayoutRhythm.cardVerticalPadding
                                                    ),
                                                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
                                            ) {
                                                Text(
                                                    text = displayName,
                                                    color = MiuixTheme.colorScheme.onBackground,
                                                    fontSize = AppTypographyTokens.Body.fontSize,
                                                    lineHeight = AppTypographyTokens.Body.lineHeight,
                                                    fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                                    relativeTimeLabel?.let { label ->
                                                        StatusPill(
                                                            label = label,
                                                            color = MiuixTheme.colorScheme.onBackgroundVariant
                                                        )
                                                    }
                                                    if (preferredForDevice) {
                                                        StatusPill(
                                                            label = stringResource(
                                                                R.string.github_asset_badge_recommended
                                                            ),
                                                            color = GitHubStatusPalette.Update
                                                        )
                                                    } else if (!likelyCompatible && abiLabel != null) {
                                                        StatusPill(
                                                            label = stringResource(
                                                                R.string.github_asset_badge_incompatible
                                                            ),
                                                            color = GitHubStatusPalette.PreRelease
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    GlassTextButton(
                                                        backdrop = contentBackdrop,
                                                        text = sizeLabel,
                                                        leadingIcon = MiuixIcons.Regular.Download,
                                                        onClick = { onOpenApkInDownloader(asset) },
                                                        modifier = Modifier.widthIn(min = assetDownloadButtonMinWidth),
                                                        variant = GlassVariant.SheetAction,
                                                        textColor = actionButtonColor,
                                                        iconTint = actionButtonColor,
                                                        horizontalPadding = 10.dp,
                                                        textMaxLines = 1,
                                                        textOverflow = TextOverflow.Clip,
                                                        textSoftWrap = false
                                                    )
                                                    GlassIconButton(
                                                        backdrop = contentBackdrop,
                                                        icon = MiuixIcons.Regular.Share,
                                                        contentDescription = context.getString(
                                                            R.string.github_cd_share_asset,
                                                            asset.name
                                                        ),
                                                        onClick = { onShareApkLink(asset) },
                                                        modifier = Modifier,
                                                        width = assetShareButtonSize,
                                                        height = assetShareButtonSize,
                                                        variant = GlassVariant.SheetAction,
                                                        iconTint = actionButtonColor
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
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatLocalVersionText(
    context: Context,
    state: VersionCheckUi
): String? {
    val rawLocalVersion = state.localVersion.trim()
    val hasUnknownPlaceholder = rawLocalVersion.equals("unknown", ignoreCase = true)
    val uninstalled = state.localVersionCode < 0L && (rawLocalVersion.isBlank() || hasUnknownPlaceholder)
    if (uninstalled) {
        return context.getString(R.string.github_item_value_local_version_uninstalled)
    }
    if (rawLocalVersion.isBlank()) return null
    val normalizedLocalVersion = formatReleaseValue(
        releaseName = rawLocalVersion,
        rawTag = rawLocalVersion
    )
    return if (state.localVersionCode >= 0L) {
        "$normalizedLocalVersion (${state.localVersionCode})"
    } else {
        normalizedLocalVersion
    }
}

@Composable
private fun GitHubAssetCountBubble(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isDark) 0.18f else 0.12f))
            .border(
                width = 0.8.dp,
                color = color.copy(alpha = if (isDark) 0.34f else 0.24f),
                shape = CircleShape
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                progress = 0f,
                size = 14.dp,
                strokeWidth = 2.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = color,
                    backgroundColor = color.copy(alpha = 0.18f)
                )
            )
        } else {
            Text(
                text = label,
                color = if (isDark) color else color.copy(alpha = 0.96f),
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
                fontWeight = AppTypographyTokens.Caption.fontWeight,
                maxLines = 1
            )
        }
    }
}
