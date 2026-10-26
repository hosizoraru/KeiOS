package os.kei.ui.page.main.sync

import org.junit.Test
import os.kei.ui.page.main.os.OsGoogleSystemServiceConfig
import os.kei.ui.page.main.os.store.OsCardKeyValueStore
import os.kei.ui.page.main.os.shortcut.*
import os.kei.ui.page.main.os.shell.*
import kotlin.test.*

class WebDavSnapshotPersistenceTest {
    @Test fun `empty activity restore survives a fresh persistence instance and ignores legacy cards`() {
        val storage = MemoryCards(); val legacy = MemoryCards()
        val defaults = OsGoogleSystemServiceConfig()
        val local = OsActivityShortcutCard("local-only", config = defaults.copy(title = "Local", packageName = "example.local"))
        val builtIn = local.copy(id = "builtin", isBuiltInSample = true)
        legacy.encode(OS_ACTIVITY_SHORTCUT_CARD_KEY_CARDS, OsActivityShortcutCardCodec.encodeCards(listOf(local)))
        OsActivityShortcutCardPersistence(storage, legacy).replaceFromSync(emptyList(), defaults)
        val fresh = OsActivityShortcutCardPersistence(storage, legacy)
        assertTrue(fresh.loadCards(defaults, defaults, listOf(builtIn)).isEmpty())
    }

    @Test fun `activity restore preserves the remote set without silently adding defaults`() {
        val storage = MemoryCards(); val defaults = OsGoogleSystemServiceConfig()
        val remote = OsActivityShortcutCard("remote", config = defaults.copy(title = "Remote", packageName = "example.remote"))
        val builtIn = remote.copy(id = "builtin", isBuiltInSample = true)
        OsActivityShortcutCardPersistence(storage, MemoryCards(), saveLegacyConfig = { _, _ -> })
            .replaceFromSync(listOf(remote), defaults)
        val fresh = OsActivityShortcutCardPersistence(storage, MemoryCards()).loadCards(defaults, defaults, listOf(builtIn))
        assertEquals(listOf("remote"), fresh.map { it.id })
    }

    @Test fun `empty shell restore survives restart without resurrecting a legacy command`() {
        val storage = MemoryCards()
        storage.encode(OS_SHELL_LEGACY_KEY_COMMAND, "echo old-legacy")
        val builtin = OsShellCommandCard(id = "builtin", title = "Builtin", command = "echo builtin")
        OsShellCommandCardPersistence(storage).replaceFromSync(emptyList())
        assertTrue(OsShellCommandCardPersistence(storage).loadCards(listOf(builtin)).isEmpty())
    }

    @Test fun `shell snapshot keeps replacement data across edits and reload`() {
        val storage = MemoryCards()
        val remote = OsShellCommandCard(id = "remote", title = "Remote", command = "echo remote")
        val persistence = OsShellCommandCardPersistence(storage)
        persistence.replaceFromSync(listOf(remote))
        persistence.saveCards(emptyList())
        assertTrue(OsShellCommandCardPersistence(storage).loadCards(listOf(remote)).isEmpty())
    }
}

private class MemoryCards : OsCardKeyValueStore {
    private val values = mutableMapOf<String, Any>()
    override fun decodeString(key: String, default: String?) = values[key] as? String ?: default
    override fun decodeBool(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun decodeLong(key: String, default: Long) = values[key] as? Long ?: default
    override fun encode(key: String, value: String) { values[key] = value }
    override fun encode(key: String, value: Boolean) { values[key] = value }
    override fun encode(key: String, value: Long) { values[key] = value }
    override fun removeValueForKey(key: String) { values.remove(key) }
}
