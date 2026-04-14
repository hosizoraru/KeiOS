package com.example.keios.ui.page.main.student

import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

private const val BA_GUIDE_KV_ID = "ba_student_guide"
private const val BA_GUIDE_KEY_CURRENT_URL = "current_url"
private const val BA_GUIDE_KEY_CACHE_PREFIX = "cache_"

object BaStudentGuideStore {
    private val store: MMKV by lazy { MMKV.mmkvWithID(BA_GUIDE_KV_ID) }

    private fun kv(): MMKV = store

    private fun encodeGuideRows(rows: List<BaGuideRow>): JSONArray {
        return JSONArray().apply {
            rows.forEach { row ->
                put(
                    JSONObject().apply {
                        put("k", row.key)
                        put("v", row.value)
                        put("img", row.imageUrl)
                        put(
                            "imgs",
                            JSONArray().apply {
                                row.imageUrls.forEach { item ->
                                    if (item.isNotBlank()) put(item)
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    private fun decodeGuideRows(obj: JSONObject, key: String): List<BaGuideRow> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val k = row.optString("k").trim()
                val v = row.optString("v").trim()
                val img = row.optString("img").trim()
                val imgs = buildList {
                    val arr = row.optJSONArray("imgs")
                    if (arr != null) {
                        for (j in 0 until arr.length()) {
                            val value = arr.optString(j).trim()
                            if (value.isNotBlank()) add(value)
                        }
                    }
                }
                val normalizedImgs = if (imgs.isNotEmpty()) imgs else listOf(img).filter { it.isNotBlank() }
                if (k.isBlank() && v.isBlank() && img.isBlank() && normalizedImgs.isEmpty()) continue
                add(BaGuideRow(key = k, value = v, imageUrl = img, imageUrls = normalizedImgs))
            }
        }
    }

    private fun encodeGalleryItems(items: List<BaGuideGalleryItem>): JSONArray {
        return JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("t", item.title)
                        put("img", item.imageUrl)
                        put("mt", item.mediaType)
                        put("mu", item.mediaUrl)
                        put("ml", item.memoryUnlockLevel)
                        put("n", item.note)
                    }
                )
            }
        }
    }

    private fun encodeVoiceEntries(items: List<BaGuideVoiceEntry>): JSONArray {
        return JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("s", item.section)
                        put("t", item.title)
                        put("a", item.audioUrl)
                        put(
                            "hs",
                            JSONArray().apply {
                                item.lineHeaders.forEach { header ->
                                    put(header.trim())
                                }
                            }
                        )
                        put(
                            "ls",
                            JSONArray().apply {
                                item.lines.forEach { line ->
                                    // 保留空字符串占位，避免多语言列在缓存后发生错位。
                                    put(line.trim())
                                }
                            }
                        )
                        put(
                            "aus",
                            JSONArray().apply {
                                item.audioUrls.forEach { url ->
                                    put(url.trim())
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    private fun encodeStringMap(map: Map<String, String>): JSONObject {
        return JSONObject().apply {
            map.forEach { (key, value) ->
                val normalizedKey = key.trim()
                val normalizedValue = value.trim()
                if (normalizedKey.isNotBlank() && normalizedValue.isNotBlank()) {
                    put(normalizedKey, normalizedValue)
                }
            }
        }
    }

    private fun decodeGalleryItems(obj: JSONObject, key: String): List<BaGuideGalleryItem> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val title = item.optString("t").trim()
                val imageUrl = item.optString("img").trim()
                val mediaType = item.optString("mt").trim().ifBlank { "image" }
                val mediaUrl = item.optString("mu").trim().ifBlank { imageUrl }
                val memoryUnlockLevel = item.optString("ml").trim()
                val note = item.optString("n").trim()
                if (imageUrl.isBlank() && mediaUrl.isBlank() && memoryUnlockLevel.isBlank() && note.isBlank()) continue
                add(
                    BaGuideGalleryItem(
                        title = title,
                        imageUrl = imageUrl,
                        mediaType = mediaType,
                        mediaUrl = mediaUrl,
                        memoryUnlockLevel = memoryUnlockLevel,
                        note = note
                    )
                )
            }
        }
    }

    private fun decodeVoiceEntries(obj: JSONObject, key: String): List<BaGuideVoiceEntry> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val section = item.optString("s").trim()
                val title = item.optString("t").trim()
                val audioUrl = item.optString("a").trim()
                val lineHeaders = buildList {
                    val headersArray = item.optJSONArray("hs")
                    if (headersArray != null) {
                        for (j in 0 until headersArray.length()) {
                            add(headersArray.optString(j).trim())
                        }
                    }
                }
                val lines = buildList {
                    val lineArray = item.optJSONArray("ls")
                    if (lineArray != null) {
                        for (j in 0 until lineArray.length()) {
                            val line = lineArray.optString(j).trim()
                            add(line)
                        }
                    }
                }
                val audioUrls = buildList {
                    val audioArray = item.optJSONArray("aus")
                    if (audioArray != null) {
                        for (j in 0 until audioArray.length()) {
                            add(audioArray.optString(j).trim())
                        }
                    }
                }.ifEmpty {
                    if (audioUrl.isNotBlank()) listOf(audioUrl) else emptyList()
                }
                if (
                    section.isBlank() &&
                    title.isBlank() &&
                    audioUrl.isBlank() &&
                    lines.isEmpty() &&
                    audioUrls.isEmpty()
                ) continue
                add(
                    BaGuideVoiceEntry(
                        section = section,
                        title = title,
                        lineHeaders = lineHeaders,
                        lines = lines,
                        audioUrls = audioUrls,
                        audioUrl = audioUrl
                    )
                )
            }
        }
    }

    private fun decodeVoiceCvByLanguage(
        obj: JSONObject,
        fallbackJp: String,
        fallbackCn: String
    ): Map<String, String> {
        val map = linkedMapOf<String, String>()
        val mapObj = obj.optJSONObject("voiceCvByLanguage")
        if (mapObj != null) {
            val keys = mapObj.keys()
            while (keys.hasNext()) {
                val key = keys.next().trim()
                if (key.isBlank()) continue
                val value = mapObj.optString(key).trim()
                if (value.isNotBlank()) {
                    map[key] = value
                }
            }
        }
        if (map.isEmpty()) {
            if (fallbackJp.isNotBlank()) map["日配"] = fallbackJp
            if (fallbackCn.isNotBlank()) map["中配"] = fallbackCn
        }
        return map
    }

    fun setCurrentUrl(url: String) {
        kv().encode(BA_GUIDE_KEY_CURRENT_URL, url.trim())
    }

    fun loadCurrentUrl(): String = kv().decodeString(BA_GUIDE_KEY_CURRENT_URL, "").orEmpty()

    private fun cacheKey(url: String): String {
        val id = url.trim().hashCode().toUInt().toString(16)
        return BA_GUIDE_KEY_CACHE_PREFIX + id
    }

    fun saveInfo(info: BaStudentGuideInfo) {
        val statsArr = JSONArray().apply {
            info.stats.forEach { (k, v) ->
                put(
                    JSONObject().apply {
                        put("k", k)
                        put("v", v)
                    }
                )
            }
        }
        val raw = JSONObject().apply {
            put("sourceUrl", info.sourceUrl)
            put("title", info.title)
            put("subtitle", info.subtitle)
            put("description", info.description)
            put("imageUrl", info.imageUrl)
            put("summary", info.summary)
            put("syncedAtMs", info.syncedAtMs)
            put("stats", statsArr)
            put("skillRows", encodeGuideRows(info.skillRows))
            put("profileRows", encodeGuideRows(info.profileRows))
            put("galleryItems", encodeGalleryItems(info.galleryItems))
            put("growthRows", encodeGuideRows(info.growthRows))
            put("simulateRows", encodeGuideRows(info.simulateRows))
            put("voiceRows", encodeGuideRows(info.voiceRows))
            put("voiceCvJp", info.voiceCvJp)
            put("voiceCvCn", info.voiceCvCn)
            put("voiceCvByLanguage", encodeStringMap(info.voiceCvByLanguage))
            put("voiceLanguageHeaders", JSONArray().apply { info.voiceLanguageHeaders.forEach { put(it) } })
            put("voiceEntries", encodeVoiceEntries(info.voiceEntries))
            put("tabSkillIconUrl", info.tabSkillIconUrl)
            put("tabProfileIconUrl", info.tabProfileIconUrl)
            put("tabVoiceIconUrl", info.tabVoiceIconUrl)
            put("tabGalleryIconUrl", info.tabGalleryIconUrl)
            put("tabSimulateIconUrl", info.tabSimulateIconUrl)
        }.toString()
        kv().encode(cacheKey(info.sourceUrl), raw)
    }

    fun cachedEntryCount(): Int {
        val store = kv()
        return store.allKeys().orEmpty().count { it.startsWith(BA_GUIDE_KEY_CACHE_PREFIX) }
    }

    fun clearAllCachedInfo() {
        val store = kv()
        store.allKeys()
            .orEmpty()
            .filter { it.startsWith(BA_GUIDE_KEY_CACHE_PREFIX) }
            .forEach(store::removeValueForKey)
        store.trim()
    }

    fun storageFootprintBytes(): Long = kv().totalSize()

    fun actualDataBytes(): Long = kv().actualSize()

    fun cacheBytesEstimated(): Long {
        val store = kv()
        return store.allKeys()
            .orEmpty()
            .filter { it.startsWith(BA_GUIDE_KEY_CACHE_PREFIX) }
            .sumOf { key -> store.decodeString(key, "").orEmpty().length.toLong() * 2 + 16L }
    }

    fun configBytesEstimated(): Long = loadCurrentUrl().length.toLong() * 2 + 16L

    fun latestSyncedAtMs(): Long {
        val store = kv()
        return store.allKeys()
            .orEmpty()
            .filter { it.startsWith(BA_GUIDE_KEY_CACHE_PREFIX) }
            .mapNotNull { key ->
                runCatching {
                    JSONObject(store.decodeString(key, "").orEmpty()).optLong("syncedAtMs", 0L)
                }.getOrNull()
            }
            .maxOrNull()
            ?: 0L
    }

    fun loadInfo(url: String): BaStudentGuideInfo? {
        val source = url.trim()
        if (source.isBlank()) return null
        val raw = kv().decodeString(cacheKey(source), "").orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val stats = mutableListOf<Pair<String, String>>()
            val arr = obj.optJSONArray("stats") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val k = row.optString("k").trim()
                val v = row.optString("v").trim()
                if (k.isNotBlank() && v.isNotBlank()) stats += k to v
            }
            val voiceCvJp = obj.optString("voiceCvJp").trim()
            val voiceCvCn = obj.optString("voiceCvCn").trim()
            val voiceCvByLanguage = decodeVoiceCvByLanguage(
                obj = obj,
                fallbackJp = voiceCvJp,
                fallbackCn = voiceCvCn
            )
            BaStudentGuideInfo(
                sourceUrl = obj.optString("sourceUrl").ifBlank { source },
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
                voiceLanguageHeaders = buildList {
                    val headers = obj.optJSONArray("voiceLanguageHeaders")
                    if (headers != null) {
                        for (j in 0 until headers.length()) {
                            val value = headers.optString(j).trim()
                            if (value.isNotBlank()) add(value)
                        }
                    }
                },
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
}
