package os.kei.feature.github.install

import android.content.Context
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.io.SharedHttpClient
import os.kei.core.log.AppLogger
import os.kei.core.privilege.PrivilegedShell
import os.kei.core.system.AppCommandResult

private const val GITHUB_ROOT_INSTALLER_TAG = "GitHubRootInstaller"

/**
 * Installs release APKs through `pm` under a root shell.
 *
 * A `su` child cannot hand a `PackageInstaller` binder back to the app, so the privileged half runs
 * as shell commands against the same install-session concept: `install-create`, `install-write`,
 * `install-commit`. Download, progress reporting, and nested-archive handling are shared with the
 * Shizuku installer through [GitHubInstallSessionWriter]; only the staged bytes take a detour
 * through the app's own cache so the root `pm` process can read them back by path.
 *
 * Results arrive as `pm` stdout rather than through an `IntentSender`, so the commit outcome is
 * parsed from the `Success` / `Failure [REASON]` line.
 */
class GitHubRootPackageInstaller(
    private val privilegedShell: PrivilegedShell = PrivilegedShell(),
    client: OkHttpClient = SharedHttpClient.base,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork,
) : GitHubManagedApkInstaller {
    private val installSessionWriter = GitHubInstallSessionWriter(client)

    override suspend fun cancel(context: Context, sessionId: Int) {
        if (sessionId <= 0) return
        withContext(ioDispatcher) {
            runRootCommand("pm install-abandon $sessionId", ABANDON_TIMEOUT_MS)
        }
        clearStagedFile(context, sessionId)
    }

    override suspend fun stage(
        context: Context,
        request: GitHubApkInstallRequest,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubApkInstallResult = withContext(ioDispatcher) {
        val appContext = context.applicationContext
        var sessionId = -1
        var stagedFile: File? = null
        try {
            onProgress(GitHubApkInstallProgress(GitHubApkInstallStage.Preparing, 4))
            resolveUnavailableCapability()?.let { return@withContext it }

            val resolvedUrl = resolveManagedInstallDownloadUrl(request).trim()
            if (!resolvedUrl.startsWith("https://", ignoreCase = true)) {
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.DownloadUrlInvalid,
                    message = "Invalid APK download URL",
                )
            }

            sessionId = createSession(
                appPackageName = request.scannedPackageName.trim(),
                installerPackageName = appContext.packageName,
            ).getOrElse { error ->
                    return@withContext GitHubApkInstallResult.Failed(
                        reason = GitHubApkInstallFailureReason.SessionCreateFailed,
                        message = error.rootMessage("Create install session failed"),
                    )
                }

            val sink = GitHubFileApkSink(stagedApkFile(appContext, sessionId))
            stagedFile = sink.file
            val writeResult =
                runCatching {
                    installSessionWriter.streamApkIntoSession(
                        context = appContext,
                        resolvedUrl = resolvedUrl,
                        asset = request.asset,
                        downloadSpeedProfile = request.downloadSpeedProfile,
                        sink = sink,
                        sessionId = sessionId,
                        onProgress = onProgress,
                    )
                }.getOrElse { error ->
                    abandonQuietly(sessionId)
                    stagedFile.deleteQuietly()
                    if (error is CancellationException) {
                        return@withContext GitHubApkInstallResult.Cancelled(request.requestId, sessionId)
                    }
                    AppLogger.e(
                        GITHUB_ROOT_INSTALLER_TAG,
                        "Stage APK failed: request=${request.requestId}, session=$sessionId, " +
                            "error=${error.rootMessage("stage failed")}",
                        error,
                    )
                    return@withContext GitHubApkInstallResult.Failed(
                        reason = if (error is IOException) {
                            GitHubApkInstallFailureReason.DownloadFailed
                        } else {
                            GitHubApkInstallFailureReason.SessionWriteFailed
                        },
                        message = error.rootMessage("Stage APK failed"),
                        sessionId = sessionId,
                    )
                }

            writeStagedFileIntoSession(
                sessionId = sessionId,
                splitName = sink.splitName,
                apkFile = sink.file,
            ).onFailure { error ->
                abandonQuietly(sessionId)
                stagedFile.deleteQuietly()
                return@withContext GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.SessionWriteFailed,
                    message = error.rootMessage("Write install session failed"),
                    sessionId = sessionId,
                )
            }

            val archiveInfo = writeResult.archiveInfo
            val stagedPackageName = archiveInfo.packageName.ifBlank { request.scannedPackageName.trim() }
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
                    targetSdk = archiveInfo.targetSdk,
                ),
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
                totalBytes = writeResult.totalBytes,
            )
        } catch (error: CancellationException) {
            abandonQuietly(sessionId)
            stagedFile.deleteQuietly()
            GitHubApkInstallResult.Cancelled(request.requestId, sessionId)
        } catch (error: Throwable) {
            AppLogger.e(
                GITHUB_ROOT_INSTALLER_TAG,
                "Managed install staging failed: request=${request.requestId}, session=$sessionId, " +
                    "error=${error.rootMessage("stage failed")}",
                error,
            )
            abandonQuietly(sessionId)
            stagedFile.deleteQuietly()
            GitHubApkInstallResult.Failed(
                reason = GitHubApkInstallFailureReason.Unknown,
                message = error.rootMessage("Stage APK failed"),
                sessionId = sessionId,
            )
        } finally {
            stagedFile.deleteQuietly()
        }
    }

    override suspend fun commit(
        context: Context,
        request: GitHubApkInstallRequest,
        sessionId: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        onProgress: suspend (GitHubApkInstallProgress) -> Unit,
    ): GitHubApkInstallResult = withContext(ioDispatcher) {
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
                    targetSdk = request.scannedTargetSdk,
                ),
            )
            resolveUnavailableCapability(sessionId)?.let { return@withContext it }

            val result = runRootCommand("pm install-commit $sessionId", COMMIT_TIMEOUT_MS)
            val outcome = parseRootInstallOutcome(result.combinedOutput())
            if (outcome.succeeded) {
                onProgress(
                    GitHubApkInstallProgress(
                        stage = GitHubApkInstallStage.Succeeded,
                        progressPercent = 100,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        sessionId = sessionId,
                        appLabel = request.scannedAppLabel,
                        packageName = request.scannedPackageName,
                        versionName = request.scannedVersionName,
                        versionCode = request.scannedVersionCode,
                        minSdk = request.scannedMinSdk,
                        targetSdk = request.scannedTargetSdk,
                    ),
                )
                GitHubApkInstallResult.Succeeded(
                    requestId = request.requestId,
                    sessionId = sessionId,
                    packageName = request.scannedPackageName,
                )
            } else {
                abandonQuietly(sessionId)
                AppLogger.w(
                    GITHUB_ROOT_INSTALLER_TAG,
                    "Commit install session failed: request=${request.requestId}, " +
                        "session=$sessionId, output=${outcome.message}",
                )
                GitHubApkInstallResult.Failed(
                    reason = GitHubApkInstallFailureReason.CommitFailed,
                    message = outcome.message.ifBlank { "pm install-commit failed" },
                    sessionId = sessionId,
                )
            }
        } catch (error: CancellationException) {
            abandonQuietly(sessionId)
            GitHubApkInstallResult.Cancelled(request.requestId, sessionId)
        } catch (error: Throwable) {
            abandonQuietly(sessionId)
            GitHubApkInstallResult.Failed(
                reason = GitHubApkInstallFailureReason.CommitFailed,
                message = error.rootMessage("Commit install session failed"),
                sessionId = sessionId,
            )
        } finally {
            clearStagedFile(context, sessionId)
        }
    }

    private fun resolveUnavailableCapability(sessionId: Int = -1): GitHubApkInstallResult.Failed? {
        val status = privilegedShell.currentStatus()
        if (status.isCommandReady) return null
        return GitHubApkInstallResult.Failed(
            reason = GitHubApkInstallFailureReason.RootUnavailable,
            message = status.text,
            sessionId = sessionId,
        )
    }

    /**
     * Mirrors the flags [ShizukuPackageInstallerBridge.applySessionParams] sets on the binder path:
     * replace an existing install, preserve the enabled setting, allow test packages, allow
     * downgrade, full app, user install reason, and update ownership.
     */
    private suspend fun createSession(
        appPackageName: String,
        installerPackageName: String,
    ): Result<Int> {
        val command =
            buildString {
                append("pm install-create -r -t -d --full --update-ownership --skip-enable")
                append(" --install-reason ").append(INSTALL_REASON_USER)
                append(" --user 0")
                append(" -i ").append(installerPackageName)
                if (appPackageName.isNotBlank()) append(" --pkg ").append(appPackageName)
            }
        val result = runRootCommand(command, SESSION_TIMEOUT_MS)
        val output = result.combinedOutput()
        val sessionId = parseRootInstallSessionId(output)
        return if (sessionId != null) {
            Result.success(sessionId)
        } else {
            Result.failure(IOException(output.ifBlank { "pm install-create produced no session id" }))
        }
    }

    private suspend fun writeStagedFileIntoSession(
        sessionId: Int,
        splitName: String,
        apkFile: File,
    ): Result<Unit> {
        val sizeBytes = apkFile.length()
        if (sizeBytes <= 0L) {
            return Result.failure(IOException("Staged APK is empty"))
        }
        val command = "pm install-write -S $sizeBytes $sessionId $splitName \"${apkFile.absolutePath}\""
        val result = runRootCommand(command, WRITE_TIMEOUT_MS)
        val output = result.combinedOutput()
        return if (parseRootInstallOutcome(output).succeeded) {
            Result.success(Unit)
        } else {
            Result.failure(IOException(output.ifBlank { "pm install-write failed" }))
        }
    }

    private suspend fun runRootCommand(command: String, timeoutMs: Long): AppCommandResult =
        privilegedShell.execCommandCancellableResult(command = command, timeoutMs = timeoutMs)

    private suspend fun abandonQuietly(sessionId: Int) {
        if (sessionId <= 0) return
        runCatching { runRootCommand("pm install-abandon $sessionId", ABANDON_TIMEOUT_MS) }
    }

    private fun clearStagedFile(context: Context, sessionId: Int) {
        if (sessionId <= 0) return
        runCatching { stagedApkFile(context.applicationContext, sessionId).delete() }
    }

    private fun stagedApkFile(context: Context, sessionId: Int): File {
        val directory = File(context.cacheDir, STAGED_DIRECTORY_NAME).apply { mkdirs() }
        return File(directory, "root-install-$sessionId.apk")
    }

    private fun File?.deleteQuietly() {
        this ?: return
        runCatching { delete() }
    }

    private fun Throwable.rootMessage(fallback: String): String =
        message?.trim()?.ifBlank { null } ?: fallback

    private companion object {
        const val STAGED_DIRECTORY_NAME = "github-root-install"
        const val INSTALL_REASON_USER = 4
        const val SESSION_TIMEOUT_MS = 30_000L
        const val WRITE_TIMEOUT_MS = 5 * 60 * 1000L
        const val COMMIT_TIMEOUT_MS = 5 * 60 * 1000L
        const val ABANDON_TIMEOUT_MS = 15_000L
    }
}

internal data class RootInstallOutcome(
    val succeeded: Boolean,
    val message: String,
)

/**
 * `pm install-create` answers with `Success: created install session [1234]`. Older and vendor
 * builds sometimes drop the prefix, so any bracketed integer counts.
 */
internal fun parseRootInstallSessionId(output: String): Int? =
    Regex("""\[(\d+)]""")
        .find(output)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

/**
 * `pm` reports terminal state on its own line: `Success` or `Failure [INSTALL_FAILED_…]`.
 * A failure line outranks a success line so partial output cannot read as a win.
 */
internal fun parseRootInstallOutcome(output: String): RootInstallOutcome {
    val lines = output.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    val failure = lines.firstOrNull { it.startsWith("Failure", ignoreCase = true) }
    if (failure != null) {
        return RootInstallOutcome(succeeded = false, message = failure)
    }
    val success = lines.firstOrNull { it.startsWith("Success", ignoreCase = true) }
    if (success != null) {
        return RootInstallOutcome(succeeded = true, message = success)
    }
    return RootInstallOutcome(succeeded = false, message = lines.joinToString(separator = "\n"))
}
