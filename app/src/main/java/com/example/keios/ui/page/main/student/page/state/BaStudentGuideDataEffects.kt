package com.example.keios.ui.page.main.student.page.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.student.BaGuideTempMediaCache
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.GuideBottomTab
import com.example.keios.ui.page.main.student.fetchGuideInfo
import com.example.keios.ui.page.main.student.page.support.collectGuideStaticImagePrefetchUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

@Composable
internal fun BindBaStudentGuideInfoLoadEffect(
    context: Context,
    sourceUrl: String,
    refreshSignal: Int,
    transitionAnimationsEnabled: Boolean,
    initialFetchDelayMs: Int,
    manualRefreshRequested: Boolean,
    onManualRefreshRequestedChange: (Boolean) -> Unit,
    currentSourceUrlProvider: () -> String,
    currentInfoProvider: () -> BaStudentGuideInfo?,
    onInfoChange: (BaStudentGuideInfo?) -> Unit,
    onErrorChange: (String?) -> Unit,
    onLoadingChange: (Boolean) -> Unit
) {
    LaunchedEffect(sourceUrl, refreshSignal) {
        if (refreshSignal == 0 && transitionAnimationsEnabled && initialFetchDelayMs > 0) {
            delay(initialFetchDelayMs.toLong())
        }
        val requestUrl = sourceUrl
        if (requestUrl.isBlank()) return@LaunchedEffect
        val manualRefresh = manualRefreshRequested
        onManualRefreshRequestedChange(false)
        onLoadingChange(true)

        val now = System.currentTimeMillis()
        val refreshIntervalHours = withContext(Dispatchers.IO) {
            BASettingsStore.loadCalendarRefreshIntervalHours()
        }
        val cacheSnapshot = withContext(Dispatchers.IO) {
            BaStudentGuideStore.loadInfoSnapshot(requestUrl)
        }
        if (requestUrl != currentSourceUrlProvider()) return@LaunchedEffect

        val cacheExpired = BaStudentGuideStore.isCacheExpired(
            snapshot = cacheSnapshot,
            refreshIntervalHours = refreshIntervalHours,
            nowMs = now
        )
        val cacheComplete = cacheSnapshot.isComplete && cacheSnapshot.info != null
        if (!manualRefresh && cacheComplete && !cacheExpired) {
            onInfoChange(cacheSnapshot.info)
            onErrorChange(null)
            onLoadingChange(false)
            return@LaunchedEffect
        }

        if (cacheComplete) {
            onInfoChange(cacheSnapshot.info)
            onErrorChange(null)
        } else if (cacheSnapshot.hasCache && !cacheSnapshot.isComplete) {
            onInfoChange(null)
        }

        val shouldClearLocalCache =
            manualRefresh || (cacheSnapshot.hasCache && (cacheExpired || !cacheSnapshot.isComplete))
        if (shouldClearLocalCache) {
            withContext(Dispatchers.IO) {
                BaStudentGuideStore.clearCachedInfo(requestUrl)
                BaGuideTempMediaCache.clearGuideCache(context, requestUrl)
            }
            if (requestUrl != currentSourceUrlProvider()) return@LaunchedEffect
        }

        val result = withContext(Dispatchers.IO) {
            runCatching { fetchGuideInfo(requestUrl) }
        }
        if (requestUrl != currentSourceUrlProvider()) return@LaunchedEffect
        result.onSuccess { latest ->
            if (requestUrl != currentSourceUrlProvider()) return@onSuccess
            onInfoChange(latest)
            onErrorChange(null)
            withContext(Dispatchers.IO) { BaStudentGuideStore.saveInfo(latest) }
        }.onFailure {
            if (requestUrl != currentSourceUrlProvider()) return@onFailure
            val hasInfo = currentInfoProvider() != null
            onErrorChange(if (hasInfo) "网络请求失败，已显示本地缓存" else "图鉴信息加载失败")
        }
        if (requestUrl != currentSourceUrlProvider()) return@LaunchedEffect
        onLoadingChange(false)
    }
}
