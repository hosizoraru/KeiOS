package os.kei.ui.page.main.ba.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.ba.BaLiquidPanel
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
import os.kei.ui.page.main.ba.support.studentGuideOpenUrl
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.LiquidLinearProgressBar
import os.kei.ui.page.main.widget.status.AppStatusColors
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
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
internal fun BaCalendarStatePanel(
    backdrop: Backdrop?,
    text: String,
    accentColor: Color,
    effectsEnabled: Boolean,
) {
    BaLiquidPanel(
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
    val accentGreen = AppStatusColors.Fresh
    val accentBlue = AppStatusColors.Cached
    val countdownBlue = AppStatusColors.Refreshing
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

    BaLiquidPanel(
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
            BaCalendarPoolEntryStatusPill(
                text = statusText,
                accentColor = statusColor,
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
        LiquidLinearProgressBar(
            progress = { activityProgress(activity, nowMs) },
            modifier = Modifier.fillMaxWidth(),
            height = 5.dp,
            activeColor = if (isRunningNow) accentGreen else accentBlue,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        )
    }
}

@Composable
internal fun BaPoolStatePanel(
    backdrop: Backdrop?,
    text: String,
    accentColor: Color,
    effectsEnabled: Boolean,
) {
    BaLiquidPanel(
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
    val accentBlue = AppStatusColors.Cached
    val accentGreen = AppStatusColors.Fresh
    val countdownBlue = AppStatusColors.Refreshing
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
    val poolCoverDecodeDimension =
        with(LocalDensity.current) {
            106.dp.roundToPx()
        }.coerceIn(256, 640)

    BaLiquidPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = statusColor,
        effectsEnabled = effectsEnabled,
        onClick = { onOpenPoolStudentGuide(pool.studentGuideOpenUrl) },
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
                        loadEnabled = isPageActive && effectsEnabled,
                        maxDecodeDimension = poolCoverDecodeDimension,
                    )
                }
            }
            Column(
                modifier = if (showPoolCoverImage) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                BaCalendarPoolEntryStatusPill(
                    text = statusText,
                    accentColor = statusColor,
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
        LiquidLinearProgressBar(
            progress = { poolProgress(pool, nowMs) },
            modifier = Modifier.fillMaxWidth(),
            height = 5.dp,
            activeColor = if (isRunningNow) accentGreen else accentBlue,
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        )
    }
}

@Composable
internal fun BaCalendarPoolEntryStatusPill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    StatusPill(
        label = text,
        color = accentColor,
        modifier = modifier.widthIn(max = 128.dp),
        size = AppStatusPillSize.Compact,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
        backgroundAlphaOverride = 0.12f,
        borderAlphaOverride = 0f,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        typographyOverride =
            TextStyle(
                fontSize = AppTypographyTokens.Caption.fontSize,
                lineHeight = AppTypographyTokens.Caption.lineHeight,
                fontWeight = FontWeight.Medium,
            ),
        contentColorOverride = accentColor,
    )
}

@Composable
private fun baCalendarKindLabel(kindId: Int, fallback: String): String {
    return LocalContext.current.baCalendarKindLabel(kindId, fallback)
}

@Composable
private fun baPoolTagLabel(tagId: Int, fallback: String): String {
    return LocalContext.current.baPoolTagLabel(tagId, fallback)
}
