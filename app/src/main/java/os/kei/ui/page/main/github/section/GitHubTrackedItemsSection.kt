package os.kei.ui.page.main.github.section

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.isKeiOsSelfTrack
import os.kei.ui.page.main.github.AppIcon
import os.kei.ui.page.main.github.GitHubCompactInfoRow
import os.kei.ui.page.main.github.share.GitHubPendingShareImportTrack
import os.kei.ui.page.main.github.GitHubSortMode
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.VersionValueRow
import os.kei.ui.page.main.os.appLucideAddIcon
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.github.formatReleaseValue
import os.kei.ui.page.main.github.githubReleaseHintMessage
import os.kei.ui.page.main.github.isLocalAppUninstalled
import os.kei.ui.page.main.github.preReleaseVersionColor
import os.kei.ui.page.main.github.stableVersionColor
import os.kei.ui.page.main.github.statusActionUrl
import os.kei.ui.page.main.github.statusColor
import os.kei.ui.page.main.github.statusIcon
import os.kei.ui.page.main.github.statusMessage
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppInfoListBody
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownActionItem
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownColumn
import os.kei.ui.page.main.widget.glass.AppLiquidAccordionCard
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.github.asset.apkAssetTarget
import os.kei.ui.page.main.github.asset.assetAbiLabel
import os.kei.ui.page.main.github.asset.assetDisplayName
import os.kei.ui.page.main.github.asset.assetFileExtensionLabel
import os.kei.ui.page.main.github.asset.assetIsPreferredForDevice
import os.kei.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import os.kei.ui.page.main.github.asset.assetRelativeTimeLabel
import os.kei.ui.page.main.github.asset.bundleReleaseUpdatedAtMillis
import os.kei.ui.page.main.github.asset.bundleCommitLabel
import os.kei.ui.page.main.github.asset.bundleTransportLabel
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtCompact
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.github.asset.prefersApiAssetTransport
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.GitHubTrackedItemsSection(
    trackedItems: List<GitHubTrackedApp>,
    filteredTracked: List<GitHubTrackedApp>,
    sortedTracked: List<GitHubTrackedApp>,
    appLastUpdatedAtByTrackId: Map<String, Long>,
    checkStates: SnapshotStateMap<String, VersionCheckUi>,
    itemRefreshLoading: SnapshotStateMap<String, Boolean>,
    contentBackdrop: LayerBackdrop,
    reduceEffectsDuringListScroll: Boolean,
    isDark: Boolean,
    apkAssetBundles: SnapshotStateMap<String, GitHubReleaseAssetBundle>,
    apkAssetLoading: SnapshotStateMap<String, Boolean>,
    apkAssetErrors: SnapshotStateMap<String, String>,
    apkAssetExpanded: SnapshotStateMap<String, Boolean>,
    trackedCardExpanded: SnapshotStateMap<String, Boolean>,
    onRefreshTrackedItem: (GitHubTrackedApp) -> Unit,
    onOpenActionsSheet: (GitHubTrackedApp) -> Unit,
    onOpenTrackSheetForEdit: (GitHubTrackedApp) -> Unit,
    onRequestDeleteTrackedItem: (GitHubTrackedApp) -> Unit,
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
        val accordionBackdrop = if (reduceEffectsDuringListScroll) null else contentBackdrop
        items(
            items = sortedTracked,
            key = { it.id },
            contentType = { "tracked_app" }
        ) { item ->
            val expanded = trackedCardExpanded[item.id] == true
            AppLiquidAccordionCard(
                backdrop = accordionBackdrop,
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
                titleAccessory = {
                    if (item.isKeiOsSelfTrack()) {
                        StatusPill(
                            label = stringResource(R.string.github_track_badge_current_app),
                            color = GitHubStatusPalette.Active,
                            size = AppStatusPillSize.Compact
                        )
                    }
                },
                onHeaderLongClick = { onOpenTrackSheetForEdit(item) },
                headerActions = {
                    val state = checkStates[item.id] ?: VersionCheckUi()
                    val isItemRefreshLoading = itemRefreshLoading[item.id] == true
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
                        alwaysLatestReleaseDownload && isAssetPanelLoading -> appLucideRefreshIcon()
                        alwaysLatestReleaseDownload && isAssetPanelExpanded -> appLucideCloseIcon()
                        alwaysLatestReleaseDownload -> appLucideDownloadIcon()
                        isAssetPanelLoading -> appLucideRefreshIcon()
                        canLoadApkAssets && isAssetPanelExpanded -> appLucideCloseIcon()
                        else -> state.statusIcon()
                    }
                    val iconTint = if (alwaysLatestReleaseDownload) latestReleaseAccent else statusColor
                    AppCompactIconAction(
                        icon = statusIcon,
                        contentDescription = state.statusMessage(context)
                            .ifBlank { stringResource(R.string.github_cd_status) },
                        tint = iconTint,
                        enabled = canLoadApkAssets || statusReleaseUrl.isNotBlank(),
                        onClick = {
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
                    )
                    if (isItemRefreshLoading) {
                        val checkingContentDescription = stringResource(R.string.github_msg_checking)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .semantics {
                                    contentDescription = checkingContentDescription
                                },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            LiquidCircularProgressBar(
                                size = 16.dp,
                                strokeWidth = 2.dp,
                                activeColor = iconTint,
                                inactiveColor = iconTint.copy(alpha = 0.18f)
                            )
                        }
                    } else {
                        GitHubTrackedItemMoreActions(
                            item = item,
                            state = state,
                            iconTint = iconTint,
                            onRefreshTrackedItem = onRefreshTrackedItem,
                            onOpenActionsSheet = onOpenActionsSheet,
                            onRequestDeleteTrackedItem = onRequestDeleteTrackedItem
                        )
                    }
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
                            valueColor = if (state.isLocalAppUninstalled()) {
                                MiuixTheme.colorScheme.onBackgroundVariant
                            } else {
                                MiuixTheme.colorScheme.primary
                            }
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
                        appLastUpdatedAtByTrackId[item.id]?.takeIf { it > 0L }
                    ) ?: stringResource(R.string.common_unknown)
                    VersionValueRow(
                        label = stringResource(R.string.github_item_label_updated_at),
                        value = appUpdatedAtLabel,
                        valueColor = GitHubStatusPalette.Active
                    )
                    if (state.releaseHint.isNotBlank()) {
                        AppSupportingBlock(
                            text = githubReleaseHintMessage(context, state.releaseHint),
                            accentColor = MiuixTheme.colorScheme.onBackgroundVariant
                        )
                    }

                    val assetBundle = apkAssetBundles[item.id]
                    val assetLoading = apkAssetLoading[item.id] == true
                    val assetError = apkAssetErrors[item.id].orEmpty()
                    val assetExpanded = apkAssetExpanded[item.id] == true
                    GitHubTrackedItemAssetPanel(
                        item = item,
                        state = state,
                        isDark = isDark,
                        contentBackdrop = contentBackdrop,
                        assetBundle = assetBundle,
                        assetLoading = assetLoading,
                        assetError = assetError,
                        assetExpanded = assetExpanded,
                        onOpenExternalUrl = onOpenExternalUrl,
                        onLoadApkAssets = onLoadApkAssets,
                        onOpenApkInDownloader = onOpenApkInDownloader,
                        onShareApkLink = onShareApkLink,
                        context = context,
                        supportedAbis = supportedAbis
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubTrackedItemMoreActions(
    item: GitHubTrackedApp,
    state: VersionCheckUi,
    iconTint: Color,
    onRefreshTrackedItem: (GitHubTrackedApp) -> Unit,
    onOpenActionsSheet: (GitHubTrackedApp) -> Unit,
    onRequestDeleteTrackedItem: (GitHubTrackedApp) -> Unit
) {
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    var menuAnchorBounds by remember(item.id) { mutableStateOf<IntRect?>(null) }
    Box(
        modifier = Modifier.capturePopupAnchor { menuAnchorBounds = it },
        contentAlignment = Alignment.Center
    ) {
        AppCompactIconAction(
            icon = appLucideMoreIcon(),
            contentDescription = stringResource(R.string.github_item_cd_more_actions),
            tint = if (state.loading) iconTint.copy(alpha = 0.68f) else iconTint,
            enabled = true,
            onClick = { menuExpanded = !menuExpanded }
        )
        if (menuExpanded) {
            SnapshotWindowListPopup(
                show = true,
                alignment = PopupPositionProvider.Align.BottomEnd,
                anchorBounds = menuAnchorBounds,
                placement = SnapshotPopupPlacement.ButtonEnd,
                enableWindowDim = false,
                onDismissRequest = { menuExpanded = false }
            ) {
                LiquidGlassDropdownColumn {
                    GitHubTrackedItemMenuAction(
                        text = stringResource(R.string.common_refresh),
                        index = 0,
                        optionSize = 3,
                        onClick = {
                            menuExpanded = false
                            onRefreshTrackedItem(item)
                        }
                    )
                    GitHubTrackedItemMenuAction(
                        text = stringResource(R.string.github_actions_menu),
                        index = 1,
                        optionSize = 3,
                        onClick = {
                            menuExpanded = false
                            onOpenActionsSheet(item)
                        }
                    )
                    GitHubTrackedItemMenuAction(
                        text = stringResource(R.string.github_track_sheet_btn_delete),
                        index = 2,
                        optionSize = 3,
                        variant = GlassVariant.SheetDangerAction,
                        onClick = {
                            menuExpanded = false
                            onRequestDeleteTrackedItem(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubTrackedItemMenuAction(
    text: String,
    index: Int,
    optionSize: Int,
    onClick: () -> Unit,
    variant: GlassVariant = GlassVariant.SheetAction
) {
    LiquidGlassDropdownActionItem(
        text = text,
        onClick = onClick,
        index = index,
        optionSize = optionSize,
        variant = variant
    )
}

internal fun formatLocalVersionText(
    context: Context,
    state: VersionCheckUi
): String? {
    val rawLocalVersion = state.localVersion.trim()
    if (state.isLocalAppUninstalled()) {
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
internal fun GitHubAssetCountBubble(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val localBackdrop = rememberLayerBackdrop()
    val activeBackdrop = localBackdrop.takeIf { LocalLiquidControlsEnabled.current }
    val shape = CircleShape
    val bubbleModifier = Modifier
        .clip(shape)
        .then(
            if (activeBackdrop == null) {
                Modifier.background(color.copy(alpha = if (isDark) 0.18f else 0.12f))
            } else {
                Modifier
            }
        )
        .border(
            width = 0.8.dp,
            color = color.copy(alpha = if (isDark) 0.34f else 0.24f),
            shape = shape
        )
    val content: @Composable () -> Unit = {
        if (loading) {
            LiquidCircularProgressBar(
                size = 14.dp,
                strokeWidth = 2.dp,
                activeColor = color,
                inactiveColor = color.copy(alpha = 0.18f)
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
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        if (activeBackdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(localBackdrop)
            )
            LiquidSurface(
                backdrop = activeBackdrop,
                modifier = Modifier
                    .matchParentSize()
                    .then(bubbleModifier),
                shape = shape,
                isInteractive = false,
                surfaceColor = color.copy(alpha = if (isDark) 0.18f else 0.12f),
                blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Compact),
                lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Compact),
                shadow = false
            ) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(bubbleModifier),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}
