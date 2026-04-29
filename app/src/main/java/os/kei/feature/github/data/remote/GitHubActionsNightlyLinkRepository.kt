package os.kei.feature.github.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsArtifactDownloadResolution
import os.kei.feature.github.model.GitHubActionsRepositoryInfo
import os.kei.feature.github.model.GitHubActionsRunArtifacts
import os.kei.feature.github.model.GitHubActionsRunStatusSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowArtifactsSnapshot
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class GitHubActionsNightlyLinkRepository(
    private val client: OkHttpClient,
    private val githubHtmlBaseUrl: String,
    private val nightlyLinkBaseUrl: String
) {
    fun fetchRepositoryInfo(
        owner: String,
        repo: String
    ): Result<GitHubActionsRepositoryInfo> = runCatching {
        val cacheKey = metadataCacheKey(owner, repo)
        cachedValue(repositoryInfoCache[cacheKey])?.let { return@runCatching it }
        val html = fetchPublicHtml(buildGitHubRepoUrl(owner, repo)).getOrThrow()
        val defaultBranch = Regex(""""defaultBranch"\s*:\s*"([^"]+)"""")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.htmlUnescape()
            ?.trim()
            .orEmpty()
            .ifBlank { DEFAULT_PUBLIC_BRANCH }
        GitHubActionsRepositoryInfo(
            owner = owner,
            repo = repo,
            fullName = "$owner/$repo",
            defaultBranch = defaultBranch
        ).also { info -> putCachedValue(repositoryInfoCache, cacheKey, info) }
    }

    fun fetchWorkflows(
        owner: String,
        repo: String,
        limit: Int
    ): Result<List<GitHubActionsWorkflow>> = runCatching {
        val workflowFiles = fetchWorkflowFiles(owner, repo)
            .take(limit.coerceIn(1, 100))
        workflowFiles.map { fileName ->
            val path = ".github/workflows/$fileName"
            GitHubActionsWorkflow(
                id = stablePositiveId("$owner/$repo/$path"),
                name = workflowNameFromFile(fileName),
                path = path,
                state = "active",
                htmlUrl = "${githubHtmlBaseUrl.trimEnd('/')}/$owner/$repo/actions/workflows/${fileName.urlEncode()}",
                badgeUrl = ""
            )
        }
    }

    fun fetchWorkflowRuns(
        owner: String,
        repo: String,
        workflowId: String,
        branch: String
    ): Result<List<GitHubActionsWorkflowRun>> {
        return fetchWorkflowArtifactSnapshot(
            owner = owner,
            repo = repo,
            workflowId = workflowId,
            branch = branch,
            artifactsPerRun = DEFAULT_ARTIFACT_LIMIT,
            resolveRunDetail = false
        ).mapCatching { snapshot -> snapshot.runs.map { it.run } }
    }

    fun fetchWorkflowRun(
        owner: String,
        repo: String,
        runId: Long
    ): Result<GitHubActionsWorkflowRun> = runCatching {
        buildNightlyRun(
            owner = owner,
            repo = repo,
            runId = runId,
            branch = "",
            workflowId = 0L,
            workflowName = "",
            htmlUrl = buildGitHubRunUrl(owner, repo, runId)
        )
    }

    fun fetchRunArtifacts(
        owner: String,
        repo: String,
        runId: Long,
        limit: Int
    ): Result<List<GitHubActionsArtifact>> = runCatching {
        val html = fetchPublicHtml(buildNightlyRunDashboardUrl(owner, repo, runId)).getOrThrow()
        parseNightlyRunArtifacts(
            html = html,
            owner = owner,
            repo = repo,
            runId = runId
        ).take(limit.coerceIn(1, 100))
    }

    fun fetchRunStatusSnapshot(
        owner: String,
        repo: String,
        runId: Long,
        artifactsLimit: Int,
        includeArtifactsWhenCompleted: Boolean
    ): Result<GitHubActionsRunStatusSnapshot> = runCatching {
        val artifacts = if (includeArtifactsWhenCompleted) {
            fetchRunArtifacts(owner, repo, runId, artifactsLimit).getOrThrow()
        } else {
            emptyList()
        }
        GitHubActionsRunStatusSnapshot(
            owner = owner,
            repo = repo,
            run = buildNightlyRun(
                owner = owner,
                repo = repo,
                runId = runId,
                branch = artifacts.firstOrNull()?.workflowRunHeadBranch.orEmpty(),
                workflowId = 0L,
                workflowName = "",
                htmlUrl = buildGitHubRunUrl(owner, repo, runId)
            ),
            artifacts = artifacts
        )
    }

    fun fetchWorkflowArtifactSnapshot(
        owner: String,
        repo: String,
        workflowId: String,
        branch: String,
        artifactsPerRun: Int,
        resolveRunDetail: Boolean
    ): Result<GitHubActionsWorkflowArtifactsSnapshot> = runCatching {
        val resolvedBranch = branch.trim().ifBlank {
            fetchRepositoryInfo(owner, repo).getOrThrow().defaultBranch
        }.ifBlank { DEFAULT_PUBLIC_BRANCH }
        val workflowFile = nightlyWorkflowFile(workflowId)
        val workflowSlug = workflowFile
            .removeSuffix(".yaml")
            .removeSuffix(".yml")
        val html = fetchPublicHtml(
            buildNightlyWorkflowDashboardUrl(owner, repo, workflowSlug, resolvedBranch)
        ).getOrThrow()
        val artifactNames = parseNightlyWorkflowArtifactNames(
            html = html,
            owner = owner,
            repo = repo,
            workflowSlug = workflowSlug,
            branch = resolvedBranch
        ).take(artifactsPerRun.coerceIn(1, 100))
        val detail = if (resolveRunDetail) {
            artifactNames.firstOrNull()?.let { artifactName ->
                fetchArtifactDetail(
                    owner = owner,
                    repo = repo,
                    workflowSlug = workflowSlug,
                    branch = resolvedBranch,
                    artifactName = artifactName
                )
            }
        } else {
            null
        }
        val syntheticRunId = stablePositiveId("$owner/$repo/$workflowSlug/$resolvedBranch/latest")
        val runId = detail?.runId?.takeIf { it > 0L } ?: syntheticRunId
        val workflowLongId = stablePositiveId("$owner/$repo/.github/workflows/$workflowFile")
        val run = buildNightlyRun(
            owner = owner,
            repo = repo,
            runId = runId,
            branch = resolvedBranch,
            workflowId = workflowLongId,
            workflowName = workflowNameFromFile(workflowFile),
            htmlUrl = detail?.runHtmlUrl.orEmpty().ifBlank {
                buildGitHubActionsQueryUrl(owner, repo, resolvedBranch)
            }
        )
        val artifacts = artifactNames.map { artifactName ->
            val detailForArtifact = if (artifactName == detail?.artifactName) detail else null
            buildNightlyArtifact(
                owner = owner,
                repo = repo,
                workflowSlug = workflowSlug,
                branch = resolvedBranch,
                artifactName = artifactName,
                runId = runId,
                artifactId = detailForArtifact?.artifactId ?: stablePositiveId(
                    "$owner/$repo/$runId/$artifactName"
                )
            )
        }
        GitHubActionsWorkflowArtifactsSnapshot(
            owner = owner,
            repo = repo,
            workflowId = workflowLongId.toString(),
            runs = listOf(GitHubActionsRunArtifacts(run = run, artifacts = artifacts))
        )
    }

    fun resolveArtifactDownloadUrl(
        artifact: GitHubActionsArtifact,
        owner: String,
        repo: String
    ): Result<GitHubActionsArtifactDownloadResolution> {
        val url = artifact.archiveDownloadUrl.trim().ifBlank {
            if (owner.isBlank() || repo.isBlank() || artifact.id <= 0L) {
                return Result.failure(IllegalArgumentException("artifact 缺少 nightly.link 下载地址"))
            }
            buildNightlyArtifactUrl(owner, repo, artifact.id)
        }
        return Result.success(
            GitHubActionsArtifactDownloadResolution(
                artifactId = artifact.id,
                downloadUrl = url
            )
        )
    }

    private fun fetchWorkflowFiles(owner: String, repo: String): List<String> {
        val cacheKey = metadataCacheKey(owner, repo)
        cachedValue(workflowFilesCache[cacheKey])?.let { return it }
        val workflowsTreeUrl = "${githubHtmlBaseUrl.trimEnd('/')}/$owner/$repo/tree/HEAD/.github/workflows"
        val treeHtml = fetchPublicHtml(workflowsTreeUrl).getOrThrow()
        val fromTree = parseWorkflowFilesFromGitHubHtml(treeHtml, owner, repo)
        if (fromTree.isNotEmpty()) {
            putCachedValue(workflowFilesCache, cacheKey, fromTree)
            return fromTree
        }
        val actionsHtml = fetchPublicHtml("${githubHtmlBaseUrl.trimEnd('/')}/$owner/$repo/actions").getOrThrow()
        return parseWorkflowFilesFromGitHubHtml(actionsHtml, owner, repo).also { files ->
            putCachedValue(workflowFilesCache, cacheKey, files)
        }
    }

    private fun parseWorkflowFilesFromGitHubHtml(
        html: String,
        owner: String,
        repo: String
    ): List<String> {
        val ownerPattern = Regex.escape(owner)
        val repoPattern = Regex.escape(repo)
        val pattern = Regex(
            """/$ownerPattern/$repoPattern/(?:blob|actions/workflows)/[^"'\s<>]*?([^/"'\s<>]+\.ya?ml)(?:["'?#/]|$)""",
            RegexOption.IGNORE_CASE
        )
        return pattern.findAll(html)
            .map { match -> match.groupValues[1].htmlUnescape().urlDecode().trim() }
            .filter { it.endsWith(".yml", ignoreCase = true) || it.endsWith(".yaml", ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sorted()
            .toList()
    }

    private fun parseNightlyWorkflowArtifactNames(
        html: String,
        owner: String,
        repo: String,
        workflowSlug: String,
        branch: String
    ): List<String> {
        val basePath = "/${owner.urlPathEscape()}/${repo.urlPathEscape()}/workflows/" +
            "${workflowSlug.urlPathEscape()}/${branch.urlPathEscape()}/"
        return parseNightlyArtifactNames(html, basePath)
    }

    private fun parseNightlyRunArtifacts(
        html: String,
        owner: String,
        repo: String,
        runId: Long
    ): List<GitHubActionsArtifact> {
        val basePath = "/${owner.urlPathEscape()}/${repo.urlPathEscape()}/actions/runs/$runId/"
        return parseNightlyArtifactNames(html, basePath).map { artifactName ->
            GitHubActionsArtifact(
                id = stablePositiveId("$owner/$repo/$runId/$artifactName"),
                name = artifactName,
                archiveDownloadUrl = "${nightlyLinkBaseUrl.trimEnd('/')}$basePath${artifactName.urlEncode()}.zip",
                workflowRunId = runId
            )
        }
    }

    private fun parseNightlyArtifactNames(html: String, encodedBasePath: String): List<String> {
        val absoluteBase = nightlyLinkBaseUrl.trimEnd('/') + encodedBasePath
        val hrefPattern = Regex("""href="([^"]+\.zip(?:\?[^"]*)?)"""", RegexOption.IGNORE_CASE)
        return hrefPattern.findAll(html)
            .map { match -> match.groupValues[1].substringBefore('?') }
            .mapNotNull { href ->
                val normalized = when {
                    href.startsWith(absoluteBase, ignoreCase = true) -> href.substring(absoluteBase.length)
                    href.startsWith(encodedBasePath, ignoreCase = true) -> href.substring(encodedBasePath.length)
                    else -> null
                } ?: return@mapNotNull null
                normalized.removeSuffix(".zip").urlDecode().trim()
            }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .toList()
    }

    private fun fetchArtifactDetail(
        owner: String,
        repo: String,
        workflowSlug: String,
        branch: String,
        artifactName: String
    ): NightlyArtifactDetail? {
        val url = "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/" +
            "workflows/${workflowSlug.urlEncode()}/${branch.urlEncode()}/${artifactName.urlEncode()}"
        return fetchPublicHtml(url)
            .mapCatching { html ->
                val runId = Regex("""/${
                    Regex.escape(owner)
                }/${Regex.escape(repo)}/actions/runs/([0-9]+)""")
                    .find(html)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: 0L
                val repoPrefix = """/${Regex.escape(owner)}/${Regex.escape(repo)}"""
                val artifactId = listOf(
                    Regex("""$repoPrefix/actions/artifacts/([0-9]+)"""),
                    Regex("""$repoPrefix/suites/[0-9]+/artifacts/([0-9]+)""")
                ).asSequence()
                    .mapNotNull { regex -> regex.find(html) }
                    .firstOrNull()
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: 0L
                NightlyArtifactDetail(
                    artifactName = artifactName,
                    runId = runId,
                    artifactId = artifactId.takeIf { it > 0L },
                    runHtmlUrl = buildGitHubRunUrl(owner, repo, runId).takeIf { runId > 0L }.orEmpty()
                )
            }
            .getOrNull()
    }

    private fun buildNightlyRun(
        owner: String,
        repo: String,
        runId: Long,
        branch: String,
        workflowId: Long,
        workflowName: String,
        htmlUrl: String
    ): GitHubActionsWorkflowRun {
        return GitHubActionsWorkflowRun(
            id = runId,
            name = workflowName.ifBlank { "nightly.link" },
            displayTitle = "Latest successful run",
            workflowId = workflowId,
            workflowName = workflowName,
            event = "push",
            status = "completed",
            conclusion = "success",
            headBranch = branch,
            htmlUrl = htmlUrl,
            repositoryFullName = "$owner/$repo",
            headRepositoryFullName = "$owner/$repo",
            createdAtMillis = null,
            updatedAtMillis = null
        )
    }

    private fun buildNightlyArtifact(
        owner: String,
        repo: String,
        workflowSlug: String,
        branch: String,
        artifactName: String,
        runId: Long,
        artifactId: Long
    ): GitHubActionsArtifact {
        return GitHubActionsArtifact(
            id = artifactId,
            name = artifactName,
            archiveDownloadUrl = "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/" +
                "workflows/${workflowSlug.urlEncode()}/${branch.urlEncode()}/${artifactName.urlEncode()}.zip",
            workflowRunId = runId,
            workflowRunHeadBranch = branch
        )
    }

    private fun fetchPublicHtml(url: String): Result<String> = runCatching {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            val bodyText = response.body.string()
            if (!response.isSuccessful) {
                error("公开页面读取失败 (HTTP ${response.code})")
            }
            bodyText
        }
    }

    private fun buildGitHubRepoUrl(owner: String, repo: String): String {
        return "${githubHtmlBaseUrl.trimEnd('/')}/$owner/$repo"
    }

    private fun buildGitHubRunUrl(owner: String, repo: String, runId: Long): String {
        return "${githubHtmlBaseUrl.trimEnd('/')}/$owner/$repo/actions/runs/$runId"
    }

    private fun buildGitHubActionsQueryUrl(owner: String, repo: String, branch: String): String {
        val query = "event:push is:success branch:$branch".urlEncode()
        return "${githubHtmlBaseUrl.trimEnd('/')}/$owner/$repo/actions?query=$query"
    }

    private fun buildNightlyWorkflowDashboardUrl(
        owner: String,
        repo: String,
        workflowSlug: String,
        branch: String
    ): String {
        return "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/" +
            "workflows/${workflowSlug.urlEncode()}/${branch.urlEncode()}?preview"
    }

    private fun buildNightlyRunDashboardUrl(owner: String, repo: String, runId: Long): String {
        return "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/actions/runs/$runId"
    }

    private fun buildNightlyArtifactUrl(owner: String, repo: String, artifactId: Long): String {
        return "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/" +
            "actions/artifacts/$artifactId.zip"
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
    }

    private fun String.urlDecode(): String {
        return runCatching {
            URLDecoder.decode(this, Charsets.UTF_8.name())
        }.getOrDefault(this)
    }

    private fun String.urlPathEscape(): String = urlEncode()

    private fun String.htmlUnescape(): String {
        return replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun nightlyWorkflowFile(workflowId: String): String {
        val fileName = workflowId.trim()
            .substringBefore('?')
            .substringAfterLast('/')
            .ifBlank { workflowId.trim() }
        return when {
            fileName.endsWith(".yml", ignoreCase = true) -> fileName
            fileName.endsWith(".yaml", ignoreCase = true) -> fileName
            else -> "$fileName.yml"
        }
    }

    private fun workflowNameFromFile(fileName: String): String {
        return fileName
            .removeSuffix(".yaml")
            .removeSuffix(".yml")
            .replace('-', ' ')
            .replace('_', ' ')
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                }
            }
            .ifBlank { fileName }
    }

    private fun stablePositiveId(value: String): Long {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
        var result = 0L
        for (index in 0 until 8) {
            result = (result shl 8) or (digest[index].toLong() and 0xffL)
        }
        return result and Long.MAX_VALUE
    }

    private fun metadataCacheKey(owner: String, repo: String): String {
        return listOf(
            githubHtmlBaseUrl.trimEnd('/'),
            nightlyLinkBaseUrl.trimEnd('/'),
            owner.lowercase(Locale.ROOT),
            repo.lowercase(Locale.ROOT)
        ).joinToString("|")
    }

    private fun <T> cachedValue(entry: CachedValue<T>?): T? {
        if (entry == null) return null
        val ageMillis = System.currentTimeMillis() - entry.fetchedAtMillis
        return entry.value.takeIf { ageMillis in 0 until PUBLIC_METADATA_CACHE_TTL_MS }
    }

    private fun <T> putCachedValue(
        cache: ConcurrentHashMap<String, CachedValue<T>>,
        key: String,
        value: T
    ) {
        if (cache.size >= PUBLIC_METADATA_CACHE_MAX_ENTRIES) {
            cache.clear()
        }
        cache[key] = CachedValue(value, System.currentTimeMillis())
    }

    private data class NightlyArtifactDetail(
        val artifactName: String,
        val runId: Long,
        val artifactId: Long?,
        val runHtmlUrl: String
    )

    private data class CachedValue<T>(
        val value: T,
        val fetchedAtMillis: Long
    )

    private companion object {
        const val DEFAULT_ARTIFACT_LIMIT = 100
        const val DEFAULT_PUBLIC_BRANCH = "main"
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        const val PUBLIC_METADATA_CACHE_TTL_MS = 120_000L
        const val PUBLIC_METADATA_CACHE_MAX_ENTRIES = 64

        val repositoryInfoCache = ConcurrentHashMap<String, CachedValue<GitHubActionsRepositoryInfo>>()
        val workflowFilesCache = ConcurrentHashMap<String, CachedValue<List<String>>>()
    }
}
