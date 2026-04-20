package com.example.keios.ui.page.main.student.section

import com.example.keios.ui.page.main.student.guideCircledNumbers
import com.example.keios.ui.page.main.student.guideSkillTypeBracketPattern
import com.example.keios.ui.page.main.student.guideSkillTypeCircledSuffixPattern
import com.example.keios.ui.page.main.student.guideSkillTypeNumericSuffixPattern
import com.example.keios.ui.page.main.student.guideSkillTypeStateSplitPattern
import com.example.keios.ui.page.main.student.sanitizeGuideSkillLabelForDisplay

internal data class GuideSkillTypeMeta(
    val baseType: String,
    val variantIndex: Int? = null,
    val stateTags: List<String> = emptyList()
)

internal data class GuideSkillOwnedTypeMeta(
    val ownerTag: String,
    val skillType: String
)

private val guideSkillOwnedTypePattern = Regex(
    """^(「[^」]{1,40}」|『[^』]{1,40}』|【[^】]{1,40}】|[A-Za-z0-9\u4E00-\u9FFF·・\-\s]{1,40})\s*的\s*(.+)$"""
)

internal data class GuideSkillTypeTokenMeta(
    val base: String,
    val variantIndex: Int? = null
)

internal fun parseGuideSkillTypeMeta(raw: String): GuideSkillTypeMeta {
    val cleaned = sanitizeGuideSkillLabelForDisplay(raw).trim()
    if (cleaned.isBlank()) return GuideSkillTypeMeta(baseType = "")

    var variantIndex: Int? = null
    val stateTags = mutableListOf<String>()
    val stateCandidates = guideSkillTypeBracketPattern
        .findAll(cleaned)
        .map { it.groupValues.getOrElse(1) { "" }.trim() }
        .filter { it.isNotBlank() }
        .toList()

    stateCandidates.forEach { candidate ->
        val tokens = candidate
            .split(guideSkillTypeStateSplitPattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(candidate) }
        tokens.forEach { token ->
            val tokenMeta = parseGuideSkillTypeToken(token)
            if (variantIndex == null && tokenMeta.variantIndex != null) {
                variantIndex = tokenMeta.variantIndex
            }
            val tag = normalizeGuideSkillStateTag(tokenMeta.base.ifBlank { token.trim() })
            if (tag.isNotBlank()) {
                stateTags += tag
            }
        }
    }

    val baseCandidate = guideSkillTypeBracketPattern
        .replace(cleaned, "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '_', '/', '／', '|', '｜')
        .trim()
    val ownedTypeMeta = splitGuideOwnedSkillType(baseCandidate.ifBlank { cleaned })
    val baseToken = ownedTypeMeta?.skillType ?: baseCandidate.ifBlank { cleaned }
    val baseMeta = parseGuideSkillTypeToken(baseToken)
    if (variantIndex == null) {
        variantIndex = baseMeta.variantIndex
    }
    val ownerTag = ownedTypeMeta
        ?.ownerTag
        ?.let(::normalizeGuideSkillStateTag)
        .orEmpty()
    if (ownerTag.isNotBlank()) {
        stateTags.add(0, ownerTag)
    }

    return GuideSkillTypeMeta(
        baseType = baseMeta.base.ifBlank { cleaned },
        variantIndex = variantIndex,
        stateTags = stateTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    )
}

internal fun splitGuideOwnedSkillType(raw: String): GuideSkillOwnedTypeMeta? {
    val normalized = raw.trim().replace(Regex("""\s+"""), " ")
    if (normalized.isBlank()) return null
    val match = guideSkillOwnedTypePattern.matchEntire(normalized) ?: return null
    val ownerTag = match.groupValues.getOrNull(1)?.trim().orEmpty()
    val skillType = match.groupValues.getOrNull(2)?.trim().orEmpty()
    if (ownerTag.isBlank() || skillType.isBlank()) return null
    if (!skillType.contains("技能")) return null
    return GuideSkillOwnedTypeMeta(
        ownerTag = ownerTag,
        skillType = skillType
    )
}

internal fun normalizeGuideSkillStateTag(raw: String): String {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return ""
    val compact = cleaned.replace(" ", "").replace("　", "")
    return if (
        compact.startsWith("对") &&
        compact.endsWith("使用") &&
        compact.length > 3
    ) {
        compact.removeSuffix("使用")
    } else {
        cleaned
    }
}

internal fun parseGuideSkillTypeToken(raw: String): GuideSkillTypeTokenMeta {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return GuideSkillTypeTokenMeta(base = "")

    val circledMatch = guideSkillTypeCircledSuffixPattern.matchEntire(cleaned)
    if (circledMatch != null) {
        val base = circledMatch.groupValues[1].trim()
        val circled = circledMatch.groupValues[2]
        val index = guideCircledNumbers.indexOf(circled).takeIf { it >= 0 }?.plus(1)
        return GuideSkillTypeTokenMeta(
            base = if (base.isBlank()) cleaned else base,
            variantIndex = index
        )
    }

    val numericMatch = guideSkillTypeNumericSuffixPattern.matchEntire(cleaned)
    if (numericMatch != null) {
        val base = numericMatch.groupValues[1].trim()
        val index = numericMatch.groupValues[2].toIntOrNull()?.takeIf { it > 0 }
        if (index != null) {
            return GuideSkillTypeTokenMeta(
                base = if (base.isBlank()) cleaned else base,
                variantIndex = index
            )
        }
    }

    return GuideSkillTypeTokenMeta(base = cleaned)
}

internal fun toGuideCircledNumber(index: Int): String {
    return guideCircledNumbers.getOrNull(index - 1) ?: index.toString()
}
