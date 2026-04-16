package com.example.keios.ui.page.main.student

import java.util.LinkedHashMap
import java.util.Locale

private const val GUIDE_SKILL_CACHE_MAX_SIZE = 96

private val guideSkillCardCache = object : LinkedHashMap<String, List<GuideSkillCardModel>>(
    GUIDE_SKILL_CACHE_MAX_SIZE,
    0.75f,
    true
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<GuideSkillCardModel>>?): Boolean {
        return size > GUIDE_SKILL_CACHE_MAX_SIZE
    }
}

private fun BaStudentGuideInfo.skillCardCacheKey(): String {
    val source = sourceUrl.trim().ifBlank { title.trim() }
    return buildString {
        append(source)
        append('|')
        append(syncedAtMs)
        append('|')
        append(skillRows.size)
        append('|')
        append(profileRows.size)
        append('|')
        append(growthRows.size)
        append('|')
        append(stats.size)
    }
}

fun BaStudentGuideInfo.skillCardsForDisplay(): List<GuideSkillCardModel> {
    val cacheKey = skillCardCacheKey()
    synchronized(guideSkillCardCache) {
        guideSkillCardCache[cacheKey]?.let { return it }
    }
    val rows = skillRowsForDisplay()
    if (rows.isEmpty()) return emptyList()

    val glossaryIcons = extractSkillGlossaryIcons(rows).ifEmpty {
        extractSkillGlossaryIcons(
            buildList {
                addAll(rows)
                addAll(profileRowsForDisplay())
                addAll(growthRowsForDisplay())
            }
        )
    }
    val drafts = parseBaseSkillDrafts(rows)
    if (drafts.isEmpty()) return emptyList()

    val computed = drafts.mapIndexedNotNull { index, draft ->
        val hasHead = draft.name.isNotBlank() || draft.iconUrl.isNotBlank()
        val hasDesc = draft.descriptionByLevel.isNotEmpty() || draft.fallbackDescription.isNotBlank()
        if (!hasHead || !hasDesc) return@mapIndexedNotNull null
        GuideSkillCardModel(
            id = "skill_${index}_${(draft.type + "_" + draft.name).lowercase(Locale.ROOT)}",
            type = draft.type.ifBlank { "技能" },
            name = draft.name.ifBlank { "未命名技能" },
            iconUrl = draft.iconUrl,
            descriptionByLevel = draft.descriptionByLevel.toMap(),
            descriptionIconsByLevel = draft.descriptionIconsByLevel.mapValues { (_, urls) -> urls.toList() },
            costByLevel = draft.costByLevel.toMap(),
            glossaryIcons = glossaryIcons,
            fallbackDescription = draft.fallbackDescription,
            fallbackDescriptionIcons = draft.fallbackDescriptionIcons.toList()
        )
    }
    synchronized(guideSkillCardCache) {
        guideSkillCardCache[cacheKey] = computed
    }
    return computed
}

private data class SkillDraft(
    var type: String = "",
    var name: String = "",
    var iconUrl: String = "",
    var fallbackDescription: String = "",
    val fallbackDescriptionIcons: MutableList<String> = mutableListOf(),
    val descriptionByLevel: LinkedHashMap<String, String> = linkedMapOf(),
    val descriptionIconsByLevel: LinkedHashMap<String, MutableList<String>> = linkedMapOf(),
    val costByLevel: LinkedHashMap<String, String> = linkedMapOf()
)

private fun parseBaseSkillDrafts(rows: List<BaGuideRow>): List<SkillDraft> {
    val result = mutableListOf<SkillDraft>()
    var draft: SkillDraft? = null
    var currentLevelKey: String? = null
    var enteredSkillBlocks = false

    fun commitDraft() {
        val item = draft ?: return
        result += item
        draft = null
        currentLevelKey = null
    }

    rows.forEach { row ->
        val key = row.key.trim()
        val value = row.value.trim()
        val image = row.imageUrl.trim()
        val iconCandidates = rowDescriptionIcons(row)

        if (key == "专武") {
            commitDraft()
            return@forEach
        }
        if (key.contains("技能名词")) {
            commitDraft()
            return@forEach
        }
        if (key.contains("升级材料") || key == "所需个数") {
            if (draft != null) commitDraft()
            return@forEach
        }

        if (key == "技能类型") {
            if (value.isBlank()) return@forEach
            enteredSkillBlocks = true
            commitDraft()
            draft = SkillDraft(type = sanitizeGuideSkillLabelForDisplay(value))
            currentLevelKey = null
            return@forEach
        }

        if (!enteredSkillBlocks) return@forEach

        if (key.isBlank() && value.isBlank() && image.isBlank()) {
            commitDraft()
            return@forEach
        }

        val active = draft ?: return@forEach
        when {
            key.contains("技能名称") && !key.contains("★") -> if (value.isNotBlank()) {
                active.name = sanitizeGuideSkillLabelForDisplay(value)
            }
            key == "技能图标" -> if (image.isNotBlank()) active.iconUrl = image
            key == "技能描述" -> {
                if (value.isNotBlank()) active.fallbackDescription = value
                if (iconCandidates.isNotEmpty()) {
                    active.fallbackDescriptionIcons.clear()
                    active.fallbackDescriptionIcons.addAll(iconCandidates)
                }
            }
            key == "技能COST" -> {
                if (value.isNotBlank()) {
                    val attachLevel = currentLevelKey
                    if (attachLevel != null) {
                        active.costByLevel[attachLevel] = value
                    } else {
                        active.costByLevel["base"] = value
                    }
                }
            }
            else -> {
                val levelLabel = toDisplayLevelLabel(key)
                if (levelLabel != null) {
                    if (value.isNotBlank()) {
                        active.descriptionByLevel[levelLabel] = value
                    }
                    if (iconCandidates.isNotEmpty()) {
                        val list = active.descriptionIconsByLevel.getOrPut(levelLabel) { mutableListOf() }
                        list.clear()
                        list.addAll(iconCandidates)
                    }
                    currentLevelKey = levelLabel
                }
            }
        }
    }
    commitDraft()
    return result
}

internal fun extractSkillGlossaryIcons(rows: List<BaGuideRow>): Map<String, String> {
    if (rows.isEmpty()) return emptyMap()
    val glossary = linkedMapOf<String, String>()
    var inGlossary = false
    rows.forEach { row ->
        val key = row.key.trim()
        val image = row.imageUrl.trim()
        if (key == "技能名词") {
            inGlossary = true
            return@forEach
        }
        if (!inGlossary) return@forEach
        if (key.contains("升级材料") || key == "专武" || key.contains("爱用品")) {
            inGlossary = false
            return@forEach
        }
        if (key == "名词图标" || key == "名词解释" || key.matches(Regex("""名词\d+"""))) return@forEach
        if (key.isNotBlank() && image.isNotBlank()) glossary[key] = image
    }
    return glossary
}

internal fun toDisplayLevelLabel(rawKey: String): String? {
    val key = rawKey.trim().replace(" ", "")
    val level = Regex("""(?i)^LV\.?(\d{1,2})$""")
        .find(key)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: return null
    return "Lv.$level"
}

private fun rowDescriptionIcons(row: BaGuideRow): List<String> {
    val candidates = if (row.imageUrls.isNotEmpty()) row.imageUrls else listOf(row.imageUrl)
    return candidates
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { isLikelyDescriptionIcon(it) }
        .distinct()
        .take(6)
}

private fun isLikelyDescriptionIcon(url: String): Boolean {
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
