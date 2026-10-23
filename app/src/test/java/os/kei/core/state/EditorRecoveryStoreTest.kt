package os.kei.core.state

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

internal class InMemoryEditorRecoveryStorage : EditorRecoveryStorage {
    val values = mutableMapOf<String, String>()
    var failWrites = false
    var syncCount = 0
    val failedKeys = mutableSetOf<String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String?): Boolean {
        if (failWrites || key in failedKeys) return false
        if (value == null) values.remove(key) else values[key] = value
        return true
    }
    override fun sync() { syncCount++ }
}

class EditorRecoveryStoreTest {
    @Test
    fun `prepared snapshot is recoverable by a new store instance`() {
        val storage = InMemoryEditorRecoveryStorage()
        EditorRecoveryStore(storage) { 100 }.save(EditorRecoverySlot.OsCard, "unsaved input")
        assertEquals("unsaved input", EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.OsCard))
    }

    @Test
    fun `checkpoint retries failed prepared writes without accessing a page`() {
        val storage = InMemoryEditorRecoveryStorage().apply { failWrites = true }
        val store = EditorRecoveryStore(storage) { 100 }
        assertFalse(store.save(EditorRecoverySlot.OsCard, "latest"))
        assertFailsWith<IllegalStateException> { store.checkpoint() }
        storage.failWrites = false
        store.checkpoint()
        assertEquals("latest", EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.OsCard))
        assertEquals(2, storage.syncCount)
    }

    @Test
    fun `cancel replaces a failed pending save rather than reviving the draft`() {
        val storage = InMemoryEditorRecoveryStorage()
        val store = EditorRecoveryStore(storage) { 100 }
        store.save(EditorRecoverySlot.OsCard, "old")
        storage.failWrites = true
        store.save(EditorRecoverySlot.OsCard, "new")
        store.save(EditorRecoverySlot.OsCard, null)
        storage.failWrites = false
        store.checkpoint()
        assertNull(EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.OsCard))
    }

    @Test
    fun `expired future and unknown version records are not restored`() {
        val storage = InMemoryEditorRecoveryStorage()
        EditorRecoveryStore(storage) { 100 }.save(EditorRecoverySlot.OsCard, "draft")
        assertNull(EditorRecoveryStore(storage) { 99 }.load(EditorRecoverySlot.OsCard))
        assertNull(EditorRecoveryStore(storage) { 101 + EditorRecoveryStore.MAX_AGE_MS }.load(EditorRecoverySlot.OsCard))
        storage.values[EditorRecoverySlot.OsCard.name] = """{"version":2,"savedAt":100,"payload":"unknown"}"""
        assertNull(EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.OsCard))
    }

    @Test
    fun `oversized command does not restore a truncated or previous executable draft`() {
        val storage = InMemoryEditorRecoveryStorage()
        val store = EditorRecoveryStore(storage) { 100 }
        store.save(EditorRecoverySlot.OsCard, "old command")
        assertTrue(store.save(EditorRecoverySlot.OsCard, "x".repeat(EditorRecoveryStore.MAX_RECORD_CHARS)))
        assertNull(EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.OsCard))
    }

    @Test
    fun `closing one editor does not clear another domain draft`() {
        val storage = InMemoryEditorRecoveryStorage()
        val store = EditorRecoveryStore(storage) { 100 }
        store.save(EditorRecoverySlot.OsCard, "shell")
        store.save(EditorRecoverySlot.GitHubTrack, "repository")
        store.save(EditorRecoverySlot.OsCard, null)
        assertEquals("repository", EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.GitHubTrack))
    }

    @Test
    fun `one failed slot cannot prevent another pending slot being saved and synced`() {
        val storage = InMemoryEditorRecoveryStorage().apply { failWrites = true }
        val store = EditorRecoveryStore(storage) { 100 }
        store.save(EditorRecoverySlot.OsCard, "shell")
        store.save(EditorRecoverySlot.GitHubTrack, "repository")
        storage.failWrites = false
        storage.failedKeys += EditorRecoverySlot.OsCard.name
        assertFailsWith<IllegalStateException> { store.checkpoint() }
        assertEquals(1, storage.syncCount)
        assertEquals("repository", EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.GitHubTrack))
        storage.failedKeys.clear()
        store.checkpoint()
        assertEquals("shell", EditorRecoveryStore(storage) { 101 }.load(EditorRecoverySlot.OsCard))
    }
}
