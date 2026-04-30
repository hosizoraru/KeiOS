package os.kei.ui.page.main.student.tabcontent

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Color
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.tabcontent.render.renderGuideArchiveTabContent
import os.kei.ui.page.main.student.tabcontent.render.renderGuideGalleryTabContent
import os.kei.ui.page.main.student.tabcontent.render.renderGuideProfileTabContent
import os.kei.ui.page.main.student.tabcontent.render.renderGuideSimulateTabContent
import os.kei.ui.page.main.student.tabcontent.render.renderGuideSkillsTabContent
import os.kei.ui.page.main.student.tabcontent.render.renderGuideVoiceTabContent
import com.kyant.backdrop.backdrops.LayerBackdrop

internal fun LazyListScope.renderBaStudentGuideTabContent(
    activeBottomTab: GuideBottomTab,
    activeBottomTabLabel: String,
    info: BaStudentGuideInfo?,
    error: String?,
    backdrop: LayerBackdrop,
    accent: Color,
    context: Context,
    sourceUrl: String,
    galleryCacheRevision: Int,
    playingVoiceUrl: String,
    isVoicePlaying: Boolean,
    voicePlayProgress: Float,
    selectedVoiceLanguage: String,
    onOpenExternal: (String) -> Unit,
    onOpenGuide: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit,
    onSaveMediaPack: (items: List<Pair<String, String>>, packTitle: String) -> Unit,
    onToggleVoicePlayback: (String) -> Unit,
    onSelectedVoiceLanguageChange: (String) -> Unit
) {
    when (activeBottomTab) {
        GuideBottomTab.Archive -> renderGuideArchiveTabContent(
            info = info
        )

        GuideBottomTab.Skills -> renderGuideSkillsTabContent(
            tabLabel = activeBottomTabLabel,
            info = info,
            error = error,
            backdrop = backdrop,
            accent = accent
        )

        GuideBottomTab.Profile -> renderGuideProfileTabContent(
            tabLabel = activeBottomTabLabel,
            info = info,
            error = error,
            backdrop = backdrop,
            accent = accent,
            context = context,
            sourceUrl = sourceUrl,
            galleryCacheRevision = galleryCacheRevision,
            onOpenExternal = onOpenExternal,
            onOpenGuide = onOpenGuide,
            onSaveMedia = onSaveMedia
        )

        GuideBottomTab.Voice -> renderGuideVoiceTabContent(
            tabLabel = activeBottomTabLabel,
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
            selectedVoiceLanguage = selectedVoiceLanguage,
            onToggleVoicePlayback = onToggleVoicePlayback,
            onSelectedVoiceLanguageChange = onSelectedVoiceLanguageChange
        )

        GuideBottomTab.Gallery -> renderGuideGalleryTabContent(
            tabLabel = activeBottomTabLabel,
            info = info,
            error = error,
            backdrop = backdrop,
            accent = accent,
            context = context,
            sourceUrl = sourceUrl,
            galleryCacheRevision = galleryCacheRevision,
            onOpenExternal = onOpenExternal,
            onSaveMedia = onSaveMedia,
            onSaveMediaPack = onSaveMediaPack
        )

        GuideBottomTab.Simulate -> renderGuideSimulateTabContent(
            tabLabel = activeBottomTabLabel,
            info = info,
            error = error,
            backdrop = backdrop,
            accent = accent
        )
    }
}
