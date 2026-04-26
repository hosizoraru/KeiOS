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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.github.formatReleaseValue
import os.kei.ui.page.main.github.isLocalAppUninstalled
import os.kei.ui.page.main.github.preReleaseVersionColor
import os.kei.ui.page.main.github.stableVersionColor
import os.kei.ui.page.main.github.statusActionUrl
import os.kei.ui.page.main.github.statusColor
import os.kei.ui.page.main.github.statusIcon
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppInfoListBody
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.MiuixAccordionCard
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.motion.appFloatingEnter
import os.kei.ui.page.main.widget.motion.appFloatingExit
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
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
        val accordionBackdrop = if (reduceEffectsDuringListScroll) null else contentBackdrop
        items(
            items = sortedTracked,
            key = { it.id },
            contentType = { "tracked_app" }
        ) { item ->
            val expanded = trackedCardExpanded[item.id] == true
            MiuixAccordionCard(
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
                        contentDescription = state.message.ifBlank { stringResource(R.string.github_cd_status) },
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
                            CircularProgressIndicator(
                                progress = 0f,
                                size = 16.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = iconTint,
                                    backgroundColor = iconTint.copy(alpha = 0.18f)
                                )
                            )
                        }
                    } else {
                        AppCompactIconAction(
                            icon = appLucideRefreshIcon(),
                            contentDescription = stringResource(R.string.common_refresh),
                            tint = if (state.loading) iconTint.copy(alpha = 0.68f) else iconTint,
                            enabled = !state.loading,
                            onClick = { onRefreshTrackedItem(item) }
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
                            text = state.releaseHint,
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
