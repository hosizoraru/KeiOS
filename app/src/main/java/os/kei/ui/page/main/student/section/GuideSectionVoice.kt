package os.kei.ui.page.main.student.section

import os.kei.ui.page.main.widget.glass.GlassVariant
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.student.BaGuideVoiceEntry
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.student.guideLocalizedVoiceEntryTitle
import os.kei.ui.page.main.student.guideLocalizedVoiceLanguage
import os.kei.ui.page.main.student.guideLocalizedVoiceLineLabel
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.glass.GlassTextButton
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GuideVoiceLanguageCard(
    headers: List<String>,
    selectedHeader: String,
    backdrop: Backdrop?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleHeaders = headers
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf("日配", "中配") }
    val visibleHeaderLabels = visibleHeaders.map { header -> header to guideLocalizedVoiceLanguage(header) }
    val dubbingLabel = stringResource(R.string.guide_voice_dubbing)
    val voiceHeaderCopyPayload = remember(visibleHeaderLabels, selectedHeader, dubbingLabel) {
        val current = selectedHeader.trim().ifBlank { visibleHeaders.firstOrNull().orEmpty() }
        val currentDisplay = visibleHeaderLabels
            .firstOrNull { (raw, _) -> raw.equals(current, ignoreCase = true) }
            ?.second
            .orEmpty()
        buildGuideCopyPayload(
            dubbingLabel,
            currentDisplay.ifBlank { visibleHeaderLabels.joinToString(" / ") { it.second } }
        )
    }
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0x223B82F6),
        contentColor = MiuixTheme.colorScheme.onBackground,
        onClick = {}
    ) {
        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideCopyable(voiceHeaderCopyPayload)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dubbingLabel,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.widthIn(min = 34.dp)
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleHeaderLabels.forEach { (header, headerLabel) ->
                        val selected = header.equals(selectedHeader.trim(), ignoreCase = true)
                        GlassTextButton(
                            backdrop = backdrop,
                            text = headerLabel,
                            textColor = if (selected) Color(0xFF2563EB) else MiuixTheme.colorScheme.onBackgroundVariant,
                            containerColor = if (selected) Color(0x443B82F6) else null,
                            variant = GlassVariant.Compact,
                            onClick = { onSelected(header) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuideVoiceEntryCard(
    entry: BaGuideVoiceEntry,
    languageHeaders: List<String>,
    backdrop: Backdrop?,
    playbackUrl: String,
    isPlaying: Boolean,
    playProgress: Float,
    onTogglePlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceLines = buildVoiceLinePairsForCard(entry, languageHeaders)
    val displaySection = guideLocalizedLabel(entry.section, R.string.guide_voice_dubbing)
    val displayTitle = guideLocalizedVoiceEntryTitle(entry.title)
        .ifBlank { stringResource(R.string.guide_voice_entry) }
    val displayVoiceLines = voiceLines.mapIndexed { index, line ->
        guideLocalizedVoiceLineLabel(line.first, index) to line.second
    }
    val entryCopyPayload = remember(displaySection, displayTitle, displayVoiceLines) {
        buildGuideVoiceEntryCopyPayload(
            section = displaySection,
            title = displayTitle,
            voiceLines = displayVoiceLines,
            fallbackSection = displaySection,
            fallbackTitle = displayTitle,
            fallbackLine = displayVoiceLines.firstOrNull()?.first.orEmpty()
        )
    }
    val normalizedPlaybackUrl = normalizeGuideMediaSource(playbackUrl)
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0x223B82F6),
        contentColor = MiuixTheme.colorScheme.onBackground,
        onClick = {}
    ) {
        CopyModeSelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideCopyable(entryCopyPayload)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.section.isNotBlank()) {
                        GlassTextButton(
                            backdrop = backdrop,
                            text = displaySection,
                            enabled = false,
                            textColor = Color(0xFF3B82F6),
                            variant = GlassVariant.Compact,
                            onClick = {}
                        )
                    }
                    Text(
                        text = displayTitle,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (normalizedPlaybackUrl.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPlaying) {
                                LiquidCircularProgressBar(
                                    progress = { playProgress.coerceIn(0f, 1f) },
                                    size = 18.dp,
                                    strokeWidth = 2.dp,
                                    activeColor = Color(0xFF3B82F6),
                                    inactiveColor = Color(0x553B82F6)
                                )
                            }
                            GlassTextButton(
                                backdrop = backdrop,
                                text = "",
                                leadingIcon = if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                                textColor = Color(0xFF3B82F6),
                                variant = GlassVariant.Compact,
                                onClick = { onTogglePlay(normalizedPlaybackUrl) }
                            )
                        }
                    }
                }

                displayVoiceLines.forEach { (label, text) ->
                    val lineCopyPayload = remember(label, text) {
                        buildGuideCopyPayload(label, text)
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val labelMaxWidth = (maxWidth * 0.28f).coerceIn(52.dp, 92.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .guideCopyable(lineCopyPayload),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = label,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                modifier = Modifier.widthIn(max = labelMaxWidth),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                text = text,
                                color = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun canonicalVoiceLineLabelInCard(raw: String): String {
    val normalized = raw
        .replace(" ", "")
        .replace("　", "")
        .lowercase()
        .trim()
    if (normalized.isBlank()) return ""
    return when {
        normalized.contains("官翻") || normalized.contains("官方翻译") || normalized.contains("官方中文") || normalized.contains("官中") -> "官翻"
        normalized.contains("韩") || normalized.contains("kr") || normalized.contains("kor") || normalized.contains("korean") -> "韩配"
        normalized.contains("中") || normalized.contains("cn") || normalized.contains("国语") || normalized.contains("国配") || normalized.contains("中文") -> "中配"
        normalized.contains("日") || normalized.contains("jp") || normalized.contains("jpn") || normalized.contains("日本") -> "日配"
        else -> raw.trim()
    }
}

internal fun voiceLinePriorityForCard(label: String): Int {
    return when (canonicalVoiceLineLabelInCard(label)) {
        "日配" -> 0
        "中配" -> 1
        "官翻" -> 2
        "韩配" -> 3
        else -> 4
    }
}

internal fun buildVoiceLinePairsForCard(
    entry: BaGuideVoiceEntry,
    fallbackHeaders: List<String>
): List<Pair<String, String>> {
    val explicitPairs = if (
        entry.lineHeaders.size == entry.lines.size &&
        entry.lineHeaders.any { it.trim().isNotBlank() }
    ) {
        entry.lineHeaders.zip(entry.lines)
    } else {
        emptyList()
    }
    val rawPairs = if (explicitPairs.isNotEmpty()) {
        explicitPairs
    } else {
        entry.lines.mapIndexed { index, line ->
            val label = fallbackHeaders.getOrNull(index).orEmpty()
            label to line
        }
    }
    return rawPairs.withIndex()
        .mapNotNull { indexed ->
            val label = indexed.value.first.trim()
            val text = indexed.value.second.trim()
            if (text.isBlank()) return@mapNotNull null
            val normalizedLabel = canonicalVoiceLineLabelInCard(label).ifBlank {
                label
            }
            Triple(normalizedLabel, text, indexed.index)
        }
        .sortedWith(
            compareBy<Triple<String, String, Int>> { item ->
                voiceLinePriorityForCard(item.first)
            }.thenBy { item ->
                item.third
            }
        )
        .map { item -> item.first to item.second }
}
