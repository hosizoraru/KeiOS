package com.example.keios.ui.page.main.student.section

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.keios.ui.page.main.student.BaGuideGalleryItem
import com.example.keios.ui.page.main.student.extractGuideWebLinks
import com.example.keios.ui.page.main.student.isInteractiveFurnitureAnimatedGalleryItem
import com.example.keios.ui.page.main.student.isInteractiveFurnitureGalleryItem
import com.example.keios.ui.page.main.student.normalizeGalleryDisplayTitle
import com.example.keios.ui.page.main.student.normalizeGuideMediaSource
import com.example.keios.ui.page.main.student.section.gallery.BindGuideGalleryAudioPlayerEffects
import com.example.keios.ui.page.main.student.section.gallery.GuideGalleryCardContent
import com.example.keios.ui.page.main.student.section.gallery.GuideImageFullscreenDialog
import com.example.keios.ui.page.main.student.section.gallery.rememberGuideGalleryAudioPlayerState
import com.example.keios.ui.page.main.student.section.gallery.rememberGuideGalleryGestureState
import com.example.keios.ui.page.main.student.stripGuideWebLinks
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GuideGalleryCardItem(
    item: BaGuideGalleryItem,
    backdrop: Backdrop?,
    onOpenMedia: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit = { _, _ -> },
    audioLoopScopeKey: String = "",
    mediaUrlResolver: (String) -> String = { it },
    embedded: Boolean = false,
    showMediaTypeLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalizedMediaType = item.mediaType.lowercase()
    val isInteractiveFurnitureAnimated = remember(item.title, item.mediaUrl, item.imageUrl) {
        isInteractiveFurnitureAnimatedGalleryItem(item)
    }
    val disableFullscreenAutoRotate = remember(
        item.title,
        item.mediaUrl,
        item.imageUrl,
        isInteractiveFurnitureAnimated
    ) {
        isInteractiveFurnitureGalleryItem(item) && !isInteractiveFurnitureAnimated
    }
    val preferredImageRaw = remember(
        item.imageUrl,
        item.mediaUrl,
        normalizedMediaType,
        isInteractiveFurnitureAnimated
    ) {
        when {
            normalizedMediaType == "video" || normalizedMediaType == "audio" -> item.imageUrl
            isInteractiveFurnitureAnimated && item.mediaUrl.isNotBlank() -> item.mediaUrl
            item.imageUrl.isNotBlank() -> item.imageUrl
            else -> item.mediaUrl
        }
    }
    val mediaTypeLabel = when (normalizedMediaType) {
        "video" -> ""
        "audio" -> ""
        "live2d" -> "Live2D"
        "imageset" -> "图集"
        else -> ""
    }
    var displayImageUrl by remember(preferredImageRaw) {
        mutableStateOf(mediaUrlResolver(preferredImageRaw))
    }
    var displayMediaUrl by remember(item.mediaUrl, preferredImageRaw) {
        mutableStateOf(mediaUrlResolver(item.mediaUrl.ifBlank { preferredImageRaw }))
    }
    LaunchedEffect(item.mediaUrl, preferredImageRaw) {
        repeat(30) {
            delay(1_000L)
            val nextImageUrl = mediaUrlResolver(preferredImageRaw)
            val nextMediaUrl = mediaUrlResolver(item.mediaUrl.ifBlank { preferredImageRaw })
            var changed = false
            if (nextImageUrl != displayImageUrl) {
                displayImageUrl = nextImageUrl
                changed = true
            }
            if (nextMediaUrl != displayMediaUrl) {
                displayMediaUrl = nextMediaUrl
                changed = true
            }
            if (!changed &&
                normalizeGuideMediaSource(displayMediaUrl).startsWith("file://", ignoreCase = true) &&
                (displayImageUrl.isBlank() || normalizeGuideMediaSource(displayImageUrl).startsWith("file://", ignoreCase = true))
            ) {
                return@LaunchedEffect
            }
        }
    }
    val noteText = item.note.trim()
    val noteLinks = remember(noteText) { extractGuideWebLinks(noteText) }
    val notePlainText = remember(noteText) { stripGuideWebLinks(noteText) }
    val displayTitle = remember(item.title, normalizedMediaType) {
        normalizeGalleryDisplayTitle(item.title, normalizedMediaType)
    }
    val isMemoryHallBgmTitle = remember(displayTitle, normalizedMediaType) {
        normalizedMediaType == "audio" && displayTitle.startsWith("回忆大厅BGM")
    }
    val saveTargetUrl = remember(
        normalizedMediaType,
        displayImageUrl,
        displayMediaUrl,
        isInteractiveFurnitureAnimated
    ) {
        when (normalizedMediaType) {
            "video", "audio" -> displayMediaUrl.ifBlank { displayImageUrl }
            else -> {
                if (isInteractiveFurnitureAnimated && displayMediaUrl.isNotBlank()) {
                    displayMediaUrl
                } else {
                    displayImageUrl.ifBlank { displayMediaUrl }
                }
            }
        }
    }
    val canSaveMedia = saveTargetUrl.isNotBlank()
    val isImageType = normalizedMediaType != "video" && normalizedMediaType != "audio"
    val canOpenMedia = item.mediaUrl.isNotBlank() &&
        normalizeGuideMediaSource(displayMediaUrl) != normalizeGuideMediaSource(displayImageUrl)

    val gestureState = rememberGuideGalleryGestureState(
        displayMediaUrl = displayMediaUrl,
        normalizedMediaType = normalizedMediaType,
        displayImageUrl = displayImageUrl
    )

    val audioTargetUrl = remember(normalizedMediaType, displayMediaUrl) {
        if (normalizedMediaType == "audio") normalizeGuideMediaSource(displayMediaUrl) else ""
    }
    val audioState = rememberGuideGalleryAudioPlayerState(
        context = context,
        audioLoopScopeKey = audioLoopScopeKey,
        audioTargetUrl = audioTargetUrl
    )
    BindGuideGalleryAudioPlayerEffects(audioState)

    val imageProgressState = remember(displayImageUrl) {
        MutableStateFlow(if (displayImageUrl.isBlank()) 1f else 0f)
    }
    val imageProgress by imageProgressState.collectAsState()
    var imageLoading by remember(displayImageUrl) { mutableStateOf(displayImageUrl.isNotBlank()) }

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        GuideGalleryCardContent(
            backdrop = backdrop,
            normalizedMediaType = normalizedMediaType,
            displayTitle = displayTitle,
            mediaTypeLabel = mediaTypeLabel,
            showMediaTypeLabel = showMediaTypeLabel,
            isMemoryHallBgmTitle = isMemoryHallBgmTitle,
            audioTargetUrl = audioTargetUrl,
            displayMediaUrl = displayMediaUrl,
            displayImageUrl = displayImageUrl,
            canSaveMedia = canSaveMedia,
            saveTargetUrl = saveTargetUrl,
            isImageType = isImageType,
            imageProgress = imageProgress,
            imageLoading = imageLoading,
            onImageLoadingChanged = { loading -> imageLoading = loading },
            imageProgressState = imageProgressState,
            notePlainText = notePlainText,
            noteLinks = noteLinks,
            canOpenMedia = canOpenMedia,
            itemMediaUrl = item.mediaUrl,
            onOpenMedia = onOpenMedia,
            onSaveMedia = onSaveMedia,
            audioState = audioState,
            gestureState = gestureState,
            modifier = contentModifier
        )
    }

    if (embedded) {
        content(
            modifier
                .fillMaxWidth()
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = Color(0x223B82F6),
                contentColor = MiuixTheme.colorScheme.onBackground
            ),
            onClick = {}
        ) {
            content(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }

    if (gestureState.showImageFullscreen && isImageType && displayImageUrl.isNotBlank()) {
        GuideImageFullscreenDialog(
            imageUrl = displayImageUrl,
            allowAutoRotate = !disableFullscreenAutoRotate,
            onDismiss = { gestureState.showImageFullscreen = false }
        )
    }
}
