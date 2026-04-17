package com.example.keios.ui.page.main.student

import android.net.Uri

private val guideWebLinkRegex = Regex(
    """(?i)((?:https?://|www\.)[^\s<>"'，。；;）)】]+)"""
)

internal fun normalizeGalleryTitle(raw: String): String {
    return raw.replace(Regex("\\s+"), "").trim()
}

internal fun normalizeGuideWebLinkForOpen(raw: String): String {
    val value = raw.trim()
        .trimEnd('。', '，', ',', ';', '；', '）', ')', '】', ']', '》', '>')
    if (value.isBlank()) return ""
    return when {
        value.startsWith("http://", ignoreCase = true) -> value
        value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("www.", ignoreCase = true) -> "https://$value"
        else -> value
    }
}

internal fun extractGuideWebLinks(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return guideWebLinkRegex.findAll(raw)
        .map { it.groupValues.getOrNull(1).orEmpty() }
        .map(::normalizeGuideWebLinkForOpen)
        .filter { link ->
            link.startsWith("http://", ignoreCase = true) ||
                link.startsWith("https://", ignoreCase = true)
        }
        .distinct()
        .toList()
}

internal fun containsGuideWebLink(raw: String): Boolean {
    return extractGuideWebLinks(raw).isNotEmpty()
}

internal fun stripGuideWebLinks(raw: String): String {
    if (raw.isBlank()) return ""
    val stripped = guideWebLinkRegex.replace(raw, " ")
    return stripped
        .replace(Regex("""\s+"""), " ")
        .trim()
}

internal fun isPlaceholderGalleryToken(raw: String): Boolean {
    val value = raw.trim().lowercase()
    if (value.isBlank()) return true
    if (value == "n" || value == "null" || value == "undefined" || value == "nan") return true
    return value.matches(Regex("""^\d+$"""))
}

internal fun hasInvalidGameKeeMediaTail(rawUrl: String): Boolean {
    val value = rawUrl.trim()
    if (value.isBlank()) return true
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    if (!host.endsWith("gamekee.com")) return false
    val segments = uri.pathSegments.filter { it.isNotBlank() }
    if (segments.size != 1) return false
    return isPlaceholderGalleryToken(segments.first())
}

internal fun isRenderableGalleryImageUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:image", ignoreCase = true)) return true
    if (isPlaceholderGalleryToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (Regex("""\.(mp3|ogg|wav|m4a|aac)(\?.*)?(#.*)?$""").containsMatchIn(lower)) {
        return false
    }
    if (Regex("""\.(png|jpg|jpeg|webp|gif|bmp|svg|avif)(\?.*)?(#.*)?$""").containsMatchIn(lower)) {
        return true
    }
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    val uri = runCatching { Uri.parse(normalized) }.getOrNull()
    val host = uri?.host?.lowercase().orEmpty()
    val path = (uri?.encodedPath ?: uri?.path ?: "").lowercase()
    if (host.contains("cdnimg") || host.contains("img")) return true
    if (path.contains("/upload") || path.contains("/uploads") || path.contains("/images/") || path.contains("/wiki/")) {
        return true
    }
    return lower.contains("x-oss-process=image")
}

internal fun isRenderableGalleryGifUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (isPlaceholderGalleryToken(value)) return false
    if (value.startsWith("data:image/gif", ignoreCase = true)) return true
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    if (Regex("""\.gif(\?.*)?(#.*)?$""").containsMatchIn(lower)) return true
    return lower.contains("format=gif") || lower.contains("image/gif")
}

internal fun isRenderableGalleryStaticImageUrl(raw: String): Boolean {
    if (!isRenderableGalleryImageUrl(raw)) return false
    return !isRenderableGalleryGifUrl(raw)
}

internal fun isRenderableGalleryVideoUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:video", ignoreCase = true)) return true
    if (isPlaceholderGalleryToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    return lower.endsWith(".mp4") ||
        lower.endsWith(".webm") ||
        lower.endsWith(".mov") ||
        lower.endsWith(".m3u8") ||
        lower.contains(".mp4?") ||
        lower.contains(".m3u8?")
}

internal fun isRenderableGalleryAudioUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:audio", ignoreCase = true)) return true
    if (isPlaceholderGalleryToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    return lower.endsWith(".mp3") ||
        lower.endsWith(".ogg") ||
        lower.endsWith(".wav") ||
        lower.endsWith(".m4a") ||
        lower.endsWith(".aac") ||
        lower.contains(".mp3?") ||
        lower.contains(".ogg?") ||
        lower.contains(".wav?") ||
        lower.contains(".m4a?") ||
        lower.contains(".aac?")
}

internal fun hasRenderableGalleryMedia(item: BaGuideGalleryItem): Boolean {
    val imageRenderable = isRenderableGalleryImageUrl(item.imageUrl)
    val mediaRenderable = when (item.mediaType.lowercase()) {
        "video" -> isRenderableGalleryVideoUrl(item.mediaUrl)
        "audio" -> isRenderableGalleryAudioUrl(item.mediaUrl)
        else -> isRenderableGalleryImageUrl(item.mediaUrl)
    }
    return imageRenderable || mediaRenderable
}

internal fun isMemoryHallFileGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("回忆大厅文件")
}

internal fun isExpressionGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    if (title.startsWith("角色表情")) return true
    if (title.startsWith("表情")) return true
    if (title == "差分") return true
    if (title.contains("表情差分")) return true
    return title.contains("差分")
}

internal fun isMemoryHallGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("回忆大厅") && !title.startsWith("回忆大厅视频") && !title.startsWith("回忆大厅文件")
}

internal fun isOfficialIntroGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("官方介绍")
}

internal fun isChocolateGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("巧克力图") || title.startsWith("情人节巧克力")
}

internal fun isInteractiveFurnitureGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("互动家具")
}

private fun looksLikeGifGalleryUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:image/gif", ignoreCase = true)) return true
    if (Regex("""\.gif(\?.*)?(#.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(value)) return true
    val lower = value.lowercase()
    return lower.contains("format=gif") || lower.contains("image/gif")
}

internal fun isInteractiveFurnitureAnimatedGalleryItem(item: BaGuideGalleryItem): Boolean {
    if (!isInteractiveFurnitureGalleryItem(item)) return false
    val media = item.mediaUrl.ifBlank { item.imageUrl }.trim()
    if (looksLikeGifGalleryUrl(media)) return true

    val numericTokens = Regex("""\d+""")
        .findAll(item.title)
        .map { it.value }
        .toList()
    if (numericTokens.size >= 2) {
        return numericTokens.last() == "2"
    }
    if (numericTokens.size == 1) {
        val token = numericTokens.first()
        if (token.length >= 2 && token.last() == '2') {
            return true
        }
    }
    return false
}

internal fun isPreviewVideoCategoryTitle(rawTitle: String): Boolean {
    val title = normalizeGalleryTitle(rawTitle)
    return title.startsWith("回忆大厅视频") || title.startsWith("PV") || title.startsWith("角色演示")
}

internal fun isPreviewVideoCategoryGalleryItem(item: BaGuideGalleryItem): Boolean {
    return isPreviewVideoCategoryTitle(item.title)
}

internal fun isPreviewVideoGalleryItem(item: BaGuideGalleryItem): Boolean {
    if (item.mediaType.lowercase() != "video") return false
    return isPreviewVideoCategoryTitle(item.title)
}

internal fun expressionGalleryOrder(title: String, fallback: Int): Int {
    val normalized = normalizeGalleryTitle(title)
    if (normalized == "角色表情" || normalized == "表情" || normalized == "差分") return 1
    val numeric = Regex("""(\d+)(?!.*\d)""")
        .find(normalized)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    return numeric ?: fallback
}
