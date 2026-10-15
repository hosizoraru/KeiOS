package os.kei.feature.github.model

enum class GitHubActionsLookupStrategyOption(
    val storageId: String,
    val label: String
) {
    NightlyLink(
        storageId = "nightly_link",
        label = "nightly.link"
    ),
    GitHubApiToken(
        storageId = "github_api_token",
        label = "GitHub API Token"
    );

    companion object {
        fun fromStorageId(value: String): GitHubActionsLookupStrategyOption {
            return entries.firstOrNull { it.storageId == value } ?: NightlyLink
        }
    }
}

enum class GitHubReleaseNotesMode(
    val storageId: String
) {
    Off("off"),
    Compact("compact"),
    Expanded("expanded");

    companion object {
        fun fromStorageId(value: String): GitHubReleaseNotesMode {
            return entries.firstOrNull { it.storageId == value } ?: Off
        }
    }
}

enum class GitHubShareImportFlowMode(
    val storageId: String
) {
    SheetAssisted("sheet_assisted"),
    NotificationFirst("notification_first");

    companion object {
        fun fromStorageId(value: String): GitHubShareImportFlowMode {
            return entries.firstOrNull { it.storageId == value } ?: SheetAssisted
        }
    }
}

data class GitHubLookupConfig(
    val selectedStrategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.AtomFeed,
    val actionsStrategy: GitHubActionsLookupStrategyOption = GitHubActionsLookupStrategyOption.NightlyLink,
    val apiToken: String = "",
    val checkAllTrackedPreReleases: Boolean = false,
    val checkAllDirectApkPreReleases: Boolean = false,
    val aggressiveApkFiltering: Boolean = false,
    val preciseApkVersionEnabled: Boolean = false,
    val scanSystemAppsByDefault: Boolean = false,
    val profileDepth: GitHubProfileDepth = GitHubProfileDepth.Basic,
    val shareImportFlowMode: GitHubShareImportFlowMode = GitHubShareImportFlowMode.SheetAssisted,
    val appManagedShareInstallEnabled: Boolean = false,
    val foregroundManagedDownloadBoostEnabled: Boolean = false,
    val onlineShareTargetPackage: String = "",
    val preferredDownloaderPackage: String = "",
    val decisionAssistEnabled: Boolean = false,
    val repositoryHealthCardEnabled: Boolean = false,
    val apkTrustCheckEnabled: Boolean = false,
    val releaseNotesMode: GitHubReleaseNotesMode = GitHubReleaseNotesMode.Off,
    val fdroidCommonRepoIds: List<String> = FdroidRepositoryPresets.defaultCommonSearchRepoIds
) {
    // Compatibility field for MCP diagnostics; share-import linkage is always enabled.
    val shareImportLinkageEnabled: Boolean
        get() = true

    val normalizedFdroidCommonRepoIds: List<String>
        get() = FdroidRepositoryPresets.normalizedCommonSearchRepoIds(fdroidCommonRepoIds)

    val actionsRequireApiToken: Boolean
        get() = actionsStrategy == GitHubActionsLookupStrategyOption.GitHubApiToken

    val actionsArtifactDownloadsAvailable: Boolean
        get() = actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink ||
            apiToken.trim().isNotBlank()
}

fun GitHubLookupConfig.githubCheckSourceSignature(): String {
    return listOf(
        // Old Atom checks could classify numeric prereleases as stable when
        // /latest redirected to the release index. Recompute those cached lanes.
        if (selectedStrategy == GitHubLookupStrategyOption.AtomFeed) "check-v4-atom-release-index"
        else "check-v3-package-scope",
        selectedStrategy.storageId,
        apiToken.trim().isNotBlank().toString(),
        checkAllTrackedPreReleases.toString(),
        aggressiveApkFiltering.toString(),
        preciseApkVersionEnabled.toString(),
        scanSystemAppsByDefault.toString(),
        profileDepth.storageId
    ).joinToString("|")
}

fun GitHubLookupConfig.defaultRepositoryProfilePurpose(): GitHubRepositoryProfilePurpose {
    return if (decisionAssistEnabled && repositoryHealthCardEnabled) {
        GitHubRepositoryProfilePurpose.HealthCard
    } else {
        GitHubRepositoryProfilePurpose.VersionCheckFast
    }
}

fun GitHubLookupConfig.githubProfileSourceSignature(
    purpose: GitHubRepositoryProfilePurpose = defaultRepositoryProfilePurpose()
): String {
    return githubProfileSourceSignature(purpose.requiredCapabilities(profileDepth))
}

fun GitHubLookupConfig.githubProfileSourceSignature(
    capabilities: Set<GitHubRepositoryProfileCapability>
): String {
    return listOf(
        "profile-v1",
        selectedStrategy.storageId,
        apiToken.trim().isNotBlank().toString(),
        checkAllTrackedPreReleases.toString(),
        aggressiveApkFiltering.toString(),
        preciseApkVersionEnabled.toString(),
        scanSystemAppsByDefault.toString(),
        profileDepth.storageId,
        capabilities.sortedBy { it.name }.joinToString(",") { it.name }
    ).joinToString("|")
}

fun GitHubLookupConfig.githubAssetSourceSignature(): String {
    return listOf(
        "asset-v4",
        selectedStrategy.storageId,
        actionsStrategy.storageId,
        apiToken.trim().isNotBlank().toString(),
        aggressiveApkFiltering.toString(),
        preciseApkVersionEnabled.toString()
    ).joinToString("|")
}
