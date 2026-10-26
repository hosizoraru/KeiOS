package os.kei.ui.page.main.sync

internal data class WebDavSyncPlan(
    val kind: WebDavBatchKind,
    val scope: WebDavSyncPlanScope,
    val createdAtMs: Long,
    val items: List<WebDavSyncPlanItem>,
) {
    val hasBlockingError: Boolean
        get() = items.any { it.remoteState is WebDavSyncPlanRemoteState.Error }

    val hasRemoteShrinkRisk: Boolean
        get() = kind == WebDavBatchKind.Upload && items.any { it.shrinksRemote }
}

internal sealed interface WebDavSyncPlanScope {
    data object Batch : WebDavSyncPlanScope
    data object AutoReview : WebDavSyncPlanScope
    data class Single(val item: WebDavSyncItem) : WebDavSyncPlanScope
}

internal data class WebDavSyncPlanItem(
    val item: WebDavSyncItem,
    val localCount: Int,
    val localHash: String,
    val remoteState: WebDavSyncPlanRemoteState,
    val effect: WebDavSyncPlanEffect,
    val workspaceIdentity: String? = null,
) {
    val remoteEtag: String?
        get() = (remoteState as? WebDavSyncPlanRemoteState.Found)?.etag

    val remoteCount: Int
        get() = when (remoteState) {
            WebDavSyncPlanRemoteState.Empty -> 0
            is WebDavSyncPlanRemoteState.Error -> -1
            is WebDavSyncPlanRemoteState.Found -> remoteState.itemCount
        }

    val shrinksRemote: Boolean
        get() {
            val remote = remoteState as? WebDavSyncPlanRemoteState.Found ?: return false
            return localCount >= 0 && remote.itemCount > localCount
        }
}

internal sealed interface WebDavSyncPlanRemoteState {
    data class Found(
        val itemCount: Int,
        val byteSize: Long,
        val etag: String?,
        val contentHash: String,
    ) : WebDavSyncPlanRemoteState

    data object Empty : WebDavSyncPlanRemoteState

    data class Error(
        val status: WebDavItemStatus,
        val detail: String?,
    ) : WebDavSyncPlanRemoteState
}

internal enum class WebDavSyncPlanEffect {
    NoChange,
    CreateRemote,
    MergeThenUpload,
    UploadOverwrite,
    DownloadReplace,
    RemoteEmpty,
    Error,
}
