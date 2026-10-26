package os.kei.ui.page.main.sync

import org.junit.Test
import kotlin.test.*

class WebDavSyncPersistenceTest {
    @Test fun `a new workspace clears baselines probes and pending state but keeps enabled preferences`() {
        val store = WebDavSyncPersistence(MemoryPreferences())
        store.saveConfig(fakeConfig(), WebDavProvider.Custom)
        store.setAutoSyncEnabled(true)
        store.setItemEnabled(WebDavSyncItem.BaAccounts, false)
        WebDavSyncItem.entries.forEach {
            store.recordCheckpoint(it, "etag", "hash", 100)
            store.saveRemoteSummaryFound(it, 2, 20, "etag", 100)
            store.setItemPendingState(it, WebDavSyncPendingState.RemoteConflict, 100)
            store.setItemFingerprintRevision(it, 3)
        }
        store.saveConfig(fakeConfig().copy(remoteDir = "another/"), WebDavProvider.Custom)
        WebDavSyncItem.entries.forEach {
            assertNull(store.getItemEtag(it)); assertNull(store.getItemContentHash(it))
            assertEquals(0L, store.getLastSyncTime(it)); assertNull(store.loadRemoteSummary(it))
            assertNull(store.loadItemPendingSummary(it)); assertEquals(1, store.getItemFingerprintRevision(it))
        }
        assertFalse(store.isItemEnabled(WebDavSyncItem.BaAccounts))
        assertTrue(store.isAutoSyncEnabled())
    }

    @Test fun `password rotation retains a persisted baseline across fresh store instances`() {
        val disk = MemoryPreferences()
        val before = WebDavSyncPersistence(disk)
        before.saveConfig(fakeConfig(), WebDavProvider.Custom)
        before.recordCheckpoint(WebDavSyncItem.BaAccounts, "etag", "hash", 123)
        before.saveConfig(fakeConfig().copy(appPassword = "new-password"), WebDavProvider.Custom)
        val after = WebDavSyncPersistence(disk)
        assertEquals("etag", after.getItemEtag(WebDavSyncItem.BaAccounts))
        assertEquals("hash", after.getItemContentHash(WebDavSyncItem.BaAccounts))
        assertEquals(123L, after.getLastSyncTime(WebDavSyncItem.BaAccounts))
    }

    @Test fun `a failed checkpoint write preserves the entire previous baseline`() {
        val disk = MemoryPreferences(); val store = WebDavSyncPersistence(disk)
        store.recordCheckpoint(WebDavSyncItem.BaAccounts, "old", "old-hash", 1)
        disk.failWrites = true
        assertFails { store.recordCheckpoint(WebDavSyncItem.BaAccounts, "new", "new-hash", 2) }
        assertEquals("old", store.getItemEtag(WebDavSyncItem.BaAccounts))
        assertEquals("old-hash", store.getItemContentHash(WebDavSyncItem.BaAccounts))
        assertEquals(1L, store.getLastSyncTime(WebDavSyncItem.BaAccounts))
    }

    @Test fun `legacy metadata migrates to a single checkpoint without retaining an absent ETag`() {
        val store = WebDavSyncPersistence(MemoryPreferences())
        store.setItemEtag(WebDavSyncItem.BaAccounts, "legacy")
        store.setItemContentHash(WebDavSyncItem.BaAccounts, "legacy-hash")
        store.setLastSyncTime(WebDavSyncItem.BaAccounts, 1)
        assertEquals("legacy", store.getItemEtag(WebDavSyncItem.BaAccounts))
        store.recordCheckpoint(WebDavSyncItem.BaAccounts, null, "new-hash", 2)
        assertNull(store.getItemEtag(WebDavSyncItem.BaAccounts))
        assertEquals("new-hash", store.getItemContentHash(WebDavSyncItem.BaAccounts))
        assertEquals(2L, store.getLastSyncTime(WebDavSyncItem.BaAccounts))
    }
}

private class MemoryPreferences : WebDavPreferences {
    private val values = mutableMapOf<String, Any>()
    var failWrites = false
    override fun decodeString(key: String, default: String?) = values[key] as? String ?: default
    override fun decodeInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun decodeLong(key: String, default: Long) = values[key] as? Long ?: default
    override fun decodeBool(key: String, default: Boolean) = values[key] as? Boolean ?: default
    private fun put(key: String, value: Any) { check(!failWrites); values[key] = value }
    override fun encode(key: String, value: String) = put(key, value)
    override fun encode(key: String, value: Int) = put(key, value)
    override fun encode(key: String, value: Long) = put(key, value)
    override fun encode(key: String, value: Boolean) = put(key, value)
    override fun containsKey(key: String) = key in values
    override fun removeValueForKey(key: String) { values.remove(key) }
}
