package com.example.keios.ui.page.main.ba

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.keios.R
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.kyant.backdrop.Backdrop
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
        title = stringResource(R.string.ba_settings_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                variant = GlassVariant.Bar,
                onClick = onDismissRequest,
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = stringResource(R.string.common_save),
                variant = GlassVariant.Bar,
                onClick = onSaveRequest,
            )
        },
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionTitle(stringResource(R.string.ba_settings_section_basic))
            BaGlassPanel(
                backdrop = backdrop,
                accentColor = Color(0xFF3B82F6),
                variant = GlassVariant.SheetAction,
            ) {
                SheetControlRow(label = stringResource(R.string.ba_settings_label_ap_notify)) {
                    Switch(
                        checked = state.apNotifyEnabled,
                        onCheckedChange = onApNotifyEnabledChange,
                    )
                }
                if (state.apNotifyEnabled) {
                    SheetControlRow(
                        label = stringResource(R.string.ba_settings_label_ap_threshold),
                        summary = stringResource(R.string.ba_settings_summary_ap_threshold, BA_AP_MAX),
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
            SheetSectionTitle(stringResource(R.string.ba_settings_section_content))
            BaGlassPanel(
                backdrop = backdrop,
                accentColor = Color(0xFF60A5FA),
                variant = GlassVariant.SheetAction,
            ) {
                SheetControlRow(label = stringResource(R.string.ba_settings_label_show_ended_activity)) {
                    Switch(
                        checked = state.showEndedActivities,
                        onCheckedChange = onShowEndedActivitiesChange,
                    )
                }
                SheetControlRow(label = stringResource(R.string.ba_settings_label_show_ended_pool)) {
                    Switch(
                        checked = state.showEndedPools,
                        onCheckedChange = onShowEndedPoolsChange,
                    )
                }
                SheetControlRow(label = stringResource(R.string.ba_settings_label_show_images)) {
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
                    text = stringResource(R.string.ba_settings_note_timezone),
                    color = Color(0xFFF59E0B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
