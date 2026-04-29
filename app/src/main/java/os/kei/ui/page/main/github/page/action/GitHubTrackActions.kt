package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.defaultKeiOsTrackedApp
import os.kei.ui.page.main.github.page.GitHubTrackEditorDraft
import os.kei.ui.page.main.github.page.GitHubTrackEditorResult

internal class GitHubTrackActions(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions
) {
    private val state get() = env.state
    private val scope get() = env.scope
    private val repository get() = env.repository

    fun openTrackSheetForAdd() {
        state.resetTrackEditor()
        state.showAddSheet = true
    }

    fun openTrackSheetForEdit(item: GitHubTrackedApp) {
        state.editingTrackedItem = item
        state.repoUrlInput = item.repoUrl
        state.packageNameInput = item.packageName
        state.selectedApp = state.appList.firstOrNull {
            it.packageName.equals(item.packageName, ignoreCase = true)
        }
        state.appSearch = ""
        state.pickerExpanded = false
        state.preferPreReleaseInput = item.preferPreRelease
        state.alwaysShowLatestReleaseDownloadButtonInput = item.alwaysShowLatestReleaseDownloadButton
        state.showAddSheet = true
    }

    fun dismissTrackSheet() {
        state.dismissTrackSheet()
    }

    fun ensureKeiOsSelfTrack() {
        val newItem = defaultKeiOsTrackedApp()
        if (state.trackedItems.any { it.id == newItem.id }) {
            env.toast(R.string.github_toast_track_exists)
            return
        }
        state.trackedItems.add(newItem)
        state.recordTrackedAddedAt(newItem.id, System.currentTimeMillis())
        env.saveTrackedItems(refreshTrackIds = setOf(newItem.id))
        env.toast(R.string.github_toast_track_current_app_added)
    }

    fun requestDeleteEditingItem() {
        state.pendingDeleteItem = state.editingTrackedItem
        state.dismissTrackSheet()
    }

    fun requestDeleteItem(item: GitHubTrackedApp) {
        if (state.deleteInProgress) return
        if (state.trackedItems.none { it.id == item.id }) return
        state.pendingDeleteItem = item
    }

    fun applyTrackSheet() {
        val draft = GitHubTrackEditorDraft(
            repoUrl = state.repoUrlInput,
            packageName = state.packageNameInput,
            preferPreRelease = state.preferPreReleaseInput,
            alwaysShowLatestReleaseDownloadButton = state.alwaysShowLatestReleaseDownloadButtonInput,
            appList = state.appList
        )
        scope.launch {
            val nowMillis = System.currentTimeMillis()
            val newItem = when (val result = repository.buildTrackedItem(draft)) {
                GitHubTrackEditorResult.InvalidRepository -> {
                    env.toast(R.string.github_toast_fill_repo_and_select_app)
                    return@launch
                }
                GitHubTrackEditorResult.InvalidPackageName -> {
                    env.toast(R.string.github_toast_invalid_package_name)
                    return@launch
                }
                is GitHubTrackEditorResult.Ready -> result.item
            }
            val editing = state.editingTrackedItem
            if (editing == null) {
                if (state.trackedItems.any { it.id == newItem.id }) {
                    env.toast(R.string.github_toast_track_exists)
                    return@launch
                }
                state.trackedItems.add(newItem)
                state.recordTrackedAddedAt(newItem.id, nowMillis)
                env.saveTrackedItems(refreshTrackIds = setOf(newItem.id))
                env.toast(R.string.github_toast_track_added)
            } else {
                val duplicate = state.trackedItems.any { it.id == newItem.id && it.id != editing.id }
                if (duplicate) {
                    env.toast(R.string.github_toast_track_exists)
                    return@launch
                }
                val existingAddedAt = state.trackedAddedAtById[editing.id]
                    ?.takeIf { it > 0L }
                    ?: state.trackedAddedAtById[newItem.id]
                    ?.takeIf { it > 0L }
                    ?: nowMillis
                val index = state.trackedItems.indexOfFirst { it.id == editing.id }
                if (index >= 0) {
                    state.trackedItems[index] = newItem
                } else {
                    state.trackedItems.add(newItem)
                }
                if (editing.id != newItem.id) {
                    state.checkStates.remove(editing.id)
                    state.trackedCardExpanded.remove(editing.id)
                    state.clearAssetUiState(editing.id)
                    state.trackedAddedAtById.remove(editing.id)
                }
                state.recordTrackedAddedAt(newItem.id, existingAddedAt)
                env.saveTrackedItems(refreshTrackIds = setOf(newItem.id))
                env.toast(R.string.github_toast_track_updated)
            }
            state.dismissTrackSheet()
        }
    }

    fun confirmDeletePendingItem() {
        if (state.deleteInProgress) return
        state.pendingDeleteItem?.let { deleting ->
            state.deleteInProgress = true
            try {
                refreshActions.cancelRefreshAll()
                state.trackedItems.remove(deleting)
                state.checkStates.remove(deleting.id)
                state.trackedCardExpanded.remove(deleting.id)
                state.trackedAddedAtById.remove(deleting.id)
                state.clearAssetUiState(deleting.id)
                env.saveTrackedItems()
                refreshActions.persistCheckCache()
                env.toast(R.string.github_toast_track_deleted, deleting.appLabel)
            } finally {
                state.deleteInProgress = false
            }
        }
        state.pendingDeleteItem = null
    }
}
