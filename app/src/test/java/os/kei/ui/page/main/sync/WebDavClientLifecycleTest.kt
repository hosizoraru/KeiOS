package os.kei.ui.page.main.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.client.*
import kotlin.test.*

class WebDavClientLifecycleTest {
    @Test fun `the app supplies its persisted baseline to preserve local edits during manual sync`(): Unit = runBlocking {
        val base = FakeWebDavSyncMetadataStore()
        val metadata = object : WebDavSyncMetadataStore by base {
            override fun getItemContentHash(item: WebDavSyncItem) = WebDavSyncEngine.contentHash("baseline")
        }
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(WebDavDownloadResult.Success("baseline", "etag")),
        )
        val port = FakeWebDavSyncDataPort("local-edit")
        val engine = WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata)
        val result = engine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, port.port)
        assertEquals(WebDavItemStatus.Uploaded, result.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertEquals("local-edit", client.uploadCalls.single().content)
        engine.close()
    }

    @Test fun `invalidate closes an idle client and a new operation constructs a fresh one`(): Unit = runBlocking {
        var created = 0; var closed = 0
        val engine = WebDavSyncEngine(clientFactory = {
            created++
            object : WebDavSyncClientBridge {
                override suspend fun testConnection() = WebDavTestConnectionResult.Success(false)
                override suspend fun download(fileName: String) = WebDavDownloadResult.Success("same", "etag")
                override suspend fun upload(fileName: String, content: String, etag: String?) = error("unexpected upload")
                override suspend fun uploadIfAbsent(fileName: String, content: String) = error("unexpected creation")
                override fun close() { closed++ }
            }
        }, metadataStore = FakeWebDavSyncMetadataStore())
        val port = FakeWebDavSyncDataPort("same").port
        engine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, port)
        engine.invalidate()
        assertEquals(1, closed)
        engine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, port)
        assertEquals(2, created)
        engine.invalidate()
        assertEquals(2, closed)
    }

    @Test fun `closing an owner during GET defers closure and prevents future client reuse`(): Unit = runBlocking {
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        var closed = 0
        val engine = WebDavSyncEngine(clientFactory = {
            object : WebDavSyncClientBridge {
                override suspend fun testConnection() = WebDavTestConnectionResult.Success(false)
                override suspend fun download(fileName: String): WebDavDownloadResult {
                    started.complete(Unit); finish.await()
                    return WebDavDownloadResult.Success("same", "etag")
                }
                override suspend fun upload(fileName: String, content: String, etag: String?) = error("unexpected upload")
                override suspend fun uploadIfAbsent(fileName: String, content: String) = error("unexpected creation")
                override fun close() { closed++ }
            }
        }, metadataStore = FakeWebDavSyncMetadataStore())
        val pending = async { engine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, FakeWebDavSyncDataPort("same").port) }
        started.await(); engine.close()
        assertEquals(0, closed)
        finish.complete(Unit)
        assertEquals(WebDavItemStatus.UpToDate, pending.await().status)
        assertEquals(1, closed)
        assertEquals(WebDavItemStatus.Error,
            engine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, FakeWebDavSyncDataPort("same").port).status)
    }
}
