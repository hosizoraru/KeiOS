package os.kei.core.prefs

import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test

class UiPrefsLauncherIconWritesTest {
    @Test(timeout = 5_000)
    fun `rapid choices retain the last value when IO tries to run writes in reverse`() = runBlocking {
        val dispatcher = ReversingDispatcher()
        val writes = mutableListOf<LauncherIconDesign>()
        val repository = UiPrefsRepository(dispatcher, persistLauncherIconDesign = { writes += it })
        val first = launch(start = CoroutineStart.UNDISPATCHED) { repository.setLauncherIconDesign(LauncherIconDesign.Apple) }
        val second = launch(start = CoroutineStart.UNDISPATCHED) { repository.setLauncherIconDesign(LauncherIconDesign.Android) }
        val immediateSelection = repository.observeSnapshots().value.launcherIconDesign

        drain(dispatcher, first, second)

        assertEquals(LauncherIconDesign.Android, immediateSelection)
        assertEquals(listOf(LauncherIconDesign.Apple, LauncherIconDesign.Android), writes)
        assertEquals(LauncherIconDesign.Android, repository.observeSnapshots().value.launcherIconDesign)
    }

    @Test(timeout = 5_000)
    fun `repositories sharing the same preference key serialize their writes`() = runBlocking {
        val dispatcher = ReversingDispatcher()
        val writes = mutableListOf<LauncherIconDesign>()
        val firstRepository = UiPrefsRepository(dispatcher, persistLauncherIconDesign = { writes += it })
        val secondRepository = UiPrefsRepository(dispatcher, persistLauncherIconDesign = { writes += it })
        val first = launch(start = CoroutineStart.UNDISPATCHED) { firstRepository.setLauncherIconDesign(LauncherIconDesign.Apple) }
        val second = launch(start = CoroutineStart.UNDISPATCHED) { secondRepository.setLauncherIconDesign(LauncherIconDesign.Android) }

        drain(dispatcher, first, second)

        assertEquals(listOf(LauncherIconDesign.Apple, LauncherIconDesign.Android), writes)
    }

    @Test(timeout = 5_000)
    fun `closing the caller does not discard a choice already applied by the platform`() = runBlocking {
        val dispatcher = ReversingDispatcher()
        val writes = mutableListOf<LauncherIconDesign>()
        val repository = UiPrefsRepository(dispatcher, persistLauncherIconDesign = { writes += it })
        val first = launch(start = CoroutineStart.UNDISPATCHED) { repository.setLauncherIconDesign(LauncherIconDesign.Apple) }
        val second = launch(start = CoroutineStart.UNDISPATCHED) { repository.setLauncherIconDesign(LauncherIconDesign.Android) }
        second.cancel()

        drain(dispatcher, first, second)

        assertTrue(second.isCancelled)
        assertEquals(listOf(LauncherIconDesign.Apple, LauncherIconDesign.Android), writes)
    }

    private suspend fun drain(dispatcher: ReversingDispatcher, vararg jobs: Job) {
        while (jobs.any { !it.isCompleted }) {
            dispatcher.runNewest()
            yield()
        }
        jobs.toList().joinAll()
    }

    private class ReversingDispatcher : CoroutineDispatcher() {
        private val pending = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            pending.addLast(block)
        }

        fun runNewest() {
            if (pending.isNotEmpty()) pending.removeLast().run()
        }
    }
}
