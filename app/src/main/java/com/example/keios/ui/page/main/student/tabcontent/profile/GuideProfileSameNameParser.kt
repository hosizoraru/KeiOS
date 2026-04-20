package com.example.keios.ui.page.main.student.tabcontent.profile

import android.net.Uri
import com.example.keios.ui.page.main.student.BaGuideRow
import com.example.keios.ui.page.main.student.containsGuideWebLink
import com.example.keios.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import com.example.keios.ui.page.main.student.isRenderableGalleryImageUrl

internal data class SameNameRoleItem(
    val name: String,
    val linkUrl: String,
    val imageUrl: String
)

internal fun isSameNameRoleRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    return key == relatedSameNameRoleHeaderKey || key == sameNameRoleNameRowKey
}

internal fun splitRoleRowTokens(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .split(Regex("""\s*(?:/|／|\||｜|\n)\s*"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

internal val sameNameGuideEmbeddedLinkRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
internal val sameNameGuidePathPattern = Regex("""^/(?:ba/tj/\d+(?:\.html)?|ba/\d+(?:\.html)?|v1/content/detail/\d+)$""")

internal fun sanitizeSameNameLinkToken(raw: String): String {
    return raw.trim().trimEnd(')', ']', '}', ',', '。', '，', ';', '；')
}

internal fun extractSameNameGuideLink(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""

    val directCandidates = buildList {
        val cleaned = sanitizeSameNameLinkToken(source)
        if (cleaned.startsWith("http://", ignoreCase = true) || cleaned.startsWith("https://", ignoreCase = true)) {
            add(cleaned)
        } else if (cleaned.startsWith("www.", ignoreCase = true)) {
            add("https://$cleaned")
        } else if (cleaned.matches(Regex("""^\d{4,}$"""))) {
            add("https://www.gamekee.com/ba/tj/$cleaned.html")
        } else if (cleaned.startsWith("/") && sameNameGuidePathPattern.matches(cleaned)) {
            add(normalizeGuideUrl(cleaned))
        }
        addAll(
            sameNameGuideEmbeddedLinkRegex.findAll(source).map { match ->
                sanitizeSameNameLinkToken(match.value)
            }
        )
    }.distinct()

    for (candidate in directCandidates) {
        val normalized = normalizeGuideUrl(candidate)
        if (normalized.isBlank()) continue
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: continue
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty()
        val hostAccepted = host == "www.gamekee.com" || host == "gamekee.com"
        val pathAccepted = sameNameGuidePathPattern.matches(path)
        if (!hostAccepted || !pathAccepted) continue
        val contentId = extractGuideContentIdFromUrl(normalized) ?: continue
        if (contentId <= 0L) continue
        return "https://www.gamekee.com/ba/tj/$contentId.html"
    }
    return ""
}

internal val sameNameRoleHintKeywords = listOf(
    "暂无同名角色",
    "未填写",
    "占位",
    "说明",
    "备注",
    "复制",
    "不用写",
    "暂时没",
    "待补充"
)

internal fun isSameNameRoleHintText(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    val compact = value
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
    if (compact.length >= 20) return true
    return sameNameRoleHintKeywords.any { keyword ->
        compact.contains(keyword.lowercase())
    }
}

internal fun extractSameNameRoleHint(row: BaGuideRow): String? {
    if (normalizeProfileFieldKey(row.key) != relatedSameNameRoleHeaderKey) return null
    val rawValue = row.value.trim().trim('*')
    if (rawValue.isBlank()) return null
    if (isProfileValuePlaceholder(rawValue)) return null
    val hasLink = extractProfileExternalLink(rawValue).isNotBlank()
    val hasImage = buildList {
        add(row.imageUrl.trim())
        addAll(row.imageUrls.map { it.trim() })
    }.any { candidate ->
        candidate.isNotBlank() && isRenderableGalleryImageUrl(candidate)
    }
    if (hasLink || hasImage) return null
    if (!isSameNameRoleHintText(rawValue)) return null
    return rawValue
}

internal fun buildSameNameRoleItems(rows: List<BaGuideRow>): List<SameNameRoleItem> {
    if (rows.isEmpty()) return emptyList()
    val items = rows.mapNotNull { row ->
        val normalizedKey = normalizeProfileFieldKey(row.key)
        if (normalizedKey != sameNameRoleNameRowKey && normalizedKey != relatedSameNameRoleHeaderKey) {
            return@mapNotNull null
        }
        val tokens = splitRoleRowTokens(row.value)
        val link = sequence<String> {
            tokens.forEach { yield(it) }
            yield(row.value)
        }.map { token -> extractSameNameGuideLink(token) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val name = tokens.firstOrNull { token ->
            token.isNotBlank() &&
                !isProfileValuePlaceholder(token) &&
                extractProfileExternalLink(token).isBlank() &&
                !isSameNameRoleHintText(token)
        }.orEmpty()
        val image = (row.imageUrls + row.imageUrl)
            .firstOrNull { candidate -> isRenderableGalleryImageUrl(candidate) }
            .orEmpty()
        if (name.isBlank() && link.isBlank() && image.isBlank()) {
            return@mapNotNull null
        }
        if (name.isBlank() && link.isBlank() && isSameNameRoleHintText(row.value)) {
            return@mapNotNull null
        }
        val fallbackName = when {
            link.isNotBlank() -> fallbackProfileLinkTitle(link)
            else -> "同名角色"
        }
        SameNameRoleItem(
            name = name.ifBlank { fallbackName },
            linkUrl = link,
            imageUrl = image
        )
    }

    return items.distinctBy { item ->
        "${item.name.trim()}|${item.linkUrl.trim()}|${item.imageUrl.trim()}"
    }
}

internal val galleryRelatedProfileLinkKeyTokens = listOf(
    "影画相关链接",
    "相关链接",
    "来源链接",
    "个人账号主页",
    "账号主页",
    "个人主页",
    "主页链接",
    "主页"
).map(::normalizeProfileFieldKey)

internal fun isGalleryRelatedProfileLinkRow(row: BaGuideRow): Boolean {
    val key = normalizeProfileFieldKey(row.key)
    if (key.isBlank()) return false
    val hasGalleryLinkKey = galleryRelatedProfileLinkKeyTokens.any { token ->
        token.isNotBlank() && key.contains(token)
    }
    if (!hasGalleryLinkKey) return false
    val linkSource = buildString {
        append(row.value)
        append(' ')
        append(row.key)
    }
    return containsGuideWebLink(linkSource)
}
