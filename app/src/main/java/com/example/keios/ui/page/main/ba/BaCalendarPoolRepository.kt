package com.example.keios.ui.page.main.ba

import android.content.Context
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
                    localOnly = true
                )
            }
        } else {
            emptyList()
        }
        val networkAvailable = isNetworkAvailable(context)
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
                    "当前离线，已显示本地缓存"
                } else {
                    "当前离线且无缓存，请联网后刷新"
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
                    BASettingsStore.saveCalendarCache(serverIndex, encodeBaCalendarEntries(entries), now)
                    BaCalendarPoolImageCache.prefetchForCalendar(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries
                    )
                    BaCalendarPoolImageCache.pruneCalendarStale(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries
                    )
                    BaCalendarPoolImageCache.applyCachedCalendarImageUrls(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries,
                        localOnly = true
                    )
                }
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
                error = if (hasCache) "本次返回空数据，已保留本地缓存" else null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        return BaCalendarSyncSnapshot(
            entries = cachedEntriesWithLocalImages,
            loading = false,
            error = if (hasCache) {
                "同步超时或网络失败，已显示本地缓存"
            } else {
                "活动日历同步失败（超时或网络异常）"
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
                    localOnly = true
                )
            }
        } else {
            emptyList()
        }
        val networkAvailable = isNetworkAvailable(context)
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
                    "当前离线，已显示本地缓存"
                } else {
                    "当前离线且无缓存，请联网后刷新"
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
                    BASettingsStore.savePoolCache(serverIndex, encodeBaPoolEntries(entries), now)
                    BaCalendarPoolImageCache.prefetchForPool(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries
                    )
                    BaCalendarPoolImageCache.prunePoolStale(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries
                    )
                    BaCalendarPoolImageCache.applyCachedPoolImageUrls(
                        context = context,
                        serverIndex = serverIndex,
                        entries = entries,
                        localOnly = true
                    )
                }
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
                error = if (hasCache) "本次返回空数据，已保留本地缓存" else null,
                lastSyncMs = cacheSnapshot.syncMs
            )
        }

        return BaPoolSyncSnapshot(
            entries = cachedEntriesWithLocalImages,
            loading = false,
            error = if (hasCache) {
                "同步超时或网络失败，已显示本地缓存"
            } else {
                "卡池同步失败（超时或网络异常）"
            },
            lastSyncMs = cacheSnapshot.syncMs
        )
    }
}
