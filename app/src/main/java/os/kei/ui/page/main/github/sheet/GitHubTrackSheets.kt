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
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetInputTitle
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubCheckLogicSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    lookupConfig: GitHubLookupConfig,
    trackedCount: Int,
    refreshIntervalHours: Int,
    refreshIntervalHoursInput: Int,
    checkAllTrackedPreReleasesInput: Boolean,
    aggressiveApkFilteringInput: Boolean,
    shareImportLinkageEnabledInput: Boolean,
    onlineShareTargetPackageInput: String,
    preferredDownloaderPackageInput: String,
    installedOnlineShareTargets: List<OnlineShareTargetOption>,
    showCheckLogicIntervalPopup: Boolean,
    showDownloaderPopup: Boolean,
    showOnlineShareTargetPopup: Boolean,
    checkLogicIntervalPopupAnchorBounds: IntRect?,
    downloaderPopupAnchorBounds: IntRect?,
    onlineShareTargetPopupAnchorBounds: IntRect?,
    downloaderOptions: List<DownloaderOption>,
    hasKeiOsSelfTrack: Boolean,
    exportInProgress: Boolean,
    importInProgress: Boolean,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onEnsureKeiOsSelfTrack: () -> Unit,
    onExportTrackedItems: () -> Unit,
    onImportTrackedItems: () -> Unit,
    onRefreshIntervalHoursInputChange: (Int) -> Unit,
    onCheckAllTrackedPreReleasesInputChange: (Boolean) -> Unit,
    onAggressiveApkFilteringInputChange: (Boolean) -> Unit,
    onShareImportLinkageEnabledInputChange: (Boolean) -> Unit,
    onPreferredDownloaderPackageInputChange: (String) -> Unit,
    onOnlineShareTargetPackageInputChange: (String) -> Unit,
    onShowCheckLogicIntervalPopupChange: (Boolean) -> Unit,
    onShowDownloaderPopupChange: (Boolean) -> Unit,
    onShowOnlineShareTargetPopupChange: (Boolean) -> Unit,
    onCheckLogicIntervalPopupAnchorBoundsChange: (IntRect?) -> Unit,
    onDownloaderPopupAnchorBoundsChange: (IntRect?) -> Unit,
    onOnlineShareTargetPopupAnchorBoundsChange: (IntRect?) -> Unit
) {
    val context = LocalContext.current
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.github_check_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.github_check_sheet_cd_save),
                onClick = onApply
            )
        }
    ) {
        val selectedRefreshOption = RefreshIntervalOption.fromHours(refreshIntervalHoursInput)
        val allDownloaderOptions = remember(downloaderOptions) {
            listOf(systemDefaultDownloaderOption(context), systemDownloadManagerOption(context)) + downloaderOptions
        }
        val onlineShareTargetOptions = remember(installedOnlineShareTargets) {
            listOf(noOnlineShareTargetOption(context)) + installedOnlineShareTargets
        }
        val selectedDownloaderLabel = allDownloaderOptions.firstOrNull {
            it.packageName == preferredDownloaderPackageInput
        }?.label ?: systemDefaultDownloaderOption(context).label
        val selectedOnlineShareTargetLabel = onlineShareTargetOptions.firstOrNull {
            it.packageName == onlineShareTargetPackageInput
        }?.label ?: noOnlineShareTargetOption(context).label
        val logicChanged = refreshIntervalHoursInput != refreshIntervalHours ||
            checkAllTrackedPreReleasesInput != lookupConfig.checkAllTrackedPreReleases ||
            aggressiveApkFilteringInput != lookupConfig.aggressiveApkFiltering ||
            shareImportLinkageEnabledInput != lookupConfig.shareImportLinkageEnabled ||
            onlineShareTargetPackageInput != lookupConfig.onlineShareTargetPackage ||
            preferredDownloaderPackageInput != lookupConfig.preferredDownloaderPackage

        SheetContentColumn(verticalSpacing = 8.dp) {
            SheetSectionTitle(stringResource(R.string.github_check_sheet_section_checks))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_refresh_interval),
                    summary = stringResource(R.string.github_check_sheet_summary_refresh_interval)
                ) {
                    AppDropdownSelector(
                        selectedText = stringResource(selectedRefreshOption.labelRes),
                        options = RefreshIntervalOption.entries.map { option ->
                            context.getString(option.labelRes)
                        },
                        selectedIndex = RefreshIntervalOption.entries.indexOf(selectedRefreshOption),
                        expanded = showCheckLogicIntervalPopup,
                        anchorBounds = checkLogicIntervalPopupAnchorBounds,
                        onExpandedChange = onShowCheckLogicIntervalPopupChange,
                        onSelectedIndexChange = { selectedIndex ->
                            onRefreshIntervalHoursInputChange(RefreshIntervalOption.entries[selectedIndex].hours)
                        },
                        onAnchorBoundsChange = onCheckLogicIntervalPopupAnchorBoundsChange,
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_prerelease_check),
                    summary = stringResource(R.string.github_check_sheet_summary_prerelease_check)
                ) {
                    AppSwitch(
                        checked = checkAllTrackedPreReleasesInput,
                        onCheckedChange = onCheckAllTrackedPreReleasesInputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_aggressive_filter),
                    summary = stringResource(R.string.github_check_sheet_summary_aggressive_filter)
                ) {
                    AppSwitch(
                        checked = aggressiveApkFilteringInput,
                        onCheckedChange = onAggressiveApkFilteringInputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_save_state)
                ) {
                    StatusPill(
                        label = if (logicChanged) {
                            stringResource(R.string.common_pending_save)
                        } else {
                            stringResource(R.string.common_synced)
                        },
                        color = if (logicChanged) {
                            GitHubStatusPalette.PreRelease
                        } else {
                            GitHubStatusPalette.Update
                        }
                    )
                }
            }

            SheetSectionTitle(stringResource(R.string.github_check_sheet_section_transfer))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_share_import_linkage),
                    summary = stringResource(R.string.github_check_sheet_summary_share_import_linkage)
                ) {
                    AppSwitch(
                        checked = shareImportLinkageEnabledInput,
                        onCheckedChange = onShareImportLinkageEnabledInputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_downloader),
                    summary = stringResource(R.string.github_check_sheet_summary_downloader)
                ) {
                    AppDropdownSelector(
                        selectedText = selectedDownloaderLabel,
                        options = allDownloaderOptions.map { it.label },
                        selectedIndex = allDownloaderOptions.indexOfFirst {
                            preferredDownloaderPackageInput == it.packageName
                        }.coerceAtLeast(0),
                        expanded = showDownloaderPopup,
                        anchorBounds = downloaderPopupAnchorBounds,
                        onExpandedChange = onShowDownloaderPopupChange,
                        onSelectedIndexChange = { selectedIndex ->
                            onPreferredDownloaderPackageInputChange(
                                allDownloaderOptions[selectedIndex].packageName
                            )
                        },
                        onAnchorBoundsChange = onDownloaderPopupAnchorBoundsChange,
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_share_to_installer),
                    summary = if (installedOnlineShareTargets.isNotEmpty()) {
                        stringResource(R.string.github_check_sheet_summary_share_available)
                    } else {
                        stringResource(R.string.github_check_sheet_summary_share_unavailable)
                    }
                ) {
                    AppDropdownSelector(
                        selectedText = selectedOnlineShareTargetLabel,
                        options = onlineShareTargetOptions.map { it.label },
                        selectedIndex = onlineShareTargetOptions.indexOfFirst {
                            onlineShareTargetPackageInput == it.packageName
                        }.coerceAtLeast(0),
                        expanded = showOnlineShareTargetPopup,
                        anchorBounds = onlineShareTargetPopupAnchorBounds,
                        onExpandedChange = onShowOnlineShareTargetPopupChange,
                        onSelectedIndexChange = { selectedIndex ->
                            onOnlineShareTargetPackageInputChange(
                                onlineShareTargetOptions[selectedIndex].packageName
                            )
                        },
                        onAnchorBoundsChange = onOnlineShareTargetPopupAnchorBoundsChange,
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction
                    )
                }
            }

            SheetSectionTitle(stringResource(R.string.github_check_sheet_section_tracks))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_track_count),
                    summary = stringResource(R.string.github_check_sheet_summary_track_count)
                ) {
                    StatusPill(
                        label = stringResource(
                            R.string.github_check_sheet_value_track_count,
                            trackedCount
                        ),
                        color = if (trackedCount > 0) {
                            GitHubStatusPalette.Active
                        } else {
                            MiuixTheme.colorScheme.onBackgroundVariant
                        }
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_track_current_app),
                    summary = if (hasKeiOsSelfTrack) {
                        stringResource(R.string.github_check_sheet_summary_track_current_app_exists)
                    } else {
                        stringResource(R.string.github_check_sheet_summary_track_current_app_missing)
                    }
                ) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = if (hasKeiOsSelfTrack) {
                            stringResource(R.string.github_check_sheet_action_track_current_app_exists)
                        } else {
                            stringResource(R.string.github_check_sheet_action_track_current_app)
                        },
                        onClick = onEnsureKeiOsSelfTrack,
                        enabled = !hasKeiOsSelfTrack && !exportInProgress && !importInProgress,
                        variant = GlassVariant.SheetAction
                    )
                }
                SheetInputTitle(stringResource(R.string.github_check_sheet_label_track_transfer))
                SheetDescriptionText(
                    text = stringResource(R.string.github_check_sheet_summary_track_transfer)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = if (exportInProgress) {
                                stringResource(R.string.github_check_sheet_action_exporting)
                            } else {
                                stringResource(R.string.github_check_sheet_action_export_tracks)
                            },
                            onClick = onExportTrackedItems,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !exportInProgress && !importInProgress,
                            variant = GlassVariant.SheetAction
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = if (importInProgress) {
                                stringResource(R.string.github_check_sheet_action_importing)
                            } else {
                                stringResource(R.string.github_check_sheet_action_import_tracks)
                            },
                            onClick = onImportTrackedItems,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !exportInProgress && !importInProgress,
                            variant = GlassVariant.SheetAction
                        )
                    }
                }
            }

            SheetSectionTitle(stringResource(R.string.github_check_sheet_section_notes))
            SheetSectionCard {
                SheetDescriptionText(
                    text = stringResource(R.string.github_check_sheet_note_1)
                )
                SheetDescriptionText(
                    text = stringResource(R.string.github_check_sheet_note_2)
                )
                SheetDescriptionText(
                    text = stringResource(R.string.github_check_sheet_note_3)
                )
            }
        }
    }
}
