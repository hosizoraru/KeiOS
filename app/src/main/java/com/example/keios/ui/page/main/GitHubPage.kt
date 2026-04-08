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
import com.example.keios.ui.page.main.widget.FrostedBlock
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
    val message: String = ""
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
    var repoUrlInput by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }
    var pickerExpanded by remember { mutableStateOf(false) }
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

    val appListPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            scope.launch { reloadApps() }
        }

    LaunchedEffect(Unit) {
        trackedItems.clear()
        trackedItems.addAll(GitHubTrackStore.load())
        reloadApps()
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

    fun refreshItem(item: GitHubTrackedApp) {
        scope.launch {
            checkStates[item.id] = VersionCheckUi(loading = true)
            val result = withContext(Dispatchers.IO) {
                val local = runCatching { GitHubVersionUtils.localVersionName(context, item.packageName) }.getOrDefault("unknown")
                GitHubVersionUtils.fetchLatestTag(item.owner, item.repo).mapCatching { latest ->
                    val cmp = GitHubVersionUtils.compareVersion(local, latest)
                    Triple(local, latest, cmp)
                }
            }
            val state = result.fold(
                onSuccess = { (local, latest, cmp) ->
                    val hasUpdate = cmp?.let { it < 0 }
                    val message = when (hasUpdate) {
                        true -> "发现更新"
                        false -> "已是最新"
                        null -> "版本格式无法精确比较"
                    }
                    VersionCheckUi(
                        loading = false,
                        localVersion = local,
                        latestTag = latest,
                        hasUpdate = hasUpdate,
                        message = message
                    )
                },
                onFailure = {
                    VersionCheckUi(
                        loading = false,
                        message = "检查失败: ${it.message ?: "unknown"}"
                    )
                }
            )
            checkStates[item.id] = state
        }
    }

    fun saveTracked() {
        GitHubTrackStore.save(trackedItems.toList())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = contentBottomPadding)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "GitHub",
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp)
            )
            Button(
                modifier = Modifier.padding(top = 2.dp),
                onClick = {
                    if (trackedItems.isEmpty()) {
                        Toast.makeText(context, "暂无可检查条目", Toast.LENGTH_SHORT).show()
                    } else {
                        trackedItems.forEach { refreshItem(it) }
                        Toast.makeText(context, "已开始检查全部跟踪", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("检查")
            }
        }
        Text(
            text = "项目版本跟踪",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        FrostedBlock(
            backdrop = backdrop,
            title = "新增跟踪",
            subtitle = "输入 GitHub 仓库并绑定本机 App",
            accent = MiuixTheme.colorScheme.primary
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPill(
                    label = if (!appListLoaded) "应用列表读取中" else if (appList.isNotEmpty()) "应用列表可用" else "应用列表受限",
                    color = if (appList.isNotEmpty()) MiuixTheme.colorScheme.secondary else MiuixTheme.colorScheme.error
                )
                Row {
                    Button(onClick = { scope.launch { reloadApps() } }) { Text("刷新列表") }
                }
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
            Spacer(modifier = Modifier.height(10.dp))
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
                    refreshItem(item)
                    repoUrlInput = ""
                    Toast.makeText(context, "已新增跟踪", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("新增并检查")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (trackedItems.isEmpty()) {
            MiuixInfoItem("跟踪列表", "暂无条目，请先新增")
        } else {
            trackedItems.forEach { item ->
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
                            state.hasUpdate == true -> "有更新"
                            state.hasUpdate == false -> "最新"
                            else -> "待检查"
                        }
                        val statusColor = when {
                            state.loading -> MiuixTheme.colorScheme.onBackgroundVariant
                            state.hasUpdate == true -> MiuixTheme.colorScheme.error
                            state.hasUpdate == false -> MiuixTheme.colorScheme.secondary
                            else -> MiuixTheme.colorScheme.onBackgroundVariant
                        }
                        val clickableModifier = if (state.hasUpdate == true) {
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
                    if (state.localVersion.isNotBlank()) MiuixInfoItem("本地版本", state.localVersion)
                    if (state.latestTag.isNotBlank()) MiuixInfoItem("GitHub Latest Tag", state.latestTag)
                    if (state.message.isNotBlank() && !state.loading) MiuixInfoItem("结果", state.message)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { refreshItem(item) }) {
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
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
