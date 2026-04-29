package os.kei.ui.page.main.ba.card

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.R
import os.kei.ui.page.main.ba.support.BAInitState
import os.kei.ui.page.main.ba.support.BA_DEFAULT_FRIEND_CODE
import os.kei.ui.page.main.ba.BaGlassCard
import os.kei.ui.page.main.ba.BaGlassMetricPanel
import os.kei.ui.page.main.ba.BaGlassPanel
import os.kei.ui.page.main.ba.support.cafeDailyCapacity
import os.kei.ui.page.main.ba.support.calculateApFullAtMs
import os.kei.ui.page.main.ba.support.calculateApNextPointAtMs
import os.kei.ui.page.main.ba.support.displayAp
import os.kei.ui.page.main.ba.support.formatBaDateTime
import os.kei.ui.page.main.ba.support.formatBaRemainingTime
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.GlassIconButton
import os.kei.ui.page.main.widget.glass.GlassSearchField
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaOverviewCard(
    backdrop: Backdrop?,
    overviewTitle: String,
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
    val apSyncTimeText = if (apSyncMs > 0L) formatBaDateTime(apSyncMs) else stringResource(R.string.ba_state_not_synced)
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
            title = overviewTitle,
            trailing = {
                Text(
                    text = if (isWorkActivated) {
                        stringResource(R.string.ba_state_activated)
                    } else {
                        stringResource(R.string.ba_state_default)
                    },
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
                        Text(stringResource(R.string.ba_overview_catalog_title), color = MiuixTheme.colorScheme.onBackground)
                    }
                    GlassIconButton(
                        backdrop = backdrop,
                        painter = painterResource(id = R.drawable.common_icon_dailyreward),
                        contentDescription = stringResource(R.string.ba_overview_cd_open_catalog),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.ba_overview_server_label), color = MiuixTheme.colorScheme.onBackground)
                            Image(
                                painter = painterResource(id = R.drawable.lobby_icon_work),
                                contentDescription = stringResource(R.string.ba_overview_cd_server_icon),
                                modifier = Modifier.size(18.dp),
                            )
                        }
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
                            contentDescription = stringResource(R.string.ba_overview_cd_ap_icon),
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
                text = stringResource(R.string.ba_overview_ap_regen_status, apNextPointRemain, apFullText),
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
                    Text(stringResource(R.string.ba_overview_cafe_ap_title), color = MiuixTheme.colorScheme.onBackground)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(
                        backdrop = backdrop,
                        painter = painterResource(id = R.drawable.item_icon_consumable_ap_3),
                        contentDescription = stringResource(R.string.ba_overview_cd_claim_cafe_ap),
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
                label = stringResource(R.string.ba_metric_ap_sync),
                value = apSyncTimeText,
                accentColor = accentBlue,
                valueColor = accentBlue,
                modifier = Modifier.weight(1f),
            )
            BaGlassMetricPanel(
                backdrop = backdrop,
                label = stringResource(R.string.ba_metric_ap_full),
                value = apFullTimeText,
                accentColor = Color(0xFF60A5FA),
                valueColor = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
