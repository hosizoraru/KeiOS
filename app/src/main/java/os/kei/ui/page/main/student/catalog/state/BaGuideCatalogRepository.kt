package os.kei.ui.page.main.student.catalog.state

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.clearBaGuideCatalogCache
import os.kei.ui.page.main.student.catalog.fetchBaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.hydrateBaGuideCatalogReleaseDateIndex
import os.kei.ui.page.main.student.catalog.isBaGuideCatalogBundleComplete
import os.kei.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import os.kei.ui.page.main.student.catalog.loadCachedBaGuideCatalogBundle

internal data class BaGuideCatalogLoadResult(
    val catalog: BaGuideCatalogBundle,
    val error: String?
)

internal class BaGuideCatalogRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun loadCatalog(
        context: Context,
        currentCatalog: BaGuideCatalogBundle,
        manualRefresh: Boolean,
        loadFailedText: String,
        refreshFailedKeepCacheText: String
    ): BaGuideCatalogLoadResult {
        val now = System.currentTimeMillis()
        val refreshIntervalHours = withContext(ioDispatcher) {
            BASettingsStore.loadCalendarRefreshIntervalHours()
        }
        val cachedBundle = withContext(ioDispatcher) {
            loadCachedBaGuideCatalogBundle()
        }
        val cacheComplete = isBaGuideCatalogBundleComplete(cachedBundle)
        val cacheExpired = isBaGuideCatalogCacheExpired(
            bundle = cachedBundle,
            refreshIntervalHours = refreshIntervalHours,
            nowMs = now
        )

        if (!manualRefresh && cacheComplete && !cacheExpired) {
            return BaGuideCatalogLoadResult(
                catalog = cachedBundle ?: BaGuideCatalogBundle.EMPTY,
                error = null
            )
        }

        val shouldClearLocalCache = manualRefresh || (cachedBundle != null && (cacheExpired || !cacheComplete))
        if (shouldClearLocalCache) {
            withContext(ioDispatcher) {
                clearBaGuideCatalogCache(context)
            }
        }

        val result = withContext(ioDispatcher) {
            runCatching { fetchBaGuideCatalogBundle(forceRefresh = true) }
        }
        return result.fold(
            onSuccess = { latest ->
                BaGuideCatalogLoadResult(
                    catalog = latest,
                    error = null
                )
            },
            onFailure = {
                val fallback = when {
                    currentCatalog.entriesByTab.values.any { it.isNotEmpty() } -> currentCatalog
                    cacheComplete && cachedBundle != null -> cachedBundle
                    else -> BaGuideCatalogBundle.EMPTY
                }
                BaGuideCatalogLoadResult(
                    catalog = fallback,
                    error = if (fallback.entriesByTab.values.all { it.isEmpty() }) {
                        loadFailedText
                    } else {
                        refreshFailedKeepCacheText
                    }
                )
            }
        )
    }

    suspend fun hydrateReleaseDateIndex(
        source: BaGuideCatalogBundle,
        onBundleUpdated: (BaGuideCatalogBundle) -> Unit
    ): BaGuideCatalogBundle {
        return hydrateBaGuideCatalogReleaseDateIndex(
            source = source,
            maxNetworkFetchPerPass = CATALOG_RELEASE_DATE_FETCH_LIMIT_PER_PASS,
            onBundleUpdated = onBundleUpdated
        )
    }
}
