package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.*

class WebDavBaRestoreTest {
    @Test fun `a backup with missing active selection agrees with the actual replacement store`() {
        val raw = buildBaAccountsExportJson(testBaAccountState(
            accounts = listOf(testBaAccountRecord("remote", 1)), activeAccountId = null,
        ))
        val remote = parseBaAccountsExportJson(raw)
        val store = BaAccountStore(InMemoryBaAccountKeyValueStore())
        store.saveAccounts(listOf(testBaAccountRecord("local-only", 0)))
        store.replaceAll(remote.accounts, remote.activeAccountId, remote.activeAccountUpdatedAtMs)
        assertEquals(listOf("remote"), store.loadAccounts().map { it.profile.id.value })
        assertEquals(buildBaAccountsSyncFingerprintJson(raw),
            buildBaAccountsSyncFingerprintJson(buildBaAccountsExportJson(store.loadState())))
    }

    @Test fun `empty account restoration clears the active account without fingerprint drift`() {
        val raw = """{"accounts":[],"activeAccountUpdatedAtMs":999}"""
        val remote = parseBaAccountsExportJson(raw)
        val store = BaAccountStore(InMemoryBaAccountKeyValueStore())
        store.saveAccounts(listOf(testBaAccountRecord("local-only", 0)))
        store.replaceAll(remote.accounts, remote.activeAccountId, remote.activeAccountUpdatedAtMs)
        assertTrue(store.loadAccounts().isEmpty())
        assertNull(store.loadState().activeAccountId)
        assertEquals(buildBaAccountsSyncFingerprintJson(raw),
            buildBaAccountsSyncFingerprintJson(buildBaAccountsExportJson(store.loadState())))
    }
}
