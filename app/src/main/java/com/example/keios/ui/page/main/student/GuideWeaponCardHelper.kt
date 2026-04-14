package com.example.keios.ui.page.main.student

fun BaStudentGuideInfo.weaponCardForDisplay(): GuideWeaponCardModel? {
    val growthRows = growthRowsForDisplay()
    val skillRows = skillRowsForDisplay()
    val allRowsForExtraStats = buildList {
        addAll(growthRows)
        addAll(skillRows)
        addAll(profileRowsForDisplay())
    }
    val rows = if (growthRows.any { it.key.trim() == "专武" }) {
        growthRows
    } else {
        buildList {
            addAll(growthRows)
            addAll(skillRows)
        }
    }
    if (rows.isEmpty()) return null

    val weaponStart = rows.indexOfFirst { it.key.trim() == "专武" }
    if (weaponStart < 0) return null

    val glossaryIcons = extractSkillGlossaryIcons(
        buildList {
            addAll(skillRows)
            addAll(growthRows)
        }
    )
    var weaponName = ""
    var weaponImage = ""
    var weaponDescription = ""
    var statHeaders: List<String> = emptyList()
    val statRows = mutableListOf<GuideWeaponStatRow>()

    val starDrafts = mutableListOf<WeaponStarDraft>()
    var currentStar: WeaponStarDraft? = null

    fun commitStarDraft() {
        val star = currentStar ?: return
        val hasDesc = star.descriptionByLevel.isNotEmpty() || star.fallbackDescription.isNotBlank()
        val hasHead = star.name.isNotBlank() || star.iconUrl.isNotBlank() || star.starIconUrl.isNotBlank()
        if (hasDesc || hasHead) {
            starDrafts += star
        }
        currentStar = null
    }

    for (i in (weaponStart + 1) until rows.size) {
        val row = rows[i]
        val key = row.key.trim()
        val value = row.value.trim()
        val image = row.imageUrl.trim()
        val iconCandidates = rowWeaponDescriptionIcons(row)
        val rowImages = if (row.imageUrls.isNotEmpty()) row.imageUrls else listOf(image)

        if (key.contains("爱用品") || key.contains("专武考据") || key.contains("初始数据")) {
            commitStarDraft()
            break
        }

        if (key.isBlank() && value.isBlank() && rowImages.all { it.isBlank() }) {
            commitStarDraft()
            continue
        }

        if (key == "专武图标") {
            if (image.isNotBlank()) weaponImage = image
            continue
        }
        if (key == "专武名称") {
            if (value.isNotBlank()) weaponName = value
            continue
        }
        if (key == "专武描述") {
            if (value.isNotBlank()) weaponDescription = value
            continue
        }
        if (key == "专武数值") {
            statHeaders = splitCompositeValues(value)
            continue
        }

        val starLabel = extractStarLabel(key)
        if (starLabel != null) {
            if (currentStar?.starLabel != starLabel) {
                commitStarDraft()
                currentStar = WeaponStarDraft(starLabel = starLabel)
            }
            val starRowIcon = rowImages
                .map { it.trim() }
                .firstOrNull { isLikelyWeaponDescriptionIcon(it) }
                .orEmpty()
            if (starRowIcon.isNotBlank() && currentStar?.iconUrl.isNullOrBlank()) {
                currentStar?.iconUrl = starRowIcon
            }
            if (key.contains("技能名称") && value.isNotBlank()) {
                currentStar?.name = value
            } else if (!key.contains("技能名称") && value.isNotBlank()) {
                currentStar?.fallbackDescription = value
            }
            continue
        }

        if (currentStar != null) {
            when {
                key.contains("技能名称") -> if (value.isNotBlank()) currentStar?.name = value
                key == "技能图标" -> if (image.isNotBlank()) currentStar?.iconUrl = image
                key == "技能描述" -> {
                    if (value.isNotBlank()) currentStar?.fallbackDescription = value
                    if (iconCandidates.isNotEmpty()) {
                        currentStar?.fallbackDescriptionIcons?.clear()
                        currentStar?.fallbackDescriptionIcons?.addAll(iconCandidates)
                    }
                }
                else -> {
                    val levelLabel = toDisplayLevelLabel(key)
                    if (levelLabel != null) {
                        if (value.isNotBlank()) {
                            currentStar?.descriptionByLevel?.set(levelLabel, value)
                        }
                        if (iconCandidates.isNotEmpty()) {
                            val list = currentStar?.descriptionIconsByLevel?.getOrPut(levelLabel) { mutableListOf() }
                            list?.clear()
                            list?.addAll(iconCandidates)
                        }
                    } else if (value.isNotBlank() && key != "所需个数" && !key.contains("升级材料")) {
                        if (currentStar?.fallbackDescription.isNullOrBlank()) {
                            currentStar?.fallbackDescription = if (key.isNotBlank()) "$key：$value" else value
                        }
                    }
                }
            }
            continue
        }

        if (key.contains("升级材料") || key == "所需个数") continue
        val statValues = splitCompositeValues(value)
        val shouldTakeAsStat = key.isNotBlank() && statValues.isNotEmpty() && !key.contains("技能")
        if (shouldTakeAsStat) {
            statRows += GuideWeaponStatRow(title = key, values = statValues)
        }
    }
    commitStarDraft()

    val starEffects = starDrafts.mapIndexedNotNull { index, draft ->
        val hasDesc = draft.descriptionByLevel.isNotEmpty() || draft.fallbackDescription.isNotBlank()
        val hasHead = draft.name.isNotBlank() || draft.iconUrl.isNotBlank() || draft.starIconUrl.isNotBlank()
        if (!hasDesc && !hasHead) return@mapIndexedNotNull null
        val titleFromFallback = draft.name.isBlank() &&
            draft.starLabel != "★2" &&
            draft.fallbackDescription.isNotBlank()
        GuideWeaponStarEffect(
            id = "weapon_star_${index}_${draft.starLabel}",
            starLabel = draft.starLabel,
            starIconUrl = draft.starIconUrl,
            name = when {
                draft.name.isNotBlank() -> draft.name
                draft.starLabel == "★2" -> "辅助技能强化"
                titleFromFallback -> draft.fallbackDescription
                else -> draft.starLabel
            },
            iconUrl = draft.iconUrl,
            descriptionByLevel = draft.descriptionByLevel.toMap(),
            descriptionIconsByLevel = draft.descriptionIconsByLevel.mapValues { (_, urls) -> urls.toList() },
            roleTag = if (draft.starLabel == "★2") "辅助技能" else "",
            fallbackDescription = if (titleFromFallback) "" else draft.fallbackDescription,
            fallbackDescriptionIcons = draft.fallbackDescriptionIcons.toList()
        )
    }

    if (weaponName.isBlank() && weaponImage.isBlank() && weaponDescription.isBlank() && statRows.isEmpty() && starEffects.isEmpty()) {
        return null
    }
    val extraStatRows = parseWeaponAdditionalAttributeRows(allRowsForExtraStats)

    return GuideWeaponCardModel(
        name = weaponName.ifBlank { "专属武器" },
        imageUrl = weaponImage,
        description = weaponDescription,
        statHeaders = statHeaders,
        statRows = statRows,
        extraStatRows = extraStatRows,
        starEffects = starEffects,
        glossaryIcons = glossaryIcons
    )
}

private data class WeaponStarDraft(
    val starLabel: String,
    var starIconUrl: String = "",
    var name: String = "",
    var iconUrl: String = "",
    var fallbackDescription: String = "",
    val fallbackDescriptionIcons: MutableList<String> = mutableListOf(),
    val descriptionByLevel: LinkedHashMap<String, String> = linkedMapOf(),
    val descriptionIconsByLevel: LinkedHashMap<String, MutableList<String>> = linkedMapOf()
)

private fun splitCompositeValues(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .replace("／", "/")
        .replace("|", "/")
        .replace("｜", "/")
        .split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" && it != "—" }
}

private fun parseKeyValuePairs(raw: String): List<Pair<String, String>> {
    val tokens = splitCompositeValues(raw)
    if (tokens.isEmpty()) return emptyList()
    val result = mutableListOf<Pair<String, String>>()
    var index = 0
    while (index + 1 < tokens.size) {
        val key = tokens[index].trim()
        val value = tokens[index + 1].trim()
        if (key.isNotBlank() && value.isNotBlank()) {
            result += key to value
        }
        index += 2
    }
    return result
}

internal fun parseWeaponAdditionalAttributeRows(rows: List<BaGuideRow>): List<GuideWeaponStatRow> {
    if (rows.isEmpty()) return emptyList()
    val normalizedRows = rows.map { row ->
        row.copy(key = row.key.trim(), value = row.value.trim())
    }

    val directPairs = normalizedRows
        .filter { row -> Regex("""^附加属性\d+$""").matches(row.key.replace(" ", "")) }
        .flatMap { row -> parseKeyValuePairs(row.value) }

    val fromLv25Pairs = normalizedRows
        .firstOrNull { row -> row.key.replace(" ", "") == "25级" }
        ?.let { row -> parseKeyValuePairs(row.value) }
        .orEmpty()

    val merged = linkedMapOf<String, String>()
    (directPairs + fromLv25Pairs).forEach { (title, value) ->
        val key = title.trim()
        val cleanValue = value.trim()
        if (key.isBlank() || cleanValue.isBlank()) return@forEach
        merged.putIfAbsent(key, cleanValue)
    }

    return merged.map { (title, value) ->
        GuideWeaponStatRow(
            title = title,
            values = listOf(value)
        )
    }
}

private fun extractStarLabel(rawKey: String): String? {
    val key = rawKey.trim()
    val star = Regex("""^(★\d+)""")
        .find(key)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
    return star.ifBlank { null }
}

private fun rowWeaponDescriptionIcons(row: BaGuideRow): List<String> {
    val candidates = if (row.imageUrls.isNotEmpty()) row.imageUrls else listOf(row.imageUrl)
    return candidates
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { isLikelyWeaponDescriptionIcon(it) }
        .distinct()
        .take(6)
}

private fun isLikelyWeaponDescriptionIcon(url: String): Boolean {
    if (url.startsWith("data:image", ignoreCase = true)) return true
    val match = Regex("""/w_(\d{1,4})/h_(\d{1,4})/""")
        .find(url)
    val width = match?.groupValues?.getOrNull(1)?.toIntOrNull()
    val height = match?.groupValues?.getOrNull(2)?.toIntOrNull()
    if (width != null && height != null) {
        return width <= 96 && height <= 96
    }
    return false
}
