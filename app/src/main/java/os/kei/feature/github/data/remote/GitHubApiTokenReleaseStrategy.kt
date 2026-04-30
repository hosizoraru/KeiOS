package os.kei.feature.github.data.remote

import os.kei.feature.github.model.GitHubAtomFeed
import os.kei.feature.github.model.GitHubAtomReleaseEntry
import os.kei.feature.github.model.GitHubApiAuthMode
import os.kei.feature.github.model.GitHubApiCredentialStatus
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.github.model.GitHubReleaseChannel
import os.kei.feature.github.model.GitHubReleaseSignalSource
import os.kei.feature.github.model.GitHubReleaseVersionSignals
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import os.kei.feature.github.model.GitHubStrategyLoadTrace
import os.kei.feature.github.model.GitHubVersionCandidateSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
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
            val latestPreEntry = pickLatestPreReleaseEntry(
                entries.filter { entry ->
                    entry.isLikelyPreRelease &&
                        GitHubVersionUtils.hasMeaningfulPreReleaseVersionCandidates(
                            entry.versionCandidates,
                            GitHubVersionCandidateSource.Link.priority
                        )
                }
            )
            val fallbackStableEntry = pickLatestStableEntry(entries.filter { !it.isLikelyPreRelease })
            val latestStableSignal = latestStableTrace.result.getOrElse {
                fallbackStableEntry?.toReleaseSignal()
                    ?: latestPreEntry?.toReleaseSignal()
                    ?: error("no release entries")
            }
            val hasStableRelease = latestStableTrace.result.isSuccess || fallbackStableEntry != null
            val latestPreSignal = latestPreEntry
                ?.toReleaseSignal()
                ?.takeUnless { preReleaseSignal ->
                    hasStableRelease && GitHubVersionUtils.referToSameReleaseVersion(
                        preReleaseSignal.versionCandidates,
                        latestStableSignal.versionCandidates
                    )
                }
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
                hasStableRelease = hasStableRelease,
                latestPreRelease = latestPreSignal
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
        if (result.isSuccess) {
            releaseCache[key] = GitHubApiCachedValue(result, now)
        } else {
            releaseCache.remove(key)
        }
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
        if (result.isSuccess) {
            stableCache[key] = GitHubApiCachedValue(result, now)
        } else {
            stableCache.remove(key)
        }
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
        if (result.isSuccess) {
            credentialCache[key] = GitHubApiCachedValue(result, now)
        } else {
            credentialCache.remove(key)
        }
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
        val prereleaseFlag = release.optBoolean("prerelease", false)
        val heuristicsChannel = GitHubAtomHeuristics.detectReleaseChannel(
            tag = rawTag,
            title = name,
            contentPreview = if (prereleaseFlag) contentPreview else ""
        )
        val channel = when {
            prereleaseFlag && !heuristicsChannel.isPreRelease -> GitHubReleaseChannel.PREVIEW
            prereleaseFlag -> heuristicsChannel
            else -> GitHubReleaseChannel.STABLE
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
        if (prereleaseFlag && !GitHubVersionUtils.hasMeaningfulPreReleaseVersionCandidates(versionCandidates, GitHubVersionCandidateSource.Link.priority)) {
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
            isLikelyPreRelease = prereleaseFlag
        )
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
                if (waitMinutes > 0) ", resets in about $waitMinutes min" else ""
            }
            .orEmpty()
        val looksRateLimited = code == 429 ||
            rateRemaining == "0" ||
            apiMessage.contains("rate limit", ignoreCase = true)
        return when (code) {
            401 -> "GitHub API token is invalid or expired"
            403, 429 -> when {
                looksRateLimited && authMode == GitHubApiAuthMode.Guest ->
                    "GitHub guest API is rate limited. Try again later or enter a token$rateResetSuffix"
                looksRateLimited ->
                    "GitHub API is rate limited$rateResetSuffix"
                else ->
                    "GitHub API access was denied${apiMessage.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}"
            }
            404 -> "Repository does not exist, or the current token lacks repository access"
            else -> "GitHub API request failed (HTTP $code${apiMessage.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""})"
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

    private fun pickLatestStableEntry(entries: List<GitHubAtomReleaseEntry>): GitHubAtomReleaseEntry? {
        return entries.maxWithOrNull(
            compareBy<GitHubAtomReleaseEntry> { it.updatedAtMillis ?: Long.MIN_VALUE }
                .thenComparator { left, right ->
                    GitHubVersionUtils.compareStructuredCandidateSets(
                        left.versionCandidates,
                        right.versionCandidates
                    ) ?: 0
                }
        )
    }

    private fun pickLatestPreReleaseEntry(entries: List<GitHubAtomReleaseEntry>): GitHubAtomReleaseEntry? {
        return entries.maxWithOrNull { left, right ->
            val byPublishedAt = compareValues(
                left.updatedAtMillis ?: Long.MIN_VALUE,
                right.updatedAtMillis ?: Long.MIN_VALUE
            )
            when {
                byPublishedAt != 0 -> byPublishedAt
                else -> GitHubVersionUtils.compareStructuredCandidateSets(
                    left.versionCandidates,
                    right.versionCandidates
                ) ?: 0
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

        private val releaseCache =
            ConcurrentHashMap<String, GitHubApiCachedValue<Result<List<GitHubAtomReleaseEntry>>>>()
        private val stableCache =
            ConcurrentHashMap<String, GitHubApiCachedValue<Result<GitHubReleaseVersionSignals>>>()
        private val credentialCache =
            ConcurrentHashMap<String, GitHubApiCachedValue<Result<GitHubApiCredentialStatus>>>()

        private val githubClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .callTimeout(18, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(14, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
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
