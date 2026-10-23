package os.kei.ui.page.main.github.page

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optBoolean
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.state.EditorRecoverySlot
import os.kei.core.state.EditorRecoveryStore
import os.kei.feature.github.data.local.parseTrackedItem
import os.kei.feature.github.data.local.trackedItemToJson
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAppSearchSource
import os.kei.feature.github.model.FdroidTrustPolicy
import os.kei.feature.github.model.FdroidVersionSelectionMode
import os.kei.feature.github.model.GitHubTrackedActionsUpdateIntervalMode
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedPreciseApkVersionMode
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode

internal class GitHubEditorRecovery(private val store: EditorRecoveryStore) {
    private var last: String? = null

    fun restore(state: GitHubPageState): Boolean {
        val raw = store.load(EditorRecoverySlot.GitHubTrack) ?: return false
        val record = raw.parseJsonObjectOrNull() ?: return false
        val editing = record.optObject("editing")?.let(::parseTrackedItem)
        // An invalid edit target must never silently turn into "add a new subscription".
        if (record.containsKey("editing") && editing == null) return false
        state.editingTrackedItem = editing
        state.repoUrlInput = record.optString("url")
        state.packageNameInput = record.optString("package")
        state.trackSourceModeInput = GitHubTrackedSourceMode.fromStorageId(record.optString("source"))
        state.preferPreReleaseInput = record.optBoolean("preRelease")
        state.alwaysShowLatestReleaseDownloadButtonInput = record.optBoolean("alwaysDownload")
        state.checkActionsUpdatesInput = record.optBoolean("actions")
        state.externalBuildUntilReleaseInput = record.optBoolean("external")
        state.updateIntervalModeInput = GitHubTrackedUpdateIntervalMode.fromStorageId(record.optString("interval"))
        state.actionsUpdateIntervalModeInput =
            GitHubTrackedActionsUpdateIntervalMode.fromStorageId(record.optString("actionsInterval"))
        state.preciseApkVersionModeInput =
            GitHubTrackedPreciseApkVersionMode.fromStorageId(record.optString("precise"))
        state.ignoreModeInput = GitHubTrackedIgnoreMode.fromStorageId(record.optString("ignore"))
        state.ignoredStableReleaseKeyInput = record.optString("ignoredStable")
        state.ignoredPreReleaseKeyInput = record.optString("ignoredPre")
        state.fdroidVersionSelectionModeInput =
            FdroidVersionSelectionMode.fromStorageId(record.optString("fdroidVersion"))
        state.fdroidVersionNameRegexInput = record.optString("versionRegex")
        state.fdroidApkNameRegexInput = record.optString("apkRegex")
        state.fdroidTrustPolicyInput = FdroidTrustPolicy.fromStorageId(record.optString("trust"))
        state.fdroidAntiFeaturePolicyInput = FdroidAntiFeaturePolicy.fromStorageId(record.optString("antiFeatures"))
        state.fdroidRepoScopeIdInput = record.optString("scope")
        state.fdroidAppSearchQueryInput = record.optString("query")
        state.fdroidSelectedCandidate = record.optObject("candidate")?.let { candidate ->
            FdroidAppSearchCandidate(
                repoUrl = candidate.optString("url"),
                repoDisplayName = candidate.optString("repoName"),
                repoPresetId = candidate.optString("preset"),
                packageName = candidate.optString("package"),
                appName = candidate.optString("name"),
                summary = "",
                packagePageUrl = candidate.optString("page"),
                source = FdroidAppSearchSource.RepositoryIndex,
            )
        }
        last = encodeGitHubEditorRecovery(state)
        return true
    }

    fun save(state: GitHubPageState) {
        val raw = if (state.showAddSheet) encodeGitHubEditorRecovery(state) else null
        if (raw == last) return
        store.save(EditorRecoverySlot.GitHubTrack, raw)
        last = raw
    }
}

private fun encodeGitHubEditorRecovery(state: GitHubPageState): String = buildJsonObject {
    put("url", state.repoUrlInput)
    put("package", state.packageNameInput)
    put("source", state.trackSourceModeInput.storageId)
    put("preRelease", state.preferPreReleaseInput)
    put("alwaysDownload", state.alwaysShowLatestReleaseDownloadButtonInput)
    put("actions", state.checkActionsUpdatesInput)
    put("external", state.externalBuildUntilReleaseInput)
    put("interval", state.updateIntervalModeInput.storageId)
    put("actionsInterval", state.actionsUpdateIntervalModeInput.storageId)
    put("precise", state.preciseApkVersionModeInput.storageId)
    put("ignore", state.ignoreModeInput.storageId)
    put("ignoredStable", state.ignoredStableReleaseKeyInput)
    put("ignoredPre", state.ignoredPreReleaseKeyInput)
    put("fdroidVersion", state.fdroidVersionSelectionModeInput.storageId)
    put("versionRegex", state.fdroidVersionNameRegexInput)
    put("apkRegex", state.fdroidApkNameRegexInput)
    put("trust", state.fdroidTrustPolicyInput.storageId)
    put("antiFeatures", state.fdroidAntiFeaturePolicyInput.storageId)
    put("scope", state.fdroidRepoScopeIdInput)
    put("query", state.fdroidAppSearchQueryInput)
    state.editingTrackedItem?.let { put("editing", trackedItemToJson(it)) }
    state.fdroidSelectedCandidate?.let { candidate ->
        put("candidate", buildJsonObject {
            put("url", candidate.repoUrl)
            put("repoName", candidate.repoDisplayName)
            put("preset", candidate.repoPresetId)
            put("package", candidate.packageName)
            put("name", candidate.appName)
            put("page", candidate.packagePageUrl)
        })
    }
}.encodeCompact()
