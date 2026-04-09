package com.example.keios.ui.page.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.GitHubTrackStore
import com.example.keios.ui.utils.rememberCardBlurColors
import com.kyant.shapes.RoundedRectangle
import java.util.concurrent.TimeUnit
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun formatGitHubCacheAgo(lastRefreshMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (lastRefreshMs <= 0L) return "未刷新"
    val deltaMs = (nowMs - lastRefreshMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
    if (minutes <= 0L) return "刚刚"
    if (minutes < 60L) return "${minutes} 分钟前"
    val hours = minutes / 60L
    val remainMinutes = minutes % 60L
    return if (remainMinutes == 0L) "${hours} 小时前" else "${hours} 小时 ${remainMinutes} 分钟前"
}

@Composable
fun HomePage(
    shizukuStatus: String,
    mcpRunning: Boolean,
    mcpPort: Int,
    mcpConnectedClients: Int,
    onOpenSettings: () -> Unit,
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 0.dp
) {
    val shizukuGranted = shizukuStatus.contains("granted", ignoreCase = true)
    val runningColor = Color(0xFF2E7D32)
    val stoppedColor = Color(0xFFC62828)
    val inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant
    val githubCacheColor = Color(0xFFF59E0B)
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val isDark = isSystemInDarkTheme()
    val foregroundBackdrop = rememberLayerBackdrop()
    val blurEnabled = isRenderEffectSupported()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val cardBlurColors = rememberCardBlurColors()

    val trackedItems = GitHubTrackStore.load()
    val (cachedStates, cachedRefreshMs) = GitHubTrackStore.loadCheckCache()
    val trackedCount = trackedItems.size
    val cacheHitCount = trackedItems.count { cachedStates.containsKey(it.id) }
    val updatableCount = trackedItems.count { cachedStates[it.id]?.hasUpdate == true }
    val preReleaseCount = trackedItems.count { cachedStates[it.id]?.isPreRelease == true }
    val stableLatestCount = trackedItems.count {
        val state = cachedStates[it.id]
        state?.hasUpdate == false && state.isPreRelease.not()
    }
    val cacheStateColor = when {
        cacheHitCount > 0 -> githubCacheColor
        else -> inactiveColor
    }
    val cacheSummaryLine = when {
        trackedCount == 0 -> "未配置 GitHub 跟踪项目"
        cacheHitCount == 0 -> "追踪 $trackedCount 项 · 暂无可用缓存"
        else -> "追踪 $trackedCount 项 · 缓存命中 $cacheHitCount 项 · 可更新 $updatableCount 项"
    }
    val cacheDetailLine = when {
        trackedCount == 0 -> "请到 GitHub 页面新增项目"
        cacheHitCount == 0 -> "请在 GitHub 页面执行一次刷新以生成缓存"
        else -> "最新稳定版 $stableLatestCount 项 · 预发行 $preReleaseCount 项 · ${formatGitHubCacheAgo(cachedRefreshMs)}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(foregroundBackdrop)
                .background(
                    brush = if (isDark) {
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF5C1D5E),
                                Color(0xFF2A2A7A),
                                Color(0xFF191C24)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFE3F1),
                                Color(0xFFE6E8FF),
                                Color(0xFFF4F7FF)
                            )
                        )
                    }
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 44.dp)
                .size(140.dp)
                .background(
                    if (isDark) Color(0x55D748A3) else Color(0x66FF8CC7),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 52.dp)
                .size(160.dp)
                .background(
                    if (isDark) Color(0x44FF8C3A) else Color(0x66FFC18B),
                    shape = CircleShape
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = "KeiOS",
                    scrollBehavior = scrollBehavior,
                    color = Color.Transparent,
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Settings,
                                contentDescription = "设置",
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 8.dp,
                    start = 12.dp,
                    end = 12.dp
                )
            ) {
                item {
                    Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .background(
                                color = if (isDark) Color(0x55FF7CC9) else Color(0x66FF95D4),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", color = Color.White)
                    }
                    Text(
                        text = "KeiOS",
                        color = titleColor,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    Text(
                        text = "MCP / GitHub Runtime",
                        color = subtitleColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                }
                item { Spacer(modifier = Modifier.height(18.dp)) }
                item {
                    Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .textureBlur(
                            backdrop = foregroundBackdrop,
                            shape = RoundedRectangle(18.dp),
                            blurRadius = 52f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = cardBlurColors,
                            enabled = blurEnabled
                        )
                        .background(
                            color = if (blurEnabled) {
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.12f)
                            } else if (isDark) {
                                MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)
                            } else {
                                Color.White.copy(alpha = 0.74f)
                            },
                            shape = RoundedRectangle(18.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isDark) 0.14f else 0.20f),
                            shape = RoundedRectangle(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Runtime Status",
                        color = titleColor
                    )
                    Text(
                        text = "MCP / GitHub 缓存状态",
                        color = subtitleColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatusPill(
                            label = "MCP",
                            color = if (mcpRunning) runningColor else stoppedColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(
                            label = "GitHub",
                            color = cacheStateColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(
                            label = "Shizuku",
                            color = if (shizukuGranted) runningColor else stoppedColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MiuixInfoItem(
                        "MCP Server",
                        "${if (mcpRunning) "运行中" else "未运行"} · 在线 $mcpConnectedClients · $mcpPort 端口 · MCP 协议"
                    )
                    MiuixInfoItem("GitHub 缓存", cacheSummaryLine)
                    MiuixInfoItem("GitHub 详情", cacheDetailLine)
                }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
