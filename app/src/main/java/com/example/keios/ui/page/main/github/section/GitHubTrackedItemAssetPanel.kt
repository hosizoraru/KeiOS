package com.example.keios.ui.page.main.github.section

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.ui.page.main.github.GitHubStatusPalette
import com.example.keios.ui.page.main.github.VersionCheckUi
import com.example.keios.ui.page.main.github.asset.apkAssetTarget
import com.example.keios.ui.page.main.widget.motion.appExpandIn
import com.example.keios.ui.page.main.widget.motion.appExpandOut
import com.kyant.backdrop.backdrops.LayerBackdrop

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
            modifier = androidx.compose.ui.Modifier
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

            GitHubTrackedItemAssetSummaryCard(
                state = state,
                assetBundle = assetBundle,
                assetLoading = assetLoading,
                assetError = assetError,
                targetLabel = target?.label
                    ?: stringResource(R.string.github_item_label_update_assets),
                targetRawTag = target?.rawTag.orEmpty(),
                fallbackReleaseUrl = target?.releaseUrl.orEmpty(),
                targetAccent = targetAccent,
                summaryContainerColor = summaryContainerColor,
                summaryBorderColor = summaryBorderColor,
                onOpenExternalUrl = onOpenExternalUrl,
                onReloadAssets = { onLoadApkAssets(item, state, false, true) },
                context = context
            )

            when {
                assetLoading -> {
                    GitHubTrackedItemAssetLoadingCard(
                        alwaysLatestReleaseDownload = alwaysLatestReleaseDownload,
                        targetAccent = targetAccent,
                        isDark = isDark
                    )
                }
                assetError.isNotBlank() -> {
                    GitHubTrackedItemAssetErrorCard(
                        assetError = assetError,
                        isDark = isDark
                    )
                }
                assetBundle != null -> {
                    assetBundle.assets.forEach { asset ->
                        GitHubTrackedItemAssetRow(
                            asset = asset,
                            alwaysLatestReleaseDownload = alwaysLatestReleaseDownload,
                            targetAccent = targetAccent,
                            summaryContainerColor = summaryContainerColor,
                            summaryBorderColor = summaryBorderColor,
                            contentBackdrop = contentBackdrop,
                            supportedAbis = supportedAbis,
                            context = context,
                            onOpenApkInDownloader = onOpenApkInDownloader,
                            onShareApkLink = onShareApkLink
                        )
                    }
                }
            }
        }
    }
}
