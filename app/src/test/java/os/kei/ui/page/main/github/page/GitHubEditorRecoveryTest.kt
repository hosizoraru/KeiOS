package os.kei.ui.page.main.github.page

import android.app.Application
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import os.kei.core.state.EditorRecoverySlot
import os.kei.core.state.EditorRecoveryStore
import os.kei.core.state.InMemoryEditorRecoveryStorage
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class GitHubEditorRecoveryTest {
    @Test
    fun `new subscription recovers exact input and options without creating a track`() {
        val storage = InMemoryEditorRecoveryStorage()
        val state = owner(storage)
        state.showAddSheet = true
        state.repoUrlInput = "  https://github.com/example/unfinished "
        state.packageNameInput = "com.example.draft"
        state.preferPreReleaseInput = true
        state.updateIntervalModeInput = GitHubTrackedUpdateIntervalMode.Hours6
        val restored = owner(storage)
        assertFalse(restored.trackDataReady)
        assertFalse(restored.showAddSheet)
        restored.reconcileRecoveredTrackEditor(emptyList())
        assertTrue(restored.showAddSheet)
        assertEquals(state.repoUrlInput, restored.repoUrlInput)
        assertEquals(state.packageNameInput, restored.packageNameInput)
        assertTrue(restored.preferPreReleaseInput)
        assertEquals(GitHubTrackedUpdateIntervalMode.Hours6, restored.updateIntervalModeInput)
        assertTrue(restored.trackedItems.isEmpty())
        assertNull(restored.editingTrackedItem)
    }

    @Test
    fun `edit identity survives and current persisted target is reconciled`() {
        val storage = InMemoryEditorRecoveryStorage()
        val state = owner(storage)
        val original = tracked()
        state.editingTrackedItem = original
        state.repoUrlInput = "unfinished URL"
        state.showAddSheet = true
        val restored = owner(storage)
        assertEquals(original, restored.editingTrackedItem)
        val current = original.copy(appLabel = "Updated label")
        restored.reconcileRecoveredTrackEditor(listOf(current))
        assertEquals(current, restored.editingTrackedItem)
        assertEquals("unfinished URL", restored.repoUrlInput)
    }

    @Test
    fun `deleted recovered edit target is not silently recreated as an add`() {
        val storage = InMemoryEditorRecoveryStorage()
        owner(storage).apply {
            editingTrackedItem = tracked()
            repoUrlInput = "unfinished"
            showAddSheet = true
        }
        val restored = owner(storage)
        restored.reconcileRecoveredTrackEditor(emptyList())
        assertFalse(restored.showAddSheet)
        assertNull(EditorRecoveryStore(storage).load(EditorRecoverySlot.GitHubTrack))
    }

    @Test
    fun `explicit cancel clears recovered input`() {
        val storage = InMemoryEditorRecoveryStorage()
        val state = owner(storage)
        state.showAddSheet = true
        state.packageNameInput = "com.example.draft"
        state.dismissTrackSheet()
        val restored = owner(storage)
        assertFalse(restored.showAddSheet)
        assertEquals("", restored.packageNameInput)
    }

    @Test
    fun `old save completion cannot clear newer input in the same editor`() {
        val storage = InMemoryEditorRecoveryStorage()
        val state = owner(storage)
        state.replaceTrackEditor {
            state.showAddSheet = true
            state.repoUrlInput = "first draft"
        }
        val savingToken = state.editorSessionToken
        state.repoUrlInput = "new input while saving"
        state.completeTrackEditorSave(savingToken)
        assertTrue(state.showAddSheet)
        assertEquals("new input while saving", owner(storage).repoUrlInput)
        state.completeTrackEditorSave(state.editorSessionToken)
        assertFalse(state.showAddSheet)
        assertNull(EditorRecoveryStore(storage).load(EditorRecoverySlot.GitHubTrack))
    }

    @Test
    fun `checkpoint during target replacement sees a complete old or new draft`() {
        val storage = InMemoryEditorRecoveryStorage()
        val store = EditorRecoveryStore(storage)
        val state = owner(storage)
        val oldTarget = tracked()
        val newTarget = oldTarget.copy(packageName = "com.example.other")
        state.replaceTrackEditor {
            state.editingTrackedItem = oldTarget
            state.repoUrlInput = "old input"
            state.showAddSheet = true
        }
        state.replaceTrackEditor {
            state.editingTrackedItem = newTarget
            store.checkpoint()
            val interrupted = owner(storage)
            assertEquals(oldTarget, interrupted.editingTrackedItem)
            assertEquals("old input", interrupted.repoUrlInput)
            state.repoUrlInput = "new input"
        }
        val restored = owner(storage)
        assertEquals(newTarget, restored.editingTrackedItem)
        assertEquals("new input", restored.repoUrlInput)
    }

    private fun owner(storage: InMemoryEditorRecoveryStorage) =
        GitHubPageState(editorRecovery = GitHubEditorRecovery(EditorRecoveryStore(storage)))

    private fun tracked() = GitHubTrackedApp(
        repoUrl = "https://github.com/example/project",
        owner = "example", repo = "project", packageName = "com.example.project", appLabel = "Project",
    )
}
