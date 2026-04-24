package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiPerformanceBudget {
    val maxGlassBlur: Dp = 11.dp
    val backdropBlur: Dp = 8.dp
    val backdropLens: Dp = 24.dp
    const val mainPagerBeyondViewportPageCount: Int = 1
    const val mainPagerActiveScrollBeyondViewportPageCount: Int = 0
    const val catalogPagerBeyondViewportPageCount: Int = 0
    const val guidePagerBeyondViewportPageCount: Int = 0
    const val guideStaticPrefetchInitialCount: Int = 5
    const val guideStaticPrefetchGalleryExtraCount: Int = 10
    const val mediaCacheParallelDownloads: Int = 3
    const val baCalendarPoolPriorityPrefetchCount: Int = 4
    const val baCalendarPoolDeferredWarmDelayMs: Long = 220L

    data class PreloadPolicy(
        val mainPagerBeyondViewportPageCount: Int,
        val catalogPagerBeyondViewportPageCount: Int,
        val guidePagerBeyondViewportPageCount: Int,
        val guideStaticPrefetchInitialCount: Int,
        val guideStaticPrefetchGalleryExtraCount: Int,
        val includeTargetPageInHeavyRender: Boolean,
        val initialFetchDelayMs: Int,
    )

    fun resolvePreloadPolicy(
        preloadingEnabled: Boolean
    ): PreloadPolicy {
        if (!preloadingEnabled) {
            return PreloadPolicy(
                mainPagerBeyondViewportPageCount = mainPagerBeyondViewportPageCount,
                catalogPagerBeyondViewportPageCount = catalogPagerBeyondViewportPageCount,
                guidePagerBeyondViewportPageCount = guidePagerBeyondViewportPageCount,
                guideStaticPrefetchInitialCount = guideStaticPrefetchInitialCount,
                guideStaticPrefetchGalleryExtraCount = guideStaticPrefetchGalleryExtraCount,
                includeTargetPageInHeavyRender = false,
                initialFetchDelayMs = 90
            )
        }
        return PreloadPolicy(
            mainPagerBeyondViewportPageCount = 1,
            catalogPagerBeyondViewportPageCount = 0,
            guidePagerBeyondViewportPageCount = 0,
            guideStaticPrefetchInitialCount = 8,
            guideStaticPrefetchGalleryExtraCount = 12,
            includeTargetPageInHeavyRender = false,
            initialFetchDelayMs = 40
        )
    }
}

internal fun Dp.clampGlassBlur(): Dp = coerceAtMost(UiPerformanceBudget.maxGlassBlur)
