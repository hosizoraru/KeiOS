package os.kei.ui.page.main.mcp.skill

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.mcp.skill.model.SkillSection
import os.kei.ui.page.main.mcp.skill.support.buildSkillSections
import os.kei.ui.page.main.mcp.skill.support.parseMarkdownBlocks

internal data class McpSkillPageContentRequest(
    val emptyMarkdown: String,
    val defaultRootTitle: String,
    val defaultOverviewTitle: String,
    val defaultContentTitle: String,
    val emptyContentText: String
)

internal class McpSkillPageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun loadSections(
        manager: McpServerManager,
        request: McpSkillPageContentRequest
    ): List<SkillSection> {
        val markdown = withContext(ioDispatcher) {
            manager.getSkillMarkdown()
        }
        return withContext(defaultDispatcher) {
            val blocks = parseMarkdownBlocks(markdown.ifBlank { request.emptyMarkdown })
            buildSkillSections(
                blocks = blocks,
                defaultRootTitle = request.defaultRootTitle,
                defaultOverviewTitle = request.defaultOverviewTitle,
                defaultContentTitle = request.defaultContentTitle,
                emptyContentText = request.emptyContentText
            )
        }
    }
}
