package com.example.keios.ui.page.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.keios.R
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.utils.GitHubTrackStore
import com.kyant.shapes.RoundedRectangle
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.onEach
import com.rosan.installer.ui.library.blend.BlendTokenConfig
import com.rosan.installer.ui.library.blend.ColorBlendToken
import com.rosan.installer.ui.library.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
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
private fun HomeInfoCard(
    backdrop: LayerBackdrop,
    blurEnabled: Boolean,
    blurRadius: Float,
    blendColors: List<BlendColorEntry>,
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)
    val glassSurface = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }
    val borderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.52f else 0.68f),
            Color.White.copy(alpha = if (isDark) 0.22f else 0.34f),
            Color.White.copy(alpha = if (isDark) 0.46f else 0.62f),
        )
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .textureBlur(
                backdrop = backdrop,
                shape = RoundedRectangle(20.dp),
                blurRadius = blurRadius,
                noiseCoefficient = BlurDefaults.NoiseCoefficient,
                colors = BlurColors(blendColors = blendColors),
                enabled = blurEnabled,
            )
            .background(
                color = if (blurEnabled) glassSurface else MiuixTheme.colorScheme.surfaceContainer,
                shape = shape
            )
            .border(width = 1.dp, brush = borderBrush, shape = shape)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.22f else 0.30f),
                                Color.White.copy(alpha = if (isDark) 0.16f else 0.20f),
                                Color.White.copy(alpha = if (isDark) 0.12f else 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = shape
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.18f else 0.24f),
                                Color.Transparent,
                                Color.White.copy(alpha = if (isDark) 0.16f else 0.22f),
                            )
                        ),
                        shape = shape
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun HomePage(
    shizukuStatus: String,
    mcpRunning: Boolean,
    mcpPort: Int,
    mcpConnectedClients: Int,
    mcpAllowExternal: Boolean,
    onOpenSettings: () -> Unit,
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    val blurEnabled = isRenderEffectSupported()
    val dynamicBackgroundEnabled = isRuntimeShaderSupported()
    val effectBackgroundEnabled = isRuntimeShaderSupported()
    val backdrop = rememberLayerBackdrop()

    val shizukuGranted = shizukuStatus.contains("granted", ignoreCase = true)
    val runningColor = Color(0xFF2E7D32)
    val stoppedColor = Color(0xFFC62828)
    val inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant
    val githubCacheColor = Color(0xFFF59E0B)

    val appVersionText = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${info.versionName ?: "unknown"} (${info.longVersionCode})"
        }.getOrDefault("版本未知")
    }

    val trackedItems = GitHubTrackStore.load()
    val (cachedStates, cachedRefreshMs) = GitHubTrackStore.loadCheckCache()
    val trackedCount = trackedItems.size
    val cacheHitCount = trackedItems.count { cachedStates.containsKey(it.id) }
    val updatableCount = trackedItems.count { cachedStates[it.id]?.hasUpdate == true }
    val cacheStateColor = if (cacheHitCount > 0) githubCacheColor else inactiveColor

    val networkModeText = if (mcpAllowExternal) "局域网可访问" else "仅本机"
    val cacheRefreshLine = if (cachedRefreshMs > 0L) formatGitHubCacheAgo(cachedRefreshMs) else "未刷新"
    val githubLastUpdateLine = when {
        trackedCount == 0 -> "未配置"
        cacheHitCount == 0 -> "暂无缓存"
        else -> cacheRefreshLine
    }
    val githubUpdatableLine = when {
        trackedCount == 0 -> "0 项"
        cacheHitCount == 0 -> "待刷新"
        else -> "$updatableCount 项"
    }

    var logoHeightPx by remember { mutableIntStateOf(0) }
    val scrollProgress by remember {
        derivedStateOf {
            if (logoHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset.toFloat() / logoHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    val topBarProgress by animateFloatAsState(
        targetValue = scrollProgress,
        label = "home_top_bar_progress"
    )
    val cardBlurRadius by animateFloatAsState(
        targetValue = if (blurEnabled) BlendTokenConfig.Effects.DEFAULT + 10f - (6f * scrollProgress) else 0f,
        label = "home_card_blur_radius"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = 1f - scrollProgress,
        label = "home_bg_alpha"
    )

    var logoHeightDp by remember { mutableStateOf(300.dp) }
    var logoAreaY by remember { mutableFloatStateOf(0f) }
    var iconY by remember { mutableFloatStateOf(0f) }
    var titleY by remember { mutableFloatStateOf(0f) }
    var summaryY by remember { mutableFloatStateOf(0f) }
    var initialLogoAreaY by remember { mutableFloatStateOf(0f) }
    var iconProgress by remember { mutableFloatStateOf(0f) }
    var titleProgress by remember { mutableFloatStateOf(0f) }
    var summaryProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .onEach { (index, offset) ->
                if (index > 0) {
                    if (iconProgress != 1f) iconProgress = 1f
                    if (titleProgress != 1f) titleProgress = 1f
                    if (summaryProgress != 1f) summaryProgress = 1f
                    return@onEach
                }

                if (initialLogoAreaY == 0f && logoAreaY > 0f) {
                    initialLogoAreaY = logoAreaY
                }
                val refLogoAreaY = if (initialLogoAreaY > 0f) initialLogoAreaY else logoAreaY

                val stage1 = (refLogoAreaY - summaryY).coerceAtLeast(1f)
                val stage2 = (summaryY - titleY).coerceAtLeast(1f)
                val stage3 = (titleY - iconY).coerceAtLeast(1f)

                val summaryDelay = stage1 * 0.5f
                summaryProgress = ((offset.toFloat() - summaryDelay) / (stage1 - summaryDelay).coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
                titleProgress = ((offset.toFloat() - stage1) / stage2)
                    .coerceIn(0f, 1f)
                iconProgress = ((offset.toFloat() - stage1 - stage2) / stage3)
                    .coerceIn(0f, 1f)
            }
            .collect { }
    }

    val cardBlendColors = remember(isDark) {
        if (isDark) ColorBlendToken.Colored_Thick_Dark else ColorBlendToken.Colored_Thick_Light
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "KeiOS",
                scrollBehavior = topAppBarScrollBehavior,
                color = MiuixTheme.colorScheme.surface.copy(alpha = if (scrollProgress == 1f) 1f else 0f),
                titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = topBarProgress),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
        val listContentPadding = PaddingValues(
            start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding() + contentTopPadding,
            end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding() + contentBottomPadding + 16.dp
        )
        val logoPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + contentTopPadding + 40.dp,
            start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
            end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
        )

        BgEffectBackground(
            dynamicBackground = dynamicBackgroundEnabled,
            modifier = Modifier.fillMaxSize(),
            bgModifier = Modifier.layerBackdrop(backdrop),
            effectBackground = effectBackgroundEnabled,
            alpha = bgAlpha,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = logoPadding.calculateTopPadding() + 52.dp,
                        start = logoPadding.calculateStartPadding(layoutDirection),
                        end = logoPadding.calculateEndPadding(layoutDirection)
                    )
                    .onSizeChanged { size ->
                        with(density) { logoHeightDp = size.height.toDp() }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            alpha = 1f - iconProgress
                            scaleX = 1f - (iconProgress * 0.05f)
                            scaleY = 1f - (iconProgress * 0.05f)
                        }
                        .onGloballyPositioned { coordinates ->
                            if (iconY != 0f) return@onGloballyPositioned
                            iconY = coordinates.positionInWindow().y + coordinates.size.height
                        }
                        .textureBlur(
                            backdrop = backdrop,
                            shape = RoundedRectangle(48.dp),
                            blurRadius = 72f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = BlurColors(blendColors = cardBlendColors),
                            enabled = blurEnabled,
                        )
                        .background(
                            color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.30f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isDark) 0.56f else 0.70f),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.34f else 0.44f),
                                        Color.White.copy(alpha = if (isDark) 0.08f else 0.14f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_notification_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = "KeiOS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 33.sp,
                    modifier = Modifier
                        .padding(top = 14.dp, bottom = 6.dp)
                        .onGloballyPositioned { coordinates ->
                            if (titleY != 0f) return@onGloballyPositioned
                            titleY = coordinates.positionInWindow().y + coordinates.size.height
                        }
                        .graphicsLayer {
                            alpha = 1f - titleProgress
                            scaleX = 1f - (titleProgress * 0.05f)
                            scaleY = 1f - (titleProgress * 0.05f)
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = 1f - summaryProgress
                            scaleX = 1f - (summaryProgress * 0.05f)
                            scaleY = 1f - (summaryProgress * 0.05f)
                        }
                        .onGloballyPositioned { coordinates ->
                            if (summaryY != 0f) return@onGloballyPositioned
                            summaryY = coordinates.positionInWindow().y + coordinates.size.height
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MCP / GitHub Overview",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = appVersionText,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
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
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                contentPadding = listContentPadding,
            ) {
                item(key = "logo_spacer") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(
                                logoHeightDp + 52.dp +
                                    logoPadding.calculateTopPadding() -
                                    listContentPadding.calculateTopPadding() + 126.dp
                            )
                            .onSizeChanged { size ->
                                logoHeightPx = size.height
                            }
                            .onGloballyPositioned { coordinates ->
                                logoAreaY = coordinates.positionInWindow().y + coordinates.size.height
                            }
                    )
                }

                item(key = "home_content") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = listContentPadding.calculateBottomPadding())
                    ) {
                        HomeInfoCard(
                            backdrop = backdrop,
                            blurEnabled = blurEnabled,
                            blurRadius = cardBlurRadius,
                            blendColors = cardBlendColors
                        ) {
                            MiuixInfoItem(
                                "MCP",
                                "状态 ${if (mcpRunning) "运行中" else "未运行"} · 端口 $mcpPort · 在线 $mcpConnectedClients · 网络模式 $networkModeText"
                            )
                        }

                        HomeInfoCard(
                            backdrop = backdrop,
                            blurEnabled = blurEnabled,
                            blurRadius = cardBlurRadius,
                            blendColors = cardBlendColors
                        ) {
                            MiuixInfoItem(
                                "GitHub Cache",
                                "追踪 $trackedCount 项 · 上次更新 $githubLastUpdateLine · 可更新 $githubUpdatableLine"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
