package os.kei.ui.page.main.github.page.action

import android.app.Application
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import os.kei.core.state.EditorRecoverySlot
import os.kei.core.state.EditorRecoveryStorage
import os.kei.core.state.EditorRecoveryStore
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.page.GitHubEditorRecovery
import os.kei.ui.page.main.github.page.GitHubPageState
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class GitHubTrackEditorPersistenceRegressionTest {
    @Test
    fun `blocked persistence leaves the editor recoverable and publishes nothing until success`() = runTest {
        val storage = RecordingStorage()
        val state = owner(storage)
        state.replaceTrackEditorForAdd()
        state.repoUrlInput = item().repoUrl
        state.packageNameInput = item().packageName
        val gate = CompletableDeferred<Unit>()
        var staged: TrackEditorSavePlan? = null
        val token = state.editorSessionToken
        val job = launch {
            assertEquals(TrackEditorSaveResult.Saved, save(state, token = token) {
                staged = it
                gate.await()
            })
        }
        runCurrent()

        assertNotNull(staged)
        assertTrue(state.trackedItems.isEmpty())
        assertTrue(state.trackedAddedAtById.isEmpty())
        assertTrue(state.showAddSheet)
        assertEquals(item().repoUrl, restored(storage).repoUrlInput)
        assertNotNull(EditorRecoveryStore(storage).load(EditorRecoverySlot.GitHubTrack))

        gate.complete(Unit)
        job.join()
        assertEquals(listOf(item()), state.trackedItems.toList())
        assertEquals(100L, state.trackedAddedAtById[item().id])
        assertFalse(state.showAddSheet)
        assertNull(EditorRecoveryStore(storage).load(EditorRecoverySlot.GitHubTrack))
    }

    @Test
    fun `failed add retains the draft and can retry without a phantom duplicate`() = runTest {
        val storage = RecordingStorage()
        val state = owner(storage)
        state.replaceTrackEditorForAdd()
        state.repoUrlInput = item().repoUrl
        val token = state.editorSessionToken

        assertFailsWith<IllegalStateException> {
            save(state, token = token) { throw IllegalStateException("storage failure") }
        }
        assertTrue(state.trackedItems.isEmpty())
        assertTrue(state.trackedAddedAtById.isEmpty())
        assertTrue(state.trackedModifiedAtById.isEmpty())
        assertEquals(token, state.editorSessionToken)
        assertEquals(item().repoUrl, restored(storage).repoUrlInput)

        assertEquals(TrackEditorSaveResult.Saved, save(state, token = token) {})
        assertEquals(listOf(item()), state.trackedItems.toList())
    }

    @Test
    fun `failed edit leaves the original identity metadata and draft intact`() = runTest {
        val state = owner()
        val original = item()
        val replacement = item("other")
        state.trackedItems.add(original)
        state.trackedAddedAtById[original.id] = 10L
        state.trackedModifiedAtById[original.id] = 20L
        state.replaceTrackEditorForEdit(original)
        state.repoUrlInput = replacement.repoUrl

        assertFailsWith<IllegalStateException> {
            save(state, editing = original, next = replacement) { plan ->
                assertEquals(listOf(replacement), plan.items)
                assertEquals(10L, plan.addedAt[replacement.id])
                assertFalse(plan.addedAt.containsKey(original.id))
                throw IllegalStateException("storage failure")
            }
        }
        assertEquals(listOf(original), state.trackedItems.toList())
        assertEquals(mapOf(original.id to 10L), state.trackedAddedAtById.toMap())
        assertEquals(mapOf(original.id to 20L), state.trackedModifiedAtById.toMap())
        assertEquals(replacement.repoUrl, state.repoUrlInput)
        assertTrue(state.showAddSheet)
    }

    @Test
    fun `successful rename stages the full list and transfers timestamps only after persistence`() = runTest {
        val state = owner()
        val original = item()
        val unrelated = item("unrelated")
        val replacement = item("replacement")
        state.trackedItems.addAll(listOf(original, unrelated))
        state.trackedAddedAtById[original.id] = 10L
        state.trackedAddedAtById[unrelated.id] = 15L
        state.replaceTrackEditorForEdit(original)

        assertEquals(TrackEditorSaveResult.Saved, save(state, editing = original, next = replacement) { plan ->
            assertEquals(listOf(replacement, unrelated), plan.items)
            assertEquals(listOf(original, unrelated), state.trackedItems.toList())
            assertEquals(mapOf(replacement.id to 10L, unrelated.id to 15L), plan.addedAt)
            assertEquals(setOf(replacement.id), plan.refreshTrackIds)
            assertEquals(1, plan.semanticUpdates.size)
        })
        assertEquals(listOf(replacement, unrelated), state.trackedItems.toList())
        assertEquals(10L, state.trackedAddedAtById[replacement.id])
        assertNull(state.trackedAddedAtById[original.id])
        assertEquals(100L, state.trackedModifiedAtById[replacement.id])
        assertFalse(state.showAddSheet)
    }

    @Test
    fun `actual duplicate never writes or closes the draft`() = runTest {
        val state = owner()
        state.trackedItems.add(item())
        state.replaceTrackEditorForAdd()

        assertEquals(TrackEditorSaveResult.Duplicate, save(state) { error("must not save") })
        assertEquals(listOf(item()), state.trackedItems.toList())
        assertTrue(state.showAddSheet)
    }

    @Test
    fun `cancelled persistence leaves a process restart draft without an optimistic entry`() = runTest {
        val storage = RecordingStorage()
        val state = owner(storage)
        state.replaceTrackEditorForAdd()
        state.repoUrlInput = "unfinished"
        val gate = CompletableDeferred<Unit>()
        val job = launch { save(state) { gate.await() } }
        runCurrent()
        job.cancel()
        job.join()

        assertTrue(state.trackedItems.isEmpty())
        assertTrue(state.showAddSheet)
        assertEquals("unfinished", restored(storage).repoUrlInput)
    }

    @Test
    fun `old save completing never closes or clears a newer editor`() = runTest {
        val storage = RecordingStorage()
        val state = owner(storage)
        state.replaceTrackEditorForAdd()
        val token = state.editorSessionToken
        val gate = CompletableDeferred<Unit>()
        val job = launch { save(state, token = token) { gate.await() } }
        runCurrent()

        val other = item("other")
        state.replaceTrackEditorForEdit(other)
        state.repoUrlInput = "new unfinished input"
        val newerToken = state.editorSessionToken
        gate.complete(Unit)
        job.join()

        assertEquals(listOf(item()), state.trackedItems.toList())
        assertEquals(newerToken, state.editorSessionToken)
        assertTrue(state.showAddSheet)
        assertEquals(other.id, state.editingTrackedItem?.id)
        assertEquals("new unfinished input", restored(storage, listOf(other)).repoUrlInput)
    }

    @Test
    fun `input changed during persistence also retains the newer draft`() = runTest {
        val state = owner()
        state.replaceTrackEditorForAdd()
        val gate = CompletableDeferred<Unit>()
        val token = state.editorSessionToken
        val job = launch { save(state, token = token) { gate.await() } }
        runCurrent()
        state.packageNameInput = "com.example.newinput"
        gate.complete(Unit)
        job.join()

        assertTrue(state.showAddSheet)
        assertEquals("com.example.newinput", state.packageNameInput)
    }

    @Test
    fun `unready stale and deleted targets never reach persistence`() = runTest {
        val state = owner(ready = false)
        state.replaceTrackEditorForAdd()
        assertEquals(TrackEditorSaveResult.Stale, save(state) { error("must not save") })
        state.reconcileRecoveredTrackEditor(emptyList())
        val token = state.editorSessionToken
        state.repoUrlInput = "changed during validation"
        assertEquals(TrackEditorSaveResult.Stale, save(state, token = token) { error("must not save") })
        assertEquals(TrackEditorSaveResult.Stale, save(state, editing = item()) { error("must not save") })
    }

    @Test
    fun `opening add or another edit writes exactly one complete replacement snapshot`() {
        val storage = RecordingStorage()
        val state = owner(storage)
        state.replaceTrackEditorForEdit(item())
        state.repoUrlInput = "old unsaved input"
        val before = storage.writes
        val token = state.editorSessionToken
        val other = item("other").copy(preferPreRelease = true)
        state.replaceTrackEditorForEdit(other)

        assertEquals(before + 1, storage.writes)
        assertEquals(token + 1, state.editorSessionToken)
        val edit = restored(storage, listOf(other))
        assertEquals(other, edit.editingTrackedItem)
        assertEquals(other.repoUrl, edit.repoUrlInput)
        assertEquals(other.packageName, edit.packageNameInput)
        assertTrue(edit.preferPreReleaseInput)

        val beforeAdd = storage.writes
        val editToken = state.editorSessionToken
        state.replaceTrackEditorForAdd()
        assertEquals(beforeAdd + 1, storage.writes)
        assertEquals(editToken + 1, state.editorSessionToken)
        val add = restored(storage)
        assertNull(add.editingTrackedItem)
        assertEquals("", add.repoUrlInput)
        assertEquals("", add.packageNameInput)
        assertTrue(add.showAddSheet)
    }

    private suspend fun save(
        state: GitHubPageState,
        token: Long = state.editorSessionToken,
        editing: GitHubTrackedApp? = null,
        next: GitHubTrackedApp = item(),
        persist: suspend (TrackEditorSavePlan) -> Unit,
    ) = persistTrackEditorSave(state, token, editing, next, 100L, persist)

    private fun owner(storage: RecordingStorage = RecordingStorage(), ready: Boolean = true) =
        GitHubPageState(editorRecovery = GitHubEditorRecovery(EditorRecoveryStore(storage))).apply {
            if (ready) reconcileRecoveredTrackEditor(listOfNotNull(editingTrackedItem))
        }

    private fun restored(storage: RecordingStorage, items: List<GitHubTrackedApp> = emptyList()) =
        owner(storage, ready = false).apply { reconcileRecoveredTrackEditor(items) }

    private fun item(repo: String = "project") = GitHubTrackedApp(
        repoUrl = "https://github.com/example/$repo",
        owner = "example",
        repo = repo,
        packageName = "com.example.$repo",
        appLabel = repo,
    )

    private class RecordingStorage : EditorRecoveryStorage {
        val values = mutableMapOf<String, String>()
        var writes = 0
        override fun read(key: String) = values[key]
        override fun write(key: String, value: String?): Boolean {
            writes++
            if (value == null) values.remove(key) else values[key] = value
            return true
        }
        override fun sync() = Unit
    }
}
