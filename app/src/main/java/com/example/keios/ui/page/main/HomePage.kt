package com.example.keios.ui.page.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberActionBarBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import com.tencent.mmkv.MMKV
import java.util.concurrent.TimeUnit
import java.util.Locale
import kotlinx.coroutines.flow.onEach
import com.rosan.installer.ui.library.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun formatGitHubCacheAgo(lastRefreshMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (lastRefreshMs <= 0L) return "未刷新"
    val deltaMs = (nowMs - lastRefreshMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
    if (minutes <= 0L) return "just now"
    if (minutes < 60L) return "${minutes}min ago"
    val hours = minutes / 60L
    val remainMinutes = minutes % 60L
    return if (remainMinutes == 0L) "${hours}h ago" else "${hours}h ${remainMinutes}min ago"
}

private const val HOME_BA_KV_ID = "ba_page_settings"
private const val HOME_BA_DEFAULT_FRIEND_CODE = "ARISUKEI"
private const val HOME_BA_AP_LIMIT_MAX = 240
private const val HOME_BA_AP_MAX = 999
private val HOME_BA_CAFE_DAILY_AP_BY_LEVEL = intArrayOf(92, 152, 222, 302, 390, 460, 530, 600, 570, 740)

private data class HomeBaOverview(
    val activated: Boolean,
    val apCurrent: Int,
    val apLimit: Int,
    val cafeStored: Int,
    val cafeCap: Int
)

private fun loadHomeBaOverview(): HomeBaOverview {
    val kv = MMKV.mmkvWithID(HOME_BA_KV_ID)

    val friendCode = kv.decodeString("id_friend_code", HOME_BA_DEFAULT_FRIEND_CODE)
        .orEmpty()
        .uppercase(Locale.ROOT)
        .filter { it in 'A'..'Z' }
        .take(8)
        .let { if (it.length == 8) it else HOME_BA_DEFAULT_FRIEND_CODE }
    val activated = friendCode != HOME_BA_DEFAULT_FRIEND_CODE

    val apLimit = kv.decodeInt("ap_limit", HOME_BA_AP_LIMIT_MAX).coerceIn(0, HOME_BA_AP_LIMIT_MAX)
    val apCurrentExact = if (kv.containsKey("ap_current_exact")) {
        kv.decodeString("ap_current_exact", "0")?.toDoubleOrNull() ?: 0.0
    } else {
        kv.decodeInt("ap_current", 0).toDouble()
    }
    val apCurrent = apCurrentExact.coerceIn(0.0, HOME_BA_AP_MAX.toDouble()).toInt()

    val cafeLevel = kv.decodeInt("cafe_level", 1).coerceIn(1, 10)
    val cafeCap = HOME_BA_CAFE_DAILY_AP_BY_LEVEL[cafeLevel - 1]
    val cafeStoredRaw = kv.decodeString("cafe_stored_ap", "0")?.toDoubleOrNull() ?: 0.0
    val cafeStored = cafeStoredRaw.coerceAtLeast(0.0).toInt().coerceAtMost(cafeCap)

    return HomeBaOverview(
        activated = activated,
        apCurrent = apCurrent,
        apLimit = apLimit,
        cafeStored = cafeStored,
        cafeCap = cafeCap
    )
}

@Composable
private fun HomeInfoCard(
    backdrop: Backdrop,
    blurEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val isInLightTheme = !isSystemInDarkTheme()
    val containerColor = if (blurEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20.dp) },
                effects = {
                    if (blurEnabled) {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    }
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (blurEnabled) 1f else 0f)
                },
                shadow = {
                    Shadow.Default.copy(
                        color = Color.Black.copy(if (isInLightTheme) 0.1f else 0.2f)
                    )
                },
                onDrawSurface = { drawRect(containerColor) }
            )
    ) {
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

@Composable
private fun HomeBottomPageLabel(
    page: BottomPage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                scaleX = page.iconScale
                scaleY = page.iconScale
            }
        if (page.iconRes != null) {
            Icon(
                painter = painterResource(id = page.iconRes),
                contentDescription = page.label,
                tint = if (page.keepOriginalColors) Color.Unspecified else MiuixTheme.colorScheme.onBackground,
                modifier = iconModifier
            )
        } else {
            page.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = page.label,
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = iconModifier
                )
            }
        }
        Text(
            text = page.label,
            color = MiuixTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun HomeInlineInfoItem(
    title: String,
    headline: String,
    detail: String = ""
) {
    val summaryColor = if (isSystemInDarkTheme()) {
        Color(0xFF8AB8FF)
    } else {
        Color(0xFF1E63D6)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = headline.ifBlank { "N/A" },
                color = summaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                color = summaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    homeIconHdrEnabled: Boolean,
    visibleBottomPages: Set<BottomPage>,
    onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onActionBarInteractingChanged: (Boolean) -> Unit = {},
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
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberMiuixLayerBackdrop()
    val actionBarBackdrop = rememberActionBarBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val homeCardBackdrop = rememberActionBarBackdrop {
        drawContent()
    }

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
    val baOverview = loadHomeBaOverview()

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
    val hdrSweepProgress = if (homeIconHdrEnabled) {
        val hdrSweep = rememberInfiniteTransition(label = "kei_hdr_sweep")
        val animated by hdrSweep.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4600, easing = LinearEasing)
            ),
            label = "kei_hdr_sweep_progress"
        )
        animated
    } else {
        0f
    }
    var iconProgress by remember { mutableFloatStateOf(0f) }
    var titleProgress by remember { mutableFloatStateOf(0f) }
    var summaryProgress by remember { mutableFloatStateOf(0f) }
    var actionBarSelectedIndex by rememberSaveable { mutableIntStateOf(1) }
    var showBottomPageEditor by rememberSaveable { mutableStateOf(false) }

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

    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "KeiOS",
                scrollBehavior = topAppBarScrollBehavior,
                color = MiuixTheme.colorScheme.surface.copy(alpha = if (scrollProgress == 1f) 1f else 0f),
                titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = topBarProgress),
                actions = {
                    LiquidActionBar(
                        backdrop = actionBarBackdrop,
                        items = listOf(
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Edit,
                                contentDescription = "编辑底栏板块",
                                onClick = {
                                    actionBarSelectedIndex = 0
                                    showBottomPageEditor = true
                                }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Info,
                                contentDescription = "关于",
                                onClick = {
                                    actionBarSelectedIndex = 1
                                    onOpenAbout()
                                }
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Settings,
                                contentDescription = "设置",
                                onClick = {
                                    actionBarSelectedIndex = 2
                                    onOpenSettings()
                                }
                            )
                        ),
                        selectedIndex = actionBarSelectedIndex,
                        onInteractionChanged = onActionBarInteractingChanged
                    )
                }
            )
        }
    ) { innerPadding ->
        val bottomSheetSafePadding = with(density) {
            WindowInsets.safeDrawing.getBottom(this).toDp()
        }
        SnapshotWindowBottomSheet(
            show = showBottomPageEditor,
            title = "底栏板块",
            onDismissRequest = { showBottomPageEditor = false },
            startAction = {
                GlassIconButton(
                    backdrop = actionBarBackdrop,
                    bottomBarStyle = true,
                    icon = MiuixIcons.Regular.Close,
                    contentDescription = "关闭",
                    onClick = { showBottomPageEditor = false }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomSheetSafePadding + 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeBottomPageLabel(
                        page = BottomPage.Home,
                        modifier = Modifier.defaultMinSize(minHeight = 24.dp)
                    )
                    StatusPill(
                        label = "固定显示",
                        color = Color(0xFF2563EB)
                    )
                }

                BottomPage.entries
                    .filter { it != BottomPage.Home }
                    .forEach { page ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HomeBottomPageLabel(
                                page = page,
                                modifier = Modifier.defaultMinSize(minHeight = 24.dp)
                            )
                            Switch(
                                checked = visibleBottomPages.contains(page),
                                onCheckedChange = { checked ->
                                    onBottomPageVisibilityChange(page, checked)
                                }
                            )
                        }
                    }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Home 固定显示，其他板块可按需隐藏或重新显示。",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp
                )
            }
        }

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
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kei_logo_color),
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer {
                                alpha = (1f - iconProgress) * 0.95f
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .then(
                                if (homeIconHdrEnabled) {
                                    Modifier.drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colorStops = arrayOf(
                                                    0f to Color.Transparent,
                                                    (hdrSweepProgress - 0.16f).coerceIn(0f, 1f) to Color.Transparent,
                                                    hdrSweepProgress.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.82f),
                                                    (hdrSweepProgress + 0.16f).coerceIn(0f, 1f) to Color.Transparent,
                                                    1f to Color.Transparent
                                                )
                                            ),
                                            blendMode = BlendMode.SrcAtop
                                        )
                                        drawRect(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.30f),
                                                    Color.Transparent
                                                ),
                                                radius = size.minDimension * 0.72f
                                            ),
                                            blendMode = BlendMode.SrcAtop
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
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
                        text = "System · MCP · GitHub · BA 一体化工具台",
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
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(
                            label = "BA",
                            color = if (baOverview.activated) runningColor else stoppedColor
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
                            backdrop = homeCardBackdrop,
                            blurEnabled = blurEnabled
                        ) {
                            HomeInlineInfoItem(
                                "MCP",
                                "状态：${if (mcpRunning) "运行中" else "未运行"}   在线设备：$mcpConnectedClients",
                                "端口：$mcpPort   网络模式：$networkModeText"
                            )
                        }

                        HomeInfoCard(
                            backdrop = homeCardBackdrop,
                            blurEnabled = blurEnabled
                        ) {
                            HomeInlineInfoItem(
                                "GitHub Cache",
                                "上次更新：$githubLastUpdateLine",
                                "追踪：$trackedCount 项   可更新：$githubUpdatableLine"
                            )
                        }

                        HomeInfoCard(
                            backdrop = homeCardBackdrop,
                            blurEnabled = blurEnabled
                        ) {
                            HomeInlineInfoItem(
                                "BA",
                                "AP：${baOverview.apCurrent}/${baOverview.apLimit}   咖啡厅AP：${baOverview.cafeStored}/${baOverview.cafeCap}"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
