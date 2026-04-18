package com.example.keios.ui.page.main

import com.example.keios.R
import com.example.keios.feature.github.data.remote.GitHubReleaseAssetFile
import com.example.keios.feature.github.data.remote.GitHubShareImportResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GitHubShareImportActions(
    private val env: GitHubPageActionEnvironment,
    private val assetActions: GitHubAssetActions
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state

    fun handleIncomingGitHubShareText(sharedText: String) {
        val normalizedText = sharedText.trim()
        if (normalizedText.isBlank()) return
        if (state.shareImportResolving) return

        scope.launch {
            state.shareImportResolving = true
            runCatching {
                withContext(Dispatchers.IO) {
                    GitHubShareImportResolver.resolve(
                        sharedText = normalizedText,
                        lookupConfig = state.lookupConfig
                    ).getOrThrow()
                }
            }.onSuccess { plan ->
                if (plan.assets.isEmpty()) {
                    env.toast(R.string.github_toast_share_import_no_apk)
                } else {
                    state.pendingShareImportPreview = GitHubShareImportPreview(
                        sourceUrl = plan.parsedLink.sourceUrl,
                        projectUrl = plan.parsedLink.projectUrl,
                        owner = plan.parsedLink.owner,
                        repo = plan.parsedLink.repo,
                        releaseTag = plan.resolvedReleaseTag,
                        releaseUrl = plan.resolvedReleaseUrl,
                        strategyLabel = state.lookupConfig.selectedStrategy.label,
                        assets = plan.assets,
                        preferredAssetName = plan.preferredAssetName
                    )
                }
            }.onFailure { error ->
                env.toast(
                    context.getString(
                        R.string.github_toast_share_import_failed,
                        error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
                    )
                )
            }
            state.shareImportResolving = false
        }
    }

    fun dismissShareImportDialog() {
        if (state.shareImportResolving) return
        state.pendingShareImportPreview = null
    }

    fun confirmShareImportSelection(asset: GitHubReleaseAssetFile) {
        val preview = state.pendingShareImportPreview ?: return
        state.pendingShareImportPreview = null
        state.pendingShareImportTrack = GitHubPendingShareImportTrack(
            projectUrl = preview.projectUrl,
            owner = preview.owner,
            repo = preview.repo
        )

        val hasOnlineShareTarget = state.lookupConfig.onlineShareTargetPackage.isNotBlank()
        if (hasOnlineShareTarget) {
            assetActions.shareApkLink(asset)
        } else {
            assetActions.openApkInDownloader(asset)
        }
        env.toast(R.string.github_toast_share_import_wait_install, asset.name)
    }
}
