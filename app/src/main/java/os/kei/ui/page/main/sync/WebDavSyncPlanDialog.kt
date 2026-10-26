@file:Suppress("FunctionName")

package os.kei.ui.page.main.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun WebDavSyncPlanDialog(
    plan: WebDavSyncPlan,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val destructive = plan.kind == WebDavBatchKind.Upload || plan.kind == WebDavBatchKind.Download
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap),
    ) {
        Text(
            text = planTitle(plan),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.CompactTitle.fontSize,
            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
        )
        Text(
            text = planSummary(plan.kind),
            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        Text(
            text = stringResource(R.string.webdav_sync_plan_remote_refreshed, formatPlanTime(plan.createdAtMs)),
            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.74f),
            fontSize = AppTypographyTokens.Caption.fontSize,
            lineHeight = AppTypographyTokens.Caption.lineHeight,
        )
        if (plan.hasRemoteShrinkRisk) {
            Text(
                text = stringResource(R.string.webdav_sync_plan_upload_shrink_warning),
                color = MiuixTheme.colorScheme.error,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
        }
        if (plan.hasBlockingError) {
            Text(
                text = stringResource(R.string.webdav_sync_plan_blocking_error),
                color = MiuixTheme.colorScheme.error,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap),
        ) {
            plan.items.forEach { item ->
                WebDavSyncPlanItemBlock(
                    item = item,
                    kind = plan.kind,
                )
            }
        }
        Spacer(Modifier.height(CardLayoutRhythm.compactSectionGap))
        AppDualActionRow(
            first = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.SheetAction,
                    text = stringResource(R.string.webdav_sync_confirm_cancel),
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = MiuixTheme.colorScheme.onBackgroundVariant,
                    onClick = onDismiss,
                )
            },
            second = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = if (destructive) GlassVariant.SheetDangerAction else GlassVariant.SheetPrimaryAction,
                    text = confirmText(plan.kind),
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor =
                        if (destructive) {
                            MiuixTheme.colorScheme.error
                        } else {
                            MiuixTheme.colorScheme.primary
                        },
                    enabled = !plan.hasBlockingError,
                    onClick = onConfirm,
                )
            },
        )
    }
}

@Composable
private fun WebDavSyncPlanItemBlock(
    item: WebDavSyncPlanItem,
    kind: WebDavBatchKind,
) {
    val isError = item.remoteState is WebDavSyncPlanRemoteState.Error
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
    ) {
        Text(
            text = stringResource(item.item.labelRes),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = countsLine(item),
            color = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.88f),
            fontSize = AppTypographyTokens.Caption.fontSize,
            lineHeight = AppTypographyTokens.Caption.lineHeight,
        )
        Text(
            text = effectLine(item, kind),
            color = effectColor(item),
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
    }
}

@Composable
private fun planTitle(plan: WebDavSyncPlan): String =
    when (val scope = plan.scope) {
        WebDavSyncPlanScope.Batch ->
            stringResource(
                when (plan.kind) {
                    WebDavBatchKind.Sync -> R.string.webdav_sync_plan_sync_title
                    WebDavBatchKind.Upload -> R.string.webdav_sync_plan_upload_title
                    WebDavBatchKind.Download -> R.string.webdav_sync_plan_download_title
                },
            )

        WebDavSyncPlanScope.AutoReview ->
            stringResource(R.string.webdav_sync_plan_auto_review_title)

        is WebDavSyncPlanScope.Single -> {
            val itemLabel = stringResource(scope.item.labelRes)
            stringResource(
                when (plan.kind) {
                    WebDavBatchKind.Sync -> R.string.webdav_sync_plan_item_sync_title
                    WebDavBatchKind.Upload -> R.string.webdav_sync_plan_item_upload_title
                    WebDavBatchKind.Download -> R.string.webdav_sync_plan_item_download_title
                },
                itemLabel,
            )
        }
    }

@Composable
private fun planSummary(kind: WebDavBatchKind): String =
    stringResource(
        when (kind) {
            WebDavBatchKind.Sync -> R.string.webdav_sync_plan_sync_summary
            WebDavBatchKind.Upload -> R.string.webdav_sync_plan_upload_summary
            WebDavBatchKind.Download -> R.string.webdav_sync_plan_download_summary
        },
    )

@Composable
private fun confirmText(kind: WebDavBatchKind): String =
    stringResource(
        when (kind) {
            WebDavBatchKind.Sync -> R.string.webdav_sync_plan_confirm_sync
            WebDavBatchKind.Upload -> R.string.webdav_sync_plan_confirm_upload
            WebDavBatchKind.Download -> R.string.webdav_sync_plan_confirm_download
        },
    )

@Composable
private fun countsLine(item: WebDavSyncPlanItem): String {
    val remoteText =
        when (val remote = item.remoteState) {
            WebDavSyncPlanRemoteState.Empty -> stringResource(R.string.webdav_sync_plan_remote_empty_count)
            is WebDavSyncPlanRemoteState.Error -> itemStatusText(remote.status)
            is WebDavSyncPlanRemoteState.Found ->
                if (remote.itemCount < 0) stringResource(R.string.webdav_sync_plan_remote_unknown_count)
                else stringResource(
                    R.string.webdav_sync_plan_remote_count_format,
                    remote.itemCount.coerceAtLeast(0),
                    formatPlanBytes(remote.byteSize),
                )
        }
    return stringResource(
        R.string.webdav_sync_plan_counts_format,
        item.localCount.coerceAtLeast(0),
        remoteText,
    )
}

@Composable
private fun effectLine(item: WebDavSyncPlanItem, kind: WebDavBatchKind): String {
    val base =
        stringResource(
            when (item.effect) {
                WebDavSyncPlanEffect.NoChange -> R.string.webdav_sync_plan_effect_no_change
                WebDavSyncPlanEffect.CreateRemote -> R.string.webdav_sync_plan_effect_create_remote
                WebDavSyncPlanEffect.MergeThenUpload -> R.string.webdav_sync_plan_effect_merge_upload
                WebDavSyncPlanEffect.UploadOverwrite -> R.string.webdav_sync_plan_effect_upload_overwrite
                WebDavSyncPlanEffect.DownloadReplace -> R.string.webdav_sync_plan_effect_download_replace
                WebDavSyncPlanEffect.RemoteEmpty -> R.string.webdav_sync_plan_effect_remote_empty
                WebDavSyncPlanEffect.Error -> R.string.webdav_sync_plan_effect_error
            },
        )
    if (kind == WebDavBatchKind.Upload && item.shrinksRemote) {
        return "$base · ${stringResource(R.string.webdav_sync_plan_effect_remote_shrink)}"
    }
    return base
}

@Composable
private fun effectColor(item: WebDavSyncPlanItem): Color =
    when {
        item.remoteState is WebDavSyncPlanRemoteState.Error -> MiuixTheme.colorScheme.error
        item.shrinksRemote -> MiuixTheme.colorScheme.error
        item.effect == WebDavSyncPlanEffect.DownloadReplace ||
            item.effect == WebDavSyncPlanEffect.UploadOverwrite -> MiuixTheme.colorScheme.error
        item.effect == WebDavSyncPlanEffect.NoChange -> Color(0xFF22C55E)
        else -> MiuixTheme.colorScheme.primary
    }

@Composable
private fun itemStatusText(status: WebDavItemStatus): String =
    stringResource(
        when (status) {
            WebDavItemStatus.Uploaded -> R.string.webdav_sync_status_uploaded
            WebDavItemStatus.Downloaded -> R.string.webdav_sync_status_downloaded
            WebDavItemStatus.Merged -> R.string.webdav_sync_status_merged
            WebDavItemStatus.UpToDate -> R.string.webdav_sync_status_up_to_date
            WebDavItemStatus.RemoteEmpty -> R.string.webdav_sync_status_remote_empty
            WebDavItemStatus.AuthFailed -> R.string.webdav_sync_status_auth_failed
            WebDavItemStatus.PermissionDenied -> R.string.webdav_sync_status_permission_denied
            WebDavItemStatus.NetworkError -> R.string.webdav_sync_status_network_error
            WebDavItemStatus.ConflictUnresolved -> R.string.webdav_sync_status_conflict
            WebDavItemStatus.BaselineRequired -> R.string.webdav_sync_status_baseline_required
            WebDavItemStatus.RateLimited -> R.string.webdav_sync_status_rate_limited
            WebDavItemStatus.Error -> R.string.webdav_sync_status_error
        },
    )

private fun formatPlanTime(timeMs: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
private fun formatPlanBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe >= 1024L * 1024L -> stringResource(R.string.webdav_sync_size_mb, safe / 1024.0 / 1024.0)
        safe >= 1024L -> stringResource(R.string.webdav_sync_size_kb, safe / 1024.0)
        else -> stringResource(R.string.webdav_sync_size_bytes, safe)
    }
}
