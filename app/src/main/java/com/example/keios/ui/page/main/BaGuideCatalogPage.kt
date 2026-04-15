package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.keios.ui.page.main.student.catalog.BA_GUIDE_INDEX_URL
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogEntry
import com.example.keios.ui.page.main.student.catalog.BaGuideCatalogTab
import com.example.keios.ui.page.main.student.catalog.fetchBaGuideCatalogBundle
import com.example.keios.ui.page.main.student.catalog.filterByQuery
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.GlassVariant
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

private const val CATALOG_BATCH_SIZE = 60

@Composable
fun BaGuideCatalogPage(
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit
) {
    val context = LocalContext.current
    val pageTitle = "学生/NPC/卫星图鉴"
    val accent = MiuixTheme.colorScheme.primary
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

    val listState = rememberLazyListState()
    var visibleCount by rememberSaveable(activeTab, searchQuery) { mutableIntStateOf(0) }
    LaunchedEffect(filteredEntries.size) {
        visibleCount = minOf(filteredEntries.size, CATALOG_BATCH_SIZE)
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            visibleCount < filteredEntries.size &&
                lastVisible >= (listState.layoutInfo.totalItemsCount - 4)
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            visibleCount = minOf(visibleCount + CATALOG_BATCH_SIZE, filteredEntries.size)
        }
    }
    val displayedEntries = remember(filteredEntries, visibleCount) {
        filteredEntries.take(visibleCount)
    }

    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
            .background(MiuixTheme.colorScheme.background),
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
                    IconButton(onClick = ::openSourceIndex) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Share,
                            contentDescription = "打开来源",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { refreshSignal += 1 }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Refresh,
                            contentDescription = "刷新列表",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            CatalogBottomTabBar(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp + navigationBarBottom
                )
            )
        }
    ) { innerPadding ->
        val progress = rememberCatalogSyncProgress(loading = loading)
        val progressColor = when {
            loading -> Color(0xFF3B82F6)
            !error.isNullOrBlank() -> Color(0xFFEF4444)
            else -> Color(0xFF22C55E)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 10.dp,
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
                    backdrop = null,
                    title = "筛选",
                    subtitle = "共 ${currentEntries.size} 项 · 显示 ${filteredEntries.size} 项",
                    accent = accent,
                    content = {
                        GlassSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = "搜索名称 / 别名 / ID",
                            backdrop = null,
                            variant = GlassVariant.Content
                        )
                    }
                )
            }

            if (!error.isNullOrBlank()) {
                item {
                    FrostedBlock(
                        backdrop = null,
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
                        backdrop = null,
                        title = "暂无结果",
                        subtitle = if (searchQuery.isBlank()) "当前分类没有可显示条目" else "未匹配到相关条目",
                        accent = accent
                    )
                }
            } else {
                items(
                    items = displayedEntries,
                    key = { "${it.tab.name}-${it.entryId}-${it.contentId}" }
                ) { entry ->
                    BaGuideCatalogEntryCard(
                        entry = entry,
                        onOpenGuide = onOpenGuide
                    )
                }
                if (visibleCount < filteredEntries.size) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = 0.3f,
                                size = 16.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = accent,
                                    backgroundColor = accent.copy(alpha = 0.30f),
                                ),
                            )
                            Text(
                                text = " 继续加载更多条目…",
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogBottomTabBar(
    tabs: List<BaGuideCatalogTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedTabIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) {
                                MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.label,
                            tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = 1f
                                    scaleY = 1f
                                }
                        )
                        Text(
                            text = tab.label,
                            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaGuideCatalogEntryCard(
    entry: BaGuideCatalogEntry,
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
                    AsyncImage(
                        model = entry.iconUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
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
            Text(
                text = "进入",
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
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
