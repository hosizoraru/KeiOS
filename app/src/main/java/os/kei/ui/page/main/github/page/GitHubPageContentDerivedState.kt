package os.kei.ui.page.main.github.page

internal data class GitHubPageContentDerivedState(
    val trackedUi: GitHubPageDerivedState = GitHubPageDerivedState(),
    val appLastUpdatedAtByTrackId: Map<String, Long> = emptyMap(),
    val pendingShareImportRepoOverlapCount: Int = 0,
    val showPendingShareImportCard: Boolean = false
)
