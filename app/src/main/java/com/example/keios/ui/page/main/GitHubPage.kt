package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.GitHubTrackStore
import com.example.keios.ui.utils.GitHubTrackedApp
import com.example.keios.ui.utils.GitHubVersionUtils
import com.example.keios.ui.utils.InstalledAppItem
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class VersionCheckUi(
    val loading: Boolean = false,
    val localVersion: String = "",
    val latestTag: String = "",
    val hasUpdate: Boolean? = null,
    val message: String = "",
    val isPreRelease: Boolean = false,
    val preReleaseInfo: String = "",
    val showPreReleaseInfo: Boolean = false
)

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
    var addSectionExpanded by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var appListLoaded by remember { mutableStateOf(false) }
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }

    val trackedItems = remember { mutableStateListOf<GitHubTrackedApp>() }
    val checkStates = remember { mutableStateMapOf<String, VersionCheckUi>() }

    suspend fun reloadApps() {
        appList = withContext(Dispatchers.IO) {
            GitHubVersionUtils.queryInstalledLaunchableApps(context)
        }
        appListLoaded = true
    }

    fun refreshItem(item: GitHubTrackedApp, showToastOnError: Boolean = false) {
        scope.launch {
            checkStates[item.id] = VersionCheckUi(loading = true)
            val state = withContext(Dispatchers.IO) {
                val local = runCatching { GitHubVersionUtils.localVersionName(context, item.packageName) }
                    .getOrDefault("unknown")
                val atomEntries = GitHubVersionUtils.fetchReleaseEntriesFromAtom(item.owner, item.repo)
                    .getOrDefault(emptyList())
                val matchedEntry = atomEntries.firstOrNull {
                    GitHubVersionUtils.compareVersionToCandidates(local, it.candidates) == 0
                }
                val latestPreEntry = atomEntries.firstOrNull { it.isLikelyPreRelease }
                val stableResult = GitHubVersionUtils.fetchLatestReleaseSignals(item.owner, item.repo)

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
                        val localIsPreRelease = matchedEntry?.isLikelyPreRelease == true && cmp != 0 && preRelevant
                        val hasUpdate = if (localIsPreRelease) false else cmp?.let { it < 0 }
                        val message = when {
                            localIsPreRelease -> "预发行"
                            hasUpdate == true -> "发现更新"
                            hasUpdate == false -> "已是最新"
                            else -> "版本格式无法精确比较"
                        }
                        val preInfo = when {
                            localIsPreRelease -> matchedEntry.let { entry -> entry.title.ifBlank { entry.tag } }
                            preRelevant -> latestPreEntry.title.ifBlank { latestPreEntry.tag }
                            else -> ""
                        }
                        VersionCheckUi(
                            loading = false,
                            localVersion = local,
                            latestTag = signals.displayVersion,
                            hasUpdate = hasUpdate,
                            message = message,
                            isPreRelease = localIsPreRelease,
                            preReleaseInfo = preInfo,
                            showPreReleaseInfo = preInfo.isNotBlank()
                        )
                    },
                    onFailure = { err ->
                        if (matchedEntry != null) {
                            val localIsPreRelease = matchedEntry.isLikelyPreRelease
                            VersionCheckUi(
                                loading = false,
                                localVersion = local,
                                latestTag = matchedEntry.title.ifBlank { matchedEntry.tag },
                                hasUpdate = false,
                                message = if (localIsPreRelease) "预发行" else "已匹配发行",
                                isPreRelease = localIsPreRelease,
                                preReleaseInfo = if (localIsPreRelease) matchedEntry.title.ifBlank { matchedEntry.tag } else "",
                                showPreReleaseInfo = localIsPreRelease
                            )
                        } else {
                            if (showToastOnError) {
                                Toast.makeText(context, "检查失败: ${err.message ?: "unknown"}", Toast.LENGTH_SHORT).show()
                            }
                            VersionCheckUi(
                                loading = false,
                                message = "检查失败: ${err.message ?: "unknown"}",
                                preReleaseInfo = "",
                                showPreReleaseInfo = false
                            )
                        }
                    }
                )
            }
            checkStates[item.id] = state
        }
    }

    fun refreshAllTracked(showToast: Boolean = true) {
        if (trackedItems.isEmpty()) {
            if (showToast) Toast.makeText(context, "暂无可检查条目", Toast.LENGTH_SHORT).show()
            return
        }
        trackedItems.forEach { refreshItem(it, showToastOnError = showToast) }
        if (showToast) Toast.makeText(context, "已开始检查全部跟踪", Toast.LENGTH_SHORT).show()
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "GitHub",
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp)
            )
            Button(
                modifier = Modifier.padding(top = 2.dp),
                onClick = { refreshAllTracked(showToast = true) }
            ) {
                Text("检查")
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
            MiuixExpandableSection(
                backdrop = backdrop,
                title = "新增跟踪",
                subtitle = "输入 GitHub 仓库并绑定本机 App",
                expanded = addSectionExpanded,
                onExpandedChange = { addSectionExpanded = it }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusPill(
                        label = if (!appListLoaded) "应用列表读取中" else if (appList.isNotEmpty()) "应用列表可用" else "应用列表受限",
                        color = if (appList.isNotEmpty()) MiuixTheme.colorScheme.secondary else MiuixTheme.colorScheme.error
                    )
                    Button(onClick = { scope.launch { reloadApps() } }) { Text("刷新列表") }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                    Button(onClick = { pickerExpanded = !pickerExpanded }) {
                        Text(if (pickerExpanded) "收起列表" else "选择应用")
                    }
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
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(app.label, color = MiuixTheme.colorScheme.onBackground)
                                    Text(app.packageName, color = MiuixTheme.colorScheme.onBackgroundVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val app = selectedApp
                        val parsed = GitHubVersionUtils.parseOwnerRepo(repoUrlInput)
                        if (app == null || parsed == null) {
                            Toast.makeText(context, "请填写正确仓库并选择 App", Toast.LENGTH_SHORT).show()
                            return@Button
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
                            return@Button
                        }
                        trackedItems.add(item)
                        saveTracked()
                        refreshItem(item, showToastOnError = true)
                        repoUrlInput = ""
                        addSectionExpanded = false
                        Toast.makeText(context, "已新增跟踪", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("新增并检查")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (trackedItems.isEmpty()) {
                MiuixInfoItem("跟踪列表", "暂无条目，请先新增")
            } else if (filteredTracked.isEmpty()) {
                MiuixInfoItem("搜索结果", "没有匹配的跟踪项目")
            } else {
                filteredTracked.forEach { item ->
                    var expanded by remember(item.id) { mutableStateOf(false) }
                    MiuixExpandableSection(
                        backdrop = backdrop,
                        title = "${item.owner}/${item.repo}",
                        subtitle = item.appLabel,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        headerActions = {
                            val state = checkStates[item.id] ?: VersionCheckUi()
                            val statusText = when {
                                state.loading -> "检查中"
                                state.isPreRelease -> "预发行"
                                state.hasUpdate == true -> "有更新"
                                state.hasUpdate == false -> "最新"
                                else -> "待检查"
                            }
                            val statusColor = when {
                                state.loading -> MiuixTheme.colorScheme.onBackgroundVariant
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
                                modifier = clickableModifier
                            )
                        }
                    ) {
                        val state = checkStates[item.id] ?: VersionCheckUi()
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
                        MiuixInfoItem("应用包名", item.packageName)
                        if (state.localVersion.isNotBlank()) {
                            Row {
                                Text("本地 ", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                Text(state.localVersion, color = MiuixTheme.colorScheme.primary)
                            }
                        }
                        if (state.latestTag.isNotBlank()) {
                            Row {
                                Text("稳定 ", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                Text(state.latestTag, color = MiuixTheme.colorScheme.secondary)
                            }
                        }
                        if (state.showPreReleaseInfo && state.preReleaseInfo.isNotBlank()) {
                            Row {
                                Text("预发 ", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                Text(state.preReleaseInfo, color = MiuixTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { refreshItem(item, showToastOnError = true) }) {
                                Text("刷新检查")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    trackedItems.remove(item)
                                    checkStates.remove(item.id)
                                    saveTracked()
                                }
                            ) {
                                Text("删除")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
