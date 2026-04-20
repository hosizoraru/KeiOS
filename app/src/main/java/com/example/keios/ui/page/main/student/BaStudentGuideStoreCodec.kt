package com.example.keios.ui.page.main.student

import org.json.JSONArray
import org.json.JSONObject

internal fun encodeGuideRows(rows: List<BaGuideRow>): JSONArray {
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

internal fun decodeGuideRowsFromArray(arr: JSONArray?): List<BaGuideRow> {
    arr ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val k = row.optString("k").trim()
            val v = row.optString("v").trim()
            val img = row.optString("img").trim()
            val imgs = buildList {
                val valueArr = row.optJSONArray("imgs")
                if (valueArr != null) {
                    for (j in 0 until valueArr.length()) {
                        val value = valueArr.optString(j).trim()
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

internal fun decodeGuideRows(obj: JSONObject, key: String): List<BaGuideRow> {
    return decodeGuideRowsFromArray(obj.optJSONArray(key))
}

internal fun encodeGalleryItems(items: List<BaGuideGalleryItem>): JSONArray {
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

internal fun decodeGalleryItemsFromArray(arr: JSONArray?): List<BaGuideGalleryItem> {
    arr ?: return emptyList()
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

internal fun decodeGalleryItems(obj: JSONObject, key: String): List<BaGuideGalleryItem> {
    return decodeGalleryItemsFromArray(obj.optJSONArray(key))
}

internal fun encodeVoiceEntries(items: List<BaGuideVoiceEntry>): JSONArray {
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

internal fun decodeVoiceEntriesFromArray(arr: JSONArray?): List<BaGuideVoiceEntry> {
    arr ?: return emptyList()
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
                        add(lineArray.optString(j).trim())
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

internal fun decodeVoiceEntries(obj: JSONObject, key: String): List<BaGuideVoiceEntry> {
    return decodeVoiceEntriesFromArray(obj.optJSONArray(key))
}

internal fun encodeStringMap(map: Map<String, String>): JSONObject {
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

internal fun decodeVoiceCvByLanguage(
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

internal fun encodeStats(stats: List<Pair<String, String>>): JSONArray {
    return JSONArray().apply {
        stats.forEach { (k, v) ->
            put(
                JSONObject().apply {
                    put("k", k)
                    put("v", v)
                }
            )
        }
    }
}

internal fun decodeStats(arr: JSONArray?): List<Pair<String, String>> {
    arr ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val k = row.optString("k").trim()
            val v = row.optString("v").trim()
            if (k.isNotBlank() && v.isNotBlank()) add(k to v)
        }
    }
}

internal fun decodeHeaders(arr: JSONArray?): List<String> {
    arr ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val value = arr.optString(i).trim()
            if (value.isNotBlank()) add(value)
        }
    }
}

internal fun parseJsonArray(raw: String): JSONArray? {
    if (raw.isBlank()) return null
    return runCatching { JSONArray(raw) }.getOrNull()
}
