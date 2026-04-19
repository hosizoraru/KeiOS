package com.example.keios.ui.page.main.ba

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.keios.R
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetFieldBlock
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
    val cafeVisitNotifyEnabled: Boolean,
    val apNotifyThresholdText: String,
    val mediaAdaptiveRotationEnabled: Boolean,
    val mediaSaveCustomEnabled: Boolean,
    val mediaSaveFixedTreeUri: String,
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
    onCafeVisitNotifyEnabledChange: (Boolean) -> Unit,
    onApNotifyThresholdTextChange: (String) -> Unit,
    onApNotifyThresholdDone: () -> Unit,
    onMediaAdaptiveRotationEnabledChange: (Boolean) -> Unit,
    onMediaSaveCustomEnabledChange: (Boolean) -> Unit,
    onMediaSaveFixedTreeUriChange: (String) -> Unit,
    onShowEndedActivitiesChange: (Boolean) -> Unit,
    onShowEndedPoolsChange: (Boolean) -> Unit,
    onShowCalendarPoolImagesChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveRequest: () -> Unit,
) {
    val context = LocalContext.current
    val settingsAccent = Color(0xFF3B82F6)
    val pickMediaSaveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val treeUri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        onMediaSaveFixedTreeUriChange(treeUri.toString())
    }

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
                accentColor = settingsAccent,
                variant = GlassVariant.SheetAction,
            ) {
                Text(
                    text = stringResource(R.string.ba_settings_card_ap_title),
                    color = settingsAccent,
                )
                SheetControlRow(label = stringResource(R.string.ba_settings_label_ap_notify)) {
                    Switch(
                        checked = state.apNotifyEnabled,
                        onCheckedChange = onApNotifyEnabledChange,
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.ba_settings_label_cafe_visit_notify),
                    summary = stringResource(R.string.ba_settings_summary_cafe_visit_notify),
                ) {
                    Switch(
                        checked = state.cafeVisitNotifyEnabled,
                        onCheckedChange = onCafeVisitNotifyEnabledChange,
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
            BaGlassPanel(
                backdrop = backdrop,
                accentColor = settingsAccent,
                variant = GlassVariant.SheetAction,
            ) {
                Text(
                    text = stringResource(R.string.ba_settings_card_media_title),
                    color = settingsAccent,
                )
                SheetControlRow(
                    label = stringResource(R.string.ba_settings_label_media_adaptive_rotation),
                    summary = stringResource(R.string.ba_settings_summary_media_adaptive_rotation),
                ) {
                    Switch(
                        checked = state.mediaAdaptiveRotationEnabled,
                        onCheckedChange = onMediaAdaptiveRotationEnabledChange,
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.ba_settings_label_media_save_custom),
                    summary = stringResource(R.string.ba_settings_summary_media_save_custom),
                ) {
                    Switch(
                        checked = state.mediaSaveCustomEnabled,
                        onCheckedChange = onMediaSaveCustomEnabledChange,
                    )
                }
                if (state.mediaSaveCustomEnabled) {
                    SheetFieldBlock(
                        title = stringResource(R.string.ba_settings_label_media_save_fixed_uri),
                        summary = if (state.mediaSaveFixedTreeUri.isBlank()) {
                            stringResource(R.string.ba_settings_summary_media_save_fixed_uri_empty)
                        } else {
                            stringResource(R.string.ba_settings_summary_media_save_fixed_uri_ready)
                        },
                        trailing = {
                            GlassTextButton(
                                backdrop = backdrop,
                                variant = GlassVariant.SheetPrimaryAction,
                                textColor = Color(0xFF3B82F6),
                                text = stringResource(R.string.ba_settings_action_pick_media_save_location),
                                onClick = {
                                    val currentTreeUri = state.mediaSaveFixedTreeUri
                                        .takeIf { it.isNotBlank() }
                                        ?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
                                    val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                        addFlags(
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            putExtra(
                                                DocumentsContract.EXTRA_INITIAL_URI,
                                                currentTreeUri ?: Uri.parse(
                                                    "content://com.android.externalstorage.documents/tree/primary%3ADownload"
                                                )
                                            )
                                        }
                                    }
                                    pickMediaSaveFolderLauncher.launch(pickerIntent)
                                }
                            )
                        }
                    ) {
                        GlassSearchField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.mediaSaveFixedTreeUri,
                            onValueChange = { onMediaSaveFixedTreeUriChange(it.trim()) },
                            label = stringResource(R.string.ba_settings_hint_media_save_fixed_uri),
                            backdrop = backdrop,
                            variant = GlassVariant.SheetInput,
                            singleLine = true,
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
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
