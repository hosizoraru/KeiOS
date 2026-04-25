package os.kei.ui.page.main.home.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.ui.page.main.home.model.formatGitHubCacheAgo
import os.kei.ui.page.main.mcp.util.formatMcpUptimeText
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class HomePageContentState(
    val homeNa: String,
    val homeAppName: String,
    val homeTagline: String,
    val homeStatusMcp: String,
    val homeStatusGitHub: String,
    val homeStatusBa: String,
    val homeStatusShizuku: String,
    val homeCardMcp: String,
    val homeCardGitHub: String,
    val homeCardBa: String,
    val homeVisibleCardsTitle: String,
    val homeVisibleCardsDesc: String,
    val shizukuGranted: Boolean,
    val runningColor: Color,
    val stoppedColor: Color,
    val inactiveColor: Color,
    val cacheStateColor: Color,
    val appVersionText: String,
    val homeStatStatus: String,
    val mcpStatusText: String,
    val homeStatRuntime: String,
    val mcpRuntimeText: String,
    val homeStatClients: String,
    val mcpConnectedClients: Int,
    val homeStatNetwork: String,
    val networkModeText: String,
    val homeStatPort: String,
    val mcpPort: Int,
    val homeStatPath: String,
    val mcpEndpointPath: String,
    val homeStatService: String,
    val mcpServerName: String,
    val homeStatToken: String,
    val mcpTokenStatusText: String,
    val homeStatStableUpdates: String,
    val githubUpdatableLine: String,
    val homeStatPreReleaseUpdates: String,
    val githubPreReleaseUpdateLine: String,
    val homeStatTracked: String,
    val trackedCountLine: String,
    val homeStatCached: String,
    val cacheHitCountLine: String,
    val homeStatStrategy: String,
    val githubStrategyText: String,
    val homeStatApi: String,
    val githubApiText: String,
    val homeStatLastUpdate: String,
    val githubLastUpdateLine: String,
    val baActivationLine: String,
    val homeStatAp: String,
    val baApLine: String,
    val homeStatCafeAp: String,
    val baCafeApLine: String,
    val homeStatApRemaining: String,
    val baApRemainingLine: String
)

@Composable
internal fun rememberHomePageContentState(
    shizukuStatus: String,
    mcpOverview: HomeMcpOverview,
    githubOverview: HomeGitHubOverview,
    baOverview: HomeBaOverview
): HomePageContentState {
    val context = LocalContext.current
    val runningColor = Color(0xFF2E7D32)
    val stoppedColor = Color(0xFFC62828)
    val inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant
    val githubCacheColor = Color(0xFFF59E0B)
    val trackedCount = githubOverview.trackedCount
    val cacheHitCount = githubOverview.cacheHitCount
    val updatableCount = githubOverview.updatableCount
    val preReleaseUpdateCount = githubOverview.preReleaseUpdateCount
    val cacheStateColor = when {
        !githubOverview.loaded -> inactiveColor
        cacheHitCount > 0 -> githubCacheColor
        else -> inactiveColor
    }
    val homeStatusLoading = stringResource(R.string.home_status_loading)
    val homeGitHubUnconfigured = stringResource(R.string.home_github_status_unconfigured)
    val homeGitHubNoCache = stringResource(R.string.home_github_status_no_cache)
    val homeGitHubPendingRefresh = stringResource(R.string.home_github_status_pending_refresh)
    val homeJustNow = stringResource(R.string.home_time_just_now)
    val homeCommonFilled = stringResource(R.string.common_filled)
    val homeCommonNotUsed = stringResource(R.string.common_not_used)
    val homeMcpRuntimePending = stringResource(R.string.mcp_runtime_pending)
    val homeBaStatusActive = stringResource(R.string.home_ba_status_active)
    val homeBaStatusInactive = stringResource(R.string.home_ba_status_inactive)
    val homeAppVersionUnknownFallback = stringResource(R.string.home_app_version_unknown_fallback)
    val homeAppVersionUnknown = stringResource(R.string.home_app_version_unknown)
    val appVersionText = remember(homeAppVersionUnknownFallback, homeAppVersionUnknown) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${info.versionName ?: homeAppVersionUnknownFallback} (${info.longVersionCode})"
        }.getOrDefault(homeAppVersionUnknown)
    }
    val networkModeText = if (mcpOverview.allowExternal) {
        stringResource(R.string.mcp_network_mode_lan_short)
    } else {
        stringResource(R.string.mcp_network_mode_local_only_short)
    }
    val mcpRuntimeText = if (!mcpOverview.running || mcpOverview.runningSinceEpochMs <= 0L) {
        homeMcpRuntimePending
    } else {
        formatMcpUptimeText(System.currentTimeMillis() - mcpOverview.runningSinceEpochMs)
    }
    val mcpStatusText = if (mcpOverview.running) {
        stringResource(R.string.home_mcp_status_running)
    } else {
        stringResource(R.string.home_mcp_status_stopped)
    }
    val mcpTokenStatusText = if (mcpOverview.authTokenConfigured) {
        mcpOverview.authTokenPreview.ifBlank { homeCommonFilled }
    } else {
        homeCommonNotUsed
    }
    val githubStrategyText = when (githubOverview.strategy) {
        GitHubLookupStrategyOption.AtomFeed -> stringResource(R.string.github_overview_strategy_atom)
        GitHubLookupStrategyOption.GitHubApiToken -> stringResource(R.string.github_overview_strategy_api)
    }
    val githubApiText = when {
        githubOverview.strategy != GitHubLookupStrategyOption.GitHubApiToken -> homeCommonNotUsed
        githubOverview.apiTokenConfigured -> homeCommonFilled
        else -> stringResource(R.string.common_guest)
    }
    val cacheRefreshLine = formatGitHubCacheAgo(
        lastRefreshMs = githubOverview.cachedRefreshMs,
        notRefreshedText = stringResource(R.string.github_refresh_ago_not_refreshed),
        justNowText = homeJustNow
    )
    val githubLastUpdateLine = when {
        !githubOverview.loaded -> homeStatusLoading
        trackedCount == 0 -> homeGitHubUnconfigured
        cacheHitCount == 0 -> homeGitHubNoCache
        else -> cacheRefreshLine
    }
    val githubUpdatableLine = when {
        !githubOverview.loaded -> homeStatusLoading
        trackedCount == 0 -> stringResource(R.string.github_overview_value_count, 0)
        cacheHitCount == 0 -> homeGitHubPendingRefresh
        else -> stringResource(R.string.github_overview_value_count, updatableCount)
    }
    val githubPreReleaseUpdateLine = when {
        !githubOverview.loaded || trackedCount == 0 || cacheHitCount == 0 ->
            stringResource(R.string.github_overview_value_count, 0)
        else -> stringResource(R.string.github_overview_value_count, preReleaseUpdateCount)
    }
    val trackedCountLine = stringResource(R.string.github_overview_value_count, trackedCount)
    val cacheHitCountLine = stringResource(R.string.github_overview_value_count, cacheHitCount)
    val baApLine = if (baOverview.loaded) {
        stringResource(R.string.home_value_fraction, baOverview.apCurrent, baOverview.apLimit)
    } else {
        homeStatusLoading
    }
    val baCafeApLine = if (baOverview.loaded) {
        stringResource(R.string.home_value_fraction, baOverview.cafeStored, baOverview.cafeCap)
    } else {
        homeStatusLoading
    }
    val baActivationLine = if (baOverview.loaded) {
        if (baOverview.activated) homeBaStatusActive else homeBaStatusInactive
    } else {
        homeStatusLoading
    }
    val baApRemainingLine = if (baOverview.loaded) {
        (baOverview.apLimit - baOverview.apCurrent).coerceAtLeast(0).toString()
    } else {
        homeStatusLoading
    }
    return HomePageContentState(
        homeNa = stringResource(R.string.common_na),
        homeAppName = stringResource(R.string.app_name),
        homeTagline = stringResource(R.string.home_header_tagline),
        homeStatusMcp = stringResource(R.string.page_mcp_title),
        homeStatusGitHub = stringResource(R.string.github_page_title),
        homeStatusBa = stringResource(R.string.home_status_ba),
        homeStatusShizuku = stringResource(R.string.home_status_shizuku),
        homeCardMcp = stringResource(R.string.home_card_title_mcp),
        homeCardGitHub = stringResource(R.string.home_card_title_github_cache),
        homeCardBa = stringResource(R.string.home_card_title_ba),
        homeVisibleCardsTitle = stringResource(R.string.home_sheet_visible_cards_title),
        homeVisibleCardsDesc = stringResource(R.string.home_sheet_visible_cards_desc),
        shizukuGranted = shizukuStatus.contains("granted", ignoreCase = true),
        runningColor = runningColor,
        stoppedColor = stoppedColor,
        inactiveColor = inactiveColor,
        cacheStateColor = cacheStateColor,
        appVersionText = appVersionText,
        homeStatStatus = stringResource(R.string.home_stat_status),
        mcpStatusText = mcpStatusText,
        homeStatRuntime = stringResource(R.string.home_stat_runtime),
        mcpRuntimeText = mcpRuntimeText,
        homeStatClients = stringResource(R.string.home_stat_clients),
        mcpConnectedClients = mcpOverview.connectedClients,
        homeStatNetwork = stringResource(R.string.home_stat_network),
        networkModeText = networkModeText,
        homeStatPort = stringResource(R.string.home_stat_port),
        mcpPort = mcpOverview.port,
        homeStatPath = stringResource(R.string.home_stat_path),
        mcpEndpointPath = mcpOverview.endpointPath,
        homeStatService = stringResource(R.string.home_stat_service),
        mcpServerName = mcpOverview.serverName,
        homeStatToken = stringResource(R.string.home_stat_token),
        mcpTokenStatusText = mcpTokenStatusText,
        homeStatStableUpdates = stringResource(R.string.home_stat_stable_updates),
        githubUpdatableLine = githubUpdatableLine,
        homeStatPreReleaseUpdates = stringResource(R.string.home_stat_prerelease_updates),
        githubPreReleaseUpdateLine = githubPreReleaseUpdateLine,
        homeStatTracked = stringResource(R.string.home_stat_tracked),
        trackedCountLine = trackedCountLine,
        homeStatCached = stringResource(R.string.home_stat_cached),
        cacheHitCountLine = cacheHitCountLine,
        homeStatStrategy = stringResource(R.string.home_stat_strategy),
        githubStrategyText = githubStrategyText,
        homeStatApi = stringResource(R.string.home_stat_api),
        githubApiText = githubApiText,
        homeStatLastUpdate = stringResource(R.string.home_stat_last_update),
        githubLastUpdateLine = githubLastUpdateLine,
        baActivationLine = baActivationLine,
        homeStatAp = stringResource(R.string.home_stat_ap),
        baApLine = baApLine,
        homeStatCafeAp = stringResource(R.string.home_stat_cafe_ap),
        baCafeApLine = baCafeApLine,
        homeStatApRemaining = stringResource(R.string.home_stat_ap_remaining),
        baApRemainingLine = baApRemainingLine
    )
}
