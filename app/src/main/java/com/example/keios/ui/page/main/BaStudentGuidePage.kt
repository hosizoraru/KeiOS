package com.example.keios.ui.page.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.example.keios.ui.page.main.student.hasRenderableGalleryMedia
import com.example.keios.ui.page.main.student.isMemoryHallFileGalleryItem
import com.example.keios.ui.page.main.student.isRenderableGalleryImageUrl
import com.example.keios.ui.page.main.student.isRenderableGalleryVideoUrl
import com.example.keios.ui.page.main.student.normalizeGuideUrl
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
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
fun BaStudentGuidePage(
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
    val backdrop: LayerBackdrop = key(activationCount) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
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
            .background(MiuixTheme.colorScheme.background)
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (loading) {
                        val progress = rememberGuideSyncProgress()
                        val foregroundColor = guideSyncProgressColor(progress)
                        CircularProgressIndicator(
                            progress = progress,
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
                        backdrop = backdrop,
                        title = "未选择学生",
                        subtitle = "请从 BA 卡池信息中点击对应卡池进入",
                        accent = accent
                    )
                }
            } else {
                renderBaStudentGuideTabContent(
                    activeBottomTab = activeBottomTab,
                    info = info,
                    error = error,
                    backdrop = backdrop,
                    accent = accent,
                    context = context,
                    sourceUrl = sourceUrl,
                    galleryCacheRevision = galleryCacheRevision,
                    playingVoiceUrl = playingVoiceUrl,
                    isVoicePlaying = isVoicePlaying,
                    voicePlayProgress = voicePlayProgress,
                    onOpenExternal = ::openExternal,
                    onToggleVoicePlayback = ::toggleVoicePlayback
                )
            }
        }
    }
}

@Composable
private fun rememberGuideSyncProgress(): Float {
    val transition = rememberInfiniteTransition(label = "guideSyncProgress")
    val progress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "guideSyncProgressValue",
    )
    return progress
}

private fun guideSyncProgressColor(progress: Float): Color {
    val value = progress.coerceIn(0f, 1f)
    return when {
        value < 0.35f -> Color(0xFF3B82F6)
        value < 0.70f -> Color(0xFF06B6D4)
        value < 0.92f -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }
}
