package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.MiuixInfoItem
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
import com.example.keios.feature.github.data.local.GitHubTrackSnapshot
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubApiTokenReleaseStrategy
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetBundle
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetRepository
import com.example.keios.feature.github.data.remote.GitHubReleaseStrategyRegistry
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.domain.GitHubReleaseCheckService
import com.example.keios.feature.github.domain.GitHubStrategyBenchmarkService
import com.example.keios.feature.github.model.GitHubApiAuthMode
import com.example.keios.feature.github.model.GitHubCheckCacheEntry
import com.example.keios.feature.github.model.GitHubApiCredentialStatus
import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.feature.github.model.GitHubStrategyLoadTrace
import com.example.keios.feature.github.model.GitHubTrackedReleaseCheck
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.feature.github.model.InstalledAppItem
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
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.max


@Composable
fun GitHubPage(
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0,
    cardPressFeedbackEnabled: Boolean = true,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    var trackedSearch by remember { mutableStateOf("") }
    var repoUrlInput by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }
    var pickerExpanded by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showStrategySheet by remember { mutableStateOf(false) }
    var showCheckLogicSheet by remember { mutableStateOf(false) }
    var editingTrackedItem by remember { mutableStateOf<GitHubTrackedApp?>(null) }
    var preferPreReleaseInput by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var appListLoaded by remember { mutableStateOf(false) }
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }
    var showSortPopup by remember { mutableStateOf(false) }
    var showCheckLogicIntervalPopup by remember { mutableStateOf(false) }
    var checkLogicIntervalPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
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
    val addButtonScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) showFloatingAddButton = false
                if (available.y > 1f) showFloatingAddButton = true
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
                        latestPreName = cached.latestPreName,
                        latestPreRawTag = cached.latestPreRawTag,
                        latestPreUrl = cached.latestPreUrl,
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

    fun VersionCheckUi.toCacheEntry(): GitHubCheckCacheEntry = GitHubCheckCacheEntry(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = latestTag,
        latestStableName = latestStableName,
        latestStableRawTag = latestStableRawTag,
        latestStableUrl = latestStableUrl,
        latestPreName = latestPreName,
        latestPreRawTag = latestPreRawTag,
        latestPreUrl = latestPreUrl,
        hasStableRelease = hasStableRelease,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreRelease,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate,
        recommendsPreRelease = recommendsPreRelease,
        releaseHint = releaseHint,
        sourceStrategyId = sourceStrategyId
    )

    fun GitHubCheckCacheEntry.toUi(): VersionCheckUi = VersionCheckUi(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = latestTag,
        latestStableName = latestStableName,
        latestStableRawTag = latestStableRawTag,
        latestStableUrl = latestStableUrl,
        latestPreName = latestPreName,
        latestPreRawTag = latestPreRawTag,
        latestPreUrl = latestPreUrl,
        hasStableRelease = hasStableRelease,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreRelease,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate,
        recommendsPreRelease = recommendsPreRelease,
        releaseHint = releaseHint,
        sourceStrategyId = sourceStrategyId
    )

    fun GitHubTrackedReleaseCheck.toUi(): VersionCheckUi = VersionCheckUi(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = stableRelease?.displayVersion.orEmpty(),
        latestStableName = stableRelease?.rawName.orEmpty(),
        latestStableRawTag = stableRelease?.rawTag.orEmpty(),
        latestStableUrl = stableRelease?.link.orEmpty(),
        latestPreName = preRelease?.rawName.orEmpty(),
        latestPreRawTag = preRelease?.rawTag.orEmpty(),
        latestPreUrl = preRelease?.link.orEmpty(),
        hasStableRelease = hasStableRelease,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreReleaseInstalled,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate,
        recommendsPreRelease = recommendsPreRelease,
        releaseHint = releaseHint,
        sourceStrategyId = strategyId
    )

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
        refreshIntervalHoursInput = GitHubTrackStore.loadRefreshIntervalHours()
        showCheckLogicIntervalPopup = false
        showCheckLogicSheet = true
    }

    fun closeCheckLogicSheet() {
        showCheckLogicIntervalPopup = false
        showCheckLogicSheet = false
    }

    suspend fun reloadApps() {
        appList = withContext(Dispatchers.IO) {
            GitHubVersionUtils.queryInstalledLaunchableApps(context)
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
            if (showToastOnError && state.message.startsWith("检查失败")) {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            checkStates[item.id] = state
            persistCheckCache()
        }
    }

    fun refreshAllTracked(showToast: Boolean = true) {
        val snapshot = trackedItems.toList()
        if (snapshot.isEmpty()) {
            if (showToast) Toast.makeText(context, "暂无可检查条目", Toast.LENGTH_SHORT).show()
            overviewRefreshState = OverviewRefreshState.Idle
            refreshProgress = 0f
            return
        }
        refreshAllJob?.cancel()
        refreshAllJob = scope.launch {
            GitHubTrackStore.clearCheckCache()
            lastRefreshMs = 0L
            overviewRefreshState = OverviewRefreshState.Refreshing
            refreshProgress = 0f
            snapshot.forEach { item ->
                checkStates[item.id] = VersionCheckUi(loading = true, message = "检查中...")
            }
            snapshot.forEachIndexed { index, item ->
                val state = resolveItemState(item)
                if (trackedItems.any { it.id == item.id }) {
                    checkStates[item.id] = state
                }
                refreshProgress = (index + 1).toFloat() / snapshot.size.toFloat()
                if (showToast && state.message.startsWith("检查失败")) {
                    Toast.makeText(context, "${item.owner}/${item.repo}: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                if (index < snapshot.lastIndex) delay(120)
            }
            overviewRefreshState = OverviewRefreshState.Completed
            lastRefreshMs = System.currentTimeMillis()
            refreshProgress = 1f
            persistCheckCache(lastRefreshMs)
            if (showToast) Toast.makeText(context, "检查完成", Toast.LENGTH_SHORT).show()
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
            aggressiveApkFiltering = previousConfig.aggressiveApkFiltering
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
                        "已切换为 ${newConfig.selectedStrategy.label}，正在重新检查",
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshAllTracked(showToast = true)
                } else {
                    Toast.makeText(
                        context,
                        "已切换为 ${newConfig.selectedStrategy.label}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            previousConfig.apiToken != newConfig.apiToken -> {
                Toast.makeText(context, "已保存 API 凭证设置", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "抓取方案未变化", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun applyCheckLogicSheet() {
        val previousConfig = GitHubTrackStore.loadLookupConfig()
        val previousRefreshIntervalHours = GitHubTrackStore.loadRefreshIntervalHours()
        val newConfig = previousConfig.copy(
            checkAllTrackedPreReleases = checkAllTrackedPreReleasesInput,
            aggressiveApkFiltering = aggressiveApkFilteringInput
        )
        GitHubTrackStore.saveLookupConfig(newConfig)
        GitHubTrackStore.saveRefreshIntervalHours(refreshIntervalHoursInput)
        lookupConfig = newConfig
        refreshIntervalHours = refreshIntervalHoursInput
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
                    Toast.makeText(context, "已更新检查逻辑，正在重新检查", Toast.LENGTH_SHORT).show()
                    refreshAllTracked(showToast = true)
                } else {
                    Toast.makeText(context, "已保存检查逻辑", Toast.LENGTH_SHORT).show()
                }
            }
            intervalChanged -> {
                Toast.makeText(context, "已保存更新间隔", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "检查逻辑未变化", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun runStrategyBenchmark() {
        if (strategyBenchmarkRunning) return
        val targets = GitHubStrategyBenchmarkService.buildTargets(trackedItems.toList())
        if (targets.isEmpty()) {
            Toast.makeText(context, "请先新增至少一个跟踪项目", Toast.LENGTH_SHORT).show()
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

    fun openExternalUrl(url: String, failureMessage: String = "无法打开链接") {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        }
    }

    data class ApkAssetTarget(
        val rawTag: String,
        val releaseUrl: String,
        val label: String
    )

    fun VersionCheckUi.apkAssetTarget(owner: String, repo: String): ApkAssetTarget? {
        val stableTag = latestStableRawTag.ifBlank {
            GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latestStableUrl)
        }
        val preTag = latestPreRawTag.ifBlank {
            GitHubReleaseAssetRepository.parseReleaseTagFromUrl(latestPreUrl)
        }
        return when {
            recommendsPreRelease && preTag.isNotBlank() -> ApkAssetTarget(
                rawTag = preTag,
                releaseUrl = latestPreUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, preTag) },
                label = "预发 APK"
            )
            hasUpdate == true && stableTag.isNotBlank() -> ApkAssetTarget(
                rawTag = stableTag,
                releaseUrl = latestStableUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, stableTag) },
                label = "稳定 APK"
            )
            hasPreReleaseUpdate && preTag.isNotBlank() -> ApkAssetTarget(
                rawTag = preTag,
                releaseUrl = latestPreUrl.ifBlank { GitHubVersionUtils.buildReleaseTagUrl(owner, repo, preTag) },
                label = "预发 APK"
            )
            else -> null
        }
    }

    fun formatAssetSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "大小未知"
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L
        return when {
            sizeBytes >= gb -> String.format("%.1f GB", sizeBytes.toDouble() / gb.toDouble())
            sizeBytes >= mb -> String.format("%.1f MB", sizeBytes.toDouble() / mb.toDouble())
            sizeBytes >= kb -> String.format("%.0f KB", sizeBytes.toDouble() / kb.toDouble())
            else -> "$sizeBytes B"
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

    fun assetActionLabels(asset: GitHubReleaseAssetFile): Pair<String, String> {
        val useApiAsset = lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken &&
            lookupConfig.apiToken.trim().isNotBlank() &&
            asset.apiAssetUrl.isNotBlank()
        return if (useApiAsset) {
            "API 下载" to "API 分享"
        } else {
            "直链下载" to "直链分享"
        }
    }

    fun shareApkLink(asset: GitHubReleaseAssetFile) {
        scope.launch {
            val resolvedUrl = resolvePreferredAssetUrl(asset)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, asset.name)
                putExtra(Intent.EXTRA_TEXT, resolvedUrl)
            }
            runCatching {
                context.startActivity(Intent.createChooser(intent, "分享 APK 下载链接"))
            }.onFailure {
                Toast.makeText(context, "无法分享链接", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openApkInDownloader(asset: GitHubReleaseAssetFile) {
        scope.launch {
            val resolvedUrl = resolvePreferredAssetUrl(asset)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            runCatching {
                context.startActivity(intent)
                Toast.makeText(context, "已交给外部下载器处理", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "无法打开下载器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadApkAssets(item: GitHubTrackedApp, state: VersionCheckUi, toggleOnlyWhenCached: Boolean = true) {
        val target = state.apkAssetTarget(item.owner, item.repo)
        if (target == null) {
            val fallbackUrl = state.statusActionUrl(item.owner, item.repo)
            if (fallbackUrl.isNotBlank()) {
                openExternalUrl(fallbackUrl)
            } else {
                Toast.makeText(context, "当前没有可加载的更新 APK", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val cachedBundle = apkAssetBundles[item.id]
        if (toggleOnlyWhenCached && cachedBundle != null && cachedBundle.tagName.equals(target.rawTag, ignoreCase = true)) {
            apkAssetExpanded[item.id] = !(apkAssetExpanded[item.id] ?: false)
            apkAssetErrors.remove(item.id)
            return
        }

        apkAssetExpanded[item.id] = true
        apkAssetLoading[item.id] = true
        apkAssetErrors.remove(item.id)
        scope.launch {
            val preferHtml = lookupConfig.selectedStrategy == GitHubLookupStrategyOption.AtomFeed
            val result = withContext(Dispatchers.IO) {
                GitHubReleaseAssetRepository.fetchApkAssets(
                    owner = item.owner,
                    repo = item.repo,
                    rawTag = target.rawTag,
                    releaseUrl = target.releaseUrl,
                    preferHtml = preferHtml,
                    aggressiveFiltering = lookupConfig.aggressiveApkFiltering,
                    apiToken = lookupConfig.apiToken
                )
            }
            apkAssetLoading[item.id] = false
            result.onSuccess { bundle ->
                apkAssetBundles[item.id] = bundle
                apkAssetErrors[item.id] = if (bundle.assets.isEmpty()) {
                    "${target.label} 当前没有可直接下载的 APK 资产"
                } else {
                    ""
                }
            }.onFailure { error ->
                apkAssetErrors[item.id] = error.message ?: "加载 APK 资产失败"
            }
        }
    }

    fun saveTracked() {
        GitHubTrackStore.save(trackedItems.toList())
    }

    fun openTrackSheetForAdd() {
        editingTrackedItem = null
        repoUrlInput = ""
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = false
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
        showAddSheet = true
    }

    fun applyTrackSheet() {
        val app = selectedApp
        val parsed = GitHubVersionUtils.parseOwnerRepo(repoUrlInput)
        if (app == null || parsed == null) {
            Toast.makeText(context, "请填写正确仓库并选择 App", Toast.LENGTH_SHORT).show()
            return
        }
        val newItem = GitHubTrackedApp(
            repoUrl = repoUrlInput.trim(),
            owner = parsed.first,
            repo = parsed.second,
            packageName = app.packageName,
            appLabel = app.label,
            preferPreRelease = preferPreReleaseInput
        )
        val editing = editingTrackedItem
        if (editing == null) {
            if (trackedItems.any { it.id == newItem.id }) {
                Toast.makeText(context, "该条目已存在", Toast.LENGTH_SHORT).show()
                return
            }
            trackedItems.add(newItem)
            saveTracked()
            refreshItem(newItem, showToastOnError = true)
            Toast.makeText(context, "已新增跟踪", Toast.LENGTH_SHORT).show()
        } else {
            val duplicate = trackedItems.any { it.id == newItem.id && it.id != editing.id }
            if (duplicate) {
                Toast.makeText(context, "该条目已存在", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "已更新跟踪", Toast.LENGTH_SHORT).show()
        }
        editingTrackedItem = null
        repoUrlInput = ""
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        preferPreReleaseInput = false
        showAddSheet = false
    }

    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { reloadApps() }
        }

    LaunchedEffect(Unit) {
        reloadApps()
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
                Toast.makeText(context, "无法打开权限页面，请手动到系统设置授权", Toast.LENGTH_SHORT).show()
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
    val sortedTracked = when (sortMode) {
        GitHubSortMode.UpdateFirst -> filteredTracked.sortedWith(
            compareByDescending<GitHubTrackedApp> { checkStates[it.id]?.hasUpdate == true }
                .thenByDescending { checkStates[it.id]?.hasPreReleaseUpdate == true }
                .thenBy { it.appLabel.lowercase() }
        )
        GitHubSortMode.NameAsc -> filteredTracked.sortedBy { it.appLabel.lowercase() }
        GitHubSortMode.PreReleaseFirst -> filteredTracked.sortedWith(
            compareByDescending<GitHubTrackedApp> { checkStates[it.id]?.isPreRelease == true }
                .thenByDescending { checkStates[it.id]?.hasUpdate == true }
                .thenBy { it.appLabel.lowercase() }
        )
    }
    val trackedCount = trackedItems.size
    val updatableCount = trackedItems.count { checkStates[it.id]?.hasUpdate == true }
    val preReleaseCount = trackedItems.count { checkStates[it.id]?.isPreRelease == true }
    val preReleaseUpdateCount = trackedItems.count { checkStates[it.id]?.hasPreReleaseUpdate == true }
    val failedCount = trackedItems.count { checkStates[it.id]?.message?.startsWith("检查失败") == true }
    val stableLatestCount = trackedItems.count {
        val s = checkStates[it.id]
        s?.hasUpdate == false && s.isPreRelease.not()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = "",
                    largeTitle = "GitHub",
                    scrollBehavior = scrollBehavior,
                    color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                    actions = {
                        Box {
                            LiquidActionBar(
                                backdrop = backdrop,
                                items = listOf(
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Edit,
                                        contentDescription = "编辑抓取方案",
                                        onClick = { openStrategySheet() }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Tune,
                                        contentDescription = "检查逻辑",
                                        onClick = { openCheckLogicSheet() }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Sort,
                                        contentDescription = "排序",
                                        onClick = { showSortPopup = !showSortPopup }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Refresh,
                                        contentDescription = "检查",
                                        onClick = { refreshAllTracked(showToast = true) },
                                        enabled = !deleteInProgress
                                    )
                                ),
                                onInteractionChanged = onActionBarInteractingChanged
                            )

                            LiquidActionBarPopupAnchors(itemCount = 4) { slotIndex, popupAnchorBounds ->
                                when (slotIndex) {
                                    2 -> if (showSortPopup) {
                                        SnapshotWindowListPopup(
                                            show = showSortPopup,
                                            alignment = PopupPositionProvider.Align.BottomStart,
                                            anchorBounds = popupAnchorBounds,
                                            placement = SnapshotPopupPlacement.ActionBarCenter,
                                            onDismissRequest = { showSortPopup = false },
                                            enableWindowDim = false
                                        ) {
                                            LiquidDropdownColumn {
                                                val modes = GitHubSortMode.entries
                                                modes.forEachIndexed { index, mode ->
                                                    LiquidDropdownImpl(
                                                        text = mode.label,
                                                        optionSize = modes.size,
                                                        isSelected = sortMode == mode,
                                                        index = index,
                                                        onSelectedIndexChange = { selectedIndex ->
                                                            sortMode = modes[selectedIndex]
                                                            showSortPopup = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                GlassSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    value = trackedSearch,
                    onValueChange = { trackedSearch = it },
                    label = "搜索已跟踪项目（仓库/应用/包名）",
                    backdrop = backdrop,
                    variant = GlassVariant.Bar,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(addButtonScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 64.dp,
                    start = 12.dp,
                    end = 12.dp
                )
            ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Spacer(modifier = Modifier.height(2.dp)) }
            item {
                val overviewShape = RoundedCornerShape(16.dp)
                val overviewTitleColor = if (isDark) Color.White else MiuixTheme.colorScheme.onBackgroundVariant
                Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(overviewShape)
                    .background(
                        overviewRefreshState.surfaceColor(
                            isDark = isDark,
                            neutralSurface = MiuixTheme.colorScheme.surface
                        ),
                        overviewShape
                    )
                    .border(
                        width = 1.dp,
                        color = overviewRefreshState.borderColor(
                            isDark = isDark,
                            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                        ),
                        shape = overviewShape
                    ),
                colors = CardDefaults.defaultColors(
                    color = overviewRefreshState.surfaceColor(
                        isDark = isDark,
                        neutralSurface = MiuixTheme.colorScheme.surface
                    ),
                    contentColor = MiuixTheme.colorScheme.onBackground
                ),
                showIndication = cardPressFeedbackEnabled,
                onClick = { refreshAllTracked(showToast = true) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { refreshAllTracked(showToast = true) },
                            onLongClick = { openTrackSheetForAdd() }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("项目版本跟踪", color = MiuixTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.weight(1f))
                        if (overviewRefreshState != OverviewRefreshState.Idle) {
                            val indicatorColor = overviewRefreshState.color(
                                neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                            val indicatorBg = overviewRefreshState.indicatorBackground(
                                neutralSurface = MiuixTheme.colorScheme.surface
                            )
                            val progressValue = when (overviewRefreshState) {
                                OverviewRefreshState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
                                OverviewRefreshState.Completed,
                                OverviewRefreshState.Cached -> 1f
                                OverviewRefreshState.Idle -> 0f
                            }
                            CircularProgressIndicator(
                                progress = progressValue,
                                size = 18.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = indicatorColor,
                                    backgroundColor = indicatorBg
                                )
                            )
                        }
                        StatusPill(
                            label = formatRefreshAgo(lastRefreshMs),
                            color = overviewRefreshState.color(
                                neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                            ),
                            backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                            borderAlphaOverride = if (isDark) 0.35f else 0.42f
                        )
                        StatusPill(
                            label = when (overviewRefreshState) {
                                OverviewRefreshState.Cached -> StatusLabelText.Cached
                                OverviewRefreshState.Refreshing -> StatusLabelText.Checking
                                OverviewRefreshState.Completed -> StatusLabelText.Checked
                                OverviewRefreshState.Idle -> StatusLabelText.PendingCheck
                            },
                            color = overviewRefreshState.color(
                                neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GitHubOverviewMetricItem(
                            label = "策略",
                            value = lookupConfig.selectedStrategy.overviewLabel(),
                            titleColor = overviewTitleColor,
                            valueColor = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "API",
                            value = lookupConfig.overviewApiLabel(),
                            titleColor = overviewTitleColor,
                            valueColor = if (lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken) {
                                if (lookupConfig.apiToken.isBlank()) {
                                    GitHubStatusPalette.PreRelease
                                } else {
                                    GitHubStatusPalette.Active
                                }
                            } else {
                                MiuixTheme.colorScheme.onBackgroundVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GitHubOverviewMetricItem(
                            label = "已追踪",
                            value = "$trackedCount 项",
                            titleColor = overviewTitleColor,
                            valueColor = if (trackedCount > 0) GitHubStatusPalette.Stable else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "稳定可更新",
                            value = "$updatableCount 项",
                            titleColor = overviewTitleColor,
                            valueColor = if (updatableCount > 0) GitHubStatusPalette.Update else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GitHubOverviewMetricItem(
                            label = "稳定已最新",
                            value = "$stableLatestCount 项",
                            titleColor = overviewTitleColor,
                            valueColor = if (stableLatestCount > 0) GitHubStatusPalette.Stable else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "预发跟踪",
                            value = "$preReleaseCount 项",
                            titleColor = overviewTitleColor,
                            valueColor = if (preReleaseCount > 0) GitHubStatusPalette.PreRelease else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GitHubOverviewMetricItem(
                            label = "预发可更新",
                            value = "$preReleaseUpdateCount 项",
                            titleColor = overviewTitleColor,
                            valueColor = if (preReleaseUpdateCount > 0) GitHubStatusPalette.PreRelease else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "检查失败",
                            value = "$failedCount 项",
                            titleColor = overviewTitleColor,
                            valueColor = if (failedCount > 0) GitHubStatusPalette.Error else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            if (trackedItems.isEmpty()) {
                item { MiuixInfoItem("跟踪列表", "暂无条目，请先新增") }
            } else if (filteredTracked.isEmpty()) {
                item { MiuixInfoItem("搜索结果", "没有匹配的跟踪项目") }
            } else {
                items(sortedTracked, key = { it.id }) { item ->
                    var expanded by remember(item.id) { mutableStateOf(false) }
                    MiuixAccordionCard(
                        backdrop = backdrop,
                        title = item.appLabel,
                        subtitle = item.packageName,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        headerStartAction = {
                            AppIcon(packageName = item.packageName, size = 24.dp)
                        },
                        onHeaderLongClick = { openTrackSheetForEdit(item) },
                        headerActions = {
                            val state = checkStates[item.id] ?: VersionCheckUi()
                            val statusColor = state.statusColor(
                                neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                            val statusReleaseUrl = state.statusActionUrl(
                                owner = item.owner,
                                repo = item.repo
                            )
                            val canLoadApkAssets = state.hasUpdate == true ||
                                state.recommendsPreRelease ||
                                state.hasPreReleaseUpdate
                            val isAssetPanelExpanded = apkAssetExpanded[item.id] == true
                            val isAssetPanelLoading = apkAssetLoading[item.id] == true
                            val statusIcon = when {
                                isAssetPanelLoading -> MiuixIcons.Regular.Refresh
                                canLoadApkAssets && isAssetPanelExpanded -> MiuixIcons.Regular.Close
                                else -> state.statusIcon()
                            }
                            val clickableModifier = if (statusReleaseUrl.isNotBlank()) {
                                Modifier.clickable {
                                    if (canLoadApkAssets) {
                                        loadApkAssets(item, state)
                                    } else {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(statusReleaseUrl)))
                                        }.onFailure {
                                            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = state.message.ifBlank { "状态" },
                                tint = statusColor,
                                modifier = clickableModifier
                            )
                        }
                    ) {
                        val state = checkStates[item.id] ?: VersionCheckUi()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GitHubCompactInfoRow(
                                label = "仓库地址",
                                value = "${item.owner}/${item.repo}",
                                valueColor = MiuixTheme.colorScheme.primary,
                                titleColor = MiuixTheme.colorScheme.primary,
                                onClick = {
                                    val releaseUrl = GitHubVersionUtils.buildReleaseUrl(item.owner, item.repo)
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                                    }.onFailure {
                                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            if (state.localVersion.isNotBlank()) {
                                val localText = if (state.localVersionCode >= 0L) {
                                    "${state.localVersion} (${state.localVersionCode})"
                                } else {
                                    state.localVersion
                                }
                                VersionValueRow(
                                    label = "本地版本",
                                    value = localText,
                                    valueColor = MiuixTheme.colorScheme.primary
                                )
                            }
                            if (state.hasStableRelease &&
                                (state.latestStableName.isNotBlank() ||
                                    state.latestStableRawTag.isNotBlank() ||
                                    state.latestTag.isNotBlank())
                            ) {
                                val latestColor = state.stableVersionColor(
                                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                                VersionValueRow(
                                    label = "稳定版本",
                                    value = formatReleaseValue(
                                        releaseName = state.latestStableName.ifBlank { state.latestTag },
                                        rawTag = state.latestStableRawTag
                                    ),
                                    valueColor = latestColor,
                                    emphasized = state.hasUpdate == true && !state.recommendsPreRelease
                                )
                            }
                            if (state.showPreReleaseInfo &&
                                (state.latestPreName.isNotBlank() ||
                                    state.latestPreRawTag.isNotBlank() ||
                                    state.preReleaseInfo.isNotBlank())
                            ) {
                                val preColor = state.preReleaseVersionColor(
                                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                                VersionValueRow(
                                    label = "预发版本",
                                    value = formatReleaseValue(
                                        releaseName = state.latestPreName.ifBlank { state.preReleaseInfo },
                                        rawTag = state.latestPreRawTag
                                    ),
                                    valueColor = preColor,
                                    emphasized = state.recommendsPreRelease || state.hasPreReleaseUpdate
                                )
                            }
                            if (state.releaseHint.isNotBlank()) {
                                GitHubCompactInfoRow(
                                    label = "提示",
                                    value = state.releaseHint,
                                    valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
                                    titleColor = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                            }

                            val assetBundle = apkAssetBundles[item.id]
                            val assetLoading = apkAssetLoading[item.id] == true
                            val assetError = apkAssetErrors[item.id].orEmpty()
                            val assetExpanded = apkAssetExpanded[item.id] == true
                            AnimatedVisibility(
                                visible = assetExpanded || assetLoading || assetError.isNotBlank()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val target = state.apkAssetTarget(item.owner, item.repo)
                                    GitHubCompactInfoRow(
                                        label = "更新资源",
                                        value = target?.label ?: "APK",
                                        valueColor = MiuixTheme.colorScheme.primary,
                                        titleColor = MiuixTheme.colorScheme.primary,
                                        emphasized = true,
                                        onClick = {
                                            val releaseUrl = assetBundle?.htmlUrl
                                                ?.takeIf { it.isNotBlank() }
                                                ?: target?.releaseUrl
                                                .orEmpty()
                                            if (releaseUrl.isNotBlank()) {
                                                openExternalUrl(releaseUrl)
                                            }
                                        }
                                    )
                                    when {
                                        assetLoading -> {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.defaultColors(
                                                    color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(
                                                        progress = 0f,
                                                        size = 18.dp,
                                                        strokeWidth = 2.dp,
                                                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                                            foregroundColor = MiuixTheme.colorScheme.primary,
                                                            backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        )
                                                    )
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "正在读取 release 里的 APK 文件",
                                                            color = MiuixTheme.colorScheme.onBackground,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = "会优先筛出 .apk，并把 arm64 资源排在前面",
                                                            color = MiuixTheme.colorScheme.onBackgroundVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        assetError.isNotBlank() -> {
                                            GitHubCompactInfoRow(
                                                label = "APK",
                                                value = assetError,
                                                valueColor = GitHubStatusPalette.Error,
                                                titleColor = GitHubStatusPalette.Error,
                                                titleMinWidth = 40.dp
                                            )
                                        }
                                        assetBundle != null -> {
                                            assetBundle.assets.forEach { asset ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.defaultColors(
                                                        color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        GitHubCompactInfoRow(
                                                            label = "文件",
                                                            value = asset.name,
                                                            valueColor = MiuixTheme.colorScheme.onBackground,
                                                            titleColor = MiuixTheme.colorScheme.onBackgroundVariant,
                                                            titleMinWidth = 40.dp
                                                        )
                                                        GitHubCompactInfoRow(
                                                            label = "信息",
                                                            value = "${formatAssetSize(asset.sizeBytes)} · ${asset.downloadCount} 次下载",
                                                            valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
                                                            titleColor = MiuixTheme.colorScheme.onBackgroundVariant,
                                                            titleMinWidth = 40.dp
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            val actionLabels = assetActionLabels(asset)
                                                            TextButton(
                                                                modifier = Modifier.weight(1f),
                                                                text = actionLabels.first,
                                                                onClick = { openApkInDownloader(asset) }
                                                            )
                                                            TextButton(
                                                                modifier = Modifier.weight(1f),
                                                                text = actionLabels.second,
                                                                onClick = { shareApkLink(asset) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            }

            AnimatedVisibility(
                visible = showFloatingAddButton,
                enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it / 2 }
                ),
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
            ) {
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.AddCircle,
                    contentDescription = "新增跟踪",
                    onClick = { openTrackSheetForAdd() },
                    modifier = Modifier.padding(end = 14.dp, bottom = contentBottomPadding - 24.dp),
                    width = 60.dp,
                    height = 44.dp,
                    variant = GlassVariant.Bar
                )
            }
        }
    }

    SnapshotWindowBottomSheet(
        show = showStrategySheet,
        title = "GitHub 抓取方案",
        onDismissRequest = { closeStrategySheet() },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = { closeStrategySheet() }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "保存方案",
                onClick = { applyLookupConfig() }
            )
        }
    ) {
        val sanitizedTokenInput = githubApiTokenInput.trim()
        val draftChanged = selectedStrategyInput != lookupConfig.selectedStrategy ||
            sanitizedTokenInput != lookupConfig.apiToken
        val tokenStatusLabel = when {
            selectedStrategyInput != GitHubLookupStrategyOption.GitHubApiToken -> "未使用"
            sanitizedTokenInput.isBlank() -> "游客"
            else -> "已填写"
        }
        val tokenStatusColor = when {
            selectedStrategyInput != GitHubLookupStrategyOption.GitHubApiToken -> MiuixTheme.colorScheme.onBackgroundVariant
            sanitizedTokenInput.isBlank() -> GitHubStatusPalette.PreRelease
            else -> GitHubStatusPalette.Update
        }
        val credentialAvailabilityLabel = when {
            credentialCheckRunning -> "检测中"
            credentialCheckStatus != null -> "可用"
            credentialCheckError != null -> "不可用"
            else -> "未检测"
        }
        val credentialAvailabilityColor = when {
            credentialCheckRunning -> MiuixTheme.colorScheme.primary
            credentialCheckStatus != null -> GitHubStatusPalette.Update
            credentialCheckError != null -> GitHubStatusPalette.Error
            else -> MiuixTheme.colorScheme.onBackgroundVariant
        }

        @Composable
        fun StrategyBenchmarkSection() {
            SheetSectionTitle("本地对比")
            SheetSectionCard {
                SheetDescriptionText(
                    text = if (trackedItems.isEmpty()) {
                        "对比测试会使用当前已追踪仓库，最多抽取 6 个样本。当前还没有可用样本。"
                    } else {
                        "对比测试会使用当前已追踪仓库做一轮冷启动和一轮缓存复测，便于直接观察 Atom、游客 API、Token API 的耗时与缓存命中差异。"
                    }
                )
                if (trackedItems.isNotEmpty()) {
                    SheetActionGroup {
                        GlassTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = if (strategyBenchmarkRunning) "对比中..." else "运行双策略对比",
                            enabled = !strategyBenchmarkRunning,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { runStrategyBenchmark() }
                        )
                    }
                }
                strategyBenchmarkError?.let { error ->
                    SheetDescriptionText(
                        text = "对比测试失败：$error"
                    )
                }
            }
            strategyBenchmarkReport?.results?.forEach { result ->
                GitHubStrategyBenchmarkCard(result = result)
            }
        }

        SheetContentColumn(
            verticalSpacing = 10.dp
        ) {
            SheetSectionTitle("配置摘要")
            GitHubStrategyDraftSummaryCard(
                selectedStrategy = selectedStrategyInput,
                tokenInput = sanitizedTokenInput,
                trackedCount = trackedItems.size,
                changed = draftChanged
            )

            SheetSectionTitle("抓取方案")
            githubStrategyGuides.forEach { guide ->
                GitHubStrategyGuideCard(
                    guide = guide,
                    selected = selectedStrategyInput == guide.option,
                    onSelect = { selectedStrategyInput = guide.option }
                )
            }

            if (selectedStrategyInput == GitHubLookupStrategyOption.GitHubApiToken) {
                SheetSectionTitle("凭证设置")
                SheetSectionCard {
                    SheetControlRow(label = "Token 状态") {
                        StatusPill(
                            label = tokenStatusLabel,
                            color = tokenStatusColor
                        )
                    }
                    SheetControlRow(label = "可用性") {
                        StatusPill(
                            label = credentialAvailabilityLabel,
                            color = credentialAvailabilityColor
                        )
                    }
                    SheetFieldBlock(
                        title = "GitHub API Token",
                        summary = if (sanitizedTokenInput.isBlank()) {
                            "当前使用游客 API，可随时补充 token"
                        } else {
                            "当前已填写 token，可在此替换"
                        },
                        trailing = {
                            GlassTextButton(
                                backdrop = backdrop,
                                variant = GlassVariant.SheetAction,
                                text = if (showApiTokenPlainText) "隐藏 Token" else "显示 Token",
                                onClick = { showApiTokenPlainText = !showApiTokenPlainText }
                            )
                        }
                    ) {
                        GlassSearchField(
                            value = githubApiTokenInput,
                            onValueChange = {
                                githubApiTokenInput = it
                                credentialCheckError = null
                                credentialCheckStatus = null
                            },
                            label = "GitHub API token（选填）",
                            backdrop = backdrop,
                            variant = GlassVariant.SheetInput,
                            singleLine = true,
                            visualTransformation = if (showApiTokenPlainText) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            }
                        )
                    }
                }
                SheetSectionTitle("立即验证")
                SheetSectionCard {
                    SheetActionGroup {
                        GlassTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = if (credentialCheckRunning) "检测中..." else "检测当前凭证",
                            enabled = !credentialCheckRunning,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { runCredentialCheck() }
                        )
                    }
                    SheetDescriptionText(
                        text = "未填写 token 时会自动走游客 API；适合刚开始少量追踪。若追踪项目增多或遇到限流，再补充本地 token 即可。token 仅保存在当前设备 MMKV。"
                    )
                    SheetDescriptionText(
                        text = "对于当前这套 GitHub API 检查和 API 资产下载，Fine-grained PAT 的 `Contents: Read` 权限就够用；它可以读取 release 元数据，也可以通过 assets API 下载 APK 二进制，不需要额外写权限。若目标是 private 仓库，还需要该 token 对对应仓库本身具备访问权限。"
                    )
                    credentialCheckError?.let { error ->
                        SheetDescriptionText(
                            text = "凭证检测失败：$error"
                        )
                    }
                }
                credentialCheckStatus?.let { status ->
                    GitHubCredentialStatusCard(status = status)
                    SheetDescriptionText(
                        text = if (status.authMode == GitHubApiAuthMode.Guest) {
                            "当前结果表明游客 API 可访问，但额度较低。若后续追踪仓库数量增加，建议补充本地 token。"
                        } else {
                            "当前 token 已被 GitHub API 接受。此结果仅表示凭证本身可用，不代表对所有私有仓库都具备访问权限。"
                        }
                    )
                }
                StrategyBenchmarkSection()
                SheetDescriptionText(
                    text = "API 方案会直接调用 Releases API。若你只是首次体验，可先用游客 API；若你要长期追踪更多仓库，再补充专用 token 会更稳。"
                )
                SheetSectionTitle("推荐新建")
                GitHubRecommendedTokenGuideCard(
                    guide = githubRecommendedTokenGuide,
                    expanded = recommendedTokenGuideExpanded,
                    onExpandedChange = { recommendedTokenGuideExpanded = it }
                )
                SheetActionGroup {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = "打开预填创建页",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openExternalUrl(
                                url = buildGitHubFineGrainedTokenTemplateUrl(),
                                failureMessage = "无法打开 GitHub 创建页"
                            )
                        }
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = "查看官方说明",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openExternalUrl(
                                url = githubFineGrainedPatDocsUrl,
                                failureMessage = "无法打开官方说明"
                            )
                        }
                    )
                }
                SheetDescriptionText(
                    text = "推荐单独创建一个 KeiOS 专用 Fine-grained token，不要复用现有 classic token。这样即使 token 泄露，暴露面也更小。"
                )
            } else {
                SheetSectionTitle("方案说明")
                SheetSectionCard {
                    SheetDescriptionText(
                        text = "Atom Feed 方案无需 Token，适合公开仓库与轻量检查。若后续需要更稳定的 release 元数据或私有仓库支持，可切换到 GitHub API Token。"
                    )
                }
                StrategyBenchmarkSection()
            }
        }
    }

    SnapshotWindowBottomSheet(
        show = showCheckLogicSheet,
        title = "检查逻辑",
        onDismissRequest = { closeCheckLogicSheet() },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = { closeCheckLogicSheet() }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "保存检查逻辑",
                onClick = { applyCheckLogicSheet() }
            )
        }
    ) {
        val selectedRefreshOption = RefreshIntervalOption.fromHours(refreshIntervalHoursInput)
        val logicChanged = refreshIntervalHoursInput != refreshIntervalHours ||
            checkAllTrackedPreReleasesInput != lookupConfig.checkAllTrackedPreReleases ||
            aggressiveApkFilteringInput != lookupConfig.aggressiveApkFiltering

        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionTitle("当前摘要")
            SheetSectionCard {
                SheetControlRow(label = "更新间隔", summary = "超过这个时间会自动视为缓存过期") {
                    Box(
                        modifier = Modifier.capturePopupAnchor { checkLogicIntervalPopupAnchorBounds = it }
                    ) {
                        GlassTextButton(
                            backdrop = backdrop,
                            variant = GlassVariant.SheetAction,
                            text = selectedRefreshOption.label,
                            onClick = { showCheckLogicIntervalPopup = !showCheckLogicIntervalPopup }
                        )
                        if (showCheckLogicIntervalPopup) {
                            SnapshotWindowListPopup(
                                show = showCheckLogicIntervalPopup,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                anchorBounds = checkLogicIntervalPopupAnchorBounds,
                                placement = SnapshotPopupPlacement.ButtonEnd,
                                onDismissRequest = { showCheckLogicIntervalPopup = false },
                                enableWindowDim = false
                            ) {
                                LiquidDropdownColumn {
                                    val options = RefreshIntervalOption.entries
                                    options.forEachIndexed { index, option ->
                                        LiquidDropdownImpl(
                                            text = option.label,
                                            optionSize = options.size,
                                            isSelected = selectedRefreshOption == option,
                                            index = index,
                                            onSelectedIndexChange = { selectedIndex ->
                                                refreshIntervalHoursInput = options[selectedIndex].hours
                                                showCheckLogicIntervalPopup = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                SheetControlRow(
                    label = "检查所有追踪项的预发行最新版",
                    summary = "开启后会额外展示每个项目的最新预发版本，但不会自动等于推荐安装预发行"
                ) {
                    Switch(
                        checked = checkAllTrackedPreReleasesInput,
                        onCheckedChange = { checked -> checkAllTrackedPreReleasesInput = checked }
                    )
                }
                SheetControlRow(
                    label = "更激进的过滤方式",
                    summary = "开启后会直接忽略文件名包含 `armeabi-v7a`、`x86_64` 的 APK 资源"
                ) {
                    Switch(
                        checked = aggressiveApkFilteringInput,
                        onCheckedChange = { checked -> aggressiveApkFilteringInput = checked }
                    )
                }
                SheetControlRow(label = "保存状态") {
                    StatusPill(
                        label = if (logicChanged) "待保存" else "已同步",
                        color = if (logicChanged) GitHubStatusPalette.PreRelease else MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            }

            SheetSectionTitle("说明")
            SheetSectionCard {
                SheetDescriptionText(
                    text = "全局开关只负责“要不要为所有项目顺手检查最新预发行”；单条目里的“优先预发行版本”只负责决定这个项目在推荐更新时是否偏向预发行。"
                )
                SheetDescriptionText(
                    text = "这样就不会再把“我想知道有没有新的预发行”和“我真的想装预发行”混成同一个开关。"
                )
                SheetDescriptionText(
                    text = "若你的设备主要只关心 arm64 版本，可以开启“更激进的过滤方式”；它会继续保留 universal，但会把文件名明确写着 `armeabi-v7a`、`x86_64` 的 APK 直接排除。"
                )
            }
        }
    }

    SnapshotWindowBottomSheet(
        show = showAddSheet,
        title = if (editingTrackedItem == null) "新增跟踪" else "编辑跟踪",
        onDismissRequest = {
            showAddSheet = false
            pickerExpanded = false
            appSearch = ""
            preferPreReleaseInput = false
            editingTrackedItem = null
        },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = {
                    showAddSheet = false
                    pickerExpanded = false
                    appSearch = ""
                    preferPreReleaseInput = false
                    editingTrackedItem = null
                }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = if (editingTrackedItem == null) "确认新增" else "确认保存",
                onClick = { applyTrackSheet() }
            )
        }
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionTitle("仓库与应用")
            SheetSectionCard {
                SheetInputTitle("GitHub 项目地址")
                GlassSearchField(
                    value = repoUrlInput,
                    onValueChange = { repoUrlInput = it },
                    label = "GitHub 项目地址",
                    backdrop = backdrop,
                    variant = GlassVariant.SheetInput,
                    singleLine = true
                )
                SheetInputTitle("筛选本机 App")
                GlassSearchField(
                    value = appSearch,
                    onValueChange = { appSearch = it },
                    label = "名称或包名",
                    backdrop = backdrop,
                    variant = GlassVariant.SheetInput,
                    singleLine = true
                )
                SheetControlRow(
                    label = "已选应用",
                    summary = if (selectedApp == null) "未选择" else null
                ) {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = if (pickerExpanded) "收起列表" else "选择应用",
                        onClick = { pickerExpanded = !pickerExpanded }
                    )
                }
                selectedApp?.let { app ->
                    GitHubSelectedAppCard(selectedApp = app)
                }
            }
            SheetSectionTitle("检查选项")
            SheetSectionCard {
                SheetControlRow(
                    label = "优先预发行版本",
                    summary = "仅影响这个项目的推荐更新目标，不影响全局是否检查预发行"
                ) {
                    Switch(
                        checked = preferPreReleaseInput,
                        onCheckedChange = { checked -> preferPreReleaseInput = checked }
                    )
                }
            }
            if (pickerExpanded) {
                val filteredApps = appList.filter { app ->
                    appSearch.isBlank() ||
                        app.label.contains(appSearch, ignoreCase = true) ||
                        app.packageName.contains(appSearch, ignoreCase = true)
                }.take(80)
                SheetSectionTitle("应用候选")
                SheetSectionCard(verticalSpacing = 6.dp) {
                    if (filteredApps.isEmpty()) {
                        MiuixInfoItem("应用列表", "没有匹配结果")
                    } else {
                        filteredApps.forEach { app ->
                            GitHubAppCandidateRow(
                                app = app,
                                selected = selectedApp?.packageName == app.packageName,
                                onClick = {
                                    selectedApp = app
                                    pickerExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (editingTrackedItem != null) {
                SheetSectionTitle(
                    text = "危险操作",
                    danger = true
                )
                SheetSectionCard {
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetDangerAction,
                        text = "删除跟踪",
                        textColor = MiuixTheme.colorScheme.error,
                        onClick = {
                            pendingDeleteItem = editingTrackedItem
                            showAddSheet = false
                            pickerExpanded = false
                            appSearch = ""
                            editingTrackedItem = null
                        }
                    )
                }
            }
        }
    }

    WindowDialog(
        show = pendingDeleteItem != null,
        title = "删除跟踪",
        summary = pendingDeleteItem?.let { "确定删除 ${it.appLabel} (${it.owner}/${it.repo}) 吗？" },
        onDismissRequest = { pendingDeleteItem = null }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = "取消",
                    onClick = {
                        if (!deleteInProgress) pendingDeleteItem = null
                    }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = if (deleteInProgress) "删除中..." else "删除",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = {
                        if (deleteInProgress) return@TextButton
                        pendingDeleteItem?.let { deleting ->
                            deleteInProgress = true
                            try {
                                cancelRefreshAll()
                                trackedItems.remove(deleting)
                                checkStates.remove(deleting.id)
                                saveTracked()
                                persistCheckCache()
                                Toast.makeText(context, "已删除 ${deleting.appLabel}", Toast.LENGTH_SHORT).show()
                            } finally {
                                deleteInProgress = false
                            }
                        }
                        pendingDeleteItem = null
                    }
                )
            }
        }
    }
}
