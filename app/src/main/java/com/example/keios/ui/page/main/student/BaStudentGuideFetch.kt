package com.example.keios.ui.page.main.student

import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
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
    val voiceCvJp: String = "",
    val voiceCvCn: String = "",
    val voiceCvByLanguage: Map<String, String> = emptyMap(),
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

private val voiceCategoryKeywords = listOf("通常", "大厅及咖啡馆", "战斗", "活动", "事件", "好感度", "成长")

private fun isVoiceCategoryKey(raw: String): Boolean {
    val key = stripHtml(raw)
        .replace(Regex("\\s+"), "")
        .trim()
    if (key.isBlank()) return false
    return voiceCategoryKeywords.any { keyword ->
        key.contains(keyword)
    }
}

private fun isVoiceBlockTailKey(raw: String): Boolean {
    val key = stripHtml(raw).trim()
    if (key.isBlank()) return false
    return key.startsWith("官方介绍") ||
        key.startsWith("角色表情") ||
        key.contains("设定集") ||
        key.contains("本家画") ||
        key.contains("原画师") ||
        key.contains("个人账号主页")
}

private fun normalizeGuideRowKey(raw: String): String {
    return stripHtml(raw)
        .replace(" ", "")
        .replace("　", "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
}

private fun isWeaponExtraAttributeKey(rawKey: String): Boolean {
    val key = normalizeGuideRowKey(rawKey)
    return Regex("""^附加属性\d+$""").matches(key)
}

private fun isTopDataStatKey(rawKey: String): Boolean {
    val key = normalizeGuideRowKey(rawKey)
    val statKeys = setOf(
        "攻击力", "防御力", "生命值", "治愈力",
        "命中值", "闪避值", "暴击值", "暴击伤害",
        "稳定值", "射程", "群控强化力", "群控抵抗力",
        "装弹数", "防御无视值", "受恢复率", "COST恢复力"
    )
    return key in statKeys
}

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
    // 避免把音频地址误判成图片（例如 BGM 的 .ogg）。
    if (Regex("""\.(mp3|ogg|wav|m4a|aac)(\?.*)?(#.*)?$""").containsMatchIn(lower)) {
        return false
    }
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

private fun extractGiftClassImageUrls(raw: String, classKeyword: String, sourceUrl: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val out = mutableListOf<String>()
    val imgTagRegex = Regex("""(?is)<img\b[^>]*>""")
    val classRegex = Regex("""(?i)\bclass\s*=\s*["']([^"']+)["']""")
    val srcRegex = Regex("""(?i)\bsrc\s*=\s*["']([^"']+)["']""")
    imgTagRegex.findAll(raw).forEach { match ->
        val tag = match.value
        val className = classRegex.find(tag)?.groupValues?.getOrNull(1).orEmpty()
        if (!className.contains(classKeyword, ignoreCase = true)) return@forEach
        val src = srcRegex.find(tag)?.groupValues?.getOrNull(1).orEmpty()
        val normalized = normalizeImageUrl(sourceUrl, src)
        if (looksLikeImageUrl(normalized)) {
            out += normalized
        }
    }
    return out.distinct()
}

private fun extractGiftImageUrlsFromCell(
    cell: JSONObject?,
    sourceUrl: String
): List<String> {
    if (cell == null) return emptyList()
    val type = cell.optString("type").trim().lowercase()
    val valueAny = cell.opt("value")
    val valueText = cell.optString("value").trim()
    val images = mutableListOf<String>()
    when (type) {
        "image" -> {
            if (!isPlaceholderMediaToken(valueText)) {
                val normalized = normalizeImageUrl(sourceUrl, valueText)
                if (looksLikeImageUrl(normalized)) {
                    images += normalized
                }
            }
        }

        "imageset", "live2d" -> {
            images += extractImageUrlsFromAny(sourceUrl, valueAny)
        }

        else -> {
            images += extractImageUrlsFromAny(sourceUrl, valueAny)
            images += extractImageUrlsFromHtml(sourceUrl, valueText)
        }
    }
    return images
        .map { it.trim() }
        .filter { looksLikeImageUrl(it) }
        .distinct()
}

private fun isLikelyGiftPreferenceIconUrl(url: String): Boolean {
    val value = url.trim()
    if (value.isBlank()) return false
    val sizeMatch = Regex("""/w_(\d{1,4})/h_(\d{1,4})/""")
        .find(value)
    val width = sizeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    val height = sizeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
    if (width != null && height != null) {
        if (width in 24..120 && height in 24..120) return true
    }
    return value.contains("gift", ignoreCase = true) &&
        !value.contains("furniture", ignoreCase = true) &&
        !value.contains("chocolate", ignoreCase = true)
}

private fun parseGiftPreferenceRowsFromBaseData(
    baseData: JSONArray,
    sourceUrl: String
): List<BaGuideRow> {
    val out = mutableListOf<BaGuideRow>()
    val giftHeaderKey = normalizeGuideRowKey("礼物偏好")
    val giftTailKeys = setOf(
        "初始数据", "顶级数据", "学生信息", "介绍", "配音",
        "专武", "爱用品", "能力解放", "装备", "羁绊等级奖励",
        "技能类型", "技能名词", "EX技能升级材料", "其他技能升级材料"
    ).map(::normalizeGuideRowKey).toSet()
    val giftStopKeyFragments = listOf(
        "互动家具", "情人节巧克力", "巧克力图", "巧克力名称", "巧克力简介",
        "相关同名角色", "同名角色名称", "配音", "配音语言", "配音大类",
        "官方介绍", "角色表情", "回忆大厅", "立绘", "本家画", "设定集", "TV动画设定图"
    ).map(::normalizeGuideRowKey)
    var inGiftBlock = false
    var giftIndex = 1
    var continuationQuota = 0
    var continuationEmojiImages: List<String> = emptyList()
    var continuationNote = ""

    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val keyCell = row.optJSONObject(0)
        val rawKey = keyCell?.optString("value").orEmpty().trim()
        val key = stripHtml(rawKey).trim()
        val normalizedKey = normalizeGuideRowKey(key)

        if (!inGiftBlock) {
            if (normalizedKey == giftHeaderKey) {
                inGiftBlock = true
            }
            continue
        }

        if (normalizedKey.isNotBlank() && normalizedKey in giftTailKeys) {
            inGiftBlock = false
            continuationQuota = 0
            continuationEmojiImages = emptyList()
            continuationNote = ""
            continue
        }
        if (normalizedKey.isNotBlank() && giftStopKeyFragments.any { fragment ->
                normalizedKey.contains(fragment)
            }
        ) {
            inGiftBlock = false
            continuationQuota = 0
            continuationEmojiImages = emptyList()
            continuationNote = ""
            continue
        }

        val keyGiftImages = extractGiftClassImageUrls(rawKey, "gift-img", sourceUrl)
        val keyEmojiImages = extractGiftClassImageUrls(rawKey, "gift-emoji", sourceUrl)
        val keyGenericImages = extractGiftImageUrlsFromCell(keyCell, sourceUrl)

        val rowGiftImages = mutableListOf<String>()
        val rowEmojiImages = mutableListOf<String>()
        val rowGenericImages = mutableListOf<String>()
        val rowTextNotes = mutableListOf<String>()
        for (j in 1 until row.length()) {
            val cell = row.optJSONObject(j) ?: continue
            val rawValue = cell.optString("value").trim()
            if (rawValue.isNotBlank()) {
                rowGiftImages += extractGiftClassImageUrls(rawValue, "gift-img", sourceUrl)
                rowEmojiImages += extractGiftClassImageUrls(rawValue, "gift-emoji", sourceUrl)
                val note = stripHtml(rawValue)
                if (note.isNotBlank()) {
                    rowTextNotes += note
                }
            }
            rowGenericImages += extractGiftImageUrlsFromCell(cell, sourceUrl)
        }

        if (keyGiftImages.isEmpty() && rowGiftImages.isEmpty() && rowGenericImages.isEmpty()) {
            continue
        }

        val explicitGiftImages = (keyGiftImages + rowGiftImages).distinct()
        val explicitEmojiImages = (keyEmojiImages + rowEmojiImages).distinct()
        val hasGiftIconKey = keyGenericImages.any(::isLikelyGiftPreferenceIconUrl) || keyGiftImages.isNotEmpty()
        val hasStructuredGiftMedia = explicitGiftImages.isNotEmpty()
        val isContinuationRow = normalizedKey.isBlank() &&
            continuationQuota > 0 &&
            rowGenericImages.isNotEmpty() &&
            keyGiftImages.isEmpty() &&
            rowGiftImages.isEmpty()
        if (!hasGiftIconKey && !hasStructuredGiftMedia && !isContinuationRow) {
            continue
        }
        val giftImages = when {
            isContinuationRow -> rowGenericImages.distinct()
            explicitGiftImages.isNotEmpty() -> (explicitGiftImages + rowGenericImages).distinct()
            hasGiftIconKey && rowGenericImages.isNotEmpty() -> rowGenericImages.distinct()
            else -> keyGenericImages.distinct()
        }
        if (giftImages.isEmpty()) continue

        val emojiImages = when {
            isContinuationRow -> continuationEmojiImages
            explicitEmojiImages.isNotEmpty() -> explicitEmojiImages
            keyGenericImages.isNotEmpty() -> keyGenericImages.filterNot { it in giftImages }.ifEmpty {
                keyGenericImages.take(1)
            }
            else -> emptyList()
        }
        val note = rowTextNotes.firstOrNull { text ->
            text.isNotBlank() && !looksLikeImageUrl(normalizeImageUrl(sourceUrl, text))
        }.orEmpty().ifBlank {
            if (isContinuationRow) continuationNote else ""
        }

        giftImages.forEachIndexed { index, giftImage ->
            val emojiImage = when {
                emojiImages.isEmpty() -> ""
                emojiImages.size == giftImages.size -> emojiImages.getOrElse(index) { emojiImages.first() }
                else -> emojiImages.first()
            }
            val imageUrls = buildList {
                add(giftImage)
                if (emojiImage.isNotBlank() && emojiImage != giftImage) {
                    add(emojiImage)
                }
            }
            out += BaGuideRow(
                key = "礼物偏好礼物$giftIndex",
                value = note,
                imageUrl = giftImage,
                imageUrls = imageUrls
            )
            giftIndex += 1
        }

        if (isContinuationRow) {
            continuationQuota = (continuationQuota - 1).coerceAtLeast(0)
        } else if (hasGiftIconKey || hasStructuredGiftMedia) {
            continuationQuota = 3
            continuationEmojiImages = emojiImages
            continuationNote = note
        } else if (normalizedKey.isNotBlank()) {
            continuationQuota = 0
            continuationEmojiImages = emptyList()
            continuationNote = ""
        }
    }

    return out
        .distinctBy { row ->
            val packedImages = row.imageUrls.joinToString("|")
            "${row.key.trim()}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
        }
        .take(80)
}

private fun parseGalleryItemsFromBaseData(baseData: JSONArray, sourceUrl: String): List<BaGuideGalleryItem> {
    val out = mutableListOf<BaGuideGalleryItem>()
    val galleryKeywords = listOf(
        "立绘", "本家画", "TV动画设定图", "回忆大厅视频", "回忆大厅", "PV", "Live", "巧克力图",
        "互动家具", "角色表情", "表情", "角色演示", "设定集", "官方介绍", "官方衍生", "情人节巧克力", "BGM"
    )
    val galleryContextStartKeywords = galleryKeywords + listOf("视频")
    val nonGallerySectionKeywords = listOf(
        "技能", "技能类型", "技能名词", "EX技能升级材料", "其他技能升级材料",
        "专武", "爱用品", "能力解放", "礼物偏好", "初始数据", "顶级数据",
        "学生信息", "介绍", "配音"
    )
    val nonGalleryFallbackKeywords = listOf(
        "头像", "技能", "图标", "语音", "台词", "专武", "武器", "装备", "材料",
        "能力解放", "礼物偏好", "初始数据", "学生信息", "角色名称", "稀有度", "所属学园", "所属社团",
        "战术作用", "攻击类型", "防御类型", "位置", "武器类型", "市街", "屋外", "屋内", "室内"
    )

    fun noteForImageIndex(texts: List<String>, index: Int, imageCount: Int): String {
        val normalized = texts.map { it.trim() }.filter { it.isNotBlank() }
        if (normalized.isEmpty()) return ""
        if (imageCount <= 1) return normalized.joinToString(" / ")
        if (normalized.size == imageCount) return normalized.getOrElse(index) { "" }
        if (normalized.size == 1) return if (index == imageCount - 1) normalized.first() else ""
        return normalized.getOrElse(index) { normalized.last() }
    }

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

    var inGalleryContext = false
    var lastGalleryTitle = ""
    for (i in 0 until baseData.length()) {
        val row = baseData.optJSONArray(i) ?: continue
        if (row.length() == 0) continue
        val key = stripHtml((row.optJSONObject(0)?.optString("value") ?: "").trim())
        if (key == "回忆大厅解锁等级") continue
        if (key.replace(" ", "").startsWith("回忆大厅文件")) continue
        val isGalleryContextStart = galleryContextStartKeywords.any { key.contains(it, ignoreCase = true) }
        val isNonGallerySectionStart = key.isNotBlank() && nonGallerySectionKeywords.any {
            key.contains(it, ignoreCase = true)
        }
        if (isNonGallerySectionStart && !isGalleryContextStart) {
            inGalleryContext = false
            lastGalleryTitle = ""
        }
        if (isGalleryContextStart) {
            inGalleryContext = true
            if (key.isNotBlank()) {
                lastGalleryTitle = key
            }
        }

        val rowImages = linkedSetOf<String>()
        val rowVideos = linkedSetOf<String>()
        val rowAudios = linkedSetOf<String>()
        val rowTexts = mutableListOf<String>()

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
                "audio" -> {
                    val direct = normalizeMediaUrl(sourceUrl, valueText)
                    if (isAudioUrl(direct)) rowAudios += direct
                    rowAudios += extractAudioUrlsFromRaw(sourceUrl, valueText)
                }
                else -> {
                    rowImages += extractImageUrlsFromHtml(sourceUrl, valueText)
                    rowImages += extractImageUrlsFromAny(sourceUrl, valueAny)
                    rowVideos += extractVideoUrlsFromAny(sourceUrl, valueAny)
                    rowAudios += extractAudioUrlsFromRaw(sourceUrl, valueText)
                    val plain = stripHtml(valueText)
                    if (plain.isNotBlank()) rowTexts += plain
                }
            }
        }

        val hasMedia = rowImages.isNotEmpty() || rowVideos.isNotEmpty() || rowAudios.isNotEmpty()
        val isFallbackGallery =
            hasMedia &&
                inGalleryContext &&
                nonGalleryFallbackKeywords.none { key.contains(it, ignoreCase = true) }
        if (!isGalleryKey(key) && !isFallbackGallery) continue
        val galleryTitle = key.ifBlank { lastGalleryTitle.ifBlank { "影画" } }

        if (rowImages.isNotEmpty()) {
            out += rowImages.mapIndexed { index, imageUrl ->
                BaGuideGalleryItem(
                    title = if (rowImages.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = imageUrl,
                    mediaType = "image",
                    mediaUrl = imageUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = noteForImageIndex(rowTexts, index, rowImages.size)
                )
            }
        }
        if (rowVideos.isNotEmpty()) {
            val videoNote = rowTexts.joinToString(" / ").trim()
            out += rowVideos.mapIndexed { index, videoUrl ->
                BaGuideGalleryItem(
                    title = if (rowVideos.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "video",
                    mediaUrl = videoUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = videoNote
                )
            }
        }
        if (rowAudios.isNotEmpty()) {
            val audioNote = rowTexts.joinToString(" / ").trim()
            out += rowAudios.mapIndexed { index, audioUrl ->
                BaGuideGalleryItem(
                    title = if (rowAudios.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                    imageUrl = rowImages.firstOrNull().orEmpty(),
                    mediaType = "audio",
                    mediaUrl = audioUrl,
                    memoryUnlockLevel = if (key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                    note = audioNote
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

private fun normalizeVoiceLanguageLabelRaw(raw: String): String {
    return stripHtml(raw)
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
        .trim()
}

private fun canonicalVoiceLanguageLabel(raw: String): String {
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

private fun defaultVoiceLanguageLabelForIndex(index: Int): String {
    return when (index) {
        0 -> "日配"
        1 -> "中配"
        2 -> "韩配"
        else -> "语言${index + 1}"
    }
}

private fun looksLikeVoiceLanguageLabel(raw: String): Boolean {
    val canonical = canonicalVoiceLanguageLabel(raw)
    if (canonical.isBlank()) return false
    if (canonical in setOf("日配", "中配", "韩配", "官翻")) return true
    val normalized = normalizeVoiceLanguageLabelRaw(canonical)
    return normalized.contains("配") || normalized.contains("翻")
}

private fun voiceLineDisplayPriority(label: String): Int {
    return when (canonicalVoiceLanguageLabel(label)) {
        "日配" -> 0
        "中配" -> 1
        "官翻" -> 2
        "韩配" -> 3
        else -> 4
    }
}

private fun sortVoiceLinePairsForDisplay(
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

private fun parseVoiceDataFromBaseData(
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

private fun parseVoiceCvByLanguageFromBaseData(baseData: JSONArray): Map<String, String> {
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
        return raw.trim()
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

private fun String?.orElse(fallback: String): String {
    val value = this.orEmpty()
    return if (value.isBlank()) fallback else value
}

private fun deriveVoiceCvLegacyFields(cvByLanguage: Map<String, String>): Pair<String, String> {
    val jp = cvByLanguage.entries.firstOrNull { (key, _) ->
        canonicalVoiceLanguageLabel(key) == "日配"
    }?.value.orEmpty()
    val cn = cvByLanguage.entries.firstOrNull { (key, _) ->
        canonicalVoiceLanguageLabel(key) == "中配"
    }?.value.orEmpty()
    return jp to cn
}

private fun mergeVoiceLanguageHeaders(
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

private fun normalizeVoiceEntriesWithHeaderCount(
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
        val (rawVoiceLanguageHeaders, rawVoiceEntries) = parseVoiceDataFromBaseData(baseData, sourceUrl)
        val voiceCvByLanguage = parseVoiceCvByLanguageFromBaseData(baseData)
        val voiceLanguageHeaders = mergeVoiceLanguageHeaders(
            rawHeaders = rawVoiceLanguageHeaders,
            voiceEntries = rawVoiceEntries,
            cvByLanguage = voiceCvByLanguage
        )
        val voiceEntries = normalizeVoiceEntriesWithHeaderCount(
            entries = rawVoiceEntries,
            headerCount = voiceLanguageHeaders.size
        )
        val (voiceCvJp, voiceCvCn) = deriveVoiceCvLegacyFields(voiceCvByLanguage)
        val galleryFromMediaTypes = parseGalleryItemsFromBaseData(baseData, sourceUrl)
        val giftPreferenceRows = parseGiftPreferenceRowsFromBaseData(baseData, sourceUrl)
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

        fun isGrowthTitleVoiceKey(raw: String): Boolean {
            val normalized = raw.replace(" ", "").lowercase()
            if (normalized.isBlank()) return false
            return (normalized.contains("成长") && normalized.contains("title")) ||
                normalized.contains("成长标题") ||
                normalized.contains("growthtitle") ||
                normalized.contains("growth_title")
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
            "互动家具", "角色表情", "设定集", "官方介绍", "官方衍生", "情人节巧克力", "BGM"
        )
        val galleryContextStartKeywords = galleryKeywords + listOf("视频")
        val nonGallerySectionKeywords = listOf(
            "技能", "技能类型", "技能名词", "EX技能升级材料", "其他技能升级材料",
            "专武", "爱用品", "能力解放", "礼物偏好", "初始数据", "顶级数据",
            "学生信息", "介绍", "配音"
        )
        val nonGalleryFallbackKeywords = listOf(
            "头像", "技能", "图标", "语音", "台词", "专武", "武器", "装备", "材料",
            "能力解放", "礼物偏好", "初始数据", "学生信息", "角色名称", "稀有度", "所属学园", "所属社团",
            "战术作用", "攻击类型", "防御类型", "位置", "武器类型", "市街", "屋外", "屋内", "室内"
        )
        val growthKeywords = listOf(
            "装备", "专武", "能力解放", "礼物偏好", "羁绊", "升级材料", "所需", "LV", "T1", "T2", "爱用品", "初始数据",
            "攻击力增加", "生命值增加", "暴击值增加", "暴击伤害增加", "装弹数", "命中值", "治愈力", "稳定值",
            // 兼容专武面板使用“攻击力/生命值/治愈力”等简写字段
            "攻击力", "生命值", "防御力", "暴击", "爆伤", "命中", "闪避"
        )
        val voiceKeywords = listOf("通常", "战斗", "活动", "大厅及咖啡馆", "事件", "好感度", "成长")

        val skillRows = mutableListOf<BaGuideRow>()
        val profileRows = mutableListOf<BaGuideRow>()
        val growthRows = mutableListOf<BaGuideRow>()
        val voiceRows = mutableListOf<BaGuideRow>()
        val galleryItems = mutableListOf<BaGuideGalleryItem>()
        val stats = mutableListOf<Pair<String, String>>()
        val summaryCandidates = mutableListOf<String>()

        fun noteForGalleryImage(textValues: List<String>, index: Int, imageCount: Int): String {
            val normalized = textValues.map { it.trim() }.filter { it.isNotBlank() }
            if (normalized.isEmpty()) return ""
            if (imageCount <= 1) return normalized.joinToString(" / ")
            if (normalized.size == imageCount) return normalized.getOrElse(index) { "" }
            if (normalized.size == 1) return if (index == imageCount - 1) normalized.first() else ""
            return normalized.getOrElse(index) { normalized.last() }
        }

        var inSkillBlock = false
        var inSkillGlossaryBlock = false
        var inWeaponBlock = false
        var inGalleryContext = false
        var inVoiceContext = false
        var currentVoiceSection = ""
        var inTopDataContext = false
        var lastGalleryTitle = ""
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
            val normalizedGuideKey = normalizeGuideRowKey(normalizedKey)
            if (normalizedGuideKey == "顶级数据") {
                inTopDataContext = true
            } else if (inTopDataContext && (
                    normalizedGuideKey == "专武" ||
                        normalizedGuideKey == "装备" ||
                        normalizedGuideKey == "爱用品" ||
                        normalizedGuideKey == "能力解放" ||
                        normalizedGuideKey.contains("羁绊等级奖励")
                    )
            ) {
                inTopDataContext = false
            }
            if (normalizedKey == "配音语言") {
                inVoiceContext = true
                currentVoiceSection = ""
                return@forEach
            }
            if (normalizedKey == "配音" || normalizedKey == "配音大类") {
                return@forEach
            }
            val isVoiceCategoryRow = isVoiceCategoryKey(normalizedKey)
            if (isVoiceCategoryRow) {
                inVoiceContext = true
                currentVoiceSection = normalizedKey
            } else if (inVoiceContext && isVoiceBlockTailKey(normalizedKey)) {
                inVoiceContext = false
                currentVoiceSection = ""
            }
            val normalizedVoiceTexts = row.textValues
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val hasMeaningfulVoicePayload = normalizedVoiceTexts.size >= 2
            val isVoiceByContext = inVoiceContext && (
                isVoiceCategoryRow || (currentVoiceSection.isNotBlank() && hasMeaningfulVoicePayload)
            )
            if (inVoiceContext &&
                !isVoiceCategoryRow &&
                !hasMeaningfulVoicePayload &&
                row.imageValues.isEmpty() &&
                row.videoValues.isEmpty()
            ) {
                // Skip placeholder rows such as "被CC5" that should not leak into profile.
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
            val isGalleryContextStart = galleryContextStartKeywords.any {
                normalizedKey.contains(it, ignoreCase = true)
            }
            val isNonGallerySectionStart = normalizedKey.isNotBlank() && nonGallerySectionKeywords.any {
                normalizedKey.contains(it, ignoreCase = true)
            }
            if (isNonGallerySectionStart && !isGalleryContextStart) {
                inGalleryContext = false
                lastGalleryTitle = ""
            }
            if (isGalleryContextStart) {
                inGalleryContext = true
                if (guideRow.key.isNotBlank()) {
                    lastGalleryTitle = guideRow.key
                }
            }

            val isVoice = containsAny(normalizedKey, voiceKeywords) || isGrowthTitleVoiceKey(normalizedKey) || isVoiceByContext
            val matchesSkillKeywords = containsAny(normalizedKey, skillKeywords)
            val matchesGrowthKeywords = containsAny(normalizedKey, growthKeywords)
            val isSkillMigratedRow =
                isWeaponExtraAttributeKey(normalizedKey) ||
                    normalizedGuideKey == "25级" ||
                    normalizedGuideKey == "顶级数据" ||
                    (inTopDataContext && isTopDataStatKey(normalizedKey))
            val isLevelRow = key.trim().matches(Regex("""(?i)^LV\.?\d{1,2}$"""))
            val isSkill = (inSkillBlock && (isLevelRow || normalizedKey == "技能COST" || normalizedKey == "技能描述" || normalizedKey == "技能图标" || normalizedKey == "技能名称" || normalizedKey == "技能类型")) ||
                (inSkillGlossaryBlock && normalizedKey.isNotBlank() && !inWeaponBlock) ||
                (matchesSkillKeywords && !inWeaponBlock) ||
                isSkillMigratedRow
            val isGrowth = inWeaponBlock || (matchesGrowthKeywords && !isSkill)
            val isProfile = containsAny(normalizedKey, profileKeywords)
            val hasMedia = row.imageValues.isNotEmpty() || row.videoValues.isNotEmpty()
            val isFallbackGallery =
                hasMedia &&
                !isSkill &&
                !isGrowth &&
                !isVoice &&
                !isProfile &&
                inGalleryContext &&
                nonGalleryFallbackKeywords.none { normalizedKey.contains(it, ignoreCase = true) }
            val isGallery = containsAny(normalizedKey, galleryKeywords) || isFallbackGallery
            val galleryTitle = guideRow.key.ifBlank { lastGalleryTitle.ifBlank { "影画" } }

            when {
                isVoice && !inWeaponBlock -> voiceRows += guideRow
                isGrowth -> growthRows += guideRow
                isSkill -> skillRows += guideRow
                isGallery -> {
                    if (row.imageValues.isNotEmpty()) {
                        galleryItems += row.imageValues.mapIndexed { index, url ->
                            BaGuideGalleryItem(
                                title = if (row.imageValues.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                                imageUrl = url,
                                mediaType = if (row.mediaTypes.contains("live2d")) "live2d" else "image",
                                mediaUrl = url,
                                memoryUnlockLevel = if (guideRow.key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                                note = noteForGalleryImage(row.textValues, index, row.imageValues.size)
                            )
                        }
                    }
                    if (row.videoValues.isNotEmpty()) {
                        val videoNote = row.textValues.joinToString(" / ").trim()
                        galleryItems += row.videoValues.mapIndexed { index, url ->
                            BaGuideGalleryItem(
                                title = if (row.videoValues.size > 1) "$galleryTitle ${index + 1}" else galleryTitle,
                                imageUrl = row.imageValues.firstOrNull().orEmpty(),
                                mediaType = "video",
                                mediaUrl = url,
                                memoryUnlockLevel = if (guideRow.key.startsWith("回忆大厅")) memoryUnlockLevel else "",
                                note = videoNote
                            )
                        }
                    }
                    val isPureMediaText = row.textValues.any { text ->
                        val normalized = normalizeMediaUrl(sourceUrl, text)
                        isAudioUrl(normalized) || looksLikeVideoUrl(normalized) || looksLikeImageUrl(normalized)
                    }
                    if (row.imageValues.isEmpty() &&
                        row.videoValues.isEmpty() &&
                        guideRow.value.isNotBlank() &&
                        !isPureMediaText
                    ) {
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

        fun normalizedGalleryTitle(raw: String): String = raw.replace(" ", "").trim()

        fun galleryCategoryOrder(rawTitle: String): Int {
            val title = normalizedGalleryTitle(rawTitle)
            return when {
                title.startsWith("立绘") -> 0
                title.startsWith("回忆大厅") && !title.startsWith("回忆大厅视频") -> 1
                title.startsWith("回忆大厅视频") -> 2
                title.startsWith("BGM") -> 3
                title.startsWith("官方介绍") -> 4
                title.startsWith("本家画") -> 5
                title.startsWith("官方衍生") -> 6
                title.startsWith("TV动画设定图") -> 7
                title.startsWith("设定集") -> 8
                title.startsWith("角色表情") -> 9
                title.startsWith("互动家具") -> 10
                title.startsWith("情人节巧克力") -> 11
                title.startsWith("巧克力图") -> 12
                title.startsWith("PV") -> 13
                title.startsWith("角色演示") -> 14
                title.startsWith("Live") -> 15
                else -> 99
            }
        }

        fun galleryTitleGroupKey(rawTitle: String): String {
            val normalized = normalizedGalleryTitle(rawTitle)
            return normalized.replace(Regex("""\d+$"""), "")
        }

        fun galleryItemIndex(rawTitle: String): Int {
            return Regex("""(\d+)(?!.*\d)""")
                .find(normalizedGalleryTitle(rawTitle))
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: Int.MAX_VALUE
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
                compareBy<BaGuideGalleryItem> { galleryCategoryOrder(it.title) }
                    .thenBy { galleryTitleGroupKey(it.title) }
                    .thenBy { galleryItemIndex(it.title) }
            )
            .take(100)
        val mergedProfileRows = (profileRows + giftPreferenceRows)
            .distinctBy { row ->
                val packedImages = row.imageUrls.joinToString("|")
                "${row.key.trim()}|${row.value.trim()}|${row.imageUrl.trim()}|$packedImages"
            }
            .take(180)

        GuideDetailExtract(
            imageUrl = firstImage,
            summary = summaryCandidates.joinToString(" · "),
            stats = stats.take(14),
            // 不截断技能行，避免长描述/术语图标在后段时被裁剪。
            skillRows = skillRows,
            profileRows = mergedProfileRows,
            galleryItems = distinctGallery,
            growthRows = growthRows.take(160),
            voiceRows = voiceRows.take(160),
            voiceCvJp = voiceCvJp,
            voiceCvCn = voiceCvCn,
            voiceCvByLanguage = voiceCvByLanguage,
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
        voiceCvJp = detail.voiceCvJp,
        voiceCvCn = detail.voiceCvCn,
        voiceCvByLanguage = detail.voiceCvByLanguage,
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
