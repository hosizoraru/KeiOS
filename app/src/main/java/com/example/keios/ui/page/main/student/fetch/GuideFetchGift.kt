package com.example.keios.ui.page.main.student.fetch

import com.example.keios.ui.page.main.student.BaGuideRow
import org.json.JSONArray
import org.json.JSONObject

internal fun extractGiftClassImageUrls(raw: String, classKeyword: String, sourceUrl: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val out = mutableListOf<String>()
    val imgTagRegex = Regex("""(?is)<img\b[^>]*>""")
    val classRegex = Regex("""(?i)\bclass\s*=\s*["']([^"']+)["']""")
    val srcRegex = Regex("""(?i)\bsrc\s*=\s*["']([^"']+)["']""")
    imgTagRegex.findAll(raw).forEach { match ->
        val tag = match.value
        val className = classRegex.find(tag)?.groupValues?.getOrNull(1).orEmpty()
        if (!className.contains(classKeyword, ignoreCase = true)) return@forEach
        val src = srcRegex.find(tag)?.groupValues?.getOrNull(1).orEmpty()
        val normalized = normalizeImageUrl(sourceUrl, src)
        if (looksLikeImageUrl(normalized)) {
            out += normalized
        }
    }
    return out.distinct()
}

internal fun extractGiftImageUrlsFromCell(
    cell: JSONObject?,
    sourceUrl: String
): List<String> {
    if (cell == null) return emptyList()
    val type = cell.optString("type").trim().lowercase()
    val valueAny = cell.opt("value")
    val valueText = cell.optString("value").trim()
    val images = mutableListOf<String>()
    when (type) {
        "image" -> {
            if (!isPlaceholderMediaToken(valueText)) {
                val normalized = normalizeImageUrl(sourceUrl, valueText)
                if (looksLikeImageUrl(normalized)) {
                    images += normalized
                }
            }
        }

        "imageset", "live2d" -> {
            images += extractImageUrlsFromAny(sourceUrl, valueAny)
        }

        else -> {
            images += extractImageUrlsFromAny(sourceUrl, valueAny)
            images += extractImageUrlsFromHtml(sourceUrl, valueText)
        }
    }
    return images
        .map { it.trim() }
        .filter { looksLikeImageUrl(it) }
        .distinct()
}

internal fun isLikelyGiftPreferenceIconUrl(url: String): Boolean {
    val value = url.trim()
    if (value.isBlank()) return false
    val sizeMatch = Regex("""/w_(\d{1,4})/h_(\d{1,4})/""")
        .find(value)
    val width = sizeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    val height = sizeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
    if (width != null && height != null) {
        if (width in 24..120 && height in 24..120) return true
    }
    return value.contains("gift", ignoreCase = true) &&
        !value.contains("furniture", ignoreCase = true) &&
        !value.contains("chocolate", ignoreCase = true)
}

internal fun parseGiftPreferenceRowsFromBaseData(
    baseData: JSONArray,
    sourceUrl: String
): List<BaGuideRow> {
    val out = mutableListOf<BaGuideRow>()
    val giftHeaderKey = normalizeGuideRowKey("礼物偏好")
    val giftTailKeys = setOf(
        "初始数据", "顶级数据", "学生信息", "介绍", "配音",
        "专武", "爱用品", "能力解放", "装备", "羁绊等级奖励",
        "技能类型", "技能名词", "EX技能升级材料", "其他技能升级材料"
    ).map(::normalizeGuideRowKey).toSet()
    val giftStopKeyFragments = listOf(
        "互动家具", "情人节巧克力", "巧克力图", "巧克力名称", "巧克力简介",
        "相关同名角色", "同名角色名称", "配音", "配音语言", "配音大类",
        "官方介绍", "角色表情", "回忆大厅", "立绘", "本家画", "设定集", "TV动画设定图"
    ).map(::normalizeGuideRowKey)
    var inGiftBlock = false
    var giftIndex = 1
    var continuationQuota = 0
    var continuationEmojiImages: List<String> = emptyList()
    var continuationNote = ""

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val keyCell = row.optJSONObject(0)
        val rawKey = keyCell?.optString("value").orEmpty().trim()
        val key = stripHtml(rawKey).trim()
        val normalizedKey = normalizeGuideRowKey(key)

        if (!inGiftBlock) {
            if (normalizedKey == giftHeaderKey) {
                inGiftBlock = true
            }
            continue
        }

        if (normalizedKey.isNotBlank() && normalizedKey in giftTailKeys) {
            inGiftBlock = false
            continuationQuota = 0
            continuationEmojiImages = emptyList()
            continuationNote = ""
            continue
        }
        if (normalizedKey.isNotBlank() && giftStopKeyFragments.any { fragment ->
                normalizedKey.contains(fragment)
            }
        ) {
            inGiftBlock = false
            continuationQuota = 0
            continuationEmojiImages = emptyList()
            continuationNote = ""
            continue
        }

        val keyGiftImages = extractGiftClassImageUrls(rawKey, "gift-img", sourceUrl)
        val keyEmojiImages = extractGiftClassImageUrls(rawKey, "gift-emoji", sourceUrl)
        val keyGenericImages = extractGiftImageUrlsFromCell(keyCell, sourceUrl)

        val rowGiftImages = mutableListOf<String>()
        val rowEmojiImages = mutableListOf<String>()
        val rowGenericImages = mutableListOf<String>()
        val rowTextNotes = mutableListOf<String>()
        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val rawValue = cell.optString("value").trim()
            if (rawValue.isNotBlank()) {
                rowGiftImages += extractGiftClassImageUrls(rawValue, "gift-img", sourceUrl)
                rowEmojiImages += extractGiftClassImageUrls(rawValue, "gift-emoji", sourceUrl)
                val note = stripHtml(rawValue)
                if (note.isNotBlank()) {
                    rowTextNotes += note
                }
            }
            rowGenericImages += extractGiftImageUrlsFromCell(cell, sourceUrl)
        }

        if (keyGiftImages.isEmpty() && rowGiftImages.isEmpty() && rowGenericImages.isEmpty()) {
            continue
        }

        val explicitGiftImages = (keyGiftImages + rowGiftImages).distinct()
        val explicitEmojiImages = (keyEmojiImages + rowEmojiImages).distinct()
        val hasGiftIconKey = keyGenericImages.any(::isLikelyGiftPreferenceIconUrl) || keyGiftImages.isNotEmpty()
        val hasStructuredGiftMedia = explicitGiftImages.isNotEmpty()
        val isContinuationRow = normalizedKey.isBlank() &&
            continuationQuota > 0 &&
            rowGenericImages.isNotEmpty() &&
            keyGiftImages.isEmpty() &&
            rowGiftImages.isEmpty()
        if (!hasGiftIconKey && !hasStructuredGiftMedia && !isContinuationRow) {
            if (normalizedKey.isNotBlank()) {
                continuationQuota = 0
                continuationEmojiImages = emptyList()
                continuationNote = ""
            } else if (continuationQuota > 0) {
                continuationQuota = (continuationQuota - 1).coerceAtLeast(0)
            }
            continue
        }

        val normalizedGiftImages = if (isContinuationRow) {
            rowGenericImages
                .filterNot(::isLikelyGiftPreferenceIconUrl)
                .filter { looksLikeImageUrl(it) }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        } else {
            if (explicitGiftImages.isNotEmpty()) {
                explicitGiftImages
            } else {
                rowGenericImages
                    .filterNot(::isLikelyGiftPreferenceIconUrl)
                    .filter { looksLikeImageUrl(it) }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }
        }

        if (normalizedGiftImages.isEmpty()) {
            if (isContinuationRow) {
                continuationQuota = (continuationQuota - 1).coerceAtLeast(0)
            }
            continue
        }

        val emojiImages = if (isContinuationRow) {
            continuationEmojiImages
        } else {
            explicitEmojiImages
                .ifEmpty {
                    rowGenericImages.filter(::isLikelyGiftPreferenceIconUrl)
                }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        val note = if (isContinuationRow) {
            continuationNote
        } else {
            rowTextNotes.joinToString(" / ").trim()
        }

        normalizedGiftImages.forEach { giftImage ->
            val imageUrls = buildList {
                add(giftImage)
                addAll(emojiImages)
            }.distinct()
            out += BaGuideRow(
                key = if (normalizedGiftImages.size > 1) "礼物偏好 ${giftIndex}" else "礼物偏好",
                value = note,
                imageUrl = giftImage,
                imageUrls = imageUrls
            )
            giftIndex += 1
        }

        if (isContinuationRow) {
            continuationQuota = (continuationQuota - 1).coerceAtLeast(0)
        } else if (hasGiftIconKey || hasStructuredGiftMedia) {
            continuationQuota = 3
            continuationEmojiImages = emojiImages
            continuationNote = note
        } else if (normalizedKey.isNotBlank()) {
            continuationQuota = 0
            continuationEmojiImages = emptyList()
            continuationNote = ""
        }
    }

    return out
        .distinctBy { row ->
            val packedImages = row.imageUrls.joinToString("|")
            "${row.key.trim()}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
        }
        .take(80)
}
