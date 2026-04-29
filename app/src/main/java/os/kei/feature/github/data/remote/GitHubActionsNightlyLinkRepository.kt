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
        cachedValue(repositoryInfoCache[cacheKey], PUBLIC_METADATA_CACHE_TTL_MS)?.let { return@runCatching it }
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
        val publicDetail = fetchPublicRunDetail(owner, repo, runId).getOrNull()
        buildNightlyRun(
            owner = owner,
            repo = repo,
            runId = runId,
            branch = "",
            workflowId = 0L,
            workflowName = "",
            htmlUrl = buildGitHubRunUrl(owner, repo, runId),
            publicDetail = publicDetail
        )
    }

    fun fetchRunArtifacts(
        owner: String,
        repo: String,
        runId: Long,
        limit: Int
    ): Result<List<GitHubActionsArtifact>> = runCatching {
        val publicDetail = fetchPublicRunDetail(owner, repo, runId).getOrNull()
        val html = fetchPublicHtml(buildNightlyRunDashboardUrl(owner, repo, runId)).getOrThrow()
        parseNightlyRunArtifacts(
            html = html,
            owner = owner,
            repo = repo,
            runId = runId,
            publicDetail = publicDetail
        ).take(limit.coerceIn(1, 100))
    }

    fun fetchRunStatusSnapshot(
        owner: String,
        repo: String,
        runId: Long,
        artifactsLimit: Int,
        includeArtifactsWhenCompleted: Boolean
    ): Result<GitHubActionsRunStatusSnapshot> = runCatching {
        val publicDetail = fetchPublicRunDetail(owner, repo, runId).getOrNull()
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
                htmlUrl = buildGitHubRunUrl(owner, repo, runId),
                publicDetail = publicDetail
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
        val branchSnapshot = fetchNightlyWorkflowArtifactNames(
            owner = owner,
            repo = repo,
            workflowFile = workflowFile,
            workflowSlug = workflowSlug,
            preferredBranch = resolvedBranch,
            artifactsPerRun = artifactsPerRun
        )
        val artifactBranch = branchSnapshot.branch
        val artifactNames = branchSnapshot.artifactNames
        if (artifactNames.isEmpty() && resolveRunDetail) {
            error(
                buildNoNightlyArtifactsMessage(
                    owner = owner,
                    repo = repo,
                    workflowFile = workflowFile,
                    branch = artifactBranch
                )
            )
        }
        val detail = if (resolveRunDetail) {
            artifactNames.firstOrNull()?.let { artifactName ->
                fetchArtifactDetail(
                    owner = owner,
                    repo = repo,
                    workflowSlug = workflowSlug,
                    branch = artifactBranch,
                    artifactName = artifactName
                )
            }
        } else {
            null
        }
        val publicRunDetail = detail?.runId
            ?.takeIf { it > 0L }
            ?.let { runId -> fetchPublicRunDetail(owner, repo, runId).getOrNull() }
        val syntheticRunId = stablePositiveId("$owner/$repo/$workflowSlug/$artifactBranch/latest")
        val runId = detail?.runId?.takeIf { it > 0L } ?: syntheticRunId
        val workflowLongId = stablePositiveId("$owner/$repo/.github/workflows/$workflowFile")
        val run = buildNightlyRun(
            owner = owner,
            repo = repo,
            runId = runId,
            branch = artifactBranch,
            workflowId = workflowLongId,
            workflowName = workflowNameFromFile(workflowFile),
            htmlUrl = detail?.runHtmlUrl.orEmpty().ifBlank {
                buildGitHubActionsQueryUrl(owner, repo, artifactBranch)
            },
            publicDetail = publicRunDetail
        )
        val publicArtifactsByName = publicRunDetail?.artifacts
            ?.associateBy { artifact -> artifact.name.normalizedArtifactNameKey() }
            .orEmpty()
        val artifacts = artifactNames.map { artifactName ->
            val detailForArtifact = if (artifactName == detail?.artifactName) detail else null
            val publicArtifact = publicArtifactsByName[artifactName.normalizedArtifactNameKey()]
            buildNightlyArtifact(
                owner = owner,
                repo = repo,
                workflowSlug = workflowSlug,
                branch = artifactBranch,
                artifactName = artifactName,
                runId = runId,
                artifactId = detailForArtifact?.artifactId ?: stablePositiveId(
                    "$owner/$repo/$runId/$artifactName"
                ),
                publicDetail = publicRunDetail,
                publicArtifact = publicArtifact
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
        cachedValue(workflowFilesCache[cacheKey], PUBLIC_METADATA_CACHE_TTL_MS)?.let { return it }
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

    private fun fetchNightlyWorkflowArtifactNames(
        owner: String,
        repo: String,
        workflowFile: String,
        workflowSlug: String,
        preferredBranch: String,
        artifactsPerRun: Int
    ): NightlyWorkflowArtifactNames {
        var firstEmptySnapshot: NightlyWorkflowArtifactNames? = null
        var lastFailure: Throwable? = null
        nightlyWorkflowBranchCandidates(workflowFile, preferredBranch).forEach { branch ->
            val html = fetchPublicHtml(
                buildNightlyWorkflowDashboardUrl(owner, repo, workflowSlug, branch)
            ).getOrElse { error ->
                lastFailure = error
                return@forEach
            }
            val artifactNames = parseNightlyWorkflowArtifactNames(
                html = html,
                owner = owner,
                repo = repo,
                workflowSlug = workflowSlug,
                branch = branch
            ).take(artifactsPerRun.coerceIn(1, 100))
            val snapshot = NightlyWorkflowArtifactNames(
                branch = branch,
                artifactNames = artifactNames
            )
            if (artifactNames.isNotEmpty()) return snapshot
            if (firstEmptySnapshot == null) {
                firstEmptySnapshot = snapshot
            }
        }
        firstEmptySnapshot?.let { return it }
        throw lastFailure ?: IllegalStateException("nightly.link 没有读取到 workflow artifact")
    }

    private fun nightlyWorkflowBranchCandidates(
        workflowFile: String,
        preferredBranch: String
    ): List<String> {
        val fileName = workflowFile.lowercase(Locale.ROOT)
        val preferred = preferredBranch.trim().ifBlank { DEFAULT_PUBLIC_BRANCH }
        val devBranches = listOf("dev", "develop")
        val candidates = mutableListOf<String>()
        if (fileName.contains("dev") || fileName.contains("develop")) {
            candidates += devBranches
            candidates += preferred
        } else {
            candidates += preferred
            if (
                fileName.contains("preview") ||
                fileName.contains("nightly") ||
                fileName.contains("unstable") ||
                fileName.contains("alpha")
            ) {
                candidates += devBranches
            }
        }
        return candidates
            .filter { branch -> branch.isNotBlank() }
            .distinctBy { branch -> branch.lowercase(Locale.ROOT) }
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
        runId: Long,
        publicDetail: GitHubActionsNightlyRunPublicDetail?
    ): List<GitHubActionsArtifact> {
        val basePath = "/${owner.urlPathEscape()}/${repo.urlPathEscape()}/actions/runs/$runId/"
        val publicArtifactsByName = publicDetail?.artifacts
            ?.associateBy { artifact -> artifact.name.normalizedArtifactNameKey() }
            .orEmpty()
        return parseNightlyArtifactNames(html, basePath).map { artifactName ->
            val publicArtifact = publicArtifactsByName[artifactName.normalizedArtifactNameKey()]
            GitHubActionsArtifact(
                id = publicArtifact?.id?.takeIf { it > 0L }
                    ?: stablePositiveId("$owner/$repo/$runId/$artifactName"),
                name = artifactName,
                sizeBytes = publicArtifact?.sizeBytes ?: 0L,
                digest = publicArtifact?.digest.orEmpty(),
                archiveDownloadUrl = "${nightlyLinkBaseUrl.trimEnd('/')}$basePath${artifactName.urlEncode()}.zip",
                workflowRunId = runId,
                workflowRunHeadBranch = publicDetail?.headBranch.orEmpty(),
                workflowRunHeadSha = publicDetail?.headSha.orEmpty(),
                createdAtMillis = publicDetail?.createdAtMillis,
                updatedAtMillis = publicArtifact?.updatedAtMillis
                    ?: publicDetail?.updatedAtMillis
                    ?: publicDetail?.createdAtMillis
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
        htmlUrl: String,
        publicDetail: GitHubActionsNightlyRunPublicDetail? = null
    ): GitHubActionsWorkflowRun {
        val resolvedWorkflowName = publicDetail?.workflowName.orEmpty()
            .ifBlank { workflowName }
        return GitHubActionsWorkflowRun(
            id = runId,
            name = resolvedWorkflowName.ifBlank { "nightly.link" },
            displayTitle = publicDetail?.displayTitle.orEmpty().ifBlank { "Latest successful run" },
            workflowId = workflowId,
            workflowName = resolvedWorkflowName,
            event = publicDetail?.event.orEmpty().ifBlank { "push" },
            status = publicDetail?.status.orEmpty().ifBlank { "completed" },
            conclusion = publicDetail?.conclusion.orEmpty().ifBlank { "success" },
            headBranch = publicDetail?.headBranch.orEmpty().ifBlank { branch },
            headSha = publicDetail?.headSha.orEmpty(),
            htmlUrl = htmlUrl,
            repositoryFullName = "$owner/$repo",
            headRepositoryFullName = "$owner/$repo",
            createdAtMillis = publicDetail?.createdAtMillis,
            updatedAtMillis = publicDetail?.updatedAtMillis ?: publicDetail?.createdAtMillis
        )
    }

    private fun buildNightlyArtifact(
        owner: String,
        repo: String,
        workflowSlug: String,
        branch: String,
        artifactName: String,
        runId: Long,
        artifactId: Long,
        publicDetail: GitHubActionsNightlyRunPublicDetail? = null,
        publicArtifact: GitHubActionsNightlyArtifactPublicDetail? = null
    ): GitHubActionsArtifact {
        return GitHubActionsArtifact(
            id = publicArtifact?.id?.takeIf { it > 0L } ?: artifactId,
            name = artifactName,
            sizeBytes = publicArtifact?.sizeBytes ?: 0L,
            digest = publicArtifact?.digest.orEmpty(),
            archiveDownloadUrl = "${nightlyLinkBaseUrl.trimEnd('/')}/${owner.urlEncode()}/${repo.urlEncode()}/" +
                "workflows/${workflowSlug.urlEncode()}/${branch.urlEncode()}/${artifactName.urlEncode()}.zip",
            workflowRunId = runId,
            workflowRunHeadBranch = publicDetail?.headBranch.orEmpty().ifBlank { branch },
            workflowRunHeadSha = publicDetail?.headSha.orEmpty(),
            createdAtMillis = publicDetail?.createdAtMillis,
            updatedAtMillis = publicArtifact?.updatedAtMillis
                ?: publicDetail?.updatedAtMillis
                ?: publicDetail?.createdAtMillis
        )
    }

    private fun fetchPublicRunDetail(
        owner: String,
        repo: String,
        runId: Long
    ): Result<GitHubActionsNightlyRunPublicDetail?> = runCatching {
        if (runId <= 0L) return@runCatching null
        fetchPublicHtml(buildGitHubRunUrl(owner, repo, runId)).getOrThrow()
            .let { html ->
                GitHubActionsNightlyLinkHtmlParser.parsePublicRunDetail(
                    html = html,
                    owner = owner,
                    repo = repo,
                    runId = runId
                )
            }
    }

    private fun fetchPublicHtml(
        url: String,
        cacheTtlMillis: Long = PUBLIC_HTML_CACHE_TTL_MS
    ): Result<String> = runCatching {
        if (cacheTtlMillis > 0L) {
            cachedValue(publicHtmlCache[url], cacheTtlMillis)?.let { cached ->
                return@runCatching cached
            }
        }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            val bodyText = response.body.string()
            if (!response.isSuccessful) {
                error(buildPublicHtmlErrorMessage(url = url, httpCode = response.code))
            }
            if (cacheTtlMillis > 0L) {
                putCachedValue(publicHtmlCache, url, bodyText)
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

    private fun buildNoNightlyArtifactsMessage(
        owner: String,
        repo: String,
        workflowFile: String,
        branch: String
    ): String {
        return "nightly.link 没有读取到 $owner/$repo 的 $workflowFile 在 $branch 分支的可下载 artifact。请确认该 workflow 最近一次成功 run 使用 actions/upload-artifact 上传了 artifact，且 artifact 仍在保留期内；建议切换 GitHub API Token。"
    }

    private fun buildPublicHtmlErrorMessage(url: String, httpCode: Int): String {
        val normalizedNightlyBase = nightlyLinkBaseUrl.trimEnd('/')
        val normalizedGitHubBase = githubHtmlBaseUrl.trimEnd('/')
        val source = when {
            url.startsWith(normalizedNightlyBase, ignoreCase = true) -> "nightly.link"
            url.startsWith(normalizedGitHubBase, ignoreCase = true) -> "GitHub 公开页面"
            else -> "公开页面"
        }
        return when (httpCode) {
            401, 403 ->
                "$source 访问被拒绝。私有仓库、受限 Actions、组织权限或全局限流会触发该状态；建议切换 GitHub API Token。"
            404 ->
                "$source 没有找到对应 Actions 资源。请确认仓库、workflow 文件、分支、run 与 artifact 均可公开访问，且 artifact 仍在保留期内；建议切换 GitHub API Token。"
            410 ->
                "$source 返回 artifact 已过期。请选择更新的 run，或切换 GitHub API Token 读取完整 Actions 数据。"
            429 ->
                "$source 当前限流。请稍后重试，或切换 GitHub API Token。"
            else ->
                "$source 读取失败 (HTTP $httpCode)。请检查网络与公开访问权限；建议切换 GitHub API Token。"
        }
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

    private fun String.normalizedArtifactNameKey(): String {
        return trim().lowercase(Locale.ROOT)
    }

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

    private fun <T> cachedValue(entry: CachedValue<T>?, ttlMillis: Long): T? {
        if (entry == null) return null
        val ageMillis = System.currentTimeMillis() - entry.fetchedAtMillis
        return entry.value.takeIf { ageMillis in 0 until ttlMillis }
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

    private data class NightlyWorkflowArtifactNames(
        val branch: String,
        val artifactNames: List<String>
    )

    private data class CachedValue<T>(
        val value: T,
        val fetchedAtMillis: Long
    )

    private companion object {
        const val DEFAULT_ARTIFACT_LIMIT = 100
        const val DEFAULT_PUBLIC_BRANCH = "main"
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        const val PUBLIC_HTML_CACHE_TTL_MS = 30_000L
        const val PUBLIC_METADATA_CACHE_TTL_MS = 120_000L
        const val PUBLIC_METADATA_CACHE_MAX_ENTRIES = 64

        val repositoryInfoCache = ConcurrentHashMap<String, CachedValue<GitHubActionsRepositoryInfo>>()
        val workflowFilesCache = ConcurrentHashMap<String, CachedValue<List<String>>>()
        val publicHtmlCache = ConcurrentHashMap<String, CachedValue<String>>()
    }
}
