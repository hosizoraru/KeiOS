package os.kei.ui.page.main.github.share

import android.content.Context
import os.kei.R
import os.kei.feature.github.notification.GitHubShareImportNotificationHelper

internal fun notifyShareImportResolving(
    context: Context,
    sharedText: String
) {
    GitHubShareImportNotificationHelper.notifyResolving(
        context = context,
        sourceLabel = sharedText
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.take(96)
            .orEmpty()
    )
}

internal fun notifyShareImportAssetReady(
    context: Context,
    preview: GitHubShareImportPreview,
    sendInstallActionEnabled: Boolean = preview.sendInstallActionEnabled
) {
    GitHubShareImportNotificationHelper.notifyAssetReady(
        context = context,
        owner = preview.owner,
        repo = preview.repo,
        releaseTag = preview.releaseTag,
        assetCount = preview.assets.size,
        sendInstallActionEnabled = sendInstallActionEnabled
    )
}

internal fun notifyShareImportInstallDetected(
    context: Context,
    candidate: GitHubPendingShareImportAttachCandidate
) {
    GitHubShareImportNotificationHelper.notifyInstallDetected(
        context = context,
        owner = candidate.owner,
        repo = candidate.repo,
        appLabel = candidate.appLabel,
        packageName = candidate.packageName,
        versionName = candidate.versionName,
        targetDisplayName = buildShareImportTargetDisplayName(
            appLabel = candidate.appLabel,
            repo = candidate.repo,
            packageName = candidate.packageName
        )
    )
}

internal fun notifyShareImportFailed(
    context: Context,
    reason: String
) {
    GitHubShareImportNotificationHelper.notifyFailed(
        context = context,
        reason = reason.ifBlank {
            context.getString(R.string.github_share_import_error_resolve_failed)
        }
    )
}
