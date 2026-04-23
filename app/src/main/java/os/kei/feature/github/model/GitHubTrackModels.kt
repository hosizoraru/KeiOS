package os.kei.feature.github.model

import os.kei.BuildConfig

private const val keiOsTrackOwner = "hosizoraru"
private const val keiOsTrackRepo = "KeiOS"
private const val keiOsTrackRepoUrl = "https://github.com/$keiOsTrackOwner/$keiOsTrackRepo"
private const val keiOsTrackAppLabel = "KeiOS"

data class GitHubTrackedApp(
    val repoUrl: String,
    val owner: String,
    val repo: String,
    val packageName: String,
    val appLabel: String,
    val preferPreRelease: Boolean = false,
    val alwaysShowLatestReleaseDownloadButton: Boolean = false
) {
    val id: String
        get() = "$owner/$repo|$packageName"
}

internal fun defaultKeiOsTrackedApp(): GitHubTrackedApp {
    return GitHubTrackedApp(
        repoUrl = keiOsTrackRepoUrl,
        owner = keiOsTrackOwner,
        repo = keiOsTrackRepo,
        packageName = BuildConfig.APPLICATION_ID,
        appLabel = keiOsTrackAppLabel
    )
}

internal fun GitHubTrackedApp.isKeiOsSelfTrack(): Boolean {
    return owner.equals(keiOsTrackOwner, ignoreCase = true) &&
        repo.equals(keiOsTrackRepo, ignoreCase = true) &&
        packageName.equals(BuildConfig.APPLICATION_ID, ignoreCase = true)
}

data class InstalledAppItem(
    val label: String,
    val packageName: String,
    val lastUpdateTimeMs: Long = -1L
)

data class GitHubCheckCacheEntry(
    val loading: Boolean = false,
    val localVersion: String = "",
    val localVersionCode: Long = -1L,
    val latestTag: String = "",
    val latestStableName: String = "",
    val latestStableRawTag: String = "",
    val latestStableUrl: String = "",
    val latestStableUpdatedAtMillis: Long = -1L,
    val latestPreName: String = "",
    val latestPreRawTag: String = "",
    val latestPreUrl: String = "",
    val latestPreUpdatedAtMillis: Long = -1L,
    val hasStableRelease: Boolean = true,
    val hasUpdate: Boolean? = null,
    val message: String = "",
    val isPreRelease: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false,
    val hasPreReleaseUpdate: Boolean = false,
    val recommendsPreRelease: Boolean = false,
    val releaseHint: String = "",
    val sourceStrategyId: String = ""
)
