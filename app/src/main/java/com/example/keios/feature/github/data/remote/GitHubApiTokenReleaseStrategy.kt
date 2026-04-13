package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubAtomFeed
import com.example.keios.feature.github.model.GitHubAtomReleaseEntry
import com.example.keios.feature.github.model.GitHubApiAuthMode
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubReleaseChannel
import com.example.keios.feature.github.model.GitHubReleaseSignalSource
import com.example.keios.feature.github.model.GitHubReleaseVersionSignals
import com.example.keios.feature.github.model.GitHubRepositoryReleaseSnapshot
import com.example.keios.feature.github.model.GitHubStrategyLoadTrace
import com.example.keios.feature.github.model.GitHubVersionCandidateSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

private data class GitHubApiCachedValue<T>(
    val value: T,
    val timestamp: Long
)

class GitHubApiTokenReleaseStrategy(
    private val apiToken: String = "",
    private val client: OkHttpClient = githubClient,
    private val apiBaseUrl: String = DEFAULT_GITHUB_API_BASE_URL
) : GitHubReleaseLookupStrategy {
    override val id: String = GitHubLookupStrategyOption.GitHubApiToken.storageId

    private val sanitizedToken: String = apiToken.trim()

    val authMode: GitHubApiAuthMode
        get() = if (sanitizedToken.isBlank()) GitHubApiAuthMode.Guest else GitHubApiAuthMode.Token

    override fun loadSnapshot(owner: String, repo: String): Result<GitHubRepositoryReleaseSnapshot> {
        return loadSnapshotTrace(owner, repo).result
    }

    fun loadSnapshotTrace(owner: String, repo: String): GitHubStrategyLoadTrace<GitHubRepositoryReleaseSnapshot> {
        val startedAt = System.currentTimeMillis()
        val entriesTrace = fetchReleaseEntriesTrace(owner, repo)
        val entries = entriesTrace.result.getOrElse { error ->
            return GitHubStrategyLoadTrace(
                result = Result.failure(error),
                fromCache = entriesTrace.fromCache,
                elapsedMs = System.currentTimeMillis() - startedAt,
                authMode = authMode
            )
        }
        val latestStableTrace = fetchLatestStableSignalTrace(owner, repo)
        val result = runCatching {
            val latestStableSignal = latestStableTrace.result.getOrElse {
                selectEffectiveLatestSignal(entries)
            }
            val latestPreEntry = pickPreferredEntry(
                entries.filter { entry ->
                    entry.isLikelyPreRelease &&
                        GitHubVersionUtils.hasComparableVersionCandidates(
                            entry.versionCandidates,
                            GitHubVersionCandidateSource.Link.priority
                        ) &&
                        GitHubVersionUtils.isRelevantPreRelease(
                            preReleaseCandidates = entry.versionCandidates,
                            stableCandidates = latestStableSignal.versionCandidates,
                            preReleaseUpdatedAtMillis = entry.updatedAtMillis,
                            stableUpdatedAtMillis = latestStableSignal.updatedAtMillis
                        )
                }
            )
            val updatedAt = entries.maxOfOrNull { it.updatedAtMillis ?: Long.MIN_VALUE }
                ?.takeIf { it > Long.MIN_VALUE }

            GitHubRepositoryReleaseSnapshot(
                strategyId = id,
                feed = GitHubAtomFeed(
                    title = "$owner/$repo releases",
                    feedUrl = buildApiUrl(owner, repo),
                    updatedAtMillis = updatedAt,
                    entries = entries
                ),
                latestStable = latestStableSignal,
                latestPreRelease = latestPreEntry?.toReleaseSignal()
            )
        }
        return GitHubStrategyLoadTrace(
            result = result,
            fromCache = entriesTrace.fromCache && latestStableTrace.fromCache,
            elapsedMs = System.currentTimeMillis() - startedAt,
            authMode = authMode
        )
    }

    fun fetchReleaseEntries(owner: String, repo: String, limit: Int = 30): Result<List<GitHubAtomReleaseEntry>> {
        return fetchReleaseEntriesTrace(owner, repo, limit).result
    }

    internal fun fetchReleaseEntriesTrace(
        owner: String,
        repo: String,
        limit: Int = 30
    ): GitHubStrategyLoadTrace<List<GitHubAtomReleaseEntry>> {
        val startedAt = System.currentTimeMillis()
        val key = cacheKey(owner, repo)
        val now = System.currentTimeMillis()
        releaseCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let {
            return GitHubStrategyLoadTrace(
                result = it.value,
                fromCache = true,
                elapsedMs = System.currentTimeMillis() - startedAt,
                authMode = authMode
            )
        }

        val result = fetch(buildApiUrl(owner, repo)).map { body ->
            parseReleaseEntries(
                json = body,
                owner = owner,
                repo = repo
            ).take(limit)
        }
        releaseCache[key] = GitHubApiCachedValue(result, now)
        return GitHubStrategyLoadTrace(
            result = result,
            fromCache = false,
            elapsedMs = System.currentTimeMillis() - startedAt,
            authMode = authMode
        )
    }

    override fun clearCaches() {
        clearSharedCaches()
    }

    private fun fetchLatestStableSignalTrace(
        owner: String,
        repo: String
    ): GitHubStrategyLoadTrace<GitHubReleaseVersionSignals> {
        val startedAt = System.currentTimeMillis()
        val key = cacheKey(owner, repo) + "|latest"
        val now = System.currentTimeMillis()
        stableCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let {
            return GitHubStrategyLoadTrace(
                result = it.value,
                fromCache = true,
                elapsedMs = System.currentTimeMillis() - startedAt,
                authMode = authMode
            )
        }

        val result = fetch(buildLatestApiUrl(owner, repo)).mapCatching { body ->
            val release = JSONObject(body)
            val entry = parseReleaseEntry(
                release = release,
                owner = owner,
                repo = repo
            ) ?: error("latest release missing")
            check(!entry.isLikelyPreRelease) { "latest release is not stable" }
            entry.toReleaseSignal()
        }
        stableCache[key] = GitHubApiCachedValue(result, now)
        return GitHubStrategyLoadTrace(
            result = result,
            fromCache = false,
            elapsedMs = System.currentTimeMillis() - startedAt,
            authMode = authMode
        )
    }

    fun checkCredential(): Result<GitHubApiCredentialStatus> {
        return checkCredentialTrace().result
    }

    fun checkCredentialTrace(): GitHubStrategyLoadTrace<GitHubApiCredentialStatus> {
        val startedAt = System.currentTimeMillis()
        val key = "credential|${cacheKey(owner = "_", repo = "_")}"
        val now = System.currentTimeMillis()
        credentialCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let {
            return GitHubStrategyLoadTrace(
                result = it.value,
                fromCache = true,
                elapsedMs = System.currentTimeMillis() - startedAt,
                authMode = authMode
            )
        }

        val result = fetch(buildRateLimitUrl()).map { body ->
            parseCredentialStatus(body)
        }
        credentialCache[key] = GitHubApiCachedValue(result, now)
        return GitHubStrategyLoadTrace(
            result = result,
            fromCache = false,
            elapsedMs = System.currentTimeMillis() - startedAt,
            authMode = authMode
        )
    }

    private fun fetch(url: String): Result<String> = runCatching {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", GITHUB_USER_AGENT)
        if (sanitizedToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $sanitizedToken")
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body.string()
            if (!response.isSuccessful) {
                error(buildErrorMessage(response, bodyText))
            }
            bodyText
        }
    }

    internal fun parseReleaseEntries(
        json: String,
        owner: String,
        repo: String
    ): List<GitHubAtomReleaseEntry> {
        val array = JSONArray(json)
        val entries = buildList {
            for (index in 0 until array.length()) {
                val release = array.optJSONObject(index) ?: continue
                val entry = parseReleaseEntry(release, owner, repo) ?: continue
                add(entry)
            }
        }

        return entries.sortedWith(
            compareByDescending<GitHubAtomReleaseEntry> { it.updatedAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.link }
        )
    }

    private fun buildApiUrl(owner: String, repo: String): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/releases?per_page=30"
    }

    private fun buildLatestApiUrl(owner: String, repo: String): String {
        return "${apiBaseUrl.trimEnd('/')}/repos/$owner/$repo/releases/latest"
    }

    private fun buildRateLimitUrl(): String {
        return "${apiBaseUrl.trimEnd('/')}/rate_limit"
    }

    private fun parseReleaseEntry(
        release: JSONObject,
        owner: String,
        repo: String
    ): GitHubAtomReleaseEntry? {
        if (release.optBoolean("draft", false)) return null

        val rawTag = release.optString("tag_name").trim()
        val name = release.optString("name").trim().ifBlank { rawTag }
        if (rawTag.isBlank() && name.isBlank()) return null

        val htmlUrl = release.optString("html_url").trim().ifBlank {
            GitHubVersionUtils.buildReleaseUrl(owner, repo)
        }
        val releaseId = release.opt("id")?.toString().orEmpty()
        val body = release.optString("body")
        val contentPreview = GitHubAtomHeuristics.buildContentPreview(body)
        val heuristicsChannel = GitHubAtomHeuristics.detectReleaseChannel(
            tag = rawTag,
            title = name,
            contentPreview = contentPreview
        )
        val prereleaseFlag = release.optBoolean("prerelease", false)
        val channel = when {
            prereleaseFlag && !heuristicsChannel.isPreRelease -> GitHubReleaseChannel.PREVIEW
            else -> heuristicsChannel
        }
        val author = release.optJSONObject("author")
        val authorName = author?.optString("login").orEmpty().trim()
        val authorAvatarUrl = author?.optString("avatar_url").orEmpty().trim()
        val publishedAtMillis = release.optString("published_at").parseIsoInstantOrNull()
            ?: release.optString("created_at").parseIsoInstantOrNull()
        val versionCandidates = GitHubVersionUtils.buildVersionCandidates(
            GitHubVersionCandidateSource.Tag to rawTag,
            GitHubVersionCandidateSource.Title to name,
            GitHubVersionCandidateSource.Link to htmlUrl,
            GitHubVersionCandidateSource.Id to releaseId,
            GitHubVersionCandidateSource.Content to contentPreview
        )
        if (prereleaseFlag && !GitHubVersionUtils.hasComparableVersionCandidates(versionCandidates, GitHubVersionCandidateSource.Link.priority)) {
            return null
        }

        return GitHubAtomReleaseEntry(
            entryId = release.optString("node_id").ifBlank { releaseId },
            tag = rawTag.ifBlank { name },
            title = name,
            link = htmlUrl,
            updatedAtMillis = publishedAtMillis,
            contentHtml = "",
            contentText = body,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            versionCandidates = versionCandidates,
            channel = channel,
            isLikelyPreRelease = prereleaseFlag || channel.isPreRelease
        )
    }

    private fun selectEffectiveLatestSignal(
        entries: List<GitHubAtomReleaseEntry>
    ): GitHubReleaseVersionSignals {
        // Some young projects only publish prereleases for a long time. In that case we still need
        // a usable "latest" signal instead of treating the repository as invalid.
        return pickPreferredEntry(entries.filter { !it.isLikelyPreRelease })
            ?.toReleaseSignal()
            ?: pickPreferredEntry(entries)
                ?.toReleaseSignal()
            ?: error("no release entries")
    }

    private fun cacheKey(owner: String, repo: String): String {
        val authKey = when (authMode) {
            GitHubApiAuthMode.Guest -> "guest"
            GitHubApiAuthMode.Token -> sanitizedToken.hashCode().toString()
        }
        return "$authKey|$owner/$repo"
    }

    private fun buildErrorMessage(response: Response, bodyText: String): String {
        val code = response.code
        val apiMessage = runCatching {
            JSONObject(bodyText).optString("message").trim()
        }.getOrDefault("")
        val rateRemaining = response.header("X-RateLimit-Remaining").orEmpty()
        val rateResetEpochSeconds = response.header("X-RateLimit-Reset").orEmpty().toLongOrNull()
        val rateResetSuffix = rateResetEpochSeconds
            ?.let { resetEpochSeconds ->
                val waitMinutes = ((resetEpochSeconds * 1000L) - System.currentTimeMillis())
                    .coerceAtLeast(0L) / 60_000L
                if (waitMinutes > 0) "，预计 $waitMinutes 分钟后恢复" else ""
            }
            .orEmpty()
        val looksRateLimited = code == 429 ||
            rateRemaining == "0" ||
            apiMessage.contains("rate limit", ignoreCase = true)
        return when (code) {
            401 -> "GitHub API token 无效或已过期"
            403, 429 -> when {
                looksRateLimited && authMode == GitHubApiAuthMode.Guest ->
                    "GitHub 游客 API 已限流，请稍后重试或填写 token$rateResetSuffix"
                looksRateLimited ->
                    "GitHub API 已限流$rateResetSuffix"
                else ->
                    "GitHub API 被拒绝访问${apiMessage.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}"
            }
            404 -> "仓库不存在，或当前 token 无权访问该仓库"
            else -> "GitHub API 请求失败 (HTTP $code${apiMessage.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""})"
        }
    }

    internal fun parseCredentialStatus(json: String): GitHubApiCredentialStatus {
        val root = JSONObject(json)
        val core = root.optJSONObject("resources")
            ?.optJSONObject("core")
        val limit = core?.optInt("limit", 0) ?: 0
        val remaining = core?.optInt("remaining", 0) ?: 0
        val used = core?.optInt("used", 0) ?: 0
        val resetAtMillis = core?.optLong("reset", 0L)
            ?.takeIf { it > 0L }
            ?.times(1000L)
        return GitHubApiCredentialStatus(
            authMode = authMode,
            coreLimit = limit,
            coreRemaining = remaining,
            coreUsed = used,
            resetAtMillis = resetAtMillis
        )
    }

    private fun pickPreferredEntry(entries: List<GitHubAtomReleaseEntry>): GitHubAtomReleaseEntry? {
        return entries.maxWithOrNull { left, right ->
            val byVersion = GitHubVersionUtils.compareStructuredCandidateSets(
                left.versionCandidates,
                right.versionCandidates
            )
            when {
                byVersion != null && byVersion != 0 -> byVersion
                else -> compareValues(
                    left.updatedAtMillis ?: Long.MIN_VALUE,
                    right.updatedAtMillis ?: Long.MIN_VALUE
                )
            }
        }
    }

    private fun GitHubAtomReleaseEntry.toReleaseSignal(): GitHubReleaseVersionSignals {
        return GitHubReleaseVersionSignals(
            displayVersion = displayVersion,
            rawTag = tag,
            rawName = title,
            link = link,
            updatedAtMillis = updatedAtMillis,
            versionCandidates = versionCandidates,
            source = GitHubReleaseSignalSource.GitHubApi,
            channel = channel,
            authorName = authorName
        )
    }

    private fun String.parseIsoInstantOrNull(): Long? {
        return runCatching {
            if (isBlank()) null else Instant.parse(this).toEpochMilli()
        }.getOrNull()
    }

    companion object {
        private const val CACHE_TTL_MS = 90_000L
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val GITHUB_USER_AGENT = "KeiOS-App/1.0 (Android)"
        private const val DEFAULT_GITHUB_API_BASE_URL = "https://api.github.com"

        private val releaseCache = mutableMapOf<String, GitHubApiCachedValue<Result<List<GitHubAtomReleaseEntry>>>>()
        private val stableCache = mutableMapOf<String, GitHubApiCachedValue<Result<GitHubReleaseVersionSignals>>>()
        private val credentialCache = mutableMapOf<String, GitHubApiCachedValue<Result<GitHubApiCredentialStatus>>>()

        private val githubClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .fastFallback(true)
                .build()
        }

        fun clearSharedCaches() {
            releaseCache.clear()
            stableCache.clear()
            credentialCache.clear()
        }
    }
}
