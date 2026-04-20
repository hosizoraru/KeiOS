package com.example.keios.ui.page.main.student.section.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
internal class GuideGalleryGestureState {
    var videoInlineExpanded by mutableStateOf(false)
    var videoInlinePlaying by mutableStateOf(false)
    var videoControlRequestId by mutableIntStateOf(0)
    var showImageFullscreen by mutableStateOf(false)

    fun requestToggleVideoPlayback() {
        videoControlRequestId += 1
    }
}

@Composable
internal fun rememberGuideGalleryGestureState(
    displayMediaUrl: String,
    normalizedMediaType: String,
    displayImageUrl: String
): GuideGalleryGestureState {
    return remember(displayMediaUrl, normalizedMediaType, displayImageUrl) {
        GuideGalleryGestureState()
    }
}
