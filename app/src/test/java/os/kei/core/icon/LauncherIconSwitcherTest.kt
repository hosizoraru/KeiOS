package os.kei.core.icon

import android.content.pm.PackageManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.core.prefs.LauncherIconDesign

class LauncherIconSwitcherTest {
    @Test
    fun `correct manifest defaults cause no package change`() {
        val components = RecordingComponents()

        assertEquals(false, components.apply(LauncherIconDesign.Android).getOrThrow())
        assertEquals(0, components.batchCalls)
        assertTrue(components.writes.isEmpty())
    }

    @Test
    fun `switch applies one atomic change and repeated apply is a no-op`() {
        val components = RecordingComponents()

        assertEquals(true, components.apply(LauncherIconDesign.Apple).getOrThrow())
        assertEquals(setOf(apple), components.enabled)
        assertEquals(1, components.batchCalls)
        assertTrue(components.writes.isEmpty())
        assertEquals(false, components.apply(LauncherIconDesign.Apple).getOrThrow())
        assertEquals(1, components.batchCalls)
    }

    @Test
    fun `fallback keeps a launchable entry at every observable step in either direction`() {
        val components = RecordingComponents().apply { rejectBatch = true }

        components.apply(LauncherIconDesign.Apple).getOrThrow()
        components.apply(LauncherIconDesign.Android).getOrThrow()

        assertEquals(setOf(android), components.enabled)
        assertTrue(components.observed.all { it.isNotEmpty() })
        assertEquals(setOf(apple, android), components.observed[0])
        assertEquals(setOf(apple), components.observed[1])
        assertEquals(setOf(apple, android), components.observed[2])
        assertEquals(setOf(android), components.observed[3])
    }

    @Test
    fun `failed activation never disables the working launcher entry`() {
        val components = RecordingComponents().apply {
            rejectBatch = true
            rejectWrite = { it.component == apple && it.enabledState == ENABLED }
        }

        assertTrue(components.apply(LauncherIconDesign.Apple).isFailure)
        assertEquals(setOf(android), components.enabled)
        assertTrue(components.observed.isEmpty())
        assertEquals(listOf(apple), components.writes.map { it.component })
    }

    @Test
    fun `silently ignored activation cannot remove the working entry`() {
        val components = RecordingComponents().apply {
            rejectBatch = true
            ignoreWrite = { it.component == apple }
        }

        assertTrue(components.apply(LauncherIconDesign.Apple).isFailure)
        assertEquals(setOf(android), components.enabled)
        assertTrue(components.writes.none { it.component == android })
    }

    @Test
    fun `failed removal restores exact original overrides without losing an entry`() {
        val components = RecordingComponents().apply {
            rejectBatch = true
            rejectWrite = { it.component == android && it.enabledState == DISABLED }
        }
        val original = components.states.toMap()

        assertTrue(components.apply(LauncherIconDesign.Apple).isFailure)
        assertEquals(original, components.states)
        assertEquals(setOf(android), components.enabled)
        assertTrue(components.observed.all { it.isNotEmpty() })
    }

    @Test
    fun `ignored removal reports failure and rolls back the newly enabled entry`() {
        val components = RecordingComponents().apply {
            rejectBatch = true
            ignoreWrite = { it.component == android && it.enabledState == DISABLED }
        }

        assertTrue(components.apply(LauncherIconDesign.Apple).isFailure)
        assertEquals(setOf(android), components.enabled)
        assertEquals(DEFAULT, components.states.getValue(apple))
        assertTrue(components.observed.all { it.isNotEmpty() })
    }

    @Test
    fun `failed rollback activation retains the newly working entry`() {
        val components = RecordingComponents().apply {
            rejectBatch = true
            throwAfterRemoval = true
            rejectWrite = { it.component == android && it.enabledState == DEFAULT }
        }

        val result = components.apply(LauncherIconDesign.Apple)

        assertTrue(result.isFailure)
        assertEquals(setOf(apple), components.enabled)
        assertTrue(components.observed.all { it.isNotEmpty() })
        assertTrue(result.exceptionOrNull()!!.suppressed.size >= 2)
    }

    @Test
    fun `a completed batch that throws is verified without redundant fallback writes`() {
        val components = RecordingComponents().apply { throwAfterBatch = true }

        assertEquals(true, components.apply(LauncherIconDesign.Apple).getOrThrow())
        assertEquals(setOf(apple), components.enabled)
        assertTrue(components.writes.isEmpty())
    }

    @Test
    fun `an ignored batch falls back and verifies actual component states`() {
        val components = RecordingComponents().apply { ignoreBatch = true }

        assertEquals(true, components.apply(LauncherIconDesign.Apple).getOrThrow())
        assertEquals(setOf(apple), components.enabled)
        assertEquals(2, components.writes.size)
    }

    @Test
    fun `an existing missing entry configuration is recovered`() {
        val components = RecordingComponents().apply {
            states[android] = DISABLED
            rejectBatch = true
        }

        assertEquals(true, components.apply(LauncherIconDesign.Apple).getOrThrow())
        assertEquals(setOf(apple), components.enabled)
    }

    @Test
    fun `duplicate entries converge to the requested design`() {
        val components = RecordingComponents().apply {
            states[apple] = ENABLED
            states[android] = ENABLED
        }

        assertEquals(true, components.apply(LauncherIconDesign.Android).getOrThrow())
        assertEquals(setOf(android), components.enabled)
        assertEquals(false, components.apply(LauncherIconDesign.Android).getOrThrow())
    }

    @Test
    fun `package read failure returns an error without mutating components`() {
        val components = RecordingComponents().apply { rejectRead = true }

        assertTrue(components.apply(LauncherIconDesign.Apple).isFailure)
        assertEquals(0, components.batchCalls)
        assertTrue(components.writes.isEmpty())
    }

    private class RecordingComponents : LauncherIconComponents {
        val states = mutableMapOf(apple to DEFAULT, android to DEFAULT)
        val writes = mutableListOf<LauncherIconComponentStateSpec>()
        val observed = mutableListOf<Set<LauncherIconComponentSpec>>()
        var batchCalls = 0
        var rejectBatch = false
        var ignoreBatch = false
        var throwAfterBatch = false
        var throwAfterRemoval = false
        var rejectRead = false
        var rejectWrite: (LauncherIconComponentStateSpec) -> Boolean = { false }
        var ignoreWrite: (LauncherIconComponentStateSpec) -> Boolean = { false }

        val enabled: Set<LauncherIconComponentSpec>
            get() = states.filter { (component, state) ->
                state == ENABLED || state == DEFAULT && manifestEnabled(component)
            }.keys

        fun apply(design: LauncherIconDesign): Result<Boolean> =
            LauncherIconSwitcher(this).apply(design.componentStateSpecs("os.kei.debug"))

        override fun enabledSetting(component: LauncherIconComponentSpec): Int {
            if (rejectRead) throw SecurityException("component settings unavailable")
            return states.getValue(component)
        }

        override fun manifestEnabled(component: LauncherIconComponentSpec): Boolean = component == android

        override fun setAtomically(settings: List<LauncherIconComponentStateSpec>) {
            batchCalls++
            if (rejectBatch) throw UnsupportedOperationException("batch unavailable")
            if (ignoreBatch) return
            settings.forEach { states[it.component] = it.enabledState }
            if (throwAfterBatch) throw IllegalStateException("error after commit")
        }

        override fun set(setting: LauncherIconComponentStateSpec) {
            writes += setting
            if (rejectWrite(setting)) throw SecurityException("write rejected")
            if (ignoreWrite(setting)) return
            states[setting.component] = setting.enabledState
            observed += enabled.toSet()
            if (throwAfterRemoval && setting.component == android && setting.enabledState == DISABLED) {
                throw IllegalStateException("error after removal")
            }
        }
    }

    private companion object {
        val apple = LauncherIconDesign.Apple.componentSpec("os.kei.debug")
        val android = LauncherIconDesign.Android.componentSpec("os.kei.debug")
        const val DEFAULT = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        const val ENABLED = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        const val DISABLED = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
}
