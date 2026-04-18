package com.example.keios.ui.page.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keios.mcp.McpServerManager
import com.example.keios.ui.page.main.widget.AppPageLazyColumn
import com.example.keios.ui.page.main.widget.AppPageScaffold
import com.example.keios.ui.page.main.widget.AppSurfaceCard
import com.example.keios.ui.page.main.widget.CopyModeSelectionContainer
import com.example.keios.ui.page.main.widget.copyModeAwareRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Ordered(val index: Int, val text: String) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
}

private data class SkillSection(
    val level: Int,
    val title: String,
    val items: List<SkillSectionItem>
)

private sealed interface SkillSectionItem {
    data class SubHeading(val level: Int, val text: String) : SkillSectionItem
    data class Paragraph(val text: String) : SkillSectionItem
    data class Bullet(val text: String) : SkillSectionItem
    data class Ordered(val index: Int, val text: String) : SkillSectionItem
    data class Code(val text: String) : SkillSectionItem
}

@Composable
fun McpSkillPage(
    mcpServerManager: McpServerManager,
    onBack: () -> Unit
) {
    val markdown by produceState(initialValue = "", mcpServerManager) {
        value = withContext(Dispatchers.IO) {
            mcpServerManager.getSkillMarkdown()
        }
    }
    val sections = remember(markdown) {
        val blocks = parseMarkdownBlocks(markdown.ifBlank { "# MCP Skill\n\n暂无内容" })
        buildSkillSections(blocks)
    }

    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val titleColor = MiuixTheme.colorScheme.onBackground
    val subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant
    val accentColor = MiuixTheme.colorScheme.primary
    val codeColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.10f)

    val subtitleVisibleTarget = if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 28) 1f else 0f
    val subtitleAlpha by animateFloatAsState(
        targetValue = subtitleVisibleTarget,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "skillSubtitleAlpha"
    )

    AppPageScaffold(
        title = "MCP Skill",
        modifier = Modifier.fillMaxSize(),
        scrollBehavior = scrollBehavior,
        topBarColor = MiuixTheme.colorScheme.surface,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Regular.Back,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        AppPageLazyColumn(
            innerPadding = innerPadding,
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topExtra = 0.dp,
            bottomExtra = 16.dp,
            sectionSpacing = 10.dp
        ) {
            item {
                SmallTitle(
                    modifier = Modifier.alpha(subtitleAlpha),
                    text = "SKILL.md"
                )
            }
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((10 * subtitleAlpha).dp)
                )
            }

            items(items = sections) { section ->
                SkillSectionCard(
                    section = section,
                    titleColor = titleColor,
                    subtitleColor = subtitleColor,
                    accentColor = accentColor,
                    codeColor = codeColor
                )
            }
        }
    }
}

@Composable
private fun SkillSectionCard(
    section: SkillSection,
    titleColor: androidx.compose.ui.graphics.Color,
    subtitleColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    codeColor: androidx.compose.ui.graphics.Color
) {
    val titleSize = when (section.level) {
        1 -> 20.sp
        2 -> 18.sp
        else -> 17.sp
    }

    AppSurfaceCard(
        containerColor = androidx.compose.ui.graphics.Color(0x223B82F6),
        contentColor = titleColor,
        showIndication = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = buildInlineStyledText(
                    text = section.title,
                    baseStyle = SpanStyle(color = titleColor, fontWeight = FontWeight.SemiBold),
                    accentStyle = SpanStyle(
                        color = accentColor,
                        background = accentColor.copy(alpha = 0.12f),
                        fontWeight = FontWeight.Medium
                    ),
                    linkStyle = SpanStyle(
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )
                ),
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleSize,
                lineHeight = (titleSize.value + 6f).sp
            )

            if (section.items.isEmpty()) {
                Text("暂无条目", color = subtitleColor)
            } else {
                section.items.forEachIndexed { index, item ->
                    SkillSectionItemView(
                        item = item,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        accentColor = accentColor,
                        codeColor = codeColor
                    )
                    if (index < section.items.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillSectionItemView(
    item: SkillSectionItem,
    titleColor: androidx.compose.ui.graphics.Color,
    subtitleColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    codeColor: androidx.compose.ui.graphics.Color
) {
    when (item) {
        is SkillSectionItem.SubHeading -> {
            val size = if (item.level <= 3) 16.sp else 15.sp
            CopyModeSelectionContainer {
                Text(
                    text = buildInlineStyledText(
                        text = item.text,
                        baseStyle = SpanStyle(color = titleColor, fontWeight = FontWeight.Medium),
                        accentStyle = SpanStyle(
                            color = accentColor,
                            background = accentColor.copy(alpha = 0.10f),
                            fontWeight = FontWeight.Medium
                        ),
                        linkStyle = SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ),
                    color = titleColor,
                    fontSize = size,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (size.value + 6f).sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .copyModeAwareRow(copyPayload = item.text)
                )
            }
        }

        is SkillSectionItem.Paragraph -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .copyModeAwareRow(copyPayload = item.text),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("·", color = subtitleColor, fontSize = 15.sp)
                CopyModeSelectionContainer {
                    Text(
                        text = buildInlineStyledText(
                            text = item.text,
                            baseStyle = SpanStyle(color = subtitleColor),
                            accentStyle = SpanStyle(
                                color = accentColor,
                                background = accentColor.copy(alpha = 0.10f),
                                fontWeight = FontWeight.Medium
                            ),
                            linkStyle = SpanStyle(
                                color = accentColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ),
                        color = subtitleColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        is SkillSectionItem.Bullet -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .copyModeAwareRow(copyPayload = item.text),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("•", color = subtitleColor, fontSize = 15.sp)
                CopyModeSelectionContainer {
                    Text(
                        text = buildInlineStyledText(
                            text = item.text,
                            baseStyle = SpanStyle(color = subtitleColor),
                            accentStyle = SpanStyle(
                                color = accentColor,
                                background = accentColor.copy(alpha = 0.10f),
                                fontWeight = FontWeight.Medium
                            ),
                            linkStyle = SpanStyle(
                                color = accentColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ),
                        color = subtitleColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        is SkillSectionItem.Ordered -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .copyModeAwareRow(copyPayload = item.text),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("${item.index}.", color = subtitleColor, fontSize = 15.sp)
                CopyModeSelectionContainer {
                    Text(
                        text = buildInlineStyledText(
                            text = item.text,
                            baseStyle = SpanStyle(color = subtitleColor),
                            accentStyle = SpanStyle(
                                color = accentColor,
                                background = accentColor.copy(alpha = 0.10f),
                                fontWeight = FontWeight.Medium
                            ),
                            linkStyle = SpanStyle(
                                color = accentColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ),
                        color = subtitleColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        is SkillSectionItem.Code -> {
            AppSurfaceCard(
                containerColor = codeColor,
                contentColor = titleColor,
                showIndication = false
            ) {
                CopyModeSelectionContainer {
                    Text(
                        text = item.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .copyModeAwareRow(copyPayload = item.text),
                        color = titleColor,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

private fun buildSkillSections(blocks: List<MarkdownBlock>): List<SkillSection> {
    if (blocks.isEmpty()) {
        return listOf(
            SkillSection(
                level = 1,
                title = "MCP Skill",
                items = listOf(SkillSectionItem.Paragraph("暂无内容"))
            )
        )
    }

    val sections = mutableListOf<SkillSection>()
    val currentItems = mutableListOf<SkillSectionItem>()
    var currentTitle = ""
    var currentLevel = 2

    fun ensureSectionStarted() {
        if (currentTitle.isBlank()) {
            currentTitle = "概览"
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
            title = currentTitle.ifBlank { "内容" },
            items = currentItems.toList()
        )
        currentItems.clear()
    }

    blocks.forEach { block ->
        when (block) {
            is MarkdownBlock.Heading -> {
                if (block.level <= 2) {
                    flushSection()
                    currentTitle = block.text.ifBlank { "未命名章节" }
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
                title = "MCP Skill",
                items = listOf(SkillSectionItem.Paragraph("暂无内容"))
            )
        )
    }
    return sections
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
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

            Regex("^\\d+\\.\\s+").containsMatchIn(trimmed) -> {
                flushParagraph()
                val match = Regex("^(\\d+)\\.\\s+(.*)$").find(trimmed)
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

private fun buildInlineStyledText(
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
                is InlineToken.Emphasis -> withStyle(baseStyle.copy(fontWeight = FontWeight.SemiBold)) { append(token.text) }
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

private sealed interface InlineToken {
    data class Plain(val text: String) : InlineToken
    data class Emphasis(val text: String) : InlineToken
    data class Code(val text: String) : InlineToken
    data class Link(val label: String, val url: String) : InlineToken
}

private fun parseInlineTokens(text: String): List<InlineToken> {
    val regex = Regex("`([^`]+)`|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|\\[(.+?)]\\((https?://[^)\\s]+)\\)")
    val tokens = mutableListOf<InlineToken>()
    var cursor = 0
    regex.findAll(text).forEach { match ->
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
