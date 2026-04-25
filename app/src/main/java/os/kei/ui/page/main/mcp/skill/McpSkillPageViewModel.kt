package os.kei.ui.page.main.mcp.skill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import os.kei.mcp.server.McpServerManager
import os.kei.ui.page.main.mcp.skill.state.McpSkillPageContentState

internal class McpSkillPageViewModel : ViewModel() {
    private val repository = McpSkillPageRepository()
    private val _contentState = MutableStateFlow(McpSkillPageContentState())
    val contentState: StateFlow<McpSkillPageContentState> = _contentState.asStateFlow()

    private var loadJob: Job? = null
    private var lastRequest: McpSkillPageContentRequest? = null

    fun loadContent(
        manager: McpServerManager,
        request: McpSkillPageContentRequest
    ) {
        if (lastRequest == request && _contentState.value.sections.isNotEmpty()) return
        if (lastRequest == request && loadJob?.isActive == true) return
        lastRequest = request
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val sections = repository.loadSections(manager, request)
            _contentState.value = McpSkillPageContentState(sections = sections)
        }
    }
}
