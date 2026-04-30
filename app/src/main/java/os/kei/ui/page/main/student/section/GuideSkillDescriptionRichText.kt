package os.kei.ui.page.main.student.section

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.R
import os.kei.ui.page.main.student.GuideRemoteIcon
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SkillDescriptionRichText(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
internal fun GuideSkillDescriptionText(
    description: String,
    glossaryIcons: Map<String, String>,
    descriptionIcons: List<String> = emptyList(),
    onLineCountChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val textColor = MiuixTheme.colorScheme.onBackground
    val iconAltText = stringResource(R.string.guide_skill_icon_alt)
    val richText = remember(description, glossaryIcons, descriptionIcons, textColor, iconAltText) {
        buildSkillDescriptionRichText(
            description = description,
            glossaryIcons = glossaryIcons,
            leadingIcons = descriptionIcons,
            numberColor = Color(0xFFD84A40),
            iconAltText = iconAltText
        )
    }
    BasicText(
        text = richText.text,
        inlineContent = richText.inlineContent,
        onTextLayout = { layoutResult ->
            onLineCountChanged?.invoke(layoutResult.lineCount)
        },
        style = TextStyle(
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        modifier = modifier
    )
}

internal fun buildSkillDescriptionRichText(
    description: String,
    glossaryIcons: Map<String, String>,
    leadingIcons: List<String>,
    numberColor: Color,
    iconAltText: String
): SkillDescriptionRichText {
    if (description.isBlank()) {
        return SkillDescriptionRichText(AnnotatedString(""), emptyMap())
    }

    val numberRegex = Regex("""(?<![A-Za-z])[-+]?\d+(?:\.\d+)?%?""")
    val glossary = glossaryIcons
        .filter { (label, icon) -> label.isNotBlank() && icon.isNotBlank() }
        .entries
        .sortedByDescending { it.key.length }

    val inlineContent = linkedMapOf<String, InlineTextContent>()
    var inlineCounter = 0
    val normalizedDescription = normalizeGlossaryToken(description)
    val fuzzyLeadingIcons = glossary
        .asSequence()
        .filter { entry ->
            val label = entry.key
            if (description.contains(label)) return@filter false
            val normalizedLabel = normalizeGlossaryToken(label)
            normalizedLabel.isNotBlank() && normalizedDescription.contains(normalizedLabel)
        }
        .map { it.value }
        .distinct()
        .take(6)
        .toList()
    val prefixIcons = (leadingIcons + fuzzyLeadingIcons)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)
    val text = buildAnnotatedString {
        prefixIcons
            .forEach { iconUrl ->
                val inlineId = "skill_icon_prefix_$inlineCounter"
                inlineCounter += 1
                inlineContent[inlineId] = InlineTextContent(
                    Placeholder(
                        width = 15.sp,
                        height = 15.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 15.dp,
                        iconHeight = 15.dp
                    )
                }
                appendInlineContent(inlineId, iconAltText)
                append(" ")
            }
        var index = 0
        while (index < description.length) {
            val glossaryMatch = glossary
                .mapNotNull { entry ->
                    val start = description.indexOf(entry.key, index)
                    if (start < 0) null else Triple(start, entry.key, entry.value)
                }
                .minByOrNull { it.first }
            val numberMatch = numberRegex.find(description, index)

            val nextIsGlossary = when {
                glossaryMatch == null -> false
                numberMatch == null -> true
                else -> glossaryMatch.first <= numberMatch.range.first
            }

            if (glossaryMatch == null && numberMatch == null) {
                append(description.substring(index))
                break
            }

            if (nextIsGlossary) {
                val (start, label, iconUrl) = glossaryMatch ?: continue
                if (start > index) {
                    append(description.substring(index, start))
                }
                val inlineId = "skill_icon_$inlineCounter"
                inlineCounter += 1
                inlineContent[inlineId] = InlineTextContent(
                    Placeholder(
                        width = 15.sp,
                        height = 15.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    GuideRemoteIcon(
                        imageUrl = iconUrl,
                        iconWidth = 15.dp,
                        iconHeight = 15.dp
                    )
                }
                appendInlineContent(inlineId, iconAltText)
                append(label)
                index = start + label.length
            } else {
                val number = numberMatch ?: continue
                if (number.range.first > index) {
                    append(description.substring(index, number.range.first))
                }
                withStyle(SpanStyle(color = numberColor, fontWeight = FontWeight.SemiBold)) {
                    append(number.value)
                }
                index = number.range.last + 1
            }
        }
    }
    return SkillDescriptionRichText(text, inlineContent)
}

internal fun normalizeGlossaryToken(raw: String): String {
    return raw
        .replace(Regex("""[\s\u3000]"""), "")
        .replace(Regex("""[，。、“”‘’：:；;（）()【】\[\]《》<>·•\-—_+*/\\|!?！？]"""), "")
        .lowercase()
}
