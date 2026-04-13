package com.example.keios.feature.github.model

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

data class GitHubLookupConfig(
    val selectedStrategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.AtomFeed,
    val apiToken: String = ""
)
