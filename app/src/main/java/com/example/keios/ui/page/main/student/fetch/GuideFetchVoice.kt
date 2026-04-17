package com.example.keios.ui.page.main.student

import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import org.json.JSONArray
import org.json.JSONObject

internal fun normalizeVoiceLanguageLabelRaw(raw: String): String {
    return stripHtml(raw)
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
        .trim()
}

internal fun canonicalVoiceLanguageLabel(raw: String): String {
    val normalized = normalizeVoiceLanguageLabelRaw(raw)
    if (normalized.isBlank()) return ""
    return when {
        normalized.contains("官翻") || normalized.contains("官方翻译") || normalized.contains("官方中文") || normalized.contains("官中") -> "官翻"
        normalized.contains("韩") || normalized.contains("kr") || normalized.contains("kor") || normalized.contains("korean") -> "韩配"
        normalized.contains("中") || normalized.contains("cn") || normalized.contains("国语") || normalized.contains("国配") || normalized.contains("中文") -> "中配"
        normalized.contains("日") || normalized.contains("jp") || normalized.contains("jpn") || normalized.contains("日本") -> "日配"
        else -> stripHtml(raw).trim()
    }
}

internal fun defaultVoiceLanguageLabelForIndex(index: Int): String {
    return when (index) {
        0 -> "日配"
        1 -> "中配"
        2 -> "韩配"
        else -> "语言${index + 1}"
    }
}

internal fun looksLikeVoiceLanguageLabel(raw: String): Boolean {
    val normalizedRaw = normalizeVoiceLanguageLabelRaw(raw)
    if (normalizedRaw.isBlank()) return false
    val sanitized = normalizedRaw
        .replace("cv", "")
        .replace("声优", "")
        .trim(':', '：', '|', '｜', '-', '－', '—', '/', '／', ',', '，', ';', '；')
    if (sanitized.isBlank()) return false
    // Strict label-only guard: avoid treating composite payloads like
    // "日 | 梅澤 めぐ" as a pure language label.
    if (Regex("""[|｜:：\-－—/／,，;；]""").containsMatchIn(sanitized)) return false
    return sanitized in setOf(
        "日", "日配", "日语", "日本", "jp", "jpn",
        "中", "中配", "中文", "国语", "国配", "cn",
        "韩", "韩配", "kr", "kor", "korean",
        "官翻", "官中", "官方翻译", "官方中文"
    )
}

internal fun voiceLineDisplayPriority(label: String): Int {
    return when (canonicalVoiceLanguageLabel(label)) {
        "日配" -> 0
        "中配" -> 1
        "官翻" -> 2
        "韩配" -> 3
        else -> 4
    }
}

internal fun sortVoiceLinePairsForDisplay(
    pairs: List<Pair<String, String>>
): List<Pair<String, String>> {
    return pairs.withIndex()
        .sortedWith(
            compareBy<IndexedValue<Pair<String, String>>> { indexed ->
                voiceLineDisplayPriority(indexed.value.first)
            }.thenBy { indexed ->
                indexed.index
            }
        )
        .map { indexed ->
            canonicalVoiceLanguageLabel(indexed.value.first).ifBlank { indexed.value.first } to indexed.value.second
        }
}

internal fun parseVoiceDataFromBaseData(
    baseData: JSONArray,
    sourceUrl: String
): Pair<List<String>, List<BaGuideVoiceEntry>> {
    val languageHeaders = mutableListOf<String>()
    val entries = mutableListOf<BaGuideVoiceEntry>()
    var inVoiceBlock = false
    var currentVoiceSection = ""

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (key == "配音语言") {
            inVoiceBlock = true
            currentVoiceSection = ""
            languageHeaders.clear()
            for (j in 1 until row.length()) {
                val cell = row.optJSONObject(j) ?: continue
                val label = canonicalVoiceLanguageLabel(cell.optString("value"))
                if (label.isNotBlank() && label != "官翻" && label !in languageHeaders) {
                    languageHeaders += label
                }
            }
            continue
        }
        if (!inVoiceBlock) continue
        if (key.isBlank() || key == "配音" || key == "配音大类") continue

        val isVoiceCategory = isVoiceCategoryKey(key)
        if (isVoiceCategory) {
            currentVoiceSection = key
        } else {
            if (entries.isNotEmpty() && isVoiceBlockTailKey(key)) {
                break
            }
            if (currentVoiceSection.isBlank()) {
                continue
            }
        }
        val section = if (isVoiceCategory) {
            key
        } else {
            currentVoiceSection
        }
        if (section.isBlank()) {
            continue
        }

        val textByAudioSegment = linkedMapOf<Int, MutableList<String>>()
        val rowAudioUrls = mutableListOf<String>()
        var title = ""
        var titleAssigned = false
        fun appendAudio(raw: String, type: String) {
            val normalizedByRaw = extractAudioUrlsFromRaw(sourceUrl, raw)
            val candidates = buildList {
                addAll(normalizedByRaw)
                if (type == "audio") {
                    val fallback = normalizeMediaUrl(sourceUrl, raw)
                    if (isAudioUrl(fallback)) add(fallback)
                }
            }
            candidates.forEach { candidate ->
                val normalized = candidate.trim()
                if (normalized.isNotBlank() && isAudioUrl(normalized) && normalized !in rowAudioUrls) {
                    rowAudioUrls += normalized
                }
            }
        }

        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val rawValue = cell.optString("value").trim()
            if (rawValue.isBlank()) continue
            if (type == "audio") {
                appendAudio(rawValue, type)
                continue
            }
            val text = stripHtml(rawValue)
            if (!titleAssigned) {
                if (text.isBlank()) continue
                title = text
                titleAssigned = true
            } else {
                if (text.isNotBlank()) {
                    val segment = rowAudioUrls.size
                    textByAudioSegment.getOrPut(segment) { mutableListOf() } += text
                }
                appendAudio(rawValue, type)
            }
        }
        if (!titleAssigned) {
            title = key
        }

        val dubbingCount = maxOf(languageHeaders.size, rowAudioUrls.size)
        val dubbingTexts = MutableList(dubbingCount) { "" }
        val segmentZero = textByAudioSegment[0].orEmpty().toMutableList()
        for (index in dubbingTexts.indices) {
            if (segmentZero.isNotEmpty()) {
                dubbingTexts[index] = segmentZero.removeAt(0).trim()
            }
        }
        textByAudioSegment[0] = segmentZero
        for (index in dubbingTexts.indices) {
            if (dubbingTexts[index].isNotBlank()) continue
            val bucket = textByAudioSegment[index].orEmpty().toMutableList()
            if (bucket.isNotEmpty()) {
                dubbingTexts[index] = bucket.removeAt(0).trim()
                textByAudioSegment[index] = bucket
            }
        }
        val officialTranslation = textByAudioSegment
            .entries
            .sortedBy { it.key }
            .flatMap { it.value }
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        val linePairs = buildList {
            for (index in dubbingTexts.indices) {
                val label = languageHeaders.getOrNull(index).orElse(defaultVoiceLanguageLabelForIndex(index))
                val text = dubbingTexts[index].trim()
                if (text.isNotBlank()) {
                    add(label to text)
                }
            }
            if (officialTranslation.isNotBlank()) {
                add("官翻" to officialTranslation)
            }
        }
        if (linePairs.isEmpty() && rowAudioUrls.none { it.isNotBlank() }) continue
        val sortedLinePairs = sortVoiceLinePairsForDisplay(linePairs)
        val lineHeaders = sortedLinePairs.map { it.first }
        val lines = sortedLinePairs.map { it.second }
        val legacyAudioUrl = rowAudioUrls.firstOrNull { it.isNotBlank() }.orEmpty()

        entries += BaGuideVoiceEntry(
            section = section,
            title = title,
            lineHeaders = lineHeaders,
            lines = lines,
            audioUrls = rowAudioUrls,
            audioUrl = legacyAudioUrl
        )
    }

    val maxAudioCount = entries.maxOfOrNull { entry -> entry.audioUrls.size } ?: 0
    val headerCount = maxOf(languageHeaders.size, maxAudioCount)
    val normalizedHeaders = languageHeaders.toMutableList()
    while (normalizedHeaders.size < headerCount) {
        val fallback = defaultVoiceLanguageLabelForIndex(normalizedHeaders.size)
        if (fallback in normalizedHeaders) {
            normalizedHeaders += "语言${normalizedHeaders.size + 1}"
        } else {
            normalizedHeaders += fallback
        }
    }
    val normalizedEntries = if (headerCount <= 0) {
        entries
    } else {
        entries.map { entry ->
            val normalizedAudioUrls = if (entry.audioUrls.size >= headerCount) {
                entry.audioUrls.take(headerCount)
            } else {
                entry.audioUrls + List(headerCount - entry.audioUrls.size) { "" }
            }
            entry.copy(
                audioUrls = normalizedAudioUrls,
                audioUrl = entry.audioUrl.ifBlank {
                    normalizedAudioUrls.firstOrNull { it.isNotBlank() }.orEmpty()
                }
            )
        }
    }

    return normalizedHeaders to normalizedEntries
}

internal fun parseVoiceCvByLanguageFromBaseData(baseData: JSONArray): Map<String, String> {
    val cvByLanguage = linkedMapOf<String, String>()

    fun cleanCvRawText(raw: String): String {
        if (raw.isBlank()) return ""
        return decodeBasicHtmlEntity(raw)
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .joinToString("\n")
            .replace(Regex("[ \t]+"), " ")
            .trim()
    }

    fun cleanCvValue(raw: String): String {
        val hintRegex = Regex("""(?:<-|←)?\s*大部分时候可以去别的图鉴复制""")
        val strippedHint = if (hintRegex.containsMatchIn(raw)) {
            val segments = raw
                .split(Regex("""\s*(?:/|／|\||｜|,|，|\n)\s*"""))
                .map { it.trim() }
                .filter { part ->
                    part.isNotBlank() && !hintRegex.containsMatchIn(part)
                }
            if (segments.isNotEmpty()) {
                segments.joinToString(" / ")
            } else {
                raw.replace(hintRegex, "")
            }
        } else {
            raw
        }
        return strippedHint.trim()
            .trim(',', '，', ';', '；', '|', '/', '／')
            .trim()
    }

    fun assignByLabel(rawLabel: String, rawValue: String) {
        val label = canonicalVoiceLanguageLabel(rawLabel)
        if (label.isBlank()) return
        val value = cleanCvValue(rawValue)
        if (value.isBlank()) return
        if (cvByLanguage[label].isNullOrBlank()) {
            cvByLanguage[label] = value
        }
    }

    val labelPattern = "日配|日语|日|jp|jpn|中配|中|cn|国语|国配|中文|韩配|韩|kr|kor|korean|官翻|官中|官方翻译|官方中文"
    val pairRegex = Regex(
        """(?i)($labelPattern)\s*(?:[\|:：\-－—])\s*([\s\S]*?)(?=(?:\s*[,，;；/／\n]\s*|\s+)(?:$labelPattern)\s*(?:[\|:：\-－—])|$)"""
    )
    val compactPairRegex = Regex("""(?i)^($labelPattern)\s*(?:[\|:：\-－—])\s*(.+)$""")
    val spacedPairRegex = Regex("""(?i)^($labelPattern)\s+(.+)$""")

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (!(key.contains("声优") || key.equals("cv", ignoreCase = true) || key.contains("CV", ignoreCase = true))) {
            continue
        }
        val blocks = mutableListOf<String>()
        for (j in 1 until row.length()) {
            val text = cleanCvRawText(row.optJSONObject(j)?.optString("value").orEmpty())
            if (text.isNotBlank()) {
                blocks += text
            }
        }
        if (blocks.isEmpty()) continue

        var cursor = 0
        while (cursor + 1 < blocks.size) {
            val left = blocks[cursor]
            val right = blocks[cursor + 1]
            if (looksLikeVoiceLanguageLabel(left) && !looksLikeVoiceLanguageLabel(right)) {
                assignByLabel(left, right)
                cursor += 2
            } else {
                cursor += 1
            }
        }

        blocks.forEach { block ->
            val normalizedBlock = block
                .replace('｜', '|')
                .replace('：', ':')
                .replace('，', ',')
                .replace('；', ';')
                .replace('\u3000', ' ')
                .trim()

            var matchedByRegex = false
            pairRegex.findAll(normalizedBlock).forEach { match ->
                matchedByRegex = true
                val label = match.groupValues.getOrNull(1).orEmpty().trim()
                val value = match.groupValues.getOrNull(2).orEmpty()
                assignByLabel(label, value)
            }

            if (!matchedByRegex) {
                normalizedBlock
                    .split('\n')
                    .flatMap { line -> line.split(Regex("""\s*[,，;；]\s*""")) }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        compactPairRegex.find(line)?.let { match ->
                            val label = match.groupValues.getOrNull(1).orEmpty().trim()
                            val value = match.groupValues.getOrNull(2).orEmpty()
                            assignByLabel(label, value)
                            return@forEach
                        }
                        spacedPairRegex.find(line)?.let { match ->
                            val label = match.groupValues.getOrNull(1).orEmpty().trim()
                            val value = match.groupValues.getOrNull(2).orEmpty()
                            assignByLabel(label, value)
                        }
                    }
            }
        }
    }

    if (cvByLanguage.isEmpty()) return emptyMap()
    val ordered = linkedMapOf<String, String>()
    listOf("日配", "中配", "韩配", "官翻").forEach { label ->
        cvByLanguage[label]?.takeIf { it.isNotBlank() }?.let { value ->
            ordered[label] = value
        }
    }
    cvByLanguage.forEach { (label, value) ->
        if (label !in ordered && value.isNotBlank()) {
            ordered[label] = value
        }
    }
    return ordered
}

internal fun String?.orElse(fallback: String): String {
    val value = this.orEmpty()
    return if (value.isBlank()) fallback else value
}

internal fun deriveVoiceCvLegacyFields(cvByLanguage: Map<String, String>): Pair<String, String> {
    val jp = cvByLanguage.entries.firstOrNull { (key, _) ->
        canonicalVoiceLanguageLabel(key) == "日配"
    }?.value.orEmpty()
    val cn = cvByLanguage.entries.firstOrNull { (key, _) ->
        canonicalVoiceLanguageLabel(key) == "中配"
    }?.value.orEmpty()
    return jp to cn
}

internal fun mergeVoiceLanguageHeaders(
    rawHeaders: List<String>,
    voiceEntries: List<BaGuideVoiceEntry>,
    cvByLanguage: Map<String, String>
): List<String> {
    val merged = mutableListOf<String>()
    rawHeaders.forEach { header ->
        val normalized = canonicalVoiceLanguageLabel(header)
        if (normalized.isNotBlank() && normalized != "官翻" && normalized !in merged) {
            merged += normalized
        }
    }
    cvByLanguage.keys.forEach { label ->
        val normalized = canonicalVoiceLanguageLabel(label)
        if (normalized.isNotBlank() && normalized != "官翻" && normalized !in merged) {
            merged += normalized
        }
    }
    val maxAudioCount = voiceEntries.maxOfOrNull { entry ->
        maxOf(entry.audioUrls.size, if (entry.audioUrl.isNotBlank()) 1 else 0)
    } ?: 0
    while (merged.size < maxAudioCount) {
        val fallback = defaultVoiceLanguageLabelForIndex(merged.size)
        if (fallback in merged) {
            merged += "语言${merged.size + 1}"
        } else {
            merged += fallback
        }
    }
    if (merged.isEmpty() && maxAudioCount > 0) {
        repeat(maxAudioCount) { index ->
            merged += defaultVoiceLanguageLabelForIndex(index)
        }
    }
    return merged
}

internal fun normalizeVoiceEntriesWithHeaderCount(
    entries: List<BaGuideVoiceEntry>,
    headerCount: Int
): List<BaGuideVoiceEntry> {
    if (headerCount <= 0) return entries
    return entries.map { entry ->
        val rawAudioUrls = if (entry.audioUrls.isNotEmpty()) {
            entry.audioUrls.map { it.trim() }
        } else {
            listOf(entry.audioUrl.trim()).filter { it.isNotBlank() }
        }
        val normalizedAudioUrls = if (rawAudioUrls.size >= headerCount) {
            rawAudioUrls.take(headerCount)
        } else {
            rawAudioUrls + List(headerCount - rawAudioUrls.size) { "" }
        }
        val legacyAudioUrl = entry.audioUrl.trim().ifBlank {
            normalizedAudioUrls.firstOrNull { it.isNotBlank() }.orEmpty()
        }
        entry.copy(
            audioUrls = normalizedAudioUrls,
            audioUrl = legacyAudioUrl
        )
    }
}

