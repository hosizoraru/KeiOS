package os.kei.ui.page.main.student.page.state

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.fetchGuideInfo

internal data class BaStudentGuideLoadResult(
    val info: BaStudentGuideInfo?,
    val error: String?
)

internal class BaStudentGuideRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun loadCurrentUrl(): String = BaStudentGuideStore.loadCurrentUrl()

    fun saveCurrentUrl(sourceUrl: String) {
        BaStudentGuideStore.setCurrentUrl(sourceUrl)
    }

    suspend fun loadGuide(
        context: Context,
        sourceUrl: String,
        currentInfo: BaStudentGuideInfo?,
        manualRefresh: Boolean,
        loadFailedText: String,
        refreshFailedKeepCacheText: String
    ): BaStudentGuideLoadResult {
        val requestUrl = sourceUrl.trim()
        if (requestUrl.isBlank()) {
            return BaStudentGuideLoadResult(info = null, error = null)
        }

        val now = System.currentTimeMillis()
        val refreshIntervalHours = withContext(ioDispatcher) {
            BASettingsStore.loadCalendarRefreshIntervalHours()
        }
        val cacheSnapshot = withContext(ioDispatcher) {
            BaStudentGuideStore.loadInfoSnapshot(requestUrl)
        }
        val cacheExpired = BaStudentGuideStore.isCacheExpired(
            snapshot = cacheSnapshot,
            refreshIntervalHours = refreshIntervalHours,
            nowMs = now
        )
        val cacheInfo = cacheSnapshot.info.takeIf { cacheSnapshot.isComplete }
        if (!manualRefresh && cacheInfo != null && !cacheExpired) {
            return BaStudentGuideLoadResult(info = cacheInfo, error = null)
        }

        val visibleInfo = when {
            cacheInfo != null -> cacheInfo
            cacheSnapshot.hasCache -> null
            currentInfo?.sourceUrl == requestUrl -> currentInfo
            else -> null
        }
        val shouldClearLocalCache =
            manualRefresh || (cacheSnapshot.hasCache && (cacheExpired || !cacheSnapshot.isComplete))
        if (shouldClearLocalCache) {
            withContext(ioDispatcher) {
                BaStudentGuideStore.clearCachedInfo(requestUrl)
                BaGuideTempMediaCache.clearGuideCache(context, requestUrl)
            }
        }

        val result = withContext(ioDispatcher) {
            runCatching { fetchGuideInfo(requestUrl) }
        }
        return result.fold(
            onSuccess = { latest ->
                withContext(ioDispatcher) { BaStudentGuideStore.saveInfo(latest) }
                BaStudentGuideLoadResult(info = latest, error = null)
            },
            onFailure = {
                BaStudentGuideLoadResult(
                    info = visibleInfo,
                    error = if (visibleInfo != null) refreshFailedKeepCacheText else loadFailedText
                )
            }
        )
    }
}
