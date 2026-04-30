package os.kei.ui.page.main.student.tabcontent.render

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.tabcontent.buildDubbingHeadersForVoiceCard
import os.kei.ui.page.main.student.tabcontent.buildGrowthTitleVoiceEntries
import os.kei.ui.page.main.student.buildGuideTabCopyPayload
import os.kei.ui.page.main.student.guideLocalizedVoiceLanguage
import os.kei.ui.page.main.student.tabcontent.buildVoiceCvDisplayMap
import os.kei.ui.page.main.student.tabcontent.buildVoiceLanguageHeadersForDisplay
import os.kei.ui.page.main.student.fetch.normalizeGuideUrl
import os.kei.ui.page.main.student.tabcontent.isGrowthTitleVoiceRow
import os.kei.ui.page.main.student.tabcontent.normalizeGuidePlaybackSource
import os.kei.ui.page.main.student.profileRowsForDisplay
import os.kei.ui.page.main.student.rememberGuideTabCopyAction
import os.kei.ui.page.main.student.tabcontent.resolveVoicePlaybackUrl
import os.kei.ui.page.main.student.section.GuideVoiceEntryCard
import os.kei.ui.page.main.student.section.GuideVoiceLanguageCard
import os.kei.ui.page.main.widget.glass.FrostedBlock
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import com.kyant.backdrop.backdrops.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderGuideVoiceTabContent(
    tabLabel: String,
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
    onToggleVoicePlayback: (String) -> Unit,
    onSelectedVoiceLanguageChange: (String) -> Unit
) {
    val guide = info
    if (guide == null) {
        item {
            FrostedBlock(
                backdrop = backdrop,
                title = tabLabel,
                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                accent = accent
            )
        }
        return
    }

    val structuredVoiceEntries = guide.voiceEntries.filter { entry ->
        entry.lines.any { line -> line.trim().isNotBlank() }
    }
    val migratedVoiceEntries = buildGrowthTitleVoiceEntries(
        guide.profileRowsForDisplay()
            .filter(::isGrowthTitleVoiceRow)
    )
    val voiceEntries = (structuredVoiceEntries + migratedVoiceEntries)
        .distinctBy { entry ->
            listOf(
                entry.section.trim(),
                entry.title.trim(),
                entry.lineHeaders.joinToString("|") { it.trim() },
                entry.lines.joinToString("|") { it.trim() },
                entry.audioUrls.joinToString("|") { normalizeGuideUrl(it) },
                normalizeGuideUrl(entry.audioUrl)
            ).joinToString("|")
        }
    val voiceCvByLanguage = buildVoiceCvDisplayMap(guide)
    val voiceLanguageHeaders = buildVoiceLanguageHeadersForDisplay(
        headers = guide.voiceLanguageHeaders,
        entries = voiceEntries
    )
    val dubbingHeaders = buildDubbingHeadersForVoiceCard(
        headers = voiceLanguageHeaders,
        entries = voiceEntries
    )
    val selectedDubbingHeader = dubbingHeaders.firstOrNull { header ->
        header.equals(selectedVoiceLanguage.trim(), ignoreCase = true)
    } ?: dubbingHeaders.firstOrNull().orEmpty()

    if (voiceCvByLanguage.isNotEmpty()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = Color(0x223B82F6),
                    contentColor = MiuixTheme.colorScheme.onBackground
                ),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    voiceCvByLanguage.forEach { (label, value) ->
                        val displayLabel = guideLocalizedVoiceLanguage(label)
                        val title = if (label in listOf("日配", "中配", "韩配")) {
                            stringResource(R.string.guide_voice_cv_format, displayLabel)
                        } else {
                            displayLabel
                        }
                        MiuixInfoItem(
                            key = title,
                            value = value,
                            onLongClick = rememberGuideTabCopyAction(
                                buildGuideTabCopyPayload(title, value)
                            )
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (voiceEntries.isNotEmpty() && dubbingHeaders.isNotEmpty()) {
        item {
            GuideVoiceLanguageCard(
                headers = dubbingHeaders,
                selectedHeader = selectedDubbingHeader,
                backdrop = backdrop,
                onSelected = onSelectedVoiceLanguageChange
            )
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (!error.isNullOrBlank()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = Color(0x223B82F6),
                    contentColor = MiuixTheme.colorScheme.onBackground
                ),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = error.orEmpty(),
                        color = MiuixTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(10.dp)) }
    }

    if (voiceEntries.isNotEmpty()) {
        voiceEntries.forEachIndexed { index, entry ->
            val playbackUrl = resolveVoicePlaybackUrl(
                entry = entry,
                headers = voiceLanguageHeaders,
                selectedHeader = selectedDubbingHeader
            )
            val directPlaybackUrl = normalizeGuidePlaybackSource(playbackUrl)
            val resolvedCachedPlaybackUrl = galleryCacheRevision.let {
                BaGuideTempMediaCache.resolveCachedUrl(
                    context = context,
                    sourceUrl = sourceUrl,
                    rawUrl = directPlaybackUrl
                )
            }
            val normalizedPlaybackUrl = normalizeGuidePlaybackSource(resolvedCachedPlaybackUrl)
            val isCurrentPlayback = normalizedPlaybackUrl.isNotBlank() &&
                isVoicePlaying &&
                (normalizedPlaybackUrl == playingVoiceUrl || directPlaybackUrl == playingVoiceUrl)
            item {
                GuideVoiceEntryCard(
                    entry = entry,
                    languageHeaders = voiceLanguageHeaders,
                    backdrop = backdrop,
                    playbackUrl = normalizedPlaybackUrl,
                    isPlaying = isCurrentPlayback,
                    playProgress = if (isCurrentPlayback) {
                        voicePlayProgress
                    } else {
                        0f
                    },
                    onTogglePlay = onToggleVoicePlayback
                )
            }
            if (index < voiceEntries.lastIndex) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }
        }
    } else {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = Color(0x223B82F6),
                    contentColor = MiuixTheme.colorScheme.onBackground
                ),
                onClick = {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.guide_voice_empty),
                        color = MiuixTheme.colorScheme.onBackgroundVariant
                    )
                }
            }
        }
    }
}
