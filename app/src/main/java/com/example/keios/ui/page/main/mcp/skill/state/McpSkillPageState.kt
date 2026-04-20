package com.example.keios.ui.page.main.mcp.skill.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.example.keios.mcp.server.McpServerManager
import com.example.keios.ui.page.main.mcp.skill.model.SkillSection
import com.example.keios.ui.page.main.mcp.skill.support.buildSkillSections
import com.example.keios.ui.page.main.mcp.skill.support.parseMarkdownBlocks
import com.example.keios.ui.page.main.widget.motion.appMotionFloatState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Stable
internal data class McpSkillPageContentState(
    val sections: List<SkillSection>,
    val subtitleAlpha: Float
)

@Composable
internal fun rememberMcpSkillPageContentState(
    mcpServerManager: McpServerManager,
    listState: LazyListState,
    emptyMarkdown: String,
    defaultRootTitle: String,
    defaultOverviewTitle: String,
    defaultContentTitle: String,
    emptyContentText: String
): McpSkillPageContentState {
    val markdown by produceState(initialValue = "", mcpServerManager) {
        value = withContext(Dispatchers.IO) {
            mcpServerManager.getSkillMarkdown()
        }
    }
    val sections by produceState(
        initialValue = emptyList<SkillSection>(),
        markdown,
        emptyMarkdown,
        defaultRootTitle,
        defaultOverviewTitle,
        defaultContentTitle,
        emptyContentText
    ) {
        value = withContext(Dispatchers.Default) {
            val blocks = parseMarkdownBlocks(markdown.ifBlank { emptyMarkdown })
            buildSkillSections(
                blocks = blocks,
                defaultRootTitle = defaultRootTitle,
                defaultOverviewTitle = defaultOverviewTitle,
                defaultContentTitle = defaultContentTitle,
                emptyContentText = emptyContentText
            )
        }
    }

    val subtitleVisibleTarget = if (
        listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 28
    ) {
        1f
    } else {
        0f
    }
    val subtitleAlpha by appMotionFloatState(
        targetValue = subtitleVisibleTarget,
        durationMillis = 220,
        label = "skillSubtitleAlpha"
    )

    return remember(sections, subtitleAlpha) {
        McpSkillPageContentState(
            sections = sections,
            subtitleAlpha = subtitleAlpha
        )
    }
}
