package os.kei.ui.page.main.sync

import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavUploadResult
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebDavSyncConditionalWriteTest {
    @Test fun `weak validator cannot trigger an unconditional automatic overwrite`(): Unit = runBlocking {
        val client = FakeWebDavSyncClientBridge(downloadResults = mutableListOf(WebDavDownloadResult.Success("remote", "W/\"weak\"")))
        val metadata = FakeWebDavSyncMetadataStore()
        val port = FakeWebDavSyncDataPort("local", mergeRemoteOnAutoConflict = true)
        val outcome = WebDavSyncEngine({ client }, metadata).uploadLocalChange(fakeConfig(), WebDavSyncItem.BaAccounts,
            port.port, null, WebDavSyncEngine.contentHash("remote"))
        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertTrue(client.uploadCalls.isEmpty())
        assertTrue(port.mergeCalls.isEmpty())
        assertTrue(metadata.hashes.isEmpty())
    }

    @Test fun `rejected strong validator never silently disables concurrency protection`(): Unit = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = MutableList(3) { WebDavDownloadResult.Success("remote-$it", "etag-same") },
            uploadResults = MutableList(4) { WebDavUploadResult.Conflict },
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val port = FakeWebDavSyncDataPort("local", mergeRemoteOnAutoConflict = true)
        val outcome = WebDavSyncEngine({ client }, metadata).uploadLocalChange(fakeConfig(), WebDavSyncItem.BaAccounts,
            port.port, "etag-old", WebDavSyncEngine.contentHash("before"))
        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(4, client.uploadCalls.size)
        assertTrue(client.uploadCalls.all { it.etag != null })
        assertTrue(metadata.hashes.isEmpty())
        assertEquals(WebDavSyncPendingState.RemoteConflict, metadata.pendingStates[WebDavSyncItem.BaAccounts])
    }

    @Test fun `server omitting refreshed validator requests review before local merge`(): Unit = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(WebDavDownloadResult.Success("new-remote", null)),
            uploadResults = mutableListOf(WebDavUploadResult.Conflict),
        )
        val port = FakeWebDavSyncDataPort("local", mergeRemoteOnAutoConflict = true)
        val outcome = WebDavSyncEngine({ client }, FakeWebDavSyncMetadataStore()).uploadLocalChange(
            fakeConfig(), WebDavSyncItem.BaAccounts, port.port, "etag-old", WebDavSyncEngine.contentHash("before"))
        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(listOf("etag-old"), client.uploadCalls.map { it.etag })
        assertTrue(port.mergeCalls.isEmpty())
    }

    @Test fun `explicit upload bypasses broken conditional support after user confirmation`(): Unit = runBlocking {
        val client = FakeWebDavSyncClientBridge()
        val port = FakeWebDavSyncDataPort("authoritative-local")
        val metadata = FakeWebDavSyncMetadataStore()
        val outcome = WebDavSyncEngine({ client }, metadata).upload(fakeConfig(), WebDavSyncItem.BaAccounts,
            port.port, expectedRemoteEtag = "W/\"weak\"", confirmedOverwrite = true)
        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertEquals(listOf<String?>(null), client.uploadCalls.map { it.etag })
        assertTrue(client.downloadCalls.isEmpty())
        assertEquals(WebDavSyncEngine.contentHash("authoritative-local"), metadata.hashes[WebDavSyncItem.BaAccounts])
    }
}
