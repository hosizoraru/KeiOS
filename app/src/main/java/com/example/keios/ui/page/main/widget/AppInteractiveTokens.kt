package com.example.keios.ui.page.main.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppInteractiveTokens {
    val controlRowMinHeight: Dp = 50.dp
    val compactControlRowMinHeight: Dp = 42.dp
    val cardHeaderLeadingSlotSize: Dp = 22.dp

    val glassIconButtonSize: Dp = 40.dp
    val compactGlassIconButtonSize: Dp = 36.dp

    val glassTextButtonMinHeight: Dp = 40.dp
    val compactGlassTextButtonMinHeight: Dp = 36.dp
    val glassTextButtonHorizontalPadding: Dp = 14.dp
    val glassTextButtonVerticalPadding: Dp = 10.dp
    val compactGlassTextButtonHorizontalPadding: Dp = 12.dp
    val compactGlassTextButtonVerticalPadding: Dp = 8.dp

    val glassSearchFieldMinHeight: Dp = 44.dp
    val glassSearchFieldHorizontalPadding: Dp = 14.dp
    val glassSearchFieldVerticalPadding: Dp = 10.dp

    val controlContentGap: Dp = 8.dp

    val liquidDropdownMinWidth: Dp = 156.dp
    val liquidDropdownMaxWidth: Dp = 248.dp
    val liquidDropdownMaxHeight: Dp = 312.dp
    val liquidDropdownMaxVisibleItemsForWidth: Int = 8
    val liquidDropdownRowMinHeight: Dp = 34.dp
    val liquidDropdownRowHorizontalPadding: Dp = 10.dp
    val liquidDropdownRowVerticalPadding: Dp = 5.dp
    val liquidDropdownRowGap: Dp = 6.dp
    val liquidDropdownCheckSize: Dp = 18.dp
    val popupAnimationOffset: Dp = 10.dp

    const val pressedScale: Float = 0.985f
    const val pressedOverlayAlphaLight: Float = 0.08f
    const val pressedOverlayAlphaDark: Float = 0.10f
    const val disabledContentAlpha: Float = 0.56f
}

internal fun defaultGlassIconButtonSize(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactGlassIconButtonSize
        else -> AppInteractiveTokens.glassIconButtonSize
    }
}

internal fun defaultGlassTextButtonMinHeight(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactGlassTextButtonMinHeight
        else -> AppInteractiveTokens.glassTextButtonMinHeight
    }
}

internal fun defaultGlassTextButtonHorizontalPadding(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactGlassTextButtonHorizontalPadding
        else -> AppInteractiveTokens.glassTextButtonHorizontalPadding
    }
}

internal fun defaultGlassTextButtonVerticalPadding(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactGlassTextButtonVerticalPadding
        else -> AppInteractiveTokens.glassTextButtonVerticalPadding
    }
}

internal fun appControlPressedOverlayAlpha(isPressed: Boolean, isDark: Boolean): Float {
    if (!isPressed) return 0f
    return if (isDark) {
        AppInteractiveTokens.pressedOverlayAlphaDark
    } else {
        AppInteractiveTokens.pressedOverlayAlphaLight
    }
}

internal fun appControlPressedOverlayColor(isDark: Boolean): Color {
    return if (isDark) {
        Color.White
    } else {
        Color(0xFF3B82F6)
    }
}
