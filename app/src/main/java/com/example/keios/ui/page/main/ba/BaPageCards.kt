package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ui.page.main.BA_AP_LIMIT_MAX
import com.example.keios.ui.page.main.BA_AP_MAX
import com.example.keios.ui.page.main.BA_DEFAULT_FRIEND_CODE
import com.example.keios.ui.page.main.BA_DEFAULT_NICKNAME
import com.example.keios.ui.page.main.BAInitState
import com.example.keios.ui.page.main.BaCalendarEntry
import com.example.keios.ui.page.main.BaPoolEntry
import com.example.keios.ui.page.main.activityProgress
import com.example.keios.ui.page.main.cafeDailyCapacity
import com.example.keios.ui.page.main.calculateInviteTicketAvailableMs
import com.example.keios.ui.page.main.calculateApFullAtMs
import com.example.keios.ui.page.main.calculateApNextPointAtMs
import com.example.keios.ui.page.main.calculateNextHeadpatAvailableMs
import com.example.keios.ui.page.main.displayAp
import com.example.keios.ui.page.main.formatBaDateTime
import com.example.keios.ui.page.main.formatBaDateTimeNoSeconds
import com.example.keios.ui.page.main.formatBaDateTimeNoYearInTimeZone
import com.example.keios.ui.page.main.formatBaRemainingTime
import com.example.keios.ui.page.main.nextArenaRefreshMs
import com.example.keios.ui.page.main.nextCafeStudentRefreshMs
import com.example.keios.ui.page.main.poolProgress
import com.example.keios.ui.page.main.serverRefreshTimeZone
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun BaOverviewCard(
    isDark: Boolean,
    backdrop: Backdrop?,
    baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    baCardBaseColor: Color,
    idFriendCode: String,
    uiNowMs: Long,
    apSyncMs: Long,
    apLimit: Int,
    apCurrent: Double,
    apRegenBaseMs: Long,
    apCurrentInput: String,
    onApCurrentInputChange: (String) -> Unit,
    onApCurrentDone: () -> Unit,
    apLimitInput: String,
    onApLimitInputChange: (String) -> Unit,
    onApLimitDone: () -> Unit,
    cafeStoredAp: Double,
    cafeLevel: Int,
    serverOptions: List<String>,
    serverIndex: Int,
    showOverviewServerPopup: Boolean,
    overviewServerPopupAnchorBounds: IntRect?,
    onOverviewServerPopupAnchorBoundsChange: (IntRect?) -> Unit,
    onOverviewServerPopupChange: (Boolean) -> Unit,
    onServerSelected: (Int) -> Unit,
    onClaimCafeStoredAp: () -> Unit,
    initState: BAInitState,
    onInitStateChange: (BAInitState) -> Unit,
    disableCardFeedback: Boolean,
) {
    val isWorkActivated = idFriendCode != BA_DEFAULT_FRIEND_CODE
    val apNextPointAt = calculateApNextPointAtMs(
        apLimit = apLimit,
        apCurrent = apCurrent,
        apRegenBaseMs = apRegenBaseMs,
        nowMs = uiNowMs,
    )
    val apFullAt = calculateApFullAtMs(
        apLimit = apLimit,
        apCurrent = apCurrent,
        apRegenBaseMs = apRegenBaseMs,
        nowMs = uiNowMs,
    )
    val apNextPointRemain = formatBaRemainingTime(apNextPointAt, uiNowMs)
    val apSyncTimeText = if (apSyncMs > 0L) formatBaDateTime(apSyncMs) else "未同步"
    val apFullText = formatBaRemainingTime(apFullAt, uiNowMs)
    val apFullTimeText = formatBaDateTime(apFullAt)
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF22C55E)
    val countdownBlue = Color(0xFF60A5FA)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(baCardShape)
            .background(baCardBaseColor, baCardShape)
            .border(
                width = 1.dp,
                color = if (isWorkActivated) {
                    Color(0xFF3B82F6).copy(alpha = if (isDark) 0.30f else 0.18f)
                } else {
                    Color(0xFFF59E0B).copy(alpha = if (isDark) 0.26f else 0.16f)
                },
                shape = baCardShape
            )
            .combinedClickable(
                onClick = {
                    if (initState == BAInitState.Empty) onInitStateChange(BAInitState.Draft)
                },
                onLongClick = { onInitStateChange(BAInitState.Empty) }
            ),
        colors = CardDefaults.defaultColors(
            color = baCardBaseColor,
            contentColor = MiuixTheme.colorScheme.onBackground
        ),
        showIndication = !disableCardFeedback,
        onClick = {
            if (initState == BAInitState.Empty) onInitStateChange(BAInitState.Draft)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                    Text("服务器", color = MiuixTheme.colorScheme.onBackground)
                }
                Box(modifier = Modifier.capturePopupAnchor { onOverviewServerPopupAnchorBoundsChange(it) }) {
                    BaLiquidButton(
                        backdrop = backdrop,
                        text = serverOptions[serverIndex],
                        variant = GlassVariant.Content,
                        onClick = { onOverviewServerPopupChange(!showOverviewServerPopup) }
                    )
                    if (showOverviewServerPopup) {
                        SnapshotWindowListPopup(
                            show = showOverviewServerPopup,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            anchorBounds = overviewServerPopupAnchorBounds,
                            placement = SnapshotPopupPlacement.ButtonEnd,
                            onDismissRequest = { onOverviewServerPopupChange(false) },
                            enableWindowDim = false
                        ) {
                            LiquidDropdownColumn {
                                serverOptions.forEachIndexed { index, server ->
                                    LiquidDropdownImpl(
                                        text = server,
                                        optionSize = serverOptions.size,
                                        isSelected = serverIndex == index,
                                        index = index,
                                        onSelectedIndexChange = { selected -> onServerSelected(selected) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.CenterStart) {
                    Text("AP", color = MiuixTheme.colorScheme.onBackground)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BaLiquidInput(
                        modifier = Modifier.width(72.dp),
                        value = apCurrentInput,
                        onValueChange = onApCurrentInputChange,
                        onImeActionDone = onApCurrentDone,
                        label = "0",
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = accentGreen
                    )
                    Text("/", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    BaLiquidInput(
                        modifier = Modifier.width(72.dp),
                        value = apLimitInput,
                        onValueChange = onApLimitInputChange,
                        onImeActionDone = onApLimitDone,
                        label = "240",
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = accentGreen
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                    Text("咖啡厅AP", color = MiuixTheme.colorScheme.onBackground)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BaLiquidButton(
                        backdrop = backdrop,
                        text = "领取",
                        textColor = Color(0xFF22C55E),
                        variant = GlassVariant.Content,
                        onClick = onClaimCafeStoredAp
                    )
                    BaLiquidButton(
                        backdrop = backdrop,
                        text = "${displayAp(cafeStoredAp)}/${cafeDailyCapacity(cafeLevel)}",
                        textColor = accentGreen,
                        variant = GlassVariant.Content,
                        onClick = {}
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                    Text("AP Sync", color = MiuixTheme.colorScheme.onBackground)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = apNextPointRemain, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(text = apSyncTimeText, color = accentBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                    Text("AP Full", color = MiuixTheme.colorScheme.onBackground)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = apFullText, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(text = apFullTimeText, color = accentBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun BaCafeCard(
    backdrop: Backdrop?,
    baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    baCardBaseColor: Color,
    baCardBorderColor: Color,
    uiNowMs: Long,
    serverIndex: Int,
    coffeeHeadpatMs: Long,
    coffeeInvite1UsedMs: Long,
    coffeeInvite2UsedMs: Long,
    onTouchHead: () -> Unit,
    onForceResetHeadpatCooldown: () -> Unit,
    onUseInviteTicket1: () -> Unit,
    onForceResetInviteTicket1Cooldown: () -> Unit,
    onUseInviteTicket2: () -> Unit,
    onForceResetInviteTicket2Cooldown: () -> Unit,
    onGlassButtonPressedChange: (Boolean) -> Unit,
    disableCardFeedback: Boolean,
) {
    val accentPink = Color(0xFFF472B6)
    val accentYellow = Color(0xFFF59E0B)
    val countdownBlue = Color(0xFF60A5FA)
    val nextHeadpatAt = calculateNextHeadpatAvailableMs(coffeeHeadpatMs, serverIndex)
    val nextStudentRefreshAt = nextCafeStudentRefreshMs(uiNowMs, serverIndex)
    val nextArenaRefreshAt = nextArenaRefreshMs(uiNowMs, serverIndex)
    val nextHeadpatText = if (coffeeHeadpatMs <= 0L || nextHeadpatAt <= uiNowMs) "0s" else formatBaRemainingTime(nextHeadpatAt, uiNowMs)
    val nextStudentRefreshText = formatBaRemainingTime(nextStudentRefreshAt, uiNowMs)
    val nextArenaRefreshText = formatBaRemainingTime(nextArenaRefreshAt, uiNowMs)
    val invite1AvailableAt = calculateInviteTicketAvailableMs(coffeeInvite1UsedMs)
    val invite2AvailableAt = calculateInviteTicketAvailableMs(coffeeInvite2UsedMs)
    val invite1Ready = coffeeInvite1UsedMs <= 0L || invite1AvailableAt <= uiNowMs
    val invite2Ready = coffeeInvite2UsedMs <= 0L || invite2AvailableAt <= uiNowMs
    val invite1Color = if (invite1Ready) accentPink else accentYellow
    val invite2Color = if (invite2Ready) accentPink else accentYellow
    val invite1Text = if (invite1Ready) "0s" else formatBaRemainingTime(invite1AvailableAt, uiNowMs)
    val invite2Text = if (invite2Ready) "0s" else formatBaRemainingTime(invite2AvailableAt, uiNowMs)
    val invite1TimeText = formatBaDateTimeNoSeconds(if (invite1Ready) uiNowMs else invite1AvailableAt)
    val invite2TimeText = formatBaDateTimeNoSeconds(if (invite2Ready) uiNowMs else invite2AvailableAt)

    Card(
        modifier = Modifier.fillMaxWidth().clip(baCardShape).background(baCardBaseColor, baCardShape).border(width = 1.dp, color = baCardBorderColor, shape = baCardShape),
        colors = CardDefaults.defaultColors(color = baCardBaseColor, contentColor = MiuixTheme.colorScheme.onBackground),
        pressFeedbackType = if (disableCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
        showIndication = !disableCardFeedback,
        onClick = {}
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("竞技场", color = accentPink)
                Text(text = nextArenaRefreshText, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("学生访问", color = accentPink)
                Text(text = nextStudentRefreshText, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BaLiquidButton(backdrop = backdrop, text = "摸摸头", textColor = accentPink, enabled = coffeeHeadpatMs <= 0L || nextHeadpatAt <= uiNowMs, variant = GlassVariant.Content, onClick = onTouchHead, onLongClick = onForceResetHeadpatCooldown, onPressedChange = onGlassButtonPressedChange)
                Text(text = nextHeadpatText, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = if (coffeeHeadpatMs > 0L) formatBaDateTimeNoSeconds(coffeeHeadpatMs) else "-", color = accentPink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BaLiquidButton(backdrop = backdrop, text = "邀请券1", textColor = invite1Color, enabled = invite1Ready, variant = GlassVariant.Content, onClick = onUseInviteTicket1, onLongClick = onForceResetInviteTicket1Cooldown, onPressedChange = onGlassButtonPressedChange)
                Text(text = invite1Text, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = invite1TimeText, color = invite1Color, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BaLiquidButton(backdrop = backdrop, text = "邀请券2", textColor = invite2Color, enabled = invite2Ready, variant = GlassVariant.Content, onClick = onUseInviteTicket2, onLongClick = onForceResetInviteTicket2Cooldown, onPressedChange = onGlassButtonPressedChange)
                Text(text = invite2Text, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = invite2TimeText, color = invite2Color, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun BaCalendarCard(
    backdrop: Backdrop?,
    baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    baCardBaseColor: Color,
    baCardBorderColor: Color,
    serverOptions: List<String>,
    serverIndex: Int,
    uiNowMs: Long,
    baCalendarEntries: List<BaCalendarEntry>,
    baCalendarLoading: Boolean,
    baCalendarError: String?,
    baCalendarLastSyncMs: Long,
    showEndedActivities: Boolean,
    showCalendarPoolImages: Boolean,
    disableCardFeedback: Boolean,
    onRefreshCalendar: () -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF22C55E)
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val visibleCalendarEntries = if (showEndedActivities) baCalendarEntries else baCalendarEntries.filter { it.endAtMs > uiNowMs }

    Card(
        modifier = Modifier.fillMaxWidth().clip(baCardShape).background(baCardBaseColor, baCardShape).border(width = 1.dp, color = baCardBorderColor, shape = baCardShape),
        colors = CardDefaults.defaultColors(color = baCardBaseColor, contentColor = MiuixTheme.colorScheme.onBackground),
        pressFeedbackType = if (disableCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
        showIndication = !disableCardFeedback,
        onClick = {}
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GameKee · ${serverOptions[serverIndex]}", color = MiuixTheme.colorScheme.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (baCalendarLoading) "同步中..." else formatBaDateTimeNoYearInTimeZone(baCalendarLastSyncMs, serverTimeZone), color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    BaLiquidIconButton(backdrop = backdrop, icon = MiuixIcons.Regular.Refresh, contentDescription = "刷新活动日历", variant = GlassVariant.Content, onClick = onRefreshCalendar)
                }
            }
            if (!baCalendarError.isNullOrBlank()) {
                Text(text = baCalendarError.orEmpty(), color = Color(0xFFF59E0B), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            } else if (!baCalendarLoading && visibleCalendarEntries.isEmpty()) {
                Text(text = if (showEndedActivities) "暂无活动" else "暂无进行中或即将开始的活动", color = MiuixTheme.colorScheme.onBackgroundVariant)
            } else {
                visibleCalendarEntries.forEachIndexed { index, activity ->
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
                    Column(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpenCalendarLink(activity.linkUrl) }, onLongClick = { onOpenCalendarLink(activity.linkUrl) }), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "${activity.kindName} · ${activity.title}", maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (showCalendarPoolImages) {
                            com.example.keios.ui.page.main.GameKeeCoverImage(imageUrl = activity.imageUrl, modifier = Modifier.fillMaxWidth())
                        }
                        Text(text = "${formatBaDateTimeNoYearInTimeZone(activity.beginAtMs, serverTimeZone)} - ${formatBaDateTimeNoYearInTimeZone(activity.endAtMs, serverTimeZone)}", color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        LinearProgressIndicator(progress = activityProgress(activity, uiNowMs), modifier = Modifier.fillMaxWidth().padding(top = 2.dp), height = 5.dp, colors = ProgressIndicatorDefaults.progressIndicatorColors(foregroundColor = if (activity.isRunning) accentGreen else accentBlue, backgroundColor = MiuixTheme.colorScheme.secondaryContainer))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(statusText, color = statusColor)
                            Text(text = remainText, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                    if (index < visibleCalendarEntries.lastIndex) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun BaPoolCard(
    backdrop: Backdrop?,
    baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    baCardBaseColor: Color,
    baCardBorderColor: Color,
    serverOptions: List<String>,
    serverIndex: Int,
    uiNowMs: Long,
    baPoolEntries: List<BaPoolEntry>,
    baPoolLoading: Boolean,
    baPoolError: String?,
    baPoolLastSyncMs: Long,
    showEndedPools: Boolean,
    showCalendarPoolImages: Boolean,
    disableCardFeedback: Boolean,
    onRefreshPool: () -> Unit,
    onOpenPoolStudentGuide: (String) -> Unit,
    onOpenCalendarLink: (String) -> Unit,
) {
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF22C55E)
    val countdownBlue = Color(0xFF60A5FA)
    val serverTimeZone = serverRefreshTimeZone(serverIndex)
    val visiblePoolEntries = if (showEndedPools) baPoolEntries else baPoolEntries.filter { it.endAtMs > uiNowMs }

    Card(
        modifier = Modifier.fillMaxWidth().clip(baCardShape).background(baCardBaseColor, baCardShape).border(width = 1.dp, color = baCardBorderColor, shape = baCardShape),
        colors = CardDefaults.defaultColors(color = baCardBaseColor, contentColor = MiuixTheme.colorScheme.onBackground),
        pressFeedbackType = if (disableCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
        showIndication = !disableCardFeedback,
        onClick = {}
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GameKee · ${serverOptions[serverIndex]}", color = MiuixTheme.colorScheme.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (baPoolLoading) "同步中..." else formatBaDateTimeNoYearInTimeZone(baPoolLastSyncMs, serverTimeZone), color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    BaLiquidIconButton(backdrop = backdrop, icon = MiuixIcons.Regular.Refresh, contentDescription = "刷新卡池", variant = GlassVariant.Content, onClick = onRefreshPool)
                }
            }
            if (!baPoolError.isNullOrBlank()) {
                Text(text = baPoolError.orEmpty(), color = Color(0xFFF59E0B), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            } else if (!baPoolLoading && visiblePoolEntries.isEmpty()) {
                Text(text = "暂无进行中或即将开始的卡池", color = MiuixTheme.colorScheme.onBackgroundVariant)
            } else {
                visiblePoolEntries.forEachIndexed { index, pool ->
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
                    Column(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpenPoolStudentGuide(pool.linkUrl) }, onLongClick = { onOpenCalendarLink(pool.linkUrl) }), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "${pool.tagName} · ${pool.name}", maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (showCalendarPoolImages) {
                            com.example.keios.ui.page.main.GameKeeCoverImage(imageUrl = pool.imageUrl, modifier = Modifier.fillMaxWidth())
                        }
                        Text(text = "${formatBaDateTimeNoYearInTimeZone(pool.startAtMs, serverTimeZone)} - ${formatBaDateTimeNoYearInTimeZone(pool.endAtMs, serverTimeZone)}", color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        LinearProgressIndicator(progress = poolProgress(pool, uiNowMs), modifier = Modifier.fillMaxWidth().padding(top = 2.dp), height = 5.dp, colors = ProgressIndicatorDefaults.progressIndicatorColors(foregroundColor = if (pool.isRunning) accentGreen else accentBlue, backgroundColor = MiuixTheme.colorScheme.secondaryContainer))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(statusText, color = statusColor)
                            Text(text = remainText, color = countdownBlue, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                    if (index < visiblePoolEntries.lastIndex) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun BaIdCard(
    backdrop: Backdrop?,
    baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    baCardBaseColor: Color,
    baCardBorderColor: Color,
    idNicknameInput: String,
    onIdNicknameInputChange: (String) -> Unit,
    onSaveIdNickname: () -> Unit,
    idFriendCodeInput: String,
    onIdFriendCodeInputChange: (String) -> Unit,
    onSaveIdFriendCode: () -> Unit,
    disableCardFeedback: Boolean,
) {
    val nicknameLengthForWidth = idNicknameInput.ifEmpty { BA_DEFAULT_NICKNAME }.length.coerceIn(1, 10)
    val nicknameFieldWidth = (nicknameLengthForWidth * 11 + 34).coerceIn(72, 124).dp
    val friendCodeLengthForWidth = idFriendCodeInput.ifEmpty { BA_DEFAULT_FRIEND_CODE }.length.coerceIn(1, 8)
    val friendCodeFieldWidth = (friendCodeLengthForWidth * 11 + 34).coerceIn(92, 128).dp
    val accentBlue = Color(0xFF3B82F6)

    Card(
        modifier = Modifier.fillMaxWidth().clip(baCardShape).background(baCardBaseColor, baCardShape).border(width = 1.dp, color = baCardBorderColor, shape = baCardShape),
        colors = CardDefaults.defaultColors(color = baCardBaseColor, contentColor = MiuixTheme.colorScheme.onBackground),
        pressFeedbackType = if (disableCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
        showIndication = !disableCardFeedback,
        onClick = {}
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.CenterStart) {
                    Text("昵称", color = MiuixTheme.colorScheme.onBackground)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BaLiquidInput(modifier = Modifier.width(nicknameFieldWidth), value = idNicknameInput, onValueChange = onIdNicknameInputChange, onImeActionDone = onSaveIdNickname, label = "Kei", backdrop = backdrop, variant = GlassVariant.SheetInput, singleLine = true, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("老师", color = accentBlue)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.CenterStart) {
                    Text("好友码", color = MiuixTheme.colorScheme.onBackground)
                }
                BaLiquidInput(modifier = Modifier.width(friendCodeFieldWidth), value = idFriendCodeInput, onValueChange = onIdFriendCodeInputChange, onImeActionDone = onSaveIdFriendCode, label = "ARISUKEI", backdrop = backdrop, variant = GlassVariant.SheetInput, singleLine = true, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun BaDebugCard(
    backdrop: Backdrop?,
    baCardShape: androidx.compose.foundation.shape.RoundedCornerShape,
    baCardBaseColor: Color,
    baCardBorderColor: Color,
    disableCardFeedback: Boolean,
    onSendApTestNotification: () -> Unit,
    onTestCafePlus3Hours: () -> Unit,
    onGlassButtonPressedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(baCardShape).background(baCardBaseColor, baCardShape).border(width = 1.dp, color = baCardBorderColor, shape = baCardShape),
        colors = CardDefaults.defaultColors(color = baCardBaseColor, contentColor = MiuixTheme.colorScheme.onBackground),
        pressFeedbackType = if (disableCardFeedback) PressFeedbackType.None else PressFeedbackType.Sink,
        showIndication = !disableCardFeedback,
        onClick = {}
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BaLiquidButton(backdrop = backdrop, text = "AP通知", variant = GlassVariant.Content, onClick = onSendApTestNotification, onPressedChange = onGlassButtonPressedChange)
                BaLiquidButton(backdrop = backdrop, text = "咖啡厅3h AP", variant = GlassVariant.Content, onClick = onTestCafePlus3Hours, onPressedChange = onGlassButtonPressedChange)
            }
        }
    }
}
