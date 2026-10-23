package os.kei.ui.page.main.os

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.core.state.EditorRecoverySlot
import os.kei.core.state.EditorRecoveryStore
import os.kei.core.state.InMemoryEditorRecoveryStorage
import os.kei.ui.page.main.os.shell.OsShellCommandCard
import os.kei.ui.page.main.os.shortcut.OsActivityCardEditMode
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtra
import os.kei.ui.page.main.os.shortcut.ShortcutIntentExtraType
import os.kei.ui.page.main.os.state.OsEditorRecovery
import os.kei.ui.page.main.os.state.decodeOsEditorRecovery

class OsEditorRecoveryTest {
    @Test
    fun `activity input survives new owner without normalizing or applying the draft`() {
        val storage = InMemoryEditorRecoveryStorage()
        val recovery = OsEditorRecovery(EditorRecoveryStore(storage))
        val state = MutableStateFlow(OsPageRuntimeState())
        val actions = OsPageOverlayRuntimeActions(state, recovery::save)
        val draft = OsGoogleSystemServiceConfig(
            title = "  unfinished  ",
            packageName = "",
            intentUriData = "content://provider/one",
            intentExtras = listOf(ShortcutIntentExtra("", ShortcutIntentExtraType.Int, "invalid yet")),
        )
        actions.updateActivityCardEditMode(OsActivityCardEditMode.Add)
        actions.updateActivityShortcutDraft(draft)
        actions.updateShowActivityShortcutEditor(true)
        val edited = draft.copy(intentUriData = "content://provider/two")
        actions.updateActivityShortcutDraft(edited)
        val restored = OsEditorRecovery(EditorRecoveryStore(storage)).restore()
        assertTrue(restored.showActivityShortcutEditor)
        assertEquals(OsActivityCardEditMode.Add, restored.activityCardEditMode)
        assertEquals(edited, restored.activityShortcutDraft)
        assertFalse(restored.showShellCommandCardEditor)
        assertFalse(restored.cardTransferInProgress)
    }

    @Test
    fun `shell input survives while command output and running work do not`() {
        val storage = InMemoryEditorRecoveryStorage()
        val recovery = OsEditorRecovery(EditorRecoveryStore(storage))
        recovery.save(OsPageRuntimeState(
            showShellCommandCardEditor = true,
            editingShellCommandCardId = "shell-1",
            shellCommandCardDraft = OsShellCommandCard(
                id = "shell-1", title = "Draft", command = "  printf 'one\\ntwo'  ",
                runOutput = "not input", lastRunAtMillis = 900,
            ),
            runningShellCommandCardIds = setOf("shell-1"),
        ))
        val restored = OsEditorRecovery(EditorRecoveryStore(storage)).restore()
        assertEquals("  printf 'one\\ntwo'  ", restored.shellCommandCardDraft?.command)
        assertEquals("", restored.shellCommandCardDraft?.runOutput)
        assertEquals(emptySet(), restored.runningShellCommandCardIds)
        assertTrue(restored.showShellCommandCardEditor)
    }

    @Test
    fun `explicit close clears recovery even before dismissal animation finishes`() {
        val storage = InMemoryEditorRecoveryStorage()
        val store = EditorRecoveryStore(storage)
        val recovery = OsEditorRecovery(store)
        val state = MutableStateFlow(OsPageRuntimeState())
        val actions = OsPageOverlayRuntimeActions(state, recovery::save)
        actions.updateEditingShellCommandCardId("shell-1")
        actions.updateShellCommandCardDraft(OsShellCommandCard("shell-1", title = "", command = "input"))
        actions.updateShowShellCommandCardEditor(true)
        actions.updateShowShellCommandCardEditor(false)
        assertNull(store.load(EditorRecoverySlot.OsCard))
        assertFalse(OsEditorRecovery(EditorRecoveryStore(storage)).restore().showShellCommandCardEditor)
    }

    @Test
    fun `unknown editor type and missing edit identity are not reopened`() {
        assertNull(decodeOsEditorRecovery("""{"kind":"future"}"""))
        assertNull(decodeOsEditorRecovery("""{"kind":"shell","command":"echo bad"}"""))
        assertNull(decodeOsEditorRecovery("""{"kind":"activity","mode":"Edit"}"""))
    }

    @Test
    fun `recovered input cannot write before the authoritative card list is loaded`() = runTest {
        var writes = 0
        val repository = OsPageCardRepository(
            ioDispatcher = Dispatchers.Unconfined,
            saveActivityCards = { _, _ -> writes++ },
        )
        val card = OsActivityShortcutCard("existing", config = OsGoogleSystemServiceConfig())
        val snapshot = OsPagePersistentState(activityShortcutCards = listOf(card), loaded = false)
        assertFailsWith<IllegalStateException> {
            repository.saveActivityShortcutCard(
                snapshot, OsActivityCardEditMode.Add, null,
                OsGoogleSystemServiceConfig(title = "recovered input"), OsGoogleSystemServiceConfig(),
            )
        }
        assertEquals(0, writes)
        val updated = repository.saveActivityShortcutCard(
            snapshot.copy(loaded = true), OsActivityCardEditMode.Edit, card.id,
            card.config.copy(title = "changed"), OsGoogleSystemServiceConfig(),
        )
        assertEquals(1, writes)
        assertEquals(listOf("existing"), updated.map { it.id })
        assertEquals("changed", updated.single().config.title)
    }

    @Test
    fun `opening another editor persists identity and content in one snapshot`() {
        val storage = InMemoryEditorRecoveryStorage()
        val recovery = OsEditorRecovery(EditorRecoveryStore(storage))
        val state = MutableStateFlow(OsPageRuntimeState())
        var saves = 0
        val actions = OsPageOverlayRuntimeActions(state) { snapshot ->
            saves++
            recovery.save(snapshot)
        }
        actions.openShellEditor(OsShellCommandCard("first", title = "first", command = "old"))
        actions.openShellEditor(OsShellCommandCard("second", title = "second", command = "new"))
        assertEquals(2, saves)
        val restored = OsEditorRecovery(EditorRecoveryStore(storage)).restore()
        assertEquals("second", restored.editingShellCommandCardId)
        assertEquals("new", restored.shellCommandCardDraft?.command)
    }

    @Test
    fun `missing recovered target is closed only after authoritative data is ready`() {
        val storage = InMemoryEditorRecoveryStorage()
        val recovery = OsEditorRecovery(EditorRecoveryStore(storage))
        val state = MutableStateFlow(OsPageRuntimeState())
        val actions = OsPageOverlayRuntimeActions(state, recovery::save)
        actions.openShellEditor(OsShellCommandCard("deleted", title = "old", command = "input"))
        actions.reconcileEditorTargets(OsPagePersistentState(loaded = false))
        assertTrue(state.value.showShellCommandCardEditor)
        actions.reconcileEditorTargets(OsPagePersistentState(loaded = true))
        assertFalse(state.value.showShellCommandCardEditor)
        assertNull(EditorRecoveryStore(storage).load(EditorRecoverySlot.OsCard))
    }
}
