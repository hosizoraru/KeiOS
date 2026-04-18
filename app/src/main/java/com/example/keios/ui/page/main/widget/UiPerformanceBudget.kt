package com.example.keios.ui.page.main.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiPerformanceBudget {
    val maxGlassBlur: Dp = 11.dp
    val backdropBlur: Dp = 8.dp
    val backdropLens: Dp = 24.dp
    const val mainPagerBeyondViewportPageCount: Int = 0
    const val catalogPagerBeyondViewportPageCount: Int = 1
    const val guidePagerBeyondViewportPageCount: Int = 1
    const val guideStaticPrefetchInitialCount: Int = 5
    const val guideStaticPrefetchGalleryExtraCount: Int = 10
    const val mediaCacheParallelDownloads: Int = 3
}

internal fun Dp.clampGlassBlur(): Dp = coerceAtMost(UiPerformanceBudget.maxGlassBlur)
