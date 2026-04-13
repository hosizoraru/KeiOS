package com.example.keios.feature.github.data.remote

import com.example.keios.feature.github.model.GitHubAtomFeed
import com.example.keios.feature.github.model.GitHubAtomReleaseEntry
import com.example.keios.feature.github.model.GitHubReleaseSignalSource
import com.example.keios.feature.github.model.GitHubReleaseVersionSignals
import com.example.keios.feature.github.model.GitHubRepositoryReleaseSnapshot
import com.example.keios.feature.github.model.GitHubStrategyLoadTrace
import com.example.keios.feature.github.model.GitHubVersionCandidateSource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URLDecoder
import java.time.Instant
import java.util.concurrent.TimeUnit

private data class CachedValue<T>(
    val value: T,
    val timestamp: Long
)

object GitHubAtomReleaseStrategy : GitHubReleaseLookupStrategy {
    override val id: String = "atom_feed"

    private const val CACHE_TTL_MS = 90_000L
    private const val GITHUB_USER_AGENT = "KeiOS-App/1.0 (Android)"

    private val feedCache = mutableMapOf<String, CachedValue<Result<GitHubAtomFeed>>>()
    private val stableCache = mutableMapOf<String, CachedValue<Result<GitHubReleaseVersionSignals>>>()

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

    private val githubNoRedirectClient: OkHttpClient by lazy {
        githubClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    override fun loadSnapshot(owner: String, repo: String): Result<GitHubRepositoryReleaseSnapshot> {
        return loadSnapshotTrace(owner, repo).result
    }

    fun loadSnapshotTrace(
        owner: String,
        repo: String,
        atomFeedUrl: String = buildFeedUrl(owner, repo),
        latestReleaseUrl: String = buildLatestReleaseUrl(owner, repo),
        requestClient: OkHttpClient = githubClient,
        noRedirectRequestClient: OkHttpClient = githubNoRedirectClient
    ): GitHubStrategyLoadTrace<GitHubRepositoryReleaseSnapshot> {
        val startedAt = System.currentTimeMillis()
        val feedTrace = fetchAtomFeedTrace(
            owner = owner,
            repo = repo,
            atomFeedUrl = atomFeedUrl,
            requestClient = requestClient
        )
        val feed = feedTrace.result.getOrElse { error ->
            return GitHubStrategyLoadTrace(
                result = Result.failure(error),
                fromCache = feedTrace.fromCache,
                elapsedMs = System.currentTimeMillis() - startedAt
            )
        }
        val latestStableTrace = fetchLatestStableSignalTrace(
            owner = owner,
            repo = repo,
            feed = feed,
            latestReleaseUrl = latestReleaseUrl,
            noRedirectRequestClient = noRedirectRequestClient
        )
        val latestStable = latestStableTrace.result.getOrElse { error ->
            return GitHubStrategyLoadTrace(
                result = Result.failure(error),
                fromCache = feedTrace.fromCache && latestStableTrace.fromCache,
                elapsedMs = System.currentTimeMillis() - startedAt
            )
        }
        val latestPre = pickPreferredEntry(
            feed.entries.filter { entry ->
                entry.isLikelyPreRelease &&
                    GitHubVersionUtils.hasComparableVersionCandidates(
                        entry.versionCandidates,
                        GitHubVersionCandidateSource.Link.priority
                    ) &&
                    GitHubVersionUtils.isRelevantPreRelease(
                        preReleaseCandidates = entry.versionCandidates,
                        stableCandidates = latestStable.versionCandidates,
                        preReleaseUpdatedAtMillis = entry.updatedAtMillis,
                        stableUpdatedAtMillis = latestStable.updatedAtMillis
                    )
            }
        )
            ?.toReleaseSignal(GitHubReleaseSignalSource.AtomEntry)

        return GitHubStrategyLoadTrace(
            result = Result.success(
                GitHubRepositoryReleaseSnapshot(
                    strategyId = id,
                    feed = feed,
                    latestStable = latestStable,
                    latestPreRelease = latestPre
                )
            ),
            fromCache = feedTrace.fromCache && latestStableTrace.fromCache,
            elapsedMs = System.currentTimeMillis() - startedAt
        )
    }

    fun fetchAtomFeed(owner: String, repo: String): Result<GitHubAtomFeed> {
        return fetchAtomFeedTrace(owner, repo).result
    }

    internal fun fetchAtomFeedTrace(
        owner: String,
        repo: String,
        atomFeedUrl: String = buildFeedUrl(owner, repo),
        requestClient: OkHttpClient = githubClient
    ): GitHubStrategyLoadTrace<GitHubAtomFeed> {
        val startedAt = System.currentTimeMillis()
        val key = "$owner/$repo|$atomFeedUrl"
        val now = System.currentTimeMillis()
        feedCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let {
            return GitHubStrategyLoadTrace(
                result = it.value,
                fromCache = true,
                elapsedMs = System.currentTimeMillis() - startedAt
            )
        }

        val result = fetch(atomFeedUrl, requestClient).map { body ->
            parseAtomFeed(xml = body, feedUrl = atomFeedUrl)
        }
        feedCache[key] = CachedValue(result, now)
        return GitHubStrategyLoadTrace(
            result = result,
            fromCache = false,
            elapsedMs = System.currentTimeMillis() - startedAt
        )
    }

    fun fetchReleaseEntries(owner: String, repo: String, limit: Int = 30): Result<List<GitHubAtomReleaseEntry>> {
        return fetchAtomFeed(owner, repo).map { it.entries.take(limit) }
    }

    private fun fetchLatestStableSignalTrace(
        owner: String,
        repo: String,
        feed: GitHubAtomFeed,
        latestReleaseUrl: String = buildLatestReleaseUrl(owner, repo),
        noRedirectRequestClient: OkHttpClient = githubNoRedirectClient
    ): GitHubStrategyLoadTrace<GitHubReleaseVersionSignals> {
        val startedAt = System.currentTimeMillis()
        val key = "$owner/$repo|$latestReleaseUrl"
        val now = System.currentTimeMillis()
        stableCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let {
            return GitHubStrategyLoadTrace(
                result = it.value,
                fromCache = true,
                elapsedMs = System.currentTimeMillis() - startedAt
            )
        }

        val request = Request.Builder()
            .url(latestReleaseUrl)
            .get()
            .header("User-Agent", GITHUB_USER_AGENT)
            .build()

        val result = runCatching {
            noRedirectRequestClient.newCall(request).execute().use { response ->
                val location = response.header("Location").orEmpty()
                val finalUrl = when {
                    location.isNotBlank() -> location
                    response.request.url.toString().contains("/releases/tag/") -> response.request.url.toString()
                    else -> ""
                }

                if (finalUrl.contains("/releases/tag/")) {
                    val rawTag = URLDecoder.decode(
                        finalUrl.substringAfterLast("/releases/tag/").trim('/'),
                        Charsets.UTF_8.name()
                    )
                    val matchedEntry = feed.entries.firstOrNull { entry ->
                        entry.tag.equals(rawTag, ignoreCase = true) ||
                            GitHubVersionUtils.compareCandidateSetsWithSources(
                                GitHubVersionUtils.normalizeVersionCandidates(rawTag),
                                entry.versionCandidates
                            ) == 0
                    }
                    matchedEntry?.toReleaseSignal(
                        source = GitHubReleaseSignalSource.LatestRedirect,
                        linkOverride = finalUrl
                    ) ?: GitHubReleaseVersionSignals(
                        displayVersion = rawTag,
                        rawTag = rawTag,
                        rawName = rawTag,
                        link = finalUrl,
                        updatedAtMillis = feed.updatedAtMillis,
                        versionCandidates = GitHubVersionUtils.buildVersionCandidates(
                            GitHubVersionCandidateSource.Tag to rawTag
                        ),
                        source = GitHubReleaseSignalSource.LatestRedirect,
                        channel = GitHubAtomHeuristics.detectReleaseChannel(rawTag, rawTag, ""),
                        authorName = ""
                    )
                } else {
                    selectEffectiveLatestSignal(feed.entries)
                }
            }
        }

        stableCache[key] = CachedValue(result, now)
        return GitHubStrategyLoadTrace(
            result = result,
            fromCache = false,
            elapsedMs = System.currentTimeMillis() - startedAt
        )
    }

    private fun selectEffectiveLatestSignal(
        entries: List<GitHubAtomReleaseEntry>
    ): GitHubReleaseVersionSignals {
        // Some young projects only publish prereleases for a long time. In that case we still need
        // a usable "latest" signal instead of treating the repository as invalid.
        return pickPreferredEntry(entries.filter { !it.isLikelyPreRelease })
            ?.toReleaseSignal(GitHubReleaseSignalSource.AtomFallback)
            ?: pickPreferredEntry(entries)
                ?.toReleaseSignal(GitHubReleaseSignalSource.AtomFallback)
            ?: error("no release entries")
    }

    override fun clearCaches() {
        feedCache.clear()
        stableCache.clear()
    }

    private fun fetch(url: String, requestClient: OkHttpClient = githubClient): Result<String> = runCatching {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", GITHUB_USER_AGENT)
            .build()
        requestClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body.string()
        }
    }

    private fun buildFeedUrl(owner: String, repo: String): String {
        return "https://github.com/$owner/$repo/releases.atom"
    }

    private fun buildLatestReleaseUrl(owner: String, repo: String): String {
        return "https://github.com/$owner/$repo/releases/latest"
    }

    private fun parseAtomFeed(
        xml: String,
        feedUrl: String
    ): GitHubAtomFeed {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(xml.reader())
        }

        var feedTitle = ""
        var feedUpdatedAt: Long? = null
        val entries = mutableListOf<GitHubAtomReleaseEntry>()

        var eventType = parser.eventType
        var inEntry = false
        var inAuthor = false

        var entryId = ""
        var entryUpdatedText = ""
        var entryLink = ""
        var entryTitle = ""
        var entryContentHtml = ""
        var entryAuthorName = ""
        var entryAuthorAvatarUrl = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.orEmpty()
                    when {
                        name == "entry" -> {
                            inEntry = true
                            inAuthor = false
                            entryId = ""
                            entryUpdatedText = ""
                            entryLink = ""
                            entryTitle = ""
                            entryContentHtml = ""
                            entryAuthorName = ""
                            entryAuthorAvatarUrl = ""
                        }

                        name == "author" && inEntry -> inAuthor = true

                        name == "title" && !inEntry -> {
                            feedTitle = GitHubAtomHeuristics.decodeXmlEscapes(parser.nextText()).trim()
                        }

                        name == "updated" && !inEntry -> {
                            feedUpdatedAt = parser.nextText().trim().parseIsoInstantOrNull()
                        }

                        name == "id" && inEntry -> {
                            entryId = GitHubAtomHeuristics.decodeXmlEscapes(parser.nextText()).trim()
                        }

                        name == "updated" && inEntry -> {
                            entryUpdatedText = parser.nextText().trim()
                        }

                        name == "title" && inEntry -> {
                            entryTitle = GitHubAtomHeuristics.decodeXmlEscapes(parser.nextText()).trim()
                        }

                        name == "content" && inEntry -> {
                            entryContentHtml = GitHubAtomHeuristics.decodeXmlEscapes(parser.nextText()).trim()
                        }

                        name == "name" && inEntry && inAuthor -> {
                            entryAuthorName = GitHubAtomHeuristics.decodeXmlEscapes(parser.nextText()).trim()
                        }

                        name == "link" && inEntry -> {
                            val rel = parser.getAttributeValue(null, "rel").orEmpty()
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            if (rel == "alternate" && href.isNotBlank()) {
                                entryLink = href
                            }
                        }

                        name.endsWith("thumbnail") && inEntry -> {
                            entryAuthorAvatarUrl = parser.getAttributeValue(null, "url").orEmpty()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name.orEmpty()
                    when {
                        name == "author" && inEntry -> inAuthor = false
                        name == "entry" && inEntry -> {
                            val effectiveLink = entryLink.ifBlank { entryId }
                            val derivedTag = extractTag(effectiveLink, entryTitle, entryId)
                            if (derivedTag.isNotBlank()) {
                                val contentText = GitHubAtomHeuristics.htmlToPlainText(entryContentHtml)
                                val contentPreview = GitHubAtomHeuristics.buildContentPreview(contentText)
                                val channel = GitHubAtomHeuristics.detectReleaseChannel(
                                    tag = derivedTag,
                                    title = entryTitle,
                                    contentPreview = contentPreview
                                )
                                val releaseEntry = GitHubAtomReleaseEntry(
                                    entryId = entryId,
                                    tag = derivedTag,
                                    title = entryTitle.ifBlank { derivedTag },
                                    link = effectiveLink,
                                    updatedAtMillis = entryUpdatedText.parseIsoInstantOrNull(),
                                    contentHtml = entryContentHtml,
                                    contentText = contentText,
                                    authorName = entryAuthorName,
                                    authorAvatarUrl = entryAuthorAvatarUrl,
                                    versionCandidates = GitHubVersionUtils.buildVersionCandidates(
                                        GitHubVersionCandidateSource.Tag to derivedTag,
                                        GitHubVersionCandidateSource.Title to entryTitle,
                                        GitHubVersionCandidateSource.Link to effectiveLink,
                                        GitHubVersionCandidateSource.Id to entryId.substringAfterLast('/'),
                                        GitHubVersionCandidateSource.Content to contentPreview
                                    ),
                                    channel = channel,
                                    isLikelyPreRelease = channel.isPreRelease
                                )
                                if (!releaseEntry.isLikelyPreRelease ||
                                    GitHubVersionUtils.hasComparableVersionCandidates(
                                        releaseEntry.versionCandidates,
                                        GitHubVersionCandidateSource.Link.priority
                                    )
                                ) {
                                    entries += releaseEntry
                                }
                            }
                            inEntry = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val sortedEntries = entries.sortedWith(
            compareByDescending<GitHubAtomReleaseEntry> { it.updatedAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.link }
        )

        return GitHubAtomFeed(
            title = feedTitle,
            feedUrl = feedUrl,
            updatedAtMillis = feedUpdatedAt,
            entries = sortedEntries
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

    private fun extractTag(
        link: String,
        title: String,
        entryId: String
    ): String {
        return when {
            link.contains("/releases/tag/") -> URLDecoder.decode(
                link.substringAfter("/releases/tag/").trim('/'),
                Charsets.UTF_8.name()
            )

            entryId.isNotBlank() -> entryId.substringAfterLast('/').trim()
            else -> title.trim()
        }
    }

    private fun GitHubAtomReleaseEntry.toReleaseSignal(
        source: GitHubReleaseSignalSource,
        linkOverride: String? = null
    ): GitHubReleaseVersionSignals {
        return GitHubReleaseVersionSignals(
            displayVersion = displayVersion,
            rawTag = tag,
            rawName = title,
            link = linkOverride ?: link,
            updatedAtMillis = updatedAtMillis,
            versionCandidates = versionCandidates,
            source = source,
            channel = channel,
            authorName = authorName
        )
    }

    private fun String.parseIsoInstantOrNull(): Long? {
        return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
    }
}
