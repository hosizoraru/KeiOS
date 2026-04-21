package com.example.keios.ui.page.main.ba.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.ui.page.main.ba.support.BAInitState
import com.example.keios.ui.page.main.ba.support.BA_DEFAULT_FRIEND_CODE
import com.example.keios.ui.page.main.ba.BaGlassCard
import com.example.keios.ui.page.main.ba.BaGlassMetricPanel
import com.example.keios.ui.page.main.ba.BaGlassPanel
import com.example.keios.ui.page.main.ba.support.cafeDailyCapacity
import com.example.keios.ui.page.main.ba.support.calculateApFullAtMs
import com.example.keios.ui.page.main.ba.support.calculateApNextPointAtMs
import com.example.keios.ui.page.main.ba.support.displayAp
import com.example.keios.ui.page.main.ba.support.formatBaDateTime
import com.example.keios.ui.page.main.ba.support.formatBaRemainingTime
import com.example.keios.ui.page.main.widget.glass.AppDropdownSelector
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassSearchField
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
                        painter = painterResource(id = R.drawable.item_icon_consumable_ap_3),
                        contentDescription = "领取咖啡厅AP",
                        variant = GlassVariant.Content,
                        iconTint = Color.Unspecified,
                        onClick = onClaimCafeStoredAp,
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "${displayAp(cafeStoredAp)}/${cafeDailyCapacity(cafeLevel)}",
                        textColor = accentGreen,
                        containerColor = accentGreen,
                        variant = GlassVariant.Floating,
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
