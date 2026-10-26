package os.kei.ui.page.main.sync

import kotlinx.coroutines.CancellationException
import os.kei.feature.webdav.client.WebDavSyncClient
import os.kei.feature.webdav.client.WebDavTestConnectionResult
import os.kei.feature.webdav.model.WebDavConfig
import os.kei.feature.webdav.model.workspaceIdentity
import os.kei.feature.webdav.sync.WebDavSyncAction
import os.kei.feature.webdav.sync.WebDavSyncExecution
import os.kei.feature.webdav.sync.WebDavSyncSession

/** App adapter: protocol policy lives in feature-webdav; this layer owns item IDs and MMKV. */
internal class WebDavSyncEngine(
    private val clientFactory: (WebDavConfig) -> WebDavSyncClientBridge = {
        RealWebDavSyncClientBridge(WebDavSyncClient(it))
    },
    private val metadataStore: WebDavSyncMetadataStore = StoreBackedWebDavSyncMetadataStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private var cachedConfig: WebDavConfig? = null
    private var cachedClient: WebDavSyncClientBridge? = null
    private val clientLock = Any()
    private var clientActive = false
    private var invalidated = false
    private var disposed = false

    private fun client(config: WebDavConfig): WebDavSyncClientBridge = synchronized(clientLock) {
        check(!disposed) { "The WebDAV client owner was cleared" }
        if (cachedConfig != config || invalidated || cachedClient == null) {
            val replacement = clientFactory(config)
            cachedClient?.close()
            cachedClient = replacement
            cachedConfig = config
            invalidated = false
        }
        clientActive = true
        checkNotNull(cachedClient)
    }

    fun invalidate() = synchronized(clientLock) {
        invalidated = true
        if (!clientActive) closeCachedClient()
    }

    fun close() = synchronized(clientLock) {
        disposed = true
        invalidated = true
        if (!clientActive) closeCachedClient()
    }

    private fun closeCachedClient() {
        cachedClient?.close()
        cachedClient = null
        cachedConfig = null
    }

    private suspend fun <T> operation(block: suspend () -> T): T = WebDavSyncOperationCoordinator.run {
        try {
            block()
        } finally {
            synchronized(clientLock) {
                clientActive = false
                if (invalidated) closeCachedClient()
            }
        }
    }

    suspend fun testConnection(config: WebDavConfig): WebDavConnectionOutcome =
        operation {
            try {
                when (val result = client(config).testConnection()) {
                    is WebDavTestConnectionResult.Success -> WebDavConnectionOutcome(
                        if (result.dirCreated) WebDavConnectionStatus.SuccessDirCreated else WebDavConnectionStatus.Success,
                    )
                    WebDavTestConnectionResult.AuthFailed -> WebDavConnectionOutcome(WebDavConnectionStatus.AuthFailed)
                    WebDavTestConnectionResult.PermissionDenied -> WebDavConnectionOutcome(WebDavConnectionStatus.PermissionDenied)
                    is WebDavTestConnectionResult.NetworkError -> WebDavConnectionOutcome(WebDavConnectionStatus.NetworkError, result.message)
                    is WebDavTestConnectionResult.InvalidUrl -> WebDavConnectionOutcome(WebDavConnectionStatus.InvalidUrl, result.message)
                    is WebDavTestConnectionResult.Error -> WebDavConnectionOutcome(WebDavConnectionStatus.Unknown, result.message)
                }
            } catch (error: CancellationException) { throw error
            } catch (_: IllegalArgumentException) {
                WebDavConnectionOutcome(WebDavConnectionStatus.InvalidUrl)
            }
        }

    suspend fun prepareChange(
        config: WebDavConfig, kind: WebDavBatchKind, item: WebDavSyncItem, port: WebDavSyncDataPort,
    ): WebDavSyncPlanItem = operation {
        val localHash = contentHash(port.fingerprintJson())
        val result = inspect(config, item, port, allowInvalid = kind == WebDavBatchKind.Upload)
        recordRemote(item, result)
        val remote = result.remote
        val state = when {
            remote != null -> WebDavSyncPlanRemoteState.Found(
                remote.itemCount, remote.content.toByteArray(Charsets.UTF_8).size.toLong(), result.etag, remote.hash,
            )
            result.missing -> WebDavSyncPlanRemoteState.Empty
            else -> WebDavSyncPlanRemoteState.Error(WebDavItemStatus.valueOf(result.status.name), result.detail)
        }
        WebDavSyncPlanItem(item, port.localCount(), localHash, state, when {
            state is WebDavSyncPlanRemoteState.Error -> WebDavSyncPlanEffect.Error
            remote != null && localHash == remote.hash -> WebDavSyncPlanEffect.NoChange
            remote != null && kind == WebDavBatchKind.Upload -> WebDavSyncPlanEffect.UploadOverwrite
            remote != null && kind == WebDavBatchKind.Download -> WebDavSyncPlanEffect.DownloadReplace
            remote != null -> WebDavSyncPlanEffect.MergeThenUpload
            kind == WebDavBatchKind.Download -> WebDavSyncPlanEffect.RemoteEmpty
            else -> WebDavSyncPlanEffect.CreateRemote
        }, workspaceIdentity = config.workspaceIdentity())
    }

    suspend fun sync(config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort) =
        execute(config, item, port, WebDavSyncAction.Sync)

    suspend fun download(
        config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort,
        expectedRemoteHash: String? = null, remoteKnownEmpty: Boolean = false,
        isCurrent: () -> Boolean = { true },
    ) = execute(config, item, port, WebDavSyncAction.Restore, expectedHash = expectedRemoteHash,
        remoteKnownEmpty = remoteKnownEmpty, isCurrent = isCurrent)

    suspend fun upload(
        config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort,
        expectedRemoteEtag: String? = null, remoteKnownEmpty: Boolean = false, confirmedOverwrite: Boolean = false,
        isCurrent: () -> Boolean = { true },
    ) = execute(config, item, port, WebDavSyncAction.Upload, expectedRemoteEtag,
        remoteKnownEmpty = remoteKnownEmpty, confirmedOverwrite = confirmedOverwrite, isCurrent = isCurrent)

    suspend fun uploadLocalChange(
        config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort,
        expectedRemoteEtag: String?, expectedRemoteHash: String?,
    ) = execute(config, item, port, WebDavSyncAction.LocalChange, expectedRemoteEtag, expectedRemoteHash)

    private suspend fun execute(
        config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort, action: WebDavSyncAction,
        expectedEtag: String? = null, expectedHash: String? = null,
        remoteKnownEmpty: Boolean = false, confirmedOverwrite: Boolean = false,
        isCurrent: () -> Boolean = { true },
    ): WebDavItemOutcome = operation {
        val result = try {
            check(metadataStore.acceptsConfig(config)) { "Connection changed" }
            val baseline = if (action == WebDavSyncAction.Sync) metadataStore.getItemContentHash(item) else expectedHash
            WebDavSyncSession(client(config)).execute(action, item.fileName, port,
                expectedEtag, baseline, remoteKnownEmpty, confirmedOverwrite, isCurrent)
        } catch (error: CancellationException) { throw error
        } catch (_: Exception) {
            WebDavSyncExecution(os.kei.feature.webdav.sync.WebDavSyncStatus.Error, "Invalid WebDAV configuration")
        }
        try {
            recordRemote(item, result)
            result.checkpoint?.let { snapshot ->
                metadataStore.recordCheckpoint(item, result.etag, snapshot.hash, nowMillis())
            }
            when (result.status) {
                os.kei.feature.webdav.sync.WebDavSyncStatus.ConflictUnresolved ->
                    metadataStore.setItemPendingState(item, WebDavSyncPendingState.RemoteConflict, nowMillis())
                os.kei.feature.webdav.sync.WebDavSyncStatus.BaselineRequired ->
                    metadataStore.setItemPendingState(item, WebDavSyncPendingState.BaselineRequired, nowMillis())
                else -> Unit
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@operation WebDavItemOutcome(WebDavItemStatus.Error, "Sync metadata could not be saved; retry this operation")
        }
        WebDavItemOutcome(WebDavItemStatus.valueOf(result.status.name), result.detail)
    }

    suspend fun probeRemote(
        config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort,
    ): WebDavRemoteProbeOutcome = operation {
        val result = inspect(config, item, port)
        recordRemote(item, result)
        val remote = result.remote
        when {
            remote != null -> WebDavRemoteProbeOutcome.Found(
                remote.itemCount, remote.content.toByteArray(Charsets.UTF_8).size.toLong(), result.etag,
            )
            result.missing -> WebDavRemoteProbeOutcome.Empty
            else -> WebDavRemoteProbeOutcome.Error(WebDavItemStatus.valueOf(result.status.name), result.detail)
        }
    }

    private suspend fun inspect(
        config: WebDavConfig, item: WebDavSyncItem, port: WebDavSyncDataPort, allowInvalid: Boolean = false,
    ) = try {
        check(metadataStore.acceptsConfig(config)) { "Connection changed" }
        WebDavSyncSession(client(config)).inspect(item.fileName, port, allowInvalid)
    } catch (error: CancellationException) { throw error
    } catch (_: Exception) {
        WebDavSyncExecution(os.kei.feature.webdav.sync.WebDavSyncStatus.Error, "Invalid WebDAV configuration")
    }

    private fun recordRemote(item: WebDavSyncItem, result: WebDavSyncExecution) {
        result.remote?.let { remote ->
            metadataStore.saveRemoteSummaryFound(item, remote.itemCount,
                remote.content.toByteArray(Charsets.UTF_8).size.toLong(), result.etag, nowMillis())
        }
        if (result.missing) metadataStore.saveRemoteSummaryEmpty(item, nowMillis())
    }

    companion object {
        fun contentHash(content: String) = os.kei.feature.webdav.sync.contentHash(content)
    }
}
