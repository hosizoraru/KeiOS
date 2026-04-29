package os.kei.feature.github.data.remote

import java.time.Instant
import java.util.Locale
import kotlin.math.roundToLong

internal object GitHubActionsNightlyLinkHtmlParser {
    fun parsePublicRunDetail(
        html: String,
        owner: String,
        repo: String,
        runId: Long
    ): GitHubActionsNightlyRunPublicDetail? {
        if (html.isBlank()) return null
        val title = titleRegex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.htmlUnescape()
            ?.cleanText()
            .orEmpty()
        val titleParts = parseRunTitle(title, owner, repo)
        val createdAt = triggeredAtRegex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.parseIsoInstantOrNull()
        val event = triggeredEventRegex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.htmlToText()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        val statusText = statusRegex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.htmlToText()
            .orEmpty()
        val status = mapRunStatus(statusText)
        val branch = branchRegex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.htmlUnescape()
            ?.cleanText()
            .orEmpty()
        val sha = commitRegex(owner, repo).find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.cleanText()
            .orEmpty()
            .ifBlank { titleParts.headSha }
        val workflowName = workflowNameRegex(runId).find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.htmlToText()
            .orEmpty()
        val artifacts = parseRunArtifacts(html, createdAt)
        if (
            titleParts.displayTitle.isBlank() &&
            branch.isBlank() &&
            sha.isBlank() &&
            createdAt == null &&
            artifacts.isEmpty()
        ) {
            return null
        }
        return GitHubActionsNightlyRunPublicDetail(
            displayTitle = titleParts.displayTitle,
            workflowName = workflowName,
            event = event,
            status = status.status,
            conclusion = status.conclusion,
            headBranch = branch,
            headSha = sha,
            createdAtMillis = createdAt,
            updatedAtMillis = createdAt,
            artifacts = artifacts
        )
    }

    fun parseSizeToBytes(value: String): Long {
        val normalized = value.htmlToText()
            .replace(",", "")
            .trim()
        if (normalized.isBlank()) return 0L
        val match = sizeRegex.find(normalized) ?: return 0L
        val amount = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return 0L
        val unit = match.groupValues.getOrNull(2)
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        val multiplier = when (unit) {
            "gb", "gib", "g" -> 1024L * 1024L * 1024L
            "mb", "mib", "m" -> 1024L * 1024L
            "kb", "kib", "k" -> 1024L
            "b", "byte", "bytes" -> 1L
            else -> 1L
        }
        return (amount * multiplier).roundToLong().coerceAtLeast(0L)
    }

    private fun parseRunArtifacts(
        html: String,
        runUpdatedAtMillis: Long?
    ): List<GitHubActionsNightlyArtifactPublicDetail> {
        return artifactRowRegex.findAll(html)
            .mapNotNull { match ->
                val id = match.groupValues.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                val row = match.groupValues.getOrNull(2).orEmpty()
                val name = artifactNameRegex.find(row)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.htmlToText()
                    .orEmpty()
                if (name.isBlank()) return@mapNotNull null
                val cells = tableCellRegex.findAll(row)
                    .map { cell -> cell.groupValues.getOrNull(1).orEmpty().htmlToText() }
                    .toList()
                val sizeBytes = cells.getOrNull(1)
                    ?.let(::parseSizeToBytes)
                    ?: 0L
                val digest = digestRegex.find(row)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.htmlToText()
                    .orEmpty()
                GitHubActionsNightlyArtifactPublicDetail(
                    id = id,
                    name = name,
                    sizeBytes = sizeBytes,
                    digest = digest,
                    updatedAtMillis = runUpdatedAtMillis
                )
            }
            .distinctBy { detail -> detail.name.lowercase(Locale.ROOT) }
            .toList()
    }

    private fun parseRunTitle(
        title: String,
        owner: String,
        repo: String
    ): RunTitleParts {
        val marker = " · $owner/$repo@"
        val displayTitle = title.substringBefore(marker, missingDelimiterValue = title)
            .removeSuffix(" · GitHub")
            .cleanText()
        val sha = Regex("""${Regex.escape(owner)}/${Regex.escape(repo)}@([0-9a-f]{7,40})""", RegexOption.IGNORE_CASE)
            .find(title)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        return RunTitleParts(displayTitle = displayTitle, headSha = sha)
    }

    private fun mapRunStatus(label: String): RunStatusParts {
        val normalized = label.trim().lowercase(Locale.ROOT)
        return when {
            normalized.contains("success") -> RunStatusParts(status = "completed", conclusion = "success")
            normalized.contains("failure") || normalized.contains("failed") ->
                RunStatusParts(status = "completed", conclusion = "failure")
            normalized.contains("cancel") -> RunStatusParts(status = "completed", conclusion = "cancelled")
            normalized.contains("skip") -> RunStatusParts(status = "completed", conclusion = "skipped")
            normalized.contains("progress") || normalized.contains("running") ->
                RunStatusParts(status = "in_progress", conclusion = "")
            normalized.contains("queue") || normalized.contains("waiting") || normalized.contains("pending") ->
                RunStatusParts(status = "queued", conclusion = "")
            else -> RunStatusParts(status = "", conclusion = "")
        }
    }

    private fun commitRegex(owner: String, repo: String): Regex {
        return Regex(
            """/${Regex.escape(owner)}/${Regex.escape(repo)}/commit/([0-9a-f]{7,40})""",
            RegexOption.IGNORE_CASE
        )
    }

    private fun workflowNameRegex(runId: Long): Regex {
        return Regex(
            """actions/runs/$runId/workflow["'][^>]*>\s*([^<]+)\s*</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }

    private fun String.parseIsoInstantOrNull(): Long? {
        return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
    }

    private fun String.htmlToText(): String {
        return replace(Regex("""<[^>]+>"""), " ")
            .htmlUnescape()
            .cleanText()
    }

    private fun String.htmlUnescape(): String {
        return replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }

    private fun String.cleanText(): String {
        return replace(Regex("""\s+"""), " ").trim()
    }

    private data class RunTitleParts(
        val displayTitle: String,
        val headSha: String
    )

    private data class RunStatusParts(
        val status: String,
        val conclusion: String
    )

    private val titleRegex = Regex("""<title>\s*(.*?)\s*</title>""", RegexOption.DOT_MATCHES_ALL)
    private val triggeredAtRegex = Regex(
        """Triggered via[\s\S]*?<relative-time[^>]*datetime=["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val triggeredEventRegex = Regex(
        """Triggered via\s+([^<]+?)\s*<relative-time""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val statusRegex = Regex(
        """<span[^>]*>\s*Status\s*</span>\s*<span[^>]*>\s*([^<]+)\s*</span>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val branchRegex = Regex(
        """<a\b(?=[^>]*class=["'][^"']*branch-name)(?=[^>]*title=["']([^"']+)["'])[^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val artifactRowRegex = Regex(
        """<tr\b[^>]*\bdata-artifact-id=["']([0-9]+)["'][^>]*>(.*?)</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val artifactNameRegex = Regex(
        """<span\b[^>]*class=["'][^"']*\btext-bold\b[^"']*["'][^>]*>(.*?)</span>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val tableCellRegex = Regex(
        """<td\b[^>]*>(.*?)</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val digestRegex = Regex(
        """<code\b[^>]*>\s*([^<]*sha256:[^<]+)\s*</code>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val sizeRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*([a-zA-Z]+)?""")
}

internal data class GitHubActionsNightlyRunPublicDetail(
    val displayTitle: String = "",
    val workflowName: String = "",
    val event: String = "",
    val status: String = "",
    val conclusion: String = "",
    val headBranch: String = "",
    val headSha: String = "",
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,
    val artifacts: List<GitHubActionsNightlyArtifactPublicDetail> = emptyList()
)

internal data class GitHubActionsNightlyArtifactPublicDetail(
    val id: Long,
    val name: String,
    val sizeBytes: Long = 0L,
    val digest: String = "",
    val updatedAtMillis: Long? = null
)
