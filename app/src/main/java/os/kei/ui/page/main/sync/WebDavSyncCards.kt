@file:Suppress("FunctionName")

package os.kei.ui.page.main.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideDatabaseIcon
import os.kei.ui.page.main.os.appLucideFolderIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideTrashIcon
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsInfoItem
import os.kei.ui.page.main.settings.support.SettingsPickerItem
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.widget.core.AppControlRow
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppOverviewPill
import os.kei.ui.page.main.widget.core.AppOverviewPillFlow
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.AppStatusColors
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun WebDavSyncOverviewCard(
    state: WebDavSyncUiState,
    cardColor: Color,
) {
    val totals = webDavSyncTotals(state)
    val statusColor =
        when {
            state.interactionLocked -> AppStatusColors.Refreshing
            state.isConfigured -> AppStatusColors.Fresh
            else -> MiuixTheme.colorScheme.onBackgroundVariant
        }
    AppOverviewCard(
        title = stringResource(R.string.webdav_sync_overview_card_title),
        subtitle = stateSummaryMessage(state).orEmpty(),
        containerColor = cardColor,
        startAction = {
            top.yukonga.miuix.kmp.basic.Icon(
                imageVector = appLucideDatabaseIcon(),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
        headerEndActions = {
            StatusPill(
                label =
                    stringResource(
                        when {
                            state.interactionLocked -> R.string.webdav_sync_status_running
                            state.isConfigured -> R.string.webdav_sync_status_active
                            else -> R.string.webdav_sync_status_setup_required
                        },
                    ),
                color = statusColor,
            )
        },
        contentVerticalSpacing = CardLayoutRhythm.compactSectionGap,
    ) {
        AppOverviewPillFlow(
            pills =
                buildList {
                    add(
                        AppOverviewPill(
                            label =
                                stringResource(
                                    R.string.webdav_sync_metric_local,
                                    totals.localItems,
                                ),
                            color = MiuixTheme.colorScheme.primary,
                        ),
                    )
                    add(
                        AppOverviewPill(
                            label =
                                stringResource(
                                    R.string.webdav_sync_metric_enabled,
                                    totals.enabledTypes,
                                    state.itemStates.size,
                                ),
                            color =
                                if (totals.enabledTypes > 0) {
                                    AppStatusColors.Fresh
                                } else {
                                    MiuixTheme.colorScheme.onBackgroundVariant
                                },
                        ),
                    )
                    if (totals.anyRemoteKnown) {
                        add(
                            AppOverviewPill(
                                label =
                                    stringResource(
                                        R.string.webdav_sync_metric_remote,
                                        totals.remoteItems,
                                    ),
                                color = AppStatusColors.Cached,
                            ),
                        )
                    }
                },
        )
        AppSupportingBlock(
            text =
                if (totals.anyRemoteKnown) {
                    stringResource(
                        R.string.webdav_sync_totals_remote_format,
                        totals.remoteItems,
                        formatBytes(totals.remoteBytes),
                    )
                } else {
                    stringResource(R.string.webdav_sync_totals_remote_unknown)
                },
            accentColor = AppStatusColors.Cached,
            fillWidth = true,
        )
        if (state.lastFullSyncTimeMs > 0) {
            SettingsInfoItem(
                key = stringResource(R.string.webdav_sync_last_sync_label),
                value = formatTime(state.lastFullSyncTimeMs),
            )
        }
    }
}

@Composable
internal fun WebDavSyncAutoSyncCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    onToggleAutoSync: (Boolean) -> Unit,
    onAutoSyncIntervalHoursChange: (Int) -> Unit,
    onResolveAutoSyncReview: () -> Unit,
) {
    val syncReady = state.isConfigured
    val reviewItems = pendingWebDavAutoSyncReviewItems(state.itemStates)
    val reviewCount = reviewItems.size
    val canResolveReview = syncReady && !state.interactionLocked && reviewCount > 0
    val intervalOptions = remember { WebDavSyncStore.AUTO_SYNC_INTERVAL_HOUR_OPTIONS }
    val intervalLabels = intervalOptions.map { hours ->
        stringResource(R.string.webdav_sync_auto_interval_value, hours)
    }
    val selectedIntervalIndex =
        intervalOptions.indexOf(state.autoSyncIntervalHours).takeIf { it >= 0 }
            ?: intervalOptions.indexOf(WebDavSyncStore.DEFAULT_AUTO_SYNC_INTERVAL_HOURS).coerceAtLeast(0)
    var intervalExpanded by remember { mutableStateOf(false) }
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_section_data),
        title = stringResource(R.string.webdav_sync_auto_card_title),
        subtitle = stringResource(R.string.webdav_sync_auto_sync_summary),
        sectionIcon = appLucideRefreshIcon(),
        containerColor = cardColor,
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.webdav_sync_auto_sync_label),
            summary = stringResource(R.string.webdav_sync_auto_sync_summary),
            checked = state.autoSyncEnabled,
            onCheckedChange = onToggleAutoSync,
            enabled = syncReady && !state.interactionLocked,
        )
        SettingsPickerItem(
            title = stringResource(R.string.webdav_sync_auto_interval_label),
            summary = stringResource(R.string.webdav_sync_auto_interval_summary),
            onClick = {
                if (syncReady && !state.interactionLocked) {
                    intervalExpanded = true
                }
            },
            trailing = {
                AppDropdownSelector(
                    selectedText = intervalLabels.getOrElse(selectedIntervalIndex) {
                        stringResource(
                            R.string.webdav_sync_auto_interval_value,
                            state.autoSyncIntervalHours,
                        )
                    },
                    options = intervalLabels,
                    selectedIndex = selectedIntervalIndex,
                    expanded = intervalExpanded,
                    onExpandedChange = { expanded ->
                        intervalExpanded = expanded && syncReady && !state.interactionLocked
                    },
                    onSelectedIndexChange = { index ->
                        intervalOptions.getOrNull(index)?.let(onAutoSyncIntervalHoursChange)
                        intervalExpanded = false
                    },
                    popupMaxWidth = 180.dp,
                    popupMatchAnchorWidth = true,
                )
            },
        )
        state.lastAutoSyncSummary?.let { summary ->
            SettingsInfoItem(
                key = stringResource(R.string.webdav_sync_last_auto_sync_label),
                value = autoSyncSummaryText(summary, reviewCount),
            )
        }
        if (reviewCount > 0) {
            Text(
                text = stringResource(R.string.webdav_sync_auto_review_current_hint, reviewCount),
                color = MiuixTheme.colorScheme.error,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
            AppStandaloneLiquidTextButton(
                variant = if (canResolveReview) GlassVariant.SheetPrimaryAction else GlassVariant.Content,
                text = if (state.planningKind == WebDavBatchKind.Sync) {
                    stringResource(R.string.webdav_sync_refreshing_remote)
                } else {
                    stringResource(R.string.webdav_sync_auto_review_action)
                },
                modifier = Modifier.fillMaxWidth(),
                buttonModifier = Modifier.fillMaxWidth(),
                textColor = if (canResolveReview) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.52f)
                },
                enabled = canResolveReview,
                onClick = onResolveAutoSyncReview,
            )
        } else if (state.lastAutoSyncSummary?.status == WebDavAutoSyncStatus.NeedsReview) {
            Text(
                text = stringResource(R.string.webdav_sync_auto_review_resolved_hint),
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f),
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
            )
        }
    }
}

@Composable
internal fun WebDavSyncRemoteSnapshotCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    onRefreshRemote: () -> Unit,
) {
    val hasEnabledItems = state.itemStates.values.any { it.enabled }
    val syncReady = state.isConfigured
    val actionEnabled = syncReady && !state.interactionLocked
    val enabledActionTextColor = MiuixTheme.colorScheme.primary
    val disabledActionTextColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.52f)
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_section_data),
        title = stringResource(R.string.webdav_sync_remote_card_title),
        subtitle = stringResource(R.string.webdav_sync_remote_never_probed),
        sectionIcon = appLucideFolderIcon(),
        containerColor = cardColor,
    ) {
        AppStandaloneLiquidTextButton(
            variant = if (actionEnabled && hasEnabledItems) GlassVariant.SheetAction else GlassVariant.Content,
            text = if (state.refreshingRemote) {
                stringResource(R.string.webdav_sync_refreshing_remote)
            } else {
                stringResource(R.string.webdav_sync_refresh_remote)
            },
            modifier = Modifier.fillMaxWidth(),
            buttonModifier = Modifier.fillMaxWidth(),
            textColor = if (actionEnabled && hasEnabledItems) enabledActionTextColor else disabledActionTextColor,
            enabled = actionEnabled && hasEnabledItems,
            onClick = onRefreshRemote,
        )
        if (state.lastRemoteProbeTimeMs > 0L) {
            Text(
                text = stringResource(
                    R.string.webdav_sync_last_remote_probe,
                    formatTime(state.lastRemoteProbeTimeMs),
                ),
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
            )
        } else {
            Text(
                text = stringResource(R.string.webdav_sync_remote_never_probed),
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
            )
        }
    }
}

@Composable
internal fun WebDavSyncBatchActionsCard(
    state: WebDavSyncUiState,
    cardColor: Color,
    onSyncAll: () -> Unit,
    onUploadAll: () -> Unit,
    onDownloadAll: () -> Unit,
) {
    val hasEnabledItems = state.itemStates.values.any { it.enabled }
    val syncReady = state.isConfigured
    val actionEnabled = syncReady && !state.interactionLocked
    val enabledActionTextColor = MiuixTheme.colorScheme.primary
    val disabledActionTextColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.52f)
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_section_data),
        title = stringResource(R.string.webdav_sync_batch_card_title),
        subtitle = stringResource(R.string.webdav_sync_actions_contract_summary),
        sectionIcon = appLucideRefreshIcon(),
        containerColor = cardColor,
    ) {
        AppDualActionRow(
            first = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = if (actionEnabled && hasEnabledItems) GlassVariant.SheetPrimaryAction else GlassVariant.Content,
                    text =
                        when {
                            state.planningKind == WebDavBatchKind.Sync -> stringResource(R.string.webdav_sync_refreshing_remote)
                            state.runningKind == WebDavBatchKind.Sync -> stringResource(R.string.webdav_sync_syncing)
                            else -> stringResource(R.string.webdav_sync_sync_all)
                        },
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = if (actionEnabled && hasEnabledItems) enabledActionTextColor else disabledActionTextColor,
                    enabled = actionEnabled && hasEnabledItems,
                    onClick = onSyncAll,
                )
            },
            second = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = if (actionEnabled && hasEnabledItems) GlassVariant.SheetAction else GlassVariant.Content,
                    text =
                        when {
                            state.planningKind == WebDavBatchKind.Upload -> stringResource(R.string.webdav_sync_refreshing_remote)
                            state.runningKind == WebDavBatchKind.Upload -> stringResource(R.string.webdav_sync_uploading)
                            else -> stringResource(R.string.webdav_sync_upload_all)
                        },
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = if (actionEnabled && hasEnabledItems) enabledActionTextColor else disabledActionTextColor,
                    enabled = actionEnabled && hasEnabledItems,
                    onClick = onUploadAll,
                )
            },
        )
        Spacer(Modifier.height(CardLayoutRhythm.compactSectionGap))
        AppStandaloneLiquidTextButton(
            variant = if (actionEnabled && hasEnabledItems) GlassVariant.SheetAction else GlassVariant.Content,
            text =
                when {
                    state.planningKind == WebDavBatchKind.Download -> stringResource(R.string.webdav_sync_refreshing_remote)
                    state.runningKind == WebDavBatchKind.Download -> stringResource(R.string.webdav_sync_downloading)
                    else -> stringResource(R.string.webdav_sync_download_all)
                },
            modifier = Modifier.fillMaxWidth(),
            buttonModifier = Modifier.fillMaxWidth(),
            textColor = if (actionEnabled && hasEnabledItems) enabledActionTextColor else disabledActionTextColor,
            enabled = actionEnabled && hasEnabledItems,
            onClick = onDownloadAll,
        )
    }
}

@Composable
internal fun WebDavSyncItemListCard(
    titleRes: Int,
    items: List<WebDavSyncItem>,
    state: WebDavSyncUiState,
    cardColor: Color,
    onToggleItem: (WebDavSyncItem) -> Unit,
    onRunItem: (WebDavSyncItem, WebDavBatchKind) -> Unit,
) {
    SettingsGroupCard(
        header = stringResource(R.string.webdav_sync_items_title),
        title = stringResource(titleRes),
        sectionIcon = appLucideDatabaseIcon(),
        containerColor = cardColor,
    ) {
        items.forEach { item ->
            WebDavSyncItemRow(
                item = item,
                state = state,
                onToggleItem = onToggleItem,
                onRunItem = onRunItem,
            )
        }
    }
}

@Composable
internal fun WebDavClearCard(
    cardColor: Color,
    onClear: () -> Unit,
) {
    AppFeatureCard(
        title = stringResource(R.string.webdav_sync_clear_label),
        subtitle = stringResource(R.string.webdav_sync_clear_summary),
        eyebrow = stringResource(R.string.webdav_sync_section_advanced),
        sectionIcon = appLucideTrashIcon(),
        containerColor = cardColor,
        showIndication = false,
    ) {
        AppStandaloneLiquidTextButton(
            variant = GlassVariant.SheetDangerAction,
            text = stringResource(R.string.webdav_sync_clear_button),
            modifier = Modifier.fillMaxWidth(),
            buttonModifier = Modifier.fillMaxWidth(),
            textColor = MiuixTheme.colorScheme.error,
            onClick = onClear,
        )
    }
}

@Composable
internal fun WebDavAdvancedInfoCard(cardColor: Color) {
    AppFeatureCard(
        title = stringResource(R.string.webdav_sync_connection),
        subtitle = stringResource(R.string.webdav_sync_missing_config_summary),
        eyebrow = stringResource(R.string.webdav_sync_section_advanced),
        sectionIcon = appLucideInfoIcon(),
        containerColor = cardColor,
        showIndication = false,
        content = {},
    )
}

private data class WebDavSyncTotals(
    val localItems: Int,
    val enabledTypes: Int,
    val remoteItems: Int,
    val remoteBytes: Long,
    val anyRemoteKnown: Boolean,
)

private fun webDavSyncTotals(state: WebDavSyncUiState): WebDavSyncTotals {
    var localItems = 0
    var enabledTypes = 0
    var remoteItems = 0
    var remoteBytes = 0L
    var anyRemoteKnown = false
    state.itemStates.forEach { (_, itemState) ->
        if (!itemState.enabled) return@forEach
        enabledTypes += 1
        if (itemState.localCount >= 0) localItems += itemState.localCount
        val remote = itemState.remoteSummary ?: return@forEach
        if (remote.empty) {
            anyRemoteKnown = true
            return@forEach
        }
        if (remote.itemCount >= 0) {
            anyRemoteKnown = true
            remoteItems += remote.itemCount
        }
        if (remote.byteSize >= 0) remoteBytes += remote.byteSize
    }
    return WebDavSyncTotals(
        localItems = localItems,
        enabledTypes = enabledTypes,
        remoteItems = remoteItems,
        remoteBytes = remoteBytes,
        anyRemoteKnown = anyRemoteKnown,
    )
}

@Composable
private fun WebDavSyncItemRow(
    item: WebDavSyncItem,
    state: WebDavSyncUiState,
    onToggleItem: (WebDavSyncItem) -> Unit,
    onRunItem: (WebDavSyncItem, WebDavBatchKind) -> Unit,
) {
    val itemState = state.itemStates[item]
    val enabled = itemState?.enabled ?: true
    val syncReady = state.isConfigured
    val actionEnabled = enabled && syncReady && !state.interactionLocked
    val running = itemState?.running == true
    val outcome = itemState?.lastOutcome
    val pending = itemState?.pendingSummary
    val lastSync = itemState?.lastSyncTimeMs?.takeIf { it > 0 }
    val statusText = when {
        running -> stringResource(R.string.webdav_sync_item_running)
        outcome != null -> itemStatusText(outcome)
        pending != null -> pendingStatusText(pending)
        lastSync != null -> stringResource(R.string.webdav_sync_last_sync, formatTime(lastSync))
        else -> stringResource(item.descriptionRes)
    }
    val statusColor = when {
        running -> MiuixTheme.colorScheme.primary
        outcome?.isSuccess == true -> Color(0xFF22C55E)
        outcome != null -> MiuixTheme.colorScheme.error
        pending != null -> MiuixTheme.colorScheme.error
        else -> MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f)
    }
    val localCount = itemState?.localCount ?: -1
    val localLine = if (localCount >= 0) {
        stringResource(R.string.webdav_sync_local_summary_format, localCount)
    } else {
        stringResource(R.string.webdav_sync_local_summary_unknown)
    }
    val remoteLine = remoteSummaryLine(itemState?.remoteSummary)
    val remoteProbeError = itemState?.remoteProbeError
    val rowAlpha = if (enabled) 1f else 0.55f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
    ) {
        AppControlRow(
            title = stringResource(item.labelRes),
            summary = itemContractSummary(item),
            trailing = {
                AppSwitch(
                    checked = enabled,
                    onCheckedChange = { onToggleItem(item) },
                    enabled = !state.interactionLocked,
                )
            },
        )
        Text(
            text = statusText,
            color = statusColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        Text(
            text = localLine,
            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.84f),
            fontSize = AppTypographyTokens.Caption.fontSize,
            lineHeight = AppTypographyTokens.Caption.lineHeight,
        )
        Text(
            text = remoteLine,
            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.84f),
            fontSize = AppTypographyTokens.Caption.fontSize,
            lineHeight = AppTypographyTokens.Caption.lineHeight,
        )
        remoteProbeError?.let { error ->
            Text(
                text = remoteProbeErrorLine(error),
                color = MiuixTheme.colorScheme.error,
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
            )
        }
        outcome?.detail?.takeIf { it.isNotBlank() }?.let { detail ->
            Text(
                text = detail,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppStandaloneLiquidTextButton(
                variant = if (actionEnabled) GlassVariant.SheetPrimaryAction else GlassVariant.Content,
                text = stringResource(R.string.webdav_sync_item_sync_action),
                modifier = Modifier.weight(1f),
                buttonModifier = Modifier.fillMaxWidth(),
                textColor = if (actionEnabled) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.52f)
                },
                enabled = actionEnabled,
                onClick = { onRunItem(item, WebDavBatchKind.Sync) },
            )
            AppStandaloneLiquidTextButton(
                variant = if (actionEnabled) GlassVariant.SheetDangerAction else GlassVariant.Content,
                text = stringResource(R.string.webdav_sync_item_upload_action),
                modifier = Modifier.weight(1f),
                buttonModifier = Modifier.fillMaxWidth(),
                textColor = if (actionEnabled) {
                    MiuixTheme.colorScheme.error
                } else {
                    MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.52f)
                },
                enabled = actionEnabled,
                onClick = { onRunItem(item, WebDavBatchKind.Upload) },
            )
            AppStandaloneLiquidTextButton(
                variant = if (actionEnabled) GlassVariant.SheetAction else GlassVariant.Content,
                text = stringResource(R.string.webdav_sync_item_download_action),
                modifier = Modifier.weight(1f),
                buttonModifier = Modifier.fillMaxWidth(),
                textColor = if (actionEnabled) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.52f)
                },
                enabled = actionEnabled,
                onClick = { onRunItem(item, WebDavBatchKind.Download) },
            )
        }
    }
}

@Composable
internal fun WebDavFieldLabel(text: String) {
    Spacer(Modifier.height(CardLayoutRhythm.denseSectionGap))
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
        fontSize = AppTypographyTokens.Caption.fontSize,
        lineHeight = AppTypographyTokens.Caption.lineHeight,
    )
}

@Composable
private fun itemContractSummary(item: WebDavSyncItem): String = when (item) {
    WebDavSyncItem.GitHubTracked -> stringResource(R.string.webdav_sync_item_github_tracked_contract)
    WebDavSyncItem.BaAccounts -> stringResource(R.string.webdav_sync_item_ba_accounts_contract)
    WebDavSyncItem.BaCatalogFavorites -> stringResource(item.descriptionRes)
    WebDavSyncItem.BaBgmFavorites -> stringResource(item.descriptionRes)
    WebDavSyncItem.OsActivityCards -> stringResource(R.string.webdav_sync_item_os_activity_contract)
    WebDavSyncItem.OsShellCards -> stringResource(R.string.webdav_sync_item_os_shell_contract)
}

@Composable
private fun autoSyncSummaryText(
    summary: WebDavAutoSyncSummary,
    currentReviewCount: Int,
): String {
    val time =
        summary.finishedAtMs
            .takeIf { it > 0L }
            ?.let(::formatTime)
            ?: stringResource(R.string.webdav_sync_last_sync_never)
    return when (summary.status) {
        WebDavAutoSyncStatus.Success ->
            stringResource(
                R.string.webdav_sync_auto_summary_success,
                summary.succeededCount,
                summary.targetCount,
                time,
            )

        WebDavAutoSyncStatus.NeedsReview ->
            if (currentReviewCount > 0) {
                stringResource(
                    R.string.webdav_sync_auto_summary_needs_review,
                    currentReviewCount,
                    summary.succeededCount,
                    summary.targetCount,
                    time,
                )
            } else {
                stringResource(R.string.webdav_sync_auto_summary_review_resolved, time)
            }

        WebDavAutoSyncStatus.Failed ->
            stringResource(
                R.string.webdav_sync_auto_summary_failed,
                summary.failedCount,
                summary.succeededCount,
                summary.targetCount,
                time,
            )

        WebDavAutoSyncStatus.Skipped ->
            stringResource(R.string.webdav_sync_auto_summary_skipped, time)

        WebDavAutoSyncStatus.Running ->
            stringResource(R.string.webdav_sync_auto_summary_running, time)
    }
}

@Composable
private fun stateSummaryMessage(state: WebDavSyncUiState): String? {
    if (!state.isConfigured || state.missingConfig) {
        return stringResource(R.string.webdav_sync_missing_config_summary)
    }
    val enabledCount = state.itemStates.values.count { it.enabled }
    if (enabledCount == 0) return stringResource(R.string.webdav_sync_no_enabled_items_summary)
    return stringResource(
        R.string.webdav_sync_enabled_items_summary,
        enabledCount,
        state.itemStates.size,
    )
}

@Composable
private fun connectionStatusText(outcome: WebDavConnectionOutcome): String {
    val base = when (outcome.status) {
        WebDavConnectionStatus.Success -> stringResource(R.string.webdav_sync_test_success)
        WebDavConnectionStatus.SuccessDirCreated -> stringResource(R.string.webdav_sync_test_success_dir_created)
        WebDavConnectionStatus.AuthFailed -> stringResource(R.string.webdav_sync_status_auth_failed)
        WebDavConnectionStatus.PermissionDenied -> stringResource(R.string.webdav_sync_status_permission_denied)
        WebDavConnectionStatus.NetworkError -> stringResource(R.string.webdav_sync_status_network_error)
        WebDavConnectionStatus.InvalidUrl -> stringResource(R.string.webdav_sync_status_invalid_url)
        WebDavConnectionStatus.Unknown -> stringResource(R.string.webdav_sync_status_unknown)
    }
    val detail = outcome.detail?.takeIf { it.isNotBlank() }
    return if (detail != null && !outcome.isSuccess) "$base · $detail" else base
}

@Composable
private fun itemStatusText(outcome: WebDavItemOutcome): String = itemStatusText(outcome.status)

@Composable
private fun itemStatusText(status: WebDavItemStatus): String = when (status) {
    WebDavItemStatus.Uploaded -> stringResource(R.string.webdav_sync_status_uploaded)
    WebDavItemStatus.Downloaded -> stringResource(R.string.webdav_sync_status_downloaded)
    WebDavItemStatus.Merged -> stringResource(R.string.webdav_sync_status_merged)
    WebDavItemStatus.UpToDate -> stringResource(R.string.webdav_sync_status_up_to_date)
    WebDavItemStatus.RemoteEmpty -> stringResource(R.string.webdav_sync_status_remote_empty)
    WebDavItemStatus.AuthFailed -> stringResource(R.string.webdav_sync_status_auth_failed)
    WebDavItemStatus.PermissionDenied -> stringResource(R.string.webdav_sync_status_permission_denied)
    WebDavItemStatus.NetworkError -> stringResource(R.string.webdav_sync_status_network_error)
    WebDavItemStatus.ConflictUnresolved -> stringResource(R.string.webdav_sync_status_conflict)
    WebDavItemStatus.BaselineRequired -> stringResource(R.string.webdav_sync_status_baseline_required)
    WebDavItemStatus.RateLimited -> stringResource(R.string.webdav_sync_status_rate_limited)
            WebDavItemStatus.Error -> stringResource(R.string.webdav_sync_status_error)
}

@Composable
private fun pendingStatusText(summary: WebDavSyncPendingSummary): String =
    when (summary.state) {
        WebDavSyncPendingState.LocalUploadPending ->
            stringResource(R.string.webdav_sync_pending_local_upload, formatTime(summary.updatedAtMs))

        WebDavSyncPendingState.RemoteConflict ->
            stringResource(R.string.webdav_sync_pending_remote_conflict, formatTime(summary.updatedAtMs))

        WebDavSyncPendingState.BaselineRequired ->
            stringResource(R.string.webdav_sync_pending_baseline_required, formatTime(summary.updatedAtMs))
    }

@Composable
private fun remoteProbeErrorLine(outcome: WebDavItemOutcome): String {
    val base = itemStatusText(outcome)
    val detail = outcome.detail?.takeIf { it.isNotBlank() } ?: return base
    return "$base · $detail"
}

@Composable
private fun urlErrorText(error: WebDavUrlError?): String? = when (error) {
    null -> null
    WebDavUrlError.Empty -> stringResource(R.string.webdav_sync_url_error_empty)
    WebDavUrlError.Scheme -> stringResource(R.string.webdav_sync_url_error_scheme)
        WebDavUrlError.Invalid -> stringResource(R.string.webdav_sync_url_error_invalid)
}

private fun formatTime(timeMs: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
private fun remoteSummaryLine(summary: WebDavRemoteSummary?): String {
    if (summary == null) return stringResource(R.string.webdav_sync_remote_summary_unknown)
    if (summary.empty) return stringResource(R.string.webdav_sync_remote_summary_empty)
    return stringResource(
        R.string.webdav_sync_remote_summary_format,
        summary.itemCount.coerceAtLeast(0),
        formatBytes(summary.byteSize),
        formatTime(summary.probedAtMs),
    )
}

@Composable
private fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe >= 1024L * 1024L -> stringResource(R.string.webdav_sync_size_mb, safe / 1024.0 / 1024.0)
        safe >= 1024L -> stringResource(R.string.webdav_sync_size_kb, safe / 1024.0)
        else -> stringResource(R.string.webdav_sync_size_bytes, safe)
    }
}
