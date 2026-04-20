package com.example.keios.ui.page.main.mcp.skill.support

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.keios.ui.page.main.mcp.skill.model.InlineToken
import com.example.keios.ui.page.main.mcp.skill.model.MarkdownBlock
import com.example.keios.ui.page.main.mcp.skill.model.SkillSection
import com.example.keios.ui.page.main.mcp.skill.model.SkillSectionItem

private val ORDERED_LIST_PREFIX_REGEX = Regex("^\\d+\\.\\s+")
private val ORDERED_LIST_CONTENT_REGEX = Regex("^(\\d+)\\.\\s+(.*)$")
private val INLINE_TOKEN_REGEX = Regex(
    "`([^`]+)`|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|\\[(.+?)]\\((https?://[^)\\s]+)\\)"
)

internal fun buildSkillSections(
    blocks: List<MarkdownBlock>,
    defaultRootTitle: String,
    defaultOverviewTitle: String,
    defaultContentTitle: String,
    emptyContentText: String
): List<SkillSection> {
    if (blocks.isEmpty()) {
        return listOf(
            SkillSection(
                level = 1,
                title = defaultRootTitle,
                items = listOf(SkillSectionItem.Paragraph(emptyContentText))
            )
        )
    }

    val sections = mutableListOf<SkillSection>()
    val currentItems = mutableListOf<SkillSectionItem>()
    var currentTitle = ""
    var currentLevel = 2

    fun ensureSectionStarted() {
        if (currentTitle.isBlank()) {
            currentTitle = defaultOverviewTitle
            currentLevel = 2
        }
    }

    fun flushSection() {
        if (currentTitle.isBlank() && currentItems.isEmpty()) return
        if (currentItems.isEmpty() && currentLevel == 1 && sections.isEmpty()) {
            currentTitle = ""
            return
        }
        if (currentItems.isEmpty() && sections.isNotEmpty()) {
            currentTitle = ""
            return
        }
        sections += SkillSection(
            level = currentLevel,
            title = currentTitle.ifBlank { defaultContentTitle },
            items = currentItems.toList()
        )
        currentItems.clear()
    }

    blocks.forEach { block ->
        when (block) {
            is MarkdownBlock.Heading -> {
                if (block.level <= 2) {
                    flushSection()
                    currentTitle = block.text.ifBlank { defaultContentTitle }
                    currentLevel = block.level
                } else {
                    ensureSectionStarted()
                    currentItems += SkillSectionItem.SubHeading(block.level, block.text)
                }
            }

            is MarkdownBlock.Paragraph -> {
                ensureSectionStarted()
                currentItems += SkillSectionItem.Paragraph(block.text)
            }

            is MarkdownBlock.Bullet -> {
                ensureSectionStarted()
                currentItems += SkillSectionItem.Bullet(block.text)
            }

            is MarkdownBlock.Ordered -> {
                ensureSectionStarted()
                currentItems += SkillSectionItem.Ordered(block.index, block.text)
            }

            is MarkdownBlock.Code -> {
                ensureSectionStarted()
                currentItems += SkillSectionItem.Code(block.text)
            }
        }
    }

    flushSection()
    if (sections.isEmpty()) {
        return listOf(
            SkillSection(
                level = 1,
                title = defaultRootTitle,
                items = listOf(SkillSectionItem.Paragraph(emptyContentText))
            )
        )
    }
    return sections
}

internal fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = mutableListOf<String>()
    val codeBuffer = mutableListOf<String>()
    var inCode = false

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            val text = paragraphBuffer.joinToString(" ").trim()
            if (text.isNotBlank()) blocks += MarkdownBlock.Paragraph(text)
            paragraphBuffer.clear()
        }
    }

    fun flushCode() {
        if (codeBuffer.isNotEmpty()) {
            blocks += MarkdownBlock.Code(codeBuffer.joinToString("\n").trimEnd())
            codeBuffer.clear()
        }
    }

    lines.forEach { raw ->
        val line = raw.trimEnd()
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            if (inCode) {
                flushCode()
                inCode = false
            } else {
                inCode = true
            }
            return@forEach
        }

        if (inCode) {
            codeBuffer += line
            return@forEach
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            return@forEach
        }

        when {
            trimmed.startsWith("#### ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(4, trimmed.removePrefix("#### ").trim())
            }

            trimmed.startsWith("### ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim())
            }

            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim())
            }

            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim())
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Bullet(trimmed.drop(2).trim())
            }

            ORDERED_LIST_PREFIX_REGEX.containsMatchIn(trimmed) -> {
                flushParagraph()
                val match = ORDERED_LIST_CONTENT_REGEX.find(trimmed)
                val index = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                val text = match?.groupValues?.getOrNull(2).orEmpty()
                blocks += MarkdownBlock.Ordered(index, text)
            }

            else -> paragraphBuffer += trimmed
        }
    }

    flushParagraph()
    flushCode()
    return blocks
}

internal fun buildInlineStyledText(
    text: String,
    baseStyle: SpanStyle,
    accentStyle: SpanStyle,
    linkStyle: SpanStyle
): AnnotatedString {
    val tokens = parseInlineTokens(text)
    return buildAnnotatedString {
        tokens.forEach { token ->
            when (token) {
                is InlineToken.Plain -> withStyle(baseStyle) { append(token.text) }
                is InlineToken.Emphasis -> withStyle(baseStyle.copy(fontWeight = FontWeight.SemiBold)) {
                    append(token.text)
                }

                is InlineToken.Code -> withStyle(accentStyle) { append(" ${token.text} ") }
                is InlineToken.Link -> {
                    withStyle(linkStyle) { append(token.label) }
                    withStyle(baseStyle.copy(color = baseStyle.color.copy(alpha = 0.72f))) {
                        append(" (${token.url})")
                    }
                }
            }
        }
    }
}

internal fun parseInlineTokens(text: String): List<InlineToken> {
    val tokens = mutableListOf<InlineToken>()
    var cursor = 0
    INLINE_TOKEN_REGEX.findAll(text).forEach { match ->
        val range = match.range
        if (range.first > cursor) {
            tokens += InlineToken.Plain(text.substring(cursor, range.first))
        }
        val code = match.groups[1]?.value
        val strong = match.groups[2]?.value
        val em = match.groups[3]?.value
        val label = match.groups[4]?.value
        val url = match.groups[5]?.value
        when {
            !code.isNullOrBlank() -> tokens += InlineToken.Code(code)
            !strong.isNullOrBlank() -> tokens += InlineToken.Emphasis(strong)
            !em.isNullOrBlank() -> tokens += InlineToken.Emphasis(em)
            !label.isNullOrBlank() && !url.isNullOrBlank() -> tokens += InlineToken.Link(label, url)
            else -> tokens += InlineToken.Plain(match.value)
        }
        cursor = range.last + 1
    }
    if (cursor < text.length) {
        tokens += InlineToken.Plain(text.substring(cursor))
    }
    return if (tokens.isEmpty()) listOf(InlineToken.Plain(text)) else tokens
}
