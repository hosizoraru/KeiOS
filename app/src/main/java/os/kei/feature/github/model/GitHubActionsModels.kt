package os.kei.feature.github.model

enum class GitHubActionsArtifactKind {
    AndroidPackage,
    AndroidBundle,
    Archive,
    Mapping,
    Report,
    Source,
    Unknown
}

enum class GitHubActionsArtifactPlatform {
    Android,
    Windows,
    Linux,
    MacOS,
    Web,
    Generic,
    Unknown
}

enum class GitHubActionsWorkflowKind {
    AndroidBuild,
    Release,
    Nightly,
    Ci,
    Quality,
    Localization,
    Dependency,
    Documentation,
    Automation,
    Unknown
}

enum class GitHubActionsRunBranchTrust {
    DefaultBranch,
    ReleaseTag,
    MainlineBranch,
    ReleaseBranch,
    PullRequest,
    FeatureBranch,
    Unknown
}

enum class GitHubActionsRunWatchState {
    Queued,
    Running,
    Completed,
    Failed,
    Unknown
}

data class GitHubActionsRepositoryInfo(
    val owner: String,
    val repo: String,
    val fullName: String = "",
    val defaultBranch: String = ""
)

data class GitHubActionsWorkflow(
    val id: Long,
    val nodeId: String = "",
    val name: String,
    val path: String = "",
    val state: String = "",
    val htmlUrl: String = "",
    val badgeUrl: String = "",
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
) {
    val displayName: String
        get() = name.ifBlank { path.ifBlank { id.toString() } }
}

data class GitHubActionsWorkflowRun(
    val id: Long,
    val name: String = "",
    val displayTitle: String = "",
    val workflowId: Long = 0L,
    val workflowName: String = "",
    val runNumber: Long = 0L,
    val runAttempt: Int = 0,
    val event: String = "",
    val status: String = "",
    val conclusion: String = "",
    val headBranch: String = "",
    val headSha: String = "",
    val htmlUrl: String = "",
    val artifactsUrl: String = "",
    val actorLogin: String = "",
    val triggeringActorLogin: String = "",
    val repositoryFullName: String = "",
    val headRepositoryFullName: String = "",
    val headRepositoryFork: Boolean = false,
    val pullRequestCount: Int = 0,
    val checkSuiteId: Long = 0L,
    val createdAtMillis: Long? = null,
    val runStartedAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
) {
    val displayName: String
        get() = displayTitle.ifBlank { name.ifBlank { workflowName.ifBlank { "#$runNumber" } } }
}

data class GitHubActionsArtifact(
    val id: Long,
    val nodeId: String = "",
    val name: String,
    val sizeBytes: Long = 0L,
    val expired: Boolean = false,
    val digest: String = "",
    val archiveDownloadUrl: String = "",
    val workflowRunId: Long = 0L,
    val workflowRunHeadBranch: String = "",
    val workflowRunHeadSha: String = "",
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,
    val expiresAtMillis: Long? = null
)

data class GitHubActionsRunArtifacts(
    val run: GitHubActionsWorkflowRun,
    val artifacts: List<GitHubActionsArtifact>
)

data class GitHubActionsWorkflowArtifactsSnapshot(
    val owner: String,
    val repo: String,
    val workflowId: String,
    val runs: List<GitHubActionsRunArtifacts>,
    val fetchedAtMillis: Long = System.currentTimeMillis()
) {
    val artifacts: List<GitHubActionsArtifact>
        get() = runs.flatMap { it.artifacts }
}

data class GitHubActionsRunStatusSnapshot(
    val owner: String,
    val repo: String,
    val run: GitHubActionsWorkflowRun,
    val artifacts: List<GitHubActionsArtifact> = emptyList(),
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

data class GitHubActionsArtifactNameTraits(
    val normalizedName: String,
    val extension: String = "",
    val kind: GitHubActionsArtifactKind = GitHubActionsArtifactKind.Unknown,
    val platform: GitHubActionsArtifactPlatform = GitHubActionsArtifactPlatform.Unknown,
    val abi: String = "",
    val flavors: List<String> = emptyList(),
    val channel: GitHubReleaseChannel = GitHubReleaseChannel.UNKNOWN,
    val releaseLike: Boolean = false,
    val debugLike: Boolean = false,
    val universalLike: Boolean = false,
    val buildNoise: Boolean = false
) {
    val androidLike: Boolean
        get() = kind == GitHubActionsArtifactKind.AndroidPackage ||
            kind == GitHubActionsArtifactKind.AndroidBundle
}

data class GitHubActionsArtifactSelectionOptions(
    val query: String = "",
    val includeRegex: Regex? = null,
    val excludeRegex: Regex? = null,
    val preferredAbis: List<String> = emptyList(),
    val hideExpired: Boolean = true,
    val hideBuildNoise: Boolean = true,
    val includeNonAndroidArtifacts: Boolean = false,
    val aggressiveAbiFiltering: Boolean = false,
    val downloadHistory: List<GitHubActionsDownloadRecord> = emptyList()
)

data class GitHubActionsArtifactMatch(
    val artifact: GitHubActionsArtifact,
    val traits: GitHubActionsArtifactNameTraits,
    val score: Int,
    val lastDownload: GitHubActionsDownloadRecord? = null,
    val reasons: List<String> = emptyList()
)

data class GitHubActionsArtifactDownloadResolution(
    val artifactId: Long,
    val downloadUrl: String
)

data class GitHubActionsWorkflowTraits(
    val normalizedName: String,
    val normalizedPath: String,
    val fileName: String,
    val kind: GitHubActionsWorkflowKind = GitHubActionsWorkflowKind.Unknown,
    val active: Boolean = false,
    val dynamic: Boolean = false,
    val androidLike: Boolean = false,
    val buildLike: Boolean = false,
    val releaseLike: Boolean = false,
    val nightlyLike: Boolean = false,
    val maintenanceLike: Boolean = false
)

data class GitHubActionsWorkflowArtifactSignal(
    val workflowId: Long,
    val recentRunCount: Int = 0,
    val successfulRunCount: Int = 0,
    val artifactCount: Int = 0,
    val nonExpiredArtifactCount: Int = 0,
    val androidArtifactCount: Int = 0,
    val trustedRunCount: Int = 0,
    val defaultBranchRunCount: Int = 0,
    val releaseTagRunCount: Int = 0,
    val pullRequestRunCount: Int = 0,
    val recommendedRunId: Long? = null,
    val recommendedRunBranch: String = "",
    val recommendedRunEvent: String = "",
    val recommendedArtifactCount: Int = 0,
    val branchRunCounts: Map<String, Int> = emptyMap(),
    val artifactNames: List<String> = emptyList()
)

data class GitHubActionsWorkflowSelectionOptions(
    val query: String = "",
    val preferredWorkflowIds: Set<Long> = emptySet(),
    val preferredWorkflowPaths: Set<String> = emptySet(),
    val includeDisabled: Boolean = false,
    val requireArtifacts: Boolean = false,
    val downloadHistory: List<GitHubActionsDownloadRecord> = emptyList()
)

data class GitHubActionsWorkflowMatch(
    val workflow: GitHubActionsWorkflow,
    val traits: GitHubActionsWorkflowTraits,
    val signal: GitHubActionsWorkflowArtifactSignal? = null,
    val score: Int,
    val lastDownload: GitHubActionsDownloadRecord? = null,
    val reasons: List<String> = emptyList()
)

data class GitHubActionsRunTraits(
    val normalizedBranch: String,
    val normalizedEvent: String,
    val normalizedStatus: String,
    val normalizedConclusion: String,
    val branchTrust: GitHubActionsRunBranchTrust = GitHubActionsRunBranchTrust.Unknown,
    val completed: Boolean = false,
    val successful: Boolean = false,
    val inProgress: Boolean = false,
    val pullRequestLike: Boolean = false,
    val forkLike: Boolean = false,
    val defaultBranch: Boolean = false,
    val releaseTag: Boolean = false,
    val releaseBranch: Boolean = false,
    val safeForRecommendation: Boolean = false
)

data class GitHubActionsRunSelectionOptions(
    val defaultBranch: String = "",
    val preferredBranches: Set<String> = emptySet(),
    val preferredEvents: Set<String> = setOf("push", "workflow_dispatch", "release"),
    val includePullRequests: Boolean = false,
    val includeNonDefaultBranches: Boolean = true,
    val includeUnsuccessful: Boolean = false,
    val requireArtifacts: Boolean = true,
    val requireAndroidArtifacts: Boolean = true,
    val artifactOptions: GitHubActionsArtifactSelectionOptions = GitHubActionsArtifactSelectionOptions(),
    val downloadHistory: List<GitHubActionsDownloadRecord> = emptyList()
)

data class GitHubActionsRunMatch(
    val runArtifacts: GitHubActionsRunArtifacts,
    val traits: GitHubActionsRunTraits,
    val artifactMatches: List<GitHubActionsArtifactMatch>,
    val score: Int,
    val lastDownload: GitHubActionsDownloadRecord? = null,
    val reasons: List<String> = emptyList()
)

data class GitHubActionsDownloadRecord(
    val owner: String,
    val repo: String,
    val workflowId: Long = 0L,
    val workflowName: String = "",
    val workflowPath: String = "",
    val runId: Long = 0L,
    val runNumber: Long = 0L,
    val runAttempt: Int = 0,
    val runDisplayName: String = "",
    val headBranch: String = "",
    val headSha: String = "",
    val event: String = "",
    val status: String = "",
    val conclusion: String = "",
    val artifactId: Long = 0L,
    val artifactName: String = "",
    val artifactDigest: String = "",
    val artifactSizeBytes: Long = 0L,
    val sourceTrackId: String = "",
    val packageName: String = "",
    val downloadedAtMillis: Long = System.currentTimeMillis()
)

data class GitHubActionsRunTrackingPlan(
    val runId: Long,
    val state: GitHubActionsRunWatchState,
    val status: String,
    val conclusion: String,
    val pollable: Boolean,
    val nextPollDelayMillis: Long = 0L,
    val staleAfterMillis: Long = 0L,
    val reason: String = ""
)
