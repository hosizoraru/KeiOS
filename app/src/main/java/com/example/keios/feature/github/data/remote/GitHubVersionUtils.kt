package com.example.keios.feature.github.data.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.example.keios.feature.github.model.GitHubReleaseChannel
import com.example.keios.feature.github.model.GitHubVersionCandidate
import com.example.keios.feature.github.model.GitHubVersionCandidateSource
import com.example.keios.feature.github.model.InstalledAppItem
import java.net.URI
import java.util.Locale
import kotlin.math.abs

object GitHubVersionUtils {
    private const val INSTALLED_APPS_CACHE_TTL_MS = 5L * 60L * 1000L

    @Volatile
    private var installedAppsCache: CachedInstalledApps? = null

    private data class CachedInstalledApps(
        val updatedAtMs: Long,
        val apps: List<InstalledAppItem>
    )

    data class LocalVersionInfo(
        val versionName: String,
        val versionCode: Long
    )

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

    fun queryInstalledLaunchableApps(
        context: Context,
        forceRefresh: Boolean = false,
        ttlMs: Long = INSTALLED_APPS_CACHE_TTL_MS
    ): List<InstalledAppItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            installedAppsCache?.takeIf { cache ->
                (now - cache.updatedAtMs).coerceAtLeast(0L) < ttlMs.coerceAtLeast(0L)
            }?.let { return it.apps }
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val activities = pm.queryIntentActivities(
            mainIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        )
        val packages = activities.map { it.activityInfo.packageName }.toSet()

        val apps = packages.mapNotNull { pkg ->
            runCatching {
                val appInfo = pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                val label = pm.getApplicationLabel(appInfo).toString()
                val pkgInfo = pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                InstalledAppItem(
                    label = label,
                    packageName = pkg,
                    lastUpdateTimeMs = pkgInfo.lastUpdateTime
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
        installedAppsCache = CachedInstalledApps(
            updatedAtMs = now,
            apps = apps
        )
        return apps
    }

    fun invalidateInstalledLaunchableAppsCache() {
        installedAppsCache = null
    }

    fun localVersionName(context: Context, packageName: String): String {
        return localVersionInfoOrNull(context, packageName)?.versionName.orEmpty().ifBlank { "unknown" }
    }

    fun localVersionCode(context: Context, packageName: String): Long {
        return localVersionInfoOrNull(context, packageName)?.versionCode ?: -1L
    }

    fun localVersionInfoOrNull(context: Context, packageName: String): LocalVersionInfo? {
        val pm = context.packageManager
        val pkgInfo = runCatching {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }.recoverCatching {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }.getOrElse { error ->
            if (error is PackageManager.NameNotFoundException) {
                return null
            }
            throw error
        }
        val versionName = pkgInfo.versionName?.trim().orEmpty().ifBlank { "unknown" }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
        return LocalVersionInfo(
            versionName = versionName,
            versionCode = versionCode
        )
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

    fun buildVersionCandidates(vararg inputs: Pair<GitHubVersionCandidateSource, String>): List<GitHubVersionCandidate> {
        val dedup = linkedMapOf<String, GitHubVersionCandidate>()
        inputs.forEach { (source, text) ->
            normalizeVersionCandidates(text).forEach { candidate ->
                val existing = dedup[candidate]
                if (existing == null || source.priority < existing.source.priority) {
                    dedup[candidate] = GitHubVersionCandidate(candidate, source)
                }
            }
        }
        return dedup.values.toList()
    }

    fun normalizeVersionCandidates(text: String): List<String> {
        val base = text.trim()
        if (base.isBlank()) return emptyList()

        val tokens = linkedSetOf<String>()

        fun push(candidate: String) {
            val normalized = candidate.trim().lowercase(Locale.ROOT)
            if (normalized.isNotBlank()) tokens += normalized
        }

        fun addCandidate(value: String) {
            val trimmed = value.trim()
                .trim('"', '\'', '(', ')', '[', ']', '{', '}', ',', ';', ':')
            if (trimmed.isBlank()) return

            val canonical = canonicalizeCandidate(trimmed)
            if (canonical.isBlank()) return

            push(canonical)
            push(canonical.removePrefix("v"))
            push(canonical.removePrefix("V"))

            val withoutBuild = canonical.substringBefore('+')
            if (withoutBuild != canonical) {
                push(withoutBuild)
                push(withoutBuild.removePrefix("v"))
            }
        }

        addCandidate(base)

        Regex("""^(?:20\d{4}|\d{6,8})[._-]+([vV]?\d+(?:[._-]\d+)+.*)$""")
            .matchEntire(base)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::addCandidate)

        val versionRegex = Regex(
            """[vV]?\d+(?:[._-]\d+)*(?:\s*[-._ ]?\s*(?:dev|nightly|canary|snapshot|alpha|beta|rc|preview|pre(?:-release)?)(?:\s*[-._ ]?\s*\d+)?)?(?:\+[0-9A-Za-z.-]+)?"""
        )
        versionRegex.findAll(base).forEach { addCandidate(it.value) }

        return filterLessSpecificCandidates(tokens.toList())
    }

    fun compareVersionToCandidates(localVersion: String, candidates: List<String>): Int? {
        return compareCandidateSets(normalizeVersionCandidates(localVersion), candidates)
    }

    fun compareVersionToStructuredCandidates(localVersion: String, candidates: List<GitHubVersionCandidate>): Int? {
        return compareCandidateSetsWithSources(normalizeVersionCandidates(localVersion), candidates)
    }

    fun compareStructuredCandidateSets(
        leftCandidates: List<GitHubVersionCandidate>,
        rightCandidates: List<GitHubVersionCandidate>
    ): Int? {
        val left = leftCandidates.map { it.value }
        return compareCandidateSetsWithSources(left, rightCandidates)
    }

    internal fun referToSameReleaseVersion(
        leftCandidates: List<GitHubVersionCandidate>,
        rightCandidates: List<GitHubVersionCandidate>,
        maxSourcePriority: Int = GitHubVersionCandidateSource.Link.priority
    ): Boolean {
        val left = leftCandidates
            .filter { it.source.priority <= maxSourcePriority }
            .flatMap { normalizeVersionCandidates(it.value) }
            .mapNotNull(::parseVersionParts)
            .filter(::isMeaningfulReleaseIdentity)
            .map(::releaseIdentityKey)
            .toSet()
        val right = rightCandidates
            .filter { it.source.priority <= maxSourcePriority }
            .flatMap { normalizeVersionCandidates(it.value) }
            .mapNotNull(::parseVersionParts)
            .filter(::isMeaningfulReleaseIdentity)
            .map(::releaseIdentityKey)
            .toSet()
        if (left.isEmpty() || right.isEmpty()) return false
        return left.any(right::contains)
    }

    internal fun hasComparableVersionCandidates(
        candidates: List<GitHubVersionCandidate>,
        maxSourcePriority: Int = GitHubVersionCandidateSource.Link.priority
    ): Boolean {
        return candidates.any { candidate ->
            candidate.source.priority <= maxSourcePriority &&
                normalizeVersionCandidates(candidate.value).any { normalized ->
                    parseVersionParts(normalized) != null
                }
        }
    }

    internal fun hasMeaningfulPreReleaseVersionCandidates(
        candidates: List<GitHubVersionCandidate>,
        maxSourcePriority: Int = GitHubVersionCandidateSource.Link.priority
    ): Boolean {
        return candidates.any { candidate ->
            candidate.source.priority <= maxSourcePriority &&
                normalizeVersionCandidates(candidate.value).mapNotNull { normalized ->
                    parseVersionParts(normalized)
                }.any { parts ->
                    parts.numbers.size >= 2 ||
                        (parts.channel.isPreRelease && parts.channelNumber > 0)
                }
        }
    }

    internal fun isRelevantPreRelease(
        preReleaseCandidates: List<GitHubVersionCandidate>,
        stableCandidates: List<GitHubVersionCandidate>,
        preReleaseUpdatedAtMillis: Long? = null,
        stableUpdatedAtMillis: Long? = null
    ): Boolean {
        val compare = compareStructuredCandidateSets(preReleaseCandidates, stableCandidates)
        return when {
            preReleaseUpdatedAtMillis != null && stableUpdatedAtMillis != null &&
                preReleaseUpdatedAtMillis > stableUpdatedAtMillis -> true
            compare != null -> compare > 0
            else -> (preReleaseUpdatedAtMillis ?: Long.MIN_VALUE) > (stableUpdatedAtMillis ?: Long.MIN_VALUE)
        }
    }

    fun classifyVersionChannel(text: String): GitHubReleaseChannel? {
        val normalized = normalizeVersionCandidates(text)
        val parsed = normalized
            .mapNotNull(::parseVersionParts)
            .maxByOrNull(::versionPartsSpecificityScore)
            ?.channel
        return parsed
    }

    fun compareCandidateSets(
        leftCandidates: List<String>,
        rightCandidates: List<String>
    ): Int? {
        val comparableRight = rightCandidates.map { GitHubVersionCandidate(it, GitHubVersionCandidateSource.Content) }
        return compareCandidateSetsWithSources(leftCandidates, comparableRight)
    }

    fun compareCandidateSetsWithSources(
        leftCandidates: List<String>,
        rightCandidates: List<GitHubVersionCandidate>
    ): Int? {
        val left = leftCandidates
            .flatMap(::normalizeVersionCandidates)
            .distinct()
            .mapNotNull { parseComparableCandidate(it, sourcePriority = 0) }
        val right = rightCandidates
            .flatMap { candidate ->
                normalizeVersionCandidates(candidate.value).mapNotNull { parsed ->
                    parseComparableCandidate(parsed, candidate.source.priority)
                }
            }
            .distinctBy { it.normalized to it.sourcePriority }

        if (left.isEmpty() || right.isEmpty()) return null

        var bestCmp: Int? = null
        var bestScore = Int.MIN_VALUE
        for (local in left) {
            for (remote in right) {
                val cmp = compareParsedVersionParts(local.parts, remote.parts)
                val score = similarityScore(local, remote)
                if (cmp == 0 && score >= bestScore) {
                    bestCmp = 0
                    bestScore = score
                    continue
                }
                if (score > bestScore) {
                    bestScore = score
                    bestCmp = cmp
                }
            }
        }
        return bestCmp
    }

    private fun canonicalizeCandidate(raw: String): String {
        return raw
            .replace(Regex("""(?i)pre[- ]release"""), "preview")
            .replace(Regex("""(?i)snapshot"""), "dev")
            .replace(Regex("""(?i)nightly"""), "dev")
            .replace(Regex("""(?i)canary"""), "dev")
            .replace('_', '.')
            .replace(Regex("""\s+"""), "")
            .replace(Regex("""\.\-|\-\.|--"""), "-")
    }

    private fun filterLessSpecificCandidates(candidates: List<String>): List<String> {
        if (candidates.size <= 1) return candidates

        val richerKeys = candidates.mapNotNull { candidate ->
            parseVersionParts(candidate)?.takeIf { parts ->
                parts.channel != GitHubReleaseChannel.STABLE || parts.channelNumber > 0
            }?.numbers
        }.toSet()

        return candidates.filter { candidate ->
            val parts = parseVersionParts(candidate) ?: return@filter true
            val isTruncatedStable = parts.channel == GitHubReleaseChannel.STABLE &&
                parts.channelNumber == 0 &&
                parts.numbers in richerKeys
            !isTruncatedStable
        }.distinct()
    }

    private data class ComparableVersionCandidate(
        val normalized: String,
        val parts: VersionParts,
        val sourcePriority: Int,
        val semanticDepth: Int,
        val looksLikeDateStamp: Boolean
    )

    private fun parseComparableCandidate(
        raw: String,
        sourcePriority: Int
    ): ComparableVersionCandidate? {
        val normalized = canonicalizeCandidate(raw).lowercase(Locale.ROOT)
        val parts = parseVersionParts(normalized) ?: return null
        return ComparableVersionCandidate(
            normalized = normalized,
            parts = parts,
            sourcePriority = sourcePriority,
            semanticDepth = parts.numbers.size + if (parts.channel != GitHubReleaseChannel.STABLE) 1 else 0,
            looksLikeDateStamp = parts.channel == GitHubReleaseChannel.STABLE &&
                parts.numbers.size == 1 &&
                parts.numbers.firstOrNull() in 20_000_000..29_999_999
        )
    }

    private fun similarityScore(
        left: ComparableVersionCandidate,
        right: ComparableVersionCandidate
    ): Int {
        val sameRawBonus = if (left.normalized == right.normalized) 180 else 0
        val sharedNumericPrefix = sharedNumericPrefix(left.parts.numbers, right.parts.numbers)
        val sameNumericLengthBonus = if (left.parts.numbers.size == right.parts.numbers.size) 30 else 0
        val sameChannelBonus = if (left.parts.channel == right.parts.channel) 50 else 0
        val sameChannelNumberBonus = if (left.parts.channelNumber == right.parts.channelNumber) 25 else 0
        val sourceBonus = sourceReliabilityBonus(right.sourcePriority)
        val semanticDepthBonus = right.semanticDepth * 70
        val numericLengthPenalty = abs(left.parts.numbers.size - right.parts.numbers.size) * 10
        val channelNumberPenalty = abs(left.parts.channelNumber - right.parts.channelNumber).coerceAtMost(20) * 6
        val dateStampPenalty = if (right.looksLikeDateStamp && left.parts.numbers.size >= 2) 420 else 0
        return sameRawBonus +
            sharedNumericPrefix * 160 +
            sameNumericLengthBonus +
            sameChannelBonus +
            sameChannelNumberBonus +
            sourceBonus -
            dateStampPenalty +
            semanticDepthBonus -
            numericLengthPenalty -
            channelNumberPenalty
    }

    private fun sourceReliabilityBonus(sourcePriority: Int): Int {
        return when (sourcePriority) {
            GitHubVersionCandidateSource.Tag.priority -> 520
            GitHubVersionCandidateSource.Title.priority -> 420
            GitHubVersionCandidateSource.Link.priority -> 280
            GitHubVersionCandidateSource.Id.priority -> 160
            else -> 40
        }
    }

    private fun versionPartsSpecificityScore(parts: VersionParts): Int {
        val channelBonus = if (parts.channel != GitHubReleaseChannel.STABLE) 100 else 0
        val depthBonus = parts.numbers.size * 10
        val channelNumberBonus = parts.channelNumber.coerceAtMost(9)
        return channelBonus + depthBonus + channelNumberBonus
    }

    private fun sharedNumericPrefix(left: List<Int>, right: List<Int>): Int {
        val max = minOf(left.size, right.size)
        var count = 0
        for (index in 0 until max) {
            if (left[index] != right[index]) break
            count++
        }
        return count
    }

    private fun compareParsedVersionParts(a: VersionParts, b: VersionParts): Int {
        val max = maxOf(a.numbers.size, b.numbers.size)
        for (index in 0 until max) {
            val av = a.numbers.getOrElse(index) { 0 }
            val bv = b.numbers.getOrElse(index) { 0 }
            if (av != bv) return av.compareTo(bv)
        }

        val channelCmp = channelRank(a.channel).compareTo(channelRank(b.channel))
        if (channelCmp != 0) return channelCmp

        if (a.channelNumber != b.channelNumber) {
            return a.channelNumber.compareTo(b.channelNumber)
        }

        return 0
    }

    private fun channelRank(channel: GitHubReleaseChannel): Int {
        return when (channel) {
            GitHubReleaseChannel.DEV -> 0
            GitHubReleaseChannel.ALPHA -> 1
            GitHubReleaseChannel.BETA -> 2
            GitHubReleaseChannel.RC -> 3
            GitHubReleaseChannel.PREVIEW -> 4
            GitHubReleaseChannel.STABLE -> 5
            GitHubReleaseChannel.UNKNOWN -> 5
        }
    }

    private data class VersionParts(
        val numbers: List<Int>,
        val channel: GitHubReleaseChannel,
        val channelNumber: Int
    )

    private fun isMeaningfulReleaseIdentity(parts: VersionParts): Boolean {
        return parts.numbers.size >= 2 || (parts.channel.isPreRelease && parts.channelNumber > 0)
    }

    private fun releaseIdentityKey(parts: VersionParts): String {
        return buildString {
            append(parts.numbers.joinToString("."))
            append('|')
            append(parts.channel.name)
            append('|')
            append(parts.channelNumber)
        }
    }

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
            """(?:^|[^a-z])(dev|nightly|canary|snapshot|alpha|beta|rc|preview|pre(?:-release)?)(?:[^a-z0-9]*(\d+))?"""
        ).find(suffix.ifBlank { normalized })

        val channel = when (channelMatch?.groupValues?.getOrNull(1).orEmpty()) {
            "dev", "nightly", "canary", "snapshot" -> GitHubReleaseChannel.DEV
            "alpha" -> GitHubReleaseChannel.ALPHA
            "beta" -> GitHubReleaseChannel.BETA
            "rc" -> GitHubReleaseChannel.RC
            "preview", "pre", "pre-release" -> GitHubReleaseChannel.PREVIEW
            else -> GitHubReleaseChannel.STABLE
        }

        val channelNumber = channelMatch
            ?.groupValues
            ?.getOrNull(2)
            ?.toIntOrNull()
            ?: 0

        return VersionParts(
            numbers = coreNumbers,
            channel = channel,
            channelNumber = channelNumber
        )
    }
}
