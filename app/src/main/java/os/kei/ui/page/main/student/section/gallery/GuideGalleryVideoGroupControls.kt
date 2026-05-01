package os.kei.ui.page.main.student.section.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import os.kei.R
import os.kei.ui.page.main.os.appLucideFullscreenIcon
import os.kei.ui.page.main.widget.glass.AppDropdownAnchorButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownSingleChoiceItem
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
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
    backdrop: Backdrop?,
    onToggleInlinePlay: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onSaveMedia: () -> Unit
) {
    val fullscreenIcon = appLucideFullscreenIcon()
    if (itemsSize > 1) {
        var showPicker by remember(itemsSize, optionLabels) { mutableStateOf(false) }
        var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
        Box(
            modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it }
        ) {
            AppDropdownAnchorButton(
                backdrop = backdrop,
                text = optionLabels.getOrElse(selectedIndex) {
                    stringResource(R.string.guide_gallery_video_format, 1)
                },
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
                    LiquidGlassDropdownColumn(
                        accentColor = Color(0xFF3B82F6),
                        backdrop = backdrop
                    ) {
                        optionLabels.forEachIndexed { idx, option ->
                            LiquidGlassDropdownSingleChoiceItem(
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
            leadingIcon = fullscreenIcon,
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
