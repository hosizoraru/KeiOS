package com.example.keios.ui.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.tencent.mmkv.MMKV
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URI
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GitHubTrackedApp(
    val repoUrl: String,
    val owner: String,
    val repo: String,
    val packageName: String,
    val appLabel: String,
    val checkPreRelease: Boolean = false
) {
    val id: String
        get() = "$owner/$repo|$packageName"
}

data class InstalledAppItem(
    val label: String,
    val packageName: String
)

data class GitHubReleaseVersionSignals(
    val displayVersion: String,
    val rawTag: String,
    val rawName: String,
    val candidates: List<String>
)

data class GitHubCheckCacheEntry(
    val loading: Boolean = false,
    val localVersion: String = "",
    val localVersionCode: Long = -1L,
    val latestTag: String = "",
    val latestStableRawTag: String = "",
    val latestPreRawTag: String = "",
    val hasUpdate: Boolean? = null,
    val message: String = "",
    val isPreRelease: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false,
    val hasPreReleaseUpdate: Boolean = false
)

data class GitHubAtomReleaseEntry(
    val tag: String,
    val title: String,
    val link: String,
    val candidates: List<String>,
    val isLikelyPreRelease: Boolean
)

object GitHubTrackStore {
    private const val KV_ID = "github_track_store"
    private const val KEY_ITEMS = "tracked_items"
    private const val KEY_CHECK_CACHE = "tracked_check_cache"
    private const val KEY_LAST_REFRESH_MS = "last_full_refresh_ms"
    private const val KEY_REFRESH_INTERVAL_HOURS = "refresh_interval_hours"

    @Volatile
    private var didAutoRefreshInSession: Boolean = false

    private fun kv(): MMKV = MMKV.mmkvWithID(KV_ID)

    fun load(): List<GitHubTrackedApp> {
        val raw = kv().decodeString(KEY_ITEMS).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val repoUrl = obj.optString("repoUrl").trim()
                    val owner = obj.optString("owner").trim()
                    val repo = obj.optString("repo").trim()
                    val packageName = obj.optString("packageName").trim()
                    val appLabel = obj.optString("appLabel").trim()
                    if (repoUrl.isNotBlank() && owner.isNotBlank() && repo.isNotBlank() && packageName.isNotBlank()) {
                        add(
                            GitHubTrackedApp(
                                repoUrl = repoUrl,
                                owner = owner,
                                repo = repo,
                                packageName = packageName,
                                appLabel = appLabel.ifBlank { packageName },
                                checkPreRelease = if (obj.has("checkPreRelease")) {
                                    obj.optBoolean("checkPreRelease", false)
                                } else {
                                    false
                                }
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(items: List<GitHubTrackedApp>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
                .put("repoUrl", item.repoUrl)
                .put("owner", item.owner)
                .put("repo", item.repo)
                .put("packageName", item.packageName)
                .put("appLabel", item.appLabel)
                .put("checkPreRelease", item.checkPreRelease)
            array.put(obj)
        }
        kv().encode(KEY_ITEMS, array.toString())
    }

    fun loadCheckCache(): Pair<Map<String, GitHubCheckCacheEntry>, Long> {
        val raw = kv().decodeString(KEY_CHECK_CACHE).orEmpty()
        val ts = kv().decodeLong(KEY_LAST_REFRESH_MS, 0L)
        if (raw.isBlank()) return emptyMap<String, GitHubCheckCacheEntry>() to ts
        val map = runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val item = obj.optJSONObject(id) ?: continue
                    put(
                        id,
                        GitHubCheckCacheEntry(
                            loading = false,
                            localVersion = item.optString("localVersion"),
                            localVersionCode = item.optLong("localVersionCode", -1L),
                            latestTag = item.optString("latestTag"),
                            latestStableRawTag = item.optString("latestStableRawTag"),
                            latestPreRawTag = item.optString("latestPreRawTag"),
                            hasUpdate = if (item.has("hasUpdate")) item.optBoolean("hasUpdate") else null,
                            message = item.optString("message"),
                            isPreRelease = item.optBoolean("isPreRelease", false),
                            preReleaseInfo = item.optString("preReleaseInfo"),
                            showPreReleaseInfo = item.optBoolean("showPreReleaseInfo", false),
                            hasPreReleaseUpdate = item.optBoolean("hasPreReleaseUpdate", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
        return map to ts
    }

    fun saveCheckCache(states: Map<String, GitHubCheckCacheEntry>, lastRefreshMs: Long) {
        val obj = JSONObject()
        states.forEach { (id, state) ->
            obj.put(
                id,
                    JSONObject()
                        .put("localVersion", state.localVersion)
                        .put("localVersionCode", state.localVersionCode)
                        .put("latestTag", state.latestTag)
                        .put("latestStableRawTag", state.latestStableRawTag)
                        .put("latestPreRawTag", state.latestPreRawTag)
                        .put("hasUpdate", state.hasUpdate)
                        .put("message", state.message)
                        .put("isPreRelease", state.isPreRelease)
                        .put("preReleaseInfo", state.preReleaseInfo)
                        .put("showPreReleaseInfo", state.showPreReleaseInfo)
                    .put("hasPreReleaseUpdate", state.hasPreReleaseUpdate)
            )
        }
        kv().encode(KEY_CHECK_CACHE, obj.toString())
        kv().encode(KEY_LAST_REFRESH_MS, lastRefreshMs)
    }

    fun clearCheckCache() {
        kv().removeValueForKey(KEY_CHECK_CACHE)
        kv().removeValueForKey(KEY_LAST_REFRESH_MS)
    }

    fun shouldAutoRefreshOnceInSession(): Boolean = !didAutoRefreshInSession

    fun markAutoRefreshDoneInSession() {
        didAutoRefreshInSession = true
    }

    fun loadRefreshIntervalHours(defaultValue: Int = 3): Int {
        val value = kv().decodeInt(KEY_REFRESH_INTERVAL_HOURS, defaultValue)
        return if (value in setOf(1, 3, 6, 12)) value else defaultValue
    }

    fun saveRefreshIntervalHours(hours: Int) {
        if (hours in setOf(1, 3, 6, 12)) {
            kv().encode(KEY_REFRESH_INTERVAL_HOURS, hours)
        }
    }
}

private data class CachedValue<T>(
    val value: T,
    val timestamp: Long
)

object GitHubVersionUtils {
    private const val CACHE_TTL_MS = 90_000L
    private const val GITHUB_USER_AGENT = "KeiOS-App/1.0 (Android)"
    private val stableCache = mutableMapOf<String, CachedValue<Result<GitHubReleaseVersionSignals>>>()
    private val atomCache = mutableMapOf<String, CachedValue<Result<List<GitHubAtomReleaseEntry>>>>()
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

    fun buildReleaseUrl(owner: String, repo: String): String {
        return "https://github.com/$owner/$repo/releases"
    }

    fun buildReleaseTagUrl(owner: String, repo: String, tag: String): String {
        val normalized = tag.trim()
        if (normalized.isBlank()) return buildReleaseUrl(owner, repo)
        val encodedTag = java.net.URLEncoder.encode(normalized, Charsets.UTF_8.name())
            .replace("+", "%20")
        return "https://github.com/$owner/$repo/releases/tag/$encodedTag"
    }

    fun buildAppListPermissionIntent(context: Context): Intent? {
        val pm = context.packageManager
        val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            putExtra("extra_pkgname", context.packageName)
        }
        if (miuiIntent.resolveActivity(pm) != null) return miuiIntent

        val detailIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")
        )
        if (detailIntent.resolveActivity(pm) != null) return detailIntent
        return null
    }

    fun parseOwnerRepo(input: String): Pair<String, String>? {
        val normalized = input.trim().let { raw ->
            if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        }
        return runCatching {
            val uri = URI(normalized)
            val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
            if (!host.contains("github.com")) return null
            val segments = uri.path.orEmpty()
                .split("/")
                .filter { it.isNotBlank() }
            if (segments.size < 2) return null
            val owner = segments[0]
            val repo = segments[1].removeSuffix(".git")
            if (owner.isBlank() || repo.isBlank()) null else owner to repo
        }.getOrNull()
    }

    fun fetchLatestReleaseSignals(
        owner: String,
        repo: String,
        atomEntriesHint: List<GitHubAtomReleaseEntry>? = null
    ): Result<GitHubReleaseVersionSignals> {
        val key = "$owner/$repo"
        val now = System.currentTimeMillis()
        stableCache[key]?.takeIf { now - it.timestamp <= CACHE_TTL_MS }?.let { return it.value }

        val result = runCatching {
            val latestUrl = "https://github.com/$owner/$repo/releases/latest"
            val latestTagFromRedirect = resolveLatestTagFromRedirect(latestUrl)

            val atomEntries = atomEntriesHint ?: fetchReleaseEntriesFromAtom(owner, repo, limit = 30)
                .getOrDefault(emptyList())
            val stableEntry = atomEntries.firstOrNull { !it.isLikelyPreRelease } ?: atomEntries.firstOrNull()

            val tag = latestTagFromRedirect.ifBlank { stableEntry?.tag.orEmpty() }
            val name = when {
                stableEntry != null && stableEntry.tag.equals(tag, ignoreCase = true) -> stableEntry.title
                else -> stableEntry?.title.orEmpty()
            }
            val candidates = collectVersionCandidates(tag, name)
            require(candidates.isNotEmpty()) { "release 版本信息为空" }

            val display = when {
                tag.isNotBlank() && name.isNotBlank() -> "$tag ($name)"
                tag.isNotBlank() -> tag
                name.isNotBlank() -> name
                else -> "unknown"
            }
            GitHubReleaseVersionSignals(
                displayVersion = display,
                rawTag = tag,
                rawName = name,
                candidates = candidates
            )
        }

        stableCache[key] = CachedValue(result, now)
        return result
    }

    fun fetchReleaseEntriesFromAtom(owner: String, repo: String, limit: Int = 20): Result<List<GitHubAtomReleaseEntry>> {
        val key = "$owner/$repo#$limit"
        val now = System.currentTimeMillis()
        atomCache[key]?.takeIf { now - it.timestamp <= CACHE_TTL_MS }?.let { return it.value }

        val atomUrl = "https://github.com/$owner/$repo/releases.atom"
        val result = runCatching {
            parseAtomEntriesStream(atomUrl, limit)
        }
        atomCache[key] = CachedValue(result, now)
        return result
    }

    fun compareVersionToCandidates(local: String, candidates: List<String>): Int? {
        val results = candidates.mapNotNull { compareVersion(local, it) }
        if (results.isEmpty()) return null
        if (results.any { it == 0 }) return 0
        val hasLower = results.any { it < 0 }
        val hasHigher = results.any { it > 0 }
        return when {
            hasLower && !hasHigher -> -1
            hasHigher && !hasLower -> 1
            else -> null
        }
    }

    fun compareVersion(local: String, remote: String): Int? {
        val localCore = extractNumericCore(local)
        val remoteCore = extractNumericCore(remote)
        if (localCore != null && remoteCore != null) {
            val coreCmp = compareNumericCore(localCore, remoteCore)
            if (coreCmp != 0) return coreCmp
            return compareSuffixWhenCoreEqual(local, remote)
        }

        val a = tokenizeVersion(local)
        val b = tokenizeVersion(remote)
        if (a.isEmpty() || b.isEmpty()) return null
        if (a.none { it.all(Char::isDigit) } || b.none { it.all(Char::isDigit) }) return null
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val left = a.getOrNull(i)
            val right = b.getOrNull(i)
            if (left == right) continue
            if (left == null) return -1
            if (right == null) return 1
            val li = left.toLongOrNull()
            val ri = right.toLongOrNull()
            val cmp = when {
                li != null && ri != null -> li.compareTo(ri)
                else -> left.compareTo(right)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    private fun compareSuffixWhenCoreEqual(local: String, remote: String): Int {
        val localPre = isLikelyPreReleaseText(local)
        val remotePre = isLikelyPreReleaseText(remote)
        val localBuild = extractBuildNumber(local)
        val remoteBuild = extractBuildNumber(remote)

        // For pre-release tracks, build numbers are usually the strongest signal.
        if ((localPre || remotePre) && localBuild != null && remoteBuild != null && localBuild != remoteBuild) {
            return localBuild.compareTo(remoteBuild)
        }

        // Stable vs pre-release on same core: stable is considered newer/final.
        if (localPre != remotePre) {
            return if (localPre) -1 else 1
        }

        val localQual = qualifierRank(local)
        val remoteQual = qualifierRank(remote)
        if (localQual != remoteQual) return localQual.compareTo(remoteQual)

        // No pre-release markers: keep compatibility with previous behavior and ignore suffix.
        if (!localPre && !remotePre) return 0

        // As a fallback for pre-release strings, compare any explicit build numbers.
        if (localBuild != null && remoteBuild != null && localBuild != remoteBuild) {
            return localBuild.compareTo(remoteBuild)
        }

        return 0
    }

    fun queryInstalledLaunchableApps(context: Context): List<InstalledAppItem> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        return packages
            .asSequence()
            .filter { info -> pm.getLaunchIntentForPackage(info.packageName) != null }
            .map { info ->
                val pkg = info.packageName
                val appInfo = info.applicationInfo
                val label = runCatching {
                    if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else pkg
                }.getOrDefault(pkg)
                InstalledAppItem(label = label.ifBlank { pkg }, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    fun localVersionName(context: Context, packageName: String): String {
        val info = context.packageManager.getPackageInfoCompat(packageName)
        return info.versionName ?: "unknown"
    }

    fun localVersionCode(context: Context, packageName: String): Long {
        val info = context.packageManager.getPackageInfoCompat(packageName)
        return info.longVersionCode
    }

    private fun resolveLatestTagFromRedirect(latestUrl: String): String {
        return runCatching {
            val request = Request.Builder()
                .url(latestUrl)
                .get()
                .header("Accept", "text/html,*/*")
                .header("User-Agent", GITHUB_USER_AGENT)
                .build()
            githubNoRedirectClient.newCall(request).execute().use { response ->
                val code = response.code
                val location = response.header("Location").orEmpty()
                if (code !in 300..399 || location.isBlank()) return@runCatching ""

                val rawTag = Regex("/releases/tag/([^?#]+)")
                    .find(location)
                    ?.groupValues
                    ?.getOrNull(1)
                    .orEmpty()
                URLDecoder.decode(rawTag, Charsets.UTF_8.name()).trim()
            }
        }.getOrDefault("")
    }

    private fun tokenizeVersion(raw: String): List<String> {
        return raw.lowercase(Locale.ROOT)
            .removePrefix("v")
            .split(Regex("[^0-9a-zA-Z]+"))
            .filter { it.isNotBlank() }
    }

    private fun collectVersionCandidates(tag: String, name: String): List<String> {
        val set = linkedSetOf<String>()
        if (tag.isNotBlank()) set += tag
        if (name.isNotBlank()) set += name
        set += extractPossibleVersionStrings(tag)
        set += extractPossibleVersionStrings(name)
        return set
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun collectVersionCandidates(tag: String, name: String, extraText: String): List<String> {
        val set = linkedSetOf<String>()
        set += collectVersionCandidates(tag, name)
        set += extractPossibleVersionStrings(extraText)
        return set
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun extractPossibleVersionStrings(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val regex = Regex("v?\\d+(?:\\.\\d+)+(?:[-+._][0-9A-Za-z]+)*")
        return regex.findAll(raw)
            .map { it.value.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun parseAtomEntriesStream(url: String, limit: Int): List<GitHubAtomReleaseEntry> {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/atom+xml")
            .header("User-Agent", GITHUB_USER_AGENT)
            .build()
        githubClient.newCall(request).execute().use { response ->
            val code = response.code
            if (code !in 200..299) return emptyList()
            val body = response.body.string()
            if (body.isBlank()) return emptyList()

            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(body.reader())

            val result = mutableListOf<GitHubAtomReleaseEntry>()
            var inEntry = false
            var currentTag: String? = null
            var currentTitle = StringBuilder()
            var currentId = StringBuilder()
            var currentLink = ""
            var currentContentSample = StringBuilder()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "entry") {
                            inEntry = true
                            currentTag = null
                            currentTitle = StringBuilder()
                            currentId = StringBuilder()
                            currentLink = ""
                            currentContentSample = StringBuilder()
                        } else if (inEntry && name == "title") {
                            currentTag = "title"
                        } else if (inEntry && name == "id") {
                            currentTag = "id"
                        } else if (inEntry && name == "content") {
                            currentTag = "content"
                        } else if (inEntry && name == "link") {
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            if ("/releases/tag/" in href) currentLink = href
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (!inEntry) {
                            // ignore
                        } else {
                            when (currentTag) {
                                "title" -> currentTitle.append(parser.text.orEmpty())
                                "id" -> currentId.append(parser.text.orEmpty())
                                "content" -> {
                                    if (currentContentSample.length < 2400) {
                                        currentContentSample.append(parser.text.orEmpty().take(2400 - currentContentSample.length))
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (name == "title" || name == "id" || name == "content") {
                            currentTag = null
                        } else if (name == "entry") {
                            inEntry = false
                            val title = currentTitle.toString().trim()
                            val tagFromLink = Regex("/releases/tag/([^\"/<>]+)")
                                .find(currentLink)
                                ?.groupValues
                                ?.getOrNull(1)
                                .orEmpty()
                            val tagFromId = currentId.toString()
                                .substringAfterLast("/", "")
                                .trim()
                            val tag = tagFromLink.ifBlank { tagFromId }
                            if (tag.isNotBlank() || title.isNotBlank()) {
                                val entry = GitHubAtomReleaseEntry(
                                    tag = tag,
                                    title = title,
                                    link = currentLink,
                                    candidates = collectVersionCandidates(tag, title, currentContentSample.toString()),
                                    isLikelyPreRelease = isLikelyPreReleaseText("$tag $title")
                                )
                                result += entry
                                if (result.size >= limit) break
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            return result
        }
    }

    private fun isLikelyPreReleaseText(raw: String): Boolean {
        val s = raw.lowercase(Locale.ROOT)
        val markers = listOf(
            "pre-release",
            "prerelease",
            "alpha",
            "beta",
            "rc",
            "preview",
            "nightly",
            "canary",
            "dev",
            "snapshot"
        )
        if (markers.any { it in s }) return true
        // Typical semantic pre-release marker: 1.2.3-alpha / 1.2.3-rc1
        // Ignore pure numeric suffixes such as 2.0.3-634166068 (build metadata style).
        return Regex("v?\\d+(?:\\.\\d+)+-[a-z][0-9a-z]*").containsMatchIn(s)
    }

    private fun qualifierRank(raw: String): Int {
        val s = raw.lowercase(Locale.ROOT)
        return when {
            "canary" in s -> -4
            "nightly" in s || "snapshot" in s || Regex("\\bdev\\b").containsMatchIn(s) -> -3
            "alpha" in s -> -2
            "beta" in s || "pre-release" in s || "prerelease" in s || "preview" in s -> -1
            Regex("\\brc\\d*\\b").containsMatchIn(s) -> 0
            else -> 1
        }
    }

    private fun extractBuildNumber(raw: String): Long? {
        val s = raw.lowercase(Locale.ROOT)
        val patterns = listOf(
            Regex("_c(\\d{2,})\\b"),
            Regex("\\bc(\\d{2,})\\b"),
            Regex("\\bbuild[._-]?(\\d{2,})\\b"),
            Regex("\\br(\\d{6,})\\b")
        )
        for (pattern in patterns) {
            val match = pattern.find(s)?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (match != null) return match
        }
        return null
    }

    private fun extractNumericCore(raw: String): List<Long>? {
        val input = raw.lowercase(Locale.ROOT).removePrefix("v")
        val match = Regex("\\d+(?:\\.\\d+)+").find(input) ?: return null
        val numbers = match.value.split(".").mapNotNull { it.toLongOrNull() }
        if (numbers.isEmpty()) return null
        return trimTrailingZeros(numbers)
    }

    private fun compareNumericCore(a: List<Long>, b: List<Long>): Int {
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val ai = a.getOrElse(i) { 0L }
            val bi = b.getOrElse(i) { 0L }
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }

    private fun trimTrailingZeros(value: List<Long>): List<Long> {
        var end = value.size
        while (end > 1 && value[end - 1] == 0L) end--
        return value.subList(0, end)
    }
}

private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }
}
