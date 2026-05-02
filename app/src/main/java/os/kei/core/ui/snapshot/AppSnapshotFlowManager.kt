package os.kei.core.ui.snapshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.SnapshotFlowManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow as composeSnapshotFlow
import kotlinx.coroutines.flow.Flow

internal class AppSnapshotFlowManager {
    @OptIn(ExperimentalComposeRuntimeApi::class)
    private val delegate = SnapshotFlowManager()
    private var disposed = false

    @OptIn(ExperimentalComposeRuntimeApi::class)
    fun dispose() {
        if (disposed) return
        disposed = true
        delegate.dispose()
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    fun <T> snapshotFlow(block: () -> T): Flow<T> {
        return composeSnapshotFlow(delegate, block)
    }
}

@Composable
internal fun rememberAppSnapshotFlowManager(): AppSnapshotFlowManager {
    val manager = remember { AppSnapshotFlowManager() }
    DisposableEffect(manager) {
        onDispose { manager.dispose() }
    }
    return manager
}
