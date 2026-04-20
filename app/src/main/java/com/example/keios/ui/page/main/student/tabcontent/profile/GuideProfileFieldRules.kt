package com.example.keios.ui.page.main.student.tabcontent.profile

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.isRenderableGalleryImageUrl
import com.example.keios.ui.page.main.student.stripGuideInlineNotes
import com.example.keios.ui.page.main.student.tabcontent.simulate.isLikelySimulateStatLabel

internal data class ProfileFieldSpec(
    val title: String,
    val aliases: List<String>,
    val hideWhenEmpty: Boolean = false
)

internal data class GiftPreferenceItem(
    val label: String,
    val giftImageUrl: String,
    val emojiImageUrl: String
)

internal val profileNicknameFieldSpecs = listOf(
    ProfileFieldSpec("角色名称", listOf("角色名称")),
    ProfileFieldSpec("全名", listOf("全名")),
    ProfileFieldSpec("假名注音", listOf("假名注音", "假名注明")),
    ProfileFieldSpec("繁中译名", listOf("繁中译名")),
    ProfileFieldSpec("简中译名", listOf("简中译名"))
)

internal val profileStudentInfoFieldSpecs = listOf(
    ProfileFieldSpec("年龄", listOf("年龄")),
    ProfileFieldSpec("生日", listOf("生日")),
    ProfileFieldSpec("身高", listOf("身高")),
    ProfileFieldSpec("画师", listOf("画师", "原画师")),
    ProfileFieldSpec("实装日期", listOf("实装日期", "首次登场日期")),
    ProfileFieldSpec("声优", listOf("声优")),
    ProfileFieldSpec("角色考据", listOf("角色考据"), hideWhenEmpty = true),
    ProfileFieldSpec("设计", listOf("设计", "设计师"), hideWhenEmpty = true)
)

internal val profileHobbyFieldSpecs = listOf(
    ProfileFieldSpec("兴趣爱好", listOf("兴趣爱好")),
    ProfileFieldSpec("个人简介", listOf("个人简介")),
    ProfileFieldSpec("MomoTalk状态消息", listOf("MomoTalk状态消息", "Momotalk状态消息")),
    ProfileFieldSpec("MomoTalk解锁等级", listOf("MomoTalk解锁等级", "Momotalk解锁等级"), hideWhenEmpty = true)
)

internal val profileStructuredFieldSpecs = profileNicknameFieldSpecs + profileStudentInfoFieldSpecs + profileHobbyFieldSpecs

internal fun shouldUseProfileValueCapsule(
    key: String,
    value: String,
    onClick: (() -> Unit)?
): Boolean {
    if (onClick != null) return false
    if (isProfileValuePlaceholder(value)) return false
    val normalizedKey = normalizeProfileFieldKey(key)
    if (value.length > 12 || value.contains('\n')) return false
    if (value.contains("http", ignoreCase = true)) return false
    if (value.contains("/") || value.contains(" / ")) return false
    if (value.contains("：") || value.contains(":")) return false
    if (normalizedKey in profileLongTextFieldKeys) return false
    if (normalizedKey in profileCapsuleFieldKeys) return true
    return value.length <= 8 && !value.contains(" ")
}

internal fun adaptiveProfileKeyMaxWidth(
    key: String,
    value: String,
    containerWidth: Dp
): Dp {
    val keyLength = key.trim().length
    val baseWidth = when {
        keyLength >= 12 -> 148.dp
        keyLength >= 10 -> 138.dp
        keyLength >= 8 -> 124.dp
        keyLength >= 6 -> 110.dp
        else -> 94.dp
    }
    val valuePenalty = when {
        value.length >= 64 -> 24.dp
        value.length >= 40 -> 16.dp
        value.length >= 24 -> 8.dp
        else -> 0.dp
    }
    val preferred = (baseWidth - valuePenalty).coerceAtLeast(84.dp)
    val containerLimit = (containerWidth * 0.48f).coerceAtLeast(84.dp)
    return preferred.coerceAtMost(containerLimit)
}

internal fun isGiftPreferenceProfileRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key.startsWith(giftPreferenceRowPrefixKey)
}

internal fun buildGiftPreferenceItems(rows: List<BaGuideRow>): List<GiftPreferenceItem> {
    if (rows.isEmpty()) return emptyList()
    return rows.mapIndexedNotNull { index, row ->
        val normalizedImages = buildList {
            add(row.imageUrl.trim())
            addAll(row.imageUrls.map { it.trim() })
        }.filter { candidate ->
            isRenderableGalleryImageUrl(candidate)
        }.distinct()
        val giftImage = normalizedImages.firstOrNull().orEmpty()
        if (giftImage.isBlank()) return@mapIndexedNotNull null
        val emojiImage = normalizedImages.firstOrNull { candidate ->
            candidate != giftImage
        }.orEmpty()
        val fallbackIndex = extractOrderedNumbers(row.key).firstOrNull() ?: (index + 1)
        val label = row.value
            .trim()
            .takeIf { it.isNotBlank() && !isProfileValuePlaceholder(it) }
            ?: "礼物$fallbackIndex"
        GiftPreferenceItem(
            label = label,
            giftImageUrl = giftImage,
            emojiImageUrl = emojiImage
        )
    }.distinctBy { item ->
        "${item.giftImageUrl}|${item.emojiImageUrl}|${item.label.trim()}"
    }
}

internal fun isProfileSectionHeaderRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key in profileSectionHeaderKeys
}

internal fun isProfileValuePlaceholder(value: String): Boolean {
    val normalized = value.trim()
    val compact = normalized
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
    if (normalized.isBlank()) return true
    if (compact.matches(Regex("""^[\\/|｜／,，;；:：._\-—~·*]+$"""))) return true
    return normalized == "-" ||
        normalized == "—" ||
        normalized == "--" ||
        normalized == "暂无" ||
        normalized == "无" ||
        compact == "n" ||
        compact == "null" ||
        compact == "undefined" ||
        compact == "nan"
}

internal val profileInstructionNoteRegex = Regex("""(?:<-|←)?\s*(?:这个|这里|此处|这条)?\s*不用写""")

internal fun stripProfileInstructionNotes(raw: String): String {
    if (raw.isBlank()) return ""
    if (!profileInstructionNoteRegex.containsMatchIn(raw)) return raw.trim()

    val segments = raw
        .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
        .map { segment ->
            profileInstructionNoteRegex
                .replace(segment, "")
                .trim()
                .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
                .trim()
        }
        .filter { it.isNotBlank() }
    if (segments.isNotEmpty()) {
        return segments.joinToString(" / ").trim()
    }
    return profileInstructionNoteRegex
        .replace(raw, "")
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

internal fun isProfileInstructionPlaceholder(value: String): Boolean {
    if (value.isBlank()) return false
    val normalized = value.trim()
    if (!profileInstructionNoteRegex.containsMatchIn(normalized)) return false
    val stripped = stripProfileInstructionNotes(normalized)
    return isProfileValuePlaceholder(stripped)
}

internal fun stripProfileCopyHint(raw: String): String {
    if (raw.isBlank()) return ""
    val hintRegex = Regex("""(?:<-|←)?\s*大部分时候可以去别的图鉴复制""")
    if (!hintRegex.containsMatchIn(raw)) return raw.trim()

    val segments = raw
        .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
        .map { it.trim() }
        .filter { part ->
            part.isNotBlank() && !hintRegex.containsMatchIn(part)
        }
    if (segments.isNotEmpty()) {
        return segments.joinToString(" / ").trim()
    }
    return raw
        .replace(hintRegex, "")
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
}

internal val profileInlineNoteStripFieldKeys = setOf(
    "角色考据",
    "设计",
    "MomoTalk解锁等级",
    "Momotalk解锁等级"
).map(::normalizeProfileFieldKey).toSet()

internal fun sanitizeProfileFieldValue(key: String, value: String): String {
    if (value.isBlank()) return ""
    val normalizedKey = normalizeProfileFieldKey(key)
    var cleaned = value.trim()
    if (normalizedKey == normalizeProfileFieldKey("声优")) {
        cleaned = stripProfileCopyHint(cleaned)
    }
    if (normalizedKey in profileInlineNoteStripFieldKeys) {
        cleaned = stripGuideInlineNotes(cleaned)
    }
    cleaned = stripProfileInstructionNotes(cleaned)
    cleaned = cleaned
        .trim(' ', '/', '／', '|', '｜', ',', '，', ';', '；')
        .trim()
    return cleaned
}

internal fun isProfileRowAliasMatch(row: BaGuideRow, aliases: List<String>): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (key.isBlank()) return false
    return aliases.any { alias ->
        key == normalizeProfileFieldKey(alias)
    }
}

internal fun buildProfileCardRows(rows: List<BaGuideRow>, specs: List<ProfileFieldSpec>): List<BaGuideRow> {
    return buildList {
        specs.forEach { spec ->
            val matched = rows.firstOrNull { row ->
                isProfileRowAliasMatch(row, spec.aliases)
            } ?: return@forEach
            val normalizedValue = sanitizeProfileFieldValue(spec.title, matched.value)
            if (isProfileInstructionPlaceholder(matched.value) && isProfileValuePlaceholder(normalizedValue)) {
                return@forEach
            }
            if (spec.hideWhenEmpty && isProfileValuePlaceholder(normalizedValue)) {
                return@forEach
            }
            add(matched.copy(key = spec.title, value = normalizedValue))
        }
    }
}

internal fun isStructuredProfileCardRow(row: BaGuideRow): Boolean {
    return profileStructuredFieldSpecs.any { spec ->
        isProfileRowAliasMatch(row, spec.aliases)
    }
}

internal val topDataStatKeys = setOf(
    "攻击力", "防御力", "生命值", "治愈力",
    "命中值", "闪避值", "暴击值", "暴击伤害",
    "稳定值", "射程", "群控强化力", "群控抵抗力",
    "装弹数", "防御无视值", "受恢复率", "COST恢复力"
)
internal val normalizedTopDataStatKeys = topDataStatKeys.map(::normalizeProfileFieldKey).toSet()

internal fun splitGuideCompositeValues(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .replace("／", "/")
        .replace("|", "/")
        .replace("｜", "/")
        .split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" && it != "—" }
}

internal fun isSkillMigratedProfileRow(
    row: BaGuideRow,
    hasTopDataHeader: Boolean,
    hasInitialDataHeader: Boolean
): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    val value = normalizeProfileFieldKey(row.value)
    if (Regex("""^附加属性\d+$""").matches(key)) return true
    if (key == normalizeProfileFieldKey("初始数据")) return true
    if (key == normalizeProfileFieldKey("顶级数据")) return true
    if (key == normalizeProfileFieldKey("25级")) return true
    if (Regex("""^t\d+$""", RegexOption.IGNORE_CASE).matches(key)) return true
    if (Regex("""^t\d+(效果|所需升级材料|技能图标)$""", RegexOption.IGNORE_CASE).matches(key)) return true
    if (
        isLikelySimulateStatLabel(row.key) &&
        isLikelySimulateStatLabel(row.value) &&
        !Regex("""\d""").containsMatchIn(value)
    ) {
        return true
    }
    if ((hasTopDataHeader || hasInitialDataHeader) && key in normalizedTopDataStatKeys) return true
    return false
}
