package os.kei.ui.page.main.student.section.gallery

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.GuideVideoControlAction
import os.kei.ui.page.main.student.GuideVideoFullscreenActivity
import os.kei.ui.page.main.student.section.buildGuideCopyPayload
import os.kei.ui.page.main.student.section.guideCopyable
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun GuideGalleryVideoGroupCardItem(
    title: String,
    items: List<BaGuideGalleryItem>,
    previewFallbackUrl: String = "",
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit = { _, _ -> },
    mediaUrlResolver: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    var selectedIndex by rememberSaveable(title, items.size) { mutableStateOf(0) }
    LaunchedEffect(items.size) {
        if (selectedIndex !in items.indices) selectedIndex = 0
    }
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }
    val displayMediaUrl = mediaUrlResolver(selectedItem.mediaUrl)
    val displayPreviewUrl = mediaUrlResolver(
        selectedItem.imageUrl.ifBlank { previewFallbackUrl }
    )
    val saveTargetUrl = remember(displayMediaUrl, displayPreviewUrl) {
        displayMediaUrl.ifBlank { displayPreviewUrl }
    }
    var videoInlineExpanded by remember(displayMediaUrl) { mutableStateOf(false) }
    var videoInlinePlaying by remember(displayMediaUrl) { mutableStateOf(false) }
    var videoControlRequestId by remember(displayMediaUrl) { mutableIntStateOf(0) }
    val noteText = selectedItem.note.trim()
    val optionLabels = remember(title, items) {
        if (items.size <= 1) {
            listOf("视频 1")
        } else {
            items.mapIndexed { index, item ->
                val normalized = item.title.trim()
                if (normalized.isNotBlank() && normalized != title) normalized else "视频 ${index + 1}"
            }
        }
    }

    AppFeatureCard(
        title = title,
        subtitle = noteText,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0x223B82F6),
        headerEndActions = {
            GuideGalleryVideoGroupHeaderActions(
                itemsSize = items.size,
                optionLabels = optionLabels,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { selectedIndex = it },
                displayMediaUrl = displayMediaUrl,
                saveTargetUrl = saveTargetUrl,
                videoInlineExpanded = videoInlineExpanded,
                videoInlinePlaying = videoInlinePlaying,
                backdrop = backdrop,
                onToggleInlinePlay = {
                    if (normalizeGuideMediaSource(displayMediaUrl).isBlank()) {
                        Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                    } else if (!videoInlineExpanded) {
                        videoInlineExpanded = true
                    } else {
                        videoControlRequestId += 1
                    }
                },
                onOpenFullscreen = {
                    val normalized = normalizeGuideMediaSource(displayMediaUrl)
                    if (normalized.isBlank()) {
                        Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                    } else {
                        GuideVideoFullscreenActivity.Companion.launch(
                            context = context,
                            mediaUrl = normalized
                        )
                    }
                },
                onSaveMedia = {
                    onSaveMedia(
                        saveTargetUrl,
                        optionLabels.getOrElse(selectedIndex) { title }
                    )
                }
            )
        }
    ) {
        if (displayMediaUrl.isBlank()) {
            Text(
                text = "未找到可播放的视频地址",
                color = MiuixTheme.colorScheme.onBackgroundVariant
            )
        } else {
            GuideInlineVideoPlayer(
                mediaUrl = displayMediaUrl,
                previewImageUrl = displayPreviewUrl,
                backdrop = backdrop,
                expanded = videoInlineExpanded,
                onExpandedChange = { expanded -> videoInlineExpanded = expanded },
                controlAction = GuideVideoControlAction.TogglePlayPause,
                controlActionToken = videoControlRequestId,
                onIsPlayingChange = { playing -> videoInlinePlaying = playing },
                showCollapsedPreview = false
            )
        }
    }
}

@Composable
fun GuideGalleryUnlockLevelCardItem(
    level: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    if (level.isBlank()) return
    val rowCopyPayload = remember(level) {
        buildGuideCopyPayload("回忆大厅解锁等级", level)
    }
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0x223B82F6)
    ) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideCopyable(rowCopyPayload)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "回忆大厅解锁等级",
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                GlassTextButton(
                    backdrop = backdrop,
                    text = level,
                    enabled = false,
                    textColor = Color(0xFF3B82F6),
                    variant = GlassVariant.Compact,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
internal fun GuideInlineVideoPlayer(
    mediaUrl: String,
    previewImageUrl: String = "",
    backdrop: Backdrop?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    controlAction: GuideVideoControlAction? = null,
    controlActionToken: Int = 0,
    onIsPlayingChange: (Boolean) -> Unit = {},
    onBufferingChange: (Boolean) -> Unit = {},
    previewProgressState: MutableStateFlow<Float>? = null,
    onPreviewLoadingChanged: ((Boolean) -> Unit)? = null,
    showCollapsedPreview: Boolean = true
) {
    val context = LocalContext.current
    val normalizedUrl = remember(mediaUrl) { normalizeGuideMediaSource(mediaUrl) }
    val normalizedPreviewUrl = remember(previewImageUrl) { normalizeGuideMediaSource(previewImageUrl) }
    var videoRatio by remember(normalizedUrl) { mutableStateOf(16f / 9f) }
    var isBuffering by remember(normalizedUrl) { mutableStateOf(false) }
    var isPlaying by remember(normalizedUrl) { mutableStateOf(false) }
    var loadError by remember(normalizedUrl) { mutableStateOf<String?>(null) }
    var loopEnabled by remember(normalizedUrl) { mutableStateOf(false) }
    val openFullscreen = remember(context, normalizedUrl) {
        {
            if (normalizedUrl.isBlank()) {
                Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
            } else {
                GuideVideoFullscreenActivity.Companion.launch(
                    context = context,
                    mediaUrl = normalizedUrl
                )
            }
        }
    }

    if (!expanded) {
        if (showCollapsedPreview) {
            GuideInlineVideoPreview(
                previewImageUrl = normalizedPreviewUrl,
                onOpenFullscreen = {
                    onExpandedChange(false)
                    openFullscreen()
                },
                previewProgressState = previewProgressState,
                onPreviewLoadingChanged = onPreviewLoadingChanged
            )
        } else {
            previewProgressState?.value = 1f
            onPreviewLoadingChanged?.invoke(false)
        }
        onBufferingChange(false)
        onIsPlayingChange(false)
        return
    }

    val player = rememberGuidePreparedVideoPlayer(
        context = context,
        mediaUrl = normalizedUrl,
        active = expanded
    )
    BindGuideVideoPlayerState(
        player = player,
        onVideoRatioChanged = { ratio -> videoRatio = ratio },
        onBufferingChanged = { buffering ->
            isBuffering = buffering
            onBufferingChange(buffering)
        },
        onIsPlayingChanged = { playing ->
            isPlaying = playing
            onIsPlayingChange(playing)
        },
        onPlayerErrorChanged = { errorCode ->
            isBuffering = false
            loadError = errorCode
        },
        onDispose = {
            isBuffering = false
            isPlaying = false
            onBufferingChange(false)
            onIsPlayingChange(false)
        }
    )
    BindGuideVideoForegroundPlaybackGuard(
        player = player,
        onForegroundStopped = {
            isBuffering = false
            isPlaying = false
            onBufferingChange(false)
            onIsPlayingChange(false)
        }
    )

    LaunchedEffect(player, loopEnabled) {
        player?.repeatMode = if (loopEnabled) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    val activePlayer = player
    if (activePlayer == null) {
        onIsPlayingChange(false)
        GuideInlineVideoUnavailableHint()
        return
    }

    LaunchedEffect(controlActionToken, controlAction, activePlayer) {
        if (controlActionToken <= 0 || controlAction == null) return@LaunchedEffect
        when (controlAction) {
            GuideVideoControlAction.TogglePlayPause -> {
                if (activePlayer.isPlaying) {
                    activePlayer.pause()
                } else {
                    activePlayer.play()
                }
            }
        }
    }

    GuideInlineVideoPlayerBody(
        player = activePlayer,
        videoRatio = videoRatio,
        loopEnabled = loopEnabled,
        onToggleLoop = { loopEnabled = !loopEnabled },
        onCollapse = { onExpandedChange(false) },
        backdrop = backdrop
    )
    GuideInlineVideoStatusHints(
        isBuffering = isBuffering,
        loadError = loadError
    )
}
