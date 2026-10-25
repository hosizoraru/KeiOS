package os.kei.feature.github.install

import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.io.SharedHttpClient
import os.kei.core.log.AppLogger
import os.kei.feature.github.data.local.GitHubInstalledAppRepository
import os.kei.feature.github.data.local.gitHubInstalledAppQueryFlags
import os.kei.feature.github.data.remote.GitHubReleaseAssetRepository
import os.kei.feature.github.model.GitHubLookupStrategyOption

private const val GITHUB_SHIZUKU_INSTALLER_TAG = "GitHubShizukuInstaller"

interface GitHubManagedApkInstaller {
    suspend fun stage(
        context: Context,
        request: GitHubApkInstallRequest,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit = {}
    ): GitHubApkInstallResult

    suspend fun commit(
        context: Context,
        request: GitHubApkInstallRequest,
        sessionId: Int,
        downloadedBytes: Long = 0L,
        totalBytes: Long = -1L,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit = {}
    ): GitHubApkInstallResult

    suspend fun cancel(context: Context, sessionId: Int) = Unit

    suspend fun install(
        context: Context,
        request: GitHubApkInstallRequest,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit = {}
    ): GitHubApkInstallResult {
        val staged = stage(context, request, onProgress)
        if (staged !is GitHubApkInstallResult.Staged) return staged
        val commitRequest = request.copy(
            scannedAppLabel = staged.appLabel.ifBlank { request.scannedAppLabel },
            scannedPackageName = staged.packageName.ifBlank { request.scannedPackageName },
            scannedVersionName = staged.versionName.ifBlank { request.scannedVersionName },
            scannedVersionCode = staged.versionCode.ifBlank { request.scannedVersionCode },
            scannedMinSdk = staged.minSdk.ifBlank { request.scannedMinSdk },
            scannedTargetSdk = staged.targetSdk.ifBlank { request.scannedTargetSdk }
        )
        return commit(
            context = context,
            request = commitRequest,
            sessionId = staged.sessionId,
            downloadedBytes = staged.downloadedBytes,
            totalBytes = staged.totalBytes,
            onProgress = onProgress
        )
    }
}

class GitHubShizukuPackageInstaller(
    private val bridge: ShizukuPackageInstallerBridge = ShizukuPackageInstallerBridge(),
    private val client: OkHttpClient = defaultClient,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork
) : GitHubManagedApkInstaller {
    private val installSessionWriter = GitHubInstallSessionWriter(client)

    override suspend fun cancel(context: Context, sessionId: Int) = withContext(ioDispatcher) {
        if (sessionId <= 0) return@withContext
        runCatching {
            bridge.packageInstaller(context.applicationContext).abandonSession(sessionId)
        }.onFailure { error ->
            AppLogger.w(
                GITHUB_SHIZUKU_INSTALLER_TAG,
                "Cancel install session failed: session=$sessionId, " +
                        "error=${error.installMessage("cancel failed")}",
                error
            )
        }
    }

    override suspend fun stage(
        context: Context,
        request: GitHubApkInstallRequest,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit
    ): GitHubApkInstallResult = withContext(ioDispatcher) {
        val appContext = context.applicationContext
        var sessionId = -1
        var packageInstaller: PackageInstaller? = null
        try {
            onProgress(GitHubApkInstallProgress(GitHubApkInstallStage.Preparing, 4))
            val capability = bridge.checkCapability()
            if (!capability.available) {
                return@withContext GitHubApkInstallResult.Failed(
                    reason = capability.failureReason ?: GitHubApkInstallFailureReason.Unknown,
                    message = capability.message.ifBlank { "Shizuku install capability unavailable" }
                )
            }
            val resolvedUrl = resolveManagedInstallDownloadUrl(request).trim()
            if (!resolvedUrl.startsWith("https://", ignoreCase = true)) {
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.DownloadUrlInvalid,
                    message = "Invalid APK download URL"
                )
            }

            packageInstaller = runCatching {
                bridge.packageInstaller(appContext)
            }.getOrElse { error ->
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "Create PackageInstaller bridge failed: request=${request.requestId}, " +
                            "package=${appContext.packageName}, error=${error.installMessage("bridge failed")}",
                    error
                )
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.SessionCreateFailed,
                    message = error.installMessage("Create PackageInstaller bridge failed")
                )
            }
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            bridge.applySessionParams(
                params = params,
                context = appContext,
                appPackageName = request.scannedPackageName.trim()
            )
            sessionId = runCatching { packageInstaller.createSession(params) }
                .getOrElse { error ->
                    AppLogger.e(
                        GITHUB_SHIZUKU_INSTALLER_TAG,
                        "Create install session failed: request=${request.requestId}, " +
                                "package=${appContext.packageName}, error=${error.installMessage("session failed")}",
                        error
                    )
                    return@withContext GitHubApkInstallResult.Failed(
                        reason = GitHubApkInstallFailureReason.SessionCreateFailed,
                        message = error.installMessage("Create install session failed")
                    )
                }

            val session = runCatching {
                bridge.wrapSession(packageInstaller.openSession(sessionId))
            }.getOrElse { error ->
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "Open install session failed: request=${request.requestId}, session=$sessionId, " +
                            "error=${error.installMessage("open failed")}",
                    error
                )
                abandonSession(packageInstaller, sessionId)
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.SessionOpenFailed,
                    message = error.installMessage("Open install session failed"),
                    sessionId = sessionId
                )
            }

            val writeResult = runCatching {
                installSessionWriter.streamApkIntoSession(
                    context = appContext,
                    resolvedUrl = resolvedUrl,
                    asset = request.asset,
                    downloadSpeedProfile = request.downloadSpeedProfile,
                    sink = GitHubSessionApkSink(session),
                    sessionId = sessionId,
                    onProgress = onProgress
                )
            }.getOrElse { error ->
                session.closeQuietly()
                abandonSession(packageInstaller, sessionId)
                if (error is CancellationException) {
                    return@withContext GitHubApkInstallResult.Cancelled(
                        request.requestId,
                        sessionId
                    )
                }
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "Stage APK failed: request=${request.requestId}, session=$sessionId, " +
                            "error=${error.installMessage("stage failed")}",
                    error
                )
                return@withContext GitHubApkInstallResult.Failed(
                    reason = if (error is IOException) {
                        GitHubApkInstallFailureReason.DownloadFailed
                    } else {
                        GitHubApkInstallFailureReason.SessionWriteFailed
                    },
                    message = error.installMessage("Stage APK failed"),
                    sessionId = sessionId
                )
            }

            session.closeQuietly()
            val archiveInfo = writeResult.archiveInfo
            val stagedPackageName = archiveInfo.packageName.ifBlank {
                request.scannedPackageName.trim()
            }
            onProgress(
                GitHubApkInstallProgress(
                    stage = GitHubApkInstallStage.ReadyToCommit,
                    progressPercent = 100,
                    downloadedBytes = writeResult.bytesWritten,
                    totalBytes = writeResult.totalBytes,
                    sessionId = sessionId,
                    appLabel = archiveInfo.appLabel,
                    packageName = stagedPackageName,
                    versionName = archiveInfo.versionName,
                    versionCode = archiveInfo.versionCode,
                    minSdk = archiveInfo.minSdk,
                    targetSdk = archiveInfo.targetSdk
                )
            )
            GitHubApkInstallResult.Staged(
                requestId = request.requestId,
                sessionId = sessionId,
                packageName = stagedPackageName,
                appLabel = archiveInfo.appLabel,
                versionName = archiveInfo.versionName,
                versionCode = archiveInfo.versionCode,
                minSdk = archiveInfo.minSdk,
                targetSdk = archiveInfo.targetSdk,
                downloadedBytes = writeResult.bytesWritten,
                totalBytes = writeResult.totalBytes
            )
        } catch (error: CancellationException) {
            packageInstaller?.let { abandonSession(it, sessionId) }
            GitHubApkInstallResult.Cancelled(request.requestId, sessionId)
        } catch (error: Throwable) {
            AppLogger.e(
                GITHUB_SHIZUKU_INSTALLER_TAG,
                "Managed install staging failed: request=${request.requestId}, session=$sessionId, " +
                        "error=${error.installMessage("stage failed")}",
                error
            )
            packageInstaller?.let { abandonSession(it, sessionId) }
            GitHubApkInstallResult.Failed(
                reason = GitHubApkInstallFailureReason.Unknown,
                message = error.installMessage("Stage APK failed"),
                sessionId = sessionId
            )
        }
    }

    override suspend fun commit(
        context: Context,
        request: GitHubApkInstallRequest,
        sessionId: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit
    ): GitHubApkInstallResult = withContext(ioDispatcher) {
        val appContext = context.applicationContext
        var packageInstaller: PackageInstaller? = null
        var session: PackageInstaller.Session? = null
        try {
            onProgress(
                GitHubApkInstallProgress(
                    stage = GitHubApkInstallStage.Committing,
                    progressPercent = 92,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    sessionId = sessionId,
                    appLabel = request.scannedAppLabel,
                    packageName = request.scannedPackageName,
                    versionName = request.scannedVersionName,
                    versionCode = request.scannedVersionCode,
                    minSdk = request.scannedMinSdk,
                    targetSdk = request.scannedTargetSdk
                )
            )
            val capability = bridge.checkCapability()
            if (!capability.available) {
                return@withContext GitHubApkInstallResult.Failed(
                    reason = capability.failureReason ?: GitHubApkInstallFailureReason.Unknown,
                    message = capability.message.ifBlank { "Shizuku install capability unavailable" },
                    sessionId = sessionId
                )
            }
            packageInstaller = runCatching {
                bridge.packageInstaller(appContext)
            }.getOrElse { error ->
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "Create PackageInstaller bridge for commit failed: request=${request.requestId}, " +
                            "package=${appContext.packageName}, error=${error.installMessage("bridge failed")}",
                    error
                )
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.SessionCreateFailed,
                    message = error.installMessage("Create PackageInstaller bridge failed"),
                    sessionId = sessionId
                )
            }
            session = runCatching {
                bridge.wrapSession(packageInstaller.openSession(sessionId))
            }.getOrElse { error ->
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "Open staged install session failed: request=${request.requestId}, session=$sessionId, " +
                            "error=${error.installMessage("open failed")}",
                    error
                )
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.SessionOpenFailed,
                    message = error.installMessage("Open staged install session failed"),
                    sessionId = sessionId
                )
            }

            val deferred = GitHubShizukuInstallCommitRegistry.register(request.requestId)
            val sender = GitHubShizukuInstallCommitRegistry.buildIntentSender(
                context = appContext,
                requestId = request.requestId,
                sessionId = sessionId
            )
            runCatching {
                session.commit(sender)
            }.onFailure { error ->
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "Commit install session failed: request=${request.requestId}, session=$sessionId, " +
                            "error=${error.installMessage("commit failed")}",
                    error
                )
                GitHubShizukuInstallCommitRegistry.unregister(request.requestId)
                session.closeQuietly()
                abandonSession(packageInstaller, sessionId)
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.CommitFailed,
                    message = error.installMessage("Commit install session failed"),
                    sessionId = sessionId
                )
            }
            session.closeQuietly()
            session = null

            val commitResult = try {
                withTimeoutOrNull(5.minutes) { deferred.await() }
            } catch (error: CancellationException) {
                GitHubShizukuInstallCommitRegistry.unregister(request.requestId)
                abandonSession(packageInstaller, sessionId)
                return@withContext GitHubApkInstallResult.Cancelled(
                    request.requestId,
                    sessionId
                )
            }
            if (commitResult == null) {
                GitHubShizukuInstallCommitRegistry.unregister(request.requestId)
                abandonSession(packageInstaller, sessionId)
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.ResultTimeout,
                    message = "Install result timed out",
                    sessionId = sessionId
                )
            }

            if (commitResult.statusCode != PackageInstaller.STATUS_SUCCESS) {
                AppLogger.e(
                    GITHUB_SHIZUKU_INSTALLER_TAG,
                    "PackageInstaller commit result failed: request=${request.requestId}, " +
                            "session=$sessionId, status=${commitResult.statusCode}, " +
                            "legacy=${commitResult.legacyStatus}, message=${commitResult.message}"
                )
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.CommitFailed,
                    message = commitResult.message.ifBlank { "PackageInstaller commit failed" },
                    sessionId = sessionId,
                    statusCode = commitResult.statusCode,
                    legacyStatus = commitResult.legacyStatus,
                    packageName = commitResult.packageName
                )
            }

            val packageName = request.scannedPackageName
                .trim()
                .ifBlank { commitResult.packageName.trim() }
            if (packageName.isBlank()) {
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.PackageNameMissing,
                    message = "Install succeeded but package name was not reported",
                    sessionId = sessionId,
                    statusCode = commitResult.statusCode,
                    legacyStatus = commitResult.legacyStatus
                )
            }
            onProgress(
                GitHubApkInstallProgress(
                    stage = GitHubApkInstallStage.Succeeded,
                    progressPercent = 100,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    sessionId = sessionId,
                    appLabel = request.scannedAppLabel,
                    packageName = packageName,
                    versionName = request.scannedVersionName,
                    versionCode = request.scannedVersionCode,
                    minSdk = request.scannedMinSdk,
                    targetSdk = request.scannedTargetSdk
                )
            )
            val installed = loadPackageInfo(appContext, packageName)
            GitHubApkInstallResult.Succeeded(
                requestId = request.requestId,
                sessionId = sessionId,
                packageName = packageName,
                appLabel = installed?.appLabel.orEmpty(),
                firstInstallTimeMs = installed?.firstInstallTimeMs ?: -1L
            )
        } catch (error: CancellationException) {
            packageInstaller?.let { abandonSession(it, sessionId) }
            GitHubApkInstallResult.Cancelled(request.requestId, sessionId)
        } catch (error: Throwable) {
            AppLogger.e(
                GITHUB_SHIZUKU_INSTALLER_TAG,
                "Managed install commit failed: request=${request.requestId}, session=$sessionId, " +
                        "error=${error.installMessage("install failed")}",
                error
            )
            packageInstaller?.let { abandonSession(it, sessionId) }
            GitHubApkInstallResult.Failed(
                reason = GitHubApkInstallFailureReason.Unknown,
                message = error.installMessage("Install failed"),
                sessionId = sessionId
            )
        } finally {
            session?.closeQuietly()
        }
    }

    private fun abandonSession(packageInstaller: PackageInstaller, sessionId: Int) {
        if (sessionId <= 0) return
        runCatching {
            packageInstaller.abandonSession(sessionId)
        }
    }

    private fun PackageInstaller.Session.closeQuietly() {
        runCatching { close() }
    }

    private fun loadPackageInfo(
        context: Context,
        packageName: String
    ): InstalledPackageInfo? {
        val pm = context.packageManager
        val info = runCatching {
            GitHubInstalledAppRepository.installedPackageInfoOrNull(context, packageName)
        }.getOrNull() ?: return null
        val label = runCatching {
            val appInfo = info.applicationInfo ?: pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(gitHubInstalledAppQueryFlags)
            )
            pm.getApplicationLabel(appInfo).toString().trim()
        }.getOrDefault("")
        return InstalledPackageInfo(
            appLabel = label,
            firstInstallTimeMs = info.firstInstallTime
        )
    }

    private fun Throwable.installMessage(fallback: String): String {
        return message?.takeIf { it.isNotBlank() } ?: fallback
    }

    private data class InstalledPackageInfo(
        val appLabel: String,
        val firstInstallTimeMs: Long
    )

    companion object {
        private val defaultClient: OkHttpClient by lazy {
            SharedHttpClient.base
        }
    }
}

internal suspend fun resolveManagedInstallDownloadUrl(request: GitHubApkInstallRequest): String {
    request.resolvedDownloadUrl.trim().takeIf { it.isNotBlank() }?.let { return it }
    val token = request.lookupConfig.apiToken.trim()
    val preferApiAsset =
        request.lookupConfig.selectedStrategy == GitHubLookupStrategyOption.GitHubApiToken
    return GitHubReleaseAssetRepository.resolvePreferredDownloadUrl(
        asset = request.asset,
        useApiAssetUrl = preferApiAsset,
        apiToken = token
    ).getOrElse { request.asset.downloadUrl }
}
