package os.kei.feature.webdav.sync

import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavError
import os.kei.feature.webdav.client.WebDavRemoteClient
import os.kei.feature.webdav.client.WebDavUploadResult

/** Domain stores supply parsing and snapshot replacement; the protocol never depends on app UI. */
data class WebDavDocumentPort(
    val exportJson: () -> String,
    val merge: (String) -> Unit,
    val localCount: () -> Int,
    val countRemoteItems: (String) -> Int,
    val fingerprintJson: () -> String = exportJson,
    val remoteFingerprintJson: (String) -> String = { it },
    val mergeRemoteOnAutoConflict: Boolean = false,
    val fingerprintRevision: Int = 1,
    val replace: (String) -> Unit = { error("This dataset does not support snapshot restore") },
)

enum class WebDavSyncAction { Sync, Upload, Restore, LocalChange }
enum class WebDavSyncStatus {
    Uploaded, Downloaded, Merged, UpToDate, RemoteEmpty,
    AuthFailed, PermissionDenied, NetworkError, ConflictUnresolved, BaselineRequired, RateLimited, Error,
}

data class WebDavDocumentSnapshot(val content: String, val fingerprint: String, val itemCount: Int) {
    val hash: String get() = contentHash(fingerprint)
}

data class WebDavSyncExecution(
    val status: WebDavSyncStatus,
    val detail: String? = null,
    val checkpoint: WebDavDocumentSnapshot? = null,
    val remote: WebDavDocumentSnapshot? = null,
    val etag: String? = null,
    val missing: Boolean = false,
)

/** One bounded state machine replaces the former interleaved fallback/remerge paths. */
class WebDavSyncSession(private val client: WebDavRemoteClient) {
    suspend fun execute(
        action: WebDavSyncAction,
        fileName: String,
        port: WebDavDocumentPort,
        expectedEtag: String? = null,
        expectedHash: String? = null,
        remoteKnownEmpty: Boolean = false,
        confirmedOverwrite: Boolean = false,
        isCurrent: () -> Boolean = { true },
    ): WebDavSyncExecution = try {
        if (!isCurrent()) WebDavSyncExecution(WebDavSyncStatus.ConflictUnresolved, "Review a fresh plan")
        else when (action) {
            WebDavSyncAction.Restore -> restore(fileName, port, expectedHash, remoteKnownEmpty, isCurrent)
            WebDavSyncAction.Upload -> {
                val local = localSnapshot(port)
                val written = when {
                    confirmedOverwrite -> client.upload(fileName, local.content)
                    remoteKnownEmpty -> client.uploadIfAbsent(fileName, local.content)
                    strongEtag(expectedEtag) != null -> client.upload(fileName, local.content, expectedEtag)
                    else -> WebDavUploadResult.Conflict
                }
                written.result(local, WebDavSyncStatus.Uploaded)
            }
            WebDavSyncAction.Sync -> reconcile(fileName, port, expectedHash)
            WebDavSyncAction.LocalChange -> {
                if (expectedEtag == null && expectedHash == null) {
                    WebDavSyncExecution(WebDavSyncStatus.BaselineRequired)
                } else {
                    val local = localSnapshot(port)
                    val strong = strongEtag(expectedEtag)
                    val first = strong?.let { client.upload(fileName, local.content, it) }
                    if (first != null && first != WebDavUploadResult.Conflict) {
                        first.result(local, WebDavSyncStatus.Uploaded)
                    } else {
                        reconcile(fileName, port, expectedHash, localChange = true)
                    }
                }
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        WebDavSyncExecution(WebDavSyncStatus.Error, "Backup validation or local store operation failed")
    }

    suspend fun inspect(
        fileName: String, port: WebDavDocumentPort, allowInvalid: Boolean = false,
    ): WebDavSyncExecution = try {
        when (val read = client.download(fileName)) {
            WebDavDownloadResult.Empty -> WebDavSyncExecution(WebDavSyncStatus.RemoteEmpty, missing = true)
            is WebDavDownloadResult.Error -> if (allowInvalid &&
                read.error is WebDavError.Unknown && read.error.code == 422
            ) {
                WebDavSyncExecution(WebDavSyncStatus.UpToDate, remote = WebDavDocumentSnapshot("", "", -1))
            } else read.error.result()
            is WebDavDownloadResult.Success -> WebDavSyncExecution(
                WebDavSyncStatus.UpToDate,
                remote = try {
                    remoteSnapshot(port, read.content)
                } catch (error: Exception) {
                    if (!allowInvalid || error is CancellationException) throw error
                    WebDavDocumentSnapshot(read.content, read.content, -1)
                },
                etag = read.etag,
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        WebDavSyncExecution(WebDavSyncStatus.Error, "Remote backup could not be validated")
    }

    private suspend fun restore(
        fileName: String, port: WebDavDocumentPort, expectedHash: String?,
        remoteKnownEmpty: Boolean, isCurrent: () -> Boolean,
    ): WebDavSyncExecution {
        val read = client.download(fileName)
        if (read == WebDavDownloadResult.Empty) return WebDavSyncExecution(WebDavSyncStatus.RemoteEmpty, missing = true)
        if (read is WebDavDownloadResult.Error) return read.error.result()
        read as WebDavDownloadResult.Success
        val remote = remoteSnapshot(port, read.content)
        if (remoteKnownEmpty || (expectedHash != null && expectedHash != remote.hash)) {
            return WebDavSyncExecution(
                WebDavSyncStatus.ConflictUnresolved, "The cloud backup changed; review a fresh plan", remote = remote,
            )
        }
        if (!isCurrent()) return WebDavSyncExecution(
            WebDavSyncStatus.ConflictUnresolved, "Local data changed during download; review a fresh plan",
        )
        val before = localSnapshot(port)
        if (before.hash == remote.hash) return success(WebDavSyncStatus.UpToDate, remote, read.etag)
        try {
            port.replace(remote.content)
            check(localSnapshot(port).hash == remote.hash) { "Restored content differs from the validated backup" }
        } catch (error: Exception) {
            try {
                port.replace(before.content)
            } catch (rollback: Exception) {
                if (error is CancellationException) {
                    error.addSuppressed(rollback)
                    throw error
                }
                if (rollback is CancellationException) throw rollback
                return WebDavSyncExecution(
                    WebDavSyncStatus.Error, "Restore failed and the previous local snapshot could not be recovered",
                )
            }
            throw error
        }
        return success(WebDavSyncStatus.Downloaded, remote, read.etag)
    }

    private suspend fun reconcile(
        fileName: String,
        port: WebDavDocumentPort,
        expectedHash: String? = null,
        localChange: Boolean = false,
    ): WebDavSyncExecution {
        repeat(MAX_ATTEMPTS) {
            when (val read = client.download(fileName)) {
                WebDavDownloadResult.Empty -> {
                    val local = localSnapshot(port)
                    val write = client.uploadIfAbsent(fileName, local.content)
                    if (write != WebDavUploadResult.Conflict) return write.result(local, WebDavSyncStatus.Uploaded)
                }
                is WebDavDownloadResult.Error -> return read.error.result()
                is WebDavDownloadResult.Success -> {
                    val remote = remoteSnapshot(port, read.content)
                    val before = localSnapshot(port)
                    if (before.hash == remote.hash) return success(WebDavSyncStatus.UpToDate, remote, read.etag)
                    if (localChange && remote.hash != expectedHash && !port.mergeRemoteOnAutoConflict) {
                        return WebDavSyncExecution(WebDavSyncStatus.ConflictUnresolved, remote = remote, etag = read.etag)
                    }
                    // Missing/weak validators cannot safely protect an automatic or merge write.
                    val etag = strongEtag(read.etag) ?: return WebDavSyncExecution(
                        WebDavSyncStatus.ConflictUnresolved,
                        "The server did not provide a strong validator; choose an explicit upload or restore",
                        remote = remote, etag = read.etag,
                    )
                    if (remote.hash != expectedHash) port.merge(remote.content)
                    val merged = localSnapshot(port)
                    if (merged.hash == remote.hash) return success(WebDavSyncStatus.Merged, remote, read.etag)
                    val write = client.upload(fileName, merged.content, etag)
                    if (write != WebDavUploadResult.Conflict) {
                        return write.result(merged, if (before.hash == merged.hash) WebDavSyncStatus.Uploaded else WebDavSyncStatus.Merged)
                    }
                }
            }
        }
        return WebDavSyncExecution(WebDavSyncStatus.ConflictUnresolved)
    }

    private fun WebDavUploadResult.result(snapshot: WebDavDocumentSnapshot, status: WebDavSyncStatus): WebDavSyncExecution =
        when (this) {
            is WebDavUploadResult.Success -> success(status, snapshot, etag)
            WebDavUploadResult.Conflict -> WebDavSyncExecution(WebDavSyncStatus.ConflictUnresolved)
            is WebDavUploadResult.Error -> error.result()
        }

    private fun WebDavError.result(): WebDavSyncExecution = when (this) {
        WebDavError.AuthFailed -> WebDavSyncExecution(WebDavSyncStatus.AuthFailed)
        WebDavError.PermissionDenied -> WebDavSyncExecution(WebDavSyncStatus.PermissionDenied)
        WebDavError.NetworkUnreachable -> WebDavSyncExecution(WebDavSyncStatus.NetworkError)
        is WebDavError.Unknown -> WebDavSyncExecution(if (code == 429 || code == 503) WebDavSyncStatus.RateLimited else WebDavSyncStatus.Error, "HTTP $code: $message")
    }

    private fun success(status: WebDavSyncStatus, snapshot: WebDavDocumentSnapshot, etag: String?) =
        WebDavSyncExecution(status, checkpoint = snapshot, remote = snapshot, etag = etag)

    companion object {
        private const val MAX_ATTEMPTS = 3
        fun localSnapshot(port: WebDavDocumentPort): WebDavDocumentSnapshot {
            val content = port.exportJson()
            return WebDavDocumentSnapshot(content, port.remoteFingerprintJson(content), port.localCount())
        }
        fun remoteSnapshot(port: WebDavDocumentPort, content: String) = WebDavDocumentSnapshot(
            content, port.remoteFingerprintJson(content), port.countRemoteItems(content).also { require(it >= 0) },
        )
        fun strongEtag(value: String?): String? = value?.trim()?.takeIf {
            it.isNotEmpty() && !it.startsWith("W/", true) && it.none(Char::isISOControl) &&
                '"' !in it.removeSurrounding("\"")
        }
    }
}

fun contentHash(content: String): String =
    MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
