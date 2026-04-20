package com.example.keios.ui.page.main.student.catalog.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.keios.ui.page.main.ba.support.BASettingsStore
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.clearBaGuideCatalogCache
import com.example.keios.ui.page.main.student.catalog.fetchBaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.hydrateBaGuideCatalogReleaseDateIndex
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogBundleComplete
import com.example.keios.ui.page.main.student.catalog.isBaGuideCatalogCacheExpired
import com.example.keios.ui.page.main.student.catalog.loadCachedBaGuideCatalogBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
internal data class BaGuideCatalogDataController(
    val catalog: BaGuideCatalogBundle,
    val loading: Boolean,
    val error: String?,
    val requestRefresh: () -> Unit
)

@Composable
internal fun rememberBaGuideCatalogDataController(
    context: Context,
    transitionAnimationsEnabled: Boolean,
    initialFetchDelayMs: Int,
    loadFailedText: String,
    refreshFailedKeepCacheText: String
): BaGuideCatalogDataController {
    var refreshSignal by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf(BaGuideCatalogBundle.EMPTY) }
    val requestRefresh = remember { { refreshSignal += 1 } }

    val loadFailedTextState = rememberUpdatedState(loadFailedText)
    val refreshFailedKeepCacheTextState = rememberUpdatedState(refreshFailedKeepCacheText)

    LaunchedEffect(refreshSignal) {
        if (refreshSignal == 0 && transitionAnimationsEnabled && initialFetchDelayMs > 0) {
            delay(initialFetchDelayMs.toLong())
        }
        val manualRefresh = refreshSignal > 0
        val now = System.currentTimeMillis()
        loading = true

        val refreshIntervalHours = withContext(Dispatchers.IO) {
            BASettingsStore.loadCalendarRefreshIntervalHours()
        }
        val cachedBundle = withContext(Dispatchers.IO) { loadCachedBaGuideCatalogBundle() }
        val cacheComplete = isBaGuideCatalogBundleComplete(cachedBundle)
        val cacheExpired = isBaGuideCatalogCacheExpired(
            bundle = cachedBundle,
            refreshIntervalHours = refreshIntervalHours,
            nowMs = now
        )

        if (!manualRefresh && cacheComplete && !cacheExpired) {
            catalog = cachedBundle!!
            error = null
            loading = false
            return@LaunchedEffect
        }

        val shouldClearLocalCache = manualRefresh || (cachedBundle != null && (cacheExpired || !cacheComplete))
        if (shouldClearLocalCache) {
            withContext(Dispatchers.IO) { clearBaGuideCatalogCache(context) }
        }

        val result = withContext(Dispatchers.IO) {
            runCatching { fetchBaGuideCatalogBundle(forceRefresh = true) }
        }
        result.onSuccess { latest ->
            catalog = latest
            error = null
        }.onFailure {
            error = if (catalog.entriesByTab.values.all { it.isEmpty() }) {
                loadFailedTextState.value
            } else {
                refreshFailedKeepCacheTextState.value
            }
        }
        loading = false
    }

    LaunchedEffect(catalog.syncedAtMs, loading) {
        if (loading) return@LaunchedEffect
        if (catalog.entriesByTab.values.all { it.isEmpty() }) return@LaunchedEffect
        hydrateBaGuideCatalogReleaseDateIndex(
            source = catalog,
            maxNetworkFetchPerPass = CATALOG_RELEASE_DATE_FETCH_LIMIT_PER_PASS,
            onBundleUpdated = { updated ->
                catalog = updated
            }
        )
    }

    return remember(catalog, loading, error, requestRefresh) {
        BaGuideCatalogDataController(
            catalog = catalog,
            loading = loading,
            error = error,
            requestRefresh = requestRefresh
        )
    }
}
