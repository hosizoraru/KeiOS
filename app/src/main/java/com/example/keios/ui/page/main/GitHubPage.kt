package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.AppIconCache
import com.example.keios.ui.utils.GitHubTrackStore
import com.example.keios.ui.utils.GitHubTrackedApp
import com.example.keios.ui.utils.GitHubVersionUtils
import com.example.keios.ui.utils.InstalledAppItem
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup

private data class VersionCheckUi(
    val loading: Boolean = false,
    val localVersion: String = "",
    val localVersionCode: Long = -1L,
    val latestTag: String = "",
    val hasUpdate: Boolean? = null,
    val message: String = "",
    val isPreRelease: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false,
    val hasPreReleaseUpdate: Boolean = false
)

private enum class GitHubSortMode(val label: String) {
    UpdateFirst("更新优先"),
    NameAsc("名称 A-Z"),
    PreReleaseFirst("预发行优先")
}

@Composable
fun GitHubPage(
    backdrop: Backdrop?,
    contentBottomPadding: Dp = 72.dp,
    scrollToTopSignal: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var trackedSearch by remember { mutableStateOf("") }
    var repoUrlInput by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }
    var pickerExpanded by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var appListLoaded by remember { mutableStateOf(false) }
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }
    var showSortPopup by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(GitHubSortMode.UpdateFirst) }
    var pendingDeleteItem by remember { mutableStateOf<GitHubTrackedApp?>(null) }

    val trackedItems = remember { mutableStateListOf<GitHubTrackedApp>() }
    val checkStates = remember { mutableStateMapOf<String, VersionCheckUi>() }

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
                        val latestPreLabel = latestPreEntry?.title?.ifBlank { latestPreEntry.tag }.orEmpty()
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
                        val hasPreReleaseUpdate = localIsPreReleaseInstalled && preRelevant && (preCmp?.let { it < 0 } == true)
                        val stableHasUpdate = cmp?.let { it < 0 } == true
                        val hasUpdate = hasPreReleaseUpdate || stableHasUpdate
                        val message = when {
                            hasPreReleaseUpdate -> "预发有更新"
                            stableHasUpdate -> "发现更新"
                            localIsPreReleaseInstalled && preRelevant -> "预发行"
                            hasUpdate == false -> "已是最新"
                            else -> "版本格式无法精确比较"
                        }
                        val preInfo = when {
                            localIsPreReleaseInstalled && preRelevant -> matchedEntry.let { entry -> entry.title.ifBlank { entry.tag } }
                            preRelevant -> latestPreEntry.title.ifBlank { latestPreEntry.tag }
                            else -> ""
                        }
                        VersionCheckUi(
                            loading = false,
                            localVersion = local,
                            localVersionCode = localVersionCode,
                            latestTag = signals.displayVersion,
                            hasUpdate = hasUpdate,
                            message = message,
                            isPreRelease = localIsPreReleaseInstalled && preRelevant,
                            preReleaseInfo = preInfo,
                            showPreReleaseInfo = preInfo.isNotBlank(),
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
                            }
                            VersionCheckUi(
                                loading = false,
                                localVersion = local,
                                localVersionCode = localVersionCode,
                                latestTag = matchedEntry.title.ifBlank { matchedEntry.tag },
                                hasUpdate = latestPre != null,
                                message = when {
                                    latestPre != null -> "预发有更新"
                                    localIsPreRelease -> "预发行"
                                    else -> "已匹配发行"
                                },
                                isPreRelease = localIsPreRelease,
                                preReleaseInfo = when {
                                    latestPre != null -> latestPre.title.ifBlank { latestPre.tag }
                                    localIsPreRelease -> matchedEntry.title.ifBlank { matchedEntry.tag }
                                    else -> ""
                                },
                                showPreReleaseInfo = localIsPreRelease || latestPre != null,
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
        }
    }

    fun refreshAllTracked(showToast: Boolean = true) {
        if (trackedItems.isEmpty()) {
            if (showToast) Toast.makeText(context, "暂无可检查条目", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            trackedItems.forEachIndexed { index, item ->
                checkStates[item.id] = VersionCheckUi(loading = true)
                val state = resolveItemState(item)
                checkStates[item.id] = state
                if (showToast && state.message.startsWith("检查失败")) {
                    Toast.makeText(context, "${item.owner}/${item.repo}: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                if (index < trackedItems.lastIndex) delay(120)
            }
            if (showToast) Toast.makeText(context, "检查完成", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveTracked() {
        GitHubTrackStore.save(trackedItems.toList())
    }

    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { reloadApps() }
        }

    LaunchedEffect(Unit) {
        trackedItems.clear()
        trackedItems.addAll(GitHubTrackStore.load())
        reloadApps()
        refreshAllTracked(showToast = false)
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) scrollState.animateScrollTo(0)
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "GitHub",
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row {
                Box(modifier = Modifier.padding(top = 2.dp)) {
                    GlassIconButton(
                        backdrop = backdrop,
                        icon = MiuixIcons.Regular.Sort,
                        contentDescription = "排序",
                        onClick = { showSortPopup = !showSortPopup }
                    )
                    if (showSortPopup) {
                        WindowListPopup(
                            show = showSortPopup,
                            alignment = PopupPositionProvider.Align.End,
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
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.Refresh,
                    contentDescription = "检查",
                    onClick = { refreshAllTracked(showToast = true) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlassIconButton(
                    backdrop = backdrop,
                    icon = MiuixIcons.Regular.AddCircle,
                    contentDescription = "新增跟踪",
                    onClick = { showAddSheet = true },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Text(
            text = "项目版本跟踪",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = trackedSearch,
            onValueChange = { trackedSearch = it },
            label = "搜索已跟踪项目（仓库/应用/包名）",
            useLabelAsPlaceholder = true,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = contentBottomPadding)
        ) {
            if (trackedItems.isEmpty()) {
                MiuixInfoItem("跟踪列表", "暂无条目，请先新增")
            } else if (filteredTracked.isEmpty()) {
                MiuixInfoItem("搜索结果", "没有匹配的跟踪项目")
            } else {
                sortedTracked.forEach { item ->
                    var expanded by remember(item.id) { mutableStateOf(false) }
                    MiuixExpandableSection(
                        backdrop = backdrop,
                        title = item.appLabel,
                        subtitle = "${item.owner}/${item.repo}",
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        headerStartAction = {
                            AppIcon(packageName = item.packageName, size = 24.dp)
                        },
                        onHeaderLongClick = { pendingDeleteItem = item },
                        headerActions = {
                            val state = checkStates[item.id] ?: VersionCheckUi()
                            val statusText = when {
                                state.loading -> "⏳"
                                state.hasPreReleaseUpdate -> "🧪⬆"
                                state.isPreRelease -> "🧪"
                                state.hasUpdate == true -> "⬆"
                                state.hasUpdate == false -> "✅"
                                else -> "•"
                            }
                            val statusColor = when {
                                state.loading -> MiuixTheme.colorScheme.onBackgroundVariant
                                state.hasPreReleaseUpdate -> MiuixTheme.colorScheme.error
                                state.isPreRelease -> MiuixTheme.colorScheme.secondary
                                state.hasUpdate == true -> MiuixTheme.colorScheme.error
                                state.hasUpdate == false -> MiuixTheme.colorScheme.secondary
                                else -> MiuixTheme.colorScheme.onBackgroundVariant
                            }
                            val clickableModifier = if (state.hasUpdate == true || state.isPreRelease) {
                                Modifier.clickable {
                                    val releaseUrl = GitHubVersionUtils.buildReleaseUrl(item.owner, item.repo)
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                                    }.onFailure {
                                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Modifier
                            }
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = clickableModifier
                            )
                        }
                    ) {
                        val state = checkStates[item.id] ?: VersionCheckUi()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MiuixInfoItem(
                                "应用包名（点击刷新）",
                                item.packageName,
                                onClick = {
                                    refreshItem(item, showToastOnError = true)
                                    Toast.makeText(context, "已刷新 ${item.appLabel}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            MiuixInfoItem(
                                "仓库地址",
                                item.repoUrl,
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
                                Row {
                                    Text("本地 ", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                    val localText = if (state.localVersionCode >= 0L) {
                                        "${state.localVersion} (${state.localVersionCode})"
                                    } else {
                                        state.localVersion
                                    }
                                    Text(
                                        localText,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (state.latestTag.isNotBlank()) {
                                val latestColor = if (state.hasUpdate == true) {
                                    MiuixTheme.colorScheme.error
                                } else {
                                    MiuixTheme.colorScheme.secondary
                                }
                                Row {
                                    Text("稳定 ", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                    Text(
                                        state.latestTag,
                                        color = latestColor,
                                        fontWeight = if (state.hasUpdate == true) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                            if (state.showPreReleaseInfo && state.preReleaseInfo.isNotBlank()) {
                                val preColor = if (state.hasPreReleaseUpdate) {
                                    MiuixTheme.colorScheme.error
                                } else {
                                    MiuixTheme.colorScheme.secondary
                                }
                                Row {
                                    Text("预发 ", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                    Text(
                                        state.preReleaseInfo,
                                        color = preColor,
                                        fontWeight = if (state.hasPreReleaseUpdate) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    WindowBottomSheet(
        show = showAddSheet,
        title = "新增跟踪",
        onDismissRequest = { showAddSheet = false },
        startAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Close,
                contentDescription = "关闭",
                onClick = { showAddSheet = false }
            )
        },
        endAction = {
            GlassIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Ok,
                contentDescription = "确认新增",
                onClick = {
                    val app = selectedApp
                    val parsed = GitHubVersionUtils.parseOwnerRepo(repoUrlInput)
                    if (app == null || parsed == null) {
                        Toast.makeText(context, "请填写正确仓库并选择 App", Toast.LENGTH_SHORT).show()
                        return@GlassIconButton
                    }
                    val item = GitHubTrackedApp(
                        repoUrl = repoUrlInput.trim(),
                        owner = parsed.first,
                        repo = parsed.second,
                        packageName = app.packageName,
                        appLabel = app.label
                    )
                    if (trackedItems.any { it.id == item.id }) {
                        Toast.makeText(context, "该条目已存在", Toast.LENGTH_SHORT).show()
                        return@GlassIconButton
                    }
                    trackedItems.add(item)
                    saveTracked()
                    refreshItem(item, showToastOnError = true)
                    repoUrlInput = ""
                    showAddSheet = false
                    Toast.makeText(context, "已新增跟踪", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = repoUrlInput,
                onValueChange = { repoUrlInput = it },
                label = "GitHub 项目地址",
                useLabelAsPlaceholder = true,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = appSearch,
                onValueChange = { appSearch = it },
                label = "筛选本机 App（名称或包名）",
                useLabelAsPlaceholder = true,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "已选应用: ${selectedApp?.label ?: "未选择"}",
                    color = MiuixTheme.colorScheme.onBackgroundVariant
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = if (pickerExpanded) "收起列表" else "选择应用",
                    onClick = { pickerExpanded = !pickerExpanded }
                )
            }
            if (pickerExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
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
                                color = MiuixTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    WindowDialog(
        show = pendingDeleteItem != null,
        title = "删除跟踪",
        summary = pendingDeleteItem?.let { "确定删除 ${it.appLabel} (${it.owner}/${it.repo}) 吗？" },
        onDismissRequest = { pendingDeleteItem = null }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            GlassTextButton(
                backdrop = null,
                text = "取消",
                onClick = { pendingDeleteItem = null }
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassTextButton(
                backdrop = null,
                text = "删除",
                onClick = {
                    pendingDeleteItem?.let { deleting ->
                        trackedItems.remove(deleting)
                        checkStates.remove(deleting.id)
                        saveTracked()
                        Toast.makeText(context, "已删除 ${deleting.appLabel}", Toast.LENGTH_SHORT).show()
                    }
                    pendingDeleteItem = null
                }
            )
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    size: Dp
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = AppIconCache.get(packageName), packageName) {
        value = withContext(Dispatchers.IO) { AppIconCache.getOrLoad(context, packageName) }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = packageName,
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule)
        )
    } else {
        Box(
            modifier = Modifier
                .width(size)
                .height(size)
                .clip(ContinuousCapsule),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("App", color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
    }
}
