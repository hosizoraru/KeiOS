package os.kei.ui.page.main.student.tabcontent

import android.net.Uri
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.BaGuideVoiceEntry
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.fetch.normalizeGuideUrl

internal fun canonicalVoiceLanguageForDisplay(raw: String): String {
    val normalized = raw
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
        .trim()
    if (normalized.isBlank()) return ""
    return when {
        normalized.contains("官翻") || normalized.contains("官方翻译") || normalized.contains("官方中文") || normalized.contains("官中") -> "官翻"
        normalized.contains("韩") || normalized.contains("kr") || normalized.contains("kor") || normalized.contains("korean") -> "韩配"
        normalized.contains("中") || normalized.contains("cn") || normalized.contains("国语") || normalized.contains("国配") || normalized.contains("中文") -> "中配"
        normalized.contains("日") || normalized.contains("jp") || normalized.contains("jpn") || normalized.contains("日本") -> "日配"
        else -> raw.trim()
    }
}

internal fun defaultVoiceLanguageHeaderForDisplay(index: Int): String {
    return when (index) {
        0 -> "日配"
        1 -> "中配"
        2 -> "韩配"
        else -> "语言${index + 1}"
    }
}

internal fun buildVoiceCvDisplayMap(info: BaStudentGuideInfo): Map<String, String> {
    val merged = linkedMapOf<String, String>()
    info.voiceCvByLanguage.forEach { (rawKey, rawValue) ->
        val key = canonicalVoiceLanguageForDisplay(rawKey)
        val value = rawValue.trim()
        if (
            key.isNotBlank() &&
            value.isNotBlank() &&
            !isOfficialTranslationHeader(key) &&
            merged[key].isNullOrBlank()
        ) {
            merged[key] = value
        }
    }
    val jp = info.voiceCvJp.trim()
    if (jp.isNotBlank() && merged["日配"].isNullOrBlank()) {
        merged["日配"] = jp
    }
    val cn = info.voiceCvCn.trim()
    if (cn.isNotBlank() && merged["中配"].isNullOrBlank()) {
        merged["中配"] = cn
    }

    val ordered = linkedMapOf<String, String>()
    listOf("日配", "中配", "韩配").forEach { label ->
        merged[label]?.takeIf { it.isNotBlank() }?.let { value ->
            ordered[label] = value
        }
    }
    merged.forEach { (key, value) ->
        if (key !in ordered && value.isNotBlank()) {
            ordered[key] = value
        }
    }
    return ordered
}

internal fun buildVoiceLanguageHeadersForDisplay(
    headers: List<String>,
    entries: List<BaGuideVoiceEntry>
): List<String> {
    val merged = mutableListOf<String>()
    headers.forEach { raw ->
        val normalized = canonicalVoiceLanguageForDisplay(raw)
        if (normalized.isBlank()) return@forEach
        if (isOfficialTranslationHeader(normalized)) return@forEach
        if (normalized !in merged) {
            merged += normalized
        }
    }
    val maxAudioCount = entries.maxOfOrNull { entry ->
        maxOf(entry.audioUrls.size, if (entry.audioUrl.trim().isNotBlank()) 1 else 0)
    } ?: 0
    while (merged.size < maxAudioCount) {
        val fallback = defaultVoiceLanguageHeaderForDisplay(merged.size)
        merged += fallback
    }
    return merged.mapIndexed { index, header ->
        header.ifBlank { defaultVoiceLanguageHeaderForDisplay(index) }
    }
}

internal fun isOfficialTranslationHeader(header: String): Boolean {
    return canonicalVoiceLanguageForDisplay(header) == "官翻"
}

internal fun hasVoiceAudioAtIndex(entries: List<BaGuideVoiceEntry>, index: Int): Boolean {
    return entries.any { entry ->
        val directAudio = entry.audioUrls.getOrNull(index).orEmpty().trim()
        directAudio.isNotBlank() ||
            (index == 0 && entry.audioUrls.isEmpty() && entry.audioUrl.trim().isNotBlank())
    }
}

internal fun buildDubbingHeadersForVoiceCard(
    headers: List<String>,
    entries: List<BaGuideVoiceEntry>
): List<String> {
    if (headers.isEmpty()) return emptyList()
    return headers.mapIndexedNotNull { index, header ->
        val normalized = canonicalVoiceLanguageForDisplay(header)
        if (normalized.isBlank()) return@mapIndexedNotNull null
        if (isOfficialTranslationHeader(normalized)) return@mapIndexedNotNull null
        if (!hasVoiceAudioAtIndex(entries, index)) return@mapIndexedNotNull null
        normalized
    }.distinct()
}

internal fun resolveVoicePlaybackUrl(
    entry: BaGuideVoiceEntry,
    headers: List<String>,
    selectedHeader: String
): String {
    val normalizedSelected = canonicalVoiceLanguageForDisplay(selectedHeader)
    if (headers.isNotEmpty() && normalizedSelected.isNotBlank()) {
        val selectedIndex = headers.indexOfFirst { header ->
            canonicalVoiceLanguageForDisplay(header) == normalizedSelected
        }
        if (selectedIndex >= 0) {
            val selectedAudio =
                normalizeGuideUrl(entry.audioUrls.getOrNull(selectedIndex).orEmpty())
            if (selectedAudio.isNotBlank()) {
                return selectedAudio
            }
        }
    }
    val fallbackAudio = entry.audioUrls
        .asSequence()
        .map(::normalizeGuideUrl)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
    if (fallbackAudio.isNotBlank()) return fallbackAudio
    return normalizeGuideUrl(entry.audioUrl)
}

internal fun normalizeGuidePlaybackSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
    }
}

internal fun isGrowthTitleVoiceRow(row: BaGuideRow): Boolean {
    fun normalize(text: String): String = text.replace(" ", "").lowercase()
    val key = normalize(row.key)
    val value = normalize(row.value)
    fun matches(text: String): Boolean {
        if (text.isBlank()) return false
        return (text.contains("成长") && text.contains("title")) ||
            text.contains("成长标题") ||
            text.contains("growthtitle") ||
            text.contains("growth_title")
    }
    return matches(key) || matches(value)
}

internal fun isVoicePlaceholderRow(row: BaGuideRow): Boolean {
    val merged = listOf(row.key.trim(), row.value.trim()).joinToString(" ").replace(" ", "")
    return Regex("""被CC\d+""").containsMatchIn(merged)
}

internal fun buildGrowthTitleVoiceEntries(rows: List<BaGuideRow>): List<BaGuideVoiceEntry> {
    return rows.mapNotNull { row ->
        val lines = parseGrowthTitleVoiceLines(row.value)
        val jp = lines.getOrNull(0).orEmpty().trim()
        val cn = lines.getOrNull(1).orEmpty().trim()
        if (jp.isBlank() && cn.isBlank()) return@mapNotNull null
        BaGuideVoiceEntry(
            section = "成长",
            title = row.key.trim(),
            lines = listOf(jp, cn),
            audioUrl = ""
        )
    }
}

internal fun parseGrowthTitleVoiceLines(raw: String): List<String> {
    val normalized = raw.trim()
    if (normalized.isBlank()) return listOf("", "")
    val lineBreakParts = normalized
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lineBreakParts.size >= 2) {
        return listOf(lineBreakParts[0], lineBreakParts[1])
    }
    val slashParts = normalized
        .split(Regex("""\s*(?:/|／|\|)\s*"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return when {
        slashParts.size >= 2 -> listOf(slashParts[0], slashParts[1])
        slashParts.size == 1 -> listOf(slashParts[0], "")
        else -> listOf("", "")
    }
}
