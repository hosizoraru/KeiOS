package com.example.keios.ui.page.main.student

import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

private const val BA_GUIDE_KV_ID = "ba_student_guide"
private const val BA_GUIDE_KEY_CURRENT_URL = "current_url"
private const val BA_GUIDE_KEY_LEGACY_CACHE_PREFIX = "cache_"
private const val BA_GUIDE_KEY_V2_CACHE_PREFIX = "guide_cache_v2_"
private const val BA_GUIDE_KEY_V2_INDEX = "guide_cache_v2_index"
private const val BA_GUIDE_CACHE_SCHEMA_VERSION = 2
private const val BA_GUIDE_MEMORY_CACHE_LIMIT = 8

private const val CACHE_SUFFIX_META = "meta"
private const val CACHE_SUFFIX_STATS = "stats"
private const val CACHE_SUFFIX_SKILL = "skill"
private const val CACHE_SUFFIX_PROFILE = "profile"
private const val CACHE_SUFFIX_GALLERY = "gallery"
private const val CACHE_SUFFIX_GROWTH = "growth"
private const val CACHE_SUFFIX_SIMULATE = "simulate"
private const val CACHE_SUFFIX_VOICE_ROWS = "voice_rows"
private const val CACHE_SUFFIX_VOICE_ENTRIES = "voice_entries"

private val CACHE_REQUIRED_SUFFIXES = listOf(
    CACHE_SUFFIX_META,
    CACHE_SUFFIX_STATS,
    CACHE_SUFFIX_SKILL,
    CACHE_SUFFIX_PROFILE,
    CACHE_SUFFIX_GALLERY,
    CACHE_SUFFIX_GROWTH,
    CACHE_SUFFIX_SIMULATE,
    CACHE_SUFFIX_VOICE_ROWS,
    CACHE_SUFFIX_VOICE_ENTRIES
)

data class BaStudentGuideCacheSnapshot(
    val info: BaStudentGuideInfo?,
    val hasCache: Boolean,
    val isComplete: Boolean,
    val syncedAtMs: Long
) {
    companion object {
        val EMPTY = BaStudentGuideCacheSnapshot(
            info = null,
            hasCache = false,
            isComplete = false,
            syncedAtMs = 0L
        )
    }
}

object BaStudentGuideStore {
    private val store: MMKV by lazy { MMKV.mmkvWithID(BA_GUIDE_KV_ID) }

    private val memoryCache = object : LinkedHashMap<String, BaStudentGuideInfo>(
        BA_GUIDE_MEMORY_CACHE_LIMIT,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BaStudentGuideInfo>?): Boolean {
            return size > BA_GUIDE_MEMORY_CACHE_LIMIT
        }
    }

    private fun kv(): MMKV = store

    private fun normalizeSourceUrl(url: String): String = normalizeGuideUrl(url).trim()

    private fun cacheId(url: String): String = normalizeSourceUrl(url).hashCode().toUInt().toString(16)

    private fun legacyCacheKey(url: String): String = BA_GUIDE_KEY_LEGACY_CACHE_PREFIX + cacheId(url)

    private fun v2EntryPrefix(id: String): String = "$BA_GUIDE_KEY_V2_CACHE_PREFIX${id}_"

    private fun v2CacheKey(id: String, suffix: String): String = "${v2EntryPrefix(id)}$suffix"

    private fun memoryGet(sourceUrl: String): BaStudentGuideInfo? {
        return synchronized(memoryCache) { memoryCache[sourceUrl] }
    }

    private fun memoryPut(info: BaStudentGuideInfo) {
        val source = normalizeSourceUrl(info.sourceUrl)
        if (source.isBlank()) return
        val normalizedInfo = if (source == info.sourceUrl) info else info.copy(sourceUrl = source)
        synchronized(memoryCache) {
            memoryCache[source] = normalizedInfo
        }
    }

    private fun memoryRemove(sourceUrl: String) {
        synchronized(memoryCache) {
            memoryCache.remove(sourceUrl)
        }
    }

    private fun memoryClear() {
        synchronized(memoryCache) {
            memoryCache.clear()
        }
    }

    private fun readV2Index(store: MMKV = kv()): MutableSet<String> {
        val raw = store.decodeString(BA_GUIDE_KEY_V2_INDEX, "").orEmpty()
        if (raw.isBlank()) return mutableSetOf()
        return runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    val value = normalizeSourceUrl(arr.optString(i))
                    if (value.isNotBlank()) add(value)
                }
            }.toMutableSet()
        }.getOrDefault(mutableSetOf())
    }

    private fun writeV2Index(index: Set<String>, store: MMKV = kv()) {
        if (index.isEmpty()) {
            store.removeValueForKey(BA_GUIDE_KEY_V2_INDEX)
            return
        }
        val raw = JSONArray().apply {
            index.sorted().forEach { put(it) }
        }.toString()
        store.encode(BA_GUIDE_KEY_V2_INDEX, raw)
    }

    private fun isInfoPayloadComplete(info: BaStudentGuideInfo?): Boolean {
        info ?: return false
        if (info.syncedAtMs <= 0L) return false
        val hasIdentity =
            info.title.isNotBlank() ||
                info.subtitle.isNotBlank() ||
                info.summary.isNotBlank() ||
                info.imageUrl.isNotBlank()
        val hasAnySection =
            info.stats.isNotEmpty() ||
                info.skillRows.isNotEmpty() ||
                info.profileRows.isNotEmpty() ||
                info.galleryItems.isNotEmpty() ||
                info.growthRows.isNotEmpty() ||
                info.simulateRows.isNotEmpty() ||
                info.voiceRows.isNotEmpty() ||
                info.voiceEntries.isNotEmpty()
        return hasIdentity && hasAnySection
    }

    private fun decodeLegacyInfo(raw: String, source: String): BaStudentGuideInfo? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val stats = decodeStats(obj.optJSONArray("stats"))
            val voiceCvJp = obj.optString("voiceCvJp").trim()
            val voiceCvCn = obj.optString("voiceCvCn").trim()
            val voiceCvByLanguage = decodeVoiceCvByLanguage(
                obj = obj,
                fallbackJp = voiceCvJp,
                fallbackCn = voiceCvCn
            )
            BaStudentGuideInfo(
                sourceUrl = normalizeSourceUrl(obj.optString("sourceUrl").ifBlank { source }),
                title = obj.optString("title"),
                subtitle = obj.optString("subtitle"),
                description = obj.optString("description"),
                imageUrl = obj.optString("imageUrl"),
                summary = obj.optString("summary"),
                stats = stats,
                skillRows = decodeGuideRows(obj, "skillRows"),
                profileRows = decodeGuideRows(obj, "profileRows"),
                galleryItems = decodeGalleryItems(obj, "galleryItems"),
                growthRows = decodeGuideRows(obj, "growthRows"),
                simulateRows = decodeGuideRows(obj, "simulateRows"),
                voiceRows = decodeGuideRows(obj, "voiceRows"),
                voiceCvJp = voiceCvJp,
                voiceCvCn = voiceCvCn,
                voiceCvByLanguage = voiceCvByLanguage,
                voiceLanguageHeaders = decodeHeaders(obj.optJSONArray("voiceLanguageHeaders")),
                voiceEntries = decodeVoiceEntries(obj, "voiceEntries"),
                tabSkillIconUrl = obj.optString("tabSkillIconUrl").trim(),
                tabProfileIconUrl = obj.optString("tabProfileIconUrl").trim(),
                tabVoiceIconUrl = obj.optString("tabVoiceIconUrl").trim(),
                tabGalleryIconUrl = obj.optString("tabGalleryIconUrl").trim(),
                tabSimulateIconUrl = obj.optString("tabSimulateIconUrl").trim(),
                syncedAtMs = obj.optLong("syncedAtMs", 0L)
            )
        }.getOrNull()
    }

    private fun decodeV2Info(source: String, id: String, store: MMKV): BaStudentGuideInfo? {
        val metaRaw = store.decodeString(v2CacheKey(id, CACHE_SUFFIX_META), "").orEmpty()
        if (metaRaw.isBlank()) return null
        return runCatching {
            val meta = JSONObject(metaRaw)
            if (meta.optInt("schema", 0) < BA_GUIDE_CACHE_SCHEMA_VERSION) return@runCatching null
            val voiceCvJp = meta.optString("voiceCvJp").trim()
            val voiceCvCn = meta.optString("voiceCvCn").trim()
            val stats = decodeStats(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_STATS), "").orEmpty()
                )
            )
            val skillRows = decodeGuideRowsFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_SKILL), "").orEmpty()
                )
            )
            val profileRows = decodeGuideRowsFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_PROFILE), "").orEmpty()
                )
            )
            val galleryItems = decodeGalleryItemsFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_GALLERY), "").orEmpty()
                )
            )
            val growthRows = decodeGuideRowsFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_GROWTH), "").orEmpty()
                )
            )
            val simulateRows = decodeGuideRowsFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_SIMULATE), "").orEmpty()
                )
            )
            val voiceRows = decodeGuideRowsFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_VOICE_ROWS), "").orEmpty()
                )
            )
            val voiceEntries = decodeVoiceEntriesFromArray(
                parseJsonArray(
                    store.decodeString(v2CacheKey(id, CACHE_SUFFIX_VOICE_ENTRIES), "").orEmpty()
                )
            )
            val voiceCvByLanguage = decodeVoiceCvByLanguage(
                obj = meta,
                fallbackJp = voiceCvJp,
                fallbackCn = voiceCvCn
            )
            BaStudentGuideInfo(
                sourceUrl = normalizeSourceUrl(meta.optString("sourceUrl").ifBlank { source }),
                title = meta.optString("title"),
                subtitle = meta.optString("subtitle"),
                description = meta.optString("description"),
                imageUrl = meta.optString("imageUrl"),
                summary = meta.optString("summary"),
                stats = stats,
                skillRows = skillRows,
                profileRows = profileRows,
                galleryItems = galleryItems,
                growthRows = growthRows,
                simulateRows = simulateRows,
                voiceRows = voiceRows,
                voiceCvJp = voiceCvJp,
                voiceCvCn = voiceCvCn,
                voiceCvByLanguage = voiceCvByLanguage,
                voiceLanguageHeaders = decodeHeaders(meta.optJSONArray("voiceLanguageHeaders")),
                voiceEntries = voiceEntries,
                tabSkillIconUrl = meta.optString("tabSkillIconUrl").trim(),
                tabProfileIconUrl = meta.optString("tabProfileIconUrl").trim(),
                tabVoiceIconUrl = meta.optString("tabVoiceIconUrl").trim(),
                tabGalleryIconUrl = meta.optString("tabGalleryIconUrl").trim(),
                tabSimulateIconUrl = meta.optString("tabSimulateIconUrl").trim(),
                syncedAtMs = meta.optLong("syncedAtMs", 0L)
            )
        }.getOrNull()
    }

    private fun readSyncedAtMsFromV2Meta(store: MMKV, id: String): Long {
        val raw = store.decodeString(v2CacheKey(id, CACHE_SUFFIX_META), "").orEmpty()
        if (raw.isBlank()) return 0L
        return runCatching { JSONObject(raw).optLong("syncedAtMs", 0L) }.getOrDefault(0L)
    }

    fun setCurrentUrl(url: String) {
        kv().encode(BA_GUIDE_KEY_CURRENT_URL, normalizeSourceUrl(url))
    }

    fun loadCurrentUrl(): String = normalizeSourceUrl(
        kv().decodeString(BA_GUIDE_KEY_CURRENT_URL, "").orEmpty()
    )

    fun saveInfo(info: BaStudentGuideInfo) {
        val source = normalizeSourceUrl(info.sourceUrl)
        if (source.isBlank()) return
        val normalizedInfo = if (source == info.sourceUrl) info else info.copy(sourceUrl = source)
        val id = cacheId(source)
        val store = kv()

        val metaRaw = JSONObject().apply {
            put("schema", BA_GUIDE_CACHE_SCHEMA_VERSION)
            put("sourceUrl", source)
            put("title", normalizedInfo.title)
            put("subtitle", normalizedInfo.subtitle)
            put("description", normalizedInfo.description)
            put("imageUrl", normalizedInfo.imageUrl)
            put("summary", normalizedInfo.summary)
            put("syncedAtMs", normalizedInfo.syncedAtMs.coerceAtLeast(0L))
            put("voiceCvJp", normalizedInfo.voiceCvJp)
            put("voiceCvCn", normalizedInfo.voiceCvCn)
            put("voiceCvByLanguage", encodeStringMap(normalizedInfo.voiceCvByLanguage))
            put(
                "voiceLanguageHeaders",
                JSONArray().apply { normalizedInfo.voiceLanguageHeaders.forEach { put(it) } }
            )
            put("tabSkillIconUrl", normalizedInfo.tabSkillIconUrl)
            put("tabProfileIconUrl", normalizedInfo.tabProfileIconUrl)
            put("tabVoiceIconUrl", normalizedInfo.tabVoiceIconUrl)
            put("tabGalleryIconUrl", normalizedInfo.tabGalleryIconUrl)
            put("tabSimulateIconUrl", normalizedInfo.tabSimulateIconUrl)
        }.toString()

        store.encode(v2CacheKey(id, CACHE_SUFFIX_META), metaRaw)
        store.encode(v2CacheKey(id, CACHE_SUFFIX_STATS), encodeStats(normalizedInfo.stats).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_SKILL), encodeGuideRows(normalizedInfo.skillRows).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_PROFILE), encodeGuideRows(normalizedInfo.profileRows).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_GALLERY), encodeGalleryItems(normalizedInfo.galleryItems).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_GROWTH), encodeGuideRows(normalizedInfo.growthRows).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_SIMULATE), encodeGuideRows(normalizedInfo.simulateRows).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_VOICE_ROWS), encodeGuideRows(normalizedInfo.voiceRows).toString())
        store.encode(v2CacheKey(id, CACHE_SUFFIX_VOICE_ENTRIES), encodeVoiceEntries(normalizedInfo.voiceEntries).toString())

        store.removeValueForKey(legacyCacheKey(source))

        val index = readV2Index(store)
        index += source
        writeV2Index(index, store)
        memoryPut(normalizedInfo)
    }

    fun loadInfoSnapshot(url: String): BaStudentGuideCacheSnapshot {
        val source = normalizeSourceUrl(url)
        if (source.isBlank()) return BaStudentGuideCacheSnapshot.EMPTY

        memoryGet(source)?.let { memory ->
            return BaStudentGuideCacheSnapshot(
                info = memory,
                hasCache = true,
                isComplete = true,
                syncedAtMs = memory.syncedAtMs
            )
        }

        val store = kv()
        val id = cacheId(source)
        val hasV2Any = CACHE_REQUIRED_SUFFIXES.any { suffix ->
            store.containsKey(v2CacheKey(id, suffix))
        }

        if (hasV2Any) {
            val hasAllRequired = CACHE_REQUIRED_SUFFIXES.all { suffix ->
                store.containsKey(v2CacheKey(id, suffix))
            }
            val syncedAtMs = readSyncedAtMsFromV2Meta(store, id)
            if (!hasAllRequired) {
                return BaStudentGuideCacheSnapshot(
                    info = null,
                    hasCache = true,
                    isComplete = false,
                    syncedAtMs = syncedAtMs
                )
            }
            val info = decodeV2Info(source = source, id = id, store = store)
            val complete = isInfoPayloadComplete(info)
            if (complete && info != null) {
                memoryPut(info)
            }
            return BaStudentGuideCacheSnapshot(
                info = if (complete) info else null,
                hasCache = true,
                isComplete = complete,
                syncedAtMs = info?.syncedAtMs ?: syncedAtMs
            )
        }

        val legacyRaw = store.decodeString(legacyCacheKey(source), "").orEmpty()
        if (legacyRaw.isNotBlank()) {
            val legacyInfo = decodeLegacyInfo(legacyRaw, source)
            val complete = isInfoPayloadComplete(legacyInfo)
            if (complete && legacyInfo != null) {
                // 旧结构自动迁移到分块缓存结构。
                saveInfo(legacyInfo)
                memoryPut(legacyInfo)
            }
            return BaStudentGuideCacheSnapshot(
                info = if (complete) legacyInfo else null,
                hasCache = true,
                isComplete = complete,
                syncedAtMs = legacyInfo?.syncedAtMs ?: 0L
            )
        }

        return BaStudentGuideCacheSnapshot.EMPTY
    }

    fun loadInfo(url: String): BaStudentGuideInfo? {
        return loadInfoSnapshot(url).info
    }

    fun isCacheExpired(
        snapshot: BaStudentGuideCacheSnapshot,
        refreshIntervalHours: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!snapshot.hasCache) return true
        if (snapshot.syncedAtMs <= 0L) return true
        val intervalMs = refreshIntervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        return (nowMs - snapshot.syncedAtMs).coerceAtLeast(0L) >= intervalMs
    }

    fun clearCachedInfo(url: String) {
        val source = normalizeSourceUrl(url)
        if (source.isBlank()) return
        val id = cacheId(source)
        val store = kv()
        store.removeValueForKey(legacyCacheKey(source))
        store.allKeys()
            .orEmpty()
            .filter { key -> key.startsWith(v2EntryPrefix(id)) }
            .forEach(store::removeValueForKey)

        val index = readV2Index(store)
        if (index.remove(source)) {
            writeV2Index(index, store)
        }
        memoryRemove(source)
    }

    fun cachedEntryCount(): Int {
        val store = kv()
        val v2Count = readV2Index(store).size
        val legacyCount = store.allKeys()
            .orEmpty()
            .count { key -> key.startsWith(BA_GUIDE_KEY_LEGACY_CACHE_PREFIX) }
        return v2Count + legacyCount
    }

    fun clearAllCachedInfo() {
        val store = kv()
        store.allKeys()
            .orEmpty()
            .filter { key ->
                key.startsWith(BA_GUIDE_KEY_LEGACY_CACHE_PREFIX) ||
                    key.startsWith(BA_GUIDE_KEY_V2_CACHE_PREFIX) ||
                    key == BA_GUIDE_KEY_V2_INDEX
            }
            .forEach(store::removeValueForKey)
        memoryClear()
        store.trim()
    }

    fun storageFootprintBytes(): Long = kv().totalSize()

    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long {
        val store = kv()
        return store.allKeys()
            .orEmpty()
            .filter { key ->
                key.startsWith(BA_GUIDE_KEY_LEGACY_CACHE_PREFIX) ||
                    key.startsWith(BA_GUIDE_KEY_V2_CACHE_PREFIX) ||
                    key == BA_GUIDE_KEY_V2_INDEX
            }
            .sumOf { key -> store.decodeString(key, "").orEmpty().length.toLong() * 2 + 16L }
    }

    fun configBytesEstimated(): Long {
        val currentUrlBytes = loadCurrentUrl().length.toLong() * 2 + 16L
        val indexBytes = kv().decodeString(BA_GUIDE_KEY_V2_INDEX, "").orEmpty().length.toLong() * 2 + 16L
        return currentUrlBytes + indexBytes
    }

    fun latestSyncedAtMs(): Long {
        val store = kv()
        var latest = 0L

        readV2Index(store).forEach { source ->
            val synced = readSyncedAtMsFromV2Meta(store, cacheId(source))
            if (synced > latest) latest = synced
        }

        store.allKeys()
            .orEmpty()
            .filter { key -> key.startsWith(BA_GUIDE_KEY_LEGACY_CACHE_PREFIX) }
            .forEach { key ->
                val synced = runCatching {
                    JSONObject(store.decodeString(key, "").orEmpty()).optLong("syncedAtMs", 0L)
                }.getOrDefault(0L)
                if (synced > latest) latest = synced
            }

        return latest
    }

    fun cachedSourceUrls(): Set<String> {
        val store = kv()
        return readV2Index(store).toSet()
    }
}
