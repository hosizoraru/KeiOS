package os.kei.ui.page.main.sync

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import os.kei.feature.webdav.model.workspaceIdentity
import os.kei.ui.page.main.student.GuideBgmFavoriteStore
import kotlin.test.*

class WebDavPayloadValidationTest {
    @Test fun `the six existing empty export envelopes remain restorable`() {
        WebDavSyncItem.entries.forEach { item ->
            val key = when (item) {
                WebDavSyncItem.BaAccounts -> "accounts"
                WebDavSyncItem.BaCatalogFavorites, WebDavSyncItem.BaBgmFavorites -> "favorites"
                else -> "items"
            }
            validateWebDavPayload(item, """{"$key":[]}""")
        }
    }

    @Test fun `missing foreign malformed and future datasets cannot become an empty restore`() {
        WebDavSyncItem.entries.forEach { item ->
            for (raw in listOf("{}", "null", "<html>login</html>", """{"items":[null]}""", """{"items":[],"accounts":[],"favorites":[],"schemaVersion":999}""")) {
                assertFails("$item must reject $raw") { validateWebDavPayload(item, raw) }
            }
        }
    }

    @Test fun `legacy BGM timestamps have a deterministic fingerprint`() {
        val raw = """{"favorites":[{"audioUrl":"https://example.test/one.mp3","title":"One"}]}"""
        val first = GuideBgmFavoriteStore.buildFavoritesFingerprintJson(raw)
        val second = GuideBgmFavoriteStore.buildFavoritesFingerprintJson(raw)
        assertEquals(first, second)
        assertTrue(first.contains("\"favoritedAtMs\":1"))
    }

    @Test fun `changing local data after preview blocks an overwrite before invoking the engine`(): Unit = runBlocking {
        val port = FakeWebDavSyncDataPort("changed").port
        val plan = WebDavSyncPlanItem(WebDavSyncItem.BaAccounts, 0, WebDavSyncEngine.contentHash("before"),
            WebDavSyncPlanRemoteState.Empty, WebDavSyncPlanEffect.CreateRemote)
        val result = WebDavSyncRepository(ioDispatcher = Dispatchers.Unconfined).invoke(
            WebDavBatchKind.Upload, fakeConfig(), WebDavSyncItem.BaAccounts, port, plan)
        assertEquals(WebDavItemStatus.ConflictUnresolved, result.status)
    }

    @Test fun `switching workspace after preview blocks applying that preview to another cloud`(): Unit = runBlocking {
        val port = FakeWebDavSyncDataPort("same").port
        val plan = WebDavSyncPlanItem(WebDavSyncItem.BaAccounts, 0, WebDavSyncEngine.contentHash("same"),
            WebDavSyncPlanRemoteState.Empty, WebDavSyncPlanEffect.CreateRemote, fakeConfig().workspaceIdentity())
        val result = WebDavSyncRepository(ioDispatcher = Dispatchers.Unconfined).invoke(
            WebDavBatchKind.Download, fakeConfig().copy(remoteDir = "Other/"), WebDavSyncItem.BaAccounts, port, plan)
        assertEquals(WebDavItemStatus.ConflictUnresolved, result.status)
    }
}
