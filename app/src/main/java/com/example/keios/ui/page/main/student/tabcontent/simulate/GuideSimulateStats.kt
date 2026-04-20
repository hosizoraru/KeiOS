package com.example.keios.ui.page.main.student.tabcontent.simulate

import com.example.keios.ui.page.main.student.tabcontent.profile.normalizeProfileFieldKey
import com.example.keios.ui.page.main.student.tabcontent.profile.normalizedTopDataStatKeys
import java.util.Locale
import kotlin.math.abs

internal fun extractSimulateLevelCapsule(rawHint: String): String {
    val hint = rawHint.trim().trim('*').trim()
    if (hint.isBlank()) return ""

    Regex("""(?i)Lv\s*\d+""")
        .find(hint)
        ?.value
        ?.replace(" ", "")
        ?.let { raw ->
            val digits = Regex("""\d+""").find(raw)?.value.orEmpty()
            if (digits.isNotBlank()) return "Lv$digits"
        }

    Regex("""(?i)T\d+""")
        .find(hint)
        ?.value
        ?.replace(" ", "")
        ?.uppercase()
        ?.let { return it }

    Regex("""\d+级""")
        .find(hint)
        ?.value
        ?.let { return it }

    return ""
}

internal fun isSimulateSubHeader(key: String): Boolean {
    val normalized = normalizeProfileFieldKey(key)
    if (Regex("""^\d+号装备$""").matches(normalized)) return true
    if (Regex("""^羁绊角色\d+$""").matches(normalized)) return true
    if (Regex("""^\d+级$""").matches(normalized)) return true
    return false
}

internal fun simulateStatGlyphForKey(raw: String): String? {
    val key = normalizeProfileFieldKey(raw)
    return when (key) {
        normalizeProfileFieldKey("攻击力") -> "✢"
        normalizeProfileFieldKey("防御力") -> "⛨"
        normalizeProfileFieldKey("生命值") -> "♥"
        normalizeProfileFieldKey("治愈力") -> "✚"
        normalizeProfileFieldKey("命中值") -> "◎"
        normalizeProfileFieldKey("闪避值") -> "◌"
        normalizeProfileFieldKey("暴击值") -> "✶"
        normalizeProfileFieldKey("暴击伤害") -> "✹"
        normalizeProfileFieldKey("稳定值") -> "≋"
        normalizeProfileFieldKey("射程") -> "➚"
        normalizeProfileFieldKey("群控强化力") -> "⬆"
        normalizeProfileFieldKey("群控抵抗力") -> "⬡"
        normalizeProfileFieldKey("装弹数") -> "☰"
        normalizeProfileFieldKey("防御无视值") -> "⊘"
        normalizeProfileFieldKey("受恢复率") -> "⟳"
        normalizeProfileFieldKey("COST恢复力") -> "⌛"
        normalizeProfileFieldKey("暴击抵抗值") -> "⛯"
        normalizeProfileFieldKey("暴伤抵抗率"),
        normalizeProfileFieldKey("暴击伤害抵抗率") -> "✺"
        else -> null
    }
}

internal fun buildSimulateMaxDeltaText(
    maxValue: String,
    initialValue: String?
): String {
    val maxText = maxValue.trim()
    val initialText = initialValue?.trim().orEmpty()
    if (maxText.isBlank() || initialText.isBlank()) return ""
    if (Regex("""\([+-]\d+(\.\d+)?\)""").containsMatchIn(maxText)) return ""

    val maxNumber = extractComparableNumber(maxText) ?: return ""
    val initialNumber = extractComparableNumber(initialText) ?: return ""
    val diff = maxNumber - initialNumber
    if (abs(diff) < 0.0001) return ""

    val sign = if (diff > 0) "+" else "-"
    val absDiff = abs(diff)
    val deltaText = if (abs(absDiff - absDiff.toLong().toDouble()) < 0.0001) {
        absDiff.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", absDiff).trimEnd('0').trimEnd('.')
    }
    return "($sign$deltaText)"
}

internal fun extractComparableNumber(raw: String): Double? {
    val normalized = raw.replace(",", "").trim()
    val numberText = Regex("""-?\d+(\.\d+)?""")
        .find(normalized)
        ?.value
        .orEmpty()
    return numberText.toDoubleOrNull()
}

internal fun isLikelySimulateStatLabel(raw: String): Boolean {
    val normalized = normalizeProfileFieldKey(raw)
    if (normalized in normalizedTopDataStatKeys) return true
    val extraStatKeys = setOf(
        "暴伤抵抗率",
        "暴击抵抗值",
        "暴伤抵抗值"
    ).map(::normalizeProfileFieldKey).toSet()
    if (normalized in extraStatKeys) return true
    return normalized.endsWith("值") || normalized.endsWith("率")
}
