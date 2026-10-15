package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppChromeTokens {
    val pageHorizontalPadding: Dp = 14.dp
    val pageSectionGap: Dp = 12.dp
    val pageSectionGapLarge: Dp = 14.dp
    val topBarToHeaderGap: Dp = 12.dp
    val pageBottomInsetExtra: Dp = 16.dp
    val pageFloatingOverlayBottomExtra: Dp = 64.dp

    val topBarExpandedHeight: Dp = 128.dp
    val topBarCollapsedHeight: Dp = 64.dp
    val topBarChromeTopPadding: Dp = 6.dp
    val topBarHorizontalPadding: Dp = 14.dp
    val topBarTitleEdgePadding: Dp = 14.dp
    val topBarTitleNavigationReserve: Dp = 76.dp
    val topBarTitleActionReserve: Dp = 178.dp
    val topBarTitleMinWidth: Dp = 86.dp
    val topBarTitleMaxWidth: Dp = 280.dp
    val topBarTitleHeight: Dp = 52.dp

    // The fixed bar widths and item step that used to live here belonged to the old bar's
    // weight-based slot maths. The toolbar sizes itself from its actions.
    val liquidActionBarMinimumTouchTarget: Dp = 48.dp
    val liquidActionBarOuterHeight: Dp = 52.dp
    val liquidActionBarInnerHeight: Dp = 44.dp
    val liquidActionBarHorizontalPadding: Dp = 4.dp

    /**
     * Fixed space between toolbar groups. Apple asks for separation so adjacent groups do not read
     * as one control, and warns that a symbol placed next to another can look like a single combined
     * action.
     */
    val liquidToolbarGroupSpacing: Dp = 8.dp

    val floatingBottomBarOuterHeight: Dp = 62.dp
    val floatingBottomBarInnerHeight: Dp = 54.dp
    val floatingBottomBarHorizontalPadding: Dp = 4.dp
}
