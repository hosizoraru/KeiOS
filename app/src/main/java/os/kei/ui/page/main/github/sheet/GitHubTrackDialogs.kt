package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.InstalledAppItem
import os.kei.ui.page.main.github.RefreshIntervalOption
import os.kei.ui.page.main.github.GitHubAppCandidateRow
import os.kei.ui.page.main.github.GitHubSelectedAppCard
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.page.GitHubTrackImportPreview
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.query.noOnlineShareTargetOption
import os.kei.ui.page.main.github.query.systemDefaultDownloaderOption
import os.kei.ui.page.main.github.query.systemDownloadManagerOption
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidDialogActionButton
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetInputTitle
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun GitHubDeleteTrackDialog(
    pendingDeleteItem: GitHubTrackedApp?,
    deleteInProgress: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    WindowDialog(
        show = pendingDeleteItem != null,
        title = stringResource(R.string.github_delete_dialog_title),
        summary = pendingDeleteItem?.let {
            stringResource(
                R.string.github_delete_dialog_summary,
                it.appLabel,
                it.owner,
                it.repo
            )
        },
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.layout.Row(
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
                    text = if (deleteInProgress) {
                        stringResource(R.string.github_delete_dialog_deleting)
                    } else {
                        stringResource(R.string.common_delete)
                    },
                    containerColor = MiuixTheme.colorScheme.error,
                    textColor = MiuixTheme.colorScheme.onError,
                    variant = GlassVariant.SheetDangerAction,
                    onClick = onConfirmDelete
                )
            }
        }
    }
}

@Composable
internal fun GitHubTrackImportDialog(
    preview: GitHubTrackImportPreview?,
    importInProgress: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: () -> Unit
) {
    WindowDialog(
        show = preview != null,
        title = stringResource(R.string.github_import_dialog_title),
        summary = preview?.let {
            stringResource(
                if (it.canImport) {
                    R.string.github_import_dialog_summary_ready
                } else {
                    R.string.github_import_dialog_summary_invalid
                }
            )
        },
        onDismissRequest = onDismissRequest
    ) {
        if (preview == null) return@WindowDialog
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 6.dp
            ) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_file_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.fileItemCount
                    )
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_valid_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.validCount
                    ),
                    valueColor = GitHubStatusPalette.Active
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_duplicate_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.duplicateCount
                    ),
                    valueColor = GitHubStatusPalette.PreRelease
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_invalid_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.invalidCount
                    ),
                    valueColor = GitHubStatusPalette.Error
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_new_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.newCount
                    ),
                    valueColor = GitHubStatusPalette.Update
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_updated_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.updatedCount
                    ),
                    valueColor = GitHubStatusPalette.Active
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_unchanged_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.unchangedCount
                    ),
                    valueColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_merged_items),
                    value = stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.mergedCount
                    ),
                    valueColor = GitHubStatusPalette.Stable
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onCancel,
                    enabled = !importInProgress
                )
                LiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = when {
                        importInProgress -> stringResource(R.string.github_check_sheet_action_importing)
                        preview.canImport -> stringResource(R.string.github_import_dialog_action_confirm)
                        else -> stringResource(R.string.common_close)
                    },
                    containerColor = if (preview.canImport) GitHubStatusPalette.Active else null,
                    onClick = if (preview.canImport) onConfirmImport else onDismissRequest,
                    enabled = !importInProgress
                )
            }
        }
    }
}
