package com.example.keios.ui.page.main

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow as ComposeTextShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.R
import com.example.keios.ui.page.main.model.BottomPage
import com.example.keios.ui.page.main.widget.glass.GlassIconButton
import com.example.keios.ui.page.main.widget.glass.GlassVariant
import com.example.keios.ui.page.main.widget.chrome.LiquidActionBar
import com.example.keios.ui.page.main.widget.chrome.LiquidActionItem
import com.example.keios.ui.page.main.widget.sheet.SheetContentColumn
import com.example.keios.ui.page.main.widget.sheet.SheetControlRow
import com.example.keios.ui.page.main.widget.sheet.SheetDescriptionText
import com.example.keios.ui.page.main.widget.sheet.SheetSectionCard
import com.example.keios.ui.page.main.widget.sheet.SheetSectionTitle
import com.example.keios.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.status.StatusPill
import com.example.keios.ui.page.main.widget.status.StatusLabelText
import com.example.keios.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.motion.appMotionFloatState
import com.example.keios.ui.page.main.widget.motion.resolvedMotionDuration
import com.example.keios.feature.github.data.local.GitHubTrackStore
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import com.example.keios.ui.page.main.mcp.util.formatMcpUptimeText
import com.example.keios.ui.page.main.os.appLucideCloseIcon
import com.example.keios.ui.page.main.os.appLucideInfoIcon
import com.example.keios.ui.page.main.os.appLucideLayersIcon
import com.example.keios.ui.page.main.os.osLucideSettingsIcon
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
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun formatGitHubCacheAgo(
    lastRefreshMs: Long,
    notRefreshedText: String,
    justNowText: String,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (lastRefreshMs <= 0L) return notRefreshedText
    val deltaMs = (nowMs - lastRefreshMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMs)
    if (minutes <= 0L) return justNowText
    if (minutes < 60L) return "${minutes}m"
    val hours = minutes / 60L
    val remainMinutes = minutes % 60L
    return if (remainMinutes == 0L) "${hours}h" else "${hours}h ${remainMinutes}m"
}

private const val HOME_BA_KV_ID = "ba_page_settings"
private const val HOME_BA_DEFAULT_FRIEND_CODE = "ARISUKEI"
private const val HOME_BA_AP_LIMIT_MAX = 240
private const val HOME_BA_AP_MAX = 999
private val HOME_BA_CAFE_DAILY_AP_BY_LEVEL = intArrayOf(92, 152, 222, 302, 390, 460, 530, 600, 570, 740)
private val HOME_KEI_TITLE_GRADIENT_COLORS = listOf(
    Color(0xFFFFD2DE),
    Color(0xFFFFCAD9),
    Color(0xFFFF99BB),
    Color(0xFFFF76A5),
    Color(0xFFFF6098),
    Color(0xFFFF5893)
)

data class HomeGitHubOverview(
    val trackedCount: Int = 0,
    val cacheHitCount: Int = 0,
    val updatableCount: Int = 0,
    val preReleaseUpdateCount: Int = 0,
    val strategy: GitHubLookupStrategyOption = GitHubLookupStrategyOption.AtomFeed,
    val apiTokenConfigured: Boolean = false,
    val cachedRefreshMs: Long = 0L,
    val loaded: Boolean = false
)

data class HomeBaOverview(
    val activated: Boolean = false,
    val apCurrent: Int = 0,
    val apLimit: Int = HOME_BA_AP_LIMIT_MAX,
    val cafeStored: Int = 0,
    val cafeCap: Int = HOME_BA_CAFE_DAILY_AP_BY_LEVEL.last(),
    val loaded: Boolean = false
)

fun loadHomeGitHubOverview(): HomeGitHubOverview {
    val snapshot = GitHubTrackStore.loadSnapshot()
    return HomeGitHubOverview(
        trackedCount = snapshot.items.size,
        cacheHitCount = snapshot.items.count { snapshot.checkCache.containsKey(it.id) },
        updatableCount = snapshot.items.count { snapshot.checkCache[it.id]?.hasUpdate == true },
        preReleaseUpdateCount = snapshot.items.count { snapshot.checkCache[it.id]?.hasPreReleaseUpdate == true },
        strategy = snapshot.lookupConfig.selectedStrategy,
        apiTokenConfigured = snapshot.lookupConfig.apiToken.isNotBlank(),
        cachedRefreshMs = snapshot.lastRefreshMs,
        loaded = true
    )
}

fun loadHomeBaOverview(): HomeBaOverview {
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
        cafeCap = cafeCap,
        loaded = true
    )
}

private fun Modifier.homeKeiHdrAccent(
    enabled: Boolean,
    sweepProgress: Float,
    sweepAlpha: Float = 0.82f,
    radialAlpha: Float = 0.30f,
    radialRadiusScale: Float = 0.72f,
    radialCenterX: Float = 0.5f,
    radialCenterY: Float = 0.5f
): Modifier {
    if (!enabled) return this
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        (sweepProgress - 0.16f).coerceIn(0f, 1f) to Color.Transparent,
                        sweepProgress.coerceIn(0f, 1f) to Color.White.copy(alpha = sweepAlpha),
                        (sweepProgress + 0.16f).coerceIn(0f, 1f) to Color.Transparent,
                        1f to Color.Transparent
                    )
                ),
                blendMode = BlendMode.SrcAtop
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = radialAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * radialCenterX, size.height * radialCenterY),
                    radius = size.minDimension * radialRadiusScale
                ),
                blendMode = BlendMode.SrcAtop
            )
        }
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
            .padding(horizontal = 10.dp)
            .padding(bottom = 8.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
    detail: String = "",
    extraDetail: String = "",
    naText: String
) {
    val summaryColor = if (isSystemInDarkTheme()) {
        Color(0xFF8AB8FF)
    } else {
        Color(0xFF1E63D6)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = headline.ifBlank { naText },
                color = summaryColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                color = summaryColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (extraDetail.isNotBlank()) {
            Text(
                text = extraDetail,
                color = summaryColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
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
    mcpRunningSinceEpochMs: Long,
    mcpPort: Int,
    mcpEndpointPath: String,
    mcpServerName: String,
    mcpAuthTokenConfigured: Boolean,
    mcpConnectedClients: Int,
    mcpAllowExternal: Boolean,
    homeGitHubOverview: HomeGitHubOverview = HomeGitHubOverview(),
    homeBaOverview: HomeBaOverview = HomeBaOverview(),
    homeIconHdrEnabled: Boolean,
    liquidActionBarLayeredStyleEnabled: Boolean = true,
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

    val homeAppVersionUnknownFallback = stringResource(R.string.home_app_version_unknown_fallback)
    val homeAppVersionUnknown = stringResource(R.string.home_app_version_unknown)
    val appVersionText = remember(homeAppVersionUnknownFallback, homeAppVersionUnknown) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${info.versionName ?: homeAppVersionUnknownFallback} (${info.longVersionCode})"
        }.getOrDefault(homeAppVersionUnknown)
    }

    val githubOverview = homeGitHubOverview
    val baOverview = homeBaOverview
    val trackedCount = githubOverview.trackedCount
    val cacheHitCount = githubOverview.cacheHitCount
    val updatableCount = githubOverview.updatableCount
    val preReleaseUpdateCount = githubOverview.preReleaseUpdateCount
    val cacheStateColor = when {
        !githubOverview.loaded -> inactiveColor
        cacheHitCount > 0 -> githubCacheColor
        else -> inactiveColor
    }

    val homeStatusLoading = stringResource(R.string.home_status_loading)
    val homeGitHubUnconfigured = stringResource(R.string.home_github_status_unconfigured)
    val homeGitHubNoCache = stringResource(R.string.home_github_status_no_cache)
    val homeGitHubPendingRefresh = stringResource(R.string.home_github_status_pending_refresh)
    val homeJustNow = stringResource(R.string.home_time_just_now)
    val homeNa = stringResource(R.string.common_na)
    val homeAppName = stringResource(R.string.app_name)
    val homeTagline = stringResource(R.string.home_header_tagline)
    val homeStatusMcp = stringResource(R.string.page_mcp_title)
    val homeStatusGitHub = stringResource(R.string.github_page_title)
    val homeStatusBa = stringResource(R.string.home_status_ba)
    val homeStatusShizuku = stringResource(R.string.home_status_shizuku)
    val homeCardMcp = stringResource(R.string.home_card_title_mcp)
    val homeCardGitHub = stringResource(R.string.home_card_title_github_cache)
    val homeCardBa = stringResource(R.string.home_card_title_ba)
    val homeMcpRuntimePending = stringResource(R.string.mcp_runtime_pending)
    val homeCommonFilled = stringResource(R.string.common_filled)
    val homeCommonNotUsed = stringResource(R.string.common_not_used)
    val networkModeText = if (mcpAllowExternal) {
        stringResource(R.string.mcp_network_mode_lan_accessible)
    } else {
        stringResource(R.string.mcp_network_mode_local_only_short)
    }
    val mcpRuntimeText = if (!mcpRunning || mcpRunningSinceEpochMs <= 0L) {
        homeMcpRuntimePending
    } else {
        formatMcpUptimeText(System.currentTimeMillis() - mcpRunningSinceEpochMs)
    }
    val mcpStatusText = if (mcpRunning) {
        stringResource(R.string.home_mcp_status_running)
    } else {
        stringResource(R.string.home_mcp_status_stopped)
    }
    val mcpTokenStatusText = if (mcpAuthTokenConfigured) homeCommonFilled else homeCommonNotUsed
    val githubStrategyText = when (githubOverview.strategy) {
        GitHubLookupStrategyOption.AtomFeed -> stringResource(R.string.github_overview_strategy_atom)
        GitHubLookupStrategyOption.GitHubApiToken -> stringResource(R.string.github_overview_strategy_api)
    }
    val githubApiText = when {
        githubOverview.strategy != GitHubLookupStrategyOption.GitHubApiToken -> homeCommonNotUsed
        githubOverview.apiTokenConfigured -> homeCommonFilled
        else -> stringResource(R.string.common_guest)
    }
    val cacheRefreshLine = formatGitHubCacheAgo(
        lastRefreshMs = githubOverview.cachedRefreshMs,
        notRefreshedText = stringResource(R.string.github_refresh_ago_not_refreshed),
        justNowText = homeJustNow
    )
    val githubLastUpdateLine = when {
        !githubOverview.loaded -> homeStatusLoading
        trackedCount == 0 -> homeGitHubUnconfigured
        cacheHitCount == 0 -> homeGitHubNoCache
        else -> cacheRefreshLine
    }
    val githubUpdatableLine = when {
        !githubOverview.loaded -> homeStatusLoading
        trackedCount == 0 -> stringResource(R.string.github_overview_value_count, 0)
        cacheHitCount == 0 -> homeGitHubPendingRefresh
        else -> stringResource(R.string.github_overview_value_count, updatableCount)
    }
    val githubPreReleaseUpdateLine = when {
        !githubOverview.loaded || trackedCount == 0 || cacheHitCount == 0 ->
            stringResource(R.string.github_overview_value_count, 0)
        else -> stringResource(R.string.github_overview_value_count, preReleaseUpdateCount)
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

    val topBarProgress by appMotionFloatState(
        targetValue = scrollProgress,
        label = "home_top_bar_progress"
    )
    val bgAlpha by appMotionFloatState(
        targetValue = 1f - scrollProgress,
        label = "home_bg_alpha"
    )

    var logoHeightDp by remember { mutableStateOf(300.dp) }
    var logoAreaY by remember { mutableFloatStateOf(0f) }
    var iconY by remember { mutableFloatStateOf(0f) }
    var titleY by remember { mutableFloatStateOf(0f) }
    var summaryY by remember { mutableFloatStateOf(0f) }
    var initialLogoAreaY by remember { mutableFloatStateOf(0f) }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val hdrSweepProgress = if (homeIconHdrEnabled && transitionAnimationsEnabled) {
        val hdrSweep = rememberInfiniteTransition(label = "kei_hdr_sweep")
        val animated by hdrSweep.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = resolvedMotionDuration(4600, transitionAnimationsEnabled),
                    easing = LinearEasing
                )
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
    val layersIcon = appLucideLayersIcon()
    val aboutIcon = appLucideInfoIcon()
    val settingsIcon = osLucideSettingsIcon()
    val editBottomPagesContentDescription = stringResource(R.string.home_cd_edit_bottom_pages)
    val aboutContentDescription = stringResource(R.string.about_page_title)
    val settingsContentDescription = stringResource(R.string.settings_title)
    val homeActionItems = remember(
        editBottomPagesContentDescription,
        aboutContentDescription,
        settingsContentDescription,
        onOpenAbout,
        onOpenSettings
    ) {
        listOf(
            LiquidActionItem(
                icon = layersIcon,
                contentDescription = editBottomPagesContentDescription,
                onClick = {
                    actionBarSelectedIndex = 0
                    showBottomPageEditor = true
                }
            ),
            LiquidActionItem(
                icon = aboutIcon,
                contentDescription = aboutContentDescription,
                onClick = {
                    actionBarSelectedIndex = 1
                    onOpenAbout()
                }
            ),
            LiquidActionItem(
                icon = settingsIcon,
                contentDescription = settingsContentDescription,
                onClick = {
                    actionBarSelectedIndex = 2
                    onOpenSettings()
                }
            )
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "",
                scrollBehavior = topAppBarScrollBehavior,
                color = MiuixTheme.colorScheme.surface.copy(alpha = if (scrollProgress == 1f) 1f else 0f),
                titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = topBarProgress),
                actions = {
                    LiquidActionBar(
                        backdrop = actionBarBackdrop,
                        layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                        items = homeActionItems,
                        selectedIndex = actionBarSelectedIndex,
                        onInteractionChanged = onActionBarInteractingChanged
                    )
                }
            )
        }
    ) { innerPadding ->
        SnapshotWindowBottomSheet(
            show = showBottomPageEditor,
            title = stringResource(R.string.home_sheet_bottom_pages_title),
            onDismissRequest = { showBottomPageEditor = false },
            startAction = {
                GlassIconButton(
                    backdrop = actionBarBackdrop,
                    variant = GlassVariant.Bar,
                    icon = appLucideCloseIcon(),
                    contentDescription = stringResource(R.string.common_close),
                    onClick = { showBottomPageEditor = false }
                )
            }
        ) {
            SheetContentColumn(
                scrollable = false,
                verticalSpacing = 10.dp
            ) {
                SheetSectionTitle(stringResource(R.string.home_sheet_visible_pages_title))
                SheetSectionCard(verticalSpacing = 10.dp) {
                    SheetControlRow(
                        labelContent = {
                            HomeBottomPageLabel(
                                page = BottomPage.Home,
                                modifier = Modifier.defaultMinSize(minHeight = 24.dp)
                            )
                        }
                    ) {
                        StatusPill(
                            label = StatusLabelText.FixedVisible,
                            color = Color(0xFF2563EB)
                        )
                    }

                    BottomPage.entries
                        .filter { it != BottomPage.Home }
                        .forEach { page ->
                            SheetControlRow(
                                labelContent = {
                                    HomeBottomPageLabel(
                                        page = page,
                                        modifier = Modifier.defaultMinSize(minHeight = 24.dp)
                                    )
                                }
                            ) {
                                Switch(
                                    checked = visibleBottomPages.contains(page),
                                    onCheckedChange = { checked ->
                                        onBottomPageVisibilityChange(page, checked)
                                    }
                                )
                            }
                        }
                }
                SheetDescriptionText(
                    text = stringResource(R.string.home_sheet_visible_pages_desc)
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
            top = innerPadding.calculateTopPadding() + contentTopPadding + 24.dp,
            start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
            end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
        )

        BgEffectBackground(
            dynamicBackground = dynamicBackgroundEnabled,
            modifier = Modifier.fillMaxSize(),
            bgModifier = Modifier,
            effectBackground = effectBackgroundEnabled,
            alpha = bgAlpha,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = logoPadding.calculateTopPadding() + 36.dp,
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
                        .size(88.dp)
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
                            .size(88.dp)
                            .graphicsLayer {
                                alpha = (1f - iconProgress) * 0.95f
                            }
                            .homeKeiHdrAccent(
                                enabled = homeIconHdrEnabled,
                                sweepProgress = hdrSweepProgress,
                                radialAlpha = 0.30f,
                                radialRadiusScale = 0.72f,
                                radialCenterX = 0.5f,
                                radialCenterY = 0.48f
                            )
                    )
                }

                BasicText(
                    text = homeAppName,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = HOME_KEI_TITLE_GRADIENT_COLORS,
                            start = Offset(14f, 6f),
                            end = Offset(260f, 104f)
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        shadow = ComposeTextShadow(
                            color = Color(0x55FF74A6),
                            offset = Offset(0f, 3f),
                            blurRadius = 16f
                        )
                    ),
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            if (titleY != 0f) return@onGloballyPositioned
                            titleY = coordinates.positionInWindow().y + coordinates.size.height
                        }
                        .graphicsLayer {
                            alpha = 1f - titleProgress
                            scaleX = 1f - (titleProgress * 0.05f)
                            scaleY = 1f - (titleProgress * 0.05f)
                        }
                        .homeKeiHdrAccent(
                            enabled = homeIconHdrEnabled,
                            sweepProgress = hdrSweepProgress,
                            radialAlpha = 0.26f,
                            radialRadiusScale = 0.82f,
                            radialCenterX = 0.32f,
                            radialCenterY = 0.34f
                        )
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
                        text = homeTagline,
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
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        StatusPill(
                            label = homeStatusMcp,
                            color = if (mcpRunning) runningColor else stoppedColor,
                            modifier = Modifier.defaultMinSize(minWidth = 62.dp)
                        )
                        StatusPill(
                            label = homeStatusGitHub,
                            color = cacheStateColor,
                            modifier = Modifier.defaultMinSize(minWidth = 72.dp)
                        )
                        StatusPill(
                            label = homeStatusBa,
                            color = when {
                                !baOverview.loaded -> inactiveColor
                                baOverview.activated -> runningColor
                                else -> stoppedColor
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 62.dp)
                        )
                        StatusPill(
                            label = homeStatusShizuku,
                            color = if (shizukuGranted) runningColor else stoppedColor,
                            modifier = Modifier.defaultMinSize(minWidth = 70.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
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
                                logoHeightDp + 36.dp +
                                    logoPadding.calculateTopPadding() -
                                    listContentPadding.calculateTopPadding() + 90.dp
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
                                title = homeCardMcp,
                                headline = stringResource(
                                    R.string.home_mcp_line_priority_primary,
                                    mcpStatusText,
                                    mcpRuntimeText
                                ),
                                detail = stringResource(
                                    R.string.home_mcp_line_priority_secondary,
                                    mcpConnectedClients,
                                    networkModeText,
                                    mcpPort
                                ),
                                extraDetail = stringResource(
                                    R.string.home_mcp_line_priority_tertiary,
                                    mcpEndpointPath,
                                    mcpServerName,
                                    mcpTokenStatusText
                                ),
                                naText = homeNa
                            )
                        }

                        HomeInfoCard(
                            backdrop = homeCardBackdrop,
                            blurEnabled = blurEnabled
                        ) {
                            HomeInlineInfoItem(
                                title = homeCardGitHub,
                                headline = if (!githubOverview.loaded) {
                                    homeStatusLoading
                                } else if (trackedCount == 0) {
                                    homeGitHubUnconfigured
                                } else if (cacheHitCount == 0) {
                                    homeGitHubPendingRefresh
                                } else {
                                    stringResource(
                                        R.string.home_github_line_priority_primary,
                                        githubUpdatableLine,
                                        githubPreReleaseUpdateLine
                                    )
                                },
                                detail = stringResource(
                                    R.string.home_github_line_priority_secondary,
                                    stringResource(R.string.github_overview_value_count, trackedCount),
                                    stringResource(R.string.github_overview_value_count, cacheHitCount)
                                ),
                                extraDetail = stringResource(
                                    R.string.home_github_line_priority_tertiary,
                                    githubStrategyText,
                                    githubApiText,
                                    githubLastUpdateLine
                                ),
                                naText = homeNa
                            )
                        }

                        HomeInfoCard(
                            backdrop = homeCardBackdrop,
                            blurEnabled = blurEnabled
                        ) {
                            HomeInlineInfoItem(
                                title = homeCardBa,
                                headline = if (baOverview.loaded) {
                                    stringResource(
                                        R.string.home_ba_line_ap,
                                        baOverview.apCurrent,
                                        baOverview.apLimit,
                                        baOverview.cafeStored,
                                        baOverview.cafeCap
                                    )
                                } else {
                                    homeStatusLoading
                                },
                                naText = homeNa
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
