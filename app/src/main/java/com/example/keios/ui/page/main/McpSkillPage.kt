package com.example.keios.ui.page.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
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
    val blocks = remember(markdown) {
        parseMarkdownBlocks(markdown.ifBlank { "# MCP Skill\n\n暂无内容" })
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "MCP Skill",
                scrollBehavior = scrollBehavior,
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 12.dp,
                end = 12.dp
            )
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

            items(items = blocks) { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val size = when (block.level) {
                            1 -> 22.sp
                            2 -> 19.sp
                            3 -> 17.sp
                            else -> 16.sp
                        }
                        Text(
                            text = buildInlineStyledText(
                                text = block.text,
                                baseStyle = SpanStyle(color = titleColor, fontWeight = FontWeight.SemiBold),
                                accentStyle = SpanStyle(
                                    color = accentColor,
                                    fontWeight = FontWeight.Medium,
                                    background = accentColor.copy(alpha = 0.12f)
                                ),
                                linkStyle = SpanStyle(
                                    color = accentColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            ),
                            color = titleColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = size,
                            lineHeight = (size.value + 7f).sp
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = buildInlineStyledText(
                                text = block.text,
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
                            lineHeight = 22.sp
                        )
                    }

                    is MarkdownBlock.Bullet -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("•", color = subtitleColor, fontSize = 15.sp)
                            Text(
                                text = buildInlineStyledText(
                                    text = block.text,
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
                                lineHeight = 22.sp
                            )
                        }
                    }

                    is MarkdownBlock.Ordered -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${block.index}.", color = subtitleColor, fontSize = 15.sp)
                            Text(
                                text = buildInlineStyledText(
                                    text = block.text,
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
                                lineHeight = 22.sp
                            )
                        }
                    }

                    is MarkdownBlock.Code -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.defaultColors(
                                color = codeColor,
                                contentColor = titleColor
                            )
                        ) {
                            Text(
                                text = block.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(codeColor)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                color = titleColor,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
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
