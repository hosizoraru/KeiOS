package com.example.keios.ui.page.main.github.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.R
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.feature.github.model.InstalledAppItem
import com.example.keios.ui.page.main.RefreshIntervalOption
import com.example.keios.ui.page.main.GitHubAppCandidateRow
import com.example.keios.ui.page.main.GitHubSelectedAppCard
import com.example.keios.ui.page.main.GitHubStatusPalette
import com.example.keios.ui.page.main.github.query.DownloaderOption
import com.example.keios.ui.page.main.github.query.OnlineShareTargetOption
import com.example.keios.ui.page.main.github.query.noOnlineShareTargetOption
import com.example.keios.ui.page.main.github.query.systemDefaultDownloaderOption
import com.example.keios.ui.page.main.github.query.systemDownloadManagerOption
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetInputTitle
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun GitHubCheckLogicSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    lookupConfig: GitHubLookupConfig,
    refreshIntervalHours: Int,
    refreshIntervalHoursInput: Int,
    checkAllTrackedPreReleasesInput: Boolean,
    aggressiveApkFilteringInput: Boolean,
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
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onRefreshIntervalHoursInputChange: (Int) -> Unit,
    onCheckAllTrackedPreReleasesInputChange: (Boolean) -> Unit,
    onAggressiveApkFilteringInputChange: (Boolean) -> Unit,
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
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
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
            onlineShareTargetPackageInput != lookupConfig.onlineShareTargetPackage ||
            preferredDownloaderPackageInput != lookupConfig.preferredDownloaderPackage

        SheetContentColumn(verticalSpacing = 8.dp) {
            SheetSectionTitle(stringResource(R.string.github_check_sheet_section_summary))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_refresh_interval),
                    summary = stringResource(R.string.github_check_sheet_summary_refresh_interval)
                ) {
                    Box(
                        modifier = Modifier.capturePopupAnchor { onCheckLogicIntervalPopupAnchorBoundsChange(it) }
                    ) {
                        GlassTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = stringResource(selectedRefreshOption.labelRes),
                            onClick = { onShowCheckLogicIntervalPopupChange(!showCheckLogicIntervalPopup) }
                        )
                        if (showCheckLogicIntervalPopup) {
                            SnapshotWindowListPopup(
                                show = showCheckLogicIntervalPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = checkLogicIntervalPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { onShowCheckLogicIntervalPopupChange(false) },
                                enableWindowDim = false
                            ) {
                                LiquidDropdownColumn {
                                    val options = RefreshIntervalOption.entries
                                    options.forEachIndexed { index, option ->
                                        LiquidDropdownImpl(
                                            text = stringResource(option.labelRes),
                                            optionSize = options.size,
                                            isSelected = selectedRefreshOption == option,
                                            index = index,
                                            onSelectedIndexChange = { selectedIndex ->
                                                onRefreshIntervalHoursInputChange(options[selectedIndex].hours)
                                                onShowCheckLogicIntervalPopupChange(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_prerelease_check),
                    summary = stringResource(R.string.github_check_sheet_summary_prerelease_check)
                ) {
                    Switch(
                        checked = checkAllTrackedPreReleasesInput,
                        onCheckedChange = onCheckAllTrackedPreReleasesInputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_aggressive_filter),
                    summary = stringResource(R.string.github_check_sheet_summary_aggressive_filter)
                ) {
                    Switch(
                        checked = aggressiveApkFilteringInput,
                        onCheckedChange = onAggressiveApkFilteringInputChange
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_downloader),
                    summary = stringResource(R.string.github_check_sheet_summary_downloader)
                ) {
                    Box(
                        modifier = Modifier.capturePopupAnchor { onDownloaderPopupAnchorBoundsChange(it) }
                    ) {
                        GlassTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = selectedDownloaderLabel,
                            onClick = { onShowDownloaderPopupChange(!showDownloaderPopup) }
                        )
                        if (showDownloaderPopup) {
                            SnapshotWindowListPopup(
                                show = showDownloaderPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = downloaderPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { onShowDownloaderPopupChange(false) },
                                enableWindowDim = false
                            ) {
                                LiquidDropdownColumn {
                                    allDownloaderOptions.forEachIndexed { index, option ->
                                        LiquidDropdownImpl(
                                            text = option.label,
                                            optionSize = allDownloaderOptions.size,
                                            isSelected = preferredDownloaderPackageInput == option.packageName,
                                            index = index,
                                            onSelectedIndexChange = { selectedIndex ->
                                                onPreferredDownloaderPackageInputChange(
                                                    allDownloaderOptions[selectedIndex].packageName
                                                )
                                                onShowDownloaderPopupChange(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_share_to_installer),
                    summary = if (installedOnlineShareTargets.isNotEmpty()) {
                        stringResource(R.string.github_check_sheet_summary_share_available)
                    } else {
                        stringResource(R.string.github_check_sheet_summary_share_unavailable)
                    }
                ) {
                    Box(
                        modifier = Modifier.capturePopupAnchor {
                            onOnlineShareTargetPopupAnchorBoundsChange(it)
                        }
                    ) {
                        GlassTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = selectedOnlineShareTargetLabel,
                            onClick = { onShowOnlineShareTargetPopupChange(!showOnlineShareTargetPopup) }
                        )
                        if (showOnlineShareTargetPopup) {
                            SnapshotWindowListPopup(
                                show = showOnlineShareTargetPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = onlineShareTargetPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { onShowOnlineShareTargetPopupChange(false) },
                                enableWindowDim = false
                            ) {
                                LiquidDropdownColumn {
                                    onlineShareTargetOptions.forEachIndexed { index, option ->
                                        LiquidDropdownImpl(
                                            text = option.label,
                                            optionSize = onlineShareTargetOptions.size,
                                            isSelected = onlineShareTargetPackageInput == option.packageName,
                                            index = index,
                                            onSelectedIndexChange = { selectedIndex ->
                                                onOnlineShareTargetPackageInputChange(
                                                    onlineShareTargetOptions[selectedIndex].packageName
                                                )
                                                onShowOnlineShareTargetPopupChange(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                SheetControlRow(label = stringResource(R.string.github_check_sheet_label_save_state)) {
                    StatusPill(
                        label = if (logicChanged) {
                            stringResource(R.string.common_pending_save)
                        } else {
                            stringResource(R.string.common_synced)
                        },
                        color = if (logicChanged) {
                            GitHubStatusPalette.PreRelease
                        } else {
                            MiuixTheme.colorScheme.onBackgroundVariant
                        }
                    )
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
                SheetDescriptionText(
                    text = stringResource(R.string.github_check_sheet_note_4)
                )
                SheetDescriptionText(
                    text = stringResource(R.string.github_check_sheet_note_5)
                )
            }
        }
    }
}

@Composable
internal fun GitHubTrackEditSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    editingTrackedItem: GitHubTrackedApp?,
    repoUrlInput: String,
    appSearch: String,
    pickerExpanded: Boolean,
    selectedApp: InstalledAppItem?,
    appList: List<InstalledAppItem>,
    preferPreReleaseInput: Boolean,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onRepoUrlInputChange: (String) -> Unit,
    onAppSearchChange: (String) -> Unit,
    onPickerExpandedChange: (Boolean) -> Unit,
    onSelectedAppChange: (InstalledAppItem?) -> Unit,
    onPreferPreReleaseInputChange: (Boolean) -> Unit,
    onRequestDelete: () -> Unit
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = if (editingTrackedItem == null) {
            stringResource(R.string.github_track_sheet_title_add)
        } else {
            stringResource(R.string.github_track_sheet_title_edit)
        },
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = if (editingTrackedItem == null) {
                    stringResource(R.string.github_track_sheet_cd_confirm_add)
                } else {
                    stringResource(R.string.github_track_sheet_cd_confirm_save)
                },
                onClick = onApply
            )
        }
    ) {
        SheetContentColumn(verticalSpacing = 8.dp) {
            SheetSectionTitle(stringResource(R.string.github_track_sheet_section_repo_app))
            SheetSectionCard {
                SheetInputTitle(stringResource(R.string.github_track_sheet_input_repo))
                GlassSearchField(
                    value = repoUrlInput,
                    onValueChange = onRepoUrlInputChange,
                    label = stringResource(R.string.github_track_sheet_input_repo),
                    backdrop = backdrop,
                    variant = GlassVariant.SheetInput,
                    singleLine = true
                )
                SheetInputTitle(stringResource(R.string.github_track_sheet_input_app_filter_title))
                GlassSearchField(
                    value = appSearch,
                    onValueChange = onAppSearchChange,
                    label = stringResource(R.string.github_track_sheet_input_app_filter),
                    backdrop = backdrop,
                    variant = GlassVariant.SheetInput,
                    singleLine = true
                )
                SheetControlRow(
                    label = stringResource(R.string.github_track_sheet_label_selected_app),
                    summary = if (selectedApp == null) {
                        stringResource(R.string.github_track_sheet_selected_none)
                    } else {
                        null
                    }
                ) {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = if (pickerExpanded) {
                            stringResource(R.string.github_track_sheet_btn_collapse)
                        } else {
                            stringResource(R.string.github_track_sheet_btn_select_app)
                        },
                        onClick = { onPickerExpandedChange(!pickerExpanded) }
                    )
                }
                selectedApp?.let { app ->
                    GitHubSelectedAppCard(selectedApp = app)
                }
            }
            SheetSectionTitle(stringResource(R.string.github_track_sheet_section_check_option))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_track_sheet_label_prefer_prerelease),
                    summary = stringResource(R.string.github_track_sheet_summary_prefer_prerelease)
                ) {
                    Switch(
                        checked = preferPreReleaseInput,
                        onCheckedChange = onPreferPreReleaseInputChange
                    )
                }
            }
            if (pickerExpanded) {
                val filteredApps = appList.filter { app ->
                    appSearch.isBlank() ||
                    app.label.contains(appSearch, ignoreCase = true) ||
                        app.packageName.contains(appSearch, ignoreCase = true)
                }.take(80)
                SheetSectionTitle(stringResource(R.string.github_track_sheet_section_app_candidates))
                SheetSectionCard(verticalSpacing = 6.dp) {
                    if (filteredApps.isEmpty()) {
                        MiuixInfoItem(
                            stringResource(R.string.github_track_sheet_label_app_list),
                            stringResource(R.string.github_track_sheet_msg_app_no_match)
                        )
                    } else {
                        filteredApps.forEach { app ->
                            GitHubAppCandidateRow(
                                app = app,
                                selected = selectedApp?.packageName == app.packageName,
                                onClick = {
                                    onSelectedAppChange(app)
                                    onPickerExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }
            if (editingTrackedItem != null) {
                SheetSectionTitle(
                    text = stringResource(R.string.github_track_sheet_danger_title),
                    danger = true
                )
                SheetSectionCard {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetDangerAction,
                        text = stringResource(R.string.github_track_sheet_btn_delete),
                        textColor = MiuixTheme.colorScheme.error,
                        onClick = onRequestDelete
                    )
                }
            }
        }
    }
}

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
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_cancel),
                    onClick = onCancel
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = if (deleteInProgress) {
                        stringResource(R.string.github_delete_dialog_deleting)
                    } else {
                        stringResource(R.string.common_delete)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = onConfirmDelete
                )
            }
        }
    }
}
