package os.kei.appwidget.github

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.local.GitHubTrackStoreSignals
import os.kei.core.concurrency.AppDispatchers

internal object GitHubWidgetPublisher {
    fun start(
        context: Context,
        scope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher = AppDispatchers.fileIo,
    ) {
        scope.launch {
            GitHubTrackStoreSignals.persistedSummaryVersion.collectLatest {
                delay(250L)
                withContext(ioDispatcher) {
                    try {
                        if (GitHubWidgetUpdater.ids(context).isNotEmpty()) publish(context)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.w("GitHubWidget", "Cannot publish tracking summary", error)
                    }
                }
            }
        }
    }

    // Only the main process reads domain stores. The receiver reads the small on-disk projection.
    fun publish(context: Context): GitHubWidgetSnapshot {
        val snapshot = projectGitHubWidget(GitHubTrackStore.loadLocalSummarySnapshot())
        GitHubWidgetStateStore(context).writeSnapshot(snapshot)
        GitHubWidgetUpdater.updateAll(context)
        return snapshot
    }
}
