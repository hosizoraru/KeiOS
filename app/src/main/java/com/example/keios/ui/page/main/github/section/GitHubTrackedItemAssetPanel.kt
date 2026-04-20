package com.example.keios.ui.page.main.github.section

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.GitHubStatusPalette
import com.example.keios.ui.page.main.github.VersionCheckUi
import com.example.keios.ui.page.main.github.asset.apkAssetTarget
import com.example.keios.ui.page.main.github.asset.assetAbiLabel
import com.example.keios.ui.page.main.github.asset.assetDisplayName
import com.example.keios.ui.page.main.github.asset.assetFileExtensionLabel
import com.example.keios.ui.page.main.github.asset.assetIsPreferredForDevice
import com.example.keios.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import com.example.keios.ui.page.main.github.asset.assetRelativeTimeLabel
import com.example.keios.ui.page.main.github.asset.bundleCommitLabel
import com.example.keios.ui.page.main.github.asset.bundleReleaseUpdatedAtMillis
import com.example.keios.ui.page.main.github.asset.bundleTransportLabel
import com.example.keios.ui.page.main.github.asset.formatAssetSize
import com.example.keios.ui.page.main.github.asset.formatReleaseUpdatedAtNoYear
import com.example.keios.ui.page.main.github.asset.prefersApiAssetTransport
import com.example.keios.ui.page.main.os.appLucideDownloadIcon
import com.example.keios.ui.page.main.os.appLucideShareIcon
import com.example.keios.ui.page.main.widget.core.AppStatusPillSize
import com.example.keios.ui.page.main.widget.core.AppSurfaceCard
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.core.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.motion.appExpandIn
import com.example.keios.ui.page.main.widget.motion.appExpandOut
import com.example.keios.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubTrackedItemAssetPanel(
    item: GitHubTrackedApp,
    state: VersionCheckUi,
    isDark: Boolean,
    contentBackdrop: LayerBackdrop,
    assetBundle: GitHubReleaseAssetBundle?,
    assetLoading: Boolean,
    assetError: String,
    assetExpanded: Boolean,
    onOpenExternalUrl: (String) -> Unit,
    onLoadApkAssets: (GitHubTrackedApp, VersionCheckUi, Boolean, Boolean) -> Unit,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit,
    context: Context,
    supportedAbis: List<String>
) {
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
                                        leadingIcon = appLucideDownloadIcon(),
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
                                        icon = appLucideShareIcon(),
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
