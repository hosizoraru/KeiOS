package os.kei.ui.page.main.github.actions

import com.tencent.mmkv.MMKV

internal data class GitHubActionsSectionExpansionState(
    val branchesExpanded: Boolean = false,
    val workflowsExpanded: Boolean = false,
    val runsExpanded: Boolean = false
)

internal object GitHubActionsUiStateStore {
    private const val KV_ID = "github_actions_ui_state"
    private const val KEY_BRANCHES_EXPANDED = "branches_expanded"
    private const val KEY_WORKFLOWS_EXPANDED = "workflows_expanded"
    private const val KEY_RUNS_EXPANDED = "runs_expanded"

    private val store: MMKV by lazy { MMKV.mmkvWithID(KV_ID) }

    fun loadSectionExpansionState(): GitHubActionsSectionExpansionState {
        return GitHubActionsSectionExpansionState(
            branchesExpanded = store.decodeBool(KEY_BRANCHES_EXPANDED, false),
            workflowsExpanded = store.decodeBool(KEY_WORKFLOWS_EXPANDED, false),
            runsExpanded = store.decodeBool(KEY_RUNS_EXPANDED, false)
        )
    }

    fun setBranchesExpanded(value: Boolean) {
        store.encode(KEY_BRANCHES_EXPANDED, value)
    }

    fun setWorkflowsExpanded(value: Boolean) {
        store.encode(KEY_WORKFLOWS_EXPANDED, value)
    }

    fun setRunsExpanded(value: Boolean) {
        store.encode(KEY_RUNS_EXPANDED, value)
    }
}
