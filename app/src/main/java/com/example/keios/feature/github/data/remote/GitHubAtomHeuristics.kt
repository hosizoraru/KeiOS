package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubReleaseChannel
import java.util.Locale

internal object GitHubAtomHeuristics {
    private val htmlLineBreakRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val htmlClosingBlockRegex = Regex("""</(?:p|div|h[1-6]|li|ul|ol|pre|code|section|article)>""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("""<[^>]+>""")
    private val whitespaceRegex = Regex("""[ \t\x0B\f\r]+""")
    private val excessiveNewlineRegex = Regex("""\n{3,}""")
    private val versionTokenRegex = Regex(
        """(?i)\b(?:version\s*)?[vV]?\d+(?:[._-]\d+)*(?:[-._ ]?(?:dev|nightly|canary|snapshot|alpha|beta|rc|preview|pre(?:-release)?)(?:[-._ ]?\d+)?)?(?:\+[0-9a-z.-]+)?\b"""
    )
    private val artifactNameRegex = Regex(
        """(?i)\b\S+\.(?:zip|apk|aab|apkm|tar\.gz|tgz|appimage|msi|dmg|exe|deb|rpm|pkg|ipa)\b"""
    )
    private val lineNoiseRegexes = listOf(
        Regex("""(?i)^\s*(?:curl|wget)\b"""),
        Regex("""(?i)\b(?:sha|checksum|md5|sha1|sha256|sha512|artifact|workflow|actions?)\b"""),
        Regex("""(?i)\b(?:docker|ghcr\.io|quay\.io|registry)\b"""),
        Regex("""(?i)^\s*download\b.*$""")
    )

    private val strongStableHints = listOf(
        "release notes",
        "full changelog",
        "whats changed",
        "what's changed",
        "install",
        "download"
    )

    fun decodeXmlEscapes(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    fun htmlToPlainText(value: String): String {
        return value
            .let(::decodeXmlEscapes)
            .replace(htmlLineBreakRegex, "\n")
            .replace(htmlClosingBlockRegex, "\n")
            .replace(htmlTagRegex, " ")
            .replace(whitespaceRegex, " ")
            .replace(excessiveNewlineRegex, "\n\n")
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }

    fun buildContentPreview(contentText: String, maxLines: Int = 12, maxChars: Int = 900): String {
        if (contentText.isBlank()) return ""

        val lines = contentText
            .lines()
            .mapNotNull(::sanitizePreviewLine)
            .distinct()
            .take(maxLines)

        return lines.joinToString("\n").take(maxChars).trim()
    }

    fun detectReleaseChannel(tag: String, title: String, contentPreview: String): GitHubReleaseChannel {
        detectFieldChannel(tag)?.let { return it }
        detectFieldChannel(title)?.let { return it }
        detectKeywordChannel(contentPreview)?.let { return it }
        return GitHubReleaseChannel.STABLE
    }

    private fun detectFieldChannel(text: String): GitHubReleaseChannel? {
        GitHubVersionUtils.classifyVersionChannel(text)?.let { return it }
        return detectKeywordChannel(text)
    }

    private fun detectKeywordChannel(text: String): GitHubReleaseChannel? {
        val hints = text
            .lowercase(Locale.ROOT)
            .replace('_', ' ')

        return when {
            Regex("""(^|[^a-z])(nightly|snapshot|canary|dev)([^a-z]|$)""").containsMatchIn(hints) -> GitHubReleaseChannel.DEV
            Regex("""(^|[^a-z])alpha([^a-z]|$)""").containsMatchIn(hints) -> GitHubReleaseChannel.ALPHA
            Regex("""(^|[^a-z])beta([^a-z]|$)""").containsMatchIn(hints) -> GitHubReleaseChannel.BETA
            Regex("""(^|[^a-z])rc([^a-z]|$)""").containsMatchIn(hints) -> GitHubReleaseChannel.RC
            Regex("""(^|[^a-z])(preview|pre-release|prerelease)([^a-z]|$)""").containsMatchIn(hints) -> GitHubReleaseChannel.PREVIEW
            strongStableHints.any { it in hints } -> GitHubReleaseChannel.STABLE
            else -> null
        }
    }

    private fun sanitizePreviewLine(raw: String): String? {
        val line = raw
            .replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '*', '•', '|', '>')
            .trim()
        if (line.isBlank()) return null
        if (line.length > 220) return null

        val isSemanticVersionLine = versionTokenRegex.containsMatchIn(line)
        val isHeading = looksLikeHeading(line)
        val digitRatio = line.count(Char::isDigit).toFloat() / line.length.coerceAtLeast(1)

        if (artifactNameRegex.containsMatchIn(line) && !isSemanticVersionLine) return null
        if (lineNoiseRegexes.any { it.containsMatchIn(line) } && !isSemanticVersionLine && !isHeading) return null
        if (digitRatio >= 0.45f && !isSemanticVersionLine) return null

        return line
    }

    private fun looksLikeHeading(line: String): Boolean {
        val normalized = line.lowercase(Locale.ROOT)
        return strongStableHints.any { it in normalized }
    }
}
