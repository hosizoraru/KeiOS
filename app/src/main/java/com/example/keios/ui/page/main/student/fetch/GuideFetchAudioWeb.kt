package com.example.keios.ui.page.main.student.fetch

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

internal fun isAudioUrl(url: String): Boolean {
    val value = url.trim().lowercase()
    if (value.isBlank()) return false
    return Regex("""\.(mp3|ogg|wav|m4a|aac)(\?.*)?$""").containsMatchIn(value)
}

internal fun extractAudioUrlsFromRaw(sourceUrl: String, raw: String): List<String> {
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

internal fun extractAudioUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 6) return emptyList()
    return when (any) {
        is String -> extractAudioUrlsFromRaw(sourceUrl, any)

        is JSONObject -> {
            val result = mutableListOf<String>()
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractAudioUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractAudioUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}

internal fun looksLikeBinaryMediaUrl(raw: String): Boolean {
    val value = raw.trim().lowercase()
    if (value.isBlank()) return false
    return Regex("""\.(png|jpg|jpeg|webp|gif|bmp|svg|avif|mp4|webm|mov|m3u8|mp3|ogg|wav|m4a|aac|zip|rar|7z|apk)(\?.*)?(#.*)?$""")
        .containsMatchIn(value)
}

internal fun normalizeWebUrlCandidate(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val normalized = when {
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> normalizeGuideUrl(value)
        value.startsWith("www.", ignoreCase = true) -> "https://$value"
        else -> normalizeGuideUrl(value)
    }.trim()
    if (!normalized.startsWith("http://", ignoreCase = true) &&
        !normalized.startsWith("https://", ignoreCase = true)
    ) {
        return ""
    }
    if (looksLikeBinaryMediaUrl(normalized)) return ""
    val host = runCatching { Uri.parse(normalized).host.orEmpty().lowercase() }.getOrDefault("")
    if (host.isBlank()) return ""
    return normalized
}

internal fun extractWebUrlsFromAny(sourceUrl: String, any: Any?, depth: Int = 0): List<String> {
    if (any == null || depth > 8) return emptyList()
    return when (any) {
        is String -> {
            val direct = normalizeWebUrlCandidate(any)
            val embedded = Regex("""https?://[^\s"'<>]+|//[^\s"'<>]+""", RegexOption.IGNORE_CASE)
                .findAll(any)
                .mapNotNull { match ->
                    normalizeWebUrlCandidate(match.value)
                }
                .toList()
            buildList {
                if (direct.isNotBlank()) add(direct)
                addAll(embedded)
            }.distinct()
        }

        is JSONObject -> {
            val result = mutableListOf<String>()
            val directKeys = listOf("url", "href", "jumpHref", "jump_url", "link")
            directKeys.forEach { key ->
                val normalized = normalizeWebUrlCandidate(any.optString(key))
                if (normalized.isNotBlank()) result += normalized
            }
            val keys = any.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result += extractWebUrlsFromAny(sourceUrl, any.opt(key), depth + 1)
            }
            result.distinct()
        }

        is JSONArray -> {
            val result = mutableListOf<String>()
            for (i in 0 until any.length()) {
                result += extractWebUrlsFromAny(sourceUrl, any.opt(i), depth + 1)
            }
            result.distinct()
        }

        else -> emptyList()
    }
}
