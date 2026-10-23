package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.ensureActive
import os.kei.feature.github.domain.GitHubTrackChangeSemanticUpdate
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.coerceForSource
import os.kei.feature.github.model.hasSameGitHubTrackingConfigIgnoringLocalAppType
import os.kei.ui.page.main.github.page.GitHubPageState
import kotlin.coroutines.coroutineContext

internal enum class TrackEditorSaveResult { Saved, Duplicate, Stale }

internal data class TrackEditorSavePlan(
    val items: List<GitHubTrackedApp>,
    val firstInstallAt: Map<String, Long>,
    val addedAt: Map<String, Long>,
    val modifiedAt: Map<String, Long>,
    val refreshTrackIds: Set<String>,
    val semanticUpdates: List<GitHubTrackChangeSemanticUpdate>,
)

internal suspend fun persistTrackEditorSave(
    state: GitHubPageState,
    expectedToken: Long,
    editing: GitHubTrackedApp?,
    newItem: GitHubTrackedApp,
    nowMillis: Long,
    persist: suspend (TrackEditorSavePlan) -> Unit,
): TrackEditorSaveResult {
    if (!state.trackDataReady || !state.showAddSheet || state.editorSessionToken != expectedToken) {
        return TrackEditorSaveResult.Stale
    }
    if (editing != null && state.trackedItems.none { it.id == editing.id }) {
        return TrackEditorSaveResult.Stale
    }
    if (state.trackedItems.any { it.id == newItem.id && it.id != editing?.id }) {
        return TrackEditorSaveResult.Duplicate
    }
    val items = if (editing == null) {
        state.trackedItems.toList() + newItem
    } else {
        state.trackedItems.map { if (it.id == editing.id) newItem else it }
    }
    val validIds = items.mapTo(mutableSetOf()) { it.id }
    val validPackages = items.mapTo(mutableSetOf()) { it.packageName.trim() }
    val addedAt = state.trackedAddedAtById.toMutableMap()
    val modifiedAt = state.trackedModifiedAtById.toMutableMap()
    val existingAddedAt = editing?.let { addedAt[it.id]?.takeIf { value -> value > 0L } }
        ?: addedAt[newItem.id]?.takeIf { it > 0L }
        ?: nowMillis
    if (editing != null && editing.id != newItem.id) {
        addedAt.remove(editing.id)
        modifiedAt.remove(editing.id)
    }
    addedAt[newItem.id] = existingAddedAt
    if (editing != newItem) modifiedAt[newItem.id] = nowMillis
    val trackingChanged = editing == null ||
        !editing.hasSameGitHubTrackingConfigIgnoringLocalAppType(newItem)
    val plan = TrackEditorSavePlan(
        items = items,
        firstInstallAt = state.trackedFirstInstallAtByPackage.filterKeys { it in validPackages },
        addedAt = addedAt.filterKeys { it in validIds },
        modifiedAt = modifiedAt.filterKeys { it in validIds },
        refreshTrackIds = if (trackingChanged) setOf(newItem.id) else emptySet(),
        semanticUpdates = if (editing != null && editing != newItem) {
            listOf(GitHubTrackChangeSemanticUpdate(previous = editing, next = newItem))
        } else emptyList(),
    )
    // No optimistic list entry or timestamp survives a failed/cancelled write.
    persist(plan)
    coroutineContext.ensureActive()
    state.trackedItems.clear()
    state.trackedItems.addAll(plan.items)
    state.trackedFirstInstallAtByPackage.clear()
    state.trackedFirstInstallAtByPackage.putAll(plan.firstInstallAt)
    state.trackedAddedAtById.clear()
    state.trackedAddedAtById.putAll(plan.addedAt)
    state.trackedModifiedAtById.clear()
    state.trackedModifiedAtById.putAll(plan.modifiedAt)
    state.completeTrackEditorSave(expectedToken)
    return TrackEditorSaveResult.Saved
}

internal fun GitHubPageState.replaceTrackEditorForAdd() {
    replaceTrackEditor {
        resetTrackEditor()
        showAddSheet = true
    }
}

internal fun GitHubPageState.replaceTrackEditorForEdit(item: GitHubTrackedApp) {
    replaceTrackEditor {
        resetTrackEditor()
        editingTrackedItem = item
        repoUrlInput = item.repoUrl
        packageNameInput = item.packageName
        selectedApp =
            appList.firstOrNull {
                it.packageName.equals(item.packageName, ignoreCase = true)
            }
        appSearch = ""
        pickerExpanded = false
        repoScanCandidates = emptyList()
        trackSourceModeInput = item.sourceMode
        preferPreReleaseInput = item.preferPreRelease
        alwaysShowLatestReleaseDownloadButtonInput = item.alwaysShowLatestReleaseDownloadButton
        checkActionsUpdatesInput = item.checkActionsUpdates
        externalBuildUntilReleaseInput = item.externalBuildUntilRelease
        updateIntervalModeInput = item.updateIntervalMode.coerceForSource(item.sourceMode)
        actionsUpdateIntervalModeInput = item.actionsUpdateIntervalMode
        preciseApkVersionModeInput = item.preciseApkVersionMode
        ignoreModeInput = item.ignoreMode
        ignoredStableReleaseKeyInput = item.ignoredStableReleaseKey
        ignoredPreReleaseKeyInput = item.ignoredPreReleaseKey
        fdroidVersionSelectionModeInput = item.fdroidConfig.selectionMode
        fdroidVersionNameRegexInput = item.fdroidConfig.versionNameRegex
        fdroidApkNameRegexInput = item.fdroidConfig.apkNameRegex
        fdroidTrustPolicyInput = item.fdroidConfig.trustPolicy
        fdroidAntiFeaturePolicyInput = item.fdroidConfig.antiFeaturePolicy
        fdroidRepoScopeIdInput = item.fdroidConfig.repoPresetId
            .ifBlank {
                FdroidRepositoryPresets.presetForRepoUrl(item.repoUrl)?.id.orEmpty()
            }
            .ifBlank { FdroidRepositoryPresets.CUSTOM_ID }
        fdroidAppSearchQueryInput = item.appLabel
        fdroidAppSearchCandidates = emptyList()
        fdroidAppSearchFailures = emptyList()
        fdroidAppSearchFailuresExpanded = false
        fdroidAppSearchRepoReports = emptyList()
        fdroidSelectedCandidate = null
        fdroidAppSearchRunning = false
        showAddSheet = true
    }
}
