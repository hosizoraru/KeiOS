package com.example.keios.feature.github.model

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

data class InstalledAppItem(
    val label: String,
    val packageName: String
)

data class GitHubCheckCacheEntry(
    val loading: Boolean = false,
    val localVersion: String = "",
    val localVersionCode: Long = -1L,
    val latestTag: String = "",
    val latestStableName: String = "",
    val latestStableRawTag: String = "",
    val latestStableUrl: String = "",
    val latestPreName: String = "",
    val latestPreRawTag: String = "",
    val latestPreUrl: String = "",
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
