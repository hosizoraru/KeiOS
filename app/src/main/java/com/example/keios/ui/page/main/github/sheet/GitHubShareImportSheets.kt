package com.example.keios.ui.page.main.github.sheet

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.data.local.GitHubPendingShareImportTrackRecord
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.ui.page.main.GitHubPendingShareImportAttachCandidate
import com.example.keios.ui.page.main.GitHubShareImportPreview
import com.example.keios.ui.page.main.GitHubStatusPalette
import com.example.keios.ui.page.main.github.asset.assetIsPreferredForDevice
import com.example.keios.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import com.example.keios.ui.page.main.github.asset.formatAssetSize
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubShareImportDialog(
    preview: GitHubShareImportPreview?,
    resolving: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: (GitHubReleaseAssetFile) -> Unit
) {
    val context = LocalContext.current
    val showSheet = resolving || preview != null
    SnapshotWindowBottomSheet(
        show = showSheet,
        title = stringResource(R.string.github_share_import_dialog_title),
        onDismissRequest = onDismissRequest,
        allowDismiss = !resolving
    ) {
        val summary = when {
            resolving -> stringResource(R.string.github_share_import_dialog_summary_parsing)
            preview != null -> stringResource(
                R.string.github_share_import_dialog_summary_ready,
                preview.owner,
                preview.repo,
                preview.releaseTag
            )
            else -> null
        }
        summary?.let { text ->
            SheetDescriptionText(text = text)
        }
        if (resolving) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                Text(
                    text = stringResource(R.string.github_share_import_dialog_summary_parsing),
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
            return@SnapshotWindowBottomSheet
        }
        if (preview == null) return@SnapshotWindowBottomSheet

        val supportedAbis = remember {
            Build.SUPPORTED_ABIS?.toList().orEmpty()
        }
        val devicePreferredAssetIndex = remember(preview.assets, supportedAbis) {
            preview.assets.indexOfFirst { asset ->
                assetIsPreferredForDevice(asset.name, supportedAbis)
            }
        }
        var selectedIndex by remember(
            preview.sourceUrl,
            preview.releaseTag,
            preview.assets,
            devicePreferredAssetIndex
        ) {
            val initialIndex = when {
                preview.preferredAssetName.isNotBlank() -> preview.defaultSelectedIndex
                devicePreferredAssetIndex >= 0 -> devicePreferredAssetIndex
                else -> preview.defaultSelectedIndex
            }.coerceAtLeast(0)
            mutableIntStateOf(initialIndex)
        }
        val safeSelectedIndex = selectedIndex.coerceIn(0, preview.assets.lastIndex)
        val selectedAsset = preview.assets.getOrNull(safeSelectedIndex)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 6.dp
            ) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_dialog_label_project),
                    value = preview.projectUrl
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_dialog_label_strategy),
                    value = preview.strategyLabel
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_dialog_label_release),
                    value = preview.releaseTag
                )
            }
            SheetSectionTitle(stringResource(R.string.github_share_import_dialog_label_assets))
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalSpacing = 6.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = preview.assets,
                        key = { _, asset -> asset.name }
                    ) { index, asset ->
                        val preferredForDevice = assetIsPreferredForDevice(asset.name, supportedAbis)
                        val likelyCompatible = assetLikelyCompatibleWithDevice(asset.name, supportedAbis)
                        val compatibilityHint = when {
                            preferredForDevice -> stringResource(
                                R.string.github_share_import_dialog_asset_hint_device_recommended
                            )
                            !likelyCompatible -> stringResource(
                                R.string.github_share_import_dialog_asset_hint_maybe_incompatible
                            )
                            else -> null
                        }
                        val baseAssetSummary = stringResource(
                            R.string.github_share_import_dialog_asset_summary,
                            formatAssetSize(asset.sizeBytes, context),
                            if (asset.apiAssetUrl.isNotBlank()) {
                                stringResource(R.string.github_asset_fetch_source_api)
                            } else {
                                stringResource(R.string.github_asset_transport_direct)
                            }
                        )
                        val assetSummary = compatibilityHint?.let { hint ->
                            "$baseAssetSummary · $hint"
                        } ?: baseAssetSummary
                        val selected = safeSelectedIndex == index
                        SheetControlRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index },
                            label = asset.name,
                            summary = assetSummary
                        ) {
                            if (preferredForDevice) {
                                StatusPill(
                                    label = stringResource(
                                        R.string.github_share_import_dialog_asset_badge_recommended
                                    ),
                                    color = GitHubStatusPalette.Update
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (!likelyCompatible) {
                                StatusPill(
                                    label = stringResource(
                                        R.string.github_share_import_dialog_asset_badge_incompatible
                                    ),
                                    color = GitHubStatusPalette.PreRelease
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { selectedIndex = index }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onCancel
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.github_share_import_dialog_action_confirm),
                    colors = ButtonDefaults.textButtonColors(
                        color = GitHubStatusPalette.Active,
                        textColor = MiuixTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        selectedAsset?.let(onConfirmImport)
                    },
                    enabled = selectedAsset != null
                )
            }
        }
    }
}

@Composable
internal fun GitHubShareImportPendingDialog(
    pending: GitHubPendingShareImportTrackRecord?,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = pending != null,
        title = stringResource(R.string.github_share_import_pending_title),
        onDismissRequest = onDismissRequest,
        allowDismiss = false
    ) {
        val pendingTrack = pending ?: return@SnapshotWindowBottomSheet
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetDescriptionText(
                text = stringResource(R.string.github_share_import_pending_dialog_summary)
            )
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 6.dp
            ) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_pending_label_target),
                    value = "${pendingTrack.owner}/${pendingTrack.repo}"
                )
                if (pendingTrack.releaseTag.isNotBlank()) {
                    MiuixInfoItem(
                        key = stringResource(R.string.github_share_import_pending_label_release),
                        value = pendingTrack.releaseTag
                    )
                }
                if (pendingTrack.assetName.isNotBlank()) {
                    MiuixInfoItem(
                        key = stringResource(R.string.github_share_import_pending_label_asset),
                        value = pendingTrack.assetName
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_close),
                    onClick = onClose
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.github_share_import_pending_action_cancel),
                    colors = ButtonDefaults.textButtonColors(
                        color = GitHubStatusPalette.PreRelease,
                        textColor = MiuixTheme.colorScheme.onPrimary
                    ),
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
internal fun GitHubShareImportAttachConfirmDialog(
    candidate: GitHubPendingShareImportAttachCandidate?,
    duplicateExists: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onConfirmAndOpenGitHub: (() -> Unit)? = null
) {
    SnapshotWindowBottomSheet(
        show = candidate != null,
        title = stringResource(R.string.github_share_import_attach_dialog_title),
        onDismissRequest = onDismissRequest
    ) {
        val attachCandidate = candidate ?: return@SnapshotWindowBottomSheet
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetDescriptionText(
                text = stringResource(
                    R.string.github_share_import_attach_dialog_summary,
                    attachCandidate.owner,
                    attachCandidate.repo
                )
            )
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 6.dp
            ) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_attach_dialog_label_app),
                    value = attachCandidate.appLabel
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_attach_dialog_label_package),
                    value = attachCandidate.packageName
                )
            }
            if (duplicateExists) {
                SheetDescriptionText(
                    text = stringResource(R.string.github_share_import_attach_dialog_duplicate_hint)
                )
            }
            if (duplicateExists) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.common_close),
                        colors = ButtonDefaults.textButtonColors(
                            color = GitHubStatusPalette.Active,
                            textColor = MiuixTheme.colorScheme.onPrimary
                        ),
                        onClick = onCancel
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.common_cancel),
                        onClick = onCancel
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.github_share_import_attach_dialog_action_confirm),
                        colors = ButtonDefaults.textButtonColors(
                            color = GitHubStatusPalette.Active,
                            textColor = MiuixTheme.colorScheme.onPrimary
                        ),
                        onClick = onConfirm
                    )
                }
                if (onConfirmAndOpenGitHub != null) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            R.string.github_share_import_attach_dialog_action_confirm_and_open_github
                        ),
                        colors = ButtonDefaults.textButtonColors(
                            color = GitHubStatusPalette.Update,
                            textColor = MiuixTheme.colorScheme.onPrimary
                        ),
                        onClick = onConfirmAndOpenGitHub
                    )
                }
            }
        }
    }
}
