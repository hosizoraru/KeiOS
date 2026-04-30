package os.kei.feature.github.model

enum class GitHubReleaseChannel(val isPreRelease: Boolean) {
    DEV(true),
    ALPHA(true),
    BETA(true),
    RC(true),
    PREVIEW(true),
    STABLE(false),
    UNKNOWN(false)
}

enum class GitHubReleaseSignalSource {
    LatestRedirect,
    AtomEntry,
    AtomFallback,
    GitHubApi
}

enum class GitHubVersionCandidateSource(val priority: Int) {
    Tag(0),
    Title(1),
    Link(2),
    Id(3),
    Content(4)
}

enum class GitHubTrackedReleaseStatus(
    val defaultMessage: String,
    private val legacyMessage: String
) {
    UpdateAvailable("github.status.update_available", "\u53d1\u73b0\u66f4\u65b0"),
    PreReleaseUpdateAvailable("github.status.prerelease_update_available", "\u9884\u53d1\u6709\u66f4\u65b0"),
    PreReleaseOptional("github.status.prerelease_optional", "\u9884\u53d1\u53ef\u9009"),
    PreReleaseTracked("github.status.prerelease_tracked", "\u9884\u53d1\u884c"),
    UpToDate("github.status.up_to_date", "\u5df2\u662f\u6700\u65b0"),
    MatchedRelease("github.status.matched_release", "\u5df2\u5339\u914d\u53d1\u884c"),
    ComparisonUncertain(
        "github.status.comparison_uncertain",
        "\u7248\u672c\u683c\u5f0f\u65e0\u6cd5\u7cbe\u786e\u6bd4\u8f83"
    ),
    Failed("github.status.failed", "\u68c0\u67e5\u5931\u8d25");

    fun failureMessage(detail: String): String {
        return "$defaultMessage: $detail"
    }

    companion object {
        const val ONLY_PRERELEASES_HINT_MESSAGE = "github.status.only_prereleases_hint"
        private const val LEGACY_ONLY_PRERELEASES_HINT =
            "\u8be5\u9879\u76ee\u6682\u65f6\u53ef\u80fd\u53ea\u6709\u9884\u53d1\u884c\u7248"
        private const val ENGLISH_FAILED = "Check failed"

        fun fromMessage(raw: String): GitHubTrackedReleaseStatus? {
            val message = raw.trim()
            if (message.isBlank()) return null
            return entries.firstOrNull { status ->
                    message == status.defaultMessage ||
                    message == status.legacyMessage ||
                    message.startsWith("${status.defaultMessage}:") ||
                    message.startsWith("${status.legacyMessage}:") ||
                    (status == Failed && (message == ENGLISH_FAILED || message.startsWith("$ENGLISH_FAILED:")))
            }
        }

        fun isFailureMessage(raw: String): Boolean {
            return fromMessage(raw) == Failed
        }

        fun isOnlyPreReleasesHint(raw: String): Boolean {
            val message = raw.trim()
            return message == ONLY_PRERELEASES_HINT_MESSAGE ||
                message == LEGACY_ONLY_PRERELEASES_HINT
        }

        fun localizedFailureDetail(raw: String, prefix: String): String {
            val message = raw.trim()
            val failed = Failed
            return when {
                message.startsWith("${failed.defaultMessage}:") ->
                    message.replaceFirst(failed.defaultMessage, prefix)
                message.startsWith("${failed.legacyMessage}:") ->
                    message.replaceFirst(failed.legacyMessage, prefix)
                message.startsWith("$ENGLISH_FAILED:") ->
                    message.replaceFirst(ENGLISH_FAILED, prefix)
                message == failed.defaultMessage || message == failed.legacyMessage -> prefix
                message == ENGLISH_FAILED -> prefix
                else -> message
            }
        }
    }
}

data class GitHubVersionCandidate(
    val value: String,
    val source: GitHubVersionCandidateSource
)

data class GitHubReleaseVersionSignals(
    val displayVersion: String,
    val rawTag: String,
    val rawName: String,
    val link: String = "",
    val updatedAtMillis: Long? = null,
    val versionCandidates: List<GitHubVersionCandidate> = emptyList(),
    val source: GitHubReleaseSignalSource = GitHubReleaseSignalSource.AtomFallback,
    val channel: GitHubReleaseChannel = GitHubReleaseChannel.UNKNOWN,
    val authorName: String = ""
) {
    val candidates: List<String>
        get() = versionCandidates.map { it.value }
}

data class GitHubAtomReleaseEntry(
    val entryId: String = "",
    val tag: String,
    val title: String,
    val link: String,
    val updatedAtMillis: Long? = null,
    val contentHtml: String = "",
    val contentText: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val versionCandidates: List<GitHubVersionCandidate> = emptyList(),
    val channel: GitHubReleaseChannel = GitHubReleaseChannel.UNKNOWN,
    val isLikelyPreRelease: Boolean
) {
    val displayVersion: String
        get() = title.ifBlank { tag }

    val candidates: List<String>
        get() = versionCandidates.map { it.value }
}

data class GitHubAtomFeed(
    val title: String = "",
    val feedUrl: String = "",
    val updatedAtMillis: Long? = null,
    val entries: List<GitHubAtomReleaseEntry> = emptyList()
)

data class GitHubRepositoryReleaseSnapshot(
    val strategyId: String,
    val feed: GitHubAtomFeed,
    val latestStable: GitHubReleaseVersionSignals,
    val hasStableRelease: Boolean = true,
    val latestPreRelease: GitHubReleaseVersionSignals? = null,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

data class GitHubTrackedReleaseCheck(
    val strategyId: String,
    val localVersion: String,
    val localVersionCode: Long = -1L,
    val matchedRelease: GitHubAtomReleaseEntry? = null,
    val stableRelease: GitHubReleaseVersionSignals? = null,
    val preRelease: GitHubReleaseVersionSignals? = null,
    val hasStableRelease: Boolean = true,
    val hasUpdate: Boolean? = null,
    val hasPreReleaseUpdate: Boolean = false,
    val recommendsPreRelease: Boolean = false,
    val isPreReleaseInstalled: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false,
    val releaseHint: String = "",
    val status: GitHubTrackedReleaseStatus = GitHubTrackedReleaseStatus.ComparisonUncertain,
    val message: String = status.defaultMessage
)
