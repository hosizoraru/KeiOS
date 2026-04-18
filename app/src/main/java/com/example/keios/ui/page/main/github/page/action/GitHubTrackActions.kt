package com.example.keios.ui.page.main

import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubVersionUtils
import com.example.keios.feature.github.model.GitHubTrackedApp
import com.example.keios.feature.github.model.InstalledAppItem

internal class GitHubTrackActions(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions
) {
    private val state get() = env.state

    fun openTrackSheetForAdd() {
        state.resetTrackEditor()
        state.showAddSheet = true
    }

    fun openTrackSheetForEdit(item: GitHubTrackedApp) {
        state.editingTrackedItem = item
        state.repoUrlInput = item.repoUrl
        state.selectedApp = state.appList.firstOrNull { it.packageName == item.packageName }
            ?: InstalledAppItem(label = item.appLabel, packageName = item.packageName)
        state.appSearch = ""
        state.pickerExpanded = false
        state.preferPreReleaseInput = item.preferPreRelease
        state.alwaysShowLatestReleaseDownloadButtonInput = item.alwaysShowLatestReleaseDownloadButton
        state.showAddSheet = true
    }

    fun dismissTrackSheet() {
        state.dismissTrackSheet()
    }

    fun requestDeleteEditingItem() {
        state.pendingDeleteItem = state.editingTrackedItem
        state.dismissTrackSheet()
    }

    fun applyTrackSheet() {
        val app = state.selectedApp
        val parsed = GitHubVersionUtils.parseOwnerRepo(state.repoUrlInput)
        if (app == null || parsed == null) {
            env.toast(R.string.github_toast_fill_repo_and_select_app)
            return
        }
        val newItem = GitHubTrackedApp(
            repoUrl = state.repoUrlInput.trim(),
            owner = parsed.first,
            repo = parsed.second,
            packageName = app.packageName,
            appLabel = app.label,
            preferPreRelease = state.preferPreReleaseInput,
            alwaysShowLatestReleaseDownloadButton = state.alwaysShowLatestReleaseDownloadButtonInput
        )
        val editing = state.editingTrackedItem
        if (editing == null) {
            if (state.trackedItems.any { it.id == newItem.id }) {
                env.toast(R.string.github_toast_track_exists)
                return
            }
            state.trackedItems.add(newItem)
            env.saveTrackedItems()
            refreshActions.refreshItem(newItem, showToastOnError = true)
            env.toast(R.string.github_toast_track_added)
        } else {
            val duplicate = state.trackedItems.any { it.id == newItem.id && it.id != editing.id }
            if (duplicate) {
                env.toast(R.string.github_toast_track_exists)
                return
            }
            val index = state.trackedItems.indexOfFirst { it.id == editing.id }
            if (index >= 0) {
                state.trackedItems[index] = newItem
            } else {
                state.trackedItems.add(newItem)
            }
            if (editing.id != newItem.id) {
                state.checkStates.remove(editing.id)
            }
            env.saveTrackedItems()
            refreshActions.refreshItem(newItem, showToastOnError = true)
            env.toast(R.string.github_toast_track_updated)
        }
        state.dismissTrackSheet()
    }

    fun confirmDeletePendingItem() {
        if (state.deleteInProgress) return
        state.pendingDeleteItem?.let { deleting ->
            state.deleteInProgress = true
            try {
                refreshActions.cancelRefreshAll()
                state.trackedItems.remove(deleting)
                state.checkStates.remove(deleting.id)
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
