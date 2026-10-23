package os.kei.ui.page.main.github.page.action

import android.content.Context
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import os.kei.core.ext.showToast
import os.kei.core.privilege.PrivilegedShell
import os.kei.feature.github.domain.GitHubActionsService
import os.kei.feature.github.domain.GitHubTrackChangeSemanticUpdate
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.page.GitHubPageRepository
import os.kei.ui.page.main.github.page.GitHubPageState
import os.kei.ui.page.main.github.page.GitHubPageViewModel

internal class GitHubPageActionEnvironment(
    val context: Context,
    val scope: CoroutineScope,
    val durableScope: CoroutineScope,
    val state: GitHubPageState,
    val viewModel: GitHubPageViewModel,
    val repository: GitHubPageRepository,
    val privilegedShell: PrivilegedShell,
    val actionsRepository: GitHubActionsService = GitHubActionsService(),
    val openLinkFailureMessage: String,
    val clock: GitHubActionClock = GitHubSystemActionClock,
) {
    fun string(
        @StringRes resId: Int,
        vararg args: Any,
    ): String = context.getString(resId, *args)

    fun toast(
        @StringRes resId: Int,
        vararg args: Any,
    ) {
        context.showToast(string(resId, *args))
    }

    fun toast(message: String?) {
        if (message.isNullOrBlank()) return
        context.showToast(message)
    }

    fun saveTrackedItems(
        refreshTrackIds: Set<String> = emptySet(),
        emitStoreSignal: Boolean = true,
        trackChangeSource: GitHubTrackChangeHistorySource = GitHubTrackChangeHistorySource.Page,
        semanticTrackUpdates: List<GitHubTrackChangeSemanticUpdate> = emptyList(),
    ) {
        state.retainTrackedFirstInstallAtByTrackedItems()
        state.retainTrackedAddedAtByTrackedItems()
        state.retainTrackedModifiedAtByTrackedItems()
        val items = state.trackedItems.toList()
        val trackedFirstInstallAtByPackage = state.trackedFirstInstallAtByPackage.toMap()
        val trackedAddedAtById = state.trackedAddedAtById.toMap()
        val trackedModifiedAtById = state.trackedModifiedAtById.toMap()
        scope.launch {
            saveTrackedItemsAwaiting(
                items = items,
                trackedFirstInstallAtByPackage = trackedFirstInstallAtByPackage,
                trackedAddedAtById = trackedAddedAtById,
                trackedModifiedAtById = trackedModifiedAtById,
                refreshTrackIds = refreshTrackIds,
                emitStoreSignal = emitStoreSignal,
                trackChangeSource = trackChangeSource,
                semanticTrackUpdates = semanticTrackUpdates,
            )
        }
    }

    suspend fun saveTrackedItemsAwaiting(
        items: List<GitHubTrackedApp>,
        trackedFirstInstallAtByPackage: Map<String, Long>,
        trackedAddedAtById: Map<String, Long>,
        trackedModifiedAtById: Map<String, Long>,
        refreshTrackIds: Set<String> = emptySet(),
        emitStoreSignal: Boolean = true,
        trackChangeSource: GitHubTrackChangeHistorySource = GitHubTrackChangeHistorySource.Page,
        semanticTrackUpdates: List<GitHubTrackChangeSemanticUpdate> = emptyList(),
    ) {
        repository.saveTrackedItems(
            context = context,
            items = items,
            trackedFirstInstallAtByPackage = trackedFirstInstallAtByPackage,
            trackedAddedAtById = trackedAddedAtById,
            trackedModifiedAtById = trackedModifiedAtById,
            refreshTrackIds = refreshTrackIds,
            emitStoreSignal = emitStoreSignal,
            trackChangeSource = trackChangeSource,
            semanticTrackUpdates = semanticTrackUpdates,
        )
        viewModel.refreshHistoryUnreadCount()
    }
}
