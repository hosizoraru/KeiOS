package os.kei.ui.page.main.ba.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.ba.BaGlassCard
import os.kei.ui.page.main.ba.BaGlassPanel
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaPoolEntry
import os.kei.ui.page.main.ba.support.GameKeeCoverImage
import os.kei.ui.page.main.ba.support.activityProgress
import os.kei.ui.page.main.ba.support.baCalendarKindLabel
import os.kei.ui.page.main.ba.support.baPoolTagLabel
import os.kei.ui.page.main.ba.support.formatBaDateTimeNoYearInTimeZone
import os.kei.ui.page.main.ba.support.formatBaRemainingTime
import os.kei.ui.page.main.ba.support.poolProgress
import os.kei.ui.page.main.ba.support.serverRefreshTimeZone
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun filterVisibleCalendarEntries(
    entries: List<BaCalendarEntry>,
    showEndedActivities: Boolean,
    nowMs: Long
): List<BaCalendarEntry> {
    return if (showEndedActivities) entries else entries.filter { it.endAtMs > nowMs }
}

internal fun filterVisiblePoolEntries(
    entries: List<BaPoolEntry>,
    showEndedPools: Boolean,
    nowMs: Long
): List<BaPoolEntry> {
    return if (showEndedPools) entries else entries.filter { it.endAtMs > nowMs }
}

@Composable
internal fun BaCalendarSectionHeaderCard(
    backdrop: Backdrop?,
    serverOptions: List<String>,
    serverIndex: Int,
    baCalendarLoading: Boolean,
    baCalendarLastSyncMs: Long,
    effectsEnabled: Boolean,
    onRefreshCalendar: () -> Unit,
) {
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val syncText = when {
        baCalendarLoading -> stringResource(R.string.ba_syncing)
        baCalendarLastSyncMs > 0L -> formatBaDateTimeNoYearInTimeZone(
            baCalendarLastSyncMs,
            serverTimeZone
        )
        else -> stringResource(R.string.ba_state_not_synced)
    }
    BaGlassCard(
        backdrop = backdrop,
        accentColor = Color(0xFF3B82F6),
        accentAlpha = 0f,
        effectsEnabled = effectsEnabled,
    ) {
        BaCardHeader(
            title = stringResource(R.string.ba_calendar_title_format, serverOptions[serverIndex]),
            trailing = {
                Text(
                    text = syncText,
                    color = countdownBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = stringResource(R.string.ba_calendar_cd_refresh),
                    variant = GlassVariant.Content,
                    onClick = onRefreshCalendar,
                )
            },
        )
    }
}

@Composable
internal fun BaCalendarCard(
    backdrop: Backdrop?,
    isPageActive: Boolean,
    serverOptions: List<String>,
    serverIndex: Int,
    baCalendarLoading: Boolean,
    baCalendarLastSyncMs: Long,
    baCalendarError: String?,
    visibleCalendarEntries: List<BaCalendarEntry>,
    nowMs: Long,
    showEndedActivities: Boolean,
    showCalendarPoolImages: Boolean,
    effectsEnabled: Boolean,
    onRefreshCalendar: () -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val syncText = when {
        baCalendarLoading -> stringResource(R.string.ba_syncing)
        baCalendarLastSyncMs > 0L -> formatBaDateTimeNoYearInTimeZone(
            baCalendarLastSyncMs,
            serverTimeZone
        )
        else -> stringResource(R.string.ba_state_not_synced)
    }
    BaGlassCard(
        backdrop = backdrop,
        accentColor = Color(0xFF3B82F6),
        accentAlpha = 0f,
        effectsEnabled = effectsEnabled,
    ) {
        BaCardHeader(
            title = stringResource(R.string.ba_calendar_title_format, serverOptions[serverIndex]),
            trailing = {
                Text(
                    text = syncText,
                    color = countdownBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = stringResource(R.string.ba_calendar_cd_refresh),
                    variant = GlassVariant.Content,
                    onClick = onRefreshCalendar,
                )
            },
        )
        when {
            !baCalendarError.isNullOrBlank() -> {
                BaCalendarStatePanel(
                    backdrop = backdrop,
                    text = baCalendarError,
                    accentColor = Color(0xFFF59E0B),
                    effectsEnabled = effectsEnabled
                )
            }

            !baCalendarLoading && visibleCalendarEntries.isEmpty() -> {
                BaCalendarStatePanel(
                    backdrop = backdrop,
                    text = if (showEndedActivities) {
                        stringResource(R.string.ba_calendar_empty_all)
                    } else {
                        stringResource(R.string.ba_calendar_empty_active)
                    },
                    accentColor = MiuixTheme.colorScheme.onBackgroundVariant,
                    effectsEnabled = effectsEnabled
                )
            }

            else -> {
                visibleCalendarEntries.forEach { activity ->
                    BaCalendarEntryPanel(
                        backdrop = backdrop,
                        isPageActive = isPageActive,
                        serverIndex = serverIndex,
                        activity = activity,
                        nowMs = nowMs,
                        showCalendarPoolImages = showCalendarPoolImages,
                        effectsEnabled = effectsEnabled,
                        onOpenCalendarLink = onOpenCalendarLink
                    )
                }
            }
        }
    }
}

@Composable
internal fun BaCalendarStatePanel(
    backdrop: Backdrop?,
    text: String,
    accentColor: Color,
    effectsEnabled: Boolean,
) {
    BaGlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = accentColor,
        effectsEnabled = effectsEnabled,
    ) {
        Text(
            text = text,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun BaCalendarEntryPanel(
    backdrop: Backdrop?,
    isPageActive: Boolean,
    serverIndex: Int,
    activity: BaCalendarEntry,
    nowMs: Long,
    showCalendarPoolImages: Boolean,
    effectsEnabled: Boolean,
    onOpenCalendarLink: (String) -> Unit,
) {
    val accentGreen = Color(0xFF22C55E)
    val accentBlue = Color(0xFF3B82F6)
    val countdownBlue = Color(0xFF60A5FA)
    val isRunningNow = nowMs in activity.beginAtMs until activity.endAtMs
    val isEnded = activity.endAtMs <= nowMs
    val remainTarget = if (isRunningNow || isEnded) activity.endAtMs else activity.beginAtMs
    val endedText = stringResource(R.string.ba_status_ended)
    val remainText = if (isEnded) endedText else formatBaRemainingTime(remainTarget, nowMs)
    val statusText = when {
        isRunningNow -> stringResource(R.string.ba_status_running)
        isEnded -> endedText
        else -> stringResource(R.string.ba_status_upcoming)
    }
    val statusColor = when {
        isRunningNow -> accentGreen
        isEnded -> MiuixTheme.colorScheme.onBackgroundVariant
        else -> accentBlue
    }
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val kindLabel = baCalendarKindLabel(activity.kindId, activity.kindName)

    BaGlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = statusColor,
        effectsEnabled = effectsEnabled,
        onClick = { onOpenCalendarLink(activity.linkUrl) },
        onLongClick = { onOpenCalendarLink(activity.linkUrl) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = remainText,
                color = countdownBlue,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Text(
            text = "$kindLabel · ${activity.title}",
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showCalendarPoolImages) {
            GameKeeCoverImage(
                imageUrl = activity.imageUrl,
                enabled = isPageActive,
                modifier = Modifier.fillMaxWidth(),
                loadEnabled = isPageActive && effectsEnabled
            )
        }
        Text(
            text = "${formatBaDateTimeNoYearInTimeZone(activity.beginAtMs, serverTimeZone)} - ${
                formatBaDateTimeNoYearInTimeZone(activity.endAtMs, serverTimeZone)
            }",
            color = countdownBlue.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(
            progress = activityProgress(activity, nowMs),
            modifier = Modifier.fillMaxWidth(),
            height = 5.dp,
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = if (isRunningNow) accentGreen else accentBlue,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.56f),
            ),
        )
    }
}

@Composable
internal fun BaPoolSectionHeaderCard(
    backdrop: Backdrop?,
    serverOptions: List<String>,
    serverIndex: Int,
    baPoolLoading: Boolean,
    baPoolLastSyncMs: Long,
    effectsEnabled: Boolean,
    onRefreshPool: () -> Unit,
) {
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val syncText = when {
        baPoolLoading -> stringResource(R.string.ba_syncing)
        baPoolLastSyncMs > 0L -> formatBaDateTimeNoYearInTimeZone(
            baPoolLastSyncMs,
            serverTimeZone
        )
        else -> stringResource(R.string.ba_state_not_synced)
    }
    BaGlassCard(
        backdrop = backdrop,
        accentColor = Color(0xFF3B82F6),
        accentAlpha = 0f,
        effectsEnabled = effectsEnabled,
    ) {
        BaCardHeader(
            title = stringResource(R.string.ba_pool_title_format, serverOptions[serverIndex]),
            trailing = {
                Text(
                    text = syncText,
                    color = countdownBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = stringResource(R.string.ba_pool_cd_refresh),
                    variant = GlassVariant.Content,
                    onClick = onRefreshPool,
                )
            },
        )
    }
}

@Composable
internal fun BaPoolCard(
    backdrop: Backdrop?,
    isPageActive: Boolean,
    serverOptions: List<String>,
    serverIndex: Int,
    baPoolLoading: Boolean,
    baPoolLastSyncMs: Long,
    baPoolError: String?,
    visiblePoolEntries: List<BaPoolEntry>,
    nowMs: Long,
    showEndedPools: Boolean,
    showCalendarPoolImages: Boolean,
    effectsEnabled: Boolean,
    onRefreshPool: () -> Unit,
    onOpenPoolStudentGuide: (String) -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val syncText = when {
        baPoolLoading -> stringResource(R.string.ba_syncing)
        baPoolLastSyncMs > 0L -> formatBaDateTimeNoYearInTimeZone(
            baPoolLastSyncMs,
            serverTimeZone
        )
        else -> stringResource(R.string.ba_state_not_synced)
    }
    BaGlassCard(
        backdrop = backdrop,
        accentColor = Color(0xFF3B82F6),
        accentAlpha = 0f,
        effectsEnabled = effectsEnabled,
    ) {
        BaCardHeader(
            title = stringResource(R.string.ba_pool_title_format, serverOptions[serverIndex]),
            trailing = {
                Text(
                    text = syncText,
                    color = countdownBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = stringResource(R.string.ba_pool_cd_refresh),
                    variant = GlassVariant.Content,
                    onClick = onRefreshPool,
                )
            },
        )
        when {
            !baPoolError.isNullOrBlank() -> {
                BaPoolStatePanel(
                    backdrop = backdrop,
                    text = baPoolError,
                    accentColor = Color(0xFFF59E0B),
                    effectsEnabled = effectsEnabled
                )
            }

            !baPoolLoading && visiblePoolEntries.isEmpty() -> {
                BaPoolStatePanel(
                    backdrop = backdrop,
                    text = if (showEndedPools) {
                        stringResource(R.string.ba_pool_empty_all)
                    } else {
                        stringResource(R.string.ba_pool_empty_active)
                    },
                    accentColor = MiuixTheme.colorScheme.onBackgroundVariant,
                    effectsEnabled = effectsEnabled
                )
            }

            else -> {
                visiblePoolEntries.forEach { pool ->
                    BaPoolEntryPanel(
                        backdrop = backdrop,
                        isPageActive = isPageActive,
                        serverIndex = serverIndex,
                        pool = pool,
                        nowMs = nowMs,
                        showCalendarPoolImages = showCalendarPoolImages,
                        effectsEnabled = effectsEnabled,
                        onOpenPoolStudentGuide = onOpenPoolStudentGuide,
                        onOpenCalendarLink = onOpenCalendarLink
                    )
                }
            }
        }
    }
}

@Composable
internal fun BaPoolStatePanel(
    backdrop: Backdrop?,
    text: String,
    accentColor: Color,
    effectsEnabled: Boolean,
) {
    BaGlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = accentColor,
        effectsEnabled = effectsEnabled,
    ) {
        Text(
            text = text,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun BaPoolEntryPanel(
    backdrop: Backdrop?,
    isPageActive: Boolean,
    serverIndex: Int,
    pool: BaPoolEntry,
    nowMs: Long,
    showCalendarPoolImages: Boolean,
    effectsEnabled: Boolean,
    onOpenPoolStudentGuide: (String) -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF22C55E)
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val isRunningNow = nowMs in pool.startAtMs until pool.endAtMs
    val isEnded = pool.endAtMs <= nowMs
    val remainTarget = if (isRunningNow || isEnded) pool.endAtMs else pool.startAtMs
    val endedText = stringResource(R.string.ba_status_ended)
    val remainText = if (isEnded) endedText else formatBaRemainingTime(remainTarget, nowMs)
    val statusText = when {
        isRunningNow -> stringResource(R.string.ba_status_running)
        isEnded -> endedText
        else -> stringResource(R.string.ba_status_upcoming)
    }
    val statusColor = when {
        isRunningNow -> accentGreen
        isEnded -> MiuixTheme.colorScheme.onBackgroundVariant
        else -> accentBlue
    }
    val showPoolCoverImage = showCalendarPoolImages && pool.imageUrl.isNotBlank()
    val tagLabel = baPoolTagLabel(pool.tagId, pool.tagName)

    BaGlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = statusColor,
        effectsEnabled = effectsEnabled,
        onClick = { onOpenPoolStudentGuide(pool.linkUrl) },
        onLongClick = { onOpenCalendarLink(pool.linkUrl) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (showPoolCoverImage) {
                Box(
                    modifier = Modifier.width(106.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    GameKeeCoverImage(
                        imageUrl = pool.imageUrl,
                        enabled = isPageActive,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        aspectRatioRange = 0.66f..1.34f,
                        loadEnabled = isPageActive && effectsEnabled
                    )
                }
            }
            Column(
                modifier = if (showPoolCoverImage) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$tagLabel · ${pool.name}",
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatBaDateTimeNoYearInTimeZone(pool.startAtMs, serverTimeZone)} - ${
                        formatBaDateTimeNoYearInTimeZone(pool.endAtMs, serverTimeZone)
                    }",
                    color = countdownBlue.copy(alpha = 0.92f),
                    maxLines = 3,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = remainText,
                    color = if (isEnded) MiuixTheme.colorScheme.onBackgroundVariant else countdownBlue,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LinearProgressIndicator(
            progress = poolProgress(pool, nowMs),
            modifier = Modifier.fillMaxWidth(),
            height = 5.dp,
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = if (isRunningNow) accentGreen else accentBlue,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.56f),
            ),
        )
    }
}

@Composable
private fun baCalendarKindLabel(kindId: Int, fallback: String): String {
    return LocalContext.current.baCalendarKindLabel(kindId, fallback)
}

@Composable
private fun baPoolTagLabel(tagId: Int, fallback: String): String {
    return LocalContext.current.baPoolTagLabel(tagId, fallback)
}
