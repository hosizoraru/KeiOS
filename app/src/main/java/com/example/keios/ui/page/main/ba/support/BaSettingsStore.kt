package com.example.keios.ui.page.main.ba

import com.tencent.mmkv.MMKV
import java.util.Locale

internal object BASettingsStore {
    private const val KV_ID = "ba_page_settings"
    private const val KEY_SERVER_INDEX = "server_index"
    private const val KEY_CAFE_LEVEL = "cafe_level"
    private const val KEY_CAFE_STORED_AP = "cafe_stored_ap"
    private const val KEY_CAFE_LAST_HOUR_MS = "cafe_last_hour_ms"
    private const val KEY_ID_NICKNAME = "id_nickname"
    private const val KEY_ID_FRIEND_CODE = "id_friend_code"
    private const val KEY_AP_LIMIT = "ap_limit"
    private const val KEY_AP_NOTIFY_ENABLED = "ap_notify_enabled"
    private const val KEY_AP_NOTIFY_THRESHOLD = "ap_notify_threshold"
    private const val KEY_AP_LAST_NOTIFIED_LEVEL = "ap_last_notified_level"
    private const val KEY_CAFE_VISIT_NOTIFY_ENABLED = "cafe_visit_notify_enabled"
    private const val KEY_CAFE_VISIT_LAST_NOTIFIED_SLOT_MS = "cafe_visit_last_notified_slot_ms"
    private const val KEY_AP_CURRENT = "ap_current"
    private const val KEY_AP_CURRENT_EXACT = "ap_current_exact"
    private const val KEY_AP_REGEN_BASE_MS = "ap_regen_base_ms"
    private const val KEY_AP_SYNC_MS = "ap_sync_ms"
    private const val KEY_POOL_SHOW_ENDED = "pool_show_ended"
    private const val KEY_ACTIVITY_SHOW_ENDED = "activity_show_ended"
    private const val KEY_SHOW_CALENDAR_POOL_IMAGES = "show_calendar_pool_images"
    private const val KEY_MEDIA_ADAPTIVE_ROTATION_ENABLED = "media_adaptive_rotation_enabled"
    private const val KEY_MEDIA_SAVE_CUSTOM_ENABLED = "media_save_custom_enabled"
    private const val KEY_MEDIA_SAVE_FIXED_TREE_URI = "media_save_fixed_tree_uri"
    private const val KEY_COFFEE_HEADPAT_MS = "coffee_headpat_ms"
    private const val KEY_COFFEE_INVITE1_USED_MS = "coffee_invite1_used_ms"
    private const val KEY_COFFEE_INVITE2_USED_MS = "coffee_invite2_used_ms"
    private const val KEY_LIST_SCROLL_INDEX = "list_scroll_index"
    private const val KEY_LIST_SCROLL_OFFSET = "list_scroll_offset"

    private const val DEFAULT_SERVER_INDEX = 2
    private const val DEFAULT_CAFE_LEVEL = 10
    private const val DEFAULT_CAFE_STORED_AP = 0.0
    private const val DEFAULT_ID_NICKNAME = BA_DEFAULT_NICKNAME
    private const val DEFAULT_ID_FRIEND_CODE = BA_DEFAULT_FRIEND_CODE
    private const val DEFAULT_AP_LIMIT = BA_AP_LIMIT_MAX
    private const val DEFAULT_AP_NOTIFY_THRESHOLD = 120
    private const val DEFAULT_AP_CURRENT = 0.0
    private const val KEY_CALENDAR_CACHE_PREFIX = "calendar_cache_"
    private const val KEY_CALENDAR_SYNC_PREFIX = "calendar_sync_"
    private const val KEY_CALENDAR_CACHE_VERSION_PREFIX = "calendar_cache_version_"
    private const val KEY_POOL_CACHE_PREFIX = "pool_cache_"
    private const val KEY_POOL_SYNC_PREFIX = "pool_sync_"
    private const val KEY_POOL_CACHE_VERSION_PREFIX = "pool_cache_version_"
    private const val KEY_CALENDAR_REFRESH_INTERVAL_HOURS = "calendar_refresh_interval_hours"
    private const val DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS = 12

    private fun calendarCacheKey(serverIndex: Int): String = "$KEY_CALENDAR_CACHE_PREFIX${serverIndex.coerceIn(0, 2)}"
    private fun calendarSyncKey(serverIndex: Int): String = "$KEY_CALENDAR_SYNC_PREFIX${serverIndex.coerceIn(0, 2)}"
    private fun calendarCacheVersionKey(serverIndex: Int): String =
        "$KEY_CALENDAR_CACHE_VERSION_PREFIX${serverIndex.coerceIn(0, 2)}"
    private fun poolCacheKey(serverIndex: Int): String = "$KEY_POOL_CACHE_PREFIX${serverIndex.coerceIn(0, 2)}"
    private fun poolSyncKey(serverIndex: Int): String = "$KEY_POOL_SYNC_PREFIX${serverIndex.coerceIn(0, 2)}"
    private fun poolCacheVersionKey(serverIndex: Int): String = "$KEY_POOL_CACHE_VERSION_PREFIX${serverIndex.coerceIn(0, 2)}"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }
    private fun kv(): MMKV = store

    fun loadCalendarCache(serverIndex: Int): Pair<String, Long> {
        val store = kv()
        return store.decodeString(calendarCacheKey(serverIndex), "").orEmpty() to
            store.decodeLong(calendarSyncKey(serverIndex), 0L)
    }

    fun saveCalendarCache(serverIndex: Int, encodedEntries: String, syncMs: Long) {
        val store = kv()
        store.encode(calendarCacheKey(serverIndex), encodedEntries)
        store.encode(calendarSyncKey(serverIndex), syncMs.coerceAtLeast(0L))
        store.encode(calendarCacheVersionKey(serverIndex), BA_CALENDAR_CACHE_SCHEMA_VERSION)
    }

    fun loadCalendarCacheVersion(serverIndex: Int): Int {
        return kv().decodeInt(calendarCacheVersionKey(serverIndex), 0)
    }

    fun loadPoolCache(serverIndex: Int): Pair<String, Long> {
        val store = kv()
        return store.decodeString(poolCacheKey(serverIndex), "").orEmpty() to
            store.decodeLong(poolSyncKey(serverIndex), 0L)
    }

    fun savePoolCache(serverIndex: Int, encodedEntries: String, syncMs: Long) {
        val store = kv()
        store.encode(poolCacheKey(serverIndex), encodedEntries)
        store.encode(poolSyncKey(serverIndex), syncMs.coerceAtLeast(0L))
        store.encode(poolCacheVersionKey(serverIndex), BA_POOL_CACHE_SCHEMA_VERSION)
    }

    fun loadPoolCacheVersion(serverIndex: Int): Int {
        return kv().decodeInt(poolCacheVersionKey(serverIndex), 0)
    }

    fun loadPoolShowEnded(): Boolean = kv().decodeBool(KEY_POOL_SHOW_ENDED, false)
    fun savePoolShowEnded(enabled: Boolean) {
        kv().encode(KEY_POOL_SHOW_ENDED, enabled)
    }

    fun loadActivityShowEnded(): Boolean = kv().decodeBool(KEY_ACTIVITY_SHOW_ENDED, false)
    fun saveActivityShowEnded(enabled: Boolean) {
        kv().encode(KEY_ACTIVITY_SHOW_ENDED, enabled)
    }

    fun loadShowCalendarPoolImages(): Boolean = kv().decodeBool(KEY_SHOW_CALENDAR_POOL_IMAGES, true)
    fun saveShowCalendarPoolImages(enabled: Boolean) {
        kv().encode(KEY_SHOW_CALENDAR_POOL_IMAGES, enabled)
    }

    fun loadMediaAdaptiveRotationEnabled(): Boolean =
        kv().decodeBool(KEY_MEDIA_ADAPTIVE_ROTATION_ENABLED, true)

    fun saveMediaAdaptiveRotationEnabled(enabled: Boolean) {
        kv().encode(KEY_MEDIA_ADAPTIVE_ROTATION_ENABLED, enabled)
    }

    fun loadMediaSaveCustomEnabled(): Boolean =
        kv().decodeBool(KEY_MEDIA_SAVE_CUSTOM_ENABLED, false)

    fun saveMediaSaveCustomEnabled(enabled: Boolean) {
        kv().encode(KEY_MEDIA_SAVE_CUSTOM_ENABLED, enabled)
    }

    fun loadMediaSaveFixedTreeUri(): String =
        kv().decodeString(KEY_MEDIA_SAVE_FIXED_TREE_URI, "").orEmpty().trim()

    fun saveMediaSaveFixedTreeUri(uri: String) {
        kv().encode(KEY_MEDIA_SAVE_FIXED_TREE_URI, uri.trim())
    }

    fun loadCalendarRefreshIntervalHours(): Int {
        val raw = kv().decodeInt(
            KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
            DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS
        )
        return BaCalendarRefreshIntervalOption.fromHours(raw).hours
    }

    fun saveCalendarRefreshIntervalHours(hours: Int) {
        kv().encode(
            KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
            BaCalendarRefreshIntervalOption.fromHours(hours).hours
        )
    }

    fun loadSnapshot(): BaPageSnapshot {
        val store = kv()
        val serverIndex = store.decodeInt(KEY_SERVER_INDEX, DEFAULT_SERVER_INDEX).coerceIn(0, 2)
        val cafeLevel = store.decodeInt(KEY_CAFE_LEVEL, DEFAULT_CAFE_LEVEL).coerceIn(1, 10)
        val cafeStoredAp = normalizeAp(
            store.decodeString(KEY_CAFE_STORED_AP, DEFAULT_CAFE_STORED_AP.toString())?.toDoubleOrNull()
                ?: DEFAULT_CAFE_STORED_AP
        )
        val idNickname = store.decodeString(KEY_ID_NICKNAME, DEFAULT_ID_NICKNAME).orEmpty().take(10)
            .ifEmpty { DEFAULT_ID_NICKNAME }
        val idFriendCode = store.decodeString(KEY_ID_FRIEND_CODE, DEFAULT_ID_FRIEND_CODE)
            .orEmpty()
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
            .let { if (it.length == 8) it else DEFAULT_ID_FRIEND_CODE }
        val apCurrent = if (store.containsKey(KEY_AP_CURRENT_EXACT)) {
            store.decodeString(KEY_AP_CURRENT_EXACT, DEFAULT_AP_CURRENT.toString())?.toDoubleOrNull() ?: DEFAULT_AP_CURRENT
        } else {
            store.decodeInt(KEY_AP_CURRENT, DEFAULT_AP_CURRENT.toInt()).toDouble()
        }
        val refreshHours = BaCalendarRefreshIntervalOption.fromHours(
            store.decodeInt(
                KEY_CALENDAR_REFRESH_INTERVAL_HOURS,
                DEFAULT_CALENDAR_REFRESH_INTERVAL_HOURS
            )
        ).hours
        return BaPageSnapshot(
            serverIndex = serverIndex,
            cafeLevel = cafeLevel,
            cafeStoredAp = normalizeAp(cafeStoredAp),
            cafeLastHourMs = store.decodeLong(KEY_CAFE_LAST_HOUR_MS, 0L),
            idNickname = idNickname,
            idFriendCode = idFriendCode,
            apLimit = store.decodeInt(KEY_AP_LIMIT, DEFAULT_AP_LIMIT).coerceIn(0, BA_AP_LIMIT_MAX),
            apCurrent = normalizeAp(apCurrent.coerceIn(0.0, BA_AP_MAX.toDouble())),
            apRegenBaseMs = store.decodeLong(KEY_AP_REGEN_BASE_MS, 0L),
            apSyncMs = store.decodeLong(KEY_AP_SYNC_MS, 0L),
            apNotifyEnabled = store.decodeBool(KEY_AP_NOTIFY_ENABLED, false),
            apNotifyThreshold = store.decodeInt(KEY_AP_NOTIFY_THRESHOLD, DEFAULT_AP_NOTIFY_THRESHOLD).coerceIn(0, BA_AP_MAX),
            apLastNotifiedLevel = store.decodeInt(KEY_AP_LAST_NOTIFIED_LEVEL, -1).coerceIn(-1, BA_AP_MAX),
            cafeVisitNotifyEnabled = store.decodeBool(KEY_CAFE_VISIT_NOTIFY_ENABLED, false),
            cafeVisitLastNotifiedSlotMs = store.decodeLong(KEY_CAFE_VISIT_LAST_NOTIFIED_SLOT_MS, 0L).coerceAtLeast(0L),
            coffeeHeadpatMs = store.decodeLong(KEY_COFFEE_HEADPAT_MS, 0L),
            coffeeInvite1UsedMs = store.decodeLong(KEY_COFFEE_INVITE1_USED_MS, 0L),
            coffeeInvite2UsedMs = store.decodeLong(KEY_COFFEE_INVITE2_USED_MS, 0L),
            showEndedPools = store.decodeBool(KEY_POOL_SHOW_ENDED, false),
            showEndedActivities = store.decodeBool(KEY_ACTIVITY_SHOW_ENDED, false),
            showCalendarPoolImages = store.decodeBool(KEY_SHOW_CALENDAR_POOL_IMAGES, true),
            mediaAdaptiveRotationEnabled = store.decodeBool(KEY_MEDIA_ADAPTIVE_ROTATION_ENABLED, true),
            mediaSaveCustomEnabled = store.decodeBool(KEY_MEDIA_SAVE_CUSTOM_ENABLED, false),
            mediaSaveFixedTreeUri = store.decodeString(KEY_MEDIA_SAVE_FIXED_TREE_URI, "").orEmpty().trim(),
            calendarRefreshIntervalHours = refreshHours
        )
    }

    fun loadCalendarCacheSnapshot(serverIndex: Int): BaCacheSnapshot {
        val store = kv()
        return BaCacheSnapshot(
            raw = store.decodeString(calendarCacheKey(serverIndex), "").orEmpty(),
            syncMs = store.decodeLong(calendarSyncKey(serverIndex), 0L),
            version = store.decodeInt(calendarCacheVersionKey(serverIndex), 0)
        )
    }

    fun loadPoolCacheSnapshot(serverIndex: Int): BaCacheSnapshot {
        val store = kv()
        return BaCacheSnapshot(
            raw = store.decodeString(poolCacheKey(serverIndex), "").orEmpty(),
            syncMs = store.decodeLong(poolSyncKey(serverIndex), 0L),
            version = store.decodeInt(poolCacheVersionKey(serverIndex), 0)
        )
    }

    fun loadServerIndex(): Int = kv().decodeInt(KEY_SERVER_INDEX, DEFAULT_SERVER_INDEX).coerceIn(0, 2)
    fun saveServerIndex(index: Int) {
        kv().encode(KEY_SERVER_INDEX, index.coerceIn(0, 2))
    }

    fun loadCafeLevel(): Int = kv().decodeInt(KEY_CAFE_LEVEL, DEFAULT_CAFE_LEVEL).coerceIn(1, 10)
    fun saveCafeLevel(level: Int) {
        kv().encode(KEY_CAFE_LEVEL, level.coerceIn(1, 10))
    }

    fun loadCafeStoredAp(): Double {
        val raw = kv().decodeString(KEY_CAFE_STORED_AP, DEFAULT_CAFE_STORED_AP.toString())
        return normalizeAp(raw?.toDoubleOrNull() ?: DEFAULT_CAFE_STORED_AP)
    }

    fun saveCafeStoredAp(storedAp: Double) {
        kv().encode(KEY_CAFE_STORED_AP, normalizeAp(storedAp).toString())
    }

    fun loadCafeLastHourMs(): Long = kv().decodeLong(KEY_CAFE_LAST_HOUR_MS, 0L)
    fun saveCafeLastHourMs(epochMs: Long) {
        kv().encode(KEY_CAFE_LAST_HOUR_MS, floorToHourMs(epochMs.coerceAtLeast(0L)))
    }

    fun loadIdNickname(): String {
        val raw = kv().decodeString(KEY_ID_NICKNAME, DEFAULT_ID_NICKNAME).orEmpty().take(10)
        return raw.ifEmpty { DEFAULT_ID_NICKNAME }
    }

    fun saveIdNickname(name: String) {
        val sanitized = name.take(10).ifEmpty { DEFAULT_ID_NICKNAME }
        kv().encode(KEY_ID_NICKNAME, sanitized)
    }

    fun loadIdFriendCode(): String {
        val normalized = kv().decodeString(KEY_ID_FRIEND_CODE, DEFAULT_ID_FRIEND_CODE)
            .orEmpty()
            .uppercase(Locale.ROOT)
            .filter { it in 'A'..'Z' }
            .take(8)
        return if (normalized.length == 8) normalized else DEFAULT_ID_FRIEND_CODE
    }

    fun saveIdFriendCode(code: String) {
        val normalized = code.uppercase(Locale.ROOT).filter { it in 'A'..'Z' }.take(8)
        kv().encode(
            KEY_ID_FRIEND_CODE,
            if (normalized.length == 8) normalized else DEFAULT_ID_FRIEND_CODE
        )
    }

    fun loadApLimit(): Int = kv().decodeInt(KEY_AP_LIMIT, DEFAULT_AP_LIMIT).coerceIn(0, BA_AP_LIMIT_MAX)
    fun saveApLimit(limit: Int) {
        kv().encode(KEY_AP_LIMIT, limit.coerceIn(0, BA_AP_LIMIT_MAX))
    }

    fun loadApNotifyEnabled(): Boolean = kv().decodeBool(KEY_AP_NOTIFY_ENABLED, false)
    fun saveApNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_AP_NOTIFY_ENABLED, enabled)
    }

    fun loadApNotifyThreshold(): Int =
        kv().decodeInt(KEY_AP_NOTIFY_THRESHOLD, DEFAULT_AP_NOTIFY_THRESHOLD).coerceIn(0, BA_AP_MAX)

    fun saveApNotifyThreshold(threshold: Int) {
        kv().encode(KEY_AP_NOTIFY_THRESHOLD, threshold.coerceIn(0, BA_AP_MAX))
    }

    fun loadApLastNotifiedLevel(): Int =
        kv().decodeInt(KEY_AP_LAST_NOTIFIED_LEVEL, -1).coerceIn(-1, BA_AP_MAX)

    fun saveApLastNotifiedLevel(level: Int) {
        kv().encode(KEY_AP_LAST_NOTIFIED_LEVEL, level.coerceIn(-1, BA_AP_MAX))
    }

    fun loadCafeVisitNotifyEnabled(): Boolean = kv().decodeBool(KEY_CAFE_VISIT_NOTIFY_ENABLED, false)
    fun saveCafeVisitNotifyEnabled(enabled: Boolean) {
        kv().encode(KEY_CAFE_VISIT_NOTIFY_ENABLED, enabled)
    }

    fun loadCafeVisitLastNotifiedSlotMs(): Long =
        kv().decodeLong(KEY_CAFE_VISIT_LAST_NOTIFIED_SLOT_MS, 0L).coerceAtLeast(0L)

    fun saveCafeVisitLastNotifiedSlotMs(slotMs: Long) {
        kv().encode(KEY_CAFE_VISIT_LAST_NOTIFIED_SLOT_MS, slotMs.coerceAtLeast(0L))
    }

    fun loadApCurrent(): Double {
        val store = kv()
        val value = if (store.containsKey(KEY_AP_CURRENT_EXACT)) {
            store.decodeString(KEY_AP_CURRENT_EXACT, DEFAULT_AP_CURRENT.toString())?.toDoubleOrNull() ?: DEFAULT_AP_CURRENT
        } else {
            store.decodeInt(KEY_AP_CURRENT, DEFAULT_AP_CURRENT.toInt()).toDouble()
        }
        return normalizeAp(value.coerceIn(0.0, BA_AP_MAX.toDouble()))
    }

    fun saveApCurrent(current: Double) {
        val normalized = normalizeAp(current)
        kv().encode(KEY_AP_CURRENT_EXACT, normalized.toString())
        kv().encode(KEY_AP_CURRENT, displayAp(normalized))
    }

    fun loadApRegenBaseMs(): Long = kv().decodeLong(KEY_AP_REGEN_BASE_MS, 0L)
    fun saveApRegenBaseMs(epochMs: Long) {
        kv().encode(KEY_AP_REGEN_BASE_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadApSyncMs(): Long = kv().decodeLong(KEY_AP_SYNC_MS, 0L)
    fun saveApSyncMs(epochMs: Long) {
        kv().encode(KEY_AP_SYNC_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadCoffeeHeadpatMs(): Long = kv().decodeLong(KEY_COFFEE_HEADPAT_MS, 0L)
    fun saveCoffeeHeadpatMs(epochMs: Long) {
        kv().encode(KEY_COFFEE_HEADPAT_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadCoffeeInvite1UsedMs(): Long = kv().decodeLong(KEY_COFFEE_INVITE1_USED_MS, 0L)
    fun saveCoffeeInvite1UsedMs(epochMs: Long) {
        kv().encode(KEY_COFFEE_INVITE1_USED_MS, epochMs.coerceAtLeast(0L))
    }

    fun loadCoffeeInvite2UsedMs(): Long = kv().decodeLong(KEY_COFFEE_INVITE2_USED_MS, 0L)
    fun saveCoffeeInvite2UsedMs(epochMs: Long) {
        kv().encode(KEY_COFFEE_INVITE2_USED_MS, epochMs.coerceAtLeast(0L))
    }

    fun clearCalendarAndPoolCaches() {
        val store = kv()
        for (serverIndex in 0..2) {
            store.removeValueForKey(calendarCacheKey(serverIndex))
            store.removeValueForKey(calendarSyncKey(serverIndex))
            store.removeValueForKey(calendarCacheVersionKey(serverIndex))
            store.removeValueForKey(poolCacheKey(serverIndex))
            store.removeValueForKey(poolSyncKey(serverIndex))
            store.removeValueForKey(poolCacheVersionKey(serverIndex))
        }
        store.trim()
    }

    fun storageFootprintBytes(): Long = kv().totalSize()
    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long {
        var total = 0L
        for (serverIndex in 0..2) {
            val calendar = loadCalendarCacheSnapshot(serverIndex)
            val pool = loadPoolCacheSnapshot(serverIndex)
            total += calendar.raw.length.toLong() * 2 + 16L
            total += pool.raw.length.toLong() * 2 + 16L
        }
        return total
    }

    fun configBytesEstimated(): Long {
        val snapshot = loadSnapshot()
        return listOf(snapshot.idNickname, snapshot.idFriendCode).sumOf { it.length.toLong() * 2 } + 160L
    }

    fun clearListScrollState() {
        val store = kv()
        store.removeValueForKey(KEY_LIST_SCROLL_INDEX)
        store.removeValueForKey(KEY_LIST_SCROLL_OFFSET)
    }
}
