package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.BaGuideTempMediaCache
import com.example.keios.ui.page.main.student.BaStudentGuideStore
import com.example.keios.ui.page.main.student.createGameKeeMediaSourceFactory
import com.example.keios.ui.page.main.student.GuideBottomTab
import com.example.keios.ui.page.main.student.GuideTab
import com.example.keios.ui.page.main.student.GuideCombatMetaTile
import com.example.keios.ui.page.main.student.GuideGalleryCardItem
import com.example.keios.ui.page.main.student.GuideGalleryExpressionCardItem
import com.example.keios.ui.page.main.student.GuideGalleryUnlockLevelCardItem
import com.example.keios.ui.page.main.student.GuideGalleryVideoGroupCardItem
import com.example.keios.ui.page.main.student.GuideProfileMetaLine
import com.example.keios.ui.page.main.student.GuideRemoteImage
import com.example.keios.ui.page.main.student.GuideRowsSection
import com.example.keios.ui.page.main.student.GuideSkillCardItem
import com.example.keios.ui.page.main.student.GuideVoiceEntryCard
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
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.utils.UiPrefs
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import top.yukonga.miuix.kmp.icon.extended.Share
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
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val scrollBehavior = MiuixScrollBehavior()

    val sourceUrl = remember { BaStudentGuideStore.loadCurrentUrl() }
    var info by remember(sourceUrl) { mutableStateOf(BaStudentGuideStore.loadInfo(sourceUrl)) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }
    var selectedBottomTabIndex by rememberSaveable(sourceUrl) { mutableIntStateOf(0) }
    var playingVoiceUrl by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var isVoicePlaying by remember(sourceUrl) { mutableStateOf(false) }
    var voicePlayProgress by remember(sourceUrl) { mutableFloatStateOf(0f) }
    var galleryPrefetchRequested by rememberSaveable(sourceUrl) { mutableStateOf(false) }
    var galleryCacheRevision by remember(sourceUrl) { mutableIntStateOf(0) }
    val bottomTabs = GuideBottomTab.entries
    val activeBottomTab = bottomTabs.getOrElse(selectedBottomTabIndex) { GuideBottomTab.Archive }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liquidBottomBarEnabled = remember { UiPrefs.isLiquidBottomBarEnabled() }
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
    val pageTitle = info?.title?.ifBlank { "学生图鉴" } ?: "学生图鉴"
    val voicePlayer = remember(context, sourceUrl) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
            .build()
    }

    DisposableEffect(voicePlayer) {
        onDispose {
            runCatching { voicePlayer.release() }
        }
    }

    DisposableEffect(sourceUrl) {
        onDispose {
            BaGuideTempMediaCache.clearGuideCache(context, sourceUrl)
        }
    }

    DisposableEffect(voicePlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isVoicePlaying = isPlaying && playingVoiceUrl.isNotBlank()
                if (!isVoicePlaying) {
                    voicePlayProgress = 0f
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                voicePlayProgress = 0f
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        playingVoiceUrl = ""
                        isVoicePlaying = false
                        voicePlayProgress = 0f
                    }
                    Player.STATE_READY -> {
                        isVoicePlaying = voicePlayer.isPlaying && playingVoiceUrl.isNotBlank()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playingVoiceUrl = ""
                isVoicePlaying = false
                voicePlayProgress = 0f
                Toast.makeText(context, "语音播放失败：${error.errorCodeName}", Toast.LENGTH_SHORT).show()
            }
        }
        voicePlayer.addListener(listener)
        onDispose {
            voicePlayer.removeListener(listener)
        }
    }

    fun shareSource() {
        val raw = info?.sourceUrl?.ifBlank { sourceUrl } ?: sourceUrl
        val target = normalizeGuideUrl(raw)
        if (target.isBlank()) {
            Toast.makeText(context, "暂无可分享的来源链接", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, target)
            }
            val chooser = Intent.createChooser(intent, "分享来源").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure {
            Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun openExternal(rawUrl: String) {
        val target = normalizeGuideUrl(rawUrl)
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
                    isVoicePlaying = false
                    voicePlayProgress = 0f
                } else {
                    voicePlayer.play()
                    playingVoiceUrl = target
                    isVoicePlaying = true
                }
            } else {
                voicePlayer.setMediaItem(MediaItem.fromUri(target))
                voicePlayer.prepare()
                voicePlayer.play()
                playingVoiceUrl = target
                isVoicePlaying = true
                voicePlayProgress = 0f
            }
        }.onFailure {
            playingVoiceUrl = ""
            isVoicePlaying = false
            voicePlayProgress = 0f
            Toast.makeText(context, "语音播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isVoicePlaying, playingVoiceUrl) {
        if (!isVoicePlaying || playingVoiceUrl.isBlank()) {
            voicePlayProgress = 0f
            return@LaunchedEffect
        }
        while (isVoicePlaying && playingVoiceUrl.isNotBlank()) {
            val duration = voicePlayer.duration
            val position = voicePlayer.currentPosition
            voicePlayProgress = if (duration > 0L && position >= 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(220)
        }
    }

    LaunchedEffect(activeBottomTab) {
        if (activeBottomTab == GuideBottomTab.Gallery) {
            galleryPrefetchRequested = true
        }
    }

    LaunchedEffect(sourceUrl, galleryPrefetchRequested, info?.syncedAtMs) {
        if (!galleryPrefetchRequested) return@LaunchedEffect
        val guide = info ?: return@LaunchedEffect
        val galleryItemsForPrefetch = if (guide.galleryItems.isNotEmpty()) {
            guide.galleryItems
                .filter(::hasRenderableGalleryMedia)
                .distinctBy { "${it.mediaType}|${it.mediaUrl.ifBlank { it.imageUrl }}" }
        } else {
            listOfNotNull(
                guide.imageUrl.takeIf { it.isNotBlank() }?.let {
                    BaGuideGalleryItem(
                        title = "立绘",
                        imageUrl = it,
                        mediaType = "image",
                        mediaUrl = it
                    ).takeIf(::hasRenderableGalleryMedia)
                }
            )
        }.filterNot(::isMemoryHallFileGalleryItem)

        val urls = galleryItemsForPrefetch
            .flatMap { item -> listOf(item.imageUrl, item.mediaUrl) }
            .filter { it.isNotBlank() && (isRenderableGalleryImageUrl(it) || isRenderableGalleryVideoUrl(it)) }
            .distinct()
        if (urls.isEmpty()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            BaGuideTempMediaCache.prefetchForGuide(
                context = context,
                sourceUrl = sourceUrl,
                rawUrls = urls
            )
        }
        galleryCacheRevision += 1
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(bottomBarNestedScrollConnection),
        topBar = {
            TopAppBar(
                title = pageTitle,
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
                    Box {
                        LiquidActionBar(
                            backdrop = backdrop,
                            items = listOf(
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Share,
                                    contentDescription = "分享来源",
                                    onClick = ::shareSource
                                ),
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新",
                                    onClick = { refreshSignal += 1 }
                                )
                            )
                        )
                    }
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
                        selectedIndex = { selectedBottomTabIndex },
                        onSelected = { index -> selectedBottomTabIndex = index },
                        backdrop = backdrop,
                        tabsCount = bottomTabs.size,
                        isBlurEnabled = liquidBottomBarEnabled
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
                                    val useThemeTintForLocalLogo =
                                        tab == GuideBottomTab.Skills || tab == GuideBottomTab.Profile || tab == GuideBottomTab.Simulate
                                    Icon(
                                        painter = painterResource(id = tab.localLogoRes),
                                        contentDescription = tab.label,
                                        tint = if (useThemeTintForLocalLogo) {
                                            MiuixTheme.colorScheme.onSurface
                                        } else {
                                            Color.Unspecified
                                        },
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

                            if (voiceEntries.isNotEmpty()) {
                                voiceEntries.forEachIndexed { index, entry ->
                                    item {
                                        GuideVoiceEntryCard(
                                            entry = entry,
                                            languageHeaders = guide.voiceLanguageHeaders,
                                            backdrop = backdrop,
                                            isPlaying = normalizeGuideUrl(entry.audioUrl) == playingVoiceUrl && isVoicePlaying,
                                            playProgress = if (normalizeGuideUrl(entry.audioUrl) == playingVoiceUrl) {
                                                voicePlayProgress
                                            } else {
                                                0f
                                            },
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
                        }
                    }

                    GuideBottomTab.Gallery -> {
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
                            val galleryItems = if (guide.galleryItems.isNotEmpty()) {
                                guide.galleryItems
                                    .filter(::hasRenderableGalleryMedia)
                                    .distinctBy {
                                        val media = it.mediaUrl.ifBlank { it.imageUrl }
                                        "${it.mediaType}|$media"
                                    }
                            } else {
                                listOfNotNull(
                                    guide.imageUrl.takeIf { it.isNotBlank() }?.let {
                                        BaGuideGalleryItem(
                                            title = "立绘",
                                            imageUrl = it,
                                            mediaType = "image",
                                            mediaUrl = it
                                        ).takeIf(::hasRenderableGalleryMedia)
                                    }
                                )
                            }
                            val cleanedGalleryItems = galleryItems.filterNot(::isMemoryHallFileGalleryItem)
                            val memoryHallPreview = cleanedGalleryItems
                                .firstOrNull {
                                    isMemoryHallGalleryItem(it) && isRenderableGalleryImageUrl(it.imageUrl)
                                }
                                ?.imageUrl
                                .orEmpty()
                            val previewVideoGroups = run {
                                val orderedCategories = cleanedGalleryItems
                                    .asSequence()
                                    .mapNotNull { item ->
                                        if (!isPreviewVideoGalleryItem(item)) return@mapNotNull null
                                        val normalized = normalizeGalleryTitle(item.title)
                                        when {
                                            normalized.startsWith("回忆大厅视频") -> "回忆大厅视频"
                                            normalized.startsWith("PV") -> "PV"
                                            normalized.startsWith("角色演示") -> "角色演示"
                                            else -> null
                                        }
                                    }
                                    .distinct()
                                    .toList()

                                orderedCategories.mapNotNull { category ->
                                    val categoryFallbackPreview =
                                        if (category == "回忆大厅视频" && isRenderableGalleryImageUrl(memoryHallPreview)) {
                                            memoryHallPreview
                                        } else {
                                            ""
                                        }
                                    val categoryItems = cleanedGalleryItems
                                        .asSequence()
                                        .filter(::isPreviewVideoGalleryItem)
                                        .filter { item ->
                                            val normalized = normalizeGalleryTitle(item.title)
                                            when (category) {
                                                "回忆大厅视频" -> normalized.startsWith("回忆大厅视频")
                                                "PV" -> normalized.startsWith("PV")
                                                "角色演示" -> normalized.startsWith("角色演示")
                                                else -> false
                                            }
                                        }
                                        .mapNotNull { item ->
                                            val currentPreview = item.imageUrl
                                            val preview = when {
                                                isRenderableGalleryImageUrl(currentPreview) -> currentPreview
                                                categoryFallbackPreview.isNotBlank() -> categoryFallbackPreview
                                                else -> ""
                                            }
                                            if (preview.isBlank()) {
                                                null
                                            } else if (preview == currentPreview) {
                                                item
                                            } else {
                                                item.copy(imageUrl = preview)
                                            }
                                        }
                                        .toList()
                                    categoryItems.takeIf { it.isNotEmpty() }?.let { category to it }
                                }
                            }
                            val memoryHallVideoGroup = previewVideoGroups.firstOrNull { it.first == "回忆大厅视频" }
                            val trailingVideoGroups = previewVideoGroups.filterNot { it.first == "回忆大厅视频" }
                            // 影画条目若没有可用封面图，则不渲染对应卡片，避免出现空壳卡片。
                            val displayGalleryItems = cleanedGalleryItems
                                .filterNot(::isPreviewVideoCategoryGalleryItem)
                                .filter { item ->
                                    when (item.mediaType.lowercase()) {
                                        "audio" -> isRenderableGalleryAudioUrl(item.mediaUrl)
                                        else -> isRenderableGalleryImageUrl(item.imageUrl)
                                    }
                                }
                            val memoryUnlockLevel = cleanedGalleryItems
                                .asSequence()
                                .map { it.memoryUnlockLevel }
                                .firstOrNull { it.isNotBlank() }
                                .orEmpty()
                                .ifBlank {
                                    val fallback = guide.profileRows
                                        .firstOrNull { it.key.trim() == "回忆大厅解锁等级" }
                                        ?.value
                                        .orEmpty()
                                    Regex("""\d+""").find(fallback)?.value.orEmpty().ifBlank { fallback }
                                }
                            val expressionItems = displayGalleryItems
                                .withIndex()
                                .filter { isExpressionGalleryItem(it.value) }
                                .sortedBy { indexed ->
                                    expressionGalleryOrder(indexed.value.title, indexed.index + 1)
                                }
                                .map { it.value }
                            val firstExpressionIndex = displayGalleryItems.indexOfFirst(::isExpressionGalleryItem)
                            val firstMemoryHallIndex = displayGalleryItems.indexOfFirst(::isMemoryHallGalleryItem)

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

                            if (displayGalleryItems.isNotEmpty() || previewVideoGroups.isNotEmpty()) {
                                var renderedCount = 0
                                var insertedUnlockLevel = false
                                var insertedMemoryHallVideoNearGallery = false
                                displayGalleryItems.forEachIndexed { index, item ->
                                    val isExpression = isExpressionGalleryItem(item)
                                    if (isExpression && index != firstExpressionIndex) {
                                        return@forEachIndexed
                                    }
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }

                                    // 将“回忆大厅解锁等级”展示在立绘组和回忆大厅之间。
                                    if (!insertedUnlockLevel &&
                                        memoryUnlockLevel.isNotBlank() &&
                                        index == firstMemoryHallIndex
                                    ) {
                                        item {
                                            GuideGalleryUnlockLevelCardItem(
                                                level = memoryUnlockLevel,
                                                backdrop = backdrop
                                            )
                                        }
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                        insertedUnlockLevel = true
                                    }

                                    item {
                                        if (isExpression && expressionItems.isNotEmpty()) {
                                            GuideGalleryExpressionCardItem(
                                                title = "角色表情",
                                                items = expressionItems,
                                                backdrop = backdrop,
                                                onOpenMedia = ::openExternal,
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        } else {
                                            GuideGalleryCardItem(
                                                item = item,
                                                backdrop = backdrop,
                                                onOpenMedia = ::openExternal,
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    renderedCount += 1

                                    // 将“回忆大厅视频”紧贴“回忆大厅”条目展示。
                                    if (!insertedMemoryHallVideoNearGallery &&
                                        memoryHallVideoGroup != null &&
                                        index == firstMemoryHallIndex
                                    ) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                        item {
                                            GuideGalleryVideoGroupCardItem(
                                                title = memoryHallVideoGroup.first,
                                                items = memoryHallVideoGroup.second,
                                                previewFallbackUrl = memoryHallPreview,
                                                backdrop = backdrop,
                                                onOpenMedia = ::openExternal,
                                                mediaUrlResolver = { raw ->
                                                    galleryCacheRevision.let {
                                                        BaGuideTempMediaCache.resolveCachedUrl(
                                                            context = context,
                                                            sourceUrl = sourceUrl,
                                                            rawUrl = raw
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        renderedCount += 1
                                        insertedMemoryHallVideoNearGallery = true
                                    }
                                }

                                if (!insertedUnlockLevel &&
                                    memoryUnlockLevel.isNotBlank() &&
                                    memoryHallVideoGroup != null
                                ) {
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                    item {
                                        GuideGalleryUnlockLevelCardItem(
                                            level = memoryUnlockLevel,
                                            backdrop = backdrop
                                        )
                                    }
                                    insertedUnlockLevel = true
                                    renderedCount += 1
                                }

                                if (!insertedMemoryHallVideoNearGallery && memoryHallVideoGroup != null) {
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                    item {
                                        GuideGalleryVideoGroupCardItem(
                                            title = memoryHallVideoGroup.first,
                                            items = memoryHallVideoGroup.second,
                                            previewFallbackUrl = memoryHallPreview,
                                            backdrop = backdrop,
                                            onOpenMedia = ::openExternal,
                                            mediaUrlResolver = { raw ->
                                                galleryCacheRevision.let {
                                                    BaGuideTempMediaCache.resolveCachedUrl(
                                                        context = context,
                                                        sourceUrl = sourceUrl,
                                                        rawUrl = raw
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    renderedCount += 1
                                }

                                trailingVideoGroups.forEach { (title, items) ->
                                    if (renderedCount > 0) {
                                        item { Spacer(modifier = Modifier.height(10.dp)) }
                                    }
                                    item {
                                        GuideGalleryVideoGroupCardItem(
                                            title = title,
                                            items = items,
                                            previewFallbackUrl = "",
                                            backdrop = backdrop,
                                            onOpenMedia = ::openExternal,
                                            mediaUrlResolver = { raw ->
                                                galleryCacheRevision.let {
                                                    BaGuideTempMediaCache.resolveCachedUrl(
                                                        context = context,
                                                        sourceUrl = sourceUrl,
                                                        rawUrl = raw
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    renderedCount += 1
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
                                                text = "暂未解析到影画鉴赏内容，点击右上角刷新后重试。",
                                                color = MiuixTheme.colorScheme.onBackgroundVariant
                                            )
                                        }
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

                                            GuideTab.Simulate -> {
                                                GuideRowsSection(
                                                    rows = growthRows,
                                                    emptyText = "暂未解析到养成模拟数据。"
                                                )
                                            }

                                            else -> {}
                                        }
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

private fun normalizeGalleryTitle(raw: String): String {
    return raw.replace(Regex("\\s+"), "").trim()
}

private fun isPlaceholderGalleryToken(raw: String): Boolean {
    val value = raw.trim().lowercase()
    if (value.isBlank()) return true
    if (value == "n" || value == "null" || value == "undefined" || value == "nan") return true
    return value.matches(Regex("""^\d+$"""))
}

private fun hasInvalidGameKeeMediaTail(rawUrl: String): Boolean {
    val value = rawUrl.trim()
    if (value.isBlank()) return true
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    if (!host.endsWith("gamekee.com")) return false
    val segments = uri.pathSegments.filter { it.isNotBlank() }
    if (segments.size != 1) return false
    return isPlaceholderGalleryToken(segments.first())
}

private fun isRenderableGalleryImageUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:image", ignoreCase = true)) return true
    if (isPlaceholderGalleryToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (Regex("""\.(mp3|ogg|wav|m4a|aac)(\?.*)?(#.*)?$""").containsMatchIn(lower)) {
        return false
    }
    if (Regex("""\.(png|jpg|jpeg|webp|gif|bmp|svg|avif)(\?.*)?(#.*)?$""").containsMatchIn(lower)) {
        return true
    }
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    val uri = runCatching { Uri.parse(normalized) }.getOrNull()
    val host = uri?.host?.lowercase().orEmpty()
    val path = (uri?.encodedPath ?: uri?.path ?: "").lowercase()
    if (host.contains("cdnimg") || host.contains("img")) return true
    if (path.contains("/upload") || path.contains("/uploads") || path.contains("/images/") || path.contains("/wiki/")) {
        return true
    }
    return lower.contains("x-oss-process=image")
}

private fun isRenderableGalleryVideoUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:video", ignoreCase = true)) return true
    if (isPlaceholderGalleryToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    return lower.endsWith(".mp4") ||
        lower.endsWith(".webm") ||
        lower.endsWith(".mov") ||
        lower.endsWith(".m3u8") ||
        lower.contains(".mp4?") ||
        lower.contains(".m3u8?")
}

private fun isRenderableGalleryAudioUrl(raw: String): Boolean {
    val value = raw.trim()
    if (value.isBlank()) return false
    if (value.startsWith("data:audio", ignoreCase = true)) return true
    if (isPlaceholderGalleryToken(value)) return false
    val normalized = if (value.startsWith("//")) "https:$value" else value
    val lower = normalized.lowercase()
    if (hasInvalidGameKeeMediaTail(normalized)) return false
    return lower.endsWith(".mp3") ||
        lower.endsWith(".ogg") ||
        lower.endsWith(".wav") ||
        lower.endsWith(".m4a") ||
        lower.endsWith(".aac") ||
        lower.contains(".mp3?") ||
        lower.contains(".ogg?") ||
        lower.contains(".wav?") ||
        lower.contains(".m4a?") ||
        lower.contains(".aac?")
}

private fun hasRenderableGalleryMedia(item: BaGuideGalleryItem): Boolean {
    val imageRenderable = isRenderableGalleryImageUrl(item.imageUrl)
    val mediaRenderable = when (item.mediaType.lowercase()) {
        "video" -> isRenderableGalleryVideoUrl(item.mediaUrl)
        "audio" -> isRenderableGalleryAudioUrl(item.mediaUrl)
        else -> isRenderableGalleryImageUrl(item.mediaUrl)
    }
    return imageRenderable || mediaRenderable
}

private fun isMemoryHallFileGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("回忆大厅文件")
}

private fun isExpressionGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("角色表情")
}

private fun isMemoryHallGalleryItem(item: BaGuideGalleryItem): Boolean {
    val title = normalizeGalleryTitle(item.title)
    return title.startsWith("回忆大厅") && !title.startsWith("回忆大厅视频") && !title.startsWith("回忆大厅文件")
}

private fun isPreviewVideoCategoryTitle(rawTitle: String): Boolean {
    val title = normalizeGalleryTitle(rawTitle)
    return title.startsWith("回忆大厅视频") || title.startsWith("PV") || title.startsWith("角色演示")
}

private fun isPreviewVideoCategoryGalleryItem(item: BaGuideGalleryItem): Boolean {
    return isPreviewVideoCategoryTitle(item.title)
}

private fun isPreviewVideoGalleryItem(item: BaGuideGalleryItem): Boolean {
    if (item.mediaType.lowercase() != "video") return false
    return isPreviewVideoCategoryTitle(item.title)
}

private fun expressionGalleryOrder(title: String, fallback: Int): Int {
    val normalized = normalizeGalleryTitle(title)
    if (normalized == "角色表情") return 1
    return Regex("""角色表情(\d+)""")
        .find(normalized)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: fallback
}
