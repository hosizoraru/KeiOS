package com.example.keios.ui.page.main.student

import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import org.json.JSONArray
import org.json.JSONObject

internal fun extractImageUrlsFromHtml(sourceUrl: String, raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val regex = Regex(
        """(?i)<img[^>]+src\s*=\s*["']([^"']+)["'][^>]*>"""
    )
    return regex.findAll(raw)
        .mapNotNull { match ->
            normalizeImageUrl(sourceUrl, match.groupValues.getOrNull(1).orEmpty())
        }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

internal fun normalizeMediaUrl(sourceUrl: String, mediaRaw: String): String {
    return normalizeImageUrl(sourceUrl, mediaRaw)
}

internal fun extractImageUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 6) return emptyList()
    return when (any) {
        is String -> {
            val normalized = normalizeImageUrl(sourceUrl, any)
            buildList {
                if (looksLikeImageUrl(normalized)) add(normalized)
                addAll(
                    any.split(",", ";")
                        .map { normalizeImageUrl(sourceUrl, it.trim()) }
                        .filter { looksLikeImageUrl(it) }
                )
                addAll(extractImageUrlsFromHtml(sourceUrl, any))
            }.distinct()
        }

        is JSONObject -> {
            val result = mutableListOf<String>()
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractImageUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractImageUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

internal fun extractVideoUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 6) return emptyList()
    return when (any) {
        is String -> {
            buildList {
                val normalized = normalizeMediaUrl(sourceUrl, any)
                if (looksLikeVideoUrl(normalized)) add(normalized)
                any.split(",", ";")
                    .map { normalizeMediaUrl(sourceUrl, it.trim()) }
                    .filter { looksLikeVideoUrl(it) }
                    .forEach { add(it) }
            }.distinct()
        }

        is JSONObject -> {
            val result = mutableListOf<String>()
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractVideoUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractVideoUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

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
            continue
        }
        val giftImages = when {
            isContinuationRow -> rowGenericImages.distinct()
            explicitGiftImages.isNotEmpty() -> (explicitGiftImages + rowGenericImages).distinct()
            hasGiftIconKey && rowGenericImages.isNotEmpty() -> rowGenericImages.distinct()
            else -> keyGenericImages.distinct()
        }
        if (giftImages.isEmpty()) continue

        val emojiImages = when {
            isContinuationRow -> continuationEmojiImages
            explicitEmojiImages.isNotEmpty() -> explicitEmojiImages
            keyGenericImages.isNotEmpty() -> keyGenericImages.filterNot { it in giftImages }.ifEmpty {
                keyGenericImages.take(1)
            }
            else -> emptyList()
        }
        val note = rowTextNotes.firstOrNull { text ->
            text.isNotBlank() && !looksLikeImageUrl(normalizeImageUrl(sourceUrl, text))
        }.orEmpty().ifBlank {
            if (isContinuationRow) continuationNote else ""
        }

        giftImages.forEachIndexed { index, giftImage ->
            val emojiImage = when {
                emojiImages.isEmpty() -> ""
                emojiImages.size == giftImages.size -> emojiImages.getOrElse(index) { emojiImages.first() }
                else -> emojiImages.first()
            }
            val imageUrls = buildList {
                add(giftImage)
                if (emojiImage.isNotBlank() && emojiImage != giftImage) {
                    add(emojiImage)
                }
            }
            out += BaGuideRow(
                key = "礼物偏好礼物$giftIndex",
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

internal val simulateSectionHeaders = listOf(
    "初始数据",
    "顶级数据",
    "专武",
    "装备",
    "爱用品",
    "能力解放",
    "羁绊等级奖励"
)

internal fun resolveSimulateSectionHeader(rawKey: String): String? {
    val key = normalizeGuideRowKey(rawKey)
    return simulateSectionHeaders.firstOrNull { header ->
        key == normalizeGuideRowKey(header)
    }
}

internal data class SimulateSupplementIcons(
    val weaponIcon: String = "",
    val favorIcon: String = "",
    val equipmentSlotIcons: Map<String, String> = emptyMap(),
    val unlockMaterialIcons: List<String> = emptyList()
)

internal fun collectSimulateSupplementIcons(
    baseData: JSONArray,
    sourceUrl: String
): SimulateSupplementIcons {
    fun extractRowImages(row: JSONArray): List<String> {
        val out = linkedSetOf<String>()
        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val rawValueAny = cell.opt("value")
            val rawValue = cell.optString("value").trim()
            if (rawValue.isBlank()) continue
            when (type) {
                "image" -> {
                    if (isPlaceholderMediaToken(rawValue)) continue
                    val normalized = normalizeImageUrl(sourceUrl, rawValue)
                    if (looksLikeImageUrl(normalized)) {
                        out += normalized
                    }
                }

                "imageset", "live2d" -> {
                    out += extractImageUrlsFromAny(sourceUrl, rawValueAny)
                }

                else -> {
                    out += extractImageUrlsFromHtml(sourceUrl, rawValue)
                    out += extractImageUrlsFromAny(sourceUrl, rawValueAny)
                }
            }
        }
        return out
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    var weaponIcon = ""
    var favorIcon = ""
    val equipmentSlotIcons = linkedMapOf<String, String>()
    var unlockMaterialIcons: List<String> = emptyList()

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        val normalizedKey = normalizeGuideRowKey(key)
        val images = extractRowImages(row)
        if (images.isEmpty()) continue

        when {
            normalizedKey == normalizeGuideRowKey("专武图标") -> {
                if (weaponIcon.isBlank()) {
                    weaponIcon = images.first()
                }
            }

            normalizedKey == normalizeGuideRowKey("爱用品图标") -> {
                if (favorIcon.isBlank()) {
                    favorIcon = images.first()
                }
            }

            Regex("""^装备([123])$""").matches(normalizedKey) -> {
                val slot = Regex("""^装备([123])$""").find(normalizedKey)?.groupValues?.getOrNull(1).orEmpty()
                if (slot.isNotBlank() && images.firstOrNull().orEmpty().isNotBlank()) {
                    equipmentSlotIcons["${slot}号装备"] = images.first()
                }
            }

            normalizedKey == normalizeGuideRowKey("能力解放所需材料") -> {
                unlockMaterialIcons = images
            }
        }
    }

    return SimulateSupplementIcons(
        weaponIcon = weaponIcon,
        favorIcon = favorIcon,
        equipmentSlotIcons = equipmentSlotIcons,
        unlockMaterialIcons = unlockMaterialIcons
    )
}

internal fun parseSimulateRowsFromBaseData(
    baseData: JSONArray,
    sourceUrl: String
): List<BaGuideRow> {
    if (baseData.length() == 0) return emptyList()

    fun rowToGuideRow(row: JSONArray): BaGuideRow {
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        val textValues = mutableListOf<String>()
        val imageValues = linkedSetOf<String>()
        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val rawValueAny = cell.opt("value")
            val rawValue = cell.optString("value").trim()
            if (rawValue.isBlank()) continue

            when (type) {
                "image" -> {
                    if (isPlaceholderMediaToken(rawValue)) continue
                    val normalized = normalizeImageUrl(sourceUrl, rawValue)
                    if (looksLikeImageUrl(normalized)) {
                        imageValues += normalized
                    }
                }

                "imageset", "live2d" -> {
                    imageValues += extractImageUrlsFromAny(sourceUrl, rawValueAny)
                }

                else -> {
                    imageValues += extractImageUrlsFromHtml(sourceUrl, rawValue)
                    imageValues += extractImageUrlsFromAny(sourceUrl, rawValueAny)
                    val text = stripHtml(rawValue)
                    if (text.isNotBlank()) {
                        textValues += text
                    }
                }
            }
        }
        val dedupImages = imageValues
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return BaGuideRow(
            key = key,
            value = textValues.joinToString(" / ").trim(),
            imageUrl = dedupImages.firstOrNull().orEmpty(),
            imageUrls = dedupImages
        )
    }

    var startIndex = -1
    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (resolveSimulateSectionHeader(key) != "初始数据") continue
        val hasTopData = (i + 1 until minOf(baseData.length(), i + 24)).any { next ->
            val nextRow = baseData.optJSONArray(next) ?: return@any false
            val nextKey = stripHtml((nextRow.optJSONObject(0)?.optString("value") ?: "").trim())
            resolveSimulateSectionHeader(nextKey) == "顶级数据"
        }
        if (hasTopData) {
            startIndex = i
        }
    }
    if (startIndex < 0) return emptyList()

    val stopKeys = listOf(
        "学生信息", "介绍", "配音语言", "配音", "配音大类", "官方介绍", "角色表情",
        "立绘", "本家画", "设定集", "TV动画设定图", "礼物偏好", "技能类型", "技能名词"
    ).map(::normalizeGuideRowKey)

    val out = mutableListOf<BaGuideRow>()
    var inSimulateBlock = false
    var seenBondRewardSection = false
    var trailingEmptyRows = 0

    for (i in startIndex until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val guideRow = rowToGuideRow(row)
        val normalizedKey = normalizeGuideRowKey(guideRow.key)
        val header = resolveSimulateSectionHeader(guideRow.key)

        if (header != null) {
            inSimulateBlock = true
            trailingEmptyRows = 0
            if (header == "羁绊等级奖励") {
                seenBondRewardSection = true
            }
            out += guideRow.copy(key = header)
            continue
        }
        if (!inSimulateBlock) continue
        if (normalizedKey.isNotBlank() && normalizedKey in stopKeys) {
            break
        }

        val hasRenderableContent =
            guideRow.key.trim().isNotBlank() ||
                guideRow.value.trim().isNotBlank() ||
                guideRow.imageUrls.isNotEmpty() ||
                guideRow.imageUrl.trim().isNotBlank()
        if (!hasRenderableContent) {
            if (seenBondRewardSection) {
                trailingEmptyRows += 1
            }
            if (trailingEmptyRows >= 2 && seenBondRewardSection) {
                break
            }
            continue
        }
        trailingEmptyRows = 0
        out += guideRow
    }

    val supplementIcons = collectSimulateSupplementIcons(baseData, sourceUrl)
    val patchedRows = mutableListOf<BaGuideRow>()
    var currentSection = ""
    var currentEquipmentSlot = ""
    var hasAppliedWeaponIcon = false
    var hasAppliedFavorIcon = false

    out.forEach { row ->
        val sectionHeader = resolveSimulateSectionHeader(row.key)
        if (sectionHeader != null) {
            currentSection = sectionHeader
            currentEquipmentSlot = ""
            patchedRows += row
            return@forEach
        }

        var patched = row
        when (currentSection) {
            "专武" -> {
                if (!hasAppliedWeaponIcon && patched.imageUrl.isBlank() && supplementIcons.weaponIcon.isNotBlank()) {
                    patched = patched.copy(
                        imageUrl = supplementIcons.weaponIcon,
                        imageUrls = listOf(supplementIcons.weaponIcon)
                    )
                    hasAppliedWeaponIcon = true
                }
            }

            "装备" -> {
                val normalizedKey = normalizeGuideRowKey(patched.key)
                val slot = Regex("""^([123])号装备$""").find(normalizedKey)?.groupValues?.getOrNull(1).orEmpty()
                if (slot.isNotBlank()) {
                    currentEquipmentSlot = "${slot}号装备"
                }
                val slotIcon = supplementIcons.equipmentSlotIcons[currentEquipmentSlot].orEmpty()
                if (slotIcon.isNotBlank() && patched.imageUrl.isBlank()) {
                    val keyAsMediaUrl = normalizeMediaUrl(sourceUrl, patched.key)
                    val keyLooksLikeMedia = looksLikeImageUrl(keyAsMediaUrl) || looksLikeVideoUrl(keyAsMediaUrl)
                    val shouldAttachSlotIcon =
                        (slot.isNotBlank() || !isTopDataStatKey(patched.key)) &&
                            !keyLooksLikeMedia
                    if (shouldAttachSlotIcon) {
                        patched = patched.copy(
                            imageUrl = slotIcon,
                            imageUrls = listOf(slotIcon)
                        )
                    }
                }
            }

            "爱用品" -> {
                if (!hasAppliedFavorIcon && patched.imageUrl.isBlank() && supplementIcons.favorIcon.isNotBlank()) {
                    patched = patched.copy(
                        imageUrl = supplementIcons.favorIcon,
                        imageUrls = listOf(supplementIcons.favorIcon)
                    )
                    hasAppliedFavorIcon = true
                }
            }

            "能力解放" -> {
                val normalizedKey = normalizeGuideRowKey(patched.key)
                if (
                    Regex("""^\d+级$""").matches(normalizedKey) &&
                    patched.imageUrl.isBlank() &&
                    patched.imageUrls.isEmpty() &&
                    supplementIcons.unlockMaterialIcons.isNotEmpty()
                ) {
                    patched = patched.copy(
                        imageUrl = supplementIcons.unlockMaterialIcons.first(),
                        imageUrls = supplementIcons.unlockMaterialIcons
                    )
                }
            }
        }

        patchedRows += patched
    }

    return patchedRows
        .map { row ->
            row.copy(
                key = row.key.trim(),
                value = row.value.trim(),
                imageUrl = row.imageUrl.trim(),
                imageUrls = row.imageUrls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            )
        }
        .filterNot { row ->
            row.key.isBlank() && row.value.isBlank() && row.imageUrls.isEmpty() && row.imageUrl.isBlank()
        }
        .take(260)
}

internal fun parseGalleryItemsFromBaseData(baseData: JSONArray, sourceUrl: String): List<BaGuideGalleryItem> {
    val out = mutableListOf<BaGuideGalleryItem>()
    val galleryKeywords = listOf(
        "立绘", "本家画", "TV动画设定图", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
        "互动家具", "角色表情", "表情", "角色演示", "设定集", "官方介绍", "官方衍生", "情人节巧克力", "BGM"
    )
    val galleryContextStartKeywords = galleryKeywords + listOf("视频")
    val nonGallerySectionKeywords = listOf(
        "技能", "技能类型", "技能名词", "EX技能升级材料", "其他技能升级材料",
        "专武", "爱用品", "能力解放", "礼物偏好", "初始数据", "顶级数据",
        "学生信息", "介绍", "配音"
    )
    val nonGalleryFallbackKeywords = listOf(
        "头像", "技能", "图标", "语音", "台词", "专武", "武器", "装备", "材料",
        "能力解放", "礼物偏好", "初始数据", "学生信息", "角色名称", "稀有度", "所属学园", "所属社团",
        "战术作用", "攻击类型", "防御类型", "位置", "武器类型", "市街", "屋外", "屋内", "室内"
    )

    fun noteForImageIndex(texts: List<String>, index: Int, imageCount: Int): String {
        val normalized = texts.map { it.trim() }.filter { it.isNotBlank() }
        if (normalized.isEmpty()) return ""
        if (imageCount <= 1) return normalized.joinToString(" / ")
        if (normalized.size == imageCount) return normalized.getOrElse(index) { "" }
        if (normalized.size == 1) return if (index == imageCount - 1) normalized.first() else ""
        return normalized.getOrElse(index) { normalized.last() }
    }

    fun isGalleryKey(raw: String): Boolean {
        val key = stripHtml(raw).trim()
        if (key.isBlank()) return false
        return galleryKeywords.any { key.contains(it, ignoreCase = true) }
    }

    val memoryUnlockLevel = run {
        var level = ""
        for (i in 0 until baseData.length()) {
            val row = baseData.optJSONArray(i) ?: continue
            if (row.length() == 0) continue
            val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
            if (key != "回忆大厅解锁等级") continue
            val value = buildString {
                for (j in 1 until row.length()) {
                    val cell = row.optJSONObject(j) ?: continue
                    val text = stripHtml(cell.optString("value"))
                    if (text.isNotBlank()) {
                        append(text)
                        break
                    }
                }
            }
            val digits = Regex("""\d+""").find(value)?.value.orEmpty()
            level = if (digits.isNotBlank()) digits else value
            break
        }
        level
    }

    var inGalleryContext = false
    var lastGalleryTitle = ""
    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (key == "回忆大厅解锁等级") continue
        if (key.replace(" ", "").startsWith("回忆大厅文件")) continue
        val isGalleryContextStart = galleryContextStartKeywords.any { key.contains(it, ignoreCase = true) }
        val isNonGallerySectionStart = key.isNotBlank() && nonGallerySectionKeywords.any {
            key.contains(it, ignoreCase = true)
        }
        if (isNonGallerySectionStart && !isGalleryContextStart) {
            inGalleryContext = false
            lastGalleryTitle = ""
        }
        if (isGalleryContextStart) {
            inGalleryContext = true
            if (key.isNotBlank()) {
                lastGalleryTitle = key
            }
        }

        val rowImages = linkedSetOf<String>()
        val rowVideos = linkedSetOf<String>()
        val rowAudios = linkedSetOf<String>()
        val rowTexts = mutableListOf<String>()

        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val valueAny = cell.opt("value")
            val valueText = cell.optString("value").trim()

            when (type) {
                "image" -> {
                    if (isPlaceholderMediaToken(valueText)) continue
                    val normalized = normalizeImageUrl(sourceUrl, valueText)
                    if (looksLikeImageUrl(normalized)) rowImages += normalized
                }
                "imageset", "live2d" -> {
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                }
                "video" -> {
                    val direct = normalizeMediaUrl(sourceUrl, valueText)
                    if (looksLikeVideoUrl(direct)) rowVideos += direct
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                }
                "audio" -> {
                    val direct = normalizeMediaUrl(sourceUrl, valueText)
                    if (isAudioUrl(direct)) rowAudios += direct
                    rowAudios += extractAudioUrlsFromRaw(sourceUrl, valueText)
                }
                else -> {
                    rowImages += extractImageUrlsFromHtml(sourceUrl, valueText)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                    rowAudios += extractAudioUrlsFromRaw(sourceUrl, valueText)
                    val plain = stripHtml(valueText)
                    if (plain.isNotBlank()) rowTexts += plain
                }
            }
        }

        val hasMedia = rowImages.isNotEmpty() || rowVideos.isNotEmpty() || rowAudios.isNotEmpty()
        val isFallbackGallery =
            hasMedia &&
                inGalleryContext &&
                nonGalleryFallbackKeywords.none { key.contains(it, ignoreCase = true) }
        if (!isGalleryKey(key) && !isFallbackGallery) continue
        val galleryTitle = key.ifBlank { lastGalleryTitle.ifBlank { "影画" } }

        if (rowImages.isNotEmpty()) {
            out += rowImages.mapIndexed { index, imageUrl ->
                BaGuideGalleryItem(
                    title = if (rowImages.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = imageUrl,
                    mediaType = "image",
                    mediaUrl = imageUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = noteForImageIndex(rowTexts, index, rowImages.size)
                )
            }
        }
        if (rowVideos.isNotEmpty()) {
            val videoNote = rowTexts.joinToString(" / ").trim()
            out += rowVideos.mapIndexed { index, videoUrl ->
                BaGuideGalleryItem(
                    title = if (rowVideos.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "video",
                    mediaUrl = videoUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = videoNote
                )
            }
        }
        if (rowAudios.isNotEmpty()) {
            val audioNote = rowTexts.joinToString(" / ").trim()
            out += rowAudios.mapIndexed { index, audioUrl ->
                BaGuideGalleryItem(
                    title = if (rowAudios.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "audio",
                    mediaUrl = audioUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = audioNote
                )
            }
        }
    }

    return out
}

internal fun parseGalleryItemsFromStyleData(styleData: JSONArray?, sourceUrl: String): List<BaGuideGalleryItem> {
    if (styleData == null || styleData.length() == 0) return emptyList()
    val out = mutableListOf<BaGuideGalleryItem>()
    for (i in 0 until styleData.length()) {
        val block = styleData.optJSONObject(i) ?: continue
        val blockName = stripHtml(block.optString("name"))
            .ifBlank { "样式 ${i + 1}" }
        val blockData = block.opt("data") ?: continue
        val imageUrls = extractImageUrlsFromAny(sourceUrl, blockData)
            .filter { it.isNotBlank() }
            .distinct()
        val videoUrls = extractVideoUrlsFromAny(sourceUrl, blockData)
            .filter { it.isNotBlank() }
            .distinct()
        if (imageUrls.isEmpty() && videoUrls.isEmpty()) continue

        imageUrls.forEachIndexed { index, imageUrl ->
            out += BaGuideGalleryItem(
                title = if (imageUrls.size > 1) "$blockName ${index + 1}" else blockName,
                imageUrl = imageUrl,
                mediaType = "image",
                mediaUrl = imageUrl
            )
        }
        videoUrls.forEachIndexed { index, videoUrl ->
            out += BaGuideGalleryItem(
                title = if (videoUrls.size > 1) "$blockName ${index + 1}" else blockName,
                imageUrl = imageUrls.firstOrNull().orEmpty(),
                mediaType = "video",
                mediaUrl = videoUrl
            )
        }
    }
    return out
}

internal fun isAudioUrl(url: String): Boolean {
    val value = url.trim().lowercase()
    if (value.isBlank()) return false
    return Regex("""\.(mp3|ogg|wav|m4a|aac)(\?.*)?$""").containsMatchIn(value)
}

internal fun extractAudioUrlsFromRaw(sourceUrl: String, raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val regex = Regex(
        """(?i)((?:https?:)?//[^\s"'<>]+|/[^\s"'<>]+|[^\s"'<>]+\.(?:mp3|ogg|wav|m4a|aac)(?:\?[^\s"'<>]*)?)"""
    )
    return regex.findAll(raw)
        .mapNotNull { match ->
            normalizeMediaUrl(sourceUrl, match.groupValues.getOrNull(1).orEmpty())
        }
        .filter { it.isNotBlank() && isAudioUrl(it) }
        .distinct()
        .toList()
}

internal fun extractAudioUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 6) return emptyList()
    return when (any) {
        is String -> extractAudioUrlsFromRaw(sourceUrl, any)

        is JSONObject -> {
            val result = mutableListOf<String>()
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractAudioUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractAudioUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

internal fun looksLikeBinaryMediaUrl(raw: String): Boolean {
    val value = raw.trim().lowercase()
    if (value.isBlank()) return false
    return Regex("""\.(png|jpg|jpeg|webp|gif|bmp|svg|avif|mp4|webm|mov|m3u8|mp3|ogg|wav|m4a|aac|zip|rar|7z|apk)(\?.*)?(#.*)?$""")
        .containsMatchIn(value)
}

internal fun normalizeWebUrlCandidate(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val normalized = when {
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> normalizeGuideUrl(value)
        value.startsWith("www.", ignoreCase = true) -> "https://$value"
        else -> normalizeGuideUrl(value)
    }.trim()
    if (!normalized.startsWith("http://", ignoreCase = true) &&
        !normalized.startsWith("https://", ignoreCase = true)
    ) {
        return ""
    }
    if (looksLikeBinaryMediaUrl(normalized)) return ""
    val host = runCatching { Uri.parse(normalized).host.orEmpty().lowercase() }.getOrDefault("")
    if (host.isBlank()) return ""
    return normalized
}

internal fun extractWebUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 8) return emptyList()
    return when (any) {
        is String -> {
            val direct = normalizeWebUrlCandidate(any)
            val embedded = Regex("""https?://[^\s"'<>]+|//[^\s"'<>]+""", RegexOption.IGNORE_CASE)
                .findAll(any)
                .mapNotNull { match ->
                    normalizeWebUrlCandidate(match.value)
                }
                .toList()
            buildList {
                if (direct.isNotBlank()) add(direct)
                addAll(embedded)
            }.distinct()
        }

        is JSONObject -> {
            val result = mutableListOf<String>()
            val directKeys = listOf("url", "href", "jumpHref", "jump_url", "link")
            directKeys.forEach { key ->
                val normalized = normalizeWebUrlCandidate(any.optString(key))
                if (normalized.isNotBlank()) result += normalized
            }
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractWebUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractWebUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

