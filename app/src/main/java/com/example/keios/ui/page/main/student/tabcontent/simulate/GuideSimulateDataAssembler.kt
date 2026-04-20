package com.example.keios.ui.page.main.student.tabcontent.simulate

import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey
import com.example.keios.ui.page.main.student.tabcontent.profile.splitGuideCompositeValues

internal fun buildGuideSimulateCacheKey(rows: List<BaGuideRow>): String {
    var hash = 17
    rows.forEach { row ->
        hash = 31 * hash + normalizeProfileFieldKey(row.key).hashCode()
        hash = 31 * hash + row.value.trim().hashCode()
        hash = 31 * hash + row.imageUrl.trim().hashCode()
        row.imageUrls.forEach { image ->
            hash = 31 * hash + image.trim().hashCode()
        }
    }
    return "${rows.size}|$hash"
}

internal fun buildGuideSimulateData(rows: List<BaGuideRow>): GuideSimulateData {
    if (rows.isEmpty()) return GuideSimulateData()
    val cacheKey = buildGuideSimulateCacheKey(rows)
    synchronized(guideSimulateDataCache) {
        guideSimulateDataCache[cacheKey]?.let { return it }
    }
    val sections = linkedMapOf<String, MutableList<BaGuideRow>>()
    val hints = mutableMapOf<String, String>()
    var currentSection = ""

    rows.forEach { row ->
        val header = resolveSimulateSectionName(row.key)
        if (header != null) {
            currentSection = header
            sections.getOrPut(header) { mutableListOf() }
            val hint = row.value.trim().trim('*').trim()
            if (hint.isNotBlank()) {
                hints[header] = hint
            }
            return@forEach
        }
        if (currentSection.isBlank()) return@forEach

        val cleaned = row.copy(
            key = row.key.trim(),
            value = row.value.trim(),
            imageUrl = row.imageUrl.trim(),
            imageUrls = row.imageUrls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        )
        if (
            cleaned.key.isBlank() &&
            cleaned.value.isBlank() &&
            cleaned.imageUrl.isBlank() &&
            cleaned.imageUrls.isEmpty()
        ) return@forEach
        sections.getOrPut(currentSection) { mutableListOf() } += cleaned
    }

    val computed = GuideSimulateData(
        initialHint = hints["初始数据"].orEmpty(),
        initialRows = expandSimulateRows(sections["初始数据"].orEmpty()),
        maxHint = hints["顶级数据"].orEmpty(),
        maxRows = expandSimulateRows(sections["顶级数据"].orEmpty()),
        weaponHint = hints["专武"].orEmpty(),
        weaponRows = expandSimulateRows(sections["专武"].orEmpty()),
        equipmentHint = hints["装备"].orEmpty(),
        equipmentRows = expandSimulateRows(sections["装备"].orEmpty()),
        favorHint = hints["爱用品"].orEmpty(),
        favorRows = sanitizeSimulateFavorRows(
            expandSimulateRows(sections["爱用品"].orEmpty())
        ),
        unlockHint = hints["能力解放"].orEmpty(),
        unlockRows = expandSimulateRows(sections["能力解放"].orEmpty()),
        bondHint = hints["羁绊等级奖励"].orEmpty(),
        bondRows = sanitizeSimulateBondRows(
            expandSimulateRows(sections["羁绊等级奖励"].orEmpty())
        )
    )
    synchronized(guideSimulateDataCache) {
        guideSimulateDataCache[cacheKey] = computed
    }
    return computed
}

internal fun resolveSimulateSectionName(rawKey: String): String? {
    val normalized = normalizeProfileFieldKey(rawKey)
    return when {
        normalized == normalizeProfileFieldKey("初始数据") -> "初始数据"
        normalized == normalizeProfileFieldKey("顶级数据") -> "顶级数据"
        normalized == normalizeProfileFieldKey("专武") -> "专武"
        normalized == normalizeProfileFieldKey("装备") -> "装备"
        normalized == normalizeProfileFieldKey("爱用品") -> "爱用品"
        normalized == normalizeProfileFieldKey("能力解放") -> "能力解放"
        normalized == normalizeProfileFieldKey("羁绊等级奖励") -> "羁绊等级奖励"
        else -> null
    }
}

internal fun expandSimulateRows(rows: List<BaGuideRow>): List<BaGuideRow> {
    if (rows.isEmpty()) return emptyList()
    val expanded = mutableListOf<BaGuideRow>()
    rows.forEach { row ->
        val key = row.key.trim()
        val value = row.value.trim()
        val icon = row.imageUrl.trim().ifBlank { row.imageUrls.firstOrNull().orEmpty() }
        val images = row.imageUrls.ifEmpty { listOfNotNull(icon.takeIf { it.isNotBlank() }) }
        if (key.isBlank() && value.isBlank() && images.isEmpty()) return@forEach

        if (value.isBlank()) {
            if (key.isNotBlank() || icon.isNotBlank()) {
                expanded += BaGuideRow(
                    key = key.ifBlank { "信息" },
                    value = "",
                    imageUrl = icon,
                    imageUrls = images
                )
            }
            return@forEach
        }

        val tokens = splitGuideCompositeValues(value)
        if (tokens.isEmpty()) {
            expanded += BaGuideRow(
                key = key.ifBlank { "信息" },
                value = value,
                imageUrl = icon,
                imageUrls = images
            )
            return@forEach
        }

        val firstTokenLooksLikeStat = isLikelySimulateStatLabel(tokens.first())
        var index = 0
        if (!firstTokenLooksLikeStat) {
            expanded += BaGuideRow(
                key = key.ifBlank { "等级" },
                value = tokens.first().trim(),
                imageUrl = icon,
                imageUrls = images
            )
            index = 1
        } else if (key.isNotBlank() && !isLikelySimulateStatLabel(key) && !isSimulateSubHeader(key)) {
            expanded += BaGuideRow(
                key = key,
                value = "",
                imageUrl = icon,
                imageUrls = images
            )
        } else if (icon.isNotBlank() && key.isNotBlank()) {
            expanded += BaGuideRow(
                key = key,
                value = "",
                imageUrl = icon,
                imageUrls = images
            )
        }

        var pairIndex = 0
        while (index + 1 < tokens.size) {
            val statKey = tokens[index].trim()
            val statValue = tokens[index + 1].trim()
            if (statKey.isNotBlank() && statValue.isNotBlank()) {
                val pairIcon = if (images.size > 1) images.getOrNull(pairIndex).orEmpty() else ""
                expanded += BaGuideRow(
                    key = statKey,
                    value = statValue,
                    imageUrl = pairIcon,
                    imageUrls = listOfNotNull(pairIcon.takeIf { it.isNotBlank() })
                )
            }
            pairIndex += 1
            index += 2
        }
    }

    return expanded.distinctBy { row ->
        val packedImages = row.imageUrls.joinToString("|")
        "${normalizeProfileFieldKey(row.key)}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
    }
}
