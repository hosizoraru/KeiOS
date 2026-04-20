package com.example.keios.ui.page.main.student.tabcontent.profile

import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.normalizeGalleryTitle

internal fun extractOrderedNumbers(raw: String): List<Int> {
    if (raw.isBlank()) return emptyList()
    return Regex("""\d+""")
        .findAll(raw)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
}

internal fun sortKeyNumbers(raw: String): Pair<Int, Int> {
    val numbers = extractOrderedNumbers(raw)
    if (numbers.isEmpty()) return -1 to -1
    val first = numbers.getOrElse(0) { -1 }
    val second = numbers.getOrElse(1) { -1 }
    return first to second
}

internal fun sortProfileRowsByKeyNumbers(rows: List<BaGuideRow>): List<BaGuideRow> {
    return rows.sortedWith(
        compareBy<BaGuideRow>(
            { sortKeyNumbers(it.key).first },
            { sortKeyNumbers(it.key).second },
            { normalizeProfileFieldKey(it.key) }
        )
    )
}

internal fun sortGalleryItemsByTitleNumbers(items: List<BaGuideGalleryItem>): List<BaGuideGalleryItem> {
    return items.sortedWith(
        compareBy<BaGuideGalleryItem>(
            { sortKeyNumbers(normalizeGalleryTitle(it.title)).first },
            { sortKeyNumbers(normalizeGalleryTitle(it.title)).second },
            { normalizeGalleryTitle(it.title) }
        )
    )
}

internal fun normalizeProfileFieldKey(raw: String): String {
    return raw
        .replace(" ", "")
        .replace("　", "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
        .lowercase()
}

internal val profileRoleReferenceFieldKey = normalizeProfileFieldKey("角色考据")
internal val relatedSameNameRoleHeaderKey = normalizeProfileFieldKey("相关同名角色")
internal val sameNameRoleNameRowKey = normalizeProfileFieldKey("同名角色名称")
internal val giftPreferenceRowPrefixKey = normalizeProfileFieldKey("礼物偏好礼物")
internal val profileSectionHeaderKeys = setOf("介绍", "学生信息", "信息")
    .map(::normalizeProfileFieldKey)
    .toSet()
internal val profileCapsuleFieldKeys = setOf(
    "角色名称", "年龄", "生日", "身高",
    "实装日期", "MomoTalk解锁等级",
    "繁中译名", "简中译名", "假名注音", "假名注明"
).map(::normalizeProfileFieldKey).toSet()
internal val profileLongTextFieldKeys = setOf(
    "全名", "个人简介", "兴趣爱好", "MomoTalk状态消息"
).map(::normalizeProfileFieldKey).toSet()
