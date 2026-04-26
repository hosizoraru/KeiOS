package os.kei.ui.page.main.student.page.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.GuideBottomTab
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

@Composable
internal fun BaStudentGuidePagerContent(
    sourceUrl: String,
    info: BaStudentGuideInfo?,
    error: String?,
    pagerState: PagerState,
    bottomTabs: List<GuideBottomTab>,
    syncProgress: Float,
    activationCount: Int,
    surfaceColor: Color,
    accent: Color,
    innerPadding: PaddingValues,
    farJumpAlpha: Float,
    navBackdrop: LayerBackdrop,
    galleryCacheRevision: Int,
    selectedVoiceLanguage: String,
    playingVoiceUrl: String,
    isVoicePlaying: Boolean,
    voicePlayProgress: Float,
    includeTargetPageInHeavyRender: Boolean,
    guidePagerBeyondViewportPageCount: Int,
    nestedScrollConnection: NestedScrollConnection,
    onOpenExternal: (String) -> Unit,
    onOpenGuide: (String) -> Unit,
    onSaveMedia: (String, String) -> Unit,
    onSaveMediaPack: (List<Pair<String, String>>, String) -> Unit,
    onToggleVoicePlayback: (String) -> Unit,
    onSelectedVoiceLanguageChange: (String) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        key = { index -> bottomTabs.getOrNull(index)?.name ?: "stale-$index" },
        overscrollEffect = null,
        beyondViewportPageCount = guidePagerBeyondViewportPageCount,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = farJumpAlpha }
            .layerBackdrop(navBackdrop)
    ) { pageIndex ->
        BaStudentGuidePagerPage(
            sourceUrl = sourceUrl,
            info = info,
            error = error,
            pageIndex = pageIndex,
            bottomTabs = bottomTabs,
            pagerState = pagerState,
            activationCount = activationCount,
            surfaceColor = surfaceColor,
            accent = accent,
            innerPadding = innerPadding,
            syncProgress = syncProgress,
            galleryCacheRevision = galleryCacheRevision,
            selectedVoiceLanguage = selectedVoiceLanguage,
            playingVoiceUrl = playingVoiceUrl,
            isVoicePlaying = isVoicePlaying,
            voicePlayProgress = voicePlayProgress,
            includeTargetPageInHeavyRender = includeTargetPageInHeavyRender,
            nestedScrollConnection = nestedScrollConnection,
            onOpenExternal = onOpenExternal,
            onOpenGuide = onOpenGuide,
            onSaveMedia = onSaveMedia,
            onSaveMediaPack = onSaveMediaPack,
            onToggleVoicePlayback = onToggleVoicePlayback,
            onSelectedVoiceLanguageChange = onSelectedVoiceLanguageChange
        )
    }
}
