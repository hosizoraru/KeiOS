package com.example.keios.feature.github.data.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.keios.feature.github.model.GitHubAtomReleaseEntry
import com.example.keios.feature.github.model.GitHubReleaseVersionSignals
import com.example.keios.feature.github.model.InstalledAppItem
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URI
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    fun queryInstalledLaunchableApps(context: Context): List<InstalledAppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val activities = pm.queryIntentActivities(
            mainIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        )
        val packages = activities.map { it.activityInfo.packageName }.toSet()

        return packages.mapNotNull { pkg ->
            runCatching {
                val appInfo = pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                val label = pm.getApplicationLabel(appInfo).toString()
                InstalledAppItem(label = label, packageName = pkg)
            }.getOrNull()
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun localVersionName(context: Context, packageName: String): String {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        return pkgInfo.versionName?.trim().orEmpty().ifBlank { "unknown" }
    }

    fun localVersionCode(context: Context, packageName: String): Long {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
    }

    fun parseOwnerRepo(urlOrPath: String): Pair<String, String>? {
        val raw = urlOrPath.trim()
            .removePrefix("git+")
            .removeSuffix(".git")
            .trimEnd('/')
        if (raw.isBlank()) return null

        if (raw.contains(":") && raw.contains("@") && raw.contains("github.com")) {
            val afterColon = raw.substringAfter(':', "")
            val ownerRepo = afterColon.removePrefix("/").split("/")
            if (ownerRepo.size >= 2) return ownerRepo[0] to ownerRepo[1]
        }

        val asUri = runCatching { URI(raw) }.getOrNull()
        if (asUri != null && asUri.host?.contains("github.com", ignoreCase = true) == true) {
            val segments = asUri.path.trim('/').split('/').filter { it.isNotBlank() }
            if (segments.size >= 2) return segments[0] to segments[1].removeSuffix(".git")
        }

        val normalized = raw
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("github.com/")
            .trim('/')
        val parts = normalized.split('/').filter { it.isNotBlank() }
        if (parts.size >= 2) return parts[0] to parts[1].removeSuffix(".git")
        return null
    }

    private fun normalizeVersionCandidates(text: String): List<String> {
        val base = text.trim().ifBlank { return emptyList() }
        val tokens = linkedSetOf<String>()
        fun addCandidate(v: String) {
            val trimmed = v.trim().trim('"', '\'', '(', ')', '[', ']', '{', '}')
            if (trimmed.isBlank()) return
            tokens += trimmed
            tokens += trimmed.removePrefix("v")
            tokens += trimmed.removePrefix("V")
            tokens += trimmed.replace('_', '.')
        }

        addCandidate(base)
        Regex("""[vV]?\d+(?:[._-]\d+)*(?:[a-zA-Z]+\d*)?(?:[-.]?(?:alpha|beta|rc|preview|canary|dev)\d*)?""")
            .findAll(base)
            .forEach { addCandidate(it.value) }

        return tokens
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun compareVersionToCandidates(localVersion: String, candidates: List<String>): Int? {
        val localTokens = normalizeVersionCandidates(localVersion)
        if (localTokens.isEmpty() || candidates.isEmpty()) return null

        var best: Int? = null
        for (local in localTokens) {
            for (remote in candidates) {
                val cmp = compareSemverLike(local, remote)
                if (cmp != null) {
                    if (best == null || kotlin.math.abs(cmp) < kotlin.math.abs(best)) {
                        best = cmp
                    }
                    if (cmp == 0) return 0
                }
            }
        }
        return best
    }

    private fun compareSemverLike(a: String, b: String): Int? {
        val pa = parseVersionParts(a) ?: return null
        val pb = parseVersionParts(b) ?: return null

        val max = maxOf(pa.numbers.size, pb.numbers.size)
        for (i in 0 until max) {
            val va = pa.numbers.getOrElse(i) { 0 }
            val vb = pb.numbers.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }

        if (pa.channel != pb.channel) {
            return pa.channel.rank.compareTo(pb.channel.rank)
        }

        if (pa.channelNumber != pb.channelNumber) {
            return pa.channelNumber.compareTo(pb.channelNumber)
        }

        return 0
    }

    private enum class Channel(val rank: Int) {
        DEV(0),
        ALPHA(1),
        BETA(2),
        RC(3),
        PREVIEW(4),
        STABLE(5)
    }

    private data class VersionParts(
        val numbers: List<Int>,
        val channel: Channel,
        val channelNumber: Int
    )

    private fun parseVersionParts(raw: String): VersionParts? {
        val src = raw.trim().lowercase(Locale.ROOT)
        if (src.isBlank()) return null

        val normalized = src.removePrefix("v")
        val coreMatch = Regex("""\d+(?:[._]\d+)*""").find(normalized) ?: return null
        val coreNumbers = coreMatch.value
            .split('.', '_')
            .mapNotNull { it.toIntOrNull() }
        if (coreNumbers.isEmpty()) return null

        val suffix = normalized.substring(coreMatch.range.last + 1)
        val channelMatch = Regex(
            """(?:^|[^a-z])(dev|canary|alpha|beta|rc|preview|pre(?:-release)?)(?:[^a-z0-9]*(\d+))?"""
        ).find(suffix.ifBlank { normalized })

        val channel = when (channelMatch?.groupValues?.getOrNull(1).orEmpty()) {
            "dev", "canary" -> Channel.DEV
            "alpha" -> Channel.ALPHA
            "beta" -> Channel.BETA
            "rc" -> Channel.RC
            "preview", "pre", "pre-release" -> Channel.PREVIEW
            else -> Channel.STABLE
        }

        val channelNumber = channelMatch
            ?.groupValues
            ?.getOrNull(2)
            ?.toIntOrNull()
            ?: 0

        return VersionParts(coreNumbers, channel, channelNumber)
    }

    private fun String.decodeXmlEscapes(): String {
        return this
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    private fun parseAtomEntries(xml: String): List<GitHubAtomReleaseEntry> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(xml.reader())
        }

        val result = mutableListOf<GitHubAtomReleaseEntry>()
        var event = parser.eventType

        var inEntry = false
        var title = ""
        var link = ""
        var id = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "entry" -> {
                            inEntry = true
                            title = ""
                            link = ""
                            id = ""
                        }
                        "title" -> if (inEntry) {
                            title = parser.nextText().decodeXmlEscapes().trim()
                        }
                        "id" -> if (inEntry) {
                            id = parser.nextText().decodeXmlEscapes().trim()
                        }
                        "link" -> if (inEntry) {
                            val rel = parser.getAttributeValue(null, "rel").orEmpty()
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            if (rel == "alternate" && href.isNotBlank()) {
                                link = href
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry" && inEntry) {
                        val effectiveLink = link.ifBlank { id }
                        val tag = when {
                            effectiveLink.contains("/releases/tag/") -> {
                                effectiveLink.substringAfter("/releases/tag/")
                            }
                            else -> title
                        }.trim()

                        if (tag.isNotBlank()) {
                            val candidates = normalizeVersionCandidates(listOf(title, tag).joinToString(" "))
                            val lower = listOf(title, tag).joinToString(" ").lowercase(Locale.ROOT)
                            val likelyPre = lower.contains("beta") || lower.contains("alpha") ||
                                lower.contains("rc") || lower.contains("preview") || lower.contains("pre-release")
                            result += GitHubAtomReleaseEntry(
                                tag = tag,
                                title = title.ifBlank { tag },
                                link = effectiveLink,
                                candidates = candidates,
                                isLikelyPreRelease = likelyPre
                            )
                        }
                        inEntry = false
                    }
                }
            }
            event = parser.next()
        }

        return result
    }

    private fun fetch(url: String): Result<String> = runCatching {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", GITHUB_USER_AGENT)
            .build()
        githubClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
    }

    fun fetchReleaseEntriesFromAtom(
        owner: String,
        repo: String,
        limit: Int = 30
    ): Result<List<GitHubAtomReleaseEntry>> {
        val key = "$owner/$repo"
        val now = System.currentTimeMillis()
        atomCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let { return it.value }

        val url = "https://github.com/$owner/$repo/releases.atom"
        val result = fetch(url).map { body ->
            parseAtomEntries(body).take(limit)
        }
        atomCache[key] = CachedValue(result, now)
        return result
    }

    fun fetchLatestReleaseSignals(
        owner: String,
        repo: String,
        atomEntriesHint: List<GitHubAtomReleaseEntry>? = null
    ): Result<GitHubReleaseVersionSignals> {
        val key = "$owner/$repo"
        val now = System.currentTimeMillis()
        stableCache[key]?.takeIf { now - it.timestamp < CACHE_TTL_MS }?.let { return it.value }

        val releaseLatestUrl = "https://github.com/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(releaseLatestUrl)
            .get()
            .header("User-Agent", GITHUB_USER_AGENT)
            .build()

        val result = runCatching {
            githubNoRedirectClient.newCall(request).execute().use { response ->
                val location = response.header("Location").orEmpty()
                val finalUrl = when {
                    location.isNotBlank() -> location
                    response.request.url.toString().contains("/releases/tag/") -> response.request.url.toString()
                    else -> ""
                }

                if (finalUrl.contains("/releases/tag/")) {
                    val rawTag = finalUrl.substringAfterLast("/releases/tag/").trim('/')
                    val decodedTag = URLDecoder.decode(rawTag, Charsets.UTF_8.name())
                    val normalized = normalizeVersionCandidates(decodedTag)
                    GitHubReleaseVersionSignals(
                        displayVersion = decodedTag,
                        rawTag = decodedTag,
                        rawName = decodedTag,
                        candidates = normalized.ifEmpty { listOf(decodedTag.lowercase(Locale.ROOT)) }
                    )
                } else {
                    val entries = atomEntriesHint ?: fetchReleaseEntriesFromAtom(owner, repo).getOrDefault(emptyList())
                    val stable = entries.firstOrNull { !it.isLikelyPreRelease }
                        ?: entries.firstOrNull()
                        ?: error("no release entries")
                    GitHubReleaseVersionSignals(
                        displayVersion = stable.title.ifBlank { stable.tag },
                        rawTag = stable.tag,
                        rawName = stable.title,
                        candidates = stable.candidates.ifEmpty {
                            normalizeVersionCandidates(stable.title.ifBlank { stable.tag })
                        }
                    )
                }
            }
        }

        stableCache[key] = CachedValue(result, now)
        return result
    }

    fun clearCaches() {
        stableCache.clear()
        atomCache.clear()
    }
}
