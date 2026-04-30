package os.kei.ui.page.main.student.page.state

import androidx.compose.ui.graphics.Color
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.GuideBottomTab

internal data class BaStudentGuidePagerHeaderState(
    val title: String,
    val showSyncIndicator: Boolean,
    val indicatorColor: Color
)

internal data class BaStudentGuideTabRenderState(
    val activeBottomTab: GuideBottomTab,
    val shouldRenderHeavyContent: Boolean,
    val playingVoiceUrl: String,
    val isVoicePlaying: Boolean,
    val voicePlayProgress: Float,
    val selectedVoiceLanguage: String
)

internal fun buildBaStudentGuidePagerHeaderState(
    tabLabel: String,
    sourceUrl: String,
    info: BaStudentGuideInfo?,
    error: String?
): BaStudentGuidePagerHeaderState {
    val indicatorColor = when {
        info == null && error.isNullOrBlank() -> Color(0xFF3B82F6)
        !error.isNullOrBlank() -> Color(0xFFEF4444)
        else -> Color(0xFF22C55E)
    }
    return BaStudentGuidePagerHeaderState(
        title = tabLabel,
        showSyncIndicator = sourceUrl.isNotBlank(),
        indicatorColor = indicatorColor
    )
}

internal fun resolveBaStudentGuideTabRenderState(
    pageIndex: Int,
    bottomTabs: List<GuideBottomTab>,
    currentPage: Int,
    settledPage: Int,
    targetPage: Int,
    includeTargetPageInHeavyRender: Boolean,
    playingVoiceUrl: String,
    isVoicePlaying: Boolean,
    voicePlayProgress: Float,
    selectedVoiceLanguage: String
): BaStudentGuideTabRenderState {
    val activeBottomTab = bottomTabs.getOrElse(pageIndex) { GuideBottomTab.Archive }
    val isVoiceTab = activeBottomTab == GuideBottomTab.Voice
    val shouldRenderHeavyContent =
        pageIndex == currentPage ||
            pageIndex == settledPage ||
            (includeTargetPageInHeavyRender && pageIndex == targetPage)
    return BaStudentGuideTabRenderState(
        activeBottomTab = activeBottomTab,
        shouldRenderHeavyContent = shouldRenderHeavyContent,
        playingVoiceUrl = if (isVoiceTab) playingVoiceUrl else "",
        isVoicePlaying = isVoiceTab && isVoicePlaying,
        voicePlayProgress = if (isVoiceTab) voicePlayProgress else 0f,
        selectedVoiceLanguage = if (isVoiceTab) selectedVoiceLanguage else ""
    )
}
