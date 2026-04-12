package com.example.keios.ui.page.main.student

import com.tencent.mmkv.MMKV
import org.json.JSONArray
import org.json.JSONObject

private const val BA_GUIDE_KV_ID = "ba_student_guide"
private const val BA_GUIDE_KEY_CURRENT_URL = "current_url"
private const val BA_GUIDE_KEY_CACHE_PREFIX = "cache_"

object BaStudentGuideStore {
    private fun kv(): MMKV = MMKV.mmkvWithID(BA_GUIDE_KV_ID)

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
                            "ls",
                            JSONArray().apply {
                                item.lines.forEach { line ->
                                    if (line.isNotBlank()) put(line)
                                }
                            }
                        )
                    }
                )
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
                if (imageUrl.isBlank() && mediaUrl.isBlank() && memoryUnlockLevel.isBlank()) continue
                add(
                    BaGuideGalleryItem(
                        title = title,
                        imageUrl = imageUrl,
                        mediaType = mediaType,
                        mediaUrl = mediaUrl,
                        memoryUnlockLevel = memoryUnlockLevel
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
                val lines = buildList {
                    val lineArray = item.optJSONArray("ls")
                    if (lineArray != null) {
                        for (j in 0 until lineArray.length()) {
                            val line = lineArray.optString(j).trim()
                            if (line.isNotBlank()) add(line)
                        }
                    }
                }
                if (section.isBlank() && title.isBlank() && audioUrl.isBlank() && lines.isEmpty()) continue
                add(
                    BaGuideVoiceEntry(
                        section = section,
                        title = title,
                        lines = lines,
                        audioUrl = audioUrl
                    )
                )
            }
        }
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
            put("voiceRows", encodeGuideRows(info.voiceRows))
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
                voiceRows = decodeGuideRows(obj, "voiceRows"),
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
