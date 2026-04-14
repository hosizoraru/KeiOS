package com.example.keios.ui.page.main.student

fun BaStudentGuideInfo.bottomTabIconUrl(tab: GuideBottomTab): String {
    fun List<BaGuideRow>.firstImage(): String = firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
    return when (tab) {
        GuideBottomTab.Archive -> ""
        GuideBottomTab.Skills -> tabSkillIconUrl.ifBlank { skillRowsForDisplay().firstImage() }
        GuideBottomTab.Profile -> tabProfileIconUrl.ifBlank { profileRowsForDisplay().firstImage() }
        GuideBottomTab.Voice -> tabVoiceIconUrl.ifBlank { voiceRows.firstImage() }
        GuideBottomTab.Gallery -> tabGalleryIconUrl.ifBlank { galleryItems.firstOrNull()?.imageUrl.orEmpty() }
        GuideBottomTab.Simulate -> tabSimulateIconUrl.ifBlank { growthRowsForDisplay().firstImage() }
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
    val value = findGuideFieldValue(*valueKeywords.toTypedArray()).ifBlank { "-" }
    return BaGuideMetaItem(
        title = title,
        value = value,
        imageUrl = iconRow?.imageUrl.orEmpty()
    )
}

fun BaStudentGuideInfo.buildProfileMetaItems(): List<BaGuideMetaItem> {
    return listOf(
        buildMetaItem("稀有度", listOf("稀有度", "星级")),
        buildMetaItem("学院", listOf("所属学园", "所属学院", "学园")),
        buildMetaItem("所属社团", listOf("所属社团", "社团"))
    )
}

fun BaStudentGuideInfo.buildCombatMetaItems(): List<BaGuideMetaItem> {
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
            value = findGuideFieldValue("战术作用", "作用").ifBlank { "-" },
            imageUrl = tacticalIcon,
            extraImageUrl = positionIcon
        )
    }
    return listOf(
        mergedTacticalPosition,
        buildMetaItem("攻击类型", listOf("攻击类型")),
        buildMetaItem("防御类型", listOf("防御类型")),
        buildMetaItem("武器类型", listOf("武器类型")),
        buildMetaItem("市街", listOf("市街")),
        buildMetaItem("屋外", listOf("屋外")),
        buildMetaItem("室内", listOf("屋内", "室内"))
    )
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
