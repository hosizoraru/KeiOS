package com.example.keios.ui.page.main.ba

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.keios.ui.page.main.BASettingsStore
import com.example.keios.ui.page.main.BA_AP_REGEN_TICK_MS
import com.example.keios.ui.page.main.BA_CALENDAR_CACHE_SCHEMA_VERSION
import com.example.keios.ui.page.main.BA_POOL_CACHE_SCHEMA_VERSION
import com.example.keios.ui.page.main.BaCalendarEntry
import com.example.keios.ui.page.main.BaPoolEntry
import com.example.keios.ui.page.main.decodeBaCalendarEntries
import com.example.keios.ui.page.main.decodeBaPoolEntries
import com.example.keios.ui.page.main.encodeBaCalendarEntries
import com.example.keios.ui.page.main.encodeBaPoolEntries
import com.example.keios.ui.page.main.fetchBaCalendarEntries
import com.example.keios.ui.page.main.fetchBaPoolEntries
import com.example.keios.ui.page.main.isNetworkAvailable
import com.example.keios.ui.page.main.runWithHardTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun BaPageCommonEffects(
    listState: LazyListState,
    scrollToTopSignal: Int,
    consumedScrollToTopSignal: Int,
    onConsumedScrollToTopSignalChange: (Int) -> Unit,
    onDisposeActionBarInteraction: () -> Unit,
    office: BaOfficeController,
    onUiNowMsChange: (Long) -> Unit,
    serverIndex: Int,
    onServerChanged: suspend () -> Unit,
    context: Context,
) {
    DisposableEffect(Unit) {
        onDispose { onDisposeActionBarInteraction() }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > consumedScrollToTopSignal) {
            onConsumedScrollToTopSignalChange(scrollToTopSignal)
            listState.animateScrollToItem(0)
        } else {
            onConsumedScrollToTopSignalChange(scrollToTopSignal)
        }
    }

    LaunchedEffect(Unit) {
        office.ensureRegenBase()
        office.ensureCafeHourBase()
        office.clampCafeStoredToCap()
        office.applyCafeStorage()
        office.applyApRegen()
        while (true) {
            delay(BA_AP_REGEN_TICK_MS)
            office.applyCafeStorage()
            office.applyApRegen()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            onUiNowMsChange(System.currentTimeMillis())
        }
    }

    LaunchedEffect(office.apCurrent) {
        val target = office.displayApInputText()
        if (office.apCurrentInput != target) office.apCurrentInput = target
    }

    LaunchedEffect(office.apLimit) {
        val target = office.apLimit.toString()
        if (office.apLimitInput != target) office.apLimitInput = target
    }

    LaunchedEffect(office.idNickname) {
        if (office.idNicknameInput != office.idNickname) office.idNicknameInput = office.idNickname
    }

    LaunchedEffect(office.idFriendCode) {
        if (office.idFriendCodeInput != office.idFriendCode) office.idFriendCodeInput = office.idFriendCode
    }

    LaunchedEffect(serverIndex) {
        onServerChanged()
    }

    LaunchedEffect(office.apCurrent, office.apNotifyEnabled, office.apNotifyThreshold) {
        office.tryApThresholdNotification(context)
    }
}

@Composable
internal fun BaCalendarSyncEffect(
    context: Context,
    serverIndex: Int,
    reloadSignal: Int,
    calendarRefreshIntervalHours: Int,
    hydrationReady: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onEntriesChange: (List<BaCalendarEntry>) -> Unit,
    onLastSyncMsChange: (Long) -> Unit,
) {
    LaunchedEffect(serverIndex, reloadSignal, calendarRefreshIntervalHours, hydrationReady) {
        if (!hydrationReady) return@LaunchedEffect
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
        val networkAvailable = isNetworkAvailable(context)
        val intervalMs = calendarRefreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val cacheExpired = !hasCache || cacheSnapshot.syncMs <= 0L || (now - cacheSnapshot.syncMs).coerceAtLeast(0L) >= intervalMs
        val cacheSchemaExpired = cacheSnapshot.version < BA_CALENDAR_CACHE_SCHEMA_VERSION
        val forceRefresh = reloadSignal > 0
        val shouldRequestNetwork = forceRefresh || cacheExpired || cacheSchemaExpired

        if (hasCache) {
            onEntriesChange(cachedEntries)
            onLastSyncMsChange(cacheSnapshot.syncMs)
        } else {
            onEntriesChange(emptyList())
            onLastSyncMsChange(0L)
        }

        if (!shouldRequestNetwork) {
            onLoadingChange(false)
            onErrorChange(null)
            return@LaunchedEffect
        }

        if (!networkAvailable) {
            onLoadingChange(false)
            onErrorChange(
                if (hasCache) {
                    "当前离线，已显示本地缓存"
                } else {
                    "当前离线且无缓存，请联网后刷新"
                }
            )
            return@LaunchedEffect
        }

        onLoadingChange(true)
        onErrorChange(null)
        try {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    runWithHardTimeout(15_000L) {
                        fetchBaCalendarEntries(serverIndex, now)
                    }
                }
            }
            result
                .onSuccess { entries ->
                    val effective = if (entries.isNotEmpty()) entries else cachedEntries
                    onEntriesChange(effective)
                    onLastSyncMsChange(if (entries.isNotEmpty()) now else cacheSnapshot.syncMs)
                    onErrorChange(
                        if (entries.isNotEmpty() || !hasCache) null else "本次返回空数据，已保留本地缓存"
                    )
                    if (entries.isNotEmpty()) {
                        BASettingsStore.saveCalendarCache(serverIndex, encodeBaCalendarEntries(entries), now)
                    }
                }
                .onFailure {
                    if (hasCache) {
                        onEntriesChange(cachedEntries)
                        onLastSyncMsChange(cacheSnapshot.syncMs)
                        onErrorChange("同步超时或网络失败，已显示本地缓存")
                    } else {
                        onErrorChange("活动日历同步失败（超时或网络异常）")
                    }
                }
        } finally {
            onLoadingChange(false)
        }
    }
}

@Composable
internal fun BaPoolSyncEffect(
    context: Context,
    serverIndex: Int,
    reloadSignal: Int,
    calendarRefreshIntervalHours: Int,
    hydrationReady: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    onEntriesChange: (List<BaPoolEntry>) -> Unit,
    onLastSyncMsChange: (Long) -> Unit,
) {
    LaunchedEffect(serverIndex, reloadSignal, calendarRefreshIntervalHours, hydrationReady) {
        if (!hydrationReady) return@LaunchedEffect
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
        val networkAvailable = isNetworkAvailable(context)
        val intervalMs = calendarRefreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val cacheExpired = !hasCache || cacheSnapshot.syncMs <= 0L || (now - cacheSnapshot.syncMs).coerceAtLeast(0L) >= intervalMs
        val cacheSchemaExpired = cacheSnapshot.version < BA_POOL_CACHE_SCHEMA_VERSION
        val forceRefresh = reloadSignal > 0
        val shouldRequestNetwork = forceRefresh || cacheExpired || cacheSchemaExpired

        if (hasCache) {
            onEntriesChange(cachedEntries)
            onLastSyncMsChange(cacheSnapshot.syncMs)
        } else {
            onEntriesChange(emptyList())
            onLastSyncMsChange(0L)
        }

        if (!shouldRequestNetwork) {
            onLoadingChange(false)
            onErrorChange(null)
            return@LaunchedEffect
        }

        if (!networkAvailable) {
            onLoadingChange(false)
            onErrorChange(
                if (hasCache) {
                    "当前离线，已显示本地缓存"
                } else {
                    "当前离线且无缓存，请联网后刷新"
                }
            )
            return@LaunchedEffect
        }

        onLoadingChange(true)
        onErrorChange(null)
        try {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    runWithHardTimeout(15_000L) {
                        fetchBaPoolEntries(serverIndex, now)
                    }
                }
            }
            result
                .onSuccess { entries ->
                    val effective = if (entries.isNotEmpty()) entries else cachedEntries
                    onEntriesChange(effective)
                    onLastSyncMsChange(if (entries.isNotEmpty()) now else cacheSnapshot.syncMs)
                    onErrorChange(
                        if (entries.isNotEmpty() || !hasCache) null else "本次返回空数据，已保留本地缓存"
                    )
                    if (entries.isNotEmpty()) {
                        BASettingsStore.savePoolCache(serverIndex, encodeBaPoolEntries(entries), now)
                    }
                }
                .onFailure {
                    if (hasCache) {
                        onEntriesChange(cachedEntries)
                        onLastSyncMsChange(cacheSnapshot.syncMs)
                        onErrorChange("同步超时或网络失败，已显示本地缓存")
                    } else {
                        onErrorChange("卡池同步失败（超时或网络异常）")
                    }
                }
        } finally {
            onLoadingChange(false)
        }
    }
}
