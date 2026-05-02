package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppInteractiveTokens {
    val controlRowMinHeight: Dp = 50.dp
    val compactControlRowMinHeight: Dp = 42.dp
    val cardHeaderLeadingSlotSize: Dp = 22.dp

    val appLiquidIconButtonSize: Dp = 40.dp
    val compactAppLiquidIconButtonSize: Dp = 36.dp

    val appLiquidTextButtonMinHeight: Dp = 40.dp
    val compactAppLiquidTextButtonMinHeight: Dp = 36.dp
    val appLiquidTextButtonHorizontalPadding: Dp = 14.dp
    val appLiquidTextButtonVerticalPadding: Dp = 10.dp
    val compactAppLiquidTextButtonHorizontalPadding: Dp = 12.dp
    val compactAppLiquidTextButtonVerticalPadding: Dp = 8.dp

    val appLiquidSearchFieldMinHeight: Dp = 44.dp
    val appLiquidSearchFieldHorizontalPadding: Dp = 14.dp
    val appLiquidSearchFieldVerticalPadding: Dp = 10.dp

    val controlContentGap: Dp = 8.dp

    val popupAnimationOffset: Dp = 10.dp

    const val pressedScale: Float = 0.985f
    const val pressedOverlayAlphaLight: Float = 0.08f
    const val pressedOverlayAlphaDark: Float = 0.10f
    const val disabledContentAlpha: Float = 0.56f
}

internal fun defaultAppLiquidIconButtonSize(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactAppLiquidIconButtonSize
        else -> AppInteractiveTokens.appLiquidIconButtonSize
    }
}

internal fun defaultAppLiquidTextButtonMinHeight(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactAppLiquidTextButtonMinHeight
        else -> AppInteractiveTokens.appLiquidTextButtonMinHeight
    }
}

internal fun defaultAppLiquidTextButtonHorizontalPadding(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactAppLiquidTextButtonHorizontalPadding
        else -> AppInteractiveTokens.appLiquidTextButtonHorizontalPadding
    }
}

internal fun defaultAppLiquidTextButtonVerticalPadding(variant: GlassVariant): Dp {
    return when (variant) {
        GlassVariant.Compact -> AppInteractiveTokens.compactAppLiquidTextButtonVerticalPadding
        else -> AppInteractiveTokens.appLiquidTextButtonVerticalPadding
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

internal fun appControlPressedOverlayColor(
    isDark: Boolean,
    variant: GlassVariant = GlassVariant.SheetAction,
    accentColor: Color = Color.Unspecified
): Color {
    val variantAccent = when (variant) {
        GlassVariant.SheetDangerAction -> Color(0xFFE25B6A)
        GlassVariant.SheetPrimaryAction -> Color(0xFF3B82F6)
        else -> Color.Unspecified
    }
    return when {
        accentColor.isPreferredPressedOverlayAccent() -> accentColor.toPressedOverlayTint()
        variantAccent.isPreferredPressedOverlayAccent() -> variantAccent.toPressedOverlayTint()
        isDark -> Color.White
        else -> Color(0xFF3B82F6)
    }
}

private fun Color.isPreferredPressedOverlayAccent(): Boolean {
    if (!isSpecified || alpha <= 0.01f) return false
    val maxChannel = maxOf(red, green, blue)
    val minChannel = minOf(red, green, blue)
    return (maxChannel - minChannel) >= 0.08f
}

private fun Color.toPressedOverlayTint(): Color {
    return copy(alpha = 1f)
}
