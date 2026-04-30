package os.kei.ui.page.main.student

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import os.kei.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.ContactsBook
import top.yukonga.miuix.kmp.icon.extended.Mic
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Tasks

data class BaStudentGuideInfo(
    val sourceUrl: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val imageUrl: String,
    val summary: String,
    val stats: List<Pair<String, String>>,
    val skillRows: List<BaGuideRow> = emptyList(),
    val profileRows: List<BaGuideRow> = emptyList(),
    val galleryItems: List<BaGuideGalleryItem> = emptyList(),
    val growthRows: List<BaGuideRow> = emptyList(),
    val simulateRows: List<BaGuideRow> = emptyList(),
    val voiceRows: List<BaGuideRow> = emptyList(),
    val voiceCvJp: String = "",
    val voiceCvCn: String = "",
    val voiceCvByLanguage: Map<String, String> = emptyMap(),
    val voiceLanguageHeaders: List<String> = emptyList(),
    val voiceEntries: List<BaGuideVoiceEntry> = emptyList(),
    val tabSkillIconUrl: String = "",
    val tabProfileIconUrl: String = "",
    val tabVoiceIconUrl: String = "",
    val tabGalleryIconUrl: String = "",
    val tabSimulateIconUrl: String = "",
    val syncedAtMs: Long
)

data class BaGuideRow(
    val key: String,
    val value: String,
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList()
)

data class BaGuideGalleryItem(
    val title: String,
    val imageUrl: String,
    val mediaType: String = "image",
    val mediaUrl: String = imageUrl,
    val memoryUnlockLevel: String = "",
    val note: String = ""
)

data class BaGuideVoiceEntry(
    val section: String,
    val title: String,
    val lineHeaders: List<String> = emptyList(),
    val lines: List<String> = emptyList(),
    val audioUrls: List<String> = emptyList(),
    val audioUrl: String = ""
)

data class BaGuideMetaItem(
    val title: String,
    val value: String,
    val imageUrl: String,
    val extraImageUrl: String = ""
)

enum class GuideTab(
    val label: String,
    @param:StringRes val labelRes: Int
) {
    Skills("角色技能", R.string.guide_tab_skills),
    Profile("学生档案", R.string.guide_tab_profile),
    Voice("语音台词", R.string.guide_tab_voice),
    Gallery("影画鉴赏", R.string.guide_tab_gallery),
    Simulate("养成模拟", R.string.guide_tab_simulate)
}

enum class GuideBottomTab(
    val label: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val localLogoRes: Int? = null,
    val guideTab: GuideTab? = null
) {
    Archive("属性概览", R.string.guide_tab_archive, MiuixIcons.Regular.ContactsBook),
    Skills(
        GuideTab.Skills.label,
        GuideTab.Skills.labelRes,
        MiuixIcons.Regular.Tasks,
        localLogoRes = R.drawable.ba_tab_skill,
        guideTab = GuideTab.Skills
    ),
    Profile(
        GuideTab.Profile.label,
        GuideTab.Profile.labelRes,
        MiuixIcons.Regular.ContactsBook,
        localLogoRes = R.drawable.ba_tab_profile,
        guideTab = GuideTab.Profile
    ),
    Voice(
        GuideTab.Voice.label,
        GuideTab.Voice.labelRes,
        MiuixIcons.Regular.Mic,
        guideTab = GuideTab.Voice
    ),
    Gallery(
        GuideTab.Gallery.label,
        GuideTab.Gallery.labelRes,
        MiuixIcons.Regular.Album,
        guideTab = GuideTab.Gallery
    ),
    Simulate(
        GuideTab.Simulate.label,
        GuideTab.Simulate.labelRes,
        MiuixIcons.Regular.Stopwatch,
        localLogoRes = R.drawable.ba_tab_simulate,
        guideTab = GuideTab.Simulate
    )
}
