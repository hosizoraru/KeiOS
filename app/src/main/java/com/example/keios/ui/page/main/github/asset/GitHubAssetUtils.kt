package com.example.keios.ui.page.main.github.asset

import android.content.Context
import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetRepository
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.ui.page.main.VersionCheckUi
import java.util.concurrent.TimeUnit

internal data class ApkAssetTarget(
    val rawTag: String,
    val releaseUrl: String,
    val label: String
)

internal fun VersionCheckUi.apkAssetTarget(
    owner: String,
    repo: String,
    context: Context,
    alwaysLatestRelease: Boolean = false
): ApkAssetTarget? {
    val stableTag = latestStableRawTag.ifBlank {
        GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latestStableUrl)
    }
    val preTag = latestPreRawTag.ifBlank {
        GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latestPreUrl)
    }
    if (alwaysLatestRelease) {
        val latestStableTagCandidate = stableTag.trim()
        val latestVersionTagCandidate = latestTag.trim()
        val latestPreTagCandidate = preTag.trim()
        val latestReleaseTag = when {
            latestStableTagCandidate.isNotBlank() -> latestStableTagCandidate
            latestVersionTagCandidate.isNotBlank() -> latestVersionTagCandidate
            latestPreTagCandidate.isNotBlank() -> latestPreTagCandidate
            else -> ""
        }
        val latestReleaseUrl = when {
            latestStableUrl.isNotBlank() -> latestStableUrl.trim()
            latestReleaseTag.isNotBlank() ->
                GitHubVersionUtils.buildReleaseTagUrl(owner, repo, latestReleaseTag)
            latestPreUrl.isNotBlank() -> latestPreUrl.trim()
            else -> ""
        }
        if (latestReleaseTag.isBlank() && latestReleaseUrl.isBlank()) return null
        val targetTag = latestReleaseTag.ifBlank {
            GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latestReleaseUrl)
        }
        if (targetTag.isBlank()) return null
        return ApkAssetTarget(
            rawTag = targetTag,
            releaseUrl = latestReleaseUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, targetTag) },
            label = context.getString(R.string.github_asset_target_latest)
        )
    }
    return when {
        recommendsPreRelease && preTag.isNotBlank() -> ApkAssetTarget(
            rawTag = preTag,
            releaseUrl = latestPreUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, preTag) },
            label = context.getString(R.string.github_asset_target_prerelease)
        )
        hasUpdate == true && stableTag.isNotBlank() -> ApkAssetTarget(
            rawTag = stableTag,
            releaseUrl = latestStableUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, stableTag) },
            label = context.getString(R.string.github_asset_target_stable)
        )
        hasPreReleaseUpdate && preTag.isNotBlank() -> ApkAssetTarget(
            rawTag = preTag,
            releaseUrl = latestPreUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, preTag) },
            label = context.getString(R.string.github_asset_target_prerelease)
        )
        else -> null
    }
}

internal fun formatAssetSize(sizeBytes: Long, context: Context): String {
    if (sizeBytes <= 0L) return context.getString(R.string.common_unknown)
    val kb = 1024L
    val mb = kb * 1024L
    val gb = mb * 1024L
    return when {
        sizeBytes >= gb -> String.format("%.1fG", sizeBytes.toDouble() / gb.toDouble())
        sizeBytes >= mb -> String.format("%.1fM", sizeBytes.toDouble() / mb.toDouble())
        sizeBytes >= kb -> String.format("%.0fK", sizeBytes.toDouble() / kb.toDouble())
        else -> "${sizeBytes}B"
    }
}

internal fun prefersApiAssetTransport(asset: GitHubReleaseAssetFile): Boolean {
    return asset.apiAssetUrl.isNotBlank()
}

internal fun assetTransportLabel(asset: GitHubReleaseAssetFile, context: Context): String {
    return if (prefersApiAssetTransport(asset)) "API" else context.getString(R.string.github_asset_transport_direct)
}

internal fun bundleTransportLabel(bundle: GitHubReleaseAssetBundle?, context: Context): String? {
    bundle ?: return null
    return if (bundle.assets.any { prefersApiAssetTransport(it) }) {
        "API"
    } else {
        context.getString(R.string.github_asset_transport_direct)
    }
}

internal fun bundleCommitLabel(bundle: GitHubReleaseAssetBundle?): String? {
    return bundle?.shortCommitSha?.trim().takeIf { !it.isNullOrBlank() }
}

internal fun assetAbiLabel(fileName: String): String? {
    val lower = fileName.lowercase()
    return when {
        "arm64-v8a" in lower || "aarch64" in lower ||
            Regex("(^|[^a-z0-9])arm64([^a-z0-9]|$)").containsMatchIn(lower) -> "arm64"
        "universal" in lower || "fat" in lower -> "universal"
        "armeabi-v7a" in lower || "armv7" in lower -> "armeabi-v7a"
        Regex("(^|[^a-z0-9])armeabi([^a-z0-9]|$)").containsMatchIn(lower) -> "armeabi"
        "x86_64" in lower -> "x86_64"
        Regex("""(^|[^a-z0-9])x86([^a-z0-9]|$)""").containsMatchIn(lower) -> "x86"
        else -> null
    }
}

internal fun assetFileExtensionLabel(fileName: String): String? {
    val trimmedName = fileName.trim()
    val extension = trimmedName.substringAfterLast('.', "")
        .takeIf { '.' in trimmedName && it.isNotBlank() }
        ?.lowercase()
        ?: return null
    return extension
}

internal fun assetDisplayName(fileName: String): String {
    val trimmedName = fileName.trim()
    val extension = assetFileExtensionLabel(trimmedName) ?: return trimmedName
    return trimmedName.removeSuffix(".$extension")
}

internal fun assetRelativeTimeLabel(
    updatedAtMillis: Long?,
    context: Context
): String? {
    val updatedAt = updatedAtMillis ?: return null
    val diffMillis = (System.currentTimeMillis() - updatedAt).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        minutes < 1L -> context.getString(R.string.github_asset_relative_just_now)
        minutes < 60L -> context.getString(R.string.github_asset_relative_min_ago, minutes)
        hours < 24L -> context.getString(R.string.github_asset_relative_hr_ago, hours)
        days < 7L && days == 1L -> context.getString(R.string.github_asset_relative_day_ago, days)
        days < 7L -> context.getString(R.string.github_asset_relative_days_ago, days)
        days < 30L -> context.getString(R.string.github_asset_relative_last_week)
        days < 60L -> context.getString(R.string.github_asset_relative_last_month)
        else -> context.getString(R.string.github_asset_relative_months_ago, days / 30L)
    }
}
