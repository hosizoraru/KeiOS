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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.keios.ui.page.main.ba.BASettingsStore
import com.example.keios.ui.page.main.student.BaStudentGuideInfo
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
import com.example.keios.ui.page.main.student.hasRenderableGalleryMedia
import com.example.keios.ui.page.main.student.isMemoryHallFileGalleryItem
import com.example.keios.ui.page.main.student.isInteractiveFurnitureGalleryItem
import com.example.keios.ui.page.main.student.isRenderableGalleryAudioUrl
import com.example.keios.ui.page.main.student.isRenderableGalleryImageUrl
import com.example.keios.ui.page.main.student.isRenderableGalleryVideoUrl
import com.example.keios.ui.page.main.student.normalizeGuideUrl
import com.example.keios.ui.page.main.student.normalizeGalleryTitle
import com.example.keios.ui.page.main.student.profileRowsForDisplay
import com.example.keios.ui.page.main.student.renderBaStudentGuideTabContent
import com.example.keios.ui.page.main.student.shouldHideMovedHeaderRow
import com.example.keios.ui.page.main.student.skillCardsForDisplay
import com.example.keios.ui.page.main.student.weaponCardForDisplay
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.core.prefs.UiPrefs
import com.rosan.installer.ui.library.effect.getMiuixAppBarColor
import com.rosan.installer.ui.library.effect.rememberMiuixBlurBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

private fun normalizeGuidePlaybackSource(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val scheme = runCatching { Uri.parse(value).scheme.orEmpty() }.getOrDefault("")
    return if (scheme.equals("file", ignoreCase = true)) {
        value
    } else {
        normalizeGuideUrl(value)
    }
}

private fun isGuideAudioPlaybackUrl(raw: String): Boolean {
    val normalized = normalizeGuidePlaybackSource(raw)
    if (normalized.isBlank()) return false
    val scheme = runCatching { Uri.parse(normalized).scheme.orEmpty() }.getOrDefault("")
    return scheme.equals("http", ignoreCase = true) ||
        scheme.equals("https", ignoreCase = true) ||
        scheme.equals("file", ignoreCase = true)
}

@Composable
fun BaStudentGuidePage(
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surface
    // Keep backdrop allocation stable per page lifecycle to avoid RenderThread native crashes
    // when rapidly switching guide tabs on some HyperOS builds.
    var activationCount by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        activationCount++
        onDispose { }
    }
    // Keep top-level backdrop only for navigator/pager layer and bottom bar.
    val navBackdrop: LayerBackdrop = key("nav-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    // Top action bar uses its own backdrop instance to avoid cross-layer recursion.
    val topBarBackdrop: LayerBackdrop = key("topbar-$activationCount") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val scrollBehavior = MiuixScrollBehavior()

    var sourceUrl by rememberSaveable { mutableStateOf(BaStudentGuideStore.loadCurrentUrl()) }
    var info by remember(sourceUrl) { mutableStateOf<BaStudentGuideInfo?>(null) }
    var loading by remember(sourceUrl) { mutableStateOf(sourceUrl.isNotBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }
    var manualRefreshRequested by remember { mutableStateOf(false) }
    var selectedBottomTabIndex by rememberSaveable(sourceUrl) { mutableIntStateOf(0) }
    var selectedVoiceLanguage by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var playingVoiceUrl by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var isVoicePlaying by remember(sourceUrl) { mutableStateOf(false) }
    var voicePlayProgress by remember(sourceUrl) { mutableFloatStateOf(0f) }
    var galleryPrefetchRequested by rememberSaveable(sourceUrl) { mutableStateOf(false) }
    var reloadingInteractiveFurnitureGifUrl by remember(sourceUrl) { mutableStateOf("") }
    var reloadingInteractiveFurnitureGifProgress by remember(sourceUrl) { mutableFloatStateOf(0f) }
    var galleryCacheRevision by remember(sourceUrl) { mutableIntStateOf(0) }
    val bottomTabs = GuideBottomTab.entries
    val pagerState = rememberPagerState(
        initialPage = selectedBottomTabIndex,
        pageCount = { bottomTabs.size }
    )
    val activeBottomTab = bottomTabs.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }
    val pageScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }
    val syncProgress = rememberGuideSyncProgress(loading = loading)
    val ignoreStringInput: (String) -> Unit = remember { { _: String -> } }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liquidBottomBarEnabled = remember { UiPrefs.isLiquidBottomBarEnabled() }
    var showBottomBar by remember { mutableStateOf(true) }
    val farJumpAlpha = remember { Animatable(1f) }
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

    fun openGuideInPage(rawUrl: String) {
        val target = normalizeGuideUrl(rawUrl)
        if (target.isBlank()) return
        if (target == sourceUrl) return
        manualRefreshRequested = false
        BaStudentGuideStore.setCurrentUrl(target)
        sourceUrl = target
        error = null
        refreshSignal += 1
    }

    LaunchedEffect(Unit) {
        val latestStored = BaStudentGuideStore.loadCurrentUrl()
        if (latestStored.isNotBlank() && latestStored != sourceUrl) {
            sourceUrl = latestStored
            error = null
        }
    }

    fun toggleVoicePlayback(rawAudioUrl: String) {
        val target = normalizeGuidePlaybackSource(rawAudioUrl)
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

    fun reloadInteractiveFurnitureGif(rawUrl: String) {
        val target = normalizeGuideUrl(rawUrl)
        if (target.isBlank()) return
        if (reloadingInteractiveFurnitureGifUrl == target) return
        val currentSource = sourceUrl
        reloadingInteractiveFurnitureGifUrl = target
        reloadingInteractiveFurnitureGifProgress = 0.12f
        pageScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    BaGuideTempMediaCache.clearMediaCache(
                        context = context,
                        sourceUrl = currentSource,
                        rawUrl = target
                    )
                }
                reloadingInteractiveFurnitureGifProgress = 0.34f
                withContext(Dispatchers.IO) {
                    BaGuideTempMediaCache.prefetchForGuide(
                        context = context,
                        sourceUrl = currentSource,
                        rawUrls = listOf(target),
                        forceReDownload = true
                    )
                }
                reloadingInteractiveFurnitureGifProgress = 1f
                delay(80)
                galleryCacheRevision += 1
            } finally {
                reloadingInteractiveFurnitureGifProgress = 0f
                if (reloadingInteractiveFurnitureGifUrl == target) {
                    reloadingInteractiveFurnitureGifUrl = ""
                }
            }
        }
    }

    fun selectBottomTab(index: Int) {
        if (index !in bottomTabs.indices) return
        val fromIndex = if (pagerState.isScrollInProgress) {
            pagerState.targetPage
        } else {
            pagerState.settledPage
        }
        if (index == fromIndex && !pagerState.isScrollInProgress) return
        showBottomBar = true
        selectedBottomTabIndex = index
        tabJumpJob?.cancel()
        tabJumpJob = pageScope.launch {
            pagerState.animateTabSwitch(
                fromIndex = fromIndex,
                targetIndex = index,
                onFarJumpBefore = {
                    farJumpAlpha.snapTo(1f)
                    farJumpAlpha.animateTo(
                        targetValue = 0.92f,
                        animationSpec = tween(durationMillis = 70)
                    )
                },
                onFarJumpAfter = {
                    farJumpAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 120)
                    )
                }
            )
        }
    }

    LaunchedEffect(sourceUrl, bottomTabs.size) {
        val targetIndex = selectedBottomTabIndex.coerceIn(0, bottomTabs.lastIndex)
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (selectedBottomTabIndex != pagerState.settledPage) {
            selectedBottomTabIndex = pagerState.settledPage
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
            .filter {
                it.isNotBlank() && (
                    isRenderableGalleryImageUrl(it) ||
                        isRenderableGalleryVideoUrl(it) ||
                        isRenderableGalleryAudioUrl(it)
                    )
            }
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

    // 预热语音资源，避免语音台词首次点播时重复走远端请求。
    LaunchedEffect(sourceUrl, info?.syncedAtMs) {
        val guide = info ?: return@LaunchedEffect
        val voiceAudioUrls = guide.voiceEntries
            .asSequence()
            .flatMap { entry ->
                sequenceOf(entry.audioUrl) + entry.audioUrls.asSequence()
            }
            .map(::normalizeGuidePlaybackSource)
            .filter(::isGuideAudioPlaybackUrl)
            .distinct()
            .toList()
        if (voiceAudioUrls.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            BaGuideTempMediaCache.prefetchForGuide(
                context = context,
                sourceUrl = sourceUrl,
                rawUrls = voiceAudioUrls
            )
        }
        galleryCacheRevision += 1
    }

    // 优先预热“互动家具1 2”GIF，减少进入学生档案时首次加载等待与失败率。
    LaunchedEffect(sourceUrl, info?.syncedAtMs) {
        val guide = info ?: return@LaunchedEffect
        val furnitureGifUrls = guide.galleryItems
            .asSequence()
            .filter(::isInteractiveFurniture12GalleryItemForPrefetch)
            .map { item -> item.mediaUrl.ifBlank { item.imageUrl } }
            .map(::normalizeGuideUrl)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        if (furnitureGifUrls.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            BaGuideTempMediaCache.prefetchForGuide(
                context = context,
                sourceUrl = sourceUrl,
                rawUrls = furnitureGifUrls
            )
        }
        galleryCacheRevision += 1
    }

    LaunchedEffect(sourceUrl, refreshSignal) {
        val requestUrl = sourceUrl
        if (requestUrl.isBlank()) return@LaunchedEffect
        val manualRefresh = manualRefreshRequested
        manualRefreshRequested = false
        loading = true

        val now = System.currentTimeMillis()
        val refreshIntervalHours = withContext(Dispatchers.IO) {
            BASettingsStore.loadCalendarRefreshIntervalHours()
        }
        val cacheSnapshot = withContext(Dispatchers.IO) {
            BaStudentGuideStore.loadInfoSnapshot(requestUrl)
        }
        if (requestUrl != sourceUrl) return@LaunchedEffect

        val cacheExpired = BaStudentGuideStore.isCacheExpired(
            snapshot = cacheSnapshot,
            refreshIntervalHours = refreshIntervalHours,
            nowMs = now
        )
        val cacheComplete = cacheSnapshot.isComplete && cacheSnapshot.info != null
        if (!manualRefresh && cacheComplete && !cacheExpired) {
            info = cacheSnapshot.info
            error = null
            loading = false
            return@LaunchedEffect
        }

        if (cacheComplete) {
            info = cacheSnapshot.info
            error = null
        } else if (cacheSnapshot.hasCache && !cacheSnapshot.isComplete) {
            info = null
        }

        val shouldClearLocalCache = manualRefresh || (cacheSnapshot.hasCache && (cacheExpired || !cacheSnapshot.isComplete))
        if (shouldClearLocalCache) {
            withContext(Dispatchers.IO) {
                BaStudentGuideStore.clearCachedInfo(requestUrl)
                BaGuideTempMediaCache.clearGuideCache(context, requestUrl)
            }
            if (requestUrl != sourceUrl) return@LaunchedEffect
        }

        val result = withContext(Dispatchers.IO) {
            runCatching { fetchGuideInfo(requestUrl) }
        }
        if (requestUrl != sourceUrl) return@LaunchedEffect
        result.onSuccess { latest ->
            if (requestUrl != sourceUrl) return@onSuccess
            info = latest
            error = null
            withContext(Dispatchers.IO) { BaStudentGuideStore.saveInfo(latest) }
        }.onFailure {
            if (requestUrl != sourceUrl) return@onFailure
            error = if (info != null) "网络请求失败，已显示本地缓存" else "图鉴信息加载失败"
        }
        if (requestUrl != sourceUrl) return@LaunchedEffect
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
                    Box {
                            LiquidActionBar(
                                backdrop = topBarBackdrop,
                                layeredStyleEnabled = liquidActionBarLayeredStyleEnabled,
                                items = listOf(
                                    LiquidActionItem(
                                        icon = MiuixIcons.Regular.Share,
                                        contentDescription = "分享来源",
                                    onClick = ::shareSource
                                ),
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = "刷新",
                                    onClick = {
                                        manualRefreshRequested = true
                                        refreshSignal += 1
                                    }
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
                        selectedIndex = { pagerState.targetPage },
                        onSelected = { index ->
                            if (index != pagerState.targetPage) {
                                selectBottomTab(index)
                            }
                        },
                        backdrop = navBackdrop,
                        tabsCount = bottomTabs.size,
                        isBlurEnabled = liquidBottomBarEnabled
                    ) {
                        bottomTabs.forEachIndexed { index, tab ->
                            FloatingBottomBarItem(
                                onClick = { selectBottomTab(index) },
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
        HorizontalPager(
            state = pagerState,
            key = { index -> bottomTabs[index].name },
            overscrollEffect = null,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = farJumpAlpha.value }
                .layerBackdrop(navBackdrop)
        ) { pageIndex ->
            val pageBottomTab = bottomTabs.getOrElse(pageIndex) { GuideBottomTab.Archive }
            val isVoiceTab = pageBottomTab == GuideBottomTab.Voice
            val isGalleryTab = pageBottomTab == GuideBottomTab.Gallery
            val pageListState = rememberSaveable(
                sourceUrl,
                pageBottomTab.name,
                saver = LazyListState.Saver
            ) {
                LazyListState()
            }
            // Each guide page owns an isolated backdrop instance.
            val pageBackdrop: LayerBackdrop = key("page-$activationCount-$sourceUrl-$pageIndex") {
                rememberLayerBackdrop {
                    drawRect(surfaceColor)
                    drawContent()
                }
            }
            LazyColumn(
                state = pageListState,
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
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SmallTitle(pageBottomTab.label)
                        }
                        if (sourceUrl.isNotBlank()) {
                            val foregroundColor = when {
                                loading -> Color(0xFF3B82F6)
                                !error.isNullOrBlank() -> Color(0xFFEF4444)
                                else -> Color(0xFF22C55E)
                            }
                            CircularProgressIndicator(
                                progress = syncProgress,
                                size = 18.dp,
                                strokeWidth = 2.dp,
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = foregroundColor,
                                    backgroundColor = foregroundColor.copy(alpha = 0.30f),
                                ),
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }

                if (sourceUrl.isBlank()) {
                    item {
                        FrostedBlock(
                            backdrop = pageBackdrop,
                            title = "未选择学生",
                            subtitle = "请从 BA 卡池信息中点击对应卡池进入",
                            accent = accent
                        )
                    }
                } else {
                    renderBaStudentGuideTabContent(
                        activeBottomTab = pageBottomTab,
                        info = info,
                        error = error,
                        backdrop = pageBackdrop,
                        accent = accent,
                        context = context,
                        sourceUrl = sourceUrl,
                        galleryCacheRevision = galleryCacheRevision,
                        playingVoiceUrl = if (isVoiceTab) playingVoiceUrl else "",
                        isVoicePlaying = isVoiceTab && isVoicePlaying,
                        voicePlayProgress = if (isVoiceTab) voicePlayProgress else 0f,
                        selectedVoiceLanguage = if (isVoiceTab) selectedVoiceLanguage else "",
                        interactiveFurnitureGifReloadingUrl = if (isGalleryTab) {
                            reloadingInteractiveFurnitureGifUrl
                        } else {
                            ""
                        },
                        interactiveFurnitureGifReloadingProgress = if (isGalleryTab) {
                            reloadingInteractiveFurnitureGifProgress
                        } else {
                            0f
                        },
                        onOpenExternal = ::openExternal,
                        onOpenGuide = ::openGuideInPage,
                        onToggleVoicePlayback = if (isVoiceTab) ::toggleVoicePlayback else ignoreStringInput,
                        onSelectedVoiceLanguageChange = if (isVoiceTab) {
                            { selectedVoiceLanguage = it }
                        } else {
                            ignoreStringInput
                        },
                        onReloadInteractiveFurnitureGif = if (isGalleryTab) {
                            ::reloadInteractiveFurnitureGif
                        } else {
                            ignoreStringInput
                        }
                    )
                }
            }
        }
    }
}

private fun isInteractiveFurniture12GalleryItemForPrefetch(item: BaGuideGalleryItem): Boolean {
    if (!isInteractiveFurnitureGalleryItem(item)) return false
    val title = normalizeGalleryTitle(item.title)
    if (title.contains("互动家具12")) return true
    val digits = title.filter(Char::isDigit)
    return digits == "12"
}

@Composable
private fun rememberGuideSyncProgress(loading: Boolean): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(loading) {
        if (loading) {
            // Restart from a visible low point and move forward once, no looping.
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
