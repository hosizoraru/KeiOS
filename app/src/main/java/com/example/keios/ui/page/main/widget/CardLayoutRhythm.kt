package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared spacing rhythm for data-oriented cards and rows.
 * Keeps section paddings and line rhythm aligned across main pages.
 */
object CardLayoutRhythm {
    val cardCornerRadius: Dp = 16.dp
    val cardHorizontalPadding: Dp = 14.dp
    val cardVerticalPadding: Dp = 12.dp
    val cardContentPadding: PaddingValues =
        PaddingValues(horizontal = cardHorizontalPadding, vertical = cardVerticalPadding)

    val sectionGap: Dp = 10.dp
    val denseSectionGap: Dp = 8.dp
    val compactSectionGap: Dp = 6.dp

    val infoRowGap: Dp = 10.dp
    val infoRowVerticalPadding: Dp = 4.dp

    val controlRowGap: Dp = 12.dp
    val controlRowTextGap: Dp = 5.dp
    val controlRowVerticalPadding: Dp = 3.dp

    val metricRowGap: Dp = 14.dp
    val metricCardHorizontalPadding: Dp = 10.dp
    val metricCardVerticalPadding: Dp = 9.dp
    val metricCardTextGap: Dp = 2.dp
}
