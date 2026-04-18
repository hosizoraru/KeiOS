package com.example.keios.ui.page.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.keios.R
import com.example.keios.feature.ba.data.remote.GameKeeFetchHelper
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
import com.example.keios.ui.page.main.student.isRenderableGalleryStaticImageUrl
import com.example.keios.ui.page.main.student.normalizeGuideUrl
import com.example.keios.ui.page.main.student.extractGuideContentIdFromUrl
import com.example.keios.ui.page.main.student.profileRowsForDisplay
import com.example.keios.ui.page.main.student.renderBaStudentGuideTabContent
import com.example.keios.ui.page.main.student.shouldHideMovedHeaderRow
import com.example.keios.ui.page.main.student.skillCardsForDisplay
import com.example.keios.ui.page.main.student.weaponCardForDisplay
import com.example.keios.ui.page.main.student.clearGuideBgmLoopScope
import com.example.keios.ui.page.main.student.clearGuideBgmPlaybackScope
import com.example.keios.ui.perf.ReportPagerPerformanceState
import com.example.keios.ui.page.main.widget.AppMotionTokens
import com.example.keios.ui.page.main.widget.UiPerformanceBudget
import com.example.keios.ui.page.main.widget.FloatingBottomBar
import com.example.keios.ui.page.main.widget.FloatingBottomBarItem
import com.example.keios.ui.page.main.widget.FrostedBlock
import com.example.keios.ui.page.main.widget.LiquidActionBar
import com.example.keios.ui.page.main.widget.LiquidActionItem
import com.example.keios.ui.page.main.widget.LocalTransitionAnimationsEnabled
import com.example.keios.ui.page.main.widget.MiuixInfoItem
import com.example.keios.ui.page.main.widget.appFloatingEnter
import com.example.keios.ui.page.main.widget.appFloatingExit
import com.example.keios.core.prefs.UiPrefs
import com.example.keios.ui.page.main.widget.resolvedMotionDuration
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
import java.io.File
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

private fun collectGuideStaticImagePrefetchUrls(info: BaStudentGuideInfo): List<String> {
    val orderedUrls = mutableListOf<String>()
    val coverUrl = normalizeGuideUrl(info.imageUrl)
    if (isRenderableGalleryStaticImageUrl(coverUrl)) {
        orderedUrls += coverUrl
    }

    val galleryItems = if (info.galleryItems.isNotEmpty()) {
        info.galleryItems
            .filter(::hasRenderableGalleryMedia)
            .filterNot(::isMemoryHallFileGalleryItem)
            .distinctBy { "${it.mediaType}|${it.mediaUrl.ifBlank { it.imageUrl }}" }
    } else {
        listOfNotNull(
            info.imageUrl.takeIf { it.isNotBlank() }?.let {
                BaGuideGalleryItem(
                    title = "立绘",
                    imageUrl = it,
                    mediaType = "image",
                    mediaUrl = it
                )
            }
        )
    }

    galleryItems.forEach { item ->
        val imageUrl = normalizeGuideUrl(item.imageUrl)
        if (isRenderableGalleryStaticImageUrl(imageUrl)) {
            orderedUrls += imageUrl
        }
        val mediaUrl = normalizeGuideUrl(item.mediaUrl)
        if (isRenderableGalleryStaticImageUrl(mediaUrl)) {
            orderedUrls += mediaUrl
        }
    }
    return orderedUrls
        .filter { it.isNotBlank() }
        .distinct()
}

private data class GuideMediaSaveRequest(
    val sourceUrl: String,
    val title: String,
    val fileName: String,
    val mimeType: String
)

private fun sanitizeGuideMediaTitle(raw: String): String {
    val cleaned = raw
        .replace(Regex("""[\\/:*?"<>|]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    return cleaned.ifBlank { "BA_media" }.take(96)
}

private fun sanitizeGuideMediaToken(raw: String): String {
    return raw
        .replace(Regex("""[\\/:*?"<>|]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(48)
}

private val guideKnownMediaExts = setOf(
    "jpg", "jpeg", "png", "webp", "gif", "bmp",
    "mp4", "webm", "mkv", "mov", "m3u8",
    "ogg", "mp3", "wav", "flac", "aac", "m4a"
)

private fun readGuideFileHeader(path: String, maxBytes: Int = 16): ByteArray {
    if (path.isBlank()) return ByteArray(0)
    val file = File(path)
    if (!file.exists() || file.length() <= 0L) return ByteArray(0)
    return runCatching {
        file.inputStream().use { input ->
            val buffer = ByteArray(maxBytes)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }
    }.getOrDefault(ByteArray(0))
}

private fun ByteArray.startsWithAscii(prefix: String): Boolean {
    if (size < prefix.length) return false
    return prefix.indices.all { index ->
        this[index].toInt().toChar() == prefix[index]
    }
}

private fun ByteArray.asciiAt(offset: Int, length: Int): String {
    if (offset < 0 || length <= 0 || size < offset + length) return ""
    return buildString(length) {
        for (i in offset until offset + length) {
            append(this@asciiAt[i].toInt().toChar())
        }
    }
}

private fun inferGuideMediaExtFromLocalFile(rawSourceUrl: String, rawTitle: String): String? {
    val parsed = runCatching { Uri.parse(rawSourceUrl) }.getOrNull()
    val path = when {
        parsed?.scheme.equals("file", ignoreCase = true) -> parsed?.path.orEmpty()
        rawSourceUrl.startsWith("/") -> rawSourceUrl
        else -> ""
    }
    val header = readGuideFileHeader(path = path, maxBytes = 16)
    if (header.isEmpty()) return null

    fun byteAt(index: Int): Int = header.getOrNull(index)?.toInt()?.and(0xFF) ?: -1

    return when {
        header.size >= 4 && header.startsWithAscii("OggS") -> "ogg"
        header.size >= 3 && header.startsWithAscii("ID3") -> "mp3"
        header.size >= 2 && byteAt(0) == 0xFF && (byteAt(1) and 0xE0) == 0xE0 -> "mp3"
        header.size >= 4 && header.startsWithAscii("fLaC") -> "flac"
        header.size >= 12 &&
            header.startsWithAscii("RIFF") &&
            header.asciiAt(8, 4) == "WAVE" -> "wav"
        header.size >= 2 && byteAt(0) == 0xFF && (byteAt(1) and 0xF6) == 0xF0 -> "aac"
        header.size >= 12 && header.asciiAt(4, 4) == "ftyp" -> {
            val lowerTitle = rawTitle.lowercase()
            if (lowerTitle.contains("bgm") || lowerTitle.contains("音频") || lowerTitle.contains("语音")) {
                "m4a"
            } else {
                "mp4"
            }
        }
        header.size >= 4 && byteAt(0) == 0x89 && header.asciiAt(1, 3) == "PNG" -> "png"
        header.size >= 3 && byteAt(0) == 0xFF && byteAt(1) == 0xD8 && byteAt(2) == 0xFF -> "jpg"
        header.size >= 6 && (header.startsWithAscii("GIF87a") || header.startsWithAscii("GIF89a")) -> "gif"
        header.size >= 12 &&
            header.startsWithAscii("RIFF") &&
            header.asciiAt(8, 4) == "WEBP" -> "webp"
        header.size >= 2 && header.startsWithAscii("BM") -> "bmp"
        else -> null
    }
}

private fun guessGuideMediaExt(rawSourceUrl: String, rawTitle: String = ""): String {
    val normalized = normalizeGuidePlaybackSource(rawSourceUrl)
    val source = normalized.substringBefore('#').substringBefore('?')
    val ext = source.substringAfterLast('.', "").lowercase()
    if (ext in guideKnownMediaExts) return ext

    inferGuideMediaExtFromLocalFile(normalized, rawTitle)?.let { inferred ->
        return inferred
    }

    val lowerSource = normalized.lowercase()
    val lowerTitle = rawTitle.lowercase()
    return when {
        lowerSource.contains("audio") || lowerTitle.contains("bgm") || lowerTitle.contains("音频") -> "ogg"
        lowerSource.contains("video") || lowerTitle.contains("视频") -> "mp4"
        else -> "bin"
    }
}

private fun mimeTypeFromGuideExt(ext: String): String {
    val normalizedExt = ext.lowercase()
    val mapMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(normalizedExt).orEmpty()
    if (mapMime.isNotBlank()) return mapMime
    return when (normalizedExt) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "ogg" -> "audio/ogg"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        else -> "application/octet-stream"
    }
}

private fun buildGuideMediaSaveRequest(
    rawUrl: String,
    rawTitle: String,
    rawPrefix: String = ""
): GuideMediaSaveRequest? {
    val source = normalizeGuidePlaybackSource(rawUrl)
    if (source.isBlank()) return null
    val ext = guessGuideMediaExt(source, rawTitle)
    val mime = mimeTypeFromGuideExt(ext)
    val baseTitle = sanitizeGuideMediaTitle(rawTitle)
    val prefix = sanitizeGuideMediaToken(rawPrefix)
        .takeIf { it.isNotBlank() && it != "学生图鉴" }
        .orEmpty()
    val title = when {
        prefix.isBlank() -> baseTitle
        baseTitle.startsWith(prefix) -> baseTitle
        else -> sanitizeGuideMediaTitle("${prefix}_${baseTitle}")
    }
    val fileName = if (title.endsWith(".$ext", ignoreCase = true)) {
        title
    } else {
        "$title.$ext"
    }
    return GuideMediaSaveRequest(
        sourceUrl = source,
        title = title,
        fileName = fileName,
        mimeType = mime
    )
}

private fun copyGuideMediaToUri(
    context: android.content.Context,
    sourceUrl: String,
    outputUri: Uri
): Boolean {
    return runCatching {
        val normalized = normalizeGuidePlaybackSource(sourceUrl)
        if (normalized.isBlank()) {
            false
        } else {
            val output = context.contentResolver.openOutputStream(outputUri) ?: return@runCatching false
            output.use { out ->
                val sourceUri = runCatching { Uri.parse(normalized) }.getOrNull()
                val scheme = sourceUri?.scheme.orEmpty().lowercase()
                when {
                    scheme == "file" -> {
                        val path = sourceUri?.path.orEmpty()
                        if (path.isBlank()) {
                            false
                        } else {
                            File(path).inputStream().use { input -> input.copyTo(out) }
                            true
                        }
                    }
                    scheme == "content" -> {
                        val parsedUri = sourceUri
                        val input = parsedUri?.let { context.contentResolver.openInputStream(it) }
                        if (input == null) {
                            false
                        } else {
                            input.use { stream -> stream.copyTo(out) }
                            true
                        }
                    }
                    scheme == "http" || scheme == "https" || normalized.startsWith("//") -> {
                        val tempDir = File(context.cacheDir, "guide_media_save")
                        if (!tempDir.exists()) tempDir.mkdirs()
                        val tempFile = File(tempDir, "tmp_${System.nanoTime().toString(16)}")
                        try {
                            val downloaded = GameKeeFetchHelper.downloadToFile(normalized, tempFile)
                            if (!downloaded || !tempFile.exists() || tempFile.length() <= 0L) {
                                false
                            } else {
                                tempFile.inputStream().use { input -> input.copyTo(out) }
                                true
                            }
                        } finally {
                            runCatching { tempFile.delete() }
                        }
                    }
                    else -> false
                }
            }
        }
    }.getOrDefault(false)
}

private fun createUniqueDocumentInTree(
    tree: DocumentFile,
    mimeType: String,
    fileName: String
): DocumentFile? {
    val base = fileName.substringBeforeLast('.', fileName).ifBlank { "BA_media" }
    val ext = fileName.substringAfterLast('.', "")
    val normalizedExt = ext.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
    val maxAttempts = 120
    for (index in 0 until maxAttempts) {
        val candidate = if (index == 0) {
            "$base$normalizedExt"
        } else {
            "$base ($index)$normalizedExt"
        }
        val exists = tree.findFile(candidate)
        if (exists == null) {
            return tree.createFile(mimeType, candidate)
        }
    }
    return null
}

@Composable
fun BaStudentGuidePage(
    liquidActionBarLayeredStyleEnabled: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val defaultPageTitle = stringResource(R.string.guide_page_title_default)
    val shareSourceEmptyText = stringResource(R.string.guide_share_source_empty)
    val shareSourceChooserTitle = stringResource(R.string.guide_share_source_chooser_title)
    val shareSourceFailedText = stringResource(R.string.common_share_failed)
    val openLinkFailedText = stringResource(R.string.common_open_link_failed)
    val shareSourceContentDescription = stringResource(R.string.guide_cd_share_source)
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
    val guideSyncToken = info?.syncedAtMs ?: -1L
    var loading by remember(sourceUrl) { mutableStateOf(sourceUrl.isNotBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshSignal by remember { mutableStateOf(0) }
    var manualRefreshRequested by remember { mutableStateOf(false) }
    var selectedBottomTabIndex by rememberSaveable(sourceUrl) { mutableIntStateOf(0) }
    var selectedVoiceLanguage by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var playingVoiceUrl by rememberSaveable(sourceUrl) { mutableStateOf("") }
    var isVoicePlaying by remember(sourceUrl) { mutableStateOf(false) }
    var voicePlayProgress by remember(sourceUrl) { mutableFloatStateOf(0f) }
    var pendingCustomSaveRequest by remember { mutableStateOf<GuideMediaSaveRequest?>(null) }
    var pendingFixedSaveRequest by remember { mutableStateOf<GuideMediaSaveRequest?>(null) }
    var galleryPrefetchRequested by rememberSaveable(sourceUrl, guideSyncToken) { mutableStateOf(false) }
    var staticImagePrefetchStage by rememberSaveable(sourceUrl, guideSyncToken) { mutableIntStateOf(0) }
    var galleryCacheRevision by remember(sourceUrl) { mutableIntStateOf(0) }
    val bottomTabs = GuideBottomTab.entries
    val pagerState = rememberPagerState(
        initialPage = selectedBottomTabIndex,
        pageCount = { bottomTabs.size }
    )
    ReportPagerPerformanceState(
        scope = "guide_detail_pager",
        currentPage = bottomTabs.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }.name,
        targetPage = bottomTabs.getOrElse(pagerState.targetPage) { GuideBottomTab.Archive }.name,
        scrolling = pagerState.isScrollInProgress
    )
    val activeBottomTab = bottomTabs.getOrElse(pagerState.currentPage) { GuideBottomTab.Archive }
    val pageScope = rememberCoroutineScope()
    var tabJumpJob by remember { mutableStateOf<Job?>(null) }
    val syncProgress = rememberGuideSyncProgress(
        loading = loading,
        animationsEnabled = transitionAnimationsEnabled
    )
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
    val pageTitle = info?.title?.ifBlank { defaultPageTitle } ?: defaultPageTitle
    val voicePlayer = remember(context, sourceUrl) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
            .build()
    }
    val customSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = pendingCustomSaveRequest
        pendingCustomSaveRequest = null
        val targetUri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || request == null || targetUri == null) {
            return@rememberLauncherForActivityResult
        }
        pageScope.launch {
            val success = withContext(Dispatchers.IO) {
                copyGuideMediaToUri(context, request.sourceUrl, targetUri)
            }
            if (success) {
                Toast.makeText(
                    context,
                    context.getString(R.string.guide_media_save_success, request.fileName),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(context, context.getString(R.string.guide_media_save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val fixedFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = pendingFixedSaveRequest
        if (result.resultCode != Activity.RESULT_OK) {
            pendingFixedSaveRequest = null
            return@rememberLauncherForActivityResult
        }
        val treeUri = result.data?.data
        if (request == null || treeUri == null) {
            pendingFixedSaveRequest = null
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        BASettingsStore.saveMediaSaveFixedTreeUri(treeUri.toString())
        pendingFixedSaveRequest = null
        pageScope.launch {
            val success = withContext(Dispatchers.IO) {
                val treeDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
                val targetDoc = createUniqueDocumentInTree(
                    tree = treeDoc,
                    mimeType = request.mimeType,
                    fileName = request.fileName
                ) ?: return@withContext false
                copyGuideMediaToUri(context, request.sourceUrl, targetDoc.uri)
            }
            if (success) {
                Toast.makeText(
                    context,
                    context.getString(R.string.guide_media_save_success, request.fileName),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(context, context.getString(R.string.guide_media_save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(voicePlayer) {
        onDispose {
            runCatching { voicePlayer.release() }
        }
    }

    DisposableEffect(sourceUrl) {
        onDispose {
            clearGuideBgmLoopScope(sourceUrl)
            clearGuideBgmPlaybackScope(sourceUrl)
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
                Toast.makeText(
                    context,
                    context.getString(R.string.guide_toast_voice_play_failed_with_reason, error.errorCodeName),
                    Toast.LENGTH_SHORT
                ).show()
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
            Toast.makeText(context, shareSourceEmptyText, Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, target)
            }
            val chooser = Intent.createChooser(intent, shareSourceChooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure {
            Toast.makeText(context, shareSourceFailedText, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, openLinkFailedText, Toast.LENGTH_SHORT).show()
        }
    }

    fun openGuideInPage(rawUrl: String) {
        val normalized = normalizeGuideUrl(rawUrl)
        val contentId = extractGuideContentIdFromUrl(normalized)
        val target = if (contentId != null && contentId > 0L) {
            "https://www.gamekee.com/ba/tj/$contentId.html"
        } else {
            normalized
        }
        if (target.isBlank()) return
        if (target == sourceUrl) return
        manualRefreshRequested = false
        BaStudentGuideStore.setCurrentUrl(target)
        sourceUrl = target
        error = null
        refreshSignal += 1
    }

    fun saveGuideMedia(rawMediaUrl: String, rawTitle: String) {
        val studentNamePrefix = info?.title?.trim().orEmpty()
        val request = buildGuideMediaSaveRequest(
            rawUrl = rawMediaUrl,
            rawTitle = rawTitle,
            rawPrefix = studentNamePrefix
        )
        if (request == null) {
            Toast.makeText(context, context.getString(R.string.guide_media_save_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val useFixedSaveLocation = BASettingsStore.loadMediaSaveCustomEnabled()
        if (!useFixedSaveLocation) {
            pendingCustomSaveRequest = request
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = request.mimeType
                putExtra(Intent.EXTRA_TITLE, request.fileName)
            }
            customSaveLauncher.launch(intent)
            return
        }

        val fixedTreeUriRaw = BASettingsStore.loadMediaSaveFixedTreeUri()
        val fixedTreeUri = fixedTreeUriRaw.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()
        }
        if (fixedTreeUri != null) {
            pageScope.launch {
                val success = withContext(Dispatchers.IO) {
                    val treeDoc = DocumentFile.fromTreeUri(context, fixedTreeUri)
                        ?: return@withContext false
                    val targetDoc = createUniqueDocumentInTree(
                        tree = treeDoc,
                        mimeType = request.mimeType,
                        fileName = request.fileName
                    ) ?: return@withContext false
                    copyGuideMediaToUri(context, request.sourceUrl, targetDoc.uri)
                }
                if (success) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.guide_media_save_success, request.fileName),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    BASettingsStore.saveMediaSaveFixedTreeUri("")
                    pendingFixedSaveRequest = request
                    val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                    }
                    fixedFolderLauncher.launch(pickerIntent)
                }
            }
            return
        }

        pendingFixedSaveRequest = request
        val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload"))
            }
        }
        fixedFolderLauncher.launch(pickerIntent)
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
                animationsEnabled = transitionAnimationsEnabled,
                onFarJumpBefore = {
                    // Keep far jump immediate: dim instantly, jump, then fade in.
                    // This avoids lingering on the previous tab for an extra frame.
                    farJumpAlpha.snapTo(0.94f)
                },
                onFarJumpAfter = {
                    farJumpAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = resolvedMotionDuration(
                                AppMotionTokens.farJumpRestoreEmphasisMs,
                                transitionAnimationsEnabled
                            ),
                            easing = if (transitionAnimationsEnabled) FastOutSlowInEasing else LinearEasing
                        )
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

    val voiceTabActive = activeBottomTab == GuideBottomTab.Voice
    val voicePlayingForProgress = if (voiceTabActive) isVoicePlaying else false
    val voiceUrlForProgress = if (voiceTabActive) playingVoiceUrl else ""
    LaunchedEffect(voiceTabActive, voicePlayingForProgress, voiceUrlForProgress) {
        if (!voiceTabActive || !voicePlayingForProgress || voiceUrlForProgress.isBlank()) {
            voicePlayProgress = 0f
            return@LaunchedEffect
        }
        while (voiceTabActive && voicePlayingForProgress && voiceUrlForProgress.isNotBlank()) {
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

    // 阶段1：进入学生图鉴后仅预加载少量静态图片，保证首屏顺滑并控制流量。
    LaunchedEffect(sourceUrl, info?.syncedAtMs) {
        if (staticImagePrefetchStage >= 1) return@LaunchedEffect
        val guide = info ?: return@LaunchedEffect
        staticImagePrefetchStage = 1

        val urls = collectGuideStaticImagePrefetchUrls(guide)
            .take(UiPerformanceBudget.guideStaticPrefetchInitialCount)
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

    // 阶段2：用户进入影画板块后，再补充一批静态图片；GIF/视频/音频保持按需加载。
    LaunchedEffect(sourceUrl, galleryPrefetchRequested, info?.syncedAtMs) {
        if (!galleryPrefetchRequested) return@LaunchedEffect
        if (staticImagePrefetchStage >= 2) return@LaunchedEffect
        val guide = info ?: return@LaunchedEffect
        staticImagePrefetchStage = 2

        val urls = collectGuideStaticImagePrefetchUrls(guide)
            .drop(UiPerformanceBudget.guideStaticPrefetchInitialCount)
            .take(UiPerformanceBudget.guideStaticPrefetchGalleryExtraCount)
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
                                    contentDescription = shareSourceContentDescription,
                                    onClick = ::shareSource
                                ),
                                LiquidActionItem(
                                    icon = MiuixIcons.Regular.Refresh,
                                    contentDescription = stringResource(R.string.common_refresh),
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
                    enter = appFloatingEnter(),
                    exit = appFloatingExit(),
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
            beyondViewportPageCount = UiPerformanceBudget.guidePagerBeyondViewportPageCount,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = farJumpAlpha.value }
                .layerBackdrop(navBackdrop)
        ) { pageIndex ->
            val pageBottomTab = bottomTabs.getOrElse(pageIndex) { GuideBottomTab.Archive }
            val isVoiceTab = pageBottomTab == GuideBottomTab.Voice
            val shouldRenderHeavyContent =
                pageIndex == pagerState.currentPage ||
                    pageIndex == pagerState.targetPage ||
                    pageIndex == pagerState.settledPage
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (shouldRenderHeavyContent) {
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
                                    title = stringResource(R.string.guide_empty_student_title),
                                    subtitle = stringResource(R.string.guide_empty_student_subtitle),
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
                                onOpenExternal = ::openExternal,
                                onOpenGuide = ::openGuideInPage,
                                onSaveMedia = ::saveGuideMedia,
                                onToggleVoicePlayback = if (isVoiceTab) ::toggleVoicePlayback else ignoreStringInput,
                                onSelectedVoiceLanguageChange = if (isVoiceTab) {
                                    { selectedVoiceLanguage = it }
                                } else {
                                    ignoreStringInput
                                }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
                if (shouldRenderHeavyContent && sourceUrl.isNotBlank() && loading && info == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                                start = 20.dp,
                                end = 20.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.q_862c2944),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(112.dp)
                            )
                            Text(
                                text = stringResource(R.string.guide_loading_title),
                                color = MiuixTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberGuideSyncProgress(
    loading: Boolean,
    animationsEnabled: Boolean
): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(loading, animationsEnabled) {
        if (!animationsEnabled) {
            progress.snapTo(if (loading) 0.9f else 1f)
            return@LaunchedEffect
        }
        if (loading) {
            // Restart from a visible low point and move forward once, no looping.
            progress.snapTo(0.12f)
            progress.animateTo(
                targetValue = 0.68f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(520, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
            progress.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(1800, animationsEnabled),
                    easing = LinearEasing
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(260, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
        }
    }
    return progress.value
}
