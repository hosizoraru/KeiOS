package os.kei.ui.page.main.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import os.kei.feature.webdav.client.WebDavDownloadResult
import os.kei.feature.webdav.client.WebDavTestConnectionResult
import os.kei.feature.webdav.client.WebDavUploadResult
import os.kei.feature.webdav.model.WebDavConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDavSyncEngineTest {
    @Test
    fun `sync operations across engine instances serialize remote writes`() = runBlocking {
        val firstDownloadStarted = CompletableDeferred<Unit>()
        val releaseFirstDownload = CompletableDeferred<Unit>()
        val secondDownloadStarted = CompletableDeferred<Unit>()
        val firstClient = object : WebDavSyncClientBridge {
            override suspend fun testConnection(): WebDavTestConnectionResult =
                WebDavTestConnectionResult.Success(dirCreated = false)

            override suspend fun upload(
                fileName: String,
                content: String,
                etag: String?,
            ): WebDavUploadResult = WebDavUploadResult.Success("etag-first")

            override suspend fun uploadIfAbsent(
                fileName: String,
                content: String,
            ): WebDavUploadResult = WebDavUploadResult.Success("etag-first")

            override suspend fun download(fileName: String): WebDavDownloadResult {
                firstDownloadStarted.complete(Unit)
                releaseFirstDownload.await()
                return WebDavDownloadResult.Success("same", "etag-first")
            }
        }
        val secondClient = object : WebDavSyncClientBridge {
            override suspend fun testConnection(): WebDavTestConnectionResult =
                WebDavTestConnectionResult.Success(dirCreated = false)

            override suspend fun upload(
                fileName: String,
                content: String,
                etag: String?,
            ): WebDavUploadResult = WebDavUploadResult.Success("etag-second")

            override suspend fun uploadIfAbsent(
                fileName: String,
                content: String,
            ): WebDavUploadResult = WebDavUploadResult.Success("etag-second")

            override suspend fun download(fileName: String): WebDavDownloadResult {
                secondDownloadStarted.complete(Unit)
                return WebDavDownloadResult.Success("same", "etag-second")
            }
        }
        val firstEngine = WebDavSyncEngine(clientFactory = { firstClient }, metadataStore = FakeWebDavSyncMetadataStore())
        val secondEngine = WebDavSyncEngine(clientFactory = { secondClient }, metadataStore = FakeWebDavSyncMetadataStore())
        val firstPort = FakeWebDavSyncDataPort(localJson = "same")
        val secondPort = FakeWebDavSyncDataPort(localJson = "same")

        val first = async(Dispatchers.Default) {
            firstEngine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, firstPort.port)
        }
        withTimeout(1_000L) { firstDownloadStarted.await() }
        val second = async(Dispatchers.Default) {
            secondEngine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, secondPort.port)
        }

        assertNull(withTimeoutOrNull(150L) { secondDownloadStarted.await() })

        releaseFirstDownload.complete(Unit)
        withTimeout(1_000L) {
            first.await()
            second.await()
        }
        assertTrue(secondDownloadStarted.isCompleted)
    }

    @Test
    fun `sync returns up to date when local and remote content hashes match`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"items":[1]}""", "etag-1"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 1234L)
        val port = FakeWebDavSyncDataPort(localJson = """{"items":[1]}""")

        val outcome = engine.sync(fakeConfig(), WebDavSyncItem.GitHubTracked, port.port)

        assertEquals(WebDavItemStatus.UpToDate, outcome.status)
        assertEquals(0, port.mergeCalls.size)
        assertEquals(0, client.uploadCalls.size)
        assertEquals("etag-1", metadata.etags[WebDavSyncItem.GitHubTracked])
        assertEquals(1234L, metadata.lastSyncTimes[WebDavSyncItem.GitHubTracked])
    }

    @Test
    fun `sync conflict retry re merges latest remote payload before second upload`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("remote-v1", "etag-1"),
                WebDavDownloadResult.Success("remote-v2", "etag-2"),
            ),
            uploadResults = mutableListOf(
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Success("etag-3"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val port = FakeWebDavSyncDataPort(localJson = "local")
        val engine = newEngine(client, metadata, now = 777L)

        val outcome = engine.sync(fakeConfig(), WebDavSyncItem.OsShellCards, port.port)

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(listOf("remote-v1", "remote-v2"), port.mergeCalls)
        assertEquals(2, client.uploadCalls.size)
        assertEquals("merge(remote-v1)", client.uploadCalls[0].content)
        assertEquals("merge(remote-v1)+merge(remote-v2)", client.uploadCalls[1].content)
        assertEquals("etag-2", client.uploadCalls[1].etag)
        assertEquals("etag-3", metadata.etags[WebDavSyncItem.OsShellCards])
        assertEquals(777L, metadata.lastSyncTimes[WebDavSyncItem.OsShellCards])
    }

    @Test
    fun `sync creates empty remote with create only write`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(WebDavDownloadResult.Empty),
            uploadIfAbsentResults = mutableListOf(WebDavUploadResult.Success("etag-new")),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val port = FakeWebDavSyncDataPort(localJson = """{"items":[1]}""")
        val engine = newEngine(client, metadata, now = 5678L)

        val outcome = engine.sync(fakeConfig(), WebDavSyncItem.GitHubTracked, port.port)

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertEquals(1, client.uploadIfAbsentCalls.size)
        assertTrue(client.uploadCalls.isEmpty())
        assertEquals("etag-new", metadata.etags[WebDavSyncItem.GitHubTracked])
        assertEquals(5678L, metadata.lastSyncTimes[WebDavSyncItem.GitHubTracked])
    }

    @Test
    fun `sync empty remote conflict re downloads and merges latest remote`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Empty,
                WebDavDownloadResult.Success("remote-after-empty-preview", "etag-after"),
            ),
            uploadIfAbsentResults = mutableListOf(WebDavUploadResult.Conflict),
            uploadResults = mutableListOf(WebDavUploadResult.Success("etag-merged")),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val port = FakeWebDavSyncDataPort(localJson = "local")
        val engine = newEngine(client, metadata, now = 6789L)

        val outcome = engine.sync(fakeConfig(), WebDavSyncItem.OsActivityCards, port.port)

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(1, client.uploadIfAbsentCalls.size)
        assertEquals(listOf("remote-after-empty-preview"), port.mergeCalls)
        assertEquals("merge(remote-after-empty-preview)", client.uploadCalls.single().content)
        assertEquals("etag-after", client.uploadCalls.single().etag)
        assertEquals("etag-merged", metadata.etags[WebDavSyncItem.OsActivityCards])
        assertEquals(6789L, metadata.lastSyncTimes[WebDavSyncItem.OsActivityCards])
    }

    @Test
    fun `probe remote found stores parsed remote summary`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"items":[1,2,3]}""", "etag-9"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 999L)
        val port = FakeWebDavSyncDataPort(
            localJson = "unused",
            remoteItemCount = 3,
        )

        val outcome = engine.probeRemote(fakeConfig(), WebDavSyncItem.BaCatalogFavorites, port.port)

        assertTrue(outcome is WebDavRemoteProbeOutcome.Found)
        assertEquals(3, outcome.itemCount)
        assertEquals(999L, metadata.remoteFound[WebDavSyncItem.BaCatalogFavorites]?.probedAtMs)
        assertEquals("etag-9", metadata.remoteFound[WebDavSyncItem.BaCatalogFavorites]?.etag)
        assertEquals(3, metadata.remoteFound[WebDavSyncItem.BaCatalogFavorites]?.itemCount)
    }

    @Test
    fun `prepare upload plan refreshes remote and marks shrink risk`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"items":[1,2,3]}""", "etag-remote"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 2468L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"items":[1]}""",
            localCount = 1,
            remoteItemCount = 3,
        )

        val planItem =
            engine.prepareChange(
                config = fakeConfig(),
                kind = WebDavBatchKind.Upload,
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
            )

        assertEquals(WebDavSyncPlanEffect.UploadOverwrite, planItem.effect)
        assertTrue(planItem.remoteState is WebDavSyncPlanRemoteState.Found)
        assertTrue(planItem.shrinksRemote)
        assertEquals("etag-remote", planItem.remoteEtag)
        assertEquals(2468L, metadata.remoteFound[WebDavSyncItem.GitHubTracked]?.probedAtMs)
        assertEquals(3, metadata.remoteFound[WebDavSyncItem.GitHubTracked]?.itemCount)
    }

    @Test
    fun `planned upload uses refreshed remote etag for conditional write`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            uploadResults = mutableListOf(WebDavUploadResult.Success("etag-after")),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 9999L)
        val port = FakeWebDavSyncDataPort(localJson = """{"items":[1]}""")

        val outcome =
            engine.upload(
                config = fakeConfig(),
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
                expectedRemoteEtag = "etag-before",
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertEquals("etag-before", client.uploadCalls.single().etag)
        assertEquals("etag-after", metadata.etags[WebDavSyncItem.GitHubTracked])
    }

    @Test
    fun `planned upload returns conflict when remote changed after preview`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            uploadResults = mutableListOf(WebDavUploadResult.Conflict),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 9999L)
        val port = FakeWebDavSyncDataPort(localJson = """{"items":[1]}""")

        val outcome =
            engine.upload(
                config = fakeConfig(),
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
                expectedRemoteEtag = "etag-before",
            )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals("etag-before", client.uploadCalls.single().etag)
        assertTrue(metadata.etags.isEmpty())
    }

    @Test
    fun `confirmed upload writes local authority without a rejected conditional preflight`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"items":[1,2]}""", "etag-refreshed"),
            ),
            uploadResults = mutableListOf(
                WebDavUploadResult.Success("etag-after"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 10_000L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"items":[1,2,3]}""",
            localCount = 3,
            remoteItemCount = 2,
        )

        val outcome =
            engine.upload(
                config = fakeConfig(),
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
                expectedRemoteEtag = "etag-preview",
                confirmedOverwrite = true,
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertEquals(listOf<String?>(null), client.uploadCalls.map { it.etag })
        assertEquals("""{"items":[1,2,3]}""", client.uploadCalls.last().content)
        assertEquals("etag-after", metadata.etags[WebDavSyncItem.GitHubTracked])
        assertEquals(3, metadata.remoteFound[WebDavSyncItem.GitHubTracked]?.itemCount)
        assertEquals("etag-after", metadata.remoteFound[WebDavSyncItem.GitHubTracked]?.etag)
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `confirmed upload keeps conflict when provider rejects final overwrite`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"items":[1,2]}""", "etag-refreshed"),
            ),
            uploadResults = mutableListOf(
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Conflict,
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 10_500L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"items":[1,2,3]}""",
            localCount = 3,
            remoteItemCount = 2,
        )

        val outcome =
            engine.upload(
                config = fakeConfig(),
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
                expectedRemoteEtag = "etag-preview",
                confirmedOverwrite = true,
            )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(listOf<String?>(null), client.uploadCalls.map { it.etag })
        assertTrue(metadata.etags.isEmpty())
        assertEquals(WebDavSyncPendingState.RemoteConflict, metadata.pendingStates[WebDavSyncItem.GitHubTracked])
    }

    @Test
    fun `confirmed upload intentionally replaces remote even when the preview was empty`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(WebDavDownloadResult.Empty),
            uploadIfAbsentResults = mutableListOf(WebDavUploadResult.Conflict),
            uploadResults = mutableListOf(WebDavUploadResult.Success("etag-after")),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 11_000L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"items":[1]}""",
            localCount = 1,
        )

        val outcome =
            engine.upload(
                config = fakeConfig(),
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
                remoteKnownEmpty = true,
                confirmedOverwrite = true,
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertTrue(client.uploadIfAbsentCalls.isEmpty())
        assertEquals(listOf(null), client.uploadCalls.map { it.etag })
        assertEquals("etag-after", metadata.etags[WebDavSyncItem.GitHubTracked])
        assertEquals(1, metadata.remoteFound[WebDavSyncItem.GitHubTracked]?.itemCount)
        assertTrue(client.downloadCalls.isEmpty())
    }

    @Test
    fun `planned upload uses create only write when preview saw empty remote`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            uploadIfAbsentResults = mutableListOf(WebDavUploadResult.Conflict),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 9999L)
        val port = FakeWebDavSyncDataPort(localJson = """{"items":[1]}""")

        val outcome =
            engine.upload(
                config = fakeConfig(),
                item = WebDavSyncItem.GitHubTracked,
                port = port.port,
                remoteKnownEmpty = true,
            )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertEquals(1, client.uploadIfAbsentCalls.size)
        assertTrue(client.uploadCalls.isEmpty())
        assertTrue(metadata.etags.isEmpty())
    }

    @Test
    fun `auto local change upload uses previous etag and never merges stale remote into local`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            uploadResults = mutableListOf(WebDavUploadResult.Success("etag-new")),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 12_345L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"local-new","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"local-new"}""",
            remoteFingerprintJson = { raw ->
                if ("local-new" in raw) """{"value":"local-new"}""" else raw
            },
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = "etag-old",
                expectedRemoteHash = WebDavSyncEngine.contentHash("""{"value":"remote-old"}"""),
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertEquals("""{"value":"local-new","exportedAtMs":2}""", client.uploadCalls.single().content)
        assertEquals("etag-old", client.uploadCalls.single().etag)
        assertEquals("etag-new", metadata.etags[WebDavSyncItem.BaAccounts])
        assertEquals(WebDavSyncEngine.contentHash("""{"value":"local-new"}"""), metadata.hashes[WebDavSyncItem.BaAccounts])
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `auto local change upload reports conflict without mutating local when remote etag changed`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"value":"remote-new"}""", "etag-new"),
            ),
            uploadResults = mutableListOf(WebDavUploadResult.Conflict),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 22_222L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"local-new","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"local-new"}""",
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = "etag-old",
                expectedRemoteHash = WebDavSyncEngine.contentHash("""{"value":"remote-old"}"""),
            )

        assertEquals(WebDavItemStatus.ConflictUnresolved, outcome.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertTrue(metadata.hashes.isEmpty())
        assertEquals(WebDavSyncPendingState.RemoteConflict, metadata.pendingStates[WebDavSyncItem.BaAccounts])
    }

    @Test
    fun `auto local change upload merges refreshed remote when port allows auto conflict merge`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"account":"device-1"}""", "etag-device-1"),
            ),
            uploadResults = mutableListOf(
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Success("etag-merged"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 22_250L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"account":"device-2"}""",
            mergeRemoteOnAutoConflict = true,
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = "etag-base",
                expectedRemoteHash = WebDavSyncEngine.contentHash("""{"account":"base"}"""),
            )

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(listOf("""{"account":"device-1"}"""), port.mergeCalls)
        assertEquals(2, client.uploadCalls.size)
        assertEquals("etag-base", client.uploadCalls[0].etag)
        assertEquals("etag-device-1", client.uploadCalls[1].etag)
        assertEquals("""{"account":"device-2"}+merge({"account":"device-1"})""", client.uploadCalls[1].content)
        assertEquals("etag-merged", metadata.etags[WebDavSyncItem.BaAccounts])
        assertEquals(
            WebDavSyncEngine.contentHash("""{"account":"device-2"}+merge({"account":"device-1"})"""),
            metadata.hashes[WebDavSyncItem.BaAccounts],
        )
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `auto local change upload remerges when remote changes repeatedly`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"account":"device-1"}""", "etag-device-1"),
                WebDavDownloadResult.Success("""{"account":"device-3"}""", "etag-device-3"),
                WebDavDownloadResult.Success("""{"account":"device-4"}""", "etag-device-4"),
            ),
            uploadResults = mutableListOf(
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Success("etag-final"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 22_300L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"account":"device-2"}""",
            mergeRemoteOnAutoConflict = true,
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = "etag-base",
                expectedRemoteHash = WebDavSyncEngine.contentHash("""{"account":"base"}"""),
            )

        assertEquals(WebDavItemStatus.Merged, outcome.status)
        assertEquals(
            listOf("etag-base", "etag-device-1", "etag-device-3", "etag-device-4"),
            client.uploadCalls.map { it.etag },
        )
        assertEquals(
            listOf(
                """{"account":"device-1"}""",
                """{"account":"device-3"}""",
                """{"account":"device-4"}""",
            ),
            port.mergeCalls,
        )
        // Each retry uploads the payload re-exported after every merge so far, not the first merge.
        assertEquals(
            """{"account":"device-2"}+merge({"account":"device-1"})+merge({"account":"device-3"})""" +
                """+merge({"account":"device-4"})""",
            client.uploadCalls.last().content,
        )
        assertEquals("etag-final", metadata.etags[WebDavSyncItem.BaAccounts])
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `successful upload records the content snapshot that was sent`() = runBlocking {
        lateinit var port: FakeWebDavSyncDataPort
        val client = FakeWebDavSyncClientBridge(
            uploadResults = mutableListOf(WebDavUploadResult.Success("etag-sent")),
            onUpload = { _, _, _ -> port.replaceLocalJson("local-after-upload-started") },
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 22_325L)
        port = FakeWebDavSyncDataPort(localJson = "local-sent")

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = "etag-before",
                expectedRemoteHash = WebDavSyncEngine.contentHash("remote-before"),
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertEquals("local-sent", client.uploadCalls.single().content)
        assertEquals(
            WebDavSyncEngine.contentHash("local-sent"),
            metadata.hashes[WebDavSyncItem.BaAccounts],
        )
    }

    @Test
    fun `auto local change upload adopts refreshed etag when conflict remote already matches local fingerprint`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"value":"same","exportedAtMs":1}""", "etag-refreshed"),
            ),
            uploadResults = mutableListOf(WebDavUploadResult.Conflict),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 22_333L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"same","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"same"}""",
            remoteFingerprintJson = { """{"value":"same"}""" },
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.OsActivityCards,
                port = port.port,
                expectedRemoteEtag = "etag-stale",
                expectedRemoteHash = WebDavSyncEngine.contentHash("""{"value":"old-algorithm"}"""),
            )

        assertEquals(WebDavItemStatus.UpToDate, outcome.status)
        assertEquals(1, client.uploadCalls.size)
        assertEquals("etag-stale", client.uploadCalls.single().etag)
        assertEquals("etag-refreshed", metadata.etags[WebDavSyncItem.OsActivityCards])
        assertEquals(
            WebDavSyncEngine.contentHash("""{"value":"same"}"""),
            metadata.hashes[WebDavSyncItem.OsActivityCards],
        )
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `auto local change upload refreshes etag when conflict keeps the same remote hash`() = runBlocking {
        val remoteFingerprint = """{"value":"remote-old"}"""
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"value":"remote-old","exportedAtMs":1}""", "etag-refreshed"),
            ),
            uploadResults = mutableListOf(
                WebDavUploadResult.Conflict,
                WebDavUploadResult.Success("etag-new"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 23_000L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"local-new","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"local-new"}""",
            remoteFingerprintJson = { raw ->
                if ("local-new" in raw) """{"value":"local-new"}""" else remoteFingerprint
            },
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = "etag-stale",
                expectedRemoteHash = WebDavSyncEngine.contentHash(remoteFingerprint),
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertEquals(listOf("etag-stale", "etag-refreshed"), client.uploadCalls.map { it.etag })
        assertEquals("etag-new", metadata.etags[WebDavSyncItem.BaAccounts])
        assertEquals(WebDavSyncEngine.contentHash("""{"value":"local-new"}"""), metadata.hashes[WebDavSyncItem.BaAccounts])
        assertTrue(metadata.pendingStates.isEmpty())
    }

    @Test
    fun `auto local change upload requires a baseline when no previous etag or hash exists`() = runBlocking {
        val client = FakeWebDavSyncClientBridge()
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 24_000L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"local-new","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"local-new"}""",
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = null,
                expectedRemoteHash = null,
            )

        assertEquals(WebDavItemStatus.BaselineRequired, outcome.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertTrue(client.downloadResults.isEmpty())
        assertTrue(client.uploadCalls.isEmpty())
        assertTrue(client.uploadIfAbsentCalls.isEmpty())
        assertTrue(metadata.hashes.isEmpty())
        assertEquals(WebDavSyncPendingState.BaselineRequired, metadata.pendingStates[WebDavSyncItem.BaAccounts])
    }

    @Test
    fun `auto local change upload validates remote hash when etag is missing`() = runBlocking {
        val remoteFingerprint = """{"value":"remote-old"}"""
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"value":"remote-old","exportedAtMs":1}""", "etag-refreshed"),
            ),
            uploadResults = mutableListOf(WebDavUploadResult.Success("etag-new")),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 25_000L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"local-new","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"local-new"}""",
            remoteFingerprintJson = { raw ->
                if ("local-new" in raw) """{"value":"local-new"}""" else remoteFingerprint
            },
        )

        val outcome =
            engine.uploadLocalChange(
                config = fakeConfig(),
                item = WebDavSyncItem.BaAccounts,
                port = port.port,
                expectedRemoteEtag = null,
                expectedRemoteHash = WebDavSyncEngine.contentHash(remoteFingerprint),
            )

        assertEquals(WebDavItemStatus.Uploaded, outcome.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertEquals(1, client.uploadCalls.size)
        assertEquals("etag-refreshed", client.uploadCalls.single().etag)
        assertEquals("etag-new", metadata.etags[WebDavSyncItem.BaAccounts])
    }

    @Test
    fun `sync compares stable fingerprints instead of export timestamp metadata`() = runBlocking {
        val client = FakeWebDavSyncClientBridge(
            downloadResults = mutableListOf(
                WebDavDownloadResult.Success("""{"value":"same","exportedAtMs":1}""", "etag-1"),
            ),
        )
        val metadata = FakeWebDavSyncMetadataStore()
        val engine = newEngine(client, metadata, now = 33_333L)
        val port = FakeWebDavSyncDataPort(
            localJson = """{"value":"same","exportedAtMs":2}""",
            localFingerprintJson = """{"value":"same"}""",
            remoteFingerprintJson = { """{"value":"same"}""" },
        )

        val outcome = engine.sync(fakeConfig(), WebDavSyncItem.BaAccounts, port.port)

        assertEquals(WebDavItemStatus.UpToDate, outcome.status)
        assertTrue(port.mergeCalls.isEmpty())
        assertTrue(client.uploadCalls.isEmpty())
        assertEquals(WebDavSyncEngine.contentHash("""{"value":"same"}"""), metadata.hashes[WebDavSyncItem.BaAccounts])
    }
}

private fun newEngine(
    client: FakeWebDavSyncClientBridge,
    metadata: FakeWebDavSyncMetadataStore,
    now: Long,
): WebDavSyncEngine = WebDavSyncEngine(clientFactory = { client }, metadataStore = metadata, nowMillis = { now })

internal fun fakeConfig() = WebDavConfig(
    serverUrl = "https://dav.example.com/dav/",
    username = "demo",
    appPassword = "secret",
    remoteDir = "KeiOS/",
)

internal class FakeWebDavSyncDataPort(
    private var localJson: String,
    private val localCount: Int = 0,
    private val remoteItemCount: Int = 0,
    private val localFingerprintJson: String? = null,
    private val remoteFingerprintJson: (String) -> String = { it },
    private val mergeRemoteOnAutoConflict: Boolean = false,
) {
    val mergeCalls = mutableListOf<String>()
    val replaceCalls = mutableListOf<String>()

    fun replaceLocalJson(value: String) {
        localJson = value
    }

    val port = WebDavSyncDataPort(
        replace = { raw -> replaceCalls += raw; localJson = raw },
        exportJson = { localJson },
        fingerprintJson = { localFingerprintJson ?: localJson },
        remoteFingerprintJson = remoteFingerprintJson,
        merge = { remote ->
            mergeCalls += remote
            localJson = if (localJson == "local") {
                "merge($remote)"
            } else {
                "$localJson+merge($remote)"
            }
        },
        localCount = { localCount },
        countRemoteItems = { remoteItemCount },
        mergeRemoteOnAutoConflict = mergeRemoteOnAutoConflict,
    )
}

internal class FakeWebDavSyncClientBridge(
    val downloadResults: MutableList<WebDavDownloadResult> = mutableListOf(),
    val uploadResults: MutableList<WebDavUploadResult> = mutableListOf(),
    val uploadIfAbsentResults: MutableList<WebDavUploadResult> = mutableListOf(),
    private val onUpload: (fileName: String, content: String, etag: String?) -> Unit = { _, _, _ -> },
) : WebDavSyncClientBridge {
    data class UploadCall(val fileName: String, val content: String, val etag: String?)
    data class UploadIfAbsentCall(val fileName: String, val content: String)

    val uploadCalls = mutableListOf<UploadCall>()
    val uploadIfAbsentCalls = mutableListOf<UploadIfAbsentCall>()
    val downloadCalls = mutableListOf<String>()

    override suspend fun testConnection(): WebDavTestConnectionResult =
        WebDavTestConnectionResult.Success(dirCreated = false)

    override suspend fun upload(
        fileName: String,
        content: String,
        etag: String?,
    ): WebDavUploadResult {
        uploadCalls += UploadCall(fileName, content, etag)
        onUpload(fileName, content, etag)
        return uploadResults.removeFirstOrNull() ?: WebDavUploadResult.Success("etag-default")
    }

    override suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult {
        uploadIfAbsentCalls += UploadIfAbsentCall(fileName, content)
        return uploadIfAbsentResults.removeFirstOrNull() ?: WebDavUploadResult.Success("etag-default")
    }

    override suspend fun download(fileName: String): WebDavDownloadResult {
        downloadCalls += fileName
        return downloadResults.removeFirst()
    }
}

internal class FakeWebDavSyncMetadataStore : WebDavSyncMetadataStore {
    data class RemoteSummaryRecord(
        val itemCount: Int,
        val byteSize: Long,
        val etag: String?,
        val probedAtMs: Long,
    )

    val etags = mutableMapOf<WebDavSyncItem, String?>()
    val hashes = mutableMapOf<WebDavSyncItem, String>()
    val lastSyncTimes = mutableMapOf<WebDavSyncItem, Long>()
    val remoteFound = mutableMapOf<WebDavSyncItem, RemoteSummaryRecord>()
    val remoteEmpty = mutableMapOf<WebDavSyncItem, Long>()
    val pendingStates = mutableMapOf<WebDavSyncItem, WebDavSyncPendingState>()

    override fun setItemEtag(item: WebDavSyncItem, etag: String?) {
        etags[item] = etag
    }

    override fun setItemContentHash(item: WebDavSyncItem, hash: String) {
        hashes[item] = hash
    }

    override fun setLastSyncTime(item: WebDavSyncItem, timeMs: Long) {
        lastSyncTimes[item] = timeMs
    }

    override fun setItemPendingState(
        item: WebDavSyncItem,
        state: WebDavSyncPendingState,
        updatedAtMs: Long,
    ) {
        pendingStates[item] = state
    }

    override fun clearItemPendingState(item: WebDavSyncItem) {
        pendingStates.remove(item)
    }

    override fun saveRemoteSummaryFound(
        item: WebDavSyncItem,
        itemCount: Int,
        byteSize: Long,
        etag: String?,
        probedAtMs: Long,
    ) {
        remoteFound[item] = RemoteSummaryRecord(itemCount, byteSize, etag, probedAtMs)
    }

    override fun saveRemoteSummaryEmpty(item: WebDavSyncItem, probedAtMs: Long) {
        remoteEmpty[item] = probedAtMs
    }
}
