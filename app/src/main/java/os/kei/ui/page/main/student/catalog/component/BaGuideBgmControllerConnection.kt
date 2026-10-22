package os.kei.ui.page.main.student.catalog.component

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

internal class BaGuideBgmControllerConnection<Controller : Any>(
    private val executor: Executor,
    private val connect: ((Controller) -> Unit) -> ListenableFuture<Controller>,
    private val isConnected: (Controller) -> Boolean,
    private val releaseController: (Controller) -> Unit,
    private val releaseFuture: (ListenableFuture<Controller>) -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private val lock = Any()
    private val pendingCommands = ArrayDeque<(Controller) -> Unit>()
    private var generation = 0L
    private var controller: Controller? = null
    private var controllerFuture: ListenableFuture<Controller>? = null

    val current: Controller?
        get() = synchronized(lock) { controller?.takeIf(isConnected) }

    fun submit(
        connectIfNeeded: Boolean = true,
        command: (Controller) -> Unit,
    ) {
        val submittedGeneration = synchronized(lock) { generation }
        executor.execute {
            synchronized(lock) {
                if (generation != submittedGeneration) return@execute
                val existing = controller
                if (existing != null && !isConnected(existing)) {
                    invalidateLocked()
                }
                controller?.let { connected ->
                    runCatching { command(connected) }
                    return@execute
                }
                // Artwork may join an intended connection, but must never start one itself.
                if (!connectIfNeeded && controllerFuture == null) return@execute
                pendingCommands.add(command)
                if (controllerFuture == null) startConnectionLocked()
            }
        }
    }

    fun disconnect(beforeRelease: ((Controller) -> Unit)? = null) {
        val controllerToRelease: Controller?
        val futureToRelease: ListenableFuture<Controller>?
        synchronized(lock) {
            controllerToRelease = controller
            futureToRelease = controllerFuture
            invalidateLocked()
        }
        executor.execute {
            controllerToRelease?.let { connected ->
                runCatching { beforeRelease?.invoke(connected) }
                runCatching { releaseController(connected) }
            }
            futureToRelease?.let { future -> runCatching { releaseFuture(future) } }
        }
    }

    private fun startConnectionLocked() {
        val future =
            runCatching { connect(::handleDisconnected) }.getOrElse {
                invalidateLocked()
                return
            }
        // Publish before registering: an already completed future may invoke the listener inline.
        controllerFuture = future
        future.addListener({ completeConnection(future) }, executor)
    }

    private fun completeConnection(future: ListenableFuture<Controller>) {
        synchronized(lock) {
            // disconnect() owns releasing obsolete futures, including already completed ones.
            if (controllerFuture !== future) return
            val connected = runCatching { future.get() }.getOrNull()
            controllerFuture = null
            if (connected == null || !isConnected(connected)) {
                invalidateLocked()
                return
            }
            controller = connected
            val connectedGeneration = generation
            val commands = pendingCommands.toList()
            pendingCommands.clear()
            commands.forEach { command ->
                if (generation != connectedGeneration || controller !== connected) return
                if (!isConnected(connected)) {
                    invalidateLocked()
                    return
                }
                runCatching { command(connected) }
            }
        }
    }

    private fun handleDisconnected(disconnected: Controller) {
        synchronized(lock) {
            if (controller === disconnected) invalidateLocked()
        }
    }

    private fun invalidateLocked() {
        generation += 1L
        controller = null
        controllerFuture = null
        pendingCommands.clear()
        onDisconnected()
    }
}
