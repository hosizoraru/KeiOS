@file:Suppress("FunctionName")

package os.kei.ui.page.main.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideHistoryIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppInfoListBody
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.AppStatusColors
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WebDavSyncHistorySummaryCard(
    history: List<WebDavSyncHistoryEntry>,
    cardColor: Color,
    onClearHistory: () -> Unit,
) {
    val issueCount = history.count { it.hasIssues }
    val latest = history.firstOrNull()?.finishedAtMs?.takeIf { it > 0L }?.let(::formatHistoryTime)
    AppOverviewCard(
        title = stringResource(R.string.webdav_sync_history_summary_title),
        subtitle = stringResource(R.string.webdav_sync_history_summary_desc),
        containerColor = cardColor,
        startAction = {
            top.yukonga.miuix.kmp.basic.Icon(
                imageVector = appLucideHistoryIcon(),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
        headerEndActions = {
            StatusPill(
                label = stringResource(R.string.webdav_sync_history_total_format, history.size),
                color =
                    if (issueCount > 0) {
                        MiuixTheme.colorScheme.error
                    } else {
                        AppStatusColors.Cached
                    },
                size = AppStatusPillSize.Compact,
            )
        },
        contentVerticalSpacing = CardLayoutRhythm.compactSectionGap,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
        ) {
            latest?.let { time ->
                WebDavSyncHistoryPill(
                    label = stringResource(R.string.webdav_sync_history_latest_format, time),
                    color = AppStatusColors.Cached,
                )
            }
            if (issueCount > 0) {
                WebDavSyncHistoryPill(
                    label = stringResource(R.string.webdav_sync_history_issues_format, issueCount),
                    color = MiuixTheme.colorScheme.error,
                )
            }
        }
        if (history.isNotEmpty()) {
            AppStandaloneLiquidTextButton(
                variant = GlassVariant.SheetDangerAction,
                text = stringResource(R.string.webdav_sync_history_clear),
                modifier = Modifier.fillMaxWidth(),
                buttonModifier = Modifier.fillMaxWidth(),
                textColor = MiuixTheme.colorScheme.error,
                onClick = onClearHistory,
            )
        }
    }
}

@Composable
internal fun WebDavSyncHistoryEmptyCard(cardColor: Color) {
    AppFeatureCard(
        title = stringResource(R.string.webdav_sync_history_empty_title),
        subtitle = stringResource(R.string.webdav_sync_history_empty_summary),
        eyebrow = stringResource(R.string.webdav_sync_section_history),
        sectionIcon = appLucideHistoryIcon(),
        containerColor = cardColor,
        showIndication = false,
        content = {},
    )
}

@Composable
internal fun WebDavSyncHistoryEntryCard(
    entry: WebDavSyncHistoryEntry,
    expanded: Boolean,
    cardColor: Color,
    onExpandedChange: (Boolean) -> Unit,
) {
    AppFeatureCard(
        title =
            stringResource(
                R.string.webdav_sync_history_entry_title,
                historySourceLabel(entry.source),
                historyKindLabel(entry.kind),
            ),
        subtitle =
            stringResource(
                R.string.webdav_sync_history_entry_summary,
                entry.succeededCount,
                entry.targetCount,
                entry.failedCount,
                entry.skippedCount,
            ),
        eyebrow = stringResource(R.string.webdav_sync_history_finished_format, formatHistoryTime(entry.finishedAtMs)),
        sectionIcon = appLucideHistoryIcon(),
        containerColor = cardColor,
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerEndActions = { HistoryStatusPill(entry.status) },
        contentVerticalSpacing = CardLayoutRhythm.compactSectionGap,
    ) {
        AppInfoListBody(
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppInfoRow(
                label = stringResource(R.string.webdav_sync_history_label_reason),
                value = historyReasonLabel(entry.reason),
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.webdav_sync_history_label_elapsed),
                value = formatHistoryDuration(entry.durationMs),
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.webdav_sync_history_label_progress),
                value =
                    stringResource(
                        R.string.webdav_sync_history_progress_format,
                        entry.succeededCount,
                        entry.targetCount,
                        entry.failedCount,
                        entry.skippedCount,
                    ),
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            entry.runtimeDiagnostics?.let { diagnostics ->
                AppInfoRow(
                    label = stringResource(R.string.webdav_sync_history_label_power),
                    value = historyPowerDiagnostics(diagnostics),
                    valueMaxLines = 2,
                    valueOverflow = TextOverflow.Ellipsis,
                )
                AppInfoRow(
                    label = stringResource(R.string.webdav_sync_history_label_network),
                    value = historyNetworkDiagnostics(diagnostics),
                    valueMaxLines = 2,
                    valueOverflow = TextOverflow.Ellipsis,
                )
                AppInfoRow(
                    label = stringResource(R.string.webdav_sync_history_label_scheduler),
                    value = historySchedulerDiagnostics(diagnostics),
                    valueMaxLines = 3,
                    valueOverflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.webdav_sync_history_items_empty),
                    color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.72f),
                    fontSize = AppTypographyTokens.Caption.fontSize,
                    lineHeight = AppTypographyTokens.Caption.lineHeight,
                )
            } else {
                entry.items.forEach { item ->
                    AppInfoRow(
                        label = stringResource(item.item.labelRes),
                        value = historyItemStatusLine(item),
                        valueColor = historyItemStatusColor(item.status),
                        valueMaxLines = 2,
                        valueOverflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun historyPowerDiagnostics(diagnostics: WebDavSyncRuntimeDiagnostics): String =
    buildList {
        add(
            stringResource(
                if (diagnostics.interactive) {
                    R.string.webdav_sync_history_diag_interactive
                } else {
                    R.string.webdav_sync_history_diag_screen_off
                },
            ),
        )
        if (diagnostics.deviceIdle) add(stringResource(R.string.webdav_sync_history_diag_deep_doze))
        if (diagnostics.lightDeviceIdle) add(stringResource(R.string.webdav_sync_history_diag_light_doze))
        if (diagnostics.powerSave) add(stringResource(R.string.webdav_sync_history_diag_battery_saver))
        if (diagnostics.lowPowerStandbyEnabled) {
            add(
                stringResource(
                    if (diagnostics.lowPowerStandbyExempt) {
                        R.string.webdav_sync_history_diag_low_power_standby_exempt
                    } else {
                        R.string.webdav_sync_history_diag_low_power_standby
                    },
                ),
            )
        }
        if (diagnostics.batteryOptimizationExempt) {
            add(stringResource(R.string.webdav_sync_history_diag_battery_optimization_exempt))
        }
    }.joinToString(" · ")

@Composable
private fun historyNetworkDiagnostics(diagnostics: WebDavSyncRuntimeDiagnostics): String =
    buildList {
        add(
            stringResource(
                if (diagnostics.networkPresent) {
                    R.string.webdav_sync_history_diag_network_present
                } else {
                    R.string.webdav_sync_history_diag_no_network
                },
            ),
        )
        add(
            stringResource(
                if (diagnostics.networkValidated) {
                    R.string.webdav_sync_history_diag_network_validated
                } else {
                    R.string.webdav_sync_history_diag_network_unvalidated
                },
            ),
        )
        if (!diagnostics.networkNotSuspended) add(stringResource(R.string.webdav_sync_history_diag_network_suspended))
        if (diagnostics.backgroundDataRestricted) {
            add(stringResource(R.string.webdav_sync_history_diag_background_data_restricted))
        }
    }.joinToString(" · ")

@Composable
private fun historySchedulerDiagnostics(diagnostics: WebDavSyncRuntimeDiagnostics): String =
    buildList {
        add(stringResource(R.string.webdav_sync_history_diag_bucket_format, diagnostics.appStandbyBucket))
        if (diagnostics.queuedDurationMs > 0L) {
            add(
                stringResource(
                    R.string.webdav_sync_history_diag_queued_format,
                    formatHistoryDuration(diagnostics.queuedDurationMs),
                ),
            )
        }
        if (diagnostics.pendingReasons.isNotEmpty()) add(diagnostics.pendingReasons.joinToString("/"))
        diagnostics.previousStopReason?.let {
            add(stringResource(R.string.webdav_sync_history_diag_previous_stop_format, it))
        }
    }.joinToString(" · ")

@Composable
private fun WebDavSyncHistoryPill(
    label: String,
    color: Color,
) {
    StatusPill(
        label = label,
        color = color,
        size = AppStatusPillSize.Compact,
        backgroundAlphaOverride = 0.16f,
        borderAlphaOverride = 0.30f,
    )
}

@Composable
private fun RowScope.HistoryStatusPill(status: WebDavAutoSyncStatus) {
    StatusPill(
        label = historyStatusLabel(status),
        color = historyStatusColor(status),
        size = AppStatusPillSize.Compact,
    )
}

@Composable
private fun historySourceLabel(source: WebDavSyncHistorySource): String =
    when (source) {
        WebDavSyncHistorySource.Manual -> stringResource(R.string.webdav_sync_history_source_manual)
        WebDavSyncHistorySource.Auto -> stringResource(R.string.webdav_sync_history_source_auto)
        WebDavSyncHistorySource.RemoteProbe -> stringResource(R.string.webdav_sync_history_source_remote_probe)
    }

@Composable
private fun historyKindLabel(kind: WebDavSyncHistoryKind?): String =
    when (kind) {
        WebDavSyncHistoryKind.Sync -> stringResource(R.string.webdav_sync_history_kind_sync)
        WebDavSyncHistoryKind.Upload -> stringResource(R.string.webdav_sync_history_kind_upload)
        WebDavSyncHistoryKind.Download -> stringResource(R.string.webdav_sync_history_kind_download)
        WebDavSyncHistoryKind.RemoteProbe -> stringResource(R.string.webdav_sync_history_kind_remote_probe)
        null -> stringResource(R.string.webdav_sync_history_kind_auto)
    }

@Composable
private fun historyStatusLabel(status: WebDavAutoSyncStatus): String =
    when (status) {
        WebDavAutoSyncStatus.Success -> stringResource(R.string.webdav_sync_history_status_success)
        WebDavAutoSyncStatus.NeedsReview -> stringResource(R.string.webdav_sync_history_status_needs_review)
        WebDavAutoSyncStatus.Failed -> stringResource(R.string.webdav_sync_history_status_failed)
        WebDavAutoSyncStatus.Skipped -> stringResource(R.string.webdav_sync_history_status_skipped)
        WebDavAutoSyncStatus.Running -> stringResource(R.string.webdav_sync_history_status_running)
    }

@Composable
private fun historyStatusColor(status: WebDavAutoSyncStatus): Color =
    when (status) {
        WebDavAutoSyncStatus.Success -> AppStatusColors.Fresh
        WebDavAutoSyncStatus.NeedsReview -> Color(0xFFF59E0B)
        WebDavAutoSyncStatus.Failed -> MiuixTheme.colorScheme.error
        WebDavAutoSyncStatus.Skipped -> MiuixTheme.colorScheme.onBackgroundVariant
        WebDavAutoSyncStatus.Running -> MiuixTheme.colorScheme.primary
    }

@Composable
private fun historyItemStatusLine(item: WebDavSyncHistoryItem): String {
    val base =
        when (item.status) {
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
    val detail = item.detail?.takeIf { it.isNotBlank() } ?: return base
    return "$base · $detail"
}

@Composable
private fun historyItemStatusColor(status: WebDavItemStatus): Color =
    when (status) {
        WebDavItemStatus.Uploaded,
        WebDavItemStatus.Downloaded,
        WebDavItemStatus.Merged,
        WebDavItemStatus.UpToDate,
        WebDavItemStatus.RemoteEmpty,
        -> AppStatusColors.Fresh

        WebDavItemStatus.BaselineRequired,
        WebDavItemStatus.ConflictUnresolved,
        -> Color(0xFFF59E0B)

        else -> MiuixTheme.colorScheme.error
    }

@Composable
private fun historyReasonLabel(reason: String): String =
    when {
        reason == "manual-batch" -> stringResource(R.string.webdav_sync_history_reason_manual_batch)
        reason == "manual-auto-review" -> stringResource(R.string.webdav_sync_history_reason_manual_auto_review)
        reason == "manual-remote-probe" -> stringResource(R.string.webdav_sync_history_reason_remote_probe)
        reason == "launch" -> stringResource(R.string.webdav_sync_history_reason_launch)
        reason == "launch-dirty" -> stringResource(R.string.webdav_sync_history_reason_launch_dirty)
        reason == "background" -> stringResource(R.string.webdav_sync_history_reason_background)
        reason == "alarm" -> stringResource(R.string.webdav_sync_history_reason_alarm)
        reason == "alarm-timeout" -> stringResource(R.string.webdav_sync_history_reason_alarm_timeout)
        reason.startsWith("manual-") -> stringResource(R.string.webdav_sync_history_reason_manual_item)
        else -> reason.ifBlank { stringResource(R.string.common_na) }
    }

@Composable
private fun formatHistoryDuration(durationMs: Long): String {
    val safe = durationMs.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    return when {
        minutes > 0L -> stringResource(R.string.webdav_sync_history_duration_minutes_seconds, minutes, seconds)
        seconds > 0L -> stringResource(R.string.webdav_sync_history_duration_seconds, seconds)
        else -> stringResource(R.string.webdav_sync_history_duration_millis, safe)
    }
}

private fun formatHistoryTime(timeMs: Long): String {
    val safe = timeMs.coerceAtLeast(0L)
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(safe))
}
