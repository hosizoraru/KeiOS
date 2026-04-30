package os.kei.ui.page.main.github.page.action

import android.content.Intent
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.core.download.AppPrivateDownloadManager
import os.kei.core.intent.SafeExternalIntents
import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsWorkflowMatch
import os.kei.ui.page.main.github.localizedGitHubActionsErrorMessage
import os.kei.ui.page.main.github.page.GitHubActionsPageRepository

internal class GitHubActionsArtifactActions(
    private val env: GitHubPageActionEnvironment,
    private val actionsRepository: GitHubActionsPageRepository,
    private val onDownloadHistoryChanged: suspend () -> Unit
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state
    private val systemDmOption get() = env.systemDmOption

    fun downloadActionsArtifact(runId: Long, artifactId: Long) {
        val item = state.actionsTargetItem ?: return
        val workflowMatch = selectedWorkflowMatch() ?: return
        val runMatch = state.actionsRuns.firstOrNull { it.runArtifacts.run.id == runId } ?: return
        val artifactMatch = runMatch.artifactMatches.firstOrNull { it.artifact.id == artifactId } ?: return
        val artifact = artifactMatch.artifact
        if (state.lookupConfig.actionsRequireApiToken && state.lookupConfig.apiToken.trim().isBlank()) {
            env.toast(R.string.github_actions_toast_token_required)
            return
        }
        if (artifact.expired) {
            env.toast(R.string.github_actions_toast_artifact_expired)
            return
        }
        if (!runMatch.traits.completed) {
            env.toast(R.string.github_actions_toast_wait_run_completed)
            return
        }
        if (
            state.actionsArtifactDownloadLoadingId == artifact.id ||
            state.actionsArtifactShareLoadingId == artifact.id
        ) {
            return
        }
        scope.launch {
            state.actionsArtifactDownloadLoadingId = artifact.id
            try {
                val resolution = actionsRepository.resolveGitHubActionsArtifactDownloadUrl(
                    artifact = artifact,
                    owner = item.owner,
                    repo = item.repo,
                    lookupConfig = state.lookupConfig,
                    preferApiTokenRedirect = true
                ).getOrThrow()
                val resolvedUrl = SafeExternalIntents.httpsExternalUrlOrNull(resolution.downloadUrl)
                    ?: error(context.getString(R.string.github_actions_error_download_url_invalid))
                val fileName = artifactArchiveFileName(artifact)
                if (openResolvedArtifactDownloadUrl(resolvedUrl, fileName)) {
                    val record = actionsRepository.buildGitHubActionsDownloadRecord(
                        owner = item.owner,
                        repo = item.repo,
                        workflow = workflowMatch.workflow,
                        run = runMatch.runArtifacts.run,
                        artifact = artifact,
                        sourceTrackId = item.id,
                        packageName = item.packageName
                    )
                    actionsRepository.recordGitHubActionsArtifactDownload(record)
                    state.actionsDownloadHistory = actionsRepository.loadGitHubActionsDownloadHistory(
                        owner = item.owner,
                        repo = item.repo
                    )
                    onDownloadHistoryChanged()
                    env.toast(R.string.github_actions_toast_download_started)
                }
            } catch (error: Throwable) {
                env.toast(
                    context.getString(
                        R.string.github_actions_toast_download_failed,
                        localizedGitHubActionsErrorMessage(
                            context = context,
                            rawMessage = error.message ?: error.javaClass.simpleName
                        )
                    )
                )
            } finally {
                state.actionsArtifactDownloadLoadingId = null
            }
        }
    }

    fun shareActionsArtifact(runId: Long, artifactId: Long) {
        val item = state.actionsTargetItem ?: return
        val runMatch = state.actionsRuns.firstOrNull { it.runArtifacts.run.id == runId } ?: return
        val artifactMatch = runMatch.artifactMatches.firstOrNull { it.artifact.id == artifactId } ?: return
        val artifact = artifactMatch.artifact
        if (state.lookupConfig.actionsRequireApiToken && state.lookupConfig.apiToken.trim().isBlank()) {
            env.toast(R.string.github_actions_toast_token_required)
            return
        }
        if (artifact.expired) {
            env.toast(R.string.github_actions_toast_artifact_expired)
            return
        }
        if (!runMatch.traits.completed) {
            env.toast(R.string.github_actions_toast_wait_run_completed)
            return
        }
        if (
            state.actionsArtifactShareLoadingId == artifact.id ||
            state.actionsArtifactDownloadLoadingId == artifact.id
        ) {
            return
        }
        scope.launch {
            state.actionsArtifactShareLoadingId = artifact.id
            try {
                val resolution = actionsRepository.resolveGitHubActionsArtifactShareUrl(
                    artifact = artifact,
                    owner = item.owner,
                    repo = item.repo,
                    lookupConfig = state.lookupConfig
                ).getOrThrow()
                val resolvedUrl = SafeExternalIntents.httpsExternalUrlOrNull(resolution.downloadUrl)
                    ?: error(context.getString(R.string.github_actions_error_download_url_invalid))
                if (shareResolvedArtifactDownloadUrl(resolvedUrl, artifact.name)) {
                    env.toast(R.string.github_actions_toast_share_started)
                }
            } catch (error: Throwable) {
                env.toast(
                    context.getString(
                        R.string.github_actions_toast_share_failed,
                        localizedGitHubActionsErrorMessage(
                            context = context,
                            rawMessage = error.message ?: error.javaClass.simpleName
                        )
                    )
                )
            } finally {
                state.actionsArtifactShareLoadingId = null
            }
        }
    }

    fun openSelectedActionsRun() {
        val run = state.actionsRuns
            .firstOrNull { it.runArtifacts.run.id == state.actionsSelectedRunId }
            ?.runArtifacts
            ?.run
            ?: return
        val url = run.htmlUrl.trim()
        if (url.isBlank()) return
        if (!SafeExternalIntents.startBrowsableUrl(context, url)) {
            env.toast(R.string.github_error_open_link)
        }
    }

    private fun selectedWorkflowMatch(): GitHubActionsWorkflowMatch? {
        val workflowId = state.actionsSelectedWorkflowId ?: return null
        return state.actionsWorkflows.firstOrNull { it.workflow.id == workflowId }
    }

    private fun openResolvedArtifactDownloadUrl(url: String, fileName: String): Boolean {
        val preferredPackage = state.lookupConfig.preferredDownloaderPackage.trim()
        return runCatching {
            when (preferredPackage) {
                systemDmOption.packageName -> {
                    enqueueWithSystemDownloadManager(url = url, fileName = fileName)
                    env.toast(R.string.github_toast_downloader_system_builtin)
                }
                "" -> {
                    require(SafeExternalIntents.startBrowsableUrl(context, url))
                    env.toast(R.string.github_toast_downloader_system_default)
                }
                else -> {
                    require(SafeExternalIntents.startBrowsableUrl(context, url, preferredPackage))
                    env.toast(R.string.github_toast_downloader_selected)
                }
            }
            true
        }.recoverCatching {
            if (preferredPackage.isNotBlank() && preferredPackage != systemDmOption.packageName) {
                require(SafeExternalIntents.startBrowsableUrl(context, url))
                env.toast(R.string.github_toast_downloader_fallback_system)
                true
            } else {
                throw it
            }
        }.getOrElse {
            env.toast(R.string.github_toast_open_downloader_failed)
            false
        }
    }

    private fun shareResolvedArtifactDownloadUrl(url: String, artifactName: String): Boolean {
        val onlineSharePackage = state.lookupConfig.onlineShareTargetPackage.trim()
        val intent = SafeExternalIntents.textShareIntent(
            text = url,
            subject = artifactName,
            targetPackage = onlineSharePackage,
            extras = if (onlineSharePackage.isNotBlank()) {
                mapOf(
                    "channel" to "Online",
                    "extra_channel" to "Online",
                    "online_channel" to true
                )
            } else {
                emptyMap()
            }
        )
        return runCatching {
            if (onlineSharePackage.isNotBlank()) {
                context.startActivity(intent)
            } else {
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.getString(R.string.github_actions_share_artifact_title)
                    )
                )
            }
            true
        }.getOrElse {
            env.toast(R.string.github_toast_share_link_failed)
            false
        }
    }

    private fun enqueueWithSystemDownloadManager(url: String, fileName: String) {
        AppPrivateDownloadManager.enqueueHttpsDownload(
            context = context,
            url = url,
            fileName = fileName,
            mimeType = "application/zip"
        )
    }

    private fun artifactArchiveFileName(artifact: GitHubActionsArtifact): String {
        val baseName = artifact.name
            .trim()
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .ifBlank { "artifact-${artifact.id}" }
        return if (baseName.endsWith(".zip", ignoreCase = true)) baseName else "$baseName.zip"
    }
}
