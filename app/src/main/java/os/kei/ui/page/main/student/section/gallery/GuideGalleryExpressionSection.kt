package os.kei.ui.page.main.student.section.gallery

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.MutableStateFlow
import os.kei.R
import os.kei.ui.page.main.os.appLucideFullscreenIcon
import os.kei.ui.page.main.os.appLucidePackageIcon
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.section.GuidePressableMediaSurface
import os.kei.ui.page.main.student.GuideRemoteImageAdaptive
import os.kei.ui.page.main.student.GuideVideoControlAction
import os.kei.ui.page.main.student.GuideVideoFullscreenActivity
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.normalizeGalleryTitle
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.widget.glass.AppDropdownAnchorButton
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownSingleChoiceItem
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressSlider
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideAudioSeekBar(
    progress: Float,
    enabled: Boolean,
    onSeekStarted: () -> Unit,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit
) {
    val sliderBackdrop = rememberLayerBackdrop()
    var seekInteractionActive by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(sliderBackdrop)
        )
        LiquidMusicProgressSlider(
            value = { progress.coerceIn(0f, 1f) },
            onValueChange = { value -> onSeekChanged(value.coerceIn(0f, 1f)) },
            onValueChangeFinished = { value -> onSeekFinished(value.coerceIn(0f, 1f)) },
            onInteractionChanged = { active ->
                if (active && !seekInteractionActive) {
                    seekInteractionActive = true
                    onSeekStarted()
                }
                if (!active && seekInteractionActive) {
                    seekInteractionActive = false
                }
            },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = sliderBackdrop,
            enabled = enabled,
            activeColor = Color(0xFF3B82F6),
            inactiveColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp)
        )
    }
}

internal fun formatAudioDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
fun GuideGalleryExpressionCardItem(
    title: String,
    items: List<BaGuideGalleryItem>,
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit = { _, _ -> },
    onSaveMediaPack: (items: List<Pair<String, String>>, packTitle: String) -> Unit = { _, _ -> },
    mediaUrlResolver: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    var showPicker by remember(title, items.size) { mutableStateOf(false) }
    var selectedIndex by rememberSaveable(title, items.size) { mutableStateOf(0) }
    var previousSelectedIndex by rememberSaveable(title, items.size) { mutableIntStateOf(0) }
    var showSwipeHint by rememberSaveable(title, items.size) { mutableStateOf(true) }
    LaunchedEffect(items.size) {
        if (selectedIndex !in items.indices) selectedIndex = 0
    }
    LaunchedEffect(selectedIndex) {
        previousSelectedIndex = selectedIndex
    }
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }
    val displayImageUrl = mediaUrlResolver(selectedItem.imageUrl)
    val displayMediaUrl = mediaUrlResolver(selectedItem.mediaUrl)
    val saveTargetUrl = remember(selectedItem.mediaType, displayImageUrl, displayMediaUrl) {
        if (selectedItem.mediaType.lowercase() == "video") {
            displayMediaUrl.ifBlank { displayImageUrl }
        } else {
            displayImageUrl.ifBlank { displayMediaUrl }
        }
    }
    val optionLabels = remember(context, items) {
        items.mapIndexed { index, item ->
            val normalizedTitle = normalizeGalleryTitle(item.title)
            val rawVariant = when {
                normalizedTitle.startsWith("角色表情") -> normalizedTitle.removePrefix("角色表情")
                normalizedTitle.startsWith("表情") -> normalizedTitle.removePrefix("表情")
                else -> ""
            }
            val variant = rawVariant
                .replace(Regex("""\d+$"""), "")
                .trim('（', '）', '(', ')', '-', '·', ' ')
            if (variant.isBlank()) {
                context.getString(R.string.guide_gallery_expression_fallback_format, index + 1)
            } else if (variant == "包") {
                context.getString(R.string.guide_gallery_expression_pack_format, index + 1)
            } else {
                val displayVariant = when (variant) {
                    "差分" -> context.getString(R.string.guide_gallery_expression_variant_difference)
                    else -> variant
                }
                context.getString(R.string.guide_gallery_expression_variant_format, index + 1, displayVariant)
            }
        }
    }
    val pickerMaxHeight = remember(optionLabels.size) {
        val maxVisibleRows = 7
        val visibleRows = optionLabels.size.coerceIn(1, maxVisibleRows)
        8.dp + (46.dp * visibleRows)
    }
    val canOpenMedia = selectedItem.mediaUrl.isNotBlank() && selectedItem.mediaUrl != selectedItem.imageUrl
    val isImageType = selectedItem.mediaType.lowercase() != "video"
    val isVideoType = selectedItem.mediaType.lowercase() == "video"
    var videoInlineExpanded by remember(displayMediaUrl, selectedItem.mediaType) { mutableStateOf(false) }
    var videoInlinePlaying by remember(displayMediaUrl, selectedItem.mediaType) { mutableStateOf(false) }
    var videoControlRequestId by remember(displayMediaUrl, selectedItem.mediaType) { mutableIntStateOf(0) }
    var showImageFullscreen by remember(displayImageUrl) { mutableStateOf(false) }
    val canSwipeExpressions = optionLabels.size > 1
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var expressionDragAccumPx by remember(title, items.size) { mutableFloatStateOf(0f) }
    val expressionDragState = rememberDraggableState { delta ->
        expressionDragAccumPx += delta
    }
    val imageProgressState = remember(displayImageUrl) {
        MutableStateFlow(if (displayImageUrl.isBlank()) 1f else 0f)
    }
    val imageProgress by imageProgressState.collectAsState()
    var imageLoading by remember(displayImageUrl) { mutableStateOf(displayImageUrl.isNotBlank()) }
    val packDownloadIcon = appLucidePackageIcon()
    val fullscreenIcon = appLucideFullscreenIcon()
    val expressionPackTargets = remember(context, items, optionLabels, mediaUrlResolver) {
        items.mapIndexedNotNull { index, item ->
            val rawImage = mediaUrlResolver(item.imageUrl)
            val rawMedia = mediaUrlResolver(item.mediaUrl)
            val target = if (item.mediaType.lowercase() == "video") {
                rawMedia.ifBlank { rawImage }
            } else {
                rawImage.ifBlank { rawMedia }
            }.trim()
            if (target.isBlank()) {
                null
            } else {
                target to optionLabels.getOrElse(index) {
                    context.getString(R.string.guide_gallery_expression_fallback_format, index + 1)
                }
            }
        }.distinctBy { it.first }
    }

    GuideLiquidCard(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = Color(0x223B82F6),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
                Box(
                    modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it }
                ) {
                    AppDropdownAnchorButton(
                        backdrop = backdrop,
                        text = optionLabels.getOrElse(selectedIndex) {
                            stringResource(R.string.guide_gallery_expression_fallback_format, 1)
                        },
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = { showPicker = !showPicker }
                    )
                    if (showPicker) {
                        SnapshotWindowListPopup(
                            show = showPicker,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            anchorBounds = pickerPopupAnchorBounds,
                            placement = SnapshotPopupPlacement.ButtonEnd,
                            onDismissRequest = { showPicker = false },
                            enableWindowDim = false
                        ) {
                            LiquidGlassDropdownColumn(
                                modifier = Modifier.heightIn(max = pickerMaxHeight),
                                accentColor = Color(0xFF3B82F6),
                                backdrop = backdrop
                            ) {
                                optionLabels.forEachIndexed { idx, option ->
                                    LiquidGlassDropdownSingleChoiceItem(
                                        text = option,
                                        optionSize = optionLabels.size,
                                        isSelected = selectedIndex == idx,
                                        index = idx,
                                        onSelectedIndexChange = { selected ->
                                            showSwipeHint = false
                                            selectedIndex = selected
                                            showPicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (isVideoType && displayMediaUrl.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = if (videoInlineExpanded && videoInlinePlaying) {
                            MiuixIcons.Regular.Pause
                        } else {
                            MiuixIcons.Regular.Play
                        },
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            if (normalizeGuideMediaSource(displayMediaUrl).isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.guide_media_video_url_invalid),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (!videoInlineExpanded) {
                                videoInlineExpanded = true
                            } else {
                                videoControlRequestId += 1
                            }
                        }
                    )
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = fullscreenIcon,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            val normalized = normalizeGuideMediaSource(displayMediaUrl)
                            if (normalized.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.guide_media_video_url_invalid),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                GuideVideoFullscreenActivity.Companion.launch(
                                    context = context,
                                    mediaUrl = normalized
                                )
                            }
                        }
                    )
                }
                if (saveTargetUrl.isNotBlank()) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = MiuixIcons.Regular.Download,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            onSaveMedia(
                                saveTargetUrl,
                                optionLabels.getOrElse(selectedIndex) { title }
                            )
                        }
                    )
                }
                if (expressionPackTargets.size > 1) {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = "",
                        leadingIcon = packDownloadIcon,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = {
                            onSaveMediaPack(expressionPackTargets, title)
                        }
                    )
                }
                if (isImageType && displayImageUrl.isNotBlank()) {
                    val imageProgressValue = if (imageLoading) imageProgress.coerceIn(0f, 1f) else 1f
                    val progressForegroundColor = if (imageProgressValue >= 0.999f) Color(0xFF34C759) else Color(0xFF3B82F6)
                    val progressBackgroundColor = if (imageProgressValue >= 0.999f) Color(0x5534C759) else Color(0x553B82F6)
                    LiquidCircularProgressBar(
                        progress = { imageProgressValue },
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        activeColor = progressForegroundColor,
                        inactiveColor = progressBackgroundColor
                    )
                }
            }
            if (canSwipeExpressions && showSwipeHint) {
                Text(
                    text = stringResource(R.string.guide_expression_swipe_hint),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (displayImageUrl.isNotBlank() && selectedItem.mediaType.lowercase() != "video") {
                GuidePressableMediaSurface(
                    modifier = Modifier.draggable(
                        state = expressionDragState,
                        orientation = Orientation.Horizontal,
                        enabled = canSwipeExpressions,
                        onDragStopped = { velocity ->
                            val totalDrag = expressionDragAccumPx
                            expressionDragAccumPx = 0f
                            val shouldGoNext = totalDrag <= -swipeThresholdPx || velocity <= -1600f
                            val shouldGoPrev = totalDrag >= swipeThresholdPx || velocity >= 1600f
                            when {
                                shouldGoNext && selectedIndex < items.lastIndex -> {
                                    showSwipeHint = false
                                    selectedIndex += 1
                                    showPicker = false
                                }

                                shouldGoPrev && selectedIndex > 0 -> {
                                    showSwipeHint = false
                                    selectedIndex -= 1
                                    showPicker = false
                                }
                            }
                        }
                    ),
                    onClick = { showImageFullscreen = true }
                ) {
                    val slideToLeft = selectedIndex > previousSelectedIndex
                    AnimatedContent(
                        targetState = displayImageUrl,
                        transitionSpec = {
                            if (!transitionAnimationsEnabled) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                val enter = slideInHorizontally(
                                    animationSpec = tween(durationMillis = 220),
                                    initialOffsetX = { fullWidth ->
                                        if (slideToLeft) fullWidth / 3 else -fullWidth / 3
                                    }
                                ) + fadeIn(animationSpec = tween(durationMillis = 180))
                                val exit = slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 180),
                                    targetOffsetX = { fullWidth ->
                                        if (slideToLeft) -fullWidth / 4 else fullWidth / 4
                                    }
                                ) + fadeOut(animationSpec = tween(durationMillis = 140))
                                enter togetherWith exit
                            }
                        },
                        label = "guide_expression_swipe_transition"
                    ) { currentImageUrl ->
                        GuideRemoteImageAdaptive(
                            imageUrl = currentImageUrl,
                            progressState = if (isImageType) imageProgressState else null,
                            onLoadingChanged = if (isImageType) {
                                { loading -> imageLoading = loading }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            if (canOpenMedia) {
                if (selectedItem.mediaType.lowercase() == "video") {
                    GuideInlineVideoPlayer(
                        mediaUrl = displayMediaUrl,
                        previewImageUrl = displayImageUrl,
                        backdrop = backdrop,
                        expanded = videoInlineExpanded,
                        onExpandedChange = { expanded -> videoInlineExpanded = expanded },
                        controlAction = GuideVideoControlAction.TogglePlayPause,
                        controlActionToken = videoControlRequestId,
                        onIsPlayingChange = { playing -> videoInlinePlaying = playing }
                    )
                } else {
                    GlassTextButton(
                        backdrop = backdrop,
                        text = stringResource(R.string.guide_action_open),
                        leadingIcon = MiuixIcons.Regular.Play,
                        textColor = Color(0xFF3B82F6),
                        variant = GlassVariant.Compact,
                        onClick = { onOpenMedia(selectedItem.mediaUrl) }
                    )
                }
            }
        }
    }

    if (showImageFullscreen && isImageType && displayImageUrl.isNotBlank()) {
        GuideImageFullscreenDialog(
            imageUrl = displayImageUrl,
            onDismiss = { showImageFullscreen = false }
        )
    }
}
