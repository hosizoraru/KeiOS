package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.ui.page.main.student.GuideRemoteIcon
import com.example.keios.ui.page.main.student.catalog.BA_GUIDE_INDEX_URL
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogEntry
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.fetchBaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.filterByQuery
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassTextButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.core.prefs.UiPrefs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BaGuideCatalogPage(
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit
) {
    val context = LocalContext.current
    val pageTitle = "学生/NPC/卫星图鉴"
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface

    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    val backdrop: LayerBackdrop = key(activationCount) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val scrollBehavior = MiuixScrollBehavior()

    var refreshSignal by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf(BaGuideCatalogBundle.EMPTY) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val tabs = BaGuideCatalogTab.entries
    val activeTab = tabs.getOrElse(selectedTabIndex) { BaGuideCatalogTab.Student }
    val currentEntries = remember(catalog, activeTab) { catalog.entries(activeTab) }
    val filteredEntries = remember(currentEntries, searchQuery) {
        currentEntries.filterByQuery(searchQuery)
    }

    val liquidBottomBarEnabled = remember { UiPrefs.isLiquidBottomBarEnabled() }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var showBottomBar by remember { mutableStateOf(true) }
    val bottomBarNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) showBottomBar = false
                if (available.y > 1f) showBottomBar = true
                return Offset.Zero
            }
        }
    }

    fun openSourceIndex() {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BA_GUIDE_INDEX_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "无法打开来源页面", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(refreshSignal) {
        loading = true
        val result = withContext(Dispatchers.IO) { runCatching { fetchBaGuideCatalogBundle() } }
        result.onSuccess { latest ->
            catalog = latest
            error = null
        }.onFailure {
            error = if (catalog.entriesByTab.values.all { it.isEmpty() }) {
                "图鉴列表加载失败"
            } else {
                "刷新失败，已显示上次列表"
            }
        }
        loading = false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .nestedScroll(bottomBarNestedScrollConnection),
        topBar = {
            TopAppBar(
                title = pageTitle,
                largeTitle = pageTitle,
                scrollBehavior = scrollBehavior,
                color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    LiquidActionBar(
                        backdrop = backdrop,
                        items = listOf(
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Share,
                                contentDescription = "打开来源",
                                onClick = ::openSourceIndex
                            ),
                            LiquidActionItem(
                                icon = MiuixIcons.Regular.Refresh,
                                contentDescription = "刷新列表",
                                onClick = { refreshSignal += 1 }
                            )
                        )
                    )
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                        animationSpec = tween(220),
                        initialOffsetY = { it / 2 }
                    ),
                    exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                        animationSpec = tween(180),
                        targetOffsetY = { it / 2 }
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    FloatingBottomBar(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 12.dp + navigationBarBottom
                            ),
                        selectedIndex = { selectedTabIndex },
                        onSelected = { index -> selectedTabIndex = index },
                        backdrop = backdrop,
                        tabsCount = tabs.size,
                        isBlurEnabled = liquidBottomBarEnabled
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            FloatingBottomBarItem(
                                onClick = { selectedTabIndex = index },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = 1f
                                            scaleY = 1f
                                        }
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val progress = rememberCatalogSyncProgress(loading = loading)
        val progressColor = when {
            loading -> Color(0xFF3B82F6)
            !error.isNullOrBlank() -> Color(0xFFEF4444)
            else -> Color(0xFF22C55E)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SmallTitle("${activeTab.label}图鉴")
                    }
                    CircularProgressIndicator(
                        progress = progress,
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            foregroundColor = progressColor,
                            backgroundColor = progressColor.copy(alpha = 0.30f),
                        ),
                    )
                }
            }

            item {
                FrostedBlock(
                    backdrop = backdrop,
                    title = "筛选",
                    subtitle = "共 ${currentEntries.size} 项 · 显示 ${filteredEntries.size} 项",
                    accent = accent,
                    content = {
                        GlassSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = "搜索名称 / 别名 / ID",
                            backdrop = backdrop,
                            variant = GlassVariant.Content
                        )
                    }
                )
            }

            if (!error.isNullOrBlank()) {
                item {
                    FrostedBlock(
                        backdrop = backdrop,
                        title = "同步状态",
                        subtitle = error.orEmpty(),
                        body = "可通过右上角刷新按钮重试",
                        accent = Color(0xFFEF4444)
                    )
                }
            }

            if (!loading && filteredEntries.isEmpty()) {
                item {
                    FrostedBlock(
                        backdrop = backdrop,
                        title = "暂无结果",
                        subtitle = if (searchQuery.isBlank()) "当前分类没有可显示条目" else "未匹配到相关条目",
                        accent = accent
                    )
                }
            } else {
                items(
                    items = filteredEntries,
                    key = { "${it.tab.name}-${it.entryId}-${it.contentId}" }
                ) { entry ->
                    BaGuideCatalogEntryCard(
                        entry = entry,
                        backdrop = backdrop,
                        onOpenGuide = onOpenGuide
                    )
                }
            }
        }
    }
}

@Composable
private fun BaGuideCatalogEntryCard(
    entry: BaGuideCatalogEntry,
    backdrop: LayerBackdrop,
    onOpenGuide: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGuide(entry.detailUrl) },
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surface.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (entry.iconUrl.isBlank()) {
                    Icon(
                        painter = painterResource(id = entry.tab.iconRes),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    GuideRemoteIcon(
                        imageUrl = entry.iconUrl,
                        modifier = Modifier,
                        iconWidth = 56.dp,
                        iconHeight = 56.dp
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.name,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.aliasDisplay.isNotBlank()) {
                        Text(
                            text = entry.aliasDisplay,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "ID ${entry.contentId}",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            GlassTextButton(
                backdrop = backdrop,
                text = "进入",
                variant = GlassVariant.Content,
                onClick = { onOpenGuide(entry.detailUrl) }
            )
        }
    }
}

@Composable
private fun rememberCatalogSyncProgress(loading: Boolean): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(loading) {
        if (loading) {
            progress.snapTo(0.12f)
            progress.animateTo(
                targetValue = 0.68f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            )
            progress.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
            )
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            )
        }
    }
    return progress.value
}
