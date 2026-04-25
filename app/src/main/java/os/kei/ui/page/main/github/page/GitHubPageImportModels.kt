package os.kei.ui.page.main.github.page

import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload

internal data class GitHubTrackImportApplyResult(
    val addedCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val invalidCount: Int,
    val duplicateCount: Int
)

internal data class GitHubTrackImportPreview(
    val payload: GitHubTrackedItemsImportPayload,
    val fileItemCount: Int,
    val validCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val newCount: Int,
    val updatedCount: Int,
    val unchangedCount: Int,
    val mergedCount: Int
) {
    val canImport: Boolean
        get() = validCount > 0
}
