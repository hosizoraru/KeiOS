package os.kei.feature.webdav.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.webdav.client.*
import kotlin.test.*

class WebDavSyncSessionTest {
    private class Document(var value: String = "local") {
        var merges = 0
        var replacements = 0
        var failReplace = false
        val port = WebDavDocumentPort(
            exportJson = { value },
            merge = { remote -> merges++; value = (value.split('|') + remote.split('|')).distinct().sorted().joinToString("|") },
            replace = { raw -> replacements++; value = raw; if (failReplace && raw == "remote") error("store failure") },
            localCount = { if (value.isEmpty()) 0 else value.split('|').size },
            countRemoteItems = { if (it.isEmpty()) 0 else it.split('|').size },
            remoteFingerprintJson = { require(it != "corrupt"); it },
            mergeRemoteOnAutoConflict = true,
        )
    }
    private class Cloud(var content: String? = "remote", var etag: String? = "\"v1\"") : WebDavRemoteClient {
        var writes = 0
        var reads = 0
        var unconditional = 0
        var conflict = false
        var failure: WebDavError? = null
        var onWrite: () -> Unit = {}
        var onRead: () -> Unit = {}
        override suspend fun testConnection() = WebDavTestConnectionResult.Success(false)
        override suspend fun download(fileName: String): WebDavDownloadResult {
            reads++
            onRead()
            return failure?.let { WebDavDownloadResult.Error(it) }
                ?: content?.let { WebDavDownloadResult.Success(it, etag) } ?: WebDavDownloadResult.Empty
        }
        override suspend fun upload(fileName: String, content: String, etag: String?): WebDavUploadResult {
            writes++; if (etag == null) unconditional++
            if (conflict || (etag != null && etag != this.etag)) return WebDavUploadResult.Conflict
            this.content = content; this.etag = "\"v${writes + 1}\""; onWrite()
            return WebDavUploadResult.Success(this.etag)
        }
        override suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult =
            if (this.content != null) WebDavUploadResult.Conflict else upload(fileName, content, null)
    }

    @Test fun `confirmed upload followed by restore removes divergent local data`(): Unit = runBlocking {
        val cloud = Cloud(); val document = Document("backup")
        val session = WebDavSyncSession(cloud)
        assertEquals(WebDavSyncStatus.Uploaded, session.execute(WebDavSyncAction.Upload, "data.json", document.port, confirmedOverwrite = true).status)
        document.value = "broken|local-only"
        val restored = session.execute(WebDavSyncAction.Restore, "data.json", document.port)
        assertEquals(WebDavSyncStatus.Downloaded, restored.status)
        assertEquals("backup", document.value)
        assertEquals(0, document.merges)
        assertEquals(contentHash("backup"), restored.checkpoint?.hash)
    }

    @Test fun `valid empty dataset replaces rather than retains local entries`(): Unit = runBlocking {
        val document = Document(); val result = WebDavSyncSession(Cloud("")).execute(WebDavSyncAction.Restore, "data.json", document.port)
        assertEquals(WebDavSyncStatus.Downloaded, result.status)
        assertEquals("", document.value)
        assertEquals(0, result.checkpoint?.itemCount)
    }

    @Test fun `missing remote leaves local data and baseline untouched`(): Unit = runBlocking {
        val document = Document(); val result = WebDavSyncSession(Cloud(null)).execute(WebDavSyncAction.Restore, "data.json", document.port)
        assertEquals(WebDavSyncStatus.RemoteEmpty, result.status)
        assertEquals("local", document.value)
        assertNull(result.checkpoint)
    }

    @Test fun `corrupt backup cannot mutate local or establish a checkpoint`(): Unit = runBlocking {
        val document = Document(); val result = WebDavSyncSession(Cloud("corrupt")).execute(WebDavSyncAction.Restore, "data.json", document.port)
        assertEquals(WebDavSyncStatus.Error, result.status)
        assertEquals("local", document.value)
        assertEquals(0, document.replacements)
        assertNull(result.checkpoint)
    }

    @Test fun `failed restore rolls back the original local snapshot`(): Unit = runBlocking {
        val document = Document().apply { failReplace = true }
        val result = WebDavSyncSession(Cloud()).execute(WebDavSyncAction.Restore, "data.json", document.port)
        assertEquals(WebDavSyncStatus.Error, result.status)
        assertEquals("local", document.value)
        assertNull(result.checkpoint)
    }

    @Test fun `two way merge preserves both sides using a conditional write`(): Unit = runBlocking {
        val cloud = Cloud(); val document = Document()
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.Sync, "data.json", document.port)
        assertEquals(WebDavSyncStatus.Merged, result.status)
        assertEquals("local|remote", cloud.content)
        assertEquals(cloud.content, document.value)
        assertEquals(0, cloud.unconditional)
    }

    @Test fun `repeated 412 never becomes an unconditional automatic overwrite`(): Unit = runBlocking {
        val cloud = Cloud().apply { conflict = true }; val document = Document()
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.LocalChange, "data.json", document.port,
            expectedEtag = "\"old\"", expectedHash = contentHash("before"))
        assertEquals(WebDavSyncStatus.ConflictUnresolved, result.status)
        assertEquals(0, cloud.unconditional)
        assertEquals(4, cloud.writes)
        assertNull(result.checkpoint)
    }

    @Test fun `weak validators request manual review without modifying either copy`(): Unit = runBlocking {
        val cloud = Cloud(etag = "W/\"weak\""); val document = Document()
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.LocalChange, "data.json", document.port,
            expectedHash = contentHash("remote"))
        assertEquals(WebDavSyncStatus.ConflictUnresolved, result.status)
        assertEquals(0, cloud.writes)
        assertEquals("local", document.value)
    }

    @Test fun `explicit overwrite works even when conditional writes are unavailable`(): Unit = runBlocking {
        val cloud = Cloud(etag = null); val document = Document()
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.Upload, "data.json", document.port, confirmedOverwrite = true)
        assertEquals(WebDavSyncStatus.Uploaded, result.status)
        assertEquals("local", cloud.content)
        assertEquals(1, cloud.unconditional)
    }

    @Test fun `upload checkpoint describes the sent bytes even if local changes during the request`(): Unit = runBlocking {
        val cloud = Cloud(); val document = Document("sent")
        cloud.onWrite = { document.value = "edited-after-send" }
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.Upload, "data.json", document.port, confirmedOverwrite = true)
        assertEquals(contentHash("sent"), result.checkpoint?.hash)
        assertEquals("edited-after-send", document.value)
    }

    @Test fun `missing baseline cannot trigger an automatic first overwrite`(): Unit = runBlocking {
        val cloud = Cloud(); val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.LocalChange, "data.json", Document().port)
        assertEquals(WebDavSyncStatus.BaselineRequired, result.status)
        assertEquals(0, cloud.writes)
    }

    @Test fun `rate limit is distinct from absence and cannot create a checkpoint`(): Unit = runBlocking {
        val cloud = Cloud().apply { failure = WebDavError.Unknown(503, "cooldown") }
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.Sync, "data.json", Document().port)
        assertEquals(WebDavSyncStatus.RateLimited, result.status)
        assertNull(result.checkpoint)
        assertFalse(result.missing)
    }

    @Test fun `upload review can inspect an invalid backup while restore review blocks it`(): Unit = runBlocking {
        val session = WebDavSyncSession(Cloud("corrupt")); val document = Document()
        assertEquals(WebDavSyncStatus.Error, session.inspect("data.json", document.port).status)
        val upload = session.inspect("data.json", document.port, allowInvalid = true)
        assertEquals(-1, upload.remote?.itemCount)
        assertEquals("local", document.value)
    }

    @Test fun `cancellation escapes the sync boundary`(): Unit = runBlocking {
        val cloud = object : WebDavRemoteClient {
            override suspend fun testConnection() = throw CancellationException()
            override suspend fun download(fileName: String) = throw CancellationException()
            override suspend fun upload(fileName: String, content: String, etag: String?) = throw CancellationException()
            override suspend fun uploadIfAbsent(fileName: String, content: String) = throw CancellationException()
        }
        assertFailsWith<CancellationException> { WebDavSyncSession(cloud).execute(WebDavSyncAction.Sync, "data.json", Document().port) }
    }

    @Test fun `upload without a condition or confirmation cannot overwrite`(): Unit = runBlocking {
        val cloud = Cloud()
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.Upload, "data.json", Document().port)
        assertEquals(WebDavSyncStatus.ConflictUnresolved, result.status)
        assertEquals(0, cloud.writes)
    }

    @Test fun `edits while GET is in progress invalidate the restore confirmation`(): Unit = runBlocking {
        val document = Document()
        val cloud = Cloud().apply { onRead = { document.value = "edited-during-download" } }
        val result = WebDavSyncSession(cloud).execute(
            WebDavSyncAction.Restore, "data.json", document.port, isCurrent = { document.value == "local" },
        )
        assertEquals(WebDavSyncStatus.ConflictUnresolved, result.status)
        assertEquals(0, document.replacements)
        assertEquals("edited-during-download", document.value)
    }

    @Test fun `rollback failure cannot claim a successful restore or checkpoint`(): Unit = runBlocking {
        val document = Document()
        val broken = document.port.copy(replace = { error("store is unwritable") })
        val result = WebDavSyncSession(Cloud()).execute(WebDavSyncAction.Restore, "data.json", broken)
        assertEquals(WebDavSyncStatus.Error, result.status)
        assertTrue(result.detail.orEmpty().contains("could not be recovered"))
        assertNull(result.checkpoint)
    }

    @Test fun `cloud changes after the preview cannot restore an unreviewed snapshot`(): Unit = runBlocking {
        val document = Document()
        for (wasMissing in listOf(false, true)) {
            val result = WebDavSyncSession(Cloud("changed")).execute(
                WebDavSyncAction.Restore, "data.json", document.port,
                expectedHash = if (wasMissing) null else contentHash("reviewed"),
                remoteKnownEmpty = wasMissing,
            )
            assertEquals(WebDavSyncStatus.ConflictUnresolved, result.status)
            assertEquals(0, document.replacements)
            assertNull(result.checkpoint)
        }
    }

    @Test fun `manual sync cannot reapply an unchanged cloud value over a local edit`(): Unit = runBlocking {
        val document = Document("local-edit")
        val cloud = Cloud("baseline")
        val result = WebDavSyncSession(cloud).execute(
            WebDavSyncAction.Sync, "data.json", document.port, expectedHash = contentHash("baseline"),
        )
        assertEquals(WebDavSyncStatus.Uploaded, result.status)
        assertEquals(0, document.merges)
        assertEquals("local-edit", cloud.content)
        assertEquals("local-edit", document.value)
        assertEquals(0, cloud.unconditional)
    }

    @Test fun `a malformed validator list cannot become an OR condition for automatic writes`(): Unit = runBlocking {
        val cloud = Cloud(etag = "\"current\", \"older\"")
        val document = Document()
        val result = WebDavSyncSession(cloud).execute(WebDavSyncAction.Sync, "data.json", document.port)
        assertEquals(WebDavSyncStatus.ConflictUnresolved, result.status)
        assertEquals(0, cloud.writes)
        assertEquals(0, document.merges)
    }
}
