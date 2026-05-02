package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DropdownNeutralTint = Color(0xFF3B82F6)

private fun dropdownAnchorTint(
    textColor: Color,
    variant: GlassVariant
): Color {
    return if (textColor.alpha <= 0f) {
        when (variant) {
            GlassVariant.SheetDangerAction -> Color(0xFFE25B6A)
            else -> DropdownNeutralTint
        }
    } else {
        textColor
    }
}

@Composable
fun AppDropdownAnchorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    variant: GlassVariant = GlassVariant.SheetAction,
    enabled: Boolean = true,
    textColor: Color = MiuixTheme.colorScheme.primary,
    minHeight: Dp = AppInteractiveTokens.compactAppLiquidTextButtonMinHeight,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 6.dp
) {
    val accentColor = dropdownAnchorTint(textColor = textColor, variant = variant)
    if (backdrop != null) {
        AppLiquidTextButton(
            backdrop = backdrop,
            text = text,
            onClick = onClick,
            modifier = modifier,
            textColor = textColor,
            containerColor = accentColor,
            enabled = enabled,
            variant = variant,
            minHeight = minHeight,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            textSoftWrap = false
        )
    } else {
        AppStandaloneLiquidTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            textColor = textColor,
            containerColor = accentColor,
            enabled = enabled,
            variant = variant,
            minHeight = minHeight,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            textSoftWrap = false
        )
    }
}

@Composable
fun AppDropdownSelector(
    selectedText: String,
    options: List<String>,
    selectedIndex: Int,
    expanded: Boolean,
    anchorBounds: IntRect?,
    onExpandedChange: (Boolean) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    onAnchorBoundsChange: (IntRect?) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    variant: GlassVariant = GlassVariant.SheetAction,
    textColor: Color = MiuixTheme.colorScheme.primary,
    minHeight: Dp = AppInteractiveTokens.compactAppLiquidTextButtonMinHeight,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 6.dp,
    anchorAlignment: Alignment = Alignment.CenterStart,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.BottomEnd,
    placement: SnapshotPopupPlacement = SnapshotPopupPlacement.ButtonEnd
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.capturePopupAnchor { onAnchorBoundsChange(it) },
        contentAlignment = anchorAlignment
    ) {
        AppDropdownAnchorButton(
            text = selectedText,
            onClick = { onExpandedChange(!expanded) },
            backdrop = backdrop,
            variant = variant,
            enabled = options.isNotEmpty(),
            textColor = textColor,
            minHeight = minHeight,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding
        )
        if (expanded && options.isNotEmpty()) {
            SnapshotWindowListPopup(
                show = true,
                alignment = alignment,
                anchorBounds = anchorBounds,
                placement = placement,
                onDismissRequest = { onExpandedChange(false) },
                enableWindowDim = false
            ) {
                val accentColor = dropdownAnchorTint(textColor = textColor, variant = variant)
                AppLiquidGlassDropdownColumn(
                    accentColor = accentColor,
                    initialScrollItemIndex = selectedIndex,
                    backdrop = backdrop
                ) {
                    DropdownSelectorChoiceList(
                        options = options,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = onSelectedIndexChange,
                        onExpandedChange = onExpandedChange,
                        accentColor = accentColor,
                        variant = variant
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownSelectorChoiceList(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    accentColor: Color,
    variant: GlassVariant
) {
    LiquidGlassDropdownSingleChoiceList(
        options = options,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selected ->
            onSelectedIndexChange(selected)
            onExpandedChange(false)
        },
        accentColor = accentColor,
        variant = variant
    )
}
