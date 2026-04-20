package com.example.keios.ui.page.main.student.catalog.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.res.stringResource
import com.example.keios.ui.page.main.student.catalog.state.BaGuideCatalogSortMode
import com.example.keios.ui.page.main.widget.glass.LiquidDropdownColumn
import com.example.keios.ui.page.main.widget.glass.LiquidDropdownImpl
import com.example.keios.ui.page.main.widget.sheet.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowListPopup
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

@Composable
internal fun BaGuideCatalogSortActionPopup(
    show: Boolean,
    anchorBounds: IntRect?,
    sortMode: BaGuideCatalogSortMode,
    onDismissRequest: () -> Unit,
    onSelectSortMode: (BaGuideCatalogSortMode) -> Unit
) {
    if (!show) return
    SnapshotWindowListPopup(
        show = show,
        alignment = PopupPositionProvider.Align.BottomStart,
        anchorBounds = anchorBounds,
        placement = SnapshotPopupPlacement.ActionBarCenter,
        onDismissRequest = onDismissRequest,
        enableWindowDim = false
    ) {
        LiquidDropdownColumn {
            val modes = BaGuideCatalogSortMode.entries
            modes.forEachIndexed { index, mode ->
                LiquidDropdownImpl(
                    text = stringResource(mode.labelRes),
                    optionSize = modes.size,
                    isSelected = sortMode == mode,
                    index = index,
                    onSelectedIndexChange = { selectedIndex ->
                        onSelectSortMode(modes[selectedIndex])
                    }
                )
            }
        }
    }
}
