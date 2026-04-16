package com.example.keios.ui.page.main.student

import java.util.LinkedHashMap

private const val GUIDE_DERIVED_CACHE_MAX_SIZE = 96

private val guideProfileMetaCache = object : LinkedHashMap<String, List<BaGuideMetaItem>>(
    GUIDE_DERIVED_CACHE_MAX_SIZE,
    0.75f,
    true
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<BaGuideMetaItem>>?): Boolean {
        return size > GUIDE_DERIVED_CACHE_MAX_SIZE
    }
}

private val guideCombatMetaCache = object : LinkedHashMap<String, List<BaGuideMetaItem>>(
    GUIDE_DERIVED_CACHE_MAX_SIZE,
    0.75f,
    true
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<BaGuideMetaItem>>?): Boolean {
        return size > GUIDE_DERIVED_CACHE_MAX_SIZE
    }
}

private fun BaStudentGuideInfo.derivedCacheKey(tag: String): String {
    val source = sourceUrl.trim().ifBlank { title.trim() }
    return buildString {
        append(tag)
        append('|')
        append(source)
        append('|')
        append(syncedAtMs)
        append('|')
        append(stats.size)
        append('|')
        append(profileRows.size)
        append('|')
        append(skillRows.size)
        append('|')
        append(growthRows.size)
        append('|')
        append(simulateRows.size)
    }
}

private val guideInlineNoteKeywords = listOf(
    "可以用", "后面的", "后面", "图标", "替换", "占位",
    "备注", "说明", "注释", "样式", "不用写", "待补",
    "todo", "tbd"
)

private val guideSkillPlaceholderKeywords = listOf(
    "名字", "名称", "技能名", "技能名称", "同上", "..."
)

private fun splitGuideSlashSegments(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .replace("／", "/")
        .replace("|", "/")
        .replace("｜", "/")
        .split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun isLikelyGuideNoteSegment(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return true
    val compact = value.replace(" ", "").lowercase()
    if (compact == "-" || compact == "—" || compact == "--") return true
    if (compact == "…" || compact == "...") return true
    return guideInlineNoteKeywords.any { keyword ->
        compact.contains(keyword.lowercase())
    }
}

internal fun stripGuideInlineNotes(raw: String): String {
    if (raw.isBlank()) return ""
    val beforeArrow = raw
        .substringBefore("<-")
        .substringBefore("←")
        .trim()
    return beforeArrow
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

internal fun sanitizeGuideMetaValueForDisplay(title: String, raw: String): String {
    val cleaned = stripGuideInlineNotes(raw)
    if (cleaned.isBlank()) return ""
    val targetTitle = title.trim()
    val requiresSlashCleanup = targetTitle in setOf("稀有度", "学院", "战术位置作用")
    if (!requiresSlashCleanup) return cleaned
    val segments = splitGuideSlashSegments(cleaned)
    if (segments.isEmpty()) return cleaned
    val first = segments.first().trim()
    if (segments.drop(1).any(::isLikelyGuideNoteSegment)) {
        return first
    }
    return first
}

internal fun sanitizeGuideSkillLabelForDisplay(raw: String): String {
    val cleaned = stripGuideInlineNotes(raw)
    if (cleaned.isBlank()) return ""
    val segments = splitGuideSlashSegments(cleaned)
    if (segments.size <= 1) return cleaned
    val first = segments.first().trim()
    val tailHasPlaceholder = segments.drop(1).any { segment ->
        val compact = segment.replace(" ", "")
        compact.isBlank() ||
            compact == "…" ||
            compact == "..." ||
            compact.length <= 2 ||
            guideSkillPlaceholderKeywords.any { keyword -> compact.contains(keyword) } ||
            isLikelyGuideNoteSegment(segment)
    }
    return if (tailHasPlaceholder) first else cleaned
}

fun BaStudentGuideInfo.bottomTabIconUrl(tab: GuideBottomTab): String {
    fun List<BaGuideRow>.firstImage(): String = firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
    return when (tab) {
        GuideBottomTab.Archive -> ""
        GuideBottomTab.Skills -> tabSkillIconUrl.ifBlank { skillRowsForDisplay().firstImage() }
        GuideBottomTab.Profile -> tabProfileIconUrl.ifBlank { profileRowsForDisplay().firstImage() }
        GuideBottomTab.Voice -> tabVoiceIconUrl.ifBlank { voiceRows.firstImage() }
        GuideBottomTab.Gallery -> tabGalleryIconUrl.ifBlank { galleryItems.firstOrNull()?.imageUrl.orEmpty() }
        GuideBottomTab.Simulate -> tabSimulateIconUrl.ifBlank { simulateRowsForDisplay().firstImage() }
    }
}

fun BaStudentGuideInfo.skillRowsForDisplay(): List<BaGuideRow> {
    if (skillRows.isNotEmpty()) return skillRows
    return stats
        .filter { (k, v) ->
            k.contains("技能") ||
                k.contains("EX", ignoreCase = true) ||
                v.contains("技能")
        }
        .map { (k, v) -> BaGuideRow(k, v) }
}

fun BaStudentGuideInfo.profileRowsForDisplay(): List<BaGuideRow> {
    if (profileRows.isNotEmpty()) return profileRows
    return stats.map { (k, v) -> BaGuideRow(k, v) }
}

fun BaStudentGuideInfo.growthRowsForDisplay(): List<BaGuideRow> {
    if (growthRows.isNotEmpty()) return growthRows
    return stats
        .filter { (k, v) ->
            listOf("装备", "专武", "爱用品", "能力解放", "羁绊", "升级材料", "所需").any {
                k.contains(it, ignoreCase = true) || v.contains(it, ignoreCase = true)
            }
        }
        .map { (k, v) -> BaGuideRow(k, v) }
}

fun BaStudentGuideInfo.simulateRowsForDisplay(): List<BaGuideRow> {
    return simulateRows
}

private fun BaStudentGuideInfo.findFirstRowByKeywords(
    rows: List<BaGuideRow>,
    keywords: List<String>,
    requireImage: Boolean = false
): BaGuideRow? {
    return rows.firstOrNull { row ->
        val key = row.key
        val value = row.value
        val hasKeyword = keywords.any {
            key.contains(it, ignoreCase = true) || value.contains(it, ignoreCase = true)
        }
        hasKeyword && (!requireImage || row.imageUrl.isNotBlank())
    }
}

private fun BaStudentGuideInfo.buildMetaItem(
    title: String,
    valueKeywords: List<String>,
    imageKeywords: List<String> = valueKeywords
): BaGuideMetaItem {
    val rows = profileRowsForDisplay() + skillRowsForDisplay()
    val iconRow = findFirstRowByKeywords(rows, imageKeywords, requireImage = true)
    val value = sanitizeGuideMetaValueForDisplay(
        title = title,
        raw = findGuideFieldValue(*valueKeywords.toTypedArray())
    ).ifBlank { "-" }
    return BaGuideMetaItem(
        title = title,
        value = value,
        imageUrl = iconRow?.imageUrl.orEmpty()
    )
}

fun BaStudentGuideInfo.buildProfileMetaItems(): List<BaGuideMetaItem> {
    val cacheKey = derivedCacheKey("profile_meta")
    synchronized(guideProfileMetaCache) {
        guideProfileMetaCache[cacheKey]?.let { return it }
    }
    val computed = listOf(
        buildMetaItem("稀有度", listOf("稀有度", "星级")),
        buildMetaItem("学院", listOf("所属学园", "所属学院", "学园")),
        buildMetaItem("所属社团", listOf("所属社团", "社团"))
    )
    synchronized(guideProfileMetaCache) {
        guideProfileMetaCache[cacheKey] = computed
    }
    return computed
}

fun BaStudentGuideInfo.buildCombatMetaItems(): List<BaGuideMetaItem> {
    val cacheKey = derivedCacheKey("combat_meta")
    synchronized(guideCombatMetaCache) {
        guideCombatMetaCache[cacheKey]?.let { return it }
    }
    val rawWeaponTypeItem = buildMetaItem("武器类型", listOf("武器类型"))
    val weaponTypeItem = rawWeaponTypeItem.copy(
        value = normalizeWeaponTypeMetaValue(rawWeaponTypeItem.value)
    )
    val mergedTacticalPosition = run {
        val rows = profileRowsForDisplay() + skillRowsForDisplay()
        val tacticalIcon = findFirstRowByKeywords(
            rows = rows,
            keywords = listOf("战术作用", "作用"),
            requireImage = true
        )?.imageUrl.orEmpty()
        val positionIcon = findFirstRowByKeywords(
            rows = rows,
            keywords = listOf("位置"),
            requireImage = true
        )?.imageUrl.orEmpty()
        BaGuideMetaItem(
            title = "战术位置作用",
            value = sanitizeGuideMetaValueForDisplay(
                title = "战术位置作用",
                raw = findGuideFieldValue("战术作用", "作用")
            ).ifBlank { "-" },
            imageUrl = tacticalIcon,
            extraImageUrl = positionIcon
        )
    }
    val computed = listOf(
        mergedTacticalPosition,
        buildMetaItem("攻击类型", listOf("攻击类型")),
        buildMetaItem("防御类型", listOf("防御类型")),
        weaponTypeItem,
        buildMetaItem("市街", listOf("市街")),
        buildMetaItem("屋外", listOf("屋外")),
        buildMetaItem("室内", listOf("屋内", "室内"))
    )
    synchronized(guideCombatMetaCache) {
        guideCombatMetaCache[cacheKey] = computed
    }
    return computed
}

fun shouldHideMovedHeaderRow(row: BaGuideRow): Boolean {
    val key = row.key
    val movedKeywords = listOf(
        "头像", "角色头像",
        "稀有度", "星级", "所属学园", "所属学院", "所属社团",
        "战术作用", "攻击类型", "防御类型", "位置", "武器类型",
        "市街", "屋外", "屋内", "室内"
    )
    if (movedKeywords.any { key.contains(it, ignoreCase = true) }) return true
    if (key.trim().equals("作用", ignoreCase = true)) return true
    return false
}

private fun BaStudentGuideInfo.findGuideFieldValue(vararg keywords: String): String {
    val normalizedKeywords = keywords.map { it.trim() }.filter { it.isNotBlank() }
    if (normalizedKeywords.isEmpty()) return "-"

    fun keyMatched(key: String): Boolean {
        return normalizedKeywords.any { key.contains(it, ignoreCase = true) }
    }

    profileRows.firstOrNull { row ->
        keyMatched(row.key) && row.value.isNotBlank()
    }?.let { return it.value }

    stats.firstOrNull { (key, value) ->
        keyMatched(key) && value.isNotBlank()
    }?.let { return it.second }

    return "-"
}

private fun normalizeWeaponTypeMetaValue(raw: String): String {
    val value = raw.trim()
    if (value.isBlank() || value == "-") return "暂无数据"
    if (value == "原网站暂无该数据") return "暂无数据"
    val compact = value.replace(" ", "")
    val hasPlaceholderHints = compact.contains("这一行") ||
        compact.contains("素材") ||
        compact.contains("占位") ||
        compact.contains("请填写") ||
        compact.contains("暂无")
    val looksSuspiciouslyLong = compact.length > 18
    if (hasPlaceholderHints || looksSuspiciouslyLong) {
        return "暂无数据"
    }
    return value
}
