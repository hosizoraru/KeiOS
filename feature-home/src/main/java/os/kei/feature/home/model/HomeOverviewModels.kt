package os.kei.feature.home.model

import androidx.compose.runtime.Immutable
import os.kei.core.prefs.CacheFreshnessSnapshot
import os.kei.feature.ba.identity.BA_CN_FRIEND_CODE_LENGTH
import os.kei.feature.ba.identity.BA_DEFAULT_FRIEND_CODE_LENGTH
import os.kei.feature.ba.identity.BA_FALLBACK_FRIEND_CODE
import os.kei.feature.ba.identity.isBaFriendCodeConfigured
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
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

@Immutable
data class HomeGitHubOverview(
    val trackedCount: Int = 0,
    val githubRepositoryCount: Int = 0,
    val gitRepositoryCount: Int = 0,
    val directApkCount: Int = 0,
    val fdroidRepositoryCount: Int = 0,
    val actionsTrackedCount: Int = 0,
    val preciseApkVersionCount: Int = 0,
    val cacheHitCount: Int = 0,
    val updatableCount: Int = 0,
    val preReleaseUpdateCount: Int = 0,
    val failedCount: Int = 0,
    val strategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.AtomFeed,
    val apiTokenConfigured: Boolean = false,
    val pendingShareImport: Boolean = false,
    val refreshIntervalHours: Int = 3,
    val cachedRefreshMs: Long = 0L,
    val cacheLabelNowMs: Long = 0L,
    val cacheFreshness: CacheFreshnessSnapshot = CacheFreshnessSnapshot.Empty,
    val refreshing: Boolean = false,
    val refreshScope: GitHubRefreshScope = GitHubRefreshScope.AllTracked,
    val refreshSource: GitHubRefreshSource = GitHubRefreshSource.Page,
    val refreshTargetCount: Int = 0,
    val refreshTotalTrackedCount: Int = 0,
    val refreshCompletedCount: Int = 0,
    val refreshProgress: Float = 0f,
    val loaded: Boolean = false,
)

@Immutable
data class HomeBaOverview(
    val activated: Boolean = false,
    val accountCount: Int = 0,
    val enabledAccountCount: Int = 0,
    val activeAccountName: String = "",
    val activeAccountEnabled: Boolean = false,
    val serverIndex: Int = 2,
    val apCurrent: Int = 0,
    val apLimit: Int = HOME_BA_AP_LIMIT_MAX,
    val apNotifyEnabled: Boolean = false,
    val apNotifyThreshold: Int = 120,
    val cafeLevel: Int = 10,
    val cafeStored: Int = 0,
    val cafeCap: Int = HOME_BA_CAFE_DAILY_AP_BY_LEVEL.last(),
    val cacheFreshness: CacheFreshnessSnapshot = CacheFreshnessSnapshot.Empty,
    val loaded: Boolean = false,
)

@Immutable
data class HomeWebDavOverview(
    val configured: Boolean = false,
    val autoSyncEnabled: Boolean = false,
    val enabledItemCount: Int = 0,
    val totalItemCount: Int = 0,
    val lastFullSyncTimeMs: Long = 0L,
    val lastAutoSyncTimeMs: Long = 0L,
    val autoSyncNeedsReview: Boolean = false,
    val autoSyncFailed: Boolean = false,
)

@Immutable
data class HomeAppOverview(
    val versionName: String = "",
    val versionCode: Long = 0L,
    val loaded: Boolean = false,
)

@Immutable
enum class HomeOverviewCard {
    MCP,
    GITHUB,
    WEBDAV,
    BA,
}

@Immutable
data class HomeOverviewSnapshot(
    val appOverview: HomeAppOverview = HomeAppOverview(),
    val mcpOverview: HomeMcpOverview = HomeMcpOverview(),
    val githubOverview: HomeGitHubOverview = HomeGitHubOverview(),
    val webDavOverview: HomeWebDavOverview = HomeWebDavOverview(),
    val baOverview: HomeBaOverview = HomeBaOverview(),
    val visibleOverviewCards: Set<HomeOverviewCard> = defaultHomeOverviewCards(),
    val showCacheFreshnessInCards: Boolean = false,
)

const val HOME_BA_AP_LIMIT_MAX = 240
const val HOME_BA_AP_MAX = 999
const val HOME_BA_DEFAULT_FRIEND_CODE = BA_FALLBACK_FRIEND_CODE

fun isHomeBaActivated(
    friendCode: String,
    serverIndex: Int? = null,
): Boolean = isBaFriendCodeConfigured(friendCode, serverIndex)

const val HOME_BA_CN_FRIEND_CODE_LENGTH = BA_CN_FRIEND_CODE_LENGTH
const val HOME_BA_DEFAULT_FRIEND_CODE_LENGTH = BA_DEFAULT_FRIEND_CODE_LENGTH
val HOME_BA_CAFE_DAILY_AP_BY_LEVEL =
    intArrayOf(
        92,
        152,
        222,
        302,
        390,
        460,
        530,
        600,
        570,
        740,
    )

fun defaultHomeOverviewCards(): Set<HomeOverviewCard> = HomeOverviewCard.entries.toSet()
