package com.example.keios.ui.page.main.github.section

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.ui.page.main.github.GitHubStatusPalette
import com.example.keios.ui.page.main.github.asset.assetAbiLabel
import com.example.keios.ui.page.main.github.asset.assetDisplayName
import com.example.keios.ui.page.main.github.asset.assetFileExtensionLabel
import com.example.keios.ui.page.main.github.asset.assetIsPreferredForDevice
import com.example.keios.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import com.example.keios.ui.page.main.github.asset.assetRelativeTimeLabel
import com.example.keios.ui.page.main.github.asset.formatAssetSize
import com.example.keios.ui.page.main.github.asset.prefersApiAssetTransport
import com.example.keios.ui.page.main.os.appLucideDownloadIcon
import com.example.keios.ui.page.main.os.appLucideShareIcon
import com.example.keios.ui.page.main.widget.core.AppSurfaceCard
import com.example.keios.ui.page.main.widget.core.AppTypographyTokens
import com.example.keios.ui.page.main.widget.core.CardLayoutRhythm
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubTrackedItemAssetRow(
    asset: GitHubReleaseAssetFile,
    alwaysLatestReleaseDownload: Boolean,
    targetAccent: Color,
    summaryContainerColor: Color,
    summaryBorderColor: Color,
    contentBackdrop: LayerBackdrop,
    supportedAbis: List<String>,
    context: Context,
    onOpenApkInDownloader: (GitHubReleaseAssetFile) -> Unit,
    onShareApkLink: (GitHubReleaseAssetFile) -> Unit
) {
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
    AppSurfaceCard(
        containerColor = summaryContainerColor,
        borderColor = summaryBorderColor
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
                        label = stringResource(R.string.github_asset_badge_recommended),
                        color = GitHubStatusPalette.Update
                    )
                } else if (!likelyCompatible && abiLabel != null) {
                    StatusPill(
                        label = stringResource(R.string.github_asset_badge_incompatible),
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
