package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.keios.ui.page.main.BA_AP_MAX
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok

internal data class BaSettingsSheetState(
    val cafeLevel: Int,
    val apNotifyEnabled: Boolean,
    val apNotifyThresholdText: String,
    val showEndedActivities: Boolean,
    val showEndedPools: Boolean,
    val showCalendarPoolImages: Boolean,
)

@Composable
internal fun BaSettingsSheet(
    show: Boolean,
    backdrop: Backdrop?,
    state: BaSettingsSheetState,
    cafeLevelOptions: List<Int>,
    showCafeLevelPopup: Boolean,
    cafeLevelPopupAnchorBounds: IntRect?,
    onCafeLevelPopupAnchorBoundsChange: (IntRect?) -> Unit,
    onShowCafeLevelPopupChange: (Boolean) -> Unit,
    onCafeLevelChange: (Int) -> Unit,
    onApNotifyEnabledChange: (Boolean) -> Unit,
    onApNotifyThresholdTextChange: (String) -> Unit,
    onApNotifyThresholdDone: () -> Unit,
    onShowEndedActivitiesChange: (Boolean) -> Unit,
    onShowEndedPoolsChange: (Boolean) -> Unit,
    onShowCalendarPoolImagesChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveRequest: () -> Unit,
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = "BA 配置",
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                variant = GlassVariant.Bar,
                onClick = onDismissRequest,
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "保存",
                variant = GlassVariant.Bar,
                onClick = onSaveRequest,
            )
        },
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionTitle("基础设置")
            BaGlassPanel(
                backdrop = backdrop,
                accentColor = Color(0xFF3B82F6),
                variant = GlassVariant.SheetAction,
            ) {
                SheetControlRow(label = "咖啡厅等级") {
                    Box(modifier = Modifier.capturePopupAnchor(onCafeLevelPopupAnchorBoundsChange)) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = "${state.cafeLevel}级",
                            variant = GlassVariant.SheetAction,
                            onClick = { onShowCafeLevelPopupChange(!showCafeLevelPopup) },
                        )
                        if (showCafeLevelPopup) {
                            SnapshotWindowListPopup(
                                show = showCafeLevelPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = cafeLevelPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { onShowCafeLevelPopupChange(false) },
                                enableWindowDim = false,
                            ) {
                                LiquidDropdownColumn {
                                    cafeLevelOptions.forEachIndexed { index, level ->
                                        LiquidDropdownImpl(
                                            text = "${level}级",
                                            optionSize = cafeLevelOptions.size,
                                            isSelected = state.cafeLevel == level,
                                            index = index,
                                            onSelectedIndexChange = { selected ->
                                                onCafeLevelChange(cafeLevelOptions[selected])
                                                onShowCafeLevelPopupChange(false)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                SheetControlRow(label = "AP 通知") {
                    Switch(
                        checked = state.apNotifyEnabled,
                        onCheckedChange = onApNotifyEnabledChange,
                    )
                }
                if (state.apNotifyEnabled) {
                    SheetControlRow(
                        label = "AP 提醒阈值",
                        summary = "建议 0-$BA_AP_MAX",
                    ) {
                        GlassSearchField(
                            modifier = Modifier.width(70.dp),
                            value = state.apNotifyThresholdText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(3)
                                val normalized = if (digits.isBlank()) {
                                    ""
                                } else {
                                    digits.toIntOrNull()?.coerceIn(0, BA_AP_MAX)?.toString().orEmpty()
                                }
                                onApNotifyThresholdTextChange(normalized)
                            },
                            onImeActionDone = onApNotifyThresholdDone,
                            label = "120",
                            backdrop = backdrop,
                            variant = GlassVariant.SheetInput,
                            singleLine = true,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            textColor = Color(0xFF22C55E),
                        )
                    }
                }
            }
            SheetSectionTitle("显示内容")
            BaGlassPanel(
                backdrop = backdrop,
                accentColor = Color(0xFF60A5FA),
                variant = GlassVariant.SheetAction,
            ) {
                SheetControlRow(label = "显示已结束活动") {
                    Switch(
                        checked = state.showEndedActivities,
                        onCheckedChange = onShowEndedActivitiesChange,
                    )
                }
                SheetControlRow(label = "显示已结束卡池") {
                    Switch(
                        checked = state.showEndedPools,
                        onCheckedChange = onShowEndedPoolsChange,
                    )
                }
                SheetControlRow(label = "显示活动/卡池图片") {
                    Switch(
                        checked = state.showCalendarPoolImages,
                        onCheckedChange = onShowCalendarPoolImagesChange,
                    )
                }
            }
            BaGlassPanel(
                backdrop = backdrop,
                accentColor = Color(0xFFF59E0B),
                variant = GlassVariant.SheetAction,
            ) {
                Text(
                    text = "不同服务器时区不同，建议按实际游玩服务器设置。",
                    color = Color(0xFFF59E0B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
