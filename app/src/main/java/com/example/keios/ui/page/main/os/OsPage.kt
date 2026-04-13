package com.example.keios.ui.page.main

import android.app.ActivityManager
import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.GlassIconButton
import com.example.keios.ui.page.main.widget.GlassVariant
import com.example.keios.ui.page.main.widget.GlassSearchField
import com.example.keios.ui.page.main.widget.MiuixExpandableSection
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.SheetDescriptionText
import com.example.keios.ui.page.main.widget.SnapshotWindowBottomSheet
import com.example.keios.ui.page.main.widget.StatusPill
import com.example.keios.ui.page.main.widget.StatusLabelText
import com.example.keios.core.system.ShizukuApiUtils
import com.example.keios.core.system.getAllJavaPropString
import com.example.keios.core.system.getAllSystemProperties
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OsPage(
    scrollToTopSignal: Int,
    shizukuStatus: String,
    shizukuApiUtils: ShizukuApiUtils,
    cardPressFeedbackEnabled: Boolean = true,
    contentBottomPadding: Dp = 72.dp,
    onActionBarInteractingChanged: (Boolean) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val inactive = MiuixTheme.colorScheme.onBackgroundVariant
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val cachedColor = Color(0xFFF59E0B)
    val refreshingColor = Color(0xFF3B82F6)
    val syncedColor = Color(0xFF22C55E)

    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shizukuReady = shizukuStatus.contains("granted", ignoreCase = true)
    var cacheLoaded by remember { mutableStateOf(false) }
    var cachePersisted by remember { mutableStateOf(false) }
    var queryInput by remember { mutableStateOf("") }
    var queryApplied by remember { mutableStateOf("") }
    var topInfoExpanded by remember { mutableStateOf(OsUiStateStore.topInfoExpanded(defaultValue = true)) }
    var systemTableExpanded by remember { mutableStateOf(OsUiStateStore.osSystemTableExpanded(defaultValue = false)) }
    var secureTableExpanded by remember { mutableStateOf(OsUiStateStore.secureTableExpanded(defaultValue = false)) }
    var globalTableExpanded by remember { mutableStateOf(OsUiStateStore.globalTableExpanded(defaultValue = false)) }
    var androidPropsExpanded by remember { mutableStateOf(OsUiStateStore.androidPropsExpanded(defaultValue = false)) }
    var javaPropsExpanded by remember { mutableStateOf(OsUiStateStore.javaPropsExpanded(defaultValue = false)) }
    var linuxEnvExpanded by remember { mutableStateOf(OsUiStateStore.linuxEnvExpanded(defaultValue = false)) }
    var visibleCards by remember { mutableStateOf(OsCardVisibilityStore.loadVisibleCards()) }
    var showCardManager by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var exportPreparing by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshProgress by remember { mutableStateOf(0f) }
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    DisposableEffect(Unit) {
        onDispose { onActionBarInteractingChanged(false) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val content = pendingExportContent
        if (uri == null || content.isNullOrBlank()) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                writer?.write(content)
            }
        }.onSuccess {
            Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "导出失败: ${it.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
        }
    }

    var sectionStates by remember {
        mutableStateOf(
            mapOf(
                SectionKind.SYSTEM to SectionState(),
                SectionKind.SECURE to SectionState(),
                SectionKind.GLOBAL to SectionState(),
                SectionKind.ANDROID to SectionState(),
                SectionKind.JAVA to SectionState(),
                SectionKind.LINUX to SectionState()
            )
        )
    }

    fun visibleSectionKinds(): Set<SectionKind> = buildSet {
        if (visibleCards.contains(OsSectionCard.SYSTEM)) add(SectionKind.SYSTEM)
        if (visibleCards.contains(OsSectionCard.SECURE)) add(SectionKind.SECURE)
        if (visibleCards.contains(OsSectionCard.GLOBAL)) add(SectionKind.GLOBAL)
        if (visibleCards.contains(OsSectionCard.ANDROID)) add(SectionKind.ANDROID)
        if (visibleCards.contains(OsSectionCard.JAVA)) add(SectionKind.JAVA)
        if (visibleCards.contains(OsSectionCard.LINUX)) add(SectionKind.LINUX)
    }

    fun isCardVisible(card: OsSectionCard): Boolean = visibleCards.contains(card)

    fun updateSection(section: SectionKind, transform: (SectionState) -> SectionState) {
        sectionStates = sectionStates.toMutableMap().also { map ->
            val old = map[section] ?: SectionState()
            map[section] = transform(old)
        }
    }

    suspend fun ensureLoad(section: SectionKind, forceRefresh: Boolean = false) {
        if (!visibleSectionKinds().contains(section)) return
        val current = sectionStates[section] ?: SectionState()
        if (current.loading) return
        if (!forceRefresh) {
            if (current.loadedFresh) return
            if (current.rows.isNotEmpty()) return
        }
        updateSection(section) { it.copy(loading = true) }
        val fresh = withContext(Dispatchers.IO) {
            buildSectionRows(section, context, shizukuStatus, shizukuApiUtils)
        }
        updateSection(section) { it.copy(rows = fresh, loading = false, loadedFresh = true) }
        cachePersisted = withContext(Dispatchers.IO) {
            OsInfoCache.write(section, fresh)
            OsInfoCache.hasPersistedCache(visibleSectionKinds())
        }
    }

    suspend fun applyCardVisibility(card: OsSectionCard, visible: Boolean) {
        val updated = visibleCards.toMutableSet().apply {
            if (visible) add(card) else remove(card)
        }.toSet()
        visibleCards = updated
        withContext(Dispatchers.IO) { OsCardVisibilityStore.saveVisibleCards(updated) }
        when (card) {
            OsSectionCard.TOP_INFO -> {
                if (!visible) topInfoExpanded = false
            }
            OsSectionCard.SYSTEM -> {
                if (!visible) {
                    systemTableExpanded = false
                    updateSection(SectionKind.SYSTEM) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.SYSTEM) }
                } else ensureLoad(SectionKind.SYSTEM, forceRefresh = true)
            }
            OsSectionCard.SECURE -> {
                if (!visible) {
                    secureTableExpanded = false
                    updateSection(SectionKind.SECURE) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.SECURE) }
                } else ensureLoad(SectionKind.SECURE, forceRefresh = true)
            }
            OsSectionCard.GLOBAL -> {
                if (!visible) {
                    globalTableExpanded = false
                    updateSection(SectionKind.GLOBAL) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.GLOBAL) }
                } else ensureLoad(SectionKind.GLOBAL, forceRefresh = true)
            }
            OsSectionCard.ANDROID -> {
                if (!visible) {
                    androidPropsExpanded = false
                    updateSection(SectionKind.ANDROID) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.ANDROID) }
                } else ensureLoad(SectionKind.ANDROID, forceRefresh = true)
            }
            OsSectionCard.JAVA -> {
                if (!visible) {
                    javaPropsExpanded = false
                    updateSection(SectionKind.JAVA) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.JAVA) }
                } else ensureLoad(SectionKind.JAVA, forceRefresh = true)
            }
            OsSectionCard.LINUX -> {
                if (!visible) {
                    linuxEnvExpanded = false
                    updateSection(SectionKind.LINUX) { SectionState() }
                    withContext(Dispatchers.IO) { OsInfoCache.clear(SectionKind.LINUX) }
                } else ensureLoad(SectionKind.LINUX, forceRefresh = true)
            }
        }
        cachePersisted = withContext(Dispatchers.IO) { OsInfoCache.hasPersistedCache(visibleSectionKinds()) }
    }

    suspend fun refreshAllSections() {
        refreshing = true
        refreshProgress = 0f
        try {
            val targets = SectionKind.entries.filter { visibleSectionKinds().contains(it) }
            val sectionCount = targets.size.coerceAtLeast(1)
            targets.forEachIndexed { index, section ->
                ensureLoad(section, forceRefresh = true)
                refreshProgress = (index + 1).toFloat() / sectionCount.toFloat()
            }
            Toast.makeText(context, if (targets.isEmpty()) "当前没有可刷新的卡片" else "系统参数已刷新并缓存", Toast.LENGTH_SHORT).show()
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(queryInput) {
        delay(180)
        queryApplied = queryInput
    }

    LaunchedEffect(Unit) {
        val visibleSections = visibleSectionKinds()
        val (cached, persisted) = withContext(Dispatchers.IO) {
            OsInfoCache.read(visibleSections) to OsInfoCache.hasPersistedCache(visibleSections)
        }
        sectionStates = mapOf(
            SectionKind.SYSTEM to SectionState(rows = if (visibleSections.contains(SectionKind.SYSTEM)) cached.system else emptyList()),
            SectionKind.SECURE to SectionState(rows = if (visibleSections.contains(SectionKind.SECURE)) cached.secure else emptyList()),
            SectionKind.GLOBAL to SectionState(rows = if (visibleSections.contains(SectionKind.GLOBAL)) cached.global else emptyList()),
            SectionKind.ANDROID to SectionState(rows = if (visibleSections.contains(SectionKind.ANDROID)) cached.android else emptyList()),
            SectionKind.JAVA to SectionState(rows = if (visibleSections.contains(SectionKind.JAVA)) cached.java else emptyList()),
            SectionKind.LINUX to SectionState(rows = if (visibleSections.contains(SectionKind.LINUX)) cached.linux else emptyList())
        )
        cachePersisted = persisted
        cacheLoaded = true
        visibleSections.forEach { section ->
            ensureLoad(section, forceRefresh = false)
        }
    }

    LaunchedEffect(shizukuReady) {
        updateSection(SectionKind.SYSTEM) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.SECURE) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.GLOBAL) { it.copy(loadedFresh = false) }
        updateSection(SectionKind.LINUX) { it.copy(loadedFresh = false) }
    }

    LaunchedEffect(topInfoExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setTopInfoExpanded(topInfoExpanded) }
    }
    LaunchedEffect(systemTableExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setOsSystemTableExpanded(systemTableExpanded) }
    }
    LaunchedEffect(secureTableExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setSecureTableExpanded(secureTableExpanded) }
    }
    LaunchedEffect(globalTableExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setGlobalTableExpanded(globalTableExpanded) }
    }
    LaunchedEffect(androidPropsExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setAndroidPropsExpanded(androidPropsExpanded) }
    }
    LaunchedEffect(javaPropsExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setJavaPropsExpanded(javaPropsExpanded) }
    }
    LaunchedEffect(linuxEnvExpanded) {
        withContext(Dispatchers.IO) { OsUiStateStore.setLinuxEnvExpanded(linuxEnvExpanded) }
    }

    LaunchedEffect(systemTableExpanded, visibleCards) {
        if (systemTableExpanded && isCardVisible(OsSectionCard.SYSTEM)) ensureLoad(SectionKind.SYSTEM)
    }
    LaunchedEffect(secureTableExpanded, visibleCards) {
        if (secureTableExpanded && isCardVisible(OsSectionCard.SECURE)) ensureLoad(SectionKind.SECURE)
    }
    LaunchedEffect(globalTableExpanded, visibleCards) {
        if (globalTableExpanded && isCardVisible(OsSectionCard.GLOBAL)) ensureLoad(SectionKind.GLOBAL)
    }
    LaunchedEffect(androidPropsExpanded, visibleCards) {
        if (androidPropsExpanded && isCardVisible(OsSectionCard.ANDROID)) ensureLoad(SectionKind.ANDROID)
    }
    LaunchedEffect(javaPropsExpanded, visibleCards) {
        if (javaPropsExpanded && isCardVisible(OsSectionCard.JAVA)) ensureLoad(SectionKind.JAVA)
    }
    LaunchedEffect(linuxEnvExpanded, visibleCards) {
        if (linuxEnvExpanded && isCardVisible(OsSectionCard.LINUX)) ensureLoad(SectionKind.LINUX)
    }

    val systemRows = sectionStates[SectionKind.SYSTEM]?.rows ?: emptyList()
    val secureRows = sectionStates[SectionKind.SECURE]?.rows ?: emptyList()
    val globalRows = sectionStates[SectionKind.GLOBAL]?.rows ?: emptyList()
    val androidRows = sectionStates[SectionKind.ANDROID]?.rows ?: emptyList()
    val javaRows = sectionStates[SectionKind.JAVA]?.rows ?: emptyList()
    val linuxRows = sectionStates[SectionKind.LINUX]?.rows ?: emptyList()

    val topInfoRows = remember(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows) {
        buildTopInfoRows(systemRows, secureRows, globalRows, androidRows, javaRows, linuxRows)
    }

    val prunedSystemRows = remember(systemRows) { removeTopInfoRows(SectionKind.SYSTEM, systemRows) }
    val prunedSecureRows = remember(secureRows) { removeTopInfoRows(SectionKind.SECURE, secureRows) }
    val prunedGlobalRows = remember(globalRows) { removeTopInfoRows(SectionKind.GLOBAL, globalRows) }
    val prunedAndroidRows = remember(androidRows) { removeTopInfoRows(SectionKind.ANDROID, androidRows) }
    val prunedJavaRows = remember(javaRows) { removeTopInfoRows(SectionKind.JAVA, javaRows) }
    val prunedLinuxRows = remember(linuxRows) { removeTopInfoRows(SectionKind.LINUX, linuxRows) }

    val q = queryApplied.trim()
    val displayedTopInfoRows = remember(q, topInfoRows, topInfoExpanded) {
        if (q.isBlank() && !topInfoExpanded) topInfoRows else sortRowsByType(filterRows(topInfoRows, q))
    }
    val displayedSystemRows = remember(q, prunedSystemRows, systemTableExpanded) {
        if (q.isBlank() && !systemTableExpanded) prunedSystemRows else sortRowsByType(filterRows(prunedSystemRows, q))
    }
    val displayedSecureRows = remember(q, prunedSecureRows, secureTableExpanded) {
        if (q.isBlank() && !secureTableExpanded) prunedSecureRows else sortRowsByType(filterRows(prunedSecureRows, q))
    }
    val displayedGlobalRows = remember(q, prunedGlobalRows, globalTableExpanded) {
        if (q.isBlank() && !globalTableExpanded) prunedGlobalRows else sortRowsByType(filterRows(prunedGlobalRows, q))
    }
    val displayedAndroidRows = remember(q, prunedAndroidRows, androidPropsExpanded) {
        if (q.isBlank() && !androidPropsExpanded) prunedAndroidRows else sortRowsByType(filterRows(prunedAndroidRows, q))
    }
    val displayedJavaRows = remember(q, prunedJavaRows, javaPropsExpanded) {
        if (q.isBlank() && !javaPropsExpanded) prunedJavaRows else sortRowsByType(filterRows(prunedJavaRows, q))
    }
    val displayedLinuxRows = remember(q, prunedLinuxRows, linuxEnvExpanded) {
        if (q.isBlank() && !linuxEnvExpanded) prunedLinuxRows else sortRowsByType(filterRows(prunedLinuxRows, q))
    }
    val groupedTopInfoRows = remember(displayedTopInfoRows, topInfoExpanded, q) {
        if (q.isBlank() && !topInfoExpanded) emptyList() else groupTopInfoRows(displayedTopInfoRows)
    }
    val totalRowsCount = remember(
        topInfoRows.size,
        prunedSystemRows.size,
        prunedSecureRows.size,
        prunedGlobalRows.size,
        prunedAndroidRows.size,
        prunedJavaRows.size,
        prunedLinuxRows.size
    ) {
        topInfoRows.size +
            prunedSystemRows.size +
            prunedSecureRows.size +
            prunedGlobalRows.size +
            prunedAndroidRows.size +
            prunedJavaRows.size +
            prunedLinuxRows.size
    }
    val visibleRowsCount = remember(
        displayedTopInfoRows.size,
        displayedSystemRows.size,
        displayedSecureRows.size,
        displayedGlobalRows.size,
        displayedAndroidRows.size,
        displayedJavaRows.size,
        displayedLinuxRows.size
    ) {
        displayedTopInfoRows.size +
            displayedSystemRows.size +
            displayedSecureRows.size +
            displayedGlobalRows.size +
            displayedAndroidRows.size +
            displayedJavaRows.size +
            displayedLinuxRows.size
    }

    fun sectionSubtitle(section: SectionKind, size: Int): String {
        val state = sectionStates[section] ?: SectionState()
        return when {
            state.loading -> "加载中..."
            !state.loadedFresh && state.rows.isNotEmpty() -> "$size 条（缓存）"
            !state.loadedFresh && state.rows.isEmpty() -> "未加载"
            else -> "$size 条"
        }
    }
    val visibleSectionKinds = visibleSectionKinds()
    val visibleSectionStates = visibleSectionKinds.mapNotNull { sectionStates[it] }
    val loadedFreshCount = visibleSectionStates.count { it.loadedFresh }
    val cachedSectionCount = visibleSectionStates.count { !it.loadedFresh && it.rows.isNotEmpty() }
    val sectionCount = visibleSectionKinds.size
    val overviewState = when {
        refreshing -> SystemOverviewState.Refreshing
        loadedFreshCount == sectionCount && sectionCount > 0 -> SystemOverviewState.Completed
        cachePersisted || cachedSectionCount > 0 -> SystemOverviewState.Cached
        else -> SystemOverviewState.Idle
    }
    val statusLabel = when (overviewState) {
        SystemOverviewState.Cached -> StatusLabelText.Cached
        SystemOverviewState.Refreshing -> StatusLabelText.Syncing
        SystemOverviewState.Completed -> StatusLabelText.Synced
        SystemOverviewState.Idle -> StatusLabelText.PendingSync
    }
    val statusColor = when (overviewState) {
        SystemOverviewState.Cached -> cachedColor
        SystemOverviewState.Refreshing -> refreshingColor
        SystemOverviewState.Completed -> syncedColor
        SystemOverviewState.Idle -> inactive
    }
    val overviewCardColor = if (overviewState == SystemOverviewState.Idle) {
        MiuixTheme.colorScheme.surface.copy(alpha = 0.66f)
    } else {
        statusColor.copy(alpha = if (isDark) 0.24f else 0.16f)
    }
    val overviewItemColor = if (overviewState == SystemOverviewState.Idle) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f)
    } else {
        statusColor.copy(alpha = if (isDark) 0.20f else 0.12f)
    }
    val indicatorProgress = when (overviewState) {
        SystemOverviewState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
        SystemOverviewState.Completed,
        SystemOverviewState.Cached -> 1f
        SystemOverviewState.Idle -> 0f
    }
    val indicatorBg = when (overviewState) {
        SystemOverviewState.Refreshing -> Color(0x553B82F6)
        SystemOverviewState.Completed -> Color(0x5522C55E)
        SystemOverviewState.Cached -> Color(0x55F59E0B)
        SystemOverviewState.Idle -> MiuixTheme.colorScheme.surface
    }
    val overviewMetrics = remember(
        topInfoRows.size,
        totalRowsCount,
        visibleRowsCount,
        sectionCount,
        loadedFreshCount
    ) {
        listOf(
            OsOverviewMetric(label = "条目视图", value = "$visibleRowsCount/$totalRowsCount"),
            OsOverviewMetric(label = "TopInfo", value = "${topInfoRows.size} 条"),
            OsOverviewMetric(label = "Fresh 覆盖", value = "$loadedFreshCount/$sectionCount"),
            OsOverviewMetric(label = "可见分区", value = "$sectionCount 个")
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = "OS",
                    scrollBehavior = scrollBehavior,
                    color = topBarMaterialBackdrop.getMiuixAppBarColor(),
                    actions = {
                        LiquidActionBar(
                            backdrop = backdrop,
                            items = listOf(
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Edit,
                                    contentDescription = "管理卡片显示",
                                    onClick = { showCardManager = true }
                                ),
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Download,
                                    contentDescription = if (exportPreparing) "准备导出中" else "导出",
                                    onClick = {
                                        if (exportPreparing) return@LiquidActionItem
                                        exportPreparing = true
                                        scope.launch {
                                            val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                            val markdown = withContext(Dispatchers.IO) {
                                                val exportSections = buildExportSections(context, shizukuStatus, shizukuApiUtils)
                                                buildOsMarkdown(generatedAt, shizukuStatus, exportSections)
                                            }
                                            val fileName = "keios-os-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}.md"
                                            pendingExportContent = markdown
                                            exportPreparing = false
                                            exportLauncher.launch(fileName)
                                        }
                                    }
                                ),
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新OS参数",
                                    onClick = {
                                        if (refreshing) return@LiquidActionItem
                                        scope.launch { refreshAllSections() }
                                    }
                                )
                            ),
                            onInteractionChanged = onActionBarInteractingChanged
                        )
                    }
                )
                GlassSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    label = "搜索OS参数",
                    backdrop = backdrop,
                    variant = GlassVariant.Bar,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { innerPadding ->
        SnapshotWindowBottomSheet(
            show = showCardManager,
            title = "显示卡片",
            onDismissRequest = { showCardManager = false },
            startAction = {
                GlassIconButton(
                    backdrop = backdrop,
                    variant = GlassVariant.Bar,
                    icon = MiuixIcons.Regular.Close,
                    contentDescription = "关闭",
                    onClick = { showCardManager = false }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                @Composable
                fun CardLabel(card: OsSectionCard, modifier: Modifier = Modifier) {
                    Row(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconModifier = Modifier
                            .size(18.dp)
                            .defaultMinSize(minHeight = 18.dp)
                        val icon = when (card) {
                            OsSectionCard.TOP_INFO -> MiuixIcons.Regular.Info
                            OsSectionCard.SYSTEM -> MiuixIcons.Regular.ListView
                            OsSectionCard.SECURE -> MiuixIcons.Regular.Lock
                            OsSectionCard.GLOBAL -> MiuixIcons.Regular.Layers
                            OsSectionCard.ANDROID -> MiuixIcons.Regular.GridView
                            OsSectionCard.JAVA -> MiuixIcons.Regular.Tune
                            OsSectionCard.LINUX -> MiuixIcons.Regular.Filter
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = card.title,
                            tint = MiuixTheme.colorScheme.onBackground,
                            modifier = iconModifier
                        )
                        Text(text = card.title, color = MiuixTheme.colorScheme.onBackground)
                    }
                }

                OsSectionCard.entries.forEach { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CardLabel(card = card, modifier = Modifier.defaultMinSize(minHeight = 24.dp))
                        Switch(
                            checked = isCardVisible(card),
                            onCheckedChange = { checked ->
                                scope.launch { applyCardVisibility(card, checked) }
                            }
                        )
                    }
                }

                SheetDescriptionText(
                    text = "隐藏卡片后会清空对应缓存；重新显示时会立即重新获取并缓存。"
                )
            }
        }
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
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = overviewCardColor,
                        contentColor = titleColor
                    ),
                    showIndication = cardPressFeedbackEnabled,
                    onClick = {
                        if (refreshing) return@Card
                        scope.launch { refreshAllSections() }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("系统参数与属性", color = titleColor)
                            Spacer(modifier = Modifier.weight(1f))
                            if (overviewState != SystemOverviewState.Idle) {
                                CircularProgressIndicator(
                                    progress = indicatorProgress,
                                    size = 16.dp,
                                    strokeWidth = 2.dp,
                                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                        foregroundColor = statusColor,
                                        backgroundColor = indicatorBg
                                    )
                                )
                            }
                            StatusPill(label = statusLabel, color = statusColor)
                        }
                        overviewMetrics.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OsOverviewMetricItem(
                                    metric = pair[0],
                                    cardColor = overviewItemColor,
                                    labelColor = subtitleColor,
                                    defaultValueColor = titleColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pair.size > 1) {
                                    OsOverviewMetricItem(
                                        metric = pair[1],
                                        cardColor = overviewItemColor,
                                        labelColor = subtitleColor,
                                        defaultValueColor = titleColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            if (isCardVisible(OsSectionCard.TOP_INFO)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "TopInfo",
                    subtitle = "${displayedTopInfoRows.size} 条",
                    expanded = topInfoExpanded,
                    onExpandedChange = { topInfoExpanded = it }
                ) {
                    if (displayedTopInfoRows.isEmpty()) {
                        Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    } else {
                        if (q.isBlank() && !topInfoExpanded) {
                            displayedTopInfoRows.forEach { row ->
                                MiuixInfoItem(row.key, row.value)
                            }
                        } else {
                            groupedTopInfoRows.forEachIndexed { index, (type, rows) ->
                                Text(
                                    text = type,
                                    color = MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 2.dp)
                                )
                                rows.forEach { row ->
                                    MiuixInfoItem(row.key, row.value)
                                }
                            }
                        }
                    }
                }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (isCardVisible(OsSectionCard.SYSTEM)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "System Table",
                    subtitle = sectionSubtitle(SectionKind.SYSTEM, if (q.isBlank()) prunedSystemRows.size else displayedSystemRows.size),
                    expanded = systemTableExpanded,
                    onExpandedChange = { systemTableExpanded = it }
                ) {
                    if (displayedSystemRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedSystemRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (isCardVisible(OsSectionCard.SECURE)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "Secure Table",
                    subtitle = sectionSubtitle(SectionKind.SECURE, if (q.isBlank()) prunedSecureRows.size else displayedSecureRows.size),
                    expanded = secureTableExpanded,
                    onExpandedChange = { secureTableExpanded = it }
                ) {
                    if (displayedSecureRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedSecureRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (isCardVisible(OsSectionCard.GLOBAL)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "Global Table",
                    subtitle = sectionSubtitle(SectionKind.GLOBAL, if (q.isBlank()) prunedGlobalRows.size else displayedGlobalRows.size),
                    expanded = globalTableExpanded,
                    onExpandedChange = { globalTableExpanded = it }
                ) {
                    if (displayedGlobalRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedGlobalRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (isCardVisible(OsSectionCard.ANDROID)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "Android Properties",
                    subtitle = sectionSubtitle(SectionKind.ANDROID, if (q.isBlank()) prunedAndroidRows.size else displayedAndroidRows.size),
                    expanded = androidPropsExpanded,
                    onExpandedChange = { androidPropsExpanded = it }
                ) {
                    if (displayedAndroidRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedAndroidRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (isCardVisible(OsSectionCard.JAVA)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "Java Properties",
                    subtitle = sectionSubtitle(SectionKind.JAVA, if (q.isBlank()) prunedJavaRows.size else displayedJavaRows.size),
                    expanded = javaPropsExpanded,
                    onExpandedChange = { javaPropsExpanded = it }
                ) {
                    if (displayedJavaRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedJavaRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (isCardVisible(OsSectionCard.LINUX)) {
                item {
                MiuixExpandableSection(
                    backdrop = backdrop,
                    title = "Linux environment",
                    subtitle = sectionSubtitle(SectionKind.LINUX, if (q.isBlank()) prunedLinuxRows.size else displayedLinuxRows.size),
                    expanded = linuxEnvExpanded,
                    onExpandedChange = { linuxEnvExpanded = it }
                ) {
                    if (displayedLinuxRows.isEmpty()) Text(text = "No matched results.", color = MiuixTheme.colorScheme.onBackgroundVariant)
                    else displayedLinuxRows.forEach { row -> MiuixInfoItem(row.key, row.value) }
                }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

private data class OsOverviewMetric(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

@Composable
private fun OsOverviewMetricItem(
    metric: OsOverviewMetric,
    cardColor: Color,
    labelColor: Color,
    defaultValueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.defaultColors(
            color = cardColor,
            contentColor = defaultValueColor
        ),
        showIndication = false,
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = metric.label,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value.ifBlank { "N/A" },
                color = metric.valueColor ?: defaultValueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
