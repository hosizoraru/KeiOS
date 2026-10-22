package os.kei.ui.page.main.student.catalog.component

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BaGuideBgmControllerConnectionTest {
    @Test
    fun `failed obsolete attempt cannot discard commands waiting for a newer connection`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("old play") }
        fixture.executor.runAll()
        val oldAttempt = fixture.attempts.single()

        fixture.connection.disconnect()
        fixture.connection.submit { it.commands.add("new play") }
        fixture.executor.runLast()
        val newAttempt = fixture.attempts.last()
        oldAttempt.future.setException(IllegalStateException("old connection failed"))
        fixture.executor.runAll()
        val controller = newAttempt.complete()
        fixture.executor.runAll()

        assertSame(controller, fixture.connection.current)
        assertEquals(listOf("new play"), controller.commands)
        assertSame(oldAttempt.future, fixture.releasedFutures.single())
    }

    @Test
    fun `obsolete successful attempt cannot replace a newer controller or consume its commands`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("old play") }
        fixture.executor.runAll()
        val oldAttempt = fixture.attempts.single()
        val obsolete = oldAttempt.complete()

        fixture.connection.disconnect()
        fixture.connection.submit { it.commands.add("new play") }
        fixture.executor.runLast()
        val current = fixture.attempts.last().complete()
        fixture.executor.runLast()
        fixture.executor.runAll()

        assertSame(current, fixture.connection.current)
        assertEquals(listOf("new play"), current.commands)
        assertEquals(listOf("release"), obsolete.commands)
        assertEquals(1, obsolete.releases)
        assertEquals(0, current.releases)
    }

    @Test
    fun `disconnect releases a completed future whose app listener has not run`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        fixture.executor.runAll()
        val attempt = fixture.attempts.single()
        val controller = attempt.complete()

        fixture.connection.disconnect()
        assertEquals(0, controller.releases)
        fixture.executor.runAll()

        assertNull(fixture.connection.current)
        assertEquals(listOf("release"), controller.commands)
        assertSame(attempt.future, fixture.releasedFutures.single())
        assertEquals(1, controller.releases)
    }

    @Test
    fun `disconnect cancels a pending future on the controller executor`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        fixture.executor.runAll()
        val attempt = fixture.attempts.single()

        fixture.connection.disconnect()
        assertFalse(attempt.future.isCancelled)
        fixture.executor.runAll()

        assertTrue(attempt.future.isCancelled)
        assertSame(attempt.future, fixture.releasedFutures.single())
        assertNull(fixture.connection.current)
    }

    @Test
    fun `session disconnection drops queued playback and metadata without reconnecting`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        val first = fixture.connectPending()
        fixture.connection.submit { it.commands.add("queued play") }

        fixture.attempts.single().disconnect(first)
        fixture.connection.submit(connectIfNeeded = false) { it.commands.add("artwork") }
        fixture.executor.runAll()

        assertNull(fixture.connection.current)
        assertEquals(1, fixture.attempts.size)
        assertEquals(listOf("play"), first.commands)
        assertEquals(1, fixture.disconnections)

        fixture.connection.submit { it.commands.add("explicit new play") }
        val second = fixture.connectPending()
        assertEquals(listOf("explicit new play"), second.commands)
        assertSame(second, fixture.connection.current)

        fixture.attempts.first().disconnect(first)
        assertSame(second, fixture.connection.current)
        assertEquals(1, fixture.disconnections)
    }

    @Test
    fun `intended operation replaces a cached disconnected controller before listener delivery`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        val first = fixture.connectPending()
        first.connected = false
        assertNull(fixture.connection.current)

        fixture.connection.submit { it.commands.add("next play") }
        val second = fixture.connectPending()

        assertEquals(2, fixture.attempts.size)
        assertEquals(listOf("play"), first.commands)
        assertEquals(listOf("next play"), second.commands)
        assertSame(second, fixture.connection.current)
    }

    @Test
    fun `controller disconnected before connection completion never receives pending playback`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        fixture.executor.runAll()
        val controller = FakeController().apply { connected = false }
        fixture.attempts.single().future.set(controller)
        fixture.executor.runAll()

        assertNull(fixture.connection.current)
        assertTrue(controller.commands.isEmpty())
        assertEquals(1, fixture.attempts.size)
    }

    @Test
    fun `stop cleanup runs before release and invalidates already queued operations`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        val controller = fixture.connectPending()
        fixture.connection.submit { it.commands.add("obsolete seek") }

        fixture.connection.disconnect { it.commands.add("stop and clear") }
        fixture.executor.runAll()

        assertEquals(listOf("play", "stop and clear", "release"), controller.commands)
        assertEquals(1, controller.releases)
        assertNull(fixture.connection.current)
    }

    @Test
    fun `terminal stop is dispatched before fallback cleanup and controller release`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        val controller = fixture.connectPending()

        fixture.connection.disconnect {
            stopBaGuideBgmNativePlayback(
                requestTerminalStop = { it.commands.add("terminal stop") },
                pause = { it.commands.add("pause") },
                stop = { it.commands.add("stop") },
                clearMediaItems = { it.commands.add("clear") },
            )
        }
        fixture.executor.runAll()

        assertEquals(listOf("play", "terminal stop", "pause", "stop", "clear", "release"), controller.commands)
        assertEquals(1, fixture.attempts.size)
        assertNull(fixture.connection.current)
    }

    @Test
    fun `terminal dispatch failure still runs all fallback cleanup and releases the controller`() {
        val fixture = Fixture()
        fixture.connection.submit { it.commands.add("play") }
        val controller = fixture.connectPending()

        fixture.connection.disconnect {
            stopBaGuideBgmNativePlayback(
                requestTerminalStop = { throw IllegalStateException("disconnected before send") },
                pause = { throw IllegalStateException("pause failed") },
                stop = { it.commands.add("stop") },
                clearMediaItems = { it.commands.add("clear") },
            )
        }
        fixture.executor.runAll()

        assertEquals(listOf("play", "stop", "clear", "release"), controller.commands)
        assertEquals(1, controller.releases)
        assertNull(fixture.connection.current)
    }

    @Test
    fun `artwork can join an intended connection but cannot create one`() {
        val fixture = Fixture()
        fixture.connection.submit(connectIfNeeded = false) { it.commands.add("orphan artwork") }
        fixture.executor.runAll()
        assertTrue(fixture.attempts.isEmpty())

        fixture.connection.submit { it.commands.add("play") }
        fixture.connection.submit(connectIfNeeded = false) { it.commands.add("artwork") }
        val controller = fixture.connectPending()

        assertEquals(listOf("play", "artwork"), controller.commands)
        assertEquals(1, fixture.attempts.size)
    }

    @Test
    fun `disconnection while draining pending commands prevents replay of the remaining batch`() {
        val fixture = Fixture()
        fixture.connection.submit {
            it.commands.add("stop")
            fixture.attempts.single().disconnect(it)
        }
        fixture.connection.submit { it.commands.add("obsolete play") }
        val controller = fixture.connectPending()

        assertEquals(listOf("stop"), controller.commands)
        assertNull(fixture.connection.current)
        assertEquals(1, fixture.attempts.size)
    }

    @Test
    fun `already completed connection with inline executor publishes before listener execution`() {
        val controller = FakeController()
        var connects = 0
        val connection =
            BaGuideBgmControllerConnection(
                executor = Executor { it.run() },
                connect = { _ ->
                    connects += 1
                    Futures.immediateFuture(controller)
                },
                isConnected = { it: FakeController -> it.connected },
                releaseController = { it.release() },
                releaseFuture = { it.get().release() },
                onDisconnected = {},
            )

        connection.submit { it.commands.add("prepare") }
        connection.submit { it.commands.add("play") }

        assertSame(controller, connection.current)
        assertEquals(listOf("prepare", "play"), controller.commands)
        assertEquals(1, connects)
    }

    private class Fixture {
        val executor = QueuedExecutor()
        val attempts = mutableListOf<Attempt>()
        val releasedFutures = mutableListOf<ListenableFuture<FakeController>>()
        var disconnections = 0
        val connection =
            BaGuideBgmControllerConnection(
                executor = executor,
                connect = { onDisconnected ->
                    Attempt(onDisconnected).also(attempts::add).future
                },
                isConnected = { controller: FakeController -> controller.connected },
                releaseController = { controller ->
                    assertTrue(executor.running)
                    controller.release()
                },
                releaseFuture = { future ->
                    assertTrue(executor.running)
                    releasedFutures.add(future)
                    // Model MediaController.releaseFuture: cancellation alone misses completed results.
                    if (!future.cancel(false)) {
                        runCatching { future.get().release() }
                    }
                },
                onDisconnected = { disconnections += 1 },
            )

        fun connectPending(): FakeController {
            executor.runAll()
            return attempts.last().complete().also { executor.runAll() }
        }
    }

    private class Attempt(private val onDisconnected: (FakeController) -> Unit) {
        val future: SettableFuture<FakeController> = SettableFuture.create()

        fun complete(): FakeController = FakeController().also { future.set(it) }

        fun disconnect(controller: FakeController) {
            controller.connected = false
            onDisconnected(controller)
        }
    }

    private class FakeController {
        var connected = true
        var releases = 0
        val commands = mutableListOf<String>()

        fun release() {
            connected = false
            releases += 1
            commands.add("release")
        }
    }

    private class QueuedExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()
        var running = false
            private set

        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }

        fun runLast() = run(tasks.removeLast())

        fun runAll() {
            while (tasks.isNotEmpty()) run(tasks.removeFirst())
        }

        private fun run(task: Runnable) {
            running = true
            try {
                task.run()
            } finally {
                running = false
            }
        }
    }
}
