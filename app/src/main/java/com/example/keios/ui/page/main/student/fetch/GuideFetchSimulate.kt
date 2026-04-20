package com.example.keios.ui.page.main.student.fetch

import com.example.keios.ui.page.main.student.BaGuideRow
import org.json.JSONArray

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
                    val keyLooksLikeMedia = looksLikeImageUrl(keyAsMediaUrl) || looksLikeVideoUrl(
                        keyAsMediaUrl
                    )
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
