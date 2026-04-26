package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiPerformanceBudget {
    val maxGlassBlur: Dp = 11.dp
    val backdropBlur: Dp = 8.dp
    val backdropLens: Dp = 24.dp
    const val mainPagerBeyondViewportPageCount: Int = 0
    const val mainPagerActiveScrollBeyondViewportPageCount: Int = 0
    const val catalogPagerBeyondViewportPageCount: Int = 0
    const val guidePagerBeyondViewportPageCount: Int = 0
    const val guideStaticPrefetchInitialCount: Int = 5
    const val guideStaticPrefetchGalleryExtraCount: Int = 10
    const val mediaCacheParallelDownloads: Int = 3
    const val baCalendarPoolPriorityPrefetchCount: Int = 4
    const val baCalendarPoolDeferredWarmDelayMs: Long = 220L
    const val listScrollGlassReductionScale: Float = 0.90f

    data class PreloadPolicy(
        val mainPagerBeyondViewportPageCount: Int,
        val mainPagerActiveScrollBeyondViewportPageCount: Int,
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
                mainPagerActiveScrollBeyondViewportPageCount = mainPagerActiveScrollBeyondViewportPageCount,
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
            mainPagerActiveScrollBeyondViewportPageCount = 1,
            catalogPagerBeyondViewportPageCount = 1,
            guidePagerBeyondViewportPageCount = 1,
            guideStaticPrefetchInitialCount = 10,
            guideStaticPrefetchGalleryExtraCount = 16,
            includeTargetPageInHeavyRender = true,
            initialFetchDelayMs = 0
        )
    }
}

internal fun Dp.clampGlassBlur(): Dp = coerceAtMost(UiPerformanceBudget.maxGlassBlur)
