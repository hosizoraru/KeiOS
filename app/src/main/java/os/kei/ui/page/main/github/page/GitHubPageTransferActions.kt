package os.kei.ui.page.main.github.page

import android.content.Context
import android.content.Intent
import android.net.Uri
import os.kei.core.ext.showToast
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.ui.page.main.github.importer.GitHubStarImportActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
internal data class GitHubPageTrackTransferCallbacks(
    val onExportTrackedItems: () -> Unit,
    val onImportTrackedItems: () -> Unit,
    val onOpenStarImport: () -> Unit,
    val onConfirmTrackImport: () -> Unit
)

internal fun handleGitHubStarImportActivityResult(
    result: ActivityResult,
    actions: GitHubPageActions
) {
    val importResult = GitHubStarImportActivity.parseResult(
        resultCode = result.resultCode,
        data = result.data
    ) ?: return
    actions.handleTrackMutationRefresh(
        affectedTrackIds = importResult.affectedTrackIds,
        removedTrackIds = importResult.removedTrackIds
    )
}

internal fun handleGitHubTrackExportDestinationResult(
    context: Context,
    scope: CoroutineScope,
    githubPageViewModel: GitHubPageViewModel,
    uri: Uri?
) {
    val request = githubPageViewModel.consumePendingExport()
    if (uri == null || request == null) {
        githubPageViewModel.finishTrackedExport()
        return
    }
    scope.launch {
        val result = runCatching {
            githubPageViewModel.writeExport(
                contentResolver = context.contentResolver,
                uri = uri,
                request = request
            )
        }
        githubPageViewModel.finishTrackedExport()
        result.onSuccess {
            context.showToast(R.string.github_toast_track_exported)
        }.onFailure {
            context.showToast(context.getString(
                R.string.github_toast_track_export_failed,
                it.javaClass.simpleName
            ))
        }
    }
}

internal fun handleGitHubTrackImportSourceResult(
    context: Context,
    scope: CoroutineScope,
    state: GitHubPageState,
    actions: GitHubPageActions,
    githubPageViewModel: GitHubPageViewModel,
    uri: Uri?
) {
    if (uri == null) {
        githubPageViewModel.finishTrackedImport()
        return
    }
    scope.launch {
        val result = runCatching {
            val raw = githubPageViewModel.readImport(
                contentResolver = context.contentResolver,
                uri = uri
            )
            actions.previewTrackedItemsImport(raw)
        }
        githubPageViewModel.finishTrackedImport()
        result.onSuccess { preview ->
            state.pendingTrackImportPreview = preview
        }.onFailure {
            context.showToast(context.getString(
                R.string.github_toast_track_import_failed,
                it.javaClass.simpleName
            ))
        }
    }
}

internal fun buildGitHubPageTrackTransferCallbacks(
    context: Context,
    scope: CoroutineScope,
    state: GitHubPageState,
    actions: GitHubPageActions,
    githubPageViewModel: GitHubPageViewModel,
    launchTrackedExport: (String) -> Unit,
    launchTrackedImport: () -> Unit,
    launchStarImport: (Intent) -> Unit
): GitHubPageTrackTransferCallbacks {
    val exportFileNameFormatter = DateTimeFormatter.ofPattern("yyMMdd-HHmm", Locale.getDefault())
    return GitHubPageTrackTransferCallbacks(
        onExportTrackedItems = {
            scope.launch {
                val exportedAtMillis = System.currentTimeMillis()
                val exportFileName = buildString {
                    append("keios-github-tracks-")
                    append(LocalDateTime.now().format(exportFileNameFormatter))
                    append(".json")
                }
                when (
                    val result = githubPageViewModel.beginTrackedExport(
                        items = state.trackedItems.toList(),
                        exportedAtMillis = exportedAtMillis,
                        fileName = exportFileName
                    )
                ) {
                    GitHubTrackedExportStartResult.Busy -> Unit
                    GitHubTrackedExportStartResult.Empty -> {
                        context.showToast(R.string.github_toast_require_track_item)
                    }

                    is GitHubTrackedExportStartResult.Failed -> {
                        context.showToast(context.getString(
                            R.string.github_toast_track_export_failed,
                            result.reason ?: result.javaClass.simpleName
                        ))
                    }

                    GitHubTrackedExportStartResult.Ready -> {
                        runCatching {
                            launchTrackedExport(exportFileName)
                        }.onFailure {
                            githubPageViewModel.finishTrackedExport()
                            context.showToast(context.getString(
                                R.string.github_toast_track_export_failed,
                                it.javaClass.simpleName
                            ))
                        }
                    }
                }
            }
        },
        onImportTrackedItems = {
            when (githubPageViewModel.beginTrackedImport()) {
                GitHubTrackedImportStartResult.Busy -> Unit
                GitHubTrackedImportStartResult.Ready -> {
                    runCatching {
                        launchTrackedImport()
                    }.onFailure {
                        githubPageViewModel.finishTrackedImport()
                        context.showToast(context.getString(
                            R.string.github_toast_track_import_failed,
                            it.javaClass.simpleName
                        ))
                    }
                }
            }
        },
        onOpenStarImport = {
            runCatching {
                launchStarImport(GitHubStarImportActivity.buildIntent(context))
            }.onFailure {
                GitHubStarImportActivity.launch(context)
            }
        },
        onConfirmTrackImport = {
            val preview = state.pendingTrackImportPreview
            if (preview != null) {
                when (githubPageViewModel.beginTrackedImport()) {
                    GitHubTrackedImportStartResult.Busy -> Unit
                    GitHubTrackedImportStartResult.Ready -> {
                        scope.launch {
                            val result = runCatching { actions.applyTrackedItemsImport(preview) }
                            githubPageViewModel.finishTrackedImport()
                            result.onSuccess { importResult ->
                                state.dismissTrackImportPreview()
                                val effectiveCount = importResult.addedCount +
                                        importResult.updatedCount +
                                        importResult.unchangedCount
                                if (effectiveCount == 0) {
                                    context.showToast(R.string.github_toast_track_import_no_valid)
                                } else {
                                    context.showToast(context.getString(
                                        R.string.github_toast_track_imported_summary,
                                        importResult.addedCount,
                                        importResult.updatedCount,
                                        importResult.unchangedCount,
                                        importResult.invalidCount + importResult.duplicateCount
                                    ))
                                }
                            }.onFailure {
                                context.showToast(context.getString(
                                    R.string.github_toast_track_import_failed,
                                    it.javaClass.simpleName
                                ))
                            }
                        }
                    }
                }
            }
        }
    )
}
