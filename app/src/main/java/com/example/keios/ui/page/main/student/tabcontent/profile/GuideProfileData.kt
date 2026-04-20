package com.example.keios.ui.page.main.student.tabcontent.profile

import android.net.Uri
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
import com.example.keios.ui.page.main.student.fetch.normalizeGuideUrl
import java.util.concurrent.ConcurrentHashMap

internal val profileLinkTitleCache = ConcurrentHashMap<String, String>()

internal fun extractProfileExternalLink(raw: String): String {
    val source = raw.trim()
    if (source.isBlank()) return ""

    val direct = when {
        source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true) -> source
        source.startsWith("www.", ignoreCase = true) -> "https://$source"
        source.startsWith("/") -> normalizeGuideUrl(source)
        else -> ""
    }
    if (direct.isNotBlank()) {
        val scheme = runCatching { Uri.parse(direct).scheme.orEmpty() }.getOrDefault("")
        if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
            return direct
        }
    }

    val embedded = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        .find(source)
        ?.value
        .orEmpty()
        .trimEnd(')', ']', '}', ',', '。', '，')
    if (embedded.isBlank()) return ""

    val embeddedScheme = runCatching { Uri.parse(embedded).scheme.orEmpty() }.getOrDefault("")
    return if (embeddedScheme.equals("http", ignoreCase = true) || embeddedScheme.equals("https", ignoreCase = true)) {
        embedded
    } else {
        ""
    }
}

internal fun resolveProfileLinkTitle(url: String): String {
    if (url.isBlank()) return ""
    profileLinkTitleCache[url]?.let { return it }
    val title = runCatching { fetchProfileLinkTitle(url) }.getOrDefault("")
    profileLinkTitleCache[url] = title
    return title
}

internal fun fetchProfileLinkTitle(url: String): String {
    if (url.isBlank()) return ""
    val html = GameKeeFetchHelper.fetchHtml(
        pathOrUrl = url,
        refererPath = "/ba/"
    )
    if (html.isBlank()) return ""

    val ogTitle = Regex(
        pattern = """<meta\s+[^>]*(?:property|name)\s*=\s*["']og:title["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(html)?.groupValues?.getOrNull(1).orEmpty()

    val titleTag = Regex(
        pattern = """<title[^>]*>(.*?)</title>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).find(html)?.groupValues?.getOrNull(1).orEmpty()

    return cleanProfileLinkTitle(
        if (ogTitle.isNotBlank()) ogTitle else titleTag
    )
}

internal fun cleanProfileLinkTitle(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun fallbackProfileLinkTitle(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull()
    val lastSegment = uri?.lastPathSegment?.trim().orEmpty()
    if (lastSegment.isNotBlank()) return lastSegment
    val host = uri?.host?.trim().orEmpty()
    if (host.isNotBlank()) return host
    return "打开链接"
}
