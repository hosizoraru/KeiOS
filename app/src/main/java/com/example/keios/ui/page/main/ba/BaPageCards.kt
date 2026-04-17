package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.ui.page.main.widget.AppDropdownSelector
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun BaCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleIconRes: Int? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            titleIconRes?.let { iconRes ->
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        trailing?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = it,
            )
        }
    }
}

@Composable
private fun BaInlineActionPanel(
    backdrop: Backdrop?,
    buttonText: String,
    buttonIconRes: Int? = null,
    countdownText: String,
    timeText: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val countdownBlue = Color(0xFF60A5FA)
    BaGlassPanel(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        accentColor = accentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (buttonIconRes != null) {
                GlassIconButton(
                    backdrop = backdrop,
                    painter = painterResource(id = buttonIconRes),
                    contentDescription = buttonText,
                    onClick = {
                        if (enabled) onClick()
                    },
                    onLongClick = onLongClick,
                    variant = GlassVariant.Content,
                    width = 52.dp,
                    height = 40.dp,
                    iconTint = Color.Unspecified
                )
            } else {
                GlassTextButton(
                    backdrop = backdrop,
                    text = buttonText,
                    textColor = accentColor,
                    enabled = enabled,
                    variant = GlassVariant.Content,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            }
            Text(
                text = countdownText,
                color = countdownBlue,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeText,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun BaOverviewCard(
    backdrop: Backdrop?,
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
    onOpenGuideCatalog: () -> Unit,
    initState: BAInitState,
    onInitStateChange: (BAInitState) -> Unit,
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
    val accentAmber = Color(0xFFF59E0B)
    val stateAccent = if (isWorkActivated) accentBlue else accentAmber

    BaGlassCard(
        backdrop = backdrop,
        accentColor = stateAccent,
        accentAlpha = 0f,
        onClick = {
            if (initState == BAInitState.Empty) onInitStateChange(BAInitState.Draft)
        },
        onLongClick = { onInitStateChange(BAInitState.Empty) },
    ) {
        BaCardHeader(
            title = "办公室总览",
            trailing = {
                Text(
                    text = if (isWorkActivated) "已激活" else "默认",
                    color = stateAccent,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            },
        )

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = stateAccent,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.heightIn(min = 40.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("学生/NPC/卫星图鉴", color = MiuixTheme.colorScheme.onBackground)
                    }
                    GlassIconButton(
                        backdrop = backdrop,
                        painter = painterResource(id = R.drawable.mp_student),
                        contentDescription = "打开图鉴",
                        variant = GlassVariant.Content,
                        onClick = onOpenGuideCatalog,
                        width = 48.dp,
                        height = 34.dp,
                        iconTint = Color.Unspecified,
                        iconModifier = Modifier
                            .width(26.dp)
                            .height(26.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.heightIn(min = 40.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("服务器", color = MiuixTheme.colorScheme.onBackground)
                    }
                    AppDropdownSelector(
                        selectedText = serverOptions[serverIndex],
                        options = serverOptions,
                        selectedIndex = serverIndex,
                        expanded = showOverviewServerPopup,
                        anchorBounds = overviewServerPopupAnchorBounds,
                        onExpandedChange = onOverviewServerPopupChange,
                        onSelectedIndexChange = onServerSelected,
                        onAnchorBoundsChange = onOverviewServerPopupAnchorBoundsChange,
                        backdrop = backdrop,
                        variant = GlassVariant.Content
                    )
                }
            }
        }

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = accentGreen,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("AP", color = accentGreen, fontWeight = FontWeight.Bold)
                        Image(
                            painter = painterResource(id = R.drawable.ba_ap_icon_tight),
                            contentDescription = "AP Icon",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassSearchField(
                        modifier = Modifier.width(72.dp),
                        value = apCurrentInput,
                        onValueChange = onApCurrentInputChange,
                        onImeActionDone = onApCurrentDone,
                        label = "0",
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = accentGreen,
                    )
                    Text("/", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    GlassSearchField(
                        modifier = Modifier.width(72.dp),
                        value = apLimitInput,
                        onValueChange = onApLimitInputChange,
                        onImeActionDone = onApLimitDone,
                        label = "240",
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = accentGreen,
                    )
                }
            }
            Text(
                text = "AP +1 $apNextPointRemain · Full AP $apFullText",
                color = Color(0xFF60A5FA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = accentGreen,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("咖啡厅AP", color = MiuixTheme.colorScheme.onBackground)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(
                        backdrop = backdrop,
                        painter = painterResource(id = R.drawable.goods_icon_biweekly_0),
                        contentDescription = "领取咖啡厅AP",
                        variant = GlassVariant.Content,
                        iconTint = Color.Unspecified,
                        onClick = onClaimCafeStoredAp,
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "${displayAp(cafeStoredAp)}/${cafeDailyCapacity(cafeLevel)}",
                        textColor = accentGreen,
                        enabled = false,
                        variant = GlassVariant.Content,
                        onClick = {},
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BaGlassMetricPanel(
                backdrop = backdrop,
                label = "AP Sync",
                value = apSyncTimeText,
                accentColor = accentBlue,
                valueColor = accentBlue,
                modifier = Modifier.weight(1f),
            )
            BaGlassMetricPanel(
                backdrop = backdrop,
                label = "AP Full",
                value = apFullTimeText,
                accentColor = Color(0xFF60A5FA),
                valueColor = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun BaCafeCard(
    backdrop: Backdrop?,
    uiNowMs: Long,
    serverIndex: Int,
    cafeLevel: Int,
    cafeLevelOptions: List<Int>,
    showCafeLevelPopup: Boolean,
    cafeLevelPopupAnchorBounds: IntRect?,
    onCafeLevelPopupAnchorBoundsChange: (IntRect?) -> Unit,
    onCafeLevelPopupChange: (Boolean) -> Unit,
    onCafeLevelChange: (Int) -> Unit,
    coffeeHeadpatMs: Long,
    coffeeInvite1UsedMs: Long,
    coffeeInvite2UsedMs: Long,
    onTouchHead: () -> Unit,
    onForceResetHeadpatCooldown: () -> Unit,
    onUseInviteTicket1: () -> Unit,
    onForceResetInviteTicket1Cooldown: () -> Unit,
    onUseInviteTicket2: () -> Unit,
    onForceResetInviteTicket2Cooldown: () -> Unit,
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
    val headpatTimeText = if (coffeeHeadpatMs > 0L) formatBaDateTimeNoSeconds(coffeeHeadpatMs) else "-"

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentPink,
        accentAlpha = 0f,
    ) {
        BaCardHeader(
            title = "咖啡厅",
            trailing = {
                GlassIconButton(
                    backdrop = backdrop,
                    painter = painterResource(id = R.drawable.mp_cafe),
                    contentDescription = "咖啡厅",
                    onClick = {},
                    variant = GlassVariant.Content,
                    width = 52.dp,
                    height = 40.dp,
                    iconTint = Color.Unspecified,
                    iconModifier = Modifier
                        .width(30.dp)
                        .height(22.dp)
                )
                AppDropdownSelector(
                    selectedText = "Lv$cafeLevel",
                    options = cafeLevelOptions.map { level -> "Lv$level" },
                    selectedIndex = cafeLevelOptions.indexOf(cafeLevel).coerceAtLeast(0),
                    expanded = showCafeLevelPopup,
                    anchorBounds = cafeLevelPopupAnchorBounds,
                    onExpandedChange = onCafeLevelPopupChange,
                    onSelectedIndexChange = { selected ->
                        onCafeLevelChange(cafeLevelOptions[selected])
                    },
                    onAnchorBoundsChange = onCafeLevelPopupAnchorBoundsChange,
                    backdrop = backdrop,
                    variant = GlassVariant.Content,
                    textColor = accentPink
                )
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BaGlassMetricPanel(
                backdrop = backdrop,
                label = "竞技场",
                value = nextArenaRefreshText,
                accentColor = accentPink,
                valueColor = countdownBlue,
                modifier = Modifier.weight(1f),
            )
            BaGlassMetricPanel(
                backdrop = backdrop,
                label = "学生访问",
                value = nextStudentRefreshText,
                accentColor = accentPink,
                valueColor = countdownBlue,
                modifier = Modifier.weight(1f),
            )
        }

        BaInlineActionPanel(
            backdrop = backdrop,
            buttonText = "摸摸头",
            buttonIconRes = R.drawable.mp_bond,
            countdownText = nextHeadpatText,
            timeText = headpatTimeText,
            accentColor = accentPink,
            enabled = coffeeHeadpatMs <= 0L || nextHeadpatAt <= uiNowMs,
            onClick = onTouchHead,
            onLongClick = onForceResetHeadpatCooldown,
        )

        BaInlineActionPanel(
            backdrop = backdrop,
            buttonText = "邀请券1",
            countdownText = invite1Text,
            timeText = invite1TimeText,
            accentColor = invite1Color,
            enabled = invite1Ready,
            onClick = onUseInviteTicket1,
            onLongClick = onForceResetInviteTicket1Cooldown,
        )

        BaInlineActionPanel(
            backdrop = backdrop,
            buttonText = "邀请券2",
            countdownText = invite2Text,
            timeText = invite2TimeText,
            accentColor = invite2Color,
            enabled = invite2Ready,
            onClick = onUseInviteTicket2,
            onLongClick = onForceResetInviteTicket2Cooldown,
        )
    }
}

@Composable
internal fun BaCalendarCard(
    backdrop: Backdrop?,
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
    val visibleCalendarEntries = if (showEndedActivities) baCalendarEntries else baCalendarEntries.filter { it.endAtMs > uiNowMs }

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

@Composable
internal fun BaPoolCard(
    backdrop: Backdrop?,
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
    val visiblePoolEntries = if (showEndedPools) baPoolEntries else baPoolEntries.filter { it.endAtMs > uiNowMs }

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

@Composable
internal fun BaIdCard(
    backdrop: Backdrop?,
    idNicknameInput: String,
    onIdNicknameInputChange: (String) -> Unit,
    onSaveIdNickname: () -> Unit,
    idFriendCodeInput: String,
    onIdFriendCodeInputChange: (String) -> Unit,
    onSaveIdFriendCode: () -> Unit,
) {
    val nicknameLengthForWidth = idNicknameInput.ifEmpty { BA_DEFAULT_NICKNAME }.length.coerceIn(1, 10)
    val nicknameFieldWidth = (nicknameLengthForWidth * 11 + 34).coerceIn(72, 124).dp
    val friendCodeLengthForWidth = idFriendCodeInput.ifEmpty { BA_DEFAULT_FRIEND_CODE }.length.coerceIn(1, 8)
    val friendCodeFieldWidth = (friendCodeLengthForWidth * 11 + 34).coerceIn(92, 128).dp
    val accentBlue = Color(0xFF3B82F6)

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentBlue,
        accentAlpha = 0f,
    ) {
        BaCardHeader(title = "ID 卡")

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = accentBlue,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("昵称", color = MiuixTheme.colorScheme.onBackground)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassSearchField(
                        modifier = Modifier.width(nicknameFieldWidth),
                        value = idNicknameInput,
                        onValueChange = onIdNicknameInputChange,
                        onImeActionDone = onSaveIdNickname,
                        label = BA_DEFAULT_NICKNAME,
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "老师",
                        color = accentBlue,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        BaGlassPanel(
            backdrop = backdrop,
            accentColor = accentBlue,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("好友码", color = MiuixTheme.colorScheme.onBackground)
                GlassSearchField(
                    modifier = Modifier.width(friendCodeFieldWidth),
                    value = idFriendCodeInput,
                    onValueChange = onIdFriendCodeInputChange,
                    onImeActionDone = onSaveIdFriendCode,
                    label = BA_DEFAULT_FRIEND_CODE,
                    backdrop = backdrop,
                    variant = GlassVariant.SheetInput,
                    singleLine = true,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun BaDebugCard(
    backdrop: Backdrop?,
    onSendApTestNotification: () -> Unit,
    onTestCafePlus3Hours: () -> Unit,
) {
    val accentAmber = Color(0xFFF59E0B)

    BaGlassCard(
        backdrop = backdrop,
        accentColor = accentAmber,
        accentAlpha = 0f,
    ) {
        BaCardHeader(title = "Debug")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassTextButton(
                backdrop = backdrop,
                text = "AP 通知",
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.SheetPrimaryAction,
                onClick = onSendApTestNotification,
            )
            GlassTextButton(
                backdrop = backdrop,
                text = "咖啡厅 3h AP",
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.SheetPrimaryAction,
                onClick = onTestCafePlus3Hours,
            )
        }
    }
}
