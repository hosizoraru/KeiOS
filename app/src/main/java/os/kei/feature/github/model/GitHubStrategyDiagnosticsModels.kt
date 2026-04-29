package os.kei.feature.github.model

data class GitHubRepoTarget(
    val owner: String,
    val repo: String
) {
    val id: String
        get() = "$owner/$repo"
}

enum class GitHubApiAuthMode(val label: String) {
    Guest("Guest"),
    Token("Token")
}

data class GitHubStrategyLoadTrace<T>(
    val result: Result<T>,
    val fromCache: Boolean,
    val elapsedMs: Long,
    val authMode: GitHubApiAuthMode? = null
)

data class GitHubApiCredentialStatus(
    val authMode: GitHubApiAuthMode,
    val coreLimit: Int,
    val coreRemaining: Int,
    val coreUsed: Int,
    val resetAtMillis: Long? = null
) {
    val summaryLabel: String
        get() = when (authMode) {
            GitHubApiAuthMode.Guest -> "Guest API available"
            GitHubApiAuthMode.Token -> "Token available"
        }
}

data class GitHubStrategyBenchmarkSample(
    val target: GitHubRepoTarget,
    val success: Boolean,
    val fromCache: Boolean,
    val elapsedMs: Long,
    val message: String = "",
    val stableTag: String = "",
    val preReleaseTag: String = ""
)

data class GitHubStrategyBenchmarkResult(
    val strategyId: String,
    val displayName: String,
    val authMode: GitHubApiAuthMode? = null,
    val coldSamples: List<GitHubStrategyBenchmarkSample>,
    val warmSamples: List<GitHubStrategyBenchmarkSample>
) {
    val totalTargets: Int
        get() = coldSamples.size

    val coldSuccessCount: Int
        get() = coldSamples.count { it.success }

    val warmSuccessCount: Int
        get() = warmSamples.count { it.success }

    val cacheHitCount: Int
        get() = warmSamples.count { it.success && it.fromCache }

    val cacheHitRate: Float
        get() = if (warmSamples.isEmpty()) 0f else cacheHitCount.toFloat() / warmSamples.size.toFloat()

    val coldAverageMs: Long
        get() = coldSamples.map { it.elapsedMs }.averageRounded()

    val warmAverageMs: Long
        get() = warmSamples.map { it.elapsedMs }.averageRounded()

    val summaryLabel: String
        get() = authMode?.let { "$displayName · ${it.label}" } ?: displayName

    val failures: List<String>
        get() = (coldSamples + warmSamples)
            .filter { !it.success && it.message.isNotBlank() }
            .map { "${it.target.id}: ${it.message}" }
            .distinct()
}

data class GitHubStrategyBenchmarkReport(
    val targets: List<GitHubRepoTarget>,
    val results: List<GitHubStrategyBenchmarkResult>,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

private fun List<Long>.averageRounded(): Long {
    if (isEmpty()) return 0L
    return (sumOf { it } / size.toDouble()).toLong()
}
