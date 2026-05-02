package os.kei.ui.page.main.student.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.student.BaGuideMetaItem
import os.kei.ui.page.main.student.GuideRemoteIcon
import os.kei.ui.page.main.student.component.GuideLiquidCard
import os.kei.ui.page.main.student.guideLocalizedLabel
import os.kei.ui.page.main.student.guideLocalizedValue
import os.kei.ui.page.main.widget.support.CopyModeSelectionContainer
import os.kei.ui.page.main.widget.support.buildTextCopyPayload
import os.kei.ui.page.main.widget.support.copyModeAwareRow
import os.kei.ui.page.main.widget.support.rememberLightTextCopyAction
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun adaptiveValueMaxLines(value: String, lineCharBudget: Int): Int {
    val normalized = value.trim()
    if (normalized.isBlank()) return 1
    if (normalized.contains('\n')) return 2
    val budget = lineCharBudget.coerceIn(10, 52)
    val breakable = normalized.any {
        it == ' ' || it == '/' || it == '-' || it == ',' || it == '，' || it == '、' || it == ':' || it == '：'
    }
    val veryLong = normalized.length > budget + budget / 2
    val longAndBreakable = breakable && normalized.length > budget
    return if (veryLong || longAndBreakable) 2 else 1
}

internal fun buildGuideCopyPayload(key: String, value: String): String {
    return buildTextCopyPayload(key, value)
}

internal fun buildGuideSkillCopyPayload(
    name: String,
    skillType: String,
    level: String,
    cost: String,
    desc: String,
    stateTags: List<String>,
    variantBadge: String?,
    fallbackSkill: String,
    statusLabel: String
): String {
    val headerParts = buildList {
        add(name.ifBlank { fallbackSkill })
        skillType.trim().takeIf { it.isNotBlank() }?.let(::add)
        variantBadge?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        level.trim().takeIf { it.isNotBlank() }?.let(::add)
        cost.trim().takeIf { it.isNotBlank() }?.let { add("COST:$it") }
    }
    val stateText = stateTags
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" / ")
    return buildString {
        append(headerParts.joinToString(" · ").ifBlank { fallbackSkill })
        if (stateText.isNotBlank()) {
            append('\n')
            append(statusLabel)
            append('：')
            append(stateText)
        }
        if (desc.isNotBlank()) {
            append('\n')
            append(desc.trim())
        }
    }
}

internal fun buildGuideVoiceEntryCopyPayload(
    section: String,
    title: String,
    voiceLines: List<Pair<String, String>>,
    fallbackSection: String,
    fallbackTitle: String,
    fallbackLine: String
): String {
    val header = buildString {
        append(section.trim().ifBlank { fallbackSection })
        append(" · ")
        append(title.trim().ifBlank { fallbackTitle })
    }
    val lines = voiceLines
        .mapNotNull { (label, text) ->
            val lineText = text.trim()
            if (lineText.isBlank()) null else "${label.trim().ifBlank { fallbackLine }}：$lineText"
        }
    if (lines.isEmpty()) return header
    return buildString {
        append(header)
        append('\n')
        append(lines.joinToString("\n"))
    }
}

internal fun buildGuideWeaponCopyPayload(
    name: String,
    level: String,
    desc: String,
    fallbackName: String
): String {
    return buildString {
        append(name.trim().ifBlank { fallbackName })
        level.trim().takeIf { it.isNotBlank() }?.let {
            append(" · ")
            append(it)
        }
        if (desc.isNotBlank()) {
            append('\n')
            append(desc.trim())
        }
    }
}

@Composable
internal fun rememberGuideCopyAction(copyPayload: String): () -> Unit {
    val quickCopyAction = rememberLightTextCopyAction(copyPayload)
    return remember(quickCopyAction) {
        { quickCopyAction?.invoke() }
    }
}

internal fun Modifier.guideCopyable(
    copyPayload: String,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    this.copyModeAwareRow(
        copyPayload = copyPayload,
        onClick = onClick
    )
}

@Composable
fun GuideProfileMetaLine(item: BaGuideMetaItem) {
    val isPosition = item.title == "位置"
    val isRarity = item.title == "稀有度"
    val inlineTitleIcon = isRarity || item.title == "学院"
    val summary = if (isPosition) "" else guideLocalizedValue(item.value).ifBlank { "-" }
    val displayTitle = guideLocalizedLabel(item.title)
    val rowCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(displayTitle, summary))
    val iconSlotWidth = 34.dp
    val iconSlotHeight = 24.dp
    val iconWidth = when {
        isRarity -> 30.dp
        isPosition -> 30.dp
        else -> 20.dp
    }
    val iconHeight = when {
        isRarity -> 24.dp
        else -> 20.dp
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val titleMaxWidth = if (inlineTitleIcon) {
            (maxWidth * 0.34f).coerceIn(68.dp, 136.dp)
        } else {
            (maxWidth * 0.42f).coerceIn(80.dp, 176.dp)
        }

        CopyModeSelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .copyModeAwareRow(
                        copyPayload = buildGuideCopyPayload(displayTitle, summary),
                        onLongClick = rowCopyAction
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (inlineTitleIcon) {
                    Row(
                        modifier = Modifier.widthIn(max = titleMaxWidth),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        if (item.imageUrl.isNotBlank()) {
                            GuideRemoteIcon(
                                imageUrl = item.imageUrl,
                                iconWidth = iconWidth,
                                iconHeight = iconHeight
                            )
                        }
                    }
                } else {
                    val hasLeadingIcon = item.imageUrl.isNotBlank()
                    Row(
                        modifier = Modifier.widthIn(max = titleMaxWidth),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        if (hasLeadingIcon) {
                            Box(
                                modifier = Modifier
                                    .width(iconSlotWidth)
                                    .height(iconSlotHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                GuideRemoteIcon(
                                    imageUrl = item.imageUrl,
                                    iconWidth = iconWidth,
                                    iconHeight = iconHeight
                                )
                            }
                        }
                    }
                }
                if (!isPosition) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = summary,
                            color = MiuixTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Clip
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GuideCombatMetaTile(
    item: BaGuideMetaItem,
    modifier: Modifier = Modifier
) {
    val value = guideLocalizedValue(item.value).ifBlank { "-" }
    val displayTitle = guideLocalizedLabel(item.title)
    val rowCopyAction = rememberGuideCopyAction(buildGuideCopyPayload(displayTitle, value))
    val adaptiveWide = item.title.contains("战术") || item.title == "武器类型"
    val iconWidth = if (adaptiveWide) 28.dp else 18.dp
    val iconHeight = if (adaptiveWide) 18.dp else 18.dp
    val extraIconWidth = 30.dp
    val extraIconHeight = 18.dp
    val iconSlotWidth = 30.dp
    val iconSlotHeight = 22.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val trailingSlotsWidth = if (item.extraImageUrl.isNotBlank()) iconSlotWidth * 2 else iconSlotWidth
        val titleMaxWidth = ((maxWidth - trailingSlotsWidth) * if (adaptiveWide) 0.52f else 0.46f)
            .coerceIn(86.dp, 180.dp)
        val valueCharBudget = ((maxWidth - titleMaxWidth - trailingSlotsWidth).value / 7.2f)
            .toInt()
            .coerceAtLeast(10)
        val valueMaxLines = adaptiveValueMaxLines(value, valueCharBudget)

        GuideLiquidCard(
            cornerRadius = 12.dp,
            surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.26f),
            isInteractive = false,
            shadow = false
        ) {
            CopyModeSelectionContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .copyModeAwareRow(
                            copyPayload = buildGuideCopyPayload(displayTitle, value),
                            onLongClick = rowCopyAction
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.widthIn(max = titleMaxWidth),
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = value,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = valueMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .width(iconSlotWidth)
                            .height(iconSlotHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.imageUrl.isNotBlank()) {
                            GuideRemoteIcon(
                                imageUrl = item.imageUrl,
                                iconWidth = iconWidth,
                                iconHeight = iconHeight
                            )
                        }
                    }
                    if (item.extraImageUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .width(iconSlotWidth)
                                .height(iconSlotHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            GuideRemoteIcon(
                                imageUrl = item.extraImageUrl,
                                iconWidth = extraIconWidth,
                                iconHeight = extraIconHeight
                            )
                        }
                    }
                }
            }
        }
    }
}
