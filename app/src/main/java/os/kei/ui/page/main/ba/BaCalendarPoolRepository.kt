package os.kei.ui.page.main.ba

import android.content.Context
import os.kei.R
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BA_CALENDAR_CACHE_SCHEMA_VERSION
import os.kei.ui.page.main.ba.support.BA_POOL_CACHE_SCHEMA_VERSION
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaPoolEntry
import os.kei.ui.page.main.ba.support.decodeBaCalendarEntries
import os.kei.ui.page.main.ba.support.decodeBaPoolEntries
import os.kei.ui.page.main.ba.support.encodeBaCalendarEntries
import os.kei.ui.page.main.ba.support.encodeBaPoolEntries
import os.kei.ui.page.main.ba.support.fetchBaCalendarEntries
import os.kei.ui.page.main.ba.support.fetchBaPoolEntries
import os.kei.ui.page.main.ba.support.isNetworkAvailable
import os.kei.ui.page.main.ba.support.runWithHardTimeout
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class BaCalendarSyncSnapshot(
    val entries: List<BaCalendarEntry>,
    val loading: Boolean,
    val error: String?,
    val lastSyncMs: Long
)

internal data class BaPoolSyncSnapshot(
    val entries: List<BaPoolEntry>,
    val loading: Boolean,
    val error: String?,
    val lastSyncMs: Long
)

internal object BaCalendarPoolRepository {
    suspend fun syncCalendar(
        context: Context,
        isPageActive: Boolean,
        serverIndex: Int,
        reloadSignal: Int,
        calendarRefreshIntervalHours: Int,
        hydrationReady: Boolean,
    ): BaCalendarSyncSnapshot {
        if (!hydrationReady) {
            return BaCalendarSyncSnapshot(
                entries = emptyList(),
                loading = true,
                error = null,
                lastSyncMs = 0L
            )
        }
        val now = System.currentTimeMillis()
        val networkAvailable = isNetworkAvailable(context)
        val cacheSnapshot = withContext(Dispatchers.IO) {
            BASettingsStore.loadCalendarCacheSnapshot(serverIndex)
        }
        val hasCache = cacheSnapshot.raw.isNotBlank()
        val cachedEntries = if (hasCache) {
            runCatching { decodeBaCalendarEntries(cacheSnapshot.raw, now) }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val cachedEntriesWithLocalImages = if (cachedEntries.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                BaCalendarPoolImageCache.applyCachedCalendarImageUrls(
                    context = context,
                    serverIndex = serverIndex,
                    entries = cachedEntries,
                    localOnly = !networkAvailable
                )
            }
        } else {
            emptyList()
        }
        val intervalMs = calendarRefreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val cacheExpired = !hasCache ||
            cacheSnapshot.syncMs <= 0L ||
            (now - cacheSnapshot.syncMs).coerceAtLeast(0L) >= intervalMs
        val cacheSchemaExpired = cacheSnapshot.version < BA_CALENDAR_CACHE_SCHEMA_VERSION
        val forceRefresh = reloadSignal > 0
        val shouldRequestNetwork = forceRefresh || cacheExpired || cacheSchemaExpired

        if (!shouldRequestNetwork) {
            return BaCalendarSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        if (!isPageActive && hasCache) {
            return BaCalendarSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        if (!networkAvailable) {
            return BaCalendarSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = if (hasCache) {
                    context.getString(R.string.ba_calendar_pool_error_offline_cached)
                } else {
                    context.getString(R.string.ba_calendar_pool_error_offline_no_cache)
                },
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        val result = withContext(Dispatchers.IO) {
            runCatching {
                runWithHardTimeout(15_000L) {
                    fetchBaCalendarEntries(serverIndex, now)
                }
            }
        }
        if (result.isSuccess) {
            val entries = result.getOrThrow()
            if (entries.isNotEmpty()) {
                val entriesWithLocalImages = withContext(Dispatchers.IO) {
                    BASettingsStore.saveCalendarCache(
                        serverIndex,
                        encodeBaCalendarEntries(entries),
                        now
                    )
                    BaCalendarPoolImageCache.prefetchForCalendar(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries.take(UiPerformanceBudget.baCalendarPoolPriorityPrefetchCount)
                    )
                    BaCalendarPoolImageCache.applyCachedCalendarImageUrls(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries,
                        localOnly = false
                    )
                }
                BaCalendarPoolImageCache.scheduleCalendarWarm(
                    context = context,
                    serverIndex = serverIndex,
                    entries = entries
                )
                return BaCalendarSyncSnapshot(
                    entries = entriesWithLocalImages,
                    loading = false,
                    error = null,
                    lastSyncMs = now
                )
            }
            return BaCalendarSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = if (hasCache) context.getString(R.string.ba_calendar_pool_error_empty_keep_cached) else null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        return BaCalendarSyncSnapshot(
            entries = cachedEntriesWithLocalImages,
            loading = false,
            error = if (hasCache) {
                context.getString(R.string.ba_calendar_pool_error_sync_failed_cached)
            } else {
                context.getString(R.string.ba_calendar_error_sync_failed)
            },
            lastSyncMs = cacheSnapshot.syncMs
        )
    }

    suspend fun syncPool(
        context: Context,
        isPageActive: Boolean,
        serverIndex: Int,
        reloadSignal: Int,
        calendarRefreshIntervalHours: Int,
        hydrationReady: Boolean,
    ): BaPoolSyncSnapshot {
        if (!hydrationReady) {
            return BaPoolSyncSnapshot(
                entries = emptyList(),
                loading = true,
                error = null,
                lastSyncMs = 0L
            )
        }
        val now = System.currentTimeMillis()
        val networkAvailable = isNetworkAvailable(context)
        val cacheSnapshot = withContext(Dispatchers.IO) {
            BASettingsStore.loadPoolCacheSnapshot(serverIndex)
        }
        val hasCache = cacheSnapshot.raw.isNotBlank()
        val cachedEntries = if (hasCache) {
            runCatching { decodeBaPoolEntries(cacheSnapshot.raw, now) }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val cachedEntriesWithLocalImages = if (cachedEntries.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                BaCalendarPoolImageCache.applyCachedPoolImageUrls(
                    context = context,
                    serverIndex = serverIndex,
                    entries = cachedEntries,
                    localOnly = !networkAvailable
                )
            }
        } else {
            emptyList()
        }
        val intervalMs = calendarRefreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val cacheExpired = !hasCache ||
            cacheSnapshot.syncMs <= 0L ||
            (now - cacheSnapshot.syncMs).coerceAtLeast(0L) >= intervalMs
        val cacheSchemaExpired = cacheSnapshot.version < BA_POOL_CACHE_SCHEMA_VERSION
        val forceRefresh = reloadSignal > 0
        val shouldRequestNetwork = forceRefresh || cacheExpired || cacheSchemaExpired

        if (!shouldRequestNetwork) {
            return BaPoolSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        if (!isPageActive && hasCache) {
            return BaPoolSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        if (!networkAvailable) {
            return BaPoolSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = if (hasCache) {
                    context.getString(R.string.ba_calendar_pool_error_offline_cached)
                } else {
                    context.getString(R.string.ba_calendar_pool_error_offline_no_cache)
                },
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        val result = withContext(Dispatchers.IO) {
            runCatching {
                runWithHardTimeout(15_000L) {
                    fetchBaPoolEntries(serverIndex, now)
                }
            }
        }
        if (result.isSuccess) {
            val entries = result.getOrThrow()
            if (entries.isNotEmpty()) {
                val entriesWithLocalImages = withContext(Dispatchers.IO) {
                    BASettingsStore.savePoolCache(
                        serverIndex,
                        encodeBaPoolEntries(entries),
                        now
                    )
                    BaCalendarPoolImageCache.prefetchForPool(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries.take(UiPerformanceBudget.baCalendarPoolPriorityPrefetchCount)
                    )
                    BaCalendarPoolImageCache.applyCachedPoolImageUrls(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries,
                        localOnly = false
                    )
                }
                BaCalendarPoolImageCache.schedulePoolWarm(
                    context = context,
                    serverIndex = serverIndex,
                    entries = entries
                )
                return BaPoolSyncSnapshot(
                    entries = entriesWithLocalImages,
                    loading = false,
                    error = null,
                    lastSyncMs = now
                )
            }
            return BaPoolSyncSnapshot(
                entries = cachedEntriesWithLocalImages,
                loading = false,
                error = if (hasCache) context.getString(R.string.ba_calendar_pool_error_empty_keep_cached) else null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        return BaPoolSyncSnapshot(
            entries = cachedEntriesWithLocalImages,
            loading = false,
            error = if (hasCache) {
                context.getString(R.string.ba_calendar_pool_error_sync_failed_cached)
            } else {
                context.getString(R.string.ba_pool_error_sync_failed)
            },
            lastSyncMs = cacheSnapshot.syncMs
        )
    }
}
