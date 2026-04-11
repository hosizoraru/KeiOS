package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.GuideBottomTab
import com.example.keios.ui.page.main.student.GuideTab
import com.example.keios.ui.page.main.student.GuideCombatMetaTile
import com.example.keios.ui.page.main.student.GuideGallerySection
import com.example.keios.ui.page.main.student.GuideProfileMetaLine
import com.example.keios.ui.page.main.student.GuideRemoteImage
import com.example.keios.ui.page.main.student.GuideRowsSection
import com.example.keios.ui.page.main.student.GuideSkillCardItem
import com.example.keios.ui.page.main.student.GuideVoiceEntryCard
import com.example.keios.ui.page.main.student.GuideVoiceLanguageCard
import com.example.keios.ui.page.main.student.GuideWeaponCardItem
import com.example.keios.ui.page.main.student.buildCombatMetaItems
import com.example.keios.ui.page.main.student.buildProfileMetaItems
import com.example.keios.ui.page.main.student.fetchGuideInfo
import com.example.keios.ui.page.main.student.growthRowsForDisplay
import com.example.keios.ui.page.main.student.normalizeGuideUrl
import com.example.keios.ui.page.main.student.profileRowsForDisplay
import com.example.keios.ui.page.main.student.shouldHideMovedHeaderRow
import com.example.keios.ui.page.main.student.showLoadingText
import com.example.keios.ui.page.main.student.skillCardsForDisplay
import com.example.keios.ui.page.main.student.weaponCardForDisplay
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BaStudentGuidePage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val scrollBehavior = MiuixScrollBehavior()

    val sourceUrl = remember { BaStudentGuideStore.loadCurrentUrl() }
    var info by remember(sourceUrl) { mutableStateOf(BaStudentGuideStore.loadInfo(sourceUrl)) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }
    var selectedBottomTabIndex by rememberSaveable(sourceUrl) { mutableIntStateOf(0) }
    var playingVoiceUrl by rememberSaveable(sourceUrl) { mutableStateOf("") }
    val bottomTabs = GuideBottomTab.entries
    val activeBottomTab = bottomTabs.getOrElse(selectedBottomTabIndex) { GuideBottomTab.Archive }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pageTitle = info?.title?.ifBlank { "学生图鉴" } ?: "学生图鉴"
    val voicePlayer = remember(context, sourceUrl) {
        // GameKee CDN 对语音资源的防盗链策略更偏向根站 Referer，
        // 使用详情页 Referer 容易返回 567（EdgeOne 拦截页）。
        val referer = "https://www.gamekee.com/"
        val desktopUa =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
        val defaultHeaders = mapOf(
            "Accept" to "*/*",
            "Accept-Language" to "zh-CN",
            "Referer" to referer,
            "Origin" to "https://www.gamekee.com",
            "User-Agent" to desktopUa,
            "device-num" to "1",
            "game-alias" to "ba",
            "Connection" to "close"
        )
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(desktopUa)
            .setDefaultRequestProperties(defaultHeaders)
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(context, httpFactory)
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    DisposableEffect(voicePlayer) {
        onDispose {
            runCatching { voicePlayer.release() }
        }
    }

    DisposableEffect(voicePlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    if (!voicePlayer.isPlaying) {
                        playingVoiceUrl = ""
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playingVoiceUrl = ""
                Toast.makeText(context, "语音播放失败：${error.errorCodeName}", Toast.LENGTH_SHORT).show()
            }
        }
        voicePlayer.addListener(listener)
        onDispose {
            voicePlayer.removeListener(listener)
        }
    }

    fun openExternal(url: String) {
        val target = normalizeGuideUrl(url)
        if (target.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleVoicePlayback(rawAudioUrl: String) {
        val target = normalizeGuideUrl(rawAudioUrl)
        if (target.isBlank()) return
        runCatching {
            if (playingVoiceUrl == target) {
                if (voicePlayer.isPlaying) {
                    voicePlayer.pause()
                    playingVoiceUrl = ""
                } else {
                    voicePlayer.play()
                    playingVoiceUrl = target
                }
            } else {
                voicePlayer.setMediaItem(MediaItem.fromUri(target))
                voicePlayer.prepare()
                voicePlayer.play()
                playingVoiceUrl = target
            }
        }.onFailure {
            playingVoiceUrl = ""
            Toast.makeText(context, "语音播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(sourceUrl, refreshSignal) {
        if (sourceUrl.isBlank()) return@LaunchedEffect
        val cached = BaStudentGuideStore.loadInfo(sourceUrl)
        if (cached != null) info = cached
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { fetchGuideInfo(sourceUrl) }
        }
        result.onSuccess { latest ->
            info = latest
            error = null
            withContext(Dispatchers.IO) { BaStudentGuideStore.saveInfo(latest) }
        }.onFailure {
            error = if (info != null) "网络请求失败，已显示本地缓存" else "图鉴信息加载失败"
        }
        loading = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = pageTitle,
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
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
                    IconButton(
                        onClick = {
                            refreshSignal += 1
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Refresh,
                            contentDescription = "刷新",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                    selectedIndex = { selectedBottomTabIndex },
                    onSelected = { index -> selectedBottomTabIndex = index },
                    backdrop = backdrop,
                    tabsCount = bottomTabs.size,
                    isBlurEnabled = true
                ) {
                    bottomTabs.forEachIndexed { index, tab ->
                        FloatingBottomBarItem(
                            onClick = { selectedBottomTabIndex = index },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            val tabIconModifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = 1f
                                    scaleY = 1f
                                }
                            if (tab.localLogoRes != null) {
                                Icon(
                                    painter = painterResource(id = tab.localLogoRes),
                                    contentDescription = tab.label,
                                    tint = Color.Unspecified,
                                    modifier = tabIconModifier
                                )
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = tabIconModifier
                                )
                            }
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item { SmallTitle(activeBottomTab.label) }
            item { Spacer(modifier = Modifier.height(14.dp)) }

            if (sourceUrl.isBlank()) {
                item {
                    FrostedBlock(
                        backdrop = backdrop,
                        title = "未选择学生",
                        subtitle = "请从 BA 卡池信息中点击对应卡池进入",
                        accent = accent
                    )
                }
            } else {
                when (activeBottomTab) {
                    GuideBottomTab.Archive -> {
                        item {
                            val guide = info
                            val profileItems = guide?.buildProfileMetaItems().orEmpty()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0x223B82F6),
                                    contentColor = MiuixTheme.colorScheme.onBackground
                                ),
                                onClick = {}
                            ) {
                                if (showLoadingText(loading = loading, hasInfo = guide != null)) {
                                    Text(
                                        text = "同步中...",
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }
                                if (guide != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(modifier = Modifier.width(112.dp)) {
                                                if (guide.imageUrl.isNotBlank()) {
                                                    GuideRemoteImage(
                                                        imageUrl = guide.imageUrl,
                                                        imageHeight = 152.dp
                                                    )
                                                } else {
                                                    Text(
                                                        text = "暂无图片",
                                                        color = MiuixTheme.colorScheme.onBackgroundVariant
                                                    )
                                                }
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(152.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                profileItems.forEach { item ->
                                                    GuideProfileMetaLine(item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(10.dp)) }
                        item {
                            val guide = info
                            val combatItems = guide?.buildCombatMetaItems().orEmpty()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = Color(0x223B82F6),
                                    contentColor = MiuixTheme.colorScheme.onBackground
                                ),
                                onClick = {}
                            ) {
                                if (showLoadingText(loading = loading, hasInfo = guide != null)) {
                                    Text(
                                        text = "同步中...",
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }
                                if (guide != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        combatItems.forEach { item ->
                                            GuideCombatMetaTile(
                                                item = item,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GuideBottomTab.Skills -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent,
                                    content = {
                                        if (loading) {
                                            Text("同步中...", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                        }
                                        error?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = it,
                                                color = MiuixTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            val skillCards = guide.skillCardsForDisplay()
                            val weaponCard = guide.weaponCardForDisplay()

                            if (showLoadingText(loading = loading, hasInfo = true) || !error.isNullOrBlank()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.defaultColors(
                                            color = Color(0x223B82F6),
                                            contentColor = MiuixTheme.colorScheme.onBackground
                                        ),
                                        onClick = {}
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (showLoadingText(loading = loading, hasInfo = true)) {
                                                Text("同步中...", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                            }
                                            error?.takeIf { it.isNotBlank() }?.let {
                                                Text(
                                                    text = it,
                                                    color = MiuixTheme.colorScheme.error,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (skillCards.isNotEmpty()) {
                                skillCards.forEachIndexed { index, card ->
                                    item {
                                        GuideSkillCardItem(
                                            card = card,
                                            backdrop = backdrop
                                        )
                                    }
                                    if (index < skillCards.lastIndex) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                }
                            } else {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.defaultColors(
                                            color = Color(0x223B82F6),
                                            contentColor = MiuixTheme.colorScheme.onBackground
                                        ),
                                        onClick = {}
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "暂未解析到结构化技能卡数据。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                            }

                            weaponCard?.let { weapon ->
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                                item {
                                    GuideWeaponCardItem(
                                        card = weapon,
                                        backdrop = backdrop
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(10.dp)) }
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.defaultColors(
                                        color = Color(0x223B82F6),
                                        contentColor = MiuixTheme.colorScheme.onBackground
                                    ),
                                    onClick = {}
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        MiuixInfoItem(
                                            key = "来源",
                                            value = guide.sourceUrl,
                                            onClick = { openExternal(guide.sourceUrl) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    GuideBottomTab.Voice -> {
                        val guide = info
                        if (guide == null) {
                            item {
                                FrostedBlock(
                                    backdrop = backdrop,
                                    title = activeBottomTab.label,
                                    subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                    accent = accent
                                )
                            }
                        } else {
                            val voiceEntries = guide.voiceEntries.filter {
                                val jp = it.lines.getOrNull(0).orEmpty().trim()
                                val cn = it.lines.getOrNull(1).orEmpty().trim()
                                jp.isNotBlank() || cn.isNotBlank()
                            }

                            if (showLoadingText(loading = loading, hasInfo = true) || !error.isNullOrBlank()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.defaultColors(
                                            color = Color(0x223B82F6),
                                            contentColor = MiuixTheme.colorScheme.onBackground
                                        ),
                                        onClick = {}
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (showLoadingText(loading = loading, hasInfo = true)) {
                                                Text("同步中...", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                            }
                                            error?.takeIf { it.isNotBlank() }?.let {
                                                Text(
                                                    text = it,
                                                    color = MiuixTheme.colorScheme.error,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (guide.voiceLanguageHeaders.isNotEmpty()) {
                                item {
                                    GuideVoiceLanguageCard(
                                        headers = guide.voiceLanguageHeaders,
                                        backdrop = backdrop
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(10.dp)) }
                            }

                            if (voiceEntries.isNotEmpty()) {
                                voiceEntries.forEachIndexed { index, entry ->
                                    item {
                                        GuideVoiceEntryCard(
                                            entry = entry,
                                            languageHeaders = guide.voiceLanguageHeaders,
                                            backdrop = backdrop,
                                            isPlaying = normalizeGuideUrl(entry.audioUrl) == playingVoiceUrl && voicePlayer.isPlaying,
                                            onTogglePlay = ::toggleVoicePlayback
                                        )
                                    }
                                    if (index < voiceEntries.lastIndex) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                }
                            } else {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.defaultColors(
                                            color = Color(0x223B82F6),
                                            contentColor = MiuixTheme.colorScheme.onBackground
                                        ),
                                        onClick = {}
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "暂未解析到结构化语音台词，点击右上角刷新后重试。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(10.dp)) }
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.defaultColors(
                                        color = Color(0x223B82F6),
                                        contentColor = MiuixTheme.colorScheme.onBackground
                                    ),
                                    onClick = {}
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        MiuixInfoItem(
                                            key = "来源",
                                            value = guide.sourceUrl,
                                            onClick = { openExternal(guide.sourceUrl) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            FrostedBlock(
                                backdrop = backdrop,
                                title = activeBottomTab.label,
                                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                                accent = accent,
                                content = {
                                    if (showLoadingText(loading = loading, hasInfo = info != null)) {
                                        Text("同步中...", color = MiuixTheme.colorScheme.onBackgroundVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    error?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            color = MiuixTheme.colorScheme.error,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    val guide = info
                                    if (guide != null) {
                                        val profileRows = guide.profileRowsForDisplay()
                                        val visibleProfileRows = profileRows.filterNot { shouldHideMovedHeaderRow(it) }
                                        val growthRows = guide.growthRowsForDisplay()
                                        val galleryItems = if (guide.galleryItems.isNotEmpty()) {
                                            guide.galleryItems
                                        } else {
                                            listOfNotNull(
                                                guide.imageUrl
                                                    .takeIf { it.isNotBlank() }
                                                    ?.let { BaGuideGalleryItem("立绘", it) }
                                            )
                                        }
                                        val activeGuideTab = activeBottomTab.guideTab

                                        Text(
                                            text = guide.summary.ifBlank { guide.description },
                                            color = MiuixTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        when (activeGuideTab) {
                                            GuideTab.Profile -> {
                                                GuideRowsSection(
                                                    rows = visibleProfileRows,
                                                    emptyText = "暂未解析到学生档案数据。"
                                                )
                                            }

                                            GuideTab.Voice -> {
                                                GuideRowsSection(
                                                    rows = guide.voiceRows,
                                                    emptyText = "语音台词解析中，当前版本先完善其他栏目。"
                                                )
                                            }

                                            GuideTab.Gallery -> {
                                                GuideGallerySection(
                                                    items = galleryItems,
                                                    emptyText = "暂未解析到影画鉴赏内容。"
                                                )
                                            }

                                            GuideTab.Simulate -> {
                                                GuideRowsSection(
                                                    rows = growthRows,
                                                    emptyText = "暂未解析到养成模拟数据。"
                                                )
                                            }

                                            else -> {}
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        MiuixInfoItem(
                                            "来源",
                                            guide.sourceUrl,
                                            onClick = { openExternal(guide.sourceUrl) }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
