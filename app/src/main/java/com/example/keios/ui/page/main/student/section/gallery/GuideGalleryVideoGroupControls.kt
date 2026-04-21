package com.example.keios.ui.page.main.student.section.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.glass.AppDropdownAnchorButton
import com.example.keios.ui.page.main.widget.glass.GlassTextButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.glass.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.glass.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.sheet.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.sheet.capturePopupAnchor
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play

@Composable
internal fun GuideGalleryVideoGroupHeaderActions(
    itemsSize: Int,
    optionLabels: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    displayMediaUrl: String,
    saveTargetUrl: String,
    videoInlineExpanded: Boolean,
    videoInlinePlaying: Boolean,
    videoInlineBuffering: Boolean,
    isVideoCached: Boolean,
    backdrop: Backdrop?,
    onToggleInlinePlay: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onSaveMedia: () -> Unit
) {
    if (itemsSize > 1) {
        var showPicker by remember(itemsSize, optionLabels) { mutableStateOf(false) }
        var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
        Box(
            modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it }
        ) {
            AppDropdownAnchorButton(
                backdrop = backdrop,
                text = optionLabels.getOrElse(selectedIndex) { "视频 1" },
                textColor = Color(0xFF3B82F6),
                variant = GlassVariant.Compact,
                onClick = { showPicker = !showPicker }
            )
            if (showPicker) {
                SnapshotWindowListPopup(
                    show = showPicker,
                    alignment = PopupPositionProvider.Align.BottomEnd,
                    anchorBounds = pickerPopupAnchorBounds,
                    placement = SnapshotPopupPlacement.ButtonEnd,
                    onDismissRequest = { showPicker = false },
                    enableWindowDim = false
                ) {
                    LiquidDropdownColumn {
                        optionLabels.forEachIndexed { idx, option ->
                            LiquidDropdownImpl(
                                text = option,
                                optionSize = optionLabels.size,
                                isSelected = selectedIndex == idx,
                                index = idx,
                                onSelectedIndexChange = { selected ->
                                    onSelectedIndexChange(selected)
                                    showPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (displayMediaUrl.isNotBlank()) {
        // Header indicator represents readiness/cache status, not playback progress.
        val indicatorProgress = when {
            isVideoCached -> 1f
            videoInlineBuffering -> 0.35f
            else -> 0.08f
        }
        val progressForegroundColor = if (isVideoCached) {
            Color(0xFF34C759)
        } else {
            Color(0xFF3B82F6)
        }
        val progressBackgroundColor = if (isVideoCached) {
            Color(0x5534C759)
        } else {
            Color(0x553B82F6)
        }
        CircularProgressIndicator(
            progress = indicatorProgress,
            size = 18.dp,
            strokeWidth = 2.dp,
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = progressForegroundColor,
                backgroundColor = progressBackgroundColor
            )
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = if (videoInlineExpanded && videoInlinePlaying) {
                MiuixIcons.Regular.Pause
            } else {
                MiuixIcons.Regular.Play
            },
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = onToggleInlinePlay
        )
        GlassTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = MiuixIcons.Regular.ExpandMore,
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = onOpenFullscreen
        )
    }

    if (saveTargetUrl.isNotBlank()) {
        GlassTextButton(
            backdrop = backdrop,
            text = "",
            leadingIcon = MiuixIcons.Regular.Download,
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            onClick = onSaveMedia
        )
    }
}
