package os.kei.ui.page.main.mcp.skill.state

import androidx.compose.runtime.Stable
import os.kei.ui.page.main.mcp.skill.model.SkillSection

@Stable
internal data class McpSkillPageContentState(
    val sections: List<SkillSection> = emptyList()
)
