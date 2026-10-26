@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.core.ui.resource.resolveString
import os.kei.feature.github.model.GitHubProfileDepth
import os.kei.feature.github.model.GitHubShareImportFlowMode
import os.kei.ui.page.main.github.GitHubOverviewMetricItem
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.RefreshIntervalOption
import os.kei.ui.page.main.github.query.DownloaderOption
import os.kei.ui.page.main.github.query.OnlineShareTargetOption
import os.kei.ui.page.main.github.query.systemDownloadManagerPackageName
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionHeader
import os.kei.ui.page.main.widget.sheet.SheetSummaryCard
import os.kei.ui.testing.KeiOsTestTags
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubCheckOverviewSection(
    backdrop: Backdrop,
    logicChanged: Boolean,
    trackedCount: Int,
    selectedRefreshOption: RefreshIntervalOption,
    selectedDownloaderPackage: String,
    selectedDownloaderLabel: String,
    appManagedShareInstallEnabled: Boolean,
) {
    val saveStateLabel =
        if (logicChanged) {
            stringResource(R.string.common_pending_save)
        } else {
            stringResource(R.string.common_synced)
        }
    val saveStateColor =
        if (logicChanged) {
            GitHubStatusPalette.Cache
        } else {
            GitHubStatusPalette.Update
        }
    SheetSummaryCard(
        title = stringResource(R.string.github_check_sheet_section_overview),
        badgeLabel = saveStateLabel,
        badgeColor = saveStateColor,
    ) {
        GitHubCheckMetricRow {
            GitHubOverviewMetricItem(
                label = stringResource(R.string.github_check_sheet_metric_refresh_interval_compact),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_refresh_interval_compact,
                        selectedRefreshOption.hours,
                    ),
                modifier = Modifier.weight(1f),
                backdrop = backdrop,
                labelMaxLines = 1,
                valueMaxLines = 1,
                labelWeight = 0.38f,
                valueWeight = 0.62f,
            )
            GitHubOverviewMetricItem(
                label = stringResource(R.string.github_check_sheet_metric_track_count_compact),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count_compact,
                        trackedCount,
                    ),
                modifier = Modifier.weight(1f),
                valueColor =
                    if (trackedCount > 0) {
                        GitHubStatusPalette.Active
                    } else {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    },
                backdrop = backdrop,
                labelMaxLines = 1,
                valueMaxLines = 1,
                labelWeight = 0.38f,
                valueWeight = 0.62f,
            )
        }
        GitHubCheckMetricRow {
            GitHubOverviewMetricItem(
                label = stringResource(R.string.github_check_sheet_metric_downloader_compact),
                value =
                    if (appManagedShareInstallEnabled) {
                        stringResource(R.string.app_name)
                    } else {
                        compactDownloaderLabel(
                            packageName = selectedDownloaderPackage,
                            fallbackLabel = selectedDownloaderLabel,
                        )
                    },
                modifier = Modifier.weight(1f),
                backdrop = backdrop,
                labelMaxLines = 1,
                valueMaxLines = 1,
                labelWeight = 0.38f,
                valueWeight = 0.62f,
            )
            GitHubOverviewMetricItem(
                label = stringResource(R.string.github_check_sheet_metric_share_import_compact),
                value = stringResource(R.string.github_check_sheet_value_registered),
                modifier = Modifier.weight(1f),
                valueColor = MiuixTheme.colorScheme.primary,
                backdrop = backdrop,
                labelMaxLines = 1,
                valueMaxLines = 1,
                labelWeight = 0.38f,
                valueWeight = 0.62f,
            )
        }
    }
}

@Composable
private fun compactDownloaderLabel(
    packageName: String,
    fallbackLabel: String,
): String =
    when (packageName) {
        "" -> stringResource(R.string.github_check_sheet_value_downloader_default_compact)
        systemDownloadManagerPackageName -> stringResource(R.string.github_check_sheet_value_downloader_builtin_compact)
        else -> fallbackLabel
    }

@Composable
private fun GitHubCheckMetricRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun GitHubCheckStrategySection(
    checkAllTrackedPreReleasesInput: Boolean,
    checkAllDirectApkPreReleasesInput: Boolean,
    aggressiveApkFilteringInput: Boolean,
    preciseApkVersionEnabledInput: Boolean,
    scanSystemAppsByDefaultInput: Boolean,
    profileDepthInput: GitHubProfileDepth,
    onCheckAllTrackedPreReleasesInputChange: (Boolean) -> Unit,
    onCheckAllDirectApkPreReleasesInputChange: (Boolean) -> Unit,
    onAggressiveApkFilteringInputChange: (Boolean) -> Unit,
    onPreciseApkVersionEnabledInputChange: (Boolean) -> Unit,
    onScanSystemAppsByDefaultInputChange: (Boolean) -> Unit,
    onProfileDepthInputChange: (GitHubProfileDepth) -> Unit,
) {
    SheetSectionHeader(stringResource(R.string.github_check_sheet_section_checks))
    SheetSectionCard {
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_github_prerelease_check),
            summary = stringResource(R.string.github_check_sheet_summary_github_prerelease_check),
        ) {
            AppSwitch(
                checked = checkAllTrackedPreReleasesInput,
                onCheckedChange = onCheckAllTrackedPreReleasesInputChange,
                modifier = Modifier.testTag(KeiOsTestTags.GitHubCheckPrereleaseSwitch),
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_direct_apk_prerelease_check),
            summary = stringResource(R.string.github_check_sheet_summary_direct_apk_prerelease_check),
        ) {
            AppSwitch(
                checked = checkAllDirectApkPreReleasesInput,
                onCheckedChange = onCheckAllDirectApkPreReleasesInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_aggressive_filter),
            summary = stringResource(R.string.github_check_sheet_summary_aggressive_filter),
        ) {
            AppSwitch(
                checked = aggressiveApkFilteringInput,
                onCheckedChange = onAggressiveApkFilteringInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_precise_apk_version),
            summary = stringResource(R.string.github_check_sheet_summary_precise_apk_version),
        ) {
            AppSwitch(
                checked = preciseApkVersionEnabledInput,
                onCheckedChange = onPreciseApkVersionEnabledInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_scan_system_apps),
            summary = stringResource(R.string.github_check_sheet_summary_scan_system_apps),
        ) {
            AppSwitch(
                checked = scanSystemAppsByDefaultInput,
                onCheckedChange = onScanSystemAppsByDefaultInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_deep_profile),
            summary = stringResource(R.string.github_check_sheet_summary_deep_profile),
        ) {
            AppSwitch(
                checked = profileDepthInput == GitHubProfileDepth.Deep,
                onCheckedChange = { enabled ->
                    onProfileDepthInputChange(
                        if (enabled) GitHubProfileDepth.Deep else GitHubProfileDepth.Basic,
                    )
                },
            )
        }
    }
}

@Composable
internal fun GitHubCheckTransferSection(
    backdrop: Backdrop,
    selectedDownloaderLabel: String,
    allDownloaderOptions: List<DownloaderOption>,
    preferredDownloaderPackageInput: String,
    showDownloaderPopup: Boolean,
    shareImportFlowModeInput: GitHubShareImportFlowMode,
    appManagedShareInstallEnabledInput: Boolean,
    foregroundManagedDownloadBoostEnabledInput: Boolean,
    selectedOnlineShareTargetLabel: String,
    onlineShareTargetOptions: List<OnlineShareTargetOption>,
    installedOnlineShareTargets: List<OnlineShareTargetOption>,
    onlineShareTargetPackageInput: String,
    showOnlineShareTargetPopup: Boolean,
    showShareImportFlowModePopup: Boolean,
    onPreferredDownloaderPackageInputChange: (String) -> Unit,
    onShareImportFlowModeInputChange: (GitHubShareImportFlowMode) -> Unit,
    onAppManagedShareInstallEnabledInputChange: (Boolean) -> Unit,
    onForegroundManagedDownloadBoostEnabledInputChange: (Boolean) -> Unit,
    onOnlineShareTargetPackageInputChange: (String) -> Unit,
    onShowDownloaderPopupChange: (Boolean) -> Unit,
    onShowOnlineShareTargetPopupChange: (Boolean) -> Unit,
    onShowShareImportFlowModePopupChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val flowModeOptions = GitHubShareImportFlowMode.entries
    val flowModeLabels =
        flowModeOptions.map { mode ->
            context.resolveString(mode.labelRes())
        }
    val selectedFlowModeIndex =
        flowModeOptions
            .indexOf(shareImportFlowModeInput)
            .coerceAtLeast(0)
    SheetSectionHeader(stringResource(R.string.github_check_sheet_section_transfer))
    SheetDescriptionText(
        text = stringResource(R.string.github_check_sheet_section_transfer_summary),
    )
    SheetSectionCard {
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_share_import_linkage),
            summary = stringResource(R.string.github_check_sheet_summary_share_import_linkage),
        ) {}
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_share_import_flow_mode),
            summary = stringResource(R.string.github_check_sheet_summary_share_import_flow_mode),
        ) {
            AppDropdownSelector(
                selectedText =
                    flowModeLabels.getOrElse(selectedFlowModeIndex) {
                        flowModeLabels.firstOrNull().orEmpty()
                    },
                options = flowModeLabels,
                selectedIndex = selectedFlowModeIndex,
                expanded = showShareImportFlowModePopup,
                onExpandedChange = onShowShareImportFlowModePopupChange,
                onSelectedIndexChange = { selectedIndex ->
                    onShareImportFlowModeInputChange(flowModeOptions[selectedIndex])
                },
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_app_managed_share_install),
            summary =
                stringResource(
                    R.string.github_check_sheet_summary_app_managed_share_install,
                ),
        ) {
            AppSwitch(
                checked = appManagedShareInstallEnabledInput,
                onCheckedChange = onAppManagedShareInstallEnabledInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_foreground_download_boost),
            summary = stringResource(R.string.github_check_sheet_summary_foreground_download_boost),
        ) {
            AppSwitch(
                checked = foregroundManagedDownloadBoostEnabledInput,
                onCheckedChange = onForegroundManagedDownloadBoostEnabledInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_share_to_installer),
            summary =
                when {
                    appManagedShareInstallEnabledInput -> {
                        stringResource(R.string.github_check_sheet_summary_share_managed_by_app)
                    }

                    installedOnlineShareTargets.isNotEmpty() -> {
                        stringResource(R.string.github_check_sheet_summary_share_available)
                    }

                    else -> {
                        stringResource(R.string.github_check_sheet_summary_share_unavailable)
                    }
                },
        ) {
            AppDropdownSelector(
                selectedText = selectedOnlineShareTargetLabel,
                options =
                    if (appManagedShareInstallEnabledInput) {
                        listOf(stringResource(R.string.app_name))
                    } else {
                        onlineShareTargetOptions.map { it.label }
                    },
                selectedIndex =
                    onlineShareTargetOptions
                        .indexOfFirst {
                            onlineShareTargetPackageInput == it.packageName
                        }.coerceAtLeast(0)
                        .takeUnless { appManagedShareInstallEnabledInput } ?: 0,
                expanded = showOnlineShareTargetPopup && !appManagedShareInstallEnabledInput,
                onExpandedChange = onShowOnlineShareTargetPopupChange,
                onSelectedIndexChange = { selectedIndex ->
                    onOnlineShareTargetPackageInputChange(
                        onlineShareTargetOptions[selectedIndex].packageName,
                    )
                },
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
                enabled = !appManagedShareInstallEnabledInput,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_downloader),
            summary =
                stringResource(
                    if (appManagedShareInstallEnabledInput) {
                        R.string.github_check_sheet_summary_downloader_managed_by_app
                    } else {
                        R.string.github_check_sheet_summary_downloader
                    },
                ),
        ) {
            AppDropdownSelector(
                selectedText = selectedDownloaderLabel,
                options =
                    if (appManagedShareInstallEnabledInput) {
                        listOf(stringResource(R.string.app_name))
                    } else {
                        allDownloaderOptions.map { it.label }
                    },
                selectedIndex =
                    allDownloaderOptions
                        .indexOfFirst {
                            preferredDownloaderPackageInput == it.packageName
                        }.coerceAtLeast(0)
                        .takeUnless { appManagedShareInstallEnabledInput } ?: 0,
                expanded = showDownloaderPopup && !appManagedShareInstallEnabledInput,
                onExpandedChange = onShowDownloaderPopupChange,
                onSelectedIndexChange = { selectedIndex ->
                    onPreferredDownloaderPackageInputChange(
                        allDownloaderOptions[selectedIndex].packageName,
                    )
                },
                backdrop = backdrop,
                variant = GlassVariant.SheetAction,
                enabled = !appManagedShareInstallEnabledInput,
            )
        }
    }
}

@Composable
internal fun GitHubCheckEnhancementSection(
    decisionAssistEnabledInput: Boolean,
    repositoryHealthCardEnabledInput: Boolean,
    apkTrustCheckEnabledInput: Boolean,
    onDecisionAssistEnabledInputChange: (Boolean) -> Unit,
    onRepositoryHealthCardEnabledInputChange: (Boolean) -> Unit,
    onApkTrustCheckEnabledInputChange: (Boolean) -> Unit,
) {
    SheetSectionHeader(stringResource(R.string.github_check_sheet_section_enhancements))
    SheetSectionCard {
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_decision_assist),
            summary = stringResource(R.string.github_check_sheet_summary_decision_assist),
        ) {
            AppSwitch(
                checked = decisionAssistEnabledInput,
                onCheckedChange = onDecisionAssistEnabledInputChange,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_repository_health),
            summary = stringResource(R.string.github_check_sheet_summary_repository_health),
        ) {
            AppSwitch(
                checked = repositoryHealthCardEnabledInput,
                onCheckedChange = onRepositoryHealthCardEnabledInputChange,
                enabled = decisionAssistEnabledInput,
            )
        }
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_apk_trust),
            summary = stringResource(R.string.github_check_sheet_summary_apk_trust),
        ) {
            AppSwitch(
                checked = apkTrustCheckEnabledInput,
                onCheckedChange = onApkTrustCheckEnabledInputChange,
                enabled = decisionAssistEnabledInput,
            )
        }
    }
}

@Composable
internal fun GitHubCheckTracksSection(
    backdrop: Backdrop,
    hasKeiOsSelfTrack: Boolean,
    exportInProgress: Boolean,
    importInProgress: Boolean,
    onEnsureKeiOsSelfTrack: () -> Unit,
) {
    SheetSectionHeader(stringResource(R.string.github_check_sheet_section_tracks))
    SheetDescriptionText(
        text = stringResource(R.string.github_check_sheet_section_tracks_summary),
    )
    SheetSectionCard {
        SheetControlRow(
            label = stringResource(R.string.github_check_sheet_label_track_current_app),
            summary =
                if (hasKeiOsSelfTrack) {
                    stringResource(R.string.github_check_sheet_summary_track_current_app_exists)
                } else {
                    stringResource(R.string.github_check_sheet_summary_track_current_app_missing)
                },
        ) {
            AppLiquidTextButton(
                backdrop = backdrop,
                text =
                    if (hasKeiOsSelfTrack) {
                        stringResource(R.string.github_check_sheet_action_track_current_app_exists)
                    } else {
                        stringResource(R.string.github_check_sheet_action_track_current_app)
                    },
                onClick = onEnsureKeiOsSelfTrack,
                enabled = !hasKeiOsSelfTrack && !exportInProgress && !importInProgress,
                variant = GlassVariant.SheetAction,
            )
        }
    }
}

internal fun GitHubShareImportFlowMode.labelRes(): Int =
    when (this) {
        GitHubShareImportFlowMode.SheetAssisted -> R.string.github_share_import_flow_mode_sheet
        GitHubShareImportFlowMode.NotificationFirst -> R.string.github_share_import_flow_mode_notification
    }

@Composable
internal fun GitHubCheckNotesSection() {
    SheetSectionHeader(stringResource(R.string.github_check_sheet_section_notes))
    SheetSectionCard {
        SheetDescriptionText(
            text = stringResource(R.string.github_check_sheet_note_transfer),
        )
        SheetDescriptionText(
            text = stringResource(R.string.github_check_sheet_note_1),
        )
        SheetDescriptionText(
            text = stringResource(R.string.github_check_sheet_note_2),
        )
        SheetDescriptionText(
            text = stringResource(R.string.github_check_sheet_note_3),
        )
    }
}
