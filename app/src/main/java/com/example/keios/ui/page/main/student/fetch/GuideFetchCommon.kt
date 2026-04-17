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

internal val GUIDE_CONTENT_ID_REGEX_PATTERNS = listOf(
    Regex("/v1/content/detail/(\\d+)"),
    Regex("/tj/(\\d+)\\.html"),
    Regex("/ba/(\\d+)\\.html"),
    Regex("/(\\d+)\\.html")
)

internal val GUIDE_CONTENT_ID_QUERY_KEYS = listOf("content_id", "id", "cid")

internal fun decodeBasicHtmlEntity(raw: String): String {
    return raw
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

internal fun stripHtml(raw: String): String {
    return decodeBasicHtmlEntity(raw.replace(Regex("<[^>]+>"), " "))
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun extractMeta(html: String, key: String): String {
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

internal fun isMeaningfulGuideRowValue(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    val compact = value
        .replace(" ", "")
        .replace("　", "")
        .trim()
    if (compact.isBlank()) return false
    if (compact in setOf("-", "--", "—", "／", "/", "\\", "|", "｜")) return false
    if (compact.matches(Regex("""^[\\/|｜／,，;；:：._\-—~·*]+$"""))) return false
    val lower = compact.lowercase()
    if (lower == "n" || lower == "null" || lower == "undefined" || lower == "nan") return false
    return true
}

internal fun normalizeImageUrl(sourceUrl: String, imageRaw: String): String {
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

internal fun extractStatsFromHtml(html: String): List<Pair<String, String>> {
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

internal data class GuideDetailExtract(
    val imageUrl: String = "",
    val summary: String = "",
    val stats: List<Pair<String, String>> = emptyList(),
    val skillRows: List<BaGuideRow> = emptyList(),
    val profileRows: List<BaGuideRow> = emptyList(),
    val galleryItems: List<BaGuideGalleryItem> = emptyList(),
    val growthRows: List<BaGuideRow> = emptyList(),
    val simulateRows: List<BaGuideRow> = emptyList(),
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

internal data class GuideBaseRow(
    val key: String,
    val textValues: List<String>,
    val imageValues: List<String>,
    val videoValues: List<String> = emptyList(),
    val mediaTypes: Set<String> = emptySet()
)

internal val voiceCategoryKeywords = listOf("通常", "大厅及咖啡馆", "战斗", "活动", "事件", "好感度", "成长")

internal fun isVoiceCategoryKey(raw: String): Boolean {
    val key = stripHtml(raw)
        .replace(Regex("\\s+"), "")
        .trim()
    if (key.isBlank()) return false
    return voiceCategoryKeywords.any { keyword ->
        key.contains(keyword)
    }
}

internal fun isVoiceBlockTailKey(raw: String): Boolean {
    val key = stripHtml(raw).trim()
    if (key.isBlank()) return false
    return key.startsWith("官方介绍") ||
        key.startsWith("角色表情") ||
        key.contains("设定集") ||
        key.contains("本家画") ||
        key.contains("原画师") ||
        key.contains("个人账号主页")
}

internal fun normalizeGuideRowKey(raw: String): String {
    return stripHtml(raw)
        .replace(" ", "")
        .replace("　", "")
        .replace("（", "(")
        .replace("）", ")")
        .trim()
}

internal fun isWeaponExtraAttributeKey(rawKey: String): Boolean {
    val key = normalizeGuideRowKey(rawKey)
    return Regex("""^附加属性\d+$""").matches(key)
}

internal fun isTopDataStatKey(rawKey: String): Boolean {
    val key = normalizeGuideRowKey(rawKey)
    val statKeys = setOf(
        "攻击力", "防御力", "生命值", "治愈力",
        "命中值", "闪避值", "暴击值", "暴击伤害",
        "稳定值", "射程", "群控强化力", "群控抵抗力",
        "装弹数", "防御无视值", "受恢复率", "COST恢复力"
    )
    return key in statKeys
}

internal fun isPlaceholderMediaToken(raw: String): Boolean {
    val value = raw.trim().lowercase()
    if (value.isBlank()) return true
    if (value == "n" || value == "null" || value == "undefined" || value == "nan") return true
    return value.matches(Regex("""^\d+$"""))
}

internal fun hasInvalidMediaTail(rawUrl: String): Boolean {
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

internal fun looksLikeImageUrl(raw: String): Boolean {
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

internal fun looksLikeVideoUrl(raw: String): Boolean {
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

fun extractGuideContentIdFromUrl(sourceUrl: String): Long? {
    val target = normalizeGuideUrl(sourceUrl)
    if (target.isBlank()) return null
    GUIDE_CONTENT_ID_REGEX_PATTERNS.forEach { regex ->
        val id = regex.find(target)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (id != null && id > 0L) return id
    }
    val uri = runCatching { Uri.parse(target) }.getOrNull() ?: return null
    GUIDE_CONTENT_ID_QUERY_KEYS.forEach { key ->
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
