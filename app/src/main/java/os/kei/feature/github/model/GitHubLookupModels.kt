package os.kei.feature.github.model

enum class GitHubLookupStrategyOption(
    val storageId: String,
    val label: String
) {
    AtomFeed(
        storageId = "atom_feed",
        label = "Atom Feed"
    ),
    GitHubApiToken(
        storageId = "github_api_token",
        label = "GitHub API Token"
    );

    companion object {
        fun fromStorageId(value: String): GitHubLookupStrategyOption {
            return entries.firstOrNull { it.storageId == value } ?: AtomFeed
        }
    }
}

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

data class GitHubLookupConfig(
    val selectedStrategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.AtomFeed,
    val actionsStrategy: GitHubActionsLookupStrategyOption = GitHubActionsLookupStrategyOption.NightlyLink,
    val apiToken: String = "",
    val checkAllTrackedPreReleases: Boolean = false,
    val aggressiveApkFiltering: Boolean = false,
    val shareImportLinkageEnabled: Boolean = false,
    val onlineShareTargetPackage: String = "",
    val preferredDownloaderPackage: String = ""
) {
    val actionsRequireApiToken: Boolean
        get() = actionsStrategy == GitHubActionsLookupStrategyOption.GitHubApiToken

    val actionsArtifactDownloadsAvailable: Boolean
        get() = actionsStrategy == GitHubActionsLookupStrategyOption.NightlyLink ||
            apiToken.trim().isNotBlank()
}
