package os.kei.appwidget.github

internal data class GitHubWidgetSnapshot(
    val available: Boolean = false,
    val trackedCount: Int = 0,
    val updateCount: Int = 0,
    val uncheckedCount: Int = 0,
    val failedCount: Int = 0,
    val ignoredCount: Int = 0,
    val lastCheckedAtMillis: Long = 0L,
    val pendingUpdates: List<GitHubWidgetPendingUpdate> = emptyList(),
    val projects: List<GitHubWidgetProject> = emptyList(),
) {
    val pendingNames: List<String> get() = pendingUpdates.map { it.name }
    val checkedCount: Int get() = (trackedCount - uncheckedCount - ignoredCount).coerceIn(0, trackedCount)
    val noUpdateCount: Int get() = (checkedCount - failedCount - updateCount).coerceAtLeast(0)
}

internal enum class GitHubWidgetProjectStatus(val storageId: String) {
    Update("update"), Failed("failed"), Unchecked("unchecked"), NoUpdate("current"), Paused("paused");

    companion object {
        fun fromStorageId(value: String): GitHubWidgetProjectStatus = when (value) {
            "update", "Update" -> Update
            "failed", "Failed" -> Failed
            "current", "NoUpdate" -> NoUpdate
            "paused", "Paused" -> Paused
            else -> Unchecked
        }
    }
}

internal data class GitHubWidgetProject(
    val name: String,
    val status: GitHubWidgetProjectStatus,
    val version: String = "",
    val preRelease: Boolean = false,
    val localVersion: String = "",
)

internal data class GitHubWidgetPendingUpdate(
    val name: String,
    val version: String = "",
    val preRelease: Boolean = false,
)

internal data class GitHubWidgetConfiguration(
    val showProjectNames: Boolean = false,
)
