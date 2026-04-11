package com.example.keios.ui.page.main.student

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.keios.R
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
    val voiceRows: List<BaGuideRow> = emptyList(),
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
    val imageUrl: String
)

data class BaGuideVoiceEntry(
    val section: String,
    val title: String,
    val lines: List<String> = emptyList(),
    val audioUrl: String = ""
)

data class BaGuideMetaItem(
    val title: String,
    val value: String,
    val imageUrl: String,
    val extraImageUrl: String = ""
)

enum class GuideTab(val label: String) {
    Skills("角色技能"),
    Profile("学生档案"),
    Voice("语音台词"),
    Gallery("影画鉴赏"),
    Simulate("养成模拟")
}

enum class GuideBottomTab(
    val label: String,
    val icon: ImageVector,
    val localLogoRes: Int? = null,
    val guideTab: GuideTab? = null
) {
    Archive("档案", MiuixIcons.Regular.ContactsBook),
    Skills(
        GuideTab.Skills.label,
        MiuixIcons.Regular.Tasks,
        localLogoRes = R.drawable.ba_tab_skill,
        guideTab = GuideTab.Skills
    ),
    Profile(
        GuideTab.Profile.label,
        MiuixIcons.Regular.ContactsBook,
        localLogoRes = R.drawable.ba_tab_profile,
        guideTab = GuideTab.Profile
    ),
    Voice(GuideTab.Voice.label, MiuixIcons.Regular.Mic, guideTab = GuideTab.Voice),
    Gallery(GuideTab.Gallery.label, MiuixIcons.Regular.Album, guideTab = GuideTab.Gallery),
    Simulate(
        GuideTab.Simulate.label,
        MiuixIcons.Regular.Stopwatch,
        localLogoRes = R.drawable.ba_tab_simulate,
        guideTab = GuideTab.Simulate
    )
}
