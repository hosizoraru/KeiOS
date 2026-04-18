package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaCalendarCard(
    backdrop: Backdrop?,
    isPageActive: Boolean,
    serverOptions: List<String>,
    serverIndex: Int,
    uiNowMs: Long,
    baCalendarEntries: List<BaCalendarEntry>,
    baCalendarLoading: Boolean,
    baCalendarError: String?,
    baCalendarLastSyncMs: Long,
    showEndedActivities: Boolean,
    showCalendarPoolImages: Boolean,
    onRefreshCalendar: () -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF22C55E)
    val accentAmber = Color(0xFFF59E0B)
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val minuteBucket = remember(uiNowMs) { uiNowMs / 60_000L }
    val visibleCalendarEntries by remember(baCalendarEntries, showEndedActivities, minuteBucket) {
        derivedStateOf {
            if (showEndedActivities) {
                baCalendarEntries
            } else {
                baCalendarEntries.filter { it.endAtMs > uiNowMs }
            }
        }
    }

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentBlue,
        accentAlpha = 0f,
    ) {
        BaCardHeader(
            title = "活动日历 · ${serverOptions[serverIndex]}",
            trailing = {
                Text(
                    text = if (baCalendarLoading) "同步中..." else formatBaDateTimeNoYearInTimeZone(baCalendarLastSyncMs, serverTimeZone),
                    color = countdownBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = "刷新活动日历",
                    variant = GlassVariant.Content,
                    onClick = onRefreshCalendar,
                )
            },
        )

        when {
            !baCalendarError.isNullOrBlank() -> {
                BaGlassPanel(
                    backdrop = backdrop,
                    accentColor = accentAmber,
                ) {
                    Text(text = baCalendarError.orEmpty(), color = accentAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            !baCalendarLoading && visibleCalendarEntries.isEmpty() -> {
                BaGlassPanel(
                    backdrop = backdrop,
                    accentColor = accentBlue,
                ) {
                    Text(
                        text = if (showEndedActivities) "暂无活动" else "暂无进行中或即将开始的活动",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }

            else -> {
                visibleCalendarEntries.forEach { activity ->
                    key(activity.id, activity.beginAtMs, activity.endAtMs) {
                        val isEnded = activity.endAtMs <= uiNowMs
                        val remainTarget = if (activity.isRunning || isEnded) activity.endAtMs else activity.beginAtMs
                        val remainText = if (isEnded) "已结束" else formatBaRemainingTime(remainTarget, uiNowMs)
                        val statusText = when {
                            activity.isRunning -> "进行中"
                            isEnded -> "已结束"
                            else -> "即将开始"
                        }
                        val statusColor = when {
                            activity.isRunning -> accentGreen
                            isEnded -> MiuixTheme.colorScheme.onBackgroundVariant
                            else -> accentBlue
                        }

                        BaGlassPanel(
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = statusColor,
                            onClick = { onOpenCalendarLink(activity.linkUrl) },
                            onLongClick = { onOpenCalendarLink(activity.linkUrl) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = statusText, color = statusColor, fontWeight = FontWeight.Medium)
                                Text(text = remainText, color = countdownBlue, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Text(
                                text = "${activity.kindName} · ${activity.title}",
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
                                )
                            }
                            Text(
                                text = "${formatBaDateTimeNoYearInTimeZone(activity.beginAtMs, serverTimeZone)} - ${formatBaDateTimeNoYearInTimeZone(activity.endAtMs, serverTimeZone)}",
                                color = countdownBlue.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            LinearProgressIndicator(
                                progress = activityProgress(activity, uiNowMs),
                                modifier = Modifier.fillMaxWidth(),
                                height = 5.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = if (activity.isRunning) accentGreen else accentBlue,
                                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.56f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BaPoolCard(
    backdrop: Backdrop?,
    isPageActive: Boolean,
    serverOptions: List<String>,
    serverIndex: Int,
    uiNowMs: Long,
    baPoolEntries: List<BaPoolEntry>,
    baPoolLoading: Boolean,
    baPoolError: String?,
    baPoolLastSyncMs: Long,
    showEndedPools: Boolean,
    showCalendarPoolImages: Boolean,
    onRefreshPool: () -> Unit,
    onOpenPoolStudentGuide: (String) -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF22C55E)
    val accentAmber = Color(0xFFF59E0B)
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val minuteBucket = remember(uiNowMs) { uiNowMs / 60_000L }
    val visiblePoolEntries by remember(baPoolEntries, showEndedPools, minuteBucket) {
        derivedStateOf {
            if (showEndedPools) {
                baPoolEntries
            } else {
                baPoolEntries.filter { it.endAtMs > uiNowMs }
            }
        }
    }

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentBlue,
        accentAlpha = 0f,
    ) {
        BaCardHeader(
            title = "卡池信息 · ${serverOptions[serverIndex]}",
            trailing = {
                Text(
                    text = if (baPoolLoading) "同步中..." else formatBaDateTimeNoYearInTimeZone(baPoolLastSyncMs, serverTimeZone),
                    color = countdownBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = "刷新卡池",
                    variant = GlassVariant.Content,
                    onClick = onRefreshPool,
                )
            },
        )

        when {
            !baPoolError.isNullOrBlank() -> {
                BaGlassPanel(
                    backdrop = backdrop,
                    accentColor = accentAmber,
                ) {
                    Text(text = baPoolError.orEmpty(), color = accentAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            !baPoolLoading && visiblePoolEntries.isEmpty() -> {
                BaGlassPanel(
                    backdrop = backdrop,
                    accentColor = accentBlue,
                ) {
                    Text(
                        text = "暂无进行中或即将开始的卡池",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }

            else -> {
                visiblePoolEntries.forEach { pool ->
                    key(pool.id) {
                        val isEnded = pool.endAtMs <= uiNowMs
                        val remainTarget = if (pool.isRunning || isEnded) pool.endAtMs else pool.startAtMs
                        val remainText = if (isEnded) "已结束" else formatBaRemainingTime(remainTarget, uiNowMs)
                        val statusText = when {
                            pool.isRunning -> "进行中"
                            isEnded -> "已结束"
                            else -> "即将开始"
                        }
                        val statusColor = when {
                            pool.isRunning -> accentGreen
                            isEnded -> MiuixTheme.colorScheme.onBackgroundVariant
                            else -> accentBlue
                        }
                        val showPoolCoverImage = showCalendarPoolImages && pool.imageUrl.isNotBlank()

                        BaGlassPanel(
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = statusColor,
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
                                            aspectRatioRange = 0.66f..1.34f
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
                                        text = "${pool.tagName} · ${pool.name}",
                                        color = MiuixTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${formatBaDateTimeNoYearInTimeZone(pool.startAtMs, serverTimeZone)} - ${formatBaDateTimeNoYearInTimeZone(pool.endAtMs, serverTimeZone)}",
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
                                progress = poolProgress(pool, uiNowMs),
                                modifier = Modifier.fillMaxWidth(),
                                height = 5.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = if (pool.isRunning) accentGreen else accentBlue,
                                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.56f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
