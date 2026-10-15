package os.kei.feature.github.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.text.format.Formatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import os.kei.R
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.core.notification.live.NotificationHelper
import os.kei.core.notification.live.builder.EnvironmentContext
import os.kei.core.notification.live.builder.LegacyNotificationBuilder
import os.kei.core.notification.live.builder.MiIslandNotificationBuilder
import os.kei.core.notification.live.builder.ModernNotificationBuilder
import os.kei.core.notification.live.builder.NotificationPayload
import os.kei.core.notification.live.builder.NotificationRenderStyle
import os.kei.core.notification.live.builder.UserSettings
import os.kei.core.prefs.UiPrefs
import os.kei.feature.github.data.local.AppIconCache
import os.kei.mcp.notification.McpNotificationHelper
import java.util.concurrent.atomic.AtomicLong

object GitHubShareImportNotificationHelper {
    private const val TAG = "GitHubShareImportNotify"
    private const val MIN_NOTIFICATION_POST_INTERVAL_MS = 400L
    const val NOTIFICATION_ID = 38991

    private val notificationPostScheduler = GitHubShareImportNotificationPostScheduler(
        minimumIntervalMs = MIN_NOTIFICATION_POST_INTERVAL_MS,
    )

    fun notifyResolving(context: Context, sourceLabel: String) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.Resolving,
                primaryLabel = sourceLabel
            )
        )
    }

    fun notifyAssetReady(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String,
        assetCount: Int,
        sendInstallActionEnabled: Boolean = false
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.AssetReady,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                count = assetCount.coerceAtLeast(0),
                sendInstallActionEnabled = sendInstallActionEnabled
            )
        )
    }

    fun notifyDelivering(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.Delivering,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyInstalling(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        progressPercent: Int,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.Installing,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName,
                progressPercentOverride = progressPercent.coerceIn(0, 100)
            )
        )
    }

    fun notifyPageInstallPreparing(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        progressPercent: Int,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.Installing,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName,
                progressPercentOverride = progressPercent.coerceIn(0, 100)
            )
        )
    }

    fun notifyInstallDownloading(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        progressPercent: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.InstallDownloading,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName,
                progressPercentOverride = progressPercent.coerceIn(0, 100),
                downloadedBytes = downloadedBytes.coerceAtLeast(0L),
                totalBytes = totalBytes
            )
        )
    }

    fun notifyPageInstallDownloading(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        progressPercent: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.InstallDownloading,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName,
                progressPercentOverride = progressPercent.coerceIn(0, 100),
                downloadedBytes = downloadedBytes.coerceAtLeast(0L),
                totalBytes = totalBytes
            )
        )
    }

    fun notifyInstallCommitting(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.InstallCommitting,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyPageInstallCommitting(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.InstallCommitting,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyInstallReady(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String,
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.InstallReady,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyWaitingInstall(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String,
        assetName: String,
        packageName: String,
        versionName: String = "",
        remainingMinutes: Int,
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.WaitingInstall,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName,
                count = remainingMinutes.coerceAtLeast(0)
            )
        )
    }

    fun notifyInstallDetected(
        context: Context,
        owner: String,
        repo: String,
        appLabel: String,
        packageName: String,
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.InstallDetected,
                owner = owner,
                repo = repo,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyAddingTrack(
        context: Context,
        owner: String,
        repo: String,
        appLabel: String,
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.AddingTrack,
                owner = owner,
                repo = repo,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyAdded(
        context: Context,
        owner: String,
        repo: String,
        appLabel: String,
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.Added,
                owner = owner,
                repo = repo,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyAlreadyTracked(
        context: Context,
        owner: String,
        repo: String,
        appLabel: String,
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.AlreadyTracked,
                owner = owner,
                repo = repo,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyPageInstallConfirm(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String = "",
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = "",
        confirmActionEnabled: Boolean = false
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.PageInstallConfirm,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName,
                pageInstallConfirmActionEnabled = confirmActionEnabled
            )
        )
    }

    fun notifyPageInstallCompleted(
        context: Context,
        owner: String,
        repo: String,
        releaseTag: String = "",
        assetName: String = "",
        appLabel: String = "",
        packageName: String = "",
        versionName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.PageInstallCompleted,
                owner = owner,
                repo = repo,
                releaseTag = releaseTag,
                assetName = assetName,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                targetDisplayName = targetDisplayName
            )
        )
    }

    fun notifyFailed(context: Context, reason: String) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.Failed,
                primaryLabel = reason
            )
        )
    }

    fun notifyPageInstallFailed(
        context: Context,
        reason: String,
        owner: String = "",
        repo: String = "",
        packageName: String = "",
        targetDisplayName: String = ""
    ) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.PageInstallFailed,
                owner = owner,
                repo = repo,
                packageName = packageName,
                targetDisplayName = targetDisplayName,
                primaryLabel = reason
            )
        )
    }

    fun notifyCancelled(context: Context) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                phase = GitHubShareImportNotificationPhase.Cancelled
            )
        )
    }

    fun notifyPageInstallCancelled(context: Context) {
        notifyState(
            context = context,
            state = GitHubShareImportNotificationState(
                source = GitHubShareImportNotificationSource.PageInstall,
                phase = GitHubShareImportNotificationPhase.PageInstallCancelled
            )
        )
    }

    fun cancel(context: Context) {
        notificationPostScheduler.clear()
        McpNotificationHelper.cancelNotification(context, NOTIFICATION_ID)
    }

    @SuppressLint("MissingPermission")
    internal fun notifyState(
        context: Context,
        state: GitHubShareImportNotificationState
    ): Boolean {
        if (!notificationsGranted(context)) return false
        notificationPostScheduler.enqueue(
            context = context.applicationContext,
            state = state,
            post = ::postState,
        )
        return true
    }

    private fun postState(
        context: Context,
        state: GitHubShareImportNotificationState,
    ) {
        McpNotificationHelper.ensureChannel(context)
        val buildResult = buildFrameworkNotificationResult(context, state)
        McpNotificationHelper.dispatchNotification(
            context = context,
            notificationId = NOTIFICATION_ID,
            notification = buildResult.notification,
            useXiaomiMagic = buildResult.useXiaomiMagic
        )
    }

    internal fun buildFrameworkLiveUpdateNotification(
        context: Context,
        state: GitHubShareImportNotificationState
    ): Notification {
        val helper = NotificationHelper(context)
        val payload = NotificationPayload(
            state = buildPayload(context, state),
            settings =
                UserSettings(
                    miIslandOuterGlow = false,
                    miIslandFirstFloat = GitHubNotificationPreferences.isSuperIslandFirstFloatEnabled(),
                ),
            environment = EnvironmentContext(
                channelId = McpNotificationHelper.LIVE_CHANNEL_ID,
                isHyperOS = helper.isHyperOS,
            ),
            semanticIconBitmap = resolveSemanticIconBitmap(context, state)
        )
        return if (helper.isModernLiveUpdateEligible) {
            ModernNotificationBuilder(context).build(payload)
        } else {
            LegacyNotificationBuilder(context).build(payload)
        }
    }

    internal fun buildFrameworkMiIslandNotification(
        context: Context,
        state: GitHubShareImportNotificationState
    ): Notification {
        val helper = NotificationHelper(context)
        val payload = NotificationPayload(
            state = buildPayload(context, state),
            settings =
                UserSettings(
                    miIslandOuterGlow = true,
                    miIslandFirstFloat = GitHubNotificationPreferences.isSuperIslandFirstFloatEnabled(),
                ),
            environment = EnvironmentContext(
                channelId = helper.resolveChannel(NotificationRenderStyle.MI_ISLAND),
                isHyperOS = helper.isHyperOS,
            ),
            semanticIconBitmap = resolveSemanticIconBitmap(context, state),
            miIslandProgressColorOverride = state.phase.miIslandProgressColor
        )
        return MiIslandNotificationBuilder(context).build(payload)
    }

    private fun buildFrameworkNotificationResult(
        context: Context,
        state: GitHubShareImportNotificationState
    ): ShareImportNotificationBuildResult {
        val helper = NotificationHelper(context)
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val bypassRestriction = UiPrefs.isSuperIslandBypassRestrictionEnabled(defaultValue = false)
        val decision = helper.resolveRenderDecision(
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
        )
        val useMiIsland = decision.style == NotificationRenderStyle.MI_ISLAND
        AppLogger.i(TAG, "buildNotification ${decision.logSummary()}")
        return if (useMiIsland) {
            ShareImportNotificationBuildResult(
                notification = buildFrameworkMiIslandNotification(context, state),
                useXiaomiMagic = decision.useXiaomiMagic
            )
        } else {
            ShareImportNotificationBuildResult(
                notification = buildFrameworkLiveUpdateNotification(context, state),
                useXiaomiMagic = false
            )
        }
    }

    private fun buildPayload(
        context: Context,
        state: GitHubShareImportNotificationState
    ): LiveNotificationPayload {
        val liveUpdateActive = state.phase.ongoing || state.phase.promotedLiveUpdate
        val openPendingIntent = GitHubShareImportNotificationActions.buildOpenPendingIntent(
            context = context,
            state = state
        )
        val primaryActionLabel = context.getString(
            GitHubShareImportNotificationActions.primaryActionLabelRes(state)
        )
        val secondaryPendingIntent =
            GitHubShareImportNotificationActions.buildSecondaryPendingIntent(
            context = context,
            state = state,
            openPendingIntent = openPendingIntent
        )
        val shortText = context.getString(state.phase.shortTextRes)
        val content = resolveContent(context, state)
        val islandTitle = state.compactIslandTitle(shortText)
        val islandSubtitle = state.compactIslandSubtitle(shortText, islandTitle)
        val progressPercent = state.resolvedProgressPercent
        val useDeterminateProgress = state.hasDeterminateDownloadProgress
        val overrideProgressPercent = progressPercent.takeIf { useDeterminateProgress }
        return LiveNotificationPayload(
            serverName = LiveNotificationPayload.GITHUB_SHARE_IMPORT_SERVER_NAME,
            running = liveUpdateActive,
            port = progressPercent,
            path = content,
            clients = if (useDeterminateProgress) 1 else 0,
            ongoing = state.phase.ongoing,
            onlyAlertOnce = true,
            openPendingIntent = openPendingIntent,
            stopPendingIntent = secondaryPendingIntent,
            focusOpenPendingIntent = openPendingIntent,
            primaryActionLabel = primaryActionLabel,
            secondaryActionLabel = GitHubShareImportNotificationActions.secondaryActionLabel(
                context,
                state
            ),
            showSecondaryActionWhenStopped = true,
            overrideTitle = context.getString(state.phase.titleRes),
            overrideContent = content,
            overrideOnlineText = islandTitle.ifBlank { shortText },
            overrideShortText = islandSubtitle.ifBlank { shortText },
            overrideProgressPercent = overrideProgressPercent,
            miFocusTitle = resolveMiFocusTitle(context, state),
            miFocusSpecialTitle = state.compactVersionBadgeLabel.takeIf {
                it.isNotBlank() && state.phase in versionBadgePhases
            },
            miFocusContent = resolveMiFocusContent(context, state),
            notificationId = NOTIFICATION_ID,
            miFocusOrderId = state.focusOrderId
        )
    }

    private fun resolveSemanticIconBitmap(
        context: Context,
        state: GitHubShareImportNotificationState
    ) = state.packageName.trim()
        .takeIf { it.isNotBlank() }
        ?.let { packageName -> AppIconCache.getOrLoad(context, packageName) }

    private fun resolveContent(
        context: Context,
        state: GitHubShareImportNotificationState
    ): String {
        if (state.pageInstallCancelActionEnabled) {
            return resolvePageInstallActiveContent(context, state)
        }
        val projectLabel = state.projectLabel
        val projectDisplayLabel = state.projectDisplayLabel
        val targetLabel = state.targetWithVersionLabel
        val content = when (state.phase) {
            GitHubShareImportNotificationPhase.Resolving -> context.getString(
                R.string.github_share_import_notify_content_resolving,
                state.primaryLabel.ifBlank { projectDisplayLabel }
            )

            GitHubShareImportNotificationPhase.AssetReady -> context.getString(
                R.string.github_share_import_notify_content_asset_ready,
                projectDisplayLabel,
                state.releaseTag.ifBlank { context.getString(R.string.github_asset_target_latest) },
                state.count.coerceAtLeast(0)
            )

            GitHubShareImportNotificationPhase.Delivering -> context.getString(
                R.string.github_share_import_notify_content_delivering,
                projectDisplayLabel,
                targetLabel
            )

            GitHubShareImportNotificationPhase.InstallDownloading -> context.getString(
                R.string.github_share_import_notify_content_install_downloading,
                targetLabel,
                formatDownloadProgress(context, state.downloadedBytes, state.totalBytes)
            )

            GitHubShareImportNotificationPhase.Installing -> context.getString(
                R.string.github_share_import_notify_target_only,
                targetLabel,
            )

            GitHubShareImportNotificationPhase.InstallReady -> context.getString(
                R.string.github_share_import_notify_target_only,
                targetLabel,
            )

            GitHubShareImportNotificationPhase.InstallCommitting -> context.getString(
                R.string.github_share_import_notify_target_only,
                targetLabel,
            )

            GitHubShareImportNotificationPhase.WaitingInstall -> context.getString(
                if (state.packageName.isNotBlank()) {
                    R.string.github_share_import_notify_content_waiting_install_exact
                } else {
                    R.string.github_share_import_notify_content_waiting_install
                },
                targetLabel,
                state.count.coerceAtLeast(0)
            )

            GitHubShareImportNotificationPhase.InstallDetected -> context.getString(
                R.string.github_share_import_notify_content_install_detected,
                state.appDisplayLabel,
                state.installDetectedProjectLabel
            )

            GitHubShareImportNotificationPhase.AddingTrack -> context.getString(
                R.string.github_share_import_notify_content_adding_track,
                state.appDisplayLabel,
                projectLabel
            )

            GitHubShareImportNotificationPhase.Added -> context.getString(
                R.string.github_share_import_notify_content_added,
                state.appDisplayLabel,
                projectLabel
            )

            GitHubShareImportNotificationPhase.AlreadyTracked -> context.getString(
                R.string.github_share_import_notify_content_already_tracked,
                state.appDisplayLabel,
                projectLabel
            )

            GitHubShareImportNotificationPhase.PageInstallConfirm -> context.getString(
                R.string.github_page_install_notify_content_confirm,
                state.appDisplayLabel,
                projectLabel
            )

            GitHubShareImportNotificationPhase.PageInstallCompleted -> context.getString(
                R.string.github_page_install_notify_content_completed,
                state.appDisplayLabel,
                projectLabel
            )

            GitHubShareImportNotificationPhase.Failed -> state.primaryLabel.ifBlank {
                context.getString(R.string.github_share_import_error_resolve_failed)
            }

            GitHubShareImportNotificationPhase.PageInstallFailed -> state.primaryLabel.ifBlank {
                context.getString(R.string.github_share_import_error_app_managed_install_failed)
            }

            GitHubShareImportNotificationPhase.PageInstallCancelled -> context.getString(
                R.string.github_page_install_notify_content_cancelled
            )

            GitHubShareImportNotificationPhase.Cancelled -> context.getString(
                R.string.github_share_import_notify_content_cancelled
            )
        }
        return if (state.source == GitHubShareImportNotificationSource.PageInstall) {
            compactWithoutEllipsis(content, PAGE_INSTALL_FOCUS_CONTENT_MAX_LENGTH)
        } else {
            content
        }
    }

    private fun resolvePageInstallActiveContent(
        context: Context,
        state: GitHubShareImportNotificationState,
    ): String {
        if (!state.hasDeterminateDownloadProgress) {
            return compactWithoutEllipsis(
                state.targetWithVersionLabel,
                PAGE_INSTALL_FOCUS_CONTENT_MAX_LENGTH,
            )
        }
        return compactStatusAndTarget(
            status = "${state.resolvedProgressPercent}%",
            target = state.targetWithVersionLabel,
            maxLength = PAGE_INSTALL_FOCUS_CONTENT_MAX_LENGTH,
        )
    }

    private fun resolveMiFocusContent(
        context: Context,
        state: GitHubShareImportNotificationState,
    ): String? {
        if (state.phase !in conciseInstallPhases) return null
        val target = state.targetDisplayLabel
        val version = state.versionBadgeLabel
        if (state.phase != GitHubShareImportNotificationPhase.InstallDownloading) {
            return compactWithoutEllipsis(
                version.takeIf { state.usesExpandedVersionLayout }.orEmpty().ifBlank { target },
                PAGE_INSTALL_FOCUS_CONTENT_MAX_LENGTH,
            )
        }
        return compactWithoutEllipsis(
            context.getString(
                R.string.github_share_import_notify_content_install_downloading,
                target,
                formatDownloadProgress(context, state.downloadedBytes, state.totalBytes),
            ),
            PAGE_INSTALL_FOCUS_CONTENT_MAX_LENGTH,
        )
    }

    private fun resolveMiFocusTitle(
        context: Context,
        state: GitHubShareImportNotificationState,
    ): String? {
        if (state.phase !in conciseInstallPhases) return null
        if (!state.usesExpandedVersionLayout) return null
        val target = state.targetDisplayLabel
        return compactWithoutEllipsis(
            target.ifBlank { context.getString(state.phase.titleRes) },
            PAGE_INSTALL_FOCUS_TITLE_MAX_LENGTH,
        )
    }

    private fun compactStatusAndTarget(
        status: String,
        target: String,
        maxLength: Int,
    ): String {
        val compactStatus = compactWithoutEllipsis(status, maxLength)
        if (compactStatus.length >= maxLength) return compactStatus
        val separator = " · "
        val targetLimit = maxLength - compactStatus.length - separator.length
        if (targetLimit <= 0) return compactStatus
        val compactTarget = compactWithoutEllipsis(target, targetLimit)
        return if (compactTarget.isBlank()) {
            compactStatus
        } else {
            compactStatus + separator + compactTarget
        }
    }

    private fun compactWithoutEllipsis(
        value: String,
        maxLength: Int,
    ): String =
        value
            .trim()
            .take(maxLength.coerceAtLeast(1))
            .trimEnd()

    private fun formatDownloadProgress(
        context: Context,
        downloadedBytes: Long,
        totalBytes: Long
    ): String {
        val downloaded = Formatter.formatFileSize(context, downloadedBytes.coerceAtLeast(0L))
        if (totalBytes > 0L) {
            return context.getString(
                R.string.github_share_import_notify_download_progress_known,
                downloaded,
                Formatter.formatFileSize(context, totalBytes)
            )
        }
        return context.getString(
            R.string.github_share_import_notify_download_progress_unknown,
            downloaded
        )
    }

    private fun notificationsGranted(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }
}

private const val PAGE_INSTALL_FOCUS_CONTENT_MAX_LENGTH = 28
private const val PAGE_INSTALL_FOCUS_TITLE_MAX_LENGTH = 18

private val GitHubShareImportNotificationState.usesExpandedVersionLayout: Boolean
    get() = versionBadgeLabel.length > compactVersionBadgeLabel.length

private val conciseInstallPhases =
    setOf(
        GitHubShareImportNotificationPhase.InstallDownloading,
        GitHubShareImportNotificationPhase.Installing,
        GitHubShareImportNotificationPhase.InstallReady,
        GitHubShareImportNotificationPhase.InstallCommitting,
        GitHubShareImportNotificationPhase.PageInstallConfirm,
        GitHubShareImportNotificationPhase.PageInstallCompleted,
    )

private val versionBadgePhases =
    conciseInstallPhases +
        setOf(
            GitHubShareImportNotificationPhase.WaitingInstall,
            GitHubShareImportNotificationPhase.InstallDetected,
        )

private data class ShareImportNotificationBuildResult(
    val notification: Notification,
    val useXiaomiMagic: Boolean
)

internal class GitHubShareImportNotificationPostScheduler(
    minimumIntervalMs: Long,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + AppDispatchers.githubNotification),
    private val nowElapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private data class PendingPost(
        val context: Context,
        val state: GitHubShareImportNotificationState,
        val generation: Long,
        val post: (Context, GitHubShareImportNotificationState) -> Unit,
    )

    private val mutex = Mutex()
    private val pacer = GitHubShareImportNotificationDispatchPacer(minimumIntervalMs)
    private val requestedGeneration = AtomicLong(0L)
    private var pendingPost: PendingPost? = null
    private var drainJob: Job? = null
    private var latestGeneration = 0L

    fun enqueue(
        context: Context,
        state: GitHubShareImportNotificationState,
        post: (Context, GitHubShareImportNotificationState) -> Unit,
    ) {
        val submittedGeneration = requestedGeneration.incrementAndGet()
        scope.launch {
            mutex.withLock {
                if (submittedGeneration < latestGeneration) return@withLock
                latestGeneration = submittedGeneration
                pendingPost = PendingPost(
                    context = context,
                    state = state,
                    generation = submittedGeneration,
                    post = post,
                )
                if (drainJob?.isActive != true) {
                    drainJob = scope.launch { drain() }
                }
            }
        }
    }

    fun clear() {
        val clearedGeneration = requestedGeneration.incrementAndGet()
        scope.launch {
            mutex.withLock {
                if (clearedGeneration < latestGeneration) return@withLock
                latestGeneration = clearedGeneration
                pendingPost = null
            }
        }
    }

    private suspend fun drain() {
        while (true) {
            val delayMs = mutex.withLock {
                if (pendingPost == null) {
                    drainJob = null
                    return
                }
                pacer.delayUntilReady(nowElapsedRealtimeMs())
            }
            if (delayMs > 0L) delay(delayMs)

            val nextPost = mutex.withLock {
                val queued = pendingPost
                pendingPost = null
                queued?.takeIf { it.generation == latestGeneration }
            } ?: continue

            nextPost.post(nextPost.context, nextPost.state)
            mutex.withLock {
                pacer.markDispatched(nowElapsedRealtimeMs())
            }
        }
    }
}
