package os.kei.ui.page.main.home.state

import os.kei.core.prefs.CacheFreshnessSnapshot
import os.kei.core.privilege.PrivilegedShell
import os.kei.feature.home.model.HomeAppOverview
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeWebDavOverview
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.ui.page.main.home.model.formatGitHubCacheAgo
import os.kei.ui.page.main.widget.status.AppStatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import os.kei.core.privilege.PrivilegeStatus

internal fun deriveHomePageContentState(
    privilegeStatus: PrivilegeStatus,
    appOverview: HomeAppOverview,
    mcpOverview: HomeMcpOverview,
    githubOverview: HomeGitHubOverview,
    webDavOverview: HomeWebDavOverview,
    baOverview: HomeBaOverview,
    runtimeNowMs: Long,
    text: HomePageContentTextBundle,
    colors: HomePageContentColors,
): HomePageContentState {
    val trackedCount = githubOverview.trackedCount
    val cacheHitCount = githubOverview.cacheHitCount
    val updatableCount = githubOverview.updatableCount
    val preReleaseUpdateCount = githubOverview.preReleaseUpdateCount
    val failedCount = githubOverview.failedCount
    val cacheStateColor =
        when {
            githubOverview.refreshing -> colors.runningColor
            !githubOverview.loaded -> colors.inactiveColor
            githubOverview.cacheFreshness.stale -> AppStatusColors.Failed
            githubOverview.cacheFreshness.fresh -> AppStatusColors.Fresh
            cacheHitCount > 0 -> colors.githubCacheColor
            else -> colors.inactiveColor
        }
    val networkModeText =
        if (mcpOverview.allowExternal) {
            text.networkLanShort
        } else {
            text.networkLocalOnlyShort
        }
    val mcpRuntimeText =
        if (!mcpOverview.running || mcpOverview.runningSinceEpochMs <= 0L) {
            text.mcpRuntimePending
        } else {
            text.uptime(runtimeNowMs - mcpOverview.runningSinceEpochMs)
        }
    val mcpStatusText =
        if (mcpOverview.running) {
            text.mcpStatusRunning
        } else {
            text.mcpStatusStopped
        }
    val mcpTokenStatusText =
        if (mcpOverview.authTokenConfigured) {
            mcpOverview.authTokenPreview.ifBlank { text.commonFilled }
        } else {
            text.commonNotUsed
        }
    val githubRefreshIntervalLine = text.shortHours(githubOverview.refreshIntervalHours.coerceAtLeast(1))
    val githubStrategyLine =
        when (githubOverview.strategy) {
            GitHubLookupStrategyOption.AtomFeed -> text.githubStrategyAtom
            GitHubLookupStrategyOption.GitHubApiToken -> text.githubStrategyApi
        }
    val cacheRefreshLine =
        formatGitHubCacheAgo(
            lastRefreshMs = githubOverview.cachedRefreshMs,
            notRefreshedText = text.githubNotRefreshed,
            justNowText = text.justNow,
            nowMs = maxOf(runtimeNowMs, githubOverview.cacheLabelNowMs),
        )
    val githubRefreshProgressLine =
        text.githubRefreshingProgress(
            completed = githubOverview.refreshCompletedCount,
            target = githubOverview.refreshTargetCount.coerceAtLeast(1),
            totalTracked = githubOverview.refreshTotalTrackedCount.coerceAtLeast(trackedCount),
        )
    val githubLastUpdateLine =
        when {
            !githubOverview.loaded -> text.loading
            githubOverview.refreshing -> githubRefreshProgressLine
            trackedCount == 0 -> text.githubUnconfigured
            cacheHitCount == 0 -> text.refreshPair(githubRefreshIntervalLine, text.githubNoCache)
            else -> text.refreshPair(githubRefreshIntervalLine, cacheRefreshLine)
        }
    val githubFocusLine =
        when {
            !githubOverview.loaded -> text.loading
            githubOverview.refreshing -> githubRefreshProgressLine
            githubOverview.pendingShareImport -> text.githubSharePending
            trackedCount == 0 -> text.githubUnconfigured
            cacheHitCount == 0 -> text.githubPendingRefresh
            failedCount > 0 -> text.failedCount(failedCount)
            else -> text.githubCount(updatableCount + preReleaseUpdateCount)
        }
    val githubShareLine =
        when {
            !githubOverview.loaded -> text.loading
            githubOverview.pendingShareImport -> text.githubSharePending
            else -> text.valueOn
        }
    val webDavStatusLine =
        when {
            !webDavOverview.configured -> text.webDavUnconfigured
            webDavOverview.autoSyncFailed -> text.webDavAutoSyncFailed
            webDavOverview.autoSyncNeedsReview -> text.webDavAutoSyncNeedsReview
            webDavOverview.autoSyncEnabled -> text.webDavAutoSync
            else -> text.webDavManualSync
        }
    val webDavSyncItemsLine =
        text.fraction(webDavOverview.enabledItemCount, webDavOverview.totalItemCount)
    val webDavLastFullSyncLine =
        webDavOverview.lastFullSyncTimeMs
            .takeIf { it > 0L }
            ?.let(::formatHomeWebDavSyncTime)
            ?: text.webDavNeverSynced
    val webDavLastAutoSyncLine =
        webDavOverview.lastAutoSyncTimeMs
            .takeIf { it > 0L }
            ?.let(::formatHomeWebDavSyncTime)
            ?: text.webDavNeverSynced
    val baApLine =
        if (baOverview.loaded) {
            text.fraction(baOverview.apCurrent, baOverview.apLimit)
        } else {
            text.loading
        }
    val baCafeApLine =
        if (baOverview.loaded) {
            text.cafeFraction(baOverview.cafeLevel, baOverview.cafeStored, baOverview.cafeCap)
        } else {
            text.loading
        }
    val baActivationLine =
        if (baOverview.loaded) {
            if (baOverview.activated) text.baStatusActive else text.baStatusInactive
        } else {
            text.loading
        }
    val baAccountsLine =
        if (baOverview.loaded) {
            text.fraction(baOverview.enabledAccountCount, baOverview.accountCount)
        } else {
            text.loading
        }
    val baActiveAccountLine =
        if (baOverview.loaded) {
            baOverview.activeAccountName.ifBlank { baActivationLine }
        } else {
            text.loading
        }
    val baServerLine =
        if (baOverview.loaded) {
            when (baOverview.serverIndex.coerceIn(0, 2)) {
                0 -> text.baServerCn
                1 -> text.baServerGlobal
                else -> text.baServerJp
            }
        } else {
            text.loading
        }
    val baNotifyLine =
        if (baOverview.loaded) {
            if (baOverview.apNotifyEnabled) {
                text.thresholdPlus(baOverview.apNotifyThreshold)
            } else {
                text.valueOff
            }
        } else {
            text.loading
        }
    val privilegeGranted = privilegeStatus.isCommandReady
    return HomePageContentState(
        homeNa = text.commonNa,
        homeAppName = text.appName,
        homeTagline = text.tagline,
        homeStatusMcp = text.mcpTitle,
        homeStatusGitHub = text.githubTitle,
        homeStatusWebDav = text.webDavTitle,
        homeStatusPrivilege = text.privilegeTitle(privilegeStatus.mode),
        homeCardMcp = text.mcpCardTitle,
        homeCardGitHub = text.githubCardTitle,
        homeCardWebDav = text.webDavCardTitle,
        homeCardBa = text.baCardTitle,
        privilegeGranted = privilegeGranted,
        runningColor = colors.runningColor,
        stoppedColor = colors.stoppedColor,
        inactiveColor = colors.inactiveColor,
        cacheStateColor = cacheStateColor,
        appVersionText = homeAppVersionText(appOverview, text),
        githubFocusLine = githubFocusLine,
        mcpStatusText = mcpStatusText,
        mcpRuntimeText = mcpRuntimeText,
        networkModeText = networkModeText,
        homeStatToken = text.statToken,
        mcpTokenStatusText = mcpTokenStatusText,
        homeStatStableUpdates = text.statStableUpdates,
        homeStatPreReleaseUpdates = text.statPreReleaseUpdates,
        homeStatFailed = text.statFailed,
        homeStatTracked = text.statTracked,
        homeStatActions = text.statActions,
        homeStatPreciseVersion = text.statPreciseVersion,
        homeStatCached = text.statCached,
        homeStatCacheState = text.statCacheState,
        githubCacheFreshnessLine = homeCacheFreshnessLine(githubOverview.cacheFreshness, text),
        homeStatShare = text.statShare,
        githubShareLine = githubShareLine,
        homeStatLastUpdate = text.statLastUpdate,
        githubLastUpdateLine = githubLastUpdateLine,
        githubStrategyLine = githubStrategyLine,
        webDavStatusLine = webDavStatusLine,
        webDavSyncItemsLine = webDavSyncItemsLine,
        webDavLastAutoSyncLine = webDavLastAutoSyncLine,
        webDavLastFullSyncLine = webDavLastFullSyncLine,
        homeStatSyncItems = text.statSyncItems,
        homeStatLastAutoSync = text.statLastAutoSync,
        homeStatLastFullSync = text.statLastFullSync,
        baActivationLine = baActivationLine,
        homeStatAp = text.statAp,
        baApLine = baApLine,
        homeStatCafeAp = text.statCafeAp,
        baCafeApLine = baCafeApLine,
        homeStatBaAccounts = text.statBaAccounts,
        baAccountsLine = baAccountsLine,
        homeStatBaActiveAccount = text.statBaActiveAccount,
        baActiveAccountLine = baActiveAccountLine,
        homeStatBaServer = text.statBaServer,
        baServerLine = baServerLine,
        homeStatBaNotify = text.statBaNotify,
        baNotifyLine = baNotifyLine,
        baCacheFreshnessLine = homeCacheFreshnessLine(baOverview.cacheFreshness, text),
    )
}

private fun formatHomeWebDavSyncTime(timeMs: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timeMs))

private fun homeCacheFreshnessLine(
    freshness: CacheFreshnessSnapshot,
    text: HomePageContentTextBundle,
): String =
    when {
        freshness.fresh -> text.cacheStateFresh
        freshness.stale -> text.cacheStateStale
        else -> text.cacheStateEmpty
    }

private fun homeAppVersionText(
    appOverview: HomeAppOverview,
    text: HomePageContentTextBundle,
): String {
    if (!appOverview.loaded) return text.appVersionUnknown
    val versionName = appOverview.versionName.ifBlank { text.appVersionUnknownFallback }
    return "v$versionName (${appOverview.versionCode})"
}
