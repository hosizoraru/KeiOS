package com.example.keios.ui.page.main.student

import android.net.Uri
import com.example.keios.ba.helper.GameKeeFetchHelper
import org.json.JSONArray
import org.json.JSONObject

fun normalizeGuideUrl(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    if (value.startsWith("data:image", ignoreCase = true)) return value
    return when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://www.gamekee.com$value"
        else -> "https://www.gamekee.com/$value"
    }
}

private fun decodeBasicHtmlEntity(raw: String): String {
    return raw
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

private fun stripHtml(raw: String): String {
    return decodeBasicHtmlEntity(raw.replace(Regex("<[^>]+>"), " "))
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractMeta(html: String, key: String): String {
    val escaped = Regex.escape(key)
    val regexes = listOf(
        Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        ),
        Regex(
            """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE)
        )
    )
    regexes.forEach { regex ->
        val value = regex.find(html)?.groupValues?.getOrNull(1).orEmpty().trim()
        if (value.isNotBlank()) return decodeBasicHtmlEntity(value)
    }
    return ""
}

private fun normalizeImageUrl(sourceUrl: String, imageRaw: String): String {
    val img = imageRaw.trim()
    if (img.isBlank()) return ""
    if (img.startsWith("http://") || img.startsWith("https://")) return img
    if (img.startsWith("data:image", ignoreCase = true)) return img
    if (img.startsWith("//")) return "https:$img"
    return if (img.startsWith("/")) {
        val source = runCatching { Uri.parse(sourceUrl) }.getOrNull()
        val host = source?.scheme?.plus("://")?.plus(source.host.orEmpty()).orEmpty()
        (host.ifBlank { "https://www.gamekee.com" }) + img
    } else {
        "https://www.gamekee.com/$img"
    }
}

private fun extractStatsFromHtml(html: String): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    val trRegex = Regex(
        """<tr[^>]*>\s*<th[^>]*>(.*?)</th>\s*<td[^>]*>(.*?)</td>\s*</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    trRegex.findAll(html).forEach { m ->
        val key = stripHtml(m.groupValues[1])
        val value = stripHtml(m.groupValues[2])
        if (key.isNotBlank() && value.isNotBlank()) rows += key to value
    }
    if (rows.isNotEmpty()) return rows.take(8)

    val dlRegex = Regex(
        """<dt[^>]*>(.*?)</dt>\s*<dd[^>]*>(.*?)</dd>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    dlRegex.findAll(html).forEach { m ->
        val key = stripHtml(m.groupValues[1])
        val value = stripHtml(m.groupValues[2])
        if (key.isNotBlank() && value.isNotBlank()) rows += key to value
    }
    return rows.take(8)
}

private data class GuideDetailExtract(
    val imageUrl: String = "",
    val summary: String = "",
    val stats: List<Pair<String, String>> = emptyList(),
    val skillRows: List<BaGuideRow> = emptyList(),
    val profileRows: List<BaGuideRow> = emptyList(),
    val galleryItems: List<BaGuideGalleryItem> = emptyList(),
    val growthRows: List<BaGuideRow> = emptyList(),
    val voiceRows: List<BaGuideRow> = emptyList(),
    val voiceLanguageHeaders: List<String> = emptyList(),
    val voiceEntries: List<BaGuideVoiceEntry> = emptyList(),
    val tabSkillIconUrl: String = "",
    val tabProfileIconUrl: String = "",
    val tabVoiceIconUrl: String = "",
    val tabGalleryIconUrl: String = "",
    val tabSimulateIconUrl: String = ""
)

private data class GuideBaseRow(
    val key: String,
    val textValues: List<String>,
    val imageValues: List<String>,
    val videoValues: List<String> = emptyList(),
    val mediaTypes: Set<String> = emptySet()
)

private val voiceCategoryKeys = setOf("通常", "大厅及咖啡馆", "战斗", "活动", "事件", "好感度")

private fun isPlaceholderMediaToken(raw: String): Boolean {
    val value = raw.trim().lowercase()
    if (value.isBlank()) return true
    if (value == "n" || value == "null" || value == "undefined" || value == "nan") return true
    return value.matches(Regex("""^\d+$"""))
}

private fun hasInvalidMediaTail(rawUrl: String): Boolean {
    val value = rawUrl.trim()
    if (value.isBlank()) return true
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    if (!host.endsWith("gamekee.com")) return false
    val segments = uri.pathSegments.filter { it.isNotBlank() }
    if (segments.size != 1) return false
    val tail = segments.first()
        .substringBefore('?')
        .substringBefore('#')
        .trim()
    return isPlaceholderMediaToken(tail)
}

private fun looksLikeImageUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:image", ignoreCase = true)) return true
    if (isPlaceholderMediaToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (Regex("""\.(png|jpg|jpeg|webp|gif|bmp|svg|avif)(\?.*)?(#.*)?$""").containsMatchIn(lower)) {
        return true
    }
    if (hasInvalidMediaTail(normalized)) return false
    val uri = runCatching { Uri.parse(normalized) }.getOrNull()
    val host = uri?.host?.lowercase().orEmpty()
    val path = (uri?.encodedPath ?: uri?.path ?: "").lowercase()
    if (host.contains("cdnimg") || host.contains("img")) return true
    if (path.contains("/upload") || path.contains("/uploads") || path.contains("/images/") || path.contains("/wiki/")) {
        return true
    }
    return lower.contains("x-oss-process=image")
}

private fun looksLikeVideoUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:video", ignoreCase = true)) return true
    if (isPlaceholderMediaToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (hasInvalidMediaTail(normalized)) return false
    return lower.endsWith(".mp4") ||
        lower.endsWith(".webm") ||
        lower.endsWith(".mov") ||
        lower.endsWith(".m3u8") ||
        lower.contains(".mp4?") ||
        lower.contains(".m3u8?")
}

private fun extractImageUrlsFromHtml(sourceUrl: String, raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val regex = Regex(
        """(?i)<img[^>]+src\s*=\s*["']([^"']+)["'][^>]*>"""
    )
    return regex.findAll(raw)
        .mapNotNull { match ->
            normalizeImageUrl(sourceUrl, match.groupValues.getOrNull(1).orEmpty())
        }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

private fun normalizeMediaUrl(sourceUrl: String, mediaRaw: String): String {
    return normalizeImageUrl(sourceUrl, mediaRaw)
}

private fun extractImageUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 6) return emptyList()
    return when (any) {
        is String -> {
            val normalized = normalizeImageUrl(sourceUrl, any)
            buildList {
                if (looksLikeImageUrl(normalized)) add(normalized)
                addAll(
                    any.split(",", ";")
                        .map { normalizeImageUrl(sourceUrl, it.trim()) }
                        .filter { looksLikeImageUrl(it) }
                )
                addAll(extractImageUrlsFromHtml(sourceUrl, any))
            }.distinct()
        }

        is JSONObject -> {
            val result = mutableListOf<String>()
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractImageUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractImageUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

private fun extractVideoUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 6) return emptyList()
    return when (any) {
        is String -> {
            buildList {
                val normalized = normalizeMediaUrl(sourceUrl, any)
                if (looksLikeVideoUrl(normalized)) add(normalized)
                any.split(",", ";")
                    .map { normalizeMediaUrl(sourceUrl, it.trim()) }
                    .filter { looksLikeVideoUrl(it) }
                    .forEach { add(it) }
            }.distinct()
        }

        is JSONObject -> {
            val result = mutableListOf<String>()
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractVideoUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractVideoUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

private fun parseGalleryItemsFromBaseData(baseData: JSONArray, sourceUrl: String): List<BaGuideGalleryItem> {
    val out = mutableListOf<BaGuideGalleryItem>()
    val galleryKeywords = listOf(
        "立绘", "本家画", "TV动画设定图", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
        "互动家具", "角色表情", "表情", "角色演示", "设定集", "官方介绍", "情人节巧克力"
    )

    fun isGalleryKey(raw: String): Boolean {
        val key = stripHtml(raw).trim()
        if (key.isBlank()) return false
        return galleryKeywords.any { key.contains(it, ignoreCase = true) }
    }

    val memoryUnlockLevel = run {
        var level = ""
        for (i in 0 until baseData.length()) {
            val row = baseData.optJSONArray(i) ?: continue
            if (row.length() == 0) continue
            val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
            if (key != "回忆大厅解锁等级") continue
            val value = buildString {
                for (j in 1 until row.length()) {
                    val cell = row.optJSONObject(j) ?: continue
                    val text = stripHtml(cell.optString("value"))
                    if (text.isNotBlank()) {
                        append(text)
                        break
                    }
                }
            }
            val digits = Regex("""\d+""").find(value)?.value.orEmpty()
            level = if (digits.isNotBlank()) digits else value
            break
        }
        level
    }

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (key == "回忆大厅解锁等级") continue
        if (key.replace(" ", "").startsWith("回忆大厅文件")) continue
        if (!isGalleryKey(key)) continue

        val rowImages = linkedSetOf<String>()
        val rowVideos = linkedSetOf<String>()

        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val valueAny = cell.opt("value")
            val valueText = cell.optString("value").trim()

            when (type) {
                "image" -> {
                    if (isPlaceholderMediaToken(valueText)) continue
                    val normalized = normalizeImageUrl(sourceUrl, valueText)
                    if (looksLikeImageUrl(normalized)) rowImages += normalized
                }
                "imageset", "live2d" -> {
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                }
                "video" -> {
                    val direct = normalizeMediaUrl(sourceUrl, valueText)
                    if (looksLikeVideoUrl(direct)) rowVideos += direct
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                }
                else -> {
                    rowImages += extractImageUrlsFromHtml(sourceUrl, valueText)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                }
            }
        }

        if (rowImages.isNotEmpty()) {
            out += rowImages.mapIndexed { index, imageUrl ->
                BaGuideGalleryItem(
                    title = if (rowImages.size > 1) "$key ${index + 1}" else key,
                    imageUrl = imageUrl,
                    mediaType = "image",
                    mediaUrl = imageUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else ""
                )
            }
        }
        if (rowVideos.isNotEmpty()) {
            out += rowVideos.mapIndexed { index, videoUrl ->
                BaGuideGalleryItem(
                    title = if (rowVideos.size > 1) "$key ${index + 1}" else key,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "video",
                    mediaUrl = videoUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else ""
                )
            }
        }
    }

    return out
}

private fun parseGalleryItemsFromStyleData(styleData: JSONArray?, sourceUrl: String): List<BaGuideGalleryItem> {
    if (styleData == null || styleData.length() == 0) return emptyList()
    val out = mutableListOf<BaGuideGalleryItem>()
    for (i in 0 until styleData.length()) {
        val block = styleData.optJSONObject(i) ?: continue
        val blockName = stripHtml(block.optString("name"))
            .ifBlank { "样式 ${i + 1}" }
        val blockData = block.opt("data") ?: continue
        val imageUrls = extractImageUrlsFromAny(sourceUrl, blockData)
            .filter { it.isNotBlank() }
            .distinct()
        val videoUrls = extractVideoUrlsFromAny(sourceUrl, blockData)
            .filter { it.isNotBlank() }
            .distinct()
        if (imageUrls.isEmpty() && videoUrls.isEmpty()) continue

        imageUrls.forEachIndexed { index, imageUrl ->
            out += BaGuideGalleryItem(
                title = if (imageUrls.size > 1) "$blockName ${index + 1}" else blockName,
                imageUrl = imageUrl,
                mediaType = "image",
                mediaUrl = imageUrl
            )
        }
        videoUrls.forEachIndexed { index, videoUrl ->
            out += BaGuideGalleryItem(
                title = if (videoUrls.size > 1) "$blockName ${index + 1}" else blockName,
                imageUrl = imageUrls.firstOrNull().orEmpty(),
                mediaType = "video",
                mediaUrl = videoUrl
            )
        }
    }
    return out
}

private fun isAudioUrl(url: String): Boolean {
    val value = url.trim().lowercase()
    if (value.isBlank()) return false
    return Regex("""\.(mp3|ogg|wav|m4a|aac)(\?.*)?$""").containsMatchIn(value)
}

private fun extractAudioUrlsFromRaw(sourceUrl: String, raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val regex = Regex(
        """(?i)((?:https?:)?//[^\s"'<>]+|/[^\s"'<>]+|[^\s"'<>]+\.(?:mp3|ogg|wav|m4a|aac)(?:\?[^\s"'<>]*)?)"""
    )
    return regex.findAll(raw)
        .mapNotNull { match ->
            normalizeMediaUrl(sourceUrl, match.groupValues.getOrNull(1).orEmpty())
        }
        .filter { it.isNotBlank() && isAudioUrl(it) }
        .distinct()
        .toList()
}

private fun parseVoiceDataFromBaseData(
    baseData: JSONArray,
    sourceUrl: String
): Pair<List<String>, List<BaGuideVoiceEntry>> {
    val languageHeaders = mutableListOf<String>()
    val rawLanguageHeaders = mutableListOf<String>()
    val entries = mutableListOf<BaGuideVoiceEntry>()
    var inVoiceBlock = false

    fun isJapaneseHeader(value: String): Boolean {
        val text = value.trim().lowercase()
        return text.contains("日") || text.contains("jp") || text.contains("jpn")
    }

    fun isChineseHeader(value: String): Boolean {
        val text = value.trim().lowercase()
        return text.contains("中") || text.contains("cn")
    }

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (key == "配音语言") {
            inVoiceBlock = true
            rawLanguageHeaders.clear()
            languageHeaders.clear()
            for (j in 1 until row.length()) {
                val cell = row.optJSONObject(j) ?: continue
                val text = stripHtml(cell.optString("value"))
                if (text.isNotBlank()) {
                    rawLanguageHeaders += text
                }
            }
            val jpHeader = rawLanguageHeaders.firstOrNull(::isJapaneseHeader) ?: "日配"
            val cnHeader = rawLanguageHeaders.firstOrNull(::isChineseHeader) ?: "中配"
            languageHeaders += jpHeader
            languageHeaders += cnHeader
            continue
        }
        if (!inVoiceBlock) continue
        if (key.isBlank() || key == "配音" || key == "配音大类") continue

        if (key !in voiceCategoryKeys) {
            if (entries.isNotEmpty() && (
                    key.startsWith("官方介绍") ||
                        key.startsWith("角色表情") ||
                        key.contains("设定集") ||
                        key.contains("本家画") ||
                        key.contains("原画师") ||
                        key.contains("个人账号主页")
                    )
            ) {
                break
            }
            continue
        }

        val languageSlotSize = maxOf(rawLanguageHeaders.size, 2)
        val languageTexts = MutableList(languageSlotSize) { "" }
        var title = ""
        var titleAssigned = false
        var languageCursor = 0
        var audioUrl = ""
        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val type = cell.optString("type").trim().lowercase()
            val rawValue = cell.optString("value").trim()
            val audioFromRaw = extractAudioUrlsFromRaw(sourceUrl, rawValue).firstOrNull().orEmpty()
            if (type == "audio") {
                val normalized = audioFromRaw.ifBlank { normalizeMediaUrl(sourceUrl, rawValue) }
                if (audioUrl.isBlank() && isAudioUrl(normalized)) {
                    audioUrl = normalized
                }
                continue
            }
            if (audioUrl.isBlank() && audioFromRaw.isNotBlank()) {
                audioUrl = audioFromRaw
            }
            val text = stripHtml(rawValue)
            if (!titleAssigned) {
                title = text
                titleAssigned = true
            } else {
                if (languageCursor < languageTexts.size) {
                    languageTexts[languageCursor] = text
                }
                languageCursor += 1
            }
        }

        val jpIndex = rawLanguageHeaders.indexOfFirst(::isJapaneseHeader).takeIf { it >= 0 } ?: 0
        val cnIndex = rawLanguageHeaders.indexOfFirst(::isChineseHeader).takeIf { it >= 0 }
            ?: if (languageTexts.size > 1) 1 else 0
        val jpText = languageTexts.getOrNull(jpIndex).orEmpty().trim()
        val cnText = languageTexts.getOrNull(cnIndex).orEmpty().trim()

        // 过滤网站预留但没有实际台词文本的占位条目。
        if (jpText.isBlank() && cnText.isBlank()) continue

        entries += BaGuideVoiceEntry(
            section = key,
            title = title,
            lines = listOf(jpText, cnText),
            audioUrl = audioUrl
        )
    }

    return languageHeaders to entries
}

private fun firstImageFromAny(any: Any?, sourceUrl: String, depth: Int = 0): String {
    if (any == null || depth > 4) return ""
    return when (any) {
        is String -> {
            val normalized = normalizeImageUrl(sourceUrl, any)
            if (looksLikeImageUrl(normalized)) normalized else ""
        }

        is JSONObject -> {
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val found = firstImageFromAny(any.opt(key), sourceUrl, depth + 1)
                if (found.isNotBlank()) return found
            }
            ""
        }

        is JSONArray -> {
            for (i in 0 until any.length()) {
                val found = firstImageFromAny(any.opt(i), sourceUrl, depth + 1)
                if (found.isNotBlank()) return found
            }
            ""
        }

        else -> ""
    }
}

private fun normalizeGuideTabLabel(raw: String): String {
    return stripHtml(raw)
        .replace(Regex("\\s+"), "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
}

private fun mapGuideTabByLabel(rawLabel: String, strict: Boolean): GuideTab? {
    val label = normalizeGuideTabLabel(rawLabel)
    if (label.isBlank()) return null
    if (strict) {
        return when (label) {
            GuideTab.Skills.label -> GuideTab.Skills
            GuideTab.Profile.label -> GuideTab.Profile
            GuideTab.Voice.label -> GuideTab.Voice
            GuideTab.Gallery.label -> GuideTab.Gallery
            GuideTab.Simulate.label -> GuideTab.Simulate
            else -> null
        }
    }
    return when {
        label.contains("语音") || label.contains("台词") -> GuideTab.Voice
        label.contains("影画") || label.contains("鉴赏") -> GuideTab.Gallery
        label.contains("养成") || label.contains("模拟") -> GuideTab.Simulate
        label.contains("档案") -> GuideTab.Profile
        label.contains("技能") -> GuideTab.Skills
        else -> null
    }
}

private fun findImageByKnownKeys(obj: JSONObject, sourceUrl: String): String {
    val keys = listOf(
        "icon", "iconUrl", "icon_url", "tabIcon", "tab_icon",
        "img", "image", "imageUrl", "image_url",
        "thumb", "cover", "src", "url"
    )
    keys.forEach { key ->
        val any = obj.opt(key) ?: return@forEach
        val fromAny = when (any) {
            is String -> normalizeImageUrl(sourceUrl, any)
            is JSONObject, is JSONArray -> firstImageFromAny(any, sourceUrl)
            else -> ""
        }
        if (looksLikeImageUrl(fromAny)) return fromAny
    }
    return ""
}

private fun extractGuideTabIcons(root: JSONObject, sourceUrl: String): Map<GuideTab, String> {
    val strict = mutableMapOf<GuideTab, String>()
    val fuzzy = mutableMapOf<GuideTab, String>()
    val labelKeys = listOf("name", "title", "label", "tabName", "tab_name", "text", "key")

    fun tryPut(tab: GuideTab?, icon: String, strictMode: Boolean) {
        if (tab == null || icon.isBlank()) return
        if (strictMode) {
            strict.putIfAbsent(tab, icon)
        } else {
            fuzzy.putIfAbsent(tab, icon)
        }
    }

    fun walk(any: Any?) {
        when (any) {
            is JSONObject -> {
                val label = labelKeys
                    .asSequence()
                    .map { key -> any.optString(key).trim() }
                    .firstOrNull { it.isNotBlank() }
                    .orEmpty()
                val icon = findImageByKnownKeys(any, sourceUrl)
                if (label.isNotBlank() && icon.isNotBlank()) {
                    tryPut(mapGuideTabByLabel(label, strict = true), icon, strictMode = true)
                    tryPut(mapGuideTabByLabel(label, strict = false), icon, strictMode = false)
                }

                val keys = any.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = any.opt(key)
                    val keyTabStrict = mapGuideTabByLabel(key, strict = true)
                    val keyTabFuzzy = mapGuideTabByLabel(key, strict = false)
                    if ((keyTabStrict != null || keyTabFuzzy != null) && value != null) {
                        val iconByValue = firstImageFromAny(value, sourceUrl)
                        if (iconByValue.isNotBlank()) {
                            tryPut(keyTabStrict, iconByValue, strictMode = true)
                            tryPut(keyTabFuzzy, iconByValue, strictMode = false)
                        }
                    }
                    walk(value)
                }
            }

            is JSONArray -> {
                for (i in 0 until any.length()) {
                    walk(any.opt(i))
                }
            }
        }
    }

    walk(root.opt("dataSource"))
    walk(root.opt("tabs"))
    walk(root)

    return buildMap {
        GuideTab.entries.forEach { tab ->
            val icon = strict[tab].orEmpty().ifBlank { fuzzy[tab].orEmpty() }
            if (icon.isNotBlank()) put(tab, icon)
        }
    }
}

private fun parseGuideDetailFromContentJson(raw: String, sourceUrl: String): GuideDetailExtract {
    if (raw.isBlank()) return GuideDetailExtract()
    return runCatching {
        val root = JSONObject(raw)
        val tabIcons = extractGuideTabIcons(root, sourceUrl)
        val styleData = root.optJSONArray("styleData")
        val galleryFromStyleData = parseGalleryItemsFromStyleData(styleData, sourceUrl)
        val baseData = root.optJSONArray("baseData")
            ?: return@runCatching GuideDetailExtract(
                galleryItems = galleryFromStyleData,
                tabSkillIconUrl = tabIcons[GuideTab.Skills].orEmpty(),
                tabProfileIconUrl = tabIcons[GuideTab.Profile].orEmpty(),
                tabVoiceIconUrl = tabIcons[GuideTab.Voice].orEmpty(),
                tabGalleryIconUrl = tabIcons[GuideTab.Gallery].orEmpty(),
                tabSimulateIconUrl = tabIcons[GuideTab.Simulate].orEmpty()
            )
        val (voiceLanguageHeaders, voiceEntries) = parseVoiceDataFromBaseData(baseData, sourceUrl)
        val galleryFromMediaTypes = parseGalleryItemsFromBaseData(baseData, sourceUrl)
        val baseRows = mutableListOf<GuideBaseRow>()
        var firstImage = ""

        for (i in 0 until baseData.length()) {
            val row = baseData.optJSONArray(i) ?: continue
            if (row.length() == 0) continue
            val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
            val textValues = mutableListOf<String>()
            val imageValues = mutableListOf<String>()
            val videoValues = mutableListOf<String>()
            val mediaTypes = mutableSetOf<String>()
            for (j in 1 until row.length()) {
                val cell = row.optJSONObject(j) ?: continue
                val type = cell.optString("type").trim().lowercase()
                val rawValueAny = cell.opt("value")
                val rawValue = cell.optString("value").trim()
                if (rawValue.isBlank()) continue

                when (type) {
                    "image" -> {
                        if (isPlaceholderMediaToken(rawValue)) continue
                        val normalized = normalizeImageUrl(sourceUrl, rawValue)
                        if (looksLikeImageUrl(normalized)) {
                            mediaTypes += type
                            imageValues += normalized
                        }
                    }

                    "imageset", "live2d" -> {
                        val images = extractImageUrlsFromAny(sourceUrl, rawValueAny)
                        if (images.isNotEmpty()) {
                            mediaTypes += type
                            imageValues += images
                        }
                    }

                    "video" -> {
                        val directVideo = normalizeMediaUrl(sourceUrl, rawValue)
                        val videos = buildList {
                            if (looksLikeVideoUrl(directVideo)) add(directVideo)
                            addAll(extractVideoUrlsFromAny(sourceUrl, rawValueAny))
                        }.distinct()
                        if (videos.isNotEmpty()) {
                            mediaTypes += type
                            videoValues += videos
                        }
                        val inlineImages = extractImageUrlsFromAny(sourceUrl, rawValueAny)
                        if (inlineImages.isNotEmpty()) imageValues += inlineImages
                    }

                    else -> {
                        val inlineImages = extractImageUrlsFromHtml(sourceUrl, rawValue)
                        if (inlineImages.isNotEmpty()) imageValues += inlineImages
                        videoValues += extractVideoUrlsFromAny(sourceUrl, rawValueAny)
                        val normalized = stripHtml(rawValue)
                        if (normalized.isNotBlank()) textValues += normalized
                    }
                }
            }
            if (firstImage.isBlank() && imageValues.isNotEmpty()) {
                firstImage = imageValues.first()
            }
            baseRows += GuideBaseRow(
                key = key,
                textValues = textValues,
                imageValues = imageValues.distinct(),
                videoValues = videoValues.distinct(),
                mediaTypes = mediaTypes
            )
        }

        val memoryUnlockLevel = run {
            val raw = baseRows.firstOrNull { it.key == "回忆大厅解锁等级" }
                ?.textValues
                ?.joinToString(" ")
                .orEmpty()
            val digits = Regex("""\d+""").find(raw)?.value.orEmpty()
            if (digits.isNotBlank()) digits else raw
        }

        fun containsAny(target: String, keywords: List<String>): Boolean {
            return keywords.any { key -> target.contains(key, ignoreCase = true) }
        }

        val skillKeywords = listOf(
            "技能", "EX", "普通技能", "被动技能", "辅助技能", "固有", "技能COST", "技能图标", "技能描述", "技能名称",
            "技能类型", "技能名词", "LV"
        )
        val profileKeywords = listOf(
            "学生信息", "角色名称", "全名", "假名注音", "简中译名", "繁中译名", "稀有度", "战术作用", "所属学园",
            "所属社团", "实装日期", "攻击类型", "防御类型", "位置", "市街", "屋外", "屋内", "武器类型", "年龄", "生日",
            "兴趣爱好", "声优", "画师", "介绍", "个人简介", "MomoTalk", "回忆大厅解锁等级", "同名角色", "角色头像"
        )
        val galleryKeywords = listOf(
            "立绘", "本家画", "TV动画设定图", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
            "互动家具", "角色表情", "设定集", "官方介绍", "情人节巧克力"
        )
        val growthKeywords = listOf(
            "装备", "专武", "能力解放", "礼物偏好", "羁绊", "升级材料", "所需", "LV", "T1", "T2", "爱用品", "初始数据",
            "攻击力增加", "生命值增加", "暴击值增加", "暴击伤害增加", "装弹数", "命中值", "治愈力", "稳定值",
            // 兼容专武面板使用“攻击力/生命值/治愈力”等简写字段
            "攻击力", "生命值", "防御力", "暴击", "爆伤", "命中", "闪避"
        )
        val voiceKeywords = listOf("通常", "战斗", "活动", "大厅及咖啡馆", "事件", "好感度")

        val skillRows = mutableListOf<BaGuideRow>()
        val profileRows = mutableListOf<BaGuideRow>()
        val growthRows = mutableListOf<BaGuideRow>()
        val voiceRows = mutableListOf<BaGuideRow>()
        val galleryItems = mutableListOf<BaGuideGalleryItem>()
        val stats = mutableListOf<Pair<String, String>>()
        val summaryCandidates = mutableListOf<String>()

        var inSkillBlock = false
        var inSkillGlossaryBlock = false
        var inWeaponBlock = false
        baseRows.forEach { row ->
            val key = row.key
            val value = row.textValues.joinToString(" / ")
            val imageUrl = row.imageValues.firstOrNull().orEmpty()
            val videoUrl = row.videoValues.firstOrNull().orEmpty()
            if (key.isBlank() && value.isBlank() && imageUrl.isBlank() && videoUrl.isBlank()) return@forEach

            val guideRow = BaGuideRow(
                key = key.ifBlank { "信息" },
                value = value,
                imageUrl = imageUrl,
                imageUrls = row.imageValues.distinct()
            )
            val normalizedKey = key.ifBlank { value }
                .replace("\n", " ")
                .trim()
            if (normalizedKey == "回忆大厅解锁等级") {
                return@forEach
            }
            if (normalizedKey.replace(" ", "").startsWith("回忆大厅文件")) {
                return@forEach
            }
            val isWeaponBlockStart = normalizedKey == "专武"
            val isWeaponBlockEnd = normalizedKey.contains("爱用品") ||
                normalizedKey.contains("专武考据") ||
                normalizedKey == "初始数据"
            val isSkillBlockStart = normalizedKey == "技能类型"
            val isSkillGlossaryStart = normalizedKey == "技能名词"
            val isSkillBlockEnd = isWeaponBlockStart ||
                normalizedKey.contains("升级材料") ||
                normalizedKey == "初始数据"
            val isSkillGlossaryEnd = isWeaponBlockStart ||
                normalizedKey.contains("升级材料") ||
                normalizedKey == "初始数据"
            if (isWeaponBlockStart) {
                inWeaponBlock = true
                inSkillBlock = false
                inSkillGlossaryBlock = false
            }
            if (isSkillBlockStart) {
                inSkillBlock = true
            }
            if (isSkillGlossaryStart) {
                inSkillGlossaryBlock = true
            }

            val isVoice = containsAny(normalizedKey, voiceKeywords)
            val matchesSkillKeywords = containsAny(normalizedKey, skillKeywords)
            val matchesGrowthKeywords = containsAny(normalizedKey, growthKeywords)
            val isLevelRow = key.trim().matches(Regex("""(?i)^LV\.?\d{1,2}$"""))
            val isSkill = (inSkillBlock && (isLevelRow || normalizedKey == "技能COST" || normalizedKey == "技能描述" || normalizedKey == "技能图标" || normalizedKey == "技能名称" || normalizedKey == "技能类型")) ||
                (inSkillGlossaryBlock && normalizedKey.isNotBlank() && !inWeaponBlock) ||
                (matchesSkillKeywords && !inWeaponBlock)
            val isGrowth = inWeaponBlock || (matchesGrowthKeywords && !isSkill)
            val isGallery = containsAny(normalizedKey, galleryKeywords)
            val isProfile = containsAny(normalizedKey, profileKeywords)

            when {
                isVoice && !inWeaponBlock -> voiceRows += guideRow
                isGrowth -> growthRows += guideRow
                isSkill -> skillRows += guideRow
                isGallery -> {
                    if (row.imageValues.isNotEmpty()) {
                        galleryItems += row.imageValues.mapIndexed { index, url ->
                            BaGuideGalleryItem(
                                title = if (row.imageValues.size > 1) "${guideRow.key.ifBlank { "影画" }} ${index + 1}" else guideRow.key.ifBlank { "影画" },
                                imageUrl = url,
                                mediaType = if (row.mediaTypes.contains("live2d")) "live2d" else "image",
                                mediaUrl = url,
                                memoryUnlockLevel = if (guideRow.key.startsWith("回忆大厅")) memoryUnlockLevel else ""
                            )
                        }
                    }
                    if (row.videoValues.isNotEmpty()) {
                        galleryItems += row.videoValues.mapIndexed { index, url ->
                            BaGuideGalleryItem(
                                title = if (row.videoValues.size > 1) "${guideRow.key.ifBlank { "影画" }} ${index + 1}" else guideRow.key.ifBlank { "影画" },
                                imageUrl = row.imageValues.firstOrNull().orEmpty(),
                                mediaType = "video",
                                mediaUrl = url,
                                memoryUnlockLevel = if (guideRow.key.startsWith("回忆大厅")) memoryUnlockLevel else ""
                            )
                        }
                    }
                    if (row.imageValues.isEmpty() && row.videoValues.isEmpty() && guideRow.value.isNotBlank()) {
                        profileRows += guideRow
                    }
                }

                isProfile -> profileRows += guideRow
                else -> {
                    if (guideRow.key.isNotBlank() && guideRow.value.isNotBlank()) {
                        profileRows += guideRow
                    }
                }
            }

            if (guideRow.key.isNotBlank() && guideRow.value.isNotBlank() && stats.none { it.first == guideRow.key }) {
                stats += guideRow.key to guideRow.value
                if (summaryCandidates.size < 4) {
                    summaryCandidates += "${guideRow.key}：${guideRow.value}"
                }
            }

            if (isWeaponBlockEnd) {
                inWeaponBlock = false
            }
            if (isSkillBlockEnd) {
                inSkillBlock = false
            }
            if (isSkillGlossaryEnd) {
                inSkillGlossaryBlock = false
            }
        }

        val distinctGallery = galleryItems
            .plus(galleryFromMediaTypes)
            .plus(galleryFromStyleData)
            .filter {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                media.isNotBlank()
            }
            .distinctBy {
                val media = it.mediaUrl.ifBlank { it.imageUrl }
                "${it.mediaType}|$media"
            }
            .sortedWith(
                compareBy<BaGuideGalleryItem> {
                    val title = it.title.replace(" ", "")
                    when {
                        title.startsWith("立绘") -> 0
                        title.startsWith("回忆大厅") && !title.startsWith("回忆大厅视频") -> 1
                        title.startsWith("回忆大厅视频") -> 2
                        else -> 3
                    }
                }.thenBy {
                    Regex("""(\d+)""").find(it.title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: Int.MAX_VALUE
                }
            )
            .take(100)

        GuideDetailExtract(
            imageUrl = firstImage,
            summary = summaryCandidates.joinToString(" · "),
            stats = stats.take(14),
            // 不截断技能行，避免长描述/术语图标在后段时被裁剪。
            skillRows = skillRows,
            profileRows = profileRows.take(120),
            galleryItems = distinctGallery,
            growthRows = growthRows.take(160),
            voiceRows = voiceRows.take(160),
            voiceLanguageHeaders = voiceLanguageHeaders,
            voiceEntries = voiceEntries,
            tabSkillIconUrl = tabIcons[GuideTab.Skills].orEmpty(),
            tabProfileIconUrl = tabIcons[GuideTab.Profile].orEmpty(),
            tabVoiceIconUrl = tabIcons[GuideTab.Voice].orEmpty(),
            tabGalleryIconUrl = tabIcons[GuideTab.Gallery].orEmpty(),
            tabSimulateIconUrl = tabIcons[GuideTab.Simulate].orEmpty()
        )
    }.getOrDefault(GuideDetailExtract())
}

private fun extractContentIdFromGuideUrl(sourceUrl: String): Long? {
    val target = normalizeGuideUrl(sourceUrl)
    if (target.isBlank()) return null
    val patterns = listOf(
        Regex("/v1/content/detail/(\\d+)"),
        Regex("/tj/(\\d+)\\.html"),
        Regex("/ba/(\\d+)\\.html"),
        Regex("/(\\d+)\\.html")
    )
    patterns.forEach { regex ->
        val id = regex.find(target)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (id != null && id > 0L) return id
    }
    val uri = runCatching { Uri.parse(target) }.getOrNull() ?: return null
    val qpKeys = listOf("content_id", "id", "cid")
    qpKeys.forEach { key ->
        val id = uri.getQueryParameter(key)?.toLongOrNull()
        if (id != null && id > 0L) return id
    }
    uri.pathSegments
        .asReversed()
        .firstNotNullOfOrNull { segment -> segment.toLongOrNull() }
        ?.let { id ->
            if (id > 0L) return id
        }
    return null
}

private fun fetchGuideInfoByApi(sourceUrl: String): BaStudentGuideInfo {
    val target = normalizeGuideUrl(sourceUrl)
    require(target.isNotBlank()) { "empty url" }
    val contentId = extractContentIdFromGuideUrl(target)
        ?: error("unable to resolve content_id")

    val refererPath = runCatching { Uri.parse(target).path.orEmpty() }
        .getOrDefault("/ba/$contentId.html")
        .ifBlank { "/ba/$contentId.html" }

    val body = GameKeeFetchHelper.fetchJson(
        pathOrUrl = "/v1/content/detail/$contentId",
        refererPath = refererPath,
        extraHeaders = mapOf(
            "device-num" to "1",
            "game-alias" to "ba"
        )
    )
    val root = JSONObject(body)
    if (root.optInt("code", -1) != 0) {
        error("api code=${root.optInt("code", -1)}")
    }
    val data = root.optJSONObject("data") ?: error("empty data")
    val title = data.optString("title").trim().ifBlank { "图鉴信息" }
    val subtitle = data.optJSONObject("game")?.optString("name").orEmpty().ifBlank { "GameKee" }
    val summaryFromApi = stripHtml(data.optString("summary"))
    val detail = parseGuideDetailFromContentJson(data.optString("content_json"), target)
    val imageFromData = firstImageFromAny(
        any = JSONObject().apply {
            put("thumb", data.opt("thumb"))
            put("image_list", data.opt("image_list"))
            put("thumb_list", data.opt("thumb_list"))
            put("video_list", data.opt("video_list"))
        },
        sourceUrl = target
    )
    val imageUrl = detail.imageUrl.ifBlank { imageFromData }
    val stats = detail.stats
    val summary = detail.summary
        .ifBlank { summaryFromApi }
        .ifBlank {
            stats.take(4).joinToString(" · ") { "${it.first}：${it.second}" }
        }
        .ifBlank { "暂无更多参数，可点击来源查看完整图鉴。" }
    val description = summaryFromApi.ifBlank { summary }

    return BaStudentGuideInfo(
        sourceUrl = target,
        title = title,
        subtitle = subtitle,
        description = description,
        imageUrl = imageUrl,
        summary = summary,
        stats = stats,
        skillRows = detail.skillRows,
        profileRows = detail.profileRows,
        galleryItems = detail.galleryItems,
        growthRows = detail.growthRows,
        voiceRows = detail.voiceRows,
        voiceLanguageHeaders = detail.voiceLanguageHeaders,
        voiceEntries = detail.voiceEntries,
        tabSkillIconUrl = detail.tabSkillIconUrl,
        tabProfileIconUrl = detail.tabProfileIconUrl,
        tabVoiceIconUrl = detail.tabVoiceIconUrl,
        tabGalleryIconUrl = detail.tabGalleryIconUrl,
        tabSimulateIconUrl = detail.tabSimulateIconUrl,
        syncedAtMs = System.currentTimeMillis()
    )
}

private fun fetchGuideInfoFromHtml(sourceUrl: String): BaStudentGuideInfo {
    val target = normalizeGuideUrl(sourceUrl)
    require(target.isNotBlank()) { "empty url" }
    val html = GameKeeFetchHelper.fetchHtml(
        pathOrUrl = target,
        refererPath = "/ba/"
    )
    if (html.isBlank()) error("empty html")

    val ogTitle = extractMeta(html, "og:title")
    val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    val rawTitle = if (ogTitle.isNotBlank()) ogTitle else stripHtml(titleTag)
    val title = rawTitle.ifBlank { "图鉴信息" }

    val siteName = extractMeta(html, "og:site_name").ifBlank { "GameKee" }
    val description = extractMeta(html, "og:description")
        .ifBlank { extractMeta(html, "description") }
        .ifBlank { "未解析到详细描述，可点击下方来源查看完整图鉴。" }
    val imageUrl = normalizeImageUrl(target, extractMeta(html, "og:image"))

    val paragraphRegex = Regex(
        "<p[^>]*>(.*?)</p>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val summary = paragraphRegex.findAll(html)
        .map { stripHtml(it.groupValues[1]) }
        .firstOrNull { it.length >= 12 }
        ?: description

    val stats = extractStatsFromHtml(html)

    return BaStudentGuideInfo(
        sourceUrl = target,
        title = title,
        subtitle = siteName,
        description = description,
        imageUrl = imageUrl,
        summary = summary,
        stats = stats,
        profileRows = stats.map { (k, v) -> BaGuideRow(k, v) },
        syncedAtMs = System.currentTimeMillis()
    )
}

fun fetchGuideInfo(sourceUrl: String): BaStudentGuideInfo {
    return runCatching { fetchGuideInfoByApi(sourceUrl) }
        .recoverCatching { fetchGuideInfoFromHtml(sourceUrl) }
        .getOrThrow()
}
