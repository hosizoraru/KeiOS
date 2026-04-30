package os.kei.feature.github.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

data class GitHubReleaseAssetFile(
    val name: String,
    val downloadUrl: String,
    val apiAssetUrl: String = "",
    val sizeBytes: Long,
    val downloadCount: Int,
    val contentType: String = "",
    val updatedAtMillis: Long? = null
)

data class GitHubReleaseAssetBundle(
    val releaseName: String,
    val tagName: String,
    val htmlUrl: String,
    val releaseUpdatedAtMillis: Long? = null,
    val assets: List<GitHubReleaseAssetFile>,
    val showingAllAssets: Boolean = false,
    val shortCommitSha: String = "",
    val fetchSource: String = "",
    val sourceConfigSignature: String = ""
)

object GitHubReleaseAssetFetchSources {
    const val HTML = "html"
    const val API = "api"
}

private data class HtmlReleaseMetadata(
    val releaseName: String,
    val tagName: String,
    val htmlUrl: String,
    val releaseUpdatedAtMillis: Long?
)

object GitHubReleaseAssetRepository {
    private const val GITHUB_API_VERSION = "2022-11-28"
    private const val GITHUB_USER_AGENT = "KeiOS-App/1.0 (Android)"
    private const val DEFAULT_GITHUB_API_BASE_URL = "https://api.github.com"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(16, TimeUnit.SECONDS)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .fastFallback(true)
            .build()
    }

    fun fetchApkAssets(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String = "",
        preferHtml: Boolean = false,
        aggressiveFiltering: Boolean = false,
        includeAllAssets: Boolean = false,
        apiToken: String = ""
    ): Result<GitHubReleaseAssetBundle> {
        val normalizedTag = rawTag.trim()
        if (normalizedTag.isBlank()) {
            return Result.failure(IllegalArgumentException("missing release tag"))
        }

        val normalizedReleaseUrl = releaseUrl.trim()
        val resolvedHtmlReleaseUrl = normalizedReleaseUrl.ifBlank {
            GitHubVersionUtils.buildReleaseTagUrl(owner, repo, normalizedTag)
        }
        val primarySource = if (preferHtml) {
            GitHubReleaseAssetFetchSources.HTML
        } else {
            GitHubReleaseAssetFetchSources.API
        }
        val primary = if (preferHtml) {
            fetchReleaseFromHtml(owner, repo, normalizedTag, resolvedHtmlReleaseUrl, apiToken)
        } else {
            fetchReleaseByTagWithFallback(owner, repo, normalizedTag, normalizedReleaseUrl, apiToken)
        }
        val fallbackSource = when {
            preferHtml -> GitHubReleaseAssetFetchSources.API
            resolvedHtmlReleaseUrl.isNotBlank() -> GitHubReleaseAssetFetchSources.HTML
            else -> primarySource
        }
        val fallback = when {
            preferHtml -> {
                fetchReleaseByTagWithFallback(
                    owner = owner,
                    repo = repo,
                    rawTag = normalizedTag,
                    releaseUrl = normalizedReleaseUrl,
                    apiToken = apiToken
                )
            }
            resolvedHtmlReleaseUrl.isNotBlank() -> {
                fetchReleaseFromHtml(owner, repo, normalizedTag, resolvedHtmlReleaseUrl, apiToken)
            }
            else -> Result.failure(primary.exceptionOrNull() ?: IllegalStateException("release fetch failed"))
        }
        val resolvedSource = if (primary.isSuccess) primarySource else fallbackSource

        return (primary.takeIf { it.isSuccess } ?: fallback).mapCatching { release ->
            parseReleaseBundle(release)
                .copy(fetchSource = resolvedSource)
                .withResolvedShortCommitSha(owner, repo, normalizedTag, apiToken)
                .selectDisplayAssets(
                    aggressiveFiltering = aggressiveFiltering,
                    includeAllAssets = includeAllAssets
                )
        }
    }

    fun resolvePreferredDownloadUrl(
        asset: GitHubReleaseAssetFile,
        useApiAssetUrl: Boolean,
        apiToken: String = ""
    ): Result<String> {
        val token = apiToken.trim()
        val apiAssetUrl = asset.apiAssetUrl.trim()
        if (!useApiAssetUrl || token.isBlank() || apiAssetUrl.isBlank()) {
            return Result.success(asset.downloadUrl)
        }
        return resolveApiAssetDownloadUrl(apiAssetUrl, token).recoverCatching {
            asset.downloadUrl
        }
    }

    fun parseReleaseTagFromUrl(url: String): String {
        val raw = url.trim()
        if (raw.isBlank()) return ""
        val marker = "/releases/tag/"
        val fromPath = runCatching {
            val uri = URI(raw)
            val path = uri.rawPath.orEmpty()
            if (!path.contains(marker)) return@runCatching ""
            val encoded = path.substringAfter(marker).trim('/').trim()
            java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
        }.getOrDefault("")
        if (fromPath.isNotBlank()) return fromPath
        return raw.substringAfter(marker, "").trim('/')
    }

    internal fun List<GitHubReleaseAssetFile>.filterRelevantApks(aggressiveFiltering: Boolean): List<GitHubReleaseAssetFile> {
        val apkCandidates = filterNonSourceAssets()
            .asSequence()
            .filter { asset ->
                val lowerName = asset.name.lowercase(Locale.ROOT)
                lowerName.endsWith(".apk") && !lowerName.contains("metadata")
            }
            .toList()

        val hasExplicitArm64 = apkCandidates.any { asset ->
            val lowerName = asset.name.lowercase(Locale.ROOT)
            "arm64-v8a" in lowerName
        }

        return apkCandidates
            .asSequence()
            .filter { asset ->
                val lowerName = asset.name.lowercase(Locale.ROOT)
                !aggressiveFiltering || !isAggressivelyIgnoredApk(
                    lowerName = lowerName,
                    hasExplicitArm64 = hasExplicitArm64
                )
            }
            .sortForDisplay()
    }

    private fun List<GitHubReleaseAssetFile>.filterNonSourceAssets(): List<GitHubReleaseAssetFile> {
        return asSequence()
            .filter { asset -> !isSourceCodeArchive(asset.name) }
            .toList()
    }

    private fun GitHubReleaseAssetBundle.withResolvedShortCommitSha(
        owner: String,
        repo: String,
        rawTag: String,
        apiToken: String
    ): GitHubReleaseAssetBundle {
        val token = apiToken.trim()
        if (token.isBlank()) return copy(shortCommitSha = "")
        val commitSha = resolveShortCommitSha(owner, repo, rawTag, token).getOrDefault("")
        return copy(shortCommitSha = commitSha)
    }

    private fun GitHubReleaseAssetBundle.selectDisplayAssets(
        aggressiveFiltering: Boolean,
        includeAllAssets: Boolean
    ): GitHubReleaseAssetBundle {
        val allAssets = assets.filterNonSourceAssets().sortForDisplay()
        val apkAssets = allAssets.filterRelevantApks(aggressiveFiltering)
        val showingAllAssets = includeAllAssets || apkAssets.isEmpty()
        return copy(
            assets = if (showingAllAssets) allAssets else apkAssets,
            showingAllAssets = showingAllAssets
        )
    }

    private fun Sequence<GitHubReleaseAssetFile>.sortForDisplay(): List<GitHubReleaseAssetFile> {
        return sortedWith(
            compareBy<GitHubReleaseAssetFile> { assetDisplayPriority(it.name) }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        ).toList()
    }

    private fun List<GitHubReleaseAssetFile>.sortForDisplay(): List<GitHubReleaseAssetFile> {
        return asSequence().sortForDisplay()
    }

    private fun isAggressivelyIgnoredApk(
        lowerName: String,
        hasExplicitArm64: Boolean
    ): Boolean {
        return "armeabi-v7a" in lowerName ||
            "x86_64" in lowerName ||
            Regex("(^|[^a-z0-9])armeabi([^a-z0-9]|$)").containsMatchIn(lowerName) ||
            Regex("(^|[^a-z0-9])x86([^a-z0-9]|$)").containsMatchIn(lowerName) ||
            (hasExplicitArm64 && ("universal" in lowerName || "fat" in lowerName))
    }

    private fun isSourceCodeArchive(fileName: String): Boolean {
        return fileName.equals("Source code.zip", ignoreCase = true) ||
            fileName.equals("Source code.tar.gz", ignoreCase = true)
    }

    private fun fetchReleaseByTagWithFallback(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        apiToken: String
    ): Result<JSONObject> {
        val byTag = fetchReleaseByTag(owner, repo, rawTag, apiToken)
        if (byTag.isSuccess) return byTag

        val fallback = fetchReleaseList(owner, repo, apiToken).mapCatching { releases ->
            findMatchingRelease(releases, rawTag)
                ?: buildReleaseStub(
                    releaseName = rawTag,
                    rawTag = rawTag,
                    releaseUrl = releaseUrl
                )
        }
        return fallback.takeIf { it.isSuccess } ?: byTag
    }

    private fun resolveApiAssetDownloadUrl(
        apiAssetUrl: String,
        apiToken: String
    ): Result<String> = runCatching {
        val request = Request.Builder()
            .url(apiAssetUrl)
            .get()
            .header("Accept", "application/octet-stream")
            .header("Authorization", "Bearer ${apiToken.trim()}")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", GITHUB_USER_AGENT)
            .header("Connection", "close")
            .build()

        client.newCall(request).execute().use { response ->
            val redirectedUrl = response.request.url.toString()
            when {
                response.isSuccessful && redirectedUrl.isNotBlank() -> redirectedUrl
                response.isRedirect -> response.header("Location").orEmpty().ifBlank {
                    error("GitHub API asset download returned no redirect URL")
                }
                else -> error(buildApiAssetErrorMessage(response))
            }
        }
    }

    private fun buildApiAssetErrorMessage(response: Response): String {
        return when (response.code) {
            401 -> "GitHub API token is invalid or expired"
            403, 429 -> "GitHub API asset download is rate limited"
            404 -> "GitHub API asset URL has expired"
            else -> "GitHub API asset download failed (HTTP ${response.code})"
        }
    }

    private fun resolveShortCommitSha(
        owner: String,
        repo: String,
        rawTag: String,
        apiToken: String
    ): Result<String> = runCatching {
        val encodedTag = URLEncoder.encode(rawTag, Charsets.UTF_8.name()).replace("+", "%20")
        val refUrl = "${DEFAULT_GITHUB_API_BASE_URL.trimEnd('/')}/repos/$owner/$repo/git/ref/tags/$encodedTag"
        val refObject = JSONObject(fetchJson(refUrl, apiToken)).optJSONObject("object")
            ?: error("Git tag ref response is missing object")
        val refType = refObject.optString("type").trim()
        val refSha = refObject.optString("sha").trim()
        val commitSha = when {
            refType.equals("commit", ignoreCase = true) -> refSha
            refType.equals("tag", ignoreCase = true) && refSha.isNotBlank() -> {
                val tagUrl = "${DEFAULT_GITHUB_API_BASE_URL.trimEnd('/')}/repos/$owner/$repo/git/tags/$refSha"
                val tagObject = JSONObject(fetchJson(tagUrl, apiToken)).optJSONObject("object")
                    ?: error("Annotated tag response is missing object")
                if (tagObject.optString("type").trim().equals("commit", ignoreCase = true)) {
                    tagObject.optString("sha").trim()
                } else {
                    ""
                }
            }
            else -> ""
        }
        commitSha.take(7)
    }.getOrElse { "" }.let { Result.success(it) }

    private fun fetchReleaseByTag(
        owner: String,
        repo: String,
        rawTag: String,
        apiToken: String
    ): Result<JSONObject> = runCatching {
        val encodedTag = URLEncoder.encode(rawTag, Charsets.UTF_8.name()).replace("+", "%20")
        val url = "${DEFAULT_GITHUB_API_BASE_URL.trimEnd('/')}/repos/$owner/$repo/releases/tags/$encodedTag"
        JSONObject(fetchJson(url, apiToken))
    }

    private fun fetchReleaseList(
        owner: String,
        repo: String,
        apiToken: String
    ): Result<JSONArray> = runCatching {
        val url = "${DEFAULT_GITHUB_API_BASE_URL.trimEnd('/')}/repos/$owner/$repo/releases?per_page=30"
        JSONArray(fetchJson(url, apiToken))
    }

    private fun fetchJson(url: String, apiToken: String): String {
        val token = apiToken.trim()
        var lastError: Throwable? = null

        repeat(2) { attempt ->
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                    .header("User-Agent", GITHUB_USER_AGENT)
                    .header("Connection", "close")
                if (token.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val bodyText = response.body.string()
                    if (!response.isSuccessful) {
                        val apiMessage = runCatching { JSONObject(bodyText).optString("message").trim() }.getOrDefault("")
                        error(
                            when (response.code) {
                                401 -> "GitHub API token is invalid or expired"
                                403, 429 -> if (token.isBlank()) {
                                    "GitHub guest API is rate limited. Try again later or enter a token"
                                } else {
                                    "GitHub API is rate limited"
                                }
                                404 -> "No release was found for this tag"
                                else -> "GitHub release request failed (HTTP ${response.code}${apiMessage.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""})"
                            }
                        )
                    }
                    return bodyText
                }
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0 && error is IOException) {
                    Thread.sleep(220)
                }
            }
        }

        val message = lastError?.message.orEmpty()
        if (message.contains("connection closed", ignoreCase = true)) {
            error("GitHub connection closed unexpectedly. Try again later.")
        }
        throw lastError ?: IllegalStateException("GitHub release request failed")
    }

    private fun fetchReleaseFromHtml(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        apiToken: String
    ): Result<JSONObject> = runCatching {
        val normalizedReleaseUrl = releaseUrl.trim().ifBlank {
            GitHubVersionUtils.buildReleaseTagUrl(owner, repo, rawTag)
        }
        val releaseHtml = fetchHtml(normalizedReleaseUrl, apiToken)
        val metadata = parseReleaseMetadataFromHtml(releaseHtml, rawTag, normalizedReleaseUrl)
        val expandedAssetsUrl = parseExpandedAssetsUrl(releaseHtml).orEmpty()
            .ifBlank { buildExpandedAssetsUrl(owner, repo, rawTag) }
        val assets = runCatching {
            val expandedAssetsHtml = fetchHtml(expandedAssetsUrl, apiToken)
            parseAssetsFromExpandedHtml(expandedAssetsHtml, owner, repo, rawTag)
        }.recoverCatching {
            parseAssetsFromReleaseHtml(releaseHtml, owner, repo, rawTag)
        }.getOrThrow()
        buildReleaseStub(
            releaseName = metadata.releaseName,
            rawTag = metadata.tagName,
            releaseUrl = metadata.htmlUrl,
            releaseUpdatedAtMillis = metadata.releaseUpdatedAtMillis,
            assets = assets
        )
    }

    private fun fetchHtml(url: String, apiToken: String): String {
        val token = apiToken.trim()
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("User-Agent", GITHUB_USER_AGENT)
                    .header("Connection", "close")
                if (token.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val bodyText = response.body.string()
                    if (!response.isSuccessful) {
                        error("GitHub release page request failed (HTTP ${response.code})")
                    }
                    return bodyText
                }
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0 && error is IOException) Thread.sleep(220)
            }
        }
        val message = lastError?.message.orEmpty()
        if (message.contains("connection closed", ignoreCase = true)) {
            error("GitHub page connection closed unexpectedly. Try again later.")
        }
        throw lastError ?: IllegalStateException("GitHub release page request failed")
    }

    private fun parseAssetsFromReleaseHtml(
        html: String,
        owner: String,
        repo: String,
        rawTag: String
    ): List<GitHubReleaseAssetFile> {
        val hrefRegex = Regex("""href=\"([^\"]+)\""", RegexOption.IGNORE_CASE)
        val unique = linkedMapOf<String, GitHubReleaseAssetFile>()
        hrefRegex.findAll(html).forEach { match ->
            val rawHref = match.groupValues.getOrNull(1).orEmpty()
            val href = rawHref.replace("&amp;", "&")
            val normalizedUrl = when {
                href.startsWith("https://") || href.startsWith("http://") -> href
                href.startsWith("/") -> "https://github.com$href"
                else -> ""
            }
            if (normalizedUrl.isBlank()) return@forEach
            if (!normalizedUrl.contains("/$owner/$repo/releases/download/")) return@forEach
            val fileName = normalizedUrl.substringAfterLast('/').substringBefore('?')
            val decodedName = runCatching { URLDecoder.decode(fileName, Charsets.UTF_8.name()) }.getOrDefault(fileName)
            unique.putIfAbsent(
                decodedName,
                GitHubReleaseAssetFile(
                    name = decodedName,
                    downloadUrl = normalizedUrl,
                    sizeBytes = 0L,
                    downloadCount = 0,
                    contentType = "application/vnd.android.package-archive",
                    updatedAtMillis = null
                )
            )
        }
        return unique.values.toList().ifEmpty {
            error("No downloadable assets found on the release page: $rawTag")
        }
    }

    private fun parseReleaseMetadataFromHtml(
        html: String,
        rawTag: String,
        releaseUrl: String
    ): HtmlReleaseMetadata {
        val releaseName = Regex(
            """<h1[^>]*class="[^"]*d-inline[^"]*"[^>]*>(.*?)</h1>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .stripHtml()
            .decodeHtmlEntities()
            .trim()
            .ifBlank {
                Regex(
                    """<meta[^>]*property="og:title"[^>]*content="([^"]+)"""",
                    RegexOption.IGNORE_CASE
                ).find(html)
                    ?.groupValues
                    ?.getOrNull(1)
                    .orEmpty()
                    .decodeHtmlEntities()
                    .substringBefore(" · ")
                    .trim()
            }
            .ifBlank { rawTag }

        val releaseUpdatedAtMillis = Regex(
            """(?:released|published)\s+this\s*<relative-time[^>]*datetime="([^"]+)"""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .parseIsoInstantOrNull()

        return HtmlReleaseMetadata(
            releaseName = releaseName,
            tagName = rawTag.trim(),
            htmlUrl = releaseUrl.trim(),
            releaseUpdatedAtMillis = releaseUpdatedAtMillis
        )
    }

    private fun parseExpandedAssetsUrl(html: String): String? {
        val src = Regex(
            """<include-fragment[^>]*src="([^"]*?/releases/expanded_assets/[^"]+)"""",
            RegexOption.IGNORE_CASE
        ).find(html)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .replace("&amp;", "&")
            .trim()
        return normalizeGitHubUrl(src)
    }

    private fun buildExpandedAssetsUrl(
        owner: String,
        repo: String,
        rawTag: String
    ): String {
        val encodedTag = URLEncoder.encode(rawTag, Charsets.UTF_8.name()).replace("+", "%20")
        return "https://github.com/$owner/$repo/releases/expanded_assets/$encodedTag"
    }

    private fun parseAssetsFromExpandedHtml(
        html: String,
        owner: String,
        repo: String,
        rawTag: String
    ): List<GitHubReleaseAssetFile> {
        val rowRegex = Regex(
            """<li\b[^>]*class="[^"]*Box-row[^"]*"[^>]*>(.*?)</li>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val linkRegex = Regex(
            """<a[^>]*href="([^"]+)"[^>]*class="[^"]*Truncate[^"]*"[^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val sizeRegex = Regex(""">\s*(\d+(?:\.\d+)?)\s*([KMGT]?B)\s*<""", RegexOption.IGNORE_CASE)
        val updatedAtRegex = Regex("""<relative-time[^>]*datetime="([^"]+)"""", RegexOption.IGNORE_CASE)
        val unique = linkedMapOf<String, GitHubReleaseAssetFile>()
        rowRegex.findAll(html).forEach { rowMatch ->
            val rowHtml = rowMatch.groupValues.getOrNull(1).orEmpty()
            val linkMatch = linkRegex.find(rowHtml) ?: return@forEach
            val normalizedUrl = normalizeGitHubUrl(linkMatch.groupValues.getOrNull(1).orEmpty())
            if (normalizedUrl.isNullOrBlank()) return@forEach
            val fileName = inferHtmlAssetFileName(normalizedUrl, linkMatch.groupValues.getOrNull(2).orEmpty())
            if (fileName.isBlank()) return@forEach
            if (!normalizedUrl.contains("/$owner/$repo/")) return@forEach
            val sizeBytes = sizeRegex.find(rowHtml)?.let { sizeMatch ->
                parseSizeBytes(
                    sizeValue = sizeMatch.groupValues.getOrNull(1).orEmpty(),
                    sizeUnit = sizeMatch.groupValues.getOrNull(2).orEmpty()
                )
            } ?: 0L
            val updatedAtMillis = updatedAtRegex.find(rowHtml)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
                .parseIsoInstantOrNull()
            unique.putIfAbsent(
                fileName,
                GitHubReleaseAssetFile(
                    name = fileName,
                    downloadUrl = normalizedUrl,
                    sizeBytes = sizeBytes,
                    downloadCount = 0,
                    contentType = inferAssetContentType(fileName),
                    updatedAtMillis = updatedAtMillis
                )
            )
        }
        return unique.values.toList().ifEmpty {
            error("No downloadable assets found on the expanded_assets page: $rawTag")
        }
    }

    private fun inferHtmlAssetFileName(
        normalizedUrl: String,
        linkInnerHtml: String
    ): String {
        val urlFileName = when {
            normalizedUrl.contains("/releases/download/") -> {
                normalizedUrl.substringAfterLast('/').substringBefore('?')
            }
            normalizedUrl.contains("/archive/refs/tags/") -> when {
                normalizedUrl.endsWith(".tar.gz", ignoreCase = true) -> "Source code.tar.gz"
                normalizedUrl.endsWith(".zip", ignoreCase = true) -> "Source code.zip"
                else -> normalizedUrl.substringAfterLast('/').substringBefore('?')
            }
            else -> ""
        }
        val decodedUrlName = runCatching {
            URLDecoder.decode(urlFileName, Charsets.UTF_8.name())
        }.getOrDefault(urlFileName).trim()
        if (decodedUrlName.isNotBlank()) return decodedUrlName

        val linkText = linkInnerHtml.stripHtml().decodeHtmlEntities().trim()
        val sourceCodeMatch = Regex("""^Source code\s*\(([^)]+)\)$""", RegexOption.IGNORE_CASE)
            .find(linkText)
        if (sourceCodeMatch != null) {
            return "Source code.${sourceCodeMatch.groupValues[1].trim()}"
        }
        return linkText
    }

    private fun normalizeGitHubUrl(rawUrl: String): String? {
        val cleaned = rawUrl.replace("&amp;", "&").trim()
        if (cleaned.isBlank()) return null
        return when {
            cleaned.startsWith("https://", ignoreCase = true) ||
                cleaned.startsWith("http://", ignoreCase = true) -> cleaned
            cleaned.startsWith("/") -> "https://github.com$cleaned"
            else -> null
        }
    }

    private fun parseSizeBytes(
        sizeValue: String,
        sizeUnit: String
    ): Long {
        val number = sizeValue.trim().toDoubleOrNull() ?: return 0L
        val multiplier = when (sizeUnit.trim().uppercase(Locale.ROOT)) {
            "KB" -> 1024.0
            "MB" -> 1024.0 * 1024.0
            "GB" -> 1024.0 * 1024.0 * 1024.0
            "TB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
            else -> 1.0
        }
        return (number * multiplier).roundToLong()
    }

    private fun inferAssetContentType(fileName: String): String {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".apk") -> "application/vnd.android.package-archive"
            lower.endsWith(".apkm") -> "application/octet-stream"
            lower.endsWith(".zip") -> "application/zip"
            lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> "application/gzip"
            else -> ""
        }
    }

    private fun findMatchingRelease(releases: JSONArray, rawTag: String): JSONObject? {
        val normalizedTag = rawTag.trim()
        for (index in 0 until releases.length()) {
            val release = releases.optJSONObject(index) ?: continue
            val candidateTag = release.optString("tag_name").trim()
            if (candidateTag.equals(normalizedTag, ignoreCase = true)) {
                return release
            }
            val htmlUrl = release.optString("html_url").trim()
            if (parseReleaseTagFromUrl(htmlUrl).equals(normalizedTag, ignoreCase = true)) {
                return release
            }
        }
        return null
    }

    private fun buildReleaseStub(
        releaseName: String,
        rawTag: String,
        releaseUrl: String,
        releaseUpdatedAtMillis: Long? = null,
        assets: List<GitHubReleaseAssetFile> = emptyList()
    ): JSONObject {
        return JSONObject()
            .put("name", releaseName)
            .put("tag_name", rawTag)
            .put("html_url", releaseUrl)
            .put("published_at", releaseUpdatedAtMillis?.let { Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL)
            .put(
                "assets",
                JSONArray().apply {
                    assets.forEach { asset ->
                        put(
                            JSONObject()
                                .put("name", asset.name)
                                .put("browser_download_url", asset.downloadUrl)
                                .put("url", asset.apiAssetUrl)
                                .put("size", asset.sizeBytes)
                                .put("download_count", asset.downloadCount)
                                .put("content_type", asset.contentType)
                                .put("updated_at", asset.updatedAtMillis?.let { Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL)
                        )
                    }
                }
            )
    }

    private fun parseReleaseBundle(release: JSONObject): GitHubReleaseAssetBundle {
        val releaseName = release.optString("name").trim()
        val tagName = release.optString("tag_name").trim().ifBlank { releaseName }
        val htmlUrl = release.optString("html_url").trim()
        val releaseUpdatedAtMillis = release.optString("published_at").parseIsoInstantOrNull()
            ?: release.optString("updated_at").parseIsoInstantOrNull()
            ?: release.optString("created_at").parseIsoInstantOrNull()
        val assetsArray = release.optJSONArray("assets") ?: JSONArray()
        val assets = buildList {
            for (index in 0 until assetsArray.length()) {
                val asset = assetsArray.optJSONObject(index) ?: continue
                val name = asset.optString("name").trim()
                val downloadUrl = asset.optString("browser_download_url").trim()
                if (name.isBlank() || downloadUrl.isBlank()) continue
                add(
                    GitHubReleaseAssetFile(
                        name = name,
                        downloadUrl = downloadUrl,
                        apiAssetUrl = asset.optString("url").trim(),
                        sizeBytes = asset.optLong("size", 0L),
                        downloadCount = when (val count = asset.opt("download_count")) {
                            is Number -> count.toInt()
                            is String -> count.toIntOrNull() ?: 0
                            else -> 0
                        },
                        contentType = asset.optString("content_type").trim(),
                        updatedAtMillis = asset.optString("updated_at").parseIsoInstantOrNull()
                            ?: asset.optString("created_at").parseIsoInstantOrNull()
                    )
                )
            }
        }
        return GitHubReleaseAssetBundle(
            releaseName = releaseName,
            tagName = tagName,
            htmlUrl = htmlUrl,
            releaseUpdatedAtMillis = releaseUpdatedAtMillis,
            assets = assets,
            shortCommitSha = ""
        )
    }

    private fun String.parseIsoInstantOrNull(): Long? {
        return runCatching { if (isBlank()) null else Instant.parse(this).toEpochMilli() }.getOrNull()
    }

    private fun String.stripHtml(): String {
        return replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.decodeHtmlEntities(): String {
        return replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun assetDisplayPriority(fileName: String): Int {
        val lower = fileName.lowercase(Locale.ROOT)
        return if (lower.endsWith(".apk")) apkAssetPriority(fileName) else 10
    }

    private fun apkAssetPriority(fileName: String): Int {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            "arm64-v8a" in lower || "aarch64" in lower || Regex("(^|[^a-z0-9])arm64([^a-z0-9]|$)").containsMatchIn(lower) -> 0
            "universal" in lower || "fat" in lower -> 1
            "armeabi-v7a" in lower || "armv7" in lower || Regex("(^|[^a-z0-9])armeabi([^a-z0-9]|$)").containsMatchIn(lower) -> 2
            "x86_64" in lower -> 3
            Regex("(^|[^a-z0-9])x86([^a-z0-9]|$)").containsMatchIn(lower) -> 4
            else -> 5
        }
    }
}
