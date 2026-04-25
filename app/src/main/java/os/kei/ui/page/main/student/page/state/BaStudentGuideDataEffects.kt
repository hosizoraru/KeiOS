package os.kei.ui.page.main.student.page.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.page.support.collectGuideStaticImagePrefetchUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BindBaStudentGuidePrefetchEffects(
    context: Context,
    sourceUrl: String,
    guideSyncToken: Long,
    info: BaStudentGuideInfo?,
    activeBottomTab: GuideBottomTab,
    galleryPrefetchRequested: Boolean,
    onGalleryPrefetchRequestedChange: (Boolean) -> Unit,
    staticImagePrefetchStage: Int,
    onStaticImagePrefetchStageChange: (Int) -> Unit,
    initialPrefetchCount: Int,
    galleryExtraPrefetchCount: Int,
    onGalleryCacheRevisionIncrease: () -> Unit
) {
    LaunchedEffect(activeBottomTab) {
        if (activeBottomTab == GuideBottomTab.Gallery) {
            onGalleryPrefetchRequestedChange(true)
        }
    }

    LaunchedEffect(sourceUrl, guideSyncToken) {
        if (staticImagePrefetchStage >= 1) return@LaunchedEffect
        val guide = info ?: return@LaunchedEffect
        onStaticImagePrefetchStageChange(1)

        val urls = collectGuideStaticImagePrefetchUrls(guide)
            .take(initialPrefetchCount)
        if (urls.isEmpty()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            BaGuideTempMediaCache.prefetchForGuide(
                context = context,
                sourceUrl = sourceUrl,
                rawUrls = urls
            )
        }
        onGalleryCacheRevisionIncrease()
    }

    LaunchedEffect(sourceUrl, galleryPrefetchRequested, guideSyncToken) {
        if (!galleryPrefetchRequested) return@LaunchedEffect
        if (staticImagePrefetchStage >= 2) return@LaunchedEffect
        val guide = info ?: return@LaunchedEffect
        onStaticImagePrefetchStageChange(2)

        val urls = collectGuideStaticImagePrefetchUrls(guide)
            .drop(initialPrefetchCount)
            .take(galleryExtraPrefetchCount)
        if (urls.isEmpty()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            BaGuideTempMediaCache.prefetchForGuide(
                context = context,
                sourceUrl = sourceUrl,
                rawUrls = urls
            )
        }
        onGalleryCacheRevisionIncrease()
    }
}
