package com.example.keios.ui.page.main.github.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.ui.page.main.github.GitHubStatusPalette
import com.example.keios.ui.page.main.github.VersionCheckUi
import com.example.keios.ui.page.main.github.asset.bundleCommitLabel
import com.example.keios.ui.page.main.github.asset.bundleReleaseUpdatedAtMillis
import com.example.keios.ui.page.main.github.asset.bundleTransportLabel
import com.example.keios.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import com.example.keios.ui.page.main.widget.core.AppStatusPillSize
import com.example.keios.ui.page.main.widget.core.AppSurfaceCard
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.core.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubTrackedItemAssetSummaryCard(
    state: VersionCheckUi,
    assetBundle: GitHubReleaseAssetBundle?,
    assetLoading: Boolean,
    assetError: String,
    targetLabel: String,
    targetRawTag: String,
    fallbackReleaseUrl: String,
    targetAccent: Color,
    summaryContainerColor: Color,
    summaryBorderColor: Color,
    onOpenExternalUrl: (String) -> Unit,
    onReloadAssets: () -> Unit,
    context: android.content.Context
) {
    AppSurfaceCard(
        containerColor = summaryContainerColor,
        borderColor = summaryBorderColor,
        onClick = {
            val releaseUrl = assetBundle?.htmlUrl
                ?.takeIf { it.isNotBlank() }
                ?: fallbackReleaseUrl
            if (releaseUrl.isNotBlank()) {
                onOpenExternalUrl(releaseUrl)
            }
        },
        onLongClick = onReloadAssets
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
                .ifBlank { fallbackReleaseName.ifBlank { targetRawTag } }
            val loadedReleaseTag = assetBundle?.tagName?.trim().orEmpty()
                .ifBlank { targetRawTag }
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
                        text = targetLabel,
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
}
