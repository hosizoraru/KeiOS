package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SheetInputTitle
import com.example.keios.ui.page.main.widget.SheetRow
import com.example.keios.ui.page.main.widget.SheetSectionTitle
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.SnapshotPopupPlacement
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.feature.github.data.local.AppIconCache
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubApiTokenReleaseStrategy
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
import top.yukonga.miuix.kmp.icon.extended.Timer
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
    var editingTrackedItem by remember { mutableStateOf<GitHubTrackedApp?>(null) }
    var checkPreReleaseInput by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var appListLoaded by remember { mutableStateOf(false) }
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }
    var showSortPopup by remember { mutableStateOf(false) }
    var showIntervalPopup by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(GitHubSortMode.UpdateFirst) }
    var pendingDeleteItem by remember { mutableStateOf<GitHubTrackedApp?>(null) }
    var overviewRefreshState by remember { mutableStateOf(OverviewRefreshState.Idle) }
    var lastRefreshMs by remember { mutableStateOf(0L) }
    var refreshIntervalHours by remember { mutableStateOf(GitHubTrackStore.loadRefreshIntervalHours()) }
    var refreshProgress by remember { mutableStateOf(0f) }
    var lookupConfig by remember { mutableStateOf(GitHubTrackStore.loadLookupConfig()) }
    var selectedStrategyInput by remember { mutableStateOf(lookupConfig.selectedStrategy) }
    var githubApiTokenInput by remember { mutableStateOf(lookupConfig.apiToken) }
    var showApiTokenPlainText by remember { mutableStateOf(false) }
    var strategyBenchmarkRunning by remember { mutableStateOf(false) }
    var strategyBenchmarkError by remember { mutableStateOf<String?>(null) }
    var strategyBenchmarkReport by remember { mutableStateOf<com.example.keios.feature.github.model.GitHubStrategyBenchmarkReport?>(null) }
    var credentialCheckRunning by remember { mutableStateOf(false) }
    var credentialCheckError by remember { mutableStateOf<String?>(null) }
    var credentialCheckStatus by remember { mutableStateOf<GitHubApiCredentialStatus?>(null) }
    var recommendedTokenGuideExpanded by remember { mutableStateOf(false) }

    val trackedItems = remember { mutableStateListOf<GitHubTrackedApp>() }
    val checkStates = remember { mutableStateMapOf<String, VersionCheckUi>() }

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }

    fun VersionCheckUi.toCacheEntry(): GitHubCheckCacheEntry = GitHubCheckCacheEntry(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = latestTag,
        latestStableRawTag = latestStableRawTag,
        latestStableUrl = latestStableUrl,
        latestPreRawTag = latestPreRawTag,
        latestPreUrl = latestPreUrl,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreRelease,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate,
        sourceStrategyId = sourceStrategyId
    )

    fun GitHubCheckCacheEntry.toUi(): VersionCheckUi = VersionCheckUi(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = latestTag,
        latestStableRawTag = latestStableRawTag,
        latestStableUrl = latestStableUrl,
        latestPreRawTag = latestPreRawTag,
        latestPreUrl = latestPreUrl,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreRelease,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate,
        sourceStrategyId = sourceStrategyId
    )

    fun GitHubTrackedReleaseCheck.toUi(): VersionCheckUi = VersionCheckUi(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = stableRelease?.displayVersion.orEmpty(),
        latestStableRawTag = stableRelease?.rawTag.orEmpty(),
        latestStableUrl = stableRelease?.link.orEmpty(),
        latestPreRawTag = preRelease?.rawTag.orEmpty(),
        latestPreUrl = preRelease?.link.orEmpty(),
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreReleaseInstalled,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate,
        sourceStrategyId = strategyId
    )

    fun persistCheckCache(refreshTimestamp: Long = lastRefreshMs) {
        val states = trackedItems.associate { item ->
            val state = checkStates[item.id] ?: VersionCheckUi()
            item.id to state.toCacheEntry()
        }
        GitHubTrackStore.saveCheckCache(states, refreshTimestamp)
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
            if (showToastOnError && state.message.startsWith("检查失败")) {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            checkStates[item.id] = state
            persistCheckCache()
        }
    }

    fun refreshAllTracked(showToast: Boolean = true) {
        if (trackedItems.isEmpty()) {
            if (showToast) Toast.makeText(context, "暂无可检查条目", Toast.LENGTH_SHORT).show()
            overviewRefreshState = OverviewRefreshState.Idle
            refreshProgress = 0f
            return
        }
        scope.launch {
            GitHubTrackStore.clearCheckCache()
            lastRefreshMs = 0L
            overviewRefreshState = OverviewRefreshState.Refreshing
            refreshProgress = 0f
            trackedItems.forEach { item ->
                checkStates[item.id] = VersionCheckUi(loading = true, message = "检查中...")
            }
            trackedItems.forEachIndexed { index, item ->
                val state = resolveItemState(item)
                checkStates[item.id] = state
                refreshProgress = (index + 1).toFloat() / trackedItems.size.toFloat()
                if (showToast && state.message.startsWith("检查失败")) {
                    Toast.makeText(context, "${item.owner}/${item.repo}: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                if (index < trackedItems.lastIndex) delay(120)
            }
            overviewRefreshState = OverviewRefreshState.Completed
            lastRefreshMs = System.currentTimeMillis()
            refreshProgress = 1f
            persistCheckCache(lastRefreshMs)
            if (showToast) Toast.makeText(context, "检查完成", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyLookupConfig() {
        val previousConfig = GitHubTrackStore.loadLookupConfig()
        val sanitizedToken = githubApiTokenInput.trim()
        val newConfig = GitHubLookupConfig(
            selectedStrategy = selectedStrategyInput,
            apiToken = sanitizedToken
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

    fun saveTracked() {
        GitHubTrackStore.save(trackedItems.toList())
    }

    fun openTrackSheetForAdd() {
        editingTrackedItem = null
        repoUrlInput = ""
        selectedApp = null
        appSearch = ""
        pickerExpanded = false
        checkPreReleaseInput = false
        showAddSheet = true
    }

    fun openTrackSheetForEdit(item: GitHubTrackedApp) {
        editingTrackedItem = item
        repoUrlInput = item.repoUrl
        selectedApp = appList.firstOrNull { it.packageName == item.packageName }
            ?: InstalledAppItem(label = item.appLabel, packageName = item.packageName)
        appSearch = ""
        pickerExpanded = false
        checkPreReleaseInput = item.checkPreRelease
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
            checkPreRelease = checkPreReleaseInput
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
        checkPreReleaseInput = false
        showAddSheet = false
    }

    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { reloadApps() }
        }

    LaunchedEffect(Unit) {
        lookupConfig = GitHubTrackStore.loadLookupConfig()
        selectedStrategyInput = lookupConfig.selectedStrategy
        githubApiTokenInput = lookupConfig.apiToken
        trackedItems.clear()
        trackedItems.addAll(GitHubTrackStore.load())
        reloadApps()
        val (cachedStates, cachedRefreshMs) = GitHubTrackStore.loadCheckCache()
        checkStates.clear()
        trackedItems.forEach { item ->
            cachedStates[item.id]
                ?.takeIf(::cacheMatchesCurrentStrategy)
                ?.let { cached -> checkStates[item.id] = cached.toUi() }
        }
        lastRefreshMs = cachedRefreshMs
        refreshIntervalHours = GitHubTrackStore.loadRefreshIntervalHours()
        val hasTracked = trackedItems.isNotEmpty()
        val hasCachedForTracked = trackedItems.any { item ->
            cachedStates[item.id]?.let(::cacheMatchesCurrentStrategy) == true
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
                    title = "GitHub",
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
                                        icon = MiuixIcons.Regular.Sort,
                                        contentDescription = "排序",
                                        onClick = { showSortPopup = !showSortPopup }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Timer,
                                        contentDescription = "刷新间隔",
                                        onClick = { showIntervalPopup = !showIntervalPopup }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Refresh,
                                        contentDescription = "检查",
                                        onClick = { refreshAllTracked(showToast = true) }
                                    ),
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.AddCircle,
                                        contentDescription = "新增跟踪",
                                        onClick = { openTrackSheetForAdd() }
                                    )
                                ),
                                onInteractionChanged = onActionBarInteractingChanged
                            )

                            LiquidActionBarPopupAnchors(itemCount = 5) { slotIndex, popupAnchorBounds ->
                                when (slotIndex) {
                                    1 -> if (showSortPopup) {
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

                                    2 -> if (showIntervalPopup) {
                                        SnapshotWindowListPopup(
                                            show = showIntervalPopup,
                                            alignment = PopupPositionProvider.Align.BottomStart,
                                            anchorBounds = popupAnchorBounds,
                                            placement = SnapshotPopupPlacement.ActionBarCenter,
                                            onDismissRequest = { showIntervalPopup = false },
                                            enableWindowDim = false
                                        ) {
                                            LiquidDropdownColumn {
                                                val options = RefreshIntervalOption.entries
                                                val selected = RefreshIntervalOption.fromHours(refreshIntervalHours)
                                                options.forEachIndexed { index, option ->
                                                    LiquidDropdownImpl(
                                                        text = option.label,
                                                        optionSize = options.size,
                                                        isSelected = selected == option,
                                                        index = index,
                                                        onSelectedIndexChange = { selectedIndex ->
                                                            val picked = options[selectedIndex]
                                                            refreshIntervalHours = picked.hours
                                                            GitHubTrackStore.saveRefreshIntervalHours(picked.hours)
                                                            showIntervalPopup = false
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 12.dp,
                end = 12.dp
            )
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Spacer(modifier = Modifier.height(2.dp)) }
            item {
                Card(
                modifier = Modifier
                    .fillMaxWidth(),
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
                    GitHubOverviewMetricItem(
                        label = "上次检查",
                        value = formatRefreshAgo(lastRefreshMs),
                        valueColor = overviewRefreshState.color(
                            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GitHubOverviewMetricItem(
                            label = "策略",
                            value = lookupConfig.selectedStrategy.overviewLabel(),
                            valueColor = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "API",
                            value = lookupConfig.overviewApiLabel(),
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
                            valueColor = if (trackedCount > 0) GitHubStatusPalette.Stable else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "稳定可更新",
                            value = "$updatableCount 项",
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
                            valueColor = if (stableLatestCount > 0) GitHubStatusPalette.Stable else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "预发跟踪",
                            value = "$preReleaseCount 项",
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
                            valueColor = if (preReleaseUpdateCount > 0) GitHubStatusPalette.PreRelease else MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GitHubOverviewMetricItem(
                            label = "检查失败",
                            value = "$failedCount 项",
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
                        subtitle = "${item.owner}/${item.repo}",
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        headerStartAction = {
                            AppIcon(packageName = item.packageName, size = 24.dp)
                        },
                        onHeaderLongClick = { openTrackSheetForEdit(item) },
                        headerActions = {
                            val state = checkStates[item.id] ?: VersionCheckUi()
                            val statusIcon = state.statusIcon()
                            val statusColor = state.statusColor(
                                neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                            val clickableModifier = if (state.hasUpdate == true || state.isPreRelease) {
                                Modifier.clickable {
                                    val releaseUrl = when {
                                        state.hasPreReleaseUpdate && state.latestPreUrl.isNotBlank() ->
                                            state.latestPreUrl
                                        state.hasPreReleaseUpdate && state.latestPreRawTag.isNotBlank() ->
                                            GitHubVersionUtils.buildReleaseTagUrl(item.owner, item.repo, state.latestPreRawTag)
                                        state.hasUpdate == true && state.latestStableUrl.isNotBlank() ->
                                            state.latestStableUrl
                                        state.hasUpdate == true && state.latestStableRawTag.isNotBlank() ->
                                            GitHubVersionUtils.buildReleaseTagUrl(item.owner, item.repo, state.latestStableRawTag)
                                        state.isPreRelease && state.latestPreUrl.isNotBlank() ->
                                            state.latestPreUrl
                                        state.isPreRelease && state.latestPreRawTag.isNotBlank() ->
                                            GitHubVersionUtils.buildReleaseTagUrl(item.owner, item.repo, state.latestPreRawTag)
                                        else -> GitHubVersionUtils.buildReleaseUrl(item.owner, item.repo)
                                    }
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                                    }.onFailure {
                                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
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
                                label = "应用包名",
                                value = item.packageName,
                                valueColor = MiuixTheme.colorScheme.primary,
                                onClick = {
                                    refreshItem(item, showToastOnError = true)
                                    Toast.makeText(context, "已检查 ${item.appLabel}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            GitHubCompactInfoRow(
                                label = "仓库地址",
                                value = item.repoUrl,
                                valueColor = MiuixTheme.colorScheme.primary,
                                onClick = {
                                    val releaseUrl = GitHubVersionUtils.buildReleaseUrl(item.owner, item.repo)
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                                    }.onFailure {
                                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            VersionValueRow(
                                label = "检查方案",
                                value = strategyLabelForId(
                                    state.sourceStrategyId.ifBlank { lookupConfig.selectedStrategy.storageId }
                                ),
                                valueColor = MiuixTheme.colorScheme.primary
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
                            if (state.latestTag.isNotBlank()) {
                                val latestColor = state.stableVersionColor(
                                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                                VersionValueRow(
                                    label = "稳定版本",
                                    value = state.latestTag,
                                    valueColor = latestColor,
                                    emphasized = state.hasUpdate == true
                                )
                            }
                            if (state.showPreReleaseInfo && state.preReleaseInfo.isNotBlank()) {
                                val preColor = state.preReleaseVersionColor(
                                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                                VersionValueRow(
                                    label = "预发版本",
                                    value = state.preReleaseInfo,
                                    valueColor = preColor,
                                    emphasized = state.hasPreReleaseUpdate
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
            SheetDescriptionText(
                text = if (trackedItems.isEmpty()) {
                    "对比测试会使用当前已追踪仓库，最多抽取 6 个样本。当前还没有可用样本。"
                } else {
                    "对比测试会使用当前已追踪仓库做一轮冷启动和一轮缓存复测，便于直接观察 Atom、游客 API、Token API 的耗时与缓存命中差异。"
                }
            )
            if (trackedItems.isNotEmpty()) {
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetAction,
                    text = if (strategyBenchmarkRunning) "对比中..." else "运行双策略对比",
                    enabled = !strategyBenchmarkRunning,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { runStrategyBenchmark() }
                )
            }
            strategyBenchmarkError?.let { error ->
                SheetDescriptionText(
                    text = "对比测试失败：$error"
                )
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
                SheetRow {
                    SheetInputTitle("Token 状态")
                    Spacer(modifier = Modifier.weight(1f))
                    StatusPill(
                        label = tokenStatusLabel,
                        color = tokenStatusColor
                    )
                }
                SheetRow {
                    SheetInputTitle("可用性")
                    Spacer(modifier = Modifier.weight(1f))
                    StatusPill(
                        label = credentialAvailabilityLabel,
                        color = credentialAvailabilityColor
                    )
                }
                SheetInputTitle("GitHub API Token")
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
                SheetRow {
                    SheetInputTitle(
                        text = if (sanitizedTokenInput.isBlank()) {
                            "当前使用游客 API，可随时补充 token"
                        } else {
                            "当前已填写 token，可在此替换"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        variant = GlassVariant.SheetAction,
                        text = if (showApiTokenPlainText) "隐藏 Token" else "显示 Token",
                        onClick = { showApiTokenPlainText = !showApiTokenPlainText }
                    )
                }
                SheetSectionTitle("立即验证")
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetAction,
                    text = if (credentialCheckRunning) "检测中..." else "检测当前凭证",
                    enabled = !credentialCheckRunning,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { runCredentialCheck() }
                )
                SheetDescriptionText(
                    text = "未填写 token 时会自动走游客 API；适合刚开始少量追踪。若追踪项目增多或遇到限流，再补充本地 token 即可。token 仅保存在当前设备 MMKV。"
                )
                credentialCheckError?.let { error ->
                    SheetDescriptionText(
                        text = "凭证检测失败：$error"
                    )
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
                    backdrop = backdrop,
                    expanded = recommendedTokenGuideExpanded,
                    onExpandedChange = { recommendedTokenGuideExpanded = it }
                )
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
                SheetDescriptionText(
                    text = "推荐单独创建一个 KeiOS 专用 Fine-grained token，不要复用现有 classic token。这样即使 token 泄露，暴露面也更小。"
                )
            } else {
                SheetSectionTitle("方案说明")
                SheetDescriptionText(
                    text = "Atom Feed 方案无需 Token，适合公开仓库与轻量检查。若后续需要更稳定的 release 元数据或私有仓库支持，可切换到 GitHub API Token。"
                )
                StrategyBenchmarkSection()
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
        SheetContentColumn {
            GlassSearchField(
                value = repoUrlInput,
                onValueChange = { repoUrlInput = it },
                label = "GitHub 项目地址",
                backdrop = backdrop,
                variant = GlassVariant.SheetInput,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassSearchField(
                value = appSearch,
                onValueChange = { appSearch = it },
                label = "筛选本机 App（名称或包名）",
                backdrop = backdrop,
                variant = GlassVariant.SheetInput,
                singleLine = true
            )
            SheetRow {
                Text(
                    text = "已选应用: ${selectedApp?.label ?: "未选择"}",
                    color = Color(0xFF3A8DFF)
                )
                GlassTextButton(
                    backdrop = backdrop,
                    variant = GlassVariant.SheetAction,
                    text = if (pickerExpanded) "收起列表" else "选择应用",
                    onClick = { pickerExpanded = !pickerExpanded }
                )
            }
            SheetRow {
                Text(
                    text = "检查预发行版本",
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = checkPreReleaseInput,
                    onCheckedChange = { checked -> checkPreReleaseInput = checked }
                )
            }
            if (pickerExpanded) {
                val filteredApps = appList.filter { app ->
                    appSearch.isBlank() ||
                        app.label.contains(appSearch, ignoreCase = true) ||
                        app.packageName.contains(appSearch, ignoreCase = true)
                }.take(80)
                if (filteredApps.isEmpty()) {
                    MiuixInfoItem("应用列表", "没有匹配结果")
                } else {
                    filteredApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedApp = app
                                    pickerExpanded = false
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppIcon(packageName = app.packageName, size = 30.dp)
                            Text(
                                text = "${app.label} · ${app.packageName}",
                                color = Color(0xFF3A8DFF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            if (editingTrackedItem != null) {
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
                    onClick = { pendingDeleteItem = null }
                )
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = "删除",
                    colors = ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = MiuixTheme.colorScheme.onError
                    ),
                    onClick = {
                        pendingDeleteItem?.let { deleting ->
                            trackedItems.remove(deleting)
                            checkStates.remove(deleting.id)
                            saveTracked()
                            persistCheckCache()
                            Toast.makeText(context, "已删除 ${deleting.appLabel}", Toast.LENGTH_SHORT).show()
                        }
                        pendingDeleteItem = null
                    }
                )
            }
        }
    }
}
