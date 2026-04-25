package os.kei.feature.home.model

import androidx.compose.runtime.Immutable
import os.kei.feature.github.model.GitHubLookupStrategyOption

@Immutable
data class HomeMcpOverview(
    val running: Boolean = false,
    val runningSinceEpochMs: Long = 0L,
    val port: Int = 0,
    val endpointPath: String = "",
    val serverName: String = "",
    val authTokenConfigured: Boolean = false,
    val authTokenPreview: String = "",
    val connectedClients: Int = 0,
    val allowExternal: Boolean = false,
)

data class HomeGitHubOverview(
    val trackedCount: Int = 0,
    val cacheHitCount: Int = 0,
    val updatableCount: Int = 0,
    val preReleaseUpdateCount: Int = 0,
    val strategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.AtomFeed,
    val apiTokenConfigured: Boolean = false,
    val cachedRefreshMs: Long = 0L,
    val loaded: Boolean = false
)

data class HomeBaOverview(
    val activated: Boolean = false,
    val apCurrent: Int = 0,
    val apLimit: Int = HOME_BA_AP_LIMIT_MAX,
    val cafeStored: Int = 0,
    val cafeCap: Int = HOME_BA_CAFE_DAILY_AP_BY_LEVEL.last(),
    val loaded: Boolean = false
)

enum class HomeOverviewCard {
    MCP,
    GITHUB,
    BA
}

data class HomeOverviewSnapshot(
    val mcpOverview: HomeMcpOverview = HomeMcpOverview(),
    val githubOverview: HomeGitHubOverview = HomeGitHubOverview(),
    val baOverview: HomeBaOverview = HomeBaOverview(),
    val visibleOverviewCards: Set<HomeOverviewCard> = defaultHomeOverviewCards()
)

internal const val HOME_BA_AP_LIMIT_MAX = 240
internal const val HOME_BA_AP_MAX = 999
internal const val HOME_BA_DEFAULT_FRIEND_CODE = "ARISUKEI"
internal val HOME_BA_CAFE_DAILY_AP_BY_LEVEL = intArrayOf(
    92,
    152,
    222,
    302,
    390,
    460,
    530,
    600,
    570,
    740
)

fun defaultHomeOverviewCards(): Set<HomeOverviewCard> = HomeOverviewCard.entries.toSet()
