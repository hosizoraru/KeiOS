package com.example.keios.ui.page.main

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.net.Uri
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.keios.R
import com.example.keios.core.background.AppBackgroundScheduler
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SearchBarHost
import com.example.keios.ui.page.main.widget.SheetActionGroup
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetControlRow
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetFieldBlock
import com.example.keios.ui.page.main.widget.SheetInputTitle
import com.example.keios.ui.page.main.widget.SheetSectionCard
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.ui.page.main.widget.capturePopupAnchor
import com.example.keios.feature.github.data.local.AppIconCache
import com.example.keios.feature.github.data.local.GitHubReleaseAssetCacheStore
import com.example.keios.feature.github.data.local.GitHubTrackSnapshot
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubApiTokenReleaseStrategy
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetRepository
import com.example.keios.feature.github.data.remote.GitHubReleaseStrategyRegistry
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.notification.GitHubRefreshNotificationHelper
import com.example.keios.feature.github.domain.GitHubReleaseCheckService
import com.example.keios.feature.github.domain.GitHubStrategyBenchmarkService
import com.example.keios.feature.github.model.GitHubApiAuthMode
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubCheckCacheEntry
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubStrategyLoadTrace
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.feature.github.model.InstalledAppItem
import com.example.keios.ui.page.main.github.asset.*
import com.example.keios.ui.page.main.github.query.*
import com.example.keios.ui.page.main.github.sheet.*
import com.example.keios.ui.page.main.github.state.*
import com.example.keios.ui.page.main.github.section.*
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.keios.ui.page.main.widget.LiquidDropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import com.example.keios.ui.page.main.widget.LiquidDropdownColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.max


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitHubPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    cardPressFeedbackEnabled: Boolean = true,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    enableSearchBar: Boolean = true,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val openLinkFailureMessage = context.getString(R.string.github_error_open_link)
    val systemDmOption = remember(context) { systemDownloadManagerOption(context) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surface
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val topBarBackdrop: LayerBackdrop = key("github-topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val contentBackdrop: LayerBackdrop = key("github-content-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val sheetBackdrop: LayerBackdrop = key("github-sheet-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    var trackedSearch by remember { mutableStateOf("") }
    var repoUrlInput by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }
    var pickerExpanded by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showStrategySheet by remember { mutableStateOf(false) }
    var showCheckLogicSheet by remember { mutableStateOf(false) }
    var showDownloaderPopup by remember { mutableStateOf(false) }
    var editingTrackedItem by remember { mutableStateOf<GitHubTrackedApp?>(null) }
    var preferPreReleaseInput by remember { mutableStateOf(false) }
    var alwaysShowLatestReleaseDownloadButtonInput by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var appListLoaded by remember { mutableStateOf(false) }
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }
    var showSortPopup by remember { mutableStateOf(false) }
    var showCheckLogicIntervalPopup by remember { mutableStateOf(false) }
    var showOnlineShareTargetPopup by remember { mutableStateOf(false) }
    var checkLogicIntervalPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var downloaderPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var onlineShareTargetPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var sortMode by remember { mutableStateOf(GitHubSortMode.UpdateFirst) }
    var pendingDeleteItem by remember { mutableStateOf<GitHubTrackedApp?>(null) }
    var overviewRefreshState by remember { mutableStateOf(OverviewRefreshState.Idle) }
    var lastRefreshMs by remember { mutableStateOf(0L) }
    var refreshIntervalHours by remember { mutableStateOf(3) }
    var refreshProgress by remember { mutableStateOf(0f) }
    var lookupConfig by remember { mutableStateOf(GitHubLookupConfig()) }
    var selectedStrategyInput by remember { mutableStateOf(GitHubLookupStrategyOption.AtomFeed) }
    var githubApiTokenInput by remember { mutableStateOf("") }
    var checkAllTrackedPreReleasesInput by remember { mutableStateOf(false) }
    var aggressiveApkFilteringInput by remember { mutableStateOf(false) }
    var onlineShareTargetPackageInput by remember { mutableStateOf("") }
    var preferredDownloaderPackageInput by remember { mutableStateOf("") }
    var refreshIntervalHoursInput by remember { mutableStateOf(refreshIntervalHours) }
    var showApiTokenPlainText by remember { mutableStateOf(false) }
    var strategyBenchmarkRunning by remember { mutableStateOf(false) }
    var strategyBenchmarkError by remember { mutableStateOf<String?>(null) }
    var strategyBenchmarkReport by remember { mutableStateOf<com.example.keios.feature.github.model.GitHubStrategyBenchmarkReport?>(null) }
    var credentialCheckRunning by remember { mutableStateOf(false) }
    var credentialCheckError by remember { mutableStateOf<String?>(null) }
    var credentialCheckStatus by remember { mutableStateOf<GitHubApiCredentialStatus?>(null) }
    var recommendedTokenGuideExpanded by remember { mutableStateOf(false) }
    var refreshAllJob by remember { mutableStateOf<Job?>(null) }
    var deleteInProgress by remember { mutableStateOf(false) }
    var showFloatingAddButton by remember { mutableStateOf(true) }
    var showSearchBar by remember { mutableStateOf(true) }
    var searchBarHideOffsetPx by remember { mutableStateOf(0f) }
    val searchBarHideThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }
    val addButtonScrollConnection = remember(searchBarHideThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) {
                    showFloatingAddButton = false
                    if (showSearchBar) {
                        searchBarHideOffsetPx = (searchBarHideOffsetPx + (-available.y)).coerceAtMost(searchBarHideThresholdPx)
                        if (searchBarHideOffsetPx >= searchBarHideThresholdPx) {
                            showSearchBar = false
                            searchBarHideOffsetPx = 0f
                        }
                    }
                }
                if (available.y > 1f) {
                    showFloatingAddButton = true
                    showSearchBar = true
                    searchBarHideOffsetPx = 0f
                }
                return Offset.Zero
            }
        }
    }

    val trackedItems = remember { mutableStateListOf<GitHubTrackedApp>() }
    val checkStates = remember { mutableStateMapOf<String, VersionCheckUi>() }
    val apkAssetBundles = remember { mutableStateMapOf<String, GitHubReleaseAssetBundle>() }
    val apkAssetLoading = remember { mutableStateMapOf<String, Boolean>() }
    val apkAssetErrors = remember { mutableStateMapOf<String, String>() }
    val apkAssetExpanded = remember { mutableStateMapOf<String, Boolean>() }
    val installedOnlineShareTargets = remember(appListLoaded, appList) {
        queryOnlineShareTargetOptions(context, appList)
    }

    val trackSnapshot by produceState(initialValue = GitHubTrackSnapshot()) {
        value = withContext(Dispatchers.IO) { GitHubTrackStore.loadSnapshot() }
    }

    LaunchedEffect(trackSnapshot) {
        val activeStrategyId = trackSnapshot.lookupConfig.selectedStrategy.storageId
        lookupConfig = trackSnapshot.lookupConfig
        selectedStrategyInput = trackSnapshot.lookupConfig.selectedStrategy
        githubApiTokenInput = trackSnapshot.lookupConfig.apiToken
        checkAllTrackedPreReleasesInput = trackSnapshot.lookupConfig.checkAllTrackedPreReleases
        aggressiveApkFilteringInput = trackSnapshot.lookupConfig.aggressiveApkFiltering
        onlineShareTargetPackageInput = trackSnapshot.lookupConfig.onlineShareTargetPackage
        preferredDownloaderPackageInput = trackSnapshot.lookupConfig.preferredDownloaderPackage
        refreshIntervalHours = trackSnapshot.refreshIntervalHours
        refreshIntervalHoursInput = trackSnapshot.refreshIntervalHours

        trackedItems.clear()
        trackedItems.addAll(trackSnapshot.items)

        val cachedStates = trackSnapshot.checkCache
        val cachedRefreshMs = trackSnapshot.lastRefreshMs
        checkStates.clear()
        trackSnapshot.items.forEach { item ->
            cachedStates[item.id]
                ?.takeIf { cache ->
                    val sourceId = cache.sourceStrategyId.ifBlank { GitHubLookupStrategyOption.AtomFeed.storageId }
                    sourceId == activeStrategyId
                }
                ?.let { cached ->
                    checkStates[item.id] = VersionCheckUi(
                        loading = false,
                        localVersion = cached.localVersion,
                        localVersionCode = cached.localVersionCode,
                        latestTag = cached.latestTag,
                        latestStableName = cached.latestStableName,
                        latestStableRawTag = cached.latestStableRawTag,
                        latestStableUrl = cached.latestStableUrl,
                        latestStableUpdatedAtMillis = cached.latestStableUpdatedAtMillis,
                        latestPreName = cached.latestPreName,
                        latestPreRawTag = cached.latestPreRawTag,
                        latestPreUrl = cached.latestPreUrl,
                        latestPreUpdatedAtMillis = cached.latestPreUpdatedAtMillis,
                        hasStableRelease = cached.hasStableRelease,
                        hasUpdate = cached.hasUpdate,
                        message = cached.message,
                        isPreRelease = cached.isPreRelease,
                        preReleaseInfo = cached.preReleaseInfo,
                        showPreReleaseInfo = cached.showPreReleaseInfo,
                        hasPreReleaseUpdate = cached.hasPreReleaseUpdate,
                        recommendsPreRelease = cached.recommendsPreRelease,
                        releaseHint = cached.releaseHint,
                        sourceStrategyId = cached.sourceStrategyId
                    )
                }
        }
        lastRefreshMs = cachedRefreshMs

        val hasTracked = trackSnapshot.items.isNotEmpty()
        val hasCachedForTracked = trackSnapshot.items.any { item ->
            cachedStates[item.id]?.let { cache ->
                val sourceId = cache.sourceStrategyId.ifBlank { GitHubLookupStrategyOption.AtomFeed.storageId }
                sourceId == activeStrategyId
            } == true
        }
        overviewRefreshState = when {
            hasCachedForTracked -> OverviewRefreshState.Cached
            hasTracked -> OverviewRefreshState.Refreshing
            else -> OverviewRefreshState.Idle
        }
    }

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }

    LaunchedEffect(installedOnlineShareTargets) {
        if (onlineShareTargetPackageInput.isNotBlank() && installedOnlineShareTargets.none {
                it.packageName == onlineShareTargetPackageInput
            }) {
            onlineShareTargetPackageInput = ""
        }
        if (lookupConfig.onlineShareTargetPackage.isNotBlank() && installedOnlineShareTargets.none {
                it.packageName == lookupConfig.onlineShareTargetPackage
            }) {
            val updatedConfig = lookupConfig.copy(onlineShareTargetPackage = "")
            GitHubTrackStore.saveLookupConfig(updatedConfig)
            lookupConfig = updatedConfig
        }
    }

    fun persistCheckCache(refreshTimestamp: Long = lastRefreshMs) {
        val states = trackedItems.associate { item ->
            val state = checkStates[item.id] ?: VersionCheckUi()
            item.id to state.toCacheEntry()
        }
        GitHubTrackStore.saveCheckCache(states, refreshTimestamp)
    }

    fun cancelRefreshAll(reason: String? = null) {
        if (refreshAllJob?.isActive == true) {
            refreshAllJob?.cancel()
            refreshAllJob = null
            val trackedCount = trackedItems.size
            if (trackedCount > 0) {
                val checkedCount = (refreshProgress * trackedCount.toFloat()).toInt()
                    .coerceIn(0, trackedCount)
                val updatableCount = trackedItems.count { checkStates[it.id]?.hasUpdate == true }
                val failedCount = trackedItems.count {
                    checkStates[it.id]?.failed == true
                }
                GitHubRefreshNotificationHelper.notifyCancelled(
                    context = context,
                    current = checkedCount,
                    total = trackedCount,
                    trackedCount = trackedCount,
                    updatableCount = updatableCount,
                    failedCount = failedCount
                )
            } else {
                GitHubRefreshNotificationHelper.cancel(context)
            }
            overviewRefreshState = if (trackedItems.isEmpty()) {
                OverviewRefreshState.Idle
            } else if (checkStates.isNotEmpty()) {
                OverviewRefreshState.Cached
            } else {
                OverviewRefreshState.Idle
            }
            refreshProgress = 0f
            reason?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun activeStrategyId(): String = lookupConfig.selectedStrategy.storageId

    fun cacheMatchesCurrentStrategy(state: GitHubCheckCacheEntry): Boolean {
        val sourceId = state.sourceStrategyId.ifBlank { GitHubLookupStrategyOption.AtomFeed.storageId }
        return sourceId == activeStrategyId()
    }

    fun openStrategySheet() {
        val config = GitHubTrackStore.loadLookupConfig()
        lookupConfig = config
        selectedStrategyInput = config.selectedStrategy
        githubApiTokenInput = config.apiToken
        showApiTokenPlainText = false
        credentialCheckRunning = false
        credentialCheckError = null
        credentialCheckStatus = null
        strategyBenchmarkError = null
        strategyBenchmarkReport = null
        recommendedTokenGuideExpanded = false
        showStrategySheet = true
    }

    fun closeStrategySheet() {
        showStrategySheet = false
        showApiTokenPlainText = false
        credentialCheckRunning = false
        recommendedTokenGuideExpanded = false
    }

    fun openCheckLogicSheet() {
        val config = GitHubTrackStore.loadLookupConfig()
        lookupConfig = config
        checkAllTrackedPreReleasesInput = config.checkAllTrackedPreReleases
        aggressiveApkFilteringInput = config.aggressiveApkFiltering
        onlineShareTargetPackageInput = config.onlineShareTargetPackage
        preferredDownloaderPackageInput = config.preferredDownloaderPackage
        refreshIntervalHoursInput = GitHubTrackStore.loadRefreshIntervalHours()
        showCheckLogicIntervalPopup = false
        showDownloaderPopup = false
        showOnlineShareTargetPopup = false
        showCheckLogicSheet = true
    }

    fun closeCheckLogicSheet() {
        showCheckLogicIntervalPopup = false
        showDownloaderPopup = false
        showOnlineShareTargetPopup = false
        showCheckLogicSheet = false
    }

    suspend fun reloadApps(forceRefresh: Boolean = false) {
        appList = withContext(Dispatchers.IO) {
            GitHubVersionUtils.queryInstalledLaunchableApps(
                context = context,
                forceRefresh = forceRefresh
            )
        }
        withContext(Dispatchers.IO) {
            AppIconCache.preload(context, appList.map { it.packageName })
        }
        appListLoaded = true
    }

    suspend fun resolveItemState(item: GitHubTrackedApp): VersionCheckUi {
        return withContext(Dispatchers.IO) {
            GitHubReleaseCheckService.evaluateTrackedApp(context, item).toUi()
        }
    }

    fun refreshItem(item: GitHubTrackedApp, showToastOnError: Boolean = false) {
        scope.launch {
            checkStates[item.id] = VersionCheckUi(loading = true)
            val state = resolveItemState(item)
            if (trackedItems.none { it.id == item.id }) return@launch
            if (showToastOnError && state.failed) {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            checkStates[item.id] = state
            persistCheckCache()
        }
    }

    fun refreshAllTracked(showToast: Boolean = true) {
        val snapshot = trackedItems.toList()
        if (snapshot.isEmpty()) {
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_no_checkable_item),
                    Toast.LENGTH_SHORT
                ).show()
            }
            overviewRefreshState = OverviewRefreshState.Idle
            refreshProgress = 0f
            GitHubRefreshNotificationHelper.cancel(context)
            return
        }
        refreshAllJob?.cancel()
        refreshAllJob = scope.launch {
            GitHubTrackStore.clearCheckCache()
            lastRefreshMs = 0L
            overviewRefreshState = OverviewRefreshState.Refreshing
            refreshProgress = 0f
            val totalCount = snapshot.size
            var updatableCount = 0
            var failedCount = 0
            GitHubRefreshNotificationHelper.notifyProgress(
                context = context,
                current = 0,
                total = totalCount,
                trackedCount = totalCount,
                updatableCount = 0,
                failedCount = 0
            )
            snapshot.forEach { item ->
                checkStates[item.id] = VersionCheckUi(
                    loading = true,
                    message = context.getString(R.string.github_msg_checking)
                )
            }
            snapshot.forEachIndexed { index, item ->
                val state = resolveItemState(item)
                if (trackedItems.any { it.id == item.id }) {
                    checkStates[item.id] = state
                }
                if (state.hasUpdate == true) {
                    updatableCount += 1
                }
                if (state.failed) {
                    failedCount += 1
                }
                refreshProgress = (index + 1).toFloat() / snapshot.size.toFloat()
                GitHubRefreshNotificationHelper.notifyProgress(
                    context = context,
                    current = index + 1,
                    total = totalCount,
                    trackedCount = totalCount,
                    updatableCount = updatableCount,
                    failedCount = failedCount
                )
                if (showToast && state.failed) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.github_toast_repo_message,
                            item.owner,
                            item.repo,
                            state.message
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (index < snapshot.lastIndex) delay(120)
            }
            overviewRefreshState = OverviewRefreshState.Completed
            lastRefreshMs = System.currentTimeMillis()
            refreshProgress = 1f
            persistCheckCache(lastRefreshMs)
            GitHubRefreshNotificationHelper.notifyCompleted(
                context = context,
                total = totalCount,
                trackedCount = totalCount,
                updatableCount = updatableCount,
                failedCount = failedCount
            )
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_check_completed),
                    Toast.LENGTH_SHORT
                ).show()
            }
            refreshAllJob = null
        }
    }

    fun applyLookupConfig() {
        val previousConfig = GitHubTrackStore.loadLookupConfig()
        val sanitizedToken = githubApiTokenInput.trim()
        val newConfig = GitHubLookupConfig(
            selectedStrategy = selectedStrategyInput,
            apiToken = sanitizedToken,
            checkAllTrackedPreReleases = previousConfig.checkAllTrackedPreReleases,
            aggressiveApkFiltering = previousConfig.aggressiveApkFiltering,
            onlineShareTargetPackage = previousConfig.onlineShareTargetPackage
        )
        GitHubTrackStore.saveLookupConfig(newConfig)
        lookupConfig = newConfig
        closeStrategySheet()

        val strategyChanged = previousConfig.selectedStrategy != newConfig.selectedStrategy
        val activeTokenChanged = newConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken &&
            previousConfig.apiToken != newConfig.apiToken
        when {
            strategyChanged || activeTokenChanged -> {
                GitHubReleaseStrategyRegistry.clearAllCaches()
                GitHubTrackStore.clearCheckCache()
                checkStates.clear()
                lastRefreshMs = 0L
                refreshProgress = 0f
                overviewRefreshState = OverviewRefreshState.Idle
                if (trackedItems.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.github_toast_strategy_switched_recheck,
                            newConfig.selectedStrategy.label
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshAllTracked(showToast = true)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.github_toast_strategy_switched,
                            newConfig.selectedStrategy.label
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            previousConfig.apiToken != newConfig.apiToken -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_api_credential_saved),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_strategy_unchanged),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun applyCheckLogicSheet() {
        val previousConfig = GitHubTrackStore.loadLookupConfig()
        val previousRefreshIntervalHours = GitHubTrackStore.loadRefreshIntervalHours()
        val newConfig = previousConfig.copy(
            checkAllTrackedPreReleases = checkAllTrackedPreReleasesInput,
            aggressiveApkFiltering = aggressiveApkFilteringInput,
            onlineShareTargetPackage = onlineShareTargetPackageInput.trim().takeIf { selected ->
                installedOnlineShareTargets.any { it.packageName == selected }
            }.orEmpty(),
            preferredDownloaderPackage = preferredDownloaderPackageInput.trim()
        )
        GitHubTrackStore.saveLookupConfig(newConfig)
        GitHubTrackStore.saveRefreshIntervalHours(refreshIntervalHoursInput)
        lookupConfig = newConfig
        refreshIntervalHours = refreshIntervalHoursInput
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
        closeCheckLogicSheet()

        val checkScopeChanged =
            previousConfig.checkAllTrackedPreReleases != newConfig.checkAllTrackedPreReleases
        val filteringChanged = previousConfig.aggressiveApkFiltering != newConfig.aggressiveApkFiltering
        val intervalChanged = previousRefreshIntervalHours != refreshIntervalHoursInput
        when {
            checkScopeChanged || filteringChanged -> {
                GitHubTrackStore.clearCheckCache()
                checkStates.clear()
                apkAssetBundles.clear()
                apkAssetLoading.clear()
                apkAssetErrors.clear()
                apkAssetExpanded.clear()
                lastRefreshMs = 0L
                refreshProgress = 0f
                overviewRefreshState = OverviewRefreshState.Idle
                if (trackedItems.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.github_toast_check_logic_updated_recheck),
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshAllTracked(showToast = true)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.github_toast_check_logic_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            intervalChanged -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_refresh_interval_saved),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_check_logic_unchanged),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun runStrategyBenchmark() {
        if (strategyBenchmarkRunning) return
        val targets = GitHubStrategyBenchmarkService.buildTargets(trackedItems.toList())
        if (targets.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.github_toast_require_track_item),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        scope.launch {
            strategyBenchmarkRunning = true
            strategyBenchmarkError = null
            val benchmarkToken = githubApiTokenInput.trim()
            runCatching {
                withContext(Dispatchers.IO) {
                    GitHubStrategyBenchmarkService.compareTargets(
                        targets = targets,
                        apiToken = benchmarkToken
                    )
                }
            }.onSuccess { report ->
                strategyBenchmarkReport = report
            }.onFailure { error ->
                strategyBenchmarkError = error.message ?: "unknown"
            }
            strategyBenchmarkRunning = false
        }
    }

    fun runCredentialCheck() {
        if (credentialCheckRunning) return
        scope.launch {
            credentialCheckRunning = true
            credentialCheckError = null
            credentialCheckStatus = null
            try {
                val token = githubApiTokenInput.trim()
                val trace: GitHubStrategyLoadTrace<GitHubApiCredentialStatus> = withContext(Dispatchers.IO) {
                    GitHubApiTokenReleaseStrategy(token).checkCredentialTrace()
                }
                credentialCheckStatus = trace.result.getOrNull()
                credentialCheckError = trace.result.exceptionOrNull()?.message
            } finally {
                credentialCheckRunning = false
            }
        }
    }

    fun openExternalUrl(url: String, failureMessage: String = openLinkFailureMessage) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun resolvePreferredAssetUrl(asset: GitHubReleaseAssetFile): String {
        val token = lookupConfig.apiToken.trim()
        val preferApiAsset = lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken
        return withContext(Dispatchers.IO) {
            GitHubReleaseAssetRepository.resolvePreferredDownloadUrl(
                asset = asset,
                useApiAssetUrl = preferApiAsset,
                apiToken = token
            ).getOrElse { asset.downloadUrl }
        }
    }

    fun shareApkLink(asset: GitHubReleaseAssetFile) {
        scope.launch {
            val resolvedUrl = resolvePreferredAssetUrl(asset)
            val onlineSharePackage = lookupConfig.onlineShareTargetPackage.trim()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, asset.name)
                putExtra(Intent.EXTRA_TEXT, resolvedUrl)
                if (onlineSharePackage.isNotBlank()) {
                    `package` = onlineSharePackage
                    putExtra("channel", "Online")
                    putExtra("extra_channel", "Online")
                    putExtra("online_channel", true)
                }
            }
            runCatching {
                if (onlineSharePackage.isNotBlank()) {
                    context.startActivity(intent)
                } else {
                    context.startActivity(
                        Intent.createChooser(intent, context.getString(R.string.github_share_apk_link_title))
                    )
                }
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_share_link_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun enqueueWithSystemDownloadManager(url: String, fileName: String) {
        val request = DownloadManager.Request(url.toUri()).apply {
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            setTitle(fileName)
            setDescription(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: throw IllegalStateException("download manager unavailable")
        manager.enqueue(request)
    }

    fun openApkInDownloader(asset: GitHubReleaseAssetFile) {
        scope.launch {
            val resolvedUrl = resolvePreferredAssetUrl(asset)
            val preferredPackage = lookupConfig.preferredDownloaderPackage.trim()
            runCatching {
                when (preferredPackage) {
                    systemDmOption.packageName -> {
                        enqueueWithSystemDownloadManager(resolvedUrl, asset.name)
                        Toast.makeText(
                            context,
                            context.getString(R.string.github_toast_downloader_system_builtin),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    "" -> {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                            }
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.github_toast_downloader_system_default),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                setPackage(preferredPackage)
                            }
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.github_toast_downloader_selected),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.recoverCatching {
                if (preferredPackage.isNotBlank() && preferredPackage != systemDmOption.packageName) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                        }
                    )
                    Toast.makeText(
                        context,
                        context.getString(R.string.github_toast_downloader_fallback_system),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    throw it
                }
            }.onFailure {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_open_downloader_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun clearApkAssetUiState(itemId: String) {
        apkAssetExpanded.remove(itemId)
        apkAssetLoading.remove(itemId)
        apkAssetErrors.remove(itemId)
        apkAssetBundles.remove(itemId)
    }

    fun loadApkAssets(
        item: GitHubTrackedApp,
        state: VersionCheckUi,
        toggleOnlyWhenCached: Boolean = true,
        includeAllAssets: Boolean = false
    ) {
        val alwaysLatestRelease = item.alwaysShowLatestReleaseDownloadButton
        val target = state.apkAssetTarget(
            owner = item.owner,
            repo = item.repo,
            context = context,
            alwaysLatestRelease = alwaysLatestRelease
        )
        if (target == null) {
            val fallbackUrl = if (alwaysLatestRelease) {
                GitHubVersionUtils.buildReleaseUrl(item.owner, item.repo)
            } else {
                state.statusActionUrl(item.owner, item.repo)
            }
            if (fallbackUrl.isNotBlank()) {
                openExternalUrl(fallbackUrl)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_no_apk_to_load),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val cachedBundle = apkAssetBundles[item.id]
        if (
            toggleOnlyWhenCached &&
            cachedBundle != null &&
            cachedBundle.tagName.equals(target.rawTag, ignoreCase = true) &&
            cachedBundle.showingAllAssets == includeAllAssets
        ) {
            apkAssetExpanded[item.id] = !(apkAssetExpanded[item.id] ?: false)
            apkAssetErrors.remove(item.id)
            return
        }

        apkAssetExpanded[item.id] = true
        apkAssetLoading[item.id] = true
        apkAssetErrors.remove(item.id)
        scope.launch {
            val preferHtml = lookupConfig.selectedStrategy == GitHubLookupStrategyOption.AtomFeed
            val refreshIntervalHours = GitHubTrackStore.loadRefreshIntervalHours()
            val assetCacheKey = GitHubReleaseAssetCacheStore.buildCacheKey(
                owner = item.owner,
                repo = item.repo,
                rawTag = target.rawTag,
                releaseUrl = target.releaseUrl,
                preferHtml = preferHtml,
                aggressiveFiltering = lookupConfig.aggressiveApkFiltering,
                includeAllAssets = includeAllAssets,
                hasApiToken = lookupConfig.apiToken.isNotBlank()
            )
            val persistedBundle = withContext(Dispatchers.IO) {
                GitHubReleaseAssetCacheStore.load(
                    cacheKey = assetCacheKey,
                    refreshIntervalHours = refreshIntervalHours
                )
            }
            if (persistedBundle != null) {
                apkAssetLoading[item.id] = false
                apkAssetBundles[item.id] = persistedBundle
                apkAssetErrors[item.id] = if (persistedBundle.assets.isEmpty()) {
                    if (includeAllAssets) {
                        context.getString(
                            R.string.github_msg_assets_no_downloadable_except_source,
                            target.label
                        )
                    } else {
                        context.getString(
                            R.string.github_msg_assets_no_downloadable,
                            target.label
                        )
                    }
                } else {
                    ""
                }
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                GitHubReleaseAssetRepository.fetchApkAssets(
                    owner = item.owner,
                    repo = item.repo,
                    rawTag = target.rawTag,
                    releaseUrl = target.releaseUrl,
                    preferHtml = preferHtml,
                    aggressiveFiltering = lookupConfig.aggressiveApkFiltering,
                    includeAllAssets = includeAllAssets,
                    apiToken = lookupConfig.apiToken
                )
            }
            apkAssetLoading[item.id] = false
            result.onSuccess { bundle ->
                apkAssetBundles[item.id] = bundle
                scope.launch(Dispatchers.IO) {
                    GitHubReleaseAssetCacheStore.save(
                        cacheKey = assetCacheKey,
                        bundle = bundle
                    )
                }
                apkAssetErrors[item.id] = if (bundle.assets.isEmpty()) {
                    if (includeAllAssets) {
                        context.getString(
                            R.string.github_msg_assets_no_downloadable_except_source,
                            target.label
                        )
                    } else {
                        context.getString(
                            R.string.github_msg_assets_no_downloadable,
                            target.label
                        )
                    }
                } else {
                    ""
                }
            }.onFailure { error ->
                apkAssetErrors[item.id] = error.message
                    ?: context.getString(R.string.github_error_load_apk_assets_failed)
            }
        }
    }

    fun saveTracked() {
        GitHubTrackStore.save(trackedItems.toList())
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
    }

    fun openTrackSheetForAdd() {
        editingTrackedItem = null
        repoUrlInput = ""
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = false
        alwaysShowLatestReleaseDownloadButtonInput = false
        showAddSheet = true
    }

    fun openTrackSheetForEdit(item: GitHubTrackedApp) {
        editingTrackedItem = item
        repoUrlInput = item.repoUrl
        selectedApp = appList.firstOrNull { it.packageName == item.packageName }
            ?: InstalledAppItem(label = item.appLabel, packageName = item.packageName)
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = item.preferPreRelease
        alwaysShowLatestReleaseDownloadButtonInput = item.alwaysShowLatestReleaseDownloadButton
        showAddSheet = true
    }

    fun applyTrackSheet() {
        val app = selectedApp
        val parsed = GitHubVersionUtils.parseOwnerRepo(repoUrlInput)
        if (app == null || parsed == null) {
            Toast.makeText(
                context,
                context.getString(R.string.github_toast_fill_repo_and_select_app),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val newItem = GitHubTrackedApp(
            repoUrl = repoUrlInput.trim(),
            owner = parsed.first,
            repo = parsed.second,
            packageName = app.packageName,
            appLabel = app.label,
            preferPreRelease = preferPreReleaseInput,
            alwaysShowLatestReleaseDownloadButton = alwaysShowLatestReleaseDownloadButtonInput
        )
        val editing = editingTrackedItem
        if (editing == null) {
            if (trackedItems.any { it.id == newItem.id }) {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_track_exists),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            trackedItems.add(newItem)
            saveTracked()
            refreshItem(newItem, showToastOnError = true)
            Toast.makeText(
                context,
                context.getString(R.string.github_toast_track_added),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val duplicate = trackedItems.any { it.id == newItem.id && it.id != editing.id }
            if (duplicate) {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_track_exists),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            val index = trackedItems.indexOfFirst { it.id == editing.id }
            if (index >= 0) {
                trackedItems[index] = newItem
            } else {
                trackedItems.add(newItem)
            }
            if (editing.id != newItem.id) {
                checkStates.remove(editing.id)
            }
            saveTracked()
            refreshItem(newItem, showToastOnError = true)
            Toast.makeText(
                context,
                context.getString(R.string.github_toast_track_updated),
                Toast.LENGTH_SHORT
            ).show()
        }
        editingTrackedItem = null
        repoUrlInput = ""
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = false
        alwaysShowLatestReleaseDownloadButtonInput = false
        showAddSheet = false
    }

    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { reloadApps(forceRefresh = true) }
        }

    LaunchedEffect(Unit) {
        AppBackgroundScheduler.scheduleGitHubRefresh(context)
        reloadApps(forceRefresh = false)
        val hasTracked = trackedItems.isNotEmpty()
        val hasCachedForTracked = trackedItems.any { item ->
            checkStates.containsKey(item.id)
        }
        val stale = hasTracked && lastRefreshMs > 0L &&
            (System.currentTimeMillis() - lastRefreshMs) >= refreshIntervalHours * 60L * 60L * 1000L
        if (!hasCachedForTracked && hasTracked) {
            refreshAllTracked(showToast = false)
        } else if (stale) {
            refreshAllTracked(showToast = false)
        } else if (hasCachedForTracked) {
            overviewRefreshState = OverviewRefreshState.Cached
        } else {
            overviewRefreshState = OverviewRefreshState.Idle
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(appListLoaded, appList) {
        if (appListLoaded && appList.isEmpty() && !hasAutoRequestedPermission) {
            hasAutoRequestedPermission = true
            val intent = GitHubVersionUtils.buildAppListPermissionIntent(context)
            if (intent != null) {
                appListPermissionLauncher.launch(intent)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.github_toast_open_permission_page_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val filteredTracked = trackedItems.filter { item ->
        trackedSearch.isBlank() ||
            item.owner.contains(trackedSearch, ignoreCase = true) ||
            item.repo.contains(trackedSearch, ignoreCase = true) ||
            item.appLabel.contains(trackedSearch, ignoreCase = true) ||
            item.packageName.contains(trackedSearch, ignoreCase = true)
    }
    val isSortUpdatable: (GitHubTrackedApp) -> Boolean = { item ->
        item.alwaysShowLatestReleaseDownloadButton || checkStates[item.id]?.hasUpdate == true
    }
    val sortedTracked = when (sortMode) {
        GitHubSortMode.UpdateFirst -> filteredTracked.sortedWith(
            compareByDescending<GitHubTrackedApp> { isSortUpdatable(it) }
                .thenByDescending { checkStates[it.id]?.hasPreReleaseUpdate == true }
                .thenBy { it.appLabel.lowercase() }
        )
        GitHubSortMode.NameAsc -> filteredTracked.sortedBy { it.appLabel.lowercase() }
        GitHubSortMode.PreReleaseFirst -> filteredTracked.sortedWith(
            compareByDescending<GitHubTrackedApp> { checkStates[it.id]?.isPreRelease == true }
                .thenByDescending { isSortUpdatable(it) }
                .thenBy { it.appLabel.lowercase() }
        )
    }
    val trackedCount = trackedItems.size
    val updatableCount = trackedItems.count { checkStates[it.id]?.hasUpdate == true }
    val preReleaseCount = trackedItems.count { checkStates[it.id]?.isPreRelease == true }
    val preReleaseUpdateCount = trackedItems.count { checkStates[it.id]?.hasPreReleaseUpdate == true }
    val failedCount = trackedItems.count { checkStates[it.id]?.failed == true }
    val stableLatestCount = trackedItems.count {
        val s = checkStates[it.id]
        s?.hasUpdate == false && s.isPreRelease.not()
    }

    GitHubMainContent(
        contentBottomPadding = contentBottomPadding,
        listState = listState,
        scrollBehavior = scrollBehavior,
        addButtonScrollConnection = addButtonScrollConnection,
        topBarBackdrop = topBarBackdrop,
        contentBackdrop = contentBackdrop,
        topBarColor = topBarMaterialBackdrop.getMiuixAppBarColor(),
        enableSearchBar = enableSearchBar,
        liquidActionBarLayeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
        showSearchBar = showSearchBar,
        trackedSearch = trackedSearch,
        sortMode = sortMode,
        showSortPopup = showSortPopup,
        showFloatingAddButton = showFloatingAddButton,
        deleteInProgress = deleteInProgress,
        isDark = isDark,
        overviewRefreshState = overviewRefreshState,
        refreshProgress = refreshProgress,
        lastRefreshMs = lastRefreshMs,
        lookupConfig = lookupConfig,
        overviewMetrics = GitHubOverviewMetrics(
            trackedCount = trackedCount,
            updatableCount = updatableCount,
            stableLatestCount = stableLatestCount,
            preReleaseCount = preReleaseCount,
            preReleaseUpdateCount = preReleaseUpdateCount,
            failedCount = failedCount
        ),
        cardPressFeedbackEnabled = cardPressFeedbackEnabled,
        trackedItems = trackedItems,
        filteredTracked = filteredTracked,
        sortedTracked = sortedTracked,
        checkStates = checkStates,
        apkAssetBundles = apkAssetBundles,
        apkAssetLoading = apkAssetLoading,
        apkAssetErrors = apkAssetErrors,
        apkAssetExpanded = apkAssetExpanded,
        onTrackedSearchChange = { trackedSearch = it },
        onShowSortPopupChange = { showSortPopup = it },
        onSortModeChange = { sortMode = it },
        onOpenStrategySheet = { openStrategySheet() },
        onOpenCheckLogicSheet = { openCheckLogicSheet() },
        onRefreshAllTracked = { refreshAllTracked(showToast = true) },
        onOpenTrackSheetForAdd = { openTrackSheetForAdd() },
        onOpenTrackSheetForEdit = { openTrackSheetForEdit(it) },
        onClearApkAssetUiState = { clearApkAssetUiState(it) },
        onLoadApkAssets = { item, state, toggleOnlyWhenCached, includeAllAssets ->
            loadApkAssets(
                item = item,
                state = state,
                toggleOnlyWhenCached = toggleOnlyWhenCached,
                includeAllAssets = includeAllAssets
            )
        },
        onOpenExternalUrl = { openExternalUrl(it) },
        onOpenApkInDownloader = { openApkInDownloader(it) },
        onShareApkLink = { shareApkLink(it) },
        onActionBarInteractingChanged = onActionBarInteractingChanged
    )

    GitHubStrategySheet(
        show = showStrategySheet,
        backdrop = sheetBackdrop,
        lookupConfig = lookupConfig,
        selectedStrategyInput = selectedStrategyInput,
        githubApiTokenInput = githubApiTokenInput,
        showApiTokenPlainText = showApiTokenPlainText,
        credentialCheckRunning = credentialCheckRunning,
        credentialCheckError = credentialCheckError,
        credentialCheckStatus = credentialCheckStatus,
        strategyBenchmarkRunning = strategyBenchmarkRunning,
        strategyBenchmarkError = strategyBenchmarkError,
        strategyBenchmarkReport = strategyBenchmarkReport,
        trackedCount = trackedItems.size,
        recommendedTokenGuideExpanded = recommendedTokenGuideExpanded,
        onDismissRequest = { closeStrategySheet() },
        onApply = { applyLookupConfig() },
        onSelectedStrategyChange = { selectedStrategyInput = it },
        onTokenInputChange = {
            githubApiTokenInput = it
            credentialCheckError = null
            credentialCheckStatus = null
        },
        onToggleTokenVisibility = { showApiTokenPlainText = !showApiTokenPlainText },
        onRunCredentialCheck = { runCredentialCheck() },
        onRunStrategyBenchmark = { runStrategyBenchmark() },
        onRecommendedTokenGuideExpandedChange = { recommendedTokenGuideExpanded = it },
        onOpenExternalUrl = { url, failureMessage ->
            openExternalUrl(url = url, failureMessage = failureMessage)
        }
    )

    val checkLogicDownloaderOptions = remember(showCheckLogicSheet) {
        queryDownloaderOptions(context)
    }
    GitHubCheckLogicSheet(
        show = showCheckLogicSheet,
        backdrop = sheetBackdrop,
        lookupConfig = lookupConfig,
        refreshIntervalHours = refreshIntervalHours,
        refreshIntervalHoursInput = refreshIntervalHoursInput,
        checkAllTrackedPreReleasesInput = checkAllTrackedPreReleasesInput,
        aggressiveApkFilteringInput = aggressiveApkFilteringInput,
        onlineShareTargetPackageInput = onlineShareTargetPackageInput,
        preferredDownloaderPackageInput = preferredDownloaderPackageInput,
        installedOnlineShareTargets = installedOnlineShareTargets,
        showCheckLogicIntervalPopup = showCheckLogicIntervalPopup,
        showDownloaderPopup = showDownloaderPopup,
        showOnlineShareTargetPopup = showOnlineShareTargetPopup,
        checkLogicIntervalPopupAnchorBounds = checkLogicIntervalPopupAnchorBounds,
        downloaderPopupAnchorBounds = downloaderPopupAnchorBounds,
        onlineShareTargetPopupAnchorBounds = onlineShareTargetPopupAnchorBounds,
        downloaderOptions = checkLogicDownloaderOptions,
        onDismissRequest = { closeCheckLogicSheet() },
        onApply = { applyCheckLogicSheet() },
        onRefreshIntervalHoursInputChange = { refreshIntervalHoursInput = it },
        onCheckAllTrackedPreReleasesInputChange = { checkAllTrackedPreReleasesInput = it },
        onAggressiveApkFilteringInputChange = { aggressiveApkFilteringInput = it },
        onPreferredDownloaderPackageInputChange = { preferredDownloaderPackageInput = it },
        onOnlineShareTargetPackageInputChange = { onlineShareTargetPackageInput = it },
        onShowCheckLogicIntervalPopupChange = { showCheckLogicIntervalPopup = it },
        onShowDownloaderPopupChange = { showDownloaderPopup = it },
        onShowOnlineShareTargetPopupChange = { showOnlineShareTargetPopup = it },
        onCheckLogicIntervalPopupAnchorBoundsChange = { checkLogicIntervalPopupAnchorBounds = it },
        onDownloaderPopupAnchorBoundsChange = { downloaderPopupAnchorBounds = it },
        onOnlineShareTargetPopupAnchorBoundsChange = { onlineShareTargetPopupAnchorBounds = it }
    )

    GitHubTrackEditSheet(
        show = showAddSheet,
        backdrop = sheetBackdrop,
        editingTrackedItem = editingTrackedItem,
        repoUrlInput = repoUrlInput,
        appSearch = appSearch,
        pickerExpanded = pickerExpanded,
        selectedApp = selectedApp,
        appList = appList,
        preferPreReleaseInput = preferPreReleaseInput,
        alwaysShowLatestReleaseDownloadButtonInput = alwaysShowLatestReleaseDownloadButtonInput,
        onDismissRequest = {
            showAddSheet = false
            pickerExpanded = false
            appSearch = ""
            preferPreReleaseInput = false
            alwaysShowLatestReleaseDownloadButtonInput = false
            editingTrackedItem = null
        },
        onApply = { applyTrackSheet() },
        onRepoUrlInputChange = { repoUrlInput = it },
        onAppSearchChange = { appSearch = it },
        onPickerExpandedChange = { pickerExpanded = it },
        onSelectedAppChange = { selectedApp = it },
        onPreferPreReleaseInputChange = { preferPreReleaseInput = it },
        onAlwaysShowLatestReleaseDownloadButtonInputChange = {
            alwaysShowLatestReleaseDownloadButtonInput = it
        },
        onRequestDelete = {
            pendingDeleteItem = editingTrackedItem
            showAddSheet = false
            pickerExpanded = false
            appSearch = ""
            editingTrackedItem = null
        }
    )

    GitHubDeleteTrackDialog(
        pendingDeleteItem = pendingDeleteItem,
        deleteInProgress = deleteInProgress,
        onDismissRequest = { pendingDeleteItem = null },
        onCancel = {
            if (!deleteInProgress) pendingDeleteItem = null
        },
        onConfirmDelete = {
            if (deleteInProgress) return@GitHubDeleteTrackDialog
            pendingDeleteItem?.let { deleting ->
                deleteInProgress = true
                try {
                    cancelRefreshAll()
                    trackedItems.remove(deleting)
                    checkStates.remove(deleting.id)
                    saveTracked()
                    persistCheckCache()
                    Toast.makeText(
                        context,
                        context.getString(R.string.github_toast_track_deleted, deleting.appLabel),
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    deleteInProgress = false
                }
            }
            pendingDeleteItem = null
        }
    )
}
