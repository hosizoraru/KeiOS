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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LiquidActionBarPopupAnchors
import com.example.keios.ui.page.main.widget.MiuixAccordionCard
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SheetContentColumn
import com.example.keios.ui.page.main.widget.SheetRow
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.SnapshotWindowListPopup
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.feature.github.data.local.AppIconCache
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.model.GitHubCheckCacheEntry
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
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
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
        latestPreRawTag = latestPreRawTag,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreRelease,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate
    )

    fun GitHubCheckCacheEntry.toUi(): VersionCheckUi = VersionCheckUi(
        loading = false,
        localVersion = localVersion,
        localVersionCode = localVersionCode,
        latestTag = latestTag,
        latestStableRawTag = latestStableRawTag,
        latestPreRawTag = latestPreRawTag,
        hasUpdate = hasUpdate,
        message = message,
        isPreRelease = isPreRelease,
        preReleaseInfo = preReleaseInfo,
        showPreReleaseInfo = showPreReleaseInfo,
        hasPreReleaseUpdate = hasPreReleaseUpdate
    )

    fun persistCheckCache(refreshTimestamp: Long = lastRefreshMs) {
        val states = trackedItems.associate { item ->
            val state = checkStates[item.id] ?: VersionCheckUi()
            item.id to state.toCacheEntry()
        }
        GitHubTrackStore.saveCheckCache(states, refreshTimestamp)
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
                val preReleaseCheckEnabled = item.checkPreRelease
                val local = runCatching { GitHubVersionUtils.localVersionName(context, item.packageName) }
                    .getOrDefault("unknown")
                val localVersionCode = runCatching { GitHubVersionUtils.localVersionCode(context, item.packageName) }
                    .getOrDefault(-1L)
                val atomEntries = GitHubVersionUtils.fetchReleaseEntriesFromAtom(item.owner, item.repo)
                    .getOrDefault(emptyList())
                val matchedEntry = atomEntries.firstOrNull {
                    GitHubVersionUtils.compareVersionToCandidates(local, it.candidates) == 0
                }
                val latestPreEntry = atomEntries.firstOrNull { it.isLikelyPreRelease }
                val stableResult = GitHubVersionUtils.fetchLatestReleaseSignals(
                    owner = item.owner,
                    repo = item.repo,
                    atomEntriesHint = atomEntries
                )

                stableResult.fold(
                    onSuccess = { signals ->
                        val cmp = GitHubVersionUtils.compareVersionToCandidates(local, signals.candidates)
                        val latestPreLabel = latestPreEntry?.tag?.ifBlank {
                            latestPreEntry.title
                        }.orEmpty()
                        val preVsStable = if (latestPreLabel.isNotBlank()) {
                            GitHubVersionUtils.compareVersionToCandidates(latestPreLabel, signals.candidates)
                        } else {
                            null
                        }
                        val preRelevant = latestPreEntry != null && (preVsStable == null || preVsStable > 0)
                        val localIsPreReleaseInstalled = matchedEntry?.isLikelyPreRelease == true
                        val preCmp = if (latestPreEntry != null) {
                            GitHubVersionUtils.compareVersionToCandidates(local, latestPreEntry.candidates)
                        } else {
                            null
                        }
                        val hasPreReleaseUpdate = preReleaseCheckEnabled &&
                            localIsPreReleaseInstalled &&
                            preRelevant &&
                            (preCmp?.let { it < 0 } == true)
                        val stableHasUpdate = cmp?.let { it < 0 } == true
                        val hasUpdate = stableHasUpdate || hasPreReleaseUpdate
                        val message = when {
                            hasPreReleaseUpdate -> "预发有更新"
                            stableHasUpdate -> "发现更新"
                            preReleaseCheckEnabled && localIsPreReleaseInstalled && preRelevant -> "预发行"
                            hasUpdate == false -> "已是最新"
                            else -> "版本格式无法精确比较"
                        }
                        val preInfo = when {
                            preReleaseCheckEnabled && localIsPreReleaseInstalled && preRelevant ->
                                matchedEntry.let { entry -> entry.title.ifBlank { entry.tag } }
                            preReleaseCheckEnabled && preRelevant ->
                                latestPreEntry.title.ifBlank { latestPreEntry.tag }
                            else -> ""
                        }
                        VersionCheckUi(
                            loading = false,
                            localVersion = local,
                            localVersionCode = localVersionCode,
                            latestTag = signals.displayVersion,
                            latestStableRawTag = signals.rawTag,
                            latestPreRawTag = latestPreEntry?.tag.orEmpty(),
                            hasUpdate = hasUpdate,
                            message = message,
                            isPreRelease = preReleaseCheckEnabled && localIsPreReleaseInstalled && preRelevant,
                            preReleaseInfo = preInfo,
                            showPreReleaseInfo = preReleaseCheckEnabled && preInfo.isNotBlank(),
                            hasPreReleaseUpdate = hasPreReleaseUpdate
                        )
                    },
                    onFailure = { err ->
                        if (matchedEntry != null) {
                            val localIsPreRelease = matchedEntry.isLikelyPreRelease
                            val latestPre = latestPreEntry?.takeIf { entry ->
                                GitHubVersionUtils.compareVersionToCandidates(
                                    entry.title.ifBlank { entry.tag },
                                    matchedEntry.candidates
                                )?.let { it > 0 } == true
                            }?.takeIf { preReleaseCheckEnabled }
                            VersionCheckUi(
                                loading = false,
                                localVersion = local,
                                localVersionCode = localVersionCode,
                                latestTag = matchedEntry.title.ifBlank { matchedEntry.tag },
                                latestStableRawTag = matchedEntry.tag,
                                latestPreRawTag = latestPre?.tag.orEmpty(),
                                hasUpdate = latestPre != null,
                                message = when {
                                    latestPre != null -> "预发有更新"
                                    preReleaseCheckEnabled && localIsPreRelease -> "预发行"
                                    else -> "已匹配发行"
                                },
                                isPreRelease = preReleaseCheckEnabled && localIsPreRelease,
                                preReleaseInfo = when {
                                    latestPre != null -> latestPre.title.ifBlank { latestPre.tag }
                                    preReleaseCheckEnabled && localIsPreRelease -> matchedEntry.title.ifBlank { matchedEntry.tag }
                                    else -> ""
                                },
                                showPreReleaseInfo = preReleaseCheckEnabled && (localIsPreRelease || latestPre != null),
                                hasPreReleaseUpdate = latestPre != null
                            )
                        } else {
                            VersionCheckUi(
                                loading = false,
                                localVersion = local,
                                localVersionCode = localVersionCode,
                                message = "检查失败: ${err.message ?: "unknown"}",
                                preReleaseInfo = "",
                                showPreReleaseInfo = false,
                                hasPreReleaseUpdate = false
                            )
                        }
                    }
                )
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
                checkStates[item.id] = VersionCheckUi(loading = true, message = "刷新中...")
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
        trackedItems.clear()
        trackedItems.addAll(GitHubTrackStore.load())
        reloadApps()
        val (cachedStates, cachedRefreshMs) = GitHubTrackStore.loadCheckCache()
        checkStates.clear()
        trackedItems.forEach { item ->
            cachedStates[item.id]?.let { cached -> checkStates[item.id] = cached.toUi() }
        }
        lastRefreshMs = cachedRefreshMs
        refreshIntervalHours = GitHubTrackStore.loadRefreshIntervalHours()
        val hasTracked = trackedItems.isNotEmpty()
        val hasCachedForTracked = trackedItems.any { cachedStates.containsKey(it.id) }
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

                            LiquidActionBarPopupAnchors(itemCount = 4) { slotIndex ->
                                when (slotIndex) {
                                    0 -> if (showSortPopup) {
                                        SnapshotWindowListPopup(
                                            show = showSortPopup,
                                            alignment = PopupPositionProvider.Align.BottomStart,
                                            onDismissRequest = { showSortPopup = false },
                                            enableWindowDim = false
                                        ) {
                                            ListPopupColumn {
                                                val modes = GitHubSortMode.entries
                                                modes.forEachIndexed { index, mode ->
                                                    DropdownImpl(
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

                                    1 -> if (showIntervalPopup) {
                                        SnapshotWindowListPopup(
                                            show = showIntervalPopup,
                                            alignment = PopupPositionProvider.Align.BottomStart,
                                            onDismissRequest = { showIntervalPopup = false },
                                            enableWindowDim = false
                                        ) {
                                            ListPopupColumn {
                                                val options = RefreshIntervalOption.entries
                                                val selected = RefreshIntervalOption.fromHours(refreshIntervalHours)
                                                options.forEachIndexed { index, option ->
                                                    DropdownImpl(
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
                    bottomBarStyle = true,
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
                    color = when (overviewRefreshState) {
                        OverviewRefreshState.Cached -> if (isDark) {
                            androidx.compose.ui.graphics.Color(0x55F59E0B)
                        } else {
                            androidx.compose.ui.graphics.Color(0x33F59E0B)
                        }
                        OverviewRefreshState.Refreshing -> if (isDark) {
                            androidx.compose.ui.graphics.Color(0x553B82F6)
                        } else {
                            androidx.compose.ui.graphics.Color(0x333B82F6)
                        }
                        OverviewRefreshState.Completed -> if (isDark) {
                            androidx.compose.ui.graphics.Color(0x5522C55E)
                        } else {
                            androidx.compose.ui.graphics.Color(0x3322C55E)
                        }
                        OverviewRefreshState.Idle -> MiuixTheme.colorScheme.surface.copy(alpha = 0.66f)
                    },
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
                        val refreshColor = when (overviewRefreshState) {
                            OverviewRefreshState.Refreshing -> Color(0xFF3B82F6)
                            OverviewRefreshState.Completed -> Color(0xFF22C55E)
                            OverviewRefreshState.Cached -> Color(0xFFF59E0B)
                            OverviewRefreshState.Idle -> MiuixTheme.colorScheme.onBackgroundVariant
                        }
                        Text("项目版本跟踪", color = MiuixTheme.colorScheme.onBackground)
                        Text(
                            text = formatRefreshAgo(lastRefreshMs),
                            color = refreshColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            label = when (overviewRefreshState) {
                                OverviewRefreshState.Cached -> "Cache"
                                OverviewRefreshState.Refreshing -> "Refreshing"
                                OverviewRefreshState.Completed -> "Synced"
                                OverviewRefreshState.Idle -> "Idle"
                            },
                            color = when (overviewRefreshState) {
                                OverviewRefreshState.Cached -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
                                OverviewRefreshState.Refreshing -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                OverviewRefreshState.Completed -> androidx.compose.ui.graphics.Color(0xFF22C55E)
                                OverviewRefreshState.Idle -> MiuixTheme.colorScheme.onBackgroundVariant
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            "追踪 $trackedCount 项",
                            color = if (trackedCount > 0) MiuixTheme.colorScheme.onBackground else MiuixTheme.colorScheme.onBackgroundVariant
                        )
                        Text("·", color = MiuixTheme.colorScheme.onBackgroundVariant)
                        Text(
                            "可更新 $updatableCount 项",
                            color = if (updatableCount > 0) Color(0xFF22C55E) else MiuixTheme.colorScheme.onBackgroundVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                "最新稳定版 $stableLatestCount 项",
                                color = if (stableLatestCount > 0) Color(0xFF3B82F6) else MiuixTheme.colorScheme.onBackgroundVariant
                            )
                            Text("·", color = MiuixTheme.colorScheme.onBackgroundVariant)
                            Text(
                                "预发行 $preReleaseCount 项",
                                color = if (preReleaseCount > 0) Color(0xFFF59E0B) else MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        }
                        if (overviewRefreshState != OverviewRefreshState.Idle) {
                            val indicatorColor = when (overviewRefreshState) {
                                OverviewRefreshState.Refreshing -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                OverviewRefreshState.Completed -> androidx.compose.ui.graphics.Color(0xFF22C55E)
                                OverviewRefreshState.Cached -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
                                OverviewRefreshState.Idle -> MiuixTheme.colorScheme.onBackgroundVariant
                            }
                            val indicatorBg = when (overviewRefreshState) {
                                OverviewRefreshState.Refreshing -> androidx.compose.ui.graphics.Color(0x553B82F6)
                                OverviewRefreshState.Completed -> androidx.compose.ui.graphics.Color(0x5522C55E)
                                OverviewRefreshState.Cached -> androidx.compose.ui.graphics.Color(0x55F59E0B)
                                OverviewRefreshState.Idle -> MiuixTheme.colorScheme.surface
                            }
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
                            val statusIcon = when {
                                state.loading -> MiuixIcons.Regular.Refresh
                                state.hasPreReleaseUpdate -> MiuixIcons.Regular.Update
                                state.isPreRelease -> MiuixIcons.Regular.Report
                                state.hasUpdate == true -> MiuixIcons.Regular.Update
                                state.hasUpdate == false -> MiuixIcons.Regular.Ok
                                else -> MiuixIcons.Regular.More
                            }
                            val statusColor = when {
                                state.loading -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                state.hasPreReleaseUpdate -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                state.hasUpdate == true -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                state.isPreRelease && state.hasUpdate == false -> androidx.compose.ui.graphics.Color(0xFF22C55E)
                                state.hasUpdate == false -> androidx.compose.ui.graphics.Color(0xFF22C55E)
                                else -> MiuixTheme.colorScheme.onBackgroundVariant
                            }
                            val clickableModifier = if (state.hasUpdate == true || state.isPreRelease) {
                                Modifier.clickable {
                                    val releaseUrl = when {
                                        state.hasPreReleaseUpdate && state.latestPreRawTag.isNotBlank() ->
                                            GitHubVersionUtils.buildReleaseTagUrl(item.owner, item.repo, state.latestPreRawTag)
                                        state.hasUpdate == true && state.latestStableRawTag.isNotBlank() ->
                                            GitHubVersionUtils.buildReleaseTagUrl(item.owner, item.repo, state.latestStableRawTag)
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
                                    Toast.makeText(context, "已刷新 ${item.appLabel}", Toast.LENGTH_SHORT).show()
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
                            if (state.localVersion.isNotBlank()) {
                                val localText = if (state.localVersionCode >= 0L) {
                                    "${state.localVersion} (${state.localVersionCode})"
                                } else {
                                    state.localVersion
                                }
                                VersionValueRow(
                                    label = "本地",
                                    value = localText,
                                    valueColor = MiuixTheme.colorScheme.primary
                                )
                            }
                            if (state.latestTag.isNotBlank()) {
                                val latestColor = if (state.hasUpdate == true) {
                                    MiuixTheme.colorScheme.error
                                } else {
                                    MiuixTheme.colorScheme.secondary
                                }
                                VersionValueRow(
                                    label = "稳定",
                                    value = state.latestTag,
                                    valueColor = latestColor,
                                    emphasized = state.hasUpdate == true
                                )
                            }
                            if (state.showPreReleaseInfo && state.preReleaseInfo.isNotBlank()) {
                                val preColor = if (state.hasPreReleaseUpdate) {
                                    MiuixTheme.colorScheme.error
                                } else {
                                    MiuixTheme.colorScheme.secondary
                                }
                                VersionValueRow(
                                    label = "预发",
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
                bottomBarStyle = true,
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
                bottomBarStyle = true,
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
                bottomBarStyle = true,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassSearchField(
                value = appSearch,
                onValueChange = { appSearch = it },
                label = "筛选本机 App（名称或包名）",
                backdrop = backdrop,
                bottomBarStyle = true,
                singleLine = true
            )
            SheetRow {
                Text(
                    text = "已选应用: ${selectedApp?.label ?: "未选择"}",
                    color = Color(0xFF3A8DFF)
                )
                GlassTextButton(
                    backdrop = backdrop,
                    bottomBarStyle = true,
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
                    bottomBarStyle = true,
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
