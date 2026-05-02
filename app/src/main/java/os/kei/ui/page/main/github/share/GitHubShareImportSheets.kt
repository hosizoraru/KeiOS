package os.kei.ui.page.main.github.share

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import os.kei.R
import os.kei.feature.github.data.local.GitHubPendingShareImportTrackRecord
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.assetIsPreferredForDevice
import os.kei.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SheetLiquidChoiceIndicator
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.glass.LiquidDialogActionButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val shareImportSheetInsideMargin = DpSize(
    BottomSheetDefaults.insideMargin.width,
    20.dp
)
private const val shareImportInfoLabelWeight = 0.24f

private fun Modifier.shareImportSheetSafeArea(): Modifier {
    return this
        .fillMaxWidth()
        .navigationBarsPadding()
        .imePadding()
        .padding(bottom = 12.dp)
}

@Composable
private fun ShareImportCompactInfoRow(
    key: String,
    value: String
) {
    AppInfoRow(
        label = key,
        value = value,
        labelWeight = shareImportInfoLabelWeight,
        valueWeight = 1f - shareImportInfoLabelWeight,
        valueTextAlign = TextAlign.Start,
        horizontalSpacing = 8.dp,
        rowVerticalPadding = 2.dp
    )
}

private fun compactProjectValue(preview: GitHubShareImportPreview): String {
    val owner = preview.owner.trim()
    val repo = preview.repo.trim()
    if (owner.isNotBlank() && repo.isNotBlank()) {
        return "$owner/$repo"
    }
    val rawProjectUrl = preview.projectUrl.trim()
    val compacted = rawProjectUrl
        .removePrefix("https://github.com/")
        .removePrefix("http://github.com/")
        .removePrefix("https://www.github.com/")
        .removePrefix("http://www.github.com/")
        .trim('/')
    return compacted.ifBlank { rawProjectUrl }
}

@Composable
internal fun GitHubShareImportSheet(
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
        insideMargin = shareImportSheetInsideMargin,
        allowDismiss = !resolving
    ) {
        if (resolving) {
            Column(
                modifier = Modifier
                    .shareImportSheetSafeArea()
                    .heightIn(min = 236.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LiquidCircularProgressBar(size = 24.dp)
                Spacer(modifier = Modifier.height(12.dp))
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
            modifier = Modifier.shareImportSheetSafeArea(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 4.dp
            ) {
                ShareImportCompactInfoRow(
                    key = stringResource(R.string.github_share_import_dialog_label_project),
                    value = compactProjectValue(preview)
                )
                ShareImportCompactInfoRow(
                    key = stringResource(R.string.github_share_import_dialog_label_strategy),
                    value = preview.strategyLabel
                )
                ShareImportCompactInfoRow(
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
                        val compatibilityHint = if (!likelyCompatible) {
                            stringResource(
                                R.string.github_share_import_dialog_asset_hint_maybe_incompatible
                            )
                        } else {
                            null
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
                            SheetLiquidChoiceIndicator(
                                selected = selected,
                                onSelect = { selectedIndex = index },
                                accentColor = GitHubStatusPalette.Active
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onCancel
                )
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.github_share_import_dialog_action_confirm),
                    containerColor = GitHubStatusPalette.Active,
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
internal fun GitHubShareImportPendingSheet(
    pending: GitHubPendingShareImportTrackRecord?,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = pending != null,
        title = stringResource(R.string.github_share_import_pending_title),
        onDismissRequest = onDismissRequest,
        insideMargin = shareImportSheetInsideMargin,
        allowDismiss = false
    ) {
        val pendingTrack = pending ?: return@SnapshotWindowBottomSheet
        Column(
            modifier = Modifier.shareImportSheetSafeArea(),
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
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_close),
                    onClick = onClose
                )
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.github_share_import_pending_action_cancel),
                    containerColor = GitHubStatusPalette.PreRelease,
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
internal fun GitHubShareImportAttachConfirmSheet(
    candidate: GitHubPendingShareImportAttachCandidate?,
    duplicateExists: Boolean,
    submitting: Boolean,
    submittingAndOpen: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onConfirmAndOpenGitHub: (() -> Unit)? = null
) {
    SnapshotWindowBottomSheet(
        show = candidate != null,
        title = stringResource(R.string.github_share_import_attach_dialog_title),
        onDismissRequest = onDismissRequest,
        insideMargin = shareImportSheetInsideMargin
    ) {
        val attachCandidate = candidate ?: return@SnapshotWindowBottomSheet
        Column(
            modifier = Modifier.shareImportSheetSafeArea(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 6.dp
            ) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_share_import_pending_label_target),
                    value = "${attachCandidate.owner}/${attachCandidate.repo}"
                )
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
            if (submitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidCircularProgressBar(size = 18.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (submittingAndOpen) {
                            stringResource(R.string.github_share_import_attach_dialog_processing_open)
                        } else {
                            stringResource(R.string.github_share_import_attach_dialog_processing_add)
                        },
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            }
            if (duplicateExists) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidDialogActionButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.common_close),
                        containerColor = GitHubStatusPalette.Active,
                        onClick = onCancel,
                        enabled = !submitting
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidDialogActionButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.common_cancel),
                        onClick = onCancel,
                        enabled = !submitting
                    )
                    LiquidDialogActionButton(
                        modifier = Modifier.weight(1f),
                        text = if (submitting && !submittingAndOpen) {
                            stringResource(R.string.common_processing)
                        } else {
                            stringResource(R.string.github_share_import_attach_dialog_action_confirm)
                        },
                        containerColor = GitHubStatusPalette.Active,
                        onClick = onConfirm,
                        enabled = !submitting
                    )
                }
                if (onConfirmAndOpenGitHub != null) {
                    LiquidDialogActionButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = if (submitting && submittingAndOpen) {
                            stringResource(R.string.common_processing)
                        } else {
                            stringResource(
                                R.string.github_share_import_attach_dialog_action_confirm_and_open_github
                            )
                        },
                        containerColor = GitHubStatusPalette.Update,
                        onClick = onConfirmAndOpenGitHub,
                        enabled = !submitting
                    )
                }
            }
        }
    }
}
