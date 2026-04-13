package com.example.keios.feature.github.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GitHubReleaseAssetFile(
    val name: String,
    val downloadUrl: String,
    val apiAssetUrl: String = "",
    val sizeBytes: Long,
    val downloadCount: Int,
    val contentType: String = ""
)

data class GitHubReleaseAssetBundle(
    val releaseName: String,
    val tagName: String,
    val htmlUrl: String,
    val assets: List<GitHubReleaseAssetFile>
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
        apiToken: String = ""
    ): Result<GitHubReleaseAssetBundle> {
        val normalizedTag = rawTag.trim()
        if (normalizedTag.isBlank()) {
            return Result.failure(IllegalArgumentException("missing release tag"))
        }

        val normalizedReleaseUrl = releaseUrl.trim()
        val primary = if (preferHtml && normalizedReleaseUrl.isNotBlank()) {
            fetchReleaseFromHtml(owner, repo, normalizedTag, normalizedReleaseUrl, apiToken)
        } else {
            fetchReleaseByTagWithFallback(owner, repo, normalizedTag, normalizedReleaseUrl, apiToken)
        }
        val fallback = if (preferHtml) {
            fetchReleaseByTagWithFallback(owner, repo, normalizedTag, normalizedReleaseUrl, apiToken)
        } else if (normalizedReleaseUrl.isNotBlank()) {
            fetchReleaseFromHtml(owner, repo, normalizedTag, normalizedReleaseUrl, apiToken)
        } else {
            Result.failure(primary.exceptionOrNull() ?: IllegalStateException("release fetch failed"))
        }

        return (primary.takeIf { it.isSuccess } ?: fallback).mapCatching { release ->
            val bundle = parseReleaseBundle(release)
            bundle.copy(assets = bundle.assets.filterRelevantApks(aggressiveFiltering))
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
        return asSequence()
            .filter { asset ->
                val lowerName = asset.name.lowercase(Locale.ROOT)
                lowerName.endsWith(".apk") &&
                    !lowerName.contains("source code") &&
                    !lowerName.contains("metadata") &&
                    (!aggressiveFiltering || !isAggressivelyIgnoredApk(lowerName))
            }
            .sortedWith(compareBy<GitHubReleaseAssetFile> { apkAssetPriority(it.name) }.thenBy { it.name.lowercase(Locale.ROOT) })
            .toList()
    }

    private fun isAggressivelyIgnoredApk(lowerName: String): Boolean {
        return "armeabi-v7a" in lowerName || "x86_64" in lowerName
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
                ?: buildReleaseStub(rawTag, releaseUrl)
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
                    error("GitHub API 资产下载未返回跳转地址")
                }
                else -> error(buildApiAssetErrorMessage(response))
            }
        }
    }

    private fun buildApiAssetErrorMessage(response: Response): String {
        return when (response.code) {
            401 -> "GitHub API token 无效或已过期"
            403, 429 -> "GitHub API 资产下载已限流"
            404 -> "GitHub API 资产地址已失效"
            else -> "GitHub API 资产下载失败 (HTTP ${response.code})"
        }
    }

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
                                401 -> "GitHub API token 无效或已过期"
                                403, 429 -> if (token.isBlank()) {
                                    "GitHub 游客 API 已限流，请稍后重试或填写 token"
                                } else {
                                    "GitHub API 已限流"
                                }
                                404 -> "未找到该 tag 对应的 release"
                                else -> "GitHub release 请求失败 (HTTP ${response.code}${apiMessage.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""})"
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
            error("GitHub 连接被中途关闭，请稍后重试")
        }
        throw lastError ?: IllegalStateException("GitHub release 请求失败")
    }

    private fun fetchReleaseFromHtml(
        owner: String,
        repo: String,
        rawTag: String,
        releaseUrl: String,
        apiToken: String
    ): Result<JSONObject> = runCatching {
        val html = fetchHtml(releaseUrl, apiToken)
        val assets = parseAssetsFromReleaseHtml(html, owner, repo, rawTag)
        buildReleaseStub(rawTag, releaseUrl, assets)
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
                        error("GitHub release 页面请求失败 (HTTP ${response.code})")
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
            error("GitHub 页面连接被中途关闭，请稍后重试")
        }
        throw lastError ?: IllegalStateException("GitHub release 页面请求失败")
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
            if (!normalizedUrl.lowercase(Locale.ROOT).endsWith(".apk")) return@forEach
            val fileName = normalizedUrl.substringAfterLast('/').substringBefore('?')
            val decodedName = runCatching { URLDecoder.decode(fileName, Charsets.UTF_8.name()) }.getOrDefault(fileName)
            unique.putIfAbsent(
                decodedName,
                GitHubReleaseAssetFile(
                    name = decodedName,
                    downloadUrl = normalizedUrl,
                    sizeBytes = 0L,
                    downloadCount = 0,
                    contentType = "application/vnd.android.package-archive"
                )
            )
        }
        return unique.values.toList().ifEmpty {
            error("未在 release 页面中找到 APK 资产: $rawTag")
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
        rawTag: String,
        releaseUrl: String,
        assets: List<GitHubReleaseAssetFile> = emptyList()
    ): JSONObject {
        return JSONObject()
            .put("name", rawTag)
            .put("tag_name", rawTag)
            .put("html_url", releaseUrl)
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
                        )
                    }
                }
            )
    }

    private fun parseReleaseBundle(release: JSONObject): GitHubReleaseAssetBundle {
        val releaseName = release.optString("name").trim()
        val tagName = release.optString("tag_name").trim().ifBlank { releaseName }
        val htmlUrl = release.optString("html_url").trim()
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
                        contentType = asset.optString("content_type").trim()
                    )
                )
            }
        }
        return GitHubReleaseAssetBundle(
            releaseName = releaseName,
            tagName = tagName,
            htmlUrl = htmlUrl,
            assets = assets
        )
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
