package os.kei.feature.home.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.prefs.CacheFreshnessSnapshot
import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.domain.GitHubRefreshRuntimeState
import os.kei.feature.github.domain.GitHubRefreshRuntimeStore
import os.kei.feature.github.domain.GitHubTrackService
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.forTrackedItem
import os.kei.feature.github.model.isValidForTrackedItem
import os.kei.feature.home.model.HOME_BA_AP_MAX
import os.kei.feature.home.model.HOME_BA_CAFE_DAILY_AP_BY_LEVEL
import os.kei.feature.home.model.HomeAppOverview
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.feature.home.model.HomeOverviewSnapshot
import os.kei.feature.home.model.HomeWebDavOverview
import os.kei.feature.home.model.defaultHomeOverviewCards
import os.kei.feature.home.model.isHomeBaActivated
import os.kei.mcp.server.McpServerUiState
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BASettingsStoreSignals
import os.kei.ui.page.main.ba.support.BaAccountStoreSnapshot
import os.kei.ui.page.main.ba.support.BaCacheSnapshot
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import os.kei.ui.page.main.sync.WebDavAutoSyncStatus
import os.kei.ui.page.main.sync.WebDavSyncItem
import os.kei.ui.page.main.sync.WebDavSyncStore
import os.kei.ui.page.main.sync.WebDavSyncStoreSignals
import os.kei.ui.page.main.sync.requiresManualReview

private const val TAG = "HomeOverviewRepository"
private const val INITIAL_OVERVIEW_LOAD_REASON = "initial"

internal fun interface HomeOverviewClock {
    fun nowMs(): Long
}

internal object HomeSystemOverviewClock : HomeOverviewClock {
    override fun nowMs(): Long = System.currentTimeMillis()
}

internal class HomeOverviewRepository(
    private val context: Context,
    private val mcpUiState: StateFlow<McpServerUiState>,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.fileIo,
    private val clock: HomeOverviewClock = HomeSystemOverviewClock,
    val initialSnapshot: HomeOverviewSnapshot = HomeOverviewSnapshot(),
) {
    private val appContext = context.applicationContext
    private val refreshRequests =
        MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 1,
        )
    private val githubTrackService = GitHubTrackService(ioDispatcher)
    private val visibleOverviewCards = MutableStateFlow(initialSnapshot.visibleOverviewCards)
    private val showCacheFreshnessInCards = MutableStateFlow(initialSnapshot.showCacheFreshnessInCards)

    suspend fun loadInitialOverview(): HomeOverviewSnapshot {
        val stored = loadStoredOverview(INITIAL_OVERVIEW_LOAD_REASON)
        return HomeOverviewSnapshot(
            appOverview = stored.appOverview,
            mcpOverview = mcpUiState.value.toHomeOverview(),
            githubOverview = stored.githubOverview.withRefreshRuntime(GitHubRefreshRuntimeStore.state.value),
            webDavOverview = stored.webDavOverview,
            baOverview = stored.baOverview,
            visibleOverviewCards = visibleOverviewCards.value,
            showCacheFreshnessInCards = showCacheFreshnessInCards.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeOverview(): Flow<HomeOverviewSnapshot> {
        val storedOverviewFlow =
            buildHomeOverviewStoreRefreshFlow(
                refreshRequests = refreshRequests,
                githubVersions = githubTrackService.trackStoreSignalVersions(),
                baHomeOverviewVersions = BASettingsStoreSignals.homeOverviewVersion,
                webDavVersions = WebDavSyncStoreSignals.version,
            ).onStart { emit(INITIAL_OVERVIEW_LOAD_REASON) }
                .mapLatest { reason ->
                    loadStoredOverview(reason)
                }
        return combine(
            mcpUiState,
            storedOverviewFlow,
            GitHubRefreshRuntimeStore.state,
            visibleOverviewCards,
            showCacheFreshnessInCards,
        ) { mcpState, storedOverview, githubRefreshRuntime, visibleCards, showCacheFreshness ->
            HomeOverviewSnapshot(
                appOverview = storedOverview.appOverview,
                mcpOverview = mcpState.toHomeOverview(),
                githubOverview = storedOverview.githubOverview.withRefreshRuntime(githubRefreshRuntime),
                webDavOverview = storedOverview.webDavOverview,
                baOverview = storedOverview.baOverview,
                visibleOverviewCards = visibleCards,
                showCacheFreshnessInCards = showCacheFreshness,
            )
        }.distinctUntilChanged()
    }

    fun requestRefresh(reason: String) {
        refreshRequests.tryEmit(reason)
    }

    suspend fun setOverviewCardVisible(
        card: HomeOverviewCard,
        visible: Boolean,
    ) {
        val updated =
            visibleOverviewCards.value
                .toMutableSet()
                .apply {
                    if (visible) add(card) else remove(card)
                }.toSet()
        visibleOverviewCards.value = updated
        withContext(ioDispatcher) {
            HomeOverviewPrefs.saveVisibleOverviewCards(updated)
        }
    }

    suspend fun setCacheFreshnessVisibleInCards(visible: Boolean) {
        showCacheFreshnessInCards.value = visible
        withContext(ioDispatcher) {
            HomeOverviewPrefs.saveCacheFreshnessVisibleInCards(visible)
        }
    }

    private suspend fun loadStoredOverview(reason: String): StoredHomeOverview =
        withContext(ioDispatcher) {
            val visibleCards =
                runCatching { HomeOverviewPrefs.loadVisibleOverviewCards() }
                    .onFailure { error ->
                        AppLogger.w(
                            TAG,
                            "loadHomeVisibleOverviewCards failed (reason=$reason)",
                            error,
                        )
                    }.getOrElse { defaultHomeOverviewCards() }
            visibleOverviewCards.value = visibleCards
            showCacheFreshnessInCards.value = HomeOverviewPrefs.loadCacheFreshnessVisibleInCards()
            val nowMs = clock.nowMs()

            val baOverview =
                runCatching {
                    val accountState = BASettingsStore.loadAccountState()
                    val baSnapshot = BASettingsStore.loadSnapshot(accountState)
                    val serverIndex = baSnapshot.serverIndex
                    loadHomeBaOverview(
                        snapshot = baSnapshot,
                        accountState = accountState,
                        cacheFreshness =
                            buildHomeBaCalendarPoolCacheFreshness(
                                calendar = BASettingsStore.loadCalendarCacheSnapshot(serverIndex),
                                pool = BASettingsStore.loadPoolCacheSnapshot(serverIndex),
                                refreshIntervalHours = baSnapshot.calendarRefreshIntervalHours,
                                nowMs = nowMs,
                            ),
                    )
                }.onFailure { error ->
                    logOverviewLoadFailure("loadHomeBaOverview", reason, error)
                }.getOrElse { HomeBaOverview(loaded = true) }
            val githubOverview =
                runCatching {
                    val githubSnapshot = githubTrackService.loadHomeOverviewTrackSnapshot()
                    loadHomeGitHubOverview(
                        snapshot = githubSnapshot,
                        cacheFreshness = buildHomeGitHubCacheFreshness(githubSnapshot, nowMs),
                        nowMs = nowMs,
                    )
                }.onFailure { error ->
                    logOverviewLoadFailure("loadHomeGitHubOverview", reason, error)
                }.getOrElse { HomeGitHubOverview(loaded = true) }
            val webDavOverview =
                runCatching {
                    loadHomeWebDavOverview()
                }.onFailure { error ->
                    logOverviewLoadFailure("loadHomeWebDavOverview", reason, error)
                }.getOrElse { HomeWebDavOverview() }
            StoredHomeOverview(
                appOverview = loadHomeAppOverview(appContext),
                githubOverview = githubOverview,
                webDavOverview = webDavOverview,
                baOverview = baOverview,
            )
        }

    private fun McpServerUiState.toHomeOverview(): HomeMcpOverview =
        HomeMcpOverview(
            running = running,
            runningSinceEpochMs = runningSinceEpochMs,
            port = port,
            endpointPath = endpointPath,
            serverName = serverName,
            authTokenConfigured = authToken.isNotBlank(),
            authTokenPreview = buildTokenPreview(authToken),
            connectedClients = connectedClients,
            allowExternal = allowExternal,
        )

    private data class StoredHomeOverview(
        val appOverview: HomeAppOverview,
        val githubOverview: HomeGitHubOverview,
        val webDavOverview: HomeWebDavOverview,
        val baOverview: HomeBaOverview,
    )
}

private fun logOverviewLoadFailure(section: String, trigger: String, error: Throwable) {
    if (error is CancellationException) throw error
    AppLogger.w(
        TAG,
        "$section failed (trigger=$trigger)",
        error,
    )
}

private fun buildTokenPreview(token: String): String {
    val trimmed = token.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.length <= 4) return trimmed
    return "${trimmed.take(2)}…${trimmed.takeLast(2)}"
}

private fun loadHomeAppOverview(context: Context): HomeAppOverview =
    try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        HomeAppOverview(
            versionName = info.versionName.orEmpty(),
            versionCode = info.longVersionCode,
            loaded = true,
        )
    } catch (_: PackageManager.NameNotFoundException) {
        HomeAppOverview()
    } catch (_: RuntimeException) {
        HomeAppOverview()
    }

internal fun buildHomeOverviewStoreRefreshFlow(
    refreshRequests: Flow<String>,
    githubVersions: Flow<Long>,
    baHomeOverviewVersions: Flow<Long>,
    webDavVersions: Flow<Long>,
): Flow<String> =
    merge(
        refreshRequests,
        githubVersions
            .drop(1)
            .map { version -> "github_store_$version" },
        baHomeOverviewVersions
            .drop(1)
            .map { version -> "ba_store_$version" },
        webDavVersions
            .drop(1)
            .map { version -> "webdav_store_$version" },
    )

private fun HomeGitHubOverview.withRefreshRuntime(
    runtime: GitHubRefreshRuntimeState,
): HomeGitHubOverview {
    if (!runtime.running) {
        return copy(
            refreshing = false,
            refreshTargetCount = 0,
            refreshTotalTrackedCount = 0,
            refreshCompletedCount = 0,
            refreshProgress = 0f,
        )
    }
    return copy(
        refreshing = true,
        refreshScope = runtime.scope,
        refreshSource = runtime.source,
        refreshTargetCount = runtime.targetCount,
        refreshTotalTrackedCount = runtime.totalTrackedCount,
        refreshCompletedCount = runtime.completedCount,
        refreshProgress = runtime.progressFraction,
    )
}

internal fun loadHomeGitHubOverview(
    snapshot: GitHubTrackSnapshot,
    cacheFreshness: CacheFreshnessSnapshot,
    nowMs: Long,
): HomeGitHubOverview {
    val activeStrategyId = snapshot.lookupConfig.selectedStrategy.storageId
    var githubRepositoryCount = 0
    var gitRepositoryCount = 0
    var directApkCount = 0
    var fdroidRepositoryCount = 0
    var actionsTrackedCount = 0
    var preciseApkVersionCount = 0
    var cacheHitCount = 0
    var updatableCount = 0
    var preReleaseUpdateCount = 0
    var failedCount = 0
    snapshot.items.forEach { item ->
        when (item.sourceMode) {
            GitHubTrackedSourceMode.GitHubRepository -> githubRepositoryCount += 1
            GitHubTrackedSourceMode.GitRepository -> gitRepositoryCount += 1
            GitHubTrackedSourceMode.DirectApk -> directApkCount += 1
            GitHubTrackedSourceMode.FdroidRepository -> fdroidRepositoryCount += 1
        }
        if (item.checkActionsUpdates) actionsTrackedCount += 1
        val itemLookupConfig = snapshot.lookupConfig.forTrackedItem(item)
        if (itemLookupConfig.preciseApkVersionEnabled) preciseApkVersionCount += 1
        val cache =
            snapshot.checkCache[item.id]
                ?.takeIf { entry ->
                    entry.isValidForTrackedItem(
                        item = item,
                        lookupConfig = itemLookupConfig,
                        activeStrategyId = activeStrategyId,
                    )
        }
        if (cache != null) {
            cacheHitCount += 1
            if (cache.hasUpdate == true) updatableCount += 1
            if (cache.hasPreReleaseUpdate == true) preReleaseUpdateCount += 1
            if (GitHubTrackedReleaseStatus.isFailureMessage(cache.message)) {
                failedCount += 1
            }
        }
    }
    return HomeGitHubOverview(
        trackedCount = snapshot.items.size,
        githubRepositoryCount = githubRepositoryCount,
        gitRepositoryCount = gitRepositoryCount,
        directApkCount = directApkCount,
        fdroidRepositoryCount = fdroidRepositoryCount,
        actionsTrackedCount = actionsTrackedCount,
        preciseApkVersionCount = preciseApkVersionCount,
        cacheHitCount = cacheHitCount,
        updatableCount = updatableCount,
        preReleaseUpdateCount = preReleaseUpdateCount,
        failedCount = failedCount,
        strategy = snapshot.lookupConfig.selectedStrategy,
        apiTokenConfigured = snapshot.lookupConfig.apiToken.isNotBlank(),
        pendingShareImport = snapshot.pendingShareImportTrack != null,
        refreshIntervalHours = snapshot.refreshIntervalHours,
        cachedRefreshMs = if (cacheHitCount > 0) snapshot.lastRefreshMs else 0L,
        cacheLabelNowMs = nowMs,
        cacheFreshness = cacheFreshness,
        loaded = true,
    )
}

internal fun loadHomeBaOverview(
    snapshot: BaPageSnapshot,
    accountState: BaAccountStoreSnapshot,
    cacheFreshness: CacheFreshnessSnapshot,
): HomeBaOverview {
    val activated = isHomeBaActivated(snapshot.idFriendCode, snapshot.serverIndex)
    var enabledAccountCount = 0
    var activeAccountName = ""
    var activeAccountEnabled = false
    accountState.accounts.forEach { account ->
        if (account.profile.enabled) enabledAccountCount += 1
        if (account.profile.id == accountState.activeAccountId) {
            activeAccountName = account.profile.displayName
            activeAccountEnabled = account.profile.enabled
        }
    }
    val apCurrent = snapshot.apCurrent.coerceIn(0.0, HOME_BA_AP_MAX.toDouble()).toInt()
    val cafeLevel = snapshot.cafeLevel.coerceIn(1, HOME_BA_CAFE_DAILY_AP_BY_LEVEL.size)
    val cafeCap = HOME_BA_CAFE_DAILY_AP_BY_LEVEL[cafeLevel - 1]
    val cafeStored =
        snapshot.cafeStoredAp
            .coerceAtLeast(0.0)
            .toInt()
            .coerceAtMost(cafeCap)

    return HomeBaOverview(
        activated = activated,
        accountCount = accountState.accounts.size,
        enabledAccountCount = enabledAccountCount,
        activeAccountName = activeAccountName,
        activeAccountEnabled = activeAccountEnabled,
        serverIndex = snapshot.serverIndex,
        apCurrent = apCurrent,
        apLimit = snapshot.apLimit,
        apNotifyEnabled = snapshot.apNotifyEnabled,
        apNotifyThreshold = snapshot.apNotifyThreshold,
        cafeLevel = cafeLevel,
        cafeStored = cafeStored,
        cafeCap = cafeCap,
        cacheFreshness = cacheFreshness,
        loaded = true,
    )
}

internal fun buildHomeGitHubCacheFreshness(
    snapshot: GitHubTrackSnapshot,
    nowMs: Long = System.currentTimeMillis(),
): CacheFreshnessSnapshot =
    CacheFreshnessSnapshot.from(
        lastUpdatedAtMs = snapshot.lastRefreshMs,
        bytes = estimateHomeGitHubCacheBytes(snapshot),
        rebuildable = true,
        ttlMs = refreshIntervalMs(snapshot.refreshIntervalHours),
        nowMs = nowMs,
    )

private fun estimateHomeGitHubCacheBytes(snapshot: GitHubTrackSnapshot): Long {
    if (snapshot.checkCache.isEmpty() && snapshot.profileCache.isEmpty()) return 0L
    val checkBytes =
        snapshot.checkCache.values.sumOf { entry ->
            listOf(
                entry.localVersion,
                entry.latestTag,
                entry.latestStableName,
                entry.latestStableRawTag,
                entry.latestStableUrl,
                entry.latestStableAuthorAvatarUrl,
                entry.latestPreName,
                entry.latestPreRawTag,
                entry.latestPreUrl,
                entry.latestPreAuthorAvatarUrl,
                entry.message,
                entry.preReleaseInfo,
                entry.releaseHint,
                entry.sourceStrategyId,
                entry.sourceConfigSignature,
                entry.repositoryProfile?.identity?.fullName?.value.orEmpty(),
                entry.repositoryProfile?.sourceConfigSignature.orEmpty(),
            ).sumOf { value -> value.length.toLong() * 2L } + 64L
        }
    val profileBytes =
        snapshot.profileCache.values.sumOf { profile ->
            listOf(
                profile.identity.fullName?.value.orEmpty(),
                profile.sourceConfigSignature,
            ).sumOf { value -> value.length.toLong() * 2L } + 96L
        }
    return checkBytes + profileBytes + 16L
}

internal fun buildHomeBaCalendarPoolCacheFreshness(
    calendar: BaCacheSnapshot,
    pool: BaCacheSnapshot,
    refreshIntervalHours: Int,
    nowMs: Long = System.currentTimeMillis(),
): CacheFreshnessSnapshot =
    CacheFreshnessSnapshot.from(
        lastUpdatedAtMs = maxOf(calendar.syncMs, pool.syncMs),
        bytes = estimateHomeBaCalendarPoolCacheBytes(calendar, pool),
        rebuildable = true,
        ttlMs = refreshIntervalMs(refreshIntervalHours),
        nowMs = nowMs,
    )

private fun estimateHomeBaCalendarPoolCacheBytes(
    calendar: BaCacheSnapshot,
    pool: BaCacheSnapshot,
): Long {
    val rawBytes = listOf(calendar.raw, pool.raw).sumOf { raw -> raw.length.toLong() * 2L }
    return if (rawBytes > 0L) rawBytes + 32L else 0L
}

private fun refreshIntervalMs(hours: Int): Long = hours.coerceAtLeast(1) * 60L * 60L * 1000L

private fun loadHomeWebDavOverview(): HomeWebDavOverview =
    WebDavSyncStore.loadLastAutoSyncSummary().let { autoSummary ->
        val pendingReview =
            WebDavSyncItem.entries.any { item ->
                WebDavSyncStore.isItemEnabled(item) &&
                    WebDavSyncStore.loadItemPendingSummary(item)?.requiresManualReview == true
            }
        HomeWebDavOverview(
            configured = WebDavSyncStore.hasConfig(),
            autoSyncEnabled = WebDavSyncStore.isAutoSyncEnabled(),
            enabledItemCount = WebDavSyncItem.entries.count(WebDavSyncStore::isItemEnabled),
            totalItemCount = WebDavSyncItem.entries.size,
            lastFullSyncTimeMs = WebDavSyncStore.getLastFullSyncTime(),
            lastAutoSyncTimeMs = autoSummary?.finishedAtMs ?: 0L,
            autoSyncNeedsReview = autoSummary?.status == WebDavAutoSyncStatus.NeedsReview && pendingReview,
            autoSyncFailed = autoSummary?.status == WebDavAutoSyncStatus.Failed,
        )
    }
