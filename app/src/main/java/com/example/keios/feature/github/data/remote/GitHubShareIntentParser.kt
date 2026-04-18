package com.example.keios.feature.github.data.remote

import java.net.URI
import java.net.URLDecoder

internal enum class GitHubSharedUrlType {
    Repo,
    Releases,
    ReleaseTag,
    ReleaseDownloadAsset
}

internal data class GitHubSharedReleaseLink(
    val sourceUrl: String,
    val projectUrl: String,
    val owner: String,
    val repo: String,
    val type: GitHubSharedUrlType,
    val releaseTag: String = "",
    val assetName: String = ""
)

internal object GitHubShareIntentParser {
    private val githubUrlRegex = Regex(
        pattern = """https?://(?:www\.)?github\.com/[^\s<>"')\]]+""",
        option = RegexOption.IGNORE_CASE
    )

    fun extractFirstGitHubUrl(text: String): String? {
        val rawText = text.trim()
        if (rawText.isBlank()) return null
        val matched = githubUrlRegex.find(rawText)?.value?.trim().orEmpty()
        val cleaned = matched.trimEnd('.', ',', ';', ':', '!', '?')
        return cleaned.ifBlank { null }
    }

    fun looksLikeGitHubShareText(text: String): Boolean {
        return extractFirstGitHubUrl(text) != null
    }

    fun parseSharedReleaseLink(text: String): GitHubSharedReleaseLink? {
        val sharedUrl = extractFirstGitHubUrl(text) ?: return null
        return parseSharedReleaseUrl(sharedUrl)
    }

    fun parseSharedReleaseUrl(rawUrl: String): GitHubSharedReleaseLink? {
        val normalizedUrl = rawUrl.trim().trimEnd('.', ',', ';', ':', '!', '?')
        if (normalizedUrl.isBlank()) return null

        val uri = runCatching { URI(normalizedUrl) }.getOrNull()
        val host = uri?.host.orEmpty().lowercase()
        val ownerRepoFromPath = runCatching {
            uri?.path
                .orEmpty()
                .split('/')
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())

        val owner = decodePathSegment(ownerRepoFromPath.getOrNull(0).orEmpty())
        val repo = decodePathSegment(ownerRepoFromPath.getOrNull(1).orEmpty()).removeSuffix(".git")

        val parsedOwnerRepo = when {
            owner.isNotBlank() && repo.isNotBlank() && (host == "github.com" || host == "www.github.com") ->
                owner to repo
            else -> GitHubVersionUtils.parseOwnerRepo(normalizedUrl)
        } ?: return null

        val parsedOwner = parsedOwnerRepo.first.trim()
        val parsedRepo = parsedOwnerRepo.second.trim()
        if (parsedOwner.isBlank() || parsedRepo.isBlank()) return null

        val projectUrl = buildProjectUrl(parsedOwner, parsedRepo)
        if (ownerRepoFromPath.size < 3) {
            return GitHubSharedReleaseLink(
                sourceUrl = normalizedUrl,
                projectUrl = projectUrl,
                owner = parsedOwner,
                repo = parsedRepo,
                type = GitHubSharedUrlType.Repo
            )
        }

        val third = ownerRepoFromPath.getOrNull(2).orEmpty().lowercase()
        val fourth = ownerRepoFromPath.getOrNull(3).orEmpty().lowercase()
        if (third != "releases") {
            return GitHubSharedReleaseLink(
                sourceUrl = normalizedUrl,
                projectUrl = projectUrl,
                owner = parsedOwner,
                repo = parsedRepo,
                type = GitHubSharedUrlType.Repo
            )
        }

        return when (fourth) {
            "tag" -> {
                val tag = decodePathSegment(ownerRepoFromPath.getOrNull(4).orEmpty())
                GitHubSharedReleaseLink(
                    sourceUrl = normalizedUrl,
                    projectUrl = projectUrl,
                    owner = parsedOwner,
                    repo = parsedRepo,
                    type = if (tag.isBlank()) GitHubSharedUrlType.Releases else GitHubSharedUrlType.ReleaseTag,
                    releaseTag = tag
                )
            }
            "download" -> {
                val tag = decodePathSegment(ownerRepoFromPath.getOrNull(4).orEmpty())
                val asset = decodePathSegment(
                    ownerRepoFromPath.drop(5).joinToString("/")
                )
                GitHubSharedReleaseLink(
                    sourceUrl = normalizedUrl,
                    projectUrl = projectUrl,
                    owner = parsedOwner,
                    repo = parsedRepo,
                    type = if (tag.isBlank() || asset.isBlank()) {
                        GitHubSharedUrlType.Releases
                    } else {
                        GitHubSharedUrlType.ReleaseDownloadAsset
                    },
                    releaseTag = tag,
                    assetName = asset
                )
            }
            else -> GitHubSharedReleaseLink(
                sourceUrl = normalizedUrl,
                projectUrl = projectUrl,
                owner = parsedOwner,
                repo = parsedRepo,
                type = GitHubSharedUrlType.Releases
            )
        }
    }

    private fun decodePathSegment(segment: String): String {
        val raw = segment.trim()
        if (raw.isBlank()) return ""
        return runCatching {
            URLDecoder.decode(raw, Charsets.UTF_8.name())
        }.getOrDefault(raw).trim()
    }

    private fun buildProjectUrl(owner: String, repo: String): String {
        return "https://github.com/${owner.trim()}/${repo.trim()}"
    }
}
